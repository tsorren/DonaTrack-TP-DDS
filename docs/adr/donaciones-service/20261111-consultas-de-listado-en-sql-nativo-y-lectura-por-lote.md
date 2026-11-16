# Listados de Donaciones en SQL nativo (ids de la página más conteo) y lectura por lote de las relaciones

- Status: proposed
- Date: 2026-11-11
- Deciders: el constructor del lote `p0-api-listados` (agente, sin consulta): la spec dejaba "las clases y consultas" al constructor. Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P0
- Tags: donaciones, persistencia, jpa, sql, rendimiento, repositorios, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL: la corrida R11 construyó
> el lote con su [spec aprobada](../../specs/completed/R11-p0-api-listados-spec.md) y dejó los ADRs
> para el cierre documental de la oleada. `[OBSERVED]` El código ya implementa esta decisión
> (`ConsultaListado`, los adaptadores `*RepositoryJpaAdapter`, `LecturaPorLote`, los mappers,
> commit `b3f8fc179`). Si el revisor lo rechaza, puede hacer falta revertir.

## Contexto y Problema

`[OBSERVED]` Antes de R11, los listados filtraban en memoria sobre `findAll()` y los mappers leían
una o varias relaciones por fila: la persona de cada donante (`DonanteMapper`) y, para cada donación
independiente, su donación original, el donante, la persona, la subcategoría y la categoría
(`DonacionIndependienteMapper`). Con 20.000 donantes eso era una consulta por fila sobre un pool de 5
conexiones ([backlog](../../interfaz/backlog-revision-2026-11-11.md), §9, causa 2).

Los listados nuevos combinan filtros opcionales, búsqueda sin tildes, condiciones sobre colecciones
("donó alguna vez de esta categoría", "algún bien contiene el texto") y un total con los mismos
filtros. Además, cada repositorio tiene una versión en memoria que tiene que dar el mismo resultado.

**Gate A:** decisión nueva: es el primer listado del servicio armado en la base con conteo, y fija
cómo se consultan y cómo se leen las relaciones. **Gate B:** estrategia de persistencia y un
trade-off deliberado entre rendimiento, legibilidad y pureza del dominio.

## Alternativas Consideradas

1. **Spring Data Specifications / Criteria API** con `Pageable`.
2. **JPQL con `JOIN FETCH`** y `Pageable`.
3. **`@EntityGraph`** en los repositorios de Spring Data.
4. **Querydsl o jOOQ** (dependencia nueva).
5. **Solo `hibernate.default_batch_fetch_size`**, sin cambiar las consultas.
6. **SQL nativo en dos pasos y lectura por lote** (elegida).

## Resultado de la Decisión

Alternativa 6.

- **Dos consultas por página.** `ConsultaListado` (paquete de adaptadores JPA) arma, con los mismos
  filtros, los ids del tramo (`SELECT id … ORDER BY <orden>, id LIMIT :limite OFFSET :desplazamiento`)
  y el total (`SELECT count(*) …`). Solo agrega las condiciones de los filtros presentes, así ningún
  parámetro llega nulo (PostgreSQL no infiere su tipo). El `FROM` lleva solo uniones uno a uno; lo
  que mira colecciones va en un `EXISTS`, para que el conteo no repita filas.
- **Las filas, por id.** El adaptador lee las entidades de esos ids con `findAllById` y las devuelve
  en el orden de la página (`ConsultaListado.enOrden`). Las tablas van calificadas con `donaciones.`.
- **Relaciones por lote.** Los mappers leen las relaciones de toda la página juntas:
  `DonanteMapper.toOutputDTOs` (personas), `DonacionMapper.toOutputDTOs` (donantes) y
  `DonacionIndependienteMapper.toDTOs`, que junta en un `record Lecturas` privado las cinco
  lecturas (donaciones originales, donantes, personas, subcategorías y categorías).
