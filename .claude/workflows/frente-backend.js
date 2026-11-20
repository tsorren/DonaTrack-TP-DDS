export const meta = {
  name: 'frente-backend',
  description: 'Implementa un frente de backend a partir de un manifiesto frente-backend/1: diseño con control humano, constructores en paralelo por tanda, integración, auditoría por tarea con preauditor y auditor, cierre, documentación y PR en una corrida aparte',
  whenToUse: 'Con un manifiesto frente-backend/1 (primer uso: docs/specs/active/SPEC-11-graphql.manifiesto.json). args: { manifiesto (el JSON como objeto), fase: "disenar"|"construir"|"pr", carpeta (la misma en las tres corridas), disenoAprobado? (SHA, en construir), stackDeLaRama?: boolean, condiciones?: string[] }. Guía: docs/IA/prompts/orquestacion-frente-backend.md',
  phases: [
    { title: 'Preparar', detail: 'rama de trabajo, estado de la corrida y baseline' },
    { title: 'Diseñar', detail: 'los lotes de diseño; después frena para el control humano' },
    { title: 'Construir', detail: 'un lote por constructor, cada uno en su worktree' },
    { title: 'Integrar', detail: 'trae los commits y corre los gates de la tanda' },
    { title: 'Auditar', detail: 'preauditor y auditor por tarea, sobre sus commits' },
    { title: 'Reparar', detail: 'arreglos de la auditoría, hasta el tope de rondas' },
    { title: 'Cerrar', detail: 'documentación, auditoría de cierre y gates de cierre' },
    { title: 'PR', detail: 'solo en la fase pr: push y PR borrador; nunca mergea' },
  ],
}

// Motor de un frente de backend. El orden del trabajo sale del manifiesto; este script solo lo
// ejecuta. Qué manda durante la corrida, las fases y las reglas de falla están en la guía
// (docs/IA/prompts/orquestacion-frente-backend.md). El script no lee ni escribe archivos: el estado
// entre corridas (<carpeta>/estado.json) lo leen y lo escriben agentes mecánicos, y el script
// comprueba que lo que vuelve es lo que mandó. Ante la duda, frena: nunca reconstruye sobre un
// estado que no pudo leer ni sigue sobre una rama que no pudo dejar en verde.

