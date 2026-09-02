# Uso de IA en DonaTrack

Esta carpeta contiene los lineamientos prácticos para usar asistentes de IA en el desarrollo de DonaTrack.

El objetivo no es reemplazar el criterio técnico del equipo, sino usar IA para:
- entender mejor las issues;
- destrabar errores locales;
- preparar planes de implementación;
- generar y revisar pruebas;
- revisar riesgos antes de abrir una Pull Request;
- mejorar documentación técnica y diagramas PlantUML.

La regla principal es:

> Todo código generado, sugerido o modificado con ayuda de IA debe ser entendido, revisado, probado y validado por la persona que firma la Pull Request.

No se acepta como justificación:
> "Lo hizo la IA."

La IA puede ayudar, pero la responsabilidad técnica sigue siendo humana.

---

## Cómo está organizada esta carpeta

### Documentos generales

- [`01-principios-de-uso.md`](./01-principios-de-uso.md)  
  Reglas generales para usar IA de forma responsable en DonaTrack.

- [`02-uso-por-equipo.md`](./02-uso-por-equipo.md)  
  Explica cómo debería usar IA cada grupo del equipo: Diseño y Abstracción, Ejecución Guiada y Soporte e Implementación.

- [`03-prompts-por-etapa.md`](./03-prompts-por-etapa.md)  
  Indica qué prompt usar según el estado de la issue: diseño, implementación, testing, review o bloqueo.

- [`04-checklist-antes-de-pr.md`](./04-checklist-antes-de-pr.md)  
  Checklist obligatorio antes de pedir review de una PR en la que se usó IA.

- [`05-antipatrones.md`](./05-antipatrones.md)  
  Errores frecuentes al usar IA y cómo evitarlos.

- [`06-contexto-base-donatrack.md`](./06-contexto-base-donatrack.md)  
  Contexto base para copiar y pegar al iniciar una conversación con ChatGPT, Claude, Gemini, Cursor o cualquier asistente similar.

- [`07-errores-frecuentes-sonarcloud-ia.md`](./07-errores-frecuentes-sonarcloud-ia.md)  
  Guía viva de errores frecuentes y checklist pre-flight de SonarCloud para agentes de IA antes de finalizar tareas o abrir PRs.

### Política de revisión

- [`review/evaluator.md`](./review/evaluator.md)  
  Fuente canónica de la política Generator/Evaluator. Roles, modos de independencia (INDEPENDENT_REVIEW / SELF_REVIEW / LIGHTWEIGHT_CLOSING_CHECK), Review Contract, vectores V1–V9, capability detection, ciclo de re-check y responsabilidad humana.

---

## Prompts listos para usar

Los prompts se encuentran en [`prompts/`](./prompts/).

### Prioridad Alta

- [`prompts/alta-diseno-arquitectura.md`](./prompts/alta-diseno-arquitectura.md)  
  Para analizar decisiones estructurales, arquitectura, límites entre módulos, persistencia, integración o refactors grandes.

- [`prompts/alta-review-diseno.md`](./prompts/alta-review-diseno.md)  
  Para revisar una PR de diseño antes de aprobarla.

### Prioridad Media

- [`prompts/media-analisis-issue.md`](./prompts/media-analisis-issue.md)  
  Para entender una issue antes de implementarla.

- [`prompts/media-plan-implementacion.md`](./prompts/media-plan-implementacion.md)  
  Para convertir un diseño aprobado en un plan de implementación concreto.

- [`prompts/media-diseno-testing.md`](./prompts/media-diseno-testing.md)  
  Para definir qué pruebas necesita una feature o refactor.

### Prioridad Baja

- [`prompts/baja-implementacion-guiada.md`](./prompts/baja-implementacion-guiada.md)  
  Para implementar DTOs, mappers, endpoints simples, validaciones o ajustes mecánicos sobre un diseño ya definido.

- [`prompts/baja-debugger.md`](./prompts/baja-debugger.md)  
  Para analizar errores concretos de compilación, tests, Spring Boot, Hibernate, Maven, Docker o integración local.

- [`prompts/baja-tests-predefinidos.md`](./prompts/baja-tests-predefinidos.md)  
  Para generar o completar tests sobre escenarios ya definidos.

### Prompts transversales

- [`prompts/reviewer-pr-implementacion.md`](./prompts/reviewer-pr-implementacion.md)  
  Para revisar una PR antes de pedir review humana.

- [`prompts/plantuml.md`](./prompts/plantuml.md)  
  Para crear o revisar diagramas PlantUML alineados a una issue.

- [`prompts/retrospectiva.md`](./prompts/retrospectiva.md)  
  Para analizar feedback del equipo después de un caso piloto o cierre de etapa.

---

## Reglas rápidas

Antes de usar IA:

1. Definí qué querés lograr.
2. Pegá contexto real del proyecto.
3. Indicá la prioridad de la tarea.
4. Aclarale a la IA si querés análisis, diseño, implementación, testing o review.
5. No aceptes código que no puedas explicar.

Antes de abrir PR:

1. Revisá manualmente cada cambio.
2. Corré los tests correspondientes.
3. Pedile a la IA una revisión crítica del diff.
4. Confirmá que el cambio respeta el diseño aprobado.
5. Completá el checklist de IA.

---

## Principio rector

En DonaTrack usamos IA para pensar mejor, diseñar mejor, testear mejor y revisar mejor.

No usamos IA para delegar responsabilidad técnica ni para introducir código que no entendemos.