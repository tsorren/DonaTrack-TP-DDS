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
| Cuándo se saldará | **Entrega 5 (Semana del 19 de Octubre 2026)** — refactor previo a la integración con la interfaz Web MVC · **A revisar:** el enunciado final ([Enunciado-TP-Anual-2026.pdf](../entregas/Enunciado-TP-Anual-2026.pdf)) renumera las entregas: E5 es Persistencia, E6 Interfaz de Usuario y Arquitectura MVC, E7 Observabilidad y Seguridad. La reprogramación queda a cargo del equipo |

---

## DTI-06 — Desacoplamiento de referencias directas entre aggregates por UUID

| Campo | Valor |
|---|---|
| ADR | [20260901-dti-06](./donaciones-service/20260901-dti-06-desacoplamiento-de-referencias-directas-entre-agregados-por-uuid.md) |
| ADR complementario | [20260901-evaluacion-de-interfaz-asignable](./donaciones-service/20260901-evaluacion-de-interfaz-asignable-vs-identificador-entidad-beneficiaria.md) |
| Decision status | `proposed` |
| Implementation status | `[VERIFIED] implemented` — Paso 6 de la persistencia de Donaciones (rama `E4_persistencia_donaciones`, 2026-10-22). `Necesidad` y `PeriodoNecesidad` ya no guardan donaciones; `DonacionIndependiente` guarda `necesidadId`, `periodoId` y `entidadBeneficiariaId` (`DestinoAsignacion`), y `Asignable` se eliminó. Lo asignado se suma sobre las donaciones (`AcumuladosDeAsignacion`) y se pasa como parámetro. Detalle en `docs/entrega-4/donaciones/bitacora-persistencia.md` |
| Target | donaciones-service · Entrega de persistencia (prioridad alta) |
| Cuándo se saldará | Saldada en código; los dos ADR siguen `proposed` hasta que un revisor humano los acepte |

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
| Cuándo se saldará | **Entrega 6: Despliegue, Observabilidad y Seguridad (Semana del 23 de Noviembre 2026)** — integración con `auth-service`, protección perimetral con roles (`ROLE_ADMIN`), traslado a `/api/admin/` y respuesta `202 Accepted` asíncrona · **A revisar:** el enunciado final ([Enunciado-TP-Anual-2026.pdf](../entregas/Enunciado-TP-Anual-2026.pdf)) renumera las entregas: E5 es Persistencia, E6 Interfaz de Usuario y Arquitectura MVC, E7 Observabilidad y Seguridad. La reprogramación queda a cargo del equipo |

---

## DTI-10 — Desacoplamiento de errores de dominio de incentivos en GlobalExceptionHandler

| Campo | Valor |
|---|---|
| ADR | [20260905-dti-10](./incentivos-service/20260905-dti-10-desacoplamiento-de-errores-de-dominio-en-global-exception-handler.md) |
| Decision status | `proposed` |
| Implementation status | `[INFERRED] deferred` — preserva consistencia con patrón preexistente en Entrega 2 |
| Target | `common-lib` (`GlobalExceptionHandler`, `ErrorCatalog`) · `incentivos-service` (`RankingController`) |
| Cuándo se saldará | **Entrega 5: Arquitectura Web MVC (Semana del 19 de Octubre 2026)** — refactor de capa Web y reemplazo del `if/else` por resolución idiomática de `Optional` o jerarquía tipada · **A revisar:** el enunciado final ([Enunciado-TP-Anual-2026.pdf](../entregas/Enunciado-TP-Anual-2026.pdf)) renumera las entregas: E5 es Persistencia, E6 Interfaz de Usuario y Arquitectura MVC, E7 Observabilidad y Seguridad. La reprogramación queda a cargo del equipo |

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
| Implementation status | `[OBSERVED] in-progress` — Fases 1, 2, 3A y 4 implementadas: ArchUnit universal y fitness functions activas, Pitest acotado a matching, persistencia efímera con `@ServiceConnection` y controllers en `@WebMvcTest`, validación viva de contratos OpenAPI en `ContractIT` erradicando AP-01, pruebas de rendimiento migradas a k6 erradicando AP-02 (`PerformanceStressIT` eliminado); Subfase 3B (AMQP) catalogada como `[DEFERRED_PENDING_RABBITMQ_CONTRACTS]`; **implementada por RM-08 (2026-10-28, rama `E5_preprod_fiel_rm08`)**: consumidor espía en el E2E y validación contra los schemas `evento-*`, aviso en el PR y error en el post-merge ([ADR de política de contratos](./20261028-politica-de-evolucion-de-contratos-entre-servicios.md)) |
| Target | Monorepo · `integration-tests` · microservicios (`donaciones`, `logistica`, `incentivos`, `notificaciones`) |
| Cuándo se saldará | **Saldada en Entrega 4 (Fases 1, 2, 3A y 4)**; Subfase 3B (AMQP) implementada por RM-08 (E5, 2026-10-28) |

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
| Cuándo se saldará | (1) y (2) ya saldados con el perfil `postgres` (persistencia JPA de `donaciones-service`), siguen en memoria solo con `!postgres`; (3) y (4) con el `auth-service` (**Entrega 6, semana del 23 de noviembre de 2026**); (5) y (6) al integrar un proveedor de logística real |

