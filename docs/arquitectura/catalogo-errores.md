# Catálogo de Errores — DonaTrack

> **Fuente:** [`ErrorCatalog.java`](../../common-lib/src/main/java/grupo5/common/exceptions/ErrorCatalog.java) · [`GlobalExceptionHandler.java`](../../common-lib/src/main/java/grupo5/common/handlers/GlobalExceptionHandler.java)
> Todos los datos son [OBSERVED] — extraídos directamente del código fuente.

---

## Formato de respuesta de error

```json
{
  "code":      "ERR-VAL-101",
  "type":      "ValidationException",
  "details":   "Descripción del error",
  "traceId":   "a3f8c2...",
  "timestamp": "2026-09-06T05:44:26",
  "errors": [
    { "field": "nombre", "message": "no puede estar vacío", "rejectedValue": "" }
  ]
}
```

> `errors` solo aparece en errores de validación de Bean (Jakarta Validation). `traceId` se resuelve desde MDC (`traceId` / `trace_id`) o se genera como UUID aleatorio.
> Clase: [`ErrorResponse.java`](../../common-lib/src/main/java/grupo5/common/responses/ErrorResponse.java) — record con `@JsonInclude(NON_NULL)`.

---

## Excepciones de dominio

| Clase | Extiende | HTTP Status asignado | Descripción |
|---|---|---|---|
| `ValidationException` | `DonaTrackException` | `400 Bad Request` | Precondición de negocio violada (campo nulo, valor inválido, etc.) |
| `BusinessStateException` | `DonaTrackException` | `409 Conflict` (ver nota*) | Transición de estado ilegal o violación de regla de negocio con estado |
| `RecursoNoEncontradoException` | `DonaTrackException` | `404 Not Found` | Recurso buscado por ID no existe |
| `InfrastructureException` | `DonaTrackException` | `500 Internal Server Error` | Fallo en capa de infraestructura (CSV, conexiones externas) |

> \* `BusinessStateException` devuelve `404 Not Found` para los códigos `ERR-EST-702` y `ERR-EST-708` (ver handler).

---

## ERR-INF — Infraestructura

| Código | Constante enum | Descripción | HTTP Status | Excepción |
|---|---|---|---|---|
| `ERR-INF-001` | `CSV_READ_ERROR` | Error al leer un archivo CSV | `500` | `InfrastructureException` |
| `ERR-INF-002` | `CSV_PROCESS_ERROR` | Error al procesar el contenido de un CSV | `500` | `InfrastructureException` |
| `ERR-INF-710` | `NOTIFICACION_INCENTIVOS_ERROR` | Fallo al enviar notificación de incentivos | `500` | `InfrastructureException` |

---

## ERR-CSR — Comunes / Transversales

| Código | Constante enum | Descripción | HTTP Status | Excepción |
|---|---|---|---|---|
| `ERR-CSR-001` | `RECURSO_NO_ENCONTRADO` | El recurso solicitado no existe (búsqueda por UUID) | `404` | `RecursoNoEncontradoException` |
| `ERR-CSR-002` | `ARGUMENTO_NULO` | Argumento obligatorio recibido como `null` | `400` | `ValidationException` |
| `ERR-CSR-003` | `ARGUMENTO_INVALIDO` | Argumento con valor inválido (también usado por bean validation, headers faltantes, type mismatch, JSON malformado) | `400` | `ValidationException` / Spring MVC |
| `ERR-CSR-500` | `ERROR_INTERNO` | Error interno del servidor (fallback genérico, FeignException) | `500` | `Exception` / `FeignException` |

---

## ERR-VAL — Validaciones de dominio

