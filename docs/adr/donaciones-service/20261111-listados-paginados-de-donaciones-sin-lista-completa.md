# Listados de Donaciones siempre paginados: la página 0 de 20 por defecto, arreglo y total en `X-Total-Count`

- Status: proposed
- Date: 2026-11-11
- Deciders: el usuario (ajuste §0.2 al aprobar la spec `R11-p0-api-listados`, 2026-11-11); los detalles que la spec dejaba abiertos, el constructor del lote `p0-api-listados` (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P0
- Tags: donaciones, contrato, rest, paginado, rendimiento, cliente-liviano, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL (AGENTS.md §7.3 pide el
> ADR antes de implementar): la corrida R11 construyó el lote con su
> [spec aprobada](../../specs/completed/R11-p0-api-listados-spec.md) y dejó los ADRs para el cierre
> documental de la oleada. `[OBSERVED]` El código ya implementa esta decisión (commit `b3f8fc179`,
> rama `E6_backlog_r11_p0`, OpenAPI de Donaciones 1.9.0). Código existente no es aprobación: si el
> revisor rechaza el ADR, puede hacer falta revertir.

## Contexto y Problema

`[OBSERVED]` Antes de R11, `GET /api/donantes`, `/api/personas`, `/donaciones-independientes` y
`/api/donaciones` devolvían todo, y `/api/entidades` y `/api/entidades/publicas` paginaban en
memoria después de leer toda la tabla (HB-15, OpenAPI 1.8.0). Con un CSV de 20.000 donantes, las
pantallas del admin pedían la lista completa en cada visita y dejaban de responder
([backlog de la revisión del 2026-11-11](../../interfaz/backlog-revision-2026-11-11.md), §9).

La spec del lote proponía sumar `page` y `size` y conservar un "modo de compatibilidad": sin esos
parámetros, la lista entera, como antes. Al aprobarla, el usuario lo retiró (§0.2): **ninguna lista
llega completa**, ni siquiera a quien no pide página.

Problema: ¿qué forma tiene un listado de Donaciones, qué recibe quien no pide página y cómo recorre
todo quien de verdad lo necesita?

**Gate A:** decisión nueva. HB-15 había fijado la forma (arreglo y `X-Total-Count`) solo para
`/api/entidades`; esto la extiende a seis rutas y, sobre todo, cambia lo que recibe un pedido sin
página. **Gate B:** cambia un contrato entre servicios de manera incompatible en el comportamiento
(AGENTS.md §8.2, *breaking change*) y tiene costo de reversión: todos los consumidores pasaron a
pedir páginas.

## Alternativas Consideradas

1. **Modo de compatibilidad** (la spec antes de §0): sin `page` ni `size`, todo; con ellos, una
   página. Retirarlo más adelante.
2. **Siempre una página** (elegida): sin parámetros, la página 0 de 20; `size` hasta 100; arreglo en
   el cuerpo y total en `X-Total-Count`.
3. **Sobre** `{ "elementos": [...], "total": n }` en el cuerpo.
4. **`Page` de Spring** serializado tal cual (`content`, `totalElements`, `pageable`…).
5. **Paginado por cursor** (*keyset*: "los 20 siguientes a este id"), sin total.

## Resultado de la Decisión

Alternativa 2. Las reglas comunes de los seis listados (`ParametrosListado`, `Paginas.conTotal`):

| Regla | Valor |
|---|---|
| Cuerpo | Arreglo JSON con el mismo DTO de cada fila que antes (en donantes, más `fechaAlta`) |
| Total | `X-Total-Count`: cuántos cumplen la búsqueda y los filtros, no el largo de la página |
| `page` | Desde 0; por defecto 0 |
| `size` | 1 a 100 (`TAMANIO_MAXIMO`); por defecto 20 (`TAMANIO_POR_DEFECTO`) |
| Orden | El de `orden` o su valor por defecto, siempre con desempate por `id` |
| Página fuera de rango | 200 con arreglo vacío y el total real |
| Errores | `page` negativo, `size` fuera de rango, `q` o `nombre` de más de 100 caracteres u `orden` desconocido: 400 con `ERR-CSR-003` |
| `estado` en `/donaciones-independientes` | Repetible (`style: form`, `explode: true`); un solo valor se comporta como antes |
| Recorrer todo | Página por página, o por id: `GET /api/personas?id=…` hasta 100 ids |

Los servicios de aplicación ya no exponen un "traer todas" (`obtenerTodas` salió de donaciones
independientes y de entidades). El seeder de demo (`demo/seeder/seeder/cuentas.py`) recorre
`/api/personas` de a 100 hasta una página corta. El cliente liviano pide página en cada pantalla del
admin (lote `p0-admin-listados`) y, donde todavía no tiene paginador, una página de 100 con el tope
declarado ([DTI-39](../DEUDA_TECNICA.md#dti-39)).

**Por qué:**

- Es la regla 1 del backlog, decidida por el usuario: ninguna pantalla pide una lista completa. Con
  el modo de compatibilidad, lo que rompía la demo seguía disponible y bastaba un consumidor olvidado
  para repetir el problema.
- El cuerpo sigue siendo un arreglo: los consumidores que leen el arreglo (el cliente liviano, el
  seeder, los tests de contrato) no cambian de forma, y leer el total es opcional.
- El total en el header es el patrón que ya usaba `/api/entidades` (HB-15): no hay una segunda forma
  de paginar en el mismo servicio. El gateway lo pasa sin cambios (`RutasGateway`).

## Consecuencias

**Positivas**

- Ningún pedido a estas rutas trae más de 100 filas. Con 35.848 donantes en la base, la página de
  Donantes del admin respondió en 0,05 s, también la última (cierre de P0, 2026-11-12).
- Un solo contrato de listado en todo el servicio, documentado en OpenAPI 1.9.0.

**Negativas**

- Un consumidor que no se migró recibe 20 filas **sin aviso**. Se mitigó migrando todos los del
  cliente liviano en la misma oleada; los que todavía piden una página de 100 sin paginador están en
  [DTI-39](../DEUDA_TECNICA.md#dti-39), con su lote en esta corrida.
- oasdiff no ve el cambio de comportamiento por defecto: se registró a mano en
  `docs/arquitectura/contratos/cambios-incompatibles-aceptados.txt` (bloque R11-01), con el texto
  marcado `[INFERRED]` hasta que lo confirme el job de contratos del PR
  ([DTI-40](../DEUDA_TECNICA.md#dti-40)).
- El paginado por desplazamiento (`OFFSET`) cuesta más en páginas profundas. Con el volumen de la
  demo no se notó; si creciera mucho, el cursor (alternativa 5) es el camino.

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| 1. Modo de compatibilidad | Nadie se rompe el día del cambio | Deja abierto el camino que tumbaba la demo; "a retirar" sin fecha; contradice la regla 1 del usuario |
| **2. Siempre una página** | Cumple la regla 1; cuerpo de siempre; mismo patrón que HB-15 | Trunca en silencio a quien no migró; cambio incompatible en el comportamiento |
| 3. Sobre `{elementos, total}` | El total viaja en el cuerpo, visible para cualquiera | Rompe a todos los consumidores actuales, que esperan un arreglo; dos formas de paginar en el mismo servicio |
| 4. `Page` de Spring | Sin código propio | Lo mismo que 3, y ata el contrato público al framework (cambia con la versión de Spring Data) |
| 5. Cursor (*keyset*) | Costo constante en cualquier página; no salta filas si se inserta algo | Sin total ni "página N de M", que piden las pantallas (regla 3 del backlog); otro patrón distinto del de HB-15 |

## Relaciones

- **Extiende:** el patrón de paginado de `/api/entidades` (HB-15 de
  [requerimientos E6](../../requerimientos/requerimientos-e6.md)), sin ADR propio.
- **Respeta:** la [política de evolución de contratos](../20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
  (`accepted`): el cambio incompatible se registra con su motivo y la aprobación del usuario.
- **Va con:** [cifras del admin como conteos](./20261111-cifras-del-admin-como-conteos-en-un-resumen.md),
  [consultas de listado en SQL nativo y lectura por lote](./20261111-consultas-de-listado-en-sql-nativo-y-lectura-por-lote.md)
  y [búsqueda sin tildes](./20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md).
- **Deuda:** [DTI-39](../DEUDA_TECNICA.md#dti-39) (consumidores del cliente que todavía no tienen
  paginador) y [DTI-40](../DEUDA_TECNICA.md#dti-40) (listas de otros servicios y mediciones).
