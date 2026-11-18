# Requerimientos — Entrega 7: Observabilidad y Seguridad (rondas de decisión)

> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión 0 · 2026-11-16 · borrador de las rondas de preguntas · para responder con el equipo**
> Fecha de entrega: **defensa el viernes 4 de diciembre de 2026**, según informó Tadeo el 2026-11-17. Hasta esa fecha el documento decía 27 de noviembre, tomado del [mapa de entregas](../entregas/README.md) (borrador de calendario de la cátedra; el enunciado no trae fechas). Las rondas 1 a 6 se respondieron con el plazo de 11 días y conservan ese razonamiento.
> **Revisión de simplificación (2026-11-18), respondida (rondas S1 a S5):** el grupo vuelve a analizar las decisiones para simplificar la arquitectura, en rondas S1 a S5 (§5). Las respuestas ✅ de las rondas 1 a 6 no se reescriben: las preguntas más afectadas llevan una nota que apunta a la revisión, y ante cualquier diferencia (también con la tabla del §4) vale la respuesta más reciente, la del §5. Para no confundirlas con las tareas S1 a S4 de Q15, las rondas se nombran siempre "Ronda S1" a "Ronda S5".

## 0. Cómo leer este documento

- **Para qué sirve:** el enunciado de E7 son cuatro requerimientos y tres entregables en una página. Este documento los baja a **preguntas de decisión con opciones**, para que de las respuestas salgan los requerimientos verificables y los ADRs, como se hizo en [E6](requerimientos-e6.md).
- **Fuentes:** [`Enunciado-TP-Anual-2026.pdf`](../entregas/Enunciado-TP-Anual-2026.pdf) (E7, pág. 28), el código de `ENTREGA_7` (igual a `main` con la Entrega 6, `f077a8b44`) y los ADRs citados.
- **Etiquetas** (AGENTS.md §3): `[DOCUMENTED]` · `[OBSERVED]` · `[INFERRED]` · `[PROPOSED]`.
- **Estado de cada pregunta:** ⏸️ sin responder · ⏳ diferida a otra sesión · ✅ respondida · 🎓 para consultar a la cátedra.
- **Cada pregunta** trae el punto de partida observado, las opciones y una **recomendada** (la primera, marcada con ⭐). La recomendación es una propuesta del agente `[PROPOSED]`: no es una decisión hasta que el grupo responda.
- **Orden:** las rondas van de lo que cambia el alcance (R1) a lo que se decide dentro de cada requerimiento (R2 a R5) y a lo que se entrega (R6). Si R1 cambia, R5 y R6 se reescriben.

---

## 1. Lo que pide la cátedra `[DOCUMENTED]`

| ID | Requerimiento | Cómo se verifica `[PROPOSED]` |
|---|---|---|
| RQ-E7-01 | Observabilidad: recolectar, **visualizar y analizar** métricas, trazas y logs, con visión integral y en tiempo real de los servicios | Un panel con los tres tipos de señal de todos los servicios, y una traza de punta a punta navegable |
| RQ-E7-02 | Monitoreo proactivo de la salud de los servicios, con **políticas de autorestart** ante fallos | Matar un servicio y verlo volver solo, con el evento visible en el monitoreo |
| RQ-E7-03 | **Rate limiting** del tráfico entrante, para proteger frente a abuso o sobrecarga | Superar el límite devuelve `429` y queda registrado; el tráfico normal pasa |
| RQ-E7-04 | **Opción 1:** gRPC en lugar de HTTP entre servicios que requieren actualizaciones en tiempo real. **Opción 2:** interfaz GraphQL en Donaciones o en Logística, con filtros dinámicos y selección de campos | Una llamada real que use gRPC, o una consulta GraphQL con filtros y campos elegidos, con test |

| Entregable | Detalle |
|---|---|
| E7-ENT-1 | Diagrama de **Componentes y Despliegue** de la solución actual |
| E7-ENT-2 | Implementación de observabilidad, monitoreo, seguridad y control |
| E7-ENT-3 | Implementación de un Web Service con gRPC o GraphQL |

> `[OBSERVED]` El título de la entrega dice "Seguridad", pero los cuatro requerimientos detallados solo nombran el rate limiting como medida de seguridad. La R1 pregunta qué más entra.

---

## 2. Punto de partida `[OBSERVED]`

| Tema | Qué hay hoy |
|---|---|
| Métricas | Los seis módulos con actuator (cuatro servicios, gateway y cliente) tienen `spring-boot-starter-actuator`, pero **solo exponen `health`** (`management.endpoints.web.exposure.include=health`). No hay Micrometer con registro de métricas, ni Prometheus. |
| Trazas | `common-lib` trae `micrometer-tracing-bridge-brave`, `feign-micrometer` y los componentes de `traceId`/MDC ([ADR de trazabilidad](../adr/20260901-sistema-unificado-de-trazabilidad-y-observabilidad-distribuida.md), `proposed`). No hay colector ni visualizador de trazas. Ese ADR **descartó** OpenTelemetry, Zipkin y Jaeger como "tracer pesado": E7 exige visualizar trazas, así que la decisión se revisa. |
| Logs | NDJSON a `stdout` con `traceId`, `serviceName` y `eventType` ([ADR](../adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md)). Sin bind-mounts: la recolección es del demonio de Docker ([ADR](../adr/20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md)). Se analizan con `scripts/analyze_preprod_logs.py`, sin panel. |
| Salud y reinicio | Los servicios y el gateway tienen `healthcheck` en `docker-compose.yml`. **No hay política de `restart`** en `docker-compose.yml` ni en el preprod (solo en `publica`, `seed` y `tunel`). |
| Gateway | `api-gateway` con Spring Cloud Gateway Server WebMVC y validación de JWT apagable ([ADR](../adr/20261107-api-gateway-validacion-jwt-en-doble-capa-y-rutas-publicas.md)). Sin rate limiting. |
| Comunicación entre servicios | RabbitMQ para eventos y REST para el resto. El único `@FeignClient` del repo es de Incentivos hacia Notificaciones. No hay gRPC ni GraphQL. |
| Tiempo real | La posición del camión se actualiza por polling cada 30 s ([ADR-E6-07](../adr/20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md)); ese ADR dejó anotado que E7 podría pasar a SSE o a gRPC en streaming, y que el rate limiting debe contar esas consultas (~7 por segundo con 200 mapas abiertos). |
| Seguridad diferida | El Key Broker (DTI-07) y el crypto-shredding definitivo (DTI-01) pasaron a E7 por la decisión D29 de E6 ([DEUDA_TECNICA.md](../adr/DEUDA_TECNICA.md)). |
| Carga | Scripts de k6 en `tests/performance/` ([ADR](../adr/20260906-migracion-pruebas-rendimiento-a-k6.md)). |
| Diagramas | `docs/arquitectura/diseno/diagrama-de-componentes.png` y el despliegue de E4 (`docs/entrega-4/arquitectura/diagramas/08-despliegue.puml`), anteriores a Keycloak, gateway, cliente liviano y MinIO. |

---

## 3. Rondas de preguntas

### Ronda 1 — Alcance y opción del requerimiento 4

**E7-Q01 — ¿gRPC o GraphQL? ✅ (2026-11-16): GraphQL en Donaciones (opción A); GraphQL en Logística queda abierta**
> **Respuesta del grupo:** GraphQL en Donaciones. Sumar también Logística (opción B) es posible, pero por ahora no; la decisión puede cambiar en los próximos días. Consecuencia `[PROPOSED]`: diseñar el endpoint de Donaciones para que Logística pueda sumar el suyo sin rehacerlo (un esquema por servicio, sin acoplarlos), y no empezar Logística hasta que el grupo lo confirme. gRPC queda descartado para E7 `[REJECTED]` por riesgo de plazo y por el costo del gateway; el ADR-E6-07 no cambia.

`[OBSERVED]` gRPC pide reemplazar HTTP "entre servicios que requieren actualizaciones en tiempo real", y hoy no hay ningún canal entre servicios que sea tiempo real: la posición del camión llega al cliente por REST con polling, y lo demás es RabbitMQ. `[OBSERVED]` GraphQL pide filtros dinámicos y selección de campos en Donaciones o Logística, y esos servicios ya tienen listados con filtros por parámetro (D28 de E6).

| Opción | Qué implica |
|---|---|
| ⭐ **A. GraphQL en Donaciones** | Un endpoint `/graphql` de solo lectura sobre donaciones y necesidades, con filtros y campos elegidos. No toca la mensajería ni el despliegue. Riesgo: no hay un "tiempo real" que justificar, pero el enunciado no lo pide para esta opción. |
| B. GraphQL en Logística | Igual, sobre entregas y seguimientos. Los datos de posición del camión tienen reglas de privacidad ([ADR-E6-07](../adr/20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md)) que hay que respetar en cada campo. |
| C. gRPC con streaming de la posición del camión | Logística publica la posición por gRPC y el cliente la recibe sin polling. Cambia el ADR-E6-07, abre un canal nuevo entre servicios (AGENTS.md §4.2 pide ADR), y el gateway WebMVC no enruta gRPC de forma nativa `[INFERRED]`. |
| D. Las dos | Cumple de más. Duplica el trabajo en 11 días. |

Criterios: ajuste al enunciado, riesgo de plazo, costo de dependencias nuevas, valor para la demo, cambios a ADRs aceptados.

**E7-Q02 — ¿El Key Broker (DTI-07) entra en E7? ✅ (2026-11-18, en la revisión de simplificación): A, queda diferido**
> **Respondida el 2026-11-18 (§5, E7-S04d):** el Key Broker no entra en E7; DTI-07 y DTI-01 se reprograman en `DEUDA_TECNICA.md` para después de E7. Hasta esa fecha estuvo ⏸️, pendiente por decisión del grupo desde el 2026-11-16.
`[DOCUMENTED]` D29 lo mandó a E7 "entero": un servicio nuevo, la emisión de `ClaveUsuarioDestruidaEvent` por RabbitMQ y el crypto-shredding definitivo (DTI-01). Ese servicio **no** figura en los cuatro requerimientos de E7.

| Opción | Qué implica |
|---|---|
| ⭐ **A. Queda diferido y se reprograma** | E7 se limita a lo que pide el enunciado; DTI-07 y DTI-01 se reprograman en `DEUDA_TECNICA.md`. Menor riesgo de plazo. |
| B. Contrato y diseño en E7, implementación después | Se escribe el ADR y el contrato del Key Broker, sin construirlo. |
| C. Se implementa en E7 | Es "Seguridad" en serio, pero suma un servicio y una base de claves a una entrega que ya trae cuatro frentes. |

**E7-Q03 — ¿Qué otras medidas de seguridad entran, si alguna? ✅ (2026-11-16): entran las cuatro**
> **Revisada el 2026-11-18 (§5, E7-S03b y E7-S04):** siguen las cuatro, más livianas. La observabilidad se protege con Grafana con usuario y clave (sin nginx delante desde la revisión de E7-S02b), porque no se expone `/actuator/prometheus`. La cadena JWT se valida con una prueba de humo en el stack de Swarm.
> **Respuesta del grupo:** proteger los endpoints de observabilidad, encabezados de seguridad en el gateway, validar la cadena JWT encendida en el preprod y escaneo de dependencias o imágenes en el CI. Las dos últimas no estaban recomendadas: suman trabajo (un Keycloak real en el preprod y un paso nuevo del pipeline) y el plan de PRs de Q15 las tiene que contar.

(Opciones originales, multiselección:)
`[OBSERVED]` hoy solo `health` está expuesto y los contenedores corren sin root. Opciones: ⭐ **proteger los endpoints de observabilidad** (que `/actuator/prometheus` y los paneles no queden públicos), ⭐ **encabezados de seguridad en el gateway**, validación de la cadena de seguridad encendida en el preprod (queda pendiente de E6), escaneo de dependencias o imágenes en el CI, ninguna otra. 🎓 Si la cátedra espera algo más bajo "Seguridad y Control", conviene preguntarlo.

---

### Ronda 2 — Observabilidad (RQ-E7-01)

