# Auditoría Final — `logistica-service`

> Contrasta el código final en `E4_refactor_logistica` (commit `1f7906fe`) contra la Fase 0 del plan (`plan-refactor-logistica-service.md`). Mismo formato de evidencia concreta: file path + método o línea relevante donde aplica.

**Fecha:** 2026-08-27  
**Branch:** `E4_refactor_logistica`  
**Commit final:** `1f7906fe`  
**Suite:** 291 tests — 0 fallos — 0 errores — 1 skipped (deuda de altura del camión, `@Disabled` intencional desde Oleada 8)

---

## Checklist específico del plan

### ✅ GestorDeEntregas / GestorDeRutas / GestorDeCamiones implementados

| Gestor | Archivo | Métodos |
|---|---|---|
| `GestorDeEntregas` | `models/entities/entregas/GestorDeEntregas.java` | `cambiarEstado(SolicitudTransicionEntrega)` — switch sobre sealed interface (`ConfirmacionRecepcion`, `NoRecepcion`, `RegresoDeposito`) |
| `GestorDeRutas` | `models/entities/rutas/GestorDeRutas.java` | `agregarEntrega(Ruta, Entrega)`, `iniciarRuta(Ruta, Camion, Chofer)`, `iniciarRuta(Ruta, Camion, Chofer, List<Entrega>, String)`, `completarRuta(Ruta, Camion, Chofer)` |
| `GestorDeCamiones` | `models/entities/camiones/GestorDeCamiones.java` | `procesarSolicitudNuevoCamion(SolicitudNuevoCamion)`, `cambiarEstado(Camion, EstadoCamion)` |

Los tres son clases finales con constructor privado (utilidades de dominio puro, sin `@Component`). `CamionesService`, `ChoferService` y `EntregasService` delegan en ellos — el switch de `CamionesService.cambiarEstado` (RF-05, hallazgo §6 original) ya no existe en esos services.

---

### ✅ Entrega y Ruta generan sus propios Domain Events

**`Entrega`** (`models/entities/entregas/Entrega.java`):
- Extiende `AgregadoConEventos<EventoEntrega>` (base de `common-lib`)
- `confirmarEntrega(actor)` → `registrarEvento(new EntregaConfirmada(...))`
- `negarEntrega(actor, justificacion, replanificable)` → `registrarEvento(new EntregaFallida(...))`
- `EntregasService` ejecuta: método de dominio → `save` → `getDomainEvents()` → publicar → `clearDomainEvents()` — el pattern objetivo.

**`Ruta`** (`models/entities/rutas/Ruta.java`):
- Extiende `AgregadoConEventos<EventoRuta>`
- `agregarEntrega(UUID entregaId)` → `registrarEvento(new EventoRutaAsignada(this.id, entregaId))`
- `iniciarRuta()` → `registrarEvento(new EventoRutaIniciada(...))`
- `RutasService` y `PlanificacionService` siguen el mismo ciclo: dominio → persist → `getDomainEvents()` → publicar → `clearDomainEvents()`.

Cuatro eventos de dominio activos (§8 de la Fase 0 tenía 0). Ningún Application Service construye ya el payload del evento manualmente.

---

### ✅ Duplicación agregarEntrega / guardarRutaPlanificada resuelta

El hallazgo §1.3 identificó dos implementaciones independientes de "asignar entrega(s) a ruta + publicar evento". Estado final:

- **`RutasService.agregarEntrega`**: llama a `GestorDeRutas.agregarEntrega(ruta, entrega)` — punto único de coordinación.
- **`PlanificacionService.procesarCallback`**: llama a `generadorDeRutas.calcularRutas(respuesta)` que internamente invoca `GestorDeRutas.agregarEntrega(ruta, entrega)` dentro de `GeneradorDeRutas.crearRuta(...)`.

Ambos caminos de entrada (manual vía API y automático vía callback) pasan por el mismo `GestorDeRutas.agregarEntrega`. La tercera variante que existía en el `GeneradorDeRutas` original / `PlanificacionService.guardarRutaPlanificada` fue eliminada en las Oleadas 3/5.

---

### ✅ Scheduler PlanificadorDeEntregas sin lógica de negocio

`PlanificadorDeEntregas.ejecutar()` (`schedulers/PlanificadorDeEntregas.java`):

```java
@Scheduled(cron = "${logistica.planificacion.cron.expression:0 0 2 * * ?}")
public void ejecutar() {
    planificacionService.iniciarPlanificacion();
}
```

