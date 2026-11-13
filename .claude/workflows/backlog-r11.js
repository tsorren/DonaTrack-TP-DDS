export const meta = {
  name: 'backlog-r11',
  description: 'Implementa una oleada del backlog de la revisión del 2026-11-11: diseño en lote, constructores en paralelo por tanda, integración, revisión, reparación acotada, verificación con el stack propio y PR',
  whenToUse: 'Con el manifiesto docs/interfaz/backlog-revision-2026-11-11.manifiesto.json. args: { manifiesto (el JSON como objeto), ola: "P0"|"P1"|"P2", fase: "disenar"|"construir", carpeta, condiciones?: string[], pr?: boolean }. Guía: docs/IA/prompts/orquestacion-backlog-r11.md',
  phases: [
    { title: 'Preparar', detail: 'rama de la oleada, estado del repo y baseline' },
    { title: 'Diseñar', detail: 'fichas de flujo y specs cortas, un lote por agente' },
    { title: 'Construir', detail: 'un lote por constructor, cada uno en su worktree' },
    { title: 'Integrar', detail: 'trae los commits y corre los gates de la tanda' },
    { title: 'Revisar', detail: 'revisión independiente de cada lote integrado' },
    { title: 'Reparar', detail: 'arreglos de la revisión, hasta el tope de rondas' },
    { title: 'Cerrar oleada', detail: 'gates completos, stack propio, seed, ensayo y documentación' },
    { title: 'PR', detail: 'un PR por oleada; nunca mergea' },
  ],
}

// Motor de la orquestación del backlog R11. Las reglas viven en la prompt
// (docs/IA/prompts/orquestacion-backlog-r11.md), en AGENTS.md y en las skills; el orden de trabajo,
// en el manifiesto. Este script solo ordena los pasos: no decide alcance ni prioridad.
// No frena por decisiones: cada agente elige la mejor opción, la anota y sigue; todo vuelve en el
// resultado para el informe final.

const A = args || {}
const M = A.manifiesto
if (!M || !Array.isArray(M.lotes)) throw new Error('Falta args.manifiesto: pasá el JSON del manifiesto como objeto, no como texto.')
if (!M.olas.includes(A.ola)) throw new Error(`args.ola tiene que ser una de ${M.olas.join(', ')}.`)
if (!['disenar', 'construir'].includes(A.fase)) throw new Error('args.fase tiene que ser "disenar" o "construir".')
if (!A.carpeta) throw new Error('Falta args.carpeta: la carpeta de trabajo de la corrida, fuera del repo.')

const OLA = A.ola
const CONDICIONES = A.condiciones || []
const TOPE = (M.topes && M.topes.constructoresEnParalelo) || 8
const RONDAS = (M.topes && M.topes.rondasDeReparacion) || 3

// Modelo y esfuerzo siempre explícitos: ningún agente hereda el modelo de la sesión.
const JUICIO = (M.modelos && M.modelos.juicio) || { model: 'opus', effort: 'medium' }
const MECANICO = (M.modelos && M.modelos.mecanico) || { model: 'sonnet', effort: 'high' }
const modelo = (l) => (l.rol === 'mecanico' ? MECANICO : JUICIO)

const indice = M.olas.indexOf(OLA)
const RAMA = `${M.ramas.prefijoOla}${OLA.toLowerCase()}`
const RAMA_PREVIA = indice === 0 ? M.ramas.base : `${M.ramas.prefijoOla}${M.olas[indice - 1].toLowerCase()}`
const DIR = `${A.carpeta}/${OLA}`

const lotes = M.lotes
  .filter((l) => l.ola === OLA)
  .filter((l) => !l.condicion || CONDICIONES.includes(l.condicion))
const omitidos = M.lotes.filter((l) => l.ola === OLA && l.condicion && !CONDICIONES.includes(l.condicion))
if (omitidos.length) log(`Lotes condicionales que no corren: ${omitidos.map((l) => `${l.id} (${l.condicion})`).join(', ')}.`)
const tandas = [...new Set(lotes.map((l) => l.tanda))].sort((a, b) => a - b)

