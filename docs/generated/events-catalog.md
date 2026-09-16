# Catálogo de Eventos Asíncronos y Mensajería RabbitMQ — DonaTrack

> **Topología:** Exchange `donatrack.events` (Topic/Direct Exchange)
> **Fuente Canónica:** [`docs/arquitectura/contratos/schemas/`](../arquitectura/contratos/schemas/)

<!-- AUTO-GENERATED: DO NOT EDIT MANUALLY -->

## 1. Matriz de Mensajería Inter-Servicio

| Evento DTO | Routing Key | Productor | Consumidores | Schema Canónico |
|---|---|---|---|---|
| `EventoDonanteRegistradoDTO` | `donante.registrado` | `donaciones-service` | `notificaciones-service` | [`evento-notificable.schema.json`](../arquitectura/contratos/schemas/evento-notificable.schema.json) |
| `EventoDonanteInactivoDTO` | `donante.inactivo` | `incentivos-service` | `notificaciones-service` | [`evento-notificable.schema.json`](../arquitectura/contratos/schemas/evento-notificable.schema.json) |
| `EventoMisionCumplidaDTO` | `mision.cumplida` | `incentivos-service` | `notificaciones-service` | [`evento-notificable.schema.json`](../arquitectura/contratos/schemas/evento-notificable.schema.json) |
| `EventoSubioCategoriaDTO` | `categoria.ascenso` | `incentivos-service` | `notificaciones-service` | [`evento-notificable.schema.json`](../arquitectura/contratos/schemas/evento-notificable.schema.json) |
| `EventoRutaAsignadaDTO` | `ruta.asignada` | `logistica-service` | `donaciones-service`, `notificaciones-service` | [`evento-ruta-asignada.schema.json`](../arquitectura/contratos/schemas/evento-ruta-asignada.schema.json) |
| `EventoRutaIniciadaDTO` | `ruta.iniciada` | `logistica-service` | `donaciones-service` | [`evento-ruta-iniciada.schema.json`](../arquitectura/contratos/schemas/evento-ruta-iniciada.schema.json) |
| `EventoEntregaExitosaDTO` | `entrega.exitosa` | `logistica-service` | `donaciones-service`, `incentivos-service`, `notificaciones-service` | [`evento-entrega-exitosa.schema.json`](../arquitectura/contratos/schemas/evento-entrega-exitosa.schema.json) |
| `EventoEntregaFallidaDTO` | `entrega.fallida` | `logistica-service` | `donaciones-service`, `notificaciones-service` | [`evento-entrega-fallida.schema.json`](../arquitectura/contratos/schemas/evento-entrega-fallida.schema.json) |
| `PersonaReplicaDTO` | `persona.replica` | `donaciones-service` | `logistica-service`, `incentivos-service`, `notificaciones-service` | [`persona-replica.schema.json`](../arquitectura/contratos/schemas/persona-replica.schema.json) |

## 2. Descripción de Eventos y Responsabilidades

### `EventoDonanteRegistradoDTO`
* **Exchange:** `donatrack.events`
* **Routing Key:** `donante.registrado`
* **Publicador:** `donaciones-service`
* **Propósito:** Emitido cuando se da de alta un nuevo donante. Despacha credenciales iniciales de acceso.
* **Schema de Validación:** [`docs/arquitectura/contratos/schemas/evento-notificable.schema.json`](../arquitectura/contratos/schemas/evento-notificable.schema.json)

### `EventoDonanteInactivoDTO`
* **Exchange:** `donatrack.events`
* **Routing Key:** `donante.inactivo`
* **Publicador:** `incentivos-service`
* **Propósito:** Emitido cuando un donante supera el umbral de inactividad de donaciones configurado.
* **Schema de Validación:** [`docs/arquitectura/contratos/schemas/evento-notificable.schema.json`](../arquitectura/contratos/schemas/evento-notificable.schema.json)

### `EventoMisionCumplidaDTO`
* **Exchange:** `donatrack.events`
* **Routing Key:** `mision.cumplida`
* **Publicador:** `incentivos-service`
* **Propósito:** Emitido cuando un donante completa una misión de fidelización (racha o volumen).
* **Schema de Validación:** [`docs/arquitectura/contratos/schemas/evento-notificable.schema.json`](../arquitectura/contratos/schemas/evento-notificable.schema.json)

### `EventoSubioCategoriaDTO`
* **Exchange:** `donatrack.events`
* **Routing Key:** `categoria.ascenso`
* **Publicador:** `incentivos-service`
* **Propósito:** Emitido cuando un donante asciende de categoría de fidelización.
* **Schema de Validación:** [`docs/arquitectura/contratos/schemas/evento-notificable.schema.json`](../arquitectura/contratos/schemas/evento-notificable.schema.json)

### `EventoRutaAsignadaDTO`
* **Exchange:** `donatrack.events`
* **Routing Key:** `ruta.asignada`
* **Publicador:** `logistica-service`
* **Propósito:** Emitido al consolidar y asignar una ruta logística para traslado de donaciones.
* **Schema de Validación:** [`docs/arquitectura/contratos/schemas/evento-ruta-asignada.schema.json`](../arquitectura/contratos/schemas/evento-ruta-asignada.schema.json)

