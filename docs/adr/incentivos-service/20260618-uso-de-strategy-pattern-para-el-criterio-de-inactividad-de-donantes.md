# Uso de Strategy pattern para el criterio de inactividad de donantes
- Status: proposed
- Date: 2026-06-18
- Deciders: Decisión Grupal

## Contexto y Problema
El sistema necesita detectar donantes inactivos de forma periódica. El criterio de inactividad puede variar, por lo que hardcodearlo en el job o el servicio lo haría difícil de cambiar o extender.

## Alternativas Consideradas
* Strategy con CriterioInactividad abstracta

## Resultado de la Decisión

Alternativa elegida: "Strategy con CriterioInactividad abstracta"

Justificación:
El uso de Strategy permite que el job sea independiente del criterio concreto, facilitando agregar o cambiar criterios de inactividad sin modificar código existente.

## Análisis de Alternativas

### Strategy con CriterioInactividad abstracta

CriterioInactividad es una clase abstracta con el método detectarInactivos(). InactividadDonaciones lo implementa filtrando por días sin donar, configurable vía InactividadConfig. El InactividadJob recibe una lista de criterios por inyección y los itera.

#### Pros
* Agregar un nuevo criterio solo requiere crear una subclase y registrarla como Bean
* El job no conoce los detalles de ningún criterio
* Los días de inactividad son configurables sin tocar lógica de negocio

#### Contras
* Requiere una clase de configuración adicional (InactividadConfig)
