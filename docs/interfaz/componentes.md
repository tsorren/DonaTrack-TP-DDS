# Catálogo de componentes y capas CUBE

> Índice para los agentes y las personas: lo primero que se lee antes de construir o revisar una
> pantalla (skills `vista-ui` y `revision-ui`). Lo pide el
> [ADR de interacción, movimiento y componentes](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
> (`proposed`). La versión visual es `/estilos` (RF-CL-05), con el mismo orden.
>
> **Estado (2026-11-04):** fase 0 del [backlog de migración](backlog-migracion-componentes.md)
> hecha en composición, utilidades, movimiento y la muestra del catálogo. Los componentes de la
> fase 2 (botón, campo, sello, aviso, diálogo…) todavía viven en las hojas de pantalla: figuran
> como heredados (ver abajo). Mientras tanto, su patrón está en
> [patrones y microcopy](patrones-y-microcopy.md#componentes-y-su-patrón).

## Cómo se usa

- Si lo que necesitás está acá, se usa como está. No se crea una clase parecida.
- Si falta, se agrega primero acá y en `/estilos` (`templates/estilos.html`), y después se usa. El
  test del cerco (`InterfazFitnessTest`) falla si una clase de la capa CUBE no figura acá y en
  `/estilos`, y si una plantilla usa una clase que no está acá ni en la lista de heredadas.
- Los nombres van en español. La columna "Equivalente" da el nombre conocido (Every Layout, Material
  o HIG) para orientarse.
- **Heredadas:** las clases que las plantillas usaban antes de este catálogo están en
  `cliente-liviano/src/test/resources/verificacion/clases-heredadas.txt`. La lista solo baja:
  al migrar un ítem del backlog, sus clases salen de la lista y entran acá. No se agregan clases
  nuevas a la lista.
- Las capas, en orden (la última gana): `pico, tokens, composicion, utilidades, bloques,
  excepciones, pantallas`, declaradas en `app.css`. Lo que no está en una capa (las hojas de
  pantalla de hoy) gana a todas, hasta que se migre.

## Composición

Hoja: `static/css/composicion.css`, capa `composicion`. El espacio entre hijos se cambia con
`data-espacio` (ver Utilidades).

| Clase | Equivalente | Cuándo | Parámetros (custom properties) |
|---|---|---|---|
| `.pila` | `stack` | Hijos en columna con el mismo espacio entre ellos | `--espacio-entre` (por `data-espacio`) |
| `.racimo` | `cluster` | Hijos en fila que bajan de renglón: botones, etiquetas. `data-alinea`: `centro`, `fin`, `entre` | `--espacio-entre` |
| `.barra-lateral` | `sidebar` | Dos hijos lado a lado; el primero es la barra y el segundo ocupa el resto. En pantalla chica, uno sobre otro | `--medida-barra` (ancho de la barra, 15rem), `--espacio-entre` |
| `.cubierta` | `cover` | Llena la altura de la pantalla; el hijo con `data-centro` queda en el medio | — |
| `.grilla` | `grid` | Tantas columnas como entren | `--medida-grilla` (ancho mínimo de columna, 16rem), `--espacio-entre` |
| `.centro` | `center` | Contenido centrado con ancho máximo y aire a los lados | `--medida-centro` (75rem) |
| `.marco` | `frame` | Fuerza una proporción y recorta lo de adentro. `data-proporcion`: `1-1`, `4-3`, `16-9` (por defecto) | — |

## Utilidades

Hoja: `static/css/utilidades.css`, capa `utilidades`. Una por token de uso.

| Clase o atributo | Token | Cuándo |
|---|---|---|
| `data-espacio="1"` a `"5"` | `--espacio-1` a `--espacio-5` | El espacio entre hijos de una pila, un racimo, una barra lateral o una grilla |
| `.texto-chico`, `.texto-base`, `.texto-grande` | `--tamano-chico`, `--tamano-base`, `--tamano-grande` | Cambiar el tamaño del texto de un elemento |
| `data-tono="suave"`, `"acento"`, `"exito"`, `"aviso"`, `"error"` | `--color-texto-suave`, `--color-acento`, `--color-exito-texto`, `--color-advertencia-texto`, `--color-error-texto` | El color del texto según lo que dice. Siempre con palabras: el color solo no alcanza |

## Movimiento

Hoja: `static/css/movimiento.css`, capa `utilidades`. Se pide con `data-movimiento` en la
plantilla. Duraciones, curvas y distancias salen de los tokens `--movimiento-*` de `tokens.css`:

- duraciones `--movimiento-duracion-` + `corta`, `media`, `larga`, `trazo`, `ambiente`;
- esperas `--movimiento-retraso-` + `ninguno`, `entrada`, `trazo`, `sello`, `cierre`, `ambiente`;
- curvas `--movimiento-curva-` + `entrada`, `enfasis`, `salida`, `base`, `ambiente`;
- distancias `--movimiento-distancia-` + `corta`, `media`, `larga`.

| `data-movimiento` | Tokens | Cuándo | Con "reducir movimiento" |
|---|---|---|---|
| `data-movimiento="aparece"` | `duracion-media`, `curva-base` | Un elemento que se muestra con un fundido | No se anima; queda visible |
| `data-movimiento="sube"` | `duracion-media`, `curva-entrada`, `distancia-media` | Un elemento que se muestra subiendo desde abajo | No se anima; queda visible |
| `data-movimiento="resalta"` | `duracion-trazo`, `curva-salida`, `--color-destacado` | Marcar un momento un dato que cambió | No se anima; queda sin marca |
| `data-movimiento="crece"` | `duracion-larga`, `curva-entrada` | Un elemento que se muestra creciendo desde un poco más chico | No se anima; queda visible |
| `data-movimiento="traza"` | `duracion-trazo`, `curva-enfasis` | Dibujar un trazo SVG; el `path` lleva `pathLength="1"` | No se anima; el trazo queda dibujado |

`data-retraso="entrada"`, `"trazo"`, `"sello"` o `"cierre"` hace esperar a la primitiva
(`--movimiento-retraso-*`), para armar una coreografía. Los `@keyframes` de estas primitivas se
llaman `mov-*` y viven solo en `movimiento.css`. Las animaciones que hoy están en las hojas de
pantalla se mueven acá con la fase 1 del backlog.

## Componentes (bloques)

Hoja: `static/css/componentes/<componente>.css`, capa `bloques`, cargada desde `app.css`. Fragmento
en `templates/componentes/<componente>.html`, con parámetros con nombre y slots (`~{::demo}`). Sus
variantes van por atributo (`data-estado`, `data-tono`), en la capa `excepciones`.

| Componente | Fragmento | Parámetros | Slots | Estados y excepciones | Movimiento | Patrón |
|---|---|---|---|---|---|---|
| `.muestra` (la ficha de una pieza en `/estilos`) | `componentes/muestra :: muestra(nombre, cuando, cuerpo)` | `nombre`, `cuando` | `cuerpo` | — | — | Tarjeta de documentación (Material: card) |

Los demás componentes (botón, campo, sello de estado, aviso, diálogo, navegación, tarjeta, tabla,
estado vacío, carga de fotos, pestañas, piezas de la entidad y mapa) se agregan acá al cerrar su
ítem de la fase 2 del backlog.

## Alpine (D31)

Hoy no hay componentes de Alpine: `static/js/interfaz.js` registra los `Alpine.data()` cuando los
haya. Cada uno entra acá con su nombre, qué estado maneja y qué plantilla lo usa. Reglas en
[`cliente-liviano/AGENTS.md`](../../cliente-liviano/AGENTS.md#alpine-d31).
