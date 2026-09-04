# Observabilidad Estructurada con NDJSON y Trazabilidad de Contexto MDC

- Status: proposed
- Date: 2026-09-03
- Deciders: Decisión Grupal
- Tags: observabilidad, logging, logstash, json, ndjson, mdc, common-lib, trace-id

## Contexto y Problema

En DonaTrack, la trazabilidad y el diagnóstico de preproducción dependían de un sistema de logging basado en cadenas de texto plano formateadas mediante patrones visuales en `logback.xml` (delimitados por el carácter pipe `|`):

1. **Fragilidad de Parsing por Expresiones Regulares:** El script de diagnóstico `scripts/analyze_preprod_logs.py` dependía de una expresión regular rígida (`LOG_HEADER_REGEX`) para separar los campos del encabezado de log (timestamp, nivel, aplicación, instancia, traceId, logger, mensaje).
2. **Búsqueda de Cadenas Mágicas:** La identificación de eventos de negocio e intercepciones se realizaba buscando subcadenas literales en el cuerpo del mensaje (`"[CONTROLLER]" in msg`, `"[SERVICE-SUCCESS]" in msg`, `"[SERVICE-ERROR]" in msg`). Cualquier cambio cosmético en un mensaje de log, un refactor en el nombre de un paquete o el renombre de un interceptor rompía silenciosamente el análisis.
3. **Fatiga de Alertas (Alert Fatigue):** Durante los primeros segundos de arranque de los microservicios en preproducción, los drivers de conexión (PostgreSQL, RabbitMQ) suelen registrar eventos transitorios de nivel `ERROR` mientras los contenedores de infraestructura terminan de inicializarse. El analizador trataba cualquier línea con nivel `ERROR` como una anomalía crítica (`CRITICAL`), generando ruido y falsos bloqueos.
4. **Falta de Estandarización Cloud-Native:** El formato de texto plano no estructurado dificulta la ingesta directa en colectores modernos de telemetría (como Fluentbit, Logstash, Vector o Datadog) sin requerir transformaciones complejas con Grok.

Se requiere formalizar una arquitectura de telemetría que garantice **observabilidad tipada**, **inmunidad a refactors** y **clasificación semántica precisa de eventos**.

## Atributos de Calidad y Drivers de Decisión

* **Mantenibilidad y Robustez:** El sistema de análisis de logs no debe ser vulnerable a refactors o cambios de presentación visual.
* **Observabilidad Distribuida:** Los eventos de log deben transportar metadatos de contexto estructurados (traceId, spanId, serviceName, eventType, tiempos de ejecución).
* **Anti-Fatiga de Alertas:** Capacidad de discriminar errores transitorios de inicialización (*bootstrap phase*) frente a fallas operativas reales en régimen de trabajo.
* **Interoperabilidad Cloud-Native:** Adopción de formatos estándar de la industria (Newline Delimited JSON — NDJSON).

## Alternativas Consideradas

