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

### ✅ Hecho (18/9, noche — cableados los 9 call-sites)

- **Los 9 call-sites migrados en los 5 archivos**, reemplazando cada llamada Feign por el método correspondiente de `IDonacionesEventPublisher`:
  - `DonantesService.java` (filas `donante.registrado`, `donante.dado-de-baja`) — hecho primero a mano en IntelliJ siguiendo la explicación de Claude; tuvo 3 errores de compilación en el primer intento los cuales fueron corregidos y confirmados.
  - `NotificacionesAsyncService.java` (fila `persona.sincronizada`, cubre los 3 call-sites de `PersonasService.java` sin tocar ese archivo — todos pasan por este único wrapper) — mapea `PersonaReplicaDTO`/`MedioDeContactoReplicaDTO` a `EventoPersonaSincronizadaV1`/`MedioDeContactoEventoDTO`.
  - `DonacionesIndependientesNotificacionesService.java` (filas `donacion.en-camino`, `donacion.recibida`, `donacion.vencida`, `donacion.entrega-fallida`) — se mantuvo el wrapper try/catch + `OutboxStore` para reintentos (decisión ya cerrada de seguir usando el Outbox en memoria), solo cambiando qué se llama adentro. `procesarDonacionRecibida` fusionó las dos llamadas viejas (Incentivos + Notificaciones) en un solo `publicarDonacionRecibida`, consistente con la ficha del catálogo (Incentivos reusa la misma routing key con `donanteId`+`personaBeneficiariaId`, ya no necesita `organizacionId`).
  - `PropuestaDeAsignacionService.java` (fila `donacion.asignada`) — el más grande de los cuatro. Necesitó agregar dos repositorios que la clase no tenía (`IDonacionesRepository`, `IDonantesRepository`, para llegar de la `DonacionIndependiente` al `donanteId`/`personaId` del donante original) y un método nuevo en `DireccionMapper` (`toDestinoEventoDTO`, no existía ninguna conversión hacia ese DTO). El campo nuevo se llamó `donacionesEventPublisher` para no chocar con el `ApplicationEventPublisher eventPublisher` de Spring que ya vivía en la clase. **`logisticaAsyncService` y `construirSolicitudEntrega` se sacaron del archivo por completo** — según el catálogo, Logística deja de recibir una llamada síncrona y pasa a suscribirse directo a `donacion.asignada.v1` (reemplaza el Command `EntregaSolicitadaV1`); de paso, Notificaciones empieza a recibir este evento por primera vez (antes no le llegaba nada de esto).
  - `SegmentacionService.java` (fila `donacion.segmentada`) — el publish salió de adentro del for y pasa a ser uno solo por `Donacion` completa, junto a `marcarSegmentadaYPublicar()`. Como el evento nuevo solo pide `donanteId`+`fecha`+`items` (sin `personaId` ni `nombre`), se cayeron `personasRepository`, `donantesRepository` y el método `obtenerNombrePersona` (quedaban sin ningún uso).
