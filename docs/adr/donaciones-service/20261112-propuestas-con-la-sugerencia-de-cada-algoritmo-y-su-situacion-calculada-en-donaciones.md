# Cada propuesta guarda qué algoritmo la sugirió y en qué puesto, y Donaciones calcula su situación

- Status: proposed
- Date: 2026-11-12
- Deciders: el usuario (variante C de Asignaciones en la revisión del 2026-11-11 y aprobación de la spec `R11-p1-asignaciones`, 2026-11-12, con las opciones recomendadas en las seis dudas); los detalles que la spec dejaba abiertos, el constructor del lote `p1-asignaciones` (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P1
- Tags: donaciones, contrato, rest, persistencia, asignacion, paginado, cliente-liviano, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL (AGENTS.md §7.3 pide el
> ADR antes de implementar): la corrida R11 construyó el lote con su
> [spec aprobada](../../specs/completed/R11-p1-asignaciones-spec.md) y dejó los ADRs para el
> cierre documental de la oleada. La spec (§7) entendía que no hacía falta uno nuevo y dejaba
> abierto escribirlo si la regla de "situación" se leía como una decisión de negocio nueva; el
> cierre lo escribe por eso y por el cambio en el modelo de `propuesta`. `[OBSERVED]` El código ya
> implementa esta decisión (commit `1557422c1`, migración `V24__sugerencias_de_propuesta.sql`,
> OpenAPI de Donaciones 1.10.0). Código existente no es aprobación: si el revisor rechaza el ADR,
> puede hacer falta revertir.

## Contexto y Problema

`[OBSERVED]` Antes de R11-24, `GestorPropuestasDeAsignacion` corría los dos algoritmos
(compatibilidad semántica y prioridad de subatendidos), consolidaba y guardaba la propuesta sin
recordar quién la sugirió (`V11__propuestas.sql`). El cliente leía todas las propuestas y todas
las ejecuciones, dos veces, con una llamada por necesidad, y cruzaba en memoria las que compiten
por la misma donación ([DTI-40](../DEUDA_TECNICA.md#dti-40), ítems 1 y 2). El usuario pidió ver qué
propuso cada algoritmo y la variante C, con pestañas Propuestas, Conflictos y Destino.

Problema: ¿dónde se guarda el origen de una propuesta, y quién decide si una propuesta pendiente
está libre, compite con otra o ya no alcanza, sin que el cliente traiga todas las pendientes?

**Gate A:** decisión nueva en dos puntos: el modelo del origen de una propuesta y la regla de
situación, que antes no existía en el servicio. **Gate B:** cambia el modelo de datos del agregado
`Propuesta` y el contrato REST de `/api/asignaciones` (aditivo en los campos, incompatible en el
comportamiento sin página, como R11-01).

## Alternativas Consideradas

Para el origen:

1. **Tabla hija `sugerencia_propuesta`**, una fila por algoritmo (elegida).
2. **Dos columnas fijas en `propuesta`**, una por algoritmo.
3. **Un texto JSON** en `propuesta`.

Para la situación:

- a. **La calcula Donaciones** para la página pedida (elegida).
- b. **La calcula el cliente** cruzando las propuestas.

Para los totales de las pestañas:

- i. **Una ruta de resumen** (`/propuestas/resumen`) (elegida).
- ii. **Tres pedidos con `size=1`** para leer tres `X-Total-Count`.

## Resultado de la Decisión

Alternativas 1, a e i:

| Regla | Valor |
|---|---|
| Base | `V24`: `sugerencia_propuesta (id_propuesta, algoritmo, puesto, de_cuantas, eligio_las_donaciones)`, clave `(id_propuesta, algoritmo)`, `1 ≤ puesto ≤ de_cuantas`; índices por `(algoritmo, puesto)` y `propuesta (estado, fecha_creacion DESC)`. Las propuestas anteriores no tienen filas |
| Origen | El puesto es el orden en la lista de ese algoritmo en esa corrida, con cuántas propuso. Si los dos sugieren la misma necesidad, la propuesta que queda lleva las dos sugerencias y una sola con `eligioLasDonaciones: true` |
| Situación | Solo en las pendientes: `SIN_STOCK` si una fragmentación pide más que lo que hay en el depósito; si no, `EN_CONFLICTO` si pide más que lo que queda descontando las otras pendientes o si otra pendiente cubre la misma necesidad; si no, `LIBRE` |
| Listado | `GET /api/asignaciones/propuestas` con `estado`, `situacion`, `algoritmo` (`COMPATIBILIDAD_SEMANTICA`, `PRIORIDAD_SUBATENDIDOS`, `AMBOS`), `donacion`, `orden` (`puesto` o `reciente`), `page`, `size`; total en `X-Total-Count`; sin parámetros, la página 0 de 20 |
| Nuevas rutas | `GET /api/asignaciones/propuestas/resumen` (pendientes, libres, en conflicto, sin stock, como conteos) y `GET /api/asignaciones/donaciones` (las donaciones en juego, paginadas). `GET /ejecuciones` pagina de la más reciente a la más vieja |
| Campos nuevos | `sugeridaPor` (nunca null: `[]` en las viejas), `situacion`, `otrasPendientesMismaNecesidad`, `necesidad.entidadId`, `entidadNombre` y `unidad`, `fragmentaciones[].disponible` y `pedidaEnOtras`; en `GrupoFragmentacionDTO`, `otrasPropuestas`. Todos aditivos |
| Lectura | Por lote para la página (sin una consulta por fila), como pide el ADR de SQL nativo y lectura por lote |
| Cliente | Cada pestaña es una dirección del servidor que pide una página (de a 10; las decididas, de a 20), el resumen y la última ejecución (`AdminOperacionAsignaciones`). Se borran las variantes A, B y C y `asignaciones.js` |

**Por qué:**

- Con columnas fijas hay que migrar otra vez al sumar un algoritmo, y R11-B05 va a elegir cuál
  correr; un JSON no se filtra ni se ordena en la base, que es lo que pide la pestaña por algoritmo.
- El puesto es lo que cada algoritmo ya produce; un puntaje numérico existe solo en la
  compatibilidad semántica y habría que inventarlo para el otro.
- Encontrar los choques en el cliente obliga a traer todas las pendientes (regla 1 del backlog).
- Un resumen con los cuatro números es un pedido; tres pedidos con `size=1` son tres consultas de
  listado para leer totales.

## Consecuencias

**Positivas**

- El admin ve, en cada propuesta, quién la sugirió y en qué puesto ("3.º de 10") y los conflictos
  ya calculados. Ningún pedido de Asignaciones trae la lista completa.
- El dato queda listo para R11-B05 (elegir qué algoritmo correr) y R11-35.

**Negativas**

- La situación depende del depósito y de las otras pendientes en el momento de leer: dos admins
  que deciden a la vez pueden ver una situación que cambió. `[INFERRED]` El aviso "pasaron a
  Conflictos" se calcula comparando el resumen antes y después del `PUT`, así que con decisiones
  concurrentes puede contar cambios ajenos.
- Sin página, `/propuestas` y `/ejecuciones` devuelven 20: el seeder (`demo/seeder/seeder/vivo.py`)
  y tres colecciones de Postman pasaron a pedir `orden=reciente&size=100`. Anotado en
  `cambios-incompatibles-aceptados.txt`.
- `[OBSERVED]` Donaciones no rechaza aprobar una propuesta `SIN_STOCK`: hoy la protege solo la
  pantalla, que esconde el botón (duda 2 de la spec, para un lote corto después).
- Las ejecuciones siguen en memoria (`AsignacionesRepositoryEnMemoria`): después de un reinicio,
  "Última asignación" dice que nunca corrió (duda 3, al backlog).
- `[OBSERVED]` A7 (las mismas sentencias para una página de 10 que para una de 1) no tiene un test
  que las cuente ([DTI-42](../DEUDA_TECNICA.md#dti-42)).

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| **1. Tabla hija** | Un algoritmo nuevo no migra; filtra y ordena en la base | Una tabla más que leer por lote |
| 2. Columnas fijas | Sin `JOIN` | Una migración por algoritmo nuevo |
| 3. JSON | Flexible | No se filtra ni se ordena en la base |
| **a. Situación en Donaciones** | El cliente no cruza; consistente entre pestañas | La regla de negocio crece en el servicio |
| b. Situación en el cliente | El servicio no cambia | Todas las pendientes en cada visita |
| **i. Resumen** | Un pedido, cuatro conteos | Una ruta más |
| ii. Tres `size=1` | Sin ruta nueva | Tres consultas de listado |

## Relaciones

- **Aplica:** [listados de Donaciones siempre paginados](./20261111-listados-paginados-de-donaciones-sin-lista-completa.md),
  [consultas de listado en SQL nativo y lectura por lote](./20261111-consultas-de-listado-en-sql-nativo-y-lectura-por-lote.md)
  y [cifras como conteos en un resumen](./20261111-cifras-del-admin-como-conteos-en-un-resumen.md).
- **Respeta:** el [Strategy del gestor de algoritmos](./20260615-strategy-gestor-algoritmos.md), el
  [Template Method del algoritmo de asignación](./20260615-template-method-algoritmo-asignacion.md) y la
  [resolución de conflictos en la consolidación](./20260901-resolucion-de-conflictos-en-consolidacion-de-propuestas-de-asignacion.md):
  los algoritmos no cambian, el gestor guarda lo que ya producían.
- **Spec y ficha:** [R11-p1-asignaciones](../../specs/completed/R11-p1-asignaciones-spec.md) ·
  [ficha de flujo](../../interfaz/flujos/r11-p1-asignaciones.md).
- **Deuda:** [DTI-40](../DEUDA_TECNICA.md#dti-40) (ítems 1 y 2, la parte de Asignaciones saldada) y
  [DTI-42](../DEUDA_TECNICA.md#dti-42).
