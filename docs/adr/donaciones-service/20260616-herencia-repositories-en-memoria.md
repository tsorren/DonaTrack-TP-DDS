# Herencia de BaseRepositoryEnMemoria en los repositories en memoria

- Status: superseded by [../20260901-limites-y-responsabilidades-del-shared-kernel-common-lib.md](../20260901-limites-y-responsabilidades-del-shared-kernel-common-lib.md)
- Date: 2026-06-16
- Deciders: Tadeo Sorrentino, Sofia Deane
- Tags: repositories, patrones-de-diseño, common-lib

> **Nota de Evolución y Superación Histórica (2026-09-01):**
> La implementación inicial obligaba a las entidades de dominio a implementar la interfaz `RecursoDTO`, contaminando el modelo con conceptos de transporte. Esta arquitectura fue formalmente **superada y reemplazada** por la consolidación del Shared Kernel en [20260901-limites-y-responsabilidades-del-shared-kernel-common-lib.md](../20260901-limites-y-responsabilidades-del-shared-kernel-common-lib.md), migrando a `CrudRepositoryEnMemoria<T extends AggregateRoot>` donde las entidades implementan la abstracción pura de dominio `AggregateRoot` (`UUID getId()`).

## Contexto y Problema

Los repositories `DonacionIndependienteRepository`, `NecesidadRepository` y `PropuestaRepository` reimplementaban cada uno su propio mecanismo de almacenamiento en memoria (una `List` privada con lógica de guardado manual). La clase `BaseRepositoryEnMemoria` ya existía en `common-lib` con un `ConcurrentHashMap` y operaciones CRUD genéricas. ¿Cómo evitar esa duplicación y centralizar el almacenamiento en memoria?

## Atributos de Calidad y Drivers de Decisión

* Reutilización: no duplicar la lógica de almacenamiento en cada repository
* Mantenibilidad: cambios en el mecanismo de persistencia en memoria afectan un único lugar
* Consistencia: todos los repositories exponen la misma interfaz base (`findAll`, `findById`, `save`, `deleteById`, etc.)

## Alternativas Consideradas

* Mantener cada repository con su propia `List` privada
* Heredar de `BaseRepositoryEnMemoria<T>`

## Resultado de la Decisión

Alternativa elegida: "Heredar de `BaseRepositoryEnMemoria<T>`"

Justificación:
Los tres repositories no agregaban ninguna lógica diferencial en el almacenamiento; solo reimplementaban lo que ya hace la clase base. Heredar elimina el código duplicado y garantiza que cualquier mejora a `BaseRepositoryEnMemoria` (por ejemplo, reemplazar la `List` por un `ConcurrentHashMap`, o agregar validaciones) beneficia a todos sin modificarlos. Cada repository solo conserva el método de consulta específico de su dominio (`findEnDeposito`, `findInsatisfechas`, `findByActivaTrue`).

Como prerequisito, las entidades `DonacionIndependiente`, `Necesidad` y `Propuesta` implementan la interfaz `RecursoDTO` (campo `UUID id`) para satisfacer el bound genérico `T extends RecursoDTO` de la clase base.

Se agrega además un método de conveniencia `save(T recurso)` a `BaseRepositoryEnMemoria` que delega a `save(null, recurso)`, para mantener la firma de un solo argumento que ya usaban los callers.

### Consecuencias Positivas

* Se elimina la `List baseDeDatosFalsa` y la lógica de guardado duplicada en los tres repositories
* `findAll`, `findById`, `deleteById`, `count` y `deleteAll` quedan disponibles sin reimplementación
* El `AtomicLong` manual de `PropuestaRepository` desaparece; el ID pasa a ser `UUID` generado por la base

### Consecuencias Negativas

* Las entidades deben implementar `RecursoDTO`, lo que agrega un campo `UUID id` a clases de dominio

### Validación

Los tests de algoritmos (`AlgoritmoCompatibilidadSemanticaTest`, `AlgoritmoPrioridadSubAtendidosTest`) siguen pasando ya que no dependen directamente de los repositories. La correcta herencia se verifica que compila sin errores y que cada repository solo declara su método de consulta específico.

## Análisis de Alternativas

### Mantener cada repository con su propia `List` privada

Cada repository gestiona su propio almacenamiento con una `ArrayList` y una comprobación `contains` antes de agregar.

#### Pros

* Independencia total entre repositories
* Sin necesidad de modificar las entidades

#### Contras

* Lógica de almacenamiento duplicada en tres clases
* Cualquier cambio (por ejemplo, usar un `Map` para búsqueda por ID) hay que replicarlo manualmente en los tres
* No se aprovecha la infraestructura ya construida en `common-lib`

### Heredar de `BaseRepositoryEnMemoria<T>`

Cada repository extiende `BaseRepositoryEnMemoria<T>` y solo define sus métodos de consulta específicos.

#### Pros

* Sin duplicación del mecanismo de almacenamiento
* Búsqueda por `UUID` eficiente (O(1) con `ConcurrentHashMap`) sin código adicional
* Coherencia con el resto de la arquitectura de `common-lib`

#### Contras

* Las entidades deben implementar `RecursoDTO`
