# Fase 0 — Auditoría y Baseline: Servicio de Notificaciones

> Documento de **auditoría/reviewer**, siguiendo `plan-refactor-oleadas-generico-v2.md` §4. No modifica código, no corrige nada. Todo lo que contiene es observación (con archivo/línea citado), inferencia (marcada explícitamente) o recomendación futura (marcada, sin implementar). Rama de trabajo prevista: `E4_refactor-notificaciones` (esta auditoría se hizo sobre `E4_refactor_Notificaciones`).

**Fuentes utilizadas, en el orden de prioridad del template:**
1. `Diagrama de Clases - Servicio Notificaciones.csv` (export de Lucidchart, texto exacto) — DC de referencia.
2. `docs/adr/notificaciones-service/*.md` (7 ADRs, 2026-05-19 a 2026-05-21).
3. `docs/aggregates/aggregate-servicio-notificaciones.md` (modelo DDD narrativo).
4. Reglas de negocio explícitas en el código (`ErrorCatalog`, `ValidationException`, etc.).
5. Código actual de `notificaciones-service` (tratado como evidencia de lo que hay, no como diseño objetivo).

---

## 0. Resultado de la verificación del diagrama PlantUML — respuesta directa

Se pidió verificar si "el diagrama de puml" está actualizado. Hay dos artefactos `.puml` relevantes y **ninguno de los dos es una fuente confiable hoy**:

| Artefacto | Estado |
|---|---|
| `docs/design/notificaciones-service/diagrama-de-clases-notificaciones.puml` | **No existe.** Sí existe el equivalente para logística (`docs/design/logistica-service/diagrama-de-clases-logistica.puml`), pero notificaciones nunca tuvo su versión mantenida a mano. |
| `notificaciones-service/target/modelo_tecnico.puml` | Existe, pero es un **artefacto de build autogenerado** (no versionado, se regenera en cada `mvn compile`) y **está desactualizado respecto del código fuente actual**: no incluye `grupo5.notificaciones.services.gestores.NotificacionGestor` ni `grupo5.notificaciones.services.events.NotificacionesCreadasEvent` (ambas clases existen en `src/main` — ver `NotificacionGestor.java`, `NotificacionesCreadasEvent.java` — pero no aparecen en el `.puml`). Las relaciones que sí describe para `NotificacionService` corresponden a una versión anterior de su constructor, previa a la introducción del `ApplicationEventPublisher` y del propio `NotificacionGestor`. |

**Conclusión:** no hay ningún `.puml` actualizado hoy contra el cual verificar. Recomendación (📝, no implementada): regenerar `target/modelo_tecnico.puml` (`mvn compile` sobre `notificaciones-service`) como parte del baseline de Fase 0, y crear `docs/design/notificaciones-service/diagrama-de-clases-notificaciones.puml` a mano —igual que se hizo para logística— usando el CSV como base y aplicando las 3 correcciones del §3 de este documento.

Además, el propio CSV (que es la fuente de mayor prioridad) tiene **tres inconsistencias internas** detectadas al reconstruir el modelo — ver §3.4. No son errores del código: son errores de dibujo del propio diagrama que conviene corregir si se decide versionarlo en `docs/design/`.

---

## 1. Resumen ejecutivo (hallazgos priorizados)

1. **[ALTO] Falsos domain events.** `NotificacionService.procesar()` construye y publica `NotificacionesCreadasEvent` a mano en vez de que `Notificacion` gestione sus propios eventos de dominio (`domainEvents`/`getDomainEvents()`) — antipatrón central que el plan v2 busca eliminar. Ver §5, §8.
2. **[ALTO] Campos muertos por refactor a medio terminar en `NotificacionService`.** El constructor asigna `this.personaRepository = personaRepository;` y `this.sender = sender;` sin recibir esos parámetros — ambos campos quedan siempre `null` y no se usan en ningún método de la clase. Ver §5.
3. **[ALTO] Riesgo de duplicación por reintentos sin idempotencia de ingesta.** `NotificacionesFeignClient` (lado `donaciones-service`) usa `FeignRetryConfig`; los DTO de entrada (`EventoDonacionAsignadaDTO`, etc.) no llevan ningún identificador de correlación. Un reintento de red puede generar notificaciones duplicadas porque `generarNotificaciones()` no tiene ninguna clave de deduplicación. Ver §5.10 (9.5).
4. **[MEDIO] Arquitectura de comunicación documentada incorrectamente.** `docs/aggregates/aggregate-servicio-notificaciones.md` describe un modelo de "suscripción a eventos" / mensajería, pero el código (y el ADR `20260519-comunicacion-con-el-servicio-de-notificaciones.md`) confirman que la comunicación es **síncrona vía REST/Feign** (push desde cada servicio productor). No hay RabbitMQ/Kafka en el `pom.xml` ni `@RabbitListener`/`@KafkaListener` en el código. Ver §3.3, §9.
5. **[MEDIO] `@Setter` público en 10 de las 12 clases de dominio no-agregado** (`MedioDeContacto`, `Correo`, `Telefono`, `EventoNotificable` y sus 7 subclases) — viola "Inmutabilidad por defecto" del plan v2. `Notificacion` y `Persona` (los dos Aggregate Roots) **ya están limpios** en este punto. Ver §5, §11.
6. **[MEDIO] Divergencia real DC↔código en el modelado de WhatsApp.** El DC (y el propio ADR de "Medios de Contacto") describen una clase `WhatsApp` separada; el código la fusionó en `Telefono` vía `TipoTelefono.WHATSAPP` sin que ni el DC ni los ADRs se hayan actualizado. Ver §3.1, §3.4.
7. **[MEDIO] Cero validación Bean Validation en los 10 DTOs de entrada.** Ningún DTO (`EventoNotificableDTO` y sus 8 implementaciones, `PersonaReplicaDTO`, `MedioDeContactoReplicaDTO`) tiene `@NotNull`/`@NotBlank`/`@Valid`. Ver §9.
8. **[MEDIO] Excepciones crudas fuera del dominio estricto pero dentro de la capa de aplicación.** `MedioDeContactoMapper` (2 sitios) y `EventoMapper.buscarPersona` (1 sitio, vía `orElseThrow`) lanzan `IllegalArgumentException` en lugar de `ValidationException`/`RecursoNoEncontradoException` de `common-lib`. Mitigado parcialmente: `GlobalExceptionHandler` ya mapea `IllegalArgumentException` a 400, así que no es un 500 silencioso, pero rompe la homogeneidad de `ErrorCatalog`. Ver §6.
9. **[MEDIO] Riesgo de `NullPointerException` en `Notificacion.ordenarMedios()`** si `MedioDeContacto.esPredeterminado` llega `null` desde el DTO (`Boolean`, nullable) — el `Comparator.comparing(MedioDeContacto::getEsPredeterminado)` no tolera `null`. Ver §5.10.
10. **[BAJO-MEDIO] Desempate no determinista en `ordenarMedios()`** entre dos medios con el mismo valor de `esPredeterminado` — no hay criterio secundario explícito. Ver §5.10.
11. **[BAJO] Wildcard imports propios del servicio.** `EventoMapper.java` importa `grupo5.notificaciones.dto.input.*` y `...eventos.*` (2 matches; hay 2 matches adicionales de wildcard de framework Spring en los controllers, de severidad menor). Ver §6, catálogo.
12. **[BAJO] Inconsistencia de códigos HTTP.** `POST /notificaciones` (creación) devuelve `200 OK` en vez de `201 Created`; `DELETE /api/notificaciones/personas/{id}` (anonimización) devuelve `200 OK` en vez de `204 No Content`. Ver §9.
13. **[BAJO] Inconsistencias menores de estilo/paquete.** `PersonasController` no usa `@RequiredArgsConstructor` (a diferencia de `NotificacionController`); el paquete de test `models/entities/medioDeContacto` no refleja el paquete real `models/entities/personas`; los repositorios `@Repository` (`NotificacionRepositoryEnMemoria`, `PersonaRepositoryEnMemoria`) viven bajo `models/repositories/impl` en vez de `infrastructure/` a pesar de ser adapters técnicos. Ver §6, §9, §11.
14. **[BAJO] Cobertura de tests con huecos concretos.** No existe `NotificacionTest.java` dedicado al Aggregate Root principal (solo cubierto indirectamente vía tests de `NotificacionService`/`NotificacionGestor`/`NotificacionRouter`); no hay test de `CambioEstadoNotificacion`; no hay Object Mothers/Fixtures (`*Mother`/`*Fixtures`). Ver §10.