Una línea. Todo el filtrado de entregas pendientes, camiones disponibles, particionado en lotes y creación de `SolicitudPlanificacion` vive ahora en `PlanificacionService.iniciarPlanificacion()` (Oleada 5). El bloque `// INICIO/FIN LOGICA DE NEGOCIO` original que estaba en el scheduler ya no existe.

---

### ✅ Historial de estado en Ruta, Camion y Chofer

| Entidad | Record de cambio | Campos | Registro en transiciones |
|---|---|---|---|
| `Camion` | `CambioEstadoCamion` | `EstadoCamion estadoAnterior, estadoNuevo, LocalDateTime timestamp` | `habilitar()`, `deshabilitar()`, `asignarARuta()`, `completarRuta()` |
| `Chofer` | `CambioEstadoChofer` | `EstadoChofer estadoAnterior, estadoNuevo, LocalDateTime timestamp` | ídem |
| `Ruta` | `CambioEstadoRuta` | `EstadoRuta estadoAnterior, estadoNuevo, LocalDateTime timestamp` | `actualizarEstado()` (llamado desde `iniciarRuta()` y `completarRuta()`) |

`CambioEstadoCamion` resolvió la inconsistencia §1.9 del plan original: los campos tipan `EstadoCamion` (no `EstadoRuta` como mostraba el DC). Los tres históricos exponen `List.copyOf(historialEstado)` — defensivo, igual que `Entrega`.

---

### ✅ Divergencia del bloque PLANIFICADOR_EXTERNO documentada

La divergencia §1.6 (DC síncrono en memoria vs. código async-con-callback) se mantiene como decisión de diseño; el DC no fue actualizado (no hubo conversación que lo cambiara). Estado de documentación:

- **`PlanificacionController`**: docstring explica que la creación de solicitudes viene solo del scheduler interno y que el controller solo expone el callback.
- **`ProveedorExternoPlanificacionSimulado`** (`infrastructure/clients/`): nombre y ubicación dejan claro el rol de simulador. El cliente HTTP real (`RestTemplate`) quedó en `infrastructure/clients/` — el paquete que antes estaba vacío.
- **Plan original §1.6**: marcado como "divergencia consciente y bien documentada, no un bug".

**Pendiente abierto**: el DC formal sigue mostrando `PlanificadorDeRutas` como componente síncrono. Quien mantenga el DC debería actualizarlo para reflejar el diseño async-con-callback antes de la Oleada 10 (persistencia real).

---

### ✅ Cobertura de tests en las zonas que eran puntos ciegos

Todas tenían cobertura cero al inicio del refactor (§10 de la Fase 0).

| Clase | Tests antes | Tests ahora | Qué cubre |
|---|---|---|---|
| `PlanificacionService` | 0 | 6 | Sin entregas, sin camiones, partición 70→50+20, 1 lote, procesarCallback exitoso, idempotencia de callback sobre solicitud PROCESADA |
| `PlanificacionController` | 0 | 5 | POST `/resultados` (200/400/404), GET `/planificaciones/{id}` (200/404) |
| `SolicitudPlanificacion` | 0 | 8 | `procesarResultados`, `marcarError`, `reintentar`, guardas de transición, constructor |
| Mappers (×6) | 0 | 2–3 c/u | Round-trip DTO↔dominio, nulos |
| `Direccion` (VO) | 0 | 6 | Validaciones del constructor + `anonimizar()` toda la jerarquía |

Test de idempotencia del callback (pedido en §5.4 de la Fase 0): existe en `PlanificacionServiceTest.procesarCallbackSobreUnaSolicitudYaProcesadaDevuelveSinReprocesar`. Verifica con `verify(rutasRepository, never()).save(any())` que ninguna ruta se crea en reprocesado.

---

## Contraste por oleada: objetivo vs. resultado

### Oleada 1 — Historial de estado en Camion y Chofer (RF-02)

**Objetivo:** `CambioEstadoCamion` / `CambioEstadoChofer`, `historialEstado` aditivo, getter con copia defensiva, tests.

**Resultado:** ✅ completo. Los tres records existen con `LocalDateTime timestamp`. Getters con `List.copyOf`. El renombre `asignarARuta` → `asignarRuta` fue evaluado y **no ejecutado** — el costo de actualizar call sites en Oleadas 3/5 no justificaba el cambio con las entregas pendientes.

---

### Oleada 2 — Domain Events en Entrega (RF-03)

**Objetivo:** `Entrega` genera `EventoEntregaExitosa`/`Fallida`; `EntregasService` sigue ciclo persist→getDomainEvents→publish→clear.

