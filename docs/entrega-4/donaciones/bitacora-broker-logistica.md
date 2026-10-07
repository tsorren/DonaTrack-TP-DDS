# Bitácora — Broker de Integración con Logística (Entrega 4)

> Registro vivo de la implementación del broker: decisiones, preguntas, aclaraciones y discusiones, en orden. Para retomar el trabajo, leer primero el **TLDR**, después el **Estado por etapa** y la última entrada del **Registro**.
>
> **Documentos relacionados:** [Plan](plan-broker-logistica.md) · [SPEC-04](../../specs/active/SPEC-04-broker-integracion-logistica.md) · [ADR 20261007](../../adr/20261007-broker-de-integracion-con-logistica.md) · [Bitácora de comunicaciones (antecedente)](bitacora-comunicaciones.md)
>
> **Rama:** `E4_donaciones_broker`

---

## TLDR

> Última actualización: **2026-10-07** — Etapa 0 cerrada y SPEC-04 aprobada.

- **Qué estamos haciendo:** un broker dentro de `donaciones-service` que elige a qué proveedor de logística mandarle cada entrega: el nuestro por RabbitMQ, u otro por HTTP. Así se cumple el requerimiento de la Entrega 4.
- **Cómo:** Broker + Adapter + Strategy. El pedido viaja como comando `entrega.solicitada.<proveedorId>.v1`; logística deja de escuchar el hecho `donacion.asignada.v1`. Ningún pedido se pierde en silencio (`mandatory` + espera de acuse) y no se reenvía a otro proveedor si no hay certeza de que el primero no lo recibió.
- **Dónde estamos:** **Etapa 0 cerrada.** Baseline en verde. ADR `proposed`, SPEC-04 **aprobada** (`APPROVED_BY_USER`) y esta bitácora creada.
- **Bloqueante para seguir:** ninguno.
- **Próximo paso:** Etapa 1, el núcleo del broker sin cablear. Antes de codear hay que acordar qué estrategias adicionales de selección se implementan.

---

## Estado por etapa

| Etapa | Contenido | Estado |
|---|---|---|
| 0 — Preparación | Rama, baseline, spec, ADR, bitácora | ✅ Cerrada |
| 1 — Núcleo sin cableado | Broker, estrategia, outbox, registro, excepciones | ⏳ Pendiente |
| 2 — Contrato, adapter AMQP y cutover | `EntregaSolicitadaV1`, `mandatory` + acuse, cambios en Logística | ⏳ Pendiente |
| 3 — Cableado | `PropuestaDeAsignacionService` → broker | ⏳ Pendiente |
| 4 — Adapter HTTP + dedup REST | `ProveedorLogisticaHttp`, 409 en `EntregasService.crear` | ⏳ Pendiente |
| 5 — Vuelta HTTP + protección | Callback, `ProcesadorEventosLogistica`, `ApiKeyFilter` | ⏳ Pendiente |
| 6 — Controller admin (recortable) | Cambiar el proveedor preferido en caliente | ⏳ Pendiente |
| 7 — Demo | Segunda instancia, Postman, guion | ⏳ Pendiente |
| 8 — Cierre | Catálogo, matriz, diagrama, deuda, índices, gates | ⏳ Pendiente |

---

## Decisiones

Registro acumulado. Las decisiones D1 a D12 vienen del plan; las que se tomen durante la implementación se agregan acá con su fecha.

| # | Decisión | Fecha | Origen |
|---|---|---|---|
| D1 | Broker in-process en Donaciones (Broker + Adapter + Strategy). | 2026-10-07 | Plan |
| D2 | Logística consume `entrega.solicitada.<id>.v1` en lugar de `donacion.asignada.v1`. Revierte la decisión del 15/9. | 2026-10-07 | Plan · **acordado por el grupo** (confirmado en la Etapa 0) |
| D3 | Outbox en memoria, detrás de un puerto, con entradas basadas en datos. | 2026-10-07 | Plan |
| D4 | Dos transportes: AMQP y HTTP. | 2026-10-07 | Plan |
| D5 | Estrategia base «preferencia + reenvío»; las adicionales se acuerdan con el equipo. | 2026-10-07 | Plan |
| D6 | API key en los endpoints expuestos (sin Spring Security). | 2026-10-07 | Plan |
| D7 | Comando dirigido por routing key, no por un exchange por proveedor. | 2026-10-07 | Plan |
| D8 | `mandatory=true` + reenvío ante devolución. | 2026-10-07 | Plan |
| D9 | Segunda instancia de Logística para la demo. | 2026-10-07 | Plan |
| D10 | Rechazado → siguiente proveedor; incierto → mismo proveedor. | 2026-10-07 | Plan |
| D11 | El relay espera el acuse con `CorrelationData`. | 2026-10-07 | Plan |
| D12 | `POST /api/entregas` deduplica por donación (409). | 2026-10-07 | Plan |
| D13 | La regla «los eventos se rutean por hecho; los comandos, por destinatario» se formaliza **dentro del ADR del broker**, no en un ADR propio. La matriz productor-consumidor va a remitir a ese ADR (Etapa 8). | 2026-10-07 | Etapa 0 |

