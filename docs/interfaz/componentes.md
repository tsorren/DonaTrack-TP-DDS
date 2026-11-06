# Catálogo de componentes y capas CUBE

> Índice para los agentes y las personas: lo primero que se lee antes de construir o revisar una
> pantalla (skills `vista-ui` y `revision-ui`). Lo pide el
> [ADR de interacción, movimiento y componentes](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
> (`proposed`). La versión visual es `/estilos` (RF-CL-05), con el mismo orden.
>
> **Estado (2026-11-05):** fases 0 y 1 del [backlog de migración](backlog-migracion-componentes.md)
> hechas (composición, utilidades, movimiento con las animaciones con nombre de las pantallas y la
> muestra del catálogo) y, de la fase 2, botón, campo de formulario y sello de estado (B2-01 a B2-03)
> y aviso breve con su cola de Alpine, banner, diálogo, estado vacío, indicador de carga y esqueleto
> (B2-04, B2-05 y B2-09), navegación, tarjeta y lista adaptable (B2-06 a B2-08), y subida de fotos con
> vista previa, pestañas, piezas de la entidad y mapa con su lista (B2-10 a B2-13), y el cuaderno de noticias
> del donante (B2-14). De la fase 3, la subtanda 1 de B3-01 (la portada): la lámina «Cómo funciona» es una
> tarjeta con `.tarjeta-cabeza`, la foto de una entrega abre un `.dialogo`, sus avisos son `.vacio` entre
> filetes, sus botones toman `data-forma="foto"` y `data-zona`, y el título y la bajada, dos tokens de tamaño;
> en la subtanda 2, los pasos, la planta, la rama de frutos y la libreta del mes son bloques propios (`.pasos`,
> `.planta`, `.frutos` y `.libreta`), y en la subtanda 3, la tierra, la red y la pausa (`.tierra-*`, `.red` y
> `.pausa`): la hoja de la portada solo dispone y salió de la lista de hojas heredadas del cerco.
> Quedan, en la hoja de su pantalla, el cuaderno de entregas, el libro de donaciones
> y otras láminas de una sola pantalla: figuran como heredadas (ver abajo). Su patrón está en
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
  pantalla de hoy) gana a todas, hasta que se migre: por eso una regla sin capa de un elemento (como `a { color }`) le ganaría a un componente, y las de base viven en la capa `pico`: el color de los enlaces, el anillo de foco (`:focus-visible`, también el de paja sobre la cabecera y el pie) y el título de sección (`main h2`). La textura `.tierra-impresa` vive en la capa `utilidades` por lo mismo: la barra de navegación, que es fija, tiene que ganarle.

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

`--tamano-titulo-portada` y `--tamano-bajada-portada` (B3-01) no tienen utilidad: son el tamaño del título y
de la bajada de la portada, que crecen con el ancho de la pantalla, y los usan `.hero h1` y `.hero-bajada` de
`app.css`.

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
pantalla, que es disposición, o en el bloque que la usa: `mov-asiento` la pide `.libreta`). Esas reglas dejan `animation-delay: var(--movimiento-retraso-ninguno)`
para anular la espera de la animación del tiempo: es un reset, no una duración.

## Componentes (bloques)

Hoja: `static/css/componentes/<componente>.css`, capa `bloques`, cargada desde `app.css`. Fragmento
en `templates/componentes/<componente>.html`, con parámetros con nombre y slots (`~{::demo}`). Sus
variantes van por atributo (`data-estado`, `data-tono`, `data-tamano`), en la misma hoja del bloque, después de la base (la capa `excepciones` queda para cuando una variante deba ganarle a otro bloque).