**E7-Q04 — ¿Qué stack de observabilidad? ✅ (2026-11-16): A, Grafana + Prometheus + Tempo + Loki**
> **Revisada el 2026-11-18 (§5, E7-S03 y E7-S03b):** los mismos componentes, en la imagen todo en uno `grafana/otel-lgtm`; los servicios mandan las tres señales por OTLP y Brave se reemplaza por el puente OTel.
> **Respuesta del grupo:** opción A. `[PROPOSED]` Consecuencias a atender: un ADR sucesor parcial del de trazabilidad (`proposed`); las imágenes de Docker nuevas quedan aprobadas por esta respuesta (AGENTS.md §6); la dependencia `micrometer-registry-prometheus`; verificar que Tempo acepte el formato Zipkin para conservar Brave `[INFERRED]`; decidir cómo el recolector de logs lee los de Docker sin chocar con el ADR "sin volúmenes del host".


| Opción | Métricas | Trazas | Logs | Costo |
|---|---|---|---|---|
| ⭐ **A. Grafana LGTM liviano** | Prometheus + Micrometer | Tempo con OpenTelemetry/Brave | Loki + recolector | Un panel único en Grafana; 4 a 5 contenedores nuevos; reutiliza Micrometer y el NDJSON |
| B. Prometheus + Grafana + Zipkin + logs sin panel | Prometheus | Zipkin (Brave ya está en `common-lib`) | NDJSON a mano | Menos contenedores; los logs siguen sin visualizar |
| C. Elastic (ELK) + APM | Elastic | Elastic APM | Elastic | Un solo producto, mucho más pesado en memoria |
| D. Spring Boot Admin + actuator | Actuator | — | — | Muy liviano; no cumple trazas ni logs |

`[INFERRED]` la opción A es la que cubre los tres tipos de señal con menos piezas, pero **requiere superar el ADR de trazabilidad** (que descartó los trazadores de terceros) con un ADR sucesor `proposed`, y agrega imágenes de Docker (AGENTS.md §6: pide aprobación previa).

**E7-Q05 — ¿Dónde corre el stack? ✅ (2026-11-16): A, compose propio `docker-compose.observabilidad.yml`; reinterpretada tras Q07 (C2) y confirmada por el grupo: namespace propio del cluster de Kubernetes**
> **Revisada el 2026-11-18 (§5, E7-S01):** sin Kubernetes, el stack de observabilidad vuelve a correr con Docker : según E7-S03, la imagen `otel-lgtm` corre como un servicio más del stack de Swarm.

⭐ **A. En un compose propio de observabilidad** (`docker-compose.observabilidad.yml`, opcional, junto al de preprod) · B. Dentro de `docker-compose.yml` · C. Solo en el preprod · D. Solo en la demo pública. `[OBSERVED]` hay siete archivos `docker-compose*.yml` en la raíz (base, demo, hybrid, preprod, publica, seed y tunel).

**E7-Q06 — ¿Qué se mide y qué se alerta? ✅ (2026-11-16): señales de oro, métricas de negocio podadas en el §3.1 y alertas solo en el panel**
> **Respuesta del grupo:** las señales de oro, las métricas de negocio mínimas y un tablero por servicio más uno general, con alertas solo en el panel. Pidió **más métricas de negocio** que las tres mínimas: el §3.1 propone un catálogo ampliado, ya podado por el grupo (Q06a y Q06b).

(Opciones originales, multiselección:)
⭐ Las señales de oro por servicio (latencia, tasa de error, tráfico, saturación) · ⭐ métricas de negocio mínimas (donaciones registradas, eventos AMQP procesados y fallidos, cola de RabbitMQ) · ⭐ un tablero por servicio más uno general · alertas por correo o Slack (suma un canal externo) · alertas solo en el panel (⭐ sin canal externo).

---

### 3.1 Catálogo ampliado de métricas de negocio `[PROPOSED]` (podado el 2026-11-16)

El grupo pidió más métricas de negocio que las tres mínimas (donaciones registradas, eventos AMQP procesados y fallidos, cola de RabbitMQ). Este catálogo sale de los estados, eventos y colas que ya existen (`docs/arquitectura/eventos-amqp.md`). **Prioridad:** **P1** = recomendada, **P2** = si alcanza el tiempo, **P3** = opcional.

**Reglas para todas** (AGENTS.md §4.3): etiquetas de baja cardinalidad (tipo, estado, canal, resultado), nunca ids de donación, de persona ni datos personales; los valores salen de los eventos y servicios de aplicación. **Excepción decidida en Q06b:** los medidores de estado actual (M02, M06, M11) pueden usar una consulta de conteo agrupado (`COUNT … GROUP BY estado`), refrescada y cacheada cada 30 s, con índice en la columna de estado; se prefiere a contar transiciones porque los contadores en memoria se reinician con cada pod y el autorestart los reinicia por diseño.

| ID | Servicio | Métrica | Qué pregunta responde | Pri. |
|---|---|---|---|---|
| M01 | Donaciones | Donaciones registradas (contador, por tipo) | ¿Cuánto se dona y cómo evoluciona? | P1 |
| M02 | Donaciones | Donaciones por estado actual (`EN_DEPOSITO`, `EN_TRASLADO`, `EN_PUERTA`, `ENTREGADA`, `ENTREGA_FALLIDA`, `VENCIDA`) | ¿Dónde está el stock en cada momento? ¿Se acumula algo? | P1 |
| M03 | Donaciones | Tiempo desde el registro hasta la entrega (histograma) | ¿Cuánto tarda una donación en llegar? | P1 |
| M04 | Donaciones | Entregas fallidas y replanificadas (contador) | ¿Qué proporción falla? | P1 |
| M05 | Donaciones | Pedidos de entrega al broker: enviados, rechazados por falta de cola y pendientes en el outbox | ¿Se atasca la salida hacia Logística? | P1 |
| M06 | Donaciones | Necesidades abiertas y cubiertas | ¿Se cubre lo que las entidades piden? | P2 |
| M07 | Donaciones | Importaciones de donantes: filas procesadas y rechazadas, duración | ¿Las cargas masivas funcionan? | P2 |
| M08 | Donaciones | Consultas GraphQL por operación: cantidad, duración y rechazos por complejidad | ¿Se usa el endpoint nuevo y qué cuesta? | P1 |
| M09 | Logística | Entregas por resultado (`exitosa`, `fallida`, `en-puerta`, `cancelada`) | ¿Cómo terminan las entregas? | P1 |
| M10 | Logística | Planificaciones ejecutadas: duración y rutas generadas | ¿Cuánto tarda planificar? | P2 |
| M11 | Logística | Camiones en ruta (medidor) y escrituras de posición por minuto | ¿Cuánta carga genera el seguimiento? Alimenta el rate limiting de Q12 | P1 |
| M12 | Logística | Fotos de recepción subidas a MinIO: correctas y fallidas | ¿Funciona el almacenamiento de objetos? | P3 |
| M13 | Logística | Callbacks de proveedores descartados (token incorrecto o entrega ajena) | ¿Hay intentos no válidos desde afuera? Es seguridad | P1 (promovida de P2 en Q06a) |
| M14 | Incentivos | Misiones cumplidas, subidas de categoría y donantes inactivos detectados | ¿Está viva la gamificación? | P2 |
| M15 | Incentivos | Puntos otorgados (contador) | ¿Cuánto reconocimiento se reparte? | P3 |
| M16 | Notificaciones | Notificaciones por canal y estado (enviada, fallida, pendiente) | ¿Llegan los avisos? | P1 |
| M17 | Notificaciones | Latencia entre el evento y el envío (histograma) | ¿Qué tan rápido avisamos? | P2 |
| M18 | Notificaciones | Mensajes en las colas muertas (`*.dlq`) | ¿Hay mensajes que no se pudieron procesar? Es la alerta más útil de la lista | P1 |
| M19 | Transversal | Eventos AMQP publicados y consumidos por routing key, con fallidos y reintentos | ¿Fluye la mensajería? | P1 |
| M20 | Transversal | Duplicados descartados por idempotencia (`evento_procesado`) | ¿Hay reentregas? Valida el patrón del ADR de idempotencia | P2 |
| M21 | Transversal | Profundidad de las colas de RabbitMQ | ¿Hay un consumidor caído o lento? | P1 |
| M22 | Cliente liviano | Consultas de seguimiento por segundo y páginas con error por pantalla | ¿Cómo se usa la interfaz? Alimenta Q12 | P2 |
| M23 | Autenticación | Inicios de sesión correctos y fallidos (de Keycloak, si se habilita su endpoint de métricas) | ¿Hay intentos de abuso? | P3 |

**Obligatorias por decisiones previas (no se podan):**

| ID | Métrica | Viene de |
|---|---|---|
| M24 | Pedidos rechazados por el rate limiting, por nivel y ruta | Q13 |
| M25 | Errores y timeouts de Redis en el gateway | Q11b (alerta del *fail-open*) |
| M26 | Reinicios de pods | Q08b (alerta de reinicios repetidos) |

**Poda ✅ (2026-11-16): Q06a = B, Q06b = A**
> **Revisada el 2026-11-18 (§5, E7-S03c):** el compromiso baja a las métricas automáticas más M01, M04, M13, M16, M24 y M25; M26 sale de `process_start_time`, y Q06b deja de aplicar.
> **Respuesta del grupo:** *Comprometido (13 + 3):* M01 a M05, M08, M09, M11, M13 (promovida a P1), M16, M18, M19 y M21, más M24 a M26. *Extensión opcional si sobra tiempo,* en este orden: M20, M06, M10, M07, M14, M17 y M22. *Fuera:* M12, M15 y M23 (P3). El grupo siguió la recomendación del agente, que cambió la propuesta original (P1 + P2, 20 métricas) por el plazo y porque `[OBSERVED]` hoy no hay ninguna métrica propia en el código: todas son código nuevo. `[INFERRED]` M18, M21 y parte de M19 pueden salir del plugin `rabbitmq_prometheus` de RabbitMQ (la imagen `rabbitmq:3.13-management-alpine` lo trae apagado), sin código propio; verificar al implementar. Q06b: los medidores de estado usan una consulta de conteo refrescada cada 30 s (regla ajustada arriba).

### Ronda 3 — Monitoreo proactivo y autorestart (RQ-E7-02)

**E7-Q07 — ¿Qué mecanismo reinicia los servicios? ✅ (2026-11-16): C2, todo en Kubernetes**
> **Revisada el 2026-11-18 (§5, E7-S01 a E7-S01d):** Kubernetes sale de E7. El despliegue pasa a Docker Swarm de un nodo, que replica los seis módulos propios y `[INFERRED]` (según la documentación de Docker, a verificar en la prueba) reinicia los contenedores caídos y los que quedan *unhealthy*. Q07b a Q07d (kind, convivencia con Compose, Kustomize) dejan de aplicar.
> **Respuesta del grupo:** opción C2, desplegar todo en Kubernetes, por el valor de aprender la herramienta. La recomendación del agente era C1 (Compose para desarrollo y CI, Kubernetes como segundo destino, con vuelta a la opción A si el cluster no estaba estable el 21 de noviembre `[PROPOSED]`). **Riesgos que el grupo toma con C2** `[INFERRED]`: son 14 contenedores a migrar (varias decenas de YAML, sin medir) en 11 días, junto con GraphQL, observabilidad, rate limiting y las cuatro medidas de seguridad; el CI de Gate 4 y los scripts de preprod hoy usan Compose; el entorno local ya tuvo inestabilidad con Docker (RabbitMQ `eacces`). **Consecuencias:** ADR ARCHITECTURAL de despliegue en Kubernetes (`proposed`); `autoheal` y el socket de Docker dejan de hacer falta; **Q05 se reinterpreta** (el stack de observabilidad corre en un namespace propio del cluster, no en un `docker-compose.observabilidad.yml`); AGENTS.md §11 y `docs/testing/` mencionan Compose, y AGENTS.md solo se modifica en un PR aparte con revisión humana (§13). Quedan abiertas Q07b a Q07d, más abajo.

(Opciones originales:)

| Opción | Qué implica |
|---|---|
| ⭐ **A. `restart: unless-stopped` + `healthcheck` + un autoheal** | Docker reinicia los procesos que caen; un contenedor "autoheal" reinicia los que quedan `unhealthy`. Usa los `healthcheck` ya escritos. Una imagen nueva. |
| B. Solo `restart: on-failure` de Docker | Cubre caídas del proceso, no cuelgues con el contenedor vivo. Sin dependencias nuevas. |
| C. Kubernetes (probes y `restartPolicy`) | Es el estándar de la industria, pero implica migrar el despliegue: fuera de proporción para 11 días. |
| D. Supervisor propio en un script | No recomendable: reimplementa lo que Docker ya hace. |

