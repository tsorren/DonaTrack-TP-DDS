# Template Method en AlgoritmoAsignacion

- Status: accepted
- Date: 2026-06-15
- Deciders: Decision Grupal
- Tags: matchmaking, patrones-de-diseño, algoritmos

## Contexto y Problema

El sistema necesita ejecutar algoritmos de asignación de donaciones que comparten la misma lógica de orquestación: iterar necesidades en algún orden, filtrar donaciones candidatas, armar propuestas y registrar reservas temporales. Sin embargo, la lógica que varía entre algoritmos es exclusivamente cuáles donaciones se consideran candidatas para una necesidad dada.

¿Cómo estructurar los algoritmos para que el flujo de ejecución esté centralizado y solo las partes que varían queden delegadas a cada implementación concreta?

## Atributos de Calidad y Drivers de Decisión

* Mantenibilidad: evitar duplicación del flujo de orquestación entre algoritmos
* Extensibilidad: agregar un nuevo algoritmo no debería requerir reimplementar la lógica de propuestas y reservas
* Cumplimiento de restricciones de la cátedra: aplicación de patrones de diseño conocidos

## Alternativas Consideradas

* Cada algoritmo implementa `ejecutar()` de forma independiente
* Template Method: `ejecutar()` concreto en la clase padre, `filtrarDonaciones()` abstracto
* Template Method con múltiples métodos abstractos obligatorios

## Resultado de la Decisión

Alternativa elegida: "Template Method: `ejecutar()` concreto en la clase padre, `filtrarDonaciones()` abstracto"

Justificación:
El Template Method permite que `AlgoritmoAsignacion.ejecutar()` defina el flujo completo (iterar, filtrar, armar propuesta, registrar reservas, limitar a 10) sin exponerlo a las subclases. Cada algoritmo concreto solo necesita implementar `filtrarDonaciones()`. El método `ordenarNecesidades()` se provee como hook con implementación por defecto (devuelve la lista sin modificar), permitiendo que algoritmos como `AlgoritmoPrioridadSubAtendidos` lo sobreescriban opcionalmente.

### Consecuencias Positivas

* Toda la lógica de propuestas, fragmentaciones y reservas vive en un único lugar
* Agregar un nuevo algoritmo requiere únicamente implementar `filtrarDonaciones()`
* El límite de 10 propuestas y la integración con `StockDeDonaciones` están garantizados para todos los algoritmos

### Consecuencias Negativas

* Las subclases no pueden alterar el flujo de `ejecutar()` sin sobreescribir el método completo
* El acoplamiento entre `AlgoritmoAsignacion` y `StockDeDonaciones` vive en la clase abstracta

### Validación

Los tests unitarios de `AlgoritmoPrioridadSubAtendidosTest` y `AlgoritmoCompatibilidadSemanticaTest` verifican que cada subclase filtra y ordena correctamente. La integración del flujo completo está cubierta por `StockDeDonacionesTest`.

## Análisis de Alternativas

### Cada algoritmo implementa `ejecutar()` de forma independiente

Cada clase concreta reimplementa desde cero el loop, la creación de propuestas y el tracking de reservas.

#### Pros

* Máxima flexibilidad para que cada algoritmo defina su propio flujo

#### Contras

* Duplicación masiva de lógica de orquestación entre algoritmos
* Un cambio en cómo se crean propuestas o se registran reservas requiere modificar todas las clases

### Template Method: `ejecutar()` concreto en la clase padre, `filtrarDonaciones()` abstracto

`AlgoritmoAsignacion` define el flujo completo. Las subclases implementan solo `filtrarDonaciones()` y, opcionalmente, `ordenarNecesidades()`.

#### Pros

* Flujo centralizado y sin duplicación
* Las subclases son simples y enfocadas en una sola responsabilidad
* Estructura organizada, legible y clara

#### Contras

* Las subclases no pueden alterar el orden de los pasos del algoritmo

### Template Method con múltiples métodos abstractos obligatorios

Además de `filtrarDonaciones()`, se exigen `ordenarNecesidades()` y potencialmente otros métodos como `calcularLimite()`.

#### Pros

* Mayor control explícito sobre cada paso del algoritmo

#### Contras

* Fuerza a cada subclase a implementar comportamientos que podrían tener una respuesta por defecto razonable
* Complejiza innecesariamente al momento de crear nuevos algoritmos simples o completamente distintos