| Componente | Fragmento | Parámetros | Slots | Estados y excepciones | Movimiento | Patrón |
|---|---|---|---|---|---|---|
| `.muestra` (la ficha de una pieza en `/estilos`) | `componentes/muestra :: muestra(nombre, cuando, cuerpo)` | `nombre`, `cuando` | `cuerpo` | `data-celda` (un hijo que se ve como una caja), `data-ventana` (una caja que contiene lo que va fijo a la pantalla, como la barra de navegación, y lleva la tierra honda de la cabecera; en el celular se desplaza de costado) y `data-fondo="suelo"` o `"suelo-hondo"` (una caja del color de la tierra o de la tierra honda, para los ejemplos que en el sitio van sobre ese plano); `data-ancho="pantalla"` (un ejemplo que necesita el ancho de una pantalla, como la rama de frutos o la libreta: la ficha se desplaza de costado) | — | Tarjeta de documentación (Material: card) |
| `.boton` | Sin fragmento: el elemento nativo, `<button class="boton">` o `<a class="boton" href>` | — | El texto del botón | `data-tono`: `principal` (la acción de la pantalla o de una parte de ella, como la tierra de la portada), `secundario` o `quitar` (dar de baja). `data-tamano="chico"`: más bajo, en contorno; con `data-tono="principal"` queda lleno y solo más angosto. `data-forma="foto"` (con el tamaño chico): una miniatura arriba y la leyenda debajo, a la izquierda (la foto de una entrega en la portada). `data-zona="suelo"` o `"suelo-hondo"`: sobre el plano de la tierra o de la tierra honda, contorno de paja, letra clara y el fondo liso del color del plano (al pasar, el de la tierra); con `data-tono="principal"` y `data-zona="suelo"`, lleno de paja («Sumar mi entidad»). `hidden` lo oculta. El texto va en el color de acento tanto en el `<a>` como en el `<button>` (el navegador pone negro el del botón). Al apretar se hunde sobre su sombra | `:active` | Material: Buttons (filled, outlined, text) |
| `.campo` y `.campo-error` | Sin fragmento: `<div class="campo">` con `label`, el control nativo (`input`, `select` o `textarea`), una ayuda `small` opcional y, si falla, `<p class="campo-error">` | — | La etiqueta, el control, la ayuda y el error | `data-estado="error"` (borde del control en el color del problema; el mensaje va en `.campo-error` y se enlaza con `aria-describedby`). `input[type="number"]` es angosto; `textarea` se estira en vertical | — | Material: Text fields |
| `.estado` y `.estado-sello` | `componentes/sello :: sello(estado)` (el dibujo) dentro de `<span class="estado" data-estado="…">` con la palabra al lado | `estado` | — | `data-estado`: donante `pendiente`, `en-camino`, `entregada`, `con-problema`; entidad `puerta`, `camino`, `salir`, `recibida`, `problema`. El fragmento dibuja además `hecho` y `falta` (etapas de un recorrido) y cualquier otro valor lo dibuja como problema. El color solo acompaña: el estado va siempre en palabras | — | Material: Chips / badges |
| `.sello-*` (tinta, disco, contorno, contorno--punteado, medio, problema, calado, calado--fino, tilde, hecho, falta) | Las piezas del dibujo del sello, para el SVG de `componentes/sello` y para los dibujos de las pantallas | — | — | — | — | — |
| `.aviso-breve` (toast), `.avisos` y `.aviso-cerrar` | Sin fragmento: `<p class="aviso-breve" data-estado="hecho" role="status">` en el flujo de la página, o el mismo aviso, sin `role` (la región ya es `aria-live`), con un `<button class="aviso-cerrar" data-cerrar aria-label="Cerrar el aviso">×</button>` que trae HTMX a la región `<div id="avisos" class="avisos">` de los layouts | — | El texto (y, en el aviso del flujo, un título `h2` y un `p`) | `data-estado="hecho"` (filete del color de acento, texto en negrita). La región es fija abajo (en el celular, sobre la barra del menú), `aria-live="polite"`. La cola (`Alpine.data("avisos")`) muestra 3 a la vez y deja el resto oculto, los saca a los 6 s (no antes de 5 s; no se van mientras el puntero o el foco están encima) y quita el de `data-cerrar`. Sin JavaScript quedan a la vista | Fundido de entrada (`--movimiento-animacion-aparece`) y de salida (`--movimiento-transicion-atenua`), solo dentro de la región | Material: Snackbar |
| `.banner` | Sin fragmento: `<section class="banner" aria-labelledby>` con `h2`, `p` y el próximo paso (un `a.boton` o un `button`) | — | El título, el texto y la acción | `data-estado="error"` (filete y título del color del problema; `role="alert"` solo si es urgente; `.aviso-codigo` lleva el código de seguimiento). Sin estado, informativo | — | Material: Banners |
| `.vacio` | Sin fragmento: `<section class="vacio" aria-labelledby>` con `h2`, `p` y, si hay, el botón del paso posible | — | El título, el texto y la acción | Mismo papel que el banner. Con el brote del donante (`.aviso-brote`, heredada). `data-forma="filetes"`: sin papel ni sombra, un renglón entre dos filetes finos que ocupa el lugar de los datos de una parte que no es una lámina (en la portada, también el «Cargando…» y el error de cada parte, con `role="status"` o `role="alert"`); con `data-zona="suelo"`, sobre la tierra: filetes de paja, letra clara y todo al centro | — | Material: Empty states |
| `.cargando` y `.esqueleto` | Sin fragmento: `<p class="cargando" role="status">` con el texto de lo que se trae y, dentro de una región con `aria-busy="true"`, los `<div class="esqueleto">` | — | El texto; los lugares de las partes | `.esqueleto`: `data-forma="punteado"` (contorno punteado) o `"hueco"` (contorno punteado sin papel adentro, el de la entidad), y `data-tamano="alto"` (más altura) o `"bajo"` (menos). Sin datos inventados | — | Material: Progress indicators |
| `.dialogo` y `.dialogo-titulo` | Sin fragmento: `<dialog class="dialogo" popover aria-labelledby>` y, en la página, un `<button popovertarget="…">` que lo abre y un botón con `popovertargetaction="hide"` que lo cierra | — | El título (`.dialogo-titulo`), el contenido y los botones | `data-tono="quitar"` (confirmar algo irreversible: borde del color del problema, más angosto, el foco entra en "No, dejarla" con `autofocus`). Sin tono, muestra algo grande: la foto de una entrega en la portada (la imagen, el título `p.dialogo-titulo`, el texto y «Cerrar»). Se abre y se cierra sin JavaScript, con Esc o tocando afuera; el navegador devuelve el foco al botón | Fundido de entrada y de salida (`--movimiento-transicion-dialogo`, con `@starting-style`) | Material: Dialogs / HIG: Alerts |
| `.navegacion`, `.navegacion-largo`, `.navegacion-aparte`, `.navegacion-destacado` | `navegacion-publica :: navegacion(actual)` para las públicas y la portada; la de cada rol, en `layout-privado` (el menú del modelo de vista) | `actual` (la pantalla de la sección actual; `''` en la portada, Contacto y Términos) | Los enlaces de las secciones, una lista (`ul role="list"`) dentro de `<nav aria-label="Secciones">` | La sección actual lleva `aria-current="page"` (en paja). `data-zona="publica"`: las páginas públicas (franja de papel sobre la barra, letra más angosta). `.navegacion-largo` es el resto del nombre, que en la barra del celular solo leen los lectores; `.navegacion-aparte` (Ingresar) va a la derecha desde la tableta y `.navegacion-destacado` (Crear una cuenta) es el botón principal. Con la clase `tierra-impresa` lleva la textura de la tierra en el celular. Dónde va dentro de la cabecera (el orden y el ancho de la fila) lo dice la hoja de la pantalla (`privado.css`, `publico-navegacion.css`) | — | Material: Navigation bar |
| `.navegacion-mas`, `.navegacion-hoja`, `.navegacion-hoja-cabecera`, `.navegacion-hoja-titulo` | Sin fragmento: un `<li class="navegacion-mas">` más en la lista, con un `<button popovertarget>` y la hoja `<div class="navegacion-hoja" popover role="dialog" aria-labelledby>` | — | El título, el botón "Cerrar" y la lista de todas las secciones | Solo con más de 5 secciones (la administración): la barra del celular muestra las 4 primeras y "Más", que abre todas en una hoja desde abajo y queda marcado si la sección actual no entra en la barra. Desde la tableta no se ve "Más": el menú entra entero. Sin popover, la barra muestra todas en filas de a 3. El foco entra a la primera sección (`autofocus`) | Sube desde abajo (`--movimiento-animacion-hoja-sube`) | Material: Navigation drawer / bottom sheet |
| `.tarjeta` y `.tarjeta-cabeza` | Sin fragmento: cualquier elemento con `class="tarjeta"` y su clase de pantalla (`lamina` del donante y la pública, `acta`, `etiqueta`, `etiqueta-datos`, `ejemplar`, `decision`, `pedido`, `foto`, `siguiente`, `ficha` de la portada) para el relleno y la disposición. Una lámina lleva arriba `<div class="tarjeta-cabeza">` con el título (`h2` a `h4`) y un rótulo `p` | `--espacio-relleno` (sin él, ninguno) | El contenido; el título y sus partes los dispone la pantalla, salvo en la cabeza de una lámina | `data-filete`: sin él, simple (borde de 2 px); `doble` (la etiqueta de herbario: borde fino y otro filete más adentro); `contorno` (el filete por fuera, sin borde: no suma alto ni ancho). Esquinas del token (`--radio-lamina`, suaves desde el 2026-11-06) y sombra plana paja (`--color-sombra`). Una sombra de otro color (la de demora del donante, la de lo que espera en la puerta de la entidad) la pone la pantalla. `.tarjeta-cabeza`: el título en cursiva y el rótulo, en una fila que baja de renglón, sobre un filete fino (la lámina «Cómo funciona» de la portada) | — | Material: Cards |
| `.lista` | Sin fragmento: `<ul class="lista" role="list">`, `<ol>` o, para los esqueletos de una carga, un `<div>`, con `--medida-columna` | `--medida-columna` (ancho mínimo de cada columna; sin él, una sola columna) | Los elementos de la lista, casi siempre `.tarjeta` | Reparte los elementos en tantas columnas como entren (no estira los pocos, a diferencia de `.grilla`) y reinicia la lista (sin viñetas, margen ni relleno). El espacio entre elementos, el margen y la medida en cada ancho los dice la hoja de la pantalla (por ejemplo, `--medida-columna: 19rem` desde 53,75 em). **Todavía no hay una tabla en el sitio** (ninguna plantilla usa `<table>`): cuando haga falta, se agrega acá | — | Material: Lists / Data tables |
| `.subida`, `.subida-icono`, `.subida-aviso`, `.subida-resumen`, `.subida-previas`, `.subida-previa`, `.subida-nombre` | Sin fragmento: `<div class="campo subida" x-data="subidaFotos">` con el `label` (el `svg.subida-icono` y el título), la ayuda (`small`), el error (`.campo-error`), el `<input type="file" x-ref="entrada" x-on:change="elegir">` y, debajo, el anuncio para lectores de pantalla (`<span class="subida-aviso" role="status" x-text="resumen">`, siempre presente) y la vista previa: `<p class="subida-resumen" hidden x-bind:hidden="sinFotos" x-text="resumen">` y `<ul class="subida-previas" hidden x-bind:hidden="sinFotos">` con un `<template x-for="foto in fotos">` (`li.subida-previa` con `img`, `small.subida-nombre` y un `button.boton` «Quitar») | — | El título, la ayuda y el control | `data-estado="error"` (el recuadro en el color del problema; el mensaje va en `.campo-error`). El control nativo es lo esencial: sin JavaScript la zona anda igual y manda lo elegido, sin vista previa. Con JavaScript (`Alpine.data("subidaFotos")`) cada imagen elegida muestra su miniatura, su nombre y un botón «Quitar» (rearma la lista del control con `DataTransfer` y devuelve el foco al control); un anuncio dice cuántas hay (`role="status"`). Las miniaturas son archivos de la persona, no datos del negocio: nada sale del navegador | — | Material: File upload (HIG: file picker) |
| `.pestanas`, `.pestanas-lista`, `.pestana`, `.pestanas-panel` | Sin fragmento: `<div class="pestanas" x-data="pestanas" x-on:hashchange.window="desdeLaDireccion">` con `<nav class="pestanas-lista" aria-label x-on:keydown="teclear">` de `<a class="pestana" id href="#panel" x-on:click="elegir">` y, como hermanos, un `<div class="pestanas-panel" id>` por pestaña | — | Los enlaces (con `id` y `href` al panel) y el contenido de cada panel (en Mi perfil, un formulario) | Sin JavaScript cada pestaña es un enlace a su sección y se ve una por vez con `:target` (la primera al entrar; la marcada se elige por lugar, hasta 6 pestañas). Con JavaScript (`Alpine.data("pestanas")`) `.pestanas` lleva `data-interactivo`, la pestaña activa `aria-selected="true"` y su panel `data-activo`; se agregan los roles `tablist`, `tab` y `tabpanel`, se recorre con ← → Inicio Fin (con vuelta) y la elegida queda en la dirección (`#sección`). Al elegir una pestaña su panel se imprime (`data-recien`) | Se imprime con la segunda tinta corrida (`--movimiento-animacion-registro-fino`); sin movimiento si se pidió reducirlo | Material: Tabs / HIG: Segmented controls |
| `.novedades` y `.novedades-cuenta` | `piezas-entidad :: novedades(cuantas)` | `cuantas` (cuántas novedades sin leer) | — | Un botón de papel con la cuenta en un disco paja; sin rojo ni signo de exclamación (es un dato). Al pasar se tiñe de acento claro; al apretar se hunde sobre su sombra | `:active` | Material: Badges |
| `.cabecera-entidad` | Sin fragmento: `<header class="cabecera-entidad">` con `<p class="rotulo">`, el `h1`, la bajada (`p`) y, si hay, el acceso a las novedades | — | El rótulo, el título, la bajada y las novedades | El acceso a las novedades va debajo en el celular y a la derecha desde 53,75 em. Otras piezas de la cabecera (un botón «Marcar») las dispone la hoja de la pantalla | — | Material: Top app bar |
| `.mapa-cuerpo`, `.mapa-dibujo`, `.mapa-lista`, `.estaciones`, `.estacion-ficha`, `.estacion-cabeza`, `.estacion-sello`, `.estacion-entregas` y `.estacion` | Sin fragmento: `<div class="mapa-cuerpo">` con la `<figure class="mapa-dibujo" aria-hidden="true">` (el dibujo) y la `<section class="mapa-lista" aria-labelledby>` con una `<ol class="estaciones" role="list">` de `<li class="estacion-ficha estacion-ficha--N" id="entidad-clave" tabindex="-1">` (cabeza con sello, nombre y cuenta, y la lista de entregas; el título de la lista es un `h2` o `h3` y el nombre de cada entidad, un `h3` o `h4`, según el nivel que toque en la página). Cada estación del dibujo es un `<g class="estacion estacion--N">` | — | El dibujo (SVG) y la lista | El dibujo es de la pantalla; el bloque pone el marco (esquinas del token, la tierra bajo la línea de tierra), las dos columnas desde 68,75 em (el dibujo queda fijo) y la lista. Al pasar, enfocar o llegar con `#entidad-<clave>` a una ficha, su estación queda llena y las demás se apagan (hasta 6; en el dibujo `.estacion--1`, `.estacion--2`, `.estacion--3`, `.estacion--4`, `.estacion--5`, `.estacion--6` y en la lista `.estacion-ficha--1`, `.estacion-ficha--2`, `.estacion-ficha--3`, `.estacion-ficha--4`, `.estacion-ficha--5`, `.estacion-ficha--6`). La lista dice todo lo del dibujo: el mapa nunca es el único medio (RF-CL-06) | Las estaciones se atenúan con `--movimiento-transicion-atenua` | Material: Maps con lista de resultados |
| `.noticias`, `.noticia`, `.noticia-marca`, `.noticia-estado`, `.noticia-texto`, `.noticia-cuando` y `.noticia-donacion` | Sin fragmento: `<ol class="noticias" role="list">` con un `<li class="noticia">` por noticia (en el sistema, la pieza `datos-donante :: aviso`): la marca (`span.noticia-marca`: «Sin leer» o «Leída»), el estado (`span.estado.noticia-estado` con el sello y la palabra), el texto (`a.noticia-texto`, o `span` si no lleva enlace), cuándo (`time.noticia-cuando`) y de qué donación (`span.noticia-donacion`) | — | Las noticias | `.noticia--sin-leer` (estación impresa en paja con su sombra hoja, marca con filete y texto en negrita); sin ella, la estación queda hueca y la marca punteada. A la vista van la marca y la fecha, el texto y el estado con la donación; para el lector, el orden del código. La línea impresa cuelga a la izquierda de la lista. `data-forma="libro"` en la lista: desde la tableta, cada noticia es un renglón con columnas (cuándo, qué pasó, estado y donación) y la marca solo para el lector. Si el texto no lleva enlace (`span.noticia-texto`), no pide el alto de un toque. La disposición de cada pantalla (la franja de tierra de las sin leer en Notificaciones, las dos columnas del detalle) queda en su hoja | — | Material: Timeline (lista con línea de tiempo) |
| `.pasos`, `.paso`, `.paso-dibujo`, `.paso-numero` y las piezas del dibujo (`.paso-sol`, `.paso-suelo`, `.paso-horizonte`, `.paso-brote`, `.paso-punta`, `.paso-punta-tinta`; dentro, `.tallo`, `.hoja`, `.hoja--tinta`, `.raiz`, `.raicilla` y `.cruce`) | Sin fragmento: `<ol class="pasos" role="list">` con un `<li class="paso">` por paso: el `svg.paso-dibujo` (`aria-hidden`; su `viewBox` recorta la parte de la ilustración que le toca), el `<span class="paso-numero" aria-hidden="true">` y el texto `p` (lo principal en `b`) | — | El dibujo, el número y el texto de cada paso | Una sola ilustración en franjas pegadas, cada una del alto de su paso (la primera con el filete de arriba y la última con el de abajo); el número, un sello paja sobre el borde izquierdo. La franja mide 5 rem en el celular y 7 rem desde 37,5 em. Lo usa la lámina «Cómo funciona» de la portada, dentro de una `.tarjeta` | Al entrar se imprime (el sol, el brote y el texto con la segunda tinta corrida), las raíces se trazan desde el brote y los números y las puntas aparecen al final; las hojas del brote se mecen en ciclo (`--movimiento-animacion-*`). Sin movimiento si se pidió reducirlo | Material: Lists (pasos numerados) |
| `.planta`, `.planta-dibujo`, `.planta-cosecha`, `.planta-sol` y las piezas del dibujo (`.tallo`, `.rama`, `.hoja`, `.hoja--tinta`, `.fruto`, `.fruto-tinta`, `.fruto-disco`, `.fruto-hoja`, `.fruto-hueco`) | Sin fragmento: `<div class="planta">` con el `svg.planta-dibujo` (`aria-hidden`: un `g.fruto` por entrega confirmada y un `circle.fruto-hueco` por la que va en camino) y la cuenta `<p class="planta-cosecha">` (el mes en `b`); el sol (`span.planta-sol`, `aria-hidden`) va aparte y lo ubica la pantalla | — | El dibujo y la cuenta | La cuenta va arriba en el celular y al lado de la planta desde 53,75 em. El dibujo es decorativo: la cuenta dice todo en palabras | Las hojas y los frutos se mecen en ciclo (`--movimiento-animacion-viento`, `-viento-fruto`); la pausa de la portada los detiene. Sin movimiento si se pidió reducirlo | — (ilustración con dato) |
| `.frutos`, `.frutos-interior`, `.frutos-cabeza`, `.frutos-rama` y `.frutos-aviso` | Sin fragmento: `<section class="frutos tierra-impresa" aria-labelledby>` con `.frutos-interior`: la cabeza (`.frutos-cabeza`: el `h2`, una nota `p` y, si hay, un `a.boton` chico con `data-zona="suelo-hondo"`) y la rama (`dl.frutos-rama`, un `div` por cifra con `dt` y `dd`) o, sin datos, `div.frutos-aviso` (el texto con `role="status"` o el error con `role="alert"` y «Volver a cargar») | — | La cabeza, las cifras o el aviso | Lo que está confirmado, en la tierra honda: cada cifra es un fruto que cuelga de una rama paja con su etiqueta de papel (el primero mandarina, el segundo paja y el tercero, lo que va en camino, verde y punteado). En el celular la rama baja por el costado; desde 53,75 em cruza de lado a lado, los frutos cuelgan a distinta altura y el botón va a la derecha de la cabeza. El aviso va en un marco doble paja | — | Material: Data visualization (cifras destacadas) |
| `.libreta`, `.libreta-interior`, `.libreta-bajada`, `.libreta-lista`, `.libreta-entrada`, `.libreta-fecha`, `.libreta-sello`, `.libreta-cuadro`, `.libreta-texto`, `.libreta-estado`, `.libreta-pendiente`, `.libreta-miniatura` y `.tinta-corrida` | Sin fragmento: `<section class="libreta" aria-labelledby>` con `.libreta-interior`: un `div` con el `h2` y `p.libreta-bajada`, y `<ol class="libreta-lista" role="list">` con un `li.libreta-entrada` por entrega: `p.libreta-fecha` (el día en `b` dentro de `time`), `svg.libreta-sello`, `svg.libreta-cuadro role="img" aria-label` (el ícono de lo donado; la segunda tinta en `.tinta-corrida`), `div.libreta-texto` (`h3`, la entidad y `p.libreta-estado`) y la foto (`button.boton` chico con `data-forma="foto"`, una `img.libreta-miniatura` y su `.dialogo`) o `p.libreta-pendiente` | — | Las entregas | `data-estado="en-camino"` en la entrega que todavía no llegó: cuadro punteado sin color, estado en el color de en camino y el último tramo de raíz punteado. En el celular, la raíz baja a la izquierda con el sello de cada entrega; desde 53,75 em, una fila por entrega (fecha, raíz con sello, cuadro, texto y foto). No es el cuaderno de entregas de la entidad (`asiento`) ni las noticias del donante | Al bajar, cada entrada aparece subiendo (`mov-asiento` con la línea de tiempo del scroll); sin línea de tiempo o con movimiento reducido, quietas | Material: Timeline |
| `.tierra-superficie`, `.tierra-bajada`, `.tierra-suelo` y `.tierra-interior` (el bloque tierra) | Sin fragmento: una `<section aria-labelledby>` sin clase propia con la superficie (`div.tierra-superficie`: el `h2` y `p.tierra-bajada`, o la planta con su sol) y el suelo (`div.tierra-suelo`, con la utilidad `tierra-impresa` si lleva textura, y adentro `div.tierra-interior`) | — | El título, la bajada, la planta y lo que va en el suelo | La superficie es papel, centrada; el suelo, la tierra con la línea de tierra arriba (4 px de tinta), letra clara y anillo de foco claro. Dónde va el sol de la planta y qué hace la escena con la superficie y el suelo lo dice la hoja de la pantalla | — | — (sección ilustrada) |
| `.red`, `.red-dibujo`, `.red-etiquetas`, `.red-etiqueta`, `.red-etiqueta--1`, `.red-etiqueta--2`, `.red-etiqueta--3`, `.red-etiqueta--5`, `.red-etiqueta--6`, `.red-etiqueta--7`, `.red-etiqueta--tuya`, `.red-sello`, `.red-pide`, `.escena`, `.escena-fija` y las piezas del dibujo (`.tronco`, `.borde`, `.ruta`, `.ruta--pendiente`, `.raiz--1`, `.raiz--2`, `.raiz--3`, `.raiz--5`, `.raiz--6`, `.raiz--7`, `.raiz--tuya` y `.cruce`) | Sin fragmento: `<div class="tierra-interior red">` (dentro de `.tierra-suelo`) con el `svg.dibujo.red-dibujo` (`aria-hidden`), el aviso sin datos (`.vacio`) y `<ul class="red-etiquetas" role="list">`: un `li.red-etiqueta.red-etiqueta--N` por entidad (un enlace con `svg.red-sello`, el nombre en `b`, el lugar en `span` y lo que pide en `span.red-pide`) y el `li.red-etiqueta.red-etiqueta--tuya` (con el botón para sumarse). `.escena` con `.escena-fija` envuelve la superficie con la planta y el suelo con la red | — | Las etiquetas y el aviso | Las etiquetas de herbario son papel con la segunda tinta corrida, colgadas de su sello, y la de tu entidad, punteada. En el celular, la espina en espejo (un tronco al centro y las etiquetas de a dos); desde 68,75 em, la red radial con cada etiqueta en la punta de su raíz (la posición sale del dibujo de 1200 × 600) y tu entidad al centro, debajo del tronco. Al pasar o enfocar una etiqueta, su raíz queda llena y las demás se apagan. `data-estado="sin-datos"` (vacío, cargando o error): no hay dibujo y tu entidad, en el flujo, no cuelga de nada. No es la etiqueta de Confirmar recepción (`etiqueta`) ni la lista de entidades (`herbario`) | La red se traza desde el brote (`--movimiento-animacion-traza`); en el celular, cada fila de etiquetas se imprime al entrar (línea de tiempo del scroll). En la computadora, la escena queda quieta durante 1,5 pantallas de scroll: el tronco crece, las raíces llegan de a una y cada etiqueta se imprime en su punta, con tu entidad al final. Sin línea de tiempo o con movimiento reducido, la escena no se fija | Material: Maps (red) con lista |
| `.pausa`, `.pausa-boton` y `.pausa-casilla` | Sin fragmento: `<p class="pausa">` con `<label class="boton pausa-boton" data-tamano="chico">`, la casilla (`input.pausa-casilla type="checkbox"`) y el texto «Pausar el movimiento» | — | El texto | La casilla cubre el botón entero: el toque es el del botón; con el foco en la casilla, el anillo va en el botón. Marcada, queda hundida sobre su sombra, en acento claro. Qué detiene lo dice la hoja de la pantalla (en la portada, el viento de los pasos y de la planta) | Solo se muestra si hay movimiento (con "reducir movimiento" no hay nada que pausar) | WCAG 2.2.2 (pausar, detener, ocultar) |
| `.recorrido` y sus piezas `.recorrido-*` (el camino de las donaciones del inicio del donante) | `componentes/recorrido` (los fragmentos del tramo, del corte de suelo con una planta por donación y de las personas; la muestra, `componentes/recorrido :: muestra`) | Los que fije la zona recorrido de la iteración 2 de Raíces cálida | — | Un tramo por etapa (en el depósito, con destino, en la entidad), cada uno con su rótulo, su título, su cuenta, el corte de suelo y su acción; quien dona al principio y quien recibe al final. Se completa al cerrar la iteración 2 | Las primitivas de `movimiento.css` | Línea de tiempo por etapas (Material: stepper) |