**Resultado:** ✅ completo. `Entrega` extiende `AgregadoConEventos<EventoEntrega>`. Los eventos se llaman `EntregaConfirmada` / `EntregaFallida` (nombres más expresivos que los del DC). La decisión sobre separar Domain Event / Integration DTO fue documentada: en este servicio el mismo objeto sirve para ambos roles por ser chico.

---

### Oleada 3 — Domain Events en Ruta + GestorDeRutas (RF-04)

**Objetivo:** `Ruta` genera `EventoRutaAsignada`/`Iniciada`; `GestorDeRutas` absorbe coordinación multi-entidad; duplicación `agregarEntrega`/`guardarRutaPlanificada` resuelta.

**Resultado:** ✅ completo. `GestorDeRutas` tiene 4 métodos públicos. `RutasService` y `PlanificacionService` convergen en el mismo gestor. Forma canónica de "crear Ruta con entregas" queda en `GestorDeRutas.agregarEntrega` invocado desde `GeneradorDeRutas.crearRuta`.

---

### Oleada 4 — GestorDeCamiones + cambiarEstado en Chofer (RF-05)

**Objetivo:** `GestorDeCamiones.cambiarEstado`; `Chofer.cambiarEstado(EstadoChofer)` directo en la entidad; `ValidadorPatentes` a dominio puro.

**Resultado:** ✅ completo. Switch de `CamionesService` movido a `GestorDeCamiones.cambiarEstado`. Para `Chofer` el switch fue movido directamente al método `Chofer.cambiarEstado(EstadoChofer)` en la entidad (el DC lo pedía así). `ValidadorPatentes` vive en `models/entities/camiones/` sin `@Component`.

---

### Oleada 5 — Sacar lógica de negocio del scheduler (RF-06)

**Objetivo:** `PlanificadorDeEntregas.ejecutar()` = 1 línea; lógica en `PlanificacionService`; `GeneradorDeRutas` separado de cliente HTTP; doble filtrado de disponibles eliminado; characterization tests portados.

**Resultado:** ✅ completo. Scheduler = 1 línea. `ProveedorExternoPlanificacionSimulado` en `infrastructure/clients/` separa el algoritmo puro del cliente HTTP. `PlanificacionService.iniciarPlanificacion` centraliza toda la lógica. El doble filtrado (scheduler + GeneradorDeRutas) fue eliminado — `PlanificacionService` filtra una sola vez.

---

### Oleada 6 — Reorganización de paquetes

**Objetivo:** mover a `models/` algoritmos Strategy, gestores, `ValidadorPatentes`; `infrastructure/clients/` para el cliente HTTP; eliminar paquete `routes/` vacío.

**Resultado:** ✅ completo. `AlgoritmoOrdenadorSimple`, `AsignadorDeEntregasPorDimension`, `PlanificadorDeRutas`, gestores, `ValidadorPatentes` todos en `models/entities/*`. `ProveedorExternoPlanificacionSimulado` en `infrastructure/clients/`. Paquete `routes/` eliminado.

---

### Oleada 7 — Limpieza legacy (RF-07, RF-08)

**Objetivo:** wildcard imports → 0 en controllers; getters redundantes de `Ruta` unificados; `EntregaPlanificadaDTO`/`SolicitudPlanificacionRequestDTO` resueltos; `findDisponibles()` reutilizado; `anonimizar()` en `Direccion`; `RutaMapper` decisión semántica movida a `Ruta.tieneSeguimientoDisponible()`.

**Resultado:** ✅ casi completo.
- Wildcard imports: **0 en los 5 controllers**. Quedan en `services/impl/EntregasService`, `services/IEntregasService`, `services/impl/RutasService`, `services/IRutasService` — estaban fuera del scope original de RF-07 (que apuntaba solo a los 4 controllers de origen).
- `Ruta` getters unificados: `getEntregaIds()` es el único. `getEntregas()`/`obtenerEntregas()` eliminados.
- `EntregaPlanificadaDTO` / `SolicitudPlanificacionRequestDTO`: **eliminados** (código muerto confirmado, sin call site en `src/main`).
- `anonimizar()` en `Direccion`/`Localidad`/`Provincia`/`Pais`: ✅ implementado.
- `RutaMapper.calcularUrlSeguimiento` → `Ruta.tieneSeguimientoDisponible()`: ✅ el método existe en `Ruta` y el mapper lo usa.

---

### Oleada 8 — Testing profundo (huecos de Planificación)

