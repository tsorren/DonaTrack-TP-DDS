# Topología Pub/Sub AMQP y Desacoplamiento de Notificaciones

- Status: proposed
- Date: 2026-09-11
- Deciders: Decisión Grupal
- Tags: arquitectura, microservicios, amqp, rabbitmq, pub-sub, inbox, idempotencia, hard-cutover, dead-letter

## Contexto y Problema

Durante las Entregas 2 y 3, la integración entre microservicios hacia `notificaciones-service` se diseñó bajo un esquema asimétrico síncrono mediante clientes declarativos OpenFeign (`NotificacionesFeignClient`) orquestados tras eventos locales de Spring (`@EventListener`), documentado en el ADR [20260901-estrategia-de-comunicacion-asimetrica-inter-servicios.md](./20260901-estrategia-de-comunicacion-asimetrica-inter-servicios.md).

Con la evolución hacia la **Entrega 4** (`docs/entregas/4/Enunciado-4.pdf`, pág. 24), surgió la necesidad de desacoplar temporalmente a los emisores ante picos de carga y asegurar disponibilidad y tolerancia a fallos. En la rama `E4_n_bd` se introdujo una topología preliminar de consumidor centrada en un único `notificaciones.exchange` conectado a una única cola `cola.eventos.notificaciones`, recibiendo el DTO polimórfico `EventoNotificableDTO` con un discriminador artificial `"tipo"`. Asimismo, se catalogó la deuda técnica [DTI-13](./20260910-dti-13-migracion-clientes-api-rest-deprecada-notificaciones-a-amqp.md) para migrar los clientes legacy Feign hacia la ruta canónica `/api/notificaciones/eventos` y AMQP.

Sin embargo, dicho esquema preliminar presenta serias limitaciones arquitectónicas:
1. **Inversión de Propiedad de Exchanges (Violación DDD):** El consumidor no debe ser dueño del exchange al que publican múltiples bounded contexts independientes. En un paradigma Pub/Sub canónico, cada emisor (`donaciones-service`, `incentivos-service`) es dueño de su propio `TopicExchange` de dominio.
2. **Monocultivo de Colas y Contratos Acoplados:** Una sola cola consumiendo múltiples tipos de eventos heterogéneos mediante un DTO polimórfico (`EventoNotificableDTO`) obliga a incluir un discriminador `"tipo"` y un `eventId` forzado en el body JSON, contaminando los records de dominio.
3. **Ausencia de Aislamiento de Fallas Terminales:** Ante errores de deserialización o poison pills, la ausencia de un clúster dedicado de Dead Letter Exchange (DLX) y Dead Letter Queues (DLQ) segregated por bounded context compromete la estabilidad global del broker.
4. **Acoplamiento Temporal Residual:** La persistencia de llamadas OpenFeign entre microservicios mantiene dependencias síncronas innecesarias que violan el principio de autonomía de servicios.

## Atributos de Calidad y Drivers de Decisión

* **Autonomía y Desacoplamiento (DDD):** Los productores son soberanos de sus eventos de dominio y sus TopicExchanges; los consumidores gestionan sus propias colas y bindings.
* **Resiliencia y Tolerancia a Fallos:** Despacho asíncrono no bloqueante, tolerancia a caídas temporales de red y aislamiento de poison pills vía Dead Letter Queues dedicadas.
* **Procesamiento Efectivamente Único (*Effectively-Once Processing*):** Deduplicación atómica mediante Transactional Inbox relacional en el consumidor.
* **Higiene de Contratos y Protocol-Native Envelope:** Contratos limpios representados en Java 21 `record` sin discriminadores artificiales, aprovechando headers nativos AMQP (`message_id`, `timestamp`, `X-Trace-Id`, `__TypeId__`).
* **Retrocompatibilidad y Hard Cutover Controlado:** Erradicar OpenFeign en la comunicación inter-servicios (Hard Cutover) mientras se preservan adaptadores secundarios para testing, QA y herramientas externas.

