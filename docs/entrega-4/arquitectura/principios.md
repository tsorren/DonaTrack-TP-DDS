# Principios arquitectónicos comunes — Entrega 4

> Entregable de **Fase 0** (`plan_entrega_4_aprendizaje_diseno_agentes.md`, §4). Reglas comunes que afectan a los 4 servicios (Donaciones, Logística, Incentivos, Notificaciones), previas al diseño independiente de cada subgrupo.
>
> Criterio de salida: todos los subgrupos pueden explicar estos principios sin depender de un integrante específico.

Cada decisión sigue el template obligatorio del plan (Problema / Alternativas / Trade-offs / Decisión elegida / Consecuencias / Qué haría revisar esta decisión en el futuro).

---

## Decisión — Comunicación entre servicios

### Problema

Los 4 servicios necesitan reflejar cambios de estado y disparar efectos en otros servicios (ej: una entrega exitosa debe actualizar el estado de la donación y disparar una notificación) sin acoplarse síncronamente ni comprometer la disponibilidad de un servicio cuando otro está caído o lento.

### Alternativas

- **A — REST/Feign síncrono** para toda comunicación interservicio.
- **B — Mensajería asíncrona vía RabbitMQ** (eventos/comandos) para toda comunicación interservicio.
- **C — Híbrido sin criterio explícito** (el estado de facto actual del código: una parte por RabbitMQ, otra parte por Feign síncrono, sin regla escrita).

### Trade-offs

- **A:** simple de razonar, pero un servicio destino caído o lento bloquea o hace fallar al llamador; no hay buffer ante caídas; contradice el atributo de Disponibilidad ya declarado como prioritario (`docs/arquitectura/principios-diseno-arquitectura.md`, §5).
- **B:** más complejidad operativa (colas, DLQ, idempotencia, versionado de mensajes), pero desacople real y tolerancia a fallos parciales.
- **C (estado actual de facto):** no es una decisión consciente, es lo que quedó del código existente; dificulta razonar sobre qué falla si un servicio cae, porque depende de qué ruta de comunicación se usó en cada caso.

### Decisión elegida

**Alternativa B.** Toda comunicación de dominio entre servicios (eventos de cambio de estado, comandos que disparan efectos secundarios en otro servicio) es asincrónica vía RabbitMQ. Esta decisión ya está fijada a nivel de equipo (plan de Entrega 4, §1.2) — el trabajo de Fase 0 es formalizarla aquí y confrontarla contra el código real, no volver a discutirla.

### Consecuencias

**Gap detectado entre la decisión y el código real:** `donaciones-service` invoca hoy a Incentivos, Logística y Notificaciones mediante **Feign síncrono** con verbos de escritura (`POST`/`PUT`/`DELETE`), en código activo (no en tests):

| Cliente Feign | Usado desde |
|---|---|
| `infrastructure/clients/IncentivosFeignClient.java` | `DonantesService`, `SegmentacionService` |
| `infrastructure/clients/LogisticaFeignClient.java` | `LogisticaAsyncService` |
| `infrastructure/clients/NotificacionesFeignClient.java` | `NotificacionesAsyncService`, `DonacionesIndependientesNotificacionesService` |

(El sufijo "Async" en esas clases es `@Async` de Spring — sigue siendo HTTP síncrono por debajo, no un broker de mensajes.)

Este código queda marcado **BLOQUEANTE para el principio de comunicación asincrónica** y debe migrarse al mismo patrón que ya funciona para Logística → Donaciones (`LogisticaEventListener` vía `@RabbitListener`): (a) escribir el evento/comando en `outbox_events` dentro de la misma transacción que la mutación de dominio, (b) que `OutboxEventPublisher` lo despache a RabbitMQ, (c) que el servicio destino lo consuma de forma idempotente.

**Pendiente:** registrar esto formalmente en `docs/adr/DEUDA_TECNICA.md` (a definir con el equipo: ADR dedicado o extensión de uno existente — ver conversación).

### Qué haría revisar esta decisión en el futuro

Si aparece un caso de uso que necesita el resultado inmediato de otro servicio para responder al usuario (ej. una validación bloqueante antes de confirmar una operación) — ahí se evalúa si corresponde una excepción documentada (query síncrona de solo lectura, sin efectos de escritura) en vez de forzarla por RabbitMQ.

---

## Decisión — Ownership de datos y particionamiento de base de datos

### Problema

Con la migración a PostgreSQL hay que decidir la topología física de la base de datos entre los 4 servicios, y cómo se garantiza en la práctica —no solo por disciplina de código— que "ningún servicio consulta directamente la base de datos de otro" (principio ya declarado en el plan, §1.3).

### Alternativas

- **A — Una instancia de PostgreSQL por servicio** (aislamiento físico total).
- **B — Una instancia compartida, un único schema**, tablas distinguidas solo por prefijo de nombre (aislamiento por convención).
- **C — Una instancia compartida, un schema por servicio**, con un rol de base de datos dedicado por servicio, `search_path` fijado a su propio schema, y `REVOKE ALL` explícito sobre los schemas ajenos.

### Trade-offs

- **A:** aislamiento más fuerte posible, pero cuadriplica la infraestructura a operar (4 contenedores, 4 volúmenes, 4 configuraciones) sin necesidad real dado el volumen de datos de un proyecto académico.
- **B:** barata de levantar, pero el aislamiento es solo convención — un `SELECT *` o un JOIN mal escrito puede tocar tablas de otro servicio sin que el motor de base de datos lo impida.
- **C:** un solo contenedor Postgres que operar, con aislamiento **garantizado por el motor de base de datos**: un servicio no puede ver ni tocar el schema ajeno aunque el código tenga un bug, porque su rol no tiene permisos sobre ese schema.

