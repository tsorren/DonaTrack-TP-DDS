# Cifras del admin como conteos en una sola ruta (`GET /api/donaciones/resumen`) y contadores públicos con `count`

- Status: proposed
- Date: 2026-11-11
- Deciders: el diseñador de la spec `R11-p0-api-listados` (agente) con la aprobación del usuario (2026-11-11); `beneficiariosActivos` como conteo de entidades activas, el constructor del lote (agente, sin consulta). Pendiente de aprobación del equipo en el PR. Redactado por el agente en el cierre documental de la oleada P0
- Tags: donaciones, contrato, rest, cifras, rendimiento, cliente-liviano, r11, entrega-6

> **Escrito después del código.** Desvío acordado del ciclo ARCHITECTURAL: la corrida R11 construyó
> el lote con su [spec aprobada](../../specs/completed/R11-p0-api-listados-spec.md) y dejó los ADRs
> para el cierre documental de la oleada. `[OBSERVED]` El código ya implementa esta decisión
> (`ResumenAdminService`, `ResumenAdminDTO`, `ContadoresPublicosService`, commit `b3f8fc179`).

## Contexto y Problema

`[OBSERVED]` Antes de R11, el inicio del admin pedía todas las donaciones y todos los donantes para
contarlos en el cliente (`AdminPersonasInicio`, `AdminPersonasRest`), y los contadores públicos de
la portada recorrían todos los donantes en memoria (`ContadoresPublicosService`). La regla 1 del
[backlog](../../interfaz/backlog-revision-2026-11-11.md) dice que los contadores se piden como
números, no como listas para contar.

Problema: ¿cómo llegan al cliente las cifras del inicio del admin (por estado, entregadas en el mes,
por vencer, donantes, entidades activas, propuestas pendientes) sin traer listas?

**Gate A:** decisión nueva: una ruta de cifras agregadas que no existía y que reemplaza el cálculo
del cliente. **Gate B:** cambia la integración entre el cliente y Donaciones (qué cálculo vive en qué
lado) con un trade-off entre llamadas, frescura y costo.

## Alternativas Consideradas

1. **Contar en el cliente** sobre las listas (lo de antes).
2. **Leer el total de cada listado** con `size=1` y `X-Total-Count`.
3. **Una ruta por cifra** (`/donaciones/cuenta?estado=…`, `/donantes/cuenta`…).
4. **Una sola ruta de resumen** con todas las cifras, cada una con su consulta de conteo (elegida).
5. **Cifras precalculadas** (tabla o vista materializada, o caché con vencimiento).

## Resultado de la Decisión

Alternativa 4.

- `GET /api/donaciones/resumen`, solo `ADMIN`, devuelve `ResumenAdminDTO`: `donacionesPorEstado`
  (los estados siempre presentes, con 0), `entregadasEnElMes` (cambios a `ENTREGADA` dentro del mes
  en curso del reloj del servicio), `porVencer` (en depósito con algún bien que vence en 7 días, y la
  próxima), `donantes` (total y activos), `entidadesActivas`, `propuestasPendientes` y `calculadoEn`.
  Cada cifra es una consulta de conteo; los índices de `V21` cubren las fechas.
- `GET /api/donaciones/contadores` (público) mantiene su respuesta: `donantesActivos` pasa a un
  `count` con `EXISTS` sobre `donacion`, y `beneficiariosActivos`, a un `count` de entidades activas
  (el mismo significado que tenía, en una consulta).
- El cliente toma las cifras del inicio del admin de esta ruta (lote `p0-admin-listados`).

**Por qué:**

- Una visita al inicio hace una llamada en lugar de seis (alternativa 3) o de traer listas
  (alternativa 1).
- La alternativa 2 no sirve para "por vencer" ni para "entregadas en el mes": no hay listado con esos
  filtros.
- `calculadoEn` deja que la pantalla diga cuándo se actualizó la cifra (RF-CL-07), sin caché que
  invalidar.

## Consecuencias

**Positivas**

- En el ensayo de 20.000 filas, el inicio del admin respondió con p95 de 0,707 s durante la
  importación (cierre de P0); sus cifras coincidieron con el servicio en el recorrido con login real.
- La ruta es nueva y aditiva: no cambia ninguna existente.

**Negativas**

- Las cifras se calculan en cada visita (varias consultas de conteo); con mucho más volumen podría
  hacer falta la alternativa 5.
- `[OBSERVED]` El cliente todavía pide la lista entera de propuestas para mostrar las pendientes, aunque
  el resumen trae `propuestasPendientes`, y la flota entera de camiones para contarla: anotado en
  [DTI-40](../DEUDA_TECNICA.md#dti-40) con su lote.
- `[INFERRED]` El borde del mes de `entregadasEnElMes` sale del reloj de
  `America/Argentina/Buenos_Aires` (`ClockConfig`) mientras Hibernate guarda en UTC: falta un test
  de una entrega del último día del mes a las 22 h ([DTI-40](../DEUDA_TECNICA.md#dti-40)).
- Las solicitudes de entidades pendientes no están: viven en Auth (Keycloak).

## Análisis por alternativa

| Alternativa | A favor | En contra |
|---|---|---|
| 1. Contar en el cliente | Sin cambios en el servicio | Trae listas enteras: lo que rompía la demo |
| 2. `size=1` y el total | Sin ruta nueva | No cubre "por vencer" ni "entregadas en el mes"; una llamada por cifra |
| 3. Una ruta por cifra | Cada cifra se pide sola | Seis llamadas por visita; seis rutas que versionar |
| **4. Un resumen** | Una llamada; cada cifra un conteo; dice cuándo se calculó | Recalcula en cada visita; mezcla cifras de varios agregados en un DTO |
| 5. Precalculadas | Lectura instantánea | Invalidación y frescura que resolver, por un volumen que el conteo ya aguanta |

## Relaciones

- **Va con:** [listados paginados sin lista completa](./20261111-listados-paginados-de-donaciones-sin-lista-completa.md).
- **Respeta:** la [política de evolución de contratos](../20261028-politica-de-evolucion-de-contratos-entre-servicios.md)
  (cambio aditivo, OpenAPI 1.9.0) y las rutas públicas del
  [ADR-E6-02](../20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md): los
  contadores públicos no exponen datos personales.
