# Brief de identidad visual — Raíces

> **Estado:** borrador, en construcción con el usuario por comparaciones A/B · **Desde:** 2026-10-31
> La identidad del cliente liviano: la única desde 2026-11-03
> ([ADR aceptado](../../adr/cliente-liviano/20261103-una-sola-identidad-en-el-cliente-liviano.md)).
> Plantilla: [`plantillas/brief-identidad.md`](../plantillas/brief-identidad.md). Lo que el usuario no definió
> dice "abierto"; lo que propone el agente dice "propuesto".
> **Dónde vive (2026-11-03):** es el sitio. Sus pantallas en `templates/`, sus hojas en
> `static/css/` y su dirección, `/<pantalla>`; la portada, en `/`.
> **Tono común (2026-10-31):** la voz, el tono de cada pantalla, los estados y las cifras los fija
> [tono y comunicación](../tono-y-comunicacion.md), que rige al sitio y prevalece
> sobre este brief donde choquen. Su foco (la persona de responsabilidad social de una empresa) es
> una de las audiencias de ese documento.

## Para qué existe esta identidad

En palabras del usuario: "una alternativa nueva de identidad visual y diseño gráfico y UX/UI. Va a
estar orientada al concepto de raíces y semillas, combinado con la logística, con el foco
principal en las empresas que donan y quieren transparencia".

Mensaje: arraigada y transparente; lo que se ve es el resultado y, debajo, a la vista, todo lo que
lo prueba.

## Personalidad

- **Qué tiene que sentir quien la ve (usuario):** una mezcla de "arraigada, clara y que crece" y
  "cálida, orgánica y comprometida", con la raíz al frente: la estructura y la trazabilidad ordenan
  la pantalla; la calidez entra por la imagen (fotos, dibujo botánico, colores tibios).
- **Intensidad (usuario):** bien lejos de lo que hay: que sorprenda.
- **Lo que no queremos parecer (usuario):** un panfleto "eco" (planeta, "be eco", hojitas
  sueltas), que suena a lavado verde.
- **Riesgos que cuida el agente (propuesto):** dibujo de cuento ante una empresa.

## Para quién

| Quién | Dónde y cómo la usa | Qué le pide a esta identidad |
|---|---|---|
| Empresa donante (foco, usuario) | La persona de responsabilidad social, en escritorio: sigue las donaciones, junta pruebas y arma el informe para su directorio | Detalle comprobable y algo que pueda mostrar |
| Donante particular, entidad, admin y público | Los del [mapa del sitio](../mapa-del-sitio.md) | Abierto |

## Referencias

Las trajo el usuario: [`referencias-raices/`](referencias/README.md). Se toman la textura de
impresión de la 01, la red que es mapa (03), la lámina de campo (05) y el corte de suelo (06).

## Concepto rector (usuario, primer A/B)

**El corte de suelo.** Arriba, poco: el título, las acciones y un brote. Abajo, en la tierra, a
quién llega lo que se dona: las raíces son una red que sale del brote hacia una entidad
beneficiaria de cada tipo (comedor, escuela, merendero, hogar, club y biblioteca), cada una con su
tipo y su nombre. Responde "¿a quién llega lo que dono?". Ver la [bitácora](../bitacora.md).

**Las etapas de una donación no van en la portada (usuario, 2026-11-01):** en el hero, las raíces
son las entidades y no los estados. Las etapas quedan para la pantalla de una donación, con las
formas de estado de la sección Color.

**El camino de la entidad y "Lo que viajó este mes" (usuario, 2026-11-01):** eran los pendientes
de la portada que marcó `revision-ui`. Al centro de la red, un lugar vacío: una línea punteada
hasta un sello hueco, "Tu entidad" y el botón "Sumar mi entidad". Debajo de la tierra, en papel, la
línea del mes: las entregas cuelgan por fecha de una línea de subte que termina hoy. Cada una lleva
un cuadro salvia con el ícono de su categoría; la que va en camino, hueca; las confirmadas, un
botón chico "Ver la foto de la entrega". Ver la [bitácora](../bitacora.md).

**Segunda iteración de la portada (usuario, 2026-11-01):** el usuario comparó la portada de
entonces con dos versiones nuevas (el afiche y la lámina de campo) y eligió una mezcla. Arriba, el
título con una lámina "Cómo funciona": el brote y sus raíces con tres llamadas numeradas. En la
tierra, el rótulo y un brote con su sol, del que sale la red radial. La red termina en etiquetas de
herbario (tipo, nombre y lo que pide cada entidad), con "Tu entidad" al centro. Las cifras van en
una cosecha (frutos que cuelgan de una rama, cada uno con su número) y el mes en un cuaderno de entregas, una fila por entrega
hasta hoy. Reemplaza lo descripto arriba en la composición; las piezas siguen siendo las mismas.
Ver la [bitácora](../bitacora.md).