## Relaciones Arquitectónicas

1. **Sucede (`supersedes`):** Reemplaza formalmente a [20260901-estrategia-de-comunicacion-asimetrica-inter-servicios.md](./20260901-estrategia-de-comunicacion-asimetrica-inter-servicios.md), abandonando la asimetría síncrona HTTP/OpenFeign para notificaciones en favor de una arquitectura 100% asíncrona basada en eventos AMQP.
2. **Evoluciona:** Extiende y potencia el ADR [20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones.md](./notificaciones-service/20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones.md), migrando la ingesta del Transactional Inbox desde el controlador REST hacia listeners AMQP (`@RabbitHandler`) tipados, extrayendo el identificador de idempotencia prioritariamente desde el header nativo `message_id`.
3. **Gobierna la Resolución de Deuda Técnica DTI-13:** Formaliza el diseño, topología y contratos para saldar la deuda técnica catalogada en [20260910-dti-13-migracion-clientes-api-rest-deprecada-notificaciones-a-amqp.md](./20260910-dti-13-migracion-clientes-api-rest-deprecada-notificaciones-a-amqp.md). En la presente Etapa 1 (Documental y Contratos) DTI-13 permanece catalogada como `[OBSERVED] in-progress`; su saldado efectivo y definitivo ocurrirá con la verificación de la implementación en código en la Etapa 2.

## Alternativas Consideradas

### Opción 1 (Elegida): Pub/Sub Canónico DDD con TopicExchanges de Emisor, Colas Segregadas y Hard Cutover
* **Exchanges de Emisores:** `donaciones.exchange` (TopicExchange) e `incentivos.exchange` (TopicExchange) declarados y gestionados por sus respectivos microservicios.
* **Colas Segregadas en Consumidor:** `notificaciones-service` declara y consume:
  - `notificaciones.donaciones`: vinculada a `donaciones.exchange` mediante bindings `donacion.*.v1`, `donante.*.v1` y `persona.*.v1` (o comodín `donacion.#`).
  - `notificaciones.incentivos`: vinculada a `incentivos.exchange` mediante binding `incentivo.*.v1` (o `incentivo.#`).
* **Dead Letter Cluster:** TopicExchange `notificaciones.dlx` enrutando a colas terminales aisladas `notificaciones.donaciones.dlq` y `notificaciones.incentivos.dlq`.
* **Protocol-Native Envelope:** Bodies JSON limpios de dominio (Java 21 `record` `Evento<X>V1`) validados por schemas JSON independientes. Metadata transportada en headers AMQP (`message_id: UUID`, `timestamp`, `X-Trace-Id`, `__TypeId__` con alias lógico versionado canónico `.v1`).
* **Mapeo de Tipos:** Configuración de `DefaultClassMapper` en `Jackson2JsonMessageConverter` con `classMapper.setTrustedPackages("*")` y listeners con métodos `@RabbitHandler` sobrecargados y tipados con records en español versionados (`EventoDonacionAsignadaV1`, etc.).
* **Hard Cutover Inter-Servicios:** Erradicación total de `NotificacionesFeignClient` planificada como arquitectura objetivo para la Etapa 2.

### Opción 2 (Rechazada): Exchange Único de Consumidor con Body Polimórfico (Topología Preliminar de `E4_n_bd`)
* Mantener `notificaciones.exchange` único donde todos los productores publican hacia una única cola `cola.eventos.notificaciones`.
* Los eventos comparten el DTO polimórfico `EventoNotificableDTO` discriminado por `"tipo"` y con `eventId` en el payload JSON.
* *Motivo de rechazo:* Viola la inversión de dependencias y el alineamiento DDD. Acopla a todos los productores a un DTO propietario de notificaciones y genera cuellos de botella e interferencia de fallas entre bounded contexts en una sola cola.