---

## Preguntas abiertas

| # | Pregunta | Para cuándo | Estado |
|---|---|---|---|
| P1 | ¿Se aprueba SPEC-04 tal como está? | Antes de la Etapa 1 | ✅ Cerrada: aprobada sin cambios (2026-10-07) |
| P2 | ¿Qué estrategias adicionales de selección se implementan, y con qué criterio exacto? (Candidatas: RoundRobin, PorCarga, PorZona.) | Etapa 1 | Abierta |
| P3 | ¿Qué código de `ErrorCatalog` usa el 409 de Logística? | Etapa 4 | Abierta |
| P4 | ¿Qué valores de `acuse-timeout-ms` y del read timeout HTTP usamos? (Propuesta: 5000 y 3000 ms.) | Etapas 2 y 4 | Abierta |

---

## Registro

### 2026-10-07 — Antecedentes: cómo se llegó a este plan

Discusión previa a la implementación, resumida para entender por qué el plan es como es.

1. **«Broker» no es RabbitMQ.** Al principio se interpretó «broker» como *message broker*. El enunciado de la Entrega 4 pide dos cosas distintas: (a) una **cola de mensajes** para notificaciones y (b) un **broker de integración** con logística que **elija** entre más de un proveedor. Este trabajo es el (b).
2. **Primer diseño (obsoleto):** un broker que envolvía `LogisticaFeignClient`. Quedó obsoleto cuando el equipo eliminó Feign y migró todo a RabbitMQ (#883): logística pasó a escuchar `donacion.asignada.v1` directo.
3. **El problema del pub/sub:** un exchange copia el mensaje a todas las colas que coinciden, no elige. Con dos proveedores escuchando el mismo hecho, los dos crean la entrega. Por eso la selección tiene que hacerse en Donaciones, y logística tiene que recibir un **pedido dirigido** (comando) en lugar del hecho.
4. **¿Cómo dirigir el comando?** Se compararon tres opciones: routing key por proveedor, un exchange por proveedor y un adapter por transporte. Se eligió **routing key por proveedor** para los proveedores AMQP (es más simple que un exchange por proveedor y deja clara la propiedad), combinada con **un adapter por transporte** (un proveedor externo puede hablar HTTP).
5. **Mensajes perdidos en silencio:** un TopicExchange descarta lo que no matchea con ningún binding. Se evaluaron `mandatory` + returns, un alternate exchange y que Donaciones declare las colas. Se eligió `mandatory` con **espera del acuse**, porque el alternate exchange impide que el broker se entere de la falla.
6. **Plan de Nico vs. plan propio:** se compararon y se unificaron. Del plan de Nico se tomó la base (outbox basado en datos, registro de solicitudes, callback protegido, alias de tipo independiente, cola por instancia, segunda instancia como demo). Se le agregaron tres ajustes:
   - **timeout ≠ rechazo**, para evitar entregas en dos proveedores;
   - **espera del acuse** en lugar de un `ReturnsCallback` asincrónico;
   - **dedup en `POST /api/entregas`**, que `[OBSERVED]` hoy no deduplica.

### 2026-10-07 — Etapa 0: Preparación

**Hecho:**
- Rama `E4_donaciones_broker` al día con `origin` (HEAD `d82d5760`, «plan unificado broker»).
- **Baseline `[VERIFIED]` — BASELINE_GREEN** con `mvn clean test -pl donaciones-service,logistica-service -am -Dspotless.check.skip=true`:

  | Módulo | Tests | Fallos | Errores | Omitidos |
  |---|---|---|---|---|
  | `common-lib` | 60 | 0 | 0 | 0 |
  | `donaciones-service` | 437 | 0 | 0 | 0 |
  | `logistica-service` | 355 | 0 | 0 | 1 |

  Los stack traces en el log (`Error simulado`, `MethodArgumentNotValidException`) son de tests que simulan fallos a propósito, no fallas.
- ADR [`20261007-broker-de-integracion-con-logistica.md`](../../adr/20261007-broker-de-integracion-con-logistica.md) creado como `proposed`.
- Spec [`SPEC-04`](../../specs/active/SPEC-04-broker-integracion-logistica.md) creada como `PROPOSED` y **aprobada sin cambios** por el usuario → `APPROVED_BY_USER`.
- Índices actualizados: `docs/specs/README.md` y `docs/ESTADO_DOCUMENTACION.md`.

**Decisiones:**
- El grupo **ya acordó** revertir la decisión del 15/9 (D2).
- La regla de ruteo eventos/comandos va **dentro del ADR del broker** (D13).

**Aclaraciones:**
- El ADR queda `proposed` aunque el grupo esté de acuerdo, porque según AGENTS.md §9.3 ningún agente puede promoverlo a `accepted`. Lo tiene que cambiar una persona del equipo.
- **Para qué sirve cada documento** (para no confundirlos):

  | Documento | Contesta | Característica |
  |---|---|---|
  | ADR | **Por qué** este diseño y no otro | Permanente: no se edita, se reemplaza con otro ADR |
  | SPEC-04 | **Qué** entra, qué no entra, qué reglas no se pueden romper y cómo se verifica | Obligatoria para tareas grandes (`docs/specs/README.md`); se aprueba antes de codear |
  | Plan | **Cómo** se hace, paso a paso | Se puede ajustar durante la implementación |
  | Bitácora | **Qué pasó** y qué falta | Se actualiza en cada paso |

  La spec es corta a propósito: remite al plan para el detalle. Lo que aporta es el **límite de alcance** (para evitar agregar cosas de más) y los **invariantes**, que son las reglas que cada test tiene que poder señalar que protege.

---

## Q&A

**¿Qué es el «broker» que pide la Entrega 4? ¿Es RabbitMQ?**
No. RabbitMQ es un *message broker* (transporta mensajes). El broker de integración es un componente de software (patrón Broker) que funciona como intermediario: Donaciones le pide «hacé esta entrega» y el broker decide **qué proveedor de logística** la recibe y cómo hablarle. RabbitMQ es uno de los transportes que usa.

**¿Por qué el broker está dentro de `donaciones-service` y no es un microservicio aparte?**
Porque quien decide a quién mandar el pedido es quien lo origina, y porque la Entrega 3 prohíbe que logística invoque a donaciones. Un microservicio aparte agregaría un contenedor, un punto único de falla y un salto de red más, sin ninguna ventaja funcional.

**¿Qué patrones de diseño usa?**
- **Broker:** el intermediario que conoce a los proveedores.
- **Strategy:** el criterio de selección es intercambiable. Es el mismo patrón que ya usamos en los algoritmos de asignación.
- **Adapter:** uno por proveedor, cada uno con su transporte.
- **Canonical Data Model:** un formato interno único (`SolicitudEntregaLogistica`).
- **Message Translator:** el adapter HTTP traduce al formato del proveedor.
- **Outbox:** los pedidos pendientes no se pierden.

**¿Por qué crearon un mensaje nuevo (`entrega.solicitada`) en lugar de reusar `donacion.asignada`?**
Porque son cosas distintas. `donacion.asignada` es un **hecho** («se asignó una donación») que le interesa a varios: Notificaciones, y antes también Logística. `entrega.solicitada` es un **pedido** dirigido a un solo proveedor elegido. Si el pedido viajara en el hecho, todos los que escuchan el hecho lo recibirían y no habría forma de elegir.

**Si un exchange de RabbitMQ reparte mensajes, ¿por qué no elige él al proveedor?**
Porque un TopicExchange **copia** el mensaje a todas las colas cuyo binding coincide; no elige una. Si dos proveedores escuchan la misma dirección, los dos reciben el pedido. La elección la tiene que hacer alguien con criterio de negocio: el broker, antes de publicar.

**¿Poner el nombre del proveedor en la routing key no contradice la regla de que la routing key nunca dice quién escucha?**
Esa regla es para **eventos**, que tienen N interesados desconocidos. Un **comando** va por naturaleza a un destinatario concreto, así que identificarlo es correcto. Formalizamos la regla completa: los eventos se rutean por hecho; los comandos, por destinatario.

**¿Por qué una routing key por proveedor y no un exchange por proveedor?**
Las dos logran que el pedido llegue solo al elegido. La routing key lo hace con la misma infraestructura (un solo exchange, el de Donaciones). El exchange por proveedor suma infraestructura por cada proveedor y deja ambiguo quién es el dueño de cada exchange.

**¿Qué pasa si el broker elige un proveedor que no tiene cola en RabbitMQ?**
Sin protección, RabbitMQ descartaría el mensaje en silencio. Por eso publicamos con `mandatory=true`, que hace que RabbitMQ lo devuelva, y el broker **espera el acuse** de cada envío. Si vuelve devuelto, el broker sabe con certeza que no llegó y lo manda al siguiente proveedor.

**¿Por qué ante un timeout no lo mandan a otro proveedor?**
Porque un timeout no significa que el pedido no llegó; significa que no sabemos. El primer proveedor pudo haber creado la entrega. Si lo mandáramos a otro, la donación podría terminar con dos entregas. Ante la duda reintentamos con el mismo proveedor, que deduplica por donación.

**¿Cómo evitan entregas duplicadas?**
De tres formas:
1. Solo se cambia de proveedor ante un rechazo seguro.
2. Cada proveedor deduplica por `donacionIndependienteId`: el listener de logística ya lo hacía, y agregamos el chequeo al endpoint REST, que responde 409.
3. Los bindings de los comandos son exactos, nunca con comodines.

**¿Por qué no usar un alternate exchange para no perder mensajes?**
Porque con un alternate exchange RabbitMQ considera que el mensaje sí se ruteó, así que no lo devuelve y el broker no se entera de que el proveedor no tenía cola. Rescata el mensaje, pero impide el reenvío automático.
