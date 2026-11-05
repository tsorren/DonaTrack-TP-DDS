# Catálogo de temas para comparar con A/B

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 1 · 2026-10-30 · usado por la skill `iteracion-ab`**

Una comparación A/B cambia **un solo tema**. Todo lo demás queda igual, para que la elección sea
clara. Para sumar un tema, se agrega una fila.

| Tema | Qué cambia entre A y B | Qué queda fijo | Cómo se juzga | Dónde se muestra |
|---|---|---|---|---|
| `direccion-general` | Concepto, color, tipografía y forma juntos (dos mundos distintos) | Contenido y estructura | ¿Cuál expresa mejor el brief? ¿Cuál se recuerda? | `/estilos` y una pantalla de muestra |
| `paleta` | Valores base de color y su uso | Tipografía, forma, contenido | Carácter, armonía, estados distinguibles (siempre AA) | `/estilos` |
| `tipografia` | Familias y escala | Color, forma, contenido | Legibilidad en 360 px, carácter del título | `/estilos` + texto largo |
| `forma` | Radios, bordes y sombras | Color, tipografía | Coherencia, sensación (amable o firme) | `/estilos` |
| `densidad` | Espacios y tamaños | Todo lo demás | Cuánto entra sin agobiar; toque cómodo en celular | Una lista o tabla real |
| `imagen` | Tratamiento de fotos e ilustración | Todo lo demás | ¿La foto real luce? ¿El estilo es propio? | Tarjetas de donación |
| `movimiento` | Transiciones de estado y avisos | Todo lo demás | ¿Explica el cambio? ¿Molesta? | Un flujo con acciones |
| `layout:<pantalla>` | Orden y disposición de una pantalla | Identidad, textos | ¿La tarea principal se ve primero? ¿Funciona en 360 px? | Esa pantalla, con datos de ejemplo |
| `componente:<nombre>` | Diseño de un componente | Identidad | Claridad, estados, toque | `/estilos` |
| `microcopy:<flujo>` | Textos de un flujo | Diseño | ¿Se entiende sin ayuda? ¿Suena a nosotros? | Las pantallas del flujo |
| `layout:navegacion-admin` | Agrupación del menú del admin (9 secciones) | Identidad | Encontrar cada sección en 1 intento | Inicio del admin |
| `modo-oscuro` | Un modo oscuro además del claro | Todo lo demás | ¿Se lee igual de bien? ¿Vale revisar cada pantalla dos veces? | `/estilos` |

## Orden sugerido para la identidad

`direccion-general` → `paleta` → `tipografia` → `forma` → `densidad`. Si la dirección general ya
dejó clara una pieza, ese tema se saltea.
