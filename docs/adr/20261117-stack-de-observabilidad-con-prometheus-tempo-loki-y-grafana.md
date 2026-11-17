# Stack de observabilidad con Prometheus, Tempo, Loki y Grafana

- Status: proposed
- Date: 2026-11-17
- Deciders: el grupo, en las rondas de decisión de E7 (Q03, Q04 a Q06b y Q08, 2026-11-16, en [requerimientos-e7.md](../requerimientos/requerimientos-e7.md)); redacción asistida por agente. Pendiente de aprobación del equipo en el PR
- Tags: observabilidad, metricas, trazas, logs, prometheus, tempo, loki, grafana, kubernetes, entrega-7

> **Sucesor parcial** del [ADR de trazabilidad unificada](./20260901-sistema-unificado-de-trazabilidad-y-observabilidad-distribuida.md)
> (solo en la parte que descartó un trazador con colector y visualizador) y del
> [ADR de logs sin volúmenes del host](./20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md)
> (solo en cómo se recolectan los logs dentro del cluster). Los dos siguen vigentes en lo demás:
> `traceId` por MDC, NDJSON a `stdout`, contenedores sin root y sin bind-mounts. Ninguno cambia de
> estado en este PR; si el equipo lo pide, el revisor humano los marca.

## Contexto y Problema

`[DOCUMENTED]` El requerimiento 1 de la Entrega 7 pide recolectar, **visualizar y analizar**
métricas, trazas y logs, con una visión integral y en tiempo real de los servicios.

`[OBSERVED]` Punto de partida:

- **Métricas.** Los seis módulos tienen actuator, pero solo exponen `health`. No hay registro de
  métricas ni Prometheus.
- **Trazas.** `common-lib` trae `micrometer-tracing-bridge-brave` y propaga el `traceId` por HTTP y
  RabbitMQ, pero no hay colector ni visualizador. El ADR de trazabilidad **descartó**
  OpenTelemetry, Zipkin y Jaeger como "tracer pesado de terceros".
- **Logs.** NDJSON a `stdout` con `traceId`, `serviceName` y `eventType`
  ([ADR de NDJSON](./20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md)); los
  recolecta el demonio de Docker y se analizan con `scripts/analyze_preprod_logs.py`, sin panel.

Problema: ¿con qué herramientas se recolectan, guardan y muestran las tres señales, dónde corren y
qué se mide, sin romper la pureza del dominio ni exponer datos personales?

**Gate A:** decisión nueva, que revisa la del ADR de trazabilidad. **Gate B:** suma tecnología e
imágenes (AGENTS.md §6), una dependencia en `common-lib`, endpoints nuevos que hay que proteger y
cambia la recolección de logs.

## Alternativas Consideradas

| Opción | Métricas | Trazas | Logs |
|---|---|---|---|
| **A. Grafana con Prometheus, Tempo y Loki** (elegida, Q04) | Prometheus + Micrometer | Tempo | Loki + recolector |
| B. Prometheus, Grafana y Zipkin; logs sin panel | Prometheus | Zipkin | NDJSON a mano |
| C. Elastic (ELK) + APM | Elastic | Elastic APM | Elastic |
| D. Spring Boot Admin + actuator | actuator | — | — |
| E. Status quo (MDC + script de logs) | — | solo `traceId` en logs | script |

## Resultado de la Decisión

Elegida **A**: un panel único en **Grafana**, con **Prometheus** (métricas), **Tempo** (trazas) y
**Loki** (logs), en un **namespace de observabilidad** del cluster (Q05 reinterpretada, ver el
[ADR de Kubernetes](./20261117-despliegue-en-kubernetes-local-con-kind-y-kustomize.md)).

- **Métricas.** `micrometer-registry-prometheus` en `common-lib` (cross-cutting y neutro de dominio,
  cumple el criterio del Shared Kernel); cada módulo expone `/actuator/prometheus`. Las métricas de
  negocio se registran en la capa de aplicación o en adaptadores, nunca en entidades.
- **Qué se mide (Q06, Q06a y Q06b).** Las señales de oro de cada servicio y el catálogo podado del
  §3.1 de los requerimientos: **comprometidas M01 a M05, M08, M09, M11, M13, M16, M18, M19 y M21,
  más M24 a M26**; opcionales, en orden, M20, M06, M10, M07, M14, M17 y M22. Las etiquetas son de
  baja cardinalidad (tipo, estado, canal, resultado): nunca ids ni datos personales (AGENTS.md §4.3).
  Los medidores de estado actual (M02, M06, M11) salen de un conteo agrupado cacheado cada 30 s, y
  no de contadores en memoria, que el autorestart reinicia por diseño.
- **Trazas.** Se conserva Brave (`micrometer-tracing-bridge-brave`) y se le suma un *reporter*
  Zipkin hacia Tempo, que acepta ese formato en su distribuidor. `[INFERRED]` Tempo recomienda
  OTLP; si el formato Zipkin da problemas, cambiar al puente de OpenTelemetry solo cambia
  dependencias de `common-lib`, no código de los servicios. Lo verifica la tarea O2.
