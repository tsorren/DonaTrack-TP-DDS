# Spec: R11 p0-api-listados — El servicio de donaciones pagina, busca y cuenta

> **Estado:** IMPLEMENTADA (2026-11-12, cierre de la oleada P0). Aprobada por el usuario el
> 2026-11-11 con los ajustes de §0; construida en `b3f8fc179` y cerrada en la rama
> `E6_backlog_r11_p0`. Pasó a `docs/specs/completed/` en el cierre documental de P0. Quedan sin
> verificar C3 y C9 (sin el test de base que piden) y C10 (oasdiff no se corrió en local): están
> en [DTI-40](../../adr/DEUDA_TECNICA.md#dti-40). Diseño del lote `p0-api-listados` del
> [backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md) (R11-01,
> parte de servicio). Ningún ADR se promueve.
> **ADRs (`proposed`, escritos después del código):** [listados paginados sin lista completa](../../adr/donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md),
> [búsqueda sin tildes con `unaccent`](../../adr/donaciones-service/20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md),
> [consultas de listado en SQL nativo y lectura por lote](../../adr/donaciones-service/20261111-consultas-de-listado-en-sql-nativo-y-lectura-por-lote.md),
> [cifras del admin como conteos](../../adr/donaciones-service/20261111-cifras-del-admin-como-conteos-en-un-resumen.md) y
> [`fecha_alta` y `ultimos` en el modelo](../../adr/donaciones-service/20261111-fecha-de-alta-del-donante-y-ultimos-importados-en-el-archivo.md).
> El §7 dice "no hace falta ADR": quedó superado por los ajustes de §0 (una extensión nueva de
> PostgreSQL y el retiro del modo sin página), ver cada ADR.
> **Nivel:** ARCHITECTURAL por AGENTS.md §7.0 (cambia el contrato REST de Donaciones y suma una
> migración), escrita con el formato corto de la ruta STANDARD de `define-spec`, como pide el lote.
> **Fecha:** 2026-11-11
> **Base:** `a564a8d41` (backlog R11 sobre `E6_seeder`).
> **Módulos:** `donaciones-service` (código, migración y tests), `api-gateway` (solo se comprueba
> `RutasGateway`), `docs/arquitectura/contratos/openapi-donaciones.yaml`.
> **Relación:** el lote `p0-admin-listados` (tanda 2) migra el cliente liviano a estos endpoints.
> Este lote no toca el cliente.

---

## 0. Ajustes del usuario al aprobar (2026-11-11) — mandan sobre el resto de la spec

El usuario aprobó la spec con estos cambios. Donde el resto del documento diga otra cosa, vale esto.

1. **La búsqueda ignora tildes y mayúsculas.** "Gomez" encuentra "Gómez" y al revés. Se hace en la
   base: `V21` crea la extensión `unaccent` (viene con la imagen oficial de PostgreSQL) y las
   búsquedas comparan `lower(unaccent(...))` contra `lower(unaccent(q))`. Los repositorios en
   memoria normalizan igual (`java.text.Normalizer`, forma NFD, sin marcas). Suma el escenario C2b:
   `q=gomez` encuentra a "Gómez" y `q=GÓMEZ` encuentra a "Gomez".
2. **Toda lista tiene página o tope, ya en este lote.** Se retira el modo "sin página devuelve todo":
   - Sin `page` ni `size`, los cuatro listados devuelven la página 0 de 20, con `X-Total-Count`.
     `size` máximo 100. Nadie recibe una lista completa.
   - Quien necesita recorrer todo (el seeder, un proceso interno) pide página por página o por id
     (`GET /api/personas?id=…`, hasta 100). Este lote pasa el seeder de demo
     (`demo/seeder/seeder/cuentas.py`, `vivo.py`) a recorrer páginas; se avisa en `fueraDeZona`.
   - Los consumidores del cliente liviano los migra `p0-admin-listados` (tanda 2): todos, no solo
     las pantallas del admin (ver su ficha).
   - `GET /api/entidades` y `/api/entidades/publicas` pasan a paginar en SQL (hoy leen todo y cortan
     en memoria), con el mismo contrato de HB-15. Entra en este lote.
   - El escenario C4 cambia: sin `page` ni `size` se recibe la página 0 de 20, en el orden por
     defecto, con `X-Total-Count` real. El OpenAPI lo dice así, y si oasdiff lo marca, se registra
     en `cambios-incompatibles-aceptados.txt` con este motivo.
   - Los demás listados completos del backlog §9 (mapa, seguimientos, notificaciones, historial del
     ranking) se paginan en esta misma corrida, en sus lotes de P1 y P2 (R11-19, R11-14, R11-43).
3. **"Los últimos importados" sale de este lote.** `GET /api/donantes/archivos/{id}` suma un campo
   opcional `ultimos`: los 5 últimos donantes procesados del archivo, con nombre visible y
   `resultado` (`NUEVO` o `ACTUALIZADO`), guardado junto con el avance cada 500 filas. Aditivo.
4. **Fecha de alta en el seed.** Se acepta que los donantes existentes tomen la hora de la
   migración. El seeder no cambia por esto.
5. El filtro "Sin activar su cuenta" no existe del lado de Donaciones y hoy no filtra nada: no se
   agrega en este lote (decisión del diseñador, aceptada).

---

## 1. Objetivo Funcional (Goal)

Con 20.000 donantes importados por CSV, las pantallas del admin piden todos los donantes y todas
las donaciones en cada visita, y el servicio hace varias lecturas por fila (backlog §9). Este lote
hace que Donaciones **pague el trabajo en la base**: cada listado se pide de a una página, con la
búsqueda, los filtros y el orden resueltos en SQL y el total como número; las cifras del inicio del
admin y de la portada salen de consultas de conteo. Ningún listado arma una fila con una consulta
propia: las relaciones se traen con un join o con una lectura por lote por página.

`[DOCUMENTED]` Regla general 1 del backlog (§2): ninguna pantalla pide una lista completa; los
contadores se piden como números. `[DOCUMENTED]` D28 de `requerimientos-e6.md` ya descartaba
filtrar en el cliente.

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (lo que incluye)

- `GET /api/donantes`, `GET /api/personas`, `GET /donaciones-independientes` y `GET /api/donaciones`
  aceptan `page`, `size`, `q` (búsqueda), filtros y `orden`, y devuelven el total en el header
  `X-Total-Count`.
- Un endpoint nuevo de cifras para el inicio del admin: `GET /api/donaciones/resumen`.
- `GET /api/donaciones/contadores` (portada) mantiene su respuesta y pasa a calcularse con
  consultas de conteo.
- Lectura por lote en los mappers que hoy leen por fila: `DonanteMapper` (persona de cada donante,
  `DonanteMapper.java:36-39`) y `DonacionIndependienteMapper` (`quienDono` y las categorías,
  `DonacionIndependienteMapper.java:81-98`). La mejora vale también para los pedidos sin página.
- Una migración nueva `V21__indices_listados.sql` (la siguiente a `V20`): la columna `fecha_alta`
  del donante y los índices de los nuevos órdenes y filtros (§3.8).
- Las consultas nuevas en los dos juegos de repositorios: los adaptadores JPA (perfil `postgres`) y
  los repositorios en memoria (perfil por defecto y tests), con el mismo resultado.
- `openapi-donaciones.yaml` pasa a la versión 1.9.0 con todo lo de §3. Los cambios son aditivos.
- Tests de servicio, de controller y de contrato (§5), más un test sobre PostgreSQL de las
  consultas nuevas.

### Out-of-Scope (lo que no incluye)

- El cliente liviano (lo migra `p0-admin-listados`), el paginador visual (R11-11) y la pantalla de
  avance de la importación (R11-02).
- El estado de cuenta "Sin activar su cuenta" del filtro de donantes: vive en Auth/Keycloak, no en
  Donaciones (ver dudas).
- La lista de propuestas pendientes del inicio del admin: se entrega solo su cantidad.
- Las entidades pendientes de aprobación: vienen de Auth (`SolicitudesDeEntidad`). Este lote da la
  lectura por lote de sus personas (`GET /api/personas?id=…`), nada más.
- `GET /api/entidades` y `/api/entidades/publicas`: ya paginan (HB-15), aunque en memoria. Pasarlos
  a SQL queda para otro lote. **Reemplazado por §0.**
- Los otros listados completos del backlog §9 (`/api/entregas/mapa`, seguimiento, notificaciones,
  historial del ranking): son de otros servicios y otros lotes.
- Búsqueda sin tildes (`unaccent`) y búsqueda por trigramas (`pg_trgm`): no se agregan extensiones. **Reemplazado por §0.**
- Poner un tope a los pedidos sin página: llega después de que `p0-admin-listados` migre el cliente
  (ver §6 y dudas). **Reemplazado por §0.**
- Rutas nuevas en el gateway: todo lo nuevo cae dentro de prefijos que ya enruta (§3.7).

## 3. Decisiones Acordadas y Trade-offs: el contrato antes y después

Decisiones del diseñador del lote (sin matriz 5D, como pide el lote):

- **Alternativa elegida para la respuesta:** arreglo en el cuerpo y total en `X-Total-Count`, igual
  que `/api/entidades`. Descartadas: un sobre `{elementos, total}` (rompe a todos los clientes
  actuales, que esperan un arreglo) y el `Page` de Spring (lo mismo, y ata el contrato al framework).
- **Reemplazado por §0.** **Sin página, como hoy:** los pedidos sin `page` ni `size` siguen devolviendo todo, para no romper
  a nadie antes de `p0-admin-listados`. Descartado: un tope por defecto ya en este lote, porque
  cortaría en silencio la lista que el cliente y el seeder esperan completa.
- **Cifras del admin en una sola ruta** (`/api/donaciones/resumen`). Descartado: un conteo por ruta
  (seis llamadas por visita al inicio) y reusar los listados con `size=1` para leer el total (no
  sirve para "por vencer" ni para "entregadas en el mes").
- **`fecha_alta` nueva en `donante`** para el orden "Más nuevos primero" que ya ofrece la pantalla.
  Descartado: ordenar por id (es un UUID al azar) o sacar el orden.
- **Reemplazado por §0.** **Búsqueda con `LIKE` sin extensiones.** Descartado `pg_trgm`/`unaccent`: una extensión nueva de
  PostgreSQL por un volumen (20.000 filas) que un recorrido resuelve.

### 3.1 Reglas comunes de los cuatro listados

Siguen el patrón ya decidido para `GET /api/entidades` (HB-15, OpenAPI 1.8.0). No hay decisión
nueva de formato.

| Regla | Valor |
|---|---|
| Cuerpo | Siempre un arreglo JSON, con el mismo DTO de hoy por fila. |
| Total | Header `X-Total-Count`: cuántos cumplen la búsqueda y los filtros, sin contar la página. Se manda también en los pedidos sin página. |
| `page` | Desde 0. Si falta y viene `size`, vale 0. |
| `size` | 1 a 100. Si falta y viene `page`, vale 20. |
| Sin `page` ni `size` | **Reemplazado por §0.** Devuelve todos los que cumplen los filtros, en el mismo orden de hoy. Es el modo de compatibilidad (§4); el OpenAPI lo marca como "a retirar". |
| Con `page` o `size` | Ordena según `orden` (o su valor por defecto) y devuelve ese tramo. El orden siempre desempata por `id`, para que una fila no salte de página. |
| `q` | **Reemplazado por §0.1 (también sin tildes).** Texto con espacios recortados; vacío es lo mismo que no mandarlo. "Contiene", sin distinguir mayúsculas (`lower(...) LIKE lower('%q%')`), con `%`, `_` y `\` escapados. Hasta 100 caracteres. |
| Errores | `page` negativo, `size` fuera de 1..100, `q` de más de 100 caracteres o un valor de `orden` desconocido: 400 con `ERR-CSR-003`, como `/api/entidades`. |
| Permisos | Los mismos `@PreAuthorize` de hoy en cada ruta. |
| Página fuera de rango | 200 con arreglo vacío y el `X-Total-Count` real. |

### 3.2 `GET /api/donantes` (solo ADMIN)

**Antes:** parámetro `canal` opcional. Devuelve todos los donantes (con los de baja); con `canal`,
solo los activos, no anonimizados y con ese medio de contacto. Una lectura de persona por donante.

**Después:** se suman estos parámetros, todos opcionales y combinables con `canal`:

| Parámetro | Valores | Qué filtra |
|---|---|---|
| `q` | texto | Nombre y apellido de la humana, razón social de la jurídica o número de documento. |
| `tipo` | `HUMANA`, `JURIDICA` | Tipo de persona. |
| `tipoJuridico` | `EMPRESA`, `ONG`, `INSTITUCION`, `GUBERNAMENTAL` | Solo jurídicas de ese tipo (implica `tipo=JURIDICA`). |
| `activo` | `true`, `false` | Donante activo o dado de baja. |
| `categoriaId` | uuid | Donó alguna vez algo de esa categoría ("Qué dona" de la pantalla). |
| `orden` | `nuevos` (por defecto), `antiguos`, `nombre` | Por `fecha_alta` descendente o ascendente, o por nombre visible (razón social, o apellido y nombre). |
| `page`, `size` | §3.1 | |

Respuesta: arreglo de `DonanteOutputDTO`, con un campo nuevo **`fechaAlta`** (`date-time`, la del
primer registro como donante; una reactivación no la cambia). Las filas de una página traen su
persona con un solo join o una lectura por lote.

Usos previstos: el listado del admin (`admin-donantes`) y el buscador de donantes al registrar una
donación (`AdminOperacionRegistro.java:35`), que pasa a pedir
`GET /api/donantes?q=<texto>&activo=true&orden=nombre&size=10`.

### 3.3 `GET /api/personas` (ADMIN o token técnico)

**Antes:** parámetro `tipo` opcional. Devuelve todas las personas.

**Después:**

| Parámetro | Valores | Qué filtra |
|---|---|---|
| `tipo` | `HUMANA`, `JURIDICA` | Igual que hoy. |
| `tipoJuridico` | como en §3.2 | |
| `q` | texto | Como en §3.2. |
| `id` | uuid, repetible, hasta 100 | Lectura por lote: solo esas personas. Un id que no existe no da error, no aparece. |
| `orden` | `nombre` (por defecto) | |
| `page`, `size` | §3.1 | |

Uso previsto: Entidades del admin pide solo las personas de las cuentas pendientes con
`GET /api/personas?id=…&id=…` en lugar de todas las jurídicas (`AdminPersonasEntidades.java:57`).

### 3.4 `GET /donaciones-independientes` (ADMIN; donante o entidad sobre lo suyo)

**Antes:** filtros `estado`, `subcategoriaId`, `categoriaId`, `donanteId`, `entidadId`, `desde`,
`hasta`, `necesidadId`. Filtra en memoria sobre `findAll()`; tres lecturas por fila para `quienDono`.

**Después:** los mismos filtros, resueltos en SQL, más:

| Parámetro | Valores | Qué cambia |
|---|---|---|
| `estado` | los 8 de hoy, **repetible** | Una o varias (`estado=ASIGNACION_REALIZADA&estado=LISTA_PARA_ENTREGAR`). Un solo valor se comporta como hoy. |
| `q` | texto | Descripción de la donación o de alguno de sus bienes. |
| `orden` | `recientes` (por defecto), `antiguas` | Por `fecha_registro`. |
| `page`, `size` | §3.1 | |

Respuesta: arreglo de `DonacionIndependienteResponseDTO` sin cambios. `quienDono` y las categorías
de los ítems se arman con lecturas por lote de la página (donaciones originales, personas y
donantes de todos los ids juntos).

Permisos: sin cambios. La regla de hoy (`principal.puedeOperarSobre(#filtro.donanteId())` o
`#filtro.entidadId()`) sigue aplicando, así que un donante o una entidad también puede paginar lo
suyo; eso sirve a R11-13 ("Mis donaciones" con páginas).

### 3.5 `GET /api/donaciones` (solo ADMIN)

**Antes:** sin parámetros; devuelve todas las donaciones originales.

**Después:**

| Parámetro | Valores | Qué filtra |
|---|---|---|
| `donanteId` | uuid | Las de ese donante. |
| `desde`, `hasta` | `date` | Fecha de la donación, inclusivas; `hasta` anterior a `desde` da 400. |
| `q` | texto | Descripción de la donación o de alguno de sus bienes. |
| `orden` | `recientes` (por defecto), `antiguas` | Por `fecha`. |
| `page`, `size` | §3.1 | |

Respuesta: arreglo de `DonacionOutputDTO` sin cambios; el donante de cada fila sale de una lectura
por lote.

### 3.6 Cifras

**`GET /api/donaciones/contadores` (público).** Respuesta sin cambios (`ContadoresPublicosDTO`).
Cambia el cálculo: `donantesActivos` pasa de recorrer todos los donantes en memoria
(`ContadoresPublicosService.java:55-58`) a un `count` con `EXISTS` sobre `donacion`;
`beneficiariosActivos` pasa a un `count` de entidades activas en vez de leerlas para paginar.

**`GET /api/donaciones/resumen` (nuevo, solo ADMIN).** Todas las cifras del inicio del admin en una
llamada, cada una con una consulta de conteo:

```json
{
  "donacionesPorEstado": {
    "EN_DEPOSITO": 120, "ASIGNACION_REALIZADA": 8, "LISTA_PARA_ENTREGAR": 3,
    "EN_TRASLADO": 2, "EN_PUERTA": 1, "ENTREGADA": 940, "ENTREGA_FALLIDA": 4, "VENCIDA": 6
  },
  "entregadasEnElMes": { "mes": "2026-11", "cantidad": 37 },
  "porVencer": {
    "dias": 7,
    "cantidad": 5,
    "proxima": { "id": "uuid", "descripcion": "Leche larga vida", "fechaVencimiento": "2026-11-13" }
  },
  "donantes": { "total": 20038, "activos": 20011 },
  "entidadesActivas": 12,
  "propuestasPendientes": 4,
  "calculadoEn": "2026-11-11T10:30:00-03:00"
}
```

| Cifra | Cómo se calcula | Reemplaza en el cliente |
|---|---|---|
| `donacionesPorEstado` | `count` agrupado por `estado_actual`; los 8 estados siempre presentes, con 0 si no hay. | `contar(todas, …)` de `AdminPersonasInicio.java:79-101` |
| `entregadasEnElMes` | Cambios a `ENTREGADA` con `timestamp` dentro del mes en curso del reloj del servicio. | El filtro por mes de `AdminPersonasInicio.java:79-83` |
| `porVencer` | Donaciones en `EN_DEPOSITO` con algún bien que vence entre hoy y dentro de 7 días; `proxima` es la de vencimiento más cercano, o `null`. | `vencen` y `primera(...)` de `AdminPersonasInicio.java:52-57` |
| `donantes` | `count` total y `count` de activos. | La lista completa de `AdminPersonasRest.java:96-103` |
| `entidadesActivas` | `count` de entidades activas. | `entidades.activas()` |
| `propuestasPendientes` | `count` de propuestas en `PENDIENTE`. | `propuestas.size()` |

Las entidades pendientes de aprobación no están: vienen de Auth.

### 3.7 Gateway

`[OBSERVED]` `RutasGateway.rutaDonaciones` ya enruta `/api/donaciones/**`, `/api/donantes/**` y
`/api/personas/**`, así que `resumen`, `contadores` y los parámetros nuevos pasan sin cambios.
`/donaciones-independientes` sigue fuera del gateway por decisión de ADR-E6-02 (alias sin `/api`);
este lote no lo cambia. El constructor solo comprueba, con `RutasGatewayTest`, que
`/api/donaciones/resumen` llega al servicio.

### 3.8 Migración `V21__indices_listados.sql`

| Cambio | Para qué |
|---|---|
| `ALTER TABLE donante ADD COLUMN fecha_alta TIMESTAMP NOT NULL DEFAULT now()` | Orden `nuevos`/`antiguos` y el campo `fechaAlta`. Los donantes que ya existen toman la hora de la migración. |
| `CREATE INDEX ix_donante_fecha_alta ON donante (fecha_alta DESC, id)` | Orden por alta, con desempate. |
| `CREATE INDEX ix_donacion_independiente_fecha ON donacion_independiente (fecha_registro DESC, id)` | Orden y filtro por fecha de registro. |
| `CREATE INDEX ix_donacion_fecha ON donacion (fecha DESC, id)` | Orden y filtro por fecha de la donación original. |
| `CREATE INDEX ix_cambio_estado_di_timestamp ON cambio_estado_donacion_independiente ("timestamp")` | Entregadas en el mes. |
| `CREATE INDEX ix_item_normalizado_vencimiento ON item_donacion_normalizado (fecha_vencimiento) WHERE fecha_vencimiento IS NOT NULL` | Por vencer. |
| `CREATE INDEX ix_juridica_tipo ON juridica (tipo_juridico)` | Filtro `tipoJuridico`. |

Lo que **no** lleva índice, a propósito: la búsqueda `q` ("contiene" no usa un índice b-tree y no se
agrega `pg_trgm`) y el orden por nombre, que cruza `humana` y `juridica` en una sola expresión. Con
20.000 filas ambos se resuelven en la base con un recorrido y un ordenamiento; el criterio de
aceptación C9 lo mide. Los filtros por estado, donante, entidad, necesidad y persona ya tienen índice
(`V9`, `V10`, `V4`).

## 4. Invariantes y compatibilidad con los clientes actuales

Invariantes que el lote no puede romper:

- **Reemplazado por §0.** Un pedido sin `page` ni `size` devuelve el mismo cuerpo que antes del cambio (salvo el campo nuevo
  `fechaAlta`).
- `X-Total-Count` es siempre el total con los filtros aplicados, nunca el largo de la página.
- Ningún listado paginado ni ninguna cifra lee todas las filas de una tabla en memoria.
- Los permisos de cada ruta no cambian; las cifras nuevas son solo para ADMIN.
- Los contadores públicos no exponen datos personales (RF-01, HB-02).
- No se renombra ninguna ruta, parámetro ni campo existente.

`[OBSERVED]` Consumidores de estas rutas en el repo:

| Consumidor | Qué pide hoy | Qué le pasa |
|---|---|---|
| Cliente liviano, `DonacionesApi` | `/api/donantes?canal`, `/api/personas?tipo`, `/donaciones-independientes` con filtros, `/api/donaciones`, `/api/donaciones/contadores` | **Reemplazado por §0.** Nada: sin `page` ni `size` recibe el mismo arreglo, en el mismo orden. Ignora el header nuevo y el campo `fechaAlta` (los records de Jackson no fallan con campos desconocidos: se verifica en C10). |
| Seeder de demo (`demo/seeder/seeder/cuentas.py:88`, `vivo.py:145`) | `GET /api/personas` completo y `/donaciones-independientes/{id}` | Nada. El seeder inserta en `donante` por API, así que `fecha_alta` toma su valor por defecto. |
| `integration-tests`, Postman, k6 | Altas y lecturas por id | Nada. |
| Otros servicios | Ninguno consulta estas rutas. | — |

Todos los cambios son aditivos (parámetros opcionales, un header, un campo de respuesta, una ruta
nueva): el job de contratos (`scripts/diff-contratos.sh`, oasdiff) no debe marcar errores ni
necesitar líneas nuevas en `cambios-incompatibles-aceptados.txt`. Pasar `estado` de un valor a una
lista de valores repetibles es compatible en la consulta; si oasdiff lo reporta como cambio de tipo
del parámetro, se registra la línea con su motivo.

## 5. Criterios de Aceptación

Cada uno se verifica con un test automatizado; los de base, con PostgreSQL (Testcontainers, como
`DonacionesPostgresContextTests`).

```gherkin
Escenario C1: Página de donantes con total
  Dado 45 donantes
  Cuando se pide GET /api/donantes?page=1&size=20
  Entonces responde 200 con 20 donantes, del 21.º al 40.º por fecha de alta descendente
  Y el header X-Total-Count vale 45

Escenario C2: Búsqueda y filtros de donantes en la base
  Dado donantes humanos y jurídicos, activos y dados de baja, con donaciones de distintas categorías
  Cuando se pide GET /api/donantes?q=gom&tipo=HUMANA&activo=true&categoriaId=<alimentos>&page=0&size=20
  Entonces solo vienen humanas activas cuyo nombre, apellido o documento contiene "gom" sin importar mayúsculas y que donaron alimentos
  Y X-Total-Count es la cantidad de esas, no la de la página

Escenario C3: Sin consulta por fila
  Dada una página de 20 donantes y otra de 20 donaciones independientes
  Cuando se arman las dos respuestas
  Entonces la cantidad de sentencias SQL es la misma con 5 filas que con 20 (contada con las estadísticas de Hibernate)

Escenario C4: Los clientes sin página siguen igual
  # Reemplazado por §0.2: sin page ni size llega la página 0 de 20, con X-Total-Count real.
  Dados los mismos datos
  Cuando se pide GET /api/donantes, /api/personas?tipo=JURIDICA, /donaciones-independientes?entidadId=<x> y /api/donaciones sin page ni size
  Entonces el cuerpo es el mismo arreglo, en el mismo orden, que antes del cambio (más fechaAlta en donantes)
  Y llega X-Total-Count con el largo del arreglo

Escenario C5: Varios estados y orden estable
  Dadas donaciones en ASIGNACION_REALIZADA, LISTA_PARA_ENTREGAR y EN_DEPOSITO, varias con la misma fecha de registro
  Cuando se pide GET /donaciones-independientes?estado=ASIGNACION_REALIZADA&estado=LISTA_PARA_ENTREGAR&page=0&size=2 y después page=1
  Entonces solo vienen de esos dos estados, ninguna se repite ni falta entre páginas

Escenario C6: Lote de personas
  Cuando se pide GET /api/personas?id=<a>&id=<b>&id=<inexistente>
  Entonces vienen solo a y b, y X-Total-Count vale 2

Escenario C7: Resumen del admin
  Dadas donaciones en cada estado, 3 entregadas este mes y 1 el mes pasado, y 2 en depósito con bienes que vencen en 2 y 5 días
  Cuando el admin pide GET /api/donaciones/resumen
  Entonces donacionesPorEstado trae los 8 estados, entregadasEnElMes.cantidad vale 3, porVencer.cantidad vale 2 y proxima es la que vence en 2 días
  Y un donante o una entidad recibe 403

Escenario C8: Parámetros inválidos
  Cuando se pide page=-1, size=0, size=101, orden=otro o un q de 101 caracteres en cualquiera de los cuatro listados
  Entonces responde 400 con ERR-CSR-003

Escenario C9: Volumen de la demo
  Dados 20.000 donantes y 2.000 donaciones independientes en PostgreSQL
  Cuando se piden la primera página de donantes con q, la de donaciones por estado, el resumen y los contadores
  Entonces cada respuesta se arma sin leer todas las filas en memoria (el test lo comprueba por la cantidad de entidades cargadas por Hibernate, no por tiempo)

Escenario C10: Contrato
  Cuando corre el job de contratos contra openapi-donaciones.yaml 1.9.0
  Entonces no hay cambios incompatibles nuevos
  Y ContratoDeLasApisTest del cliente liviano sigue en verde sin tocarlo
```

Además: `mvn clean test -pl donaciones-service -am` y `mvn test -pl api-gateway` en verde,
`mvn spotless:check` y el pre-flight de SonarCloud (`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`).

## 6. Riesgos

| Riesgo | Cómo se cubre |
|---|---|
| Dos implementaciones de cada repositorio (JPA y en memoria) que den resultados distintos. | Los tests de servicio corren contra la de memoria y los de base contra PostgreSQL con los mismos casos (C1, C2, C5). |
| El orden "de siempre" de los pedidos sin página no está definido hoy (`findAll()`). | C4 fija el orden que devuelve hoy cada repositorio y lo conserva; los pedidos paginados sí lo definen. |
| `fecha_alta` de los donantes existentes es la hora de la migración: en una base con historia, "más nuevos" no distingue a los viejos entre sí (desempatan por id). | Aceptado: la base de la demo se arma después de la migración. Queda dicho en el OpenAPI. |
| **Reemplazado por §0.** La búsqueda no ignora tildes: "Gomez" no encuentra "Gómez". | Aceptado en este lote; queda como duda. |
| **Reemplazado por §0.** El modo sin página sigue trayendo todo, que es justo lo que rompe la demo. | Lo usan solo los clientes que migra `p0-admin-listados`. Retirarlo (tope de tamaño por defecto) es un paso posterior, ya anotado como "a retirar" en el OpenAPI. |
| Pool de 5 conexiones compartido con el importador (backlog §9). | Las consultas nuevas son cortas y de solo lectura; el ensayo de 20.000 filas es R11-03. |
| `estado` repetible cambia el tipo del parámetro en el OpenAPI. | Ver §4; si oasdiff lo marca, se registra con su motivo. |
| `DonanteOutputDTO` es la respuesta de otras rutas (`/{id}`, alta, preferencias). | `fechaAlta` aparece en todas: es aditivo y se documenta en cada una. |

## 7. Especificación Técnica de Diseño (Technical Design): ADR, plan de tests y siguiente etapa

Aprobada la spec, el constructor del lote implementa con `implement-task` en `donaciones-service`,
actualiza `openapi-donaciones.yaml` (1.9.0) y suma la deuda de paginado y rendimiento en
`docs/adr/DEUDA_TECNICA.md` (backlog §10). Después, `p0-admin-listados` migra el cliente.

**ADR:** no hace falta uno nuevo (AGENTS.md §9.1, Gate A). El formato paginado es el ya decidido en
HB-15; los conteos y la lectura por lote implementan la regla general 1 del backlog, decidida por el
usuario; `fecha_alta` es un ajuste aditivo del modelo, como `activo` en `V8`. Gate B no se evalúa.

**Plan de tests (primero el test, después el código), en este orden:**

1. Repositorios en memoria: filtros, `q`, orden y tramo de cada listado (C1, C2, C5, C6).
2. Mismos casos sobre PostgreSQL con Testcontainers, más la migración `V21` aplicada sobre una base
   con datos (C9).
3. Servicios: lectura por lote, contada con las estadísticas de Hibernate (C3); resumen y
   contadores con reloj fijo (C7).
4. Controllers con MockMvc: parámetros, header, 400 y permisos (C4, C7, C8).
5. Contrato: OpenAPI 1.9.0 contra oasdiff y `ContratoDeLasApisTest` del cliente sin tocarlo (C10).
6. `RutasGatewayTest`: `/api/donaciones/resumen` llega al servicio.

Las rutas y los nombres de parámetros de §3 son el contrato; las clases y consultas las decide el
constructor siguiendo los adaptadores que ya existen.

## 8. Cierre (2026-11-12): revisión y lo que quedó

Resumen de la revisión de integración de la oleada P0, hecha por un revisor separado del
constructor (`[INFERRED]` por cómo la orquesta el workflow: lee la rama y los informes, no el
razonamiento del constructor), y del cierre con el stack propio. Lo pendiente está
en [DTI-40](../../adr/DEUDA_TECNICA.md#dti-40).

```text
=== REVIEW CONTRACT ===

Mode: INDEPENDENT_REVIEW (revisión de integración de la oleada P0 del workflow backlog-r11)
Task Level: ARCHITECTURAL

V1 Regression risks:
  [OBSERVED] Sin página llega la página 0 de 20: los consumidores del cliente liviano se migraron
  en p0-admin-listados (misma rama). Los que piden una página de 100 sin paginador: DTI-39.

V2 Architecture/invariant violations:
  NONE_DETECTED. Dominio sin JPA (LecturaPorLote, criterios del dominio); SQL en los adaptadores.

V3 Contracts/integration:
  [INFERRED] C10: oasdiff no se corrió en local; las dos líneas de R11-01 en
  cambios-incompatibles-aceptados.txt se confirman con el job de contratos del PR (DTI-40).

V4 Tests/coverage:
  [OBSERVED] C3 solo con dobles de prueba (never().findById); C9 sin test de volumen (DTI-40).
  [VERIFIED] ListadosPostgresJpaTest: 11 tests sobre postgres:16-alpine con Flyway completo (V21).
  [VERIFIED] mvn clean test -pl donaciones-service,api-gateway,cliente-liviano -am en verde en el
  cierre (1.141 tests en donaciones-service), con el stack del usuario detenido.

V5 Scope violations:
  [OBSERVED] Fuera de zona, declarado: demo/seeder/seeder/cuentas.py recorre páginas.

V6 Evidence integrity:
  NONE_DETECTED.

V7 SonarCloud pre-flight:
  [TESTS_NOT_EXECUTED_BY_EVALUATOR] No consta en los informes del lote; lo mide el Quality Gate del PR.

V8 Security/privacy:
  NONE_DETECTED. Mismos @PreAuthorize; el resumen es solo ADMIN; los contadores públicos no
  exponen datos personales.

V9 Documentation graph integrity:
  [OBSERVED] Spec movida a completed/ con README, ESTADO_DOCUMENTACION, DEUDA_TECNICA (DTI-39 y
  DTI-40) y docs/generated regenerado (OpenAPI 1.9.0) en el cierre documental.

Menores, sin corregir: DonanteMapper.toOutputDTOs no filtra personaId nulos; borde del mes de
entregadasEnElMes (reloj de Buenos Aires contra UTC) sin test. Ambos en DTI-40.
```

Ensayo de volumen real (cierre de P0, stack propio con 35.848 donantes): Donantes del admin
respondió en 0,05 s por página, también la última, y la búsqueda "ibanez" en 0,12 s
([guía del ensayo](../../testing/ensayo-importacion-20000.md#resultados-del-cierre-de-p0-2026-11-12)).
