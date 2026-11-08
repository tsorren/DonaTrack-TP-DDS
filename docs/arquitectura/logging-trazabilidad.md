# Sistema Unificado de Logging y Trazabilidad Distribuida

Este documento detalla la arquitectura de **observabilidad, trazabilidad distribuida y logging estructurado** centralizado en `common-lib` y consumido por los cuatro microservicios de la plataforma (**`donaciones-service`**, **`notificaciones-service`**, **`incentivos-service`** y **`logistica-service`**).

---

## 1. Arquitectura y Componentes del Sistema de Logs

El subsistema de observabilidad en `grupo5.common.logging` proporciona logging declarativo y no invasivo mediante Spring AOP y filtros de Servlet:

```text
HTTP Request / Inbound
    │
    ▼
ServerHttpObservationFilter de Spring Boot (Brave abre el span; XTraceIdPropagationFactory adopta el X-Trace-Id)
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
* **`XTraceIdPropagationFactory.java`**: Hace que Brave adopte el `X-Trace-Id` entrante como `traceId` del span del servidor (ADR-E6-02, `proposed`: el cliente liviano lo genera en `TraceIdFiltro` y lo manda en cada llamada a las APIs). `LoggingAutoConfiguration` la registra con un `BeanPostProcessor` que envuelve la `Propagation.Factory` de Spring Boot, así toda app que use `common-lib` la toma sin configuración.

### D. Cómo se elige el `traceId` de un pedido HTTP
El span del servidor lo abre Micrometer Tracing (Brave) antes de `TraceResponseHeaderFilter`, y Brave carga su `traceId` en el MDC. Por eso el `traceId` sale de Brave, en este orden:

1. Contexto W3C (`traceparent`) o B3 del pedido, si viene.
2. `X-Trace-Id`, si tiene el formato de `common-lib`: 32 caracteres hexadecimales en minúscula, con las dos mitades de 16 caracteres distintas de cero (con la alta en cero, Brave lo imprimiría con 16 caracteres; con la baja en cero, no lo acepta). Se conserva la decisión de muestreo que haya llegado, por ejemplo `b3: 0`.
3. Uno nuevo, generado por Brave.

`TraceResponseHeaderFilter` devuelve en `X-Trace-Id` el mismo valor del MDC. Sin tracing en el classpath, el filtro usa el header tal como llega (o `X-Request-Id`, o un UUID).

[VERIFIED] 2026-11-07: `curl -H "X-Trace-Id: 0123456789abcdef0123456789abcdef" http://localhost:8080/api/categorias` contra `donaciones-service` devuelve ese id y los logs del controller y del service lo muestran. `XTraceIdPropagationFactoryTest` levanta la autoconfiguración de Brave de Spring Boot y comprueba que la propagación queda envuelta.

[INFERRED] Vale igual para `notificaciones-service`, `incentivos-service` y `logistica-service`: toman `LoggingAutoConfiguration` de `common-lib` y ninguno configura `management.tracing.*` ni una `Propagation.Factory` propia. No se probó con curl en esos tres.

---

## 3. Ejemplo de Salida Estructurada de Logs

```text
2026-08-29 18:02:46.456 | INFO  | donaciones-service | f8b20c4f | a1b2c3d4e5f6 | g.d.m.r.DonacionIndependienteRepository | [REPOSITORY] [ACTION: SAVE] [ENTITY: DonacionIndependiente] [ID: 00000000-0000-0000-0000-000000000001] - Entity saved successfully
2026-08-29 18:02:46.457 | INFO  | incentivos-service | e9c30a1b | a1b2c3d4e5f6 | g.i.m.repositories.RankingRepository     | [REPOSITORY] [ACTION: SAVE] [ENTITY: RankingMensual] [ID: da26a9e8-2046-4282-9403-84660ce6c52d] - Entity saved successfully
2026-08-29 18:02:46.458 | INFO  | logistica-service  | c4d21f8e | a1b2c3d4e5f6 | g.l.m.repositories.EntregaRepository     | [REPOSITORY] [ACTION: SAVE] [ENTITY: Entrega] [ID: 550e8400-e29b-41d4-a716-446655440000] - Entity saved successfully
```
