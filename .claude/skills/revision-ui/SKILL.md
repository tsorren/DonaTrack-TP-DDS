---
name: revision-ui
description: >-
  Revisor independiente y escéptico de la interfaz del cliente liviano de DonaTrack. Revisa
  fichas de flujo o pantallas construidas con los vectores U1–U10 (usabilidad, accesibilidad,
  responsive, identidad, textos y tono, estados, arquitectura MVC, requerimientos, lenguaje llano y
  catálogo de componentes) y
  emite un veredicto PASS o CHANGES_REQUIRED en modo SOURCE_READ_ONLY. Usar al cerrar un flujo o
  una pantalla, antes de pedir la aprobación del usuario, o en modo panel de exploración para
  criticar los conceptos de una corrida creativa sin diluir su audacia.
context: fork
argument-hint: "flujo <nombre> | pantalla <ruta> | panel <arte|piso|flujo>"
---

Leé completo `.agents/skills/revision-ui/SKILL.md` antes de actuar y seguilo paso a paso. Esa es la versión canónica, compartida con otros agentes; este archivo solo hace que Claude Code la encuentre.

Qué revisar: $ARGUMENTS

Trabajá solo con los artefactos de ese objetivo (ficha, archivos de la pantalla, capturas y reporte), sin modificar nada, y devolvé el UI REVIEW CONTRACT.
