export const meta = {
  name: 'oleada-ui',
  description: 'Implementa una oleada de interfaz a partir de un manifiesto oleada-ui/1: diseño en lote, constructores en paralelo por tanda, integración, revisión bloqueante, reparación acotada, cierre con el stack propio, revisión de pantallas, hoja de contacto y PR',
  whenToUse: 'Con un manifiesto oleada-ui/1 (plantilla: docs/interfaz/plantillas/manifiesto-oleada.ejemplo.json). args: { manifiesto (el JSON como objeto), ola, fase: "disenar"|"construir", carpeta, condiciones?: string[], pr?: boolean }. Guía: docs/IA/prompts/orquestacion-oleada-ui.md',
  phases: [
    { title: 'Preparar', detail: 'rama de la oleada, estado del repo y baseline' },
    { title: 'Diseñar', detail: 'fichas de flujo y specs cortas, un lote por agente' },
    { title: 'Construir', detail: 'un lote por constructor, cada uno en su worktree' },
    { title: 'Integrar', detail: 'trae los commits y corre los gates de la tanda' },
    { title: 'Revisar', detail: 'revisión independiente de cada lote integrado, con sus decisiones' },
    { title: 'Reparar', detail: 'arreglos de la revisión, hasta el tope de rondas' },
    { title: 'Cerrar oleada', detail: 'gates completos, stack propio, seed, ensayo y documentación' },
    { title: 'Revisar pantallas', detail: 'revisar-ui independiente de todas las pantallas, arreglos y hoja de contacto' },
    { title: 'PR', detail: 'un PR por oleada; nunca mergea' },
  ],
}

// Motor genérico de una oleada de interfaz; sucesor de backlog-r11.js (archivado en
// docs/IA/history/harness-ui-2026-11/workflows/). Las reglas viven en AGENTS.md, en las skills y en
// la guía (docs/IA/prompts/orquestacion-oleada-ui.md); el orden de trabajo, las decisiones del usuario
// y las reglas propias de la oleada, en el manifiesto. Este script solo ordena los pasos.
// Lo que no está decidido, cada agente lo elige, lo anota y sigue. Lo que el usuario ya decidió está
// en M.decisiones: cada una va a un lote como criterio de aceptación o queda diferida con su OK.

// ── Regla oleada-ui/1 ── idéntica en .claude/workflows/oleada-ui.js y en
// scripts/agent-check/checks/manifiestos-ui.js: el sandbox del workflow no hace require, así que la
// regla va copiada, y scripts/tests/run-tests.js falla si las dos copias difieren.
function problemasDelManifiesto(M) {
  const p = []
  const txt = (v) => typeof v === 'string' && v.trim() !== ''
  if (!M || typeof M !== 'object' || Array.isArray(M)) return ['el manifiesto no es un objeto']
  if (M.formato !== 'oleada-ui/1') p.push(`formato tiene que ser "oleada-ui/1" (vino ${JSON.stringify(M.formato)})`)
  if (!txt(M.nombre) || !/^[a-z0-9-]+$/.test(M.nombre)) p.push('nombre: solo minúsculas, números y guiones')
  for (const k of ['backlog', 'prefijoCommit']) if (!txt(M[k])) p.push(`falta ${k}`)
  if (!M.ramas || !txt(M.ramas.base) || !txt(M.ramas.destino) || !txt(M.ramas.prefijoOla)) p.push('ramas necesita base, destino y prefijoOla')
  const olas = Array.isArray(M.olas) && M.olas.length && M.olas.every(txt) ? M.olas : (p.push('olas: una lista de textos, con al menos una'), [])
  if (!Array.isArray(M.reglas) || !M.reglas.every(txt)) p.push('reglas: una lista de textos (puede estar vacía)')
  const lotes = Array.isArray(M.lotes) ? M.lotes : (p.push('lotes: una lista'), [])
  const ids = new Set()
  for (const l of lotes) {
    if (!l || !txt(l.id)) { p.push('hay un lote sin id'); continue }
    const n = `lote ${l.id}`
    if (ids.has(l.id)) p.push(`${n}: id repetido`)
    ids.add(l.id)
    if (!olas.includes(l.ola)) p.push(`${n}: su ola no está en olas`)
    if (!Number.isInteger(l.tanda)) p.push(`${n}: tanda tiene que ser un entero`)
    for (const k of ['items', 'modulos', 'rutasVisuales']) if (!Array.isArray(l[k])) p.push(`${n}: ${k} tiene que ser una lista`)
    for (const k of ['titulo', 'que', 'archivos']) if (!txt(l[k])) p.push(`${n}: falta ${k}`)
    if (!['QUICK', 'STANDARD', 'CONTRATO'].includes(l.ruta)) p.push(`${n}: ruta tiene que ser QUICK, STANDARD o CONTRATO`)
    if (!['ninguno', 'ficha', 'spec', 'ficha+spec'].includes(l.disenio)) p.push(`${n}: disenio tiene que ser ninguno, ficha, spec o ficha+spec`)
    if (!['juicio', 'mecanico'].includes(l.rol)) p.push(`${n}: rol tiene que ser juicio o mecanico`)
  }
  if (M.ensayo !== undefined && (!M.ensayo || !txt(M.ensayo.guia) || !Array.isArray(M.ensayo.lotes) || !M.ensayo.lotes.every((id) => ids.has(id)))) {
    p.push('ensayo necesita guia y lotes que existan')
  }
  const dids = new Set()
  for (const d of Array.isArray(M.decisiones) ? M.decisiones : (p.push('decisiones: una lista (puede estar vacía)'), [])) {
    if (!d || !txt(d.id)) { p.push('hay una decisión sin id'); continue }
    const n = `decisión ${d.id}`
    if (dids.has(d.id)) p.push(`${n}: id repetido`)
    dids.add(d.id)
    for (const k of ['texto', 'quien', 'ok']) if (!txt(d[k])) p.push(`${n}: falta ${k}`)
    const enLote = d.lote !== undefined
    const diferida = d.diferida !== undefined
    if (enLote === diferida) {
      p.push(`${n}: ${enLote ? 'tiene lote y diferida a la vez' : 'sin asignar: va a un lote con su criterio comprobable, o queda diferida con quién la difirió y dónde consta el OK'}`)
    } else if (enLote) {
      if (!ids.has(d.lote)) p.push(`${n}: el lote ${d.lote} no existe`)
      if (!txt(d.criterio)) p.push(`${n}: falta el criterio comprobable`)
    } else if (!d.diferida || !txt(d.diferida.quien) || !txt(d.diferida.ok)) {
      p.push(`${n}: diferida necesita quien y ok`)
    }
  }
  return p
}
// ── fin de la regla oleada-ui/1 ──

