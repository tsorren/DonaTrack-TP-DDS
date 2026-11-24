# Observabilidad con OpenTelemetry y la imagen grafana/otel-lgtm

- Status: proposed
- Date: 2026-11-18
- Deciders: el grupo, en las rondas de decisión de E7 (Q03 a Q06b y Q08, 2026-11-16) y en la revisión de simplificación (E7-S03 a E7-S03c, 2026-11-18), en [requerimientos-e7.md](../requerimientos/requerimientos-e7.md); redacción asistida por agente. Pendiente de aprobación del equipo en el PR
- Tags: observabilidad, metricas, trazas, logs, opentelemetry, otlp, grafana, entrega-7

> **Reescrito el 2026-11-18 (E7-S05 A).** La versión del 2026-11-17 decidía cuatro contenedores
> (Prometheus, Tempo, Loki y Grafana) en un namespace de Kubernetes, Brave con un *reporter*
> Zipkin, Grafana Alloy leyendo los logs del nodo y 16 métricas propias. La revisión de
> simplificación lo cambió por una imagen todo en uno, todo por OTLP y seis métricas propias. Ese
> texto queda en el historial del PR de los ADR de E7, que no estaba mergeado.
>
> **Sucesor parcial** del [ADR de trazabilidad unificada](./20260901-sistema-unificado-de-trazabilidad-y-observabilidad-distribuida.md),
> solo en la parte que descartó OpenTelemetry y un colector con visualizador: este ADR adopta
> OpenTelemetry como forma de exportar las tres señales. Sigue vigente en lo demás (`traceId` por
> MDC, propagación por HTTP y RabbitMQ, encabezado `X-Trace-Id`). Con el
> [ADR de logs sin volúmenes del host](./20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md)
> ya **no hay choque**: los logs salen por OTLP desde la aplicación y nadie lee el disco del host.
> Ninguno cambia de estado en este PR; si el equipo lo pide, el revisor humano los marca.

## Contexto y Problema

`[DOCUMENTED]` El requerimiento 1 de la Entrega 7 pide recolectar, **visualizar y analizar**
métricas, trazas y logs, con una visión integral y en tiempo real de los servicios.

`[OBSERVED]` Punto de partida:

- **Métricas.** Los seis módulos tienen actuator, pero solo exponen `health`. No hay registro de
  métricas.
- **Trazas.** `common-lib` trae `micrometer-tracing-bridge-brave` y propaga el `traceId` por HTTP
  (`FeignTraceRequestInterceptor`, `TraceResponseHeaderFilter` con `X-Trace-Id`) y por RabbitMQ,
  sin colector ni visualizador. El ADR de trazabilidad **descartó** OpenTelemetry, Zipkin y Jaeger
  como "tracer pesado de terceros".
- **Logs.** NDJSON a `stdout` con `traceId`, `serviceName` y `eventType`
  ([ADR de NDJSON](./20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md)), sin
  panel.

Problema: ¿con qué herramientas se recolectan, guardan y muestran las tres señales, cómo salen de
los servicios y qué se mide, sin romper la pureza del dominio ni exponer datos personales, y con
el menor número de piezas posible?

**Gate A:** decisión nueva, que revisa la del ADR de trazabilidad. **Gate B:** suma una imagen y
cambia una dependencia estructural de `common-lib` (Brave por OpenTelemetry), que usan todos los
servicios.

## Alternativas Consideradas

**Qué stack (Q04 y E7-S03).**

| Opción | Qué es |
|---|---|
| **A1. Imagen todo en uno `grafana/otel-lgtm`** (elegida, E7-S03 A) | Colector OpenTelemetry, Prometheus, Tempo, Loki y Grafana en un contenedor |
| A2. Cuatro contenedores separados (Q04) | Prometheus, Tempo, Loki y Grafana, cada uno con su configuración |
| B | Prometheus, Grafana y Zipkin; logs sin panel |
| C | Elastic (ELK) + APM |
| D | Spring Boot Admin |
| E | Status quo (MDC + script de logs) |

**Cómo salen las señales (E7-S03b).**

| Opción | Métricas | Trazas | Logs |
|---|---|---|---|
| **1. Todo por OTLP** (elegida) | registro OTLP de Micrometer | puente OpenTelemetry | OTLP |
| 2. Mixto clásico | *scrape* de `/actuator/prometheus` | Brave en formato Zipkin | Alloy leyendo los logs de Docker con el socket montado |
| 3. Mixto sin socket | *scrape* | Zipkin | *appender* de Logback hacia Loki |

