# Arquitectura de common-lib - Guía de Responsabilidades y Buenas Prácticas

Este documento detalla el alcance, las responsabilidades y los límites de diseño de la biblioteca compartida `common-lib` dentro del sistema **DonaTrack**. Se fundamenta en los principios de diseño de microservicios, bajo acoplamiento, alta cohesión y despliegue independiente.

---

## 1. Propósito de la Biblioteca Compartida
`common-lib` funciona como un módulo transversal de infraestructura y utilidades comunes. Su objetivo es evitar la duplicación de código en **conceptos transversales** (*cross-cutting concerns*), garantizando que todos los microservicios compartan la misma semántica técnica y de comunicación, sin comprometer su autonomía de negocio.

---

## 2. ¿Para qué se USA `common-lib`?

Las siguientes responsabilidades están centralizadas en `common-lib` debido a su naturaleza transversal:

### A. Jerarquía de Excepciones Técnicas y de Negocio
* **Qué incluye:** La clase base abstracta `DonaTrackException` y sus especializaciones:
  * `BusinessStateException` (para conflictos de estado/negocio).
  * `ValidationException` (para datos de entrada incorrectos).
  * `InfrastructureException` (para fallas técnicas o externas).
  * `RecursoNoEncontradoException` (excepción común de persistencia).
* **Justificación:** Centralizar la taxonomía de excepciones asegura que todo el ecosistema de microservicios comprenda y propague los mismos tipos de fallas técnicas.

### B. Catálogo de Errores de Negocio (`ErrorCatalog`)
* **Qué incluye:** El enum `ErrorCatalog` con prefijos estandarizados:
  * `ERR-INF-xxx` (errores de infraestructura/archivos).
  * `ERR-VAL-xxx` (errores de validación de datos).
  * `ERR-EST-xxx` (errores de estado de negocio).
  * `ERR-CSR-xxx` (errores comunes del ciclo de vida de la API, ej: argumento inválido o error genérico).
* **Justificación:** Provee una lista única de códigos legibles por máquina, permitiendo a los clientes de la API (frontend u otros servicios) tomar decisiones lógicas específicas sin depender de cadenas de texto.

### C. Manejo de Errores HTTP (`GlobalExceptionHandler` y `ErrorResponse`)
* **Qué incluye:**
  * El récord `ErrorResponse`, que captura información estructural (`code`, `type`, `details`, `timestamp`) sin strings de UI formateados.
  * El `@RestControllerAdvice` global que intercepta las excepciones y genera las respuestas HTTP mapeadas a los códigos de estado correspondientes (400, 404, 409, 500).
* **Justificación:** Garantiza que cualquier error en cualquier servicio del sistema se exponga al cliente con el mismo formato JSON estandarizado.

### D. Abstracciones de Persistencia en Memoria (DDD)
* **Qué incluye:**
  * La interfaz `AggregateRoot` (que encapsula el contrato de identidad mediante un `UUID`).
  * El contrato genérico `CrudRepository<T extends AggregateRoot>`.
  * La clase abstracta `CrudRepositoryEnMemoria<T extends AggregateRoot>`, que proporciona una base concurrente segura (`ConcurrentHashMap`) para persistencia volátil en Fase 1, con soporte de CRUD completo, búsquedas polimórficas y logging estructurado.
* **Justificación:** Facilita la adopción de **Arquitectura Hexagonal (Puertos y Adaptadores)** y aislamiento de dominio sin necesidad de duplicar código de infraestructura de repositorios en cada microservicio.

### E. Trazabilidad y Observabilidad Distribuida Activa
* **Qué incluye:** El paquete `grupo5.common.logging` con autoconfiguración Spring Boot:
  * `TraceResponseHeaderFilter`: Inyecta el encabezado `X-Trace-Id` en las respuestas HTTP.
  * `FeignTraceRequestInterceptor`: Propaga automáticamente el contexto de trazabilidad en llamadas síncronas entre microservicios.
  * `ControllerLoggingInterceptor`: Loguea peticiones HTTP entrantes (verbo, URI, handler).
  * `ServiceLoggingAspect` y `ScheduledJobLoggingAspect`: Registra inicio y finalización de servicios y cron jobs con span ID dedicado.
  * `MdcTaskDecorator`: Propaga el MDC (Mapped Diagnostic Context) a hilos asíncronos (`@Async`).