const A = args || {}
const M = A.manifiesto
if (!M || typeof M !== 'object') throw new Error('Falta args.manifiesto: pasá el JSON del manifiesto como objeto, no como texto.')
const PROBLEMAS = problemasDelManifiesto(M)
if (PROBLEMAS.length) throw new Error(`El manifiesto no cumple oleada-ui/1; no arranca:\n- ${PROBLEMAS.join('\n- ')}`)
if (!M.olas.includes(A.ola)) throw new Error(`args.ola tiene que ser una de ${M.olas.join(', ')}.`)
if (!['disenar', 'construir'].includes(A.fase)) throw new Error('args.fase tiene que ser "disenar" o "construir".')
if (!A.carpeta) throw new Error('Falta args.carpeta: la carpeta de trabajo de la corrida, fuera del repo.')

const OLA = A.ola
const OLEADA = `${M.nombre}-${OLA.toLowerCase()}`
const CONDICIONES = A.condiciones || []
const TOPE = (M.topes && M.topes.constructoresEnParalelo) || 8
const RONDAS = (M.topes && M.topes.rondasDeReparacion) || 3
const HOJA = `docs/interfaz/evidencia/${OLEADA}/hoja`
const STACK = `donatrack-${M.nombre}`
const PREFIJO = `${M.prefijoCommit}, ${OLA}, `

// Modelo y esfuerzo siempre explícitos: ningún agente hereda el modelo de la sesión.
// Las vistas van con Opus high por decisión del ADR del harness de interfaz (2026-11-15): no se cambia
// desde el manifiesto.
const JUICIO = (M.modelos && M.modelos.juicio) || { model: 'opus', effort: 'medium' }
const MECANICO = (M.modelos && M.modelos.mecanico) || { model: 'sonnet', effort: 'high' }
const VISTAS = { model: 'opus', effort: 'high' }
const esVisual = (l) => l.rutasVisuales.length > 0
const modelo = (l) => (esVisual(l) ? VISTAS : l.rol === 'mecanico' ? MECANICO : JUICIO)

const indice = M.olas.indexOf(OLA)
const RAMA = `${M.ramas.prefijoOla}${OLA.toLowerCase()}`
const RAMA_PREVIA = indice === 0 ? M.ramas.base : `${M.ramas.prefijoOla}${M.olas[indice - 1].toLowerCase()}`
const DIR = `${A.carpeta}/${OLA}`

const deLaOla = M.lotes.filter((l) => l.ola === OLA)
const lotes = deLaOla.filter((l) => !l.condicion || CONDICIONES.includes(l.condicion))
const omitidos = deLaOla.filter((l) => l.condicion && !CONDICIONES.includes(l.condicion))
if (omitidos.length) log(`Lotes condicionales que no corren: ${omitidos.map((l) => `${l.id} (${l.condicion})`).join(', ')}.`)
const tandas = [...new Set(lotes.map((l) => l.tanda))].sort((a, b) => a - b)

// Decisiones del usuario: las de los lotes de esta ola se rinden por ID al final (cumplida o no
// cumplida, con el motivo); las diferidas se informan como diferidas.
const decisionesDe = (l) => M.decisiones.filter((d) => d.lote === l.id)
const DECISIONES_OLA = M.decisiones.filter((d) => deLaOla.some((l) => l.id === d.lote))
const DIFERIDAS = M.decisiones.filter((d) => d.diferida)
const estadoDecision = {}
const noCumplida = (l, motivo) => { for (const d of decisionesDe(l)) estadoDecision[d.id] = `no cumplida: ${motivo}` }
for (const l of omitidos) noCumplida(l, `el lote ${l.id} no corrió (condición ${l.condicion})`)

const REGLAS = M.reglas.length ? `\nReglas propias de esta oleada (valen para todo lo que hagas):\n${M.reglas.map((r) => `- ${r}`).join('\n')}` : ''

const COMUN = `Trabajás en el repo DonaTrack. Seguí AGENTS.md (raíz) y, si tocás el cliente, cliente-liviano/AGENTS.md y docs/interfaz/README.md. El trabajo sale de ${M.backlog}: leé ahí tus ítems con su evidencia (archivo y línea). Español llano en todo texto de pantalla (docs/interfaz/tono-y-comunicacion.md).
Reglas de esta corrida:
- Las decisiones del usuario que te tocan están en tu ficha de lote: se cumplen tal cual y no se reabren.
- Si falta una decisión que no está ahí, no frenes a preguntar: elegí la mejor opción con tu criterio, anotala en "decisiones" (qué elegiste, qué descartaste y por qué) y seguí.
- No estimes tiempos ni horas en ningún documento.
- Solo tus archivos. Si necesitás tocar otro, hacelo solo si es imprescindible y decilo en "fueraDeZona".
- Una migración de base nueva lleva el número siguiente al último de su servicio; si choca con otra al integrar, la renumera el integrador.
- Nunca edites una línea por número sin leerla antes. No hagas push ni PR: eso lo hace el paso de PR.
- Commits con el prefijo "${PREFIJO}". No uses --no-verify.${REGLAS}
La carpeta de trabajo de la corrida (fuera del repo) es ${A.carpeta}.`

const RUTAS = `Ceremonia según la ruta del lote:
- QUICK: implementar con test y cierre liviano (LIGHTWEIGHT_CLOSING_CHECK de docs/IA/review/evaluator.md).
- STANDARD: seguir la spec corta o la ficha si existen; TDD; gates locales.
- CONTRATO: cambia la API de un servicio. La spec corta ya está en docs/specs/active/ y manda. Actualizá el contrato (docs/arquitectura/contratos/ y los contratos vivos) y, si la ruta es nueva, RutasGateway. El ADR se escribe al cerrar la oleada: dejá en "paraElAdr" la decisión, las alternativas y el porqué. Es un desvío acordado del ciclo ARCHITECTURAL completo.`

const texto = { type: 'string' }
const si = { type: 'boolean' }
const lista = { type: 'array', items: texto }