---

## 2. Modelo objetivo reconstruido del DC

Por paquete conceptual (Flex polygons del CSV: PERSONAS, NOTIFICACIONES, EVENTOS, CONTACTOS, NOTIFICADOR):

### PERSONAS
| Clase | Tipo | Atributos | Métodos | Responsabilidad |
|---|---|---|---|---|
| `Persona` | Class | `id: UUID`, `denominacion: String`, `mediosDeContacto: List<MedioDeContacto>` | `agregarMedioContacto()`, `quitarMedioContacto()`, `definirMedioContactoPredeterminado()`, `anonimizar()` | Aggregate Root; destinatario de notificaciones |

### NOTIFICACIONES (+ diagrama de estados embebido)
| Clase | Tipo | Atributos | Métodos | Responsabilidad |
|---|---|---|---|---|
| `Notificacion` | Class | `id`, `persona: Persona`, `mensaje`, `fechaCreacion`, `estadoNotificacion`, `historialEstado: List<CambioEstadoNotificacion>` | `notificar(NotificacionSender)`, `ordenarMedios(): List<MedioDeContacto>` (privado en el DC), `anonimizar()` | Aggregate Root; registra el intento y estado final de una notificación |
| `EstadoNotificacion` (enum) | Enum | `PENDIENTE`, `ENVIADA`, `FALLIDA` | — | Estado del envío. Transiciones dibujadas: `Pendiente→Enviada` ("Funcionó un medio"), `Pendiente→Fallida` ("Fallaron todos los medios") |
| `CambioEstadoNotificacion` | Class | `estadoAnterior`, `estadoNuevo` (tipados **`EstadoChofer`** en el CSV — ver §3.4), `timestamp: LocalDateTime` | — | Historial inmutable de transiciones |

### EVENTOS (jerarquía de `EventoNotificable`, políticas de dominio no persistentes)
| Clase | Tipo | Atributos propios | Métodos | Responsabilidad |
|---|---|---|---|---|
| `EventoNotificable` (abstract) | Class | `persona: Persona`, `fecha` | `generarNotificaciones(): List<Notificacion>` | Raíz de la jerarquía de factorías |
| `EventoDeDonacion` (abstract) | Class | `entidadBeneficiaria: Persona`, `detalleDonacion` | `generarNotificaciones()`, `# armarMensajeDonante()`, `# armarMensajeBeneficiario()` | Template method para eventos con 2 destinatarios |
| `DonacionAsignada` | Class | — | `armarMensajeDonante()`, `armarMensajeBeneficiario()` | |
| `DonacionRecibida` | Class | `fechaHoraRecepcion`, `camion: String`, `patenteCamion` | idem | |
| `DonacionEnCamino` | Class | `enlaceSeguimiento` | idem | |
| `EntregaFallida` | Class | `administracion: Persona`, `motivo`, `replanificable` | `generarNotificaciones()` (override, 3 destinatarios), `armarMensajeAdmin()` (privado) | |
| `MisionCumplida` | Class | `nombreMision`, `recompensa` | `generarNotificaciones()` | |
| `SubioCategoria` | Class | `categoriaNueva`, `categoriaAnterior` | `generarNotificaciones()` | |
| `DonanteRegistrado` | Class | `credencialesDeAcceso` | `generarNotificaciones()` | |
| `DonanteInactivo` | Class | `diasSinActividad: Integer` | `generarNotificaciones()` | |
| `Anonimizable` (interface) | Interface | — | `anonimizar()` | Contrato transversal |

### CONTACTOS
| Clase | Tipo | Atributos | Métodos | Responsabilidad |
|---|---|---|---|---|
| `MedioDeContacto` (abstract) | Class | `esPredeterminado: Boolean` | `enviarMensaje(String, NotificacionSender): void`, `# medioDeContacto()` | Base polimórfica de canal de contacto |
| `Correo` | Class | `direccionCorreo` | `enviarMensaje(...): boolean`, `anonimizar()` | |
| `Telefono` | Class | `caracteristica`, `codigoArea`, `numero`, `tipoTelefono: TipoTelefono` | `enviarMensaje(...): boolean`, `obtenerNumeroCompleto()`, `anonimizar()` | |
| `TipoTelefono` (enum) | Enum | `ESTANDAR`, `WHATSAPP` | — | |

### NOTIFICADOR
| Clase | Tipo | Atributos | Métodos | Responsabilidad |
|---|---|---|---|---|
| `NotificacionSender` (interface) | Interface | — | `enviarA(Correo, String)`, `enviarA(Telefono, String)`, `enviarA(WhatsApp, String)` | Double dispatch hacia el router |
| `NotificacionRouter` | Class | `correoApi: CorreoAdapter`, `telefonoApi: TelefonoAdapter`, `whatsappApi: WhatsAppAdapter` | `enviarA(Correo,...)`, `enviarA(Telefono,...)`, `enviarA(WhatsApp,...)` | Facade de infraestructura, implementa `NotificacionSender` |
| `CorreoAdapter` / `TelefonoAdapter` / `WhatsAppAdapter` (interfaces) | Interface | — | `enviarMail`/`enviarSms`/`enviarWhatsapp` | Puertos hacia proveedores externos |

---

## 3. Matriz DC → código