- **Los 5 tests unitarios correspondientes actualizados** (mocks de `IncentivosFeignClient`/`NotificacionesFeignClient` reemplazados por `IDonacionesEventPublisher`, `verify()` apuntando a los métodos `publicarX`, tests de reintento por outbox ajustados a que ahora hay una sola llamada fusionada en `procesarDonacionRecibida`).
- **Verificado con `mvn` real** (sí había `mvn` disponible en este entorno, a diferencia del 18/9 tarde): `mvn -pl donaciones-service -am test` → **437 tests, 0 fallos, BUILD SUCCESS** (incluye `ArchitectureFitnessTest`, ninguna regla de ArchUnit rota).
- **Nota:** el chequeo de Spotless (`mvn compile`) no corre en este entorno — `google-java-format` choca con la versión de JDK instalada (`NoSuchMethodError` interno de javac, no es un problema del código). Se verificó todo con `-Dspotless.check.skip=true`; falta correr `mvn spotless:apply` desde IntelliJ antes de commitear, igual que se hizo con el publisher el 18/9.
- **Descubierto de paso:** `LogisticaAsyncService`/`ILogisticaAsyncService`/`LogisticaFeignClient` quedaron sin ningún llamador en todo el repo (confirmado con grep) al sacar `logisticaAsyncService` de `PropuestaDeAsignacionService`. No se borraron — mismo criterio que con los Feign clients de Incentivos/Notificaciones (retirar recién cuando se decida la limpieza global, pendiente #2 de abajo).

### ✅ Hecho (18/9, más tarde — fix de `validate-contracts.js` y de un NPE marcado por SonarCloud)

- **`scripts/validate-contracts.js` arreglado:** las 3 aserciones del bloque `evento-donacion-asignada-v1` (la positiva y las 2 negativas) usaban el campo viejo `personaDonanteId` en el payload de prueba; se cambiaron a `donanteId`+`personaId`, consistente con el schema real ya partido. **137/137 en verde.**
- **NPE real corregido en `NotificacionesAsyncService.sincronizarPersona()`** (`javabugs:S2259` en SonarCloud, PR #883): `PersonaMapper.toReplicaDTO(Persona p)` devuelve `null` explícitamente si `p == null`, y ese valor entra directo como `dto` a `sincronizarPersona` desde los 3 call-sites de `PersonasService`. Se agregó un guard `if (dto == null) { log.warn(...); return; }` al principio del método, antes del `try`, así también cubre el `dto.id()` que estaba en el `catch`. Hoy nunca pasa en la práctica (los call-sites reales nunca pasan null), pero el contrato del mapper lo permite y Sonar lo puede probar.
  - **`dto.mediosDeContacto()` también blindado** (mismo método, mismo motivo: no tiene `@NotNull` en `PersonaReplicaDTO`): se agregó un fallback a `List.of()` si viene null, antes de armar el stream. Hoy `PersonaMapper.toReplicaDTO` siempre arma esa lista (nunca null), pero el contrato del record lo permite.

### ✅ Hecho (19/9 — revisión del cableado, sin implementar todavía)

- **Duplicado `EventoDonacionSegmentadaDTO` (donaciones-service) resuelto** — Sofía ya lo borró (commit `d253adb8`, confirmado sin uso real). Sigue existiendo una clase con el mismo nombre en `incentivos-service` (su propia copia del contrato, por diseño) — esa no se toca.
- **Revisión de diseño del cableado de call-sites**, con 2 hallazgos anotados como `//TODO` directamente en el código (no arreglados todavía a propósito, para discutir antes de tocar código ajeno):
  - `PropuestaDeAsignacionService.construirEventoDonacionAsignada`: `entidad`/`personaBeneficiaria` se re-buscan en cada vuelta del `for` de `onPropuestaAprobada`, aunque dependen solo de `necesidad` (invariante en todo el loop) — se puede sacar esa búsqueda afuera del for. No es un problema de performance hoy (repos en memoria), pero se vuelve N consultas SQL evitables cuando llegue la persistencia real.
  - `NotificacionesAsyncService.sincronizarPersona`: doble mapeo — `Persona → PersonaReplicaDTO` (en `PersonaMapper`, pensado para el `PUT` Feign que ya no se llama) y después `PersonaReplicaDTO → EventoPersonaSincronizadaV1` (mapeo nuevo de Sofía). Confirmado con grep que `PersonaReplicaDTO`/`MedioDeContactoReplicaDTO` no tienen ya ningún otro consumidor real — candidatos a eliminarse junto con `PersonaMapper.toReplicaDTO`/`toMedioReplicaDTO`, mapeando `Persona → EventoPersonaSincronizadaV1` directo en un solo paso.
  - Detalle completo del razonamiento de ambos en la sección 16 de los apuntes.
- **Pregunta de contrato con Incentivos (`donacion.segmentada`) — CERRADA (19/9):** Incentivos había pedido un payload agregado (`categorias: [...]` + `cantidadTotal` único) en vez del detalle actual (`items: [{categoria, cantidad}]`). Se evaluaron 3 opciones (mantener el detalle / dar solo el agregado / mandar ambos). **Decisión final: no se agrega `cantidadTotal`** — Incentivos aceptó calcular la suma ellos mismos a partir de `items`. El contrato de `donacion.segmentada.v1` queda exactamente como está, sin cambios de código. Detalle en sección 12 de los apuntes.

### ✅ Hecho (19/9 — TODO 1 resuelto: lookups redundantes en `PropuestaDeAsignacionService`)

- `entidad`/`personaBeneficiaria` (y el `destino` derivado) se resuelven **una sola vez por `PropuestaAprobada`**, antes del `for` de `onPropuestaAprobada`, en vez de una vez por cada fragmentación. Nuevo método privado `resolverDatosBeneficiario(Necesidad)` que devuelve un record interno `DatosBeneficiario(personaBeneficiariaId, destino)`, con su propio try/catch (si falla, ninguna fragmentación de ese evento publica `donacion.asignada.v1`, mismo criterio de tolerancia a fallos que ya existía, ahora aplicado una vez en vez de N).
- `construirEventoDonacionAsignada` quedó más corto: solo resuelve lo que sigue variando por fragmentación (`donacionOriginal`/`donante`), recibe `DatosBeneficiario` ya armado.
- **Test nuevo** `onPropuestaAprobada_conVariasFragmentaciones_debeResolverEntidadYPersonaUnaSolaVez` (2 fragmentaciones en el mismo evento), verificando `times(1)` en `entidadesBeneficiariasRepository`/`personasRepository`/`direccionMapper` y `times(2)` en el publish. Los 2 tests viejos de este archivo **no necesitaron ningún cambio** (ninguno usaba más de una fragmentación).
- **Verificado con Maven real** (se encontró JDK 21 + Maven bundleados con IntelliJ en la máquina, sin descargar nada — ver referencia nueva en memoria de Claude): `mvn -pl donaciones-service -am test` → **438 tests, 0 fallos, BUILD SUCCESS** (437 de Sofía + el nuevo). También se corrió `mvn spotless:apply` sobre los 2 archivos tocados — formateo aplicado limpio, sin cambios de comportamiento.

### ✅ Hecho (19/9 — TODO 2 resuelto: doble mapeo en `NotificacionesAsyncService`)

- `INotificacionesAsyncService.sincronizarPersona` pasa a recibir `EventoPersonaSincronizadaV1` (antes `PersonaReplicaDTO`) — el mapeo `Persona → Evento` se hace en un solo paso, en el hilo síncrono de `PersonasService`, **antes** de cruzar el límite `@Async` (se decidió así, y no pasando `Persona` directo, porque `CrudRepositoryEnMemoria` no hace copia defensiva — la `Persona` es mutable y compartida por referencia; pasarla directo al método `@Async` hubiera reintroducido una condición de carrera real).
- Nuevo método `PersonaMapper.toEventoPersonaSincronizadaV1(Persona)` — mapeo directo, sin pasar por `PersonaReplicaDTO`.
- `PersonasService` actualiza sus 3 call-sites a la nueva firma.
- A propósito **no se tocó** en este paso: `NotificacionesFeignClient`, `PersonaReplicaDTO`, `MedioDeContactoReplicaDTO`, `PersonaMapper.toReplicaDTO`/`toMedioReplicaDTO` — quedaron con duplicación transitoria hasta el paso 3 (retiro de Feign clients), a pedido explícito del usuario para no mezclar pasos mientras trabaja dividido con Sofía.
- Tests actualizados: `NotificacionesAsyncServiceTest` (+ 1 test nuevo para el guard de null), `PersonasServiceTest`, y `DonacionesServiceApplicationTest` (apareció recién al correr la suite completa — buena evidencia de por qué conviene correr todo, no solo lo que uno cree relacionado). **439 tests, 0 fallos.**

### ✅ Hecho (19/9 — punto 3 resuelto: retiro completo de los Feign clients)

Se verificó primero con `grep` en **todo el repo** (no solo `donaciones-service`) que ningún otro módulo (incluido `integration-tests`) importa estas clases — los tests de integración le pegan por HTTP a los controllers de cada servicio, no a estas clases Java. Sin contraindicación, se borró todo de una:

- **Feign clients**: `NotificacionesFeignClient`, `IncentivosFeignClient`, `LogisticaFeignClient`, `FeignRetryConfig`.
- **Wrapper huérfano**: `LogisticaAsyncService`/`ILogisticaAsyncService` (quedó sin llamador al cablear `donacion.asignada` el 18/9) + su test.
- **DTOs que quedaron sin ningún uso**: `EventoNotificableDTO` (marcadora) + sus 6 implementaciones (`EventoDonacionVencidaDTO`, `EventoRutaIniciadaDTO`, `EventoDonanteRegistradoDTO`, `EventoDonacionRecibidaDTO`, `EventoDonacionAsignadaDTO`, `EventoEntregaFallidaDTO`), `DonanteRegistradoDTO`, `NuevaDonacionRequest`, `DonacionExitosaRequest`, `RegistrarDonanteRequest`, `NuevaEntregaRequest`, y **`PersonaReplicaDTO`/`MedioDeContactoReplicaDTO`** (el resabio que se dejó pendiente del TODO 2 — este era el momento correcto de sacarlos).
- **`PersonaMapper.toReplicaDTO`/`toMedioReplicaDTO`** borrados — se cierra la duplicación transitoria con `toEventoPersonaSincronizadaV1`.
- **Infraestructura Feign de fondo, también retirada** (nada la necesitaba ya): `@EnableFeignClients` en `DonacionesServiceApplication`, dependencia `spring-cloud-starter-openfeign` en `donaciones-service/pom.xml`.
- Tests ajustados (no borrados salvo `LogisticaAsyncServiceTest`): `DonacionesServiceApplicationTest` (sacados los 3 `@MockitoBean` de Feign), `PersonaMapperTest` (reemplazado el test de `toReplicaDTO` por uno de `toEventoPersonaSincronizadaV1`).
- **Verificado con el reactor completo** (los 7 módulos: `donatrack`, `common-lib`, `donaciones-service`, `notificaciones-service`, `incentivos-service`, `logistica-service`, `integration-tests`): `mvn test` → **BUILD SUCCESS** en todos, sin ninguna sorpresa cruzada. `donaciones-service` solo: **437 tests, 0 fallos** (439 - 2 del test borrado).
- **No se tocó** (deliberadamente, es config de despliegue, otra capa): las properties `donatrack.notificaciones.url`/`donatrack.incentivos.url`/`donatrack.logistica.url` y sus variables de entorno en `docker-compose.yml` quedaron sin uso del lado de `donaciones-service`, pero no se limpiaron en este paso.

### ✅ Hecho (19/9 — punto 4 resuelto: `matriz-productor-consumidor.md` reescrita)

Comparada fila por fila contra `catalogo-mensajes.md` (fuente de verdad) y el código real. Reescrita por completo, no parcheada — la versión vieja tenía una estructura entera (por "Ola 1/2/3") que el propio catálogo dice que reemplaza. Cambios:

- **9 filas, una por evento confirmado**, sin filas de "reuso" separadas — cuando un evento tiene 2 consumidores se listan ambos en la misma fila (`DonanteRegistradoV1`, `DonacionRecibidaV1`, `PersonaSincronizadaV1`).
- **Se sacó la columna "Fase"/Ola** (decisión del usuario) — ya no hay eventos "por confirmar" en distintas oleadas, los 9 están igual de confirmados.
- **La columna "Reemplaza (Feign)" se mantuvo, renombrada a "Origen (pre-RabbitMQ)"** (decisión del usuario) — valor histórico de dónde vino cada evento, aunque esas clases Feign ya no existan en el código (se borraron en el punto 3).
- **3 filas eliminadas por completo** (no corregidas): `PersonaReplicaV1` (nombre/alcance viejo de `PersonaSincronizadaV1`), `EntregaSolicitadaV1` (el Command a Logística que se descartó en la reunión del 15/9), `DonacionCargadaV1` (versión temprana especulativa, sin relación real con ningún evento final).
- **Agregada la fila que faltaba**: `DonacionSegmentadaV1`, que no existía cuando se escribió la versión anterior.
- Corregidas dos afirmaciones del encabezado que ya eran falsas: la topología de exchange decía "no decidida" (está confirmada desde el 15/9) y el alcance decía "van a migrar" (ya migraron).

### Nota — gap de consumidores en otros servicios (no es trabajo nuestro, solo para que quede registrado)

Al verificar el lado consumidor de los 9 eventos (con grep de `@RabbitListener`/`@RabbitHandler` real en cada servicio, no solo colas/bindings declarados en su `RabbitMQConfig.java`), esto era cierto al 19/9:

- **`notificaciones-service`** tiene 7 de los 8 handlers que le corresponden (`DonanteRegistradoV1`, `DonacionAsignadaV1`, `DonacionRecibidaV1`, `DonacionEnCaminoV1`, `DonacionVencidaV1`, `DonacionEntregaFallidaV1`, y el de `donante.inactivo` que emite Incentivos). **Falta `PersonaSincronizadaV1`**: la cola está bindeada por el wildcard `persona.#`, pero no hay ningún `@RabbitHandler` que la procese. La lógica de persistencia ya existe (`IPersonasService.sincronizar()`, la misma que usaba el endpoint REST `PUT /api/notificaciones/personas` antes de esta migración) — solo falta cablear el consumer a esa lógica.
- **`incentivos-service`** no tiene **ningún** `@RabbitListener` implementado en todo el servicio. Tiene cola/binding/`idClassMapping` listos para `DonacionAsignadaV1`, `DonacionSegmentadaV1` y `PersonaSincronizadaV1`, pero nada los procesa — y ni siquiera tiene binding declarado todavía para `DonanteRegistradoV1` ni `DonacionRecibidaV1` (los dos eventos que debería reusar por diseño).
- **`logistica-service`** sí tiene su consumer andando (`DonacionAsignadaEventListener`, sobre `DonacionAsignadaV1`) — este servicio está al día.

Esto significa que, hasta que los equipos de Notificaciones e Incentivos completen sus consumers, los tests de integración/e2e que dependen de la réplica de `Persona` (`PersonIntegrationIT`, `CrossServiceCommunicationIT`, `DonationIntegrationIT`, `FullDistributedDonationE2EIT`) van a seguir fallando — no por nada de `donaciones-service`, que está completo del lado productor. No se toca código de esos servicios desde acá.

### Pendiente

1. Agregar DLX al `LogisticaEventListener` existente.
2. Más adelante (no ahora): persistencia real (JPA/Postgres) de los 7 agregados que ahora hacen falta (los 6 originales + `Donacion`, por `donacion.segmentada`), y swap del Outbox en memoria al real.
3. (Opcional, no bloqueante) Limpiar las properties/env vars de Notificaciones/Incentivos/Logística que quedaron sin uso tras el retiro de Feign — es config de despliegue compartida, evaluar aparte.