const PREPARACION = {
  type: 'object',
  properties: { ok: si, motivo: texto, rama: texto, base: texto, baseline: texto, faltanDisenios: lista },
  required: ['ok', 'motivo', 'rama', 'base', 'baseline', 'faltanDisenios'],
}
const DISENIO = {
  type: 'object',
  properties: { sha: texto, documentos: lista, resumen: texto, decisiones: lista, dudas: lista },
  required: ['sha', 'documentos', 'resumen', 'decisiones', 'dudas'],
}
const ENTREGA = {
  type: 'object',
  properties: {
    sha: texto, hecho: si, resumen: texto, archivos: texto, gates: texto,
    decisiones: lista, fueraDeZona: lista, pendiente: lista, paraElAdr: texto,
  },
  required: ['sha', 'hecho', 'resumen', 'archivos', 'gates', 'decisiones', 'fueraDeZona', 'pendiente'],
}
const INTEGRACION = {
  type: 'object',
  properties: { sha: texto, integrados: lista, fallidas: lista, gatesOk: si, detalle: texto },
  required: ['sha', 'integrados', 'fallidas', 'gatesOk', 'detalle'],
}
const REVISION = {
  type: 'object',
  properties: {
    veredicto: { type: 'string', enum: ['PASS', 'CHANGES_REQUIRED'] },
    decisiones: lista, bloqueantes: lista, menores: lista,
  },
  required: ['veredicto', 'decisiones', 'bloqueantes', 'menores'],
}
const CIERRE = {
  type: 'object',
  properties: {
    sha: texto, gatesOk: si, gates: texto, stackOk: si, stack: texto,
    ensayo: texto, ensayoSuficiente: si, bloqueantes: lista, informe: texto,
  },
  required: ['sha', 'gatesOk', 'gates', 'stackOk', 'stack', 'ensayo', 'ensayoSuficiente', 'bloqueantes', 'informe'],
}
const DOCUMENTOS = {
  type: 'object',
  properties: { sha: texto, documentos: lista, adrs: lista, decisiones: texto },
  required: ['sha', 'documentos', 'adrs', 'decisiones'],
}
const HOJA_DE_CONTACTO = {
  type: 'object',
  properties: { sha: texto, carpeta: texto, pantallas: lista, imagenes: lista },
  required: ['sha', 'carpeta', 'pantallas', 'imagenes'],
}
const PEDIDO_PR = {
  type: 'object',
  properties: { url: texto, rama: texto, nota: texto },
  required: ['url', 'rama', 'nota'],
}

const ficha = (l) => {
  const ds = decisionesDe(l)
  return `Lote ${l.id} (${l.items.join(', ')}): ${l.titulo}.
Qué hay que lograr: ${l.que}
Tus archivos: ${l.archivos}
Ruta: ${l.ruta}. Módulos: ${l.modulos.length ? l.modulos.join(', ') : '(ninguno de Maven)'}. Pantallas a comprobar: ${l.rutasVisuales.length ? l.rutasVisuales.join(', ') : '(ninguna)'}.
Decisiones del usuario que este lote cumple (criterios de aceptación; no se reabren): ${ds.length ? ds.map((d) => `${d.id}: ${d.texto} (decidió: ${d.quien}; OK en ${d.ok}). Se comprueba así: ${d.criterio}`).join(' | ') : '(ninguna)'}`
}

const gatesDe = (ls) => {
  const modulos = [...new Set(ls.flatMap((l) => l.modulos))]
  const paginas = [...new Set(ls.flatMap((l) => l.rutasVisuales))]
  const maven = modulos.length ? `mvn clean test -pl ${modulos.join(',')} -am` : '(sin módulos de Maven)'
  const visual = paginas.length
    ? `rm -rf cliente-liviano/target/capturas; mvn test -pl cliente-liviano -Pvisual -Dtest=VerificacionVisualTest -Dsurefire.failIfNoSpecifiedTests=false -Dverificacion.paginas=${paginas.join(',')}`
    : '(sin verificación visual)'
  return { maven, visual, paginas }
}

const enGrupos = (xs, n) => {
  const grupos = []
  for (let i = 0; i < xs.length; i += n) grupos.push(xs.slice(i, i + n))
  return grupos
}
// Corre los thunks de a TOPE por vez, en el orden dado.
const enParalelo = async (thunks) => {
  const salida = []
  for (const grupo of enGrupos(thunks, TOPE)) salida.push(...(await parallel(grupo)))
  return salida
}

const resultado = {
  ola: OLA, fase: A.fase, rama: RAMA, sha: '', lotes: [], decisiones: [], pendientes: [],
  decisionesDelUsuario: [], paraAprobar: [], ensayo: '', ensayoSuficiente: null,
  revisionDePantallas: '', hoja: '', pr: '', detenida: '',
}
// Cada decisión de la ola, por ID, con su estado; las diferidas, con quién y dónde consta el OK.
const rendirDecisiones = () => {
  resultado.decisionesDelUsuario = [
    ...DECISIONES_OLA.map((d) => `${d.id} (lote ${d.lote}): ${estadoDecision[d.id] || 'no cumplida: el lote no llegó a revisarse'} — ${d.texto}`),
    ...DIFERIDAS.map((d) => `${d.id}: diferida por ${d.diferida.quien} (OK en ${d.diferida.ok}) — ${d.texto}`),
  ]
}
const terminar = (motivo) => {
  if (motivo) { resultado.detenida = motivo; log(motivo) }
  rendirDecisiones()
  return resultado
}
const anotar = (id, e) => {
  for (const d of e.decisiones || []) resultado.decisiones.push(`${id}: ${d}`)
  for (const p of e.pendiente || []) resultado.pendientes.push(`${id}: ${p}`)
  for (const f of e.fueraDeZona || []) resultado.decisiones.push(`${id} tocó fuera de su zona: ${f}`)
}

const integrar = async (entregas, ls, etiqueta, base) => {
  const g = gatesDe(ls)
  return agent(`${COMUN}

Sos el integrador de ${etiqueta}. En este checkout, en la rama ${RAMA} (tiene que estar en ${base}; si no, frená y decilo en detalle):
1. git cherry-pick de cada commit, en este orden: ${entregas.map((e) => `${e.lote} = ${e.sha}`).join('; ')}. Si un conflicto es de agregados en un archivo compartido (el final de una hoja de estilos, una fila más de una tabla de un documento, una ruta más del gateway), quedate con los dos lados. Si dos migraciones tienen el mismo número, renumerá la segunda al siguiente libre. Si es otra cosa, git cherry-pick --abort de ese commit y anotalo en fallidas con el motivo.
2. Gates de lo integrado: ${g.maven}
3. Verificación visual: ${g.visual}. Mirá el reporte: accesibilidad AA, nada desbordado ni superpuesto y áreas de toque de 48 px.
4. Si un gate falla por un choque entre lotes (no por un defecto de un solo lote), arreglalo y comiteá "${PREFIJO}integración ${etiqueta}". Si falla por un lote solo, no lo arregles: decilo en detalle con el lote, el test y el mensaje.
Devolvé sha (HEAD), integrados (ids de lote), fallidas (id y motivo), gatesOk y detalle.`, { label: `integrar ${etiqueta}`, phase: 'Integrar', schema: INTEGRACION, ...MECANICO })
}

