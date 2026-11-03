# Flujo: Donar y seguir, el recorrido del donante en Raíces

> **Estado:** pendiente de aprobación del usuario: se aprueba al elegir A o B (corrida A/B del
> 2026-11-02) · **Fecha:** 2026-11-02 · **Aprobó:** —
> Plantilla de la skill `ux-flujo`. Ficha compartida por los dos sitios de la corrida A/B de
> Raíces: A en `/prototipo/raices/<pantalla>` y B en `/prototipo/raices-b/<pantalla>`
> ([ADR propuesto](../../adr/cliente-liviano/20261101-tercera-alternativa-raices-como-prototipo-propio.md)).
> Fija para quién es, el recorrido, los estados, los textos, los datos y las salidas. **No fija la
> composición ni la navegación visual:** eso lo decide cada sitio.
> Pantallas nuevas: `donante-inicio`, `donante-donaciones`, `donante-donaciones-detalle`,
> `donante-entidades` y `donante-notificaciones`. Ya aprobadas y enlazadas:
> [`donante-seguimiento`](raices-seguir-entregas.md) y [`donante-perfil`](raices-mi-perfil.md)
> (con `-guardado`).

## 1. Para quién y para qué

| | |
|---|---|
| Quién | La persona de responsabilidad social de una empresa que dona (en el ejemplo, Distribuidora Ejemplo SA). Trabaja en la computadora de la oficina; a veces mira desde el celular, entre reuniones. Le rinde cuentas a su directorio: necesita saber qué donó la empresa, a quién llegó y qué lo prueba |
| Tono | Panel, avisos y seguimiento del donante: cálido, agradecido, participativo. La entrega con problema: honesto, responsable, sereno. Entidades: respetuoso, en contexto, digno ([tono según la pantalla](../tono-y-comunicacion.md#tono-según-la-pantalla)) |
| Qué quiere lograr | "Ver de un vistazo qué donó mi empresa, en qué estado está cada donación, qué está confirmado y a quién le llegó" |
| Dispositivo principal | Ambos; la computadora primero |
| Requerimientos | RF-05 (sus donaciones, con filtro por estado y categoría). RF-06 (entidades activas, paginadas, con búsqueda por nombre). RF-07 (sin Incentivos: la cuenta es una empresa). RF-08 (notificaciones de asignación y recepción; las de misión y ascenso no aplican a una empresa). RF-09 (seguimiento, ya aprobado). RF-CL-07 (lo que cambia dice cuándo se actualizó). RF-CL-08 (errores con código para soporte) |
| Datos que usa | `GET /donaciones-independientes?estado&subcategoriaId&donanteId` (RF-05), `GET /api/entidades` y `GET /api/necesidades` (RF-06), `GET /api/notificaciones/persona/{personaId}` y `PATCH .../marcar-todas-leidas` (RF-08), `GET /api/entregas/seguimiento/donaciones/{id}` (detalle). Huecos: HB-03, HB-08, HB-09, HB-15, HB-16, HB-18, HB-20 y los de la [lista de la corrida](#huecos-de-backend). En la maqueta, las siete donaciones de `prototipo/cozy-medieval/viaje-datos.html` (las mismas de Seguimiento), los estados de `EstilosController.datosDeEjemplo` (`estados`, `pasos`) y `?escenario=` (los valores de `Escenario`) |
| Boceto de E1 | Págs. 15 (inicio), 16 (mis donaciones), 17 (beneficiarios) y 6 (notificaciones) |

## 2. Dónde está en el sitio

```text
publico-ingresar ──(cuenta de ejemplo de la empresa)──► donante-inicio (nivel 1)
donante-inicio
 ├─ Mis donaciones ─► donante-donaciones (2) ─► donante-donaciones-detalle?donacion=<clave> (3)
 ├─ Entidades ──────► donante-entidades (2)
 ├─ Seguimiento ────► donante-seguimiento (2, aprobada) ─► donante-donaciones-detalle (3)
 ├─ (acceso fijo) ──► donante-notificaciones (2) ─► donante-donaciones-detalle (3)
 └─ (cuenta) ───────► donante-perfil (2, aprobada)
```

- El menú del donante es el de `PrototipoController.deRaices`: Inicio, Mis donaciones, Entidades y
  Seguimiento. Incentivos no está (RF-07).
- Notificaciones no es una sección del menú (como en Cozy, donde va en la carta). Se llega desde el
  inicio ("Ver todas las notificaciones") y desde un acceso fijo en las pantallas del donante,
  con la cantidad sin leer. Dónde va ese acceso lo decide cada sitio. A lo suma a `layout-privado`
  como un agregado, salvo en las dos pantallas aprobadas (Seguimiento y Mi perfil), que A no toca
  (encargo, sección 1): ahí se llega a Notificaciones por el menú (Inicio). B, que puede rehacer
  las aprobadas, lo lleva en todas.
- Mi perfil y Salir siguen en la cuenta, como hoy. Salir lleva a `publico-portada`.
- Nada queda a más de 3 niveles (RNF-UI-02).

## 3. Recorrido

```mermaid
flowchart TD
  ingresar([publico-ingresar]) --> inicio[donante-inicio]
  registro([publico-registro-donante]) --> inicio
  inicio -->|Ver Mis donaciones| donaciones[donante-donaciones]
  inicio -->|la entrega con problema| detalle[donante-donaciones-detalle]
  inicio -->|Ver seguimiento| seguimiento[donante-seguimiento]
  inicio -->|Conocer las entidades| entidades[donante-entidades]
  inicio -->|Ver todas las notificaciones| notificaciones[donante-notificaciones]
  donaciones -->|filtra| donaciones
  donaciones -->|una donación| detalle
  donaciones -->|Ver seguimiento| seguimiento
  detalle -->|en curso: Ver dónde está| seguimiento
  detalle -->|Volver a Mis donaciones| donaciones
  seguimiento -->|una entrega| detalle
  notificaciones -->|un aviso| detalle
  notificaciones -->|Marcar todas como leídas| leidas[donante-notificaciones-leidas]
  entidades -->|busca por nombre| entidades
  entidades -->|Ver Mis donaciones| donaciones
```

Camino feliz ("donar y seguir"): la empresa ingresa → ve en el inicio lo confirmado y lo que está en
curso → abre Mis donaciones → elige una → ve sus etapas, sus noticias y, si llegó, la foto → si está
en curso, pasa a Seguimiento para ver dónde va → vuelve. Por el costado: Entidades (a quién llegan
las donaciones y qué piden) y Notificaciones (qué pasó desde la última vez).

Desvíos considerados:

- **Volver:** el menú, el enlace "Volver a Mis donaciones" del detalle y el botón Atrás del
  navegador. Ninguna pantalla deja algo a medio hacer.
- **Filtro sin resultados** (Mis donaciones) y **búsqueda sin resultados** (Entidades): un aviso
  que dice qué pasó y "Sacar los filtros" o "Ver todas".
- **Donación que no existe** (`?donacion=` desconocida): "No encontramos esa donación" y la vuelta a
  Mis donaciones.
- **Error del servidor o sin conexión:** el banner de error, con el código para soporte y "Volver a
  intentar" (`?escenario=error`).
- **Tarda:** cada parte con su lugar reservado y "Cargando…" (`?escenario=lento`).
- **Cuenta nueva, sin donaciones:** el estado vacío explica cómo empieza (`?escenario=vacio`).
- **Entrega con problema:** se muestra como una más, en su lugar, con qué pasó y la nueva salida; el
  inicio la nombra aparte, sin alarma.
- **Sin permiso:** en el sistema, la sesión del donante solo trae sus donaciones; quien no inició
  sesión va a Ingresar; una entidad o un admin que abre estas direcciones ve "Esta pantalla es para
  quienes donan." y "Ir a tu inicio". En la maqueta no se dibuja: cada rol tiene su dirección.

## 4. Pantallas

Los esquemas dicen **qué hay** en cada pantalla, no dónde va: la composición, el orden visual, el
dibujo y la navegación son de cada sitio. Regla de Raíces para los dos: como mucho una imagen de
raíz o siembra por pantalla, en el título grande; nunca en botones, datos ni errores.

### donante-inicio (nivel 1)

```text
[Inicio]  Hola, Distribuidora Ejemplo SA
          bajada
[Lo que está confirmado]  2 entregas confirmadas · 2 entidades · 7 donaciones registradas
                          período y fuente · primera entrega confirmada
[En curso]                3 entregas en curso (1 en camino, 1 con problema, 1 pendiente) → Seguimiento
                          2 esperan destino → Mis donaciones
[La que se demoró]        Útiles escolares: qué pasó y la nueva salida → detalle
[Últimas noticias]        las 3 más nuevas → Ver todas las notificaciones (4 sin leer)
[Accesos]                 Ver Mis donaciones · Conocer las entidades
```

- **Lo que está confirmado** usa el mismo nombre que las cifras de la portada. Separa resultado
  (entregas confirmadas, entidades que confirmaron) de actividad (donaciones registradas), como pide
  [cifras y datos](../tono-y-comunicacion.md#cifras-y-datos).
- **El juego se insinúa** (brief de Raíces): para la empresa, un hito de lo acumulado, dibujado con
  los datos y sin medallas. El dato: la primera entrega confirmada y cuántas van. La forma es de cada
  sitio.
- Qué cambia respecto del boceto (pág. 15): los tres anillos (meta mensual, ayuda respecto del mes
  anterior, necesidades satisfechas) se reemplazan por cifras con período y fuente. Una "meta
  mensual" no está en los requerimientos y "necesidades satisfechas" afirma un efecto que la
  plataforma no prueba. Las notificaciones pendientes del boceto quedan como "Últimas noticias".

### donante-donaciones (nivel 2)

```text
[Mis donaciones]  Lo que donó tu empresa
                  7 donaciones, de la más reciente a la primera. Actualizado …
[Filtro]          Estado [Todos ▾]  Categoría [Todas ▾]  [Filtrar]  (Sacar los filtros)
[Lista]           por donación: N.º · nombre (enlace al detalle) · categoría con ícono · cantidad
                  · para quién (o "Todavía sin destino") · donada el … · sello de estado + nota
[Pie]             Las que están en curso también se ven en Seguimiento. → Ver seguimiento
```

- El filtro es un formulario `get` real: estado (`pendiente`, `en-camino`, `entregada`,
  `con-problema`, los `tono` de `EstadoEntrega`) y categoría (`alimentos`, `ropa`, `escolares`,
  los valores de la ficha pública). Combinar los dos devuelve solo las que cumplen ambos (RF-05). Un
  valor desconocido no da error: se ignora y se muestran todas, con ese filtro en "Todos" o "Todas"
  (el mismo criterio de Donaciones recibidas de la entidad).
- El orden es por fecha, la más reciente primero (el N.º sigue ese orden, del 7 al 1). Agrupar por
  estado o no es decisión de cada sitio.
- Qué cambia respecto del boceto (pág. 16): la tabla de 5 columnas pasa a una lista de donaciones,
  porque en 360 px no entra (bocetos de E1); la columna "Detalle" pasa a ser el nombre como enlace.

### donante-donaciones-detalle (nivel 3)

`?donacion=<clave>`, con las claves de `viaje-datos`: `arroz`, `leche`, `lentejas`, `utiles`,
`zapatillas`, `ropa` y `fideos`. Sin parámetro, `lentejas` (la que va en camino), como en Cozy. El
menú marca Mis donaciones.

```text
(Volver a Mis donaciones)
[Mis donaciones]  <nombre de la donación>
                  <cantidad> para <entidad>  |  <cantidad>, todavía sin destino
                  sello de estado + nota
[Aviso]           solo con problema: qué pasó, la última noticia y la nueva salida
[Etapas]          En depósito · Asignada · Lista para entregar · En camino · Entregada, con su fecha
[La foto]         entregada: la foto de la entidad con su pie | si no: qué falta y quién la sube
[Datos]           N.º · Qué · Cuánto · Categoría · Para · Donada · Entregada (si llegó) · Figura como
[Noticias]        las de esta donación, de la más nueva a la primera
[Acción]          en curso: Ver dónde está → donante-seguimiento
```

- "Figura como" dice qué nombre ve la entidad (HB-20) y enlaza a Mi perfil, donde se cambia
  ([ficha de Mi perfil](raices-mi-perfil.md)).
- Qué cambia respecto del boceto: E1 no dibujó el detalle (pág. 16 solo tiene la columna). Toma de
  Cozy los mismos datos y suma la foto como prueba de lo confirmado, el foco de Raíces.

### donante-entidades (nivel 2)

```text
[Entidades]   Quiénes reciben las donaciones
              Entidades activas y lo que piden hoy. …
[Búsqueda]    Buscar por nombre [__________] [Buscar]  (Ver todas)
[Lista]       por entidad: nombre · tipo y zona · lo que tu empresa ya donó ahí (sello + texto)
              · lo que pide: cada necesidad con cuánto recibió y cuánto falta
              · si no pide nada: "Hoy no pide nada."
[Paginado]    Página 1 de 8 · Página siguiente
[Cómo donar]  la llevás al depósito, la registran y la asignan · dirección y horario del depósito
              → Ver Mis donaciones
```

- Seis entidades por página (RF-06 pide paginado). La maqueta dibuja la página 1; `?pagina=2` en
  adelante muestra el aviso "En la maqueta solo está dibujada la primera página." con "Volver a la
  página 1", para no inventar 40 entidades.
- No hay detalle de la entidad en esta corrida: cada entrada dice todo lo que hay. El nombre no se
  enlaza a `donante-entidades-detalle` (caería en "sin dibujar" dentro del recorrido).
- Sin botón "Donar" por entidad o por necesidad: la entidad final la elige el admin con los
  algoritmos (RF-17) y la donación la registra el depósito (RF-15). Ver
  [preguntas abiertas](#9-decisiones-y-preguntas).
- Qué cambia respecto del boceto (pág. 17): el boceto muestra tarjetas por donación con sus
  entidades; eso ya lo dicen Mis donaciones y el detalle. Esta pantalla responde RF-06 (navegar
  entidades) y, de cada una, suma lo que la empresa ya donó ahí.

### donante-notificaciones (nivel 2)

```text
[Notificaciones]  Noticias de tus donaciones
                  4 sin leer. …                        [Marcar todas como leídas]
[Sin leer (4)]    por aviso: "Sin leer" · sello del estado de su donación · texto (enlace al
                  detalle) · fecha y hora · nombre y cantidad de la donación
[Leídas (13)]     lo mismo, con "Leída"
```

- De la más nueva a la más vieja. "Sin leer" y "Leída" se distinguen por el texto y por la forma,
  no solo por el color.
- "Marcar todas como leídas" es un formulario `post`. En la maqueta va a
  `donante-notificaciones-leidas` (la misma lista, todas leídas, con el aviso), como Mi perfil va a
  `donante-perfil-guardado`.
- Qué cambia respecto del boceto (pág. 6): la tabla Estado / Detalle / Fecha pasa a una lista
  (360 px); el filtro del boceto se reemplaza por los dos grupos, sin leer y leídas, que el contrato
  ya separa (`leida`).

## 5. Estados

En la maqueta, `?escenario=vacio`, `error` y `lento` en las cinco pantallas, como la portada y
Seguimiento de Raíces. El código para soporte es `a1b2c3d4e5f6` y su enlace va a
`publico-contacto?codigo=a1b2c3d4e5f6&volver=<pantalla>` ([ficha de Contacto](raices-contacto.md)).

| Pantalla | Estado | Qué se ve | Texto |
|---|---|---|---|
| Inicio | Con datos | Las cuatro partes del esquema | Ver Textos |
| Inicio | Vacío (cuenta nueva) | El saludo y un aviso en lugar de cifras, curso y noticias | "Todavía no hay donaciones de tu empresa." · "Cuando lleves tu primera donación al depósito y la registren, la vas a ver acá, con cada paso hasta que la entidad confirme la entrega." · "Conocer las entidades" |
| Inicio | Cargando | El saludo; cada parte con su lugar reservado, sin datos inventados | "Cargando tu inicio…" |
| Inicio | Error del servidor | El saludo y el banner de error | "No pudimos cargar tu inicio." · "Es un problema nuestro y no cambia el estado de tus donaciones. Probá de nuevo en unos minutos; si sigue, escribinos con este código." · `a1b2c3d4e5f6` · "Volver a intentar" |
| Inicio | Éxito / error de datos / deshabilitado | No aplica: no tiene acciones que cambien algo | — |
| Mis donaciones | Con datos | El filtro y las 7 donaciones | Ver Textos |
| Mis donaciones | Filtrado | El filtro con lo elegido, la cuenta y solo las que cumplen | "2 donaciones con esos filtros." · "Sacar los filtros" |
| Mis donaciones | Vacío con filtros | El filtro y el aviso | "No hay donaciones con esos filtros." · "Probá con otro estado o con otra categoría." · "Sacar los filtros" |
| Mis donaciones | Vacío (cuenta nueva) | El título y el aviso, sin filtro | "Tu empresa todavía no tiene donaciones registradas." · "Las registra el equipo del depósito cuando las recibe. Desde ese momento ves cada una acá, con su estado." · "Conocer las entidades" |
| Mis donaciones | Cargando | El título y el filtro; la lista con su lugar reservado | "Cargando tus donaciones…" |
| Mis donaciones | Error del servidor | El título y el banner | "No pudimos cargar tus donaciones." · el mismo cuerpo, código y "Volver a intentar" del inicio |
| Detalle | En depósito, sin destino (`arroz`, `zapatillas`) | Etapas con la primera actual; en el lugar de la foto, qué falta | "Todavía sin destino: espera una necesidad que le corresponda. Cuando se le asigne una entidad, vas a ver acá a cuál y cuándo sale." |
| Detalle | Asignada, espera el camión (`leche`) | Etapas; qué falta; "Ver dónde está" | "Falta confirmar la entrega: la confirma Merendero Ejemplo Sur con la foto." |
| Detalle | En camino (`lentejas`) | Etapas con "En camino" actual; qué falta; "Ver dónde está" | "Falta confirmar la entrega: la confirma Comedor Barrio Ejemplo con la foto." |
| Detalle | Con problema (`utiles`) | El aviso arriba; etapas; "Ver dónde está" | Aviso: "No pudo llegar a Escuela Ejemplo N.º 12." · "El camino estaba cortado. Sale de nuevo el 17 de octubre. Última noticia: 11 de octubre, 16:10." |
| Detalle | Entregada (`ropa`, `fideos`) | Etapas completas; la foto con su pie; sin "Ver dónde está" | "Merendero Ejemplo Sur confirmó la entrega el 9 de octubre y subió esta foto." (el mismo patrón de la portada) |
| Detalle | No encontrada | El aviso, sin datos | "No encontramos esa donación." · "Puede que el enlace esté incompleto. Buscala en Mis donaciones." · "Volver a Mis donaciones" |
| Detalle | Cargando | El nombre si se sabe; etapas, foto y datos con su lugar | "Cargando la donación…" |
| Detalle | Error del servidor | "Volver a Mis donaciones" y el banner | "No pudimos cargar esta donación." · cuerpo, código y "Volver a intentar" |
| Entidades | Con datos | La búsqueda, 6 entidades, el paginado y "Cómo donar" | Ver Textos |
| Entidades | Búsqueda sin resultados | La búsqueda con lo escrito y el aviso | "Ninguna entidad activa se llama así." · "Probá con otra parte del nombre." · "Ver todas" |
| Entidades | Página no dibujada (`?pagina=2` o más) | El paginado y el aviso | "En la maqueta solo está dibujada la primera página." · "Volver a la página 1" |
| Entidades | Vacío | La búsqueda no; el aviso y "Cómo donar" | "Por ahora no hay entidades activas." · "Cuando una entidad se sume y la aprueben, aparece acá con lo que pide." |
| Entidades | Cargando | El título y la búsqueda; la lista con su lugar | "Cargando las entidades…" |
| Entidades | Error del servidor | El título y el banner; "Cómo donar" sigue (es texto fijo) | "No pudimos cargar las entidades." · cuerpo, código y "Volver a intentar" |
| Notificaciones | Con datos | Los dos grupos y "Marcar todas como leídas" | Ver Textos |
| Notificaciones | Éxito (`donante-notificaciones-leidas`) | El aviso arriba, que toma el foco; un solo grupo, todas leídas; sin el botón | "Listo: marcaste todas como leídas." (`role="status"`) · bajada: "No tenés notificaciones sin leer." |
| Notificaciones | Vacío | El título y el aviso | "Todavía no tenés notificaciones." · "Te avisamos cuando se asigne una donación a una entidad, cuando salga el camión y cuando la entidad confirme la entrega." · "Ver Mis donaciones" |
| Notificaciones | Cargando | El título; los grupos con su lugar | "Cargando tus notificaciones…" |
| Notificaciones | Error del servidor | El título y el banner | "No pudimos cargar tus notificaciones." · cuerpo, código y "Volver a intentar" |
| Notificaciones | Error al marcar | El banner arriba, la lista como estaba | "No pudimos marcarlas como leídas. Siguen como estaban. Probá de nuevo." (en el sistema; en la maqueta no se dibuja) |
| Las cinco | Sin permiso | No se dibuja en la maqueta (ver Desvíos) | "Esta pantalla es para quienes donan." · "Ir a tu inicio" |

En el sistema, "Cargando…" y los errores se anuncian en la región `#avisos`; en la maqueta llegan
con la página y no son región viva (como en Seguimiento).

## 6. Textos

"Hoy" es el jueves 15 de octubre de 2026 y la última actualización, las 16:25 (los mismos de
Seguimiento). Toda la maqueta de Raíces es ese jueves, y cada rol lo mira a la hora de sus pantallas
ya aprobadas: lo público a las 14:30, el donante a las 16:25 y la entidad a las 18:30. Por eso las
lentejas de 15 kg van "en camino" acá (salieron a las 14:30) y en la ficha de la entidad ya están en
su puerta (llegaron a las 18:20): es la misma entrega, más tarde. Los nombres, cantidades, notas,
etapas y noticias de las donaciones salen de `viaje-datos`, la única fuente de lo que donó la
empresa (la ficha de la entidad toma de ahí lo que le llega de Distribuidora Ejemplo SA); acá van
los propios de cada pantalla. Dos textos de `viaje-datos` se muestran distinto, porque el archivo
lo comparte Cozy y no se toca: la categoría `cat-vestimenta` se muestra "Ropa" (como en las fichas
pública y de la entidad) y el destino "Escuela Ejemplo 12" se muestra "Escuela Ejemplo N.º 12"
(como en la portada). Las plantillas nuevas del donante lo escriben así.

### donante-inicio

| Elemento | Texto | Nota |
|---|---|---|
| Rótulo | Inicio | El nombre del menú |
| Título | Hola, Distribuidora Ejemplo SA | El saludo del boceto. Si lleva la imagen de raíz, es acá |
| Bajada | "Gracias por donar. Acá ves qué donó tu empresa, dónde está cada donación y qué ya confirmaron las entidades." | Reconoce el aporte sin adornarlo |
| Cifras: título | Lo que está confirmado | El nombre de las cifras de la portada |
| Cifras | "2 entregas confirmadas" · "2 entidades confirmaron lo que recibieron" · "7 donaciones registradas" | Fideos (Comedor Barrio Ejemplo) y ropa (Merendero Ejemplo Sur). La tercera es actividad, no resultado: va aparte o más chica |
| Cifras: alcance | "Desde el 1 de octubre de 2026, según las confirmaciones de las entidades. Actualizado hoy, 15 de octubre, 16:25." | Período, fuente y fecha |
| Hito | "Primera entrega confirmada: fideos secos, el 5 de octubre, en Comedor Barrio Ejemplo." | Lo acumulado, sin medalla ni "¡felicitaciones!" |
| En curso: título | En curso | — |
| En curso | "3 entregas en curso: 1 en camino, 1 con problema y 1 pendiente." · "Ver seguimiento" | Los mismos números que Seguimiento |
| Esperan destino | "2 donaciones esperan destino en el depósito." · "Ver Mis donaciones" | Arroz y zapatillas; lo pendiente se dice pendiente |
| La que se demoró | "Útiles escolares no pudo llegar a Escuela Ejemplo N.º 12: el camino estaba cortado. Sale de nuevo el 17 de octubre." · "Ver el detalle" (`?donacion=utiles`) | Qué pasó y el próximo paso, sin "lamentablemente". Nombre accesible del enlace: "Ver el detalle de Útiles escolares" |
| Noticias: título | Últimas noticias | — |
| Noticias | Las tres más nuevas de la lista de Notificaciones, con su fecha | "Hoy, 14:30", "Ayer, 17:00", "Ayer, 12:15", cada una con `datetime` |
| Noticias: enlace | "Ver todas las notificaciones (4 sin leer)" | — |
| Accesos | "Ver Mis donaciones" · "Conocer las entidades" | "Ver seguimiento" ya está en En curso |

### donante-donaciones

| Elemento | Texto | Nota |
|---|---|---|
| Rótulo | Mis donaciones | — |
| Título | Lo que donó tu empresa | — |
| Bajada | "7 donaciones, de la más reciente a la primera. Actualizado hoy, 15 de octubre, 16:25." | Con filtro: "2 donaciones con esos filtros." |
| Filtro | "Estado" (Todos · Pendiente · En camino · Entregada · Con problema) · "Categoría" (Todas · Alimentos · Ropa · Útiles escolares) · "Filtrar" · "Sacar los filtros" | Nombre accesible del formulario: "Filtrar donaciones". Los estados, con el texto de `EstadoEntrega` |
| Número | "N.º 7" | Para el lector: "Donación número 7" |
| Para quién | "Para Comedor Barrio Ejemplo" · "Todavía sin destino" | — |
| Fecha | "Donada el 12 de octubre de 2026" | — |
| Estado y nota | El sello y la nota de `viaje-datos`; en las entregadas, "Entrega confirmada el 5 de octubre, con foto de la entidad" en lugar de "Llegó el 5 de octubre" | Lo confirmado dice qué lo respalda |
| Pie | "Las que están en curso también se ven en Seguimiento, con dónde va el camión." · "Ver seguimiento" | — |

### donante-donaciones-detalle

| Elemento | Texto | Nota |
|---|---|---|
| Volver | Volver a Mis donaciones | — |
| Rótulo | Mis donaciones | La sección del menú |
| Título | El nombre de la donación ("Lentejas secas") | — |
| Bajada | "15 kg para Comedor Barrio Ejemplo" · "10 kg, todavía sin destino" | — |
| Etapas: título | Cada paso de esta donación | Nombre accesible de la lista: "Etapas de Lentejas secas" |
| Etapas | Las cinco de `pasos`, con su detalle ("12 de octubre", "Hoy, 14:30", "Pendiente", "11 de octubre: no pudo llegar", "Nueva salida: 17 de octubre") | Las mismas de Seguimiento |
| Foto: pie | "<Entidad> confirmó la entrega el <fecha> y subió esta foto." | Alt del dato: "Cajas con la ropa de abrigo junto a la puerta del merendero" |
| Foto: falta | "Falta confirmar la entrega: la confirma <entidad> con la foto." | La forma del tono común para lo pendiente |
| Datos: título | Los datos | — |
| Datos | "Donación" (N.º 5) · "Qué" · "Cuánto" · "Categoría" · "Para" · "Donada" · "Entregada" (si llegó) · "Figura como": "Distribuidora Ejemplo SA" con "Cambialo en Mi perfil" | "Figura como" es lo que ve la entidad (HB-20) |
| Noticias: título | Noticias de esta donación | "De la más nueva a la primera" |
| Acción | "Ver dónde está" → `donante-seguimiento` | Solo en curso (con destino y sin entregar) |

### donante-entidades

| Elemento | Texto | Nota |
|---|---|---|
| Rótulo | Entidades | — |
| Título | Quiénes reciben las donaciones | El mismo rótulo de la tierra en la portada |
| Bajada | "Entidades activas y lo que piden hoy. Cada una pide lo que necesita y confirma lo que recibe. Actualizado hoy, 15 de octubre, 16:25." | La entidad actúa: pide y confirma |
| Búsqueda | "Buscar por nombre" · "Buscar" · "Ver todas" | `type="search"`; nombre accesible del formulario: "Buscar entidades" |
| Cuenta | "46 entidades activas. Página 1 de 8." | El 46 es el de la portada |
| Tipo y zona | "Comedor comunitario en la zona oeste" | Del dato; sin hablar de carencias |
| Lo que ya donaste | "Tus fideos secos: entrega confirmada el 5 de octubre." · "Tus lentejas secas van en camino." | Con su sello; solo en las que recibieron algo de la empresa |
| Lo que pide | El mismo texto de cada pedido de la ficha pública: "Lentejas secas · Faltan 30 de 40 kg este mes · Se repite cada mes" · en uno de una sola vez: "Aceite de girasol · Faltan 20 litros · Hasta el 20 de octubre". Más de 3: "Ver los 5 pedidos" · "Ver menos" | Nombre accesible de la lista: "Lo que pide <entidad>". La barra, si hay, repite el texto |
| Sin necesidades | "Hoy no pide nada." | — |
| Paginado | "Página siguiente" · "Página anterior" | Nombre accesible: "Páginas de entidades" |
| Cómo donar: título | Cómo llega tu donación | — |
| Cómo donar | "Llevás la donación al depósito. Ahí la registran y la asignan a una necesidad abierta de una entidad. Desde que la registran, ves cada paso en Mis donaciones." · "Depósito Central Ejemplo · Calle Ejemplo 1234, Ciudad Ejemplo · Lunes a viernes de 9 a 17 h; sábados de 9 a 13 h" · "Ver Mis donaciones" | RF-15 y RF-17. Los datos del depósito, sintéticos (los de Cozy) |

Entidades de la maqueta (página 1). **Lo que pide cada una sale de una sola tabla:** la de
"Entidades con pedidos vigentes" de la [ficha pública](raices-publico-navegable.md#63-datos-de-maqueta),
con los mismos pedidos, cantidades, unidades y fechas (el porqué de cada pedido también, si el sitio
lo muestra). Acá se resume:

| Entidad | Tipo y zona | Lo que ya donó la empresa | Lo que pide (de la ficha pública) |
|---|---|---|---|
| Comedor Barrio Ejemplo | Comedor comunitario en la zona oeste | Fideos secos (entregada, 5 de octubre) · Lentejas secas (en camino) | Lentejas secas, faltan 30 de 40 kg este mes · Arroz, faltan 25 de 40 kg este mes · Aceite de girasol, faltan 20 litros · Leche en polvo, faltan 12 de 24 unidades · Ropa de abrigo, faltan 25 de 30 unidades |
| Escuela Ejemplo N.º 12 | Escuela primaria en la zona centro | Útiles escolares (con problema: sale de nuevo el 17 de octubre) | Sillas, faltan 14 de 20 unidades · Útiles escolares, faltan 4 cajas |
| Merendero Ejemplo Sur | Merendero en la zona sur | Ropa de abrigo (entregada, 9 de octubre) · Leche en polvo (pendiente) | Galletitas, faltan 10 de 20 kg este mes · Leche en polvo, faltan 20 unidades |
| Hogar Casa Ejemplo | Hogar de día para personas mayores en la zona norte | — | Frazadas, faltan 10 de 20 unidades |
| Club Ejemplo Norte | Club de barrio en la zona norte | — | Camisetas deportivas, faltan 15 unidades |
| Biblioteca Popular Ejemplo | Biblioteca popular en la zona centro | — | Sillas, faltan 4 de 8 unidades |

El orden es el de la portada y la ficha pública. Las seis de la página 1 tienen pedidos; "Hoy no
pide nada." queda para las entidades activas sin pedidos, que en la maqueta no se dibujan (están
en las páginas siguientes).

### donante-notificaciones

| Elemento | Texto | Nota |
|---|---|---|
| Rótulo | Notificaciones | — |
| Título | Noticias de tus donaciones | — |
| Bajada | "4 sin leer. Cada aviso cuenta qué pasó con una donación de tu empresa." | Sin "¡Tenés novedades!" |
| Acción | Marcar todas como leídas | — |
| Grupos | "Sin leer (4)" · "Leídas (13)" | — |
| Cada aviso | "Sin leer" o "Leída" · el sello del estado actual de su donación · el texto, enlace al detalle (`?donacion=<clave>`) · la fecha ("Hoy, 14:30", "Ayer, 17:00", "11 de octubre, 16:10") · "Lentejas secas, 15 kg" | — |
| Sin leer | Las cuatro del 14 y el 15 de octubre: lentejas salió (hoy, 14:30) · arroz recibido en el depósito (ayer, 17:00) · leche asignada a Merendero Ejemplo Sur (ayer, 12:15) · lentejas lista para entregar (ayer, 9:10) | — |
| Leídas | Las otras trece cartas de `viaje-datos`, de la más nueva a la más vieja | La de recepción dice "<Entidad> confirmó que recibió tu <donación> y sumó una foto." |

Ayudas por flujo crítico: ninguna de estas pantallas envía datos de la donación; la única acción,
"Marcar todas como leídas", es reversible en el contrato (`PATCH /{id}/leida`). La ayuda está en las
bajadas (qué es cada cifra y cuándo se actualizó) y en "Cómo llega tu donación".

Filtro de tono:

- Lo pendiente se dice pendiente: "Falta confirmar la entrega: la confirma <entidad> con la foto";
  "Todavía sin destino"; "esperan destino en el depósito".
- Actividad no es impacto: "donaciones registradas" (actividad) y "entregas confirmadas"
  (resultado), separadas; nunca "familias ayudadas" ni "necesidades satisfechas".
- Las cifras llevan período, fuente y fecha de actualización.
- La entrega con problema dice qué pasó, cuándo se supo y la nueva salida, sin urgencia.
- Las entidades piden y confirman; ninguna se describe por una carencia.
- Sin rankings ni comparaciones entre entidades o donantes.
- Palabras que no van (brief de Raíces): "eco", "huella verde", "florecer", "regá", "cosechá" y
  "sostenible" puesto de adorno. Tampoco "donar ahora" ni signos de exclamación.

## 7. Accesibilidad

- Zonas: el enlace para saltar al contenido, la navegación del donante (`nav` con nombre), el
  contenido (`main`) con un solo `h1` y un `h2` por parte.
- La sección actual del menú lleva `aria-current="page"`; en el detalle, Mis donaciones.
- Orden del foco: saltar al contenido, la marca, el menú, el acceso a Notificaciones, la cuenta
  (Mi perfil, Salir), y después el contenido en el orden de lectura. En el detalle, "Volver a Mis
  donaciones" va antes del título en el código, como en Cozy.
- Cifras: lista de definiciones (`dl`) con nombre ("Lo que está confirmado"); ningún número va
  solo. Si un sitio las dibuja, el dibujo es `aria-hidden` y el texto dice lo mismo.
- Estados: siempre texto y forma (el sello); el color solo acompaña. "Sin leer" y "Leída", en texto.
- Etapas: lista ordenada (`ol`) con nombre; la actual con `aria-current="step"`; cada detalle en
  texto.
- Filtros y búsqueda: `label` visible por campo; el resultado ("2 donaciones con esos filtros.") en
  una región `role="status"`, para que se lea después de filtrar.
- "Listo: marcaste todas como leídas." toma el foco al cargar y es `role="status"`.
- Errores: `role="alert"`; carga: `aria-busy` en la parte que espera.
- Barras de "lo que pide": `aria-hidden`, con el texto completo al lado.
- La foto lleva su `alt` del dato; con "reducir movimiento", nada se anima.
- Objetivos táctiles de 48 × 48 px ([piso](../direccion-de-arte.md#el-piso-no-se-negocia)); nada se
  corta ni desborda en 360 px.

## 8. Revisión de usabilidad

| # | Heurística | Cómo la cumple | Riesgo que queda |
|---|---|---|---|
| 1 | Visibilidad del estado | Cada donación con su sello, su nota y su etapa; cifras y listas con su fecha de actualización; "4 sin leer" | El estado de cada donación cruza Donaciones y Logística (HB-16); si no llega en una llamada, el inicio tarda |
| 2 | Lenguaje del usuario | Donación, depósito, entidad, en camino, entrega confirmada; sin códigos ni "persona jurídica" | — |
| 3 | Control y libertad | Solo consulta; menú, "Volver a Mis donaciones" y Atrás; "Sacar los filtros" y "Ver todas" | "Marcar todas como leídas" no se deshace en la maqueta (en el sistema, una por una) |
| 4 | Consistencia | Los datos, números y estados de Seguimiento; "Lo que está confirmado" y "Quiénes reciben las donaciones", como la portada; el mismo banner de error con código | Los nombres de las entidades de `viaje-datos` y los de la portada no coinciden del todo (ver Decisiones) |
| 5 | Prevención de errores | Filtros con opciones cerradas; un valor desconocido se ignora y muestra todas, no un error | — |
| 6 | Reconocer antes que recordar | El N.º y el nombre de cada donación iguales en todas las pantallas; cada aviso nombra su donación | — |
| 7 | Flexibilidad | Quien quiere el panorama se queda en el inicio; quien busca algo filtra o entra al detalle desde cualquier pantalla | Sin atajos de teclado propios |
| 8 | Diseño minimalista | Cada pantalla responde una pregunta: qué pasó (inicio), qué donamos (mis donaciones), qué pasó con esta (detalle), a quién (entidades), qué hay de nuevo (notificaciones) | El inicio tiene cuatro partes: cada sitio tiene que jerarquizarlas |
| 9 | Recuperarse de errores | El error dice que las donaciones no se afectan, da el código y "Volver a intentar"; la donación que no existe lleva a la lista | — |
| 10 | Ayuda | "Cómo llega tu donación" en Entidades; las bajadas dicen qué cuenta cada cifra | No hay página de ayuda propia |

Los 7 factores de la experiencia ([criterios de la cátedra](../criterios-catedra-ux-ui.md),
sección 2):

| Factor | Cómo lo cumple |
|---|---|
| Útil | Responde lo que la persona de RSE tiene que contar: qué se donó, a quién llegó y qué lo prueba |
| Usable | Listas con enlaces, filtros de opciones cerradas y estados con texto; nada que aprender |
| Deseable | La identidad de Raíces (paleta Oliva, risografía, sellos) y un hito de lo acumulado dibujado con los datos |
| Valioso | La foto y la fecha de confirmación de cada entrega son la prueba que la empresa muestra |
| Ubicable | Cuatro secciones en el menú, Notificaciones a un toque desde las pantallas (en A, salvo Seguimiento y Mi perfil, aprobadas: desde Inicio) y nada a más de 3 niveles |
| Accesible | Estados con forma y texto, listas como alternativa a todo dibujo, foco ordenado; WCAG AA y Ley 26.653 |
| Creíble | Cada cifra con período, fuente y fecha; lo pendiente se dice pendiente; la entrega con problema se muestra tal cual |

Qué conviene probar con personas reales (la revisión heurística no reemplaza al test con
usuarios): si una persona de RSE distingue "donaciones registradas" de "entregas confirmadas" sin
ayuda, y si encuentra en menos de un minuto la foto de una entrega para mostrarla.

## 9. Decisiones y preguntas

Decisiones tomadas (agente, propuestas; se aprueban al elegir A o B):

- Ingresar con la cuenta de ejemplo de la empresa lleva a `donante-inicio` (plan de la corrida).
- Misiones e incentivos quedan fuera: la cuenta es una empresa (RF-07).
- Los datos son los de `viaje-datos`, los mismos de Seguimiento, para que todo el recorrido diga lo
  mismo. Las entidades suman las de la portada que no tienen donaciones de la empresa: Hogar Casa
  Ejemplo, Club Ejemplo Norte y Biblioteca Popular Ejemplo.
- Notificaciones no entra al menú; se llega desde el inicio y desde un acceso fijo que ubica cada
  sitio.
- Entidades sin detalle ni botón "Donar": la entidad la elige el admin (RF-17).
- Paginado de seis por página; la maqueta dibuja solo la página 1 y lo dice.
- "Marcar todas como leídas" va a `donante-notificaciones-leidas`, el patrón de
  `donante-perfil-guardado`.
- Los desvíos del boceto (los anillos del inicio, las tablas de las págs. 6 y 16, la pág. 17) van a
  la [bitácora](../bitacora.md) en el cierre de la corrida, junto con la elección de A o B.

Para la integración (no se resuelven acá):

- Nombre de la escuela: las tres fichas de la corrida usan "Escuela Ejemplo N.º 12", el de la
  portada. `viaje-datos` y Seguimiento (aprobada, compartida con Cozy) siguen diciendo "Escuela
  Ejemplo 12": se igualan al integrar. Lo mismo con la categoría "Vestimenta" de `viaje-datos`,
  que las pantallas nuevas muestran "Ropa".
- La portada dice "Registrás lo que donás" y RF-15 dice que la registra el admin en el depósito.
  Esta ficha dice "Ahí la registran". Si el grupo decide otra cosa, cambia "Cómo llega tu donación".

Preguntas abiertas para el usuario (al elegir A o B):

1. "¿La empresa puede elegir a qué entidad o necesidad va lo que dona?" → "No: la elige el admin
   con los algoritmos (RF-17); Entidades solo informa (Recomendada)" · "Sí: un 'Donar' por necesidad,
   como en Cozy (pide un requerimiento y un hueco nuevos)".

## Huecos de backend

Datos que la maqueta inventa porque el backend no los da hoy. Van también a la lista de la
corrida ([raices-ab-huecos-backend.md](raices-ab-huecos-backend.md)), sin DTI ni ADR nuevos.

| Pantalla | Dato que falta | Por qué |
|---|---|---|
| donante-inicio | Resumen de la empresa: entregas confirmadas, entidades que confirmaron, donaciones registradas y desde cuándo | No hay un endpoint agregado; contarlo del listado de RF-05 obliga a cruzar cada donación con su entrega en Logística (HB-16) |
| donante-donaciones | Filtro por categoría | HB-03 |
| donante-donaciones, detalle | Fecha de cada etapa y nota del estado | El listado de Donaciones no trae las etapas; el historial está en Logística por entrega (`/api/entregas/{id}/historial`) y desde la donación solo se llega por el seguimiento (HB-16) |
| donante-donaciones-detalle | Foto y fecha de recepción | `SeguimientoEntregaResponseDTO` no trae fotos; están en el DTO de la entrega |
| donante-donaciones-detalle | "Figura como" (nombre visible) | HB-20 |
| donante-entidades | Búsqueda por nombre, paginado, solo activas, tipo y zona | HB-08 y HB-15 (hoy `GET /api/entidades` devuelve todo, con las dadas de baja) |
| donante-entidades | Lo que pide cada entidad, con cuánto recibió y cuánto falta | HB-18 |
| donante-entidades | Lo que la empresa ya donó a cada entidad | No hay filtro de las donaciones de un donante por entidad asignada (HB-04 es del lado de la entidad) |
| donante-entidades | Dirección y horario del depósito | No hay endpoint de depósitos |
| donante-notificaciones | Tipo de evento e id de la donación en `NotificacionDTO` | Sin ellos no se pone el sello ni se enlaza al detalle (hoy trae solo `mensaje`) |
| donante-notificaciones | Avisos "recibida en el depósito" y "lista para entregar" | No están en `EventoNotificableDTO`; RF-08 pide asignación y recepción (HB-09) |
