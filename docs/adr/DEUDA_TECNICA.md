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
| Implementation status | `[INFERRED] deferred` — requiere el servicio del Key Broker (desde el 2026-11-04, aparte de `auth-service`, que es Keycloak) |
| Target | notificaciones-service · auth-service (prioridad alta) |
| Cuándo se saldará | **Entrega 7: Observabilidad y Seguridad** — diferido desde E6 por la decisión D29 de [requerimientos-e6](../requerimientos/requerimientos-e6.md): E6 construye el IdP y el gateway, y el Key Broker queda para el hito de Seguridad; se cancelará en simultáneo con la construcción de `auth-service`, la emisión de `ClaveUsuarioDestruidaEvent` en RabbitMQ y la adopción de `RemoteAuthKeyBrokerClient`. Desde el [ADR del proveedor de identidad](./auth-service/20261104-proveedor-de-identidad-keycloak.md) (`proposed`, 2026-11-04) el IdP es Keycloak, así que el Key Broker será un **servicio aparte** y no parte de `auth-service` |

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
| Cuándo se saldará | **Entrega 6: Interfaz de Usuario y Arquitectura MVC (13/11/2026, según el [mapa de entregas](../entregas/README.md), que la toma del borrador de calendario de la cátedra)**, que incluye el servicio de autenticación — integración con `auth-service`, protección perimetral con roles (`ROLE_ADMIN`), traslado a `/api/admin/` y respuesta `202 Accepted` asíncrona. Con el IdP en Keycloak, la protección por rol llega con la validación del token en los servicios (E2-01 de la [SPEC-06](../specs/active/SPEC-06-auth-mvp-keycloak.md)) · Nombre y fecha corregidos el 2026-11-06 (§13 de [requerimientos-e6](../requerimientos/requerimientos-e6.md)): decían "Entrega 6: Despliegue, Observabilidad y Seguridad (Semana del 23 de Noviembre 2026)", sin respaldo en el enunciado ni en el repo. Si el equipo pasa una parte a E7 (Observabilidad y Seguridad), se reprograma acá |

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
| Cuándo se saldará | (1) y (2) ya saldados con el perfil `postgres` (persistencia JPA de `donaciones-service`), siguen en memoria solo con `!postgres`; (3) y (4) con el `auth-service` (**Entrega 6: Interfaz de Usuario y Arquitectura MVC, 13/11/2026**, según el [mapa de entregas](../entregas/README.md), tomada del borrador de calendario de la cátedra; antes decía "semana del 23 de noviembre", sin respaldo; con Keycloak, en E2-07 de la [SPEC-06](../specs/active/SPEC-06-auth-mvp-keycloak.md): client credentials y retiro de `X-API-Key`); (5) y (6) al integrar un proveedor de logística real |

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
7. **Traza AMQP solo de ida.** Donaciones envía `X-Trace-Id` en sus publicaciones (`DonacionesEventPublisher`, `ProveedorLogisticaAmqp`) y en el pedido HTTP (`ProveedorLogisticaHttp`). `[OBSERVED] partial` desde HB-19 (2026-11-09): Logística envía `X-Trace-Id` en sus cinco eventos de vuelta (`LogisticaEventPublisher.identidad()`, si hay un traceId en el MDC) y lo restaura en el MDC al consumir la cancelación (`EntregaSolicitadaEventListener.onEntregaCancelada`). Falta: restaurarlo en los demás listeners (el pedido de entrega en Logística, todos los de Donaciones y Notificaciones), e Incentivos no lo envía. Contradice `docs/arquitectura/eventos-amqp.md` (restauración en el consumidor) y deja a medias el ADR de trazabilidad distribuida.
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
| ADR | [20261102-contrato-de-entregas-para-la-entidad-remito-reporte-y-datos-de-donaciones](./20261102-contrato-de-entregas-para-la-entidad-remito-reporte-y-datos-de-donaciones.md) y su ajuste [20261108-entregas-con-problema-fotos-privadas-y-cantidad-recibida](./20261108-entregas-con-problema-fotos-privadas-y-cantidad-recibida.md) |
| Decision status | `proposed` (los dos) |
| Implementation status | `[OBSERVED] partial` (HB-17, 2026-11-08) — ítems 1 a 3 implementados: `numeroRemito` y `reporte` en la confirmación y en `EntregaResponseDTO` (Flyway V7 de Logística, también en Mongo), `reporte` en `entrega.exitosa`, `necesidadId` en `DonacionIndependienteResponseDTO`, y Donaciones cuenta lo recibido (Flyway V14). Excluir de las destacadas las entregas con reporte (HB-01): implementado en el PR #954, la pertenencia (HB-13) y conectar la pantalla |
| Target | `logistica-service` (`Entrega`, DTOs de entregas, `entrega.exitosa`) · `donaciones-service` (`DonacionIndependienteResponseDTO`) · `openapi-logistica.yaml`, `openapi-donaciones.yaml` y `schemas/evento-entrega-exitosa.schema.json` |
| Cuándo se saldará | Cuando `cliente-liviano` consuma Logística para Confirmar recepción (RF-12). Pide antes HB-13 (pertenencia) |

1. **Remito.** `numeroRemito` en la confirmación (`ENTREGADA`) y en `EntregaResponseDTO`; fuera de las respuestas públicas y de los eventos.
2. **Reporte.** `reporte` opcional en la confirmación (`motivo`, `detalle`, `cantidadRecibida`), guardado en la entrega y como campo opcional de `entrega.exitosa`. Decidido el 2026-11-08 ([ajuste](./20261108-entregas-con-problema-fotos-privadas-y-cantidad-recibida.md), `proposed`): la necesidad cuenta lo que llegó.
3. **`necesidadId`** como campo opcional de `DonacionIndependienteResponseDTO`.
4. **Pantalla "Llegó con un problema":** diseñada y dibujada en el prototipo (2026-11-07,
   [ficha](../interfaz/flujos/raices-llego-con-un-problema.md)). Por decisión del usuario, las fotos
   de una entrega con reporte no se publican en la portada ni en el mapa; el ADR dice que la foto
   llega igual a la portada. Ajustado en el [ADR de entregas con problema](./20261108-entregas-con-problema-fotos-privadas-y-cantidad-recibida.md)
   (`proposed`, 2026-11-08); lo implementa HB-01.

---

## DTI-30 — La necesidad no expone estado, versión, unidad, fecha límite ni historia de períodos
<a id="dti-30"></a>

