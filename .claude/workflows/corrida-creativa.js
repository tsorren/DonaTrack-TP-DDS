export const meta = {
  name: 'corrida-creativa',
  description: 'Diseño creativo desatendido del cliente liviano a partir de un encargo aprobado: iteraciones que escalan con constructores en paralelo, críticos, arreglos, evidencia, auditoría y PR',
  whenToUse: 'Con un encargo creativo aprobado (skill relevamiento-creativo). args: { encargo, carpeta, iteraciones?, conceptos?, pr?, inicio?, auditoria? }. Detalle: .agents/skills/corrida-creativa/SKILL.md',
  phases: [
    { title: 'Preparar', detail: 'lee el encargo, baseline y capturas de antes' },
    { title: 'Encargo', detail: 'el director elige las partes y llena las plantillas' },
    { title: 'Construir', detail: 'una parte por constructor, cada uno en su worktree' },
    { title: 'Integrar', detail: 'trae los commits y corre la verificación conjunta' },
    { title: 'Criticar', detail: 'arte, piso y flujo, sin diluir la audacia' },
    { title: 'Decidir', detail: 'matriz, ganador y arreglos por parte' },
    { title: 'Arreglar', detail: 'los arreglos de cada parte, conservando la idea' },
    { title: 'Cerrar iteración', detail: 'gates, evidencia, bitácora, commit y nota de traspaso' },
    { title: 'Cierre', detail: 'página de antes y después y documentación' },
    { title: 'Auditoría', detail: 'pre-auditor y qa-gatekeeper, hasta dos rondas' },
    { title: 'PR', detail: 'solo si el encargo lo autoriza' },
  ],
}

// Motor de la skill corrida-creativa (.agents/skills/corrida-creativa/SKILL.md): las reglas viven
// en la skill, en la dirección de arte y en las plantillas de docs/interfaz/plantillas/; este
// script solo ordena los pasos. Los agentes leen y escriben archivos; el script no acumula
// contexto.

const A = args || {}
const SKILL = '.agents/skills/corrida-creativa/SKILL.md'
const PLANTILLAS = 'docs/interfaz/plantillas'
const TIPOS = ['concepto', 'sistema', 'flujo']
const LENTES = ['arte', 'piso', 'flujo']
const TOPE_POR_PASO = 5

// Modelo y esfuerzo de cada rol (tabla de la skill, sección 1).
const DISENO = { model: 'opus', effort: 'high' }
const ARREGLO = { model: 'opus', effort: 'medium' }
const CRITICA = { model: 'opus', effort: 'medium' }
const MECANICO = { model: 'sonnet', effort: 'high' }

const COMUN = `Trabajás en el repo DonaTrack, cliente liviano (cliente-liviano/). Seguí AGENTS.md (raíz) y cliente-liviano/AGENTS.md. Español llano. Rutas con barra normal; los comandos, en Git Bash. Nunca edites una línea por número sin leerla antes. No hagas push. Antes de cada verificación visual, rm -rf cliente-liviano/target/capturas: la verificación no la limpia y quedarían capturas de corridas anteriores. La carpeta de trabajo de la corrida (fuera del repo) es: ${A.carpeta}`

const texto = { type: 'string' }
const si = { type: 'boolean' }
const lista = { type: 'array', items: texto }

