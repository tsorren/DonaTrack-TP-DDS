# Bitácora de diseño de la interfaz

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Se actualiza en cada decisión · parte del [proceso de diseño](README.md)**

Cada decisión de diseño queda acá, de la más nueva a la más vieja, con el mismo formato que usa el
entregable para la cátedra. Las comparaciones A/B agregan sus capturas en `evidencia/<tema>/`.

Desde el 2026-10-30 hay dos alternativas de identidad. Las decisiones anteriores a la entrada
"Dos alternativas de identidad" son de la **Alternativa Glassmorphism**. Las de la **Alternativa
Cozy / Medieval** dicen "(Cozy / Medieval)" en el tema y guardan su evidencia en
`evidencia/cozy-medieval/`.

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

## Decisiones

### 2026-10-31 · flujo (Cozy / Medieval) · Iteración 3: el flujo completo del sitio, como prototipo

- **Decisión:** el flujo completo del sitio en Cozy queda diseñado como un prototipo navegable de 21
  pantallas con sus estados, para los cuatro roles: la portada como encrucijada, el mapa de
  entregadas, ingresar y registrarse; el inicio, el fichero de donaciones por etapa, el viaje de una
  donación con la foto pegada al pueblo, el mapa de seguimiento, el cuaderno de sellos, las
  entidades y las cartas del donante; el pueblo de la entidad, el acta de recepción y "la tinta llega
  y sella"; y la mesa de trabajo del admin, con entidades por aprobar, donaciones por vencer y
  asignaciones. La ficha está en [flujo-completo-cozy-medieval.md](flujo-completo-cozy-medieval.md).
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
  Glassmorphism: la carga de fotos con `capture` le saca la galería al celular.
- **Quién decidió:** el agente, con la libertad que dio el usuario, después de tres críticos.
- **Evidencia:** `evidencia/cozy-medieval/flujo-completo/` (las pantallas principales, 360 y
  1280 px).

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
  cajón actual en papel claro). Reemplaza dos decisiones anteriores de Cozy: la barra con el lomo y
  la pestaña con canto de latón, y el alfiler sobre el camino del celular.
- **Quién decidió:** el agente, con dos críticos (arte y piso).
- **Evidencia:** `evidencia/cozy-medieval/componente-navegacion/` (cajones y carteles, 360 y 1280 px).

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
- **Evidencia:** `evidencia/cozy-medieval/la-mesa/` (inicio y muestrario, antes y después, 360 y
  1280 px).

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
  Cuando se aplique, reemplaza tres decisiones anteriores de Cozy: la estructura "cada pantalla es
  una hoja del atlas", la barra con el lomo y la pestaña con canto de latón, y el inicio "a lo
  ancho".
- **Quién decidió:** el agente, con la libertad que dio el usuario ("creatividad absoluta y
  decisiones autónomas"), después de dos críticos independientes (arte y piso).
- **Evidencia:** `evidencia/cozy-medieval/concepto-inicio/` (antes, A, B y C; 360 y 1280 px).

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
  solo lo que dice la tabla.
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

- **Decisión:** la [directiva de estructura](estructura-cozy-medieval.md) queda aplicada en
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

- **Decisión:** [directiva de estructura y navegación](estructura-cozy-medieval.md), con 24
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
  duplican. Cambia una regla del [brief](identidad/brief-cozy-medieval.md) ("lo propio de Cozy va
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

- **Decisión:** [brief de la Alternativa Cozy / Medieval](identidad/brief-cozy-medieval.md):
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

- **Decisión:** [brief de identidad](identidad/brief.md): cercana, confiable y viva con un toque
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