* **Justificación:** Garantiza visibilidad end-to-end y correlación de logs a través de todo el ecosistema distribuido.

### F. Eventos de Dominio y Documentación OpenAPI
* **Qué incluye:**
  * `AgregadoConEventos` y `EventoDeDominio` en `grupo5.common.events`: Abstracciones base para acumulación y despacho desacoplado de eventos de dominio.
  * `DonaTrackOpenApiAutoConfiguration` y `DonaTrackOpenApiProperties` en `grupo5.common.openapi`: Estandarización automática de Swagger UI y especificaciones OpenAPI 3 para todos los microservicios.

---

## 3. ¿Para qué NO se usa `common-lib`?

Para evitar caer en el anti-patrón de un **Monolito Distribuido**, los siguientes elementos están estrictamente excluidos de la librería común:

### A. Clases Base de Negocio (`BaseService` y `BaseController`)
* **Por qué NO:**
  * **Acoplamiento de Rutas HTTP:** Definir mapeos de endpoints (`@GetMapping`, etc.) en clases base compartidas restringe la flexibilidad REST de los microservicios.
  * **Violación de Liskov:** Si un servicio específico no requiere un endpoint (por ejemplo, no permite borrar mediante `DELETE`), heredar de un controlador genérico obliga a lanzar excepciones del tipo `UnsupportedOperationException`, degradando la calidad del código.
  * **Anemicidad:** Los servicios genéricos CRUD eliminan la lógica del dominio, convirtiendo la capa de servicios en un simple puente inerte hacia el repositorio.
* **Justificación:** Los controladores y servicios contienen semántica y reglas de negocio específicas de cada dominio. Deben implementarse localmente en cada microservicio.

### B. DTOs Locales de Entrada/Salida
* **Por qué NO:**
  * Los DTOs de peticiones (ej. `CrearDonacionDTO`) y respuestas son contratos de la API pública de un microservicio específico.
* **Justificación:** Compartirlos centralizadamente obliga a que cualquier cambio en los campos de la API de un microservicio requiera desplegar una nueva versión de `common-lib` y actualizar todos los demás microservicios, rompiendo el despliegue independiente.

### C. Mensajes y Textos para la Interfaz de Usuario (UI)
* **Por qué NO:**
  * Las excepciones y respuestas de error del sistema no deben contener frases formateadas para el usuario (ej: `"No se encontró la donación con id..."` en español).
* **Justificación:** El formateo y traducción de textos de UI son responsabilidades del cliente (Frontend) o de una capa de internacionalización específica de la presentación, basándose en el código máquina (`code`) retornado.

---

## 4. Decisiones de Diseño y Evolución Futura

A medida que el ecosistema de **DonaTrack** crezca hacia la Entrega 2, se planea extender `common-lib` para incorporar:

### A. Seguridad y Autenticación Estandarizada
* **Objetivo:** Evitar que cada microservicio implemente manualmente la validación de tokens de seguridad cuando se introduzca `auth-service`.
* **Implementación:**
  * Proveer utilidades compartidas para parsear y validar JWTs (JSON Web Tokens).
  * Definir filtros comunes de seguridad para delegar roles y extraer el contexto del usuario autenticado.

### B. Configuraciones de Resiliencia Avanzada
* **Objetivo:** Compartir políticas de reintentos y tolerancia a fallas en la red.
* **Implementación:**
  * Definir configuraciones compartidas de Resilience4j (Circuit Breakers, Retries y Rate Limiters) específicas para los clientes Feign definidos en la biblioteca.
