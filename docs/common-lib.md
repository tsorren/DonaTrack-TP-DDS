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

### D. Abstracciones de Persistencia y Pruebas
* **Qué incluye:**
  * La interfaz `RecursoDTO` (que encapsula el contrato de identidad mediante un `UUID`).
  * El contrato genérico `BaseRepository<T extends RecursoDTO>`.
  * La clase abstracta `BaseRepositoryEnMemoria<T extends RecursoDTO>`, que proporciona una base segura para pruebas volátiles (Fakes) con lógica de autogeneración de UUIDs (Create vs. Update) y validaciones de nulos.
* **Justificación:** Facilita la adopción de **Arquitectura Hexagonal (Puertos y Adaptadores)** y pruebas unitarias de integración rápidas sin necesidad de duplicar código de infraestructura de pruebas en cada microservicio.

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

A medida que el ecosistema de **DonaTrack** crezca, se planea extender `common-lib` para incorporar las siguientes utilidades transversales:

### A. Trazabilidad y Observabilidad Distribuida (TraceID)
* **Objetivo:** Garantizar que cada solicitud que entra al sistema pueda ser rastreada a través de todos los microservicios involucrados.
* **Implementación:**
  * Configurar interceptores de clientes HTTP (como Feign o RestTemplate) para propagar cabeceras de trazabilidad (`X-B3-TraceId` o estándar W3C `traceparent`).
  * Proveer un filtro de Servlet común para inyectar el `traceId` en el contexto de logs de Spring (MDC - Mapped Diagnostic Context).
  * Estandarizar la salida de logs en formato JSON estructurado para su ingesta por recolectores de logs (ej. ELK Stack / Grafana Loki).

### B. Seguridad y Autenticación Estandarizada
* **Objetivo:** Evitar que cada microservicio implemente manualmente la validación de tokens de seguridad.
* **Implementación:**
  * Proveer utilidades compartidas para parsear y validar JWTs (JSON Web Tokens).
  * Definir filtros comunes de seguridad para delegar roles y extraer el contexto del usuario autenticado (inyectando un `UserPrincipalDTO` común en el contexto de seguridad).

### C. Configuraciones de Resiliencia y Comunicación Inter-Servicios
* **Objetivo:** Compartir políticas de reintentos y tolerancia a fallas en la red.
* **Implementación:**
  * Definir configuraciones compartidas de Resilience4j (Circuit Breakers, Retries y Rate Limiters) específicas para los clientes Feign definidos en la biblioteca.
