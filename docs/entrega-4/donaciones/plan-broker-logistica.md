# Plan de implementación — Broker de Integración con Logística (Donaciones)

> **Entregable de la Entrega 4**, requerimiento de integración «Broker de Integración con Logística» (`docs/entregas/4/Enunciado-4.pdf`, pág. 24): «Para la integración entre el servicio de donaciones y el servicio de logística deberá implementarse un broker. Este broker deberá permitir seleccionar entre más de 1 servicio de logística disponible, entendiendo que esta el propio servicio construido y otro servicio potencial que cumpla igual objetivo.»
>
> **Versión unificada.** Toma como base el plan de Nico y le integra tres ajustes de una revisión cruzada (ver [Cambios respecto a la versión anterior](#cambios-respecto-a-la-versión-anterior)). Reemplaza al tutorial previo en Google Docs, que partía de un estado anterior del código (Feign + `POST /api/entregas`).
>
> **Rama de trabajo:** `E4_donaciones_broker`. `[OBSERVED]` Existe en `origin` y apunta al mismo commit que `ENTREGA_4` (`40a07571`).
>
> **Nivel de tarea:** ARCHITECTURAL (AGENTS.md §7.0). Cambia la comunicación entre servicios y un contrato AMQP, y revierte una decisión de la reunión del 15/9. Requiere spec, ADR `proposed` y `ENHANCED_REVIEW_REQUIRED`.
>
> **Etiquetas epistémicas** (AGENTS.md §3): `[OBSERVED]` leído en el código · `[DOCUMENTED]` en un ADR o doc · `[INFERRED]` deducido · `[PROPOSED]` no existe aún · `[A VERIFICAR]` supuesto que hay que confirmar antes de implementar.

---

## Cambios respecto a la versión anterior

| # | Cambio | Motivo |
|---|---|---|
| C1 | **Se distingue «rechazado» de «incierto».** Un timeout ya no pasa al siguiente proveedor: se reintenta con el mismo. | Con un timeout, el proveedor pudo haber creado la entrega. Pasar a otro podía dejar **una donación con dos entregas en dos proveedores distintos** (§2.2). |
| C2 | **El relay espera el acuse de RabbitMQ** (`CorrelationData` + `getReturned()`) en lugar de usar un `ReturnsCallback` asincrónico. | Resuelve el punto abierto «cómo se correlaciona la devolución con el outbox». Además elimina `DevolucionSolicitudCallback`, la correlación por header propio, el estado `REENVIADA` y el pool dedicado. El relay ya corre fuera del hilo de la petición, así que esperar no bloquea a nadie (§2.2). |
| C3 | **Deduplicación en el camino REST de Logística.** | `[OBSERVED]` `EntregasService.crear` guarda sin chequear duplicados; solo el listener AMQP usa `existsByIdDonacion`. Sin esto, reintentar contra la instancia HTTP duplica entregas (§2.6). |
| C4 | **Fases reordenadas:** el adapter HTTP va antes que el controller de administración, y este queda marcado como recortable. | La consigna exige dos proveedores, no cambiarlos en caliente (§3). |
| C5 | **Puntos `[A VERIFICAR]` cerrados:** Spring Security no está en ningún `pom.xml`; el camino REST no deduplica; la segunda instancia necesita datos propios. | Verificados en el código (§5). |

---

## Glosario: el correo

Para explicar cada fase usamos esta analogía.

| RabbitMQ / código | En el correo |
|---|---|
| Exchange (`donaciones.exchange`) | La **oficina de correo**: recibe sobres y los reparte, pero no los guarda |
| Cola | El **buzón** de un destinatario: ahí esperan los sobres hasta que los retira |
| Binding | El **cartel** que el destinatario cuelga en la oficina: «los sobres con esta dirección, a mi buzón» |
| Routing key | La **dirección** escrita en el sobre |
| Envelope (headers AMQP: `message_id`, `X-Trace-Id`, `__TypeId__`) | El **sobre**: número de seguimiento, sello de trazabilidad y tipo de formulario que lleva adentro |
| Payload (record `...V1`) | La **carta**: el formulario dentro del sobre |
| Publisher confirm | El **acuse de recibo** de la carta certificada |
| Return (`mandatory`) | **«Devolver al remitente»**: no hay ningún buzón con esa dirección |
| Adapter HTTP | Un **courier privado** que no usa nuestro correo: lo llamamos por teléfono |
| Broker | El **secretario de despacho**: decide con qué empresa mandar cada pedido |
| Outbox | La **bandeja de pendientes** del secretario |

---

## Decisiones

| # | Decisión |
|---|---|
| D1 | El broker vive **dentro de Donaciones** (in-process), con Broker + Adapter + Strategy. Corresponde a la Alternativa A del informe de alineación. |
| D2 | Logística deja de escuchar `donacion.asignada.v1` y pasa a recibir el comando `entrega.solicitada.<proveedorId>.v1`. Revierte lo decidido el 15/9; se avisa al grupo. |
| D3 | El outbox del broker es **en memoria, detrás de un puerto, con entradas basadas en datos**, listo para pasar a JPA (§2.4). |
| D4 | Se implementan los dos transportes: AMQP y HTTP. |
| D5 | La estrategia base es «preferencia configurable + reenvío». Las estrategias adicionales se definen con el equipo, no solo con el agente (§2.1). |
| D6 | Se protegen los endpoints expuestos con una API key por configuración (§2.7). |
| D7 | El comando se dirige con la routing key (`entrega.solicitada.<proveedorId>.v1`), no con un exchange por proveedor (§2.3). |
| D8 | Se publica con `mandatory=true`. Si RabbitMQ devuelve el mensaje porque no encontró ninguna cola, el broker lo reenvía a otro proveedor (§2.2). |
| D9 | Se agrega una segunda instancia de `logistica-service` al docker-compose para la demo (§2.8). |
| D10 | **(C1)** Un envío **rechazado** (seguro que no llegó) pasa al siguiente proveedor. Un envío **incierto** (pudo haber llegado) se reintenta con el **mismo** proveedor (§2.2). |
| D11 | **(C2)** El relay publica y **espera el acuse** con `CorrelationData`. No hay `ReturnsCallback` que dispare reenvíos (§2.2). |
| D12 | **(C3)** El camino REST de Logística (`EntregasService.crear`) deduplica por `donacionId` (§2.6). |
| — | Controller de administración para consultar y cambiar el proveedor preferido en caliente (§2.7). **Es recortable** si el plazo aprieta. |

---

## 0. Estado de partida

### 0.1 Cómo funciona hoy `[OBSERVED]`

**Ida (Donaciones → Logística):**

```
PropuestaDeAsignacionService.onPropuestaAprobada()          (@EventListener sincrónico)
  └─ publicarDonacionAsignada()
       └─ DonacionesEventPublisher.publicarDonacionAsignada()
            └─ RabbitTemplate → donaciones.exchange, routingKey "donacion.asignada.v1"
                 ├─ cola notificaciones.donaciones          → Notificaciones
                 └─ cola logistica.donaciones.asignadas     → DonacionAsignadaEventListener (Logística)
                                                              └─ IEntregasService.crear(...)
```

**Vuelta (Logística → Donaciones):** `logistica.exchange` (`ruta.asignada`, `ruta.iniciada`, `entrega.exitosa`, `entrega.fallida`) → 4 colas de Donaciones → `LogisticaEventListener`. La idempotencia la da `IEventosConsumidosRepository`.

**Lo que no hay:** `[OBSERVED]` ningún servicio configura `mandatory`, publisher confirms ni publisher returns. Los clientes Feign hacia Logística se eliminaron en la migración a AMQP (#883). La propiedad `donatrack.logistica.url` quedó sin uso.

### 0.2 El problema

Donaciones no elige a quién le habla: publica un hecho público y se suscribe quien quiera. Mientras Logística escuche `donacion.asignada.v1`, **no puede haber selección**. Si el broker eligiera otro proveedor, nuestra Logística crearía la entrega igual. Y si dos proveedores se suscriben al mismo hecho, **los dos crean la entrega** (el exchange copia el mensaje a cada cola que coincide; no elige). Por eso este plan también toca `logistica-service` (D2).

### 0.3 Decisiones ya documentadas que este plan respeta o revierte

| Decisión | Fuente | Tratamiento |
|---|---|---|
| Un TopicExchange por servicio productor; Donaciones es dueña de `donaciones.exchange` | `[DOCUMENTED]` ADR `20260911-topologia-pubsub-amqp-...`, `matriz-productor-consumidor.md` | Se respeta. |
| Routing key = hecho/intención, nunca «quién escucha» | `[DOCUMENTED]` `matriz-productor-consumidor.md` | Se respeta para eventos y se acota para comandos. Regla a dejar escrita: **los eventos se rutean por hecho; los comandos, por destinatario.** `EntregaSolicitadaV1` es un comando, una orden a un destinatario concreto, y por eso su routing key lleva el proveedor (§2.3). |
| `EntregaSolicitadaV1` descartado el 15/9; Logística se suscribe a `donacion.asignada.v1` | `[DOCUMENTED]` matriz, bitácora de comunicaciones | **Se revierte** (D2), con justificación en el ADR: el enunciado exige selección, y con un evento público es imposible. |
| Logística no invoca a Donaciones ni a Notificaciones | `[DOCUMENTED]` Enunciado E3 | Se respeta. El broker vive en Donaciones, que es quien origina el pedido. |
| Idempotencia en consumidores; `traceId` propagado | `[DOCUMENTED]` ADR `20260901-patron-de-idempotencia-...`, `logging-trazabilidad.md` | Se respeta (§2.6). |
| Convención `<entidad>Id`, contratos AMQP V1 sin `@JsonAlias` | `[DOCUMENTED]` ADR `20260919-convencion-canonica-...` | Se respeta en el contrato nuevo. |
| Hard cutover en lugar de convivencia de canales | `[DOCUMENTED]` ADR `20260911-topologia-pubsub-...` | Se aplica: un solo PR cambia ambos lados. |
| Evitar acoplamiento temporal síncrono entre servicios | `[DOCUMENTED]` ADR `20260911-topologia-pubsub-...` | Se respeta. Las llamadas HTTP y la espera de acuses salen siempre del relay del outbox, nunca del hilo de la petición. No hay health checks hacia Logística (§2.2). |
| Event-time obligatorio | `[DOCUMENTED]` ADR `20260901-consistencia-temporal-...` | El comando lleva la `fecha` del hecho original. |

> **Nota:** el ADR `20260901-estrategia-de-comunicacion-asimetrica-...` está `superseded`. No hay que citarlo como vigente en la justificación.

---

## 1. Diseño

### 1.1 Piezas y patrones

```
PropuestaDeAsignacionService
   │  publica donacion.asignada.v1 (sin cambios: lo consume Notificaciones)
   │  y además llama a ──▶ ILogisticaBroker
   ▼
LogisticaBroker ──▶ IEstrategiaSeleccionProveedor   (ordena los proveedores)
   │   └ registra la solicitud y deja una entrada PENDIENTE en el outbox (§2.4)
   ▼
LogisticaOutboxRelay  (@Scheduled: toma pendientes, llama al adapter y ESPERA el resultado)
   │   └ informa el resultado al broker: PUBLICADO · RECHAZADO · INCIERTO · ERROR_CONTRATO
   ├──────────────────────────────┐
   ▼                              ▼
ProveedorLogisticaAmqp          ProveedorLogisticaHttp
 (comando con RK por proveedor,   (REST + traducción de contrato)
  mandatory + espera de acuse)
```

| Pieza | Patrón | Por qué |
|---|---|---|
| `IProveedorLogistica` | Puerto / Adapter | Cada proveedor puede tener su protocolo y su contrato. El broker solo conoce la interfaz: sumar un proveedor es configuración o una clase nueva (Open/Closed). |
| `ProveedorLogisticaAmqp` | Adapter | Publica el comando con la routing key de ese proveedor (§2.3) y espera el acuse. Es genérico: se instancia una vez por cada proveedor configurado con transporte `amqp`. |
| `ProveedorLogisticaHttp` | Adapter + Message Translator | Para un proveedor que no habla nuestro AMQP: traduce el modelo canónico a su contrato y lo envía por REST. |
| `SolicitudEntregaLogistica` | Canonical Data Model | Donaciones habla un solo idioma y cada adapter traduce. Si cambia el contrato AMQP, el adapter HTTP no se entera, y al revés. |
| `LogisticaBroker` | Broker | Es el único punto que conoce a los proveedores: los ordena con la estrategia y decide qué hacer con cada resultado (D10). |
| `IEstrategiaSeleccionProveedor` | Strategy | El criterio puede cambiar. `[DOCUMENTED]` Es el mismo patrón que usan los algoritmos de asignación (`20260615-strategy-gestor-algoritmos`). |
| `ILogisticaOutbox` + `LogisticaOutboxRelay` | Transactional Outbox (versión interina en memoria) | El error final no se pierde, y el día de pasar a base de datos solo cambia la implementación (§2.4). |
| `LogisticaProveedorController` | Adaptador HTTP de entrada | Consulta y cambia el proveedor preferido en caliente (recortable). |
| `LogisticaCallbackController` | Adaptador HTTP de entrada | Recibe los avisos de un proveedor HTTP y los traduce a los eventos de siempre (§2.5). |

### 1.2 Por qué el broker vive en Donaciones

El enunciado de E3 prohíbe que Logística invoque a Donaciones, y quien decide a quién mandar es quien origina el pedido. Además, desde el punto de vista de Donaciones, Logística pasa a ser «un proveedor más». Se descartó un microservicio broker dedicado porque suma un contenedor (+250 MB), un punto único de falla y un salto de red, sin ganar nada (informe de alineación, Alternativa B).

### 1.3 Ubicación en capas

`[OBSERVED]` El servicio ya separa puertos y adaptadores: `services/IDonacionesEventPublisher` (puerto) ↔ `infrastructure/events/DonacionesEventPublisher` (adaptador), y las interfaces llevan prefijo `I`. El plan sigue ese mismo estilo:

```
donaciones-service/src/main/java/grupo5/donaciones/
├── services/
│   ├── ILogisticaBroker.java                       NUEVO  (puerto que usa PropuestaDeAsignacionService)
│   ├── IProveedorLogistica.java                    NUEVO  (puerto que implementan los adapters)
│   ├── IEstrategiaSeleccionProveedor.java          NUEVO
│   ├── ILogisticaOutbox.java                       NUEVO  (puerto del outbox basado en datos, §2.4)
│   └── impl/
│       ├── LogisticaBroker.java                    NUEVO  (orden + decisión ante cada resultado)
│       └── SeleccionPorPreferenciaConFallback.java NUEVO  (estrategia base; otras: §2.1)
├── infrastructure/logistica/
│   ├── ProveedorLogisticaAmqp.java                 NUEVO  (genérico, uno por proveedor amqp)
│   ├── ProveedorLogisticaHttp.java                 NUEVO  (REST + traducción)
│   ├── EnvioRechazadoException.java                NUEVO  (seguro que no llegó → reenviar)
│   ├── EnvioInciertoException.java                 NUEVO  (pudo haber llegado → reintentar al mismo)
│   ├── ErrorContratoProveedorException.java        NUEVO  (4xx → no reintentar)
│   └── ProcesadorEventosLogistica.java             NUEVO  (extraído de LogisticaEventListener, §2.5)
├── infrastructure/outbox/
│   ├── LogisticaOutboxEnMemoria.java               NUEVO  (interina; se reemplaza por JPA sin tocar el broker)
│   └── LogisticaOutboxRelay.java                   NUEVO  (@Scheduled)
├── models/repositories/ISolicitudesEntregaRepository.java   NUEVO (+ impl en memoria)
├── controllers/
│   ├── ILogisticaProveedorController.java          NUEVO  (recortable)
│   ├── ILogisticaCallbackController.java           NUEVO
│   └── impl/ {LogisticaProveedorController, LogisticaCallbackController}.java
├── config/ {RabbitMQConfig (CAMBIA), LogisticaProveedoresConfig (NUEVO), ApiKeyFilter (NUEVO)}
├── dto/comunicaciones/EventoEntregaSolicitadaV1.java   NUEVO (contrato AMQP)
├── dto/logistica/ {SolicitudEntregaLogistica, ProveedorLogisticaDTO,
│                   PreferenciaProveedorRequestDTO, AvisoProveedorRequestDTO}.java  NUEVOS
└── infrastructure/LogisticaEventListener.java      CAMBIA: delega en el procesador
```

Ya **no** forman parte del diseño (por C2): `DevolucionSolicitudCallback`, la correlación por header propio, el estado `REENVIADA` y el pool dedicado al reenvío.

`[OBSERVED]` Las reglas ArchUnit de `ArchitectureFitnessTest` exigen tres cosas: que las entidades de dominio no dependan de `controllers`/`infrastructure`, que los `@RestController` estén en `controllers..` y que no inyecten repositorios. El diseño las cumple: el broker (capa de aplicación) depende de puertos y los adapters (infraestructura) los implementan.

---

## 2. Decisiones de diseño

### 2.1 Selección de proveedor (D5)

> 🗣️ **En criollo:** el secretario tiene una regla escrita para decidir a qué empresa llamar primero y a cuál después si la primera devuelve el sobre.

`[PROPOSED]` La estrategia base es `SeleccionPorPreferenciaConFallback`: primero el proveedor preferido, y los demás quedan como orden de reenvío.

```properties
donatrack.logistica.proveedores=${LOGISTICA_PROVEEDORES:donatrack,externo}
donatrack.logistica.proveedor-preferido=${LOGISTICA_PROVEEDOR:donatrack}
donatrack.logistica.proveedor.donatrack.transporte=amqp
donatrack.logistica.proveedor.externo.transporte=http
donatrack.logistica.proveedor.externo.url=${LOGISTICA_EXTERNA_URL:http://localhost:8084}
donatrack.logistica.proveedor.externo.connect-timeout-ms=${LOGISTICA_EXTERNA_CONNECT_TIMEOUT_MS:1000}
donatrack.logistica.proveedor.externo.read-timeout-ms=${LOGISTICA_EXTERNA_READ_TIMEOUT_MS:3000}
donatrack.logistica.acuse-timeout-ms=${LOGISTICA_ACUSE_TIMEOUT_MS:5000}
```

- `donatrack.logistica.url` (sin uso) se reemplaza por esta estructura.
- Un id que no esté en `donatrack.logistica.proveedores` se rechaza al arrancar y al cambiarlo por el controller.
- El preferido en runtime vive en memoria (`AtomicReference`), inicializado desde la propiedad. No persiste entre reinicios.

**Estrategias adicionales (a definir con el equipo, no solo con el agente).** Se inventan para poder probar el broker: no hay una regla de negocio real detrás, así que el grupo tiene que acordar el criterio antes de implementarlas (Fase 1). Candidatas para arrancar la conversación:

| Candidata | Idea | Qué permite probar |
|---|---|---|
| `PorPreferencia` (base) | El preferido por configuración; el resto como reenvío. | Selección fija y reenvío. |
| `RoundRobin` | Alterna proveedores entre solicitudes. | Que la elección cambie entre llamadas. |
| `PorCarga` | Elige el proveedor con menos solicitudes `ENVIADA` en curso (o con un tope configurable). | Que la estrategia use el registro de solicitudes. |
| `PorZona` | Elige según el destino (provincia o código postal). | Que la estrategia dependa del contenido de la solicitud. |

Cada estrategia es una clase chica con sus tests. Solo se implementan las acordadas, y se documentan como **estrategias de prueba** en el ADR y en el código.

### 2.2 Resultado de cada envío: rechazado, incierto o error de contrato (D8, D10, D11)

> 🗣️ **En criollo:** mandamos todo por carta certificada y **esperamos el acuse**. Si la oficina devuelve el sobre porque la dirección no tiene buzón, el secretario prueba con otra empresa. Si el acuse no llega a tiempo, no sabemos si el sobre entró, así que **no** se lo mandamos a otra empresa (podría terminar en dos lados): lo dejamos en la bandeja para reintentar con la misma.

**El problema** `[OBSERVED]` + `[INFERRED]`: un TopicExchange descarta en silencio un mensaje que no coincide con ningún binding. Si se elige un proveedor AMQP cuya cola no existe, la entrega desaparece sin dejar ni un log.

**Cuatro resultados posibles:**

| Resultado | Cuándo | ¿Llegó? | Qué hace el broker |
|---|---|---|---|
| **PUBLICADO** | AMQP: acuse positivo sin devolución. HTTP: 2xx, o 409 «ya existía» (§2.6). | Sí | Marca la entrada `PUBLICADO` y la solicitud `ENVIADA` a ese proveedor. |
| **RECHAZADO** (`EnvioRechazadoException`) | AMQP: el mensaje fue **devuelto** (`getReturned() != null`) o hubo **nack**. HTTP: **conexión rechazada** o **503**. | **Seguro que no** | Descarta ese proveedor **para esta donación**, le pide a la estrategia el siguiente y crea una entrada nueva. Si no queda ninguno, la solicitud queda `FALLIDA` y se loguea a nivel `error`. |
| **INCIERTO** (`EnvioInciertoException`) | AMQP: **no llegó el acuse** dentro de `acuse-timeout-ms`. HTTP: **timeout de lectura**, **500**, **502** o **504**. | **No se sabe** | **No reenvía a otro proveedor.** La entrada queda `PENDIENTE` con backoff **contra el mismo proveedor**. La deduplicación del proveedor absorbe el duplicado (§2.6). Si agota los intentos, la entrada queda `FALLIDO` y la solicitud `FALLIDA` con log `error`, para revisión manual. |
| **ERROR_CONTRATO** (`ErrorContratoProveedorException`) | HTTP: **4xx** distinto de 409. | No | No se reintenta ni se reenvía. Es un bug de traducción o de datos y reenviarlo lo escondería. La entrada queda `FALLIDO` con log `error`. |

**Por qué se distingue incierto de rechazado (C1):** si un timeout pasara al siguiente proveedor, el primero pudo haber creado la entrega igual, y la donación quedaría **con dos entregas en dos empresas distintas**. Un reenvío solo es seguro si hay certeza de que no llegó.

**Cómo se detecta la devolución (C2):** el adapter AMQP publica con `CorrelationData` y espera:

```java
CorrelationData cd = new CorrelationData(entrada.id().toString());
rabbitTemplateComandos.convertAndSend(
    RabbitMQConfig.EXCHANGE_DONACIONES, routingKeyDe(proveedorId), comando, headersTrazabilidad(entrada), cd);

CorrelationData.Confirm confirm;
try {
  confirm = cd.getFuture().get(acuseTimeoutMs, TimeUnit.MILLISECONDS);
} catch (TimeoutException e) {
  throw new EnvioInciertoException(proveedorId, "sin acuse de RabbitMQ", e);
} catch (InterruptedException e) {
  Thread.currentThread().interrupt();
  throw new EnvioInciertoException(proveedorId, "interrumpido esperando acuse", e);
} catch (ExecutionException e) {
  throw new EnvioInciertoException(proveedorId, "error de canal AMQP", e);
}
if (cd.getReturned() != null) throw new EnvioRechazadoException(proveedorId, "sin cola para la routing key");
if (!confirm.ack())          throw new EnvioRechazadoException(proveedorId, "nack: " + confirm.reason());
```

- `[DOCUMENTED]` (Spring AMQP) Con `mandatory`, el mensaje devuelto queda cargado en `CorrelationData` **antes** de que se complete el acuse, así que alcanza con chequearlo después del `get`. `[A VERIFICAR]` con un test de Testcontainers en la Fase 2.
- `[INFERRED]` Sin `mandatory`, RabbitMQ confirma (ack) también los mensajes que no rutea, y el relay los daría por entregados. Por eso los dos mecanismos van juntos.
- Esperar es aceptable porque **el relay corre en su propio hilo `@Scheduled`**, nunca en el de la petición. `[OBSERVED]` `onPropuestaAprobada` es un `@EventListener` sincrónico, y el broker solo registra la solicitud y la entrada del outbox, no publica.
- Con esto ya no hacen falta el `ReturnsCallback` que dispara reenvíos, la correlación por header, el estado `REENVIADA` ni el pool dedicado.

**Configuración:**

```properties
spring.rabbitmq.publisher-confirm-type=correlated
spring.rabbitmq.publisher-returns=true
```

- **Template dedicado al comando** (`rabbitTemplateComandos`) con `setMandatory(true)`. El template compartido de los otros 9 eventos no cambia de comportamiento. Puede tener un `ReturnsCallback` **solo de log**, aunque no es parte de este alcance.
- `[OBSERVED]` `publisher-confirm-type` afecta a la connection factory compartida. Los publishers que no pasan `CorrelationData` siguen funcionando igual.

**Sin health checks.** No se consulta `/actuator/health` de Logística desde Donaciones: eso reintroduciría el acoplamiento síncrono que el ADR `20260911` elimina.

**Límites (van al ADR):**
- `mandatory` solo detecta «no hay ninguna cola». **No** detecta que el consumidor esté caído: si la cola existe, el mensaje se rutea y espera a que Logística vuelva. Eso es resiliencia por mensajería, no failover por disponibilidad.
- `[INFERRED]` **Carrera de primera vez:** si Donaciones publica antes de que el proveedor declare su cola, el mensaje vuelve y el broker pasa al siguiente. Antes el mismo caso pasaba en silencio con `donacion.asignada.v1`; ahora es visible.
- El relay procesa entradas de a una y cada una puede esperar hasta `acuse-timeout-ms`. Alcanza para el volumen actual (asignaciones por lote). Si en el futuro hiciera falta más throughput, se paraleliza el relay.

### 2.3 Contrato AMQP nuevo y topología (D7)

> 🗣️ **En criollo:** diseñamos el formulario oficial de «pedido de entrega» y le escribimos en el sobre la dirección de la empresa elegida. Cada empresa cuelga en la oficina un cartel con **su** dirección exacta, y nada de «todo lo que empiece con…».

`[PROPOSED]` Nuevo mensaje `EntregaSolicitadaV1` en `donaciones.exchange` (un único exchange, el del productor), con routing key `entrega.solicitada.<proveedorId>.v1`. Para el proveedor propio: `entrega.solicitada.donatrack.v1`. Solo se usa con proveedores AMQP; el proveedor HTTP no tiene routing key.

```
                         ┌─ binding "entrega.solicitada.donatrack.v1" ─▶ [logistica.donatrack.entregas.solicitadas]
Donaciones ─▶ [ donaciones.exchange ]
                         └─ binding "entrega.solicitada.otra.v1"      ─▶ [logistica.otra.entregas.solicitadas]
```

- **Una cola por consumidor**, declarada y bindeada por el propio consumidor. Donaciones no declara colas ajenas (`[DOCUMENTED]` matriz y ADR `20260911`).
- **Alternativa descartada: un exchange por proveedor.** Da el mismo aislamiento con más infraestructura, y deja abierta la pregunta de quién es el dueño de cada exchange.
- **Los bindings son exactos, nunca con comodín.** `[INFERRED]` Un binding `entrega.solicitada.#` recibiría las solicitudes de todos los proveedores y duplicaría entregas. Con credenciales compartidas, RabbitMQ no lo impide: la protección es la convención más un test (Fase 2). Para un entorno real, ver §4.
- **Cada instancia de Logística necesita su propio nombre de cola**, no solo su propia routing key. Si dos instancias usan la misma cola con distinta routing key, la cola queda con dos bindings y las instancias compiten por ella, lo que anula la selección. El nombre se parametriza con `LOGISTICA_INSTANCIA_ID` (default `donatrack`).
- **El alias de tipo (`__TypeId__`) es independiente de la routing key.** `[OBSERVED]` Hoy cada `idClassMapping` usa la routing key como alias. Con varias claves para una misma clase, el mapeo inverso sería ambiguo. `[PROPOSED]` Un alias único y estable, `entrega.solicitada.v1 → EventoEntregaSolicitadaV1`, en ambos `RabbitMQConfig`. `[A VERIFICAR]` con un test de serialización.
- **Payload:** `donacionIndependienteId`, `personaBeneficiariaId`, `destino`, `pesoTotalKG`, `volumenTotalM3` (`[OBSERVED]` los que `mapearACrearEntregaRequestDTO` ya usa) más `fecha`, la del hecho original. Nombres según la convención `<entidad>Id`.
- **Contrato JSON:** se agrega `docs/arquitectura/contratos/schemas/evento-entrega-solicitada-v1.schema.json`, que tiene que pasar `scripts/validate-contracts.js`. El record se replica en cada servicio, sin `@JsonAlias`.
- **En Logística (D2):** la cola se bindea a `entrega.solicitada.<LOGISTICA_INSTANCIA_ID>.v1` en lugar de `donacion.asignada.v1`. `DonacionAsignadaEventListener` pasa a llamarse `EntregaSolicitadaEventListener`; el mapeo a `CrearEntregaRequestDTO` y la deduplicación quedan iguales.
- **Cutover** `[INFERRED]`: la cola vieja `logistica.donaciones.asignadas` es durable y conserva su binding aunque el código deje de declararla, así que seguiría acumulando mensajes que nadie consume. El cutover incluye **eliminar la cola y el binding viejos en cada ambiente**.
- `donacion.asignada.v1` no cambia y queda con un solo consumidor: Notificaciones. En el catálogo y la matriz, Logística sale de sus consumidores y se agrega la fila del comando con la regla «eventos por hecho, comandos por destinatario».

### 2.4 Outbox del broker: el error final no se pierde (D3)

> 🗣️ **En criollo:** el secretario no confía en su memoria. Anota cada pedido en una planilla (qué donación, a qué empresa, cuántos intentos, en qué estado) y un ayudante la recorre cada tantos segundos para despachar lo pendiente. Hoy la planilla es en lápiz (memoria); mañana pasa a tinta (base de datos) sin cambiar el procedimiento.

`[OBSERVED]` Hoy `publicarDonacionAsignada` atrapa cualquier excepción y solo la loguea. Si falla, la donación queda asignada sin entrega y nadie se entera. El broker no debe repetir ese patrón.

`[OBSERVED]` Ya existen `OutboxStore` y `OutboxRetryScheduler` (reintentos con backoff y dead-letter), pero cada entrada guarda un `Runnable` (`entry.getAccion().run()`) en un `ConcurrentHashMap`. Una lambda no se puede guardar en una tabla, así que reutilizarlo obligaría a rediseñarlo el día de pasar a base de datos. `[DOCUMENTED]` El outbox transaccional real depende de la migración a PostgreSQL (ADR `20260901-patron-transactional-outbox-...`, `proposed`).

`[PROPOSED]` El broker **no** reutiliza `OutboxStore`. Usa un outbox basado en datos, detrás de un puerto:

- `ILogisticaOutbox` (puerto) + `LogisticaOutboxEnMemoria` (implementación interina) + `LogisticaOutboxRelay` (`@Scheduled`).
- Cada entrada guarda **datos, no código**: `id`, `donacionIndependienteId`, `proveedorId`, `payload` (JSON canónico), `intentos`, `maxIntentos`, `proximoIntento`, `estado` (`PENDIENTE` / `PUBLICADO` / `FALLIDO`) y `traceId`. El adapter de cada proveedor deriva de ahí la routing key o la URL.
- **Flujo:** el broker guarda la entrada `PENDIENTE` → el relay se la pasa al adapter del proveedor y **espera** → informa el resultado al broker, que aplica la tabla de §2.2.
- **Al pasar a base de datos** solo cambia la implementación (`LogisticaOutboxJpa`), y el relay lee con `SELECT ... FOR UPDATE SKIP LOCKED`, como dice el ADR. El broker y los tests no se tocan.

**Qué se prueba y qué no (va al ADR):** con el outbox en memoria se prueba el **comportamiento** (reintentos, backoff, dead-letter, reenvío). Todavía faltan las dos garantías del patrón: **atomicidad** (estado y mensaje en una misma transacción) y **durabilidad** (los pendientes se pierden si el servicio se reinicia). Es deuda declarada en `DEUDA_TECNICA.md`.

### 2.5 Camino de vuelta (D4)

> 🗣️ **En criollo:** nuestra empresa de reparto nos avisa por correo, como siempre. El courier privado nos llama por teléfono, y anotamos lo que nos dice en el mismo formulario que usan las cartas, para que lo procese la misma persona.

- **Proveedor AMQP** (incluida otra instancia de nuestro `logistica-service`): publica en `logistica.exchange` los mismos eventos de hoy, y Donaciones los consume por `donacionIndependienteId`. No cambia nada en Donaciones. `[A VERIFICAR]` Para un proveedor real que no sea nuestra imagen, quién es dueño de ese exchange queda como límite a registrar en el ADR.
- **Proveedor HTTP:** necesita su propio camino de vuelta. Sin él, el proveedor podría tomar la entrega y la donación nunca avanzaría de estado.
  1. Extraer de `LogisticaEventListener` la lógica de `aplicarCambioEstado` (con idempotencia) a `ProcesadorEventosLogistica`, para que la misma idempotencia aplique a los dos caminos.
  2. `LogisticaCallbackController` (`POST /api/logistica/proveedores/{proveedorId}/avisos`) traduce el aviso a los mismos records (`EventoRutaAsignada`, `EventoRutaIniciada`, `EventoEntregaExitosa`, `EventoEntregaFallida`) y delega en el procesador. No tiene lógica de dominio (AGENTS.md §4.2).
  3. Está protegido con una API key por proveedor (§2.7) y con una **verificación de pertenencia**: el aviso de un proveedor solo se acepta para una donación que el registro de solicitudes (§2.6) le asignó a ese mismo proveedor.

### 2.6 Idempotencia, correlación y trazabilidad (D12)

> 🗣️ **En criollo:** cada empresa tiene que reconocer un pedido repetido («esta donación ya la tengo») y no armar dos entregas. Nosotros, además, llevamos el registro de qué empresa tomó cada donación.

- **Clave de idempotencia:** `donacionIndependienteId`.
  - Camino AMQP: `[OBSERVED]` Logística ya deduplica con `existsByIdDonacion` en el listener.
  - Camino REST: `[OBSERVED]` **no deduplica.** `EntregasService.crear` (`logistica-service/.../services/impl/EntregasService.java`) guarda la entrega sin chequear nada. `[PROPOSED]` (C3) Agregar el chequeo en `EntregasService.crear` y, si la entrega ya existe, responder **409** con un código del catálogo de errores. El adapter HTTP toma 409 como `PUBLICADO` («ya estaba»). `[A VERIFICAR]` qué código de `ErrorCatalog` corresponde, según el ADR `20260903-estandarizacion-de-codigos-de-estado-http`.
  - Esto es lo que vuelve **seguro** el reintento contra el mismo proveedor ante un envío incierto (D10). Si un proveedor no deduplica, un incierto puede generar duplicados en ese proveedor. Esto va como requisito de contrato en el ADR.
- **Registro de la solicitud:** `ISolicitudesEntregaRepository` (en memoria, como el resto de los repositorios del servicio) guarda `donacionId`, proveedor actual, proveedores descartados y estado (`PENDIENTE` / `ENVIADA` / `FALLIDA`). Sirve para elegir el siguiente sin repetir un proveedor ya descartado, para no reenviar una donación ya tomada, para validar el callback (§2.5) y para replanificación y auditoría.
- **`traceId`:** se propaga en el header AMQP (`X-Trace-Id`, `[OBSERVED]` ya lo agrega `DonacionesEventPublisher`) y en el mismo header de las llamadas HTTP. Se guarda en la entrada del outbox, así que se conserva en los reintentos y reenvíos.

### 2.7 Controller de administración y protección de endpoints (D6)

> 🗣️ **En criollo:** una ventanilla para que el administrador vea qué empresas hay y cambie la preferida sin reiniciar nada, y un portero que pide credencial antes de dejar pasar a cualquiera.

| Método | Ruta | Función | Protección | ¿Recortable? |
|---|---|---|---|---|
| GET | `/api/logistica/proveedores` | Lista los proveedores configurados (id, transporte, cuál es el preferido). | API key de administración | Sí |
| PUT | `/api/logistica/proveedor-preferido` | Cambia el preferido en caliente. Body: `{ "proveedorId": "externo" }`. Un id no configurado devuelve el error del catálogo (ADR `20260903-estandarizacion-de-codigos-de-estado-http`). | API key de administración | Sí |
| POST | `/api/logistica/proveedores/{proveedorId}/avisos` | Callback del proveedor HTTP (§2.5). | API key del proveedor | **No**, si se expone el callback |

**Protección implementada (no solo recomendada):**
- `ApiKeyFilter` en `config/`, un `OncePerRequestFilter` común que exige el header `X-API-Key` en estas rutas. `[OBSERVED]` **Spring Security no está en ningún `pom.xml`**. No se agrega, porque sería una dependencia nueva que requiere aprobación (AGENTS.md §6).
- Claves por configuración (`donatrack.logistica.admin-api-key` y `donatrack.logistica.proveedor.<id>.callback-api-key`), leídas de variables de entorno. **Nunca hardcodeadas** (AGENTS.md §4.3); los tests usan valores sintéticos.
- **Falla cerrado:** si no hay clave configurada, el endpoint rechaza todo.
- Comparación en tiempo constante (`MessageDigest.isEqual`). Responde 401 sin clave o con clave incorrecta. Las claves no se loguean.
- La clave por proveedor le da identidad real al callback: sin ella no se sabría qué proveedor está avisando.
- No reemplaza al `auth-service` de la Entrega 6: es una protección mínima y así se declara en el ADR.

### 2.8 Demo sin proveedor real: una segunda instancia de Logística (D9)

> 🗣️ **En criollo:** para mostrar «otra empresa de reparto», abrimos una segunda sucursal de la nuestra y la tratamos como si fuera un courier privado: le hablamos por teléfono (HTTP) y no por correo.

`[PROPOSED]` Se levanta una segunda instancia de `logistica-service` (`LOGISTICA_INSTANCIA_ID=externo`, con su propio puerto) y se la configura como **proveedor HTTP**. Donaciones le habla por REST (`POST /api/entregas`), traduciendo el modelo canónico, así que usa un transporte y un contrato distintos a los del proveedor `donatrack` (AMQP).

- `[OBSERVED]` En `docker-compose.yml`, `logistica-service` usa la imagen local `donatrack/logistica-service:local`. La segunda instancia es otra entrada del compose con la misma imagen y otras variables de entorno.
- `[INFERRED]` **Necesita sus propios datos:** cada instancia tiene repositorios en memoria independientes, así que la instancia `externo` necesita su flota de camiones (y choferes) cargada para poder planificar rutas en la demo.
- `[INFERRED]` **Su vuelta llega por AMQP, no por el callback:** al ser nuestra misma imagen, publica sus eventos en `logistica.exchange` con las mismas routing keys, y Donaciones los procesa por `donacionIndependienteId`. Así hay que contarlo en la defensa. El callback HTTP se demuestra con Postman.

**Guion de demo:**
- **Alternar en vivo:** el controller admin, o la variable `LOGISTICA_PROVEEDOR` + reinicio si se recortó el controller, cambia el preferido entre `donatrack` (AMQP) y `externo` (HTTP).
- **Reenvío por devolución (AMQP):** se configura un proveedor AMQP `otra` sin levantar su instancia. Al elegirlo, RabbitMQ devuelve el mensaje y el broker pasa al siguiente.
- **Reenvío por rechazo HTTP:** se baja la instancia `externo` y se la elige como preferida. La conexión es rechazada y el broker pasa al siguiente.
- **Envío incierto:** con un `read-timeout-ms` muy bajo contra la instancia `externo`, el broker **no** cambia de proveedor y reintenta con el mismo. La entrega se crea una sola vez gracias al 409 (D12).
- **Callback:** con Postman, llamando al endpoint de avisos con la API key del proveedor. También se muestra que un aviso de otro proveedor es rechazado.

**Sin WireMock ni dependencias nuevas.** Los tests del adapter HTTP usan `MockRestServiceServer` sobre `RestClient.Builder`. `[A VERIFICAR]` que esté disponible con el `spring-boot-starter-test` actual (`[INFERRED]` viene en `spring-test`).

---

## 3. Fases de implementación

Cada fase es verificable e independiente. No se avanza sin baseline ni sin cerrar la anterior. Siguiendo la regla del equipo, **se explica cada fase y se espera confirmación antes de tocar código.** Todo va en la rama `E4_donaciones_broker`.

| Fase | 🗣️ En criollo | Contenido | Validación |
|---|---|---|---|
| **0 — Preparación** | Antes de abrir la oficina, revisamos que todo lo que ya existe funcione. | Rama `E4_donaciones_broker`. Spec corta (Goal / Scope / Constraints / Validation). ADR `proposed`. Baseline `mvn test -pl donaciones-service` y `-pl logistica-service`. **Acuerdo del grupo para revertir la decisión del 15/9.** | Baseline registrado (`BASELINE_GREEN/RED`) |
| **1 — Núcleo sin cableado** | Contratamos al secretario, le damos su regla y su planilla, todavía sin conectarlo a ninguna empresa real. | `ILogisticaBroker`/`LogisticaBroker`, `IProveedorLogistica`, `IEstrategiaSeleccionProveedor`, `SeleccionPorPreferenciaConFallback`, `SolicitudEntregaLogistica`, las tres excepciones (§2.2), `ISolicitudesEntregaRepository` + impl en memoria, `ILogisticaOutbox` + `LogisticaOutboxEnMemoria` + `LogisticaOutboxRelay`. Sesión con el equipo para acordar las estrategias adicionales (§2.1) e implementarlas. | `mvn test -pl donaciones-service -Dtest='LogisticaBroker*Test,LogisticaOutbox*Test'` con proveedores fake y reloj controlado: usa el preferido · **rechazado → pasa al siguiente** · **incierto → NO pasa al siguiente, reintenta al mismo con backoff** · error de contrato → no reintenta ni reenvía · no repite un proveedor descartado · todos descartados → `FALLIDA` + log `error` · incierto agotado → `FALLIDA` + log `error` · no reenvía una donación ya tomada · cada estrategia acordada |
| **2 — Contrato, adapter AMQP y cutover** | Imprimimos el formulario de pedido, mandamos por carta certificada esperando el acuse, y nuestra Logística cambia su cartel en la oficina. | `EventoEntregaSolicitadaV1` + schema validado por `validate-contracts.js`. Alias de tipo único en ambos `RabbitMQConfig`. `rabbitTemplateComandos` con `mandatory` + confirms + returns. `ProveedorLogisticaAmqp` con espera de acuse (§2.2). En Logística: `LOGISTICA_INSTANCIA_ID`, cola y binding exactos por instancia, `EntregaSolicitadaEventListener`. Eliminación de la cola y el binding viejos. **Un solo PR para ambos servicios.** | Gate 2 de ambos módulos. Test de serialización del alias. Test de contrato AMQP. **Testcontainers con RabbitMQ real:** routing key sin binding → `getReturned() != null` → `EnvioRechazado`; routing key con binding → `PUBLICADO`; la cola propia no recibe claves ajenas |
| **3 — Cableado** | Cuando se aprueba una asignación, además de publicar la noticia, le pasamos el pedido al secretario. | `PropuestaDeAsignacionService` llama a `ILogisticaBroker` después de publicar `donacion.asignada.v1`, dentro del mismo loop por fragmentación y reusando `DatosBeneficiario`. | Gate 2. Los tests de `PropuestaDeAsignacionService` existentes siguen en verde sin debilitarlos. El broker se invoca una vez por fragmentación. `donacion.asignada.v1` se sigue publicando |
| **4 — Adapter HTTP + dedup REST en Logística** *(antes era la Fase 5)* | Damos de alta al courier privado: lo llamamos por teléfono con su propio formulario, y nuestra Logística aprende a decir «esta donación ya la tengo». | `ProveedorLogisticaHttp` (traducción + `RestClient` con timeouts de properties, **mapeo de errores según la tabla de §2.2**). `LogisticaProveedoresConfig`, que arma los proveedores configurados. **En Logística: chequeo `existsByIdDonacion` en `EntregasService.crear` → 409** (D12). | Tests del adapter (`MockRestServiceServer`): 2xx → `PUBLICADO` · 409 → `PUBLICADO` · conexión rechazada / 503 → `EnvioRechazado` · timeout / 500 / 502 / 504 → `EnvioIncierto` · otros 4xx → `ErrorContrato`. Test en Logística: el segundo `crear` con la misma donación devuelve 409 y no duplica |
| **5 — Vuelta del proveedor HTTP + protección** | El courier nos puede llamar para contar cómo fue la entrega, pero solo si se identifica y solo sobre donaciones que le dimos a él. | `ProcesadorEventosLogistica` extraído, `LogisticaCallbackController`, `ApiKeyFilter` con la API key por proveedor, verificación de pertenencia donación↔proveedor, tests de idempotencia compartida. | Los tests existentes de `LogisticaEventListener` siguen en verde. 401 sin clave. Un aviso de otro proveedor es rechazado. Un aviso duplicado no cambia el estado dos veces |
| **6 — Controller admin** *(antes era la Fase 4; recortable)* | La ventanilla para cambiar la empresa preferida sin reiniciar. | `LogisticaProveedorController`, DTOs, la clave de administración en `ApiKeyFilter`. | `@WebMvcTest`: 200 con clave, 401 sin clave o con clave incorrecta, falla cerrado sin clave configurada. ArchUnit en verde |
| **7 — Demo** | Abrimos la segunda sucursal y ensayamos la función. | Segunda instancia de Logística en el docker-compose con su flota cargada, colección Postman, guion de defensa (§2.8). | Gate 4 (o `[DEFERRED_NO_DOCKER]`) |
| **8 — Cierre** | Actualizamos la guía de la oficina. | `catalogo-mensajes.md`, matriz, ADR, diagrama de componentes (entregable 4 de E4), `docs/README.md`, `ESTADO_DOCUMENTACION.md`, `DEUDA_TECNICA.md`, pre-flight SonarCloud, `mvn spotless:check`, Gate 3/4. | Checklist AGENTS.md §12 |

**Orden de recorte si el plazo aprieta:** primero la Fase 6 (controller admin; el preferido se cambia por variable de entorno + reinicio), después las estrategias adicionales de la Fase 1 (queda solo la base). **No** se recortan la Fase 4 (sin ella no hay segundo proveedor y no se cumple la consigna) ni la API key del callback si se expone ese endpoint.

---

## 4. ADR requerido (Two-Gate Rule)

- **Gate A (decisión nueva):** sí. Introduce el patrón broker y revierte el descarte de `EntregaSolicitadaV1`.
- **Gate B (significancia):** sí. Afecta la integración entre servicios y un contrato AMQP.
- ⇒ **ADR `proposed` en la raíz de `docs/adr/`**, porque toca dos servicios. `[OBSERVED]` Los ADR transversales, como `20260911-topologia-...`, ya están ahí. Por ejemplo: `2026MMDD-broker-de-integracion-con-logistica.md`. Ningún agente puede promoverlo a `accepted` (AGENTS.md §9.3).

**Relaciones:** *evoluciona* `20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones`; *revisa* la decisión del 15/9 registrada en `docs/entrega-4/donaciones/bitacora-comunicaciones.md`.

**Drivers:** cumplir la consigna (selección entre ≥2 proveedores), extensibilidad (un proveedor nuevo es configuración o un adapter), higiene de contratos (el hecho de dominio no se contamina con datos de ruteo), mantener la asincronía con nuestra propia Logística, **no perder ni duplicar entregas**.

**Alternativas a documentar:**

| Alternativa | Veredicto |
|---|---|
| `if/switch` por proveedor en `PropuestaDeAsignacionService` | ❌ Mezcla orquestación de negocio con integración. Cada proveedor nuevo obliga a modificar el servicio. |
| Balanceo de infraestructura (Spring Cloud LoadBalancer, API Gateway) | ❌ Sirve para réplicas del mismo servicio con el mismo contrato. Acá elegir es una decisión de negocio. |
| Consumidores competitivos en una sola cola | ❌ No hay control sobre quién toma cada entrega. |
| Mantener `donacion.asignada.v1` como único canal | ❌ No hay selección y se duplican las entregas. |
| Campo `proveedorLogistica` en `donacion.asignada.v1` + filtro en cada proveedor (Message Filter) | ❌ Mete datos de ruteo en un hecho que escucha Notificaciones y obliga a cada proveedor a filtrar. |
| Microservicio broker dedicado | ❌ Un servicio más, un punto único de falla y un salto de red adicional. |
| Un exchange por proveedor | ❌ Mismo aislamiento con más infraestructura y la propiedad del exchange ambigua. |
| Alternate exchange en lugar de `mandatory` | ❌ Rescata el mensaje, pero RabbitMQ lo da por ruteado, así que el broker no se entera de la falla y no puede reenviar. |
| `ReturnsCallback` asincrónico para disparar reenvíos | ❌ Obliga a correlacionar por header, a mantener estados intermedios y a usar un pool aparte. Esperar el acuse en el relay da el mismo resultado de forma determinista. |
| Reenviar a otro proveedor ante cualquier error, incluido el timeout | ❌ Un timeout no garantiza que el pedido no haya llegado: puede dejar dos entregas para una donación. |
| **Broker + Adapter + Strategy en Donaciones; un adapter por transporte (AMQP/HTTP); comando direccionado por routing key; `mandatory` con espera de acuse; reenvío solo ante rechazo seguro; outbox basado en datos** ✅ | Selección explícita y testeable, un solo exchange, sin entregas perdidas en silencio ni duplicadas entre proveedores, cada proveedor con su transporte natural, extensible por configuración. |

**Consecuencias negativas (van en el ADR):**
- Una capa más de indirección y un mensaje más en el catálogo.
- Se revierte una decisión del equipo, y Logística deja de enterarse de la asignación «como hecho».
- No hay failover por disponibilidad en AMQP: el reenvío solo cubre «no hay cola» y «nack».
- Un envío incierto no se reenvía. Si el proveedor nunca responde, la solicitud termina `FALLIDA` y necesita revisión manual.
- Cada proveedor **tiene que** deduplicar por `donacionIndependienteId`. Es un requisito de contrato.
- El relay espera acuses de a una entrada (latencia de despacho de unos milisegundos a unos segundos).
- Contrato nuevo y cutover coordinado en dos servicios, con limpieza de la cola vieja.
- Hay que mantener una traducción por cada proveedor HTTP.
- El preferido vive en memoria: vuelve al valor de la propiedad al reiniciar.
- Outbox en memoria: sin atomicidad ni durabilidad hasta pasar a base de datos.
- Protección por API key: mínima y de transición hasta el `auth-service` (Entrega 6).
- Con un proveedor AMQP que no sea nuestra imagen, queda abierta la propiedad del exchange de vuelta.

**Recomendaciones para un entorno real** (sección del ADR; **no** se implementan en esta tarea):

Con credenciales de RabbitMQ compartidas entre servicios, nada impide que un consumidor se bindee con comodín (`entrega.solicitada.#`) y reciba los pedidos de los demás. Eso trae entregas duplicadas, exposición de datos (el comando lleva la dirección de destino y el id de la persona beneficiaria) y la posibilidad de suplantar pedidos. El riesgo ya existe hoy con `donacion.asignada.v1`, pero el broker lo vuelve más sensible porque el mensaje va dirigido. `[INFERRED]` Las medidas son funciones estándar de RabbitMQ, no probadas en este proyecto.

| Medida | Qué cubre |
|---|---|
| Un usuario de RabbitMQ por servicio o proveedor, en lugar de credenciales compartidas | Identidad propia de cada proveedor |
| Permisos configure/write/read por patrón de nombre de cola | Qué colas puede declarar y bindear cada usuario |
| Topic permissions por routing key | Cada proveedor solo puede leer las claves que le corresponden: aunque se bindee con `#`, no recibe lo ajeno |
| Topología provisionada desde definiciones versionadas | Los servicios no declaran bindings arbitrarios |
| TLS y un vhost por entorno | Tráfico cifrado y ambientes separados |
| Idempotencia en el consumidor (`existsByIdDonacion` en ambos caminos) | Defensa en profundidad si igual llegan duplicados |

Para esta entrega, la mitigación del lado de RabbitMQ es solo la convención (bindings exactos) más el test de la Fase 2. La protección de los endpoints HTTP sí se implementa (§2.7).

---

## 5. Puntos abiertos

### Cerrados en esta versión

| Punto | Resolución |
|---|---|
| Si el camino REST de Logística deduplica por `donacionId` | `[OBSERVED]` **No.** `EntregasService.crear` guarda sin chequear. Se agrega el chequeo con 409 en la Fase 4 (D12). |
| Si el proyecto incluye Spring Security | `[OBSERVED]` **No**, en ningún `pom.xml`. `ApiKeyFilter` como `OncePerRequestFilter` común, sin dependencias nuevas. |
| Cómo se correlaciona la devolución con la entrada del outbox | Con `CorrelationData` por entrada y espera del acuse en el relay (D11). Se confirma con Testcontainers en la Fase 2. |

### Siguen abiertos (se resuelven durante la implementación)

| Punto | Cuándo | Cómo |
|---|---|---|
| Qué estrategias adicionales se implementan y con qué criterio (§2.1) | Fase 1 | Sesión conjunta del equipo y el agente. Solo se implementan las acordadas. |
| Qué código de `ErrorCatalog` usa el 409 de Logística | Fase 4 | Revisar el catálogo y el ADR `20260903-estandarizacion-de-codigos-de-estado-http`. |
| Valor de `acuse-timeout-ms` y del read timeout HTTP | Fase 2 / 4 | Medir en la demo local. Los defaults propuestos son 5000 ms y 3000 ms. |

---

## 6. Riesgos

| Riesgo | Mitigación |
|---|---|
| Mensaje AMQP no ruteable perdido en silencio | `mandatory` + espera de acuse + `getReturned()` → reenvío al siguiente proveedor (§2.2) |
| **Doble entrega en dos proveedores tras un timeout** | Incierto ≠ rechazado: un incierto se reintenta solo contra el mismo proveedor (D10) |
| Entrega duplicada en el mismo proveedor al reintentar | Dedup por `donacionIndependienteId` en ambos caminos de Logística (listener existente + `EntregasService.crear` con 409, D12) |
| `mandatory` global devolviendo eventos no ruteables de otros flujos | Template dedicado al comando; el compartido no cambia |
| Carrera de primera vez (cola todavía no declarada) | La devolución se detecta y se reenvía; documentado como límite |
| Cola vieja `logistica.donaciones.asignadas` acumulando mensajes | Eliminar la cola y el binding en el cutover; verificación en la Fase 2 |
| Dos instancias de Logística compitiendo por la misma cola | Nombre de cola parametrizado con `LOGISTICA_INSTANCIA_ID` |
| Cutover parcial (un servicio actualizado y el otro no) | Un solo PR; test de contrato de `EntregaSolicitadaV1` en ambos módulos |
| Binding con comodín que recibe solicitudes ajenas | Bindings exactos; test de que la cola propia no recibe claves ajenas; usuarios y permisos por proveedor en un entorno real (§4) |
| Alias de tipo ambiguo con varias routing keys para una clase | Alias único `entrega.solicitada.v1`; test de serialización en ambos servicios |
| Relay lento por esperar acuses o llamadas HTTP | Timeouts cortos y explícitos; las esperas nunca ocurren en el hilo de la petición; paralelizar el relay si hiciera falta |
| Callback falsificado o de un proveedor que no corresponde | API key por proveedor + verificación de pertenencia (§2.5, §2.7) |
| Claves en el código, los logs o los tests | Variables de entorno, falla cerrado, valores sintéticos en tests, nunca se loguean (AGENTS.md §4.3) |
| Outbox en memoria pierde pendientes al reiniciar y no es atómico con el estado | Declarado como deuda; el puerto permite pasar a `LogisticaOutboxJpa` sin tocar el broker |
| Reutilizar `OutboxStore` (guarda un `Runnable`, no persistible) | El broker usa su propio outbox basado en datos (§2.4) |
| Reenvío que esconde errores de validación | 4xx → `ErrorContrato`, sin reintento ni reenvío |
| `catch (Exception)` amplio (smell de SonarCloud) | Excepciones tipadas; pre-flight con `docs/IA/07-errores-frecuentes-sonarcloud-ia.md` |
| Segunda instancia sin datos para planificar en la demo | Cargar su flota en el compose o con un seeder por instancia (Fase 7) |
| Estrategias inventadas que parecen reglas de negocio reales | Documentarlas como estrategias de prueba en el ADR y en el código |
| Cambio de valor de `donanteId`/`personaId` (tarea separada de identidad compartida) | Este plan no depende de esa tarea; mantener rama y PR separados |

---

## 7. Fuera de alcance

- Persistencia JPA de `donaciones-service` y la implementación transaccional del outbox (`LogisticaOutboxJpa`). El plan deja el puerto y la versión en memoria listos para esa evolución (§2.4).
- Cambios de identidad `Donante.id = personaId`.
- Failover por disponibilidad (health checks) hacia Logística.
- Seguridad de RabbitMQ por proveedor (usuarios, permisos, topic permissions, TLS): queda como recomendación en el ADR (§4).
- Autenticación real (Entrega 6): la API key de §2.7 es una protección mínima de transición.
- Integración con un servicio de logística de terceros real: el proveedor «externo» se simula con una segunda instancia de nuestra propia imagen.
- Cambios en la planificación de rutas de `logistica-service`.
- Activar returns o confirms con reintento para los otros 9 eventos de `donaciones.exchange` (se registra como deuda aparte).
