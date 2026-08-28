# Plan de Refactor por Oleadas — Servicio de Notificaciones

> Adaptación de `plan-refactor-oleadas-generico-v2.md` al servicio de notificaciones, siguiendo la receta de su §7. Se apoya en los hallazgos de [`fase-0-auditoria.md`](./fase-0-auditoria.md) — leer ese documento primero. Rama de trabajo: `E4_refactor-notificaciones`.
>
> Convención de estado por oleada: `✅` = hay diff de código y la suite pasa en verde. `📝` = decisión de diseño/análisis, sin diff. Las Oleadas 1, 2, 3, 4, 6, 7 y 8 ya están ejecutadas y documentadas con su propia "Bitácora de ejecución" (formato de gobernanza de la plantilla v2 §3: Problema/Evidencia/Objetivo/Fuera de scope/Tests/Diseño/IA/Verificación humana); las Oleadas 5, 9, 9.5, 10 y 11 todavía son plan, sin ejecutar.

## Agregados principales del servicio

Siguiendo `docs/aggregates/aggregate-servicio-notificaciones.md` (confirmado contra el código en la Fase 0):

- **`Notificacion`** — Aggregate Root principal. Registra el intento y estado final de una notificación despachada.
- **`Persona`** — Aggregate Root secundario, réplica de lectura de datos de contacto (no es dueño de reglas de negocio complejas).
- **`EventoNotificable`/`EventoDeDonacion`** (+ 7 subclases) — Políticas de dominio transitorias (factorías polimórficas), **no** son Aggregate Roots ni tienen repositorio propio. Esta clasificación ya está decidida y documentada por el equipo; este plan no la cuestiona.

## Oleadas que no aplican a este servicio

| Oleada del plan genérico | ¿Aplica? | Motivo |
|---|---|---|
| **Oleada 5 — Scheduling** | ❌ No | No hay ningún `@Scheduled` en `notificaciones-service` (confirmado por `grep`, Fase 0 §9). Se omite por completo. |

Todas las demás oleadas (1, 2, 3, 4, 6, 7, 8, 9, 9.5, 10, 11) aplican. La Oleada 11 (sincronización con `common-lib`) no es condicional: `common-lib` ya tiene `AgregadoConEventos<E>`/`EventoDeDominio` (construidos por incentivos-service) y notificaciones todavía no los usa (Fase 0.5).

---

## Oleada 1 — Tell, Don't Ask en entidades desacopladas

**Pregunta clave:** ¿qué entidades tienen decisiones de estado viviendo en services?

**Respuesta:** ninguna decisión de negocio grande vive fuera del dominio (ver Fase 0 §5-6). El hallazgo de esta oleada es más chico y puntual: `MedioDeContacto`/`Correo`/`Telefono` exponen `@Setter` público (Lombok) sobre `esPredeterminado`, y quien decide desmarcar/marcar el medio predeterminado es `Persona.definirMedioDeContactoPredeterminado()` — la decisión ya está bien ubicada, pero la ejecuta llamando setters públicos genéricos en lugar de un método semántico del propio medio.

**RF de esta oleada:** `RF-01` (ver Fase 0 §13) — introducir `marcarComoPredeterminado()`/`desmarcarComoPredeterminado()` en `MedioDeContacto`, eliminando el `@Setter` de ese campo puntual. Es el nodo más aislado del grafo de dependencias (Fase 0 §12) — trabajo en paralelo seguro.

**Fuera de scope:** el resto de los `@Setter` del dominio (en `EventoNotificable` y su jerarquía) — eso requiere cambiar la forma en que las subclases llaman al constructor de la base, y se aborda en Oleada 3 (RF-06), no acá, para no mezclar dos diseños distintos en el mismo PR.

### Bitácora de ejecución — ✅ RF-01

#### Problema
`MedioDeContacto` exponía `@Setter` (Lombok) público sobre su único campo, `esPredeterminado`. Cualquier código externo podía asignarlo directamente en vez de pedirle a la entidad que ejecute su propio comportamiento — violación de Tell, Don't Ask sobre el nodo más aislado del grafo de dependencias del servicio.

#### Evidencia
- `MedioDeContacto.java:9` — `@Setter` a nivel de clase, sobre el campo `esPredeterminado`.
- `Persona.java:53` — `.ifPresent(m -> m.setEsPredeterminado(false));`
- `Persona.java:55` — `medioDeContacto.setEsPredeterminado(true);`
- `AdminSeeder.java:41` — `correo.setEsPredeterminado(true);`
- `MedioDeContactoMapper.java:39` — `medio.setEsPredeterminado(dto.esPredeterminado());` (asignación directa de un `Boolean` nullable, no un "marcar/desmarcar el resto" como en `Persona`).
- 4 archivos de test con el mismo call site: `NotificacionRouterTest.java` (5 llamadas — el RF original decía 4; se corrige acá con evidencia: líneas 40, 46, 53, 73, 88), `NotificacionServiceTest.java:57`, `PersonaMapperTest.java:53`, `NotificacionGestorTest.java:43`.

#### Objetivo
Reemplazar el `@Setter` de `esPredeterminado` por dos métodos semánticos (`marcarComoPredeterminado()`/`desmarcarComoPredeterminado()`), y migrar todos los call sites de producción y de test a usarlos, sin dejar ningún caller en `setEsPredeterminado`.

#### Decisión de diseño previa (consultada con la usuaria antes de tocar código)
`MedioDeContactoMapper.toEntity(dto)` no hace un "marcar y desmarcar el resto" como `Persona` — asigna el valor de `dto.esPredeterminado(): Boolean` (nullable) tal cual llega. Sin un tercer método genérico (explícitamente fuera de alcance) hay que decidir a qué método mapea un valor `null`. Alternativas planteadas:
- **A — null → desmarcar** (`Boolean.TRUE.equals(valor) ? marcar() : desmarcar()`): nunca deja el campo en `null`, coincide con el default del constructor (`esPredeterminado = false`). Cambia el comportamiento actual (hoy un DTO con `null` deja el campo en `null` literal), pero no hay ningún test que dependa de eso.
- **B — null → marcar** (`Boolean.FALSE.equals(valor) ? desmarcar() : marcar()`): riesgo de que dos medios con datos incompletos (`null`) terminen ambos marcados como predeterminados a la vez — el mismo estado inconsistente que la propia ADR de "Medios de Contacto" ya advertía como posible.

**Decisión de la usuaria: Alternativa A** (null → desmarcar).

#### Fuera de scope
- El resto de los `@Setter` del dominio en `EventoNotificable`/`EventoDeDonacion` y sus 7 subclases → RF-06 (Oleada 3).
- El riesgo de `NullPointerException` en `Notificacion.ordenarMedios()` cuando `esPredeterminado` llega `null` → RF-07 (Oleada 9.5), aunque esta oleada toca el mismo campo desde otro ángulo (mapeo, no comparación).
- Mover `MedioDeContactoTest.java` del paquete `models/entities/medioDeContacto` al paquete real `models/entities/personas` → Oleada 6. **No se adelantó**: el archivo se editó in-place, sin moverlo de paquete.
- Domain Events en `Notificacion` → RF-02 (Oleada 2). No se tocó nada de `NotificacionService`/`NotificacionGestor` más allá de los call sites de test que ya usaban el setter viejo.

#### Qué se hizo
1. `MedioDeContacto.java`: quitado `@Setter`; agregados `marcarComoPredeterminado()` y `desmarcarComoPredeterminado()`. `@Getter` intacto.
2. `Persona.definirMedioDeContactoPredeterminado()` (líneas 53/55): usa los dos métodos nuevos.
3. `AdminSeeder.java:41`: `correo.setEsPredeterminado(true)` → `correo.marcarComoPredeterminado()`.
4. `MedioDeContactoMapper.toEntity(dto)`: reemplazado el `set` directo por un `if (Boolean.TRUE.equals(dto.esPredeterminado()))` (Alternativa A), con comentario explicando el criterio.
5. Los 4 archivos de test (5 call sites en `NotificacionRouterTest`, 1 en cada uno de los otros 3) migrados a los métodos semánticos — ninguno quedó con el setter viejo, ninguno usa reflection.
6. Tests nuevos agregados (no existían antes de este RF): 3 en `MedioDeContactoTest.java` (estado inicial no-predeterminado, marcar, desmarcar) y un archivo nuevo `MedioDeContactoMapperTest.java` (3 tests: `true`→marcado, `false`→desmarcado, `null`→desmarcado) — este último no existía en absoluto antes del RF, y era necesario para dejar probada la decisión de diseño de la Alternativa A.

#### Tests / Verificación
- Baseline previo al cambio: suite de `notificaciones-service` en verde (confirmado con `mvn -pl notificaciones-service -am test`, exit code 0) antes de tocar ningún archivo.
- `PersonaTest.definirMedioDeContactoPredeterminado()` (ya existente) actúa como characterization test de este RF: sigue pasando sin modificarse, porque solo ejercita el método público de `Persona`, no el setter.
- Tests nuevos: 3 en `MedioDeContactoTest` + 3 en `MedioDeContactoMapperTest` (archivo nuevo).
- Barrido mecánico de cierre: `grep -rn "setEsPredeterminado" notificaciones-service/src` → 0 matches (los únicos restantes están en `target/site/jacoco/*.html`, reporte de cobertura desactualizado, no fuente).
- Suite de `notificaciones-service`: **71 tests, 0 failures, 0 errors** (antes: 65 — 6 tests nuevos).
- Reactor completo (`mvn test` desde la raíz): **612 tests, 0 failures, 0 errors** — `common-lib` 9, `donaciones-service` 309, `incentivos-service` 56, `logistica-service` 167, `notificaciones-service` 71.

