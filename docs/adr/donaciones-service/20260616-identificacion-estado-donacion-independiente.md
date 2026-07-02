# Identificación del estado actual de una DonacionIndependiente sin instanceof
- Status: accepted
- Date: 2026-06-16
- Deciders: Tadeo Sorrentino, Sofia Deane
- Tags: patrones-de-diseño, estado, donaciones-independientes

## Contexto y Problema
`DonacionIndependienteRepository` necesita filtrar donaciones que estén en un estado particular (por ejemplo, "en depósito"). La implementación original usaba `instanceof EnDeposito`, que es una construcción específica de Java que no se puede trasladar a otro lenguaje y rompe el encapsulamiento: el que filtra necesita conocer la clase concreta del estado. ¿Cómo identificar el estado actual de una `DonacionIndependiente` sin depender de la clase concreta?

## Atributos de Calidad y Drivers de Decisión
* Independencia del lenguaje: la lógica no debe depender de mecanismos de reflexión propios de Java
* Encapsulamiento: quien filtra no debería conocer los estados concretos
* Mantenibilidad: agregar un estado nuevo no debería requerir cambios en múltiples lugares ni permitir inconsistencias silenciosas
* Cohesión: la pregunta "¿en qué estado estoy?" debería responderse desde la propia entidad o estado

## Alternativas Consideradas
* `instanceof` sobre el estado concreto
* Enum `TipoEstadoDonacion` con `getTipo()` en la interfaz `EstadoDonacion`
* Método booleano en `DonacionIndependiente` respaldado por el enum (`estaEnDeposito()`)
* Método booleano en la interfaz `EstadoDonacion` con default `false`, override en cada estado concreto

## Resultado de la Decisión

Alternativa elegida: "Enum `TipoEstadoDonacion` con `getTipo()` en la interfaz `EstadoDonacion`"

Justificación:
Permite filtrar fácilmente en queries al repositorio.

### Validación

La implementación elegida debe permitir que `DonacionIndependienteRepository.findEnDeposito()` filtre correctamente sin importar `EnDeposito` ni ninguna otra clase de estado concreta. Se valida que agregar un nuevo estado no requiera cambios fuera del propio estado y que el compilador detecte implementaciones incompletas.
