# [DTI-03] Desacoplamiento de SegmentacionEventListener en Servicio de Aplicación

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: deuda-tecnica, dti-03, eventos, cohesion, srp, refactor

## Contexto y Problema

En `donaciones-service`, la clase `SegmentacionEventListener` escucha el evento de normalización de donaciones. Sin embargo, en lugar de actuar como un listener ligero que simplemente traduce el evento e invoca un caso de uso, acumula **9 dependencias inyectadas** (`DonantesRepository`, `PersonasRepository`, `CategoriasRepository`, `IncentivosFeignClient`, `IDonacionesIndependientesRepository`, entre otros) y ejecuta directamente toda la lógica de negocio de la segmentación de lotes, obtención de entidades y persistencia de stock. Esto genera un componente altamente acoplado, difícil de testear y propenso a fallas en cascada.

## Atributos de Calidad y Drivers de Decisión

* **Principio de Responsabilidad Única (SRP):** Un listener solo debe escuchar eventos y delegar; no debe contener lógica de negocio.
* **Bajo Acoplamiento:** Reducir el fan-in / fan-out excesivo de dependencias inyectadas en un único bean.
* **Testeabilidad:** Permitir probar la lógica de segmentación de forma aislada sin simular el contexto de eventos de Spring.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Registrado en [docs/adr/DEUDA_TECNICA.md](../DEUDA_TECNICA.md) como **DTI-03**; Oleada 4 del Plan Genérico v2.
* **Hallazgo:** El modelo de referencia del equipo consensuado en el debate arquitectónico (Plan Genérico v2 §1) establece taxativamente: *"Los Listeners son adaptadores de entrada puros: solo traducen el evento hacia una llamada a un Application Service, cero lógica de negocio propia"*.

## Alternativas Consideradas

* **Extracción de `SegmentacionService` (Application Service):** Crear la interfaz y servicio de aplicación `SegmentacionService` que concentre las dependencias de negocio y orqueste el caso de uso. `SegmentacionEventListener` se reduce a un adaptador delgado de 1 sola dependencia que delega directamente.
* **Mantener la Lógica en el Listener:** Dejar las 9 dependencias agrupadas en el listener.

## Resultado de la Decisión

Alternativa elegida: "Extracción de `SegmentacionService` (Application Service)"

Justificación:
Alinea a `donaciones-service` con la arquitectura objetivo de capas de DonaTrack. Restaura el listener a su rol natural de adaptador de infraestructura y encapsula el caso de uso de segmentación en un servicio testeable y reutilizable.

### Consecuencias Positivas

* Listener ultraligero y desacoplado (pasa de 9 dependencias a 1).
* Caso de uso de segmentación encapsulado y comprobable mediante tests unitarios aislados con mocks reducidos.
* Coherencia arquitectónica con el resto de los listeners del monorepo.

### Consecuencias Negativas

* Creación de una nueva interfaz y clase de servicio (`ISegmentacionService` e implementación).

### Validación

Se valida mediante:
1. `SegmentacionEventListener` inyecta únicamente `ISegmentacionService`.
2. Tests unitarios dedicados en `SegmentacionServiceTest` cubriendo el flujo completo de segmentación.

## Análisis de Alternativas

### Extracción de SegmentacionService

#### Pros
* Cumple estrictamente SRP y el modelo de arquitectura por capas.
* Tests unitarios simples y limpios.

#### Contras
* Archivo adicional en el proyecto.

### Mantener en Listener

#### Pros
* Menor número de archivos.

#### Contras
* Mantenimiento complejo, acumulación de deuda técnica y violación de los principios rectores de `AGENTS.md`.