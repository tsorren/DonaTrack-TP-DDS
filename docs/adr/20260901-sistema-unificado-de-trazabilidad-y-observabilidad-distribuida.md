# Sistema Unificado de Trazabilidad y Observabilidad Distribuida

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: observabilidad, trazabilidad, logging, mdc, feign, rabbitmq

## Contexto y Problema

En una arquitectura distribuida donde una acción de usuario (ej: registrar una donación) desencadena procesamiento asíncrono, replicación HTTP hacia `notificaciones-service` e `incentivos-service`, y eventos AMQP con `logistica-service`, diagnosticar errores o monitorear la latencia se vuelve extremadamente complejo si los logs de cada contenedor carecen de correlación. Se requiere un mecanismo unificado, no invasivo y transversal que propague un identificador único de traza (`traceId`) a través de protocolos heterogéneos (HTTP síncrono, mensajería AMQP y pools de hilos `@Async`).

## Atributos de Calidad y Drivers de Decisión

* **Observabilidad y Diagnosticabilidad:** Capacidad de reconstruir la cronología completa de una transacción distribuida a través de múltiples microservicios.
* **Mantenibilidad y No Invasión:** No exigir a los desarrolladores pasar manualmente el `traceId` como parámetro en cada método o DTO de negocio.
* **Rendimiento:** Minimizar la sobrecarga de CPU y memoria en el formateo y propagación de trazas.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 9 (Trazabilidad Integral) y Oleada 9.5 (Hardening de Bordes) en todos los microservicios.
* **Hallazgo:** En la Oleada 9 se descubrió que al despachar tareas asíncronas con `@Async` (ej: normalización semántica o envíos de notificaciones), el hilo hijo nacía con el MDC vacío, perdiendo el `traceId` y fragmentando la traza. Esto obligó a diseñar e incorporar `MdcTaskDecorator` en `common-lib` para clonar el contexto al despachar tareas al pool de hilos.

## Alternativas Consideradas

* **Framework de Observabilidad Centralizado con Spring AOP, Servlet Filters y MDC:** Centralizar en `grupo5.common.logging` de `common-lib` componentes automáticos: `TraceResponseHeaderFilter` (HTTP inbound con header `X-Trace-Id`), `FeignTraceRequestInterceptor` (HTTP outbound), `MdcTaskDecorator` (hilos `@Async`), `ServiceLoggingAspect` / `ScheduledJobLoggingAspect` (spans automáticos) y propagación en cabeceras de RabbitMQ.
* **Tracer Pesado de Terceros (OpenTelemetry / Spring Cloud Sleuth / Zipkin / Jaeger):** Incorporar un agente o SDK distribuido completo con colector remoto dedicado.
* **Propagación Manual en Parámetros de Dominio:** Modificar todas las firmas de métodos y DTOs para que reciban `String traceId`.

## Resultado de la Decisión

Alternativa elegida: "Framework de Observabilidad Centralizado con Spring AOP, Servlet Filters y MDC"

Justificación:
Esta alternativa satisface el requerimiento académico y productivo con máxima simplicidad (KISS) y cero dependencias pesadas adicionales. Al apoyarse en SLF4J MDC y el estándar de Spring Boot AOP, la instrumentación es 100% transparente para las capas de dominio y aplicación, y compatible tanto con HTTP como con RabbitMQ.

### Consecuencias Positivas

* Correlación extremo a extremo verificable en logs agregados con el patrón canónico `%X{traceId:-NO_TRACE}`.
* Inyección transparente del header HTTP `X-Trace-Id` en las respuestas de cara a clientes y tests de integración (`TracingContractIT`).
* Conservación del contexto de traza al cruzar límites asíncronos (`@Async`) y mensajería de colas.

### Consecuencias Negativas

* Requiere asegurar que cualquier nuevo `@Async` o pool de ejecución configure explícitamente el `MdcTaskDecorator`.

### Validación

Se valida mediante:
1. `TracingContractIT` en `integration-tests`, verificando la presencia de `X-Trace-Id` en las respuestas REST de los 4 servicios.
2. Comprobación en logs de que llamadas cruzadas Feign comparten el mismo `traceId`.
3. Verificación de logs de jobs `@Scheduled` conteniendo spans dedicados.

## Análisis de Alternativas

### Framework Centralizado con Spring AOP y MDC

#### Pros
* Cero infraestructura externa requerida (sin servidores Zipkin ni bases de datos de trazas).
* Logging estructurado de alto rendimiento delimitado por barras (`|`).
* Desacoplamiento absoluto del código de dominio.

#### Contras
* No provee visualizaciones gráficas de grafos distribuidos complejas (como UI de Jaeger), aunque es integrable a futuro.

### Tracer Pesado de Terceros (OpenTelemetry / Zipkin)

#### Pros
* Gráficos visuales avanzados de flujos distribuidos fuera de la caja.

#### Contras
* Incremento de complejidad operativa y memoria en Docker Compose.
* Riesgo de sobre-ingeniería innecesaria en la etapa actual.

### Propagación Manual

#### Pros
* Control explícito del flujo.

#### Contras
* Contaminación severa de firmas de dominio y DTOs con detalles técnicos.
* Altamente propenso a olvidos de desarrolladores.