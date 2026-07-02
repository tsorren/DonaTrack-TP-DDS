# Manejo de donaciones durante la ejecución del algoritmo

- Status: accepted
- Date: 2026-06-15
- Deciders: Sofia Deane
- Tags: matchmaking, algoritmos, dominio

## Contexto y Problema

Durante la ejecución de un algoritmo de asignación, varias necesidades pueden competir por las mismas donaciones. El algoritmo construye propuestas que todavía no están confirmadas, por lo que no se puede modificar el estado real de las donaciones hasta que una propuesta sea aprobada. Sin embargo, el algoritmo necesita saber cuántas unidades de cada donación quedan disponibles a medida que genera propuestas sucesivas dentro de la misma corrida.

¿Cómo rastrear la disponibilidad de donaciones durante la ejecución del algoritmo sin modificar prematuramente el estado de dominio?

## Atributos de Calidad y Drivers de Decisión

* Consistencia: el estado de `DonacionIndependiente` no debe cambiar hasta que una propuesta sea confirmada
* Correctitud: el algoritmo no debe proponer la misma unidad de donación para dos necesidades distintas
* Separación de responsabilidades: la lógica de tracking temporal no debe contaminar la entidad de dominio

## Alternativas Consideradas

* Mutar `DonacionIndependiente.cantidad` durante el algoritmo y revertir si la propuesta es rechazada
* `StockDeDonaciones`: clase local que replica las cantidades disponibles para la duración del algoritmo
* Clonar todas las `DonacionIndependiente` al inicio de la corrida

## Resultado de la Decisión

Alternativa elegida: "`StockDeDonaciones`: clase local que replica las cantidades disponibles para la duración del algoritmo"

Justificación:
`StockDeDonaciones` encapsula un `Map<DonacionIndependiente, Integer>` que registra cuántas unidades de cada donación quedan sin reservar en la corrida actual. Se crea al inicio de `AlgoritmoAsignacion.ejecutar()` y se descarta al terminar. Esto garantiza que las entidades de dominio permanezcan intactas hasta la confirmación de una propuesta, y evita tener que clonar objetos o implementar lógica de rollback.

### Consecuencias Positivas

* `DonacionIndependiente` no cambia de estado hasta `Propuesta.confirmar()`
* El tracking temporal queda contenido en una clase con responsabilidad única
* Si el algoritmo falla o no se confirma ninguna propuesta, el estado de dominio queda intacto

### Consecuencias Negativas

* Las cantidades en `StockDeDonaciones` y en las `DonacionIndependiente` pueden divergir temporalmente dentro de una corrida, lo que requiere que todo el acceso durante el algoritmo pase por el stock y no por las donaciones directamente

### Validación

`StockDeDonacionesTest` verifica que `disponibles()` excluye correctamente las donaciones agotadas y que `registrarReservas()` descuenta las cantidades de forma precisa.

## Análisis de Alternativas

### Mutar `DonacionIndependiente.cantidad` durante el algoritmo y revertir si la propuesta es rechazada

El algoritmo modifica `getCantidad()` en las donaciones a medida que genera reservas, y las restaura si la propuesta no se confirma.

#### Pros

* No requiere una estructura auxiliar de tracking

#### Contras

* Contamina el estado de dominio con operaciones temporales
* Implementar rollback es complejo y propenso a errores: si el algoritmo lanza una excepción a mitad de camino, el estado queda inconsistente
* Viola la idea de que el estado de `DonacionIndependiente` solo cambia a través de transiciones de estado definidas

### `StockDeDonaciones`: clase local de tracking

Un `Map<DonacionIndependiente, Integer>` inicializado con las cantidades reales al inicio del algoritmo. Se consulta y actualiza durante la corrida sin tocar las entidades.

#### Pros

* Estado de dominio intacto hasta confirmación explícita
* Sin necesidad de rollback
* Responsabilidad de tracking bien delimitada en una clase pequeña

#### Contras

* Requiere que el código del algoritmo use el stock en lugar de consultar directamente las donaciones

### Clonar todas las `DonacionIndependiente` al inicio de la corrida

Se trabaja sobre copias durante el algoritmo y al confirmar se aplican los cambios a las originales.

#### Pros

* El algoritmo puede operar libremente sobre las copias sin riesgo de contaminar el estado real

#### Contras

* Clonar entidades con historial, estado y referencias bidireccionales es costoso y complejo
* Al confirmar, reconciliar las copias con las originales requiere lógica adicional
* Pérdida de identidad de objeto: las propuestas referenciarían copias, no las entidades reales
