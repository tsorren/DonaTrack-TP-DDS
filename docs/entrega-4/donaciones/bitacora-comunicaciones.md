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

## Comparación con `origin/ENTREGA_4` (18/9, post-commit `3623a761`)

Mientras se hacía este commit local, en paralelo llegaron 2 commits de `tsorren` a `ENTREGA_4` (`b6bbae82`, `caf1f80c`, "fix(contratos): alinear contratos AMQP desacoplados de incentivos...") que tocan los mismos 6 schemas/records con decisiones **contrarias** a las cerradas acá:

- `personaDonanteId` único (en `ENTREGA_4`) vs `donanteId`+`personaId` separados (local, decisión de la reunión del 15/9 — ver arriba). **Choque real, no un simple desactualizado.**
- `@JsonAlias({"pesoTotal","pesoTotalKG"})`/`@JsonAlias({"volumenTotal","volumenTotalM3"})` reintroducido en `EventoDonacionAsignadaV1` (`ENTREGA_4`) — acá se había sacado a propósito por YAGNI.
- `urlMapa` en `donacion.en-camino` perdió `format: uri` en `ENTREGA_4`.
- `donante.registrado` en `ENTREGA_4` perdió el campo `nombre` (y no tiene todavía el record Java `EventoDonanteRegistradoV1`, solo el schema).
- Naming: `EventoDonacionSegmentadaDTO` (`ENTREGA_4`) vs `EventoDonacionSegmentadaV1` (local, consistente con la convención sin sufijo `DTO` para el payload real de AMQP).
- `ENTREGA_4` todavía no tiene los records Java de `en-camino`, `entrega-fallida`, `recibida`, `vencida`, `persona-sincronizada` (solo los schemas, algunos ya con `personaDonanteId`) — local va más adelantado ahí.
- `catalogo-mensajes.md` y esta bitácora son 100% nuevos, no existen en `ENTREGA_4` todavía — sin conflicto.

**Decisión (18/9):** dejar todo como está en local (`donanteId`+`personaId` separados, sin `@JsonAlias`, `format: uri` en `urlMapa`, `nombre` en `donante.registrado`, naming `...V1` sin `DTO`). **Pendiente comunicarle a tsorren/equipo de Incentivos** que `donaciones-service` ya había resuelto esto distinto en la reunión del 15/9, antes de que el PR de esta rama choque contra `ENTREGA_4`.

## Pendiente / próximos pasos

### ✅ Hecho (19/9 — esta rama, sin commitear todavía)

- Los 9 JSON Schemas de `docs/arquitectura/contratos/schemas/`: 6 corregidos (split `personaDonanteId` → `donanteId`+`personaId`, `donacion.asignada` sin categorías/cantidades, `donante.registrado` con `nombre`), `persona.sincronizada` sin cambios (ya estaba bien), `donacion.segmentada` nuevo desde cero.
- Los 9 records Java en `donaciones-service/.../dto/comunicaciones/` (+ 3 tipos auxiliares: `DestinoEventoDTO`, `MedioDeContactoEventoDTO`, `ItemSegmentadoEventoDTO`).
- `RabbitMQConfig.java` con las 8 routing keys ya traídas de `E4_n_bd` + la de `donacion.segmentada.v1` agregada.
- `docs/entrega-4/integracion/catalogo-mensajes.md` reescrito con las 9 fichas finales.
- Apuntes de estudio completos en `~/2026/dsi/apuntes_comunicaciones_implementacion.md`.

### ✅ Hecho (18/9, tarde — esta rama, sin commitear todavía)

- **Choque de contrato con `ENTREGA_4` resuelto:** se mergeó `ENTREGA_4` a esta rama (commit `7c75c456`) y en la resolución de conflictos se mantuvo la decisión propia (`donanteId`+`personaId` separados, sin `@JsonAlias`, `format: uri` en `urlMapa`, `nombre` en `donante.registrado`, naming `...V1`). Ya se avisó a tsorren/equipo de Incentivos y se subió a una PR — punto cerrado, ya no es pendiente.
  - Queda un resabio del merge sin resolver: **`EventoDonacionSegmentadaDTO.java` y `EventoDonacionSegmentadaV1.java` coexisten** en `dto/comunicaciones/` — el primero es el diseño viejo (por `DonacionIndependiente`, `categorias`/`cantidad` separados, `personaDonanteId`) traído por el merge desde el trabajo de Incentivos, el segundo es el nuestro (por `Donacion` completa, `items` consolidado, `donanteId`). Hay que decidir si se borra el `DTO` viejo o si Incentivos lo sigue necesitando por otro motivo — no se tocó todavía.
