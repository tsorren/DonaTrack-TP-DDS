# Sistema Unificado de Logging y Trazabilidad Distribuida

Este documento detalla la arquitectura de **observabilidad, trazabilidad distribuida y logging estructurado** centralizado en `common-lib` y consumido por los cuatro microservicios de la plataforma (**`donaciones-service`**, **`notificaciones-service`**, **`incentivos-service`** y **`logistica-service`**).

---

## 1. Arquitectura y Componentes del Sistema de Logs

El subsistema de observabilidad en `grupo5.common.logging` proporciona logging declarativo y no invasivo mediante Spring AOP y filtros de Servlet:

```text
HTTP Request / Inbound
    │
    ▼
ServerHttpObservationFilter de Spring Boot (OpenTelemetry abre el span; XTraceIdPropagator adopta el X-Trace-Id)
    │
    ▼
TraceResponseHeaderFilter (Inyecta cabecera HTTP X-Trace-Id en Response)
    │
    ▼
ControllerLoggingInterceptor (Registra Verbo, URI, Clase y Método de entrada)
    │
    ▼
ServiceLoggingAspect (Intercepta beans @Service -> [SERVICE-SUCCESS] / [SERVICE-ERROR])
    │
    ├──▶ CrudRepositoryEnMemoria (Registra operaciones [REPOSITORY] [ACTION: SAVE/DELETE])
    │
    └──▶ FeignTraceRequestInterceptor (Propaga cabecera traceId en llamadas salientes HTTP)
```

---

## 2. Componentes Clave en `common-lib`

### A. Formato y Layout Estándar en Logback
Configurado en `common-lib/src/main/resources/logback-spring.xml` con formato delimitado por barras (`|`):
```text
%d{yyyy-MM-dd HH:mm:ss.SSS} | %-5level | ${appName} | ${instanceId} | %X{traceId:-NO_TRACE} | %logger{36} | %msg%n
```
* **`appName`**: Nombre del microservicio (`spring.application.name`).
* **`instanceId`**: Identificador único de 8 caracteres de la instancia JVM generado por `InstanceIdPropertyDefiner` (permite diferenciar réplicas en Docker).
* **`traceId`**: Identificador de correlación distribuida propagado en el MDC (Mapped Diagnostic Context).
* **Appender `OTEL`**: entrega cada log al SDK de OpenTelemetry para exportarlo por OTLP (ver la sección E).

### B. Persistencia de Logs en Archivo
* **`logs/actual/<servicio>-<instanceId>.log`**: Archivo de log activo para la instancia actual en ejecución.
* **`logs/registro/<execution_id>/<servicio>-<instanceId>.log`**: Historial segregado por ejecución para auditoría.

### C. Aspectos e Interceptores
* **`InstanceIdPropertyDefiner.java`**: Extrae `INSTANCE_ID` de variables de entorno o genera un ID aleatorio persistente para la JVM.
* **`ControllerLoggingInterceptor.java`**: Intercepta y registra cada llamada HTTP entrante.
* **`ServiceLoggingAspect.java`**: Monitorea de forma automática la ejecución exitosa o fallida de todos los beans anotados con `@Service`.
* **`ScheduledJobLoggingAspect.java`**: Genera un span y `traceId` dedicado para cada ejecución de cron jobs (`@Scheduled`), como la verificación de inactividad o cálculo de rachas.
* **`FeignTraceRequestInterceptor.java`**: Propaga las cabeceras de trazabilidad en llamadas Feign entre microservicios.
* **`MdcTaskDecorator.java`**: Propaga el contexto de trazabilidad hacia hilos asíncronos (`@Async`).
* **`XTraceIdPropagator.java`**: Hace que OpenTelemetry adopte el `X-Trace-Id` entrante como `traceId` del span del servidor (ADR-E6-02, `proposed`: el cliente liviano lo genera en `TraceIdFiltro` y lo manda en cada llamada a las APIs). `LoggingAutoConfiguration` lo registra con un `BeanPostProcessor` que envuelve el `TextMapPropagator` de Spring Boot, así toda app que use `common-lib` lo toma sin configuración.
* **`OtlpValoresPorDefecto.java`**: Valores por defecto de OpenTelemetry con la prioridad más baja (ver la sección E).

### D. Cómo se elige el `traceId` de un pedido HTTP
El span del servidor lo abre Micrometer Tracing (puente OpenTelemetry) antes de `TraceResponseHeaderFilter`, y su `Slf4JEventListener` carga el `traceId` y el `spanId` en el MDC. Por eso el `traceId` sale de OpenTelemetry, en este orden:

1. Contexto W3C (`traceparent`) del pedido, si viene. B3 ya no se lee: nada en el repo lo emite (E7, tarea O2).
2. `X-Trace-Id`, si tiene el formato de `common-lib`: 32 caracteres hexadecimales en minúscula, con las dos mitades de 16 caracteres distintas de cero (la misma regla que con Brave). OpenTelemetry no deja fijar un `traceId` sin padre: se arma un padre remoto con un `spanId` nuevo y **muestreado**, así que `[INFERRED]` en Tempo la raíz aparece con un padre desconocido.
3. Uno nuevo, generado por OpenTelemetry.

