# Spec: harness-ui-paso-3-skills — Harness de interfaz de la opción A: tres skills cortas y reglas

> **Estado:** implementada (2026-11-15, rama `E6_harness_frontend_skills`). Sale del plan que aprobó
> el usuario el 2026-11-15 ("OK, arrancá"), con tres decisiones: el archivo en `docs/IA/history`,
> lo histórico de `docs/interfaz/` sin mover y un workflow sucesor para el paso 4. Ningún ADR se
> promueve.
> **Nivel:** ARCHITECTURAL. Reemplaza el proceso de UI con el que trabajan los agentes (paso 3 del
> plan de migración del harness de frontend).
> **ADR:** uno nuevo, `proposed`: [harness de interfaz con sistema cerrado y tres skills cortas](../../adr/cliente-liviano/20261115-harness-de-interfaz-con-sistema-cerrado-y-tres-skills-cortas.md).
> **Depende de:** el kit del paso 2 ([R11-B04-kit-listado](R11-B04-kit-listado-spec.md), PR #983).
> **Deuda:** [DTI-44](../../adr/DEUDA_TECNICA.md#dti-44), ítem 15.

## 1. Objetivo Funcional (Goal)

Que un agente que toca la interfaz lea poco, use el sistema visual cerrado y el kit de listas, y no
vuelva a armar barras propias. Que la exploración visual tenga su propio camino, sin ranking y fuera
de producción. Todo con topes de tamaño que verifica una máquina.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope
- Tres skills, con la versión canónica en `.agents/skills/` y el adaptador en `.claude/skills/`:
  `construir-ui`, `revisar-ui` y `explorar-ui`. `construir-ui` y `revisar-ui` parten del borrador de la
  línea base (iii), por copia y ajustadas al kit real.
- Al archivo `docs/IA/history/harness-ui-2026-11/`: las 7 skills de UI, `corrida-creativa.js` y 4
  plantillas (`encargo-constructor`, `encargo-creativo`, `encargo-critico` e `iteracion-ab`).
- `.claude/rules/interfaz.md`, de 10 líneas o menos; `cliente-liviano/AGENTS.md`, de 80 o menos.
- El cerco de las skills en `InterfazFitnessTest`. En `agent-check`, `SKILL_ADAPTER_SYNC` y
  `UI_HARNESS_SIZE`, con sus tests.
- Las referencias vivas:
  - `docs/interfaz/README.md`, `componentes.md`, `criterios-catedra-ux-ui.md`,
    `direccion-de-arte.md`, `tono-y-comunicacion.md` y `plantillas/flujo.md`;
  - las notas de historia de `temas-ab.md` y `encargos/README.md`;
  - `context-index`, `docs/IA/README.md`, `scripts/visual/README.md`, el Javadoc de
    `VariantesInterceptor` y DTI-28;
  - las tres líneas de `backlog-r11.js` que citaban skills archivadas.

### Out-of-Scope
- El workflow con trazabilidad de decisiones, revisión bloqueante, hoja de contacto y Opus high: el
  paso 4, en su PR.
- La excepción de la exploración en el `AGENTS.md` raíz: un PR dedicado (§13).
- El brief de una página y la identidad: el paso 1, que no se reabre.
- Mover la bitácora, la evidencia, lo histórico o las fichas cerradas: decisión del usuario.
- Las referencias en ADRs, specs cerradas, bitácora, fichas, roadmap y planes: son registros.

## 3. Decisiones Acordadas y Trade-offs

| Decisión | Alternativa descartada | Por qué |
|---|---|---|
| Archivo en `docs/IA/history/` (usuario) | `docs/interfaz/archivo/`; borrar | `agent-check` ya trata ese prefijo como historia; se puede leer desde el repo |
| Lo histórico de `docs/interfaz/` queda en su lugar (usuario) | Moverlo | 21 archivos de `docs/adr` lo enlazan y no se editan; los 30 KB se logran con que ninguna regla mande leerlo |
| Tres skills (`construir-ui`, `revisar-ui`, `explorar-ui`) | Las dos del borrador (iii) | Sin una skill de exploración, el diagnóstico C2 queda sin camino |
| La ficha del flujo como paso 1 de `construir-ui` | Una cuarta skill | El tope de tres skills del plan; la ficha baja a 150 líneas o menos |
| El catálogo se consulta por búsqueda | Leerlo entero | Pesa 75 KB y rompería el tope de 30 KB de lectura |
| `revisar-ui` con U1–U7, todos bloqueantes | Los U1–U10 de `revision-ui` con "menores" | Una inconsistencia con el sistema bloquea (común 2 del plan) |
| Topes verificados por `agent-check` | Topes solo escritos en el ADR | El riesgo de A es que las reglas vuelvan a crecer |
| `backlog-r11.js` apuntado a las skills nuevas | Dejarlo roto hasta el paso 4 | Cada PR deja el repo coherente por sí solo |

## 4. Invariantes

- El cerco de las skills no se afloja: las dos skills citan el literal `componentes.md` y, además,
  `componentes/listado`. El borrador de (iii) lo aflojaba a "catálogo" y eso no se trae.
- Ninguna regla real del `AGENTS.md` del módulo se pierde sin un destino (§7 de esta spec).
- Lo explorado no se mergea: así `explorar-ui` no choca con el minimalismo del `AGENTS.md` raíz.
- Ningún ADR ni registro histórico se edita.

## 5. Criterios de Aceptación

- **Dado** `.agents/skills/construir-ui` o `revisar-ui` sin `componentes.md` o sin
  `componentes/listado`, **cuando** corre `InterfazFitnessTest`, **entonces** falla.
- **Dado** un adaptador de `.claude/skills/` sin su versión canónica, o con otro frontmatter,
  **cuando** corre `node scripts/agent-check.js`, **entonces** falla `SKILL_ADAPTER_SYNC`.
- **Dado** que una skill de UI pasa de 60 líneas, la regla de 10 o el `AGENTS.md` del módulo de 80,
  **cuando** corre `agent-check`, **entonces** falla `UI_HARNESS_SIZE`.
- **Dado** un agente que toca `cliente-liviano/`, **cuando** suma su lectura obligatoria (la regla,
  el `AGENTS.md` del módulo, la skill y el tono), **entonces** son 30 KB o menos, medidos con `wc -c`.

## 7. Especificación Técnica de Diseño (Technical Design)

Destino de lo que sale de `cliente-liviano/AGENTS.md` (de 185 a 80 líneas):

| Regla anterior | Dónde queda |
|---|---|
| Tabla de skills del proceso | "Lectura obligatoria y skills" del módulo y `docs/interfaz/README.md` |
| Tabla de estructura (paquetes y carpetas, una fila por paquete) | "Stack y pertenencia", resumida; el detalle de cada clase está en el código |
| Pertenencia, `common-lib` y `traceId` | Igual, resumida |
| Plantillas, estados y textos de la ficha | "Plantillas, HTMX y Alpine" y `construir-ui`, pasos 6 y 7 |
| `HX-Request`, `hx-indicator`, `hx-boost` y el `main#contenido` | Igual, resumida |
| Alpine (D31), diálogos y `data-bloquea` | Igual, resumida; la cola de avisos (3 a la vez, 6 s) queda en el catálogo |
| Reglas de estilos, capas, movimiento y migración del backlog | "Estilos", resumida |
| Accesibilidad, anchos, datos y secretos | "Piso y datos" |
| Comandos de validación | Los 4 principales; variantes y boceto, en `explorar-ui` |
| Contraste que axe no mide | `revisar-ui`, U5 |
| La primera pantalla del celular (`-360x640.png`) | `docs/interfaz/README.md`, anchos |
| "Una pantalla está lista" | `docs/interfaz/README.md` y `revisar-ui` |
| Las preguntas al usuario | El módulo, en una línea; las reglas, en `docs/interfaz/README.md` |

Lectura obligatoria medida (bytes, `wc -c`): la regla, 500; el `AGENTS.md` del módulo, 5.858; la
skill más larga (`construir-ui`), 3.689; y el tono, 14.377. En total, 24.424 bytes, unos 24 KB. Sin
contar el `AGENTS.md` raíz (25.459 bytes), que rige para toda tarea del repo.

## 8. Validación

- `mvn spotless:check` y `mvn clean test -pl cliente-liviano -am` en verde.
- `node scripts/agent-check.js` sin fallas y `node scripts/tests/run-tests.js` en verde.
