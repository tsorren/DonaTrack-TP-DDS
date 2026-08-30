# Diagnóstico Arquitectónico de DonaTrack

> [!NOTE]
> Este documento es un **diagnóstico de estado actual**, no un plan de refactor. Sirve como base para diseñar intervenciones controladas en una etapa posterior.

---

## 1. Resumen Ejecutivo

1. **El proyecto está significativamente más alineado con el modelo de referencia de lo que suele verse en proyectos académicos.** Las entidades de dominio son ricas en comportamiento, no meramente anémicas.

2. **`DonacionIndependiente` implementa correctamente State Pattern** con transiciones encapsuladas en clases de estado concretas (`EnDeposito`, `AsignacionRealizada`, `ListaParaEntregar`, etc.), historial de cambios y validaciones de transición inválida.

3. **`Donacion` usa transiciones simples pero correctas** (`marcarNormalizada()`, `marcarSegmentada()`) con guardas explícitas sobre el estado actual.

4. **El principal foco de preocupación es `ProcesadorDeDonaciones`**: una clase ubicada en `infrastructure/` que orquesta normalización, segmentación e incentivos, mezclando responsabilidades de Application Service con ubicación de infraestructura.

5. **`DonacionesIndependientesService.cambiarEstado()`** mezcla orquestación de casos de uso heterogéneos (asignar, trasladar, entregar, fallar) en un único método con un `switch` extenso y coordinación con múltiples microservicios.

6. **`AlgoritmosService`** cumple una doble función: ejecuta algoritmos de asignación (responsabilidad de dominio/application) y gestiona propuestas (CRUD + eventos + notificaciones), lo cual reduce su cohesión.

7. **`SegmentacionEventListener`** (infrastructure) contiene lógica de coordinación compleja que parece de Application Service: obtiene donantes, personas, categorías, registra en incentivos y persiste donaciones independientes.

8. **Los Controllers están correctamente implementados** como adaptadores HTTP puros, sin lógica de negocio.

9. **Los Repositories están correctamente abstraídos**: interfaces en `models/repositories/` con implementaciones en memoria en `impl/`, usando un `CrudRepository<T extends AggregateRoot>` genérico.

10. **Falta un diagrama PlantUML para `donaciones-service`**, que es el servicio con mayor complejidad de dominio. Solo existe diagrama para `logistica-service`.

---

## 2. Mapa Arquitectónico Actual

### Estructura general del proyecto (multi-servicio)

```text
┌────────────────────────────────────────────────────────┐
│                    DonaTrack                           │
├──────────────────┬──────────────────┬──────────────────┤
│ donaciones-svc   │ logistica-svc    │ notificaciones   │
│ (65 clases)      │ (70 clases)      │ (55 clases)      │
├──────────────────┼──────────────────┼──────────────────┤
│ incentivos-svc   │ common-lib       │ auth-service     │
│ (47 clases)      │ (20 clases)      │ (vacío/.gitkeep) │
├──────────────────┴──────────────────┴──────────────────┤
│ integration-tests │ cliente-liviano │ docs/adr (51)    │
└────────────────────────────────────────────────────────┘
```

### Flujo de responsabilidades en `donaciones-service`

```text
Controller (adaptador HTTP puro)
    │
    ▼
Service (@Service - orquestación de caso de uso)
    │
    ├──▶ Repository (interfaz en models/repositories/)
    │          └── impl EnMemoria (models/repositories/impl/)
    │
    ├──▶ Entity de dominio (comportamiento + estado)
    │
    ├──▶ Mapper (services/mappers/)
    │
    └──▶ Infrastructure
              ├── Feign Clients (incentivos, notificaciones, logística)
              ├── ProcesadorDeDonaciones (normalización + segmentación async)
              ├── Algoritmos de asignación
              ├── Segmentadores
              ├── Event Listeners (RabbitMQ, Spring Events)
              └── Analizadores semánticos
```

### Flujo en `logistica-service`

```text
Controller (adaptador HTTP)
    │
    ▼
Service (@Service - orquestación)
    │
    ├──▶ Entity con comportamiento rico
    │       (Entrega.iniciarRuta(), Ruta.completarRuta(), etc.)
    │
    ├──▶ Repository (interfaz → impl EnMemoria)
    │
    ├──▶ LogisticaEventPublisher (RabbitMQ)
    │
    └──▶ Algoritmos (GeneradorDeRutas, AsignadorDeEntregas)
```

> [!IMPORTANT]
> **Observación clave:** El servicio de logística tiene la separación más limpia de responsabilidades. Las transiciones de estado están en las entidades, los services orquestan, y la infraestructura publica eventos. Puede servir como referencia interna.

---

## 3. Inventario de Componentes

### donaciones-service

| Componente | Ubicación | Rol actual | Rol conceptual | Estado |
|---|---|---|---|---|
| `DonacionesController` | `controllers/impl/` | Adaptador HTTP | Presentation | ✅ Correcto |
| `DonacionesIndependientesController` | `controllers/impl/` | Adaptador HTTP | Presentation | ✅ Correcto |
| `AsignacionController` | `controllers/impl/` | Adaptador HTTP | Presentation | ✅ Correcto |
| `PropuestasController` | `controllers/impl/` | Adaptador HTTP | Presentation | ✅ Correcto |
| `DonacionesService` | `services/impl/` | Orquestación: cargar/listar donaciones | Application | ✅ Correcto |
| `DonacionesIndependientesService` | `services/impl/` | Orquestación: cambios de estado + notificaciones | Application + Coordinación | ⚠️ Cohesión baja |
| `AlgoritmosService` | `services/impl/` | Ejecución de algoritmos + gestión propuestas | Application + Dominio mixto | ⚠️ Cohesión baja |
| `PropuestaService` | `services/impl/` | Fachada de asignación, event listener | Application | ⚠️ Doble rol |
| `NecesidadesService` | `services/impl/` | CRUD necesidades | Application | ✅ Correcto |
| `ProcesadorDeDonaciones` | `infrastructure/` | Normalización + segmentación async | Application (ubicado en Infra) | ❌ Mal ubicado |
| `SegmentacionEventListener` | `infrastructure/events/` | Segmentar + registrar incentivos + persistir | Application (ubicado en Infra) | ❌ Orquestación en Infra |
| `LogisticaEventListener` | `infrastructure/` | Receptor RabbitMQ → cambios estado | Infra/Adaptador | ⚠️ Aceptable |
| `Donacion` | `models/entities/donaciones/` | Aggregate Root con transiciones | Domain | ✅ Correcto |
| `DonacionIndependiente` | `models/entities/donacionesIndependientes/` | Aggregate Root + State Pattern | Domain | ✅ Correcto |
| `Necesidad` (abstract) | `models/entities/necesidades/` | Aggregate Root con comportamiento | Domain | ✅ Correcto |
| `Propuesta` | `models/entities/propuestas/` | Aggregate Root + domain events | Domain | ✅ Correcto |
| `AlgoritmoAsignacion` | `infrastructure/algoritmos/` | Template Method para asignación | Domain/Application | ⚠️ Ubicación discutible |
| Feign Clients | `infrastructure/clients/` | Comunicación inter-servicios | Infrastructure | ✅ Correcto |
| Repositories (interfaces) | `models/repositories/` | Contrato de persistencia | Domain | ✅ Correcto |
| Repositories (impl) | `models/repositories/impl/` | Almacenamiento en memoria | Infrastructure | ✅ Correcto |
| Mappers | `services/mappers/` | DTO ↔ Entity | Application/Presentation | ✅ Correcto |

