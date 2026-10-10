# [DTI-06] Desacoplamiento de Referencias Directas entre Agregados por UUID

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: deuda-tecnica, dti-06, ddd, agregados, persistencia, jpa

## Contexto y Problema

Uno de los principios inviolables de Domain-Driven Design (DDD) establece que **los agregados solo pueden referenciarse entre sí mediante su identificador global (`UUID`)**, y nunca mediante referencias directas a objetos en memoria. Actualmente, en `donaciones-service`, la clase `NecesidadExtraordinaria` mantiene una colección directa `List<DonacionIndependiente>` en memoria para registrar las donaciones que satisfacen la necesidad. Esto acopla dos Aggregate Roots independientes, provoca problemas de carga masiva de grafos de objetos (*eager loading* involuntario) y creará conflictos severos de mapeo relacional al migrar a JPA/Hibernate en la Entrega 2.

## Atributos de Calidad y Drivers de Decisión

* **Bajo Acoplamiento (DDD):** Proteger los límites transaccionales de cada aggregate root.
* **Escalabilidad y Rendimiento:** Evitar traer colecciones de donaciones pesadas a memoria heap cada vez que se consulta una necesidad.
* **Persistencia Limpia en JPA:** Permitir mapeos relacionales normalizados sin relaciones forzadas `@ManyToMany` bidireccionales entre agregados.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Registrado en [docs/adr/DEUDA_TECNICA.md](../DEUDA_TECNICA.md) como **DTI-06**; Modelo de Agregados DDD ([aggregates-donaciones.md](../../arquitectura/aggregates-donaciones.md) §2.6).
* **Hallazgo:** Durante la auditoría de DDD de `donaciones-service`, se constató que mientras `DonacionIndependiente` sí referencia correctamente a `necesidadId` por UUID, `NecesidadExtraordinaria` rompe la simetría conteniendo objetos directos de `DonacionIndependiente`.

## Alternativas Consideradas

* **Referencias Débiles por Identificador (`List<UUID>` o Consulta de Repositorio):**
  1. Reemplazar la colección en `NecesidadExtraordinaria` por `List<UUID> donacionesAsignadasIds` o, preferentemente, eliminar la colección directa y resolver el cruce mediante consultas especializadas en el repositorio (`donacionesRepository.findByNecesidadId(UUID necesidadId)`).
  2. Actualizar el cálculo de progreso y cantidades de la necesidad para que reciba los datos de aportes como parámetros de dominio o agregaciones escalares.
* **Mantener Colección de Entidades Directas (`List<DonacionIndependiente>`):** Conservar los punteros a objetos en memoria.

## Resultado de la Decisión

Alternativa elegida: "Referencias Débiles por Identificador (`List<UUID>` o Consulta de Repositorio)"

Justificación:
Garantiza la pureza del diseño DDD y simplifica drásticamente la migración a JPA. Al no mantener referencias cruzadas directas, cada agregado puede persistirse, bloquearse o cachearse de forma totalmente independiente, eliminando productos cartesianos y excepciones de concurrencia al guardar necesidades.

### Consecuencias Positivas

* Respeto estricto a las invariantes de Aggregate Roots de Evans.
* Mapeo relacional en PostgreSQL trivial: una columna `necesidad_id UUID` en la tabla `donacion_independiente` actúa como foreign key débil sin requerir colecciones pesadas en `necesidad`.
* Optimización radical del consumo de memoria heap en consultas masivas de necesidades.

### Consecuencias Negativas

* Requiere que la capa de aplicación consulte al repositorio de donaciones cuando se requiera el detalle expandido de donaciones de una necesidad para armar un DTO de respuesta.

### Validación

Se valida mediante:
1. `NecesidadExtraordinaria.java` no importa ni contiene referencias de tipo `DonacionIndependiente`.
2. Tests unitarios verificando que el progreso de una necesidad se calcule correctamente mediante DTOs o identificadores escalares.

## Análisis de Alternativas

### Referencias Débiles por UUID / Repositorio

#### Pros
* Arquitectura escalable y desacoplada.
* Transición natural e inmediata a JPA/Hibernate.

#### Contras
* Requiere orquestación explícita en Application Service para consultas compuestas.

### Colección de Objetos Directos

#### Pros
* Fácil navegación en memoria en pruebas unitarias primitivas.

#### Contras
* Violación estructural de DDD.
* Pesadilla de rendimiento y mapeo relacional en JPA (riesgo de `LazyInitializationException` y ciclos infinitos en serialización JSON).