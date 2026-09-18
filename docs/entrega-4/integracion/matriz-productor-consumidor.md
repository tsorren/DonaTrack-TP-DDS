# Matriz productor-consumidor — Entrega 4

> Entregable de **Fase 4** (`plan_entrega_4_aprendizaje_diseno_agentes.md`, §12). Construida a partir del inventario real de integraciones Feign de `donaciones-service` (no son ejemplos del plan, son los flujos que existen hoy en el código) y del ADR de comunicación asimétrica (`docs/adr/20260901-estrategia-de-comunicacion-asimetrica-inter-servicios.md`).
>
> Alcance de esta versión: solo las comunicaciones salientes de **Donaciones** que hoy son Feign síncrono y van a migrar a RabbitMQ. No incluye Logística→Donaciones (ya es async, ver `docs/arquitectura/eventos-amqp.md`) ni comunicaciones internas de otros servicios que Donaciones no origina.
>
> Estado de columna "Fase": indica en qué oleada de `docs/entrega-4/donaciones/plan-implementacion-rabbitmq.md` se implementa cada mensaje.
>
> **Nota:** esta tabla usa `donaciones.exchange` como placeholder de trabajo. La topología de exchange (uno por servicio vs. uno compartido) todavía **no está decidida por el equipo** — ver `principios.md` §Convenciones de naming y la conversación con el agente. Si se elige la alternativa de exchange compartido, solo cambia el nombre del exchange en esta tabla, no la forma de los mensajes.

## Vista desde Donaciones: routing key = hecho/intención, nunca "quién escucha"

Donaciones-service **nunca declara colas en su propio exchange** — solo publica. La única decisión que le toca es qué routing key le pone a cada mensaje, en función del propio dominio (qué hecho/intención comunica), **nunca** en función de cuántos servicios lo consumen. Agregar un consumidor nuevo a una routing key que ya existe es 0 líneas de cambio en `donaciones-service` — el trabajo entero (cola + binding + listener) es del lado del consumidor que se suma. Por eso son **8 routing keys**, no 12 mensajes: las dos filas de reuso de Ola 2 comparten routing key con Ola 1, no suman una nueva.

| Mensaje | Tipo | Productor | Consumidor(es) | Consecuencia | Reemplaza (Feign) | Cambio en `donaciones-service` | Fase |
|---|---|---|---|---|---|---|---|
| `DonanteRegistradoV1` (`donante.registrado.v1`) | Event | Donaciones | Notificaciones | Enviar credenciales/bienvenida al donante | `NotificacionesFeignClient.enviarEvento` (tipo `DONANTE_REGISTRADO`) | Nuevo — Slice C | Ola 1 |
| `DonacionAsignadaV1` (`donacion.asignada.v1`) | Event | Donaciones | Notificaciones | Notificar vínculo donación↔beneficiaria | `NotificacionesFeignClient.enviarEvento` (tipo `DONACION_ASIGNADA`) | Nuevo — Slice C | Ola 1 |
| `DonacionRecibidaV1` (`donacion.recibida.v1`) | Event | Donaciones | Notificaciones | Notificar comprobante de recepción | `NotificacionesFeignClient.enviarEvento` (tipo `DONACION_RECIBIDA`) | Nuevo — Slice C | Ola 1 |
| `DonacionEnCaminoV1` (`donacion.en-camino.v1`) | Event | Donaciones | Notificaciones | Notificar inicio de traslado logístico | `NotificacionesFeignClient.enviarEvento` (tipo `DONACION_EN_CAMINO`) | Nuevo — Slice C | Ola 1 |
| `DonacionVencidaV1` (`donacion.vencida.v1`) | Event | Donaciones | Notificaciones | Alertar vencimiento sin asignar | `NotificacionesFeignClient.enviarEvento` (tipo `DONACION_VENCIDA`) | Nuevo — Slice C | Ola 1 |
| `EntregaFallidaV1` (`entrega.fallida.v1`) | Event | Donaciones | Notificaciones | Alertar a donante/beneficiaria/admin | `NotificacionesFeignClient.enviarEvento` (tipo `ENTREGA_FALLIDA`) | Nuevo — Slice C | Ola 1 |
| `PersonaReplicaV1` (`persona.replica.v1`) | Event | Donaciones | Notificaciones (hoy). **ABIERTO:** ¿también Incentivos y Logística? (`docs/generated/events-catalog.md` lo sugiere sin decisión formal — ver Fase 4 de Ola 1) | Sincronizar copia de datos de persona | `NotificacionesFeignClient.sincronizarPersona` | Nuevo — Slice D | Ola 1 |
| `DonacionRecibidaV1` (`donacion.recibida.v1`, **reuso — misma routing key que Ola 1**) | Event | Donaciones | **Incentivos** (propuesto) | Otorgar puntos/medalla por donación exitosa | `IncentivosFeignClient.procesarDonacionExitosa` | **Ninguno** — solo cola/binding nuevo del lado de Incentivos | Ola 2 — **ABIERTO**, ver plan §Ola 2 |
| `DonanteRegistradoV1` (`donante.registrado.v1`, **reuso — misma routing key que Ola 1**) | Event | Donaciones | **Incentivos** (propuesto) | Alta de donante en gamificación | `IncentivosFeignClient.registrarDonante` | **Ninguno** — solo cola/binding nuevo del lado de Incentivos | Ola 2 — **ABIERTO** |
| `DonanteDadoDeBajaV1` (`donante.dado-de-baja.v1`) | Event | Donaciones | Incentivos | Baja de donante en gamificación | `IncentivosFeignClient.darDeBaja` | Nuevo — Slice C | Ola 2 — **confirmado el 18/9**, payload `donanteId+personaId+fecha`, ver `catalogo-mensajes.md` |
| `DonacionCargadaV1` (`donacion.cargada.v1`, propuesta — routing key nueva) | Event | Donaciones | Incentivos | Registrar intento de donación (previo a éxito) | `IncentivosFeignClient.procesarDonacion` | Nuevo, si se confirma | Ola 2 — **ABIERTO** |
| `EntregaSolicitadaV1` (`entrega.solicitada.v1`, propuesta — routing key nueva) | **Command** | Donaciones | Logística | Crear entrega pendiente de transporte | `LogisticaFeignClient.registrarEntregaPendiente` | Nuevo, si se confirma | Ola 3 — **ABIERTO** |

**Nota sobre Ola 2 (Incentivos):** a diferencia de `DonanteDadoDeBajaV1` y `DonacionCargadaV1` (routing keys genuinamente nuevas), `DonacionRecibidaV1` y `DonanteRegistradoV1` **ya existen desde Ola 1** — se propone que Incentivos los reutilice en vez de que Donaciones invente comandos nuevos tipo `OtorgarPuntosCommand`. Para esos dos, todo el trabajo (cola + binding + `@RabbitListener` + reacción de negocio) es de Incentivos; Donaciones no cambia nada. Es una propuesta de Donaciones, no una decisión cerrada: requiere acuerdo con el subgrupo de Incentivos en Fase 4 de esa ola (regla del plan, §9: "el consumidor declara qué necesita, el productor declara qué puede garantizar").

**Nota sobre Ola 3 (Logística):** `EntregaSolicitadaV1` se modela como **Command**, no como Event — Donaciones no informa un hecho ("una entrega ocurrió"), le pide a Logística que haga algo ("creá esta entrega"). Ver distinción Command/Event del plan, §10.
