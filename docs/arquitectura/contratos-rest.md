# Contratos REST Consolidados — DonaTrack

> **Catálogo Canónico de Endpoints, Operaciones HTTP y Especificaciones OpenAPI**  
> **Ámbito:** Microservicios `donaciones-service`, `logistica-service`, `incentivos-service`, `notificaciones-service` y Shared Kernel `common-lib`.

---

## 1. Matriz de Puertos y Documentación Interactiva

Todos los microservicios exponen su documentación interactiva Swagger UI y su definición OpenAPI 3.0 mediante la autoconfiguración de `common-lib` (`DonaTrackOpenApiAutoConfiguration`) y la topología de puertos definida en `docker-compose.yml` y `application.properties`:

| Microservicio | Puerto Local | Context Path | Swagger UI | OpenAPI 3.0 YAML |
|---|:---:|---|---|---|
| **`donaciones-service`** | `8080` | `/` | [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) | [`contratos/openapi-donaciones.yaml`](./contratos/openapi-donaciones.yaml) |
| **`notificaciones-service`** | `8081` | `/` | [http://localhost:8081/swagger-ui.html](http://localhost:8081/swagger-ui.html) | [`contratos/openapi-notificaciones.yaml`](./contratos/openapi-notificaciones.yaml) |
| **`incentivos-service`** | `8082` | `/` | [http://localhost:8082/swagger-ui.html](http://localhost:8082/swagger-ui.html) | [`contratos/openapi-incentivos.yaml`](./contratos/openapi-incentivos.yaml) |
| **`logistica-service`** | `8083` | `/` | [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html) | [`contratos/openapi-logistica.yaml`](./contratos/openapi-logistica.yaml) |

---

## 2. Catálogo de Endpoints por Microservicio

### 2.1 `donaciones-service` (Puerto 8080)

*Documento de Dominio:* [`aggregates-donaciones.md`](./aggregates-donaciones.md)

| Método | Endpoint | Descripción | Request DTO / Schema | Códigos HTTP |
|---|---|---|---|:---:|
| `POST` | `/api/donaciones` | Alta de donación general y desglose en ítems | `DonacionInputDTO` | `201`, `400` |
| `GET` | `/api/donaciones` | Listado de donaciones registradas | — | `200` |
| `GET` | `/api/donaciones/{id}` | Consulta de donación por ID | — | `200`, `404` |
| `GET` | `/donaciones-independientes` | Listado con filtros (`estado`, `subcategoriaId`, `donanteId`) | — | `200` |
| `GET` | `/donaciones-independientes/{id}` | Consulta de donación independiente por UUID | — | `200`, `404` |
| `PATCH` | `/donaciones-independientes/{id}/estado` | Transición en máquina de 7 estados (header `X-Actor` obligatorio) | [`cambio-estado-donacion-request.schema.json`](./contratos/schemas/cambio-estado-donacion-request.schema.json) | `200`, `400`, `404` |
| `POST` | `/api/necesidades` | Registro de necesidades de entidades beneficiarias | `NecesidadDTO` | `201`, `400` |
| `GET` | `/api/necesidades` | Listado de necesidades activas (filtros `entidadId`, `tipo`) | — | `200` |
| `GET` | `/api/necesidades/{id}` | Consulta de necesidad por ID | — | `200`, `404` |
| `PUT` | `/api/necesidades/{id}` | Actualización de necesidad existente | `NecesidadDTO` | `200`, `400`, `404` |
| `DELETE` | `/api/necesidades/{id}` | Baja de necesidad registrada | — | `204`, `404` |
| `POST` | `/api/asignaciones/ejecuciones` | Ejecución del algoritmo de asignación | — | `201`, `400` |
| `GET` | `/api/asignaciones/ejecuciones` | Historial de ejecuciones de asignación | — | `200` |
| `GET` | `/api/asignaciones/propuestas` | Listado de propuestas de asignación generadas | — | `200` |
| `PUT` | `/api/asignaciones/propuestas/{id}/estado` | Actualización de estado de propuesta | `ActualizarEstadoRequestDTO` | `200`, `400`, `404` |
| `POST` | `/api/entidades` | Alta de entidad beneficiaria | `EntidadBeneficiariaInputDTO` | `201`, `400` |
| `GET` | `/api/entidades` | Listado de entidades beneficiarias | — | `200` |
| `GET` | `/api/entidades/{id}` | Consulta de entidad beneficiaria por ID | — | `200`, `404` |
| `PUT` | `/api/entidades/{id}` | Actualización de entidad beneficiaria | `EntidadBeneficiariaInputDTO` | `200`, `400`, `404` |
| `DELETE` | `/api/entidades/{id}` | Baja de entidad beneficiaria | — | `204`, `404` |
| `POST` | `/api/donantes` | Registro de nuevo donante | `DonanteInputDTO` | `201`, `400` |
| `GET` | `/api/donantes` | Listado de donantes (filtro opcional `canal`) | — | `200` |
| `GET` | `/api/donantes/{id}` | Consulta de donante por ID | — | `200`, `404` |
| `DELETE` | `/api/donantes/{id}` | Eliminación de donante | — | `204`, `404` |
| `POST` | `/api/donantes/archivos` | Carga asíncrona de archivo/padrón de donantes (MinIO) | `ArchivoInputDTO` | `202`, `400` |
| `GET` | `/api/donantes/archivos/{id}` | Consulta de estado de procesamiento de archivo | — | `200`, `404` |
| `POST` | `/api/categorias` | Alta de categoría de donación | `CategoriaInputDTO` | `201`, `400` |
| `GET` | `/api/categorias` | Listado de categorías de donación | — | `200` |
| `GET` | `/api/categorias/{id}` | Consulta de categoría por ID | — | `200`, `404` |
| `PUT` | `/api/categorias/{id}` | Actualización de categoría existente | `CategoriaInputDTO` | `200`, `400`, `404` |
| `DELETE` | `/api/categorias/{id}` | Baja de categoría (retorna entidad eliminada en el cuerpo) | — | `200`, `404` |
| `POST` | `/api/subcategorias` | Alta de subcategoría | `SubcategoriaInputDTO` | `201`, `400` |
| `GET` | `/api/subcategorias` | Listado de subcategorías | — | `200` |
| `GET` | `/api/subcategorias/{id}` | Consulta de subcategoría por ID | — | `200`, `404` |
| `PUT` | `/api/subcategorias/{id}` | Actualización de subcategoría existente | `SubcategoriaInputDTO` | `200`, `400`, `404` |
| `DELETE` | `/api/subcategorias/{id}` | Baja de subcategoría (retorna entidad eliminada en el cuerpo) | — | `200`, `404` |
| `POST` | `/api/subcategorias/{id}/aliases` | Agregar alias semántico a una subcategoría | `AliasSubcategoriaInputDTO` | `200`, `400`, `404` |
| `DELETE` | `/api/subcategorias/{id}/aliases/{alias}` | Quitar alias semántico de una subcategoría | — | `200`, `404` |
| `GET` | `/api/items-normalizados/pendientes` | Listado de ítems normalizados pendientes de revisión | — | `200` |
| `GET` | `/api/items-normalizados/{id}` | Consulta de ítem normalizado por ID | — | `200`, `404` |
| `PATCH` | `/api/items-normalizados/{id}` | Revisión y reclasificación manual de normalización | `ItemDonacionNormalizadoPatchDTO` | `200`, `400`, `404` |
| `POST` | `/api/personas` | Alta de persona y contactos en donaciones | `PersonaInputDTO` | `201`, `400` |
| `GET` | `/api/personas` | Listado de personas (filtro opcional por `?tipo=HUMANA|JURIDICA`) | — | `200` |
| `PUT` | `/api/personas/{id}` | Actualización de datos de persona | `PersonaInputDTO` | `200`, `400`, `404` |
| `DELETE` | `/api/personas/{id}` | Baja y supresión de persona | — | `204`, `404` |

> **Notas de reconciliación con el código fuente Java (`donaciones-service`):**
> - **D1:** `/api/items-normalizados` no implementa un CRUD estándar; expone `GET /pendientes`, `GET /{id}` y `PATCH /{id}` conforme a `ItemDonacionNormalizadoController.java`.
> - **D2:** Se incorporan los endpoints de gestión de alias semánticos de subcategorías: `POST /api/subcategorias/{id}/aliases` y `DELETE /api/subcategorias/{id}/aliases/{alias}` (`SubcategoriasController.java`).
> - **D3:** `GET /api/personas/{id}` no existe en `donaciones-service` (la consulta por ID reside en `notificaciones-service` vía `GET /api/notificaciones/personas/{id}`). En `donaciones-service`, el listado general con filtro `?tipo=` cubre la consulta.
> - **D4:** `DELETE /api/categorias/{id}` y `DELETE /api/subcategorias/{id}` devuelven código `200 OK` retornando el DTO del recurso eliminado (`CategoriaOutputDTO` y `SubcategoriaOutputDTO`), en lugar de `204 No Content`.

---

### 2.2 `logistica-service` (Puerto 8083)

*Documento de Dominio:* [`aggregates-logistica.md`](./aggregates-logistica.md)

| Método | Endpoint | Descripción | Request DTO / Schema | Códigos HTTP |
|---|---|---|---|:---:|
| `POST` | `/api/entregas` | Registrar entrega de donación para planificar | [`crear-entrega-request.schema.json`](./contratos/schemas/crear-entrega-request.schema.json) | `201`, `400` |
| `GET` | `/api/entregas` | Listar entregas registradas | — | `200` |
| `GET` | `/api/entregas/{id}` | Consulta de entrega por ID | — | `200`, `404` |
| `PATCH` | `/api/entregas/{id}/estado` | Transición de estado de entrega (`PENDIENTE`, `EN_TRASLADO`, etc.) | [`cambio-estado-entrega-request.schema.json`](./contratos/schemas/cambio-estado-entrega-request.schema.json) | `200`, `400`, `404` |
| `PATCH` | `/api/entregas/{id}/fotos` | Adjuntar URL de foto de recepción | `AdjuntarFotoRecepcionRequestDTO` | `200`, `400`, `404` |
| `GET` | `/api/entregas/{id}/historial` | Historial de transiciones de una entrega | — | `200`, `404` |
| `GET` | `/api/rutas` | Listar rutas planificadas (filtro opcional `camionId`) | — | `200` |
| `GET` | `/api/rutas/{id}` | Consulta de ruta con paradas y URL de seguimiento | — | `200`, `404` |
| `GET` | `/api/rutas/{id}/entregas` | Consulta de ruta detallando entregas asignadas | — | `200`, `404` |
| `POST` | `/api/rutas/{id}/entregas` | Agregar entrega a una ruta planificada | `AgregarEntregaRutaRequestDTO` | `201`, `400`, `404` |
| `PATCH` | `/api/rutas/{id}/estado` | Cambio de estado de ruta | `CambioEstadoRutaRequestDTO` | `200`, `400`, `404` |
| `POST` | `/api/camiones` | Alta de camión en la flota (patente, capacidad, peso) | `CamionRequestDTO` | `201`, `400` |
| `GET` | `/api/camiones` | Listado de camiones | — | `200` |
| `GET` | `/api/camiones/{id}` | Consulta de camión por ID | — | `200`, `404` |
| `PATCH` | `/api/camiones/{id}/estado` | Cambio de estado operativo de camión | `CambioEstadoCamionRequestDTO` | `200`, `400`, `404` |
| `DELETE` | `/api/camiones/{id}` | Baja de camión de la flota | — | `204`, `404` |
| `POST` | `/api/choferes` | Alta de chofer | `ChoferRequestDTO` | `201`, `400` |
| `GET` | `/api/choferes` | Listado de choferes | — | `200` |
| `GET` | `/api/choferes/{id}` | Consulta de chofer por ID | — | `200`, `404` |
| `PATCH` | `/api/choferes/{id}/estado` | Cambio de estado de chofer | `CambioEstadoChoferRequestDTO` | `200`, `400`, `404` |
| `DELETE` | `/api/choferes/{id}` | Baja de chofer | — | `204`, `404` |
| `POST` | `/api/logistica/planificaciones/ejecuciones` | Disparador manual de planificación (habilitado si `manual-enabled=true`) | — | `202` |
| `GET` | `/api/logistica/planificaciones/{id}` | Consulta de estado de solicitud de planificación | — | `200`, `404` |
| `POST` | `/api/logistica/resultados` | Callback webhook para recepción de resultados del optimizador | `CallbackPlanificacionRequestDTO` | `200`, `400` |
| `POST` | `/api/logistica/callback/rutas` | Callback webhook alternativo para recepción de resultados | `CallbackPlanificacionRequestDTO` | `200`, `400` |

---

### 2.3 `incentivos-service` (Puerto 8082)

*Documento de Dominio:* [`aggregates-incentivos.md`](./aggregates-incentivos.md)

| Método | Endpoint | Descripción | Request DTO / Schema | Códigos HTTP |
|---|---|---|---|:---:|
| `GET` | `/api/incentivos/ranking/ultimo` | Último ranking mensual calculado | — | `200`, `204` |
| `GET` | `/api/incentivos/ranking/historial` | Historial de rankings mensuales | — | `200` |
| `POST` | `/api/incentivos/ranking/calcular` | Cálculo y persistencia de ranking mensual (`?periodo=YYYY-MM`) | — | `200`, `400` |
| `GET` | `/api/incentivos/ranking/posicion/{donanteId}` | Posición de un donante en ranking (`?periodo=YYYY-MM`) | — | `200`, `204` |
| `GET` | `/api/incentivos/ranking/{periodo}` | Consulta de ranking mensual por período específico (`YYYY-MM`) | — | `200`, `400`, `404` |
| `POST` | `/api/incentivos/donantes/{donanteId}` | Registro de donante en módulo incentivos | `RegistrarDonanteRequest` | `201`, `400` |
| `DELETE` | `/api/incentivos/donantes/{donanteId}` | Baja de donante en módulo incentivos | — | `204`, `404` |
| `PATCH` | `/api/incentivos/donantes/{donanteId}` | Modificación de datos de donante | `ModificarDonanteRequest` | `200`, `400`, `404` |
| `GET` | `/api/incentivos/donantes/{donanteId}` | Consulta del perfil consolidado del donante en incentivos | — | `200`, `404` |
| `GET` | `/api/incentivos/donantes/{donanteId}/ascensos` | Historial de transiciones de categoría del donante | — | `200`, `404` |
| `GET` | `/api/incentivos/donantes/{donanteId}/metricas` | Métricas de impacto, donaciones completadas y nivel del donante | — | `200`, `404` |
| `GET` | `/api/incentivos/admin/resumen` | Resumen consolidado del sistema de incentivos | — | `200` |
| `GET` | `/api/incentivos/donantes/{donanteId}/misiones` | Misiones asignadas al donante (GoF Template Method) | — | `200`, `404` |
| `GET` | `/api/incentivos/donantes/{donanteId}/insignias` | Insignias obtenidas por el donante (`?soloVisibles=true/false`) | — | `200`, `404` |
| `PUT` | `/api/incentivos/donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad` | Configurar visibilidad pública de insignia (`?visible=true/false`) | — | `200`, `404` |
| `POST` | `/api/incentivos/donaciones` | Notificar donación para evaluación de progreso de misiones | `NuevaDonacionRequest` | `200`, `400` |
| `POST` | `/api/incentivos/donaciones/exitosa` | Notificar donación exitosa para evaluación de misiones | `DonacionExitosaRequest` | `200`, `400` |
| `POST` | `/api/incentivos/evaluaciones-inactividad` | Disparo manual de evaluación de inactividad de donantes (testing/admin) | — | `200` |
| `POST` | `/api/incentivos/verificaciones-racha` | Disparo manual de verificación de vigencia de rachas (testing/admin) | — | `200` |

> **Notas de reconciliación con el código fuente Java (`incentivos-service`):**
> - **D5:** El parámetro de ruta para visibilidad de insignias utiliza la variable `{nombreInsignia}` conforme a `InsigniasController.java` (`@PathVariable String nombreInsignia`).
> - **D6:** Los endpoints de procesos batch (`POST /api/incentivos/evaluaciones-inactividad` y `POST /api/incentivos/verificaciones-racha`) operan como disparadores on-demand para pruebas automatizadas y soporte administrativo, amparados bajo deuda técnica catalogada ([DTI-09](../adr/DEUDA_TECNICA.md#dti-09-seguridad-control-de-acceso-y-asincronia-en-procesos-batch-de-incentivos)).

---

### 2.4 `notificaciones-service` (Puerto 8081)

*Documento de Dominio:* [`aggregates-notificaciones.md`](./aggregates-notificaciones.md)

| Método | Endpoint | Descripción | Request DTO / Schema | Códigos HTTP |
|---|---|---|---|:---:|
| `POST` | `/notificaciones` | Despacho asíncrono de alertas multicanal vía payload polimórfico REST | [`evento-notificable.schema.json`](./contratos/schemas/evento-notificable.schema.json) | `202`, `400` |
| `GET` | `/notificaciones/persona/{personaId}` | Consulta de historial de notificaciones generadas para una persona | — | `200` |
| `PUT` | `/api/notificaciones/personas` | Sincronización idempotente de réplica de persona y contactos | [`persona-replica.schema.json`](./contratos/schemas/persona-replica.schema.json) | `200`, `400` |
| `GET` | `/api/notificaciones/personas/{id}` | Consulta de réplica de persona | — | `200`, `404` |
| `DELETE` | `/api/notificaciones/personas/{id}` | Baja y desasociación de datos de contacto (Crypto-Shredding) | — | `204`, `404` |

---

## 3. Estándar Unificado de Manejo de Errores

Todos los microservicios heredan el manejador global de excepciones provisto por `common-lib` (`GlobalExceptionHandler`), asegurando uniformidad absoluta en los códigos de estado y cuerpo de error JSON estructurado en `ErrorResponse`:

```json
{
  "code": "ERR-VAL-001",
  "type": "ValidationException",
  "details": "El estado es obligatorio",
  "traceId": "c3f81e05-6490-4a82-9c12-ef36c92e1041",
  "timestamp": "2026-09-05T13:48:00.000Z",
  "errors": [
    {
      "field": "estado",
      "message": "El estado es obligatorio",
      "rejectedValue": null
    }
  ]
}
```

* **`code`:** Código canónico estandarizado proveniente de `ErrorCatalog` (ej. `ERR-VAL-001`, `ERR-ENT-001`).
* **`type`:** Nombre de la clase de excepción lanzada (`ValidationException`, `RecursoNoEncontradoException`, etc.).
* **`details`:** Mensaje explicativo amigable para el consumidor de la API.
* **`traceId`:** Identificador correlativo distribuido propagado en el header HTTP `X-Trace-Id` y capturado en el MDC de SLF4J / Logback.
* **`timestamp`:** Marca de tiempo ISO-8601 en UTC del momento de ocurrencia del error.
* **`errors`:** (Opcional) Detalle de fallas a nivel de campo en validaciones de entrada (`FieldErrorDTO`).
