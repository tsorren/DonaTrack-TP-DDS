---
name: revisar-ui
description: >-
  Revisa con ojo escéptico y en modo solo lectura pantallas, componentes o fichas de flujo del
  cliente liviano de DonaTrack contra el sistema visual cerrado, comparando las pantallas entre sí,
  y emite PASS o CHANGES_REQUIRED. Usar antes de dar por terminada una vista, un componente o una
  migración de pantallas, y antes de abrir un PR con cambios de interfaz.
---

# Revisar la interfaz

Rigen [`AGENTS.md`](../../../AGENTS.md), [`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md)
y el modo del [Evaluator](../../../docs/IA/review/evaluator.md): **SOURCE_READ_ONLY**. Podés correr
tests y `-Pvisual`; no corregís nada. Recibís solo los artefactos (archivos, capturas, reporte),
no el razonamiento de quien los hizo; si no, declarás `Mode: SELF_REVIEW`.

1. **Capturas.** Las de cada pantalla tocada a 360 y 1280 px (`-Pvisual` las deja en
   `cliente-liviano/target/capturas/`). Miralas lado a lado, con las de otra pantalla que use las
   mismas piezas.
2. **Vectores.** Todo lo que falla en U1 a U7 es BLOQUEANTE:

| Vector | Qué se mira |
|---|---|
| U1 Entre pantallas | La misma pieza se ve y se comporta igual; mismos rótulos, orden de controles y nombres de parámetro; ninguna barra de búsqueda o filtros fuera de `componentes/listado` |
| U2 Sistema | Cada clase está en [`componentes.md`](../../../docs/interfaz/componentes.md) o es heredada; la pieza nueva tiene su fila y su muestra en `/estilos`; sin colores ni medidas literales nuevas |
| U3 Lo pedido | Cumple la ficha o el encargo campo por campo, y el criterio del requerimiento |
| U4 Estados y textos | Vacío, sin resultados, cargando, error y sin permiso con su texto; [tono](../../../docs/interfaz/tono-y-comunicacion.md): lo pendiente se dice pendiente, cifras con período |
| U5 Piso | AA sin errores en el reporte de axe, foco visible, toque de 48 px, sin desbordes ni palabras cortadas a 360, 1280 y 1920 px; el texto sobre un degradado o una imagen, a ojo (axe no lo mide) |
| U6 Arquitectura | Funciona sin JavaScript; controller sin lógica; Alpine sin `fetch` ni `x-html` |
| U7 Usabilidad | Las 10 heurísticas de [la cátedra](../../../docs/interfaz/criterios-catedra-ux-ui.md): la persona sabe dónde está, qué pasó y cómo seguir |

3. **Menores:** lo que mejora sin romper nada de lo anterior. Se anotan, no bloquean.

Salida:

```text
=== UI REVIEW CONTRACT ===
Mode:    INDEPENDENT_REVIEW | SELF_REVIEW
Target:  <pantallas o ficha>
U1…U7:   NONE_DETECTED | NOT_APPLICABLE | NOT_VERIFIED | <hallazgo>
Verdict: PASS | CHANGES_REQUIRED
BLOCKING: - <archivo:línea o captura> — <qué falla> — <cómo se arregla>
MINOR:    - <mejora>
```

Cada hallazgo cita su evidencia. Con un BLOCKING, CHANGES_REQUIRED: se arregla y se revisa otra vez.
Una propuesta de `explorar-ui` no se revisa con esto: la elige el humano mirándola.