**Pulido de la portada (usuario, 2026-11-01):** el título manda, bien por encima de los títulos de
sección, y le gana lugar a la lámina, que sigue a su derecha. La lámina es una sola ilustración en
tres franjas pegadas (el brote, la raíz que se abre y la punta que llega), cada una con su número
impreso como sello sobre el borde izquierdo y su paso a la derecha: el número está una sola vez.
En el celular, la red es una espina en espejo; cada entidad cuelga de un sello con el ícono de lo
que pide. En el cuaderno del mes, cada entrega confirmada muestra la miniatura de su foto, que se
abre grande al tocarla. Ver la [bitácora](../bitacora.md).

**Los frutos (usuario, A/B de frutos, 2026-11-02):** en la portada, el brote de la superficie crece
a una planta que da frutos con los datos. Cada fruto impreso es una entrega confirmada del mes y el
hueco, la que va en camino; al lado, una etiqueta repite la cuenta en texto. El usuario pidió
reusar en otras pantallas la idea de los frutos grandes con cifras adentro (la variante D, "raíz y
copa"), con el título "Lo que está confirmado". Ver la [bitácora](../bitacora.md).

## Estructura privada (usuario, 2026-11-01)

En pantalla grande, el menú va en una columna de papel a la izquierda, como el lomo de un herbario,
con la cuenta al pie como su etiqueta; se ve abierta al entrar y se puede plegar a una tira angosta.
La sección actual, impresa en verde oliva con la segunda tinta corrida. En el celular y la tableta,
una barra de navegación inferior en la tierra, con la sección actual en paja. La usan todas las
pantallas privadas del prototipo (`templates/layout-privado.html`). Ver la
[bitácora](../bitacora.md).

## Mapa de las entregas (usuario, 2026-11-01)

En Seguir entregas, el estilo del mapa es la red de la portada: el brote del depósito al centro y
una línea de subte por entrega, en espejo, con codos a 45° redondeados (review de Sofía, 2026-11-02), hasta la estación de su entidad. Lo
recorrido va impreso (paja con borde) y lo que falta, hueco, solo el contorno. El sello de estado
es la estación donde está la entrega: el camión va dentro del sello "en camino". En el sistema, ese
estilo va sobre MapLibre. Debajo del dibujo, una tarjeta por entrega con el mismo número (borde de
tinta y sombra plana salvia); al pasar por una, su línea queda llena y el resto se apaga. Arriba,
un registro de cifras por estado con doble filete. En cada tarjeta, la categoría en un cuadro salvia
y las etapas en una barra de progreso de sellos: es la primera pantalla que usa los sellos de las
etapas. Ver la [bitácora](../bitacora.md).

## Confirmar recepción (usuario, 2026-11-01)

La primera pantalla de la entidad. El acta de una entrega lleva una etiqueta de herbario (doble
filete y sombra plana salvia) con la categoría, cuánto llegó, el pedido que cubre y quién donó, y el
último tramo como raíz vertical: lo cumplido con el sello impreso y la confirmación hueca, unida por
una línea paja que va hueca hacia lo que falta. En pantalla grande, dos filas de dos columnas:
arriba, la etiqueta y el tramo; abajo, las pruebas y lo que recibiste (review de Sofía, 2026-11-02;
antes, la etiqueta y el tramo a la izquierda y el formulario a la derecha). Las fotos se eligen en una zona de carga punteada con la
cámara al centro. Al confirmar, la raíz completa llega impresa del brote del depósito a la
estación de la entidad: se cierra el ciclo. Ver la [bitácora](../bitacora.md).

## Necesidades (usuario, 2026-11-01)

La segunda pantalla de la entidad. Cada pedido vigente es una etiqueta de herbario, como las de la
portada, en dos columnas desde la tableta; al abrirla ocupa el ancho entero y despliega por qué hace
falta, las entregas para ese pedido, quién donó y, si se repite, una barra por período con lo que
llegó y el estado escrito (la del período en curso, punteada). En pantalla grande, el detalle va en
datos y gráfico: las partes a la izquierda y las barras de los períodos subiendo a la derecha; el
resumen plegado dice hasta cuándo, y cuánto de cuánto solo en la barra (review de Sofía,
2026-11-02). "Publicar otra necesidad" va plegado
arriba de la lista, y las cumplidas y terminadas, plegadas al pie. Ver la [bitácora](../bitacora.md).

## Voz

- **Mensaje del hero (usuario, segundo A/B):** "Lo que donás echa raíces en tu comunidad." Lo
  cálido va en el título; la transparencia, en la bajada y en el dibujo del suelo.
- **Bajada (tono común, 2026-10-31):** "Seguí cada donación desde que se registra hasta que la
  entidad confirma la entrega. DonaTrack te muestra a quién se asignó, en qué estado está y la foto
  que sube la entidad al confirmar." Reemplaza la del segundo A/B ("Cada donación llega a un comedor, una escuela o un
  merendero…"), que prometía la llegada. El usuario decidió corregirla al adoptar el brief de tono;
  la redacción es del agente. Desde el 2026-11-01 dice "Seguí cada donación, tuya o de tu empresa,
  …", para que el hero le hable también a la empresa, el foco de esta identidad (aviso de
  `revision-ui`; redacción del agente).
