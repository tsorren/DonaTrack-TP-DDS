# Las organizaciones vistas por un donante, en un recurso propio con token y la marca "ya le donó" por página

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (aprobación de la spec `R11-p1-donante-listas`, 2026-11-12, con las opciones recomendadas en las tres dudas); los detalles que la spec dejaba abiertos, el constructor del lote `p1-donante-listas` (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P1
- Tags: donaciones, contrato, rest, seguridad, paginado, cliente-liviano, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL (AGENTS.md §7.3 pide el
> ADR antes de implementar): la corrida R11 construyó el lote con su
> [spec aprobada](../../specs/completed/R11-p1-donante-listas-spec.md) y dejó los ADRs para el cierre
> documental de la oleada. La spec (§7.1) entendía que no hacía falta ADR, porque el recurso sigue
> patrones ya decididos; el cierre lo escribe porque la decisión elige entre dos formas de control
> de acceso para la misma pregunta y fija un contrato nuevo de Donaciones. `[OBSERVED]` El código ya
> implementa esta decisión (commits `4b1b47b7e`, `bcb2d02b6` y `a848415ae`, OpenAPI de Donaciones
> 1.10.0). Código existente no es aprobación: si el revisor rechaza el ADR, puede hacer falta
> revertir.

## Contexto y Problema

`[OBSERVED]` Antes de R11-12, Entidades del donante pedía una página de 6 de
`GET /api/entidades/publicas` y resolvía "Categoría" y "Ya doné" en la plantilla, sobre esas 6:
el total y las páginas eran los de todas las activas, y "Ya le donaste" salía de cruzar con una
página de 100 donaciones del donante ([DTI-39](../DEUDA_TECNICA.md#dti-39), ítem 1). La regla
general 1 del backlog pide que los filtros se resuelvan en el servidor.

Problema: "Ya doné" depende de quién mira, y `/api/entidades/publicas` es una ruta pública sin
token. ¿Dónde se pregunta "a cuáles de estas organizaciones ya les doné", y cómo llega la marca de
cada una sin una segunda vuelta por página?

**Gate A:** decisión nueva: es el primer listado de Donaciones que cambia según quién lo pide.
**Gate B:** toca el control de acceso (qué ruta pide token) y el contrato REST entre el cliente y
Donaciones.

## Alternativas Consideradas

1. **Recurso del donante** `GET /api/donantes/{id}/entidades` (elegida), con token y la regla
   `DONANTE + SU_ID`, que resuelve búsqueda, categoría y `soloDonadas` en SQL y trae `yaLeDono` en
   cada fila.
2. **`donanteId` en la ruta pública** `/api/entidades/publicas`, con control de acceso solo cuando
   viene ese parámetro.
3. **Cruzar en el cliente** la página de organizaciones con todas las donaciones del donante.
4. **Una segunda llamada por página** que devuelva a cuáles de esos ids ya les donó.

## Resultado de la Decisión

Alternativa 1:

| Regla | Valor |
|---|---|
| Ruta | `GET /api/donantes/{id}/entidades`, detrás de `/api/donantes/**` en el gateway (ya pide token) |
| Parámetros | `nombre` (sin tildes), `categoriaId`, `soloDonadas`, `page`, `size`; total en `X-Total-Count`, como los listados de R11-01 |
| Cuerpo | Arreglo de `EntidadParaDonanteDTO` (los datos públicos de la organización más `yaLeDono`) |
| `yaLeDono` | Una sola consulta por página (`recibieronDe`, `SELECT DISTINCT … IN (:ids)` sobre los ids de la página), en el adaptador JPA y en el repositorio en memoria |
| "Ya doné" | Cuenta cualquier donación asignada a la organización, en cualquier estado, igual que Mis donaciones (duda 1 de la spec) |
| Categoría | También en la ruta pública: `categoriaId` opcional en `/api/entidades/publicas`, con la misma regla: una necesidad activa de una subcategoría de esa categoría, sin vencer o sin fecha límite |
| Cubierto | Una organización con lo de la categoría ya cubierto sigue en el resultado y la pantalla lo dice ("Lo que pedía de … ya está cubierto"), duda 3 |

**Por qué:**

- La ruta pública sigue siendo pública: no aparece un parámetro que, según su valor, cambie quién
  puede llamar. Con la alternativa 2, la lista del gateway y la de Donaciones decidirían distinto
  sobre la misma ruta.
- Cruzar en el cliente obliga a traer todas las donaciones del donante (contra la regla 1), y una
  segunda llamada por página hace el mismo cruce con otra vuelta de red.
- La categoría es información pública (lo que piden las organizaciones): ponerla en las dos rutas
  evita dos reglas para lo mismo.

## Consecuencias

**Positivas**

- "Ya doné" y "Categoría" filtran todas las organizaciones activas, y el total y las páginas son
  los de lo filtrado (R11-12). Entidades deja de leer las donaciones del donante.
- El donante solo ve su propia marca: la regla `DONANTE + SU_ID` ya probada en el resto de
  `/api/donantes/{id}`.

**Negativas**

- Dos rutas devuelven casi lo mismo (la pública y la del donante); un cambio en los datos públicos
  de la organización hay que reflejarlo en los dos DTOs.
- Una organización con lo de la categoría ya cubierto aparece al filtrar por esa categoría: llevar
  la cuenta de lo asignado a SQL es más grande que el lote.
- `[OBSERVED]` A9 (una sola consulta de `yaLeDono` por página) está bien por lectura del código,
  pero no tiene un test que cuente las sentencias; A1 y A5 no corren sobre PostgreSQL
  ([DTI-42](../DEUDA_TECNICA.md#dti-42)).
- `[OBSERVED]` La fecha "sin vencer" usa la zona del sistema (`LocalDate.now(ZoneId.systemDefault())`)
  y la spec pedía la de Buenos Aires: en un contenedor en UTC, una necesidad que vence hoy cambia
  a las 21 h de Argentina ([DTI-42](../DEUDA_TECNICA.md#dti-42)).

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| **1. Recurso del donante** | La ruta pública no cambia de acceso; una consulta por página para la marca | Dos DTOs de organización parecidos |
| 2. `donanteId` en la pública | Una sola ruta | Acceso que depende de un parámetro; gateway y servicio deciden distinto |
| 3. Cruzar en el cliente | Sin cambios en Donaciones | Todas las donaciones del donante en cada visita |
| 4. Segunda llamada por página | La ruta pública queda igual | Dos vueltas por página; el mismo cruce |

## Relaciones

- **Aplica:** [listados de Donaciones siempre paginados](./20261111-listados-paginados-de-donaciones-sin-lista-completa.md),
  la [búsqueda sin tildes](./20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md)
  y las [consultas de listado en SQL nativo con lectura por lote](./20261111-consultas-de-listado-en-sql-nativo-y-lectura-por-lote.md).
- **Respeta:** el [ADR de la validación JWT en doble capa y rutas públicas](../20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md).
- **Spec:** [R11-p1-donante-listas](../../specs/completed/R11-p1-donante-listas-spec.md).
- **Deuda:** [DTI-39](../DEUDA_TECNICA.md#dti-39) (ítem 1, saldado para Entidades y Mis
  donaciones) y [DTI-42](../DEUDA_TECNICA.md#dti-42).
