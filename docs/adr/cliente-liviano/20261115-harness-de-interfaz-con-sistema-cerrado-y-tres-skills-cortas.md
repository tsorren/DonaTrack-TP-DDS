# Harness de interfaz: sistema visual cerrado, un kit de listas cercado en el build y tres skills cortas

- Status: proposed
- Date: 2026-11-15
- Deciders: el usuario (2026-11-14, "Sí, confirmá A y anotalo", después de la línea base sobre
  R11-B04; 2026-11-15, al aprobar el plan de los pasos 3 y 4); este registro, el agente que
  implementa el paso 3. Pendiente de aprobación del equipo en el PR
- Tags: cliente-liviano, interfaz, harness, skills, agentes, entrega-6

> `[OBSERVED]` El paso 2 de esta decisión (el kit `componentes/listado` y su cerco en
> `InterfazFitnessTest`) ya está en el PR #983. Este ADR registra la decisión completa, de la que el
> kit es una parte, y el cambio de harness que la acompaña.

## Contexto y Problema

El diagnóstico del harness de frontend (2026-11-14) encontró tres síntomas en el cliente liviano:
- **Cohesión baja.** La identidad no tiene una fuente única: el brief contradice a `tokens.css`. Solo
  el color está cercado. Las listas no tienen un componente de búsqueda y filtros: 13 plantillas
  arman su propio `<form role="search">`, con parámetros de nombres distintos (`buscar`, `nombre`, `q`).
- **Alternativas poco diferenciadas.** Identidad, componentes, textos y metáforas llegan fijos a
  cada corrida creativa. Los conceptos "opuestos" solo cambian la disposición. Además, el proceso
  sesga hacia una ganadora: "(Recomendada)" y una matriz que premia lo seguro.
- **Requisitos de UX perdidos.** R11-B04, la barra común de búsqueda y filtros, se difirió en la
  planificación sin que nadie lo marcara. Ningún gate comparaba pantallas entre sí.

A eso se suma el peso de la lectura. Las reglas mandaban leer unos 206 KB antes de una tarea de UI,
con 7 skills de UI de 77 a 162 líneas y un `cliente-liviano/AGENTS.md` de 185.

La pregunta: ¿qué harness hace que los agentes produzcan pantallas cohesivas y cumplan los
requisitos de UX, sin cerrar la exploración visual?

## Atributos de Calidad y Drivers de Decisión

- K1 Cohesión sostenida entre pantallas y en el tiempo.
- K2 Requisitos de UX cumplidos en corridas desatendidas.
- K3 Diferenciación y calidad de las alternativas.
- K4 Identidad distintiva, propia del dominio.
- K5 Carga de contexto y mantenimiento del harness.
- K6 Costo de migración.
- K7 El humano decide y el agente ejecuta.
- Restricciones: Thymeleaf y CSS sin build de frontend
  ([ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)); todo
  anda sin JavaScript; la identidad no se reabre (decisión del usuario, 2026-11-14).

## Relaciones Arquitectónicas

1. **Se apoya en:** [una sola identidad](./20261103-una-sola-identidad-en-el-cliente-liviano.md)
   (`accepted`). Una identidad o un rediseño nuevo se sigue proponiendo en una rama propia; cambia
   la herramienta: `explorar-ui` en lugar de `relevamiento-creativo` y `corrida-creativa`.
