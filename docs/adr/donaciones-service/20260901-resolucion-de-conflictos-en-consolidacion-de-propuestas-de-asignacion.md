# Resolución de Conflictos en Consolidación de Propuestas de Asignación

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: donaciones, algoritmos, matching, heuristica, dominio

## Contexto y Problema

En `donaciones-service`, el proceso de emparejamiento inteligente de stock disponible contra necesidades extraordinarias utiliza dos heurísticas complementarias:
1. `AlgoritmoCompatibilidadSemantica`: Empareja bienes normalizados por afinidad de subcategorías y atributos específicos.
2. `AlgoritmoPrioridadSubAtendidos`: Evalúa el historial de asistencia de las entidades beneficiarias y prioriza aquellas organizaciones que presentan un menor porcentaje de cobertura histórica.
Al ejecutar ambos algoritmos sobre el mismo inventario y consolidar los resultados en el método `consolidar()`, surgen situaciones de conflicto donde **ambos algoritmos proponen donaciones para satisfacer la misma necesidad**. Se requiere documentar formalmente la regla de resolución de colisiones y su justificación de impacto social.

## Atributos de Calidad y Drivers de Decisión

* **Justicia y Equidad Social:** Garantizar que los recursos escasos de donaciones se distribuyan atendiendo a las entidades con mayor vulnerabilidad o menor asistencia previa.
* **Determinismo:** El algoritmo de consolidación debe arrojar un resultado reproducible y sin ambigüedad ante el mismo stock y demandas.
* **Mantenibilidad:** Evitar mezclas de código complejas o heurísticas impredecibles en el método de fusión de propuestas.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Hallazgo §982 de la [Auditoría Final del Proyecto](../arquitectura/diseno/auditoria-final-proyecto.md) y diseño de `donaciones-service`.
* **Hallazgo:** La auditoría final observó que `consolidar()` descartaba silenciosamente las propuestas semánticas a favor de las de prioridad cuando ambas coincidían en una necesidad, señalando: *"Esta decisión no está documentada en comentarios ni ADR"*.

## Alternativas Consideradas

* **Prevalencia Determinista de Prioridad Subatendidos (Inteligencia Social):** Ante colisiones sobre la misma necesidad, `consolidar()` selecciona exclusivamente la propuesta generada por `AlgoritmoPrioridadSubAtendidos` y descarta la de `AlgoritmoCompatibilidadSemantica`.
* **División Proporcional Salmónica (Fragmentación 50/50):** Dividir el stock disponible por la mitad entre ambas sugerencias.
* **Prevalencia Semántica (Mayor Ajuste Técnico):** Priorizar el emparejamiento semántico exacto de bienes por sobre la condición de la entidad.

## Resultado de la Decisión

Alternativa elegida: "Prevalencia Determinista de Prioridad Subatendidos (Inteligencia Social)"

Justificación:
El objetivo primordial de la plataforma DonaTrack es maximizar el impacto social de la ayuda comunitaria. Si ambos algoritmos coinciden en que una necesidad puede ser cubierta, el criterio de prioridad social aporta un juicio ético superior frente al emparejamiento puramente léxico o de catálogo. Descartar la propuesta semántica a favor de la prioritaria asegura que las entidades más necesitadas reciban los bienes de manera inmediata.

### Consecuencias Positivas

* Asignación socialmente justa y alineada con los valores rectores de las organizaciones benéficas.
* Regla determinista, simple de auditar y comprender sin ramas condicionales opacas.
* Cero riesgo de sobre-asignar el mismo lote de stock a dos entidades distintas.

### Consecuencias Negativas

* La propuesta semántica descartada podría haber tenido un ajuste ligeramente más fino en atributos secundarios de los bienes (ej: marca o embalaje).

### Validación

Se valida mediante tests unitarios dedicados en `GestorPropuestasDeAsignacionTest`:
1. Fixture con una misma necesidad cubierta por ambos algoritmos.
2. Verificación de que la propuesta consolidada final retenga el identificador y cálculo del `AlgoritmoPrioridadSubAtendidos`.

## Análisis de Alternativas

### Prevalencia de Prioridad Subatendidos

#### Pros
* Enfoque ético y solidario coherente con el negocio de donaciones.
* Criterio determinista y claro.

#### Contras
* Descarte de la alternativa semántica sin negociación intermedia.

### División Proporcional 50/50

#### Pros
* Aparenta complacer a ambas heurísticas.

#### Contras
* Fragmenta lotes físicos pequeños (ej: una heladera no se puede dividir al 50%).
* Complica innecesariamente la logística de transporte.

### Prevalencia Semántica

#### Pros
* Optimiza la coincidencia técnica de especificaciones de producto.

#### Contras
* Puede perpetuar la inequidad, favoreciendo a entidades grandes y marginando a comedores periféricos subatendidos.