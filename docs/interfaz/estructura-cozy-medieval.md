# Estructura y navegación de la Alternativa Cozy / Medieval

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Estado:** aprobada por el usuario; juego de plantillas propio y estructura base implementados
> en `/estilos` y `/estilos/armazon` (ver la [bitácora](bitacora.md)) · **Fecha:** 2026-10-30
> **Origen:** 8 rondas de preguntas (24 decisiones) con el usuario, armadas con tres expertos
> (dirección de arte, arquitectura de la información y plantillas) y revisadas por cuatro críticos
> (piso, arte, lenguaje y alcance) antes de mostrarse.
> **Base:** el [brief de Cozy / Medieval](identidad/brief-cozy-medieval.md) y la primera tanda de
> la [bitácora](bitacora.md) (El cartógrafo, Almendra con Alegreya, Pergamino suave y Nogal y
> bosque).

El brief dijo cómo **se ve** Cozy. Esta directiva dice cómo **se arma**: cómo se reparte cada
pantalla, cómo se navega y qué momentos se recuerdan. Rige para Cozy; Glassmorphism no cambia.

> **Segunda versión (2026-10-31): la mesa del cartógrafo.** El usuario vio la primera versión
> aplicada y dijo que se parecía a la página de siempre. Pidió repensarla de cero, con libertad
> completa, y tres iteraciones cada vez más ambiciosas (ver la [bitácora](bitacora.md)). Lo que
> cambia está en la tabla de abajo; lo demás de esta directiva sigue vigente.

| Antes (primera versión) | Ahora (la mesa del cartógrafo) |
|---|---|
| Cada pantalla es una hoja del atlas | Cada pantalla es la **mesa de nogal del cartógrafo**, vista desde arriba, con objetos sueltos ordenados: cada objeto, una parte de la pantalla, con su forma y su papel |
| Barra lateral: el lomo de cuero con la pestaña unida | **Los cajones de la mesa**: en escritorio, arriba; en el celular, abajo, en el borde de la mesa. El cajón de la sección actual está abierto y lleva el ◆ |
| Celular: el alfiler sobre el camino de la barra de abajo | Los mismos cajones, con ícono y nombre corto; con muchas secciones, la fila se desplaza a lo ancho y el actual queda a la vista |
| Una hoja con regiones y títulos al margen | **Un objeto por parte**: la nota del saludo, el mapa del viaje, la carta con las noticias, el libro de cuentas y el cuaderno de sellos |
| El recorrido a lo ancho de la hoja | **El mapa del viaje**: el camino de tinta sale de los datos (cualquier cantidad de etapas), el lápiz marca lo que falta, el alfiler con "Estás acá" está en la actual y la leyenda dice lo mismo en texto |
| El catálogo `/estilos` como hoja | **El muestrario del cartógrafo**: cada componente, una lámina sobre la mesa |
| La madera de nogal es lo que navega | La madera es **la mesa**: el fondo de todo. Lo que navega son los cajones |
| Los mismos textos que Glassmorphism | Los mismos datos y acciones; **voz propia** en títulos y rótulos (brief, Voz) |
| Almendra en títulos y rótulos | Almendra solo en los títulos grandes; los rótulos chicos, en Alegreya (la "E" de Almendra se confunde en tamaño chico) |

Regla nueva de oficio: **un solo recurso material por objeto** (los esquineros, en el mapa; la
chinche, en la nota), para que la mesa se vea profesional y no un disfraz.

## La idea en una frase

**Cada donación deja un camino en el mapa, sobre la mesa del cartógrafo.** La mesa es el lugar de
trabajo; los cajones guardan las secciones; el mapa muestra el viaje en curso, con el alfiler donde
estás; las cartas traen las noticias y el cuaderno guarda los sellos de cada misión.

La primera versión decía: *cada pantalla es una hoja del atlas; la barra es el lomo de cuero; la
sección actual, una pestaña que se funde con la hoja.*

## Reglas fijas

Rigen todas las decisiones de abajo. No se preguntaron: salen del brief y de los críticos.

**Un signo, un significado.**

