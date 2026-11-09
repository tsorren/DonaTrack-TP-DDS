# Ronda de ajustes del 2026-11-09

Pedido de la usuaria del 2026-11-09: una lista de ajustes por pantalla, varias pruebas A/B/C y las
abejas de la portada con la opción B (caminos separados, adelante o detrás de cada lámina). Este
plan reparte el trabajo y deja escrito qué se hizo en cada parte.

## Cómo se ven las pruebas A/B/C

Con el mecanismo de siempre (`cliente-liviano/AGENTS.md`):

- `?variante=<tema>-a|b|c` suma `static/css/variantes/<tema>-x.css` o reemplaza la pantalla por
  `templates/variantes/<tema>-x.html`.
- Las variantes se borran al decidir.
- La opción **a** es siempre la propuesta recomendada.
- La pantalla sin `?variante=` queda como estaba, salvo los arreglos que no son de gusto.

## Reglas generales, en todas las pantallas

1. **Encabezado de sección.** Si el título no tiene ilustración al lado, el contenedor ocupa todo el
   ancho de la página: el título, la bajada, la tira de cifras y los botones.
   - Sin topes de 38 o 48 rem en la tira de cifras.
   - La bajada conserva un largo de línea legible, de unos 70 caracteres.
2. **Desplegable con botón.** El botón queda alineado al centro vertical del desplegable, no al
   rótulo. Lo mismo para la búsqueda con su botón.
3. **Flecha de los desplegables.** Tiene aire a la derecha (`padding-inline-end` y
   `background-position`); no queda pegada al borde. La caja del desplegable va redondeada.
4. **Cajas redondeadas.** Ninguna caja, tarjeta, barra de progreso ni campo queda con esquinas
   rectas. Incluye el estado `:hover`, que no debe perder el redondeo.

## Reparto

| Parte | Pantallas | Quién |
|---|---|---|
| Abejas opción B, pausa en la cabecera | portada (hero) | sesión principal |
| Reglas generales en componentes compartidos | `componentes/campo.css`, `filtros.css` | sesión principal |
| Portada | Cómo funciona, Bajo tierra, Participá, Entidades, Mapa | agente portada |
| Registro | `publico-registro*.html` | agente registro |
| Distribuidora (donante empresa) | `donante-*` | agente distribuidora |
| Donante persona (Martina Ruiz) | `templates/persona/donante-*.html`, `donante-misiones` | agente persona |
| Beneficiario (entidad) | `entidad-*` | agente entidad |
| Administración | `admin-*` | agente admin |

La persona donante ahora puede tener pantallas propias: si existe
`templates/persona/<pantalla>.html`, se muestra en lugar del aviso de la maqueta
(`SitioController`).

## Resultado

Lo completa cada parte al terminar, con las URLs de las pruebas A/B/C.

### Registro (agente registro)

Pantallas: `/publico-registro`, `/publico-registro-donante` (y `?tipo=persona`),
`/publico-registro-entidad` y sus estados de error. Sin pruebas A/B/C.

- **Textos de ayuda y ejemplos.** Se sacaron los ejemplos que repetían la ayuda: la contraseña ya no
  dice "Al menos 8 caracteres" dentro del campo (queda la ayuda, que es la regla). Se quedaron las
  ayudas que dicen quién ve el dato, para qué se usa o qué pasa después (nombre de la empresa o de la
  entidad, correo, "Qué es", tipo de organización, dirección de entrega). La altura muestra solo
  "4500", que entraba cortado en el campo angosto.
- **Nombre y apellido sin ejemplo** en los tres formularios (empresa, persona y representante de la
  entidad).
- **Índice de pasos fijo arriba** (`componentes/registro.css`): ahora queda pegado al bajar (antes el
  `sticky` estaba en la lista y no tenía recorrido). En el celular, que no lo mostraba, es una franja
  de papel con los pasos en fila. Con JavaScript (`publico.js`, al final), cada punto se llena por
  cuartos según los campos obligatorios de su sección que ya están bien (`validity`), relleno cuando
  la sección está completa; los lectores de pantalla oyen "La entidad: completo", "a medias" o "sin
  empezar". Sin JavaScript, el índice se ve como antes.
- **Tipo de organización como desplegable** (`<select id="tipo">`, mismas tres opciones y la misma
  ayuda), con la flecha de la regla global de `campo.css`.
