# Broker de Integración con Logística

- Status: proposed
- Date: 2026-10-07
- Deciders: Decisión Grupal
- Tags: arquitectura, integracion, broker, strategy, adapter, amqp, rabbitmq, http, outbox, idempotencia, entrega-4

## Contexto y Problema

La Entrega 4 (`docs/entregas/4/Enunciado-4.pdf`, pág. 24) exige que la integración entre `donaciones-service` y `logistica-service` se haga a través de un **broker** que permita *seleccionar entre más de un servicio de logística disponible*: el propio y otro servicio potencial que cumpla el mismo objetivo.

En el estado actual (`[OBSERVED]` rama `ENTREGA_4`, después de la migración a AMQP #883), `donaciones-service` publica el **hecho** `donacion.asignada.v1` en `donaciones.exchange`, y lo consumen Notificaciones y Logística. Logística crea la entrega a partir de ese hecho (`DonacionAsignadaEventListener`). Con esta topología no se puede elegir proveedor:

1. Un TopicExchange **copia** el mensaje a cada cola cuyo binding coincida; no elige. Si un segundo proveedor se suscribiera al mismo hecho, ambos crearían la entrega.
2. Aunque el broker eligiera otro proveedor, nuestra Logística seguiría creando la entrega, porque escucha el hecho público.
3. `[OBSERVED]` Ningún servicio configura `mandatory` ni publisher confirms/returns. Un mensaje dirigido a una routing key sin cola se descarta en silencio.

Además, en la reunión del 15/9 (`docs/entrega-4/donaciones/bitacora-comunicaciones.md`) se había descartado un comando `EntregaSolicitadaV1` y Logística pasó a suscribirse directo a `donacion.asignada.v1`. En ese momento había un único proveedor y no se había considerado el requisito del broker.

## Atributos de Calidad y Drivers de Decisión

* **Cumplimiento de la consigna:** selección explícita entre ≥ 2 proveedores de logística.
* **Extensibilidad (Open/Closed):** sumar un proveedor debe ser configuración o una clase nueva, sin tocar el servicio de aplicación.
* **Higiene de contratos:** el hecho de dominio `donacion.asignada.v1` no debe contaminarse con datos de ruteo.
* **Disponibilidad y desacoplamiento temporal:** la comunicación con nuestra propia Logística sigue siendo asincrónica (ADR [20260911](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md)).
* **Integridad:** ninguna entrega se pierde en silencio y ninguna donación termina con entregas en dos proveedores.

## Relaciones Arquitectónicas

1. **Evoluciona:** [20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones](./20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md). Mantiene un TopicExchange por productor y colas declaradas por el consumidor, y agrega la distinción entre eventos y comandos.
2. **Revisa:** la decisión del 15/9 registrada en [`bitacora-comunicaciones.md`](../entrega-4/donaciones/bitacora-comunicaciones.md) («Logística ya no recibe un Command `EntregaSolicitadaV1`»). Se reintroduce el comando con la justificación de este ADR. Revisión acordada por el grupo (ver [`bitacora-broker-logistica.md`](../entrega-4/donaciones/bitacora-broker-logistica.md)).
3. **Aplica:** [20260919-convencion-canonica-identificadores-y-contratos-amqp](./20260919-convencion-canonica-identificadores-y-contratos-amqp.md) (`<entidad>Id`, records sin `@JsonAlias`), [20260901-consistencia-temporal-y-normalizacion-semantica-de-eventos](./20260901-consistencia-temporal-y-normalizacion-semantica-de-eventos.md) (event-time), [20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos](./20260901-patron-de-idempotencia-y-deduplicacion-en-consumo-de-eventos-distribuidos.md) y [20260901-patron-transactional-outbox-para-consistencia-eventual](./20260901-patron-transactional-outbox-para-consistencia-eventual.md) (en una versión interina en memoria).
4. **Precedente de patrón:** [donaciones-service/20260615-strategy-gestor-algoritmos](./donaciones-service/20260615-strategy-gestor-algoritmos.md) (Strategy).
5. **Refinado por:** [20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad](./20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md), que define el camino de vuelta de los proveedores (decisión 7).

## Alternativas Consideradas

| Alternativa | Veredicto |
|---|---|
| `if/switch` por proveedor en `PropuestaDeAsignacionService` | ❌ Mezcla orquestación de negocio con integración. Cada proveedor nuevo obliga a modificar el servicio. |
| Balanceo de infraestructura (Spring Cloud LoadBalancer, API Gateway) | ❌ Sirve para réplicas del mismo servicio con el mismo contrato. Elegir proveedor es una decisión de negocio. |
| Consumidores competitivos en una sola cola | ❌ No hay control sobre quién toma cada entrega. |
| Mantener `donacion.asignada.v1` como único canal | ❌ No hay selección y se duplican entregas. |
| Campo `proveedorLogistica` en `donacion.asignada.v1` + filtro en cada consumidor (Message Filter) | ❌ Mete datos de ruteo en un hecho que escucha Notificaciones y obliga a cada proveedor a filtrar. |
| Microservicio broker dedicado | ❌ Suma un contenedor, un punto único de falla y un salto de red, sin beneficio funcional. |
| Un exchange por proveedor | ❌ Mismo aislamiento que la routing key, con más infraestructura y la propiedad del exchange ambigua. |
| Alternate exchange en lugar de `mandatory` | ❌ Rescata el mensaje, pero RabbitMQ lo considera ruteado: el broker no se entera de la falla y no puede reenviar. |
| `ReturnsCallback` asincrónico para disparar reenvíos | ❌ Exige correlación por header, estados intermedios y un pool dedicado. Esperar el acuse en el relay logra lo mismo de forma determinista. |
| Reenviar a otro proveedor ante cualquier error, incluido el timeout | ❌ Un timeout no garantiza que el pedido no haya llegado: podría dejar dos entregas para una misma donación. |
| Callback HTTP: el proveedor avisa a Donaciones por un endpoint propio (protegido con API key) | ❌ Se implementó en una primera versión y se quitó: contradice la letra del requerimiento 3 del enunciado si el proveedor se considera un servicio de logística. |
| Polling: Donaciones consulta periódicamente el estado al proveedor | ⏳ No se implementó. Es la evolución natural para un proveedor que no pueda publicar en nuestro RabbitMQ: cumple el enunciado al pie de la letra, pero exige un método de consulta por adapter, un traductor de los estados de cada proveedor a nuestros eventos y un planificador. |
| **Broker in-process (Broker + Adapter + Strategy) en Donaciones, un adapter por transporte, comando direccionado por routing key, `mandatory` con espera de acuse, reenvío solo ante rechazo seguro y outbox basado en datos** | ✅ Elegida. |

## Resultado de la Decisión

Se adopta un **broker in-process en `donaciones-service`**:

1. **Piezas:** `ILogisticaBroker` (puerto de aplicación), `IProveedorLogistica` (puerto que implementa cada adapter), `IEstrategiaSeleccionProveedor` (Strategy), `SolicitudEntregaLogistica` (Canonical Data Model) y un outbox propio basado en datos (`ILogisticaOutbox` + relay `@Scheduled`).
2. **Un adapter por transporte:**
   * `ProveedorLogisticaAmqp`: publica el **comando** `EntregaSolicitadaV1` en `donaciones.exchange` con routing key `entrega.solicitada.<proveedorId>.v1`.
   * `ProveedorLogisticaHttp`: traduce el modelo canónico al contrato REST del proveedor (Message Translator).
3. **Regla de ruteo (nueva convención):** **los eventos se rutean por hecho; los comandos, por destinatario.** Un evento (`donacion.asignada.v1`) puede tener N interesados, y su routing key nunca indica quién escucha. Un comando (`entrega.solicitada.<proveedorId>.v1`) es una orden a un destinatario elegido, y su routing key lo identifica. Los consumidores de comandos se bindean con la clave **exacta**, nunca con comodines.
4. **Logística deja de consumir `donacion.asignada.v1`** y consume `entrega.solicitada.<LOGISTICA_INSTANCIA_ID>.v1`, con una cola propia por instancia. `donacion.asignada.v1` queda con un solo consumidor: Notificaciones.
5. **Confiabilidad de la publicación:** template dedicado al comando con `mandatory=true` y publisher confirms (`correlated`). El relay espera el acuse (`CorrelationData`) y clasifica el resultado:

   | Resultado | Señal | Acción del broker |
   |---|---|---|
   | Publicado | Ack sin devolución · HTTP 2xx o 409 | Solicitud `ENVIADA` |
   | Rechazado (seguro que no llegó) | Mensaje devuelto · sin conexión con RabbitMQ · conexión HTTP rechazada · 503 | Siguiente proveedor según la estrategia. Si todos rechazaron, nueva ronda completa más tarde (backoff), hasta agotar las rondas |
   | Incierto (pudo haber llegado) | Nack · sin acuse a tiempo · timeout de lectura · HTTP 500/502/504 | Reintento con backoff **al mismo** proveedor |
   | Error de contrato | HTTP 4xx (≠ 409) | Sin reintento ni reenvío; `FALLIDO` + log `error` |

6. **Idempotencia del proveedor:** todo proveedor deduplica por `donacionIndependienteId`. Es un requisito de contrato. La obligación recae en el proveedor, no en Donaciones. Nuestra Logística interna se alcanza solo por AMQP, donde el listener ya deduplica. No se modifica `POST /api/entregas`: el stand-in HTTP `externo` de la demo (D9) no deduplica por ese camino, y ese riesgo se asume.
7. **Camino de vuelta, solo por mensajería y con identidad del proveedor:** todo proveedor informa publicando en `logistica.exchange` los eventos existentes, con su id y su token en headers; HTTP se usa solo de ida. Detalle, alternativas y consecuencias en [20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad](./20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md).
8. **Selección:** estrategia base «preferencia configurable + reenvío». Otras estrategias se implementan como **estrategias de prueba**, acordadas por el equipo, y no representan reglas de negocio reales.

## Consecuencias Positivas

* Se cumple la consigna con una selección explícita, testeable con JUnit y sin infraestructura adicional.
* `donacion.asignada.v1` sigue siendo un hecho limpio, y Notificaciones no se ve afectado.
* Ninguna entrega se pierde en silencio: la ausencia de cola se detecta y se reenvía.
* Una caída pasajera de los proveedores (por ejemplo, RabbitMQ caído) no deja asignaciones sin entrega: si todos rechazan, la ronda se reintenta más tarde.
* Ninguna donación queda con entregas en dos proveedores: solo se reenvía ante rechazo seguro.
* Cada proveedor usa su transporte natural. Un proveedor AMQP nuevo se suma por configuración, y uno HTTP con un adapter.
* El outbox basado en datos permite pasar a JPA sin tocar el broker.

## Consecuencias Negativas

* Una capa más de indirección y un mensaje más en el catálogo.
* Se revierte una decisión previa del equipo, y Logística deja de enterarse de la asignación «como hecho».
* No hay failover por disponibilidad en AMQP: el reenvío solo cubre «no hay cola» y «sin conexión». Un nack se trata como incierto: si el canal se corta con acuses pendientes, spring-rabbit genera él mismo un nack y el mensaje pudo haber quedado en la cola. Si la cola existe, el mensaje espera a que el consumidor vuelva.
* Un envío incierto no se reenvía. Si el proveedor nunca responde, la solicitud termina `FALLIDA` y requiere revisión manual.
* El relay espera acuses de a una entrada (latencia de despacho de milisegundos a segundos).
* Requiere un cutover coordinado en dos servicios. La cola vieja `logistica.donaciones.asignadas` queda huérfana en un RabbitMQ que haya corrido la versión anterior: hay que recrearlo o borrarla a mano (los RabbitMQ del proyecto no tienen volúmenes, así que recrear el contenedor alcanza).
* Hay que mantener una traducción por cada proveedor HTTP.
* En la demo, el proveedor HTTP `externo` es una segunda instancia de nuestra Logística, cuyo `POST /api/entregas` no deduplica: ante un timeout simulado el reintento puede dejar una entrega duplicada en esa instancia. Es la razón por la que la idempotencia es un requisito de contrato para el proveedor.
* `[INFERRED]` El contrato REST del único proveedor HTTP actual (`externo`) es el de nuestra propia Logística (`CrearEntregaRequestDTO`), porque en la demo es una segunda instancia de la misma imagen (D9). No lo definió un proveedor real. Con un proveedor real, el adapter se escribe contra su especificación (path, nombres y tipos de campos, unidades, autenticación, códigos de respuesta, idempotencia) y debe validarse con tests de contrato contra su OpenAPI. El broker, la estrategia y el outbox no cambian: lo que se reemplaza es el adapter.
* El outbox es en memoria: sin atomicidad ni durabilidad hasta la migración a PostgreSQL (deuda declarada).
* La protección por API key es mínima y de transición hasta el `auth-service` (Entrega 6).
* Para un proveedor que no sea nuestra imagen, queda abierta la propiedad del exchange de vuelta. Además se le exige publicar en nuestro RabbitMQ (credenciales, red y permisos), lo que un tercero que solo hable HTTP no podría cumplir sin la evolución por polling.
* El proveedor que publica un evento de vuelta se identifica con su id y un token propio en headers (`X-Proveedor-Id`, `X-Proveedor-Token`), que Donaciones compara con el configurado y contrasta con el registro de solicitudes (D51). Es una protección mínima: el token viaja en un header, sin rotación ni firma del cuerpo, y lo puede ver quien consuma de `logistica.exchange`. En un entorno real se resuelve con usuarios y topic permissions por proveedor en RabbitMQ (`DEUDA_TECNICA.md`, DTI-14, ítem 7; sin implementar).
* Con credenciales de RabbitMQ compartidas, un binding con comodín podría recibir comandos ajenos. Se mitiga por convención y con un test; en un entorno real se recomiendan usuarios y topic permissions por proveedor (sin implementar).

## Validación

1. Tests unitarios del broker con proveedores fake: rechazado → siguiente; incierto → mismo proveedor; error de contrato → sin reenvío; todos rechazan → nueva ronda; rondas agotadas → `FALLIDA`.
2. Test de integración con RabbitMQ real (Testcontainers): routing key sin binding → mensaje devuelto → `EnvioRechazado`; la cola de una instancia no recibe claves ajenas.
3. Tests del adapter HTTP con `MockRestServiceServer`: mapeo de 2xx, 409, conexión rechazada, timeouts, 5xx y 4xx.
4. Tests del procesador de vuelta (`ProcesadorEventosLogisticaTest`) y del listener AMQP: el mismo evento, por el camino que sea, se aplica una sola vez, y un evento duplicado se ignora.
5. `node scripts/validate-contracts.js` valida `evento-entrega-solicitada-v1.schema.json`.
6. Implementación de referencia: [`docs/specs/active/SPEC-04-broker-integracion-logistica.md`](../specs/active/SPEC-04-broker-integracion-logistica.md) y [`plan-broker-logistica.md`](../entrega-4/donaciones/plan-broker-logistica.md).
