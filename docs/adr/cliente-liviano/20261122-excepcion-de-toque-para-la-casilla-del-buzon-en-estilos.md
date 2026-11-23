# Excepción de toque para la casilla del buzón en `/estilos`

- Status: proposed
- Date: 2026-11-22
- Deciders: el usuario («Mantenerla», 2026-11-22, registrado como DA-16 en el
  [backlog](../../interfaz/backlog-guia-al-codigo.md), sección 2.2). La excepción la escribió el
  cierre de la ola C0 (`e7ba71c36`) sin consultar; este registro, la sesión que lanzó la ola.
  Pendiente de aprobación del grupo en el PR
- Tags: cliente-liviano, interfaz, accesibilidad, verificacion-visual, entrega-7

> **Origen:** ola C0 de la guía «Huerta y galpón» ([DTI-46](../DEUDA_TECNICA.md#dti-46), ítem 2).
> Complementa el [ADR de la verificación visual](./20261030-verificacion-visual-con-playwright-y-axe.md),
> que no se edita (`AGENTS.md` §2 y §9.5).

## Contexto y Problema

- `[DOCUMENTED]` El [ADR de la verificación visual](./20261030-verificacion-visual-con-playwright-y-axe.md)
  fija que `VerificacionVisualTest` falla si un control visible mide menos de 48 × 48 px. La única
  excepción son los enlaces dentro de un texto, como admite WCAG 2.5.8.
- `[OBSERVED]` Por pedido del usuario del 2026-11-12, la casilla de los buzones mide un cuarto de
  48 px. Lo que se toca es su etiqueta `.buzon-casilla`, que mide 48 × 48 px
  (`cliente-liviano/src/main/resources/static/css/componentes/buzon.css:84-91` y `:157-166`).
- `[OBSERVED]` La medición del test (`CONTROLES_CHICOS`, `VerificacionVisualTest.java:91`) mira el
  `input` y no su etiqueta. Por eso `/estilos`, que muestra la demo de la bandeja, falla 9 veces
  (dos casillas, en los tres anchos). Las mismas 9 fallas están en `093aed884`, antes de la ola C0
  (`gate2-estilos-base.log` de la corrida). Las tres bandejas reales fallan igual.
- `[OBSERVED]` En el cierre de la ola C0, el agente agregó `EXCEPCIONES_CONOCIDAS`
  (`VerificacionVisualTest.java:85`). Tomó como decisión del usuario un texto que no lo era: la
  respuesta «Opción B», que corregía DA-14a. El usuario la revisó después y eligió mantenerla.

## Alternativas consideradas

1. **Mantener la excepción acotada** (la de `e7ba71c36`): ruta, tipo y detalle exactos, solo en
   `/estilos`. Las fallas exceptuadas van a `excepciones.json`, aparte de `reporte.json`.
2. **Revertirla:** `/estilos` vuelve a fallar el piso hasta que se resuelva de otra forma.
3. **Medir la casilla por su etiqueta:** cambiar `CONTROLES_CHICOS` para que un `input` dentro de
   una etiqueta de 48 px cuente como de 48 px. Arregla también las tres bandejas.
4. **Volver la casilla a 48 px:** deshace el pedido del usuario del 2026-11-12.

## Resultado de la decisión

Alternativa 1, elegida por el usuario («Mantenerla», 2026-11-22).

## Consecuencias

- Positivas: `/estilos`, la pantalla de la ola C0, vuelve a pasar el piso sin cambiar la casilla ni
  la medición. Cualquier otra falla de toque en `/estilos` sigue contando. Las exceptuadas quedan a
  la vista en `excepciones.json`, y un test fija el alcance
  (`soloLasCasillasDeLaDemoDeLaBandejaEnEstilosSonExcepcion`, `VerificacionVisualTest.java:430`).
- Negativas: hay una lista de excepciones en el gate y hay que mantenerla. Las tres bandejas
  (`/admin-notificaciones`, `/donante-notificaciones` y `/entidad-notificaciones`) siguen fallando
  por la misma causa. Diverge del ADR de la verificación visual hasta que el grupo apruebe este.

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| 1. Mantener | Ya está hecha y probada; es la más acotada | Deja las bandejas en rojo; suma una lista que puede crecer |
| 2. Revertir | El gate queda como lo fija su ADR | `/estilos` sigue en rojo por algo que no es un defecto |
| 3. Medir por la etiqueta | Resuelve la causa en todo el sitio | Cambia cómo mide el gate: pide su propia revisión |
| 4. Casilla de 48 px | Sin excepciones | Deshace una decisión del usuario |

## Trabajo futuro

- La alternativa 3 sigue abierta para cuando se quiera limpiar las bandejas. Si se adopta, este
  ADR pasa a `superseded`, con la aprobación humana.
- Seguimiento en [DTI-46](../DEUDA_TECNICA.md#dti-46), ítems 2 y 3.