| Signo | Significa | Dónde se usa |
|---|---|---|
| ● | Etapa cumplida | Recorrido, avance de una misión |
| ◆ | Ahora: la etapa actual o la sección donde estás | Recorrido, barra lateral, alfiler |
| ○ | Lo que falta | Recorrido, pendiente |
| ◉ | Entregada: el pueblo sellado, solo con la recepción confirmada | Estado, recorrido, mapa |
| ┄▸ | En camino | Estado, mapa |
| ┄╳┄ | Con problema: el camino cortado | Estado, mapa |
| ⌂ | Una entidad: un pueblo | Mapa, inicio de la entidad |

**Cada material dice qué es.**

- La **tinta** es una donación: el camino de tinta es siempre el recorrido de una entrega. No une
  secciones, filas ni grupos.
- El **latón** sos vos y la navegación: la barra, la pestaña, el alfiler y lo que te espera.
- El **sello** marca solo lo confirmado (la recepción y los pasos de una misión), y es de latón.
- El **rojo lacre** marca solo "con problema".
- La **rosa de los vientos** marca por dónde empezar.
- La **madera de nogal** es lo que navega; el **pergamino liso**, lo que se lee; la **cuadrícula de
  meridianos**, solo donde hay geografía.

**Una audacia por pantalla.** El resto, disciplinado:

| Pantalla | Su audacia |
|---|---|
| Inicio del donante | El viaje en curso, con el alfiler en la etapa actual |
| Inicio de la entidad | Su pueblo, con los caminos de las entregas que le llegan |
| Inicio del admin | Ninguna: la mesa de trabajo |
| Confirmar recepción | La tinta que llega y sella |
| Detalle de una donación | El recorrido, con la foto pegada al pueblo |
| Misiones e insignias | El cuaderno de sellos |
| Mapa de entregadas y Seguir entregas | El mapa grabado |
| Portada | La encrucijada |

**Los datos van limpios.** Tablas, formularios y listas, en pergamino liso, sin adornos.

**Los mismos datos y acciones que Glassmorphism; voz propia en títulos y rótulos** (brief). Si una
decisión pide un dato o una acción nueva, se agrega en las dos alternativas o no se agrega. Un título
o rótulo propio va en la tabla de voz (`cliente-liviano/src/test/resources/voz/cozy-medieval.txt`).

## Cómo se separa la estructura

**Decisión: un juego completo de páginas para Cozy** (usuario, ronda 1). Cozy tiene su propia copia
de todas las plantillas: layouts, fragmentos y páginas, en `templates/alternativas/cozy-medieval/`.
Las de `templates/` siguen siendo las de Glassmorphism y no cambian.

| Opción | Por qué no |
|---|---|
| Piezas propias con respaldo (la recomendada) | El usuario prefirió libertad total, sin depender de las piezas compartidas |
| Desvíos dentro de las compartidas | Cada cambio tocaría archivos de Glassmorphism |
| Solo estilos, como hoy | No deja hacer la mayoría de las decisiones de abajo |

Consecuencias:

- **Cada pantalla y cada componente nuevo se escribe dos veces** (HTML y estilos), una por
  alternativa. El costo es el doble.
- **Los textos se duplican.** Un test compara los dos juegos (datos, acciones y nombres de los
  controles) y deja pasar solo los títulos y rótulos de la tabla de voz.
- Un test exige que cada plantilla de Glassmorphism tenga su gemela en Cozy, y al revés.
- El catálogo `/estilos` de Cozy muestra los componentes de Cozy.
- En la maqueta, `?alternativa=cozy-medieval` elige el juego de Cozy; `?variante=` sigue
  funcionando encima.
- Cambia una regla del brief aprobado ("lo propio de Cozy va solo en sus hojas de estilo") y el
  [ADR propuesto](../adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md),
  que se actualizan en la misma tarea.

## La hoja

| Tema | Decisión | Alternativas descartadas |
|---|---|---|
| Contenedor | **Una hoja con regiones.** Todo el contenido es una sola hoja; las secciones se separan con un filete de tinta y aire. Solo el saludo y la misión llevan marco propio. | Lo secundario plegado; hojas de distinto tamaño; las tarjetas de hoy |
| Títulos | **Títulos al margen.** En pantallas anchas, el título de cada región va en una columna angosta a la izquierda, como las notas de un códice; en el celular, arriba. | Título sobre el filete; arriba, como hoy |
| Textura | **Cada material, su significado.** Liso donde se lee; cuadrícula de meridianos solo donde hay mapa; veta de nogal en la barra. | Tus caminos en el margen; lisa, como hoy |