| Clase/elemento del DC | ¿Existe en código? | Dónde | Qué difiere |
|---|---|---|---|
| `Persona` | ✅ Sí | `models/entities/personas/Persona.java` | Código agrega `TipoPersona tipoPersona` (HUMANA/JURIDICA), no dibujado en el DC. Getter de colección usa `Collections.unmodifiableList` en vez de `List.copyOf` (§11). |
| `Notificacion` | ✅ Sí | `models/entities/notificaciones/Notificacion.java` | Código agrega `actualizarEstado()` (público, no está en el DC) que es quien alimenta `historialEstado`; `ordenarMedios()` es público en código, el DC lo dibuja `-` (privado). |
| `EstadoNotificacion` | ✅ Sí, idéntico | `models/entities/notificaciones/EstadoNotificacion.java` | Sin diferencias. |
| `CambioEstadoNotificacion` | ✅ Sí | `.../eventos/CambioEstadoNotificacion.java` | **El DC tipa mal sus dos campos** (`EstadoChofer` en vez de `EstadoNotificacion` — ver §3.4). El código usa correctamente `EstadoNotificacion`. |
| `EventoNotificable`, `EventoDeDonacion` y las 7 subclases | ✅ Sí, 1:1 | `models/entities/notificaciones/eventos/*.java` | Coinciden en atributos y jerarquía. Todas usan `@Setter` (Lombok) para que las subclases llamen `this.setPersona(...)` desde su propio constructor — el DC no expone esto (no dibuja setters), es un detalle de implementación (§11). |
| `Anonimizable` | ✅ Sí | `models/ports/Anonimizable.java` | Código agrega 2 constantes (`VALOR_NUMERICO`, `VALOR_STRING`) no dibujadas en el DC. |
| `MedioDeContacto`, `Correo`, `Telefono` | ✅ Sí | `models/entities/personas/*.java` | Ídem `@Setter` no dibujado. `enviarMensaje` devuelve `boolean` en código; el DC lo dibuja `void` (inconsistente incluso dentro del propio DC, que en `NotificacionRouter` sí dibuja `boolean`). |
| `TipoTelefono` | ✅ Sí, idéntico | `models/entities/personas/TipoTelefono.java` | Sin diferencias. |
| **`WhatsApp` (clase)** | ❌ **No existe como clase** | — | Ver §3.1 — fusionada en `Telefono` vía `TipoTelefono.WHATSAPP`. |
| `NotificacionSender` | ⚠️ Existe, pero **sin el método de `WhatsApp`** | `models/ports/NotificacionSender.java` | Solo `enviarA(Correo,...)` y `enviarA(Telefono,...)`. Coherente con que no existe la clase `WhatsApp`. |
| `NotificacionRouter` | ⚠️ Existe, pero **sin el `enviarA(WhatsApp,...)` del DC** | `infrastructure/NotificacionRouter.java` | Resuelve WhatsApp dentro de `enviarA(Telefono,...)` chequeando `telefono.getTipo() == TipoTelefono.WHATSAPP` (double dispatch simplificado, ver ADR "Double Dispatch"). |
| `CorreoAdapter`, `TelefonoAdapter`, `WhatsAppAdapter` | ✅ Sí, idéntico | `infrastructure/*.java` | Sin diferencias funcionales (siguen existiendo aunque `WhatsApp` no exista como entidad de dominio — tiene sentido, son puertos hacia el proveedor de WhatsApp, no hacia la entidad). |

### 3.1 Divergencia real: modelado de WhatsApp

El ADR `20260520-medios-de-contacto.md` describe la decisión original: *"la clase Whatsapp hereda de Teléfono reemplazando únicamente el método para enviar mensajes"*. Ni el DC ni ese ADR se actualizaron cuando el equipo pasó a resolverlo con `TipoTelefono.WHATSAPP` dentro de `Telefono` (confirmado en `NotificacionRouter.enviarA(Telefono,...)` y en `MedioDeContactoMapper`, que mapea `"WHATSAPP"` a un `Telefono` con `tipo=WHATSAPP`, no a una subclase). Es una decisión de diseño ya tomada y consistente en todo el código — el DC y el ADR son los que quedaron desactualizados, no el código.

### 3.2 `IPersonasController` / DTOs de sincronización — no están dibujados en el DC

`PersonaReplicaDTO`, `MedioDeContactoReplicaDTO`, `IPersonasController`/`PersonasController`/`IPersonasService`/`PersonasService`, y los 8 DTOs de entrada bajo `dto/input/` (uno por evento) no aparecen en el DC. Esto es esperable: el DC modela el **dominio**, no la capa de transporte/replicación (DTOs y controllers son adapters). No es una deuda, es simplemente algo fuera del alcance de un diagrama de clases de dominio — se documenta para que la Fase 0 quede completa, no para "corregir" nada.

### 3.3 Arquitectura de comunicación — el DC no lo dice, pero un documento del proyecto sí, y está mal

El DC no representa mecanismos de comunicación entre servicios (correcto, no le corresponde). El problema es que `docs/aggregates/aggregate-servicio-notificaciones.md` §1 sí lo afirma, y lo afirma de forma incorrecta: *"No realiza llamadas REST síncronas a otros servicios... se suscribe a los eventos y mantiene una proyección"*. La evidencia real:
- `docs/adr/notificaciones-service/20260519-comunicacion-con-el-servicio-de-notificaciones.md` — decisión **explícita y aceptada**: comunicación **sincrónica**, push-based vía API REST.
- `notificaciones-service/pom.xml` no tiene dependencia de AMQP/Kafka; no hay ningún `@RabbitListener`/`@KafkaListener`/`RabbitTemplate` en `src/main` (barrido `grep` → 0 matches).
- `NotificacionController.procesarEvento` y `PersonasController.sincronizar/anonimizar` son endpoints REST síncronos.
- Del lado consumidor, `donaciones-service/.../NotificacionesFeignClient.java` + `NotificacionesAsyncService.java` confirman el patrón real: Feign síncrono, envuelto en `@Async` **del lado del llamador** (no hay cola de mensajería intermedia).

No se corrige en esta fase; se deja registrado como inconsistencia documental a resolver (actualizar el `.md` de agregados, no el código).

### 3.4 Errores del propio CSV (a corregir si se decide versionar el `.puml`)

1. `CambioEstadoNotificacion.estadoAnterior`/`estadoNuevo` están tipados `EstadoChofer` en el CSV (fila 97-100) — típico copy-paste desde el diagrama de logística (`CambioEstadoEntrega`). Debería ser `EstadoNotificacion`. El código ya lo tiene bien.
2. `NotificacionSender.enviarA(WhatsApp, String)` y el `enviarA(WhatsApp, String)` de `NotificacionRouter` referencian una clase `WhatsApp` que no está dibujada como shape en ningún lado del propio diagrama — inconsistencia interna del DC, además de estar desalineada con el código (§3.1).
3. La relación `MedioDeContacto → NotificacionSender` (línea `id98`, fila 119 del CSV) está dibujada como `Generalization` (herencia). Semánticamente debería ser una dependencia de uso (`MedioDeContacto.enviarMensaje(String, NotificacionSender)` recibe el sender como parámetro; no lo extiende ni lo implementa). En el código no hay tal relación de herencia.

---

## 4. Matriz código → DC (clases del código sin equivalente directo en el DC)

