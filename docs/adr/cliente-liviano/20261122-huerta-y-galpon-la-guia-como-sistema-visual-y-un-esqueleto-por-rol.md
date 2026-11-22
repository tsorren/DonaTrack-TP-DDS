# «Huerta y galpón»: la guía versionada es el sistema visual del cliente liviano, con un esqueleto por rol

- Status: proposed
- Date: 2026-11-22
- Deciders: el usuario (2026-11-20, al elegir la mezcla C; registrado como G-02 en el
  [backlog](../../interfaz/backlog-guia-al-codigo.md), sección 2.1); este registro, el agente del
  lote `c0-adr` de la ola C0. Pendiente de aprobación del grupo en el PR
- Tags: cliente-liviano, interfaz, identidad, guia, entrega-7

> **Origen:** ola C0 del backlog [la guía «Huerta y galpón» al código](../../interfaz/backlog-guia-al-codigo.md)
> (ítem C0-01). Cambiar la identidad pide un ADR ([`docs/interfaz/README.md`](../../interfaz/README.md),
> tabla «Quiero… / Hago…», fila «Cambiar la identidad»). La decisión consta en notas de sesión que
> no están en el repo; el backlog, sección 2.1, es su registro versionado.

## Contexto y Problema

- `[DOCUMENTED]` El [ADR de la piel Huerta](./20261110-piel-huerta-con-abejas-como-identidad-del-cliente-liviano.md)
  (`proposed`) dejó como identidad la variante K, «Huerta con abejas»: su piel (papel crema, verde
  hoja, Archivo y DM Sans) y su composición en todo el sitio. Su descripción vigente es el
  [brief](../../interfaz/identidad/brief.md) y su verdad visual, `/estilos`.
- `[DOCUMENTED]` Entre el 2026-11-20 y el 2026-11-22 se hizo un rediseño en tres fases. Fase 2: cuatro
  direcciones en baja fidelidad (Poda, Acta, Recorrido y Remito), las mismas 7 pantallas a 360 y
  1280 px, sin ranking; después, tres mezclas (A, B y C). El usuario eligió la mezcla C, «Huerta y
  galpón» (backlog, sección 2.1, G-02). Fase 3: la guía de estilo y trece pantallas en alta fidelidad.
- `[OBSERVED]` La guía está versionada en [`docs/interfaz/guia/`](../../interfaz/guia/guia-y-pantallas.html)
  y se arma con `guia/armar.js` desde `guia/partes/`. Sus valores están en
  [`guia/partes/05-tokens.css`](../../interfaz/guia/partes/05-tokens.css). Su encargo: «un esqueleto
  por rol: aireado y cálido para lo público, quien dona y la organización; denso para el admin»
  ([`guia/CRITERIOS-FASE3.md`](../../interfaz/guia/CRITERIOS-FASE3.md)).
- `[VERIFIED]` (backlog, sección 3.2, medido el 2026-11-22): de los 117 tokens de uso de hoy, la
  guía conserva 73 con su nombre (6 cambian de valor), suma 46 nuevos y no trae 44.
- `[OBSERVED]` La guía nombra la huerta, la raíz y los frutos como piezas que «ya son de Raíces»
  (`guia/partes/30-inicio.html`) y dibuja la navegación «a la manera de Raíces»
  (`guia/partes/40-guia.html`). En `guia/partes/` no aparece ninguna abeja.

Problema: ¿qué es la fuente de verdad visual del cliente liviano mientras se lleva el rediseño al
código, y el sitio sigue teniendo una sola composición para todos los roles?

## Atributos de Calidad y Drivers de Decisión

- Lo que eligió el usuario, mirando las opciones.
- Calidez en lo que ven quienes donan, las organizaciones y el público: la foto de la entrega, las
  personas con nombre y el agradecimiento solo por un hecho confirmado (backlog, sección 2.1, fase 2b).
- Densidad para el trabajo del admin: tablas con selección de varias filas y un cajón siempre
  abierto en escritorio (`guia/partes/40-guia.html`, «Listas y tablas» y «Navegación»).
- Una sola fuente de valores: los tokens de la guía se suman a `tokens.css` sin renombrar ninguno
  de los que ya existen (backlog, sección 2.1, G-01).