### Decisión elegida

**Alternativa C.** Ya implementada parcialmente en `persistencia/init-db/01-init-schemas-roles.sql`: crea los 4 schemas (`donaciones`, `logistica`, `incentivos`, `notificaciones`) y, para `notificaciones_user` (el único servicio ya migrado a Postgres), aplica `GRANT` sobre su propio schema, `REVOKE ALL` sobre los otros tres, y fija `search_path`.

### Consecuencias

- Cada servicio que migre a Postgres debe completar el mismo bloque de `GRANT`/`REVOKE`/`search_path` para su rol **antes** de manejar datos reales — si no, el aislamiento no es real aunque el schema exista.
- `donaciones_user` tenía el rol creado pero sin este bloque completo. Se completó en esta misma rama (`persistencia/init-db/01-init-schemas-roles.sql`).
- El límite de este aislamiento es "quién puede conectarse con qué credencial": no impide que, dentro del propio servicio, alguien hardcodee a mano una connection string con las credenciales de otro servicio. La disciplina de nunca compartir/hardcodear credenciales ajenas complementa esto, no lo reemplaza.

### Qué haría revisar esta decisión en el futuro

Si el volumen de datos o los requisitos de disponibilidad de un servicio puntual exigen escalarlo independientemente del resto (ej. Donaciones crece mucho más que Notificaciones) — ahí se evalúa separar esa instancia específica a infraestructura propia.

---

## Decisión — Transactional Outbox: patrón de persistencia y distribución en capas

### Problema

El ADR (`docs/adr/20260901-patron-transactional-outbox-para-consistencia-eventual.md`, status: `proposed`) ya decidió **qué** patrón usar para resolver el dual-write (Transactional Outbox con polling worker), pero no dice **cómo** se distribuye esa lógica entre las capas que ya usa el proyecto (dominio / servicio de aplicación / repositorio / infraestructura), ni si el repositorio del outbox debería reusar el contrato genérico de `common-lib` (`CrudRepository<T extends AggregateRoot>` / `CrudRepositoryEnMemoria`) o ser uno propio.

Además, la implementación que hoy existe bajo `infrastructure/outbox/` (`OutboxEntry`, `OutboxStore`, `OutboxRetryScheduler`) **no es el patrón del ADR**: es un `ConcurrentHashMap` en memoria que guarda un `Runnable` (la llamada Feign fallida) con reintento por backoff exponencial. No tiene tabla en Postgres, no serializa un payload, no publica a RabbitMQ, y no sobrevive un reinicio del proceso — se usa hoy exactamente para envolver las llamadas Feign síncronas de `DonacionesIndependientesNotificacionesService` que ya quedaron marcadas como bloqueantes en la Decision Card de Comunicación entre servicios.

### Alternativas

- **A — Forzar el Outbox a través del contrato genérico `CrudRepository<T extends AggregateRoot>`** de common-lib, tratando cada entrada de outbox como un agregado más.
- **B — Repositorio propio para el Outbox** (`IOutboxRepository`, interfaz angosta y específica), sin heredar de `CrudRepository`/`CrudRepositoryEnMemoria`, siguiendo el mismo patrón que ya existe en el propio código para Idempotencia: `infrastructure/idempotency/IEventosConsumidosRepository` + `EventosConsumidosRepositoryEnMemoria` (interfaz a medida con `yaFueConsumido(...)`/`registrar(...)`, sin extender nada de common-lib).
- **C — Dejar el mecanismo actual** (`OutboxStore` con `Runnable` en memoria) y no migrarlo al patrón del ADR.

### Trade-offs

- **A:** reutiliza un contrato existente, pero `AggregateRoot` está pensado para conceptos de dominio con identidad de negocio (`Donación`, `Necesidad`, `Propuesta`); una entrada de outbox no es un agregado de dominio, es un registro técnico de infraestructura. El contrato genérico (`save`, `findAll`, `count`, `deleteAll`) tampoco tiene las operaciones que el outbox realmente necesita (`obtenerPendientes` filtrado por estado, `marcarProcesado`, `incrementarReintento`) — habría que forzarlas o contaminar el contrato compartido con métodos que solo le sirven al outbox.
- **B:** interfaz angosta (ISP, ya citado como principio del proyecto en `docs/arquitectura/principios-diseno-arquitectura.md` §3), consistente con un precedente que ya existe en el propio código. Es más código propio que reutilizar el genérico, pero es código que refleja exactamente lo que se necesita.
- **C:** cero esfuerzo de migración, pero deja dos problemas reales sin resolver: (1) si el proceso se reinicia con entradas pendientes, se pierden — un `Runnable` no es serializable ni persistente; (2) sigue dependiendo de que la llamada Feign síncrona exista para empezar — no resuelve el dual-write real (commit en DB vs. publish del evento), solo agrega reintentos sobre una llamada síncrona que, por la Decision Card de Comunicación, tiene que dejar de existir de todos modos.

### Decisión elegida

**Alternativa B.** El repositorio del Outbox **no hereda** de `CrudRepository`/`CrudRepositoryEnMemoria` de common-lib — sigue el mismo patrón que Idempotencia. Distribución en capas:

| Capa | Responsabilidad | Ejemplo concreto |
|---|---|---|
| **Dominio** (`models/`) | El agregado registra que ocurrió un evento de negocio, sin saber cómo se entrega. No conoce Outbox, RabbitMQ ni JSON. | `DonacionIndependiente` agrega un `PropuestaAprobada` a su lista transitoria de eventos al cambiar de estado. |
| **Servicio de aplicación** (`services/`) | Dentro de la **misma transacción** que persiste el agregado: toma los eventos de dominio producidos y los traduce a un registro de Outbox (payload serializado, `aggregate_type`, `aggregate_id`, `trace_id`), y los guarda vía el repositorio de Outbox. | `DonacionesIndependientesService.cambiarEstado(...)` guarda la entidad y el evento en la misma transacción, reemplazando el `try/catch` + Feign directo actual. |
| **Repositorio** (`infrastructure/outbox/`) | Interfaz propia `IOutboxRepository` (no extiende `CrudRepository`) con los métodos que realmente se usan: `guardar(evento)`, `obtenerPendientes(limit)` (el `SELECT ... FOR UPDATE SKIP LOCKED` del ADR), `marcarProcesado(id)`, `marcarError(id)`. | Análogo directo a `IEventosConsumidosRepository`. |
| **Infraestructura / Relay** (`infrastructure/outbox/`) | El worker (`@Scheduled`) que consulta pendientes, publica a RabbitMQ y marca el resultado. No es un servicio de dominio ni de aplicación — es un adaptador técnico puro. | Reemplaza a `OutboxRetryScheduler`, pero hablando contra el repositorio durable en vez de ejecutar un `Runnable` en memoria. |

### Consecuencias

- El código actual (`infrastructure/outbox/OutboxEntry.java`, `OutboxStore.java`, `OutboxRetryScheduler.java`) queda marcado como gap frente a esta decisión: implementa un mecanismo de reintento en memoria, no el Transactional Outbox del ADR. Se resuelve junto con la migración de la Decision Card de Comunicación entre servicios — es el mismo código (`DonacionesIndependientesNotificacionesService` envolviendo `notificacionesFeignClient`/`incentivosFeignClient`).
- El ADR de Outbox no cubre esta distribución en capas ni la decisión de no heredar de `common-lib`; esta Decision Card lo completa. Conviene referenciarla desde el ADR o desde `DEUDA_TECNICA.md` cuando se registre el ítem de migración.

### Qué haría revisar esta decisión en el futuro

Si en algún momento la lógica de polling/relay del Outbox necesitara ser literalmente idéntica en más de un servicio (hoy solo la usa Donaciones), ahí se evaluaría mover esa parte —no el contrato de agregados— a `common-lib` como componente reutilizable.

---

## Decisión — Criterio de idempotencia

### Problema

Los eventos que viajan por RabbitMQ pueden entregarse más de una vez (*at-least-once delivery*). Hace falta un criterio único de deduplicación que sirva tanto para lo que Donaciones ya consume (eventos de Logística) como para lo que va a empezar a producir/consumir a medida que se migren las comunicaciones Feign síncronas (Decision Card de Comunicación entre servicios). Hoy el único mecanismo que existe (`LogisticaEventListener` + `EventosConsumidosRepositoryEnMemoria`) deduplica por una **clave de negocio compuesta** (`eventType + businessId + donacionId`), no por un identificador de mensaje — porque hoy **no existe ningún `eventId`/`messageId` en ningún mensaje del sistema**, ni en el payload ni como propiedad AMQP (verificado: no hay `setMessageId` ni uso de `MessageProperties` en el publisher de Logística).

### Alternativas

- **A — Clave de negocio compuesta** (lo que ya existe): `eventType + businessId + entidad afectada`. Funciona sin cambiar ningún contrato, pero es ad-hoc por tipo de evento — cada mensaje nuevo obliga a pensar de nuevo cuál es "su" clave de negocio.
- **B — `eventId` único por mensaje**, generado por el productor, usado como clave de deduplicación en todos los consumidores. Dos formas de transportarlo:
  - **B1 — Como propiedad estándar AMQP `messageId`** (parte del sobre del mensaje, no del payload).
  - **B2 — Como campo dentro del payload JSON** de cada evento (igual que hoy viaja `traceId` embebido en algunos DTOs).

### Trade-offs

- **A:** cero costo de migración de contratos, pero no escala como criterio transversal — cada tipo de evento nuevo repite la discusión de cuál es su clave de negocio.
- **B (en general):** un solo campo, un solo criterio, sirve para cualquier evento futuro sin pensar de nuevo la clave; es lo que ya asume el ADR de Outbox ("eventId, clave de idempotencia para el receptor") y lo que pide el plan de Entrega 4 como concepto mínimo (§5.5).
- **B1 (header AMQP):** coherente con la plantilla de catálogo de mensajes del propio plan (§11), que documenta `messageId` como campo del sobre, separado del `Payload`; no hay que tocar ni versionar cada schema JSON para agregarlo. Contra: menos visible para quien solo mira el cuerpo del mensaje en logs o herramientas que no inspeccionan headers AMQP.
- **B2 (campo en payload):** totalmente visible en cualquier log o captura del mensaje sin inspeccionar headers; pero hay que agregarlo a cada schema/DTO existente y a cada uno nuevo, duplicando lo que ya resuelve `messageId` a nivel de transporte.

### Decisión elegida

**Alternativa B** (`eventId` como criterio único de deduplicación), **transversal a los 4 servicios**: todo mensaje que se intercambie entre servicios va a llevar un `eventId`. Esto se instrumenta como los dos patrones complementarios que ya menciona el plan (§1.2, §5.5): **Outbox** del lado del productor (genera y persiste el `eventId` junto con el evento, en la misma transacción que la mutación de dominio) e **Inbox** del lado del consumidor (usa ese `eventId` para descartar duplicados antes de procesar).