// ── Regla frente-backend/1 ──
function problemasDelManifiesto(M) {
  const p = []
  const txt = (v) => typeof v === 'string' && v.trim() !== ''
  const textos = (v) => Array.isArray(v) && v.every(txt)
  if (!M || typeof M !== 'object' || Array.isArray(M)) return ['el manifiesto no es un objeto']
  if (M.formato !== 'frente-backend/1') p.push(`formato tiene que ser "frente-backend/1" (vino ${JSON.stringify(M.formato)})`)
  if (!txt(M.nombre) || !/^[a-z0-9-]+$/.test(M.nombre)) p.push('nombre: solo minúsculas, números y guiones')
  for (const k of ['spec', 'guia', 'prefijoCommit']) if (!txt(M[k])) p.push(`falta ${k}`)
  if (!M.ramas || !txt(M.ramas.base) || !txt(M.ramas.trabajo) || !txt(M.ramas.destino)) p.push('ramas necesita base, trabajo y destino')
  if (!M.topes || !Number.isInteger(M.topes.constructoresEnParalelo) || !Number.isInteger(M.topes.rondasDeReparacion) || M.topes.constructoresEnParalelo < 1 || M.topes.rondasDeReparacion < 1) p.push('topes necesita constructoresEnParalelo y rondasDeReparacion, enteros de 1 o más')
  for (const k of ['juicio', 'mecanico', 'preauditor', 'auditor']) if (!M.modelos || !M.modelos[k] || !txt(M.modelos[k].model)) p.push(`modelos.${k} necesita model`)
  const duras = M.precedencia && Array.isArray(M.precedencia.reglasDuras) ? M.precedencia.reglasDuras : (p.push('precedencia.reglasDuras: una lista'), [])
  for (const r of duras) if (!r || !txt(r.id) || !txt(r.texto) || !['tarea', 'cierre'].includes(r.seAudita)) p.push('cada regla dura lleva id, texto y seAudita (tarea o cierre)')
  if (!M.precedencia || !txt(M.precedencia.ok) || !textos(M.precedencia.flexibles)) p.push('precedencia necesita ok y flexibles (una lista de textos)')
  if (!textos(M.reglas)) p.push('reglas: una lista de textos (puede estar vacía)')
  const tareas = Array.isArray(M.tareas) && M.tareas.length ? M.tareas : (p.push('tareas: una lista con al menos una'), [])
  const orden = []
  for (const t of tareas) {
    if (!t || !txt(t.id) || !txt(t.nombre)) { p.push('cada tarea lleva id y nombre'); continue }
    if (orden.includes(t.id)) p.push(`tarea ${t.id}: id repetido`)
    orden.push(t.id)
  }
  const lotes = Array.isArray(M.lotes) ? M.lotes : (p.push('lotes: una lista'), [])
  const ids = new Set()
  for (const l of lotes) {
    if (!l || !txt(l.id)) { p.push('hay un lote sin id'); continue }
    const n = `lote ${l.id}`
    if (ids.has(l.id)) p.push(`${n}: id repetido`)
    ids.add(l.id)
    if (!orden.includes(l.tarea)) p.push(`${n}: su tarea no está en tareas`)
    if (!['disenar', 'construir'].includes(l.fase)) p.push(`${n}: fase tiene que ser disenar o construir`)
    if (!Number.isInteger(l.tanda)) p.push(`${n}: tanda tiene que ser un entero`)
    if (!['juicio', 'mecanico'].includes(l.rol)) p.push(`${n}: rol tiene que ser juicio o mecanico`)
    if (!textos(l.modulos)) p.push(`${n}: modulos tiene que ser una lista de textos (puede estar vacía)`)
    if (!textos(l.archivos) || !l.archivos.length) p.push(`${n}: archivos tiene que ser una lista de globs, con al menos uno`)
    for (const k of ['titulo', 'que']) if (!txt(l[k])) p.push(`${n}: falta ${k}`)
  }
  // Dos globs pueden nombrar el mismo archivo solo si el tramo fijo de uno (hasta su primer
  // comodín: * ? [ {) es prefijo del tramo fijo del otro; una ruta sin comodín también puede ser
  // una carpeta. Puede dar un solapamiento de más; no deja pasar uno real.
  const fijo = (g) => g.split(/[*?[{]/)[0]
  const literal = (g) => fijo(g) === g
  const dentro = (a, b) => a === b || a.startsWith(b.endsWith('/') ? b : `${b}/`)
  const seSolapan = (a, b) => (literal(a) && literal(b) ? dentro(a, b) || dentro(b, a) : fijo(a).startsWith(fijo(b)) || fijo(b).startsWith(fijo(a)))
  const validos = lotes.filter((l) => l && txt(l.id) && Number.isInteger(l.tanda) && textos(l.archivos))
  for (let i = 0; i < validos.length; i++) {
    for (let j = i + 1; j < validos.length; j++) {
      const a = validos[i]
      const b = validos[j]
      if (a.fase !== b.fase) continue
      if (a.tanda === b.tanda) {
        const par = a.archivos.flatMap((x) => b.archivos.filter((y) => seSolapan(x, y)).map((y) => `${x} y ${y}`))[0]
        if (par) p.push(`lotes ${a.id} y ${b.id}: comparten tanda y sus zonas se solapan (${par})`)
      }
      const [antes, despues] = orden.indexOf(a.tarea) < orden.indexOf(b.tarea) ? [a, b] : [b, a]
      if (antes.tarea !== despues.tarea && orden.includes(antes.tarea) && orden.includes(despues.tarea) && antes.tanda > despues.tanda) {
        p.push(`lote ${antes.id}: su tanda es posterior a la del lote ${despues.id}, de una tarea que viene después`)
      }
    }
  }
  const dids = new Set()
  for (const d of Array.isArray(M.decisiones) ? M.decisiones : (p.push('decisiones: una lista (puede estar vacía)'), [])) {
    if (!d || !txt(d.id)) { p.push('hay una decisión sin id'); continue }
    const n = `decisión ${d.id}`
    if (dids.has(d.id)) p.push(`${n}: id repetido`)
    dids.add(d.id)
    for (const k of ['texto', 'quien', 'ok']) if (!txt(d[k])) p.push(`${n}: falta ${k}`)
    const enLote = d.lote !== undefined
    const aCargo = d.aCargoDe !== undefined
    if (enLote === aCargo) {
      p.push(`${n}: ${enLote ? 'tiene lote y aCargoDe a la vez' : 'sin destino: va a un lote con su criterio comprobable, o queda aCargoDe alguien con quien y ok'}`)
    } else if (enLote) {
      if (!ids.has(d.lote)) p.push(`${n}: el lote ${d.lote} no existe`)
      if (!txt(d.criterio)) p.push(`${n}: falta el criterio comprobable`)
    } else if (!d.aCargoDe || !txt(d.aCargoDe.quien) || !txt(d.aCargoDe.ok)) {
      p.push(`${n}: aCargoDe necesita quien y ok`)
    }
  }
  const g = M.gates || {}
  if (!textos(g.porLote) || !textos(g.cierre)) p.push('gates necesita porLote y cierre, listas de comandos')
  if (!Array.isArray(g.siHayStack) || !g.siHayStack.every((x) => x && txt(x.gate) && txt(x.comando))) p.push('gates.siHayStack: una lista de gate y comando (puede estar vacía)')
  if (!Array.isArray(g.diferidos) || !g.diferidos.every((x) => x && txt(x.gate) && txt(x.a))) p.push('gates.diferidos: una lista de gate y a (puede estar vacía)')
  if (!M.documentacion || !textos(M.documentacion.archivos) || !txt(M.documentacion.que)) p.push('documentacion necesita archivos (lista) y que')
  // El título va entre comillas en un comando: sin comillas, acentos graves, $ ni barras invertidas.
  if (!M.pr || !txt(M.pr.titulo) || /["`$\\]/.test(M.pr.titulo) || !txt(M.pr.plantilla)) p.push('pr necesita plantilla y un titulo sin comillas dobles, acentos graves, $ ni barras invertidas')
  return p
}
// ── fin de la regla frente-backend/1 ──

const A = args || {}
const M = A.manifiesto
if (!M || typeof M !== 'object') throw new Error('Falta args.manifiesto: pasá el JSON del manifiesto como objeto, no como texto.')
const PROBLEMAS = problemasDelManifiesto(M)
if (PROBLEMAS.length) throw new Error(`El manifiesto no cumple frente-backend/1; no arranca:\n- ${PROBLEMAS.join('\n- ')}`)
if (!['disenar', 'construir', 'pr'].includes(A.fase)) throw new Error('args.fase tiene que ser "disenar", "construir" o "pr".')
if (!A.carpeta) throw new Error('Falta args.carpeta: la carpeta de trabajo de la corrida, fuera del repo y la misma en las tres fases.')

const CONDICIONES = A.condiciones || []
const lotes = M.lotes.filter((l) => !l.condicion || CONDICIONES.includes(l.condicion))
const omitidos = M.lotes.filter((l) => !lotes.includes(l))
const deDiseno = lotes.filter((l) => l.fase === 'disenar')
const deConstruir = lotes.filter((l) => l.fase === 'construir')
if (A.fase === 'construir' && deDiseno.length && !A.disenoAprobado) {
  throw new Error('Falta args.disenoAprobado: el SHA del diseño que aprobó el dueño del frente en el primer control.')
}

const RAMA = M.ramas.trabajo
const TOPE = M.topes.constructoresEnParalelo
const RONDAS = M.topes.rondasDeReparacion
const ESTADO = `${A.carpeta}/estado.json`
// Modelo y esfuerzo siempre explícitos: ningún agente hereda el modelo de la sesión.
const JUICIO = M.modelos.juicio
const MECANICO = M.modelos.mecanico
const modelo = (l) => (l.rol === 'mecanico' ? MECANICO : JUICIO)
// preauditor y auditor son agentes de la sesión del usuario. Si el tipo no existe acá (agent()
// lanza o no devuelve nada), corre un agente genérico con el mismo encargo y el mismo modelo.
const conTipo = async (prompt, opts, rol) => {
  const { agentType, ...resto } = rol
  if (agentType) {
    try {
      const r = await agent(prompt, { ...opts, ...resto, agentType })
      if (r) return r
    } catch (e) { /* el tipo no está en la sesión: sigue con el genérico */ }
    log(`El agente "${agentType}" no respondió o no está en esta sesión; corre uno genérico con el mismo encargo.`)
  }
  return agent(prompt, { ...opts, ...resto })
}

const ORDEN = M.tareas.map((t) => t.id)
const TAREAS = ORDEN.filter((t) => lotes.some((l) => l.tarea === t))
const lotesDe = (t, ls) => ls.filter((l) => l.tarea === t)
const decisionesDe = (ls) => M.decisiones.filter((d) => ls.some((l) => l.id === d.lote))
const prefijoDe = (t) => `${M.prefijoCommit}${t}, `
const archivosDe = (ls) => [...new Set(ls.flatMap((l) => l.archivos))]
// Un lote sin módulos no corre Maven. En gates.porLote, "<módulos del lote>" se reemplaza.
const gatesDe = (ls) => {
  const modulos = [...new Set(ls.flatMap((l) => l.modulos))]
  return modulos.length ? M.gates.porLote.map((g) => g.replace('<módulos del lote>', modulos.join(','))).join(' ; ') : ''
}
const lineas = (xs) => xs.map((x) => `- ${x}`).join('\n')
const durasDe = (ambito) => M.precedencia.reglasDuras.filter((r) => ambito === 'cierre' || r.seAudita === 'tarea')
const escaparRegex = (s) => s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')

// Los SHA se comparan entre corridas: van completos, y la comparación tolera uno abreviado. Un SHA
// que devuelve un agente solo se usa si tiene forma de SHA.
const SHA = 'Todo SHA que devuelvas va completo (git rev-parse HEAD), sin texto alrededor.'
const esSha = (s) => typeof s === 'string' && /^[0-9a-f]{7,40}$/.test(s.trim())
const shaO = (s, alternativa) => (esSha(s) ? s.trim() : alternativa)
const mismoSha = (a, b) => esSha(a) && esSha(b) && (a.trim().startsWith(b.trim()) || b.trim().startsWith(a.trim()))
// El documento de contrato lo deja la fase disenar: recién en construir es un encargo aprobado.
const HAY_CONTRATO = A.fase === 'construir' && Boolean(M.contrato)

const COMUN = `Trabajás en el repo DonaTrack, en una corrida del workflow frente-backend (frente ${M.nombre}). Leé la spec ${M.spec} y la guía ${M.guia}; el manifiesto te llega resumido acá.
Qué manda en esta corrida, por decisión del dueño del frente (${M.precedencia.ok}):
1. Las reglas duras. No se pisan nunca:
${lineas(M.precedencia.reglasDuras.map((r) => r.texto))}
2. El manifiesto: tus decisiones, las reglas de la corrida y la zona de archivos de tu lote.
3. La guía.
4. AGENTS.md y el resto del repo, como orientación. Si chocan con el manifiesto, gana el manifiesto: anotá el choque en "conflictos" (la regla, qué hiciste y por qué) y seguí. Lo que queda flexible:
${lineas(M.precedencia.flexibles)}
Reglas de la corrida:
${lineas(M.reglas)}
- Commits con el prefijo "${M.prefijoCommit}<tarea>, ", nombrando los archivos: nunca "git add -A" ni "--no-verify". Sin push.
- Nunca edites una línea por número sin leerla antes.
- ${SHA}
- Tu resultado trae cuatro listas: decisiones (lo que elegiste sin consultar, con lo que descartaste y por qué), conflictos, fueraDeZona y pendientes (lo que tiene que resolver una persona).
La carpeta de la corrida, fuera del repo, es ${A.carpeta}.`
const CONTRATO = HAY_CONTRATO ? `\nEl diseño aprobado está en ${M.contrato}: es tu encargo y manda sobre este resumen.` : ''

const texto = { type: 'string' }
const si = { type: 'boolean' }
const lista = { type: 'array', items: texto }
const PREPARACION = {
  type: 'object',
  properties: { ok: si, motivo: texto, base: texto, baseline: texto, hayEstado: si, estado: texto, disenoEnRama: si, tareasVigentes: lista, lotesEnRama: lista },
  required: ['ok', 'motivo', 'base', 'baseline', 'hayEstado', 'estado', 'disenoEnRama', 'tareasVigentes', 'lotesEnRama'],
}
const ENTREGA = {
  type: 'object',
  properties: { sha: texto, hecho: si, resumen: texto, archivos: lista, gates: texto, tocaCitasOCifras: si, decisiones: lista, conflictos: lista, fueraDeZona: lista, pendientes: lista },
  required: ['sha', 'hecho', 'resumen', 'archivos', 'gates', 'tocaCitasOCifras', 'decisiones', 'conflictos', 'fueraDeZona', 'pendientes'],
}
const INTEGRACION = {
  type: 'object',
  properties: { sha: texto, integrados: lista, fallidas: lista, gatesOk: si, culpables: lista, detalle: texto },
  required: ['sha', 'integrados', 'fallidas', 'gatesOk', 'culpables', 'detalle'],
}
const VERIFICACION = {
  type: 'object',
  properties: { sha: texto, gatesOk: si, culpables: lista, detalle: texto },
  required: ['sha', 'gatesOk', 'culpables', 'detalle'],
}
const PREAUDITORIA = {
  type: 'object',
  properties: { sha: texto, corregido: lista, dudoso: lista },
  required: ['sha', 'corregido', 'dudoso'],
}
const AUDITORIA = {
  type: 'object',
  properties: {
    shaAuditado: texto, veredicto: { type: 'string', enum: ['PASS', 'CHANGES_REQUIRED'] },
    decisiones: lista, bloqueantes: lista, menores: lista, contrato: texto,
  },
  required: ['shaAuditado', 'veredicto', 'decisiones', 'bloqueantes', 'menores', 'contrato'],
}
const CIERRE = {
  type: 'object',
  properties: { sha: texto, gatesOk: si, gates: texto, disenoIgual: si, diferidos: lista, informe: texto },
  required: ['sha', 'gatesOk', 'gates', 'disenoIgual', 'diferidos', 'informe'],
}
const DOCUMENTOS = {
  type: 'object',
  properties: { sha: texto, documentos: lista, decisiones: texto },
  required: ['sha', 'documentos', 'decisiones'],
}
const GUARDADO = { type: 'object', properties: { ok: si, contenido: texto }, required: ['ok', 'contenido'] }
const HECHO = { type: 'object', properties: { ok: si, detalle: texto }, required: ['ok', 'detalle'] }
const PEDIDO_PR = { type: 'object', properties: { url: texto, rama: texto, nota: texto }, required: ['url', 'rama', 'nota'] }

const ficha = (l) => {
  const ds = decisionesDe([l])
  return `Lote ${l.id} (tarea ${l.tarea}): ${l.titulo}.
Qué hay que lograr: ${l.que}
Tu zona de archivos: ${l.archivos.join(' ; ')}
Módulos de Maven: ${l.modulos.length ? l.modulos.join(', ') : '(ninguno: este lote no corre Maven)'}.
Decisiones que este lote cumple (criterios de aceptación; no se reabren): ${ds.length ? ds.map((d) => `${d.id}: ${d.texto} (decidió: ${d.quien}; consta en ${d.ok}). Se comprueba así: ${d.criterio}`).join(' | ') : '(ninguna)'}`
}

// Corre los thunks de a TOPE por vez, en el orden dado.
const enParalelo = async (thunks) => {
  const salida = []
  for (let i = 0; i < thunks.length; i += TOPE) salida.push(...(await parallel(thunks.slice(i, i + TOPE))))
  return salida
}

// ── Estado entre corridas y resultado ────────────────────────────────────────────────────────
// El archivo es una línea de JSON con un campo "control": el largo del resto. Así un estado
// truncado o resumido por el agente que lo transcribe no se toma por bueno.
const nuevoEstado = () => ({
  formato: 'frente-backend/1', nombre: M.nombre, shaDiseno: '', shaBaseConstruir: '', lotes: [], tareas: {}, decisiones: {},
  reporte: { decisiones: [], conflictos: [], fueraDeZona: [], pendientes: [] }, cierre: null, shaFinal: '',
})
const serializar = (e) => JSON.stringify({ ...e, control: JSON.stringify(e).length })
const leerEstado = (txt) => {
  let e
  try { e = JSON.parse(txt) } catch (x) { return null }
  if (!e || typeof e !== 'object' || Array.isArray(e)) return null
  const { control, ...cuerpo } = e
  if (control !== JSON.stringify(cuerpo).length) return null
  if (cuerpo.formato !== 'frente-backend/1' || cuerpo.nombre !== M.nombre || !cuerpo.tareas || !cuerpo.reporte || !cuerpo.decisiones || !Array.isArray(cuerpo.lotes)) return null
  return cuerpo
}
let estado = nuevoEstado()
const resultado = {
  frente: M.nombre, fase: A.fase, rama: RAMA, sha: '', tareas: [], decisionesDelUsuario: [],
  decisiones: [], conflictos: [], fueraDeZona: [], pendientes: [], paraAprobar: [], cierre: '', pr: '', siguiente: '', detenida: '',
}
// Guarda el estado y comprueba que lo que quedó en el disco es lo que mandó. Dos intentos; si no,
// corta la corrida: seguir sin estado haría que la corrida siguiente reconstruya lo ya integrado.
const guardar = async (fase) => {
  const linea = serializar(estado)
  for (let intento = 1; intento <= 2; intento++) {
    const g = await agent(`Guardás el estado de una corrida del workflow frente-backend. No tocás el repo. mkdir -p ${A.carpeta}.${intento === 1 ? ` Si ya existe ${ESTADO}, primero copialo a ${ESTADO}.anterior.` : ''} Después escribí en ${ESTADO} exactamente esta línea de JSON, sin cambiar ni un carácter:
${linea}
Por último leé el archivo del disco (cat ${ESTADO}) y devolvé en "contenido" lo que leíste, tal cual, sin resumir.`, { label: `estado ${fase}`, phase: fase, schema: GUARDADO, ...MECANICO })
    const leido = g && leerEstado(g.contenido)
    if (leido && JSON.stringify(leido) === JSON.stringify(estado)) return
  }
  // Se corta con un error, pero antes queda a la vista lo que la corrida sabía.
  rendir()
  log(`Reporte antes de cortar. Tareas: ${resultado.tareas.join('; ')}. Lotes integrados: ${estado.lotes.join(', ') || 'ninguno'}. Pendientes: ${resultado.pendientes.join(' || ') || 'ninguno'}.`)
  throw new Error(`No se pudo guardar el estado en ${ESTADO} (dos intentos). La corrida se corta acá y la rama ${RAMA} queda como está. El último estado bueno, si había, está en ${ESTADO}.anterior: revisá los dos archivos antes de volver a lanzar.`)
}
// Los Review Contract van aparte del estado: son texto largo y no se comparan.
const guardarContratos = async (fase, contratos) => {
  if (!contratos.length) return
  const g = await agent(`Guardás los Review Contract de una auditoría del workflow frente-backend. No tocás el repo: escribís en ${A.carpeta}. Cada archivo lleva el texto que está entre su línea de inicio y su línea de fin, tal cual.
${contratos.map((c) => `<<<INICIO ${c.ruta}>>>\n${c.contenido}\n<<<FIN ${c.ruta}>>>`).join('\n')}
Devolvé ok y, si algo falló, el detalle.`, { label: `contratos ${fase}`, phase: fase, schema: HECHO, ...MECANICO })
  if (!g || !g.ok) estado.reporte.pendientes.push(`No se guardaron los Review Contract en ${A.carpeta}: ${g ? g.detalle : 'el agente no respondió'}`)
}
const anotar = (id, e) => {
  for (const k of ['decisiones', 'conflictos', 'fueraDeZona', 'pendientes']) {
    for (const x of (e && e[k]) || []) estado.reporte[k].push(`${id}: ${x}`)
  }
}
// Cada decisión, por ID, con su estado; las que no van a un lote, con quién las tiene a cargo.
const rendir = () => {
  resultado.tareas = TAREAS.map((t) => `${t}: ${estado.tareas[t] ? estado.tareas[t].veredicto : 'SIN_CONSTRUIR'}${estado.tareas[t] && estado.tareas[t].motivo ? ` (${estado.tareas[t].motivo})` : ''}`)
  resultado.decisionesDelUsuario = M.decisiones.map((d) => (d.lote
    ? `${d.id} (lote ${d.lote}): ${estado.decisiones[d.id] || 'pendiente: su tarea todavía no se auditó'} — ${d.texto}`
    : `${d.id}: a cargo de ${d.aCargoDe.quien} (${d.aCargoDe.ok}) — ${d.texto}`))
  for (const k of ['decisiones', 'conflictos', 'fueraDeZona', 'pendientes']) resultado[k] = [...new Set([...estado.reporte[k], ...resultado[k]])]
}
const terminar = (motivo) => {
  if (motivo) { resultado.detenida = motivo; log(motivo) }
  rendir()
  return resultado
}

// ── Integrar ─────────────────────────────────────────────────────────────────────────────────
const verificar = (ls, etiqueta, cuando) => agent(`Corrés los gates de ${etiqueta} en el repo DonaTrack, rama ${RAMA}, ${cuando}. No arreglás ni comiteás nada: solo corrés y contás.
Gates: ${gatesDe(ls)}
Si alguno falla: gatesOk=false; en culpables, los ids de lote que lo causan, entre ${ls.map((l) => l.id).join(', ')} (mirá qué test falla y qué commit tocó ese código con git log --stat; vacío solo si no se puede saber); y en detalle, el test y el mensaje.
${SHA} Devolvé sha (HEAD), gatesOk, culpables y detalle.`, { label: `gates ${etiqueta} ${cuando}`, phase: 'Integrar', schema: VERIFICACION, ...MECANICO })

// Devuelve { sha, ids } con los lotes que quedaron integrados y en verde, null si el integrador no
// respondió, o { fatal } si la rama pudo quedar en rojo y hay que mirarla a mano.
// Gates en rojo: un agente de arreglos, una vez, y se vuelven a correr. Si siguen rojos, se
// revierten los lotes que los causan (o todos los de la tanda si no se sabe cuáles) y se corren
// otra vez sobre lo que queda.
const integrar = async (entregas, ls, etiqueta, base) => {
  const entregados = ls.filter((l) => entregas.some((e) => e.lote === l.id))
  const gates = gatesDe(entregados)
  const i = await agent(`${COMUN}

Sos el integrador de ${etiqueta}. En este checkout, en la rama ${RAMA} (HEAD tiene que ser ${base}; si no, frená y decilo en detalle):
1. git cherry-pick de cada commit, en este orden: ${entregas.map((e) => `${e.lote} = ${e.sha}`).join('; ')}. Si un conflicto es de agregados en un archivo compartido (una propiedad más, una fila más de una tabla), quedate con los dos lados. Si es otra cosa, git cherry-pick --abort de ese commit y anotalo en fallidas con el lote y el motivo.
2. Gates de lo integrado: ${gates || '(ninguno: estos lotes no corren Maven)'}
3. No arregles nada. Si un gate falla, gatesOk=false, en culpables los ids de lote que lo causan (mirá qué test falla y qué commit tocó ese código; vacío solo si no se puede saber) y en detalle el test y el mensaje.
Devolvé sha (HEAD), integrados (ids de lote), fallidas, gatesOk, culpables y detalle.`, { label: `integrar ${etiqueta}`, phase: 'Integrar', schema: INTEGRACION, ...MECANICO })
  if (!i) return null
  let ids = entregados.map((l) => l.id).filter((id) => i.integrados.includes(id))
  let sha = shaO(i.sha, base)
  for (const f of i.fallidas) estado.reporte.pendientes.push(`${etiqueta}, sin integrar: ${f}`)
  if (i.gatesOk || !ids.length || !gates) return { sha, ids }

  const arreglo = await agent(`${COMUN}${CONTRATO}

Los gates de ${etiqueta} dieron rojo después de integrar, en la rama ${RAMA}: ${i.detalle}
Lotes integrados: ${ids.join(', ')}. Arreglá la causa en este checkout, sin debilitar ningún test. Gates: ${gates}
Comiteá "${M.prefijoCommit}integración ${etiqueta}" nombrando los archivos. Devolvé los campos de siempre (sha = HEAD).`, { label: `arreglo de gates ${etiqueta}`, phase: 'Integrar', schema: ENTREGA, ...JUICIO })
  anotar(`integración ${etiqueta}`, arreglo)
  sha = shaO(arreglo && arreglo.sha, sha)
  const enRojo = `${etiqueta}: los gates quedaron en rojo y no se pudo dejar la rama ${RAMA} en verde. Revisala a mano antes de volver a lanzar.`
  const quedan = (xs) => entregados.filter((l) => xs.includes(l.id))
  const v = await verificar(quedan(ids), etiqueta, 'después del arreglo')
  if (!v) return { fatal: enRojo }
  if (v.gatesOk) return { sha: shaO(v.sha, sha), ids }

  const senalados = (v.culpables.length ? v.culpables : i.culpables).filter((id) => ids.includes(id))
  const aRevertir = senalados.length ? senalados : ids
  const r = await agent(`Revertís lotes que dejaron los gates en rojo, en el repo DonaTrack, rama ${RAMA}. Con git revert --no-edit, sacá todos los commits de ${base}..HEAD que sean de estos lotes: ${aRevertir.join(', ')} (el id del lote está en el asunto del commit) y el del arreglo "${M.prefijoCommit}integración ${etiqueta}", si existe. No uses reset ni push. Devolvé ok y, en detalle, solo el SHA completo de HEAD (git rev-parse HEAD).`, { label: `revertir ${etiqueta}`, phase: 'Integrar', schema: HECHO, ...MECANICO })
  if (!r || !r.ok || !esSha(r.detalle)) return { fatal: enRojo }
  sha = r.detalle.trim()
  estado.reporte.pendientes.push(`${etiqueta}: gates en rojo después del arreglo; se revirtieron ${aRevertir.join(', ')}${senalados.length ? '' : ' (todos los de la tanda: no se pudo saber cuál los causaba)'}`)
  ids = ids.filter((id) => !aRevertir.includes(id))
  if (ids.length) {
    const v2 = await verificar(quedan(ids), etiqueta, 'después de revertir')
    if (!v2 || !v2.gatesOk) return { fatal: enRojo }
    sha = shaO(v2.sha, sha)
  }
  return { sha, ids }
}

// ── Auditoría de una tarea (o del cierre) ────────────────────────────────────────────────────
// Cada decisión se rinde en una línea "<ID>: CUMPLE — evidencia" o "<ID>: NO CUMPLE — evidencia".
// Con líneas contradictorias gana NO CUMPLE; una decisión sin rendir deja la auditoría sin PASS.
const rendida = (r, d) => {
  const patron = new RegExp('^\\s*' + escaparRegex(d.id) + '\\s*:\\s*(NO CUMPLE|CUMPLE)\\s*(?:—|–|-|$)', 'i')
  const estados = r.decisiones.map((x) => x.match(patron)).filter(Boolean).map((m) => m[1].toUpperCase())
  return estados.includes('NO CUMPLE') ? 'NO CUMPLE' : estados.length ? 'CUMPLE' : ''
}
const conDecisionesRendidas = (r, ds) => {
  const noCumple = ds.filter((d) => rendida(r, d) === 'NO CUMPLE').map((d) => `${d.id}: NO CUMPLE — criterio: ${d.criterio}`)
  const sinRendir = ds.filter((d) => rendida(r, d) === '').map((d) => d.id)
  if (!noCumple.length && !sinRendir.length) return r
  return { ...r, veredicto: 'CHANGES_REQUIRED', bloqueantes: [...r.bloqueantes, ...noCumple], sinRendir }
}

// id: la tarea o "cierre". alcance(desde): qué commits mira. head: el HEAD conocido al empezar.
// sinArreglos: una sola ronda, sin preauditor ni arreglos (el diseño que cambió el dueño).
// Devuelve veredicto, auditado (el último SHA que miró el auditor), head, rondas, motivo y contratos.
const auditar = async ({ id, alcance, ls, ambito, desde, head, sinArreglos }) => {
  const ds = decisionesDe(ls)
  const gates = gatesDe(ls)
  const contratos = []
  let auditado = ''
  let previos = ''
  let pasaPreauditor = !sinArreglos
  let motivo = ''
  for (let ronda = 1; ronda <= RONDAS; ronda++) {
    const mira = auditado ? `solo lo nuevo desde ${auditado}: ${alcance(auditado)}` : alcance(desde)
    if (pasaPreauditor) {
      const pre = await conTipo(`Preauditoría de ${id} en el repo DonaTrack, rama ${RAMA}, ronda ${ronda}. Mirá ${mira}
Chequeá lo mecánico: citas, rutas, nombres de clases y métodos, cifras, links y consistencia entre el código, los tests y los documentos tocados. Corregí directamente lo trivial y comiteá "${M.prefijoCommit}${id}, arreglos de la pre-auditoría" nombrando los archivos (nunca git add -A; sin push). No cambies decisiones ni alcance: lo de fondo va en dudoso. ${SHA} Devolvé sha (HEAD), corregido y dudoso.`, { label: `preauditoría ${id} r${ronda}`, phase: 'Auditar', schema: PREAUDITORIA }, M.modelos.preauditor)
      if (pre) {
        head = shaO(pre.sha, head)
        for (const x of pre.dudoso) estado.reporte.pendientes.push(`${id}, preauditoría: ${x}`)
      }
    }
    const a = await conTipo(`Auditoría de fondo de ${id} en el repo DonaTrack, rama ${RAMA}, ronda ${ronda}. Solo lectura: no modificás nada. Mode INDEPENDENT_REVIEW. Mirá ${mira}
Spec: ${M.spec}. Guía de la corrida: ${M.guia}.${HAY_CONTRATO ? ` Diseño aprobado: ${M.contrato}.` : ''}
${ls.length ? `Lotes que se auditan:\n${ls.map(ficha).join('\n')}` : 'Se audita el cierre: la documentación y los commits que no son de ninguna tarea.'}
Emití en "contrato" el Review Contract de docs/IA/review/evaluator.md (V1 a V9) y anotá en shaAuditado el HEAD que miraste, completo (git rev-parse HEAD).
Decisiones: en "decisiones", una línea por ID con este formato exacto: "<ID>: CUMPLE — <evidencia>" o "<ID>: NO CUMPLE — <evidencia>" (archivo:línea); una decisión sin su línea cuenta como no comprobada. IDs a rendir: ${ds.map((d) => d.id).join(', ') || '(ninguno)'}.
Qué bloquea, y nada más:
${lineas([...durasDe(ambito).map((r) => `regla dura: ${r.texto}`), 'una decisión NO CUMPLE', 'el "Qué hay que lograr" de un lote, sin cumplir', ...(gates ? [`estos gates en rojo: ${gates}`] : [])])}
En esta corrida manda el manifiesto: un hallazgo que solo se apoya en una regla flexible del repo va en menores, nunca en bloqueantes. Flexibles: ${M.precedencia.flexibles.join(' | ')}${previos ? `\nHallazgos de la ronda anterior, a verificar: ${previos}` : ''}
Con un bloqueante, CHANGES_REQUIRED. Cada hallazgo con archivo:línea y la corrección concreta.`, { label: `auditoría ${id} r${ronda}`, phase: 'Auditar', schema: AUDITORIA }, M.modelos.auditor)
    const r = a && conDecisionesRendidas(a, ds)
    if (r) {
      auditado = shaO(r.shaAuditado, head)
      head = auditado
      contratos.push({ ruta: `${A.carpeta}/auditoria-${id}-r${ronda}.md`, contenido: r.contrato })
    }
    if (r && r.veredicto === 'PASS' && !r.bloqueantes.length) {
      for (const m of r.menores) estado.reporte.pendientes.push(`${id}, menor: ${m}`)
      for (const d of ds) estado.decisiones[d.id] = `cumplida (PASS de la auditoría de ${id}, ronda ${ronda})`
      return { veredicto: 'PASS', auditado, head, rondas: ronda, motivo: '', contratos }
    }
    motivo = !r ? 'el auditor no respondió' : [...r.bloqueantes, ...(r.sinRendir || []).map((x) => `${x} sin rendir`)].join(' | ') || 'CHANGES_REQUIRED sin bloqueantes'
    if (ronda === RONDAS || sinArreglos) {
      for (const d of ds) estado.decisiones[d.id] = `no cumplida: ${id} quedó sin PASS tras ${ronda} ronda(s)`
      return { veredicto: 'CHANGES_REQUIRED', auditado, head, rondas: ronda, motivo, contratos }
    }
    previos = motivo
    pasaPreauditor = false
    // Un auditor mudo o sin bloqueantes concretos se vuelve a auditar; no hay nada que arreglar.
    if (!r || !r.bloqueantes.length) continue
    const arreglo = await agent(`${COMUN}${CONTRATO}

Arreglás los hallazgos bloqueantes de la auditoría de ${id}, ronda ${ronda}, en este checkout, rama ${RAMA}. Las auditorías van en serie: nadie más toca la rama ahora.
${ls.map(ficha).join('\n')}
Hallazgos: ${r.bloqueantes.map((b, n) => `${n + 1}) ${b}`).join(' ')}
Verificá cada hallazgo contra el archivo antes de tocar nada; si uno no es real, no lo arregles y decilo en decisiones con el porqué. Gates: ${gates || '(ninguno)'}
Comiteá "${M.prefijoCommit}${id}, arreglos ronda ${ronda}" nombrando los archivos. tocaCitasOCifras=true si cambiaste citas, rutas, nombres o cifras de un documento. Devolvé los campos de siempre (sha = HEAD).`, { label: `arreglos ${id} r${ronda}`, phase: 'Reparar', schema: ENTREGA, ...JUICIO })
    anotar(`${id}, arreglos ronda ${ronda}`, arreglo)
    head = shaO(arreglo && arreglo.sha, head)
    pasaPreauditor = Boolean(arreglo && arreglo.tocaCitasOCifras)
  }
  return { veredicto: 'CHANGES_REQUIRED', auditado, head, rondas: RONDAS, motivo, contratos }
}
// Los commits de una tarea se reconocen por el asunto: su prefijo, más los arreglos de integración
// de las tandas (o del diseño) donde tiene lotes.
const commitsDeLaTarea = (t, etiquetas) => (desde) => `los commits de la tarea ${t} y los arreglos de integración de sus tandas: git log -E --format="%H %s" ${desde}..HEAD --grep="^${escaparRegex(prefijoDe(t))}"${etiquetas.map((e) => ` --grep="^${escaparRegex(`${M.prefijoCommit}integración ${e}`)}$"`).join('')}`
const auditarTarea = async (t, ls, etiquetas, desde, head) => {
  const a = await auditar({ id: t, alcance: commitsDeLaTarea(t, etiquetas), ls, ambito: 'tarea', desde, head })
  estado.tareas[t] = { veredicto: a.veredicto, shaAuditado: a.auditado, rondas: a.rondas, motivo: a.motivo }
  await guardar('Auditar')
  await guardarContratos('Auditar', a.contratos)
  log(`Tarea ${t}: ${a.veredicto}${a.motivo ? ` (${a.motivo})` : ''}.`)
  return a
}

// ── Preparar ─────────────────────────────────────────────────────────────────────────────────
phase('Preparar')
if (omitidos.length) log(`Lotes condicionales que no corren: ${omitidos.map((l) => `${l.id} (${l.condicion})`).join(', ')}.`)
const baselineDe = gatesDe(deConstruir)
const prep = await agent(`Preparás una corrida del workflow frente-backend (frente ${M.nombre}, fase ${A.fase}) en el repo DonaTrack. No modificás archivos del repo.
1. git status: si hay cambios sin comitear en archivos versionados, ok=false y el motivo (no los toques ni los guardes en stash). Los archivos sin versionar que ya estaban no cuentan.
2. git switch ${RAMA}. Si la rama no existe, ok=false: el workflow no la crea.
3. mkdir -p ${A.carpeta}. Si existe ${ESTADO}: hayEstado=true y en "estado" su contenido, tal cual, sin resumir, reformatear ni envolverlo en nada. Si no existe: hayEstado=false y "estado" vacío.
4. base = el SHA completo de HEAD (git rev-parse HEAD), sin texto alrededor.
${A.fase === 'construir'
    ? `5. Baseline: ${baselineDe || '(sin Maven)'}. baseline = GREEN o RED; si es RED, dejá el detalle en ${A.carpeta}/baseline.md y seguí con ok=true.
6. disenoEnRama = true si ${A.disenoAprobado || '(no hay diseño)'} existe y es ancestro de HEAD (git merge-base --is-ancestor)${deDiseno.length ? `, y además "git diff --quiet ${A.disenoAprobado} HEAD -- ${archivosDe(deDiseno).join(' ')}" no muestra cambios` : ''}.
7. tareasVigentes: de las tareas que el estado guardado da en PASS, los ids que cumplen las dos cosas: su shaAuditado sigue siendo ancestro de HEAD, y no hay commits posteriores con su prefijo (git log -E <shaAuditado>..HEAD --grep="^${escaparRegex(M.prefijoCommit)}<id>, " no devuelve nada).
8. lotesEnRama: de estos ids de lote, ${M.lotes.map((l) => l.id).join(', ')}, los que están hoy en la rama: tienen un commit con ese id en el asunto (git log --format="%H %s" ${M.ramas.base}..HEAD) que no fue revertido después. Un asunto que empieza con "Revert" no cuenta, y un lote cuyo commit tiene su Revert tampoco.`
    : '5. baseline = "no corrida", disenoEnRama = false, tareasVigentes y lotesEnRama vacíos.'}
Devolvé ok, motivo, base, baseline, hayEstado, estado, disenoEnRama, tareasVigentes y lotesEnRama.`, { label: 'preparar', phase: 'Preparar', schema: PREPARACION, ...MECANICO })

if (!prep) return terminar('Detenida en Preparar: el agente no respondió.')
if (!prep.ok) return terminar(`No arrancó: ${prep.motivo}`)
if (!esSha(prep.base)) return terminar(`No arrancó: Preparar no devolvió el SHA de HEAD (vino "${prep.base}").`)
if (prep.hayEstado) {
  // Un agente puede envolver el JSON en un bloque de código: se le saca antes de leerlo.
  const guardado = leerEstado(prep.estado.trim().replace(/^```[a-z]*\s*/, '').replace(/\s*```$/, ''))
  if (!guardado) return terminar(`No arrancó: ${ESTADO} existe pero no se pudo leer entero (está truncado, cambiado o es de otro frente). No se empieza de cero sobre una rama con trabajo: revisá el archivo.`)
  estado = guardado
}
let base = prep.base.trim()
resultado.sha = base
for (const d of decisionesDe(omitidos)) estado.decisiones[d.id] = `no cumplida: el lote ${d.lote} no corrió (condición ${omitidos.find((l) => l.id === d.lote).condicion})`
log(`Frente ${M.nombre}, fase ${A.fase}: rama ${RAMA} en ${base}, baseline ${prep.baseline}.`)

// ── Fase "pr": solo si todo quedó en PASS y nadie tocó la rama después del cierre ─────────────
if (A.fase === 'pr') {
  const sinPass = TAREAS.filter((t) => !estado.tareas[t] || estado.tareas[t].veredicto !== 'PASS')
  if (sinPass.length) return terminar(`No se abre el PR: tareas sin PASS: ${sinPass.join(', ')}.`)
  if (!estado.cierre || estado.cierre.veredicto !== 'PASS') return terminar(`No se abre el PR: el cierre no está en PASS${estado.cierre && estado.cierre.motivo ? ` (${estado.cierre.motivo})` : ''}.`)
  if (!mismoSha(base, estado.shaFinal)) return terminar(`No se abre el PR: HEAD (${base}) no es el commit que cerró la fase construir (${estado.shaFinal || 'ninguno'}). Volvé a lanzar construir.`)
  rendir()
  phase('PR')
  const pr = await agent(`${COMUN}

Abrís el PR del frente ${M.nombre}. Esta es la única etapa que hace push.
1. git push -u origin ${RAMA}. Nunca a ${M.ramas.destino}.
2. Si ya hay un PR abierto de ${RAMA}, actualizá su cuerpo en lugar de abrir otro (nunca mientras corre su CI).
3. Cuerpo en ${A.carpeta}/pr-body.md con la plantilla de ${M.pr.plantilla}, con las etiquetas de AGENTS.md §3: qué se pidió (${M.spec}), las tareas con su veredicto (${resultado.tareas.join('; ')}), los gates del cierre (${estado.cierre.gates}), lo diferido (${M.gates.diferidos.map((d) => `${d.gate}: ${d.a}`).join('; ') || 'nada'}) y lo que queda pendiente. Sin estimaciones de tiempo.
   - Sección "Decisiones": una fila por ID con su estado, tal cual: ${resultado.decisionesDelUsuario.join(' || ')}
   - Sección "Reglas del repo flexibilizadas": cada conflicto anotado: ${resultado.conflictos.join(' || ') || '(ninguno)'}
   - Sección "Decisiones que tomaron los agentes" y "Fuera de zona": ${A.carpeta}/decisiones.md
   - Sección "Pendientes de una persona": ${[...(M.pendientesDeUnaPersona || []), ...resultado.pendientes].join(' || ') || '(ninguno)'}
   - Última línea: 🤖 Generated with [Claude Code](https://claude.com/claude-code)
4. gh pr create ${M.pr.borrador === false ? '' : '--draft '}--base ${M.ramas.destino} --head ${RAMA} --title "${M.pr.titulo}" --body-file ${A.carpeta}/pr-body.md. No lo mergees ni actives el merge automático.
Devolvé url, rama y nota (lo que el dueño tiene que saber antes de pedir revisión).`, { label: 'PR', phase: 'PR', schema: PEDIDO_PR, ...MECANICO })
  if (pr) resultado.pr = pr.url
  else resultado.pendientes.push(`PR: el agente no respondió; la rama ${RAMA} quedó local`)
  return terminar('')
}

// ── Fase "disenar": los lotes de diseño, su auditoría y el freno para el primer control ───────
if (A.fase === 'disenar') {
  phase('Diseñar')
  if (!deDiseno.length) return terminar('Este frente no tiene lotes de diseño: pasá directo a construir.')
  const desde = base
  const disenios = (await enParalelo(deDiseno.map((l) => () => agent(`${COMUN}

Sos el diseñador del lote. No escribís código de producción: solo lo que el dueño aprueba antes de construir.
${ficha(l)}
Primero: git reset --hard ${base} (este worktree es tuyo).
Comiteá "${prefijoDe(l.tarea)}${l.id}" nombrando tus archivos. Si no pudiste terminar algo, hecho=false y contalo en pendientes. En resumen, lo que el dueño tiene que mirar, en tres líneas y sin jerga; cada pregunta abierta, en decisiones, con la opción que elegiste y la que descartaste. Devolvé los campos de siempre.`, { label: `diseño ${l.id}`, phase: 'Diseñar', isolation: 'worktree', schema: ENTREGA, ...modelo(l) }).then((e) => e && { ...e, lote: l.id })))).filter(Boolean)
  if (!disenios.length) return terminar('Detenida en Diseñar: ningún diseñador respondió.')
  for (const e of disenios) anotar(e.lote, e)
  const integrado = await integrar(disenios, deDiseno, 'el diseño', base)
  if (!integrado) return terminar('Detenida al integrar el diseño: el integrador no respondió.')
  if (integrado.fatal) return terminar(integrado.fatal)
  base = integrado.sha
  resultado.sha = base
  estado.lotes = [...new Set([...estado.lotes, ...integrado.ids])]
  await guardar('Integrar')
  for (const t of TAREAS.filter((x) => lotesDe(x, deDiseno).length)) {
    const ls = lotesDe(t, deDiseno)
    if (ls.some((l) => !integrado.ids.includes(l.id))) {
      estado.tareas[t] = { veredicto: 'CHANGES_REQUIRED', shaAuditado: '', rondas: 0, motivo: 'un lote de diseño no se integró' }
      continue
    }
    const a = await auditarTarea(t, ls, ['el diseño'], desde, base)
    base = a.head
  }
  estado.shaDiseno = base
  resultado.sha = base
  for (const d of decisionesDe(deConstruir)) estado.decisiones[d.id] = estado.decisiones[d.id] || 'pendiente: se comprueba en la fase construir'
  await guardar('Auditar')
  for (const e of disenios) resultado.paraAprobar.push({ lote: e.lote, archivos: e.archivos, resumen: e.resumen, decisiones: e.decisiones })
  const disenoSinPass = TAREAS.filter((t) => lotesDe(t, deDiseno).length && estado.tareas[t].veredicto !== 'PASS')
  if (disenoSinPass.length) return terminar(`El diseño quedó sin PASS (${disenoSinPass.map((t) => `${t}: ${estado.tareas[t].motivo}`).join('; ')}). Está en ${base}, para que el dueño lo revise.`)
  resultado.siguiente = `Primer control: el dueño aprueba el diseño de ${base} y lanza construir con disenoAprobado.`
  return terminar('')
}

// ── Fase "construir" ─────────────────────────────────────────────────────────────────────────
// El estado dice qué lotes ya están integrados. Si alguno no aparece en la rama, no se adivina.
const perdidos = estado.lotes.filter((id) => !prep.lotesEnRama.includes(id))
if (perdidos.length) return terminar(`No arrancó: el estado dice que ${perdidos.join(', ')} ya se integraron, pero no aparecen en la rama ${RAMA}. Revisá la rama y ${ESTADO} a mano.`)
// Y al revés: un lote que ya está en la rama y no figura en el estado (una corrida que murió entre
// integrar y guardar) no se construye de nuevo encima de sus propios commits.
const sobrantes = prep.lotesEnRama.filter((id) => M.lotes.some((l) => l.id === id) && !estado.lotes.includes(id))
if (sobrantes.length) return terminar(`No arrancó: ${sobrantes.join(', ')} ya están en la rama ${RAMA}, pero el estado no los tiene como integrados. Revisá la rama y ${ESTADO} a mano.`)

const tareasDeDiseno = TAREAS.filter((t) => lotesDe(t, deDiseno).length)
if (deDiseno.length) {
  if (!prep.disenoEnRama) return terminar(`No arrancó: el diseño aprobado (${A.disenoAprobado}) no está en la rama ${RAMA}, o sus archivos cambiaron después.`)
  if (!mismoSha(A.disenoAprobado, estado.shaDiseno)) {
    // El dueño cambió el diseño en el primer control: se vuelve a auditar, una ronda y sin arreglos.
    phase('Auditar')
    for (const t of tareasDeDiseno) {
      const a = await auditar({ id: t, alcance: (d) => `los commits de ${d}..${A.disenoAprobado} y el diseño tal como quedó en ${A.disenoAprobado}`, ls: lotesDe(t, deDiseno), ambito: 'tarea', desde: estado.shaDiseno || M.ramas.base, head: base, sinArreglos: true })
      estado.tareas[t] = { veredicto: a.veredicto, shaAuditado: a.auditado, rondas: a.rondas, motivo: a.motivo }
      await guardarContratos('Auditar', a.contratos)
    }
    estado.shaDiseno = A.disenoAprobado
    estado.lotes = [...new Set([...estado.lotes, ...deDiseno.map((l) => l.id)])]
    await guardar('Auditar')
  }
  const sinPass = tareasDeDiseno.filter((t) => !estado.tareas[t] || estado.tareas[t].veredicto !== 'PASS')
  if (sinPass.length) {
    return terminar(`No arrancó: el diseño no está en PASS (${sinPass.map((t) => `${t}: ${(estado.tareas[t] && estado.tareas[t].motivo) || 'sin auditar'}`).join('; ')}). Lo que decidió el dueño no lo arregla un agente.`)
  }
}

const BASELINE = prep.baseline === 'RED' ? ` El baseline ya fallaba antes de empezar: leé ${A.carpeta}/baseline.md y no lo cuentes como tuyo.` : ''
// Al reanudar, las auditorías siguen mirando desde donde empezó la primera corrida de construir.
estado.shaBaseConstruir = estado.shaBaseConstruir || base
const desdeConstruir = estado.shaBaseConstruir
// Reanudar: una tarea en PASS, con su SHA auditado en la rama y sin commits suyos posteriores, no
// se rehace. De las demás no se reconstruyen los lotes ya integrados: se auditan otra vez.
const vigentes = new Set(prep.tareasVigentes.filter((t) => estado.tareas[t] && estado.tareas[t].veredicto === 'PASS'))
// Una tarea rota (un lote sin integrar o sin PASS) frena las tandas posteriores de ella y de las
// tareas que vienen después. Dentro de la misma tanda, cada tarea sigue por su cuenta.
const rotaEnTanda = {}
const frenada = (t, tanda) => ORDEN.some((r) => rotaEnTanda[r] !== undefined && rotaEnTanda[r] < tanda && ORDEN.indexOf(r) <= ORDEN.indexOf(t))
const romper = (t, tanda, motivo) => {
  if (rotaEnTanda[t] === undefined) rotaEnTanda[t] = tanda
  estado.tareas[t] = { veredicto: 'CHANGES_REQUIRED', shaAuditado: (estado.tareas[t] && estado.tareas[t].shaAuditado) || '', rondas: (estado.tareas[t] && estado.tareas[t].rondas) || 0, motivo }
  for (const d of decisionesDe(lotesDe(t, deConstruir))) estado.decisiones[d.id] = `no cumplida: ${motivo}`
}
const tandas = [...new Set(deConstruir.map((l) => l.tanda))].sort((a, b) => a - b)
const etiquetasDe = (t) => [...new Set(lotesDe(t, deConstruir).map((l) => `tanda ${l.tanda}`))]

for (const tanda of tandas) {
  const etiqueta = `tanda ${tanda}`
  const deLaTanda = deConstruir.filter((l) => l.tanda === tanda && !vigentes.has(l.tarea))
  const frenados = deLaTanda.filter((l) => frenada(l.tarea, tanda))
  if (frenados.length) {
    log(`${etiqueta}: no corren ${frenados.map((l) => l.id).join(', ')} porque una tarea anterior quedó sin PASS.`)
    // La tarea que se rompió en esta corrida conserva su motivo; las que arrastra, llevan este.
    for (const t of new Set(frenados.map((l) => l.tarea))) if (rotaEnTanda[t] === undefined) romper(t, tanda, 'en esta corrida no se construyó ni se auditó: una tarea anterior quedó sin PASS')
  }
  const corren = deLaTanda.filter((l) => !frenados.includes(l))
  const aConstruir = corren.filter((l) => !estado.lotes.includes(l.id))
  if (!corren.length) continue

  if (aConstruir.length) {
    phase('Construir')
    const entregas = (await enParalelo(aConstruir.map((l) => () => {
      const otros = aConstruir.filter((o) => o !== l).map((o) => `${o.id} (${o.archivos.join(' ; ')})`).join(' | ')
      return agent(`${COMUN}${CONTRATO}

Sos el constructor del lote.
${ficha(l)}
En paralelo trabajan otros lotes; no toques sus zonas: ${otros || '(ninguno)'}.
Primero: git reset --hard ${base} (este worktree es tuyo).${BASELINE}
Test primero, después el código. Gates antes de comitear: ${gatesDe([l]) || '(ninguno)'}
Comiteá "${prefijoDe(l.tarea)}${l.id}" nombrando tus archivos. Si no pudiste terminar algo, hecho=false y contalo en pendientes; igual comiteá lo que quedó bien y en verde.
Devolvé sha, hecho, resumen, archivos, gates (cuáles corriste y cómo dieron), tocaCitasOCifras, decisiones, conflictos, fueraDeZona y pendientes.`, { label: `${l.id}`, phase: 'Construir', isolation: 'worktree', schema: ENTREGA, ...modelo(l) }).then((e) => e && { ...e, lote: l.id })
    }))).filter(Boolean)
    for (const e of entregas) anotar(e.lote, e)
    for (const l of aConstruir.filter((x) => !entregas.some((e) => e.lote === x.id))) estado.reporte.pendientes.push(`${l.id}: el constructor no respondió`)
    const integrado = entregas.length ? await integrar(entregas, aConstruir, etiqueta, base) : null
    if (integrado && integrado.fatal) {
      await guardar('Integrar')
      return terminar(integrado.fatal)
    }
    if (entregas.length && !integrado) estado.reporte.pendientes.push(`${etiqueta}: el integrador no respondió; la tanda quedó sin integrar`)
    if (integrado) {
      base = integrado.sha
      estado.lotes = [...new Set([...estado.lotes, ...integrado.ids])]
    }
    for (const l of aConstruir.filter((x) => !estado.lotes.includes(x.id))) romper(l.tarea, tanda, `el lote ${l.id} no se integró`)
    // Se guarda apenas se integra: si la corrida muere después, estos lotes no se reconstruyen.
    await guardar('Integrar')
  }
  resultado.sha = base

  // Auditar, en serie y en el orden del manifiesto, cada tarea cuya última tanda es esta.
  const cierran = TAREAS.filter((t) => corren.some((l) => l.tarea === t) && Math.max(...lotesDe(t, deConstruir).map((l) => l.tanda)) === tanda)
  for (const t of cierran) {
    if (rotaEnTanda[t] !== undefined) continue
    phase('Auditar')
    const a = await auditarTarea(t, lotesDe(t, deConstruir), etiquetasDe(t), desdeConstruir, base)
    base = a.head
    if (a.veredicto !== 'PASS') {
      romper(t, tanda, `quedó sin PASS tras ${a.rondas} ronda(s): ${a.motivo}`)
      await guardar('Auditar')
    }
  }
  resultado.sha = base
  log(`${etiqueta}: lotes integrados hasta ahora: ${estado.lotes.join(', ') || 'ninguno'}.`)
}

if (!deConstruir.some((l) => estado.lotes.includes(l.id))) {
  await guardar('Cerrar')
  return terminar('La fase construir no integró ningún lote.')
}

// ── Cerrar: documentación, auditoría de cierre y, al final, los gates sobre el último commit ──
phase('Cerrar')
const hechos = deConstruir.filter((l) => estado.lotes.includes(l.id))
rendir()
const docs = await agent(`${COMUN}

Escribís la documentación de cierre del frente ${M.nombre}, en este checkout, rama ${RAMA} (HEAD ${base}). Solo documentos.
Tu zona: ${M.documentacion.archivos.join(' ; ')}
Qué hay que lograr: ${M.documentacion.que}
Lotes integrados: ${hechos.map((l) => `${l.id} (${l.tarea})`).join('; ')}. Tareas: ${resultado.tareas.join('; ')}.
Además, fuera del repo, escribí ${A.carpeta}/decisiones.md en lenguaje llano:
Decisiones por ID: ${resultado.decisionesDelUsuario.join(' || ')}
Decisiones que tomaron los agentes sin consultar: ${resultado.decisiones.join(' || ') || '(ninguna)'}
Reglas del repo flexibilizadas: ${resultado.conflictos.join(' || ') || '(ninguna)'}
Fuera de zona: ${resultado.fueraDeZona.join(' || ') || '(nada)'}
Pendientes: ${resultado.pendientes.join(' || ') || '(ninguno)'}
Comiteá "${M.prefijoCommit}documentación" nombrando los archivos. Devolvé sha (HEAD), documentos y decisiones (la ruta del archivo).`, { label: 'documentación', phase: 'Cerrar', schema: DOCUMENTOS, ...JUICIO })
if (docs) base = shaO(docs.sha, base)
else estado.reporte.pendientes.push('Documentación de cierre: el agente no respondió')

const prefijos = ORDEN.map((t) => escaparRegex(prefijoDe(t))).join('|')
const auditoriaDeCierre = await auditar({
  id: 'cierre', ls: [], ambito: 'cierre', desde: desdeConstruir, head: base,
  alcance: (d) => `la documentación de cierre y los commits que no son de ninguna tarea: git log -E --format="%H %s" ${d}..HEAD --invert-grep --grep="^(${prefijos})"`,
})
base = auditoriaDeCierre.head
await guardarContratos('Cerrar', auditoriaDeCierre.contratos)

// Los gates corren al final, sobre el commit que la fase pr va a empujar.
const conStack = A.stackDeLaRama === true ? M.gates.siHayStack : []
const cierre = await agent(`Cerrás la fase construir del frente ${M.nombre} en el repo DonaTrack, rama ${RAMA}. No arreglás ni comiteás nada: solo corrés y contás. Este es el commit que se va a empujar.
1. Gates de cierre, todos: ${M.gates.cierre.join(' ; ')}
2. ${conStack.length ? `Los servicios de esta rama están levantados: corré además ${conStack.map((g) => `${g.gate}: ${g.comando}`).join(' ; ')}` : `Sin stack de la rama: no corras ${M.gates.siHayStack.map((g) => g.gate).join(', ') || 'nada más'}; declaralo en diferidos como ${M.gates.sinDocker || '[DEFERRED_NO_DOCKER]'}.`}
3. Diferidos del manifiesto, tal cual: ${M.gates.diferidos.map((d) => `${d.gate}, a ${d.a}`).join('; ') || '(ninguno)'}.
4. disenoIgual = ${deDiseno.length ? `true si "git diff --quiet ${A.disenoAprobado} HEAD -- ${archivosDe(deDiseno).join(' ')}" no muestra cambios (el diseño no cambia después del primer control)` : 'true (este frente no tiene diseño)'}.
Escribí el informe en ${A.carpeta}/cierre.md, con cada comando y su resultado. ${SHA} Devolvé sha (HEAD), gatesOk (true solo si todos los de los puntos 1 y 2 dieron verde), gates (cada uno con su resultado), disenoIgual, diferidos e informe (la ruta).`, { label: 'cierre', phase: 'Cerrar', schema: CIERRE, ...MECANICO })
if (!cierre) estado.reporte.pendientes.push('Cierre: el agente no respondió; los gates de cierre no corrieron')
else base = shaO(cierre.sha, base)

const motivosDelCierre = [
  ...(auditoriaDeCierre.veredicto !== 'PASS' ? [`auditoría de cierre sin PASS: ${auditoriaDeCierre.motivo}`] : []),
  ...(!cierre ? ['los gates de cierre no corrieron'] : []),
  ...(cierre && !cierre.gatesOk ? [`gates en rojo: ${cierre.gates}`] : []),
  ...(cierre && !cierre.disenoIgual ? ['el diseño cambió después del primer control'] : []),
]
estado.cierre = { veredicto: motivosDelCierre.length ? 'CHANGES_REQUIRED' : 'PASS', gates: cierre ? cierre.gates : '', diferidos: cierre ? cierre.diferidos : [], motivo: motivosDelCierre.join('; ') }
estado.shaFinal = base
resultado.sha = base
resultado.cierre = `${estado.cierre.veredicto}${estado.cierre.motivo ? `: ${estado.cierre.motivo}` : ''}`
await guardar('Cerrar')

const sinPass = TAREAS.filter((t) => !estado.tareas[t] || estado.tareas[t].veredicto !== 'PASS')
if (sinPass.length || motivosDelCierre.length) {
  return terminar(`Construir terminó con pendientes: ${[...sinPass.map((t) => `${t} sin PASS`), ...motivosDelCierre].join('; ')}. La fase pr no va a correr.`)
}
resultado.siguiente = `Segundo control: el dueño revisa la rama en ${base} y lanza la fase pr.`
return terminar('')