- **Portada, segunda iteración (2026-11-01):** la bajada del hero pasa a "Registrás lo que donás,
  vos o tu empresa. DonaTrack te muestra a qué entidad se asignó, en qué estado está y la foto que
  sube la entidad cuando confirma la entrega." La lámina dice "Cómo funciona" con tres pasos
  ("Registrás lo que donás.", "Se asigna a una entidad.", "La entidad confirma la entrega.").
  Cada etiqueta dice "Pide …" y la de tu entidad, "Pedí lo que necesitás y confirmá lo que
  recibís.". El rótulo de la tierra suma la cifra: "entre las 46 que están en el mapa. Cada una pide
  lo que necesita y confirma lo que recibe.". Las cifras se llaman "Lo que está confirmado" y la
  bajada del mes es "Las últimas entregas de octubre de 2026. La entidad sube la foto cuando
  confirma la entrega." (las cuatro entregas son una muestra, no el mes entero). La que va en
  camino dice "Falta confirmar la entrega: la confirma el comedor con la foto.", la forma del tono
  común para lo pendiente. Redacción del agente; reemplazan los textos anteriores de esta sección
  donde difieren.
- **Estados y foto de la portada (2026-11-01):** sin datos, "Todavía no hay entidades en el mapa.
  La primera puede ser la tuya.", "Todavía no hay entregas confirmadas. Se cuentan cuando cada
  entidad confirma lo que recibe." y "Todavía no hay entregas confirmadas este mes. Cuando una
  entidad confirme una entrega, aparece acá con su foto.". Con error, "No pudimos cargar las
  entidades (las cifras, las entregas del mes). Probá de nuevo en unos minutos." y el botón
  "Volver a cargar". Cargando, "Cargando las entidades…" (las cifras, las entregas del mes). La
  ventana de la foto dice "<entidad> confirmó la entrega el <fecha> y subió esta foto." y
  "Cerrar". Redacción del agente.
- **Rótulo de la tierra (2026-11-01):** "Quiénes reciben las donaciones", con la bajada "Una entidad
  de cada tipo, entre las que están en el mapa." Reemplaza "Todo lo que pasa hasta que llega", que
  afirmaba la llegada y se fue con las etapas.
- **Botón principal (usuario, 2026-10-31):** "Quiero donar".
  Reemplaza "Donar ahora" (urgencia artificial).
- **Camino de la entidad y línea del mes (2026-11-01):** "Tu entidad" y "Sumar mi entidad";
  "Lo que viajó este mes", con la bajada "La entidad sube la foto cuando
  confirma la entrega. Octubre de 2026."; "Ver la foto de la entrega"; y, en la que va en camino,
  "La foto llega cuando el comedor confirme la entrega." Redacción del agente.
- **Etapas y cifras:** con los nombres de los
  [estados](../tono-y-comunicacion.md#los-estados-de-una-donación) (registrada, asignada, preparada,
  en camino, entrega confirmada); lo pendiente se dice pendiente ("Falta confirmar la entrega").
  Las cifras son entregas confirmadas, con período y fuente.
- **Tono (usuario):** término medio entre "botánica medida" y "cálida y comunitaria", sin
  efectismo.
- **Regla (propuesto):** como mucho una imagen de raíz o siembra por pantalla, en el título grande;
  el resto habla de las personas que reciben; nunca en botones, datos ni errores.
