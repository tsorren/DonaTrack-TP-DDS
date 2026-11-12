# Flujo: el tablero del inicio de la empresa donante (R11-25)

> **Estado:** en revisión · **Fecha:** 2026-11-12 · **Aprobó:** pendiente (usuario)
> Ficha de la skill `ux-flujo` para el lote `p2-tablero` del
> [backlog de la revisión del 2026-11-11](../backlog-revision-2026-11-11.md) (P2, ruta STANDARD).
> La decisión del usuario de la sección 3 del backlog ("Tablero del inicio de la empresa":
> rediseño completo con el **boceto A** de la sección 8, tres columnas, como máximo 3 donaciones por
> columna y paginación en cada una) ya está tomada: esta ficha la baja a pantalla, no la reabre. B y
> C quedan descartadas.
> Amplía, sin reemplazarla, la [ficha del donante](raices-donante-navegable.md) (el inicio de la
> empresa, `donante-inicio`). Solo cambia el bloque "Donación por donación"; el resto del inicio
> (las cifras, el recorrido, las raíces y las noticias) queda como está.
> Usa el paginador del sitio en su forma corta ([ficha del paginador](r11-p1-paginador.md), §4.2:
> `componentes/paginas :: corta(p)`), que ya se pensó para estas columnas.

## 0. Qué pasa hoy (evidencia)

Leído en `e9331b3a2`. `[V]` es leído en el código; `[H]`, hipótesis sin medir.