### logistica-service

| Componente | Ubicación | Rol actual | Rol conceptual | Estado |
|---|---|---|---|---|
| Controllers (5) | `controllers/impl/` | Adaptador HTTP | Presentation | ✅ Correcto |
| `EntregasService` | `services/impl/` | Orquestación + eventos | Application | ✅ Correcto |
| `RutasService` | `services/impl/` | Orquestación + eventos | Application | ✅ Correcto |
| `Entrega` | `models/entities/entregas/` | Entity con estado rico | Domain | ✅ Correcto |
| `Ruta` | `models/entities/rutas/` | Entity con comportamiento | Domain | ✅ Correcto |
| `Camion` | `models/entities/camiones/` | Entity con estado | Domain | ✅ Correcto |
| `LogisticaEventPublisher` | `infrastructure/` | Publicador RabbitMQ | Infrastructure | ✅ Correcto |
| `GeneradorDeRutas` | `services/impl/` | Algoritmo de generación | Domain Service | ✅ Correcto |

### notificaciones-service

| Componente | Ubicación | Rol actual | Rol conceptual | Estado |
|---|---|---|---|---|
| `NotificacionService` | `services/` | Orquestación notificaciones | Application | ✅ Correcto |
| `Notificacion` | `models/entities/` | Entity con `notificar()` | Domain | ✅ Correcto |
| `NotificacionSender` | `models/ports/` | Port de envío | Domain port | ✅ Correcto |
| `NotificacionRouter` | `infrastructure/` | Router → adapters concretos | Infrastructure | ✅ Correcto |
| Adapters (Correo, Telefono, WhatsApp) | `infrastructure/` | Adaptadores de envío | Infrastructure | ✅ Correcto |

### incentivos-service

| Componente | Ubicación | Rol actual | Rol conceptual | Estado |
|---|---|---|---|---|
| `IncentivosService` | `services/` | Orquestación donaciones + métricas | Application | ✅ Correcto |
| `DonanteIncentivos` | `models/entities/donante/` | Aggregate Root con lógica rica | Domain | ✅ Correcto |
| `Mision` (+ subclases) | `models/entities/donante/misiones/` | Template Method para misiones | Domain | ✅ Correcto |
| `CriterioInactividad` | `models/entities/inactividad/` | Strategy para inactividad | Domain | ✅ Correcto |
| Jobs (3) | `jobs/` | Tareas programadas | Application/Infra | ✅ Correcto |

---

## 4. Diagnóstico de Controllers

### Conclusión general: Controllers bien implementados

Los controllers en todos los servicios actúan como adaptadores HTTP puros. Reciben DTOs, delegan a services e interfaces, y retornan `ResponseEntity`. No contienen lógica de negocio.

**Observación menor (BAJO, Confianza Alta):**
- `AsignacionController` inyecta la clase concreta `PropuestaService` en vez de una interfaz. Todos los demás controllers usan interfaces de service.

**Evidencia:**
```java
// AsignacionController.java L21
private final PropuestaService propuestaService; // clase concreta
// vs DonacionesController.java L22
private final IDonacionesService service;         // interfaz
```

---

## 5. Diagnóstico de Application Services / `@Service`

### 5.1 `DonacionesService`

- **Casos de uso:** Cargar donación, listar donaciones, obtener donación.
- **Dependencias:** `IDonacionesRepository`, `IDonantesRepository`, `DonacionMapper`, `ProcesadorDeDonaciones`
- **Cohesión:** Alta — los métodos son CRUD coherente sobre `Donacion`.
- **¿Funciona como Facade?** Sí, delega normalización async a `ProcesadorDeDonaciones`.
- **Severidad:** Sin problemas.
- **Confianza:** Alta.

### 5.2 `DonacionesIndependientesService` ⚠️

- **Casos de uso:** Cambiar estado de donación independiente (múltiples casos en uno).
- **Dependencias:** 9 dependencias inyectadas (repositorios, feign clients, mapper, otro service).
- **Problema principal:** El método `cambiarEstado()` agrupa **6 casos de uso distintos** en un único `switch`:
  - `ASIGNACION_REALIZADA` → asignar
  - `VENCIDA` → vencer
  - `LISTA_PARA_ENTREGAR` → planificar ruta
  - `EN_TRASLADO` → iniciar recorrido + notificar
  - `ENTREGADA` → confirmar + registrar incentivos + notificar
  - `ENTREGA_FALLIDA` → registrar falla + notificar

**Evidencia:**
```java
// DonacionesIndependientesService.java L74-86
switch (request.estado()) {
  case TipoEstadoDonacion.ASIGNACION_REALIZADA -> asignarDonacion(actor, donacion, request);
  case TipoEstadoDonacion.VENCIDA -> donacion.vencer(actor);
  case TipoEstadoDonacion.LISTA_PARA_ENTREGAR -> donacion.planificarRuta(actor);
  case TipoEstadoDonacion.EN_TRASLADO -> procesarDonacionEnTraslado(...);
  case TipoEstadoDonacion.ENTREGADA -> procesarDonacionEntregada(...);
  case TipoEstadoDonacion.ENTREGA_FALLIDA -> procesarEntregaFallida(...);
  ...
}
```

**Análisis método por método:**

| Método | Tipo responsabilidad | Problema |
|---|---|---|
| `cambiarEstado()` | Application (orquestación) | Mezcla 6 flujos distintos |
| `asignarDonacion()` | Application + Domain | Correcto: busca necesidad, delega a entidad |
| `procesarDonacionEnTraslado()` | Application + Infra | Llama dominio + Feign client directamente |
| `procesarDonacionEntregada()` | Application + Infra | Llama dominio + Feign x2 (incentivos + notificaciones) |
| `procesarEntregaFallida()` | Application + Infra | Llama dominio + Feign |
| `obtenerDonanteId()` | Navigation/Query | Traversal: DonacionIndep → Donacion → Donante |
| `obtenerPersonaDonanteId()` | Navigation/Query | Traversal: Donante → personaId |
| `obtenerOrganizacionId()` | Navigation/Query | Traversal: Asignable → Necesidad → entidadId |
| `obtenerPersonaBeneficiariaId()` | Navigation/Query | Traversal: Necesidad → Entidad → juridicaId |

