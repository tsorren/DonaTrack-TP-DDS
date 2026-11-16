# La piel Huerta con abejas (variante K) reemplaza la composición de Raíces en el cliente liviano

- Status: proposed
- Date: 2026-11-10
- Deciders: a confirmar por el grupo (la decisión de diseño la tomó el usuario el 2026-11-08)
- Tags: cliente-liviano, interfaz, identidad, entrega-6

> **Origen:** revisión de la PR 959 ("E6 ajustes visuales"), 2026-11-10. El cambio de identidad
> figura en el [brief](../../interfaz/identidad/brief.md) y en la [bitácora](../../interfaz/bitacora.md)
> como decisión del 2026-11-08, pero no tenía ADR. `[OBSERVED]` El código de la PR ya implementa
> esta decisión; este ADR la documenta para que el grupo la acepte o la rechace (AGENTS.md §9.3).

## Contexto y Problema

- `[DOCUMENTED]` El [ADR de una sola identidad](./20261103-una-sola-identidad-en-el-cliente-liviano.md)
  (`accepted`) dejó a Raíces como la identidad del sitio, servida en `/` y `/<pantalla>`. Fijó también
  cómo se cambia: una propuesta nueva se diseña sobre el sitio, en su rama, y el merge decide (P1).
- `[DOCUMENTED]` El 2026-11-08 el usuario adoptó primero la piel de la variante K ("Huerta con
  abejas", del prototipo `frontend-donatrack`) sobre la estructura de Raíces. Después pidió la
  composición entera de K: las piezas de K reemplazan a las de Raíces (frutos, libreta, red, planta,
  tierra, pasos y pausa) en todo el sitio ([bitácora](../../interfaz/bitacora.md), entradas del
  2026-11-08 "Piel Huerta", "Todo el sitio con la composición de la variante K" e "Ilustraciones de
  diseño").
- `[OBSERVED]` En la rama `E6_AjustesVisuales` (PR 959) las pantallas usan el componente `huerta`
  (`templates/componentes/huerta.html`), las ilustraciones de `static/img/ilustraciones/` y la
  paleta, letra y forma de K en `tokens.css`. Los SVG fuente de la diseñadora están en
  [`docs/interfaz/identidad/fuentes/`](../../interfaz/identidad/fuentes/README.md).

Problema: ¿el sitio sigue siendo "Raíces con otra piel" o cambia de identidad? Las fichas de flujo,
el catálogo de componentes y los textos que dicen "Raíces" dependen de la respuesta.

## Atributos de Calidad y Drivers de Decisión

Coherencia visual con lo que el usuario y el equipo eligieron 35 · trazabilidad de las decisiones 25 ·
costo del cambio 20 · reversibilidad 20.

## Relaciones Arquitectónicas

1. **Reemplazaría en parte (`superseded`, a aprobar por el grupo):** el
   [ADR de una sola identidad](./20261103-una-sola-identidad-en-el-cliente-liviano.md), solo en
   *cuál* es la identidad (Raíces pasa a ser Huerta con abejas). Sigue vigente lo demás: una sola
   identidad, servida en `/`, sin `?alternativa=`, propuestas en su rama y el archivo histórico.
2. **Respeta:** [ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (mismo stack, dos niveles de tokens, MapLibre solo desde el WebJar).
3. **Respeta:** [interacción, movimiento y componentes](./20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
   (las animaciones de las abejas usan las animaciones con nombre y respetan "reducir movimiento").

## Alternativas Consideradas

Puntajes de 1 a 5; estimaciones del agente (`[INFERRED]`).

| Alternativa | Coherencia | Trazabilidad | Costo | Reversible | **Total** |
|---|:-:|:-:|:-:|:-:|:-:|
| **A. Huerta con abejas es la identidad: composición, piezas e ilustraciones de K en todo el sitio** | 5 | 4 | 3 | 3 | **4,0** |
| B. Raíces con la piel de K (paleta, letra y forma), sin cambiar la composición | 2 | 4 | 4 | 4 | 3,3 |
| C. Volver a Raíces y dejar K como variante A/B | 1 | 5 | 2 | 5 | 3,0 |

- **A.** A favor: es lo que eligió el usuario y lo que ya implementa la PR; una sola metáfora por
  trabajo (plantas para el fruto, raíces para el alcance, abejas para la logística). En contra:
  las fichas de flujo y el catálogo todavía describen piezas de Raíces; hay que actualizarlos.
- **B.** A favor: menos cambio estructural. En contra: el usuario la probó y la rechazó ("no
  mantuviste la estética de la variante k", bitácora del 2026-11-08).
- **C.** A favor: nada que migrar. En contra: deshace el trabajo elegido y vuelve a la convivencia
  de variantes que el ADR anterior retiró.

## Resultado de la Decisión

`[PROPOSED]` Alternativa A: la identidad del cliente liviano es "Huerta con abejas". El
[brief](../../interfaz/identidad/brief.md) (secciones del 2026-11-08) es su descripción vigente; donde
choca con las secciones de Raíces, manda la de K. Los nombres de archivo `raices-*` de las fichas de
flujo se conservan: son direcciones estables, no la identidad.

## Consecuencias

- **Positivas:** la identidad documentada coincide con el código; las skills y los agentes que
  diseñen después leen una sola dirección.
- **Negativas:** quedan textos que nombran a Raíces (fichas de flujo, catálogo de componentes,
  `docs/interfaz/README.md`); se actualizan cuando el grupo acepte este ADR.
- **Riesgo:** si el grupo lo rechaza, la PR 959 tendría que volver a la composición de Raíces.