const COMUN = `Trabajás en el repo DonaTrack. Seguí AGENTS.md (raíz) y, si tocás el cliente, cliente-liviano/AGENTS.md y docs/interfaz/README.md. El trabajo sale de ${M.backlog}: leé ahí tus ítems con su evidencia (archivo y línea) y las "Reglas generales nuevas" (sección 2), que valen para todo lo que hagas. Español llano en todo texto de pantalla (docs/interfaz/tono-y-comunicacion.md).
Reglas de esta corrida:
- No frenes a preguntar. Si falta una decisión, elegí la mejor opción con tu criterio, anotala en "decisiones" (qué elegiste, qué descartaste y por qué) y seguí.
- No estimes tiempos ni horas en ningún documento.
- Solo tus archivos. Si necesitás tocar otro, hacelo solo si es imprescindible y decilo en "fueraDeZona".
- No se renombran rutas, clases ni campos de la API por el cambio de "entidad" a "Organización": eso es solo texto visible y lo hace el lote p1-nombres.
- Nunca traigas una lista completa de un servicio: página, búsqueda y filtros en el servidor; los totales, como números.
- Una migración de base nueva lleva el número siguiente al último de su servicio; si choca con otra al integrar, la renumera el integrador.
- Nunca edites una línea por número sin leerla antes. No hagas push ni PR: eso lo hace el paso de PR.
- Commits con el prefijo "E6: backlog R11, ${OLA}, ". No uses --no-verify.
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
  properties: { veredicto: { type: 'string', enum: ['PASS', 'CHANGES_REQUIRED'] }, bloqueantes: lista, menores: lista },
  required: ['veredicto', 'bloqueantes', 'menores'],
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
const PEDIDO_PR = {
  type: 'object',
  properties: { url: texto, rama: texto, nota: texto },
  required: ['url', 'rama', 'nota'],
}

const ficha = (l) => `Lote ${l.id} (${l.items.join(', ')}): ${l.titulo}.
Qué hay que lograr: ${l.que}
Tus archivos: ${l.archivos}
Ruta: ${l.ruta}. Módulos: ${l.modulos.length ? l.modulos.join(', ') : '(ninguno de Maven)'}. Pantallas a comprobar: ${l.rutasVisuales.length ? l.rutasVisuales.join(', ') : '(ninguna)'}.`

const gatesDe = (ls) => {
  const modulos = [...new Set(ls.flatMap((l) => l.modulos))]
  const paginas = [...new Set(ls.flatMap((l) => l.rutasVisuales))]
  const maven = modulos.length ? `mvn clean test -pl ${modulos.join(',')} -am` : '(sin módulos de Maven)'
  const visual = paginas.length
    ? `rm -rf cliente-liviano/target/capturas; mvn test -pl cliente-liviano -Pvisual -Dtest=VerificacionVisualTest -Dsurefire.failIfNoSpecifiedTests=false -Dverificacion.paginas=${paginas.join(',')}`
    : '(sin verificación visual)'
  return { maven, visual }
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
  paraAprobar: [], ensayo: '', ensayoSuficiente: null, pr: '', detenida: '',
}
const terminar = (motivo) => {
  if (motivo) { resultado.detenida = motivo; log(motivo) }
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
4. Si un gate falla por un choque entre lotes (no por un defecto de un solo lote), arreglalo y comiteá "E6: backlog R11, ${OLA}, integración ${etiqueta}". Si falla por un lote solo, no lo arregles: decilo en detalle con el lote, el test y el mensaje.
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
log(`Oleada ${OLA}, fase ${A.fase}: ${lotes.length} lotes en ${tandas.length} tandas, rama ${RAMA}, baseline ${prep.baseline}.`)

// ── Fase "disenar": fichas y specs de toda la oleada, para aprobar de una vez ─────────────────
if (A.fase === 'disenar') {
  phase('Diseñar')
  if (!conDisenio.length) return terminar(`La oleada ${OLA} no tiene lotes con diseño previo: pasá directo a construir.`)
  const disenios = (await enParalelo(conDisenio.map((l) => () => agent(`${COMUN}

