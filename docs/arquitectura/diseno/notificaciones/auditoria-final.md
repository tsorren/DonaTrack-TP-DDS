# Auditoría Final — Servicio de Notificaciones

> Cierre del [plan de oleadas](./plan-oleadas-notificaciones.md), sobre los hallazgos de la [Fase 0](./fase-0-auditoria.md). Cubre las Oleadas 1, 2, 3, 4, 6, 7, 8, 9, 9.5, 10 y 11 (la 5 no aplicó — confirmado, sin `@Scheduled` en el servicio). Mismo formato que la Fase 0 original: todo ✅ cita archivo:línea real; lo que sea solo diseño/documentación va con 📝, nunca con ✅.

## 1. Diagrama de Clases (DC) vs. código final

**Ninguna oleada tocó [`diagrama-de-clases-notificaciones.puml`](./diagrama-de-clases-notificaciones.puml).** Queda explícitamente pendiente, no implícitamente resuelto por haber ejecutado las oleadas. Divergencias concretas confirmadas contra el código de hoy:

| Elemento del `.puml` | Dibuja | Código real | Oleada que lo cambió |
|---|---|---|---|
| `Notificacion` | `- persona: Persona` (línea 113), sin `personaId`, sin `domainEvents`/`getDomainEvents()`/`clearDomainEvents()` | `Notificacion.java:25` `private UUID personaId;`; extiende `AgregadoConEventos<NotificacionDomainEvent>` (línea 26) | Ya era así en el baseline (personaId); domain events, Oleada 2; migración a `AgregadoConEventos`, Oleada 11 |
| `SERVICES.NotificacionesCreadasEvent` | Dibujada como clase activa (líneas 290-293) | Eliminada — `grep -rn "NotificacionesCreadasEvent" notificaciones-service/src` → 0 matches | Oleada 2 |
| `SERVICES.NotificacionGestor` | Sin `eventPublisher`, escucha `NotificacionesCreadasEvent` (línea 327) | `NotificacionGestor.java` tiene `ApplicationEventPublisher eventPublisher`, escucha `onNotificacionCreada(NotificacionCreada event)` | Oleada 2 |
| `SERVICES.NotificacionService` | Sin validación, sin código HTTP explícito | `procesar()` recibe `@Valid @RequestBody` desde el controller; `NotificacionController.procesarEvento` devuelve `202 Accepted` (`NotificacionController.java:27`) | Oleada 9 |
| Controllers | No dibujados en absoluto (`NotificacionController`/`PersonasController`/`IPersonasController`) | Existen desde antes del baseline; `PersonasController` con `@RequiredArgsConstructor` desde Oleada 4 | Preexistente / Oleada 4 |
| `EventoNotificable`/`EventoDeDonacion` | Sin marca de "no persistido" | Documentado explícitamente en `decisiones_futuras_en_oleada_10.md` §3 | Oleada 10 (documental) |

Esto no es una regresión de ninguna oleada — el `.puml` nunca estuvo en el alcance de ningún prompt de ejecución (todos hablaban de código y bitácoras, ninguno pidió actualizar el diagrama). Queda como pendiente explícito en la sección 4.

## 2. Checklist de RFs, con evidencia

