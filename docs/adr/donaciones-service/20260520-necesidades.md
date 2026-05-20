# Necesidades
- Status: proposed
- Date: 2026-05-20
- Deciders: Decisión Grupal

## Contexto y Problema
Queremos modelar las necesidades de las entidades beneficiarias siguiendo los principios SOLID

## Atributos de Calidad y Drivers de Decisión
* Extensibilidad
* Flexibilidad

## Alternativas Consideradas
* Clase Abstracta Necesidad

## Resultado de la Decisión

Alternativa elegida: "Clase Abstracta Necesidad"

## Análisis de Alternativas

### Clase Abstracta Necesidad

Esta clase contiene los atributos subcategoria (Clase), cantidadNecesitada, descripcion y una lista de donaciones asignadas. Aplica el patrón template method en estaSatisfecha() que determina si la cantidad acumulada (metodo abstacto) es mayor a la cantidad necesitada. Luego NecesidadExtraordinaria y NecesidadRecurrente implementan el metodo abstracto con lógica y atributos especificos.

#### Pros
* Estandariza las necesidades
* Permite extender el dominio a nuevos tipos de necesidades fácilmente, solo se necesita determinar la lógica de cantidadAcumulada