2. **Se apoya en:** [interacción, movimiento y componentes](./20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
   (`proposed`, D33). El catálogo y las capas CUBE siguen como están. El kit de listas es un
   componente más de ese catálogo.
3. **Acompaña a:** [sin Pico.css](./20261114-sin-pico-css-en-el-cliente-liviano.md) (`proposed`),
   que resolvió la base declarada que no era la real (C1.5).

## Alternativas Consideradas

| Opción | Qué implica |
|---|---|
| **A. Sistema propio cerrado** | Identidad en `tokens.css` y `/estilos`. Componentes propios, con un kit único de listas. Cerco en `InterfazFitnessTest`. 3 skills cortas y reglas con tope de tamaño |
| B. Stock con brief corto | Sin skills de UI propias: el plugin `frontend-design`, un brief de una página y el `AGENTS.md` del módulo. Solo el cerco de color y AA |
| C. Librería externa tematizada | Bootstrap 5.3, solo con CSS, tematizado por variables en un `tema.css`. Una regla y una skill de revisión |

Matriz, con puntaje de 1 a 5. Pesos del perfil "Equilibrado", elegido por el usuario el 2026-11-14:

| Criterio | Peso | A | B | C |
|---|---|---|---|---|
| K1 Cohesión | 15 | 4 | 2 | 5 |
| K2 Requisitos de UX | 15 | 4 | 3 | 5 |
| K3 Alternativas | 15 | 4 | 4 | 2 |
| K4 Identidad propia | 15 | 4 | 4 | 2 |
| K5 Carga del harness | 15 | 3 | 5 | 4 |
| K6 Costo de migración | 10 | 3 | 5 | 2 |
| K7 Humano decide | 15 | 5 | 3 | 4 |
| **Total** | 100 | **390** | 365 | 350 |

Línea base sobre R11-B04 (n = 1 por condición; el informe y sus mediciones quedaron fuera del repo,
en la carpeta local de la línea base, y el resumen está en la
[bitácora](../../interfaz/bitacora.md), entrada del kit del 2026-11-15):
- Las tres condiciones migraron las 6 pantallas a un solo fragmento.
- A sumó 3 medidas literales nuevas; el harness actual, 35; B, 32.
- A fue la única corrida completa y revisada.
- El usuario comparó A y B en la maqueta: "No se ven diferencias".

Descartadas:
- **B:** la cohesión depende del modelo en cada sesión. Sin un kit, no hay contra qué verificar los
  requisitos de UX.
- **C:** rehacer 74 hojas y casi todas las plantillas. Además, el sitio quedaría con aspecto de
  plantilla, que choca con la identidad.
- **Harness actual podado:** no resuelve el sistema sin cercar ni el requisito perdido.
- **Tailwind v4:** necesita build de frontend.
- **Web components:** necesitan JavaScript para mostrarse.

## Resultado de la Decisión

Se elige **A**, en cuatro pasos:
1. **Identidad:** no se reabre por ahora.
2. **Kit** (PR #983):
   - `componentes/listado` con un `vistas.Listado`, y un nombre de parámetro por campo, con alias;
   - el cerco en `InterfazFitnessTest`: ninguna barra propia, parámetros de una lista cerrada y un
     tope de medidas literales que solo baja;
   - Pico, fuera.
3. **Harness** (este cambio):
   - 3 skills de 60 líneas o menos, con la versión canónica en `.agents/skills/` y el adaptador en
     `.claude/skills/`:
     - `construir-ui`: producción con el sistema que hay;
     - `revisar-ui`: solo lectura, con las pantallas comparadas entre sí (U1–U7), donde todo lo que
       se aparta del sistema es bloqueante;
     - `explorar-ui`: opciones que divergen en ejes de producto, sin ranking, que no se mergean.
   - `.claude/rules/interfaz.md` de 10 líneas o menos y `cliente-liviano/AGENTS.md` de 80 o menos.
   - Lectura obligatoria propia de la UI de 30 KB o menos: la regla, el `AGENTS.md` del módulo, la
     skill y el tono. El catálogo se consulta por búsqueda.
   - Las 7 skills anteriores, `corrida-creativa.js` y las plantillas de encargo pasan a
     [`docs/IA/history/harness-ui-2026-11/`](../../IA/history/harness-ui-2026-11/README.md).
   - La bitácora, la evidencia y las fichas cerradas quedan donde están: los enlazan ADRs que no
     se editan, y ninguna regla manda leerlos.
   - `agent-check` controla que cada adaptador tenga su versión canónica, con el mismo frontmatter
     (`SKILL_ADAPTER_SYNC`), y los topes de líneas de las skills, la regla y el `AGENTS.md` del
     módulo (`UI_HARNESS_SIZE`).
   - `InterfazFitnessTest` exige que `construir-ui` y `revisar-ui` citen el catálogo y el kit.
4. **Workflow** (PR aparte):
   - cada decisión de una ronda con el usuario, con ID y asignada a un lote o diferida con su OK;
   - la revisión de interfaz, bloqueante;
   - una hoja de contacto en el PR;
   - Opus high para los constructores de vistas.

## Consecuencias

### Positivas
- La barra de listas y sus nombres los controla el build, no la memoria del agente.
- La lectura obligatoria de UI baja de unos 206 KB a unos 24 KB. Se mide con `wc -c` sobre la
  regla, el `AGENTS.md` del módulo, la skill más larga y el tono.
- La exploración queda separada de la producción: puede soltar la piel y los componentes sin
  chocar con los cercos, porque no se mergea.

### Negativas
- Hay menos texto guía para casos raros: el panel de críticos, el catálogo de temas A/B y las
  plantillas de encargo quedan como historia.
- Los enlaces de los ADRs, specs cerradas y fichas a las skills archivadas quedan rotos, porque esos
  registros no se editan ([DTI-44](../DEUDA_TECNICA.md#dti-44), ítem 15).
- El riesgo principal de A: que las reglas vuelvan a crecer. Lo contienen los topes de tamaño de
  esta decisión, que `agent-check` verifica (`UI_HARNESS_SIZE`); el tono y el catálogo no tienen
  tope.
- La excepción de la exploración frente al "minimalismo suficiente" del `AGENTS.md` raíz necesita
  un PR dedicado (§13). Hasta entonces, `explorar-ui` evita el choque con una regla: lo explorado no
  se mergea.

## Análisis por alternativa

- **A:** costo medio. El kit migró 17 plantillas; el harness, unos 30 archivos. Se revierte
  restaurando el archivo histórico y las reglas anteriores.
- **B:** costo bajo, pero la deriva entre sesiones e integrantes queda sin cerco.
- **C:** costo alto y una identidad de plantilla.

## Validación

- `mvn clean test -pl cliente-liviano -am` en verde, con `lasSkillsDeInterfazEmpiezanPorElCatalogo`
  sobre las skills nuevas.
- `node scripts/agent-check.js` sin fallas, con `SKILL_ADAPTER_SYNC` y `UI_HARNESS_SIZE` en PASS,
  y `node scripts/tests/run-tests.js` con los casos que fallan a propósito (18.2, 18.3, 18.5 y 18.6).
