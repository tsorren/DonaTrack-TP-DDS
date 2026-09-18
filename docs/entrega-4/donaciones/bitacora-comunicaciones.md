# Bitácora — Comunicaciones RabbitMQ de Donaciones (Entrega 4)

> Resumen de contexto para retomar este trabajo en otra sesión. Para continuar: pedile a Claude que lea este archivo completo antes de seguir.
>
> Última actualización: 2026-09-18.

## Jerarquía de fuentes de verdad usada en esta conversación

1. **Reunión de equipo del 15/9** (transcripta en notas de Gemini) — verdad absoluta. Todo lo que ahí se omite se trata como duda, no se asume.
2. **Conversación de Discord del 16/9** entre Incentivos (ber, mirinda) y Tadeo — más reciente que la reunión, corrige varias cosas que ahí se habían asumido.
3. **Rama remota `origin/E4_n_bd`** (sin mergear a `ENTREGA_4` todavía) — fuente de patrones de contrato ya implementados (schemas, `RabbitMQConfig`, records Java) para Notificaciones/Incentivos/Donaciones. Válida como patrón técnico a respetar, pero su contenido específico sobre `donacion.asignada` (con categorías/cantidades para Incentivos) quedó superado por el Discord del 16/9.
4. Los docs de `docs/entrega-4/` (Fases 0-5, ya escritos antes de esta conversación) — pueden estar desactualizados donde contradicen 1-3; ya se encontraron contradicciones reales (ver más abajo).

## Decisiones cerradas en esta conversación

- **Topología de exchange:** un `TopicExchange` por servicio productor (`donaciones.exchange`, `incentivos.exchange`). Confirmado en la reunión del 15/9, aunque `principios.md` todavía dice "no cerrada" — hay que actualizar ese doc en algún momento (contradicción real detectada, no resuelta en el archivo todavía).
- **`eventId`:** viaja como propiedad AMQP `messageId`, nunca duplicado en el payload. Confirmado en la reunión.
- **Orden de implementación:** el equipo decidió **comunicaciones primero, persistencia real (JPA/Postgres) después** — invierte el orden que proponían los docs de Fase 3/Etapa 1. Implica seguir usando el `OutboxStore`/`OutboxEntry` en memoria que ya existe (no la tabla `outbox_events` real todavía). Riesgo conocido y anotado: si el proceso se reinicia con un evento pendiente, se pierde (no sobrevive un restart) — pendiente de trackear como ítem explícito en algún lado (ej. `DEUDA_TECNICA.md`), no dejarlo como "después lo vemos" verbal.
- **Convención de IDs:** nunca un campo combinado ambiguo (`personaDonanteId`, que es literalmente el origen del quilombo que reportó Incentivos). Siempre `donanteId` y, solo donde el consumidor lo necesita, `personaId` por separado.
- **Lista final: Donaciones publica 9 eventos** (bajó de una lista inicial de 11):
  1. `donante.registrado.v1` → Notificaciones, Incentivos
  2. `donante.dado-de-baja.v1` → Incentivos
  3. `donacion.asignada.v1` → Notificaciones, Logística
  4. `donacion.recibida.v1` → Notificaciones, Incentivos (reuso)
  5. `donacion.en-camino.v1` → Notificaciones
  6. `donacion.vencida.v1` → Notificaciones
  7. `donacion.entrega-fallida.v1` → Notificaciones
  8. `persona.sincronizada.v1` → Notificaciones, Incentivos
  9. `donacion.segmentada.v1` → Incentivos (nuevo)
- **Se descartaron 2 eventos que se habían propuesto:**
  - `donacion.entregada` — se fusionó con `donacion.recibida.v1` (mismo hecho de negocio, Incentivos reusa el mismo mensaje).
  - `donante.modificado` — innecesario, `persona.sincronizada.v1` ya lleva `denominacion` (el nombre); Incentivos filtra por su propio mapeo `personaId↔donanteId`.
- **`donacion.segmentada.v1`:** un solo evento consolidado por `Donacion` completa (no uno por cada `DonacionIndependiente`, que es la granularidad actual del código). Payload: `donanteId`, `fecha`, `items: [{categoria, cantidad}]` — lista de pares, no dos listas paralelas.
- **Logística ya no recibe un Command `EntregaSolicitadaV1`** (diseño viejo de `plan-implementacion-rabbitmq.md`) — se suscribe directo a `donacion.asignada.v1`, igual que Notificaciones. Confirmado en la reunión del 15/9.
- **Listener:** Donaciones no necesita ningún listener nuevo. Ya tiene `LogisticaEventListener` (existente, sobre `logistica.exchange`, routing keys `ruta.asignada`/`ruta.iniciada`/`entrega.exitosa`/`entrega.fallida`) — alimenta 3 de los 9 publishers (en-camino, recibida, entrega-fallida). Pendiente: agregarle DLX (mandato de la reunión para todos los módulos).

## Los 9 call-sites en el código real (ya identificados)

