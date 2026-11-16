# Segunda ronda de ajustes del 2026-11-09

Devolución de la usuaria sobre las pruebas de la ronda de la mañana
(`plan-ronda-2026-11-09.md`). Elige variantes, pide arreglos sobre las elegidas y nuevas pruebas
donde la idea no cerró. Mismo mecanismo de variantes (`?variante=<tema>-x`, la **a** es la
recomendada). Al elegir una variante, pasa a ser la pantalla y se borran sus hermanas.

## Reglas generales nuevas

1. **Filtros al lado del buscador.** En pantalla ancha, los filtros van en la misma fila que la
   búsqueda y su botón. En el celular bajan debajo. Vale en todo el sitio.
2. **Sin el borde verde de selección.** Ninguna tarjeta, ficha ni fila seleccionada lleva el
   recuadro o anillo verde ("se ve super genérico"). La selección se marca con fondo, peso del
   texto o un indicador propio, nunca con ese borde.
3. **Íconos centrados en su contenedor.** Los pictogramas de categoría (olla, remera…) van
   centrados en su caja redondeada, en los dos ejes.
4. **Árboles nuevos.** El redondo, el triangular y el ovalado son ahora el manzano
   (`arbol-redondo.svg`), el pino con piñas (`arbol-triangular.svg`) y el ciruelo
   (`arbol-ovalado.svg`). Los planos (`arbol-1` a `arbol-4`) se borraron.

## Decisiones y pedidos por pantalla

| Pantalla | Decisión | Pedido |
|---|---|---|
| Portada, Entidades | **b** + la barra de progreso de **a** | Íconos centrados. Nueva prueba A/B: cómo se integra la barra en la vista plegada y cómo se ve al desplegar. |
| Distribuidora, Inicio | **a** | Ilustraciones de diseño a escala (la misma escala de las láminas). |
| Distribuidora, Últimas noticias | **a**, rediseñada | Primera línea del título alineada con la bolita, más aire en el título, jerarquía de estado y fecha, "Ver todas" al final. |
| Distribuidora, Entidades | **c** | Barra de progreso coherente (sin rayada sin %), filtros al lado del buscador, sin borde verde, las entidades a las que ya donaste atenuadas y como filtro. |
| Persona, Misiones | nuevas **a** y **b** | a: figuritas literal, una huerta armada en 4 piezas. b: invernadero que se llena al donar. |
| Persona, Dashboard | **b**, rehecha | Vista anual mes a mes, no día a día. |
| Beneficiario, Necesidades | **b** | La categoría también se pliega. |
| Admin, Entidades | **c** | Estado con bolita redonda (punto al centro), sin borde verde, ficha debajo del ítem en el celular, selector Activas / Pendientes / Inactivas tras el título. |
| Admin, Asignaciones | nuevas **a/b/c** | Rehechas según el enunciado del matchmaking. |

## Reparto

| Parte | Quién |
|---|---|
| Árboles nuevos | sesión principal (hecho) |
| Portada, Entidades | agente portada |
| Distribuidora + filtros al lado del buscador (componente compartido) | agente distribuidora |
| Persona: Misiones y Dashboard | agente persona |
| Beneficiario + Admin Entidades + barrido del borde verde | agente entidad-admin |
| Admin, Asignaciones | agente asignaciones |

## Resultado

Cada agente agrega acá una subsección con lo que hizo y las URLs de sus pruebas.

### Admin, Asignaciones

Se borraron `propuestas-a|b` (plantillas, datos comunes y hojas). Nueva prueba A/B/C según el
enunciado del matchmaking (la **a** es la recomendada). Las tres muestran la cola de donaciones En
depósito; por cada una, el ranking de cada algoritmo (hasta 10 entidades, con su puesto y el porqué:
lo que pide la entidad en Compatibilidad semántica, cuántas donaciones recibió de agosto a octubre
en Prioridad a sub-atendidas); primero las candidatas en común (las que están en todos los
rankings) y, si no hay ninguna, las ejecuciones completas para elegir de cualquiera. Asignar pide
confirmación. Las ejecuciones se recorren como lista (`ejecuciones` en los datos): un algoritmo
nuevo suma una columna o un bloque sin tocar la plantilla. Búsqueda con los filtros al lado (en el
celular, debajo), con el componente `filtros`: categoría, tiempo en el depósito y coincidencias.
Datos de ejemplo: Arroz y Cuadernos con 3 en común, Mesas plegables con 1, Camperas de abrigo sin
coincidencias.