- **El dominio sin JPA.** Las interfaces de repositorio del dominio extienden `LecturaPorLote`, con
  un `findAllById` por defecto que lee por id (los repositorios en memoria); los adaptadores JPA lo
  redefinen con una sola consulta. Los criterios (`CriterioDonantes`, `CriterioDonacionesIndependientes`,
  `Paginado`, `Tramo`, `TextoBuscado`…) son del dominio y los usan las dos implementaciones.
- `hibernate.default_batch_fetch_size=50` (`application-postgres.properties`) queda como respaldo
  para las colecciones de cada entidad.

**Por qué:**

- `unaccent` calificado y `EXISTS` sobre colecciones son directos en SQL; con Criteria o JPQL hay que
  registrar funciones y armar subconsultas con una API más larga para el mismo resultado.
- Con `JOIN FETCH` y una colección, Hibernate pagina en memoria (advertencia HHH90003004) o el
  conteo repite filas: justo lo que hay que evitar.
- La lectura por lote deja la cantidad de consultas fija por página, no por fila, y no mete
  anotaciones ni DTOs de JPA en el dominio
  ([capas y puertos y adaptadores](../20261009-estilo-de-microservicios-con-capas-y-puertos-y-adaptadores.md)).
- No suma dependencias (AGENTS.md §6).

## Consecuencias

**Positivas**

- Una página cuesta dos consultas más una lectura por lote por cada relación, con 5 filas o con 100.
- El perfil en memoria y los tests conservan el mismo comportamiento sin tocar cada test (los
  repositorios en memoria suman constructores para recibir los repositorios relacionados).

**Negativas**

- Hay SQL escrito a mano en los adaptadores: un cambio de columna no lo detecta el compilador.
  Lo cubren los tests sobre PostgreSQL (`ListadosPostgresJpaTest`, 11 casos, `postgres:16-alpine`).
- Dos implementaciones de cada criterio (SQL y Java) que pueden divergir. Se mitiga con los mismos
  casos en las dos (C1, C2, C5, C6).
- `[OBSERVED]` La cantidad de sentencias por página (C3) solo se comprobó con dobles de prueba
  (`never().findById`), y el volumen (C9) no tiene test: queda en
  [DTI-40](../DEUDA_TECNICA.md#dti-40).
- `DonanteMapper.toOutputDTOs` pasa los `personaId` a `findAllById` sin filtrar nulos, a diferencia
  de `DonacionMapper`; anotado en [DTI-40](../DEUDA_TECNICA.md#dti-40).

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| 1. Specifications / Criteria | Tipado; composición de filtros | `unaccent` y `EXISTS` con una API verbosa; el conteo de Spring Data repite la consulta con sus joins |
| 2. JPQL con `JOIN FETCH` | Una sola consulta por página | Con colecciones pagina en memoria o repite filas en el conteo |
| 3. `@EntityGraph` | Declarativo | Mismo problema que 2 con colecciones; no sirve para relaciones entre agregados por UUID, que no son asociaciones JPA |
| 4. Querydsl o jOOQ | Tipado y SQL expresivo | Dependencia y generación de código nuevas por cinco listados |
| 5. Solo `batch_fetch_size` | Sin código | Sigue filtrando en memoria; no resuelve las lecturas entre agregados, que se hacen por id en los mappers |
| **6. SQL nativo y lote** | Filtros, búsqueda y total en la base; consultas fijas por página; dominio limpio | SQL a mano; dos implementaciones por criterio |

## Relaciones

- **Respeta:** [herencia de repositorios en memoria](./20260616-herencia-repositories-en-memoria.md),
  [referencias entre agregados por UUID](./20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md)
  y [estrategia de mapeo ORM](./20260901-estrategia-de-mapeo-orm-y-herencia-relacional-en-donaciones.md).
- **Va con:** [listados paginados sin lista completa](./20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
  y [búsqueda sin tildes](./20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md).
