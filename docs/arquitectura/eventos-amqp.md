# Contratos y Esquema de Eventos AMQP (RabbitMQ) — DonaTrack

> **Catálogo Canónico de Mensajería Asíncrona, Topología de Colas y Schemas de Eventos**  
> **Productores:** `logistica-service`, `donaciones-service` (target AMQP / actual REST), `incentivos-service` (target AMQP / actual REST)  
> **Consumidores:** `donaciones-service`, `notificaciones-service`  

---

## 1. Arquitectura de Mensajería y Desacoplamiento Temporal

La interacción asíncrona entre microservicios se desacopla temporalmente mediante **RabbitMQ**:
* **Logística hacia Donaciones:** Cuando ocurren transiciones en el ciclo de vida de una ruta o de una entrega, `logistica-service` emite un evento de dominio hacia `logistica.exchange` sin bloquear la atención de peticiones HTTP ni requerir disponibilidad síncrona inmediata de `donaciones-service`.
* **Eventos de Dominio hacia Notificaciones:** Cuando ocurren eventos notificables en el ecosistema (`donante-registrado`, `donacion-asignada`, `donacion-recibida`, `donacion-vencida`, `mision-cumplida`, etc.), `notificaciones-service` expone `notificaciones.exchange` para ingesta asíncrona mediante el arnés de deduplicación Transactional Inbox. Los emisores actuales (`donaciones-service` e `incentivos-service`) despachan interinamente vía REST HTTP; su migración hacia publicación nativa en este exchange está catalogada en la deuda técnica [DTI-13](../adr/DEUDA_TECNICA.md#dti-13-migracion-de-clientes-consumidores-de-la-api-rest-deprecada-de-notificaciones-a-ruta-canonica-y-amqp).

---

## 2. Topología de RabbitMQ

### 2.1 Exchanges Principales
* **`logistica.exchange`:** `TopicExchange`, durable, para eventos del ciclo logístico de entregas y rutas.
* **`notificaciones.exchange`:** `TopicExchange`, durable, para ingesta multicanal de eventos de dominio notificables.

### 2.2 Colas, Routing Keys y Enlaces (Bindings)

| Routing Key | Nombre de la Cola | Consumidor | Propósito | JSON Schema Canónico |
|---|---|---|---|---|
| `ruta.asignada` | `donaciones.ruta.asignada` | `donaciones-service` (`LogisticaEventListener`) | Notifica que una donación fue incluida en una ruta planificada | [`evento-ruta-asignada.schema.json`](./contratos/schemas/evento-ruta-asignada.schema.json) |
| `ruta.iniciada` | `donaciones.ruta.iniciada` | `donaciones-service` (`LogisticaEventListener`) | Transiciona donaciones a `EN_TRASLADO` y difunde URL de tracking | [`evento-ruta-iniciada.schema.json`](./contratos/schemas/evento-ruta-iniciada.schema.json) |
| `entrega.exitosa` | `donaciones.entrega.exitosa` | `donaciones-service` (`LogisticaEventListener`) | Transiciona donación a `ENTREGADA` | [`evento-entrega-exitosa.schema.json`](./contratos/schemas/evento-entrega-exitosa.schema.json) |
| `entrega.fallida` | `donaciones.entrega.fallida` | `donaciones-service` (`LogisticaEventListener`) | Transiciona donación a `ENTREGA_FALLIDA` y evalúa replanificación | [`evento-entrega-fallida.schema.json`](./contratos/schemas/evento-entrega-fallida.schema.json) |
| `notificaciones.#` | `cola.eventos.notificaciones` | `notificaciones-service` (`NotificacionRabbitListener`) | Ingesta asíncrona de eventos de dominio con deduplicación por Transactional Inbox | [`evento-notificable.schema.json`](./contratos/schemas/evento-notificable.schema.json) |

---

## 3. Estructura de Cargas Útiles (Payloads JSON)

La serialización de mensajes se realiza mediante Jackson (`JacksonJsonMessageConverter`), garantizando compatibilidad tipada con Java 21 `record`.

### 3.1 EventoRutaAsignada (`ruta.asignada`)
Publicado cuando se planifica una entrega y se asigna a un camión.
```json
{
  "rutaId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "donacionIndependienteId": "98765432-abcd-ef01-2345-6789abcdef01",
  "fechaAsignacion": "2026-09-05T13:30:00"
}
```

### 3.2 EventoRutaIniciada (`ruta.iniciada`)
Publicado cuando el chofer inicia el recorrido de la ruta.
```json
{
  "rutaId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "camionId": "c1d2e3f4-5678-90ab-cdef-1234567890ab",
  "patenteCamion": "AA123BB",
  "donacionesIndependientesIds": [
    "98765432-abcd-ef01-2345-6789abcdef01",
    "12345678-90ab-cdef-1234-567890abcdef"
  ],
  "fechaInicio": "2026-09-05T14:00:00",
  "urlMapa": "https://maps.donatrack.utn.edu.ar/tracking/a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}
```

### 3.3 EventoEntregaExitosa (`entrega.exitosa`)
Publicado tras la confirmación de recepción en el domicilio de la entidad beneficiaria.
```json
{
  "entregaId": "e1f2a3b4-c5d6-7890-abcd-ef1234567890",
  "donacionIndependienteId": "98765432-abcd-ef01-2345-6789abcdef01",
  "camionId": "c1d2e3f4-5678-90ab-cdef-1234567890ab",
  "patenteCamion": "AA123BB",
  "fechaEntrega": "2026-09-05T15:30:00"
}
```

### 3.4 EventoEntregaFallida (`entrega.fallida`)
Publicado ante imposibilidad de concretar la entrega.
```json
{
  "entregaId": "e1f2a3b4-c5d6-7890-abcd-ef1234567890",
  "donacionIndependienteId": "98765432-abcd-ef01-2345-6789abcdef01",
  "justificacion": "Destinatario ausente tras tres intentos de contacto",
  "fechaFalla": "2026-09-05T16:00:00",
  "replanificable": true
}
```

### 3.5 EventoNotificable (`notificaciones.#`)
Publicado por microservicios emisores (`donaciones-service`, `incentivos-service`) para despachar alertas y notificaciones multicanal. Posee estructura polimórfica discriminada por el campo `tipo` y clave unívoca de idempotencia `eventId`.
```json
{
  "eventId": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "tipo": "DONANTE_REGISTRADO",
  "idPersonaDonante": "a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11",
  "fecha": "2026-09-05T12:00:00",
  "credencialesDeAcceso": "clave-inicial-123"
}
```
*(Catálogo completo de variantes polimórficas en [`evento-notificable.schema.json`](./contratos/schemas/evento-notificable.schema.json) y especificación de migración en [`guia-migracion-notificaciones-e4.md`](./contratos/guia-migracion-notificaciones-e4.md)).*

---

## 4. Idempotencia, Trazabilidad y Manejo de Fallas

1. **Idempotencia en los Receptores:**
   * **`donaciones-service`:** Toda transición ejecutada en su máquina de estados verifica si el estado destino es compatible o redundante (State Pattern). Si la donación ya se encuentra en el estado solicitado o en un estado posterior válido, la operación concluye de forma segura sin efectos colaterales.
   * **`notificaciones-service` (Transactional Inbox):** Cada evento procesado se registra de forma atómica en la tabla `notificaciones.evento_procesado` indexada por `event_id`. Si ingresa un mensaje con un `eventId` ya persistido, `NotificacionService` omite el reenvío de mensajes (Email, WhatsApp, SMS) y descarta el duplicado de forma idempotente. Para llamadas legacy sin `eventId`, se genera un identificador de resguardo.
2. **Trazabilidad Distribuida:** En la implementación actual, la correlación vía `traceId` opera sobre el tráfico HTTP síncrono mediante interceptores (`ControllerLoggingInterceptor`, Feign client). La propagación de `X-Trace-Id` en los headers de los mensajes RabbitMQ (`MessageProperties`) es una mejora técnica proyectada. En `notificaciones-service`, el `eventId` actúa como identificador de correlación e idempotencia de extremo a extremo.
3. **Manejo de Errores y Poison Pills:**
   * **`donaciones-service`:** En caso de excepción al procesar un evento de logística, `LogisticaEventListener` captura el error y registra el fallo en los logs (`log.error`), completando el ciclo del listener para no bloquear la cola.
   * **`notificaciones-service`:** El listener AMQP `NotificacionRabbitListener` no abre transacciones de base de datos a nivel listener (desacoplado de `@Transactional`), delegando la frontera transaccional a `NotificacionService.procesarEvento(...)`. Ante payloads irrecuperables (poison pills) o inconsistencias irremediables, se captura la excepción y se completa el mensaje para evitar bucles infinitos de redelivery y saturación del broker.
   * **Evolución Proyectada:** La incorporación de un Dead Letter Exchange (`dlx.exchange`) y Dead Letter Queue (`donaciones.dlq`, `notificaciones.dlq`) con reintentos exponenciales está catalogada como evolución de infraestructura pendiente.