| Clase en código | Paquete | Motivo de no estar en el DC |
|---|---|---|
| `TipoPersona` (enum) | `models.entities.personas` | Atributo agregado a `Persona` (`HUMANA`/`JURIDICA`) no contemplado en el DC original. |
| `PersonaReplicaDTO`, `MedioDeContactoReplicaDTO`, DTOs de `dto/input/*` (8 clases), `NotificacionDTO` | `dto/*` | Capa de transporte, fuera del alcance de un diagrama de dominio. |
| `IPersonasController`, `PersonasController`, `NotificacionController` | `controllers` | Adapters de entrada REST. |
| `IPersonasService`, `PersonasService`, `NotificacionService` | `services/impl` | Application Services — el DC modela dominio, no orquestación. |
| `NotificacionGestor` | `services.gestores` | Listener de `NotificacionesCreadasEvent` que dispara el envío efectivo — pieza de orquestación reactiva, no dibujada. |
| `NotificacionesCreadasEvent` | `services.events` | Evento de aplicación ad-hoc (no domain event del agregado) — ver §8. |
| `EventoMapper`, `PersonaMapper`, `MedioDeContactoMapper` | `services/mappers` | DTO↔entidad, capa de aplicación. |
| `INotificacionRepository`, `IPersonaRepository` y sus `*EnMemoria` | `models/repositories[/impl]` | Persistencia — el DC no dibuja repositorios. |
| `CorreoEnvioMock`, `TelefonoEnvioMock`, `WhatsappEnvioMock` | `infrastructure/mockEnvios` | Implementaciones mock de los adapters, para desarrollo/tests. |
| `AdminConstantes`, `AdminSeeder` | `config` | Seed de un usuario admin — infraestructura de arranque, sin relación con el dominio de notificaciones en sí. |

Nada de esto es deuda: es exactamente lo que corresponde tener por fuera del dominio en la arquitectura objetivo (§1 del plan v2).

---

## 5. Auditoría de Application Services

### `NotificacionService.procesar(EventoNotificableDTO dto)`

| Paso | Código | Clasificación |
|---|---|---|
| 1 | `mapper.toEntity(dto)` | MAPEO |
| 2 | `evento.generarNotificaciones()` | DOMINIO (delegado al `EventoNotificable`) |
| 3 | `repository.saveAll(notificaciones)` | PERSISTENCIA |
| 4 | `eventPublisher.publishEvent(new NotificacionesCreadasEvent(this))` | **POSIBLE_LOGICA_DE_NEGOCIO** — el Service construye el evento a mano en vez de leerlo de la entidad. Es el paso 6 del flujo estándar ("publica eventos que generó el dominio"), pero acá el dominio no generó ningún evento: el Service lo inventa. |

**Hallazgo adicional en el constructor** (no es un paso de `procesar`, pero es del mismo Service):
```java
public NotificacionService(
    INotificacionRepository repository, EventoMapper mapper, ApplicationEventPublisher eventPublisher) {
  this.repository = repository;
  this.personaRepository = personaRepository; // <- se autoasigna null, el parámetro no existe
  this.sender = sender;                        // <- idem
  this.mapper = mapper;
  this.eventPublisher = eventPublisher;
}
```
`personaRepository` y `sender` son campos de la clase (declarados `= null`) que **nunca reciben un valor real** porque el constructor no los toma como parámetro — se asignan a sí mismos. Ningún método de `NotificacionService` los usa. Es código residual de un rename/refactor anterior (coincide con el commit `1fcb6f1a refactor notificaciones` de esta misma rama) que compila porque Java permite la autoasignación, pero es lógicamente muerto. Clasificación: **DUDOSO → limpieza de deuda técnica** (no bloquea nada, pero conviene documentarlo en el resumen ejecutivo para que no se lo lean como una intención de diseño).

### `NotificacionService.obtenerPorPersona(UUID personaId)`

| Paso | Código | Clasificación |
|---|---|---|
| 1 | `repository.findByPersonaId(personaId)` | PERSISTENCIA |
| 2 | `.map(n -> new NotificacionDTO(...))` | MAPEO (inline, no delegado a un mapper dedicado — bajo impacto, es una única línea) |

### `PersonasService.sincronizar(PersonaReplicaDTO dto)`

| Paso | Código | Clasificación |
|---|---|---|
| 1 | `mapper.toEntity(dto)` | MAPEO |
| 2 | `repository.save(persona)` | PERSISTENCIA |

### `PersonasService.anonimizar(UUID id)`

| Paso | Código | Clasificación |
|---|---|---|
| 1 | `repository.findById(id).orElseThrow(...)` | PERSISTENCIA + VALIDACION_TECNICA (404) |
| 2 | `persona.anonimizar()` | DOMINIO |
| 3 | `repository.save(persona)` | PERSISTENCIA |
| 4 | `notificacionRepository.findByPersonaId(id)` | PERSISTENCIA |
| 5 | `notificacion.anonimizar()` por cada una | DOMINIO (aplicado sobre el *otro* agregado, `Notificacion`) |
| 6 | `notificacionRepository.save(notificacion)` por cada una | PERSISTENCIA |

Este método coordina **dos agregados** (`Persona` y `Notificacion`) desde un único Application Service. Es aceptable según el criterio del plan v2 (§1: "un Service puede tener asociado más de un repositorio si el caso de uso lo requiere"), y aquí el caso de uso ("anonimizar a una persona y todo lo que la referencia") lo requiere genuinamente. Clasificación general: ORQUESTACION correcta, sin lógica de negocio fuera de lugar.

### `NotificacionGestor.notificarPendientes()` (activado por `@EventListener` sobre `NotificacionesCreadasEvent`)

| Paso | Código | Clasificación |
|---|---|---|
| 1 | `repository.findByEstado(PENDIENTE)` | PERSISTENCIA |
| 2 | `notificacion.notificar(sender)` por cada una | DOMINIO |
| 3 | `repository.save(notificacion)` por cada una | PERSISTENCIA |

Este es, con diferencia, el Application Service **mejor alineado** con el flujo estándar del plan v2: activador (Listener) → orquestador delgado → un único método de dominio → persiste. Buen ejemplo a preservar, no a tocar en Oleada 4.

### Compilación / firmas

No se detectó ningún rename a medio terminar entre entidad↔service↔test (a diferencia de lo que la Fase 0 de donaciones había encontrado). El único hallazgo de esta categoría es el constructor de `NotificacionService` descrito arriba, que es autocontenido dentro de una sola clase y no rompe compilación en ningún call-site.

---

## 6. Inventario de lógica de negocio fuera del dominio

| Archivo:método | Regla detectada | Ubicación actual | Dueño posible según el DC | Evidencia |
|---|---|---|---|---|
| `NotificacionService.procesar():44` | "toda vez que se procesa un evento, hay que avisar que hay notificaciones nuevas" | Application Service (evento construido a mano) | `Notificacion`/`EventoNotificable` (domain event genuino) | `eventPublisher.publishEvent(new NotificacionesCreadasEvent(this))` |
| `MedioDeContactoMapper.toEntity():36,69` | "tipo de medio no soportado → error" | Mapper (capa de aplicación) | Podría vivir como una `ValidationException` con código de `ErrorCatalog` en vez de `IllegalArgumentException` | `throw new IllegalArgumentException(...)` ×2 |
| `EventoMapper.buscarPersona():61` | "persona no encontrada → error" | Mapper | Mismo criterio que `PersonasService` (que ya usa `ValidationException`/`ErrorCatalog.RECURSO_NO_ENCONTRADO`) | `orElseThrow(() -> new IllegalArgumentException(...))` |

No se detectó lógica de negocio "grande" escondida en infraestructura (a diferencia de logística/incentivos) — el servicio es chico y ya está razonablemente bien encapsulado. Los tres hallazgos de esta tabla son de homogeneización, no de relocalización estructural.

