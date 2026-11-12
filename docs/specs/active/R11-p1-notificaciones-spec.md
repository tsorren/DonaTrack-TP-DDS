# Spec: R11-p1-notificaciones — Bandeja de notificaciones paginada en el servidor

> **Aprobada el 2026-11-12** con las opciones recomendadas (aprobación automática pedida por el usuario; ver la sección final de aprobación). Lo que sigue sobre "pendiente de aprobación" queda superado.
>
> **Estado:** PROPOSED (2026-11-12). La aprobación es la del usuario al revisar juntos los diseños
> de la oleada P1; ningún ADR se promueve.
> **Nivel:** STANDARD por su forma (un endpoint existente gana parámetros, sin cambiar el cuerpo),
> ruta CONTRATO del [manifiesto](../../interfaz/backlog-revision-2026-11-11.manifiesto.json): cambia
> el comportamiento de un contrato público, así que lleva esta spec antes del código.
> **Fecha:** 2026-11-12
> **Módulos impactados:** `notificaciones-service`, `cliente-liviano`
> **Base:** `ebe332642` (rama de la oleada P1).
> **Relación:** ítem R11-14 del [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md);
> pantalla en la [ficha de flujo del lote](../../interfaz/flujos/r11-p1-notificaciones.md). Sigue el
> mismo contrato de listado que [R11-p0-api-listados](../completed/R11-p0-api-listados-spec.md)
> (§3.1 y §0: página, tope, total en `X-Total-Count`). Usa el paginador del lote `p1-paginador`
> (R11-11).

---

## 1. Objetivo Funcional (Goal)

La bandeja de notificaciones del donante y de la organización se pide de a una página. Notificaciones
resuelve la página, "Sin leer", la búsqueda, la categoría y el orden, y devuelve el total como
número. El cliente deja de traer y contar la lista completa (regla general 1 del backlog) y dibuja
cada fila según la decisión "Notificaciones leídas" de la sección 3 del backlog.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (lo que incluye)

- **Notificaciones:** `GET /api/notificaciones/persona/{personaId}` (y su alias legado
  `/notificaciones/persona/{personaId}`, que llama al mismo método) acepta `page`, `size`, `q`,
  `tipo`, `excluirTipo` y `orden`, y devuelve `X-Total-Count`. La consulta pagina, filtra y ordena en
  la base, en las dos implementaciones del repositorio (JPA y en memoria).
- Migración `V6__indice_bandeja_por_persona.sql` (la última de Notificaciones es `V5`): índice por
  persona y fecha para el orden y la página.
- OpenAPI `openapi-notificaciones.yaml` de `1.2.0` a `1.3.0`, con los parámetros, el header y el
  cambio de comportamiento sin página.
- **Cliente liviano:**
  - `NotificacionesApi.bandeja` con los parámetros nuevos y la respuesta con su header.
  - `DonanteNotificacionesRest` y `EntidadNotificacionesRest` piden una página con los filtros de la
    dirección, leen el total del header y el número de sin leer del conteo, y arman el título de
    cada aviso (tabla de la ficha). `marcar` deja de pedir la bandeja completa.
  - `componentes/buzon.html`: un fragmento nuevo para la bandeja paginada (los filtros ya vienen
    resueltos), "Mostrando X a Y de N", el paginador, la nota de "Marcar todas" y la fila con título,
    mensaje y vínculo aparte; sobre cerrado para "Marcar como no leída".
  - `buzon.css`: fondo suave, punto y título en negrita para sin leer; sin fondo y texto apagado para
    leída; vínculos en peso normal y subrayados.
- Lo imprescindible fuera de esos archivos para que llegue la página (ver §7).

### Out-of-Scope (lo que no incluye)

- La campanita con desplegable y las notificaciones del admin (`p2-campanita`, R11-29 y R11-30). La
  bandeja del admin sigue con su lista de ejemplo y el fragmento `buzon` de hoy.
- `GET /api/notificaciones` (el listado general por persona o estado, solo ADMIN): no lo usa ninguna
  pantalla; queda para `p2-listas-restantes` o su lote (se anota como pendiente).
- Lo que `EntidadNotificacionesRest` lee de `DonacionesApi.asignadas` para "Confirmar recepción"
  (`p2-listas-restantes`, DTI-39 ítem 1). Cambia solo en que se calcula para los avisos de la página.