Sobre **B1 vs. B2** (dónde vive el `eventId`): quedan **las dos marcadas como válidas**, a decidir recién en Fase 4 (Diseño de contratos, donde participan productor y consumidor de cada mensaje) — no es algo que Donaciones pueda cerrar unilateralmente. **Recomendación: B1** (propiedad AMQP `messageId`), por ser coherente con la plantilla de catálogo de mensajes del plan y no requerir tocar cada schema; si el equipo prioriza visibilidad directa en logging simple por sobre esa consistencia, B2 es la alternativa razonable.

### Consecuencias

- **Gap con el código actual:** `LogisticaEventListener` + `EventosConsumidosRepositoryEnMemoria` deduplican hoy por clave de negocio (Alternativa A), no por `eventId`. Migrar requiere que Logística (el productor) empiece a generar y adjuntar un `eventId` a cada evento — es un cambio de contrato, no algo que Donaciones pueda hacer sola (regla del plan, §23: cambios de contrato requieren acuerdo entre productor y consumidor).
- Es transversal: cualquier servicio que empiece a producir mensajes (Donaciones vía Outbox, Incentivos, Notificaciones) genera `eventId` de la misma forma, y cualquiera que consuma aplica el mismo criterio de Inbox.
- El storage de la tabla de Inbox sigue en memoria por ahora (`ConcurrentHashMap`) — no se toca en Fase 0. Se documenta la decisión primero; la migración a tabla durable en el schema `donaciones` (y el cambio de clave a `eventId`) queda como trabajo de implementación para más adelante (Fase 3/10).

### Qué haría revisar esta decisión en el futuro

Si algún productor no puede garantizar unicidad real del `eventId` (ej. un reintento manual que genera un id nuevo por error), el fallback de "clave de negocio + estado ya alcanzado" que ya existe en `LogisticaEventListener` sigue siendo válido como red de seguridad adicional — no se elimina, complementa al criterio de `eventId`.

---

## Decisión — Versionado de mensajes

### Problema

Hoy **no existe ningún versionado** de mensajes en el sistema: ni en el nombre de la clase (`EventoRutaIniciada`, no `EventoRutaIniciadaV1`), ni en los schemas JSON (`version` no aparece en ninguno de `docs/arquitectura/contratos/schemas/`), ni en el nombre de las colas (`donaciones.entrega.fallida`) ni en los routing keys (`entrega.fallida`) de `RabbitMQConfig`. Esto contrasta con el propio plan de Entrega 4, que en su ejemplo de matriz productor-consumidor (§12) ya nombra los mensajes con sufijo de versión (`SolicitarPlanificacionV1`, `RutaIniciadaV1`).

Síntoma concreto de la falta de un criterio: en `donaciones-service` conviven `EventoEntregaFallida` (evento AMQP crudo recibido de Logística) y `EventoEntregaFallidaDTO` (forma completamente distinta, hacia Notificaciones) — dos contratos que terminaron distinguiéndose por sufijo `DTO` en vez de por una versión explícita. Sin un criterio, esa deriva se repite.

### Alternativas

- **A — Versión en el nombre de la clase/tipo** (`EventoEntregaFallidaV1`), solo del lado del código.
- **B — Versión en el routing key de RabbitMQ** (`entrega.fallida.v1`), aprovechando que el proyecto ya usa un `TopicExchange` con un routing key por tipo de evento.
- **C — Versión como campo explícito en el payload** (`"version": 1`).

### Trade-offs

- **A:** visible en el código y no requiere tocar infraestructura de RabbitMQ. Pero RabbitMQ no sabe nada de esa versión: dos formas distintas del mismo evento terminan en la **misma cola, mismo routing key**, mezcladas — el consumidor necesita algo *en el mensaje* para distinguirlas antes de poder mapearlas. Además, como cada servicio mantiene su propia copia de la clase (no hay contrato compartido vía `common-lib` — ver el caso `EventoEntregaFallida`/`EventoEntregaFallidaDTO`), nombrar la clase V1 de un lado no garantiza nada del otro lado: es convención de código local, no algo que viaje por la red.
- **B:** es el mecanismo que el `TopicExchange` ya expone para esto. Permite que productor viejo y nuevo publiquen en paralelo (cada uno a su routing key) y que cada versión vaya a **su propia cola** — coexistencia real: consumidores viejos y nuevos migran de a uno sin romper a nadie en el proceso. Auditable desde afuera del código (management UI de RabbitMQ muestra qué versiones están activas y quién las consume). Contra: solo versiona el transporte, sigue haciendo falta algo del lado del código que sepa deserializar el nuevo shape; y cuesta una cola/binding más por cada bump de versión (bajo, mismo patrón de `@Bean` que ya existe en `RabbitMQConfig`, pero no gratis).
- **C:** no toca infraestructura, es solo un campo JSON. Pero es el que peor encaja con el patrón actual: cada `@RabbitListener` está tipado directo a una clase concreta (`onEntregaFallida(EventoEntregaFallida evento)`, deserialización automática de Jackson). Meter la versión en el payload obligaría a cambiar ese patrón a algo genérico (recibir JSON crudo, leer `version`, recién ahí decidir a qué clase mapear) — un cambio de patrón de listeners más grande que lo que resuelve.

### Decisión elegida

**Alternativa B (routing key) combinada con A (nombre de clase versionado)**, no una sola de las tres de forma aislada: el routing key resuelve la coexistencia y el transporte durante una migración; el nombre de clase versionado del lado del código permite que el listener tipado de Spring siga funcionando igual que hoy, sin pasar a deserialización genérica. La Alternativa C (campo `version` en payload) queda como **complemento opcional**, no obligatorio, solo para el caso puntual de que un mismo consumidor necesite leer más de una versión desde la misma cola.

### Consecuencias

