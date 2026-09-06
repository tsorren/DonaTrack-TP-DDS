# Bitácora de Refactor por Oleadas — `incentivos-service`

> Registro de auditoría, transformaciones arquitectónicas y gobernanza del refactor de `incentivos-service`.
> Cada oleada debe cumplir estrictamente la regla de gobernanza (Problema, Evidencia, Objetivo, Fuera de scope, Tests / Verificación, Diseño resultante, IA utilizada).

---

## Oleada 0: Auditoría exhaustiva, Baseline verde e Inventario completo

### Problema
El microservicio `incentivos-service` implementa el subsistema de gamificación, ascensos, métricas y tableros de líderes de DonaTrack. Aunque el modelo cuenta con una base orientada a objetos más rica que la de servicios anémicos iniciales, presentaba inconsistencias arquitectónicas, riesgos de concurrencia y violaciones a los principios de diseño de DonaTrack:
1. **Acoplamiento y violación masiva de SRP en Application Services**: `IncentivosService` agrupaba 5 responsabilidades divergentes (gestión de perfil, procesamiento transaccional de donaciones/misiones, gestión de visibilidad de insignias, analítica/dashboard y detección de inactividad).
2. **Defectos de Tell Don't Ask**: Métricas del sistema y rachas eran interrogadas directamente mediante bucles y casteos (`instanceof`) en la capa de servicios en lugar de delegar en el Aggregate Root.
3. **Riesgo crítico de reentrancia en Domain Events**: `DonanteIncentivos.getDomainEvents()` exponía `Collections.unmodifiableList()`, vulnerable a `ConcurrentModificationException` si un listener o consumidor muta la entidad durante la iteración de eventos.
4. **Violación de pureza de dominio y excepciones crudas**: Constructores de `Mision`, `RankingMensual` e `InactividadDonaciones` lanzaban `IllegalArgumentException` en vez de excepciones del catálogo (`ValidationException`).
5. **Ambigüedad en `Insignia`**: Se utilizaba un único record tanto para la plantilla inmutable de recompensa de una misión como para la insignia ganada por el donante (con estado de visualización mutable).
6. **Domain Services no gestionables e infraestructura huérfana de contratos**: `GestorDeRankings` y `GestorDeInactivos` eran clases con métodos estáticos sin configuración Spring (`DomainServicesConfig`), y clientes como `NotificacionesClient` y `N8nClient` no exponían interfaces.
7. **Ausencia de Object Mothers y validaciones declarativas**: Los tests instanciaban entidades directamente por constructor posicional, y los DTOs de entrada carecían de anotaciones `@NotNull`/`@NotBlank`.

### Evidencia
- **`IncentivosService.java`**: 220 líneas concentrando todas las operaciones del servicio.
- **`DonanteIncentivos.java` L.111**: `return Collections.unmodifiableList(this.domainEvents);`.
- **`Mision.java` L.30-33**: `throw new IllegalArgumentException("...");`.
- **`RankingMensual.java` L.18, L.27**: `throw new IllegalArgumentException("...");`.
- **`InactividadDonaciones.java` L.20**: `throw new IllegalArgumentException("...");`.
- **`EntradaRanking.java` L.23**: Método `setPosicion()` permitía mutación externa pos-construcción.
- **`NotificacionesClient.java` / `N8nClient.java`**: Clases `@Component` sin interfaces desacopladas.
- **`target/modelo_tecnico.puml`**: Evidencia de acoplamiento y falta de contratos de infraestructura.