- Un campo `titulo` en la notificación, un conteo por categoría o un "marcar como leídas las de este
  filtro" en el servicio.
- El cambio de "entidad" a "Organización" en los textos (`p1-nombres`). Esta spec no renombra rutas,
  clases ni campos.
- Búsqueda con `unaccent` o índices de texto (ver §3.7).

## 3. Decisiones Acordadas y Trade-offs: el contrato antes y después

Sin matriz 5D, como pide el lote: cada decisión dice la alternativa elegida y las descartadas (§3.5 a §3.10).

### 3.1 Antes

`GET /api/notificaciones/persona/{personaId}` (permiso `VER_SUS_NOTIFICACIONES`) y el alias
`GET /notificaciones/persona/{personaId}` (ADMIN):

| Parámetro | Tipo | Qué hace |
|---|---|---|
| `personaId` (ruta) | uuid | La persona |
| `leida` | boolean, opcional | `true` o `false`, solo esas; sin valor, todas |

Respuesta `200`: arreglo de `NotificacionDTO` (`id`, `mensaje`, `estado`, `fechaCreacion`, `leida`,
`fechaLectura`, `tipoEvento`, `referenciaId`) con **todas** las de la persona, sin orden definido
(el de la base). Sin header de total. `personaId` desconocido: `200` con `[]`.

### 3.2 Después

Misma ruta, mismo permiso, mismo cuerpo. Parámetros:

| Parámetro | Tipo | Por defecto | Qué hace |
|---|---|---|---|
| `personaId` (ruta) | uuid | — | Igual que antes |
| `leida` | boolean | todas | Igual que antes |
| `q` | texto, hasta 100 | — | Las que contienen el texto en `mensaje`, sin distinguir mayúsculas ni tildes (`camion` encuentra "camión"). Vacío o solo espacios: sin filtro |
| `tipo` | repetible, valores de `TipoEvento` | — | Solo las de esos tipos |
| `excluirTipo` | repetible, valores de `TipoEvento` | — | Todas menos las de esos tipos; **las que no tienen tipo entran** (así el cliente arma "Otras novedades") |
| `orden` | `recientes` \| `antiguas` | `recientes` | Por `fechaCreacion` descendente o ascendente; desempata por `id` en el mismo sentido, para que una fila no salte de página |
| `page` | entero ≥ 0 | 0 | La página |
| `size` | 1 a 100 | 20 | Cuántas por página |

`tipo` y `excluirTipo` juntos se combinan con "y". Respuesta:

| | |
|---|---|
| `200` | Arreglo de `NotificacionDTO` (sin cambios) con el tramo pedido, en el orden pedido |
| Header `X-Total-Count` | Cuántas cumplen `leida`, `q`, `tipo` y `excluirTipo`, sin contar la página. Siempre presente, también con `[]` |
| Sin `page` ni `size` | Página 0 de 20, en el orden por defecto, con `X-Total-Count` (como R11-p0 §0: no hay modo "devuelve todo") |
| Página fuera de rango | `200` con `[]` y el `X-Total-Count` real |
| `400` (`ERR-CSR-003`) | `page` negativo, `size` fuera de 1..100, `q` de más de 100 caracteres, `orden` desconocido o un `tipo`/`excluirTipo` que no es un `TipoEvento` |
| `personaId` sin notificaciones | `200` con `[]` y `X-Total-Count: 0` |

Ejemplos:

```http
GET /api/notificaciones/persona/{id}?page=1&size=20
GET /api/notificaciones/persona/{id}?leida=false&q=camion&tipo=DONACION_EN_CAMINO&page=0&size=20
GET /api/notificaciones/persona/{id}?excluirTipo=DONACION_ASIGNADA&excluirTipo=DONACION_EN_CAMINO&excluirTipo=DONACION_RECIBIDA&excluirTipo=ENTREGA_FALLIDA&excluirTipo=DONACION_VENCIDA
```

Los otros endpoints (`.../no-leidas/conteo`, `PATCH /{id}/leida`, `.../marcar-todas-leidas`) no
cambian.

### 3.3 Base

```sql
-- V6__indice_bandeja_por_persona.sql
CREATE INDEX idx_notificacion_persona_fecha
    ON notificacion (persona_id, fecha_creacion DESC, id DESC);
```

`idx_notificacion_persona_leida` (V3) queda: lo usa el conteo de sin leer.

### 3.4 Cliente liviano