**Barrido mecánico de la Fase 0 (sin fixes aplicados):**
```
grep -rn "@Setter" models/                                     → 12 matches (ver §11)
grep -rn "import .*\.\*;" .                                     → 4 matches (2 propios en EventoMapper, 2 de framework en controllers)
grep -rn "throw new IllegalArgumentException" .                 → 2 matches (MedioDeContactoMapper)
grep -rn "new IllegalArgumentException" .                       → 3 matches (+ 1 vía orElseThrow en EventoMapper)
grep -rnE "@(Component|Autowired|Qualifier|Value|Service)" models/ → 0 matches
grep -rnE "@Repository" models/                                  → 2 matches (impl/ — adapters de infra bajo paquete models, ver §9)
grep -rn "instanceof" .                                          → 0 matches
grep -rn "@Scheduled|@Async|@RabbitListener|@KafkaListener|RabbitTemplate" src/main → 0 matches
```

---

## 7. Estados y transiciones

`EstadoNotificacion` (PENDIENTE → ENVIADA | FALLIDA) es un enum simple sin patrón State. La precondición y la decisión de transicionar viven correctamente **dentro** de la entidad `Notificacion`:
- `notificar(NotificacionSender)` decide si pasa a `ENVIADA` (algún medio respondió `true`) o `FALLIDA` (todos fallaron o hubo excepción), y en ambos casos delega en `actualizarEstado()`.
- `actualizarEstado()` es quien realmente muta el estado y registra el `CambioEstadoNotificacion` en el historial — nadie fuera de la entidad decide el estado, solo se le informa el resultado del envío vía `notificar()`. Esto ya cumple con el criterio "¿quién decide invocar la transición?" del plan v2: la propia entidad, correctamente.

No hay transición `FALLIDA → PENDIENTE` (retry) ni ninguna forma de reintento automático — coincide con lo dibujado en el DC (que tampoco la contempla) y con el ADR de estado, que deja el retry expresamente como trabajo futuro no implementado. No es una brecha respecto del DC; es una funcionalidad que ninguna de las dos fuentes reclama todavía.

---

## 8. Domain Events

| Evento | Quién lo crea hoy | Quién lo publica hoy | Cuándo | ¿Coincide con el patrón objetivo? |
|---|---|---|---|---|
| `NotificacionesCreadasEvent` | `NotificacionService` (a mano, `new NotificacionesCreadasEvent(this)`) | `NotificacionService` (mismo método) | Después de `repository.saveAll(notificaciones)` en `procesar()` | **No.** Es un evento de aplicación genérico ("algo se creó"), no un domain event que el agregado `Notificacion` registró sobre sí mismo. No transporta ningún dato del hecho de negocio (ni siquiera qué notificaciones se crearon) — solo dispara a `NotificacionGestor` para que relea *todas* las pendientes de la base. |

No existe ningún otro evento de dominio en el servicio. `EventoNotificable`/`EventoDeDonacion` y sus subtipos **no son domain events** en el sentido DDD — son, como ya documenta `aggregate-servicio-notificaciones.md`, políticas/factorías transitorias sin identidad persistente. Este documento coincide con el código en ese punto y no se cuestiona en esta auditoría.

**Candidato real para Oleada 2:** que `Notificacion` registre sus propios domain events (`NotificacionEnviada`, `NotificacionFallida`, o directamente `NotificacionCreada` en el constructor) vía una lista interna con `getDomainEvents()`/`clearDomainEvents()`, y que `NotificacionService`/`NotificacionGestor` los lean, publiquen y limpien — en vez de que el Service arme un evento vacío de contenido a mano.

---

## 9. Repositories, mappers, clients, listeners/schedulers

| Componente | Tipo | Observación |
|---|---|---|
| `INotificacionRepository`, `IPersonaRepository` | Repository (puerto) | Extienden `CrudRepository<T>` de `common-lib`. Bien ubicados como puertos de dominio. |
| `NotificacionRepositoryEnMemoria`, `PersonaRepositoryEnMemoria` | Repository (adapter, `@Repository`) | Extienden `CrudRepositoryEnMemoria` de `common-lib`. Viven bajo `models/repositories/impl/` — funcionalmente correctos, pero por convención de la arquitectura objetivo (§1 del plan v2: Repositories son adapters) podrían vivir bajo `infrastructure/` como en logística. Bajo impacto — es un patrón replicado también en donaciones/incentivos, no exclusivo de este servicio. |
| `EventoMapper`, `PersonaMapper`, `MedioDeContactoMapper` | Mapper | Sin interfaz explícita (no es bloqueante para mockear, son `@Component` concretos e inyectables igual). |
| `NotificacionRouter` | Infra (adapter + facade) | Implementa `NotificacionSender`, delega en 3 adapters. Ejemplo limpio de Double Dispatch, sin `instanceof`. |
| `CorreoAdapter`/`TelefonoAdapter`/`WhatsAppAdapter` | Infra (puertos) | Cada uno con su mock (`*EnvioMock`) para desarrollo/tests. |
| `NotificacionGestor` | Listener (`@EventListener`, interno al proceso) | No es un listener de mensajería externa — reacciona a un `ApplicationEvent` de Spring dentro del mismo JVM. |
| Schedulers | — | **No existen.** No hay ningún `@Scheduled` en el servicio (barrido `grep` → 0 matches). La Oleada 5 del roadmap genérico no aplica a este servicio. |
| Comunicación entrante | REST síncrono (`NotificacionController`, `PersonasController`) | Confirmado por ADR + ausencia de dependencias de mensajería (§3.3). |
| Comunicación saliente | Ninguna. `notificaciones-service` no llama a otros servicios — solo es invocado. | |

**Códigos HTTP actuales vs. estándar del plan v2:**

| Endpoint | Código actual | Código esperado | 
|---|---|---|
| `POST /notificaciones` (crear notificaciones a partir de un evento) | `200 OK` | `201 Created` o `202 Accepted` (es fire-and-forget desde el punto de vista del llamador) |
| `GET /notificaciones/persona/{id}` | `200 OK` | ✅ correcto |
| `PUT /api/notificaciones/personas` (upsert réplica) | `200 OK` | ✅ correcto |
| `DELETE /api/notificaciones/personas/{id}` (anonimizar) | `200 OK` | `204 No Content` |
| `GET /api/notificaciones/personas/{id}` | `200 OK` | ✅ correcto |

**Trazabilidad / validación transversal:** `GlobalExceptionHandler` de `common-lib` ya cubre `RecursoNoEncontradoException`, `ValidationException`, `BusinessStateException`, `InfrastructureException`, `IllegalArgumentException` y `Exception` genérica (confirmado leyendo el código fuente, no solo el `.puml`). **No** tiene handler para `MethodArgumentTypeMismatchException` ni `DateTimeParseException` — un UUID o una fecha malformada en un path variable de cualquiera de los 2 controllers hoy cae en el handler genérico de `Exception` → `500`. Esto es una brecha de `common-lib`, no exclusiva de notificaciones (aplica a todos los servicios).

No existe `TraceResponseHeaderFilter`/`FeignTraceRequestInterceptor`/uso de `MDC` en ningún servicio del monorepo todavía (barrido `grep -rln "TraceId|X-Trace-Id|MDC"` sobre todo el repo → 0 matches) — confirmado en la Fase 0.5 (§13).

