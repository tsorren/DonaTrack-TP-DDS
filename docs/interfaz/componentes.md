# Catálogo de componentes y capas CUBE

> Índice para los agentes y las personas: lo primero que se lee antes de construir o revisar una
> pantalla (skills `vista-ui` y `revision-ui`). Lo pide el
> [ADR de interacción, movimiento y componentes](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
> (`proposed`). La versión visual es `/estilos` (RF-CL-05), con el mismo orden.
>
> **Estado (2026-11-04):** fases 0 y 1 del [backlog de migración](backlog-migracion-componentes.md)
> hechas (composición, utilidades, movimiento con las animaciones con nombre de las pantallas y la
> muestra del catálogo) y, de la fase 2, botón, campo de formulario y sello de estado (B2-01 a B2-03)
> y aviso breve con su cola de Alpine, banner, diálogo, estado vacío, indicador de carga y esqueleto
> (B2-04, B2-05 y B2-09). Los demás componentes (navegación, tarjeta, tabla, carga de fotos,
> pestañas, piezas de la entidad y mapa) todavía viven en las hojas de pantalla: figuran como heredados
> (ver abajo). Mientras tanto, su patrón está en
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
  pantalla de hoy) gana a todas, hasta que se migre: por eso una regla sin capa de un elemento (como `a { color }`) le ganaría a un componente, y las de base viven en la capa `pico`.

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
llaman `mov-*` y viven solo en `movimiento.css` (el cerco de `InterfazFitnessTest` falla si una
hoja de la carpeta `css/` que no es `movimiento.css`, `app.css` ni `tokens.css` declara `@keyframes` o
usa `--movimiento-duracion-*`, `--movimiento-curva-*` o una espera `--movimiento-retraso-*` que no sea
`retraso-ninguno`).

### Animaciones con nombre

Fase 1 del backlog. Las animaciones de una pantalla o una identidad (la portada, la lámina de contacto,
el menú completo) no se piden con `data-movimiento`: la hoja de pantalla las referencia completas, con
una custom property de `movimiento.css`, por ejemplo `animation: var(--movimiento-animacion-registro)` o
`transition: var(--movimiento-transicion-atenua)`. Cada una junta `@keyframes`, duración, curva y espera, siempre
de los tokens. Las custom properties existen solo con movimiento permitido: con "reducir movimiento"
la declaración queda sin valor y el elemento no se anima. Una animación nueva se agrega acá, en
`movimiento.css` y en `/estilos`.

| Custom property | `@keyframes` | Qué hace | Tokens (duración · curva · espera) |
|---|---|---|---|
| `--movimiento-animacion-aparece` | `mov-aparece` | Fundido | media · base · — |
| `--movimiento-animacion-aparece-cierre` | `mov-aparece` | Fundido al final de una coreografía | media · base · cierre |
| `--movimiento-animacion-registro` | `mov-registro` | Se imprime: aparece con la segunda tinta corrida (7 px, −6 px) | larga · entrada · entrada |
| `--movimiento-animacion-registro-fino` | `mov-registro-fino` | Igual, con corrimiento de 4 px (panel de una pestaña) | media · entrada · — |
| `--movimiento-animacion-traza` | `mov-traza` | Dibuja un trazo SVG con `pathLength="1"` | trazo · enfasis · entrada |
| `--movimiento-animacion-traza-raiz` | `mov-traza` | Dibuja un trazo después del brote | trazo · enfasis · trazo |
| `--movimiento-animacion-recorta` | `mov-recorta` | Revela un trazo de izquierda a derecha con `clip-path` | trazo · enfasis · entrada |
| `--movimiento-animacion-baja` | `mov-baja` | Aparece bajando 6 px desde arriba | media · base · sello |
| `--movimiento-animacion-imprime` | `mov-imprime` | Un sello aparece desde escala 1,4 | media · salida · sello |
| `--movimiento-animacion-hoja-sube` | `mov-hoja-sube` | Una hoja (menú completo) sube desde abajo | corta · salida · — |
| `--movimiento-animacion-viento`, `--movimiento-animacion-viento-fruto` | `mov-viento`, `mov-viento-fruto` | Las hojas y los frutos se mecen en ciclo, sin fin | ambiente · ambiente · ambiente |
| `--movimiento-transicion-atenua` | — | Transición de opacidad al pasar o enfocar | corta |
| `--movimiento-transicion-dialogo` | — | Un diálogo (`.dialogo`) aparece y se va con un fundido; `overlay` y `display` con `allow-discrete` para que la salida también se vea. Va con `@starting-style` en la hoja del bloque | media · salida · — |

