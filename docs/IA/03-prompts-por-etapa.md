# Prompts por etapa del flujo

## Objetivo

Este documento indica qué prompt usar según la etapa en la que se encuentra una issue dentro del flujo de DonaTrack.

La idea es evitar usar siempre el mismo prompt para todo. No es lo mismo analizar una issue, diseñar arquitectura, implementar una tarea chica, debuggear un error o revisar una PR.

---

# 1. Cuando recibís una issue y todavía no está clara

## Estado posible

- Pendiente.
- Lista para diseñar.
- Lista para implementar, pero con dudas.

## Objetivo

Entender la issue antes de tocar código.

## Prompt recomendado

[`prompts/media-analisis-issue.md`](./prompts/media-analisis-issue.md)

## Cuándo usarlo

Usalo si necesitás responder:

- ¿Qué pide realmente esta issue?
- ¿Qué entra y qué no entra?
- ¿Está lista para implementar?
- ¿Necesita diseño previo?
- ¿Está mal dividida?
- ¿Qué preguntas debería hacer?

## Resultado esperado

Después de usar este prompt deberías saber si la issue está:

- lista para diseñar;
- lista para implementar;
- bloqueada por falta de definición;
- demasiado grande y necesita sub-issues.

---

# 2. Cuando la issue requiere diseño técnico

## Estado posible

- Lista para diseñar.
- En diseño.

## Objetivo

Pensar arquitectura, responsabilidades, contratos y riesgos antes de escribir código.

## Prompt recomendado

[`prompts/alta-diseno-arquitectura.md`](./prompts/alta-diseno-arquitectura.md)

## Cuándo usarlo

Usalo si la issue toca:

- entidades importantes;
- services centrales;
- repositorios;
- persistencia;
- integración entre módulos;
- comunicación entre servicios;
- refactors transversales;
- decisiones de arquitectura.

## Resultado esperado

Deberías obtener:

- alternativas de diseño;
- riesgos;
- recomendación técnica;
- impacto en módulos;
- diagramas necesarios;
- criterios para habilitar implementación.

---

# 3. Cuando hay que crear o revisar PlantUML

## Estado posible

- En diseño.
- En review de diseño.

## Objetivo

Documentar el diseño técnico en archivos `.puml`.

## Prompt recomendado

[`prompts/plantuml.md`](./prompts/plantuml.md)

## Cuándo usarlo

Usalo si necesitás:

- crear un diagrama de clases;
- crear un diagrama de secuencia;
- crear un diagrama de actividad o flujo;
- revisar un `.puml`;
- ordenar clases por paquetes;
- verificar que el diagrama represente bien la issue.

## Resultado esperado

Deberías obtener:

- tipo de diagrama recomendado;
- código PlantUML;
- explicación de las relaciones;
- dudas abiertas;
- recomendaciones de claridad.

---

# 4. Cuando hay que revisar una PR de diseño

## Estado posible

- En review Diseño.

## Objetivo

Evaluar si el diseño está listo para pasar a implementación.

## Prompt recomendado

[`prompts/alta-review-diseno.md`](./prompts/alta-review-diseno.md)

## Cuándo usarlo

Usalo antes de aprobar una PR que contiene:

- archivos `.puml`;
- propuesta de arquitectura;
- definición de contratos;
- rediseño de responsabilidades;
- cambios de dominio.

## Resultado esperado

Deberías obtener:

- veredicto general;
- riesgos;
- ambigüedades;
- comentarios sobre responsabilidades;
- comentarios sobre contratos;
- tests que deberían derivarse del diseño;
- preguntas para el owner.

---

# 5. Cuando el diseño ya está aprobado y hay que implementar

## Estado posible

- Lista para implementar.
- En implementación.

## Objetivo

Convertir el diseño aprobado en pasos concretos de implementación.

## Prompt recomendado

[`prompts/media-plan-implementacion.md`](./prompts/media-plan-implementacion.md)

## Cuándo usarlo

Usalo cuando ya existe:

- issue clara;
- diseño aprobado;
- PlantUML mergeado o validado;
- alcance definido.

## Resultado esperado

Deberías obtener:

- archivos a revisar;
- pasos de implementación;
- orden sugerido;
- riesgos;
- tests necesarios;
- checklist antes de PR.

---

# 6. Cuando la tarea es baja, acotada o mecánica

## Estado posible

- Lista para implementar.
- En implementación.

## Objetivo

Implementar algo chico sin tomar decisiones arquitectónicas.

## Prompt recomendado

[`prompts/baja-implementacion-guiada.md`](./prompts/baja-implementacion-guiada.md)

## Cuándo usarlo

Usalo para:

- DTOs;
- mappers;
- adapters;
- builders;
- endpoints simples;
- validaciones ya definidas;
- queries simples;
- ajustes de nombres;
- refactors locales;
- documentación operativa.

## Resultado esperado

Deberías obtener:

