# [DTI-05] Segregación de Responsabilidades en AlgoritmosService

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: deuda-tecnica, dti-05, cohesion, srp, diseno, refactor

## Contexto y Problema

En `donaciones-service`, la clase `AlgoritmosService` sufre de baja cohesión al asumir dos responsabilidades conceptualmente disjuntas:
1. **Ejecución Algorítmica Pura:** Orquestar los algoritmos de matching (`AlgoritmoCompatibilidadSemantica`, `AlgoritmoPrioridadSubAtendidos`), alimentar el stock de inventario disponible y calcular las propuestas sugeridas.
2. **Gestión de Ciclo de Vida de Propuestas:** Actuar como servicio CRUD para consultar propuestas, aprobar propuestas confirmadas, rechazar sugerencias y notificar a los actores involucrados.
Mezclar ambas tareas en una sola clase dificulta el testing, acopla los endpoints de ejecución automática con los de administración manual y diluye el principio de responsabilidad única.

## Atributos de Calidad y Drivers de Decisión

* **Alta Cohesión:** Agrupar métodos que colaboran estrechamente sobre el mismo aggregate root.
* **Separación de Responsabilidades (SRP):** Segregar la computación de matching de la gestión transaccional de propuestas aprobadas.
* **Flexibilidad:** Permitir evolucionar los algoritmos de asignación sin modificar la API de gestión de propuestas.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Registrado en [docs/adr/DEUDA_TECNICA.md](../DEUDA_TECNICA.md) como **DTI-05**; Diagnóstico Arquitectónico §1.6.
* **Hallazgo:** Se detectó que `Propuesta` es un Aggregate Root completo según el modelo DDD (`aggregates-donaciones.md`), por lo que merece su propio Application Service de gestión desacoplado del motor de cálculo.

## Alternativas Consideradas

* **Segregación en `AlgoritmosService` y `PropuestasService`:**
  - `AlgoritmosService`: Se enfoca estrictamente en invocar y consolidar algoritmos de matching y retornar propuestas preliminares.
  - `PropuestasService`: Administra el repositorio de propuestas, su aprobación, persistencia definitiva, ejecución de asignación en stock y publicación de eventos.
* **Mantener Servicio Único:** Conservar todas las operaciones dentro de `AlgoritmosService`.

## Resultado de la Decisión

Alternativa elegida: "Segregación en `AlgoritmosService` y `PropuestasService`"

Justificación:
`Propuesta` es un aggregate root con su propia persistencia y ciclo de vida (`PENDIENTE`, `APROBADA`, `DESCARTADA`). Separar la computación de la gestión del aggregate root cumple con las pautas de Domain-Driven Design y permite una división limpia de controladores REST (`AlgoritmosController` vs `PropuestaDeAsignacionController`).

### Consecuencias Positivas

* Cohesión excelente: cada servicio tiene un propósito delimitado y claro.
* Simplificación de suites de test: los tests de algoritmos no necesitan mockear la persistencia de propuestas aprobadas.
* Alineación 1:1 entre Aggregate Roots y Application Services.

### Consecuencias Negativas

* Requiere dividir la interfaz actual y reubicar las inyecciones de dependencias.

### Validación

Se valida mediante:
1. Existencia de `AlgoritmosService` y `PropuestaDeAsignacionService` con dependencias independientes.
2. Tests unitarios segregados para cada responsabilidad.

## Análisis de Alternativas

### Segregación de Servicios

#### Pros
* Arquitectura limpia y altamente mantenible.
* Respeta los límites de agregados de DDD.

#### Contras
* Mayor número de archivos de servicio e interfaces.

### Mantener Servicio Unificado

#### Pros
* Menos clases en el paquete de servicios.

#### Contras
* Clase dios que continuará creciendo a medida que se agreguen más heurísticas o acciones sobre propuestas.