`mov-crece-alto` (el tronco crece de arriba) y `mov-asiento` (un asiento aparece subiendo) no son
custom properties: son `@keyframes` para las líneas de tiempo del scroll
(`animation: mov-asiento linear both` más `animation-timeline` y `animation-range` en la hoja de
pantalla, que es disposición). Esas reglas dejan `animation-delay: var(--movimiento-retraso-ninguno)`
para anular la espera de la animación del tiempo: es un reset, no una duración.

## Componentes (bloques)

Hoja: `static/css/componentes/<componente>.css`, capa `bloques`, cargada desde `app.css`. Fragmento
en `templates/componentes/<componente>.html`, con parámetros con nombre y slots (`~{::demo}`). Sus
variantes van por atributo (`data-estado`, `data-tono`, `data-tamano`), en la misma hoja del bloque, después de la base (la capa `excepciones` queda para cuando una variante deba ganarle a otro bloque).

| Componente | Fragmento | Parámetros | Slots | Estados y excepciones | Movimiento | Patrón |
|---|---|---|---|---|---|---|
| `.muestra` (la ficha de una pieza en `/estilos`) | `componentes/muestra :: muestra(nombre, cuando, cuerpo)` | `nombre`, `cuando` | `cuerpo` | — | — | Tarjeta de documentación (Material: card) |
| `.boton` | Sin fragmento: el elemento nativo, `<button class="boton">` o `<a class="boton" href>` | — | El texto del botón | `data-tono`: `principal` (la acción de la pantalla), `secundario` o `quitar` (dar de baja). `data-tamano="chico"`: más bajo, en contorno; con `data-tono="principal"` queda lleno y solo más angosto. `hidden` lo oculta. Al apretar se hunde sobre su sombra | `:active` | Material: Buttons (filled, outlined, text) |
| `.campo` y `.campo-error` | Sin fragmento: `<div class="campo">` con `label`, el control nativo (`input`, `select` o `textarea`), una ayuda `small` opcional y, si falla, `<p class="campo-error">` | — | La etiqueta, el control, la ayuda y el error | `data-estado="error"` (borde del control en el color del problema; el mensaje va en `.campo-error` y se enlaza con `aria-describedby`). `input[type="number"]` es angosto; `textarea` se estira en vertical | — | Material: Text fields |
| `.estado` y `.estado-sello` | `componentes/sello :: sello(estado)` (el dibujo) dentro de `<span class="estado" data-estado="…">` con la palabra al lado | `estado` | — | `data-estado`: donante `pendiente`, `en-camino`, `entregada`, `con-problema`; entidad `puerta`, `camino`, `salir`, `recibida`, `problema`. El fragmento dibuja además `hecho` y `falta` (etapas de un recorrido) y cualquier otro valor lo dibuja como problema. El color solo acompaña: el estado va siempre en palabras | — | Material: Chips / badges |
| `.sello-*` (tinta, disco, contorno, contorno--punteado, medio, problema, calado, calado--fino, tilde, hecho, falta) | Las piezas del dibujo del sello, para el SVG de `componentes/sello` y para los dibujos de las pantallas | — | — | — | — | — |
| `.aviso-breve` (toast), `.avisos` y `.aviso-cerrar` | Sin fragmento: `<p class="aviso-breve" data-estado="hecho" role="status">` en el flujo de la página, o el mismo aviso, sin `role` (la región ya es `aria-live`), con un `<button class="aviso-cerrar" data-cerrar aria-label="Cerrar el aviso">×</button>` que trae HTMX a la región `<div id="avisos" class="avisos">` de los layouts | — | El texto (y, en el aviso del flujo, un título `h2` y un `p`) | `data-estado="hecho"` (filete del color de acento, texto en negrita). La región es fija abajo (en el celular, sobre la barra del menú), `aria-live="polite"`. La cola (`Alpine.data("avisos")`) muestra 3 a la vez y deja el resto oculto, los saca a los 6 s (no antes de 5 s; no se van mientras el puntero o el foco están encima) y quita el de `data-cerrar`. Sin JavaScript quedan a la vista | Fundido de entrada (`--movimiento-animacion-aparece`) y de salida (`--movimiento-transicion-atenua`), solo dentro de la región | Material: Snackbar |
| `.banner` | Sin fragmento: `<section class="banner" aria-labelledby>` con `h2`, `p` y el próximo paso (un `a.boton` o un `button`) | — | El título, el texto y la acción | `data-estado="error"` (filete y título del color del problema; `role="alert"` solo si es urgente; `.aviso-codigo` lleva el código de seguimiento). Sin estado, informativo | — | Material: Banners |
| `.vacio` | Sin fragmento: `<section class="vacio" aria-labelledby>` con `h2`, `p` y, si hay, el botón del paso posible | — | El título, el texto y la acción | Mismo papel que el banner. Con el brote del donante (`.aviso-brote`, heredada) | — | Material: Empty states |
| `.cargando` y `.esqueleto` | Sin fragmento: `<p class="cargando" role="status">` con el texto de lo que se trae y, dentro de una región con `aria-busy="true"`, los `<div class="esqueleto">` | — | El texto; los lugares de las partes | `.esqueleto`: `data-forma="punteado"` (contorno punteado) y `data-tamano="alto"` (más altura). Sin datos inventados | — | Material: Progress indicators |
| `.dialogo` y `.dialogo-titulo` | Sin fragmento: `<dialog class="dialogo" popover aria-labelledby>` y, en la página, un `<button popovertarget="…">` que lo abre y un botón con `popovertargetaction="hide"` que lo cierra | — | El título (`.dialogo-titulo`), el contenido y los botones | `data-tono="quitar"` (confirmar algo irreversible: borde del color del problema, más angosto, el foco entra en "No, dejarla" con `autofocus`). Se abre y se cierra sin JavaScript, con Esc o tocando afuera; el navegador devuelve el foco al botón | Fundido de entrada y de salida (`--movimiento-transicion-dialogo`, con `@starting-style`) | Material: Dialogs / HIG: Alerts |