### `EventoRutaIniciadaDTO`
* **Exchange:** `donatrack.events`
* **Routing Key:** `ruta.iniciada`
* **Publicador:** `logistica-service`
* **Propósito:** Emitido cuando el transporte inicia el recorrido de la ruta asignada.
* **Schema de Validación:** [`docs/arquitectura/contratos/schemas/evento-ruta-iniciada.schema.json`](../arquitectura/contratos/schemas/evento-ruta-iniciada.schema.json)

### `EventoEntregaExitosaDTO`
* **Exchange:** `donatrack.events`
* **Routing Key:** `entrega.exitosa`
* **Publicador:** `logistica-service`
* **Propósito:** Notifica la recepción exitosa de la donación en la entidad beneficiaria.
* **Schema de Validación:** [`docs/arquitectura/contratos/schemas/evento-entrega-exitosa.schema.json`](../arquitectura/contratos/schemas/evento-entrega-exitosa.schema.json)

### `EventoEntregaFallidaDTO`
* **Exchange:** `donatrack.events`
* **Routing Key:** `entrega.fallida`
* **Publicador:** `logistica-service`
* **Propósito:** Notifica el fracaso del intento de entrega de la donación, activando justificación.
* **Schema de Validación:** [`docs/arquitectura/contratos/schemas/evento-entrega-fallida.schema.json`](../arquitectura/contratos/schemas/evento-entrega-fallida.schema.json)

### `PersonaReplicaDTO`
* **Exchange:** `donatrack.events`
* **Routing Key:** `persona.replica`
* **Publicador:** `donaciones-service`
* **Propósito:** Sincronización eventual de datos de contacto y roles de personas entre bounded contexts.
* **Schema de Validación:** [`docs/arquitectura/contratos/schemas/persona-replica.schema.json`](../arquitectura/contratos/schemas/persona-replica.schema.json)

## 3. Esquemas JSON de Eventos y Payloads Registrados

| Schema File | Título | Tipo | Propiedades Obligatorias |
|---|---|---|---|
| [`cambio-estado-donacion-request.schema.json`](../arquitectura/contratos/schemas/cambio-estado-donacion-request.schema.json) | `CambioEstadoDonacionIndependienteRequestDTO` | `object` | `estado` |
| [`cambio-estado-entrega-request.schema.json`](../arquitectura/contratos/schemas/cambio-estado-entrega-request.schema.json) | `CambioEstadoEntregaRequestDTO` | `object` | `estado` |
| [`crear-entrega-request.schema.json`](../arquitectura/contratos/schemas/crear-entrega-request.schema.json) | `CrearEntregaRequestDTO` | `object` | `idDonacion`, `idBeneficiaria`, `destino`, `volumenTotalM3` |
| [`donacion-independiente-response.schema.json`](../arquitectura/contratos/schemas/donacion-independiente-response.schema.json) | `DonacionIndependienteResponseDTO` | `object` | `id`, `donacionOriginalId`, `estadoActual`, `fechaRegistro` |
| [`entrega-response.schema.json`](../arquitectura/contratos/schemas/entrega-response.schema.json) | `EntregaResponseDTO` | `object` | `id`, `idDonacion`, `idBeneficiaria`, `estadoActual` |
| [`evento-entrega-exitosa.schema.json`](../arquitectura/contratos/schemas/evento-entrega-exitosa.schema.json) | `EventoEntregaExitosa` | `object` | `entregaId`, `donacionIndependienteId`, `fechaEntrega` |
| [`evento-entrega-fallida.schema.json`](../arquitectura/contratos/schemas/evento-entrega-fallida.schema.json) | `EventoEntregaFallida` | `object` | `entregaId`, `donacionIndependienteId`, `fechaFalla`, `replanificable` |
| [`evento-notificable.schema.json`](../arquitectura/contratos/schemas/evento-notificable.schema.json) | `EventoNotificableDTO` | `polymorphic (oneOf)` | *(definido en subschemas)* |
| [`evento-ruta-asignada.schema.json`](../arquitectura/contratos/schemas/evento-ruta-asignada.schema.json) | `EventoRutaAsignada` | `object` | `rutaId`, `donacionIndependienteId`, `fechaAsignacion` |
| [`evento-ruta-iniciada.schema.json`](../arquitectura/contratos/schemas/evento-ruta-iniciada.schema.json) | `EventoRutaIniciada` | `object` | `rutaId`, `camionId`, `patenteCamion`, `donacionesIndependientesIds`, `fechaInicio` |
| [`persona-replica.schema.json`](../arquitectura/contratos/schemas/persona-replica.schema.json) | `PersonaReplicaDTO` | `object` | `id`, `denominacion`, `tipoPersona` |

---
*Generado mecánicamente por DonaTrack Knowledge Engine.*
