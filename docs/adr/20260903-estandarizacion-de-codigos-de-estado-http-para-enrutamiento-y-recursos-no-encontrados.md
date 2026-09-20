# Estandarización de Códigos de Estado HTTP para Excepciones de Enrutamiento y Recursos en Spring Boot 3

- Status: proposed
- Date: 2026-09-03
- Deciders: Decisión Grupal
- Tags: api, rest, http, global-exception-handler, common-lib, spring-boot-3, observabilidad, contratos

## Contexto y Problema

En la arquitectura de microservicios de DonaTrack, todas las APIs REST delegan el tratamiento uniforme de excepciones en el componente centralizado `GlobalExceptionHandler` (`@RestControllerAdvice`) ubicado en el Shared Kernel `common-lib`.

Se detectó ([OBSERVED] Issue #788 / Auditoría Integral de Logística) que ante solicitudes HTTP con métodos no permitidos (por ejemplo, invocar un endpoint `PATCH` mediante `POST` o `DELETE`) o consultas a rutas no registradas (`/api/ruta-inexistente`), Spring Boot 3 / Spring 6 arroja internamente excepciones del framework (`HttpRequestMethodNotSupportedException`, `NoResourceFoundException` o `NoHandlerFoundException`).

Al no estar explícitamente capturadas, estas excepciones eran absorbidas por el interceptor genérico de rescate `@ExceptionHandler(Exception.class)`, provocando que el servidor devolviera una respuesta **`500 Internal Server Error`** en lugar de los códigos estándar **`405 Method Not Allowed`** o **`404 Not Found`**.

Este comportamiento introduce tres riesgos técnicos:
1. **Contrato HTTP engañoso:** Viola el RFC 9110 al reportar un fallo interno del servidor ante un error inherente a la construcción de la solicitud del cliente (*Client Error 4xx*).
2. **Degradación de Resiliencia en Clientes Feign:** En llamadas inter-servicio, los clientes HTTP con políticas de reintento (*Retry / Resilience4j*) interpretan el código `500` como una falla transitoria de infraestructura, reintentando inútilmente peticiones inválidas y saturando la red o abriendo *Circuit Breakers* falsamente.
3. **Contaminación de Observabilidad:** El registro a nivel `ERROR` con stacktrace completo en `handleGeneric` satura los logs y dispara falsas alarmas críticas en sistemas de monitoreo y telemetría.

## Atributos de Calidad y Drivers de Decisión

* **Corrección de Contratos REST (RFC 9110 / RFC 7807):** Transmitir con precisión semántica si el fallo corresponde al emisor de la petición (4xx) o a un defecto del servidor (5xx).
* **Observabilidad Limpia:** Eliminar falsas alertas `500` en dashboards de monitoreo generadas por errores de ruteo de clientes o escaneos automáticos.
* **Resiliencia y Eficiencia Inter-Servicio:** Prevenir bucles de reintentos innecesarios en clientes Feign al recibir respuestas 4xx definitivas.
* **Uniformidad del Shared Kernel:** Mantener una estructura de respuesta homogénea (`ErrorResponse`) en todos los microservicios del ecosistema.

## Alternativas Consideradas

* **Opción 1: Intercepción explícita y centralizada en `GlobalExceptionHandler` con `ErrorResponse` estándar (Elegida):** Mapear `HttpRequestMethodNotSupportedException` a `405 Method Not Allowed` (incluyendo lista de métodos soportados) y `NoResourceFoundException` / `NoHandlerFoundException` a `404 Not Found` bajo el catálogo `ErrorCatalog.ARGUMENTO_INVALIDO` y `ErrorCatalog.RECURSO_NO_ENCONTRADO` respectivamente.
* **Opción 2: Delegar en el manejo por defecto de Spring Boot (BasicErrorController / WhiteLabel):** Excluir estas excepciones del `@RestControllerAdvice` y permitir que el servlet de Spring resuelva los códigos 405/404 nativos.
* **Opción 3: Manejo local en cada microservicio:** Crear controladores de excepción específicos en cada uno de los cuatro servicios (`donaciones`, `logistica`, `notificaciones`, `incentivos`).

## Resultado de la Decisión

Alternativa elegida: **"Opción 1: Intercepción explícita y centralizada en `GlobalExceptionHandler` con `ErrorResponse` estándar"**

### Justificación

La Opción 1 preserva la regla de oro del Shared Kernel establecida en el ADR `20260901-limites-y-responsabilidades-del-shared-kernel-common-lib.md`, consolidando el manejo transversal sin duplicar código en los microservicios. A su vez, garantiza que las respuestas 405 y 404 mantengan la misma estructura JSON estandarizada (`ErrorResponse` con `code`, `message` y `timestamp`) que esperan los frontends y consumidores de la API, superando a la Opción 2 que produciría respuestas no homogéneas con el resto de la plataforma.

### Consecuencias Positivas

* **Semántica HTTP Correcta:** Los clientes reciben `405` ante verbos no soportados y `404` ante rutas no mapeadas.
* **Cese de Reintentos Inútiles:** Los clientes Feign identifican inmediatamente errores 4xx y no disparan reintentos automáticos ni abren circuitos de resiliencia erróneamente.
* **Higiene de Logs:** Las excepciones de ruteo se registran a nivel `WARN` con mensaje conciso, eliminando el ruido de trazas y alertas falsas en herramientas de observabilidad.
* **Retrocompatibilidad Total:** El cambio es puramente aditivo y estandariza el comportamiento para los 4 microservicios en simultáneo.

### Consecuencias Negativas

* Requiere mantener alineadas las excepciones específicas del framework ante futuras actualizaciones mayores de Spring Boot (por ejemplo, `NoResourceFoundException` introducida a partir de Spring Boot 3.2 / Spring 6).

### Validación

Se valida empíricamente mediante pruebas automatizadas:
1. **Pruebas Unitarias de Manejador:** `GlobalExceptionHandlerTest.java` en `common-lib` verifica el retorno de `405 METHOD_NOT_ALLOWED` y `404 NOT_FOUND` ante las excepciones del framework.
2. **Pruebas de Integración MockMvc:** `ValidacionHttpTest.java` en `logistica-service` verifica mediante peticiones `DELETE` o `PUT` a endpoints que solo aceptan `PATCH`/`GET` que el código de respuesta obtenido sea exactamente `405`.

## Análisis de Alternativas

### Opción 1: Intercepción explícita en `GlobalExceptionHandler`

#### Pros
* Unificación del payload `ErrorResponse` con código máquina legible.
* Beneficio automático para todos los microservicios que importan `common-lib`.
* Elimina completamente la posibilidad de que errores de cliente se transformen en `500`.

#### Contras
* Acopla levemente `GlobalExceptionHandler` a clases de excepciones de la capa web de Spring (`spring-webmvc`).

### Opción 2: Delegar en el manejo por defecto de Spring Boot

#### Pros
* No requiere código adicional en `GlobalExceptionHandler`.

#### Contras
* Respuestas heterogéneas: los errores 405/404 devolverían el esquema por defecto de Spring (`timestamp`, `status`, `error`, `path`), rompiendo el contrato estándar de DonaTrack (`code`, `message`, `details`).

### Opción 3: Manejo local en cada microservicio

#### Pros
* Aislamiento por módulo.

#### Contras
* Violación directa de DRY (Don't Repeat Yourself) y de la arquitectura de Shared Kernel de DonaTrack.