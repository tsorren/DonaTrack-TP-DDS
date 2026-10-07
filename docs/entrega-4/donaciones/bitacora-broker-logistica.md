# Bitácora — Broker de Integración con Logística (Entrega 4)

> Registro vivo de la implementación del broker: decisiones, preguntas, aclaraciones y discusiones, en orden. Para retomar el trabajo, leer primero el **TLDR**, después el **Estado por etapa** y la última entrada del **Registro**.
>
> **Documentos relacionados:** [Plan](plan-broker-logistica.md) · [SPEC-04](../../specs/active/SPEC-04-broker-integracion-logistica.md) · [ADR 20261007](../../adr/20261007-broker-de-integracion-con-logistica.md) · [Bitácora de comunicaciones (antecedente)](bitacora-comunicaciones.md)
>
> **Rama:** `E4_donaciones_broker`

---

## TLDR

> Última actualización: **2026-10-07** — Etapa 3 hecha y Gate 4 en verde; Etapa 4 parte 1 (adapter HTTP) hecha, parte 2 (dedup REST en Logística) pendiente.

- **Qué estamos haciendo:** un broker dentro de `donaciones-service` que elige a qué proveedor de logística mandarle cada entrega: el nuestro por RabbitMQ, u otro por HTTP. Así se cumple el requerimiento de la Entrega 4.
- **Cómo:** Broker + Adapter + Strategy. El pedido viaja como comando `entrega.solicitada.<proveedorId>.v1`; logística deja de escuchar el hecho `donacion.asignada.v1`. Ningún pedido se pierde en silencio (`mandatory` + espera de acuse) y no se reenvía a otro proveedor si no hay certeza de que el primero no lo recibió.
- **Dónde estamos:** **Etapas 0, 1, 2 y 3 hechas**. Ya existen el broker, el adapter AMQP hacia nuestra logística y el comando `entrega.solicitada.<id>.v1`; logística ya escucha el comando y no el hecho. Si todos los proveedores rechazan un pedido, el broker reintenta la ronda completa más tarde (D27).
- **Atención:** la asignación **ya llama al broker** (Etapa 3), pero el pedido sale recién cuando corre el relay del outbox (cada 10 s por defecto; en `docker-compose.preprod.yml` está en 1 s) y solo hay adapter AMQP para `donatrack`. **Gate 4 en verde (26/26)** con las Etapas 2 y 3 juntas, sin MinIO (ver Etapa 3).
- **Bloqueante para seguir:** ninguno.
- **Próximo paso:** Etapa 4 parte 2: deduplicación REST en Logística (`EntregasService.crear` → 409). Decisión pendiente: hacerla mínima o dejarla como deuda y proponérsela al equipo de Logística.

---

## Estado por etapa

| Etapa | Contenido | Estado |
|---|---|---|
| 0 — Preparación | Rama, baseline, spec, ADR, bitácora | ✅ Cerrada |
| 1 — Núcleo sin cableado | Broker, estrategia, outbox, registro, excepciones | ✅ Cerrada |
| 2 — Contrato, adapter AMQP y cutover | `EntregaSolicitadaV1`, `mandatory` + acuse, cambios en Logística | ✅ Cerrada (sin commitear) |
| 3 — Cableado | `PropuestaDeAsignacionService` → broker | ✅ Hecha (Gate 4 en verde) |
| 4 — Adapter HTTP + dedup REST | `ProveedorLogisticaHttp`, 409 en `EntregasService.crear` | 🟡 Parte 1 (adapter HTTP) hecha; parte 2 (dedup en Logística) pendiente |
| 5 — Vuelta HTTP + protección | Callback, `ProcesadorEventosLogistica`, `ApiKeyFilter` | ⏳ Pendiente |
| 6 — Controller admin (recortable) | Cambiar el proveedor preferido en caliente | ⏳ Pendiente |
| 7 — Demo | Segunda instancia, Postman, guion | ⏳ Pendiente |
| 8 — Cierre | Catálogo, matriz, diagrama, deuda, índices, gates | ⏳ Pendiente |

---

## Decisiones

Registro acumulado. Las decisiones D1 a D12 vienen del plan; las que se tomen durante la implementación se agregan acá con su fecha.

