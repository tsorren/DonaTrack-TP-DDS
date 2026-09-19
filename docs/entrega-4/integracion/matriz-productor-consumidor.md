# Matriz productor-consumidor — Entrega 4

> Entregable de **Fase 4** (`plan_entrega_4_aprendizaje_diseno_agentes.md`, §12). Vista tabular de los 9 eventos que publica `donaciones-service`, complementa a `catalogo-mensajes.md` (que es la fuente de verdad — ante cualquier diferencia entre ambos documentos, gana el catálogo).
>
> Reescrita el 19/9 — la versión anterior describía un estado especulativo previo a la reunión del 15/9 (organizado por "Ola 1/2/3"), con eventos que terminaron con otro nombre, otros que se descartaron, y sin el evento `DonacionSegmentadaV1` que se agregó después. Esta versión refleja los 9 eventos ya migrados a RabbitMQ e implementados en el código — no quedan eventos "propuestos" ni "abiertos".

## Vista desde Donaciones: routing key = hecho/intención, nunca "quién escucha"

Donaciones-service **nunca declara colas en su propio exchange** — solo publica en `donaciones.exchange` (topología confirmada en la reunión del 15/9: un `TopicExchange` por servicio productor). La única decisión que le toca es qué routing key le pone a cada mensaje, en función del propio dominio (qué hecho/intención comunica), **nunca** en función de cuántos servicios lo consumen. Agregar un consumidor nuevo a una routing key que ya existe es 0 líneas de cambio en `donaciones-service` — el trabajo entero (cola + binding + listener) es del lado del consumidor que se suma. Por eso son **9 routing keys para 9 eventos**, sin necesidad de filas de "reuso" separadas: cuando un evento tiene más de un consumidor (`DonanteRegistradoV1`, `DonacionRecibidaV1`, `PersonaSincronizadaV1`), se listan todos en la misma fila.

| Evento | Routing key | Consumidor(es) | Trigger | Consecuencia | Origen (pre-RabbitMQ) |
|---|---|---|---|---|---|
| `DonanteRegistradoV1` | `donante.registrado.v1` | Notificaciones, Incentivos | Alta de un `Donante` (`DonantesService.crearDonante`) | Notificaciones envía credenciales de bienvenida; Incentivos crea su `DonanteIncentivos` con el mapeo `donanteId↔personaId` | `NotificacionesFeignClient.enviarEvento` + `IncentivosFeignClient.registrarDonante` |
| `DonanteDadoDeBajaV1` | `donante.dado-de-baja.v1` | Incentivos | Baja de un `Donante` (`DonantesService.eliminarDonante`) | Incentivos da de baja al donante en su esquema de gamificación | `IncentivosFeignClient.darDeBaja` |
| `DonacionAsignadaV1` | `donacion.asignada.v1` | Notificaciones, Logística | `Propuesta` aprobada y confirmada (`PropuestaDeAsignacionService.onPropuestaAprobada`) | Notificaciones avisa al donante; Logística crea la Entrega directo a partir de este evento | `NotificacionesFeignClient.enviarEvento` + `LogisticaFeignClient.registrarEntregaPendiente` (el Command `EntregaSolicitadaV1` que se había propuesto para Logística se descartó — se suscribe a este mismo evento) |
| `DonacionRecibidaV1` | `donacion.recibida.v1` | Notificaciones, Incentivos | Logística confirma `entrega.exitosa` (`procesarDonacionRecibida`) | Notificaciones envía comprobante; Incentivos otorga puntos/medalla (mismo hecho que un eventual `donacion.entregada`, no hizo falta crear uno aparte) | `NotificacionesFeignClient.enviarEvento` + `IncentivosFeignClient.procesarDonacionExitosa` |
| `DonacionEnCaminoV1` | `donacion.en-camino.v1` | Notificaciones | Reemisión de `EventoRutaIniciada` de Logística (`procesarRutaIniciada`) | Notificar inicio de traslado | `NotificacionesFeignClient.enviarEvento` |
| `DonacionVencidaV1` | `donacion.vencida.v1` | Notificaciones | Vencimiento sin asignar (`procesarDonacionVencida`) | Alertar vencimiento | `NotificacionesFeignClient.enviarEvento` |
| `DonacionEntregaFallidaV1` | `donacion.entrega-fallida.v1` | Notificaciones | Reemisión de `EventoEntregaFallida` de Logística (`procesarDonacionFallida`) | Alertar a donante/beneficiaria/admin | `NotificacionesFeignClient.enviarEvento` |
| `PersonaSincronizadaV1` | `persona.sincronizada.v1` | Notificaciones, Incentivos | Alta, edición o anonimización de una `Persona` (`PersonasService.crearPersona`/`actualizarPersona`/`eliminarPersona`) | Notificaciones actualiza su réplica de contacto; Incentivos actualiza el nombre solo si el `personaId` corresponde a un donante ya registrado | `NotificacionesFeignClient.sincronizarPersona` (también reemplaza el `donante.modificado` que se había propuesto por separado) |
| `DonacionSegmentadaV1` | `donacion.segmentada.v1` | Incentivos | `Donacion` completa termina de segmentarse (`SegmentacionService`) | Incentivos calcula puntos según categoría y cantidad donada | `IncentivosFeignClient.procesarDonacion` |

## Notas que ya no aplican (dejadas fuera a propósito)

Tres cosas que estaban en la versión anterior de este documento y se sacaron por completo, no solo se corrigieron:

- **`PersonaReplicaV1`** — nombre viejo de `PersonaSincronizadaV1`, con un alcance más chico (solo "réplica" de datos, sin cubrir la anonimización ni confirmar a Incentivos como consumidor).
- **`EntregaSolicitadaV1`** (Command a Logística) — descartado en la reunión del 15/9; Logística se suscribe directo a `donacion.asignada.v1`.
- **`DonacionCargadaV1`** — versión temprana especulativa de lo que terminó siendo `DonacionSegmentadaV1`, con un propósito distinto ("intento de donación previo a éxito").
