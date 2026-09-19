# Catálogo de mensajes — Entrega 4

> Entregable de **Fase 4** (`plan_entrega_4_aprendizaje_diseno_agentes.md`, §11). Complementa `matriz-productor-consumidor.md`.
>
> Reemplaza la versión anterior de este documento (Ola 1/2/3 especulativas). Esta versión refleja lo confirmado en la reunión de equipo del 15/9, la corrección de Incentivos del 16/9 sobre IDs y eventos, y la investigación de la rama `E4_n_bd` (contratos ya parcialmente implementados para Notificaciones/Incentivos).

## Política común (aplica a todo mensaje de este catálogo salvo que se indique lo contrario)

- **Transporte:** `TopicExchange` durable, un exchange por servicio productor — `donaciones.exchange` para todo lo que sale de Donaciones. **Confirmado** en la reunión del 15/9 (Donaciones e Incentivos tienen su propio topic exchange; Notificaciones tiene colas dedicadas por productor). Ya no es una decisión abierta.
- **IDs:** nunca un campo combinado ambiguo (tipo `personaDonanteId`). Cuando un evento necesita el donante, viaja `donanteId`; si además hace falta correlacionar con la persona, viaja `personaId` por separado (solo donde el consumidor lo necesita, no en todos).
- **Outbox:** todo publish se escribe vía el mecanismo de reintento en memoria ya existente (`OutboxStore`/`OutboxEntry`), no la tabla física todavía — el equipo decidió priorizar comunicaciones antes que persistencia real para esta entrega. Migración a Outbox durable (JPA + `outbox_events`) queda para una oleada posterior.
- **Idempotencia:** todo mensaje lleva su identificador único como propiedad AMQP estándar `messageId`, no como campo del payload. **Confirmado** en la reunión del 15/9 ("el protocolo AMQP ya trae Message ID en el encabezado, no hace falta duplicarlo").
- **Versionado:** routing key versionado (`.v1`) + nombre de clase versionado (`Evento<Nombre>V1`, sin sufijo `DTO`). Confirmado como patrón ya en uso en la rama `E4_n_bd`.
- **Retry/DLQ (lado consumidor):** pendiente en todos los módulos (mandato de la reunión del 15/9, tres reintentos y luego Dead Letter Exchange). No es responsabilidad de Donaciones configurarlo del lado de quien consume.
- **`fecha`:** timestamp real del hecho de negocio, no `now()` del publisher.

## Donaciones nunca declara colas en su propio exchange

Solo publica. Cada consumidor (Notificaciones, Incentivos, Logística) declara su propia cola/binding contra `donaciones.exchange`, en su propio código. Agregar un segundo consumidor a una routing key ya existente es cero cambios en `donaciones-service`.

---

## DonanteRegistradoV1

- **Tipo:** Event · **Productor:** Donaciones · **Consumidor/es:** Notificaciones, Incentivos
- **Routing key:** `donante.registrado.v1`
- **Trigger:** alta de un `Donante` completada (`DonantesService.crearDonante`).
- **Payload:** `donanteId: UUID`, `personaId: UUID`, `nombre: String`, `fecha: DateTime`, `credencialesDeAcceso: String`
- **Consecuencia esperada:** Notificaciones envía credenciales de bienvenida al donante; Incentivos crea su `DonanteIncentivos` con el mapeo `donanteId ↔ personaId`.
- **Compatibilidad futura:** aditivo permitido (campo nuevo opcional); cambio de tipo o campo requerido es breaking → bump a v2.

## DonanteDadoDeBajaV1

- **Tipo:** Event · **Productor:** Donaciones · **Consumidor:** Incentivos
- **Routing key:** `donante.dado-de-baja.v1`
- **Trigger:** baja de un `Donante` (`DonantesService.eliminarDonante`).
- **Payload:** `donanteId: UUID`, `personaId: UUID`, `fecha: DateTime`
- **Consecuencia esperada:** Incentivos da de baja al donante en su esquema de gamificación.
- **No reemplaza ni se fusiona con `PersonaSincronizadaV1`:** son hechos distintos. `eliminarDonante` borra el agregado `Donante` (el rol) y nunca pasa por `PersonasService` — la `Persona` sigue existiendo intacta. `persona.sincronizada` solo se dispara cuando cambia la `Persona` en sí (alta/edición/anonimización). Confirmado el 18/9, cierra el ítem que estaba "ABIERTO" en `matriz-productor-consumidor.md`.

## DonacionAsignadaV1

- **Tipo:** Event · **Productor:** Donaciones · **Consumidor/es:** Notificaciones, Logística
- **Routing key:** `donacion.asignada.v1`
- **Trigger:** una `Propuesta` se aprueba y la `DonacionIndependiente` queda confirmada contra una `Necesidad`/`EntidadBeneficiaria` (`PropuestaDeAsignacionService.onPropuestaAprobada`).
- **Payload:** `donanteId: UUID`, `personaId: UUID`, `fecha: DateTime`, `personaBeneficiariaId: UUID`, `descripcion: String`, `destino: {calle, altura, piso?, departamento?, codigoPostal, localidad, provincia, pais}`, `pesoTotalKG: Number`, `volumenTotalM3: Number`
- **Consecuencia esperada:** Notificaciones avisa al donante del vínculo; Logística crea la Entrega directamente a partir de este evento (reemplaza lo que iba a ser un Command `EntregaSolicitadaV1` separado — Logística se suscribe a este mismo evento).
- **Nota:** no lleva categorías ni cantidades — eso es exclusivo de `DonacionSegmentadaV1`, que se dispara antes, en un punto distinto del flujo.