**Ítems:**

1. **Outbox del broker en memoria solo con `!postgres`** (`LogisticaOutboxEnMemoria`): sin atomicidad con el estado ni durabilidad; los pedidos pendientes se pierden si `donaciones-service` se reinicia. Con el perfil `postgres` el outbox es `LogisticaOutboxJpaAdapter` (tabla `outbox_logistica`, `V12__logistica_outbox_idempotencia.sql`), sin tocar el broker.
2. **«Una solicitud activa por donación» no es atómica solo con `!postgres`** (`SolicitudesEntregaRepositoryEnMemoria`): dos pedidos simultáneos para la misma donación podrían crear dos solicitudes. Hoy no ocurre (cada donación se procesa una vez por aprobación). Con el perfil `postgres` las solicitudes son `SolicitudEntregaRepositoryJpaAdapter` (tabla `solicitud_entrega`, también en `V12`).
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
| Implementation status | `[OBSERVED] in-progress` — relevado en `origin/main` `a808942c` por el análisis [preprod fiel](../testing/preprod-fiel.md) (B2, B3). La rama `E5_preprod_fiel_rm08` (2026-10-28) implementa RM-06 (`merge.yml`: candidato `:sha-<7>`, `preprod-post-merge`, `promote` y manifiesto; `main.yml`: servicios no modificados por digest) y RM-07 sin Render (`deploy.yml`, ensayo con smoke y rollback). Se cierra cuando la primera corrida post-merge y de `deploy.yml` lo confirme |
| Target | `.github/workflows/merge.yml` · `.github/workflows/main.yml` · `.github/workflows/deploy.yml` |
| Cuándo se saldará | Con la primera corrida en CI, según el [roadmap](../testing/preprod-fiel-roadmap.md) (RM-06 y RM-07); sujeto a la aprobación del ADR. Render ya no es precondición (D3). `deploy.yml` corre recién cuando llegue a `main`: GitHub solo dispara `workflow_run` y `workflow_dispatch` de workflows presentes en la rama por defecto |

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

## DTI-26 — Base de datos por servicio: implementada en el repo (falta la nube, RM-01)
<a id="dti-26"></a>