| # | Hoy | Dónde |
|---|---|---|
| H1 | El adaptador del inicio pide **todas** las donaciones de la empresa (una página de 100, el tope de Donaciones) y la plantilla las reparte en las columnas con filtros de Thymeleaf. Ninguna columna tiene tope: con 48 entregadas, "Ya llegó" muestra 48 renglones `[V]` | `rest/DonanteInicioRest.java:73-75`, `rest/DonanteDonaciones.java:56-66`, `rest/DonacionesApi.java:50-51`, `donante-inicio.html:25-33,106` |
| H2 | Con más de 100 donaciones, las más viejas no aparecen en ninguna columna: es la deuda [DTI-39](../../adr/DEUDA_TECNICA.md#dti-39), ítem 1, que deja el inicio del donante para este lote `[V]` | `DEUDA_TECNICA.md` (DTI-39, ítem 1) |
| H3 | El orden de las columnas es **Ya llegó · En camino o por salir · Para tener en cuenta**; el boceto A elegido pone **Para tener en cuenta · En camino o por salir · Ya llegó** `[V]` | `donante-inicio.html:103,119,141`, backlog §8 |
| H4 | "En camino o por salir" junta dos listas: primero las que van en camino (`EN_TRASLADO`, `EN_PUERTA`) y después las asignadas que esperan el camión (`ASIGNACION_REALIZADA`, `LISTA_PARA_ENTREGAR`) `[V]` | `donante-inicio.html:28-29,122-137`, `rest/DonacionesRest.java:487-494` |
| H5 | "Para tener en cuenta" muestra las que tuvieron un problema (`ENTREGA_FALLIDA`, `VENCIDA`) y, al final, un renglón "2 esperan destino" que nombra a todas las que siguen en el depósito sin destino (`EN_DEPOSITO`) y lleva a Mis donaciones con el estado pendiente `[V]` | `donante-inicio.html:30-31,144-160` |
| H6 | Ninguna columna dice cuántas tiene; el boceto A lleva la cifra en la cabeza de cada una ("Ya llegó (48)") `[V]` | `donante-inicio.html:104,120,142` |
| H7 | La cifra "entregas en curso hoy" de arriba suma las tres listas de la plantilla (en camino, por salir y con problema): sale de la misma lista de 100 `[V]` | `donante-inicio.html:82` |
| H8 | Donaciones ya pagina y filtra en la base: `GET /donaciones-independientes` acepta `donanteId`, `estado` repetible, `page` y `size` (hasta 100), ordena por fecha de registro (más recientes primero) y devuelve el total en `X-Total-Count`. El adaptador del año de la persona ya cuenta así, con páginas de una `[V]` | `DonacionesIndependientesController.java:36-46`, `FiltroDonacionesIndependientes.java`, `DonacionesIndependientesService.java:63-76`, `rest/DonanteInicioRest.java:168-197` |
| H9 | El paginador del sitio ya tiene la forma corta para columnas angostas (`corta(p)`), pero `web.Paginadores.de` arma las direcciones siempre con `?pagina=`: tres paginadores en la misma pantalla se pisarían. `vistas.Paginador.de(nombre, pagina, paginas, direccion, cambio)` es público y recibe la función que arma cada dirección `[V]` | `componentes/paginas.html:17-22`, `web/Paginadores.java:20-30,38-45`, `vistas/Paginador.java:53-70` |
| H10 | Los enlaces del paginador llevan `hx-boost="true"`; las pantallas del admin mueven el foco al título de la lista con `tabindex="-1"` y `th:autofocus` cuando `p.cambio` `[V]` | `componentes/paginas.html:27-60`, `admin-asignaciones.html:75,130` |
| H11 | La maqueta tiene siete donaciones (dos entregadas, una en camino, una por salir, una con problema y dos esperan destino): con 3 por columna no se ve ningún paginador `[V]` | `maqueta/DonanteInicioMaqueta.java:27-41`, `maqueta/DonanteDonacionesDeEjemplo.java` |

## 1. Para quién y para qué

| | |
|---|---|
| Quién | La persona de responsabilidad social de la empresa donante (en el ejemplo, Distribuidora Ejemplo SA). Entra a su inicio desde la computadora de la oficina; a veces, desde el celular entre reuniones. Le rinde cuentas a su directorio |
| Tono | Panel del donante: cálido, agradecido, participativo. La columna "Para tener en cuenta": honesto, responsable, sereno ([tono según la pantalla](../tono-y-comunicacion.md#tono-según-la-pantalla)) |
| Qué quiere lograr | "De un vistazo, saber qué tengo que mirar, qué está saliendo y qué ya llegó, sin que la pantalla se vuelva una lista interminable" |
| Dispositivo principal | Los dos. En la computadora, las tres columnas lado a lado; en el celular, una debajo de la otra |
| Requerimientos | RF-05 (el donante consulta sus donaciones y su estado), RF-06 (seguimiento de la entrega), regla general 1 del backlog (nunca se trae la lista completa) y regla 3 (un solo paginador) |
| Datos que usa | `GET /donaciones-independientes?donanteId=…&estado=…&page=…&size=3` una vez por columna (la página y su total en `X-Total-Count`); `size=1` con `estado=EN_DEPOSITO` para contar las que esperan destino; el seguimiento de Logística que ya pide el inicio, para el texto "Sale del depósito el…" |
| Boceto de E1 | Sin boceto de E1 para el tablero (pág. 15 tenía anillos, ya reemplazados). Parte del **boceto A** de la sección 8 del backlog |

## 2. Dónde está en el sitio

`donante-inicio` (nivel 1, "Inicio" en el menú de la empresa), segundo bloque, debajo de "Lo que
ya dio fruto". Cada donación del tablero lleva a `donante-donaciones-detalle` (nivel 3, por Mis
donaciones). El renglón de las que esperan destino lleva a `donante-donaciones?estado=pendiente`
(nivel 2). Ver el [mapa del sitio](../mapa-del-sitio.md): no cambia ninguna ruta ni se agrega
ninguna pantalla.

La dirección del inicio suma tres parámetros opcionales, uno por columna: `tener`, `camino` y
`llego` (la página de cada una, desde 1). Ejemplo: `/donante-inicio?camino=2&llego=5`. Cada
paginador cambia solo el suyo y conserva los otros dos, así "Atrás" del navegador vuelve a lo que
se veía y la dirección se puede compartir o recargar.

## 3. Recorrido

```mermaid
flowchart TD
  entra([Entra a su inicio]) --> tablero[Tablero: 3 columnas, hasta 3 donaciones en cada una]
  tablero -->|toca una donación| detalle[donante-donaciones-detalle]
  tablero -->|toca › o ‹ en una columna| cambia[Esa columna muestra otras 3; las demás no se mueven]
  cambia --> tablero
  tablero -->|toca "esperan destino"| misdon[Mis donaciones, estado pendiente]
  tablero -->|toca "Mis donaciones" o "Ver seguimiento"| otras[Las pantallas de la cabecera]
  cambia -->|Donaciones no responde para esa columna| errcol[Esa columna dice que no se pudo cargar y ofrece volver a intentar]
  entra -->|dirección con una página que no existe| ultima[La columna muestra su última página]
  entra -->|ninguna donación| vacio[El vacío de todo el inicio, como hoy]
  entra -->|Donaciones no responde| errtodo[El error de todo el inicio, como hoy]
  detalle -->|Atrás| tablero
```

Desvíos considerados:

- **Volver:** "Atrás" en el detalle devuelve al inicio con las mismas páginas de cada columna (están
  en la dirección).
- **Sin JavaScript:** cada flecha es un enlace real; la pantalla se carga entera y baja sola hasta
  la columna que cambió (la dirección lleva el ancla de esa columna).
- **Página que no existe o mal escrita** (`?llego=99`, `?llego=abc`): la mal escrita es la 1; la que
  se pasa, la última que hay (como `vistas.Tramo`). No es un error.
- **Error del servidor en una columna:** solo esa columna lo dice; las otras dos se ven.
- **Error del servidor en todo el inicio, sin conexión o sin permiso:** como hoy (el error de
  `datos-donante`, las páginas de error y el pase a ingresar sin sesión).
- **Cambia algo mientras mira** (una donación pasa de "En camino" a "Ya llegó"): al pasar de página,
  la columna se lee de nuevo y puede correrse un lugar. No se avisa: el total de la cabeza se
  actualiza con la misma lectura.

## 4. Pantallas

### 4.1 El tablero en la computadora (desde unos 58 rem: tres columnas)

```text
Donación por donación
┌ Para tener en cuenta · 1 ─┐ ┌ En camino o por salir · 5 ─┐ ┌ Ya llegó · 48 ────────────┐
│ ▢ Útiles escolares,       │ │ ▢ Lentejas secas, 15 kg    │ │ ▢ Fideos secos, 8 kg      │
│   2 cajas                 │ │   Salió hoy a las 14:30,   │ │   Entrega confirmada el   │
│   No pudo llegar el 14 de │ │   hacia Comedor Barrio …   │ │   5 de octubre a Comedor …│
│   octubre.                │ │ ▢ Leche en polvo, 6 kg     │ │ ▢ Ropa de abrigo, 3 bolsas│
│                           │ │   Sale del depósito el 16  │ │   …                       │
│                           │ │   de octubre. Va a …       │ │ ▢ …                       │
│                           │ │ ▢ …                        │ │                           │
│ ─────────────────────────│ │                            │ │                           │
│ 2 donaciones esperan      │ │  Mostrando 1 a 3 de 5.     │ │  Mostrando 1 a 3 de 48.   │
│ destino en el depósito. › │ │        ‹  1 de 2  ›        │ │       ‹  1 de 16  ›       │
└───────────────────────────┘ └────────────────────────────┘ └───────────────────────────┘
```

- **Orden de las columnas, el del boceto A:** primero lo que pide atención, después lo que está en
  movimiento y al final lo que ya llegó. Sigue el recorrido de la mirada (arriba y a la izquierda
  lo más importante, criterios §3) y, en el celular, deja arriba lo urgente. Hoy el orden es el
  inverso (H3).
- **Cada columna es una tarjeta** (patrón *tarjetas*, criterios §5: un solo tema por tarjeta) con
  una **lista** de hasta 3 donaciones (patrón *listas*: tocar un ítem lleva al detalle) y su
  **paginado** (criterios §4: "paginado para recorrer mucho contenido").
- **La cabeza** es la pastilla de color del estado que ya tiene cada columna (`data-estado`), con el
  nombre y la cifra: "Ya llegó · 48". La cifra es el total de Donaciones, no lo que entra en la
  página.
- **Cada donación**, igual que hoy: el dibujo de su estado a la izquierda (planta según la
  categoría si llegó, brote si va en camino, maceta si espera el camión, el de reintentar si tuvo
  un problema), el nombre con la cantidad como enlace al detalle y, debajo, qué pasó.
- **El pie de cada columna**, abajo de todo y a la misma altura en las tres: "Mostrando 1 a 3 de 48."
  y el paginador corto `‹ 1 de 16 ›` (`componentes/paginas :: corta(p)`). Con una sola página no se
  dibuja ninguno de los dos (el componente no dibuja nada y la frase sobra: la cabeza ya dice 2 de 2).
- **"Para tener en cuenta"** suma, sobre su pie y separado por el filete, el renglón de las que
  esperan destino: es una cifra (pedida como número, `size=1`), no una lista, y no se pagina. Deja
  de nombrar a cada una ("Arroz largo fino y Zapatillas"): con 300 en el depósito, la frase sería
  interminable; los nombres están a un toque, en Mis donaciones.
- **Alto parejo y sin saltos:** las tres columnas miden lo mismo (el alto de la más alta) y el pie
  apoya abajo. Una columna con más de una página reserva el alto de 3 donaciones, así al llegar a la
  última página (con 1 o 2) el paginador no sube de lugar bajo el dedo.

### 4.2 El tablero en el celular (menos de unos 58 rem: una columna debajo de la otra)

```text
Donación por donación
┌ Para tener en cuenta · 1 ────────┐
│ ▢ Útiles escolares, 2 cajas      │
│   No pudo llegar el 14 de oct…   │
│ 2 donaciones esperan destino  ›  │
└──────────────────────────────────┘
┌ En camino o por salir · 5 ───────┐
│ ▢ Lentejas secas, 15 kg          │
│ ▢ Leche en polvo, 6 kg           │
│ ▢ …                              │
│ Mostrando 1 a 3 de 5.            │
│        ‹  1 de 2  ›              │
└──────────────────────────────────┘
┌ Ya llegó · 48 ───────────────────┐
│ …                                │
│       ‹  1 de 16  ›              │
└──────────────────────────────────┘
```

- La misma grilla que hoy (`.grilla` con `--medida-grilla`): las columnas bajan solas cuando no
  entran, sin un punto de corte propio. A 360 px, una por renglón; entre 600 y 900 px puede quedar
  dos arriba y una abajo, a todo el ancho (la grilla ya lo resuelve así en "Lo acumulado").
- Las flechas miden al menos 48 × 48 px (las del componente).
- Con columnas apiladas, la reserva de alto de 4.1 evita que lo de abajo salte al cambiar de página.

### 4.3 Qué pasa al tocar una flecha

- **Con JavaScript:** cambia **solo esa columna** (HTMX: el enlace del paginador, que ya tiene
  `hx-boost`, hereda de la columna `hx-target`, `hx-select` y `hx-swap` para reemplazarla a ella
  sola, sin subir la pantalla). La dirección del navegador se actualiza. El foco pasa al título de
  la columna, que dice el nombre, la cifra y la página ("Ya llegó, 48 donaciones, página 2 de 16").
- **Sin JavaScript:** la pantalla se carga entera con la página nueva de esa columna y baja hasta
  ella por el ancla (`#tablero-llego`, `#tablero-camino` o `#tablero-tener`).
- **Construcción** (para el constructor; los nombres son orientativos):
  - El puerto del inicio suma una lectura por columna: la página pedida de un grupo de estados, de a
    3, con su total; y la cifra de las que esperan destino. El adaptador REST la pide a Donaciones
    con `estado` repetible, `page`, `size=3` y `X-Total-Count`; nunca la lista entera. La maqueta la
    corta de su lista con `vistas.Tramo.de`.
  - Los grupos, en un solo lugar del adaptador: **Para tener en cuenta** = `ENTREGA_FALLIDA`,
    `VENCIDA`; **En camino o por salir** = `EN_TRASLADO`, `EN_PUERTA`, `ASIGNACION_REALIZADA`,
    `LISTA_PARA_ENTREGAR`; **Ya llegó** = `ENTREGADA`; **esperan destino** = `EN_DEPOSITO`. Son los
    mismos que hoy separa la plantilla (H4, H5).
  - El controller lee `tener`, `camino` y `llego` con `Tramo.pedida`, pide las tres columnas y arma
    cada paginador con `vistas.Paginador.de` y su propia función de dirección (cambia solo su
    parámetro, conserva los otros y lleva el ancla de la columna). `cambio` es verdadero solo si el
    pedido viene de HTMX y trae el parámetro de esa columna: con la carga entera, ningún título toma
    el foco (manda el ancla). No hace falta tocar `web.Paginadores`.
  - Cada columna lee sola: si una falla (`RestClientException`), esa columna queda en error y las
    otras siguen.
  - La plantilla deja de filtrar listas con `?[...]` para el tablero: recibe las tres columnas ya
    armadas.
  - Para ver los paginadores en la maqueta: `?escenario=muchas`, con más donaciones de ejemplo solo
    para el tablero (ver la sección 9).

## 5. Estados

| Pantalla | Estado | Qué se ve | Texto |
|---|---|---|---|
| Tablero | Con datos | Las tres columnas con hasta 3 donaciones, su cifra y, si hay más de una página, "Mostrando…" y el paginador | Ver la sección 6 |
| Tablero | Vacío (ninguna donación) | No hay tablero: el vacío de todo el inicio, como hoy | "Todavía no hay donaciones de tu empresa." (sin cambios) |
| Para tener en cuenta | Vacía, sin ninguna que espere destino | La tarjeta con su cabeza en 0 y una línea | "Nada para tener en cuenta por ahora." |
| Para tener en cuenta | Vacía, con algunas que esperan destino | La línea y, debajo, el renglón de las que esperan destino | "Ninguna donación con problema." · "2 donaciones esperan destino en el depósito." |
| En camino o por salir | Vacía | La tarjeta con su cabeza en 0 y una línea con la fecha | "Ninguna en camino ni por salir hoy, 15 de octubre." |
| Ya llegó | Vacía | La tarjeta con su cabeza en 0 y una línea | "Todavía no llegó ninguna. Cuando una organización confirme una entrega, la vas a ver acá." |
| Columna | Cargando (con JavaScript, al cambiar de página) | El indicador del sitio en el enlace tocado; la columna sigue a la vista hasta que llega la nueva | Sin texto nuevo (el de `hx-indicator` del sitio, si lo hay) |
| Tablero | Cargando (la pantalla entera) | Como hoy, `?escenario=lento` | "Cargando tu inicio…" (sin cambios) |
| Columna | Primera o última página | La flecha que no sigue, apagada y sin enlace | "‹ 1 de 16 ›" con ‹ apagado; "‹ 16 de 16 ›" con › apagado |
| Columna | Página que no existe en la dirección | La última que hay, sin aviso | — |
| Columna | Error del servidor en esa columna | La cabeza sin cifra y un aviso dentro de la tarjeta, con un enlace a la misma dirección | "No pudimos cargar esta columna." · "Volver a intentar" |
| Tablero | Error del servidor en todo el inicio | Como hoy, `?escenario=error` | "No pudimos cargar tu inicio." (sin cambios) |
| Tablero | Sin permiso o sin sesión | Como hoy: sin sesión, a ingresar; sin permiso, la página 403 con el marco de la cuenta | (sin cambios) |
| Columna | Éxito | No hay acción que confirmar: cambiar de página no guarda nada | — |

## 6. Textos

| Elemento | Texto | Nota |
|---|---|---|
| Título del bloque | Donación por donación | Sin cambios |
| Cabeza, primera columna | Para tener en cuenta · 1 | Lectura: "Para tener en cuenta, 1 donación" |
| Cabeza, segunda columna | En camino o por salir · 5 | Lectura: "En camino o por salir, 5 donaciones" |
| Cabeza, tercera columna | Ya llegó · 48 | Lectura: "Ya llegó, 48 donaciones". Con separador de miles: "Ya llegó · 1.284" |
| Lectura del título con más de una página | …, página 2 de 16 | Solo para lectores de pantalla, para que al tomar el foco se oiga dónde quedó |
| Cada donación: nombre | Fideos secos, 8 kg | Sin cambios (enlace al detalle) |
| Cada donación: qué pasó | "Entrega confirmada el 5 de octubre a Comedor Barrio Ejemplo." · "Salió el 15 de octubre, 14:30, hacia Comedor Barrio Ejemplo." · "Sale del depósito el 16 de octubre. Va a Merendero Ejemplo Sur." · "No pudo llegar el 14 de octubre." | Las notas de hoy (`DonacionesRest.nota` y el seguimiento), sin cambios: el rediseño no reescribe los estados |
| Esperan destino (1) | "1 donación espera destino en el depósito." | Enlace a Mis donaciones con el estado pendiente |
| Esperan destino (varias) | "2 donaciones esperan destino en el depósito." | Lo pendiente se dice pendiente. Deja de nombrar a cada una (sección 4.1) |
| Ayuda de esperan destino | "Se asignan cuando una organización pida algo así." | Sin cambios; va debajo, más chica |
| Qué parte se ve | "Mostrando 1 a 3 de 48." | Regla de texto 20 (una lista paginada dice qué parte se ve). Solo con más de una página |
| Paginador | ‹ 1 de 16 › | El del componente, forma corta. Nombre accesible del `nav`: "Páginas de Ya llegó" (y de cada columna) |
| Vacías | Ver la sección 5 | Sin "¡Todo en orden!" ni "¡Genial!": lo vacío no se celebra ni se lamenta |
| Error de una columna | "No pudimos cargar esta columna." · "Volver a intentar" | Qué pasó y el próximo paso, sin culpa ni urgencia |

Ayudas por flujo crítico: el tablero no es un flujo crítico (no se carga ni se confirma nada); la
ayuda es la de las que esperan destino, que ya estaba.

Filtro de tono (las 7 preguntas): las cifras de la cabeza son totales de Donaciones al momento de
la lectura, con la fecha de "Actualizado hoy, …" que ya dice la bajada del inicio; "Mostrando 1 a 3
de 48" dice qué parte se ve; "Ya llegó" se usa solo para lo que la organización confirmó (estado
`ENTREGADA`), nunca para lo que está en la puerta; "Para tener en cuenta" no dice "urgente" ni
"atrasada": cada donación dice qué pasó.

## 7. Accesibilidad

- **Zonas:** el tablero es una `section` con su `h2`; cada columna, una `section` con su `h3` como
  nombre (`aria-labelledby`), una lista `ul` y su `nav` del paginador con nombre propio ("Páginas de
  Ya llegó"). Tres `nav` en la pantalla se distinguen por ese nombre.
- **Orden del foco:** columna por columna, en el orden visual (la primera es "Para tener en cuenta"):
  las donaciones, el renglón de las que esperan destino (en la primera) y las flechas.
- **Al cambiar de página:** el foco va al `h3` de la columna (`tabindex="-1"`), que se oye con su
  cifra y la página. No hace falta un aviso `aria-live`: el foco ya cuenta lo que cambió.
- **Flechas apagadas:** `span` sin enlace, no toman el foco ni se leen (las del componente).
- **Color:** la pastilla de cada columna tiene texto; el color del estado acompaña, no reemplaza.
- **Contraste y toque:** los colores son los tokens de estado que ya pasan AA; las flechas, 48 ×
  48 px.
- **Sin mapa** en este bloque.

## 8. Revisión de usabilidad

| # | Heurística | Cómo la cumple | Riesgo que queda |
|---|---|---|---|
| 1 | Visibilidad del estado | Cada columna dice cuántas tiene y qué parte se ve ("Mostrando 1 a 3 de 48", "1 de 16"). Lo que pide atención va primero | La cifra es la de la lectura: si algo cambia mientras mira, se ve al pasar de página o al recargar |
| 2 | Lenguaje del usuario | "Para tener en cuenta", "En camino o por salir", "Ya llegó": las palabras de hoy, sin estados internos | "Por salir" junta dos estados del servicio (asignada y lista para entregar); la nota de cada una aclara cuál |
| 3 | Control y libertad | Cada columna se recorre sola; "Atrás" vuelve a las páginas de antes (están en la dirección) | — |
| 4 | Consistencia | El mismo paginador del sitio en su forma corta, las mismas pastillas de color y los mismos dibujos por estado que Mis donaciones y Seguimiento | Mis donaciones usa la forma larga en la computadora; acá, la corta siempre (decidido en la ficha del paginador) |
| 5 | Prevención de errores | Una página que no existe muestra la última, sin error; las flechas apagadas no son enlaces | — |
| 6 | Reconocer antes que recordar | Cada donación con su dibujo, su nombre y qué pasó; la cifra de cada columna está a la vista | — |
| 7 | Flexibilidad | Quien quiere todo va a "Mis donaciones" (cabecera) con filtros; el tablero es el vistazo | No hay "Ver todas" por columna: la cabecera ya lleva a Mis donaciones (sección 9) |
| 8 | Diseño minimalista | 3 por columna y un pie chico; la columna con una sola página no muestra pie | — |
| 9 | Recuperarse de errores | Si una columna no carga, lo dice y ofrece volver a intentar; las otras siguen | — |
| 10 | Ayuda | La ayuda de las que esperan destino se mantiene; los estados se explican en cada nota | — |

Los 7 factores (Morville):

- **Útil:** responde qué mirar, qué está saliendo y qué llegó, con cifras reales y sin lista entera.
- **Usable:** tres listas cortas que se recorren con dos flechas; el detalle, a un toque.
- **Deseable:** se conservan los dibujos de cada estado y los colores de las columnas.
- **Valioso:** la empresa ve lo acumulado ("Ya llegó · 48") para rendir cuentas, sin que la
  pantalla crezca con el tiempo.
- **Ubicable:** las tres columnas quedan siempre en el mismo lugar del inicio; la dirección guarda
  la página.
- **Accesible:** títulos con nombre, foco que acompaña el cambio y flechas de 48 px.
- **Creíble:** las cifras son totales del servicio y "Ya llegó" es solo lo confirmado.

Para probar con personas reales: si "Para tener en cuenta" se entiende como algo para hacer (no
hay ninguna acción que el donante pueda tomar, solo enterarse) y si, en el celular, la gente nota
que cada columna tiene su propio paginador.

## 9. Decisiones y preguntas

Decisiones tomadas (agente, 2026-11-12, sin consultar; van a la [bitácora](../bitacora.md) al
integrar el lote):

1. **El orden de las columnas es el del boceto A** (Para tener en cuenta · En camino o por salir ·
   Ya llegó). Descartado: dejar el orden de hoy. El boceto elegido ya lo fija y pone arriba lo que
   pide atención, también en el celular.
2. **La página de cada columna va en su propio parámetro de la dirección** (`tener`, `camino`,
   `llego`). Descartados: guardar la página en la sesión (rompe "Atrás" y el enlace compartido) y
   un único `?pagina=` (tres paginadores se pisarían).
3. **Con JavaScript cambia solo la columna tocada**, heredando `hx-target` y `hx-select` de la
   columna, sin tocar el componente `paginas`. Descartado: reemplazar todo el `main` con
   `hx-boost` (subiría la pantalla y movería las otras dos columnas).
4. **El foco va al título de la columna solo en el pedido de HTMX;** en la carga entera, manda el
   ancla. Descartado: `autofocus` siempre (con tres parámetros en la dirección, al recargar tomaría
   el foco la primera columna y la pantalla saltaría).
5. **Las que esperan destino quedan como cifra, sin nombres, en la primera columna.** Descartados:
   paginarlas como una cuarta lista (el boceto tiene tres columnas) y seguir nombrándolas (obliga a
   traer la lista entera).
6. **El orden dentro de cada columna es el de Donaciones: las más recientes primero por fecha de
   registro.** Descartado: ordenar por fecha de entrega o poner las "en camino" antes que las "por
   salir" (Donaciones no ordena por esos campos; ordenar en el cliente obliga a traer todas). La
   nota de cada donación dice su fecha.
7. **Sin "Ver todas" por columna.** Descartado: un enlace en cada pie (la regla 2 lo pide para los
   resúmenes, pero el usuario eligió paginar cada columna; la cabecera del inicio ya lleva a "Mis
   donaciones" y tres enlaces más recargan el pie).
8. **Una columna que falla no tumba el inicio.** Descartado: el error de toda la pantalla por una
   columna (las otras dos tienen datos).
9. **La maqueta suma `?escenario=muchas`** para ver los paginadores con los totales del boceto
   (1, 5 y 48), con donaciones de ejemplo solo en el tablero. Descartado: sumar donaciones a
   `DonanteDonacionesDeEjemplo` (la comparten Mis donaciones, Seguimiento y el detalle, y cambiaría
   las cifras y el hito del inicio, que dicen "desde el 1 de octubre").

Preguntas abiertas para el usuario (con la opción recomendada):

1. **La cifra "entregas en curso hoy" de arriba**, ¿pasa a salir de los totales de las columnas
   (En camino o por salir + Para tener en cuenta)? → **Sí (recomendada):** las cifras de arriba y
   las del tablero dicen lo mismo y no dependen de la lista de 100. · No: queda como está, de la
   lista.
2. **El resto del inicio** (las cifras del mes, el hito, el recorrido, las raíces y las noticias)
   sigue leyendo la lista de hasta 100 donaciones. ¿Se deja anotado en la deuda para un lote
   propio? → **Sí, a la deuda (recomendada):** cambiarlo toca el componente de la huerta y las
   noticias, fuera de este lote. · No: se hace en este lote.
3. **La pastilla de la cabeza con la cifra** ("Ya llegó · 48"), ¿o la cifra entre paréntesis, como
   el boceto ("Ya llegó (48)")? → **Con el punto medio (recomendada):** dentro de la pastilla de color se lee más limpio que con
   paréntesis. · Entre paréntesis, como el boceto.