- **Logs.** `[PROPOSED]` **Grafana Alloy** (el grupo no eligió recolector en Q04; lo propone este ADR
  y lo confirma su revisión), como DaemonSet, lee los logs de los pods desde el nodo
  (`/var/log/pods`) y los manda a Loki. Los contenedores siguen escribiendo NDJSON a `stdout`, sin
  root y sin bind-mounts. Según la documentación de Grafana (fuente externa, ver Referencias),
  Promtail, el recolector clásico de Loki, llegó a su fin de vida el 2 de marzo de 2026 y se
  reemplaza por Alloy.
- **Correlación.** Grafana enlaza un log con su traza por el `traceId`: `[OBSERVED]` hoy el mismo
  `traceId` está en el span de Brave y en el MDC de los logs. `[PROPOSED]` Las métricas enlazan con
  sus trazas de ejemplo (*exemplars*) cuando existan; hoy no hay métricas.
- **RabbitMQ.** La profundidad de colas y las colas muertas (M18, M21) salen del plugin
  `rabbitmq_prometheus`, que la imagen trae apagado `[INFERRED]`. Lo verifica la tarea O3.
- **Alertas (Q06).** Solo en el panel de Grafana, sin canal externo: reinicios repetidos (M26),
  Redis caído (M25), rechazos del rate limiting (M24) y colas muertas (M18).
- **Tableros.** Uno general y uno por servicio.
- **Protección (Q03).** El grupo pidió proteger los endpoints de observabilidad; el cómo lo propone
  este ADR `[PROPOSED]`: `/actuator/prometheus` no pasa por el gateway ni por el Ingress (Prometheus
  lo lee dentro del cluster) y Grafana pide usuario y contraseña, guardados en un Secret.

## Análisis por alternativa

**A. Grafana con Prometheus, Tempo y Loki (elegida)**
- A favor: cubre las tres señales con un solo panel y las correlaciona por `traceId`; reutiliza
  Micrometer, Brave y el NDJSON que ya existen; son las herramientas de referencia de Kubernetes.
- En contra: cuatro componentes nuevos más el recolector; consumo de memoria alto en el cluster
  local; contradice al ADR de trazabilidad, por eso este ADR es su sucesor parcial.

**B. Prometheus, Grafana y Zipkin, logs sin panel**
- A favor: menos contenedores; Brave habla Zipkin sin cambios.
- En contra: los logs siguen sin visualizar, y el requerimiento pide visualizar y analizar las tres
  señales.

**C. Elastic + APM**
- A favor: un solo producto para todo.
- En contra: Elasticsearch pide mucha memoria, y el cluster local ya está al límite; el APM usa un
  agente Java que reemplaza a Brave.

**D. Spring Boot Admin**
- A favor: muy liviano.
- En contra: no tiene trazas ni logs; no cumple el requerimiento.

**E. Status quo**
- En contra: no visualiza nada; no cumple el requerimiento. Era la elección del ADR de trazabilidad,
  que sirvió para E4 a E6, pero no para E7.

**Principios.** La instrumentación queda en `common-lib` y en adaptadores, y el dominio no conoce
Micrometer (pureza del dominio, inversión de dependencias). Cada componente del stack tiene una sola
responsabilidad (guardar métricas, trazas o logs) y Grafana es la única fachada de consulta.

## Consecuencias

- **Positivas.**
  - Una traza de punta a punta navegable, con sus logs y sus métricas, en un solo panel.
  - El autorestart (M26) y el *fail-open* del rate limiting (M25) se ven y alertan.
  - Las métricas de negocio responden preguntas del dominio (dónde está el stock, cuánto tarda una
    donación, si llegan los avisos).
- **Negativas.**
  - Una dependencia nueva en `common-lib` y cinco imágenes nuevas: Prometheus, Tempo, Loki y
    Grafana, aprobadas por el grupo en Q04, y Alloy, que se aprueba con este ADR.
  - Todas las métricas de negocio son código nuevo `[OBSERVED]`: hoy no hay ninguna.
  - Alloy lee `/var/log/pods` del nodo con un montaje `hostPath`, solo lectura: es el único acceso al
    host del sistema y una excepción acotada al ADR de aislamiento. `[INFERRED]` Puede necesitar
    correr con permisos de lectura sobre esos archivos (root o el grupo del nodo); lo define la
    tarea L1.
  - La verificación completa (tarea O5) necesita una máquina de 16 GB.
  - Sin cluster (el plan B del punto de control del 22/11), el stack corre igual en Compose, pero
    los logs vuelven a salir del demonio de Docker y hace falta otra configuración del recolector.

## Referencias

- [Requerimientos de E7](../requerimientos/requerimientos-e7.md): Q03 a Q06b, §3.1 (catálogo de métricas) y Q08
- [ADR de trazabilidad unificada](./20260901-sistema-unificado-de-trazabilidad-y-observabilidad-distribuida.md)
- [ADR de NDJSON y MDC](./20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md)
- [ADR de aislamiento de contenedores y logs sin volúmenes del host](./20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md)
- [ADR de Kubernetes](./20261117-despliegue-en-kubernetes-local-con-kind-y-kustomize.md)
- [Logging y trazabilidad](../arquitectura/logging-trazabilidad.md)
- Fin de vida de Promtail: https://grafana.com/docs/loki/latest/send-data/promtail/ · Receptores de Tempo: https://grafana.com/docs/tempo/latest/configuration/