Sos el diseñador del lote. No escribís código: solo los documentos que se aprueban antes de construir.
${ficha(l)}
Documento pedido: ${l.disenio}.
Primero: git reset --hard ${base} (este worktree es tuyo).
${l.disenio.includes('ficha') ? `- Ficha de flujo: seguí la skill ux-flujo (.agents/skills/ux-flujo/SKILL.md) y la plantilla docs/interfaz/plantillas/flujo.md. Si la pantalla ya tiene ficha en docs/interfaz/flujos/, actualizala en lugar de crear otra; si no, creá docs/interfaz/flujos/r11-${l.id}.md. Recorrido con desvíos, cada estado con su texto, el patrón conocido que sigue (docs/interfaz/criterios-catedra-ux-ui.md) y la revisión con las 10 heurísticas. Las decisiones del usuario ya están tomadas en la sección 3 del backlog: no las reabras.` : ''}
${l.disenio.includes('spec') ? `- Spec corta: docs/specs/active/R11-${l.id}-spec.md, con el formato de la ruta STANDARD de la skill define-spec: alcance, fuera de alcance, contrato antes y después (rutas, parámetros, respuesta), criterios de aceptación verificables, compatibilidad con los clientes actuales y riesgos. Sin matriz 5D ni diseño de clases.` : ''}
Comiteá "E6: backlog R11, ${OLA}, diseño ${l.id}". Devolvé sha, documentos (rutas), resumen (lo que el usuario tiene que mirar, en 3 líneas y sin jerga), decisiones y dudas (cada una con la opción que recomendás).`, { label: `diseño ${l.id}`, phase: 'Diseñar', isolation: 'worktree', schema: DISENIO, ...JUICIO }).then((d) => d && { ...d, lote: l.id })))).filter(Boolean)

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
  return terminar('')
}

// ── Fase "construir" ─────────────────────────────────────────────────────────────────────────
const sinAprobar = new Set(prep.faltanDisenios)
const BASELINE = prep.baseline === 'RED' ? ` El baseline ya fallaba antes de empezar: leé ${DIR}/baseline.md y no lo cuentes como tuyo.` : ''

for (const t of tandas) {
  const etiqueta = `${OLA} tanda ${t}`
  const deLaTanda = lotes.filter((l) => l.tanda === t)
  const frenados = deLaTanda.filter((l) => sinAprobar.has(l.id))
  for (const l of frenados) resultado.pendientes.push(`${l.id}: no se construyó porque falta su ${l.disenio} aprobado en la rama`)
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
Cómo trabajar: skill implement-task (.agents/skills/implement-task/SKILL.md); para vistas, además, vista-ui (.agents/skills/vista-ui/SKILL.md): todos los estados, mejora progresiva, solo tokens semánticos y el catálogo de componentes. Test primero. Lo más simple que funcione.
Gates antes de comitear: ${g.maven} ; ${g.visual}
Comiteá "E6: backlog R11, ${OLA}, ${l.id}". Si no pudiste terminar algo, hecho=false y contalo en pendiente; igual comiteá lo que quedó bien y en verde.
Devolvé sha, hecho, resumen, archivos, gates (cuáles corriste y cómo dieron), decisiones, fueraDeZona, pendiente y paraElAdr (solo ruta CONTRATO).`, { label: `${l.id}`, phase: 'Construir', isolation: 'worktree', schema: ENTREGA, ...modelo(l) }).then((e) => e && { ...e, lote: l.id })
  }))).filter(Boolean)

  const mudos = aConstruir.filter((l) => !entregas.some((e) => e.lote === l.id))
  for (const l of mudos) resultado.pendientes.push(`${l.id}: el constructor no respondió`)
  if (!entregas.length) { log(`${etiqueta}: ningún constructor entregó.`); continue }
  for (const e of entregas) anotar(e.lote, e)

  // ── Integrar ───────────────────────────────────────────────────────────────────────────────
  let integracion = await integrar(entregas, aConstruir, etiqueta, base)
  if (!integracion) { resultado.pendientes.push(`${etiqueta}: el integrador no respondió; la tanda quedó sin integrar`); continue }
  for (const f of integracion.fallidas) resultado.pendientes.push(`${etiqueta}, sin integrar: ${f}`)
  base = integracion.sha
  let integrados = aConstruir.filter((l) => integracion.integrados.includes(l.id))

  // ── Revisar y reparar, hasta RONDAS ────────────────────────────────────────────────────────
  let aRevisar = integrados
  let gatesRotos = integracion.gatesOk ? '' : integracion.detalle
  for (let ronda = 1; ronda <= RONDAS && aRevisar.length; ronda++) {
    const desde = prep.base
    const revisiones = (await enParalelo(aRevisar.map((l) => () => agent(`Sos un revisor independiente y escéptico en el repo DonaTrack. Solo lectura: no modificás nada ni corrés nada que escriba. Revisás el lote ${l.id} ya integrado en la rama ${RAMA} (HEAD ${base}); su cambio está en los commits "E6: backlog R11, ${OLA}, ${l.id}" y sus arreglos (git log ${desde}..${base} --grep="${l.id}").
${ficha(l)}
${l.disenio !== 'ninguno' ? `Su diseño aprobado está en docs/interfaz/flujos/ o docs/specs/active/ (buscá ${l.id}): revisá contra eso.` : ''}
Cómo revisar: ${l.ruta === 'QUICK' ? 'LIGHTWEIGHT_CLOSING_CHECK de docs/IA/review/evaluator.md.' : 'skill implementation-review (.agents/skills/implementation-review/SKILL.md), vectores V1–V9.'}${l.rutasVisuales.length ? ' Además, skill revision-ui (.agents/skills/revision-ui/SKILL.md), vectores U1–U10, mirando las capturas de cliente-liviano/target/capturas con la herramienta de lectura de imágenes.' : ''}
Comprobá en particular las reglas generales de la sección 2 de ${M.backlog}: ninguna lista completa pedida a un servicio, el paginador único, las ventanas, lo que queda fijo y la cuenta visible con sesión.${gatesRotos ? ` Al integrar fallaron gates: ${gatesRotos}. Decí si la causa es de este lote.` : ''}
Bloqueante es solo lo que rompe un criterio de aceptación, un gate, un requerimiento del enunciado o una regla del repo. Lo demás va en menores. Cada hallazgo con archivo:línea y qué habría que cambiar.`, { label: `revisión ${l.id} r${ronda}`, phase: 'Revisar', schema: REVISION, ...JUICIO }).then((r) => r && { ...r, lote: l })))).filter(Boolean)

    const conCambios = revisiones.filter((r) => r.veredicto === 'CHANGES_REQUIRED' && r.bloqueantes.length)
    for (const r of revisiones.filter((x) => !conCambios.includes(x))) {
      for (const m of r.menores) resultado.pendientes.push(`${r.lote.id}, menor: ${m}`)
    }
    if (!conCambios.length) { aRevisar = []; break }
    if (ronda === RONDAS) {
      for (const r of conCambios) resultado.pendientes.push(`${r.lote.id}: quedó con bloqueantes tras ${RONDAS} rondas: ${r.bloqueantes.join(' | ')}`)
      log(`${etiqueta}: ${conCambios.map((r) => r.lote.id).join(', ')} quedaron con bloqueantes tras ${RONDAS} rondas.`)
      aRevisar = []
      break
    }

    const arreglos = (await enParalelo(conCambios.map((r) => () => {
      const g = gatesDe([r.lote])
      return agent(`${COMUN}

Arreglás los hallazgos bloqueantes de la revisión del lote ${r.lote.id}, ronda ${ronda}. ${RUTAS}
${ficha(r.lote)}
Hallazgos: ${r.bloqueantes.map((b, i) => `${i + 1}) ${b}`).join(' ')}
Primero: git reset --hard ${base} (este worktree es tuyo). Verificá cada hallazgo contra el archivo antes de tocar nada; si uno no es real, no lo arregles y decilo en decisiones con el porqué. Gates: ${g.maven} ; ${g.visual}
Comiteá "E6: backlog R11, ${OLA}, ${r.lote.id}, arreglos ronda ${ronda}". Devolvé los campos de siempre.`, { label: `arreglos ${r.lote.id} r${ronda}`, phase: 'Reparar', isolation: 'worktree', schema: ENTREGA, ...JUICIO }).then((e) => e && { ...e, lote: r.lote.id })
    }))).filter(Boolean)
    for (const e of arreglos) anotar(e.lote, e)
    if (!arreglos.length) { for (const r of conCambios) resultado.pendientes.push(`${r.lote.id}: el arreglo de la ronda ${ronda} no respondió`); break }

    const reintegrado = await integrar(arreglos, conCambios.map((r) => r.lote), `${etiqueta}, arreglos ronda ${ronda}`, base)
    if (!reintegrado) { resultado.pendientes.push(`${etiqueta}: el integrador de arreglos no respondió`); break }
    for (const f of reintegrado.fallidas) resultado.pendientes.push(`${etiqueta}, arreglo sin integrar: ${f}`)
    base = reintegrado.sha
    gatesRotos = reintegrado.gatesOk ? '' : reintegrado.detalle
    aRevisar = conCambios.map((r) => r.lote).filter((l) => reintegrado.integrados.includes(l.id))
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
const conEnsayo = hechos.some((l) => l.items.includes('R11-03') || l.items.includes('R11-B08'))
  || (OLA !== 'P0' && hechos.some((l) => l.modulos.includes('donaciones-service')))

