# Plan de Refactorización Técnica e Incremental: `donaciones-service`

> [!IMPORTANT]
> **Propósito del documento:** Este plan técnico establece la hoja de ruta ejecutable para refactorizar `donaciones-service`, corrigiendo los problemas de asignación de responsabilidades identificados en el diagnóstico, preservando el comportamiento existente y preparando el terreno para la futura persistencia relacional, sin incurrir en sobreingeniería ni reescrituras innecesarias.

---

## 0. Marco Teórico y Criterios de Ingeniería para el Proyecto

### 0.1 ¿Qué significa refactorizar en este contexto?
En el contexto de **DonaTrack** (proyecto multi-servicio con arquitectura por capas, DDD y desarrollo en equipo), refactorizar significa:
* **Preservación estricta del comportamiento observable:** El sistema debe responder exactamente los mismos códigos HTTP, payloads JSON y eventos asíncronos para las mismas entradas.
* **Reubicación de responsabilidades:** Mover lógica de negocio desde capas técnicas (ej. `infrastructure/`, listeners) hacia el Dominio o Aplicación según corresponda.
* **Aumento de cohesión y reducción de acoplamiento:** Dividir clases con múltiples motivos de cambio (God Services, switches de casos de uso disímiles) en componentes especializados.
* **Clarificación del modelo de dominio:** Hacer explícitas las invariantes y políticas de negocio (ej. extraer `PoliticaConsolidacionPropuestas` en lugar de esconderla en métodos privados estáticos).
* **Satisfacción de observaciones docentes sin reescritura:** Resolver los señalamientos de la cátedra con el menor número de cambios atómicos y verificables.

### 0.2 ¿Qué NO debería considerarse refactor?
* ❌ **Reescritura de microservicios o módulos:** Cambiar la arquitectura completa a hexagonal pura o reactiva solo por estética.
* ❌ **Incorporación de nuevos features o reglas de negocio no solicitadas.**
* ❌ **Introducción prematura de JPA/Hibernate:** Agregar anotaciones `@Entity`, `@Table` o repositorios de base de datos relacional cuando la entrega actual exige almacenamiento en memoria.
* ❌ **Reemplazo de frameworks o librerías:** Cambiar Spring Web por WebFlux, o Spring Events por otra tecnología sin justificación funcional.
* ❌ **Optimización prematura de rendimiento:** Alterar estructuras de datos o algoritmos que ya cumplen los tiempos de respuesta requeridos.
* ❌ **Renombramientos masivos o cosméticos** que no cambien responsabilidades y solo generen conflictos de merge en Git.

---

### 0.3 Refactors necesarios vs. peligrosos/innecesarios antes de la entrega

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ MATRIZ DE DECISIÓN DE REFACTORS PREVIO A LA ENTREGA                         │
├──────────────────────────────────────┬──────────────────────────────────────┤
│ REFACTORS NECESARIOS (INCLUIR)       │ REFACTORS PELIGROSOS (NO HACER / POSP)│
├──────────────────────┬───────────────┼──────────────────────┬───────────────┤
│ Concepto             │ Motivo        │ Concepto             │ Motivo        │
├──────────────────────┼───────────────┼──────────────────────┼───────────────┤
│ Mover Dominio fuera  │ Observación   │ Desacoplar Agregados │ Rompe cálculo │
│ de Infrastructure    │ crítica de    │ Necesidad ↔ DI       │ en memoria y  │
│ (REF-03, REF-04)     │ arquitectura  │ por IDs (P8)         │ algoritmos    │
├──────────────────────┼───────────────┼──────────────────────┼───────────────┤
│ Separar Listener de  │ El listener   │ Introducir capas de  │ Agrega capas  │
│ Caso de Uso (REF-05) │ no debe ser   │ Ports/Adapters para  │ vacías sin    │
│                      │ dueño del CU  │ Feign clients        │ polimorfismo  │
├──────────────────────┼───────────────┼──────────────────────┼───────────────┤
│ Modularizar switches │ Evita baja    │ Migrar repositorios  │ Incompatible  │
│ monolíticos (REF-07) │ cohesión y    │ en memoria a JPA     │ con alcance   │
│                      │ bugs futuros  │ antes de tiempo      │ de Entrega 1  │
├──────────────────────┼───────────────┼──────────────────────┼───────────────┤
│ Eliminar fugas HTTP  │ Limpieza de   │ Reemplazar Spring    │ Riesgo de     │
│ en App (REF-01)      │ capas y       │ Events por brokers   │ integración   │
│                      │ uniformidad   │ externos internos    │ innecesario   │
└──────────────────────┴───────────────┴──────────────────────┴───────────────┘
```

---

### 0.4 Code Smells frecuentes en un equipo de 10 integrantes
En proyectos grupales con división de roles, suelen emerger problemas característicos:
1. **Anemic Domain Model (Modelo Anémico):** Entidades convertidas en meras bolsas de getters/setters (`@Data` indiscriminado), mientras los `@Service` ejecutan todas las validaciones e invariantes de negocio.
2. **Duplicated Traversal (Navegación Duplicada):** Diferentes integrantes escriben independientemente la misma cadena de consultas para resolver un dato (ej. `DonacionIndependiente → Donacion → Donante → Persona`).
3. **God Service / Swiss-Army Service:** Servicios que acumulan 10+ dependencias inyectadas y métodos para cualquier operación relacionada con una palabra clave (ej. `DonacionesIndependientesService` agrupando 6 flujos operativos).
4. **Feature Envy (Envidia de Atributos):** Un servicio le pide repetidamente datos a una entidad para calcular algo que la entidad o un Value Object podrían calcular por sí mismos (`Tell, Don't Ask`).
5. **Divergent Representation (Modelos Divergentes):** Cada microservicio modela una versión distinta del mismo concepto (ej. `Persona` o `Donante`) con atributos incompatibles sin un contrato claro de replicación/sincronización.
6. **Leaky Framework Concerns (Fuga de Framework):** Clases de dominio importando librerías de infraestructura (ej. `@JsonIgnore` de Jackson en entidades, o `ResponseStatusException` de Spring en servicios).

