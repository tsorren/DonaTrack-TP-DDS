---
name: ux-flujo
description: >-
  Diseña un flujo del cliente liviano de DonaTrack antes de escribir código: para quién es, dónde
  está en el sitio, recorrido con desvíos, pantallas, estados, textos, accesibilidad y revisión con
  las 10 heurísticas. Usar al empezar cualquier pantalla o flujo nuevo (por ejemplo, confirmar
  recepción, registrar donación o importar CSV), o cuando el usuario pide rediseñar uno.
---

# Skill: ux-flujo — Diseño de un flujo

> **Ámbito:** cliente liviano (E6).
> **Alineación:** [`docs/interfaz/README.md`](../../../docs/interfaz/README.md),
> [`plantillas/flujo.md`](../../../docs/interfaz/plantillas/flujo.md) y
> [`patrones-y-microcopy.md`](../../../docs/interfaz/patrones-y-microcopy.md) (RNF-UI-01 a 06).

## 1. Juntar el contexto

- El requerimiento y su criterio de aceptación:
  [`requerimientos-e6.md` §6](../../../docs/requerimientos/requerimientos-e6.md).
- Los huecos de backend que lo afectan (§8, HB-xx).
- El boceto de E1: ubicarlo en [`bocetos-e1.md`](../../../docs/interfaz/bocetos-e1.md) y leer la
  página (cómo leer el PDF: README, "Cómo leer los bocetos de E1").
- El lugar en el [mapa del sitio](../../../docs/interfaz/mapa-del-sitio.md).
- Los contratos: `docs/arquitectura/contratos/openapi-*.yaml`.

## 2. Completar la ficha

Copiar la plantilla a `docs/interfaz/flujos/<flujo>.md` y completar sus 9 secciones:

- El **boceto es punto de partida**. Si una pantalla mejora alejándose de él (por ejemplo, una
  tabla que en 360 px pasa a tarjetas), se cambia y el motivo va a la
  [bitácora](../../../docs/interfaz/bitacora.md).
- **Cada pantalla tiene todos sus estados**, con su texto, siguiendo las reglas de textos.
- **Nada de reglas de negocio inventadas.** Si algo no está en los requerimientos ni en los
  contratos, va a "Preguntas abiertas".

## 3. Preguntar lo que falta

Con `AskUserQuestion`, solo lo que el usuario sabe y los documentos no dicen: como máximo 3
preguntas por ronda, opción recomendada y palabras simples. Ejemplos de preguntas bien hechas:

- "Cuando llegan dos entregas juntas, ¿la entidad las confirma de a una o todas juntas?" → "De a
  una (Recomendada): si una foto falla, no se traba la otra" · "Todas juntas, como el boceto".
- "¿Qué pasa si la entrega llegó incompleta?" → "Se confirma igual y se reporta el problema
  (Recomendada)" · "No se confirma hasta resolverlo".

## 4. Comparar donde haya dudas

Si una pantalla tiene más de una buena disposición, correr `iteracion-ab` con el tema
`layout:<pantalla>`.

## 5. Revisar y aprobar

1. Correr `revision-ui` en modo **flujo** (vectores U1, U5, U6, U8 y U9) y corregir lo bloqueante.
2. Mostrar al usuario un resumen de 5 líneas: qué hace el flujo, cuántas pantallas tiene, el
   cambio más importante respecto del boceto y las decisiones tomadas. Pedir la aprobación con una
   sola pregunta.
3. Marcar la ficha como `aprobado`, con la fecha y quién aprobó.

## Criterio de salida

- [ ] Ficha completa, sin "a definir".
- [ ] Todos los estados con su texto.
- [ ] Desvíos del boceto anotados en la bitácora.
- [ ] Revisión sin hallazgos bloqueantes y aprobación del usuario.