## Navegación

| Tema | Decisión | Alternativas descartadas |
|---|---|---|
| Barra lateral | **Lomo con pestaña unida.** La barra es el lomo de cuero del atlas; la sección actual es una pestaña de pergamino que sale de la barra y se funde con la hoja, sin línea entre las dos. | Barra angosta de íconos; el itinerario de hoy |
| Admin | **Lomo angosto y plegable** (usuario: "opción 1 y que sea plegable"). La misma barra, más angosta, con los nombres completos; se puede plegar para dejar todo el ancho a las tablas. | Grupos por tema; igual que los demás |
| Celular | **Alfiler sobre el camino.** La línea de puntos une los íconos de la barra de abajo; la sección actual sube como un alfiler de latón con su nombre. | Menú desplegable; la barra de hoy |
| Dónde estoy | **Desvío en la barra lateral.** Una pantalla de nivel 3 cuelga de su sección como una parada chica; en el celular, arriba del título, la flecha de volver con el nombre de la sección. | Ruta sobre el título; sección arriba del título |
| Avisos | **Carta de la posta.** La campana pasa a ser una carta cerrada, el correo que trae noticias del viaje. | Campana, como hoy |
| Pendientes | **Sí, en latón.** La barra muestra cuántas cosas te esperan, solo donde hay algo que hacer: la entidad, las entregas por confirmar; el admin, las entidades por aprobar. | No, como hoy |
| Movimiento | **La hoja se asienta.** Cada página nueva aparece con un leve asentamiento, como una hoja que se apoya en la mesa. La barra no se mueve. | Nada al navegar; la marca se desliza |

## Lo primero que se ve

| Tema | Decisión | Alternativas descartadas |
|---|---|---|
| Inicio | **Cada rol, su "estás acá".** El donante ve su viaje en curso con el alfiler en la etapa actual; la entidad, Confirmar recepción; el admin, las entidades por aprobar, si hay. | Tu mapa de huellas; diario de viaje |
| Portada | **Encrucijada.** Arriba, el título y "Donar ahora"; abajo, tres caminos que salen de la rosa: soy donante, soy beneficiario y el mapa de entregadas; debajo, las destacadas del mes. | Portada de atlas (la recomendada); índice del atlas |

## El mapa y la aldea

| Tema | Decisión | Alternativas descartadas |
|---|---|---|
| Mapa | **Mapa grabado.** Un estilo propio del mapa: tierra de pergamino, costas y calles en tinta sepia fina; cada entidad, un pueblo; cada entrega, su camino; al lado, la misma lista. | Base clara con signos grabados; el de Glassmorphism |
| Aldea | **Tu pueblo, en la entidad.** En el inicio de la entidad, su pueblo y los caminos de las entregas que le llegan, con la cantidad en texto. | Pueblos en el mapa de entregadas; sin aldea por ahora |
| Fotos | **Pegada a su pueblo.** La foto derecha, con borde de papel, cuelga del pueblo sellado del recorrido; el pie dice cuándo llegó. | Esquineros de cuaderno; borde de papel, como hoy |

## Leer

| Tema | Decisión | Alternativas descartadas |
|---|---|---|
| Cifras | **Leyenda del mapa** (usuario; la recomendada era el libro de cuentas). Las cifras son la leyenda de la hoja: cada una con su signo y su texto ("◉ 2 entregadas", "◆ 1 en camino"). | Libro de cuentas; las de hoy |
| Tablas | **Libro de registro.** Sin franjas ni caja: una regla de tinta entre filas y el doble filete del encabezado; en el celular, cada fila en dos renglones. | La de hoy |

## El viaje de una donación

| Tema | Decisión | Alternativas descartadas |
|---|---|---|
| Estado | **Signos de mapa.** Cada estado es un signo del viaje con su texto (tabla de signos de arriba): cambia la forma, no solo el color. | Píldora con signo; la píldora de hoy |
| Listas | **Por etapa del viaje.** Debajo del filtro por estado y categoría, grupos por etapa con su cantidad y su signo; las entregadas, plegadas. | Pestañas y páginas; capítulos por mes |
| Confirmar recepción | **Acta de recepción.** Arriba, qué llega y su último tramo; en el medio, las fotos como pruebas; al pie, el botón, donde iría la firma. | Las fotos primero; formulario en la hoja |
| Recorrido | No se preguntó: va en el inicio y en el detalle, como hoy; en las listas, solo el signo del estado. | — |