const PREPARACION = {
  type: 'object',
  properties: {
    ok: si, motivo: texto, alternativa: texto, nombre: texto, ancla: texto,
    iteraciones: { type: 'integer' }, conceptos: { type: 'integer' }, pr: si,
    rama: texto, ramaBase: texto, base: texto, brief: texto, voz: texto,
    referencias: texto, intocables: texto, antes: texto, ruido: texto, baseline: texto,
  },
  required: ['ok', 'motivo', 'alternativa', 'nombre', 'ancla', 'iteraciones', 'conceptos', 'pr', 'rama', 'ramaBase', 'base', 'intocables', 'antes', 'ruido', 'baseline'],
}
const PLAN = {
  type: 'object',
  properties: {
    bloqueo: texto, base: texto, rutas: texto, variantes: lista,
    partes: {
      type: 'array',
      items: {
        type: 'object',
        properties: { nombre: texto, idea: texto, archivos: texto },
        required: ['nombre', 'idea', 'archivos'],
      },
    },
  },
  required: ['bloqueo', 'base', 'rutas', 'variantes', 'partes'],
}
const ENTREGA = {
  type: 'object',
  properties: {
    rama: texto, sha: texto, idea: texto, navegacion: texto, voz: texto, pisado: texto,
    mejor: texto, flojo: texto, reporteVacio: si, fitnessOk: si, ficha: texto,
  },
  required: ['rama', 'sha', 'idea', 'reporteVacio', 'fitnessOk', 'mejor', 'flojo'],
}
const INTEGRACION = {
  type: 'object',
  properties: { sha: texto, integradas: lista, fallidas: lista, reporteVacio: si, fitnessOk: si, capturas: texto },
  required: ['sha', 'integradas', 'fallidas', 'reporteVacio', 'fitnessOk', 'capturas'],
}
const DECISION = {
  type: 'object',
  properties: {
    ganador: texto, porque: texto,
    arreglos: {
      type: 'array',
      items: {
        type: 'object',
        properties: { parte: texto, archivos: texto, cantidad: { type: 'integer' } },
        required: ['parte', 'archivos', 'cantidad'],
      },
    },
  },
  required: ['ganador', 'porque', 'arreglos'],
}
const CIERRE_ITERACION = {
  type: 'object',
  properties: { ok: si, motivo: texto, sha: texto, evidencia: texto, nota: texto },
  required: ['ok', 'motivo', 'sha', 'nota'],
}
const CIERRE = {
  type: 'object',
  properties: { sha: texto, resumen: texto, autorrevision: texto, agentCheck: texto, archivos: texto },
  required: ['sha', 'resumen', 'autorrevision', 'archivos'],
}
const VEREDICTO = {
  type: 'object',
  properties: {
    veredicto: { type: 'string', enum: ['PASS', 'CHANGES_REQUIRED'] },
    hallazgos: lista,
    informe: texto,
  },
  required: ['veredicto', 'hallazgos', 'informe'],
}
const ARREGLO_FINAL = {
  type: 'object',
  properties: { sha: texto, informe: texto },
  required: ['sha', 'informe'],
}
const PULL = {
  type: 'object',
  properties: { url: texto, rama: texto, motivo: texto },
  required: ['url', 'rama', 'motivo'],
}

const resultado = { estado: 'en curso', iteraciones: [] }
const terminar = (estado) => {
  resultado.estado = estado
  log(estado)
  return resultado
}

if (!A.encargo || !A.carpeta) {
  return terminar('No arrancó: faltan args.encargo (ruta del encargo creativo) y args.carpeta (carpeta de trabajo fuera del repo).')
}

// ── Preparar ─────────────────────────────────────────────────────────────────────────────────
phase('Preparar')
const prep = await agent(`${COMUN}

Sos quien prepara una corrida creativa desatendida (skill ${SKILL}; leela). Encargo creativo: ${A.encargo}.

1. Leé el encargo entero. Tiene que decir "aprobado por el usuario" y no tener "a definir". Si no, devolvé ok=false con el motivo.
2. Sacá del encargo: la alternativa (nombre de carpeta, por ejemplo cozy-medieval) y su nombre visible, la pantalla ancla (la ruta SIN la barra inicial, por ejemplo prototipo/publico-portada), la cantidad de iteraciones, de conceptos, si autoriza PR, la rama base del PR, el brief, la tabla de voz (cliente-liviano/src/test/resources/voz/<alternativa>.txt) y la carpeta de referencias.${A.iteraciones ? ` Iteraciones: usá ${A.iteraciones} (pisa el encargo).` : ''}${A.conceptos ? ` Conceptos: usá ${A.conceptos} (pisa el encargo).` : ''}${A.pr !== undefined ? ` PR: ${A.pr ? 'sí' : 'no'} (pisa el encargo).` : ''}
3. Si la alternativa no existe (static/css/alternativas/<alternativa>/tokens.css), creala: el brief en docs/interfaz/identidad/ (si el encargo no lo trae), static/css/alternativas/<alternativa>/tokens.css y app.css propios, el juego completo de plantillas copiado de templates/ a templates/alternativas/<alternativa>/ (cada layout, fragmento y página, con sus referencias cambiadas a ~{alternativas/<alternativa>/...}; InterfazFitnessTest exige el juego entero) y la tabla de voz cliente-liviano/src/test/resources/voz/<alternativa>.txt (vacía si todavía no dice nada a su modo). Corré mvn test -pl cliente-liviano -Dtest=InterfazFitnessTest y comiteá "E6: <nombre>, alternativa nueva (preparación)". Si el test no pasa, devolvé ok=false con el motivo.
4. git status --porcelain tiene que estar vacío (si no, ok=false). Anotá la rama actual y el SHA de HEAD como base.${A.inicio > 1 ? ` Esta corrida retoma desde la iteración ${A.inicio}: leé ${A.carpeta}/traspaso-iteracion-${A.inicio - 1}.md y comprobá que HEAD es el SHA que dice.` : ''}
5. Baseline: mvn test -pl cliente-liviano (en segundo plano si tarda). Si falla, ok=false con el detalle (la corrida no arranca en rojo).
6. Capturas de antes: mkdir -p ${A.carpeta}/antes ${A.carpeta}/ruido y corré dos veces la verificación visual sin alternativa (recorre todas las identidades) sobre las páginas reales y la ancla:
   mvn test -pl cliente-liviano -Pvisual -Dtest=VerificacionVisualTest -Dsurefire.failIfNoSpecifiedTests=false -Dverificacion.paginas=estilos,estilos/armazon,<ancla>
   Después de la primera, copiá cliente-liviano/target/capturas/*.png a ${A.carpeta}/antes/; después de la segunda, a ${A.carpeta}/ruido/ (mide lo que se mueve aunque nada cambie).
7. intocables: un glob (sintaxis de Java) de las capturas de las páginas reales de las identidades que NO se diseñan, para scripts/visual/Imagenes.java comparar. Si se diseña una alternativa, Glassmorphism es "{estilos,estilos-armazon}-[0-9]*.png" (sus capturas no llevan "--"). Si hay otras alternativas que no se diseñan, sumalas con su sufijo --<nombre>.

Devolvé los datos con el formato pedido (antes = la carpeta de las capturas de antes; ruido = la de la segunda corrida; baseline = GREEN o RED).`, { label: 'preparar', phase: 'Preparar', schema: PREPARACION, ...MECANICO })