| # | Decisión | Fecha | Origen |
|---|---|---|---|
| D1 | Broker in-process en Donaciones (Broker + Adapter + Strategy). | 2026-10-07 | Plan |
| D2 | Logística consume `entrega.solicitada.<id>.v1` en lugar de `donacion.asignada.v1`. Revierte la decisión del 15/9. | 2026-10-07 | Plan · **acordado por el grupo** (confirmado en la Etapa 0) |
| D3 | Outbox en memoria, detrás de un puerto, con entradas basadas en datos. | 2026-10-07 | Plan |
| D4 | Dos transportes: AMQP y HTTP. | 2026-10-07 | Plan |
| D5 | Estrategia base «preferencia + reenvío»; las adicionales se acuerdan con el equipo. | 2026-10-07 | Plan |
| D6 | API key en los endpoints expuestos (sin Spring Security). | 2026-10-07 | Plan |
| D7 | Comando dirigido por routing key, no por un exchange por proveedor. | 2026-10-07 | Plan |
| D8 | `mandatory=true` + reenvío ante devolución. | 2026-10-07 | Plan |
| D9 | Segunda instancia de Logística para la demo. | 2026-10-07 | Plan |
| D10 | Rechazado → siguiente proveedor; incierto → mismo proveedor. | 2026-10-07 | Plan |
| D11 | El relay espera el acuse con `CorrelationData`. | 2026-10-07 | Plan |
| D12 | `POST /api/entregas` deduplica por donación (409). | 2026-10-07 | Plan |
| D13 | La regla «los eventos se rutean por hecho; los comandos, por destinatario» se formaliza **dentro del ADR del broker**, no en un ADR propio. La matriz productor-consumidor va a remitir a ese ADR (Etapa 8). | 2026-10-07 | Etapa 0 |
| D14 | Solo se implementa la estrategia base (`SeleccionPorPreferenciaConFallback`). No hay estrategias de prueba adicionales (RoundRobin, PorZona y PorCarga quedan descartadas por alcance). | 2026-10-07 | Etapa 1 |
| D15 | El registro de solicitudes es un **agregado con comportamiento**: `SolicitudEntrega` en `models/entities/logistica/`, con transiciones validadas en sus métodos (como `Propuesta.aceptar()`). Para no confundir nombres, el formato canónico del plan (`SolicitudEntregaLogistica`) pasa a llamarse **`DatosEntregaLogistica`**. | 2026-10-07 | Etapa 1 |
| D16 | Los 4 puertos y las 3 excepciones (`EnvioRechazado`, `EnvioIncierto`, `ErrorContratoProveedor`) van en el subpaquete **`services/logistica/`**. Las excepciones son `RuntimeException` propias, sin tocar common-lib ni `ErrorCatalog`, porque nunca llegan a un controller. Corrige el plan, que las ponía en `infrastructure/`: el broker (aplicación) las atrapa y no debe depender de infraestructura. | 2026-10-07 | Etapa 1 |
| D17 | Tiempos por defecto del outbox del broker: **los mismos que el outbox existente** (relay cada 10 s, máximo 5 intentos, backoff 30 s · 2^n), configurables por variable de entorno. | 2026-10-07 | Etapa 1 |
| D18 | **Relay y broker, variante C:** el relay llama al adapter, traduce la excepción a un enum `ResultadoEnvio` (`PUBLICADO`, `RECHAZADO`, `INCIERTO`, `ERROR_CONTRATO`) y se lo pasa al broker. El broker aplica la regla como lógica pura, sin hablar con la red ni atrapar excepciones. Se descartaron A (el broker decide **y** hace I/O) y B (lógica repartida sin ganar simplicidad). | 2026-10-07 | Etapa 1 |
| D19 | Una excepción **desconocida** del adapter (que no es ninguna de las tres) se traduce como **`INCIERTO`**: no se sabe si el pedido salió, así que se reintenta con el mismo proveedor y nunca se arriesga una entrega en dos proveedores. | 2026-10-07 | Etapa 1 |
| D20 | Un proveedor configurado **sin adapter registrado** se traduce como **`RECHAZADO`** (seguro que no salió) → siguiente proveedor, con log de advertencia. Permite tener la lista de proveedores configurada antes de que existan los adapters. | 2026-10-07 | Etapa 1 |
| D21 | **Una solicitud activa por donación:** si ya hay una `PENDIENTE` o `ENVIADA`, un pedido nuevo se ignora con log (idempotencia). Si la anterior quedó `FALLIDA`, se permite crear una nueva (reintento manual o replanificación futura). | 2026-10-07 | Etapa 1 |
| D22 | Las transiciones inválidas de `SolicitudEntrega` lanzan `BusinessStateException` con un **código nuevo en common-lib**: `SOLICITUD_ENTREGA_TRANSICION_INVALIDA` (`ERR-EST-413`). Sigue el precedente de un código por entidad con estados; `common-lib/AGENTS.md` permite extender el catálogo respetando los prefijos. | 2026-10-07 | Etapa 1 |
| D23 | Se agregan **dependencias de test de Testcontainers** (`org.testcontainers:rabbitmq` y `junit-jupiter`, versión del BOM del pom raíz) para probar contra un RabbitMQ real que un sobre sin buzón vuelve devuelto. Los tests usan `@DisabledIfDockerUnavailable` (common-lib), como los de notificaciones. Aprobado por el usuario (AGENTS.md §6). | 2026-10-07 | Etapa 2 |
| D24 | El adapter AMQP espera el acuse de RabbitMQ **5 segundos** (`donatrack.logistica.acuse-timeout-ms`, configurable). Pasado ese tiempo, el envío es `INCIERTO`. | 2026-10-07 | Etapa 2 |
| D25 | ~~Logística borra la cola vieja al arrancar, solo si está vacía.~~ **Reemplazada por D28.** | 2026-10-07 | Etapa 2 |
| D26 | Los adapters AMQP se crean **de forma genérica desde la configuración**: `LogisticaProveedoresConfig` arma un `ProveedorLogisticaAmqp` por cada proveedor con `transporte=amqp`. En la Etapa 4 se agrega el caso `http`. | 2026-10-07 | Etapa 2 |
| D27 | **Si todos los proveedores rechazan un pedido, se reintenta la ronda completa más tarde** en lugar de fallar en el acto: se olvidan los descartes y se vuelve a empezar por el preferido, con la misma espera creciente que los envíos inciertos (60 s, 120 s…), hasta 5 rondas (`outbox.max-intentos`); recién ahí la solicitud queda `FALLIDA`. Motivo: una caída pasajera (RabbitMQ caído y el proveedor HTTP también) no debe dejar asignaciones sin entrega para siempre. No hay riesgo de duplicar: solo se reintenta lo que es seguro que no llegó. Se descartaron «caída de RabbitMQ = incierto» (no aprovecha el otro proveedor y no resuelve HTTP) y «fallar en el acto». | 2026-10-07 | Etapa 2 |
| D28 | **No hay código de limpieza de la cola vieja** (`logistica.donaciones.asignadas`); se eliminó `LimpiezaColaObsoleta`. Motivo: los RabbitMQ del proyecto son descartables (los `docker-compose` no tienen volúmenes), así que la cola huérfana desaparece al recrear el contenedor, y CI/preprod arrancan siempre con uno nuevo. El código resolvía un problema que casi no existe y sumaba un caso borde. **Paso de cutover:** si un RabbitMQ viene de antes del broker (se detuvo y arrancó sin recrearse), recrearlo (`docker compose down` + `up`) o borrar la cola desde la consola de administración. | 2026-10-07 | Etapa 2 |
| D29 | **El pedido al broker no lleva el donante.** `DatosEntregaLogistica` se arma con la donación asignada y los datos del beneficiario que ya se resuelven una vez por evento; Logística nunca usó el donante (`CrearEntregaRequestDTO` no lo tiene). Sumarlo sería un cambio aditivo de contrato aparte. | 2026-10-07 | Etapa 3 |
| D30 | **Una falla del broker no tumba la aprobación:** `solicitarEntregaALogistica` captura la excepción y la loguea, igual que la publicación de `donacion.asignada.v1`; si faltan los datos del beneficiario no se llama al broker (`warn`). | 2026-10-07 | Etapa 3 |
| D31 | **Clasificación del adapter HTTP:** 2xx y 409 → publicado; 503, conexión rechazada, host inexistente y timeout de conexión → rechazado; timeout de lectura y 500/502/504/otros 5xx → incierto; otros 4xx y redirecciones → error de contrato. Toda falla de red que no sea «no se pudo conectar» cuenta como incierta (prefiere reintentar al mismo proveedor antes que arriesgar dos entregas). | 2026-10-07 | Etapa 4 |
| D32 | **Cliente HTTP:** `RestClient` sobre el `HttpClient` del JDK (HTTP/1.1), con timeout de conexión (1000 ms) y de lectura (3000 ms) separados y configurables por proveedor (`donatrack.logistica.proveedor.<id>.connect-timeout-ms` / `read-timeout-ms`). Un proveedor `http` sin `url` queda sin adapter (se trata como rechazado, igual que D20). Sin dependencias nuevas. | 2026-10-07 | Etapa 4 |