// ── Preparar ─────────────────────────────────────────────────────────────────────────────────
phase('Preparar')
const conDisenio = lotes.filter((l) => l.disenio !== 'ninguno')
const g0 = gatesDe(lotes)
const prep = await agent(`${COMUN}

Preparás la oleada ${OLA}, fase ${A.fase}. En este checkout:
1. git status: si hay cambios sin comitear en archivos versionados, ok=false y el motivo (no los toques ni los guardes en stash). Los archivos sin seguimiento que ya estaban no cuentan.
2. Rama de la oleada: ${RAMA}. Si existe, git switch ${RAMA}. Si no, creala desde ${RAMA_PREVIA} (git switch -c ${RAMA} ${RAMA_PREVIA}); si ${RAMA_PREVIA} no existe, ok=false.
3. mkdir -p ${DIR}.
${A.fase === 'construir'
    ? `4. Baseline: ${g0.maven}. baseline = GREEN o RED; si es RED, dejá el detalle en ${DIR}/baseline.md y seguí (ok=true): los constructores tienen que saber qué ya fallaba.
5. Diseños: estos lotes necesitan su documento aprobado en la rama: ${conDisenio.length ? conDisenio.map((l) => `${l.id} (${l.disenio})`).join('; ') : 'ninguno'}. Las fichas están en docs/interfaz/flujos/ y las specs en docs/specs/active/ con el id del lote en el nombre o en el encabezado. En faltanDisenios, los ids de lote que no tienen el suyo.`
    : `4. baseline = "no corrida" y faltanDisenios vacío: en esta fase solo se escriben documentos.`}
Devolvé ok, motivo, rama, base (el SHA de HEAD), baseline y faltanDisenios.`, { label: 'preparar', phase: 'Preparar', schema: PREPARACION, ...MECANICO })

if (!prep) return terminar('Detenida en Preparar: el agente no respondió.')
if (!prep.ok) return terminar(`No arrancó: ${prep.motivo}`)
let base = prep.base
resultado.sha = base
log(`Oleada ${OLA}, fase ${A.fase}: ${lotes.length} lotes en ${tandas.length} tandas, ${DECISIONES_OLA.length} decisiones asignadas, rama ${RAMA}, baseline ${prep.baseline}.`)

// ── Fase "disenar": fichas y specs de toda la oleada, para aprobar de una vez ─────────────────
if (A.fase === 'disenar') {
  phase('Diseñar')
  if (!conDisenio.length) return terminar(`La oleada ${OLA} no tiene lotes con diseño previo: pasá directo a construir.`)
  const disenios = (await enParalelo(conDisenio.map((l) => () => agent(`${COMUN}

Sos el diseñador del lote. No escribís código: solo los documentos que se aprueban antes de construir.
${ficha(l)}
Documento pedido: ${l.disenio}.
Primero: git reset --hard ${base} (este worktree es tuyo).
${l.disenio.includes('ficha') ? `- Ficha de flujo: seguí el paso 1 de la skill construir-ui (.agents/skills/construir-ui/SKILL.md) y la plantilla docs/interfaz/plantillas/flujo.md. Si la pantalla ya tiene ficha en docs/interfaz/flujos/, actualizala en lugar de crear otra; si no, creá docs/interfaz/flujos/${M.nombre}-${l.id}.md. Recorrido con desvíos, cada estado con su texto, el patrón conocido que sigue (docs/interfaz/criterios-catedra-ux-ui.md) y la revisión con las 10 heurísticas. Cada decisión del usuario de este lote aparece en la ficha con su ID.` : ''}
${l.disenio.includes('spec') ? `- Spec corta: docs/specs/active/${M.nombre}-${l.id}-spec.md, con el formato de la ruta STANDARD de la skill define-spec: alcance, fuera de alcance, contrato antes y después (rutas, parámetros, respuesta), criterios de aceptación verificables (las decisiones del usuario de este lote, con su ID), compatibilidad con los clientes actuales y riesgos. Sin matriz 5D ni diseño de clases.` : ''}
Comiteá "${PREFIJO}diseño ${l.id}". Devolvé sha, documentos (rutas), resumen (lo que el usuario tiene que mirar, en 3 líneas y sin jerga), decisiones y dudas (cada una con la opción que recomendás).`, { label: `diseño ${l.id}`, phase: 'Diseñar', isolation: 'worktree', schema: DISENIO, ...modelo(l) }).then((d) => d && { ...d, lote: l.id })))).filter(Boolean)

  const sinDisenio = conDisenio.filter((l) => !disenios.some((d) => d.lote === l.id))
  if (sinDisenio.length) log(`Sin diseño (el agente no respondió): ${sinDisenio.map((l) => l.id).join(', ')}.`)
  if (!disenios.length) return terminar('Detenida en Diseñar: ningún diseñador respondió.')

  const integrado = await integrar(disenios, [], `diseños ${OLA}`, base)
  if (!integrado) return terminar('Detenida al integrar los diseños: el integrador no respondió.')
  resultado.sha = integrado.sha
  for (const d of disenios) {
    anotar(d.lote, { decisiones: d.decisiones })
    resultado.paraAprobar.push({ lote: d.lote, documentos: d.documentos, resumen: d.resumen, dudas: d.dudas })
  }
  for (const f of integrado.fallidas) resultado.pendientes.push(`diseño sin integrar: ${f}`)
  for (const l of sinDisenio) resultado.pendientes.push(`${l.id}: sin diseño`)
  for (const d of DECISIONES_OLA) estadoDecision[d.id] = 'pendiente: se comprueba en la fase construir'
  return terminar('')
}

// ── Fase "construir" ─────────────────────────────────────────────────────────────────────────
const sinAprobar = new Set(prep.faltanDisenios)
const BASELINE = prep.baseline === 'RED' ? ` El baseline ya fallaba antes de empezar: leé ${DIR}/baseline.md y no lo cuentes como tuyo.` : ''
// Lotes con PASS de su revisión. Un lote visual (revisar-ui es bloqueante) o con decisiones del
// usuario que termina sin PASS, por la razón que sea (sin diseño, sin construir, sin integrar o con
// bloqueantes), frena el PR.
const conPass = new Set()
const frenaElPr = (l) => esVisual(l) || decisionesDe(l).length > 0
// Una decisión rendida como NO CUMPLE es bloqueante aunque el revisor no la haya listado como tal.
const conDecisionesComoBloqueantes = (r) => {
  const noCumple = r.decisiones.filter((x) => /NO CUMPLE/i.test(x) && !r.bloqueantes.includes(x))
  return noCumple.length ? { ...r, veredicto: 'CHANGES_REQUIRED', bloqueantes: [...r.bloqueantes, ...noCumple] } : r
}