| Campo | Valor |
|---|---|
| ADR | [20261028-base-de-datos-por-servicio-en-el-mismo-motor-postgresql](./20261028-base-de-datos-por-servicio-en-el-mismo-motor-postgresql.md) (reemplaza a [20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql](./20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md)) |
| Decision status | `accepted` (2026-10-29); el ADR de multi-schema queda `superseded` |
| Implementation status | **Resuelta en el repo (2026-10-29, rama `E5_dti26_base_por_servicio`).** `[VERIFIED] implemented` — `01-init-schemas-roles.sql` crea las 4 bases con dueño su rol y les revoca `CONNECT` a PUBLIC (también en `POSTGRES_DB` y `postgres`). En `postgres:16-alpine`, cada rol entra solo a su base. Los tests de Testcontainers de incentivos, logística y notificaciones pasan con `withDatabaseName(<servicio>)`; los de Donaciones usan la base por defecto del contenedor y no cambiaron. Fuera del repo queda RM-01: confirmar que Neon admite una base por servicio antes de desplegar. **Para validar por un humano:** el script también revoca `CONNECT` a PUBLIC en `POSTGRES_DB` (`donatrack`) y en `postgres`, un endurecimiento que el ADR no menciona (sin él, cualquier rol entraba a la base administrativa); el superusuario y `pg_isready` no se ven afectados. **Desvío del ADR:** las copias del script en `*/src/test/resources/init-db/` no se actualizaron. Siguen creando los 4 schemas en una sola base y los tests corren como superusuario del contenedor, así que ningún test automatizado vigila el aislamiento entre bases (Validación 1 del ADR); se verificó a mano con `postgres:16-alpine` |
| Target | `persistencia/init-db/01-init-schemas-roles.sql` · 2 composes (`docker-compose.yml`, `docker-compose.preprod.yml`) · 4 `application-postgres.properties` · 5 tests de Testcontainers (incentivos, logística, notificaciones; los de Donaciones usan la base por defecto del contenedor) · `persistencia/README.md` |
| Cuándo se saldará | Saldada en el repo el 2026-10-29, después del merge de #897 (RM-19 del [roadmap](../testing/preprod-fiel-roadmap.md)). Pendiente fuera del repo: RM-01 (Neon). Cerrado el pendiente del enunciado: la pág. 25 (numeración impresa) de `docs/entregas/Enunciado-TP-Anual-2026.pdf` pide «persistencia en un esquema relacional propio» por servicio y admite compartir un mismo motor «a fines de simplicidad»; una base por servicio, con su schema adentro, lo cumple. Para un volumen local anterior: `docker compose down -v` |

---

## DTI-27 — Contratos de Donaciones incumplidos, detectados por los contratos vivos (RM-08)
<a id="dti-27"></a>

| Campo | Valor |
|---|---|
| ADR | [20261028-politica-de-evolucion-de-contratos-entre-servicios](./20261028-politica-de-evolucion-de-contratos-entre-servicios.md) |
| Decision status | `accepted` (2026-10-29) |
| Implementation status | **Resuelta.** `[VERIFIED] implemented` — `CONTRATOS_ESTRICTO=true PREPROD_STRICT=true ./run-preprod-tests.sh` (2026-10-29, rama `E5_dti27_adrs_pendientes`): 30/30, exit 0 y sin avisos de contratos AMQP. El ítem 3 se corrigió en la spec y los ítems 1 y 2 en el productor |
| Target | `donaciones-service` (publicación de eventos) · `docs/arquitectura/contratos/openapi-donaciones.yaml` |
| Cuándo se saldará | Antes del corte de E5 (D5: el post-merge de `ENTREGA_5` falla y no promueve hasta que se corrijan). Saldada el 2026-10-29: los 3 ítems están resueltos y verificados con el E2E estricto |

1. **`donacion.en-camino.v1` sin `urlMapa`.** El schema y el DTO (`EventoDonacionEnCaminoV1`, `@NotBlank`) lo exigen, pero en el E2E Donaciones publica el evento con `urlMapa` en `null` y `@JsonInclude(NON_NULL)` lo omite. **Resuelto (2026-10-29, decisión del responsable de integración: el productor siempre envía):** `PATCH /donaciones-independientes/{id}/estado` con `EN_TRASLADO` y sin `urlMapa` devuelve 400 (`ERR-VAL-415`, `DONACION_INDEPENDIENTE_RECORRIDO_SIN_URL_MAPA`). La causa era la transición por REST: Logística sí manda `urlMapa` en `RutaIniciada` (`GeneradorDeURLSeguimiento`).
2. **`donacion.recibida.v1` sin `patenteCamion`.** Mismo caso (`EventoDonacionRecibidaV1`, `@NotBlank`). **Resuelto (2026-10-29, misma decisión):** `ENTREGADA` sin `patenteCamion` devuelve 400 (`ERR-VAL-416`, `DONACION_INDEPENDIENTE_ENTREGA_SIN_PATENTE`). El validador va en los estados `ListaParaEntregar` y `EnTraslado`, así que una transición inválida sigue dando `ESTADO_DONACION_TRANSICION_INVALIDA`. Los clientes de `integration-tests` mandan ambos datos. **Hueco que queda:** los schemas de entrada de Logística no exigen esos datos (`evento-ruta-iniciada.schema.json` no tiene `urlMapa` en `required` y `evento-entrega-exitosa.schema.json` tampoco tiene `patenteCamion`). Un proveedor que cumpla esos schemas sin mandarlos haría fallar el cambio de estado en `ProcesadorEventosLogistica`: la donación quedaría en `EN_TRASLADO`, con un log de error y sin DLQ. El Logística propio siempre los manda. Cerrarlo pide decidir si esos campos pasan a `required` en los schemas de Logística.
3. **`POST /api/personas` sin body en la spec.** `openapi-donaciones.yaml` no declara el body del request ni el de la respuesta 201; el servicio recibe y devuelve una persona. La validación en vivo de Donaciones (RM-08, criterio 2) lo rechaza. **Resuelto (2026-10-29, decisión del responsable de integración: completar la spec):** `PersonaInputDTO` y `PersonaOutputDTO` (polimórficos HUMANA/JURIDICA, con dirección y medios de contacto) en `components`, y el body de `POST`, `GET` y `PUT` de `/api/personas`. `ContractIT` pasa 7/7 contra el preprod, incluida una persona eliminada: la anonimización (DTI-01) devuelve `ANONIMIZADO`, por eso la salida de los medios de contacto no exige formato de correo. oasdiff marca el body requerido nuevo como incompatible: está registrado en `cambios-incompatibles-aceptados.txt`, porque el servicio siempre lo exigió.

