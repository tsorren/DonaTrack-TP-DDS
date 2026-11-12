# Spec: R11-p2-listas-restantes — Las listas que faltan

> **Aprobada el 2026-11-12** con las opciones recomendadas (aprobación automática pedida por el usuario para P1 y P2; ver "Aprobación del diseño" al final).
> **Estado:** PROPOSED (2026-11-12). Espera la aprobación del usuario antes de construir; ningún ADR
> se promueve.
> **Nivel:** STANDARD por su forma (endpoints que ya existen ganan parámetros y uno nuevo de
> conteos), ruta CONTRATO del [manifiesto](../../interfaz/backlog-revision-2026-11-11.manifiesto.json):
> cambia el comportamiento de contratos públicos, así que lleva esta spec antes del código.
> **ADR:** ninguno nuevo (AGENTS.md §9.1, Gate A). Se aplica a otros servicios el patrón ya decidido
> en los ADRs `proposed` de [listados siempre paginados](../../adr/donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
> y de [cifras como conteos](../../adr/donaciones-service/20261111-cifras-del-admin-como-conteos-en-un-resumen.md):
> `page`, `size`, total en `X-Total-Count` y las cifras como números.
> **Fecha:** 2026-11-12
> **Base:** `e9331b3a2`.
> **Módulos:** `logistica-service`, `incentivos-service`, `donaciones-service`, `cliente-liviano`.
> **Ítem:** R11-43 del [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md),
> lote `p2-listas-restantes`. Salda [DTI-39](../../adr/DEUDA_TECNICA.md#dti-39) ítem 1 (la parte de
> la organización) e ítem 2, y la parte de seguimientos e historial del ranking de
> [DTI-40](../../adr/DEUDA_TECNICA.md#dti-40) ítem 3.
> **Pantallas a comprobar:** `entidad-seguimiento`, `admin-ranking`, `entidad-recibidas`,
> `entidad-necesidades`, `admin-entidades`.

---

## 1. Objetivo Funcional (Goal)

Que ninguna de estas pantallas pida una lista completa (regla 1 del backlog):

- **Seguir entregas** (Logística) y el **historial del ranking** (Incentivos) se piden de a una
  página, y Logística arma cada página sin una lectura a la base por entrega.
- Las pantallas de la organización (**Recibidas, Seguir entregas, Confirmar recepción,
  Necesidades y Notificaciones**) dejan de leer "una página de 100 asignadas" para cruzar o contar:
  cada lista pide su página con el paginador `paginas`, y cada cifra es un número del servicio.
- En **Organizaciones del admin**, que ya va de a una página, vuelven los filtros Estado,
  Necesidades y Tipo, resueltos en Donaciones.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (lo que entra)

1. **Logística, los dos seguimientos.** `GET /api/entregas/seguimiento/beneficiarios/{id}` y
   `GET /api/entregas/seguimiento/donantes/{id}` paginan en la base y filtran por `estado` e
   `idDonacion` (los dos repetibles). Cada página se arma con una lectura de las rutas, una de los
   camiones y una de las entregas de esas rutas, no una por entrega ni una por parada.
2. **Incentivos, historial del ranking.** `GET /api/incentivos/ranking/historial` pagina en la base,
   del mes más reciente al más viejo.
3. **Donaciones, filtros de Organizaciones.** `GET /api/entidades` filtra por `estado` (activa o de
   baja), `conNecesidades` (con o sin necesidades vigentes) y `rubro` (el tipo que se ve en
   pantalla).
4. **Donaciones, conteos de una organización.** Endpoint nuevo
   `GET /donaciones-independientes/conteos?entidadId=` con cuántas asignadas tiene en cada estado,
   cuántas recibió en el mes y lo mismo por necesidad.
5. **Donaciones, orden "lo que hay que atender primero".** `GET /donaciones-independientes` acepta
   `orden=ATENCION`: en la puerta, con problema, en camino, por salir y recibidas, y dentro de cada
   grupo, la más reciente primero.
6. **Cliente liviano.**
   - `RecibidasRest` (Recibidas y Seguir entregas): una página de 20 con el paginador, los filtros
     de estado resueltos en Donaciones y la cantidad de cada filtro sacada de los conteos.
   - `EntidadSeguimientoPuerto.noticias`: pide la noticia solo de las donaciones de la página
     (`idDonacion` repetido), no todas las entregas de la organización.
   - `EntidadConfirmarRest`: las que esperan confirmación, de a una página de Donaciones
     (`estado=EN_PUERTA&estado=EN_TRASLADO&orden=ATENCION`); de Logística, solo las entregas de esa
     página. Confirmar busca la entrega por `idDonacion` y nada más.
   - `EntidadNecesidadesRest`: por cada necesidad vigente, como mucho 3 en curso y 3 que ya
     llegaron, y las cifras de los conteos (regla 2: resumen con "Ver todas", que lleva a Recibidas).
   - `EntidadNotificacionesRest`: de Logística, solo las entregas de las donaciones de los avisos de
     la página; ya no lee las asignadas de Donaciones.
   - `DonacionesApi.asignadas` desaparece; se suma `conteos`.
   - `RankingRest` y `admin-ranking.html`: el menú "Mes" muestra los meses de la página (12) y el
     paginador `paginas` recorre las páginas de meses.
   - `AdminPersonasEntidades` y `admin-entidades.html`: vuelven Estado, Necesidades y Tipo, con la
     cantidad de cada estado.
7. **Contratos escritos.** `openapi-logistica.yaml`, el OpenAPI de Incentivos y el de Donaciones,
   con los parámetros nuevos y `X-Total-Count`.

### Out-of-Scope (lo que no entra)

- **El inicio de la organización** (`entidad-inicio`, `EntidadInicioController`) sigue usando
  `RecibidasPuerto.recibidas(entidad)` sin página: esa firma queda como está para no romperlo,
  leyendo una página de 100 como hoy, y queda anotada en DTI-39 ítem 1.
- **Seguimiento e inicio del donante** (`DonanteSeguimientoRest`, `DonanteInicioRest`): no son de
  este lote. Para que no pierdan entregas cuando Logística empiece a mandar 20 por defecto, el
  cliente pide la del donante con `size=100`, el mismo tope que ya tiene su lista de Donaciones
  (DTI-39 ítem 1).
- **Las necesidades vigentes de Organizaciones del admin** se siguen leyendo con
  `GET /api/necesidades?estado=VIGENTE` sin página; `NecesidadesController` es de DTI-40 ítem 2.
- **Los puestos de un mes del ranking** llegan todos dentro de cada mes; no se paginan acá.
- `GET /api/notificaciones` general, la cuenta de camiones por estado y las mediciones C3 y C9 de
  DTI-40: el backlog las nombra para este lote, pero no están en los archivos del encargo (ver las
  dudas del informe de diseño).
- Renombrar rutas, clases o campos por el cambio de "entidad" a "Organización".
- Migraciones: ninguna. Los índices que hacen falta ya existen (`idx_entrega_beneficiario`,
  `idx_entrega_donante`, `idx_entrega_ruta`, `idx_donacion_independiente_beneficiario`,
  `idx_donacion_independiente_estado`, `idx_donacion_independiente_destino`). Si la verificación
  muestra que falta uno, va con el número siguiente de su servicio (Donaciones `V25`, Logística
  `V11`, Incentivos `V5`).

## 3. Decisiones Acordadas y Trade-offs

Sin matriz 5D (lo pide el lote). Cada decisión, con la alternativa descartada:

1. **El orden de "lo que hay que atender" lo resuelve Donaciones** (`orden=ATENCION`). Sin eso, la
   página 1 podría no tener las que esperan en la puerta. Dentro de cada grupo ordena por fecha de
   registro, no por la fecha del último cambio como hoy en memoria: evita una subconsulta al
   historial en cada página. Descartado: ordenar en el cliente (no sirve con páginas) y ordenar
   por el último cambio (más caro, sin un índice nuevo).
2. **Recibidas muestra todas las recibidas, de a una página**, con "N este mes" desde
   `entregadasEnElMes`. Hoy esconde las de meses anteriores porque leía todo; con página ya no hace
   falta, y filtrar "recibidas este mes" junto con los otros estados pediría un filtro nuevo por
   fecha de cambio. Descartado: un parámetro `entregadaEn=AAAA-MM`.
3. **Pendientes de Organizaciones salen de Keycloak, como hoy.** Con `ver=pendientes` no se pide la
   lista a Donaciones; con `activas` o `inactivas`, se pide con `estado=ACTIVA` o `BAJA` y no se
   muestran las pendientes. Necesidades y Tipo se aplican sobre las pendientes en el cliente,
   porque esa lista ya llega entera de Keycloak (DTI-42 ítem 8). La cantidad de cada estado no
   depende de la búsqueda ni de los otros filtros, como en la maqueta.
4. **Tipo es el rubro de la jurídica**, con la misma lista de opciones que ya tiene la pantalla
   (Comedor, Merendero, Escuela, Hogar, Club, Biblioteca, Centro comunitario). Una organización sin
   rubro no aparece con ningún tipo elegido. Descartado: un endpoint con los rubros que existen
   (no hace falta para la demo).
5. **El ranking va de a 12 meses** (un año), y el menú "Mes" muestra los de la página. Descartado:
   20 por defecto, que llenaría el menú.
6. **Notificaciones deja de cruzar con las asignadas**: Logística ya filtra por la organización, y
   una donación reasignada tiene su entrega cancelada, que el filtro de estado deja afuera.
7. **Seguimiento del donante con `size=100`** en `LogisticaApi`, hasta que su lote lo pagine.

## 4. Invariantes

- Ninguna llamada nueva devuelve una lista completa: sin `page` ni `size`, la primera página; el
  tope es 100.
- Los permisos de cada ruta no cambian; `conteos` usa los mismos que el listado de donaciones.
- El cuerpo de cada elemento no cambia: los cambios de contrato son aditivos (parámetros opcionales,
  encabezado `X-Total-Count`, un endpoint nuevo).
- Las cifras de pantalla salen de un conteo del servicio, nunca del largo de una página.
- El paginador es el componente `paginas` y conserva búsqueda y filtros (regla 3).
- No se renombra ninguna ruta, clase ni campo por "Organización".

## 5. Criterios de Aceptación

```gherkin
Escenario: Seguir entregas de a una página
  Dado una organización con 45 entregas en Logística
  Cuando el cliente pide su seguimiento sin page ni size
  Entonces recibe 20 seguimientos y X-Total-Count vale 45
  Y Logística leyó rutas, camiones y paradas una vez cada uno

Escenario: Lo que espera en la puerta sale primero
  Dado una organización con 30 donaciones en camino y 2 en la puerta
  Cuando abre Recibidas
  Entonces la página 1 empieza por las 2 en la puerta
  Y el filtro "Esperan tu confirmación" dice 2, contado por Donaciones

Escenario: El ranking por años
  Dado 30 meses calculados en Incentivos
  Cuando el admin abre Ranking
  Entonces el menú Mes muestra los 12 más recientes y el paginador ofrece 3 páginas

Escenario: Los filtros de Organizaciones
  Dado 46 organizaciones activas y 3 de baja
  Cuando el admin elige Estado "Inactivas"
  Entonces la lista muestra las 3 de baja, pedidas con estado=BAJA
```

| # | Criterio | Cómo se verifica |
|---|---|---|
| C1 | Los dos seguimientos sin parámetros devuelven 20 como mucho y `X-Total-Count`; `size=101` y `page=-1` dan 400. | Test del controller de Logística. |
| C2 | `estado` e `idDonacion` filtran en la base; el seguimiento del donante sin `estado` devuelve solo las tres activas. | Test del repositorio con base (Testcontainers o el de la casa) y del servicio. |
| C3 | Armar una página de 20 seguimientos lee rutas, camiones y entregas de las rutas una vez cada uno, no una vez por entrega. | Test del servicio que cuenta las llamadas a los repositorios con dobles. |
| C4 | El historial del ranking sin parámetros trae los 12 meses más recientes, del más nuevo al más viejo, con el total de meses. | Test del controller y del repositorio de Incentivos. |
| C5 | `GET /api/entidades?estado=BAJA` trae solo las de baja; `conNecesidades=true`, solo las que tienen una necesidad vigente y no vencida hoy; `rubro=centro comunitario` encuentra "Centro Comunitario"; el total respeta los filtros. | Test del repositorio de Donaciones con base. |
| C6 | `conteos` trae los 8 estados siempre, `entregadasEnElMes` y `porNecesidad`; otra organización recibe 403; sin `entidadId`, 400. | Tests del controller (permisos) y del servicio. |
| C7 | `orden=ATENCION` devuelve primero `EN_PUERTA`, después las con problema, en camino, por salir y recibidas. | Test del repositorio con base. |
| C8 | Ninguna clase del cliente llama a `DonacionesApi.asignadas` (ya no existe) ni a un seguimiento de la organización sin `idDonacion`. | `grep` y tests de los adaptadores con `MockRestServiceServer` que esperan las URLs exactas. |
| C9 | Recibidas, Seguir entregas y Confirmar recepción muestran el paginador `paginas` cuando hay más de 20, y cambiar de página conserva los filtros. | Tests de los controllers y verificación visual. |
| C10 | En Organizaciones, los tres filtros se ofrecen fuera de la maqueta, cada estado con su cantidad, y "Sacar los filtros" vuelve a todas. | Test de `AdminPersonasEntidades` y verificación visual. |
| C11 | Las cinco pantallas pasan `-Pvisual` (axe, toque y desborde) en 360, 1280 y 1920 px. | `-Pvisual` sobre `entidad-seguimiento`, `admin-ranking`, `entidad-recibidas`, `entidad-necesidades` y `admin-entidades`. |
| C12 | `mvn clean test` de los cuatro módulos y `spotless:check`, en verde; los OpenAPI escritos coinciden con el código. | Gate 2 y el job de contratos del PR. |

## 6. Contrato antes y después

Las reglas comunes son las de los listados de P0: sin `page` ni `size`, la página 0 de 20; `size`
de 1 a 100; un valor fuera de rango da 400 con `ERR-CSR-003`; el total, en `X-Total-Count`. El
cuerpo de cada elemento no cambia.

### 6.1 Logística

| Ruta | Antes | Después |
|---|---|---|
| `GET /api/entregas/seguimiento/beneficiarios/{idBeneficiaria}` | Sin parámetros. Todas las entregas de la organización, en cualquier estado. Lista de `SeguimientoEntregaResponseDTO`. | `page`, `size`, `estado` (repetible, `EstadoEntrega`), `idDonacion` (repetible, hasta 100). Una página; total en `X-Total-Count`. Orden: las que no llegaron primero, después por llegada y salida, de la más reciente a la más vieja, desempate por id. |
| `GET /api/entregas/seguimiento/donantes/{donanteId}` | Sin parámetros. Las activas del donante (`PENDIENTE`, `EN_TRASLADO`, `EN_PUERTA`). | Mismos parámetros. Sin `estado`, las mismas tres activas de hoy; con `estado`, solo esas. Una página y `X-Total-Count`. |

Permisos: los de hoy (`VER_EL_SEGUIMIENTO_DE_SU_BENEFICIARIA`, `VER_EL_SEGUIMIENTO_DE_SU_DONANTE`).
Un `estado` que no existe da 400.

### 6.2 Incentivos

| Ruta | Antes | Después |
|---|---|---|
| `GET /api/incentivos/ranking/historial` | Sin parámetros. Todos los meses calculados, sin orden fijo. Lista de `RankingMensualDTO`. | `page`, `size` (sin ellos, la página 0 de **12**: un año; hasta 100). Del mes más reciente al más viejo. Total de meses en `X-Total-Count`. |

`GET /ranking/{periodo}` y `/ultimo` no cambian. La pantalla usa `/{periodo}` cuando el mes pedido
en `?mes=` no está en la página.

### 6.3 Donaciones

| Ruta | Antes | Después |
|---|---|---|
| `GET /api/entidades` | `nombre`, `page`, `size`. Activas y de baja. | Además: `estado` = `ACTIVA` o `BAJA` (sin él, las dos); `conNecesidades` = `true` (con alguna necesidad vigente y no vencida hoy) o `false` (sin ninguna); `rubro`, igual al rubro de la jurídica sin distinguir mayúsculas ni tildes. Un `estado` desconocido da 400. |
| `GET /donaciones-independientes` | `orden` = `RECIENTES` o `ANTIGUAS`. | Además `orden=ATENCION`: `EN_PUERTA`; `ENTREGA_FALLIDA` y `VENCIDA`; `EN_TRASLADO`; `ASIGNACION_REALIZADA`, `LISTA_PARA_ENTREGAR` y `EN_DEPOSITO`; `ENTREGADA`. Dentro de cada grupo, la más reciente primero; desempate por id. El resto de los filtros no cambia (`estado` repetible, `entidadId` y `necesidadId` ya existen). |
| `GET /donaciones-independientes/conteos` (nuevo) | — | `entidadId` (obligatorio), `necesidadId` (opcional). Mismos permisos que el listado: ADMIN, o la organización sobre sí misma con `VER_DONACIONES`. Respuesta abajo. |

Respuesta de `conteos`:

```json
{
  "porEstado": [
    { "estado": "EN_DEPOSITO", "cantidad": 0 },
    { "estado": "ASIGNACION_REALIZADA", "cantidad": 3 },
    { "estado": "EN_TRASLADO", "cantidad": 1 },
    { "estado": "EN_PUERTA", "cantidad": 2 },
    { "estado": "LISTA_PARA_ENTREGAR", "cantidad": 0 },
    { "estado": "ENTREGADA", "cantidad": 41 },
    { "estado": "ENTREGA_FALLIDA", "cantidad": 1 },
    { "estado": "VENCIDA", "cantidad": 0 }
  ],
  "entregadasEnElMes": { "mes": "2026-11", "cantidad": 5 },
  "porNecesidad": [
    { "necesidadId": "…", "porEstado": [ { "estado": "EN_PUERTA", "cantidad": 1 } ] }
  ]
}
```

- `porEstado` trae siempre los 8 estados, en el orden del ciclo de vida, con 0 si no hay (igual que
  el resumen del admin).
- `entregadasEnElMes` cuenta las que pasaron a `ENTREGADA` en el mes en curso (reloj de Buenos
  Aires), como el resumen del admin.
- `porNecesidad` trae solo las necesidades con alguna asignada, y en cada una solo los estados con
  alguna. Con `necesidadId`, todo se cuenta sobre esa necesidad.

### 6.4 Lo que pide cada pantalla

| Pantalla | Antes | Después |
|---|---|---|
| Recibidas | 1 página de 100 asignadas; filtra, agrupa y cuenta en memoria; las recibidas, solo las del mes. | 1 página de 20 (`entidadId`, `estado` según los filtros elegidos, `orden=ATENCION`) + 1 conteo. El paginador conserva los filtros. |
| Seguir entregas | 1 página de 100 asignadas + todas las entregas de Logística. | 1 página de 20 de las que no llegaron + 1 conteo (las cifras por estado) + 1 seguimiento de Logística con los `idDonacion` de la página. |
| Confirmar recepción | Todas las entregas de Logística + 1 página de 100 asignadas. | 1 página de 20 de Donaciones (`EN_PUERTA`, `EN_TRASLADO`) + 1 seguimiento con sus `idDonacion`. Confirmar: 1 seguimiento con `idDonacion=clave`. |
| Necesidades | Las necesidades + 1 página de 100 asignadas agrupadas por necesidad. | Las necesidades + 1 conteo (con `porNecesidad`) + por cada vigente, 2 páginas de 3 (en curso y recibidas, con `necesidadId`). |
| Notificaciones | 1 página de avisos + todas las entregas + 1 página de 100 asignadas. | 1 página de avisos + 1 seguimiento con los `idDonacion` de los avisos de entrega en camino. |
| Ranking | Todo el historial. | 1 página de 12 meses (+ `/{periodo}` si el mes pedido está en otra página). |
| Organizaciones | 1 página de 20 + la cifra de activas. | 1 página de 20 con `estado`, `conNecesidades` y `rubro` + 2 conteos de tamaño 1 (activas y de baja). |

## 7. Compatibilidad con los clientes actuales

- **Cambio de comportamiento que hay que aceptar.** Quien hoy llama a los dos seguimientos o al
  historial sin parámetros recibía todo; después recibe la primera página (20, o 12 meses). Los
  parámetros y el encabezado son nuevos y opcionales: oasdiff no debería marcarlo como incompatible,
  pero se anota en `cambios-incompatibles-aceptados.txt` si el job lo marca.
- **Cliente liviano:** todas sus llamadas cambian en este lote, salvo el seguimiento del donante, que
  pasa a pedir `size=100` (decisión 7 de §3), y `recibidas(entidad)` del inicio de la organización, que
  queda con su página de 100.
- **Seeder de demo, integration-tests y gateway:** se revisa con `grep` que ninguno lea estas rutas
  esperando todo; el gateway no cambia (las rutas son las mismas). `SeguridadLogisticaTest` sigue
  valiendo: los permisos no cambian.
- **`GET /donaciones-independientes/conteos`** convive con `/{id}`: Spring elige la ruta literal
  antes que la variable.
- **Maqueta:** las vistas de ejemplo siguen andando; las pantallas reciben los mismos objetos con la
  página y los totales.

## 8. Riesgos

| Riesgo | Qué se hace |
|---|---|
| Recibidas ordenada por fecha de registro dentro de cada grupo, no por el último cambio. | Se acepta (decisión 1 de §3). Si en la demo se nota, el siguiente paso es ordenar por el último cambio con un índice en Donaciones. |
| Necesidades hace 2 llamadas por necesidad vigente. | Con pocas vigentes por organización alcanza; si crece, un listado por necesidad agrupado en Donaciones. |
| Las cifras de la página y las del conteo pueden diferir un instante si cambia un estado entre las dos llamadas. | Se acepta: cada cifra dice lo que el servicio contó. |
| Una donación en `EN_PUERTA` en Donaciones cuya entrega en Logística ya cambió. | Confirmar muestra solo las que Logística también tiene esperando; la página puede quedar con menos de 20. |
| El inicio de la organización y el seguimiento del donante siguen con un tope de 100. | Quedan en DTI-39 ítem 1 con su lote. |
| Los puestos de un mes del ranking llegan todos. | Fuera de este lote; ver dudas. |

## Aprobación del diseño p2-listas-restantes (2026-11-12)

Aprobado por el usuario el 2026-11-12 con las opciones recomendadas: pidió aprobar en forma automática todas las opciones recomendadas de P1 y P2 para que la corrida termine sin su intervención.

1. El backlog (DTI-40) también le asigna a este lote GET /api/notificaciones general, la cuenta de camiones por estado y las mediciones C3 y C9, pero notificaciones-service y esas pantallas no están en los archivos del encargo. Recomiendo pasarlas a un lote propio de P2 y anotarlo en DTI-40, en vez de agrandar este.
   **Resuelto: la opción recomendada.**
2. Los puestos de un mes del ranking llegan todos dentro de cada mes, y con 20.000 donantes un mes podría tener miles. Recomiendo un lote siguiente con GET /ranking/{periodo}/entradas paginado; para la demo alcanza, porque el seed tiene 12 a 15 puestos por mes.
   **Resuelto: la opción recomendada.**
3. Recibidas deja de esconder las recibidas de meses anteriores (decisión 2). Recomiendo aceptarlo: con paginador no hace falta esconderlas, y la cifra del mes queda visible.
   **Resuelto: la opción recomendada.**
4. Dentro de cada grupo el orden pasa a ser por fecha de registro en lugar de la del último cambio. Recomiendo aceptarlo para la demo y pasar al último cambio solo si se nota.
   **Resuelto: la opción recomendada.**
5. El inicio de la organización (entidad-inicio) sigue leyendo 100 asignadas. Recomiendo dejarlo fuera de este lote y registrarlo en DTI-39 con el lote del tablero; la alternativa es sumarlo acá con el conteo y una página de 3.
   **Resuelto: la opción recomendada.**
6. Para que el cambio sea completo hay que tocar archivos que no están en la lista: LogisticaApi, IncentivosApi, RankingRest, los puertos y vistas de Recibidas, Seguimiento y Ranking, sus controllers web y la maqueta, y los OpenAPI de los tres servicios. Recomiendo autorizarlos como imprescindibles para el constructor y nombrarlos en su fueraDeZona.
   **Resuelto: la opción recomendada.**