| Evento | Archivo:método actual |
|---|---|
| `donante.registrado` | `DonantesService.crearDonante()` |
| `donante.dado-de-baja` | `DonantesService.eliminarDonante()` |
| `donacion.asignada` | `PropuestaDeAsignacionService.onPropuestaAprobada()` → `notificarLogistica()` |
| `donacion.recibida` | `DonacionesIndependientesNotificacionesService.procesarDonacionRecibida()` |
| `donacion.en-camino` | `...procesarRutaIniciada()` |
| `donacion.vencida` | `...procesarDonacionVencida()` |
| `donacion.entrega-fallida` | `...procesarDonacionFallida()` |
| `persona.sincronizada` | `PersonasService.crearPersona()` / `actualizarPersona()` / `eliminarPersona()` (3 call-sites) |
| `donacion.segmentada` | `SegmentacionService.registrarEnIncentivos()` — hay que mover el publish de "dentro del for" a "una vez, después del for" (junto a `marcarSegmentadaYPublicar()`) |

## Estado del repo / git

- Rama de trabajo actual: **`E4_donaciones_comunicaciones`** (creada desde `ENTREGA_4`, no desde `E4_n_bd` — para no arrastrar cambios de los otros 3 servicios).
- Traídos desde `origin/E4_n_bd` (solo archivos puntuales, con `git checkout origin/E4_n_bd -- <paths>`), en staging sin commitear: `RabbitMQConfig.java`, `EventoDonacionAsignadaV1.java`, `DestinoEventoDTO.java`, y 7 JSON Schemas (`evento-donacion-asignada-v1`, `evento-donante-registrado-v1`, `evento-donacion-recibida-v1`, `evento-donacion-en-camino-v1`, `evento-donacion-entrega-fallida-v1`, `evento-donacion-vencida-v1`, `evento-persona-sincronizada-v1`). Todos necesitan la corrección de IDs (`personaDonanteId` → `donanteId`+`personaId`) y `donacion.asignada` necesita que le saquen `categorias`/`cantidades`.
- Había un cambio local sin commitear en `docs/entrega-4/arquitectura/principios.md` (la nota "Pendiente de decisión — Topología de exchange") — se guardó en `git stash` (mensaje: "local: nota topologia-exchange pendiente en principios.md..."), recuperable con `git stash pop` si hiciera falta.
- `docs/entrega-4/integracion/catalogo-mensajes.md` ya fue **reescrito completo** con las 9 fichas finales (Fase 4 lista).

## Pendiente / próximos pasos

### ✅ Hecho (19/9 — esta rama, sin commitear todavía)

- Los 9 JSON Schemas de `docs/arquitectura/contratos/schemas/`: 6 corregidos (split `personaDonanteId` → `donanteId`+`personaId`, `donacion.asignada` sin categorías/cantidades, `donante.registrado` con `nombre`), `persona.sincronizada` sin cambios (ya estaba bien), `donacion.segmentada` nuevo desde cero.
- Los 9 records Java en `donaciones-service/.../dto/comunicaciones/` (+ 3 tipos auxiliares: `DestinoEventoDTO`, `MedioDeContactoEventoDTO`, `ItemSegmentadoEventoDTO`).
- `RabbitMQConfig.java` con las 8 routing keys ya traídas de `E4_n_bd` + la de `donacion.segmentada.v1` agregada.
- `docs/entrega-4/integracion/catalogo-mensajes.md` reescrito con las 9 fichas finales.
- Apuntes de estudio completos en `~/2026/dsi/apuntes_comunicaciones_implementacion.md`.

### Pendiente

1. **Commitear** lo de arriba (no se hizo todavía — sigue todo en el working tree de `E4_donaciones_comunicaciones`).
2. Confirmar con Incentivos, Notificaciones y Logística los 9 contratos antes de darlos por definitivos (por si piden más ajustes).
   - **Pregunta abierta planteada al grupo (18/9):** ¿agregar un campo `anonimizado: Boolean` (opcional, default `false`) a `PersonaSincronizadaV1`, para que un consumidor pueda reaccionar distinto cuando el evento viene de `PersonasService.eliminarPersona()` (que en realidad anonimiza, no borra) en vez de un alta/edición? Hoy ningún consumidor pidió esto — se decide con el resto del equipo, no unilateralmente.
3. Escribir el publisher genérico (`IDonacionesEventPublisher`/`DonacionesEventPublisher`) reusando `RabbitTemplate` + el `OutboxStore` en memoria ya existente (no la tabla real todavía — decisión de equipo: comunicaciones primero, persistencia después).
4. Cablear los 9 call-sites (reemplazar las llamadas Feign actuales) — ver la tabla de call-sites más arriba en este documento.
5. Retirar `NotificacionesFeignClient`/`IncentivosFeignClient` cuando todo esté migrado (conservando los endpoints de los controllers, decisión ya tomada en la reunión).
6. Agregar DLX al `LogisticaEventListener` existente.
7. **Mergear `E4_n_bd` a `ENTREGA_4`** (tarea de Notificaciones, según Tadeo) — recién ahí se puede abrir el PR real de esta rama contra `ENTREGA_4`, porque hoy el diff incluiría contenido que en teoría debería venir de esa rama.
8. Más adelante (no ahora): persistencia real (JPA/Postgres) de los 7 agregados que ahora hacen falta (los 6 originales + `Donacion`, por `donacion.segmentada`), y swap del Outbox en memoria al real.