---

## 10. Tests actuales

| Test | Cubre | Observación |
|---|---|---|
| `NotificacionServiceTest` (127 líneas) | `NotificacionService.procesar/obtenerPorPersona` | No verifica el contenido de `NotificacionesCreadasEvent` publicado, solo que se publique algo. |
| `NotificacionGestorTest` (67 líneas) | `NotificacionGestor.notificarPendientes/onNotificacionesCreadas` | — |
| `NotificacionRouterTest` (201 líneas) | Double dispatch de los 3 adapters | Test más grande del módulo, buena cobertura de infraestructura. |
| `NotificacionControllerTest` (158 líneas), `PersonasControllerTest` (64 líneas) | Controllers | — |
| `PersonasServiceTest` (72 líneas) | `PersonasService` (sincronizar/anonimizar/obtener) | — |
| `PersonaTest` (64 líneas) | `Persona` (agregar/quitar/definir predeterminado, anonimizar) | — |
| `MedioDeContactoTest` (113 líneas, en paquete `models/entities/medioDeContacto`) | `MedioDeContacto`/`Correo`/`Telefono` | **Paquete de test no refleja el paquete real** (`models/entities/personas`) — mismatch de convención. |
| `EventosTest` (153 líneas) + `DonanteInactivoTest` (41 líneas) | Jerarquía `EventoNotificable`/`EventoDeDonacion` | Cubren varias subclases juntas; no hay un archivo por clase de evento. |
| `EventoMapperTest` (176 líneas), `PersonaMapperTest` (65 líneas) | Mappers | — |
| `NotificacionRepositoryEnMemoriaTest` (83 líneas), `PersonasRepositoryTest` (71 líneas) | Repos en memoria | — |

**Huecos concretos de cobertura:**
- **No existe `NotificacionTest.java`** dedicado al Aggregate Root principal. `notificar()`, `ordenarMedios()` y `actualizarEstado()` solo se ejercitan indirectamente a través de `NotificacionGestorTest`/`NotificacionRouterTest`/`NotificacionServiceTest`. No hay ningún test que verifique directamente que `historialEstado` acumula las transiciones correctas, ni el caso borde `esPredeterminado == null` (§5.10 más abajo, riesgo de NPE nunca ejercitado por un test).
- No hay test dedicado de `CambioEstadoNotificacion`.
- No hay Object Mothers/Fixtures (`*Mother`/`*Fixtures`) — cada test construye sus propias instancias manualmente. No bloqueante, sí sería útil para Oleada 8.
- `MedioDeContactoTest` vive en un paquete de test que no espeja el paquete de producción.

---

## 11. Deuda respecto del DC (consolidado por categoría)

| Categoría | Hallazgo |
|---|---|
| **Falta implementación** | Domain Events reales sobre `Notificacion` (el DC no lo pide explícitamente, pero es el objetivo transversal del plan v2 para el Aggregate Root principal). |
| **Responsabilidad mal ubicada** | Construcción manual de `NotificacionesCreadasEvent` en el Application Service en vez de en el dominio. Excepciones crudas en `MedioDeContactoMapper`/`EventoMapper` en vez de `ErrorCatalog`. |
| **Modelo divergente respecto del DC (y de los ADRs)** | `WhatsApp` como clase separada (DC/ADR) vs. `TipoTelefono.WHATSAPP` dentro de `Telefono` (código) — el código está más evolucionado que la documentación, no al revés. |
| **Naming / convención** | Paquete de test `medioDeContacto` vs. paquete de producción `personas`. `PersonasController` sin `@RequiredArgsConstructor` (inconsistente con `NotificacionController`). |
| **Acoplamiento inverso / encapsulación** | `@Setter` público en 10 clases de dominio (todas menos `Notificacion` y `Persona`) — las subclases de `EventoNotificable`/`EventoDeDonacion` mutan sus propios atributos heredados llamando setters públicos heredados desde su constructor, en vez de un constructor protegido en la base. `Persona.getMediosDeContacto()` usa `Collections.unmodifiableList` en vez de `List.copyOf` (vista de solo lectura sobre una lista que sigue siendo mutable por dentro — no ejerce el riesgo de `ConcurrentModificationException` hoy porque no hay iteración concurrente conocida, pero es el mismo antipatrón que el catálogo transversal marca como riesgo). |
| **Documentación desactualizada (no es deuda de código)** | `docs/aggregates/aggregate-servicio-notificaciones.md` describe comunicación asíncrona por eventos; el ADR y el código coinciden en que es síncrona por REST. El DC tiene los 3 errores de dibujo del §3.4. No existe `.puml` mantenido a mano ni actualizado (§0). |

---

## 12. Grafo de dependencias (fan-in / fan-out)

```
Persona ←──────────────┐
                        │ (referencia directa a objeto, no solo UUID)
Notificacion ───────────┤
   ↑ usa                │
   │                    │
EventoNotificable (+7 subclases) ──produce──> List<Notificacion>
   │
   └─ construido por EventoMapper (fan-in: 1, fan-out: 8 subclases + IPersonaRepository)

NotificacionService ──usa──> EventoMapper, INotificacionRepository, ApplicationEventPublisher
        │
        └─publishEvent──> NotificacionesCreadasEvent ──escuchado por──> NotificacionGestor
                                                                              │
                                                                              └─usa──> INotificacionRepository, NotificacionSender

NotificacionRouter (implementa NotificacionSender) ──usa──> CorreoAdapter, TelefonoAdapter, WhatsAppAdapter

PersonasService ──usa──> IPersonaRepository, INotificacionRepository (coordina 2 agregados), PersonaMapper
```

**Nodo de mayor fan-out:** `EventoMapper` (construye 8 tipos distintos de evento + depende de `IPersonaRepository`). Es también el nodo con más wildcard imports propios (§6) y el que concentra las 2 excepciones crudas restantes.

**Nodo más frágil para el refactor incremental:** `Notificacion`, porque es el único punto que: (a) referencia directamente a `Persona` (acoplamiento entre agregados, candidato de Oleada 10), (b) es el destino de todas las factorías de `EventoNotificable`, y (c) es donde debería vivir el nuevo mecanismo de Domain Events de Oleada 2 — cualquier cambio ahí afecta a las 8 subclases de evento y a `NotificacionGestor`/`NotificacionService` simultáneamente. Conviene tocarlo en un RF aislado, no en paralelo con otro.

**Nodo más aislado / seguro para trabajar en paralelo:** la jerarquía `MedioDeContacto`/`Correo`/`Telefono` — solo la usa `Notificacion.ordenarMedios()` y `NotificacionRouter`, ninguno de los cuales cambia de contrato si se le quitan los `@Setter` públicos (Oleada 1/3).

---

## 13. Candidatos de slices futuros (RFs)

Numerados en orden propuesto (menor riesgo primero). El orden es una propuesta, no la secuencia final de ejecución.