| Pantalla | Parámetro de la dirección | Va al servicio como |
|---|---|---|
| Bandeja | `?pagina=N` (desde 1) | `page=N-1`, `size=20` |
| | `?ver=sin-leer` | `leida=false` |
| | `?buscar=<texto>` | `q=<texto>` recortado |
| | `?tema=<valor>` | `tipo=` los tipos de esa categoría; `otras`: `excluirTipo=` los tipos de todas las demás categorías del rol |
| | `?orden=viejas` | `orden=antiguas` |
| | `?agrupar=` | No va al servicio: agrupa dentro de la página |

Una `?pagina` más allá de la última se vuelve a pedir como la última (`page = ceil(total/20) - 1`),
una sola vez. "N sin leer" sale de `.../no-leidas/conteo`.

### 3.5 Mismo contrato que los listados de P0

Página y total en `X-Total-Count`, cuerpo sin envoltorio, tope de 100 y página 0 de 20 sin
parámetros. Es el contrato que ya decidió R11-p0 (ADR `proposed` de listados paginados de
Donaciones) aplicado a un caso nuevo: por la regla de dos puertas de `AGENTS.md` §9.1 no es una
decisión nueva. Descartado: un `Page` de Spring o un objeto `{contenido, total}` en el cuerpo
(rompe a quien espera un arreglo y deja dos formas de paginar en el sistema).

### 3.6 Categoría con `tipo` y `excluirTipo`

Las categorías son de la pantalla (cambian por rol y agrupan tipos: "Problemas" son la entrega
fallida y la vencida). El servicio filtra por tipo; el cliente traduce. `excluirTipo` resuelve
"Otras novedades" (sin tipo o de un tipo que el rol no agrupa) sin que el cliente conozca todos los
tipos. Descartado: un valor especial `tipo=NINGUNO` (obliga al cliente a listar el resto de los
tipos) y categorías en el servicio (presentación en el dominio).

### 3.7 Búsqueda sin tildes con `translate`, sin extensión nueva

`translate(lower(mensaje), 'áéíóúüñ', 'aeiouun') LIKE '%' || :q || '%'`, con `q` normalizado igual
en Java y `%`, `_` y `\` escapados. Anda en PostgreSQL y en H2. Primero filtra por `persona_id`
(índice), así que recorre solo las de una persona. Descartado: `unaccent` (extensión nueva en el
esquema de Notificaciones, con un ADR propio todavía `proposed` para Donaciones) y `ILIKE` solo
(no encuentra "camion" en "camión").

### 3.8 La página no carga el historial de estados

`NotificacionEntity.historialEstado` es `EAGER` y el DTO no lo usa. La consulta de la página
devuelve las columnas del DTO (proyección), no la entidad: una consulta para la página y otra para
el total. Descartado: leer entidades con `@BatchSize` (sigue cargando un historial que nadie
muestra).

### 3.9 Quién es dueño al marcar

`PATCH /{id}/leida` ya exige `MARCAR_SU_NOTIFICACION` (el dueño sale de la notificación). El
adaptador del donante deja de pedir la bandeja completa para filtrar ids ajenos; un id ajeno o
inexistente devuelve `403` o `404`, que el adaptador ignora para ese id (como hoy ignora los que no
son suyos) y sigue con los demás.

### 3.10 Fragmento nuevo en el buzón, el de hoy se queda

`buzon(...)` filtra en la plantilla y lo usa la bandeja del admin con datos de ejemplo. El fragmento
nuevo recibe la página ya resuelta (avisos, total, sin leer y paginador) y comparte `fila`. El viejo
se borra cuando `p2-campanita` conecte la bandeja del admin. Descartado: cambiar la firma de `buzon`
(rompe la pantalla del admin, que no es de este lote).

## 4. Invariantes y compatibilidad con los clientes actuales

- El cuerpo de la respuesta no cambia: un arreglo de `NotificacionDTO` con los mismos campos.
- `X-Total-Count` es siempre el total con los filtros aplicados, nunca el largo de la página.
- Ninguna respuesta de la bandeja trae más de 100 notificaciones.
- Los permisos no cambian: `VER_SUS_NOTIFICACIONES` para la bandeja, `MARCAR_SU_NOTIFICACION` para
  marcar una.
- Leer la bandeja no cambia ninguna notificación (no marca nada como leída).

| Cliente | Qué usa hoy | Qué le pasa |
|---|---|---|
| Cliente liviano, bandejas del donante y de la organización | `bandeja(personaId, null)` y espera todas | Se migra en este lote (§3.4). Es el único consumidor de la ruta `/api/...` |
| Cliente liviano, conteo, marcar y marcar todas | Los tres endpoints que no cambian | Nada |
| Cliente liviano, bandeja del admin | Datos de ejemplo en su plantilla, fragmento `buzon` | Nada: el fragmento de hoy se queda (§3.10); toma los estilos nuevos de la fila, sin título (sin leer: fondo y punto, el mensaje sin negrita) |
| Colecciones de Postman (`flujo-7`, `flujo-8`), alias legado | `/notificaciones/persona/{id}` y miran que haya avisos | Reciben las 20 más nuevas. Cada persona de esas colecciones tiene menos de 20 avisos: no cambia lo que comprueban `[INFERRED]`. Se corre `flujo-7` para confirmarlo |
| Quien llame sin parámetros y espere todas | Ninguno conocido `[OBSERVED]` (grep de `notificaciones/persona` en el repo) | Recibe 20 con el total en el header. Es un cambio de comportamiento, como el de R11-p0 §0: va en el OpenAPI y, si `oasdiff` lo marca, se registra en el PR |
| Los records del cliente | `NotificacionDTO` sin cambios | Nada; el header nuevo lo leen solo los adaptadores migrados |

## 5. Criterios de Aceptación

```gherkin
C1 Página y total
  Dado una persona con 45 notificaciones
  Cuando se pide GET /api/notificaciones/persona/{id}?page=1&size=20
  Entonces llegan 20, de la 21.ª a la 40.ª más nueva
  Y X-Total-Count vale 45