### Objetivo
1. **Verificar Baseline Verde**: Comprobar que el servicio compile limpiamente y que la totalidad de los tests unitarios e integrales existentes (56 tests) pasen en verde.
2. **Elaborar Inventario Exhaustivo**: Catalogar el 100% de las clases, entidades, agregados, servicios, DTOs y controladores, mapeando cada problema a su oleada de resolución (1 a 13).
3. **Diseñar el Diagrama de Clases de Referencia**: Crear `docs/design/incentivos-service/diagrama-de-clases-incentivos.puml` incorporando el diseño enriquecido final (separación `Insignia`/`InsigniaGanada`, métodos Tell-Don't-Ask, descomposición SRP de servicios, interfaces de adaptadores).
4. **Inicializar la Bitácora de Gobernanza**: Establecer este documento como bitácora viva del servicio.
5. **Alinear Convenciones de Control**: Establecer la nomenclatura de avance (`✅` para código con diff de git y tests pasando, `📝` para decisiones o análisis conceptuales).

### Fuera de scope
- No se introducen modificaciones en la lógica productiva de dominio ni refactors de código en esta fase (reservados para las Oleadas 1 a 13 según el roadmap).
- No se altera la estructura de endpoints REST ni payloads HTTP existentes.

### Tests / Verificación
- `mvn clean test -f incentivos-service/pom.xml`: ✅ 56 tests ejecutados, 0 fallos, 0 errores, 0 omitidos (`BUILD SUCCESS`).
- `mvn spotless:check -f incentivos-service/pom.xml`: ✅ 59 archivos limpios sin desalineaciones de formato.
- Cobertura de tests inicial registrada en Jacoco (`target/site/jacoco/index.html`).

### Diseño resultante
Se consolidó la arquitectura objetivo del microservicio documentada en:
1. [`docs/design/incentivos-service/plan-refactor-incentivos.md`](./plan-refactor-incentivos.md) (Roadmap de 13 oleadas con consolidaciones justificadas).
2. [`docs/design/incentivos-service/diagrama-de-clases-incentivos.puml`](./diagrama-de-clases-incentivos.puml) (Diagrama de Clases PlantUML alineado con `donatrack-style.puml`).

#### Catálogo de Componentes Auditados:
- **Aggregate Roots**: `DonanteIncentivos` (gamificación reactiva con eventos de dominio), `RankingMensual` (agregado de cálculo periódico/proyección sin eventos de dominio).
- **Entidades y Objetos de Valor**: `Mision` (abstracta), `MisionRacha`, `MisionCompletitud`, `MisionDonacionesExitosas`, `MisionHabilDonador`, `Insignia` (plantilla), `InsigniaGanada` (poseída), `Metricas`, `CambioCategoria`, `EventoDonacion`, `EntradaRanking`, `DonanteInactivo`.
- **Domain Services Puros**: `GestorDeRankings`, `GestorDeInactivos`, `CriterioInactividad`, `InactividadDonaciones`, `MisionFactory`.
- **Application Services (Descomposición SRP)**: `GestionDonanteService`, `MisionesDonacionService`, `InsigniasService`, `MetricasIncentivosService`, `InactividadService`, `RankingService`.
- **Adaptadores e Infraestructura**: `INotificacionesClient` (`NotificacionesClient`), `IN8nClient` (`N8nClient`), `NotificacionesFeignClient`, `NotificacionesIncentivosListener`.
- **Schedulers**: `InactividadJob`, `RachaJob`, `RankingMensualJob`.

### IA utilizada
- Auditoría estática y trazabilidad de código fuente de `incentivos-service`.
- Comparación contra el plan genérico de refactor de DonaTrack y las lecciones aprendidas en `donaciones-service`.
- Generación de modelos PlantUML conformes a la guía de estilos de arquitectura.
- Formalización del plan de ejecución y estructura de bitácora.

---

## Oleada 1: Tell, Don't Ask en Agregados y Misiones

### Problema
Existían múltiples violaciones al principio Tell, Don't Ask donde los servicios de aplicación interrogaban el estado interno de las entidades para tomar decisiones de negocio, acoplando la lógica y favoreciendo el diseño anémico:
1. **Introspección y casteo de misiones en el servicio**: `IncentivosService.verificarRachasVencidas()` iteraba sobre todas las misiones del donante, filtraba con `instanceof MisionRacha` y ejecutaba el casteo manual en vez de solicitarle al agregado `DonanteIncentivos` que verifique sus propias rachas.
2. **Consultas anémicas sobre misiones y métricas**: `IncentivosService.obtenerMetricas()` calculaba `misionesCompletadas` recorriendo la lista de misiones con un stream externo, y `obtenerResumenSistema()` consultaba `d.getMetricas().donacionesEnMes(...) > 0` directamente sobre el objeto embebido de métricas.
3. **Mutación externa en `EntradaRanking`**: `GestorDeRankings` creaba `EntradaRanking` con posición dummy `0` y luego mutaba la posición mediante `entrada.setPosicion(posicion.getAndIncrement())`, requiriendo un setter público mutable.
4. **Duplicación de lógica de completitud en `MisionDonacionesExitosas`**: La lógica de marcar `completada=true`, registrar la fecha de completitud y otorgar la insignia estaba duplicada en `MisionDonacionesExitosas.evaluarProgresoExitoso()` respecto a `Mision.evaluarProgreso()`.
5. **Inconsistencia de visibilidad en `Insignia`**: Se analizó la ambigüedad conceptual donde `Insignia` como plantilla de misión no debe mezclarse con la insignia ganada por el usuario. Se aprobó formalmente la **Opción A** (separación en `Insignia` plantilla e `InsigniaGanada` poseída) para implementar en la Oleada 3.

### Evidencia
- `IncentivosService.java`:
  - L.206-213: bucle con `instanceof MisionRacha` y cast `(MisionRacha) m`.
  - L.132-133: `(int) donante.getMisiones().stream().filter(Mision::isCompletada).count();`.
  - L.173-176: `d.getMetricas().donacionesEnMes(mesActual) > 0`.
- `GestorDeRankings.java` L.18-28: `new EntradaRanking(0, ...)` seguido de `entrada.setPosicion(...)`.
- `EntradaRanking.java`: `@Setter` público en toda la clase con atributos mutables.
- `MisionDonacionesExitosas.java` L.26-42: bloque repetido de `setCompletada(true)`, `setFechaCompletada(...)` y `donante.otorgarInsignia(...)`.

### Objetivo
1. **Enriquecer `DonanteIncentivos`**:
   - Agregar `public void verificarRachas(YearMonth mesActual)`.
   - Agregar `public int misionesCompletadas()`.
   - Agregar `public boolean tuvoActividadEnMes(YearMonth mes)`.
2. **Adelgazar `IncentivosService`**:
   - Reemplazar el bucle de introspección por `todos.forEach(d -> d.verificarRachas(mesActual))`.
   - Delegar el conteo de misiones en `donante.misionesCompletadas()`.
   - Delegar la verificación de actividad mensual en `d.tuvoActividadEnMes(...)`.
3. **Inmutabilidad en `EntradaRanking`**:
   - Eliminar `@Setter` y el método `setPosicion()`, estableciendo todos los campos como `final`.
   - Calcular la posición previamente en `GestorDeRankings` e instanciar `EntradaRanking` con su posición definitiva.
4. **Unificar ciclo de completitud en `Mision`**:
   - Extraer `protected void completar(DonanteIncentivos donante, LocalDate fecha)` en `Mision`.
   - Reutilizar `completar(donante, fecha)` en `MisionDonacionesExitosas.evaluarProgresoExitoso()`.
5. **Añadir tests unitarios dedicados**:
   - Crear `DonanteIncentivosTest` para validar exhaustivamente los nuevos métodos de comportamiento del Aggregate Root.

### Fuera de scope
- Descomposición SRP de `IncentivosService` en 5 Application Services especializados (Oleada 4).
- Eliminación de `IllegalArgumentException` y migración a `ValidationException(ErrorCatalog)` (Oleada 3).
- Implementación de `InsigniaGanada` (Oleada 3).
- Corrección de `Collections.unmodifiableList` a `List.copyOf` en domain events (Oleada 2).

### Tests / Verificación
- **Tests unitarios nuevos**:
  - `DonanteIncentivosTest`:
    - `verificarRachas_deberiaResetearProgresoSiRachaVencida`: ✅ Valida reseteo de racha ante mes salteado.
    - `verificarRachas_noDeberiaAfectarMisionVigente`: ✅ Valida preservación de racha en mes consecutivo.
    - `verificarRachas_noDeberiaModificarMisionYaCompletada`: ✅ Valida inmutabilidad de misiones completadas.
    - `misionesCompletadas_deberiaContabilizarSoloMisionesCompletadas`: ✅ Valida conteo exacto de misiones logradas.
    - `tuvoActividadEnMes_deberiaIndicarSiHuboDonacionesEnElPeriodo`: ✅ Valida detección de actividad mensual.
    - `otorgarInsignia_y_configurarVisibilidad_deberianFuncionarCorrectamente`: ✅ Valida visibilidad de insignias.
- **Suite completa**:
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **62 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - `mvn spotless:check -f incentivos-service/pom.xml`: ✅ 60 archivos limpios sin desalineaciones de formato.

### Diseño resultante
- El Aggregate Root `DonanteIncentivos` recupera su rol como guardián de las reglas de negocio de gamificación, coordinando sus misiones y evaluando su actividad sin permitir anomia en la capa de servicios.
- `IncentivosService` se convierte en un orquestador más delgado que despacha acciones al modelo de dominio sin interrogar estructuras internas ni realizar filtrados por tipo (`instanceof`).
- `EntradaRanking` se consolida como una entidad inmutable cuyos valores quedan fijados al momento del cómputo del ranking.
- La jerarquía de `Mision` encapsula la mutación de estado y el otorgamiento de insignias en su método protegido `completar()`.

### IA utilizada
- Detección estática de violaciones a Tell Don't Ask en los Application Services.
- Refactorización de algoritmos de ordenamiento y cómputo de posiciones inmutables en `GestorDeRankings`.
- Generación de tests unitarios focalizados en el comportamiento de dominio.
- Verificación cruzada con la suite de regresión y formateo Spotless.

---

## Oleada 2: Domain Events y Seguridad de Concurrencia

### Problema
`DonanteIncentivos` implementa el patrón Domain Events para registrar transiciones clave (`AscensoDonante`, `MisionCompletada`). No obstante:
1. **Riesgo crítico de reentrancia en `getDomainEvents()`**: Exponía `Collections.unmodifiableList(this.domainEvents)`. Este método no crea una copia independiente de la colección sino un wrapper de solo lectura sobre la lista mutable subyacente. Si un listener síncrono o un flujo de orquestación invoca `clearDomainEvents()` o genera nuevos eventos mientras se itera la lista, se lanza `ConcurrentModificationException`.
2. **Ausencia de test canónico de reentrancia**: La suite carecía de un test de regresión que verificara la inmutabilidad y el aislamiento del snapshot devuelto frente a mutaciones posteriores en el Aggregate Root.
3. **Formalización sobre `RankingMensual`**: Se requería formalizar la decisión arquitectónica de que `RankingMensual` no requiere infraestructura de eventos de dominio.

### Evidencia
- `DonanteIncentivos.java` L.113: `return Collections.unmodifiableList(this.domainEvents);`.
- Ausencia de tests de reentrancia e inmutabilidad de snapshot en `DonanteIncentivosTest`.

### Objetivo
1. **Defensa ante Concurrencia y Reentrancia**: Modificar `DonanteIncentivos.getDomainEvents()` para retornar `List.copyOf(this.domainEvents)`.
2. **Test Canónico de Reentrancia**: Incorporar a `DonanteIncentivosTest` un test canónico que confirme que el snapshot de eventos permanece intacto tras invocar `clearDomainEvents()`.
3. **Decisión de Diseño Documentada (📝)**: Registrar formalmente que `RankingMensual` no requiere Domain Events por ser un agregado de cálculo/proyección batch.

### Fuera de scope
- Erradicación de `IllegalArgumentException` y adopción de `ValidationException(ErrorCatalog)` (Oleada 3).
- Descomposición SRP de `IncentivosService` (Oleada 4).

### Tests / Verificación
- **Test canónico agregado**:
  - `DonanteIncentivosTest.getDomainEvents_debeRetornarCopiaInmutableEInmuneAMutacionesPosteriores`: ✅ Confirma snapshot intacto tras `clearDomainEvents()` y rechazo a mutaciones con `UnsupportedOperationException`.
- **Suite completa**:
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **63 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - `mvn spotless:check -f incentivos-service/pom.xml`: ✅ 60 archivos limpios.

### Diseño resultante
- `DonanteIncentivos` garantiza aislamiento total de sus eventos de dominio: cada llamada a `getDomainEvents()` entrega un snapshot inmutable e independiente (`List.copyOf`). El Application Service puede iterar, publicar o procesar los eventos de forma segura mientras el ciclo de vida del agregado continúa de forma desacoplada.
- `RankingMensual` queda formalizado como Aggregate Root proyectado sin eventos de dominio internos; su persistencia y notificación externa son gestionadas directamente por el servicio orquestador (`RankingService`).

### IA utilizada
- Identificación de vulnerabilidades de concurrencia y vistas mutables en Domain Events.
- Implementación de pruebas canónicas de reentrancia e inmutabilidad de snapshots.
- Verificación automatizada con Maven y Spotless.

---

## Oleada 3: Parameter Objects, Guardas Estrictas, Eliminación de `@Setter` y Separación `Insignia`/`InsigniaGanada`

### Problema
1. **Violación de Pureza de Dominio con Excepciones Crudas**: Diversas clases de dominio (`Mision`, `RankingMensual`, `InactividadDonaciones`) lanzaban `IllegalArgumentException` directa ante argumentos inválidos o nulos, violando el principio arquitectónico de 0 excepciones crudas y rompiendo la coherencia con el catálogo unificado de errores (`ErrorCatalog`).
2. **Mutabilidad Externa y Violación de Encapsulamiento**: La jerarquía completa de `Mision` (`Mision`, `MisionRacha`, `MisionCompletitud`, `MisionDonacionesExitosas`, `MisionHabilDonador`) y la entidad embebida `Metricas` exponían anotaciones `@Setter` públicas, permitiendo mutaciones de estado descontroladas desde clases externas sin pasar por los métodos de comportamiento.
3. **Ambigüedad Conceptual en `Insignia`**: Se utilizaba un único record mutable/inconsistente tanto para la plantilla de recompensa de una misión (`Mision.insignia`) como para la insignia en posesión del donante (`DonanteIncentivos.insignias`), generando riesgo de que la configuración de visibilidad del usuario fuera sobreescrita por la plantilla.
4. **Value Objects sin Guardas de Invariantes**: `EventoDonacion` permitía la construcción de instancias sin fecha o con cantidades no positivas.

### Evidencia
- `Mision.java`: L.30, L.33 lanzaban `IllegalArgumentException` y poseía `@Setter` a nivel de clase.
- `RankingMensual.java`: L.18 y L.27 lanzaban `IllegalArgumentException`.
- `InactividadDonaciones.java`: L.20 lanzaba `IllegalArgumentException`.
- `Metricas.java`: anotada con `@Setter` público.
- `Insignia.java`: un solo record mezclaba plantilla y estado poseído con visibilidad mutable.
- `InactividadDonacionesTest.java`: verificaba `IllegalArgumentException.class`.

### Objetivo
1. **Incorporar Códigos de Error al `ErrorCatalog` (`common-lib`)**:
   - `MISION_NOMBRE_INVALIDO("ERR-VAL-711")`
   - `MISION_OBJETIVO_INVALIDO("ERR-VAL-712")`
   - `RANKING_PERIODO_NULO("ERR-VAL-713")`
   - `RANKING_ENTRADA_NULA("ERR-VAL-714")`
   - `INACTIVIDAD_DIAS_INVALIDOS("ERR-VAL-715")`
2. **Erradicar `IllegalArgumentException` del Dominio**:
   - Reemplazar por `throw new ValidationException(ErrorCatalog.X)` en `Mision`, `RankingMensual` e `InactividadDonaciones`.
3. **Eliminar `@Setter` en `Metricas` y Jerarquía de `Mision`**:
   - Encapsular mutaciones de progreso y estado en métodos protegidos (`completar()`, `setProgresoActual()`) y semánticos (`setNumeroMision()`, `setInsignia()`).
   - Confinar las actualizaciones de `Metricas` a métodos de comportamiento (`registrarDonacion`, `registrarDonacionExitosa`, `registrarOrganizacionAyudada`).
4. **Separación de Responsabilidades `Insignia` (plantilla) e `InsigniaGanada` (poseída)**:
   - Crear `InsigniaGanada` con métodos inmutables de evolución de visibilidad (`ocultada()`, `mostrada()`, `conVisibilidad(boolean)`).
   - Reducir `Insignia` a plantilla inmutable (`nombre`, `descripcion`, `imagenUrl`).
   - Migrar `DonanteIncentivos.insignias` a `List<InsigniaGanada>`.
   - Sobrecargar `InsigniaDTO.desde()` para soportar ambas representaciones limpiamente.
5. **Guardas Estrictas en `EventoDonacion`**:
   - Validar `fecha` obligatoria y `cantidadBienes` positiva con copia defensiva inmutable de categorías (`List.copyOf`).

### Fuera de scope
- Descomposición SRP de `IncentivosService` en 5 Application Services especializados (Oleada 4).
- Creación de `DomainServicesConfig` y desestatización de `GestorDeInactivos`/`GestorDeRankings` (Oleada 4).
- Construcción de Object Mothers centralizados (Oleada 8).
- Validaciones declarativas en DTOs y `@Valid` en controllers (Oleada 9).

### Tests / Verificación
- **Tests actualizados y nuevos**:
  - `InactividadDonacionesTest`: actualizado para validar `ValidationException` con `ErrorCatalog.INACTIVIDAD_DIAS_INVALIDOS`.
  - `MisionesTest`: agregados tests de guardas para nombre nulo/vacío, objetivo inválido, categoría nula e insignia nula.
  - `RankingMensualTest`: agregados tests de guardas para periodo nulo y entrada nula.
  - `DonanteIncentivosTest`: agregados tests para IDs nulos, insignia nula e insignia inexistente.
  - `InsigniaTest`: nueva suite unitaria para inmutabilidad y validaciones de `Insignia` e `InsigniaGanada`.
  - `EventoDonacionTest`: nueva suite unitaria para constructor y builder con guardas de invariantes.
- **Barridos mecánicos**:
  - `grep -rnE "throw new Illegal(Argument|State)Exception" src/main/java/**/models/`: ✅ **0 matches**.
  - `grep -rn "@Setter" src/main/java/**/models/`: ✅ **0 matches**.
- **Suite completa**:
  - `mvn clean test -f common-lib/pom.xml`: ✅ **27 tests ejecutados, 0 fallos, 0 errores** (`BUILD SUCCESS`).
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **82 tests ejecutados, 0 fallos, 0 errores** (`BUILD SUCCESS`).
  - `mvn spotless:check`: ✅ **100% de archivos limpios** en ambos módulos.

### Diseño resultante
- El modelo de dominio de `incentivos-service` alcanza pureza total frente a excepciones crudas: todas las violaciones se comunican a través de `ValidationException` o `BusinessStateException` mapeadas al catálogo general.
- Se elimina la exposición de setters mutables: las entidades preservan invariantes internas y solo mutan su estado a través de comportamiento semántico y controlado.
- La distinción formal entre `Insignia` (plantilla inmutable de misión) e `InsigniaGanada` (recompensa en posesión del donante) resuelve la inconsistencia latente de visibilidad, garantizando que las preferencias del usuario permanezcan inalteradas independientemente de re-procesamientos de misiones.

### IA utilizada
- Detección exhaustiva de constructores con excepciones crudas y setters expuestos.
- Rediseño inmutable de Value Objects y records de insignias.
- Generación de tests unitarios de caracterización y verificación de guardas.
- Verificación cruzada con Maven, JaCoCo y Spotless.

---

## Oleada 4: Descomposición SRP de Application Services, Domain Services Puros y Segregación de Controladores REST

### Problema
1. **Monolito de Application Service (`IncentivosService`)**: Concentraba 5 responsabilidades de negocio divergentes (gestión de perfil del donante, procesamiento transaccional de donaciones y reglas de misiones, administración de visibilidad de insignias, analítica y reportes de administración, y detección/notificación de inactividad), violando el principio de responsabilidad única (SRP) e introduciendo acoplamiento innecesario.
2. **Controlador REST Concentrador (`IncentivosController`)**: Un único controlador REST inyectaba `IIncentivosService` y manejaba 10 endpoints de diferentes áreas temáticas (donantes, donaciones, misiones, insignias y métricas/resumen).
3. **Domain Services No Gestionados y Estáticos**: `GestorDeRankings` y `GestorDeInactivos` poseían métodos estáticos no instanciables, imposibilitando la inyección de dependencias limpia y el desacoplamiento para pruebas unitarias.
4. **Clientes de Infraestructura sin Contratos Formales**: `NotificacionesClient` y `N8nClient` eran clases concretas consumidas directamente por los servicios y listeners sin abstracciones de interfaz (`INotificacionesClient`, `IN8nClient`).

### Evidencia
- `IncentivosService.java`: 213 líneas, 5 dependencias heterogéneas (`IDonanteIncentivosRepository`, `IRankingService`, `List<CriterioInactividad>`, `ApplicationEventPublisher`, `NotificacionesClient`).
- `IncentivosController.java`: acoplaba todos los endpoints a `IIncentivosService`.
- `GestorDeInactivos.java` / `GestorDeRankings.java`: métodos `public static`.
- `NotificacionesClient.java` / `N8nClient.java`: clases concretas sin interfaz.

### Objetivo
1. **Descomponer `IncentivosService` en 5 Application Services Delgados**:
   - `GestionDonanteService` (`IGestionDonanteService`): Perfil y ciclo de vida (registro, modificación, baja, consulta).
   - `MisionesDonacionService` (`IMisionesDonacionService`): Procesamiento de donaciones normales/exitosas, misiones y verificación de rachas.
   - `InsigniasService` (`IInsigniasService`): Consulta y visibilidad de insignias ganadas.
   - `MetricasIncentivosService` (`IMetricasIncentivosService`): Métricas de donante y resumen del sistema.
   - `InactividadService` (`IInactividadService`): Detección y notificación diaria de inactividad.
2. **Segregar `IncentivosController` en 4 Controladores REST Especializados** (preservando 100% de los contratos HTTP):
   - `DonanteIncentivosController` (`IDonanteIncentivosController`): `POST`, `PATCH`, `DELETE` en `/api/incentivos/donantes/{donanteId}`.
   - `MisionesDonacionController` (`IMisionesDonacionController`): `POST /donaciones`, `POST /donaciones/exitosa`, `GET /donantes/{donanteId}/misiones`.
   - `InsigniasController` (`IInsigniasController`): `GET /donantes/{donanteId}/insignias`, `PUT /donantes/{donanteId}/insignias/{nombreInsignia}/visibilidad`.
   - `MetricasIncentivosController` (`IMetricasIncentivosController`): `GET /donantes/{donanteId}/metricas`, `GET /admin/resumen`.
3. **Desestatizar Domain Services y Registrar en `DomainServicesConfig`**:
   - Convertir `GestorDeInactivos` y `GestorDeRankings` en POJOs con métodos de instancia.
   - Crear `DomainServicesConfig` con beans `@Bean` para mantener el paquete `models/` libre de anotaciones de framework.
4. **Contratos de Infraestructura**:
   - Crear `INotificacionesClient` e `IN8nClient` e implementarlos en `NotificacionesClient` y `N8nClient`.
5. **Alineación de Jobs y Listeners**:
   - `InactividadJob` → inyecta `IInactividadService`.
   - `RachaJob` → inyecta `IMisionesDonacionService`.
   - `NotificacionesIncentivosListener` → inyecta `INotificacionesClient` e `IN8nClient`.
   - `RankingService` → inyecta `GestorDeRankings` e `IN8nClient`.
6. **Desacoplar la Suite de Tests**:
   - Reemplazar `IncentivosServiceTest` por 5 suites unitarias enfocadas (`GestionDonanteServiceTest`, `MisionesDonacionServiceTest`, `InsigniasServiceTest`, `MetricasIncentivosServiceTest`, `InactividadServiceTest`) y actualizar `RankingServiceTest`.

### Fuera de scope
- Erradicación de `Optional.get()` y `orElseThrow()` ciegos en capas de aplicación (Oleada 5).
- Tratamiento de excepciones de infraestructura con `RemoteServiceUnavailableException` (Oleada 6).
- Unificación del scheduler y resilience (Oleada 7).

### Tests / Verificación
- **Suites unitarias ejecutadas**:
  - `GestionDonanteServiceTest`: ✅ 6 tests (registro idempotente, modificación, baja, errores de inexistencia, listado).
  - `MisionesDonacionServiceTest`: ✅ 7 tests (métricas tras donación, error de recurso no encontrado, eventos de ascenso, eventos de misión completada con/sin insignia, consulta de misiones, verificación de rachas).
  - `InsigniasServiceTest`: ✅ 3 tests (listado vacío, configuración de visibilidad, error de insignia no encontrada).
  - `MetricasIncentivosServiceTest`: ✅ 2 tests (cálculo de métricas con posición en ranking, resumen global del sistema).
  - `InactividadServiceTest`: ✅ 2 tests (detección y notificación a cliente, resiliencia ante excepciones remotas).
  - `RankingServiceTest`: ✅ 15 tests actualizados con `GestorDeRankings` instanciado e `IN8nClient`.
- **Barridos mecánicos**:
  - `grep -rnE "@(Component|Autowired|Qualifier|Value|Service|Repository)" models/entities/`: ✅ **0 matches**.
- **Suite completa**:
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **86 tests ejecutados, 0 fallos, 0 errores** (`BUILD SUCCESS`).
  - `mvn spotless:check -f incentivos-service/pom.xml`: ✅ **84 archivos limpios** (100% compliant).

### Diseño resultante
- Se consolida una arquitectura de servicios limpia, altamente cohesiva y desacoplada: cada Application Service posee una única razón para cambiar y declara exclusivamente las dependencias que necesita.
- La capa de presentación REST queda organizada por capacidades de dominio sin modificar en absoluto los contratos, rutas ni respuestas expuestas a los consumidores HTTP.
- Los Domain Services actúan como lógica pura de dominio ejecutable en memoria, mientras que su ciclo de vida como beans es administrado externamente por `DomainServicesConfig`.
- Los clientes de infraestructura quedan protegidos por interfaces explícitas, facilitando tests con mocks limpios y previniendo el acoplamiento a clientes concretos.

### IA utilizada
- Descomposición modular de interfaces y clases de servicios respetando la granularidad funcional.
- Segregación de endpoints REST manteniendo idénticas las rutas de URI y parámetros de consulta.
- Migración y generación de suites de tests unitarios aisladas por cada servicio especializado.
- Verificación cruzada con Maven, JaCoCo y Spotless.

---

## Oleada 5: Tests de Schedulers (Jobs)

### Problema
1. **Ausencia de Pruebas Unitarias en Componentes Batch (`jobs/`)**: Los tres componentes de scheduling (`InactividadJob`, `RachaJob`, `RankingMensualJob`) no disponían de cobertura de tests unitarios que garantizaran la correcta delegación hacia los Application Services correspondientes (`IInactividadService`, `IMisionesDonacionService`, `IRankingService`).
2. **Falta de Validación de Resiliencia ante Fallos en Schedulers**: No existía una prueba explícita que verificara que los errores no controlados (como caídas de n8n o fallos de infraestructura) capturados en `RankingMensualJob` fuesen adecuadamente atrapados y no propagasen excepciones que pudiesen comprometer los threads de Spring TaskScheduler.

### Evidencia
- Directorio `src/test/java/grupo5/incentivos/jobs/` inexistente.
- Cero pruebas unitarias para `InactividadJob.java`, `RachaJob.java` y `RankingMensualJob.java`.

### Objetivo
1. **Implementar `InactividadJobTest`**:
   - Validar que `job.ejecutar()` invoca exactamente una vez a `IInactividadService.procesarInactividad()`.
2. **Implementar `RachaJobTest`**:
   - Validar que `job.verificarRachasVencidas()` pasa el `YearMonth` correspondiente al mes en curso (`ZoneId.systemDefault()`) a `IMisionesDonacionService.verificarRachasVencidas(YearMonth)`.
3. **Implementar `RankingMensualJobTest`**:
   - Validar que `job.ejecutarRankingMensual()` pasa el periodo actual a `IRankingService.calcularYNotificar(YearMonth)`.
   - Validar que ante cualquier `RuntimeException` lanzada por el servicio de ranking, el job maneja la excepción internamente (`assertDoesNotThrow`) sin interrumpir la ejecución.

### Fuera de scope
- Reorganización de infraestructura (`infrastructure/adapters/`, `infrastructure/schedulers/`) (Oleada 6+7).
- Eliminación de wildcard imports restantes (Oleada 6+7).
- Construcción de Object Mothers centralizados (Oleada 8).

### Tests / Verificación
- **Suites unitarias creadas**:
  - `InactividadJobTest`: ✅ 1 test (delegación a `procesarInactividad`).
  - `RachaJobTest`: ✅ 1 test (pasaje de `YearMonth` actual a `verificarRachasVencidas`).
  - `RankingMensualJobTest`: ✅ 2 tests (cálculo/notificación con periodo actual y manejo resiliente ante fallos de servicio).
- **Suite completa**:
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **90 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - `mvn spotless:check -f incentivos-service/pom.xml`: ✅ **87 archivos limpios** (100% compliant).

### Diseño resultante
- La capa de schedulers (`jobs/`) cuenta ahora con cobertura de pruebas unitarias 100% aislada con Mockito, asegurando que cualquier cambio futuro en los contratos de los servicios especializados sea detectado inmediatamente por la suite de integración continua.
- Se garantiza contractualmente la resiliencia en la ejecución periódica de jobs batch.

### IA utilizada
- Generación de tests unitarios con captura de argumentos (`ArgumentCaptor`) para verificación temporal de `YearMonth`.
- Pruebas de resiliencia y verificación de no-propagación de excepciones.
- Verificación cruzada y formateo automático con Spotless.

---

## Oleadas 6+7: Reorganización de Infraestructura, Eliminación de Wildcard Imports y Limpieza General

### Problema
1. **Falta de Sub-empaquetado Semántico en Infraestructura**: Los adaptadores de infraestructura (`NotificacionesClient`, `N8nClient`) y clientes Feign convivían en la raíz de `infrastructure/` sin separar clientes declarativos de adaptadores. Además, los componentes de scheduling residían en un paquete raíz no estándar (`jobs/`) en lugar de ubicarse bajo `infrastructure.schedulers`.
2. **Presencia de Wildcard Imports en Producción**: Clases de producción como `RankingController`, `MisionFactory` y `CrudRepositoryEnMemoria` utilizaban `import .*`, reduciendo la claridad de dependencias y violando las normas de estilo de DonaTrack.
3. **Inconsistencia de Nomenclatura en Tests**: Existía un archivo de test nombrado en plural (`IncentivosServiceApplicationTests.java`) en discordancia con el estándar del proyecto (`*Test.java`).

### Evidencia
- `src/main/java/grupo5/incentivos/infrastructure/NotificacionesClient.java` y `N8nClient.java` en raíz de infraestructura.
- `src/main/java/grupo5/incentivos/jobs/` fuera del árbol de infraestructura.
- `RankingController.java` (`import org.springframework.web.bind.annotation.*`).
- `MisionFactory.java` (`import grupo5.incentivos.models.entities.misiones.*`).
- `CrudRepositoryEnMemoria.java` en `common-lib` (`import java.util.*`).
- `IncentivosServiceApplicationTests.java` (sufijo `Tests`).

### Objetivo
1. **Reorganizar la Capa de Infraestructura**:
   - `infrastructure/adapters/`: [`NotificacionesClientAdapter.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/main/java/grupo5/incentivos/infrastructure/adapters/NotificacionesClientAdapter.java) y [`N8nClientAdapter.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/main/java/grupo5/incentivos/infrastructure/adapters/N8nClientAdapter.java).
   - `infrastructure/clients/`: [`NotificacionesFeignClient.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/main/java/grupo5/incentivos/infrastructure/clients/NotificacionesFeignClient.java).
   - `infrastructure/schedulers/`: Reubicar [`InactividadJob.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/main/java/grupo5/incentivos/infrastructure/schedulers/InactividadJob.java), [`RachaJob.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/main/java/grupo5/incentivos/infrastructure/schedulers/RachaJob.java) y [`RankingMensualJob.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/main/java/grupo5/incentivos/infrastructure/schedulers/RankingMensualJob.java) (así como sus suites de tests unitarios correspondientes).
2. **Erradicar el 100% de Wildcard Imports**: Reemplazar por imports explícitos clase por clase en `RankingController`, `MisionFactory` y `CrudRepositoryEnMemoria`.
3. **Estandarizar Nomenclatura de Tests**: Renombrar a singular [`IncentivosServiceApplicationTest.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/test/java/grupo5/incentivos/IncentivosServiceApplicationTest.java).

### Fuera de scope
- Construcción de Object Mothers centralizados (Oleada 8).
- Validaciones declarativas con anotaciones `@Valid` en controllers y DTOs (Oleada 9).

### Tests / Verificación
- **Barridos mecánicos**:
  - `find src/test -name "*Tests.java"`: ✅ **0 matches** (100% estandarizado en singular `*Test.java`).
  - `grep -rn "import .*\.\*" src/main/java/`: ✅ **0 matches** en `incentivos-service` y `common-lib`.
- **Suite completa**:
  - `mvn clean test -f common-lib/pom.xml`: ✅ **27 tests ejecutados, 0 fallos, 0 errores** (`BUILD SUCCESS`).
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **90 tests ejecutados, 0 fallos, 0 errores** (`BUILD SUCCESS`).
  - `mvn spotless:check`: ✅ **100% de archivos limpios** en ambos módulos (`common-lib` 28 archivos, `incentivos-service` 87 archivos).

### Diseño resultante
- La estructura de paquetes del microservicio refleja cabalmente una arquitectura limpia: las dependencias externas y llamadas a servicios remotos se encuentran explícitamente contenidas en adaptadores (`adapters`) y clientes (`clients`), mientras que los disparadores programados residen en `schedulers`.
- El código fuente no contiene imports comodín, optimizando el análisis estático y previniendo colisiones de nombres inadvertidas.

### IA utilizada
- Reorganización semántica de paquetes y actualización de declaraciones de paquete e imports.
- Reemplazo preciso de wildcard imports por tipos explícitos.
- Barridos automatizados mediante herramientas de búsqueda y comprobación cruzada de builds.

---

## Oleada 8: Object Mothers, "Tell, Don't Ask", Desacoplamiento Arquitectónico y Cobertura Total (100% de Capas)

### Problema
1. **Construcción Dispersa y Posicional en Tests**: Las suites de pruebas instanciaban entidades de dominio (`DonanteIncentivos`, `EventoDonacion`, `RankingMensual`, `Mision`) y DTOs de forma manual y posicional (`new DonanteIncentivos(...)`, `new NuevaDonacionRequest(...)`), generando fragilidad ante futuros cambios en constructores.
2. **Violaciones del Principio "Tell, Don't Ask" en Suites de Pruebas**: Varios tests manipulaban colecciones o estructuras internas de las entidades desde el exterior (`donante.getMisiones().add(...)`, `donante.getMetricas().registrarDonacion(...)`), acoplándose a los detalles de implementación en lugar de interactuar mediante métodos de comportamiento del agregado.
3. **Gaps de Cobertura en Capas Críticas**: No existían tests unitarios para los 5 Controladores REST (`DonanteIncentivosController`, `MisionesDonacionController`, `InsigniasController`, `MetricasIncentivosController`, `RankingController`), el listener de notificaciones (`NotificacionesIncentivosListener`), los adaptadores de infraestructura (`NotificacionesClientAdapter`, `N8nClientAdapter`), las configuraciones `@Bean` (`DomainServicesConfig`, `InactividadConfig`) ni los métodos de mapeo de DTOs (`DTOsAndMappersTest`).
4. **Casos Borde no Cubiertos**: Faltaban pruebas para situaciones límite como donaciones repetidas en el mismo mes en rachas, saltos de 2+ meses, misiones ya completadas que no deben resetearse, donantes en categoría máxima (`HEROE`), idempotencia en asignación de insignias, umbrales exactos ($N-1$ vs $N$) en grandes donaciones, y podios con menos de 3 donantes en rankings.

### Evidencia
- `InactividadDonacionesTest`: utilizaba `donante.getMetricas().registrarDonacion(...)`.
- `RankingServiceTest`: utilizaba `donante.getMisiones().add(...)` y constructores posicionales directos.
- Ausencia de tests para controllers, listeners, adaptadores y DTOs en `src/test/java/`.

### Objetivo
1. **Crear Object Mothers y Fixtures Centralizados en `grupo5.incentivos.fixtures`**:
   - [`DonanteIncentivosMotherTest.java`](../../../../incentivos-service/src/test/java/grupo5/incentivos/fixtures/DonanteIncentivosMother.java): métodos canónicos para donantes colaboradores, con misiones activas, rachas y misiones completadas en meses específicos.
   - [`MisionMotherTest.java`](../../../../incentivos-service/src/test/java/grupo5/incentivos/fixtures/MisionMother.java): métodos para las 4 subclases de `Mision` (`racha`, `exitosas`, `completitud`, `habilDonador`) con y sin insignias.
   - [`RankingMensualMotherTest.java`](../../../../incentivos-service/src/test/java/grupo5/incentivos/fixtures/RankingMensualMother.java): creación de rankings mensuales vacíos o poblados con $N$ entradas.
   - [`EventoDonacionMotherTest.java`](../../../../incentivos-service/src/test/java/grupo5/incentivos/fixtures/EventoDonacionMother.java): eventos parametrizados por fecha, categorías y cantidad de bienes.
   - [`IncentivosFixturesTest.java`](../../../../incentivos-service/src/test/java/grupo5/incentivos/fixtures/IncentivosFixtures.java): fábrica para requests de entrada (`RegistrarDonanteRequest`, `NuevaDonacionRequest`, `DonacionExitosaRequest`, `ModificarDonanteRequest`).
2. **Aplicar "Tell, Don't Ask" en el 100% de los Tests**:
   - Erradicar mutaciones y accesos a listas internas; ordenar al agregado ejecutar acciones (`donante.registrarDonacion(...)`, `donante.otorgarInsignia(...)`) y consultar estado semántico (`donante.getCategoria()`, `donante.misionesCompletadas()`, `donante.insigniasVisibles()`).
   - Hacer idempotente `DonanteIncentivos.otorgarInsignia(insignia)` e incorporar el método `insigniasVisibles()`.
3. **Alcanzar Cobertura Total (100% de Componentes del Microservicio)**:
   - Suites creadas para Controllers: `DonanteIncentivosControllerTest`, `MisionesDonacionControllerTest`, `InsigniasControllerTest`, `MetricasIncentivosControllerTest`, `RankingControllerTest`.
   - Suites creadas para Servicios/Listeners: `NotificacionesIncentivosListenerTest` y refactor exhaustivo de los 6 Application Services.
   - Suites creadas para Infraestructura y Configs: `NotificacionesClientAdapterTest`, `N8nClientAdapterTest`, `DomainServicesConfigTest`, `InactividadConfigTest`.
   - Suites creadas para DTOs/Mappers: `DTOsAndMappersTest`.
4. **Cubrir Casos Borde Rigurosos**:
   - Inmutabilidad de rachas completadas ante periodos vencidos posteriores.
   - Categorías duplicadas en `MisionCompletitud` que no cuentan doble.
   - Umbrales exactos de bienes ($N-1$ vs $N$) en `MisionHabilDonador`.
   - Donante en categoría máxima `HEROE` que no falla al completar misiones.
   - Podios seguros en `RankingMensual` con 0, 1, 2, 3 y 4+ donantes.
   - Resiliencia ante caídas del cliente de notificaciones en `InactividadService`.
5. **Auditoría Crítica y Alineación de Casos de Uso Reales**:
   - **Consulta de Posición por Periodo**: Implementada en `IRankingService`, `RankingService` y `RankingController` (`GET /api/incentivos/ranking/posicion/{donanteId}?periodo=YYYY-MM`), resolviendo la limitación de solo poder consultar el último ranking.
   - **Gestión Integral y Contrato Dual de Insignias**: Soporte para parámetro opcional `soloVisibles` en `IInsigniasService`, `InsigniasService`, `IInsigniasController` e `InsigniasController`. `GET /api/incentivos/donantes/{donanteId}/insignias?soloVisibles=true` retorna únicamente las insignias visibles (`donante.insigniasVisibles()`) para perfiles públicos/showcase, mientras que la consulta estándar sin parámetro o con `soloVisibles=false` retorna todas las insignias con su bandera `visible` para administración y reactivación en paneles de gestión privada.
   - **Unificación de Excepciones**: Estandarizado `BusinessStateException(ErrorCatalog.DONANTE_INCENTIVOS_NO_ENCONTRADO)` en todos los flujos de servicios.
   - **Determinismo y Deduplicación**: Desempate determinista por ID en `GestorDeRankings` y deduplicación de alertas por persona en `GestorDeInactivos`.

### Fuera de scope
- Validaciones declarativas Jakarta Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`) en DTOs y controllers (Oleada 9).
- Propagación de trazabilidad distribuida `X-Trace-Id` (Oleada 9).

### Tests / Verificación
- **Barridos mecánicos**:
  - `grep -rn "new DonanteIncentivos(" src/test/java/**/services/`: ✅ **0 matches**.
  - `grep -rn "new NuevaDonacionRequest(" src/test/java/**/services/`: ✅ **0 matches**.
  - `grep -rn "new RegistrarDonanteRequest(" src/test/java/**/services/`: ✅ **0 matches**.
  - `grep -rn "getMisiones().add(" src/test/`: ✅ **0 matches** (100% Tell, Don't Ask).
  - `grep -rn "getMetricas().registrarDonacion(" src/test/`: ✅ **0 matches** (100% Tell, Don't Ask).
  - `find src/test -name "*Tests.java"`: ✅ **0 matches**.
- **Suite completa**:
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **144 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - `mvn clean test` (reactor completo): ✅ **7/7 módulos exitosos, 100% en verde** (`BUILD SUCCESS`).
  - `mvn spotless:check -f incentivos-service/pom.xml`: ✅ **103 archivos limpios** (100% compliant con Google Java Format).

### Diseño resultante
- Todas las suites de pruebas de `incentivos-service` se encuentran totalmente desacopladas de las representaciones internas de las entidades, actuando como especificaciones vivas de comportamiento bajo la convención AAA (Arrange-Act-Assert) y el principio "Tell, Don't Ask".
- El microservicio alcanza cobertura integral en todas sus capas arquitectónicas (Dominio, Aplicación, Controladores, Infraestructura, Schedulers, Configuración y DTOs) con contratos de negocio que soportan todos los casos de uso reales del sistema.

### IA utilizada
- Diseño e implementación de Object Mothers y Test Data Builders canónicos.
- Auditoría crítica y corrección de compromisos asumidos en tests para robustecer el código de producción.
- Refactorización de suites de pruebas para eliminar manipulaciones directas de colecciones internas.
- Generación de pruebas exhaustivas para controladores REST, listeners de eventos, adaptadores y DTOs.
- Verificación mecánica con herramientas de análisis de patrones (`ripgrep`), validación continua con Maven y formateo con Spotless.

---

## Oleada 9 — Validación en DTOs, Controladores REST, Códigos HTTP y Trazabilidad Distribuida

### Problema
- **Bordes HTTP Desprotegidos**: Los DTOs de entrada carecían de anotaciones declarativas Jakarta Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@NotEmpty`, `@Positive`, `@PastOrPresent`), permitiendo que cargas útiles incompletas, con cadenas en blanco o con fechas en el futuro llegaran hasta la lógica de negocio.
- **Falta de Validación Automática en Controladores**: Los controladores REST no estaban anotados con `@Validated` ni los `@RequestBody` con `@Valid`, omitiendo la interceptación automática de errores por Spring MVC.
- **Puntos Ciegos en el Manejador Global de Excepciones**: Excepciones de conversión de tipos en paths (`MethodArgumentTypeMismatchException`, ej. UUIDs malformados) y errores de parseo de fechas (`DateTimeParseException`) no estaban explícitamente capturados en `GlobalExceptionHandler`, provocando errores HTTP 500 en lugar de respuestas limpias HTTP 400 Bad Request.
- **Trazabilidad Desconectada**: La aplicación no escaneaba el paquete compartido `grupo5`, impidiendo que los filtros de encabezados `X-Trace-Id` (`TraceResponseHeaderFilter`), interceptores Feign salientes (`FeignTraceRequestInterceptor`) y el manejador global fueran auto-configurados. Además, los jobs en background carecían de inicialización de `traceId` en el MDC.

### Evidencia
- Requests con campos nulos o nombres vacíos no eran rechazados en la capa web.
- `IncentivosServiceApplication` anotado con `@SpringBootApplication` sin `scanBasePackages = "grupo5"`.
- Respuestas 500 al enviar UUIDs malformados en `@PathVariable` o periodos inválidos en `@RequestParam`.

### Objetivo
1. Implementar validaciones declarativas Jakarta Bean Validation en todos los DTOs de solicitud (`RegistrarDonanteRequest`, `NuevaDonacionRequest`, `DonacionExitosaRequest`, `ModificarDonanteRequest`).
2. Anotar los 5 controladores REST segregados y sus interfaces con `@Valid` y `@Validated`.
3. Estandarizar códigos HTTP semánticos (201 Created, 200 OK, 204 No Content, 400 Bad Request, 404 Not Found, 409 Conflict).
4. Robustecer `GlobalExceptionHandler` en `common-lib` para capturar `MethodArgumentTypeMismatchException` y `DateTimeParseException`.
5. Habilitar escaneo de componentes compartidos (`scanBasePackages = "grupo5"`) e inicialización contextual de MDC en los schedulers de background.
6. Diseñar suites de pruebas de validación unitaria (`DTOValidationTest`) e integración web (`ControllersWebMvcValidationTest`).

### Qué se hizo
1. **Validaciones Declarativas en DTOs**:
   - `RegistrarDonanteRequest`: `@NotNull` en `idDonante` e `idPersona`, `@NotBlank` y `@Size(min = 2, max = 100)` en `nombre`.
   - `NuevaDonacionRequest`: `@NotNull` en `donanteId` y `fecha`, `@PastOrPresent` en `fecha`, `@NotEmpty` en `categorias` con validación de elementos no vacíos y con límite de longitud (`List<@NotBlank @Size(max = 50) String>`), y `@Positive` en `cantidadBienes`.
   - `DonacionExitosaRequest`: `@NotNull` en `donanteId` y `organizacionId`.
   - `ModificarDonanteRequest`: `@NotBlank` y `@Size(min = 2, max = 100)` en `nombre`.
2. **Controladores REST y Códigos HTTP**:
   - Agregado `@Valid` en todos los `@RequestBody` y `@Validated` a nivel de clase en `DonanteIncentivosController`, `MisionesDonacionController`, `InsigniasController`, `MetricasIncentivosController` y `RankingController`.
   - Agregada validación de consistencia entre `@PathVariable UUID donanteId` y `@RequestBody.idDonante()` en `DonanteIncentivosController.registrarDonante`.
   - Validación de regex con `@Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$")` en el parámetro `periodo` de `RankingController`.
   - Códigos de respuesta: `201 Created` en registro, `200 OK` en transacciones/consultas, `204 No Content` en bajas y rankings vacíos.
3. **Robustecimiento de `GlobalExceptionHandler` en `common-lib`**:
   - Implementado handler para `MethodArgumentTypeMismatchException` retornando 400 Bad Request con detalle del parámetro y tipo esperado.
   - Implementado handler para `DateTimeParseException` retornando 400 Bad Request con detalle del formato inválido.
4. **Trazabilidad Distribuida (`X-Trace-Id`)**:
   - Configurado `@SpringBootApplication(scanBasePackages = "grupo5")` en `IncentivosServiceApplication`.
   - Inyección contextual de `MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""))` con limpieza en bloque `finally` en `InactividadJob`, `RachaJob` y `RankingMensualJob`.
5. **Nuevas Suites de Testing**:
   - `DTOValidationTest`: 11 casos de prueba cubriendo límites, nulos, blancos, colecciones vacías y fechas futuras con `ValidatorFactory`.
   - `ControllersWebMvcValidationTest`: 11 casos de integración WebMvc con `MockMvc` validando respuestas HTTP 201, 200, 204, 400 (Bean Validation, TypeMismatch, DateParse, Path vs Body), 404 y verificación de la cabecera `X-Trace-Id`.

### Fuera de scope
- Preparación para persistencia JPA / Hibernate (Oleada 10).
- Estrategias de herencia y mapeo relacional de misiones e insignias (Oleada 10).

### Tests / Verificación
- **Pruebas de Validación y Controladores**:
  - `DTOValidationTest`: ✅ 11 tests en verde.
  - `ControllersWebMvcValidationTest`: ✅ 11 tests en verde.
- **Suite completa**:
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **166 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - `mvn clean test` (reactor completo): ✅ **7/7 módulos exitosos, 100% en verde** (`BUILD SUCCESS`).
  - `mvn spotless:check`: ✅ **100% de archivos limpios en todo el repositorio**.

### Diseño resultante
- El microservicio `incentivos-service` cuenta con una arquitectura de bordes HTTP totalmente blindada bajo el principio de "Defense in Depth", con validaciones declarativas automáticas en DTOs, validaciones de parámetros en controladores, respuestas de error uniformes con códigos HTTP estándar y trazabilidad distribuida end-to-end con `X-Trace-Id`.

### IA utilizada
- Análisis crítico de puntos ciegos y vectores de mejora en la capa de transporte HTTP y manejo global de excepciones.
- Generación de restricciones declarativas Jakarta Bean Validation y validaciones temporales con `@PastOrPresent`.
- Configuración de infraestructura de trazabilidad distribuida con Spring Boot Component Scanning y MDC en Schedulers.
- Construcción de suites de pruebas con `jakarta.validation.Validator` y `@WebMvcTest` con `MockMvc`.

---

## Oleada 10 — Preparación Conceptual para Persistencia Real 📝

> **Nota de Gobernanza**: Esta oleada es estrictamente analítica y de diseño técnico (`📝`), asegurando que la capa de dominio permanezca limpia, desacoplada y preparada para la fase de persistencia física relacional en PostgreSQL 15+ con JPA/Hibernate 6.

### Problema
- **Inexistencia de Estrategia ORM**: El microservicio carecía de definiciones formales sobre cómo persistir la jerarquía polimórfica de misiones (`Mision`), los Value Objects embebidos (`Metricas`, `InsigniaGanada`) y las proyecciones de ranking (`RankingMensual`).
- **Riesgo de Cuello de Botella en Memoria**: Cargar colecciones no acotadas de historial de donaciones (`List<EventoDonacion>`) dentro del componente embebido `Metricas` en cada lectura del donante amenazaba el rendimiento en entornos de alta concurrencia.
- **Riesgo de Doble Escritura (*Dual-Write*)**: La publicación de eventos de dominio (`AscensoDonante`, `MisionCompletada`) hacia `notificaciones-service` o brokers de mensajería dentro de transacciones de base de datos podía generar inconsistencias si la transacción hacía rollback después del dispatch externo.

### Evidencia
- Ausencia de documentos técnicos DDL y ORM en `docs/design/`.
- `DonanteIncentivos` y `RankingMensual` operando exclusivamente con repositorios en memoria sin esquema relacional formal.

### Objetivo
1. Diseñar el mapeo ORM JPA/Hibernate 6 para todos los agregados, jerarquías polimórficas y Value Objects.
2. Definir la estrategia de herencia `@Inheritance(strategy = SINGLE_TABLE)` para la jerarquía `Mision`.
3. Optimizar el almacenamiento de `Metricas` mediante aplanamiento escalar en `@Embeddable`.
4. Diseñar el esquema relacional DDL completo en PostgreSQL 15+ con índices, claves foráneas y restricciones `CHECK`.
5. Diseñar el patrón Transactional Outbox con la tabla `outbox_events` y relay asíncrono.
6. Especificar la estrategia de Crypto-Shredding para supresión de datos personales (GDPR / Ley 25.326).
7. Diseñar la suite de integración de persistencia con Testcontainers (`PostgreSQLContainer`).

### Qué se hizo
1. **Auditoría de Agregados y Mapeo ORM**:
   - `DonanteIncentivos` (AR): `@Embedded Metricas`, `@OneToMany` para `Mision` (`CascadeType.ALL`, `orphanRemoval = true`, `FetchType.LAZY`), `@ElementCollection` para `InsigniasGanadas`. Concurrencia optimista con campo `version: Long` (`@Version`).
   - `RankingMensual` (AR): Mapeo de `periodo` (`YearMonth`) con `AttributeConverter` a `VARCHAR(7)` con restricción `UNIQUE`. `@ElementCollection` para `EntradaRanking`.
   - `Mision` (Jerarquía Polimórfica): Estrategia `@Inheritance(strategy = SINGLE_TABLE)` con discriminador `@DiscriminatorColumn(tipo_mision)`. Mapeo `@ElementCollection` para categorías en `MisionCompletitud`.
2. **Optimización de Rendimiento en `Metricas`**:
   - Aplanamiento escalar de `Metricas` (`total_donaciones`, `donaciones_consecutivas`, `max_donaciones_consecutivas`, `total_donaciones_exitosas`, `ultima_donacion_fecha`) en la tabla `donante_incentivos`, evitando sobrecarga de memoria al leer donantes.
3. **Esquema Relacional DDL (PostgreSQL 15+)**:
   - Tablas `donante_incentivos`, `mision`, `mision_categorias_necesarias`, `mision_categorias_donadas`, `donante_insignia_ganada`, `donante_historial_donacion`, `ranking_mensual`, `ranking_mensual_posicion` y `outbox_events`.
   - Índices compuestos, claves foráneas con cascada apropiada y restricciones de unicidad e integridad (`CHECK > 0`).
4. **Patrón Transactional Outbox**:
   - Tabla `outbox_events` con despacho seguro mediante `SELECT FOR UPDATE SKIP LOCKED` y serialización JSONB de eventos de dominio (`AscensoDonante`, `MisionCompletada`).
5. **Privacidad y Crypto-Shredding**:
   - Anonimización por destrucción de clave criptográfica (DEK) asociada al `personaId` del donante, garantizando el derecho de supresión sin romper integridad referencial ni alterar las estadísticas históricas de rankings.
6. **Estrategia de Testcontainers**:
   - Especificación de suite `@DataJpaTest` con contenedor efímero `PostgreSQLContainer` para validación de converters, herencia `SINGLE_TABLE` y concurrencia optimista.
7. **Documento de Referencia Técnica**:
   - Creado [`docs/design/incentivos-service/decisiones_futuras_en_oleada_10.md`](decisiones_futuras_en_oleada_10.md).

### Fuera de scope
- Cierre final de code review y sanitización de seguridad (Oleadas 11+12).
- Barrido mecánico obligatorio y auditoría final de Domain Events (Oleadas 11+12).

### Tests / Verificación
- **Suite completa**:
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **166 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - `mvn clean test` (reactor completo): ✅ **7/7 módulos exitosos, 100% en verde** (`BUILD SUCCESS`).
  - `mvn spotless:check`: ✅ **100% de archivos limpios en todo el repositorio**.

### Diseño resultante
- `incentivos-service` cuenta con una arquitectura de persistencia relacional física completamente diseñada, optimizada para alto rendimiento, con transaccionalidad ACID atómica, consistencia eventual garantizada mediante Transactional Outbox, cumplimiento de normativas de privacidad (Crypto-Shredding) y cero acoplamientos invasivos en el dominio puro.

### IA utilizada
- Modelado de esquemas relacionales DDL avanzados y diseño de herencia polimórfica en JPA.
- Análisis de rendimiento para aplanamiento escalar de Value Objects y colecciones históricas.
- Diseño de diagramas de arquitectura Mermaid (clases, secuencias de outbox y crypto-shredding).
- Elaboración de planes de testing de integración con Testcontainers.

---

## Oleada 11: Cierre Exhaustivo de Code Review, Higiene de Logs, Sanitización de Seguridad y Estabilización de Contratos de Entrada

### Problema
1. **Fragilidad y "Magic Strings" en Suites de Pruebas (Violación de DAMP over DRY)**: Diversos tests unitarios (`GestionDonanteServiceTest`, `DTOsAndMappersTest`, `RankingMensualTest`, `InsigniaTest`) empleaban literales de texto hardcodeados en sus aserciones (`assertEquals("Modificado", ...)`, `assertEquals("COLABORADOR", ...)`, `assertEquals("2026-05", ...)`), generando fragilidad ante refactors de fixtures y violando la práctica de comparar dinámicamente contra los objetos de entrada o entidades.
2. **Gaps de Pruebas Defensivas en Domain Services POJOs (Principio de Mínimo Asombro - POLA)**: Aunque `GestorDeRankings`, `GestorDeInactivos` e `InactividadDonaciones` operaban como POJOs puros libres de frameworks, carecían de pruebas unitarias explícitas que validaran su comportamiento determinista ante inputs nulos o colecciones vacías (`calcular(null, ...)`, `procesarInactividad(null, null)`).
3. **Auditoría de Higiene en Logs y Aislamiento de Clientes Externos (Bulkhead Pattern)**: Era necesario auditar y formalizar que los adaptadores de infraestructura (`NotificacionesClientAdapter`, `N8nClientAdapter`) y jobs batch (`RankingMensualJob`, `InactividadJob`, `RachaJob`) aíslen fallas externas sin propagar excepciones que comprometan el flujo principal del donante, garantizando que los logs no filtren información sensible ni stacktraces crudos innecesarios.
4. **Formalización de Decisiones de Negocio en DTOs y Fronteras REST (Defense in Depth)**: Se requería documentar exhaustivamente las justificaciones operativas de cada restricción declarativa (`@NotNull`, `@NotBlank`, `@Size`, `@NotEmpty`, `@Positive`, `@PastOrPresent`) y la consistencia de contratos HTTP (validación path vs body, regex en periodos, soporte opcional de `soloVisibles`).

### Evidencia
- `GestionDonanteServiceTest.java`: `assertEquals("Modificado", guardado.getNombre());` y `assertEquals("Test", donante.getNombre());`.
- `DTOsAndMappersTest.java`: `assertEquals("COLABORADOR", dto.categoria());`, `assertEquals("Insignia Plantilla", dtoPlantilla.nombre());`, `assertEquals("2026-05", dto.periodo());`.
- `RankingMensualTest.java`: `assertEquals("Donante 1", ranking.getPodio().getFirst().getNombreDonante());`.
- `DomainServicesConfigTest.java`: Únicamente verificaba instanciación de beans no nulos sin pruebas de defensividad ante nulls/vacíos.

### Objetivo
1. **Parametrización Dinámica en Aserciones de Tests**:
   - Reemplazar magic strings por accesos dinámicos a las propiedades de los DTOs y entidades (`request.nombre()`, `donante.getCategoria().name()`, `plantilla.nombre()`, `mayo.toString()`, etc.).
2. **Guards Defensivos y Tests de Null-Safety en Domain Services**:
   - Agregar pruebas unitarias exhaustivas en `DomainServicesConfigTest` validando que `GestorDeRankings` y `GestorDeInactivos` retornen colecciones/proyecciones vacías seguras ante inputs nulos o listas vacías.
3. **Auditoría de Higiene de Logs y Aislamiento de Integraciones Remotas**:
   - Verificar la no-fuga de datos sensibles en adaptadores de notificación y WebClient n8n, garantizando que caídas transitorias de servicios externos se registren con logs estructurados sin interrumpir el flujo del donante.
4. **Consolidar la Matriz de Invariantes y No-Regresión Lógica (Oleadas 0 a 10)**:
   - Registrar formalmente las salvaguardas que garantizan que ninguna regla de negocio conquistada en oleadas previas sea alterada.
5. **Documentar Decisiones de Frontera y Justificaciones de DTOs**:
   - Explicar las razones operativas de los campos obligatorios vs opcionales en `RegistrarDonanteRequest`, `NuevaDonacionRequest`, `DonacionExitosaRequest` y `ModificarDonanteRequest`.

### Fuera de scope
- Auditoría exhaustiva final de State Pattern y hardening pre-JPA (Oleada 12).
- Verificación final de gobernanza y cierre de gaps de persistencia relacional (Oleada 13).

### Tests / Verificación
- **Suites Actualizadas y Nuevas**:
  - `GestionDonanteServiceTest`: ✅ Aserciones dinámicas con `request.nombre()`.
  - `DTOsAndMappersTest`: ✅ Aserciones dinámicas con enums y propiedades de records.
  - `InsigniaTest`: ✅ Variables locales parametrizadas en validaciones de insignias.
  - `RankingMensualTest`: ✅ Aserción dinámica en podios de ranking.
  - `DomainServicesConfigTest`: ✅ 3 tests unitarios (instanciación de beans, null-safety en `GestorDeRankings` y combinaciones de nulls/vacíos en `GestorDeInactivos`).
- **Barridos Mecánicos**:
  - `git grep "import .*\.\*"`: ✅ **0 matches** (100% imports explícitos).
  - `git grep -E "@(Component|Autowired|Qualifier|Value)" src/main/java/**/models/entities/`: ✅ **0 matches** (100% pureza de dominio).
  - `Get-ChildItem -Path src/test -Filter "*Tests.java"`: ✅ **0 matches** (100% singular `*Test.java`).
- **Suite completa**:
  - `mvn clean test -f incentivos-service/pom.xml`: ✅ **170 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - `mvn clean test` (reactor completo): ✅ **7/7 módulos exitosos, 100% en verde** (`BUILD SUCCESS`).
  - `mvn spotless:check`: ✅ **100% de archivos limpios en todo el repositorio**.

### Diseño resultante
- **Robustez y Resiliencia en Dominio e Infraestructura**: Los Domain Services son inmunes a inputs nulos o listas vacías, retornando resultados neutros seguros bajo el Principio de Mínimo Asombro. Las llamadas a sistemas externos permanecen desacopladas y aisladas de fallos.
- **Tests Limpios y Mantenibles**: Se erradican las aserciones frágiles atadas a literales duplicados, asegurando que las suites de pruebas operen como especificaciones dinámicas y expresivas del comportamiento del sistema.
- **Fronteras HTTP Blindadas y Justificadas**: Las validaciones declarativas Jakarta Bean Validation y los contratos de controladores quedan formalmente justificados en función de la realidad operativa de DonaTrack.

### IA utilizada
- Detección de magic strings en aserciones de pruebas y parametrización dinámica con DTOs.
- Diseño e implementación de pruebas defensivas de null-safety en Domain Services.
- Verificación mecánica con herramientas de análisis de patrones (`ripgrep`), validación continua con Maven y formateo con Spotless.

### Verificación humana
- [x] Verificada la parametrización dinámica de aserciones en `GestionDonanteServiceTest`, `DTOsAndMappersTest`, `InsigniaTest` y `RankingMensualTest`.
- [x] Verificada la cobertura defensiva de null-safety en `DomainServicesConfigTest`.
- [x] Verificada la ausencia de wildcard imports, FQCNs y anotaciones Spring en `models/entities/`.
- [x] Verificada la matriz de invariantes y no-regresión de Oleadas 0 a 10.
- [x] Build multi-módulo limpio: `mvn clean test` (**BUILD SUCCESS** en 7/7 módulos).
- [x] Formato Spotless limpio: `mvn spotless:check` (**BUILD SUCCESS**).

---

## Oleada 12: Hardening Final de Dominio, Consistencia Temporal e Infraestructura Pre-JPA

### Problema
1. **Desacople Temporal e Inconsistencia en Insignias Ganadas (Event Time vs. Processing Time)**: `DonanteIncentivos.otorgarInsignia(Insignia)` estampaba ciegamente `LocalDate.now(ZoneId.systemDefault())`. Cuando `Mision.completar(donante, fecha)` se ejecutaba a partir de un `EventoDonacion` con fecha de negocio ($F$), dicha fecha se descartaba y la `InsigniaGanada` quedaba registrada con el reloj del servidor en tiempo de procesamiento, provocando inconsistencias en reprocesamientos de eventos históricos o diferidos.
2. **Sensibilidad a Mayúsculas y Espacios en `MisionCompletitud` (Falta de Normalización Canónica)**: Las categorías de bienes donados ingresaban sin sanitizar; cadenas como `" Alimentos "`, `"alimentos"` y `"ALIMENTOS"` eran tratadas como categorías distintas en el `Set<String>`, permitiendo avances artificiales de la misión.
3. **Discrepancia Semántica en Umbrales de `MisionHabilDonador`**: La descripción textual indicaba `"más de X bienes"` ($> X$), mientras que la regla de negocio evaluaba $\ge X$.
4. **Fuga de Referencias Mutables en `Metricas`**: Los getters de Lombok exponían las referencias directas a `historialDonaciones` y `organizacionesAyudadas`, permitiendo que consumidores externos mutaran las colecciones internas del agregado.
5. **Riesgo de Agotamiento de Hilos en Despacho Asíncrono (`SimpleAsyncTaskExecutor`)**: `@EnableAsync` sin un `TaskExecutor` explícito creaba hilos de sistema operativo no acotados ante ráfagas de eventos de notificación, arriesgando fallos de memoria nativa.

### Evidencia
- `DonanteIncentivos.java`: `new InsigniaGanada(..., LocalDate.now(ZoneId.systemDefault()));`.
- `Mision.java`: `donante.otorgarInsignia(this.insignia);` (sin propagar la fecha de completitud).
- `MisionCompletitud.java`: `this.categoriasdonadas.addAll(evento.getCategorias());` (sin `trim().toLowerCase(Locale.ROOT)`).
- `MisionHabilDonador.java` y `MisionFactory.java`: Descripción con `"más de X bienes"`.
- `IncentivosServiceApplication.java`: `@EnableAsync` a nivel de aplicación sin pool acotado ni configuración de rechazo.

### Objetivo
1. **Propagación de Fecha en Insignias Ganadas**:
   - Sobrecargar `DonanteIncentivos.otorgarInsignia(Insignia, LocalDate fecha)` con fallback defensivo a `LocalDate.now()`.
   - Propagar `fecha` desde `Mision.completar(donante, fecha)` hacia `donante.otorgarInsignia(this.insignia, fecha)`.
2. **Canonicalización Léxica en `MisionCompletitud`**:
   - Aplicar `cat.trim().toLowerCase(Locale.ROOT)` y filtrado de cadenas vacías/nulas en `MisionCompletitud`.
3. **Alineación Semántica en `MisionHabilDonador`**:
   - Homogeneizar descripciones a `"Realiza una donacion con al menos X bienes"` ("Una donación con al menos 50 bienes").
4. **Encapsulamiento Defensivo en `Metricas`**:
   - Sobrescribir getters para retornar `List.copyOf(this.historialDonaciones)` y `Set.copyOf(this.organizacionesAyudadas)`.
5. **Aislamiento y Backpressure en Infraestructura Asíncrona (`AsyncConfig`)**:
   - Crear [`AsyncConfig.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/main/java/grupo5/incentivos/config/AsyncConfig.java) declarando `@Bean(name = "notificacionesTaskExecutor") ThreadPoolTaskExecutor` con límites estrictos (`corePoolSize=2`, `maxPoolSize=10`, `queueCapacity=500`, `CallerRunsPolicy` y prefijo `async-notif-`).
   - Enrutar `@Async("notificacionesTaskExecutor")` en `NotificacionesClientAdapter`.
6. **Auditoría de Invariantes y Estado Terminal (`TRANSFORMADOR`)**:
   - Incorporar tests de estabilidad para donantes en categoría máxima que completan todas sus misiones y continúan donando.

### Fuera de scope
- Implementación de esquema JPA, entidades `@Entity` y tablas PostgreSQL (Oleada 13 / Fase Física).
- Implementación de relay de Transactional Outbox (Post-JPA).

### Tests / Verificación
- **Nuevas Pruebas Unitarias Agregadas**:
  - `DonanteIncentivosTest`:
    - `otorgarInsignia_conFechaEspecifica_debeAsignarFechaCorrecta()`: ✅ Verifica que la insignia ganada guarde la fecha del evento.
    - `otorgarInsignia_conFechaNula_debeAsignarFechaActual()`: ✅ Verifica fallback seguro ante nulo.
    - `configurarVisibilidadInsignia_conNombreNuloOVacio_debeLanzarExcepcion()`: ✅ Guard defensivo validado.
    - `donanteEnCategoriaMaxima_debeRegistrarDonacionesSinErrores()`: ✅ Estabilidad de estado terminal verificada.
  - `MisionesTest`:
    - `misionCompletitud_conCategoriasConEspaciosYMayusculas_debeNormalizarSinDuplicar()`: ✅ Normalización con `Locale.ROOT`.
    - `misionCompletar_debePropagarFechaDeDonacionAInsigniaGanada()`: ✅ Propagación end-to-end de fecha de misión a insignia.
  - `MetricasTest` *(NEW)*:
    - 6 tests unitarios cubriendo cálculo de métricas, inicialización, actualización y **copias defensivas inmutables** (`UnsupportedOperationException` en listas/sets retornados).
  - `AsyncConfigTest` *(NEW)*:
    - 1 test unitario verificando la configuración del pool (`core=2`, `max=10`, `queue=500`, prefijo `async-notif-`).
- **Resultados de Ejecución**:
  - `incentivos-service`: ✅ **181 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - Reactor Completo (7 módulos): ✅ **7/7 módulos en verde** (`BUILD SUCCESS` en `donatrack`, `common-lib`, `donaciones-service`, `notificaciones-service`, `incentivos-service`, `logistica-service`, `integration-tests`).
  - Spotless: ✅ **100% compliant** en todos los módulos (`BUILD SUCCESS`).
  - Pureza de dominio: ✅ **0 wildcard imports**, **0 anotaciones Spring en models**, **0 plural tests**.

### Diseño resultante
- **Consistencia Temporal Determinista**: Las insignias ganadas capturan fielmente el *Event Time* de la donación que las ameritó, permitiendo reprocesamiento y auditoría fidedigna.
- **Dominio Robusto e Inmutable**: Normalización léxica en misiones, umbrales semánticamente alineados, copias inmutables en métricas y manejo seguro de donantes en estados máximos de gamificación.
- **Infraestructura Asíncrona Resiliente**: El microservicio cuenta con un pool acotado de ejecución asíncrona con política `CallerRunsPolicy`, garantizando que ráfagas de notificaciones no agoten los recursos del sistema operativo y apliquen backpressure natural.

### IA utilizada
- Diagnóstico crítico de consistencia temporal (*Event Time* vs *Processing Time*) y canonicalización léxica con `Locale.ROOT`.
- Diseño del ejecutor asíncrono acotado con política de rechazo `CallerRunsPolicy`.
- Implementación de suites de prueba de caracterización y copias defensivas inmutables.
- Verificación multi-módulo continua con Maven y formateo con Spotless.

### Verificación humana
- [x] Verificada la sobrecarga de `otorgarInsignia(Insignia, LocalDate)` y su propagación en `Mision.completar`.
- [x] Verificada la normalización `trim().toLowerCase(Locale.ROOT)` en `MisionCompletitud`.
- [x] Verificada la inmutabilidad de colecciones en `Metricas.getHistorialDonaciones()` y `Metricas.getOrganizacionesAyudadas()`.
- [x] Verificada la configuración de `AsyncConfig` con `ThreadPoolTaskExecutor` acotado.
- [x] Verificada la estabilidad del agregado `DonanteIncentivos` en categoría `TRANSFORMADOR`.
- [x] 181 tests en verde en `incentivos-service` y 7/7 módulos en verde en el reactor general.
- [x] Formato Spotless 100% compliant (`mvn spotless:check`).

---

## Oleada 13: Gobernanza Final, Gaps de Persistencia Relacional, Escalabilidad y Sincronización Arquitectónica

### Problema
1. **Riesgo de Agotamiento de Memoria JVM en Cálculo de Ranking Masivo**: El diseño en memoria de `GestorDeRankings.calcular` requería iterar sobre `List<DonanteIncentivos>` (`findAll()`), lo que ante 500.000+ donantes en producción produciría sobrecarga masiva de Garbage Collection y `OutOfMemoryError`.
2. **Gaps de Resiliencia ante Concurrencia y Fallos de Red**: La transición a persistencia física en PostgreSQL requería formalizar el manejo de colisiones de versión concurrente (`@Version` con `@Retryable`), la mitigación del problema $N+1$ / producto cartesiano mediante `FetchType.LAZY` y `@EntityGraph`, el ciclo de vida de Outbox con Dead Letter Queue (DLQ) y la deduplicación de mensajes asíncronos.
3. **Desfase en el Diagrama Maestro PlantUML (`diagrama-de-clases-incentivos.puml`)**: El diagrama de clases no reflejaba las evoluciones conquistadas en las Oleadas 1 a 12 (sobrecarga `otorgarInsignia`, nuevos métodos de `Metricas`, `soloVisibles` en `IInsigniasService`, `AsyncConfig`).
4. **Cierre y Consolidación de Bitácoras de Gobernanza**: Se requería auditar integralmente las 13 oleadas del refactor, asegurando cero ítems pendientes y sellando la trazabilidad técnica del repositorio.

### Evidencia
- `decisiones_futuras_en_oleada_10.md`: Carecía de especificaciones formales para agregaciones nativas de ranking, índices parciales para outbox, políticas de reintento con backoff exponencial y jobs de purga.
- `diagrama-de-clases-incentivos.puml`: Presentaba métodos y atributos obsoletos en `Metricas`, `DonanteIncentivos`, `Mision` y servicios.
- `plan-refactor-incentivos.md`: Requería el sellado definitivo de los checklists de gobernanza.

### Objetivo
1. **Especificación de Cómputo de Ranking Escalable en Base de Datos**:
   - Formalizar en `decisiones_futuras_en_oleada_10.md` la consulta SQL nativa de agregación e inserción en un único pase (`ROW_NUMBER() OVER (...)`) en PostgreSQL 15+, reduciendo la complejidad en Heap de $O(N)$ a $O(1)$.
2. **Optimización de Índices Parciales y Resiliencia**:
   - Documentar el índice parcial `WHERE estado = 'PENDIENTE'` en `outbox_events` ($< 1$ ms de latencia en polling).
   - Documentar `@Retryable` con backoff exponencial para `GestionDonanteService` y `MisionesDonacionService` ante `OptimisticLockException`.
   - Formalizar `FetchType.LAZY` obligatorio en todas las colecciones y `@EntityGraph` para lecturas individuales.
   - Definir el ciclo de vida de Outbox con Dead Letter Queue (DLQ tras 5 reintentos) y job de purga (`OutboxCleanupJob` $> 14$ días).
3. **Sincronización Total del Diagrama PlantUML**:
   - Actualizar exhaustivamente `diagrama-de-clases-incentivos.puml` con el 100% de las clases, interfaces, firmas y relaciones de las 13 oleadas.
4. **Auditoría Integral de No-Regresión y Sellado de Gobernanza**:
   - Verificar la matriz de no-regresión de Oleadas 0 a 12, consolidar el plan maestro y certificar el reactor multi-módulo con suite verde y Spotless 100% compliant.

### Fuera de scope
- Implementación del código de persistencia física JPA/PostgreSQL (fase física posterior).

### Tests / Verificación
- **Auditoría de Invariantes y No-Regresión**:
  - Oleadas 0 a 12 auditadas exhaustivamente y verificadas en código y tests.
- **Barridos Mecánicos**:
  - `git grep "import .*\.\*"`: ✅ **0 matches** en código de producción.
  - `git grep -E "@(Component|Autowired|Qualifier|Value)" src/main/java/**/models/entities/`: ✅ **0 matches** (dominio 100% puro).
  - `Get-ChildItem -Path src/test -Filter "*Tests.java"`: ✅ **0 matches** (100% singular `*Test.java`).
- **Ejecución de Suites**:
  - `incentivos-service`: ✅ **181 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - Reactor Completo (7 módulos): ✅ **7/7 módulos en verde** (`BUILD SUCCESS` en `donatrack`, `common-lib`, `donaciones-service`, `notificaciones-service`, `incentivos-service`, `logistica-service`, `integration-tests`).
  - Spotless: ✅ **100% compliant** en todo el reactor multi-módulo (`BUILD SUCCESS`).

### Diseño resultante
- **Arquitectura Física y de Escalabilidad Totalmente Especificada**: PostgreSQL 15+ optimizado con DDL completo, agregaciones SQL de ranking, índices parciales, concurrency control con reintentos y Outbox con DLQ.
- **Modelado Técnico Sincronizado**: `diagrama-de-clases-incentivos.puml` representa fielmente la totalidad del sistema.
- **Gobernanza Completa y Sellada**: 13 oleadas planificadas, ejecutadas, verificadas y documentadas con rigor de ingeniería de software.

### IA utilizada
- Modelado relacional avanzado, análisis de consultas SQL de agregación por ventana y diseño de índices parciales.
- Especificación de patrones de resiliencia (Spring Retry, Idempotent Consumer, Dead Letter Queue).
- Sincronización y validación de diagramas PlantUML.
- Auditoría automatizada de matrices de no-regresión y verificación multi-módulo continua.

### Verificación humana
- [x] Verificada la especificación técnica de SQL Ranking Aggregation e índices parciales en `decisiones_futuras_en_oleada_10.md`.
- [x] Verificada la estrategia de resiliencia (`@Retryable`, `FetchType.LAZY`, Outbox DLQ y Purga).
- [x] Verificada la sincronización de `diagrama-de-clases-incentivos.puml`.
- [x] Verificada la matriz de no-regresión de Oleadas 0 a 12.
- [x] 181 tests en verde en `incentivos-service` y 7/7 módulos en verde en el reactor multi-módulo.
- [x] Formato Spotless 100% compliant (`mvn spotless:check`).
- [x] Bitácora `oleadas-refactor.md` y plan `plan-refactor-incentivos.md` 100% completados y sellados.

---

## Post-Auditoría: Hardening Integral de Dominio, Trazabilidad Asíncrona y Persistencia Distribuida

### Problema
Tras la auditoría crítica integral del microservicio, se detectaron 5 oportunidades de hardening inmediato en memoria y 6 requerimientos de arquitectura física para la fase relacional:
1. **Falso Positivo de Inactividad**: `InactividadDonaciones.esInactivo` evaluaba `ultimaDonacion == null || ultimaDonacion.isBefore(umbral)`. Los donantes recién registrados sin donaciones eran catalogados erróneamente como inactivos en su primer día al no evaluarse su `fechaRegistro`.
2. **Degradación Destructiva ante Eventos Fuera de Orden en `MisionRacha`**: Si un evento con fecha retroactiva o diferida ingresaba con un mes anterior a `ultimoMesDonado`, `calcularNuevoProgreso` caía en la rama `else`, reseteando el progreso a 1 y sobreescribiendo `ultimoMesDonado`.
3. **Pérdida de Contexto de Trazabilidad en Hilos `@Async`**: `AsyncConfig` configuraba el `ThreadPoolTaskExecutor` sin un `TaskDecorator`, perdiendo el `X-Trace-Id` / `MDC` en los hilos del pool al despachar notificaciones asíncronas.
4. **Residuo de Introspección en `MetricasIncentivosService`**: Violaba levemente "Tell, Don't Ask" al penetrar colecciones internas de misiones y acceder a `metricas.donacionesPorPeriodo()` en lugar de delegar en el agregado `DonanteIncentivos`.
5. **Criterio de Desempate de Rankings**: Ante empate de misiones en el mes, `GestorDeRankings` desempataba únicamente por `UUID`, sin premiar el volumen total de donaciones en el período.
6. **Gaps de Persistencia Distribuida (Oleada 10)**: Necesidad de especificar formalmente la idempotencia en ingesta con `donacionId`, la coordinación de schedulers en clúster con **ShedLock**, la integración de webhooks n8n vía **Transactional Outbox**, proyecciones SQL nativas y control de acceso con claims JWT.

### Evidencia
- `InactividadDonaciones.java`: Líneas 38-41 evaluaban `ultimaDonacion == null` retornando `true` (inactivo).
- `MisionRacha.java`: Líneas 33-52 carecían de guarda para `mesEvento.isBefore(ultimoMesDonado)`.
- `AsyncConfig.java`: Líneas 14-24 configuraban el bean `notificacionesTaskExecutor` sin `setTaskDecorator`.
- `MetricasIncentivosService.java`: Líneas 36 y 53 interpelaban colecciones internas de donantes y misiones.
- `GestorDeRankings.java`: Líneas 22-28 solo incluían `thenComparing(DonanteIncentivos::getId)`.

### Objetivo
1. **Hardening de Dominio e Inactividad**:
   - Incorporar `fechaRegistro: LocalDate` en `DonanteIncentivos` con constructor de reconstitución/hidratación y delegador `donacionesPorPeriodo()`.
   - Evaluar `fechaRegistro` en `InactividadDonaciones` cuando no existe `ultimaDonacion`, calculando días de inactividad respecto al alta real.
   - Proteger `MisionRacha` reteniendo el progreso actual sin mutar `ultimoMesDonado` ante eventos pasados.
   - Desempatar en `GestorDeRankings` por `donacionesEnMes(periodo)` (DESC) antes de comparar por `UUID`.
   - Limpiar `MetricasIncentivosService` delegando en `donante.misionesCompletadas()` y `donante.donacionesPorPeriodo()`.
2. **Propagación Asíncrona de Trazabilidad**:
   - Configurar `TaskDecorator` en `AsyncConfig` con copia bidireccional y limpieza segura en `finally` del contexto `MDC` de SLF4J.
3. **Ampliación de Decisiones de Persistencia Distribuida**:
   - Incorporar en `decisiones_futuras_en_oleada_10.md` las secciones 11.3 (Idempotencia con `donacionId`), 12 (ShedLock en PostgreSQL), 13 (Outbox para n8n), 14 (Proyecciones SQL), 15 (Seguridad JWT) y 16 (Concurrencia de Misiones).
4. **Sincronización de Modelado y Fixtures**:
   - Sincronizar `diagrama-de-clases-incentivos.puml` y `DonanteIncentivosMotherTest`.

### Fuera de scope
- Implementación de controladores REST para endpoints nuevos o mutación de contratos de otros microservicios en esta fase.

### Tests / Verificación
- **Tests Creados y Actualizados**:
  - `InactividadDonacionesTest`: Validación de donante nuevo registrado hoy (0 inactivos) y donante antiguo sin donaciones (45 días inactivo).
  - `MisionesTest`: Test `racha_conEventoDiferidoDeMesAnterior_noDeberiaResetearProgresoAvanzado`.
  - `AsyncConfigTest`: Test concurrente `notificacionesTaskExecutor_deberiaPropagarMdcContextoAHiloDeTrabajo`.
  - `RankingMensualTest`: Test `gestorDeRankings_conEmpateDeMisiones_deberiaDesempatarPorDonacionesTotalesEnMes`.
- **Métricas de Cobertura JaCoCo**:
  - Líneas: **99.00%** (792 / 800 líneas cubiertas).
  - Instrucciones (Bytecode): **98.35%** (3.340 / 3.396 instrucciones cubiertas).
  - Ramas (Branches): **81.04%** (171 / 211 ramas cubiertas).
- **Ejecución de Suites**:
  - `incentivos-service`: ✅ **185 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (`BUILD SUCCESS`).
  - Reactor Completo (7 módulos): ✅ **7/7 módulos en verde** (`BUILD SUCCESS`).
  - Spotless: ✅ **100% compliant** en todo el repositorio (`BUILD SUCCESS`).

### Diseño resultante
- **Dominio Robusto y Libre de Falsos Positivos**: El Aggregate Root encapsula su fecha de registro y es inmune a eventos desordenados en el tiempo.
- **Trazabilidad Integral End-to-End**: Los logs de tareas asíncronas en hilos secundarios mantienen el `X-Trace-Id` original del request.
- **Arquitectura Física y Distribuida Sellada**: Todas las decisiones relacionales (PostgreSQL, ShedLock, Outbox, JWT, Idempotencia) quedan completamente especificadas para la posterior fase física.

### IA utilizada
- Diagnóstico crítico de casos borde temporales y diseño de TaskDecorator para SLF4J MDC.
- Modelado de contratos distribuidos para ShedLock y Transactional Outbox.
- Auditoría automatizada de cobertura JaCoCo y no-regresión multi-módulo.

### Verificación humana
- [x] Verificada la lógica de `fechaRegistro` y prevención de falsos positivos en `InactividadDonaciones`.
- [x] Verificado el blindaje de `MisionRacha` ante eventos fuera de orden.
- [x] Verificada la propagación del contexto `MDC` en `AsyncConfig`.
- [x] Verificado el desempate determinista por donaciones mensuales en `GestorDeRankings`.
- [x] Verificadas las secciones 11.3 a 16 añadidas en `decisiones_futuras_en_oleada_10.md`.
- [x] 185 tests en verde en `incentivos-service` (99.00% cobertura de líneas).
- [x] 7/7 módulos en verde en el reactor multi-módulo (`mvn clean test`).
- [x] Formato Spotless 100% compliant (`mvn spotless:check`).
- [x] `diagrama-de-clases-incentivos.puml`, `oleadas-refactor.md` y `plan-refactor-incentivos.md` 100% sincronizados y sellados.

---

## Oleada 14: Centralización de Domain Events (`AgregadoConEventos`), Polimorfismo en Misiones, Tell Don't Ask y Normalización de Fixtures

### Problema
Tras el merge de `E4_refactor` (incorporando las Oleadas 14 y 15 de `donaciones-service` y la abstracción centralizada `AgregadoConEventos` en `common-lib`), se identificaron deudas arquitectónicas residuales y oportunidades de alineación en `incentivos-service`:
1. **Duplicación de Infraestructura de Domain Events**: `DonanteIncentivos` implementaba `AggregateRoot` directamente y mantenía una lista mutable `domainEvents` con métodos manuales de snapshot y limpieza, además de usar el modificador `transient` en una entidad de dominio no serializable.
2. **Violación de Polimorfismo y Uso de `instanceof` en Misiones**: `DonanteIncentivos.verificarRachas` utilizaba `m instanceof MisionRacha` y casteo manual `(MisionRacha) m` en lugar de despachar la verificación de vigencia de forma polimórfica a la jerarquía de `Mision`.
3. **Violación de *Tell, Don't Ask* en Métricas e Inactividad**: `GestorDeRankings` penetraba en la entidad embebida `Metricas` (`d.getMetricas().donacionesEnMes(periodo)`), e `InactividadDonaciones` duplicaba lógica ternaria para resolver la fecha de referencia entre `donante.getMetricas().getUltimaDonacion()` y `donante.getFechaRegistro()`. Asimismo, `MisionCompletitud` presentaba el atributo `categoriasdonadas` en minúsculas.
4. **Sufijo `*Test` Incorrecto en Clases Fixture**: Las clases de ayuda y Object Mothers en `grupo5.incentivos.fixtures` (`DonanteIncentivosMotherTest`, `EventoDonacionMotherTest`, `MisionMotherTest`, `RankingMensualMotherTest`, `IncentivosFixturesTest`) estaban nombradas con sufijo `*Test`, provocando que Surefire y SonarCloud las ejecutasen como suites de test vacías (0 tests ejecutados).

### Evidencia
- `DonanteIncentivos.java`: Declaraba `private final transient List<EventoDonanteIncentivos> domainEvents` y bucle con `m instanceof MisionRacha`.
- `GestorDeRankings.java:29`: `(DonanteIncentivos d) -> d.getMetricas().donacionesEnMes(periodo)`.
- `InactividadDonaciones.java:40, 48`: Ternarios duplicados penetrando en `donante.getMetricas().getUltimaDonacion()`.
- `MisionCompletitud.java:14`: `private final Set<String> categoriasdonadas`.
- `src/test/java/grupo5/incentivos/fixtures/`: 5 archivos nombrados con sufijo `*Test.java`.

### Objetivo
1. **Heredar de `AgregadoConEventos`**: Extender `AgregadoConEventos<EventoDonanteIncentivos>` en `DonanteIncentivos`, eliminando la lista `domainEvents` local, los métodos duplicados y el modificador `transient`.
2. **Polimorfismo en `Mision` (*Tell, Don't Ask*)**: Declarar `public void verificarVigencia(YearMonth mesActual)` con implementación vacía por defecto en `Mision` y sobreescribirla en `MisionRacha`. Refactorizar `DonanteIncentivos.verificarRachas` para iterar y delegar polimórficamente sin `instanceof` ni casteos.
3. **Enriquecer `DonanteIncentivos`**: Incorporar `donacionesEnMes(YearMonth)` y `fechaUltimaActividad()` en `DonanteIncentivos`, actualizando `GestorDeRankings` e `InactividadDonaciones`. Normalizar `MisionCompletitud.categoriasDonadas`.
4. **Normalizar Fixtures**: Renombrar las 5 clases fixture a `*Mother` / `IncentivosFixtures` y actualizar el 100% de los tests consumidores.
5. **Añadir Tests de Caracterización**: Cobertura para los nuevos métodos del agregado y para la preservación de estado en misiones no-racha durante la verificación de vigencia.

### Fuera de scope
- Modificaciones en endpoints REST, DTOs de entrada/salida o contratos externos.
- Persistencia física relacional JPA / PostgreSQL (fase posterior).

### Tests / Verificación
- **Nuevas Pruebas Unitarias Agregadas en `DonanteIncentivosTest`**:
  - `donacionesEnMes_deberiaRetornarCantidadCorrectaPorPeriodo`: ✅ Valida delegación de cómputo mensual.
  - `fechaUltimaActividad_sinDonaciones_deberiaRetornarFechaRegistro`: ✅ Valida fallback a fecha de alta.
  - `fechaUltimaActividad_conDonacion_deberiaRetornarFechaUltimaDonacion`: ✅ Valida fecha de última actividad.
  - `verificarRachas_conMisionesNoRacha_noDebeAlterarEstado`: ✅ Valida despacho polimórfico seguro y preservación de progreso.
- **Resultados de Ejecución**:
  - `incentivos-service`: ✅ **189 tests ejecutados, 0 fallos, 0 errores, 0 omitidos** (+4 tests, `BUILD SUCCESS`).
  - Reactor Completo (7 módulos): ✅ **7/7 módulos en verde** (`BUILD SUCCESS`).
  - Spotless: ✅ **100% compliant** (`mvn spotless:check`).
  - Barrido de Fixtures: ✅ **0 clases con sufijo `*Test` en `fixtures/`**.
  - Barrido de `instanceof`: ✅ **0 `instanceof` en `models/entities/`**.

### Diseño resultante
- **Arquitectura DDD Pura y Centralizada**: `DonanteIncentivos` reutiliza la abstracción estándar `AgregadoConEventos` de `common-lib`.
- **Modelo de Dominio Polimórfico y Rico**: La verificación temporal de misiones es polimórfica y desacoplada de tipos concretos; las interacciones con métricas e inactividad se realizan mediante métodos de negocio del Aggregate Root (*Tell, Don't Ask*).
- **Convenciones de Testing Canónicas**: Las Object Mothers y Fixtures se encuentran estrictamente desacopladas de las suites de prueba ejecutables.

### IA utilizada
- Diagnóstico post-merge de consistencia contra `common-lib` y `donaciones-service`.
- Refactorización orientada a polimorfismo para erradicación de `instanceof`.
- Automatización de renombrado de fixtures y actualización masiva de referencias.
- Verificación continua con Maven, Spotless y análisis estático.

### Verificación humana
- [x] Verificada la herencia `DonanteIncentivos extends AgregadoConEventos<EventoDonanteIncentivos>`.
- [x] Verificado el método polimórfico `verificarVigencia` en `Mision` y `MisionRacha`.
- [x] Verificada la eliminación total de `instanceof` en `models/entities/`.
- [x] Verificada la delegación *Tell, Don't Ask* en `GestorDeRankings` e `InactividadDonaciones`.
- [x] Verificado el renombrado de fixtures (`DonanteIncentivosMother`, `EventoDonacionMother`, `MisionMother`, `RankingMensualMother`, `IncentivosFixtures`).
- [x] 189 tests en verde en `incentivos-service` y 7/7 módulos en verde en el reactor multi-módulo.
- [x] Formato Spotless 100% compliant (`mvn spotless:check`).