**E7-Q07b a Q07d — Consecuencias de elegir Kubernetes ✅ (2026-11-16): kind, Compose convive y el CI migra al final, Kustomize**
> **Respuesta del grupo:** Q07b **kind**; Q07c **A**: Compose convive durante E7 y el CI migra al final solo si Kubernetes pasa todas las pruebas (la demo pública y el túnel siguen en Compose); Q07d **YAML con Kustomize**. `[INFERRED]` Con Compose y Kubernetes en paralelo hay dos definiciones del despliegue que pueden desfasarse: conviene una prueba o un chequeo que las compare (puertos, variables y imágenes) antes de migrar el CI.

(Opciones originales:)
- **Q07b — ¿Qué cluster local?** Opciones `[INFERRED]`: ⭐ **kind** (Kubernetes dentro de contenedores Docker, reproducible y usable en GitHub Actions) · Docker Desktop Kubernetes (ya instalado, un solo nodo, menos reproducible) · minikube (más herramientas, más pesado) · k3d (liviano, menos difundido).
- **Q07c — ¿Qué pasa con Docker Compose?** ⭐ **Conviven durante E7 y el CI migra al final, solo si Kubernetes pasa todas las pruebas** (la demo pública y el túnel siguen en Compose) · se retira ya · queda para siempre como alternativa.
- **Q07d — ¿Cómo se escriben los manifiestos?** ⭐ **YAML con Kustomize** (viene en `kubectl`, sin plantillas, base más variantes) · un Helm chart propio · generarlos con `kompose` y ajustarlos a mano.

**E7-Q08 — ¿Qué se supervisa y con qué política? ✅ (2026-11-16): Q08a = 1, Q08b = 1, Q08c = sí**
> **Respuesta del grupo** (formulación final, con recomendación y alternativa en cada una): **Q08a:** se vigilan los cuatro servicios, el gateway, el cliente **y** la infraestructura (Postgres, Mongo, RabbitMQ, Keycloak, MinIO). **Q08b:** reinicio **sin tope** (Kubernetes lo hace por defecto, con espera creciente), más una alerta en Grafana cuando un servicio se reinicia varias veces seguidas. **Q08c:** se **separan liveness y readiness** (probes de Kubernetes y grupos de salud de Spring Boot); el reinicio mira solo liveness, porque `[OBSERVED]` el `/actuator/health` de Incentivos incluye a RabbitMQ y un corte breve provocaría reinicios en cascada.

(Planteo original:)
Alcance: ⭐ los cuatro servicios, el gateway y el cliente; ⭐ la infraestructura (Postgres, Mongo, RabbitMQ, Keycloak, MinIO). Política: ⭐ reinicio con espera creciente y tope de intentos, y visibilidad de cada reinicio en el panel; reinicio siempre y sin tope. `[OBSERVED]` hoy `/actuator/health` no distingue *liveness* de *readiness*: ⭐ habilitar los grupos de salud de Spring Boot (liveness y readiness) y usarlos en los `healthcheck`.

**E7-Q09 — ¿Cómo se demuestra el autorestart? ✅ (2026-11-16): A, script de caos reproducible** (con Kubernetes: `kubectl delete pod` y espera de la recuperación)
> **Revisada el 2026-11-18 (§5, E7-S01c):** `[PROPOSED]` el script mata un contenedor de una réplica (`docker kill`) y espera a que Swarm lo reemplace.
⭐ Un script de caos (`docker kill` de un servicio y espera de recuperación) que se corre en la defensa y queda en el CI como prueba de preprod · solo una demostración manual · sin demostración automática.

---

### Ronda 4 — Rate limiting (RQ-E7-03)

**E7-Q10 — ¿Dónde se limita? ✅ (2026-11-16): A, en el api-gateway**
> **Respuesta del grupo:** un filtro en el gateway. `[OBSERVED]` el jar de Spring Cloud Gateway 5.0.1 (WebMVC) ya trae `Bucket4jFilterFunctions` (capacidad, período, resolvedor de clave, código de estado, encabezado y *timeout*); `api-gateway/pom.xml` no declara todavía la librería Bucket4j. Quedan fuera: el Ingress y los servicios.

| Opción | Qué implica |
|---|---|
| ⭐ **A. En el api-gateway** | Un punto único, antes de que el tráfico llegue a los servicios. `[INFERRED]` hay que verificar qué ofrece la variante WebMVC del gateway (filtro propio o Bucket4j) antes de elegir la librería. |
| B. En cada servicio con Resilience4j | Protege también el tráfico interno; repite configuración cuatro veces. |
| C. En el cliente liviano | Protege solo lo que pasa por la interfaz; no protege las APIs. |
| D. Gateway más límites finos en servicios críticos | Más robusto, más trabajo. |

**E7-Q11 — ¿Qué librería y dónde viven los contadores? ✅ (2026-11-16): B, Bucket4j con Redis**
> **Respuesta del grupo:** Bucket4j con los contadores en un Redis compartido (la recomendación del agente era A, en memoria con una réplica). Consecuencias `[PROPOSED]`: un workload nuevo de Redis en el cluster (sin persistencia, sus contadores son descartables) y dos o más dependencias nuevas en el gateway (Bucket4j y su integración con Redis); se toma la elección de B como aprobación de esas dependencias y de la imagen de Redis (AGENTS.md §6), a confirmar con el grupo; el gateway puede escalar a varias réplicas con un límite único y exacto; Redis entra en las probes y en el panel de observabilidad. Abiertas Q11b y Q11c.

(Planteo original de Q11:) ⭐ **A. Bucket4j con contadores en memoria** (una sola instancia del gateway; sin infraestructura nueva) · B. Bucket4j con Redis (necesario si el gateway se replica; suma un contenedor y una dependencia) · C. Filtro propio sin librería. `[OBSERVED]` hoy hay una sola instancia del gateway.

**E7-Q11b — Si Redis no responde, ¿el gateway deja pasar o rechaza? ✅ (2026-11-16): A, deja pasar con alerta**
> **Respuesta del grupo:** *fail-open*. Si Redis no responde dentro del *timeout* configurado en el filtro, el gateway deja pasar el pedido y se dispara una alerta en el panel; el autorestart de Kubernetes levanta Redis. Riesgo aceptado `[PROPOSED]`: durante la caída no hay protección contra abuso. Hay que fijar el *timeout* corto, para que esperar a Redis no degrade el sitio.

(Planteo original:) ⭐ **Deja pasar (*fail-open*) con una alerta:** prioriza que el sistema siga disponible; durante la caída no hay límite. · Rechaza todo (*fail-closed*): protege contra el abuso pero convierte a Redis en un punto único de falla de todo el sitio. · Cae a un contador local en memoria por réplica: se pierde exactitud pero se conserva algo de protección. `[OBSERVED]` el filtro del gateway tiene un *timeout* configurable.

**E7-Q11c — ¿Cuántas réplicas del gateway? ✅ (2026-11-16): A, 2 réplicas**
> **Revisada el 2026-11-18 (§5, E7-S01c y E7-S01d):** se mantienen Redis y las dos réplicas del gateway, ahora detrás del *routing mesh* de Swarm; la cantidad de réplicas sale de una variable (1 en desarrollo, 2 en la defensa).
> **Respuesta del grupo:** dos réplicas del gateway detrás de un Service de Kubernetes, con el límite contado en conjunto en Redis. Riesgo `[INFERRED]`: más consumo en el entorno local con antecedentes de inestabilidad; el manifiesto debe fijar requests y limits de recursos.

(Planteo original:) ⭐ **2 réplicas:** muestra el beneficio de Redis (el límite se cuenta en conjunto) y la tolerancia a fallos de Kubernetes; consume más recursos en un entorno local ya inestable. · 1 réplica: más liviano, pero Redis no aporta nada que la memoria no diera.

**E7-Q12 — ¿A quién y con qué límites? ✅ (2026-11-16): identificación en dos niveles y límites iniciales**
> **Revisada el 2026-11-18 (§5, E7-S02 y E7-S02b):** quedan los cuatro cupos del PR 986, con sus valores; la IP real iba a llegar a través de un nginx delante, en lugar de un Ingress. Después de la prueba de Swarm, E7-S02b se revisó: sin proxy delante, la IP queda aproximada y los visitantes sin cuenta comparten la cubeta por IP.
> **Respuesta del grupo:** por IP para el tráfico público y por `sub` del token cuando hay usuario. Límites iniciales `[PROPOSED]`, a calibrar con k6 (`tests/performance/`): público 60 por minuto por IP; autenticado en lectura 300 por minuto por usuario; escrituras (POST, PUT, DELETE) 30 por minuto; subidas pesadas (fotos, importaciones) 10 por minuto; seguimiento del camión **aparte**, 120 por minuto por usuario (el ADR-E6-07 pide contarlo o excluirlo); endpoint GraphQL 60 por minuto por usuario **más un tope de profundidad y complejidad dentro del servicio**, porque todas las consultas entran por una sola URL. `[INFERRED]` Con un Ingress delante, el gateway ve la IP del Ingress: hay que leer `X-Forwarded-For` y confiar solo en el proxy propio, o la IP se puede falsificar.

(Planteo original:)
Clave del límite: ⭐ por usuario autenticado (`sub`) y por IP para el tráfico público · solo por IP · por ruta. Límites base `[PROPOSED]`: público 60 solicitudes por minuto por IP; autenticado 300 por minuto por usuario; rutas de escritura más bajas; el seguimiento del camión (polling cada 30 s) **excluido o con un límite aparte**, como pide el ADR-E6-07. Los valores se calibran con k6.

**E7-Q13 — ¿Cómo responde y qué se registra? ✅ (2026-11-16): A, respuesta completa**
> **Respuesta del grupo:** `429` con `Retry-After`, el formato de error del catálogo, el `traceId`, una métrica de rechazos y una alerta en el panel. `[INFERRED]` el filtro de Bucket4j configura un encabezado propio, pero `Retry-After` probablemente haya que agregarlo a mano: verificar al implementar.

(Planteo original:)
⭐ `429 Too Many Requests` con `Retry-After`, el `traceId` de la solicitud, un contador de rechazos en las métricas y una alerta en el panel · solo `429` sin métricas. `[OBSERVED]` el [catálogo de errores](../arquitectura/catalogo-errores.md) fija el formato de error, y la respuesta tiene que respetarlo.

---

### Ronda 5 — Detalle de la opción elegida en E7-Q01 (RQ-E7-04)

Q01 se respondió: **GraphQL en Donaciones** (Logística, abierta). La rama gRPC queda descartada. La nueva interfaz es aditiva: los contratos REST no cambian (AGENTS.md §8.2).

**E7-Q18 — ¿Qué se puede consultar? ✅ (2026-11-16): B, con una extensión opcional si sobra tiempo**
> **Revisada el 2026-11-18 (§5, E7-S04c):** donaciones, necesidades y entidades públicas, sin el donante ("quién donó") y sin la extensión opcional; GraphQL en Logística queda fuera de E7.
> **Respuesta del grupo:** B (donaciones, necesidades y entidades públicas). La consulta de "quién donó" entra en B con visibilidad mínima: el donante sale con su nombre visible o su categoría, según su preferencia (ADR del nombre visible), sin documento, dirección ni medios de contacto. Como **extensión opcional si sobra tiempo**, el grupo eligió dejar una de las dos piezas de C. `[PROPOSED]` Como "quién donó" ya está dentro de B, la pieza opcional es la otra mitad de C: las asignaciones (`/api/asignaciones`, operativas, de administración) o el detalle del donante para el propio donante (`DONANTE + SU_ID`). Cuál se hace, y si se hace, se decide al implementar, con el esquema de B ya terminado y los tests de privacidad en verde; no entra en el alcance comprometido.

(Planteo original:) A. Solo donaciones · B. Donaciones, necesidades y entidades públicas · C. B más donantes y asignaciones. ⭐ B.
`[OBSERVED]` `DonacionOutputDTO.donante` devuelve la persona entera (documento, dirección, medios de contacto); `DonantesController` y `PropuestaDeAsignacionController` (`/api/asignaciones`) están reservados a administración y al propio donante (`@PreAuthorize`).