C2 Sin parámetros
  Cuando se pide GET /api/notificaciones/persona/{id} (o el alias /notificaciones/persona/{id})
  Entonces llegan las 20 más nuevas, de la más nueva a la más vieja, con X-Total-Count: 45

C3 Filtros en el servidor
  Dado 10 sin leer, 3 de ellas DONACION_EN_CAMINO con "camión" en el mensaje
  Cuando se pide ?leida=false&q=CAMION&tipo=DONACION_EN_CAMINO
  Entonces llegan esas 3 y X-Total-Count vale 3

C4 excluirTipo incluye las sin tipo
  Dado 2 sin tipo, 1 MISION_CUMPLIDA y 5 DONACION_ASIGNADA
  Cuando se pide ?excluirTipo=DONACION_ASIGNADA
  Entonces llegan las 3 que no son DONACION_ASIGNADA (incluidas las 2 sin tipo)

C5 Orden estable
  Dado 3 notificaciones con la misma fechaCreacion
  Cuando se piden con size=1 en las páginas 0, 1 y 2, con orden=recientes y con orden=antiguas
  Entonces cada una aparece una sola vez en cada recorrido

C6 Errores
  Cuando se pide page=-1, size=0, size=101, q de 101 caracteres, orden=otro o tipo=NO_EXISTE
  Entonces la respuesta es 400 con ERR-CSR-003

C7 Fuera de rango
  Cuando se pide page=9 con 45 notificaciones
  Entonces la respuesta es 200 con [] y X-Total-Count: 45

C8 Permisos sin cambios
  Dado un donante con token
  Cuando pide la bandeja de otra persona
  Entonces la respuesta es 403, como antes

C9 La página no carga el historial
  Cuando se pide una página de 20
  Entonces se ejecutan dos consultas (la página y el total) y ninguna lee notificacion_historial_estado

C10 El cliente no trae la lista completa
  Dado el cliente con datos reales
  Cuando se abre donante-notificaciones o entidad-notificaciones, con o sin filtros, en cualquier página
  Entonces cada llamada a la bandeja lleva page y size=20
  Y marcar una o varias no pide la bandeja

C11 Cifras del servicio
  Entonces "N sin leer" sale del conteo y "Mostrando X a Y de N" del X-Total-Count

C12 Filtros y página en la dirección
  Dado ?ver=sin-leer&tema=camino&buscar=lentejas&pagina=2
  Cuando se toca la página 3 o se marca una fila
  Entonces la dirección conserva ver, tema y buscar
  Y ?pagina=99 muestra la última página que existe

