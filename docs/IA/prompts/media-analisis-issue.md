# Prompt — Análisis de Issue

## Cuándo usar este prompt

Usá este prompt cuando tomes una issue y necesites entenderla antes de diseñar o implementar.

Sirve para:
- interpretar el objetivo real de la issue;
- detectar ambigüedades;
- separar alcance de no-alcance;
- identificar archivos o módulos afectados;
- encontrar riesgos técnicos;
- preparar preguntas para el equipo;
- decidir si la issue está lista para diseñar o implementar.

---

## Prompt

Actuá como analista técnico de issues para el proyecto DonaTrack.

Necesito analizar una issue antes de empezar a implementar. No quiero que escribas código todavía.

## Contexto del proyecto

DonaTrack es un proyecto académico desarrollado en equipo. El flujo de trabajo separa diseño técnico e implementación. Las issues pueden tener prioridad Alta, Media o Baja según su riesgo técnico.

El objetivo de este análisis es evitar arrancar a codear con una issue ambigua, incompleta o mal delimitada.

## Issue a analizar

[PEGAR DESCRIPCIÓN COMPLETA DE LA ISSUE]

## Contexto adicional disponible

Prioridad asignada:
[ALTA / MEDIA / BAJA / NO DEFINIDA]

Módulo afectado:
[PEGAR SI SE SABE]

Comentarios previos del equipo:
[PEGAR SI EXISTEN]

Diseño previo o PlantUML:
[PEGAR SI EXISTE]

Archivos, clases o endpoints relacionados:
[PEGAR SI SE CONOCEN]

## Reglas

- No escribas código.
- No inventes clases, entidades, endpoints ni reglas de negocio que no estén en el contexto.
- Si falta información, marcala explícitamente.
- Separá lo que está claro de lo que es una suposición.
- Detectá si la issue está lista para implementar o si necesita diseño previo.
- Marcá riesgos de arquitectura, integración, persistencia, testing o alcance.
- Proponé preguntas concretas para dejar en la issue de GitHub.
- Si la issue parece demasiado grande, sugerí cómo dividirla en sub-issues.

## Formato de respuesta esperado

Respondé con esta estructura:

### 1. Interpretación de la issue

Explicá en lenguaje simple qué se busca lograr.

### 2. Objetivo funcional

Describí cuál parece ser el resultado esperado desde el punto de vista del usuario, sistema o regla de negocio.

### 3. Alcance

Separá:

#### Entra en esta issue

- ...

#### No debería entrar en esta issue

- ...

### 4. Información disponible

Listá qué datos, reglas o decisiones ya están claras.

### 5. Información faltante o ambigua

Listá todo lo que falta definir antes de avanzar.

Indicá si cada punto debería resolverlo:
- el owner de la issue;
- el reviewer;
- el equipo de Diseño y Abstracción;
- el profesor/enunciado;
- el equipo completo.

### 6. Módulos, clases o archivos probablemente afectados

Listá posibles zonas del proyecto a revisar.

Separá por:

- dominio;
- services;
- repositories;
- controllers;
- DTOs/mappers;
- tests;
- documentación/PlantUML;
- configuración o infraestructura.

### 7. Riesgos técnicos

Separá los riesgos en:

#### Riesgos de arquitectura

- ...

#### Riesgos de dominio

- ...

#### Riesgos de integración

- ...

#### Riesgos de persistencia/JPA

- ...

#### Riesgos de testing

- ...

#### Riesgos de alcance

- ...

### 8. ¿Necesita diseño previo?

Respondé una de estas opciones:

- Sí, necesita PR de diseño antes de implementar.
- No, puede pasar directo a implementación.
- Depende, falta aclarar algunos puntos.

Justificá la respuesta.

### 9. Posible división en sub-issues

Si la issue parece grande, proponé una división en:

- sub-issue de diseño;
- sub-issue de implementación;
- sub-issue de testing;
- sub-issue de documentación;
- sub-issue de validación o soporte.

### 10. Plan inicial recomendado

Proponé un plan corto de próximos pasos, sin código.

Ejemplo:

1. Revisar clases actuales.
2. Confirmar criterio de aceptación.
3. Actualizar PlantUML.
4. Pedir review de diseño.
5. Recién después implementar.

### 11. Preguntas para comentar en GitHub

Redactá preguntas concretas y bien formuladas para dejar en la issue.

### 12. Veredicto de readiness

Indicá una opción:

- Lista para diseñar.
- Lista para implementar.
- Bloqueada por falta de definición.
- Requiere división en sub-issues.