```text
RF-01 — Tell, Don't Ask en MedioDeContacto/Correo/Telefono
Objetivo: reemplazar la mutación externa vía @Setter de "esPredeterminado" por un método semántico
  (ej. marcarComoPredeterminado()/desmarcarComoPredeterminado()) invocado desde Persona.
Clases afectadas: MedioDeContacto, Correo, Telefono, Persona.
Dependencias: ninguna.
Riesgo: bajo — nodo más aislado del grafo de dependencias (§12).
Motivo del orden: primero, es paralelo-seguro y no toca el Aggregate Root principal.

RF-02 — Domain Events reales en Notificacion (Oleada 2)
Objetivo: que Notificacion registre sus propios domain events (ej. NotificacionCreada/NotificacionEnviada/
  NotificacionFallida) con getDomainEvents()/clearDomainEvents() (List.copyOf), reemplazando la construcción
  manual de NotificacionesCreadasEvent en NotificacionService.
Clases afectadas: Notificacion, NotificacionService, NotificacionGestor.
Dependencias: ninguna (common-lib no tiene AgregadoConEventos<T> que heredar — ver Fase 0.5, §14).
Riesgo: medio — Notificacion es el nodo de mayor fan-in del grafo (§12); tocarlo afecta a NotificacionGestor
  y NotificacionService simultáneamente.
Motivo del orden: es el corazón del refactor v2 para este servicio; conviene hacerlo antes de tocar
  excepciones/validación porque cambia la forma en que se prueba NotificacionService.

RF-03 — Decisión documentada (📝) sobre Domain Events en Persona
Objetivo: registrar explícitamente si Persona (réplica de lectura) necesita Domain Events o no.
Clases afectadas: ninguna (solo documentación).
Dependencias: ninguna.
Riesgo: bajo.
Motivo del orden: evita reabrir la discusión más adelante, igual que se hizo en incentivos para agregados de cálculo.

RF-04 — Copias defensivas reales en Persona.getMediosDeContacto()
Objetivo: reemplazar Collections.unmodifiableList por List.copyOf.
Clases afectadas: Persona.
Dependencias: ninguna.
Riesgo: bajo.
Motivo del orden: mecánico, sin impacto de diseño.

RF-05 — Homogeneizar excepciones crudas en la capa de mappers
Objetivo: MedioDeContactoMapper y EventoMapper.buscarPersona deben lanzar ValidationException/
  RecursoNoEncontradoException con código de ErrorCatalog, igual que ya hace PersonasService.
Clases afectadas: MedioDeContactoMapper, EventoMapper, ErrorCatalog (agregar códigos si faltan).
Dependencias: ninguna.
Riesgo: bajo — ya está mitigado por GlobalExceptionHandler (no produce 500 hoy).
Motivo del orden: sencillo, mejora consistencia antes de la oleada de validación por capas.

RF-06 — Eliminar @Setter público en la jerarquía EventoNotificable/EventoDeDonacion (Oleada 3)
Objetivo: pasar de "this.setPersona(...)" en el constructor de cada subclase a un constructor
  protegido en la clase base que reciba esos valores.
Clases afectadas: EventoNotificable, EventoDeDonacion y las 7 subclases.
Dependencias: ninguna.
Riesgo: medio — toca 9 clases a la vez; conviene un solo PR grande y bien explicado, o dividir por
  jerarquía (EventoDeDonacion y sus 4 hijas en un RF, las 3 hijas directas de EventoNotificable en otro).
Motivo del orden: después de RF-02, para no mezclar el cambio de Domain Events con el de setters en la
  misma revisión.

RF-07 — Guardas de borde en MedioDeContacto.esPredeterminado (Oleada 9.5)
Objetivo: evitar el NPE potencial en ordenarMedios() cuando esPredeterminado llega null desde un DTO
  (MedioDeContactoMapper), y definir un criterio de desempate determinista cuando dos medios empatan.
Clases afectadas: MedioDeContacto, MedioDeContactoMapper, Notificacion.ordenarMedios().
Dependencias: idealmente después de RF-01 (mismo área de código).
Riesgo: bajo, pero es un bug real de borde, no solo estilo.
Motivo del orden: agrupa con el hardening de bordes, después de que la jerarquía de contactos ya
  esté con Tell-Don't-Ask aplicado.

RF-08 — Limpieza de campos muertos en NotificacionService (Oleada 4/7)
Objetivo: eliminar personaRepository y sender del constructor/clase, que nunca se inicializan con un
  valor real y no se usan.
Clases afectadas: NotificacionService.
Dependencias: ninguna.
Riesgo: bajo — no se usan en ningún lado, eliminarlos no cambia comportamiento observable.
Motivo del orden: limpieza pura, se puede hacer en cualquier momento; se ubica después de RF-02 porque
  ese RF ya va a tocar el constructor de NotificacionService.

RF-09 — Validación Bean Validation en los DTOs de entrada (Oleada 9)
Objetivo: @NotNull/@NotBlank/@Valid en los 8 DTOs de dto/input/, PersonaReplicaDTO y
  MedioDeContactoReplicaDTO; @Valid en los 2 controllers.
Clases afectadas: los 10 DTOs + NotificacionController + PersonasController.
Dependencias: ninguna.
Riesgo: bajo — aditivo, no cambia contratos existentes si los llamadores ya envían datos válidos.
Motivo del orden: independiente del resto, se puede paralelizar con RF-01/RF-04.

RF-10 — Idempotencia de ingesta ante reintentos de Feign (Oleada 9.5/10)
Objetivo: agregar una clave de correlación (ej. eventId) a los DTOs de entrada y deduplicar antes de
  generarNotificaciones(), dado que NotificacionesFeignClient ya reintenta (FeignRetryConfig) sin que
  notificaciones-service pueda distinguir un reintento de un evento nuevo.
Clases afectadas: EventoNotificableDTO y sus 8 implementaciones, NotificacionService, esquema de
  persistencia futuro (Oleada 10).
Dependencias: coordinación con el lado emisor (donaciones-service y cualquier otro productor) para que
  empiece a enviar el eventId — cruza el límite del servicio, requiere acuerdo de contrato.
Riesgo: alto — es el único RF de esta lista que no se puede cerrar completo solo dentro de
  notificaciones-service.
Motivo del orden: al final, porque depende de que otros servicios adopten el campo nuevo.

RF-11 — Actualizar docs/aggregates/aggregate-servicio-notificaciones.md (📝, documental)
Objetivo: corregir la descripción de "comunicación asíncrona por eventos" a lo que realmente hay
  (REST síncrono push, async solo del lado del llamador vía @Async, sin cola de mensajería).
Clases afectadas: ninguna (solo el .md).
Dependencias: ninguna.
Riesgo: bajo.
Motivo del orden: se puede hacer en paralelo con cualquier otro RF, no bloquea nada.

RF-12 — Crear/actualizar el .puml mantenido a mano del servicio
Objetivo: crear docs/design/notificaciones-service/diagrama-de-clases-notificaciones.puml (siguiendo el
  formato de logística) corrigiendo los 3 errores de dibujo del §3.4 del CSV original, y regenerar
  target/modelo_tecnico.puml como parte del baseline.
Clases afectadas: ninguna de código.
Dependencias: idealmente después de RF-02/RF-06, para que el diagrama nuevo ya refleje el diseño post-refactor
  y no haya que rehacerlo dos veces.
Riesgo: bajo.
Motivo del orden: al final de la lista corta, pero antes de la Auditoría Final del roadmap completo.
```

---

## 14. Verificación cruzada contra una segunda fuente del DC

