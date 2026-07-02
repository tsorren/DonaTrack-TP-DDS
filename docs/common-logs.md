# walkthrough.md

# Standard Logging System Walkthrough

We have successfully implemented the unified, standardized logging system inside `common-lib` and consolidated it across all microservices (`donaciones-service`, `notificaciones-service`, and `incentivos-service`).

---

## Changes Made

### 1. Core Dependency Integrations
* **`common-lib/pom.xml`**:
  * Added `spring-aop` and `aspectjweaver` to enable Aspect-Oriented Programming (AOP) for logging aspects.
  * Added `io.micrometer:micrometer-tracing-bridge-brave` for automated local trace propagation.
  * Added `io.github.openfeign:feign-micrometer` to propagate tracing context on outbound Feign calls across microservices automatically.
  * Replaced `spring-web` with `spring-boot-starter-web` to resolve Spring MVC and Servlet API dependencies at compile time.

### 2. Logback Unified Layout & File Persistence
* **`common-lib/src/main/resources/logback-spring.xml`** [NEW]:
  * Configured a Console Appender with the custom pipe-delimited format (including service name and unique instance ID):
    `%d{yyyy-MM-dd HH:mm:ss.SSS} | %-5level | ${appName} | ${instanceId} | %X{traceId:-NO_TRACE} | %logger{36} | %msg%n`
  * Configured `FILE_ACTUAL` (FileAppender) mapping to `logs/actual/<service>-<instanceId>.log` (prevents file locking conflicts when running multiple replicas locally).
  * Configured `FILE_REGISTRO` (FileAppender) mapping to `logs/registro/<execution_id>/<service>-<instanceId>.log` (retains historical logs, cleanly segregating different replica instances).

### 3. Aspect-Oriented and Interceptor Logging
* **`InstanceIdPropertyDefiner.java`** [NEW]: Generates a unique 8-character ID for the running JVM instance (reads from `INSTANCE_ID` env var, `instance.id` system property, or falls back to a random short UUID).
* **`ControllerLoggingInterceptor.java`** [NEW]: Logs every incoming HTTP request (Verb, path, package, class, method).
* **`ServiceLoggingAspect.java`** [NEW]: Automatically logs service completion status (`[SERVICE-SUCCESS]` / `[SERVICE-ERROR]` with package, class, and method details) for all `@Service` beans.
* **`ScheduledJobLoggingAspect.java`** [NEW]: Uses Micrometer's `Tracer` to start a new scoped span for `@Scheduled` job executions, automatically generating and registering a trace ID for background jobs.
* **`LoggingAutoConfiguration.java`** [NEW]: Exposes aspects and registers the Spring MVC interceptor.
* **`CommonLibAutoConfiguration.java`**: Imported `LoggingAutoConfiguration.class` to trigger auto-registration on classpath resolution.

### 4. Database & Exception Logging Consolidation
* **`CrudRepositoryEnMemoria.java`**: Added standard logging for generic repository operations (Write actions like Save/Delete logged at `INFO`, Read actions like Find logged at `DEBUG`).
* **`GlobalExceptionHandler.java`**: Injected logger to log exceptions with stacktraces and catalog error codes (Warnings for 4xx validations, Errors for 5xx errors/infrastructures).
* **Duplicate Exception Handlers Cleaned Up**: Deleted local `GlobalExceptionHandler` and `ErrorResponse` custom classes from `incentivos-service` and `notificaciones-service` to let them fallback to `common-lib`'s handlers.

### 5. Services Promotion & Cleanup
* **`ProcesadorDeDonaciones.java`**:
  * Promoted annotation from `@Component` to `@Service` to trigger Aspect logging.
  * Removed redundant manual entry/exit logs.
* **`NotificacionesAsyncService.java`**:
  * Cleaned up redundant log statements and unused logger fields/imports.
  * Retained exception-catching try-catch blocks to preserve its business exception handling contract.

---

## Verification Results

### 1. Automated Tests
We executed `mvn clean test` from the root directory to verify the build and test stability.
* **Result**: **BUILD SUCCESS**
* **Microservices Tested**:
  * `common-lib`: Compiles and spotless formats successfully.
  * `donaciones-service`: 266 tests passed.
  * `notificaciones-service`: 62 tests passed.
  * `incentivos-service`: 53 tests passed.
  * `integration-tests`: Skipped tests as configured by default, model generation succeeded.

### 2. Log Format Sample
During the test execution, logs were outputted in the correct standardized format:
```
2026-06-19 08:02:46.456 | INFO  | unknown-service | f8b20c4f | NO_TRACE | g.i.m.r.DonanteIncentivosRepository | [REPOSITORY] [ACTION: SAVE] [ENTITY: DonanteIncentivos] [ID: 00000000-0000-0000-0000-000000000001] - Entity saved successfully
2026-06-19 08:02:46.457 | INFO  | unknown-service | f8b20c4f | NO_TRACE | g.i.m.repositories.RankingRepository | [REPOSITORY] [ACTION: SAVE] [ENTITY: RankingMensual] [ID: da26a9e8-2046-4282-9403-84660ce6c52d] - Entity saved successfully
```
*Note: In local unit tests, the service name defaults to `unknown-service`, the instanceId resolves to a dynamic 8-character string (like `f8b20c4f`), and correlation ID defaults to `NO_TRACE` as expected when HTTP/Scheduler contexts are not initialized.*
