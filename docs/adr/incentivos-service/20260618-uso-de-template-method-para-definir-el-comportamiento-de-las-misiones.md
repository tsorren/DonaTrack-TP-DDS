# Uso de Template Method para definir el comportamiento de las misiones
- Status: proposed
- Date: 2026-06-18
- Deciders: Decisión Grupal

## Contexto y Problema
Las misiones comparten una estructura común pero cada tipo tiene su propia lógica de ejecución.
Es necesario garantizar que todos los tipos de misión respeten el mismo contrato sin duplicar código.

## Alternativas Consideradas
* Template Method en clase Mision

## Resultado de la Decisión

Alternativa elegida: "Template Method en clase Mision"

Justificación:
Permite que cada tipo de misión encapsule su propia lógica sin modificar el servicio, facilitando la extensibilidad.

### Consecuencias Positivas
* Mayor cohesión y extensibilidad, agregar un tipo de misión nuevo no requiere tocar código existente

### Consecuencias Negativas
* Las subclases dependen de la estructura definida por Mision, lo que reduce flexibilidad si el algoritmo base cambia

### Validación

Verificar que al agregar un nuevo tipo de misión solo sea necesario crear una subclase sin modificar el servicio ni la superclase.

## Análisis de Alternativas

### Template Method en clase Mision

La superclase Mision define el esqueleto del algoritmo y cada subclase concreta implementa los pasos específicos

#### Pros
* Evita duplicación de código entre tipos de misión
* Garantiza consistencia en la estructura de ejecución
* Agregar un nuevo tipo solo requiere implementar los métodos abstractos

#### Contras
* Las subclases quedan acopladas a la estructura de la superclase