if (!prep) return terminar('Detenida en Preparar: el agente no respondió.')
if (!prep.ok) return terminar(`No arrancó: ${prep.motivo}`)
Object.assign(resultado, { rama: prep.rama, base: prep.base, alternativa: prep.alternativa })
log(`Encargo listo: ${prep.nombre}, ancla ${prep.ancla}, ${prep.iteraciones} iteraciones, ${prep.conceptos} conceptos, PR ${prep.pr ? 'sí' : 'no'}.`)

const DATOS = `Datos de la corrida: alternativa ${prep.alternativa} (${prep.nombre}); encargo ${A.encargo}; ancla ${prep.ancla}; brief ${prep.brief || '(ver encargo)'}; voz ${prep.voz || '(ver encargo)'}; referencias ${prep.referencias || '(ver encargo)'}; capturas de antes en ${prep.antes}; glob de lo que no se diseña: ${prep.intocables}.`

let base = prep.base
const desde = Math.max(1, A.inicio || 1)
const hasta = Math.min(3, prep.iteraciones)

for (let n = desde; n <= hasta; n++) {
  const tipo = TIPOS[n - 1]
  const dir = `${A.carpeta}/iteracion-${n}`
  const previa = n > 1 ? `La iteración anterior dejó su decisión en ${A.carpeta}/iteracion-${n - 1}/decision.md y su nota de traspaso en ${A.carpeta}/traspaso-iteracion-${n - 1}.md: leelas.` : 'Es la primera iteración.'
  const etiqueta = `it${n} ${tipo}`

  // ── Encargo ──────────────────────────────────────────────────────────────────────────────
  const plan = await agent(`${COMUN}

Sos el director de la iteración ${n} de ${hasta} (${tipo}) de una corrida creativa. Leé la skill ${SKILL} (sección 3), el encargo ${A.encargo}, docs/interfaz/direccion-de-arte.md ("Libertad creativa" y "Oficio") y las plantillas ${PLANTILLAS}/encargo-constructor.md y ${PLANTILLAS}/encargo-critico.md. ${previa}
${DATOS}
Base: ${base}.

1. mkdir -p ${dir}.
2. Elegí las partes:
   - concepto: ${prep.conceptos} conceptos de la ancla, OPUESTOS en composición y navegación (no solo en estilo), cada uno con un nombre corto y su idea en una frase. Nombres de variante concepto-a, concepto-b, … (archivos templates/variantes/concepto-<letra>.html y static/css/variantes/concepto-<letra>.css). Un concepto tímido pierde: elegí ideas que alguien recuerde.
   - sistema: de 2 a ${TOPE_POR_PASO} zonas de propiedad con archivos disjuntos (por ejemplo, "estructura": layouts, navegación y la ancla; "componentes": fragmentos y catálogo).
   - flujo: un grupo de pantallas por rol (publico, donante, entidad, admin), según docs/interfaz/mapa-del-sitio.md. Si PrototipoController no sirve a esta alternativa (mirá su CARPETA), devolvé bloqueo con el motivo y no hagas nada más.
3. Solo en sistema: si hace falta, un commit de preparación en este worktree (la hoja de cada zona, vacía, enlazada después de app.css en los layouts de la alternativa; la tabla de voz si falta): "E6: ${prep.nombre}, iteración ${n}, preparación". La base nueva es ese SHA.
4. Llená la plantilla del constructor en ${dir}/encargo-constructor.md: reemplazá cada @MARCA@ según la sección "Marcas" de la plantilla (@ARREGLOS@ = ${dir}/arreglos.md; @RUTAS@ = las rutas a comprobar separadas por coma y SIN barra inicial; @VARIANTES@ = -Dverificacion.variantes=<tu variante> en concepto o vacío; @COMMIT@ = "E6: ${prep.nombre}, iteración ${n}, <parte>"), borrá las secciones de "Qué se construye" que no son de esta iteración (dejá también "Arreglos") y dejala autocontenida.
5. Llená la plantilla del crítico en ${dir}/encargo-critico.md, también según su sección "Marcas" (@CAPTURAS@ = ${dir}/capturas/, @REPORTE@ = ${dir}/capturas/reporte.json, @PROPUESTAS@ = las partes con su idea, @CODIGO@ = los archivos de cada parte, @ANTES@ = las capturas de ${prep.antes} de la ancla).

Devolvé bloqueo (vacío si no hay), base, rutas (separadas por coma, sin barra inicial), variantes (los nombres en concepto; vacío en las demás) y partes (nombre, idea, archivos).`, { label: `${etiqueta} · director`, phase: 'Encargo', schema: PLAN, ...DISENO })

  if (!plan) return terminar(`Detenida en el encargo de la iteración ${n}: el director no respondió.`)
  if (plan.bloqueo) {
    resultado.pendiente = `Iteración ${n} (${tipo}) sin hacer: ${plan.bloqueo}`
    log(resultado.pendiente)
    if (!resultado.iteraciones.length) return terminar(`Detenida antes de la iteración ${n}: ${plan.bloqueo}`)
    break
  }
  base = plan.base || base
  let partes = plan.partes
  if (partes.length > TOPE_POR_PASO) {
    log(`Tope de ${TOPE_POR_PASO} constructores por paso: quedan afuera ${partes.slice(TOPE_POR_PASO).map((p) => p.nombre).join(', ')}.`)
    partes = partes.slice(0, TOPE_POR_PASO)
  }
  const otras = (p) => partes.filter((o) => o !== p).map((o) => `${o.nombre} (${o.archivos})`).join('; ')

  // ── Construir ────────────────────────────────────────────────────────────────────────────
  const entregas = (await parallel(partes.map((p) => () => agent(`${COMUN}

Sos el constructor de la parte "${p.nombre}" en la iteración ${n} (${tipo}) de una corrida creativa. Leé entero ${dir}/encargo-constructor.md y seguilo: es tu encargo. Si tenés la skill ponytail, aplicala al código (lo más simple que funcione), nunca a la idea.
Tu parte: ${p.idea}
Tus archivos (nada más): ${p.archivos}
Otras partes en paralelo (no toques sus archivos): ${otras(p) || 'ninguna'}
Primero: git reset --hard ${base} (este worktree es tuyo).${tipo === 'flujo' ? ' Sumá en ficha un bloque "Para la ficha del flujo" de hasta 200 palabras: el recorrido de tu grupo en pasos, los desvíos y las decisiones con su porqué.' : ''}
Devolvé la rama y el SHA de tu commit y el resto de los campos.`, { label: `${etiqueta} · ${p.nombre}`, phase: 'Construir', isolation: 'worktree', schema: ENTREGA, ...DISENO }).then((e) => e && { ...e, parte: p.nombre })))).filter(Boolean)

  if (entregas.length < partes.length) log(`Iteración ${n}: ${partes.length - entregas.length} constructor(es) no respondieron.`)
  if (!entregas.length) return terminar(`Detenida en la iteración ${n}: ningún constructor entregó.`)

  // ── Integrar ─────────────────────────────────────────────────────────────────────────────
  const integracion = await agent(`${COMUN}

Sos el integrador de la iteración ${n} (${tipo}). En este worktree (la rama de la corrida, en ${base}):
1. git cherry-pick de cada commit, en este orden: ${entregas.map((e) => `${e.parte} = ${e.sha}`).join('; ')}. Si un conflicto es de agregados en un archivo compartido (la tabla de voz, el final de tokens.css), quedate con los dos lados. Si es otra cosa, git cherry-pick --abort de ese y anotalo en fallidas con el motivo.
2. mvn test -pl cliente-liviano -Dtest=InterfazFitnessTest.
3. Verificación conjunta: mvn test -pl cliente-liviano -Pvisual -Dtest=VerificacionVisualTest -Dsurefire.failIfNoSpecifiedTests=false -Dverificacion.alternativa=${prep.alternativa} -Dverificacion.paginas=${plan.rutas}${plan.variantes.length ? ` -Dverificacion.variantes=${plan.variantes.join(',')}` : ''}
4. mkdir -p ${dir}/capturas y copiá ahí cliente-liviano/target/capturas/ (PNG, reporte.json, index.html y comparacion.html si está).
No arregles nada: solo integrá y medí. Devolvé el SHA de HEAD, las partes integradas y fallidas, si reporte.json dio [] y si el test de reglas pasó.`, { label: `${etiqueta} · integrar`, phase: 'Integrar', schema: INTEGRACION, ...MECANICO })

  if (!integracion) return terminar(`Detenida al integrar la iteración ${n}.`)
  if (!integracion.integradas.length) return terminar(`Detenida en la iteración ${n}: no se pudo integrar ninguna parte (${integracion.fallidas.join('; ')}).`)
  base = integracion.sha
  const integradas = partes.filter((p) => integracion.integradas.includes(p.nombre))

  // ── Criticar ─────────────────────────────────────────────────────────────────────────────
  const criticas = (await parallel(LENTES.map((lente) => () => agent(`Sos el crítico de ${lente} de la iteración ${n} (${tipo}) de una corrida creativa en el repo DonaTrack. Solo lectura: no modificás nada ni corrés nada que escriba. Leé entero ${dir}/encargo-critico.md y seguilo con tu lente: ${lente}. Las partes integradas son: ${integradas.map((p) => `${p.nombre} (${p.idea})`).join('; ')}${integracion.fallidas.length ? `; no se integraron: ${integracion.fallidas.join('; ')}` : ''}. El reporte del piso dio ${integracion.reporteVacio ? 'vacío' : 'fallas (leelo)'} y el test de reglas ${integracion.fitnessOk ? 'pasó' : 'falló'}. Mirá las capturas con la herramienta de lectura de imágenes. Tu respuesta final es tu crítica, en el formato del encargo.`, { label: `${etiqueta} · crítica ${lente}`, phase: 'Criticar', ...CRITICA }).then((t) => t && `## Crítica de ${lente}\n\n${t}`)))).filter(Boolean)

  if (!criticas.length) return terminar(`Detenida en la iteración ${n}: ningún crítico respondió.`)

  // ── Decidir ──────────────────────────────────────────────────────────────────────────────
  const decision = await agent(`${COMUN}

Sos el decisor de la iteración ${n} (${tipo}) de una corrida creativa. Leé el encargo ${A.encargo}, ${dir}/encargo-constructor.md y la dirección de arte. Las entregas de los constructores:
${JSON.stringify(entregas, null, 1)}

Las críticas (textuales):
${criticas.join('\n\n')}

1. Guardá las críticas, textuales, en ${dir}/criticas.md.
2. Escribí ${dir}/decision.md: la matriz con criterios ponderados (el mensaje del encargo, la audacia, el oficio, el piso y si da para todo el sitio), puntajes por parte, ${tipo === 'concepto' ? 'el ganador, qué se toma de los otros conceptos para combinar' : 'qué se conserva y qué se corrige de cada zona'}, las opciones descartadas y por qué. No diluyas la audacia: una crítica que lleva a volver a lo de antes se descarta, con su porqué.
3. Escribí ${dir}/arreglos.md: primero las reglas comunes de la pasada; después, por parte, la lista numerada de arreglos (los BLOQUEA primero), cada uno con dónde y cómo, conservando la idea. ${tipo === 'concepto' ? 'En concepto, los arreglos (y lo que se toma de los otros) van solo a la parte ganadora.' : 'Una lista por zona; la zona que no tiene arreglos no figura.'}
Devolvé el ganador (o "todas las zonas"), el porqué en dos frases y los arreglos por parte (parte, archivos, cantidad).`, { label: `${etiqueta} · decidir`, phase: 'Decidir', schema: DECISION, ...DISENO })

  if (!decision) return terminar(`Detenida al decidir la iteración ${n}.`)
  let arreglos = decision.arreglos.filter((a) => a.cantidad > 0)
  if (arreglos.length > TOPE_POR_PASO) {
    log(`Tope de ${TOPE_POR_PASO} arregladores: quedan para el cierre ${arreglos.slice(TOPE_POR_PASO).map((a) => a.parte).join(', ')}.`)
    arreglos = arreglos.slice(0, TOPE_POR_PASO)
  }

  // ── Arreglar ─────────────────────────────────────────────────────────────────────────────
  const arregladas = (await parallel(arreglos.map((a) => () => agent(`${COMUN}

Sos el arreglador de la parte "${a.parte}" en la iteración ${n} (${tipo}). Leé ${dir}/encargo-constructor.md (todo, y en especial "Arreglos"), ${dir}/decision.md y tu lista en ${dir}/arreglos.md. Hacé cada arreglo SIN perder la idea ni la audacia; el que no se pueda hacer sin traicionarla, explicalo. Tus archivos: ${a.archivos}.
Primero: git reset --hard ${base} (este worktree es tuyo). Comprobá como dice el encargo y comiteá "E6: ${prep.nombre}, iteración ${n}, arreglos ${a.parte}". Devolvé la rama, el SHA y el resto de los campos (en idea: qué arreglaste por número y qué no, con su porqué).`, { label: `${etiqueta} · arreglos ${a.parte}`, phase: 'Arreglar', isolation: 'worktree', schema: ENTREGA, ...ARREGLO }).then((e) => e && { ...e, parte: a.parte })))).filter(Boolean)

  if (arregladas.length < arreglos.length) log(`Iteración ${n}: ${arreglos.length - arregladas.length} arreglador(es) no respondieron; el cierre lo anota.`)

  // ── Cerrar iteración ─────────────────────────────────────────────────────────────────────
  const fichas = entregas.filter((e) => e.ficha).map((e) => `### ${e.parte}\n${e.ficha}`).join('\n\n')
  const cierre = await agent(`${COMUN}

Sos quien cierra la iteración ${n} de ${hasta} (${tipo}) de una corrida creativa (skill ${SKILL}, sección 3, paso 7). ${DATOS}
En este worktree (rama de la corrida, en ${base}):
1. git cherry-pick de los arreglos: ${arregladas.length ? arregladas.map((e) => `${e.parte} = ${e.sha}`).join('; ') : 'no hay'}. Si un arreglo ya está aplicado, seguí.
2. Gates: mvn test -pl cliente-liviano (todo verde); la verificación visual de la iteración (-Dverificacion.alternativa=${prep.alternativa} -Dverificacion.paginas=${plan.rutas}${plan.variantes.length ? ` -Dverificacion.variantes=${plan.variantes.join(',')}` : ''}) con reporte.json en []; y lo que no se diseña sin cambios: otra corrida visual sin alternativa con -Dverificacion.paginas=estilos,estilos/armazon y después java scripts/visual/Imagenes.java comparar ${prep.antes} cliente-liviano/target/capturas "${prep.intocables}" ${prep.ruido} (tiene que decir SIN CAMBIOS; la carpeta de ruido descuenta lo que se mueve solo). Copiá las capturas finales a ${dir}/final/. Si un gate falla, arreglá la causa con el cambio más chico que conserve la idea; si no podés, devolvé ok=false con el motivo y no comitees.
3. Evidencia: java scripts/visual/Imagenes.java jpg (400 KB o menos) a docs/interfaz/evidencia/${prep.alternativa}/<tema>/ (tema: ${tipo === 'concepto' ? `concepto-<ancla>` : tipo === 'sistema' ? 'sistema' : 'flujo-completo'}, o el que ya use la bitácora). En concepto: las cuatro capturas (360, 1280, 1920 y 360x640) de cada concepto y del antes; en sistema y flujo: 360 y 1280 de cada ruta y la 360x640 de la ancla.
4. ${tipo === 'flujo' ? `Ficha del flujo: escribí o actualizá docs/interfaz/flujo-completo-${prep.alternativa}.md con lo que mandaron los constructores:\n${fichas || '(no mandaron bloques)'}\n   y sumala a la tabla "Archivos de esta carpeta" de docs/interfaz/README.md y a docs/ESTADO_DOCUMENTACION.md si no está.\n4b. ` : ''}Bitácora (docs/interfaz/bitacora.md, arriba de todo en "Decisiones"): "### <fecha de hoy, con date +%F> · ${tipo} (${prep.nombre}) · Iteración ${n}: <decisión en una línea>", con Decisión, Alternativas (lo descartado), Por qué (de ${dir}/decision.md), Costo (incluí "Reemplaza: …" con la decisión anterior que reemplaza y quién la había tomado, o "No reemplaza nada"), Quién decidió: "corrida creativa desatendida; el usuario decide con el merge" y Evidencia.
5. Commit de control: "E6: ${prep.nombre}, iteración ${n}, ${tipo}".
6. Nota de traspaso en ${A.carpeta}/traspaso-iteracion-${n}.md: el SHA, qué se decidió, qué archivos cambiaron, dónde están las capturas, los gates con su resultado, qué quedó sin hacer y cómo retomar (args.inicio = ${n + 1}).
Devolvé ok, el motivo (o "todo en verde"), el SHA, la carpeta de la evidencia y la ruta de la nota.`, { label: `${etiqueta} · cerrar`, phase: 'Cerrar iteración', schema: CIERRE_ITERACION, ...MECANICO })

  if (!cierre) return terminar(`Detenida al cerrar la iteración ${n}.`)
  resultado.iteraciones.push({ n, tipo, ganador: decision.ganador, porque: decision.porque, sha: cierre.sha, nota: cierre.nota, ok: cierre.ok, motivo: cierre.motivo })
  if (!cierre.ok) return terminar(`Detenida en el cierre de la iteración ${n}: ${cierre.motivo}`)
  base = cierre.sha
  log(`Iteración ${n} (${tipo}) cerrada en ${base}: ${decision.ganador}.`)
}