### Personas y Dirección (1xx)

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-101` | `HUMANA_NOMBRE_VACIO` | Nombre de persona humana vacío o nulo | `400` |
| `ERR-VAL-102` | `HUMANA_APELLIDO_VACIO` | Apellido de persona humana vacío o nulo | `400` |
| `ERR-VAL-103` | `HUMANA_FECHA_NACIMIENTO_FUTURA` | Fecha de nacimiento está en el futuro | `400` |
| `ERR-VAL-104` | `JURIDICA_SIN_REPRESENTANTE_INICIAL` | Persona jurídica creada sin representante | `400` |
| `ERR-VAL-105` | `JURIDICA_AGREGAR_REPRESENTANTE_NULO` | Representante a agregar es nulo | `400` |
| `ERR-VAL-106` | `JURIDICA_QUITAR_REPRESENTANTE_INEXISTENTE` | Representante a quitar no pertenece a la jurídica | `400` |
| `ERR-VAL-108` | `DIRECCION_CALLE_VACIA` | Calle de dirección vacía o nula | `400` |
| `ERR-VAL-109` | `DIRECCION_ALTURA_INVALIDA` | Altura de dirección inválida | `400` |
| `ERR-VAL-110` | `DIRECCION_CODIGO_POSTAL_VACIO` | Código postal vacío o nulo | `400` |
| `ERR-VAL-111` | `DIRECCION_LOCALIDAD_NULA` | Localidad de dirección nula | `400` |

### Donantes y Donaciones (2xx)

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-201` | `DONANTE_SIN_PERSONA` | Donante creado sin persona asociada | `400` |
| `ERR-VAL-202` | `DONACION_SIN_DONANTE` | Donación creada sin donante | `400` |
| `ERR-VAL-203` | `DONACION_ITEM_NULO` | Ítem a agregar a la donación es nulo | `400` |
| `ERR-VAL-204` | `DONACION_ITEM_YA_AGREGADO` | El ítem ya está incluido en la donación | `400` |
| `ERR-VAL-205` | `DONACION_ITEM_NO_PERTENECE` | El ítem no pertenece a esta donación | `400` |
| `ERR-VAL-206` | `ITEM_DONACION_SIN_BIEN` | Ítem de donación sin bien asociado | `400` |
| `ERR-VAL-207` | `ITEM_DONACION_CANTIDAD_INVALIDA` | Cantidad del ítem de donación inválida | `400` |
| `ERR-VAL-208` | `ESTADO_ARCHIVO_INVALIDO` | Estado de archivo CSV inválido | `400` |
| `ERR-VAL-209` | `DEPOSITO_NOMBRE_NULO` | Nombre de depósito nulo o vacío | `400` |
| `ERR-VAL-210` | `DEPOSITO_DIRECCION_NULA` | Dirección de depósito nula | `400` |