### Opción 3 (Rechazada): Mantener Arquitectura Híbrida Asimétrica (OpenFeign + AMQP)
* Continuar utilizando OpenFeign para llamadas inter-servicios donaciones/incentivos $\to$ notificaciones, utilizando AMQP solo para logística.
* *Motivo de rechazo:* Viola las consignas explícitas de la Entrega 4. Mantiene el acoplamiento temporal síncrono, la vulnerabilidad a timeouts en cascada y la duplicación de alertas ante reintentos de red.

## Resultado de la Decisión

Se aprueba la **Opción 1: Pub/Sub Canónico DDD con TopicExchanges de Emisor, Colas Segregadas y Hard Cutover**.

### Topología AMQP Consolidada

```text
donaciones-service                      notificaciones-service (Consumer)
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
```

### Catálogo de Eventos y Enrutamiento AMQP

| N° | Evento de Dominio | Emisor | TopicExchange | Routing Key | Tipo AMQP (`__TypeId__`) | Cola Consumidora | Payload / Schema |
|:---:|---|---|---|---|---|---|---|
| 1 | Donación Asignada | `donaciones-service` | `donaciones.exchange` | `donacion.asignada.v1` | `donacion.asignada.v1` | `notificaciones.donaciones` | [`evento-donacion-asignada-v1.schema.json`](../arquitectura/contratos/schemas/evento-donacion-asignada-v1.schema.json) |
| 2 | Donación en Camino | `donaciones-service` | `donaciones.exchange` | `donacion.en-camino.v1` | `donacion.en-camino.v1` | `notificaciones.donaciones` | [`evento-donacion-en-camino-v1.schema.json`](../arquitectura/contratos/schemas/evento-donacion-en-camino-v1.schema.json) |
| 3 | Donación Recibida | `donaciones-service` | `donaciones.exchange` | `donacion.recibida.v1` | `donacion.recibida.v1` | `notificaciones.donaciones` | [`evento-donacion-recibida-v1.schema.json`](../arquitectura/contratos/schemas/evento-donacion-recibida-v1.schema.json) |
| 4 | Entrega Fallida | `donaciones-service` | `donaciones.exchange` | `donacion.entrega-fallida.v1` | `donacion.entrega-fallida.v1` | `notificaciones.donaciones` | [`evento-donacion-entrega-fallida-v1.schema.json`](../arquitectura/contratos/schemas/evento-donacion-entrega-fallida-v1.schema.json) |
| 5 | Donación Vencida | `donaciones-service` | `donaciones.exchange` | `donacion.vencida.v1` | `donacion.vencida.v1` | `notificaciones.donaciones` | [`evento-donacion-vencida-v1.schema.json`](../arquitectura/contratos/schemas/evento-donacion-vencida-v1.schema.json) |
| 6 | Donante Registrado | `donaciones-service` | `donaciones.exchange` | `donante.registrado.v1` | `donante.registrado.v1` | `notificaciones.donaciones` | [`evento-donante-registrado-v1.schema.json`](../arquitectura/contratos/schemas/evento-donante-registrado-v1.schema.json) |
| 7 | Persona Sincronizada | `donaciones-service` | `donaciones.exchange` | `persona.sincronizada.v1` | `persona.sincronizada.v1` | `notificaciones.donaciones` | [`evento-persona-sincronizada-v1.schema.json`](../arquitectura/contratos/schemas/evento-persona-sincronizada-v1.schema.json) |
| 8 | Misión Cumplida | `incentivos-service` | `incentivos.exchange` | `incentivo.mision-cumplida.v1` | `incentivo.mision-cumplida.v1` | `notificaciones.incentivos` | [`evento-incentivo-mision-cumplida-v1.schema.json`](../arquitectura/contratos/schemas/evento-incentivo-mision-cumplida-v1.schema.json) |
| 9 | Subió de Categoría | `incentivos-service` | `incentivos.exchange` | `incentivo.subio-categoria.v1` | `incentivo.subio-categoria.v1` | `notificaciones.incentivos` | [`evento-incentivo-subio-categoria-v1.schema.json`](../arquitectura/contratos/schemas/evento-incentivo-subio-categoria-v1.schema.json) |
| 10 | Donante Inactivo | `incentivos-service` | `incentivos.exchange` | `incentivo.donante-inactivo.v1` | `incentivo.donante-inactivo.v1` | `notificaciones.incentivos` | [`evento-incentivo-donante-inactivo-v1.schema.json`](../arquitectura/contratos/schemas/evento-incentivo-donante-inactivo-v1.schema.json) |

