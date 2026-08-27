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
   - [`DonanteIncentivosMotherTest.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/test/java/grupo5/incentivos/fixtures/DonanteIncentivosMotherTest.java): métodos canónicos para donantes colaboradores, con misiones activas, rachas y misiones completadas en meses específicos.
   - [`MisionMotherTest.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/test/java/grupo5/incentivos/fixtures/MisionMotherTest.java): métodos para las 4 subclases de `Mision` (`racha`, `exitosas`, `completitud`, `habilDonador`) con y sin insignias.
   - [`RankingMensualMotherTest.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/test/java/grupo5/incentivos/fixtures/RankingMensualMotherTest.java): creación de rankings mensuales vacíos o poblados con $N$ entradas.
   - [`EventoDonacionMotherTest.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/test/java/grupo5/incentivos/fixtures/EventoDonacionMotherTest.java): eventos parametrizados por fecha, categorías y cantidad de bienes.
   - [`IncentivosFixturesTest.java`](file:///c:/IdeaProjects/DonaTrack-TP-DDS/incentivos-service/src/test/java/grupo5/incentivos/fixtures/IncentivosFixturesTest.java): fábrica para requests de entrada (`RegistrarDonanteRequest`, `NuevaDonacionRequest`, `DonacionExitosaRequest`, `ModificarDonanteRequest`).
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