const cierre = await agent(`${COMUN}

Cerrás la oleada ${OLA} en la rama ${RAMA} (HEAD ${base}). Lotes integrados: ${hechos.map((l) => `${l.id} (${l.items.join(', ')})`).join('; ')}.
1. Gates completos: ${gF.maven} ; ${gF.visual}. Si tocaron servicios, además los tests de integración que apliquen (docs/testing/integration-tests.md).
2. Stack propio de la corrida, sin tocar los datos del usuario:
   - Si hay un stack del usuario corriendo, detenelo con docker compose stop. Nunca docker compose down -v sobre ese stack: borra sus datos.
   - Levantá el de la corrida con otro nombre de proyecto: docker compose -p donatrack-r11 up -d --build. Si los archivos de compose fijan nombres de contenedor que chocan, resolvelo con un archivo de override dentro de ${A.carpeta}, no editando los compose del repo.
   - Seed: docs/testing/demo-seeder.md, con -p donatrack-r11. La clave de la demo sale del .env; no la escribas en ningún archivo ni en tu respuesta.
3. Recorrido con login real, una cuenta por rol de la tabla del seeder (persona donante, empresa donante, organización, administración): cada pantalla tocada en esta oleada carga, muestra datos del seed y cumple lo que dice su ítem del backlog. Anotá lo que no.
${conEnsayo ? `4. Ensayo de la importación: seguí docs/testing/ensayo-importacion-20000.md con el CSV de 20.000 filas. Medí cuánto tarda, si la página de avance se actualiza sola, y si las pantallas del admin, la portada y el inicio del donante responden mientras importa y después. ensayo = las mediciones; ensayoSuficiente según el criterio de la guía.` : `4. Sin ensayo en esta oleada: ensayo vacío y ensayoSuficiente=true.`}
5. Al terminar: docker compose -p donatrack-r11 down -v (solo ese proyecto) y, si detuviste el stack del usuario, docker compose start para devolverlo como estaba.
6. Si un gate falla por algo chico y evidente, arreglalo y comiteá "E6: backlog R11, ${OLA}, cierre". Lo que no sea chico va en bloqueantes, con el lote, la evidencia y la opción que recomendás.
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
Comiteá "E6: backlog R11, ${OLA}, arreglos del cierre". Devolvé los campos de siempre (sha = HEAD).`, { label: `arreglos del cierre ${OLA}`, phase: 'Cerrar oleada', schema: ENTREGA, ...JUICIO })
  if (arreglo) { anotar(`cierre ${OLA}`, arreglo); base = arreglo.sha || base }
}

