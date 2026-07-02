# Procesamiento de Donaciones
- Status: accepted
- Date: 2026-06-09
- Deciders: Decisión Grupal

## Alternativas Consideradas
* Procesamiento Sincrónico
* Procesamiento Asincrónico

## Resultado de la Decisión

Alternativa elegida: "Procesamiento Asincrónico"

Justificación:
Delegamos responsabilidades y separamos el procesamiento intenso para poder responder rápidamente la request del usuario.

## Análisis de Alternativas

### Procesamiento Sincrónico

La donación se segmenta al momento de crearla

#### Pros
* Simple de implementar

#### Contras
* Puede causar timeout en requests pesadas

### Procesamiento Asincrónico

La donación se crea y se responde al usuario, se encola la tarea de segmentar la donación.

#### Pros
* Reduce tiempo de respuesta al usuario
* Mantiene trazabilidad de donaciones

#### Contras
* Aumenta la complejidad al incorporar gestión de tareas pendientes
