# Búsqueda sin tildes ni mayúsculas en Donaciones: extensión `unaccent` en el esquema del servicio, llamada calificada

- Status: proposed
- Date: 2026-11-11
- Deciders: el usuario (ajuste §0.1 al aprobar la spec `R11-p0-api-listados`, 2026-11-11: "Gomez" encuentra "Gómez"); cómo llamarla, el constructor del lote `p0-api-listados` (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P0
- Tags: donaciones, postgresql, extension, busqueda, persistencia, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL: la corrida R11 construyó
> el lote con su [spec aprobada](../../specs/completed/R11-p0-api-listados-spec.md) y dejó los ADRs
> para el cierre documental de la oleada. `[OBSERVED]` El código ya implementa esta decisión
> (`V21__indices_listados.sql`, `ConsultaListado.sinTildes`, `TextoBuscado`, commit `b3f8fc179`).
> Si el revisor lo rechaza, puede hacer falta revertir.

## Contexto y Problema

La búsqueda `q` de donantes, personas y donaciones, y `nombre` de entidades, comparaban con
`lower(...) LIKE lower('%q%')`: "Gomez" no encontraba "Gómez" y "direccion" no encontraba
"Dirección". La spec proponía aceptarlo para no sumar extensiones; el usuario pidió lo contrario al
aprobarla (§0.1) y lo repitió para toda la corrida: toda búsqueda ignora tildes.

Hay dos implementaciones de cada repositorio: la JPA sobre PostgreSQL 16 (perfil `postgres`) y la
en memoria (perfil por defecto y tests). Las dos tienen que dar el mismo resultado.

**Gate A:** decisión nueva (no había búsqueda sin tildes). **Gate B:** suma una dependencia
estructural nueva, una extensión de PostgreSQL que la migración crea.

## Alternativas Consideradas

1. **Sin tildes no** (la spec antes de §0): solo `lower`.
2. **`unaccent(x)` sin calificar**, con la extensión donde la cree la base.
3. **`unaccent` en el esquema `donaciones` y llamada calificada** (elegida):
   `lower(donaciones.unaccent('donaciones.unaccent', x))`.
4. **Columna normalizada guardada** (un `nombre_busqueda` que escribe la aplicación al guardar).
5. **Intercalación ICU no determinista** (insensible a acentos) en las columnas.
6. **`unaccent` más `pg_trgm`** con un índice de trigramas para el "contiene".

## Resultado de la Decisión

Alternativa 3.

- `V21` hace `CREATE EXTENSION IF NOT EXISTS unaccent SCHEMA donaciones`. `unaccent` viene con la
  imagen oficial de PostgreSQL y es una extensión confiable (*trusted*): la crea el dueño de la base,
  sin superusuario.
- Las consultas usan `ConsultaListado.contiene(expresión, parámetro)`, que escribe
  `lower(donaciones.unaccent('donaciones.unaccent', expr)) LIKE lower(donaciones.unaccent('donaciones.unaccent', :q)) ESCAPE '\'`,
  con `%`, `_` y `\` escapados en el texto buscado.
- Los repositorios en memoria normalizan igual en Java (`TextoBuscado`: `Normalizer` en forma NFD,
  sin marcas, en minúsculas).
- La búsqueda "contiene" no lleva índice: se resuelve con un recorrido.

**Por qué:**

- Es lo que pidió el usuario, en la base y no en el cliente (regla 1 del backlog: la búsqueda se
  resuelve en el servidor, sobre todo el listado y no sobre una página).
- La llamada sin calificar (alternativa 2) falló en el contenedor de pruebas: `unaccent` resuelve
  su diccionario por el `search_path`, y la conexión de Testcontainers no tiene el esquema del
  servicio en él. Calificar la función y el diccionario no depende de la configuración de la
  conexión, y sigue la convención del proyecto de calificar las tablas con `donaciones.`
  (`SpringDataEventoConsumidoRepository`).
- Con el volumen de la demo, un recorrido alcanza: en el cierre de P0, "ibanez" entre 35.848
  donantes respondió "Coinciden 877 donantes" en 0,12 s.

## Consecuencias

**Positivas**

- "Gomez" y "Gómez", "GÓMEZ" y "gomez" se encuentran entre sí, en la base y en memoria (escenario
  C2b, `ListadosPostgresJpaTest` sobre `postgres:16-alpine` con Flyway completo).
- No depende del `search_path` ni del usuario con el que corre la migración.

**Negativas**

- El servicio necesita la extensión: una base de Donaciones sin `unaccent` disponible no arranca la
  migración `V21`.
- El SQL queda atado al nombre del esquema `donaciones` (ya lo estaba en otras consultas nativas).
- Sin índice para "contiene": con mucho más volumen hará falta la alternativa 6.
- `unaccent` no es `IMMUTABLE`: no se puede usar tal cual en una columna generada ni en un índice de
  expresión sin una función envoltorio.

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| 1. Solo `lower` | Sin dependencias nuevas | No cumple el pedido del usuario |
| 2. `unaccent(x)` sin calificar | El SQL más corto | Depende del `search_path`; falló en el contenedor de pruebas |
| **3. En el esquema y calificada** | Funciona con cualquier `search_path`; extensión confiable que trae la imagen | Dependencia de la extensión; SQL atado al esquema |
| 4. Columna normalizada | Indexable con b-tree para "empieza con"; sin extensión | Hay que escribirla en cada alta y cambio de persona y llenarla para las filas que ya existen; dos fuentes del mismo dato |
| 5. Intercalación ICU no determinista | Ignora acentos en `=` y en el orden | `[INFERRED]` PostgreSQL 16 no admite `LIKE` con intercalaciones no deterministas, y el "contiene" es justo lo que se busca; cambia la definición de las columnas |
| 6. `unaccent` y `pg_trgm` | "Contiene" con índice, para volúmenes grandes | Dos extensiones y un índice con función envoltorio por un volumen que el recorrido ya resuelve |

## Relaciones

- **Va con:** [listados paginados sin lista completa](./20261111-listados-paginados-de-donaciones-sin-lista-completa.md)
  y [consultas de listado en SQL nativo](./20261111-consultas-de-listado-en-sql-nativo-y-lectura-por-lote.md).
- **Respeta:** [base de datos por servicio en el mismo motor](../20261028-base-de-datos-por-servicio-en-el-mismo-motor-postgresql.md):
  la extensión vive en el esquema de Donaciones y no afecta a los otros servicios.
- **Alcance en el cliente:** la búsqueda de Entidades del donante dejó de filtrar la página en el
  cliente y usa la de Donaciones (arreglos del cierre de P0, `867cd738e`).