Los demás componentes (navegación, tarjeta, tabla, carga de fotos, pestañas, piezas de la entidad y
mapa) se agregan acá al cerrar su ítem de la fase 2 del backlog.

**Quedan como clases heredadas**, de una sola pantalla: en la portada, `aviso` y `aviso--suelo` (el
estado sin datos en un marco de filete, con otro aspecto que el banner; B3-01), `foto-ventana` y
`foto-titulo` (su propia ventana de la foto); `aviso-brote` (el dibujo del brote de un estado vacío del
donante), `aviso-acciones` (los botones de un banner de Ingresar), `aviso--nombre` y `aviso-codigo`
(el renglón de código de un banner de error); y `hueco` (el esqueleto de la entidad, sin fondo y más
bajo) y `seguimiento-reserva` (el de Seguir entregas del donante).

**Indicadores de carga que hoy son banner:** los «Cargando…» de Entregas, Entidades y Mapa públicos
(`publico-donaciones`, `publico-entidades` y `publico-mapa`) usan `.banner`, no `.cargando`, porque hoy
son una lámina y pasarían a un texto en negrita; se unifican en la fase 3 si el usuario lo confirma.

Las variantes por atributo van en la misma hoja del bloque, después de la base: la capa `excepciones`
queda para cuando una variante necesite ganarle a otro bloque. El tamaño, la sombra y el color de los
botones de una pantalla (por ejemplo `.boton--foto` o `.boton--suelo`) siguen en la hoja de la
pantalla como clases heredadas, hasta la fase 3.

## Alpine (D31)

`static/js/interfaz.js` registra los `Alpine.data()`. Cada uno entra acá con su nombre, qué estado
maneja y qué plantilla lo usa. Reglas en
[`cliente-liviano/AGENTS.md`](../../cliente-liviano/AGENTS.md#alpine-d31).

| `Alpine.data()` | Estado que maneja | Dónde se usa | Detalle |
|---|---|---|---|
| `avisos` | La cola de avisos breves: cuáles se ven (3 a la vez; el resto, `hidden`), el reloj de cada uno (6 s) y su salida con un fundido (`data-saliendo`). Si no hay fundido ("reducir movimiento"), lo quita enseguida | La región `#avisos` de `layout-privado` y `layout-publico` (`x-data="avisos"`) | El contenido de cada aviso lo trae el servidor (HTMX lo agrega a la región, o viene con la página); Alpine no arma texto. Pausa mientras el puntero o el foco están sobre la región (`pausar`, `seguir`) y cierra el aviso cuyo botón lleva `data-cerrar` (`cerrar`) |
| `avisoDeMuestra` | Nada: agrega a la región un aviso armado con el `<template id="aviso-de-muestra">` de la página | Solo `/estilos` | Sirve para ver la cola; en el sistema el aviso llega de HTMX |