---

### 0.5 Detección de responsabilidades mezcladas entre servicios
Para identificar si dos servicios tienen responsabilidades solapadas o límites difusos:
* **El síntoma del "Query-then-Mutate":** Si el Servicio A le pide toda la información interna al Servicio B por REST/Feign para tomar una decisión de negocio y luego le manda la orden modificada al Servicio B, la lógica de decisión debería pertenecer directamente al Servicio B.
* **Doble fuente de verdad:** Si dos servicios mutan y persisten el mismo dato conceptual (ej. el estado de una entrega siendo administrado en paralelo por donaciones y logística sin un evento de orquestación claro).
* **Contratos hinchados:** DTOs de eventos que envían grafos enteros de objetos en lugar de identificadores y datos mínimos de cambio de estado.

---

### 0.6 Criterio para separar Dominio, Aplicación e Infraestructura

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ GUÍA RÁPIDA DE DECISIÓN DE CAPAS                                            │
├─────────────────┬───────────────────────────────────────────────────────────┤
│ ¿Qué contiene?  │ Criterio de Ubicación                                     │
├─────────────────┼───────────────────────────────────────────────────────────┤
│ DOMAIN          │ • Invariantes de negocio (no pueden violarse nunca).      │
│                 │ • Entidades ricas, Value Objects, State Pattern.          │
│                 │ • Domain Services: Algoritmos de matching, políticas.     │
│                 │ • Puertos (interfaces) y contratos de Repositorios.       │
│                 │ • 🚫 CERO imports de Spring Web, Jackson, Feign o SQL.    │
├─────────────────┼───────────────────────────────────────────────────────────┤
│ APPLICATION     │ • Casos de uso: coordinación y secuencia de operaciones.   │
│                 │ • Transacciones, invocación de repositorios y eventos.    │
│                 │ • Mappers (Entity ↔ DTO).                                 │
│                 │ • Helpers de resolución entre agregados (ej. Resolver).   │
│                 │ • Fachadas de orquestación frente a Presentation.         │
├─────────────────┼───────────────────────────────────────────────────────────┤
│ INFRASTRUCTURE  │ • Adaptadores técnicos: Listeners (RabbitMQ, Events).     │
│                 │ • Clientes HTTP/Feign hacia otros microservicios.         │
│                 │ • Implementaciones de repositorios (EnMemoria, JPA).      │
│                 │ • Integraciones externas (IA, normalizadores semánticos). │
└─────────────────┴───────────────────────────────────────────────────────────┘
```

---

### 0.7 Preparación del código para Persistencia Relacional (JPA/Hibernate)
Para que el paso a base de datos relacional en la siguiente entrega sea fluido y sin fricción:
* **Identificadores Inmutables (Surrogate Keys):** Todas las entidades tienen un `UUID id` único e inmutable (preparado para claves primarias sin depender de datos naturales que puedan ser anonimizados, ver DTI-01).
* **Encapsulamiento de Colecciones:** Las entidades exponen listas mediante `Collections.unmodifiableList()`, obligando a usar métodos de negocio (`agregarItem()`, `quitarItem()`) en lugar de mutar la lista directamente.
* **Value Objects Inmutables:** Clases como `Bien`, `BienNormalizado` e `ItemDonacion` implementadas como Java `record`s, listas para mapearse como `@Embeddable` en JPA.
* **Postergación de P8:** Mantener temporalmente `List<DonacionIndependiente>` dentro de `NecesidadExtraordinaria` y reemplazarlo por colección de IDs o tabla intermedia `@OneToMany` únicamente al redactar los mapeos ORM de JPA.

---

### 0.8 Preparación para Integración Asincrónica Robusta
* **Idempotencia en Listeners:** Asegurar que si un evento (`DonacionNormalizadaEvent` o `EventoRutaIniciada`) se entrega más de una vez (semántica at-least-once), la aplicación no duplique registros ni corrompa estados.
* **Eventos Inmutables y Livianos:** Los eventos de dominio transportan identificadores y valores mínimos necesarios (`UUID donacionId`, `UUID necesidadId`), delegando a la capa de aplicación la carga de entidades.
* **Aislamiento del Listener:** El `@EventListener` o `@RabbitListener` solo desempaqueta el payload y delega a un Application Service con manejo de excepciones y logging adecuado.

---

### 0.9 Protocolo de Validación de No-Regresión
Para certificar que ningún refactor rompa el código previo:
1. **Línea base (Baseline):** Ejecutar `mvn clean test` en todo el repositorio antes de comenzar la Fase 1 y guardar el reporte de tests exitosos.
2. **Tests de Caracterización:** Proteger los endpoints existentes con `@WebMvcTest` y tests de integración (`CrossServiceCommunicationIT.java`).
3. **Validación PR por PR:** Ninguna PR se mergea si no pasan el 100% de los tests unitarios y de integración de forma local y en el pipeline de GitHub Actions.
4. **Verificación en Preproducción:** Ejecutar `./run-preprod-tests.sh` con Docker Compose para validar la interacción entre los 5 microservicios levantados.

---

## 1. Validación del Diagnóstico Previo

A continuación se resume la validación técnica de cada hallazgo contrastado directamente contra el código fuente:

* **P1 (`ProcesadorDeDonaciones` en `infrastructure/`) — CONFIRMADO:**
  Es un Application Service que orquesta el caso de uso asíncrono de normalización de donación, persiste entidades y dispara eventos de aplicación (`DonacionNormalizadaEvent`), pero reside erróneamente en la capa de infraestructura.
* **P2 (`SegmentacionEventListener` con orquestación completa) — CONFIRMADO:**
  Aloja el caso de uso completo de segmentación, guardado de donaciones independientes, persistencia y comunicación Feign con incentivos dentro del listener. Debe separarse el Adaptador de Eventos (técnico) del Application Service.
* **P3 (Algoritmos de asignación en `infrastructure/algoritmos/`) — CONFIRMADO:**
  `AlgoritmoAsignacion`, sus variantes y `StockDeDonaciones` contienen lógica de dominio pura y deben residir en la capa de Dominio (`models/domainServices/algoritmos/`).
* **P4 (`DonacionesIndependientesService.cambiarEstado()` monolítico) — CONFIRMADO:**
  Un único método con `switch` maneja 6 transiciones de negocio heterogéneas con efectos colaterales disímiles. Debe modularizarse por comportamiento semántico.
* **P5 (`AlgoritmosService` con baja cohesión) — CONFIRMADO:**
  Mezcla ejecución de algoritmos, políticas de consolidación, ciclo de vida de propuestas y notificaciones. Debe descomponerse claramente frente a `PropuestaService`.
* **P6 (`Necesidad.toDTO()` en el Dominio) — CONFIRMADO:**
  La entidad de dominio conoce la clase `NecesidadDTO`. Debe extraerse a un Mapper formal.
* **P8 (Referencia directa `NecesidadExtraordinaria` $\to$ `List<DonacionIndependiente>`) — POSPUESTO:**
  Se mantiene en memoria para esta iteración. Su desacoplamiento a nivel de IDs se pospone para la fase de persistencia JPA/relacional (DTI-01), evitando romper `cantidadAcumulada()` y algoritmos prematuramente.
* **P9 (`ResponseStatusException` en Application) — CONFIRMADO:**
  Fuga de preocupación HTTP en capa de aplicación. Debe reemplazarse por `RecursoNoEncontradoException` / `ValidationException`.
* **P10 (`@JsonIgnore` en `DonacionIndependiente`) — CONFIRMADO:**
  Dependencia de Jackson en el dominio; debe eliminarse.
* **P12 (Traversal repetido de actores) — CONFIRMADO:**
  Navegación relacional `DonacionIndependiente → Donacion → Donante → Persona` repetida en 3 clases. Se extraerá a un `DonacionActorResolver`.
* **P14 (`consolidar()` privado estático en servicio) — CONFIRMADO:**
  Regla de dominio oculta en un método privado. Se extraerá a `PoliticaConsolidacionPropuestas`.

---

## 2. Arquitectura Objetivo de `donaciones-service`

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ PRESENTATION LAYER                                                          │
│   controllers/                                                              │
│     ├── DonacionesController             (POST /api/donaciones, GET)        │
│     ├── DonacionesIndependientesController (PATCH .../estado)               │
│     ├── AsignacionController             (POST /api/asignaciones/ejecutar)  │
│     ├── PropuestasController             (GET, PUT .../estado)              │
│     └── NecesidadesController, etc.                                         │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ calls DTOs / APIs
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ APPLICATION LAYER                                                           │
│   services/                                                                 │
│     ├── DonacionesService                (Carga y consulta de donaciones)   │
│     ├── NormalizacionDonacionesService   (Orquesta normalización async)     │
│     ├── SegmentacionDonacionesService    (Orquesta segmentación e incentivos)│
│     ├── AsignacionDonacionesService      (Orquesta ejecución de algoritmos) │
│     ├── PropuestaService                 (Gestión de propuestas y lifecycle)│
│     ├── DonacionesIndependientesService  (Orquesta transiciones operativas) │
│     ├── DonacionActorResolver            (Resolución de Donante/Persona)    │
│     └── mappers/                         (Mappers Entity ↔ DTO)             │
└──────────────────────┬───────────────────────────────────────┬──────────────┘
                       │ uses domain models                    │ calls
                       ▼                                       ▼
┌──────────────────────────────────────────────┐ ┌────────────────────────────┐
│ DOMAIN LAYER                                 │ │ INFRASTRUCTURE LAYER       │
│   models/entities/                           │ │   infrastructure/          │
│     ├── donaciones/ (Donacion, Bien, Item)   │ │     ├── events/            │
│     ├── donacionesIndependientes/ (State)    │ │     │     └── Segmentacion │
│     ├── necesidades/ (Necesidad, etc.)       │ │     │           Listener   │
│     └── propuestas/ (Propuesta, Eventos)     │ │     ├── clients/ (Feign)   │
│   models/domainServices/ (o algoritmos/)     │ │     ├── analizadores/      │
│     ├── AlgoritmoAsignacion (Template)       │ │     ├── segmentadores/     │
│     ├── AlgoritmoCompatibilidadSemantica     │ │     ├── LogisticaListener  │
│     ├── AlgoritmoPrioridadSubAtendidos       │ │     └── repositories/impl/ │
│     ├── StockDeDonaciones                    │ └────────────────────────────┘
│     └── PoliticaConsolidacionPropuestas      │
│   models/ports/ (Segmentador)                │
│   models/repositories/ (Interfaces)          │
└──────────────────────────────────────────────┘
```

