# Contratos y Esquema de Eventos AMQP (RabbitMQ) — DonaTrack

> **Catálogo Canónico de Mensajería Asíncrona, Topología de Colas y Schemas de Eventos**  
> **Productor Principal:** `logistica-service`  
> **Consumidor Principal:** `donaciones-service`

---

## 1. Arquitectura de Mensajería y Desacoplamiento Temporal

La interacción entre el microservicio de logística y el microservicio de donaciones se desacopla temporalmente mediante **RabbitMQ**. Cuando ocurren transiciones en el ciclo de vida de una ruta o de una entrega, `logistica-service` emite un evento de dominio hacia el broker sin bloquear la atención de peticiones HTTP ni requerir disponibilidad síncrona inmediata de `donaciones-service`.

---

## 2. Topología de RabbitMQ

### 2.1 Exchange Principal
* **Nombre:** `logistica.exchange`
* **Tipo:** `TopicExchange`
* **Durable:** `true`
* **Auto-delete:** `false`

### 2.2 Colas, Routing Keys y Enlaces (Bindings)

| Routing Key | Nombre de la Cola | Consumidor | Propósito | JSON Schema Canónico |
|---|---|---|---|---|
| `ruta.asignada` | `donaciones.ruta.asignada` | `donaciones-service` (`LogisticaEventListener`) | Notifica que una donación fue incluida en una ruta planificada | [`evento-ruta-asignada.schema.json`](./contratos/schemas/evento-ruta-asignada.schema.json) |
| `ruta.iniciada` | `donaciones.ruta.iniciada` | `donaciones-service` (`LogisticaEventListener`) | Transiciona donaciones a `EN_TRASLADO` y difunde URL de tracking | [`evento-ruta-iniciada.schema.json`](./contratos/schemas/evento-ruta-iniciada.schema.json) |
| `entrega.exitosa` | `donaciones.entrega.exitosa` | `donaciones-service` (`LogisticaEventListener`) | Transiciona donación a `ENTREGADA` | [`evento-entrega-exitosa.schema.json`](./contratos/schemas/evento-entrega-exitosa.schema.json) |
| `entrega.fallida` | `donaciones.entrega.fallida` | `donaciones-service` (`LogisticaEventListener`) | Transiciona donación a `ENTREGA_FALLIDA` y evalúa replanificación | [`evento-entrega-fallida.schema.json`](./contratos/schemas/evento-entrega-fallida.schema.json) |

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

---

## 4. Idempotencia, Trazabilidad y Manejo de Fallas

1. **Idempotencia en el Receptor:** Toda transición ejecutada por `donaciones-service` en su máquina de estados verifica si el estado destino es compatible o redundante (State Pattern). Si la donación ya se encuentra en el estado solicitado o en un estado posterior válido, la operación concluye de forma segura sin efectos colaterales.
2. **Trazabilidad Distribuida:** En la implementación actual, la correlación vía `traceId` opera sobre el tráfico HTTP síncrono mediante interceptores (`ControllerLoggingInterceptor`, Feign client). La propagación de `X-Trace-Id` en los headers de los mensajes RabbitMQ (`MessageProperties`) es una mejora técnica proyectada.
3. **Manejo de Errores en Consumidores:** En caso de excepción al procesar un evento de logística, `LogisticaEventListener` captura el error y registra el fallo en los logs (`log.error`), completando el ciclo del listener para no bloquear la cola. La incorporación de un Dead Letter Exchange (`dlx.exchange`) y Dead Letter Queue (`donaciones.dlq`) con reintentos exponenciales está catalogada como deuda técnica / evolución de infraestructura pendiente.