// ── Cierre ───────────────────────────────────────────────────────────────────────────────────
phase('Cierre')
const fin = await agent(`${COMUN}

Sos quien cierra la corrida creativa (skill ${SKILL}, sección 4). ${DATOS} Iteraciones hechas: ${JSON.stringify(resultado.iteraciones)}.${resultado.pendiente ? ` Quedó sin hacer: ${resultado.pendiente}; decilo en la página, en la autorrevisión y en la bitácora.` : ''} Las capturas de cada iteración están en ${A.carpeta}/iteracion-<n>/capturas/ y /final/; las de antes, en ${prep.antes}.
1. Página de antes y después: escribí un guion en ${A.carpeta}/resumen/guion.txt (formato en scripts/visual/README.md): el pedido del usuario textual, y por iteración qué se hizo y por qué, con las capturas de antes y de después de la ancla (1280 y 360x640) y las de los conceptos. Corré java scripts/visual/Imagenes.java resumen ${A.carpeta}/resumen/guion.txt ${A.carpeta}/resumen-antes-despues.html. No entra al repo, y no se la mandás al usuario: se la manda la sesión que lanzó la corrida.
2. Documentación: docs/interfaz/README.md (tabla "Archivos de esta carpeta") y docs/ESTADO_DOCUMENTACION.md con lo nuevo que dejó la corrida (evidencia, fichas); si nada cambió, decilo. node scripts/agent-check.js tiene que dar 0 FAIL.
3. Autorrevisión en ${A.carpeta}/autorrevision.md: una fila por criterio del "Criterio de salida" de la skill y del encargo: criterio · CUMPLE / NO CUMPLE / NO APLICA · archivo:línea o comando.
4. Si cambiaste algo en el repo, comiteá "E6: ${prep.nombre}, cierre de la corrida creativa".
Devolvé el SHA de HEAD, la ruta de la página, la de la autorrevisión, el resultado de agent-check y archivos (la salida de git diff --name-only ${prep.base}..HEAD, separada por coma).`, { label: 'cierre', phase: 'Cierre', schema: CIERRE, ...MECANICO })