---

## 3. Mapa de Responsabilidades (Antes $\to$ Después)

| Clase | Responsabilidad Actual | Problema | Responsabilidad Objetivo | Acción |
|---|---|---|---|---|
| `ProcesadorDeDonaciones` | Normalización async en `infrastructure/` | Ubicación incorrecta | Application Service: normalización | Renombrar y mover a `services/impl/NormalizacionDonacionesService` |
| `SegmentacionEventListener` | Segmentación, incentivos y persistencia | Caso de uso en listener | Adaptador de eventos (entry point) | Conservar como listener técnico, delegar a `SegmentacionDonacionesService` |
| *Nueva*: `SegmentacionDonacionesService` | *No existía* (en listener) | Caso de uso sin servicio | Application Service: segmentar e incentivos | Crear en `services/impl/` |
| `AlgoritmoAsignacion` (+ subclases) | Matching en `infrastructure/` | Dominio en infra | Domain Services / Domain Algorithms | Mover a `models/domainServices/algoritmos/` |
| `StockDeDonaciones` | Gestión de stock en `infrastructure/` | Dominio en infra | Domain Helper | Mover a `models/domainServices/algoritmos/` |
| *Nueva*: `PoliticaConsolidacionPropuestas` | Método privado estático | Regla de negocio oculta | Domain Policy (método público testeable) | Extraer a `models/domainServices/` |
| `AlgoritmosService` | Algoritmos + Propuestas + Notificaciones | Baja cohesión | Application Service: ejecución de matching | Renombrar a `AsignacionDonacionesService`, delegar propuestas a `PropuestaService` |
| `PropuestaService` | Wrapper parcial + Event listener | Responsabilidad dividida | Application Service: ciclo de vida de propuestas | Asumir gestión de estado, historial y aprobación |
| `DonacionesIndependientesService` | Switch monolítico en `cambiarEstado()` | Baja cohesión | Casos de uso semánticos + dispatcher | Exponer métodos semánticos (`iniciarTraslado`, etc.) |
| `Necesidad` | Contiene `toDTO()` en la entidad | Acoplamiento Dominio $\to$ DTO | Entidad pura de dominio | Remover `toDTO()`, crear `NecesidadMapper` |
| *Nueva*: `NecesidadMapper` | *No existía* | Mapeo en entidad | Mapper formal DTO $\leftrightarrow$ Entity | Crear en `services/mappers/` |
| `DonacionIndependiente` | `@JsonIgnore` en `asignadaA` | Acoplamiento con Jackson | Entidad pura de dominio | Remover anotación `@JsonIgnore` |
| *Nueva*: `DonacionActorResolver` | Traversal duplicado en 3 clases | Duplicación de consultas | Componente de aplicación para resolver actores | Crear en `services/impl/` (o `services/`) |