#### Diseño resultante
`MedioDeContacto` decide su propio estado de "predeterminado" a través de dos métodos con nombre de intención (Tell, Don't Ask); `Persona` y `MedioDeContactoMapper` informan la intención (marcar/desmarcar) en vez de asignar un valor crudo. `Correo`/`Telefono` conservan su propio `@Setter` (Lombok) sobre sus campos propios — no se tocó, porque ese `@Setter` no aplica al campo `esPredeterminado` (heredado) y queda fuera de este RF.

#### IA utilizada
Detección estática de los 6 call sites (`grep`), verificación de que no había ningún test existente que ejerciera `esPredeterminado == null` antes de fijar el criterio de la Alternativa A, generación de los tests nuevos y de los reemplazos mecánicos en los 4 archivos de test. La decisión de diseño (Alternativa A vs. B) se dejó explícitamente a la usuaria, no se asumió.

#### Verificación humana
- [x] `grep -rn "setEsPredeterminado" notificaciones-service/src` → 0 matches.
- [x] Ningún test usa reflection ni un setter paralelo de test.
- [x] `MedioDeContactoTest.java` no se movió de paquete (se dejó explícito como fuera de scope, no se adelantó Oleada 6).
- [x] Suite de `notificaciones-service` en verde (71/71).
- [x] Reactor completo en verde (612/612).
- [x] Formatter/linter (`spotless:check`) — no corrido en este RF (no fue solicitado explícitamente; queda para antes de abrir el PR).
  git add docs/refactor/notificaciones/fase-0-auditoria.md docs/design/notificaciones-service/
  git commit -m "docs: Fase 0 - auditoria y diagrama de clases de notificaciones-service"
#### 🔁 Devolución necesaria
Archivos con call sites de `setEsPredeterminado` actualizados a `marcarComoPredeterminado()`/`desmarcarComoPredeterminado()` — 2 de producción + 4 de test, sin ninguno roto:
1. `notificaciones-service/src/main/java/grupo5/notificaciones/config/AdminSeeder.java` (línea 41)
2. `notificaciones-service/src/main/java/grupo5/notificaciones/services/mappers/MedioDeContactoMapper.java` (línea 39, ahora if/else)
3. `notificaciones-service/src/test/java/grupo5/notificaciones/infrastructure/NotificacionRouterTest.java` (5 call sites: líneas 40, 46, 53, 73, 88 — no 4 como decía el pedido original)
4. `notificaciones-service/src/test/java/grupo5/notificaciones/services/NotificacionServiceTest.java` (línea 57)
5. `notificaciones-service/src/test/java/grupo5/notificaciones/services/mappers/PersonaMapperTest.java` (línea 53)
6. `notificaciones-service/src/test/java/grupo5/notificaciones/services/NotificacionGestorTest.java` (línea 43)

Más 2 archivos de `models/entities/personas/Persona.java` (líneas 53/55) y `models/entities/personas/MedioDeContacto.java` (definición de los métodos nuevos), que no tenían "callers rotos" sino que son el propio origen del cambio.

---

## Oleada 2 — Domain Events en el Aggregate Root principal (`Notificacion`)

**Pregunta clave:** ¿cuál es el aggregate root principal? ¿qué eventos genera? ¿`common-lib` ya tiene una base de agregado con eventos para heredar?

**Respuesta:** el aggregate root principal es `Notificacion`. Hoy no genera domain events propios: `NotificacionService.procesar()` construye y publica `NotificacionesCreadasEvent` a mano (Fase 0 §8) — es el antipatrón central que esta oleada corrige. `common-lib` ya tiene una base de agregado con eventos (`AgregadoConEventos<E extends EventoDeDominio>`, construida por incentivos-service — Fase 0.5), pero esta oleada implementa el mecanismo localmente en `Notificacion` (lista de eventos + interfaz marcadora `NotificacionDomainEvent`, sin extender esa base) para no acoplar esta oleada a una migración de herencia; la migración a `AgregadoConEventos<E>` queda registrada como Oleada 11.

**RF de esta oleada:** `RF-02`.

**Acciones concretas:**
1. `Notificacion` gestiona internamente una lista de domain events (candidatos: `NotificacionCreada`, `NotificacionEnviada`, `NotificacionFallida`) con `getDomainEvents()` devolviendo `List.copyOf(this.domainEvents)` — nunca `Collections.unmodifiableList`.
2. Test canónico de reentrancia: el snapshot devuelto por `getDomainEvents()` queda intacto después de `clearDomainEvents()`, y rechaza mutaciones (`UnsupportedOperationException` al intentar modificarlo).
3. `notificar()`/`actualizarEstado()` registran el evento correspondiente en cada transición real de estado.
4. `NotificacionService.procesar()` deja de construir `NotificacionesCreadasEvent` a mano: persiste, lee `getDomainEvents()` de cada `Notificacion` creada, publica, limpia.
5. `NotificacionGestor` sigue reaccionando a que "hay notificaciones nuevas que enviar" — evaluar si conviene que reaccione directamente al nuevo domain event en lugar de al `ApplicationEvent` genérico actual, o si se mantiene una capa de traducción; **decisión de diseño a tomar durante la ejecución de la oleada, no en este plan**.
6. Documentar explícitamente (📝) la decisión sobre `Persona` (Oleada 3, RF-03): es una réplica de lectura, probablemente no necesita domain events propios.

**Fuera de scope:** decidir si `EventoNotificable`/`EventoDeDonacion` deberían convertirse también en domain events formales — el equipo ya decidió (ADR + `aggregate-servicio-notificaciones.md`) que son políticas transitorias sin identidad persistente, y esta oleada no reabre esa decisión.

### Bitácora de ejecución — ✅ RF-02

#### Revisión previa de la Oleada 1 (pedida antes de arrancar)
Se releyó la sección "🔁 Devolución necesaria" de la Oleada 1 y se confirmó en el working tree (no solo en la bitácora) que ninguno de los 6 archivos quedó roto: `grep -rn "setEsPredeterminado" notificaciones-service/src` → 0 matches; los 9 archivos que referencian `marcarComoPredeterminado`/`desmarcarComoPredeterminado` siguen intactos. `git status` confirma que el working tree de la Oleada 1 sigue presente, sin reversiones. Se procedió a tocar `Notificacion`/`NotificacionService`/`NotificacionGestor` con esa base confirmada.

#### Problema
`NotificacionService.procesar()` construía `NotificacionesCreadasEvent` a mano y lo publicaba — un evento de aplicación vacío de contenido (ni siquiera decía qué notificación se creó), en vez de que `Notificacion` (el Aggregate Root) generara sus propios domain events. Además, en el mismo archivo, `Notificacion.getHistorialEstado()` (getter liso de Lombok) devolvía la referencia mutable interna de `historialEstado` — mismo antipatrón de snapshot no inmutable que domainEvents estaba a punto de introducir si no se corregía también.

#### Evidencia
- `NotificacionService.java` (antes): `eventPublisher.publishEvent(new NotificacionesCreadasEvent(this));` dentro de `procesar()`.
- `NotificacionGestor.java` (antes): `@EventListener public void onNotificacionesCreadas(NotificacionesCreadasEvent event)`.
- `Notificacion.java`: clase anotada `@Getter` a nivel de clase sin excepción para `historialEstado` → `getHistorialEstado()` generado por Lombok devolvía `this.historialEstado` tal cual (mutable).
- `common-lib/.../AggregateRoot.java`: confirmado que solo define `getId()` — no hay ninguna base de agregado con eventos que heredar (Fase 0.5 de la auditoría).

#### Objetivo
Que `Notificacion` gestione sus propios domain events (`NotificacionCreada`, `NotificacionEnviada`, `NotificacionFallida`) desde `actualizarEstado()`, con `getDomainEvents()` devolviendo `List.copyOf(...)` y `clearDomainEvents()`; que `NotificacionService.procesar()` persista, lea, publique y limpie esos eventos en vez de inventar uno; y corregir `getHistorialEstado()` con el mismo criterio de inmutabilidad.

#### Decisión de diseño tomada (no consultada — el pedido explícitamente dejó esto a mi criterio)
**`NotificacionGestor` pasa a escuchar directamente el domain event (`NotificacionCreada`), eliminando `NotificacionesCreadasEvent` por completo**, en vez de mantenerlo como capa de traducción.

Por qué: el evento viejo no llevaba ningún dato (`new NotificacionesCreadasEvent(this)`, `this` siendo el propio `NotificacionService`) — era un signal vacío, mantenerlo como intermediario no aportaba nada una vez que existe un evento de dominio real y con contenido (`notificacionId`, `personaId`, `fecha`). Eliminarlo también cierra el RF de punta a punta: ya no queda ningún rastro del evento de aplicación ad-hoc que esta oleada vino a reemplazar.

Costado a documentar, no a esconder: con el evento viejo, `notificarPendientes()` se disparaba **una vez por llamada a `procesar()`** (sin importar cuántas notificaciones creara esa llamada). Con el evento nuevo, se dispara **una vez por cada `Notificacion` creada** — un `EntregaFallida` (3 destinatarios) ahora dispara `notificarPendientes()` 3 veces en vez de 1. Cada invocación sigue siendo segura (`notificarPendientes()` relee `findByEstado(PENDIENTE)` y ya no encuentra las que la invocación anterior dejó `ENVIADA`/`FALLIDA`), así que no hay duplicación de envíos — solo repositorio-scans redundantes en el caso de varios destinatarios por evento. No empeora el riesgo de concurrencia/idempotencia ya señalado en RF-10/Oleada 9.5 (ese riesgo ya existía con el evento viejo, que tampoco distinguía qué notificación lo disparó); queda anotado ahí, no se resuelve acá.

**Consecuencia no listada explícitamente en el pedido, pero necesaria para que el RF no deje un hueco:** si solo se enganchaba `NotificacionCreada` en `NotificacionService`, los eventos `NotificacionEnviada`/`NotificacionFallida` que `actualizarEstado()` genera dentro de `notificacion.notificar(sender)` (llamado desde `NotificacionGestor.notificarPendientes()`) iban a quedar generados pero nunca publicados ni limpiados — la "regla de oro" del plan de refactor ("si la mutación generó domain events, hay que publicarlos") se hubiera roto en `NotificacionGestor`, no en `NotificacionService`. Se agregó `ApplicationEventPublisher` a `NotificacionGestor` y el mismo patrón persistir→publicar→limpiar después de cada `save()` dentro de `notificarPendientes()`. Esto excede lo enumerado literalmente en "Alcance (SÍ)", pero es una extensión directa del propio objetivo de RF-02 (que los 3 tipos de evento listados —no solo el primero— realmente lleguen a algún lado); se documenta acá explícitamente en vez de dejarlo pasar en silencio.

#### Fuera de scope
- Campos muertos `personaRepository`/`sender` en `NotificacionService` — **no se tocaron**, siguen en el constructor tal cual (confirmado: `grep -n "personaRepository\|sender" NotificacionService.java` sigue mostrando las mismas 4 líneas de antes). RF-08, Oleada 4.
- `@Setter` de `EventoNotificable`/`EventoDeDonacion` y de `Persona.getMediosDeContacto()` — no se tocaron. RF-04/RF-06, Oleada 3.
- Domain Events en `Persona` — no implementados. Nota 📝 más abajo.

#### 📝 Nota de diseño — Persona y Domain Events (RF-03, no implementado en esta oleada)
`Persona` es una réplica de lectura (proyección local sincronizada vía `sincronizar()`/`anonimizar()`, per `aggregate-servicio-notificaciones.md` y confirmado en la Fase 0): no toma decisiones de negocio propias más allá de mantener consistente su lista de medios de contacto. No se le ven hoy consumidores que necesiten reaccionar a "una persona se sincronizó" o "una persona se anonimizó" dentro de este mismo servicio. Se deja explícitamente documentado que **probablemente no necesita domain events propios**, para que la Oleada 3 no tenga que reabrir esta pregunta desde cero — si en el futuro aparece un consumidor real (ej. auditoría de anonimización), ahí se evalúa agregarlos.

#### Qué se hizo
1. Nuevo paquete `grupo5.notificaciones.models.entities.notificaciones.events` (inglés, para no confundir con el paquete `eventos` en español que ya usan las políticas `EventoNotificable`/`EventoDeDonacion`): `NotificacionDomainEvent` (interfaz marcadora), `NotificacionCreada`, `NotificacionEnviada`, `NotificacionFallida` (records inmutables).
2. `Notificacion.java`: campo `domainEvents` (`List<NotificacionDomainEvent>`, `transient`, sin `@Getter` de Lombok); `getDomainEvents()` devuelve `List.copyOf(...)`; `clearDomainEvents()`. `actualizarEstado()` registra el evento correspondiente según el nuevo estado (`PENDIENTE`→`NotificacionCreada`, `ENVIADA`→`NotificacionEnviada`, `FALLIDA`→`NotificacionFallida`) en un método privado `registrarDomainEvent(...)`.
3. `Notificacion.getHistorialEstado()`: de getter liso de Lombok a método explícito con `List.copyOf(this.historialEstado)` (mismo criterio que domainEvents, en el mismo archivo, tal como se pidió).
4. `NotificacionService.procesar()`: ya no construye `NotificacionesCreadasEvent`; después de `saveAll`, itera las notificaciones creadas, publica cada domain event y limpia.
5. `NotificacionGestor`: nuevo parámetro de constructor `ApplicationEventPublisher`; el `@EventListener` pasa a `onNotificacionCreada(NotificacionCreada event)`; `notificarPendientes()` publica y limpia los domain events de cada notificación después de `save()`.
6. Eliminado `services/events/NotificacionesCreadasEvent.java` (y el paquete `services/events`, que quedó vacío) — ya no lo construye ni lo escucha nadie.
7. Tests actualizados: `NotificacionServiceTest` (las 2 aserciones de `NotificacionesCreadasEvent` pasan a `NotificacionCreada`, con conteo corregido a `times(3)` en el caso de `EntregaFallida`); `NotificacionGestorTest` (mock de `ApplicationEventPublisher`, nuevas aserciones de `NotificacionEnviada`/`NotificacionFallida` publicados y limpiados, test nuevo para `onNotificacionCreada`).
8. Test nuevo `NotificacionTest.java` (no existía — hueco de Fase 0 §10), acotado a lo que esta oleada modificó: registro de los 3 tipos de evento, reentrancia/inmutabilidad de `getDomainEvents()` y `getHistorialEstado()`, `clearDomainEvents()`, y el caso borde de `persona == null` en `notificar()`. No cubre `notificar()`/`ordenarMedios()` de punta a punta — eso sigue siendo un hueco para Oleada 8.
9. `mvn spotless:apply` sobre `notificaciones-service` para corregir formato (reordenamiento de imports, wrapping de líneas) detectado por `spotless:check` al compilar los tests — sin cambios semánticos, confirmado revisando el diff.

#### Tests / Verificación
- Baseline previo (heredado de la Oleada 1, reverificado): suite de `notificaciones-service` en verde antes de tocar `Notificacion`/`NotificacionService`/`NotificacionGestor`.
- Tests nuevos: 8 en `NotificacionTest.java` (archivo nuevo), 1 nuevo en `NotificacionGestorTest` (`onNotificacionCreada_deberiaDelegarANotificarPendientes`), más aserciones nuevas agregadas a los 2 tests existentes de `NotificacionGestorTest` y a los 2 de `NotificacionServiceTest` que ya tocaban el evento.
- Barrido mecánico: `grep -rn "NotificacionesCreadasEvent" notificaciones-service/src` → 0 matches (clase eliminada).
- Suite `notificaciones-service`: **80 tests, 0 failures, 0 errors** (71 antes de esta oleada + 9 nuevos).
- Reactor completo: **621 tests, 0 failures, 0 errors** — `common-lib` 9, `donaciones-service` 309, `incentivos-service` 56, `logistica-service` 167, `notificaciones-service` 80.
- `mvn spotless:apply` corrido sobre `notificaciones-service` para que `spotless:check` (atado al ciclo de vida de test) no bloqueara el build; el diff se revisó y es puramente de formato.

#### Diseño resultante
`Notificacion` es dueña de su propio ciclo de vida de eventos: cada transición de estado real (incluida la creación) queda registrada como un hecho de dominio inmutable y recuperable vía snapshot. `NotificacionService` y `NotificacionGestor` son los únicos que persisten, publican y limpian esos eventos — ninguno de los dos vuelve a construir un evento de la nada. `NotificacionGestor` reacciona directamente al hecho de dominio (`NotificacionCreada`), sin capa de traducción intermedia.

#### IA utilizada
Detección estática de usos existentes de `getHistorialEstado()`/`NotificacionesCreadasEvent` antes de tocar esas clases (para confirmar que no había characterization tests que se rompieran), diseño e implementación del mecanismo de domain events y de los 3 records, migración de los tests existentes, generación de `NotificacionTest.java`. La decisión de acoplar `NotificacionGestor` directamente al domain event (en vez de mantener la traducción) y la decisión de extender el patrón persistir→publicar→limpiar a `NotificacionGestor` (no pedida explícitamente) se tomaron aplicando la "regla de oro" del plan de refactor, documentadas acá para que se puedan revisar o revertir.

#### Verificación humana
- [x] `Notificacion` genera `NotificacionCreada`/`NotificacionEnviada`/`NotificacionFallida` en cada transición real de `actualizarEstado()`.
- [x] `getDomainEvents()` y `getHistorialEstado()` devuelven `List.copyOf(...)`, nunca la lista interna ni `Collections.unmodifiableList`.
- [x] Test de reentrancia: un snapshot tomado antes de una transición posterior no crece, y rechaza mutaciones (`UnsupportedOperationException`) — cubierto para ambas colecciones.
- [x] `NotificacionService.procesar()` ya no construye ningún evento a mano.
- [x] `NotificacionesCreadasEvent` eliminado, 0 referencias remanentes.
- [x] Campos muertos de `NotificacionService` y `@Setter` de Oleada 3 no tocados.
- [x] Suite de `notificaciones-service` en verde (80/80).
- [x] Reactor completo en verde (621/621).
- [x] `spotless:check` en verde tras `spotless:apply` (diff puramente de formato, revisado).

#### 🔁 Devolución necesaria
`common-lib` ya tiene una base genérica para este mismo patrón: `AgregadoConEventos<E extends EventoDeDominio>` (campo `domainEvents` privado, `registrarEvento()`, `getDomainEvents()` con `List.copyOf`, `clearDomainEvents()`) y la interfaz `EventoDeDominio` (`id: UUID`, `timestamp: LocalDateTime`), construidas por incentivos-service. El mecanismo implementado acá en `Notificacion.java` (interfaz marcadora `NotificacionDomainEvent`, campo + `getDomainEvents()`/`clearDomainEvents()` locales) es equivalente en forma pero no extiende esa base — queda registrado como pendiente de migración en la Oleada 11 (Sincronización con `common-lib`), en vez de quedar como una reimplementación paralela.
- El patrón de orquestación (persistir → leer `getDomainEvents()` → publicar cada uno → `clearDomainEvents()`), aplicado en `NotificacionService.procesar()` y `NotificacionGestor.notificarPendientes()`, no cambia con la migración: sigue funcionando igual una vez que `Notificacion` extienda `AgregadoConEventos<NotificacionDomainEvent>`.
- `donaciones-service/.../Propuesta.java` tiene una implementación local de este mismo patrón con un antipatrón que esta oleada evitó en `Notificacion` (`@Getter` de Lombok liso sobre la lista mutable, sin `List.copyOf`) — también es candidata a migrar a `AgregadoConEventos<E>`, fuera del alcance de este servicio.

---

## Oleada 3 — Domain Events en agregado secundario + pureza del dominio

**Pregunta clave:** ¿hay un segundo agregado con transiciones de estado? ¿switches anémicos? ¿excepciones crudas o `@Setter` en el dominio? ¿algún concepto juega doble rol de plantilla/instancia poseída?

**Respuesta:**
- Segundo agregado: `Persona`, sin transiciones de estado complejas (solo `anonimizar()` y gestión de la lista de medios).
- Switch anémico: no se detectó ninguno en un Service; el `switch` de `EventoMapper.toEntity()` es sobre un `sealed interface` (`EventoNotificableDTO`), que es exactamente el patrón que el plan v2 recomienda para reemplazar `instanceof` — **no es un hallazgo negativo**, es un ejemplo a preservar.
- Excepciones crudas: sí, 2 en `MedioDeContactoMapper` + 1 en `EventoMapper.buscarPersona()` (RF-05).
- `@Setter` en dominio: sí, 10 de 12 clases no-agregado (RF-06).
- Doble rol plantilla/instancia poseída: no se detectó ningún caso análogo al de "recompensa" de incentivos — 📝 se documenta que este ítem **no aplica** a este servicio, para no reabrirlo después.

**RFs de esta oleada:** `RF-03`, `RF-04`, `RF-05`, `RF-06`.

**Acciones concretas:**
1. (RF-03, 📝) Documentar si `Persona` necesita domain events — probable respuesta: no, por ser proyección de lectura pura.
2. (RF-04) `Persona.getMediosDeContacto()`: `Collections.unmodifiableList` → `List.copyOf`.
3. (RF-05) `MedioDeContactoMapper` y `EventoMapper.buscarPersona()`: `IllegalArgumentException` → `ValidationException`/`RecursoNoEncontradoException` con código de `ErrorCatalog` (agregar códigos nuevos si faltan, ej. `MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO`). Barrido de cierre: `grep -rn "throw new IllegalArgumentException" .` → 0 matches (hoy: 2, más 1 vía `orElseThrow` sin la palabra `throw` literal — verificar con `grep -rn "new IllegalArgumentException" .` → objetivo también 0).
4. (RF-06) Eliminar `@Setter` de `EventoNotificable`, `EventoDeDonacion` y las 7 subclases; introducir constructor(es) protegido(s) en las clases base para que las subclases pasen sus valores sin depender de setters heredados públicos. Barrido de cierre: `grep -rn "@Setter" models/` → objetivo 0 (hoy: 12, de los cuales 2 —`MedioDeContacto`/`Correo`/`Telefono`— ya se resolvieron en Oleada 1).
5. Guardas estrictas en los constructores de las 7 subclases de evento (obligatoriedad de `persona`/`fecha`, y de los campos propios de cada una).

**Fuera de scope:** el riesgo de `NullPointerException` en `ordenarMedios()` por `esPredeterminado == null` — se agrupa con el hardening de bordes (Oleada 9.5, RF-07) para no mezclar "pureza del dominio" con "casos borde de ejecución" en el mismo PR, aunque tocan archivos parecidos.

### Bitácora de ejecución — ✅ RF-03 (📝) + RF-04 + RF-05 + RF-06

#### Conteo real de `@Setter` (pedido antes de arrancar)
`grep -rln "@Setter" models/entities/notificaciones/eventos/` da **9 archivos** con la anotación declarada: `EventoNotificable`, `EventoDeDonacion`, `DonacionEnCamino`, `DonacionRecibida`, `EntregaFallida`, `MisionCumplida`, `SubioCategoria`, `DonanteInactivo`, `DonanteRegistrado`. **`DonacionAsignada` no está en esa lista** — no tiene campos propios, así que no hay nada que anotarle con `@Setter`. Pero sí es una de las clases que este RF tiene que tocar, porque su constructor llama a `this.setPersona(...)/this.setEntidadBeneficiaria(...)/etc.` (setters heredados de `EventoDeDonacion`/`EventoNotificable`, que van a desaparecer). Conclusión: **9 archivos con `@Setter` propio + `DonacionAsignada` sin `@Setter` propio pero con constructor a reescribir = 10 clases tocadas por RF-06**, que es el número que se pidió usar. Quedó documentado acá porque "10 clases con `@Setter` público" no es literalmente exacto (son 9 con la anotación + 1 sin ella pero igual de afectada).

#### Landmine confirmado y resuelto
`DonanteInactivoTest.java` instanciaba con `new DonanteInactivo()` + `setPersona()`/`setDiasInactividad()`. Reescrito para usar el constructor completo (`new DonanteInactivo(mockDonante, dias, TEST_DATE_TIME)`), sin agregar ningún setter ni constructor paralelo de test.

#### Problema
1. `Persona.getMediosDeContacto()` usaba `Collections.unmodifiableList` — vista de solo lectura sobre una lista que sigue siendo mutable por dentro (mismo antipatrón ya corregido en `Notificacion` en la Oleada 2, pendiente acá).
2. `MedioDeContactoMapper` (2 sitios) y `EventoMapper.buscarPersona()` (1 sitio, vía `orElseThrow`) lanzaban `IllegalArgumentException` cruda en vez de una excepción de `common-lib` con código de `ErrorCatalog`.
3. Las 10 clases de la jerarquía de eventos exponían `@Setter` público (o consumían setters heredados) para que cada subclase mutara sus propios campos heredados desde su constructor — violación de encapsulación/Tell-Don't-Ask, y fuente del landmine de test de arriba.

#### Evidencia
- `Persona.java:18`: `return java.util.Collections.unmodifiableList(this.mediosDeContacto);`
- `MedioDeContactoMapper.java:36,75` (antes de esta oleada): 2× `throw new IllegalArgumentException(...)`.
- `EventoMapper.java:61` (antes): `.orElseThrow(() -> new IllegalArgumentException(...))`.
- `EventoMapperTest.java:174` (antes): `assertThrows(IllegalArgumentException.class, () -> mapper.toEntity(dto));` — characterization test que confirmaba el comportamiento viejo antes de tocarlo.
- Las 10 clases listadas arriba, cada una con `this.setX(...)` en su constructor sobre setters heredados.

#### Objetivo
RF-03: documentar que `Persona` no necesita Domain Events (sin código). RF-04: `List.copyOf` en `getMediosDeContacto()`. RF-05: migrar las 3 excepciones crudas a `ValidationException`/`ErrorCatalog`. RF-06: constructores protegidos con guardas en `EventoNotificable`/`EventoDeDonacion`, `@Setter` fuera de las 10 clases, constructores vacíos eliminados si nadie los usa.

#### 📝 RF-03 — Persona no necesita Domain Events
Confirmado en esta oleada, sin código: `Persona` es una réplica de lectura sin transiciones de estado complejas (solo gestiona su lista de medios de contacto y su propia anonimización). No hay ningún consumidor dentro de este servicio que necesite reaccionar a "una persona se sincronizó/anonimizó". Se deja registrado para que Oleada 4 en adelante no reabra la pregunta — si aparece un caso de uso real, se evalúa ahí.

#### RF-05 — Decisiones de mapeo de excepciones
- **`MedioDeContactoMapper` (2 sitios):** nueva entrada en `ErrorCatalog` (`common-lib`), sección `// === NOTIFICACIONES (9xx) ===`, `MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO("ERR-VAL-901")`. Los dos `default ->` (uno en `toEntity`, otro en `toReplicaDTO`) lanzan `new ValidationException(ErrorCatalog.MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO)`.
- **`EventoMapper.buscarPersona()` (persona no encontrada):** se evaluó si alcanzaba con `RECURSO_NO_ENCONTRADO` (`ERR-CSR-001`) genérico ya existente, tal como se pidió — **sí alcanza**, no se creó un código nuevo. Se usó `new ValidationException(ErrorCatalog.RECURSO_NO_ENCONTRADO)`, el mismo patrón que ya usa `PersonasService.obtenerPersona()`/`anonimizar()` para el mismo tipo de caso, en vez de `RecursoNoEncontradoException(UUID)` (que existe en `common-lib` y sería semánticamente más preciso para un 404 real). Se prefirió consistencia interna del servicio sobre precisión semántica aislada: usar `RecursoNoEncontradoException` solo acá hubiera dejado dos criterios distintos para el mismo caso ("persona no encontrada") dentro del mismo servicio. Si se quiere el 404 real, es un cambio a evaluar para **los dos** call sites juntos (`PersonasService` y `EventoMapper`), no uno solo — queda anotado como posible ítem de una oleada futura, no se decidió acá.
- **Pérdida de mensaje descriptivo (efecto secundario a documentar, no un bug introducido):** `ValidationException` (común a los 3 casos) no acepta mensaje custom — `super()` sin argumentos, `getMessage()` da `null` (confirmado leyendo `ValidationException`/`DonaTrackException` en `common-lib`). Los mensajes dinámicos que tenían las `IllegalArgumentException` viejas (`"Persona no encontrada con ID: " + id`, `"Tipo de medio de contacto no soportado: " + tipo`) se pierden; queda solo el código de `ErrorCatalog` en la respuesta HTTP. Esto ya pasa hoy con `PersonasService` (mismo patrón, mismo límite de `common-lib`), no es una regresión de esta oleada.
- **Archivo compartido — aviso pedido:** `ErrorCatalog.java` es de `common-lib`, usado por los 4 servicios. Se agregó únicamente la sección `NOTIFICACIONES (9xx)` con 1 entrada nueva (`ERR-VAL-901`), sin tocar ninguna entrada existente. **Avisar antes de mergear** si `donaciones`/`incentivos`/`logistica` tienen una rama en paralelo tocando este mismo archivo, para evitar colisión de códigos — no se pudo verificar ramas remotas no fetcheadas desde acá.

#### RF-06 — Diseño de los constructores protegidos
- `EventoNotificable(Persona persona, LocalDateTime fecha)`: guarda `persona == null` y `fecha == null`, cada una con `ValidationException(ErrorCatalog.ARGUMENTO_NULO)` (código genérico ya existente, no se creó uno nuevo por campo — ver nota de alcance abajo).
- `EventoDeDonacion(Persona persona, Persona entidadBeneficiaria, String detalleDonacion, LocalDateTime fecha)`: llama a `super(persona, fecha)`, después guarda `entidadBeneficiaria == null` y `detalleDonacion == null`, mismo criterio.
- **Alcance de las guardas, decidido explícitamente:** el pedido dice guardas "en esos constructores protegidos" (los 2 de las clases base) — no se agregaron guardas de obligatoriedad en los campos propios de cada subclase concreta (`patenteCamion`, `motivo`, `diasInactividad`, etc.). Agregar validación a los ~13 campos propios de las 8 subclases es un cambio más grande, mejor scoped a un RF de "Value Objects / guardas estrictas" dedicado (mencionado en el catálogo del plan v2 §5, Oleada 3 general) — no se coló acá.
- Los 8 constructores vacíos (`DonacionAsignada()`, etc.) se **eliminaron los 8**, no solo el de `DonanteInactivo`: se verificó con `grep` que ninguno se usaba fuera de tests (y el único uso real, en `DonanteInactivoTest`, ya se reescribió). `EventoMapper` siempre usa el constructor completo vía el `switch` sobre el DTO sellado — nunca los vacíos.
- Los campos que antes eran mutables (`persona`, `fecha`, `entidadBeneficiaria`, `detalleDonacion`, y los propios de cada subclase) pasan a `final` — coherente con "Inmutabilidad por defecto" del plan v2, no pedido literalmente pero consistente con sacar el `@Setter` (si no son `final`, quedan mutables solo por no tener setter público, que es una protección más débil).
- Se conservaron todos los `@Getter` de clase necesarios para que `EventoMapperTest` (que verifica `getPatenteCamion()`, `getEnlaceSeguimiento()`, `getAdministracion()`, `getMotivo()`, `isReplanificable()` sobre el resultado del mapper) siguiera funcionando sin cambios — se verificó call-site por call-site antes de decidir qué `@Getter` mantener y cuál se podía dar de baja (ninguno se dio de baja, se usan todos).

#### Fuera de scope (confirmado)
- `Notificacion.getHistorialEstado()`/`getDomainEvents()` — no tocados (ya resueltos en Oleada 2).
- Guard de `esPredeterminado == null` en `ordenarMedios()` — no implementado (RF-07, Oleada 9.5).
- `personaRepository`/`sender` muertos en `NotificacionService` — no tocados (RF-08, Oleada 4); confirmado con `grep` que siguen igual que en la Oleada 2.

#### Qué se hizo
1. `ErrorCatalog.java` (`common-lib`): nueva sección `NOTIFICACIONES (9xx)` con `MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO("ERR-VAL-901")`.
2. `Persona.getMediosDeContacto()`: `Collections.unmodifiableList` → `List.copyOf`.
3. `MedioDeContactoMapper`: 2 `IllegalArgumentException` → `ValidationException(MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO)`.
4. `EventoMapper.buscarPersona()`: `IllegalArgumentException` → `ValidationException(RECURSO_NO_ENCONTRADO)`.
5. `EventoNotificable`/`EventoDeDonacion`: constructores protegidos con guardas, campos `final`, sin `@Setter`.
6. Las 8 subclases concretas: constructores reescritos para llamar a `super(...)`, campos propios `final`, constructores vacíos eliminados, sin `@Setter`.
7. Tests: `DonanteInactivoTest` reescrito (landmine); `EventoMapperTest` actualizado (`ValidationException` en vez de `IllegalArgumentException`); `EventosTest.java` +4 tests nuevos (guardas de los 2 constructores protegidos); `MedioDeContactoMapperTest.java` +2 tests nuevos (las 2 ramas de excepción, sin cobertura antes de esta oleada).
8. `mvn spotless:apply` sobre `notificaciones-service` (2 violaciones de formato mecánicas — wrapping de líneas largas — sin cambios semánticos).

#### Tests / Verificación
- Tests nuevos/actualizados: 4 en `EventosTest.java`, 2 en `MedioDeContactoMapperTest.java`, 1 reescrito en `DonanteInactivoTest.java`, 1 assertion actualizada en `EventoMapperTest.java`.
- Barrido `grep -rln "@Setter" models/entities/notificaciones/eventos/` → 1 archivo (`EventoNotificable.java`), pero es un **falso positivo**: la única coincidencia es la palabra "@Setter" dentro de un comentario explicativo mío, no una anotación real. Confirmado con `grep -rn "^\s*@Setter\s*$"` → 0 matches reales, y `grep -rln "import lombok.Setter"` → 0 matches.
- Barrido `grep -rn "new IllegalArgumentException" notificaciones-service/src` → 0 matches (antes: 2 en `MedioDeContactoMapper`; el tercer sitio, `EventoMapper`, usaba `orElseThrow` sin la palabra "new IllegalArgumentException" contigua pero también migrado y confirmado por separado).
- Barrido `grep -rn "Collections.unmodifiableList" notificaciones-service/src/main` → 0 matches.
- Suite `notificaciones-service`: **86 tests, 0 failures, 0 errors** (80 antes de esta oleada + 6 nuevos: 4 en EventosTest + 2 en MedioDeContactoMapperTest; el reescrito de DonanteInactivoTest no suma, reemplaza).
- Suite `common-lib`: **9 tests, 0 failures, 0 errors** (sin tests nuevos — el cambio en `ErrorCatalog` es aditivo, no requería test propio).
- Reactor completo: **627 tests, 0 failures, 0 errors** — `common-lib` 9, `donaciones-service` 309, `incentivos-service` 56, `logistica-service` 167, `notificaciones-service` 86.
- `mvn spotless:apply` corrido de nuevo sobre `notificaciones-service` (2 violaciones mecánicas de wrapping, mismo criterio que en Oleadas 1 y 2).

#### Diseño resultante
`Persona` expone una copia real de solo lectura de sus medios de contacto. Las 3 excepciones de negocio de la capa de mappers usan el mismo mecanismo (`ValidationException` + `ErrorCatalog`) que ya usaba `PersonasService`, homogeneizando todo `notificaciones-service` bajo un solo criterio. Toda la jerarquía de `EventoNotificable` se construye completa de una sola vez, con guardas de obligatoriedad en los 2 puntos de entrada compartidos (`EventoNotificable`, `EventoDeDonacion`) y campos `final` de ahí para abajo — ninguna subclase vuelve a mutar sus propios atributos heredados después de construirse.

#### IA utilizada
Conteo y verificación mecánica de `@Setter`/constructores vacíos/usos de getters antes de decidir qué tocar (para no romper `EventoMapperTest` sin darme cuenta); reescritura de las 10 clases y de los tests afectados; detección del landmine confirmado contra el pedido; generación de los tests nuevos de guardas y de las 2 ramas de excepción sin cobertura previa. La decisión de `ValidationException(RECURSO_NO_ENCONTRADO)` vs. `RecursoNoEncontradoException` para "persona no encontrada", y el alcance de las guardas (solo en los 2 constructores base, no en cada subclase), se tomaron aplicando el criterio de consistencia interna del servicio y el texto literal del pedido, documentadas para revisión.

#### Verificación humana
- [x] Conteo real de `@Setter` hecho antes de tocar código (9 archivos con la anotación + `DonacionAsignada` sin ella pero afectada = 10 clases).
- [x] `DonanteInactivoTest` reescrito con constructor completo, sin setter ni constructor de test paralelo.
- [x] `Persona.getMediosDeContacto()` usa `List.copyOf`.
- [x] 0 `IllegalArgumentException` nuevas en el módulo; `ErrorCatalog` con sección `NOTIFICACIONES (9xx)` nueva, sin tocar entradas existentes.
- [x] 0 `@Setter` real en `models/entities/notificaciones/eventos/` (verificado sin el falso positivo de comentario).
- [x] Constructores protegidos con guardas en `EventoNotificable`/`EventoDeDonacion`; constructores vacíos eliminados donde nadie los usaba (los 8).
- [x] Campos muertos de `NotificacionService` y guard de `esPredeterminado == null` no tocados.
- [x] Suite de `notificaciones-service` en verde (86/86).
- [x] Reactor completo en verde (627/627).
- [ ] **Pendiente de la usuaria:** confirmar que ninguna otra rama está tocando `ErrorCatalog.java` en paralelo antes de mergear esta oleada.

---

## Oleada 4 — Unificación de servicios, Domain Services y controllers

**Pregunta clave:** ¿hay services duplicados o con mucho fan-in? ¿lógica pura atrapada en application layer?

**Respuesta:** no hay duplicación entre `NotificacionService`/`PersonasService`/`NotificacionGestor` — cada uno tiene una responsabilidad clara y no se pisan. No hay fan-in alto sobre ningún Service (Fase 0 §12). El único hallazgo de esta oleada es la limpieza de los campos muertos del constructor de `NotificacionService` (`personaRepository`, `sender`, nunca asignados con un valor real).

**RF de esta oleada:** `RF-08`.

**Acciones concretas:**
1. Eliminar `personaRepository` y `sender` de `NotificacionService` (campos y línea de autoasignación en el constructor) — no se usan en ningún método.
2. Confirmar con la suite existente que ningún test dependía (por accidente, vía reflection o similar) de esos campos — improbable, pero se verifica antes de cerrar.
3. Evaluar si conviene ponerle interfaz explícita a `EventoMapper`/`PersonaMapper`/`MedioDeContactoMapper` para mockear más limpio en tests — de bajo impacto, opcional, no bloqueante para cerrar la oleada.
4. Homogeneizar el uso de `@RequiredArgsConstructor` (Lombok) entre `NotificacionController` (ya lo usa) y `PersonasController` (no lo usa) — cosmético, se puede incluir acá o en Oleada 7.

**Fuera de scope:** unificar los prefijos de ruta REST (`/notificaciones` vs. `/api/notificaciones/personas`) — cambiar contratos HTTP existentes requiere coordinar con quien ya integra (`donaciones-service`, vía `NotificacionesFeignClient`), así que se documenta como pregunta abierta para el equipo, no se decide unilateralmente en esta oleada.

### Bitácora de ejecución — ✅ RF-08

#### Problema
`NotificacionService` declaraba `personaRepository`/`sender` inicializados en `null`, y el constructor los "autoasignaba" a sí mismos (`this.personaRepository = personaRepository`, refiriéndose al campo — el constructor solo recibe `repository`/`mapper`/`eventPublisher` como parámetros reales). Nunca toman un valor real ni se usan en ningún método de la clase. `PersonasController` construía a mano en vez de usar `@RequiredArgsConstructor` como ya hacía `NotificacionController`, inconsistencia menor de estilo entre los 2 controllers del servicio.

#### Evidencia
- `NotificacionService.java:19-20` (antes): `private IPersonaRepository personaRepository = null; private NotificacionSender sender = null;`
- `NotificacionService.java:29-30` (antes): `this.personaRepository = personaRepository; this.sender = sender;` — el constructor no declaraba esos parámetros, esas líneas son autoasignación del campo a sí mismo.
- `PersonasController.java` (antes): constructor manual de una línea, mientras `NotificacionController.java` ya usaba `@RequiredArgsConstructor`.

#### Confirmación previa a borrar (pedida explícitamente)
`grep -rn "personaRepository\|\bsender\b" NotificacionService.java NotificacionServiceTest.java` → 0 referencias en el test. `grep -rn "ReflectionTestUtils\|getDeclaredField\|setAccessible\|Field\.\|\.class\.getField" src` (todo el módulo) → 0 matches. Ningún test depende de esos campos por reflection ni de ninguna otra forma. Seguro para eliminar.

#### Objetivo
Eliminar los 2 campos muertos de `NotificacionService`; evaluar y decidir si conviene ponerle interfaz explícita a los 3 mappers; homogeneizar `@RequiredArgsConstructor` en `PersonasController`.

#### Decisión evaluada — interfaces explícitas para `EventoMapper`/`PersonaMapper`/`MedioDeContactoMapper`
**No se agregaron.** Razones:
1. Los tests ya mockean estas 3 clases concretas sin problema (`@Mock private EventoMapper mapper;` en `NotificacionServiceTest`, ya funcionando desde antes de esta oleada) — el proyecto usa el inline-mock-maker de Mockito (confirmado por el warning de auto-attach de Byte Buddy en la salida de tests), que mockea clases concretas sin necesitar que sean interfaces. El argumento de "para mockear más limpio" no aplica acá como aplicaría con una librería de mocking más limitada.
2. A diferencia de `NotificacionSender`/`CorreoAdapter`/`TelefonoAdapter`/`WhatsAppAdapter` (que sí son interfaces porque tienen múltiples implementaciones reales: el adapter real y el mock de infraestructura), estos 3 mappers tienen exactamente una implementación cada uno y no hay ningún caso de uso previsto de una segunda implementación intercambiable — la interfaz sería puramente ceremonial.
3. Es coherente con no "inventar por las dudas" abstracciones sin un consumidor real (mismo criterio aplicado en oleadas anteriores).

Documentado acá para que no se reabra la pregunta sin una razón nueva concreta (ej. si en el futuro aparece una segunda implementación real de algún mapper).

#### Qué se hizo
1. `NotificacionService.java`: eliminados `personaRepository`/`sender` (campos y las 2 líneas de autoasignación del constructor); el constructor ahora solo recibe y asigna `repository`/`mapper`/`eventPublisher`, sin agregar `@RequiredArgsConstructor` (no fue pedido para esta clase — se mantiene el constructor explícito para no ampliar el diff más allá de lo pedido).
2. `PersonasController.java`: `@RequiredArgsConstructor` (Lombok) en vez del constructor manual; mismo patrón que `NotificacionController`.
3. Sin cambios en `EventoMapper`/`PersonaMapper`/`MedioDeContactoMapper` (decisión de no agregar interfaces, documentada arriba).

#### 📝 Nota — pregunta abierta para el equipo (no decidida acá)
Prefijos de ruta REST inconsistentes entre los 2 controllers: `NotificacionController` usa `/notificaciones`, `PersonasController` usa `/api/notificaciones/personas`. Unificarlos cambiaría un contrato HTTP que ya consume `donaciones-service` (`NotificacionesFeignClient.sincronizarPersona`/`anonimizarPersona`/`enviarEvento`, con las rutas actuales hardcodeadas). Queda como pregunta abierta para el equipo — no se tocó ningún path en esta oleada.

#### Tests / Verificación
- No se agregaron tests nuevos: RF-08 es limpieza pura (eliminar código muerto + homogeneizar un constructor), sin cambio de comportamiento observable. Los tests existentes (`NotificacionServiceTest`, `PersonasControllerTest` con `@InjectMocks`) siguen pasando sin modificarse — `@InjectMocks` resuelve el constructor generado por Lombok igual que resolvía el manual.
- Barrido: `grep -n "personaRepository\|IPersonaRepository\|NotificacionSender" NotificacionService.java` → 0 matches.
- Suite `notificaciones-service`: **86 tests, 0 failures, 0 errors** (mismo número que al cierre de la Oleada 3 — no se esperaba ni hubo cambio de cantidad).
- Reactor completo: **627 tests, 0 failures, 0 errors** — `common-lib` 9, `donaciones-service` 309, `incentivos-service` 56, `logistica-service` 167, `notificaciones-service` 86.
- `spotless:check` en verde sin necesitar `spotless:apply` esta vez.

#### Diseño resultante
`NotificacionService` queda con exactamente los 3 colaboradores que usa. `PersonasController` y `NotificacionController` comparten el mismo estilo de inyección (`@RequiredArgsConstructor`). Los 3 mappers siguen siendo clases concretas sin interfaz, decisión explícita y documentada, no un descuido.

#### IA utilizada
Verificación mecánica (`grep`) de ausencia de reflection y de referencias de test antes de borrar los campos muertos; evaluación de la relación costo/beneficio de agregar interfaces a los 3 mappers, contrastando con el uso real de Mockito en el proyecto; homogeneización del constructor de `PersonasController`.

#### Verificación humana
- [x] Confirmado con `grep` (no asumido) que ningún test depende de `personaRepository`/`sender` por reflection u otra vía.
- [x] `personaRepository`/`sender` eliminados de `NotificacionService`.
- [x] Decisión sobre interfaces de mappers evaluada y documentada (no implementada).
- [x] `PersonasController` usa `@RequiredArgsConstructor`, igual que `NotificacionController`.
- [x] Prefijos de ruta REST no tocados; pregunta abierta documentada para el equipo.
- [x] `@Setter` de eventos (Oleada 3) y Bean Validation de DTOs (Oleada 9) no tocados.
- [x] Suite de `notificaciones-service` en verde (86/86).
- [x] Reactor completo en verde (627/627).

---

## Oleada 5 — Scheduling

**No aplica.** No hay procesos periódicos en este servicio (Fase 0 §9). Se omite.

---

## Oleada 6 — Reorganización de paquetes

**Pregunta clave:** ¿hay dominio puro en `infrastructure/`?

**Respuesta:** no — al revés, hay dos adapters técnicos (`NotificacionRepositoryEnMemoria`, `PersonaRepositoryEnMemoria`, ambos `@Repository`) viviendo bajo `models/repositories/impl/` en lugar de `infrastructure/`. Es un patrón replicado también en otros servicios del monorepo, así que mover solo el de notificaciones generaría inconsistencia con el resto — se documenta como candidato, pero se recomienda decidirlo a nivel de convención de todos los servicios, no solo acá.

**Acciones concretas (bajo impacto, opcionales para esta oleada):**
1. 📝 Documentar la posible reubicación de `models/repositories/impl/*` bajo `infrastructure/`, condicionada a que se adopte como convención transversal.
2. Ajustar el paquete de test `models/entities/medioDeContacto` para que refleje el paquete real de producción `models/entities/personas`.
3. Cero cambios funcionales esperados — solo estas dos reorganizaciones, si se decide ejecutarlas.

### Bitácora de ejecución — ✅ Oleada 6

#### Problema
Dos desalineamientos de paquete señalados en la Fase 0: (1) los `@Repository` en memoria de notificaciones viven en `models/repositories/impl/` en vez de `infrastructure/`, y (2) `MedioDeContactoTest.java` vive en un paquete de test (`models/entities/medioDeContacto`) que no refleja el paquete real de producción (`models/entities/personas`).

#### Evidencia y verificación previa (pedida antes de decidir)
`grep -rl "@Repository"` en los 4 servicios del monorepo → **los 4** (`donaciones`, `incentivos`, `logistica`, `notificaciones`) tienen sus `@Repository` bajo `models/repositories/impl/` (o `models/repositories/` directo en incentivos). Ninguno los movió a `infrastructure/`. Confirmado con evidencia, no asumido.

#### Objetivo
Mover `MedioDeContactoTest.java` al paquete correcto; dejar documentado (no ejecutado) el candidato de mover los repos a `infrastructure/`.

#### Decisión — repos `@Repository` NO se movieron
Confirmado que los 4 servicios comparten la misma convención (`models/repositories/impl/`). Mover solo el de `notificaciones-service` generaría la única excepción del monorepo, exactamente lo que el pedido pidió evitar. Queda como candidato **condicionado a una convención transversal**: si en algún momento el equipo decide adoptar `infrastructure/` para los 4 servicios, `NotificacionRepositoryEnMemoria`/`PersonaRepositoryEnMemoria` se mueven junto con el resto, no antes.

#### Qué se hizo
1. `MedioDeContactoTest.java`: movido de `models/entities/medioDeContacto` a `models/entities/personas` (mismo paquete que `Correo`/`Telefono`/`TipoTelefono`, que ya prueba). Cambiado el `package` del archivo; los imports de `Correo`/`Telefono`/`TipoTelefono` se eliminaron porque ahora son del mismo paquete (ya no hace falta importarlos) — es la actualización mecánica de imports que pedía el alcance, no un cambio de comportamiento.
2. Directorio `models/entities/medioDeContacto` (test) eliminado por quedar vacío.
3. Cero cambios de firma de método, cero cambios de comportamiento — confirmado que ningún otro archivo referenciaba el paquete viejo (`grep -rn "models.entities.medioDeContacto"` → 0 matches) antes de dar la oleada por cerrada.

#### Fuera de scope
- Reorganización de `models/repositories/impl/` → `infrastructure/`: no ejecutada (ver decisión arriba).
- Cualquier cambio funcional: no hubo ninguno, es la oleada explícitamente "cero cambios funcionales".

#### Tests / Verificación
- Nota de proceso: la primera corrida de `mvn test` (sin `clean`) mostró 97 tests en `notificaciones-service` en vez de 86 — Maven no había purgado el reporte viejo (`grupo5....medioDeContacto.MedioDeContactoTest.txt`) de `target/surefire-reports/`, y mi barrido de conteo sumó el reporte viejo (stale) más el nuevo. Se corrigió corriendo `mvn clean test`: con `target/` limpio, el conteo real es 86 (idéntico al cierre de la Oleada 4), confirmando que la oleada no agregó ni perdió ningún test, solo lo reubicó. Se deja esta nota para que quede claro que el 97 no era un error del código, sino un artefacto de build no limpiado.
- Suite `notificaciones-service` (con `clean`): **86 tests, 0 failures, 0 errors** — mismo número que el cierre de la Oleada 4.
- Reactor completo (con `clean`): **627 tests, 0 failures, 0 errors** — `common-lib` 9, `donaciones-service` 309, `incentivos-service` 56, `logistica-service` 167, `notificaciones-service` 86.
- Ningún test se modificó más allá del `package`/imports de `MedioDeContactoTest.java`.

#### Diseño resultante
El paquete de test de `MedioDeContacto`/`Correo`/`Telefono` ahora refleja el paquete real de producción. La estructura de `infrastructure/` vs. `models/repositories/impl/` queda sin cambios, consistente con el resto del monorepo.

#### IA utilizada
Verificación mecánica (`grep`) de la convención real de los 4 servicios antes de decidir no mover los repos; movimiento del archivo de test y limpieza de imports; detección y explicación del artefacto de build stale en el primer conteo de tests.

#### Verificación humana
- [x] Convención de `@Repository` verificada en los 4 servicios antes de decidir (no se asumió).
- [x] `MedioDeContactoTest.java` en el paquete correcto (`models/entities/personas`).
- [x] Directorio viejo eliminado, 0 referencias colgantes al paquete anterior.
- [x] 0 cambios de firma de método o de comportamiento.
- [x] Suite de `notificaciones-service` en verde (86/86, confirmado con `clean`).
- [x] Reactor completo en verde (627/627, confirmado con `clean`).

---

## Oleada 7 — Limpieza legacy y pureza de persistencia

**Foco A — Persistencia pura:** ya cumplido; los repositorios operan sobre `Notificacion`/`Persona` (entidades), no sobre DTOs.

**Foco B — Domain services puros:** no hay Domain Services en este servicio (no se detectó ningún algoritmo de negocio con estado `static` que convertir). Sin acción.

**Foco C — Declaratividad y naming:** `PersonasController` sin `@RequiredArgsConstructor` (si no se resolvió en Oleada 4); ningún fixture con sufijo `*Test` indebido detectado (no hay Object Mothers todavía — ver Oleada 8).

**Foco D — Limpieza de código:**
1. Wildcard imports propios del servicio: `EventoMapper.java` (`dto.input.*`, `...eventos.*`) → expandir a imports explícitos. Los 2 wildcard de framework en los controllers (`org.springframework.web.bind.annotation.*`) son un patrón común en controllers Spring MVC del propio monorepo — decidir si el barrido de cierre los incluye o los excepciona explícitamente.
2. Confirmar (ya verificado en Fase 0): 0 comentarios residuales de versión informal (`XMejorado`), 0 `instanceof`.

**Barrido mecánico de cierre propuesto:**
```
grep -rn "import .*\.\*;" services/mappers/          → 0 matches (hoy: 2, en EventoMapper)
find src/test -name "*Tests.java"                     → 0 matches (ya en 0)
grep -rnE "@(Component|Autowired|Qualifier|Value)" models/entities/ → 0 matches (ya en 0)
```

### Bitácora de ejecución — ✅ Oleada 7

#### Problema
`EventoMapper.java` tenía 2 wildcard imports de código propio del servicio (`dto.input.*`, `...eventos.*`) — el resto de los focos de esta oleada (Persistencia pura, Domain Services puros) ya estaban resueltos de antes, y `PersonasController` ya tenía `@RequiredArgsConstructor` desde la Oleada 4.

#### Evidencia
- `EventoMapper.java:5-6` (antes): `import grupo5.notificaciones.dto.input.*;` / `import grupo5.notificaciones.models.entities.notificaciones.eventos.*;`.

#### Objetivo
Wildcard imports de `EventoMapper.java` a imports explícitos; decidir y documentar el criterio sobre los wildcard imports de framework en los controllers; reconfirmar `instanceof`/comentarios de versión informal; confirmar `@RequiredArgsConstructor` en `PersonasController`.

#### Decisión — wildcard imports de framework en los controllers: SE EXCEPTÚAN del barrido
`grep` sobre **los 4 servicios del monorepo** muestra `import org.springframework.web.bind.annotation.*;` en **17 archivos** (10 en `donaciones-service`, 2 en `incentivos-service`, 3 en `logistica-service`, 2 en `notificaciones-service` — incluyendo `NotificacionController`, que ya lo tenía desde antes de este refactor). Es la convención establecida de todos los controllers Spring MVC del monorepo, no un descuido puntual de `notificaciones-service`. **Se decide excepcionar del barrido de "0 wildcard imports" cualquier import wildcard de un paquete de terceros/framework (`org.springframework.*`, etc.); el barrido de "0 wildcard imports" aplica solo a imports wildcard de código propio (`grupo5.*`).** Corregir solo los 2 de notificaciones sería la única inconsistencia del monorepo en sentido inverso (los otros 3 servicios seguirían con el wildcard). Se documenta acá para que esta duda no se reabra en una oleada futura — el criterio es explícito y transversal, no ad-hoc.

#### Reconfirmaciones pedidas (Fase 0 → hoy)
- **`instanceof`:** el barrido de Fase 0 (`grep -rn "instanceof" .`) se corrió scoped a `src/main` y dio 0 — sigue siendo así. Hallazgo nuevo, fuera de ese scope original: `PersonaMapperTest.java:41` tiene `assertTrue(entity.getMediosDeContacto().get(0) instanceof Correo)`, un `instanceof` de **test**, no de dominio — es una aserción de tipo idiomática (equivalente a `assertInstanceOf`, que ya se usa en otros tests del mismo módulo, ej. `EventoMapperTest`). No es una violación de "cero `instanceof`/casteo en el dominio" (ese principio es sobre lógica de producción despachando por subtipo, no sobre aserciones de test). No se tocó, porque no fue lo que se pidió reconfirmar/corregir en esta oleada (que hablaba del módulo en el sentido de Fase 0, es decir `src/main`) — se deja anotado como nit de estilo trivial y de bajísimo riesgo para quien haga limpieza de tests en Oleada 8, no se mezcló en este commit.
- **Comentarios de versión informal (`XMejorado`, etc.):** `grep` de nombres de clase con sufijos `Mejorado/Viejo/Antiguo/Old/V2/Nuevo` → 0 matches. Confirmado.
- **`@RequiredArgsConstructor` en `PersonasController`:** ya resuelto en la Oleada 4, reconfirmado con `grep` — sigue presente, no hizo falta ningún cambio acá.

#### Hallazgo adicional, documentado pero no corregido (fuera del alcance pedido)
Además de `EventoMapper.java`, hay wildcard imports de código propio (`grupo5.*`) en **3 archivos de test**: `NotificacionRouterTest.java` (`models.entities.personas.*`), `NotificacionControllerTest.java` (`dto.input.*`), `EventoMapperTest.java` (`dto.input.*` + `eventos.*`). El barrido pedido para esta oleada estaba scoped a `services/mappers/` (producción) — estos 3 son de test y no estaban en ese scope. Se documentan para una futura pasada de limpieza de tests (Oleada 8), no se tocaron acá para no ampliar el diff más allá de lo pedido.

#### Qué se hizo
1. `EventoMapper.java`: los 2 wildcard imports expandidos a 17 imports explícitos (9 de `dto.input.*`, 8 de `...eventos.*` — hay 9 clases en cada paquete referenciadas, incluyendo las interfaces/abstractas `EventoNotificableDTO`/`EventoNotificable`).
2. Ningún otro archivo de producción tocado — Focos A, B y la parte de `@RequiredArgsConstructor` de Foco C ya estaban resueltos de antes.

#### Fuera de scope
- Los 3 wildcard imports de test (`grupo5.*`) — documentados arriba, no corregidos.
- El `instanceof` de test en `PersonaMapperTest.java` — documentado arriba, no corregido.
- Los 17 wildcard imports de framework en controllers — exceptuados por decisión explícita, no "fuera de scope por descuido" sino por criterio documentado.

#### Tests / Verificación
- Barrido pedido: `grep -rn "import .*\.\*;" services/mappers/` → 0 matches (antes: 2, ambos en `EventoMapper`).
- Barrido pedido: `grep -rnE "@(Component|Autowired|Qualifier|Value)" models/entities/` → 0 matches (ya estaba en 0).
- Sin tests nuevos ni modificados: es una reorganización de imports pura, cero cambio de comportamiento.
- Suite `notificaciones-service` (con `clean`): **86 tests, 0 failures, 0 errors** — mismo número que el cierre de la Oleada 6.
- Reactor completo (con `clean`): **627 tests, 0 failures, 0 errors** — `common-lib` 9, `donaciones-service` 309, `incentivos-service` 56, `logistica-service` 167, `notificaciones-service` 86.

#### Diseño resultante
`EventoMapper.java` deja explícito de qué depende exactamente, sin wildcard imports de código propio. Los controllers mantienen el wildcard de framework por convención transversal ya documentada, sin ambigüedad para oleadas futuras.

#### IA utilizada
`grep` sobre los 4 servicios para fundamentar la decisión de excepcionar wildcards de framework (en vez de asumirlo); expansión mecánica de los 2 wildcard imports a explícitos; reconfirmación de `instanceof`/naming/`@RequiredArgsConstructor` contra la Fase 0, incluyendo la precisión del scope original (`src/main` vs. todo el módulo) para no reportar un falso hallazgo.

#### Verificación humana
- [x] `EventoMapper.java` sin wildcard imports propios.
- [x] Decisión sobre wildcards de framework en controllers tomada y documentada con evidencia de los 4 servicios (17 archivos), no unilateral sin sustento.
- [x] `instanceof`/comentarios de versión informal reconfirmados contra Fase 0, con la precisión de scope (`src/main`) explicitada.
- [x] `PersonasController` con `@RequiredArgsConstructor` (ya desde Oleada 4).
- [x] Los 2 barridos mecánicos pedidos en 0.
- [x] Suite de `notificaciones-service` en verde (86/86, con `clean`).
- [x] Reactor completo en verde (627/627, con `clean`).

---

## Oleada 8 — Refactor profundo de testing

**Pregunta clave:** ¿los tests están acoplados a constructores internos? ¿hay magic strings en aserciones? ¿qué casos borde faltan cubrir?

**Respuesta:** no se detectaron magic strings duplicados llamativos en las aserciones existentes (a confirmar con una revisión línea por línea al ejecutar la oleada). El hueco real es de **cobertura**, no de acoplamiento:

**Acciones concretas:**
1. Crear `NotificacionTest.java` dedicado (hoy no existe — Fase 0 §10): cubrir `notificar()` (éxito con el primer medio, fallback al segundo medio si el primero lanza excepción o devuelve `false`, `FALLIDA` cuando todos fallan, `FALLIDA` inmediata si `persona == null`), `ordenarMedios()` (predeterminado primero), y que `historialEstado` acumule cada transición con el `timestamp` correcto.
2. Test dedicado de `CambioEstadoNotificacion` (construcción, inmutabilidad de sus 3 campos `final`).
3. Catálogo de casos borde a cubrir explícitamente para este dominio:
   - `esPredeterminado == null` en algún medio de la lista (hoy no probado — riesgo de NPE real, ver Oleada 9.5).
   - Persona con 0, 1 y N medios de contacto.
   - Los 4 medios de contacto del mismo tipo (`ESTANDAR` y `WHATSAPP`) marcados simultáneamente como predeterminados (estado inconsistente que la propia ADR de "Medios de Contacto" advertía como riesgo posible).
   - `EntregaFallida.generarNotificaciones()` con y sin `replanificable` (ya cubierto parcialmente por `EventosTest`, confirmar explícitamente el mensaje al admin, hoy solo alcanzable vía `armarMensajeAdmin()` privado).
4. Introducir Object Mothers (`PersonaMother`, `NotificacionMother`, `EventoNotificableMother` o uno por subtipo) para reducir la construcción manual repetida en los tests existentes — nombrarlos `*Mother`, nunca `*Test`.
5. Mover `MedioDeContactoTest` al paquete que refleje `models/entities/personas` (si no se hizo en Oleada 6).

### Bitácora de ejecución — ✅ Oleada 8

#### Corrección de premisa (con evidencia, antes de tocar nada)
El pedido decía "crear `NotificacionTest.java` dedicado (hoy no existe)" y "crear un test dedicado para `MedioDeContactoMapper` (hoy no existe ninguno)". **Los dos ya existían** — se crearon en la Oleada 2 (`NotificacionTest.java`, 8 tests, scoped a domain events) y en la Oleada 1/3 (`MedioDeContactoMapperTest.java`, 5 tests, scoped a `esPredeterminado` y excepciones RF-05). No se crearon archivos nuevos por error de duplicar — se **extendieron** los dos archivos existentes con exactamente la cobertura que pedía esta oleada (`notificar()`, `ordenarMedios()`, timestamps, y los caminos felices de `MedioDeContactoMapper` que sí faltaban).

#### Problema
`Notificacion.notificar()`/`ordenarMedios()` no tenían ningún test que los ejercitara directamente (solo indirectamente vía `NotificacionRouterTest`/`NotificacionGestorTest`/`NotificacionServiceTest`, a través del Router real). `CambioEstadoNotificacion` no tenía getters ni test. `MedioDeContactoMapper` no tenía cobertura de los caminos felices de `TELEFONO`/`WHATSAPP` en `toEntity()` ni de ningún caso de `toReplicaDTO()`. `EntregaFallida.armarMensajeAdmin()` solo era alcanzable indirectamente. Los 2 casos borde de la Fase 0 (`esPredeterminado == null`, empate de predeterminados) no tenían ningún test que los documentara. `Notificacion.anonimizar()` — encontrado al revisar la cobertura completa de la clase, no listado en el pedido original — tampoco tenía ningún test.

#### Objetivo
Cerrar los huecos de cobertura listados arriba sin cambiar comportamiento de producción, salvo el mínimo necesario para que `CambioEstadoNotificacion` sea testeable.

#### Cambio de producción mínimo (fuera de lo estrictamente "solo testing", justificado)
`CambioEstadoNotificacion` no tenía ningún getter — imposible escribir un test de "construcción e inmutabilidad de sus 3 campos" sin poder leerlos. Se le agregó `@Getter` (Lombok). Sin este cambio, el punto 2 del pedido ("test dedicado de `CambioEstadoNotificacion`... inmutabilidad de sus 3 campos") no se podía cumplir. La inmutabilidad en sí no se "testea" con una aserción activa (son `final`, sin setters — no hay nada que un test pueda intentar mutar sin recurrir a reflection, que se evita); se deja constancia leyendo los 3 valores tal cual quedaron seteados, y documentado explícitamente en el Javadoc del test.

#### Qué se hizo
1. **`CambioEstadoNotificacion.java`:** agregado `@Getter`.
2. **`CambioEstadoNotificacionTest.java`** (nuevo, no existía): 2 tests — construcción con los 3 campos, y el caso real de `estadoAnterior == null` (primera transición).
3. **`NotificacionTest.java`** (extendido, de 8 a 21 tests): `notificar()` (5: éxito primer medio, fallback por excepción, fallback por `false`, todos fallan, persona nula sin consultar al sender); `ordenarMedios()` (4: predeterminado 2do agregado queda primero, 0 medios, 1 medio, N=3 medios); casos borde (2: `esPredeterminado == null` → `NullPointerException` documentado como característico, no corregido — RF-07/Oleada 9.5; dos `Telefono` del mismo family (`ESTANDAR`/`WHATSAPP`) ambos predeterminados → no explota, orden de alta preservado por sort estable); `historialEstado` con timestamps dentro de la ventana de ejecución y no decrecientes (1); `anonimizar()` (1, hueco encontrado no listado en el pedido).
4. **`EventosTest.java`** (extendido, de 11 a 13 tests): `EntregaFallida.generarNotificaciones()` con `replanificable=true` y `replanificable=false`, confirmando el mensaje completo al admin (antes solo alcanzable indirectamente) y el sufijo de los mensajes a donante/beneficiario.
5. **`MedioDeContactoMapperTest.java`** (extendido, de 5 a 13 tests): `toEntity()` con `TELEFONO`/`WHATSAPP` (2, no existía ninguno), insensibilidad a mayúsculas del `tipo` (1), `toReplicaDTO()` con `Correo`/`Telefono` ESTÁNDAR/`Telefono` WHATSAPP (3, no existía ninguno), `toEntity`/`toReplicaDTO` con `null` (2).
6. **Object Mothers nuevos** (paquete `grupo5.notificaciones.mothers`, ninguno con sufijo `*Test`):
   - `PersonaMother`: `generica()`, `sinMedios()`, `conUnMedioPredeterminado()`, `conMedios(MedioDeContacto...)`.
   - `MedioDeContactoMother`: `correo()`, `correoPredeterminado()`, `telefono(TipoTelefono)`, `telefonoPredeterminado(TipoTelefono)`, `correoConEsPredeterminadoNulo()` (subtipo anónimo que sobreescribe el getter, no reflection — necesario para el caso borde de NPE).
   - `NotificacionMother`: `pendiente(Persona)`, `pendiente(Persona, String)`.
   - `EventoNotificableMother` (genérico, no uno por subtipo — decisión explícita, ver abajo): `entregaFallida(...)`, `donanteInactivo(...)`.
   - Todos consumidos por los tests nuevos de esta oleada, no quedaron decorativos.
7. Confirmado (sin acción): `MedioDeContactoTest.java` ya estaba en `models/entities/personas` desde la Oleada 6.

#### Decisión — un solo Object Mother genérico para la jerarquía de eventos, no uno por subtipo
La jerarquía tiene 8 subclases concretas. Se decidió un único `EventoNotificableMother` con métodos agregados a medida que un test los necesita (hoy: `entregaFallida`, `donanteInactivo`), en vez de 8 archivos `*Mother` de antemano. Las 6 subclases restantes se siguen construyendo directamente en los tests existentes (`EventosTest.java`), que ya lo hacían antes de esta oleada sin problema — no había ninguna repetición ahí que justificara una mother dedicada por clase. Si una oleada futura agrega muchos más tests sobre un subtipo puntual, ahí se evalúa si conviene su propio mother.

#### Fuera de scope
- RF-07 (guard de `esPredeterminado == null`) y el desempate determinista — **no se corrigieron**, solo se documentaron con un test que caracteriza el comportamiento actual (con bug). Cuando se ejecute RF-07/Oleada 9.5, estos 2 tests van a necesitar actualizarse para reflejar el comportamiento corregido, no borrarse.
- Magic strings en aserciones existentes: se revisó al pasar por los archivos tocados, no se encontró ninguno llamativo; no se hizo una auditoría exhaustiva de archivos no tocados en esta oleada (fuera del foco pedido).

#### Tests / Verificación
- Suite `notificaciones-service` (con `clean`): **111 tests, 0 failures, 0 errors** (86 antes de esta oleada + 25 nuevos: 12 en `NotificacionTest` + 1 de `anonimizar()` + 2 en `CambioEstadoNotificacionTest` + 2 en `EventosTest` + 8 en `MedioDeContactoMapperTest`).
- `mvn spotless:apply` corrido una vez (reordenamiento de imports y wrapping de líneas en los archivos nuevos/extendidos, sin cambios semánticos).
- Reactor completo (con `clean`): **652 tests, 0 failures, 0 errors** — `common-lib` 9, `donaciones-service` 309, `incentivos-service` 56, `logistica-service` 167, `notificaciones-service` 111.

#### Diseño resultante
`Notificacion` queda con cobertura directa (no solo indirecta vía Router/Gestor/Service) de sus 3 métodos de comportamiento (`notificar`, `ordenarMedios`, `anonimizar`) y de la acumulación de su historial. `CambioEstadoNotificacion` es observable y tiene su propio test. `MedioDeContactoMapper` tiene los 9 caminos posibles (3 tipos × entrada/salida, más 2 defaults, más 2 nulls) cubiertos. Dos riesgos reales de la Fase 0 (NPE por `esPredeterminado == null`, empate de predeterminados) pasaron de estar solo documentados en un `.md` a estar caracterizados por un test ejecutable.

#### IA utilizada
Verificación de que `NotificacionTest.java`/`MedioDeContactoMapperTest.java` ya existían antes de crear nada nuevo (evitar duplicar); diseño e implementación de los 4 Object Mothers y de los ~25 tests nuevos; detección del hueco de `anonimizar()` no listado en el pedido; decisión de mother único vs. uno por subtipo, documentada con su razón.

#### Verificación humana
- [x] Premisa del pedido ("hoy no existe") verificada contra el código antes de actuar, corregida con evidencia.
- [x] `notificar()`, `ordenarMedios()`, `historialEstado` con timestamps: cubiertos en `NotificacionTest.java`.
- [x] `CambioEstadoNotificacion`: test dedicado nuevo; `@Getter` agregado como cambio mínimo justificado.
- [x] Los 4 casos borde pedidos: cubiertos (NPE de `esPredeterminado`, 0/1/N medios, empate de predeterminados, `EntregaFallida` con/sin `replanificable`).
- [x] Object Mothers con sufijo `*Mother`, ninguno `*Test`, todos consumidos por tests reales.
- [x] `MedioDeContactoTest.java` reconfirmado en el paquete correcto.
- [x] Suite de `notificaciones-service` en verde (111/111, con `clean`).
- [x] Reactor completo en verde (652/652, con `clean`).

---

## Oleada 9 — Validación por capas, HTTP clásico y trazabilidad

**Pregunta clave:** ¿los DTOs tienen validación? ¿las respuestas HTTP son consistentes? ¿el servicio aprovecha lo que `common-lib` ya expone?

**Respuesta:** cero anotaciones de Bean Validation en los 10 DTOs de entrada/réplica (Fase 0 §1, hallazgo #7) — brecha total, propia de este servicio. 2 códigos HTTP no estandarizados (Fase 0 §9). `common-lib` ya aporta `GlobalExceptionHandler` (cubre `MethodArgumentNotValidException`, `HandlerMethodValidationException`, `ConstraintViolationException`, `MissingRequestHeaderException`, `MissingServletRequestParameterException`, `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`, `DateTimeParseException`, `FeignException`, etc.) y trazabilidad (`TraceResponseHeaderFilter` + `FeignTraceRequestInterceptor`, autoconfigurados vía `LoggingAutoConfiguration`/`CommonLibAutoConfiguration`) — notificaciones ya los hereda sin configuración adicional; el trabajo real de esta oleada queda acotado a lo propio del servicio.

**RF de esta oleada:** `RF-09`.

**Acciones concretas:**
1. `@NotNull`/`@NotBlank`/`@Positive`/`@PastOrPresent` según corresponda en los 8 DTOs de `dto/input/*`, `PersonaReplicaDTO`, `MedioDeContactoReplicaDTO`.
2. `@Valid` en `NotificacionController.procesarEvento` y en `PersonasController.sincronizar`.
3. Ajustar códigos HTTP: `POST /notificaciones` → `201 Created` (o `202 Accepted`, a decidir según se considere el procesamiento síncrono o fire-and-forget desde la óptica del llamador); `DELETE /api/notificaciones/personas/{id}` → `204 No Content`.

### Bitácora de ejecución — ✅ RF-09

#### Problema
Los 10 DTOs de entrada/réplica no tenían ninguna anotación de Bean Validation; ningún controller usaba `@Valid`; `POST /notificaciones` y `DELETE /api/notificaciones/personas/{id}` devolvían `200 OK` en vez de un código semánticamente correcto.

#### Evidencia
- `grep -rE "@(NotNull|NotBlank|Positive|PastOrPresent)" notificaciones-service/src/main/java/grupo5/notificaciones/dto/` → 0 matches (antes).
- `NotificacionController.procesarEvento`/`PersonasController.sincronizar`: sin `@Valid` (antes).
- `NotificacionController.procesarEvento` → `ResponseEntity.ok().build()`; `PersonasController.anonimizar` → `ResponseEntity.ok().build()` (antes).
- `common-lib`'s `GlobalExceptionHandler` ya tenía `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` y `@ExceptionHandler(DateTimeParseException.class)` desde antes de esta oleada — confirmado con `grep -n "@ExceptionHandler" common-lib/.../GlobalExceptionHandler.java`, sin necesidad de tocar `common-lib`.

#### Objetivo
Cerrar `RF-09` sin invadir `common-lib` (ya tiene lo que hace falta) ni el resto del dominio.

#### Fuera de scope
- Los campos condicionales de `MedioDeContactoReplicaDTO` (`direccionCorreo`/`caracteristica`/`codigoArea`/`numero`) quedan sin anotación: son obligatorios según el `tipo` (un `CORREO` no tiene teléfono, un `TELEFONO`/`WHATSAPP` no tiene correo), y expresar "obligatorio si tipo == X" requiere un validador cruzado a medida que no se pidió en esta oleada. Ese caso puntual de formato por tipo ya lo resuelve `MedioDeContactoMapper` (RF-05, Oleada 3). Documentado con un comentario en el propio DTO para que no se lea como un olvido.
- `esPredeterminado: Boolean` en `MedioDeContactoReplicaDTO` queda sin `@NotNull`: `null` es un valor válido y manejado explícitamente desde la Oleada 1 (`MedioDeContactoMapper` lo trata como "no predeterminado").
- No se tocó `common-lib` — el hallazgo original sobre `MethodArgumentTypeMismatchException`/`DateTimeParseException` era incorrecto (ver Evidencia); no había nada que coordinar.

#### Qué se hizo
1. Agregada la dependencia explícita `spring-boot-starter-validation` al `pom.xml` de `notificaciones-service` — ya llegaba transitivamente vía `common-lib` (que sí la declara), pero se la deja explícita siguiendo el mismo patrón que `logistica-service` (que también la declara aunque dependa de `common-lib`), para no depender silenciosamente de una transitividad.
2. Bean Validation en los 8 DTOs de `dto/input/*`: `idPersonaDonante`/`idPersonaBeneficiaria`/`idPersonaAdmin` → `@NotNull`; `fecha` → `@NotNull @PastOrPresent`; `detalleDonacion`/`enlaceSeguimiento`/`patenteCamion`/`credencialesDeAcceso`/`motivo`/`nombreMision`/`recompensa`/`categoriaNueva`/`categoriaVieja` → `@NotBlank`; `diasInactivo` → `@NotNull @Positive`; `replanificable` queda sin anotación (es `boolean` primitivo, no puede ser `null`). Mensajes en español, mismo estilo que `donaciones-service`/`incentivos-service` (`@NotNull(message = "...")`).
3. `PersonaReplicaDTO`: `id`/`tipoPersona` → `@NotNull`; `denominacion` → `@NotBlank`; `mediosDeContacto` → `List<@Valid MedioDeContactoReplicaDTO>` (cascada de validación sobre cada medio, mismo patrón que `HumanaInputDTO.mediosDeContacto` en `donaciones-service`).
4. `MedioDeContactoReplicaDTO`: solo `tipo` → `@NotBlank` (ver Fuera de scope para el resto).
5. `@Valid` agregado en `NotificacionController.procesarEvento(@Valid @RequestBody EventoNotificableDTO dto)` y `PersonasController.sincronizar(@Valid @RequestBody PersonaReplicaDTO dto)`.
6. `POST /notificaciones` → `202 Accepted`: se descartó `201 Created` porque `evento.generarNotificaciones()` puede devolver 0..N `Notificacion` (ej. `EntregaFallida` genera notificación al admin y al donante) — no hay un único recurso/Location que devolver: el endpoint acepta un evento de dominio, no crea "un" recurso identificable de punta a punta.
7. `DELETE /api/notificaciones/personas/{id}` → `204 No Content` (`ResponseEntity.noContent().build()`).
8. Tests actualizados: `NotificacionControllerTest` (6 aserciones `isOk()` → `isAccepted()`, métodos renombrados a `..._deberiaResponderAceptadoY...`); `PersonasControllerTest` (`anonimizar_deberiaRetornarStatusOk` → `anonimizar_deberiaRetornarNoContent`, `isOk()` → `isNoContent()`; agregado `.setControllerAdvice(new GlobalExceptionHandler())` en el `standaloneSetup`, porque a diferencia de `NotificacionControllerTest` — que usa `@WebMvcTest` y levanta el `GlobalExceptionHandler` autoconfigurado — el `standaloneSetup` no registra automáticamente ningún `@ControllerAdvice`; mismo patrón que `RutasControllerTest` en `logistica-service`).
9. Tests nuevos: `NotificacionControllerTest.procesarEvento_conDetalleDonacionEnBlanco_deberiaResponderBadRequest` y `.obtenerPorPersona_conPersonaIdMalformado_deberiaResponderBadRequest`; `PersonasControllerTest.sincronizar_conDenominacionEnBlanco_deberiaResponderBadRequest`, `.anonimizar_conIdMalformado_deberiaResponderBadRequest`, `.obtenerPersona_conIdMalformado_deberiaResponderBadRequest` — los 3 últimos confirman en runtime (no solo por lectura de código) que un UUID malformado en un `@PathVariable` ya devuelve `400` vía el `GlobalExceptionHandler` de `common-lib`, sin cambios ahí.
10. `mvn spotless:apply` sobre `notificaciones-service` para corregir el wrapping de una línea nueva — sin cambios semánticos.

#### Tests / Verificación
- Suite `notificaciones-service` (con `clean`): **116 tests, 0 failures, 0 errors** (111 antes de esta oleada + 5 tests nuevos).
- Reactor completo (con `clean`): pendiente de confirmar en esta misma corrida (ver cierre de la respuesta).
- Confirmado en el log de ejecución: `MethodArgumentNotValidException` y `MethodArgumentTypeMismatchException` resueltos por `GlobalExceptionHandler` sin ningún cambio en `common-lib` (líneas de log `[EXCEPTION: MethodArgumentTypeMismatchException]`/`[EXCEPTION: MethodArgumentNotValidException]` con código `ERR-CSR-003`, status 400).

#### Diseño resultante
Los 10 DTOs de entrada validan sus invariantes de formato antes de llegar a la capa de servicios; los 2 controllers devuelven códigos HTTP semánticamente correctos (`202`/`204`); no se tocó `common-lib` porque ya tenía todo lo necesario.

#### IA utilizada
Relectura de los DTOs y de `NotificacionService.procesar()` para decidir `202` vs `201` con evidencia (cardinalidad 0..N de `generarNotificaciones()`); búsqueda del patrón de estilo ya usado en `donaciones-service`/`incentivos-service` para las anotaciones (mensajes en español, `List<@Valid X>`) en vez de inventar un estilo nuevo; confirmación en runtime (no solo estática) de que `common-lib` ya resuelve `MethodArgumentTypeMismatchException`.

#### Verificación humana
- [x] Los 10 DTOs con Bean Validation (8 de `dto/input/*` + `PersonaReplicaDTO` + `MedioDeContactoReplicaDTO`).
- [x] `@Valid` en los 2 controllers.
- [x] `POST /notificaciones` → `202`, `DELETE /api/notificaciones/personas/{id}` → `204`, decisión documentada con motivo.
- [x] Confirmado en runtime que un UUID malformado devuelve `400` sin tocar `common-lib`.
- [x] Suite de `notificaciones-service` en verde (116/116, con `clean`).
- [x] Reactor completo en verde (892/892, con `clean`): `common-lib` 32, `donaciones-service` 394, `incentivos-service` 189, `logistica-service` 161, `notificaciones-service` 116.

---

## Oleada 9.5 — Hardening de bordes temporales, concurrencia e idempotencia

**Chequeos aplicables a este servicio** (de los 8 del catálogo genérico, con lo que la Fase 0 encontró):

1. **Falsos positivos por ausencia de dato:** no aplica — este servicio no tiene predicados de "¿pasó X?" basados en `null` (ese patrón vive en incentivos/donantes, no acá).
2. **Degradación destructiva ante eventos fuera de orden:** no aplica directamente — `Notificacion.actualizarEstado()` no revierte estados ya avanzados (`ENVIADA`/`FALLIDA` son terminales, `notificar()` no se vuelve a invocar sobre una notificación ya resuelta en el flujo actual). Verificar en la práctica que `NotificacionGestor.notificarPendientes()` solo toma las `PENDIENTE` (ya así hoy) y documentar que esto ya cumple el criterio.
3. **Propagación de trazabilidad en hilos asíncronos:** no aplica todavía — no hay `@Async` dentro de `notificaciones-service` (el `@Async` vive del lado del llamador, en `donaciones-service`). Si se agrega alguno en el futuro, aplicar `TaskDecorator`.
4. **Executor acotado con backpressure:** no aplica — no hay `@EnableAsync` en este servicio.
5. **Desempate determinista:** **sí aplica.** `Notificacion.ordenarMedios()` usa `Comparator.comparing(MedioDeContacto::getEsPredeterminado).reversed()` sin criterio secundario — si dos medios empatan en `esPredeterminado`, el orden queda a criterio del orden de iteración incidental de la lista interna de `Persona`. Definir un criterio de desempate explícito (ej. por tipo de medio, o por orden de alta) — parte de `RF-07`.
6. **Idempotencia en reprocesamiento:** **sí aplica, es el hallazgo de mayor riesgo de esta oleada.** `NotificacionesFeignClient` (lado `donaciones-service`) usa `FeignRetryConfig`; ningún DTO de entrada lleva una clave de correlación, y `generarNotificaciones()` crea instancias nuevas cada vez sin deduplicar. Un reintento de red genera notificaciones duplicadas. Es `RF-10` — requiere coordinación con los servicios emisores (cruza el límite de este servicio).
7. **Residuos de introspección post-refactor:** revisar, tras Oleada 3, que ningún Application Service quedó accediendo directamente a `mediosDeContacto`/`domainEvents` en lugar de pasar por el método semántico del agregado.
8. **Consistencia semántica texto↔código:** revisar la ADR de estado de notificaciones ("queda en pendiente si no hay conexión y luego se reintenta") contra el código real (hoy no hay reintento automático, solo fallback entre medios en el mismo intento) — dejar la ADR corregida o marcada como "no implementado todavía" (📝, documental).

**Además, hallazgo propio no listado en el catálogo genérico:** `esPredeterminado: Boolean` (objeto, nullable) combinado con `Comparator.comparing(MedioDeContacto::getEsPredeterminado)` puede lanzar `NullPointerException` en tiempo de ejecución si el valor llega `null` desde `MedioDeContactoMapper` (el DTO `MedioDeContactoReplicaDTO.esPredeterminado()` es `Boolean`, no `boolean`). Es `RF-07`.

### Bitácora de ejecución — ✅ RF-07 / 📝 RF-10

#### Problema
De los 4 puntos que sí aplican: el guard de `esPredeterminado == null` era un bug real de ejecución sin corregir; los otros 3 eran, en distinta medida, verificaciones — dos de ellas (idempotencia y consistencia de la ADR) requerían reconfirmar evidencia antes de documentarlas, no darlas por sentado.

#### Evidencia
- `Notificacion.ordenarMedios()` (antes): `Comparator.comparing(MedioDeContacto::getEsPredeterminado).reversed()` — sin guard. Confirmado que `NullPointerException` era alcanzable en la suite solo de forma forzada: `MedioDeContactoMother.correoConEsPredeterminadoNulo()` construye el `null` con un subtipo anónimo que sobreescribe el getter, porque el constructor de `MedioDeContacto` ya deja `esPredeterminado = false` por defecto y sus únicos mutadores (`marcarComoPredeterminado()`/`desmarcarComoPredeterminado()`) nunca asignan `null` — desde que la Oleada 1 quitó el `@Setter`, no queda ningún camino de producción real que deje el campo en `null`. Igual se corrige: el contrato del getter (`Boolean`, nullable) lo sigue permitiendo, y es una guarda barata.
- `grep -rn "\.mediosDeContacto\b" notificaciones-service/src/main/java` → 0 matches fuera de `Persona.java` (el único uso externo, en `PersonaMapper.java`, es sobre `dto.mediosDeContacto()` del DTO o `entity.getMediosDeContacto()` — el getter semántico, no el campo). `grep -rn "\.domainEvents\b" ... ` → 0 matches fuera de `Notificacion.java`. Sin residuos de introspección.
- `docs/adr/notificaciones-service/20260520-estado-de-notificaciones.md`: el análisis de alternativas menciona un estado "Cancelada" que no existe (el enum real es `PENDIENTE`/`ENVIADA`/`FALLIDA`) y "queda en pendiente si no hay conexión y luego se reintenta", que no está implementado — `notificar()` pasa a `FALLIDA` de inmediato si todos los medios fallan en el mismo intento, sin reintento posterior.
- Idempotencia (reconfirmado en esta sesión, no asumido del hallazgo original): `donaciones-service/.../NotificacionesFeignClient.java` tiene `configuration = FeignRetryConfig.class`, que registra `new Retryer.Default(100, 2000, 5)` (hasta 5 reintentos) para `enviarEvento(EventoNotificableDTO dto)`. `incentivos-service` también tiene su propio `NotificacionesFeignClient`, pero sin `configuration` — usa el `Retryer` default de Feign (`NEVER_RETRY`), así que el riesgo de duplicados es específico del tráfico `donaciones-service` → `notificaciones-service`, no de `incentivos-service`. Ningún DTO de ninguno de los dos clientes lleva clave de correlación (`eventId`).

#### Objetivo
Corregir el guard de `ordenarMedios()` con un criterio de desempate explícito; confirmar (no asumir) el resto de los 4 chequeos; documentar RF-10 sin implementarlo.

#### Fuera de scope
- `RF-10` (idempotencia) no se implementó — cruza a `donaciones-service`, tal como pedía el prompt. Documentado abajo como propuesta, pendiente de coordinar.
- No se reabrieron los 4 chequeos que la Fase 0 ya había descartado (falsos positivos por null, degradación destructiva, trazabilidad en async, executor con backpressure) — se reconfirmó solo la ausencia de `@Async` (`grep -rn "@Async\|@EnableAsync" notificaciones-service/src/main/java` → 0 matches), sin evidencia nueva que amerite reabrir los otros 3.
- No se tocó el código de `donaciones-service`/`incentivos-service` — la verificación de sus `NotificacionesFeignClient` fue de solo lectura.

#### Qué se hizo
1. `Notificacion.ordenarMedios()`: comparador cambiado de `Comparator.comparing(MedioDeContacto::getEsPredeterminado)` a `Comparator.comparing((MedioDeContacto m) -> Boolean.TRUE.equals(m.getEsPredeterminado()))` — `Boolean.TRUE.equals(null)` devuelve `false` en vez de lanzar `NullPointerException`, tratando `null` como "no predeterminado" (mismo criterio que ya usa `MedioDeContactoMapper.toEntity()` para el mapeo inverso, ahora consistente en los dos sentidos).
2. Desempate explícito documentado (no un nuevo campo): `Stream.sorted()` está garantizado estable en Java, así que dos medios empatados en `esPredeterminado` conservan el orden de alta en `Persona.mediosDeContacto` — se deja explícito con un comentario en el propio método, en vez de dejarlo como un efecto colateral no documentado de la implementación del sort.
3. Test `ordenarMedios_conEsPredeterminadoNulo_deberiaLanzarNullPointerException` reescrito a `..._deberiaTratarloComoNoPredeterminado`: ya no espera la excepción, verifica que ambos medios (uno con `null`, uno con `false`) queden tratados como no predeterminados y conserven el orden de alta.
4. Javadoc de `MedioDeContactoMother.correoConEsPredeterminadoNulo()` actualizado: aclara que ese estado no es alcanzable vía `MedioDeContactoMapper` (siempre invoca un mutador), y que el Mother lo fuerza solo para ejercitar el guard, no para documentar un bug pendiente.
5. `docs/adr/notificaciones-service/20260520-estado-de-notificaciones.md`: agregada una sección "Nota de implementación" al final — corrige el nombre del estado terminal real (`FALLIDA`, no `Cancelada`) y marca 📝 explícitamente que el reintento automático no está implementado, explicando el comportamiento real de `notificar()` y aclarando que el `FeignRetryConfig` del lado de `donaciones-service` opera a nivel de la llamada HTTP completa, no de un medio de contacto puntual. La sección original ("Análisis de Alternativas") se dejó intacta como registro histórico de la decisión.
6. RF-10 (idempotencia): documentado en el punto 6 del catálogo de chequeos (arriba) con la reconfirmación de evidencia de esta sesión (ver Evidencia) — sin implementar nada del lado de `donaciones-service`. Propuesta a futuro: agregar `eventId: UUID` a `EventoNotificableDTO` (ambos lados, `donaciones-service` y `notificaciones-service`) y deduplicar en `NotificacionService.procesar()`/`EventoMapper` contra un registro de `eventId` ya procesados — requiere coordinación explícita antes de implementarse.
7. Punto 7 (residuos de introspección): verificado sin hallazgos — ver Evidencia.

#### Tests / Verificación
- Suite `notificaciones-service` (con `clean`): **116 tests, 0 failures, 0 errors** (mismo número que el cierre de la Oleada 9 — un test renombrado/reescrito, ninguno agregado ni perdido).
- Reactor completo (con `clean`): pendiente de confirmar en esta misma corrida (ver cierre de la respuesta).

#### Diseño resultante
`ordenarMedios()` ya no puede lanzar `NullPointerException` por un `esPredeterminado` nulo, con un criterio de desempate documentado explícitamente en vez de implícito. La ADR de estado de notificaciones queda alineada con el comportamiento real del código, sin perder el registro histórico de por qué se decidió el enum simple. `RF-10` queda como decisión pendiente y coordinada, no como una implementación apurada del lado equivocado del límite de servicio.

#### IA utilizada
Verificación de que el `NullPointerException` original solo era alcanzable de forma forzada (lectura de `MedioDeContacto`, `MedioDeContactoMapper` y el Mother existente) antes de decidir el alcance real del fix; relectura en runtime (no solo por nombre de archivo) de `NotificacionesFeignClient`/`FeignRetryConfig` en `donaciones-service` **e** `incentivos-service` para no asumir el hallazgo original sin reconfirmarlo, tal como pidió la usuaria; búsqueda de residuos de introspección con `grep` en vez de revisión manual archivo por archivo.

#### Verificación humana
- [x] Guard de `esPredeterminado == null` corregido, con test actualizado (no solo agregado uno nuevo al lado del viejo).
- [x] Criterio de desempate explícito y documentado.
- [x] Idempotencia (RF-10): reconfirmada con evidencia de esta sesión, no asumida — incluye el hallazgo adicional de que `incentivos-service` no comparte el riesgo (sin `FeignRetryConfig`).
- [x] Sin residuos de introspección post-Oleada 3.
- [x] ADR de estado de notificaciones corregida y marcada 📝 donde corresponde, sin borrar el registro histórico.
- [x] Suite de `notificaciones-service` en verde (116/116, con `clean`).
- [x] Reactor completo en verde (892/892, con `clean`): `common-lib` 32, `donaciones-service` 394, `incentivos-service` 189, `logistica-service` 161, `notificaciones-service` 116.

---

## Oleada 10 — Preparación para persistencia real

| Eje | Aplicación a notificaciones-service |
|---|---|
| **Límites de Agregados** | `Notificacion` ya referencia a `Persona` por `personaId: UUID` en vez de por objeto — el límite entre los dos agregados está resuelto por diseño; `notificar(Persona persona, ...)`/`ordenarMedios(Persona persona)` reciben la `Persona` como parámetro en vez de guardarla, y el lookup real se hace en `NotificacionGestor` vía `IPersonaRepository`. No requiere trabajo adicional en esta oleada más que confirmar que la persistencia futura respete este límite (tabla `notificacion` con columna `persona_id`, sin FK a nivel de agregado). |
| **Estrategia ORM** | Jerarquía `MedioDeContacto`/`Correo`/`Telefono`: candidata a `SINGLE_TABLE` con discriminador (`tipo_medio`). Jerarquía `EventoNotificable`/`EventoDeDonacion` (+8 subclases): **no se persiste** — son políticas transitorias, no requieren mapeo ORM; documentar esta decisión explícitamente para que nadie intente mapearlas "por consistencia" con el resto del dominio. |
| **Constructores limpios** | Verificado: ninguna de las 8 subclases de evento, ni las 2 clases base, tiene un constructor vacío — cada una tiene exactamente un constructor de negocio, con guardas de obligatoriedad en las 2 clases base (RF-06, Oleada 3). No hay ningún residuo de deserialización que limpiar. |
| **Idempotencia de ingesta** | Depende de `RF-10` (Oleada 9.5) — especificar cómo se deduplica por `eventId` una vez que el campo exista en los DTOs. |
| **Coordinación distribuida** | No aplica — no hay schedulers en este servicio. |
| **Esquema relacional** | A diseñar cuando se ejecute esta oleada: tabla `notificacion` (con `historial_estado` como tabla hija o columna JSON, a decidir), tabla `persona` + `medio_de_contacto` (con discriminador). |
| **No-regresión** | Verificar que Oleadas 8 y 9 (mothers, fixtures, validación) sigan funcionando tras cualquier cambio de esta oleada. |

> Nota: cero anotaciones JPA prematuras, cero dependencias de base de datos física — esta oleada es de análisis y documentación (📝) hasta que se decida ejecutar la migración física.

### Bitácora de ejecución — 📝 análisis

#### Problema
Documentar las decisiones de persistencia real sin implementarlas. Dos premisas del pedido no coincidían con el código: "revertir a `UUID personaId`" (ya es así) y "constructor vacío" en la jerarquía de eventos (no existe ninguno).

#### Evidencia
- `Notificacion.java`: `private UUID personaId;`, sin campo `Persona persona` ni comentario `// antes: UUID personaId`.
- `grep -n "public Donacion.*()\|protected Evento.*()"` sobre las 10 clases de la jerarquía de eventos → 0 matches de constructor sin argumentos; las 10 tienen exactamente un constructor de negocio.
- Jerarquía de eventos: 8 subclases concretas confirmadas por listado de archivos (`DonacionAsignada`, `DonacionEnCamino`, `DonacionRecibida`, `DonanteInactivo`, `DonanteRegistrado`, `EntregaFallida`, `MisionCumplida`, `SubioCategoria`), no 7.

#### Objetivo
Producir `docs/design/notificaciones-service/decisiones_futuras_en_oleada_10.md` (mismo estilo que el equivalente de `donaciones-service`) con: mapeo ORM, esquema DDL, estrategia de deduplicación por `eventId` (sin implementar), y confirmación de que las oleadas anteriores no sufrieron regresión.

#### Fuera de scope
- No se agregó ninguna anotación JPA ni dependencia de base de datos.
- No se implementó la deduplicación por `eventId` (RF-10) — queda documentada como propuesta, pendiente de coordinar con `donaciones-service`/`incentivos-service`.
- No se tocó ningún archivo de `src/main`/`src/test`.

#### Qué se hizo
1. Corregidas las dos premisas del pedido con evidencia (ver Evidencia) directamente en la tabla de esta oleada, en vez de ejecutar un "revertir" sobre algo que ya está resuelto o "documentar cuál constructor es cuál" sobre constructores que no existen.
2. Creado `docs/design/notificaciones-service/decisiones_futuras_en_oleada_10.md`: mapeo `SINGLE_TABLE` para `MedioDeContacto`/`Correo`/`Telefono`, no-persistencia explícita de la jerarquía de eventos, esquema DDL completo (`persona`, `medio_de_contacto`, `notificacion`, `notificacion_historial_estado`, y la tabla de deduplicación comentada como propuesta), y la justificación de tabla hija vs. columna JSON para `historial_estado`.

#### Tests / Verificación
- Sin cambios de código — no aplica cobertura nueva.
- Suite `notificaciones-service` (con `clean`): **116 tests, 0 failures, 0 errors** — mismo número que el cierre de la Oleada 9.5.
- Reactor completo (con `clean`): **892 tests, 0 failures, 0 errors** — `common-lib` 32, `donaciones-service` 394, `incentivos-service` 189, `logistica-service` 161, `notificaciones-service` 116. Sin regresión.

#### Diseño resultante
Documento de referencia listo para cuando se decida ejecutar la migración física, sin deuda de diseño abierta: los límites de agregado ya están resueltos en el código actual, la jerarquía de eventos queda explícitamente fuera del alcance de persistencia, y la deduplicación por evento tiene una propuesta concreta a la espera de coordinación entre servicios.

#### IA utilizada
Verificación con `grep`/lectura directa de las 10 clases de la jerarquía de eventos y de `Notificacion.java` antes de aceptar las premisas del pedido; redacción del esquema DDL siguiendo el estilo ya usado en `decisiones_futuras_en_oleada_10.md` de `donaciones-service`, escalado al tamaño real del dominio de notificaciones (sin las secciones de MinIO/crypto-shredding, que no aplican acá).

#### Verificación humana
- [x] Premisas del pedido verificadas contra el código, corregidas con evidencia en vez de ejecutadas literalmente.
- [x] Documento de decisiones creado con el mismo estilo que el de `donaciones-service`.
- [x] Cero anotaciones JPA, cero dependencias de base de datos física.
- [x] Sin cambios funcionales — reactor completo en verde, mismos números que el cierre de la Oleada 9.5 (892/892).

---

## Oleada 11 — Sincronización con `common-lib`

**Estado hoy: aplica.** La Fase 0.5 confirma que `common-lib` ya tiene una base de Domain Events real, construida y con tests propios: `AgregadoConEventos<E extends EventoDeDominio>` y `EventoDeDominio` (`registrarEvento`/`getDomainEvents`/`clearDomainEvents`, con snapshot inmutable vía `List.copyOf`). `Notificacion` implementa hoy su propia lista de `domainEvents`/`NotificacionDomainEvent` en paralelo a esa base (Oleada 2/RF-02), en vez de extenderla.

**Acción concreta:** migrar `Notificacion` para que extienda `AgregadoConEventos<NotificacionDomainEvent>`, eliminando la lista de eventos local y delegando `registrarEvento`/`getDomainEvents`/`clearDomainEvents` a la clase base. `NotificacionDomainEvent` (y sus 4 subtipos: `NotificacionCreada`, `NotificacionEnviada`, `NotificacionFallida`, `CambioEstadoNotificacion`) deben pasar a implementar/extender `EventoDeDominio` en vez de ser una jerarquía sellada independiente. Sin esto, `notificaciones-service` queda como el único servicio de los cuatro que no usa la base común para algo que ya construyó por su cuenta.

---

## Auditoría Final (al completar las oleadas que apliquen)

Repetir la Fase 0 comparando el DC actualizado (corregido según §3.4 de `fase-0-auditoria.md`) contra el código final, y correr el checklist genérico de la plantilla v2 (Domain Events, Tell-Don't-Ask, excepciones, `@Setter`, copias defensivas, validación por capas, HTTP, determinismo, idempotencia, JPA, reuso de `common-lib`, suite completa en verde).
