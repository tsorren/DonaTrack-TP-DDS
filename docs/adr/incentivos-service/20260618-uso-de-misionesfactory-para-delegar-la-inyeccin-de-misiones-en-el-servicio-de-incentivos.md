# Uso de MisionesFactory para delegar la inyección de misiones en el servicio de incentivos
- Status: accepted
- Date: 2026-06-18
- Deciders: Bernardo Estigarribia, Miranda Rossi

## Contexto y Problema
El método registrarDonante necesita instanciar misiones, lo que le agrega responsabilidades que no le corresponden. Se necesita delegar esa tarea para mantener el método cohesivo.

## Alternativas Consideradas
* MisionesFactory para inyección de misiones
* Inyección de misiones directamente en registrarDonante

## Resultado de la Decisión

Alternativa elegida: "MisionesFactory para inyección de misiones"

Justificación:
Delegar la creación de misiones a una Factory respeta SRP, manteniendo registrarDonante enfocado en su responsabilidad principal y haciendo el código más mantenible y extensible.

## Análisis de Alternativas

### MisionesFactory para inyección de misiones

El servicio de incentivos delega en MisionesFactory la responsabilidad de instanciar e inyectar las misiones, desacoplando esa lógica del método registrarDonante

#### Pros
* Reduce la responsabilidad del método registrarDonante
* Centraliza la lógica de creación de misiones en un único lugar
* Facilita agregar nuevos tipos de misión sin tocar el servicio

#### Contras
* Agrega una clase extra al diseño

### Inyección de misiones directamente en registrarDonante

El método se encarga él mismo de instanciar y manejar las misiones además de registrar al donante

#### Pros
* Menos clases en el diseño

#### Contras
* El método acumula demasiadas responsabilidades