**Cuántas métricas propias (E7-S03c).** Automáticas más seis propias (elegida) · las 16
comprometidas en Q06a · solo automáticas más M24.

## Resultado de la Decisión

Elegidas la **imagen `grafana/otel-lgtm`** (A1) y **todo por OTLP** con
`spring-boot-starter-opentelemetry` (1), con las métricas automáticas y **seis propias**.

- **El stack.** `[DOCUMENTED]` Según la [documentación de Grafana](https://grafana.com/docs/opentelemetry/docker-lgtm/),
  la imagen trae el colector OpenTelemetry, Prometheus, Tempo, Loki y Grafana, y recibe OTLP en los
  puertos 4317 y 4318. Admite configuración propia para Prometheus, Loki y Tempo. Grafana la
  publica para desarrollo, demos y pruebas, no para producción: este ADR la justifica como entorno
  académico de demostración. Corre con una réplica en el stack de Swarm (ver el
  [ADR de despliegue](./20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md)). La
  elección se toma como aprobación de la imagen (AGENTS.md §6), a confirmar en el PR.
- **Cómo salen las señales.** `[DOCUMENTED]` Según el
  [blog de Spring](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/), el starter de
  Spring Boot 4 trae el puente OpenTelemetry para trazas y el registro OTLP de Micrometer para
  métricas, y configura el envío de logs por OTLP.
  - Se configura una sola vez en `common-lib`, donde **reemplaza a Brave**: es un cambio
    estructural de `common-lib` (AGENTS.md §7.0), sin cambios en el código de los servicios.
  - Los servicios empujan las señales al colector: no hace falta *scrape* ni recolector de logs.
  - El NDJSON a `stdout` sigue igual: los logs salen por los dos lados.
- **A verificar primero (tarea O2), antes de tocar los demás servicios:**
  - `[INFERRED]` Según la versión de Boot 4.0.x, los logs por OTLP pueden necesitar el *appender*
    de Logback, y las fuentes no coinciden. Si fallan, solo los logs caen a la opción 3.
  - Que el `traceId` siga en el MDC con el mismo nombre, y por lo tanto en el NDJSON, y que
    `X-Trace-Id` siga saliendo en las respuestas.
  - Que la propagación por HTTP y por RabbitMQ siga funcionando entre servicios. `[INFERRED]` Con
    los dos puentes, Spring Boot propaga en formato W3C por defecto. Igual, todos los servicios
    tienen que cambiar juntos, porque `common-lib` es uno solo.
- **Qué se mide (E7-S03c).**
  - **Automáticas:** señales de oro (`http.server.requests`) y JVM de cada módulo; GraphQL (M08,
    `[INFERRED]` Spring for GraphQL las emite solo).
  - **RabbitMQ:** profundidad de colas, colas muertas y parte de los eventos (M21, M18 y parte de
    M19) salen del plugin de Prometheus de RabbitMQ, leído con un *scrape* configurado en el
    Prometheus de la imagen. `Insufficient evidence to determine` si la imagen
    `rabbitmq:3.13-alpine` lo trae encendido: lo verifica la tarea O3.
  - **Reinicios (M26):** se derivan del cambio de `process_start_time` de cada módulo propio; los
    reinicios de la infraestructura no se cuentan.
  - **Propias:** M01 (donaciones registradas, por tipo), M04 (entregas fallidas y replanificadas),
    M13 (callbacks de proveedores descartados), M16 (notificaciones por canal y estado), M24
    (pedidos rechazados por el rate limiting) y M25 (errores y *timeouts* de Redis en el gateway).
  - **Fuera del compromiso:** M02, M03, M05, M09 y M11, y con M02 la consulta de conteo cada 30 s
    de Q06b.
  - Las métricas propias se registran en la capa de aplicación o en adaptadores, nunca en
    entidades. Las etiquetas son de baja cardinalidad (tipo, estado, canal, resultado): nunca ids
    ni datos personales (AGENTS.md §4.3).
- **Correlación.** Grafana enlaza un log con su traza por el `traceId`, y una métrica con sus trazas
  de ejemplo (*exemplars*) cuando existan.
- **Alertas (Q06).** Solo en el panel de Grafana, sin canal externo: reinicios repetidos (M26),
  Redis caído (M25), rechazos del rate limiting (M24) y colas muertas (M18).
- **Tableros.** Uno general y uno por servicio.
- **Protección (Q03).** `/actuator/prometheus` no se expone (las métricas salen por OTLP), y el
  colector no se publica fuera de la red del stack. Grafana se publica con usuario y clave
  propios, sin proxy delante. `Insufficient evidence to determine` si la imagen trae Grafana con
  acceso anónimo: la tarea O1 lo verifica, lo apaga si hace falta y pone la clave en una variable
  o un *secret*.

## Análisis por alternativa

**A1. Imagen todo en uno (elegida)**
- A favor: un contenedor en lugar de cuatro más el recolector; trae las fuentes de datos de
  Grafana ya conectadas. Matriz del grupo (plazo 35 %, cátedra 25 %, piezas 20 %, realismo 20 %):
  A1 4,15 frente a A2 3,1. Pasa de unos 3,5 a unos 1 día-persona.
- En contra: no es para producción; sin réplicas ni retención larga.

**A2. Cuatro contenedores separados**
- A favor: es como se despliega en producción; cada pieza escala sola.
- En contra: cuatro configuraciones y un recolector de logs más; el costo que la revisión quería
  sacar.

**B, C, D y E.** Zipkin sin panel de logs no visualiza las tres señales; Elastic pide más memoria de
la que deja el stack replicado; Spring Boot Admin no tiene trazas ni logs; el status quo no
visualiza nada. Ninguno cumple el requerimiento con menos piezas que A1.

**1. Todo por OTLP (elegida)**
- A favor: un solo protocolo y un solo lugar de configuración; sin socket de Docker ni lectura del
  disco del host; sin endpoint de métricas que proteger. Matriz del grupo (plazo 35 %, riesgo
  técnico 25 %, piezas 20 %, seguridad 20 %): 1 4,15, 3 3,45, 2 2,65.
- En contra: cambia Brave por OpenTelemetry en `common-lib`, que usan todos; el envío de logs
  depende de la versión de Boot.

**2. Mixto clásico**
- En contra: tres caminos distintos y Alloy con el socket de Docker, contra el ADR de logs sin
  volúmenes del host.

**3. Mixto sin socket**
- A favor: no toca Brave.
- En contra: tres caminos y un *appender* propio para Loki; queda como plan B solo para los logs.

**Seis métricas propias.** Las 16 de Q06a costaban unos 3 días-persona de código nuevo; quedarse
solo con M24 deja sin preguntas del dominio al panel. Las seis cubren donaciones, entregas,
seguridad, avisos y el rate limiting, en unos 1,5 días-persona.

**Principios.** La instrumentación queda en `common-lib` y en adaptadores, y el dominio no conoce
Micrometer ni OpenTelemetry (pureza del dominio, inversión de dependencias). Grafana es la única
fachada de consulta.

## Consecuencias

- **Positivas.**
  - Una traza de punta a punta navegable, con sus logs y sus métricas, en un solo panel.
  - El autorestart (M26) y el *fail-open* del rate limiting (M25) se ven y alertan.
  - Sin socket de Docker ni montajes del host: el ADR de aislamiento queda intacto.
- **Negativas.**
  - Brave sale de `common-lib`: si la propagación o el MDC cambian, cambian para todos los
    servicios a la vez.
  - Una imagen nueva, pensada para demos y no para producción.
  - Las seis métricas propias son código nuevo `[OBSERVED]`: hoy no hay ninguna.
  - Si los logs por OTLP fallan en la versión de Boot del repo, los logs van por otro camino que
    las trazas y las métricas.
- **Abiertas.**
  - El resultado de la verificación de la tarea O2 (logs por OTLP, MDC y propagación).
  - Si sobra tiempo: las cinco que salieron del compromiso (M02, M03, M05, M09 y M11) y las siete
    que ya eran opcionales en Q06a.

## Referencias

- [Requerimientos de E7](../requerimientos/requerimientos-e7.md): Q03 a Q06b, §3.1 (catálogo de métricas), Q08 y §5 (E7-S03 a E7-S03c)
- [ADR de trazabilidad unificada](./20260901-sistema-unificado-de-trazabilidad-y-observabilidad-distribuida.md)
- [ADR de NDJSON y MDC](./20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md)
- [ADR de aislamiento de contenedores y logs sin volúmenes del host](./20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md)
- [ADR de despliegue en Docker Swarm](./20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md)
- [Logging y trazabilidad](../arquitectura/logging-trazabilidad.md)
- `grafana/otel-lgtm`: https://grafana.com/docs/opentelemetry/docker-lgtm/ · OpenTelemetry con Spring Boot: https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/