`--espacio-relleno` y `--medida-columna` son parámetros de `.tarjeta` y `.lista`, no tokens de `tokens.css`: llevan el prefijo de los tokens de uso para pasar el cerco, y esos nombres quedan reservados (no se definen en `:root`).

De las piezas con línea de tiempo (B2-14) solo las noticias del donante tienen más de una pantalla y son un
bloque (`.noticias`). `asiento` es el nombre de dos piezas distintas (el cuaderno de entregas recibidas de
la entidad y el libro de donaciones del donante: cada una con su disposición; el de la portada es desde B3-01
el bloque `.libreta`), y
`novedad` y `libro` son de una sola pantalla: siguen como clases heredadas de la hoja de su pantalla, igual
que las demás láminas de una sola pantalla (`.confirmado`, `.puerta`, `.grupo-avisos--sin-leer`,
`.etiqueta-entidad`, `.herbario`). De la entidad quedan en su hoja `.acta` (el cuerpo de la etiqueta de herbario; en
Confirmar recepción `acta` es además la grilla de la página, otro uso del mismo nombre) y `.salidas`
(las salidas de una parte; las páginas públicas tienen las suyas con el mismo nombre y otra
disposición). Del dibujo del mapa quedan en la hoja de la pantalla `.estacion-numero`, `.mapa-fruto` y
el rótulo, porque compiten con reglas sin capa de `sellos.css` y `publico-listas.css`.