**E7-Q19 — ¿Solo lectura o también mutaciones? ✅ (2026-11-16): A, solo lectura**
> **Respuesta del grupo:** solo consultas (*queries*). Las escrituras siguen por REST, con su Outbox, su idempotencia y sus contratos.

**E7-Q20 — ¿Cómo se autoriza y se protege la privacidad? ✅ (2026-11-16): A, adaptador sobre los servicios de aplicación**
> **Respuesta del grupo:** los resolvers de GraphQL son adaptadores de entrada, como los controladores REST (AGENTS.md §4.2): delegan en los servicios de aplicación existentes y heredan la misma autorización y las mismas reglas de privacidad. No hay acceso directo a repositorios. El problema de las consultas N+1 en relaciones anidadas se mitiga con carga por lotes en los resolvers.

**E7-Q21 — ¿Cómo se evitan las consultas costosas? ✅ (2026-11-16): todos los límites**
> **Respuesta del grupo:** `[PROPOSED]` paginación obligatoria con tope (por ejemplo, 50), profundidad máxima (por ejemplo, 5), tope de complejidad por consulta, *timeout* por consulta, e introspección y GraphiQL apagados por defecto y encendidos solo en preprod y demo. Se suman al límite por minuto del gateway (Q12), que cuenta pedidos y no su costo. Valores a calibrar al implementar.

**E7-Q22 — ¿Cómo se expone y quién lo consume? ✅ (2026-11-16): exposición recomendada; consumo B**
> **Revisada el 2026-11-18 (§5, E7-S04b):** en lugar de una pantalla nueva, una pantalla existente cambia su adaptador para pedir sus datos por GraphQL.
> **Respuesta del grupo:** *Exposición:* ruta `/api/donaciones/graphql` detrás del gateway (JWT, rate limiting y `traceId` aplican solos; `/api/logistica/graphql` si Logística se confirma); el esquema `.graphqls` vive en el repo, se documenta junto a los contratos de `docs/arquitectura/contratos/` y un test de contrato lo vigila. *Consumo:* **B**, la API más una pantalla del cliente liviano que la use, con tests y scripts de k6. El grupo eligió B y no A, que era la recomendación del agente. Riesgos `[INFERRED]`: la pantalla debe respetar el sistema visual cerrado del cliente liviano (skills `construir-ui` y `revisar-ui`); el cliente renderiza en el servidor y llama a las APIs desde adaptadores en `cliente/rest/`, así que la pantalla consume GraphQL desde un adaptador nuevo, y hay que elegir cuál (pantalla a definir al implementar, con el grupo).

**E7-Q23 — ¿Qué librería de GraphQL? ✅ (2026-11-16): A, Spring for GraphQL**
> **Respuesta del grupo:** Spring for GraphQL (integración oficial del ecosistema Spring). Se toma la elección como aprobación de la dependencia nueva (AGENTS.md §6), a confirmar con el grupo. `[INFERRED]` verificar la compatibilidad con la versión de Spring Boot del repo antes de implementar.

---

### Ronda 6 — Entregables, plan y cierre

**E7-Q14 — Diagrama de componentes y despliegue (E7-ENT-1) ✅ (2026-11-16): PlantUML, despliegue solo en Kubernetes, borrador temprano y versión final**
> **Revisada el 2026-11-18 (§5, E7-S01):** el diagrama de despliegue se redibuja con Docker Swarm de un nodo en lugar de Kubernetes; el alcance del dibujo se revisa en la Ronda S5.
> **Respuesta del grupo:** *Q14a A:* PlantUML con PNG renderizado en `docs/entrega-7/arquitectura/diagramas/`, con el estilo de E4 (`estilo-e4.iuml`) y el job `generate-diagrams` del CI. *Q14b C* (el grupo no siguió la recomendación del agente, que era A): el diagrama de despliegue muestra **solo Kubernetes**; Compose (demo pública y túnel) no se dibuja. Riesgo asumido `[INFERRED]`: si la cátedra pregunta por la demo pública, no está en el diagrama; se cubre en la defensa. *Q14c B:* un borrador al comienzo y la versión final al cierre; el grupo pidió que **el borrador esté lo mejor diagramado posible**, porque sirve de acuerdo del grupo y de referencia para los ADR. El de componentes es uno solo e incluye gateway, Keycloak, MinIO, cliente liviano, Redis y el stack de observabilidad. Criterio de la cátedra `[OBSERVED]` (memoria de las devoluciones): diagramas legibles impresos (7 pt o más), sin evidencia del repo, con decisiones justificadas.

(Planteo original:) `[OBSERVED]` los diagramas existentes son anteriores al gateway, Keycloak, MinIO y el cliente liviano. Formato: ⭐ **PlantUML** en el repo (como `08-despliegue.puml`) · Mermaid en Markdown · draw.io. Contenido: ⭐ dos diagramas separados (componentes y despliegue). Cuándo: ⭐ se redibujan al final, con lo realmente implementado.

**E7-Q15 — ¿Cómo se parte el trabajo? ⏳ (2026-11-16): diferida a una sesión de planificación con Opus; propuesta lista el 2026-11-17 y ajustada ese día con el primer feedback del grupo (versión 2); falta el ajuste del resto**
> **Respuesta del grupo:** no se decide acá. El reparto se diseña en una sesión aparte, con un modelo Opus, asignando aproximadamente **un frente a cada compañero**, y con un **diagrama de Gantt dinámico o un CPM (método del camino crítico)**, porque las estimaciones de tiempo pueden ser erróneas. Insumos para esa sesión `[PROPOSED]`, no decididos:
>
> | # | Rama `E7_*` | Qué lleva | Depende de |
> |---|---|---|---|
> | 0 | `E7_adrs` | Los cuatro ADR `proposed` (Q16b B: van primero, en su propio PR) | — |
> | 1 | `E7_k8s_base` | kind, Kustomize, los 14 contenedores, Redis, probes de liveness y readiness, reinicio automático | — (camino crítico) |
> | 2 | `E7_metricas` | Micrometer y Prometheus en los servicios, las 13 métricas comprometidas, endpoints de observabilidad protegidos | — |
> | 3 | `E7_observabilidad_stack` | Prometheus, Tempo, Loki y Grafana en su namespace, tableros, alertas y el script de caos | 1 y 2 |
> | 4 | `E7_rate_limiting` | Bucket4j con Redis, 2 réplicas, `429` completo, M24 y M25 | 1 |
> | 5 | `E7_graphql` | Esquema B en Donaciones, límites, M08, tests de contrato y k6 | — |
> | 6 | `E7_graphql_pantalla` | Pantalla del cliente que lo consume | 5 |
> | 7 | `E7_seguridad` | Encabezados, cadena JWT en preprod, escaneo en el CI | — |
> | 8 | `E7_diagramas` | Borrador temprano y versión final | 1 a 7 |
>
> Notas para la planificación `[INFERRED]`: el prefijo `E7_*` sale de la regla de E4 (verificarla con el primer PR); si el CI migra a Kubernetes (Q07c), AGENTS.md §11 se cambia en un PR aparte con revisión humana (AGENTS.md §13); con Q16b B el PR de ADR entra al camino crítico antes de implementar y su revisión tiene que ser rápida; el plazo era el 27 de noviembre de 2026 (11 días desde el 16 de noviembre). **Corrección del 2026-11-17:** la defensa es el viernes 4 de diciembre de 2026 (18 días desde el 16 de noviembre).

**Propuesta de reparto de la sesión de planificación, versión 2 `[PROPOSED]` (2026-11-17), con el primer feedback del grupo; falta el ajuste del resto**

*Plan vivo.* El reparto completo está en un artifact de Claude, "Plan E7 DonaTrack" (`https://claude.ai/artifact/WrmqZdidxaqL5Cw6cHLLfS`), versión 2 del plan: calendario por persona, tabla de tareas editable, dedicación y ausencias por persona, camino crítico, holguras y probabilidad de llegar al congelamiento. Se recalcula al cambiar una estimación, un avance, una predecesora, un dueño, una dedicación o un período. Es privado hasta que Tadeo lo comparta desde su menú; que se pueda abrir sin cuenta de Claude está sin verificar (probarlo en una ventana privada). Esta sección es la línea base del 17 de noviembre; si difiere del artifact, vale el artifact.

*Cómo se actualiza.* Matriz de la decisión (los compañeros no tienen cuenta de Claude, y en cualquier opción el único que puede escribir es Tadeo):

| Criterio (peso) | ⭐ A. La página se guarda a sí misma | B. Base de datos del artifact | C. JSON en el repo |
|---|---|---|---|
| Se ve sin cuenta (40 %) | Sí: el estado viaja dentro de la página (`[INFERRED]`, falta probarlo sin cuenta) | No: sin sesión no carga | Sí |
| Esfuerzo por cambio (30 %) | Tadeo edita y guarda; el resto avisa por el grupo | Igual que A, solo con cuenta | Un PR por estimación |
| Simular escenarios (20 %) | Sí, en el navegador de cada uno, sin guardar | No | No |
| Complejidad (10 %) | Baja | Media | Media |

Elegida A. Cada compañero cambia sus valores en su navegador, toca "Copiar cambios" y pega el texto en el grupo; Tadeo los carga y toca "Guardar para el grupo".

*Calendario.* Origen 16/11 · sprint de cluster del miércoles 18 al domingo 22/11 · **punto de control al final del domingo 22/11** · parcial de DDS el viernes 27/11 · congelamiento de código al final del miércoles 2/12 (parámetro editable) · jueves 3/12, integración final y ensayo de la defensa · viernes 4/12, defensa.

