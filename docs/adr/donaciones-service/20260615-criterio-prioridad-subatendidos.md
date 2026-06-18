# Criterio de Prioridad en AlgoritmoPrioridadSubAtendidos

- Status: proposed
- Date: 2026-06-15
- Deciders: Sofia Deane
- Tags: matchmaking, algoritmos, prioridad

## Contexto y Problema

`AlgoritmoPrioridadSubAtendidos` ordena las necesidades para que las entidades beneficiarias menos atendidas sean procesadas primero. La pregunta es qué métrica usar para definir "sub-atendida": ¿la cantidad de donaciones recibidas en los últimos 3 meses, o el porcentaje de necesidades satisfechas en ese período?

## Atributos de Calidad y Drivers de Decisión

* Equidad: el criterio debe ser justo entre entidades de distinto tamaño o cantidad de necesidades
* Representatividad: la métrica debe reflejar cuántas de sus necesidades fueron cubiertas, no solo cuántas donaciones recibió
* Trazabilidad: el criterio debe poder calcularse a partir de datos ya existentes en el dominio

## Alternativas Consideradas

* Contar las donaciones recibidas en el último trimestre por entidad
* Calcular el porcentaje de necesidades satisfechas en el último trimestre por entidad

## Resultado de la Decisión

Alternativa elegida: "Calcular el porcentaje de necesidades satisfechas en el último trimestre por entidad"

Justificación:
Una entidad con muchas necesidades puede haber recibido muchas donaciones pero aun así tener la mayoría de sus necesidades insatisfechas. El porcentaje (necesidades satisfechas / total de necesidades del trimestre) normaliza la comparación y captura mejor la situación real de cada entidad, independientemente de su volumen de necesidades.

### Consecuencias Positivas

* El orden es equitativo entre entidades grandes y pequeñas
* La métrica es expresiva: una tasa de 0% significa que ninguna necesidad fue cubierta en el período
* El cálculo usa datos ya disponibles en `NecesidadRepository` sin necesidad de una tabla auxiliar
* Las entidades sin historial reciente se tratan con tasa 0.0 (máxima prioridad), lo que favorece a las nuevas entidades

### Consecuencias Negativas

* Requiere inyectar `NecesidadRepository` en el algoritmo para acceder al historial de todas las necesidades
* El cálculo itera sobre todas las necesidades del repositorio en cada ejecución; puede ser costoso a escala

### Validación

El test `ordenarNecesidades_cuandoUnaEntidadTieneMayorPorcentajeSatisfecho_debeQuedarAlFinal` verifica que una entidad con tasa del 100% queda al final del orden y la de tasa 0% queda primera.

## Análisis de Alternativas

### Contar las donaciones recibidas en el último trimestre por entidad

Se suman las `DonacionIndependiente` asignadas a las necesidades de cada entidad en los últimos 3 meses. La entidad con menos donaciones recibidas tiene mayor prioridad.

#### Pros

* Cálculo simple y directo
* No requiere acceso al repositorio de necesidades históricas

#### Contras

* Injusto entre entidades de distinto tamaño: una entidad con 20 necesidades y 15 donaciones recibidas puede aparecer como "muy atendida" aunque el 25% de sus necesidades sigan sin cubrir
* Una entidad pequeña con 1 necesidad y 1 donación aparece tan atendida como una grande con 10 necesidades y 10 donaciones, aunque la situación sea estructuralmente diferente
* Contar donaciones no equivale a contar necesidades cubiertas: una necesidad puede requerir varias donaciones para satisfacerse

### Calcular el porcentaje de necesidades satisfechas en el último trimestre por entidad

Se itera sobre `NecesidadRepository.findAll()` filtrando por los últimos 3 meses. Por cada entidad se calcula `satisfechas / total`. Las entidades se ordenan de menor a mayor tasa.

#### Pros

* Normaliza la comparación entre entidades con distinto volumen de necesidades
* Usa `estaSatisfecha()` ya definido en el dominio
* El corte de 3 meses evita que eventos históricos muy antiguos distorsionen la prioridad actual

#### Contras

* Necesita acceso a `NecesidadRepository`, lo que introduce una dependencia en el dominio
* Si una entidad no tiene ninguna necesidad en el último trimestre, su tasa es 0.0 por defecto, lo que le otorga prioridad máxima aunque no necesariamente sea la más urgente

## Links

<!-- Refined by [20260615-strategy-gestor-algoritmos.md](20260615-strategy-gestor-algoritmos.md) -->