- **Palabras que no van (propuesto):** "eco", "huella verde", "florecer", "regá", "cosechá" y
  "sostenible" puesto de adorno.
- **Trato y registro:** vos, cercano (el del tono común).

## Juego

Se insinúa (usuario): el crecimiento es el avance, dibujado con los datos; sin medallas. Para la
empresa, hitos de lo acumulado.

## Color (usuario, tercer A/B; paleta Oliva desde el 2026-11-02)

**Oliva** (antes, Bosque). Lo que se ve es papel con grano; lo que sostiene la donación es tierra de
monte, en verdes oliva: más cálida y menos oscura que Bosque, con frutos mandarina. Detalle, valores
y alternativas en la [bitácora](../bitacora.md).

**Elegida para reemplazarla (usuario, 2026-11-05): Huerta viva en cera.** Es más saturada, con
papel de cera, verde hoja honda, tierra oliva viva, raíces paja, frutos zanahoria y los estados de
Huerta viva. Todavía no está aplicada: se prueba con `?variante=paleta-h` en la portada y en una
pantalla de donante, y después reemplaza a esta tabla.

| Color | Qué es en la pantalla | Qué significa |
|---|---|---|
| Papel crema | El fondo de arriba: título, bajada y botones | Lo que se ve: el resultado, a la vista |
| Verde oliva | El título en cursiva y los botones | La organización que actúa |
| Tres verdes oliva | La tierra en tres horizontes: la red y sus entidades, el rótulo y las cifras | Lo que sostiene la donación, cada vez más hondo |
| Paja | Las raíces, las etapas cumplidas y la rama de la que cuelgan las cifras de la portada; en esa rama, el fruto de las entidades | El recorrido comprobado |
| Salvia | El brote y el sol; en la rama de las cifras, el fruto de lo que va en camino, sin la segunda tinta corrida y con el contorno punteado, como el fruto hueco de la planta | El resultado: lo que creció; en el fruto, lo que todavía está creciendo |
| Mandarina | Los frutos de la planta de la portada, con su segunda tinta (la del que va en camino, punteado), y el fruto más grande de la rama de las cifras | Lo confirmado (en la planta, este mes; en la rama, desde marzo) |

**Estados de una entrega** (siempre con texto y una forma, nunca solo con color): pendiente, gris
tierra con círculo punteado; en camino, ámbar con medio círculo; entregada, verde con tilde; con
problema, rojo óxido con "!".

**Cómo se dibujan, en la tierra (usuario, ronda de formas, 2026-11-01): el sello.** Lo cumplido
está impreso: un disco paja lleno con la segunda tinta (salvia) corrida detrás y el tilde calado.
En camino, un círculo con su mitad izquierda en ámbar. Pendiente, un círculo punteado. Con
problema, un disco óxido con "!" calado. El numeral de lo que todavía no pasó va hueco (solo
contorno): lo que falta todavía no se imprimió. Se descartaron la marca de lámina (trazo fino, lo
que falta apagado) y la etiqueta recta (cuadrados). Las etapas salieron de la portada; la primera pantalla que usa los sellos es Seguir
entregas (2026-11-01).