*Feedback del grupo (16 y 17/11).* Anush pidió Seguridad, porque rinde tres parciales muy juntos. Miranda y Ber ya trabajan en el rate limiting: [PR 986](https://github.com/tsorren/DonaTrack-TP-DDS/pull/986), en borrador desde la rama `E7_rate_limit`. Tadeo toma el esqueleto del cluster, el arranque completo y Redis dentro del PR 986. Todo el grupo rinde el segundo parcial de DDS el 27/11 y puede tener otros exámenes (el grupo estima 2 o 3 días perdidos por parcial `[INFERRED]`). La memoria máxima de las máquinas del grupo es 16 GB: la tienen Tadeo y Nicolás, probablemente Ber y Miranda, y varios tienen menos.

*Divergencia del PR 986 con Q11* `[OBSERVED]`. El PR 986 usa Bucket4j **en memoria** y trae su propio ADR (`proposed`, fechado el 15/11). Q11 eligió el 16/11 Bucket4j con Redis, *fail-open* y dos réplicas del gateway; el ADR del PR reconoce que, con dos réplicas, cada una cuenta por su lado y el cupo se duplica. Decisión de Tadeo (17/11): Q11 no cambia; él suma Redis y el *fail-open* dentro del PR 986 (tarea R1b) y se lo comunica al grupo, que no estaba al tanto. Decisión de Tadeo (17/11, al arrancar A0): el ADR del PR 986, ya actualizado a Redis, se mueve al PR de los ADR (A0), que lleva los cuatro de Q16a, como pidió Q16b; el PR 986 se integra después de A0 y quita su copia del ADR y sus entradas de índice (`60db332f4` en `E7_rate_limit`). El PR 986 cubre además parte de R2 (cuatro cupos, lecturas y escrituras por cuenta o por IP, contra las seis clases de Q12; `429` con `Retry-After`, `ERR-CSR-429` y `traceId`); no trae M24/M25 (R3) ni la calibración con k6 (R4).

*Dedicación y ausencias* `[INFERRED]`. La versión 1 suponía que cada persona trabaja una jornada completa todos los días. El historial de git no lo sostiene: en el cierre de E6 los integrantes trabajaron en ráfagas de 1 a 3 días por semana. La versión 2 le da a cada persona una dedicación (qué parte de una jornada completa rinde cada día corrido) y períodos con otra dedicación:

- **Fórmula de partida.** Intensidad × horas / 8, con piso de 0,3. Intensidad: en cuántos de los días que duró cada PR propio hubo commits (la mediana de la persona). Horas: las que pasan entre el primer y el último commit de un día activo, en promedio. La fórmula se calibró contra el PR 986: predice unos 2 días, y tardó entre 2 y 3. No ve que con agentes se hace más por hora. Cada uno corrige la suya.
- **Valores de partida.** Tadeo 0,64 · Ber 0,39 · Camila Lencina 0,59 · Martín 0,39 · Anush 0,3 · Camila Suarez 0,3 · Nicolás 0,6 · Sofía 0,62 · Miranda 0,49.
- **Sprint de cluster.** Tadeo, Ber y Camila Lencina con dedicación completa del 18 al 22/11, para que la cadena de Kubernetes termine antes de los parciales.
- **Parcial de DDS.** La mitad de la dedicación de cada uno el 25/11 y nada el 26 y el 27/11, para todos menos Anush.
- **Anush.** Sin dedicación del 20 al 27/11 hasta que confirme las fechas de sus tres parciales. Los demás exámenes de cada uno están sin relevar.
- **Cierre de Tadeo.** Dedicación completa del 28/11 al 2/12, para la verificación final, los diagramas y la integración.

*Punto de control del 22/11.* Al cierre del sprint, si el cluster no levanta todos los servicios (K5), la integración y la demo pasan a Docker Compose y Kubernetes queda como segundo destino (la opción C1 de Q07, que el agente había recomendado). La decisión es del grupo; la versión 2 la deja como hito.
> **Revisado el 2026-11-18 (§5, E7-S01):** el grupo adelantó la salida de Kubernetes. `[PROPOSED]` El sprint de cluster y este punto de control se reemplazan por una prueba de Swarm de media jornada (decidido en E7-S05d; se refleja en la versión 3 del plan).

*Workflow de agentes (W1).* Tadeo prepara y lanza un workflow de agentes, como en E6, que escribe las tareas de código bien acotadas: S1, S2, M2 a M4, R3, O3, O4 y G3. El dueño de cada una revisa y prueba lo que sale; su estimación baja al 40 % de la original `[INFERRED]`. W1 arranca después del esqueleto del cluster (K1).

*Reparto.* Los frentes ya no son de una sola persona: cada tarea va a quien la puede hacer a tiempo. La oferta son los días-persona que rinde cada uno del 17/11 al congelamiento, con su dedicación y sus períodos; la demanda es lo que le falta de sus tareas comprometidas. Cifras del generador con "hoy" = 17/11 `[INFERRED]`.

| Persona | Tareas | Demanda / oferta (días-persona) | Termina |
|---|---|---|---|
| Tadeo | A0, W1, K1, K5, R1b, O5, A1, D1, I2 (+ K7 condicional) y la revisión de PRs | 9,3 / 12,2 + revisiones | 02/12 |
| Ber | K2, R3, R4 | 4,2 / 8,3 | 29/11 |
| Camila Lencina | K3, K4, L1, K6 | 5,3 / 10,0 | 24/11 |
| Martín | M1 a M4 | 3,2 / 5,3 | 29/11 |
| Anush | S1, S2 | 0,7 / 2,4 | 30/11 |
| Camila Suarez | O1, O3, O4 | 2,4 / 4,0 | 30/11 |
| Nicolás | G1 a G4 | 4,7 / 8,1 | 28/11 |
| Sofía | P1 a P3, O2 (+ P4 condicional) | 4,4 / 8,4 | 29/11 |
| Miranda | R1 (hecha en el PR 986), R2, S3, S4 | 4,0 / 6,6 | 30/11 |

*Criterios de asignación (versión 2).* (1) Las tareas de la cadena de Kubernetes van a quienes hacen el sprint; (2) lo que necesita el cluster completo (O5, I2) va a quien tiene 16 GB; (3) la cadena JWT (S3, S4) va a Miranda, que hizo la etapa 2 del Auth Service (PRs 962 y 969); (4) el pedido de cada integrante se respeta: Anush se queda con lo liviano de Seguridad y Miranda con el rate limiting; (5) cada persona termina antes del congelamiento con su dedicación. Evidencia `[OBSERVED]`: historial de git, PRs del grupo desde septiembre y el chat del grupo del 16 y 17/11.

*Tareas, estimaciones y dependencias* (días-persona de jornada completa; o = optimista, m = probable, p = pesimista; valores de partida del agente `[INFERRED]`, cada dueño corrige los suyos):

| ID | Dueño | Tarea | o / m / p | Depende de |
|---|---|---|---|---|
| A0 | Tadeo | PR de los cuatro ADR revisado (el de rate limiting se mueve desde el PR 986) | 0,25 / 0,5 / 1 | — |
| W1 | Tadeo | Workflow de agentes para las tareas de código: preparar, lanzar y repartir lo que sale (después del esqueleto del cluster) | 1 / 1,5 / 3 | K1 |
| K1 | Tadeo | kind, Kustomize y namespaces (esqueleto del cluster) | 0,5 / 1 / 2 | A0 |
| K2 | Ber | Infra en el cluster: Postgres, Mongo, RabbitMQ, Keycloak, MinIO, n8n y Redis | 1,5 / 2,5 / 5 | K1 |
| K5 | Tadeo | Sistema completo arrancando en kind, con Ingress (punto de control del 22/11) | 0,5 / 1 / 3 | K2, K4 |
| K7 | Tadeo | *Condicional:* migrar el CI a Kubernetes y el PR de AGENTS.md §11 (Q07c) | 1 / 1,5 / 3 | K6, O5 |
| K3 | Camila Lencina | Grupos de salud liveness y readiness en los seis módulos | 0,5 / 1 / 1,5 | A0 |
| K4 | Camila Lencina | Manifiestos de las seis apps: probes, recursos, gateway con dos réplicas | 1 / 2 / 3,5 | K1, K3 |
| L1 | Camila Lencina | Loki y recolector de logs de los pods desde el nodo | 1 / 1,5 / 3 | K1 |
| K6 | Camila Lencina | Script de caos y prueba del autorestart | 0,5 / 0,5 / 1 | K5 |
| M1 | Martín | Micrometer y Prometheus en common-lib; `/actuator/prometheus` protegido | 0,5 / 1 / 2 | A0 |
| M2 | Martín | Métricas de Donaciones M01 a M05 *(workflow)* | 0,6 / 1 / 1,6 | M1, W1 |
| M3 | Martín | Métricas de Logística M09, M11 y M13 *(workflow)* | 0,4 / 0,6 / 1 | M1, W1 |
| M4 | Martín | Métricas de Notificaciones M16 y transversales M19 *(workflow)* | 0,2 / 0,4 / 0,8 | M1, W1 |
| O1 | Camila Suarez | Prometheus y Grafana en el namespace de observabilidad | 0,5 / 1 / 2 | K1 |
| O2 | Sofía | Tempo recibiendo las trazas de Brave (formato Zipkin) | 0,5 / 1 / 2 | O1 |
| O3 | Camila Suarez | Scrape de servicios y RabbitMQ (M18, M21); tableros general y por servicio *(workflow)* | 0,6 / 0,8 / 1,6 | O1, M1, W1 |
| O4 | Camila Suarez | Alertas en el panel: reinicios M26, Redis M25, rechazos M24, colas muertas M18 *(workflow)* | 0,2 / 0,4 / 0,8 | O3, W1 |
| O5 | Tadeo | Verificación de punta a punta en el cluster: traza, logs y métricas (16 GB) | 0,5 / 1 / 2 | O2, L1, O4, K5, M2, M3, M4 |
| R1 | Miranda | Bucket4j en el gateway; clave por IP y `sub`; 429 (hecho en el PR 986, con Ber) — avance 100 % | 1 / 1,5 / 3 | — |
| R1b | Tadeo | Redis y *fail-open* dentro del PR 986 (decisión Q11 del grupo) | 0,5 / 1 / 2 | R1 |
| R2 | Miranda | Seis clases de límite de Q12; 429 con Retry-After, catálogo y traceId (en parte en el PR 986) — avance 50 % | 1 / 1,5 / 2,5 | R1 |
| R3 | Ber | Métricas M24 y M25 con tests *(workflow)* | 0,2 / 0,4 / 0,6 | R2, R1b, M1, W1 |
| R4 | Ber | Calibración con k6 en el cluster con dos réplicas | 0,5 / 1 / 2 | R3, K5 |
| G1 | Nicolás | Esquema GraphQL acordado (contrato B) | 0,5 / 0,5 / 1 | A0 |
| G2 | Nicolás | Resolvers sobre los servicios de aplicación, carga por lotes y privacidad del donante | 1,5 / 2,5 / 4 | G1 |
| G3 | Nicolás | Límites (paginación, profundidad, complejidad, *timeout*) y M08 *(workflow)* | 0,2 / 0,4 / 0,8 | G2, M1, W1 |
| G4 | Nicolás | Ruta en el gateway, test de contrato y k6 | 0,5 / 1 / 2 | G3 |
| P1 | Sofía | Elegir y diseñar la pantalla que consume GraphQL | 0,5 / 0,5 / 1 | G1 |
| P2 | Sofía | Adaptador GraphQL del cliente y pantalla contra el esquema | 1 / 1,5 / 3 | P1 |
| P3 | Sofía | Integración con la API real, revisión de UI y tests | 0,5 / 1 / 2 | P2, G2 |
| P4 | Sofía | *Condicional:* métricas opcionales y extensión C de GraphQL | 1 / 2 / 3 | P3 |
| S1 | Anush | Encabezados de seguridad en el gateway *(workflow)* | 0,2 / 0,2 / 0,4 | A0, W1 |
| S2 | Anush | Escaneo de dependencias e imágenes en el CI *(workflow)* | 0,2 / 0,4 / 0,8 | A0, W1 |
| S3 | Miranda | Cadena JWT encendida en el preprod con Keycloak real | 1 / 2 / 4 | A0 |
| S4 | Miranda | Cadena JWT y encabezados en el cluster | 0,5 / 1 / 2 | S3, S1, K5 |
| A1 | Tadeo | Borrador de los diagramas de componentes y despliegue | 0,25 / 0,5 / 1 | — |
| D1 | Tadeo | Diagramas: versión final | 0,5 / 1 / 1,5 | K5, G4 |
| I2 | Tadeo | Integración final en `ENTREGA_7` y prueba de humo | 0,5 / 1 / 2 | K6, O5, R4, G4, P3, S4, S2, M4 |

*Método.* Duración esperada PERT = (o + 4m + p) / 6, en días-persona; la dedicación de cada día la convierte en días corridos. Cada persona toma sus tareas por orden de inicio posible y rellena los huecos: si espera a otra, avanza con lo que ya puede hacer, sin partir tareas. El camino crítico cuenta ese orden además de las dependencias. Las condicionales no le quitan tiempo a lo comprometido. La probabilidad sale de 600 simulaciones con distribución triangular.

*Resultado de la línea base, versión 2* `[INFERRED]` (con "hoy" = 17/11): tareas críticas **A0, A1, K1, O1, O3, O4, O5 e I2** (A1 no bloquea a K1: es crítica porque Tadeo la hace entre A0 y K1, y cualquier demora suya corre todo lo que sigue). Fin esperado el **miércoles 2/12**, con 0,75 días de margen hasta el congelamiento. Solo el 13 % de las simulaciones llega al congelamiento, y el 80 % termina a más tardar el domingo 6/12, después de la defensa. Es un plan al límite: depende del sprint, del punto de control y del cierre de Tadeo. Si el grupo no puede sostener el sprint, la única palanca que queda es la del punto de control (Compose).

*Cambios respecto de la versión 1.* Rate limiting: de Camila Suarez a Miranda (con Ber), más R1b para Tadeo. Seguridad: S1 y S2 a Anush, S3 y S4 a Miranda. Stack de observabilidad: de Anush a Camila Suarez (O1, O3, O4), con Tempo (O2) para Sofía y la verificación final (O5) para Tadeo. Kubernetes: K1 y K5 a Tadeo; Ber se queda con K2 y suma R3 y R4. Tareas nuevas W1 y R1b. D1 ya no espera a R4. Línea base anterior: fin jueves 26/11, margen 6,7 días, sin dedicación ni parciales.

**E7-Q16 — ¿Qué ADRs hacen falta? ✅ (2026-11-16): los cuatro; Q16b B, primero y en un PR aparte**
> **Respuesta del grupo:** *Q16a:* cuatro ADR `proposed`: (1) despliegue en Kubernetes (ARCHITECTURAL: kind, Kustomize, convivencia con Compose, probes y política de reinicio sin tope con alerta); (2) stack de observabilidad (sucesor parcial del [ADR de trazabilidad](../adr/20260901-sistema-unificado-de-trazabilidad-y-observabilidad-distribuida.md): Prometheus, Tempo, Loki y Grafana, catálogo de métricas, alertas en el panel, protección de endpoints y recolección de logs en el cluster); (3) rate limiting en el gateway (Bucket4j con Redis, *fail-open*, identificación y límites, `429`); (4) GraphQL en Donaciones (solo lectura, adaptador sobre los servicios de aplicación, límites, ruta, librería). Sin ADR propio: la seguridad perimetral (sigue el [ADR del pipeline de CI](../adr/20260426-pipeline-unificado-de-cicd-y-automatizacion-de-calidad.md)) y los diagramas. *Q16b B* (el grupo no siguió la recomendación del agente, que era A): los cuatro ADR se escriben **primero y en un PR aparte**, para validar la decisión antes de implementarla. Riesgo asumido: ese PR frena el arranque del camino crítico hasta que se revise. `[OBSERVED]` el ADR 2 tiene que resolver el choque con el [ADR de logs sin volúmenes del host](../adr/20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md), pensado para Docker: en Kubernetes el recolector lee los logs de los pods desde el nodo. Solo una persona del grupo puede pasar un ADR a `accepted` (AGENTS.md §9.3). **Escritos (A0, 2026-11-17):** Kubernetes y observabilidad, reescritos el 2026-11-18 por la revisión de simplificación (E7-S05) como [despliegue en Docker Swarm](../adr/20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md) y [observabilidad con OpenTelemetry y `otel-lgtm`](../adr/20261118-observabilidad-con-opentelemetry-y-grafana-otel-lgtm.md), [rate limiting](../adr/20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md) (movido desde el PR 986) y [GraphQL](../adr/donaciones-service/20261117-graphql-de-solo-lectura-en-donaciones.md), en el PR de la rama `E7_adrs`.

**E7-Q17 — ¿Qué consultamos a la cátedra? ✅ (2026-11-16): A, tres consultas 🎓**
> **Revisada el 2026-11-18 (§5, E7-S01 y E7-S05e):** la consulta 1 (Kubernetes) ya no aplica; Swarm es parte de Docker, que el enunciado nombra. Se mandan las otras dos con el texto ajustado de E7-S05e, que reemplaza al borrador de abajo.
> **Respuesta del grupo:** enviar tres consultas a la cátedra. (1) ¿Se acepta **Kubernetes (kind)** como entorno de despliegue y de defensa? `[OBSERVED]` el enunciado (pág. 23) pide "Docker o similar" y no menciona Compose ni Kubernetes. (2) ¿"Seguridad" incluye algo más que rate limiting? El grupo suma encabezados, protección de observabilidad, cadena JWT y escaneo. (3) ¿GraphQL de solo lectura, sin tiempo real, cumple la opción 2 sin justificar casos de uso adicionales? La cuarta (infraestructura externa en el diagrama de despliegue) la decide el grupo: con Q14b C, el diagrama muestra solo Kubernetes. **Pendiente:** enviar las consultas (texto redactado abajo), preferentemente hoy, para tener la respuesta antes de la defensa del 4 de diciembre, y anotar acá las respuestas.

**Texto de las consultas (borrador del 2026-11-16, para copiar y enviar por el canal de la cátedra)**

> **Asunto:** Grupo 5 (DonaTrack): consultas sobre la Entrega 7 (Observabilidad y Seguridad)
>
> Hola, somos el Grupo 5. Estamos organizando la Entrega 7 y nos quedaron tres dudas de alcance. Si pueden respondernos antes de la defensa (4/12), evitamos rehacer trabajo. Alcanza con una respuesta corta a cada una.
>
> **1. Entorno de despliegue (Kubernetes en lugar de Docker Compose).** El enunciado pide "generar contenedores (utilizando Docker o similar) y desplegar allí cada uno de los servicios". Para E7 queremos desplegar todo en un cluster local de Kubernetes (con kind), incluyendo el stack de observabilidad y el autorestart de los servicios. Mantendríamos también la versión con Docker Compose. *Consulta:* ¿es aceptable Kubernetes como entorno de despliegue y de defensa, o prefieren que la demo corra con Docker Compose?
>
> **2. Alcance de "Seguridad".** Para el control de tráfico implementamos rate limiting en el API Gateway (con respuesta `429`, `Retry-After` y métricas). Además planeamos: encabezados de seguridad HTTP en el gateway, protección de los endpoints de observabilidad, validación de la cadena de JWT activada en el entorno de preprod y escaneo de dependencias e imágenes en el CI. *Consulta:* ¿"Seguridad" incluye algo más que el rate limiting, o con esto cubrimos lo esperado?
>
> **3. GraphQL de solo lectura.** Elegimos la opción 2 (GraphQL) en el servicio de Donaciones. Implementaríamos consultas (*queries*) con filtros dinámicos y selección de campos sobre donaciones, necesidades y entidades públicas, con paginación obligatoria y límites de profundidad y complejidad. No incluiríamos mutaciones ni suscripciones en tiempo real: las escrituras siguen por REST. *Consulta:* ¿un servicio GraphQL de solo consulta cumple la opción 2, o se espera algo más, como mutaciones o tiempo real?
>
> Muchas gracias. Grupo 5 — DonaTrack

---

## 4. Resumen de las respuestas recomendadas `[PROPOSED]`

| Pregunta | Recomendación | Estado |
|---|---|---|
| Q01 gRPC o GraphQL | GraphQL en Donaciones, solo lectura | ✅ GraphQL en Donaciones; Logística abierta |
| Q02 Key Broker | Diferido y reprogramado | ✅ A, diferido (2026-11-18, §5, E7-S04d) |
| Q03 Otra seguridad | Proteger observabilidad y encabezados en el gateway | ✅ las cuatro medidas |
| Q04 Stack | Grafana LGTM liviano (Prometheus, Tempo, Loki) | ✅ opción A |
| Q05 Dónde corre | Compose propio de observabilidad | ✅ A, reinterpretada como namespace del cluster |
| Q06 Qué se mide | Señales de oro, métricas de negocio mínimas y alertas en el panel | ✅ con 16 métricas de negocio comprometidas (13 del catálogo y 3 obligatorias) y 7 opcionales; §3.1 podado |
| Q07 Mecanismo de restart | C1 (Kubernetes híbrido); el grupo eligió **C2, todo en Kubernetes** | ✅ C2; Q07b kind, Q07c A, Q07d Kustomize |
| Q08 Alcance y política | Servicios e infraestructura; reinicio sin tope con alerta; liveness y readiness separados | ✅ las tres recomendaciones |
| Q09 Demostración | Script de caos reproducible | ✅ opción A |
| Q10 Dónde se limita | En el gateway | ✅ opción A |
| Q11 Librería | Bucket4j en memoria | ✅ B, con Redis (distinto de la recomendación); Q11b fail-open y Q11c 2 réplicas ✅ |
| Q12 Clave y límites | Por usuario y por IP; seguimiento aparte | ✅ tal cual |
| Q13 Respuesta | `429` con `Retry-After`, métrica y alerta | ✅ opción A |
| Q18 Qué se consulta | Donaciones, necesidades y entidades públicas (B) | ✅ B, con una pieza de C como extensión opcional si sobra tiempo |
| Q19 Mutaciones | Solo lectura | ✅ opción A |
| Q20 Autorización | Adaptador sobre los servicios de aplicación | ✅ opción A |
| Q21 Límites de consulta | Paginación, profundidad, complejidad, timeout, introspección apagada | ✅ todos |
| Q22 Exposición y consumo | Ruta propia detrás del gateway; API con GraphiQL, tests y k6 | ✅ exposición recomendada; consumo B (API más una pantalla del cliente) |
| Q23 Librería | Spring for GraphQL | ✅ opción A |
| Q14 Diagramas | PlantUML, componentes y despliegue separados | ✅ PlantUML; despliegue solo Kubernetes; borrador temprano muy cuidado y versión final |
| Q15 Partición | Un PR por frente (ocho) | ⏳ propuesta de la sesión de planificación, versión 2 (2026-11-17): reparto por tarea con dedicación por persona, sprint de cluster, parciales, punto de control del 22/11 y workflow de agentes; CPM vivo en un artifact; Redis dentro del PR 986; falta el ajuste del resto del grupo |
| Q16 ADRs | Cuatro, junto con cada PR | ✅ los cuatro; escritos primero en un PR aparte (distinto de la recomendación) |
| Q17 Cátedra | Tres consultas (Kubernetes, alcance de seguridad, GraphQL de solo lectura) | ✅ opción A; falta enviarlas |

---

## 5. Revisión de simplificación (2026-11-18)

**Por qué.** `[INFERRED]` La línea base de Q15 (versión 2) llegaba al congelamiento del 2 de diciembre en el 13 % de las simulaciones. El grupo revisa las decisiones para simplificar la arquitectura, empezando por la que más arrastra. Las preguntas se presentaron en texto, con contexto, matriz de alternativas y recomendación.

**Cómo.** Cinco rondas: S1 despliegue, S2 rate limiting, S3 observabilidad, S4 seguridad y GraphQL, S5 proceso (ADR, diagramas, workflow W1, reparto y consultas a la cátedra). Lo que nunca se recorta, porque lo pide el enunciado: métricas, trazas y logs visibles; autorestart; rate limiting con `429`; GraphQL con filtros y selección de campos; diagramas de componentes y despliegue.

**Mapa de palancas** `[INFERRED]` (días-persona con la estimación probable de la tabla de Q15; demanda total del plan, unos 38):

| # | Decisión revisada | ¿La pide el enunciado? | Qué saca | Días-persona que libera | Ronda |
|---|---|---|---|---|---|
| 1 | Todo en Kubernetes (Q07 C2) | No: pide "Docker o similar" y autorestart | kind, Kustomize, Ingress, manifiestos, migración del CI | ≈ 6, más 1,5 condicional | S1 ✅ |
| 2 | Redis, réplicas y *fail-open* (Q11, Q11b, Q11c) | No | Depende de S1 | ≈ 0,3 | S1 ✅ (se mantiene) |
| 3 | Seis clases de límite (Q12) | No | Dos clases | ≈ 0,75 | S2 ✅ |
| 4 | Cadena JWT con Keycloak real (tareas S3 y S4 de la tabla de Q15) | No (Q03) | Keycloak real en el preprod | ≈ 3 | S4 ✅ |
| 5 | Pantalla del cliente que consume GraphQL (Q22 B) | No | P1 a P3 de Q15 | ≈ 3 | S4 ✅ |

### Ronda S1 — Despliegue

**E7-S01 — ¿Dónde corre todo? ✅ (2026-11-18): A, volver a Docker (Kubernetes sale de E7)**
> **Respuesta del grupo:** opción A. Las opciones fueron A (volver a Docker), B (Kubernetes recortado, con el punto de control del 22/11) y C (seguir como está). Matriz: plazo y riesgo 35 %, lo que corrige la cátedra 25 %, cantidad de piezas 20 %, aprendizaje 20 %; totales A 3,95, B 3,45, C 2,55. `[INFERRED]` Si el aprendizaje pesaba 40 % o más, ganaba B. Se pierden el esqueleto del cluster (K1, PR 989), el ADR de Kubernetes del PR 987 (`proposed`, sin mergear) y el despliegue del borrador de diagramas (PR 988).

**E7-S01b — Redis y réplicas del gateway ✅ (2026-11-18): se mantienen, como consecuencia de E7-S01c**
> Redis, el *fail-open* (R1b, PR 986) y las dos réplicas del gateway siguen como en Q11, Q11b y Q11c. Con réplicas, el contador compartido en Redis se justifica.

**E7-S01c — ¿Docker Swarm para escalar en horizontal? ✅ (2026-11-18): C, Swarm de un nodo replicando los seis módulos propios**
> **Respuesta del grupo:** opción C. Las opciones fueron A (Swarm con el gateway ×2), B (Compose con un contenedor autoheal y una réplica en memoria) y C (Swarm replicando todo). Matriz: plazo 35 %, cátedra 25 %, piezas 20 %, aprendizaje y escalabilidad 20 %; totales A 3,85, B 3,75, C 3,15. El grupo eligió C, distinta de la recomendación (A). Se replican el gateway, el cliente liviano y los cuatro servicios. Postgres, Mongo, RabbitMQ, Keycloak, MinIO y Redis quedan con una réplica porque guardan estado.
> `[OBSERVED]` El cliente liviano declara `spring-session-jdbc`, pero la sesión en la base está apagada por defecto (`CLIENTE_SESIONES_JDBC_HABILITADO`, `false` si no se define): con dos réplicas hay que encenderla. Donaciones, Incentivos y Logística tienen 11 tareas `@Scheduled` (5, 4 y 2; una de Logística mueve la posición simulada de los camiones y está encendida por defecto) y ningún `pom.xml` declara ShedLock. Hay 8 clases con `@RabbitListener` (1 en Donaciones, 2 en Incentivos, 1 en Logística y 4 en Notificaciones). `[INFERRED]` Por eso el cliente se puede replicar sin cambios de código (solo con esa variable) y los servicios con tareas programadas no.
> `[INFERRED]` A verificar en la prueba de Swarm: que funcione en Docker Desktop para Windows; que el *routing mesh* reparta entre réplicas; que `docker stack deploy` no construye imágenes, ignora `depends_on` y quizá no lee el `.env`; y que el *routing mesh* oculta la IP real del cliente, lo que obliga a un proxy delante que agregue `X-Forwarded-For` (detalle en la Ronda S2).
> **Resultado de la prueba de Swarm (2026-11-18)** `[VERIFIED]`: Swarm sirve y no se pasa a la opción B. En Docker Desktop para Windows, reinicia las tareas *unhealthy* sin *autoheal* y el *routing mesh* reparte 10 y 10 entre dos réplicas del gateway. `docker stack` no lee el `.env` y además rechaza el compose: la forma larga de `depends_on` no se ignora, se rechaza, y también `published` como texto, `start_interval` y `host_ip`. Hace falta un paso que arme el archivo del stack. Sin `depends_on`, el stack converge con reintentos, salvo mongodb, que necesita `hostname: mongodb`. Detrás del mesh, el gateway ve `10.0.0.2` para todos, y un nginx en modo host ve `172.19.0.1` para todos (ver la revisión de E7-S02b). Resumen y límites de la prueba en el [ADR de despliegue en Docker Swarm](../adr/20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md).

**E7-S01d — Lo que pide replicar los servicios ✅ (2026-11-18): A, A, A**
> **Respuesta del grupo:** A en las tres. Cada opción elegida, tal como se presentó:
> - **S01d-1, tareas programadas: A, ShedLock con JDBC.** Una tabla por servicio, creada con una migración de Flyway, y `@SchedulerLock` en cada método. Retoma el [ADR de ShedLock en Incentivos](../adr/incentivos-service/20260901-coordinacion-distribuida-de-schedulers-con-shedlock.md) (`proposed`, sin implementar) y lo extiende a los tres servicios. `[PROPOSED]` Suma dos dependencias y la elección se toma como su aprobación (AGENTS.md §6), a confirmar con el grupo; si va en `common-lib`, es un cambio estructural (AGENTS.md §7.0). Descartadas: partir cada servicio en *api* y *worker*, y locks propios con `pg_advisory_lock`.
> - **S01d-2, orden de los eventos: A, *single active consumer* de RabbitMQ** en las colas donde importa el orden. Una réplica consume y la otra queda de reserva. Las colas existentes se vuelven a declarar. Descartadas: confiar solo en la máquina de estados y la idempotencia, e ignorarlo.
> - **S01d-3, memoria: A, réplicas por variable:** 1 en desarrollo y 2 en la defensa y la prueba final, con límites de memoria por servicio. Descartada: siempre dos réplicas, que solo se podrían levantar con 16 GB.

**Consecuencias de la Ronda S1** `[PROPOSED]`. Se bajan a tareas y ADR en la Ronda S5.

| Qué | Cambio |
|---|---|
| ADR | El de Kubernetes (PR 987) se reescribe como despliegue en Docker Swarm de un nodo: autorestart, réplicas, ShedLock y *single active consumer* |
| PRs | El PR 989 (K1) se cierra; la rama queda. El despliegue del PR 988 se redibuja |
| Tareas de Q15 que salen | K1 (hecha), K2, K4, K5, K7 y la tarea S4 (cadena JWT en el cluster) |
| Tareas de Q15 que siguen | K3 (grupos de salud: el `healthcheck` de Swarm usa liveness), K6 (script de caos, con Docker) y L1 (recolección de logs; según E7-S03 y E7-S03b, la absorbe `otel-lgtm` con los logs por OTLP) |
| Tareas nuevas | Prueba de Swarm (≤ 0,5), override de Swarm sobre los compose (0,5), proxy con la IP real (0,5; sale con la revisión de E7-S02b), ShedLock en tres servicios (1 a 1,5), *single active consumer* (0,5), réplicas por variable |
| Calendario | El sprint de cluster y el punto de control del 22/11 se reemplazan por la prueba de Swarm (decidido en E7-S05d) |
| Balance | Unos 3,5 días-persona nuevos contra unos 6 liberados (más 1,5 condicional): saldo de 2,5 a 4 a favor |

### Ronda S2 — Rate limiting

`[OBSERVED]` Punto de partida, en la rama `E7_rate_limit` (PR 986):
- cuatro cupos por minuto, lecturas y escrituras por usuario (`sub`) o por IP: 300, 60, 120 y 30, configurables con `GATEWAY_LIMITE_*`;
- la IP se toma de `X-Forwarded-For` solo si el pedido llega de un proxy confiable (`GATEWAY_LIMITE_PROXIES_CONFIABLES`);
- el cliente liviano reenvía la IP del navegador en `X-Forwarded-For` (`ClientesHttp`);
- ya están Redis con *fail-open*, `429` con `Retry-After`, el error del catálogo y el `traceId`.

**E7-S02 — ¿Cuántas clases de límite? ✅ (2026-11-18): A, los cuatro cupos del PR 986**
> **Respuesta del grupo:** opción A. Las opciones fueron A (los cuatro cupos), B (cuatro más una clase para GraphQL) y C (las seis clases de Q12).
> - El seguimiento del camión (2 consultas por minuto) entra en el cupo de lectura: queda contado, como pide el ADR-E6-07.
> - Las subidas pesadas entran en escrituras.
> - GraphQL entra en lecturas; su costo lo frenan los límites de Q21 dentro del servicio.
> - Los valores iniciales son los del PR, no los de Q12.
> - `[INFERRED]` Libera unos 0,75 días-persona: lo que faltaba de R2.

**E7-S02b — ¿Cómo se conserva la IP real detrás de Swarm? ✅ (2026-11-18): B, un nginx delante como única entrada; revisada a A el mismo día (ver abajo)**
> **Respuesta del grupo:** opción B, distinta de la recomendación (A, aceptar la IP aproximada y documentarla). Las opciones fueron A (aceptarla y documentarla), B (un nginx delante como única entrada) y C (publicar cliente y gateway en modo host, que contradice E7-S01c). Matriz: plazo 35 %, cátedra 25 %, piezas 20 %, exactitud del límite por IP 20 %; totales A 3,9 y B 3,65.
> `[INFERRED]` El *routing mesh* de Swarm reescribe la IP de origen; con réplicas, el cupo por IP lo compartirían todos los visitantes.
> `[PROPOSED]` Cómo queda:
> - **El proxy:** un nginx con configuración fija, 1 réplica y publicado en modo host, como único punto de entrada; enruta al cliente y al gateway y agrega `X-Forwarded-For`.
> - **Sin socket de Docker:** por eso nginx y no Traefik.
> - **Aprobación de la imagen:** la elección se toma como aprobación de la imagen nueva (AGENTS.md §6), a confirmar con el grupo.
> - **A verificar al implementar:** que el cliente tome la IP del navegador del encabezado de nginx (por ejemplo, con `server.forward-headers-strategy`) antes de reenviarla al gateway.
> - **Costo:** unos 0,5 a 1 día-persona.
>
> **Revisada el 2026-11-18, después de la prueba de Swarm: pasa a A, sin nginx** (a confirmar con el grupo). `[VERIFIED]` En Docker Desktop para Windows, nginx en modo host tampoco ve la IP del visitante: ve `172.19.0.1` en todos los pedidos de la PC, y el gateway detrás del mesh ve `10.0.0.2`. Desde otra máquina, sin probar. `[INFERRED]` Con la evidencia, la matriz con los mismos pesos da A 3,70, B 2,85 y C' 2,75 (C' es defender en un Linux con Docker nativo, donde el modo host sí vería la IP). Cómo queda:
> - el cliente liviano se publica en el *routing mesh*, sin proxy delante;
> - todos los visitantes **sin cuenta** comparten la cubeta por IP (120 lecturas y 30 escrituras por minuto en total). Las cuentas con token validado tienen su propia cubeta;
> - Grafana se publica con su propio usuario y clave;
> - `[PROPOSED]` el gateway no se publica en el stack de Swarm, o `GATEWAY_LIMITE_PROXIES_CONFIABLES` se limita a la red de la aplicación, porque la regex por defecto confía en `10.*`, que incluye al mesh;
> - libera los 0,5 a 1 día-persona del proxy y la imagen de nginx.
>
> Detalle en el [ADR de despliegue en Docker Swarm](../adr/20261118-despliegue-en-docker-swarm-de-un-nodo-con-replicas.md) y en el [ADR de rate limiting](../adr/20261115-rate-limiting-en-el-api-gateway-por-cuenta-o-por-ip.md).

