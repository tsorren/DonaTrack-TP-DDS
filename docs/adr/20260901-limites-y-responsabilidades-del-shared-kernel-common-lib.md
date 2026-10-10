# Límites y Responsabilidades del Shared Kernel (common-lib)

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: arquitectura, common-lib, shared-kernel, ddd, acoplamiento

## Contexto y Problema

En un ecosistema de microservicios multi-módulo basado en Maven como DonaTrack (`donaciones-service`, `notificaciones-service`, `incentivos-service`, `logistica-service`), existe una fuerte tentación de compartir clases entre módulos para evitar duplicación. Sin embargo, compartir indiscriminadamente modelos de dominio, DTOs de transporte o clases base de servicios/controladores crea un acoplamiento binario estrecho, transformando la arquitectura en el antipatrón de **Monolito Distribuido** (donde cualquier cambio en una regla de negocio obliga a recompilar y redesplegar todos los microservicios). Se requiere formalizar qué conceptos tienen derecho a residir en la biblioteca compartida `common-lib` (Shared Kernel) y cuáles deben estar terminantemente prohibidos.

## Atributos de Calidad y Drivers de Decisión

* **Bajo Acoplamiento (Low Coupling):** Garantizar la autonomía de despliegue y evolución de cada microservicio.
* **Mantenibilidad:** Evitar dependencias cíclicas y cambios en cascada en contratos de API.
* **Simplicidad e Integridad Conceptual:** Compartir únicamente infraestructura técnica transversal sin contaminar la semántica de negocio.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 0.5 (Inventario de `common-lib`) y Oleada 11 (Sincronización con `common-lib`) del [Plan Genérico de Refactor por Oleadas v2](../arquitectura/diseno/plan-refactor-oleadas-generico-v2.md).
* **Hallazgo:** Durante los primeros refactors, existía la ambigüedad de si DTOs comunes o controladores genéricos CRUD debían vivir en `common-lib`. La experiencia demostró que abstraer `BaseController` o `BaseService` violaba el principio de sustitución de Liskov (cuando un servicio no permitía `DELETE`) y acoplaba las rutas REST. Se consolidó la regla de que `common-lib` debe ser estrictamente agnóstica de negocio.

## Alternativas Consideradas

* **Shared Kernel Puramente Técnico y Transversal (Aislado de Dominio):** Centralizar únicamente abstracciones genéricas (`AggregateRoot`, `CrudRepository<T>`, jerarquía base de excepciones `DonaTrackException`, catálogo de códigos `ErrorCatalog`, interceptores de observabilidad/MDC y autoconfiguración OpenAPI). Prohibir entidades de negocio, DTOs y clases base CRUD.
* **Shared Kernel Extendido (Biblioteca Compartida de Negocio):** Incluir clases base como `BaseEntity`, `BaseController`, `BaseService` y DTOs comunes de personas y donaciones compartidos por todos los servicios.
* **Zero Shared Code (Microservicios Totalmente Aislados sin common-lib):** Duplicar la infraestructura técnica (filtros de servlet, manejo de errores, clases de excepciones) en cada uno de los cuatro proyectos.

## Resultado de la Decisión

Alternativa elegida: "Shared Kernel Puramente Técnico y Transversal (Aislado de Dominio)"

Justificación:
Esta alternativa preserva la independencia de los Bounded Contexts de Domain-Driven Design (DDD). Al permitir únicamente utilidades transversales (*cross-cutting concerns*) y contratos genéricos con tipos parametrizados (`T extends AggregateRoot`), ningún microservicio se acopla a las reglas de negocio de otro. Los DTOs permanecen como contratos locales de cada API pública y los controladores retienen su flexibilidad semántica.

### Consecuencias Positivas

* Previene el acoplamiento semántico entre bounded contexts; cada servicio evoluciona su modelo libremente.
* Estandariza respuestas de error HTTP (RFC 7807) y códigos máquina legibles en todo el ecosistema mediante `GlobalExceptionHandler` y `ErrorCatalog`.
* Provee la base para persistencia en memoria concurrente segura (`CrudRepositoryEnMemoria`) y observabilidad distribuida (`traceId`).

### Consecuencias Negativas

* Requiere mapear DTOs homólogos entre servicios que colaboran vía REST/Feign (ej. réplica de personas en notificaciones), asumiendo una ligera duplicación estructural en favor de la autonomía.

### Validación

Se valida mediante inspección de dependencias y código fuente:
1. `common-lib` no contiene importaciones de paquetes `grupo5.donaciones`, `grupo5.logistica`, `grupo5.incentivos` ni `grupo5.notificaciones`.
2. No existen clases llamadas `BaseService` ni `BaseController` en `common-lib`.
3. Todos los repositorios de los microservicios extienden `CrudRepository<T extends AggregateRoot>`.

## Análisis de Alternativas

### Shared Kernel Puramente Técnico y Transversal

#### Pros
* Autonomía de despliegue real para cada servicio.
* Reutilización de componentes críticos de auditoría, logging y tratamiento de fallas.
* Cumplimiento estricto de las directrices de `AGENTS.md` (§4.2).

#### Contras
* Requiere disciplina de equipo para rechazar PRs que intenten colocar lógica de negocio "común" por comodidad.

### Shared Kernel Extendido

#### Pros
* Ahorro inicial aparente de líneas de código al no reescribir controladores ni DTOs parecidos.

#### Contras
* Rompe el despliegue independiente: modificar un campo en un DTO obliga a recompilar toda la solución.
* Convierte a los servicios en una arquitectura monolítica con la sobrecarga de red de los microservicios.

### Zero Shared Code

#### Pros
* Desacoplamiento binario absoluto entre proyectos Maven.

#### Contras
* Duplicación masiva de código de infraestructura (filtros HTTP, MDC, configuración Swagger).
* Dispersión y falta de uniformidad en los formatos de error expuestos a los clientes de la API.