const adrs = hechos.filter((l) => l.ruta === 'CONTRATO')
const docs = await agent(`${COMUN}

Escribís la documentación de cierre de la oleada ${OLA}, en este checkout, rama ${RAMA} (HEAD ${base}). Solo documentos.
Lotes integrados: ${hechos.map((l) => `${l.id} (${l.items.join(', ')}; ${l.ruta})`).join('; ')}.
1. ${M.backlog}: marcá el estado de cada ítem de la oleada (hecho, parcial con lo que falta, o no hecho con el motivo) y actualizá la sección 10 con lo que ya quedó al día.
2. docs/interfaz/bitacora.md: una entrada por decisión de diseño de esta oleada, con el formato de la bitácora y quién decidió (usuario, o agente cuando fue una decisión tomada sin consultar).
3. Lo que corresponda de: docs/interfaz/patrones-y-microcopy.md (reglas generales), componentes.md, mapa-del-sitio.md, tono-y-comunicacion.md, docs/requerimientos/requerimientos-e6.md (estado de RF-20, RF-AUTH-07 y los que toque) y docs/adr/DEUDA_TECNICA.md (deuda nueva de paginado y rendimiento si no existe; lo que quedó pendiente de esta oleada).
4. ADRs de los lotes de contrato: ${adrs.length ? adrs.map((l) => l.id).join(', ') : 'ninguno'}. Uno por decisión, con docs/adr/README.md, tomando la decisión, las alternativas y el porqué de la spec y del código integrado. Cada uno dice que se escribió después del código, como desvío acordado del ciclo ARCHITECTURAL. Mové las specs cerradas de docs/specs/active/ a docs/specs/completed/.
5. ${DIR}/decisiones.md (fuera del repo): las decisiones que los agentes tomaron sin consultar y lo que quedó pendiente, en lenguaje llano, para el informe final al usuario. Estas son las que juntó el workflow:
Decisiones: ${resultado.decisiones.join(' || ') || '(ninguna)'}
Pendientes: ${resultado.pendientes.join(' || ') || '(ninguno)'}
Comiteá "E6: backlog R11, ${OLA}, documentación". Devolvé sha, documentos, adrs y decisiones (la ruta del archivo del punto 5).`, { label: `documentación ${OLA}`, phase: 'Cerrar oleada', schema: DOCUMENTOS, ...JUICIO })
if (docs) base = docs.sha || base
else resultado.pendientes.push(`Documentación de cierre de ${OLA}: el agente no respondió`)
resultado.sha = base

