# Gestion de Necesidades y Periodos
- Status: accepted
- Date: 2026-06-09
- Deciders: Decisión Grupal

## Contexto y Problema
Las necesidades de las entidades beneficiarias estaban modeladas de forma simple. En particular, las necesidades recurrentes (consumos periódicos de bienes) no contaban con un soporte adecuado para modelar el tiempo y sus ciclos. El sistema registraba la recurrencia pero no guardaba de forma histórica ni aislada el progreso de donaciones por cada ciclo (semana, mes, etc.), impidiendo auditar si una entidad satisfizo su necesidad en un periodo particular.

Para corregir esto, se requería refactorizar el modelo de necesidades para soportar de manera flexible tanto demandas extraordinarias como demandas recurrentes parametrizables en el tiempo.

## Atributos de Calidad y Drivers de Decisión
* Trazabilidad

## Alternativas Consideradas
* Modelo Simple de Necesidad Recurrente
* Modelo con Subclases y Periodos Independientes

## Resultado de la Decisión

Alternativa elegida: "Modelo con Subclases y Periodos Independientes"

Justificación:
Esta alternativa permite modelar las necesidades con herencia y polimorfismo limpios. 

Necesidad Extraordinaria: Se satisface mediante donaciones acumuladas directas.

Necesidad Recurrente: Contiene un atributo periodo y una lista de periodos históricos.

Cada PeriodoNecesidad actúa como un contenedor aislado que almacena su fecha de finalización, y el listado específico de donaciones asignadas a ese periodo en particular.

Planificador de Necesidades: Para automatizar la rotación de ciclos, se introdujo el componente PlanificadorDeNecesidades con una tarea programada (@Scheduled), el cual se ejecuta diariamente de forma automática, evalúa si el periodo actual ha vencido y, en su caso, cierra el ciclo y genera un nuevo periodo.

## Análisis de Alternativas

### Modelo Simple de Necesidad Recurrente

Mantener un único objeto de necesidad recurrente con acumuladores y fechas modificables, perdiendo el histórico de ciclos anteriores una vez vencidos.

#### Contras
* Alto acoplamiento entre necesidades y segmentación
* Imposibilita segmentación cuando aún no hay necesidades

### Modelo con Subclases y Periodos Independientes

Modelar las necesidades mediante una jerarquía basada en una clase abstracta Necesidad. Para las necesidades recurrentes, delegar el control del tiempo en una clase PeriodoNecesidad y orquestar su rotación a través de un Planificador de Necesidades.

#### Pros
* Permite la segmentación aún cuando no existen necesidades
* Posibilita la extracción de la cantidad necesaria para satisfacer una necesidad

#### Contras
* Aumenta la complejidad de la lógica de las donaciones independientes y sus items