if (!fin) return terminar('Detenida en el cierre de la corrida.')
Object.assign(resultado, { sha: fin.sha, resumen: fin.resumen, autorrevision: fin.autorrevision })

// ── Auditoría ────────────────────────────────────────────────────────────────────────────────
if (A.auditoria !== false) {
  phase('Auditoría')
  const alcance = `Plan aprobado: el encargo ${A.encargo} y la skill ${SKILL}. Criterios: el "Criterio de salida" de la skill, la sección 6 (libertades y límites) del encargo, la dirección de arte (piso, "Libertad creativa" y "Oficio") y cliente-liviano/AGENTS.md. Alcance: git diff ${prep.base}..HEAD (archivos: ${fin.archivos}). Autorrevisión del implementador: ${fin.autorrevision}. Capturas: ${A.carpeta}/iteracion-*/final/. Glob de lo que no se diseña: ${prep.intocables} (antes en ${prep.antes}). Nivel: STANDARD.`

  const REFRESCAR = `Si cambiás plantillas o estilos, después de comprobar: copiá las capturas nuevas sobre las de la carpeta final/ de la última iteración, volvé a sacar los JPG de evidencia que correspondan (java scripts/visual/Imagenes.java jpg) y volvé a correr java scripts/visual/Imagenes.java resumen ${A.carpeta}/resumen/guion.txt ${A.carpeta}/resumen-antes-despues.html, para que la página y la evidencia muestren lo comiteado. Sumá al final de ${fin.autorrevision} qué cambiaste y los gates que corriste.`

  try {
    const pre = await agent(`Pre-auditoría de una corrida creativa en el repo DonaTrack. ${alcance} Listá los incumplimientos con archivo:línea. Solo lectura.`, { label: 'pre-auditor', phase: 'Auditoría', agentType: 'lector-auditoria' })
    if (pre) {
      const arreglo = await agent(`${COMUN}

Un pre-auditor revisó la corrida creativa y dijo:
${pre}

Arreglá en este worktree lo que sea un incumplimiento real (verificá cada uno contra el archivo antes de tocar nada), sin diluir la idea. Corré los gates que toque lo que cambies (mvn test -pl cliente-liviano y, si tocás plantillas o estilos, la verificación visual de esas rutas). ${REFRESCAR} Comiteá "E6: ${prep.nombre}, arreglos de la pre-auditoría". Si no hay nada real, no comitees. Devolvé el SHA de HEAD y, en informe, qué arreglaste y qué descartaste, con su porqué.`, { label: 'arreglos pre-auditoría', phase: 'Auditoría', schema: ARREGLO_FINAL, ...ARREGLO })
      if (arreglo && arreglo.sha) resultado.sha = arreglo.sha
    }
  } catch (e) {
    log(`Sin pre-auditor (lector-auditoria no disponible): ${e.message}`)
  }

  // Si qa-gatekeeper no está, el agente por defecto audita con el mismo pedido.
  const auditar = async (pedido, opciones) => {
    try {
      const v = await agent(pedido, { ...opciones, agentType: 'qa-gatekeeper' })
      if (v) return v
    } catch (e) {
      log(`Sin qa-gatekeeper (${e.message}): audita el agente por defecto.`)
    }
    return agent(pedido, opciones)
  }

  let veredicto = null
  let previa = null
  for (let ronda = 1; ronda <= 2; ronda++) {
    const encabezado = ronda === 1 ? 'Ronda 1.' : `Ronda ${ronda}, acotada. Base: ${previa.sha}. Hallazgos de la ronda anterior, textuales:\n${previa.hallazgos.join('\n')}`
    veredicto = await auditar(`${encabezado}

Auditoría de cierre de una corrida creativa desatendida en el repo DonaTrack, con contexto limpio y en solo lectura: no modificás nada. ${alcance}
Devolvé el veredicto, los hallazgos (uno por línea, con gravedad y archivo:línea) y el informe.`, { label: `auditoría ronda ${ronda}`, phase: 'Auditoría', schema: VEREDICTO, ...MECANICO })
    if (!veredicto || veredicto.veredicto === 'PASS' || ronda === 2) break
    const arreglo = await agent(`${COMUN}

La auditoría de cierre (ronda ${ronda}) pidió cambios:
${veredicto.hallazgos.join('\n')}

Arreglá cada hallazgo en este worktree sin diluir la idea; verificá con los gates que toque. ${REFRESCAR} Comiteá "E6: ${prep.nombre}, arreglos de la auditoría (ronda ${ronda})". Devolvé el SHA de HEAD y, en informe, qué arreglaste por hallazgo.`, { label: `arreglos auditoría ronda ${ronda}`, phase: 'Auditoría', schema: ARREGLO_FINAL, ...ARREGLO })
    previa = { sha: (arreglo && arreglo.sha) || resultado.sha, hallazgos: veredicto.hallazgos }
    resultado.sha = previa.sha
  }
  resultado.auditoria = veredicto ? { veredicto: veredicto.veredicto, hallazgos: veredicto.hallazgos } : { veredicto: 'SIN RESPUESTA', hallazgos: [] }
  if (!veredicto || veredicto.veredicto !== 'PASS') return terminar('Terminó sin PASS de la auditoría: la rama queda para revisar, sin PR.')
}