- Ningún mensaje existente hoy está versionado (ni clase, ni routing key, ni payload). No se migra retroactivamente en Fase 0 — el criterio se aplica de acá en adelante para mensajes nuevos y para el próximo cambio breaking de uno existente.
- Es una decisión de contrato, transversal a los 4 servicios (regla del plan, §23: cambios de contrato requieren acuerdo entre productor y consumidor) — el próximo mensaje nuevo o el próximo breaking change es quien primero aplica este criterio en la práctica.
- Falta definir la política de compatibilidad (qué cambio es aditivo vs. breaking, y por lo tanto cuándo corresponde bumpear versión) — queda pendiente como discusión siguiente, no cerrada en esta Decision Card.

### Qué haría revisar esta decisión en el futuro

Si el número de tipos de evento crece mucho y mantener una cola+routing key por versión de cada uno se vuelve difícil de operar, ahí se evaluaría un mecanismo más centralizado (ej. un registro de schemas tipo Schema Registry) — no se justifica hoy con el volumen de mensajes del proyecto.

---

## Decisión — Política de referencias entre servicios

### Problema

El principio base ya está fijado (§1.3 del plan, formalizado en la Decision Card de Ownership): las referencias externas se representan mediante identificadores, nunca objetos compartidos. Falta cerrar tres detalles concretos de esa política:

1. **Tipo del identificador**: hoy toda referencia —interna al agregado, entre agregados del mismo servicio, o entre servicios— es un `UUID` crudo. No existe ningún Value Object wrapper (`PersonaId`, `DonacionId`, etc.) en el proyecto, pese a que el plan (§6.3) pide distinguir esas tres categorías y no mapearlas todas igual.
2. **Validación de existencia**: `LogisticaEventListener` usa `entregaId`/`camionId` recibidos en eventos de Logística sin verificar que existan del otro lado. No hay ningún mecanismo de validación hoy.
3. **Referencias huérfanas**: sin FK real entre schemas (por la Decision Card de Ownership), si el lado dueño de una entidad la eliminara, la referencia quedaría colgada sin que la base de datos lo detecte.

### Alternativas y trade-offs

**1) Tipo del identificador**
- **A — UUID crudo en todo:** cero fricción, cero código extra, pero sin seguridad de tipos — nada impide pasar un `donacionId` donde se espera un `personaId`, y el inventario de qué referencia cruza servicio (§6.3) no queda marcado en ningún lado, solo en nombres de variable.
- **B — Value Object wrapper para todo:** máxima seguridad de tipos y coherente con lo que el proyecto ya hace para otros Value Objects (`Direccion`, `Localidad`, `PeriodoNecesidad`), pero implica ~12-15 wrappers y sus respectivos `AttributeConverter` (mismo patrón que `EstadoDonacionIndependienteConverter`) para todo el inventario de agregados, más anotaciones Jackson para no romper los schemas JSON existentes (que declaran esos campos como string UUID, no objeto). Ceremonia desproporcionada al riesgo real de las referencias puramente internas.
- **C — Wrapper acotado solo a IDs que forman parte de un contrato entre servicios** (la identidad de un agregado tal como aparece en el payload de un evento que otro servicio consume): concentra el costo justo donde el riesgo de un ID equivocado es más caro de detectar y corregir (corrompe consistencia entre sistemas de forma asíncrona, no un bug local que un test atrapa rápido), sin pagar la ceremonia completa de B ni resignar toda la seguridad de A.

**2) Validación de existencia**
- **A — Confiar siempre en el payload:** cero costo, coherente con la asincronía y el ownership ya decidido, pero un ID inventado o corrupto no se detecta hasta que falla algo más adelante.
- **B — Validar siempre:** máxima seguridad, pero reintroduce acoplamiento síncrono entre servicios exactamente donde la Decision Card de Comunicación ya decidió evitarlo.

**3) Referencias huérfanas**
- **A — Aceptarlas como parte de la consistencia eventual**, sin mecanismo de detección/reconciliación.
- **B — Prevenirlas activamente**, extendiendo la política de "nunca hard-delete, solo anonimizar/cambiar estado" (que Donaciones ya usa para `Persona` vía crypto-shredding) al resto del sistema, de forma que el dueño del dato nunca elimine algo que otro servicio pueda estar referenciando.

### Decisión elegida

1. **Alternativa C** (wrapper acotado a IDs de contrato entre servicios). No se aplica a referencias internas al mismo servicio (`Necesidad.entidadId`, `Propuesta.necesidadQueSatisfaceId` siguen siendo `UUID` crudo) — ahí el riesgo no justifica la ceremonia (KISS/YAGNI, ya citado como principio del proyecto).
2. **Alternativa A** (confiar en el payload por defecto). **Recomendación para más adelante, no regla cerrada hoy:** activar validación puntual solo cuando la operación sea irreversible o cara de revertir, afecte a datos de un tercero que no participó en la operación original, o cuando el propio evento ya traiga contexto suficiente para detectar la inconsistencia sin una llamada extra a otro servicio. Queda como criterio sugerido a aplicar caso por caso cuando aparezca una operación así, no como algo a implementar ahora.
3. **Alternativa A** (aceptar referencias huérfanas hoy, sin mecanismo de detección). La orientación general del proyecto ya es "eliminar lo menos posible" —anonimizar y cambiar de estado en vez de borrar, para mantener trazabilidad cuando haga falta (mismo patrón que crypto-shredding en `Persona`)— pero no se implementa ningún mecanismo nuevo de prevención u orfandad en Fase 0.

### Consecuencias

- El wrapper de IDs de contrato (punto 1) se aplica al próximo mensaje/contrato nuevo o al primer refactor de uno existente — no se migra retroactivamente todo lo que ya funciona.
- No se agrega ninguna validación de existencia ni mecanismo anti-orfandad en esta fase; ambos quedan escritos como mejoras posibles, no como trabajo pendiente obligatorio.