### Bienes y Categorías (3xx)

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-300` | `DESCRIPCION_BIEN_VACIA` | Descripción del bien vacía | `400` |
| `ERR-VAL-301` | `BIEN_VENCIMIENTO_REQUERIDO` | El bien requiere fecha de vencimiento | `400` |
| `ERR-VAL-302` | `BIEN_VENCIMIENTO_NO_PERMITIDO` | El bien no admite fecha de vencimiento | `400` |
| `ERR-VAL-305` | `BIEN_ESTADO_REQUERIDO` | Estado del bien es requerido | `400` |
| `ERR-VAL-306` | `CATEGORIA_SIN_NOMBRE` | Categoría sin nombre definido | `400` |
| `ERR-VAL-307` | `CATEGORIA_SIN_USO_DEFINIDO` | Categoría sin tipo de uso definido | `400` |
| `ERR-VAL-308` | `CATEGORIA_SIN_VENCIMIENTO_DEFINIDO` | Categoría sin política de vencimiento definida | `400` |
| `ERR-VAL-309` | `CATEGORIA_SIN_UNIDAD` | Categoría sin tipo de unidad definido | `400` |
| `ERR-VAL-310` | `SUBCATEGORIA_SIN_CATEGORIA` | Subcategoría sin categoría padre | `400` |
| `ERR-VAL-311` | `SUBCATEGORIA_SIN_NOMBRE` | Subcategoría sin nombre | `400` |
| `ERR-VAL-312` | `BIEN_NORMALIZADO_SIN_BIEN` | Bien normalizado sin bien base asociado | `400` |
| `ERR-VAL-313` | `BIEN_NORMALIZADO_SIN_SUBCATEGORIA` | Bien normalizado sin subcategoría | `400` |
| `ERR-VAL-314` | `BIEN_NORMALIZADO_RANGO_CONFIANZA` | Rango de confianza del bien normalizado inválido | `400` |
| `ERR-VAL-315` | `BIEN_NORMALIZADO_SIN_ESTADO` | Bien normalizado sin estado definido | `400` |
| `ERR-VAL-316` | `DIMENSIONES_BIEN_INVALIDAS` | Dimensiones del bien inválidas | `400` |

### Segmentaciones (4xx)

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-402` | `DONACION_INDEPENDIENTE_SUBCATEGORIA_NULA` | Subcategoría nula en donación independiente | `400` |
| `ERR-VAL-403` | `DONACION_INDEPENDIENTE_ORIGINAL_NULA` | Donación original nula al crear independiente | `400` |
| `ERR-VAL-404` | `DONACION_INDEPENDIENTE_AGREGAR_ITEM_NULO` | Ítem nulo al agregar a donación independiente | `400` |
| `ERR-VAL-405` | `DONACION_INDEPENDIENTE_QUITAR_ITEM_INEXISTENTE` | Ítem no pertenece a la donación independiente | `400` |
| `ERR-VAL-407` | `ITEM_DONACION_INDEPENDIENTE_SIN_BIEN` | Ítem de donación independiente sin bien | `400` |
| `ERR-VAL-408` | `ITEM_DONACION_INDEPENDIENTE_CANTIDAD_INVALIDA` | Cantidad inválida en ítem de donación independiente | `400` |
| `ERR-VAL-410` | `ESTADO_DONACION_TRANSICION_INVALIDA` | Transición de estado de donación no permitida | `400` |
| `ERR-VAL-411` | `DONACION_INDEPENDIENTE_ASIGNACION_SIN_NECESIDAD` | Asignación de donación independiente sin necesidad | `400` |
| `ERR-VAL-412` | `DONACION_INDEPENDIENTE_FALLA_SIN_JUSTIFICACION` | Fallo de donación independiente sin justificación | `400` |

### Beneficiarios y Necesidades (5xx)

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-501` | `FECHA_ASIGNACION_FUTURA` | Fecha de asignación está en el futuro | `400` |
| `ERR-VAL-502` | `ENTIDAD_BENEFICIARIA_SIN_PERSONA_JURIDICA` | Entidad beneficiaria sin persona jurídica | `400` |
| `ERR-VAL-503` | `AGREGAR_NECESIDAD_NULA` | Necesidad a agregar es nula | `400` |
| `ERR-VAL-504` | `NECESIDAD_YA_REGISTRADA` | La necesidad ya está registrada en la entidad | `400` |
| `ERR-VAL-505` | `NECESIDAD_NO_PERTENECE_A_ENTIDAD` | La necesidad no pertenece a esta entidad | `400` |
| `ERR-VAL-506` | `NECESIDAD_SIN_SUBCATEGORIA` | Necesidad sin subcategoría definida | `400` |
| `ERR-VAL-507` | `CANTIDAD_NECESITADA_INVALIDA` | Cantidad necesitada inválida (≤ 0) | `400` |
| `ERR-VAL-508` | `DESCRIPCION_NECESIDAD_VACIA` | Descripción de necesidad vacía | `400` |
| `ERR-VAL-509` | `ASIGNAR_DONACION_NULA` | Donación a asignar es nula | `400` |
| `ERR-VAL-510` | `DONACION_YA_ASIGNADA` | La donación ya fue asignada a esta necesidad | `400` |
| `ERR-VAL-511` | `DONACION_NO_PERTENECE_A_NECESIDAD` | La donación no pertenece a esta necesidad | `400` |
| `ERR-VAL-512` | `NECESIDAD_RECURRENTE_SIN_PERIODO` | Necesidad recurrente sin periodo definido | `400` |
| `ERR-VAL-513` | `FECHA_INICIO_NULA` | Fecha de inicio nula | `400` |
| `ERR-VAL-514` | `FECHA_INICIO_FUTURA` | Fecha de inicio está en el futuro | `400` |
| `ERR-VAL-516` | `PERIODO_DONACION_NULA` | Donación de periodo es nula | `400` |

### Matchmaking (6xx)

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-601` | `PROPUESTA_FRAGMENTACION_DONACION_NULA` | Donación nula en propuesta de fragmentación | `400` |
| `ERR-VAL-602` | `PROPUESTA_FRAGMENTACION_CANTIDAD_INVALIDA` | Cantidad inválida en propuesta de fragmentación | `400` |
| `ERR-VAL-603` | `PROPUESTA_CONFIRMAR_SIN_NECESIDAD` | Confirmación de propuesta sin necesidad asociada | `400` |
| `ERR-VAL-604` | `STOCK_LISTA_DONACIONES_NULA` | Lista de donaciones nula al crear stock | `400` |
| `ERR-VAL-605` | `ALGORITMO_NECESIDAD_NULA` | Necesidad nula en algoritmo de matchmaking | `400` |
| `ERR-VAL-606` | `ALGORITMO_DONACIONES_NULAS` | Lista de donaciones nula en algoritmo | `400` |
| `ERR-VAL-607` | `ALGORITMO_NECESIDADES_NULAS` | Lista de necesidades nula en algoritmo | `400` |

