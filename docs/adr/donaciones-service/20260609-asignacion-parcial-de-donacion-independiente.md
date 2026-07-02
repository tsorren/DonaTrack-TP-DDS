# Asignación parcial de donación independiente
- Status: accepted
- Date: 2026-06-09
- Deciders: Decisión Grupal

## Contexto y Problema
Al segmentar las donaciones, es posible que una entidad necesite solo una parte de una donación independiente. Por ejemplo se donan 100 sillas y una entidad beneficiaria requiere 40 sillas.
¿Cómo podemos asignar a esa entidad beneficiaria unicamente la cantidad necesaria?

## Alternativas Consideradas
* Segmentación en base a necesidades
* Fragmentación de donaciones

## Resultado de la Decisión

Alternativa elegida: "Fragmentación de donaciones"

Justificación:
Desacoplamos la segmentación de la asignación de donaciones

## Análisis de Alternativas

### Segmentación en base a necesidades

La segmentación solo se ejecuta cuando hay necesidades existentes y crea donaciones independientes en base a las cantidades requeridas

#### Contras
* Alto acoplamiento entre necesidades y segmentación
* Imposibilita segmentación cuando aún no hay necesidades

### Fragmentación de donaciones

Las donaciones independientes que aún no estén asignadas pueden ser fragmentadas, generando una nueva donación independiente con la cantidad necesaria

#### Pros
* Permite la segmentación aún cuando no existen necesidades
* Posibilita la extracción de la cantidad necesaria para satisfacer una necesidad

#### Contras
* Aumenta la complejidad de la lógica de las donaciones independientes y sus items