**Quedan como clases heredadas**, de una sola pantalla: `aviso-brote` (el dibujo del brote de un estado vacío del
donante), `aviso-acciones` (los botones de un banner de Ingresar), `aviso--nombre` y `aviso-codigo`
(el renglón de código de un banner de error); y `seguimiento-reserva` (el esqueleto de Seguir entregas
del donante). El esqueleto de la entidad, que era `hueco`, es desde B2-12 `.esqueleto` con
`data-forma="hueco"` y `data-tamano="bajo"`.

Los «Cargando…» de Entregas, Entidades y Mapa públicos (`publico-donaciones`, `publico-entidades` y
`publico-mapa`) son `.cargando` con `role="status"` desde el 2026-11-04 (el usuario lo confirmó; dejaron de
ser una lámina). Los de la portada, como su estado vacío y su error, son `.vacio` con `data-forma="filetes"`
desde B3-01: sin papel, igual que antes (el usuario eligió conservar las dos líneas, 2026-11-04).

Las variantes por atributo van en la misma hoja del bloque, después de la base: la capa `excepciones`
queda para cuando una variante necesite ganarle a otro bloque. Los botones propios de la portada
(`boton--foto`, `boton--suelo`, `boton--mapa`) son desde B3-01 variantes del bloque (`data-forma="foto"`,
`data-zona`) o, el del mapa, solo su lugar en la hoja; `boton--google` sigue en la hoja de su pantalla
como clase heredada, hasta la fase 3.