**Prioridad (usuario):** el diseño gráfico antes que la accesibilidad; "en el futuro habilitaremos
un modo accesible". Hoy la paleta igual pasa AA, y el
[piso de la dirección de arte](../direccion-de-arte.md#el-piso-no-se-negocia) sigue vigente.

## Estilo visual, forma y letra

**Estilo (usuario, cuarto A/B): impreso editorial**, un afiche risográfico (referencias 01 y 02).
Detalle en la [bitácora](../bitacora.md).

| Pieza | Cómo es | Qué significa |
|---|---|---|
| Textura | Risografía suave, solo en el fondo: un polvo fino de tinta sobre el papel de todas las páginas y, en los planos de tierra, el papel que se asoma en puntitos; tarjetas, botones, el sol y los dibujos, lisos (usuario, A/B de textura, 2026-11-02) | Oficio: algo hecho a mano, no una plantilla |
| Sol | Medio disco salvia claro, plano, que sale de la tierra detrás del brote | El resultado: lo que creció |
| Brote | Dos cotiledones planos y simétricos: uno verde oliva con la segunda tinta salvia corrida detrás, el otro salvia | Lo que se ve: orgánico y cálido |
| Planta y frutos | En la portada, el brote crecido: tallo y ramas en verde oliva y un fruto mandarina por entrega confirmada del mes, un disco con su segunda tinta corrida y una hoja; lo que va en camino, un fruto hueco punteado en la segunda tinta (A/B de frutos y de paleta, 2026-11-02) | El resultado, contado con los datos |
| Tierra | Tres horizontes en verdes oliva, con la textura de la tierra. El semitono al pie de cada horizonte (ronda de tierra, 2026-11-01) quedó afuera: ya no estaba dibujado y en el A/B de textura (2026-11-02) el usuario eligió la risografía | Lo hondo, impreso |
| Raíces | Una **red radial**: líneas de subte paja con borde verde oscuro, codos a 45° redondeados (review de Sofía, 2026-11-02) y estaciones, del brote a una entidad de cada tipo, en espejo (usuario, ronda de ilustración, 2026-11-01). Donde no entra, una **espina en espejo**: un tronco al centro y las entidades de a dos, cada una con su rama y su cruce (usuario, A/B del celular, 2026-11-01) | Lo que sostiene: la logística que llega a la comunidad |
| Entidades | Un sello paja con la segunda tinta corrida y el ícono de lo que pide (el del cuaderno del mes); debajo, la etiqueta de herbario con tipo, nombre y lo que pide (usuario, A/B de la red, 2026-11-01) | Quién recibe |
| Títulos | Grandes, con la parte en cursiva corrida por una segunda tinta salvia | Calidez y comunidad |
| Numerales y cifras | Gigantes, anchos y color paja (derechos desde el A/B de letra). En "Lo que está confirmado" de la portada, en tinta dentro de frutos que cuelgan de una rama paja, cada uno de su tamaño y su color, con una etiqueta de papel (A/B de cifras, 2026-11-02) | El recorrido, como capítulos de un afiche; en la rama, la cosecha |
| Botones y bordes | Esquina suave: el papel (láminas, etiquetas y tarjetas) y los botones apenas redondeados, con una sombra plana salvia corrida (usuario, A/B de bordes, 2026-11-02; antes, rectos); al apretar, el botón se hunde sobre su sombra (usuario, ronda de formas, 2026-11-01) | Tinta plana, impreso; un papel grueso |

**Forma:** sale del estilo (esquina suave, sombra plana corrida). La línea entre horizontes no entró; la
grava tampoco (la reemplazó el semitono, que después salió con el A/B de textura). En la ronda de ilustración se descartaron el grabado de
lámina, el plano risográfico con raíces orgánicas, la orgánica simétrica, el abanico, la
bifurcada y la fila de lámina (bitácora).

**Movimiento (usuario, ronda de movimiento, 2026-11-01):** al entrar, una vez, la página se
imprime (el sol aparece y la segunda tinta entra corrida y calza, 1 s) y la red se traza desde el
brote hasta cada entidad (1 s más). Al pasar el mouse o el foco por una entidad, su línea queda
llena y el resto se apaga. Sin movimiento con "reducir movimiento"; sin JavaScript (CSS). Se
descartó el pulso de entrega (puntos que recorren las líneas): movimiento sin fin, que pide un
botón de pausa. **Viento (usuario, review de Sofía, 2026-11-02):** después del trazado, las hojas
del brote y de la planta y los frutos se mecen unos 3 s y quedan quietos unos 20, en ciclo (el
usuario eligió el ciclo contra una sola ráfaga, la recomendada). Las entregas de "Lo que viajó este
mes" aparecen de a una al bajar (línea de tiempo de scroll; sin ella, quietas). Un botón "Pausar el
movimiento", bajo la lámina, frena el viento (WCAG 2.2.2).

**Letra (usuario, quinto A/B): grotesca ancha de afiche.** Una sola familia, Archivo
(Omnibus-Type, Buenos Aires); el carácter sale del ancho y del peso. Detalle en la
[bitácora](../bitacora.md).

| Pieza | Cómo es | Qué significa |
|---|---|---|
| Título, numerales, cifras y marca | Anchos y pesados; la parte cálida del título, en cursiva | Las letras gigantes de un afiche (referencia 02): firme, se recuerda |
| Texto, botones y datos | Ancho normal (los botones, apenas anchos) | Clara y neutra: deja hablar al título |

## Libertades (usuario)

La composición de cada pantalla, la navegación, la voz en títulos y rótulos y el movimiento como
mejora sobre lo que ya funciona. Fijos: los datos y las acciones, el piso de la
[dirección de arte](../direccion-de-arte.md#el-piso-no-se-negocia), y el stack.

## Restricciones

- El piso de la dirección de arte.
- Letras con licencia OFL, servidas desde el propio sitio.
- Lo que dice un dibujo sale de los datos y está también en texto ("Oficio").