// ── PR ───────────────────────────────────────────────────────────────────────────────────────
if (prep.pr && A.auditoria === false) log('Sin PR: el encargo lo autoriza, pero la auditoría se salteó.')
if (prep.pr && A.auditoria !== false) {
  phase('PR')
  const pr = await agent(`${COMUN}

Sos quien abre el PR de la corrida creativa, autorizado por el encargo ${A.encargo}. Esta vez sí hacés push.
1. Si la rama actual (${prep.rama}) no empieza con E6_, renombrala con git branch -m a E6_<nombre corto del encargo> (lo exige el CI de ENTREGA_6).
2. Comprobá la cuenta de GitHub activa (gh auth status) y la regla de credenciales del entorno antes del push. git push -u origin <rama>.
3. Cuerpo del PR en ${A.carpeta}/pr-body.md con la plantilla de docs/IA/04-checklist-antes-de-pr.md: qué se pidió (el encargo), qué se hizo por iteración, gates con su resultado, la auditoría, lo que queda abierto y que la página de antes y después está en la sesión (${fin.resumen}). Última línea: 🤖 Generated with [Claude Code](https://claude.com/claude-code)
4. gh pr create --draft --base ${prep.ramaBase} --body-file ${A.carpeta}/pr-body.md con un título "E6: ${prep.nombre}, corrida creativa".
Devolvé la URL, la rama y el motivo si algo falló (URL vacía).`, { label: 'pr', phase: 'PR', schema: PULL, ...MECANICO })
  resultado.pr = pr || { url: '', motivo: 'el agente no respondió' }
}

return terminar(`Corrida terminada en ${resultado.sha}.${resultado.pendiente ? ` Pendiente: ${resultado.pendiente}.` : ''}${resultado.pr ? ` PR: ${resultado.pr.url || resultado.pr.motivo}` : ' Sin PR (el encargo no lo autoriza, se pidió así o se salteó la auditoría).'}`)