| Campo | Valor |
|---|---|
| ADR | [20261102-ciclo-de-vida-de-la-necesidad-en-el-contrato](./donaciones-service/20261102-ciclo-de-vida-de-la-necesidad-en-el-contrato.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] implemented` en el [PR #957](https://github.com/tsorren/DonaTrack-TP-DDS/pull/957) (HB-18): los siete puntos, Flyway V17 y OpenAPI de Donaciones 1.5.0, con la migración del contrato del ajuste del 2026-11-09 del ADR. Queda abierto: lo recibido no descuenta la cantidad no recibida hasta que se integre HB-17 (PR #951) |
| Target | `donaciones-service` (`NecesidadDTO`, `NecesidadesService`, `NecesidadRecurrente`, `Subcategoria`, consulta de lo recibido) · `openapi-donaciones.yaml` · `cambios-incompatibles-aceptados.txt` |
| Cuándo se saldará | Con el merge de HB-18 a `ENTREGA_6`; el descuento de lo no recibido, al integrar #951. Los consumidores del repo (`integration-tests` y Postman) se migraron en el mismo PR |

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
| ADR complementario | [20261007-broker-de-integracion-con-logistica](./20261007-broker-de-integracion-con-logistica.md) · cómo se implementa: [20261109-cancelacion-de-entregas-que-no-salieron](./20261109-cancelacion-de-entregas-que-no-salieron.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] partial` — HB-19 ([PR #958](https://github.com/tsorren/DonaTrack-TP-DDS/pull/958)) implementa los ítems 1 a 5. Queda pendiente lo de abajo |
| Target | `donaciones-service` (baja, broker, procesador de eventos de vuelta) · `logistica-service` (`Entrega.cancelar`, listener del comando, salida de la ruta) · `schemas/` (comando `entrega.cancelada.<proveedorId>.v1` y evento `entrega.cancelada`) |
| Cuándo se saldará | Antes de habilitar la baja de necesidades en el cliente con datos reales |

1. **Comando** `entrega.cancelada.<proveedorId>.v1` por el outbox del broker, y marca de "cancelación pedida" en la donación.
2. **`Entrega.cancelar`** solo desde `PENDIENTE`, estado `CANCELADA`, salida de la ruta sin iniciar y evento firmado `entrega.cancelada`. **Incompatible** (valor nuevo del enum).
3. **Vuelta** a `EN_DEPOSITO` sin destino al recibir el evento.
4. **Guarda `NECESIDAD_INACTIVA`** en la asignación por `PATCH`.
5. **Proveedores HTTP** sin cancelación: la entrega sigue y se loguea.

Pendiente después de HB-19 (ADR 20261109, "Trabajo futuro" y "Consecuencias negativas"):

- Si la confirmación `entrega.cancelada` se pierde (se publica después del commit, sin outbox), la donación queda con `cancelacionPedida` hasta que se repite la baja.
- Si el proveedor no tiene la entrega (el pedido se descartó por inválido) no confirma, y si el pedido termina `FALLIDA` después de la baja la cancelación se descarta con un log `error`: en los dos casos la donación queda asignada y con la marca hasta una revisión manual, como cualquier solicitud `FALLIDA` del broker.
- El seguimiento por entidad de Logística devuelve también las entregas `CANCELADA` (historia); la pantalla decide si las muestra.
- Si el pedido todavía no salió y RabbitMQ lo devuelve, el broker puede pasarlo a un proveedor HTTP después de la baja: la cancelación termina `FALLIDO` por error de contrato y la marca queda hasta que la entrega sale (`ruta.iniciada`).
- El ADR `proposed` [referencias a donaciones sin clave foránea](./logistica-service/20261007-referencias-a-donaciones-sin-clave-foranea.md) sigue describiendo `UNIQUE (id_donacion)`; el ajuste a una entrega activa por donación está en el ADR 20261109 (no se edita el ADR anterior).
- Una ruta que queda sin paradas sigue `PENDIENTE` y no se puede iniciar; camión y chofer no se liberan.
- No hay aviso al donante de que su donación va a otra entidad.
- Una base Mongo persistente conserva el índice único viejo `idDonacion` de `entregas`: hay que borrarlo a mano.

---

## DTI-32 — No hay preferencia para ocultar el nombre del donante ni constancia de los legales
<a id="dti-32"></a>

| Campo | Valor |
|---|---|
| ADR | [20261102-nombre-visible-del-donante-y-constancia-de-los-legales](./20261102-nombre-visible-del-donante-y-constancia-de-los-legales.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] partial` (HB-20, 2026-11-10, [PR #960](https://github.com/tsorren/DonaTrack-TP-DDS/pull/960), Donaciones V19 y OpenAPI 1.7.0) — hechos los ítems 1, 2, 3 (ajustado) y 5; pendientes el 4 (texto) y el 6 (categoría, del lado del cliente). Cliente liviano conectado el 2026-11-10 (corrida `conexion-pantallas`, [SPEC-08](../specs/active/SPEC-08-conexion-de-pantallas.md), `[OBSERVED]` en las ramas `E6_conexion_*`, sin mergear): el registro anota la constancia con `PUT /api/personas/{id}/legales`, Mi perfil del donante cambia el nombre visible con `PATCH /api/donantes/{id}/preferencias`, y Recibidas y Confirmar recepción leen `quienDono` en lugar de `/api/donaciones/{id}`. Sigue sin cliente la categoría con el nombre oculto (ítem 6) |
| Target | `donaciones-service` (`Donante`, `QuienDonoDTO`, `PATCH /api/donantes/{id}/preferencias`, `AceptacionLegales`, `GET` y `PUT /api/personas/{id}/legales`) · `openapi-donaciones.yaml` · `cliente-liviano` (registro, Mi perfil, Recibidas). Desde el ajuste A2 (2026-11-10) no toca `POST /api/personas` ni `cambios-incompatibles-aceptados.txt`; `GET /api/personas/me` queda con HB-11 |
| Cuándo se saldará | Con el merge de los PRs de la conexión de pantallas (por rol) y el registro y el perfil con login real (RF-03, RI-AUTH-02, HB-11) |

1. **`mostrarNombre`** en `Donante` y su endpoint de preferencias. **Hecho** (`PATCH /api/donantes/{id}/preferencias`).
2. **`QuienDonoDTO`** en las respuestas que lee la entidad, en lugar de `DonanteResumenDTO`. **Hecho** como `quienDono` en `DonacionIndependienteResponseDTO`, el listado que lee la entidad; `DonanteResumenDTO` no lo leía la entidad.
3. **`AceptacionLegales`** (persona, versión y fecha) y `legalesAceptados` obligatorio en el alta. **Hecho con ajuste**: la constancia va en `GET` y `PUT /api/personas/{id}/legales`, y el alta no la exige (aditivo). El sitio la pide al completar el perfil. Riesgo abierto: la constancia es de la persona (la organización), no de cada miembro invitado.
4. **Texto de los legales:** lo escribe el equipo.
5. **`mostrarNombre` en la donación** (opcional; solo oculta) para "Esta vez, sin mi nombre" al registrarla (review de Sofía, 2026-11-02). **Hecho** en `DonacionInputDTO` (columna `donacion.nombre_oculto`).
6. **Categoría del ranking** con el nombre oculto (en el prototipo, dato fijo de la maqueta; `cliente-liviano` no tiene todavía un cliente de Incentivos): el cliente liviano la pide a Incentivos (`GET /api/incentivos/donantes/{donanteId}`, `DonantePerfilDTO.categoria`) con el `donanteId`, que `QuienDonoDTO` suma también con el nombre oculto; el cliente liviano lo usa en el servidor y a la entidad le muestra solo la categoría, nunca el id. Falta decidir con qué credencial lo pide el cliente liviano (`[INFERRED]` el perfil de incentivos es del propio donante, no de la entidad).

Complementa el ADR aceptado de [privacidad de usuarios](./20260519-privacidad-de-usuarios.md): ocultar el nombre no reemplaza la anonimización.

---

## DTI-33 — El formulario de contacto no tiene endpoint ni bandeja
<a id="dti-33"></a>

| Campo | Valor |
|---|---|
| ADR | [20261110-recepcion-de-mensajes-de-contacto-en-notificaciones](./notificaciones-service/20261110-recepcion-de-mensajes-de-contacto-en-notificaciones.md) (HB-21) |
| Decision status | `proposed` |
| Decidido por | Usuario (2026-11-02, pregunta 17 del artifact de Raíces): formulario dentro de DonaTrack. Usuario (2026-11-10, HB-21): Notificaciones como dueño, sobre la matriz del ADR |
| Implementation status | `[OBSERVED] partial` (2026-11-10, rama `E6_HB21_contacto`) — **hecho:** `notificaciones-service` recibe `POST /api/contacto` (tabla `mensaje_contacto` con `clave_envio` UNIQUE en `V4__mensajes_de_contacto.sql`, número de una identidad, límite contado en la tabla y en configuración, `429` con `ERR-EST-903`, aviso a la persona Administración), lista y lee (`GET /api/contacto`, `GET /api/contacto/{numero}`) y responde por correo con `NotificacionRouter` (`RESPONDIDO` o `ENVIO_FALLIDO`); OpenAPI 1.1.0 y ruta pública `POST /api/contacto` en el gateway. Desde 2026-11-11 (E2-01, PR 962) el envío es la ruta pública de Notificaciones y leer y responder piden `ADMIN` cuando `NOTIFICACIONES_SEGURIDAD_HABILITADA=true` (apagada por defecto, M-07 de la matriz). **Pendiente:** (a) con sesión, tomar la persona del token e ignorar el `correo` del cuerpo, como pide el ADR; (b) la retención: borrar la IP a las 24 horas y anonimizar correo y texto a los 180 días de cerrados (sin tarea programada todavía); (c) el cifrado del correo y el texto (crypto-shredding, ADR `proposed`), que Notificaciones tampoco aplica a la réplica de personas; (d) el asunto del correo (`CorreoAdapter.enviarMail` no lo recibe); (e) la pantalla del admin. Lo del cliente liviano (el campo trampa `sitio` y la clave de envío en un campo oculto, que se repite si se reenvía el formulario) está hecho en la rama `E6_conexion_publico` (conexión de pantallas, SPEC-08); `codigoError` se valida con el formato de los códigos de los avisos (12 a 32 caracteres hexadecimales) |
| Target | `notificaciones-service` (`POST /api/contacto`, `GET /api/contacto`, `GET /api/contacto/{numero}`, `POST /api/contacto/{numero}/respuesta`, tabla con Flyway, OpenAPI) · `api-gateway` (ruta pública) · `cliente-liviano` (clave de envío, campo trampa e IP del navegador) |
| Cuándo se saldará | Antes de publicar el sitio. Hasta entonces, la pantalla es maqueta |

1. **Dueño.** Decidido: Notificaciones recibe `POST /api/contacto` (tema, texto, código de error opcional, correo si no hay sesión y clave de envío) y responde con el número del mensaje. El administrador recibe un aviso en su bandeja (persona Administración de id fijo) y lee los mensajes en `GET /api/contacto`, solo con `ADMIN` desde HB-13.
2. **Abuso y repetidos.** Decidido: una clave de envío única por formulario devuelve el mismo número si se reenvía (el caso "sin conexión"); un límite contado en la tabla (5 por hora por correo y 20 por día por IP, configurables) responde `429`; campo trampa y largo máximo. El límite en el gateway y el CAPTCHA quedan descartados.
3. **Respuesta.** Decidido: `POST /api/contacto/{numero}/respuesta` la manda por correo con `NotificacionRouter` y "Respuesta a tu mensaje Nº …"; estados `RECIBIDO`, `RESPONDIDO` y `ENVIO_FALLIDO`. Sin acuse de recibo por correo.
4. **Pendiente del grupo.** Confirmar los 180 días de retención de los mensajes de personas sin cuenta.

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

## DTI-35 — El cliente no muestra la posición del camión y sus textos todavía dicen "ubicación aproximada"
<a id="dti-35"></a>

| Campo | Valor |
|---|---|
| ADR | [ADR-E6-07 — Posición del camión: fuente, privacidad y actualización](./20261106-posicion-del-camion-fuente-privacidad-y-actualizacion.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] partial` — **Logística (puntos 1 y 2), hecho el 2026-11-08 (HB-05 y HB-06):** `PosicionCamion` en `Camion` con `registrarPosicion` idempotente, puerto `FuentePosicionCamion` con `FuentePosicionCamionSimulada` y `ActualizadorPosicionCamiones` cada 30 s sobre las rutas `EN_TRASLADO` (el ADR las llama `EN_CURSO`), persistencia en Mongo, JPA (`V6__camion_posicion.sql`) y memoria, y `posicionCamion` en `SeguimientoEntregaResponseDTO` y en `openapi-logistica.yaml` 1.3.0. **Cliente, avance del 2026-11-10 (corrida `conexion-pantallas`, `[OBSERVED]`):** `donante-seguimiento` lee `posicionCamion` de Logística y muestra la última actualización. **Pendiente:** el cliente liviano no tiene el fragmento con polling (punto 3) y las plantillas `donante-seguimiento`, `entidad-seguimiento-confirmar`, `publico-mapa`, `publico-legales` y `viaje-datos`, y las fichas `raices-seguir-entregas`, `raices-confirmar-recepcion`, `raices-publico-navegable`, `raices-legales` y `raices-ab-huecos-backend`, dicen que la ubicación es aproximada |
| Target | `logistica-service` (`Camion`, puerto y adaptador simulado, `SeguimientoEntregaResponseDTO`, `openapi-logistica.yaml`) · `cliente-liviano` (fragmento del marcador y textos de ubicación) · `docs/interfaz/flujos/` |
| Cuándo se saldará | Entrega 6: la parte de Logística quedó hecha con la tarea 6 del [plan E6 sin auth](../requerimientos/plan-e6-trabajo-sin-auth.md) (HB-05 y HB-06, 2026-11-08); el fragmento y los textos, con la sesión que conecte el cliente a Logística |

1. **Posición del camión.** Value object `PosicionCamion(latitud, longitud, registradaEn)` en `Camion`, puerto `FuentePosicionCamion` y adaptador simulado que avanza cada 30 s; descarta la posición más vieja que la guardada.
2. **Seguimiento.** Campo aditivo `posicionCamion` en `SeguimientoEntregaResponseDTO` y en el contrato OpenAPI.
3. **Cliente.** Endpoint de fragmento con el marcador y la "última actualización", pedido cada 30 s con la pestaña visible.
4. **Textos.** Las plantillas, las fichas de flujo y los legales dejan de decir "aproximada": las coordenadas son exactas y la dirección textual no se publica (D24 revisada).

## DTI-36 — Las fotos de recepción no llegan a Donaciones y la entrega no sabe quién donó
<a id="dti-36"></a>

| Campo | Valor |
|---|---|
| ADR | [Réplicas por evento: fotos de recepción en Donaciones y donante en la entrega](./20261106-replicas-por-evento-fotos-de-recepcion-y-donante-de-la-entrega.md) |
| Decision status | `proposed` |
| Implementation status | `[OBSERVED] partial` — los schemas de `entrega.exitosa` y `entrega.solicitada.v1` ya tienen los campos opcionales (contrato primero, 2026-11-06). **Donante (punto 2), hecho el 2026-11-08 (HB-16):** `EventoEntregaSolicitadaV1` de los dos servicios, `DatosEntregaLogistica`, el pedido HTTP, `Entrega`, `EntregaDocument`, `EntregaEntity` (`V5__entrega_donante_id.sql`) y `CrearEntregaRequestDTO` llevan `donanteId`, y `GET /api/entregas/seguimiento/donantes/{donanteId}` lista las activas. **Fotos (punto 1), hecho el 2026-11-08 (HB-01, PR #954):** `EntregaConfirmada` y `EventoEntregaExitosa` de Logística llevan `fotosRecepcion`; Donaciones las guarda en la donación (V15) y sirve `GET /api/donaciones/destacadas` |
| Target | `logistica-service` (`EventoEntregaExitosa`, `EventoEntregaSolicitadaV1`, `Entrega`, `EntregaDocument`, `CrearEntregaRequestDTO`, `openapi-logistica.yaml`) · `donaciones-service` (los dos records, `ProveedorLogisticaAmqp`, `ProcesadorEventosLogistica`, la donación independiente) |
| Cuándo se saldará | Entrega 6: tarea 7 (HB-01) del [plan E6 sin auth](../requerimientos/plan-e6-trabajo-sin-auth.md), la que queda; la URL estable de las fotos depende de ADR-E6-08 (HB-10). La tarea 5 (HB-16) quedó hecha el 2026-11-08 |

1. **Fotos (pendiente).** Logística publica `fotosRecepcion` en `entrega.exitosa`; Donaciones las guarda al pasar a `ENTREGADA` y su `EventoEntregaExitosa` declara `@JsonIgnoreProperties(ignoreUnknown = true)`.
2. **Donante (hecho, 2026-11-08).** El broker manda `donanteId` (de `Donacion.donanteId`); Logística lo guarda en `Entrega` y en `EntregaDocument` con índice, y lo acepta también en `POST /api/entregas`.
3. **Datos viejos.** Sin backfill: las entregas y donaciones previas no aparecen en el listado por donante ni en destacadas.

## DTI-37 — Logística no recibe fotos: solo acepta URLs, sin validar tipo, tamaño ni metadatos
<a id="dti-37"></a>

| Campo | Valor |
|---|---|
| ADR | [ADR-E6-08 — Fotos de recepción en almacenamiento de objetos](./logistica-service/20261106-fotos-de-recepcion-en-almacenamiento-de-objetos.md) |
| Decision status | `proposed` |
| Implementation status | `[VERIFIED] partial` (HB-10, PR #952, 2026-11-08) — ítems 1 a 3 hechos: `FotosRecepcionController` (`POST` multipart y `GET` con `302`), `LimpiadorFotoRecepcion` (5 MB, magic bytes, re-codificación con `ImageIO`), puerto `AlmacenFotosRecepcion` con `MinioAlmacenFotosRecepcion` y `AlmacenFotosEnMemoria`, `io.minio:minio:8.5.17`, usuario `logistica-service` en `init-usuarios.sh` y los dos compose. Tests: `LimpiadorFotoRecepcionTest`, `FotosRecepcionControllerTest` y `MinioAlmacenFotosRecepcionIntegracionTest` (MinIO por Testcontainers). Faltan los ítems 4 (huérfanas) y 5 (orientación). **Para validar por un humano, fuera del ADR:** (a) tope de 24 megapíxeles leído del encabezado antes de decodificar, con `413` (un PNG chico en bytes puede ocupar gigas en memoria y Logística corre con `-Xmx384m`); (b) subir a una entrega `ENTREGADA` da `409`; (c) el `GET` busca la clave con `.jpg` y después con `.png` (`statObject`), sin guardar nada en la entrega, así que no hay migración; (d) la URL estable se arma sobre `logistica.fotos.url-publica-base` (el gateway, `http://localhost:8086` en el compose; en preprod, que no tiene gateway, `http://logistica-service:8083`); (e) la entrega no se toca al subir: guarda la URL estable que llega en `fotosRecepcion` al confirmar (en Mongo, en el campo `clave` de `FotoRecepcionSubdocument`, como cualquier URL), no la clave del objeto. Queda para F5 la ruta `GET /api/entregas/*/fotos/*` en la allowlist del gateway: hoy la allowlist solo admite rutas literales |
| Target | `logistica-service` (puerto `AlmacenFotosRecepcion` y adaptador MinIO, validador de fotos, `FotosRecepcionController`, `openapi-logistica.yaml`, `pom.xml`) · `persistencia/minio/init-usuarios.sh` y los compose (usuario `logistica-service`) |
| Cuándo se saldará | Entrega 6: tarea 8 del [plan E6 sin auth](../requerimientos/plan-e6-trabajo-sin-auth.md) (HB-10). Coordinar los compose con A1-01 de SPEC-06 (PR #927) |

1. **Subida.** `POST /api/entregas/{id}/fotos` multipart: tope de 5 MB, JPEG o PNG por magic bytes, re-codificación sin metadatos y clave `entregas/<idEntrega>/<fotoId>.<ext>` en `logistica-fotos-recepcion`.
2. **Lectura.** `GET /api/entregas/{id}/fotos/{fotoId}` con `302` hacia una URL firmada de 5 minutos.
3. **Infraestructura.** `io.minio:minio:8.5.17`, usuario `logistica-service` con política `logistica-*` y endpoint público para firmar.
4. **Huérfanas.** Regla de ciclo de vida para fotos de entregas que nunca se confirman.
5. **Orientación.** Al re-codificar se pierde el tag EXIF `Orientation`: una foto vertical de celular guardada como píxeles apaisados (Orientation 6 u 8) se muestra girada. Falta leer el tag y rotar los píxeles antes de codificar. `[INFERRED]` de la auditoría de cierre de HB-10; sin test todavía.

---

---

## DTI-38 — Lo que deja abierto el cierre de E6: `EN_PUERTA`, avisos sin enlace y token del cliente
<a id="dti-38"></a>

| Campo | Valor |
|---|---|
| ADR | [Estado `EN_PUERTA` de la entrega](./logistica-service/20261111-estado-en-puerta-de-la-entrega.md) (HB-22) |
| Decision status | `proposed` |
| Decidido por | Usuario (2026-11-11): estado nuevo contra la recomendación de la matriz ([SPEC-09](../specs/active/SPEC-09-cierre-e6-demo-punta-a-punta.md) §3.1) |
| Implementation status | `[OBSERVED] partial` (2026-11-11, rama `E6_cierre_control`) — **hecho:** `EN_PUERTA` en Logística (V9, OpenAPI 1.9.0) y en Donaciones (V20, OpenAPI 1.8.0), `tipoEvento` y `referenciaId` en Notificaciones (V5, OpenAPI 1.2.0), `donacionIndependienteId` en los eventos de Donaciones y el token del usuario hacia las cuatro APIs. **Falta:** los ítems de abajo |
| Target | `logistica-service` · `donaciones-service` · `notificaciones-service` · `cliente-liviano` (`ClientesHttp`) · `integration-tests` y `postman` (del grupo, PR 969) |
| Cuándo se saldará | Antes de publicar el sitio; el ítem 3 con el Gate 3 del grupo |

1. **Quién marca `EN_PUERTA`.** El `PATCH` a `EN_PUERTA` pasa por el permiso `CONFIRMAR_SU_ENTREGA`: la entidad podría marcar su propia entrega en la puerta. El cliente no lo hace (consecuencias del ADR).
2. **Proveedores externos.** Un proveedor que no publica `entrega.en-puerta` (n8n) deja sus entregas en "En camino"; se confirman igual desde `EN_TRASLADO`. Una carrera entre `ruta.iniciada` y `entrega.en-puerta` (colas distintas) deja la donación "En camino" hasta la confirmación.
3. **Pruebas de integración y Postman.** `LogisticaIntegrationIT`, `FullDistributedDonationE2EIT` y los flujos de `postman/` esperan un historial exacto sin `EN_PUERTA`: con la simulación de posición (30 s por tramo) puede aparecer en el medio. Fuera de la zona del cierre (PR 969); opciones: `logistica.posicion.simulada.habilitada=false` en el Gate 3 o aceptar `EN_PUERTA` opcional. Gate 3 `[DEFERRED_NO_DOCKER]`.
4. **Mongo persistente.** Una base que ya tenía el índice `uq_entrega_donacion_activa` lo conserva (redundante con el nuevo); se borra a mano.
5. **Diagramas.** Los `.puml` y el Lucidchart de clases de Donaciones y Logística no dibujan `EnPuerta`, `Entrega.llegarAPuerta` ni `entregasAlcanzadas`; `docs/generated/contracts-summary.json` no se regeneró.
6. **Avisos.** El aviso a la entidad "llegó a tu puerta" no existe (la maqueta tiene el tema "En tu puerta"). Las notificaciones anteriores a la V5 y las de un emisor que no manda el id quedan en "Otras novedades" o sin enlace al detalle.
7. **Token del cliente.** El cliente no distingue un endpoint público de uno privado: sin sesión llama sin token y deja que la API conteste 401 o 200 (`ErroresHttp` convierte el 401 en "Ingresar").
8. **Pantallas de F2.** El Mapa muestra lugares y no entidades (Logística no dice qué entidad recibió y Donaciones no da el lugar de las entregas: falta un id de entrega o de donación en el DTO público); `MisionDTO` no trae la unidad del avance ni la donación que completó cada misión; Misiones no está en el menú de la persona fuera de maqueta (decidir de dónde sale que la cuenta es `HUMANA` sin una llamada por pantalla) y su escena de huerta a 360 px tiene enlaces de 22 a 45 px (escena de 46 rem con desplazamiento propio o lista de las 8 macetas: decisión de diseño).

---

## DTI-39 — Listas del cliente liviano que todavía no se piden de a una página (R11-01)
<a id="dti-39"></a>

| Campo | Valor |
|---|---|
| ADR | [Listados de Donaciones siempre paginados](./donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md) (`proposed`, escrito después del código en el cierre documental de P0); implementa R11-01 del [backlog de la revisión del 2026-11-11](../interfaz/backlog-revision-2026-11-11.md); contrato en la [spec `R11-p0-api-listados`](../specs/completed/R11-p0-api-listados-spec.md) |
| Decision status | `proposed` |
| Decidido por | Usuario (2026-11-11, ajustes al aprobar R11 P0 en la [ficha del admin](../interfaz/flujos/raices-admin-navegable.md)): ninguna lista llega completa; cada una se pide de a una página o con un tope |
| Implementation status | `[OBSERVED] partial` (2026-11-11, lote `p0-admin-listados`) — **hecho:** Donantes y Donaciones del admin de a una página con el paginador provisorio, la búsqueda del registro (hasta 10, con cuántos coinciden), las cifras del inicio desde `GET /api/donaciones/resumen`, las personas de las solicitudes por `id` (también los nombres de las que piden sumarse, en el inicio y en Entidades), los nombres de las entidades de las donaciones del donante recorriendo las páginas de las públicas con tope, Entidades del admin de a una página de 20 con la búsqueda por nombre en el servidor y la cifra de activas como número, y el avance de la importación sin la lista. En los arreglos del cierre de P0 (2026-11-12), la búsqueda de Entidades del donante queda en Donaciones, sin tildes: la pantalla ya no vuelve a filtrar la página por nombre y el paginador conserva la búsqueda. **Falta:** los ítems de abajo |
| Target | `cliente-liviano` · `donaciones-service` |
| Cuándo se saldará | En esta corrida. Ítem 1: las pantallas del donante con R11-13 y R11-12 (lote `p1-donante-listas`); las de la entidad, con el lote `p2-listas-restantes` (R11-43). Ítem 2: lote `p2-listas-restantes`. Los paginadores provisorios pasan al definitivo con R11-11 (lote `p1-paginador`) — `[OBSERVED]` hecho (2026-11-12, cierre documental de P1): las listas del admin pasan por el pase `paginador.html` al componente `paginas`, y Entidades y Mis donaciones del donante lo usan directo (`p1-donante-listas`); el pase se borra cuando ninguna plantilla lo nombre ([DTI-42](#dti-42), ítem 3). Ítems 3 y 4 (cierre documental de P0, 2026-11-12, decisión del agente: el usuario pidió que todo lo pedido se haga en esta corrida, así que ningún ítem queda para "un lote propio" sin nombre): el filtro y los órdenes por vencimiento, "Por nombre", "Por donante", "Qué dona" y "Sin activar su cuenta", con el lote `p2-admin-bandeja` (R11-34), que tiene que sumar `donaciones-service` a sus archivos porque cambia el contrato de `/donaciones-independientes` y `/api/donantes`; la descarga de las filas con error, con el lote `p1-importaciones` (R11-20) — `[OBSERVED]` hecha (2026-11-12): `GET /admin-donantes-importado/filas-con-error.csv`, de a tandas de 100. Lo que falta del lado de los servicios y de las otras pantallas está en [DTI-40](#dti-40) |

1. **Una página de 100 en lugar de una lista.** `DonacionesApi.donaciones(donanteId)` y `asignadas(entidadId)` piden una página de 100 (el tope del servicio) porque desde R11-01 el servicio manda 20 si no se pide página. Las usan Mis donaciones y Entidades del donante, su inicio y su seguimiento (lote `p1-donante-listas`), y Recibidas, Confirmar recepción, Necesidades y Notificaciones de la entidad (lote `p2-listas-restantes`). Con más de 100 donaciones, esas pantallas no ven las más viejas. Cada una tiene que pedir su página con su paginador (las listas) o un número del servicio (las cifras y los cruces). Los nombres de las entidades de esas donaciones se buscan en `GET /api/entidades/publicas` de a 100, hasta 10 páginas (mil entidades activas): más allá, la donación queda con su destino y sin nombre, hasta que Donaciones lea las públicas por `id`. `[OBSERVED]` Saldado para Entidades y Mis donaciones del donante (lote `p1-donante-listas`, 2026-11-12): Entidades pide `GET /api/donantes/{id}/entidades` de a 6, con la búsqueda, la categoría y "Ya doné" resueltas en Donaciones y la marca `yaLeDono` de cada una, sin leer las donaciones; Mis donaciones, de a 12 con el componente `paginas` y la subcategoría. Seguimiento del donante pide de una vez solo los estados en curso (`estado` repetible, una página de 100): con más de 100 en curso, no ve las más viejas. Falta: el inicio del donante (lote `p2-tablero`) y las pantallas de la entidad (`p2-listas-restantes`).
2. **Filtros de Entidades del admin.** `admin-entidades` ya va de a una página de 20 con su paginador y la búsqueda por nombre en Donaciones, pero `GET /api/entidades` no filtra por estado (activa o de baja), por necesidades vigentes ni por tipo: fuera de la maqueta, esos tres filtros no se ofrecen. Las que piden sumarse salen primero, en la página 1. Las necesidades vigentes de la página se leen con una sola llamada (`GET /api/necesidades?estado=VIGENTE`), sin página: `entidadId` no se repite.
3. **Vencen pronto.** `GET /donaciones-independientes` no filtra ni ordena por vencimiento: en Donaciones del admin, "Vencen en los próximos 7 días" muestra las del depósito con cuántas vencen (del resumen) y el filtro no se ofrece. Los órdenes "Vencen antes", "Por nombre" y "Por donante" tampoco se ofrecen.
4. **Donantes.** Donaciones no guarda "sin activar su cuenta" ni qué dona cada donante en la fila: esos filtros y la columna "Qué dona" no se ofrecen fuera de la maqueta. Las filas con error de una importación se ven de a 20; no hay descarga.

---

## DTI-40 — Paginado y rendimiento: lo que deja abierto la oleada P0 de R11
<a id="dti-40"></a>

| Campo | Valor |
|---|---|
| ADR | [Listados de Donaciones siempre paginados](./donaciones-service/20261111-listados-paginados-de-donaciones-sin-lista-completa.md), [búsqueda sin tildes](./donaciones-service/20261111-busqueda-sin-tildes-con-unaccent-en-el-esquema-del-servicio.md), [SQL nativo y lectura por lote](./donaciones-service/20261111-consultas-de-listado-en-sql-nativo-y-lectura-por-lote.md) y [cifras del admin como conteos](./donaciones-service/20261111-cifras-del-admin-como-conteos-en-un-resumen.md) (todos `proposed`) |
| Decision status | `proposed` |
| Decidido por | Usuario (2026-11-11): regla 1 del [backlog](../interfaz/backlog-revision-2026-11-11.md#2-reglas-generales-nuevas) y punto 4 del pedido de la corrida ("que todas las listas tengan algún método de paginación o loteo"). La asignación de cada ítem a un lote es del agente (cierre documental de P0, 2026-11-12) |
| Implementation status | `[OBSERVED] partial` (2026-11-12, rama `E6_backlog_r11_p0`) — **hecho:** los cuatro listados de Donaciones, `/api/entidades` y `/api/entidades/publicas` paginan, buscan sin tildes y cuentan en la base; el resumen del admin y los contadores públicos son conteos; los mappers leen por lote (`b3f8fc179`). El ensayo de 20.000 filas dio 125 a 142 s, p95 del sitio menor a 0,6 s y Donaciones por debajo de 420 MiB ([guía](../testing/ensayo-importacion-20000.md#resultados-del-cierre-de-p0-2026-11-12)). **Falta:** los ítems de abajo |
| Target | `donaciones-service` · `cliente-liviano` · `logistica-service` · `notificaciones-service` · `incentivos-service` |
| Cuándo se saldará | En esta corrida, con el lote que nombra cada ítem. Las asignaciones a lotes de P2 valen igual: el usuario pidió que todo lo pedido entre en esta corrida |

1. **Listas enteras en el cliente del admin.** `AdminPersonasInicio` pide todas las propuestas (`GET /api/asignaciones/propuestas`) para mostrar las pendientes, aunque el resumen trae `propuestasPendientes`, y toda la flota (`logistica.camiones(null)`) para contar los camiones en ruta y disponibles. `AdminOperacionRest` (Asignaciones) lee todas las propuestas, también las decididas, y todas las ejecuciones, dos veces (para mostrar y para decidir), y hace una llamada por necesidad. **Lote:** el inicio, con `p1-admin-inicio` (R11-21: la cifra del resumen y como mucho 3 pendientes con "Revisar propuestas"); Asignaciones, con `p1-asignaciones` (R11-24: propuestas y ejecuciones paginadas en el servidor); la cuenta de camiones por estado, con `p2-listas-restantes` (necesita un conteo en Logística). — Inicio `[OBSERVED]` hecho en su parte de propuestas (2026-11-12, `p1-admin-inicio`): `AdminPersonasInicio` ya no pide `GET /api/asignaciones/propuestas` y muestra solo `propuestasPendientes` del resumen (los tests lo verifican con `never().propuestas(any())`); siguen la flota entera para contar camiones y la lista entera de solicitudes de Keycloak para contar organizaciones ([DTI-42](#dti-42), ítem 8). — Asignaciones `[OBSERVED]` hecho (2026-11-12, `p1-asignaciones`): cada pestaña pide una página, el resumen y la última ejecución; el nombre de la organización y la unidad vienen en la propuesta y decidir ya no pide la lista.
2. **Listados de Donaciones sin página.** `PropuestaDeAsignacionController` (historial de ejecuciones y propuestas), `ItemDonacionNormalizadoController` (pendientes de normalizar) y `NecesidadesController` (tres listados) devuelven todo. **Lote:** propuestas y ejecuciones, `p1-asignaciones`; necesidades (con `entidadId` repetible, que pide DTI-39 ítem 2) y pendientes de normalizar, `p2-listas-restantes`. — Propuestas y ejecuciones `[OBSERVED]` hecho (2026-11-12, `p1-asignaciones`): paginan y filtran en la base, con `X-Total-Count`; `ejecuciones` sigue en memoria (pregunta 3 de la ficha). Categorías, subcategorías y proveedores quedan sin página: son catálogos acotados que llenan listas desplegables (decisión del agente, a confirmar por el usuario).
3. **Listas de otros servicios.** Notificaciones de la persona (`GET /api/notificaciones/persona/{id}`, sin `page` ni `size`; en el recorrido del cierre, unos 149 KB para una entidad), mapa público, seguimientos de entregas, historial del ranking y camiones. **Lote:** `p1-notificaciones` (R11-14), `p1-mapa` (R11-19) y `p2-listas-restantes` (R11-43), con el mismo patrón: `page`, `size` y `X-Total-Count`, y las cifras como números. `[OBSERVED]` Notificaciones de la persona, hecho (2026-11-12, lote `p1-notificaciones`): `GET /api/notificaciones/persona/{id}` y su alias pagina en la base (20 por defecto, 100 como mucho), busca sin tildes con `translate`, filtra por `tipo` y `excluirTipo` y devuelve el total en `X-Total-Count` (OpenAPI de Notificaciones 1.3.0, índice `V6`); las bandejas del donante y de la entidad piden de a 20 y las sin leer salen del conteo del servicio. `[OBSERVED]` Sigue sin página `GET /api/notificaciones` (el listado general con `personaId` y `estado`, que el cliente liviano no usa para las bandejas). **Lote:** `p2-listas-restantes`. `[OBSERVED]` La verificación visual de las dos bandejas marca las casillas (`input` de 20 × 20 dentro de un `label.buzon-casilla` de 48 × 48 px): la verificación mide el `input` y no su `label`, y la misma marca sale en la muestra de `.buzon` de `/estilos`, anterior al lote. — `[VERIFIED]` hecho (2026-11-12, arreglos de la ronda 1 de `p1-notificaciones`): el `input` de cada casilla y el de «Seleccionar todas» miden 48 × 48 px y la marca de 20 px se dibuja al centro (`componentes/buzon.css`); la verificación no se cambió y `paginasCumplenElPisoEnTodosLosAnchos` pasa en `/donante-notificaciones` y `/entidad-notificaciones`.
4. **El importador lee el CSV entero.** `ImportadorService` arma una `List<FilaPadron>` con todo el archivo antes de procesar. El ensayo mostró que el tiempo y la memoria alcanzan, pero es una lista sin loteo. **Lote:** `p1-importador` (R11-B08), que por el punto 4 del pedido entra en esta corrida aunque el ensayo haya alcanzado. — `[OBSERVED]` hecho (2026-11-12, `p1-importador`): `CargadorDonantes` cuenta las filas en una primera pasada (`contarFilas`) y las entrega de a tandas de 500 en la segunda (`leerPorTandas`); `ImportadorService` procesa y guarda cada tanda antes de leer la siguiente, así que nunca hay más de 500 filas en memoria. Si la lectura se corta a mitad, las tandas ya leídas quedan importadas y el archivo termina en `ERROR` con sus números (reimportar es seguro). `[DEFERRED_NO_DOCKER]` Falta repetir el ensayo de 20.000 filas (C8 y C9 de la [spec](../specs/active/R11-p1-importador-spec.md)) al cerrar P1. `[INFERRED]` Cada empresa importada deja una persona "Representante Legal" sin documento (unas 2.980 con el archivo de la guía), que puede verse en el listado de personas del admin: se acepta para la demo (duda 2 de la spec); lo definitivo es que el CSV traiga el representante o que el listado oculte a los representantes. **Lote:** ninguno en el manifiesto: lo decide el usuario.
5. **Mediciones que pide la spec y no se hicieron.** C3 (sentencias por página contadas con las estadísticas de Hibernate: hoy solo con dobles de prueba), C9 (volumen de 20.000 donantes en un test de base) y C10 (oasdiff no se corrió en local: el texto de las dos líneas de R11-01 en `cambios-incompatibles-aceptados.txt` es `[INFERRED]` hasta que lo confirme el job de contratos del PR). `[INFERRED]` Borde del mes de `entregadasEnElMes` (reloj de Buenos Aires, Hibernate en UTC): falta un test con una entrega del último día del mes a las 22 h. `DonanteMapper.toOutputDTOs` no filtra los `personaId` nulos antes de `findAllById`. Los `DonacionesPostgres*ContextTests` fallan con el RabbitMQ del usuario encendido (`ACCESS_REFUSED`, de entorno). **Lote:** C10, el paso de PR de P0; C3, C9, el borde del mes y el filtro de nulos, `p2-listas-restantes` (el lote de contrato que vuelve a tocar los listados de Donaciones).
6. **Hallazgos del cierre de P0, fuera del paginado.** `[OBSERVED]` El constructor de `Persona` agrega un teléfono `ESTANDAR` vacío, así que el filtro `canal=TELEFONO` de `/api/donantes` encuentra a casi todos (anterior a R11). `[INFERRED]` Notificaciones perdió un aviso de bienvenida en el ensayo: `EventoMapper.buscarPersona` lanza `ValidationException` cuando la persona todavía no se copió (carrera entre `sincronizarPersona` y `donante.registrado`) y el mensaje se descarta sin cola de mensajes muertos. Ninguno de los dos tiene lote en el manifiesto: los decide el usuario. — `[OBSERVED]` Aviso de bienvenida, parte de Notificaciones implementada y **pendiente de verificar en el ensayo** (2026-11-12, `p1-importador`, ajustada en la ronda 1 de revisión): cuando `donante.registrado` llega antes que su persona, `ReintentoPersonaFaltante` deja salir el error para que el contenedor reencole el mensaje, sin esperar en el hilo del consumidor (la cola tiene un solo consumidor y `persona.sincronizada` viaja por la misma cola: una espera la dejaba frenada detrás y el aviso terminaba siempre en la DLQ). Cuenta las entregas por `messageId` y a la décima lo rechaza sin reencolar y va a `notificaciones.donaciones.dlq`; sin `messageId`, va directo a la DLQ. El resto de los errores sigue igual. `[VERIFIED]` con un test que pasa los dos eventos por el mismo listener en el orden de la cola (convertidor y decisión de reencolar del contenedor reales, sin broker). `[INFERRED]` El tope cuenta entregas, no tiempo: con la cola vacía, las diez vueltas pueden pasar en pocos milisegundos si `persona.sincronizada` todavía no salió de Donaciones. `[VERIFIED]` con un test, el contenedor por defecto reencolaba esa `ValidationException` sin tope (no la descartaba): la pérdida del ensayo no se explica solo por el listener y queda `[INFERRED]` (pudo ser la publicación de `persona.sincronizada` que falló en Donaciones). `[OBSERVED]` Sigue la carrera en Donaciones: `persona.sincronizada` sale desde un hilo aparte (`NotificacionesAsyncService.sincronizarPersona`, `@Async`) y `donante.registrado` desde el del importador; lo definitivo es publicar la persona antes, en el mismo hilo (`PersonasService` y `NotificacionesAsyncService`). **Lote:** ninguno en el manifiesto: lo decide el usuario. Medir en el ensayo de P1 el pico de `notificaciones.donaciones` y que la DLQ quede en 0 (C9); si llega algún aviso a la DLQ, el arreglo es el orden de publicación en Donaciones, no subir el tope.
7. **Pendientes de pantalla de P0.** `[OBSERVED]` Durante el sondeo del avance (R11-02), una importación que pasa a `ERROR` sin ninguna fila leída hace que `AdminDonantesController.importacion` devuelva `admin-donantes-importar-error`, que no tiene `#avance`: el bloque del avance desaparece sin aviso. Falta responder con `HX-Redirect` a la página de error de la importación y su test. **Lote:** `p1-importaciones` (R11-20, que rehace esas pantallas) — `[OBSERVED]` hecho (2026-11-12): el sondeo responde `HX-Redirect` a `/admin-donantes-importar-error`, con su test. `[OBSERVED]` Las pantallas de persona (`?cuenta=persona` en `paginas.txt`) no pasaron por la verificación visual (axe, toque y desborde a 360, 1280 y 1920 px): el cierre corrió las rutas sin parámetros, que muestran la empresa. **Lote:** `p1-donante-listas` (R11-12 y R11-13, que vuelve a tocar esas pantallas), con la corrida de `-Pvisual` sobre esas rutas como criterio de hecho. — `[VERIFIED]` hecho (2026-11-12): `-Pvisual` pasó sobre `/donante-entidades?cuenta=persona`, `?cuenta=persona&ya=si` y `/donante-donaciones?cuenta=persona` (axe, toque y desborde en los tres anchos).

---

## DTI-41 — Lo que deja abierto la segunda tanda de P0 de R11: esperas, errores con sesión, indicador y cuenta pendiente
<a id="dti-41"></a>

| Campo | Valor |
|---|---|
| ADR | Ninguno: los cuatro lotes (`p0-esperas`, `p0-errores`, `p0-indicador`, `p0-pendiente`) son STANDARD o QUICK, sin cambio de contrato ni decisión nueva de arquitectura (AGENTS.md §9.1, Gate A). Implementan R11-04, R11-05, R11-07 y R11-08 del [backlog de la revisión del 2026-11-11](../interfaz/backlog-revision-2026-11-11.md) |
| Decision status | `proposed` (la deuda; no hay ADR detrás) |
| Decidido por | Usuario (2026-11-11): los ítems y las reglas generales del backlog. El cómo de cada lote y la asignación de cada ítem de abajo, agente (cierre documental de la segunda tanda de P0, 2026-11-12) |
| Implementation status | `[OBSERVED] partial` (2026-11-12, rama `E6_backlog_r11_p0`) — **hecho:** límites de espera en el cliente liviano y en el gateway, con 504 en el gateway (`db81e5fcf`); páginas de error y Contacto con el marco de la cuenta (`f8fc74281`, `926843bea`); `hx-disinherit` en todos los formularios con indicador y `IndicadorEnFormulariosTest` (`48fee1275`); la cuenta pendiente navega lo público (`b97aefaa5`, `c5c16c9dd`). No deja deuda de paginado ni de rendimiento: ningún lote pide listas. **Falta:** los ítems de abajo |
| Target | `cliente-liviano` · `api-gateway` |
| Cuándo se saldará | En esta corrida, con el lote que nombra cada ítem. Lo que no tiene lote lo decide el usuario |

1. **Esperas (R11-04).** `[DEFERRED_NO_DOCKER]` El 504 y los tiempos no se probaron en el stack de Docker (Gate 3/4): el stack era del usuario y no se tocó. **Lote:** el paso de verificación con el stack propio de P0. `[OBSERVED]` Sonar S1604: `ClientesHttp.java` y `EsperasGateway.java` implementan `ClientHttpRequestFactory` con una clase anónima aunque es `@FunctionalInterface`; se cambia por una lambda. `[OBSERVED]` La regla que separa una subida (rutas `POST /api/donantes/archivos` y `POST /api/entregas/{id}/fotos`) está copiada en los dos módulos (no va a `common-lib`: no es neutra al dominio); falta un comentario cruzado y un test que compare esas rutas con las de `DonacionesApi` y `LogisticaApi`. `[INFERRED]` Los comentarios de `application.properties` y `application.yml` dicen que el gateway contesta primero, pero por defecto `cliente.api.*.url` no pasa por el gateway: hay que aclararlo. `[OBSERVED]` El gateway contesta el mismo 504 sin cuerpo cuando el servicio está caído y cuando tarda; lo esperable sería 502 para la conexión rechazada y un `ErrorResponse` (al cliente liviano no le cambia nada). `[OBSERVED]` Falta un test del límite de conexión. Si en la demo correr la asignación pasa los 15 s, se sube `CLIENTE_HTTP_TIMEOUT_LECTURA` y `GATEWAY_TIMEOUT_LECTURA`. **Lote:** ninguno en el manifiesto: los decide el usuario (son arreglos chicos de `p0-esperas`).
2. **Errores y Contacto con sesión (R11-05).** La [ficha de errores](../interfaz/flujos/raices-paginas-de-error.md) espera el OK del usuario sobre el marco con sesión. `[INFERRED]` Falta probar con Keycloak real que el `SecurityContext` llega a `/error` en un 500 del contenedor (con MockMvc y `oidcLogin` anda). `[OBSERVED]` No hay capturas ni verificación automática (axe, desbordes, toque) del estado con sesión (`/publico-contacto?escenario=con-sesion`, `/donante-inicio?respuesta=403`, `/admin-inicio?respuesta=500`). `[OBSERVED]` `publico-paginas.css:8` (`.raices-publico main`) no aplica con el marco privado: Contacto y los errores con sesión quedan más anchos que sin sesión, y la ficha pide que se vean igual. `[OBSERVED]` Una persona cuyo tipo todavía no quedó en la sesión ve en el error el menú de empresa; en la maqueta, Contacto con `?cuenta=persona` también. `[OBSERVED]` `catch (RuntimeException)` sin registro en `Pantallas.conMarcoDeLaCuenta` (posible S1166) y `AreaDeLaSesionMaqueta` con un `substring` frágil. Con login real, Contacto sigue pidiendo el correo (pregunta abierta de su ficha). **Lote:** las capturas y el ancho, `p1-portada` (R11-15, que vuelve a tocar el marco con sesión), que no se integró en P1 ([DTI-42](#dti-42), ítem 1); el resto, ninguno en el manifiesto: lo decide el usuario. `[OBSERVED]` Hallazgo ajeno, sin confirmar: `SeguridadConfig` arma `CuentaVista` con el rol "Sesión iniciada" y `layout-privado` muestra la campanita del admin solo si el rol es "Administración"; con login real el admin probablemente no ve la campanita. **Lote:** `p2-campanita` (R11-30) o `p1-nombres` (R11-44, que cambia de dónde sale el nombre de la cuenta).
3. **Indicador (R11-07).** `[INFERRED]` El arreglo es el que documenta HTMX 2, pero no está `[VERIFIED]` en un navegador: falta un paso de Playwright que toque "Cancelar" con un pedido lento y compruebe que el cartel no aparece. `IndicadorEnFormulariosTest` lee las plantillas como texto: no ve lo que entra por `th:replace` o `th:insert` (hoy, solo `envio :: indicador`) y toma como pedido un `th:href` a un ancla. La sangría del atributo agregado no está alineada en cuatro plantillas. "Sumar otro bien" (registrar donación) ya no muestra ningún cartel. **Lote:** ninguno en el manifiesto: lo decide el usuario. La verificación visual con un filtro de dos pantallas sin movimiento falla siempre en `lasPantallasConMovimientoDejanSuVideoYRespetanReducirMovimiento`: el paso de verificación corre la verificación completa.
4. **Cuenta pendiente (R11-08).** No hay plazo acordado de revisión: la pantalla dice "unos días hábiles" (pregunta abierta 2 de la [ficha pública](../interfaz/flujos/raices-publico-navegable.md)); si el equipo lo fija, cambian la pantalla y la ficha. Con la cuenta pendiente, lo público muestra el encabezado de visitante ("Ingresar", "Crear una cuenta", que la devuelven a la solicitud) y la única salida para cerrar sesión está en "Recibimos la solicitud" (regla general 6). **Lote:** `p1-portada` (R11-15). `[OBSERVED]` `p1-portada` no se integró en P1 ([DTI-42](#dti-42), ítem 1). Ver y corregir la solicitud: `p2-pendiente-solicitud` (R11-41). `[OBSERVED]` `PerfilPendienteTest` no prueba `/publico-entidades` (el botón principal, que era el defecto), `/publico-donaciones`, `/publico-mapa` ni `/publico-contacto`; `EstadoDeCuenta` lista a mano las pantallas públicas permitidas, y una pantalla pública nueva quedaría bloqueada sin aviso; una ruta pública que no existe lleva a la solicitud en lugar del 404. `[OBSERVED]` El párrafo "Mientras tanto…" de `publico-registro-entidad-enviada.html` queda separado del resto por un hueco mayor que el ritmo de la pantalla, y no hay captura del estado con sesión pendiente. **Lote:** los tests y el espaciado, `p2-pendiente-solicitud` (vuelve a tocar esa pantalla y ese filtro).

---

## DTI-42 — Lo que deja abierto la oleada P1 de R11
<a id="dti-42"></a>

| Campo | Valor |
|---|---|
| ADR | Los siete `proposed` de la oleada, escritos después del código en el cierre documental: [cuentas de administración](./auth-service/20261112-cuentas-de-administracion-marcadas-en-el-realm-y-alta-desde-el-sitio.md), [autor de la decisión de una propuesta](./donaciones-service/20261112-autor-de-la-decision-de-una-propuesta-desde-el-token.md), [historial de importaciones](./donaciones-service/20261112-historial-de-importaciones-con-nombre-y-fecha-de-subida-en-el-archivo.md), [organizaciones vistas por un donante](./donaciones-service/20261112-entidades-vistas-por-un-donante-en-un-recurso-propio-con-la-marca-ya-le-dono.md), [bandeja paginada](./notificaciones-service/20261112-bandeja-por-persona-paginada-con-busqueda-sin-tildes-por-translate.md), [mapa por período y con tope](./logistica-service/20261112-mapa-publico-por-periodo-y-con-tope.md) y [sugerencia de cada algoritmo](./donaciones-service/20261112-propuestas-con-la-sugerencia-de-cada-algoritmo-y-su-situacion-calculada-en-donaciones.md). Implementan R11-10 a R11-24, R11-44, R11-45 y R11-B08 del [backlog de la revisión del 2026-11-11](../interfaz/backlog-revision-2026-11-11.md) |
| Decision status | `proposed` |
| Decidido por | Usuario (2026-11-11 y 2026-11-12): los ítems, las reglas generales del backlog y la aprobación automática de los diseños de P1 con las opciones recomendadas. El cómo de cada lote, sus constructores (agente, sin consulta). La asignación de cada ítem de abajo, agente (cierre documental de P1, 2026-11-12) |
| Implementation status | `[OBSERVED] partial` (2026-11-12, rama `E6_backlog_r11_p1`, HEAD `0edf2811d`) — **hecho:** 13 lotes integrados (`p1-admins`, `p1-paginador`, `p1-menus`, `p1-iconos`, `p1-ranking`, `p1-importaciones`, `p1-admin-inicio`, `p1-donante-listas`, `p1-notificaciones`, `p1-mapa`, `p1-asignaciones`, `p1-nombres`, `p1-importador`). `[VERIFIED]` en el cierre: reactor (`common-lib`, Donaciones, Notificaciones, Logística, cliente liviano y gateway) en verde, `auth-service` 34/34 con `mvn -f auth-service/pom.xml test`, `spotless:check` en verde, Gate 3 30/30 en el stack propio y la verificación visual de 18 pantallas en verde después de los arreglos de `/estilos` (`0edf2811d`). **Sin integrar:** `p1-portada` (ítem 1). **Falta:** los ítems de abajo |
| Target | `cliente-liviano` · `donaciones-service` · `notificaciones-service` · `logistica-service` · `auth-service` · `incentivos-service` |
| Cuándo se saldará | En esta corrida, con el lote que nombra cada ítem. Lo que dice "sin lote" lo decide el usuario |

1. **`p1-portada` sin integrar (R11-15 y R11-17).** `[OBSERVED]` El cherry-pick de `d8964a17d` chocó en `templates/layout-privado.html`: `p1-portada` reemplaza el `<details class="navegacion-cuenta">` por el fragmento nuevo `cuenta-menu :: menu`, y `p1-admins` (ya integrado) agregó dentro de ese mismo bloque el enlace "Administradores". Se abortó. Para integrarlo hay que pasar ese enlace a `cuenta-menu.html` y volver a correr la verificación visual. Arrastra lo que DTI-41 le asignaba: el encabezado de la cuenta en la portada y en lo público para la cuenta pendiente (ítem 4), y las capturas y el ancho de errores y Contacto con sesión (ítem 2). **Lote:** reintegrar `p1-portada` (sin lote nuevo en el manifiesto: lo decide el usuario).
2. **Cuentas de administración (R11-10, `p1-admins`).** El stack de Docker del usuario tiene el realm importado y no toma `administracion` ni `altaPor`: hay que reimportar el realm (recrear solo Keycloak o importar el realm; nunca `down -v`) antes de mostrar Administradores y Mi perfil del admin en la demo. `[INFERRED]` Que `/users/count` acepte `q` en Keycloak 26.8 y los tres casos nuevos de `RealmTokenContractIT` (IT con Docker) no constan como corridos. `[OBSERVED]` Si falla el `DELETE` que deshace un alta a medias, la cuenta queda y la pantalla dice "No se creó nada"; sin `Location`, no se busca la cuenta para borrarla (`AdministradoresKeycloak`). `[INFERRED]` El alta exitosa va con `hx-boost`: el navegador puede guardar en su historial (o en el de HTMX) la página con la contraseña provisoria; falta `hx-history="false"` o no hacer *push* de esa dirección, y una prueba en un navegador real. `[OBSERVED]` "Dar de alta" es un botón con `popovertarget` y la ficha pedía un enlace a `/admin-administradores-nuevo` sin JavaScript (anda igual, con el popover nativo); la contraseña tiene 16 caracteres al azar más 3 guiones y C1 dice "16 caracteres" (aclarar la spec); "Otra contraseña" lee la cuenta dos veces; un comentario quedó cortado por spotless. El popover que no bloquea el fondo es de `p2-ventanas` (R11-32) y el botón principal fijo en el celular, de `p2-armazon` (R11-33). **Lote:** reimportar el realm, el paso de verificación con el stack del usuario; el resto, sin lote.
3. **Paginador (R11-11, `p1-paginador`).** `[OBSERVED]` La ventana es simétrica (`1 … 7 8 9` en la última página) y la ficha §4.1 dibujaba `1 … 8 9`: queda registrado en la [bitácora](../interfaz/bitacora.md), la ficha no se reescribe. `[INFERRED]` Entre 600 y 700 px, con números de cuatro cifras, la forma larga puede partirse en dos renglones (no se desborda: `flex-wrap`); las capturas no cubren ese ancho. `[OBSERVED]` En la forma corta las flechas se ven chicas; en la primera y la última página los `li` apagados quedan vacíos para el lector. `templates/paginador.html` sigue como pase del provisorio: lo nombran las listas del admin y `admin-importaciones`; se borra cuando ninguna lo nombre. **Lote:** sin lote (arreglos chicos del componente).
4. **Menús que se cierran (R11-16, `p1-menus`).** `[INFERRED]` Al llegar un fragmento por HTMX se cierran todos los menús, también el de la cuenta: el sondeo de 3 s del avance de una importación lo cierra solo. Arreglo: cerrar solo los menús dentro del elemento reemplazado. `[OBSERVED]` Esc cierra los menús aunque haya un `<dialog popover>` abierto encima, y el foco va al menú y no a quien abrió el diálogo. **Lote:** sin lote.
5. **Íconos (R11-22, `p1-iconos`).** `[OBSERVED]` `/estilos` no muestra los nueve símbolos del sprite: los cinco nuevos no se ven en ninguna captura. `Comun.icono` ya no devuelve `null` para una categoría con nombre: quedan guardas muertas en `entidad-necesidades.html` y `AdminPersonasEntidades`. `DonanteDonacionesDeEjemplo` usa `cat-vestimenta`, que no existe (el símbolo es `cat-ropa`). Una línea del comentario de `iconos.html` tiene 171 columnas. **Lote:** sin lote.
6. **Ranking (R11-23, `p1-ranking`).** `[OBSERVED]` `GET /api/incentivos/ranking/historial` trae todos los meses con todos sus puestos y la tabla no se pagina: con el seed nuevo, 12 a 15 puestos o más por mes (regla 1). La ficha `raices-admin-navegable.md` §4.9 sigue con el título y el mes de antes. El seed ya no deja meses cerrados sin ranking, así que el estado vacío solo se ve en la maqueta. A 360 px el título ocupa cuatro renglones. **Lote:** el historial paginado, `p2-listas-restantes` (R11-43); el resto, sin lote.
7. **Importaciones (R11-20, `p1-importaciones`).** La barra de búsqueda y filtros no queda fija al desplazarse (regla 5, `p2-armazon`). `[OBSERVED]` La plantilla usa el pase `paginador :: paginador` en lugar de `componentes/paginas :: paginas`; en el celular es la tabla apilada con `data-rotulo` y la ficha pedía una lista; cuando el sondeo responde `HX-Redirect`, `AdminDonantesController` igual pide y tira una página de donantes; a 1280 px queda poco aire bajo "Actualizar" y los botones de la columna de acción se ven desparejos; la región `aria-live` con la cuenta está dentro del `main` que se reemplaza y puede no anunciarse. No hay capturas propias de los escenarios `vacia`, `sin-resultados` y `con-viejas`. Falta probar la descarga con el CSV real de 20.000 filas en el stack y llevar las dos columnas de `archivo` al DER. **Lote:** la barra fija, `p2-armazon`; el resto, sin lote.
8. **Inicio del admin (R11-21, `p1-admin-inicio`).** `[OBSERVED]` Sigue pidiendo toda la flota para contar camiones (ítem 1 de [DTI-40](#dti-40), `p2-listas-restantes`) y cuenta las organizaciones pendientes con el largo de la lista entera de solicitudes de Keycloak (regla 1). Cuando Logística falla, el `catch` no registra la excepción, así que el código que se muestra no se puede unir con la falla en los registros. La plantilla lee `?escenario` también fuera de la maqueta. Con 0 entregas fallidas, la fila "No se pudo entregar" sale sin su aclaración. El encabezado de la ficha `raices-entidad-navegable.md` (R11-21) sigue "en revisión" aunque abajo dice aprobada. **Lote:** camiones, `p2-listas-restantes`; el resto, sin lote.
9. **Listas del donante (R11-12, R11-13, R11-18, `p1-donante-listas`).** `[OBSERVED]` A9 (una sola consulta de `yaLeDono` por página) no tiene un test que cuente sentencias, y A1 y A5 no corren sobre PostgreSQL. El repositorio en memoria recorre todas las necesidades, subcategorías y donaciones en cada pedido (solo perfil en memoria). Con una subcategoría elegida, cambiar la categoría no hace nada y no se explica. Las plantillas de `persona/` de la maqueta no tienen el filtro Categoría ni el paginador común. El filtro "sin vencer" usa la zona del sistema y no la de Buenos Aires. Seguimiento pide una página de 100 con los estados en curso y `EN_DEPOSITO`: con más de 100, la cifra "Esperan destino" se queda corta sin aviso. Los filtros no quedan fijos (regla 5, `p2-armazon`). El inicio del donante sigue con la página de 100 (`p2-tablero`) y las pantallas de la organización también (`p2-listas-restantes`, [DTI-39](#dti-39)). **Lote:** los que se nombran; el resto, sin lote.
10. **Notificaciones (R11-14, `p1-notificaciones`).** `[OBSERVED]` Sin `?agrupar`, el menú dibuja "Sin agrupar" como filtro aplicado; el botón con casillas elegidas dice "Marcar N como leídas" y la ficha pide "Marcar las N elegidas como leídas"; los avisos de la organización no usan los textos de la ficha ("Listo: marcaste…"), el donante sí; marcar una fila manda la página entera (la ficha pedía cambiar solo la fila con HTMX); "Mostrando X a Y" usa un 20 escrito a mano; el vínculo "Ver Organizaciones" de un aviso de necesidad existe solo en la maqueta; el botón "Marcar todas como leídas" no dice que alcanza a toda la cuenta (lo dice la nota). La maqueta tiene menos de 20 avisos, así que ninguna captura muestra el paginador ni los estados marcados. `GET /api/notificaciones` (general) sigue sin página ([DTI-40](#dti-40), ítem 3). **Lote:** el listado general, `p2-listas-restantes`; la campanita, `p2-campanita`; el resto, sin lote.
11. **Mapa (R11-19, `p1-mapa`).** `[OBSERVED]` `CONTROLES_CHICOS` de `VerificacionVisualTest` compara `width < 48` sin tolerancia y marca los marcadores de MapLibre en posición subpíxel (47,99998 px), en el escenario `muchas` y en `/donante-seguimiento`; la propuesta es redondear antes de comparar (cambio del test, sin debilitarlo). Las rutas `?escenario=muchas` no tienen capturas revisadas a ojo; a 360 px "6 entregas confirmadas" baja a dos renglones y la ficha pedía acortarlo. `[INFERRED]` Los dos adaptadores escriben distinto "con coordenadas"; si el mapper devolviera `null` para una entrega, la cifra con tope contaría de más. Falta probar en un navegador real el cambio de página con HTMX (foco, aviso y reintento) y correr oasdiff sobre Logística. La organización en lugar del lugar con datos reales va al backlog (toca D24). **Lote:** sin lote.
12. **Asignaciones (R11-24, `p1-asignaciones`).** `[OBSERVED]` A7 no tiene un test que cuente sentencias (Hibernate Statistics). La pestaña Destino pide `GET /api/categorias` sin página para el filtro: es un catálogo chico, excepción a la regla 1 que el usuario tiene que confirmar. En el celular el panel "Estado de la asignación" queda al final y la ficha lo ponía arriba en una línea; las pestañas no quedan fijas (regla 5) y a 360 px "Destino" queda fuera de la vista sin una pista de que se desliza; poco aire antes de "Decididas"; no hay capturas de Conflictos ni de Destino. Donaciones no rechaza aprobar una propuesta `SIN_STOCK`; las ejecuciones siguen en memoria; elegir el algoritmo es R11-B05. **Lote:** rechazar `SIN_STOCK`, un lote corto después de P1 (duda 2 de la spec); las ejecuciones en la base, backlog; lo fijo, `p2-armazon`; el resto, sin lote.
13. **Nombres (R11-44, R11-45, `p1-nombres`).** `[OBSERVED]` Si el servicio no devuelve el nombre, el saludo dice "Hola, Tu cuenta" (la propuesta: solo "Hola"). El registro de un donante jurídico dice "Empresa, ONG o institución" y no nombra a Gubernamental, que se acepta. Sin tipo jurídico, el admin muestra "Persona jurídica" y el perfil "Empresa donante". La sesión guarda el nombre de una sola área a la vez. `AdminPersonasEntidades.necesidadesVigentes` pide todas las necesidades vigentes y todas las subcategorías (anterior a P1; regla 1). El corte en sílabas de la barra del celular ("Organiza-ciones") espera la opinión del usuario. Los comentarios del código y las fichas de `flujos/` siguen diciendo "entidad" (no se ven). No se probó con login real de punta a punta. **Lote:** las necesidades vigentes del admin, `p2-listas-restantes`; el resto, sin lote.
14. **Importador (R11-B08, `p1-importador`).** `[DEFERRED_NO_DOCKER]` Falta el ensayo de 20.000 filas de la [spec](../specs/active/R11-p1-importador-spec.md) (C8 y C9): 19.400 creadas, 400 actualizadas y 200 con error; pico de `notificaciones.donaciones` y vuelta a 0; `notificaciones.donaciones.dlq` en 0; un aviso por donante creado; memoria de Donaciones contra los 420 MiB de P0. Por eso la spec sigue en `active/`. `[INFERRED]` El tope de 10 entregas cuenta vueltas, no tiempo: con la cola vacía pueden pasar en milisegundos antes de que salga `persona.sincronizada`; el arreglo de fondo es el orden de publicación en Donaciones ([DTI-40](#dti-40), ítem 6). El conteo por `messageId` vive en memoria (se pierde al reiniciar, cada réplica cuenta aparte y pueden quedar entradas huérfanas). `[OBSERVED]` Si la lectura se corta a mitad de una tanda, las filas con error de esa tanda no se guardan (`ImportadorService`, el `catch`). El modelo de la cola del test devuelve el mensaje al final y RabbitMQ lo devuelve cerca de la cabeza: la redacción de la spec y de DTI-40 ítem 6 sobre ese orden tiene que decir que depende del *prefetch*. No hay un test que combine el reencolado con el inbox en PostgreSQL. Cada reencolado deja un WARN con la traza. `[OBSERVED]` `DonacionesPostgresMinioContextTests.java` y `MinioStorageAdapterIntegracionTest.java` tienen los bytes `EF BB BF` (sin mirar si es un BOM de inicio de archivo). **Lote:** el ensayo, el paso de verificación con el stack propio; el orden de publicación y los representantes legales, sin lote ([DTI-40](#dti-40), ítems 4 y 6).
15. **Gates y entorno.** `[OBSERVED]` Los tests de contexto de Donaciones y Notificaciones fallan con el RabbitMQ del usuario encendido (`ACCESS_REFUSED`); el cierre los corrió en verde con el stack detenido. El comando del gate del workflow nombra `-pl auth-service`, que no está en el reactor raíz: hay que correrlo como `mvn -f auth-service/pom.xml test`. La prueba de movimiento de la verificación visual falla siempre que el filtro de páginas deja afuera las pantallas con movimiento (ya en [DTI-41](#dti-41), ítem 3). `/estilos` desborda 1 px a 320 px, por debajo del piso de 360 (antes y después del cierre). El informe del cierre no registra una corrida de Gate 4 (`run-preprod-tests.sh`). No se corrió `revision-ui` como revisión de pantalla aparte en `p1-portada` ni en `p1-admin-inicio`. **Lote:** el ajuste del comando, el workflow; el resto, el paso de verificación.