---

## Preguntas abiertas

| # | Pregunta | Para cuándo | Estado |
|---|---|---|---|
| P1 | ¿Se aprueba SPEC-04 tal como está? | Antes de la Etapa 1 | ✅ Cerrada: aprobada sin cambios (2026-10-07) |
| P2 | ¿Qué estrategias adicionales de selección se implementan, y con qué criterio exacto? (Candidatas: RoundRobin, PorCarga, PorZona.) | Etapa 1 | ✅ Cerrada: ninguna, solo la base (D14) |
| P3 | ¿Qué código de `ErrorCatalog` usa el 409 de Logística? | Etapa 4 | Abierta |
| P4 | ¿Qué valores de `acuse-timeout-ms` y del read timeout HTTP usamos? (Propuesta: 5000 y 3000 ms.) | Etapas 2 y 4 | Parcial: acuse = 5000 ms (D24). Read timeout HTTP = 3000 ms y connect timeout = 1000 ms (D32) ✅ |

---

## Registro

### 2026-10-07 — Antecedentes: cómo se llegó a este plan

Discusión previa a la implementación, resumida para entender por qué el plan es como es.

1. **«Broker» no es RabbitMQ.** Al principio se interpretó «broker» como *message broker*. El enunciado de la Entrega 4 pide dos cosas distintas: (a) una **cola de mensajes** para notificaciones y (b) un **broker de integración** con logística que **elija** entre más de un proveedor. Este trabajo es el (b).
2. **Primer diseño (obsoleto):** un broker que envolvía `LogisticaFeignClient`. Quedó obsoleto cuando el equipo eliminó Feign y migró todo a RabbitMQ (#883): logística pasó a escuchar `donacion.asignada.v1` directo.
3. **El problema del pub/sub:** un exchange copia el mensaje a todas las colas que coinciden, no elige. Con dos proveedores escuchando el mismo hecho, los dos crean la entrega. Por eso la selección tiene que hacerse en Donaciones, y logística tiene que recibir un **pedido dirigido** (comando) en lugar del hecho.
4. **¿Cómo dirigir el comando?** Se compararon tres opciones: routing key por proveedor, un exchange por proveedor y un adapter por transporte. Se eligió **routing key por proveedor** para los proveedores AMQP (es más simple que un exchange por proveedor y deja clara la propiedad), combinada con **un adapter por transporte** (un proveedor externo puede hablar HTTP).
5. **Mensajes perdidos en silencio:** un TopicExchange descarta lo que no matchea con ningún binding. Se evaluaron `mandatory` + returns, un alternate exchange y que Donaciones declare las colas. Se eligió `mandatory` con **espera del acuse**, porque el alternate exchange impide que el broker se entere de la falla.
6. **Plan de Nico vs. plan propio:** se compararon y se unificaron. Del plan de Nico se tomó la base (outbox basado en datos, registro de solicitudes, callback protegido, alias de tipo independiente, cola por instancia, segunda instancia como demo). Se le agregaron tres ajustes:
   - **timeout ≠ rechazo**, para evitar entregas en dos proveedores;
   - **espera del acuse** en lugar de un `ReturnsCallback` asincrónico;
   - **dedup en `POST /api/entregas`**, que `[OBSERVED]` hoy no deduplica.

### 2026-10-07 — Etapa 0: Preparación

**Hecho:**
- Rama `E4_donaciones_broker` al día con `origin` (HEAD `d82d5760`, «plan unificado broker»).
- **Baseline `[VERIFIED]` — BASELINE_GREEN** con `mvn clean test -pl donaciones-service,logistica-service -am -Dspotless.check.skip=true`:

  | Módulo | Tests | Fallos | Errores | Omitidos |
  |---|---|---|---|---|
  | `common-lib` | 60 | 0 | 0 | 0 |
  | `donaciones-service` | 437 | 0 | 0 | 0 |
  | `logistica-service` | 355 | 0 | 0 | 1 |

  Los stack traces en el log (`Error simulado`, `MethodArgumentNotValidException`) son de tests que simulan fallos a propósito, no fallas.
- ADR [`20261007-broker-de-integracion-con-logistica.md`](../../adr/20261007-broker-de-integracion-con-logistica.md) creado como `proposed`.
- Spec [`SPEC-04`](../../specs/active/SPEC-04-broker-integracion-logistica.md) creada como `PROPOSED` y **aprobada sin cambios** por el usuario → `APPROVED_BY_USER`.
- Índices actualizados: `docs/specs/README.md` y `docs/ESTADO_DOCUMENTACION.md`.

**Decisiones:**
- El grupo **ya acordó** revertir la decisión del 15/9 (D2).
- La regla de ruteo eventos/comandos va **dentro del ADR del broker** (D13).

**Aclaraciones:**
- Los commits los hace el equipo a mano; el agente deja los cambios sin commitear.
- El ADR queda `proposed` aunque el grupo esté de acuerdo, porque según AGENTS.md §9.3 ningún agente puede promoverlo a `accepted`. Lo tiene que cambiar una persona del equipo.
- **Para qué sirve cada documento** (para no confundirlos):

  | Documento | Contesta | Característica |
  |---|---|---|
  | ADR | **Por qué** este diseño y no otro | Permanente: no se edita, se reemplaza con otro ADR |
  | SPEC-04 | **Qué** entra, qué no entra, qué reglas no se pueden romper y cómo se verifica | Obligatoria para tareas grandes (`docs/specs/README.md`); se aprueba antes de codear |
  | Plan | **Cómo** se hace, paso a paso | Se puede ajustar durante la implementación |
  | Bitácora | **Qué pasó** y qué falta | Se actualiza en cada paso |

  La spec es corta a propósito: remite al plan para el detalle. Lo que aporta es el **límite de alcance** (para evitar agregar cosas de más) y los **invariantes**, que son las reglas que cada test tiene que poder señalar que protege.

### 2026-10-07 — Etapa 1: Núcleo del broker sin cableado

**Hecho** (todo en `donaciones-service`, salvo una línea en common-lib):

| Pieza | Archivo | Qué es |
|---|---|---|
| Formato canónico | `dto/logistica/DatosEntregaLogistica.java` | El pedido de entrega en el idioma de Donaciones (D15) |
| Puertos | `services/logistica/ILogisticaBroker`, `IProveedorLogistica`, `IEstrategiaSeleccionProveedor`, `ILogisticaOutbox` | Interfaces (D16) |
| Excepciones | `services/logistica/EnvioProveedorException` (base) + `EnvioRechazadoException`, `EnvioInciertoException`, `ErrorContratoProveedorException` | Lo que lanzan los adapters (D16) |
| Resultado | `services/logistica/ResultadoEnvio` | `PUBLICADO` / `RECHAZADO` / `INCIERTO` / `ERROR_CONTRATO` (D18) |
| Entrada del outbox | `services/logistica/EntradaOutboxLogistica` + `EstadoEntradaOutbox` | Un intento de envío; solo datos; backoff `30 s · 2^n` (D3, D17) |
| Registro | `models/entities/logistica/SolicitudEntrega` + `EstadoSolicitudEntrega`; `models/repositories/ISolicitudesEntregaRepository` + `impl/SolicitudesEntregaRepositoryEnMemoria` | Agregado con transiciones validadas (D15, D21, D22) |
| Broker | `services/impl/LogisticaBroker` | Aplica la regla ante cada resultado; no habla con la red (D10, D18) |
| Estrategia | `services/impl/SeleccionPorPreferenciaConFallback` | Preferido primero; falla al arrancar si el preferido no está en la lista (D14) |
| Outbox | `infrastructure/outbox/LogisticaOutboxEnMemoria` | Implementación interina en memoria (D3) |
| Relay | `infrastructure/outbox/LogisticaOutboxRelay` | `@Scheduled`; llama al adapter, traduce a `ResultadoEnvio` y propaga el `traceId` de la entrada al MDC (D18, D19, D20) |
| Reloj | `config/ClockConfig` | `Clock` inyectable, mismo patrón que logística, para testear el backoff |
| Catálogo de errores | `common-lib/.../ErrorCatalog`: `SOLICITUD_ENTREGA_TRANSICION_INVALIDA("ERR-EST-413")` | D22 |
| Configuración | `application.properties`: `donatrack.logistica.proveedores`, `proveedor-preferido`, `outbox.intervalo-ms`, `outbox.max-intentos`, `outbox.backoff-base-segundos` | Valores por defecto según D17 |

**Tests** `[VERIFIED]` (46 nuevos, todos en verde):

| Test | Casos | Qué protege |
|---|---|---|
| `SolicitudEntregaTest` | 11 | Transiciones válidas e inválidas; no se reasigna un proveedor descartado |
| `EntradaOutboxLogisticaTest` | 4 | Backoff exponencial y agotamiento de intentos |
| `SeleccionPorPreferenciaConFallbackTest` | 4 | Orden preferido + resto; validación de configuración |
| `LogisticaBrokerTest` | 14 | Cada resultado (`PUBLICADO`, `RECHAZADO`, `INCIERTO`, `ERROR_CONTRATO`); una solicitud activa por donación; propagación del `traceId` |
| `LogisticaOutboxRelayTest` | 9 | Traducción excepción → resultado; proveedor sin adapter → `RECHAZADO`; excepción desconocida → `INCIERTO`; un error no corta el loop; MDC |
| `LogisticaBrokerFlujoTest` | 4 | Broker + relay + outbox reales con reloj ajustable. **Un envío incierto nunca llega al otro proveedor**, ni siquiera cuando se agotan los intentos |

**Regresión** `[VERIFIED]`: `mvn test -pl donaciones-service,logistica-service -am` (con `spotless:check` incluido) → common-lib 60/60, donaciones 483/483 (antes 437), logística 355 (1 omitido, igual que el baseline). `ArchitectureFitnessTest` y la carga del contexto de Spring (`DonacionesServiceApplicationTest`) pasan con los beans nuevos.

**Diferencias con el plan:**
- Nombres: el formato canónico es `DatosEntregaLogistica` (no `SolicitudEntregaLogistica`) y el registro es `SolicitudEntrega` (D15).
- Las excepciones están en `services/logistica/`, no en `infrastructure/logistica/` (D16).
- El relay **no** llama al broker para despachar: llama al adapter y le pasa un `ResultadoEnvio` (D18).
- La entrada del outbox guarda el record `DatosEntregaLogistica`, que son datos, no código, en lugar de un JSON en texto. Serializarlo a JSON es responsabilidad de la futura implementación JPA del puerto.
- Estados de la solicitud: `PENDIENTE` / `ENVIADA` / `FALLIDA`. No hay `REENVIADA`: el reenvío se ve en `proveedoresDescartados`.
- `donatrack.logistica.url` sigue en `application.properties` sin uso; se reemplaza en la Etapa 4, junto con la configuración del adapter HTTP.

**Aclaraciones y límites conocidos:**
- Si se pidieran dos entregas **en simultáneo** para la misma donación, el chequeo «una solicitud activa por donación» no es atómico en memoria y podrían crearse dos. Hoy no pasa: `PropuestaDeAsignacionService` procesa cada donación una sola vez por aprobación. Con JPA se resuelve con una restricción única. Va a `DEUDA_TECNICA.md` en la Etapa 8.
- Con la configuración por defecto (`donatrack,externo`) y sin adapters, cualquier pedido terminaría `FALLIDA` (los dos proveedores se tratan como `RECHAZADO`, D20). Hoy no hay impacto, porque nada llama al broker hasta la Etapa 3.
- Revisión: `[SELF_REVIEW_FALLBACK]`. Se corrigieron, entre otras cosas, métodos auxiliares sin estado que no eran `static` (S2325) y la visibilidad del método `enviar` del relay. La revisión independiente queda para el cierre (Etapa 8).

### 2026-10-07 — Etapa 2: Contrato, adapter AMQP y cutover

**Hecho:**

| Lado | Pieza | Qué es |
|---|---|---|
| Contrato | `docs/arquitectura/contratos/schemas/evento-entrega-solicitada-v1.schema.json` + 3 chequeos en `scripts/validate-contracts.js` | El formulario del pedido. El validador pasa 146/146 `[VERIFIED]` |
| Contrato | `evento-donacion-asignada-v1.schema.json` | Solo cambia la descripción: logística ya no lo consume |
| donaciones | `dto/comunicaciones/EventoEntregaSolicitadaV1` | Record del comando |
| donaciones | `config/RabbitMQConfig` | `routingKeyEntregaSolicitada(id)` y alias fijo `entrega.solicitada.v1` en el `DefaultClassMapper` |
| donaciones | `infrastructure/logistica/ProveedorLogisticaAmqp` | Adapter AMQP: publica con `CorrelationData`, espera el acuse (D11, D24) y traduce: devuelto, nack o sin conexión → `EnvioRechazado`; sin acuse a tiempo o error de canal → `EnvioIncierto`. `message_id` = id de la entrada del outbox y `X-Trace-Id` = traceId de la entrada |
| donaciones | `config/LogisticaProveedoresConfig` + `infrastructure/logistica/ProveedoresLogistica` | Arma un adapter AMQP por cada proveedor con `transporte=amqp` (D26), con un template exclusivo con `mandatory=true` |
| donaciones | `infrastructure/outbox/LogisticaOutboxRelay` | Ahora recibe `ProveedoresLogistica` en lugar de un `ObjectProvider` |
| donaciones | `LogisticaBroker` + `SolicitudEntrega` | Reintento por rondas (D27): `SolicitudEntrega.iniciarNuevaRonda()` y contador `ronda` |
| donaciones | `application.properties` | `spring.rabbitmq.publisher-confirm-type=correlated`, `publisher-returns=true`, `donatrack.logistica.acuse-timeout-ms=5000`, `donatrack.logistica.proveedor.donatrack.transporte=amqp` |
| logística | `config/RabbitMQConfig` | Cola `logistica.<instancia>.entregas.solicitadas` bindeada con la clave exacta `entrega.solicitada.<instancia>.v1`; propiedad `logistica.instancia-id` (`LOGISTICA_INSTANCIA_ID`, default `donatrack`) |
| logística | `infrastructure/EntregaSolicitadaEventListener` | Reemplaza a `DonacionAsignadaEventListener`. Misma lógica: idempotencia por `existsByIdDonacion` y mismo manejo de errores |
| logística | Eliminados | `DonacionAsignadaEventListener`, `EventoDonacionAsignadaV1` (de logística) y su test |
| poms | `donaciones-service` y `logistica-service` | `org.testcontainers:rabbitmq` y `junit-jupiter` en scope test (D23) |

**Tests** `[VERIFIED]`:

| Test | Casos | Qué protege |
|---|---|---|
| `ProveedorLogisticaAmqpTest` | 7 | Cada respuesta de RabbitMQ (acuse, devolución, nack, sin acuse, sin conexión, error de canal) se traduce al resultado correcto; sobre con `message_id` y `X-Trace-Id` |
| `LogisticaProveedoresConfigTest` | 2 | Adapters armados desde la configuración; un proveedor sin transporte soportado queda sin adapter |
| `ProveedorLogisticaAmqpRabbitTest` | 2 | **Contra un RabbitMQ real:** el pedido llega solo a la cola del proveedor elegido, con alias `entrega.solicitada.v1`; un pedido a un proveedor sin cola **vuelve devuelto** y no le llega a nadie |
| `MensajeriaLogisticaRabbitTest` (logística) | 1 | **Contra un RabbitMQ real:** cada instancia recibe solo sus pedidos |
| `RabbitMQConfigTest` (logística) | 4 | Binding exacto por instancia; alias del comando; deserialización de un comando real |
| `EntregaSolicitadaEventListenerTest` (logística) | 5 | Los mismos 5 casos del listener anterior, ahora con el comando |
| `LogisticaBrokerTest`, `SolicitudEntregaTest`, `LogisticaBrokerFlujoTest` | +5 | Rondas: nueva ronda con espera cuando todos rechazan; `FALLIDA` cuando se agotan las rondas; flujo «caída pasajera → sale en la ronda siguiente» |

Los tests contra RabbitMQ real usan Testcontainers: sin Docker se saltean (`@DisabledIfDockerUnavailable`) y con Docker corren.

**Regresión** `[VERIFIED]` (`mvn test -pl donaciones-service,logistica-service -am`, con `spotless:check` y Docker): common-lib 60/60, donaciones 498/498 (antes 483), logística 357 (1 omitido, igual que el baseline). `agent-check` pasa. (Antes de D28 eran 360: se quitaron los 3 tests de la limpieza.)

**Hallazgos:**
- `[VERIFIED]` Con `mandatory` y publisher confirms, **el mensaje devuelto queda cargado en el `CorrelationData` antes de que se complete el acuse**. Era el punto «a verificar» del plan (§2.2): el adapter detecta la devolución de forma determinista.
- `[VERIFIED]` `DefaultClassMapper` arma el mapa clase → alias (lo que se escribe en `__TypeId__`) en `afterPropertiesSet()`, que Spring llama porque el mapper es un bean. En producción el header sale con el alias; en un test que lo construye a mano hay que llamarlo explícitamente. El test con RabbitMQ real lo detectó.

**Diferencias con el plan:**
- El template de comandos no es un bean de Spring: lo crea `LogisticaProveedoresConfig`, para no generar dos `RabbitTemplate` candidatos a inyección en el resto del servicio.
- Sin conexión con RabbitMQ (`AmqpConnectException`) se trata como **rechazado**: es seguro que el sobre no salió, así que se prueba con el siguiente proveedor y, si no hay, la ronda se reintenta (D27).
- Se agregó el reintento por rondas (D27), que no estaba en el plan.

**Aclaración — la cola vieja de logística (D28):**
- **Qué es:** `logistica.donaciones.asignadas`, el buzón donde logística recibía `donacion.asignada.v1` antes del broker.
- **Quién la creó:** el código anterior de logística, que la tenía como bean `Queue` + `Binding` en su `RabbitMQConfig`. Spring declara esos beans en RabbitMQ al conectarse.
- **Por qué sigue ahí:** el código nuevo ya no la declara, pero RabbitMQ nunca borra por su cuenta una cola durable. Queda huérfana, sin dueño, y como su binding sigue activo, acumula una copia de cada `donacion.asignada.v1` que nadie lee.
- **Dónde existe:** solo en un servidor de RabbitMQ que corrió la versión vieja y no se recreó desde entonces. Los del proyecto no tienen volúmenes, así que recrear el contenedor la elimina.
- **Historia de la decisión:** primero se implementó una limpieza automática al arrancar (D25), después se le agregó desenganchar el binding, y finalmente se eliminó (D28): un paso de cutover documentado alcanza.

**Pendientes que pasan a otras etapas:**
- **Etapa 3:** sin el cableado, una asignación no genera entrega y el Gate 4 (`FullDistributedDonationE2EIT`) falla. Las Etapas 2 y 3 se mergean juntas.
- **Etapa 8:** catálogo de mensajes, matriz productor-consumidor, regenerar `docs/generated/` (`node scripts/generate-repo-knowledge.js`) y el diagrama de componentes.
- Revisión: `[SELF_REVIEW_FALLBACK]`; la revisión independiente queda para el cierre.

---

### 2026-10-07 — Etapa 3: Cableado

**Qué se hizo.** `PropuestaDeAsignacionService.onPropuestaAprobada` llama al broker por cada fragmentación, justo después de publicar `donacion.asignada.v1` (que se sigue publicando).

| Pieza | Archivo | Qué es |
|---|---|---|
| Dependencia | `services/impl/PropuestaDeAsignacionService` | Campo `ILogisticaBroker` (se inyecta por `@RequiredArgsConstructor`); depende del puerto, no de la implementación |
| Llamada | mismo archivo, `solicitarEntregaALogistica` | Arma `DatosEntregaLogistica` (donación asignada, beneficiario y destino ya resueltos, peso, volumen, hora actual); sin datos de beneficiario no llama (D30); captura y loguea fallas (D30) |

**Tests** (`PropuestaDeAsignacionServiceTest`, 4 nuevos, ninguno existente se modificó salvo el constructor del `setUp`):

| Caso | Qué protege |
|---|---|
| Solicita la entrega con los datos del beneficiario | Mapeo de campos hacia el broker |
| Varias fragmentaciones → una solicitud por cada una | Una entrega por fragmentación |
| Sin datos del beneficiario → no llama al broker | Misma política que `donacion.asignada.v1` |
| El broker falla → se procesan las demás fragmentaciones y se guarda la necesidad | La aprobación no se rompe por el broker |

**Validación.** Baseline previo: `mvn test -pl donaciones-service -am` → `BASELINE_GREEN` (498 tests, 2 skipped). Después: 502 tests, 0 fallos, 2 skipped; `spotless:check` OK. Gate 4 `[VERIFIED]`: 26 tests de `integration-tests` en verde, incluido `FullDistributedDonationE2EIT` (aprobar propuesta → outbox → RabbitMQ → entrega creada en Logística).

**Cómo se corrió el Gate 4.** `./run-preprod-tests.sh` falló al levantar el stack: `quay.io` devolvió `401` al bajar la imagen de MinIO (problema de red/registro local, no del código). Ningún servicio depende de MinIO, así que se levantó el mismo compose sin ese servicio y se repitieron los mismos pasos del script (n8n + `mvn verify -pl integration-tests`). `docker-compose.preprod.yml` y el script no se tocaron salvo lo de abajo.

**Hallazgo.** La primera corrida falló el E2E (25/26): el relay corre cada 10 s y el test espera 8 s la entrega en Logística, así que pasaba o no según el momento del ciclo. Solución: `LOGISTICA_OUTBOX_INTERVALO_MS=1000` en el servicio `donaciones-service` de `docker-compose.preprod.yml` (solo entorno de pruebas; no se tocó ningún assert ni el valor por defecto de producción, D17). Segunda corrida: 26/26.

---

### 2026-10-07 — Etapa 4, parte 1: Adapter HTTP

**Qué se hizo.** El proveedor `externo` puede recibir los pedidos por HTTP: `ProveedorLogisticaHttp` traduce el modelo canónico al contrato de `POST /api/entregas` de Logística y clasifica la respuesta (D31). `LogisticaProveedoresConfig` arma el adapter cuando `transporte=http` (D32). La parte 2 (409 en `EntregasService.crear`, que toca Logística) queda pendiente.

| Pieza | Archivo | Qué es |
|---|---|---|
| Adapter | `infrastructure/logistica/ProveedorLogisticaHttp` | Implementa `IProveedorLogistica`; record privado `PedidoEntregaHttp` con los nombres del contrato de Logística; header `X-Trace-Id` |
| Configuración | `config/LogisticaProveedoresConfig` | Soporta `transporte=http`; `restClient(url, connectMs, readMs)` |
| Properties | `application.properties` | `proveedor.externo.transporte=http`, `url` (`LOGISTICA_EXTERNA_URL`, por defecto `http://localhost:8084`), `connect-timeout-ms`, `read-timeout-ms` |

**Tests** (+21): `ProveedorLogisticaHttpTest` (15, `MockRestServiceServer`: cuerpo, traza, 201/409, 503, 500/502/504, 400/404/422, redirección, conexión rechazada, host, timeout de conexión y de lectura); `ProveedorLogisticaHttpRedRealTest` (3, servidor real en loopback: éxito, timeout de lectura real → incierto, puerto cerrado real → rechazado); `LogisticaProveedoresConfigTest` (+3: http con url, http sin url, AMQP y HTTP juntos). El test existente «sin transporte soportado» usaba `http` como ejemplo de transporte no soportado; ahora usa `grpc`, con la misma intención y las mismas aserciones.

**Validación.** `mvn clean test -pl donaciones-service -am`: 523 tests, 0 fallos; `spotless:check` OK. Con el valor por defecto `externo` tiene adapter pero no hay instancia en `localhost:8084`: solo se usaría si `donatrack` rechaza, y entonces la conexión rechazada vuelve a ser `RECHAZADO` (rondas con backoff, D27). Gate 4 no se repitió tras este cambio.

---

## Q&A

**¿Qué es el «broker» que pide la Entrega 4? ¿Es RabbitMQ?**
No. RabbitMQ es un *message broker* (transporta mensajes). El broker de integración es un componente de software (patrón Broker) que funciona como intermediario: Donaciones le pide «hacé esta entrega» y el broker decide **qué proveedor de logística** la recibe y cómo hablarle. RabbitMQ es uno de los transportes que usa.

**¿Por qué el broker está dentro de `donaciones-service` y no es un microservicio aparte?**
Porque quien decide a quién mandar el pedido es quien lo origina, y porque la Entrega 3 prohíbe que logística invoque a donaciones. Un microservicio aparte agregaría un contenedor, un punto único de falla y un salto de red más, sin ninguna ventaja funcional.

**¿Qué patrones de diseño usa?**
- **Broker:** el intermediario que conoce a los proveedores.
- **Strategy:** el criterio de selección es intercambiable. Es el mismo patrón que ya usamos en los algoritmos de asignación.
- **Adapter:** uno por proveedor, cada uno con su transporte.
- **Canonical Data Model:** un formato interno único (`SolicitudEntregaLogistica`).
- **Message Translator:** el adapter HTTP traduce al formato del proveedor.
- **Outbox:** los pedidos pendientes no se pierden.

**¿Por qué crearon un mensaje nuevo (`entrega.solicitada`) en lugar de reusar `donacion.asignada`?**
Porque son cosas distintas. `donacion.asignada` es un **hecho** («se asignó una donación») que le interesa a varios: Notificaciones, y antes también Logística. `entrega.solicitada` es un **pedido** dirigido a un solo proveedor elegido. Si el pedido viajara en el hecho, todos los que escuchan el hecho lo recibirían y no habría forma de elegir.

**Si un exchange de RabbitMQ reparte mensajes, ¿por qué no elige él al proveedor?**
Porque un TopicExchange **copia** el mensaje a todas las colas cuyo binding coincide; no elige una. Si dos proveedores escuchan la misma dirección, los dos reciben el pedido. La elección la tiene que hacer alguien con criterio de negocio: el broker, antes de publicar.

**¿Poner el nombre del proveedor en la routing key no contradice la regla de que la routing key nunca dice quién escucha?**
Esa regla es para **eventos**, que tienen N interesados desconocidos. Un **comando** va por naturaleza a un destinatario concreto, así que identificarlo es correcto. Formalizamos la regla completa: los eventos se rutean por hecho; los comandos, por destinatario.

**¿Por qué una routing key por proveedor y no un exchange por proveedor?**
Las dos logran que el pedido llegue solo al elegido. La routing key lo hace con la misma infraestructura (un solo exchange, el de Donaciones). El exchange por proveedor suma infraestructura por cada proveedor y deja ambiguo quién es el dueño de cada exchange.

**¿Qué pasa si el broker elige un proveedor que no tiene cola en RabbitMQ?**
Sin protección, RabbitMQ descartaría el mensaje en silencio. Por eso publicamos con `mandatory=true`, que hace que RabbitMQ lo devuelva, y el broker **espera el acuse** de cada envío. Si vuelve devuelto, el broker sabe con certeza que no llegó y lo manda al siguiente proveedor.

**¿Por qué ante un timeout no lo mandan a otro proveedor?**
Porque un timeout no significa que el pedido no llegó; significa que no sabemos. El primer proveedor pudo haber creado la entrega. Si lo mandáramos a otro, la donación podría terminar con dos entregas. Ante la duda reintentamos con el mismo proveedor, que deduplica por donación.

**¿Cómo evitan entregas duplicadas?**
De tres formas:
1. Solo se cambia de proveedor ante un rechazo seguro.
2. Cada proveedor deduplica por `donacionIndependienteId`: el listener de logística ya lo hacía, y agregamos el chequeo al endpoint REST, que responde 409.
3. Los bindings de los comandos son exactos, nunca con comodines.

**¿Por qué no usar un alternate exchange para no perder mensajes?**
Porque con un alternate exchange RabbitMQ considera que el mensaje sí se ruteó, así que no lo devuelve y el broker no se entera de que el proveedor no tenía cola. Rescata el mensaje, pero impide el reenvío automático.

**¿Por qué el broker no llama directamente a los proveedores?**
Para separar **decidir** de **ejecutar**. El relay hace la llamada a la red y anota qué pasó en una de cuatro categorías (publicado, rechazado, incierto, error de contrato). El broker solo aplica la regla sobre esa categoría. Así la regla se prueba como una tabla de casos, sin red ni mocks de RabbitMQ, y las esperas de red nunca ocurren dentro de la lógica de negocio.

**¿Qué pasa si el proveedor tira un error que nadie esperaba?**
Se trata como **incierto**: no sabemos si el pedido salió, así que se reintenta con el mismo proveedor y, si se agotan los intentos, la solicitud queda fallida con un log de error para revisión manual. Lo que nunca hace es probar con otro proveedor, para no arriesgar una entrega duplicada.

**¿Qué pasa si se cae `donaciones-service` con pedidos pendientes?**
Hoy el outbox es en memoria, así que los pedidos pendientes se pierden. Es una limitación declarada: el outbox está detrás de una interfaz (`ILogisticaOutbox`) y sus entradas son solo datos, así que pasarlo a una tabla en PostgreSQL es cambiar la implementación, sin tocar el broker ni sus tests.

**¿Por qué el outbox guarda datos y no una función a ejecutar, como el outbox que ya existía?**
El outbox anterior guarda un `Runnable` (código), que no se puede guardar en una tabla. El nuevo guarda qué donación, a qué proveedor, cuántos intentos y cuándo reintentar: todo eso entra en una fila de base de datos.

**¿Cada cuánto se reintenta un envío incierto?**
El relay revisa la bandeja cada 10 segundos. Un envío incierto se reprograma con espera exponencial (60 s, 120 s, 240 s…), hasta 5 intentos. Son los mismos valores que el outbox existente del servicio, y se pueden cambiar por variable de entorno.

**¿Cómo evitan que una donación tenga dos pedidos de entrega?**
El registro de solicitudes guarda una solicitud por donación. Si llega otro pedido para una donación que ya tiene una solicitud pendiente o enviada, se ignora. Solo se permite una nueva si la anterior falló, por ejemplo para un reintento manual.

**¿Cómo se prueba algo que depende del paso del tiempo, como los reintentos?**
El broker y el relay no leen la hora del sistema directamente: usan un `Clock` inyectado. En los tests se usa un reloj que se puede adelantar a mano, lo que permite verificar que un reintento ocurre a los 60 segundos sin esperar 60 segundos.

**¿Cómo se sabe que un pedido llegó a la logística elegida?**
El adapter manda el sobre como carta certificada: RabbitMQ tiene que confirmar que lo recibió (publisher confirm) y, si no encuentra ninguna cola con esa dirección, lo devuelve (`mandatory`). El adapter espera esa respuesta antes de dar el envío por bueno. Está probado contra un RabbitMQ real: un pedido a un proveedor sin cola vuelve devuelto y no le llega a nadie.

**¿Qué pasa si RabbitMQ está caído?**
Es seguro que el pedido no salió, así que el broker prueba con el siguiente proveedor (por ejemplo, el que usa HTTP). Si todos fallan, no se da por perdido: el broker vuelve a intentar la ronda completa más tarde, con esperas crecientes, hasta 5 veces. Recién ahí la solicitud queda fallida para revisión manual.

**¿Cómo se evita que una logística reciba los pedidos de otra?**
Cada instancia de logística tiene su propia cola y la conecta al exchange con su dirección exacta (`entrega.solicitada.<instancia>.v1`), nunca con comodines. Está probado contra un RabbitMQ real: un pedido para `externo` no le llega a `donatrack`. En un entorno real se sumarían permisos por usuario en RabbitMQ.

**¿Qué pasó con la cola que logística usaba antes?**
La creó la versión anterior de logística y RabbitMQ la conserva aunque el código ya no la declare, porque nunca borra una cola durable por su cuenta. En este proyecto los RabbitMQ son descartables (no guardan estado entre recreaciones), así que alcanza con recrear el contenedor al hacer el cambio. Se evaluó borrarla automáticamente al arrancar y se descartó: resolvía un problema que casi no existe y agregaba casos borde.

**¿Por qué el nombre del tipo de mensaje no cambia aunque la dirección incluya el proveedor?**
La dirección (routing key) cambia por proveedor, pero el tipo del contenido es siempre el mismo formulario. Por eso el header `__TypeId__` lleva un alias fijo, `entrega.solicitada.v1`. Si usara la dirección, el mismo tipo Java tendría varios nombres y no se podría saber cuál escribir al enviar.

**¿El contrato REST del proveedor HTTP lo estamos infiriendo? ¿Un proveedor real nos diría qué nombres espera?**
Sí, el contrato de `externo` sale de nuestro propio código (`CrearEntregaRequestDTO` de Logística), porque en la demo `externo` es una segunda instancia de nuestra Logística (D9); no lo definió un proveedor real. Un proveedor real publicaría una especificación (casi siempre OpenAPI) con endpoint, nombres y tipos de campos, unidades, autenticación y códigos de respuesta, y se escribiría un adapter contra ella: el modelo canónico (`DatosEntregaLogistica`) es el idioma de Donaciones y no cambia, solo se traduce en cada adapter. Hoy están fijos en `ProveedorLogisticaHttp`: el path, los nombres y tipos de campos, las unidades, la ausencia de autenticación y la lectura de códigos (409/503/5xx). Un proveedor real podría usar `200` con error en el cuerpo, `202` para «recibido, lo proceso después» o un header `Idempotency-Key`. Con un proveedor real habría que validar el adapter contra su OpenAPI (tests de contrato). Queda registrado como supuesto `[INFERRED]` en las consecuencias del ADR. Para la defensa: el aporte es el broker y el modelo canónico; un segundo adapter con otro formato no tocaría nada del broker.