// ── PR ───────────────────────────────────────────────────────────────────────────────────────
if (A.pr === false) return terminar('')
phase('PR')
const pr = await agent(`${COMUN}

Abrís el PR de la oleada ${OLA}. Esta es la única etapa que hace push.
1. git push -u origin ${RAMA}.
2. Destino: ${M.ramas.destino}. Si ya hay un PR abierto de ${RAMA}, actualizá su cuerpo en lugar de abrir otro. ${indice > 0 ? `Esta rama sale de ${RAMA_PREVIA}: si su PR todavía no se mergeó, decilo en la primera línea del cuerpo (este PR incluye sus commits).` : `Esta rama sale de ${M.ramas.base}: si ${M.ramas.base} todavía no está en ${M.ramas.destino}, decilo en la primera línea del cuerpo.`}
3. Cuerpo en ${DIR}/pr-body.md con la plantilla de docs/IA/04-checklist-antes-de-pr.md: qué se pidió (el backlog y la oleada), los ítems hechos con su id, los gates con su resultado, la verificación con el stack${conEnsayo ? ' y el ensayo de 20.000 filas' : ''}, las decisiones tomadas sin consultar (${DIR}/decisiones.md) y lo que queda pendiente. Sin estimaciones de tiempo. Última línea: 🤖 Generated with [Claude Code](https://claude.com/claude-code)
4. gh pr create --base ${M.ramas.destino} --head ${RAMA} --title "E6: backlog R11, oleada ${OLA}" --body-file ${DIR}/pr-body.md. No lo mergees ni actives el merge automático: lo revisa y lo mergea el usuario.
Devolvé url, rama y nota (lo que el usuario tiene que saber antes de revisar).`, { label: `PR ${OLA}`, phase: 'PR', schema: PEDIDO_PR, ...MECANICO })
if (pr) resultado.pr = pr.url
else resultado.pendientes.push(`PR de ${OLA}: el agente no respondió; la rama ${RAMA} quedó local`)

return terminar('')