---

## 4. Detalle de los Refactors Propuestos

---

### REF-01: Corrección de Fuga de Excepciones HTTP en Application Service

* **Objetivo:** Eliminar `ResponseStatusException` de `AlgoritmosService` y utilizar las excepciones estándar de `common-lib`.
* **Problema actual:** `AlgoritmosService` lanza `ResponseStatusException(HttpStatus.NOT_FOUND)` y `BAD_REQUEST`, acoplando la capa de aplicación con el framework Web/HTTP.
* **Evidencia:** [`AlgoritmosService.java:L174,L179`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/AlgoritmosService.java#L174-L179).
* **Responsabilidad actual:** Application Service maneja códigos de estado HTTP directamente.
* **Responsabilidad objetivo:** Application Service lanza excepciones de dominio/aplicación (`RecursoNoEncontradoException`, `ValidationException`).
* **Clases afectadas:** `grupo5.donaciones.services.impl.AlgoritmosService`.
* **Métodos afectados:** `actualizarEstadoPropuesta(UUID id, EstadoPropuesta estado)`.
* **Cambios conceptuales:** Desacoplar la capa de aplicación del protocolo HTTP.
* **Cambios estructurales:** Reemplazar `new ResponseStatusException(HttpStatus.NOT_FOUND)` por `new RecursoNoEncontradoException(id)` y `BAD_REQUEST` por `new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO)`.
* **Dependencias / Precondiciones:** `GlobalExceptionHandler` de `common-lib` (ya disponible).
* **Riesgos:** Ninguno.
* **Tests:** `PropuestasControllerTest.java`, `AlgoritmosServiceTest.java`.
* **Criterio de aceptación:** Cero imports de `ResponseStatusException` en `services/`.
* **Complejidad:** Muy Baja | **Riesgo:** Bajo

---

### REF-02: Desacoplamiento de DTOs y Jackson del Dominio

* **Objetivo:** Remover `Necesidad.toDTO()` y `@JsonIgnore` de `DonacionIndependiente`, delegando la transformación a un Mapper formal.
* **Problema actual:** `Necesidad` conoce y referencia `NecesidadDTO`. `DonacionIndependiente` tiene anotaciones de serialización web (`@JsonIgnore`).
* **Evidencia:** [`Necesidad.java:L49-62`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/necesidades/Necesidad.java#L49-L62), [`DonacionIndependiente.java:L30`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/donacionesIndependientes/DonacionIndependiente.java#L30).
* **Responsabilidad actual:** Las entidades conocen la representación externa y detalles de serialización.
* **Responsabilidad objetivo:** Dominio puro sin dependencias de DTOs ni librerías de serialización.
* **Clases afectadas:** `Necesidad.java`, `NecesidadExtraordinaria.java`, `NecesidadRecurrente.java`, `DonacionIndependiente.java`, `NecesidadesService.java`, *Nueva:* `NecesidadMapper.java`.
* **Métodos afectados:** `Necesidad.toDTO()`, `NecesidadesService.guardar()`, `NecesidadesService.obtenerPorId()`, `NecesidadesService.listarConFiltros()`.
* **Cambios conceptuales:** Separación estricta entre modelo de dominio y contrato de transferencia.
* **Cambios estructurales:**
  1. Crear `grupo5.donaciones.services.mappers.NecesidadMapper` como `@Component`.
  2. Eliminar métodos `toDTO()` en `Necesidad` y subclases.
  3. Eliminar `@JsonIgnore` en `DonacionIndependiente`.
  4. Inyectar `NecesidadMapper` en `NecesidadesService`.
* **Riesgos:** Errores de mapeo en tests que consumían `toDTO()`.
* **Tests:** `NecesidadesServiceTest.java`, `NecesidadesControllerTest.java`, *Nuevo:* `NecesidadMapperTest.java`.
* **Criterio de aceptación:** El paquete `models/entities/` no tiene dependencias hacia `grupo5.donaciones.dto` ni `com.fasterxml.jackson`.
* **Complejidad:** Baja | **Riesgo:** Bajo

---

### REF-03: Reubicación de Algoritmos de Asignación y Política de Consolidación al Dominio

* **Objetivo:** Mover los algoritmos de asignación y stock desde `infrastructure/algoritmos/` hacia `models/domainServices/algoritmos/`, y extraer la política de consolidación de propuestas con visibilidad pública de instancia.
* **Problema actual:** Lógica de asignación de dominio pura reside en `infrastructure/`. La regla de consolidación está oculta en un método privado estático de `AlgoritmosService`.
* **Evidencia:** [`infrastructure/algoritmos/*`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/infrastructure/algoritmos/AlgoritmoAsignacion.java), [`AlgoritmosService.java:L71-96`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/AlgoritmosService.java#L71-L96).
* **Responsabilidad actual:** Infraestructura aloja algoritmos de negocio; regla de consolidación atrapada en servicio.
* **Responsabilidad objetivo:** El Dominio contiene las políticas de asignación, algoritmos y reglas de consolidación. `PoliticaConsolidacionPropuestas` es una clase de dominio instanciable con método público para permitir testing unitario aislado con fixtures complejas.
* **Clases afectadas:**
  - Mover `AlgoritmoAsignacion`, `AlgoritmoCompatibilidadSemantica`, `AlgoritmoPrioridadSubAtendidos`, `StockDeDonaciones` a `grupo5.donaciones.models.domainServices.algoritmos`.
  - *Nueva:* `PoliticaConsolidacionPropuestas` en `grupo5.donaciones.models.domainServices`.
  - `AlgoritmosService.java` (actualizar imports y delegar consolidación).
* **Métodos afectados:** `AlgoritmosService.consolidar()`, `AlgoritmosService.ejecutar()`.
* **Cambios conceptuales:** Reclasificación de Domain Services y Domain Policies según DDD.
* **Cambios estructurales:** Repackaging de 4 clases de algoritmos, creación de `PoliticaConsolidacionPropuestas` y actualización de imports.
* **Riesgos:** Ajuste de imports en tests de algoritmos.
* **Tests:** `AlgoritmoCompatibilidadSemanticaTest`, `AlgoritmoPrioridadSubAtendidosTest`, `StockDeDonacionesTest`, *Nuevo:* `PoliticaConsolidacionPropuestasTest`.
* **Criterio de aceptación:** El directorio `infrastructure/algoritmos/` deja de existir; `PoliticaConsolidacionPropuestas` se prueba unitariamente de forma aislada.
* **Complejidad:** Baja | **Riesgo:** Bajo

---

### REF-04: Reubicación de Normalización de Donaciones a Application Service

* **Objetivo:** Transformar `ProcesadorDeDonaciones` en un Application Service formal (`NormalizacionDonacionesService`) en el paquete `services/impl/`.
* **Problema actual:** `ProcesadorDeDonaciones` está en `infrastructure/` pero orquesta el caso de uso de normalización asíncrona, persiste entidades y avanza estados.
* **Evidencia:** [`ProcesadorDeDonaciones.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/infrastructure/ProcesadorDeDonaciones.java).
* **Responsabilidad actual:** Infraestructura orquesta el flujo de carga y normalización.
* **Responsabilidad objetivo:** Application Service orquesta el caso de uso `normalizarDonacionAsync(Donacion)` coordinando analizadores y repositorios.
* **Clases afectadas:**
  - `infrastructure/ProcesadorDeDonaciones.java` $\to$ mover/renombrar a `services/impl/NormalizacionDonacionesService.java` (implementando `INormalizacionDonacionesService`).
  - `DonacionesService.java` (inyectar la nueva interfaz).
* **Métodos afectados:** `DonacionesService.cargarDonacion()`, `NormalizacionDonacionesService.procesar()`.
* **Cambios conceptuales:** Claridad de que el procesamiento de normalización es un caso de uso de la aplicación.
* **Cambios estructurales:** Creación de interface en `services/`, implementación en `services/impl/`, eliminación en `infrastructure/`. Spring `@Async` se mantiene en el nuevo servicio.
* **Riesgos:** Modificación de inyección en `DonacionesService`.
* **Tests:** Renombrar y adaptar `NormalizacionDonacionesServiceTest.java`, `DonacionesServiceTest.java`.
* **Criterio de aceptación:** `infrastructure/` no contiene servicios de orquestación de normalización.
* **Complejidad:** Baja | **Riesgo:** Bajo

---

### REF-05: Desacoplamiento de Segmentación (Listener Fino + Application Service)

* **Objetivo:** Separar la responsabilidad de Adaptador de Eventos (técnica) del Caso de Uso de Segmentación (aplicación).
* **Problema actual:** `SegmentacionEventListener` en `infrastructure/events/` contiene toda la lógica de segmentación, coordinación con incentivos y mutación de estado.
* **Evidencia:** [`SegmentacionEventListener.java:L41-88`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/infrastructure/events/SegmentacionEventListener.java#L41-L88).
* **Responsabilidad actual:** El listener técnico es dueño del caso de uso completo (9 dependencias).
* **Responsabilidad objetivo:**
  - `SegmentacionEventListener` (Infrastructure): captura `DonacionNormalizadaEvent` y delega inmediatamente a la aplicación (1 dependencia).
  - `SegmentacionDonacionesService` (Application): ejecuta el caso de uso `segmentarDonacion(UUID donacionId)`.
* **Clases afectadas:**
  - `infrastructure/events/SegmentacionEventListener.java` (adaptador fino).
  - *Nueva:* `grupo5.donaciones.services.ISegmentacionDonacionesService` e `impl.SegmentacionDonacionesService`.
* **Métodos afectados:** `SegmentacionEventListener.onDonacionNormalizada()`.
* **Cambios estructurales:** Extraer la lógica de orquestación del listener hacia el nuevo servicio.
* **Riesgos:** Ninguno funcional si se preserva la firma del evento.
* **Tests:** `SegmentacionDonacionesServiceTest.java` (unitario puro) y `SegmentacionEventListenerTest` (delegación).
* **Criterio de aceptación:** `SegmentacionEventListener` tiene solo 1 dependencia inyectada (`ISegmentacionDonacionesService`) y no contiene lógica de negocio ni persistencia directa.
* **Complejidad:** Media | **Riesgo:** Bajo

---

### REF-06: Extracción de `DonacionActorResolver` para Notificaciones

* **Objetivo:** Unificar la lógica repetida de navegación entre agregados (`DonacionIndependiente → Donacion → Donante → Persona`) en un servicio/helper reutilizable de la capa de aplicación con nomenclatura clara.
* **Problema actual:** Código duplicado de traversal en `DonacionesIndependientesService`, `SegmentacionEventListener` y `AlgoritmosService`.
* **Evidencia:** Métodos privados idénticos en 3 servicios distintos.
* **Responsabilidad actual:** Cada servicio navega manualmente múltiples repositorios para armar DTOs de notificación e incentivos.
* **Responsabilidad objetivo:** `DonacionActorResolver` en `services/` (o `services/impl/`) resuelve las identidades requeridas a partir de una `DonacionIndependiente` o `Donacion`.
* **Clases afectadas:**
  - *Nueva:* `grupo5.donaciones.services.impl.DonacionActorResolver`.
  - `DonacionesIndependientesService.java`, `SegmentacionDonacionesService.java`, `AlgoritmosService.java` / `PropuestaService.java`.
* **Métodos afectados:** `obtenerDonanteId()`, `obtenerPersonaDonanteId()`, `obtenerPersonaBeneficiariaId()`.
* **Cambios estructurales:** Creación de la clase `DonacionActorResolver` e inyección en los servicios consumidores.
* **Riesgos:** Muy bajo.
* **Tests:** *Nuevo:* `DonacionActorResolverTest.java`.
* **Criterio de aceptación:** No existen métodos privados duplicados para navegación Donacion $\to$ Donante $\to$ Persona en los servicios.
* **Complejidad:** Baja | **Riesgo:** Muy Bajo

---

### REF-07: Modularización por Comportamiento de `DonacionesIndependientesService` y Desacoplamiento de `LogisticaEventListener`

* **Objetivo:** Dividir el método monolítico `cambiarEstado()` en métodos de caso de uso semánticos y cohesivos, manteniendo el dispatcher para retrocompatibilidad con el Controller, y actualizar `LogisticaEventListener` para que invoque directamente los métodos semánticos.
* **Problema actual:** Un único método `cambiarEstado()` con `switch` ejecuta 6 transiciones radicalmente diferentes. Además, `LogisticaEventListener` construye DTOs artificiales para invocar el dispatcher genérico.
* **Evidencia:** [`DonacionesIndependientesService.java:L66-90`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/DonacionesIndependientesService.java#L66-L90), [`LogisticaEventListener.java:L36-92`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/infrastructure/LogisticaEventListener.java#L36-L92).
* **Responsabilidad actual:** Servicio monolítico y listener técnico acoplado a un DTO genérico de cambio de estado.
* **Responsabilidad objetivo:**
  - `DonacionesIndependientesService` expone métodos específicos y semánticos por caso de uso:
    - `asignar(UUID id, UUID necesidadId, String actor)`
    - `iniciarTraslado(UUID id, String urlMapa, String actor)`
    - `confirmarEntrega(UUID id, String patenteCamion, String actor)`
    - `registrarFallaEntrega(UUID id, String justificacion, Boolean replanificable, String actor)`
    - `marcarVencida(UUID id, String actor)`
    - `retornarAlDeposito(UUID id, String actor)`
    - `cambiarEstado(UUID, DTO, actor)` como dispatcher para el Controller REST.
  - `LogisticaEventListener`: invoca directamente `iniciarTraslado()`, `confirmarEntrega()`, etc., sin instanciar `CambioEstadoDonacionIndependienteRequestDTO`.
* **Clases afectadas:** `IDonacionesIndependientesService.java`, `DonacionesIndependientesService.java`, `LogisticaEventListener.java`.
* **Riesgos:** Bajo (los flujos internos ya están casi aislados en métodos privados; se formalizan como métodos de caso de uso).
* **Tests:** `DonacionesIndependientesServiceTest.java`, `LogisticaEventListenerTest.java`.
* **Criterio de aceptación:** `LogisticaEventListener` llama directamente a métodos semánticos; el dispatcher enruta limpiamente; tests unitarios cubren cada método por separado.
* **Complejidad:** Media | **Riesgo:** Bajo

---

### REF-08: Reestructuración y Alta Cohesión entre `AsignacionDonacionesService` y `PropuestaService`

* **Objetivo:** Clarificar las responsabilidades: `AsignacionDonacionesService` (ejecución de matching de donaciones) y `PropuestaService` (gestión del ciclo de vida, confirmación y aprobación de propuestas).
* **Problema actual:** `AlgoritmosService` ejecuta algoritmos pero también aprueba propuestas, maneja eventos y notifica. `PropuestaService` actúa en parte como wrapper y en parte como event listener.
* **Evidencia:** [`AlgoritmosService.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/AlgoritmosService.java) y [`PropuestaService.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/PropuestaService.java).
* **Responsabilidad objetivo:**
  - `AsignacionDonacionesService` (Application): orquesta la ejecución de algoritmos sobre necesidades y donaciones, aplica `PoliticaConsolidacionPropuestas` y guarda las propuestas generadas.
  - `PropuestaService` (Application): gestiona el listado, consulta, cambio de estado, aprobación (`confirmar`), fragmentación de donaciones y despacho a logística/notificaciones.
* **Clases afectadas:**
  - Renombrar `AlgoritmosService` $\to$ `AsignacionDonacionesService` (implementando `IAsignacionDonacionesService`).
  - Mover `aprobarPropuesta()`, `actualizarEstadoPropuesta()` y `listarPropuestas()` íntegramente a `PropuestaService`.
  - `AsignacionController.java` y `PropuestasController.java` (inyectar interfaces).
* **Riesgos:** Ajuste de inyección de dependencias en controllers.
* **Tests:** `AsignacionControllerTest`, `PropuestasControllerTest`, `AsignacionDonacionesServiceTest`, `PropuestaServiceTest`.
* **Criterio de aceptación:** `AsignacionDonacionesService` tiene solo 4 dependencias; `PropuestaService` gestiona el ciclo de vida completo de propuestas; controllers inyectan interfaces.
* **Complejidad:** Media | **Riesgo:** Medio

---

### REF-09: Creación del Diagrama PlantUML de Dominio y Capas de `donaciones-service`

* **Objetivo:** Documentar el modelo de dominio y casos de uso en `docs/design/donaciones-service/diagrama-de-clases-donaciones.puml`, reflejando fielmente el diseño resultante.
* **Problema actual:** Falta documentación gráfica de clases para el servicio principal del proyecto.
* **Responsabilidad objetivo:** Diagrama PlantUML claro, estructurado con la paleta de estilo común (`donatrack-style.puml`), que muestre:
  - Entidades de dominio con su comportamiento (`Donacion`, `DonacionIndependiente`, `Necesidad`, `Propuesta`, `Bien`).
  - El State Pattern de `DonacionIndependiente`.
  - Los Domain Services / Algoritmos de Asignación y Políticas de Consolidación.
  - Los puertos (`Segmentador`) y contratos de repositorio.
  - La relación conceptual con los casos de uso principales sin ruido accidental.
* **Clases afectadas:** *Nuevo archivo:* `docs/design/donaciones-service/diagrama-de-clases-donaciones.puml`.
* **Riesgos:** Ninguno sobre el código.
* **Criterio de aceptación:** El archivo `.puml` compila limpiamente y el dominio permite comprender el negocio aun si se ocultan los servicios de aplicación.
* **Complejidad:** Media | **Riesgo:** Nulo

---

## 5. Secuencia y Dependencias entre Pull Requests

```text
┌─────────────────────────────────────────────────────────────────────────────┐
│ PR 1: Limpieza de Dominio y Desacoplamiento Básico                          │
│   ├── REF-01 (Fuga HTTP en Application resuelta)                            │
│   ├── REF-02 (toDTO y @JsonIgnore fuera del Dominio + NecesidadMapper)      │
│   └── REF-03 (Algoritmos/Stock al Dominio + PoliticaConsolidacion pública)  │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ (Dominio limpio y testeable de forma aislada)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PR 2: Pipeline de Carga, Normalización y Segmentación                       │
│   ├── REF-06 (DonacionActorResolver en Application)                         │
│   ├── REF-04 (NormalizacionDonacionesService en Application)                │
│   └── REF-05 (SegmentacionDonacionesService + Listener Fino)                │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ (Pipeline asíncrono ordenado sin duplicación)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PR 3: Modularización Operativa de Donaciones Independientes                 │
│   └── REF-07 (Métodos semánticos + LogisticaEventListener desacoplado)     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ (Eliminación del switch monolítico)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PR 4: Cohesión de Asignación y Ciclo de Vida de Propuestas                  │
│   └── REF-08 (AsignacionDonacionesService vs PropuestaService)              │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ (Separación clara: Matching vs Lifecycle)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ PR 5: Documentación Arquitectónica Oficial                                  │
│   └── REF-09 (Diagrama PlantUML oficial donaciones-service)                 │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 6. Qué NO Refactorizar (Protección de Diseño Existente)

1. **Controllers de todos los microservicios:** Cumplen su rol como adaptadores HTTP limpios, validando DTOs y delegando a la aplicación.
2. **State Pattern de `DonacionIndependiente`:** Las clases `EnDeposito`, `AsignacionRealizada`, `ListaParaEntregar`, `EnTraslado`, `Entregada`, `EntregaFallida`, `Vencida` implementan un patrón ejemplar con invariantes protegidas.
3. **Entidades con transiciones directas (`Donacion`, `Propuesta`):** Sus métodos (`marcarNormalizada()`, `marcarSegmentada()`, `confirmar()`) encapsulan correctamente sus estados.
4. **Logística-service en su totalidad:** Sus entidades (`Entrega`, `Ruta`, `Camion`, `Chofer`), servicios y publicación de eventos RabbitMQ están excelentemente desacoplados.
5. **Notificaciones-service e Incentivos-service:** No requieren cambios de estructura interna.
6. **Mecanismo de comunicación asíncrona (RabbitMQ y Spring Events):** Las anotaciones `@RabbitListener`, `@EventListener` y `@Async` se conservan como herramientas técnicas de infraestructura.
7. **`common-lib`:** Su jerarquía de excepciones (`BusinessStateException`, `ValidationException`, etc.) y repositorios base (`CrudRepository`) no se modifican.
8. **Referencia directa `NecesidadExtraordinaria` $\to$ `DonacionIndependiente` (P8):** Se pospone expresamente para la etapa de base de datos relacional (DTI-01), evitando sobreingeniería prematura.
9. **ADRs existentes:** Se mantienen como histórico inmutable de decisiones.

---

## 7. Estrategia de Testing y Validación

### Validación por Nivel:

1. **Dominio Puro (Unit Tests):**
   - Verificar transiciones invariantes de `DonacionIndependiente` y sus clases de estado.
   - Verificar `PoliticaConsolidacionPropuestasTest` de forma aislada con fixtures complejas de propuestas superpuestas y disjuntas.
   - Verificar que los algoritmos de asignación ordenen y filtren idénticamente en su nueva ubicación.
2. **Servicios de Aplicación (Unit Tests con Mocks):**
   - `NormalizacionDonacionesServiceTest` y `SegmentacionDonacionesServiceTest` validando el encadenamiento de guardado y disparo de eventos.
   - `DonacionActorResolverTest` validando resolución de IDs de personas donantes y beneficiarias.
   - `DonacionesIndependientesServiceTest` testeando cada método semántico (`iniciarTraslado`, `confirmarEntrega`, etc.) por separado.
3. **Controladores y Adaptadores (WebMvc / Listener Tests):**
   - `@WebMvcTest` asegurando que ningún cambio de servicio altere códigos HTTP ni contratos JSON.
   - `LogisticaEventListenerTest` verificando que los eventos de RabbitMQ invocan directamente los métodos semánticos.
4. **Pruebas de Integración E2E:**
   - Ejecutar la suite `CrossServiceCommunicationIT.java` para validar el flujo completo inter-servicio (Donaciones $\to$ Incentivos $\to$ Notificaciones $\to$ Logística).

---

## 8. Resumen Ejecutivo de Refactors

| Refactor | Prioridad | Beneficio | Complejidad | Riesgo | Pull Request |
|---|---|---|---|---|---|
| **REF-01** (HTTP Exceptions) | Alta | Elimina fuga de presentación en Application | Muy Baja | Muy Bajo | **PR 1** |
| **REF-02** (DTOs/Jackson fuera de Dominio) | Alta | Dominio puro sin acoplamiento externo | Baja | Bajo | **PR 1** |
| **REF-03** (Algoritmos/Stock al Dominio) | Alta | Corrige ubicación de lógica pura de negocio | Baja | Bajo | **PR 1** |
| **REF-06** (`DonacionActorResolver`) | Media | Elimina código duplicado de navegación | Baja | Muy Bajo | **PR 2** |
| **REF-04** (Normalización a App Service) | Crítica | Corrige caso de uso ubicado en infraestructura | Baja | Bajo | **PR 2** |
| **REF-05** (Separar Listener de Segmentación) | Crítica | Convierte listener en adaptador fino | Media | Bajo | **PR 2** |
| **REF-07** (Modularizar DonacionesIndependientes) | Alta | Elimina método monolítico, llamadas directas en listener | Media | Bajo | **PR 3** |
| **REF-08** (Separar Asignacion y Propuestas) | Alta | Cohesión en servicios y controladores | Media | Medio | **PR 4** |
| **REF-09** (Diagrama PlantUML) | Alta | Documentación arquitectónica completa | Media | Nulo | **PR 5** |
| *P8 (Referencia agregados Necesidad ↔ DI)* | Media | Desacoplamiento estricto de IDs | Alta | Alto | **POSPONER (Persistencia)** |