| RF | Enunciado | Estado | Evidencia |
|---|---|---|---|
| RF-01 | Tell-Don't-Ask en `MedioDeContacto` | ✅ | `MedioDeContacto.java:15-19` (`marcarComoPredeterminado()`/`desmarcarComoPredeterminado()`), sin `@Setter` sobre `esPredeterminado` |
| RF-02 | `Notificacion` genera sus propios Domain Events, con copia defensiva real | ✅ | `Notificacion.java:26` (`extends AgregadoConEventos<NotificacionDomainEvent>`); `AgregadoConEventos.java:23-24` (`getDomainEvents()` → `List.copyOf`); `Notificacion.java:47-48` (`getHistorialEstado()` → `List.copyOf`) |
| RF-03 | Domain Events / transiciones en agregado secundario (`Persona`) | 📝 | `Persona` no tiene transiciones de estado complejas — decisión documentada en la bitácora de Oleada 3, no un RF con código propio que verificar acá |
| RF-04 | `Persona.getMediosDeContacto()` con copia defensiva | ✅ | `Persona.java:17-18`: `return List.copyOf(this.mediosDeContacto);` |
| RF-05 | 0 excepciones crudas en mappers; `ErrorCatalog` con sección NOTIFICACIONES | ✅ | `grep -rn "new IllegalArgumentException" notificaciones-service/src/main` → 0 matches; `ErrorCatalog.java:132` `// === NOTIFICACIONES (9xx) ===`, con `MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO("ERR-VAL-901")` |
| RF-06 | 0 `@Setter` público en `EventoNotificable`/`EventoDeDonacion` y sus 8 subclases | ✅ | `grep -rn "@Setter" notificaciones-service/.../eventos/*.java` → 0 matches (solo un comentario que menciona el patrón viejo, `EventoNotificable.java:17`); guardas de obligatoriedad en `EventoNotificable.java` y `EventoDeDonacion.java` |
| RF-07 | Guard de `esPredeterminado == null` en `ordenarMedios()`, con desempate determinista | ✅ | `Notificacion.java:90-104`: `Boolean.TRUE.equals(m.getEsPredeterminado())` (sin NPE) + comentario documentando el desempate por orden de alta (estabilidad de `Stream.sorted()`) |
| RF-08 | `personaRepository`/`sender` eliminados de `NotificacionService` | ✅ | `NotificacionService.java:16-18`: únicos 3 campos son `repository`, `mapper`, `eventPublisher` — sin `personaRepository` ni `sender` |
| RF-09 | Los 10 DTOs con Bean Validation + `@Valid` en los 2 controllers; códigos HTTP correctos | ✅ | `grep -rl "jakarta.validation" notificaciones-service/.../dto/` → exactamente 10 archivos (8 de `dto/input/*` + `PersonaReplicaDTO` + `MedioDeContactoReplicaDTO`); `NotificacionController.java:21` y `PersonasController.java:20` con `@Valid`; `NotificacionController.java:27` → `202`, `PersonasController.java:29` → `204` |
| RF-10 | Idempotencia por `eventId` — documentada, no implementada sin coordinar | 📝 (confirmado que sigue así) | `grep -rn "eventId"` en `notificaciones-service`/`donaciones-service` → 0 matches de implementación; propuesta documentada en `decisiones_futuras_en_oleada_10.md` §5 y en la bitácora de Oleada 9.5, punto 6 del catálogo de chequeos. No se tocó `NotificacionesFeignClient` de `donaciones-service` en ningún momento |
| RF-11/RF-12 | Documentación y sincronización con `common-lib` | ✅ | Esta auditoría + `decisiones_futuras_en_oleada_10.md` + Oleada 11 (migración a `AgregadoConEventos`) |

## 3. Cobertura de tests

| Clase | Tests dedicados | Evidencia |
|---|---|---|
| `Notificacion` | 21 | `NotificacionTest.java` (350 líneas) — domain events, `notificar()`, `ordenarMedios()` (incluye el guard de `esPredeterminado == null`, RF-07), `historialEstado`, `anonimizar()` |
| `MedioDeContactoMapper` | 13 | `MedioDeContactoMapperTest.java` |
| `EventoMapper` | 9 | `EventoMapperTest.java` |
| `PersonaMapper` | 2 | `PersonaMapperTest.java` |
| `CambioEstadoNotificacion` | dedicado (Oleada 8) | `CambioEstadoNotificacionTest.java` |

Suite completa de `notificaciones-service`: **116 tests, 0 failures, 0 errors** (confirmado con `clean` al cierre de la Oleada 11). Reactor completo: **892 tests, 0 failures, 0 errors** (`common-lib` 32, `donaciones-service` 394, `incentivos-service` 189, `logistica-service` 161, `notificaciones-service` 116).

## 4. Pendientes explícitos

- **[`diagrama-de-clases-notificaciones.puml`](./diagrama-de-clases-notificaciones.puml) no está actualizado.** Ninguna oleada lo tocó (ver §1). No se corrige en esta auditoría — es un cambio de documentación de diseño, no de código, y no estaba en el alcance de ningún prompt ejecutado.
- **RF-10 (idempotencia) sigue sin implementar**, tal como se pidió mantenerlo — es una decisión que cruza el límite de `notificaciones-service` y requiere coordinación con `donaciones-service`/`incentivos-service` antes de tocar el contrato del DTO compartido.
- **RF-03** no generó ningún artefacto de código propio — quedó resuelto como decisión documental en la bitácora de Oleada 3 (`Persona` no necesita domain events propios), no como un ítem verificable con evidencia de línea.

## Cierre

Se completaron RF-01, RF-02, RF-04, RF-05, RF-06, RF-07, RF-08, RF-09 y RF-11/RF-12 con evidencia de código verificable. RF-03 quedó resuelto por decisión documental, sin código propio que auditar. RF-10 queda pendiente, documentado y sin implementar, tal como se decidió explícitamente. El `.puml` del servicio queda desactualizado respecto del código final — pendiente explícito, no resuelto en este cierre. No se implementó nada nuevo en este último paso: esta auditoría es exclusivamente de verificación.