- El piso de [dirección de arte](../../interfaz/direccion-de-arte.md#el-piso-no-se-negocia):
  contraste AA, controles de 48 px, nada desbordado entre 360 y 1920 px y lo esencial sin JavaScript.

## Relaciones Arquitectónicas

1. **Reemplazaría en parte (`superseded`, a aprobar por el grupo):** el
   [ADR de la piel Huerta](./20261110-piel-huerta-con-abejas-como-identidad-del-cliente-liviano.md).
   - *Se conserva* la piel de K. Paleta, letras y forma siguen, con 73 tokens que conservan su nombre.
   - *Cambia* la composición. Deja de ser una para todo el sitio y pasa a dos esqueletos: la huerta
     para lo público, quien dona y la organización, y el galpón para el admin.
   - *Cambia* la fuente de verdad visual. Pasa del brief y `/estilos` a la guía versionada.
   - Este ADR no cambia el estado de aquel. La relación la decide el grupo.
2. **Se apoya en:** [una sola identidad](./20261103-una-sola-identidad-en-el-cliente-liviano.md)
   (`accepted`). Dos esqueletos no son dos identidades. Comparten tokens, letra, sello y piezas.
   Se sirven en `/` y `/<pantalla>`, sin `?alternativa=`: el esqueleto sale del rol de la pantalla,
   no de una elección de quien la mira. La propuesta se exploró aparte y entra por una rama
   (`E7_guia_ui`), como fija ese ADR.
3. **Cambia, por decisión del usuario, dos supuestos del** [harness de interfaz](./20261115-harness-de-interfaz-con-sistema-cerrado-y-tres-skills-cortas.md)
   (`proposed`).
   - Ese ADR dice que la identidad «no se reabre por ahora». El usuario la reabrió con este rediseño.
   - Mientras dura el backlog, la verdad visual es la guía y no `/estilos`. `/estilos` vuelve a
     serlo cuando cada ola lleva al código lo que la guía dibuja.
   - El resto del harness sigue igual: sistema cerrado, catálogo, kit de listas, `InterfazFitnessTest`
     y las tres skills.
4. **Respeta:** [sin Pico.css](./20261114-sin-pico-css-en-el-cliente-liviano.md) (`proposed`): la
   base es CSS propio con tokens.
5. **Respeta:** [ADR-E6-06](./20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md)
   (`proposed`) en el stack (Thymeleaf, HTMX y `tokens.css` en dos niveles) e
   [interacción, movimiento y componentes](./20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
   (`proposed`) en las capas CUBE y el movimiento con tokens.

## Alternativas Consideradas

Las de la fase 2 (2026-11-20), con el motivo del descarte tal como lo registra el backlog
(sección 2.1). Se mostraron sin ranking y eligió el usuario mirándolas. Por eso no hay matriz
ponderada: ponerle puntaje ahora inventaría un criterio que no se usó.

| Alternativa | Qué era | Por qué no |
|---|---|---|
| Poda | Evoluciona Raíces, densidad media: una sola metáfora y la tarea primero | No sola: el usuario pidió mezclarla con Acta, «con un poco más de calidez» y «detalles de diseño gráfico para embellecer» |
| Acta | Evoluciona Raíces, densidad alta, escritorio primero: cada registro con su prueba | No sola: el mismo pedido de mezcla |
| Recorrido | Evoluciona Raíces, densidad baja, celular primero: la línea como columna vertebral | El usuario eligió mezclar Poda y Acta |
| Remito | Rompe con Raíces: otra identidad, la donación como comprobante | El usuario eligió mezclar Poda y Acta |
| Mezcla A · Cuaderno | Esqueleto de Poda; cada fila con la prueba de Acta (foto, quién recibió, cuándo); densidad media | El usuario eligió C |
| Mezcla B · Herbario | Esqueleto de Acta (cajón, tablas, lista con detalle) con «para mirar hoy» de Poda; densidad alta | El usuario eligió C |
| **Mezcla C · Huerta y galpón (elegida)** | Un esqueleto por rol: Poda con más aire para lo público, quien dona y la organización; Acta para el admin | De las tres mezclas, la que más calidez pone |
| Seguir con «Huerta con abejas» (status quo) | La composición única de K en todo el sitio, sin rediseño | El usuario pidió el rediseño y eligió C |

### Análisis por alternativa

Lo que sigue, salvo el motivo del descarte, es `[INFERRED]`.

- **Poda.** A favor: una sola metáfora y la tarea primero, buena para quien dona y la organización.
  En contra: densidad media para el admin, que trabaja con muchas filas a la vez.
- **Acta.** A favor: cada registro con su prueba; rinde en el escritorio del admin. En contra:
  densidad alta y escritorio primero en roles que usan el celular.
- **Recorrido.** A favor: celular primero y la línea de la entrega como eje. En contra: poca
  densidad para el admin.
- **Remito.** A favor: la donación como comprobante es clara. En contra: rompe con Raíces y con la
  piel ya elegida, y obliga a rehacer todo lo reconocible.
- **Mezcla A.** A favor: la prueba de Acta en cada fila. En contra: un solo esqueleto de densidad
  media para todos los roles.
- **Mezcla B.** A favor: densidad y orden del admin. En contra: el mismo esqueleto denso para lo
  público y quien dona.
- **Mezcla C.** A favor: cada rol recibe la densidad que pide su trabajo, sobre una sola piel. En
  contra: dos esqueletos que mantener y un cerco que tiene que valer para los dos.
- **Status quo.** A favor: nada que migrar. En contra: deja sin resolver la densidad del admin y la
  calidez que pidió el usuario.

## Resultado de la Decisión

`[PROPOSED]` Mezcla C, «Huerta y galpón»:

1. La guía versionada en [`docs/interfaz/guia/`](../../interfaz/guia/guia-y-pantallas.html) es el
   sistema visual del cliente liviano. Sus valores están en `guia/partes/05-tokens.css`. Donde la
   guía y el catálogo de hoy difieren, manda la guía.
2. Hay un esqueleto por rol. La **huerta** es aireada y cálida y la usan lo público, quien dona y
   la organización: barra superior y, en el celular, barra inferior con 4 destinos. El **galpón**
   es denso y lo usa el admin: cajón siempre abierto en escritorio y tablas con selección de varias
   filas. Los dos comparten tokens, letra, sello y piezas.
3. Los tokens de la guía se suman a `tokens.css` y ningún nombre existente cambia (G-01). En el
   producto no hay consultas de contenedor para los quiebres. Los valores en `cqi` pasan a `vw` y
   los quiebres usan `--medida-quiebre-*`.
4. El rediseño entra al código por olas, con el workflow `oleada-ui`, según el
   [backlog](../../interfaz/backlog-guia-al-codigo.md). Estado de implementación: en curso desde la
   ola C0 (tokens).

## Consecuencias

- **Positivas:**
  - La dirección elegida queda registrada con sus alternativas y sus motivos.
  - Los agentes de cada ola leen una sola fuente de valores y de piezas.
  - El admin gana densidad sin quitarle calidez al resto.
- **Negativas:**
  - Mientras dura el backlog, conviven pantallas ya migradas y pantallas sin migrar, que se ven
    inconsistentes con los tokens nuevos. Regla del backlog: no se arreglan fuera de su ola.
  - Dos esqueletos que mantener.
  - Quedan textos que nombran otra fuente de verdad o la identidad anterior. Se actualizan en sus
    olas o cuando el grupo acepte este ADR:
    - en [`cliente-liviano/AGENTS.md`](../../../cliente-liviano/AGENTS.md), «Verdad visual: `/estilos`»
      y el brief como intención;
    - el [brief](../../interfaz/identidad/brief.md);
    - el [catálogo de componentes](../../interfaz/componentes.md).
- **Abierto (no lo decide este ADR):**
  - qué pasa con las ilustraciones y las piezas de K que la guía no dibuja, como las abejas;
  - las decisiones pendientes de la ronda de la fase B (backlog, sección 2.2).
- **Riesgo:** si el grupo rechaza este ADR, las olas ya integradas en `E7_guia_ui` tendrían que
  volver a la composición de K. Los tokens nuevos se pueden quedar: solo se suman.
