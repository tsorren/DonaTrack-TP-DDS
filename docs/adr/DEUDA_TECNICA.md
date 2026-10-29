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
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** (vencida; reprogramación pendiente del equipo) para surrogate keys en JPA; **Entrega 7: Observabilidad y Seguridad** para crypto-shredding definitivo con el Key Broker (diferido desde E6 por la decisión D29 de [requerimientos-e6](../requerimientos/requerimientos-e6.md)) |

---

## DTI-02 — Reubicación de ProcesadorDeDonaciones a capa de aplicación

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-02](./donaciones-service/20260901-dti-02-reubicacion-de-procesador-de-donaciones-a-capa-de-aplicacion.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `ProcesadorDeDonaciones` permanece en paquete `infrastructure/` |
| Target | donaciones-service (prioridad media) |
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** (vencida; reprogramación pendiente del equipo) — estabilización de servicios de aplicación durante persistencia |

---

## DTI-03 — Desacoplamiento de SegmentacionEventListener

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-03](./donaciones-service/20260901-dti-03-desacoplamiento-de-segmentacion-event-listener-en-servicio-de-aplicacion.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `SegmentacionEventListener` permanece en paquete `infrastructure/events/` |
| Target | donaciones-service (prioridad media) |
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** (vencida; reprogramación pendiente del equipo) — desacoplamiento de listeners locales previo a la integración |

---

## DTI-04 — Descomposición de cambiarEstado() en DonacionesIndependientesService

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-04](./donaciones-service/20260901-dti-04-descomposicion-de-cambiarestado-en-donaciones-independientes-service.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — método monolítico `cambiarEstado()` activo en `DonacionesIndependientesService` |
| Target | donaciones-service (prioridad media) |
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** (vencida; reprogramación pendiente del equipo) — alineación de transacciones cortas con State Pattern |

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
| Cuándo se saldará | **Entrega 4 (Semana del 14 de Septiembre 2026)** (vencida; reprogramación pendiente del equipo) — obligatorio para mapeo independiente de entidades JPA |

---

## DTI-07 — Dependencia diferida de auth-service para Key Broker y solución interina de Crypto-Shredding

| Campo | Valor |
|---|---|
| ADR | [20260902-dti-07](./notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md) |
| ADR complementario | [20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes](./notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md) |
| Decision status | `proposed` |
| Implementation status | `[INFERRED] deferred` — requiere implementación del microservicio auth-service |
| Target | notificaciones-service · auth-service (prioridad alta) |
| Cuándo se saldará | **Entrega 7: Observabilidad y Seguridad** — diferido desde E6 por la decisión D29 de [requerimientos-e6](../requerimientos/requerimientos-e6.md): E6 construye el IdP y el gateway, y el Key Broker queda para el hito de Seguridad; se cancelará en simultáneo con la construcción de `auth-service`, la emisión de `ClaveUsuarioDestruidaEvent` en RabbitMQ y la adopción de `RemoteAuthKeyBrokerClient` |

---

## DTI-08 — Campos de observabilidad diferidos (spanId, executionTimeMs, errorCode estructurado, AMQP_DISPATCH/EXCEPTION, filtro BOOTSTRAP)
<a id="dti-08-campos-de-observabilidad-diferidos-spanid-executiontimems-errorcode-estructurado-amqp_dispatchexception-filtro-bootstrap"></a>

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
| Implementation status | **Resuelta.** `[VERIFIED] implemented (PR #856)` — `MisionMapper` creado en `services.mappers` (`incentivos-service/src/main/java/grupo5/incentivos/services/mappers/MisionMapper.java`) y `MisionDTO` purificado como record anémico |
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
| Implementation status | `[OBSERVED] in-progress` — Resolución técnica formalizada en ADR 20260911 y SPEC-03 en Entrega 4: adopción de Pub/Sub canónico AMQP, TopicExchanges de emisores, colas segregadas, clúster DLQ y erradicación total de OpenFeign (Hard Cutover); persistencia relacional física de outbox SQL diferida a Oleada 10 para servicios en memoria. El alias `POST /notificaciones` quedó en un método propio marcado `@Deprecated` (2026-10-14); sigue activo mientras `incentivos-service` lo consuma vía `NotificacionesFeignClient` |
| Target | `incentivos-service` (`NotificacionesFeignClient`, `NotificacionesClientAdapter`); `donaciones-service` ya publica por AMQP (`NotificacionesAsyncService`) y no tiene cliente Feign |
| Cuándo se saldará | **En ejecución en Entrega 4 (Septiembre 2026)** — desacoplamiento AMQP completo y eliminación de clientes Feign mediante ADR transversal [20260911](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) y [SPEC-03](../specs/active/SPEC-03-topologia-amqp-y-desacoplamiento-notificaciones.md) (al verificar la implementación en código de la Etapa 2) |

---

## DTI-14 — Limitaciones interinas del Broker de Integración con Logística

| Campo | Valor |
|---|---|
| ADR | [20261007-broker-de-integracion-con-logistica](./20261007-broker-de-integracion-con-logistica.md) (consecuencias negativas) |
| Spec | [SPEC-04](../specs/active/SPEC-04-broker-integracion-logistica.md) · registro detallado en la [bitácora del broker](../entrega-4/donaciones/bitacora-broker-logistica.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — el broker funciona completo, con estas limitaciones declaradas |
| Target | `donaciones-service` (broker de logística) |
| Cuándo se saldará | (1) y (2) con la persistencia JPA de `donaciones-service` (Oleada 10); (3) y (4) con el `auth-service` (**Entrega 6, semana del 23 de noviembre de 2026**); (5) y (6) al integrar un proveedor de logística real |

**Ítems:**

1. **Outbox del broker en memoria** (`LogisticaOutboxEnMemoria`): sin atomicidad con el estado ni durabilidad; los pedidos pendientes se pierden si `donaciones-service` se reinicia. El puerto `ILogisticaOutbox` y las entradas basadas en datos permiten pasar a `LogisticaOutboxJpa` con `SELECT … FOR UPDATE SKIP LOCKED` sin tocar el broker.
2. **«Una solicitud activa por donación» no es atómica** (`SolicitudesEntregaRepositoryEnMemoria`): dos pedidos simultáneos para la misma donación podrían crear dos solicitudes. Hoy no ocurre (cada donación se procesa una vez por aprobación); con JPA se resuelve con una restricción única.
3. **Protección por API key de transición** (`ApiKeyFilter`): claves por variable de entorno, sin rotación ni identidad de usuario. El `401` usa el código provisional `ERR-AUT-401`, que no está en `ErrorCatalog` (agregarlo toca `common-lib`).
4. **El proveedor preferido vive en memoria**: un cambio por `PUT /api/logistica/proveedor-preferido` se pierde al reiniciar y vuelve al valor configurado.
5. **Seguridad de RabbitMQ por proveedor**: con credenciales compartidas, un consumidor podría bindearse con comodín y recibir comandos ajenos. Se mitiga con bindings exactos y un test; en un entorno real corresponden usuarios y topic permissions por proveedor (recomendación del ADR, no implementada).
6. **Idempotencia de proveedores HTTP**: es requisito de contrato del proveedor (D33). ~~El stand-in `externo` de la demo (segunda instancia de `logistica-service`) no deduplica `POST /api/entregas`: ante un timeout puede quedar una entrega duplicada en esa instancia.~~ Desde #886, `POST /api/entregas` deduplica (409, `ERR-EST-816`): el stand-in ya no deja duplicados (D55). Sigue vigente el requisito para proveedores reales.
7. **La identidad del proveedor en los eventos de vuelta es un token en un header** (`VerificadorOrigenEventos`, D51; ADR [20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad](./20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md)): cada evento lleva `X-Proveedor-Id` y `X-Proveedor-Token`, Donaciones compara el token con `donatrack.logistica.proveedor.<id>.token-vuelta` y contrasta la donación con el registro de solicitudes; sin identidad válida el evento se descarta. Es el único camino de vuelta (el callback HTTP se quitó, D50). Límites: el token viaja en el mensaje (lo ve cualquier consumidor de `logistica.exchange`), no hay rotación ni firma del cuerpo ni protección contra repeticiones, y los tokens son por variable de entorno. Para un entorno real corresponde un usuario de RabbitMQ por proveedor con topic permissions de publicación (ítem 5, sin implementar).
8. **Un evento de vuelta que falla al aplicarse no se reintenta** (`ProcesadorEventosLogistica`): si `cambiarEstado` lanza, el procesador deja el error en el log y no marca el evento como consumido, pero el listener termina normal y RabbitMQ hace ack. Como el consumo no tiene requeue ni DLQ, la donación queda en su estado anterior hasta que alguien republique el evento a mano. Se cierra con una DLQ y reintentos acotados en las colas de vuelta, o propagando el error para que el contenedor rechace el mensaje. El comportamiento actual lo fija `ProcesadorEventosLogisticaTest.onEntregaExitosa_cuandoServicioFalla_noPropagaNiRegistraComoConsumido`.

---

## DTI-15 — Normalización y geocodificación de direcciones
<a id="dti-15"></a>

| Campo | Valor |
|---|---|
| ADR | [20261009-dti-14](./20261009-dti-15-normalizacion-y-geocodificacion-de-direcciones.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] not-started` — `Direccion` guarda `localidad`/`provincia`/`pais` como texto libre sin validar en `donaciones-service` y en `logistica-service`; `AlgoritmoOrdenadorSimple` ordena entregas por UUID por falta de coordenadas |
| Target | `donaciones-service` (validación contra API Georef al cargar direcciones) · `logistica-service` (geocodificación y ordenamiento geográfico de entregas) |
| Cuándo se saldará | **A definir** — posterior a la migración de `donaciones-service` a PostgreSQL |


---

## DTI-16 — Garantías de mensajería, trazabilidad AMQP y fitness functions no registradas (Entrega 4)

| Campo | Valor |
|---|---|
| ADR | [20260901-patron-transactional-outbox-para-consistencia-eventual](./20260901-patron-transactional-outbox-para-consistencia-eventual.md) (ítem 1) |
| ADR complementario | [20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) (`message_id` obligatorio, ítem 2; bindings por consumidor, ítem 10) · [20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos](./20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos.md) (ítems 3 y 4) |
| ADR complementario | [incentivos-service/20260901-coordinacion-distribuida-de-schedulers-con-shedlock](./incentivos-service/20260901-coordinacion-distribuida-de-schedulers-con-shedlock.md) (ítem 5) · [20260901-aislamiento-concurrente-y-gobernanza-de-pools-de-hilos-async](./20260901-aislamiento-concurrente-y-gobernanza-de-pools-de-hilos-async.md) (ítem 6) · [20260901-sistema-unificado-de-trazabilidad-y-observabilidad-distribuida](./20260901-sistema-unificado-de-trazabilidad-y-observabilidad-distribuida.md) (ítem 7) |
| ADR complementario | [20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2](./20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md) (ítem 8) · [20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest](./20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest.md) (ítem 9) · [logistica-service/20260901-planificacion-de-rutas-asincrona-por-lotes-y-callback-rest](./logistica-service/20260901-planificacion-de-rutas-asincrona-por-lotes-y-callback-rest.md) (ítem 11) |
| Decision status | `proposed` (los ADRs vinculados) |
| Implementation status | `[OBSERVED] in-progress` — desvíos confirmados por lectura estática en `baseline/e4-prs` (ENTREGA_4 con #889, más #887 y #892). Ítems 2 y 10 resueltos en la rama `E4_fix-hallazgos-auditoria` (2026-10-14); el resto sigue abierto. Declarados en [`arquitectura-sistema.md`](../entrega-4/arquitectura/arquitectura-sistema.md) §5.6, §8 y §9 |
| Target | `logistica-service` (1, 11, 12) · `incentivos-service` (2–6) · `notificaciones-service` (2, 10) · transversal (7–9) |
| Cuándo se saldará | Sin fecha acordada. Prioridad sugerida: 2, 1, 4 y 12 (pérdida o descarte silencioso de mensajes); después 11, 3, 5, 6, 8, 10, 7 y 9 |

**Ítems:**

1. **Logística publica después del commit, sin outbox ni publisher confirms** (`LogisticaEventPublisher`, `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` con `convertAndSend` directo; `publisher-confirm-type` solo está en `donaciones-service`). Si el proceso cae entre el commit y la publicación, o RabbitMQ no recibe el mensaje, el hecho se pierde sin aviso: hay una ventana de pérdida. Tampoco asigna `messageId` ni `X-Trace-Id`. La tabla `evento_entrega` (`V1__init_logistica.sql`) está rotulada como base de un outbox futuro, pero no tiene relay. Se cierra con un outbox transaccional sobre esa tabla o, como mínimo, con publisher confirms.
2. **Incentivos publica sin `messageId`** (`IncentivosEventosPublisher`, `convertAndSend` sin `MessagePostProcessor`). Cuando falta `messageId`, Notificaciones usa como clave del inbox un id de negocio del evento (`NotificacionService.resolverEventId`, con `personaDonanteId` para los eventos de incentivos). `[INFERRED]` Con el perfil `postgres`, un segundo evento de incentivos de la misma persona se descartaría como duplicado; no verificado con ejecución. Contradice el `message_id` obligatorio del ADR 20260911. Se cierra asignando un UUID por publicación. **Resuelto (2026-10-14):** `IncentivosEventosPublisher` asigna un `messageId` UUID por publicación (`IncentivosEventosPublisherTest`), y `resolverEventId` usa solo el `messageId`: sin él, el evento se procesa sin inbox y con log `[INBOX_SIN_CLAVE]`, en lugar de usar un id de negocio o un UUID aleatorio (`NotificacionServiceTest`).
3. **Incentivos no deduplica las donaciones** (`IncentivosEventosListener.onDonacionSegmentada` y `onDonacionRecibida` no consultan un inbox; `NuevaDonacionRequest` no trae id de donación; `MisionesDonacionService` suma en cada recepción). Un reenvío de RabbitMQ suma dos veces el progreso del donante. Se cierra con un inbox como el de Notificaciones.
4. **Incentivos sin DLQ** (`RabbitMQConfig`, `setDefaultRequeueRejected(false)`; sus colas no tienen argumentos `x-dead-letter-*`). Un mensaje que falla al procesarse se descarta. Se cierra con DLX y DLQ, como en Notificaciones.
5. **Schedulers de Incentivos sin lock distribuido** (`InactividadJob`, `RachaJob`, `RankingMensualJob`; 0 referencias a ShedLock en código y poms). Con más de una réplica, cada job corre una vez por réplica. El ADR de ShedLock (`proposed`) ponía como condición tener PostgreSQL, que #889 ya incorpora.
6. **`CallerRunsPolicy` en el pool asíncrono de Incentivos** (`AsyncConfig`, pool 2/10/500; el mismo default está en `CommonAsyncAutoConfiguration`). `[INFERRED]` Con la cola llena, la publicación AMQP corre en el hilo que la pidió y sus errores solo dejan un `warn` (`IncentivosEventosPublisher`). Contradice el objetivo de no bloquear al llamador.
7. **Traza AMQP solo de ida.** Donaciones envía `X-Trace-Id` en sus publicaciones (`DonacionesEventPublisher`, `ProveedorLogisticaAmqp`) y en el pedido HTTP (`ProveedorLogisticaHttp`). Ningún listener lo restaura en el MDC, y Logística e Incentivos no lo envían. Contradice `docs/arquitectura/eventos-amqp.md` (restauración en el consumidor) y deja a medias el ADR de trazabilidad distribuida.
8. **Tests JPA de Notificaciones excluidos de CI.** Surefire excluye `**/RepositoriosJpaTest.java` (`pom.xml`, sección `excludes`): el test de repositorios JPA de Notificaciones no corre en `mvn verify` ni en CI. Sí corren `RepositoriosLogisticaJpaTest`, `RepositoriosIncentivosJpaTest` y `ServicesSobrePostgresTest`. El ADR multi-schema cita `RepositoriosJpaTest` como evidencia de validación. **Resuelto (2026-10-28, RM-02):** se quitó la exclusión del `pom.xml` raíz; `mvn verify -pl notificaciones-service -am` corre `RepositoriosJpaTest` con 6 tests, 0 fallas y 0 salteados.
9. **Reglas ArchUnit faltantes.** El ADR 20260906 declara dos reglas que no están codificadas: dependencias permitidas de los controllers (`onlyDependOnClassesThat`) y dominio sin `jakarta.persistence` en todos los servicios (hoy solo en Logística). Ninguna regla impide que `services` dependa de `infrastructure`. Fugas observadas: Donaciones (`OutboxStore`; `ProcesadorDeDonaciones`, DTI-02), Incentivos (`IN8nClient` e `INotificacionesClient`, puertos ubicados en `infrastructure/`) y Logística (`RutaMapper` → `GeneradorDeURLSeguimiento`). El dominio de Donaciones importa `NecesidadDTO`, por eso su regla omite `dto..`. Además, las reglas de dominio solo inspeccionan `models.entities..`: no cubren `models.algoritmos`, `normalizacion`, `segmentacion`, `ports` ni `storage` (hoy sin imports de `dto`, `controllers` ni `infrastructure` en esos paquetes).
10. **Notificaciones recibe hechos que no procesa** (`RabbitMQConfig` de Notificaciones liga `notificaciones.donaciones` con `donacion.#` y `donante.#`; su mapeo de tipos y `DonacionEventListener` no cubren `donacion.segmentada.v1` ni `donante.dado-de-baja.v1`, que Donaciones publica para Incentivos). `[INFERRED]` La conversión falla, el mensaje se rechaza sin requeue y termina en `notificaciones.donaciones.dlq`; no verificado con ejecución. Llena la DLQ de mensajes legítimos y confunde la revisión manual. Se cierra con bindings explícitos por clave o con un descarte controlado de los tipos ajenos. **Resuelto (2026-10-14):** bindings explícitos, una clave por handler (`RabbitMQConfig.ROUTING_KEYS_DONACIONES_CONSUMIDAS`); `RabbitMQConfigTest` verifica que coincidan con el mapeo de tipos. En un broker ya provisionado, los bindings `donacion.#` y `donante.#` previos persisten hasta borrarlos a mano o recrear la cola.
11. **Callback de planificación con URL por defecto** (`logistica.self.base-url=${LOGISTICA_SELF_BASE_URL:http://localhost:8083}`; `PlanificacionService` arma el callback con ese valor). La instancia `logistica-externo` de `docker-compose.demo.yml` escucha en `PORT=8084` y no define `LOGISTICA_SELF_BASE_URL`. `[INFERRED]` Su planificador simulado llama a un puerto donde no escucha nadie: el callback falla, la solicitud queda pendiente y esa instancia no emite `ruta.asignada`; no verificado con ejecución. El mismo riesgo aplica en la nube si la plataforma asigna otro `PORT`. Se cierra fijando `LOGISTICA_SELF_BASE_URL` en la demo y en el despliegue.
12. **Comando del broker descartado sin aviso** (`EntregaSolicitadaEventListener` de Logística). Ante un error de validación al crear la entrega, el listener registra el error y confirma el mensaje. El broker de integración ya marcó la solicitud `ENVIADA` por el acuse de RabbitMQ: Donaciones no se entera y la donación queda en "Asignación realizada", sin reenvío. Se cierra publicando `entrega.fallida` (u otro hecho de rechazo) al descartar.

---

## DTI-17 — Baja lógica de camiones: el ADR dice irreversible, el código permite rehabilitar
<a id="dti-17"></a>

| Campo | Valor |
|---|---|
| ADR | [20260703-baja-logica-camion](./logistica-service/20260703-baja-logica-camion.md) |
| Qué decide el ADR | La baja es lógica por estado `DESHABILITADO` y es irreversible desde la API: un camión deshabilitado no transiciona a ningún otro estado vía el controller. |
| Qué hace el código | `Camion.habilitar()` acepta `DESHABILITADO` y vuelve a `DISPONIBLE` (`logistica-service/src/main/java/grupo5/logistica/models/entities/camiones/Camion.java:97-103`); `GestorDeCamiones.cambiarEstado` lo invoca con `DISPONIBLE` (`.../camiones/GestorDeCamiones.java:34`) y `CamionesController.cambiarEstado` lo expone (`.../controllers/impl/CamionesController.java:52-54`). |
| Impacto | La trazabilidad que justifica el ADR depende de que la baja sea definitiva; hoy cualquier cliente de la API puede reactivar un camión dado de baja. |
| Decision status | `accepted` (ADR) |
| Implementation status | `[OBSERVED] divergence` — hallazgo de la auditoría de documentación del 2026-10-14; sin resolver |
| Acción sugerida | Decidir si se ajusta el código o se reemplaza el ADR |
| Cuándo se saldará | Sin fecha asignada — pendiente de decisión del equipo |

---

## DTI-18 — Criterio de prioridad de subatendidos: el código implementa la alternativa rechazada
<a id="dti-18"></a>

| Campo | Valor |
|---|---|
| ADR | [20260615-criterio-prioridad-subatendidos](./donaciones-service/20260615-criterio-prioridad-subatendidos.md) |
| Qué decide el ADR | Ordenar por el porcentaje de necesidades satisfechas en el último trimestre por entidad (normaliza entre entidades de distinto tamaño). |
| Qué hace el código | `AlgoritmoPrioridadSubAtendidos.ordenarNecesidades` cuenta donaciones asignadas en los últimos 3 meses por entidad y ordena de menor a mayor (`donaciones-service/src/main/java/grupo5/donaciones/models/algoritmos/AlgoritmoPrioridadSubAtendidos.java:20-37`), que es la alternativa «contar las donaciones recibidas» descartada por el ADR. |
| Impacto | Una entidad con muchas necesidades puede recibir prioridad menor o mayor que la prevista por la equidad que el ADR busca; el test `ordenarNecesidades_cuandoUnaEntidadTieneMayorPorcentajeSatisfecho_debeQuedarAlFinal` (`AlgoritmoPrioridadSubAtendidosTest.java:82`) debe revisarse contra el criterio vigente. |
| Decision status | `accepted` (ADR) |
| Implementation status | `[OBSERVED] divergence` — hallazgo de la auditoría de documentación del 2026-10-14; sin resolver |
| Acción sugerida | Decidir si se ajusta el código o se reemplaza el ADR |
| Cuándo se saldará | Sin fecha asignada — pendiente de decisión del equipo |

---

## DTI-19 — Strategy en el gestor de algoritmos: acceso por índice en lugar de métodos con nombre
<a id="dti-19"></a>

| Campo | Valor |
|---|---|
| ADR | [20260615-strategy-gestor-algoritmos](./donaciones-service/20260615-strategy-gestor-algoritmos.md) |
| Qué decide el ADR | `GestorAlgoritmos` mantiene una `List<AlgoritmoAsignacion>` y la accede con métodos privados con nombre (`algoritmoPorCompatibilidad()`, `algoritmoPorPrioridad()`), descartando el acceso crudo por índice. |
| Qué hace el código | La clase `GestorAlgoritmos` no existe; la real es `GestorPropuestasDeAsignacion`, que accede por índice con `algoritmos.getFirst()` y `algoritmos.get(1)` (`donaciones-service/src/main/java/grupo5/donaciones/models/entities/propuestas/GestorPropuestasDeAsignacion.java:51-53`), la alternativa rechazada. |
| Impacto | El orden de la lista define implícitamente qué algoritmo es cuál; reordenar el constructor (`:25-30`) cambia el comportamiento sin que el nombre lo revele. |
| Decision status | `accepted` (ADR) |
| Implementation status | `[OBSERVED] divergence` — hallazgo de la auditoría de documentación del 2026-10-14; sin resolver |
| Acción sugerida | Decidir si se ajusta el código o se reemplaza el ADR |
| Cuándo se saldará | Sin fecha asignada — pendiente de decisión del equipo |

---

## DTI-20 — Resolución de conflictos en la consolidación de propuestas: regla de descarte distinta
<a id="dti-20"></a>

| Campo | Valor |
|---|---|
| ADR | [20260901-resolucion-de-conflictos-en-consolidacion-de-propuestas-de-asignacion](./donaciones-service/20260901-resolucion-de-conflictos-en-consolidacion-de-propuestas-de-asignacion.md) |
| Qué decide el ADR | Ante colisión sobre la misma necesidad, `consolidar()` conserva solo la propuesta de `AlgoritmoPrioridadSubAtendidos` y descarta la semántica. |
| Qué hace el código | `consolidar` devuelve las propuestas del segundo algoritmo cuyas necesidades también cubre el primero (la intersección) y, si no hay ninguna, la unión de ambas listas (`donaciones-service/src/main/java/grupo5/donaciones/models/entities/propuestas/GestorPropuestasDeAsignacion.java:62-82`); no aplica una prevalencia explícita de la prioridad social ni evita proponer dos donaciones para una necesidad en el caso de unión. |
| Impacto | El criterio ético que justifica el ADR no se aplica de forma determinista, y la regla vigente no está documentada. |
| Decision status | `proposed` (ADR) |
| Implementation status | `[OBSERVED] divergence` — hallazgo de la auditoría de documentación del 2026-10-14; sin resolver |
| Acción sugerida | Decidir si se ajusta el código o se reemplaza el ADR |
| Cuándo se saldará | Sin fecha asignada — pendiente de decisión del equipo |

---

## DTI-21 — Alcance REST de EntidadBeneficiaria: decisión vigente (PUT y DELETE) sin ADR
<a id="dti-21"></a>

| Campo | Valor |
|---|---|
| ADR | [20260702-alcance-operaciones-rest-entidad-beneficiaria](./donaciones-service/20260702-alcance-operaciones-rest-entidad-beneficiaria.md) (Status `rejected`) |
| Qué proponía el ADR (rechazado) | No exponer `PUT` ni `DELETE` sobre `EntidadBeneficiaria`; la baja se lograría anonimizando su Persona Jurídica. |
| Qué hace el código | `EntidadBeneficiariaController` expone `@PutMapping("/{id}")` y `@DeleteMapping("/{id}")` (`donaciones-service/src/main/java/grupo5/donaciones/controllers/impl/EntidadBeneficiariaController.java:52-63`); desde la #892 el `DELETE` es una baja lógica: `entidad.darDeBaja()` y `save`, sin borrar la fila, y desactiva sus necesidades (`.../services/impl/EntidadBeneficiariaService.java`). Antes hacía `repository.delete(entidad)`. |
| Impacto | La decisión vigente (mutación y borrado físico) no tiene ADR que registre su justificación ni su efecto sobre las `Necesidad` que referencian la entidad. |
| Decision status | `rejected` (ADR) |
| Implementation status | `[OBSERVED] undocumented` — hallazgo de la auditoría de documentación del 2026-10-14; sin resolver |
| Acción sugerida | Registrar la decisión vigente en un ADR nuevo |
| Cuándo se saldará | Sin fecha asignada — pendiente de decisión del equipo |

---

## DTI-22 — Incentivos descarta en silencio los eventos que llegan antes que el alta del donante
<a id="dti-22"></a>

| Campo | Valor |
|---|---|
| ADR | [20261009-tolerancia-al-desorden-de-eventos-en-incentivos-y-consistencia-eventual-en-tests](./20261009-tolerancia-al-desorden-de-eventos-en-incentivos-y-consistencia-eventual-en-tests.md) |
| ADR complementario | [20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) (clúster DLQ, hoy solo en Notificaciones) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] not-started` — solo se ajustaron los 4 tests de `CrossServiceCommunicationIT` que fallaron (`PollingUtils.esperarDonanteEnIncentivos`) |
| Target | `incentivos-service` (`RabbitMQConfig`, `IncentivosEventosListener`) · `integration-tests` |
| Cuándo se saldará | A definir por el equipo |

1. **Pérdida de eventos por desorden entre colas:** cada evento de Incentivos tiene su propia cola y RabbitMQ no ordena entre colas. Si `donacion.segmentada` o `donacion.recibida` llega antes que `donante.registrado`, `MisionesDonacionService.obtenerDonante` lanza `DONANTE_INCENTIVOS_NO_ENCONTRADO` y, con `defaultRequeueRejected=false` y sin DLQ, el mensaje se descarta.
2. **Tests que suponen propagación inmediata:** las lecturas cruzadas entre servicios no siempre esperan al evento; con el RabbitMQ local la carrera no se manifiesta.

---

## DTI-23 — Orden de eventos baja/alta del donante en incentivos (cola única)
<a id="dti-23"></a>

| Campo | Valor |
|---|---|
| ADR | [20261007-identidad-compartida](./donaciones-service/20261007-identidad-compartida-de-roles-donante-y-entidad-beneficiaria.md) (sección «Orden de eventos del ciclo de vida del donante») |
| Relacionada | [DTI-22](#dti-22): mismo origen (colas separadas sin orden entre sí) en los mismos archivos de `incentivos-service`; esta es la variante baja/alta del mismo donante |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] not-started` — `incentivos-service` mantiene dos colas (`incentivos.donante-registrado`, `incentivos.donante-dado-de-baja`) con un listener cada una. Con el id de rol igual al de la persona, un `DELETE` seguido de un `POST /api/donantes` rápido puede llegar al revés: el alta se ignora (el perfil existe), la baja borra el perfil y las siguientes `donacion.segmentada` y `donacion.recibida` se descartan (`defaultRequeueRejected=false`). Misiones y ranking dejan de contar en silencio |
| Target | `incentivos-service` (`RabbitMQConfig`, `IncentivosEventosListener`): cola única `incentivos.donante-ciclo-de-vida`, concurrencia 1, retiro de las dos colas actuales. Instrucciones de entrega en el plan de roles, Etapa 6 |
| Cuándo se saldará | **A definir** — a cargo del equipo de `incentivos-service`; antes de mergear a `main`, o con el riesgo declarado en el PR |

---

## DTI-24 — Lo publicado no es lo validado y el despliegue no se reproduce desde el repo
<a id="dti-24"></a>

| Campo | Valor |
|---|---|
| ADR | [20261020-promocion-de-artefactos-por-digest-y-despliegue-reproducible-de-logistica](./20261020-promocion-de-artefactos-por-digest-y-despliegue-reproducible-de-logistica.md) |
| ADR complementario | [logistica-service/20261009-despliegue-de-logistica-en-paas-con-contenedores](./logistica-service/20261009-despliegue-de-logistica-en-paas-con-contenedores.md) (puntos 1 y 5) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] not-started` — relevado en `origin/main` `a808942c` por el análisis [preprod fiel](../testing/preprod-fiel.md) (B2, B3) |
| Target | `.github/workflows/merge.yml` · `.github/workflows/main.yml` · `deploy.yml` nuevo |
| Cuándo se saldará | E6, según el [roadmap](../testing/preprod-fiel-roadmap.md) (RM-06 y RM-07); sujeto a la aprobación del ADR y a las precondiciones de Render |

1. **Imagen publicada sin preprod:** `merge.yml` reconstruye desde el commit integrado y publica `:<rama>`, `:sha-<7>` y `:latest` sin correr el preprod (`merge.yml:259-269`). La imagen que valida el PR (`pr-<N>`) no es la que se publica.
2. **Estado post-merge sin validar:** `main.yml` solo corre en `pull_request` (`main.yml:44-46`). `[INFERRED]` Dos PR en verde pueden romper la base al combinarse.
3. **Despliegue manual sin rastro:** no hay job de despliegue, smoke ni rollback ensayado; Render, Neon y CloudAMQP se configuran a mano. No se sabe si Render construye desde el repo o baja la imagen de GHCR.

---

## DTI-25 — El gate de preprod todavía puede dar verdes falsos
<a id="dti-25"></a>

| Campo | Valor |
|---|---|
| ADR | [20260903-protocolo-salida-semantico-y-quality-gate-estricto](./20260903-protocolo-salida-semantico-y-quality-gate-estricto.md) ("Ningún pull request debe obtener estado verde si contiene tests fallidos [...] o ausencia total de ejecución de pruebas") |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] in-progress` — parte del protocolo ya está implementada: los códigos de salida y el umbral de 0 tests, que ya falla (`report_test_failures.py --fail-on-zero-tests`, `main.yml:801`); las tres vías de abajo estaban abiertas en `origin/main` `a808942c`. **RM-02 (2026-10-28, rama `E5_preprod_fiel_rm08`)** cierra las tres en el código: `PREPROD_STRICT` en `BaseIT`, `--required-groups` en `report_test_failures.py`, `::error` sin imagen `pr-<N>` y Sonar requerido con `SONAR_TOKEN`. Se cierra cuando la primera corrida de CI lo confirme |
| Target | `integration-tests` (`BaseIT.java`) · `scripts/report_test_failures.py` · `.github/workflows/main.yml` |
| Cuándo se saldará | E5, según el [roadmap](../testing/preprod-fiel-roadmap.md) (RM-02) |

