# Sistema Unificado de Logging y Trazabilidad Distribuida

Este documento detalla la arquitectura de **observabilidad, trazabilidad distribuida y logging estructurado** centralizado en `common-lib` y consumido por los cuatro microservicios de la plataforma (**`donaciones-service`**, **`notificaciones-service`**, **`incentivos-service`** y **`logistica-service`**).

---

## 1. Arquitectura y Componentes del Sistema de Logs

El subsistema de observabilidad en `grupo5.common.logging` proporciona logging declarativo y no invasivo mediante Spring AOP y filtros de Servlet:

```text
HTTP Request / Inbound
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

---

## 3. Ejemplo de Salida Estructurada de Logs

```text
2026-08-29 18:02:46.456 | INFO  | donaciones-service | f8b20c4f | a1b2c3d4e5f6 | g.d.m.r.DonacionIndependienteRepository | [REPOSITORY] [ACTION: SAVE] [ENTITY: DonacionIndependiente] [ID: 00000000-0000-0000-0000-000000000001] - Entity saved successfully
2026-08-29 18:02:46.457 | INFO  | incentivos-service | e9c30a1b | a1b2c3d4e5f6 | g.i.m.repositories.RankingRepository     | [REPOSITORY] [ACTION: SAVE] [ENTITY: RankingMensual] [ID: da26a9e8-2046-4282-9403-84660ce6c52d] - Entity saved successfully
2026-08-29 18:02:46.458 | INFO  | logistica-service  | c4d21f8e | a1b2c3d4e5f6 | g.l.m.repositories.EntregaRepository     | [REPOSITORY] [ACTION: SAVE] [ENTITY: Entrega] [ID: 550e8400-e29b-41d4-a716-446655440000] - Entity saved successfully
```
