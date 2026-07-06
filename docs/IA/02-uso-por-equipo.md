# Uso de IA por equipo

## Objetivo

Este documento explica cómo debería usar IA cada grupo de trabajo de DonaTrack según su nivel de responsabilidad, autonomía técnica y tipo de tareas.

El equipo se organiza en tres grupos:

1. Equipo de Diseño y Abstracción.
2. Equipo de Ejecución Guiada.
3. Equipo de Soporte e Implementación.

La IA no cumple el mismo rol para todos. Cuanto mayor es el riesgo técnico de una tarea, más cuidadoso debe ser el uso de IA.

---

# 1. Equipo de Diseño y Abstracción

## Integrantes

- Tade
- Ber
- Miri

## Foco del equipo

Este equipo trabaja sobre decisiones de mayor impacto técnico:

- arquitectura;
- límites entre módulos;
- responsabilidades del dominio;
- contratos internos;
- estrategia de persistencia;
- integración entre servicios;
- refactors transversales;
- revisiones de diseño;
- desbloqueo de tareas complejas.

## Cómo debería usar IA

La IA puede ayudar a:

- comparar alternativas de diseño;
- detectar riesgos de arquitectura;
- revisar responsabilidades entre clases;
- analizar acoplamiento;
- detectar sobrecarga de services;
- revisar posibles problemas de JPA/Hibernate;
- pensar contratos entre módulos;
- generar o revisar PlantUML;
- preparar una PR de diseño;
- revisar si una issue necesita dividirse en sub-issues.

## Cómo NO debería usar IA

No debería usarse para:

- decidir arquitectura sin revisión humana;
- aceptar una solución solo porque “parece prolija”;
- generar refactors grandes sin validar impacto;
- cambiar contratos entre módulos sin consenso;
- aprobar una PR de diseño sin análisis propio;
- reemplazar la discusión técnica del equipo.

## Prompts recomendados

- [`prompts/alta-diseno-arquitectura.md`](./prompts/alta-diseno-arquitectura.md)
- [`prompts/alta-review-diseno.md`](./prompts/alta-review-diseno.md)
- [`prompts/plantuml.md`](./prompts/plantuml.md)
- [`prompts/reviewer-pr-implementacion.md`](./prompts/reviewer-pr-implementacion.md)

## Ejemplo de uso correcto

Antes de diseñar un refactor transversal:

1. Pegar issue y contexto del módulo.
2. Pedir alternativas de diseño.
3. Pedir riesgos.
4. Pedir impacto en PlantUML.
5. Discutir la recomendación con el equipo.
6. Recién después abrir PR de diseño.

---

# 2. Equipo de Ejecución Guiada

## Integrantes

- Sofi
- Belén
- Nico

## Foco del equipo

Este equipo funciona como puente entre diseño e implementación.

Trabaja sobre:

- features medianas;
- implementación sobre diseños aprobados;
- diagnóstico de bugs de complejidad media;
- diseño de pruebas;
- revisión de alcance de issues;
- detección temprana de riesgos;
- acompañamiento al equipo de soporte;
- validación técnica antes de PR.

## Cómo debería usar IA

La IA puede ayudar a:

- analizar una issue antes de implementarla;
- detectar ambigüedades;
- transformar un diseño aprobado en un plan de implementación;
- definir tests relevantes;
- diagnosticar bugs;
- revisar si una tarea está lista para implementar;
- preparar comentarios para GitHub;
- revisar una PR antes de pedir review humana;
- acompañar tareas de prioridad baja con instrucciones más claras.

## Cómo NO debería usar IA

No debería usarse para:

- modificar arquitectura aprobada sin escalar;
- hacer refactors grandes dentro de una issue media;
- implementar sin revisar diseño;
- aceptar cambios que no se pueden explicar;
- resolver bloqueos estructurales sin consultar al equipo de Diseño y Abstracción.

## Prompts recomendados