for (const t of tandas) {
  const etiqueta = `${OLA} tanda ${t}`
  const deLaTanda = lotes.filter((l) => l.tanda === t)
  const frenados = deLaTanda.filter((l) => sinAprobar.has(l.id))
  for (const l of frenados) {
    resultado.pendientes.push(`${l.id}: no se construyó porque falta su ${l.disenio} aprobado en la rama`)
    noCumplida(l, `falta su ${l.disenio} aprobado`)
  }
  if (frenados.length) log(`${etiqueta}: sin diseño aprobado, no se construyen ${frenados.map((l) => l.id).join(', ')}.`)
  const aConstruir = deLaTanda.filter((l) => !sinAprobar.has(l.id))
  if (!aConstruir.length) continue

  // ── Construir ──────────────────────────────────────────────────────────────────────────────
  const entregas = (await enParalelo(aConstruir.map((l) => () => {
    const g = gatesDe([l])
    const otros = aConstruir.filter((o) => o !== l).map((o) => `${o.id} (${o.archivos})`).join('; ')
    return agent(`${COMUN}

Sos el constructor del lote. ${RUTAS}
${ficha(l)}
${l.disenio !== 'ninguno' ? `Tu diseño aprobado está en la rama (docs/interfaz/flujos/ y docs/specs/active/, buscá ${l.id}): es tu encargo y manda sobre este resumen.` : 'Este lote no lleva diseño previo.'}
En paralelo trabajan otros lotes; no toques sus archivos: ${otros || '(ninguno)'}.
Primero: git reset --hard ${base} (este worktree es tuyo).${BASELINE}
Cómo trabajar: skill implement-task (.agents/skills/implement-task/SKILL.md); para vistas, además, construir-ui (.agents/skills/construir-ui/SKILL.md): todos los estados, mejora progresiva, solo tokens semánticos, el catálogo de componentes y componentes/listado para toda lista con búsqueda o filtros. Test primero.${esVisual(l) ? ' La pantalla tiene que verse y comportarse igual que las demás que usan las mismas piezas: compará tus capturas con las de otra pantalla del sistema antes de comitear.' : ' Lo más simple que funcione.'}
Gates antes de comitear: ${g.maven} ; ${g.visual}
Comiteá "${PREFIJO}${l.id}". Si no pudiste terminar algo, hecho=false y contalo en pendiente; igual comiteá lo que quedó bien y en verde.
Devolvé sha, hecho, resumen, archivos, gates (cuáles corriste y cómo dieron), decisiones, fueraDeZona, pendiente y paraElAdr (solo ruta CONTRATO).`, { label: `${l.id}`, phase: 'Construir', isolation: 'worktree', schema: ENTREGA, ...modelo(l) }).then((e) => e && { ...e, lote: l.id })
  }))).filter(Boolean)

  const mudos = aConstruir.filter((l) => !entregas.some((e) => e.lote === l.id))
  for (const l of mudos) {
    resultado.pendientes.push(`${l.id}: el constructor no respondió`)
    noCumplida(l, 'el constructor no respondió')
  }
  if (!entregas.length) { log(`${etiqueta}: ningún constructor entregó.`); continue }
  for (const e of entregas) anotar(e.lote, e)

  // ── Integrar ───────────────────────────────────────────────────────────────────────────────
  const integracion = await integrar(entregas, aConstruir, etiqueta, base)
  if (!integracion) {
    resultado.pendientes.push(`${etiqueta}: el integrador no respondió; la tanda quedó sin integrar`)
    for (const l of aConstruir) noCumplida(l, 'la tanda quedó sin integrar')
    continue
  }
  for (const f of integracion.fallidas) resultado.pendientes.push(`${etiqueta}, sin integrar: ${f}`)
  base = integracion.sha
  const integrados = aConstruir.filter((l) => integracion.integrados.includes(l.id))
  for (const l of aConstruir.filter((x) => !integrados.includes(x))) noCumplida(l, 'no se integró')

  // ── Revisar y reparar, hasta RONDAS. Solo PASS aprueba: un revisor que no responde o que pide
  // cambios sin decir cuáles se vuelve a revisar en la ronda siguiente. ──────────────────────────
  let aRevisar = integrados
  let gatesRotos = integracion.gatesOk ? '' : integracion.detalle
  for (let ronda = 1; ronda <= RONDAS && aRevisar.length; ronda++) {
    const desde = prep.base
    const revisiones = (await enParalelo(aRevisar.map((l) => () => agent(`Sos un revisor independiente y escéptico en el repo DonaTrack. Solo lectura: no modificás nada ni corrés nada que escriba. Revisás el lote ${l.id} ya integrado en la rama ${RAMA} (HEAD ${base}); su cambio está en los commits "${PREFIJO}${l.id}" y sus arreglos (git log ${desde}..${base} --grep="${l.id}").
${ficha(l)}
${l.disenio !== 'ninguno' ? `Su diseño aprobado está en docs/interfaz/flujos/ o docs/specs/active/ (buscá ${l.id}): revisá contra eso.` : ''}
Cómo revisar: ${l.ruta === 'QUICK' ? 'LIGHTWEIGHT_CLOSING_CHECK de docs/IA/review/evaluator.md.' : 'skill implementation-review (.agents/skills/implementation-review/SKILL.md), vectores V1–V9.'}${esVisual(l) ? ' Además, skill revisar-ui (.agents/skills/revisar-ui/SKILL.md), vectores U1–U7, mirando las capturas de cliente-liviano/target/capturas a 360 y 1280 px con la herramienta de lectura de imágenes, junto a las de otra pantalla que use las mismas piezas.' : ''}
Cada decisión del usuario de la ficha es un criterio de aceptación: en "decisiones", una línea por ID con CUMPLE o NO CUMPLE y su evidencia (archivo:línea o captura).${M.reglas.length ? ` Comprobá también las reglas propias de la oleada: ${M.reglas.join(' | ')}.` : ''}${gatesRotos ? ` Al integrar fallaron gates: ${gatesRotos}. Decí si la causa es de este lote.` : ''}
Bloqueante es lo que rompe un criterio de aceptación (incluida toda decisión del usuario que NO CUMPLE), un gate, un requerimiento del enunciado o una regla del repo${esVisual(l) ? ', y todo lo que falla en U1–U7 de revisar-ui: cualquier inconsistencia con el sistema visual o con otra pantalla es BLOQUEANTE, no menor' : ''}. Lo demás va en menores. Con un bloqueante, el veredicto es CHANGES_REQUIRED. Cada hallazgo con archivo:línea y qué habría que cambiar.`, { label: `revisión ${l.id} r${ronda}`, phase: 'Revisar', schema: REVISION, ...(esVisual(l) ? VISTAS : JUICIO) }).then((r) => r && { ...conDecisionesComoBloqueantes(r), lote: l })))).filter(Boolean)

    const aprobados = revisiones.filter((r) => r.veredicto === 'PASS' && !r.bloqueantes.length)
    for (const r of aprobados) {
      conPass.add(r.lote.id)
      for (const m of r.menores) resultado.pendientes.push(`${r.lote.id}, menor: ${m}`)
      for (const d of decisionesDe(r.lote)) estadoDecision[d.id] = `cumplida (PASS de la revisión del lote, ronda ${ronda})`
    }
    const conCambios = revisiones.filter((r) => !aprobados.includes(r) && r.bloqueantes.length)
    const sinVeredicto = aRevisar.filter((l) => !aprobados.some((r) => r.lote === l) && !conCambios.some((r) => r.lote === l))
    if (ronda === RONDAS) {
      for (const r of conCambios) resultado.pendientes.push(`${r.lote.id}: quedó con bloqueantes tras ${RONDAS} rondas: ${r.bloqueantes.join(' | ')}`)
      for (const l of sinVeredicto) resultado.pendientes.push(`${l.id}: sin PASS tras ${RONDAS} rondas (el revisor no respondió o no dio bloqueantes)`)
      const quedan = [...conCambios.map((r) => r.lote), ...sinVeredicto]
      for (const l of quedan) noCumplida(l, `el lote quedó sin PASS de su revisión tras ${RONDAS} rondas`)
      if (quedan.length) log(`${etiqueta}: ${quedan.map((l) => l.id).join(', ')} quedaron sin PASS tras ${RONDAS} rondas.`)
      break
    }
    if (!conCambios.length) { aRevisar = sinVeredicto; continue }

    const arreglos = (await enParalelo(conCambios.map((r) => () => {
      const g = gatesDe([r.lote])
      return agent(`${COMUN}

Arreglás los hallazgos bloqueantes de la revisión del lote ${r.lote.id}, ronda ${ronda}. ${RUTAS}
${ficha(r.lote)}
Hallazgos: ${r.bloqueantes.map((b, i) => `${i + 1}) ${b}`).join(' ')}
Primero: git reset --hard ${base} (este worktree es tuyo). Verificá cada hallazgo contra el archivo antes de tocar nada; si uno no es real, no lo arregles y decilo en decisiones con el porqué. Gates: ${g.maven} ; ${g.visual}
Comiteá "${PREFIJO}${r.lote.id}, arreglos ronda ${ronda}". Devolvé los campos de siempre.`, { label: `arreglos ${r.lote.id} r${ronda}`, phase: 'Reparar', isolation: 'worktree', schema: ENTREGA, ...modelo(r.lote) }).then((e) => e && { ...e, lote: r.lote.id })
    }))).filter(Boolean)
    for (const e of arreglos) anotar(e.lote, e)

    const reintegrado = arreglos.length ? await integrar(arreglos, conCambios.map((r) => r.lote), `${etiqueta}, arreglos ronda ${ronda}`, base) : null
    if (reintegrado) {
      for (const f of reintegrado.fallidas) resultado.pendientes.push(`${etiqueta}, arreglo sin integrar: ${f}`)
      base = reintegrado.sha
      gatesRotos = reintegrado.gatesOk ? '' : reintegrado.detalle
    } else {
      resultado.pendientes.push(`${etiqueta}, ronda ${ronda}: los arreglos no se integraron (no respondió el arreglo o el integrador)`)
    }
    // Todo lo que no aprobó vuelve a revisarse: lo arreglado y lo que quedó igual.
    aRevisar = [...conCambios.map((r) => r.lote), ...sinVeredicto]
  }

  for (const l of integrados) resultado.lotes.push({ id: l.id, items: l.items, tanda: t })
  resultado.sha = base
  log(`${etiqueta}: integrados ${integrados.map((l) => l.id).join(', ') || 'ninguno'}.`)
}