Para los ítems 1 y 2 se evaluaron dos caminos. Se eligió que el productor siempre envíe el dato; el contrato de los eventos no cambia. La alternativa descartada era dejar el campo opcional en los schemas y los DTOs de Donaciones y Notificaciones (cambio compatible para los consumidores): Notificaciones usa ambos datos para armar el aviso y la URL de seguimiento ya la genera Logística.

**Cambio de contrato de la API:** el body de `PATCH /donaciones-independientes/{id}/estado` ahora exige `urlMapa` si el estado es `EN_TRASLADO` y `patenteCamion` si es `ENTREGADA`. El schema y el OpenAPI lo dicen en la descripción de los campos; `oasdiff` no lo marca porque ambos campos siguen siendo opcionales en el schema.

---

## DTI-28 — La verificación visual del cliente liviano no corre en el CI
<a id="dti-28"></a>

| Campo | Valor |
|---|---|
| ADR | [20261030-verificacion-visual-con-playwright-y-axe](./cliente-liviano/20261030-verificacion-visual-con-playwright-y-axe.md) |
| ADR complementario | [20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas](./cliente-liviano/20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `VerificacionVisualTest` (axe AA, toque de 48 px, desbordes y capturas) corre solo con `mvn test -pl cliente-liviano -Pvisual`. El CI (`main.yml`, job `build-and-test`) corre `mvn clean verify -pl cliente-liviano -am` sin el perfil, así que no la ejecuta. En el CI corre `InterfazFitnessTest` (tokens, colores, quiebres y contraste) |
| Target | `.github/workflows/main.yml` (un job o paso con `-Pvisual` y caché de los navegadores de Playwright) · `cliente-liviano/pom.xml` |
| Cuándo se saldará | Antes del cierre de E6. Hasta entonces, cada flujo la corre a mano (skill `vista-ui`) |

1. **Riesgo:** un PR puede romper la accesibilidad, el toque o el responsive sin que el CI lo note.
2. **Costo de saldarla:** el job baja unos 200 MB de driver más Chromium; con caché, es una vez por versión de Playwright.

---

## DTI-29 — El contrato de entregas no tiene remito ni reporte de problemas, y la donación no devuelve su necesidad
<a id="dti-29"></a>

| Campo | Valor |
|---|---|
| ADR | [20261102-contrato-de-entregas-para-la-entidad-remito-reporte-y-datos-de-donaciones](./20261102-contrato-de-entregas-para-la-entidad-remito-reporte-y-datos-de-donaciones.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `CambioEstadoEntregaRequestDTO` no acepta remito ni reporte; `EntregaResponseDTO` no los devuelve; `DonacionIndependienteResponseDTO` no devuelve `necesidadId`. El prototipo de Raíces ya pide el remito al confirmar |
| Target | `logistica-service` (`Entrega`, DTOs de entregas, `entrega.exitosa`) · `donaciones-service` (`DonacionIndependienteResponseDTO`) · `openapi-logistica.yaml`, `openapi-donaciones.yaml` y `schemas/evento-entrega-exitosa.schema.json` |
| Cuándo se saldará | Cuando `cliente-liviano` consuma Logística para Confirmar recepción (RF-12). Pide antes HB-13 (pertenencia) |

1. **Remito.** `numeroRemito` en la confirmación (`ENTREGADA`) y en `EntregaResponseDTO`; fuera de las respuestas públicas y de los eventos.
2. **Reporte.** `reporte` opcional en la confirmación (`motivo`, `detalle`, `cantidadRecibida`), guardado en la entrega y como campo opcional de `entrega.exitosa`. Falta decidir qué hace Donaciones con una cantidad recibida menor (por ejemplo, cuánto cuenta para el período).
3. **`necesidadId`** como campo opcional de `DonacionIndependienteResponseDTO`.
4. **Pantalla "Llegó con un problema":** sin diseñar (`sin-dibujar` en el prototipo).

---

## DTI-30 — La necesidad no expone estado, versión, unidad, fecha límite ni historia de períodos
<a id="dti-30"></a>

| Campo | Valor |
|---|---|
| ADR | [20261102-ciclo-de-vida-de-la-necesidad-en-el-contrato](./donaciones-service/20261102-ciclo-de-vida-de-la-necesidad-en-el-contrato.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `NecesidadDTO` (`id, tipo, idEntidad, idSubcategoria, cantidadNecesitada, descripcion, estaSatisfecha, fechaInicio, fechaFin`); `GET /api/necesidades` sin filtro por estado y con las dadas de baja; período de cualquier largo; el cambio de cantidad vale desde el próximo período. El prototipo de Raíces ya muestra lo decidido |
| Target | `donaciones-service` (`NecesidadDTO`, `NecesidadesService`, `NecesidadRecurrente`, `Subcategoria`, consulta de lo recibido) · `openapi-donaciones.yaml` · `cambios-incompatibles-aceptados.txt` |
| Cuándo se saldará | Cuando `cliente-liviano` consuma Donaciones para Necesidades (RF-10). Los dos cambios incompatibles se coordinan con `integration-tests` y Postman |

1. **`estado`** calculado (`VIGENTE`, `CUMPLIDA`, `TERMINADA`, `DADA_DE_BAJA`) y filtro `?estado=`; corregir "Listar necesidades activas" en la OpenAPI.
2. **`version`** en la salida y en el `PUT` (`409` si no coincide).
3. **`fechaLimite`** obligatoria en las extraordinarias. **Incompatible.**
4. **`periodicidad`** (`SEMANAL`, `QUINCENAL`, `MENSUAL`) obligatoria en las recurrentes; `fechaFin` deprecado como entrada. **Incompatible.**
5. **Cambio de cantidad** en el período en curso, que queda cumplido si lo recibido ya alcanza, y `ultimoCambio` (`cantidadAnterior`, `fecha`) en la salida mientras dure ese período, para el cartel de la entidad (review de Sofía, 2026-11-02).
6. **`GET /api/necesidades/{id}/periodos`** (objetivo, asignado, recibido y resultado) y `GET /donaciones-independientes?necesidadId=`.
7. **`unidad`** en la subcategoría (hereda la de la categoría) y de solo lectura en la necesidad.

Refina el ADR aceptado de [gestión de necesidades y períodos](./donaciones-service/20260609-gestion-de-necesidades-y-periodos.md).

---

## DTI-31 — Dar de baja una necesidad no cancela sus entregas que todavía no salieron
<a id="dti-31"></a>

| Campo | Valor |
|---|---|
| ADR | [20261102-baja-de-una-necesidad-con-entregas-en-curso](./20261102-baja-de-una-necesidad-con-entregas-en-curso.md) |
| ADR complementario | [20261007-broker-de-integracion-con-logistica](./20261007-broker-de-integracion-con-logistica.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `NecesidadesService.eliminar` solo llama a `desactivar()`; Logística no tiene cancelación (`EstadoEntrega` sin `CANCELADA`); la asignación por `PATCH` no consulta si la necesidad sigue activa |
| Target | `donaciones-service` (baja, broker, procesador de eventos de vuelta) · `logistica-service` (`Entrega.cancelar`, listener del comando, salida de la ruta) · `schemas/` (comando `entrega.cancelada.<proveedorId>.v1` y evento `entrega.cancelada`) |
| Cuándo se saldará | Antes de habilitar la baja de necesidades en el cliente con datos reales |

1. **Comando** `entrega.cancelada.<proveedorId>.v1` por el outbox del broker, y marca de "cancelación pedida" en la donación.
2. **`Entrega.cancelar`** solo desde `PENDIENTE`, estado `CANCELADA`, salida de la ruta sin iniciar y evento firmado `entrega.cancelada`. **Incompatible** (valor nuevo del enum).
3. **Vuelta** a `EN_DEPOSITO` sin destino al recibir el evento.
4. **Guarda `NECESIDAD_INACTIVA`** en la asignación por `PATCH`.
5. **Proveedores HTTP** sin cancelación: la entrega sigue y se loguea.

---

## DTI-32 — No hay preferencia para ocultar el nombre del donante ni constancia de los legales
<a id="dti-32"></a>

| Campo | Valor |
|---|---|
| ADR | [20261102-nombre-visible-del-donante-y-constancia-de-los-legales](./20261102-nombre-visible-del-donante-y-constancia-de-los-legales.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `Donante` sin preferencia; `DonanteResumenDTO` devuelve la persona entera (documento, dirección, contactos); sin términos ni constancia de aceptación. El prototipo de Raíces ya muestra el nombre, la opción en Mi perfil, los legales en el pie y la casilla en la ficha |
| Target | `donaciones-service` (`Donante`, `QuienDonoDTO`, `PATCH /api/donantes/{id}/preferencias`, `AceptacionLegales`, `POST /api/personas`, `GET /api/personas/me`) · `openapi-donaciones.yaml` · `cambios-incompatibles-aceptados.txt` |
| Cuándo se saldará | Con el registro y el perfil reales (RF-03, RI-AUTH-02, HB-11) |

1. **`mostrarNombre`** en `Donante` y su endpoint de preferencias.
2. **`QuienDonoDTO`** en las respuestas que lee la entidad, en lugar de `DonanteResumenDTO`.
3. **`AceptacionLegales`** (persona, versión y fecha) y `legalesAceptados` obligatorio en el alta. **Incompatible.**
4. **Texto de los legales:** lo escribe el equipo.
5. **`mostrarNombre` en la donación** (opcional; solo oculta) para "Esta vez, sin mi nombre" al registrarla (review de Sofía, 2026-11-02).
6. **Categoría del ranking** con el nombre oculto (en el prototipo, dato fijo de la maqueta; `cliente-liviano` no tiene todavía un cliente de Incentivos): el cliente liviano la pide a Incentivos (`GET /api/incentivos/donantes/{donanteId}`, `DonantePerfilDTO.categoria`) con el `donanteId`, que `QuienDonoDTO` suma también con el nombre oculto; el cliente liviano lo usa en el servidor y a la entidad le muestra solo la categoría, nunca el id. Falta decidir con qué credencial lo pide el cliente liviano (`[INFERRED]` el perfil de incentivos es del propio donante, no de la entidad).

Complementa el ADR aceptado de [privacidad de usuarios](./20260519-privacidad-de-usuarios.md): ocultar el nombre no reemplaza la anonimización.

---

## DTI-33 — El formulario de contacto no tiene endpoint ni bandeja
<a id="dti-33"></a>

| Campo | Valor |
|---|---|
| ADR | Sin ADR propio. Si el contacto termina en un servicio nuevo o en un canal nuevo entre servicios, lo pide (AGENTS.md §9.1) |
| Decision status | `proposed` |
| Decidido por | Usuario (2026-11-02, pregunta 17 del artifact de Raíces): formulario dentro de DonaTrack |
| Implementation status | `[OBSERVED] deferred` — ningún servicio tiene mensajes de contacto. El prototipo de Raíces tiene la pantalla de Contacto, y los avisos de error enlazan a ella con el código del error |
| Target | Por decidir: `notificaciones-service` (ya tiene una bandeja de notificaciones) o `donaciones-service` |
| Cuándo se saldará | Antes de publicar el sitio. Hasta entonces, la pantalla es maqueta |

1. **Dueño.** Decidir qué servicio recibe el mensaje (`POST /api/contacto`: tema, mensaje, código de error opcional, y correo si no hay sesión) y dónde lo lee el administrador.
2. **Abuso y repetidos.** Límite de envíos por persona o dirección, porque el formulario es público; y detectar el mismo mensaje enviado dos veces (la pantalla sin conexión pide mandarlo de nuevo).
3. **Respuesta.** Por correo, desde Notificaciones, con el número del mensaje.

## DTI-34 — El cliente liviano no tiene catálogo, tokens de movimiento ni cerco de componentes
<a id="dti-34"></a>

| Campo | Valor |
|---|---|
| ADR | [Interacción, movimiento y componentes del cliente liviano](cliente-liviano/20261104-interaccion-movimiento-y-componentes-del-cliente-liviano.md) |
| Decision status | `proposed` |
| Decidido por | tsorren (2026-11-04): Alpine CSP, CSS nativo, sistema de movimiento, capa CUBE con cerco y migración incremental con backlog obligatorio |
| Implementation status | `[OBSERVED] partial` (2026-11-04): fase 0 implementada en la rama `E6_raices` (tokens `--movimiento-*` y 0 duraciones o curvas literales fuera de `tokens.css`; `@layer`; composición, utilidades y `movimiento.css`; Alpine CSP; catálogo `/estilos` y `componentes.md`; cerco en `InterfazFitnessTest`; skills; video; View Transitions). Fase 1 también implementada (2026-11-04, `E6_raices`): los 13 `@keyframes` y las duraciones, curvas y esperas de las 7 hojas de pantalla pasaron a `movimiento.css` como animaciones con nombre (`--movimiento-animacion-*`), con una regla nueva del cerco. Fase 2, tanda 1 (B2-01 a B2-03: botón, campo y sello) también implementada (2026-11-04, `E6_raices`, 38033d2a): variantes por atributo, fragmento `componentes/sello` y tope en 197. Fase 2, tanda 2 (B2-04, B2-05 y B2-09: aviso breve con la cola de Alpine, banner, diálogo, estado vacío, carga y esqueleto) también implementada (2026-11-04, `E6_raices`, 38033d2a): tope en 189. Fase 2, tanda 3 (B2-06 a B2-08: navegación, tarjeta y lista adaptable) también implementada (2026-11-04, `E6_raices`): tope en 186; la tabla no se hizo porque el sitio no tiene ninguna. Fase 2, tanda 4 (B2-10 a B2-13: subida de fotos con vista previa y pestañas, las dos con Alpine, piezas de la entidad y mapa con su lista) también implementada (2026-11-04, `E6_raices`): tope en 172; `acta` y `salidas` quedan en su hoja y el dibujo del mapa en la hoja de la pantalla. Fase 2, tanda 5 (B2-14, recortado al cuaderno de noticias del donante, con su libro de registro, más los «Cargando…» públicos como `.cargando` y el color de `.boton`) también implementada (2026-11-04, `E6_raices`): el tope sigue en 172 (no baja); `asiento`, `novedad` y `libro` quedan heredadas. Fase 3, B3-01 (la portada, en 3 subtandas): la subtanda 1 también implementada (2026-11-05, `E6_raices`): la lámina, el diálogo de la foto, los avisos, los botones propios y el tamaño del título pasan a bloques existentes y a dos tokens; tope en 171. La subtanda 2 también implementada (2026-11-05, `E6_raices`): los pasos, la planta, la rama de frutos y la libreta del mes son bloques nuevos (`.pasos`, `.planta`, `.frutos`, `.libreta`); tope en 163. La subtanda 3 también implementada (2026-11-05, `E6_raices`): la tierra, la red y la pausa son bloques (`.tierra-*`, `.red`, `.pausa`) y `publico-portada.css` solo dispone, así que salió de `HOJAS_HEREDADAS`; B3-01 queda hecho y el tope en 159. Sigue en deuda el resto de la fase 3 (B3-02 a B3-06): 159 clases de un solo uso (tope 159) y 370 clases heredadas sin componente. El ADR sucesor `proposed` [Diálogo con `popover` y fragmentos solo donde hay estructura](cliente-liviano/20261104-dialogo-con-popover-y-fragmentos-solo-donde-hay-estructura-en-el-cliente-liviano.md) corrige dos frases de D31 y D33; el usuario pidió una nota de remisión en el ADR de origen (puesta el 2026-11-04) y la aceptación la decide una persona al integrar el PR. Pico no se carga en ninguna plantilla |
| Target | `cliente-liviano` |
| Cuándo se saldará | Fase 0 del [backlog de migración](../interfaz/backlog-migracion-componentes.md) antes de cerrar E6; fases 1 a 3, incrementales, revisadas al cerrar cada entrega |

1. **Base y cerco (fase 0).** Tokens y primitivas de movimiento, `@layer`, composición y utilidades, Alpine CSP, catálogo y reglas nuevas de `InterfazFitnessTest`.
2. **Migración.** Movimiento suelto (fase 1), componentes compartidos (fase 2) y hojas de pantalla reducidas a disposición (fase 3); el tope de clases de un solo uso solo baja.

## DTI-35 — El seguimiento no tiene posición del camión y los textos del cliente todavía dicen "ubicación aproximada"
<a id="dti-35"></a>

| Campo | Valor |
|---|---|
| ADR | [ADR-E6-07 — Posición del camión: fuente, privacidad y actualización](./20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `Camion` no tiene posición; no existen `PosicionCamion` ni `FuentePosicionCamion`; `SeguimientoEntregaResponseDTO` no tiene `posicionCamion`; el cliente liviano no tiene el fragmento con polling. Las plantillas `donante-seguimiento`, `entidad-seguimiento-confirmar`, `publico-mapa`, `publico-legales` y `viaje-datos`, y las fichas `raices-seguir-entregas`, `raices-confirmar-recepcion`, `raices-publico-navegable`, `raices-legales` y `raices-ab-huecos-backend`, dicen que la ubicación es aproximada |
| Target | `logistica-service` (`Camion`, puerto y adaptador simulado, `SeguimientoEntregaResponseDTO`, `openapi-logistica.yaml`) · `cliente-liviano` (fragmento del marcador y textos de ubicación) · `docs/interfaz/flujos/` |
| Cuándo se saldará | Entrega 6: tarea 6 del [plan E6 sin auth](../requerimientos/plan-e6-trabajo-sin-auth.md) (HB-05 y HB-06) para Logística; los textos, con la sesión que conecte el cliente a Logística |

1. **Posición del camión.** Value object `PosicionCamion(latitud, longitud, registradaEn)` en `Camion`, puerto `FuentePosicionCamion` y adaptador simulado que avanza cada 30 s; descarta la posición más vieja que la guardada.
2. **Seguimiento.** Campo aditivo `posicionCamion` en `SeguimientoEntregaResponseDTO` y en el contrato OpenAPI.
3. **Cliente.** Endpoint de fragmento con el marcador y la "última actualización", pedido cada 30 s con la pestaña visible.
4. **Textos.** Las plantillas, las fichas de flujo y los legales dejan de decir "aproximada": las coordenadas son exactas y la dirección textual no se publica (D24 revisada).

## DTI-37 — Logística no recibe fotos: solo acepta URLs, sin validar tipo, tamaño ni metadatos
<a id="dti-37"></a>

| Campo | Valor |
|---|---|
| ADR | [ADR-E6-08 — Fotos de recepción en almacenamiento de objetos](./logistica-service/20261106-fotos-de-recepcion-en-almacenamiento-de-objetos.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] deferred` — `PATCH /api/entregas/{id}/fotos` y la confirmación reciben URLs sin validar; `logistica-service/pom.xml` no tiene `io.minio`; no hay bucket ni usuario de MinIO para Logística. `FotoRecepcionSubdocument.clave` ya está previsto para la clave del objeto |
| Target | `logistica-service` (puerto `AlmacenFotosRecepcion` y adaptador MinIO, validador de fotos, `EntregasController`, `openapi-logistica.yaml`, `pom.xml`) · `persistencia/minio/init-usuarios.sh` y los compose (usuario `logistica-service`) |
| Cuándo se saldará | Entrega 6: tarea 8 del [plan E6 sin auth](../requerimientos/plan-e6-trabajo-sin-auth.md) (HB-10). Coordinar los compose con A1-01 de SPEC-06 (PR #927) |

1. **Subida.** `POST /api/entregas/{id}/fotos` multipart: tope de 5 MB, JPEG o PNG por magic bytes, re-codificación sin metadatos y clave `entregas/<idEntrega>/<fotoId>.<ext>` en `logistica-fotos-recepcion`.
2. **Lectura.** `GET /api/entregas/{id}/fotos/{fotoId}` con `302` hacia una URL firmada de 5 minutos.
3. **Infraestructura.** `io.minio:minio:8.5.17`, usuario `logistica-service` con política `logistica-*` y endpoint público para firmar.
4. **Huérfanas.** Regla de ciclo de vida para fotos de entregas que nunca se confirman.

---