- Reglas generales: la cabeza del registro ya va centrada a todo el ancho; las tarjetas del tipo de
  cuenta conservan el redondeo en `:hover`. Verificado a 1280 y 375 px, sin desbordes ni errores en
  la consola.


### Abejas y pausa (sesión principal)

- **Abejas, opción B.** Las abejas del hero siguen los recorridos nuevos de `assets/DonaTrack_Abeja1, 2
  y 3.svg`, una por ciclo. Van por delante de las tres láminas en los tramos llenos del dibujo y por
  detrás en los punteados (`static/js/abejas.js`).
  - Las láminas se ubican con el mismo encuadre del dibujo (`componentes/huerta.css`).
  - Sin JavaScript, con la pausa o con "reducir movimiento", las abejas quedan quietas adelante.
- **Se borró la prueba A/B de abejas** (`abejas-ab.html` e `img/abejas-ab/`).
- **La pausa pasó a la cabecera**, junto a la marca (fragmento `huerta :: pausa`).
  - En esta rama no hay botón de modo día/noche. Queda pendiente decidir si se agrega.
- **Persona donante.** `SitioController` muestra `templates/persona/<pantalla>.html` si existe.

### Distribuidora (agente distribuidora)

Donante empresa, Distribuidora Ejemplo SA. Arreglos sin prueba (se ven sin `?variante=`):

- **Abeja de las láminas** (`componentes/huerta.css`, `movimiento.css`). Ya no sube y baja en la
  esquina: da una vuelta corta alrededor de su planta y vuelve a su lugar, una sola vez (4,4 s), con
  la animación nueva `--movimiento-animacion-ronda`. Las láminas pares salen un poco después. Sin
  movimiento con "reducir movimiento" o con la pausa.
- **Raíces de lo que donaste.** El texto es más corto y queda fijo en dos renglones
  (`data-renglones="2"`): si no entra, se recorta con puntos suspensivos.
- **Raíces y Últimas noticias, del mismo alto.** La fila se estira (`align-items: stretch`) y las dos
  la llenan.
- **Filtros de Mis donaciones y Entidades** (`donante-comun.css`). Las cajas y los desplegables son
  redondeados y la flecha queda a 1 rem del borde. La regla global de `campo.css` no alcanzaba: sin
  Pico, la flecha es la del navegador y no se corre. Acá se dibuja con dos degradados.
- **Pie de Mis donaciones.** El texto y "Ver seguimiento" van centrados, en el 80 % del ancho y con
  más aire sobre las láminas. Es el único "Ver seguimiento" debajo de las tiles.
- **Seguimiento.**
  - Se sacó la abeja del dibujo bajo tierra.
  - Las tarjetas de "Entrega por entrega" tienen otra estructura, sin la columna del número.
  - Tres grupos: qué es (número, nombre y cantidad en una pastilla), a dónde va (categoría y
    entidad, cada una con su ícono) y cómo va (estado, fecha con reloj, etapas y última noticia,
    sobre un panel propio).
  - "Esperan destino" quedó con las esquinas redondeadas.

Pruebas A/B/C (la **a** es la recomendada):