if (!resultado.lotes.length) return terminar(`La oleada ${OLA} no integró ningún lote.`)

// ── Cerrar oleada ────────────────────────────────────────────────────────────────────────────
phase('Cerrar oleada')
const hechos = lotes.filter((l) => resultado.lotes.some((r) => r.id === l.id))
const gF = gatesDe(hechos)
const conEnsayo = Boolean(M.ensayo) && hechos.some((l) => M.ensayo.lotes.includes(l.id))

const cierre = await agent(`${COMUN}

Cerrás la oleada ${OLA} en la rama ${RAMA} (HEAD ${base}). Lotes integrados: ${hechos.map((l) => `${l.id} (${l.items.join(', ')})`).join('; ')}.
1. Gates completos: ${gF.maven} ; ${gF.visual}. Si tocaron servicios, además los tests de integración que apliquen (docs/testing/integration-tests.md).
2. Stack propio de la corrida, sin tocar los datos del usuario:
   - Si hay un stack del usuario corriendo, detenelo con docker compose stop. Nunca docker compose down -v sobre ese stack: borra sus datos.
   - Levantá el de la corrida con otro nombre de proyecto: docker compose -p ${STACK} up -d --build. Si los archivos de compose fijan nombres de contenedor que chocan, resolvelo con un archivo de override dentro de ${A.carpeta}, no editando los compose del repo.
   - Seed: docs/testing/demo-seeder.md, con -p ${STACK}. La clave de la demo sale del .env; no la escribas en ningún archivo ni en tu respuesta.
3. Recorrido con login real, una cuenta por rol de la tabla del seeder: cada pantalla tocada en esta oleada carga, muestra datos del seed y cumple lo que dice su ítem en ${M.backlog}. Anotá lo que no.
${conEnsayo ? `4. Ensayo: seguí ${M.ensayo.guia}. ensayo = las mediciones; ensayoSuficiente según el criterio de la guía.` : `4. Sin ensayo en esta oleada: ensayo vacío y ensayoSuficiente=true.`}
5. Al terminar: docker compose -p ${STACK} down -v (solo ese proyecto) y, si detuviste el stack del usuario, docker compose start para devolverlo como estaba.
6. Si un gate falla por algo chico y evidente, arreglalo y comiteá "${PREFIJO}cierre". Lo que no sea chico va en bloqueantes, con el lote, la evidencia y la opción que recomendás.
Escribí el informe en ${DIR}/cierre.md. Devolvé sha (HEAD), gatesOk, gates, stackOk, stack, ensayo, ensayoSuficiente, bloqueantes e informe (la ruta).`, { label: `cierre ${OLA}`, phase: 'Cerrar oleada', schema: CIERRE, ...MECANICO })

