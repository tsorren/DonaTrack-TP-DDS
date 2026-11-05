# Backlog de migración a componentes y capas CUBE

> Obligatorio por el [ADR de interacción, movimiento y componentes](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md)
> (`proposed`, D31–D33). Deuda asociada: [DTI-34](../adr/DEUDA_TECNICA.md#dti-34).
> Línea de base medida el 2026-11-04 `[OBSERVED]`: 8.924 líneas de CSS en 29 hojas, 212 clases de un
> solo uso, 13 `@keyframes` en 5 hojas, 28 duraciones literales fuera de `tokens.css`, 0 tokens de
> movimiento y sin catálogo `/estilos`.

## Reglas del backlog

1. **Primero la base (fase 0).** Sin el cerco, migrar no sirve: la entropía vuelve.
2. **Quien toca, migra.** Toda tarea que modifica una hoja de pantalla migra antes los ítems de la
   fase 2 que esa pantalla usa. Una pantalla nueva nace en la capa CUBE.
3. **El tope baja.** Cada ítem cerrado baja el tope de clases de un solo uso del cerco al valor medido
   después del cambio. El tope nunca sube.
4. **Cada ítem cierra con:** su entrada en [`componentes.md`](componentes.md) y en `/estilos`;
   `mvn clean test -pl cliente-liviano -am` verde; `-Pvisual` sin cambios visuales no buscados
   (capturas antes y después); y la tabla de abajo actualizada con fecha y commit.
5. **Revisión por entrega.** Al cerrar cada entrega se revisa el avance, se repriorizan los ítems y
   se anota el tope vigente.

## Fase 0 — Base y cerco

> Cerrada el 2026-11-04. Notas de lo hecho: las duraciones se redondearon hasta 0,1 s para entrar en
> la escala de tokens; Pico no se carga en ninguna plantilla, así que la capa `pico` queda declarada y
> vacía; las clases que las plantillas ya usaban están en
> `cliente-liviano/src/test/resources/verificacion/clases-heredadas.txt` (527, solo baja); el cerco
> mide 211 clases de un solo uso (sin comentarios, textos ni urls): el tope quedó en 211, el valor medido (regla 3), y no en los 212 de la línea de base, que contaban una palabra de un comentario. Las capturas de
> `-Pvisual` antes y después son iguales salvo 7 de 584 de la portada con 8 a 19 px de diferencia,
> que también aparecen entre dos corridas del mismo código (ruido de la barra fija).

| ID | Ítem | Depende de | Hecho cuando | Estado |
|---|---|---|---|---|
| B0-01 | Tokens de movimiento en `tokens.css`: duraciones, curvas y distancias, en dos niveles | — | Las 28 duraciones literales tienen su token equivalente | Hecho (2026-11-04) |
| B0-02 | `@layer pico, tokens, composicion, utilidades, bloques, excepciones, pantallas` en `app.css`, con Pico en la capa `pico` | — | Capturas de `-Pvisual` iguales a las de antes | Hecho (2026-11-04) |
| B0-03 | `composicion.css` (`pila`, `racimo`, `barra-lateral`, `cubierta`, `grilla`, `centro`, `marco`) y `utilidades.css` | B0-01, B0-02 | Cada primitiva con su entrada en el catálogo | Hecho (2026-11-04) |
| B0-04 | `movimiento.css` con las primitivas (`aparece`, `sube`, `resalta`, `crece`, `traza`) en `data-movimiento` y "reducir movimiento" resuelto adentro | B0-01 | Video del movimiento en el perfil `visual` | Hecho (2026-11-04) |
| B0-05 | Alpine CSP: WebJar `alpinejs__csp` en `cliente-liviano/pom.xml`, carga `defer` en los layouts y `Alpine.data()` en `static/js/` | — | Test del layout que verifica la carga desde el WebJar | Hecho (2026-11-04) |
| B0-06 | Catálogo: `/estilos` y [`componentes.md`](componentes.md) (nombre, cuándo, parámetros, slots, estados, movimiento, patrón) | B0-03 | RF-CL-05 cumplido; axe AA sobre `/estilos` | Hecho (2026-11-04) |
| B0-07 | Cerco en `InterfazFitnessTest`: clases en el catálogo, tope de clases de un solo uso (211, el valor medido), hojas de pantalla sin color, fuente, `@keyframes` ni duraciones, sin duraciones literales fuera de `tokens.css` | B0-01, B0-06 | Las reglas fallan con un caso inventado y pasan sobre el sitio | Hecho (2026-11-04) |
| B0-08 | Skills: `vista-ui` empieza por el catálogo y `revision-ui` rechaza clases fuera de él (vector nuevo) | B0-06 | Las dos skills citan `componentes.md` | Hecho (2026-11-04) |
| B0-09 | Video de movimiento con Playwright (`recordVideo`) en `VerificacionVisualTest` | B0-04 | Un video por pantalla con movimiento | Hecho (2026-11-04) |
| B0-10 | View Transitions entre páginas y en los intercambios de HTMX | B0-02, B0-04 | Navegación sin animación donde no hay soporte; respeta "reducir movimiento" | Hecho (2026-11-04) |

## Fase 1 — Movimiento que hoy está suelto

> Cerrada el 2026-11-04. Notas de lo hecho: los 13 `@keyframes` de las 5 hojas pasaron a
> `movimiento.css` como 10 (`mov-*`): `aparece`, `traza` y `lamina-traza` eran iguales a las
> primitivas `mov-aparece` y `mov-traza` y se reusaron. Cada animación se declaró **completa**
> (`@keyframes`, duración, curva y espera, solo con tokens) como una custom property
> `--movimiento-animacion-*` (y `--movimiento-transicion-atenua`) de `movimiento.css`; la hoja de
> pantalla la pide con `animation: var(--movimiento-animacion-registro)` y no declara `@keyframes`,
> duraciones, curvas ni esperas. Se eligió ese camino y no `data-movimiento` en las plantillas
> porque las clases animadas (`.raiz`, `.fruto`, `.etiqueta`) las comparten varias pantallas y el
> cambio tocaba unos 40 elementos SVG de 10 plantillas. Las custom properties solo existen con
> movimiento permitido: "reducir movimiento" quedó resuelto adentro (el bloque `reduce` de
> `.linea` en `donante-seguimiento.css` quedó redundante). Quedan en la portada dos
> `animation-delay: var(--movimiento-retraso-ninguno)` (las reglas de la escena con línea de tiempo
> del scroll anulan así la espera de la animación del tiempo): es un reset con un token, no una
> duración. Nueva regla del cerco en `InterfazFitnessTest`: `@keyframes`, `--movimiento-duracion-*` y
> `--movimiento-curva-*` y las esperas (salvo `retraso-ninguno`) no se escriben en ninguna hoja
> de la carpeta `css/` salvo `movimiento.css`, `app.css` y `tokens.css`. No cambió ninguna clase: el tope de
> clases de un solo uso sigue en 211, el valor medido.

| ID | Hoja | Contenido a mover | Estado |
|---|---|---|---|
| B1-01 | `publico-portada.css` | 7 `@keyframes` y 16 duraciones (viento, traza, crece…) a `movimiento.css` y tokens | Hecho (2026-11-04) |
| B1-02 | `publico-paginas.css` | 2 `@keyframes` y 4 duraciones | Hecho (2026-11-04) |
| B1-03 | `entidad-seguimiento-confirmar.css` | 2 `@keyframes` y 4 duraciones | Hecho (2026-11-04) |
| B1-04 | `privado.css`, `donante-perfil.css` | 1 `@keyframes` cada una y sus duraciones | Hecho (2026-11-04) |
| B1-05 | `publico-mapa.css`, `donante-seguimiento.css` | 1 duración cada una | Hecho (2026-11-04) |

## Fase 2 — Componentes

Prioridad por uso: primero los que comparten varias pantallas. El patrón de cada uno está en
[patrones y microcopy](patrones-y-microcopy.md#componentes-y-su-patrón).

> Tanda 1 (B2-01 a B2-03), 2026-11-04. Corte acordado con el usuario: botón, campo y sello primero
> (estilo puro, los más compartidos); las otras tandas son B2-04, B2-05 y B2-09 (avisos, diálogo y
> vacíos), B2-06 a B2-08 (navegación, tarjeta y tabla) y B2-10 a B2-13 (fotos, pestañas, entidad y
> mapa). Notas de lo hecho: las variantes pasaron a atributos (`data-tono`, `data-tamano`,
> `data-estado`) y las plantillas ya no usan `boton--principal`, `--secundario`, `--chico`, `--quitar` ni `--aviso`, `campo--error` ni `estado--*`; botón y campo
> no tienen fragmento (son elementos nativos: el contrato es el HTML del catálogo) y el sello sí
> (`componentes/sello`, que reemplaza cuatro copias con vocabularios distintos y siete SVG en línea).
> Quedan como clases heredadas los ganchos de una sola pantalla (`boton--foto`, `--mapa`, `--google`,
> `--suelo`, `pausa-boton`, `campo--fotos`, `campo--corto`) y `:where(button).boton` con el color de los
> enlaces en la portada y en `publico-listas.css`: en las pantallas privadas, un `<button class="boton">`
> secundario conserva el texto negro por defecto del navegador, y unificarlo sería un cambio visual
> (hallazgo para la fase 3, no corregido). Del resto de `formularios.css` (`.formulario`, `.opciones`,
> `.resumen`, `.obligatorios`, `.aviso--hecho`) no se migró nada: no era parte del ítem.

> Tanda 2 (B2-04, B2-05 y B2-09), 2026-11-04. Notas de lo hecho: el papel del aviso (la lámina con el
> filete y la sombra plana) vivía en `privado.css` como `.aviso` y servía para cuatro cosas distintas; se
> separó en `.aviso-breve` (una acción que salió bien, `data-estado="hecho"`), `.banner` (algo que afecta a
> la pantalla, `data-estado="error"`) y `.vacio`, que comparten la regla de `componentes/aviso.css`.
> `.cargando` (el texto) y `.esqueleto` (el lugar de la parte que espera; `data-forma="punteado"` y
> `data-tamano="alto"`) están en `componentes/carga.css`, y `.dialogo` en `componentes/dialogo.css`.
> La región `#avisos` de los layouts privado y público lleva la cola de Alpine (`avisos`: 3 a la vez, 6 s,
> pausa con el puntero o el foco). El diálogo se abre con `popover`, sin JavaScript, y no con
> `showModal()`: una baja tiene que poder confirmarse sin JavaScript. Quedan fuera, como clases heredadas
> de una sola pantalla: en la portada, `aviso`, `aviso--suelo`, `foto-ventana` y `foto-titulo` (otro
> aspecto; B3-01), el esqueleto `hueco` de la entidad (sin fondo y más bajo) y `seguimiento-reserva` y
> `seguimiento-aviso` de Seguir entregas del donante (este último nunca tuvo estilo; unificarlo con
> `.cargando` lo pondría en negrita). Los textos «Cargando…» de las páginas públicas, que hoy son una
> lámina, pasaron a `.banner` para no cambiar lo que se ve (hallazgo para la fase 3: son indicadores de
> carga y quizá deberían ser `.cargando`).

> Tanda 3 (B2-06, B2-07 y B2-08), 2026-11-04. Notas de lo hecho: la barra de secciones (la de cada rol, con
> su hoja "Más", y la pública) es un solo bloque, `.navegacion` (`componentes/navegacion.css`), con
> `data-zona="publica"` para la pública; dónde va dentro de la cabecera (el orden y el ancho de la fila)
> queda en `privado.css` y `publico-navegacion.css`, porque es disposición de esa cabecera. La cabecera
> misma (`.armazon`, la cuenta y el acceso a Notificaciones) no se migró: no era parte del ítem. La
> tarjeta (`.tarjeta`, `data-filete`: simple, doble y contorno) es solo el papel, con el filete, la esquina
> y la sombra: 10 piezas de 8 hojas lo repetían y lo piden ahora con el atributo (`lamina` del donante, `acta`,
> `etiqueta`, `ejemplar`, `etiqueta-datos`, `decision`, `pedido`, `foto`, `siguiente` y la `lamina` pública);
> el relleno y la disposición de cada una siguen en su hoja, por eso esas clases siguen como ganchos
> heredados. La lista adaptable (`.lista`, con `--medida-columna`) reemplaza el reinicio de la lista y las
> columnas de `laminas`, `actas`, `ejemplares` y `ejemplares-reserva`. **La tabla no se hizo:** ninguna plantilla
> usa `<table>`, así que no hay nada que migrar (el catálogo lo dice). Para que la barra (fija) le gane a
> `.tierra-impresa`, esa textura pasó a la capa `utilidades`, y el anillo de foco (`:focus-visible`) y el de
> paja sobre la cabecera y el pie, a la capa `pico`: una regla sin capa le gana a una con capa. Quedan como
> clases heredadas las piezas con línea de tiempo (el cuaderno de entregas recibidas, `asiento`, `noticias`,
> `libro`), que repiten una estructura propia: se propone un ítem nuevo para ellas (B2-14) si el usuario lo
> confirma. Las demás láminas de una sola pantalla (`.confirmado`, `.puerta`, `.grupo-avisos--sin-leer`,
> `.estacion-ficha` y `.etiqueta-entidad`) no son iguales a ninguna variante y se quedan.

> Tanda 4 (B2-10 a B2-13), 2026-11-04. Notas de lo hecho: **B2-10, la zona de subida de fotos** (`.subida`,
> `componentes/subida.css`) es el campo de fotos de Confirmar recepción (`.campo--fotos`) hecho bloque, con el
> control nativo de archivos adentro (lo esencial: sin JavaScript manda lo elegido) y la vista previa de
> Alpine (`subidaFotos`): miniatura, nombre y «Quitar» por foto, un anuncio de cuántas hay y el foco de
> vuelta al control; los archivos quedan fuera del estado reactivo. **B2-11, las pestañas** (`.pestanas`,
> `componentes/pestanas.css`) son las del perfil del donante (`perfil-pestanas`, `perfil-panel`): sin
> JavaScript siguen siendo enlaces con `:target`; con JavaScript, Alpine (`pestanas`) agrega los roles de
> pestañas, las flechas, Inicio y Fin, y cambia de panel sin recargar, dejando `#sección` en la dirección.
> Para eso cada formulario del perfil quedó dentro de un `div.pestanas-panel` con el `id` de la sección.
> **B2-12, las piezas de la entidad:** `.novedades` (el acceso a Notificaciones) y `.cabecera-entidad`
> pasaron de `entidad-comun.css` a sus bloques, y el `.hueco` de la entidad (que la tanda 2 dejó heredado)
> es ahora un esqueleto: `.esqueleto` con `data-forma="hueco"` (punteado sin papel) y `data-tamano="bajo"`.
> `.acta` (el cuerpo de la etiqueta de herbario, que en Confirmar recepción es además la grilla de la página
> con el mismo nombre) y `.salidas` (las páginas públicas tienen las suyas con el mismo nombre y otra
> disposición) siguen en su hoja. **B2-13, el mapa con su lista** (`.mapa-cuerpo`, `componentes/mapa.css`):
> el marco del dibujo, las dos columnas, la lista de fichas y el resalte cruzado pasaron al bloque; el dibujo
> de la pantalla (`.estacion-numero`, `.mapa-fruto`, el rótulo y el esqueleto del mapa) quedó en
> `publico-mapa.css`, porque compite con reglas sin capa de `sellos.css` y `publico-listas.css`: una regla
> sin capa le gana a una con capa. La ficha de cada entidad no usa `.tarjeta`: su sombra mide 0,35 rem y la de
> la tarjeta, 0,4 rem. Se propone un ítem nuevo, B2-14, para las piezas con línea de tiempo (el cuaderno de
> entregas recibidas, `asiento`; las noticias y el libro de donaciones), que ya estaba anunciado en la tanda 3.

> Tanda 5 (B2-14 y dos hallazgos de la fase 3), 2026-11-04. Notas de lo hecho: **B2-14, el cuaderno de
> noticias** (`.noticias` y `.noticia`, `componentes/noticias.css`) es la lista que cuelga de una línea impresa
> por fecha del donante (Inicio, Notificaciones y el detalle de la donación, más la pieza `datos-donante ::
> aviso`). Trae dos variantes que antes estaban en las hojas de pantalla: `data-forma="libro"` (desde la
> tableta, un renglón con columnas por noticia, el de las leídas de Notificaciones) y el texto sin enlace
> (`span.noticia-texto`, que reemplaza a `noticia--simple`). **Se recortó el ítem:** de lo que B2-14 anunciaba
> solo las noticias tienen más de una pantalla. `asiento` es el nombre de tres piezas con disposiciones distintas
> (el cuaderno de entregas recibidas de la entidad, el de la portada y el libro de donaciones del donante), y
> `novedad` (la bandeja de la entidad) y `libro` (Mis donaciones) son de una sola pantalla: quedan en su hoja,
> como `acta` y `salidas`. **Hallazgos de la fase 3, resueltos con la confirmación del usuario:** los «Cargando…»
> de Entregas, Entidades y Mapa públicos pasan de `.banner` a `.cargando` con `role="status"` (dejan de ser una
> lámina; su margen sigue en 0 dentro de `.publica`, con `[role="status"]` y no con la clase, para no sumar una
> clase de un solo uso), y `.boton` toma el color de los enlaces en su base (el `<button>` secundario de las
> privadas tenía el negro del navegador; los dos `:where(button).boton` de `publico-listas.css` y de
> `publico-portada.css` ya no hacen falta). El tope de clases de un solo uso queda en 172: mover la base de
> `.noticia*` al bloque dejaba 176 (tres clases de noticia pasaban a figurar una sola vez en las hojas de pantalla, y `.cargando` sumaba otra en `publico-listas.css`), y
> absorber el libro y el texto sin enlace en el bloque lo devolvió a 172. **El tope no baja en este ítem.**

| ID | Componente | Origen hoy | Pantallas que lo usan | Estado |
|---|---|---|---|---|
| B2-01 | Botón | `app.css`, `formularios.css` | Todas | Hecho (2026-11-04) |
| B2-02 | Campo de formulario | `formularios.css`, `publico-registro.css` | Registro, ingreso, perfil, necesidades, confirmar | Hecho (2026-11-04) |
| B2-03 | Sello de estado | `sellos.css` | Privadas | Hecho (2026-11-04) |
| B2-04 | Aviso (toast) y banner, con la cola en Alpine | `app.css`, `privado.css` | Todas las acciones | Hecho (2026-11-04) |
| B2-05 | Diálogo (`<dialog>` + `@starting-style`) | Foto de la entrega en la portada, bajas | Portada, necesidades | Hecho (2026-11-04) |
| B2-06 | Navegación por rol y barra pública | `privado.css`, `publico-navegacion.css` | Todas | Hecho (2026-11-04) |
| B2-07 | Tarjeta | `donante-comun.css`, `entidad-comun.css`, `publico-donaciones.css` y otras 5 hojas | Listas y seguimiento | Hecho (2026-11-04) |
| B2-08 | Tabla o lista adaptable | `publico-donaciones.css`, `entidad-recibidas.css`, `donante-entidades.css` | Listados | Hecho (2026-11-04); la tabla no: el sitio no tiene ninguna |
| B2-09 | Estado vacío, esqueleto e indicador de carga | Varias hojas | Todas las listas | Hecho (2026-11-04) |
| B2-10 | Carga de fotos con vista previa (Alpine) | `entidad-seguimiento-confirmar.css` | Confirmar recepción | Hecho (2026-11-04) |
| B2-11 | Pestañas (Alpine) | `donante-perfil.css` | Mi perfil | Hecho (2026-11-04) |
| B2-12 | Piezas de la entidad | `piezas-entidad.html`, `entidad-comun.css` | Sitio de la entidad | Hecho (2026-11-04); `acta` y `salidas` no: comparten nombre con otro uso |
| B2-13 | Mapa con su lista (RF-CL-06) | `publico-mapa.css` | Mapa público, seguimiento | Hecho (2026-11-04); el dibujo queda en la hoja de la pantalla |
| B2-14 | Cuaderno con línea de tiempo (`asiento`, `noticias`, `libro`) | `entidad-recibidas.css`, `entidad-notificaciones.css`, `donante-notificaciones.css`, `donante-donaciones.css` | Entregas recibidas, Notificaciones, Mis donaciones | Hecho (2026-11-04), recortado: solo `.noticias` (de `donante-comun.css`, `donante-notificaciones.css` y `donante-donaciones-detalle.css`; Inicio, Notificaciones y detalle del donante); `asiento`, `novedad` y `libro` no |

## Fase 3 — Hojas de pantalla reducidas a disposición

Al terminar las fases 1 y 2, cada hoja de pantalla queda solo con la disposición propia. Una hoja sale
de `HOJAS_HEREDADAS` (`InterfazFitnessTest`) cuando no declara `color:`, `background:` (ni `-color` ni
`-image`), `font:` ni `font-family:`, ni `@keyframes`: cada pieza visual va a un bloque de `componentes/` o
a un token. Orden acordado con el usuario: la portada en 3 subtandas y después una hoja por tanda
(B3-02, B3-05, B3-04, B3-03); B3-06 se parte por rol.

> B3-01, subtanda 1, 2026-11-05. Notas de lo hecho: la lámina «Cómo funciona» es una `.tarjeta` de filete
> doble (`ficha` queda como gancho de su disposición) con una cabeza nueva del bloque, `.tarjeta-cabeza` (el
> título en cursiva y su rótulo sobre un filete fino); su sombra pasa de 0,45 a 0,4 rem, como las demás
> tarjetas (el usuario lo eligió). La foto de una entrega abre un `<dialog class="dialogo" popover>` con
> `.dialogo-titulo`, con el fundido de entrada y salida del diálogo (el usuario lo aceptó). Los avisos de la
> tierra y del cuaderno del mes (vacío, cargando y error) son `.vacio` con `data-forma="filetes"` (sin papel,
> entre dos filetes: el usuario eligió conservar las dos líneas) y, en la tierra, `data-zona="suelo"`. Los
> botones propios pasan a variantes del bloque: `data-forma="foto"` (la miniatura dentro del botón chico),
> `data-zona="suelo"` y `"suelo-hondo"` (el chico sobre la tierra y la tierra honda, y «Sumar mi entidad»
> con el tono principal); el del mapa solo tenía su lugar en la cabeza de la rama y lo toma por estructura
> (`.registro-cabeza > .boton`). El título y la bajada de la portada usan dos tokens nuevos,
> `--tamano-titulo-portada` y `--tamano-bajada-portada`, en `.hero h1` y `.hero-bajada` de `app.css` (solo
> la portada los usa), y la clase `portada` desaparece. Se borró `.suelo a`: su color solo alcanzaba a los
> botones con forma de enlace (los de las etiquetas tienen el suyo) y, sin capa, les habría ganado a las
> variantes. Salen 9 clases de la lista de heredadas (441 quedan). El tope baja a 171: `foto-titulo` y
> `boton--mapa` tenían una sola mención y `hero-bajada` pasa a tenerla (solo en `app.css`). Las líneas de la
> tabla se corrigieron al valor de hoy: las de la línea de base (2026-11-04) quedaron viejas con las fases 1 y 2
> (por ejemplo, `privado.css` tenía 522 y hoy tiene 239).

| ID | Hoja | Líneas (2026-11-05) | Estado |
|---|---|:-:|---|
| B3-01 | `publico-portada.css` | 1.446 (1.590 antes de la subtanda 1) | En curso: subtanda 1 hecha (2026-11-05) |
| B3-02 | `entidad-necesidades.css` | 575 | Pendiente |
| B3-03 | `privado.css` | 239 | Pendiente |
| B3-04 | `entidad-seguimiento-confirmar.css` | 387 | Pendiente |
| B3-05 | `publico-registro.css` | 453 | Pendiente |
| B3-06 | Las 23 hojas restantes (con `app.css`, sin `tokens.css` ni las de la capa CUBE) | 4.070 en total | Pendiente |

## Registro de avance

| Fecha | Ítems cerrados | Tope de clases de un solo uso | Commit |
|---|---|:-:|---|
| 2026-11-04 | — (línea de base) | 212 | — |
| 2026-11-04 | B0-01 a B0-10 (fase 0 completa) | 211 (el cerco mide sin comentarios, textos ni urls: 211; los 212 de la línea de base incluían una palabra de un comentario) | 2e7ace16 (rama `E6_raices`) |
| 2026-11-04 | B1-01 a B1-05 (fase 1 completa) | 211 (medido después del cambio; la fase no toca selectores) | c0884659 (rama `E6_raices`) |
| 2026-11-04 | B2-01 a B2-03 (tanda 1 de la fase 2: botón, campo y sello) | 197 (medido después del cambio; era 211) | 38033d2a (rama `E6_raices`; tandas 1 y 2 en un solo commit) |
| 2026-11-04 | B2-04, B2-05 y B2-09 (tanda 2 de la fase 2: aviso breve con su cola, banner, diálogo, estado vacío, carga y esqueleto) | 189 (medido después del cambio; era 197) | 38033d2a (rama `E6_raices`; tandas 1 y 2 en un solo commit) |
| 2026-11-04 | B2-06, B2-07 y B2-08 (tanda 3 de la fase 2: navegación, tarjeta y lista adaptable) | 186 (medido después del cambio; era 189) | 392f6239 (rama `E6_raices`) |
| 2026-11-04 | B2-10 a B2-13 (tanda 4 de la fase 2: subida de fotos con vista previa, pestañas, piezas de la entidad y mapa con su lista) | 172 (medido después del cambio; era 186) | 9343fbb4 (rama `E6_raices`) |
| 2026-11-04 | B2-14 (tanda 5 de la fase 2: cuaderno de noticias del donante, con el libro de registro y el texto sin enlace) y los dos hallazgos de la fase 3 (los «Cargando…» públicos y el botón secundario) | 172 (medido después del cambio; era 172: no baja) | 06cd7190 (rama `E6_raices`) |
| 2026-11-05 | B3-01, subtanda 1 (la portada: lámina, diálogo de la foto, avisos, botones propios y tamaño del título) | 171 (medido después del cambio; era 172) | 7c07ca8b (rama `E6_raices`) |