- Inicio, estructura (eje: cómo se ordena lo que la empresa necesita saber al entrar):
  - [a](http://localhost:8091/donante-inicio?variante=inicio-empresa-a), el mes en cuatro
    preguntas: qué dio fruto (cifras grandes con su planta), un tablero (ya llegó / en camino o por
    salir / para tener en cuenta, con el dibujo de cada donación), el recorrido de la que va en
    camino y, al final, lo acumulado (raíces y noticias).
  - [b](http://localhost:8091/donante-inicio?variante=inicio-empresa-b), octubre como línea de
    tiempo: franja de cifras y un día por suceso con su dibujo; después de "Hoy", lo programado con
    borde punteado.
  - [c](http://localhost:8091/donante-inicio?variante=inicio-empresa-c), la huerta en una sola
    lámina: tres canteros con su cifra (dieron fruto, van creciendo, en el depósito), las raíces
    debajo, una nota con cinta para lo que conviene mirar y las noticias en fila.
- Tile de Últimas noticias, solo CSS (eje: cómo se lee lo último que pasó). En las tres, del mismo
  alto que las raíces:
  - [a](http://localhost:8091/donante-inicio?variante=noticias-a), línea de tiempo con un punto del
    color del estado.
  - [b](http://localhost:8091/donante-inicio?variante=noticias-b), la más nueva destacada y las
    otras dos en un renglón.
  - [c](http://localhost:8091/donante-inicio?variante=noticias-c), fichas con una franja del color
    del estado.
- Entidades (eje: qué se ve primero de cada entidad). Las tres conservan la búsqueda, el paginado y
  los escenarios:
  - [a](http://localhost:8091/donante-entidades?variante=entidades-donante-a), una lámina por
    entidad con la planta de lo que pide (y la abeja si ya le donaste), íconos de tipo y zona, y
    barras redondeadas.
  - [b](http://localhost:8091/donante-entidades?variante=entidades-donante-b), un mapa de
    parcelas por zona con un sello numerado por entidad y, al lado, la ficha con el mismo número.
  - [c](http://localhost:8091/donante-entidades?variante=entidades-donante-c), el avance de los
    pedidos por categoría, con barras gruesas y su porcentaje.

Para que el servidor de la maqueta reconozca una variante nueva, el archivo tiene que estar también
en `target/classes`: `VariantesInterceptor` busca los nombres en el classpath, aunque las plantillas
y las hojas se lean de `src/`. Se copiaron ahí. Lo propio de cada variante va con atributos
`data-v`, para no sumar clases fuera del catálogo.

### Portada, Entidades y Mapa (agente portada)

Arreglos sin prueba (se ven sin `?variante=`):

- **Cómo funciona, macetas a escala** (`componentes/huerta.html`, `huerta.css`).
  - Las ilustraciones de los pasos tenían macetas distintas: el paso 1 una más chica y el paso 3
    una más alta. Además, cada dibujo se encajaba en la misma altura (`object-fit`), así que el
    paso 2 se veía más grande que el 3.
  - Dos dibujos nuevos con la maceta común (la del paso 2, del naranjo y de la menta):
    `img/ilustraciones/como-paso-1.svg` (el sobre y las semillas) y `como-paso-3.svg` (el brote).
  - Los pasos usan la escala de las láminas: `data-dibujo` y `--medida-alto`, por
    `--medida-escala-paso`. La maceta mide lo mismo en los cuatro y la planta crece: semillas,
    brote, hojas y el naranjo con frutos en el paso 4 (antes, un nudo).
  - `paso-1-semillas.svg` y `paso-3-brote.svg` no se tocaron: los usan el hero y la lámina "en
    camino". **Pendiente:** la lámina "en camino" (`huerta :: planta`) sigue con la maceta alta de
    `paso-3-brote`; puede pasar a `como-paso-3` cuando se decida.
- **Bajo tierra** (`huerta :: raices`).
  - Raicillas finas que salen de las raíces principales y de las secundarias (`.huerta-raicilla`).
  - Las categorías, como carteles de herbario colgados de su nudo con un hilo, con su ícono
    (sprite `iconos :: categorias`) y el nombre en tinta sobre paja.
  - La raíz sigue desde el pie del cartel: ninguna línea pisa el texto.
  - En el celular, los rótulos de las entidades van más grandes.
- **Quién participa.** "Qué podés donar", "Qué puede donar una empresa" y "Cómo se suma una entidad"
  son ahora un ícono de información al lado del botón.
  - Es un componente nuevo, `componentes/info.html` + `componentes/info.css`, registrado en el
    catálogo y en `/estilos`.
  - Botón de 48 px con foco visible, que abre un globo (popover nativo, sin JavaScript) con el
    detalle y el enlace a la página completa.
- **Entidades, hero a todo el ancho** (regla general 1). El título y la bajada (hasta 70 caracteres)
  a la izquierda; la cifra del árbol a la derecha, en una columna de 20 a 28 rem, en dos renglones.
- **Mapa real** (`publico-mapa.html`, `componentes/mapa.css`, `static/js/mapa.js`).
  - El dibujo de la red de subte pasó a ser un mapa de MapLibre GL con el estilo `positron` de
    OpenFreeMap, como fija D21.
  - Un marcador numerado por entidad, un botón de 48 px. Su globo (nombre, entregas del último mes y
    lo que pide) lo arma el servidor en un `<template>` de cada ficha.
  - La atribución se ve siempre ("OpenFreeMap © OpenMapTiles Data from OpenStreetMap").
  - Al pasar o enfocar una ficha de la lista, su marcador se destaca y el mapa va hasta ella; con
    `#entidad-<clave>`, se abre su globo.
  - Sin JavaScript se ve un aviso y la lista con los mismos datos (RF-CL-06).
  - Las coordenadas son sintéticas, una por barrio de CABA: `lat` y `lng` en `datos-publicos.html`,
    que además suma `faltan` y `total` numéricos a cada pedido.
- **Decisión sobre la carga de MapLibre.** Se agregó el WebJar `org.webjars.npm:maplibre-gl` al
  `pom.xml`, con la versión en `maplibre.version`, sin versión en la URL (`/webjars/maplibre-gl/dist/`).
  - La versión es **5.9.0**, no la 4.7.1 del pedido: en Maven Central no hay WebJar 4.x.
  - `mapa.js` prueba primero el WebJar y, si no responde, carga la misma versión desde
    `https://unpkg.com/maplibre-gl@5.9.0/dist/` (js y css).
  - Hasta que se reinicie el servidor, el WebJar da 404 (se ve en la consola) y el mapa sale de unpkg.
  - Al cambiar la versión, hay que cambiar las dos: el `pom.xml` y `VERSION` en `mapa.js`.

Prueba A/B/C de Entidades, de estructura (la **a** es la recomendada). Cada variante cambia solo
cómo se muestran las entidades. La cabeza, los filtros, los estados y las salidas son los de
`publico-entidades.html`: el bloque se pasa en `vistaEntidades`.

- [a](http://localhost:8091/publico-entidades?variante=entidades-a). Eje: cuánto falta, a la vista.
  - Láminas como las de la portada, con la maceta común y la planta según lo que ya llegó de sus
    pedidos: en la maceta, hasta un cuarto; brote, hasta la mitad; planta de su categoría, desde
    ahí.
  - Cada pedido lleva su barra redondeada (`.progreso`) y, en palabras, lo que falta.
- [b](http://localhost:8091/publico-entidades?variante=entidades-b). Eje: recorrer rápido.
  - Una lista compacta agrupada por tipo: comedores y merenderos, escuelas y bibliotecas, hogares y
    clubes.
  - Un renglón por entidad, con chips de categoría con su ícono, el chip "Emergencia" y los pedidos
    desplegables (`details`).
- [c](http://localhost:8091/publico-entidades?variante=entidades-c). Eje: el conjunto de un
  vistazo.
  - Un vivero sobre la tierra: una parcela por entidad, con su cartel, y una maceta por pedido en
    su etapa. La emergencia se marca con un punto.
  - Los pedidos se abren en una ventana (`.dialogo` popover).

Archivos de la prueba: `templates/variantes/entidades-a|b|c.html` y
`static/css/variantes/entidades-a|b|c.css`, copiados también en `target/classes`.

Verificado a 1280 y 375 px, sin desbordes. En la consola solo aparecen los 404 del WebJar, que se
esperan hasta reiniciar.

### Beneficiario (agente entidad)

Comedor Barrio Ejemplo. Arreglos sin prueba (se ven sin `?variante=`):

- **Necesidades, tarjeta rearmada** (`entidad-necesidades :: pedido(n)`, fragmento nuevo que usan la
  lista y las variantes). Tres grupos con separación clara:
  - Qué hace falta: pictograma redondeado, nombre y, debajo, categoría y si se repite.
  - Cuánto llegó: la cifra grande ("10 de 40 kg recibidos") y la barra, en su caja.
  - Hasta cuándo y cómo vienen sus entregas: el plazo y una pastilla de estado ("1 entrega espera tu
    confirmación", "1 entrega con un problema", "Sin entregas en curso").
  - Al pie, "Ver el detalle" (o "Cerrar", abierta); las acciones siguen al final del detalle.
- **Redondeo en `:hover`.** El resumen tiene el radio de la tarjeta (abierta, solo arriba): el fondo
  del hover ya no deja esquinas rectas.
- **Barras redondeadas.** `componentes/progreso.css` pasó a pastilla (afecta también a Misiones y
  al catálogo, para bien). Necesidades (también las barras de los períodos) e Inicio usan el
  componente. El cuadro del pictograma de categoría (`sellos.css`) va redondeado en todo el sitio.
- **Encabezado a todo el ancho** en Necesidades: sin el tope de 64 rem de la pantalla ni el de 48 rem
  de la tira de cifras; la bajada, a 70 caracteres.
- **Inicio, "Tus pedidos":** cada renglón en su caja redondeada, con la barra redondeada. En "En tu
  puerta" se sacó el "Espera tu confirmación" de cada acta (lo dice la parte).
- **Donaciones recibidas:** la línea de subte de estados se cambió por el componente de filtros
  (`componentes/filtros :: interruptor`), una pastilla por estado con su cantidad. Se pueden prender
  varias (`?estado=en-camino&estado=por-salir`); "Filtrar" queda en la fila y "Sacar los filtros"
  aparece con un filtro puesto. Los enlaces viejos (`?estado=recibidas`) siguen andando. Bajo
  "Esperan tu confirmación" se sacó la pastilla repetida: cada acta dice cuándo llegó.
- **Seguir entregas:**
  - El sistema de raíces se dio vuelta: la puerta de la entidad arriba, en la línea de tierra, con
    su dirección, y una raíz por entrega que baja hasta su depósito.
  - "Tus entregas, por estado" va arriba del dibujo, en el mismo bloque. Cada cifra lleva a la
    primera entrega de su estado, que queda resaltada.
  - En pantalla grande ese bloque queda fijo arriba (sticky) y la lista se mueve al lado. En el
    celular y la tableta no queda fijo, porque taparía casi toda la pantalla.
- Se sacaron `acta-estado` y `filtro-linea` de `clases-heredadas.txt` (ya no las usa ninguna
  plantilla).

Pruebas A/B/C de Necesidades (eje: cómo se ve el avance de cada pedido; la **a** es la
recomendada):

- [a](http://localhost:8091/entidad-necesidades?variante=necesidades-a), láminas que crecen: el
  pictograma grande dentro de un anillo que se llena con lo que llegó, y la planta de la huerta
  (semillas, maceta, brote, árbol) que crece al lado de la cifra según el avance. Solo CSS.
- [b](http://localhost:8091/entidad-necesidades?variante=necesidades-b), tablero por categoría: un
  cantero por categoría con su pictograma grande, cuántos pedidos tiene y cuántas entregas esperan,
  y las tarjetas compactas adentro (de a tres en pantalla grande). Estructura:
  `templates/variantes/necesidades-b.html`.
- [c](http://localhost:8091/entidad-necesidades?variante=necesidades-c), frascos en el estante: cada
  pedido es un frasco con tapa y marcas de los cuartos que se llena de abajo arriba, con la cifra
  grande al lado; las tarjetas se apoyan en un estante. Solo CSS.

Las variantes usan `data-avance` (de a 10) y `data-etapa` (1 a 4) de la tarjeta, porque no pueden
poner un `style`. Verificado a 1280 y 375 px, sin desbordes; sin correr Maven.

### Donante persona, Martina Ruiz (agente persona)

Se entra con `http://localhost:8091/donante-misiones?cuenta=persona` (queda en la sesión;
`?cuenta=empresa` vuelve a la empresa). Arreglos y pantallas nuevas (se ven sin `?variante=`):

- **Pantallas de la persona completas** (`templates/persona/`). Ya no aparece el aviso de la maqueta:
  - Datos de ejemplo en un solo lugar, `persona/datos-persona.html`: 8 donaciones chicas, una por mes
    desde junio (alimentos, ropa y una caja de útiles), coherentes con Misiones (Racha Inicial,
    Explorador, ascenso a Sostenedor el 2 de septiembre, Gran Aporte 12 de 50 con la ropa de abrigo)
    y con la entidad (los 5 kg de arroz recibidos el 9 de octubre por Comedor Barrio Ejemplo). Hoy,
    la ropa va en camino, la leche espera el camión y los fideos esperan destino.
  - `donante-inicio` (nuevo dashboard, ver la prueba), `donante-donaciones`,
    `donante-donaciones-detalle` (el cuerpo de la empresa con los datos de la persona),
    `donante-seguimiento` (dibujo propio con sus dos entregas), `donante-entidades` ("Lo que ya
    donaste" en cada entidad), `donante-notificaciones` y `-leidas` (suma los avisos de misiones:
    misión completa y ascenso) y `donante-perfil` y `-guardado` (nombre, apellido, documento y fecha
    de nacimiento en vez de razón social y CUIT; "Persona donante · Sostenedor" si oculta su nombre).
  - Los mismos `?escenario=vacio`, `error` y `lento` de la empresa (y `oculto`, `sin-conexion` en
    Mi perfil). La campana del layout muestra 3 sin leer para la persona.
- **Misiones** (`donante-misiones.html`, `donante-misiones.css`):
  - La categoría completa queda plegada en un desplegable cerrado (Colaborador · completa).
  - Solo se ve la categoría actual con sus misiones. Las siguientes muestran su nombre y "Sus
    misiones se ven cuando completes las de …", sin nombrarlas; las insignias de esas categorías
    tampoco se nombran ("2 más, en Transformador").
  - Las tarjetas tienen aire entre sí (`gap`) y el encabezado va a todo el ancho.
  - La pantalla se partió en `datos(vista)` y `cuerpo` para que las variantes usen los mismos datos.
- **"novedades urgentes sin leer"** en el acceso a Notificaciones de la entidad
  (`piezas-entidad :: novedades`, con singular "novedad urgente sin leer"). Del lado del donante el
  texto no aparece ("Ver todas (N sin leer)").

Pruebas A/B (la **a** es la recomendada):

- Misiones (eje: qué metáfora ordena las misiones). Las dos respetan el desplegable y lo oculto:
  - [a](http://localhost:8091/donante-misiones?cuenta=persona&variante=misiones-a), álbum de
    figuritas: una tapa con los 6 casilleros, una página por categoría y una figurita numerada por
    misión. Pegada (borde de papel, leve giro) si está completa; hueco punteado con la silueta, lo que
    pide y la barra si falta. La página completa, plegada con sus figuritas en miniatura; la que
    sigue, un sobre cerrado.
  - [b](http://localhost:8091/donante-misiones?cuenta=persona&variante=misiones-b), huerta: las
    cuatro etapas (semilla, maceta, brote y planta) explicadas arriba, un cantero por categoría y una
    planta por misión parada en su tierra, en la etapa que le toca. El cantero que sigue es tierra
    sin sembrar.
- Inicio de la persona (eje: qué es lo protagonista):
  - [a](http://localhost:8091/donante-inicio?cuenta=persona&variante=dashboard-persona-a), su
    huerta (es la pantalla base): una planta por donación de junio a hoy, cada una con su estado y
    enlace al detalle; este mes (4 donaciones y la foto del arroz que llegó); la próxima misión con su
    barra; el recorrido de la ropa en camino y las últimas noticias.
  - [b](http://localhost:8091/donante-inicio?cuenta=persona&variante=dashboard-persona-b), su mes
    en un almanaque: la hoja de octubre con un color por lo que pasó cada día (donaste, se asignó,
    llegó, en camino, lo que viene), la cronología día por día y, al costado, las cifras del mes, la
    próxima misión y las noticias.

Archivos de la prueba: `templates/variantes/misiones-a|b.html`, `dashboard-persona-a|b.html` y
`static/css/variantes/misiones-a|b.css`, `dashboard-persona-b.css`, copiados también en
`target/classes` (igual que `templates/persona/`, porque `SitioController` busca las plantillas de
la persona en el classpath). Sin clases nuevas: lo propio va por id y atributos `data-`. Verificado
a 1280 y 375 px, sin desbordes; sin correr Maven.

### Administración (agente admin)

Leo, de Administración. Arreglos sin prueba (se ven sin `?variante=`):

- **Reglas generales en todo el admin** (`admin-inicio.css`, `admin-secciones.css`).
  - La mesa ocupa todo el ancho de la página (sin el tope de 76 rem) y la bajada del encabezado
    llega a 70 caracteres (antes, 40 rem: cortaba "7 en el depósito · 4 asignadas…").
  - Si el encabezado lleva acciones, van a la derecha del texto y, en el celular, debajo.
  - Un desplegable o una búsqueda con su botón: los dos miden 48 px y quedan alineados entre sí,
    no con el rótulo (Donaciones, Ranking "Ver otro mes", Registrar donación, Entidades activas).
- **Flecha de los desplegables** (`componentes/campo.css`, al final). La regla global no movía la
  flecha: sin Pico, es la del navegador. Ahora todos los `select` del sitio dibujan su flecha con dos
  degradados de tinta, a 1 rem del borde (como hizo la distribuidora en `donante-comun.css`).
- **Donantes.** Se sacó "Donantes, del más nuevo al más antiguo" (queda solo para lectores).
  - "Registrar donante" e "Importar CSV" al costado del encabezado. "Importar CSV" es un enlace
    real (`?importar=abrir#importar`) que abre el bloque de importar, con "Cerrar".
  - Búsqueda en una sola fila con su botón, y filtros con el componente de pastillas: tipo
    (personas, empresas, organizaciones), qué dona, estado de la cuenta (activo, sin activar, dado
    de baja) y orden (más nuevos, más antiguos, por nombre). Formulario GET real; 10 donantes de
    ejemplo, con la columna "Qué dona" y el estado con su sello.
- **Donaciones.** "Registrar donación" al costado del encabezado; las cifras y la fecha en un renglón
  a todo el ancho; el filtro alineado.
- **Camiones.** Los deshabilitados quedan apagados: fila sobre papel gris, texto en la tinta suave
  (5:1, AA), sello en gris y "Deshabilitado" en palabras; en el celular, borde punteado. Es una
  variante nueva del bloque tabla, `tr data-atenuada="si"` (`componentes/tabla.css`, registrada en
  el catálogo). Los disponibles llevan ahora el sello verde. Búsqueda por patente o nombre (cada
  camión tiene un nombre de ejemplo) y filtros de estado y capacidad, GET real.
- **Campana del admin.** `layout-privado.html` suma la campana para la administración (3 sin leer,
  ninguna en `-leidas`), sin tocar Java: se reconoce por la cuenta. En pantalla grande va junto a la
  cuenta (`privado.css`). Pantallas nuevas: `admin-notificaciones` y `admin-notificaciones-leidas`
  (bloque noticias: la importación terminada, las 3 entidades que piden sumarse, el camión
  deshabilitado; leídas: propuestas, vencimientos y ranking), con `?escenario=vacio|lento|error`.

Prueba A/B/C de Entidades (estructura; la **a** es la recomendada). Las tres tienen arriba las que
piden sumarse con Aprobar, Pedir corrección y Rechazar (diálogos popover, sin JavaScript), y las
activas con su estado y sus pedidos vigentes, con íconos de categoría:

- [a](http://localhost:8091/admin-entidades?variante=admin-entidades-a). Eje: bandeja y directorio.
  Tira de cifras, una lámina por solicitud con lo que mandó (tildes y cruces) y cuánto hace que
  espera, y las activas como láminas con sus pedidos en etiquetas; búsqueda y filtros (pedidos,
  tipo).
- [b](http://localhost:8091/admin-entidades?variante=admin-entidades-b). Eje: tablero por estado.
  Tres columnas: piden sumarse, con pedidos vigentes y sin pedidos vigentes.
- [c](http://localhost:8091/admin-entidades?variante=admin-entidades-c). Eje: una a la vez. La lista
  a la izquierda y la ficha de la elegida (`&entidad=<clave>`) a la derecha, con sus datos, lo que
  mandó o sus pedidos.

Prueba A/B de las propuestas de asignación (estructura; la **a** es la recomendada). Las dos agrupan
por estado y muestran la donación → la entidad, cuánto cubre (barra y palabras) y por qué se propone,
con datos que ya existen (categoría, cobertura, vencimiento, antigüedad del pedido): el puntaje del
algoritmo no se inventa porque el backend no lo expone.

- [a](http://localhost:8091/admin-asignaciones?variante=propuestas-a). Eje: cada propuesta como un
  recorrido. Lámina con la donación a la izquierda, la flecha y la entidad a la derecha; Aprobar y
  Descartar al pie; las aprobadas y descartadas en dos columnas de renglones.
- [b](http://localhost:8091/admin-asignaciones?variante=propuestas-b). Eje: comparar. Pestañas por
  estado y una tabla con una fila por propuesta (tarjetas en el celular).
- Los estados de después de actuar se ven con `&accion=aprobada|descartada|corrida`: las pantallas
  `admin-asignaciones-aprobada` y demás redirigen sin la variante.

Archivos de la prueba: `templates/variantes/admin-entidades-a|b|c.html`, `propuestas-a|b.html`, lo
común en `templates/variantes/comun/admin-entidades-datos.html` y `propuestas-datos.html`, y
`static/css/variantes/admin-entidades-a|b|c.css`, `propuestas-a|b.css`. Copiados en
`target/classes`, igual que `admin-notificaciones*.html` (SitioController busca las pantallas en el
classpath). Sin clases nuevas: lo propio va por atributos `data-`. Verificado a 1280 y 375 px con un
Chrome sin ventana (no había pestañas libres en el panel), sin desbordes ni errores en la consola;
sin correr Maven.