**E7-S02c — ¿Cuánto k6? ✅ (2026-11-18): A, un escenario que demuestra**
> **Respuesta del grupo:** opción A. Las opciones fueron A (un escenario que demuestra), B (calibración completa por clase) y C (sin k6, solo tests de integración). El escenario muestra tres cosas: el tráfico normal pasa, una ráfaga recibe `429`, y el cupo se cuenta entre las dos réplicas del gateway (Redis). Los valores quedan como están, sin calibrar. `[INFERRED]` La tarea R4 de Q15 baja de unos 1 a unos 0,5 días-persona.

### Ronda S3 — Observabilidad

El enunciado pide las tres señales (métricas, trazas y logs): lo que se simplifica es cuántas piezas y cuánto código propio.

**E7-S03 — ¿Cuántas piezas para el stack? ✅ (2026-11-18): A, la imagen todo en uno `grafana/otel-lgtm`**
> **Respuesta del grupo:** opción A. Las opciones fueron A (la imagen todo en uno) y B (cuatro contenedores separados, como en Q04). Matriz: plazo 35 %, cátedra 25 %, piezas 20 %, realismo 20 %; totales A 4,15 y B 3,1.
> `[DOCUMENTED]` Según la [documentación de Grafana](https://grafana.com/docs/opentelemetry/docker-lgtm/), la imagen trae el colector OpenTelemetry, Prometheus, Tempo, Loki y Grafana, y recibe OTLP en los puertos 4317 y 4318. Admite configuración propia para Prometheus, Loki y Tempo. Grafana la publica para desarrollo, demos y pruebas, no para producción.
> `[PROPOSED]` El ADR lo justifica como entorno académico de demostración, y el diagrama la dibuja como un nodo con sus componentes adentro. La elección se toma como aprobación de la imagen (AGENTS.md §6), a confirmar con el grupo. Pasa de unos 3,5 a unos 1 día-persona (tareas O1, O2 y L1 de Q15).

**E7-S03b — ¿Cómo salen las señales de los servicios? ✅ (2026-11-18): A, todo por OTLP con `spring-boot-starter-opentelemetry`**
> **Respuesta del grupo:** opción A. Las opciones fueron:
> - A: todo por OTLP;
> - B: mixto clásico, con *scrape* de `/actuator/prometheus`, Brave en formato Zipkin y Alloy leyendo los logs de Docker con el socket montado;
> - C: mixto sin socket, con un appender de Logback hacia Loki.
>
> Matriz: plazo 35 %, riesgo técnico 25 %, piezas 20 %, seguridad 20 %; totales A 4,15, C 3,45 y B 2,65.
> `[DOCUMENTED]` Según el [blog de Spring](https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot/), el starter de Spring Boot 4 trae el puente OTel para trazas y el registro OTLP de Micrometer para métricas, y configura el envío de logs por OTLP.
> `[PROPOSED]` Consecuencias:
> - **Configuración en un solo lugar:** se configura en `common-lib`, donde reemplaza a Brave; es un cambio estructural (AGENTS.md §7.0).
> - **Sin recolector de logs ni socket de Docker:** desaparece el choque con el ADR de logs sin volúmenes del host.
> - **Endpoint protegido sin trabajo extra:** `/actuator/prometheus` no se expone, y la medida de Q03 de proteger la observabilidad se reduce a Grafana con usuario y clave detrás del nginx de E7-S02b. Con la revisión de E7-S02b no hay nginx: Grafana se publica con su propio usuario y clave.
> - **Logs:** el NDJSON a la salida estándar sigue igual.
> - **RabbitMQ:** se lee con un *scrape* de su plugin, configurado en el Prometheus de la imagen.
>
> `[INFERRED]` Riesgo a verificar primero: según la versión de Boot 4.0.x, los logs por OTLP pueden necesitar el appender de Logback, y las fuentes no coinciden. Si fallan, solo los logs caen a la opción C.

**E7-S03c — ¿Cuántas métricas propias? ✅ (2026-11-18): A, las automáticas y seis propias**
> **Respuesta del grupo:** opción A. Las opciones fueron A (automáticas más seis propias), B (las 16 comprometidas en Q06a) y C (solo automáticas más M24).
> - **Automáticas:** señales de oro (`http.server.requests`), JVM, el plugin de RabbitMQ (M18, M21 y parte de M19) y GraphQL (M08; `[INFERRED]` Spring for GraphQL las emite solo).
> - **Reinicios (M26):** se derivan del cambio de `process_start_time` de cada servicio propio; los de la infraestructura no se cuentan.
> - **Propias:** M01, M04, M13, M16, M24 y M25.
> - **Salen del compromiso:** M02 (y con ella la consulta de conteo de Q06b), M03, M05, M09 y M11.
> - **Alertas:** las cuatro del panel siguen.
> - **Costo:** `[INFERRED]` pasa de unos 3 a unos 1,5 días-persona (tareas M2 a M4 de Q15).

### Ronda S4 — Seguridad extra, GraphQL y Key Broker

Siguen sin preguntarse: los encabezados de seguridad en el gateway (tarea S1 de Q15), el escaneo con Trivy en el CI (tarea S2) y los límites de consulta de Q21.

**E7-S04 — ¿Cómo se valida la cadena JWT con Keycloak real? ✅ (2026-11-18): A, encendida por defecto en el stack de Swarm, con una prueba de humo**
> **Respuesta del grupo:** opción A. Las opciones fueron:
> - A: encendida por defecto, con una prueba de humo;
> - B: el preprod completo en verde con `run-preprod-tests.sh --auth`;
> - C: sacarla del alcance.
>
> `[OBSERVED]` El preprod ya tiene Keycloak, los servicios apuntan a su JWKS y `PREPROD_SEGURIDAD=true` prende la validación.
> `[PROPOSED]` La prueba de humo cubre una ruta por servicio: sin token responde `401`, con token `200` y con un rol sin permiso `403`. Se corre en la defensa. La tarea S3 de Q15 pasa de unos 2 a unos 1 día-persona.

**E7-S04b — ¿Quién consume GraphQL? ✅ (2026-11-18): C, una pantalla existente cambia su adaptador**
> **Respuesta del grupo:** opción C. Las opciones fueron:
> - A: sin pantalla (API, GraphiQL en preprod, tests y k6);
> - B: pantalla nueva, como en Q22;
> - C: cambiar el adaptador de una pantalla existente.
>
> Matriz: plazo 35 %, cátedra 25 %, piezas 20 %, valor de demo 20 %; totales C 4,0, A 3,9 y B 3,1.
> `[PROPOSED]` Por ejemplo, la lista pública de necesidades pide por GraphQL solo los campos que muestra, sin diseño de pantalla nueva. La pantalla se elige al implementar. Las tareas P1 a P3 de Q15 pasan de unos 3 a unos 1 a 1,5 días-persona.

**E7-S04c — ¿Qué se consulta por GraphQL? ✅ (2026-11-18): A, donaciones, necesidades y entidades, sin el donante**
> **Respuesta del grupo:** opción A. Las opciones fueron:
> - A: sin el donante;
> - B: como en Q18, con "quién donó";
> - C: solo donaciones.
>
> El esquema no incluye el tipo Donante, así que no hacen falta los tests de privacidad del nombre visible. No hay extensión opcional, y GraphQL en Logística queda fuera de E7. `[INFERRED]` Libera unos 0,5 a 1 día-persona de la tarea G2.

**E7-S04d — ¿El Key Broker entra en E7? (Q02) ✅ (2026-11-18): A, queda diferido**
> **Respuesta del grupo:** opción A. Las opciones fueron:
> - A: diferido;
> - B: solo el ADR y el contrato;
> - C: construirlo en E7.
>
> DTI-07 y DTI-01 se reprogramaron en [`DEUDA_TECNICA.md`](../adr/DEUDA_TECNICA.md) para después de E7, sin fecha. Con esto se cierra Q02.

### Ronda S5 — ADR, diagramas, workflow, reparto y cátedra

**E7-S05 — ¿Qué se hace con los ADR? ✅ (2026-11-18): A, reescribir los cuatro dentro del PR 987, antes del merge**
> **Respuesta del grupo:** opción A. Las opciones fueron A (reescribirlos en el PR 987), B (dejar el de Kubernetes y escribir un sucesor) y C (un ADR por cada decisión nueva).
> `[PROPOSED]` Qué cambia en cada uno:
> - **Kubernetes:** pasa a "Despliegue en Docker Swarm de un nodo con réplicas", que incluye ShedLock, el *single active consumer* y nginx (nginx salió con la revisión de E7-S02b).
> - **Observabilidad:** se actualiza a `otel-lgtm` y OTLP.
> - **Rate limiting:** se actualiza a los cuatro cupos detrás de nginx (con la revisión de E7-S02b, detrás del *routing mesh*).
> - **GraphQL:** se actualiza al alcance sin el donante y al consumo desde una pantalla existente.
> - **ShedLock en Incentivos:** al ADR existente se le propone la relación "superado por" el de Swarm. La aprobación es humana (AGENTS.md §9.3).
>
> Los ADR del PR 987 no están mergeados, así que todavía no son registros históricos.

**E7-S05b — ¿Qué se hace con los diagramas? ✅ (2026-11-18): A, rehacer ahora el borrador en el PR 988**
> **Respuesta del grupo:** opción A. Las opciones fueron A (rehacerlo ahora) y B (solo la versión final, al cierre).
> `[PROPOSED]` Qué muestra cada diagrama:
> - **Despliegue:** un nodo de Swarm con nginx, las réplicas, `otel-lgtm`, Redis y la infraestructura.
> - **Componentes:** nginx en lugar del Ingress, y la imagen todo en uno en lugar de los cuatro contenedores.
> - Con la revisión de E7-S02b, sin nginx: el cliente liviano se publica en el *routing mesh*; si el gateway también se publica queda abierto (ver el ADR de despliegue en Docker Swarm).
>
> La versión final queda para el cierre. El PR 989 (esqueleto de Kubernetes) se cierra por E7-S01; la rama queda.

**E7-S05c — ¿Qué pasa con el workflow de agentes W1? ✅ (2026-11-18): B, rediseñarlo para las tareas nuevas**
> **Respuesta del grupo:** opción B, distinta de la recomendación (A, reducir W1 a S1, S2, R3 y G3). Las opciones fueron A (W1 reducido), B (rediseñar W1) y C (sin W1).
> `[PROPOSED]` Cómo queda:
> - **Tareas nuevas o cambiadas en las rondas S1 a S4:** ShedLock, el consumidor único, OTLP en `common-lib` y las métricas propias, entre otras.
> - **Tareas que no cambiaron:** S1, S2, R3 y G3.
> - **Qué se rehace:** el plan de W1 (`.claude/plan-e7-w1.md`), el script y la prueba en seco, más una ronda de auditoría del workflow.

**E7-S05d — ¿Cómo se rehace el reparto? ✅ (2026-11-18): A, versión 3 del plan con la propuesta, publicada en el artifact**
> **Respuesta del grupo:** opción A. Las opciones fueron A (versión 3 con la propuesta) y B (que cada uno reestime primero). `[PROPOSED]` Propuesta inicial, a ajustar con el grupo:
>
> | Persona | Tareas |
> |---|---|
> | Tadeo | Prueba de Swarm (media jornada, primera, en lugar del punto de control del 22/11), OTLP en `common-lib`, ADR y diagramas |
> | Ber | Override de Swarm, réplicas por variable, nginx (sale con la revisión de E7-S02b), el escenario de k6 (R4) y R3 |
> | Camila Lencina | ShedLock en tres servicios, *single active consumer*, K3 y K6 |
> | Camila Suarez | `otel-lgtm`, tableros y alertas |
> | Martín | Las seis métricas propias |
> | Sofía | Adaptador GraphQL de la pantalla existente |
> | Nicolás, Miranda y Anush | Sin cambio de dueño: GraphQL, cadena JWT con prueba de humo, encabezados y Trivy |
>
> El motor CPM es el mismo y recalcula la probabilidad.

**E7-S05e — ¿Qué consultas se mandan a la cátedra? ✅ (2026-11-18): A, la 2 y la 3, hoy**
> **Respuesta del grupo:** opción A. Las opciones fueron A (la 2 y la 3), B (sumar una sobre Swarm) y C (no mandar nada). La consulta 1 (Kubernetes) se saca: Swarm es Docker, que el enunciado nombra. El texto ajustado reemplaza al borrador de Q17:

> **Asunto:** Grupo 5 (DonaTrack): consultas sobre la Entrega 7 (Observabilidad y Seguridad)
>
> Hola, somos el Grupo 5. Estamos organizando la Entrega 7 y nos quedaron dos dudas de alcance. Si pueden respondernos antes de la defensa (4/12), evitamos rehacer trabajo. Alcanza con una respuesta corta a cada una.
>
> **1. Alcance de "Seguridad".** Para el control de tráfico implementamos rate limiting en el API Gateway (con respuesta `429`, `Retry-After` y métricas). Además planeamos:
> - encabezados de seguridad HTTP en el gateway;
> - acceso con usuario y clave al panel de observabilidad;
> - validación de los tokens JWT de Keycloak activada en el entorno de despliegue, con una prueba de acceso por rol;
> - escaneo de dependencias e imágenes en el CI.
>
> *Consulta:* ¿"Seguridad" incluye algo más que el rate limiting, o con esto cubrimos lo esperado?
>
> **2. GraphQL de solo lectura.** Elegimos la opción 2 (GraphQL) en el servicio de Donaciones. Implementaríamos consultas (*queries*) con filtros dinámicos y selección de campos sobre donaciones, necesidades y entidades, con paginación obligatoria y límites de profundidad y complejidad. Una pantalla del sistema las consume. No incluiríamos mutaciones ni suscripciones en tiempo real: las escrituras siguen por REST. *Consulta:* ¿un servicio GraphQL de solo consulta cumple la opción 2, o se espera algo más, como mutaciones o tiempo real?
>
> Muchas gracias. Grupo 5 — DonaTrack

### Cierre de la revisión

**Balance** `[INFERRED]`. Ahorro neto de unos 9 días-persona: de los ~38 del plan quedan unos 29. La revisión libera unos 15: Kubernetes, el stack de observabilidad, las métricas, R2, R4, la tarea S3, la pantalla y el donante en GraphQL. A cambio suma unos 6 de trabajo nuevo: Swarm, ShedLock, el consumidor único, nginx, OTLP en `common-lib` y W1. Es una estimación gruesa; la cifra real sale de la versión 3 del plan (E7-S05d). Con la revisión de E7-S02b, nginx sale y libera otros 0,5 a 1.

**Qué sigue** `[PROPOSED]`, en orden:
1. Prueba de Swarm. Si falla, se vuelve a E7-S01c B (Compose con autoheal). ✅ Hecha el 2026-11-18: Swarm sirve (ver el resultado en E7-S01c y la revisión de E7-S02b).
2. ADR reescritos (PR 987) y borrador de diagramas (PR 988).
3. Versión 3 del plan.
4. Rediseño de W1.
5. Envío de las dos consultas.
6. Cierre del PR 989.