### Qué haría revisar esta decisión en el futuro

- Si aparece una operación real que cumpla los criterios del punto 2 (irreversible, afecta a un tercero, validación barata) — ahí se evalúa agregar validación puntual para ese caso, no para todos.
- Si empiezan a aparecer referencias huérfanas reales que generen problemas visibles (no solo teóricos) — ahí se evalúa extender la política de "anonimizar en vez de borrar" (Alternativa B del punto 3) al resto de las entidades referenciadas entre servicios.

---

## Decisión — Responsabilidades de `common-lib`

### Problema

`common-lib` ya contiene un conjunto coherente de responsabilidades (repositorios genéricos `CrudRepository`/`CrudRepositoryEnMemoria`/`CrudRepositoryJpaAdapter` + `AggregateRoot`; una base de Domain Events genérica `AgregadoConEventos<E>`/`EventoDeDominio`, esta última con un `id: UUID` autogenerado en el constructor — el mismo concepto de `eventId` que se decidió como criterio de idempotencia; jerarquía de excepciones + `GlobalExceptionHandler`; infraestructura de tracing/logging; config compartida de OpenAPI; utilidades de test compartidas), y ya existe una regla **automatizada** que impide que dependa de dominio de ningún servicio (`common-lib/src/test/java/grupo5/common/architecture/CommonArchitectureTest.java`, ArchUnit: `common-lib` no puede depender de `grupo5.donaciones..`, `grupo5.incentivos..`, `grupo5.logistica..` ni `grupo5.notificaciones..`).

Lo que falta es un criterio explícito de **cuándo algo nuevo entra a `common-lib`**. El propio historial del proyecto ya muestra un patrón de facto: Outbox e Idempotencia son conceptualmente genéricos (cualquier servicio que consuma/produzca mensajes los necesita), pero ambos nacieron dentro de `donaciones-service`, no en `common-lib` — nadie los puso ahí de entrada.

### Alternativas

- **A — Proactivo:** todo lo que se identifique como conceptualmente genérico entra a `common-lib` desde el diseño, aunque todavía lo use un solo servicio.
- **B — Reactivo (lo que pasó de hecho con Outbox/Idempotencia):** nada nuevo entra a `common-lib` hasta que un segundo servicio necesite exactamente lo mismo en la práctica.
- **C — Híbrido, basado en certeza de decisión ya tomada:** lo que es una necesidad cierta y compartida por diseño desde el principio (repositorios, manejo de excepciones, tracing — todo servicio lo necesita sí o sí) entra proactivamente; lo que es incierto se queda local hasta que se repita (Alternativa B); pero cuando el equipo **ya decidió explícitamente** que algo va a ser transversal (como Outbox e Inbox, por las Decision Cards de Outbox e Idempotencia de este mismo documento), se promueve a `common-lib` de una vez, sin esperar a que se duplique 3 veces más para recién ahí consolidar.

### Trade-offs

- **A:** evita duplicación, pero arriesga abstracciones especuladas sin caso real que terminan mal ajustadas a como cada servicio realmente necesita usarlas — el riesgo que el proyecto ya evitó orgánicamente al construir Outbox/Idempotencia primero dentro de Donaciones antes de generalizar.
- **B:** garantiza que todo lo compartido nace de necesidad demostrada, nunca de una suposición — pero en el caso de Outbox/Inbox significaría construirlos 4 veces (uno por servicio) sabiendo de antemano que se va a repetir, para recién después consolidar. Desperdicio evitable cuando la necesidad ya está decidida, no es una sospecha.
- **C:** usa el conocimiento que el equipo ya tiene (Outbox e Inbox son transversales, ya decidido en Cards anteriores) para promoverlos ahora, sin caer en promover cosas todavía inciertas. Es más criterio para aplicar caso por caso que una regla mecánica única, pero evita los dos extremos de A y B.

### Decisión elegida

**Alternativa C.** Para Outbox e Idempotencia concretamente: el **contrato** (`IOutboxRepository`, `IInboxRepository` o equivalente) y el **worker/relay genérico** (polling, reintentos, publish a RabbitMQ) se diseñan para vivir en `common-lib`, siguiendo el mismo patrón que ya existe con `CrudRepositoryJpaAdapter` (clase base abstracta que cada servicio extiende con lo específico suyo). La **traducción entre un evento de dominio concreto y un registro de outbox/inbox** se queda en cada servicio, porque ahí sí depende del dominio de cada uno.

### Consecuencias

- No se implementa nada de esto en Fase 0 — se documenta la decisión primero (misma regla ya aplicada en Idempotencia y Versionado). Cuando se construya el Outbox real (reemplazando el mecanismo en memoria actual) y el Inbox con clave `eventId` (reemplazando la clave de negocio actual), el diseño nace pensado para `common-lib` desde el principio, no como refactor posterior.
- La regla ArchUnit existente (`CommonArchitectureTest`) sigue aplicando sin cambios: el nuevo código de Outbox/Inbox en `common-lib` tiene que ser igual de agnóstico a paquetes de servicio que el resto de la librería.

### Qué haría revisar esta decisión en el futuro

Si en la práctica el molde genérico de Outbox/Inbox no le encaja bien a algún servicio (ej. necesita una forma de payload muy distinta a la de los demás), ese servicio puede no extender la base común y resolverlo local — la Alternativa C no obliga a forzar todo al molde compartido, solo dice que el molde se diseña en `common-lib` cuando ya se sabe que sirve para todos.

---

## Decisión — Convenciones de naming

### Confirmadas sin cambios (formalizadas, no había alternativa real a discutir)

