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
  exploración ([brief](identidad/brief-raices.md)), es el corte de suelo. Arriba, poco: el título,
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
