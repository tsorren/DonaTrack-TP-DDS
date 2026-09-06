# [DTI-02] Reubicación de ProcesadorDeDonaciones a Capa de Aplicación

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: deuda-tecnica, dti-02, arquitectura-capas, cohesion, refactor

## Contexto y Problema

En `donaciones-service`, la clase `ProcesadorDeDonaciones` cumple el rol de coordinar el ciclo de vida inicial de las donaciones: normalización asíncrona de bienes mediante el analizador semántico, segmentación en donaciones independientes de inventario, y notificación de eventos hacia `incentivos-service`. Sin embargo, esta clase se encuentra ubicada en el paquete `grupo5.donaciones.infrastructure/`. Esto viola el principio de Arquitectura en Capas y Arquitectura Limpia: una clase que orquesta casos de uso de negocio y coordina múltiples servicios es, por definición, un **Application Service** y no un adaptador tecnológico de infraestructura.

## Atributos de Calidad y Drivers de Decisión

* **Alta Cohesión y Arquitectura Limpia:** La estructura de paquetes debe reflejar con fidelidad la responsabilidad de cada componente.
* **Mantenibilidad:** Evitar que los desarrolladores busquen casos de uso de negocio dentro de paquetes técnicos.
* **Separación de Responsabilidades:** Reservar `infrastructure/` exclusivamente para clientes HTTP, configuraciones de beans, publicadores de mensajería y adaptadores de red.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Registrado en [docs/adr/DEUDA_TECNICA.md](../DEUDA_TECNICA.md) como **DTI-02**; Diagnóstico Arquitectónico §1.4.
* **Hallazgo:** La auditoría arquitectónica inicial identificó a `ProcesadorDeDonaciones` como uno de los principales focos de desorden estructural del proyecto.

## Alternativas Consideradas

* **Reubicación al Paquete de Aplicación (`services/`):** Mover `ProcesadorDeDonaciones.java` al paquete `grupo5.donaciones.services/` (o un subpaquete de casos de uso `services.procesamiento/`), manteniendo únicamente los adaptadores externos (como llamadas Feign o analizadores de NLP externos) en `infrastructure/`.
* **Mantener en `infrastructure/`:** Dejar la clase donde está para no modificar importaciones existentes.

## Resultado de la Decisión

Alternativa elegida: "Reubicación al Paquete de Aplicación (`services/`)"

Justificación:
La regla de dependencias de la Arquitectura Limpia establece que las capas superiores (servicios de aplicación) orquestan el dominio y consumen la infraestructura a través de interfaces o adaptadores. Colocar un orquestador dentro de `infrastructure/` invierte confusamente las responsabilidades del sistema.

### Consecuencias Positivas

* Mapa de paquetes 100% coherente con el modelo de capas de DonaTrack.
* Claridad para nuevos integrantes y herramientas de análisis estático (SonarCloud).
* Facilita el testeo unitario aislando la orquestación de la infraestructura.

### Consecuencias Negativas

* Requiere actualizar importaciones en controladores y tests asociados.

### Validación

Se valida mediante:
1. `ProcesadorDeDonaciones.java` reside en `grupo5.donaciones.services`.
2. `mvn clean test -pl donaciones-service` compila y aprueba la totalidad de sus tests unitarios.

## Análisis de Alternativas

### Reubicación en Application Services

#### Pros
* Arquitectura en capas limpia y predecible.
* Cumple las reglas de gobernanza de `AGENTS.md`.

#### Contras
* Modificación de rutas de archivos en git.

### Mantener en Infraestructura

#### Pros
* Cero cambios en git.

#### Contras
* Perpetúa deuda técnica y degrada la legibilidad arquitectónica del proyecto.