- **Términos de dominio en español** (`Donación`, `Necesidad`, `Propuesta`, `Entrega`, `Camión`), con sufijos técnicos en inglés (`Repository`, `Exception`, `Handler`, `Config`, `Scheduler`, `Converter`) — coherente en toda la base, incluso en nombres híbridos como `RecursoNoEncontradoException`.
- **Repositorios:** interfaz `I<Nombre>Repository` + implementación `<Nombre>RepositoryEnMemoria` (o `...JpaAdapter` cuando corresponda) — 100% consistente, se mantiene igual.
- **Colas y routing keys de RabbitMQ:** cola con prefijo del consumidor (`donaciones.entrega.fallida`), routing key sin prefijo (`entrega.fallida`) — separa "quién escucha" de "qué pasó", se mantiene igual.

### Inconsistencia 1 — Nombres de IDs foráneos (prefijo vs. sufijo)

**Problema:** en `donaciones-service` conviven 42 campos con sufijo (`donacionId`, `entregaId`, `personaId`) contra 11 con prefijo (`idPersona`, `idDonante`, `idNecesidad`). No es una submodalidad con su propia lógica (ej. "prefijo en DTOs salientes") — los 11 casos con prefijo aparecen mezclados incluso ahí: `DonacionExitosaRequest` usa sufijo (`donanteId`) en el mismo tipo de contrato donde otros usan prefijo.

**Alternativas:**
- **A — Dejarlo mixto**, sin regla.
- **B — Documentar una lógica de separación**, si existiera una razón real para tener ambos estilos.
- **C — Unificar todo a un solo estilo.**

**Trade-offs:** A perpetúa la inconsistencia sin ningún beneficio a cambio. B requeriría que exista una lógica real detrás de la separación — se buscó y no la hay (el caso `DonacionExitosaRequest` la contradice). C tiene el costo de un rename en 11 lugares, pero elimina una inconsistencia sin justificación real.

**Decisión elegida:** **C**, unificando a **sufijo** (`personaId`, `donanteId`, `entregaId`) — es el estilo que ya domina ampliamente (42 contra 11) y es el que ya usa el propio dominio para sus identificadores locales (`Aggregate.getId()`, convención de `common-lib`).

**Consecuencias:** no se migran los 11 casos existentes de forma retroactiva en Fase 0 — se corrigen oportunistamente cuando se toque ese código, y todo campo/parámetro nuevo usa sufijo desde ya.

### Inconsistencia 2 — Sufijo "DTO" en clases de evento (`Evento<X>` vs. `Evento<X>DTO`)

**Problema:** a primera vista parece una inconsistencia de nombres (`EventoEntregaFallida` vs. `EventoEntregaFallidaDTO`), pero al revisar el uso real **sí hay una lógica detrás**, simplemente no está escrita en ningún lado: `Evento<X>` (sin sufijo) se usa para la clase que representa el **payload tal como viaja por RabbitMQ** (lo que llega a un `@RabbitListener` o lo que se publica al exchange); `Evento<X>DTO` se usa para una forma **derivada, adaptada para un canal síncrono** (el body de una llamada Feign saliente, con campos distintos a los del evento AMQP original).

**Alternativas:**
- **A — Unificar** (sacar el sufijo `DTO` en todos lados, ya que "total, todos son eventos").
- **B — Documentar la lógica ya existente** como regla explícita, sin renombrar nada.

**Trade-offs:** A borraría una distinción que hoy es real y útil (distinguir "esto es el mensaje AMQP" de "esto es un body de Feign derivado de ese mensaje, con otra forma") — se perdería información al unificar. B no tiene costo de migración y hace explícito algo que ya se sigue en la práctica, solo que nadie lo escribió.

**Decisión elegida:** **B.** Regla: una clase sin sufijo `DTO` que empiece con `Evento` representa el payload real de un mensaje AMQP (entrante o saliente). Una clase con sufijo `DTO` es una forma derivada de ese evento para un canal distinto (típicamente el body de una llamada REST/Feign saliente) — nunca el mismo contrato que viaja por RabbitMQ.

**Consecuencias:** no se renombra nada existente. La regla se aplica al nombrar clases nuevas: si es literalmente el payload AMQP, sin `DTO`; si es una forma adaptada para REST/Feign, con `DTO`.

### Qué haría revisar esta decisión en el futuro

Si en algún refactor grande de `donaciones-service` se toca de todos modos gran parte del código (ej. la migración a Postgres), ahí conviene aprovechar y limpiar los 11 casos de prefijo `idX` restantes en el mismo pase, en vez de esperar a que se toquen uno por uno.

---

## Decisión — Estrategia de errores y retries

### Problema

Hoy **no hay ninguna configuración de retry ni de dead-letter en RabbitMQ**, en ningún servicio. Se verificó exhaustivamente: ningún `application.properties`/`.yml` de los 4 servicios tiene configuración de retry (`spring.rabbitmq.listener.simple.retry`) ni de DLX; ningún `RabbitMQConfig.java` declara argumentos de `x-dead-letter-exchange` en sus colas; no hay `@Retryable`, `RetryOperationsInterceptor`, `RepublishMessageRecoverer` ni `RetryTemplate` en todo el repositorio; el `docker-compose.yml` levanta RabbitMQ vainilla, sin importar ninguna definición de colas/políticas. El comportamiento por defecto de Spring AMQP ante una excepción no capturada en un `@RabbitListener` es reencolar el mensaje — un mensaje "envenenado" (que siempre falla) generaría hoy un loop de redelivery infinito.