## DonacionRecibidaV1

- **Tipo:** Event · **Productor:** Donaciones · **Consumidor/es:** Notificaciones, Incentivos (reuso — misma routing key, sin cambios en Donaciones)
- **Routing key:** `donacion.recibida.v1`
- **Trigger:** Logística confirma `entrega.exitosa`, que dispara `DonacionesIndependientesNotificacionesService.procesarDonacionRecibida`.
- **Payload:** `donanteId: UUID`, `personaId: UUID`, `fecha: DateTime`, `personaBeneficiariaId: UUID`, `descripcion: String`, `patenteCamion: String`
- **Consecuencia esperada:** Notificaciones envía comprobante de recepción; Incentivos otorga puntos/medalla usando `donanteId` + `personaBeneficiariaId` (sin necesitar un evento `donacion.entregada` aparte — mismo hecho de negocio).

## DonacionEnCaminoV1

- **Tipo:** Event · **Productor:** Donaciones · **Consumidor:** Notificaciones
- **Routing key:** `donacion.en-camino.v1`
- **Trigger:** reemisión de `EventoRutaIniciada` (ya llega async de Logística) vía `procesarRutaIniciada`.
- **Payload:** `donanteId: UUID`, `personaId: UUID`, `fecha: DateTime`, `personaBeneficiariaId: UUID`, `descripcion: String`, `urlMapa: String`
- **Consecuencia esperada:** notificar inicio de traslado.

## DonacionVencidaV1

- **Tipo:** Event · **Productor:** Donaciones · **Consumidor:** Notificaciones
- **Routing key:** `donacion.vencida.v1`
- **Trigger:** `procesarDonacionVencida` (vencimiento sin asignar).
- **Payload:** `donanteId: UUID`, `personaId: UUID`, `fecha: DateTime`, `personaAdminId: UUID`, `descripcion: String`, `motivo: String`
- **Consecuencia esperada:** alertar vencimiento sin asignar.

## DonacionEntregaFallidaV1

- **Tipo:** Event · **Productor:** Donaciones · **Consumidor:** Notificaciones
- **Routing key:** `donacion.entrega-fallida.v1`
- **Trigger:** reemisión de `EventoEntregaFallida` (de Logística) vía `procesarDonacionFallida`.
- **Payload:** `donanteId: UUID`, `personaId: UUID`, `fecha: DateTime`, `personaBeneficiariaId: UUID`, `descripcion: String`, `personaAdminId: UUID`, `justificacion: String`, `replanificable: Boolean`
- **Consecuencia esperada:** alertar a donante/beneficiaria/admin.

## PersonaSincronizadaV1

- **Tipo:** Event (replicación de estado, no una orden) · **Productor:** Donaciones · **Consumidor/es:** Notificaciones, Incentivos
- **Routing key:** `persona.sincronizada.v1`
- **Trigger:** alta, modificación o baja de una `Persona` (`PersonasService.crearPersona` / `actualizarPersona` / `eliminarPersona`).
- **Payload:** `personaId: UUID`, `denominacion: String`, `tipoPersona: HUMANA|JURIDICA`, `mediosDeContacto: [{tipo, esPredeterminado?, direccionCorreo?, caracteristica?, codigoArea?, numero?}]`
- **Consecuencia esperada:** Notificaciones actualiza su réplica de datos de contacto; Incentivos, **solo si** el `personaId` corresponde a un donante que ya tiene registrado (mapeo armado desde `DonanteRegistradoV1`), actualiza el nombre (`denominacion`) — se dispara para cualquier persona del sistema, no solo donantes, así que el filtro es responsabilidad de cada consumidor.
- **Reemplaza:** el evento `donante.modificado` que se había propuesto — no hace falta, este ya cubre el caso.

## DonacionSegmentadaV1

- **Tipo:** Event · **Productor:** Donaciones · **Consumidor:** Incentivos
- **Routing key:** `donacion.segmentada.v1`
- **Trigger:** una `Donacion` completa termina de segmentarse en una o más `DonacionIndependiente` (`SegmentacionService`, al final de `procesarDonacionNormalizada`/`marcarSegmentadaYPublicar`).
- **Payload:** `donanteId: UUID`, `fecha: DateTime`, `items: [{categoria: String, cantidad: Integer}]` (una entrada por cada `DonacionIndependiente` resultante de esa `Donacion`, no un evento por cada una).
- **Consecuencia esperada:** Incentivos calcula puntos según categoría y cantidad donada.
- **Nota de diseño:** se decidió consolidar en **un solo evento por `Donacion`** (no uno por `DonacionIndependiente`, que era la granularidad del código actual) — cambia el punto de publish de "dentro del `for` de `registrarEnIncentivos`" a "después del `for`, una sola vez".