## Alpine (D31)

`static/js/interfaz.js` registra los `Alpine.data()`. Cada uno entra acá con su nombre, qué estado
maneja y qué plantilla lo usa. Reglas en
[`cliente-liviano/AGENTS.md`](../../cliente-liviano/AGENTS.md#alpine-d31).

| `Alpine.data()` | Estado que maneja | Dónde se usa | Detalle |
|---|---|---|---|
| `avisos` | La cola de avisos breves: cuáles se ven (3 a la vez; el resto, `hidden`), el reloj de cada uno (6 s) y su salida con un fundido (`data-saliendo`). Si no hay fundido ("reducir movimiento"), lo quita enseguida | La región `#avisos` de `layout-privado` y `layout-publico` (`x-data="avisos"`) | El contenido de cada aviso lo trae el servidor (HTMX lo agrega a la región, o viene con la página); Alpine no arma texto. Pausa mientras el puntero o el foco están sobre la región (`pausar`, `seguir`) y cierra el aviso cuyo botón lleva `data-cerrar` (`cerrar`) |
| `subidaFotos` | La vista previa de las fotos elegidas en el control de archivos: la lista de lo que se dibuja (`fotos`: número, dirección blob: de la miniatura, nombre y textos) y, fuera del estado reactivo, los archivos y las direcciones para soltarlas | El campo de fotos de `entidad-seguimiento-confirmar` (`x-data="subidaFotos"` en `.subida`) y `/estilos` | Lo esencial es el control nativo, que manda lo que tenga elegido; Alpine solo muestra miniaturas, nombre y «Quitar». Quitar rearma el control con `DataTransfer`. Suelta las direcciones al cambiar y al destruirse. No arma datos del negocio: son archivos de la persona |
| `pestanas` | Qué pestaña está activa y qué panel se ve: lo marca con atributos (`aria-selected`, `data-activo`, `data-recien`, `data-interactivo`), sin estado propio | `.pestanas` de `donante-perfil` y `/estilos` | Agrega los roles `tablist`, `tab` y `tabpanel` solo si hay JavaScript. Clic (`elegir`), ← → Inicio Fin (`teclear`) y `#sección` desde otro enlace (`desdeLaDireccion`); deja `#sección` en la dirección con `history.replaceState`. Sin JavaScript anda con `:target` |
| `avisoDeMuestra` | Nada: agrega a la región un aviso armado con el `<template id="aviso-de-muestra">` de la página | Solo `/estilos` | Sirve para ver la cola; en el sistema el aviso llega de HTMX |
