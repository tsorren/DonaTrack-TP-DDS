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
