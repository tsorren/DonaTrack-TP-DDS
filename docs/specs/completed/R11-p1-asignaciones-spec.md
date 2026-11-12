# Spec: R11 p1-asignaciones — Cada propuesta dice qué algoritmo la sugirió y en qué puesto

> **Cerrada el 2026-11-12** (cierre documental de la oleada P1, rama `E6_backlog_r11_p1`): construida e integrada; pasa a `completed/`. ADR `proposed`, escrito después del código: [sugerencia de cada algoritmo y situación](../../adr/donaciones-service/20261112-propuestas-con-la-sugerencia-de-cada-algoritmo-y-su-situacion-calculada-en-donaciones.md). Lo que quedó pendiente está en [DTI-42](../../adr/DEUDA_TECNICA.md#dti-42). Lo que sigue sobre "pendiente de aprobación" o "sin ADR" queda superado.
>
> **Aprobada el 2026-11-12** con las opciones recomendadas (aprobación automática pedida por el usuario; ver la sección final de aprobación). Lo que sigue sobre "pendiente de aprobación" queda superado.
>
> **Estado:** PROPOSED (2026-11-12). Falta la aprobación del usuario. Diseño del lote
> `p1-asignaciones` del [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md)
> (R11-24; cierra para Asignaciones los ítems 1 y 2 de [DTI-40](../../adr/DEUDA_TECNICA.md#dti-40)).
> **Ficha de flujo:** [r11-p1-asignaciones.md](../../interfaz/flujos/r11-p1-asignaciones.md).
> **Nivel:** ARCHITECTURAL por AGENTS.md §7.0 (cambia el contrato REST de Donaciones y suma una
> migración), con el formato corto de la ruta STANDARD de `define-spec`, como pide el lote.
> **ADR:** no hace falta uno nuevo (§7). El paginado aplica el ADR `proposed`
> [listados siempre paginados](../../adr/donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md).
> **Fecha:** 2026-11-12 · **Base:** `ebe332642`.
> **Módulos:** `donaciones-service` (dominio de propuestas, servicio, controller, migración y
> tests), `cliente-liviano` (Asignar donaciones), `docs/arquitectura/contratos/openapi-donaciones.yaml`.

## 1. Objetivo Funcional (Goal)

Que el admin vea, en cada propuesta de asignación, **qué algoritmo la sugirió y en qué puesto**, y
que la pantalla Asignar donaciones pase a la variante C mejorada (pestañas Propuestas / Conflictos /
Destino) sin pedir nunca la lista completa de propuestas ni de ejecuciones.

`[OBSERVED]` Hoy el gestor corre los dos algoritmos, consolida y guarda la propuesta sin recordar
quién la sugirió (`GestorPropuestasDeAsignacion.java:26-84`, `V11__propuestas.sql`), y el cliente
lee todas las propuestas y todas las ejecuciones, con una llamada por necesidad
(`AdminOperacionRest.java:141-189`).

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (lo que incluye)

1. **Donaciones guarda de dónde salió cada propuesta.** Por cada algoritmo que la sugirió: el
   algoritmo, el puesto en la lista de ese algoritmo en esa corrida (1 = la primera), cuántas
   propuso ese algoritmo en esa corrida, y si sus donaciones son las que quedaron en la propuesta.
   Cuando los dos sugieren la misma necesidad, la propuesta que queda (la del segundo) lleva las dos
   sugerencias. Migración nueva `V24` (la siguiente a `V23`; la ficha la llamaba `V22`): tabla hija de `propuesta`.
2. **La respuesta de la propuesta suma campos** (aditivo): las sugerencias, la situación (libre, en
   conflicto, ya no alcanza), cuántas otras pendientes cubren la misma necesidad, la organización
   y la unidad de la necesidad, y por cada donación lo que hay hoy en el depósito y lo que piden de
   ella las otras pendientes.
3. **`GET /api/asignaciones/propuestas` pagina y filtra en la base:** `estado` (repetible),
   `situacion` (repetible), `algoritmo`, `donacion`, `orden`, `page`, `size`. Total en
   `X-Total-Count`. Sin parámetros, la página 0 de 20 (como el resto de los listados desde R11-01).
4. **`GET /api/asignaciones/propuestas/resumen`** (nuevo): los totales de las pendientes por
   situación, como números.
5. **`GET /api/asignaciones/donaciones`** (nuevo): las donaciones que están en al menos una
   propuesta pendiente, paginadas, con cuántas propuestas las piden y si alcanzan.
6. **`GET /api/asignaciones/ejecuciones` pagina**, de la más reciente a la más vieja.
7. **Cliente liviano:** `admin-asignaciones` con las tres pestañas y la tarjeta común de la ficha;
   la variante C deja de ser variante; se borran las variantes A, B y C, su fragmento común de
   datos, sus hojas de estilo y `static/js/asignaciones.js` (cantidades ponderadas); la maqueta pasa
   a tener datos sintéticos con algoritmo y puesto; `AdminOperacionRest` pide solo la página y los
   totales que muestra.
8. OpenAPI de Donaciones 1.10.0 con los esquemas de estas rutas (hoy no tienen ninguno) y la línea
   de cambio de comportamiento en `cambios-incompatibles-aceptados.txt`.

### Out-of-Scope (lo que no incluye)

- Elegir qué algoritmo correr (R11-B05): `POST /api/asignaciones/ejecuciones` sigue sin parámetros.
- Buscar en las decididas, su fecha, quién aprobó y el motivo del descarte (`p2-admin-bandeja`,
  R11-35). Acá las decididas solo se paginan.
- Autor real de la aprobación (`p1-admins`, R11-10): `aceptar("SISTEMA")` no se toca.
- Rechazar en el servidor la aprobación de una propuesta que ya no alcanza (pregunta 2 de la ficha).
- Aprobar en lote, cantidades editables o "ponderadas", elegir una organización que ningún algoritmo
  propuso.
- Guardar las ejecuciones en la base (hoy en memoria, `AsignacionesRepositoryEnMemoria`).
- Renombrar rutas, clases o campos por "Organización" (`p1-nombres`).
- El inicio del admin (`p1-admin-inicio`): ya toma `propuestasPendientes` de `/api/donaciones/resumen`.

## 3. Decisiones Acordadas y Trade-offs: el contrato antes y después

Decisiones del diseñador del lote (sin matriz 5D, como pide el lote):

- **Alternativa elegida para el algoritmo y el puesto:** una tabla hija de `propuesta` con una fila
  por algoritmo que la sugirió. Descartadas: dos columnas fijas en `propuesta` (una por algoritmo:
  hay que migrar otra vez cuando se sume uno, y R11-B05 va a elegir cuál correr) y un texto JSON
  (no se puede filtrar ni ordenar en la base).
- **El puesto es el orden en la lista de cada algoritmo, con su total.** Descartado: un puntaje
  numérico, que hoy solo existe en la compatibilidad semántica y no se expone (sería inventarlo
  para el otro).
- **La situación (libre, en conflicto, ya no alcanza) la calcula Donaciones.** Descartado: que el
  cliente cruce las propuestas para encontrar los choques, porque necesita todas las pendientes
  (regla 1 del backlog).
- **Arreglo en el cuerpo y total en `X-Total-Count`**, como los listados de R11-01. Descartado: un
  sobre `{elementos, total}`, que rompe a los clientes de hoy.
- **Totales en una ruta de resumen** (`/propuestas/resumen`). Descartado: tres pedidos con
  `size=1` para leer tres totales.
- **Organización y unidad dentro de la necesidad de la propuesta.** Descartado: seguir con una
  llamada por necesidad desde el cliente (DTI-40, ítem 1).

Todas las rutas siguen bajo `/api/asignaciones`, solo para `ADMIN` (la de correr, también
`ADMIN_O_SERVICIO`), detrás de la misma ruta del gateway (`/api/asignaciones/**`, sin cambios).

### 3.1 `GET /api/asignaciones/propuestas`

| | Antes (1.9.0) | Después (1.10.0) |
|---|---|---|
| Parámetros | Ninguno | `estado` (`PENDIENTE`, `APROBADA`, `DESCARTADA`; repetible), `situacion` (`LIBRE`, `EN_CONFLICTO`, `SIN_STOCK`; repetible; solo filtra pendientes), `algoritmo` (`COMPATIBILIDAD_SEMANTICA`, `PRIORIDAD_SUBATENDIDOS`, `AMBOS`), `donacion` (uuid de la donación independiente), `orden` (`puesto` por defecto, `reciente`), `page` (desde 0), `size` (1 a 100, por defecto 20) |
| Cuerpo | Arreglo de todas las propuestas | Arreglo de la página pedida |
| Total | — | `X-Total-Count` |
| Errores | — | 400 `ERR-CSR-003` con un valor desconocido o fuera de rango, como los listados de R11-01 |

Orden:

- `puesto`: el mejor puesto de la propuesta (con `algoritmo=X`, el puesto de X), después
  `fechaCreacion` descendente, después `id`. Con `situacion=EN_CONFLICTO`, primero agrupa por la
  primera donación en conflicto, para que las que compiten salgan juntas.
- `reciente`: `fechaCreacion` descendente, después `id`.

`algoritmo=AMBOS` trae las que sugirieron los dos; `algoritmo=X`, las que sugirió X (solas o con el
otro).

Cada elemento (`PropuestaDTO`), con lo nuevo en **negrita** (todo aditivo, ningún campo cambia de
nombre ni de tipo):

```json
{
  "id": "uuid",
  "estado": "PENDIENTE",
  "fechaCreacion": "2026-11-12T00:00:03",
  "necesidad": {
    "id": "uuid", "descripcion": "Arroz", "cantidadNecesitada": 30, "tipo": "PUNTUAL",
    "estado": "PENDIENTE",
    "entidadId": "uuid", "entidadNombre": "Comedor Barrio Ejemplo", "unidad": "kg"
  },
  "fragmentaciones": [
    {
      "donacionIndependiente": { "...": "igual que hoy" },
      "cantidadNecesaria": 25,
      "disponible": 25,
      "pedidaEnOtras": 25
    }
  ],
  "sugeridaPor": [
    { "algoritmo": "PRIORIDAD_SUBATENDIDOS", "puesto": 1, "deCuantas": 6, "eligioLasDonaciones": true },
    { "algoritmo": "COMPATIBILIDAD_SEMANTICA", "puesto": 3, "deCuantas": 10, "eligioLasDonaciones": false }
  ],
  "situacion": "EN_CONFLICTO",
  "otrasPendientesMismaNecesidad": 0
}
```

Nuevos: `necesidad.entidadId`, `necesidad.entidadNombre`, `necesidad.unidad`,
`fragmentaciones[].disponible`, `fragmentaciones[].pedidaEnOtras`, `sugeridaPor`, `situacion`,
`otrasPendientesMismaNecesidad`.

Reglas de los campos nuevos:

| Campo | Regla |
|---|---|
| `sugeridaPor` | Una entrada por algoritmo. Vacío (`[]`, nunca null) en las propuestas creadas antes de `V24`. Exactamente una con `eligioLasDonaciones: true` cuando no está vacío |
| `puesto`, `deCuantas` | `1 ≤ puesto ≤ deCuantas ≤ 10` (el tope de `AlgoritmoAsignacion`) |
| `disponible` | Cantidad de esa donación independiente que sigue `EN_DEPOSITO`; 0 si ya no está en el depósito |
| `pedidaEnOtras` | Suma de lo que piden de esa donación las **otras** propuestas `PENDIENTE` |
| `situacion` | Solo en `PENDIENTE` (null en las decididas). `SIN_STOCK` si alguna fragmentación pide más que su `disponible`; si no, `EN_CONFLICTO` si alguna pide más que `disponible - pedidaEnOtras` o si `otrasPendientesMismaNecesidad > 0`; si no, `LIBRE` |
| `otrasPendientesMismaNecesidad` | Otras `PENDIENTE` con la misma `necesidad.id` |

Todo se calcula en la base para la página pedida (lectura por lote, sin una consulta por fila),
como pide el ADR de [SQL nativo y lectura por lote](../../adr/donaciones-service/20261111-consultas-de-listado-en-sql-nativo-y-lectura-por-lote.md).

### 3.2 `GET /api/asignaciones/propuestas/resumen` (nuevo)

Sin parámetros. Responde 200:

```json
{ "pendientes": 11, "libres": 8, "enConflicto": 2, "sinStock": 1 }
```

`pendientes = libres + enConflicto + sinStock`, con la misma regla de `situacion`. Son conteos de la
base, no el largo de una lista. La ruta literal `/propuestas/resumen` se declara antes de
`/propuestas/{id}/estado`; no choca porque esa termina en `/estado`.

### 3.3 `GET /api/asignaciones/donaciones` (nuevo)

Las donaciones independientes que aparecen en al menos una propuesta `PENDIENTE`. Parámetros:
`categoriaId`, `soloEnConflicto` (booleano), `page`, `size` (como 3.1). Total en `X-Total-Count`.
Orden: primero las que no alcanzan, después las que más días llevan en el depósito, después `id`.

```json
[
  {
    "donacionIndependiente": { "...": "el mismo DTO de /donaciones-independientes" },
    "disponible": 25,
    "pedidaEnPendientes": 50,
    "propuestasPendientes": 2,
    "alcanza": false
  }
]
```

El detalle de una donación (paso 2 de Destino) es `GET /api/asignaciones/propuestas?estado=PENDIENTE&donacion=<id>`.

### 3.4 `GET /api/asignaciones/ejecuciones`

| | Antes | Después |
|---|---|---|
| Parámetros | Ninguno | `page`, `size` (como 3.1) |
| Cuerpo | Todas, en el orden en que se guardaron | La página, de la más reciente a la más vieja |
| Total | — | `X-Total-Count` |

`EjecucionAsignacionDTO` no cambia. La pantalla pide `page=0&size=1`.

### 3.5 Sin cambios

- `POST /api/asignaciones/ejecuciones`: sigue sin parámetros y responde 201 con las propuestas que
  generó (a lo sumo 20, una por algoritmo y necesidad); cada una ya trae los campos nuevos de 3.1.
- `PUT /api/asignaciones/propuestas/{id}/estado`: mismo cuerpo, mismas respuestas y mismos 409.

### 3.6 Cliente liviano (contrato con la pantalla)

| Dirección | Qué pide a Donaciones |
|---|---|
| `/admin-asignaciones` o `?pestana=propuestas[&algoritmo=][&page=]` | `propuestas?estado=PENDIENTE&situacion=LIBRE&size=10…`, `propuestas/resumen`, `ejecuciones?page=0&size=1`, `propuestas?estado=APROBADA&estado=DESCARTADA&orden=reciente&size=3` |
| `?pestana=conflictos[&page=]` | `propuestas?estado=PENDIENTE&situacion=EN_CONFLICTO&situacion=SIN_STOCK&size=10…`, `propuestas/resumen`, `ejecuciones?page=0&size=1` |
| `?pestana=destino[&categoria=][&page=]` | `asignaciones/donaciones?size=10…`, `propuestas/resumen`, `ejecuciones?page=0&size=1` |
| `?pestana=destino&donacion=<id>` | `propuestas?estado=PENDIENTE&donacion=<id>&size=20`, `propuestas/resumen`, `ejecuciones?page=0&size=1` |
| `?vista=decididas[&page=]` | `propuestas?estado=APROBADA&estado=DESCARTADA&orden=reciente&size=20…` |
| Aprobar y descartar | `PUT …/propuestas/{id}/estado`; el nombre de la organización para el aviso viaja en el formulario (oculto) o sale de `GET propuestas?…` de la página de vuelta, nunca de la lista completa |

Las pantallas de resultado de la maqueta (`-aprobada`, `-descartada`, `-error`, `-corrida`) se
conservan, con sus escenarios (`vacio`, `lento`, `error`, `corriendo`, `nuevas`, `propia`, `cambio`).
`?variante=asignaciones-*` deja de existir.

## 4. Invariantes y compatibilidad con los clientes actuales

Invariantes:

- Aprobar y descartar no cambian: misma regla, misma atomicidad, mismos 409.
- Ningún campo de hoy de `PropuestaDTO` cambia de nombre, de tipo ni de significado.
- Ninguna ruta de `/api/asignaciones` devuelve una lista sin tope (máximo 100 por página).
- Una propuesta no pierde sus sugerencias al guardarse otra vez (aprobar o descartar).
- La regla de consolidación del gestor (H1 de la ficha) no cambia: solo se registra de quién vino.

Compatibilidad:


| Cliente | Qué usa | Efecto | Qué hace este lote |
|---|---|---|---|
| Cliente liviano, Asignaciones | `propuestas()`, `ejecuciones()`, `necesidad(id)`, `entidad(id)` | Pasa a páginas y totales | Lo migra (§3.6) |
| Cliente liviano, inicio del admin (`AdminPersonasInicio`) | `propuestas()` para filtrar pendientes (DTI-40 ítem 1) | Con la página por defecto vería solo 20 | Lo cambia `p1-admin-inicio` (tanda 1, antes que este) al `propuestasPendientes` del resumen. Si al integrar todavía llama a `propuestas()`, el integrador lo pasa a `propuestas?estado=PENDIENTE&size=3` |
| Seeder de demo (`demo/seeder/seeder/vivo.py:124-126`) | `GET /api/asignaciones/propuestas` y filtra `PENDIENTE` en Python | Con la página por defecto vería solo las primeras 20: puede no encontrar las de su corrida | Cambio de una línea fuera de zona, imprescindible: `?estado=PENDIENTE&orden=reciente&size=100` (las de una corrida son a lo sumo 20). Lo hace el constructor y lo declara |
| Tests de integración (`integration-tests/.../DonacionesApiClient.java:147,155`) | `POST /ejecuciones` y `PUT …/estado` | Ninguno: esas rutas no cambian | Nada |
| Colecciones de Postman (`postman/flujo-4`, `flujo-8`, `flujo-9`) | `GET /propuestas` sin parámetros | Reciben la página 0 de 20; en esos flujos hay menos | Nada; se anota en el cambio de OpenAPI |
| Quien lea el JSON de la propuesta | Los campos de hoy | Solo se suman campos | Nada |

## 5. Criterios de Aceptación

Los de Donaciones con PostgreSQL (Testcontainers, como `AprobacionPropuestaTransaccionalJpaTest`);
los del cliente con MockMvc y el doble de `DonacionesApi`; los visuales con `-Pvisual` (360, 1280 y
1920 px, axe sin fallas serias).

```gherkin
Escenario A1: Puesto de un solo algoritmo
  Dado necesidades que solo propone Compatibilidad semántica, 4 en su lista
  Cuando se corre la asignación
  Entonces cada propuesta guarda una sugerencia COMPATIBILIDAD_SEMANTICA
  Y sus puestos son 1, 2, 3 y 4, con deCuantas 4

Escenario A2: Los dos sugieren la misma necesidad
  Dado que los dos algoritmos proponen para la necesidad N
  Cuando se corre la asignación
  Entonces la propuesta de N lleva dos sugerencias, con el puesto de cada algoritmo
  Y solo la de PRIORIDAD_SUBATENDIDOS tiene eligioLasDonaciones en true

Escenario A3: Las sugerencias se guardan
  Dado una propuesta con dos sugerencias guardada con V24
  Cuando se lee de nuevo, o se aprueba y se lee
  Entonces devuelve las dos sugerencias iguales
  Y una propuesta creada antes de V24 devuelve sugeridaPor []

Escenario A4: Página por defecto y errores
  Dado 45 propuestas
  Cuando se pide GET /api/asignaciones/propuestas sin parámetros
  Entonces responde 200 con 20 y X-Total-Count 45
  Y size=101 o situacion=OTRA responden 400 con ERR-CSR-003

Escenario A5: Situación de cada pendiente
  Dado una donación de 25 kg y dos pendientes que piden 25 kg cada una
  Cuando se piden las propuestas
  Entonces las dos salen EN_CONFLICTO
  Y al aprobar una, la otra sale SIN_STOCK
  Y una sola pendiente que pide 10 de 25 sale LIBRE
  Y dos pendientes de la misma necesidad salen EN_CONFLICTO

Escenario A6: Filtros en la base
  Dado pendientes de los dos algoritmos y varias donaciones
  Cuando se filtra por situacion, algoritmo o donacion
  Entonces la página y X-Total-Count respetan el filtro

Escenario A7: Sin una consulta por fila
  Dado 10 propuestas con 3 fragmentaciones cada una
  Cuando se pide una página de 10
  Entonces la cantidad de sentencias SQL es la misma que con 1 propuesta

Escenario A8: Resumen coherente
  Dado pendientes en las tres situaciones
  Cuando se pide GET /api/asignaciones/propuestas/resumen
  Entonces pendientes = libres + enConflicto + sinStock
  Y cada cifra coincide con X-Total-Count de GET /propuestas filtrado por esa situación

Escenario A9: Donaciones en juego
  Dado una donación en dos pendientes que no alcanza y otra en una que alcanza
  Cuando se pide GET /api/asignaciones/donaciones
  Entonces trae solo esas dos, primero la que no alcanza

Escenario A10: Última ejecución
  Dado tres ejecuciones
  Cuando se pide GET /api/asignaciones/ejecuciones?page=0&size=1
  Entonces trae la más reciente y X-Total-Count 3

Escenario A11: El cliente no pide listas completas
  Dado la pantalla Asignar donaciones en cualquiera de sus pestañas
  Cuando se arma la página
  Entonces el cliente no llama a propuestas ni a ejecuciones sin página
  Y no llama a necesidad(id) ni a entidad(id) por propuesta

Escenario A12: Sugerida por
  Dado una propuesta sugerida por los dos algoritmos y otra sin sugerencias
  Cuando se muestra la tarjeta
  Entonces la primera dice "Parecido con lo pedido · 3.º de 10" y "Quienes menos recibieron · 1.º de 6"
  Y la nota "Las donaciones son las que eligió Quienes menos recibieron."
  Y la segunda dice "Sin el dato del algoritmo: se generó antes de que lo guardáramos."

Escenario A13: Pestañas sin JavaScript
  Dado el navegador sin JavaScript
  Cuando se toca la pestaña Conflictos
  Entonces se carga /admin-asignaciones?pestana=conflictos con aria-current="page" en esa pestaña
  Y las cifras de las pestañas son las del resumen

Escenario A14: Ya no alcanza
  Dado una propuesta SIN_STOCK
  Cuando se muestra en Conflictos
  Entonces no tiene Aprobar, sí Descartar y la franja "Ya no alcanza"

Escenario A15: Textos de la ficha
  Dado cada estado de las secciones 5 y 6 de la ficha
  Cuando se muestra
  Entonces el texto es el de la ficha, con "organización"

Escenario A16: Sin variantes
  Dado el árbol del cliente al cerrar el lote
  Cuando se busca variantes/asignaciones o variante=asignaciones
  Entonces no aparece ninguna plantilla, hoja, script ni ruta de paginas.txt

Escenario A17: Contrato
  Dado OpenAPI de Donaciones 1.10.0
  Cuando corre oasdiff en el job de contratos del PR
  Entonces solo marca el cambio de comportamiento de 3.1 y 3.4
  Y ese cambio está en cambios-incompatibles-aceptados.txt

Escenario A18: Gates
  Dado el lote terminado
  Cuando se corre mvn clean test -pl donaciones-service -am y -pl cliente-liviano -am, y spotless:check
  Entonces todo pasa
```

## 6. Riesgos

| Riesgo | Qué se hace |
|---|---|
| `[INFERRED]` La situación de cada propuesta depende de todas las pendientes: si se calcula en Java sobre la lista entera, vuelve el problema de R11-01 | Consulta con agregados sobre `posible_fragmentacion` y `propuesta` pendientes, en la base (A7) |
| `[INFERRED]` Aprobar una propuesta `SIN_STOCK` desde otro cliente asigna lo que quede de la donación, que puede ser menos de lo propuesto (`PosibleFragmentacion.confirmar` asigna la donación entera si no supera lo pedido) | La pantalla no ofrece Aprobar; la regla en el servidor es la pregunta 2 de la ficha |
| Migraciones de otros lotes de esta corrida (`p2-admin-bandeja` guarda el motivo; `p1-admins`, el autor) pueden tomar también `V22` | Si choca al integrar, la renumera el integrador |
| `p1-admins` toca `PropuestaDeAsignacionService` (quién aprueba) en la tanda 1 | Este lote no cambia `actualizarEstado`; el choque se limita a imports y constructor |
| Una página de Conflictos puede cortar un grupo de propuestas que compiten | El orden las agrupa y la franja dice con cuántas compite; "Comparar en Destino" muestra el grupo entero |
| `[OBSERVED]` Las ejecuciones viven en memoria: tras un reinicio, el panel dice que nunca corrió | Se acepta; pregunta 3 de la ficha. El paginado se hace igual sobre la lista en memoria |
| Pedir varias cosas por pantalla (página, resumen, última ejecución, decididas) suma llamadas | Son 3 o 4 llamadas fijas por visita, todas acotadas, contra las N+2 de hoy |

## 7. Especificación Técnica de Diseño (Technical Design): ADR, plan de tests y archivos

### Archivos

Del lote: `donaciones-service` (propuestas de asignación: dominio, algoritmos, gestor, servicio,
mapper, controller, DTOs, adaptador JPA, `V24__sugerencias_de_propuesta.sql`, tests);
`cliente-liviano`: `templates/admin-asignaciones.html`, `templates/variantes/asignaciones-*.html`
(se borran), `web/AdminAsignacionesController.java`, la parte de asignaciones de
`rest/AdminOperacionRest.java` y su hoja de estilo.

Fuera de zona, imprescindibles (el constructor los declara): `rest/DonacionesApi.java` (records y
parámetros), `vistas/AdminAsignacionesVista.java`, `puertos/AdminOperacionPuerto.java`,
`maqueta/AdminOperacionMaqueta.java`, `web/AdminAsignacionesEnvios.java`,
`templates/variantes/comun/asignaciones-datos.html`, `static/css/variantes/asignaciones-*.css` y
`variantes/comun/asignaciones.css`, `static/js/asignaciones.js` (se borran),
`src/test/resources/verificacion/paginas.txt`, sus tests, `docs/arquitectura/contratos/openapi-donaciones.yaml`,
`cambios-incompatibles-aceptados.txt` y la línea del seeder (§4).

### ADR (Two-Gate, AGENTS.md §9.1)

No hace falta uno nuevo (AGENTS.md §9.1). **Gate A:** el paginado y los totales aplican la decisión
ya tomada en el ADR `proposed` de listados paginados; los campos nuevos son una evolución aditiva
(AGENTS.md §8.2, *feature*); guardar quién sugirió la propuesta es un dato más del agregado `Propuesta`, en una
tabla hija, sin cambiar la estrategia de persistencia ni los límites entre servicios. El cambio de
comportamiento de 3.1 y 3.4 (sin página, ya no todas) es el mismo que ese ADR aceptó para los otros
listados y se anota igual en `cambios-incompatibles-aceptados.txt`. Si el revisor entiende que la
regla de "situación" es una decisión de negocio nueva, se escribe un ADR corto `proposed` antes de
construir.

### Plan de tests (primero el test, después el código)

1. Gestor y algoritmos con dobles: puesto, total y las dos sugerencias (A1, A2).
2. Adaptador JPA y `V24` sobre PostgreSQL con datos de antes (A3).
3. Consultas de listado, situación, filtros, resumen y donaciones en juego sobre PostgreSQL (A4 a
   A9), con el conteo de sentencias (A7).
4. Controller con MockMvc: parámetros, header, 400 y permisos (A4, A10).
5. Cliente: `AdminOperacionRest` con el doble de `DonacionesApi` (A11); plantillas con MockMvc
   (A12 a A15); `-Pvisual` sobre `admin-asignaciones` y sus pestañas.
6. Contrato: OpenAPI 1.10.0, oasdiff y `ContratoDeLasApisTest` del cliente (A17).

Las rutas, parámetros y campos de §3 son el contrato; las clases y consultas las decide el
constructor siguiendo los adaptadores que ya existen.


## Aprobación del diseño p1-asignaciones (2026-11-12)

Aprobado por el usuario de forma automática: pidió que, en P1 y P2, cada duda se resuelva con la opción recomendada por el diseñador, sin parada. Cada duda queda resuelta así:

1. ¿Sumamos «Aprobar todas las de esta página» en Propuestas? Recomiendo: todavía no. Primero hay que probar la pantalla nueva en la demo y, si aprobar de a una cansa, sumarlo con su aviso de resultado parcial. **Resuelto: la opción recomendada.**
2. ¿Donaciones también tiene que rechazar la aprobación de una propuesta que ya no alcanza? Hoy solo la pantalla esconde el botón, y otro cliente podría asignar menos de lo propuesto. Recomiendo: sí, en un lote corto después de este, porque cambia la regla de aprobación y su error. **Resuelto: la opción recomendada.**
3. ¿Guardamos las ejecuciones en la base? Hoy viven en memoria y después de un reinicio «Última asignación» dice que nunca corrió. Recomiendo: sí, pero en el backlog; R11-24 no lo pide y es otra tabla. **Resuelto: la opción recomendada.**
4. El seeder (demo/seeder/seeder/vivo.py:124-126) pide GET /api/asignaciones/propuestas sin parámetros. Con la página por defecto de 20 podría no encontrar las propuestas de su corrida. Recomiendo: que el constructor cambie esa línea a ?estado=PENDIENTE&orden=reciente&size=100 y lo declare como fuera de zona. La regla de no tocar el sistema desde el seeder no aplica, porque acá cambia el sistema y el seeder solo se adapta. **Resuelto: la opción recomendada.**
5. Si p1-admin-inicio (tanda 1) no llegó a cambiar AdminPersonasInicio para que use propuestasPendientes del resumen, ese inicio vería solo 20 propuestas. Recomiendo: que el integrador lo pase a propuestas?estado=PENDIENTE&size=3 si hace falta. **Resuelto: la opción recomendada.**
6. V22 puede chocar con las migraciones de p2-admin-bandeja (el motivo del descarte) o de p1-admins. Recomiendo: que la renumere el integrador, como dice la regla de la corrida. **Resuelto: la opción recomendada.**

## Cierre (2026-11-12): revisión y lo que quedó

Resumen del cierre de la oleada P1 para el lote `p1-asignaciones` (`1557422c1`). La revisión la hizo
el revisor del workflow `backlog-r11`, separado del constructor (`[INFERRED]` por cómo lo orquesta
el workflow: lee la rama y los informes, no el razonamiento del constructor). Este bloque lo
redactó el agente del cierre documental con lo que consta en los commits y en
[DTI-42](../../adr/DEUDA_TECNICA.md#dti-42), ítem 12, donde está todo lo pendiente.

```text
=== REVIEW CONTRACT ===

Mode: INDEPENDENT_REVIEW (revisión de integración de la oleada P1 del workflow backlog-r11)
Task Level: ARCHITECTURAL

V1 Regression risks:
  [OBSERVED] Sin página, /propuestas y /ejecuciones devuelven 20: el seeder y tres colecciones de
  Postman piden `orden=reciente&size=100` (cambios-incompatibles-aceptados.txt).

V2 Architecture/invariant violations:
  NONE_DETECTED en el cierre. ADR `proposed` escrito después del código (desvío acordado).

V3 Contracts/integration:
  [OBSERVED] Donaciones no rechaza aprobar una propuesta SIN_STOCK: la protege solo la pantalla.

V4 Tests/coverage:
  [VERIFIED] Reactor en verde en el cierre (common-lib, Donaciones, Notificaciones, Logística,
  cliente liviano y gateway), auth-service 34/34, spotless:check en verde, Gate 3 30/30 en el
  stack propio y verificación visual de 18 pantallas en verde (HEAD 0edf2811d).
  Lo que no tiene test, en DTI-42 ítem 12.

V5 Scope violations:
  NONE_DETECTED.

V6 Evidence integrity:
  NONE_DETECTED.

V7 SonarCloud pre-flight:
  [TESTS_NOT_EXECUTED_BY_EVALUATOR] No consta; lo mide el Quality Gate del PR.

V8 Security/privacy:
  NONE_DETECTED. Mismos permisos de ADMIN.

V9 Documentation graph integrity:
  [OBSERVED] Spec movida a completed/ con README, ESTADO_DOCUMENTACION, specs/README y
  DEUDA_TECNICA (DTI-42) en el cierre documental. Gate 4 sin corrida registrada (DTI-42 ítem 15).
```