`TraceResponseHeaderFilter` devuelve en `X-Trace-Id` el mismo valor del MDC. Sin tracing en el classpath, el filtro usa el header tal como llega (o `X-Request-Id`, o un UUID).

[VERIFIED] 2026-11-19 (E7, tarea O2): con `api-gateway` levantado, un pedido sin header recibe un id nuevo de 32 hex; con `X-Trace-Id: 0123456789abcdef0123456789abcdef` lo adopta en la respuesta y en el log (la línea del WARN del proxy lo muestra); con uno inválido genera otro; con `traceparent` gana el de W3C. Es el mismo resultado que con Brave antes del cambio. `XTraceIdPropagatorTest` levanta la autoconfiguración de OpenTelemetry de Spring Boot y comprueba que la propagación queda envuelta y que el `traceId` llega al MDC.

[VERIFIED] 2026-11-07, con Brave: el mismo `curl` contra `donaciones-service` devolvía ese id y los logs del controller y del service lo mostraban.

[INFERRED] Vale igual para `notificaciones-service`, `incentivos-service` y `logistica-service`: toman `LoggingAutoConfiguration` de `common-lib` y ninguno configura `management.tracing.*` ni un `TextMapPropagator` propio. No se probó con curl en esos tres.

**Con el gateway (SPEC-07).** `api-gateway` toma la misma autoconfiguración, así que elige el `traceId` con este mismo orden. Después lo copia en el `X-Trace-Id` del pedido que manda al servicio, y el servicio lo adopta por el paso 2. A la respuesta le saca el `X-Trace-Id` del servicio para que el header no quede duplicado. [VERIFIED] 2026-11-07 con `RutasGatewayTest` y servicios falsos: el header enviado y el generado llegan al servicio y vuelven una sola vez.

### E. Exportación por OTLP (E7)
`spring-boot-starter-opentelemetry` (en `common-lib`) reemplaza a Brave ([ADR de observabilidad con otel-lgtm](https://github.com/tsorren/DonaTrack-TP-DDS/pull/987), `proposed`, en el PR 987). Las señales salen solo si se configuran. `[VERIFIED]` 2026-11-19: `api-gateway` corrió 2 minutos sin colector y sin avisos de OTLP.

| Señal | Se prende con | Por defecto |
|---|---|---|
| Trazas | `MANAGEMENT_OPENTELEMETRY_TRACING_EXPORT_OTLP_ENDPOINT` (por ejemplo `http://otel-lgtm:4318/v1/traces`) | Sin endpoint, no se exportan |
| Logs | `MANAGEMENT_OPENTELEMETRY_LOGGING_EXPORT_OTLP_ENDPOINT` (`.../v1/logs`); el *appender* `OTEL` de `logback-spring.xml` los entrega al SDK | Sin endpoint, no se exportan. El NDJSON a stdout sigue igual |
| Métricas | `MANAGEMENT_OTLP_METRICS_EXPORT_ENABLED=true` y `MANAGEMENT_OTLP_METRICS_EXPORT_URL` (`.../v1/metrics`) | Apagadas (`OtlpValoresPorDefecto`) |

El muestreo queda en 1.0 por defecto (entorno de demostración); se baja con `MANAGEMENT_TRACING_SAMPLING_PROBABILITY`, salvo en los pedidos con `X-Trace-Id` adoptado, que se muestrean siempre (el padre remoto llega muestreado). Cualquier `application.properties` o variable de entorno pisa estos valores.

**Por RabbitMQ** no cambia nada: el `traceId` viaja solo en el header `X-Trace-Id` que ponen a mano los publicadores. `[OBSERVED]` Incentivos no lo manda y el `LogisticaEventListener` de Donaciones no lo restaura; la observación de Spring AMQP (`traceparent` en los mensajes) queda fuera de O2.

---

## 3. Ejemplo de Salida Estructurada de Logs

```text
2026-08-29 18:02:46.456 | INFO  | donaciones-service | f8b20c4f | a1b2c3d4e5f6 | g.d.m.r.DonacionIndependienteRepository | [REPOSITORY] [ACTION: SAVE] [ENTITY: DonacionIndependiente] [ID: 00000000-0000-0000-0000-000000000001] - Entity saved successfully
2026-08-29 18:02:46.457 | INFO  | incentivos-service | e9c30a1b | a1b2c3d4e5f6 | g.i.m.repositories.RankingRepository     | [REPOSITORY] [ACTION: SAVE] [ENTITY: RankingMensual] [ID: da26a9e8-2046-4282-9403-84660ce6c52d] - Entity saved successfully
2026-08-29 18:02:46.458 | INFO  | logistica-service  | c4d21f8e | a1b2c3d4e5f6 | g.l.m.repositories.EntregaRepository     | [REPOSITORY] [ACTION: SAVE] [ENTITY: Entrega] [ID: 550e8400-e29b-41d4-a716-446655440000] - Entity saved successfully
```
