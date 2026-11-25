# Bitácora de diseño de la interfaz

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Se actualiza en cada decisión · parte del [proceso de diseño](README.md)**

Cada decisión de diseño queda acá, de la más nueva a la más vieja, con el mismo formato que usa el
entregable para la cátedra. Las comparaciones A/B agregan sus capturas en `evidencia/<tema>/`.

Desde el 2026-10-30 hay dos alternativas de identidad. Las decisiones anteriores a la entrada
"Dos alternativas de identidad" son de la **Alternativa Glassmorphism**. Las de la **Alternativa
Cozy / Medieval** dicen "(Cozy / Medieval)" en el tema. Desde el 2026-11-03 el sitio tiene una sola
identidad, Raíces; los documentos y la evidencia de las otras dos están en el
[archivo histórico](historico/README.md), que no es guía para diseñar.

## Formato

```markdown
### AAAA-MM-DD · <tema> · <decisión en una línea>

- **Decisión:** qué se eligió.
- **Alternativas:** qué otras opciones hubo (en un A/B, la variante descartada).
- **Por qué:** el motivo, en palabras simples.
- **Costo:** qué se resigna o qué queda pendiente.
- **Quién decidió:** usuario, diseñadora o equipo.
- **Evidencia:** capturas o enlace (si hay).
```

Una decisión A/B (skill `iteracion-ab`) usa el mismo formato, en 15 líneas o menos: en
**Alternativas**, una línea por vuelta, con las opciones y lo que dijo el usuario; en **Costo**, solo
lo que queda pendiente. El detalle de valores va en el código y en las capturas, no acá.

## Decisiones

### 2026-11-24 · publico · Manifiesto de PUB2: las respuestas de la cuenta con el aviso en la página

- **Decisión:** Recuperar la contraseña, Pedir el enlace y Activar la cuenta responden en su misma
  pantalla con el aviso en la página del catálogo, sin página propia y sin el sello de recibido.
  «Solicitud enviada», de la organización, se queda como la página de estado de la solicitud, con
  «Solicitud recibida» como aviso en la página (DA-32). Reparto en 4 lotes de una tanda, sin fichas;
  el registro en una página usa `componentes/registro` como está
  ([backlog, sección 15](backlog-guia-al-codigo.md)).
- **Alternativas:** el aviso breve que se va solo, como Contacto; llevar Activar a Ingresar con un
  aviso breve; pasar «Solicitud enviada» a un aviso breve.
- **Por qué:** el aviso dice qué hacer después, y si se va, queda el formulario vacío y la persona
  vuelve a pedir el enlace. Ingresar es una pantalla de PUB1. A «Solicitud enviada» se vuelve con
  «Ver mi solicitud».
- **Costo:** las tres rutas de respuesta siguen en la maqueta. `.recibido` puede quedar sin uso en
  `publico-paginas.css`; si queda, se anota para Z.
- **Quién decidió:** el autor del manifiesto, con la opción recomendada, delegada por el
  relevamiento del 2026-11-24 (corrida desatendida, rama = gate; plan `.claude/plan-guia-resto.md`).
  El usuario no la respondió.

### 2026-11-24 · navegación · El menú público: Inicio, Entregas, Organizaciones y Cómo funciona (DA-01, DA-05, DA-17)

- **Decisión:** el menú público es Inicio, Entregas, Organizaciones y Cómo funciona
  (`/publico-nosotros`). El Mapa sale del menú y, en el Mapa, queda marcada Entregas. Entregas va sin
  «confirmadas», como en la guía. Cómo funciona lleva el ícono `#i-brote`, incluido como símbolo en un
  `svg.iconos` dentro del menú, sin editar `iconos.html`. En la barra de arriba, Inicio se ve desde
  68,75 em; en el celular, siempre. Solo cambia el menú público.
- **Alternativas:** dejar el Mapa en el menú; mostrar Inicio desde 56 em; traer entero
  `iconos :: comunes`; cambiar todos los menús juntos.
- **Por qué:** el Mapa es una vista de las entregas; entre 56 y 68,75 em la fila con Inicio no entra
  en un renglón y rompía `ArmazonFijoTest`; cada ola rehace el menú de su rol.
- **Costo:** entre 56 y 68,75 em Inicio no está en la barra (se llega por el logo).
- **Quién decidió:** usuario (DA-01, DA-05 y DA-17, rondas del 2026-11-22 y del 2026-11-23). El ancho
  de Inicio, el ícono y «Entregas», agente (lote `pub1-portada`). Consta como no cumplida: la revisión
  de pantallas de la ola no llegó a PASS; el comportamiento está en el código y en los tests.
- **Evidencia:** backlog §13.3 (PUB1-02); `navegacion-publica.html`, `26ebd4af3`.

### 2026-11-24 · portada · La portada sigue a A01, sin las abejas, el corte bajo tierra ni el cierre de hoy (DA-22)

- **Decisión:** la portada usa `layout-publico.html` y va en el orden de A01: el título sobre la
  trama (sin el papel debajo de cada letra), el dibujo con un fruto por entrega confirmada, la última
  entrega con su foto y su sello, las cifras, «Cómo funciona» con la raíz numerada de 5 pasos y hasta
  3 entregas más del último mes con «Ver todas». «Quiero donar» lleva a `/publico-registro-donante` y
  «Soy una organización» a `/publico-registro-entidad`. La última entrega dice «<organización>
  confirmó la entrega con una foto.», sin «Ver las 2 fotos». El título del mes es «Entregas
  confirmadas del último mes», porque el puerto trae un mes corrido. El texto del dibujo no cuenta
  frutos (el SVG dibuja 9 fijos): la cuenta va en los frutos de debajo. El pie de cada entrega lleva
  el año.
- **Alternativas:** seguir con la cabecera propia de la portada; conservar las piezas de hoy debajo de
  A01 (descartada en DA-22); «Ver las N» de A01; mandar «Quiero donar» a Cómo donar.
- **Por qué:** sin lámina a todo el ancho, el armazón ya trae el salto al contenido, el indicador de
  carga y la precarga de las letras; no hay un detalle público de una entrega ni la cuenta de sus
  fotos; los registros son lo que ya enlazaba la portada.
- **Costo:** fallan `AbejasYRenglonesTest.conReducirMovimientoLasAbejasQuedanQuietasDelanteYEnteras` y
  los dos de `BajoTierraRotulosTest`, que miden piezas que la portada ya no tiene: los decide el
  usuario. La etiqueta de la última entrega no tiene el marco protagonista de A01. La sección del mes
  no tiene estado de error, bloqueante del lote ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítems 1, 3
  y 6).
- **Quién decidió:** usuario (DA-22, sin respuesta: la opción recomendada, delegada por el relevamiento del 2026-11-24). Lo
  demás, agente (lote `pub1-portada`), sin consultar. Consta como no cumplida: la revisión de
  pantallas de la ola no llegó a PASS.
- **Evidencia:** backlog §13.3 (PUB1-01); `publico-portada.html`, `26ebd4af3`, `1878a1e39` y
  `33e1c7284`.

### 2026-11-24 · portada · Las cifras van en una frase, en la portada y en Cómo funciona

- **Decisión:** bajo «Cómo sabemos que llega», las cifras son una frase con cada número en `<strong>`
  y `.texto-grande` («Hasta hoy, 1.284 entregas confirmadas, …»), con los rótulos del puerto en
  minúscula y la fecha `actualizado` del puerto («Actualizado el …», solo con datos). Cómo funciona
  copia el mismo marcado con `cifrasDeNosotros`. El vacío va con `.vacio data-forma="filetes"`.
- **Alternativas:** `huerta :: cifras` (otro título, árboles que no entran en media columna y una fecha
  fija); la lista de datos (`componentes/datos`), que fue la primera versión, con las columnas
  `minmax(0,1fr) auto` para que «Organizaciones» no se montara sobre el número a 360 px; informar una
  variante de `.datos` como pieza faltante; la fecha fija en la plantilla.
- **Por qué:** A01 no usa una lista de datos y las cifras no son «lo asentado»; la fecha fija sería un
  dato de ejemplo escrito en la plantilla.
- **Costo:** con el adaptador real los rótulos de Cómo funciona no llevan período (`PublicoRest`);
  `huerta :: cifras` quedó sin llamadores ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítem 6).
- **Quién decidió:** agente (lotes `pub1-portada` y `pub1-conocer`, en sus rondas de arreglo), sin
  consultar.
- **Evidencia:** `publico-portada.html`, `publico-nosotros.html`; `33e1c7284` y `3b08f925f`.

### 2026-11-24 · estados · Los errores de carga de lo público son el banner con el código

- **Decisión:** en la portada y en Cómo funciona, un error de carga es el banner de error
  (`data-estado="error"`) con «Es un problema nuestro. Probá de nuevo en unos minutos; si sigue,
  escribinos con este código.», el enlace a Contacto con el código y la vuelta, `.aviso-codigo` y
  «Volver a cargar», como en Entregas, el Mapa y Organizaciones. Las cifras de la portada llevan el
  banner en línea, con su propio id (`cifras-error-titulo`).
- **Alternativas:** el renglón de filetes con `role="alert"` (la versión de las rondas de los lotes);
  dos `avisoError` en la misma página; cambiar el fragmento `avisoError`.
- **Por qué:** lo que se parece se comporta igual (U1, U4); `avisoError` tiene fijo `id="error-titulo"`
  y dos repetirían el id, y su firma no cambia porque lo usan pantallas de PUB2.
- **Costo:** la sección de las entregas del mes de la portada sigue sin estado de error
  ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítem 1).
- **Quién decidió:** agente (revisión de pantallas de la ola, ronda 2), sin consultar.
- **Evidencia:** `publico-portada.html`; `5556086f0`.

### 2026-11-24 · entregas · Entregas y el Mapa: frutos, la etiqueta de cada entrega y quién la recibió (DA-01, DA-10)

- **Decisión:** Entregas cuenta en frutos (`componentes/huerta :: frutos`, uno por entrega) y muestra
  cada entrega con la etiqueta del catálogo, con «Recibió: <la organización>» (nunca una persona) y
  la categoría y el barrio en el lugar («Muebles · Centro Ejemplo»); «Foto de ejemplo» cuando la foto
  no es una dirección http(s); «Ver la foto de la entrega» fuera de la etiqueta. Un período sin
  entregas muestra solo los frutos en cero. Entregas enlaza el Mapa, y el Mapa vuelve por sus migas
  «Entregas / Mapa», sin rótulo ni parte en otra tinta en el título. La cuenta del Mapa va en texto,
  con la cifra en `<b>`. El listado del Mapa no queda fijo. `publico-mapa.css` corrige que MapLibre
  corra el mapa 82 px desde 68,75 em (`top: 0`). A 360 px, Categoría va en su renglón y Filtrar
  debajo. En Cómo funciona, «Cuidamos los datos» dice que lo publicado nunca muestra quién donó.
- **Alternativas:** la cifra heredada con árbol (`.cifra-fruto`); frutos en el Mapa (con el tope de
  Logística pueden ser cientos); el pictograma de la categoría, que la etiqueta no tiene; columnas de
  22 rem (daban tres a 1280 px); `grid-template-columns: 1fr` en el listado a 360 px.
- **Por qué:** como A10 y Entregas de la organización; la etiqueta del catálogo no tiene lugar para el
  pictograma; en lo público quien recibe es la organización (HG-03).
- **Costo:** el número dentro del sello del Mapa (`.estacion-numero`) sigue heredado, y con él
  `publico-mapa.css` en `HOJAS_HEREDADAS`; el arreglo del mapa es de disposición y el de fondo es del
  catálogo ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítem 6). Con datos reales llegan hasta 50
  frutos; no se probó cómo se ven.
- **Quién decidió:** usuario (DA-01 y DA-10). Lo demás, agente (lote `pub1-entregas` y revisión de
  pantallas de la ola, rondas 1 y 2). Consta como no cumplida: la revisión de pantallas de la ola no
  llegó a PASS.
- **Evidencia:** backlog §13.3 (PUB1-03, PUB1-04); `8cfbaab5d`, `a56418c53`, `60a4c976f`,
  `fb9e34317` y `5556086f0`.

### 2026-11-24 · organizaciones · Organizaciones públicas: tarjetas por grupo, de a 6, filtradas en el controller

- **Decisión:** los filtros y las páginas pasan de la plantilla a `PublicoEntidadesController`, sobre
  la lista que ya da el puerto: se ordena por grupo, se corta la página de 6 y se agrupa la página
  (Comedores y merenderos, etc.). El total de cada organización va en el atributo `vigentes`
  (pedidos sin filtrar). Lo satisfecho va en palabras en el pie de la tarjeta, sin barra; el
  plegable, solo para las necesidades que pasan de 3, y la misión en `.recorte`, fuera del plegable.
  Las salidas son dos botones, «Quiero donar» y «Sumar mi organización». El fragmento `pantalla`
  conserva su firma.
- **Alternativas:** paginar en la plantilla (Thymeleaf no arma el paginador); sumar un campo al record
  `Entidad` del puerto; una vista propia; sacar los grupos, que A02 no tiene; la salida heredada
  `.salidas` con «¿Ya tenés cuenta? Ingresar».
- **Por qué:** el paginador necesita el total ya filtrado; ninguna decisión autoriza sacar los grupos y
  los tests los afirman; dos barras en la misma tarjeta confundían; sin anidar `details` (DTI-48).
- **Costo:** el controller tiene lógica (busca, filtra, ordena y pagina), contra lo que pide
  `cliente-liviano/AGENTS.md`; con la API paginada, la búsqueda solo ve la primera respuesta; las
  ramas `yaDonadas` del fragmento ya no las usa nadie ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50),
  ítems 4 y 5).
- **Quién decidió:** agente (lote `pub1-organizaciones`), sin consultar.
- **Evidencia:** backlog §13.3 (PUB1-05); `375608498`, `a1cb6457c` y `8cef6d2ee`.

### 2026-11-24 · ingresar · Ingresar es la columna del formulario de A12

- **Decisión:** una sola columna, con el h1 «Ingresar» y su bajada, y las cuentas de ejemplo debajo
  del formulario en una tarjeta `data-papel="hueco"`. El error es un solo mensaje (`.campo-error`, con
  el signo del catálogo) en la contraseña, con los dos campos marcados y el resumen enlazado a
  `#clave`. Salen el «o» con filetes, los discos con la inicial del rol y `boton--google`: «Continuar
  con Google» es un `.boton` secundario junto a Ingresar. La contraseña con «Mostrar» va en un
  `.racimo`, sin placeholder. El estado lento lleva `aria-busy` y «Ingresando…», sin `disabled`.
- **Alternativas:** las dos columnas de antes; conservar el rótulo y `.publica-cabeza`; un mensaje por
  campo; «Mostrar» chico o en otro renglón; `disabled` en el envío.
- **Por qué:** el arquetipo es la columna del formulario; dos mensajes repetían el texto y uno sugería
  cuál falló (RNF-AUTH-10); el ingreso no valida el largo de la contraseña.
- **Costo:** los selectores nuevos de `publico-ingresar.css` son estructurales y los heredarán activar
  y recuperar (PUB2) cuando migren; `data-papel="hueco"` estira el sentido de «lo que falta»;
  `.enlace-campo` de `publico-registro.css` quedó sin uso ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50),
  ítem 7).
- **Quién decidió:** agente (lote `pub1-ingresar`), sin consultar.
- **Evidencia:** backlog §13.3 (PUB1-06); `5ce687f2d` y `09942eb6c`.

### 2026-11-24 · conocer · Cómo funciona, Cómo donar y Ayuda, con la composición de A01 (DA-05, DA-23)

- **Decisión:** Nosotros, Para donantes y Para organizaciones se llaman Cómo funciona, Cómo donar y
  Ayuda, con las rutas de hoy, y el pie los nombra igual. Las tres llevan el título sobre la trama
  (sin la máscara de papel y sin `<em>`) y la raíz de 5 pasos con el texto de su público. Cómo
  funciona sube sus dos salidas a la cabecera, a los registros con los rótulos de la portada («Quiero
  donar» y «Soy una organización»), y pierde la sección «Sumate». Cómo donar y Ayuda tienen una sola
  acción llena y las preguntas frecuentes en `.plegable`, con «Ver» oculto a lectores. En Ayuda,
  «Tu cuenta y tus datos» son dos tarjetas con su h3.
- **Alternativas:** el botón repetido al final, en secundario; «Cómo donar» y «Ayuda para
  organizaciones» como salidas de Cómo funciona; `details` sin clase; la lista en párrafos.
- **Por qué:** una sola acción llena por página; cada rótulo lleva a un solo lugar en todo el sitio;
  lo que se parece se comporta igual.
- **Costo:** «Qué ves con tu cuenta» de Cómo donar sigue como lista suelta, distinta de Ayuda
  (bloqueante del lote tras 3 rondas, [DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítem 2). Las fichas de
  flujo conservan los nombres viejos (ítem 8).
- **Quién decidió:** usuario (DA-05, ronda 4 del 2026-11-23; DA-23, sin respuesta: la recomendada, delegada por el
  relevamiento del 2026-11-24). La composición, agente (lote `pub1-conocer` y revisión de pantallas
  de la ola, ronda 1). Consta como no cumplida: la revisión de pantallas de la ola no llegó a PASS.
- **Evidencia:** backlog §13.3 (PUB1-07, PUB1-08); `6c5dc109c`, `9738e96df`, `3b08f925f` y
  `fb9e34317`.

### 2026-11-24 · contacto · El envío de Contacto termina en Contacto, con el número en el aviso (DA-06, DA-24)

- **Decisión:** enviar vuelve a Contacto con un aviso breve que lleva el número («Recibimos tu
  mensaje. Su número es 7.»); el campo trampa da el aviso sin número. Con `?volver=`, Contacto ofrece
  «Volver a …», un botón secundario, antes y después de enviar. Ya no hay «Volver a mi inicio» ni «Ir
  a la portada». Sale la lámina «Cuándo escribirnos» con su ilustración. «Enviar mensaje» va en un
  racimo, como «Ingresar». En Legales, el aviso de texto de ejemplo es `.aviso data-tipo="info"`
  («Texto de ejemplo»).
- **Alternativas:** una página de respuesta propia; «Volver a …» como enlace suelto; conservar la lámina
  al costado; un número inventado para el spam.
- **Por qué:** A12 es una sola columna de formulario; con sesión, el menú del marco ya lleva al
  inicio; para spam no se inventa un número.
- **Costo:** la lámina venía del A/B aprobado el 2026-11-04: si vuelve, lo decide el usuario. Si el
  envío real se rechaza, se pierde el `?volver=`. La maqueta muestra un aviso más rico que el envío
  real ([DTI-50](../adr/DEUDA_TECNICA.md#dti-50), ítem 9).
- **Quién decidió:** usuario (DA-06, ronda 4 del 2026-11-23; DA-24, sin respuesta: la recomendada, delegada por el relevamiento del
  2026-11-24). Lo demás, agente (lote `pub1-contacto` y revisión de pantallas, ronda 1). Consta como no
  cumplida: la revisión de pantallas de la ola no llegó a PASS.
- **Evidencia:** backlog §13.3 (PUB1-09, PUB1-10); `35f6c8307` y `fb9e34317`;
  `ErroresConSesionTest` actualizado al comportamiento nuevo.

### 2026-11-24 · donaciones · Arreglo después de la ola DON: la misma palabra para el mismo estado

- **Decisión:** la vista se llama «Confirmadas» y la opción del filtro, «Entrega confirmada». El
  detalle y el seguimiento usan la regla de la lista (Registrada, Asignada, Entrega confirmada) y
  «Donación 0005»; la raíz nombra los pasos por su lugar, como la tabla. La nota del seguimiento y
  «Cómo va» del detalle en camino dicen «En camino desde las HH:MM» (HG-02). Los datos de ejemplo de
  Organizaciones de la persona dicen lo mismo que los de la empresa («Faltan los 20 litros», con la
  barra vacía).
- **Alternativas:** «En curso» en lugar de «Pendientes», como la guía; tomar el nombre del paso del
  dato.
- **Por qué:** la lista ya tiene la vista «En camino», y «En curso» la incluiría; el dato trae los
  nombres del backend («En depósito», «Lista para entregar»), que no dice ninguna otra pantalla.
- **Costo:** la raíz supone 5 pasos en el orden de la guía; si el dato trae otra cantidad, queda su
  nombre.
- **Quién decidió:** agente, sobre los bloqueantes de la revisión de `don-donaciones`, `don-detalle`
  y de pantallas ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 1 a 3).
- **Evidencia:** `MisDonacionesController.java`, `donante-donaciones-detalle.html`,
  `donante-seguimiento.html` y sus tests.

### 2026-11-24 · navegación · El menú de quien dona: Inicio, Donaciones, Organizaciones, Misiones y Cómo donar (DA-03a, DA-03b, DA-05, DA-17)

- **Decisión:** la empresa ve Inicio, Donaciones, Organizaciones y Cómo donar; la persona, además,
  Misiones antes de Cómo donar. Seguimiento deja de ser una sección: se abre desde el detalle de
  cada donación y su pantalla queda marcada en Donaciones. Cómo donar es la página de hoy Para
  donantes (`/publico-para-donantes`), con el ícono `nav-ayuda`. Solo cambian los menús de quien dona.
- **Alternativas:** dejar Seguimiento en el menú; Misiones al final; una página nueva de «Cómo
  donar»; cambiar todos los menús juntos en C1.
- **Por qué:** el seguimiento es de una donación, no de la cuenta; la página ya explica cómo donar;
  cada ola rehace el menú con las pantallas a las que lleva.
- **Costo:** `EN_LA_SECCION` vale ahora también para las pantallas de nivel 3 por prefijo: una ruta
  inexistente que empiece como una de ellas muestra «sin dibujar» en vez de 404. Quien entra a Cómo
  donar sale del área privada a una página pública.
- **Quién decidió:** usuario (DA-03a, ronda 2 del 2026-11-22; DA-03b y DA-05, ronda 4 del
  2026-11-23; DA-17, ronda 3 del 2026-11-22). El ícono y el prefijo de `EN_LA_SECCION`, agente
  (lote `don-inicio`). Consta como no cumplida: la revisión de pantallas de la ola no llegó a PASS;
  el comportamiento está en el código y en los tests.
- **Evidencia:** backlog §12.3 (DON-02); `Pantallas.java`, `524d86d6d`.

### 2026-11-24 · inicio · El inicio de la empresa es A04 y deja el tablero de tres columnas

- **Decisión:** aviso del problema arriba, una sola lámina protagonista con la donación que viaja
  (raíz de 5 pasos y el mapa del recorrido, decorativo, en un `.marco`), «También en curso» y «Ya
  confirmadas en `<mes>`» con un fruto por entrega del mes, del 1 a hoy. Sale el tablero de R11-25 con
  sus paginadores, y salen las cifras con árboles, el hito, las últimas noticias, la pista y las
  raíces acumuladas: A04 no los tiene. Las noticias siguen en la campanita. Sin nada en camino, la
  lámina dice «Nada en camino ahora» y deja de ser protagonista (escenario nuevo `sin-camino`).
- **Alternativas:** conservar la llamada al tablero solo por los totales; un ícono nuevo para Cómo
  donar; los rótulos del mapa de la guía (depósito, patente, destino), que el catálogo no tiene.
- **Por qué:** la lista del inicio ya trae todo y da los totales; una sola protagonista y una sola
  acción llena (piso de la guía).
- **Costo:** el método `tablero()` del puerto y sus adaptadores quedan sin uso hasta Z
  ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítem 6). Los 7 tests del tablero se reescribieron como
  tests de A04, con aserciones igual de fuertes.
- **Quién decidió:** agente (lote `don-inicio`), sin consultar; el título con el mes, agente, al
  arreglar la ronda 1 de la revisión de pantallas.
- **Evidencia:** backlog §12.3 (DON-01); `524d86d6d`, `91f31ec86` y `2cd69d8ba`.

### 2026-11-24 · textos · Lo que el servicio no da no se escribe: HG-01 y HG-02 con su texto de reemplazo

- **Decisión:** la hora sale de la nota que ya llega: «Salió hoy a las X» se muestra «En camino desde
  las X», sin hora estimada (HG-02). Una donación que no pudo llegar dice el motivo, la última noticia
  y «Todavía sin fecha de nueva salida» (HG-01). En el detalle se omite lo que el adaptador no
  entrega: «Sale del depósito», la necesidad que cubre (HG-04) y el texto de cada paso; la raíz muestra
  la fecha de cada paso o «Pendiente». Los datos de ejemplo de Útiles escolares pierden la fecha
  inventada de la nueva salida.
- **Alternativas:** imprimir la nota de la maqueta («sale de nuevo el 17»); escribir en la plantilla
  los datos de la guía.
- **Por qué:** la regla de la ola: un dato que no llega se dice con el texto de «Si no llega» (§7).
- **Costo:** el reemplazo depende del texto exacto de la nota; el seguimiento y el detalle en camino
  todavía no lo usan ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítem 2). Quedan fechas inventadas en
  `viaje-datos.html` y `DonanteNotificacionesMaqueta` (ítem 9).
- **Quién decidió:** agente (lotes `don-inicio` y `don-detalle`), sin consultar.
- **Evidencia:** backlog §7 y §12.3; `donante-inicio.html`, `DonacionesMaqueta.java`.

### 2026-11-24 · inicio · El inicio de la persona deriva de A04, con la huerta (DA-03c, DA-08, DON-P2, DON-P3)

- **Decisión:** como su ficha: aviso del problema arriba, una sola protagonista (la que viaja o, sin
  ninguna, la huerta con «Ver mi huerta» llena), «También en curso», «Ya confirmadas en `<mes>`» solo
  con las del mes, la huerta chica, el almanaque de 12 meses en una fila con un fruto por entrega
  confirmada y las 3 últimas novedades. La lista mes por mes sale. El mes del almanaque va abreviado,
  con el nombre completo para lectores; 3 columnas en el celular, 6 desde 37,5 em y 12 desde 68,75 em.
  Los datos son los 8 de Martina Ruiz que ya leen su detalle y su seguimiento.
- **Alternativas:** el ejemplo dibujado en la ficha (Arroz en camino, Ropa con problema); los nombres
  completos de los meses en una fila; una lista propia de las confirmadas del mes en la vista.
- **Por qué:** las mismas claves en todas las pantallas de la persona; «Septiembre» no entra a 1280 px
  y un mes más angosto mediría menos de 48 px; la vista y el adaptador quedan fuera del lote.
- **Costo:** los frutos de cada mes salen de las hasta 3 donaciones que trae el año, con «+N»: en la
  maqueta, octubre muestra 0 frutos y 1 entrega confirmada ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49),
  ítem 10). Martina no tiene donaciones con problema: el aviso se prueba con mocks.
- **Quién decidió:** usuario (DA-03c y DA-08, ronda 4 del 2026-11-23; DON-P2 y DON-P3, ronda 5 del
  2026-11-23, «va asi»); la disposición y los datos, agente (lote `don-inicio-persona`). Consta como
  no cumplida: la revisión de pantallas de la ola no llegó a PASS.
- **Evidencia:** ficha [`guia-don-inicio-persona.md`](flujos/guia-don-inicio-persona.md); backlog
  §12.3 (DON-09); `d282ddcc9` a `9d7c5990c`.

### 2026-11-24 · donaciones · Donaciones es A05, con una vista por estado del filtro

- **Decisión:** vistas Todas, Pendientes, En camino, Entregadas y Con problema con su cuenta, como
  `.segmentos` (enlaces con `aria-current`), sobre el mismo `?estado=` del filtro; la tabla lleva un
  sello y una raíz por fila. El sello dice Registrada (pendiente sin organización), Asignada (con
  ella) o Entrega confirmada, igual que el detalle. El paso de la raíz sale del estado y de la
  organización. La persona usa la misma composición (DA-13).
- **Alternativas:** `.pestanas` del catálogo, que es una pieza de paneles y sin JavaScript marca
  siempre la primera; paneles por estado, que no se combinan con el paginador del servidor; un
  parámetro nuevo para «En curso».
- **Por qué:** la vista es un enlace a la misma lista filtrada; el sello no puede contradecir al
  detalle.
- **Costo:** las vistas se ven como pastillas y no como solapas; cada carga hace 4 pedidos más a
  Donaciones, uno por cuenta. La vista «Entregadas» y el filtro «Entregada» no coinciden con el
  sello «Entrega confirmada»: bloqueante abierto ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 1 y 6).
- **Quién decidió:** agente (lote `don-donaciones`), sin consultar; DA-13 (usuario, ronda 4 del
  2026-11-23), que consta como no cumplida: la revisión de pantallas no llegó a PASS.
- **Evidencia:** backlog §12.3 (DON-03); `53712e382`, `d8403c8d5` y `642c1698e`.

### 2026-11-24 · detalle · El detalle es una plantilla con las variantes de la guía; el seguimiento se abre desde él (DA-03a)

- **Decisión:** la variante sale del estado, el destino y el problema: asignada (A06, sello
  «Asignada»), en camino (A07, con el botón secundario «Ver dónde está el camión» a su seguimiento) y
  con problema (A08, el motivo una vez y la última noticia en «Qué sigue»). La entregada y la que
  espera destino siguen la composición de A06; la entregada pone la etiqueta de entrega. El detalle
  y el seguimiento de la persona salen de `persona/datos-persona`, la misma fuente.
- **Alternativas:** dejar «Pendiente» en la asignada; una variante nueva para la entregada; el mapa
  dentro del detalle, que pedía otro puerto en el controller; que `DonacionesMaqueta` lea la sesión.
- **Por qué:** el sello y el paso actual no se contradicen; el detalle lleva al lugar donde está el
  mapa sin sumar dependencias.
- **Costo:** quedan bloqueantes del lote: el número con «N.º», la que espera destino con «Pendiente»
  y dos aserciones que no pueden fallar ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 2 y 3).
- **Quién decidió:** usuario (DA-03a, ronda 2 del 2026-11-22; DA-13, ronda 4); las variantes y la
  fuente de la persona, agente (lote `don-detalle`). Consta como no cumplida: la revisión de
  pantallas no llegó a PASS.
- **Evidencia:** backlog §12.3 (DON-04); `39efd6083`, `9572f7047` y `e4cc56f3f`.

### 2026-11-24 · seguimiento · El mapa del seguimiento: el marco del catálogo, el camión solo si viaja y márgenes propios

- **Decisión:** el mapa y su nota van en `[data-parte="mapa"]`, con `.mapa-dibujo` envolviendo a
  `.mapa-real`; el camión aparece solo si la donación va en camino. Los marcadores miden el toque
  mínimo más 1 px, porque MapLibre los ubica en fracciones de píxel (47,99997 px). `mapa.js` acepta
  un atributo opcional, `data-mapa-margenes`, y el seguimiento usa 64 96 136 64 para que ningún punto
  quede bajo los botones o la atribución; sin el atributo, todo sigue en 64. En el celular el mapa es
  3/4. Se elige la donación con `?donacion=` o, sin él, la primera en camino.
- **Alternativas:** un solo elemento `mapa-dibujo mapa-real`, que en pantalla ancha baja el mapa
  sobre la nota; anular el `top` en la hoja; un mapa más alto sin cambiar el encuadre; mover las
  coordenadas de ejemplo para pasar el chequeo.
- **Por qué:** el arreglo queda en la pantalla o en un atributo que las demás no usan; no se fuerza
  un verde con los datos.
- **Costo:** el defecto del marco y la atribución de dos renglones siguen en `componentes/mapa.css`
  para `publico-mapa` ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítem 7).
- **Quién decidió:** agente (lote `don-detalle` y arreglos de la revisión de pantallas), sin consultar.
- **Evidencia:** backlog §12.3 (DON-05); `componentes.md` (fila del mapa); `2cd69d8ba` y `9b2d3e147`.

### 2026-11-24 · organizaciones · Organizaciones deriva de A02 sin atenuar las tarjetas a las que ya donaste

- **Decisión:** una tarjeta del catálogo por organización; «Ya le donaste» en palabras y no con la
  tarjeta atenuada; la necesidad sin porcentaje, con la cuenta en palabras («Faltan 30 de 40 kg este
  mes») y la barra con su `aria-label`; «Ver N necesidades más» con el `.plegable` de un solo rótulo;
  «Cómo llega tu donación» como `.aviso` de información, con «Ver tus donaciones». La persona usa el
  mismo fragmento `hoja` (DA-13).
- **Alternativas:** repintar la tarjeta atenuada en la hoja; `data-papel="tierra"`, que dice «lo que
  va a pasar»; filas de alto igual en toda la página (`grid-auto-rows: 1fr`), que dejaba huecos.
- **Por qué:** el catálogo no tiene una tarjeta atenuada y la hoja de una pantalla solo dispone.
- **Costo:** la maqueta de la persona sigue con sus datos en la plantilla
  ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítem 9).
- **Quién decidió:** agente (lote `don-organizaciones`), sin consultar; DA-13, usuario, que consta
  como no cumplida: la revisión de pantallas no llegó a PASS.
- **Evidencia:** backlog §12.3 (DON-06); `f67f49d3e` y `2cd69d8ba`.

### 2026-11-24 · perfil · El perfil de quien dona sigue al de la organización y guardar da un aviso breve (DA-06)

- **Decisión:** una columna, un formulario por sección; el CUIT y el documento como texto fijo con
  «No se cambia desde acá»; el escenario lento afecta los 3 formularios con `aria-busy` y
  «Guardando…». Guardar vuelve al perfil con el aviso breve del catálogo (fragmento
  `donante-perfil :: aviso`); en la maqueta, `/donante-perfil-guardado` es el perfil con el aviso. El
  buzón de quien dona nombra `donante-comun.css`, donde vive su `.bajada`, y la hoja
  `donante-notificaciones.css` se borra.
- **Alternativas:** el `input readonly`, que sin estilo parece editable; los pares de campos lado a
  lado; que el buzón nombre `privado.css`, que le sacaba el estilo a la bajada.
- **Por qué:** las dos áreas se ven iguales con las mismas piezas; la respuesta se ve donde se hizo
  la acción.
- **Costo:** el buzón queda atado a una hoja heredada; la versión de la empresa no salió en las
  capturas por la sesión de la maqueta ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 7 y 8).
- **Quién decidió:** usuario (DA-06 y DA-13, ronda 4 del 2026-11-23); la forma, agente (lote
  `don-perfil`). Consta como no cumplida: la revisión de pantallas no llegó a PASS.
- **Evidencia:** backlog §12.3 (DON-07, DON-08); `ae68989d2` y `69edbeb07`.

### 2026-11-24 · misiones · Misiones es la huerta, con lugares de 48 px y sin el álbum (DA-08, DA-21, DON-P1)

- **Decisión:** según su ficha: la huerta en una sola lámina protagonista, sin acción llena. Cada
  lugar mide 48 × 48 px como mínimo con el toque encima del dibujo; por debajo de 53,75 em cada lugar
  es un renglón de 48 px con su dibujo, el nombre y el estado. Los hitos y las misiones usan el sello
  del catálogo (completa → entregada, en curso → en camino, en espera → pendiente; las cerradas sin
  sello) y el mensaje de la organización va en una `.tarjeta`. Se borran `misiones-a` y `misiones-b`
  con sus hojas.
- **Alternativas:** el área de toque superpuesta en todos los anchos, que a 360 px pisa unos 26 px
  entre lugares; una sección aparte con la categoría actual; el ícono de categoría, cuyo estilo vive
  en una hoja que la pantalla no carga.
- **Por qué:** una sola Misiones que mantener; la hoja de la pantalla solo dispone.
- **Costo:** quedan sin uso `huerta-armada.svg` y `Historia.categoria`; el contorno punteado al pasar
  y algunas medidas siguen en la hoja ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítems 7 y 9).
- **Quién decidió:** usuario (DA-08 y DA-21, ronda 4 del 2026-11-23; DON-P1, ronda 5 del 2026-11-23);
  la disposición, agente (lote `don-misiones` y arreglos de la revisión de pantallas). Consta como no
  cumplida: la revisión de pantallas no llegó a PASS; las dos rutas de `/donante-misiones` pasan el
  toque.
- **Evidencia:** ficha [`guia-don-misiones.md`](flujos/guia-don-misiones.md); backlog §12.3 (DON-10);
  `79b073dfb`, `2cd69d8ba` y `9b2d3e147`.

### 2026-11-24 · variantes · Una variante que no existe se ignora, en lugar de dar 404

- **Decisión:** `?variante=<nombre>` sin archivos muestra la pantalla igual que sin el parámetro, y
  no carga ninguna hoja de `/css/variantes/`. Así `?variante=misiones-a`, ya borrada, muestra la
  huerta (DON-P1). El nombre de ruta y de vista sigue saliendo solo de los archivos que existen.
- **Alternativas:** ignorar solo `misiones-a`, que obligaba a escribir el nombre en el código; aceptar
  el 404 como «deja de servirse».
- **Por qué:** el criterio de DON-P1 pide la misma pantalla, sin nombrar el álbum fuera de `docs/`.
- **Costo:** vale para todas las rutas: un enlace con una variante equivocada ya no avisa con un 404.
  Dos tests de `VariantesInterceptorTest` pasaron de afirmar el 404 a afirmar la pantalla sin la hoja.
- **Quién decidió:** agente (arreglos de la revisión de pantallas, ronda 2), sin consultar; lo puede
  revisar el usuario ([DTI-49](../adr/DEUDA_TECNICA.md#dti-49), ítem 5).
- **Evidencia:** `VariantesInterceptor.java`, `9b2d3e147`.

### 2026-11-24 · admin · Manifiesto de ADM1: Por decidir en dos rutas y el galpón con barra arriba

- **Decisión:** `/admin-inicio` y `/admin-asignaciones` llevan la misma cabecera y la misma barra
  de pestañas de A03; el inicio abre Propuestas y debajo lleva el resumen y los gráficos (DA-25).
  La tabla con selección aprueba o descarta varias propuestas, una por una contra el puerto de hoy
  (DA-26). El galpón conserva la barra superior fina en todos los anchos y lleva las secciones al
  cajón (DA-27). Fichas: [Ranking](flujos/guia-adm-ranking.md), sin podio y con el mes como hoy
  (DA-28 y DA-31), y [los gráficos del inicio](flujos/guia-adm-inicio-graficos.md), con un solo
  resumen denso, sin «Entregas para mirar hoy», y los gráficos en la pantalla (DA-29 y DA-30).
  Reparto en 7 lotes de 2 tandas ([backlog, sección 14](backlog-guia-al-codigo.md)).
- **Alternativas:** una sola ruta que redirige a la otra; selección sin envío conjunto o un pedido
  en lote nuevo a Donaciones; la guía tal cual, sin barra arriba en escritorio; un podio; conservar
  las cuatro tarjetas; los gráficos como pieza del catálogo. La opción descartada de cada una está
  en §2.2 del backlog.
- **Por qué:** los tests y el ingreso del admin llegan a las dos rutas; el puerto y el adaptador
  `rest` no cambian en una ola de rol; `ArmazonFijoTest` mide la cabecera del admin y nadie lo edita;
  el resto sigue la densidad del galpón sin piezas nuevas.
- **Costo:** Solicitudes y Por vencer llevan a otras pantallas en lugar de abrirse en la bandeja; la
  barra superior del galpón se aparta de la guía en escritorio; falta «Entregas para mirar hoy».
- **Quién decidió:** el autor del manifiesto, con la opción recomendada, delegada por el
  relevamiento del 2026-11-24 (corrida desatendida, rama = gate; plan `.claude/plan-guia-resto.md`).
  El usuario no las respondió.

### 2026-11-24 · publico · Manifiesto de PUB1: la portada de A01 sin las abejas

- **Decisión:** la portada sigue al arquetipo A01, sin las abejas, el corte «bajo tierra» ni el
  cierre de la portada de K (DA-22). El pie nombra Nosotros, Para donantes y Para organizaciones
  como «Cómo funciona», «Cómo donar» y «Ayuda» (DA-23). Contacto responde con un aviso breve en
  Contacto que lleva el número del mensaje (DA-24). Reparto en 6 lotes de una tanda
  ([backlog, sección 13](backlog-guia-al-codigo.md)).
- **Alternativas:** conservar las abejas y el corte debajo de A01; dejar el pie con los nombres de
  hoy; llevar el envío de Contacto a la pantalla de origen.
- **Por qué:** la portada es una pantalla «directa» y la guía no dibuja abejas; cada enlace del pie
  nombra la página a la que lleva; la portada no tiene región de avisos, así que el aviso se
  perdería.
- **Costo:** `AbejasYRenglonesTest` y `BajoTierraRotulosTest` van a fallar en lo que miden de la
  portada de K. No se editan y lo decide el usuario, como el lema del pie en C1.
- **Quién decidió:** el autor del manifiesto, con la opción recomendada, delegada por el
  relevamiento del 2026-11-24 (corrida desatendida, rama = gate; plan `.claude/plan-guia-resto.md`).
  El usuario no las respondió.

### 2026-11-23 · donante · Fichas de DON: Misiones y el inicio de la persona con la guía

- **Decisión:** las fichas [`guia-don-misiones.md`](flujos/guia-don-misiones.md) y
  [`guia-don-inicio-persona.md`](flujos/guia-don-inicio-persona.md). Misiones: la huerta
  (`misiones-b`) queda como la única y el álbum (`misiones-a`) se borra (DON-P1); cada lugar, de
  48 × 48 px. El inicio de la persona deriva de A04 con una sola lámina protagonista: la donación en
  camino o, sin ninguna, la huerta con «Ver mi huerta» (DON-P3); el almanaque se queda chico, en
  una fila de 12 meses, y la lista mes por mes sale (DON-P2). La planta es una misión solo en
  Misiones; el fruto, una entrega confirmada.
- **Alternativas:** dejar la variante del álbum sin migrar; sacar el almanaque o dejarlo como hoy;
  una lámina vacía sin nada en camino.
- **Por qué:** una sola Misiones que migrar; el almanaque responde a que las personas no donan
  todos los días (usuaria, 2026-11-09); una sola protagonista y una sola acción llena (piso de la
  guía).
- **Costo:** la protagonista del inicio cambia según haya algo en camino.
- **Quién decidió:** usuario («va asi» a las fichas, las tres preguntas y el reparto, 2026-11-23).
- **Evidencia:** las fichas, §9; backlog §12.

### 2026-11-23 · datos · Recepción confirmada muestra lo asentado en Logística, no lo de la dirección

- **Decisión:** el problema de Recepción confirmada sale del motivo del reporte de Logística
  (`EntidadEntregaVista.problema`), y la llegada, solo del cambio a `EN_PUERTA` del historial. El
  puerto recibe el `?problema=` de la respuesta, pero solo lo usa la maqueta, que no guarda el
  acta. Sin cómo va la necesidad, «Tu necesidad» dice cuál es, como Llegó con un problema.
- **Alternativas:** seguir leyendo `?problema=` en la plantilla (al volver a abrir la página el
  problema desaparecía, y con un parámetro inventado aparecía uno que no existe); pedir a Donaciones
  la cuenta de la necesidad en esta pantalla (una llamada más por cada confirmación).
- **Por qué:** la página promete mostrar lo que quedó asentado y poder volver a abrirse (DA-06).
- **Costo:** la cuenta de donaciones de la necesidad no aparece en esta pantalla con datos reales;
  se conecta en Z con HG-04 ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 2).
- **Quién decidió:** agente, al arreglar en sesión los bloqueantes de la ronda 3 de `org-confirmar`.
- **Evidencia:** backlog §11 (ORG-11 y ORG-12); `EntidadConfirmarRestTest`.

### 2026-11-23 · navegación · Entregas reúne «Donaciones recibidas» y «Seguir entregas» (DA-02)

- **Decisión:** una sola pantalla, Entregas (`/entidad-seguimiento`), con Para confirmar, Por llegar y
  Confirmadas. `/entidad-recibidas` redirige ahí y conserva su consulta, así que los filtros de
  antes siguen andando. Los textos, las migas y las salidas que decían «Seguir entregas» o
  «Donaciones recibidas» pasan a «Entregas».
- **Alternativas:** dejar las dos pantallas; borrar `/entidad-recibidas` sin redirigir.
- **Por qué:** las dos mostraban las mismas entregas en momentos distintos; la redirección no rompe
  los enlaces guardados ni los de las novedades.
- **Costo:** los tests que afirmaban la ruta vieja se actualizaron con aserciones igual de fuertes.
- **Quién decidió:** usuario (DA-02, «3 si», ronda 2 del 2026-11-22; diferida a ORG por DA-17).
  Cumplida: PASS de la revisión de `org-entregas`, ronda 2.
- **Evidencia:** backlog §11.1 (ORG-07); `e7c9d1327` y `1520674a0`.

### 2026-11-23 · navegación · Ayuda de la organización es Para organizaciones (DA-05)

- **Decisión:** la entrada «Ayuda» del menú lleva a `/publico-para-entidades`, con otro nombre; su
  ícono es `nav-ayuda`, con el dibujo de `i-ayuda`.
- **Alternativas:** una página de ayuda nueva.
- **Por qué:** la página ya explica a la organización cómo funciona y no suma una pantalla antes del
  30/11.
- **Costo:** quien entra a Ayuda sale del área privada a una página pública.
- **Quién decidió:** usuario (DA-05, ronda 4 del 2026-11-23); el ícono, agente (lote
  `org-entregas`), sin consultar. Cumplida: PASS de la revisión de `org-entregas`, ronda 2.
- **Evidencia:** backlog §11.1 (ORG-09); `iconos.html`.

### 2026-11-23 · navegación · En ORG cambia solo el menú de la organización (DA-17)

- **Decisión:** Inicio, Necesidades, Entregas y Ayuda. Los menús de quien dona, el admin, la persona
  y lo público quedan como estaban.
- **Alternativas:** cambiar todos los menús juntos.
- **Por qué:** cada ola rehace el menú junto con las pantallas a las que lleva.
- **Costo:** hasta su ola, los otros roles conviven con sus entradas de hoy.
- **Quién decidió:** usuario (DA-17, ronda 3 del 2026-11-22). Cumplida: PASS de la revisión de
  `org-entregas`, ronda 2; `[VERIFIED]` en el recorrido con login.
- **Evidencia:** backlog §11.3; `Pantallas.java`.

### 2026-11-23 · avisos · Solo confirmar una recepción tiene página de respuesta (DA-06)

- **Decisión:** publicar, cambiar o dar de baja una necesidad termina en Necesidades con el aviso
  breve del catálogo (signo y cerrar, por `hx-swap-oob` a `#avisos`); en la maqueta, las rutas de
  respuesta siguen y muestran Necesidades con el aviso. Guardar el perfil responde en el perfil
  (DA-06-perfil). Confirmar una recepción conserva su página.
- **Alternativas:** una página de respuesta por acción, como antes.
- **Por qué:** la respuesta se ve donde se hizo la acción; confirmar es el momento que merece su
  página, con la raíz completa y lo que quedó asentado.
- **Costo:** la parte de Necesidades consta como no cumplida: el lote quedó sin PASS tras 3 rondas
  por bloqueantes de su hoja, no del aviso ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 1).
- **Quién decidió:** usuario (DA-06, ronda 4 del 2026-11-23). En el perfil, cumplida: PASS de la
  revisión de `org-perfil`, ronda 2. En Necesidades, el comportamiento está `[VERIFIED]` en el
  recorrido, sin PASS del lote.
- **Evidencia:** backlog §11.1 (ORG-05, ORG-06); `b99ddff71` y `9e409b266`.

### 2026-11-23 · buzón · La casilla del buzón mide 48 × 48 px de verdad (DA-21)

- **Decisión:** en cada fila y en «Seleccionar todas», el control nativo, transparente, de 48 px,
  queda encima de la casilla dibujada (`::before`, 1,375 rem, borde de tinta y `--radio-estampa`;
  elegida, llena de verde con su tilde), como la opción de C1-12. Solo con CSS: el marcado no cambia.
- **Alternativas:** extender la excepción de DA-16 a las rutas del buzón; que el test mida la
  etiqueta.
- **Por qué:** pasa el piso de toque sin tocar el test.
- **Costo:** la excepción de DA-16 de `/estilos` queda sin uso y sigue en el test, que no se edita
  ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 10).
- **Quién decidió:** usuario (DA-21, ronda 4 del 2026-11-23); el tamaño y el dibujo, agente (lote
  `org-buzon`). Cumplida: PASS de la revisión del lote, ronda 1; las tres rutas `/…-notificaciones`
  salen de la línea de base `[VERIFIED]`.
- **Evidencia:** backlog §11.3; `buzon.css`.

### 2026-11-23 · buzón · El rótulo, el título y la pestaña dicen «Novedades»

- **Decisión:** sobre el título, el rótulo «Novedades» en la organización y en quien dona, como en
  el dibujo de la ficha; en el admin, «Administración». La organización conserva «Tus novedades».
  El `<title>` de las 8 pantallas pasa a «Novedades - DonaTrack». De la ficha (§6) cambian solo el
  título, el total y el nombre del paginador («Páginas de novedades»).
- **Alternativas:** quitar el rótulo (las otras pantallas lo tienen); dejar «Notificaciones»
  (repite el título con otro nombre); cambiar también los vacíos y la búsqueda.
- **Por qué:** la pestaña dice lo mismo que el título; los demás textos los hereda la ficha de R11-14.
- **Costo:** quedan textos con «notificaciones» al lado de «Novedades», y en quien dona el rótulo
  repite el título ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 5).
- **Quién decidió:** agente (lote `org-buzon`), sin consultar.
- **Evidencia:** `d9e8b73d7`.

### 2026-11-23 · buzón · Los estados del buzón son los de lista del catálogo

- **Decisión:** en la organización y en quien dona, el error es un `.aviso` con
  `data-tipo="error"`, `role="alert"` y el signo «problema»; la carga, una región `aria-busy` con
  `.cargando` y el esqueleto de tres renglones. El botón de ícono es `.buzon-accion`, de 48 px, con
  el aro de foco en tokens. Ningún texto cambia.
- **Alternativas:** seguir con el `.banner` de `piezas-entidad` y `datos-donante`.
- **Por qué:** son las piezas que la guía usa para una lista con error o cargando.
- **Costo:** el admin sigue con el banner y el esqueleto heredados hasta ADM2.
- **Quién decidió:** agente (lote `org-buzon`), sin consultar.
- **Evidencia:** `d9e8b73d7`.

### 2026-11-23 · inicio · La puerta de Inicio: hasta cinco frutos y la raíz punteada sin entrega

- **Decisión:** el árbol dibuja hasta 5 frutos en los lugares de la guía y la cuenta exacta va
  debajo (`huerta :: frutos`). Sin entrega en la puerta y sin ninguna confirmada, el tramo al
  quinto nudo queda punteado. Las partes que cambian con el estado se pintan desde la hoja de la
  pantalla, con selectores `data-dibujo` y los tokens de uso.
- **Alternativas:** un fruto por entrega (con muchas no entran); ningún fruto (se pierde D4); colores
  en la plantilla o clases nuevas (los cercos no lo permiten).
- **Por qué:** la cuenta se lee siempre y el dibujo no se desborda.
- **Costo:** `entidad-inicio.css` sigue como hoja heredada; conviene llevar la puerta con sus estados
  al catálogo ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 3).
- **Quién decidió:** agente (lote `org-inicio`), sin consultar.
- **Evidencia:** `74f441ee4`.

### 2026-11-23 · inicio · Inicio deja lo que la guía no muestra y lleva el problema arriba

- **Decisión:** salen la fecha larga, la cabecera con su rótulo y el acceso a las novedades (la
  campana ya lo da). Las entregas con problema van en un aviso arriba, titulado «`<nombre>`,
  <cuánto>: con problema» (el grupo incluye las vencidas), con «Todavía sin fecha de nueva salida.»
  (HG-01) y el enlace a la página del problema. «Lo que viene» muestra solo en camino y pendientes.
- **Alternativas:** «no pudo llegar» como título (mentiría para una vencida); conservar la fecha.
- **Por qué:** la composición de A09, sin inventar lo que el grupo no distingue.
- **Costo:** las filas de «Lo que viene» y «Tus necesidades» son `div` sin enlace.
- **Quién decidió:** agente (lotes `org-inicio` y `org-entregas`), sin consultar.
- **Evidencia:** `74f441ee4` y `e7c9d1327`.

### 2026-11-23 · necesidades · Necesidades conserva el detalle dentro de la tarjeta plegable

- **Decisión:** cada necesidad es un `.plegable` con su detalle (por qué hace falta, entregas, quién
  donó, períodos, cambiar y dar de baja); abierta ocupa el ancho entero y la pedida por defecto queda
  abierta. Salen el registro de cifras y los canteros por categoría. La lámina protagonista va en
  una `.grilla` de 18 rem. «Nueva» y «Cambiada hoy» son un `strong.texto-chico` con
  `data-tono="acento"`; el cartel del cambio, un `.aviso` de información; «Ver los anteriores», un
  botón chico secundario.
- **Alternativas:** las tarjetas plegadas de A02 sin el detalle (no habría cómo cambiar ni dar de
  baja); `.estado` o `.chip` para «Nueva»; `.plegable-ver` para los anteriores (la flecha seguiría
  al plegable de afuera).
- **Por qué:** seguir A02 sin perder acciones y sin piezas nuevas.
- **Costo:** el rótulo «Ver el detalle» / «Cerrar» quedó en la hoja y fue bloqueante de la revisión
  ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 1).
- **Quién decidió:** agente (lote `org-necesidades`), sin consultar.
- **Evidencia:** `279ce7cef`, `5f713b7db` y `b99ddff71`.

### 2026-11-23 · necesidades · Los textos de Necesidades que fija un test o el dato de hoy

- **Decisión:** el botón sigue «Publicar otra necesidad» (lo busca un test de `visual/`); la cuenta
  sigue «10 de 40 kg recibidos» (HG-04) y la barra lleva el `aria-label` «Llegaron X de Y …»; hay
  dos pestañas, Vigentes y «Cumplidas y terminadas», porque la vista no distingue cumplidas de dadas
  de baja. Las chapas son solo las que piden atención («Espera tu confirmación», «Con un problema»).
- **Alternativas:** los textos y las tres pestañas de la guía.
- **Por qué:** no inventar un dato ni editar un test de `visual/`.
- **Costo:** dos textos distintos de la guía.
- **Quién decidió:** agente (lote `org-necesidades`), sin consultar.
- **Evidencia:** `279ce7cef`.

### 2026-11-23 · perfil · El perfil de la organización sigue el formulario de A12

- **Decisión:** «Mi perfil» como título, con el nombre de la organización debajo; una columna, sin
  lámina, con «Todos los datos son obligatorios.»; «guardando» con `aria-busy` en el botón. Hoja
  propia, `entidad-perfil.css`, que solo dispone. El error del servidor sigue como banner. Sin migas.
  Fuera de la maqueta, el aviso «Todavía no podemos guardar estos cambios desde acá…» no cambia.
- **Alternativas:** seguir cargando `donante-perfil.css` (es de otro rol); un formulario a todo el
  ancho; «Listo. Guardamos…» en el sitio real (sería falso: no se guarda nada).
- **Por qué:** la composición de A12 con las piezas del catálogo.
- **Costo:** el perfil de quien dona se ve distinto hasta su ola.
- **Quién decidió:** agente (lote `org-perfil`), sin consultar.
- **Evidencia:** `728e65683`, `206096090` y `9e409b266`.

### 2026-11-23 · entregas · Entregas no inventa fotos ni horas

- **Decisión:** una confirmada con foto va con `etiqueta-entrega`; sin foto, una `.tarjeta` con el
  mismo sello. En el sitio real las confirmadas no traen foto (pedirla sería una llamada por fila).
  La entrega con problema (A11) tiene 4 pasos sin horas y la necesidad como tarjeta con «Ver tu
  necesidad», sin barra (HG-04). El desplegable de Estado sigue el orden de las tarjetas y
  «Recibidas» pasa a «Confirmadas». Sin entrega elegida, la página del problema muestra la primera
  con problema; con una que no tiene problema, un vacío con «Volver a Entregas».
- **Alternativas:** escenas de ejemplo en el sitio real; los 5 pasos con horas de la guía; la foto
  por entrega (N+1).
- **Por qué:** el backend no entrega esos datos y la regla de la ola prohíbe inventarlos.
- **Costo:** las confirmadas reales salen sin imagen ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 4).
- **Quién decidió:** agente (lote `org-entregas`), sin consultar.
- **Evidencia:** `e7c9d1327` y `1520674a0`.

### 2026-11-23 · confirmar · Confirmar es A12 directo y el problema sigue en su página

- **Decisión:** sin el radio «¿Llegó bien?» ni el campo «Quién recibe»: «llegó con un problema» se
  abre por un enlace y anda sin JavaScript. Sin etiqueta de herbario; una línea suave «Para tu
  necesidad: …». El placeholder de «Cuánto llegó» sale (parece un valor cargado).
- **Alternativas:** el formulario de la guía con el radio y el nombre.
- **Por qué:** el backend no guarda quién recibe (HG-03) y el problema necesita su formulario.
- **Costo:** una pantalla más que la guía para el problema.
- **Quién decidió:** agente (lote `org-confirmar`), sin consultar.
- **Evidencia:** `ea633995f` y `29123f4da`.

### 2026-11-23 · confirmar · Lo que dice la recepción confirmada

- **Decisión:** el pie dice «Recibió: <la organización>, el <día>.» (HG-03); quién donó va en la
  frase, y si ocultó el nombre, «Donante <categoría>» o «un donante». La foto se muestra si es una
  dirección; la de la maqueta es una escena con el rótulo «Foto de ejemplo». Los pasos llevan su
  fecha cuando el historial la tiene, y sin el dato van sin fecha. La raíz se traza una vez, al
  llegar desde el acta.
- **Alternativas:** la fecha de la guía escrita en la plantilla; caer en la fecha de registro.
- **Por qué:** mostrar solo lo que quedó asentado.
- **Costo:** el problema sale de la dirección y «Llegó a tu puerta» puede caer en la fecha de
  registro: bloqueantes de la ronda 3 ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 2).
- **Quién decidió:** agente (lote `org-confirmar`), sin consultar.
- **Evidencia:** `ea633995f`, `3eca434eb` y `35af5ec8e`.

### 2026-11-23 · confirmar · Los frutos de la confirmada cuentan las recibidas del mes

- **Decisión:** la cifra sale de las recibidas del mes, con el mismo criterio que Inicio, y suma la
  recién confirmada si Donaciones todavía no la tiene; el árbol dibuja hasta 5.
- **Alternativas:** el conteo exacto del mes de Donaciones (la maqueta no lo tiene y pedía un
  método nuevo en el puerto).
- **Por qué:** sin sumarla, el fruto que brota no se ve.
- **Costo:** el cálculo quedó en el controller: bloqueante de la ronda 3
  ([DTI-48](../adr/DEUDA_TECNICA.md#dti-48), ítem 2).
- **Quién decidió:** agente (lote `org-confirmar`), sin consultar.
- **Evidencia:** `29123f4da`.

### 2026-11-23 · buzón · El buzón de los tres roles toma la fila de la campana de la guía

- **Decisión:** la ficha [`guia-org-buzon.md`](flujos/guia-org-buzon.md): «Novedades» como título
  en los tres roles; sin leer, con el punto lleno y la negrita, **sin fondo** (revierte el fondo
  suave que aprobó R11-14); y el buzón del admin cambia en la ola ORG junto con los otros dos.
- **Alternativas:** dejar los títulos de hoy; conservar el fondo suave; una variante del admin hasta
  ADM2.
- **Por qué:** la campana y el buzón muestran la misma señal; en la guía, el salvia es de pasar el
  mouse o del foco, no de un estado; y el buzón es una sola pieza.
- **Costo:** sin fondo, una fila sin leer se distingue menos de lejos.
- **Quién decidió:** usuario («ok» a la ficha con las tres recomendadas, 2026-11-23). Cumplida
  (DA-08): PASS de la revisión de `org-buzon`, ronda 1.
- **Evidencia:** la ficha, §9; backlog §11.

### 2026-11-23 · proceso · G1: las cuatro áreas se pueden mostrar así, como excepción a la regla del piso

- **Decisión:** las cuatro áreas pueden entrar a medias el 30/11, aunque 11 pantallas sin rehacer
  fallan el piso por toque.
- **Alternativas:** aplicar la regla del plan (§10) y dejar las cuatro áreas en «no se puede mostrar
  así», con todas sus olas necesarias para el go.
- **Por qué:** las 11 fallan solo por toque, no por contraste; el usuario conoció la regla y mantuvo
  su respuesta.
- **Costo:** si una ola no llega, su área se muestra con esas fallas de toque.
- **Quién decidió:** usuario («Las 4 se muestran» y, al conocer la regla, «Mantener, excepción»).
- **Evidencia:** backlog §2.2 (fila G1), §10.3 a §10.5; la hoja en `guia-c1/G1/hoja-g1.html`, fuera
  del repo.

### 2026-11-23 · proceso · Ronda 4: lo que necesitan los manifiestos de rol (DA-03b, DA-03c, DA-04, DA-05, DA-06, DA-08, DA-13 y DA-21)

- **Decisión:** el admin entra por «Por decidir», que reemplaza a Inicio y a Asignar, y los
  gráficos de hoy van a una ficha de ADM1 (DA-04). La persona suma Misiones como cuarta entrada de
  su menú (DA-03b) y su inicio deriva del de quien dona, con la tarjeta de la huerta (DA-03c).
  «Cómo funciona», «Cómo donar» y «Ayuda» son Nosotros, Para donantes y Para organizaciones con
  otro nombre (DA-05). Solo confirmar una recepción tiene página de respuesta; el resto, aviso
  breve (DA-06). Llevan ficha Misiones, Ranking, el inicio de la persona y el buzón; lo demás sin
  arquetipo se deriva (DA-08). Las 6 plantillas de `persona/` se migran con su gemela (DA-13). La
  casilla del buzón pasa el toque midiendo 48 px de verdad, con el patrón de C1-12 (DA-21).
- **Alternativas:** las de cada fila del backlog §2.2. Para la casilla: extender la excepción de
  DA-16 a las rutas del buzón, o que el test mida la etiqueta.
- **Por qué:** cada una usa lo que ya existe (páginas, plantillas, el patrón de la opción de C1) y
  no suma pantallas ni cambia tests antes del 30/11.
- **Costo:** cuatro fichas para escribir en sesión (DA-19) y la persona con una entrada de menú más
  que la guía.
- **Quién decidió:** usuario («Todas las recomendadas», ronda 4 del 2026-11-23).
- **Evidencia:** backlog §2.2.

### 2026-11-23 · proceso · Los menús de cada rol no cambian en C1 (DA-17; DA-01, DA-02 y DA-03a diferidas)

- **Decisión:** C1 no cambia ninguna entrada de ningún menú. El menú de cada rol cambia en la ola de
  ese rol: el público en PUB1 (DA-01, el Mapa pasa a Entregas), el de la organización en ORG (DA-02,
  «Donaciones recibidas» y «Seguir entregas» se funden en «Entregas») y el de quien dona en DON
  (DA-03a, «Seguimiento» sale del menú y se abre desde el detalle).
- **Alternativas:** cambiar los cuatro menús junto con el armazón, en C1.
- **Por qué:** una entrada nueva sin su pantalla rehecha lleva a una pantalla vieja; cada ola rehace
  el menú junto con las pantallas a las que lleva.
- **Costo:** hasta la ola de cada rol, el armazón nuevo convive con las entradas de hoy.
- **Quién decidió:** usuario (DA-17, ronda 3 del 2026-11-22). Cumplida: PASS de la revisión de
  `c1-armazon`, ronda 1.
- **Evidencia:** backlog §2.2 y §10.2; `41bc195b8`.

### 2026-11-23 · proceso · Las piezas del galpón no entran en C1 (DA-18)

- **Decisión:** la tabla con selección, el cajón y el armazón del galpón pasan a la ola ADM1.
- **Alternativas:** traerlas al catálogo en C1 junto con las demás piezas.
- **Por qué:** solo las usa el admin, y entran con sus pantallas.
- **Costo:** el admin sigue con el armazón de la huerta hasta ADM1. También queda para ADM1 la
  casilla sola de 48 × 48 px.
- **Quién decidió:** usuario (DA-18, ronda 3 del 2026-11-22).
- **Evidencia:** backlog §2.2 y §10.2.

### 2026-11-23 · sello · «Vencida», «cancelada» y «en revisión» van con el sello neutro y el signo de información (DA-07)

- **Decisión:** los tres estados que la guía no dibuja usan el par de «pendiente» y su palabra
  (DA-07). Como signo, el lote eligió `e-info`, el mismo para los tres.
- **Alternativas:** para el signo, `e-registrada` (se confunde con «registrada», que usa el mismo
  par), `e-atencion` y `e-problema` (alarman, y DA-07 pide algo neutro) y `e-espera` (es el de la
  chapa de pedido).
- **Por qué:** neutro, sin alarma y sin confundirse con otro estado.
- **Costo:** `/admin-donaciones` sigue mapeando «vencida» a `problema` hasta ADM1, porque su plantilla
  no es de la ola.
- **Quién decidió:** usuario (DA-07); el signo, agente (lote `c1-estados`), sin consultar.
  Cumplida: PASS de la revisión del lote, ronda 2.
- **Evidencia:** `ebb17dd0d` y `dfdb876d3`; `componentes/sello.css` y `sello.html`.

### 2026-11-23 · base · La base de la guía entra en una capa propia, con el aro de foco de un solo color

- **Decisión:** el reset de márgenes, las listas sin viñetas, los títulos, `a`, `strong`, `svg` y el
  foco van en `@layer base`; `body` sigue sin capa. La medida del texto se fija en `main :where(p)`.
  El foco conserva el selector genérico `:focus-visible`. `.dt`, `.es-foco` y `.es-cursor` de la
  maqueta no entran.
- **Alternativas:** dejar la base sin capa (les ganaba a `.pila > * + *` y a todos los bloques);
  restringir el foco a la lista de elementos de la guía (se perdía en otros).
- **Por qué:** que cualquier composición o componente le gane a la base sin pelear especificidad.
- **Costo:** en las pantallas que no son de la ola, las listas pierden viñetas, los párrafos quedan en
  34 rem, el texto pasa a `--color-texto` con interlineado 1,5 y algunas partes quedan más apretadas.
  Es un dato para G1.
- **Quién decidió:** agente (lote `c1-base`), sin consultar.
- **Evidencia:** `1447644f2`; `app.css`.

### 2026-11-23 · composición · Los nombres de la guía se traducen a las piezas que ya existen

- **Decisión:** `.fila` es `.racimo`, `.entre` es `data-alinea="entre"`, `.suave` es
  `data-tono="suave"`, `.chico` es `.texto-chico` y `.oculto-visual` es `.solo-lectores`. Lo que
  faltaba entra con la forma de hoy: `data-alinea` acepta varias palabras (`"entre arriba"`) y
  `data-espacio="0"`. `.pila` sigue con margen y no pasa a `gap`. No entran `.medida`, `.cifra`,
  `.enlace-suelto` ni `.sobre-suelo` (es `.raices-pie`).
- **Alternativas:** copiar las clases de la guía; reescribir `.pila` con `gap`.
- **Por qué:** la regla de la ola: los nombres que existen se conservan. Reescribir `.pila` podía
  mover pantallas que no son de la ola.
- **Costo:** quien lee la guía tiene que traducir los nombres. `.enlace-suelto` queda sin pieza.
- **Quién decidió:** agente (lote `c1-base`), sin consultar.
- **Evidencia:** `1447644f2`; `composicion.css` y `componentes.md`.

### 2026-11-23 · movimiento · Las animaciones nuevas reusan las de hoy y sus duraciones van al token más cercano

- **Decisión:** el trazo y el asomo de la guía son `mov-traza` y `mov-aparece`, que ya existían. Son
  nuevas solo `mov-gira`, `mov-late` y `mov-brote` (`--movimiento-animacion-brote`, porque `-brota` ya
  es otra). El giro de 0,9 s va con `--movimiento-duracion-larga` (0,75 s) y el latido de 1,4 s con
  `--movimiento-duracion-trazo` (1,1 s). `--movimiento-animacion-traza-raiz` toma la curva de la
  guía.
- **Alternativas:** duraciones literales, que la regla de la ola no permite; un token nuevo en
  `tokens.css`, que no se toca.
- **Por qué:** una duración va siempre a un token.
- **Costo:** el giro y el latido son algo más rápidos que en la guía; la diferencia está anotada en
  `movimiento.css` y en el catálogo.
- **Quién decidió:** agente (lote `c1-base`), sin consultar.
- **Evidencia:** `1447644f2`.

### 2026-11-23 · tokens · Las fichas de tokens de /estilos pasan al fragmento de muestra

- **Decisión:** las 14 fichas usan `componentes/muestra :: muestra`; el relleno pasa a
  `.muestra-demo`. Se suma una sección «Base» con tres fichas (títulos y texto, foco y listas).
- **Alternativas:** dejarlas a mano (el bloqueante de C0); un parámetro nuevo del fragmento para los
  títulos en prosa.
- **Por qué:** lo que existe se usa como está, y la pieza nueva de la base necesita su muestra.
- **Costo:** los títulos en prosa («Letra», «Color: superficies y texto») salen en monoespaciada,
  porque el fragmento envuelve el nombre en `<code>`.
- **Quién decidió:** agente (lote `c1-base`), sin consultar.
- **Evidencia:** `1447644f2`; [DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítem 1.

### 2026-11-23 · símbolos · El ícono es `.icono` y el signo de estado, una variante por atributo

- **Decisión:** la hoja nueva es `icono.css`, con `.icono`; el `.signo` de la guía es
  `.icono[data-forma="signo"]`, con puntas rectas. Íconos y signos van en dos fragmentos nuevos de
  `iconos.html`, `comunes` y `signos`, y no en `navegacion`. Las medidas salen de tokens exactos.
- **Alternativas:** una segunda clase `.signo` (otra hoja, fuera del patrón de variantes por
  atributo); sumar los símbolos a `navegacion`, que cargan los dos layouts.
- **Por qué:** no agrandar todas las páginas y seguir el catálogo.
- **Costo:** ningún layout carga todavía esos fragmentos: los incluye la primera ola que los use en
  una pantalla.
- **Quién decidió:** agente (lote `c1-simbolos`), sin consultar.
- **Evidencia:** `cb50bb372` y `b5cef7d93`.

### 2026-11-23 · símbolos · La marca de tres colores se resuelve con un hueco en el cuerpo

- **Decisión:** en `i-marca`, el cuerpo toma `currentColor`, la hoja hereda `--color-hoja` y la hoja
  clara es un hueco (`fill-rule="evenodd"`).
- **Alternativas:** colores en hex en la plantilla; la marca de un solo color.
- **Por qué:** ni los atributos SVG ni una plantilla sin `style` aceptan `var()`.
- **Costo:** sobre un fondo de color, la hoja clara toma ese color en lugar del papel.
- **Quién decidió:** agente (lote `c1-simbolos`), sin consultar.
- **Evidencia:** `cb50bb372`; `iconos.html`.

### 2026-11-23 · ilustraciones · Los dibujos de la guía entran como los de hoy, con los colores en hex

- **Decisión:** el recorrido, la puerta, el mapa y las 7 escenas van en `static/img/ilustraciones/`.
  Cada clase `f-*` y `s-*` de la guía pasa al hex de su token, y cada archivo trae el símbolo y lo
  dibuja con `<use>`, para servir como `<img>` y como `<use href>`. De la puerta entran solo los
  símbolos fijos; las partes que cambian con el estado las dibuja la pantalla.
- **Alternativas:** clases de color nuevas; ponerlos sueltos en `static/img/`.
- **Por qué:** el producto no tiene las clases de la maqueta, y así no se mezclan con otros recursos.
- **Costo:** los dibujos no siguen un cambio de tema; los números piden «DM Sans» por nombre.
- **Quién decidió:** agente (lote `c1-simbolos`), sin consultar.
- **Evidencia:** `cb50bb372`.

### 2026-11-23 · armazón · El menú de abajo llega hasta 56 em en todos los roles

- **Decisión:** el menú pasa a la barra de arriba recién desde `--medida-quiebre-menu` (56 em), como
  la guía, también en las áreas privadas (antes, desde 37,5 em). La barra de arriba va en una sola
  fila, y `--medida-cabecera-fija` tiene un solo valor (4,125 rem). Con 5 secciones, la letra se
  estrecha en el celular y se aprieta entre 56 y 68,75 em.
- **Alternativas:** conservar el corte de 37,5 em en las privadas.
- **Por qué:** manda la guía.
- **Costo:** entre 600 y 896 px las privadas muestran el menú abajo. A 320 px, con 5 secciones, dos
  nombres bajan de renglón (75 px de alto, sin desborde).
- **Quién decidió:** agente (lote `c1-armazon`), sin consultar.
- **Evidencia:** `41bc195b8` y `00e6aed69`; `app.css` y `componentes/navegacion.css`.

### 2026-11-23 · armazón · En el celular la barra de arriba no queda fija, aunque la guía la pega

- **Decisión:** se mantiene R11-33: en el celular arriba no queda nada fijo.
- **Alternativas:** pegar la barra arriba en todo ancho, como la guía (`10-componentes.css:318`).
- **Por qué:** rige la decisión R11-33 y `ArmazonFijoTest` lo afirma; esta ola no edita los tests de
  `visual/`.
- **Costo:** una diferencia con la guía. Queda pendiente para el usuario
  ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)).
- **Quién decidió:** agente (lote `c1-armazon`), sin consultar; la decisión de fondo es del usuario.
- **Evidencia:** `app.css` (bloque de lo fijo).

### 2026-11-23 · armazón · El pie es el corte de suelo de la guía, sin el lema

- **Decisión:** el pie pierde el lema «Lo que donás echa raíces.» y los fondos de raíces y textura.
  La marca toma la letra de la guía y conserva el `img` de hoy. Las reglas de la campana pasan a
  `componentes/campanita.css`, y el menú de la cuenta y el de la campana llevan el borde de tinta.
- **Alternativas:** conservar el lema y los fondos.
- **Por qué:** la guía no los dibuja.
- **Costo:** sale un texto que estaba en el pie desde la identidad Raíces. Ahora
  `AbejasYRenglonesTest.loQueDonasYEchaRaicesVanEnterosEnSuRenglonEnTodosLosAnchos` da error porque
  mide ese lema: el usuario decide si se conserva o si se quita esa medición
  ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)). `img/raices-pie.svg` queda sin uso.
- **Quién decidió:** agente (lote `c1-armazon`), sin consultar.
- **Evidencia:** `41bc195b8`; `pie.html` y `app.css`.

### 2026-11-23 · armazón · Las migas dibujan su flecha en línea

- **Decisión:** `componentes/migas.css` y su fragmento dibujan la flecha con el trazo en línea, no
  con `use href="#i-ir"`. El enlace lleva un ancho mínimo de 48 px.
- **Alternativas:** usar el símbolo `i-ir`, que obliga a la página a cargar `iconos :: comunes`.
- **Por qué:** que el fragmento ande en cualquier página y que un texto corto llegue al toque mínimo.
- **Costo:** la flecha está dibujada dos veces.
- **Quién decidió:** agente (lote `c1-armazon`), sin consultar.
- **Evidencia:** `41bc195b8`.

### 2026-11-23 · formularios · La opción y la casilla dibujan su marca y dejan el control nativo encima, transparente

- **Decisión:** la opción y la casilla son `.opciones`, con su variante de casilla. El control nativo
  queda transparente sobre todo el bloque, para que mida 48 px o más, y la marca de 22 px es un
  `::before`. `.opciones` queda en una columna.
- **Alternativas:** mostrar el control nativo de 22 px, como la guía (no llega al toque mínimo);
  encimarlo solo en el relleno (46 px, 44 con error); las clases `.opcion` y `.casilla` de la guía
  (`.casilla` ya es la casilla de los términos del registro); dos columnas desde 37,5 em (los
  párrafos de nota ocupaban una celda).
- **Por qué:** el piso de toque de 48 px no se negocia y no se toca el test.
- **Costo:** la casilla de los términos del registro conserva el dibujo de antes y se ve distinta.
- **Quién decidió:** agente (lote `c1-formularios`), sin consultar.
- **Evidencia:** `6051f2a1a` y `a5d384c7a`.

### 2026-11-23 · formularios · El botón trae su estado «cargando» y no las variantes que nadie usa

- **Decisión:** el botón con `aria-busy` muestra la ruedita (`--movimiento-animacion-gira`, de
  `--espacio-3`). Deshabilitado les gana a todos los tonos, también con el cursor encima. El botón
  con foto y el botón chico pasan a tokens. No entran las variantes de texto e ícono.
- **Alternativas:** traer las variantes de la guía.
- **Por qué:** ninguna plantilla las usa.
- **Costo:** las hojas de pantalla que pisan `.boton` pueden verse distintas del botón nuevo (dato para
  G1).
- **Quién decidió:** agente (lote `c1-formularios`), sin consultar.
- **Evidencia:** `6051f2a1a`.

### 2026-11-23 · formularios · El error de campo lleva el signo de la guía, y sin signo sigue siendo un párrafo

- **Decisión:** el error va en fila solo si lleva el signo (`.campo-error:has(> svg)`), de 1 rem
  (`--espacio-3`) y con `--espacio-0` hacia abajo. Las cuatro muestras de error de `/estilos` lo
  llevan.
- **Alternativas:** el margen de `--espacio-1` que proponía la revisión; un error siempre en fila.
- **Por qué:** `--espacio-0` es el token más cercano a los 0,3125 rem de la guía, y un error sin signo
  no debe partir su texto.
- **Costo:** el error que arma `publico.js` sigue sin signo.
- **Quién decidió:** agente (lote `c1-formularios`, rondas 1 y 2), sin consultar.
- **Evidencia:** `c7814ee54` y `7d4fcefc0`.

### 2026-11-23 · estados · El sello de la guía se ve en la chapa; suelto sigue el dibujo de hoy

- **Decisión:** `sello(estado)` dibuja las dos cosas: el signo de la guía, que se ve solo dentro de
  `span.estado`, y el dibujo de hoy, que se ve suelto o en `p.estado`. Los signos se dibujan en línea
  en `sello :: signo(nombre)`. El borde punteado va solo en «registrada». El sello deja partir el
  renglón (sin `nowrap`). La chapa de espera es `.sello-pedido`.
- **Alternativas:** cambiar el dibujo por el signo en todos lados (rediseñaba etapas y contadores de
  pantallas que no son de la ola); usar `use href="#e-…"` (las pantallas no cargan ese sprite).
- **Por qué:** la pieza nueva sin rehacer pantallas, y textos largos de sello que con `nowrap` se
  salían del borde.
- **Costo:** dos fuentes de verdad para la geometría de los signos (`iconos.html` y `sello.html`).
  Cada `span.estado` de las pantallas cambia de pastilla a chapa.
- **Quién decidió:** agente (lote `c1-estados`), sin consultar.
- **Evidencia:** `ebb17dd0d` y `dfdb876d3`.

### 2026-11-23 · avisos · El aviso en la página es una pieza nueva y el breve toma el papel de la guía

- **Decisión:** clase nueva `.aviso` con `data-tipo`; `.banner` no se convierte. El aviso breve de la
  guía se aplica a `.aviso-breve:not([data-pista])`, y «hecho» pinta el signo con el color del éxito,
  sin franja ni negrita. El ícono del aviso mide 1,25 rem con una regla propia.
- **Alternativas:** convertir `.banner`; sumar el tamaño a `.aviso-breve > .icono` (no le ganaba a
  `.icono[data-forma="signo"]`).
- **Por qué:** la pista conserva su papel, y la pieza nueva no rehace pantallas.
- **Costo:** los avisos breves reales todavía no traen el signo ni el botón de ícono.
- **Quién decidió:** agente (lote `c1-estados`; la regla del ícono, en la ronda 1), sin consultar.
- **Evidencia:** `ebb17dd0d` y `a976019af`.

### 2026-11-23 · entrega · La lámina pierde la sombra granulada y sus variantes van por `data-papel`

- **Decisión:** `.tarjeta` lleva el filete fino de la guía y `isolation: isolate`. Sus variantes son
  `data-papel` (protagonista, hueco, tierra). `data-filete="contorno"` pasa a ser el filete en tinta;
  el hueco usa `background-color: transparent`, para no borrar la trama.
- **Alternativas:** las clases `lamina--*` (heredadas); `data-tono` (ya es una utilidad de texto);
  conservar el `outline` de contorno (dibujaba dos líneas); subir la especificidad de la trama.
- **Por qué:** seguir la guía sin chocar con nombres que ya existen.
- **Costo:** las pantallas que usan `.tarjeta` pierden la sombra granulada; `grano-sombra.svg` y
  `--color-sombra-grano` quedan sin uso. Con `tierra`, el atajo `background` todavía borra la trama.
- **Quién decidió:** agente (lote `c1-entrega`; el hueco, en la ronda 1), sin consultar.
- **Evidencia:** `4c6d2a36c`, `0e574abf8` y `0f68f2e14`.

### 2026-11-23 · entrega · La etiqueta de la entrega se apila sola, sin media query

- **Decisión:** `etiqueta-entrega`, con el nombre de su hoja como prefijo. Con la foto al costado, se
  apila sola por debajo de unos 21,8 rem (`flex-wrap`) y el filete entre foto y texto es una sombra.
  El sello es siempre el de entregada; el título, un `h3`. Las medidas de la guía pasan a tokens: el
  hueco del agujero a `--espacio-5` (guía, 2 rem) y el rótulo a `--espacio-0` (guía, 0,375 rem).
- **Alternativas:** el quiebre de 37,5 em (apilaba la etiqueta en celulares de 390 a 600 px, donde la
  guía la muestra al costado); `.etiqueta`, `.foto` y `.foto-falta` (son clases heredadas).
- **Por qué:** el `@container` de la guía no tiene un quiebre equivalente.
- **Costo:** las variantes de espera y de fila quedan para ORG. En `/estilos` la foto al costado se
  ve cortada en la captura de 1280 px.
- **Quién decidió:** agente (lote `c1-entrega`), sin consultar.
- **Evidencia:** `4c6d2a36c`.

### 2026-11-23 · raíz · La raíz de los 5 pasos y los frutos son fragmentos nuevos

- **Decisión:** `huerta :: raiz-pasos(pasos, forma, nombre, recien)` y
  `huerta :: frutos(cuenta, periodo, recien)`, con las clases `.huerta-raiz-*` y `.huerta-frutos*`.
  Las formas van por `data-forma` (acostada, numerada, numerada tendida); la tendida, desde 56 em. Con
  `recien`, el último tramo es una raya SVG que se dibuja con la animación de hoy. Las medidas salen
  de `--espacio-*`.
- **Alternativas:** rehacer `recorrido(d)` o `cifras(estado, valores)`; los nombres `raiz`,
  `raiz-paso` y `fruto` (heredados); `mov-recorta` para el trazo; omitir el trazo.
- **Por qué:** `cifras` cuenta números grandes y sus pantallas no son de la ola. `mov-traza` anima un
  trazo SVG y la raíz de la guía usa bordes.
- **Costo:** los frutos se separan 0,25 rem (guía, 0,3125). La tendida con un paso que falta dibuja
  una línea punteada. La puerta y `.estado-raiz` quedan para ORG, y ninguna pantalla usa todavía
  estas piezas.
- **Quién decidió:** agente (lote `c1-raiz`), sin consultar.
- **Evidencia:** `10f3d3b64` y `bd1c8b233`.

### 2026-11-23 · listas · La lista de filas es una variante, y la vacía y la que carga pierden sus abejas

- **Decisión:** la lista de la guía entra como `.lista[data-forma="filas"]`. La caja de la lista vacía
  se pinta desde `lista.css`. Salen la abeja dormida de `.vacio`, la abeja que vuela en `.cargando` y
  la ola del esqueleto, que pasa a latir; con tres renglones, son los de la guía. La caja de carga va
  en `[aria-busy="true"]:has(> .esqueleto > i)`.
- **Alternativas:** cambiar la `.lista` base (unas 27 plantillas la usan como grilla); la clase
  `lista-fila` de la guía; editar `aviso.css`, de otro lote.
- **Por qué:** manda la guía, que no dibuja las abejas.
- **Costo:** pisa la ronda de ilustraciones del 2026-11-08 y la ola del esqueleto, que había decidido
  el usuario el 2026-11-12. Quedan reglas muertas en `aviso.css`. La lista con error sigue con el
  `.banner` y la vacía, sin la trama (bloqueantes abiertos, [DTI-47](../adr/DEUDA_TECNICA.md#dti-47)).
- **Quién decidió:** agente (lote `c1-listas`), sin consultar.
- **Evidencia:** `c919e6848`, `6456a75fc` y `d641c4b60`.

### 2026-11-23 · listas · La tabla se apila por debajo de 56 em con el rótulo en el margen de la celda

- **Decisión:** el corte pasa de 53,75 a 56 em. Apilada, el rótulo flota en el margen de la celda
  (`float` con margen negativo), y la celda de acciones ocupa todo el ancho. Desde 56 em, la cabecera
  usa `--color-galpon-cabecera` y la fila bajo el cursor, `--color-fila-cursor`.
- **Alternativas:** la grilla de dos columnas de la guía (partía las celdas que mezclan texto, `<br>`
  y botones); el rótulo en `::before` absoluto (la primera versión); un `min-height`; `.tabla-caja`
  (pide un envoltorio en las plantillas).
- **Por qué:** las celdas reales del admin mezclan contenidos.
- **Costo:** las tablas del admin, que no son de la ola, pasan de tarjetas a filas entre filetes.
- **Quién decidió:** agente (lote `c1-listas`, y la ronda 1), sin consultar.
- **Evidencia:** `c919e6848`, `2c97c0b57` y `6456a75fc`.

### 2026-11-23 · listas · Las pestañas se pueden desplazar de costado

- **Decisión:** desde 37,5 em, el filete es una sombra hacia adentro de la lista, que se desplaza de
  costado (`overflow-x: auto`). Se conserva la animación de impresión de la elegida.
- **Alternativas:** el borde con margen de -1,5 px de la guía (recortaba la solapa elegida al
  desplazar).
- **Por qué:** en columnas angostas las pestañas se salían.
- **Costo:** en la ficha de `/estilos` a 1280 px la tercera pestaña queda escondida, sin barra a la
  vista: bloqueante abierto ([DTI-47](../adr/DEUDA_TECNICA.md#dti-47)).
- **Quién decidió:** agente (lote `c1-listas`), sin consultar.
- **Evidencia:** `c919e6848`; `pestanas.css`.

### 2026-11-22 · proceso · Las casillas de la demo de la bandeja en /estilos son una excepción conocida del gate visual

- **Decisión:** `VerificacionVisualTest` deja afuera del piso las fallas de toque de las casillas
  de 12 × 12 px de la demo de la bandeja en `/estilos` (`EXCEPCIONES_CONOCIDAS`: ruta, tipo y detalle
  exactos). No cuentan en `reporte.json` y quedan a la vista en `excepciones.json` (`e7ba71c36`).
- **Alternativas:** que la medición de toque cuente cada casilla por su etiqueta de 48 px (también
  limpiaba las tres pantallas de notificaciones); dejar el gate de `/estilos` en rojo hasta que el
  usuario decida.
- **Por qué:** la casilla chica es decisión del usuario del 2026-11-12 (`buzon.css`) y la falla ya
  estaba antes de la oleada C0 (igual en `093aed884`).
- **Costo:** `/admin-notificaciones`, `/donante-notificaciones` y `/entidad-notificaciones` siguen
  fallando por la misma casilla en la corrida completa. El ADR de la verificación visual
  (`20261030`) no prevé esta excepción ([DTI-46](../adr/DEUDA_TECNICA.md#dti-46), ítem 2).
- **Quién decidió:** agente (cierre de C0), sin consultar. El agente la tomó como la respuesta
  «Opción B» del usuario, pero esa respuesta es la de la corrección de DA-14a (backlog §2.2, escrita
  en `8a2b0e646`, antes del cierre), que lanzó la segunda corrida. La excepción no tiene decisión del
  usuario: él decide si queda, se revierte o se cambia la medición. **Actualización (2026-11-22):**
  el usuario la revisó y decidió mantenerla («Mantenerla», DA-16 del backlog). Queda en el
  [ADR de la excepción de toque](../adr/cliente-liviano/20261122-excepcion-de-toque-para-la-casilla-del-buzon-en-estilos.md)
  (`proposed`).
- **Evidencia:** `guia-r2/C0/cierre.md`, sección 6, fuera del repo.

### 2026-11-22 · tokens · En /estilos, las listas de alias y valores ocupan la fila entera y cada nombre va en un renglón

- **Decisión:** las listas `data-tipo="valor"` de `.muestra-tokens` van en una columna, con cada `li`
  en bloque y su `code` sin cortes, y su ficha ocupa la fila entera de la grilla (también
  «Movimiento»). En el celular usan `--tamano-micro` y relleno `--espacio-1`, y vuelven a
  `--tamano-chico` y `--espacio-2` desde 37,5 em. En Movimiento, número y unidad van con `&nbsp;`.
- **Alternativas:** `overflow-wrap: anywhere` (partía cada nombre); dejar la ficha angosta (un alias
  de unos 55 caracteres no entra en 16 rem); bajar el nombre de la guía a su renglón; achicar el
  relleno de toda la ficha `.muestra`.
- **Por qué:** la revisión de la primera corrida pidió que cada alias se lea entero.
- **Costo:** otra letra para la misma pieza en el celular; huecos en la grilla a 1280 y 1920 px.
- **Quién decidió:** agente (lote `c0-tokens`).
- **Evidencia:** `ebce607ad` y `7d413d1b2`; captura `estilos-360` de la corrida del cierre.

### 2026-11-22 · tokens · La grilla de las fichas de token pasa a columnas de 22 rem

- **Decisión:** en la sección Tokens de `/estilos`, la grilla de fichas usa `--medida-grilla: 22rem`
  (con `:has`, solo en esa sección).
- **Alternativas:** las columnas de antes (los nombres de color se partían en 2 o 3 renglones a
  1280 px); columnas de 23 rem (dejaban solo 2 columnas).
- **Por qué:** el mismo defecto de la tarjeta de alias, en las fichas de color.
- **Costo:** `22rem` es una medida literal que el cerco no cuenta; a 1280 px
  `--color-accion-secundaria-activa` todavía baja de renglón en un guion.
- **Quién decidió:** agente (lote `c0-tokens`), por su cuenta, más allá de lo que pidió la revisión.
- **Evidencia:** `ebce607ad`, `componentes/muestra.css`.

### 2026-11-22 · tokens · Salen también tres valores base sin uso

- **Decisión:** además de los 3 tokens de DA-14a, salen `--p-nodo`, `--p-destacado` y
  `--p-suelo-hondo`, que solo alimentaban a tokens retirados o pasados a alias.
- **Alternativas:** dejarlos (valores base sin ninguna referencia).
- **Por qué:** no son tokens de uso; G-01 y el conteo de 73 conservados no cambian, ni ningún color
  calculado.
- **Costo:** DA-14a no los nombra.
- **Quién decidió:** agente (lote `c0-tokens`, primera corrida).
- **Evidencia:** `9e995ec42`, `tokens.css`.

### 2026-11-22 · tokens · Se retiran 3 tokens y no 4: `--medida-quiebre-medio` se conserva (DA-14a, corregida)

- **Decisión:** los 10 nombres de color de hoy que la guía no trae pasan a ser alias del token de la
  guía del mismo valor; se retiran `--color-nodo`, `--color-estrato` y `--medida-estrato`;
  `--medida-quiebre-medio` queda; los otros 30 de los 44 no se tocan en C0.
- **Alternativas:** retirar los 4 que pedía la primera versión de DA-14a.
- **Por qué:** 39 media queries usan el valor de `--medida-quiebre-medio` (53,75 em) y
  `InterfazFitnessTest` exige que cada media query use un quiebre declarado en `tokens.css`. La
  primera corrida lo encontró y se paró.
- **Costo:** queda un quiebre que la guía no trae.
- **Quién decidió:** usuario (DA-14a: «todas las recomendadas», ronda 1; corregida con «Opción B»).
- **Evidencia:** backlog §2.2 y §3.2; `9e995ec42`.

### 2026-11-22 · tokens · Los tokens de la guía se suman sin renombrar ninguno de antes (G-01)

- **Decisión:** `tokens.css` suma los 46 tokens nuevos de «Huerta y galpón» y toma el valor de la
  guía en los 6 que cambian; ningún token que ya existía cambia de nombre. La guía mide en `cqi` y el
  sitio en `vw`.
- **Alternativas:** renombrar los tokens de hoy a los de la guía en todas las hojas.
- **Por qué:** cambiar el sistema sin tocar las pantallas que no son de la ola.
- **Costo:** conviven dos nombres para un mismo color hasta la ola Z (DA-14b).
- **Quién decidió:** usuario (G-01, cierre de la fase 3 de la guía).
- **Evidencia:** `9e995ec42`; `docs/interfaz/componentes.md`, sección Tokens.

### 2026-11-22 · identidad · La identidad pasa a «Huerta y galpón», con un esqueleto por rol (G-02)

- **Decisión:** la guía de `docs/interfaz/guia/` es el sistema visual: un esqueleto aireado para lo
  público, quien dona y la organización, y uno denso para el admin (mezcla C). `direccion-de-arte.md`
  deja de nombrar a Pico como base.
- **Alternativas:** Poda, Acta, Recorrido y Remito solas; las mezclas A (Cuaderno) y B (Herbario).
- **Por qué:** de las tres mezclas, la que más calidez pone.
- **Costo:** la piel Huerta de antes queda reemplazada en parte; la relación `superseded` espera la
  aprobación del grupo.
- **Quién decidió:** usuario (G-02, 2026-11-20).
- **Evidencia:** [ADR `proposed`](../adr/cliente-liviano/20261122-huerta-y-galpon-la-guia-como-sistema-visual-y-un-esqueleto-por-rol.md); `34cf87ae7`.

### 2026-11-15 · proceso · Las oleadas de interfaz rinden cada decisión del usuario y no abren PR sin la revisión de pantallas (paso 4 de la opción A)

- **Decisión:** el workflow `oleada-ui` reemplaza a `backlog-r11`. Arranca solo si cada decisión
  de la ronda con el usuario tiene ID y va a un lote, con un criterio que el revisor comprueba, o
  queda diferida con quién la difirió y dónde consta el OK. Las vistas las construye Opus high;
  `revisar-ui` es bloqueante; antes del PR, un revisor independiente mira todas las pantallas a 360
  y 1280 px, y el PR trae la hoja de contacto con una casilla que marca el usuario.
- **Alternativas:** un workflow por backlog, como R11; abrir el PR con los bloqueantes como
  pendientes; armar la hoja con `lado` (deforma la de 1280 junto a la de 360).
- **Por qué:** en R11, la barra común de filtros (R11-B04) se difirió sin que nadie lo marcara, y
  ningún paso comparaba pantallas entre sí.
- **Costo:** el workflow no se corrió con agentes reales; las rutas viejas de los registros de R11
  quedan sin tocar ([DTI-44](../adr/DEUDA_TECNICA.md#dti-44), ítem 16).
- **Quién decidió:** usuario (2026-11-15, plan del paso 4: sucesor genérico, `backlog-r11` al
  archivo y el manifiesto de R11 como registro).
- **Evidencia:** [spec del paso 4](../specs/active/harness-ui-paso-4-workflow-spec.md) y
  [guía](../IA/prompts/orquestacion-oleada-ui.md).

### 2026-11-15 · proceso · El harness de interfaz pasa a tres skills cortas sobre el sistema cerrado (paso 3 de la opción A)

- **Decisión:** `construir-ui` (producir), `revisar-ui` (cerrar, en solo lectura, con las pantallas
  comparadas entre sí y todo desvío del sistema como bloqueante) y `explorar-ui` (opciones que
  divergen en para qué sirve la pantalla, metáfora y densidad, sin ranking y sin mergear). La regla
  baja a 10 líneas y `cliente-liviano/AGENTS.md`, a 80. Las 7 skills anteriores, la corrida creativa
  y sus plantillas van al [archivo](../IA/history/harness-ui-2026-11/README.md).
- **Alternativas:** archivar en `docs/interfaz/archivo/` o borrar; mover también la bitácora, la
  evidencia y las fichas cerradas; las dos skills del borrador de la línea base (iii).
- **Por qué:** el diagnóstico encontró mucho texto para leer y poca restricción verificable. La
  lectura obligatoria baja de unos 206 KB a unos 24 KB, y los topes los controla `agent-check`. Lo
  histórico no se mueve porque lo enlazan ADRs que no se editan.
- **Costo:** quedan rotos los enlaces de los registros a las skills archivadas
  ([DTI-44](../adr/DEUDA_TECNICA.md#dti-44), ítem 15). El panel de críticos y el A/B de un solo tema
  no tienen reemplazo: elige el humano mirando.
- **Quién decidió:** usuario (2026-11-14, opción A; 2026-11-15, archivo, histórico sin mover y
  plan de los pasos 3 y 4).
- **Evidencia:** [ADR `proposed`](../adr/cliente-liviano/20261115-harness-de-interfaz-con-sistema-cerrado-y-tres-skills-cortas.md)
  y [spec del paso 3](../specs/active/harness-ui-paso-3-skills-spec.md).

### 2026-11-15 · componente:listado · Toda lista con búsqueda o filtros usa la misma barra (kit de la opción A)

- **Decisión:** las 9 listas que seguían con pastillas o con `form.filtro` pasan a `componentes/listado`:
  Organizaciones, Camiones, Registrar donación, Actividad, Importaciones y Donantes del admin, y
  Entidades, Entregas confirmadas y el Mapa públicos. También pasan Propuestas y Destino de
  Asignaciones. Un nombre por campo: `q` pasa a `buscar`, `ver` a `estado` y `pedidos` a `pedido`,
  y los nombres viejos siguen valiendo como alias. «Quién» de Actividad queda como `autor`: es un
  desplegable de cuentas, no el texto de la contraparte (DTI-44, ítem 13). En el celular el botón
  que pliega los campos aparece solo con dos o más y dice «Filtros», «Orden» o «Filtros y orden»;
  una barra de solo búsqueda dice «Buscar» y «Borrar la búsqueda», y una sin pliegue queda fija
  desde la tableta (revisión de interfaz del kit). Los menús de período de Actividad e
  Importaciones pasan a rango. Organizaciones del donante suma el orden por nombre de la condición
  (ii) de la línea base. Pico.css sale del stack.
- **Alternativas:** las opciones B (skills de stock y brief corto) y C (librería tematizada) del
  harness de frontend; dejar Pico declarado o cargarlo.
- **Por qué:** en la línea base solo la A unificó los nombres y contuvo las medidas sueltas (3
  nuevas, contra 32 y 35). Comparando (ii) y (iii) en la maqueta, el usuario dijo "No se ven
  diferencias". Pico no se cargaba desde la identidad única: sacarlo no cambia el aspecto.
- **Costo:** Ranking conserva su selector de mes, y los alias y los períodos que no pasan a rango
  quedan en [DTI-44](../adr/DEUDA_TECNICA.md#dti-44). El usuario no vio las 9 pantallas sumadas.
- **Quién decidió:** usuario (opción A, públicas dentro del fragmento y sin Pico, 2026-11-14); la
  forma de cada barra, agente.
- **Evidencia:** [ADR sin Pico.css](../adr/cliente-liviano/20261114-sin-pico-css-en-el-cliente-liviano.md);
  cercos en `InterfazFitnessTest`.

### 2026-11-14 · componente:listado · La barra de filtros nueva, con los campos abiertos (R11-B04)

- **Decisión:** `componentes/listado` reemplaza a la fila de pastillas en Mis donaciones (empresa y
  persona), Organizaciones del donante, Donaciones recibidas, Donaciones del admin, Todas las
  decididas y las Notificaciones de los tres roles. Un formulario get con los campos a la vista, en
  este orden y con estos parámetros en todas: Buscar (`buscar`), fecha Desde y Hasta (`desde`,
  `hasta`), Estado, Categoría, quién (`quien`), los propios de cada pantalla y Ordenar (`orden`,
  `antiguas`). En el celular, con JavaScript, se pliegan debajo de «Filtros y orden (n)».
- **Alternativas:** seguir con pastillas que abren menús (lo que el usuario descartó el
  2026-11-11); un `<details>` siempre plegado; un panel lateral. El A/B que pide R11-B04 no se hizo:
  el encargo pidió trabajar sin consultar.
- **Por qué:** con los campos a la vista se ve qué filtra cada lista sin abrir nada, el rango de
  fechas entra como dos campos y anda igual sin JavaScript. En el celular, plegados, no tapan la
  lista.
- **Costo:** Asignaciones (Propuestas y Destino), Donantes, Entidades, Camiones, Importaciones,
  Actividad y Ranking del admin y lo público siguen con las pastillas; quedan en
  [DTI-44](../adr/DEUDA_TECNICA.md#dti-44).
- **Quién decidió:** usuario (rediseñar y los campos, 2026-11-11); el diseño, agente sin consultar.

### 2026-11-14 · componente:listado · Lo que el agente decidió sin consultar en R11-B04

- **Decisión:** (1) Nada se envía solo al cambiar un campo: «Filtrar» o Enter (WCAG 3.2.2; antes,
  tocar una pastilla ya filtraba). (2) Las fechas al revés se dan vuelta. (3) Quién es un texto
  que busca sin mayúsculas ni tildes, no un desplegable: puede haber miles de donantes. (4) Un
  campo que la API no resuelve no se ofrece con datos reales (quién en todas; la fecha en
  Notificaciones). (5) El período fijo «Registrada» y «Decidida» pasa a Desde y Hasta. (6) Estado
  es uno solo en Recibidas (antes, varias pastillas prendidas): los varios que llegan desde
  Necesidades se juntan en una opción más. (7) En Notificaciones, Estado (Todas, Sin leer, Leídas)
  reemplaza a «Todas | Sin leer», y Agrupar queda sin agrupar por defecto (como estaba). (8)
  Organizaciones no tiene fecha, estado, quién ni orden: la organización no los tiene. (9) Los
  parámetros viejos (`nombre`, `ver`, `tema`, `orden=viejas`) siguen valiendo. (10) «Sacar los
  filtros» se ve igual en todas las listas (la bandeja lo tenía como vínculo subrayado). (11) La
  primera opción dice «Todos los estados» y «Todas las categorías» en todas las listas. (12) La
  barra se pliega hasta 68,75 em: abierta, en la tableta llegaba a tres renglones fijos. (13) Agrupar
  no cuenta como filtro: solo cambia cómo se ve la lista.
- **Alternativas:** las que cada punto descarta.
- **Por qué:** una sola barra que se use igual en todas las listas, accesible y sin JavaScript.
- **Costo:** lo que la API no resuelve, en [DTI-44](../adr/DEUDA_TECNICA.md#dti-44).
- **Quién decidió:** agente, sin consultar (encargo R11-B04 del 2026-11-14).

### 2026-11-12 · proceso · Cierre de P2: las decisiones de ocho lotes, escritas al cierre

- **Decisión:** las doce entradas que siguen (hasta la de "armazón", sin incluirla) son las decisiones de diseño de los lotes
  de P2 que no escribieron su entrada (`p2-tablero`, `p2-entidades-donante`, `p2-ventanas`,
  `p2-portada-detalles`, `p2-pendiente-solicitud`, `p2-listas-restantes`, `p2-admin-graficos` y
  `p2-campanita`). Cada una dice si la decidió el usuario (su ficha o su spec, aprobadas con las
  opciones recomendadas) o un agente sin consultar.
- **Alternativas:** dejar las decisiones solo en las fichas y en los informes de los constructores.
- **Por qué:** la bitácora es el registro común del sitio; las fichas de `p2-tablero`, `p2-ventanas`
  y `p2-entidades-donante` pedían anotarlas al construir.
- **Costo:** se escribieron después de construir; lo que quedó abierto está en
  [DTI-43](../adr/DEUDA_TECNICA.md#dti-43).
- **Quién decidió:** agente (cierre documental de P2).
- **Evidencia:** [estado de P2](backlog-revision-2026-11-11.md#estado-de-p2-al-cierre-de-la-oleada-2026-11-12).

### 2026-11-12 · donante · Tablero de la empresa: tres columnas de a 3 y cada una con su página

- **Decisión:** (R11-25, lote `p2-tablero`) el inicio de la empresa muestra el boceto A en el
  orden Para tener en cuenta · En camino o por salir · Ya llegó; cada columna trae como mucho 3
  donaciones, su total en la cabeza y su paginador corto (`‹ 1 de 16 ›`), y guarda su página en
  la dirección (`?tener=`, `?camino=`, `?llego=`), así recargar o compartir conserva las tres. Con
  JavaScript cambia solo la columna tocada y el foco va a su título; sin JavaScript, la página
  entera baja hasta la columna (`#tablero-…`). "Esperan destino" es una cifra dentro de Para tener
  en cuenta, sin lista ni "Ver todas".
- **Alternativas:** una sola página para las tres columnas (cambiar una cambiaba todas); "Ver
  todas" hacia Mis donaciones en cada columna (la ficha lo descartó: el tablero ya pagina);
  reemplazar la grilla entera con HTMX (cambiaba las otras columnas mientras se leían).
- **Por qué:** el tablero crecía sin límite; con tres paginadores independientes cada columna se
  recorre sin perder las otras.
- **Costo:** el resto del inicio sigue leyendo la lista de 100 y cada flecha arma el inicio entero
  ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 16, y [DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 1).
- **Quién decidió:** usuario (ficha [r11-p2-tablero](flujos/r11-p2-tablero.md) aprobada con las
  opciones recomendadas, 2026-11-12).
- **Evidencia:** `DonanteInicioControllerTest`; `paginas.txt` con `?escenario=muchas`; recorrido
  con login real del cierre de P2.

### 2026-11-12 · donante · Tablero: la columna tocada se nombra en una cabecera y la altura queda reservada

- **Decisión:** con HTMX, la columna que cambió se identifica con una cabecera propia
  (`X-Tablero`, en `hx-headers`, permitido por la CSP) y la respuesta sale sin el `id` de la
  columna, para que HTMX no salte al ancla; los pies y el "Volver a intentar" de las otras columnas
  se renuevan con `hx-select-oob`, sin volver a anunciar su aviso. Las columnas de más de una
  página reservan el alto de 3 donaciones, así el paginador no se mueve bajo el dedo. Si falla una
  columna, falla solo esa (también la cifra de las que esperan destino) y "entregas en curso hoy"
  vuelve a contarse sobre la lista de antes.
- **Alternativas:** usar `HX-Target` (deja de llegar después del primer cambio, porque el destino
  pierde su `id`); un escucha en `interfaz.js` (es de otro lote y la CSP no deja `hx-on`); armar
  `hx-swap-oob` en el servidor (cambiaba el controller); mostrar una cifra parcial si falla una
  columna.
- **Por qué:** sigue el patrón que ya usa el sitio (`#avisos`), anda igual sin JavaScript y una
  falla no tapa lo que sí cargó.
- **Costo:** la reserva supone filas de 4,5 rem; con notas largas el paginador puede subir igual;
  HTMX deja un aviso en la consola cuando la columna no tiene pie
  ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 1).
- **Quién decidió:** agente (constructor de `p2-tablero`, sin consulta).
- **Evidencia:** `conHtmxLaColumnaTocadaTomaElFocoYNoLlevaElAncla` y los tests del controller.

### 2026-11-12 · donante · Organizaciones: una tarjeta por organización, con los pedidos de la categoría primero

- **Decisión:** (R11-27, lote `p2-entidades-donante`) cada organización es una sola tarjeta, de
  igual alto que las de su fila, con sus pedidos adentro: primero los de la categoría elegida (o
  todos, si no hay), después el resto, y 3 a la vista; los demás, en "Ver N más", un plegable
  dentro del pie de la tarjeta. La categoría pasa a ser un filtro. Las tarjetas "Ya le donaste" se
  atenúan: papel de la página con un filete por dentro en lugar de la sombra.
- **Alternativas:** seguir agrupando por categoría (una organización partida en varios renglones);
  `.plegable` del catálogo (es una tarjeta: quedaba una tarjeta dentro de otra); armar el orden con
  `T()` en la plantilla (Thymeleaf 3.1 restringe las clases estáticas); un método nuevo en la vista
  (la ficha pedía no cambiar vista ni controller).
- **Por qué:** quien dona busca a quién ayudar, no una categoría suelta; la tarjeta entera responde
  "qué pide esta organización".
- **Costo:** "Ya le donaste" queda dentro del título y suma al nombre accesible; en la versión de
  persona, la búsqueda y los filtros siguen sobre los datos de ejemplo de su primera página; las
  tarjetas atenuadas casi no se distinguen y queda aire en las de poco contenido
  ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 3).
- **Quién decidió:** usuario (sección 3 del backlog y la ficha del donante, §10, aprobada con las
  opciones recomendadas) y agente (los detalles de la construcción).
- **Evidencia:** `DonanteEntidadesControllerTest`; [ficha del donante](flujos/raices-donante-navegable.md).

### 2026-11-12 · donante · "Cómo llega tu donación" es una franja de ayuda arriba de la lista

- **Decisión:** (R11-28) la ayuda del depósito es una franja con otro fondo (agua) e ícono de
  información, arriba de la búsqueda, con los datos reales del depósito. Es un fragmento propio
  (`donante-entidades :: ayuda(conDeposito)`) que usa también la persona; su ícono copia el dibujo
  de `componentes/info` con `data-v`. En el celular el ícono va arriba del título; desde 37,5 em, a
  la izquierda.
- **Alternativas:** una tarjeta más de la lista (se confundía con las organizaciones); usar el
  componente `info` tal cual (lo convertía en un botón); sumar clases nuevas.
- **Por qué:** se lee como ayuda y no como una organización más, sin agrandar el catálogo.
- **Costo:** a 360 px ocupa la primera pantalla; es una pieza de pantalla con `data-v`, no un bloque
  del catálogo ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 3; [catálogo](componentes.md)).
- **Quién decidió:** usuario (la franja, sección 3 del backlog) y agente (cómo se armó).
- **Evidencia:** capturas de `donante-entidades` en la verificación visual del cierre.

### 2026-11-12 · componente:dialogo · Las ventanas de la regla 4 bloquean el fondo con `showModal()`

- **Decisión:** (R11-32, lote `p2-ventanas`, las 13 decisiones de la ficha) las ventanas de
  confirmar algo que no se deshace y de formularios cortos llevan `data-bloquea`; con JavaScript se
  abren con `showModal()` sobre el mismo `<dialog popover>`: fondo inerte y quieto, el foco no sale
  con Tab y vuelve al botón, tocar afuera no cierra y Esc y "Cancelar" sí. Sin JavaScript, el popover
  de siempre. Un solo escucha de clics en el documento. Si se corta la conexión al enviar, el aviso
  va dentro de la ventana ("No sabemos si se guardó."). Al guardar el historial de HTMX se cierran.
- **Alternativas:** quitar y devolver el atributo `popover` al abrir (no hizo falta: los tres
  motores aceptan `showModal()` sobre un popover); bloquear todas las ventanas (la foto de una
  entrega es para mirar); el plugin Focus de Alpine (dependencia nueva).
- **Por qué:** la regla 4 pide que esas ventanas bloqueen; con lo nativo del navegador, sin perder
  la confirmación sin JavaScript.
- **Costo:** "Pedir corrección" y "Rechazar" de Organizaciones no tienen aviso de corte (sus
  formularios no usan `hx-boost`); cambia una decisión `proposed` del cliente, registrada en un
  [ADR `proposed`](../adr/cliente-liviano/20261112-ventanas-que-bloquean-el-fondo-con-showmodal-sobre-el-dialogo-con-popover.md)
  sin editar el original ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 4).
- **Quién decidió:** usuario (ficha [r11-p2-ventanas](flujos/r11-p2-ventanas.md) aprobada con las
  opciones recomendadas) y agente (dónde va el aviso de corte, las ventanas de Organizaciones sin él).
- **Evidencia:** `laVentanaQueBloqueaRetieneElFocoEnLosTresMotores` (Chromium, Firefox y WebKit).

### 2026-11-12 · organización · Publicar otra necesidad, en una ventana de 3 pasos

- **Decisión:** (R11-31) la ventana tiene tres pasos, "Qué y cuánto", "Cuándo" y "Por qué y
  revisar", con "Paso N de 3", "Atrás", "Siguiente" y un resumen con "Cambiar". Los textos de error
  de cada campo vienen en el HTML, ocultos, y se muestran al revisar un paso o al enviar: un campo
  que falta en un paso oculto lleva a ese paso. "Siguiente" lleva el foco al primer campo con error.
  "Hasta cuándo" es obligatoria solo con "Una sola vez". Cancelar no pregunta y guarda lo escrito
  hasta recargar. Sin necesidades vigentes, el formulario sigue en la página. En el celular, el
  indicador muestra solo las marcas (el lector oye "<paso>: Hecho / Paso actual / Falta") y el
  resumen pone el nombre de cada dato en su renglón.
- **Alternativas:** el bloque que se desplegaba en la página; `novalidate` y revisar todo a mano;
  nombres de paso más largos.
- **Por qué:** el formulario era corto pero se veía largo; partido en pasos se lee de a poco y el
  resumen deja revisar antes de publicar.
- **Costo:** a 360 px el paso 3 se desplaza por dentro; el orden visual de los botones en el
  celular no coincide con el del foco; una fecha pasada con "Cada semana" no deja avanzar
  ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 4).
- **Quién decidió:** usuario (ficha aprobada) y agente (los errores en el HTML, el foco diferido al
  enviar, el resumen del celular).
- **Evidencia:** `publicarOtraNecesidadVaEnTresPasos` y `target/capturas/ventanas/`.

### 2026-11-12 · público · Abejas que se funden y títulos que no cortan donde caiga

- **Decisión:** (R11-36, lote `p2-portada-detalles`) cada abeja se funde al pasar por detrás de una
  maceta, en los dos sentidos: opacidad 0 justo donde cambia de capa y entera a 10 puntos del
  recorrido. El fundido lo hace el JavaScript del recorrido, cuadro por cuadro. Con "reducir
  movimiento" (también si se activa en vuelo) vuelven quietas y delante; con la pausa, quedan donde
  estaban. (R11-38) "lo que donás" y "echa raíces" van enteros en su renglón: el título del cierre
  de la portada con `nowrap` en la parte destacada y el pie con espacios que no se cortan.
- **Alternativas:** una transición de CSS (chocaba con la regla de que los tiempos viven en
  `movimiento.css`); fundir solo al entrar (el salto al salir también se ve).
- **Por qué:** las abejas desaparecían de golpe y los títulos partían la frase destacada.
- **Costo:** las abejas todavía saltan de lugar antes de la primera ubicación del script, y si se
  pausa en pleno cambio de capa una abeja puede quedar casi invisible
  ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 5).
- **Quién decidió:** usuario (los dos ítems, sección 3 del backlog) y agente (cómo se construyó).
- **Evidencia:** `AbejasYRenglonesTest` (perfil `visual`); captura `publico-portada-1280.png`.

### 2026-11-12 · organización · La solicitud pendiente se ve y se corrige en una página aparte

- **Decisión:** (R11-41, lote `p2-pendiente-solicitud`) "Recibimos la solicitud" muestra "Lo que nos
  enviaste" con los datos que tiene Donaciones y "Corregir los datos", que lleva a una página
  propia (`publico-registro-entidad-corregir`) con el formulario del registro lleno, sin correo,
  contraseña ni términos. El formulario es el mismo fragmento del registro en modo corregir, y los
  dos modos comparten el resumen de errores y los avisos por campo. Si el equipo ya la aprobó,
  "Ingresar de nuevo" vuelve a pedir la sesión sin pedir la clave.
- **Alternativas:** un tercer modo dentro de la hoja de registro (expresiones de tres ramas y
  riesgo para el registro); dos resúmenes de errores (era justo la diferencia que marcó la
  revisión); dejar editar el correo (es de solo lectura y viene del token).
- **Por qué:** quien espera puede revisar y arreglar lo que mandó sin volver a registrarse; la
  regla 4 manda los formularios largos a una página aparte.
- **Costo:** en el registro, los enlaces del resumen terminan ahora con punto; todo 400 dice
  "Falta …" aunque el dato esté y sea inválido, y todo 409 dice "ya aprobada"; "Guardar los
  cambios" no queda fijo en el celular ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 6).
- **Quién decidió:** usuario (ficha [r11-p2-pendiente-solicitud](flujos/r11-p2-pendiente-solicitud.md)
  aprobada con las opciones recomendadas) y agente (la plantilla propia, el modo con `th:with`, el
  resumen unificado).
- **Evidencia:** `PerfilPendienteTest`, `SolicitudEntidadServiceTest`; recorrido de la cuenta
  pendiente en el cierre (11 de 11).

### 2026-11-12 · organización · Las listas de la organización van de a una página y sus cifras son conteos

- **Decisión:** (R11-43, lote `p2-listas-restantes`) Recibidas y Seguir entregas van de a 20 con el
  paginador común, primero lo que hay que atender (en la puerta, con problema, en camino, por salir
  y recibidas); Recibidas muestra todas las recibidas, no solo las del mes, con "N este mes". Cada
  necesidad muestra 3 en curso y 3 recibidas con "Ver todas". Confirmar recepción no tiene
  paginador: el acta lee solo su donación y ofrece la siguiente que espera. El menú "Mes" del
  ranking muestra los 12 meses de la página y el paginador recorre los años; un mes de otra página
  se muestra igual. Organizaciones del admin recupera Estado, Necesidades y Tipo.
- **Alternativas:** seguir escondiendo las recibidas de meses anteriores (con páginas ya no hace
  falta); ordenar por el último cambio (más caro); una lista de a 20 en el acta (muestra una sola
  entrega); 20 meses por página en el ranking (llenaba el menú).
- **Por qué:** ninguna de esas pantallas puede traer todo (regla 1), y una cifra sacada del largo
  de una página engaña.
- **Costo:** dentro de cada grupo el orden es por fecha de registro; "Ver todas" de una necesidad
  lleva a Recibidas sin filtrar por la necesidad; con `?mes=` de otra página el menú queda sin
  opción marcada; el inicio de la organización sigue con una página de 100
  ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 7).
- **Quién decidió:** usuario (spec [R11-p2-listas-restantes](../specs/completed/R11-p2-listas-restantes-spec.md)
  aprobada con las opciones recomendadas) y agente (el acta sin paginador y el filtro `id`, en los
  arreglos de la revisión).
- **Evidencia:** `EntidadNecesidadesControllerTest`, `EntidadConfirmarControllerTest`,
  `RankingServiceTest`; dos [ADR `proposed`](../adr/20261112-seguimientos-de-logistica-e-historial-del-ranking-paginados-por-defecto.md).

### 2026-11-12 · admin · Inicio del admin: dos gráficos de 12 meses, cada uno con su tabla

- **Decisión:** (R11-42, lote `p2-admin-graficos`) al final de "Cómo está todo", a todo el ancho,
  dos gráficos de los últimos 12 meses contando el actual, "Mes a mes" y "Por categoría", con dos
  series: registradas y entregas confirmadas, en pares de columnas (no una dentro de otra). Cada
  uno tiene una frase con lo más importante y su tabla siempre visible. Registradas va en contorno
  de tinta suave con relleno beige y entregas confirmadas, llena del acento: se distinguen por la
  forma, el lugar en el par, la leyenda y la tabla. El tope de la escala es el primer 10, 20, 50,
  100, 200… que alcanza la cifra más alta. Una falla muestra su aviso dentro de la tarjeta, sin
  tapar las cifras.
- **Alternativas:** un selector de período o 6 meses; una tercera serie de vencidas; una librería
  de gráficos en JavaScript (dependencia nueva, no anda sin JavaScript); dos rellenos de color (el
  acento y la tinta suave no llegan a 3:1 entre sí); usar `.tabla` (en el celular hace tarjetas).
- **Por qué:** el usuario pidió gráficos además de las cifras (cambia el "sin gráficos" de la ficha);
  una tabla al lado hace accesible el dibujo.
- **Costo:** a 360 px los rótulos de los meses se leen pegados; los colores de las series viven en
  la hoja de la pantalla; los enlaces a Donaciones filtrada quedan para después
  ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 8).
- **Quién decidió:** usuario (ficha R11-42 aprobada con las opciones recomendadas) y agente (los
  colores, la escala, las frases de empate y "solo el 1", la tabla sin `.tabla`).
- **Evidencia:** `AdminInicioControllerTest`, `AdminPersonasInicioTest`; [sección de la ficha](flujos/raices-entidad-navegable.md#r11-42--los-últimos-12-meses-en-gráficos);
  [ADR `proposed`](../adr/donaciones-service/20261112-evolucion-del-admin-en-un-recurso-aparte-contada-por-mes-con-limites-como-parametros.md).

### 2026-11-12 · componente:campanita · La campanita abre las 5 sin leer y "Ver todas"

- **Decisión:** (R11-29, lote `p2-campanita`) la campanita de la cabecera privada es un desplegable
  liviano (`<details>`) con las 5 sin leer más nuevas, con título y hace cuánto, y "Ver todas". La
  lista se pide cada vez que se abre (si llega con el desplegable ya cerrado, se descarta); nunca
  con cada pantalla ni sondeando. Cada fila es un botón que marca el aviso como leído y lleva a lo
  que nombra; el destino lo calcula el servidor desde la notificación. Sin JavaScript, la campana
  lleva a la bandeja sin leer. Es un componente compartido (`componentes/campanita`).
- **Alternativas:** precargar la lista en cada página (una llamada más siempre); mandar el destino
  desde el navegador (habría que validarlo contra redirecciones abiertas); el estilo dentro de
  `privado.css` (como decía la spec; un componente propio no agranda la hoja ni las heredadas).
- **Por qué:** se mira lo nuevo sin dejar la pantalla; la campanita no cuesta nada hasta que se abre.
- **Costo:** se perdió la marca de pantalla actual (`aria-current`) en la bandeja del rol; el
  fragmento viejo `buzon(...)` sigue porque lo usa la bandeja de la persona en la maqueta
  ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 9).
- **Quién decidió:** usuario (sección 3 del backlog y ficha [r11-p2-campanita](flujos/r11-p2-campanita.md)
  aprobada) y agente (el evento propio de apertura, el componente aparte, conservar `buzon`).
- **Evidencia:** test de la campanita abierta en `VerificacionVisualTest` y capturas
  `target/capturas/campanita/*-abierta-{360,1280,1920}.png`.

### 2026-11-12 · admin · Una bandeja compartida para la Administración, con el aviso de importación terminada

- **Decisión:** (R11-30) la administración tiene campanita con número real y una bandeja compartida
  por todas sus cuentas (`ID_ADMIN`), de a 20, con la misma forma que las otras dos; la bajada dice
  que abrir un aviso lo marca leído para todos. Llegan "Terminó una importación" (con o sin errores)
  y "Una importación se cortó", con la fecha de subida y las cifras, sin el nombre del archivo, y
  los mensajes de contacto. Los dos avisos de importación llevan a la página del archivo, que elige
  resultado, avance o error.
- **Alternativas:** una bandeja por cuenta de admin (pide copiar las cuentas en Notificaciones);
  avisar solo a quien subió el archivo (no se guarda quién); un aviso por llamada REST en lugar de
  un evento.
- **Por qué:** reusa el destinatario que ya existía para el contacto; el evento solo cuenta el hecho
  y Notificaciones elige a quién avisar.
- **Costo:** si un admin abre un aviso, los demás no lo ven como nuevo; cada importación manda un
  correo a la dirección de la Administración ([DTI-43](../adr/DEUDA_TECNICA.md#dti-43), ítem 9).
- **Quién decidió:** usuario (spec y ficha aprobadas con las opciones recomendadas: bandeja
  compartida y correo por importación) y agente (los ejemplos de la maqueta: solo los avisos que
  hoy genera algún servicio).
- **Evidencia:** [ADR `proposed`](../adr/20261112-aviso-de-importacion-terminada-por-evento-amqp-a-la-bandeja-compartida-de-la-administracion.md);
  recorrido de la administración en el cierre.

### 2026-11-12 · armazón · Cabecera y filtros fijos desde la tableta, botón fijo en el celular y pantalla de carga sin JavaScript

- **Decisión:** desde la tableta (ventana de 37,5 em de alto o más) la cabecera con el menú queda
  fija arriba y la barra de búsqueda y filtros (`.filtros-barra` y `form.filtro`, salvo el Mapa
  público) queda fija justo debajo mientras se recorre el listado; en el celular arriba no queda
  nada (el menú ya está fijo abajo) y el botón de Confirmar recepción queda pegado encima del menú
  (`data-fijo="celular"`). La pantalla de carga es un `body::before` oculto en reposo que una
  animación muestra 0,3 s y funde en 0,2 s, sin JavaScript, con precarga de Archivo y DM Sans y
  letras de respaldo ajustadas (lote `p2-armazon`, R11-33 y R11-40; las 8 decisiones de la ficha).
- **Alternativas:** fijar la cabecera también en el celular (con el menú y la barra se come más de
  un tercio de la pantalla); esconderla al bajar y mostrarla al subir (pide JavaScript); quitar la
  pantalla de carga con JavaScript al llegar las letras (rompe la condición del backlog); medir las
  alturas con JavaScript (rompe RNF-CL-02); fijar el filtro del Mapa (ya fija el mapa).
- **Por qué:** con listas largas, el menú y los filtros se perdían al bajar; la pantalla de carga
  tapa el salto de las letras sin demorar la página más de medio segundo.
- **Desvíos de la construcción:** con "reducir movimiento" no hay pantalla de carga (la animación
  no corre y el papel no aparece). En el celular, la barra queda fija solo con la ventana de 45 em
  de alto o más y hasta dos pastillas; la de tres pastillas o más y `form.filtro` se desplazan con
  la página, para no comerse más de un cuarto del alto (arreglos de la ronda 1). En Entregas
  confirmadas el formulario está en `.publica-barra` con la cifra: en la tableta angosta la barra
  se disuelve (`display: contents`) y desde 53,75 em queda fija la barra entera (ronda 2).
- **Valores medidos** (2026-11-12, con las letras cargadas, redondeados hacia arriba a 1/8 rem):
  cabecera privada de 5 secciones, 8 rem hasta 68,75 em y 4,5 rem desde ahí; la de la
  administración, 11,375 rem hasta 53,75 em y 8 rem desde ahí; la pública, 12,25 rem, 8,875 rem
  desde 53,75 em y 5,5 rem desde 68,75 em. La pública corta su menú a 660 y 920 px, fuera de los
  puntos de quiebre: por eso mide al menos su medida (`min-height`) y la barra no deja hueco
  (ronda 2). Barra de filtros más alta: 18 rem en la tableta, 14,5 rem desde 53,75 em, 11 rem desde
  68,75 em y 9,875 rem (157 px) en el celular alto.
- **Costo:** la pantalla de carga aparece en cada página que se carga entera, no solo en la primera
  de la visita (CSS no las distingue); de 660 a 900 px y de 920 a 1100 px la cabecera pública
  queda 54 px más alta de lo que pide su contenido. El botón fijo del celular, por ahora, solo en
  Confirmar recepción.
- **Quién decidió:** usuario (ficha aprobada con las opciones recomendadas, 2026-11-12) y equipo
  (los desvíos de la construcción).
- **Evidencia:** [ficha r11-p2-armazon](flujos/r11-p2-armazon.md); `ArmazonFijoTest` (perfil
  visual) y las capturas de la verificación visual (`publico-portada`, `donante-inicio`,
  `donante-entidades`, `admin-donantes`, `entidad-seguimiento-confirmar`).

### 2026-11-12 · admin · Donantes y Donaciones como bandeja; Todas las decididas se busca y guarda el motivo

- **Decisión:** Donaciones y Donantes del admin pasan a ser una bandeja (`.maestro`, el bloque de
  Entidades): el renglón abre su ficha en la misma página, al lado y fija al desplazarse desde
  53,75 em; en el celular, la del primero no se abre sola y la elegida trae "Cerrar la ficha".
  Todas las decididas suma búsqueda por organización o donación, pastillas de estado, categoría,
  fecha (períodos fijos), quién decidió (Vos, las cuentas por su nombre o "Antes de las cuentas")
  y el orden por la fecha de la decisión; Donaciones guarda el motivo del descarte (hasta 500
  caracteres, `V26`) y lo muestra en la decidida (lote `p2-admin-bandeja`, R11-34 y R11-35).
- **Alternativas:** el período como `?desde=&hasta=` libres en la dirección (descartado: los
  períodos fijos ya son la regla de las otras listas y no piden validar fechas a mano); "Antes de
  las cuentas" como dos valores, SISTEMA y sin autor (descartado: para quien mira es lo mismo);
  buscar el nombre de la organización también en la base en memoria (descartado: solo PostgreSQL
  tiene las organizaciones juntas; en memoria se busca por lo que pide y lo que se donó).
- **Por qué:** con una tabla y un detalle aparte, revisar varias donaciones era ir y volver; la
  bandeja deja la lista a la vista. Sin buscador ni quién decidió, Todas las decididas no servía
  para contestar "¿quién descartó esto y por qué?".
- **Costo:** en la primera entrega quedaron afuera el filtro y el orden por vencimiento en
  Donaciones, la pastilla de fecha de alta en Donantes y el contacto y las 3 últimas donaciones en
  la ficha del donante; los arreglos de la ronda 1 (2026-11-12) los suman (`venceHasta`,
  `orden=VENCE`, `altaDesde` y `altaHasta`, OpenAPI de Donaciones 1.13.0; el texto de límite de
  `?vence=pronto` se va). Queda para después el pictograma de la categoría en el renglón de
  Donaciones. La ficha de la donación del renglón cuesta una lectura por clave.
- **Quién decidió:** usuario (diseño aprobado con las opciones recomendadas, 2026-11-12) y equipo
  (lo de la construcción).
- **Evidencia:** [ficha r11-p2-admin-bandeja](flujos/r11-p2-admin-bandeja.md); capturas de la
  verificación visual (`admin-donantes`, `admin-donaciones`, `admin-asignaciones`).

### 2026-11-12 · donante · "Ver mi huerta" al medio, maceta que se suelta y aviso de lo que espera destino

- **Decisión:** (R11-37, lote `p2-donante-detalles`) la lámina de "Ver mi huerta" va centrada (en
  pantalla ancha, el título al lado del dibujo, los dos juntos y al medio; en el celular, uno sobre
  otro) y el botón queda al medio del invernadero. En Misiones, con una maceta elegida, tocar afuera
  (en un lugar sin enlace, botón ni campo, y fuera de la ficha) o apretar Esc toca "Ver toda la
  huerta": `static/js/paneles.js` lo hace con `a[data-suelta]` y `[data-suelta-adentro]`; con un menú
  o una ventana abiertos, el toque o la tecla solo los cierran. (R11-39) El texto bajo "Entrega por
  entrega" pasa a un aviso (`data-v="sin-destino"`, pieza `donante-seguimiento :: sinDestino`) con el
  ícono del reloj, el título "Todavía esperan destino", las donaciones en una lista y qué pasa después.
- **Alternativas:** soltar con `pointerdown` (se descartó: un arrastre para desplazar en el celular
  soltaría la maceta); soltar con Alpine (se descartó: la maceta elegida vive en la dirección y el
  enlace ya la suelta, con o sin JavaScript); usar el aviso de "Cómo llega tu donación" (agua) tal
  cual (se descartó: esto es información sobre una donación pendiente, va con el tono de info).
- **Por qué:** el enlace "Ver toda la huerta" ya era la salida sin JavaScript; el JavaScript solo lo
  toca, así vale igual con HTMX. El aviso se ve como aviso y no como un renglón perdido bajo las
  tarjetas.
- **Costo:** las macetas del dibujo siguen siendo blancos de toque chicos en el celular (ya lo eran).
- **Quién decidió:** agente, dentro del backlog del 2026-11-11.
- **Evidencia:** `DonanteDetallesTest` (perfil `visual`), `DonanteSeguimientoControllerTest` y
  `DonanteMisionesControllerTest`.

### 2026-11-12 · admin · Registro de actividad: pestaña de Administradores, sin detalle propio

- **Decisión:** la actividad de administración (R11-26, lote `p2-actividad`) es la pestaña
  "Actividad" de Administradores (`Cuentas · Actividad`), una tabla de la más reciente a la más
  vieja, de a 20, con una frase en pasado por acción; el nombre de cada fila filtra por esa cuenta y
  "Ver la donación" o "Ver la importación" llevan a la pantalla de lo que tocó. Acción en seis
  grupos (no 14 tipos), sin pastilla de orden; quién se busca por nombre en Keycloak (hasta 20
  cuentas) o con "Solo lo mío". Lo de antes del lote no se inventa: solo entran las propuestas
  decididas con autor (V22).
- **Alternativas:** una entrada nueva en el menú principal o en el de la cuenta; un detalle por
  acción; un desplegable con todas las cuentas o con los 14 tipos; guardar el nombre del admin en
  cada fila.
- **Por qué:** es la sección de las cuentas; una fila dice todo; traer todas las cuentas rompe la
  regla 1 y el nombre de una cuenta es un dato personal que envejece (ficha §9).
- **Costo:** en el celular es la tabla apilada (`.tabla` con `data-rotulo`) y no una lista `ol`; el
  nombre como enlace va en un párrafo (así lo mide el piso de toque) y el texto para el lector es
  "Bruno Díaz: ver solo sus acciones", no "Ver solo lo de Bruno Díaz" (axe pide que el nombre
  visible esté en el accesible). "Registró una donación en el depósito" no lleva a la donación (el
  registro guarda la donación original). Pendientes en
  [DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 2.
- **Quién decidió:** el usuario (sección 3 del backlog y aprobación automática de la ficha con las
  opciones recomendadas); los detalles de construcción, el agente constructor del lote.
- **Evidencia:** `cliente-liviano/target/capturas/admin-actividad-*.png` (12 rutas, 360, 1280 y
  1920 px); [ficha r11-p2-actividad](flujos/r11-p2-actividad.md).

### 2026-11-12 · público · Portada con sesión: la cuenta se ve en todo lo público, no solo en la portada

- **Decisión:** con la sesión iniciada, la portada y todas las públicas que usan `layout-publico`
  (Entregas confirmadas, Mapa, Organizaciones, Nosotros, las dos landings y Legales) muestran la
  cuenta en lugar de "Ingresar" y "Crear una cuenta" (R11-15, lote `p1-portada`).
- **Alternativas:** solo la portada.
- **Por qué:** el problema ("si voy al home me salgo") vuelve al primer clic en cualquier pública.
- **Costo:** no hay capturas de la portada con sesión ni del ancho de 768 px
  ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 1).
- **Quién decidió:** usuario (regla general 6 y "la misma portada", sección 3 del backlog); bajarlo a
  todo lo público, agente en la ficha [r11-p1-portada](flujos/r11-p1-portada.md) §9.1, aprobada
  por el usuario con las opciones recomendadas.

### 2026-11-12 · público · Portada con sesión: navegación pública más la cuenta, no el marco del rol

- **Decisión:** con sesión, lo público conserva su navegación (Entregas confirmadas, Mapa,
  Organizaciones) y cambia "Ingresar" y "Crear una cuenta" por "Ir a mi inicio", destacado ("Mi
  inicio" en la barra del celular), y la foto con el menú de la cuenta.
- **Alternativas:** el marco privado del rol, como en las páginas de error y Contacto.
- **Por qué:** la decisión del usuario es "la misma portada", y lo público tiene sus propios destinos.
- **Costo:** con sesión hay dos encabezados: el del rol en errores y Contacto, y el público con la
  cuenta en la portada y las públicas.
- **Quién decidió:** agente, en la ficha (§9.2), aprobada por el usuario con las opciones
  recomendadas.

### 2026-11-12 · público · La cuenta pendiente ve "Ver mi solicitud" en lo público

- **Decisión:** la organización con la solicitud pendiente también ve su cuenta en lo público, con
  "En revisión" debajo del nombre y "Ver mi solicitud" ("Mi solicitud" en el celular) en lugar de
  "Ir a mi inicio".
- **Alternativas:** "Ir a mi inicio" para todos, que llevaría igual a la solicitud.
- **Por qué:** la cuenta pendiente no tiene inicio; prometerlo confunde. Cumple la regla 6 y lo que
  R11-08 le dejaba a este lote.
- **Costo:** en "Recibimos la solicitud", "Ver mi solicitud" apunta a la misma página y no se marca
  como la actual ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 1).
- **Quién decidió:** agente, en la ficha (§9.3), aprobada por el usuario con las opciones
  recomendadas.

### 2026-11-12 · público · La cuenta en lo público sale solo de la sesión, sin campanita

- **Decisión:** el encabezado público con sesión no pide nada a ningún servicio: no tiene campanita
  y arma la cuenta con lo que ya está en la sesión (`CuentasDelMenu.deLaSesion` y
  `AreaDeLaSesion.pendiente`). Si el nombre todavía no quedó guardado, el menú dice "Tu cuenta",
  como en errores y Contacto. Si la cuenta no se puede armar, la página sale con el encabezado de
  visitante.
- **Alternativas:** la campanita con el número de avisos; pedir el nombre a Donaciones o a Keycloak
  al cargar la portada (`CuentasDelMenu.de()`).
- **Por qué:** la portada no se tiene que caer ni frenar porque falle un servicio.
- **Costo:** sin la campanita, los avisos sin leer no se ven desde lo público. Con ingreso real, si
  la portada es la primera página después del login, el menú dice "Tu cuenta" hasta entrar a una
  página privada. El error al armar la cuenta se calla sin registro.
- **Quién decidió:** agente: la campanita, en la ficha (§9.4), aprobada por el usuario; usar
  `deLaSesion` en lugar de `de()`, el constructor sin consulta (lote `p1-portada`).

### 2026-11-12 · público · Portada con sesión: los botones de registro quedan como están

- **Decisión:** con la sesión iniciada, "Quiero donar", "Soy una organización", los de "¿Qué planta
  sos vos?" y los del cierre de la portada no cambian.
- **Alternativas:** cambiarlos por "Ir a mi inicio" con sesión.
- **Por qué:** la decisión fue "la misma portada" y esos botones ya llevan al inicio del área;
  cambiarlos tocaba fragmentos de `huerta.html` fuera de Bajo tierra.
- **Costo:** quien dona y toca "Quiero donar" con la sesión abierta va a su inicio sin aviso; conviene
  probarlo con personas.
- **Quién decidió:** agente, duda 1 de la ficha (§9.5), resuelta con la opción recomendada por pedido
  del usuario.

### 2026-11-12 · público · La marca de la portada dice "ir a la portada" y la cuenta sin perfil no cambia

- **Decisión:** el logo de la portada se anuncia como "DonaTrack, ir a la portada", como en el resto
  de las públicas. La cuenta sin perfil sigue como estaba: no llega a la portada y su registro ya
  tiene "Cerrar sesión".
- **Alternativas:** dejar "ir al inicio" en la portada; mostrar la cuenta también sin perfil.
- **Por qué:** con sesión, "inicio" ahora es "Mi inicio", y el mismo nombre para dos lugares
  confunde. La cuenta sin perfil no tiene dónde ir.
- **Costo:** ninguno.
- **Quién decidió:** agente, en la ficha (§9.6 y §9.8), aprobada por el usuario con las opciones
  recomendadas.

### 2026-11-12 · componentes · El menú de la cuenta pasa a un fragmento compartido, con "Administradores"

- **Decisión:** el menú de la foto de la cuenta es un fragmento, `cuenta-menu :: menu`, que usan la
  cabecera privada (`layout-privado`) y la pública con sesión. El enlace "Administradores" de
  `p1-admins` va dentro del fragmento, con la misma condición: solo si Mi perfil termina en
  `/admin-perfil`.
- **Alternativas:** dejar el bloque `<details>` duplicado en `layout-privado` y en la cabecera
  pública.
- **Por qué:** el mismo menú en los dos lados, escrito una sola vez. Al integrar, el primer intento
  chocó justo ahí con "Administradores".
- **Costo:** ninguno en pantalla; `PortadaConSesionMaquetaTest` ahora exige Mi perfil y
  "Administradores" en la administración, y que la organización no tenga "Administradores".
- **Quién decidió:** agente, sin consulta, al integrar `p1-portada` (`9c0a589eb`).

### 2026-11-12 · público · Bajo tierra: cada rótulo con su franja y la letra con techo

- **Decisión:** desde 600 px, cada rótulo de Bajo tierra tiene un ancho tope en % del dibujo, corta
  por palabras sin guion y crece hacia la tierra libre; la letra crece con el dibujo, pero menos y con
  techo (`clamp(0.875rem, 2.6cqi, 2rem)`); el área táctil de "Tu organización" no corre el texto.
  `BajoTierraRotulosTest` mide en un navegador que los rótulos no se crucen de 600 a 1920 px, con
  una letra de respaldo más ancha (Verdana) para cubrir el peor caso (R11-17).
- **Alternativas:** los rótulos como `<text>` dentro del SVG (no corta en renglones y pierde la lista
  para los lectores); líneas de llamada hacia los costados (cambia el dibujo de la diseñadora); subir
  el punto de quiebre a 900 px (esconde el dibujo en las tabletas y no arregla el choque).
- **Por qué:** el choque depende del largo del texto, y con "Tu organización" y "Organizaciones
  religiosas" los textos se alargaron.
- **Costo:** la medición usa la caja del elemento y no suma el contorno de la letra; falta sumarlo o
  explicarlo en el Javadoc ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 1).
- **Quién decidió:** agente, en la ficha (§9.7, sección 4), aprobada por el usuario con las opciones
  recomendadas.

### 2026-11-12 · público · Bajo tierra: "Centros comunitarios" con franja de 26 %, no 22 %

- **Decisión:** la franja de "Centros comunitarios" quedó en 26 % del dibujo
  (`huerta.css`, `[data-lugar="comunitarios"]`); la tabla de la ficha §4 decía 22 %.
- **Alternativas:** el 22 % de la ficha.
- **Por qué:** la ficha da los valores como punto de partida y permite ajustarlos con la medición del
  constructor (`BajoTierraRotulosTest`). La ficha no se reescribe.
- **Costo:** ninguno; el ajuste no estaba registrado hasta esta entrada.
- **Quién decidió:** agente, sin consulta (constructor del lote `p1-portada`).

### 2026-11-12 · proceso · Cierre de P1: las demos de /estilos que necesitan ancho ocupan la pantalla

- **Decisión:** las demostraciones de `/estilos` que se arman en varias columnas (la libreta del
  mes, la bandeja de la organización y su fila paginada, la flota, lo de bajo tierra, los
  segmentos, el maestro) llevan `data-ancho="pantalla"`: ancho mínimo desde la tableta y la ficha
  se desplaza de costado. La tabla pasa a todo el ancho, como el paginador, y la ficha que se
  desplaza va con `position: relative` para que el texto para lectores no se escape.
- **Alternativas:** dejarlas en la celda angosta del catálogo; sacarlas de la verificación visual.
- **Por qué:** en la celda angosta sus enlaces medían 15, 16 o 24 px y la página quedaba más ancha
  que la pantalla entre 1024 y 1440 px: 32 fallas de la verificación visual, solo en el catálogo.
- **Costo:** ninguno en el sitio; las pantallas reales no cambian. `/estilos` sigue desbordando
  1 px a 320 px, debajo del piso de 360 ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 15).
- **Quién decidió:** agente, sin consulta, en el cierre de la oleada (`1606a7a60` y `0edf2811d`).

### 2026-11-12 · admin · Asignaciones: los textos de la ficha con "organización" y el panel al final en el celular

- **Decisión:** los textos de Asignaciones son los de la ficha `r11-p1-asignaciones`, salvo que
  dicen "organización" donde la ficha decía "entidad" (escenario A15 de la spec, con R11-44). En el
  celular, el panel "Estado de la asignación" quedó al final de la página; la ficha lo ponía arriba,
  en una línea.
- **Alternativas:** los textos literales de la ficha, con "entidad"; el panel arriba, como la ficha.
- **Por qué:** "organización" es la palabra del sitio desde `p1-nombres`; el panel al final es lo
  que dejó el constructor, no una elección contra la ficha.
- **Costo:** el panel arriba queda por hacer, con la pista de que las pestañas se deslizan a 360 px
  ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 12).
- **Quién decidió:** usuario ("Organización", R11-44); el panel al final, el agente sin consulta
  (lote `p1-asignaciones`).

### 2026-11-12 · importador · El CSV se lee de a 500 filas y el aviso de bienvenida se reintenta hasta 10 veces

- **Decisión:** el importador lee el CSV de donantes de a tandas de 500, sin cargarlo entero.
  Notificaciones, cuando el aviso de bienvenida llega antes que la persona, devuelve el mensaje a
  la cola hasta 10 veces, contadas por `messageId`; después va a la cola de mensajes muertos.
- **Alternativas:** leer el archivo entero en memoria (lo de P0); mandar el aviso a la cola de
  muertos la primera vez; reintentar sin tope.
- **Por qué:** con 20.000 filas el archivo entero pesa en la memoria de Donaciones, y el aviso no
  se tiene que perder porque `persona.sincronizada` llegue un poco después; un mensaje roto no tiene
  que girar para siempre.
- **Costo:** el tope cuenta vueltas, no tiempo, y el conteo vive en memoria. Falta el ensayo C8/C9:
  por eso la spec `R11-p1-importador` sigue en `active/` ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42),
  ítem 14).
- **Quién decidió:** agente, sin consulta (spec y lote `p1-importador`, aprobados con las opciones
  recomendadas).

### 2026-11-12 · nombres · "Organización" en lo que se ve, "Tu cuenta" si falta el nombre

- **Decisión:** en los textos de pantalla, "entidad" pasa a "organización" (menú, títulos, inicio);
  las rutas, clases y campos no cambian. Si la cuenta no tiene nombre, el menú dice "Tu cuenta". En
  la barra del celular las palabras largas se cortan en sílabas ("Organiza-ciones"). El tipo
  jurídico se muestra con un texto propio ("Empresa, ONG o institución" en el registro).
- **Alternativas:** dejar "entidad"; cambiar también las rutas y las clases (fuera del alcance).
- **Por qué:** "organización" es la palabra de la gente que dona y recibe.
- **Costo:** sin nombre, el saludo dice "Hola, Tu cuenta"; el registro no nombra a Gubernamental;
  sin tipo jurídico, el admin dice "Persona jurídica" y el perfil "Empresa donante"; el corte en
  sílabas espera la opinión del usuario; las fichas de `flujos/` siguen diciendo "entidad"
  ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 13).
- **Quién decidió:** usuario ("Organización", R11-44 y R11-45); "Tu cuenta", el corte en sílabas y
  los textos del tipo jurídico, el agente sin consulta (lote `p1-nombres`).

### 2026-11-12 · público · Mapa: botones debajo, lista plegada y de a 10

- **Decisión:** los botones de salida del mapa de entregadas quedan debajo del mapa; la lista de
  lugares, plegada y de a 10 con el paginador común (`?pagina=` y `?abierta=`). En el celular el
  mapa es cuadrado. Logística devuelve el último mes con tope de 200.
- **Alternativas:** lista abierta y entera (lo de antes); el mapa paginado (cada página movía los
  puntos); de a 20.
- **Por qué:** el mapa contesta "adónde llegaron" con todos los puntos; la lista es su alternativa
  accesible y no tiene que tapar los botones.
- **Costo:** con más de 200 entregas en el mes, el mapa muestra las más recientes y lo dice.
- **Quién decidió:** usuario (botones y lista plegada, ampliación R11-19); el mapa cuadrado, las 10
  por página y el tope, el agente. ADR `proposed`:
  [mapa público por período y con tope](../adr/logistica-service/20261112-mapa-publico-por-periodo-y-con-tope.md).

### 2026-11-12 · notificaciones · La bandeja: sin leer y leída, de a 20 y con búsqueda sin tildes

- **Decisión:** la notificación sin leer va con fondo suave, punto y título en negrita; la leída,
  sin fondo. La bandeja pide de a 20, con búsqueda sin tildes, categoría y orden en la dirección.
  Notificaciones busca con `translate` en SQL nativo, y las categorías de la pantalla se arman con
  `tipo` y `excluirTipo` ("Otras novedades" sin listar todos los tipos).
- **Alternativas:** `unaccent`, como Donaciones; `ILIKE` solo; un valor especial `tipo=NINGUNO`;
  las categorías dentro del servicio.
- **Por qué:** `translate` anda en PostgreSQL y en H2 sin una extensión nueva; `ILIKE` no encuentra
  "camion" en "camión"; las categorías cambian por rol y son de la pantalla.
- **Costo:** `translate` cubre las vocales con tilde, la ü y la ñ, no cualquier signo. Lo que no
  sigue la ficha (textos de los botones, marcar una fila sin mandar la página) está en
  [DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 10.
- **Quién decidió:** usuario (los estilos de sin leer y leída, spec `R11-p1-notificaciones`);
  `translate` y `excluirTipo`, el agente. ADR `proposed`:
  [bandeja por persona paginada](../adr/notificaciones-service/20261112-bandeja-por-persona-paginada-con-busqueda-sin-tildes-por-translate.md).

### 2026-11-12 · admin · Inicio del admin: tarjetas que no piden listas y el error de camiones en su tarjeta

- **Decisión:** el inicio del admin muestra tarjetas con conteos de los resúmenes (ya no pide las
  propuestas); si Logística no contesta, solo la tarjeta de camiones dice que no se pudo leer y las
  demás siguen.
- **Alternativas:** una página de error para todo el inicio; esconder la tarjeta que falló.
- **Por qué:** una falla de un servicio no tiene que tapar lo que sí se sabe, y esconderla haría
  creer que no hay camiones.
- **Costo:** para contar camiones todavía pide toda la flota, y la falla de Logística no queda en
  los registros ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 8).
- **Quién decidió:** usuario (las tarjetas, ficha del inicio); el error acotado a su tarjeta, el
  agente.

### 2026-11-12 · admin · Ranking: el mes en el título y "Volver al último mes"

- **Decisión:** el título dice el mes que se mira y, en un mes anterior, aparece "Volver al último
  mes". Los datos de demostración no dejan meses vacíos.
- **Alternativas:** el mes solo en el selector; un mes vacío con su estado vacío en la demo.
- **Por qué:** una cifra siempre con su período (tono) y una salida clara al mes actual.
- **Costo:** la ficha del ranking (§4.9) no se reescribió, y el historial trae todos los meses
  con todos sus puestos sin paginar ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 6).
- **Quién decidió:** usuario (título y "Volver al último mes"); la demo sin meses vacíos, el agente.

### 2026-11-12 · componente:chip · Una categoría sin ícono usa el genérico

- **Decisión:** el sprite pasa a nueve símbolos (cinco nuevos) y una categoría que no tiene el suyo
  usa `cat-generico`, en vez de quedar sin ícono.
- **Alternativas:** sin ícono; la primera letra de la categoría.
- **Por qué:** todos los chips iguales de alto y de forma, y una categoría nueva no rompe la lista.
- **Costo:** una categoría nueva se ve con el genérico hasta que tenga el suyo; `/estilos` todavía
  no muestra los nueve ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 5).
- **Quién decidió:** agente, sin consulta (lote `p1-iconos`).

### 2026-11-12 · componente:navegacion-cuenta · Los menús se cierran solos

- **Decisión:** el menú de la cuenta y los menús de filtros se cierran al tocar afuera, con Esc y al
  perder el foco, sin cambiar los estilos. Sin JavaScript funcionan como antes.
  `MenusQueSeCierranTest` lo prueba en un navegador.
- **Alternativas:** cerrarlos solo con el mismo botón (lo de antes).
- **Por qué:** es lo que espera la gente de un menú (heurística de estándares).
- **Costo:** al llegar un fragmento por HTMX se cierran todos los menús (el sondeo de una
  importación cierra el de la cuenta), y Esc no respeta un diálogo abierto encima
  ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 4).
- **Quién decidió:** usuario (R11-16: que se cierren al tocar afuera, con Esc y al perder el foco);
  cómo, el agente sin consulta (lote `p1-menus`).

### 2026-11-12 · componente:paginador · El paginador común: las 7 decisiones de su ficha y la ventana simétrica

- **Decisión:** el paginador común `paginas` (`« ‹ 1 2 3 … 9 › »`; `‹ 3 de 9 ›` debajo de 600 px)
  sigue las 7 decisiones de la [ficha](flujos/r11-p1-paginador.md#9-decisiones-y-preguntas):
  (1) primera, última y la actual con una vecina de cada lado, sin "…" que esconda una sola página;
  (2) extremos apagados y visibles; (3) forma corta por ancho y por `data-forma="corta"`; (4) las
  dos formas en el marcado y el CSS elige, sin JavaScript; (5) la ventana se calcula en Java
  (`Paginadores.de`); (6) el provisorio queda como pase hasta que nadie lo nombre; (7) sin "ir a la
  página" ni selector de cuántas por página. Al construirlo, la ventana quedó simétrica: en la
  última página muestra `1 … 7 8 9`.
- **Alternativas:** las descartadas en la ficha (dos vecinas por lado, ventana fija de 5, un
  segundo componente chico, elegir la forma en el servidor o con JavaScript); la ventana asimétrica
  que dibujaba la ficha (`1 … 8 9`).
- **Por qué:** un patrón conocido, igual en todas las listas, que anda sin JavaScript; la ventana
  simétrica deja a la página actual con la misma cantidad de vecinas en cualquier punto.
- **Costo:** la ficha (§4.1) no se reescribe; entre 600 y 700 px, con números de cuatro cifras, la
  forma larga puede partirse en dos renglones ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 3).
- **Quién decidió:** las 7, el agente que diseñó la ficha, aprobadas por el usuario con la
  aprobación automática de P1 (opción recomendada en cada duda); la ventana simétrica, el agente
  constructor sin consulta (lote `p1-paginador`).

### 2026-11-12 · admin · Administradores: cuentas marcadas en el realm, alta desde el sitio

- **Decisión:** el admin tiene Mi perfil y Administradores en el menú de la cuenta, fuera del menú
  de secciones. Un admin da de alta a otro desde el sitio, con una contraseña provisoria que se
  muestra una sola vez; la cuenta se marca con un atributo del realm. Si el alta falla, el error
  vuelve dentro de la misma ventana (`HX-Retarget`). El historial de propuestas muestra el nombre de
  quien decidió, que Donaciones toma del token.
- **Alternativas:** dar de alta solo desde la consola de Keycloak; el error en una página aparte;
  el autor como identificador, sin nombre.
- **Por qué:** RF-AUTH-07 pide que un admin cree a otro; el error en la ventana no hace perder lo
  escrito; un nombre se entiende, un identificador no.
- **Costo:** el stack del usuario tiene el realm viejo: hay que reimportarlo (sin `down -v`) antes
  de mostrar Administradores; la página con la contraseña provisoria puede quedar en el historial
  del navegador ([DTI-42](../adr/DEUDA_TECNICA.md#dti-42), ítem 2).
- **Quién decidió:** usuario (cuentas por atributo y contraseña provisoria, spec `R11-p1-admins`);
  `HX-Retarget` y el nombre en el historial, el agente. ADRs `proposed`:
  [cuentas marcadas en el realm](../adr/auth-service/20261112-cuentas-de-administracion-marcadas-en-el-realm-y-alta-desde-el-sitio.md)
  y [autor desde el token](../adr/donaciones-service/20261112-autor-de-la-decision-de-una-propuesta-desde-el-token.md).

### 2026-11-12 · donante · "Ya doné" y Categoría filtran todas las entidades, en Donaciones

- **Decisión:** Entidades del donante pide `GET /api/donantes/{id}/entidades` de a 6: la búsqueda,
  la categoría y "Ya doné" se resuelven en Donaciones sobre todas las activas y cada entidad trae
  si el donante ya le donó. Las categorías del filtro son las reales (las del catálogo), un pedido
  sin categoría va en "Otros pedidos" y una entidad con lo de la categoría elegida ya cubierto
  aparece con "Lo que pedía de … ya está cubierto". Mis donaciones suma la subcategoría (manda
  sobre la categoría, RF-05) y las dos listas usan el componente `paginas`. El nombre de la entidad
  va en su propia línea, con "Ya le donaste" debajo, y corta palabras largas (R11-18).
- **Alternativas:** filtrar la página traída (lo de P0, que decía "En esta página"); un `donanteId`
  en la ruta pública; cruzar en el cliente las donaciones del donante (pide la lista entera);
  sacar de la lista a la entidad con todo cubierto (lleva lo asignado a SQL, más grande que el lote).
- **Por qué:** la regla 1 del backlog (nunca la lista entera) y que el filtro diga lo que hace.
- **Costo:** al filtrar por categoría puede aparecer una entidad sin nada pendiente de esa categoría;
  Seguimiento pide de una vez solo los estados en curso, con tope de 100.
- **Quién decidió:** usuario (spec `R11-p1-donante-listas`, las tres dudas con la opción
  recomendada); los detalles, el agente.
- **Arreglos de la ronda 1 de revisión:** una entidad que llega sin ningún pedido vigente (con
  "Ya doné", la que ya tiene todo cubierto) se ve en "Sin pedidos pendientes" con "Hoy no pide
  nada." (texto de la ficha) y su marca, así la pantalla muestra todas las que cuenta. La maqueta
  dibuja una segunda página con un nombre de 60 letras sin espacios, uno de 120 con espacios y esa
  entidad; las capturas a 360, 768, 1280 y 1920 px, como empresa y como persona, están en
  [`evidencia/r11-p1-donante-listas/`](evidencia/r11-p1-donante-listas/).

### 2026-11-12 · admin · Asignar donaciones: tres pestañas y quién sugirió cada propuesta

- **Decisión:** la variante C pasa a ser la pantalla, con tres pestañas que son enlaces del servidor
  (Propuestas, Conflictos y Destino, con la cuenta del resumen), una sola tarjeta de propuesta con
  "Sugerida por" (el algoritmo en lenguaje llano y su puesto, "3.º de 10") y el panel "Estado de la
  asignación" a la derecha desde 68,75 em. Se borran las variantes A, B y C, su hoja común y
  `asignaciones.js`. Todo llega paginado del servidor (de a 10; las decididas, de a 20).
- **Alternativas:** cargar las tres pestañas juntas y alternarlas con Alpine (tres listas por
  visita); el panel arriba del código en el celular (cambia el orden del foco de la ficha).
- **Por qué:** cada pestaña es una dirección que anda sin JavaScript y pide una sola página; el
  panel va después de la lista en el código, como pide el orden del foco, y en el celular queda al
  final.
- **Costo:** el panel cambia de columna en 68,75 em (el quiebre ancho de los tokens), no en 1280 px
  como dice la ficha. "Alcanza para una" solo se dice cuando compiten dos; con más, "no alcanza para
  todas". El nombre de la organización para el aviso viaja oculto en el formulario.
- **Quién decidió:** usuario (pestañas, variante C, sin variantes); el agente, lo demás (lote
  `p1-asignaciones`, [ficha](flujos/r11-p1-asignaciones.md)).

### 2026-11-12 · admin · Las importaciones de donantes son una pestaña de Donantes

- **Decisión:** `/admin-importaciones`, pestaña "Importaciones" junto a "Donantes", con la lista de a
  20, búsqueda por nombre del archivo y filtros Estado, Subida y Ordenar resueltos en Donaciones.
  "Terminadas" incluye las que terminaron con filas con error; cada renglón lleva a su avance o a su
  resultado, y el resultado ofrece descargar las filas con error en CSV.
- **Alternativas:** una entrada propia en el menú; "Terminadas" solo para las sin error; una tabla y
  una lista aparte para celular (se usó una sola tabla con `data-rotulo`).
- **Por qué:** es parte del trabajo con donantes; la palabra "Terminada" vale para las dos; menos
  marcado que mantener.
- **Costo:** la barra de búsqueda fija al desplazarse queda para el lote dueño de `filtros.css`; las
  importaciones viejas dicen "Archivo sin nombre guardado" y "Sin fecha guardada".
- **Quién decidió:** usuario (ficha r11-p1-importaciones aprobada); los detalles, el agente.

### 2026-11-12 · entidad · La cuenta pendiente navega lo público, sin el ingreso ni el registro

- **Decisión:** con la solicitud pendiente, la cuenta puede ir a la portada, Entidades, Donaciones,
  el Mapa, Nosotros, Para donantes, Para entidades, Legales y Contacto (también enviar el
  formulario). Ingresar, Crear una cuenta, Recuperar, Activar y todo lo privado la siguen llevando a
  "Recibimos la solicitud". La cuenta sin perfil sigue igual: solo puede completarlo (R11-08).
- **Alternativas:** dejar pasar todo lo que empieza con `/publico-` (vería "Crear una cuenta" o
  "Ingresar" y entraría en un lazo); dejar Contacto afuera.
- **Por qué:** los botones de la pantalla volvían a la misma pantalla. Una lista explícita evita el
  lazo, y la regla general 6 nombra el contacto con soporte entre lo que ve quien tiene sesión.
- **Costo:** una pantalla pública nueva hay que sumarla a la lista (`EstadoDeCuenta`); una ruta
  pública que no existe lleva a la solicitud en lugar del 404; en lo público la cuenta pendiente ve
  el encabezado de visitante hasta R11-15 ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 4).
- **Quién decidió:** usuario (la cuenta pendiente navega lo público, R11-08) y agente (la lista y
  Contacto), sin consultar (lote `p0-pendiente`).
- **Evidencia:** `PerfilPendienteTest`, [ficha pública](flujos/raices-publico-navegable.md).

### 2026-11-12 · componente:envio · Lo que está dentro de un formulario no hereda su cartel de carga

- **Decisión:** todo formulario con `hx-indicator` que tiene adentro un enlace o un pedido HTMX lleva
  `hx-disinherit="hx-indicator hx-select-oob"`: "Cancelar" y "Llegó con un problema" ya no muestran
  "Registrando…" ni traen los avisos de un envío. Se revisaron todos los formularios, no solo los
  cuatro anotados, y `IndicadorEnFormulariosTest` falla si uno nuevo repite el defecto. "Sumar otro
  bien" (registrar donación) queda sin cartel: cargar otro renglón es rápido y no es el envío.
- **Alternativas:** `hx-boost="false"` en cada enlace (pierde la navegación con boost y las
  transiciones, y hay que acordarse en cada enlace nuevo); poner el indicador en el botón de envío
  (el envío con Enter queda sin cartel).
- **Por qué:** se pone una sola vez, en el formulario, y no toca su propio envío.
- **Costo:** el test lee las plantillas como texto (no ve lo que entra por `th:insert`); falta
  comprobarlo en un navegador con un pedido lento ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 3).
- **Quién decidió:** usuario (R11-07) y agente (cómo y el alcance), sin consultar (lote
  `p0-indicador`).
- **Evidencia:** [catálogo, fila del indicador](componentes.md), `IndicadorEnFormulariosTest`.

### 2026-11-12 · errores · El error con sesión no pide nada a los servicios y elige su salida

- **Decisión:** con sesión, 404 y 403 ofrecen "Volver a mi inicio" (el inicio del área); sin sesión,
  "Ir al inicio" (la portada). En el 5xx, "Volver a intentar" sigue primero. El menú del error se
  arma con lo que ya está en la sesión: la campanita va sin número y el menú de persona (con
  Misiones) sale solo si la sesión ya guarda que la cuenta es de una persona. La cuenta sin perfil o
  con la solicitud pendiente ve el marco público. Si algo falla al armar el marco privado, queda el
  público: nunca una página en blanco.
- **Alternativas:** pedir los avisos sin leer y el tipo de cuenta en la página de error (otra
  llamada que puede fallar justo cuando algo falló); resolver el marco en cada controller y en cada
  plantilla (se repetía).
- **Por qué:** una página de error tiene que andar aunque los servicios no contesten.
- **Costo:** una persona cuyo tipo todavía no quedó en la sesión ve el menú de empresa en el error;
  con sesión, Contacto y los errores se ven más anchos que sin ella; faltan las capturas del estado
  con sesión ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 2).
- **Quién decidió:** agente, sin consultar (lote `p0-errores`), dentro de la ficha.
- **Evidencia:** `ErroresConSesionTest`, `SitioControllerTest`.

### 2026-11-12 · errores · Con sesión, las páginas de error y Contacto muestran la cuenta

- **Decisión:** con sesión activa, las páginas 404, 403 y 5xx y las tres de Contacto (el
  formulario, enviado y su error) usan el encabezado y el menú del área de la cuenta. Sin sesión,
  el marco público de siempre (R11-05, regla general 6).
- **Alternativas:** el marco público para todos, como decidía la ficha del 2026-11-07.
- **Por qué:** con el marco de visitante ("Ingresar / Crear una cuenta") parecía que la sesión se
  había cerrado.
- **Costo:** cambia la decisión del 2026-11-07; la [ficha](flujos/raices-paginas-de-error.md) espera
  el OK del usuario sobre cómo quedó. Falta probarlo con Keycloak real en un 500 de verdad.
- **Quién decidió:** usuario (ronda de la revisión del 2026-11-11). Cómo se arma (un puerto que dice
  el área de la sesión y un aviso acotado a las páginas de error), agente, sin consultar.
- **Evidencia:** `ErroresConSesionTest`, `SitioControllerTest`.

### 2026-11-12 · proceso · Una llamada que tarda demasiado termina en la página de falla

- **Decisión:** el cliente liviano y el gateway tienen límites de espera: conexión 3 s; lectura 15 s
  en el cliente y 10 s en el gateway; subida del CSV y de las fotos del acta 120 s y 100 s. Se
  cambian por variable de entorno, sin tocar código. Pasado el límite, el gateway contesta 504 y el
  cliente muestra su página de falla con el código de soporte (R11-04).
- **Alternativas:** un solo límite para todo (la subida del CSV no entraba); otro cliente para las
  subidas (obligaba a tocar pantallas de otros lotes); dejar que el gateway conteste 500.
- **Por qué:** una pantalla colgada no dice nada; una falla con su código sí dice qué hacer.
- **Costo:** 15 s puede quedar corto para correr la asignación en la demo; el 504 y los tiempos no
  se probaron en Docker ([DTI-41](../adr/DEUDA_TECNICA.md#dti-41), ítem 1).
- **Quién decidió:** usuario (R11-04) y agente (los valores y cómo), sin consultar (lote
  `p0-esperas`).
- **Evidencia:** `ClientesHttpEsperasTest`, `EsperasGatewayTest`.

### 2026-11-12 · entidad · La solicitud enviada dice que la revisión suele llevar unos días hábiles

- **Decisión:** "Solicitud enviada" dice "La revisión suele llevar unos días hábiles" y "Mientras
  tanto, podés recorrer el sitio: la portada, las entidades y lo que piden". La cuenta pendiente
  navega lo público; lo privado la sigue llevando a esa pantalla (R11-08).
- **Alternativas:** seguir sin plazo, como fijaba la ficha; dar una cifra ("hasta 5 días hábiles").
- **Por qué:** R11-08 pide decir cuánto suele tardar. Sin plazo, la entidad no sabe cuánto
  esperar. Una cifra sería una promesa que nadie fijó; "unos días hábiles" orienta sin prometer.
- **Costo:** sigue sin haber un plazo acordado (pregunta abierta 2 de la
  [ficha](flujos/raices-publico-navegable.md)); si el equipo lo fija, el texto lo toma.
- **Quién decidió:** usuario (decir cuánto suele tardar, R11-08) y agente (la frase sin cifra ni
  fecha), sin consultar (lote `p0-pendiente`).

### 2026-11-12 · admin · Los motivos de las filas con error, en una frase

- **Decisión:** en "Filas con error" de la importación, la columna "Qué pasó" muestra una frase por
  motivo ("La fila no tiene las columnas esperadas.", "El correo ya es de otro donante.") y no el
  código del servicio (`FORMATO_INVALIDO`, `PERSONA_DUPLICADA_EN_CONFLICTO`). Un motivo que el
  cliente no conoce muestra la frase de error inesperado, nunca el código.
- **Alternativas:** escribir las frases en la plantilla (difícil de probar); que Donaciones mande el
  texto (cambia el contrato, donde `motivo` es una lista cerrada).
- **Por qué:** español llano y la regla 4 de los textos: sin códigos a la vista.
- **Costo:** un motivo nuevo del servicio necesita su frase en el cliente (`AdminPersonasRest`).
- **Quién decidió:** agente, en los arreglos del cierre de P0, sin consultar.
- **Evidencia:** `AdminPersonasRestTest` (los datos de prueba pasan a los códigos reales).

### 2026-11-12 · donante · La búsqueda de Entidades la resuelve Donaciones, sin tildes

- **Decisión:** la pantalla Entidades del donante deja de volver a filtrar la página por nombre:
  manda la búsqueda a Donaciones, que ignora tildes y mayúsculas. La cuenta con búsqueda es el total
  del servicio ("N entidades activas tienen «x» en el nombre. Página p de n.") y el paginador
  conserva lo buscado. En la maqueta, el adaptador busca igual, sin tildes.
- **Alternativas:** seguir filtrando en la pantalla, sin tildes (seguía mirando una sola página).
- **Por qué:** el pedido de la corrida: toda búsqueda ignora tildes y se resuelve en el servidor.
  "direccion" no encontraba "Dirección de Desarrollo Social".
- **Costo:** "Ya doné" y "Categoría" siguen filtrando la página, y lo dicen, hasta `p1-donante-listas`.
- **Quién decidió:** usuario (regla) y agente (cómo, en los arreglos del cierre de P0).
- **Evidencia:** `DonanteEntidadesSinMaquetaTest`.

### 2026-11-12 · proceso · El ensayo de 20.000 filas alcanza en rendimiento; la cuenta, no

- **Decisión:** con dos corridas en el stack propio, la importación de 20.000 filas tardó de 125 a
  142 s sin frenar al resto del sitio (p95 menor a 0,6 s) y Donaciones no pasó de 420 MiB: el
  importador por lotes (R11-B08) no hace falta por rendimiento. La cuenta falló porque el importador
  no creaba empresas; se arregló pasando el representante de relleno que ya arma `PersonaMapper`.
- **Alternativas:** que el CSV traiga el representante (cambia el contrato de la importación y pide
  ADR); que una empresa pueda no tener representante (rompe una regla del dominio).
- **Por qué:** es el arreglo más chico que no cambia ni el archivo ni el dominio.
- **Costo:** falta repetir la corrida con el archivo de la guía. La lectura del CSV sin cargarlo
  entero entra igual en esta corrida (`p1-importador`), por el pedido de que toda lista tenga
  paginación o loteo.
- **Quién decidió:** agente (criterios de "suficiente" de la guía y arreglo del cierre), sin
  consultar.
- **Evidencia:** [guía del ensayo](../testing/ensayo-importacion-20000.md#resultados-del-cierre-de-p0-2026-11-12).

### 2026-11-11 · regla general · Ninguna lista llega completa: sin página, la primera de 20

- **Decisión:** los listados de Donaciones (donantes, personas, donaciones, donaciones
  independientes y entidades) devuelven siempre una página: si no se pide, la primera de 20, y como
  mucho 100. El total viaja aparte, como número. Toda pantalla pide su página; un resumen pide 3 y
  "Ver todas".
- **Alternativas:** que sin página siguiera llegando todo hasta migrar el cliente (lo proponía la
  spec del lote).
- **Por qué:** era lo que tumbaba las pantallas del admin con 20.000 donantes, y bastaba una
  pantalla olvidada para repetirlo.
- **Costo:** una pantalla que no se migró ve 20 sin aviso; las que todavía piden una página de 100
  sin paginador están en DTI-39, con su lote.
- **Quién decidió:** usuario, al aprobar la spec `R11-p0-api-listados` (ajuste §0.2).
- **Evidencia:** [ADR](../adr/donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md) (`proposed`).

### 2026-11-11 · regla general · La búsqueda ignora tildes y mayúsculas

- **Decisión:** "Gomez" encuentra "Gómez", y "GÓMEZ" encuentra "gomez", en todas las búsquedas que
  resuelve Donaciones. Lo hace la base.
- **Alternativas:** dejarlo para después (lo proponía la spec del lote); normalizar en el cliente
  (filtraría solo la página).
- **Por qué:** quien busca no sabe cómo se cargó el nombre.
- **Costo:** Donaciones necesita la extensión `unaccent` de PostgreSQL.
- **Quién decidió:** usuario, al aprobar la spec (ajuste §0.1) y en el pedido de la corrida.
- **Evidencia:** [ADR](../adr/donaciones-service/20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md) (`proposed`).

### 2026-11-11 · componente:paginador · Un paginador provisorio, solo con piezas del catálogo

- **Decisión:** hasta que exista el paginador común (R11-11), las listas del admin usan "‹ Anterior ·
  Página N de M · Siguiente ›" debajo de la lista, armado con racimo, botón y el tono suave. Cada
  enlace es una dirección real (`?pagina=N` con la búsqueda y los filtros); el extremo que no sigue
  no es un enlace; con una sola página no se dibuja; al cambiar, el foco va al título de la lista.
  Una página pedida más allá de la última muestra la última.
- **Alternativas:** una clase nueva (el cerco del catálogo la rechaza); mostrar una página vacía
  fuera de rango (no se sabe cómo volver).
- **Por qué:** funciona sin JavaScript, el "atrás" vuelve a la página anterior y R11-11 lo cambia de
  forma sin cambiar las direcciones.
- **Costo:** no se puede saltar a una página por su número hasta R11-11.
- **Quién decidió:** usuario (paginador provisorio en la ficha del admin) y agente (los detalles).
- **Evidencia:** [catálogo](componentes.md#piezas-compuestas-sin-clase-propia), [ficha del admin](flujos/raices-admin-navegable.md), §4.1.

### 2026-11-11 · admin · Páginas de 20 con el servicio y de 5 en la maqueta

- **Decisión:** Donantes, Donaciones y Entidades del admin piden páginas de 20. La maqueta usa 5,
  para que el paginador se vea con sus 10 donantes de ejemplo y en las capturas.
- **Alternativas:** 20 también en la maqueta (el paginador nunca aparecería).
- **Por qué:** 20 es el tamaño por defecto del servicio; 5 deja probar el paginador sin datos reales.
- **Costo:** la maqueta no muestra cómo se ve una página llena.
- **Quién decidió:** agente, sin consultar.

### 2026-11-11 · admin · Lo que Donaciones no filtra no se ofrece

- **Decisión:** fuera de la maqueta, Donantes ofrece tipo (Personas, Empresas, ONG, Instituciones y
  Organismos públicos), categoría, estado y orden. No ofrece "Sin activar su cuenta" ni la columna
  "Qué dona", porque Donaciones no los guarda. Donaciones no ofrece "Vence en los próximos 7 días"
  ni los órdenes por vencimiento, nombre o donante: con "Ver las que vencen" muestra las del
  depósito y dice cuántas vencen y que la lista todavía no se puede filtrar así. Entidades no ofrece
  estado, necesidades ni tipo. Un valor de filtro que no es una de las opciones se descarta.
- **Alternativas:** filtrar en la pantalla la página traída (engaña: parece que filtra todo); traer
  todo para filtrar (la regla 1 lo prohíbe).
- **Por qué:** regla 1: si el servicio no resuelve un filtro, filtrar una página da un dato falso.
- **Costo:** esos filtros vuelven con `p2-admin-bandeja` y `p2-listas-restantes`, que cambian el
  contrato de Donaciones (DTI-39).
- **Quién decidió:** agente, sin consultar.

### 2026-11-11 · admin · El buscador del registro trae 10 y dice cuántos coinciden

- **Decisión:** al registrar una donación, "Quién dona" pide a Donaciones hasta 10 donantes activos
  ordenados por nombre, con cuántos coinciden. Si son más, dice "Hay N donantes que coinciden.
  Escribí más del nombre o el documento para encontrarlo." Es un buscador, no un listado: no tiene
  paginador.
- **Alternativas:** paginar el buscador (es una elección rápida, no una lista para recorrer).
- **Por qué:** con 20.000 donantes el buscador no encontraba al número 21 en adelante.
- **Costo:** quien no recuerda el nombre tiene que ir a Donantes.
- **Quién decidió:** agente, sin consultar.

### 2026-11-11 · admin · Entidades de a una página, las que piden sumarse primero

- **Decisión:** Entidades del admin va de a una página de 20 con la búsqueda por nombre en Donaciones
  (sin tildes). Las que piden sumarse vienen de Keycloak: se filtran por nombre sin tildes, van
  primero, solo en la página 1, y cuentan en el total. La cifra de activas es un número del
  servicio.
- **Alternativas:** asignar la pantalla a otro lote (el revisor lo marcó como bloqueante de este).
- **Por qué:** la regla 1 vale también para Entidades, y lo que espera una decisión va arriba.
- **Costo:** las necesidades vigentes de la página todavía se leen en una sola llamada sin página
  (DTI-39, ítem 2).
- **Quién decidió:** agente, sin consultar (arreglos de la ronda 2 de `p0-admin-listados`).

### 2026-11-11 · admin · El avance de la importación se actualiza solo y se puede salir

- **Decisión:** la página de avance se vuelve a pedir cada 3 segundos con HTMX, solo mientras la
  pestaña está a la vista, y cambia solo el bloque del avance. El cambio de etapa se anuncia a los
  lectores de pantalla. Si un pedido falla, quedan las cifras de antes y un aviso. Sin JavaScript,
  un enlace "Ver el avance". "Se actualiza sola" aparece recién cuando llegó una actualización. La
  última importación queda en la sesión: Donantes muestra una franja hasta que se abre el resultado.
  Un archivo que falló con filas ya leídas se muestra como importación cortada, con su resultado.
- **Alternativas:** eventos del servidor o WebSocket (un canal nuevo, que pide ADR); recordar la
  importación en la dirección o en el navegador.
- **Por qué:** lo pidió el usuario (avance en vivo y poder seguir trabajando) y el sondeo ya es el
  patrón del sitio (posición del camión, D22).
- **Costo:** otro admin u otro navegador no ve la franja hasta la vista de importaciones (R11-20).
  Una importación que falla sin leer ninguna fila deja vacío el bloque durante el sondeo (DTI-40,
  ítem 7, `p1-importaciones`).
- **Quién decidió:** usuario (en vivo, se puede salir) y agente (cómo), sin consultar.
- **Evidencia:** [ficha del admin](flujos/raices-admin-navegable.md), R11-02 (en revisión).

### 2026-11-11 · donante · El inicio de la persona: el año, aunque esté vacío, y la huerta compacta

- **Decisión:** si la cuenta donó otros años pero no este, el inicio dice "Todavía no hay donaciones
  tuyas registradas en <año>." y no que nunca donó; las cifras cuentan solo el año. La huerta de la
  persona queda compacta (en pantalla ancha, el título al lado del dibujo) para que el año se vea
  enseguida. El tipo de la cuenta se guarda en la sesión; sin tipo, nunca se trata como empresa.
- **Alternativas:** el vacío de siempre ("Todavía no donaste"), que mentía para quien donó antes.
- **Por qué:** tono: no afirmar más de lo que se sabe.
- **Costo:** en el celular, "Tu huerta" queda debajo del saludo y de los dos botones: hay que
  confirmar con el usuario si eso es "arriba de todo" (R11-06).
- **Quién decidió:** agente, sin consultar (lote `p0-persona`).
- **Evidencia:** [ficha](flujos/r11-p0-persona.md).

### 2026-11-11 · donante · Mis donaciones de a una página, con los filtros en el servidor

- **Decisión:** Mis donaciones pide a Donaciones una página de 12 con el mes, el estado y la
  categoría, y cuenta el total con `X-Total-Count`. Debajo de las láminas, un paginador provisorio
  (anterior, "Página N de M", siguiente) que conserva el mes y los filtros.
- **Alternativas:** dejar las páginas para `p1-donante-listas` (descartada: "Ver todas las de
  octubre (25)" llevaba a 20 sin aviso); usar ya el paginador de `p1-paginador` (todavía no existe).
- **Por qué:** reglas 1 y 2 del backlog: ninguna lista completa, y "Ver todas" lleva al listado
  completo paginado.
- **Costo:** el paginador numerado lo pone `p1-paginador`.
- **Quién decidió:** equipo (arreglos de la revisión de p0-persona, ronda 2).

### 2026-11-11 · donante · El año de la persona, de a 3 por mes y con las cifras como números

- **Decisión:** en el inicio de la persona, cada mes muestra sus 3 donaciones más recientes y
  "Ver todas", que lleva a Mis donaciones filtrada por ese mes (`?mes=2026-10`). Las cifras del año
  (donaciones, entregadas, en camino, en el depósito) y las de cada mes llegan como números: el
  total (`X-Total-Count`) que da Donaciones para cada pedido. La persona no pide la lista entera.
- **Alternativas:** dejar el resumen del inicio sin páginas como excepción aprobada (el usuario
  pidió páginas o lotes en todas las listas); un endpoint nuevo de resumen del donante (cambia la
  API de Donaciones, fuera de este lote); contar en la plantilla (rompe la regla 1).
- **Por qué:** reglas 1 y 2 del backlog R11: nada de listas completas y un resumen con 3 y "Ver
  todas".
- **Costo:** el inicio hace un pedido por mes del año, más tres para las cifras. Mis donaciones
  filtrada por mes todavía no tiene páginas: las suma `p1-donante-listas`, que conserva el `mes`.
- **Quién decidió:** usuario (punto 4 del pedido de la corrida) y agente (arreglos de la ronda 1).
- **Evidencia:** [ficha](flujos/r11-p0-persona.md), sección 4.

### 2026-11-11 · donante · Las personas ven sus pantallas de persona (R11-06, R11-09)

- **Decisión:** el tipo de la cuenta se pide a Donaciones una vez por sesión; persona (`HUMANA`) y
  sin tipo ven Inicio y Mi perfil de `persona/` (con datos reales) y las demás pantallas comunes con
  sus textos; solo la persona confirmada tiene Misiones en el menú y "Ver mi huerta", arriba de
  todo. La empresa pierde la huerta y "¿Qué planta sos vos?". "Ya doné" filtra la página y lo dice.
  Con login real, "Cómo llega tu donación" va sin el depósito de ejemplo y ofrece escribir.
- **Alternativas:** pedir el tipo en cada pantalla o sumarlo al token; deducirlo de la razón social;
  cablear las siete plantillas de `persona/`; una dirección fija del depósito en la configuración.
- **Por qué:** una cuenta sin tipo nunca se trata como empresa; una sola versión de cada pantalla
  común; ningún dato de ejemplo con login real.
- **Costo:** sin "Tu próxima misión" ni fecha de nacimiento; "Ya doné" abarca la página hasta
  `p1-donante-listas`; la persona tiene que escribir para saber el depósito.
- **Quién decidió:** usuario (backlog R11, sección 3) y agente (ficha `r11-p0-persona.md`, sección 9).
- **Evidencia:** [ficha](flujos/r11-p0-persona.md).

### 2026-11-08 · ux · Revisión de UX: portada, entidades, registro e inicio del donante

- **Decisión:** a pedido del usuario, como revisión de UX/UI:
  - General: las abejas son las de los assets; los títulos de sección ya no se equilibran en
    renglones cortos y la bajada llega a 48 rem; los campos para escribir llevan texto de ejemplo,
    esquinas redondeadas y un fondo más claro (`--color-campo`).
  - Portada: la pausa es un ícono ‖ en un círculo, en la esquina inferior derecha de las láminas; las
    abejas vuelan en loop por los dos recorridos que dibujó el usuario; el árbol de cómo funciona
    queda plantado sobre la tierra en todos los anchos; «Qué ganás si donás» pasa a «Qué podés
    donar», en la tarjeta de quien dona; las acciones de quién participa quedan alineadas al pie; las
    preguntas tienen respuestas completas y cercanas; el cierre dice «Mirá hasta dónde llega lo que
    donás»; lo que viajó este mes es una galería de fotos.
  - Entidades: el buscador con «Por ejemplo: comedor.» adentro; filtros como los de LinkedIn
    (categoría, tipo de pedido recurrente o extraordinario, emergencias); la cuenta con el árbol, en
    el hero.
  - Registro: en una sola página, centrado, como el de frontend-donatrack; el enlace a los términos,
    dentro de la casilla; validación en castellano.
  - Inicio del donante: el estado de las donaciones en una barra con su leyenda; las macetas a
    escala; la tarjeta de cada donación entera como enlace, con el estado arriba y los datos con
    íconos (sin el número interno).
- **Alternativas:** para el cierre, «Sumate a la huerta» y «Empezá a ver dónde llega lo que donás».
  Para quién participa, pestañas como en K: se descartaron porque esconden dos de las tres opciones.
- **Por qué:** los problemas que el usuario fue encontrando al usar el sitio.
- **Costo:** la validación en castellano necesita JavaScript; sin él, valida el navegador en su
  idioma. Los pedidos de ejemplo tienen tipo y emergencia en la maqueta; con backend, HB pendiente.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/piel-huerta/`](evidencia/piel-huerta/).

### 2026-11-08 · componente:navegacion · La campana y el menú de la cuenta en la foto de perfil

- **Decisión:** en las pantallas internas, Notificaciones es una campana (antes, un sobre) y la cuenta
  es una foto de perfil (una planta por defecto) que despliega un menú con el nombre y el rol, Mi
  perfil y Salir, como en K. Es un `<details>`: anda sin JavaScript. Mis donaciones pasa a la huerta
  de K (una lámina por donación).
- **Alternativas:** la cuenta a la vista en la cabecera, con el nombre y los dos enlaces (lo de antes).
- **Por qué:** lo pidió el usuario.
- **Costo:** el menú no se cierra al tocar afuera (sin JavaScript); se cierra con la misma foto.
- **Quién decidió:** usuario.
- **Evidencia:** —

### 2026-11-08 · direccion-general · Todo el sitio con la composición de la variante K

- **Decisión:** el sitio deja la composición de Raíces y toma la de K ("Huerta con abejas"): la
  portada en el orden de K (láminas con enjambre, cómo funciona en escalera, bajo tierra, cifras en
  árboles con las entregas del mes, quién participa, preguntas y cierre), el inicio del donante como
  el de K (cifras en fila, una lámina por donación, raíces, noticias y recorrido con la abeja), el
  pie de tierra con raíces y lema, el estado en pastillas, las noticias en renglones, los mapas con
  raíces en vez de líneas de subte y las cifras públicas en árboles. Salen los componentes de Raíces
  (frutos, libreta, red, planta, tierra, pasos y pausa); las piezas de K son el componente `huerta`.
- **Alternativas:** la piel de K sobre la estructura de Raíces (la entrada de arriba), que el usuario
  rechazó: "no mantuviste la estética de la variante k, adaptaste el estilo que estaba en el repo".
  Portada e inicio primero, o todo de una vez: eligió todo. Con las piezas de Raíces: reemplazarlas
  por las de K o dejarlas donde K no tiene equivalente: eligió reemplazarlas.
- **Por qué:** el usuario quiere la estética de K, no la de Raíces con otra piel.
- **Costo:** las raíces de la portada son una muestra de los tipos de entidad, no datos. La prueba
  de Nosotros que miraba la rama de frutos (`data-cosecha`) ahora mira las cifras en árboles. Las
  etiquetas de herbario (`data-filete="doble"`) ya no dibujan filete.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/piel-huerta/`](evidencia/piel-huerta/).

### 2026-11-08 · imagen · Ilustraciones de diseño: pasos, plantas, abeja y reintentar

- **Decisión:** el sitio usa las ilustraciones de la variante K (versión clara). La lámina «Cómo
  funciona» pasa a 4 pasos con sus dibujos (suma «Viaja a la entidad»); el inicio del donante suma
  «Lo que ya dio fruto», una planta por entrega confirmada según la categoría (componente `huerta`);
  la abeja duerme en los vacíos, vuela mientras algo carga y lleva la entrega en camino del mapa de
  seguimiento (antes, el camión); la maceta de reintentar va en los avisos y las páginas de error.
- **Alternativas:** plantas una por categoría donde se muestre una donación, o solo de adorno; abeja
  solo para esperas o solo para la logística; 3 pasos sin la maceta. El usuario eligió una planta
  por entrega confirmada, la abeja para las dos cosas y 4 pasos.
- **Por qué:** son las ilustraciones de diseño de K y el usuario pidió usarlas.
- **Costo:** la planta de las hojas al viento quedó sin uso. El vuelo de la abeja dura menos de 5 s
  (sin pausa); mientras algo carga, vuela en ciclo.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/piel-huerta/`](evidencia/piel-huerta/).

### 2026-11-08 · estilo · Piel Huerta: la estética de la variante K, sobre la estructura de Raíces

- **Decisión:** el sitio toma la piel de la variante K ("Huerta con abejas") de `frontend-donatrack`:
  paleta de K, títulos en Archivo bien ancho y texto en DM Sans (sale Fraunces), cabecera de papel
  con un filete fino, tarjetas de papel claro sin filete con sombra granulada riso, botón principal
  verde con sombra plana de hoja y los demás en contorno, esquinas de 16 y 10 px, y la parte
  destacada de un título derecha, sin segunda tinta. La estructura de cada pantalla no cambia.
  El verde de K (`#2f7d3b`) pasa a `#2a7035` y el naranja de las cifras de la planta vuelve a
  `#a8420a`: con los de K, el texto no llegaba a AA sobre los fondos de estado y el papel.
- **Alternativas:** vuelta 1 (2026-11-08), la variante `piel-huerta-a` contra lo de hoy en la portada
  y el inicio del donante; el usuario eligió la piel. Antes descartó "K completa" (abejas, láminas
  por donación y raíces de impacto) y "solo la paleta", y mantener Fraunces.
- **Por qué:** al usuario le gusta la estética de K y quiere llevarla a las pantallas armadas.
  Los paneles que no son `.tarjeta` (avisos, diálogos, fichas del mapa, etiquetas de la red y de
  entidades, cuadernos de novedades, Contacto y Crear una cuenta) pasan al papel claro sin filete
  con la elevación suave de K (`--sombra-papel`); el aviso de error y el de "hecho", con una franja
  de su color a la izquierda.
- **Costo:** la demora del donante queda con el rojo solo en su sombra plana, sin borde.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/piel-huerta/`](evidencia/piel-huerta/).

### 2026-11-07 · pantalla · F7 construida: Llegó con un problema, páginas de error y el recorrido sin "sin dibujar"

- **Decisión:** Llegó con un problema es el fragmento del acta con un parámetro más,
  `acta(conError, problema)`, y una hoja nueva, `entidad-seguimiento-problema.css`, que solo ubica
  «Qué pasó» debajo de las pruebas en pantalla grande. El formulario manda `motivo` y la vuelta trae
  `?problema=`, que suma «El problema» a lo asentado y quita la cifra del pedido. Las páginas de
  error son `templates/error/404`, `403` y `5xx` (Spring Boot las elige por el código), con una
  pieza compartida, `pagina-error`, y la hoja `publico-error.css`; en la maqueta,
  `?respuesta=404`, `403` o `500` en cualquier pantalla. El dibujo usa las piezas del bloque pasos;
  el viento de las hojas quedó limitado a `.paso`, porque fuera de la lámina se mecía sin fin y sin
  pausa (bloqueo de `revision-ui`). El recorrido de `SitioControllerTest` falla si una vista es
  `sin-dibujar`, y el piso visual acepta el código de `?respuesta=`.
- **Alternativas:** una plantilla propia para el problema (duplicaba el acta); dejar Mi perfil en el
  menú del admin con su "sin dibujar" (rompía el criterio de hecho).
- **Por qué:** lo mínimo que cumple las dos fichas aprobadas sin clases nuevas fuera del catálogo.
  Mi perfil salió del menú del admin porque el mapa del sitio lo da solo al donante y a la entidad.
- **Costo:** en pantalla grande queda aire entre el remito y la firma, y el foco recorre por
  columnas (avisos de `revision-ui`). Las fotos de una entrega con problema no se publican: se
  aparta del ADR del contrato de entregas (`proposed`), anotado en DTI-29.
- **Quién decidió:** agente, dentro de las fichas aprobadas por el usuario; Mi perfil del admin,
  agente, por el mapa del sitio.
- **Evidencia:** `SitioControllerTest` (44 tests, 6 nuevos o cambiados), módulo 69/69, `-Pvisual`
  de las rutas de F7 y `revision-ui` en modo pantalla, con el bloqueo corregido.

### 2026-11-07 · flujo · Fichas de F7: Llegó con un problema y páginas de error

- **Decisión:** [Llegó con un problema](flujos/raices-llego-con-un-problema.md) es el acta de
  Confirmar recepción con el grupo «Qué pasó» (motivo, cuánto llegó si llegó menos y un detalle);
  un botón confirma y reporta a la vez. Quien donó ve el motivo y el detalle; el equipo, todo. Las
  fotos de una entrega con problema no se publican. [Las páginas de error](flujos/raices-paginas-de-error.md)
  van con el marco público y un corte de suelo chico, con el código para soporte y Contacto.
- **Alternativas:** el reporte en una página aparte y la confirmación después (dos pasos); el
  detalle siempre obligatorio; solo el equipo ve el reporte; las fotos publicables como las demás;
  el marco de cada área en las páginas de error; solo texto, sin dibujo.
- **Por qué:** el contrato confirma y reporta en un solo envío; una foto de algo dañado no va a la
  portada; las páginas de error tienen que andar sin saber quién entró.
- **Costo:** se aparta de una consecuencia del ADR del contrato de entregas (la foto llega a la
  portada); queda para el grupo, con qué hace Donaciones con una cantidad menor.
- **Quién decidió:** usuario (2026-11-07), en tres rondas de preguntas; las fichas, aprobadas.
- **Evidencia:** `revision-ui` en modo flujo, con sus dos bloqueos corregidos antes de aprobar.

### 2026-11-07 · pantalla · F6 construida: Nosotros, Para donantes, Para entidades y Activar cuenta

- **Decisión:** las tres de lectura comparten una hoja nueva, `publico-conocer.css`, que solo dispone
  (relleno de tarjetas, ritmo de 40 px entre secciones, 48 px de toque en las preguntas) sobre
  `publico-paginas.css`; el resto son piezas del catálogo (`.pila`, `.grilla`, `.racimo`, `.tarjeta`,
  `.lista`, `.frutos`). Las cifras de Nosotros usan una variante nueva de `.frutos`,
  `data-cosecha="completa"`, porque las tres están cumplidas y el tercer fruto no debía leerse "en
  camino". Activar cuenta copia las piezas de Ingresar y Recuperar, marca "Ingresar" en la
  navegación y, con el enlace vencido, cambia el título a "Activá tu cuenta". Las preguntas
  frecuentes son `<details>` nativos.
- **Alternativas:** envolver cada sección en otra pila (más marcado para lo mismo); dejar el tercer
  fruto punteado (contradecía su significado).
- **Por qué:** lo mínimo que cumple la ficha aprobada sin clases nuevas fuera del catálogo.
- **Costo:** en 360 px el texto de cada categoría de Para donantes baja debajo del ícono; el
  `data-espacio` de la cabecera de las landings no rige entre rótulo, título y bajada (ganan sus
  márgenes, como en el resto del sitio). Avisos de `revision-ui`, sin corregir.
- **Quién decidió:** agente, dentro de la ficha aprobada por el usuario.
- **Evidencia:** `SitioControllerTest` (2 tests nuevos), `-Pvisual` de las 17 rutas de F6 y
  `revision-ui` PASS en la ronda 3.

### 2026-11-07 · flujo · Ficha de F6: Nosotros, Para donantes, Para entidades y Activar cuenta

- **Decisión:** una ficha para las cuatro públicas que faltan
  ([`raices-conocer-y-activar`](flujos/raices-conocer-y-activar.md)). Se enlazan desde el pie de
  todas las páginas y desde la portada; el menú público no cambia. "Landing donante" y "Landing
  beneficiario" se llaman "Para donantes" y "Para entidades". Nosotros conserva lo del boceto (pág.
  3): cifras, ahora con período y fecha; "Quiénes somos" con una lámina en lugar de una foto;
  "Nuestra solución" pasa a "Cómo trabajamos", sin "en tiempo real" ni "automático"; "¡Unirse a la
  Comunidad!" pasa a dos salidas, una por público. Activar cuenta sale del correo: elegir la
  contraseña, cuenta activada y, si el enlace venció o se usó, "Pedir otro enlace", que también está
  en Ingresar.
- **Alternativas:** Nosotros en el menú público (seis opciones en el celular); solo el pie;
  Nosotros sin cifras; Activar como un solo pedido de enlace, con la contraseña en el IdP.
- **Por qué:** el menú del celular ya está lleno; las cifras con fecha dan confianza; el correo es
  lo que la maqueta ya promete al registrar un donante en el depósito.
- **Costo:** las cifras esperan HB-02; los textos de Activar dependen de D12.
- **Quién decidió:** usuario (enlaces, Activar y cifras); agente (nombres y forma de cada página).
- **Evidencia:** revisión de la ficha con `revision-ui` en modo flujo.

### 2026-11-07 · pantalla · F5 construida: Misiones, Seguir entregas y Mi perfil de la entidad

- **Decisión:** la cuenta persona va en la sesión (`?cuenta=persona`, `?cuenta=empresa` vuelve) y
  `SitioController` le muestra un aviso (`aviso-persona`) en las otras pantallas del donante, sin
  tocar las de la empresa; su bandeja no muestra cuenta sin leer. Las insignias son el sello de
  Raíces (lleno la obtenida, hueco la que falta), no un pictograma propio por insignia. La barra de
  progreso es un bloque nuevo, `.progreso`, sobre el `<progress>` nativo. Seguir entregas de la
  entidad usa el bloque `.mapa-cuerpo` (hasta 6 líneas con resalte cruzado) y las piezas del dibujo
  del donante; sus cifras van debajo del dibujo, como leyenda, porque a la derecha va la lista (la
  ficha las ponía a la derecha). En el menú, la sección se llama "Misiones" (§2 de la ficha): con
  "Misiones e insignias" la cabecera desbordaba a 1100 px; el rótulo y el título dicen el nombre
  entero. Con 5 secciones, la barra del celular usa la letra angosta de la del admin.
- **Alternativas:** guardar la cuenta en la dirección de cada enlace (se pierde al usar el menú);
  un pictograma por insignia (más dibujo sin ficha que lo pida); la red del donante con sus clases
  propias (resalta solo 3 líneas).
- **Por qué:** lo mínimo que cumple las fichas aprobadas con piezas del catálogo.
- **Costo:** "Llegó con un problema" (Confirmar recepción) sigue en `sin-dibujar`; las insignias
  propias quedan para cuando exista el perfil público que las muestre.
- **Quién decidió:** agente, dentro de las fichas aprobadas por el usuario.
- **Evidencia:** `SitioControllerTest` y las capturas de `-Pvisual`.

### 2026-11-07 · flujo · Fichas de F5: Seguir entregas de la entidad, su perfil y Misiones

- **Decisión:** Seguir entregas de la entidad es la red radial del donante dada vuelta, con la puerta
  de la entidad al centro y una línea por entrega; confirmar sigue en su pantalla, de a una. Mi
  perfil de la entidad copia la forma del perfil del donante (Datos y Avisos en pestañas), sin
  "Cómo te ven" y con la dirección de entrega fija. Misiones e insignias muestra las 6 misiones,
  las de la categoría siguiente cerradas, con una barra de progreso y la cuenta en texto en lugar
  de la torta del boceto (pág. 18), y sin el detalle de misión (págs. 20 a 22). Para verla, una
  segunda cuenta de ejemplo, una persona (Martina Ruiz); con ella, las demás pantallas del donante
  muestran un aviso. Del boceto de la pág. 28 salen los botones "<Estado>" y "Reportar problema": el
  estado va en cada entrega y lo que llega con un problema se informa en Confirmar recepción
  ("Llegó con un problema"). Las misiones avanzan de a una, como en Incentivos: la siguiente de la
  categoría queda "en espera".
- **Alternativas:** una sola línea del depósito a la puerta; solo la categoría actual de misiones;
  un perfil de la entidad sin pestañas ni avisos; que la persona vea las pantallas de la empresa, o
  adaptar sus textos para las dos cuentas.
- **Por qué:** la red es la misma pieza del donante y cada entrega tiene su línea; la barra deja
  leer cuánto falta, y el detalle de misión ya entra en su tarjeta (un nivel menos); la dirección la
  revisa la administración al aprobar la entidad (RF-21, con la constancia de domicilio de la ficha del admin); el aviso no toca pantallas aprobadas de la
  empresa.
- **Costo:** las insignias pierden la ilustración en color del boceto (van como pictogramas
  impresos de Raíces); la persona de ejemplo solo ve Misiones.
- **Quién decidió:** usuario (la red, las 6 misiones, el perfil, la segunda cuenta y el aviso);
  agente (la barra en lugar de la torta, el detalle de misión dentro de la tarjeta, las insignias
  como pictogramas, la dirección fija y sacar los botones de la pág. 28).

### 2026-11-07 · flujo · Una solicitud que espera corrección no muestra acciones

- **Decisión:** mientras una entidad corrige su solicitud ("Esperando corrección"), se ocultan
  Aprobar, Pedir corrección y Rechazar; vuelven cuando la entidad la reenvía.
- **Alternativas:** dejar las tres acciones a la vista (la ficha no lo prohibía).
- **Por qué:** no hay nada que decidir hasta que la entidad corrija.
- **Costo:** ninguno en la maqueta; en el sistema depende del estado de la solicitud (HB-12).
- **Quién decidió:** usuario, a partir de un aviso de `revision-ui`.

### 2026-11-07 · vistas · Camiones y Ranking del admin (F4)

- **Decisión:** se construyen Camiones (la flota en la tabla adaptable con sus cifras y su fecha,
  "Agregar camión" plegado arriba, "Deshabilitar" en un diálogo con el motivo opcional y "Volver a
  habilitar"; "En ruta" sin botón) y Ranking (el último mes, "Ver otro mes" como formulario GET y
  el criterio con la fecha del cálculo arriba de la tabla), según la ficha
  [`raices-admin-navegable`](flujos/raices-admin-navegable.md) §4.8 y §4.9. Sin componentes ni
  estilos nuevos. Con esto, todo el menú del admin está dibujado.
- **Alternativas:** la ficha nombra solo los resultados de agregar y deshabilitar; se suman
  `admin-camiones-habilitado` ("Volviste a habilitar el camión AA 999 ZZ. Queda disponible para las
  próximas rutas.") y `admin-camiones-agregar-error` (capacidad y altura mayores que 0, como valida
  `CamionRequestDTO`), porque cada acción lleva su aviso (RNF-UI-05). En el diálogo, el foco entra
  en "Cancelar", como en F3; la ficha (§3 y §7) se ajustó a esto.
- **Revisión:** `revision-ui` PASS sin bloqueantes. De sus avisos, se arreglaron la hora de las
  cifras después de una acción (18:35), el motivo de ejemplo del camión deshabilitado y la ficha.
  El criterio del ranking dejó de nombrar el servicio ("según Incentivos"): ahora dice "Ordenado por la
  cantidad de misiones que cada donante completó en el mes." (decisión del usuario).
- **Por qué:** cumplir RF-18 y RF-19 con el mismo patrón lista → acción → aviso de F2 y F3.
- **Costo:** la maqueta muestra un motivo de ejemplo, no el escrito al deshabilitar (el POST
  vuelve sin lo escrito); el hueco de los deshabilitados y del motivo, en
  [`raices-ab-huecos-backend`](flujos/raices-ab-huecos-backend.md).
- **Quién decidió:** agente, dentro de la ficha aprobada.

### 2026-11-07 · vistas · Donaciones y Asignar donaciones del admin (F3)

- **Decisión:** se construyen Donaciones (lista con filtro por estado y "vencen en los próximos 7
  días", detalle con su historial, marcar como vencida en un diálogo, registrar en tres pasos) y
  Asignar donaciones (propuestas con Aprobar y Descartar, las decididas plegadas, correr la
  asignación a mano), según la ficha [`raices-admin-navegable`](flujos/raices-admin-navegable.md)
  §4.5 a §4.7. Sin componentes nuevos: `.tabla`, `.tarjeta`, `.dialogo` y el sello de estado.
- **Alternativas:** el filtro "vencen pronto" como casilla (la ficha lo dibuja como botón): no
  llegaba al tamaño de toque, así que es un `select` "Vence" junto al de Estado. El donante dado
  de baja del ejemplo es Pedro Ejemplo (la ficha decía Ana), para que coincida con la lista de
  Donantes. Correr la asignación vuelve a `admin-asignaciones-corrida`, una pantalla que la ficha
  nombra como estado pero no como página. En "Marcar como vencida", el foco entra en "Cancelar",
  como pide el catálogo para `.dialogo` con tono quitar (la ficha, §7, decía "al primer campo").
  El error del servidor de Registrar donación no tiene "Volver a cargar": dice "Probá de nuevo con
  «Registrar donación»", para no perder lo escrito (igual que Registrar donante).
- **Revisión:** `revision-ui` dio CHANGES_REQUIRED con 2 bloqueantes: los resultados de la búsqueda
  de donante (nombre accesible distinto del texto y 19 px de alto) y el detalle sin la lista de sus
  bienes. Los dos se arreglaron y se volvieron a comprobar con la verificación visual de las 29
  rutas de F3 y el test del controller; también la alineación de las filas, la fecha de la donación
  vencida y el sello neutro de las propuestas decididas.
- **Por qué:** cumplir RF-15 a RF-17 con el mismo patrón lista → acción → aviso de F2.
- **Costo:** "Sumar otro bien" recarga sin lo escrito en la maqueta (con Alpine, queda para
  después); en el inicio, "En el depósito" pasa de 3 a 7 (decisión de la ficha). Huecos de backend
  nuevos y la divergencia `donanteId`/`idDonante`, en
  [`raices-ab-huecos-backend`](flujos/raices-ab-huecos-backend.md).
- **Quién decidió:** agente, dentro de la ficha aprobada.

### 2026-11-07 · vistas · Donantes y Entidades del admin (F2), con la tabla adaptable

- **Decisión:** se construyen Donantes (lista, registrar, importar CSV con su avance y su resultado
  en la misma pantalla) y Entidades (pestañas Piden sumarse y Activas; aprobar, pedir corrección,
  rechazar y registrar), según la ficha [`raices-admin-navegable`](flujos/raices-admin-navegable.md).
  Las listas usan un bloque nuevo, `.tabla`: tabla desde 53,75 em y una tarjeta por fila en el
  celular ([catálogo](componentes.md)).
- **Alternativas:** las pestañas de Entidades como `?vista=` (la ficha) o como el bloque
  `.pestanas` con `#solicitudes` y `#activas`; "Quién dona" con Alpine o con un formulario aparte y
  "Cambiar".
- **Por qué:** el bloque `.pestanas` ya existe y anda sin JavaScript; "Cambiar" es lo que la ficha
  pide sin JavaScript, y alcanza.
- **Costo:** el cambio de campos en el momento con Alpine queda para después; sin él, "Cambiar"
  recarga y pierde lo escrito. Sin JavaScript, las pestañas no dicen cuál está activa a un lector de
  pantalla (`:target`, sin `aria-current`). El error de Registrar entidad muestra solo "Falta la
  dirección": "Una empresa no puede ser entidad" no se alcanza, porque el tipo no ofrece Empresa.
  "Descargar las filas con error" vuelve a la misma pantalla en la maqueta. La búsqueda de
  Donantes y Entidades no filtra en la maqueta. Los huecos del backend de F2 quedan en
  [`raices-ab-huecos-backend`](flujos/raices-ab-huecos-backend.md).
- **Quién decidió:** agente, dentro de la ficha aprobada por el usuario.

### 2026-11-07 · flujo · Admin en una sola ficha, con tres reglas de trabajo

- **Decisión:** el recorrido del admin va en una sola ficha,
  [`raices-admin-navegable`](flujos/raices-admin-navegable.md), con importar CSV y asignar adentro.
  Ante una entidad que pide sumarse, el admin aprueba, pide corrección o rechaza; corregir y
  rechazar llevan un texto que le llega a la entidad. El motivo para descartar una propuesta es
  opcional. En Camiones, el admin solo pasa entre disponible y deshabilitado.
- **Alternativas:** una ficha por sección, o fichas aparte para el CSV y para asignar; solo aprobar
  entidades; motivo obligatorio al descartar; que el admin cambie los tres estados del camión.
- **Por qué:** una ficha, como las de donante y entidad, mantiene el mismo patrón en las seis
  secciones. Pedir corrección evita rechazar a una entidad a la que le falta un papel. "En ruta" lo
  pone la logística, así no queda una ruta falsa.
- **Costo:** pedir corrección y rechazar se suman a HB-12, que todavía no existe en el backend.
  El motivo de descartar y volver a habilitar un camión necesitan huecos nuevos de backend
  (ficha, Huecos).
- **Quién decidió:** usuario. Además, el agente fijó dos cosas: el correo del donante es
  obligatorio en el registro, porque por ahí le llega su acceso (RF-15, D12), y el inicio del
  admin pasa a "7 en el depósito" en F3, porque solo pueden vencer las donaciones del depósito.
- **Evidencia:** la ficha, §4 y §9.

### 2026-11-07 · proceso · Roadmap de las vistas del enunciado: el admin primero

- **Decisión:** las vistas que faltan se terminan en 7 fases, según el
  [roadmap](roadmap-vistas-enunciado.md): primero el admin (ficha y tres tandas de pantallas),
  después entidad y donante, después lo público y al final las páginas de error y el cierre.
- **Alternativas:** empezar por lo público o por entidad y donante, que ya tienen casi todo.
- **Por qué:** el admin tiene 6 de los 20 requerimientos mínimos (RF-15 a RF-20) y ninguna
  sección dibujada.
- **Costo:** quedan afuera la autenticación real y los datos de las APIs. Hay tres decisiones
  abiertas, que se piden al empezar F1, F2 y F5.
- **Quién decidió:** usuario (alcance: los cuatro roles, sin auth); el orden de las fases es una
  propuesta del agente.
- **Evidencia:** la matriz requerimiento → vista del roadmap.

### 2026-11-06 · revisión del usuario (Raíces cálida): vuelve la versión de la etapa 1; de la corrida queda solo el estrato de las barras

- **Decisión:** el sitio vuelve a como estaba al cerrar la etapa 1 (`49b46ab4`: tierra humus, marco
  casi negro, esquinas suaves y Fraunces). De la corrida creativa se conserva solo el estrato: la
  línea de paja y la franja de humus donde la cabecera, el pie y la barra del menú tocan la página
  (`--color-estrato`, `--color-horizonte` y `--medida-estrato` en `tokens.css`).
- **Alternativas:** quedarse con el resultado de la corrida (el recorrido de concepto-b, las abejas
  y la herencia), descartado por el usuario.
- **Por qué:** lo dijo el usuario al ver el resultado: "Prefiero la versión de antes, lo único que
  quiero mantener del estilo nuevo son las lineas en las barras".
- **Costo:** se pierden el recorrido, la firma de las abejas, la planta por donación y la voz nueva
  de los títulos. Las dos entradas de abajo (iteraciones 1 y 2) y su evidencia en
  `evidencia/raices-calida-*` quedan como registro de lo que se probó. Reemplaza: las decisiones de
  las iteraciones 1 y 2 de la corrida (corrida creativa desatendida).
- **Quién decidió:** el usuario, 2026-11-06.

### 2026-11-06 · sistema (Raíces cálida) · Iteración 2: el camino de la donación pasa al sitio, con un marco de suelo y paja y la firma de dos abejas

- **Decisión:** las tres zonas de la iteración se quedan, porque en sistema son partes del mismo
  sitio y no compiten. (1) **recorrido:** el inicio del donante es el camino de concepto-b en tres
  tramos ("Lo que espera destino", "Lo que está viajando", "Lo que ya recibieron"), con una planta
  por donación sacada de los datos, un solo botón relleno ("Ver seguimiento"), cada cifra con el
  signo de su etapa y pegada a su frase, y el aviso de problema tomado de los datos. Está
  hecho como bloque del catálogo (`componentes/recorrido`). (2) **casa:** un estrato de paja y humus
  bajo la cabecera, la barra del menú de humus con nombres y la firma de la casa: dos abejas con su
  vuelo punteado, decorativas, de tamaño fijo, pegadas a la marca, una sola por pantalla y solo en
  cabecera, portada y estados vacíos. (3) **herencia:** el resto del sitio retoma el material y la
  voz sin cambiar su composición: frutos en lo entregado, raíces en "En tu puerta" y "Sin leer",
  títulos que encadenan con la ancla ("Conocé a quienes reciben", "A quién le llegó cada una") y los
  `role="status"` de los vacíos que no son vacíos reales. Las variantes de concepto se borran.
- **Alternativas:** elegir una sola zona y descartar las otras `[REJECTED]`. Rehacer la herencia con
  más dibujo `[REJECTED]`: el encargo fija la composición de las demás pantallas. Un solo orden de
  los tres tramos del celular con el dibujo antes de la acción `[REJECTED]`: sacaba "Ver
  seguimiento" de la primera pantalla de 360 × 640; se toma el objetivo con la acción antes del
  dibujo. Pasar la cabecera a humus, la cabecera de una sola línea y bajar la opacidad de las raíces
  de "Sin leer" `[REJECTED]`. Plantas por destino en el mapa de seguimiento, el vacío de Mis
  donaciones con el corte de suelo y corregir el calendario de la entidad: no se hicieron
  (ver Costo). Volver "Entidades, donantes y camiones" a "La red" `[REJECTED]`.
- **Por qué:** la ancla es la que mejor cuenta el mensaje ("lo que donás llega y se ve; detrás hay
  personas") y su patrón sirve para el resto: ponderado 420 de 500 contra 360 de la casa y 330 de la
  herencia. La casa ata el marco de noche de la etapa 1 al suelo y firma sin ruido. La herencia
  extiende la voz y el material con la disciplina de no tocar la composición. Sin `BLOQUEA` en las
  críticas y con el piso en `[]`.
- **Costo:** (1) la herencia queda tímida frente a la ancla: `entidad-inicio`, `admin-inicio` y
  `donante-donaciones` casi no cambian; la distancia es de la iteración de flujo. (2) Quedan
  anotados para el flujo: el mapa de seguimiento dibujado desde los datos con plantas, el vacío de
  Mis donaciones, "Confirmar recepción" en la primera pantalla de la entidad, el calendario único
  (el de la entidad dice 18:20 y 18:30 contra "hoy, 16:25") y el nombre del estado de una donación
  sin destino, que hoy se llama igual que el de una asignada. (3) Las fichas de `docs/interfaz/flujos/`
  (`raices-seguir-entregas`, `raices-entidad-navegable`, `raices-necesidades`) mostraban los títulos
  viejos: se actualizaron en los arreglos de la auditoría de cierre (ronda 1), junto con
  `raices-donante-navegable`, `raices-publico-navegable`, el brief (que suma la etapa 2) y
  `docs/ESTADO_DOCUMENTACION.md`. `docs/README.md` no cambia: solo enlaza a `interfaz/README.md`,
  que ya lista las carpetas de evidencia. (4) Sin `escenario=muchas`: el caso grande (30 plantas) se probó
  en el navegador, no en las capturas. **Reemplaza:** el inicio del donante de la etapa 1 (decidido
  por el usuario), los títulos y rótulos de las pantallas del donante, la entidad y el admin que se
  reescribieron con la voz nueva ("Tus entregas en curso", "Lo asignado a tu entidad", "Tus
  pedidos", entre otros, de la etapa 1 y de las entradas de Seguir entregas y Necesidades) y las
  variantes `concepto-a|b|c` de la iteración 1 (de la corrida).
- **Quién decidió:** corrida creativa desatendida; el usuario decide con el merge.
- **Evidencia:** `evidencia/raices-calida-sistema/` (360 y 1280 de cada ruta, con los estados vacío,
  error y lento del inicio del donante, y la primera pantalla de 360 × 640 de la ancla).

### 2026-11-06 · concepto (Raíces cálida) · Iteración 1: el inicio del donante es el camino de la donación, en tres tramos (concepto-b, El recorrido)

- **Decisión:** ganó concepto-b sobre `/donante-inicio`: la pantalla es el camino de la donación, del
  depósito a la entidad, en tres tramos ("Lo que espera destino", "Lo que está viajando", "Lo que ya
  recibieron"). Cada tramo lleva una planta por donación, su cifra apoyada en el suelo y su acción;
  quien dona está al principio y quien recibe, al final. En escritorio va de izquierda a derecha y en
  el celular baja. Se tomó de a el corte de suelo con raíces y el signo de "espera destino", y de c la
  cifra que sale del suelo y "Ver seguimiento" como único botón relleno. Abejas solo en la cabecera.
  Los tres conceptos quedan como variantes en la rama (`?variante=concepto-a|b|c`); el sitio no cambia.
- **Alternativas:** concepto-a, La lámina (una escena grande a todo el ancho, con el campo leído al
  revés, abejas sin pausa y el único bloqueo de piso): `[REJECTED]`, pero su corte de suelo pasa a b.
  concepto-c, Las cifras que crecen (el número es la punta del tallo): `[REJECTED]`, mezcla unidades
  y en el celular pierde la calidez; pasan a b la cifra desde la planta y el botón relleno. También
  descartadas: llenar el hueco de la primera columna con datos nuevos, duplicar "Ver seguimiento",
  subir las cifras en banda al celular, mover el tramo "viajando" al principio y sacar la tierra en
  el celular.
- **Por qué:** es el que mejor cuenta el mensaje ("lo que donás llega y se ve; detrás hay
  personas"): lo que donás va de vos a una entidad y se ve en qué tramo está; el problema vive en su
  tramo, junto a "Ver seguimiento". Ponderado 440 de 500, contra 365 de a y 340 de c. Su patrón (tramo
  con rótulo, título, cuenta, tierra y acción) sirve para seguimiento y Mis donaciones en la
  iteración 2.
- **Costo:** (1) en 360 × 640 "Ver seguimiento" no entra en la primera pantalla: la cabecera ocupa
  unos 190 px y el tramo del depósito llena el resto; queda para la iteración 2, que rehace la
  cabecera. (2) En 1280 quedan huecos bajo "Ver Mis donaciones" y "7 donaciones registradas". (3) La
  verificación con filtro de páginas falla `lasPantallasConMovimientoDejanSuVideoYRespetanReducirMovimiento`
  porque deja afuera la portada y `/estilos`; sin filtro pasa, y el piso (`reporte.json`) da `[]`. (4)
  Los arreglos de a y c no se aplicaron. No reemplaza nada: las variantes conviven con el
  inicio del donante actual (etapa 1, decidida por el usuario) hasta que la iteración 2 lleve el
  ganador al sitio.
- **Quién decidió:** corrida creativa desatendida; el usuario decide con el merge.
- **Evidencia:** `evidencia/raices-calida-concepto-donante-inicio/` (las cuatro capturas de a, b y c y
  del antes; estados vacío, error y lento de b en 360 y 1280).

### 2026-11-06 · color-y-acabado (Raíces) · La tierra humus, un paso más honda

- **Decisión:** B, Humus hondo: `--p-suelo` `#46301f` y `--p-suelo-medio` `#3b2819`. Reemplaza el
  humus de la entrada siguiente (`#5a3d28` y `#4d3322`); el marco casi negro no cambia.
- **Alternativas:** una vuelta, en la portada y Seguir entregas: A, Humus (control); B, Humus hondo;
  C, Café tostado (`#36251a`, la recomendada); D, Negro tibio (`#2a1f17`).
- **Por qué:** el usuario pidió la tierra más oscura; B lo es sin dejar de leerse marrón.
- **Costo:** ninguno pendiente.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/calida-etapa-1/tierra-*.jpg`.

### 2026-11-06 · color-y-acabado y tipografia (Raíces) · Tierra humus con marco de noche, esquinas suaves y títulos en Fraunces

- **Decisión:** E, etapa 1 de [Raíces cálida](encargos/raices-calida.md). La tierra humus en
  `--p-suelo` y `--p-suelo-medio` (`#5a3d28`, `#4d3322`) y el casi negro en el más hondo (cabecera, pie
  y cifras). Esquinas suaves (0,75 y 0,5 rem). Fraunces en títulos y marca; las cifras y los
  numerales, en Archivo con el token nuevo `--fuente-cifra` (22 reglas de números).
- **Alternativas:**
  - Vuelta 1, tierra: A, Huerta de noche (control); B, Humus; C, Kraft de día; D, Monte tibio (la
    primera D, Horizontes, se rehízo por parecerse a B). Letra: Fraunces, Literata y Young Serif.
    El usuario pidió una mezcla de A y B, como D, y eligió Fraunces.
  - Vuelta 2: E y F, la mezcla con esquinas suaves o rectas; eligió E (la recomendada).
- **Por qué:** suma calidez sin perder el negro que gusta para la empresa.
- **Costo:** se borraron Literata y Young Serif; Fraunces suma 82 KB. Falta la etapa 2 (la corrida).
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/calida-etapa-1/`.

### 2026-11-06 · proceso (raices-calida) · Encargo creativo: Raíces más cálida para quien dona solo, en dos etapas

- **Decisión:** el [encargo `raices-calida`](encargos/raices-calida.md), en la rama
  `E6_raices_calida`, para todo el sitio. Etapa 1: un A/B de tierra más tibia y clara (con el negro
  de hoy como control), esquinas suaves y serif cálida en los títulos. Etapa 2: una corrida de dos
  vueltas con la huerta de cada donante en `/donante-inicio`, las abejas como firma y más personas
  en la ilustración.
- **Alternativas:** dos registros, de día para la persona y de noche para la empresa (pedía un ADR
  y el doble de contraste que comprobar); solo las pantallas de la persona; ir bien lejos, que
  reabría las fichas aprobadas; las estaciones; las abejas atadas a un dato.
- **Por qué:** Huerta de noche gusta para la empresa, pero perdió la calidez que pedía la encuesta
  "Huerta con abejas" (calidez 25 y ganas de sumarse 15) y no atrae a quien dona solo.
- **Costo:** la empresa pierde el aspecto de Huerta de noche si en el A/B no gana el control.
- **Quién decidió:** usuario, con la encuesta de una integrante del equipo.
- **Evidencia:** el encargo.

### 2026-11-06 · proceso · iteracion-ab: modo exploración con eje de divergencia y vueltas en boceto

- **Decisión:** la skill `iteracion-ab` distingue exploración (buscar un aspecto nuevo) de ajuste
  (retocar uno elegido). En exploración:
  - cada opción declara su eje de divergencia; en paleta, qué color domina, claro u oscuro,
    temperatura o saturación, y no solo el tono de cada color en su rol;
  - desde la segunda vuelta, una opción sale de la familia elegida;
  - "cambio mínimo", superficie mínima y ponytail rigen el código, no la idea; la skill lee la
    intensidad del brief, y "Lo que evitamos" es un aviso;
  - las vueltas van en boceto: `-Dverificacion.boceto=true` captura solo a 1280 px, sin axe, toque,
    desborde ni anchos intermedios. El piso, la evidencia y la bitácora, una vez, para la elegida.
  Además: el tema `color-y-acabado` (color dominante, radios, bordes, sombras y grano juntos);
  una variante puede crear un token de uso nuevo para mover un rol que no lo tenía (como la sombra
  antes de `--color-sombra`); y las decisiones A/B van a la bitácora en 15 líneas o menos.
  Cambian la skill, su plantilla, el catálogo de temas (versión 2), la dirección de arte (versión
  4), el proceso (versión 3), las reglas del módulo, `VerificacionVisualTest`, una nota en el ADR
  de verificación visual (`proposed`) y una línea de `identidad-visual` y de `revision-ui`.
- **Alternativas:** constructores en paralelo con encargos opuestos; cargar la skill
  `frontend-design`; una ficha corta de exploración; tope de 7 vueltas. Quedan sin aplicar.
- **Por qué:** el 2026-11-05 las vueltas de solo paleta dieron opciones muy parecidas: la skill
  pedía cambiar solo tokens en la misma estructura, y la diversidad apareció recién al cambiar qué
  color domina y el acabado. Las reglas de minimalismo, pensadas para el código, recortaban la
  idea. Cada vuelta corría la comprobación completa (3 anchos, 6 intermedios, axe, video y otros
  tests), y el modo rápido se armó a mano.
- **Costo:** una opción en boceto puede llegar al usuario con un problema de contraste o de celular;
  se descubre al aplicar, y el arreglo tiene que conservar la idea. Las capturas de boceto no traen
  360 px.
- **Quién decidió:** usuario.
- **Evidencia:** las entradas del 2026-11-05 de esta bitácora (siete vueltas de paleta y acabado) y
  la versión anterior de la skill, `temas-ab.md` y `VerificacionVisualTest` en el historial de git.

### 2026-11-05 · paleta y acabado (Raíces) · Huerta de noche aplicada; "con problema" en negro y rojo vivo

- **Decisión:** Huerta de noche se aplica a todo el sitio, después de verla en la portada y en el
  inicio del donante. Cambia un solo tema de la variante: "con problema". Lleva el texto en tinta
  negra (`#17140f`), y el rojo vivo va en la sombra plana y en el fondo del botón de quitar
  apretado (`#ee3b24`) y en el ícono "!" (`#d12a17`). Cambios:
  - `tokens.css` lleva los valores de la variante y tres tokens nuevos: `--p-problema-sello` y
    `--color-problema-sello`, para el ícono, y `--color-sombra`, la sombra plana (paja). Los dos
    radios quedan en 0.
  - 19 sombras de papel o de verde pasan de `--color-hoja` a `--color-sombra`: botones, tarjetas,
    diálogos, banner, estado vacío, aviso breve, marca, mapa, red, novedades, láminas de ingreso y
    registro, y las pantallas que las repetían.
  - Nueve sombras siguen en hoja, porque van detrás de piezas que ya son paja, o que se vuelven paja
    al pasar el mouse, y una sombra paja no se vería: la sección actual del menú, el botón
    destacado del menú público, el contador de avisos, el punto de "sin leer", los números de los
    pasos, de los asientos y de las entregas, la etiqueta del fruto paja y el ejemplo inicial de
    ingreso.
  - La segunda tinta del título de la portada pasa a la sombra paja. Se borra la variante
    `paleta-noche.css`.
- **Alternativas:** para "con problema", sobre Huerta de noche:
  - la remolacha de la variante (`#86244c` sobre `#f1cfdc`): al usuario no le gustó para nada;
  - A, Tomate (`#b3261e` sobre `#fcd6cb`): el rojo de alerta de siempre. Era la recomendada;
  - C, Óxido (`#7a2e12` sobre `#efcfa8`): un rojo de tierra, el más tranquilo y el que menos alarma.
- **Por qué:** el usuario eligió B, la más de afiche. El rojo sale de la sombra, como las demás
  piezas, y es la única pieza con sombra roja. Además, no se confunde con la zanahoria de los
  frutos. En la portada y en el inicio del donante, la cabecera de tierra negra se lee bien: el
  paja de la sección actual y de los enlaces se destaca.
- **Costo:**
  - Los errores de formulario y el texto de "con problema" pasan a tinta negra. Lo marcan el ícono
    rojo, el borde y la sombra roja. El texto negro sobre el rojo vivo (el botón de quitar apretado)
    da 4,62:1: pasa AA, sin mucho margen.
  - La sombra paja sobre el papel de cera tiene poco contraste; el borde de tinta sigue marcando
    cada pieza.
  - Los títulos conservan la segunda tinta hoja; solo la portada pasa a paja.
  - Quedan algunos radios fijos de hojas de pantalla, fuera de los tokens.
  - El bloque de la red de la portada, alto para la animación de las raíces, queda como una gran
    superficie casi negra en una captura de página entera. Al bajar en el navegador, se ve la
    animación.
- **Quién decidió:** usuario.
- **Evidencia:** capturas a 1280 px de la portada y del inicio del donante, con y sin la variante,
  y de las tres alternativas de "con problema", mostradas al usuario y no guardadas. Verificación
  visual con contraste y axe en 360, 1280 y 1920 px, exit 0.

### 2026-11-05 · paleta y acabado (Raíces) · Huerta de noche: afiche verde sobre tierra casi negra, con la paja como luz

- **Decisión:** B9, Huerta de noche. Reemplaza a H, Huerta viva en cera, elegida antes en el día. Se
  borra su variante (`paleta-h.css`). Todavía no está aplicada: `tokens.css` no cambia. Queda como
  variante (`css/variantes/paleta-noche.css`, `?variante=paleta-noche`) para probarla en la portada
  y en una pantalla de donante. Cambian el color y el acabado:
  - papel de cera `#fbf2d3`, tinta `#17140f` y tinta suave `#4a4234`;
  - verde hoja honda en títulos y botones (`#2d6a1e`, al apretar `#1f4f14`), lechuga en los
    secundarios (`#d3e98a`) y hoja en la ilustración (`#86b832`);
  - la tierra casi negra, en tres pasos (`#2a2a1c`, `#222217` y `#17170f`);
  - raíces paja (`#f4c844`) y frutos zanahoria (`#f07a12`, con su segunda tinta `#a8420a`);
  - los estados de Huerta viva, como en H;
  - esquinas rectas: los dos radios por token (`--radio-lamina` y `--radio-boton`) en 0; quedan
    algunos radios fijos de hojas de pantalla;
  - sombras planas de paja detrás de botones, tarjetas, diálogos y avisos breves, y como segunda
    tinta del énfasis del título de la portada, como faroles sobre la tierra oscura. Van en la capa
    de los bloques, así que el botón apretado, el de quitar y el de la tierra conservan las suyas;
  - el mismo grano que el sitio actual.
- **Alternativas:** cuatro vueltas más, ahora de color y acabado (esquinas, sombras y grano), en la
  portada y solo en escritorio.
  - Cuarta: cuatro direcciones que cambian qué color domina. A, Invernadero: todo claro, tierra
    lechuga, sin sombras ni grano. B, Afiche de huerta: tierra casi negra, zanahoria en títulos y
    botones, esquinas rectas y sombras de tinta. C, Lechuga y remolacha. D, Panal y hoja: la tierra
    de miel. Al usuario le gustaron el fondo de A, aunque con pocos colores, y B, aunque el negro le
    pareció muy fuerte.
  - Quinta: A2 (en flor) y A3 (regado), sobre el fondo de A; B2 (tierra de monte) y B3 (humus), con
    la tierra menos negra. No gustó ninguna; de B prefirió la original.
  - Sexta: B con el color principal en verde. B4, verde hoja honda; B5, verde vivo con secundarios
    durazno; B6, botones verdes y el énfasis de los títulos en zanahoria. El usuario eligió B4 como
    base.
  - Séptima: tres acabados sobre B4. B7, dos tintas corridas (sombras zanahoria y más grano); B8,
    paquete de semillas (contornos gruesos y sombras grandes verde brote); y B9, huerta de noche. La
    recomendada fue B7.
- **Por qué:** el usuario eligió B9. Las vueltas de solo paleta daban alternativas muy parecidas. Al
  cambiar qué color domina y cómo se terminan las piezas, apareció un estilo propio: el afiche. El
  verde mantiene las plantas como protagonistas. La tierra oscura da la seriedad que pidió la
  encuesta, y la paja de las raíces suma calidez sin agregar un color nuevo.
- **Costo:**
  - El negro de la tierra domina la red de entidades y las cifras; falta verlo en una pantalla
    privada, donde la cabecera también es tierra.
  - Las sombras de paja sobre el papel de cera tienen poco contraste. Son decorativas: el borde de
    tinta sigue marcando cada pieza.
  - La variante no cambia la sombra verde hoja del banner, el estado vacío, el mapa, la red, las
    novedades ni las láminas de las páginas públicas de ingreso y registro.
  - Al aplicarla, `tokens.css` necesita los radios en 0, y las sombras pasan a la paja en cada
    componente. Hoy las sombras usan `--color-hoja` en las hojas de componentes: hará falta un token
    de sombra.
  - La variante pasa los 32 pares de contraste del test y 5 más (el mínimo es 5,43:1, medido con un
    script fuera del test), y la verificación visual completa de la portada con
    `?variante=paleta-noche` (contraste y axe en 360, 1280 y 1920 px; exit 0, sin capturas
    guardadas como evidencia).
  - Por pedido del usuario, se exploró rápido: un solo ancho en las comparaciones, y los documentos
    y la auditoría solo al elegir.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/acabado-huerta-noche/`, con la primera pantalla y las cifras de
  cada alternativa a 1280 px: `vuelta-4-*` (A, B, C y D), `vuelta-5-*` (A2, A3, B2 y B3),
  `vuelta-6-*` (B4, B5 y B6) y `vuelta-7-*` (B7, B8 y B9).

### 2026-11-05 · paleta (Raíces) · Huerta viva en cera: más saturada, con papel de cera y frutos zanahoria

- **Reemplazada** el mismo día por Huerta de noche (B9, entrada de arriba); su variante se borró.
- **Decisión:** H, Huerta viva en cera. Elegida para reemplazar a Oliva, todavía sin aplicar:
  `tokens.css` no cambia. Queda como variante (`css/variantes/paleta-h.css`, `?variante=paleta-h`)
  para probarla en la portada y en una pantalla de donante antes de aplicarla. Valores base:
  - papel de cera `#fbf2d3`, tinta `#1e2417` y tinta suave `#4b4a32`;
  - verde hoja honda en títulos y botones (`#2d6a1e`, al apretar `#1f4f14`), lechuga en los
    secundarios (`#d3e98a`) y hoja en la ilustración y las sombras (`#86b832`);
  - la tierra en tres oliva saturados (`#4c5a1f`, `#3d4a17` y `#2b3612`);
  - raíces paja (`#f4c844`);
  - frutos zanahoria (`#ef7a16`, con su segunda tinta `#9a4708`);
  - los estados de Huerta viva: pendiente `#ebe3c4` / `#4b4a32`, en camino `#f8e3a0` / `#6a4f05`,
    entregada `#d3e98a` / `#24521a` y con problema `#f1cfdc` / `#86244c` (remolacha).
- **Alternativas:** tres vueltas.
  - Primera, en el muestrario `/estilos`: A, Huerta en flor (verde hoja, girasol, tomate y rosa); B,
    Bosque y frutos (verde pino, mora y frambuesa); y C, Abejas (papel amarillo, miel y café de
    panal, desde los tokens que trajo el usuario). Al usuario le gustó la línea de A; el muestrario
    no servía para juzgar una paleta.
  - Segunda, en la portada, solo en escritorio: A y cuatro de la misma familia. D, zapallo y
    lavanda; E, regada (tierra verde agua y celeste); F, pétalos (raíces rosa y frutilla); y G,
    mediodía (la más clara). D fue la que más se acercó, sin convencer.
  - Tercera, desde la encuesta "Huerta con abejas" de una integrante del equipo. Ganó su paleta B,
    Huerta viva, con el papel de cera de su A, los estados y el verde oscuro de B, saturación 4 de
    5, verde y amarillo parejos y un registro menos infantil. Con esa base, D de control, H (la
    mezcla directa), I (tierra verde hoja honda) y J (sobria: tierra de monte y frutos tostados con
    sombra remolacha). La recomendada fue I.
- **Por qué:** el usuario eligió H. Oliva se veía apagada. Huerta viva sube la saturación sin
  volverse chillona, conserva las plantas y suma colores: lechuga, paja, zanahoria y remolacha. El
  papel de cera da calidez. La tierra oliva y los estados sobrios sostienen la seriedad que pidió la
  encuesta.
- **Costo:**
  - Falta probarla en la portada completa y en una pantalla de donante, y aplicarla en
    `tokens.css`. Al aplicarla hay que correr el test de contraste del sitio. La variante ya pasa
    los 32 pares de ese test y 5 más (el mínimo es 4,73:1), medidos con un script fuera del test,
    que no recorre las variantes.
  - La variante se conserva, aunque la skill `iteracion-ab` pide borrarla al decidir: el usuario
    pidió solo elegir y probar la paleta después en la portada y en una pantalla de donante. Se
    borra al aplicarla.
  - Por pedido del usuario, las comparaciones fueron solo en escritorio (1280 px), sin capturas de
    360 px. El celular se mira al probarla en las pantallas.
  - Con problema pasa de rojo óxido a remolacha: hay que mirar que se siga leyendo como problema.
  - Las otras respuestas de la encuesta quedan para sus propios temas: las abejas, la ilustración,
    una letra serif para los títulos y la huerta que cambia con los meses.
  - Para que una variante de estilo se vea en las páginas públicas, `layout-publico.html` carga
    ahora la hoja de la variante, como ya lo hacían la portada y el layout privado.
- **Quién decidió:** usuario, con la encuesta de una integrante del equipo.
- **Evidencia:** `evidencia/raices/paleta-huerta-viva/`.
  - Primera vuelta en el muestrario a 1280 px (`vuelta-1-*`): componentes y bloques, la actual y
    A, B y C.
  - Segunda y tercera en la portada a 1280 px (`vuelta-2-*` y `vuelta-3-*`): la primera pantalla
    y las cifras de cada una.

### 2026-11-05 · stack · Fase 3, B3-01 subtanda 3: la tierra, la red y la pausa son bloques y la portada solo dispone

- **Decisión:** última subtanda de `publico-portada.css`: tres bloques nuevos. El de la tierra, sin clase raíz (la
  parte de la superficie al suelo: `.tierra-superficie` con el título y `.tierra-bajada`, `.tierra-suelo` y `.tierra-interior`; la
  `section` no lleva clase propia), `.red` (quiénes reciben: el dibujo
  `.red-dibujo` con sus piezas, `.red-etiquetas`, `.red-etiqueta` con `--1` a `--7` y `--tuya`, `.red-sello`,
  `.red-pide`, `data-estado="sin-datos"`, y la escena fija `.escena`/`.escena-fija`) y `.pausa` (`.pausa-boton`,
  `.pausa-casilla`). La hoja queda con la disposición de la portada (la grilla, el lugar de la lámina y del sol,
  el aire de los botones chicos en la tierra y del aviso, y el tamaño del dibujo de la red, que le tiene que
  ganar a `.dibujo` de `app.css`, sin capa), la pausa que detiene el viento de los pasos y de la
  planta, y lo que la escena hace con la planta y el suelo. No declara colores, fondos, fuentes ni
  `@keyframes`: sale de `HOJAS_HEREDADAS` y el cerco la trata como una hoja de pantalla nueva.
- **Alternativas:** conservar `suelo`, `herbario` y `etiqueta` como nombres (descartada: `herbario` y
  `etiqueta` nombran piezas de la lista de entidades y de Confirmar recepción, y `suelo`, genérico, es parte de
  la tierra: `.tierra-suelo`); la escena
  como bloque propio (descartada: solo existe para la red); la regla de la pausa en su bloque (descartada:
  nombraría piezas de otros dos bloques; qué detiene es de la pantalla); `.herbario--sin-red` como clase del
  bloque (descartada: es un estado, va por atributo como en los demás).
- **Por qué:** cierra B3-01: la hoja de la pantalla más grande del sitio queda solo con disposición.
- **Costo:** el tope baja de 163 a 159 y salen 33 clases de la lista de heredadas (370 quedan; también `tierra`,
  que no tenía estilo). La hoja pasa de 695 a 146 líneas.
- **Quién decidió:** usuario (tsorren), 2026-11-04: qué piezas van en la subtanda 3 y que la hoja sale de la
  lista al terminarla. El agente decidió y queda para que el usuario confirme (el usuario pidió correr sin
  preguntar): los nombres de los tres bloques y de sus partes y `data-estado="sin-datos"`.
  **Cambios visuales:** ninguno. **Cambios de comportamiento:** ninguno. **Hallazgos en el camino:** la regla
  `.suelo--registro { border-top: 0 }` («las cifras siguen en la tierra, sin otra línea de tierra») no tenía
  efecto: perdía contra `.suelo`, declarada después con el mismo peso, y la línea de tierra de 4 px sí se ve
  sobre la rama de frutos. Una primera versión la hizo funcionar (`data-continua`) y subió 4 px todo lo de
  abajo; se quitó para no cambiar lo que se ve, y queda para que el usuario decida si esa línea va. Además,
  `.dibujo` de `app.css` le ganaba al tamaño 0 del dibujo de la red movido al bloque: el tamaño volvió a la
  hoja. En la entrada de la subtanda 2 se corrigió la redacción de las observaciones de la auditoría de la
  subtanda 1 (eran tres, resueltas con dos cambios), a pedido de su auditoría de cierre.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. El cerco con
  la portada fuera de `HOJAS_HEREDADAS` pasa (15/15). `-Pvisual` completo (56 pruebas verdes, `reporte.json`
  vacío; la base, una corrida completa sobre la subtanda 2) y, tras los dos arreglos, `-Pvisual` sobre `/estilos`
  y la portada con sus cuatro escenarios: 56 verdes, `reporte.json` vacío y 14 de las 16 capturas de la
  portada iguales a la base; las otras 2 difieren en una fila de 7 y 11 px que también cambia entre dos
  corridas del mismo código (ruido). Estados del test temporal (viejo dos veces y nuevo, con la tierra, la
  red, sus etiquetas con el puntero, la pausa con foco y marcada, y la escena): ninguna diferencia fuera del
  ruido.

### 2026-11-05 · stack · Fase 3, B3-01 subtanda 2: los pasos, la planta, la rama de frutos y la libreta del mes son bloques

- **Decisión:** segunda subtanda de `publico-portada.css`: cuatro bloques nuevos en `componentes/`, con su
  movimiento adentro. `.pasos` (la lámina «Cómo funciona»: `.paso`, `.paso-dibujo` con sus piezas
  `.paso-*`, `.paso-numero`), `.planta` (el brote con frutos: `.planta-dibujo`, `.planta-cosecha`,
  `.planta-sol`), `.frutos` (lo que está confirmado: `.frutos-interior`, `.frutos-cabeza`, `.frutos-rama`,
  `.frutos-aviso`) y `.libreta` (lo que viajó este mes: `.libreta-lista`, `.libreta-entrada` con
  `data-estado="en-camino"` y sus partes `.libreta-*`). Las piezas genéricas del dibujo (`.tallo`, `.hoja`,
  `.hoja--tinta`, `.raiz`, `.raicilla`, `.cruce`, `.rama`, `.fruto`, `.fruto-*`, `.tinta-corrida`) se nombran
  dentro de cada bloque. En la hoja quedan la pausa del viento (detiene dos bloques) y la escena. `.muestra`
  suma `data-ancho="pantalla"` para los ejemplos anchos de `/estilos`. Además, las tres observaciones de la
  auditoría de la subtanda 1, con dos cambios: la zona suelo del estado vacío exige los filetes (lo que también
  quita la dependencia del orden de los imports para centrar su botón) y la muestra del botón aclara el tono
  principal.
- **Alternativas:** conservar los nombres (`registro`, `viajo`, `cuaderno`, `asiento`, `ejemplar`, `cuadro`,
  `llamada`, `vineta`) como bloques (descartada: `registro`, `cuaderno`, `asiento` y `ejemplar` ya nombran
  otras piezas del sitio, y el handoff pedía nombres distintos); prefijar también las piezas del dibujo
  (`.paso-hoja`, `.planta-fruto-disco`; descartada: son las mismas piezas en dos dibujos y la pausa las
  detiene juntas); un bloque de título de sección para los `h2` de la portada (descartada por ahora: cada
  bloque lleva el suyo; se puede unificar cuando la tierra sea bloque, en la subtanda 3); el movimiento de
  cada pieza en la hoja (descartada: es parte del bloque, como en las pestañas).
- **Por qué:** cada una de estas piezas es una unidad con su dibujo, sus estados y su movimiento; en la hoja
  solo quedaba disposición propia de la portada.
- **Costo:** el tope baja de 171 a 163 y salen 38 clases de la lista de heredadas (403 quedan). Las clases de
  dibujo que comparten otras pantallas (`.raiz`, `.rama`, `.fruto`) siguen también como heredadas. La hoja
  pasa de 1.446 a 695 líneas y sigue en `HOJAS_HEREDADAS` hasta la subtanda 3.
- **Quién decidió:** usuario (tsorren), 2026-11-04: qué piezas van en la subtanda 2. El agente decidió y queda
  para que el usuario confirme (el usuario pidió correr sin preguntar): los nombres de los cuatro bloques y
  de sus partes, `data-estado="en-camino"` en la libreta y `data-ancho="pantalla"` en la muestra.
  **Cambios visuales:** ninguno en la portada. **Hallazgo resuelto en el camino:** el título de la rama de
  frutos va dentro de `.tierra` y heredaba sin querer de `.tierra h2` el `text-wrap: balance` y la sombra
  del `em`; la primera corrida lo mostró (la palabra «confirmado» sin su segunda tinta) y ahora el bloque los
  declara, así que se ve igual que antes.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. `-Pvisual`
  completo (55 pruebas más la temporal), con la corrida de la subtanda 1 como ruido: 573 de 588 capturas
  iguales y las 15 distintas eran `/estilos` (contenido nuevo) y el título de la rama de frutos (la sombra del
  `em`, corregida). Las muestras nuevas de `/estilos` fallaron axe la primera vez (el contraste de `.frutos`
  fuera de la tierra y una región con desplazamiento sin nada enfocable): `.frutos` declara su letra clara
  (la que ya heredaba en la portada) y la muestra lleva el botón del mapa. Después, `-Pvisual` sobre
  `/estilos` y la portada con sus cuatro escenarios: 56 pruebas verdes, `reporte.json` vacío y las 16 capturas
  de la portada iguales a la base. Estados (test temporal, viejo dos veces y nuevo): solo cambia el `outline`
  del `dl` de la rama, que no se ve (estilo `none`), y un estado apretado a 1920 px que también cambia entre
  corridas.

### 2026-11-05 · stack · Fase 3, B3-01 subtanda 1: la portada usa la tarjeta, el diálogo, el estado vacío, el botón y dos tokens

- **Decisión:** primera de las 3 subtandas de `publico-portada.css`, que solo reusa bloques existentes. La lámina
  «Cómo funciona» es `.tarjeta[data-filete="doble"]` con una cabeza nueva del bloque, `.tarjeta-cabeza` (el
  título en cursiva y su rótulo sobre un filete fino); `ficha` queda como gancho de su disposición. La foto de
  una entrega abre un `<dialog class="dialogo" popover>` con `.dialogo-titulo`. Los avisos de la tierra y del
  cuaderno del mes (vacío, cargando y error) son `.vacio[data-forma="filetes"]` (sin papel, entre dos filetes)
  y, en la tierra, además `data-zona="suelo"`. Los botones propios son variantes de `.boton`:
  `data-forma="foto"` (la miniatura dentro del botón chico) y `data-zona="suelo"` y `"suelo-hondo"` (el chico
  sobre la tierra y la tierra honda; «Sumar mi entidad», con el tono principal, lleno de paja); el del mapa
  solo tenía su lugar y lo toma por estructura (`.registro-cabeza > .boton`). El título y la bajada usan
  `--tamano-titulo-portada` y `--tamano-bajada-portada` en `.hero h1` y `.hero-bajada` de `app.css` (solo la
  portada los usa) y se va la clase `portada`. `/estilos` suma las muestras y `.muestra` un fondo de tierra
  (`data-fondo`) para las que van sobre ella.
- **Alternativas:** dejar la sombra de la lámina en 0,45 rem con un token `--sombra-lamina` (descartada por el
  usuario: menos de 1 px, y la hoja seguiría declarando una sombra); la foto sin fundido, con una variante del
  diálogo (descartada por el usuario); el aviso del cuaderno con el papel del estado vacío (descartada por el
  usuario: cambiaba los tres estados y el «Cargando…» volvía a ser una lámina, lo que la tanda 5 sacó en las
  páginas públicas); una variante `data-forma` para el botón del mapa (descartada: su regla era solo de
  lugar); los tokens del título usados desde la hoja de la pantalla (descartada: el título ya vivía en
  `.hero h1` de `app.css`, que solo usa la portada, y la regla de la hoja lo pisaba).
- **Por qué:** la fase 3 saca de la hoja de pantalla todo lo que no es disposición, y estas piezas ya tenían
  un bloque igual o casi igual en el catálogo. Además se borró `.suelo a`: sin capa, le habría ganado al color
  de las variantes del botón, y para los enlaces de las etiquetas ya estaba pisado por el suyo.
- **Costo:** el tope de clases de un solo uso baja de 172 a 171 (`foto-titulo` y `boton--mapa` tenían una sola
  mención y `hero-bajada` pasa a tenerla, solo en `app.css`). Salen 9 clases de la lista de heredadas (441
  quedan). La hoja todavía no sale de `HOJAS_HEREDADAS`: le faltan las subtandas 2 y 3.
- **Quién decidió:** usuario (tsorren), 2026-11-04: la sombra a 0,4 rem, el fundido del diálogo y el aviso del
  cuaderno con sus dos líneas; antes, el corte en 3 subtandas y qué va en cada una. El agente decidió y queda
  para que el usuario confirme: los nombres (`.tarjeta-cabeza`, `data-forma="filetes"`, `data-forma="foto"`,
  `data-zona="suelo"` y `"suelo-hondo"`, `data-fondo`, `--tamano-titulo-portada` y `--tamano-bajada-portada`) y
  borrar `.suelo a`. **Cambios visuales:** la sombra de la lámina «Cómo funciona», 0,8 px más corta (pedido
  del usuario). **Cambios de comportamiento:** la ventana de la foto aparece y se va con un fundido (sin él
  con "reducir movimiento") y, al abrirse, el foco entra en ella (antes quedaba en el botón; el navegador lo
  devuelve al cerrar, como antes); al apretar los botones chicos de la tierra, su sombra se esconde como en
  los demás botones (antes quedaba).
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. Estados en un
  test temporal de Playwright (borrado) sobre el código viejo (un worktree en `HEAD`, dos corridas) y el nuevo,
  a 360, 1280 y 1920 px, en reposo, con foco, con el puntero y apretados, más el diálogo abierto y cerrado:
  entre las dos corridas viejas solo cambia «Sumar mi entidad» apretado a 1920 px; contra el nuevo, solo la
  sombra de la lámina, el fundido y el foco del diálogo y la sombra apretada de los tres botones chicos de la
  tierra. `-Pvisual` (55 pruebas verdes, más la temporal), con una corrida de ruido como base, las dos sobre
  el código sin tocar: 569 de 588 capturas iguales; difieren `/estilos` (3, contenido nuevo) y la portada en
  sus cuatro escenarios (16, la sombra de la lámina; recorte lado a lado revisado). `reporte.json` vacío.

### 2026-11-04 · stack · Fase 2, tanda 5 del backlog: cuaderno de noticias del donante y dos hallazgos de la fase 3

- **Decisión:** se implementa B2-14 recortado y se resuelven dos hallazgos que la tanda 2 había dejado para la
  fase 3. **Noticias:** `.noticias` y `.noticia` (`componentes/noticias.css`) son el cuaderno del donante en
  Inicio, Notificaciones y el detalle de la donación (y la pieza `datos-donante :: aviso`): una lista que
  cuelga de una línea impresa por fecha, con la estación impresa si no se leyó y hueca si se leyó.
  `data-forma="libro"` es la variante de las leídas de Notificaciones (desde la tableta, un renglón con columnas)
  y `span.noticia-texto` es el texto sin enlace (reemplaza a `noticia--simple`); la franja de tierra de las sin
  leer y las dos columnas del detalle siguen en las hojas de esas pantallas. **«Cargando…» públicos:** los de
  Entregas, Entidades y Mapa son `.cargando` con `role="status"` y no `.banner`. **Botón:** `.boton` toma el
  color de los enlaces en su base, así que el `<button>` secundario de las privadas deja el negro del navegador
  y es verde como en las públicas; se sacan los dos `:where(button).boton` que lo parchaban. Nota de remisión
  en el ADR de origen hacia el ADR sucesor `proposed`.
- **Alternativas:** migrar también `asiento`, `novedad` y `libro` (descartada: `asiento` nombra tres piezas con
  disposiciones distintas, el cuaderno de entregas recibidas de la entidad, el de la portada y el libro de
  donaciones, y `novedad` y `libro` son de una sola pantalla; el mismo criterio que dejó `acta` y `salidas`);
  subir el tope a 176 (descartada: el tope nunca sube; mover la base de `.noticia*` al bloque dejaba tres
  clases de noticia con una sola mención en las hojas de pantalla, y `.cargando` sumaba una cuarta); mantener `noticia--simple` como clase heredada (descartada:
  era una clase de un solo uso y el bloque la resuelve por el elemento); `.publica .cargando` en `publico-listas.css`
  (descartada: sumaba una clase de un solo uso; se usa `[role="status"]`); `.cargando` con una variante que
  conserve la lámina (descartada: `.banner` y `.cargando` quedan con un significado cada uno); dejar el negro
  del navegador en el botón y pintarlo solo en las públicas (descartada: dos botones iguales con dos colores).
- **Por qué:** las noticias son lo único de B2-14 que usan varias pantallas; el libro de registro y el texto
  sin enlace son dos disposiciones del mismo cuaderno y pertenecen al bloque; los «Cargando…» son indicadores
  de carga, no avisos que afecten a la pantalla; y el botón con el texto negro era un hallazgo (no un
  diseño) de la tanda 1.
- **Costo:** el tope de clases de un solo uso no baja (172 antes y después): el aporte de este ítem es sacar
  `noticia--simple` y 8 clases de la lista de heredadas, pero la base que se mudó dejó otras tres clases con
  una sola mención, y absorberlas en el bloque lo devolvió a 172. `asiento`, `novedad` y `libro` siguen
  heredadas. Las noticias en `bloques` pierden contra cualquier regla sin capa que las toque: la franja de tierra de las sin leer en `donante-notificaciones.css` y las dos columnas del detalle les ganan a propósito, y las capturas confirman que nada más cambió.
- **Quién decidió:** usuario (tsorren), 2026-11-04: confirmó los nombres de la tanda 4; pidió la nota de
  remisión en el ADR de origen (opción B); aprobó pasar los «Cargando…» públicos a `.cargando` y unificar el
  botón secundario al verde; y pidió no hacer push. El agente decidió y queda para que el usuario confirme: el
  recorte de B2-14 (solo `.noticias`), los nombres (`.noticias`, `.noticia`, `.noticia-marca`, `.noticia-estado`,
  `.noticia-texto`, `.noticia-cuando`, `.noticia-donacion`, `data-forma="libro"`, `span.noticia-texto`) y que el
  tope no baje. **Cambios visuales por confirmar:** en las pantallas privadas, el texto del botón secundario
  pasa de negro a verde (la marca «Marcar todas como leídas» de las notificaciones, «Filtrar» de las entregas
  recibidas y otros); y los «Cargando…» de Entregas, Entidades y Mapa públicos dejan de ser una lámina con
  borde y sombra y son texto en negrita. **Cambio de comportamiento por confirmar:** ninguno.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. Estados en
  un test temporal de Playwright (borrado) sobre el código viejo (un worktree en `HEAD`, dos corridas iguales)
  y el nuevo, a 360 y 1280 px, en reposo, con foco y con el puntero: las noticias de Inicio, Notificaciones y el
  detalle no cambian; el botón secundario cambia solo el color del texto (de negro a verde) y su contorno de
  foco; el «Cargando…» de las tres públicas pasa a texto sin papel ni borde con margen 0. `-Pvisual` (55 pruebas verdes), con una corrida de ruido como base, las dos sobre el código sin tocar: 550 de 588 capturas iguales; difieren `/estilos` (3, contenido nuevo), los tres «Cargando…» públicos «lento» (12, el tamaño cambia por la lámina que ya no está y las de 360 × 640 por el texto), el texto de «Marcar todas como leídas» y de «Filtrar» (donante-notificaciones, entidad-notificaciones y entidad-recibidas, 20 capturas: cajas de unos 233 × 13 px y 53 × 13 px, el texto del botón) y 8 a 16 px en la portada (sin ninguna clase tocada; antes y ruido ya difieren allí). `/estilos` se volvió a comprobar con la muestra final (axe AA, toque y desbordes: verde).

### 2026-11-04 · stack · Fase 2, tanda 4 del backlog: subida de fotos con vista previa, pestañas, piezas de la entidad y mapa con su lista

- **Decisión:** se implementan B2-10 a B2-13. **Subida de fotos:** `.subida` (`componentes/subida.css`) es el campo
  de fotos de Confirmar recepción: una zona punteada con el control nativo de archivos adentro (lo esencial:
  sin JavaScript manda lo elegido) y, con Alpine (`subidaFotos`), la vista previa de cada imagen (miniatura,
  nombre y «Quitar»), un anuncio con la cuenta (`role="status"`) y el foco de vuelta al control; los
  archivos quedan fuera del estado reactivo y «Quitar» rearma el control con `DataTransfer`. **Pestañas:**
  `.pestanas` (`componentes/pestanas.css`), las del perfil del donante. Sin JavaScript siguen siendo enlaces
  con `:target` (un solo aspecto, elegido por lugar hasta 6 pestañas); con JavaScript (`pestanas`) la raíz
  lleva `data-interactivo`, se agregan los roles `tablist`, `tab` y `tabpanel`, se recorre con ← → Inicio Fin
  y la pestaña queda en la dirección. Cada formulario del perfil quedó dentro de un `div.pestanas-panel`
  con el `id` de la sección. **Piezas de la entidad:** `.novedades` (`componentes/novedades.css`) y
  `.cabecera-entidad` (`componentes/cabecera-entidad.css`) pasaron de `entidad-comun.css` a sus bloques, y el
  `.hueco` de la entidad es ahora `.esqueleto` con `data-forma="hueco"` y `data-tamano="bajo"` (8 usos en 4
  plantillas). **Mapa con su lista:** `.mapa-cuerpo` (`componentes/mapa.css`) trae el marco del dibujo, las
  dos columnas, la lista de fichas y el resalte cruzado; el dibujo de la pantalla (`.estacion-numero`,
  `.mapa-fruto`, el rótulo y el esqueleto del mapa) queda en `publico-mapa.css`. El tope de clases de un solo
  uso baja de 186 a 172 y las heredadas, de 478 a 459 (19 menos: 6 sin uso, 12 ya en la capa CUBE y `avisos`,
  que la tanda 2 dejó en las dos listas). Se agrega B2-14 (el cuaderno con línea de tiempo) como propuesto y
  un ADR sucesor `proposed` para dos frases de D31 y D33.
- **Alternativas:** una sola hoja `entidad.css` para las piezas de la entidad (descartada: son tres piezas sin
  relación entre sí; un bloque por hoja es la convención); pestañas con roles fijos en el HTML (descartada: sin
  flechas serían una promesa falsa, así que los roles los pone Alpine solo si hay JavaScript); pestañas sin
  `:target` y todos los paneles apilados sin JavaScript (descartada: cambiaba lo que se ve sin JavaScript);
  `role="tabpanel"` en el propio formulario (descartada: `form` no admite ese rol, así que se agregó el
  contenedor); un `<template x-for>` o `<ul>` servido por el servidor para la vista previa (descartada: la
  lista son archivos de la persona, que el servidor no conoce); `.tarjeta` para la ficha de la entidad del
  mapa (descartada: su sombra mide 0,35 rem y la de la tarjeta, 0,4 rem); mover el dibujo del mapa a la
  capa de bloques (descartada: compite con `.dibujo-rotulo` de `sellos.css` y con el `.esqueleto` de las
  páginas públicas, y una regla sin capa le gana a una con capa); migrar `.salidas` y `.acta` (descartada: las
  páginas públicas tienen su `.salidas` con otra disposición, y `acta` es también la grilla de Confirmar
  recepción).
- **Por qué:** el campo de fotos, las pestañas, el acceso a novedades, la cabecera y el mapa con su lista eran
  piezas completas dentro de hojas de pantalla; D31 pide para Alpine justamente la vista previa de fotos y las
  pestañas, y el reparto «HTMX habla con el servidor, Alpine con lo que no sale del navegador» se cumple: nada
  de esto pide nada al servidor. Las pestañas con `:target` seguían funcionando sin JavaScript pero no
  se podían recorrer con teclado, que es lo que pide el patrón de pestañas.
- **Costo:** `acta` y `salidas` siguen en `entidad-comun.css`; el dibujo del mapa, en `publico-mapa.css`; la
  barra de pestañas del perfil elige la marcada por lugar y admite hasta 6 (como el resalte del mapa); en los
  escenarios con error de Confirmar recepción, el control de fotos recibe el color de borde del error con ancho
  0 (no se ve; el cálculo cambia por la capa); la vista previa muestra solo imágenes (el resto se manda igual y
  el servidor lo valida); la ficha del mapa no usa la tarjeta. Hallazgo del test de comportamiento: dentro de un
  manejador de Alpine, `this.$el` es el elemento que lleva la directiva y no la raíz del componente (arreglado
  guardando la raíz al iniciar; queda anotado en el código).
- **Quién decidió:** usuario (tsorren), 2026-11-04: hacer B2-10 a B2-13 en esta tanda; confirmó los nombres y
  las decisiones de la tanda 3 (navegación, tarjeta, lista, `data-zona`, `data-filete`, `--espacio-relleno`,
  `--medida-columna`, las 10 piezas migradas a la tarjeta, no construir la tabla y mover `.tierra-impresa` y
  `:focus-visible` de capa); agregar B2-14 al backlog; y el ADR sucesor `proposed` para D31. El agente decidió y
  queda para que el usuario confirme: los nombres (`.subida` y sus piezas, `.pestanas`, `.pestana`,
  `.pestanas-panel`, `data-interactivo`, `.novedades`, `.cabecera-entidad`, `data-forma="hueco"`,
  `data-tamano="bajo"`, `Alpine.data("subidaFotos")` y `Alpine.data("pestanas")`); el `div.pestanas-panel` alrededor
  de cada formulario; los roles de pestañas agregados por Alpine; que `acta` y `salidas` queden; y dejar el dibujo
  del mapa en su hoja. **Cambios visuales por confirmar:** ninguno. **Cambio de comportamiento por confirmar:** con
  JavaScript, elegir una pestaña del perfil ya no mueve la página hasta el panel (antes era un enlace a un
  ancla) y se recorre con las flechas; sin JavaScript anda como antes.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md), DTI-34 y el
  [ADR sucesor](../adr/cliente-liviano/20261104-dialogo-con-popover-y-fragmentos-solo-donde-hay-estructura-en-el-cliente-liviano.md).
  `-Pvisual` (55 pruebas verdes) con una corrida de ruido como base, las dos sobre el código sin tocar: 584 de
  588 capturas iguales; `/estilos` (3) cambió por el contenido nuevo y la portada «lento» a 360 px difiere 15
  px (no usa ninguna clase tocada, y antes y ruido ya difieren 7 px en esa misma captura). Estados, en un test
  temporal de Playwright (borrado) sobre el código viejo (un worktree en `HEAD`), el nuevo y otra corrida del
  viejo como ruido, con y sin JavaScript, a 360 y 1280 px: 140 capturas iguales (hover y foco por teclado de la
  zona de fotos, el botón y las pestañas; las pestañas con `#datos`, `#como-te-avisamos` y tras guardar; los
  accesos a novedades, los esqueletos «lento» de Inicio, Recibidas, Notificaciones y Administración, y el mapa
  con una ficha elegida), y los estilos calculados iguales salvo la estructura nueva (el contenedor de pestañas y
  los paneles sin `id`) y el color de borde invisible de la zona de fotos. Comportamiento (test temporal,
  borrado, 4 pruebas): elegir dos imágenes muestra dos miniaturas, el anuncio «2 fotos elegidas» y pasa axe AA;
  «Quitar» deja una, el control manda solo esa y el foco vuelve a él; sin JavaScript el control queda y la vista
  previa no aparece; las pestañas cambian con clic, ← → (con vuelta), Inicio y Fin, con `tabindex` rotativo,
  roles y axe AA, y llegar con `#sección` abre la pestaña; sin JavaScript se ve una sección por vez con `:target`
  y la pestaña marcada. Tras la auditoría de cierre se ajustaron tres detalles (el anuncio de la vista previa pasó a una región
  viva siempre presente, `.subida-aviso`, solo para lectores de pantalla; el título de la lista del mapa y el nombre de
  cada entidad aceptan dos niveles de título, y el nombre se achica en vez de desbordar) y se repitió `-Pvisual` en
  `/estilos`, el mapa, Confirmar recepción, el perfil y la portada (4 pruebas verdes, 12 capturas iguales a las de
  antes) y el test de comportamiento (4 verdes).

### 2026-11-04 · stack · Fase 2, tanda 3 del backlog: navegación, tarjeta y lista adaptable

- **Decisión:** se implementan B2-06, B2-07 y B2-08. **Navegación:** la barra de secciones de cada rol (con
  su hoja "Más", un popover) y la de las páginas públicas son un solo bloque, `.navegacion`
  (`componentes/navegacion.css`), con `data-zona="publica"` para la pública y las piezas `.navegacion-mas`,
  `.navegacion-hoja` (+ `-cabecera` y `-titulo`), `.navegacion-largo`, `.navegacion-aparte` y
  `.navegacion-destacado`. Dónde va dentro de la cabecera (el orden y el ancho de la fila) sigue en
  `privado.css` y `publico-navegacion.css`: es disposición de esa cabecera. **Tarjeta:** `.tarjeta`
  (`componentes/tarjeta.css`) es solo el papel (filete, esquina suave y sombra plana salvia), con
  `data-filete`: simple, `doble` (etiqueta de herbario) y `contorno`; el relleno (`--espacio-relleno`) y la
  disposición los pone la hoja de cada pantalla. La piden 10 piezas de 8 hojas (`lamina` del donante,
  `acta`, `etiqueta`, `ejemplar`, `etiqueta-datos`, `foto`, `decision`, `pedido`, `siguiente` y la `lamina`
  pública), que dejaron de repetir entre 4 y 6 declaraciones. **Lista adaptable:** `.lista` (`componentes/lista.css`)
  reinicia la lista y reparte los elementos en columnas de al menos `--medida-columna` (una sola sin ella);
  la usan `laminas`, `actas`, `ejemplares` y `ejemplares-reserva`. El tope de clases de un solo uso baja de
  189 a 186 y las heredadas, de 487 a 478.
- **Alternativas:** una tarjeta con relleno, título y disposición propios (descartada: cada una de las 10
  piezas tiene otro relleno y otra estructura, y unificarlos cambiaba lo que se ve); `.grilla` para las
  listas (descartada: usa `auto-fit` y estira los pocos elementos, y no reinicia la lista); variantes de la
  barra pública con custom properties (descartada: el cerco solo deja `var()` de tokens de uso; se resolvió con
  `:where()` para que pesen lo mismo que la base); un fragmento Thymeleaf para el menú privado (descartada:
  `layout-privado` ya lo arma con el modelo y no se repite); migrar también la cabecera (`.armazon`, la
  cuenta y el acceso a Notificaciones; descartada: no era parte del ítem).
- **Por qué:** la barra era la misma en dos hojas con diferencias de tres propiedades; el papel de la
  tarjeta salía de 10 lugares con un mismo valor de tinta, esquina y sombra; el reinicio y la grilla de las
  listas se repetían en cuatro. Una regla sin capa le gana a una con capa, así que la barra (fija) no podía
  ganarle a `.tierra-impresa` (`position: relative`) ni al anillo de foco sin capa: la textura pasó a la capa
  `utilidades` y el anillo de foco (`:focus-visible`, y el de paja sobre la cabecera y el pie), a `pico`.
- **Costo:** **la tabla no se hizo:** ninguna plantilla usa `<table>`, así que no había nada que migrar (el
  catálogo lo dice). Siguen como heredadas las clases gancho de las 10 piezas (relleno y disposición), las
  piezas con línea de tiempo (el cuaderno de entregas recibidas con `asiento`, `noticias` y `libro`, que
  repiten una estructura propia; se propone un ítem B2-14) y otras láminas de una sola pantalla
  (`.confirmado`, `.puerta`, `.grupo-avisos--sin-leer`, `.estacion-ficha`, `.etiqueta-entidad`). La cabecera
  `.armazon` sigue en `privado.css`. En `/estilos` las barras de ejemplo van dentro de una caja con
  `transform` (para contener lo fijo) y, en el celular, con ancho mínimo y desplazamiento de costado (si no,
  cada celda mide 46 px y falla el toque de 48).
- **Quién decidió:** usuario (tsorren), 2026-11-04: hacer B2-06, B2-07 y B2-08 en esta tanda. El agente
  decidió y queda para que el usuario confirme: los nombres (`.navegacion` y sus piezas, `.tarjeta`, `.lista`,
  `data-zona`, `data-filete`, `--espacio-relleno`, `--medida-columna`); que `data-filete` tenga tres
  valores; las 10 piezas migradas a la tarjeta; el alcance de la lista (grillas de tarjetas, no el cuaderno);
  no construir la tabla; la cabecera fuera; mover la textura `.tierra-impresa` a la capa `utilidades` y el
  anillo de foco a `pico`; y quitar `cursor: pointer` del botón "Cerrar" de la hoja (ya lo da `.boton`).
  **Cambios visuales por confirmar:** ninguno.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. `-Pvisual`
  (55 pruebas verdes) con una corrida de ruido como base, las dos sobre el código sin tocar: 583 de 588
  capturas iguales; `/estilos` (3) cambió por el contenido nuevo y la
  portada (2) difiere 7 y 19 px en las mismas filas que entre dos corridas del mismo código. Hover, foco por
  teclado y hoja "Más", en un test temporal de Playwright (borrado) sobre el código viejo (un worktree en
  `HEAD`), el nuevo y otra corrida del viejo como ruido: 83 estados con los estilos calculados idénticos y
  55 capturas iguales (la barra de Donante, Administración y Mapa a 360, 768 y 1280 px y de la portada a
  360 y 1280 px; las tarjetas de Necesidades, Recibidas, Inicio de la entidad, Entidades, Inicio del donante
  y Entregas confirmadas con el foco puesto), salvo 2
  px que también difieren entre las dos corridas del código viejo. `/estilos` con las barras, las tarjetas y la
  lista pasa axe AA, el toque de 48 px y sin desbordes en 360, 1280 y 1920 px.

### 2026-11-04 · stack · Fase 2, tanda 2 del backlog: aviso breve y banner con su cola, diálogo, estado vacío, carga y esqueleto

- **Decisión:** se implementan B2-04, B2-05 y B2-09. El papel del aviso (la lámina con filete y sombra
  plana) vivía en `privado.css` como `.aviso` y servía para cuatro cosas; se separó en `.aviso-breve`
  (una acción que salió bien, `data-estado="hecho"`), `.banner` (afecta a la pantalla,
  `data-estado="error"`) y `.vacio`, con la regla común en `componentes/aviso.css`. `.cargando` y
  `.esqueleto` (`data-forma="punteado"`, `data-tamano="alto"`) están en `componentes/carga.css` y `.dialogo`
  en `componentes/dialogo.css`, con la transición `--movimiento-transicion-dialogo` y `@starting-style`.
  La región `#avisos` de los layouts privado y público lleva la cola de Alpine (`avisos`: 3 a la vez, 6 s,
  pausa con el puntero o el foco; el texto lo trae el servidor). Se migraron 77 usos en 33 plantillas; el
  tope de clases de un solo uso baja de 197 a 189 y las heredadas, de 500 a 487.
- **Alternativas:** `showModal()` desde Alpine para el diálogo (descartada: sin JavaScript no abre, y
  confirmar una baja es esencial; con `popover` se pierde el fondo inerte y el foco atrapado); un solo
  `.aviso` con tres variantes (descartada: son tres componentes del patrón, con rol y comportamiento
  distintos); migrar la portada (descartada: su estado sin datos es un marco de filete, otro aspecto;
  queda para B3-01); unificar los «Cargando…» públicos con `.cargando` (descartada: hoy son una lámina y
  pasarían a un texto en negrita); armar los avisos en JavaScript (prohibido por D31).
- **Por qué:** el aspecto sale de un solo lugar, el nombre dice qué es cada cosa y la cola cumple lo que
  pide la pauta (aviso en región `aria-live`, no se va antes de 5 s, no se va con el puntero o el foco encima).
  Los textos «Cargando…» públicos pasaron a `.banner` y los de la entidad y el donante a `.cargando` para no
  cambiar lo que se ve.
- **Costo:** quedan como heredadas de una sola pantalla `aviso` y `aviso--suelo` (portada), `foto-ventana` y
  `foto-titulo` (portada), `aviso-brote`, `aviso-acciones`, `aviso--nombre`, `aviso-codigo`, `hueco`,
  `seguimiento-reserva` y `seguimiento-aviso` (este último nunca tuvo estilo). `main h2` pasó a la capa
  `pico`: sin eso su color le ganaba al título del banner de error (otra vez, una regla sin capa le gana a
  una con capa). El diálogo de la foto (portada aparte) y el de la baja comparten base y se distinguen por
  `data-tono="quitar"`.
- **Quién decidió:** usuario (tsorren), 2026-11-04: hacer B2-04, B2-05 y B2-09 en esta tanda. El agente decidió
  y queda para que el usuario confirme: los nombres (`.aviso-breve`, `.banner`, `.vacio`, `.cargando`,
  `.esqueleto`, `.dialogo`) y los atributos `data-estado`, `data-forma` y `data-tamano`; `popover` y no
  `showModal()`; los 6 s y las 3 visibles de la cola; la región también en el layout público; el cambio de
  `AGENTS.md` del módulo sobre diálogos y avisos; los «Cargando…» públicos como `.banner`; y
  la portada fuera. **Cambio visual por confirmar:** en «Noticias de tus donaciones» del donante, el aviso
  «Listo: marcaste todas como leídas» ahora va en negrita, como el mismo aviso en las demás pantallas:
  esa hoja nunca cargaba `formularios.css`, que era donde estaba la negrita (4 capturas).
- **Confirmación:** usuario (tsorren), 2026-11-04: confirmó todo lo anterior, incluido el cambio visual de la
  negrita en «Noticias de tus donaciones» y, de la tanda 1, el cambio de Entidades del donante y sus decisiones.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34. `-Pvisual`
  (55 pruebas verdes, con los dos tests nuevos de la cola y del diálogo) con una corrida de ruido como base,
  las dos sobre el código sin tocar: 579 de 588 capturas iguales; `/estilos` (3) cambió por el contenido
  nuevo; la portada en 1920 px (2) difiere 16 px en la barra fija, como entre dos corridas del mismo código;
  y las 4 de `donante-notificaciones-leidas` por la negrita de arriba. Abiertos a mano, en un navegador sin
  cabeza (1280 y 360 px): la cola con 4 avisos (se ven 3), el diálogo de la baja y el de la foto con su
  fondo, y el hover del botón de cerrar. Los diálogos y la región no salen en las capturas de página porque
  `-Pvisual` no los abre.

### 2026-11-04 · stack · Fase 2, tanda 1 del backlog: botón, campo y sello de estado

- **Decisión:** se implementan B2-01, B2-02 y B2-03. Cada uno tiene su hoja en `componentes/` (capa
  `bloques`): `boton.css`, `campo.css` y `sello.css`, y su entrada en `componentes.md` y en `/estilos`.
  Las variantes pasan de clases a atributos: `data-tono="principal|secundario|quitar"` y
  `data-tamano="chico"` en el botón, `data-estado="error"` en el campo y `data-estado="…"` en el renglón
  `.estado`. El sello es un solo fragmento, `componentes/sello :: sello(estado)`, que reemplaza cuatro
  copias con vocabularios distintos y siete SVG en línea (`hecho` y `falta` son estados del fragmento).
  El tope de clases de un solo uso baja de 211 a 197.
- **Alternativas:** conservar las clases `boton--principal` y similares como excepciones (descartada: el
  ADR pide variantes por atributo); un fragmento por botón y por campo (descartada: son elementos
  nativos con atributos que cambian en cada uso, como `aria-describedby`, y un fragmento con slots
  habría tocado 117 botones y 67 campos sin ganar nada); unificar el botón chico en los tres aspectos
  que tenía (descartada: cambiaba lo que se ve en Necesidades y Notificaciones de la entidad).
- **Por qué:** el aspecto sale de un solo lugar y las plantillas dicen qué es cada cosa. Por esa misma
  regla de no cambiar lo que se ve, `data-tamano="chico"` sin tono es el botón en contorno de las
  páginas públicas, y con `data-tono="principal"` es el botón lleno de las dos pantallas privadas.
- **Costo:** quedan fuera de los componentes, como clases heredadas de una sola pantalla,
  `boton--foto`, `--mapa`, `--google`, `--suelo`, `pausa-boton`, `campo--fotos` y `campo--corto`, y el color
  de los `<button>` de la portada y de `publico-listas.css`. En las pantallas privadas un botón
  secundario conserva el texto negro por defecto del navegador, mientras que en las públicas es verde:
  una diferencia que ya existía y que no se tocó (hallazgo para la fase 3).
- **Quién decidió:** usuario (tsorren), 2026-11-04: hacer B2-01 a B2-03 en esta tanda. El agente decidió y
  queda para que el usuario confirme: los nombres de los atributos (`data-tono`, `data-tamano` y
  `data-estado`, que comparte el nombre `data-tono` con la utilidad de color del texto, sin
  mezclar valores); que botón y campo no tengan fragmento; que las variantes vivan en la hoja del bloque y
  no en la capa `excepciones`; `cursor: pointer` para todo `<button class="boton">`, que antes solo tenían
  algunas pantallas; que `.boton[hidden]` oculte el botón en todas las pantallas, no solo en las públicas; y mover `a { color }` de `app.css` a la capa `pico` (la de base), porque sin capa le ganaba a cualquier componente.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34.
  `-Pvisual` (53 pruebas verdes) con una corrida de ruido como base: sin cambios en 575 de 588 capturas;
  las de `/estilos` (3) cambiaron por el contenido nuevo; las de la portada (2) difieren 15 y 16 px, como
  entre dos corridas del mismo código; y hay **un cambio visual por confirmar**: en Entidades del
  donante (8 capturas en 360 px) el campo "Buscar por nombre" ya no se mete debajo del botón "Buscar".
  Pasaba porque esa pantalla nunca cargaba `formularios.css` y el campo no tenía `max-width`; con el
  componente cargado en todas las pantallas el campo respeta su columna. Es una corrección de un
  defecto, pero cambia lo que se ve: queda para que el usuario la confirme. En la verificación apareció
  y se corrigió otro efecto de las capas: `a { color }` (sin capa) le ganaba al texto de los botones
  principales, que quedaba del color de su fondo; ahora esa regla vive en la capa `pico`.

### 2026-11-04 · stack · Fase 1 del backlog de componentes: el movimiento suelto, a `movimiento.css`

- **Decisión:** se implementan B1-01 a B1-05. Los 13 `@keyframes` de siete hojas de pantalla pasan a
  `movimiento.css` (10 `mov-*`; `aparece`, `traza` y `lamina-traza` se reusan de las primitivas). Cada
  animación se declara completa (`@keyframes`, duración, curva y espera, solo con tokens) como una
  custom property `--movimiento-animacion-*` o `--movimiento-transicion-atenua`, y la hoja de pantalla
  la pide con `animation: var(--movimiento-animacion-registro)`. Las custom properties existen solo con
  movimiento permitido. El cerco suma una regla: `@keyframes`, `--movimiento-duracion-*`,
  `--movimiento-curva-*` y las esperas (salvo `retraso-ninguno`) no se escriben en ninguna hoja de la
  carpeta `css/` salvo `movimiento.css`, `app.css` y `tokens.css`.
- **Alternativas:** `data-movimiento` en las plantillas (descartada: las clases animadas, como `.raiz`,
  `.fruto` y `.etiqueta`, las comparten varias pantallas y tocaba unos 40 elementos SVG de 10
  plantillas, con riesgo de cambio visual); mover los selectores animados a `movimiento.css` (descartada:
  bajaba el conteo de clases de un solo uso sin migrar nada y llenaba la capa CUBE de clases de
  pantalla).
- **Por qué:** cumple la fase 1 (hojas de pantalla sin `@keyframes` ni duraciones) sin tocar plantillas
  y sin cambio visual; un rediseño del movimiento cambia solo `movimiento.css`.
- **Costo:** las animaciones de pantalla no se piden con `data-movimiento` sino con una custom property
  desde la hoja: dos maneras de pedir movimiento (el catálogo las documenta juntas). Quedan en la portada
  dos `animation-delay: var(--movimiento-retraso-ninguno)`, un reset de las reglas con línea de tiempo del
  scroll. El tope de clases de un solo uso no baja: sigue en 211 porque la fase no toca selectores.
- **Quién decidió:** usuario (tsorren), 2026-11-04: implementar la fase 1. El mecanismo de las custom
  properties lo decidió el agente: queda para que el usuario lo confirme.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34.
  `-Pvisual` sin cambios en 583 de 588 capturas; las de `/estilos` cambiaron por el contenido nuevo y
  las de la portada (5) difieren 6 a 27 px en las mismas filas que entre dos corridas del mismo código.

### 2026-11-04 · stack · Fase 0 del backlog de componentes: base, catálogo y cerco

- **Decisión:** se implementan los diez ítems de la fase 0 (B0-01 a B0-10): tokens de movimiento
  (`--movimiento-*`, con las 28 duraciones literales reemplazadas), `@layer` en `app.css`,
  `composicion.css`, `utilidades.css` y `movimiento.css`, Alpine CSP desde el WebJar, el catálogo
  `/estilos` con `componentes.md`, el cerco en `InterfazFitnessTest` (tope de clases de un solo
  uso en 211, el valor medido), las skills `vista-ui` y `revision-ui` con el catálogo y el vector U10, el video del
  movimiento con Playwright y View Transitions.
- **Alternativas:** mover todos los `@keyframes` ahora (queda en la fase 1); exigir que toda clase de
  las plantillas esté en el catálogo desde el primer día (imposible con 527 clases heredadas: lista
  congelada que solo baja); dejar las duraciones tal cual (no cabían en una escala de cinco).
- **Por qué:** sin el cerco, migrar no sirve: la entropía vuelve (regla 1 del backlog).
- **Costo:** las duraciones se redondearon hasta 0,1 s para entrar en la escala de tokens; Pico no se
  carga hoy en ninguna plantilla, así que la capa `pico` queda declarada y vacía.
- **Quién decidió:** usuario (tsorren), 2026-11-04: la fase 0 y el OK a la dependencia `alpinejs__csp`. El redondeo de las duraciones y el tope en 211 en lugar de 212 los decidió el agente: queda para que el usuario los confirme.
- **Evidencia:** [backlog](backlog-migracion-componentes.md), [catálogo](componentes.md) y DTI-34.

### 2026-11-04 · stack · Alpine CSP, CSS nativo con sistema de movimiento y capa CUBE con cerco

- **Decisión:** Alpine.js con el build CSP para el estado que no sale del navegador (HTMX sigue
  hablando con el servidor); "anda sin JavaScript" solo en lo esencial; `@layer`, View Transitions,
  `@starting-style`, `@container` y `popover` como mejora; tokens y primitivas de movimiento; capa
  CUBE propia (composición, utilidades, componentes con slots, excepciones) con un cerco en los tests
  y en las skills, y migración incremental con backlog obligatorio.
- **Alternativas:** Alpine estándar, Petite-Vue o seguir con JavaScript propio; GSAP o Motion;
  Tailwind v4 (diferido después de E6), Spring ViewComponent o seguir con una hoja por pantalla.
- **Por qué:** todo lo diseña y lo construye Claude Code. Un vocabulario cerrado de piezas y de
  movimiento hace que los agentes compongan en lugar de inventar, y las plantillas que dicen qué es
  cada cosa abaratan los rediseños. Sin build y sin salir del motor de plantillas que pide el enunciado.
- **Costo:** catálogo y cerco por construir antes de cerrar E6; conviven hojas por pantalla y capas
  CUBE mientras dure la migración.
- **Quién decidió:** usuario (tsorren), 2026-11-04.
- **Evidencia:** [ADR](../adr/cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md) (`proposed`), [backlog](backlog-migracion-componentes.md) y DTI-34.

### 2026-11-04 · movimiento:red (Raíces) · La portada: la escena queda quieta y las raíces llegan de a una

- **Decisión:** en la computadora, al llegar a "Quiénes reciben", la escena entera (de la planta con
  sus frutos hasta "Tu entidad") queda fija en la pantalla durante 1,5 pantallas de scroll. El
  tronco crece, las raíces llegan de a una, de izquierda a derecha, y cada etiqueta se imprime en su
  punta; "Tu entidad", al final. La red se achica para entrar en la pantalla. En el celular, cada
  fila de etiquetas se imprime al entrar y su tramo de tronco crece. Solo CSS (líneas de tiempo de
  scroll): sin línea de tiempo (Firefox) o con movimiento reducido, la escena no se fija y la red se
  traza al cargar, como antes. Para fijar la planta con la red, la plantilla separa el rótulo de la
  escena (`.escena` y `.escena-fija`) y saca las cifras del bloque de la tierra.
- **Alternativas:** A, una vez al llegar (con un script); B, ligada al scroll, ida y vuelta, sin
  fijar. El usuario eligió C y pidió que se vea desde los frutos hasta "Tu entidad".
- **Por qué:** es lo que pidió Nico (las raíces bajan al scrollear y se forma quién recibe, como
  "Cómo funciona" al entrar), y fijar la escena cuenta el recorrido de la donación paso a paso.
- **Costo:** la portada suma 1,5 pantallas de scroll en la computadora. La red tiene un ancho
  mínimo (60 rem) para que las etiquetas no se pisen: en pantallas muy bajas, "Tu entidad" puede
  quedar justo al borde (medido: entra a 1280 × 720 y a 1366 × 650). Las capturas de página
  completa no muestran el movimiento: se revisa con video o con cuadros de scroll. La revisión
  independiente marcó que las raíces se trazaban juntas y que las etiquetas se pisaban en
  pantallas bajas; se arregló antes de cerrar.
- **Quién decidió:** usuario (2026-11-04), con la nota de Nico.
- **Evidencia:** [`ab-red-a-una-vez.jpg`](evidencia/raices/mejoras-nico/ab-red-a-una-vez.jpg),
  [`ab-red-b-con-el-scroll.jpg`](evidencia/raices/mejoras-nico/ab-red-b-con-el-scroll.jpg),
  [`ab-red-c-tierra-quieta.jpg`](evidencia/raices/mejoras-nico/ab-red-c-tierra-quieta.jpg) y la aplicada,
  [`portada-escena-fija-1280.jpg`](evidencia/raices/mejoras-nico/portada-escena-fija-1280.jpg),
  [`portada-escena-fija-1920.jpg`](evidencia/raices/mejoras-nico/portada-escena-fija-1920.jpg) y
  [`portada-celular-375.jpg`](evidencia/raices/mejoras-nico/portada-celular-375.jpg) (cuadros cada 170 a 260 px de scroll; 1280 a 720 px de alto).

### 2026-11-04 · layout:contacto (Raíces) · Contacto con lámina: de la raíz al correo

- **Decisión:** a la derecha del formulario, a su misma altura (en el celular, debajo), una lámina
  como "Cómo funciona" de la portada: un brote cuya raíz central llega a un sobre, y "Cuándo
  escribirnos" con tres motivos. Las raíces se trazan al entrar.
- **Alternativas:** B, un corte de suelo alto con el mensaje sobre la tierra; C, tres raíces con un
  motivo en cada etiqueta. La frase "Contactanos para empezar a ser parte de nuestra comunidad"
  se cambió: en la página de contacto no tenía sentido (usuario).
- **Por qué:** repite el lenguaje de la portada y el sobre se entiende solo; los motivos ayudan a
  elegir el tema.
- **Costo:** ninguno en datos; la lámina no cambia el formulario.
- **Quién decidió:** usuario (2026-11-04), con la nota de Nico.
- **Evidencia:** [`ab-contacto-1280.jpg`](evidencia/raices/mejoras-nico/ab-contacto-1280.jpg),
  [`ab-contacto-360.jpg`](evidencia/raices/mejoras-nico/ab-contacto-360.jpg), [`contacto-1280.jpg`](evidencia/raices/mejoras-nico/contacto-1280.jpg) y
  [`contacto-360.jpg`](evidencia/raices/mejoras-nico/contacto-360.jpg).

### 2026-11-04 · layout:perfil (Raíces) · Mi perfil con pestañas: cómo te ven, datos y avisos

- **Decisión:** Mi perfil suma "Datos de tu cuenta" (razón social, tipo, rubro, CUIT fijo y
  dirección) y "Cómo te avisamos" (correo, WhatsApp, teléfono, por dónde y qué). Las tres secciones
  van en pestañas sobre la lámina; se ve una por vez. Cada pestaña es un enlace a su sección: anda
  sin JavaScript y con "atrás", y al guardar vuelve a la misma. Es maqueta: guardar no cambia nada
  hasta el perfil real (RI-AUTH-02, HB-11; [huecos de backend](flujos/raices-ab-huecos-backend.md)).
- **Alternativas:** A, una columna con todo a la vista; B, dos columnas en la computadora. El
  usuario pidió comparar pestañas contra A y eligió pestañas.
- **Por qué:** son tres tareas que no se hacen juntas; cada una entra en una pantalla y se encuentra
  por su nombre.
- **Costo:** lo que no está en la pestaña abierta no se ve; los campos salen de donaciones-service
  pero no se guardan.
- **Quién decidió:** usuario (2026-11-04), con la nota de Nico ("faltan más campos").
- **Evidencia:** [`ab-perfil-una-y-dos-columnas-1280.jpg`](evidencia/raices/mejoras-nico/ab-perfil-una-y-dos-columnas-1280.jpg),
  [`perfil-pestanas-1280.jpg`](evidencia/raices/mejoras-nico/perfil-pestanas-1280.jpg) y
  [`perfil-pestanas-375.jpg`](evidencia/raices/mejoras-nico/perfil-pestanas-375.jpg).

### 2026-11-04 · layout:admin (Raíces) · Los avisos abiertos del pulido, cerrados

- **Decisión:** en el menú del admin en el celular, la hoja de "Más" se llama "Todas las secciones"
  (el menú ya se llamaba "Secciones"), el foco entra a la primera sección al abrirla y vuelve a "Más"
  al cerrarla, y sin ventanas del navegador (popover, navegadores de antes de 2024) la barra
  muestra las 9 secciones en filas de a 3. El orden de las cifras de Seguimiento no cambia: el
  dibujo es decorativo (`aria-hidden`, sin enlaces), y el orden de lectura y de foco (cifras,
  después la lista) coincide con lo que se ve de arriba abajo.
- **Alternativas:** dejar los avisos abiertos.
- **Por qué:** el usuario pidió cerrar los avisos abiertos del pulido como mejora propia.
- **Costo:** con el respaldo, la barra del celular ocupa tres filas en navegadores viejos.
- **Quién decidió:** agente, dentro del alcance que dio el usuario (2026-11-04).
- **Evidencia:** prueba en vivo a 360 px: al cargar, el foco queda en la página; al abrir "Más",
  en "Inicio"; al cerrar, en "Más".

### 2026-11-03 · layout:publico y admin (Raíces) · Pulido de las públicas y del admin: el menú de 7 secciones y Contacto con menú

- **Decisión:** cuarto grupo del pulido visual (páginas públicas e inicio del admin).
  - **Menú del admin en el celular, F:** las 7 secciones se pisaban en la barra. Ahora la barra
    muestra 4 y "+ Más", que abre una hoja desde abajo con las 7 en filas, Cerrar arriba y la
    página oscurecida detrás. Es una ventana del navegador (popover): anda sin JavaScript y se
    cierra con Esc o tocando afuera. "Más" queda marcado si la sección actual no entra en la barra.
    Arreglo de la revisión de cierre: a 360 px "Entidades" y "Donaciones" se pisaban en celdas de
    72 px; en esa barra la letra va angosta (`font-stretch` 75 %), del mismo tamaño, y la hoja
    lleva el nombre "Secciones" para el lector de pantalla.
  - **Menú del admin en pantalla grande:** va entero en una fila propia de la cabecera. Antes se
    partía al lado de la cuenta y dejaba un hueco.
  - **Contacto y Términos y privacidad** llevan el menú público, sin sección marcada. Se cambia la
    prueba que exigía lo contrario (de la auditoría de la corrida A/B).
  - Las demás públicas, sin cambios.
- **Alternativas:** para el menú del admin, primero A (dos filas de 4), B (la barra se desliza de
  costado) y C (el menú arriba, en la cabecera). El usuario descartó A y C y propuso D: un botón
  "+ Más" con un menú a pantalla completa de filas. Después pidió variantes de D con buenas
  prácticas: E (pantalla completa con Cerrar arriba), F (hoja desde abajo) y G (baldosas de tierra
  con solo las 3 que faltan).
- **Por qué:** con más de 5 secciones, la práctica común es mostrar 4 y poner el resto detrás de
  "Más"; la barra que se desliza esconde secciones sin avisar (UX Planet, SAP Fiori iOS). La hoja
  desde abajo queda al alcance del pulgar y se entiende como algo encima de la página. Una ventana
  así lleva Cerrar visible y con nombre, Esc y el foco de vuelta al botón (guías de hojas de OVO y
  react-native-ama).
- **Costo:** 3 secciones quedan a un toque más. Con muchas más secciones, la hoja pediría
  desplazarse. La ventana del navegador no bloquea el resto de la página para el lector de
  pantalla (no es modal).
- **Quién decidió:** usuario (D propia, F entre E, F y G, y el menú en Contacto y Términos,
  2026-11-03).
- **Evidencia:** [`evidencia/raices/pulido-publicas/`](evidencia/raices/pulido-publicas/), láminas
  del primer A/B (A, B y C), de B contra D y de E, F y G; la barra arreglada, en
  `admin-inicio-360x640.jpg`.

### 2026-11-03 · layout:entidad (Raíces) · Pulido del sitio de la entidad: Notificaciones y el menú en un renglón

- **Decisión:** tercer grupo del pulido visual (sitio de la entidad), con dos arreglos directos.
  - **Notificaciones:** se lleva la variante C que el usuario eligió para el donante. En pantalla
    grande, las sin leer van en una franja de tierra, una tarjeta por novedad colgada de una raíz
    horizontal (la cinta paja de "sin leer" pasa a ser el tramo que cuelga). Las leídas siguen en
    filas, debajo, y la bandeja pasa de 60 a 76 rem. "Marcar todas como leídas" va a la derecha
    del título. En el celular, nada cambia.
  - **Menú de la cabecera:** en pantalla grande, un menú de hasta 5 secciones (donante y entidad)
    va en un solo renglón y sin partir sus etiquetas. Antes, a 1280 px, "Seguir entregas" de la
    entidad bajaba a un segundo renglón y dejaba un hueco a su lado. Se achica la cuenta.
- **Alternativas:** para Notificaciones, ninguna nueva: la decisión del donante vale para las dos.
  Para el menú, partirlo en dos filas a todo el ancho, como en la tableta (más alto, sin hueco).
- **Por qué:** la bandeja de la entidad dejaba vacío el tercio derecho, como la del donante antes
  del pulido, y las dos pantallas hacen lo mismo.
- **Costo:** el menú de la administración (7 secciones) sigue partiéndose en dos renglones: queda
  para su grupo. En la entidad, el nombre de la cuenta ocupa tres renglones a 1280 px.
- **Quién decidió:** usuario (Notificaciones C, en el grupo del donante); el agente aplicó los dos
  arreglos.
- **Evidencia:** [`evidencia/raices/pulido-entidad/`](evidencia/raices/pulido-entidad/).

### 2026-11-03 · layout:portada (Raíces) · Pulido de la portada: el menú del celular y el rótulo del mes

- **Decisión:** segundo grupo del pulido visual (la portada). Hay un arreglo directo y un A/B.
  - **Arreglo:** en el celular, la barra del menú no se veía en la portada ni en las públicas que
    la llevan. La cabecera recortaba su fondo para estirarlo a todo el ancho, y el recorte también
    escondía la barra fija, que va dentro. Ahora el fondo se estira con un borde de imagen, que no
    recorta. Vale también para el pie.
  - **Rótulo "Octubre de 2026", A:** queda como está, suelto junto a la planta.
- **Alternativas:** B, la etiqueta atada con un hilo al fruto en camino; C, un cartel clavado en
  la tierra.
- **Por qué:** el rótulo ya se lee como la cuenta de la planta y sin trazos de más la franja queda
  limpia.
- **Costo:** el rótulo no señala a qué planta se refiere. Las capturas de pantalla completa no
  muestran lo que va fijo, así que la verificación visual no detectaba la barra escondida.
- **Quién decidió:** usuario (2026-11-03).
- **Evidencia:** [`evidencia/raices/pulido-portada/`](evidencia/raices/pulido-portada/), láminas del
  A/B en 1280 y 360 px.

### 2026-11-03 · layout:donante (Raíces) · Pulido del sitio del donante, pantalla por pantalla

- **Decisión:** primer grupo del pulido visual (sitio del donante). Hay tres arreglos directos y tres A/B.
  - **Arreglos:** en el detalle de una donación, "Los datos" va justo debajo de la foto (antes
    esperaba a que terminara la raíz). El acceso a Notificaciones va en todas las pantallas
    privadas, también en Mi perfil, Seguimiento, Necesidades y Confirmar recepción. En el inicio
    sale el botón "Ver Mis donaciones" del final, porque ya estaba en "Esperan destino".
  - **Notificaciones, C:** en pantalla grande, las sin leer van en una franja de tierra a todo el
    ancho, una tarjeta por aviso colgada de una raíz horizontal. Las leídas van debajo, como un
    libro de registro: una fila por aviso con fecha, qué pasó y estado.
  - **Inicio, B:** en la franja "Lo que está confirmado", el hito pasa a la izquierda, debajo de la
    actividad. La rama sigue a la derecha.
  - **Seguimiento, B:** en pantalla grande, las cifras por estado van a la derecha del dibujo, en
    una columna, como su leyenda. Las tarjetas siguen debajo.
- **Alternativas:** cada A/B tuvo tres variantes (segura, media y audaz) más la versión de hoy.
  - Notificaciones: A, una columna angosta; B, la lámina arriba y el registro debajo.
  - Inicio: A, tres columnas; C, un tendedero de lado a lado.
  - Seguimiento: A, la lista al lado del mapa; C, el mapa a todo el ancho.
- **Por qué:** en las tres pantallas quedaba un lado vacío en 1280 px: la columna izquierda en
  Notificaciones y en la franja del inicio, y el tercio derecho en Seguimiento. En Notificaciones,
  la franja repite el lenguaje del inicio y la página queda la más corta. En el inicio y en
  Seguimiento se eligió el cambio más chico que llena el hueco.
- **Costo:** en Notificaciones, las sin leer ya no quedan fijas al bajar. En el inicio, el hito ya
  no cuelga de la rama desde la tableta. En el celular, nada cambia.
- **Quién decidió:** usuario (las tres variantes y los arreglos, 2026-11-03).
- **Evidencia:** [`evidencia/raices/pulido-donante/`](evidencia/raices/pulido-donante/). Las capturas `antes-*`
  son de antes del pulido y las que llevan `--` en el nombre, de las variantes.

### 2026-11-03 · layout:recepcion-confirmada (Raíces) · Lo nuevo primero y la raíz con su grosor

- **Decisión:** dos arreglos en Recepción confirmada.
  - **El orden:** lo asentado y la entrega que sigue van primero también en el código, no solo en
    pantalla; después, la etiqueta, el último tramo y los botones al final. En pantalla grande,
    lo asentado pasa a la izquierda y la etiqueta con el tramo, a la derecha.
  - **El grosor:** la raíz grande tiene el mismo grosor que las líneas de Seguir entregas en
    cualquier ancho. El trazado al confirmar se revela de izquierda a derecha.
- **Alternativas:** para el orden, el del acta (la etiqueta y el tramo primero, lo asentado más
  abajo en el celular). Para el grosor, que crezca con tope, o dejarlo como estaba.
- **Por qué:** en el celular se veía primero lo asentado, pero el lector de pantalla y el teclado
  empezaban por la etiqueta, que estaba antes en el código (WCAG 1.3.2 y 2.4.3). La raíz engordaba
  con la pantalla (unos 3 px en el celular y 17 en la computadora) y no hablaba igual que las otras
  líneas de subte.
- **Costo:** en pantalla grande, Recepción confirmada ya no tiene la etiqueta a la izquierda como
  el acta de Confirmar recepción. El enlace a los textos legales, que figuraba pendiente, ya estaba
  resuelto: el pie de todas las páginas lleva "Términos y privacidad".
- **Quién decidió:** usuario (las dos, con la opción recomendada).
- **Evidencia:** `evidencia/raices/recepcion-confirmada/`.

### 2026-11-03 · proceso · Una sola identidad: Raíces es el sitio y se retiran Glassmorphism y Cozy / Medieval

- **Decisión:** el cliente liviano queda con una sola identidad, Raíces, servida en `/` (la portada)
  y `/<pantalla>`. Se borran las plantillas, hojas, imágenes, scripts y fuentes de Glassmorphism y
  de Cozy, el catálogo `/estilos` y `?alternativa=` con sus reglas (gemelas, tabla de voz, juego
  completo). Sus documentos pasan al [archivo histórico](historico/README.md). Las skills, el
  workflow `corrida-creativa` y las plantillas de encargo no usan ninguna identidad como ejemplo.
  Una identidad o un rediseño nuevo se propone en su propia rama, sobre el sitio.
- **Alternativas:** dejar a Raíces en `/prototipo/raices/` con `/` redirigiendo ahí; dejar el
  mecanismo de alternativas vacío; borrar los documentos viejos o dejarlos donde estaban.
- **Por qué:** el grupo eligió Raíces y es la única con el recorrido de los tres roles; una pantalla
  se escribe una vez; los ejemplos de las identidades viejas no condicionan los diseños que vengan
  (pedido del usuario).
- **Costo:** comparar dos identidades lado a lado pide dos ramas. Los enlaces de los dos ADR
  anteriores a los documentos movidos quedan rotos (los ADR no se editan).
- **Quién decidió:** usuario.
- **Evidencia:** [ADR aceptado](../adr/cliente-liviano/20261103-una-sola-identidad-en-el-cliente-liviano.md).

### 2026-11-03 · estructura (Raíces) · Sitio A elegido, con la cabecera, el menú y el pie de B

- **Decisión:** el usuario eligió el sitio A de la corrida A/B y tomó tres cosas de B: la cabecera
  y el pie en verde oscuro (tierra honda) a todo el ancho, en todas las páginas; el menú horizontal
  en la cabecera desde la tableta (abajo solo en el celular), en las privadas y en las públicas;
  y la textura a la mitad, en el papel y en la tierra. La marca va en un cartel de papel sobre la
  tierra, como en B. Con la elección, las tres fichas `raices-*-navegable.md` quedan aprobadas.
- **Alternativas:** el menú abajo también en la tableta (descartado: cambia de lugar a mitad de
  tamaño); la textura a la mitad solo en el papel, o casi imperceptible.
- **Por qué:** el usuario prefiere A, con la cabecera de B; la tableta y la textura, las
  recomendadas.
- **Costo:** reemplaza la columna plegable de pantalla grande (A/B de la estructura privada,
  2026-11-01, decisión del usuario): sale el botón de plegar y `js/raices-menu.js`. Las seis
  pantallas aprobadas cambian en su cabecera y su pie, que ya no son iguales píxel a píxel a las
  de antes. Contacto y Términos cambian solo la cabecera y el pie: siguen sin la navegación
  pública, que nunca tuvieron (exclusión de `layout-publico.html`). El sitio B se borró
  (usuario, 2026-11-03: plantillas, hojas, su `Prototipo` en el controller, sus rutas de
  verificación y sus tests); su evidencia queda en `evidencia/raices/ab-navegable/b/`.
- **Quién decidió:** el usuario (la tableta y la textura, en una ronda de dos preguntas).
- **Evidencia:** `docs/interfaz/evidencia/raices/cabecera-de-b/` (1280 y primera pantalla del
  celular de cuatro pantallas); la tableta, por la verificación visual de las 146 rutas de A, que
  mide desbordes en los anchos intermedios, con `reporte.json` vacío.

### 2026-11-02 · flujo (Raíces) · Corrida A/B: dos sitios navegables

- **Decisión:** una corrida desatendida dejó la tercera alternativa como dos sitios navegables de
  la maqueta, con los recorridos público ("entrar"), del donante ("donar y seguir") y de la entidad
  ("recibir"), más el inicio del admin con su menú, sin enlaces muertos:
  - **A** (`/prototipo/raices/`): fiel a lo aprobado. Extiende las piezas actuales (layouts,
    sellos, formularios, etiquetas de herbario) sin rediseñar las siete pantallas ya aprobadas.
    Para que se pueda llegar a las nuevas desde el ancla, solo cambia la portada: suma la
    navegación pública, dos enlaces "Ver todas…" (a Entregas confirmadas y a Entidades) y sus
    enlaces van a Raíces en vez de a Cozy. El acceso fijo a Notificaciones del layout privado y
    la navegación del layout público van solo en las pantallas nuevas: en las otras seis
    aprobadas no aparecen, y sus capturas son iguales a las de antes, píxel a píxel (arreglo de
    la auditoría, ronda 1). Lo único que cambia en ellas sin verse es el destino de "Salir",
    que va a la portada de Raíces y no a la de Cozy. 18 pantallas nuevas (las 16 del plan y Recuperar la
    contraseña con su aviso, para que Ingresar no salga a Cozy).
  - **B** (`/prototipo/raices-b/`): audaz, con el mismo ADN (paleta Oliva, risografía, raíces).
    Otra navegación (una línea pública de subte y otra por rol) y otra composición; rehace
    también las siete aprobadas y suma la lista de Seguir entregas de la entidad y la
    recuperación de la contraseña.
  - Lo que lo prueba: un test de `PrototipoControllerTest` que, desde las portadas y los inicios
    de cada rol, sigue en anchura los enlaces de cada sitio, exige 200 y que ninguno salga a
    otra alternativa (salvo "sin dibujar", que ofrece la misma pantalla en Cozy), y que las 23
    pantallas de los tres recorridos tienen plantilla propia en los dos. Lo que el backend todavía
    no da quedó con datos sintéticos y en una lista aparte, sin DTI ni ADR nuevos.
- **Alternativas:** correr dos veces la corrida creativa guardada (elige un ganador por
  iteración y su flujo se bloquea fuera de Cozy), hacerlo a mano con agentes sueltos, o un solo
  sitio con `?variante=` (cambia el estilo, no la estructura ni la navegación).
- **Por qué:** el usuario pidió "aprende del diseño actual de Raíces y plantea una corrida
  desatendida A/B que implemente las demás pantallas de forma navegable". Para comparar de
  verdad, B tiene que poder cambiar la estructura y la navegación, y A, mostrar lo que sale de
  seguir el sistema sin arriesgar nada. Como las 16 pantallas nuevas no tenían flujo aprobado, la
  corrida escribió tres fichas compartidas por A y B
  ([público](flujos/raices-publico-navegable.md), [donante](flujos/raices-donante-navegable.md)
  y [entidad](flujos/raices-entidad-navegable.md)), que quedan pendientes de aprobación hasta
  que el usuario elija.
- **Costo:** No reemplaza nada: A y B conviven con las siete pantallas aprobadas de Raíces y con
  Cozy y Glassmorphism, que no cambian (comparación píxel a píxel sin diferencias). B vive en
  `raices-b`, con su `Prototipo` propio en el controller, sus plantillas y sus hojas
  (`templates/prototipo/raices-b/`, `static/css/prototipo/raices-b/`); al elegir un sitio se
  borra el otro. Misiones queda fuera (RF-07 la oculta a la empresa) y el resto del admin, sin
  dibujar. Los huecos de backend están en las fichas y juntos en
  [flujos/raices-ab-huecos-backend.md](flujos/raices-ab-huecos-backend.md).
- **Quién decidió:** corrida desatendida; el usuario elige A o B y aprueba las fichas.
- **Evidencia:** `evidencia/raices/ab-navegable/a/` y `b/` (360 y 1280 px de cada pantalla nueva
  y la primera pantalla del celular de cada portada; en `b/`, también las siete aprobadas que B
  rehízo); página de antes y después en la carpeta de la corrida, fuera del repo.
- **Revisión:** `revision-ui` independiente: PASS en A y en B (modo pantalla) y en las tres fichas
  (modo flujo, tras arreglar seis hallazgos bloqueantes de la ficha pública y la de la entidad).
  Veredictos, arreglos y avisos que quedan:
  [evidencia/raices/ab-navegable/revision-ui.md](evidencia/raices/ab-navegable/revision-ui.md).

### 2026-11-02 · proceso (Raíces) · La review de la diseñadora, decidida

- **Decisión:** Sofía Deane, la diseñadora del grupo, respondió tarde el artifact. Vio la versión
  del 1 de noviembre (paleta Bosque, bordes rectos). El usuario decidió cada nota, de a un tema:
  - **Cambio de cantidad (12):** sigue valiendo desde el período en curso, y la necesidad muestra
    un cartel a quien la administra: "Cambiada hoy" en el resumen y, en el detalle, "Hoy pasó de
    40 a 45 kg por mes. Vale desde octubre: lo que ya llegó cuenta."
  - **Nombre oculto (16):** por defecto se ven los dos, empresa y persona. Con el nombre oculto, la
    entidad ve la categoría del ranking de Incentivos: "Donante Sostenedor" o "Empresa donante ·
    Sostenedor". Reemplaza "Una persona que prefiere no mostrar su nombre".
  - **Anónimo por donación (15):** se suma "Esta vez, sin mi nombre" al registrar una donación.
    Queda escrito en la ficha de Mi perfil y en el ADR, sin pantalla: Raíces no tiene todavía la de
    registrar una donación.
  - **Contacto (17):** ya cumplía el pedido: el formulario no muestra ninguna dirección de correo.
  - **Legales (18):** sin cambios: en el pie, al registrarse y en la constancia que ya muestra
    Mi perfil ("Aceptaste la versión 1…"). La portada no suma un menú.
  - **Diseño (1 a 4):** cuatro A/B, en las entradas siguientes. Los bordes redondeados que pedía ya
    estaban (esquina suave).
- **Alternativas:** para la 12, valer desde el próximo período con cartel (el voto de Sofía) o
  quedar sin cartel. Para la 16, empresa visible y persona oculta (la recomendada), las dos ocultas,
  o los textos «Una persona que dona» y «Un donante». Para la 18, un menú corto en la portada
  (la recomendada; la pregunta ofrecía además un apartado en Mi perfil, que ya existía).
- **Por qué:** el usuario mantuvo sus decisiones de la mañana en la 12 y la 16, y tomó de Sofía el
  cartel y el nombre más cálido. Eligió el nivel del ranking contra la recomendada porque es lo que
  propuso Sofía y ya existe en Incentivos.
- **Costo:** la categoría sale de otro servicio (Incentivos) y suma una llamada al armar la pantalla
  (ADR del nombre visible y DTI-32). Si no llega, la entidad ve "Un donante" o "Una empresa
  donante" (usuario, por la revisión). Quedan para el grupo dos dudas: una categoría poco común
  puede dejar adivinar quién donó, y las empresas no ven sus incentivos (RF-07). El cartel pide
  `ultimoCambio` en el contrato (DTI-30).
- **Quién decidió:** usuario (2026-11-02), con la review de Sofía Deane.
- **Evidencia:** fichas de Necesidades, Confirmar recepción, Seguir entregas y Mi perfil;
  `evidencia/raices/review-sofia/`.

### 2026-11-02 · layout:necesidades (Raíces) · El detalle, datos y gráfico; el resumen sin el total repetido

- **Decisión:** B. En la computadora, el detalle abierto pone a la izquierda "Por qué hace falta" y
  "Quién donó" lado a lado, con las entregas debajo; a la derecha, los períodos como barras que suben
  de abajo arriba, con el nombre y lo que llegó debajo. Sin períodos, las tres partes van una al
  lado de la otra, como antes. El resumen de cada tarjeta dice solo hasta cuándo ("Octubre, hasta
  el 31"): cuánto pide va una sola vez, en la barra ("10 de 40 kg recibidos"; "0 de 20 litros
  recibidos" si no llegó nada).
- **Alternativas:** A, tres columnas y una ficha por mes debajo, a todo el ancho.
- **Por qué:** Sofía pidió aprovechar el espacio y que no parezca una lista. B se lee como datos y
  gráfico y deja la tarjeta abierta más baja. El total aparecía dos veces en cada tarjeta plegada.
- **Costo:** los textos debajo de cada barra quedan en dos o tres renglones. Por la revisión, el
  usuario eligió (las recomendadas) mostrar los cuatro últimos períodos, del más viejo a la izquierda
  al período en curso a la derecha, con la base común y "Ver los anteriores" plegado; la columna
  del gráfico se ensanchó para que "Septiembre" entre. Las barras verticales (`writing-mode` sobre
  `progress`) se comprobaron solo en Chromium.
- **Quién decidió:** usuario (2026-11-02), la recomendada, a pedido de Sofía.
- **Evidencia:** `evidencia/raices/review-sofia/necesidades-detalle-ab.jpg` (el A/B, antes de los
  ajustes de la revisión), `necesidades-detalle-final.jpg` (como quedó) y
  `necesidades-detalle-360.jpg` (en el celular, las dos son iguales).

### 2026-11-02 · layout:confirmar-recepcion (Raíces) · El acta en dos filas de dos columnas

- **Decisión:** A. En la computadora, arriba, la etiqueta de la entrega y "El último tramo" lado a
  lado; abajo, "Las pruebas" a la izquierda y "Lo que recibiste" con la firma y el botón a la
  derecha. El botón de las fotos queda centrado en su zona. Recepción confirmada sigue en dos
  columnas, con la etiqueta fija a la izquierda.
- **Alternativas:** B, como estaba: la etiqueta y el tramo a la izquierda, fijos al bajar, y el
  formulario a la derecha.
- **Por qué:** Sofía pidió las dos piezas en columnas para achicar el scroll. A ordena la página
  (primero qué llegó, después qué hacer) y acerca el botón Confirmar.
- **Costo:** la página casi no se acorta (de 1214 a 1185 px a 1280 × 900): el alto lo pone el
  formulario. La etiqueta deja de quedar a la vista mientras se completa el formulario. El texto del
  botón de archivos ("Choose Files") lo pone el navegador según su idioma.
- **Quién decidió:** usuario (2026-11-02), la recomendada, a pedido de Sofía.
- **Evidencia:** `evidencia/raices/review-sofia/confirmar-dos-filas-ab.jpg` y
  `confirmar-dos-filas-360.jpg`.

### 2026-11-02 · estilo (Raíces) · Las líneas de subte, con los codos redondeados

- **Decisión:** A. Las líneas del mapa de Seguir entregas conservan los recorridos y los codos a
  45°, redondeados. El usuario pidió lo mismo en la red de la portada ("Quiénes reciben las
  donaciones") y se llevó también a la raíz de Recepción confirmada, para que las tres hablen igual.
- **Alternativas:** B, raíces curvas, sin codos.
- **Por qué:** Sofía pidió formas menos rígidas, con un punto intermedio. A sigue leyéndose como un
  recorrido con estaciones, sin la frialdad del ángulo, y cambia poco lo ya aprobado.
- **Costo:** ninguno visible; los trazados son un poco más largos de escribir.
- **Quién decidió:** usuario (2026-11-02), la recomendada en las dos preguntas.
- **Evidencia:** `evidencia/raices/review-sofia/lineas-mapa-ab.jpg`, `lineas-mapa-a-360.jpg` y
  `lineas-mapa-b-360.jpg`.

### 2026-11-02 · movimiento (Raíces) · Viento en ciclo en la portada y las entregas del mes de a una

- **Decisión:** A. Después de trazarse las raíces, las hojas del brote y de la planta y los frutos
  se mecen unos 3 segundos y quedan quietos unos 20, una y otra vez. Las tarjetas de "Lo que viajó
  este mes" aparecen de a una al bajar. Con "reducir movimiento", todo queda quieto; sin línea de
  tiempo de scroll (Firefox), las tarjetas se ven quietas.
- **Alternativas:** B, una sola ráfaga al entrar (la recomendada); sin viento, como estaba; las
  tarjetas quietas.
- **Por qué:** Sofía propuso las dos formas del viento y las tarjetas de a una para que la portada
  no quede rígida. El usuario prefirió que la portada se sienta viva cada vez que se la mira.
- **Costo:** el movimiento vuelve aunque nadie lo busque y compite con la lectura de la parte de
  arriba. Como se repite sin fin, WCAG 2.2.2 pide una forma de pausarlo: `revision-ui` lo marcó y
  el usuario eligió un botón "Pausar el movimiento" bajo la lámina "Cómo funciona" (la recomendada,
  contra una sola ráfaga o pocas repeticiones). Es una casilla con forma de botón, sin JavaScript;
  con "reducir movimiento" no se muestra. En Firefox y Safari, el giro de las hojas no se comprobó.
- **Quién decidió:** usuario (2026-11-02), contra la recomendada en el viento.
- **Evidencia:** el movimiento no se ve en una captura; se ve en la maqueta, en
  `/prototipo/raices/publico-portada`.

### 2026-11-02 · proceso (Raíces) · Las preguntas abiertas de Raíces, decididas

- **Decisión:** las 18 preguntas abiertas de las fichas de Raíces se mandaron al grupo en un
  artifact (2026-11-01). Respondieron Nico, Anush y Camila, y el usuario decidió cada tema en
  cuatro rondas, con los votos a la vista. En el front:
  - **Necesidades:** la cantidad va en la unidad de la subcategoría del catálogo. La lista la dice
    entre paréntesis y "Cuánto" es un número, así que "latas" y "cajas" pasan a "unidades". Se
    suma "Cada quincena" (15 días). La fecha límite se conserva. Al cambiar la cantidad, la ayuda
    dice que vale desde el período en curso. El diálogo de la baja dice qué pasa con cada entrega,
    según si salió.
  - **Nombre de quien donó:** se ve, sea empresa o persona; si lo ocultó en su perfil, "Una
    persona que prefiere no mostrar su nombre". Reemplaza "Donó: Una persona".
  - **Confirmar recepción:** sin la cantidad de bultos. La etiqueta dice cuánto llegó, y la nota
    del botón dice que lo incompleto o dañado se confirma igual y se reporta.
  - **Pantallas nuevas:** Contacto, Mi perfil (mínimo) y Términos y privacidad, con un pie corto
    en todas las páginas, también en las privadas. Cada "escribinos con este código" lleva a
    Contacto con el código ya escrito.
- **Alternativas:** las del artifact, con sus votos (tabla en el handoff de Raíces). Para el campo
  de bultos: sumarlo al contrato junto al remito, o dejarlo solo en pantalla. Para Mi perfil: un
  interruptor que guarda solo. Para el contacto: solo con sesión. Para el pie privado: los enlaces
  en el menú de la cuenta.
- **Por qué:** cada pantalla deja de mostrar datos que el contrato no tiene, o los marca como
  pendientes de backend en su ADR. El usuario fue contra lo más votado en dos temas: la fecha límite
  (el grupo pidió sacarla) y la baja (el grupo se dividió).
- **Costo:** el backend queda en cuatro ADR `proposed` y en las DTI-29 a DTI-33, sin tocar
  servicios. El texto de los legales es de ejemplo. El registro de Raíces, con su casilla de
  aceptación, queda descrito en la ficha de Legales hasta que se dibuje.
- **Quién decidió:** usuario (2026-11-02), con las respuestas del grupo. La forma de cada pantalla
  nueva se aprueba al verla dibujada.
- **Evidencia:** fichas `raices-necesidades.md`, `raices-confirmar-recepcion.md`,
  `raices-contacto.md`, `raices-mi-perfil.md` y `raices-legales.md`; ADRs en `docs/adr/`
  (20261102-…).

### 2026-11-02 · componente:cifras (Raíces) · "Lo que está confirmado": una cosecha colgada de una rama

- **Decisión:** C, la rama. En la portada, las tres cifras de "Lo que está confirmado" cuelgan de
  una rama color paja, cada una dentro de un fruto de su tamaño y su color: mandarina, la más grande,
  las entregas confirmadas desde marzo; paja, las entidades en el mapa; verde, las donaciones en
  camino. Cada fruto lleva la segunda tinta corrida, como los de la planta, una hoja salvia y, debajo, una
  etiqueta de papel con el rótulo, derecha. En la computadora, la rama cruza de lado a lado y los
  frutos cuelgan a distinta altura; en el celular, la rama baja por el costado. Sin datos (vacío,
  cargando o error), el aviso sigue en el marco doble. Reemplaza el registro de campo con líneas
  guía.
- **Alternativas:** tres, con la paleta Oliva: A, segura, el mismo registro con cada línea guía
  terminando en un fruto mandarina con el número adentro; B, media, tres frutos mandarina grandes
  en fila, sin marco, con el rótulo debajo; y C, audaz, la rama. La recomendada fue C. El usuario
  eligió C, con las etiquetas derechas en lugar de inclinadas. Después, por un aviso de la revisión
  ("en camino" se dibujaba hueco en la planta y lleno en la rama), el usuario eligió que el fruto
  verde de la rama pierda la segunda tinta corrida y lleve el contorno punteado en esa tinta, como
  el fruto hueco de la planta (descartadas: el fruto hueco del todo y dejarlo como estaba).
- **Por qué:** C es la que tiene más impacto y color, y los colores dicen algo: en la planta de
  arriba, el mandarina es una entrega confirmada, así que las que van en camino quedan verdes,
  todavía creciendo. A y B pintaban de mandarina también las 32 en camino.
- **Costo:** las tres cifras ya no se leen en una columna alineada; el tamaño de cada fruto es
  fijo (no proporcional al número) y con cifras de más dígitos habría que achicar la letra.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/cifras/`: las tres en 1280 y 360 px (`tres-*`), C aplicada en
  la portada (`aplicada-*`) y la planta junto a la rama con el fruto en camino punteado
  (`en-camino-planta-y-rama.jpg`).

### 2026-11-02 · bordes (Raíces) · Esquina suave en el papel y en los botones

- **Decisión:** B, esquina suave. Las láminas, las etiquetas, las tarjetas, las cifras, los
  dibujos, la zona de fotos y los avisos de las cinco pantallas de Raíces tienen las esquinas
  apenas redondeadas (0,75 rem), como un papel grueso; los botones, un poco menos (0,5 rem), como un
  canto gastado. Se conservan el marco doble de línea fina y la sombra plana salvia. Queda en
  `tokens.css` como `--radio-lamina` y `--radio-boton`. Los campos de texto y las barras de avance
  quedan rectos. En Recepción confirmada, el dibujo de la raíz gana aire a los lados para que el
  rótulo de la entidad no toque la esquina, y la línea de tierra sigue hasta el marco.
- **Alternativas:** cinco en la portada: A, segura, rectos como hasta ahora (esquina viva, marco
  doble y sombra salvia); B, media, esquina suave; C, media, botones como semillas (el papel recto
  y los botones en píldora); D, audaz, troquel (el papel con las esquinas mordidas hacia adentro,
  como un boleto, y los botones en bisel); y E, audaz, afiche grueso (un solo marco de tinta grueso
  y la sombra en tinta, más larga). La recomendada fue D. El usuario eligió D y la pidió en todas
  las pantallas; quedó aplicada en dos commits. La revisión independiente avisó que Firefox y
  Safari no dibujan la esquina mordida y la muestran redondeada, como B. Ante eso, el usuario
  eligió B para todos los navegadores.
- **Por qué:** con B, el sitio se ve igual en todos los navegadores. D era la de más carácter,
  pero solo en Chrome y Edge; en los demás se convertía en B sin decirlo. C hacía que los botones
  parecieran de una aplicación; E pesaba tanto que competía con el contenido, contra la textura
  suave recién elegida.
- **Costo:** se resigna el troquel, la que más decía "impreso". Al pasar de rectos a redondeados,
  el estilo pierde un poco de afiche y gana amabilidad.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/bordes/`: las cinco en la portada, de cerca (botones, una
  etiqueta y la esquina de la lámina) y en la primera pantalla del celular (`cinco-*`); y B
  aplicada en la portada en 360 y 1280 px y en las cinco pantallas (`aplicada-*`).

### 2026-11-02 · textura (Raíces) · Risografía suave, solo en el fondo

- **Decisión:** B, risografía, más suave y solo en el fondo. El papel de todas las páginas lleva un
  polvo fino de tinta. En los planos de tierra (la tierra y las cifras de la portada, los mapas de
  Seguir entregas y de Recepción confirmada, y la barra del menú en el celular), el papel se asoma
  en puntitos entre la tinta. Las dos texturas van detrás del contenido. Tarjetas, botones, el sol
  y los dibujos quedan lisos. Reemplaza al grano de las cuatro franjas de la portada, que iba encima
  del contenido. En los mapas, la tierra deja de ser un rectángulo del dibujo y la pinta el fondo
  del marco, para que la textura quede debajo de las raíces. El token del semitono, que ya no usaba
  nada, sale de `tokens.css`. Al ver la aplicada, el usuario pidió que los botones tampoco tengan
  la textura: los de contorno, que eran transparentes, toman el fondo liso de su plano (papel,
  tierra o el verde hondo de las cifras).
- **Alternativas:** dos vueltas. En la primera, cinco en la portada: A, segura, el grano de hoy
  parejo, sumado al registro de cifras; B, media, risografía (polvo de tinta en el papel y el papel
  que se asoma en la tierra y el sol); C, media, semitono (papel liso, el sol en puntos y la tierra
  que se oscurece con puntos); D, audaz, linograbado (el sol en anillos tallados y la tierra con
  marcas de gubia); y E, audaz, serigrafía sobre papel reciclado (fibras y manchas de tinta
  despareja). En la segunda, el usuario pidió B, con apenas menos polvo, y C en todo el sitio. Al
  llevar C a los mapas, sus puntos caían encima de las raíces y las hacían parecer punteadas, que en
  Raíces quiere decir "en camino": se pasaron detrás antes de mostrarla. La recomendada fue B.
- **Por qué:** el usuario eligió B, "más suave y solo aplicada a elementos de fondo para evitar
  molestar la vista". Es el afiche risográfico del estilo elegido y la única que lleva la textura a
  todas las pantallas, no solo a donde hay tierra. D tenía más carácter, pero sus rayas competían con
  la red de raíces. E, de lejos, parecía sucia más que impresa.
- **Costo:** sobre el papel de fondo, el polvo baja un poco el contraste del texto suelto. La
  verificación pasa AA en las 5 rutas de Raíces. El sol queda liso. En los mapas, la altura de la
  tierra se repite en la hoja de cada pantalla (la línea de tierra del dibujo, en `calc`): si el
  dibujo cambia, hay que cambiarla.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/textura/`: la primera vuelta de cerca y en la portada, y cada
  variante en 360 px (`ronda1-*`); B contra C de cerca, en Seguir entregas y en Necesidades
  (`ronda2-*`); y la aplicada de cerca, en las cuatro pantallas, en el celular y la portada en 360 y
  1280 px (`aplicada-*`); los botones de contorno, lisos (`aplicada-botones.jpg`).

### 2026-11-02 · paleta (Raíces) · Oliva: la tierra en verdes oliva y los frutos mandarina

- **Decisión:** G, Oliva y mandarina. Reemplaza a Bosque, con los mismos valores por función. La
  tierra pasa a tres verdes oliva (`#56633a`, `#4a562e` y `#343d20`), el verde de títulos y botones
  a oliva (`#3d5a2a`) y el papel a un crema más tibio (`#f2ebd7`). Las raíces siguen en paja, un
  poco más clara (`#f3d68c`). Al aplicarla, la tierra se oscureció un poco (de `#5c6a3a`, el valor
  de la G) y la paja se aclaró, para que el texto paja sobre la tierra pase el contraste AA de los
  tokens (4,57:1). Los frutos son mandarina (`#e8822a`), con su segunda tinta (`#96460b`), que también
  dibuja el contorno punteado del fruto que va en camino. Los estados de una entrega no cambian.
- **Alternativas:** dos vueltas en la portada. En la primera, cinco: A, Bosque al sol (los mismos
  verdes, más claros); B, Huerta (tierra de humus marrón y frutos naranja); C, Oliva y mora; D, Riso
  (tierra verde azulada, raíces girasol y frutos rosa flúor que tiñen la cursiva de los títulos, con
  las tintas de un afiche risográfico); y E, Tierra colorada (la tierra roja de Misiones y frutos
  violeta). Al usuario le gustó la tierra oliva de C y le llamó la atención E, pero no los frutos de
  ninguna de las dos. En la segunda, cinco sobre la tierra oliva: F, tomate; G, mandarina; H, perfil
  de suelo (la franja de las cifras en tierra colorada); I, membrillo (raíces hueso); y J, pomelo
  (con la cursiva de los títulos teñida). Al final, G contra H con frutos mandarina, en las pantallas
  de Raíces. La recomendada fue H.
- **Por qué:** el usuario eligió G. Una sola tierra se lee como un sistema. El naranja es casi el
  opuesto del oliva, así que el fruto resalta, y queda lejos del rojo de "con problema". Además, no
  cambia nada fuera de la portada. H sumaba un sexto color y teñía de rojo el borde de las líneas
  del mapa de Seguir entregas.
- **Costo:** la franja de las cifras sigue oscura (un oliva casi negro). El disco del fruto queda
  por debajo de 3:1 sobre el papel, pero lo dibuja su segunda tinta y la cuenta está en texto al
  lado. Por eso el fruto hueco pasa a la segunda tinta. Hay que mirar en Seguir entregas que el
  mandarina no se confunda con el ámbar de "en camino": hoy los frutos están solo en la portada. Las
  otras tres pantallas heredan la paleta sin cambios de estructura.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/paleta-oliva/`: las dos vueltas en 1280 px, G y H en la portada
  en 360 y 1280 px, y Seguir entregas con G y H. Referencia de las tintas de la D: tintas
  Sunflower y Fluorescent Pink en
  [EverythingRiso 2024, SAIC](https://sites.saic.edu/servicebureau/wp-content/uploads/sites/20/2025/03/EverythingRiso_2024.pdf).

### 2026-11-02 · imagen:frutos (Raíces) · En la portada, la planta da frutos con los datos

- **Decisión:** C. El brote de la superficie crece a una planta. Cada fruto impreso es una entrega
  confirmada del mes y el hueco, la que va en camino. Al lado, una etiqueta repite la cuenta
  ("Octubre de 2026 · 3 entregas confirmadas y 1 en camino"). El resto de la portada no cambia.
  Los frutos traen dos valores base nuevos (`--p-fruto` y `--p-fruto-hondo`, su segunda tinta),
  provisorios hasta el A/B de paleta.
- **Alternativas:** primero, cinco papeles para los frutos en láminas: A, solo color; B, el fruto
  como sello de lo cumplido; C, la planta que da frutos; D, raíz y copa (las cifras dentro de
  frutos grandes, colgadas de un árbol cuyo tronco baja a las raíces); y E, el corte de un fruto
  (gajos como tipos de entidad). El usuario pidió C y D mejor dibujadas, y se armaron sobre la
  portada real. La recomendada fue D.
- **Por qué:** el usuario se quedó con C para la portada. Pidió reusar en otras partes del diseño
  la idea de D: frutos grandes con cifras que impactan. Queda como patrón para las cifras de otras
  pantallas, por ejemplo lo acumulado de una empresa. Si se usa, va con el título "Lo que está
  confirmado" y no "dio fruto", porque solo contamos entregas confirmadas.
- **Costo:** con la regla de "una imagen de raíz o siembra por pantalla", la portada ya tiene la
  suya. Si el mes no tiene entregas, la planta queda sin frutos y la etiqueta dice "Todavía no hay
  entregas confirmadas este mes." (el texto del cuaderno vacío). Si las cifras dan error o tardan,
  queda la planta sola, porque el aviso ya está en el cuaderno.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/frutos-portada/` (C y D en 360 y 1280 px).

### 2026-11-01 · componente:periodos (Raíces) · Los períodos: una barra por período

- **Decisión:** en el detalle de una necesidad que se repite, la historia de los períodos es una
  barra que se llena por período, con el nombre a la izquierda y lo que llegó y el estado escritos
  debajo; la del período en curso, punteada, porque todavía no cerró.
- **Alternativas:** A, una raíz con una estación por período, como el último tramo de Confirmar
  recepción, con el sello lleno, a medias, hueco o punteado (la recomendada).
- **Por qué:** los largos de las barras se comparan de un vistazo: se ve qué mes llegó menos.
- **Costo:** se aparta del lenguaje de raíces y estaciones de las otras pantallas; los sellos de
  período salieron de la plantilla.
- **Quién decidió:** usuario (B).
- **Evidencia:** `evidencia/raices/necesidades-periodos/`. La comparación aislada de los períodos
  es `ab-1280.jpg`; las páginas enteras (`a-*` y `b-*`) se capturaron con la lista todavía en
  filas, así que `a-*` es la misma imagen que la A de la lista.

### 2026-11-01 · layout:entidad-necesidades (Raíces) · La lista: una etiqueta de herbario por pedido

- **Decisión:** cada pedido vigente es una etiqueta de herbario, con doble filete y sombra salvia,
  como las de la portada. Desde la tableta van en dos columnas y la abierta ocupa el ancho entero;
  en pantalla grande, la abierta alinea su resumen en columnas y pone los períodos al lado de las
  partes. "Publicar otra necesidad" queda plegado arriba de la lista, como en Cozy.
- **Alternativas:**
  - A, un libro de registro: una fila por pedido a todo el ancho, con las columnas alineadas, como
    la tabla de Cozy y el boceto (la recomendada);
  - para publicar, una columna fija al costado, que no se dibujó.
- **Por qué:** las etiquetas tienen el carácter de Raíces y repiten la pieza de la portada. Publicar
  se hace pocas veces, y la columna le quitaría ancho a la lista.
- **Costo:** comparar las cifras de dos pedidos cuesta más que en una columna alineada.
- **Quién decidió:** usuario (B para la lista; plegado arriba, la recomendada, para publicar).
- **Evidencia:** `evidencia/raices/necesidades-lista/`.

### 2026-11-01 · flujo:necesidades (Raíces) · Los pedidos vigentes con su detalle desplegado, su historia y lo que llega

- **Decisión:** la ficha de Necesidades de la entidad en Raíces
  ([`raices-necesidades.md`](flujos/raices-necesidades.md)) se aparta del boceto de E1 (páginas 26
  y 29 a 31) y del mapa del sitio, y suma contenido.
  - **Decisiones del usuario, en dos rondas de preguntas:**
    - las que se repiten muestran su historia de períodos, con cuánto llegó en cada uno;
    - el detalle se despliega en la misma lista, como el boceto, y no en una página propia de nivel
      3, como dice el mapa del sitio;
    - el detalle suma las entregas para ese pedido, quién donó y el motivo;
    - las cumplidas y terminadas se ven aparte, plegadas.
  - **Desvíos del boceto, del agente:**
    - la columna "Estado" sale de la fila: la lista es de las vigentes y las otras van aparte;
    - los períodos dicen su estado en texto ("Llegó todo", "Llegó una parte", "No llegó nada"),
      cuánto llegó y sus fechas, sin rojo y sin leyenda aparte;
    - el botón "+" pasa a "Publicar otra necesidad";
    - "Recurrencia" pasa a "¿Se repite?", y la fecha solo se pide si es una sola vez.
  - **Del requerimiento y del contrato:** editar y dar de baja (RF-10, que Cozy no tiene); qué
    hace falta, de la lista del catálogo; el motivo, en la descripción obligatoria.
- **Alternativas:**
  - una página propia por necesidad;
  - solo decir que se repite, sin la historia;
  - mostrar solo las vigentes, como Cozy.
- **Por qué:**
  - desplegado, no se pierde de vista el resto de los pedidos;
  - el color solo no alcanza, como anota el boceto, y el rojo de "no llegó nada" culpa;
  - las fechas al pasar el mouse no existen en el celular;
  - "cada semana" y una fecha a un mes se contradicen, y el contrato saca el período de las fechas.
- **Costo:** el contrato no tiene la unidad, la fecha límite de las de una sola vez, la historia
  con lo recibido, las entregas por necesidad, quién donó, las terminadas ni la versión para el
  conflicto: quedan como preguntas para el equipo.
- **Quién decidió:** usuario (contenido, detalle y terminadas) y agente (desvíos del boceto y
  formulario según el contrato, propuestos).
- **Evidencia:** la ficha. La revisión independiente en modo flujo pidió cambios en la ronda 1, con
  cuatro hallazgos bloqueantes, y quedaron corregidos:
  - faltaban los estados de error y de espera al cambiar y al dar de baja, y el del pedido que ya
    no está vigente;
  - el registro contaba la entrega con un problema como "en camino";
  - "¿Se repite?" y "Hasta cuándo" se contradecían, y la fecha de las de una sola vez no estaba en
    el contrato;
  - esta entrada faltaba.

  La ronda 2 dio PASS, con avisos menores aplicados: la baja dice si quedan entregas asignadas, el
  error de datos al cambiar lleva su resumen y cada escenario de la maqueta dice qué estado muestra.

  La pantalla construida pasó `revision-ui` en modo pantalla en la ronda 2. La ronda 1 marcó dos
  bloqueantes: el aviso del estado vacío quedaba debajo del formulario abierto, y el diálogo de baja
  decía "todavía no llegó" de entregas que ya estaban en la puerta. Los dos quedaron corregidos. El
  usuario aprobó la ficha al ver la pantalla. La auditoría de cierre dio PASS.

### 2026-11-01 · componente:cierre-ciclo (Raíces) · Recepción confirmada: la raíz completa llega a tu entidad

- **Decisión:** al confirmar, arriba de la página, del brote del depósito sale la línea de subte de la
  entrega y llega impresa hasta la estación de la entidad, con su tilde y su nombre. En la
  respuesta al formulario la línea se traza una vez y el sello se imprime; con "reducir movimiento",
  aparece completa.
- **Alternativas:** B, un sello "Recibido" con la fecha y la hora estampado sobre la etiqueta, como
  un comprobante, con la raíz solo en el último tramo.
- **Por qué:** el usuario pidió que la persona sienta que se cerró un ciclo, y la raíz que llega lo
  dice sin palabras.
- **Costo:** el dibujo es fijo para el ejemplo; en el sistema se arma con los datos de la entrega.
- **Quién decidió:** usuario (A, la recomendada).
- **Evidencia:** `evidencia/raices/confirmar-cierre/` (a y b en 360 y 1280, y la comparación).

### 2026-11-01 · componente:carga-fotos (Raíces) · Las fotos, en una zona de carga grande

- **Decisión:** las fotos se eligen en un recuadro punteado, como un sello hueco, con la cámara al
  centro (patrón: área para cargar archivos, la del boceto de E1). El campo es el nativo: la cámara
  o la galería del celular, sin JavaScript, y en la computadora se puede soltar la foto.
- **Alternativas:** A, el campo de archivos con su botón, compacto, como un campo más.
- **Por qué:** la foto es la prueba que pide RF-12; se ve como el paso principal.
- **Costo:** en el celular ocupa unos 100 px más.
- **Quién decidió:** usuario (B, la recomendada).
- **Evidencia:** `evidencia/raices/confirmar-fotos/`.

### 2026-11-01 · layout:entidad-seguimiento-confirmar (Raíces) · El acta en dos columnas en pantalla grande

- **Decisión:** en pantalla grande, la etiqueta de la entrega y el último tramo a la izquierda,
  quietos mientras se baja, y el formulario a la derecha. En el celular, una columna.
- **Alternativas:** A, una sola hoja angosta, igual que en el celular.
- **Por qué:** mientras completa, la persona ve qué entrega confirma, y el botón entra casi en la
  primera pantalla.
- **Costo:** ninguno en el celular; en anchos intermedios, como en las otras pantallas, una columna.
- **Quién decidió:** usuario (B, la recomendada).
- **Evidencia:** `evidencia/raices/confirmar-composicion/`.

### 2026-11-01 · flujo:confirmar-recepcion (Raíces) · Una entrega por vez, con su etiqueta, y la raíz que llega al confirmar

- **Decisión:** la ficha de Confirmar recepción en Raíces
  ([`raices-confirmar-recepcion.md`](flujos/raices-confirmar-recepcion.md)) se aparta del boceto de
  E1 (página 28) en cuatro puntos y suma contenido.
  - **Desvíos del boceto, del agente:**
    - se confirma una entrega por vez, no varias juntas, como en Cozy;
    - no tiene la casilla "Disclaimer legal": el texto dice qué pasa al confirmar y quién ve las
      fotos;
    - no tiene el mapa, que ya está en Seguir entregas; queda el último tramo como raíz;
    - "Reportar problema" pasa a ser el enlace "Llegó con un problema".
  - **Decisiones del usuario, en una ronda de preguntas:**
    - la etiqueta de la entrega muestra el último tramo, la necesidad que cubre y quién donó;
    - la pantalla dice quién ve las fotos;
    - al confirmar, la raíz de la entrega se completa hasta la entidad, para que se sienta que se
      cerró un ciclo.
  - **Propuesto por el agente:** si dona una persona, la entidad no ve su nombre.
- **Alternativas:**
  - para un problema, solo confirmar (como Cozy) o dibujar ya el reporte;
  - al terminar, un comprobante sobrio con el sello.
- **Por qué:**
  - confirmar varias juntas complica los errores, y la nota del boceto ya lo advertía;
  - la casilla no decía qué se aceptaba;
  - el mapa repetía Seguir entregas.
  - La etiqueta ayuda a no confundir dos entregas que llegaron el mismo día.
- **Costo:**
  - el contrato no tiene la cantidad de bultos, el remito, la necesidad ni quién donó: queda como
    pregunta para el equipo;
  - la foto puede mostrarse en lo público (RF-01 y RF-02), y la pantalla lo dice.
- **Quién decidió:** usuario (contenido y cierre) y agente (desvíos del boceto, propuestos).
- **Evidencia:** la ficha. La revisión independiente en modo flujo pidió cambios en la ronda 1, con
  seis hallazgos bloqueantes, y quedaron corregidos:
  - el estado "Ya confirmaste esta entrega";
  - el estado "sin conexión", separado del error del servidor;
  - quién ve las fotos, dicho con su alcance real;
  - los textos de los cinco errores de datos;
  - las horas del éxito;
  - el campo de bultos, ahora vacío.

  La pantalla construida pasó `revision-ui` en modo pantalla en la ronda 3. La ronda 1 marcó dos
  bloqueantes: en Recepción confirmada de ropa decía que quedaba una entrega y que no quedaba
  ninguna, y la hora de confirmación no coincidía dentro de la pantalla. La ronda 2 marcó que la
  raíz animada se cortaba en pantallas anchas. Los tres quedaron corregidos. El usuario aprobó la
  ficha al ver la pantalla.

### 2026-11-01 · componente:etapas y layout:portada (Raíces) · Avisos de la revisión: un solo renglón de estado en la tarjeta y la lámina más angosta en el celular

- **Decisión:** el usuario cerró los avisos de diseño que dejó la revisión.
  - **Un solo renglón de estado en la tarjeta:** el estado con su nota baja y queda debajo de la
    barra de etapas, en lugar del nombre de la etapa actual. Antes el estado se leía tres veces
    (sello, renglón y etapa actual).
  - **La lámina "Cómo funciona" en el celular:** la franja del dibujo pasa de 112 a 80 px y el
    texto de cada paso gana 32 px. Desde 600 px de ancho, como antes.
  - **El brote del estado vacío se queda:** en un celular de 640 px de alto, "Ver Mis donaciones"
    queda debajo de la barra del menú y se llega bajando.
- **Alternativas:** en la tarjeta, sacar solo la nota (se perdía "sale de nuevo el 17 de
  octubre") o dejarla igual; en el vacío, sacar el brote en el celular; en la lámina, dejarla igual.
- **Por qué:** la repetición del estado alargaba la tarjeta sin sumar nada. Con la franja angosta,
  el paso se lee en renglones más largos. El brote es la firma de Raíces, y en celulares más altos
  el botón entra.
- **Costo:** el dibujo de la lámina se recorta un poco a los lados en el celular. El enlace a los
  textos legales sigue pendiente: falta esa pantalla en todo el cliente liviano.
- **Quién decidió:** usuario.
- **Evidencia:** sin capturas propias; la verificación visual de las rutas de Raíces pasó sin
  hallazgos, también en 1100 px.
- **Además, sin decisión de diseño:** en el celular, el aviso de error es más apretado y "Volver a
  intentar" entra en la primera pantalla. El aviso de carga ya no es una región viva, porque llega
  con la página (en el sistema va a `#avisos`). Los pictogramas de categoría están una sola vez,
  en `prototipo/raices/iconos.html`. `PrototipoControllerTest` prueba los cuatro estados de
  Seguimiento. Los anchos intermedios suman 1100 px, el quiebre ancho.

### 2026-11-01 · componente:etapas (Raíces) · Las etapas, una barra de progreso con la actual debajo

- **Decisión:** en cada tarjeta de Seguir entregas, las cinco etapas de la donación van en una
  barra de progreso (patrón: barra de progreso con pasos). Son cinco sellos en fila, unidos por una
  línea: lo hecho, un disco paja con el tilde; la actual, el sello del estado de la entrega; lo que
  falta, un círculo hueco. Debajo va solo la etapa actual, con su detalle. Las otras siguen en la
  lista (`ol`, con `aria-current="step"` en la actual), ocultas a la vista, para el lector de
  pantalla.
- **Alternativas:** B, la lista de etapas, una por renglón, con nombre y fecha.
- **Por qué:** el usuario eligió A. Las tarjetas quedan más cortas y el avance se ve de un vistazo;
  el detalle completo está en la donación.
- **Costo:** a la vista no están el nombre ni la fecha de las etapas que no son la actual. El tilde
  de lo hecho va en tinta y no calado, para que se vea sobre el paja.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/etapas/`](evidencia/raices/etapas/).

### 2026-11-01 · microcopy y layout:donante-seguimiento (Raíces) · La pantalla se completa con cifras, categoría y etapas

- **Decisión:** después de los A/B 2 y 3, el usuario vio la pantalla vacía y pidió rondas de
  preguntas. Quedó así:
  - debajo de la bajada, un registro de cifras por estado (en camino, con problema, pendientes) y,
    aparte, las que esperan destino, con enlace a Mis donaciones;
  - en cada tarjeta, la categoría con su pictograma y su nombre, y las etapas de la donación;
  - la más avanzada primero (en camino, con problema y pendiente), también en la numeración del
    dibujo;
  - la entrega con problema, como una más;
  - en el celular, el dibujo chico arriba.
- **Alternativas:** sin cifras; tarjetas solo con el estado; un registro de novedades con la más
  nueva arriba (descartado: cada tarjeta ya dice su última noticia y el historial va en el
  detalle); la entrega con problema primero y con aviso; la más reciente primero; el dibujo plegado
  en el celular.
- **Por qué:** decisiones del usuario; cada una tenía una opción recomendada y la eligió, salvo la
  categoría, que sumó a las etapas.
- **Costo:** la pantalla es más larga; las cifras ocupan un bloque más en el celular. El ícono de
  alimentos repite el de la portada en la plantilla (deuda chica: un fragmento de íconos de Raíces
  cuando haya una tercera pantalla que los use).
- **Quién decidió:** usuario.
- **Evidencia:** la ficha [`flujos/raices-seguir-entregas.md`](flujos/raices-seguir-entregas.md).

### 2026-11-01 · layout:donante-seguimiento (Raíces) · El dibujo arriba y una tarjeta por entrega debajo

- **Decisión:** en Seguir entregas, en pantalla grande, el dibujo va arriba, con 48 rem de ancho,
  para que los nombres de las entidades se lean. Debajo va la lista "Entrega por entrega", con una
  tarjeta por entrega y el mismo número que en el dibujo (patrón: tarjetas, un tema por tarjeta):
  borde de tinta y sombra plana salvia, como los botones. En el celular, lo mismo en una columna.
  Al pasar el mouse o el foco por una entrega, su línea queda llena y el resto se apaga, como la red
  de la portada.
- **Alternativas:** A, el dibujo y la lista lado a lado (patrón: vista dividida de mapa y lista),
  con el dibujo fijo en pantalla mientras se recorre la lista.
- **Por qué:** el usuario eligió B. El dibujo grande se lee sin esfuerzo y las tres entregas quedan
  a la par, en una fila.
- **Costo:** en 1280 × 800 las tarjetas empiezan cerca del pie de la pantalla, y al bajar hasta
  ellas el dibujo sale de la vista; el vínculo al pasar el mouse se aprovecha menos. RF-CL-06 pide
  la lista "al lado" del mapa: acá va debajo, junto al dibujo, como ya iba en el celular. Con más
  de tres entregas, las tarjetas pasan a una segunda fila.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/lista-y-dibujo/`](evidencia/raices/lista-y-dibujo/).

### 2026-11-01 · componente:mapa-entregas (Raíces) · Una red radial de subte, con el brote al centro

- **Decisión:** en Seguir entregas, el dibujo de las entregas en curso es una red radial en espejo,
  la misma figura que la red de la portada (patrón: diagrama de líneas de transporte). El brote del
  depósito va al centro, con su estación. De ahí sale una línea de subte por entrega, con codos a
  45°: una a cada lado y una hacia abajo. Cada línea termina en la estación de su entidad, con el
  sello numerado y el nombre debajo. Lo recorrido va impreso, paja con borde; lo que falta va hueco,
  solo con el contorno, porque todavía no se imprimió. El sello de estado es la estación donde está
  la entrega: la pendiente, en la salida del depósito; la que va en camino, el camión dentro de su
  sello, y la que tiene un problema, el sello óxido donde se cortó el camino.
- **Alternativas:** en la primera vuelta, B, marcadores de mapa (patrón: seguimiento de un envío,
  como el boceto de E1): raíces curvas, lo que falta punteado, un alfiler con el número en cada
  entidad y el camión en un globo. Ganó A, líneas de subte desde el brote a la izquierda, y el
  usuario pidió probar la planta centrada, con raíces simétricas como en la portada. En la segunda
  vuelta, B fue la espina en espejo, la red de la portada en el celular: un tronco hacia abajo y una
  rama por entrega, a un lado y al otro.
- **Por qué:** el usuario eligió la red radial. Habla el mismo idioma que la portada (consistencia,
  heurística 4), y "hueco" es la regla del brief para lo que todavía no pasó. Las raíces curvas se
  habían descartado en la ronda de ilustración de la portada.
- **Costo:** la red radial está pensada para 3 entregas, una por dirección; con más, se llena antes
  que la espina. Solo vale para el dibujo de la maqueta: en el sistema, MapLibre sigue las calles
  (D21) y conserva el estilo, con la línea con borde, el tramo hueco y las estaciones con sello. En
  1280 px el dibujo ocupa unos 540 px y los nombres quedan chicos; queda para el A/B de la lista.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/mapa-entregas/`](evidencia/raices/mapa-entregas/) (`vuelta1-*`
  y `vuelta2-*`).

### 2026-11-01 · layout:armazon-privado (Raíces) · Una columna a la izquierda que se puede plegar

- **Decisión:** en las pantallas privadas de Raíces, en pantalla grande, el menú va en una columna
  de papel a la izquierda (patrón: cajón de navegación fijo), con la cuenta al pie como la etiqueta
  de un herbario. Se ve abierta al entrar y un botón la pliega a una tira angosta y la vuelve a
  abrir; el navegador recuerda la elección. Sin JavaScript, la columna queda abierta. En el celular
  y la tableta, una barra de navegación inferior fija, igual en las dos opciones.
- **Alternativas:** B, una franja de tierra arriba con las secciones como pestañas (patrón: barra
  superior con pestañas); en una primera vuelta, B en el celular era una grilla de dos por dos
  arriba, que se descartó antes de mostrarla por no seguir un patrón conocido; arrancar plegada,
  con un botón "Menú".
- **Por qué:** el usuario eligió A y pidió que fuera desplegable. La columna queda donde se mira
  primero (lectura en F) y crece mejor (el admin tiene 7 secciones); la barra inferior es el patrón
  para 3 a 5 destinos al alcance del pulgar
  ([criterios de la cátedra](criterios-catedra-ux-ui.md), secciones 3 a 5).
- **Costo:** la columna resta 16 rem de ancho al contenido cuando está abierta; un script chico
  (`js/raices-menu.js`) para plegarla.
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/estructura-privada/`](evidencia/raices/estructura-privada/).

### 2026-11-01 · proceso · Los criterios de UX/UI de la cátedra guían el diseño y la revisión

- **Decisión:** el material oficial de la cátedra, "Introducción al diseño UX/UI 2026", queda
  resumido como criterios en [criterios-catedra-ux-ui.md](criterios-catedra-ux-ui.md) y enlazado
  desde las skills `ux-flujo`, `iteracion-ab`, `vista-ui` y `revision-ui`. Cambia cómo trabajan los
  agentes: patrón conocido primero (las variantes A/B comparan patrones conocidos), los 7 factores
  de Morville en cada ficha, wireflow en la ficha, largo de línea de 50 a 75 caracteres, 60-30-10,
  accesibilidad con nombre (POUR, WCAG, Ley 26.653) y una nota de qué probar con personas reales.
- **Alternativas:** seguir solo con la dirección de arte y el catálogo de patrones del repo.
- **Por qué:** pedido del usuario, para que los agentes trabajen de forma más profesional y alineada
  con la cátedra.
- **Costo:** un documento más que mantener; las fichas ya aprobadas no se revisaron con estos
  criterios.
- **Quién decidió:** usuario.

### 2026-11-01 · flujo:seguir-entregas (Raíces) · La cuenta es una empresa y el mapa es un dibujo en la maqueta

- **Decisión:** en "Seguir entregas" de Raíces, la cuenta de ejemplo es una empresa (Distribuidora
  Ejemplo SA), con las donaciones de Ana en Cozy, y su menú no tiene Incentivos (RF-07). El menú
  muestra las secciones del donante; las que Raíces no dibujó abren un aviso con enlace a Cozy. El
  mapa del boceto (pág. 19) se representa en la maqueta con un dibujo SVG fijo: una raíz por entrega
  desde el depósito, con el sello de su estado; en el sistema, ese estilo va sobre MapLibre (D21).
- **Alternativas:** Ana, como en Cozy; un menú con solo lo que existe; un esquema que reemplace al
  mapa también en el sistema (cambiaría la lectura de RF-09 y D21).
- **Por qué:** Raíces habla a las empresas que donan; el menú completo muestra cómo será la
  navegación; el dibujo sigue lo que hizo Cozy en su maqueta y no cambia lo que pide RF-09.
- **Costo:** la cuenta no coincide con la de Cozy (las donaciones sí); el dibujo solo cubre las tres
  entregas del ejemplo.
- **Quién decidió:** usuario (la cuenta y el menú); agente, propuesto (el dibujo)
  ([ficha](flujos/raices-seguir-entregas.md)).

### 2026-11-01 · proceso (Raíces) · Raíces vive como prototipo propio hasta que se elija

- **Decisión:** Raíces no arma todavía su juego de plantillas. Sus pantallas van en
  `templates/prototipo/raices/`, sus hojas en `static/css/prototipo/raices/` (con dos niveles de
  tokens, como las otras) y se ven en `/prototipo/raices/<pantalla>`. La portada se mudó ahí
  desde la variante `concepto-b` y se ve igual (comparación píxel a píxel de sus 16 capturas). Si el equipo la elige, se arma su juego
  completo; si la descarta, se borran sus carpetas.
- **Alternativas:** el juego completo ya (copiar las 18 plantillas del catálogo antes de cualquier
  pantalla); seguir como variantes A/B, que se borran al decidir.
- **Por qué:** se puede seguir diseñando pantalla por pantalla sin escribir tres veces cosas que
  nadie eligió todavía.
- **Costo:** sin catálogo `/estilos` de Raíces; que sus datos de ejemplo coincidan con los de Cozy
  lo vigila la revisión, no un test; mudar la portada.
- **Quién decidió:** usuario (dónde vive); la dirección y los tokens, propuestos por el agente
  ([ADR propuesto](../adr/cliente-liviano/20261101-tercera-alternativa-raices-como-prototipo-propio.md)).

### 2026-11-01 · portada (Raíces) · Tres arreglos de la revisión del pulido

- **Decisión:** la revisión independiente del pulido dio PASS con avisos; el usuario pidió arreglar
  tres.
  - **El punteado del cuaderno** va de sello a sello en pantalla grande: media fila en la entrega
    confirmada de arriba y media en la que va en camino. Antes pasaba 3 px por debajo del sello de
    hoy.
  - **Sin datos,** la etiqueta de tu entidad ya no cuelga de un punteado suelto.
  - **Sin datos,** la bajada de "Lo que viajó este mes" no nombra el mes: dice solo que la entidad
    sube la foto cuando confirma la entrega.
- **Alternativas:** dejarlos como avisos.
- **Por qué:** los dos punteados dibujaban una raíz que no llevaba a nada, y la bajada contradecía
  el aviso de que no hay entregas.
- **Costo:** quedan abiertos dos avisos: la franja de la lámina deja unos 155 px al texto en 360 px
  y falta el enlace a los textos legales (en todo el cliente liviano, desde antes).
- **Quién decidió:** usuario.
- **Evidencia:** sin capturas propias; la verificación visual de la portada y sus tres estados pasó.

### 2026-11-01 · layout:portada (Raíces) · El cuaderno del mes: la foto a la vista

- **Decisión:** cada entrega confirmada de "Lo que viajó este mes" muestra una miniatura de la foto
  que subió la entidad, dentro del botón y con la leyenda "Ver la foto de la entrega" debajo;
  tocarla abre la ventana con la foto grande, como antes. Además se corrigió un corte de la raíz
  en pantalla grande: entre la penúltima entrega confirmada y la que va a hoy faltaba el tramo de
  arriba de la fila (en el celular no pasaba).
- **Alternativas:** B, el botón chico en contorno de hoy, sin miniatura.
- **Por qué:** la foto es la prueba de la entrega; a la vista, se ve sin buscarla, que es lo que
  promete la alternativa ("lo que se ve es el resultado; debajo, todo lo que lo prueba"). Las
  cifras no se compararon: el agente no encontró nada para corregir y el usuario no lo pidió.
- **Costo:** el cuaderno queda más alto (unos 100 px en pantalla grande y 190 en el celular) y
  carga tres imágenes chicas más.
- **Quién decidió:** usuario (A/B del cuaderno, cuarto paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-cuaderno/`](evidencia/raices/portada-cuaderno/) (A y B
  lado a lado en 1280 y 360 px, y cada una entera).

### 2026-11-01 · layout:portada (Raíces) · La lámina: una ilustración en tres franjas, el número una sola vez

- **Decisión:** la lámina "Cómo funciona" es una sola ilustración partida en tres franjas pegadas,
  sin espacio entre ellas: el brote, la raíz que se abre y la punta que llega. Cada franja crece
  con su paso, que va a la derecha, y lleva el número impreso como sello paja (con la segunda
  tinta corrida) sobre su borde izquierdo. El número está una sola vez: se fueron las llamadas y
  las guías del dibujo. El rótulo "Lámina 1" sube a 0,875 rem para leerse.
- **Alternativas:** se recorrieron cuatro rondas.
  1. Números de contorno más grandes contra números como sellos: el usuario eligió los sellos, pero
     sin el número repetido en el dibujo y en la leyenda.
  2. Guías que bajan como rieles hasta el número de cada paso, por la derecha o por la izquierda:
     descartadas porque "no ayudan visualmente".
  3. Tres viñetas separadas, un halo del color de cada sello en el dibujo entero, o números en los
     dos lugares: el usuario pidió mezclar las viñetas con los números en los dos.
  4. La mezcla con el número encima de la ilustración (elegida) o en su propia columna, a la
     izquierda.
- **Por qué:** cada franja muestra la parte del dibujo de la que habla su paso, así que no hacen
  falta líneas ni números repetidos, y la ilustración sigue siendo una. El número encima deja más
  ancho al texto en el celular. Los sellos hablan el mismo idioma que la red y el cuaderno del mes.
- **Costo:** la franja recorta los costados del dibujo cuando el paso es alto (en el celular se
  ven solo el tallo y las raíces del centro). El sello tapa un poco el borde izquierdo de cada
  franja.
- **Quién decidió:** usuario (cuatro rondas, parte del tercer paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-lamina/`](evidencia/raices/portada-lamina/) (una
  comparación por ronda, numeradas del 1 al 4, y las dos versiones finales enteras).

### 2026-11-01 · layout:portada (Raíces) · Arriba: el título grande, al lado de la lámina

- **Decisión:** el título de la portada crece hasta 5,75 rem (antes 4,5) y manda, bien por encima
  de "Quiénes reciben" (3,75 rem). En pantalla grande la columna del texto le gana lugar a la
  lámina (1,1 a 0,9; antes 0,92 a 1,08), que sigue a la derecha.
- **Alternativas:** B, el título de lado a lado como un afiche, con la bajada y la lámina debajo.
- **Por qué:** el título apenas se distinguía de los títulos de sección y el usuario había pedido
  más impacto de afiche. A lo logra sin bajar la lámina fuera de la primera pantalla.
- **Costo:** la lámina queda más angosta; por eso se rehízo (entrada de arriba).
- **Quién decidió:** usuario (A/B de arriba, tercer paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-arriba/`](evidencia/raices/portada-arriba/) (A y B lado
  a lado en 1280 y 1920 px, y cada una entera).

### 2026-11-01 · layout:portada (Raíces) · Red y etiquetas: el sello en la punta

- **Decisión:** cada raíz de la red termina en un sello paja con el ícono de lo que pide la entidad
  (alimentos, ropa o muebles, los mismos del cuaderno del mes) y la segunda tinta corrida. La
  etiqueta de herbario cuelga debajo, sin ojal. Vale en todos los anchos: en el celular, el sello
  asoma sobre cada etiqueta de la espina. Además, en pantalla grande las etiquetas crecen con el
  ancho hasta donde no se pisan (de 8,25 a 9,5 rem), con letra algo mayor, y "Sumar mi entidad" va
  en una sola línea.
- **Alternativas:** A, la etiqueta de hoy con su ojal, solo más grande (con los mismos arreglos de
  tamaño y del botón).
- **Por qué:** el ícono dice qué pide cada entidad antes de leer, une la red con el cuaderno del
  mes y cumple lo que el brief ya decía de las entidades ("un sello paja con la segunda tinta
  corrida"). Las etiquetas de 8,25 rem quedaban chicas en 1920 px y el botón se partía en dos
  líneas.
- **Costo:** seis sellos más en la tierra; en el celular, más aire entre filas para que el sello
  no toque la etiqueta de arriba.
- **Quién decidió:** usuario (A/B de la red, segundo paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-red/`](evidencia/raices/portada-red/) (A y B lado a
  lado en 1280 y 360 px, y cada una entera).

### 2026-11-01 · layout:portada (Raíces) · En el celular, la espina en espejo

- **Decisión:** debajo de 1100 px, donde la red no entra, un tronco baja del brote por el centro y
  las etiquetas cuelgan de él de a dos, cada una con una rama corta hasta un cruce, como la red en
  espejo de la pantalla grande. Al final, el tronco sigue punteado hasta tu entidad. Hasta 1100 px
  quedan dos columnas (antes eran tres desde 600 px). La pantalla grande no cambia.
- **Alternativas:** B, la línea al costado: el tronco dobla a la izquierda y cada entidad es una
  estación, una debajo de la otra, con lo que pide a la derecha.
- **Por qué:** antes las etiquetas colgaban sueltas, sin unirse al brote, y en el celular no se
  veía lo que la red cuenta. A repite la forma de la red grande y es la más corta. B se leía como
  lista, pero repetía la raíz al costado del cuaderno del mes, justo debajo.
- **Costo:** las etiquetas son algo más angostas (la rama ocupa el centro); "Pide ropa de abrigo"
  va en dos líneas.
- **Quién decidió:** usuario (A/B del celular, primer paso del pulido de la portada).
- **Evidencia:** [`evidencia/raices/portada-celular/`](evidencia/raices/portada-celular/) (A y B
  lado a lado y cada una entera, en 360 px).

### 2026-11-01 · portada (Raíces) · Foto en ventana, estados de la portada y botón del mapa

- **Decisión:** tres arreglos de la revisión independiente, elegidos por el usuario.
  - **La foto:** "Ver la foto de la entrega" abre una ventana sobre la portada con la foto, qué se
    entregó, la entidad, la fecha de la confirmación y "Cerrar". Es el popover del navegador: se
    abre y se cierra sin JavaScript, también con Esc o tocando afuera.
  - **Los estados:** las entidades, las cifras y las entregas del mes tienen estado vacío, de
    error y de carga (`?escenario=vacio`, `error` y `lento`). Cada aviso ocupa el lugar de los datos
    y dice qué pasa; el de error suma "Volver a cargar". Sin datos no hay red y la etiqueta de tu
    entidad queda sola, con su botón.
  - **El mapa:** "Ver el mapa de entregas confirmadas" pasa de enlace chico a un botón chico en
    contorno, como el de la foto.
- **Alternativas:** la foto en una pestaña nueva o en una pantalla propia; solo el estado vacío,
  o vacío y error sin el de carga; el enlace del mapa más alto pero con aspecto de enlace.
- **Por qué:** la ventana no saca a la persona de la portada y muestra la foto con lo que prueba.
  Los estados siguen el tono común (qué pasa, qué se sabe y qué hacer). El botón cumple los 48 px
  de toque y repite un control que ya está en la página.
- **Costo:** en un navegador sin popover (anteriores a 2024) el botón de la foto no abre nada.
  El estado de carga casi no se ve en una página que arma el servidor.
- **Quién decidió:** usuario (una ronda de tres preguntas).
- **Evidencia:** [`evidencia/raices/portada-segunda-iteracion/`](evidencia/raices/portada-segunda-iteracion/)
  (los tres estados, en 360 px).

### 2026-11-01 · portada (Raíces) · Segunda iteración: la mezcla de la actual y la lámina de campo

- **Decisión:** la portada de Raíces es D, una mezcla de la versión actual (A) y la lámina de
  campo (C), diseñada con el usuario en dos rondas.
  - **Arriba:** el título de A ("Lo que donás echa raíces en tu comunidad.") a la izquierda; a la
    derecha, una lámina "Cómo funciona": el brote y sus raíces con tres llamadas numeradas, y su
    leyenda en texto (registrás lo que donás, se asigna a una entidad, la entidad confirma la
    entrega con fecha y foto).
  - **En la tierra:** el rótulo "Quiénes reciben las donaciones" va en papel, sobre un brote con su
    sol. Del brote sale la red radial de A, que termina en etiquetas de herbario: tipo, nombre y lo
    que pide cada entidad. La etiqueta vacía de "Tu entidad" queda al centro.
  - **Cifras:** un registro de campo, cada una con su nombre completo y una línea guía punteada
    hasta el número.
  - **El mes:** un cuaderno de entregas, una fila por entrega, que baja de la primera a hoy.
  - **En el celular:** las etiquetas cuelgan de a dos, sin la red.
- **Alternativas:** A, la actual (título centrado, red radial con sellos y nombres chicos, cifras
  gigantes y la línea del mes de lado a lado). B, el afiche: el título gigante cruza un sol, con
  pasos 01, 02 y 03 al lado; las entidades cuelgan de una línea de subte horizontal; las cifras
  van en una frase y los días del mes son numerales gigantes. C, la lámina de campo, con el título
  "Seguí tu donación hasta donde echa raíces" y siete etiquetas en fila colgadas de raíces curvas.
- **Por qué:** el usuario pidió más impacto de afiche, un mensaje más claro y una red más
  legible. No eligió ninguna de las tres y armó la mezcla en dos rondas: hero de C con el título
  de A, red de A con etiquetas de C, y el registro y el cuaderno de C. La lámina explica qué se
  puede comprobar detrás de la metáfora del título, como pide el tono común. Las etiquetas hacen
  legible la red y muestran a la entidad haciendo algo (pide y confirma).
- **Costo:**
  - El hero deja de ser el afiche centrado con el sol grande; el brote y el sol bajan al rótulo de
    la tierra.
  - "Pide …" es un dato nuevo de cada entidad (sintético en la maqueta). Si la alternativa se
    construye, sale de las necesidades que carga la entidad.
  - Se fueron las franjas de semitono entre horizontes.
  - Falta la revisión independiente (`revision-ui`) de la portada nueva.
- **Quién decidió:** usuario, sobre capturas de las cuatro versiones en 360, 1280 y 1920 px (dos
  rondas de preguntas para armar D).
- **Evidencia:** [`evidencia/raices/portada-segunda-iteracion/`](evidencia/raices/portada-segunda-iteracion/)
  (B, C y la final, en 360 y 1280 px). A, en
  [`evidencia/raices/portada-entidad-y-mes/`](evidencia/raices/portada-entidad-y-mes/).

### 2026-11-01 · lo que viajó (Raíces) · La línea del mes, con un cuadro por categoría

- **Decisión:** "Lo que viajó este mes" va debajo de la tierra, otra vez en papel. Las entregas
  del mes cuelgan de una línea de subte paja con borde verde oscuro, como la red de arriba, por
  fecha, y la línea termina en "Hoy", con un tramo punteado hasta la donación que va en camino. Cada
  entrega muestra un cuadro salvia claro (el color del sol) con un ícono mínimo y centrado de su
  categoría, con la segunda tinta corrida. Las categorías son las del catálogo: alimentos, ropa y
  muebles. La que va en camino se dibuja hueca, solo con el contorno. Las confirmadas llevan un
  botón chico, en contorno, que dice "Ver la foto de la entrega". En el celular la línea baja por
  la izquierda.
- **Alternativas:** lámina de muestras (fotos con etiqueta de herbario); pliego en dos tintas (las
  fotos en verde y paja, a color al pasar el mouse); la línea con fotos; la línea con
  ilustraciones sueltas, sin cuadro; el cuadro con fondo verde bosque e ícono paja; el enlace de
  texto a la foto.
- **Por qué:** la línea repite el lenguaje de la red y se lee como un recorrido que termina hoy. Lo
  que falta se ve hueco, como el sello de lo que todavía no se imprimió. El usuario prefirió
  ícono a foto en la portada, en cuadro y con fondo. La foto es la prueba de la entrega y la bajada
  del hero la promete, así que queda a un botón de distancia, sin competir con "Quiero donar".
- **Costo:** la portada ya no muestra la foto. Hay un ícono por categoría y ninguno por
  subcategoría. La foto de las sillas es una ilustración sintética nueva
  (`img/raices/ejemplo-entrega-sillas.svg`).
- **Quién decidió:** usuario, sobre láminas dibujadas en el chat (C entre tres; después, el cuadro
  con fondo y el botón).
- **Evidencia:** [`evidencia/raices/portada-entidad-y-mes/`](evidencia/raices/portada-entidad-y-mes/).

### 2026-11-01 · camino de la entidad (Raíces) · Un lugar vacío al centro de la red

- **Decisión:** "Sumar mi entidad" va dentro de la red de la tierra: una línea punteada baja por
  el centro hasta un sello hueco, con el rótulo "Tu entidad" y el botón paja. Al pasar el mouse o
  el foco por el botón, su línea se completa y las demás se apagan. En el celular va al final de la
  lista de entidades, a todo el ancho, con borde punteado.
- **Alternativas:** una línea de texto debajo de "Quiero donar" e "Ingresar"; una franja propia al
  pie con los tres pasos de la entidad; las dos primeras juntas.
- **Por qué:** la red ya responde "¿a quién llega lo que dono?". El lugar vacío le dice a la entidad
  que puede ser la próxima, con el mismo lenguaje (lo hueco es lo que todavía no se imprimió).
- **Costo:** queda debajo de la primera pantalla. La entidad que no baja no lo ve.
- **Quién decidió:** usuario (la recomendada).
- **Evidencia:** [`evidencia/raices/portada-entidad-y-mes/`](evidencia/raices/portada-entidad-y-mes/).

### 2026-11-01 · movimiento (Raíces) · La página se imprime y la red se traza

- **Decisión:** al entrar, una vez, la portada se imprime: el sol aparece y la segunda tinta entra
  corrida y calza (1 s). Después la red se traza desde el brote hasta cada entidad (1 s). Al pasar
  el mouse o el foco por una entidad, su línea queda llena y el resto se apaga. Todo en CSS, sin
  JavaScript; con "reducir movimiento" no se mueve nada.
- **Alternativas:** solo el trazado; solo la imprenta; un pulso de entrega (puntos que recorren las
  líneas cada tanto); sin resaltado.
- **Por qué:** la imprenta es lo más propio del estilo riso y el trazado cuenta el concepto (lo que
  se dona llega a cada entidad). Las dos terminan solas. El pulso no termina nunca: WCAG 2.2.2 pide
  un botón de pausa, distrae de la lectura y sugiere datos en vivo que la maqueta no tiene.
- **Costo:** unos 2 s hasta ver la red completa. Con una conexión lenta, el corrimiento de la
  imprenta puede leerse como un error de carga.
- **Quién decidió:** usuario, sobre tres animaciones en vivo.
- **Evidencia:** [`evidencia/raices/lenguaje-visual/`](evidencia/raices/lenguaje-visual/).

### 2026-11-01 · ilustración (Raíces) · Las raíces son una red radial hacia las entidades

- **Decisión:** en el hero, las raíces dejan de ser las etapas de una donación y pasan a ser las
  entidades beneficiarias: una red radial que sale del brote, con líneas paja de borde verde oscuro,
  codos a 45° y estaciones, en espejo, hacia una entidad de cada tipo (comedor, escuela,
  merendero, hogar, club y biblioteca). Cada entidad es un sello con su tipo y su nombre; en el
  celular, los rótulos van en lista debajo del dibujo. El brote son dos cotiledones planos con la
  segunda tinta corrida; el sol, un medio disco plano. Las etapas salen de la portada y el rótulo
  "Todo lo que pasa hasta que llega" se va con ellas: la tierra dice "Quiénes reciben las
  donaciones".
- **Alternativas:** el grabado de lámina, el plano risográfico, la red de mapa y la mezcla de las
  dos últimas (primera ronda). Pedido del usuario: raíces hacia entidades y una versión orgánica
  fiel a una raíz real. Después, la red y la orgánica en versión simétrica, y cuatro variantes más:
  abanico, bifurcada, fila de lámina y red radial. Para las etapas: una sección debajo del hero o
  una raíz con marcas. Para las entidades: las de más entregas o las más recientes, y solo el tipo
  como rótulo.
- **Por qué:** el usuario quiere que la raíz muestre a quién llega lo donado, con simetría y algo
  placentero de ver. Eligió la red radial: une raíz y logística, el pedido original de esta
  alternativa. Una entidad por tipo muestra la variedad de la comunidad.
- **Costo:** la red es más fría que una raíz orgánica frente al título cálido. Con una entidad por
  tipo, el grosor ya no compara volumen (las cifras quedan en el pie). Las etapas no tienen todavía
  dónde verse. Los seis enlaces van al mismo mapa del prototipo. `revision-ui` marcó que la
  variante, desde antes, cubre solo el hero: le faltan "Sumar mi entidad" y "Lo que viajó este
  mes" (brief, "Pendientes de la portada").
- **Quién decidió:** usuario.
- **Evidencia:** [`evidencia/raices/lenguaje-visual/`](evidencia/raices/lenguaje-visual/)
  (`red-radial-*.jpg`).

### 2026-11-01 · formas (Raíces) · Estados como sello y botón que se hunde

- **Decisión:** los estados se dibujan como sello: lo cumplido, un disco paja lleno con la segunda
  tinta corrida y el tilde calado; en camino, medio círculo ámbar; pendiente, círculo punteado;
  con problema, disco óxido con "!". El numeral de lo que falta va hueco. Los botones siguen rectos
  con sombra salvia corrida y, al apretarlos, se hunden sobre su sombra.
- **Alternativas:** para los estados, la marca de lámina (trazo fino, lo que falta apagado) y la
  etiqueta recta (cuadrados). Para los botones, el registro corrido (un contorno salvia que no
  calza) y la etiqueta de lámina (esquina cortada y doble filete), vistos en tamaño real en
  reposo, al apretar y con foco.
- **Por qué:** el sello pone la audacia en los datos y se lee de un vistazo (lleno, comprobado;
  hueco, falta). El botón que se hunde continúa el mismo gesto y deja limpio el foco. El registro
  corrido sumaba tres marcos con el foco.
- **Costo:** el numeral hueco de 18 px puede verse frágil en el celular; hay que probarlo cuando las
  etapas tengan pantalla. Hoy ninguna pantalla de Raíces usa los estados.
- **Quién decidió:** usuario.
- **Evidencia:** sin capturas de los estados (no hay pantalla que los use); el botón, en
  [`evidencia/raices/lenguaje-visual/`](evidencia/raices/lenguaje-visual/).

### 2026-11-01 · tierra (Raíces) · Semitono en lugar de grava

- **Decisión:** al pie de cada horizonte de la tierra, una franja de semitono: puntos de imprenta
  con el color del horizonte de abajo, que crecen con lo hondo y con la pantalla (franja de 12 a
  44 px, trama de 7 a 11 px). Nunca debajo de texto. La grava sale del código.
- **Alternativas:** grano solo (tierra lisa) y la grava que estaba en el código; el semitono más
  discreto (trama fija de 7 px), probado primero en la maqueta.
- **Por qué:** es lo único que cumple "que sorprenda" sin poner ruido debajo de los datos, y une
  los horizontes sin la línea descartada. El usuario lo pidió más marcado después de verlo en la
  maqueta.
- **Costo:** axe no mide el contraste sobre los puntos; por eso los puntos nunca van debajo de
  texto.
- **Quién decidió:** usuario, en lámina y después en la maqueta.
- **Evidencia:** [`evidencia/raices/lenguaje-visual/`](evidencia/raices/lenguaje-visual/)
  (`semitono-*.jpg`).

### 2026-10-31 · proceso · El brief de tono del equipo rige a todas las alternativas

- **Decisión:** el *Brief de Tono y Comunicación v1.0* del equipo
  ([PDF](identidad/brief-tono-y-comunicacion-v1.0.pdf)) pasa a ser regla común de la interfaz, en
  [tono y comunicación](tono-y-comunicacion.md): audiencias por rol, tono por pantalla, nombres de
  los estados (lo pendiente se dice pendiente), cifras con período y fuente, personas por su rol y
  un filtro de 7 preguntas. Lo citan la dirección de arte, patrones y textos, el README, las
  plantillas, las skills y `cliente-liviano/AGENTS.md`; `revision-ui` lo revisa en U5, que pasa a
  llamarse "Textos y tono". Las alternativas siguen libres en metáfora, arte, paleta, letra y voz
  de títulos y rótulos.
- **Alternativas:** volcar el tono dentro de cada brief de alternativa, sin documento común; o
  ajustar solo patrones y textos.
- **Por qué:** el brief se escribió para que todas las exploraciones digan lo mismo aunque se vean
  distinto. Un solo documento evita que cada alternativa invente su voz de base.
- **Costo:** los briefs de Glassmorphism y Cozy / Medieval tienen frases y textos que chocan
  ("números de impacto", tono festivo, "Tu impacto", "Tu huella", "Donar ahora" en la portada de
  Cozy, cifras sin período). Por ahora solo llevan una nota con la lista; corregirlos queda para
  después, a decisión del usuario. Reemplaza, como base de la voz, las tres líneas de "Voz y tono"
  de patrones y textos y lo común de la sección Voz de cada brief (decididos por el usuario en las
  rondas de identidad); la voz propia de títulos y rótulos de cada alternativa sigue igual.
- **Quién decidió:** usuario.
- **Evidencia:** [brief de tono v1.0](identidad/brief-tono-y-comunicacion-v1.0.pdf).

### 2026-10-31 · textos (Raíces) · La portada adopta el tono común

- **Decisión:** en la portada de Raíces (concepto B) el botón principal dice "Quiero donar" (el
  mismo texto que el del donante en la portada de Cozy); la bajada pasa a "Seguí cada donación desde
  que se registra hasta que la entidad confirma la entrega. DonaTrack te muestra a quién se asignó,
  en qué estado está y la foto que sube la entidad al confirmar."; las etapas usan los nombres de
  los estados (registrada, asignada, preparada para entregar, en camino) y la última, que todavía
  no ocurrió, dice "Falta confirmar la entrega. La confirma el comedor con la foto"; las cifras son
  "entregas confirmadas" y "donaciones en camino", con período, fuente y año. Reemplaza el botón
  "Donar ahora" y la bajada "Cada donación llega a un comedor, una escuela o un merendero…", que
  eligió el usuario en el segundo A/B de Raíces (mensaje). El título "Lo que donás echa raíces en
  tu comunidad" y el rótulo "Todo lo que pasa hasta que llega" se mantienen; el rótulo afirma la
  llegada y queda para revisar cuando Raíces tenga su tabla de voz.
- **Alternativas:** "Sumate a la iniciativa" o "Donar" para el botón; dejar "Donar ahora"; ajustar
  también el título.
- **Por qué:** "ahora" es urgencia artificial; "cada donación llega" promete algo que la plataforma
  no garantiza; "Recibida" se leía como cumplida estando pendiente; una cifra sin período se presta
  a malas lecturas. La metáfora del título es libre y la bajada dice qué se puede comprobar.
- **Costo:** la portada de Raíces deja de tener el mismo botón principal que la de Cozy ("Donar
  ahora"), hasta que se revisen los textos de Cozy.
- **Quién decidió:** usuario (el botón, mantener el título y corregir la bajada); la redacción de
  la bajada, las etapas y las cifras es del agente, con el brief de tono.
- **Evidencia:** [`evidencia/raices/tono-portada/`](evidencia/raices/tono-portada/).

### 2026-10-31 · tipografia (Raíces) · Grotesca ancha de afiche: Archivo en todo

- **Decisión:** la letra de Raíces es Archivo (Omnibus-Type, Buenos Aires), una sola familia con
  sus anchos. El título, los numerales, las cifras y la marca van anchos y pesados, como las letras
  gigantes de la referencia 02. La parte cálida del título ("en tu comunidad") y "Todo lo que pasa
  hasta que llega" van en su cursiva. El texto y los botones van en ancho normal o apenas ancho.
  Reemplaza las provisorias: Alegreya era la letra de Cozy y DM Sans la de Glassmorphism. Está
  aplicada en `variantes/comun/raices.css`, con dos variables, `--p-letra-titulo` y
  `--p-letra-texto`, y en `variantes/concepto-b.css`. Tiene licencia OFL y se sirve desde
  `static/fuentes/` junto a su licencia (`@fontsource-variable/archivo` 5.3.0, subconjunto
  latino).
- **Alternativas:** B, Clarendon de paquete de semillas: Besley en los títulos, los numerales y las
  cifras, con su cursiva, y Archivo en el texto. Era más cálida y estaba más cerca de lo que ya
  había. El usuario la eligió primero y después la cambió por A, antes de cerrar la decisión.
- **Por qué:** A es la que más sorprende y la que mejor separa Raíces de las otras dos
  alternativas: sin remates, no se confunde con Cozy; ancha y pesada, no se confunde con
  Glassmorphism. Además, es la recomendada por el agente.
- **Costo:** la calidez ya no está en la letra. Queda en el dibujo, los colores, el grano y la
  segunda tinta corrida del título. Las cifras anchas ocupan más lugar, así que van con un pequeño
  aire antes de su rótulo. Las dos variantes pasaron la verificación AA. Sigue el A/B de la grava.
- **Quién decidió:** usuario, con la recomendación del agente (A), después de elegir primero B.
- **Evidencia:** `evidencia/raices/tipografia-portada/`.

### 2026-10-31 · proceso · La verificación visual mide el contraste que axe deja sin medir

- **Decisión:** donde axe no puede calcular el contraste (por un pseudo-elemento, un degradado o
  una imagen de fondo), `-Pvisual` lo mide con el color de fondo de los contenedores, sin ver lo que
  pinta el pseudo-elemento, y falla por debajo de AA. No mide el texto que está sobre un degradado
  lineal o sobre una imagen sin color opaco debajo.
- **Alternativas:** fallar con cada caso que axe no midió (8.127 en las dos alternativas: todo en
  rojo); solo listarlos en el reporte (la regresión de Raíces seguía pasando); cambiar el CSS para
  que axe pueda medir (rehacer diseños aprobados).
- **Por qué:** `ed3fe14c` dejó el texto del suelo de Raíces en 1,6:1 y `-Pvisual` pasó igual. Con la
  medición nueva, ese caso falla en los tres anchos. Además encontró el título de "Ranking" de Cozy
  directo sobre la madera (1,5:1): ahora va en una nota, como pide la regla de la mesa.
- **Costo:** el texto sobre un degradado lineal o una foto sigue sin medirse automáticamente y queda
  para la revisión a ojo. La medición tampoco ve el fondo que pinta un pseudo-elemento.
- **Quién decidió:** usuario.
- **Evidencia:** sin capturas; las cifras salen de las corridas de `-Pvisual` del 2026-10-31 (la
  completa, con 0 fallas, y la de Raíces sin el color del suelo, con 30 fallas de contraste).

### 2026-10-31 · estilo (Raíces) · Impreso editorial: un afiche risográfico

- **Decisión:** el estilo visual de Raíces es el impreso editorial (referencias 01 y 02): grano de
  impresión marcado sobre el papel, el sol y la tierra; un medio sol salvia claro que sale de la
  tierra detrás del brote; el título más grande, con "en tu
  comunidad" corrida por una segunda tinta salvia; numerales y cifras gigantes en cursiva; botones
  rectos con una sombra plana salvia corrida. Aplicado en `variantes/comun/raices.css` (grano,
  título y botones) y en `variantes/concepto-b.css` y `templates/variantes/concepto-b.html` (sol
  y numerales). La grava de la franja de las etapas venía con B y sigue en el código, pero **no**
  está aprobada: el usuario pidió decidirla en su propio A/B. Suma el tema `estilo` al
  [catálogo](temas-ab.md).
- **Alternativas:** A, lámina de campo (referencia 05), recomendada por el agente: un marco fino
  que cruzaba el corte de suelo, cuadrícula tenue, "Ficha N.º 0412", una regla de días debajo de
  las raíces y fechas y cifras en letra de máquina. Decía "registrado y comprobable". El usuario
  había descartado antes Minimalista y Moderno.
- **Por qué:** el usuario eligió la más cálida y memorable, la que tiene oficio y comunidad. Sigue
  el gusto que ya había marcado por la textura de la referencia 01.
- **Costo:** la transparencia ya no está en el estilo; queda en el dibujo del suelo, las fechas y
  la bajada. Se acerca a Cozy (cálida) y al afiche "eco" de la referencia 02, así que la voz sigue
  sin imágenes verdes y la letra tiene que separarla. La segunda tinta corrida se usa solo en el
  título: repetida, parece un efecto barato. El sol, propuesto en la paleta, queda aprobado con
  este estilo; la grava queda pendiente de su A/B y la línea entre horizontes no entró. Las dos
  variantes pasaron la verificación AA. Siguen el A/B de la grava y el de la letra.
- **Quién decidió:** usuario, contra la recomendación del agente (A).
- **Evidencia:** `evidencia/raices/estilo-portada/`.

### 2026-10-31 · paleta (Raíces) · Bosque: la tierra en tres verdes de monte y raíces color paja

- **Decisión:** la paleta de Raíces es Bosque, solo los colores. Arriba, papel crema (`#eeebde`),
  tinta (`#18211b`) y verde bosque (`#234a35`) en el título en cursiva y los botones. Abajo, la
  tierra en tres horizontes verdes, cada vez más hondos (`#2a4a37`, `#1d3628` y `#12241a`): las
  raíces arriba, las etapas en el medio y las cifras abajo. Las raíces, las etapas cumplidas y
  las cifras van en color paja (`#e6c27a`); la etapa en camino, en crema (`#f1ead2`); el brote, en
  salvia (`#9cb889`). Estados de una entrega, siempre con texto y una forma propia: pendiente en
  gris tierra con círculo punteado, en camino en ámbar con medio círculo, entregada en verde con
  tilde y con problema en rojo óxido con "!". Aplicada en `variantes/comun/raices.css`, con los
  valores por función (`--p-suelo`, `--p-raiz`, `--p-acento`…) para que la base no dependa del
  tono.
- **Alternativas:** tres rondas. Ronda 1: Herbario (pino y liquen), Tierra colorada (arcilla y
  ladrillo) y Cianotipo (azul de Prusia y ocre), recomendada por el agente. El usuario pidió
  priorizar el diseño gráfico sobre la accesibilidad ("en el futuro habilitaremos un modo
  accesible") y comparar el verde con un marrón. Ronda 2: Herbario, Humus (tierra marrón, verde
  solo en lo que llegó), Musgo y humus (horizonte verde arriba, tierra marrón abajo) y Humus con
  brote (tierra marrón con títulos y botones en verde), recomendada por el agente. Ronda 3, con más
  ambición gráfica: Humus con brote contra Bosque, la heredera de la paleta provisoria. Las dos
  tenían un sol detrás del brote, una línea de horizonte y grava en la franja del medio.
- **Por qué:** el usuario eligió la de verdes, la más cercana a la original: botánica y de lámina
  de campo, con la profundidad de los tres horizontes.
- **Costo:** es la más cercana al tono "eco" que el brief descarta y, con el paja sobre el bosque,
  a Cozy (bosque y latón); lo separan la forma, la letra y la voz, que siguen abiertas. El sol, la
  línea de horizonte y la grava **no** se aprobaron (el usuario aprobó "únicamente la paleta") y se
  sacaron; quedan para el A/B del estilo visual. Con la prioridad gráfica, las tres rondas igual
  pasaron la verificación AA. Las capturas de la ronda 3 se perdieron con un `mvn clean`; de esa
  ronda quedan la Humus con brote sin sol (ronda 2) y la elegida aplicada. De la ronda 2 quedan
  Humus y Humus con brote: Herbario es la misma de la ronda 1 y Musgo y humus se sobrescribió.
- **Quién decidió:** usuario, contra la recomendación del agente (Cianotipo y después Humus con
  brote).
- **Evidencia:** `evidencia/raices/paleta-portada/`.

### 2026-10-31 · microcopy:portada (Raíces) · "Lo que donás echa raíces en tu comunidad"

- **Decisión:** el mensaje del hero es el C, "la comunidad que crece". Título: "Lo que donás echa
  raíces en tu comunidad." Bajada: "Cada donación llega a un comedor, una escuela o un merendero.
  DonaTrack te muestra a quién, cuándo y cómo, con la foto de la entrega." Rótulo de la tierra:
  "Todo lo que pasa hasta que llega". Aplicado en `templates/variantes/concepto-b.html`.
- **Alternativas:** A, "la prueba a la vista" ("Debajo de cada donación, todo a la vista"),
  recomendada por el agente porque el título describe el dibujo y le habla a todos; B, "directo a
  la empresa" ("Tu empresa dona. Acá se comprueba").
- **Por qué:** el usuario prefirió la más cálida, la que pone en el centro a las personas que
  reciben. La transparencia queda en el dibujo del suelo y en la bajada ("a quién, cuándo y cómo").
- **Costo:** la transparencia ya no está en el título; "raíces en tu comunidad" está cerca del tono
  "eco" que el usuario descartó, así que el resto de la página no suma imágenes verdes en los
  textos (regla de voz del brief).
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/raices/mensaje-portada/`.

### 2026-10-31 · concepto (Raíces) · El hero de la portada es un corte de suelo

- **Decisión:** el concepto rector de la Alternativa Raíces, una tercera alternativa en
  exploración ([brief](identidad/brief.md)), es el corte de suelo. Arriba, poco: el título,
  las acciones y un brote. Abajo, en la tierra, las cinco etapas de una donación como raíces, cada
  una con su rótulo de lámina. Queda como base de las próximas comparaciones
  (`templates/variantes/concepto-b.html`).
- **Alternativas:** A, la red que es mapa: un sol salvia del que sale una raíz que cruza la página,
  con una punta por cada entidad adonde llegó lo donado en octubre.
- **Por qué:** el público foco, la persona de responsabilidad social de una empresa, quiere poder
  demostrar adónde fue su donación. B lo cuenta de un vistazo: lo que se ve es el resultado y
  debajo está lo que lo prueba. A decía "llegamos a muchos" más que "podés comprobarlo".
- **Costo:** el dibujo es fijo para el caso de ejemplo; en la alternativa real se dibuja desde los
  datos. La paleta y las letras siguen provisorias. Antes de construir la alternativa hace falta un
  ADR `proposed` para la tercera alternativa.
- **Quién decidió:** usuario, con la recomendación del agente.
- **Evidencia:** `evidencia/raices/concepto-portada/`.

### 2026-10-31 · proceso (Raíces) · Una tercera alternativa, diseñada con el usuario por A/B

- **Decisión:** la identidad de raíces y semillas para empresas que donan es una tercera
  alternativa. Se diseña con el usuario, en comparaciones A/B que empiezan por el hero de la
  portada, en lugar de una corrida creativa desatendida.
- **Alternativas:** reemplazar a Cozy o a Glassmorphism; una corrida desatendida de tres
  conceptos (relevamiento hecho hasta la ronda de la corrida).
- **Por qué:** el usuario quiere tomar las decisiones de estilo y mensaje en cada paso; el hero
  sirve después para orientar todo el sitio.
- **Costo:** cada pantalla nueva se escribiría tres veces; el ADR de dos alternativas necesita un
  sucesor.
- **Quién decidió:** usuario.

### 2026-10-31 · proceso · Diseño creativo desde cero: relevamiento con el usuario y corrida desatendida

- **Decisión:** la forma de trabajar que rehízo Cozy / Medieval queda como proceso. La skill
  `relevamiento-creativo` le pregunta al usuario por sus referencias, el mensaje, las libertades y
  hasta dónde llegar, y deja un encargo creativo aprobado en `encargos/`. La skill
  `corrida-creativa` (con su workflow guardado) trabaja sola: iteraciones que escalan (concepto,
  sistema y flujo), conceptos opuestos construidos en paralelo, críticos de arte, piso y flujo que
  no diluyen la audacia, decisión con matriz, arreglos, evidencia, bitácora y un commit de control
  por iteración, y al final la página de antes y después, la auditoría y el PR si el encargo lo
  autoriza. La dirección de arte suma "Libertad creativa" y "Oficio"; las plantillas, los tres
  encargos; la verificación visual, `-Dverificacion.paginas` y la primera pantalla del celular
  (360 × 640); y `scripts/visual/` junta las herramientas de imagen.
- **Alternativas:** extender solo `identidad-visual`, `iteracion-ab` y `revision-ui` (mezcla lo que
  necesita al usuario con lo que no, en skills que ya son largas); un workflow con los encargos
  adentro (esconde las reglas en un script y no sirve para trabajar a mano); skills genéricas en el
  repositorio de harness desde el principio (pierden el piso automático y los tests, que viven en
  este repo).
- **Por qué:** el primer intento de Cozy salió tímido por encargos de "cambio mínimo", variantes del
  mismo orden y críticas que sacaron lo llamativo; lo que funcionó fue el encargo creativo
  explícito, la divergencia real y los guardarraíles automáticos. Escrito, se repite sin depender
  de una sesión.
- **Costo:** una corrida completa usa decenas de agentes (unos 38 con tres iteraciones y dos
  conceptos). La de prueba, con una iteración y dos conceptos, usó 15 agentes, unos 2 millones de
  tokens de los subagentes y 74 minutos. La iteración de flujo depende de `PrototipoController`, que hoy sirve solo a Cozy /
  Medieval. Para comparar conceptos sobre una pantalla del prototipo, las variantes de estructura
  ahora también la reemplazan (`?variante=` en `/prototipo/<pantalla>`).
- **Quién decidió:** usuario (aprobó la propuesta, opción A, el 2026-10-31).
- **Evidencia:** una corrida de prueba sobre la portada pública de Cozy (una iteración, dos
  conceptos, sin PR) en la rama local `prueba/corrida-creativa-portada`, que no se sube: terminó
  con la auditoría en PASS y ganó "El libro del viaje". Mostró tres fallas del proceso, ya
  corregidas: la comparación de lo que no se diseña daba DISTINTO sin cambios reales (spinners
  animados; ahora las capturas congelan las animaciones y `comparar` descuenta una carpeta de
  ruido), los arreglos de la auditoría dejaban vieja la página de antes y después, y el
  pre-auditor no podía ver qué archivos cambiaron. Nota de traspaso en la carpeta de notas de la
  sesión.

### 2026-10-31 · flujo (Cozy / Medieval) · Iteración 3: el flujo completo del sitio, como prototipo

- **Decisión:** el flujo completo del sitio en Cozy queda diseñado como un prototipo navegable de 21
  pantallas con sus estados, para los cuatro roles: la portada como encrucijada, el mapa de
  entregadas, ingresar y registrarse; el inicio, el fichero de donaciones por etapa, el viaje de una
  donación con la foto pegada al pueblo, el mapa de seguimiento, el cuaderno de sellos, las
  entidades y las cartas del donante; el pueblo de la entidad, el acta de recepción y "la tinta llega
  y sella"; y la mesa de trabajo del admin, con entidades por aprobar, donaciones por vencer y
  asignaciones. La ficha está en [flujo-completo-cozy-medieval.md](historico/cozy-medieval/flujo-completo.md).
  En la maqueta se recorre en `/prototipo/<pantalla>`, empezando por `/prototipo/publico-portada`.
  Reglas que salen del prototipo: un solo calendario de ejemplo; el nombre del menú siempre a la
  vista (en el admin, como título; en los demás, como rótulo sobre el título propio); el lápiz para
  lo propuesto y la tinta para lo que pasó; los formularios por POST, para que nada de lo escrito
  quede en la dirección.
- **Alternativas:** construir pantallas reales en las dos alternativas (no hay flujos aprobados ni
  APIs, y cada una exige su gemela en Glassmorphism); dejar el flujo solo en un documento (no se
  puede recorrer ni comprobar el piso); hacerlo con variantes A/B (se borran al decidir y no se
  pueden enlazar entre sí).
- **Por qué:** el usuario pidió que la tercera iteración diseñe el flujo completo. Un prototipo que
  se recorre de punta a punta muestra si la mesa del cartógrafo aguanta todo el sitio, y se comprueba
  con la misma verificación visual que las pantallas reales (39 rutas, sin hallazgos). Lo hicieron
  cinco grupos en paralelo; tres críticos (arte, piso y flujo) dijeron que se siente un solo sitio y
  que es más ambicioso y mejor que la iteración 2, y marcaron los arreglos que se hicieron (fechas,
  enlaces a la donación equivocada, callejones, contraseñas en la dirección, títulos del admin).
- **Costo:** son pantallas de diseño, sin datos reales ni gemela: cada una se construye de nuevo,
  en las dos alternativas, cuando se apruebe su flujo. Quedan sin dibujar Donaciones recibidas,
  Donantes, Camiones, Ranking, Notificaciones de la entidad y del admin, y Mi perfil. El prototipo
  pidió piezas compartidas que no existen todavía (el diálogo con campos, `campo` con valor cargado y
  `autocomplete`, los pendientes en latón en la navegación) y mostró un problema que también tiene
  Glassmorphism: la carga de fotos con `capture` le saca la galería al celular. En la maqueta,
  `/notificaciones`, `/perfil` y `/salir` llevan al prototipo de Cozy, también desde el layout de
  Glassmorphism, hasta que existan esas pantallas.
- **Quién decidió:** el agente, con la libertad que dio el usuario, después de tres críticos.
- **Evidencia:** `evidencia/cozy-medieval/flujo-completo/` (las pantallas principales, 360, 1280
  y 1920 px).

### 2026-10-31 · componente:navegacion (Cozy / Medieval) · Los cajones del cartógrafo

- **Decisión:** la navegación de Cozy son los cajones de la mesa: en escritorio, una fila arriba; en
  el celular, abajo, en el borde de la mesa más cercano. El cajón de la sección actual está abierto,
  muestra su interior y lleva el ◆. Con muchas secciones (el admin tiene 7), la fila del celular se
  desplaza a lo ancho y un script chico deja el actual a la vista; sin el script, la fila igual se
  puede recorrer.
- **Alternativas:** carteles de nogal con punta de flecha, con el alfiler ◆ en la sección actual
  (del concepto "el camino es la página").
- **Por qué:** los dos críticos eligieron los cajones. Dicen dónde estás por forma y por lugar, no
  solo por un signo de 12 px; son lo único que suena a "cartógrafo"; y en el alto contraste del
  sistema conservan el borde. Los carteles se leían más livianos, pero sobre la madera casi no se
  distinguían y parecían una ruta de migas.
- **Costo:** son lo más pesado de la cabecera; se aliviaron (sin losa propia, solo la etiqueta del
  cajón actual en papel claro). Reemplaza tres decisiones anteriores de Cozy: la barra con el lomo
  y la pestaña con canto de latón; el alfiler sobre el camino del celular; y, para el admin, el lomo
  angosto y plegable que había elegido el usuario ("opción 1 y que sea plegable"). Los cajones no
  tienen barra lateral que plegar: ocupan una franja arriba y dejan todo el ancho a las tablas. Si
  el usuario quiere volver a poder plegarlos, se decide con el flujo del admin.
- **Quién decidió:** el agente, con dos críticos (arte y piso).
- **Evidencia:** `evidencia/cozy-medieval/componente-navegacion/` (cajones y carteles, 360, 1280 y 1920 px).

### 2026-10-31 · estructura (Cozy / Medieval) · Iteración 2: la mesa en todo Cozy

- **Decisión:** la mesa del cartógrafo pasa del concepto al juego real de plantillas de Cozy: la
  mesa y la cabecera en las dos estructuras (pública y privada); el inicio del donante con sus cinco
  objetos (la nota, el mapa del viaje, la carta, el libro de cuentas con el rango pegado y el
  cuaderno de sellos); el mapa del viaje sale de los datos y sirve para cualquier cantidad de etapas
  y cualquier etapa actual (probado con 3, 5 y 7); el catálogo `/estilos` es el muestrario del
  cartógrafo, con cada componente como una lámina numerada sobre la mesa (botones de cuero, campos
  como renglones de registro, aviso como esquela, banner clavado con chinches, diálogo como carta,
  ficha con la foto pegada, libro de registro). Regla de oficio: un solo recurso material por objeto.
  Los rótulos chicos pasan a Alegreya y Almendra queda en los títulos grandes.
- **Alternativas:** dejar el concepto como variante y aplicar solo el inicio. Para la navegación,
  ver la entrada de los cajones.
- **Por qué:** el usuario pidió una segunda iteración más ambiciosa, con esquemorfismo rústico y
  profesional. Los críticos marcaron lo que faltaba para que no pareciera un disfraz (demasiados
  recursos por objeto, "Estás acá" repetido tres veces, la "E" de Almendra ilegible en chico) y se
  corrigió en una pasada.
- **Costo:** reemplaza tres decisiones de Cozy: "cada pantalla es una hoja del atlas", el inicio "a
  lo ancho" y Almendra en los rótulos. La tierra sin explorar se queda como idea, dibujada como un
  trozo de mapa rectangular. El mapa todavía no nombra el destino: ese dato no llega al recorrido.
- **Quién decidió:** el agente, con la libertad que dio el usuario, después de dos críticos.
- **Evidencia:** `evidencia/cozy-medieval/la-mesa/` (inicio y muestrario, antes y después, 360, 1280
  y 1920 px; el muestrario de 1920 va en dos mitades).

### 2026-10-31 · concepto (Cozy / Medieval) · Iteración 1: la mesa del cartógrafo

- **Decisión:** el inicio del donante, y desde ahí todo Cozy, es **la mesa del cartógrafo**: una
  mesa de nogal vista desde arriba con objetos sueltos, cada uno con su forma y su papel (la nota
  del saludo, el mapa del viaje con el alfiler, la carta con las noticias, el libro de cuentas y el
  cuaderno de sellos), y los cajones de la mesa como navegación. Se combina con lo mejor de los
  otros dos: del camino, "Estás acá" como hito grande de latón y los carteles de madera con punta
  de flecha (que pasan a competir con los cajones en la iteración 2); del libro, los asientos con
  guía de puntos y doble filete, el rango como etiqueta pegada y el recorrido que sirve para
  cualquier cantidad de etapas.
- **Alternativas:** A, **el camino es la página** (el viaje de la donación como columna de toda la
  página, con carteles de madera arriba); B, **el libro abierto** (dos páginas con el lomo al medio,
  cintas señaladoras y un índice de uñeros en el canto). Los tres se construyeron en serio, en una
  copia aparte cada uno, y pasaron el piso en una sola corrida (360, 1280 y 1920 px, sin hallazgos).
- **Por qué:** el crítico de arte la puso primera: es la idea más memorable, cuenta el viaje con un
  mapa de verdad y es la que más da para el resto del sitio (listas como carpetas, detalle como
  papel ampliado, confirmar recepción con el sello sobre el mapa). Además es la que mejor responde
  a la dirección nueva del usuario: esquemorfismo rústico y profesional. El camino se entiende
  rápido, pero su mitad de abajo vuelve a ser tarjetas comunes y su dibujo depende de que haya
  cinco etapas. El libro es el más pulido y el más sólido, pero es el que más se parece a la página
  rechazada.
- **Costo:** el crítico de piso marcó tres bloqueos que la iteración 2 tiene que resolver: el mapa
  está dibujado para cinco etapas con la actual en la cuarta (tiene que salir de los datos); con las
  7 secciones del admin, la barra de abajo del celular no entra; y en 360 × 640 "Estás acá" queda
  apenas debajo de la primera pantalla. El nogal deja de ser solo lo que navega: pasa a ser la mesa.
  Al aplicarse (en la iteración 2) reemplazó tres decisiones anteriores de Cozy: la estructura "cada pantalla es
  una hoja del atlas", la barra con el lomo y la pestaña con canto de latón, y el inicio "a lo
  ancho".
- **Quién decidió:** el agente, con la libertad que dio el usuario ("creatividad absoluta y
  decisiones autónomas"), después de dos críticos independientes (arte y piso).
- **Evidencia:** `evidencia/cozy-medieval/concepto-inicio/` (antes, A, B y C; 360, 1280 y 1920 px).

### 2026-10-31 · referencias (Cozy / Medieval) · Esquemorfismo rústico, profesional y con juego liviano

- **Decisión:** el usuario sumó 18 imágenes de referencia y una dirección: "esquemorfismo rústico.
  Evitar que sea gimmick-y. Buscar profesionalismo. Gamificación ligera, creatividad e identidad
  propia. Explorar, aventura." Pidió tres iteraciones autónomas, cada una más ambiciosa, y que la
  última diseñe el flujo completo del sitio.
- **Alternativas:** ninguna; es un insumo del usuario.
- **Por qué:** el primer intento de Cozy cambió el vestido y no la forma. Las referencias muestran
  objetos con cuerpo (cuero, madera, latón y papel) ordenados con oficio editorial.
- **Costo:** son imágenes de terceros: quedan como referencia y no entran al sitio ni al entregable.
- **Quién decidió:** usuario.
- **Evidencia:** `identidad/referencias-cozy-medieval/`.

### 2026-10-30 · proceso · JavaScript propio, solo como mejora

- **Decisión:** se permite JavaScript propio mientras las páginas se sigan armando en el servidor.
  Lo esencial sigue funcionando sin él, respeta "reducir movimiento" y no hay SPA, frameworks de
  cliente ni build de frontend. Cambia la regla "nada de JavaScript propio salvo en los mapas"
  (`cliente-liviano/AGENTS.md` y D18 del ADR-E6-06, que sigue `proposed`).
- **Alternativas:** seguir sin JavaScript propio.
- **Por qué:** aclaración del usuario: "está permitido usar javascript mientras se siga
  considerando Server Side Rendering".
- **Costo:** más código propio para revisar; cada script tiene que funcionar como agregado.
- **Quién decidió:** usuario.
- **Evidencia:** ninguna.

### 2026-10-30 · voz (Cozy / Medieval) · Los mismos datos y acciones, con voz propia

- **Decisión:** Cozy dice a su modo los títulos y los rótulos ("Tu viaje", "Estás acá", "Cartas del
  camino") y muestra los mismos datos y las mismas acciones que Glassmorphism, con los mismos
  nombres de botones y enlaces. Los títulos propios van en una tabla
  (`cliente-liviano/src/test/resources/voz/cozy-medieval.txt`), sin números. El test que comparaba
  los textos de las gemelas pasa a comparar datos, acciones y nombres de controles, y deja pasar
  solo lo que dice la tabla. Reemplaza la regla del brief aprobado "los mismos textos que
  Glassmorphism" (entrada "Brief aprobado" de Cozy y directiva de estructura, Reglas fijas).
- **Alternativas:** los mismos textos (la regla anterior); voz libre, sin test; la voz marcada en
  cada plantilla con un atributo (ver el
  [ADR propuesto](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md),
  pregunta Textos).
- **Por qué:** el usuario quiso que Cozy tenga voz. La tabla deja la voz a la vista y el test
  asegura que las dos alternativas se comparan sobre lo mismo. Una prueba de mutación lo demostró:
  la voz en un título pasa; cambiar un dato, el destino de un enlace o el nombre de un botón, o
  poner números en la voz, falla.
- **Costo:** un título propio se escribe en la plantilla y en la tabla. El test no sabe qué es un
  título: si alguien declara un dato sin números como rótulo, lo frena la revisión de la tabla.
- **Quién decidió:** usuario (la regla); agente (cómo vigilarla).
- **Evidencia:** `InterfazFitnessTest` (`lasGemelasTienenLosMismosDatosYAcciones` y
  `laVozPropiaSoloCambiaTitulosYRotulos`).

### 2026-10-30 · componente:barra-lomo (Cozy / Medieval) · Pestaña con canto de latón

- **Decisión:** la pestaña de la sección actual sigue unida a la hoja y, del lado del lomo,
  termina en una cinta de latón cortada en V, como el señalador de un libro.
- **Alternativas:** pestaña de pergamino liso, sin canto.
- **Por qué:** se encuentra más rápido dónde estás y suma la idea del libro con una pieza chica,
  del material de la navegación. Era la recomendada.
- **Costo:** ninguno. En el celular no cambia nada: ahí manda el alfiler.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/componente-barra-lomo/` (A y B, 360 y 1280 px).

### 2026-10-30 · componente:estado-vacio (Cozy / Medieval) · Tierra sin explorar con rayado

- **Decisión:** el estado vacío es una isla de mapa sin dibujar, con la costa punteada alrededor
  del mensaje, unas líneas finas de agua por fuera y la rosa junto a la acción.
- **Alternativas:** solo la costa punteada, sin el agua (la recomendada, por más sobria).
- **Por qué:** al usuario le gustó más: tiene más aire de mapa antiguo.
- **Costo:** el agua es muy tenue y suma un adorno a un componente que aparece en muchas
  pantallas.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/componente-estado-vacio/` (A y B, 360 y 1280 px,
  recortadas en la sección "Estado vacío").

### 2026-10-30 · layout:inicio-donante (Cozy / Medieval) · El viaje a lo ancho

- **Decisión:** en el inicio del donante, el recorrido de la última donación cruza la hoja de lado
  a lado, a todo el ancho, y la leyenda de cifras va debajo. En el celular el recorrido baja de
  arriba abajo.
- **Alternativas:** el viaje a lo alto: el recorrido vertical y la leyenda al costado (la
  recomendada).
- **Por qué:** elección del usuario. Se queda la disposición de la estructura base.
- **Costo:** en pantallas de 1920 px las paradas quedan muy separadas, y la leyenda queda un
  renglón más abajo del viaje.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/layout-inicio-donante/` (A y B, 360 y 1280 px).

### 2026-10-30 · estructura (Cozy / Medieval) · La estructura base, aplicada en el catálogo y el armazón

- **Decisión:** la [directiva de estructura](historico/cozy-medieval/estructura.md) queda aplicada en
  `/estilos` y `/estilos/armazon`, sobre el juego de plantillas propio de Cozy. Ese juego ya está
  en el código: la copia está en `templates/alternativas/cozy-medieval/`, `?alternativa=` la elige,
  y cuatro tests vigilan que cada plantilla tenga su gemela, que los textos sean iguales, que Cozy
  use solo sus piezas y que su catálogo esté completo. Lo aplicado:
  - una hoja con regiones y títulos al margen;
  - la veta de nogal en el lomo y el pergamino liso donde se lee;
  - el lomo con la pestaña unida a la hoja y, en el celular, el alfiler sobre el camino;
  - la carta de la posta en lugar de la campana;
  - los estados como signos de mapa;
  - las tablas como libro de registro;
  - las cifras como leyenda;
  - la misión como cuaderno de sellos;
  - el vacío como tierra sin explorar;
  - la hoja que se asienta al cambiar de página, solo sin "reducir movimiento".

  Dos revisores (piso y arte) miraron el resultado. Por eso:
  - el saludo quedó sin el mapa de adorno: su rosa, su camino entre entidades y su cuadrícula
    rompían las reglas de los materiales y le competían al viaje;
  - "Donaciones realizadas" quedó sin un signo que no estaba en la tabla;
  - los avisos quedaron sin rombo, porque el rombo es "ahora";
  - la carta quedó sin sello de latón;
  - la categoría pasó a pergamino con trazo de latón;
  - la cabecera fija ya no tapa el foco;
  - los dibujos se ven con los colores de alto contraste del sistema.
- **Alternativas:** — (aplica una directiva aprobada).
- **Por qué:** pedido del usuario: la directiva de estructura y navegación.
- **Costo:**
  - "1 sin entregar" lleva el signo de "con problema", en lacre. El usuario no sabe si esa
    donación tuvo un problema o todavía no salió: queda para confirmar con el equipo.
  - El mes de cada sello y la condición de cada paso de la misión serían textos nuevos: llegan con
    el flujo de misiones, en las dos alternativas.
  - El texto del catálogo "En el celular, cada fila se muestra como una tarjeta" ya no describe a
    Cozy. Es el mismo en las dos alternativas.
  - Glassmorphism tiene la misma cabecera fija sin reserva arriba y no se tocó.
  - Las decisiones de "Lo que todavía no se puede hacer" de la directiva siguen esperando sus
    pantallas.
- **Quién decidió:** usuario (directiva); equipo (detalles y ajustes de la revisión).
- **Evidencia:** `evidencia/cozy-medieval/estructura-base/` (armazón antes y después, 360 y 1280
  px).

### 2026-10-30 · estructura (Cozy / Medieval) · Cada pantalla es una hoja del atlas

- **Decisión:** [directiva de estructura y navegación](historico/cozy-medieval/estructura.md), con 24
  decisiones en 8 rondas. Entre ellas: una hoja con regiones y títulos al margen, en lugar de
  tarjetas iguales; barra de lomo de cuero con la pestaña unida a la hoja; alfiler sobre el camino
  en el celular; carta de la posta en lugar de la campana; estados como signos de mapa; tablas como
  libro de registro; cifras como leyenda del mapa; la tinta que llega y sella al confirmar una
  recepción; y una audacia por pantalla. Reglas fijas: un signo, un significado; la tinta es una
  donación; el latón sos vos y la navegación; el sello, solo lo confirmado; el lacre, solo "con
  problema".
- **Alternativas:** las de cada ronda, listadas en la directiva. El usuario se apartó de la
  recomendada en 7 temas: el juego de páginas, dónde estoy (desvío en la barra), el movimiento (la
  hoja se asienta), el admin (lomo angosto y plegable), la portada (encrucijada), las cifras
  (leyenda del mapa) y el vacío (tierra sin explorar).
- **Por qué:** pedido del usuario: repensar la estructura, la información y la navegación de Cozy
  sin contenedores genéricos, con ambición y momentos memorables. Las preguntas salieron de tres
  expertos (dirección de arte, arquitectura de la información y plantillas). Cuatro críticos (piso,
  arte, lenguaje y alcance) las revisaron antes de mostrarlas y sacaron las efectistas, las que
  inventaban datos y las que rompían el piso.
- **Costo:** varias decisiones dependen de pantallas o datos que todavía no existen (tabla "Lo que
  todavía no se puede hacer" de la directiva). La implementación y las tres primeras comparaciones
  quedan para la sesión siguiente.
- **Quién decidió:** usuario.
- **Evidencia:** las vistas previas de cada opción están en la directiva. Las capturas llegan con
  las comparaciones.

### 2026-10-30 · proceso (Cozy / Medieval) · Cozy tiene su propio juego de páginas

- **Decisión:** Cozy deja de compartir las plantillas con Glassmorphism y tiene una copia completa
  (layouts, fragmentos y páginas) en `templates/alternativas/cozy-medieval/`. Las de `templates/`
  siguen siendo las de Glassmorphism, sin cambios. Los textos se mantienen iguales en las dos: un
  test compara los dos juegos. Cada plantilla tiene su gemela en la otra alternativa.
- **Alternativas:** piezas propias con respaldo (la recomendada): Cozy guarda solo lo que cambia y
  el resto sale de las compartidas. Desvíos dentro de las compartidas. Solo estilos, como hoy.
- **Por qué:** el usuario prefirió darle a Cozy libertad total de estructura sin depender de las
  piezas de Glassmorphism.
- **Costo:** cada pantalla y cada componente se escriben dos veces (HTML y estilos) y los textos se
  duplican. Cambia una regla del [brief](historico/cozy-medieval/brief.md) ("lo propio de Cozy va
  solo en sus hojas de estilo") y el
  [ADR propuesto](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md),
  que se actualiza con la implementación.
- **Quién decidió:** usuario.
- **Evidencia:** —

### 2026-10-30 · paleta (Cozy / Medieval) · Nogal y bosque

- **Decisión:** pergamino y tinta sepia para leer; barra lateral y cuero de nogal oscuro; acciones
  en verde bosque; latón para sellos, monedas y la placa de categoría. Los estados, en tinte, con
  texto e ícono: ámbar (pendiente), azul de mapa (en camino), verde bosque (entregada) y rojo lacre
  (con problema).
- **Alternativas:** Bosque y latón: barra lateral verde bosque y botones de latón con letra de
  tinta.
- **Por qué:** el verde marca la acción principal y el latón queda para los detalles; con latón en
  los botones, todo lo dorado competía. Era la recomendada.
- **Costo:** ninguno. Contrastes AA comprobados en su `tokens.css`.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/paleta/` (B). En esta tanda, la A de cada ronda es la
  base: sus capturas están en `evidencia/cozy-medieval/direccion-general/a-cartografo-*`.

### 2026-10-30 · forma (Cozy / Medieval) · Pergamino suave

- **Decisión:** esquinas redondeadas (de 6 a 16 px; los estados, en píldora), un solo trazo de
  tinta en paneles y tarjetas, sombra suave y botones con una sombra corta de tinta. Sin dobles
  filetes.
- **Alternativas:** Hoja de atlas (esquinas casi rectas y doble filete en los marcos importantes,
  la recomendada); Marco tallado (madera con remaches de latón).
- **Por qué:** al usuario le gustó más: es la más amable.
- **Costo:** se pierde el doble filete de atlas, que reforzaba la idea del mapa. El mapa queda en
  la cuadrícula, la rosa de los vientos y el camino de tinta. La cabecera y el encabezado de las
  tablas conservan su doble línea.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/forma/` (B y C).

### 2026-10-30 · tipografia (Cozy / Medieval) · Almendra para títulos y rótulos

- **Decisión:** Almendra, de Ana Sanfelippo, en títulos y rótulos; Alegreya, de Juan Pablo del
  Peral (Huerta Tipográfica), en el texto. Las dos son de diseñadores argentinos, con licencia OFL
  y servidas desde el propio sitio.
- **Alternativas:** Cinzel, mayúsculas de inscripción (la recomendada); Grenze Gotisch, gótica
  legible.
- **Por qué:** al usuario le gustó más: es más cálida y medieval.
- **Costo:** Almendra tiene solo dos pesos (normal y negrita). Cinzel y Grenze Gotisch se borraron
  del sitio.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/tipografia/` (B y C).

### 2026-10-30 · direccion-general (Cozy / Medieval) · El cartógrafo

- **Decisión:** "cada donación deja un camino en el mapa". La audacia es el saludo del tablero:
  una hoja de mapa con la cuadrícula de meridianos, la rosa de los vientos y, en pantallas
  anchas, el camino de tinta con las cifras en cartelas. La barra lateral es de nogal, con un
  camino de puntos de latón, y la sección actual es una pestaña de pergamino. Los íconos tienen
  trazo de grabado; los estados vacíos muestran la rosa de los vientos ("por dónde empezar"). La
  rosa y el filete de los títulos son dibujos propios.
- **Alternativas:** La aldea del bosque: barra verde bosque, botones de latón, marcos de madera con
  remaches y el saludo en un cartel.
- **Por qué:** es la más adulta y cuenta el recorrido de cada donación, que es el centro de
  DonaTrack. Era la recomendada.
- **Costo:** la aldea queda como segunda capa del concepto, sin elementos propios por ahora.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/direccion-general/`.

### 2026-10-30 · identidad (Cozy / Medieval) · Brief aprobado

- **Decisión:** [brief de la Alternativa Cozy / Medieval](historico/cozy-medieval/brief.md):
  medieval de juego de rol para adultos, en todo el sitio; idea rectora "el mapa del viaje", con
  la aldea que se ayuda como segunda capa; contenido claro con la barra lateral oscura; pergamino
  y tinta, verde bosque, latón y madera tallada; íconos de grabado en tinta; los mismos textos que
  Glassmorphism.
- **Alternativas:** las opciones de tres rondas de preguntas, con seis fotos de referencia del
  usuario (mapa antiguo, carreta, tablero de una isla, menú de misiones, cómoda tallada y una
  paleta de otoño). Para la intensidad: cálida con toques medievales, o casi sin medieval.
- **Por qué:** el usuario quiere contrastar dos mensajes, uno profesional y serio y otro cálido y
  de aventura. Antes de elegir la intensidad pidió los pros y contras de cada una, que quedaron en
  el brief. Al aprobarlo sumó una condición: nada efectista; cada adorno cuenta algo del sistema.
- **Costo:** el riesgo de parecer un juego o de cansar en las pantallas de trabajo. Se cuida así:
  los datos van sobre pergamino limpio, la letra decorativa solo en títulos y sin texturas de
  fondo.
- **Quién decidió:** usuario.
- **Evidencia:** las fotos de referencia no se guardan en el repositorio (no son nuestras); el
  brief las describe.

### 2026-10-30 · proceso · Dos alternativas de identidad: cada una con su hoja completa

- **Decisión:** la identidad que había hasta hoy pasa a llamarse **Alternativa Glassmorphism** y
  queda igual: sus hojas (`static/css/tokens.css` y `app.css`) no se tocan y es la que se ve por
  defecto. La **Alternativa Cozy / Medieval** tiene su propio `tokens.css` y su propio `app.css`,
  completos, en `static/css/alternativas/cozy-medieval/`. Las dos comparten las pantallas, los
  componentes y los textos. En la maqueta se elige con `?alternativa=cozy-medieval`, también
  junto con `?variante=`. La comprobación visual recorre las dos y las compara.
  [ADR propuesto](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md).
- **Alternativas:** una sola hoja de colores con una sección por alternativa y los estilos
  compartidos (la recomendada por el agente, por el mantenimiento); una rama propia para Cozy.
- **Por qué:** el usuario prefirió darle a Cozy libertad total sin tocar nada de Glassmorphism.
- **Costo:** cada pantalla o componente nuevo se estiliza dos veces, una por alternativa, y se
  revisa dos veces. La regla "un solo `tokens.css`" (RNF-UI-11) pasa a ser "uno por
  alternativa", sujeta a la aprobación del ADR. Fuera de la maqueta todavía no hay forma de
  elegir la alternativa.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/cozy-medieval/alternativas/` (Glassmorphism y Cozy lado a lado).

### 2026-10-30 · proceso · Decisiones en tanda: 7 rondas preparadas de una vez

- **Decisión:** las comparaciones se preparan juntas y el usuario las recorre seguidas, sin
  esperar al agente entre una y otra: una sola corrida de la comprobación visual con todas las
  opciones, una página de resumen con la recomendada de cada ronda y las preguntas en tres
  tandas. Las piezas que comparten varias opciones van en una carpeta `variantes/comun/`, que no
  cuenta como variante y se borra con ellas. El visor abre una comparación desde un enlace
  (`#izq=…&der=…&ancho=…`).
- **Alternativas:** una ronda por vez, con el agente en el medio.
- **Por qué:** pedido del usuario: decidir rápido, sin tiempos muertos.
- **Costo:** si una decisión cambia otra (la paleta y los estados), las opciones se arman sobre
  las dos variantes posibles.
- **Quién decidió:** usuario.
- **Evidencia:** la de cada ronda, en su entrada.

### 2026-10-30 · paleta · Grafito cálido y menta

- **Decisión:** la paleta Grafito cálido y menta: grafito y grises cálidos en lugar del verde
  petróleo del prototipo, con la misma menta, el turquesa y los rojos. Cierra la entrada
  "Pendiente entre la del prototipo y Grafito cálido", más abajo.
- **Alternativas:** la paleta del prototipo tal cual (verde petróleo, menta y turquesa), que era
  la recomendada por ser fiel al prototipo.
- **Por qué:** al usuario le gustó por sobria y cálida; la menta mantiene la marca y lo vivo.
- **Costo:** se aparta del prototipo en el color base: hay que contárselo a la diseñadora. El
  "verde petróleo" de los textos de la bitácora anterior ahora es grafito.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/paleta/` (de la ronda anterior) y las capturas de los estados sobre
  grafito en `evidencia/componente-insignia/`.

### 2026-10-30 · componente:insignia · "En camino" pasa a azul

- **Decisión:** el estado "en camino" (y los avisos informativos) usan un azul (#2457a3 sobre
  #eaf1fc) en lugar del azul petróleo. Pendiente, entregada y con problema quedan igual. Ajusta
  la entrada "Estados en tinte, un color por estado", más abajo.
- **Alternativas:** dejar los cuatro estados como estaban.
- **Por qué:** comprobado por el agente: con la paleta del prototipo, el azul petróleo quedaba a
  12° de tono del verde de la marca y se confundía. El azul se distingue con las dos paletas y
  cumple el contraste (6,2:1).
- **Costo:** ninguno.
- **Quién decidió:** usuario, con la recomendación del agente.
- **Evidencia:** `evidencia/componente-insignia/estados-*` (A y B sobre las dos paletas).

### 2026-10-30 · layout:armazon-privado · Barra lateral fija, como el prototipo

- **Decisión:** las pantallas privadas usan `layout-privado.html`: en escritorio, la barra
  lateral con la ruta punteada, de todo el alto y con la lista fija al desplazarse; arriba, la
  marca, la campana de notificaciones y el menú de la cuenta (Mi perfil y Salir). La muestra está
  en `/estilos/armazon`.
- **Alternativas:** una barra superior con las secciones en una franja; un riel angosto con ícono
  y nombre corto.
- **Por qué:** es el diseño del prototipo, mantiene el motivo de la ruta y deja las secciones
  siempre a la vista.
- **Costo:** la barra lateral le quita 248 px al contenido en escritorio. Las pantallas de
  notificaciones y perfil, y la salida, llegan con sus flujos y con el IdP. El prototipo podía
  plegar la barra con JavaScript: no se trajo.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/layout-armazon-privado/`.

### 2026-10-30 · layout:armazon-privado · Sin buscador arriba, por ahora

- **Decisión:** la barra superior del área privada no lleva el buscador del prototipo.
- **Alternativas:** el buscador a la derecha, como el prototipo.
- **Por qué:** ningún requisito dice qué busca; las listas que lo necesitan (por ejemplo, las
  entidades, RF-06) tienen su propio buscador. Si aparece la necesidad, se suma.
- **Costo:** se aparta del prototipo.
- **Quién decidió:** usuario, con la recomendación del agente.
- **Evidencia:** las capturas de `evidencia/layout-armazon-privado/` muestran el buscador
  descartado.

### 2026-10-30 · layout:navegacion-celular · Barra inferior, como el prototipo

- **Decisión:** en el celular, las secciones van en una barra fija abajo, con ícono y nombre
  corto (por ejemplo, "Donaciones" por "Mis donaciones"). Los lectores de pantalla leen el nombre
  completo. Es el mismo fragmento `navegacion`, acomodado por `layout-privado`.
- **Alternativas:** un botón "Menú" arriba que abre la lista completa.
- **Por qué:** las secciones quedan a un toque y al alcance del pulgar; la entidad usa el
  celular con una mano.
- **Costo:** entran 5 secciones como mucho. El admin, con más secciones, usa la versión de
  escritorio (ver la decisión siguiente).
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/layout-navegacion-celular/`.

### 2026-10-30 · layout:navegacion-admin · 6 secciones por tema, como el prototipo

- **Decisión:** el menú del admin tiene Inicio y 6 secciones: Donantes, Entidades, Donaciones,
  Asignaciones, Camiones y Ranking. Registrar donantes, importarlos por CSV, registrar y aprobar
  entidades y registrar donaciones pasan a ser acciones dentro de cada lista. Actualizado en el
  [mapa del sitio](mapa-del-sitio.md).
- **Alternativas:** las 9 secciones del mapa en una lista; las 9 en 4 grupos con título (Altas,
  Donaciones, Logística y Reportes).
- **Por qué:** 7 opciones se leen de un vistazo y cada lista junta lo que se hace con ese tema.
- **Costo:** registrar e importar quedan a un clic más. Importar donantes pasa al nivel 3: su
  resultado por fila se muestra en esa misma pantalla, para no pasar de 3 niveles (se confirma
  al diseñar el flujo).
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/layout-navegacion-admin/`.

### 2026-10-30 · componente:tarjeta · Sin foto, portada con el ícono de lo donado

- **Decisión:** cuando una entrega todavía no tiene foto, la tarjeta muestra una portada del
  mismo tamaño con el ícono de lo donado (alimentos, ropa, útiles) sobre el fondo oscuro de la
  marca, con las curvas. El estado flota arriba, como en las tarjetas con foto. Los íconos son
  los del prototipo.
- **Alternativas:** una portada con la ruta punteada y el avance; solo texto, como estaba.
- **Por qué:** todas las tarjetas miden lo mismo y cada una se reconoce de un vistazo. La ruta
  repetía lo que dice el estado.
- **Costo:** cada entrega necesita su categoría para elegir el ícono. La foto del prototipo sigue
  sin licencia clara: el ejemplo usa un dibujo propio.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/componente-tarjeta/`.

### 2026-10-30 · modo-oscuro · Solo modo claro

- **Decisión:** el sitio tiene solo modo claro (`data-theme="light"` en los dos layouts).
- **Alternativas:** también un modo oscuro que siga al del celular o la compu.
- **Por qué:** el prototipo no lo tiene y duplicaría la revisión de cada pantalla (contraste y
  capturas) en una entrega con fecha.
- **Costo:** quien usa el modo oscuro ve el sitio claro.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/modo-oscuro/`.

### 2026-10-30 · proceso · Las comparaciones se ven en un visor, no en una galería

- **Decisión:** `target/capturas/index.html` y `comparacion.html` son un visor pensado para
  1920 × 1080: índice de secciones a la izquierda, dos variantes lado a lado elegibles con
  selectores o con las flechas, ancho de 360, 1280 o 1920 (teclas 1, 2 y 3) y desplazamiento a la
  par. Cada variante muestra el primer comentario de su CSS como descripción.
- **Alternativas:** la galería con todas las capturas una debajo de la otra; páginas armadas a
  mano para cada comparación.
- **Por qué:** con varias opciones y capturas de página completa, la galería no se podía recorrer
  ni comparar.
- **Costo:** el visor usa JavaScript (solo en una herramienta de trabajo, no en el sitio).
- **Quién decidió:** usuario.

### 2026-10-30 · paleta · Pendiente entre la del prototipo y Grafito cálido

- **Decisión:** pendiente. Quedan dos candidatas como variantes `paleta-a` (la del prototipo:
  petróleo, menta y turquesa) y `paleta-b` (Grafito cálido y menta). `tokens.css` sigue con la del
  prototipo hasta decidir.
- **Alternativas:** se descartaron "Verdes y sol"; Bosque y lima, Océano y aqua, Ciruela y menta;
  Café y menta, Vino y menta, Grafito y sol; Grafito y menta (gustó, pero con dudas para un
  proyecto solidario); y tres nuevas: Atardecer, Girasol y Mar y fucsia.
- **Por qué:** el grafito gustó por sobrio; falta confirmar que transmita lo comunitario.
- **Costo:** los estados se armonizan recién con la paleta final.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/paleta/`.

### 2026-10-30 · densidad · Como el prototipo

- **Decisión:** los espacios y las alturas del prototipo v2.
- **Alternativas:** una versión compacta, con cerca de un cuarto menos de aire.
- **Por qué:** cómoda en el celular, que es donde la usan la entidad y el donante.
- **Costo:** las tablas del admin pueden necesitar una versión compacta propia más adelante.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/densidad/`.

### 2026-10-30 · tipografia · DM Sans sola

- **Decisión:** DM Sans para todo, como el prototipo.
- **Alternativas:** DM Sans para leer y Bricolage Grotesque para títulos y números.
- **Por qué:** el cambio era sutil y una sola familia es más simple de mantener.
- **Costo:** ninguno.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/tipografia/`.

### 2026-10-30 · componente:insignia · Estados en tinte, un color por estado

- **Decisión:** cada estado de una entrega es una píldora con fondo claro, texto, borde e ícono del
  mismo tono: ámbar (pendiente), azul petróleo (en camino), verde (entregada) y rojo terroso (con
  problema).
- **Alternativas:** la píldora oscura del prototipo, con solo el ícono de color (se distinguía
  mal); píldoras de color sólido.
- **Por qué:** se distinguen de un vistazo, como pidió el usuario, sin competir con los paneles
  oscuros.
- **Costo:** hay que verificar que los colores no choquen con la paleta final.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/componente-insignia/`.

### 2026-10-30 · direccion-general · Fiel al prototipo v2

- **Decisión:** la identidad del prototipo v2, llevada al cliente tal cual: paleta, DM Sans,
  curvas topográficas, vidrio, ruta punteada con nodos que brillan, panel destacado con su red,
  tiles de cifras, recorrido numerado, misión y menú lateral.
- **Alternativas:** primera vuelta: "el prototipo tal cual" en versión reducida y "el camino
  visible" (plano y firme). Las dos se rechazaron: la B, por plana y vacía; la A, por ser una
  versión degradada del prototipo.
- **Por qué:** el prototipo ya es el diseño de la diseñadora y del equipo; la tarea es llevarlo
  bien, no reinterpretarlo.
- **Costo:** tres desvíos por el piso de accesibilidad: controles de 48 px (el prototipo usa 44),
  borde de los campos con contraste 3:1 y verde oscuro para textos verdes sobre fondo claro.
  Quedan para F4 el armazón de las pantallas privadas (barra lateral fija, buscador, campana y
  avatar) y la foto real de las tarjetas (la del prototipo no tiene licencia clara). Usa
  degradados, vidrio y números grandes, que la dirección de arte marca como genéricos: es decisión
  del usuario. Las capturas de la primera vuelta no se guardaron.
- **Quién decidió:** usuario.
- **Evidencia:** `evidencia/direccion-general/`.

### 2026-10-30 · identidad · Brief aprobado

- **Decisión:** [brief de identidad](historico/glassmorphism/brief.md): cercana, confiable y viva con un toque
  alegre; números de impacto como protagonistas; voseo cercano; formas mixtas.
- **Alternativas:** las opciones de las tres rondas de preguntas.
- **Por qué:** respuestas del usuario.
- **Costo:** ninguno.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · El prototipo v2 reemplaza a los bocetos de E1 como punto de partida

- **Decisión:** el punto de partida de la interfaz es el prototipo v2 de la diseñadora (Figma
  "Prototipo v2", [demo navegable](https://sofiadeane.github.io/frontend-donatrack/) y repositorio
  `sofiadeane/frontend-donatrack`). Es punto de partida, no molde: la primera comparación A/B lo
  enfrenta con una versión que lo lleva más lejos.
- **Alternativas:** tomarlo como identidad cerrada y comparar solo lo que dejó abierto; usarlo
  como una referencia más y armar dos direcciones nuevas.
- **Por qué:** los bocetos de E1 quedaron desactualizados. El prototipo ya tiene paleta, letra,
  un motivo propio (la ruta punteada) y pantallas del público y del donante.
- **Costo:** el prototipo no cubre todavía las pantallas de la entidad ni del admin. Los bocetos
  de E1 siguen sirviendo para esas pantallas, como referencia de contenido.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · Flujo piloto: confirmar recepción con fotos

- **Decisión:** el proceso se prueba de punta a punta con el flujo "Confirmar recepción con fotos"
  de la entidad (RF-12, boceto pág. 28).
- **Alternativas:** registrar donación (admin), registro de cuentas e importar CSV.
- **Por qué:** es el que más exige a la interfaz (celular, cámara, carga de archivos, estados,
  avisos). Además es el momento con más valor emocional del sistema, y su decisión técnica (D27)
  ya está tomada.
- **Costo:** la integración real con Logística (subida multipart, HB-10) queda para después. El
  piloto usa datos de ejemplo.
- **Quién decidió:** equipo, al aprobar el plan.

### 2026-10-30 · proceso · Anchos de referencia: 360, 1280 y 1920 px

- **Decisión:** se diseña, se captura y se compara en 360 (celular), 1280 (escritorio) y 1920
  (pantalla grande).
- **Alternativas:** 360, 768 y 1280, como proponía RNF-UI-07 en su forma de verificar.
- **Por qué:** cubre desde el celular hasta las pantallas grandes, que hoy son comunes. La tableta
  sigue cubierta por una comprobación automática de anchos intermedios.
- **Costo:** ninguna captura fija de tableta para revisar a ojo.
- **Quién decidió:** usuario.

### 2026-10-30 · proceso · Los bocetos de E1 son punto de partida, no molde

- **Decisión:** los bocetos de E1 orientan estructura y contenido, y se pueden mejorar. Cada
  cambio importante se anota acá.
- **Alternativas:** implementarlos tal cual (lectura literal de RQ-E6-06).
- **Por qué:** la cátedra no es estricta con eso y los bocetos tienen problemas de uso en celular
  (tablas anchas, carrusel, etiquetas dentro de los campos).
- **Costo:** hay que justificar cada desvío en el entregable.
- **Quién decidió:** usuario.