C13 Cómo se ve cada fila (360, 1280 y 1920 px, las dos pantallas)
  Entonces una sin leer tiene fondo suave, punto y solo el título en negrita
  Y una leída no tiene fondo propio ni punto, y su título y mensaje van en el tono apagado
  Y los vínculos de las dos van en peso normal y subrayados
  Y "Marcar como no leída" usa el sobre cerrado y "Marcar como leída" el tilde
  Y el texto apagado, el título sobre el fondo suave y el vínculo sobre los dos fondos cumplen AA
  Y con colores forzados la sin leer sigue distinguiéndose por el punto y la negrita

C14 Sin JavaScript
  Entonces filtrar, cambiar de página y marcar funcionan con la página completa
```

## 6. Riesgos

- **El total y la página son dos lecturas:** si llega un aviso entre las dos, el total puede diferir
  en uno de lo que se ve. Se acepta: la pantalla no muestra cifras exactas de algo que cambia solo.
- **Una fila repetida al pasar de página** si llegan avisos nuevos mientras se mira. Se acepta; el
  orden con desempate por `id` evita que una se pierda.
- **`translate` no usa índice** para `q`: con una persona con muchos miles de avisos la búsqueda
  recorre todos los suyos. Hoy el volumen es por persona (decenas a cientos). Si la importación
  masiva (R11-B08) manda muchos avisos a una sola persona, se mide en el ensayo de la oleada.
- **El comportamiento sin parámetros cambia** para el alias legado. Mitigación: §4.
- **Los títulos por tipo viven en el cliente:** un tipo nuevo cae en "Novedad" hasta que se agregue
  a la tabla. Es un texto, no un error.
- **El paginador de `p1-paginador`** puede no estar integrado cuando se construya este lote: se usa
  el provisorio con las mismas direcciones (ficha, sección 4).

## 7. Archivos

Del lote (manifiesto): en `notificaciones-service`, el listado paginado por persona
(`NotificacionController`, `INotificacionController`, `NotificacionService`,
`INotificacionRepository` y sus dos implementaciones, `SpringDataNotificacionRepository`, la
migración `V6`, sus tests y `openapi-notificaciones.yaml`); en `cliente-liviano`,
`componentes/buzon.html`, `componentes/buzon.css`, `DonanteNotificacionesRest.java`,
`EntidadNotificacionesRest.java` y sus tests.

Fuera de la lista e imprescindibles para que la página llegue a la pantalla (se declaran en
`fueraDeZona` al construir): `rest/NotificacionesApi.java` (la firma de `bandeja`), los dos puertos
y sus vistas (`DonanteNotificacionesPuerto`, `EntidadNotificacionesPuerto`,
`DonanteNotificacionesVista`, `EntidadNotificacionesVista`: la consulta y la página), las dos
maquetas (paginan en memoria sus datos de ejemplo y escriben el título), los dos controllers de la
bandeja y los dos de marcar (leen y conservan los parámetros), y las llamadas al buzón en
`donante-notificaciones.html`, `persona/donante-notificaciones.html` y `entidad-notificaciones.html`.
El catálogo `/estilos` y `docs/interfaz/componentes.md` suman el fragmento nuevo y los estados de la
fila.


## Aprobación del diseño p1-notificaciones (2026-11-12)

Aprobado por el usuario de forma automática: pidió que, en P1 y P2, cada duda se resuelva con la opción recomendada por el diseñador, sin parada. Cada duda queda resuelta así:

1. ¿Se aceptan los títulos por tipo de aviso que propone la ficha (tabla de la sección 6)? Recomiendo aceptarlos así y ajustar palabras sueltas al revisar las capturas. **Resuelto: la opción recomendada.**
2. ¿Hace falta un ADR para este lote al cerrar la oleada, como pide la guía de orquestación para los lotes que cambian un contrato? Recomiendo uno corto, propuesto, en notificaciones-service, que remita al ADR de listados paginados de P0 y deje anotado el desvío (translate en lugar de unaccent). **Resuelto: la opción recomendada.**
3. Si p1-paginador no está integrado cuando se construya este lote: recomiendo usar el paginador provisorio, que ya existe y usa las mismas direcciones, y cambiarlo al integrar. **Resuelto: la opción recomendada.**
4. GET /api/notificaciones, el listado general solo para el admin, sigue devolviendo todo, pero no lo usa ninguna pantalla. Recomiendo anotarlo en DTI-40 como pendiente de p2-listas-restantes en lugar de sumarlo a este lote. **Resuelto: la opción recomendada.**