### Incentivos (7xx)

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-701` | `DONANTE_INCENTIVOS_ID_NULO` | ID de donante nulo al consultar incentivos | `400` |
| `ERR-VAL-703` | `INSIGNIA_SIN_NOMBRE` | Insignia sin nombre definido | `400` |
| `ERR-VAL-704` | `INSIGNIA_NULA` | Insignia nula | `400` |
| `ERR-VAL-705` | `EVENTO_DONACION_SIN_FECHA` | Evento de donación sin fecha | `400` |
| `ERR-VAL-706` | `EVENTO_DONACION_FECHA_FUTURA` | Fecha de evento de donación está en el futuro | `400` |
| `ERR-VAL-707` | `EVENTO_DONACION_CANTIDAD_INVALIDA` | Cantidad del evento de donación inválida | `400` |
| `ERR-VAL-709` | `MISION_SIN_CATEGORIA` | Misión sin categoría definida | `400` |
| `ERR-VAL-711` | `MISION_NOMBRE_INVALIDO` | Nombre de misión inválido | `400` |
| `ERR-VAL-712` | `MISION_OBJETIVO_INVALIDO` | Objetivo de misión inválido | `400` |
| `ERR-VAL-713` | `RANKING_PERIODO_NULO` | Periodo de ranking nulo | `400` |
| `ERR-VAL-714` | `RANKING_ENTRADA_NULA` | Entrada de ranking nula | `400` |
| `ERR-VAL-715` | `INACTIVIDAD_DIAS_INVALIDOS` | Días de inactividad inválidos | `400` |

### Logística (8xx)

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-801` | `ESTADO_CAMION_TRANSICION_INVALIDA` | Transición de estado de camión no permitida | `400` |
| `ERR-VAL-802` | `ESTADO_RUTA_TRANSICION_INVALIDA` | Transición de estado de ruta no permitida | `400` |
| `ERR-VAL-803` | `ESTADO_ENTREGA_TRANSICION_INVALIDA` | Transición de estado de entrega no permitida | `400` |
| `ERR-VAL-805` | `GENERADOR_RUTAS_ENTREGAS_NULAS` | Lista de entregas nula en generador de rutas | `400` |
| `ERR-VAL-806` | `GENERADOR_RUTAS_CAMIONES_NULOS` | Lista de camiones nula en generador de rutas | `400` |
| `ERR-VAL-807` | `SOLICITUD_PLANIFICACION_CANTIDAD_INVALIDA` | Cantidad inválida en solicitud de planificación | `400` |
| `ERR-VAL-808` | `SOLICITUD_PLANIFICACION_LOTE_EXCEDIDO` | Lote máximo excedido en solicitud de planificación | `400` |
| `ERR-VAL-809` | `SOLICITUD_PLANIFICACION_CALLBACK_VACIO` | URL de callback vacía en solicitud de planificación | `400` |
| `ERR-VAL-811` | `SOLICITUD_PLANIFICACION_RESULTADO_NULO` | Resultado nulo en solicitud de planificación | `400` |
| `ERR-VAL-812` | `CAMION_PATENTE_VACIA` | Patente de camión vacía | `400` |
| `ERR-VAL-813` | `CAMION_PATENTE_FORMATO_INVALIDO` | Formato de patente de camión inválido | `400` |
| `ERR-VAL-814` | `CAMION_PATENTE_DUPLICADA` | Patente de camión duplicada | `400` |
| `ERR-VAL-815` | `ESTADO_CHOFER_TRANSICION_INVALIDA` | Transición de estado de chofer no permitida | `400` |