* **Alternativa 4.A — Observabilidad Estructurada con Logstash JSON y Contexto MDC (Elegida):**
  - **Shared Kernel (`common-lib`):** Inclusión de la dependencia `net.logstash.logback:logstash-logback-encoder` en el Shared Kernel.
  - **Emisión en NDJSON:** En perfiles de preproducción y producción, Logback emite eventos directamente en formato JSON (una línea por evento) a la salida estándar (`stdout`), incluyendo metadatos estructurados enriquecidos.
  - **Contexto MDC Tipado (implementado en esta entrega):** Los interceptores (`ControllerLoggingInterceptor`, `ServiceLoggingAspect`) enriquecen el MDC (`Mapped Diagnostic Context`) con campos clave: `traceId`, `serviceName`, `eventType` (`HTTP_IN`, `SERVICE_SUCCESS`, `SERVICE_ERROR`), `httpMethod`, `endpoint`.
  - **Contexto MDC Tipado (diferido — ver [DTI-08](./DEUDA_TECNICA.md#dti-08--campos-de-observabilidad-diferidos-spanid-executiontimems-errorcode-estructurado-amqp_dispatchexception-filtro-bootstrap)):** `spanId`, `executionTimeMs`, `errorCode` como campo MDC/JSON estructurado (hoy `errorCode` solo existe embebido en el texto del mensaje de `GlobalExceptionHandler`, no como clave estructurada), y los valores de `eventType` `AMQP_DISPATCH`/`EXCEPTION`.
  - **Parsing Determinístico en Python:** `analyze_preprod_logs.py` procesa cada línea mediante `json.loads(line)`. La clasificación se basa en atributos estructurados (p. ej. `entry.get("eventType") == "SERVICE_ERROR"`) en lugar de expresiones regulares. La clasificación por `eventType == "EXCEPTION"` queda pendiente hasta que ese valor se emita (ver diferido arriba).
  - **Filtro Anti-Fatiga (diferido — ver DTI-08):** Se planea incorporar la dimensión `stage: "BOOTSTRAP"` para ignorar reintentos transitorios durante los primeros $N$ segundos de arranque del contenedor. Aún no implementado.
  - **Perfil de Desarrollo Amigable:** Se conserva un perfil `local-dev` con salida tradicional por consola para desarrolladores que no deseen leer JSON en su terminal local.

* **Alternativa 4.B — Robustecer Expresiones Regulares sobre Texto Plano:**
  - Mantener el logging en texto plano delimitado por pipes pero actualizar la expresión regular en Python para hacerla más flexible y tolerante a fallos.
  - *Descarte:* Es un parche cosmético que perpetúa el acoplamiento a cadenas libres, mantiene la fragilidad ante refactors y no resuelve la integración con colectores cloud-native.

* **Alternativa 4.C — Adopción Completa de OpenTelemetry con Agente Java:**
  - Inyectar el agente Java de OpenTelemetry en cada contenedor y desplegar un colector OTel con Jaeger o Prometheus.
  - *Descarte:* Sobrecarga excesiva de infraestructura y complejidad para el alcance académico del proyecto; introduce costos de memoria adicionales en runners de CI.

## Resultado de la Decisión

Alternativa elegida: **Alternativa 4.A — Observabilidad Estructurada con Logstash JSON y Contexto MDC**

### Justificación:
`logstash-logback-encoder` es el estándar de facto en el ecosistema Spring Boot para emitir logs estructurados de alto rendimiento sin sobrecarga operacional.  
El desacoplamiento entre la producción del log y su consumo es absoluto: los analizadores consumen un objeto JSON bien tipado. Modificar el mensaje textual visible para un usuario o desarrollador no altera en lo más mínimo la capacidad de los scripts de telemetría para auditar trazas distribuidas, errores o tiempos de respuesta.

### Consecuencias Positivas:
* **Inmunidad a Refactorizaciones:** Renombrar clases o cambiar mensajes descriptivos no rompe los analizadores.
* **Filtrado Semántico Inteligente:** Posibilidad de silenciar alertas falsas durante el bootstrapping de servicios.
* **Trazabilidad Distribuida Real:** Todos los eventos emitidos por un flujo preservan de forma nativa su `traceId` en una clave JSON de primer nivel.
* **Consumo Universal:** El archivo generado por `docker compose logs` puede ser parseado por Python en CI o ingerido por cualquier herramienta de agregación de logs.

### Consecuencias Negativas y Mitigaciones:
* **Legibilidad Humana en Terminal Cruda:** Una traza de logs en formato JSON en una consola local sin herramientas auxiliares puede resultar abrumadora para lectura humana.
  - *Mitigación:* Se preserva el perfil de desarrollo local `local-dev` en Logback que emite salida coloreada legible por humanos cuando no se ejecute en entorno de preproducción/CI.

## Referencias y Trabajo Futuro

* [`docs/auditoria/revision-critica-devops-ci.md`](../auditoria/revision-critica-devops-ci.md): Eje 4 de la revisión crítica DevOps.
* [`docs/arquitectura/logging-trazabilidad.md`](../arquitectura/logging-trazabilidad.md): Guía de observabilidad distribuida y MDC.
* [`DEUDA_TECNICA.md#dti-08`](./DEUDA_TECNICA.md#dti-08--campos-de-observabilidad-diferidos-spanid-executiontimems-errorcode-estructurado-amqp_dispatchexception-filtro-bootstrap): Campos de esta ADR aún no implementados (`spanId`, `executionTimeMs`, `errorCode` estructurado, `AMQP_DISPATCH`/`EXCEPTION`, filtro `BOOTSTRAP`).
