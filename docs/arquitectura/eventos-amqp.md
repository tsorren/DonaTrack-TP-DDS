# Contratos y Esquema de Eventos AMQP (RabbitMQ) — DonaTrack

> **Catálogo Canónico de Mensajería Asíncrona, Topología de Colas y Schemas de Eventos**  
> **Productores:** `logistica-service`, `donaciones-service`, `incentivos-service`  
> **Consumidores:** `donaciones-service`, `notificaciones-service`  
> **ADR de Referencia:** [`20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md`](../adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md)

---

## 1. Arquitectura de Mensajería y Desacoplamiento Temporal

La interacción entre microservicios se desacopla temporal y espacialmente mediante **RabbitMQ** siguiendo los principios de **Domain-Driven Design (DDD)** y el patrón **Publish/Subscribe**:

1. **Logística hacia Donaciones:** Cuando ocurren transiciones en el ciclo de vida de una ruta o de una entrega, `logistica-service` emite un evento de dominio hacia `logistica.exchange` sin bloquear la atención de peticiones HTTP ni requerir disponibilidad síncrona inmediata de `donaciones-service`.
2. **Pub/Sub Canónico hacia Notificaciones (Hard Cutover):** Los microservicios emisores son soberanos de sus eventos de dominio y publican en sus propios TopicExchanges:
   * `donaciones-service` publica en `donaciones.exchange`.
   * `incentivos-service` publica en `incentivos.exchange`.
   * `notificaciones-service` actúa como consumidor desacoplado, siendo dueño de sus colas dedicadas segregadas por bounded context (`notificaciones.donaciones` y `notificaciones.incentivos`), procesando mensajes mediante listeners `@RabbitHandler` tipados y deduplicando atómicamente con **Transactional Inbox** relacional sobre PostgreSQL.
   * Como arquitectura objetivo de la Etapa 2 de SPEC-03, se erradicará el uso de clientes HTTP síncronos OpenFeign para la emisión de notificaciones inter-servicios, saldando definitivamente la deuda técnica [DTI-13](../adr/DEUDA_TECNICA.md#dti-13-migracion-de-clientes-consumidores-de-la-api-rest-deprecada-de-notificaciones-a-ruta-canonica-y-amqp) (actualmente en estado `[OBSERVED] in-progress`).

---

## 2. Topología de RabbitMQ

```text
donaciones-service                      notificaciones-service (Consumidor)
[donaciones.exchange] ──(donacion.*.v1)► [notificaciones.donaciones] ──► DonacionesEventListener
   (TopicExchange)    ──(donante.*.v1)─►                                   ├── NotificacionService (Inbox)
                      ──(persona.*.v1)─►                                   └── PersonasService (Sync)
                             │
                     (x-dead-letter)
                             ▼
                     [notificaciones.dlx] ──► [notificaciones.donaciones.dlq]

incentivos-service
[incentivos.exchange] ──(incentivo.*.v1) [notificaciones.incentivos] ──► IncentivosEventListener
   (TopicExchange)           │                                            └── NotificacionService (Inbox)
                     (x-dead-letter)
                             ▼
                     [notificaciones.dlx] ──► [notificaciones.incentivos.dlq]

logistica-service                       donaciones-service (Consumidor)
[logistica.exchange]  ──(ruta.#)──────► [donaciones.ruta.asignada / iniciada] ──► LogisticaEventListener
   (TopicExchange)    ──(entrega.#)───► [donaciones.entrega.exitosa / fallida]
```

### 2.1 Exchanges del Sistema

| Exchange | Tipo | Propietario / Emisor | Propósito |
|---|---|---|---|
| **`donaciones.exchange`** | `TopicExchange` (Durable) | `donaciones-service` | Eventos de dominio de donaciones, donantes y sincronización de personas |
| **`incentivos.exchange`** | `TopicExchange` (Durable) | `incentivos-service` | Eventos de dominio de gamificación, misiones, categorías e inactividad |
| **`logistica.exchange`** | `TopicExchange` (Durable) | `logistica-service` | Eventos del ciclo logístico de entregas y rutas de transporte |
| **`notificaciones.dlx`** | `TopicExchange` (Durable) | `notificaciones-service` | Dead Letter Exchange para aislamiento de poison pills y fallos terminales |

---

### 2.2 Colas, Routing Keys y Enlaces (Bindings)

#### A. Eventos hacia `donaciones-service` (desde Logística)

| Routing Key | Nombre de la Cola | Consumidor | Propósito | JSON Schema Canónico |
|---|---|---|---|---|
| `ruta.asignada` | `donaciones.ruta.asignada` | `donaciones-service` (`LogisticaEventListener`) | Notifica que una donación fue incluida en una ruta planificada | [`evento-ruta-asignada.schema.json`](./contratos/schemas/evento-ruta-asignada.schema.json) |
| `ruta.iniciada` | `donaciones.ruta.iniciada` | `donaciones-service` (`LogisticaEventListener`) | Transiciona donaciones a `EN_TRASLADO` y difunde URL de tracking | [`evento-ruta-iniciada.schema.json`](./contratos/schemas/evento-ruta-iniciada.schema.json) |
| `entrega.exitosa` | `donaciones.entrega.exitosa` | `donaciones-service` (`LogisticaEventListener`) | Transiciona donación a `ENTREGADA` | [`evento-entrega-exitosa.schema.json`](./contratos/schemas/evento-entrega-exitosa.schema.json) |
| `entrega.fallida` | `donaciones.entrega.fallida` | `donaciones-service` (`LogisticaEventListener`) | Transiciona donación a `ENTREGA_FALLIDA` y evalúa replanificación | [`evento-entrega-fallida.schema.json`](./contratos/schemas/evento-entrega-fallida.schema.json) |

#### B. Eventos hacia `notificaciones-service` (Catálogo de 10 Eventos Versionados)

| N° | Evento de Dominio | Emisor | TopicExchange | Routing Key | Tipo AMQP (`__TypeId__`) | Cola Receptora | JSON Schema de Contrato |
|:---:|---|---|---|---|---|---|---|
| 1 | Donación Asignada | `donaciones-service` | `donaciones.exchange` | `donacion.asignada.v1` | `donacion.asignada.v1` | `notificaciones.donaciones` | [`evento-donacion-asignada-v1.schema.json`](./contratos/schemas/evento-donacion-asignada-v1.schema.json) |
| 2 | Donación en Camino | `donaciones-service` | `donaciones.exchange` | `donacion.en-camino.v1` | `donacion.en-camino.v1` | `notificaciones.donaciones` | [`evento-donacion-en-camino-v1.schema.json`](./contratos/schemas/evento-donacion-en-camino-v1.schema.json) |
| 3 | Donación Recibida | `donaciones-service` | `donaciones.exchange` | `donacion.recibida.v1` | `donacion.recibida.v1` | `notificaciones.donaciones` | [`evento-donacion-recibida-v1.schema.json`](./contratos/schemas/evento-donacion-recibida-v1.schema.json) |
| 4 | Entrega Fallida | `donaciones-service` | `donaciones.exchange` | `donacion.entrega-fallida.v1` | `donacion.entrega-fallida.v1` | `notificaciones.donaciones` | [`evento-donacion-entrega-fallida-v1.schema.json`](./contratos/schemas/evento-donacion-entrega-fallida-v1.schema.json) |
| 5 | Donación Vencida | `donaciones-service` | `donaciones.exchange` | `donacion.vencida.v1` | `donacion.vencida.v1` | `notificaciones.donaciones` | [`evento-donacion-vencida-v1.schema.json`](./contratos/schemas/evento-donacion-vencida-v1.schema.json) |
| 6 | Donante Registrado | `donaciones-service` | `donaciones.exchange` | `donante.registrado.v1` | `donante.registrado.v1` | `notificaciones.donaciones` | [`evento-donante-registrado-v1.schema.json`](./contratos/schemas/evento-donante-registrado-v1.schema.json) |
| 7 | Persona Sincronizada | `donaciones-service` | `donaciones.exchange` | `persona.sincronizada.v1` | `persona.sincronizada.v1` | `notificaciones.donaciones` | [`evento-persona-sincronizada-v1.schema.json`](./contratos/schemas/evento-persona-sincronizada-v1.schema.json) |
| 8 | Misión Cumplida | `incentivos-service` | `incentivos.exchange` | `incentivo.mision-cumplida.v1` | `incentivo.mision-cumplida.v1` | `notificaciones.incentivos` | [`evento-incentivo-mision-cumplida-v1.schema.json`](./contratos/schemas/evento-incentivo-mision-cumplida-v1.schema.json) |
| 9 | Subió de Categoría | `incentivos-service` | `incentivos.exchange` | `incentivo.subio-categoria.v1` | `incentivo.subio-categoria.v1` | `notificaciones.incentivos` | [`evento-incentivo-subio-categoria-v1.schema.json`](./contratos/schemas/evento-incentivo-subio-categoria-v1.schema.json) |
| 10 | Donante Inactivo | `incentivos-service` | `incentivos.exchange` | `incentivo.donante-inactivo.v1` | `incentivo.donante-inactivo.v1` | `notificaciones.incentivos` | [`evento-incentivo-donante-inactivo-v1.schema.json`](./contratos/schemas/evento-incentivo-donante-inactivo-v1.schema.json) |

#### C. Clúster de Dead Letter Queues (Aislamiento de Fallas)

| Cola Principal | Argumento DLX | Exchange DLX | Routing Key | Cola Terminal (DLQ) |
|---|---|---|---|---|
| `notificaciones.donaciones` | `x-dead-letter-exchange` | `notificaciones.dlx` | `notificaciones.donaciones` | `notificaciones.donaciones.dlq` |
| `notificaciones.incentivos` | `x-dead-letter-exchange` | `notificaciones.dlx` | `notificaciones.incentivos` | `notificaciones.incentivos.dlq` |

---

## 3. Envelope Nativo de Protocolo y Payloads JSON

La serialización de mensajes se realiza mediante `Jackson2JsonMessageConverter`, garantizando compatibilidad tipada con records de Java 21 sin contaminación de datos de transporte en el cuerpo de dominio.

### 3.1 Envelope AMQP (Headers Estándar)

Cada mensaje publicado en RabbitMQ incluye en sus `MessageProperties`:
* **`message_id`:** Identificador UUID unívoco generado por el emisor. Actúa como clave primaria de deduplicación en el Inbox.
* **`timestamp`:** Marca temporal de emisión del mensaje.
* **`content_type`:** `application/json`.
* **`X-Trace-Id`:** Identificador de trazabilidad distribuida propagado para correlación de logs.
* **`__TypeId__`:** Alias lógico versionado canónico (ej. `donacion.asignada.v1`, `incentivo.mision-cumplida.v1`). Permite a `DefaultClassMapper` en `notificaciones-service` mapear el JSON al record Java específico sin acoplar los nombres de paquetes internos entre microservicios.

### 3.2 Ejemplos de Payloads de Dominio Limpios

#### Donación Asignada (`donacion.asignada.v1`)
```json
{
  "personaDonanteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "fecha": "2026-09-11T14:30:00Z",
  "personaBeneficiariaId": "b1ffcd88-8d1c-4fe9-aa7e-7cc8ae491b22",
  "descripcion": "Caja de alimentos no perecederos (10 kg)"
}
```

#### Donante Registrado (`donante.registrado.v1`)
```json
{
  "personaDonanteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "fecha": "2026-09-11T10:00:00Z",
  "credencialesDeAcceso": "clave-inicial-temporal-123"
}
```

#### Persona Sincronizada (`persona.sincronizada.v1`)
```json
{
  "personaId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "denominacion": "Juan Pérez",
  "tipoPersona": "HUMANA",
  "mediosDeContacto": [
    {
      "tipo": "CORREO",
      "esPredeterminado": true,
      "direccionCorreo": "juan.perez@example.com"
    },
    {
      "tipo": "WHATSAPP",
      "esPredeterminado": false,
      "codigoArea": "11",
      "numero": "44445555"
    }
  ]
}
```

#### Misión Cumplida (`incentivo.mision-cumplida.v1`)
```json
{
  "personaDonanteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "fecha": "2026-09-11T16:00:00Z",
  "nombreMision": "Primera Donación del Mes",
  "puntosObtenidos": 150
}
```

---

## 4. Idempotencia, Trazabilidad y Clúster Dead Letter

1. **Idempotencia en los Receptores:**
   * **`donaciones-service`:** Toda transición en su máquina de estados verifica si el estado destino es compatible o redundante (State Pattern).
   * **`notificaciones-service` (Transactional Inbox):**
     * Cada mensaje recibido es interceptado en `NotificacionService`.
     * Se extrae el `UUID` desde el header AMQP `message_id`.
     * Se ejecuta la sentencia relacional atómica sobre PostgreSQL:
       ```sql
       INSERT INTO evento_procesado (event_id, fecha_procesamiento)
       VALUES (?, ?) ON CONFLICT DO NOTHING;
       ```
     * Si la fila no se inserta (retorna 0 filas afectadas), significa que el evento ya fue procesado con anterioridad; el servicio descarta el mensaje de forma silenciosa e idempotente emitiendo un `ACK` al broker.
2. **Trazabilidad Distribuida:**
   * El emisor extrae el `traceId` activo del contexto de logging (MDC) y lo inyecta como header `X-Trace-Id` en las propiedades del mensaje RabbitMQ.
   * El consumidor lee `X-Trace-Id` y lo restablece en su propio MDC antes de procesar el evento, garantizando correlación de logs de extremo a extremo entre productores y consumidores.
3. **Manejo de Poison Pills y Clúster Dead Letter:**
   * Las colas `notificaciones.donaciones` y `notificaciones.incentivos` están configuradas con `x-dead-letter-exchange: notificaciones.dlx`.
   * Si un mensaje no puede deserializarse o agota los reintentos transitorios por errores irreparables, RabbitMQ lo enruta hacia `notificaciones.dlx` y de allí a su cola muerta correspondiente (`notificaciones.donaciones.dlq` o `notificaciones.incentivos.dlq`).
   * Esto previene poison pills, redelivery loops infinitos y garantiza que una falla en un bounded context no degrade el procesamiento de los demás.