### Notificaciones (9xx)

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-901` | `MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO` | Tipo de medio de contacto no soportado | `400` |

### Pruebas

| Código | Constante enum | Descripción | HTTP Status |
|---|---|---|---|
| `ERR-VAL-999` | `TEST_FECHA_INVALIDA` | Usado exclusivamente en tests | `400` |

---

## ERR-EST — Estado de negocio

| Código | Constante enum | Descripción | HTTP Status | Nota |
|---|---|---|---|---|
| `ERR-EST-107` | `JURIDICA_SIN_REPRESENTANTES_RESTANTES` | Operación dejaría a la jurídica sin representantes | `409` | |
| `ERR-EST-406` | `FRAGMENTACION_CANTIDAD_INSUFICIENTE` | Cantidad insuficiente para fragmentar | `409` | |
| `ERR-EST-409` | `ITEM_DONACION_INDEPENDIENTE_FRAGMENTACION_INVALIDA` | Fragmentación de ítem independiente inválida | `409` | |
| `ERR-EST-515` | `SIN_PERIODO_ACTIVO` | No existe período activo para la operación | `409` | |
| `ERR-EST-702` | `DONANTE_INCENTIVOS_NO_ENCONTRADO` | Donante no encontrado en servicio de incentivos | **`404`** | Excepción especial en handler |
| `ERR-EST-708` | `INSIGNIA_NO_ENCONTRADA` | Insignia no encontrada | **`404`** | Excepción especial en handler |
| `ERR-EST-804` | `ENTREGA_YA_ASIGNADA_A_RUTA` | La entrega ya está asignada a otra ruta | `409` | |
| `ERR-EST-810` | `SOLICITUD_PLANIFICACION_TRANSICION_INVALIDA` | Transición de estado de solicitud de planificación inválida | `409` | |

---

## Manejo de errores de Spring MVC

Estos errores no se originan en `ErrorCatalog` directamente, pero el handler los normaliza usando `ERR-CSR-003`:

| Excepción Spring | HTTP Status | Código asignado |
|---|---|---|
| `MethodArgumentNotValidException` | `400` | `ERR-CSR-003` |
| `HandlerMethodValidationException` | `400` | `ERR-CSR-003` |
| `ConstraintViolationException` | `400` | `ERR-CSR-003` |
| `MissingRequestHeaderException` | `400` | `ERR-CSR-003` |
| `MissingServletRequestParameterException` | `400` | `ERR-CSR-003` |
| `HttpMessageNotReadableException` | `400` | `ERR-CSR-003` |
| `MethodArgumentTypeMismatchException` | `400` | `ERR-CSR-003` |
| `DateTimeParseException` | `400` | `ERR-CSR-003` |
| `IllegalArgumentException` | `400` | `ERR-CSR-003` |
| `FeignException` | propagado / `502` | `ERR-CSR-500` |
| `Exception` (genérico) | `500` | `ERR-CSR-500` |

---

## Resumen por prefijo

| Prefijo | Dominio | Cantidad | HTTP Status típico |
|---|---|---|---|
| `ERR-INF` | Infraestructura | 3 | `500` |
| `ERR-CSR` | Comunes / Transversales | 4 | `400` / `404` / `500` |
| `ERR-VAL` | Validación de dominio | 93 | `400` |
| `ERR-EST` | Estado de negocio | 8 | `409` (excepto 702 y 708 → `404`) |
| **Total** | | **108** | |