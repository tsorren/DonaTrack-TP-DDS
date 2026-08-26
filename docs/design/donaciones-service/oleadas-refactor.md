Comentarios de oleadas previas:

Oleada 1:
FIX-00 y RF Archivo:
baseline compilable y decision de finalizacion en dominio

FIX-00: agrega Propuesta.confirmar() como unica API publica de aprobacion, delegando a aceptar(null). Corrige llamada sin argumentos en AsignacionService que impedia compilar el modulo.

RF-Archivo: mueve la decision PROCESADO vs PROCESADO_CON_ERRORES desde ImportadorService hacia Archivo.finalizarProcesamiento(int erroresDeNegocio), alineando el codigo con el Diagrama de Clases. El service pasa de decidir el estado a informar el resultado; la entidad decide su propio ciclo de vida.

RF-Persona:
## Problema

La decisión de si dos personas son la misma (duplicados al importar un CSV de donantes) vivía
fuera de la entidad `Persona`, repartida en un Strategy de aplicación
(`CriterioDuplicado` + `CriterioPorDocumento` + `CriterioPorMedioDeContacto` +
`ValidadorPersonaDuplicada`). Además existía `IValidadorPersonaDuplicada`, una interfaz sin
ninguna implementación (código huérfano).

## Evidencia

El diagrama de clases actualizado (`diagrama-de-clases-donaciones.puml`) define
`esDuplicadaDe(Persona): Boolean` directamente en `Persona`, `Humana` y `Juridica`, sin ningún
Strategy de comparación — es decir, la comparación par a par es una regla de dominio que debe
vivir en la entidad (Tell-Don't-Ask), no en un servicio de aplicación.

## Objetivo

- Mover la lógica de comparación (documento exacto + medio de contacto: correo case-insensitive,
  teléfono por sufijo de dígitos incluyendo WhatsApp) a `Persona.esDuplicadaDe(Persona)`.
- `Humana`/`Juridica` overridean el método delegando en `super.esDuplicadaDe(...)` — por ahora no
  agregan criterios propios del subtipo (ver "Fuera de scope").
- Adelgazar `ValidadorPersonaDuplicada`: ya no orquesta una lista de Strategies, solo recorre
  `IPersonasRepository` preguntándole a cada persona existente si es duplicada de la que se
  importa.
- Eliminar `CriterioDuplicado`, `CriterioPorDocumento`, `CriterioPorMedioDeContacto` e
  `IValidadorPersonaDuplicada` (Strategy reemplazada / interfaz huérfana).

## Fuera de scope

- No se agregaron criterios de duplicado nuevos por subtipo (p. ej. `Humana` comparando
  nombre+apellido+fechaNacimiento, `Juridica` por razón social). No existen hoy y definirlos
  implica una decisión de negocio no validada — candidato a un RF propio si se necesita.
- No se tocó `ImportadorService` (su única dependencia pública, `buscarDuplicado(Persona)`,
  mantiene la misma firma).

## Tests

- Characterization tests escritos primero contra el código viejo (`CriterioPorDocumentoTest`,
  `CriterioPorMedioDeContactoTest`, `ValidadorPersonaDuplicadaTest` orquestando Strategies),
  confirmados en verde antes de mover nada.
- Portados a `PersonaTest` (comparación documento/correo/teléfono, guards de null/blank,
  `Juridica` delega correctamente) y a `ValidadorPersonaDuplicadaTest` reescrito para la firma
  nueva (`IPersonasRepository`).
- `HumanaTest`/`JuridicaTest` corridos sin cambios para confirmar que no rompí nada existente.

## Diseño resultante

`Persona.esDuplicadaDe` concentra la regla de dominio (documento O medio de contacto en común).
`ValidadorPersonaDuplicada` queda como el único punto de acceso a datos (recorre el repositorio),
sin decidir qué significa "ser duplicada". Se evaluaron 3 alternativas (modelo rico puro / mantener
Strategy / híbrido) — se eligió modelo rico puro por alinear con el DC y por YAGNI (hoy no hay
ningún requisito de criterios de duplicado configurables/plugables).

## IA utilizada

Análisis del DC y del código actual, comparación de alternativas de diseño, generación de
characterization tests, implementación del refactor y de los tests nuevos.


RF-Propuesta:
### Problema
Existía ambigüedad en la API de aprobación de `Propuesta` (`confirmar` vs `aceptar`), ausencia de guardas de transición de estado en el Aggregate Root y una lista no tipada (`List<Object>`) de domain events que no heredaba de un contrato transversal.

### Evidencia
- `Propuesta.java` exponía `confirmar()` que omitía el actor de auditoría.
- `domainEvents` utilizaba `List<Object>`.
- No se impedían transiciones inválidas desde estados finales (`APROBADA` o `DESCARTADA`).

### Objetivo
- Consolidar a `Propuesta` como el Aggregate Root de referencia del patrón Domain Events.
- Crear la clase abstracta transversal `EventoDeDominio` en `common-lib` (`id`, `timestamp`) y hacer que `PropuestaAprobada` herede de ella.
- Estandarizar la API de aprobación exclusivamente en `aceptar(String actor)`.
- Tipar la colección como `List<PropuestaAprobada>` y asegurar inmutabilidad al exponerla (`Collections.unmodifiableList`).

### Fuera de Scope
- Refactor de `PosibleFragmentacion.confirmar()` (Oleada 4).
- Extracción de `GestorPropuestasDeAsignacion` y `consolidar()` (Oleada 4).
- Modificaciones al listener `onPropuestaAprobada` o integración con logística.

### Tests
- `PropuestaTest`: 100% de cobertura sobre transiciones de estado, guardas `PENDIENTE`, asignación de actor (`explícito`, `nulo`, `en blanco`), inmutabilidad de la lista y ciclo de `clearDomainEvents`.
- `AsignacionServiceTest`: verificación de interacción con `aceptar("SISTEMA")`.
- Suite completa `mvn clean test` pasando en verde en todos los módulos.

### Diseño Resultante
`Propuesta` es un Aggregate Root puro sin acoplamiento a frameworks. Maneja su ciclo de vida y registra `PropuestaAprobada` (que extiende `EventoDeDominio`). El Application Service persiste la entidad, publica los eventos en el bus de Spring y finalmente invoca `clearDomainEvents()`.

### Verificación
- Formateo: `mvn spotless:check` ✅
- Build y tests: `mvn clean test` (Reactor Build Success) ✅

Oleada 2:
# Oleada 2: Refactor de Domain Events en `Donacion` y Desduplicación de Normalización (RF-04 y RF-05)

## Problema

1. **Falta de Domain Events internos en Aggregate Root**:
   [`Donacion`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/donaciones/Donacion.java) no gestionaba sus propios eventos de dominio durante sus transiciones de estado (`CARGADA`, `NORMALIZADA`, `SEGMENTADA`). Las notificaciones se disparaban mediante eventos de aplicación ad-hoc creados y publicados manualmente desde servicios e infraestructura (`eventPublisher.publishEvent(new DonacionNormalizadaEvent(...))`).
2. **Duplicación de Regla de Negocio (Normalización)**:
   La regla para evaluar si una donación completó su normalización (verificar que no queden ítems en `PENDIENTE_REVISION`) estaba duplicada con idéntica lógica imperativa en dos clases: [`ProcesadorDeDonaciones`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/infrastructure/ProcesadorDeDonaciones.java) e [`ItemDonacionNormalizadoService`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/services/impl/ItemDonacionNormalizadoService.java).
3. **Violación de *Tell, Don't Ask***:
   [`ItemDonacionNormalizado`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/itemsNormalizados/ItemDonacionNormalizado.java) actuaba como una entidad anémica donde los servicios navegaban sus atributos internos (`item.getBien().estadoNormalizacion() == PENDIENTE_REVISION`) para tomar decisiones.

---

## Evidencia

- **Diagrama de Clases ([`donaciones-clases.puml`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/docs/design/donaciones-service/lucid/donaciones-clases.puml))**:
  - `Donacion` contiene `- eventos: List<EventoDonacion>`, `- registrarEvento(EventoDonacion)` y `+ limpiarEventos(): void`.
  - Los eventos heredan de una base común (`EventoDonacion`).
- **Código Previo**:
  ```java
  // Duplicado en ProcesadorDeDonaciones L78-86 e ItemDonacionNormalizadoService L163-177:
  boolean tienePendientes = items.stream()
      .anyMatch(i -> i.getBien().estadoNormalizacion() == EstadoNormalizacion.PENDIENTE_REVISION);
  if (!tienePendientes) {
      donacion.marcarNormalizada();
      donacionRepository.save(donacion);
      eventPublisher.publishEvent(new DonacionNormalizadaEvent(donacionId));
  }
  ```

---

## Objetivo

1. **Implementar Domain Events en [`Donacion`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/donaciones/Donacion.java) (RF-04)**:
   - Crear jerarquía [`EventoDonacion`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/donaciones/events/EventoDonacion.java) (`DonacionCargada`, [`DonacionNormalizada`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/donaciones/events/DonacionNormalizada.java), [`DonacionSegmentada`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/donaciones/events/DonacionSegmentada.java)) extendiendo de [`EventoDeDominio`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/common-lib/src/main/java/grupo5/common/events/EventoDeDominio.java) de `common-lib`.
   - Encapsular `domainEvents` en `Donacion` con `getDomainEvents()` inmodificable y `clearDomainEvents()`.
   - Reemplazar `IllegalStateException` por `BusinessStateException(ErrorCatalog.ESTADO_DONACION_TRANSICION_INVALIDA)`.
2. **Encapsular y Desduplicar la Normalización (RF-05)**:
   - Añadir métodos de comportamiento semántico en [`ItemDonacionNormalizado`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/itemsNormalizados/ItemDonacionNormalizado.java): `estaPendienteDeRevision()`, `estaResuelto()`, `estaAceptado()`.
   - Crear la política de dominio [`EvaluadorNormalizacion.estanTodosNormalizados(items)`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/itemsNormalizados/EvaluadorNormalizacion.java).
   - Eliminar `DonacionNormalizadaEvent` y suscribir [`SegmentacionEventListener`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/infrastructure/events/SegmentacionEventListener.java) directamente al domain event [`DonacionNormalizada`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/main/java/grupo5/donaciones/models/entities/donaciones/events/DonacionNormalizada.java).
   - Unificar el ciclo en Application Services: mutar entidad $\rightarrow$ guardar $\rightarrow$ publicar eventos $\rightarrow$ limpiar eventos.

---

## Fuera de scope

- **Máquina de estados de `DonacionIndependiente`**: Se mantiene intacta para la Oleada 3.
- **Creación de `SolicitudCambioEstadoDonacionIndependiente`**: Diferido a la Oleada 3.
- **Algoritmos de asignación y `PosibleFragmentacion`**: Diferido a la Oleada 4.
- **Reubicación de paquetes de infraestructura**: Diferido a la Oleada 6.
- **Inmutabilidad de `Bien`**: Se preservó como Value Object crudo inmutable respetando el ADR de 2026-06-13.

---

## Tests

1. **[`DonacionTest.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/test/java/grupo5/donaciones/models/entities/donaciones/DonacionTest.java)**:
   - Validación de creación e inicialización con `DonacionCargada`.
   - Transiciones válidas e inválidas de estados (`CARGADA` $\rightarrow$ `NORMALIZADA` $\rightarrow$ `SEGMENTADA`).
   - Inmutabilidad de la lista de `domainEvents` y limpieza con `clearDomainEvents()`.
   - Invariantes de agregación y remoción de `ItemDonacion`.
2. **[`EvaluadorNormalizacionTest.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/test/java/grupo5/donaciones/models/entities/itemsNormalizados/EvaluadorNormalizacionTest.java)**:
   - Evaluación de listas con ítems pendientes (retorna `false`).
   - Evaluación de listas con todos los ítems resueltos/aceptados/rechazados (retorna `true`).
   - Comportamiento de métodos semánticos en `ItemDonacionNormalizado`.
3. **Tests de Servicios y Listeners Actualizados**:
   - [`ProcesadorDeDonacionesTest`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/test/java/grupo5/donaciones/infrastructure/ProcesadorDeDonacionesTest.java), [`ItemDonacionNormalizadoServiceTest`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/test/java/grupo5/donaciones/services/ItemDonacionNormalizadoServiceTest.java), [`SegmentacionEventListenerTest`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/test/java/grupo5/donaciones/infrastructure/SegmentacionEventListenerTest.java), [`DonacionesServiceTest`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/donaciones-service/src/test/java/grupo5/donaciones/services/DonacionesServiceTest.java).

---

## Diseño resultante

- **Aggregate Root con límites claros**: `Donacion` solo es responsable de su ciclo de vida y la emisión de sus eventos (`DonacionCargada`, `DonacionNormalizada`, `DonacionSegmentada`), sin acoplarse a normalizadores semánticos de infraestructura.
- **Separación de responsabilidades y Desduplicación**: `EvaluadorNormalizacion` centraliza la regla de negocio pura aplicable tanto al pipeline automático inicial como a la revisión manual del administrador.
- **Tell, Don't Ask**: `ItemDonacionNormalizado` encapsula el conocimiento sobre su estado de revisión.
- **Coreografía desacoplada por eventos**: `SegmentacionEventListener` reacciona de forma transparente a `DonacionNormalizada` sin importar si la normalización fue inmediata o manual.

---

## IA utilizada

- **Análisis arquitectónico**: Evaluación de diagramas UML, ADRs históricos de inmutabilidad de bienes y comparación de flujos automáticos vs manuales bajo DDD.
- **Diseño e Implementación**: Creación de la jerarquía de eventos de dominio, encapsulación en Aggregate Root y extracción de `EvaluadorNormalizacion`.
- **Generación de Pruebas**: Construcción de suites de caracterización unitaria (`DonacionTest`, `EvaluadorNormalizacionTest`) y actualización de mock verifications.
- **Formateo y Verificación**: Ejecución y corrección con `mvn spotless:apply` y validación del build multi-módulo (`mvn clean test`).

---

## Verificación humana

- Verificación de la jerarquía de eventos heredando de `EventoDeDominio` en `common-lib`.
- Validación de que `Bien` permanece como record inmutable y la mutabilidad/estado reside en `BienNormalizado`/`ItemDonacionNormalizado`.
- Comprobación de que la regla `estanTodosNormalizados` no quedó duplicada y se invoca homogéneamente en los dos flujos.
- Ejecución limpia de la suite de pruebas multi-módulo del reactor (`BUILD SUCCESS`, 0 errores, 0 fallos).

Oleada 3:
# Oleada 3: Domain Events de `DonacionIndependiente` y `SolicitudCambioEstadoDonacionIndependiente` (RF-06 y RF-07)

## Problema
- `DonacionIndependiente` no registraba eventos de dominio internamente y no disponía del método `cambiarEstado(SolicitudCambioEstadoDonacionIndependiente)`.
- `DonacionesIndependientesService` contenía un switch anémico de 6 ramas que acoplaba el caso de uso con 9 dependencias (repositorios de donantes, personas, entidades y clientes Feign de incentivos y notificaciones), orquestando llamadas remotas síncronas en medio de la transición de estado.

## Evidencia
- En `donaciones-clases.puml` (L375-404 y L613-642), se especifica `SolicitudCambioEstadoDonacionIndependiente`, `DonacionIndependiente.cambiarEstado(solicitud)` y la jerarquía `EventoDonacionIndependiente` (`EventoDonacionAsignada`, `EventoRutaIniciada`, `EventoDonacionRecibida`, `EventoDonacionFallida`).
- El código previo delegaba la lógica de negocio y llamadas externas directamente en el switch del servicio de aplicación.

## Objetivo
1. Crear la jerarquía `EventoDonacionIndependiente` heredando de `EventoDeDominio` (`common-lib`) y gestionar los `domainEvents` dentro de `DonacionIndependiente`.
2. Crear el parameter object de dominio `SolicitudCambioEstadoDonacionIndependiente` y delegar las transiciones a los estados concretos del patrón State.
3. Extraer las llamadas remotas hacia `DonacionIndependienteNotificacionesListener`, reduciendo las dependencias del Application Service de 9 a 4.

## Fuera de scope
- Lógica de asignación y algoritmos (`StockDeDonaciones`, `PosibleFragmentacion.confirmar`) diferida a la Oleada 4.
- Mapeos de DTOs en `Necesidad` diferida a la Oleada 5.

## Tests
- `DonacionIndependienteEstadosTest`: Transiciones completas con `SolicitudCambioEstadoDonacionIndependiente`, invariantes y verificación de domain events.
- `DonacionIndependienteNotificacionesListenerTest`: Validación de eventos de integración con Feign.
- `DonacionesIndependientesServiceTest`: Validación de casos de uso y publicación de eventos.

## Diseño resultante
- Separación clara entre Domain Events (hechos del dominio) e Integration DTOs (payloads de comunicación remota).
- State Pattern enriquecido en el aggregate root, encapsulando invariantes y transiciones.
- Application Service delgado y desacoplado, enfocado puramente en orquestación de repositorios y publicación de eventos.

## IA utilizada
- Análisis de diagramas PlantUML y diagnóstico de responsabilidades en `DonacionesIndependientesService`.
- Diseño e implementación de eventos de dominio, `SolicitudCambioEstadoDonacionIndependiente` y el EventListener desacoplado.
- Generación y adaptación de pruebas unitarias (`DonacionIndependienteEstadosTest`, `DonacionIndependienteNotificacionesListenerTest`, `DonacionesIndependientesServiceTest`).
- Formateo con `mvn spotless:apply` y verificación de compilación multi-módulo (`mvn clean test`).

## Verificación humana
- Validación de que los estados concretos transicionan correctamente y registran sus eventos correspondientes.
- Comprobación de que `DonacionesIndependientesService` no invoca directamente clientes Feign.
- Verificación de ejecución exitosa en todo el reactor (BUILD SUCCESS, 0 fallos, 0 errores).

Oleada 4:
# Oleada 4: Asignación y Propuestas — Unificación de Services, Controllers y `PosibleFragmentacion.confirmar`

## Problema
- `PosibleFragmentacion` era una clase anémica sin métodos de comportamiento; la lógica de decisión sobre si fragmentar o no una donación residía en el Application Service / EventListener.
- `AsignacionService` y `PropuestaDeAsignacionService` duplicaban responsabilidades de orquestación, contenían lógica de consolidación pura en métodos estáticos privados y creaban niveles innecesarios de indirección.
- Los controladores `AsignacionController` y `PropuestasController` estaban fragmentados bajo la misma raíz `/api/asignaciones/*`.

## Evidencia
- En `donaciones-clases.puml` (L532-568), `PosibleFragmentacion` define el método `+ confirmar(necesidad: Necesidad, actor: String): DonacionIndependiente`, y `GestorPropuestasDeAsignacion` encapsula los algoritmos de matching y `consolidar(p1, p2)`.
- El código previo delegaba la fragmentación en `PropuestaDeAsignacionService.onPropuestaAprobada` y el matching en `AsignacionService`.

## Objetivo
1. Trasladar la decisión de negocio de fragmentación a `PosibleFragmentacion.confirmar(necesidad, actor)`.
2. Crear `GestorPropuestasDeAsignacion` como Domain Service puro de matchmaking y consolidación.
3. Unificar `AsignacionService` y `PropuestaDeAsignacionService` en un único Application Service (`PropuestaDeAsignacionService` / `IPropuestaDeAsignacionService`), eliminando `AsignacionService.java`.
4. Unificar `AsignacionController` y `PropuestasController` en `PropuestaDeAsignacionController` / `IPropuestaDeAsignacionController` bajo `@RequestMapping("/api/asignaciones")`.

## Fuera de scope
- Planificación y generación de períodos de `NecesidadRecurrente` (Oleada 5).
- Reorganización de paquetes de infraestructura (Oleada 6).

## Tests
- `PosibleFragmentacionTest`: Validación exhaustiva del método de dominio `confirmar` con y sin fragmentación y guardas.
- `GestorPropuestasDeAsignacionTest`: Pruebas de reglas de consolidación de propuestas (intersección, unión y listas vacías).
- `PropuestaDeAsignacionServiceTest`: Pruebas del servicio de aplicación unificado y consumo de eventos.
- `PropuestaDeAsignacionControllerTest`: Pruebas unitarias de los 4 endpoints REST unificados.

## Diseño resultante
- Dominio rico con `PosibleFragmentacion` y `GestorPropuestasDeAsignacion` como Domain Service.
- Application Service delgado y sin duplicación, enfocado en persistencia y orquestación.
- Controlador web unificado con 100% de compatibilidad en rutas y contratos REST.

## IA utilizada
- Diagnóstico de responsabilidades y diseño de unificación de servicios y controladores.
- Implementación de `PosibleFragmentacion.confirmar` y `GestorPropuestasDeAsignacion`.
- Refactorización y unificación de `PropuestaDeAsignacionService` y `PropuestaDeAsignacionController`.
- Generación de tests unitarios y validación en todo el reactor multi-módulo (`mvn clean test`).

## Verificación humana
- Validación de que `PosibleFragmentacion.confirmar` muta y asigna las entidades adecuadamente.
- Comprobación de que no quedan clases huérfanas de asignaciones previas (`AsignacionService`, `AsignacionController`, etc.).
- Verificación de ejecución exitosa en todo el reactor (BUILD SUCCESS, 0 fallos, 0 errores).# Oleada 4: Asignación y Propuestas — Unificación de Services, Controllers y `PosibleFragmentacion.confirmar`

## Problema
- `PosibleFragmentacion` era una clase anémica sin métodos de comportamiento; la lógica de decisión sobre si fragmentar o no una donación residía en el Application Service / EventListener.
- `AsignacionService` y `PropuestaDeAsignacionService` duplicaban responsabilidades de orquestación, contenían lógica de consolidación pura en métodos estáticos privados y creaban niveles innecesarios de indirección.
- Los controladores `AsignacionController` y `PropuestasController` estaban fragmentados bajo la misma raíz `/api/asignaciones/*`.

## Evidencia
- En `donaciones-clases.puml` (L532-568), `PosibleFragmentacion` define el método `+ confirmar(necesidad: Necesidad, actor: String): DonacionIndependiente`, y `GestorPropuestasDeAsignacion` encapsula los algoritmos de matching y `consolidar(p1, p2)`.
- El código previo delegaba la fragmentación en `PropuestaDeAsignacionService.onPropuestaAprobada` y el matching en `AsignacionService`.

## Objetivo
1. Trasladar la decisión de negocio de fragmentación a `PosibleFragmentacion.confirmar(necesidad, actor)`.
2. Crear `GestorPropuestasDeAsignacion` como Domain Service puro de matchmaking y consolidación.
3. Unificar `AsignacionService` y `PropuestaDeAsignacionService` en un único Application Service (`PropuestaDeAsignacionService` / `IPropuestaDeAsignacionService`), eliminando `AsignacionService.java`.
4. Unificar `AsignacionController` y `PropuestasController` en `PropuestaDeAsignacionController` / `IPropuestaDeAsignacionController` bajo `@RequestMapping("/api/asignaciones")`.

## Fuera de scope
- Planificación y generación de períodos de `NecesidadRecurrente` (Oleada 5).
- Reorganización de paquetes de infraestructura (Oleada 6).

## Tests
- `PosibleFragmentacionTest`: Validación exhaustiva del método de dominio `confirmar` con y sin fragmentación y guardas.
- `GestorPropuestasDeAsignacionTest`: Pruebas de reglas de consolidación de propuestas (intersección, unión y listas vacías).
- `PropuestaDeAsignacionServiceTest`: Pruebas del servicio de aplicación unificado y consumo de eventos.
- `PropuestaDeAsignacionControllerTest`: Pruebas unitarias de los 4 endpoints REST unificados.

## Diseño resultante
- Dominio rico con `PosibleFragmentacion` y `GestorPropuestasDeAsignacion` como Domain Service.
- Application Service delgado y sin duplicación, enfocado en persistencia y orquestación.
- Controlador web unificado con 100% de compatibilidad en rutas y contratos REST.

## IA utilizada
- Diagnóstico de responsabilidades y diseño de unificación de servicios y controladores.
- Implementación de `PosibleFragmentacion.confirmar` y `GestorPropuestasDeAsignacion`.
- Refactorización y unificación de `PropuestaDeAsignacionService` y `PropuestaDeAsignacionController`.
- Generación de tests unitarios y validación en todo el reactor multi-módulo (`mvn clean test`).

## Verificación humana
- Validación de que `PosibleFragmentacion.confirmar` muta y asigna las entidades adecuadamente.
- Comprobación de que no quedan clases huérfanas de asignaciones previas (`AsignacionService`, `AsignacionController`, etc.).
- Verificación de ejecución exitosa en todo el reactor (BUILD SUCCESS, 0 fallos, 0 errores).

Oleada 5:
# PR — Oleada 5: Planificación de Necesidades Recurrentes y Enriquecimiento de Dominio (DDD)

## Problema
Inicialmente existía una mezcla de responsabilidades en la planificación periódica de necesidades:
1. El scheduler (`PlanificadorDeNecesidades`) delegaba en `PlanificacionNecesidadesService`, pero la lógica de renovación de períodos estaba delegada en una clase utilitaria anémica (`GestorNecesidades`).
2. `GestorNecesidades` violaba el principio de encapsulación (*Tell, Don't Ask*) y la Ley de Demeter al consultar el período actual de `NecesidadRecurrente`, mutarlo externamente invocando `.finalizo()` y luego solicitar la creación de un nuevo período.
3. Además, `GestorNecesidades` estaba anotado con `@Component`, acoplando indebidamente la capa de dominio a frameworks de infraestructura (Spring).

## Evidencia
- **Código anterior:** `GestorNecesidades` iteraba la colección de necesidades, preguntaba `necesidad.hayQueGenerarNuevo(fecha)`, obtenía el período con `necesidad.obtenerPeriodoActual().finalizo()` y forzaba `necesidad.generarNuevoPeriodo()`.
- **Divergencia de diseño:** Aunque el plan inicial planteaba conservar `GestorNecesidades` como un mediador entre el servicio de aplicación y el dominio, el análisis de DDD demostró que la gestión de períodos es una invariante exclusiva de la raíz de agregado `NecesidadRecurrente`.

## Objetivo
- **Enriquecer la entidad de dominio (`NecesidadRecurrente`):** Agregar el método de negocio `renovarPeriodoSiCorresponde(LocalDate fechaActual)` que valida si el período venció, finaliza el período anterior y genera el nuevo ciclo en un solo paso cohesivo y atómico.
- **Eliminar `GestorNecesidades`:** Suprimir la clase intermediaria anémica y desacoplar completamente el paquete de dominio de anotaciones de Spring (`@Component`).
- **Adelgazar `PlanificacionNecesidadesService`:** Reducir el servicio de aplicación a su rol puro de orquestador (recuperar necesidades activas, filtrar aquellas que mutaron mediante `renovarPeriodoSiCorresponde` y persistirlas en el repositorio).
- **Mantener el Scheduler delgado:** `PlanificadorDeNecesidades` solo actúa como disparador temporal del caso de uso.

## Fuera de scope
- No se modificaron las reglas de cálculo o asignación de donaciones en `PeriodoNecesidad`.
- No se alteró la estructura de persistencia ni la interfaz `INecesidadesRepository`.
- No se realizaron reorganizaciones de paquetes (reservado para la Oleada 6).

## Tests
La suite de pruebas protege exhaustivamente el nuevo flujo y las reglas de negocio:
- **`NecesidadRecurrenteTests`:**
  - `renovarPeriodoSiCorresponde_cuandoPeriodoAunEstaVigente_deberiaRetornarFalse()`: valida que no se modifique el agregado si no venció.
  - `renovarPeriodoSiCorresponde_cuandoPeriodoVencio_deberiaRetornarTrueYCrearNuevoPeriodo()`: valida la finalización del período vencido, la creación del nuevo período con acumuladores en cero y la preservación del histórico.
  - `renovarPeriodoSiCorresponde_cuandoNoTienePeriodos_deberiaRetornarTrueYCrearPeriodo()`: valida la inicialización si no existían períodos.
- **`PlanificacionNecesidadesServiceTest`:**
  - Valida que el servicio consulte las necesidades recurrentes activas y no satisfechas.
  - Valida que únicamente se invoque `necesidadRepository.save(...)` para aquellas entidades que fueron efectivamente modificadas/renovadas.

## Diseño resultante
- **Arquitectura en capas limpia:**
  $$\text{Scheduler (Trigger)} \longrightarrow \text{Application Service (Orquestación/Persistencia)} \longrightarrow \text{NecesidadRecurrente (Lógica de Dominio)}$$
- **Protección de Invariantes y Encapsulación:** `NecesidadRecurrente` controla su propio ciclo de vida y la consistencia de sus `PeriodoNecesidad`, garantizando un modelo de dominio rico (*Rich Domain Model*).
- **Pureza del Dominio:** La capa de entidades queda constituida únicamente por POJOs puros de Java sin dependencias de infraestructura ni de Spring.

## IA utilizada
- **Análisis de Diseño:** Evaluación comparativa entre `@Component`, métodos estáticos y métodos de instancia/entidad bajo los lineamientos de DDD.
- **Implementación y Refactor:** Creación del método en la entidad, refactorización de `PlanificacionNecesidadesService` y eliminación de clases redundantes.
- **Generación y actualización de Tests:** Actualización de las suites unitarias de aplicación y dominio.

## Verificación humana
- [x] Verificado el cumplimiento de *Tell, Don't Ask* en `NecesidadRecurrente`.
- [x] Verificada la eliminación total de dependencias de Spring en el paquete de entidades de necesidades.
- [x] Ejecución completa de la suite Maven: `mvn test` (**314 tests pasando, 0 fallos, 0 errores**).
- [x] Verificación de formateo de código mediante Spotless.

Oleada 6:
RF-Oleada6: reorganizacion de paquetes - mover logica de dominio fuera de infrastructure
- Mover infrastructure.algoritmos.* -> models.algoritmos.*
  AlgoritmoAsignacion, AlgoritmoCompatibilidadSemantica,
  AlgoritmoPrioridadSubAtendidos, StockDeDonaciones
- Mover infrastructure.analizadores.{Normalizador,NormalizadorBasicoTexto,
  NormalizadorSemantico,ComparadorTexto} -> models.normalizacion.*
- Mover infrastructure.segmentadores.* -> models.segmentacion.*
  AbstractSegmentador, SegmentadorSimple, SegmentadorComplejo
- Actualizar imports en AsignacionService y NormalizadorSemanticoBien
- Actualizar package y imports de todos los tests afectados
- infrastructure/ queda solo con: clients, events, csv, seeder,
  messaging, NormalizadorSemanticoBien

Sin cambios funcionales. Suite: 316 PASS, 0 FAIL.


# PR — Oleada 6: Reorganización de Paquetes — Extracción de Lógica de Dominio fuera de Infrastructure

## Problema
Componentes con lógica pura de negocio y algoritmos de dominio se encontraban alojados dentro del paquete `infrastructure`:
1. **Algoritmos de Asignación y Matching:** Residían en `infrastructure.algoritmos`.
2. **Normalizadores y Comparadores de Texto:** Residían en `infrastructure.analizadores`.
3. **Segmentadores de Bienes:** Residían en `infrastructure.segmentadores`.

Tener estas clases en `infrastructure` violaba la separación de capas de Arquitectura Limpia/Hexagonal y DDD, ya que son POJOs puros con reglas de negocio del dominio que no dependen de frameworks, bases de datos ni clientes externos.

## Evidencia
- En el Diagrama de Clases objetivo (`donaciones-clases.puml`), los algoritmos de asignación, las estrategias de segmentación y los comparadores semánticos forman parte integral del modelo de dominio.
- Las clases `AlgoritmoAsignacion`, `NormalizadorSemantico`, `SegmentadorSimple`, etc., no tenían dependencias de Spring ni de infraestructura externa, siendo candidatos directos para migrar a `models.*`.

## Objetivo
- **Reorganizar paquetes de Dominio:**
  - `infrastructure.algoritmos.*` $\longrightarrow$ `grupo5.donaciones.models.algoritmos.*` (`AlgoritmoAsignacion`, `AlgoritmoCompatibilidadSemantica`, `AlgoritmoPrioridadSubAtendidos`, `StockDeDonaciones`).
  - `infrastructure.analizadores.*` (lógica pura) $\longrightarrow$ `grupo5.donaciones.models.normalizacion.*` (`Normalizador`, `NormalizadorBasicoTexto`, `NormalizadorSemantico`, `ComparadorTexto`).
  - `infrastructure.segmentadores.*` $\longrightarrow$ `grupo5.donaciones.models.segmentacion.*` (`AbstractSegmentador`, `SegmentadorSimple`, `SegmentadorComplejo`).
- **Preservar componentes de infraestructura legítimos:** `NormalizadorSemanticoBien` permanece en `infrastructure.analizadores` por ser un `@Component` de Spring acoplado a repositorios y properties.
- **Sincronización y Resolución de Conflictos:** Merge con `E4_refactor_donaciones` (Oleadas 1 a 5), resolviendo el conflicto por la eliminación de `AsignacionService` (unificado en la Oleada 4) y actualizando los imports en `GestorPropuestasDeAsignacion`.
- **Migración de Tests:** Reubicar todos los tests unitarios bajo sus paquetes espejo en `src/test/java/.../models/*`.

## Fuera de scope
- No se introdujeron cambios funcionales ni de comportamiento en los algoritmos ni normalizadores (*Zero behavioral regression*).
- Limpieza de interfaces legacy huérfanas o métodos muertos (reservado para la Oleada 7).

## Tests
- Se ejecutaron todos los tests unitarios migrados (`StockDeDonacionesTest`, `AlgoritmoCompatibilidadSemanticaTest`, `AlgoritmoPrioridadSubAtendidosTest`, `NormalizadorSemanticoTest`, `SegmentadorSimpleTest`, `SegmentadorComplejoTest`, etc.).
- Se validaron las interacciones con `GestorPropuestasDeAsignacionTest` y `NormalizadorSemanticoBienTest`.
- Suite completa multi-módulo ejecutada exitosamente: **314 tests pasando en `donaciones-service`**, 546+ tests en el reactor general (0 fallos, 0 errores).

## Diseño resultante
- **Capa de Dominio enriquecida y limpia:** Los subpaquetes de `models/` (`algoritmos`, `normalizacion`, `segmentacion`, `entities`, `repositories`, `ports`) concentran toda la lógica pura de negocio.
- **Capa de Infraestructura acotada:** `infrastructure/` queda exclusivamente reservada para adaptadores de entrada/salida: clientes Feign (`clients/`), listeners de eventos (`events/`), seeders (`CatalogDataInitializer`), lectores de archivos (`csv/`) y beans de orquestación técnica (`NormalizadorSemanticoBien`, `ProcesadorDeDonaciones`).

## IA utilizada
- Diagnóstico de dependencias e identificación de clases de dominio dentro de `infrastructure`.
- Resolución de conflictos *delete/modify* contra `E4_refactor_donaciones` (Oleada 4 y 5).
- Reubicación de paquetes, actualización de imports y migración de suites de prueba.
- Verificación cruzada con `mvn spotless:check` y `mvn clean test`.

## Verificación humana
- [x] Verificado que `models.algoritmos`, `models.normalizacion` y `models.segmentacion` son POJOs puros sin anotaciones de Spring.
- [x] Verificada la correcta integración con las clases unificadas de la Oleada 4 (`GestorPropuestasDeAsignacion`) y Oleada 5 (`NecesidadRecurrente`).
- [x] Ejecución del build completo del reactor Maven: `mvn clean test` (**BUILD SUCCESS en los 7 módulos**).
- [x] Verificación de formateo con Spotless: `mvn spotless:check` (**CLEAN**).

---

Oleada 7:
# PR — Oleada 7: Cierre Integral de Refactor — Persistencia Pura, Normalización DDD, Declaratividad y Estandarización de Calidad

## Problema
Al finalizar las oleadas 1 a 6, se identificaron cuatro grupos de deudas técnicas arquitectónicas y de diseño:
1. **Persistencia Antipatrón DTO en Repositorio de Asignaciones**: `IAsignacionesRepository` persistía `EjecucionAsignacionDTO` en lugar de una entidad del modelo de dominio, violando la pureza de la capa de persistencia en DDD.
2. **Normalización Acoplada a Infraestructura**: `NormalizadorSemanticoBien` permanecía en `infrastructure.analizadores` como `@Component` inyectando repositorios y `@Value`, mientras que `ProcesadorDeDonaciones` retenía dependencias huérfanas (`Segmentador`, `donacionesIndependientesRepository`, `incentivosFeignClient`). Asimismo, `ItemDonacionNormalizadoService` utilizaba filtrado imperativo en lugar del comportamiento de dominio `estaPendienteDeRevision()`.
3. **Nombres No Declarativos y Ruptura de Contratos / Colisiones**:
   - `LectorCSVMejorado` utilizaba versionado informal en el nombre y se inyectaba directamente como clase concreta en `ImportadorService` en lugar de utilizar el puerto `CargadorDonantes`.
   - `EstadoDonacion.java` en `donacionesIndependientes` colisionaba con el enum `EstadoDonacion` de `donaciones`, dificultando la declaratividad y comprensión.
   - `IDonacionesIndependientesController` no existía como interfaz, rompiendo la convención arquitectónica del servicio.
   - `IArchivoDonantesService` e `IDonantesController` carecían de los métodos para la carga asincrónica de archivos de donantes.
4. **Residuos Legacy y Tests Desalineados**: Existían carpetas y archivos huérfanos (`routes/`, `.gitkeep` innecesarios), nombres de tests en plural (`*Tests.java`), comentarios residuales (`// refactor ok`), FQCN en inyecciones e imports con comodín (`*`).

## Evidencia
- En `donaciones-clases.puml` (L532-642), se especifica la entidad de dominio `EjecucionAsignacion` (`UUID id`, `LocalDateTime fechaEjecucion`, `Integer cantidadPropuestasGeneradas`), el puerto de dominio `CargadorDonantes` y la interfaz de estado `EstadoDonacionIndependiente`.
- El repositorio `IAsignacionesRepository` operaba con DTOs en lugar de entidades.
- `NormalizadorSemanticoBien` contenía delimitadores `// INICIO LOGICA DE NEGOCIO` y `// FIN LOGICA DE NEGOCIO`, evidenciando que era lógica de dominio atrapada en un adapter de Spring.

## Objetivo
1. **Persistencia Pura (Componente A)**:
   - Crear entidad de dominio `EjecucionAsignacion` en `models.entities.propuestas`.
   - Refactorizar `IAsignacionesRepository` y `AsignacionesRepositoryEnMemoria` para almacenar `EjecucionAsignacion`.
   - Crear `EjecucionAsignacionMapper` y desacoplar `PropuestaDeAsignacionService`.
2. **Normalización DDD (Componente B)**:
   - Migrar `NormalizadorSemanticoBien` a `grupo5.donaciones.models.normalizacion` como Domain Service POJO puro sin anotaciones de Spring ni repositorios inyectados.
   - Limpiar `ProcesadorDeDonaciones` eliminando dependencias huérfanas y delegando en el domain service puro.
   - Aplicar *Tell, Don't Ask* en `ItemDonacionNormalizadoService.obtenerPendientes()` delegando en `ItemDonacionNormalizado::estaPendienteDeRevision`.
3. **Declaratividad y Nombres (Componente C)**:
   - Renombrar `LectorCSVMejorado` $\longrightarrow$ `LectorDonantesCSV` e inyectar el puerto `CargadorDonantes` en `ImportadorService`.
   - Renombrar `EstadoDonacion` $\longrightarrow$ `EstadoDonacionIndependiente` y actualizar todos los estados concretos y referencias.
   - Crear e implementar `IDonacionesIndependientesController`.
   - Completar contratos en `IArchivoDonantesService` e `IDonantesController`.
4. **Limpieza Legacy y Estandarización de Tests (Componente D)**:
   - Eliminar directorio `routes/`, `models/entities/gitkeep` y `.gitkeep` redundantes.
   - Eliminar comentarios `// refactor ok` en `CategoriasService` y `SubcategoriasService`.
   - Reemplazar FQCNs e imports con comodín por imports explícitos.
   - Estandarizar nombres de tests a singular (`AsignableTest`, `EntidadBeneficiariaTest`, `NecesidadExtraordinariaTest`, `NecesidadRecurrenteTest`).
   - Crear suites completas de prueba para `ArchivoDonantesServiceTest` e `ImportadorServiceTest`.

## Fuera de scope
- Modificaciones al contrato de APIs externas o esquemas de persistencia relacional fuera de `donaciones-service`.

## Tests
- **Suites de Dominio Actualizadas/Creadas**:
  - `NormalizadorSemanticoBienTest`: Casos de scoring semántico, umbrales y guardas en `models.normalizacion`.
  - `NormalizadorSemanticoTest`: Alias y normalización canónica en `models.normalizacion`.
  - `AsignableTest`, `EntidadBeneficiariaTest`, `NecesidadExtraordinariaTest`, `NecesidadRecurrenteTest`: Estandarizados y con cobertura de invariantes de negocio.
  - `DonacionIndependienteEstadosTest`: Máquina de estados con `EstadoDonacionIndependiente`.
- **Suites de Servicios y Controladores**:
  - `PropuestaDeAsignacionServiceTest`: Integración con entidad `EjecucionAsignacion` y mapper.
  - `ProcesadorDeDonacionesTest`: Integración desacoplada con el Domain Service.
  - `ArchivoDonantesServiceTest` e `ImportadorServiceTest`: Importación asincrónica, mapeo de donantes y detección de duplicados.
  - `LectorDonantesCSVTest`: Lectura de CSVs válidos, vacíos, desordenados y dataset con BOM UTF-8.
- **Resultado en `donaciones-service`**: **366 tests pasando, 0 fallos, 0 errores**.
- **Resultado en todo el Reactor Multi-Módulo**: **7/7 módulos pasando exitosamente (`BUILD SUCCESS`)**.

## Diseño resultante
- **DDD Estricto y Pureza de Repositorios**: Todos los repositorios operan exclusivamente sobre entidades de dominio y aggregates (`EjecucionAsignacion`, `Donacion`, `Necesidad`, `Persona`, `Donante`, `ItemDonacionNormalizado`, etc.).
- **Desacoplamiento Total de Infraestructura**: `NormalizadorSemanticoBien` es un Domain Service puro y reutilizable sin conocimiento del framework ni de la base de datos.
- **Declaratividad y Consistencia**: Eliminación de ambigüedades de nombrado (`EstadoDonacionIndependiente`, `LectorDonantesCSV`), desacoplamiento mediante puertos (`CargadorDonantes`) y 100% de controladores alineados con sus contratos de interfaz.
- **Código Limpio y Libre de Smells**: Eliminación de imports wildcard, FQCNs en campos y archivos legacy huérfanos.

## IA utilizada
- Diagnóstico exhaustivo de consistencia contra `donaciones-clases.puml` y principios DDD.
- Refactorización guiada de persistencia pura, migración de domain services y resolución de colisiones de nombres.
- Estandarización de nombres y generación de tests unitarios faltantes.
- Formateo con Spotless (`mvn spotless:apply`) y validación de compilación y pruebas en todo el reactor Maven (`mvn clean test`).

## Verificación humana
- [x] Verificado que `IAsignacionesRepository` persiste `EjecucionAsignacion` y no un DTO.
- [x] Verificado que `NormalizadorSemanticoBien` es un POJO de dominio puro sin `@Component`, `@Value` ni repositorios inyectados.
- [x] Verificado que `ImportadorService` inyecta el puerto `CargadorDonantes` y la clase implementadora se llama `LectorDonantesCSV`.
- [x] Verificado que `EstadoDonacionIndependiente` resolvió la colisión con `EstadoDonacion`.
- [x] Verificada la existencia e implementación de `IDonacionesIndependientesController`.
- [x] Ejecución del build completo del reactor Maven: `mvn clean test` (**BUILD SUCCESS en los 7 módulos, 0 fallos, 0 errores**).
- [x] Verificación de formateo con Spotless: `mvn spotless:check` (**CLEAN**).

---

Oleada 8:
# PR — Oleada 8: Refactor Profundo de Testing — Desacoplamiento de Entidades, Object Mothers / Test Builders, Tell Don't Ask y Clasificación Arquitectónica

## Problema
La suite de pruebas de `donaciones-service` presentaba fuertes acoplamientos a la implementación concreta de las entidades de dominio:
1. **Acoplamiento por Constructores y Boilerplate Masivo**: Para instanciar una simple `DonacionIndependiente`, `Necesidad` o `Donacion`, los tests invocaban directamente constructores posicionales con 6 a 10 parámetros, requiriendo 15-20 líneas de setup en cada `@BeforeEach`. Cualquier cambio en la estructura interna de una entidad rompía decenas de pruebas en cascada (*Fragile Tests Antipattern*).
2. **Violación de *Tell, Don't Ask* en Aserciones**: Múltiples tests inspeccionaban getters internos anidados para validar estados de negocio en lugar de interrogar métodos semánticos o eventos de dominio observables.
3. **Falta de Fixtures y DTOs Centralizados**: Duplicación masiva de creación de DTOs y objetos auxiliares a lo largo de las capas de servicios y controladores.
4. **Nombres No Estandarizados**: Existían suites nombradas en plural (`AnonimizacionesTest`, `DonacionesServiceApplicationTests`).

## Evidencia
- En tests como `DonacionIndependienteEstadosTest`, `DonacionIndependienteFragmentacionTest`, `NecesidadExtraordinariaTest`, `NecesidadRecurrenteTest`, `AlgoritmoPrioridadSubAtendidosTest`, `AlgoritmoCompatibilidadSemanticaTest`, `StockDeDonacionesTest`, más del 60% del código correspondía a inicializaciones repetitivas de personas, donantes, categorías, subcategorías, bienes y bienes normalizados.

## Objetivo
1. **Crear Infraestructura de Test Fixtures (*Object Mother & Test Data Builders*)**:
   - `PersonaMother` / `PersonaBuilder` (personas humanas, jurídicas, direcciones válidas, medios de contacto).
   - `CategoriaMother` / `SubcategoriaMother` (categorías canónicas y subcategorías con alias).
   - `BienMother` / `BienNormalizadoMother` (alimentos perecederos, ropa, muebles, con estados y vencimientos).
   - `DonanteMother` (donantes humanos y jurídicos).
   - `DonacionMother` (donaciones simples, con ítems, normalizadas y segmentadas).
   - `DonacionIndependienteMother` (donaciones independientes en cada estado del ciclo de vida: `enDeposito`, `asignada`, `listaParaEntregar`, `enTraslado`, `entregada`, `entregaFallida`, `vencida`).
   - `NecesidadMother` (necesidades extraordinarias y recurrentes con períodos y frecuencias configuradas).
   - `PropuestaMother` (propuestas de asignación y fragmentaciones).
   - `DTOFixtures` (DTOs de entrada y salida para controladores y servicios).
2. **Aplicar *Tell, Don't Ask* y Aserciones Semánticas**:
   - Verificar transiciones a través de comandos semánticos y eventos de dominio (`getDomainEvents()`).
   - Usar métodos semánticos de negocio (`estaSatisfecha()`, `estaPendienteDeRevision()`, `renovarPeriodoSiCorresponde()`).
3. **Refactorizar Capas de Dominio, Algoritmos, Servicios y Controladores**:
   - Reemplazar toda la construcción manual en los tests por las Mothers y Fixtures centralizadas.
   - Expandir la cobertura de controladores REST (`CategoriasControllerTest`, etc.).
4. **Estandarización de Nomenclatura**:
   - Renombrar `AnonimizacionesTest` $\longrightarrow$ `AnonimizacionTest`.
   - Renombrar `DonacionesServiceApplicationTests` $\longrightarrow$ `DonacionesServiceApplicationTest`.

## Fuera de scope
- Alteraciones de contratos REST o lógica de producción.

## Tests
- **Suites Refactorizadas**:
  - `DonacionIndependienteEstadosTest`, `DonacionIndependienteFragmentacionTest`, `DonacionTest`.
  - `NecesidadExtraordinariaTest`, `NecesidadRecurrenteTest`, `AsignableTest`, `EntidadBeneficiariaTest`.
  - `AnonimizacionTest`, `HumanaTest`, `JuridicaTest`, `PersonaTest`.
  - `AlgoritmoCompatibilidadSemanticaTest`, `AlgoritmoPrioridadSubAtendidosTest`, `StockDeDonacionesTest`.
  - `DonacionesServiceTest`, `DonacionesIndependientesServiceTest`, `PropuestaDeAsignacionServiceTest`.
  - `CategoriasControllerTest`, `DonacionesControllerTest`, `DonacionesIndependientesControllerTest`.
  - `DonacionesServiceApplicationTest`.
- **Resultado en `donaciones-service`**: **369 tests pasando (0 fallos, 0 errores, 0 skipped)**.
- **Resultado en todo el Reactor Multi-Módulo**: **7/7 módulos en verde (`BUILD SUCCESS`)**.

## Diseño resultante
- **Desacoplamiento Máximo**: Los tests no conocen los detalles internos ni constructores kilométricos de las entidades; se comunican a través de Mothers declarativas.
- **Legibilidad BDD/DDD (*DAMP over DRY in Scenarios, DRY in Fixtures*)**: Los tests se leen como especificaciones de requerimientos de negocio.
- **Resistencia al Cambio (*Robust Testing Architecture*)**: Si el modelo de dominio evoluciona, solo se adaptan las Mothers correspondientes, manteniendo intactos los cientos de tests del microservicio.

## IA utilizada
- Investigación y diseño del marco de testing desacoplado (Object Mother / Test Builders / Tell Don't Ask).
- Implementación de la infraestructura de fixtures en `src/test/java/grupo5/donaciones/fixtures/`.
- Refactorización exhaustiva de las suites de prueba de dominio, algoritmos, servicios y controladores.
- Formateo con Spotless (`mvn spotless:apply`) y validación en todo el reactor multi-módulo (`mvn clean test`).

## Verificación humana
- [x] Verificada la existencia y uso de las Mothers en `src/test/java/grupo5/donaciones/fixtures/`.
- [x] Verificado el principio *Tell, Don't Ask* en las aserciones de estado y eventos de dominio.
- [x] Verificada la unificación a nombres en singular de todas las suites (`AnonimizacionTest`, `DonacionesServiceApplicationTest`).
- [x] Verificada la suite completa de `donaciones-service`: **369 tests pasando (0 fallos, 0 errores)**.
- [x] Ejecución limpia del reactor Maven: `mvn clean test` (**BUILD SUCCESS en los 7 módulos**).
- [x] Formateo validado: `mvn spotless:check` (**CLEAN**).

---

Oleada 9:
# PR — Oleada 9: Validación por Capas, Respuestas HTTP Clásicas, Manejo Robusto de Excepciones, Trazabilidad Integral (`TraceID`) y Preservación del Desacoplamiento

## Problema
1. **Falta de Validación Declarativa en la Entrada**: Los DTOs de entrada carecían de anotaciones Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@NotEmpty`, `@Valid`), permitiendo que datos inconsistentes o vacíos atravesaran el boundary hacia los servicios de aplicación.
2. **Validaciones Imperativas Manuales en Controladores**: Controladores como `DonantesController` realizaban comprobaciones manuales de strings (`if (input.path() == null) ...`), devolviendo códigos de error sin payload estandarizado.
3. **Omisión de Manejo de Excepciones de Validación en `GlobalExceptionHandler`**: Excepciones nativas de Spring como `MethodArgumentNotValidException`, `MissingRequestHeaderException` o `HttpMessageNotReadableException` no contaban con handlers dedicados, cayendo en errores 500 genéricos o respuestas sin formato homogéneo.
4. **Pérdida de Trazabilidad en Respuestas y Clientes Distribuidos**: `ErrorResponse` no incluía el identificador de correlación `traceId`, y las respuestas HTTP no devolvían el header `X-Trace-Id`. Las llamadas Feign salientes no propagaban automáticamente el contexto de trazas a los microservicios downstream.
5. **Riesgo de Acoplamiento en Tests de Frontera**: Los tests de controladores requerían una fábrica centralizada de DTOs (`DTOFixtures`) para evitar constructores directos repetitivos y frágiles en los escenarios de validación.

## Evidencia
- Requests con campos faltantes provocaban excepciones en cascada en la capa de servicios o dominio, o devolvían HTTP 500 no informativo.
- Los logs del servidor contenían `traceId` en MDC pero los clientes HTTP no tenían forma de correlacionar un error 400/404/500 con los logs.
- Varias suites de controladores instanciaban DTOs manualmente con constructores complejos en lugar de usar fixtures centralizados.

## Objetivo
1. **Validación por Capas con Segregación Estricta de Responsabilidades**:
   - **Capa Web (Boundary / DTOs / Controllers)**: Validación sintáctica y estructural declarativa mediante Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@Positive`, `@NotEmpty`, `@Valid`) y anotación `@Valid` en todos los `@RequestBody` de los controladores.
   - **Capa de Aplicación (Services)**: Asume que los DTOs ya fueron filtrados sintácticamente; se enfoca en validación de existencia (`RecursoNoEncontradoException` $\rightarrow$ 404), duplicados y orquestación.
   - **Capa de Dominio (Entities & Aggregates)**: Protege invariantes intrínsecas de estado y transiciones del State Pattern (`ValidationException` $\rightarrow$ 400, `BusinessStateException` $\rightarrow$ 409).
2. **Respuestas HTTP Clásicas y Limpias**:
   - Uso consistente de `201 CREATED` para `POST` de creación, `202 ACCEPTED` para procesos asincrónicos en segundo plano (`POST /api/donantes/archivos`), `200 OK` para consultas y actualizaciones, y `204 NO CONTENT` para eliminaciones.
   - Eliminación total de chequeos imperativos manuales en controllers.
3. **Manejo Global de Errores y Errores Estructurados por Campo**:
   - `FieldErrorDTO(String field, String message, Object rejectedValue)` para detallar fallos de validación específicos por campo.
   - Handlers en `GlobalExceptionHandler` para `MethodArgumentNotValidException`, `ConstraintViolationException`, `MissingRequestHeaderException`, `MissingServletRequestParameterException`, `HttpMessageNotReadableException`, `FeignException`.
4. **Trazabilidad Integral (`TraceID`)**:
   - `ErrorResponse` enriquecido con `traceId` resuelto automáticamente desde `MDC` o `Tracer`.
   - `TraceResponseHeaderFilter` inyectando `X-Trace-Id` en el response header de cada petición HTTP.
   - `FeignTraceRequestInterceptor` propagando `X-Trace-Id` a llamadas HTTP downstream entre microservicios.
5. **Preservación y Fortalecimiento del Desacoplamiento de la Oleada 8**:
   - Expansión de `DTOFixtures` para cubrir todos los DTOs de entrada del microservicio.
   - Uso sistemático de `DTOFixtures` en los 10 controladores REST con `MockMvcBuilders.standaloneSetup`, configurando `GlobalExceptionHandler` y `TraceResponseHeaderFilter`.
   - Aislamiento estricto de frontera (*Boundary Isolation*) y preservación de *Tell, Don't Ask*.

## Fuera de scope
- Modificación de contratos de negocio de dominio o reglas de scoring de algoritmos.

## Tests
- **Nuevas Suites en `common-lib`**:
  - `GlobalExceptionHandlerTest`: Verificación de todos los mapeos de status codes (400, 404, 409, 500, 502/Feign), extracción de `fieldErrors` y presencia de `traceId`.
  - `TraceResponseHeaderFilterTest`: Verificación de inyección de header `X-Trace-Id` y sincronización con MDC.
  - `ErrorResponseTest`: Verificación de serialización y resolución de `traceId`.
- **Nuevas y Refactorizadas Suites en `donaciones-service` (10/10 Controladores)**:
  - `CategoriasControllerTest`: Casos 200, 201 y 400 (`errors[0].field == 'nombre'`).
  - `SubcategoriasControllerTest`: Casos 200, 201 y 400 (`errors[0].field == 'nombre'`).
  - `DonacionesControllerTest`: Casos 200, 201 y 400 (`errors[0].field == 'items'`).
  - `DonacionesIndependientesControllerTest`: Casos 200, 400 (header faltante / estado nulo), 404 y 409.
  - `DonantesControllerTest`: Casos 200, 201, 202, 204 y 400 (`idPersona` / `path`).
  - `EntidadBeneficiariaControllerTest`: Casos 200, 201 y 400 (`juridicaId`).
  - `ItemDonacionNormalizadoControllerTest`: Casos 200 y 400 (`estadoNormalizacion`).
  - `NecesidadesControllerTest`: Casos 200, 201 y 400 (`cantidadNecesitada`).
  - `PersonasControllerTest`: Casos 200, 201, 204 y 400 (`nombre`).
  - `PropuestaDeAsignacionControllerTest`: Casos 200, 201 y 400 (`estado`).
- **Resultados**:
  - `common-lib`: **24 tests pasando (0 fallos, 0 errores)**.
  - `donaciones-service`: **385 tests pasando (0 fallos, 0 errores)** (+16 tests respecto a Oleada 8).
  - **Reactor Multi-Módulo completo**: **7/7 módulos pasando exitosamente (`BUILD SUCCESS`)**.

## Diseño resultante
- **Arquitectura de Validaciones Pura**: Separación clara entre validación de frontera (Bean Validation), validación de casos de uso (Application Services) y protección de invariantes (Domain Entities).
- **Consistencia de API REST y Problem Details**: Respuestas de error auto-descriptivas, con códigos de catálogo, detalles a nivel de campo y correlación vía `traceId`.
- **Trazabilidad Distribuida de Extremo a Extremo**: Propagación bidireccional de `X-Trace-Id` en HTTP y Feign.
- **Ecosistema de Testing Desacoplado**: Todas las pruebas consumen Mothers y `DTOFixtures`, garantizando nula fragilidad (*Low Test Fragility*) ante evoluciones del modelo.

## IA utilizada
- Diagnóstico de validaciones y diseño de mejoras para errores estructurados a nivel de campo y trazabilidad Feign.
- Implementación de `TraceResponseHeaderFilter`, `FeignTraceRequestInterceptor`, `FieldErrorDTO`, `ErrorResponse` y `GlobalExceptionHandler`.
- Enriquecimiento de todos los DTOs y Controladores con `@Valid` y códigos de estado clásicos.
- Expansión de `DTOFixtures` y refactorización desacoplada de los 10 controladores REST.
- Validación completa, pruebas unitarias y de integración con Spotless y reactor Maven.

## Verificación humana
- [x] Verificado el filtro `TraceResponseHeaderFilter` y la presencia del header `X-Trace-Id` en las respuestas HTTP.
- [x] Verificado `FeignTraceRequestInterceptor` para propagación de trazas distribuidas.
- [x] Verificada la inclusión del array `errors` con `FieldErrorDTO` en `ErrorResponse` ante `MethodArgumentNotValidException`.
- [x] Verificada la eliminación de comprobaciones imperativas en controllers (`DonantesController`).
- [x] Verificados los códigos HTTP clásicos: `201 Created`, `202 Accepted`, `200 OK`, `204 No Content`, `400 Bad Request`, `404 Not Found`, `409 Conflict`.
- [x] Verificada la expansión completa de `DTOFixtures.java` para todos los DTOs de entrada.
- [x] Verificado el desacoplamiento en los 10 tests de controladores usando `DTOFixtures` y standalone MockMvc.
- [x] Verificada la suite completa de `donaciones-service`: **385 tests pasando (0 fallos, 0 errores)**.
- [x] Verificada la suite completa de `common-lib`: **24 tests pasando (0 fallos, 0 errores)**.
- [x] Ejecución del build completo del reactor Maven: `mvn clean test` (**BUILD SUCCESS en los 7 módulos**).
- [x] Formateo Spotless validado: `mvn spotless:check` (**CLEAN**).

---

Oleada 10:
# PR — Oleada 10: Análisis y Preparación Integral para Persistencia Real (JPA/Hibernate, PostgreSQL y MinIO)

## Problema
De cara a la futura migración desde repositorios en memoria hacia persistencia relacional con PostgreSQL, ORM con JPA/Hibernate 6 y almacenamiento de objetos S3 con MinIO, una auditoría 360° sobre `donaciones-service` identificó 7 deudas técnicas estructurales y de diseño que impedirían un mapeo limpio si no se corrigen previamente en el dominio y aplicación:
1. **Violaciones de Límites de Agregados e Interfaz `Asignable` Artificial**: `DonacionIndependiente` retenía `@JsonIgnore private Asignable asignadaA;` (acoplamiento por objeto en memoria); `NecesidadExtraordinaria` y `PeriodoNecesidad` almacenaban colecciones directas `List<DonacionIndependiente>` con referencias circulares; y `PosibleFragmentacion` retenía una instancia mutable `donacionOriginal`.
2. **Acoplamiento al Sistema de Archivos Local**: `Archivo.path` almacenaba rutas absolutas rígidas de disco y `LectorDonantesCSV` leía directamente con `FileReader`, siendo incompatible con contenedores Docker y almacenamiento S3 en la nube.
3. **Ghost Objects y Polución de Constructores**: `Persona.java` instanciaba forzadamente `new Telefono()` vacío con campos en `null` por defecto, violando restricciones `NOT NULL` de base de datos. Asimismo, los constructores de entidades mezclaban la creación de negocio (que emite eventos de dominio) con la rehidratación técnica de persistencia.
4. **Polución de Contratos por Privacidad (`Anonimizable`)**: Las entidades implementaban `Anonimizable` con mutaciones destructivas imperativas (`nombre = "ANONIMIZADO"`), ensuciando el modelo y dificultando la auditoría sin resolver el Derecho de Supresión (Art. 16 Ley 25.326).
5. **Ausencia de Control de Concurrencia**: Inexistencia de campos de versión en agregados transaccionales de inventario (`DonacionIndependiente`, `Necesidad`, `Propuesta`), exponiendo al sistema a sobreescrituras ciegas (*Lost Updates*).
6. **Riesgo de Doble Escritura (*Dual-Write*)**: La publicación síncrona de eventos en memoria no estaba alineada con los límites transaccionales de base de datos ni con el envío asíncrono a RabbitMQ.

## Evidencia
- En `donaciones-clases.puml`, la interfaz `Asignable` no existe; el diseño objetivo modela referencias explícitas por `UUID` (`necesidadId`, `entidadBeneficiariaId`).
- En `Persona.java:L27,L34`, `this.mediosDeContacto.add(new Telefono())` creaba registros nulos artificiales.
- En `NecesidadExtraordinaria.java:L14` y `PeriodoNecesidad.java:L13`, la lista `List<DonacionIndependiente>` provocaba riesgo de N+1 y memory ballooning.
- En `ADR 20260519-privacidad-de-usuarios.md`, se preveía la necesidad de revisar la interfaz `Anonimizable` para desacoplar el modelo y garantizar integridad referencial histórica.

## Objetivo
1. **Desacoplar Agregados por Identificadores Explícitos (DDD)**:
   - Eliminar la interfaz `Asignable` y el campo `asignadaA` en `DonacionIndependiente`, reemplazándolos por `UUID necesidadId` y `UUID entidadBeneficiariaId`.
   - Eliminar `List<DonacionIndependiente>` dentro de `NecesidadExtraordinaria` y `PeriodoNecesidad`, gestionando `cantidadAcumulada` escalar y consultas delegadas al repositorio.
   - Hacer a `PosibleFragmentacion` puramente stateless respecto a la donación original, recibiéndola por parámetro en `confirmar(donacionOriginal, necesidad, actor)`.
2. **Patrón Strategy de Almacenamiento y Lectores (FileSystem vs MinIO)**:
   - Diseñar el puerto de dominio `CargadorDonantes` con soporte dual: `LectorDonantesFileSystemStrategy` (para desarrollo local, fixtures y tests unitarios) y `LectorDonantesMinioStrategy` (para ingesta streaming desde buckets MinIO S3).
   - Conceptualizar `Archivo.path` como `objectKey` e introducir el puerto `StoragePort`.
3. **Limpieza de Entidades y Reconstitución Limpia**:
   - Eliminar el *Ghost Object* `new Telefono()` en `Persona`.
   - Separar métodos de Creación de Negocio (`crear(...)` con eventos de dominio) de constructores de Reconstitución Técnica (hidratación sin eventos falsos).
   - Diseñar `EstadoDonacionIndependienteFactory` para mapear las 7 clases del State Pattern bidireccionalmente contra `TipoEstadoDonacion` (columna SQL).
4. **Alternativas de Privacidad y Crypto-Shredding**:
   - Eliminar la interfaz técnica `Anonimizable` de `Persona`, `Donante`, `EntidadBeneficiaria` y `MedioDeContacto`.
   - Modelar el ciclo de vida mediante `EstadoPersona` (`ACTIVA`, `ANONIMIZADA`) y el método de negocio `darDeBajaPorPrivacidad(actor, motivo)` que emite `PersonaAnonimizada`.
   - Diseñar el puerto `CryptoPort` para permitir a futuro la destrucción de la clave DEK (*Crypto-Shredding*), volviendo los datos cifrados en PostgreSQL matemáticamente irrecuperables sin romper FKs ni auditoría histórica.
5. **Control de Concurrencia y Consistencia Eventual**:
   - Analizar y preparar campos de `version: Long` para Optimistic Concurrency Control (OCC) en base de datos y simulación CAS en repositorios en memoria.
   - Diseñar el patrón Transactional Outbox (`outbox_events`) para eliminar el problema de la doble escritura entre PostgreSQL y RabbitMQ.
6. **Marco de Verificación de No-Regresión para Oleadas 8 y 9**:
   - Blindar el 100% de los Object Mothers, fixtures, aserciones *Tell, Don't Ask*, Bean Validation declarativa, códigos HTTP clásicos (201, 202, 200, 204, 400, 404, 409), respuestas estructuradas `FieldErrorDTO` y trazabilidad `traceId` en HTTP y Feign.

## Fuera de scope
- **Cero anotaciones JPA prematuras en producción**: No se introducen `@Entity`, `@Table`, `@Column` ni `@Embeddable` en esta oleada.
- **Cero dependencias de base de datos física**: No se agregan drivers de PostgreSQL ni SDK de MinIO en los `pom.xml`.
- **Persistencia física diferida**: Todo el detalle de implementación de esquemas relacionales DDL, JPA Converters, Testcontainers, scripts de migración y configuración de buckets se encuentra formalmente documentado en el archivo complementario:
  👉 [`decisiones_futuras_en_oleada_10.md`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/docs/design/donaciones-service/decisiones_futuras_en_oleada_10.md).

## Tests / Plan de Validación y No-Regresión
- **Protección de la Oleada 8**:
  - `PersonaMother`, `DonacionMother`, `DonacionIndependienteMother`, `NecesidadMother`, `PropuestaMother` adaptadas a métodos de creación limpios y referencias por UUID sin romper tests preexistentes.
  - Aserciones semánticas verificando invariantes y eventos de dominio (`getDomainEvents()`).
  - Nomenclatura singular en todas las suites de prueba (`LectorDonantesStrategyTest`, etc.).
- **Protección de la Oleada 9**:
  - Validación declarativa Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Positive`, `@Valid`) en todos los DTOs de entrada.
  - Verificación de status codes clásicos (`201 Created`, `202 Accepted` en archivos, `200 OK`, `204 No Content`, `400`, `404`, `409 Conflict`).
  - Manejo de `FieldErrorDTO` y propagación de `traceId` en MDC, headers `X-Trace-Id` y clientes Feign.
- **Línea Base de Validación**:
  - `donaciones-service`: **385 tests pasando (0 fallos, 0 errores)**.
  - `common-lib`: **24 tests pasando (0 fallos, 0 errores)**.
  - Reactor Multi-Módulo: **7/7 módulos pasando exitosamente (`BUILD SUCCESS`)**.

## Diseño resultante
- **Dominio Puro y Preparado para Persistencia**: Agregados desacoplados con límites estrictos, referencias por `UUID`, colecciones protegidas y ausencia de ghost objects.
- **Almacenamiento Desacoplado**: Strategy dual de carga de datos que convive con el sistema de archivos local y habilita la conexión transparente con MinIO S3.
- **Arquitectura de Privacidad Escalable**: Supresión legal mediante Crypto-shredding y eventos de dominio sin ensuciar las entidades de negocio.
- **Concurrencia y Transacciones Robustas**: Bloqueo optimista preparado y diseño de Transactional Outbox para consistencia eventual inter-microservicios.

## IA utilizada
- Auditoría integral 360° de los 10 agregados del microservicio de donaciones.
- Diagnóstico de acoplamientos contra `donaciones-clases.puml` y diseño de eliminación de `Asignable`.
- Diseño del patrón Strategy para almacenamiento dual (FileSystem vs MinIO) y análisis de concurrencia CAS vs OCC.
- Elaboración del documento de arquitectura complementario [`decisiones_futuras_en_oleada_10.md`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/docs/design/donaciones-service/decisiones_futuras_en_oleada_10.md).

## Verificación humana
- [x] Verificada la eliminación conceptual de `Asignable` y el desacoplamiento por `UUID` en `DonacionIndependiente` y `Necesidad`.
- [x] Verificado el patrón Strategy para `CargadorDonantes` con preservación del lector sobre FileSystem local.
- [x] Verificado el análisis de concurrencia (`AtomicLong`/CAS en memoria vs `@Version Long` en PostgreSQL).
- [x] Verificada la estrategia de Crypto-Shredding y eliminación de la interfaz `Anonimizable`.
- [x] Verificada la no-regresión de todas las capacidades de las Oleadas 8 y 9.
- [x] Verificada la existencia y completitud de [`decisiones_futuras_en_oleada_10.md`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/docs/design/donaciones-service/decisiones_futuras_en_oleada_10.md).
- [x] Ejecución limpia del reactor Maven: `mvn clean test` (**BUILD SUCCESS en los 7 módulos**).
- [x] Formateo Spotless validado: `mvn spotless:check` (**CLEAN**).

---

Oleada 11:
# PR — Oleada 11: Cierre de Observaciones de Code Review, Limpieza Mecánica y Estabilización de Contratos de Entrada

## Problema
Al integrar las oleadas 7 a 9 y tras los reviews cruzados de las PRs #770, #771 y #772, quedaron pendientes diversas observaciones puntuales, limpiezas mecánicas y definiciones de contratos:
1. **Inconsistencias y Observaciones de Code Review (#770 y #772 / Etapa 0)**:
   - `ProcesadorDeDonaciones.java:88`: El log informativo de scoring semántico duplicaba el parámetro `confianza()` (`(confianza: {}, {})`), ensuciando la salida de trazabilidad técnica.
   - `NormalizadorSemanticoBien.java:50`: El método auxiliar `normalizarBien(...)` había quedado con visibilidad pública/paquete en lugar de `private`, exponiendo indebidamente la mecánica interna de resolución.
   - `NormalizadorSemanticoBienTest.java`: Carecía de pruebas unitarias defensivas para los guards ante inputs nulos (`donacion == null` e `item == null`).
   - `GlobalExceptionHandler.java:194`: En `handleFeignException`, se concatenaba directamente `ex.getMessage()` dentro del mensaje público del `ErrorResponse`, provocando una potencial fuga de información sensible, headers internos o URLs del servicio remoto hacia el cliente HTTP externo.
   - `GlobalExceptionHandlerTest.java`: No verificaba la no-fuga de mensajes en errores Feign y carecía de pruebas unitarias para `handleHandlerMethodValidation` y `handleConstraintViolation`.
2. **Residuos y Limpieza Mecánica (#770 y #771 / Etapa 1)**:
   - Presencia de wildcard imports (`import ...*`) en clases reales en tests de infraestructura y mappers: `SegmentacionEventListenerTest.java:22`, `MedioDeContactoMapperTest.java:5`, `PersonaMapperTest.java:8`.
   - En `DonacionesServiceTest.java:76`: Se utilizaba el string hardcodeado `"desc"` en una aserción de DTO en lugar de validar dinámicamente contra `inputDTO.descripcion()`.
3. **Decisiones de Negocio y Endurecimiento de Frontera (#772 / Etapa 2)**:
   - **Flexibilidad en `DireccionInputDTO`**: Al endurecer las validaciones con Bean Validation en la Oleada 9, se evaluó si los campos de altura (`numero`) y `codigoPostal` debían ser estrictamente `@NotNull`/`@NotBlank`. En la operatoria real de donaciones y comedores comunitarios existen domicilios sin numeración catastral (`S/N`) y localidades sin código postal delimitado. Forzar validaciones estrictas bloqueaba casos de uso legítimos.
   - **Fail-Fast en `ItemDonacionNormalizadoPatchDTO.estadoNormalizacion`**: Se analizó si existía un caso de uso válido de PATCH parcial que omitiera el estado de normalización (solo actualizando `subcategoriaId`). El flujo del dominio (`ItemDonacionNormalizadoService.actualizarEstandar()` y `BienNormalizado`) siempre requirió el estado (`ERR-VAL-315`), por lo que marcarlo `@NotNull` constituye un fail-fast temprano que rechaza peticiones mal formadas en el boundary HTTP sin alterar la semántica de negocio.
   - **Mapeo 1:1 de `FeignException` a Status Público**: Se evaluó si normalizar todos los errores de Feign a un status genérico (502 Bad Gateway) o mantener el mapeo 1:1 del status remoto (404, 400). Se determinó preservar el mapeo 1:1 para permitir que los clientes distingan si un recurso downstream no existe (404) o si el servicio remoto está caído, documentándolo explícitamente en el handler.
   - **Handlers `@Validated` a nivel Controller**: `handleHandlerMethodValidation` y `handleConstraintViolation` no poseían invocaciones activas al no usarse `@Validated` en la raíz de los controladores, dejándolos documentados como soporte especulativo preventivo.

## Evidencia
- `ProcesadorDeDonaciones.java:88`: `log.info("... (confianza: {}, {})", item.getId(), subcategoria.getId(), confianza, confianza);`.
- `GlobalExceptionHandler.java:194`: `"Error de comunicación con servicio remoto: " + ex.getMessage()`.
- `SegmentacionEventListenerTest.java:22`, `MedioDeContactoMapperTest.java:5`, `PersonaMapperTest.java:8`: `import grupo5.donaciones.models.repositories.*;`, `import grupo5.donaciones.dto.mediosDeContacto.*;`, `import grupo5.donaciones.models.entities.personas.*;`.
- `DonacionesServiceTest.java:76`: `"desc"` hardcodeado.

## Objetivo
1. **Aplicar Fixes de Code Review**:
   - Corregir el log de `confianza` en `ProcesadorDeDonaciones`.
   - Restablecer la visibilidad `private` en `NormalizadorSemanticoBien.normalizarBien`.
   - Eliminar la concatenación de `ex.getMessage()` en `handleFeignException` de `GlobalExceptionHandler`.
   - Agregar tests de sanitización y guards faltantes en `NormalizadorSemanticoBienTest` y `GlobalExceptionHandlerTest`.
2. **Ejecutar Limpieza Mecánica**:
   - Reemplazar todos los wildcard imports por imports canónicos explícitos en los tests señalados.
   - Parametrizar la aserción de descripción en `DonacionesServiceTest` usando `inputDTO.descripcion()`.
3. **Consolidar y Documentar Decisiones de Frontera y Negocio**:
   - Mantener altura y código postal opcionales en `DireccionInputDTO` para preservar la máxima flexibilidad operativa.
   - Mantener `@NotNull` en `ItemDonacionNormalizadoPatchDTO.estadoNormalizacion` como fail-fast de boundary.
   - Documentar explícitamente en el código la decisión de mapeo 1:1 de `FeignException` y el rol preventivo de los handlers `@Validated`.

## Fuera de scope
- Corrección de bugs de concurrencia y reentrancia de domain events (#761 - Oleada 12).
- Protección de invariantes de necesidad en la máquina de estados de asignación (#762 - Oleada 12).
- Desacoplamiento de Domain Services de Spring (Oleada 12).

## Tests
- **`NormalizadorSemanticoBienTest`**:
  - `normalizar_conDonacionNula_noLanzaExcepcion()`: verifica la guarda defensiva ante `donacion == null`.
  - `normalizar_conItemNulo_noLanzaExcepcion()`: verifica la guarda ante ítems nulos en la lista.
- **`GlobalExceptionHandlerTest`**:
  - `handleFeignException_noDebeFugarDetallesInternos()`: valida que la respuesta HTTP no contenga el stacktrace ni el mensaje crudo de Feign.
  - `handleHandlerMethodValidation_retornaBadRequest()` y `handleConstraintViolation_retornaBadRequest()`: validan el mapeo estructurado a 400.
- **Suites Actualizadas**:
  - `SegmentacionEventListenerTest`, `MedioDeContactoMapperTest`, `PersonaMapperTest` y `DonacionesServiceTest`.
- **Resultados**:
  - `common-lib`: **27 tests pasando (0 fallos, 0 errores)** (+3 tests).
  - `donaciones-service`: **387 tests pasando (0 fallos, 0 errores)** (+2 tests).

## Diseño resultante
- **Boundary HTTP Sanitizado y Seguro**: Respuestas de error homogéneas que protegen la topología interna de la infraestructura de microservicios sin filtrar detalles de clientes Feign.
- **Contratos DTO Alineados a la Realidad Operativa**: Validaciones declarativas que previenen datos inválidos sin restringir escenarios legítimos de direcciones incompletas (`S/N`).
- **Código Limpio y Mantenible**: Supresión total de wildcard imports y cadenas duplicadas en suites de prueba.

## IA utilizada
- Detección de duplicaciones en logs, revisión de modificadores de visibilidad y sanitización de payloads de excepción.
- Generación de tests de frontera y sanitización de seguridad en `GlobalExceptionHandlerTest`.
- Refactorización mecánica de imports y parametrización de fixtures en tests.

## Verificación humana
- [x] Verificado el fix del log en `ProcesadorDeDonaciones.java:88`.
- [x] Verificado el modificador `private` en `NormalizadorSemanticoBien.normalizarBien`.
- [x] Verificada la eliminación de fuga de información en `handleFeignException` (`GlobalExceptionHandler.java:194`).
- [x] Verificada la eliminación de wildcard imports en `SegmentacionEventListenerTest`, `MedioDeContactoMapperTest` y `PersonaMapperTest`.
- [x] Verificada la parametrización de `inputDTO.descripcion()` en `DonacionesServiceTest.java:76`.
- [x] Validadas las justificaciones de negocio para `DireccionInputDTO` y `ItemDonacionNormalizadoPatchDTO`.
- [x] Build multi-módulo limpio: `mvn clean test` (**BUILD SUCCESS**).

---

Oleada 12:
# PR — Oleada 12: Resolución de Deuda Técnica Crítica de Dominio/Concurrencia (#761, #762) y Desacoplamiento Estructural de Spring en Domain Services

## Problema
Durante sucesivas oleadas se venían postergando tres deudas técnicas críticas de dominio y arquitectura:
1. **Reentrancia y `ConcurrentModificationException` al Iterar `domainEvents` (#761 / Etapa 3)**:
   - En `Donacion`, `DonacionIndependiente` y `Propuesta`, el método `getDomainEvents()` retornaba `Collections.unmodifiableList(this.domainEvents)`. Esto devolvía una *vista no modificable* vinculada en vivo a la lista interna mutable del agregado.
   - Como los repositorios en memoria (`CrudRepositoryEnMemoria.findById()`) devolvían la misma referencia en memoria del objeto guardado (no una copia defensiva), se producía un fallo de reentrancia real en cascada:
     $$\text{Application Service} \xrightarrow{\text{mutación}} \text{donacion.marcarNormalizada()} \xrightarrow{\text{iteración}} \text{getDomainEvents().forEach(publishEvent)}$$
     El bucle comenzaba a iterar sobre la vista viva. Al dispararse síncronamente `SegmentacionEventListener.onDonacionNormalizada`, el listener recuperaba la **misma instancia** de `Donacion` e invocaba `marcarSegmentada()` y `clearDomainEvents()`. Esto mutaba la lista interna mientras el `forEach` exterior continuaba iterando, provocando una `ConcurrentModificationException` en tiempo de ejecución.
2. **Inconsistencia e Invariante Rota en `ASIGNACION_REALIZADA` (#762 / Etapa 3)**:
   - En el patrón State de `DonacionIndependiente`, el estado `EnDeposito.asignar(DonacionIndependiente, SolicitudCambioEstadoDonacionIndependiente)` contenía un condicional permisivo: solo asignaba el receptor si `solicitud.getNecesidad() != null`. Si la solicitud se creaba con `necesidad == null`, la ejecución continuaba y completaba la transición al estado `AsignacionRealizada`, dejando `asignadaA = null` y el atributo `necesidadId` del evento `EventoDonacionAsignada` en `null`.
   - El Aggregate Root quedaba en un estado corrupto (formalmente "asignado" pero sin necesidad ni beneficiario que satisfaga). Peor aún, los tests unitarios preexistentes ejercitaban este bug como comportamiento esperado.
3. **Acoplamiento Incompleto de Domain Services a Spring (Etapa 4)**:
   - Aunque la Oleada 7 había extraído `NormalizadorSemanticoBien` como POJO puro sin dependencias de Spring, sus clases hermanas `NormalizadorBasicoTexto.java`, `ComparadorTexto.java` y `GestorPropuestasDeAsignacion.java` continuaban anotadas con `@Component`, `@Autowired` y `@Qualifier("normalizadorBasicoTexto")`.
   - Esto contradecía la regla de pureza de dominio de DDD y Arquitectura Hexagonal, donde los servicios de dominio dentro de `models/` deben ser POJOs puros agnósticos al contenedor de inyección de dependencias.

## Evidencia
- `Donacion.java`, `DonacionIndependiente.java`, `Propuesta.java`: `return Collections.unmodifiableList(this.domainEvents);`.
- `EnDeposito.java:24-30`: `if (solicitud != null && solicitud.getNecesidad() != null) { d.asignarReceptor(solicitud.getNecesidad()); } asignar(d, ...);`.
- `NormalizadorBasicoTexto.java:6`: `@Component("normalizadorBasicoTexto")`.
- `ComparadorTexto.java:9-16`: `@Component`, `@Autowired`, `@Qualifier(...)`.
- `GestorPropuestasDeAsignacion.java:18-23`: `@Component`, `@Autowired`.

## Objetivo
1. **Inmunizar `domainEvents` ante Concurrencia y Reentrancia (#761)**:
   - Refactorizar `getDomainEvents()` en `Donacion`, `DonacionIndependiente` y `Propuesta` para retornar una copia defensiva inmutable e independiente: `List.copyOf(this.domainEvents)`.
   - Garantizar que cualquier mutación o limpieza de eventos ocurrida durante la ejecución de listeners síncronos no afecte el snapshot tomado al inicio de la publicación.
2. **Blindar la Invariante de Asignación (#762)**:
   - Agregar una guarda obligatoria en `EnDeposito.asignar(...)`: `if (solicitud == null || solicitud.getNecesidad() == null) throw new ValidationException(ErrorCatalog.DONACION_INDEPENDIENTE_ASIGNACION_SIN_NECESIDAD);`.
   - Crear el código de error `DONACION_INDEPENDIENTE_ASIGNACION_SIN_NECESIDAD` (`ERR-VAL-411`) en `ErrorCatalog.java`.
   - Corregir los tests de estados para proporcionar una `Necesidad` válida provista por `NecesidadMother`.
3. **Desacoplar Totalmente los Domain Services de Spring**:
   - Remover `@Component`, `@Autowired` y `@Qualifier` de `NormalizadorBasicoTexto`, `ComparadorTexto` y `GestorPropuestasDeAsignacion`, convirtiéndolos en POJOs puros.
   - Crear la clase de infraestructura `DomainServicesConfig.java` (`@Configuration`) en `grupo5.donaciones.config`, instanciando y componiendo explícitamente el bean de Spring `GestorPropuestasDeAsignacion` (`new GestorPropuestasDeAsignacion(new ComparadorTexto(new NormalizadorBasicoTexto()))`).
   - Mantener `PropuestaDeAsignacionService` intacto recibiendo el bean por constructor.

## Fuera de scope
- Persistencia física JPA en PostgreSQL ni esquemas DDL relacionales (Oleada 10/13).
- Modificaciones en la firma pública de `IPropuestaDeAsignacionService`.

## Tests
- **`DonacionTest`**:
  - `getDomainEvents_debeSerUnaCopiaInmuneAMutacionesPosteriores()`: valida que tomar un snapshot de eventos y luego mutar la entidad (`marcarSegmentada()`) no altere el tamaño ni la iteración de la copia tomada.
- **`DonacionIndependienteEstadosTest`**:
  - `cambiarEstado_aAsignacionRealizadaSinNecesidad_lanzaExcepcion()`: valida el rechazo inmediato con `ERR-VAL-411` y la permanencia en estado `EnDeposito`.
  - `getDomainEvents_debeSerUnaCopiaInmuneAMutacionesPosteriores()`: valida la inmutabilidad del snapshot de eventos ante mutaciones concurrentes (`planificarRuta()`, `iniciarRecorrido()`).
  - Adaptación de la suite completa usando `NecesidadMother` en transiciones de asignación.
- **Resultados**:
  - `donaciones-service`: **389 tests pasando (0 fallos, 0 errores)** (+2 tests de snapshots y +1 test de guard).
  - `common-lib`: **27 tests pasando (0 fallos, 0 errores)** (+1 entrada de catálogo `ERR-VAL-411`).
  - Reactor Multi-Módulo completo: **7/7 módulos pasando exitosamente (`BUILD SUCCESS`)**.

## Diseño resultante
- **Resiliencia de Eventos de Dominio**: El patrón `List.copyOf()` garantiza aislamiento total entre la emisión de eventos y los suscriptores síncronos de infraestructura.
- **Integridad de Agregados**: `DonacionIndependiente` protege sus invariantes de asignación, imposibilitando estados huérfanos o eventos con identidades nulas.
- **Dominio Desacoplado (DDD Puro)**: Todos los componentes de `models/` son POJOs puros de Java. El framework Spring queda confinado exclusivamente a la capa de configuración técnica (`DomainServicesConfig`).

## IA utilizada
- Diagnóstico del ciclo de reentrancia entre el bus de eventos y el listener de segmentación.
- Detección de tests viciados que validaban la ausencia de necesidad en asignaciones.
- Implementación de copias defensivas, catálogo de errores y configuración de ensamblado de Domain Services.

## Verificación humana
- [x] Verificado el reemplazo de `Collections.unmodifiableList` por `List.copyOf` en `Donacion`, `DonacionIndependiente` y `Propuesta`.
- [x] Verificada la guarda de asignación con `ERR-VAL-411` en `EnDeposito.java` y `ErrorCatalog.java`.
- [x] Verificada la eliminación total de `@Component`/`@Autowired` en `NormalizadorBasicoTexto`, `ComparadorTexto` y `GestorPropuestasDeAsignacion`.
- [x] Verificada la clase de configuración `DomainServicesConfig.java`.
- [x] Verificada la ejecución limpia de `mvn clean test` (**389 tests en donaciones-service, 27 en common-lib**).

---

Oleada 13:
# PR — Oleada 13: Gobernanza de Calidad, Trazabilidad de Bitácora y Preparación Arquitectónica Pre-Persistencia JPA

## Problema
Al auditar el progreso global del refactor y de cara a la implementación de persistencia real relacional (JPA/Hibernate y PostgreSQL), se evidenciaron dos problemáticas estructurales:
1. **Desalineación entre Bitácora de Refactor y Diff Real de Git (Etapa 5)**:
   - En las revisiones de las Oleadas 7 a 10 se identificó una desconexión recurrente: la bitácora declaraba ítems como "refactorizados" o marcados con checkmark `✅` sin que existiera código ni tests reales en el árbol de Git.
   - En Oleada 10, los ítems `Desacoplamiento Asignable -> UUIDs`, `Strategy FileSystem/MinIO` y `Crypto-shredding modelado` estaban marcados con `✅` en la tabla de auditoría final del plan de refactor, cuando en realidad eran únicamente especificaciones conceptuales documentadas en `decisiones_futuras_en_oleada_10.md`.
   - Esto generaba falsos positivos para revisores futuros y agentes de IA, quienes asumían erróneamente que esas capacidades ya estaban implementadas en código.
2. **Gaps Críticos Pre-Persistencia Real JPA (Etapa 6)**:
   - El documento de persistencia futura (`decisiones_futuras_en_oleada_10.md`) cubría el esquema DDL y el mapeo de entidades, pero carecía de especificaciones clave en tres áreas fundamentales:
     - *Capa de Repositorios y Queries*: Los repositorios en memoria (`NecesidadesRepositoryEnMemoria`, etc.) resolvían consultas mediante `findAll()` y filtrado en streams de Java invocando métodos de negocio de los agregados (`estaSatisfecha()`). Esto no se traduce directamente a cláusulas `WHERE` de SQL en Spring Data JPA sin diseñar queries derivadas o specifications.
     - *Transacciones Multi-Agregado*: Métodos como `PosibleFragmentacion.confirmar` mutan simultáneamente `DonacionIndependiente` y `Necesidad`, requiriendo delimitar formalmente las fronteras de `@Transactional` en los Application Services para garantizar atomicidad ACID.
     - *Mapeo de Relaciones, Lazy Loading y Cascadas*: Ausencia de directrices para colecciones de Value Objects (`PeriodoNecesidad`, `ItemDonacion`) y prevención de excepciones *LazyInitializationException* o problemas de rendimiento *N+1*.

## Evidencia
- `plan-implementacion-refactor-donatrack-donaciones.md:888-918`: Ítems conceptuales de Oleada 10 con `✅` en la auditoría final sin diff de código.
- `decisiones_futuras_en_oleada_10.md`: Ausencia de secciones dedicadas a repositorios de queries SQL vs filtrado Java, transacciones multi-agregado y políticas de cascada JPA.

## Objetivo
1. **Establecer Reglas Estrictas de Gobernanza y Trazabilidad (Etapa 5)**:
   - Incorporar la subsección `## Trazabilidad de la bitácora` en la sección 16 del plan de refactor (`plan-implementacion-refactor-donatrack-donaciones.md`), estableciendo que todo ítem marcado como ✅ o declarado "refactorizado" debe citar obligatoriamente el `archivo:línea` real que lo prueba.
   - Formalizar la convención de símbolos en la documentación:
     - `✅` = Código implementado con diff de Git y tests pasando.
     - `📝` / `🔵` = Análisis o diseño conceptual sin código todavía.
   - Corregir retroactivamente la tabla de Auditoría Final (sección 20) pasando los 3 ítems de Oleada 10 de `✅` a `📝`.
   - Reforzar el prompt del Agente del Reviewer (sección 4) instruyéndolo a ejecutar y confrontar el `git diff` real contra las afirmaciones de la bitácora antes de aprobar.
2. **Formalizar la Mitigación de Gaps Pre-JPA (Etapa 6)**:
   - Documentar formalmente las estrategias requeridas para los 3 gaps antes de iniciar la persistencia física:
     1. Transformación de queries Java a consultas SQL / Spring Data JPA optimizadas (`WHERE` indexados).
     2. Delimitación de transacciones multi-agregado mediante `@Transactional` en Application Services.
     3. Configuración de `FetchType.LAZY` y políticas de cascade para colecciones y entidades dependientes.

## Fuera de scope
- Implementación de esquemas físicos relacionales en PostgreSQL o agregado de dependencias de drivers/Flyway en los `pom.xml`.
- Modificación de lógica de producción en los microservicios.

## Tests
- Auditoría documental completa y verificación cruzada de trazabilidad entre bitácora y árbol de Git.
- Suite completa del reactor Maven ejecutada exitosamente: **389 tests en `donaciones-service`**, 27 tests en `common-lib`, 167 en `logistica-service`, **0 fallos, 0 errores (`BUILD SUCCESS` en los 7 módulos)**.

## Diseño resultante
- **Gobernanza Rigurosa y Verificable**: Proceso de desarrollo con trazabilidad 1:1 entre bitácoras y diffs reales de Git, eliminando ambigüedades sobre el estado real de la arquitectura.
- **Arquitectura Preparada para Persistencia Relacional**: Hoja de ruta técnica consolidada y documentada para una transición limpia y predecible hacia Spring Data JPA y PostgreSQL.

## IA utilizada
- Auditoría cruzada de consistencia entre commits de Git y bitácoras de refactor.
- Actualización de políticas de gobernanza, templates de PR y prompts de revisión en `plan-implementacion-refactor-donatrack-donaciones.md`.
- Formulación de directrices de mitigación para los gaps de persistencia relacional.

## Verificación humana
- [x] Verificada la subsección `Trazabilidad de la bitácora` en `plan-implementacion-refactor-donatrack-donaciones.md`.
- [x] Verificada la corrección de símbolos (`📝`) en la tabla de auditoría final.
- [x] Verificada la instrucción de comprobación de `git diff` en el prompt del Reviewer.
- [x] Verificada la especificación técnica para la resolución de queries JPA, transacciones multi-agregado y lazy loading.
- [x] Ejecución del build completo del reactor Maven: `mvn clean test` (**BUILD SUCCESS en los 7 módulos, 0 fallos, 0 errores**).
- [x] Verificación de formateo con Spotless: `mvn spotless:check` (**CLEAN**).

---

Generalización del refactor en plan genérico exhaustivo:

# Generalización: Plan Genérico de Refactor por Oleadas v3

Tras completar las 13 oleadas del refactor de `donaciones-service`, se generalizaron las lecciones aprendidas en un plan genérico exhaustivo aplicable a los demás microservicios (`logistica-service`, `incentivos-service`, `notificaciones-service`).

## Documento de referencia

👉 [`plan-generico-refactor-servicios.md`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/docs/design/plan-generico-refactor-servicios.md)

## Principio de exhaustividad

El plan v3 incorpora un principio fundamental aprendido de las oleadas 11–13: **cada oleada debe aplicarse a TODOS los elementos del microservicio que cumplan la condición**, no solo a un ejemplo representativo. Esto se implementa mediante:

- **Fase 0 con inventario completo**: Tabla de tracking de TODAS las entidades, agregados, services, controllers, DTOs, schedulers e interfaces del servicio.
- **Checklists de completitud** en cada oleada que exigen cobertura al 100% antes de cerrar la PR.
- **No-regresión acumulativa**: Cada oleada verifica que TODAS las oleadas anteriores siguen funcionando.

## 5 lecciones retroalimentadas desde oleadas 11–13 hacia oleadas tempranas

Estas correcciones, que en `donaciones-service` se descubrieron tardíamente y requirieron oleadas adicionales de hardening, ahora se incorporan **desde el día 1** en el plan genérico:

1. **`List.copyOf()` desde oleada 2** (no `Collections.unmodifiableList`): Previene `ConcurrentModificationException` cuando EventListeners síncronos mutan la entidad durante la iteración de eventos.
   - *Descubierto en*: Oleada 12, Eje A (reentrancia en `Donacion`, `DonacionIndependiente`, `Propuesta`).

2. **Guardas estrictas en State Pattern desde oleada 3**: Cada estado concreto debe rechazar inmediatamente transiciones con datos incompletos (`null`). No usar condicionales permisivos (`if (x != null)`).
   - *Descubierto en*: Oleada 12, Eje B (invariante rota en `EnDeposito.asignar` permitía asignación sin necesidad).

3. **Domain Services sin `@Component` desde oleada 4**: Crear POJOs puros y ensamblarlos en `DomainServicesConfig.java` (`@Configuration`).
   - *Descubierto en*: Oleada 12, Eje C (`NormalizadorBasicoTexto`, `ComparadorTexto`, `GestorPropuestasDeAsignacion` mantenían `@Component`/`@Autowired`).

4. **Convención ✅/📝 desde Fase 0**: Todo ítem marcado ✅ debe tener diff de Git real. Usar 📝 para análisis/diseño sin código.
   - *Descubierto en*: Oleada 13, Eje A (ítems de Oleada 10 como `Desacoplamiento Asignable → UUIDs` marcados ✅ sin código implementado).

5. **No exponer `ex.getMessage()` de Feign desde oleada 9**: Los mensajes crudos de `FeignException` contienen headers internos, URLs y stack traces. Usar mensaje genérico sanitizado.
   - *Descubierto en*: Oleada 11, Etapa 0 (fuga de información sensible en `GlobalExceptionHandler.handleFeignException`).