A favor: la jerarquía de excepciones de `common-lib` ya distingue, sin que nadie lo haya conectado todavía a una política de retry, lo transitorio de lo terminal: `InfrastructureException` envuelve una causa externa (red, DB, storage — algo que un reintento podría resolver), mientras que `BusinessStateException`/`ValidationException` son violaciones de regla de negocio (reintentar no cambia el resultado).

### Distinción importante: esto no es lo mismo que Outbox

Outbox (Decision Card correspondiente) y la estrategia de retry de esta Card resuelven problemas distintos, aunque comparten la misma "forma" general (algo falla, se espera, se reintenta con backoff, se cuenta el intento):

- **Outbox** actúa del lado del **productor**, *antes* de que el mensaje llegue al broker: garantiza que la escritura en DB y la publicación del evento sean atómicas (el dual-write).
- **Retry + DLQ** (esta Card) actúa del lado del **consumidor**, *después* de que el mensaje ya fue entregado por el broker: decide qué hacer cuando el procesamiento de un mensaje ya recibido falla.

De hecho, el mecanismo que hoy existe en el código bajo el nombre "Outbox" (`OutboxStore`/`OutboxRetryScheduler`, ya señalado como gap en la Decision Card de Outbox) es, en la forma, más parecido a un retry-con-backoff casero para una llamada saliente que al patrón Outbox real — otra razón más para no seguir llamándolo así una vez que se migre.

### Alternativas (para el lado consumidor)

- **A — Retry interceptor + DLX/DLQ con `RepublishMessageRecoverer`:** Spring Retry reintenta N veces en memoria (sin devolver el mensaje al final de la cola), con backoff configurable; agotados los reintentos, se republica automáticamente a una cola de dead-letter para inspección/replay manual.
- **B — Cola de retry con TTL y dead-lettering automático:** el mensaje fallido se enruta a una cola intermedia con TTL; al expirar, el broker lo dead-letra de vuelta a la cola original — reintento sin bloquear el hilo consumidor, backoff real entre intentos.

### Trade-offs

- **A:** topología simple (una DLQ por cola original), backoff configurable sin infraestructura extra. Contra: los reintentos en memoria bloquean el hilo consumidor mientras esperan el backoff — si el backoff es largo, ese consumidor no procesa otros mensajes mientras tanto.
- **B:** no bloquea el consumidor, backoff real entre intentos. Contra: más colas para declarar y razonar (una cola de retry por paso de backoff, o una con TTL fijo si no hace falta backoff exponencial) — más complejidad operativa para el volumen de mensajes de este proyecto.

### Decisión elegida

**Alternativa A** (Retry interceptor + DLX/DLQ con `RepublishMessageRecoverer`). Se aprovecha la distinción que ya existe en la jerarquía de excepciones: solo `InfrastructureException` (y sus causas) dispara reintento; `BusinessStateException`/`ValidationException` van directo a dead-letter sin reintentar, porque reintentar una violación de regla de negocio no cambia el resultado.

### Consecuencias

- No se implementa nada en Fase 0 — se documenta la decisión primero, misma regla aplicada al resto de las Cards. Queda como trabajo de Fase 10 (Implementación), sobre las 4 colas ya declaradas en `RabbitMQConfig`.
- Es aplicable en principio a los 4 servicios por igual (cualquiera que consuma mensajes necesita esta política), aunque cada uno declara sus propias colas y por lo tanto su propia configuración de retry/DLQ — no es algo que viva en `common-lib` salvo que, siguiendo el criterio ya fijado en la Decision Card de `common-lib`, un segundo servicio la necesite en una forma idéntica.

### Qué haría revisar esta decisión en el futuro

Si el bloqueo del hilo consumidor durante el backoff de la Alternativa A se vuelve un cuello de botella real (alto volumen de mensajes fallidos simultáneos, consumidores sin hilos libres para el resto de la cola) — ahí se evalúa migrar a la Alternativa B para esa cola puntual.

---

## Decisión — Consistencia eventual entre servicios (resuelta vía otras Cards)

Este tema del checklist de Fase 0 (§4 del plan) no tiene una alternativa propia que evaluar acá: ya queda resuelto por la combinación de dos decisiones cerradas en este mismo documento. **Outbox** (el productor persiste el evento junto con su mutación de dominio, en la misma transacción, y lo publica de forma desacoplada) e **Inbox/Idempotencia** (el consumidor deduplica por `eventId` antes de aplicar el efecto) son, juntos, el mecanismo que tolera que un evento tarde en llegar, llegue duplicado, o que el consumidor esté temporalmente caído, sin perder ni duplicar el efecto de negocio.

Se deja como entrada propia solo para que el checklist de Fase 0 quede completo — la decisión de fondo está en las Cards de **Transactional Outbox** y **Criterio de idempotencia**, no acá.

---

## Decisión — Contratos independientes del dominio (resuelta vía otras Cards + evidencia ya en código)

**Principio:** los DTOs que cruzan el límite de un servicio (eventos AMQP, requests/responses REST) no son ni las entidades JPA de persistencia ni los objetos de dominio — son una forma propia, mapeada explícitamente en ambos sentidos.

No es un principio por construir, ya es una práctica seguida en el código: los controllers de Donaciones mapean DTOs de entrada/salida mediante mappers dedicados (`DonacionIndependienteMapper`, `EntregaMapper`, ya citados en `docs/arquitectura/principios-diseno-arquitectura.md` §2), sin exponer nunca las entidades de dominio directamente a la red.

Las consecuencias concretas de este principio para los contratos de integración ya están tratadas en las Cards de **Versionado de mensajes**, **Política de referencias entre servicios**, y la regla `Evento<X>` vs. `Evento<X>DTO` de **Convenciones de naming**. Se deja esta entrada solo para dejar registrado el principio en sí, tal como lo pide el checklist de Fase 0.