1. **Tests salteados cuentan como verdes:** `ContractIT` (`ContractIT.java:41,88,140`) y `LogisticaIntegrationIT` (`LogisticaIntegrationIT.java:24`) se saltean por `assumeTrue` si `BaseIT.isServiceAvailable` no recibe 200 de `/v3/api-docs` en 500 ms (`BaseIT.java:35-53`), y el reporte no los cuenta como falla.
2. **Imagen base en lugar de la del PR:** si no encuentra la imagen `pr-<N>` de un servicio, el preprod usa la de la rama base con solo un aviso (`main.yml:699-704`).
3. **Sonar opcional:** el quality gate final no exige `sonar-analysis` (`main.yml:912`, `"never"`).

La exclusión de `RepositoriosJpaTest` de Surefire, otra vía del mismo problema, estaba registrada en el ítem 8 de DTI-16 y quedó resuelta con RM-02.

---

## DTI-26 — Base de datos por servicio: decisión propuesta, sin implementar
<a id="dti-26"></a>

| Campo | Valor |
|---|---|
| ADR | [20261028-base-de-datos-por-servicio-en-el-mismo-motor-postgresql](./20261028-base-de-datos-por-servicio-en-el-mismo-motor-postgresql.md) (propone reemplazar a [20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql](./20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md)) |
| Decision status | `proposed` (los dos ADRs; la relación `superseded` la aprueba un humano) |
| Implementation status | `[OBSERVED] not-started` — por decisión del usuario (2026-10-28, SPEC-05) solo se documentó: `persistencia/init-db`, los composes y las properties chocan con `origin/E4_donaciones_minio` y `origin/E4_persistencia_donaciones` |
| Target | `persistencia/init-db/*` · 3 composes · 3 `application-postgres.properties` · 4 tests de Testcontainers · `persistencia/README.md` |
| Cuándo se saldará | Después de que mergeen las dos ramas de E4, según el [roadmap](../testing/preprod-fiel-roadmap.md) (RM-19). Precondiciones: Neon admite una base por servicio (RM-01) y el enunciado de E5 no exige un schema por servicio |
