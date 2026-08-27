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