La segunda fuente disponible fue `target/modelo_tecnico.puml` (generado, no de diseño). Como se detalla en §0, esa fuente está desactualizada (falta `NotificacionGestor` y `NotificacionesCreadasEvent`) y por eso **no se usó como fuente de verdad** para ninguna sección de este documento — se usó únicamente como apoyo puntual para confirmar firmas de métodos ya visibles en el propio código fuente, nunca para decidir algo que el CSV o el código no confirmaran directamente. No se priorizó por sobre el CSV en ningún punto de este documento.

---

## Checklist de criterios de finalización de Fase 0

- ¿Qué partes del DC ya existen? → §3 (todas las clases de dominio existen; 2 desviaciones puntuales en WhatsApp y en el tipo de `enviarMensaje`).
- ¿Qué partes faltan? → Ninguna clase de dominio del DC falta en el código. Falta la clase `WhatsApp` tal como está dibujada, pero por una decisión de diseño posterior y deliberada (§3.1).
- ¿Qué conceptos existen con otro diseño? → WhatsApp (clase → enum dentro de Telefono), comunicación (eventos → REST síncrono documentado al revés en `aggregate-servicio-notificaciones.md`).
- ¿Dónde está hoy la lógica de negocio? → Correctamente dentro de las entidades (`Notificacion.notificar/ordenarMedios/actualizarEstado`, `Persona.*`, `EventoNotificable.generarNotificaciones` y subclases). Los 3 puntos de fuga están en la capa de mappers (§6), no en Services grandes.
- ¿Qué Services están haciendo demasiado? → Ninguno concentra múltiples responsabilidades divergentes; el hallazgo principal es de deuda técnica puntual (constructor de `NotificacionService`), no de sobre-concentración.
- ¿Qué reglas deberían revisarse para mover al dominio? → La construcción del domain event de `Notificacion` (§8, RF-02).
- ¿Cómo están modelados los estados? → Enum simple + historial inmutable, decisión y transición ya viven en la entidad (§7). Sin deuda.
- ¿Quién genera y publica Domain Events? → Hoy: el Application Service, a mano, sin domain events reales (§8). Debería ser: la entidad `Notificacion`.
- ¿Qué dependencias de infraestructura tiene hoy el dominio? → Ninguna (`grep` de anotaciones Spring en `models/` → 0, salvo los 2 `@Repository` en `impl/`, que son adapters, no POJOs de dominio).
- ¿Qué comportamiento está protegido por tests? → Bien cubierto en Router/Service/Gestor/Controllers/Mappers/Persona; hueco concreto en `Notificacion` (Aggregate Root principal, sin test dedicado) y en `CambioEstadoNotificacion` (§10).
- ¿Qué partes pueden refactorizarse independientemente? → `MedioDeContacto`/`Correo`/`Telefono` (nodo más aislado, §12); validación de DTOs (RF-09); documentación (RF-11, RF-12).
- ¿Cuál sería un orden seguro para comenzar? → RF-01 → RF-02 → (RF-03, RF-04 en paralelo) → RF-05 → RF-06 → RF-07 → RF-08 → RF-09 (paralelizable desde el inicio) → RF-10 → RF-11/RF-12.

---

## Fase 0.5 — Inventario de `common-lib`

No es el primer servicio en refactorizarse (donaciones y logística ya avanzaron varias oleadas; incentivos tiene una rama de refactor propia, `origin/E4_refactor_incentivos`). Se releva qué hay hoy en `common-lib` (idéntico en `main` y en `origin/E4_refactor_incentivos` — no diverge entre ramas):

| Clase/paquete de `common-lib` | ¿Notificaciones ya lo usa? | Qué hacer |
|---|---|---|
| `BusinessStateException`, `ValidationException`, `InfrastructureException`, `RecursoNoEncontradoException`, `DonaTrackException` (abstracta), `ErrorCatalog` | ✅ Parcialmente (`PersonasService` ya usa `ValidationException`+`ErrorCatalog.RECURSO_NO_ENCONTRADO`) | **Reutilizar tal cual**, extender a `MedioDeContactoMapper`/`EventoMapper` (RF-05). Agregar códigos nuevos a `ErrorCatalog` si se necesitan (ej. `MEDIO_DE_CONTACTO_TIPO_NO_SOPORTADO`). |
| `GlobalExceptionHandler` | ✅ Sí (indirectamente, vía `@RestControllerAdvice` global) | **Reutilizar tal cual.** Falta —a nivel `common-lib`, no de este servicio— soporte para `MethodArgumentTypeMismatchException`/`DateTimeParseException` (§9); si se agrega, beneficia a los 4 servicios. |
| `AggregateRoot`, `CrudRepository<T>`, `CrudRepositoryEnMemoria<T>` | ✅ Sí (`Notificacion`, `Persona` implementan `AggregateRoot`; los repos en memoria extienden `CrudRepositoryEnMemoria`) | **Reutilizar tal cual.** |
| `ControllerLoggingInterceptor`, `ServiceLoggingAspect`, `ScheduledJobLoggingAspect`, `LoggingAutoConfiguration` | ✅ Sí (auto-configurado vía `CommonLibAutoConfiguration`) | **Reutilizar tal cual.** `ScheduledJobLoggingAspect` no tiene nada que interceptar en este servicio (no hay `@Scheduled`), sin impacto. |
| `DonaTrackOpenApiAutoConfiguration`/`Properties` | ✅ Sí (autoconfig) | **Reutilizar tal cual.** |
| **`AgregadoConEventos<T>`** (o equivalente base para Domain Events) | ❌ No existe en `common-lib` todavía | **Construir localmente en notificaciones-service** para RF-02 (`Notificacion` implementa su propio `domainEvents`/`getDomainEvents()`/`clearDomainEvents()` con `List.copyOf`), documentando la intención de que sea candidato a extraerse a `common-lib` cuando un segundo servicio lo necesite — igual que hoy `donaciones-service/Propuesta.java` ya implementó su propia versión (usando `@Getter` de Lombok sobre la lista mutable directamente, **sin** `List.copyOf` — antipatrón real ya presente en el monorepo, fuera del alcance de esta auditoría porque pertenece a `donaciones-service`, pero vale la nota para no copiar ese patrón). |
| **`TraceResponseHeaderFilter`, `FeignTraceRequestInterceptor`** (trazabilidad `X-Trace-Id`/MDC) | ❌ No existen en `common-lib`, en ningún servicio del monorepo | No hay nada que reutilizar todavía. Si se construye en este servicio, documentarlo pensando en que sea la base para los demás (Oleada 9). Si otro servicio lo construye primero, **Oleada 11 (condicional)** de este roadmap deberá adoptarlo acá. |
| **`ShedLock`/coordinación de schedulers** | No aplica — este servicio no tiene schedulers. | Sin acción. |

**Conclusión de Fase 0.5:** `common-lib` está en un estado bastante temprano (esencialmente: excepciones + logging + OpenAPI + persistencia en memoria). No hay abstracciones de Domain Events ni de trazabilidad para heredar todavía — notificaciones-service construiría, no heredaría, si decide avanzar con RF-02/Oleada 9 antes que los demás servicios.

---

## Fin de la Fase 0

No se implementó ninguna de las recomendaciones listadas arriba. Este documento es exclusivamente de auditoría.