**Severidad: ALTA**
**Confianza: Alta**

**Observación:** Los métodos `obtenerDonanteId()`, `obtenerPersonaDonanteId()`, etc. realizan navegación relacional cruzando varios aggregates para armar DTOs de notificación. Este traversal se repite en `SegmentacionEventListener` y `AlgoritmosService`.

---

### 5.3 `AlgoritmosService` ⚠️

- **Casos de uso:** Ejecutar algoritmos de asignación, listar propuestas, actualizar estado de propuesta.
- **Dependencias:** 10 dependencias inyectadas.
- **Problema principal:** Mezcla ejecución de algoritmos con gestión de propuestas y notificaciones.

**Análisis método por método:**

| Método | Tipo responsabilidad | Problema |
|---|---|---|
| `ejecutar()` | Application + Domain | Correcto como orquestación, pero `consolidar()` es lógica de dominio incrustada |
| `consolidar()` | Domain (static) | Regla de negocio de consolidación de propuestas. Es estática y privada, pero pertenece al dominio |
| `listarPropuestas()` | Application (delegación) | Correcto |
| `actualizarEstadoPropuesta()` | Application | Usa `ResponseStatusException` (presentation concern) |
| `aprobarPropuesta()` | Application + Infra | Domain events + Feign notifications |
| `notificarFragmentacion()` | Infra (notificación) | Traversal complejo: DI → Donacion → Donante → Persona |

**Evidencia de `ResponseStatusException` en Application Service:**
```java
// AlgoritmosService.java L174
.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
// vs. el patrón correcto usado en otros services:
.orElseThrow(() -> new RecursoNoEncontradoException(id));
```

**Severidad: ALTA** (cohesión baja, `ResponseStatusException` en service)
**Confianza: Alta**

---

### 5.4 `PropuestaService` ⚠️

- **Casos de uso:** Ejecutar asignación, listar propuestas, actualizar estado, manejar evento `PropuestaAprobada`.
- **Dependencias:** 9 dependencias.
- **Problema:** Combina responsabilidades de fachada (delegando a `AlgoritmosService`) con event handling (`@EventListener onPropuestaAprobada`).

**Análisis del `onPropuestaAprobada()`:**
```java
// PropuestaService.java L85-126
@EventListener
public void onPropuestaAprobada(PropuestaAprobada event) {
    // 1. Buscar necesidad
    // 2. Para cada fragmentación:
    //    a. Buscar donación original
    //    b. Fragmentar si es necesario
    //    c. Asignar (dominio)
    //    d. Persistir
    //    e. Notificar logística
    // 3. Persistir necesidad
}
```

Este es un flujo de caso de uso completo que:
- Combina lógica de dominio (`fragmentarse()`, `asignar()`, `asignarDonacion()`)
- Con coordinación entre aggregates (`Necesidad` ← `DonacionIndependiente`)
- Y comunicación inter-servicio (`logisticaAsyncService.registrarEntregaPendiente()`)

**Severidad: MEDIA** (la lógica es correcta pero el listener es un caso de uso completo disfrazado de evento handler)
**Confianza: Media**

---

### 5.5 `ProcesadorDeDonaciones` ❌

- **Ubicación:** `infrastructure/ProcesadorDeDonaciones.java`
- **Anotación:** `@Service`
- **Responsabilidad actual:** Normaliza ítems de donación, los persiste, y decide si avanzar el estado a NORMALIZADA.

**Problema:** Es un Application Service (orquesta normalización, persistencia, decisión de estado, publicación de eventos) **pero está ubicado en el paquete `infrastructure/`**.

**Evidencia:**
```java
// ProcesadorDeDonaciones.java L38-48
@Async
public void procesar(Donacion donacion) {
    List<ItemDonacionNormalizado> itemsNormalizados = normalizador.normalizar(donacion);
    itemNormalizadoRepository.saveAll(itemsNormalizados);
    finalizarNormalizacion(donacion, itemsNormalizados);
}
```

```java
// L72-92 finalizarNormalizacion()
private void finalizarNormalizacion(Donacion donacion, ...) {
    if (!tienePendientes) {
        donacion.marcarNormalizada();          // transición de dominio
        donacionRepository.save(donacion);      // persistencia
        eventPublisher.publishEvent(...);       // evento de aplicación
    }
}
```

**Severidad: CRÍTICO** (orquestación de caso de uso en paquete de infraestructura)
**Confianza: Alta**

---

### 5.6 `SegmentacionEventListener` ❌

- **Ubicación:** `infrastructure/events/SegmentacionEventListener.java`
- **Anotación:** `@Component`
- **Responsabilidad actual:** Escucha `DonacionNormalizadaEvent`, segmenta donaciones, registra en incentivos, persiste donaciones independientes, marca ítems como segmentados, avanza estado de donación.

**Problema:** Es un **caso de uso completo de segmentación** ubicado en `infrastructure/events/`:

**Evidencia:**
```java
// SegmentacionEventListener.java L42-88
@EventListener
public void onDonacionNormalizada(DonacionNormalizadaEvent event) {
    // 1. Buscar donación
    // 2. Obtener ítems aceptados
    // 3. Segmentar (dominio)
    // 4. Registrar en incentivos (infra → Feign)
    // 5. Persistir donaciones independientes
    // 6. Marcar ítems como segmentados
    // 7. Avanzar estado a SEGMENTADA
}
```

Tiene **9 dependencias** inyectadas y hace traversal complejo para registrar incentivos (DonacionIndep → Donacion → Donante → Persona → nombre).

**Severidad: CRÍTICO** (caso de uso de aplicación completo en infraestructura)
**Confianza: Alta**

---

## 6. Diagnóstico del Dominio

### 6.1 Entidades con comportamiento rico ✅

Las siguientes entidades encapsulan correctamente su comportamiento:

| Entity | Comportamiento encapsulado | Calidad |
|---|---|---|
| `Donacion` | `agregarItem()`, `quitarItem()`, `marcarNormalizada()`, `marcarSegmentada()`, historial de estados | ✅ Bueno |
| `DonacionIndependiente` | State Pattern completo, `fragmentarse()`, `asignar()`, `confirmarEntrega()`, `vencer()`, etc. | ✅ Excelente |
| `Necesidad` (abstract) | `asignarDonacion()`, `quitarDonacion()`, `estaSatisfecha()`, `cantidadAcumulada()`, validaciones | ✅ Bueno |
| `Propuesta` | `confirmar()`, `rechazar()`, `agregarFragmentacion()`, domain events | ✅ Bueno |
| `Bien` (record) | `estaVencido()`, validaciones de construcción | ✅ Bueno |
| `ItemDonacion` (record) | `getPesoTotal()`, `getVolumenTotal()`, validaciones | ✅ Bueno |
| `Entrega` (logística) | State machine con `iniciarRuta()`, `confirmarEntrega()`, `negarEntrega()`, historial | ✅ Excelente |
| `Ruta` (logística) | `iniciarRuta()`, `completarRuta()`, `agregarEntrega()` | ✅ Bueno |
| `Camion` (logística) | `asignarARuta()`, `completarRuta()`, `habilitar()`, `deshabilitar()` | ✅ Bueno |
| `DonanteIncentivos` | `registrarDonacion()`, `intentarAscenso()`, `otorgarInsignia()` | ✅ Excelente |
| `Notificacion` | `notificar(Persona, NotificacionSender)` | ✅ Bueno |

### 6.2 Entidades mayormente estructurales ⚠️

| Entity | Observación |
|---|---|
| `Donante` | Wrapper de `personaId` con `anonimizar()` vacío. No tiene comportamiento propio significativo |
| `EntidadBeneficiaria` | Wrapper de `juridicaId` con `anonimizar()` vacío |

**Severidad: BAJO** — Estos actúan como identificadores de rol (un concepto válido), no necesariamente necesitan más comportamiento.
**Confianza: Media**

### 6.3 State Pattern bien implementado ✅

La interfaz `EstadoDonacion` (del paquete `donacionesIndependientes`) implementa un State Pattern correcto:

```text
interface EstadoDonacion
    ├── EnDeposito       → asignar(), vencer()
    ├── AsignacionRealizada → planificarRuta()
    ├── ListaParaEntregar   → iniciarRecorrido()
    ├── EnTraslado          → confirmarEntrega(), registrarFalla()
    ├── Entregada           → (terminal)
    ├── EntregaFallida      → retornar(), replanificar()
    └── Vencida             → (terminal)
```

