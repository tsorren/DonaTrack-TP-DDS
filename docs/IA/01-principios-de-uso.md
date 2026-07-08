# Principios de uso de IA en DonaTrack

## Objetivo

Este documento define los principios generales para usar asistentes de IA en DonaTrack de forma responsable, útil y alineada al flujo de trabajo del equipo.

La IA puede ayudarnos a acelerar tareas, destrabar errores, generar ideas de testing y revisar riesgos, pero no reemplaza el criterio técnico de quienes diseñan, implementan y revisan el sistema.

---

## Principio principal

> Todo código generado, sugerido o modificado con ayuda de IA debe ser entendido, revisado, probado y validado por la persona que firma la Pull Request.

No se acepta como justificación:

> "Lo hizo la IA."

La IA puede asistir, pero la responsabilidad técnica sigue siendo humana.

---

## Para qué usamos IA

En DonaTrack usamos IA para:

- entender mejor una issue antes de implementarla;
- detectar ambigüedades de alcance;
- preparar planes de implementación;
- analizar errores de compilación, tests, Spring Boot, Hibernate, Docker o Maven;
- generar ideas de casos de prueba;
- escribir esqueletos de tests;
- revisar un diff antes de pedir review humana;
- mejorar documentación técnica;
- revisar o generar PlantUML;
- detectar riesgos de arquitectura, integración o persistencia;
- ordenar retrospectivas y aprendizajes del equipo.

---

## Para qué NO usamos IA

No usamos IA para:

- copiar y pegar código sin entenderlo;
- delegar decisiones arquitectónicas sin validación humana;
- hacer cambios masivos fuera del alcance de una issue;
- aprobar automáticamente una solución;
- reemplazar la review humana;
- inventar reglas de negocio;
- modificar contratos entre módulos sin consultar al equipo;
- generar código contra clases, endpoints o entidades que no existen;
- resolver errores “probando cambios al azar”.

---

## Regla de oro

Antes de pedirle código a una IA, primero pedile análisis.

Orden recomendado:

1. Entender la issue.
2. Aclarar alcance.
3. Identificar riesgos.
4. Revisar diseño o PlantUML si corresponde.
5. Planificar implementación.
6. Recién después escribir código.
7. Diseñar o ajustar tests.
8. Revisar el diff.
9. Abrir PR.

---

## La IA necesita contexto

Un prompt útil debe incluir contexto real del proyecto.

Ejemplos de contexto útil:

- descripción completa de la issue;
- prioridad de la tarea;
- módulo afectado;
- diseño aprobado;
- PlantUML;
- clases relacionadas;
- endpoints afectados;
- errores completos;
- tests existentes;
- reglas de negocio;
- restricciones del equipo;
- qué NO debe tocarse.

Un prompt sin contexto suele generar respuestas genéricas, incompletas o incorrectas.

---

## No confiar en la primera respuesta

La primera respuesta de una IA no debe tomarse como definitiva.

Siempre conviene:

- pedirle que explique supuestos;
- pedirle riesgos;
- pedirle alternativas;
- pedirle tests;
- pedirle que revise su propio alcance;
- usar otro prompt de review antes de abrir PR.

---

## Separar generación de evaluación

No conviene que la misma conversación que generó una solución sea la única que la evalúe.

Flujo recomendado:

1. Usar IA en modo builder para entender o implementar.
2. Usar IA en modo reviewer para revisar críticamente.
3. Hacer revisión humana.
4. Correr tests.
5. Recién después pedir review formal.

---

## IA como apoyo al aprendizaje

Cuando no se entiende un concepto, la IA puede usarse como tutor.

Ejemplos:

- “Explicame qué significa este error de Hibernate.”
- “Explicame qué hace esta anotación de Spring.”
- “Mostrame un ejemplo simple de este patrón.”
- “Ayudame a entender por qué este test falla.”

Pero después de recibir la explicación, cada integrante debe poder explicar con sus palabras qué cambió y por qué.

---

## Criterio mínimo antes de aceptar código generado

Antes de aceptar código sugerido por IA, la persona responsable debe poder responder:

1. ¿Qué problema resuelve?
2. ¿Qué archivos modifica?
3. ¿Por qué esta solución respeta el diseño aprobado?
4. ¿Qué casos borde cubre?
5. ¿Qué tests validan el cambio?
6. ¿Qué podría romper?
7. ¿Qué parte del código no entiendo todavía?

Si alguna de estas respuestas no está clara, no corresponde abrir PR.

---

## Principio final

En DonaTrack usamos IA para pensar mejor, diseñar mejor, testear mejor y revisar mejor.

No usamos IA para delegar responsabilidad técnica ni para introducir código que no entendemos.