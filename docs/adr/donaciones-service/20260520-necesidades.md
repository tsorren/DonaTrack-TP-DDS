# Necesidades

- Status: superseded by [20260609-gestion-de-necesidades-y-periodos.md](./20260609-gestion-de-necesidades-y-periodos.md)
- Date: 2026-05-20
- Deciders: Decisión Grupal

> **Nota de Evolución y Superación Histórica (2026-06-09):**
> Esta decisión inicial de modelado (adoptada para la Entrega 1) carecía de soporte para la dimensión temporal y los ciclos de renovación en demandas periódicas. Fue formalmente **superada y reemplazada** por [20260609-gestion-de-necesidades-y-periodos.md](./20260609-gestion-de-necesidades-y-periodos.md), la cual introdujo la entidad `PeriodoNecesidad` y el componente programado `PlanificadorDeNecesidades`.

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

Esta clase contiene los atributos subcategoria (Clase), cantidadNecesitada, descripcion y una lista de donaciones
asignadas. Aplica el patrón template method en estaSatisfecha() que determina si la cantidad acumulada (metodo abstacto)
es mayor a la cantidad necesitada. Luego NecesidadExtraordinaria y NecesidadRecurrente implementan el metodo abstracto
con lógica y atributos especificos.

#### Pros

* Estandariza las necesidades
* Permite extender el dominio a nuevos tipos de necesidades fácilmente, solo se necesita determinar la lógica de
  cantidadAcumulada