- [a](http://localhost:8091/admin-asignaciones?variante=asignaciones-a). Eje: bandeja y detalle. La
  cola a la izquierda (cada donación con cuántas en común) y la elegida a la derecha
  (`&donacion=<clave>`): candidatas en común con su puesto en cada algoritmo y "Asignar" (diálogo
  popover), y "Ver cada ejecución" plegada debajo; sin coincidencias
  ([camperas](http://localhost:8091/admin-asignaciones?variante=asignaciones-a&donacion=camperas)),
  un aviso y las ejecuciones abiertas, cada puesto con "Asignar".
- [b](http://localhost:8091/admin-asignaciones?variante=asignaciones-b). Eje: una tarjeta por
  donación, todas a la vista. Las candidatas en común en línea y la tabla de posiciones (una fila
  por puesto, una columna por algoritmo; las en común con fondo; en el celular, una tarjeta por
  puesto). Sin coincidencias, la tabla se abre con "Asignar" en cada entidad.
- [c](http://localhost:8091/admin-asignaciones?variante=asignaciones-c). Eje: paso a paso, para el
  celular. 1) Elegí la donación; 2) revisá las candidatas (`&donacion=`; sin coincidencias, cada
  ejecución una debajo de otra:
  [camperas](http://localhost:8091/admin-asignaciones?variante=asignaciones-c&donacion=camperas));
  3) confirmá el destino (`&entidad=`), una página con "Asignar destino" y "Elegir otra".
- Estados: `&accion=asignada&donacion=<clave>&entidad=<k>` (el aviso y la donación sale de la cola;
  el POST real va a `admin-asignaciones-aprobada`, que no conserva la variante), `?escenario=vacio`
  y `?escenario=error`.

Archivos: `templates/variantes/asignaciones-a|b|c.html`, `templates/variantes/comun/asignaciones-datos.html`,
`static/css/variantes/asignaciones-a|b|c.css` y `static/css/variantes/comun/asignaciones.css`. Sin
clases nuevas (lo propio va por `data-`), sin cambios en Java. La selección se marca con fondo y
peso, sin borde verde. Verificado con curl (todas las combinaciones, 200) y en el navegador a 1280 y
375 px, sin desbordes. `InterfazFitnessTest` y `SitioControllerTest` fallan por clases y un texto de
otras partes en curso (`maestro`, `maestro-ficha`, `segmentos`, `plegable`, Entidades del admin); nada
de Asignaciones.

### Distribuidora y filtros (agente distribuidora)

- **Filtros al lado del buscador** (regla general 1, componente compartido). La barra es
  `form.filtros-barra` en `componentes/filtros.css`, registrada en el catálogo (fila de `.filtros`)
  y en `/estilos`: `.campo.filtro-buscar` con su `.filtro-buscar-fila` (el campo y «Buscar») y, como
  hermano, el `div.filtros` con las pastillas. En el celular los filtros bajan debajo; desde
  53,75 em van en la misma fila, a la derecha, centrados con el campo, y el menú de la última
  pastilla se abre hacia la izquierda. `.filtro-buscar` y `.filtro-buscar-fila` pasaron del
  listado de heredadas al componente (se sacaron los duplicados de `admin-secciones.css` y
  `publico-entidades.css`). La usan [Entidades públicas](http://localhost:8091/publico-entidades),
  [Donantes](http://localhost:8091/admin-donantes), [Camiones](http://localhost:8091/admin-camiones)
  y Entidades del donante. `.filtro--buscar` queda como la forma vieja (la usan todavía las pruebas
  de Admin Entidades y Asignaciones): para pasarla a la barra, cambiar la clase del formulario por
  `filtros-barra` y sacar `pila`.
- **[Inicio](http://localhost:8091/donante-inicio?cuenta=empresa)**: la **a** es la pantalla; se
  borraron `inicio-empresa-a|b|c`. Las ilustraciones van a escala, como las láminas
  (`data-dibujo` y `--medida-alto`; se sumaron los tres árboles a la lista de `huerta.css`): en
  las cifras, una escala de 0,56 px por unidad sobre la línea de suelo (la maceta igual en el
  naranjo y el brote; el pino, el más alto); en el tablero, cada dibujo apoya abajo de su marco a
  0,26 px por unidad. Se conservó el hito "Primera entrega confirmada" debajo de las cifras.
- **Últimas noticias**: la **a**, rehecha; se borraron `noticias-a|b|c`. La bolita queda a la altura
  de la primera línea del título (interlineado fijo), el título con más aire y hasta tres renglones,
  y debajo, discretos en un renglón, "Sin leer" (texto verde, solo si lo está; la leída lo dice
  para el lector y su bolita es hueca), cuándo y de qué donación. Sin las pastillas apiladas. El
  hilo va de bolita en bolita y termina en la última. «Ver todas (4 sin leer)» pasó al final de la
  tile, también en el orden del código. Toda la noticia es el toque del enlace.
- **[Entidades](http://localhost:8091/donante-entidades?cuenta=empresa)**: la **c** es la
  pantalla; se borraron `entidades-donante-a|b|c`. Todo pedido tiene meta, así que todos llevan la
  barra (`.progreso`, nativa, con su nombre accesible) y el porcentaje; el que todavía no recibió
  nada, en 0 % ("Faltan los 20 litros"), sin la barra rayada. Barra en la misma fila que la
  búsqueda: Categoría y «Ya doné»
  ([con filtros](http://localhost:8091/donante-entidades?ya=si&categoria=alimentos)). Sin el borde
  verde: los pedidos de las entidades a las que ya donaste van atenuados (papel de la página, sin
  sombra, título en la tinta suave y la barra apagada) y con "Ya le donaste" en palabras.
- "Ver seguimiento" (pie de Mis donaciones) no se tocó.
- Verificado a 1280 y 375 px sin desbordes (capturas sin cabeza). `InterfazFitnessTest` y
  `SitioControllerTest` pasan en lo mío; quedan en rojo `almanaque` (fuera del catálogo, de
  Persona) y `lasEntidadesDelAdminSeApruebanCorrigenORechazan` (Admin Entidades).

### Portada, Entidades

**Elegida: la b con la barra de la a** (se ve sin `?variante=`,
[base](http://localhost:8091/publico-entidades)).

- La lista compacta agrupada por tipo de entidad pasó a `publico-entidades.html` (el herbario se fue).
  Un renglón por entidad, que se despliega en el lugar sin JavaScript.
  - Plegado: el nombre, el tipo y el barrio, las categorías que pide y «Ver los N pedidos».
  - Desplegado: cada pedido con su ícono, la barra redondeada de lo que ya llegó (`.progreso`, la de
    la a) y, en palabras, lo que falta, hasta cuándo o si se repite, y por qué. Sin cantidad, sin
    barra: solo el texto.
- Dos componentes nuevos, en el catálogo y en `/estilos`: `.plegable` (`componentes/plegable.css`;
  abierto no lleva borde ni anillo verde, la flecha dice si está abierto) y `.chip`
  (`componentes/chip.css`, la categoría con su ícono; `data-tono="emergencia"`).
- La búsqueda y los filtros usan la barra compartida (`.filtros-barra`, del agente distribuidora): en
  una fila en ancho, uno debajo del otro en el celular. La hoja de la pantalla solo pone el espacio
  debajo.
- Al cargar (`?escenario=lento`), los esqueletos son renglones.
- Clases heredadas que se dejaron de usar, sacadas de `clases-heredadas.txt`: `herbario`,
  `herbario-lista`, `etiqueta-entidad`, `etiqueta-sello`, `etiqueta-tipo`, `etiqueta-barrio`,
  `etiqueta-pide` y `pedidos-menos`.

**Íconos centrados (regla general 3).** El problema no era la caja sino el `<use>`: con el aire
alrededor (`viewBox="-14 -14 128 128"`), el símbolo ocupaba el 100 % del viewBox desde el 0 y
quedaba corrido unos 3 px abajo y a la derecha. Ahora el `use` lleva `width="100" height="100"` en
Entidades, el Mapa (`publico-mapa.html`) y Entregas (`publico-donaciones.html`); la regla quedó
anotada en `sellos.css`. Además, el lápiz de útiles escolares (`iconos.html`) se corrió 6 unidades a
la izquierda: su dibujo estaba descentrado en el símbolo. Medido: el centro del dibujo coincide con
el de la caja (0 px de diferencia).

**Prueba A/B, la barra en el renglón plegado y desplegado** (la **a** es la recomendada).

- [a](http://localhost:8091/publico-entidades?variante=entidades-a). Plegado: una barra fina debajo
  del nombre con un pedido en palabras (el de emergencia, si hay; si no, el primero con cantidad),
  por ejemplo «Ropa de abrigo: faltan 25 de 30 unidades». Desplegado: esa línea se va y cada pedido
  muestra su barra entera con las cifras, como la base.
- [b](http://localhost:8091/publico-entidades?variante=entidades-b). Plegado: la barra como pie de
  la tarjeta, partida en un tramo por pedido con cantidad, y debajo «Ya llegó una parte en 4 de sus
  4 pedidos con cantidad». Desplegado: el pie se va y los pedidos quedan en filas, como una tabla
  (qué, hasta cuándo y por qué; la barra y lo que falta, a la derecha en ancho).
- Ninguna de las dos suma porcentajes entre pedidos: las unidades son distintas (kg, litros,
  unidades) y el promedio sería una cifra sin respaldo (tono, «Cifras y datos»).

Archivos: `templates/variantes/entidades-a|b.html` y `static/css/variantes/entidades-a|b.css`
(las viejas a, b y c se borraron), copiados en `target/classes`.

**Árboles nuevos en las cifras.**

- Impacto (`componentes/huerta.html` y `huerta.css`): los tres árboles a la misma escala (el pino, el
  más alto, mide 10 rem; manzano 133 × 150, pino 77 × 160 y ciruelo 58 × 133 px, por su viewBox) y
  apoyados en la línea de suelo (la primera fila de la cifra mide 10 rem y el árbol va abajo).
  Compatible con las cifras del inicio del donante, que ponen su propio alto.
- La cifra con árbol de las listas públicas (`.fruto::before` en `publico-listas.css`): el manzano
  con su proporción (`aspect-ratio`), sin deformarse.

Verificado a 1280 y 375 px, sin desborde horizontal; las URLs dan 200 y `?variante=entidades-c`, 404.
`InterfazFitnessTest`: lo de esta parte pasa. Al cerrar quedaba una falla ajena (`almanaque`, de
otro agente, sin catálogo ni muestra en `/estilos`).

### Beneficiario, Admin Entidades y selección

Sin pruebas nuevas: las elegidas pasaron a ser la pantalla y se borraron sus hermanas.

- **Beneficiario, Necesidades** (`necesidades-b` pasó a base; se borraron
  `templates/variantes/necesidades-b.html` y `static/css/variantes/necesidades-a|b|c.css`). La lista
  de vigentes va en un cantero por categoría (`details[data-cantero]`, abierto de partida) con el
  pictograma grande, el nombre y su resumen: cuántas necesidades, cuántas cubiertas («ninguna
  cubierta todavía», «2 de 5 cubiertas», «todas cubiertas») y cuántas entregas esperan tu
  confirmación, más «Plegar» / «Mostrar» con su flecha. Plegada, la categoría deja a la vista solo ese
  resumen; cada necesidad se sigue plegando adentro. Las tarjetas, compactas (de a tres en pantalla
  grande). Estilos en `entidad-necesidades.css`; sin clases nuevas. Se sacaron `data-avance` y
  `data-etapa` (solo los usaban las variantes).
- **Admin, Entidades** (`admin-entidades-c` pasó a base; se borraron `admin-entidades-a|b|c` en
  plantillas y hojas y `variantes/comun/admin-entidades-datos.html`, cuyos datos y fragmentos
  `acciones(e)` y `documentos(e)` viven ahora en `admin-entidades.html`).
  - Justo después del título, un selector **Activas · 46 / Pendientes · 3 / Inactivas · 3**: bloque
    nuevo `.segmentos` (enlaces reales con `aria-current="page"`, `?ver=activas|pendientes|inactivas`;
    de partida, activas). Se sumaron 3 inactivas de ejemplo con su fecha y motivo de baja.
  - Estado como **bolita**: círculo con anillo y punto al centro, del color del estado, y la palabra al
    lado (variante `span.estado[data-forma="punto"]` de `componentes/sello.css`; `data-estado`
    `recibida` para Activa, `pendiente` o `camino` (le falta un dato) y el nuevo `inactiva`, en tinta
    suave).
  - Lista y ficha: bloque nuevo `.maestro` / `.maestro-ficha`. La ficha va dentro del renglón elegido:
    **en el celular queda justo debajo del ítem**; desde 53,75 em, a la derecha (el `li` elegido se
    disuelve con `display: contents` y la ficha ocupa la segunda columna de arriba abajo). El elegido
    ya no lleva el filete verde: papel claro, sombra suave y un indicador redondeado de tinta a la
    izquierda.
  - Las activas se buscan con los filtros al lado (`form.filtros-barra`: Pedidos y Tipo).
  - Después de aprobar, la recién aprobada aparece elegida en Activas (47); después de pedir
    corrección o rechazar, se ve Pendientes.
  - Los dos bloques nuevos están en el catálogo (`componentes.md`) y en `/estilos`. Se sacó
    `filtro--buscar` de `clases-heredadas.txt` (ya no la usa ninguna plantilla).
- **Regla general, sin el borde verde de selección.** Reglas cambiadas:
  - `componentes/registro.css`, `.registro-tipo[aria-current]` (el tipo de cuenta elegido en el
    registro): sin el anillo `inset 0 0 0 2px` verde; queda el fondo verde claro y el tilde.
  - `componentes/mapa.css`, `.estacion-ficha:target` (la ficha a la que se llega con
    `#entidad-<clave>` en el mapa público y Seguir entregas): sin el `outline` verde ni la sombra
    paja; fondo verde claro.
  - `variantes/admin-entidades-c.css`, `a[aria-current]` (filete verde a la izquierda): borrada con la
    variante; el bloque `.maestro` usa la marca nueva.
  - Sin cambios, a propósito: los anillos de foco (`:focus-visible`, `:has(:focus-visible)`,
    `.subida:focus-within`); los filtros y opciones elegidas que se llenan de verde (`.filtros-*`,
    `.filtro-opcion:has(:checked)`, `.eleccion a[aria-current]`), que son botones llenos y no un borde;
    la pestaña elegida (`.pestana[aria-selected]`, subrayado). Las variantes de otros agentes
    (`entidades-donante-b|c`, `noticias-a`, `dashboard-persona-b`) no se tocaron: marcan "ya donaste",
    "sin leer" o un día, no una selección, y las maneja cada agente.

URLs: [Necesidades](http://localhost:8091/entidad-necesidades),
[Entidades, activas](http://localhost:8091/admin-entidades),
[pendientes](http://localhost:8091/admin-entidades?ver=pendientes&entidad=merendero-este),
[inactivas](http://localhost:8091/admin-entidades?ver=inactivas),
[aprobada](http://localhost:8091/admin-entidades-aprobada),
[corrección](http://localhost:8091/admin-entidades-correccion),
[sin pendientes](http://localhost:8091/admin-entidades?escenario=vacio&ver=pendientes),
[catálogo](http://localhost:8091/estilos). Verificado con curl y en el navegador a 1280 y 375 px
(sin desbordes); `InterfazFitnessTest` y `SitioControllerTest` en verde (59 pruebas).

### Donante persona

Martina Ruiz, se entra con `?cuenta=persona` (queda en la sesión).

- **Inicio (sin variante): la idea de la b como vista anual.**
  [/donante-inicio?cuenta=persona](http://localhost:8091/donante-inicio?cuenta=persona). Su 2026 en
  doce meses: el mes en que donó muestra cuántas veces y una planta por donación (crecida si llegó,
  brote si va en camino, maceta si espera en el depósito) y lleva a ese mes en la lista de abajo; el
  mes sin donaciones y el que viene se ven neutros, en papel, sin marca de falta ni racha. Debajo, mes
  por mes, qué donó y a dónde llegó (este mes, con la foto de la entidad). Al costado, el año en
  cifras (con período y fuente), la próxima misión y las noticias. Componente nuevo `.almanaque`
  (`componentes/almanaque.css`, en el catálogo y en `/estilos`); `persona-inicio.css` solo dispone.
  Se borraron `variantes/dashboard-persona-a|b.html` y `variantes/dashboard-persona-b.css` (también
  en `target/classes`).
- **Misiones, nuevas a y b** (la **a** es la recomendada):
  - [a, figuritas literal](http://localhost:8091/donante-misiones?cuenta=persona&variante=misiones-a):
    una huerta armada (`img/ilustraciones/huerta-armada.svg`, hecha con los árboles, plantas, macetas
    y abejas existentes) partida en 4 cuadrantes. La pieza 1 llega con la cuenta; la 2, 3 y 4, al
    completar las misiones de Colaborador, Sostenedor y Transformador. La que falta, en gris. Cada
    pieza es un botón que abre un diálogo nativo (`popover`, sin JavaScript, foco y Esc del
    navegador): la obtenida dice qué hiciste y cuándo; la que falta, qué te falta (con la barra de la
    misión en curso). Las misiones de una categoría cerrada no se nombran. Al lado, las 4 piezas en
    texto.
  - [b, invernadero](http://localhost:8091/donante-misiones?cuenta=persona&variante=misiones-b): un
    invernadero de vidrio (`img/ilustraciones/invernadero.svg`) con un lugar por misión: Colaborador
    en el estante de la izquierda, Sostenedor en la mesada, Transformador en el de la derecha. La
    misión completa es una planta; la que está en curso, un brote; la que falta, un lugar con contorno
    punteado. Al costado, qué hay en cada lugar y qué falta para llenarlo (la categoría completa,
    plegada).
  - Las dos, también con `&escenario=vacio`, `lento` y `error`. Sin clases nuevas: ids y `data-`.
- Reglas generales: sin borde ni anillo verde de selección, cajas redondeadas, números e íconos
  centrados en sus círculos.
- Verificado: las URLs dan 200 (la de `dashboard-persona-b`, 404); 1280 y 375 px sin desborde;
  `InterfazFitnessTest` y `SitioControllerTest` en verde. Sin `revision-ui`.

## Tercera ronda (2026-11-09 noche)

### Portada, Entidades

**La tarjeta de entidad que dibujó la usuaria** (`Asset 21.svg`), ahora la pantalla base
([plegada](http://localhost:8091/publico-entidades),
[desplegada](http://localhost:8091/publico-entidades?abrir=comedor)). Las variantes `entidades-a|b`
se borraron (plantilla y hoja, también en `target/classes`; `?variante=entidades-a|b` da 404).

- Plegada, una fila en papel claro sin filete: la foto (por ahora, el ícono de la categoría centrado
  en una caja de salvia), el nombre en la letra de títulos, los chips de lo que pide (y
  «Emergencia»), «Necesidades satisfechas: X %» con su barra y «Ver todas las necesidades ⌄». En el
  celular se apila: foto con nombre y chips, la barra debajo y el enlace debajo.
- Desplegada: «**Misión:** …» recortada a tres renglones con desvanecido y «Leer más.» (un `details`,
  sin JavaScript); filete; «**Emergencia:**» con su contexto y el pedido (ícono de alerta en caja roja
  suave, lo que falta, hasta cuándo y la barra); filete; «Necesidades recurrentes» en verde con su
  `info`, en dos columnas (una en el celular); «Con fecha límite» igual, para los pedidos sin
  emergencia que no se repiten (el dibujo no los mostraba, pero «Ver todas» no puede esconderlos);
  al pie, centrado, «Ver sus entregas en el mapa» (`/publico-mapa#entidad-<clave>`, solo las que
  tienen entregas en el mapa).
- **Respaldo del %:** necesidades cubiertas del todo en octubre de 2026 sobre el total de ese mes (las
  cubiertas más las vigentes). Cuenta necesidades, nunca promedia kilos con litros. Se dice en una
  línea arriba de la lista, debajo de cada barra («Cubiertas: 3 de 8, en octubre») y en el nombre
  accesible de la barra. Datos de ejemplo en `fichas` (dentro de la plantilla, para no tocar
  `datos-publicos`, que comparten Mapa y Entregas): la misión, el contexto de la emergencia y las
  cubiertas por entidad (comedor 3 de 8, merendero 2 de 4, escuela 1 de 3, biblioteca 2 de 3, hogar 3
  de 4, club 0 de 1). Las necesidades son las de la maqueta, distintas por entidad.
- `?abrir=<clave>` abre una tarjeta (revisión y enlaces desde afuera).
- Componentes, en el catálogo y en `/estilos`: `.plegable` suma `.plegable-foto` y `.plegable-medida`
  (y queda sin filete, que aparece al pasar); nuevos `.necesidad` (`componentes/necesidad.css`, con
  `.necesidades`, `.necesidad-icono`, `.necesidad-texto`; `data-tono="emergencia"`) y `.recorte`
  (`componentes/recorte.css`, el «Leer más.» con `:has()` sobre el `details` abierto).

Verificado a 1280 y 375 px, plegada y desplegada, sin desborde horizontal; los estados vacío, lento
y error, y la búsqueda sin resultados dan 200. `InterfazFitnessTest` y `SitioControllerTest` pasan.

Pendiente: la foto real de cada entidad (hoy, el ícono); «Leer más.» no se vio abierto en captura
(headless sin clic); el espacio entre un grupo y el título del siguiente quedó justo.

### Huerta

La huerta de la diseñadora (`DonaTrack_Huerta*.svg`) reemplaza al invernadero de **misiones-b**.

- [Huerta](http://localhost:8091/donante-misiones?cuenta=persona&variante=misiones-b),
  [detalle de una planta](http://localhost:8091/donante-misiones?cuenta=persona&variante=misiones-b&lugar=estante-izq),
  [detalle de una maceta "?"](http://localhost:8091/donante-misiones?cuenta=persona&variante=misiones-b&lugar=mesada-medio)
  y [vacía](http://localhost:8091/donante-misiones?cuenta=persona&variante=misiones-b&escenario=vacio).
  El invernadero vacío de fondo y 8 lugares: la bienvenida (el jazmín de la mesada, llega con la
  cuenta), las 6 misiones (Racha Inicial, estante izquierdo; Explorador, piso izquierdo; Gran Aporte,
  Constancia, en la mesada; Impacto Real, estante derecho; Diversidad Solidaria, piso derecho) y la
  planta colgante de la huerta completa. Lo conseguido muestra su planta; lo que falta, la maceta
  negra con "?".
- Cada lugar es un enlace `?lugar=<clave>` (sin JavaScript), con nombre «planta: misión, conseguida»
  o «Maceta sin planta todavía: misión». Al elegirlo, el resto del dibujo se atenúa (sin borde de
  selección) y la ficha (a la derecha; debajo en el celular) dice qué hiciste, 3 hitos (llevaste al
  depósito, llegó, la entidad confirmó, con fechas de `persona/datos-persona`), «Ver la donación» y el
  mensaje de la entidad en un globo (`details`). La maceta "?" dice qué falta, con la barra si está en
  curso; la de una categoría que no se abre no se nombra. Sin elegir, la ficha resume y muestra la
  próxima en crecer.
- Capas separadas por script (fuera del repo) en `static/img/ilustraciones/huerta/`: `invernadero.svg`
  (21 KB), una planta y una maceta por lugar recortadas a su caja (las macetas, < 1 KB; las plantas, 3 a
  18 KB, salvo `colgante.svg`, 88 KB), `abejas.svg` (6 KB, con al menos una misión completa) y
  `acceso.svg` (106 KB). Metadatos fuera, decimales a 1, estilos solo de las clases usadas. Se borró
  `img/ilustraciones/invernadero.svg` (también en `target/classes`).
- Inicio de la persona: tarjeta «Tu huerta» con el invernadero lleno (sin el botón dibujado) y el
  botón real «Ver mi huerta» encima, a `misiones-b`.
- Sin clases nuevas: ids y `data-`. Verificado: 200 en todas las URLs, 1280 y 375 px sin desborde,
  `InterfazFitnessTest` y `SitioControllerTest` en verde. Sin `revision-ui`.
- Pendiente: decidir si la bienvenida y la planta colgante son logros reales (hoy solo de maqueta);
  en el celular los lugares chicos de la mesada miden unos 24 × 48 px; el globo del mensaje no se vio
  abierto en captura.

### Admin, Asignaciones (lote y conflictos)

Pedido: acceso directo al stock y a los rankings completos, propuestas en dos grupos (sin conflictos y
con conflictos), aceptar en lote las sin conflictos y llegar al detalle de la necesidad y de la entidad
de cada propuesta. Aplicado a las tres variantes (la **a** sigue siendo la recomendada).

**Supuestos.**

- La usuaria escribió "aceptar todas las conflictivas de una"; por contexto se tomó como **las sin
  conflictos**: aceptar en lote solo es seguro donde no hay choque. Las con conflictos se resuelven de
  a una.
- Una **propuesta** lleva una donación a la mejor candidata en común (menor puesto sumado; a igual suma,
  la de mejor puesto en Compatibilidad semántica). Si la necesidad pide más de lo que trae la donación,
  la propuesta suma stock del mismo bien que está en el depósito.
- Dos propuestas están **en conflicto** si se pisan: usan el mismo stock y no alcanza para las dos, o
  usan las mismas unidades (dos candidatas empatadas para una sola donación). "Elegir esta" **no
  asigna**: la ganadora pasa a Sin conflictos y la otra se vuelve a calcular con lo que queda o queda
  sin propuesta (su pedido sigue abierto). En la maqueta lo elegido viaja en la dirección
  (`&gana=<propuesta>`, se acumulan), por GET.
- El admin no tiene pantalla de necesidades: la necesidad se ve en un diálogo (qué pide, para qué,
  cuánto llegó, cuánto falta, hasta cuándo y cuánto cubre la propuesta), con enlace a la ficha de la
  entidad en Entidades (`/admin-entidades?entidad=<clave>`), que lista sus pedidos. La entidad, igual:
  diálogo con tipo, lugar y donaciones recibidas de agosto a octubre, y el enlace a su ficha cuando la
  tiene en la maqueta (Comedor Ejemplo Centro, Huerta, Cooperativa, Refugio y Parroquia no la tienen).
- Los envíos reales siguen yendo a `admin-asignaciones-aprobada` (no conservan la variante); el lote
  manda un `propuesta` por cada una. Lo que pasa después se ve con `&accion=lote` y `&accion=asignada`.

**Datos de ejemplo** (coherentes con Entidades del admin): 9 donaciones en depósito de 8 bienes. Sin
conflictos, 5: lentejas → Comedor Barrio Ejemplo, leche en polvo → Escuela Ejemplo N.º 12, aceite →
Cooperativa Ejemplo Este, frazadas → Hogar Casa Ejemplo y cuadernos → Biblioteca Popular Ejemplo. Con
conflictos, 2 pares: **arroz** (50 kg en dos donaciones; 25 kg para Comedor Barrio y 40 kg para Comedor
Ejemplo Centro suman 65 kg: "Si aceptás esta, quedan 25 kg de arroz en el depósito: no alcanzan los
40 kg de la propuesta para Comedor Ejemplo Centro") y **mesas plegables** (una donación de 4, Escuela
Ejemplo N.º 3 y Club Ejemplo Norte empatan: las dos usan las mismas 4 mesas). Sin coincidencias:
Camperas de abrigo. Los rankings de Compatibilidad semántica tienen hasta 10 puestos (menos cuando
pocas entidades piden ese bien, y lo dice); el de Prioridad a sub-atendidas, 10.

**Variantes.**

- [a](http://localhost:8091/admin-asignaciones?variante=asignaciones-a), recomendada. Selector de
  vistas tras el título (`.segmentos`): **Propuestas** (de partida; Sin conflictos con «Aceptar las 5
  sin conflictos» y su confirmación con la lista, Con conflictos de a pares con el porqué y «Elegir
  esta», Sin propuesta), **Por donación** (la bandeja de antes, con un renglón «Propuesta: a …» y «Ver
  los rankings completos»), **[Stock en depósito](http://localhost:8091/admin-asignaciones?variante=asignaciones-a&vista=stock)**
  (por categoría y bien: cuánto hay, de qué donaciones, cuántos días lleva cada una y qué propuesta lo
  usa o si se lo disputan dos) y **[Rankings](http://localhost:8091/admin-asignaciones?variante=asignaciones-a&vista=rankings&donacion=arroz-ana)**
  (elegir la donación; cada ejecución completa con el porqué, las en común y la propuesta marcadas).
  Después de elegir: [arroz para Comedor Ejemplo Centro y mesas para el Club](http://localhost:8091/admin-asignaciones?variante=asignaciones-a&gana=arroz-ccentro&gana=mesas-club).
- [b](http://localhost:8091/admin-asignaciones?variante=asignaciones-b). Todo en una página, con un
  índice de accesos arriba (5 sin conflictos · 2 conflictos · 9 en el stock · Rankings por donación):
  las sin conflictos en una **tabla** para compararlas, los pares en conflicto, el stock y una tarjeta
  por donación con su tabla de posiciones; «Ver los rankings» abre la de esa donación
  (`&abrir=<clave>#donacion-<clave>`).
- [c](http://localhost:8091/admin-asignaciones?variante=asignaciones-c). Empieza por «¿Qué querés
  hacer?»: [aceptar las 5 sin conflictos](http://localhost:8091/admin-asignaciones?variante=asignaciones-c&tramite=lote)
  (la página es la confirmación, sin diálogo), [resolver 2 conflictos](http://localhost:8091/admin-asignaciones?variante=asignaciones-c&tramite=conflictos)
  (uno por página, «Conflicto 1 de 2»; al terminar lleva al lote), elegir el destino de una donación
  (los tres pasos de antes, `&tramite=donacion`) y, como atajos, el stock (`&vista=stock`) y los
  rankings (`&vista=rankings`).
- Estados: `&accion=lote`, `&accion=asignada&donacion=<clave>&entidad=<k>`, `?escenario=vacio|error`.

Archivos: `templates/variantes/asignaciones-a|b|c.html`, `templates/variantes/comun/asignaciones-datos.html`
(datos y fragmentos nuevos: `vistas`, `entidad`, `necesidad`, `propuesta`, `lote`, `conflicto`, `stock`,
`elegirDonacion`, `rankings`), `static/css/variantes/asignaciones-a|b|c.css` y
`static/css/variantes/comun/asignaciones.css`. Sin clases nuevas (lo propio va por `data-`), sin
cambios en Java. Sin borde ni anillo verde: los conflictos van en fondo de advertencia, las propuestas
en lámina con sombra, todo redondeado. Verificado con curl (63 combinaciones, 200 y página completa) y
con capturas sin cabeza a 1280 y 375 px, sin desborde (a 375 se midió dentro de un `iframe` de 375 px:
la ventana sin cabeza de Chrome no baja de unos 500 px). `InterfazFitnessTest` y `SitioControllerTest`
en verde (59 pruebas).

Pendientes: las hojas de las variantes usaban `--medida-toque-minimo`, que no existe en `tokens.css`
(se cambió por `3rem` en las tres de Asignaciones); el número de propuestas en conflicto de la vista
Por donación no se recalcula si se asigna suelta una donación de un par (caso borde de la maqueta).
Sin `revision-ui`.

## Cuarta ronda (2026-11-10)

### Portada, Entidades (plegada)

[Plegada](http://localhost:8091/publico-entidades), [desplegada](http://localhost:8091/publico-entidades?abrir=comedor).

- **Plegada, como la marcó la usuaria** (`entidades-plegada.png`): en ancho (desde 68,75 em) cuatro
  columnas centradas en alto, de ancho fijo salvo la del nombre (foto de 7,5 rem, barra de 17 rem,
  enlace de 10 rem), así la barra y el enlace quedan alineados entre tarjetas; foto más grande en su
  caja de salvia; el enlace dice «Ver necesidades ⌄». Entre 53,75 y 68,75 em sigue la fila flexible;
  en el celular, apilada como antes. Más aire entre tarjetas (`data-espacio="3"`) y entre un grupo y
  el título del siguiente (`--espacio-5`: el `data-espacio` de cada sección redefinía
  `--espacio-entre` y la pila de afuera tomaba el chico).
- **El %**: ya variaba por entidad y sale de «Cubiertas: X de Y» (comedor 3 de 8 = 38 %, merendero
  2 de 4 = 50 %, escuela 1 de 3 = 33 %, biblioteca 2 de 3 = 67 %, hogar 3 de 4 = 75 %, club 0 de 1 =
  0 %); el 50 % parejo era del dibujo. Se deja la línea «Cubiertas: …» debajo de la barra.
- **Desplegada, más aire**: el cuerpo pasa a `--espacio-4` arriba y a los costados y `--espacio-5`
  abajo en el celular; `--espacio-5` a los costados en ancho.
- **Cabecera**: la cifra va al lado del manzano, nunca encima (el árbol con ancho y alto propios,
  5,5 rem de alto; la cifra a 3,5 rem, centrada con él). No se reprodujo el «6» tapado de la captura
  en Chrome a 900, 1280 ni 1920 px: puede ser una hoja vieja en caché del navegador.
- **La nota de «Necesidades satisfechas»**: una línea («las de octubre de 2026 ya cubiertas del todo.
  Al 15 de octubre de 2026.») con un `info` que tiene la explicación larga; pegada a los filtros (se
  sacó el margen de 2 rem). En el celular el `info` baja a su renglón. No va dentro de la tarjeta: un
  botón dentro del `summary` no se puede usar.

Solo `publico-entidades.html` y `publico-entidades.css`; sin clases nuevas. Verificado a 1280 y 375
px, plegada y desplegada; vacío, lento, error y búsqueda sin resultados dan 200.
`InterfazFitnessTest` y `SitioControllerTest` pasan.

### Bajo tierra

Portada, sección `huerta :: raices` (`componentes/huerta.html`, `componentes/huerta.css`), con el
dibujo de la diseñadora (`Bajo tierra.svg`). Ver en [/](http://localhost:8091/) y en
[/estilos](http://localhost:8091/estilos) (muestra `.huerta-bajo`).

- **Raíces del dibujo, sin textos ni textura.** Se sacaron del SVG original (1,9 MB, con el bitmap de
  la tierra y los rótulos en contornos) solo las raíces, los nudos y el tallo, con los decimales
  redondeados: quedó un SVG decorativo inline de unos 8 KB, pintado con los tokens de la tierra
  (`--color-raiz`, `--color-suelo-hondo`). La textura es la de siempre (`img/tierra-textura.svg`) y los
  estratos son líneas finas cada 8,5 rem en CSS.
- **Los rótulos son HTML real** encima del dibujo, en porcentajes (`data-lugar`), con la letra a escala
  del ancho (nunca menor de 14 px): una lista de categorías (amarillo, letra de títulos) con sus tipos
  de entidad (crema). Alimentos: comedores (antes decía "Comederos"), merenderos y organizaciones
  religiosas. Ropa: clubes y hogares. Muebles y útiles: escuelas y centros comunitarios. Los lectores
  de pantalla leen esa misma lista; el dibujo va oculto para ellos.
- **"Tu entidad"**, al final de la raicilla punteada, es un enlace al registro de entidad
  (`/publico-registro-entidad`; para los lectores de pantalla, "Tu entidad: sumala a DonaTrack").
- **Celular (menos de 37,5 em).** Sin dibujo: primero el título y el texto, después la misma lista como
  tres raíces apiladas, cada una con su nudo, su línea que baja y una rama corta por tipo de entidad, y
  "Tu entidad" al final de una línea punteada. Sin desborde a 375 px.
- **Desde 37,5 em** el tallo sale del borde de arriba de la tierra, debajo del árbol de "Cómo funciona".
- **Raíces de lo donado (`raices-donante`)**, coherentes con el dibujo nuevo: raíces más gruesas y el
  nudo con el anillo de tierra honda. El resto de esa pieza no cambió.
- Se borraron los estilos que ya no usa nadie (raicillas, carteles de herbario, raíces de tercer y
  cuarto nivel).

Pendiente: el texto de los tipos por categoría es una muestra del dibujo, no sale de datos; cuando haya
backend, conviene que salga de las categorías y tipos reales.

### Admin, Asignaciones (tarjeta de propuesta y conflictos ponderados)

[a](http://localhost:8091/admin-asignaciones?variante=asignaciones-a),
[b](http://localhost:8091/admin-asignaciones?variante=asignaciones-b#conflictos),
[c, conflictos](http://localhost:8091/admin-asignaciones?variante=asignaciones-c&tramite=conflictos),
[c, lote](http://localhost:8091/admin-asignaciones?variante=asignaciones-c&tramite=lote).

- **Tarjeta de la propuesta** (maqueta de la usuaria), en el fragmento común `propuesta`: lámina clara
  con sombra de papel en tres filas. 1) Ícono de la categoría en su caja, «Aceite de girasol, 20 litros»
  (negrita, tinta suave) → la entidad (enlace verde subrayado que abre su detalle). 2) Los puestos en
  insignias («1.º», «2.º», fondo acento claro) con el nombre del algoritmo y, a la derecha, «Cobertura al
  asignar: 0 % → 100 %» en una caja de papel (`--color-pendiente-fondo`); la cobertura sale de lo que ya
  llegó y lo que suma la propuesta. 3) «Confirmar asignación» (POST directo, sin diálogo: la tarjeta ya
  muestra lo que confirmaba el diálogo) y, a la derecha, «Ver la necesidad» y «Ver los rankings». La b
  sigue con su tabla para las sin conflictos.
- **Conflictos con cantidades ponderadas** (fragmentos `conflictos` y `conflicto`, iguales en las tres).
  A la pregunta "Sin cambiar cómo funcionan los algoritmos de asignación, ¿se puede hacer algo así?": sí.
  Los algoritmos siguen armando sus rankings por donación; esto es solo cómo se presenta cuando el stock
  de un bien no alcanza para todas las necesidades que compiten por él. **Regla de la cantidad
  ponderada:** el peso de cada entidad es la suma de 1 / puesto en cada algoritmo; cada una recibe
  stock × peso / suma de pesos, sin pasar de lo que necesita (lo que sobra se reparte entre las demás con
  la misma regla); se redondea a la unidad y la unidad que falta va a la de mayor resto. Ejemplos: arroz,
  50 kg para Comedor Barrio Ejemplo (1.º y 2.º, necesita 30) y Comedor Ejemplo Centro (1.º y 9.º,
  necesita 40) → 29 y 21 kg; mesas plegables, 4 para Escuela Ejemplo N.º 3 (1.º y 7.º, necesita 5) y
  Club Ejemplo Norte (4.º y 4.º, necesita 6) → 3 y 1.
- Título «Con conflictos · 3 donaciones independientes», la explicación y «Aceptar todas las cantidades
  ponderadas» (con confirmación que lista cada reparto), con aire antes del primer conflicto (margen
  propio más el de la pila). Cada conflicto es una banda de fondo de advertencia: «En el depósito: 50 kg
  de arroz», por qué no alcanza, la suma («Asignás 50 de 50 kg.», `role="status"`) y «Asignar»; al lado,
  una lámina por entidad con su nombre, «Necesitan 30 kg antes del 30/11/2026», sus puestos, la cantidad
  (− [n] kg +, un `input type="number"` real con `max` = lo que necesita) y la cantidad ponderada. El
  conflicto de las mesas muestra cantidades cambiadas a mano (5 y 3, pasan las 4): en rojo (fondo y texto
  de problema), lo dice y «Asignar» queda con `aria-disabled`.
- Mejora progresiva en `static/js/asignaciones.js` (script plano con delegación en el documento, cargado
  desde el fragmento `datos`; no se registra dos veces si llega con un panel de HTMX): los botones − y +
  aparecen con JavaScript y la suma se revisa al cambiar un valor. Sin JavaScript se escribe el número y
  se envía; el servidor debería validar la suma (la maqueta no valida: el POST vuelve, como antes, a
  `admin-asignaciones-aprobada`).
- Datos corregidos para que cierren: Comedor Barrio Ejemplo necesita 30 kg de arroz (llegaron 10 de
  40); Escuela Ejemplo N.º 3, 5 mesas (llegó 1 de 6); Club Ejemplo Norte, 6 mesas. El «Elegir esta» por
  pares (`&gana=`) ya no tiene botón, pero las direcciones viejas siguen andando.
- Mismo panel sin recargar (`hx-boost`): el selector de vistas, la búsqueda y los filtros, elegir la
  donación de los rankings, «Ver los rankings», la cola de la a, los enlaces del stock y los pasos de la c
  (estos dos, y «Ver los rankings», vuelven arriba: `show:window:top`). Los POST no.
- Sin clases nuevas (todo por `data-`), sin cambios en Java. Verificado con curl (22 combinaciones, 200
  y página completa) y en el navegador a 1280 y 375 px sin desborde. Sin `revision-ui`.