### Envelope Nativo y Deserialización

1. **Headers AMQP Obligatorios:**
   * `message_id`: UUID generado unívocamente por el emisor en el momento de crear el evento de dominio. Utilizado como clave primaria de deduplicación en el Transactional Inbox.
   * `timestamp`: Fecha y hora de emisión del mensaje en el broker.
   * `X-Trace-Id`: Identificador de trazabilidad distribuida para observabilidad de extremo a extremo.
   * `__TypeId__`: Alias lógico versionado canónico (ej. `donacion.asignada.v1`, `incentivo.mision-cumplida.v1`), desacoplando nombres de paquetes Java entre emisor y receptor.
2. **Bodies JSON Limpios:** Los cuerpos de los mensajes contienen exclusivamente las propiedades intrínsecas de dominio, sin discriminadores artificiales ni IDs técnicos de infraestructura.
3. **Mapeo Tipado en Receptor:**
   ```java
   DefaultClassMapper classMapper = new DefaultClassMapper();
   classMapper.setTrustedPackages("*");
   Map<String, Class<?>> idClassMapping = new HashMap<>();
   idClassMapping.put("donacion.asignada.v1", EventoDonacionAsignadaV1.class);
   idClassMapping.put("donacion.en-camino.v1", EventoDonacionEnCaminoV1.class);
   idClassMapping.put("donacion.recibida.v1", EventoDonacionRecibidaV1.class);
   idClassMapping.put("donacion.entrega-fallida.v1", EventoDonacionEntregaFallidaV1.class);
   idClassMapping.put("donacion.vencida.v1", EventoDonacionVencidaV1.class);
   idClassMapping.put("donante.registrado.v1", EventoDonanteRegistradoV1.class);
   idClassMapping.put("persona.sincronizada.v1", EventoPersonaSincronizadaV1.class);
   idClassMapping.put("incentivo.mision-cumplida.v1", EventoIncentivoMisionCumplidaV1.class);
   idClassMapping.put("incentivo.subio-categoria.v1", EventoIncentivoSubioCategoriaV1.class);
   idClassMapping.put("incentivo.donante-inactivo.v1", EventoIncentivoDonanteInactivoV1.class);
   classMapper.setIdClassMapping(idClassMapping);
   ```

### Idempotencia y Estrategia de Persistencia (Resolución de Deuda Técnica DTI-13)

* **En `notificaciones-service`:**
  Se mantiene y refuerza el **Transactional Inbox Pattern** relacional sobre PostgreSQL. La tabla `evento_procesado` (`event_id UUID PRIMARY KEY, fecha_procesamiento TIMESTAMP`) garantiza deduplicación atómica $O(1)$ mediante inserción con control de colisión (`ON CONFLICT DO NOTHING`). El `NotificacionService` extrae el `eventId` prioritariamente desde el header AMQP `message_id`. Si ya fue procesado, el mensaje se reconoce (`ACK`) y se descarta de forma silenciosa e idempotente.
* **En `donaciones-service` e `incentivos-service`:**
  Ambos microservicios continúan operando 100% en memoria (`CrudRepositoryEnMemoria`), conforme a las constraints activas de la Fase 1 en `docs/context-index.md`. La publicación AMQP se realiza mediante `RabbitTemplate` y reintentos en memoria vía `OutboxStore`.
  > [!NOTE]
  > Se pospone la creación de tablas SQL físicas `outbox_events` con Flyway en `donaciones` e `incentivos` a la Oleada 10 como deuda técnica justificada, evitando forzar una migración prematura a PostgreSQL en servicios en memoria.

