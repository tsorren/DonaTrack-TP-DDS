# Contratos REST Consolidados — DonaTrack

> **Catálogo Canónico de Endpoints, Operaciones HTTP y Especificaciones OpenAPI**  
> **Ámbito:** Microservicios `donaciones-service`, `logistica-service`, `incentivos-service`, `notificaciones-service` y Shared Kernel `common-lib`.

---

## 1. Matriz de Puertos y Documentación Interactiva

Todos los microservicios exponen su documentación interactiva Swagger UI y su definición OpenAPI 3.0 mediante la autoconfiguración de `common-lib` (`DonaTrackOpenApiAutoConfiguration`):

| Microservicio | Puerto Local | Context Path | Swagger UI | OpenAPI 3.0 YAML |
|---|:---:|---|---|---|
| **`donaciones-service`** | `8081` | `/` | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [`contratos/openapi-donaciones.yaml`](./contratos/openapi-donaciones.yaml) |
| **`logistica-service`** | `8082` | `/` | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | [`contratos/openapi-logistica.yaml`](./contratos/openapi-logistica.yaml) |
| **`incentivos-service`** | `8083` | `/` | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) | [`contratos/openapi-incentivos.yaml`](./contratos/openapi-incentivos.yaml) |
| **`notificaciones-service`** | `8084` | `/` | [http://localhost:8084/swagger-ui.html](http://localhost:8084/swagger-ui.html) | [`contratos/openapi-notificaciones.yaml`](./contratos/openapi-notificaciones.yaml) |

---

## 2. Catálogo de Endpoints por Microservicio

### 2.1 `donaciones-service` (Puerto 8081)

*Documento de Dominio:* [`aggregates-donaciones.md`](./aggregates-donaciones.md)

| Método | Endpoint | Descripción | Request DTO / Schema | Códigos HTTP |
|---|---|---|---|:---:|
| `POST` | `/donaciones-independientes` | Alta de donación independiente en estado inicial `EN_DEPOSITO` | `DonacionIndependienteInputDTO` | `201`, `400` |
| `GET` | `/donaciones-independientes` | Listado paginado de donaciones independientes | — | `200` |
| `GET` | `/donaciones-independientes/{id}` | Consulta por UUID | — | `200`, `404` |
| `PATCH` | `/donaciones-independientes/{id}/estado` | Transición de estado en la máquina de 7 estados (State Pattern) | [`cambio-estado-donacion-request.schema.json`](./contratos/schemas/cambio-estado-donacion-request.schema.json) | `200`, `400`, `404` |
| `POST` | `/api/necesidades` | Registro de necesidades declaradas por entidades beneficiarias | `NecesidadInputDTO` | `201`, `400` |
| `GET` | `/api/necesidades` | Listado de necesidades activas | — | `200` |
| `POST` | `/api/asignaciones` | Ejecución de algoritmos de matching (`AlgoritmoAsignacion`) | `PropuestaAsignacionInputDTO` | `200`, `400` |
| `POST` | `/api/entidades` | Alta de entidad beneficiaria | `EntidadBeneficiariaInputDTO` | `201`, `400` |
| `POST` | `/api/donantes` | Registro de donante individual | `DonanteInputDTO` | `201`, `400` |

---

### 2.2 `logistica-service` (Puerto 8082)

*Documento de Dominio:* [`aggregates-logistica.md`](./aggregates-logistica.md)

| Método | Endpoint | Descripción | Request DTO / Schema | Códigos HTTP |
|---|---|---|---|:---:|
| `POST` | `/api/entregas` | Registrar entrega de donación para planificar | [`crear-entrega-request.schema.json`](./contratos/schemas/crear-entrega-request.schema.json) | `201`, `400` |
| `GET` | `/api/entregas/{id}` | Consulta de entrega | — | `200`, `404` |
| `PATCH` | `/api/entregas/{id}/estado` | Transición de estado de entrega | [`cambio-estado-entrega-request.schema.json`](./contratos/schemas/cambio-estado-entrega-request.schema.json) | `200`, `400`, `404` |
| `POST` | `/api/rutas` | Creación de ruta logística con paradas asociadas | `CrearRutaRequestDTO` | `201`, `400` |
| `GET` | `/api/rutas/{id}` | Consulta de ruta con URL de tracking calculada bajo demanda | — | `200`, `404` |
| `PATCH` | `/api/rutas/{id}/estado` | Cambio de estado de ruta (`INICIADA`, `COMPLETADA`) | `CambioEstadoRutaRequestDTO` | `200`, `400` |
| `POST` | `/api/camiones` | Alta de vehículo en la flota (patente, volumen, peso) | `CamionRequestDTO` | `201`, `400` |
| `POST` | `/api/choferes` | Alta de chofer asignable | `ChoferRequestDTO` | `201`, `400` |
| `POST` | `/api/logistica/planificaciones/manual` | Planificación manual de entregas en ruta | `PlanificacionManualRequestDTO` | `200`, `400` |

---

### 2.3 `incentivos-service` (Puerto 8083)

*Documento de Dominio:* [`aggregates-incentivos.md`](./aggregates-incentivos.md)

| Método | Endpoint | Descripción | Request DTO / Schema | Códigos HTTP |
|---|---|---|---|:---:|
| `GET` | `/api/incentivos/ranking` | Ranking acumulado de donantes | — | `200` |
| `GET` | `/api/incentivos/ranking/{anio}/{mes}` | Cómputo de ranking mensual mediante window functions SQL | — | `200` |
| `GET` | `/api/incentivos/donantes/{id}` | Consulta de perfil del donante, puntos acumulados e insignias | — | `200`, `404` |
| `GET` | `/api/incentivos/misiones` | Catálogo de misiones activas (GoF Template Method) | — | `200` |
| `GET` | `/api/incentivos/insignias` | Catálogo de insignias y logros disponibles | — | `200` |

---

### 2.4 `notificaciones-service` (Puerto 8084)

*Documento de Dominio:* [`aggregates-notificaciones.md`](./aggregates-notificaciones.md)

| Método | Endpoint | Descripción | Request DTO / Schema | Códigos HTTP |
|---|---|---|---|:---:|
| `POST` | `/notificaciones` | Despacho asíncrono de alertas multicanal por evento de dominio | [`evento-notificable.schema.json`](./contratos/schemas/evento-notificable.schema.json) | `202`, `400` |
| `GET` | `/notificaciones/persona/{id}` | Consulta de historial de notificaciones enviadas a una persona | — | `200` |
| `PUT` | `/api/notificaciones/personas` | Sincronización idempotente de réplica de persona y contactos | [`persona-replica.schema.json`](./contratos/schemas/persona-replica.schema.json) | `200`, `400` |
| `GET` | `/api/notificaciones/personas/{id}` | Consulta de réplica de persona | — | `200`, `404` |
| `DELETE` | `/api/notificaciones/personas/{id}` | Baja y desasociación de datos de contacto (Crypto-Shredding) | — | `204`, `404` |

---

## 3. Estándar Unificado de Manejo de Errores

Todos los microservicios heredan el manejador global de excepciones provisto por `common-lib` (`GlobalExceptionHandler`), asegurando uniformidad absoluta en los códigos de estado y cuerpo de error JSON:

```json
{
  "errorCode": "VAL_001",
  "message": "El estado es obligatorio",
  "traceId": "c3f81e05-6490-4a82-9c12-ef36c92e1041",
  "timestamp": "2026-09-05T13:48:00.000Z"
}
```

* **`traceId`:** Propagado en el header HTTP `X-Trace-Id` y capturado en el MDC de SLF4J / Logback.
* **`errorCode`:** Proveniente del catálogo unificado `ErrorCatalog`.
