# Índice de Deuda Técnica — DonaTrack

> Índice canónico de decisiones arquitectónicas diferidas. La descripción completa de cada ítem vive en el ADR vinculado, no en este archivo.
>
> Para el modelo de estados, ver [`docs/adr/README.md`](./README.md) — sección "ADR status ≠ implementation status".

---

## DTI-01 — Anonimización automática y surrogate keys para JPA

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-01](./donaciones-service/20260901-dti-01-automatizacion-de-anonimizacion-y-surrogate-keys-para-jpa.md) |
| Decision status | `proposed` |
| Implementation status | `[INFERRED] deferred` — requiere integración de capa de persistencia |
| Target | donaciones-service · Entrega de persistencia (prioridad alta) |
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** para surrogate keys en JPA; **Entrega 6 (Semana del 23 de Noviembre 2026)** para crypto-shredding definitivo con `auth-service` |

---

## DTI-02 — Reubicación de ProcesadorDeDonaciones a capa de aplicación

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-02](./donaciones-service/20260901-dti-02-reubicacion-de-procesador-de-donaciones-a-capa-de-aplicacion.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `ProcesadorDeDonaciones` permanece en paquete `infrastructure/` |
| Target | donaciones-service (prioridad media) |
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** — estabilización de servicios de aplicación durante persistencia |

---

## DTI-03 — Desacoplamiento de SegmentacionEventListener

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-03](./donaciones-service/20260901-dti-03-desacoplamiento-de-segmentacion-event-listener-en-servicio-de-aplicacion.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `SegmentacionEventListener` permanece en paquete `infrastructure/events/` |
| Target | donaciones-service (prioridad media) |
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** — desacoplamiento de listeners locales previo a la integración |

---

## DTI-04 — Descomposición de cambiarEstado() en DonacionesIndependientesService

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-04](./donaciones-service/20260901-dti-04-descomposicion-de-cambiarestado-en-donaciones-independientes-service.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — método monolítico `cambiarEstado()` activo en `DonacionesIndependientesService` |
| Target | donaciones-service (prioridad media) |
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** — alineación de transacciones cortas con State Pattern |

---

## DTI-05 — Segregación de responsabilidades en AlgoritmosService

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-05](./donaciones-service/20260901-dti-05-segregacion-de-responsabilidades-en-algoritmos-service.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] in-progress` — responsabilidades divididas entre `GestorPropuestasDeAsignacion` (dominio) y `PropuestaDeAsignacionService` (aplicación); `AlgoritmosService` no introducido |
| Target | donaciones-service (prioridad baja/media) |
| Cuándo se saldará | **Entrega 5 (Semana del 19 de Octubre 2026)** — refactor previo a la integración con la interfaz Web MVC |

---

## DTI-06 — Desacoplamiento de referencias directas entre aggregates por UUID

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-06](./donaciones-service/20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md) |
| ADR complementario | [20260901-evaluacion-de-interfaz-asignable](./donaciones-service/20260901-evaluacion-de-interfaz-asignable-vs-identificador-entidad-beneficiaria.md) |
| Decision status | `proposed` |
| Implementation status | `[INFERRED] deferred` — crítico para mapeo relacional |
| Target | donaciones-service · Entrega de persistencia (prioridad alta) |
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** — obligatorio para mapeo independiente de entidades JPA |

---

## DTI-07 — Dependencia diferida de auth-service para Key Broker y solución interina de Crypto-Shredding

| Campo | Valor |
|---|---|
| ADR | [20260902-dti-07](./notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md) |
| ADR complementario | [20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes](./notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md) |
| Decision status | `proposed` |
| Implementation status | `[INFERRED] deferred` — requiere implementación del microservicio auth-service |
| Target | notificaciones-service · auth-service (prioridad alta) |
| Cuándo se saldará | **Entrega 6: Despliegue, Observabilidad y Seguridad (Semana del 23 de Noviembre 2026)** — formalmente diferido al hito de Seguridad de la cátedra; se cancelará en simultáneo con la construcción de `auth-service`, la emisión de `ClaveUsuarioDestruidaEvent` en RabbitMQ y la adopción de `RemoteAuthKeyBrokerClient` |

---

## DTI-08 — Campos de observabilidad diferidos (spanId, executionTimeMs, errorCode estructurado, AMQP_DISPATCH/EXCEPTION, filtro BOOTSTRAP)

| Campo | Valor |
|---|---|
| ADR | [20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc](./20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md) |
| Decision status | `proposed` |
| Implementation status | `in-progress` — implementado: `eventType` (`HTTP_IN`, `SERVICE_SUCCESS`, `SERVICE_ERROR`), `httpMethod`, `endpoint`. Pendiente: `spanId`, `executionTimeMs`, `errorCode` como campo MDC/JSON estructurado (hoy solo existe embebido en el mensaje de texto de `GlobalExceptionHandler`), `eventType` `AMQP_DISPATCH`/`EXCEPTION`, filtro anti-fatiga `BOOTSTRAP` |
| Target | `common-lib` (`ControllerLoggingInterceptor`, `ServiceLoggingAspect`, `GlobalExceptionHandler`) · `scripts/analyze_preprod_logs.py` |
| Cuándo se saldará | Sin fecha asignada — pendiente de priorización |

---

## DTI-09 — Seguridad, control de acceso y asincronía en procesos batch de incentivos
<a id="dti-09-seguridad-control-de-acceso-y-asincronia-en-procesos-batch-de-incentivos"></a>

| Campo | Valor |
|---|---|
| ADR | [20260905-dti-09](./incentivos-service/20260905-dti-09-seguridad-y-asincronia-en-procesos-batch-de-incentivos.md) |
| Decision status | `proposed` |
| Implementation status | `[INFERRED] deferred` — endpoints creados para testing; requiere auth-service y Spring Security |
| Target | `incentivos-service` (`ProcesosIncentivosController`, `InactividadService`) |
| Cuándo se saldará | **Entrega 6: Despliegue, Observabilidad y Seguridad (Semana del 23 de Noviembre 2026)** — integración con `auth-service`, protección perimetral con roles (`ROLE_ADMIN`), traslado a `/api/admin/` y respuesta `202 Accepted` asíncrona |

---

## DTI-10 — Desacoplamiento de errores de dominio de incentivos en GlobalExceptionHandler

| Campo | Valor |
|---|---|
| ADR | [20260905-dti-10](./incentivos-service/20260905-dti-10-desacoplamiento-de-errores-de-dominio-en-global-exception-handler.md) |
| Decision status | `proposed` |
| Implementation status | `[INFERRED] deferred` — preserva consistencia con patrón preexistente en Entrega 2 |
| Target | `common-lib` (`GlobalExceptionHandler`, `ErrorCatalog`) · `incentivos-service` (`RankingController`) |
| Cuándo se saldará | **Entrega 5: Arquitectura Web MVC (Semana del 19 de Octubre 2026)** — refactor de capa Web y reemplazo del `if/else` por resolución idiomática de `Optional` o jerarquía tipada |

---

## DTI-11 — Extracción de MisionMapper dedicado y purificación de MisionDTO

| Campo | Valor |
|---|---|
| ADR | [20260905-dti-11](./incentivos-service/20260905-dti-11-extraccion-de-mision-mapper-y-purificacion-de-mision-dto.md) |
| Decision status | `proposed` |
| Implementation status | `[VERIFIED] implemented (PR #856)` — `MisionMapper` creado en `services.mappers` y `MisionDTO` purificado como record anémico |
| Target | `incentivos-service` (`MisionDTO`, `MisionMapper`, `MisionesDonacionService`) |
| Cuándo se saldará | **Saldada en PR #856 (Septiembre 2026)** — se implementó el componente `grupo5.incentivos.services.mappers.MisionMapper` resolviendo la insignia del donante y desacoplando `MisionDTO` |

---

## DTI-12 — Modernización del arnés de testing y erradicación de antipatrones de QA (AP-01, AP-02, AP-03)

| Campo | Valor |
|---|---|
| ADR | [20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes](./20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes.md) |
| ADR complementario | [20260906-estrategia-contratos-openapi-wiremock-y-esquemas-amqp](./20260906-estrategia-contratos-openapi-wiremock-y-esquemas-amqp.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] in-progress` — Fases 1, 2, 3A y 4 implementadas: ArchUnit universal y fitness functions activas, Pitest acotado a matching, persistencia efímera con `@ServiceConnection` y controllers en `@WebMvcTest`, validación viva de contratos OpenAPI en `ContractIT` erradicando AP-01, pruebas de rendimiento migradas a k6 erradicando AP-02 (`PerformanceStressIT` eliminado); Subfase 3B (AMQP) catalogada como `[DEFERRED_PENDING_RABBITMQ_CONTRACTS]` |
| Target | Monorepo · `integration-tests` · microservicios (`donaciones`, `logistica`, `incentivos`, `notificaciones`) |
| Cuándo se saldará | **Saldada en Entrega 4 (Fases 1, 2, 3A y 4)**; Subfase 3B (AMQP) diferida formalmente hasta la congelación de contratos RabbitMQ |

---

## DTI-13 — Migración de clientes consumidores de la API REST deprecada de notificaciones a ruta canónica y AMQP

| Campo | Valor |
|---|---|
| ADR | [20260910-dti-13](./20260910-dti-13-migracion-clientes-api-rest-deprecada-notificaciones-a-amqp.md) |
| ADR complementario | [20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) |
| ADR complementario | [20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones](./notificaciones-service/20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] in-progress` — Resolución técnica formalizada en ADR 20260911 y SPEC-03 en Entrega 4: adopción de Pub/Sub canónico AMQP, TopicExchanges de emisores, colas segregadas, clúster DLQ y erradicación total de OpenFeign (Hard Cutover); persistencia relacional física de outbox SQL diferida a Oleada 10 para servicios en memoria |
| Target | `donaciones-service` (`NotificacionesFeignClient`) · `incentivos-service` (`NotificacionesFeignClient`, `NotificacionesClientAdapter`) |
| Cuándo se saldará | **En ejecución en Entrega 4 (Septiembre 2026)** — desacoplamiento AMQP completo y eliminación de clientes Feign mediante ADR transversal [20260911](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) y [SPEC-03](../specs/active/SPEC-03-topologia-amqp-y-desacoplamiento-notificaciones.md) (al verificar la implementación en código de la Etapa 2) |