### Hard Cutover y Adaptadores Secundarios

> [!NOTE]
> **Calibración Epistémica de Implementación:** Las directivas de Hard Cutover (erradicación de `NotificacionesFeignClient` y baja definitiva de `POST /notificaciones`) constituyen la **arquitectura objetivo de SPEC-03 (Etapa 2)**. En la presente etapa de diseño y formalización de contratos (`[OBSERVED] Etapa 1`), los adaptadores legacy síncronos continúan temporalmente activos en el runtime del repositorio garantizando continuidad operativa y compatibilidad de suites existentes hasta su reemplazo en código.

1. **Comunicación Inter-Servicios 100% Asíncrona (Arquitectura Objetivo):**
   Se erradica el uso de `NotificacionesFeignClient` en `donaciones-service` e `incentivos-service`. Ningún microservicio invocará a `notificaciones-service` vía HTTP.
2. **Destino de Controladores en `notificaciones-service`:**
   * **Baja de Ruta Legacy:** Se eliminará definitivamente el alias `@PostMapping("/notificaciones")`.
   * **Preservación de Adaptador Secundario QA:** Se conserva `@PostMapping("/api/notificaciones/eventos")` exclusivamente como adaptador secundario para disparos manuales de QA, pruebas exploratorias con Postman y webhooks externos.
   * **Preservación de Consulta de Historial:** Se mantiene `@GetMapping({"/api/notificaciones/persona/{personaId}", "/notificaciones/persona/{personaId}"})` para garantizar compatibilidad con `NotificacionesApiClient` (`integration-tests`) y `flujo-8-e2e-distribuido.json`.
   * **Preservación de Ingesta Administrativa:** Se conserva `PUT /api/notificaciones/personas` en `PersonasController` para reconciliación y compatibilidad con `ContractIT`.

## Consecuencias Positivas

* **Desacoplamiento Absoluto:** Los microservicios emisores no sufren degradación ni bloqueos ante sobrecargas o caídas de `notificaciones-service`.
* **Segregación de Responsabilidades y Bounded Contexts:** Colas independientes impiden que fallos en un tipo de evento detengan el procesamiento de otros.
* **Higiene de Contratos:** Cero contaminación de DTOs polimórficos; contratos validados por JSON Schemas canónicos.
* **Tolerancia a Veneno (Poison Pills):** Los mensajes defectuosos son desviados automáticamente a sus DLQs dedicadas tras agotar reintentos, preservando la fluidez operativa.
* **Observabilidad de Extremo a Extremo:** Trazabilidad preservada mediante la inyección y propagación de `X-Trace-Id` en los headers de RabbitMQ.

## Consecuencias Negativas

* **Mayor Huella de Infraestructura:** RabbitMQ requiere declarar múltiples TopicExchanges, colas, bindings y DLQs (mitigado mediante autoconfiguración declarativa en `RabbitMQConfig`).
* **Consistencia Eventual:** Las notificaciones se generan de forma asíncrona; las pruebas de integración distribuidas requieren sincronización explícita con Awaitility.

## Validación y Criterios de Aceptación

1. **Unitarios y Modulares:** Tests de listeners AMQP (`DonacionesEventListenerTest`, `IncentivosEventListenerTest`) y publishers (`DonacionesEventPublisherTest`) pasando en verde.
2. **Integración con Testcontainers:** `NotificacionesAmqpInboxIT` (ArchUnit-compliant con sufijo `*IT`) valida el ciclo de vida completo: publicación hacia exchanges, recepción en colas segregadas, inserción en `evento_procesado`, descarte de duplicados y ruteo a DLQ.
3. **Contratos y Schemas:** Validación mecánica de los 10 JSON Schemas mediante `node scripts/validate-contracts.js`.
4. **Regresión E2E y QA:** Pruebas de `ContractIT` y colecciones Postman operando exitosamente contra la ruta canónica `/api/notificaciones/eventos`.