- **`EventoDonanteRegistradoV1.java` creado** (`dto/comunicaciones/`) — faltaba desde el commit "9 schemas + 9 records" (en realidad eran 8 records + el schema de este evento sin su record). Seguía el patrón Bean Validation ya usado en los otros 7 (`@NotNull`/`@NotBlank`/`@PastOrPresent`/`@JsonFormat`), con `donanteId`+`personaId` separados y `nombre` incluido (igual que el schema).
- **Publisher genérico escrito**, con interfaz (decisión y justificación abajo):
  - `services/IDonacionesEventPublisher.java` — 8 métodos (uno por evento con contrato ya definido; `donante.dado-de-baja` queda afuera, ver pendiente más abajo).
  - `infrastructure/events/DonacionesEventPublisher.java` — implementación con `RabbitTemplate`, un método público por evento + un helper privado `publicar(routingKey, evento)` que hace `convertAndSend` con un `MessagePostProcessor` que setea `messageId` (propiedad AMQP estándar, decisión de idempotencia ya cerrada) y el header `X-Trace-Id` (reusando `FeignTraceRequestInterceptor.MDC_TRACE_KEY`/`TRACE_HEADER` de `common-lib`, mismo mecanismo que ya usan las llamadas Feign).
  - **Decisión de diseño (interfaz sí):** se investigó el precedente real del repo — `ComunicadorEventosLogistica` (interfaz, en `services`) + `ComunicadorEventosLogisticaRabbit` (impl, en `infrastructure`) en `logistica-service`, versus `LogisticaEventPublisher` (concreta, sin interfaz). Conclusión: la interfaz va donde una capa de aplicación (los 9 call-sites) depende directamente de infraestructura — ese es el lugar de `ComunicadorEventosLogistica`, no el de `LogisticaEventPublisher` (que es un colaborador interno, un nivel más abajo, sin otro consumidor). Como nuestros 9 call-sites van a inyectar el publisher directamente (reemplazando a los Feign clients, que también son interfaces), corresponde interfaz. Respaldado además por `docs/arquitectura/principios-diseno-arquitectura.md` (sección DIP), que nombra explícitamente `NotificacionesFeignClient`/`ComunicadorEventosLogistica` como el patrón ya adoptado. Detalle completo en la sección 14 de los apuntes.
  - **No compilado todavía** (no hay `mvn`/`mvnw` disponible en el entorno de esta sesión) — pendiente de compilar/revisar en IntelliJ. **Revisado y aprobado por el usuario en IntelliJ el 18/9.**

### ✅ Hecho (18/9, noche — cierre de `donante.dado-de-baja.v1`)

- **`donante.dado-de-baja.v1` cerrado como 9º y último evento**, con payload `donanteId`+`personaId`+`fecha` (se agregó `personaId`+`fecha`, la ficha original del catálogo solo tenía `donanteId`). Se aclaró explícitamente por qué esto es un hecho distinto de `persona.sincronizada` y no se fusiona con él: `DonantesService.eliminarDonante` borra el agregado `Donante` (el rol) y nunca pasa por `PersonasService` — la `Persona` sigue existiendo, `persona.sincronizada` no se dispara en ese flujo. Esto también respondió una pregunta del usuario sobre si `donante.dado-de-baja` había quedado absorbido por la propuesta descartada `donante.modificado` — no, son eventos distintos con triggers distintos; `donante.modificado` sí se descartó (lo reemplaza `persona.sincronizada`), `donante.dado-de-baja` solo estaba "ABIERTO" sin cerrar.
  - Nuevo: `docs/arquitectura/contratos/schemas/evento-donante-dado-de-baja-v1.schema.json`
  - Nuevo: `dto/comunicaciones/EventoDonanteDadoDeBajaV1.java`
  - `RabbitMQConfig.java`: agregada `ROUTING_KEY_DONANTE_DADO_DE_BAJA`
  - `IDonacionesEventPublisher.java`/`DonacionesEventPublisher.java`: agregado el 9º método, `publicarDonanteDadoDeBaja` — el publisher ahora cubre los 9 eventos completos
  - `catalogo-mensajes.md` y `matriz-productor-consumidor.md`: actualizado el payload y cerrado el estado "ABIERTO" de esa fila
  - **Nota:** `matriz-productor-consumidor.md` tiene más contenido desactualizado (Command `EntregaSolicitadaV1`, nombres viejos `PersonaReplicaV1`/`EntregaFallidaV1`) que quedó superado por `catalogo-mensajes.md` — no se tocó, es una limpieza aparte, no se metió en este cambio.

### Pendiente

1. **Cablear los 9 call-sites** (reemplazar las llamadas Feign actuales por `IDonacionesEventPublisher`, ya completo con los 9 métodos) — ver la tabla de call-sites más arriba en este documento.
2. Resolver el duplicado `EventoDonacionSegmentadaDTO` vs `EventoDonacionSegmentadaV1` (ver arriba) — se puede borrar el `DTO` ya, confirmado que no lo usa nadie en `donaciones-service` (ni código ni tests).
3. Retirar `NotificacionesFeignClient`/`IncentivosFeignClient` cuando todo esté migrado (conservando los endpoints de los controllers, decisión ya tomada en la reunión).
4. Limpieza aparte (no bloqueante): refrescar `matriz-productor-consumidor.md` completo, tiene contenido pre-reunión del 15/9 que ya no coincide con `catalogo-mensajes.md`.
5. Agregar DLX al `LogisticaEventListener` existente.
6. Más adelante (no ahora): persistencia real (JPA/Postgres) de los 7 agregados que ahora hacen falta (los 6 originales + `Donacion`, por `donacion.segmentada`), y swap del Outbox en memoria al real.