**Objetivo:** Object Mothers, tests de zonas sin cobertura, casos borde, `@Disabled` de altura, reemplazar mocks de `Entrega` por instancias reales.

**Resultado:** ✅ completo. 5 Object Mothers, 8 suites nuevas o ampliadas, casos borde cubiertos (exactamente 100 entregas, límite exacto de capacidad, callback duplicado, `reintentar`). `mock(Entrega.class)` reemplazado por `EntregaMother.pendiente()` en `PlanificacionServiceTest`. `@Disabled` documenta la deuda de altura.

---

### Oleada 9 — Validación por capas + HTTP

**Objetivo:** Bean Validation en todos los request DTOs, `@Valid`/`@Validated` en controllers, verificar que `GlobalExceptionHandler` esté activo.

**Resultado:** ✅ completo. 15 DTOs anotados. 5 controllers con `@Validated` + `@Valid`. `GlobalExceptionHandler` activo vía `scanBasePackages = "grupo5"` — maneja `MethodArgumentNotValidException` (400), `MethodArgumentTypeMismatchException` (400 por UUID malformado), `HttpMessageNotReadableException` (400 por JSON roto). `DtoValidationTest` (36 tests) + `ValidacionHttpTest` (14 tests con `LocalValidatorFactoryBean`). 3 controller tests ajustados: sus stubs de service quedaban innecesarios porque Bean Validation ahora produce el 400 antes de llegar al service.

---

### Oleada 9.5 — Hardening de bordes

**Objetivo:** `CallerRunsPolicy`, `TaskDecorator` MDC, test de idempotencia, documentar criterio de orden de `AlgoritmoOrdenarSimple`.

**Resultado:** ✅ completo. `CallerRunsPolicy` en `AsyncConfig`. `TaskDecorator` copia `MDC.getCopyOfContextMap()` al hilo async y lo limpia en `finally`. Test de idempotencia ya existía desde Oleada 8 (verificado). `AlgoritmoOrdenarSimple` tiene Javadoc 📝 con la deuda conocida.

---

## Qué quedó pendiente

| Ítem | Dónde estaba en el plan | Estado |
|---|---|---|
| Wildcard imports en `services/impl/EntregasService`, `services/IRutasService`, etc. | RF-07 apuntaba a los 4 controllers (resuelto); los services no estaban en scope | Pendiente si se desea consistencia total |
| Nombre `asignarARuta` → `asignarRuta` | Evaluado en Oleada 1, no ejecutado | Pendiente (deuda de naming menor) |
| DC actualizado para reflejar diseño async-con-callback | §1.6 — decisión consciente de no actualizar sin conversación con quien mantiene el DC | Pendiente |
| Restricción de altura de camión en `AsignadorDeEntregasPorDimension` | §1.5 y `@Disabled` en suite | Pendiente hasta que `Entrega` modele `alturaM` |
| `POST /api/logistica/callback/rutas` sin verificación de origen | §9 nota de seguridad | Pendiente — fuera de scope arquitectónico de este plan |
| Oleada 10 (persistencia real: ORM, `@Embeddable`, `@Version`, Transactional Outbox) | Oleada 10 del roadmap | No iniciada — requiere infraestructura de base de datos |

---

## Qué se completó respecto a la Fase 0

Todos los hallazgos de la tabla §6 original (lógica de negocio fuera del dominio) fueron resueltos:

- ✅ Coordinación multi-entidad de `iniciar`/`completar` → `GestorDeRutas`
- ✅ Construcción manual de eventos → domain events propios de `Entrega` y `Ruta`
- ✅ Duplicación `agregarEntrega`/`guardarRutaPlanificada` → punto único en `GestorDeRutas.agregarEntrega`
- ✅ Switch `cambiarEstado` duplicado → `GestorDeCamiones` y `Chofer.cambiarEstado`
- ✅ Lógica en scheduler → `PlanificacionService.iniciarPlanificacion`
- ✅ Decisión de `RutaMapper.calcularUrlSeguimiento` → `Ruta.tieneSeguimientoDisponible()`
- ✅ `ValidadorPatentes` en `services/impl` → `models/entities/camiones/`
- ✅ Algoritmos Strategy en `services/impl` → `models/entities/planificacion/`
- ✅ `EntregasService` dependía de `IRutasService` (Service→Service) → usa `ICamionRepository` directamente

**El dominio es autónomo, los Application Services orquestan sin decidir, el scheduler dispara sin pensar. El código final coincide con los principios transversales que el plan estableció desde la Fase 0.**