- alcance confirmado;
- pasos concretos;
- archivos a modificar;
- código sugerido si corresponde;
- pruebas mínimas;
- preguntas para el mentor si hay dudas.

---

# 7. Cuando hay que pensar qué testear

## Estado posible

- En diseño.
- Lista para implementar.
- En implementación.

## Objetivo

Diseñar una estrategia de pruebas para una issue.

## Prompt recomendado

[`prompts/media-diseno-testing.md`](./prompts/media-diseno-testing.md)

## Cuándo usarlo

Usalo si necesitás definir:

- tests unitarios;
- tests de integración;
- pruebas manuales;
- casos borde;
- errores esperados;
- datos de prueba;
- riesgos de regresión.

## Resultado esperado

Deberías obtener:

- casos principales;
- casos borde;
- tests unitarios sugeridos;
- tests de integración sugeridos;
- pruebas manuales;
- datos necesarios;
- riesgos si no se testea.

---

# 8. Cuando ya hay escenarios de prueba definidos

## Estado posible

- En implementación.
- En review Implementación.

## Objetivo

Convertir escenarios ya definidos en tests concretos.

## Prompt recomendado

[`prompts/baja-tests-predefinidos.md`](./prompts/baja-tests-predefinidos.md)

## Cuándo usarlo

Usalo si ya sabés qué casos cubrir y necesitás ayuda para:

- escribir tests;
- completar tests;
- corregir tests;
- mejorar nombres;
- ordenar datos;
- agregar asserts relevantes.

## Resultado esperado

Deberías obtener:

- nombres de tests;
- estructura Arrange / Act / Assert;
- datos de prueba;
- código sugerido;
- explicación del test;
- cómo correrlo localmente.

---

# 9. Cuando aparece un error concreto

## Estado posible

- En implementación.
- Bloqueada.
- En review Implementación.
- Pipeline fallando.

## Objetivo

Entender un error antes de modificar código.

## Prompt recomendado

[`prompts/baja-debugger.md`](./prompts/baja-debugger.md)

## Cuándo usarlo

Usalo ante errores de:

- compilación;
- Maven o Gradle;
- Spring Boot;
- Hibernate/JPA;
- tests;
- Docker;
- endpoints;
- integración local;
- conflictos de ramas;
- pipeline.

## Resultado esperado

Deberías obtener:

- traducción del error;
- causas probables;
- archivos a revisar;
- pasos de diagnóstico;
- posible solución mínima;
- cómo validar;
- comentario sugerido para GitHub.

---

# 10. Cuando la implementación está lista y querés revisar antes de PR

## Estado posible

- En implementación.
- Antes de pasar a En review Implementación.

## Objetivo

Revisar críticamente el cambio antes de pedir review humana.

## Prompt recomendado

[`prompts/reviewer-pr-implementacion.md`](./prompts/reviewer-pr-implementacion.md)

## Cuándo usarlo

Usalo antes de abrir PR o antes de pedir review si modificaste:

- controllers;
- services;
- repositories;
- entidades;
- DTOs;
- tests;
- configuración;
- integración entre módulos.

## Resultado esperado

Deberías obtener:

- resumen de la PR;
- riesgos críticos;
- comentarios por archivo;
- tests faltantes;
- casos borde no cubiertos;
- preguntas para el autor;
- veredicto de readiness.

---

# 11. Cuando termina una etapa o caso piloto

## Estado posible

- Después de una entrega.
- Después del caso piloto.
- Después de una semana de trabajo.
- Después de un refactor importante.

## Objetivo

Ordenar feedback y detectar mejoras del proceso.

## Prompt recomendado

[`prompts/retrospectiva.md`](./prompts/retrospectiva.md)

## Cuándo usarlo

Usalo para analizar:

- qué funcionó bien;
- qué generó fricción;
- qué bloqueos aparecieron;
- qué aportó valor;
- cómo se usó IA;
- qué mejorar en el próximo ciclo.

## Resultado esperado

Deberías obtener:

- señales de éxito;
- señales de alerta;
- problemas raíz;
- mejoras recomendadas;
- acciones propuestas;
- decisiones abiertas;
- riesgos para el próximo ciclo.

---

# Regla práctica

Si no sabés qué prompt usar:

1. Si no entendés la issue: `media-analisis-issue.md`.
2. Si hay riesgo de arquitectura: `alta-diseno-arquitectura.md`.
3. Si hay que revisar diseño: `alta-review-diseno.md`.
4. Si hay que implementar sobre diseño aprobado: `media-plan-implementacion.md`.
5. Si es tarea chica y clara: `baja-implementacion-guiada.md`.
6. Si falló algo: `baja-debugger.md`.
7. Si hay que pensar pruebas: `media-diseno-testing.md`.
8. Si ya sabés qué testear: `baja-tests-predefinidos.md`.
9. Si terminaste código: `reviewer-pr-implementacion.md`.
10. Si terminó una etapa: `retrospectiva.md`.