- [`prompts/media-analisis-issue.md`](./prompts/media-analisis-issue.md)
- [`prompts/media-plan-implementacion.md`](./prompts/media-plan-implementacion.md)
- [`prompts/media-diseno-testing.md`](./prompts/media-diseno-testing.md)
- [`prompts/baja-debugger.md`](./prompts/baja-debugger.md)
- [`prompts/reviewer-pr-implementacion.md`](./prompts/reviewer-pr-implementacion.md)

## Ejemplo de uso correcto

Antes de implementar una issue media:

1. Usar `media-analisis-issue.md`.
2. Confirmar alcance.
3. Revisar diseño aprobado.
4. Usar `media-plan-implementacion.md`.
5. Implementar en pasos chicos.
6. Usar `media-diseno-testing.md`.
7. Usar `reviewer-pr-implementacion.md` antes de abrir PR.

---

# 3. Equipo de Soporte e Implementación

## Integrantes

- Martín
- Anush
- Aylen

## Foco del equipo

Este equipo trabaja sobre tareas acotadas, mecánicas o de bajo riesgo, siempre que el diseño y el alcance estén claros.

Ejemplos:

- DTOs;
- mappers;
- adapters;
- builders;
- endpoints simples;
- validaciones predefinidas;
- queries simples;
- tests sobre escenarios ya definidos;
- bugs ya diagnosticados;
- ajustes de nombres, firmas o paquetes;
- documentación operativa;
- refactors locales y mecánicos.

## Cómo debería usar IA

La IA puede ayudar a:

- entender una tarea acotada;
- seguir pasos de implementación guiada;
- analizar errores concretos;
- completar tests predefinidos;
- mejorar nombres de tests;
- revisar cambios chicos;
- redactar comentarios de bloqueo;
- entender conceptos puntuales.

## Cómo NO debería usar IA

No debería usarse para:

- tomar decisiones arquitectónicas;
- inventar flujos no definidos;
- modificar entidades centrales sin guía;
- cambiar contratos entre módulos;
- resolver ambigüedades de negocio sin consultar;
- hacer cambios grandes “porque la IA lo sugirió”;
- abrir PRs grandes sin checkpoint previo.

## Prompts recomendados

- [`prompts/baja-implementacion-guiada.md`](./prompts/baja-implementacion-guiada.md)
- [`prompts/baja-debugger.md`](./prompts/baja-debugger.md)
- [`prompts/baja-tests-predefinidos.md`](./prompts/baja-tests-predefinidos.md)
- [`prompts/media-analisis-issue.md`](./prompts/media-analisis-issue.md)

## Ejemplo de uso correcto

Para implementar un mapper o DTO:

1. Pegar la issue.
2. Pegar el diseño o indicación recibida.
3. Usar `baja-implementacion-guiada.md`.
4. Pedir pasos antes de código.
5. Implementar solo lo indicado.
6. Correr tests.
7. Pedir checkpoint si aparece duda.
8. Abrir PR chica y revisable.

---

# Regla de escalamiento

Si usando IA aparece una duda que afecta:

- arquitectura;
- contratos entre módulos;
- persistencia;
- entidades centrales;
- transacciones;
- reglas de negocio ambiguas;
- alcance de la issue;

entonces no se debe seguir implementando a ciegas.

Corresponde:

1. comentar el bloqueo en la issue;
2. mover la issue a Bloqueada si corresponde;
3. pedir ayuda por canal público;
4. escalar a Ejecución Guiada o Diseño y Abstracción según el caso.

---

# Resumen

La IA debe adaptarse al nivel de riesgo de la tarea.

- En prioridad Alta: IA para explorar, comparar y revisar diseño.
- En prioridad Media: IA para planificar, validar y ejecutar con criterio.
- En prioridad Baja: IA para guiar, explicar y evitar errores mecánicos.

En todos los casos, la responsabilidad final sigue siendo de la persona que firma la PR.