if (!cierre) {
  resultado.pendientes.push(`Cierre de ${OLA}: el agente no respondió; la oleada quedó integrada pero sin verificar con el stack`)
} else {
  base = cierre.sha
  resultado.ensayo = cierre.ensayo
  resultado.ensayoSuficiente = cierre.ensayoSuficiente
  for (const b of cierre.bloqueantes) resultado.pendientes.push(`cierre ${OLA}, bloqueante: ${b}`)
  if (!cierre.gatesOk) resultado.pendientes.push(`cierre ${OLA}: gates en rojo: ${cierre.gates}`)
  if (!cierre.stackOk) resultado.pendientes.push(`cierre ${OLA}: no se pudo verificar con el stack: ${cierre.stack}`)
}

if (cierre && cierre.bloqueantes.length) {
  const arreglo = await agent(`${COMUN}

Arreglás los bloqueantes que dejó el cierre de la oleada ${OLA}, en este checkout, rama ${RAMA} (HEAD ${base}). ${RUTAS}
Bloqueantes: ${cierre.bloqueantes.map((b, i) => `${i + 1}) ${b}`).join(' ')}
El informe del cierre está en ${cierre.informe}. Verificá cada uno contra el código antes de tocar nada. Arreglá los que sean reales y acotados; el que pida un rediseño o un cambio de contrato nuevo, no: dejalo en pendiente con la opción que recomendás. Gates: ${gF.maven} ; ${gF.visual}
Comiteá "${PREFIJO}arreglos del cierre". Devolvé los campos de siempre (sha = HEAD).`, { label: `arreglos del cierre ${OLA}`, phase: 'Cerrar oleada', schema: ENTREGA, ...(hechos.some(esVisual) ? VISTAS : JUICIO) })
  if (arreglo) { anotar(`cierre ${OLA}`, arreglo); base = arreglo.sha || base }
}

rendirDecisiones()
const adrs = hechos.filter((l) => l.ruta === 'CONTRATO')
const docs = await agent(`${COMUN}

Escribís la documentación de cierre de la oleada ${OLA}, en este checkout, rama ${RAMA} (HEAD ${base}). Solo documentos.
Lotes integrados: ${hechos.map((l) => `${l.id} (${l.items.join(', ')}; ${l.ruta})`).join('; ')}.
1. ${M.backlog}: marcá el estado de cada ítem de la oleada (hecho, parcial con lo que falta, o no hecho con el motivo).
2. docs/interfaz/bitacora.md: una entrada por decisión de diseño de esta oleada, con el formato de la bitácora. Las del usuario llevan su ID y su "quién"; las que tomó un agente sin consultar dicen "agente".
3. Lo que corresponda de: docs/interfaz/patrones-y-microcopy.md, componentes.md, mapa-del-sitio.md, tono-y-comunicacion.md, el documento de requerimientos de la entrega si cambia el estado de uno, y docs/adr/DEUDA_TECNICA.md (lo que quedó pendiente de esta oleada).
4. ADRs de los lotes de contrato: ${adrs.length ? adrs.map((l) => l.id).join(', ') : 'ninguno'}. Uno por decisión, con docs/adr/README.md, tomando la decisión, las alternativas y el porqué de la spec y del código integrado. Cada uno dice que se escribió después del código, como desvío acordado del ciclo ARCHITECTURAL. Mové las specs cerradas de docs/specs/active/ a docs/specs/completed/.
5. ${DIR}/decisiones.md (fuera del repo), en lenguaje llano, para el informe final al usuario:
Decisiones del usuario, por ID: ${resultado.decisionesDelUsuario.join(' || ') || '(ninguna)'}
Decisiones que tomaron los agentes sin consultar: ${resultado.decisiones.join(' || ') || '(ninguna)'}
Pendientes: ${resultado.pendientes.join(' || ') || '(ninguno)'}
Comiteá "${PREFIJO}documentación". Devolvé sha, documentos, adrs y decisiones (la ruta del archivo del punto 5).`, { label: `documentación ${OLA}`, phase: 'Cerrar oleada', schema: DOCUMENTOS, ...JUICIO })
if (docs) base = docs.sha || base
else resultado.pendientes.push(`Documentación de cierre de ${OLA}: el agente no respondió`)
resultado.sha = base

// ── Revisar pantallas: revisar-ui independiente sobre todas las pantallas de la oleada, con las
// capturas a 360 y 1280 lado a lado. Sin PASS no hay PR. Después, la hoja de contacto. ──────────
const visuales = hechos.filter(esVisual)
const gV = gatesDe(visuales)
let revisionOk = !visuales.length
if (visuales.length) {
  phase('Revisar pantallas')
  for (let ronda = 1; ronda <= RONDAS; ronda++) {
    const rev = await agent(`Sos un revisor de interfaz independiente en el repo DonaTrack, en la rama ${RAMA} (HEAD ${base}). No conocés cómo se construyó: recibís solo los artefactos. Seguí la skill revisar-ui (.agents/skills/revisar-ui/SKILL.md), Mode INDEPENDENT_REVIEW, sobre TODAS las pantallas de la oleada juntas: ${gV.paginas.join(', ')}.
1. Capturas: ${gV.visual}. Por cada pantalla, mirá su captura de 360 y la de 1280 una al lado de la otra (herramienta de lectura de imágenes), y comparalas con las de las demás pantallas de la oleada y con al menos una pantalla del sitio que use las mismas piezas.
2. U1–U7 de revisar-ui: todo lo que falla es BLOQUEANTE, en especial cualquier inconsistencia entre pantallas o con el sistema visual (componentes.md, componentes/listado, tokens).
3. Decisiones del usuario que estas pantallas cumplen (criterios de aceptación): ${DECISIONES_OLA.filter((d) => visuales.some((l) => l.id === d.lote)).map((d) => `${d.id}: ${d.texto}; se comprueba así: ${d.criterio}`).join(' | ') || '(ninguna)'}. En "decisiones", una línea por ID con CUMPLE o NO CUMPLE y su evidencia; un NO CUMPLE es bloqueante.${M.reglas.length ? `\n4. Reglas propias de la oleada: ${M.reglas.join(' | ')}.` : ''}
No modificás nada. Cada hallazgo con archivo:línea o captura, qué falla y cómo se arregla. Con un bloqueante, CHANGES_REQUIRED.`, { label: `pantallas r${ronda}`, phase: 'Revisar pantallas', schema: REVISION, ...VISTAS }).then((r) => r && conDecisionesComoBloqueantes(r))
    if (rev && rev.veredicto === 'PASS' && !rev.bloqueantes.length) {
      revisionOk = true
      resultado.revisionDePantallas = `PASS en la ronda ${ronda}`
      for (const m of rev.menores) resultado.pendientes.push(`pantallas, menor: ${m}`)
      break
    }
    const motivo = rev ? rev.bloqueantes.join(' | ') || 'CHANGES_REQUIRED sin bloqueantes' : 'el revisor no respondió'
    resultado.revisionDePantallas = `sin PASS en la ronda ${ronda}: ${motivo}`
    if (ronda === RONDAS || !rev || !rev.bloqueantes.length) continue
    const arreglo = await agent(`${COMUN}

Arreglás los bloqueantes de la revisión de pantallas de la oleada ${OLA}, ronda ${ronda}, en este checkout, rama ${RAMA} (HEAD ${base}). Skill construir-ui (.agents/skills/construir-ui/SKILL.md).
Hallazgos: ${rev.bloqueantes.map((b, i) => `${i + 1}) ${b}`).join(' ')}
Verificá cada uno contra el archivo y las capturas antes de tocar nada; si uno no es real, no lo arregles y decilo en decisiones con el porqué. Gates: ${gV.maven} ; ${gV.visual}
Comiteá "${PREFIJO}arreglos de pantallas ronda ${ronda}". Devolvé los campos de siempre (sha = HEAD).`, { label: `arreglos de pantallas r${ronda}`, phase: 'Revisar pantallas', schema: ENTREGA, ...VISTAS })
    if (arreglo) { anotar(`pantallas ${OLA}`, arreglo); base = arreglo.sha || base }
  }
  resultado.sha = base
}

