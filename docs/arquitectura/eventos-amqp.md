# Contratos y Esquema de Eventos AMQP (RabbitMQ) — DonaTrack

> **Catálogo Canónico de Mensajería Asíncrona, Topología de Colas y Schemas de Eventos**  
> **Productores:** `logistica-service`, `donaciones-service`, `incentivos-service`  
> **Consumidores:** `donaciones-service`, `notificaciones-service`, `incentivos-service`, `logistica-service`  
> **ADRs de Referencia:** [`20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md`](../adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) · [`20261007-broker-de-integracion-con-logistica.md`](../adr/20261007-broker-de-integracion-con-logistica.md)

---

## 1. Arquitectura de Mensajería y Desacoplamiento Temporal

La interacción entre microservicios se desacopla temporal y espacialmente mediante **RabbitMQ** siguiendo los principios de **Domain-Driven Design (DDD)** y el patrón **Publish/Subscribe**:

1. **Logística hacia Donaciones:** Cuando ocurren transiciones en el ciclo de vida de una ruta o de una entrega, `logistica-service` emite un evento de dominio hacia `logistica.exchange` sin bloquear la atención de peticiones HTTP ni requerir disponibilidad síncrona inmediata de `donaciones-service`.
2. **Pub/Sub Canónico hacia Notificaciones (Hard Cutover):** Los microservicios emisores son soberanos de sus eventos de dominio y publican en sus propios TopicExchanges:
   * `donaciones-service` publica en `donaciones.exchange`.
   * `incentivos-service` publica en `incentivos.exchange`.
   * `notificaciones-service` actúa como consumidor desacoplado, siendo dueño de sus colas dedicadas segregadas por bounded context (`notificaciones.donaciones` y `notificaciones.incentivos`), procesando mensajes mediante listeners `@RabbitHandler` tipados y deduplicando atómicamente con **Transactional Inbox** relacional sobre PostgreSQL.
   * Como arquitectura objetivo de la Etapa 2 de SPEC-03, se erradicará el uso de clientes HTTP síncronos OpenFeign para la emisión de notificaciones inter-servicios, saldando definitivamente la deuda técnica [DTI-13](../adr/DEUDA_TECNICA.md#dti-13-migracion-de-clientes-consumidores-de-la-api-rest-deprecada-de-notificaciones-a-ruta-canonica-y-amqp) (actualmente en estado `[OBSERVED] in-progress`).

3. **Donaciones hacia Logística (broker de integración):** el broker de `donaciones-service` elige, para cada donación asignada, a qué proveedor de logística pedirle la entrega. A un proveedor AMQP le manda el **comando** `entrega.solicitada.<proveedorId>.v1` en `donaciones.exchange` (ver §2.2 B2); a un proveedor HTTP, un `POST` por REST. Logística ya no consume el hecho `donacion.asignada.v1`.

---

## 2. Topología de RabbitMQ

```text
donaciones-service
[donaciones.exchange] ──(donacion.asignada.v1)─► [notificaciones.donaciones]      ──► Notificaciones (Inbox)
   (TopicExchange)    ──(entrega.solicitada.donatrack.v1)► [logistica.donatrack.entregas.solicitadas] ──► Logística (comando del broker)
                      ──(donacion.segmentada.v1)► [incentivos.donacion-segmentada]   ──► Incentivos
                      ──(donacion.recibida.v1)──► [incentivos.donacion-recibida]     ──► Incentivos
                      ──(persona.sincronizada.v1)► [incentivos.persona-sincronizada] ──► Incentivos
                      ──(donante.registrado.v1)─► [incentivos.donante-registrado]    ──► Incentivos
                      ──(donante.dado-de-baja.v1)► [incentivos.donante-dado-de-baja] ──► Incentivos
                      ──(donacion.*.v1 / etc.)──► [notificaciones.donaciones]
                             │
                     (x-dead-letter)
                             ▼
                     [notificaciones.dlx] ────► [notificaciones.donaciones.dlq]

incentivos-service
[incentivos.exchange] ──(incentivo.*.v1)───────► [notificaciones.incentivos]      ──► Notificaciones (Inbox)
   (TopicExchange)           │
                     (x-dead-letter)
                             ▼
                     [notificaciones.dlx] ────► [notificaciones.incentivos.dlq]

logistica-service
[logistica.exchange]  ──(ruta.#)───────────────► [donaciones.ruta.asignada / iniciada] ──► LogisticaEventListener
   (TopicExchange)    ──(entrega.#)────────────► [donaciones.entrega.exitosa / fallida]
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

#### B. Eventos hacia `notificaciones-service`, `logistica-service` e `incentivos-service`

| N° | Evento de Dominio | Emisor | TopicExchange | Routing Key | Tipo AMQP (`__TypeId__`) | Colas Receptoras | JSON Schema de Contrato |
|:---:|---|---|---|---|---|---|---|
| 1 | Donación Asignada | `donaciones-service` | `donaciones.exchange` | `donacion.asignada.v1` | `donacion.asignada.v1` | `notificaciones.donaciones` | [`evento-donacion-asignada-v1.schema.json`](./contratos/schemas/evento-donacion-asignada-v1.schema.json) |
| 1b | Donación Segmentada | `donaciones-service` | `donaciones.exchange` | `donacion.segmentada.v1` | `donacion.segmentada.v1` | `incentivos.donacion-segmentada` | [`evento-donacion-segmentada-v1.schema.json`](./contratos/schemas/evento-donacion-segmentada-v1.schema.json) |
| 2 | Donación en Camino | `donaciones-service` | `donaciones.exchange` | `donacion.en-camino.v1` | `donacion.en-camino.v1` | `notificaciones.donaciones` | [`evento-donacion-en-camino-v1.schema.json`](./contratos/schemas/evento-donacion-en-camino-v1.schema.json) |
| 3 | Donación Recibida | `donaciones-service` | `donaciones.exchange` | `donacion.recibida.v1` | `donacion.recibida.v1` | `notificaciones.donaciones`, `incentivos.donacion-recibida` | [`evento-donacion-recibida-v1.schema.json`](./contratos/schemas/evento-donacion-recibida-v1.schema.json) |
| 4 | Entrega Fallida | `donaciones-service` | `donaciones.exchange` | `donacion.entrega-fallida.v1` | `donacion.entrega-fallida.v1` | `notificaciones.donaciones` | [`evento-donacion-entrega-fallida-v1.schema.json`](./contratos/schemas/evento-donacion-entrega-fallida-v1.schema.json) |
| 5 | Donación Vencida | `donaciones-service` | `donaciones.exchange` | `donacion.vencida.v1` | `donacion.vencida.v1` | `notificaciones.donaciones` | [`evento-donacion-vencida-v1.schema.json`](./contratos/schemas/evento-donacion-vencida-v1.schema.json) |
| 6 | Donante Registrado | `donaciones-service` | `donaciones.exchange` | `donante.registrado.v1` | `donante.registrado.v1` | `notificaciones.donaciones`, `incentivos.donante-registrado` | [`evento-donante-registrado-v1.schema.json`](./contratos/schemas/evento-donante-registrado-v1.schema.json) |
| 7 | Persona Sincronizada | `donaciones-service` | `donaciones.exchange` | `persona.sincronizada.v1` | `persona.sincronizada.v1` | `notificaciones.donaciones`, `incentivos.persona-sincronizada` | [`evento-persona-sincronizada-v1.schema.json`](./contratos/schemas/evento-persona-sincronizada-v1.schema.json) |
| 7b | Donante Dado de Baja | `donaciones-service` | `donaciones.exchange` | `donante.dado-de-baja.v1` | `donante.dado-de-baja.v1` | `incentivos.donante-dado-de-baja` | [`evento-donante-dado-de-baja-v1.schema.json`](./contratos/schemas/evento-donante-dado-de-baja-v1.schema.json) |
| 8 | Misión Cumplida | `incentivos-service` | `incentivos.exchange` | `incentivo.mision-cumplida.v1` | `incentivo.mision-cumplida.v1` | `notificaciones.incentivos` | [`evento-incentivo-mision-cumplida-v1.schema.json`](./contratos/schemas/evento-incentivo-mision-cumplida-v1.schema.json) |
| 9 | Subió de Categoría | `incentivos-service` | `incentivos.exchange` | `incentivo.subio-categoria.v1` | `incentivo.subio-categoria.v1` | `notificaciones.incentivos` | [`evento-incentivo-subio-categoria-v1.schema.json`](./contratos/schemas/evento-incentivo-subio-categoria-v1.schema.json) |
| 10 | Donante Inactivo | `incentivos-service` | `incentivos.exchange` | `incentivo.donante-inactivo.v1` | `incentivo.donante-inactivo.v1` | `notificaciones.incentivos` | [`evento-incentivo-donante-inactivo-v1.schema.json`](./contratos/schemas/evento-incentivo-donante-inactivo-v1.schema.json) |

#### B2. Comando del broker de logística hacia `logistica-service`

Único **comando** del sistema: va a un destinatario elegido por el broker de logística de `donaciones-service`, así que su routing key identifica al destinatario (regla «eventos por hecho, comandos por destinatario», [ADR 20261007](../adr/20261007-broker-de-integracion-con-logistica.md)).

| Mensaje | Emisor | TopicExchange | Routing Key | Tipo AMQP (`__TypeId__`) | Cola Receptora | JSON Schema de Contrato |
|---|---|---|---|---|---|---|
| Entrega Solicitada | `donaciones-service` (broker) | `donaciones.exchange` | `entrega.solicitada.<proveedorId>.v1` | `entrega.solicitada.v1` (fijo) | `logistica.<instancia>.entregas.solicitadas` (binding exacto por `LOGISTICA_INSTANCIA_ID`) | [`evento-entrega-solicitada-v1.schema.json`](./contratos/schemas/evento-entrega-solicitada-v1.schema.json) |

Se publica con `mandatory=true` y publisher confirms (`correlated`); el broker espera el acuse. Si RabbitMQ lo devuelve (no hay cola para esa routing key), es un rechazo seguro y el broker prueba con el siguiente proveedor. Un nack o la falta de acuse son inciertos: el mensaje pudo haber quedado en la cola, así que se reintenta con el mismo proveedor. Un proveedor HTTP no usa este comando: recibe el pedido por REST, y como todo proveedor informa su avance publicando en `logistica.exchange` los eventos de vuelta (no invoca a Donaciones por HTTP).

#### B3. Eventos de vuelta de los proveedores de logística

Todo proveedor de logística informa el avance de una entrega publicando en `logistica.exchange` los eventos `ruta.asignada`, `ruta.iniciada`, `entrega.exitosa` y `entrega.fallida` (los mismos que ya publica nuestra Logística). No invoca a Donaciones por HTTP (Enunciado 4, requerimiento de implementación 3). Cada mensaje debe llevar dos headers:

| Header | Valor |
|---|---|
| `X-Proveedor-Id` | Id del proveedor (el de `donatrack.logistica.proveedores`) |
| `X-Proveedor-Token` | Token propio del proveedor, acordado con Donaciones (`donatrack.logistica.proveedor.<id>.token-vuelta`) |
| `__TypeId__` | El alias del evento, igual a su routing key (`ruta.asignada`, `ruta.iniciada`, `entrega.exitosa`, `entrega.fallida`), **no** el nombre de la clase del emisor: Donaciones solo conoce esos alias y descarta con un error de conversión cualquier otro valor |

`donaciones-service` descarta, con un aviso en el log, los eventos sin esos headers, con un token incorrecto o sobre una donación que no está asignada a ese proveedor en el registro del broker. Nuestra Logística firma así sus eventos con `logistica.instancia-id` y `logistica.token-vuelta`. Es una protección mínima (DTI-14, ítem 7). Decisión y alternativas: ADR [20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad](../adr/20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md).

#### C. Clúster de Dead Letter Queues (Aislamiento de Fallas)

| Cola Principal | Argumento DLX | Exchange DLX | Routing Key | Cola Terminal (DLQ) |
|---|---|---|---|---|
| `notificaciones.donaciones` | `x-dead-letter-exchange` | `notificaciones.dlx` | `notificaciones.donaciones.dlq` | `notificaciones.donaciones.dlq` |
| `notificaciones.incentivos` | `x-dead-letter-exchange` | `notificaciones.dlx` | `notificaciones.incentivos.dlq` | `notificaciones.incentivos.dlq` |

---

## 3. Envelope Nativo de Protocolo y Payloads JSON

La serialización de mensajes se realiza mediante `JacksonJsonMessageConverter`, garantizando compatibilidad tipada con records de Java 21 sin contaminación de datos de transporte en el cuerpo de dominio.

### 3.1 Envelope AMQP (Headers Estándar)

Cada mensaje publicado en RabbitMQ incluye en sus `MessageProperties`:
* **`message_id`:** Identificador UUID unívoco generado por el emisor. Actúa como clave primaria de deduplicación en el Inbox.
* **`timestamp`:** Marca temporal de emisión del mensaje.
* **`content_type`:** `application/json`.
* **`X-Trace-Id`:** Identificador de trazabilidad distribuida propagado para correlación de logs.
* **`__TypeId__`:** Alias lógico versionado canónico (ej. `donacion.asignada.v1`, `donacion.segmentada.v1`, `incentivo.mision-cumplida.v1`). Permite a `DefaultClassMapper` en los servicios receptores mapear el JSON al record Java específico sin acoplar los nombres de paquetes internos entre microservicios.

### 3.2 Ejemplos de Payloads de Dominio Limpios

#### Donación Asignada (`donacion.asignada.v1`)
```json
{
  "donacionIndependienteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "donanteId": "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
  "personaId": "d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a44",
  "fecha": "2026-09-15T14:30:00Z",
  "personaBeneficiariaId": "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33",
  "descripcion": "Caja de alimentos no perecederos (10 kg)",
  "destino": {
    "calle": "Av. Medrano",
    "altura": 951,
    "codigoPostal": "C1179AAQ",
    "localidad": "CABA",
    "provincia": "Buenos Aires",
    "pais": "Argentina"
  },
  "pesoTotalKG": 10.5,
  "volumenTotalM3": 0.25
}
```

#### Donación Segmentada (`donacion.segmentada.v1`)
```json
{
  "donanteId": "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22",
  "fecha": "2026-09-15T14:30:00Z",
  "items": [{ "categoria": "ALIMENTOS", "cantidad": 10 }]
}
```

#### Donante Registrado (`donante.registrado.v1`)
```json
{
  "donanteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "personaId": "e0eebc99-9c0b-4ef8-bb6d-6bb9bd380a55",
  "nombre": "Juan Pérez",
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
  "recompensa": "Insignia de Bronce"
}
```

#### Subió de Categoría (`incentivo.subio-categoria.v1`)
```json
{
  "personaDonanteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "fecha": "2026-09-11T16:00:00Z",
  "nombreNuevaCategoria": "Plata",
  "nombreViejaCategoria": "Bronce"
}
```

#### Entrega Solicitada (`entrega.solicitada.<proveedorId>.v1`)
```json
{
  "donacionIndependienteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "personaBeneficiariaId": "c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a33",
  "destino": {
    "calle": "Av. Medrano",
    "altura": 951,
    "codigoPostal": "C1179AAQ",
    "localidad": "CABA",
    "provincia": "Buenos Aires",
    "pais": "Argentina"
  },
  "pesoTotalKG": 10.5,
  "volumenTotalM3": 0.25,
  "fecha": "2026-09-15T14:30:00Z"
}
```

> **Fechas con `Z`.** Las fechas de los eventos se serializan con `@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'")` sobre un `LocalDateTime`. Es la misma convención en Donaciones y en Logística. El sufijo `Z` supone que la JVM corre en UTC: así corren los contenedores, que no fijan `TZ`. Con otra zona, la hora publicada queda corrida.

#### Ruta Asignada (`ruta.asignada`)
```json
{
  "rutaId": "7f9c2e10-1234-4a5b-8c9d-0e1f2a3b4c5d",
  "donacionIndependienteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "fechaAsignacion": "2026-09-16T08:00:00Z"
}
```

#### Ruta Iniciada (`ruta.iniciada`)
```json
{
  "rutaId": "7f9c2e10-1234-4a5b-8c9d-0e1f2a3b4c5d",
  "camionId": "11223344-5566-7788-99aa-bbccddeeff00",
  "patenteCamion": "AB123CD",
  "donacionesIndependientesIds": [
    "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"
  ],
  "fechaInicio": "2026-09-16T09:15:00Z",
  "urlMapa": "http://localhost:8080/seguimiento/7f9c2e10-1234-4a5b-8c9d-0e1f2a3b4c5d"
}
```

#### Entrega Exitosa (`entrega.exitosa`)
```json
{
  "entregaId": "55667788-99aa-bbcc-ddee-ff0011223344",
  "donacionIndependienteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "camionId": "11223344-5566-7788-99aa-bbccddeeff00",
  "patenteCamion": "AB123CD",
  "fechaEntrega": "2026-09-16T11:40:00Z"
}
```

#### Entrega Fallida (`entrega.fallida`)
```json
{
  "entregaId": "55667788-99aa-bbcc-ddee-ff0011223344",
  "donacionIndependienteId": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "justificacion": "Domicilio sin atención al momento de la visita",
  "fechaFalla": "2026-09-16T11:40:00Z",
  "replanificable": true
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