## Momentos memorables

| Tema | Decisión | Alternativas descartadas |
|---|---|---|
| Al confirmar | **La tinta llega y sella.** El último tramo se entinta y el pueblo queda sellado, una sola vez y solo en la respuesta a Confirmar recepción; el aviso va en texto. | Sello en el aviso; solo el aviso |
| Misión | **Cuaderno de sellos.** Cada paso cumplido es un sello de latón con su mes; los que faltan, en relieve y sin tinta, con la condición escrita. | Hitos en el camino; el panel de hoy |
| Vacío | **Tierra sin explorar** (usuario; la recomendada era "la rosa señala"). Un trozo de mapa sin dibujar, con la costa punteada alrededor del mensaje y la rosa junto a la acción. | La rosa señala; el camino que espera; como hoy |

## Cuidados del piso

Lo marcaron los críticos; se cumple al construir, sin preguntar:

- **Latón sobre pergamino** da 1,83:1: todo control o signo de latón sobre la hoja lleva borde o
  trazo oscuro para llegar a 3:1. El foco dentro de la pestaña unida usa el anillo oscuro.
- **La barra de abajo y el alfiler** no tapan el foco: la página reserva su alto
  (`scroll-padding`). El alfiler entra en 360 px con hasta 5 secciones.
- **La hoja se asienta** solo con `prefers-reduced-motion: no-preference`, en 200 ms o menos y 8 px
  o menos; si el navegador no tiene transiciones entre páginas, el cambio es inmediato.
- **La tinta llega y sella** se ve ya sellada con "reducir movimiento".
- **Títulos al margen:** el orden del HTML es el de lectura; la columna del título tiene ancho
  mínimo y corta palabras largas.
- **Tierra sin explorar:** el rayado rodea el mensaje, nunca va debajo del texto.
- **Leyenda y mapa:** todo lo que dice un dibujo está también en texto o en una lista al lado
  (RF-CL-06).
- **Plegables** (grupos de una lista, barra del admin) con `details` y `summary` o un enlace real:
  funcionan sin programación.

## Lo que todavía no se puede hacer

Algunas decisiones dependen de pantallas o datos que no existen. Quedan para cuando llegue su flujo:

| Decisión | Qué falta |
|---|---|
| Pendientes en latón | Un dato por sección (entregas por confirmar, entidades por aprobar) |
| Admin plegable | Guardar la preferencia de barra plegada entre páginas, y el texto del control, en las dos alternativas |
| Admin en el celular | Sus 7 secciones no entran con nombre en la barra de abajo: se decide con el flujo del admin |
| Desvío en la barra | Que la navegación sepa la pantalla de nivel 3 |
| Inicio de la entidad y del admin, aldea | Las pantallas y sus datos |
| Portada (encrucijada) | La landing y las destacadas del mes |
| Mapa grabado | Las pantallas con mapa y un estilo propio de MapLibre |
| Acta y "la tinta llega y sella" | La ficha del flujo piloto (Confirmar recepción) |
| Listas por etapa | Las pantallas de listas de donaciones |
| Fotos pegadas a su pueblo | El detalle de una donación |

## Primeras comparaciones

Sobre las dos páginas que ya existen (`/estilos` y `/estilos/armazon`), con Cozy ya separado y la
estructura base de esta directiva aplicada:

| Tema | A | B |
|---|---|---|
| `layout:inicio-donante` | Viaje a lo alto: el recorrido vertical, protagonista, con la leyenda al costado | Viaje a lo ancho: el recorrido horizontal a todo el ancho de la hoja, con la leyenda debajo |
| `componente:barra-lomo` | Pestaña unida de pergamino liso | Pestaña unida con el canto de latón cortado, como un señalador |
| `componente:estado-vacio` | Tierra sin explorar con rayado alrededor | Tierra sin explorar solo con la costa punteada |

Las elecciones quedan en la [bitácora](bitacora.md), con capturas en
`evidencia/cozy-medieval/<tema>/`.