// Sin PASS de las pantallas, o con un lote visual o con decisiones que terminó sin PASS de su
// revisión (por la razón que sea), no se abre el PR.
const motivosSinPr = []
const sinPass = lotes.filter((l) => frenaElPr(l) && !conPass.has(l.id)).map((l) => l.id)
if (!revisionOk) {
  motivosSinPr.push(`la revisión de pantallas no llegó a PASS en ${RONDAS} rondas (${resultado.revisionDePantallas})`)
  for (const l of visuales) noCumplida(l, 'la revisión de pantallas no llegó a PASS')
}
if (sinPass.length) motivosSinPr.push(`lotes sin PASS de su revisión: ${sinPass.join(', ')}`)
if (motivosSinPr.length) return terminar(`No se abre el PR de ${OLA}: ${motivosSinPr.join('; ')}. La rama ${RAMA} queda local en ${base}.`)

if (visuales.length) {
  const hoja = await agent(`Armás la hoja de contacto de la oleada ${OLA}, en este checkout, rama ${RAMA} (HEAD ${base}). Solo evidencia; no tocás código.
1. Capturas: ${gV.visual}.
2. mkdir -p ${HOJA} y java scripts/visual/Imagenes.java jpg cliente-liviano/target/capturas ${HOJA} 400 "*-{360,1280}.png" (las de 360 y 1280 de cada pantalla, como JPG de 400 KB o menos). No uses la orden lado: deforma la captura de 1280 al ponerla junto a la de 360.
3. Guion ${HOJA}/guion.txt para scripts/visual/README.md (orden resumen): "# Hoja de contacto, oleada ${OLEADA}" y, por pantalla, "## <ruta>" seguido de "img <captura>-360.jpg | 360 px" y "img <captura>-1280.jpg | 1280 px" (dos líneas img seguidas van en la misma fila, lado a lado). Después: java scripts/visual/Imagenes.java resumen ${HOJA}/guion.txt ${HOJA}/hoja.html
4. git add ${HOJA} y comiteá "${PREFIJO}hoja de contacto".
Devolvé sha (HEAD), carpeta, pantallas (las rutas) e imagenes (las rutas de los JPG, en pares 360 y 1280).`, { label: `hoja ${OLA}`, phase: 'Revisar pantallas', schema: HOJA_DE_CONTACTO, ...MECANICO })
  if (!hoja) return terminar(`No se abre el PR de ${OLA}: no se armó la hoja de contacto. La rama ${RAMA} queda local en ${base}.`)
  base = hoja.sha || base
  resultado.hoja = hoja.carpeta
  resultado.sha = base
}

// ── PR ───────────────────────────────────────────────────────────────────────────────────────
rendirDecisiones()
if (A.pr === false) return terminar('')
phase('PR')
const pr = await agent(`${COMUN}

Abrís el PR de la oleada ${OLA}. Esta es la única etapa que hace push.
1. git push -u origin ${RAMA}.
2. Destino: ${M.ramas.destino}. Si ya hay un PR abierto de ${RAMA}, actualizá su cuerpo en lugar de abrir otro (nunca mientras corre su CI). ${indice > 0 ? `Esta rama sale de ${RAMA_PREVIA}: si su PR todavía no se mergeó, decilo en la primera línea del cuerpo (este PR incluye sus commits).` : `Esta rama sale de ${M.ramas.base}: si ${M.ramas.base} todavía no está en ${M.ramas.destino}, decilo en la primera línea del cuerpo.`}
3. Cuerpo en ${DIR}/pr-body.md con la plantilla de docs/IA/04-checklist-antes-de-pr.md: qué se pidió (${M.backlog} y la oleada), los ítems hechos con su id, los gates con su resultado, la verificación con el stack${conEnsayo ? ' y el ensayo' : ''}, la revisión de pantallas (${resultado.revisionDePantallas || 'sin pantallas'}), las decisiones tomadas sin consultar (${DIR}/decisiones.md) y lo que queda pendiente. Sin estimaciones de tiempo.
   - Sección "Decisiones del usuario": una fila por ID con su estado, tal cual: ${resultado.decisionesDelUsuario.join(' || ') || '(ninguna)'}
${resultado.hoja ? `   - Sección "Hoja de contacto": una tabla | Pantalla | 360 px | 1280 px | con cada par de JPG de ${resultado.hoja}, como imágenes con la dirección https://github.com/<dueño>/<repo>/raw/<sha>/<ruta> (el dueño y el repo, de gh repo view --json nameWithOwner; el sha, el HEAD que empujaste), el enlace a ${resultado.hoja}/hoja.html y, debajo, esta casilla sin marcar: "- [ ] Hoja de contacto aprobada por el usuario".` : '   - Sin hoja de contacto: la oleada no tocó pantallas; decilo en una línea.'}
   - Última línea: 🤖 Generated with [Claude Code](https://claude.com/claude-code)
4. gh pr create --draft --base ${M.ramas.destino} --head ${RAMA} --title "${M.prefijoCommit}, oleada ${OLA}" --body-file ${DIR}/pr-body.md. No lo mergees ni actives el merge automático: lo revisa y lo mergea el usuario.
Devolvé url, rama y nota (lo que el usuario tiene que saber antes de revisar).`, { label: `PR ${OLA}`, phase: 'PR', schema: PEDIDO_PR, ...MECANICO })
if (pr) resultado.pr = pr.url
else resultado.pendientes.push(`PR de ${OLA}: el agente no respondió; la rama ${RAMA} quedó local`)

return terminar('')
