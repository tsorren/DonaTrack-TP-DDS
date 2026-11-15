---
name: revision-ui
description: >-
  Revisor independiente y escéptico de la interfaz del cliente liviano de DonaTrack. Revisa fichas
  de flujo o pantallas construidas con los vectores U1–U10 (usabilidad, accesibilidad, responsive,
  identidad, textos y tono, estados, arquitectura MVC, requerimientos, lenguaje llano y catálogo
  de componentes) y emite un
  veredicto PASS o CHANGES_REQUIRED en modo SOURCE_READ_ONLY. Usar al cerrar un flujo o una
  pantalla, antes de pedir la aprobación del usuario, o en modo panel de exploración para criticar
  los conceptos de una corrida creativa sin diluir su audacia.
---

# Skill: revision-ui — Revisión independiente de la interfaz

> **Ámbito:** `docs/interfaz/flujos/` y `cliente-liviano/` (E6).
> **Alineación:** [`docs/IA/review/evaluator.md`](../../../docs/IA/review/evaluator.md) (modos y
> formato), [`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md) y
> [`docs/interfaz/README.md`](../../../docs/interfaz/README.md). **Criterios de la cátedra:**
> [`criterios-catedra-ux-ui.md`](../../../docs/interfaz/criterios-catedra-ux-ui.md) (las 10 heurísticas con su "cómo se consigue", los 7 factores y los patrones conocidos).
> **Catálogo:** [`componentes.md`](../../../docs/interfaz/componentes.md) (D33): se lee primero, es la vara del vector U10.

## 1. Modo

- **SOURCE_READ_ONLY:** no modifica código, documentos ni configuración. Puede correr
  comprobaciones que no destruyen nada: los tests del módulo, `-Pvisual` y la lectura de capturas.
  Si no puede correrlas, lo declara con `[TESTS_NOT_EXECUTED_BY_EVALUATOR]`.
- **Independiente:** recibe solo los artefactos (la ficha, los archivos de la pantalla, las
  capturas y el reporte), no el razonamiento de quien los hizo. Si corre en el mismo contexto, lo
  declara como `Mode: SELF_REVIEW`.
- **Modo flujo:** revisa la ficha con U1, U5, U6, U8 y U9.
- **Modo pantalla:** revisa los 10 vectores, empezando por leer `componentes.md`.
- **Modo panel de exploración:** compara varias propuestas creativas (los conceptos de una
  [`corrida-creativa`](../corrida-creativa/SKILL.md) o de una iteración A/B de estructura) con una
  sola lente (arte, piso o flujo) y el
  [encargo de crítica](../../../docs/interfaz/plantillas/encargo-critico.md). Sección 4.

## 2. Vectores

| Vector | Qué revisa | Hallazgo bloqueante típico |
|---|---|---|
| U1 Usabilidad | Las 10 heurísticas ([preguntas](../../../docs/interfaz/patrones-y-microcopy.md#las-10-heurísticas-como-preguntas)) | El usuario no sabe si su acción funcionó |
| U2 Accesibilidad | Reporte de axe en 3 anchos, foco visible y en orden, etiquetas, nombres accesibles, color con texto | Una violación AA o un control sin nombre |
| U3 Anchos y toque | Capturas en 360, 1280 y 1920; desbordes; controles de 48×48 px | La acción principal no se ve o no se toca bien en 360 px |
| U4 Identidad | Solo tokens de uso; coherencia con el brief; una audacia por pantalla; nada de "Lo que evitamos" (en el panel de exploración, un aviso si la opción declara su motivo) | Colores sueltos, o una pantalla que podría ser de cualquier producto |
| U5 Textos y tono | Reglas de textos; acciones con verbo y resultado; errores que dicen cómo seguir; el [tono común](../../../docs/interfaz/tono-y-comunicacion.md): tono del momento, estados como son, cifras con período y fecha, personas por su rol, el filtro de 7 preguntas | "Error" sin explicación, botones "Aceptar", un paso pendiente que se lee cumplido, una cifra sin período, urgencia o culpa |
| U6 Estados y avisos | Todos los estados de la ficha; aviso adecuado; espera de más de 300 ms con indicador | Falta el estado vacío o el de error del servidor |
| U7 Arquitectura | Controller sin lógica, sin dominio, sin `common-lib`; funciona sin JS; piezas compartidas reutilizadas | Un `hx-post` sin `action` real |
| U8 Requerimientos | Criterio de aceptación del RF; navegación de 3 niveles o menos; desvíos del boceto anotados en la bitácora | Un criterio del RF que no se cumple |
| U9 Lenguaje llano | Lo que ve el usuario del proceso (preguntas, resúmenes, A/B): una decisión por pregunta, 3 o menos por ronda, sin jerga | Una pregunta con dos decisiones o con jerga técnica |
| U10 Catálogo y capas | Cada clase de las plantillas está en [`componentes.md`](../../../docs/interfaz/componentes.md) (o es heredada de la lista del cerco); una clase o un componente nuevo tiene su entrada en el catálogo y en `/estilos`; las hojas de pantalla no declaran colores, fuentes, `@keyframes` ni duraciones; el movimiento sale de `data-movimiento` y de los tokens `--movimiento-*`; el estado del navegador, de Alpine CSP, y el del servidor, de HTMX; la migración del backlog que la tarea debía hacer | Una clase fuera del catálogo, una hoja de pantalla con `@keyframes` o una duración literal, un componente nuevo sin su entrada |

## 3. Veredicto

```text
=== UI REVIEW CONTRACT ===

Mode:    INDEPENDENT_REVIEW | SELF_REVIEW
Target:  flujo <nombre> | pantalla <ruta>

U1 Usabilidad:        NONE_DETECTED | NOT_APPLICABLE | NOT_VERIFIED | <hallazgo + evidencia>
U2 Accesibilidad:     …
U3 Anchos y toque:    …
U4 Identidad:         …
U5 Textos y tono:     …
U6 Estados y avisos:  …
U7 Arquitectura:      …
U8 Requerimientos:    …
U9 Lenguaje llano:    …
U10 Catálogo y capas: …

Verdict:  PASS | CHANGES_REQUIRED

Findings:
  BLOCKING:
    - <archivo:línea o captura> — <qué falla> — <cómo se arregla>
  ADVISORY:
    - <mejora sugerida>
```

- Cada hallazgo cita su evidencia: `archivo:línea`, una captura o una línea del reporte.
- Sin hallazgo, el vector dice `NONE_DETECTED`; no se rellena con texto.
- `CHANGES_REQUIRED` si hay al menos un BLOCKING.

## 4. Panel de exploración

Mientras se explora, la revisión no elige lo seguro: **no diluye la audacia**.

- Por propuesta: lo más fuerte, lo más flojo y los arreglos concretos que **conservan la idea**,
  cada uno con `archivo:línea` o con la captura y la zona.
- `BLOQUEA` solo lo que rompe el piso, la seguridad o la tarea. Un hallazgo que lleve a volver a lo
  de antes no sirve.
- Con más de una propuesta: el ranking, por qué, y qué tomar de las otras para combinar en la
  ganadora.
- No emite `PASS` ni `CHANGES_REQUIRED`: el veredicto de cierre sigue siendo el de los modos flujo
  o pantalla, cuando la propuesta elegida ya está construida.