Transiciones inválidas lanzan `BusinessStateException`. El ADR [20260616-identificacion-estado-donacion-independiente.md](file:///c:/IdeaProjects/DonaTrack-TP-DDS/docs/adr/donaciones-service/20260616-identificacion-estado-donacion-independiente.md) documenta la decisión de usar `TipoEstadoDonacion` enum + `getTipo()`.

### 6.4 Contraste: `Donacion` vs `DonacionIndependiente`

| Aspecto | `Donacion` | `DonacionIndependiente` |
|---|---|---|
| Estados | Enum simple (`CARGADA`, `NORMALIZADA`, `SEGMENTADA`) | State Pattern completo (7 estados) |
| Transiciones | `marcarNormalizada()`, `marcarSegmentada()` con guards | Delegadas al estado actual |
| Historial | `List<CambioEstadoDonacion>` | `List<CambioEstado>` |
| Comportamiento | `agregarItem()`, `quitarItem()` | `fragmentarse()`, `asignar()`, `confirmarEntrega()`, etc. |

**Observación:** La diferencia de sofisticación entre ambas es coherente con el dominio — `Donacion` tiene un ciclo de vida simple (carga → normalización → segmentación), mientras que `DonacionIndependiente` tiene un ciclo de vida operativo complejo.

### 6.5 Value Objects identificados

| Candidato | Uso actual | ¿Debería ser VO? | Evidencia |
|---|---|---|---|
| `Bien` | `record` inmutable con `estaVencido()` | ✅ **Ya es un VO de facto** | Inmutable, con comportamiento |
| `ItemDonacion` | `record` inmutable con peso/volumen | ✅ **Ya es un VO de facto** | Inmutable, con cálculos |
| `BienNormalizado` | `record` inmutable | ✅ **Ya es un VO de facto** | Inmutable |
| `Direccion` (logística) | Entity con validaciones | ⚠️ Candidato a VO | Tiene identidad pero es más bien un concepto de valor |
| `Email`, `Telefono` | Clases separadas en `personas/` | ✅ Actúan como VOs | Representan medios de contacto |
| `Estado` enum | Enum NUEVO/USADO/DESGASTADO | N/A | Enum, ya es equivalente a VO |

### 6.6 `Necesidad.toDTO()` dentro del dominio ⚠️

**Evidencia:**
```java
// Necesidad.java L49-59
protected NecesidadDTO toDTO(LocalDate fechaFin) {
    return new NecesidadDTO(
        this.id, this.getTipoNecesidad().name(), ...);
}
```

`Necesidad` conoce y referencia directamente `NecesidadDTO`, que es un DTO del paquete `dto/`. Esto crea un acoplamiento del dominio hacia la capa de presentación/aplicación.

**Severidad: MEDIO**
**Confianza: Alta**

### 6.7 `DonacionIndependiente` usa `@JsonIgnore` ⚠️

```java
// DonacionIndependiente.java L30
@JsonIgnore private Asignable asignadaA;
```

Una entidad de dominio no debería conocer anotaciones de serialización JSON.

**Severidad: BAJO**
**Confianza: Alta**

---

## 7. Diagnóstico de Repositories

### Patrón general ✅

```text
CrudRepository<T extends AggregateRoot>  (common-lib, interface)
         ↑
CrudRepositoryEnMemoria<T>               (common-lib, abstract class)
         ↑
DonacionesRepositoryEnMemoria            (models/repositories/impl/)
```

- Las **interfaces** están en `models/repositories/` (dentro del dominio).
- Las **implementaciones** están en `models/repositories/impl/` (aún dentro del mismo paquete, no en `infrastructure/`).

**Observación:** Las implementaciones en memoria están en `models/repositories/impl/`, que es un subpaquete del dominio. Conceptualmente podrían ir en infraestructura, pero dado que son stubs sin tecnología externa, la ubicación actual es aceptable como decisión pragmática.

**Severidad: BAJO** (la ubicación funciona, pero cuando se agregue persistencia real, las implementaciones JPA deberían ir en `infrastructure/`).
**Confianza: Alta**

### Métodos custom correctos

Los repositories agregan consultas que filtran por estado o condición, usando Streams sobre la colección en memoria:
```java
// Ejemplo: findEnDeposito(), findByEstaSatisfechaFalseActivaTrue()
```

Estos métodos no contienen lógica de negocio, solo filtrado.

---

## 8. Diagnóstico de Infrastructure

### 8.1 Feign Clients ✅

| Client | Ubicación | Qué hace |
|---|---|---|
| `IncentivosFeignClient` | `infrastructure/clients/` | REST hacia incentivos-service |
| `NotificacionesFeignClient` | `infrastructure/clients/` | REST hacia notificaciones-service |
| `LogisticaFeignClient` | `infrastructure/clients/` | REST hacia logistica-service |

Correctamente aislados en `infrastructure/clients/`. Sin embargo, son consumidos directamente por Services y EventListeners sin una abstracción intermedia (port).

### 8.2 RabbitMQ ✅

- `logistica-service` publica eventos via `LogisticaEventPublisher`.
- `donaciones-service` los consume via `LogisticaEventListener` con `@RabbitListener`.
- La configuración está en `RabbitMQConfig`.

### 8.3 Algoritmos de asignación ⚠️

| Clase | Ubicación | Observación |
|---|---|---|
| `AlgoritmoAsignacion` (abstract) | `infrastructure/algoritmos/` | Template Method con lógica de dominio |
| `AlgoritmoCompatibilidadSemantica` | `infrastructure/algoritmos/` | Implementación concreta |
| `AlgoritmoPrioridadSubAtendidos` | `infrastructure/algoritmos/` | Implementación concreta |
| `StockDeDonaciones` | `infrastructure/algoritmos/` | Helper de stock |

**Problema:** Estas clases implementan lógica de negocio pura (matching de donaciones con necesidades por subcategoría y prioridad). No dependen de ningún detalle técnico externo. Su ubicación en `infrastructure/` no corresponde a su responsabilidad.

**Contraste con logística:** En `logistica-service`, los algoritmos equivalentes (`AlgoritmoOrdenadorDeEntregas`, `AlgoritmoAsignadorDeEntregas`) están definidos como interfaces en `services/` con implementaciones en `services/impl/`. Esto es más coherente.

**Severidad: ALTA**
**Confianza: Alta**

### 8.4 Segmentadores ⚠️

| Clase | Ubicación |
|---|---|
| `Segmentador` (interface) | `models/ports/` ✅ |
| `AbstractSegmentador` | `infrastructure/segmentadores/` |
| `SegmentadorSimple` | `infrastructure/segmentadores/` |
| `SegmentadorComplejo` | `infrastructure/segmentadores/` |

La interfaz `Segmentador` está correctamente en `models/ports/`. Las implementaciones están en `infrastructure/`, lo cual es aceptable si se considera que la segmentación podría depender de heurísticas o configuraciones externas.

### 8.5 Normalizador Semántico

El `NormalizadorSemanticoBien` en `infrastructure/analizadores/` interactúa con la categorización semántica. Su ubicación en infraestructura es razonable ya que podría involucrar servicios externos (IA/NLP).

---

## 9. Diagnóstico de Casos de Uso

### 9.1 Cargar Donación
```text
Caso de uso: Cargar una nueva donación
Entrada: DonacionInputDTO
Controller: DonacionesController.cargarDonacion()
Application Service: DonacionesService.cargarDonacion()
Dominio: Donacion (constructor + agregarItem)
Repositorios: IDonacionesRepository, IDonantesRepository
Infraestructura: ProcesadorDeDonaciones (async)
Dónde está hoy la lógica: Bien distribuida
Dónde parece estar mal ubicada: ProcesadorDeDonaciones en infrastructure/
Confianza: Alta
```

### 9.2 Normalizar Donación
```text
Caso de uso: Normalizar ítems de una donación
Entrada: DonacionNormalizadaEvent (trigger interno)
Controller: N/A (async)
Application Service: ProcesadorDeDonaciones ← UBICADO EN infrastructure/
Dominio: Donacion.marcarNormalizada(), ItemDonacionNormalizado
Repositorios: IItemDonacionNormalizadoRepository, IDonacionesRepository
Infraestructura: NormalizadorSemanticoBien, ApplicationEventPublisher
Dónde está la lógica: infrastructure/ProcesadorDeDonaciones
Dónde parece que debería estar: services/ (Application Service)
Confianza: Alta
```

### 9.3 Segmentar Donación
```text
Caso de uso: Segmentar donación normalizada en donaciones independientes
Entrada: DonacionNormalizadaEvent
Controller: N/A (event-driven)
Application Service: SegmentacionEventListener ← UBICADO EN infrastructure/events/
Dominio: Segmentador.segmentar(), DonacionIndependiente, Donacion.marcarSegmentada()
Repositorios: múltiples
Infraestructura: IncentivosFeignClient
Dónde está la lógica: infrastructure/events/SegmentacionEventListener
Dónde parece que debería estar: services/ (Application Service)
Confianza: Alta
```

### 9.4 Ejecutar Algoritmo de Asignación
```text
Caso de uso: Ejecutar matching automático donaciones ↔ necesidades
Entrada: POST /api/asignaciones/ejecuciones
Controller: AsignacionController.ejecutar()
Application Service: PropuestaService → AlgoritmosService
Dominio: AlgoritmoAsignacion (Template Method), Propuesta, Necesidad
Repositorios: IDonacionesIndependientesRepository, INecesidadesRepository, IPropuestasRepository
Infraestructura: NotificacionesFeignClient
Dónde está la lógica: Distribuida entre PropuestaService y AlgoritmosService
Dónde parece estar mal ubicada: AlgoritmoAsignacion en infrastructure/algoritmos/
Confianza: Alta
```

### 9.5 Aprobar Propuesta → Asignar + Fragmentar + Notificar Logística
```text
Caso de uso: Confirmar propuesta y ejecutar asignación real
Entrada: PUT /api/asignaciones/propuestas/{id}/estado
Controller: PropuestasController.actualizarEstado()
Application Service: PropuestaService.onPropuestaAprobada() (@EventListener)
Dominio: Propuesta.confirmar(), DonacionIndependiente.fragmentarse(), .asignar(), Necesidad.asignarDonacion()
Repositorios: IDonacionesIndependientesRepository, INecesidadesRepository
Infraestructura: LogisticaAsyncService
Dónde está la lógica: PropuestaService como event listener
Confianza: Media (el event listener combina demasiadas responsabilidades)
```

### 9.6 Cambiar Estado de Donación Independiente (múltiples)
```text
Caso de uso: Transiciones de estado (asignar, trasladar, entregar, fallar, vencer, retornar)
Entrada: PATCH /donaciones-independientes/{id}/estado o RabbitMQ events
Controller: DonacionesIndependientesController + LogisticaEventListener
Application Service: DonacionesIndependientesService.cambiarEstado()
Dominio: DonacionIndependiente.asignar/planificarRuta/iniciarRecorrido/confirmarEntrega/etc.
Repositorios: múltiples
Infraestructura: IncentivosFeignClient, NotificacionesFeignClient
Dónde está la lógica: Service orquesta + domain ejecuta transición
Problema: Un solo método switch para 6 casos de uso distintos
Confianza: Alta
```

---

## 10. Diagnóstico de Cambios de Estado

### `Donacion` — Transiciones simples ✅

```text
CARGADA ──▶ NORMALIZADA ──▶ SEGMENTADA
```

Encapsuladas en la entidad con guards (`if (estadoActual != CARGADA) throw`).
Historial registrado en `List<CambioEstadoDonacion>`.

### `DonacionIndependiente` — State Pattern completo ✅

```text
EN_DEPOSITO ──▶ ASIGNACION_REALIZADA ──▶ LISTA_PARA_ENTREGAR ──▶ EN_TRASLADO
                                                                       │
                    ┌──────────────────────────────────────────────────┤
                    ▼                                                  ▼
              ENTREGA_FALLIDA ──▶ EN_DEPOSITO (retornar)          ENTREGADA
                    │
                    ▼
              EN_DEPOSITO (replanificar)

EN_DEPOSITO ──▶ VENCIDA (terminal)
```

Cada estado concreto define qué transiciones acepta. Transiciones inválidas lanzan `BusinessStateException`.

### `Entrega` (logística) — Transiciones con guards ✅

```text
PENDIENTE ──▶ EN_TRASLADO ──▶ ENTREGADA
                    │                    NO_RECIBIDA ──▶ REVISION
                    └──▶ NO_RECIBIDA ──▶ REVISION
```

### `EstadoDonacion` (de `Donacion`) vs `EstadoDonacion` (de `DonacionIndependiente`) ⚠️

Existen dos clases/interfaces llamadas `EstadoDonacion` en paquetes distintos:
- `models.entities.donaciones.EstadoDonacion` — un **enum** simple
- `models.entities.donacionesIndependientes.EstadoDonacion` — una **interfaz** (State Pattern)

**Severidad: BAJO** (ambigüedad de nombres)
**Confianza: Alta**

---

## 11. Diagnóstico de Agregados

### Aggregates explícitos (implementan `AggregateRoot`)

| Aggregate Root | Servicio | Componentes internos |
|---|---|---|
| `Donacion` | donaciones | `ItemDonacion` (lista), `CambioEstadoDonacion` (historial), `Bien` (VO) |
| `DonacionIndependiente` | donaciones | `ItemDonacionIndependiente` (lista), `CambioEstado` (historial), `EstadoDonacion` (state) |
| `ItemDonacionNormalizado` | donaciones | `BienNormalizado` (VO) |
| `Propuesta` | donaciones | `PosibleFragmentacion` (lista), domain events |
| `Categoria` | donaciones | `Subcategoria` (por ref ID) |
| `Archivo` | donaciones | — |
| `Donante` | donaciones | — (ref a `Persona` por ID) |
| `EntidadBeneficiaria` | donaciones | — (ref a `Juridica` por ID) |
| `Persona` (abstract) | donaciones | `MedioDeContacto` (lista), `Direccion` |
| `Necesidad` (abstract) | donaciones | `DonacionIndependiente` (lista asignadas) |
| `Entrega` | logística | `CambioEstadoEntrega` (historial), `Direccion` |
| `Ruta` | logística | Lista de UUIDs de entregas |
| `Camion` | logística | — |
| `Chofer` | logística | — |
| `DonanteIncentivos` | incentivos | `Mision` (lista), `Insignia` (lista), `Metricas`, `CambioCategoria` (historial) |
| `RankingMensual` | incentivos | `EntradaRanking` (lista) |
| `Notificacion` | notificaciones | — |
| `Persona` | notificaciones | `MedioDeContacto` (lista) |

### Observación sobre `Necesidad` ↔ `DonacionIndependiente`

`Necesidad` (y `NecesidadExtraordinaria`) mantiene una lista de `DonacionIndependiente` asignadas. Esto crea una referencia directa entre dos Aggregate Roots.

```java
// NecesidadExtraordinaria.java L14
private List<DonacionIndependiente> donacionesAsignadas;
```

Esto podría causar problemas cuando se implemente persistencia real: dos aggregates no deberían mantener referencias directas a objetos de otro aggregate, sino a sus IDs.

**Severidad: MEDIO** (impactará cuando se agregue BD)
**Confianza: Alta**

---

## 12. Diagnóstico de Diagramas PlantUML

### Diagramas existentes

| Diagrama | Servicio | Estado |
|---|---|---|
| [diagrama-de-clases-logistica.puml](file:///c:/IdeaProjects/DonaTrack-TP-DDS/docs/design/logistica-service/diagrama-de-clases-logistica.puml) | logística | ✅ Existe y está actualizado |
| *Ninguno* | donaciones | ❌ **FALTA** |
| *Ninguno* | notificaciones | ❌ No existe |
| *Ninguno* | incentivos | ❌ No existe |

### Análisis del diagrama de logística vs código

| Aspecto | Diagrama | Código | Coincide |
|---|---|---|---|
| `Entrega` con historial y métodos | ✅ | ✅ | ✅ |
| `Ruta` con estados y métodos | ✅ | ✅ | ✅ |
| `Camion` con estados y métodos | ✅ | ✅ | ✅ |
| `Chofer` | Aparece pero sin métodos detallados | Tiene métodos | ⚠️ Parcial |
| `Direccion`, `Localidad`, `Provincia`, `Pais` | ✅ | ✅ | ✅ |
| `AlgoritmoOrdenadorDeEntregas` (interface) | ✅ | ✅ | ✅ |
| `AlgoritmoAsignadorDeEntregas` (interface) | ✅ | ✅ | ✅ |
| `GeneradorDeRutas` | ✅ | ✅ | ✅ |
| `PlanificacionService` | ✅ | ✅ | ✅ |
| `RutasService` | ✅ | ✅ | ✅ |
| `EntregasService` | ✅ | ✅ | ✅ |
| `CamionesService` | ✅ | ✅ | ✅ |
| `ValidadorPatentes` | ✅ | ✅ | ✅ |
| `LogisticaEventPublisher` | ✅ | ✅ | ✅ |
| `ChoferService` | No aparece | ✅ Existe | ⚠️ Falta en diagrama |
| `PlanificadorDeEntregas` (scheduler) | ✅ | ✅ | ✅ |
| `RuteadorExternoClient` | ✅ En diagrama | ❌ No existe en código | ❌ Diagrama muestra diseño futuro |
| `GeneradorDeURLSeguimiento` | No aparece | ✅ Existe | ⚠️ Falta en diagrama |
| Controllers | No aparecen | ✅ Existen | ✅ OK (no son dominio) |
| DTOs | No aparecen | ✅ Existen | ✅ OK (no son dominio) |
| Mappers | No aparecen | ✅ Existen | ✅ OK (no son dominio) |

> [!IMPORTANT]
> **Falta el diagrama de clases del servicio de donaciones**, que es el de mayor complejidad de dominio. Esto es relevante respecto a la indicación del profesor:
> *"Los casos de uso tienen que estar representados en el diagrama (si saco los services, igualmente deberían quedar en el diagrama)."*

**Severidad: ALTA** (falta el diagrama más importante)
**Confianza: Alta**

---

## 13. Matriz de Problemas

| # | Problema | Evidencia | Resp. Actual | Resp. Esperada | Severidad | Confianza |
|---|---|---|---|---|---|---|
| P1 | `ProcesadorDeDonaciones` es Application Service en `infrastructure/` | [ProcesadorDeDonaciones.java](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/infrastructure/ProcesadorDeDonaciones.java) | Infra | Application | CRÍTICO | Alta |
| P2 | `SegmentacionEventListener` es caso de uso completo en `infrastructure/events/` | [SegmentacionEventListener.java](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/infrastructure/events/SegmentacionEventListener.java) | Infra | Application | CRÍTICO | Alta |
| P3 | Algoritmos de asignación en `infrastructure/algoritmos/` con lógica de dominio pura | [AlgoritmoAsignacion.java](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/infrastructure/algoritmos/AlgoritmoAsignacion.java) | Infra | Domain | ALTA | Alta |
| P4 | `DonacionesIndependientesService.cambiarEstado()` mezcla 6 casos de uso | [DonacionesIndependientesService.java:L66-90](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/DonacionesIndependientesService.java#L66-L90) | Application | Application (dividido) | ALTA | Alta |
| P5 | `AlgoritmosService` tiene baja cohesión (algoritmos + propuestas + notificaciones) | [AlgoritmosService.java](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/AlgoritmosService.java) | Application | Application (dividido) | ALTA | Alta |
| P6 | `Necesidad.toDTO()` crea acoplamiento dominio → DTO | [Necesidad.java:L49-59](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/necesidades/Necesidad.java#L49-L59) | Domain | Application/Mapper | MEDIA | Alta |
| P7 | Falta diagrama PlantUML para donaciones-service | `docs/design/` sin `.puml` de donaciones | — | Documentación | ALTA | Alta |
| P8 | `NecesidadExtraordinaria` tiene referencia directa a `DonacionIndependiente` (entre aggregates) | [NecesidadExtraordinaria.java:L14](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/necesidades/NecesidadExtraordinaria.java#L14) | Domain | Domain (ref por ID) | MEDIA | Alta |
| P9 | `AlgoritmosService` usa `ResponseStatusException` (presentation concern) | [AlgoritmosService.java:L174](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/AlgoritmosService.java#L174) | Application | Application | MEDIA | Alta |
| P10 | `DonacionIndependiente` usa `@JsonIgnore` (concern de serialización en dominio) | [DonacionIndependiente.java:L30](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/donacionesIndependientes/DonacionIndependiente.java#L30) | Domain | Application/Mapper | BAJO | Alta |
| P11 | Dos clases/interfaces llamadas `EstadoDonacion` en paquetes distintos | `entities.donaciones` vs `entities.donacionesIndependientes` | Domain | Domain | BAJO | Alta |
| P12 | Traversal repetido entre aggregates para notificaciones (DI→Donacion→Donante→Persona) | Múltiples services y listeners | Application | Application (extraer) | MEDIA | Alta |
| P13 | `AsignacionController` inyecta clase concreta `PropuestaService` | [AsignacionController.java:L21](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/controllers/impl/AsignacionController.java#L21) | Presentation | Presentation | BAJO | Alta |
| P14 | `consolidar()` en `AlgoritmosService` es lógica de dominio en un method estático privado | [AlgoritmosService.java:L71-96](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/AlgoritmosService.java#L71-L96) | Application | Domain | MEDIA | Media |

---

## 14. Mapa de Candidatos a Refactor

> [!CAUTION]
> **NO IMPLEMENTAR.** Estos son diagnósticos, no instrucciones.

```text
ProcesadorDeDonaciones (infrastructure/)
    ↓
procesar() — orquesta normalización, persistencia, transición de estado
    ↓
Application Service ubicado en infrastructure
    ↓
services/ (Application Service)
    ↓
Es un caso de uso (procesar donación), no un detalle técnico
```

```text
SegmentacionEventListener (infrastructure/events/)
    ↓
onDonacionNormalizada() — segmenta, persiste, registra incentivos, cambia estado
    ↓
Caso de uso completo disfrazado de event listener
    ↓
services/ (Application Service o parte de un servicio existente)
    ↓
La responsabilidad es "segmentar donación", no "escuchar evento"
```

```text
AlgoritmoAsignacion (infrastructure/algoritmos/)
    ↓
ejecutar() — Template Method para matching donaciones ↔ necesidades
    ↓
Lógica de dominio pura ubicada en infrastructure
    ↓
models/ o services/ (Domain Service / Domain Algorithm)
    ↓
No depende de ningún detalle técnico
```

```text
DonacionesIndependientesService
    ↓
cambiarEstado() — switch con 6 ramas heterogéneas
    ↓
Múltiples casos de uso en un método
    ↓
Métodos separados o services por comportamiento
    ↓
Cada transición tiene consecuencias distintas (notificaciones, incentivos)
```

```text
AlgoritmosService
    ↓
consolidar() — regla de consolidación de propuestas
    ↓
Regla de dominio en service estático
    ↓
Propuesta o clase de dominio dedicada
    ↓
La decisión de qué propuestas prevalecen es de negocio
```

```text
Necesidad
    ↓
toDTO() — entidad de dominio conoce DTO
    ↓
Acoplamiento dominio → aplicación
    ↓
Mapper externo
    ↓
El dominio no debería conocer la representación de presentación
```

---

## 15. Riesgos y Dependencias

### Arquitectura
- **Riesgo:** Reubicar `ProcesadorDeDonaciones` y `SegmentacionEventListener` podría romper la cadena asíncrona Donación → Normalización → Segmentación si no se preservan los eventos Spring internos.
- **Mitigación:** Mantener `@Async` y `@EventListener` como mecanismo, pero en la capa correcta.

### Persistencia
- **Riesgo:** La referencia directa `NecesidadExtraordinaria.donacionesAsignadas: List<DonacionIndependiente>` no será mapeable directamente cuando se agregue JPA, porque cruza límites de aggregate.
- **Dependencia:** La decisión sobre persistencia (documentada como DTI-01 en `DEUDA_TECNICA.md`) afectará la estructura de aggregates.

### Integración entre módulos
- **Riesgo:** El traversal repetido (DonacionIndependiente → Donacion → Donante → Persona) para obtener IDs de notificación es frágil y se duplica en 3 lugares.
- **Mitigación:** Extraer un servicio de resolución de actores o enriquecer los eventos con los IDs necesarios.

### Tests
- **Riesgo bajo:** Los tests unitarios existentes validan comportamiento de entidades y controllers. Un refactor que mueva clases pero preserve interfaces no debería romperlos.
- **Dependencia:** Los integration tests (`integration-tests/`) dependen de los endpoints REST. Si los endpoints no cambian, no se rompen.

### Diagramas
- **Riesgo:** Sin diagrama de donaciones-service, la auditoría del profesor sobre "los casos de uso deben quedar en el diagrama" no puede satisfacerse para el servicio principal.

### Contratos
- **Riesgo:** Los DTOs de comunicación entre servicios (`dto/comunicaciones/`) definen contratos implícitos. Un cambio en la estructura de estos DTOs puede romper la integración.

### Regresión
- **Riesgo:** Mover clases de `infrastructure/` a `services/` es un refactor de ubicación que requiere actualizar imports pero no debería cambiar comportamiento.

---

## 16. Preguntas Abiertas

1. **Intención del profesor sobre "casos de uso en el dominio":**
   ¿Se refiere a que las entidades deben expresar las operaciones de negocio (lo cual `DonacionIndependiente` ya hace con `asignar()`, `confirmarEntrega()`, etc.)? ¿O se refiere a una representación explícita de casos de uso como clases separadas dentro de `models/`?

2. **¿Debería existir una clase `ProcesarDonacion` como Domain Service?**
   El profesor mencionó "clase procesarDonación para cambios de estado". Actualmente eso está parcialmente en `ProcesadorDeDonaciones` (para normalización/segmentación) y en `DonacionesIndependientesService` (para transiciones operativas). ¿Se espera unificar?

3. **Límite de aggregate entre `Necesidad` y `DonacionIndependiente`:**
   `NecesidadExtraordinaria` mantiene una lista directa de `DonacionIndependiente`. Esto funciona en memoria pero es problemático para persistencia. ¿El equipo ya evaluó esta decisión para la próxima entrega?

4. **¿Los algoritmos de asignación son considerados dominio o infraestructura?**
   `AlgoritmoAsignacion` (Template Method) y sus implementaciones están en `infrastructure/algoritmos/`. No dependen de tecnología externa. ¿Fue una decisión deliberada o conveniencia de paquete?

5. **¿Qué servicios deberían tener diagrama PlantUML?**
   Actualmente solo logística tiene uno. ¿Se espera uno para cada servicio o al menos para donaciones-service?

6. **Anonimización (`Anonimizable`):**
   `Donante.anonimizar()` y `EntidadBeneficiaria.anonimizar()` están vacíos con comentario "coordinado a nivel de PersonasService". ¿Está planificado para la entrega 2 (DTI-01)?

---

## 17. Conclusión

### Estado actual

El proyecto DonaTrack está **notablemente bien estructurado** para un proyecto académico. Las entidades de dominio contienen comportamiento rico, las transiciones de estado están encapsuladas (especialmente el State Pattern en `DonacionIndependiente`), los controllers son adaptadores puros, y los repositories siguen un patrón limpio con interfaces en dominio e implementaciones separadas.

### Problemas principales

1. **`ProcesadorDeDonaciones`** — Application Service ubicado en `infrastructure/`
2. **`SegmentacionEventListener`** — Caso de uso completo de segmentación disfrazado de event listener en `infrastructure/`
3. **Algoritmos de asignación** — Lógica de dominio pura en `infrastructure/algoritmos/`
4. **`DonacionesIndependientesService.cambiarEstado()`** — 6 casos de uso en un único switch
5. **`AlgoritmosService`** — Baja cohesión (algoritmos + propuestas + notificaciones + lógica consolidación)
6. **Falta diagrama PlantUML** para donaciones-service
7. **`Necesidad.toDTO()`** — Acoplamiento dominio → DTO
8. **Referencia directa entre aggregates** `Necesidad` ↔ `DonacionIndependiente`
9. **Traversal repetido** para resolución de actores de notificación
10. **`ResponseStatusException`** en Application Service

### Refactors potenciales (categorías)

| Categoría | Impacto | Complejidad |
|---|---|---|
| Reubicar `ProcesadorDeDonaciones` y `SegmentacionEventListener` a `services/` | Alto | Baja (mover + repackage) |
| Reubicar algoritmos de asignación a `models/` o `services/` | Alto | Baja |
| Dividir `DonacionesIndependientesService.cambiarEstado()` por comportamiento | Medio | Media |
| Dividir `AlgoritmosService` en ejecución vs gestión de propuestas | Medio | Media |
| Extraer `Necesidad.toDTO()` a un Mapper | Bajo | Baja |
| Crear diagrama PlantUML de donaciones-service | Alto (documentación) | Media |
| Resolver referencia directa Necesidad → DonacionIndependiente | Medio | Alta (impacta persistencia) |
| Extraer servicio de resolución de actores de notificación | Bajo | Media |

### Qué NO debería tocarse

| Componente | Razón |
|---|---|
| **Controllers** (todos los servicios) | Adaptadores HTTP puros y correctos |
| **State Pattern de `DonacionIndependiente`** | Excelente implementación de dominio |
| **Transiciones de `Donacion`** | Simples, correctas, encapsuladas |
| **Entities de logistica-service** | `Entrega`, `Ruta`, `Camion`, `Chofer` — comportamiento rico y correcto |
| **Services de logistica-service** | Buena separación orquestación/dominio |
| **Notificaciones-service** | Port/Adapter limpio con `NotificacionSender` |
| **Incentivos-service** | `DonanteIncentivos` con lógica rica, misiones con Template Method, criterios con Strategy |
| **common-lib** | `CrudRepository<T extends AggregateRoot>` y excepciones centralizadas |
| **Repositories (interfaces)** | Correctamente en `models/repositories/` |
| **DTOs y Mappers** | Ubicación y estructura correctas |
| **ADRs existentes (51)** | Documentación rica de decisiones |
| **Feign Clients** | Correctamente aislados en `infrastructure/clients/` |
| **RabbitMQ event publishing** (logística) | Correctamente en `infrastructure/` |
