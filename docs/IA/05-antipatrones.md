# Antipatrones de uso de IA

## Objetivo

Este documento recopila errores frecuentes al usar IA en desarrollo de software y cómo evitarlos dentro de DonaTrack.

La idea no es prohibir la IA, sino usarla mejor.

---

# 1. Copiar y pegar sin entender

## Qué es

Aceptar código generado por IA sin leerlo, sin entenderlo y sin validarlo.

## Por qué es peligroso

Puede introducir:

- bugs funcionales;
- errores de persistencia;
- cambios fuera de alcance;
- código incompatible con el proyecto;
- tests falsamente verdes;
- deuda técnica difícil de detectar.

## Cómo evitarlo

Antes de aceptar código, preguntarse:

- ¿Qué hace?
- ¿Por qué lo hace?
- ¿Qué toca?
- ¿Qué rompe si falla?
- ¿Qué tests lo validan?

---

# 2. Pedir “haceme la issue”

## Qué es

Darle a la IA una issue y pedirle directamente que implemente todo.

## Por qué es peligroso

La IA puede:

- asumir reglas de negocio inexistentes;
- inventar clases o endpoints;
- saltarse el diseño previo;
- generar una solución demasiado grande;
- mezclar varias responsabilidades.

## Cómo evitarlo

Usar primero:

[`prompts/media-analisis-issue.md`](./prompts/media-analisis-issue.md)

Después, si corresponde:

[`prompts/media-plan-implementacion.md`](./prompts/media-plan-implementacion.md)

---

# 3. Vibe coding

## Qué es

Ir aceptando cambios sugeridos por IA porque “parecen funcionar”, sin estrategia ni comprensión completa.

## Por qué es peligroso

El código puede avanzar rápido pero en dirección incorrecta.

Síntomas:

- muchos archivos modificados;
- no se puede explicar el diseño;
- los tests fallan y se arreglan con parches;
- aparecen nuevas abstracciones sin necesidad;
- la PR se vuelve gigante.

## Cómo evitarlo

Trabajar en pasos chicos:

1. análisis;
2. alcance;
3. diseño;
4. plan;
5. implementación mínima;
6. tests;
7. review.

---

# 4. Cambios masivos fuera de alcance

## Qué es

Aprovechar una issue chica para hacer refactors grandes sugeridos por IA.

## Por qué es peligroso

Genera:

- conflictos de merge;
- PRs difíciles de revisar;
- regresiones;
- discusiones innecesarias;
- retraso en la entrega.

## Cómo evitarlo

Si aparece una mejora fuera de alcance:

1. anotarla;
2. comentarla en la issue;
3. crear otra issue si corresponde;
4. no mezclarla en la PR actual.

---

# 5. Confiar en la autoevaluación de la IA

## Qué es

Preguntarle a la misma IA que generó el código:

> “¿Está todo bien?”

Y aceptar su respuesta como review suficiente.

## Por qué es peligroso

La IA puede justificar su propia solución y no detectar problemas reales.

## Cómo evitarlo

Usar un prompt separado de review:

[`prompts/reviewer-pr-implementacion.md`](./prompts/reviewer-pr-implementacion.md)

Y pedirle explícitamente que actúe como reviewer crítico o adversarial.

---

# 6. No dar contexto del proyecto

## Qué es

Pedir ayuda con prompts genéricos como:

> “Arreglá este error.”
> “Haceme los tests.”
> “Mejorá este código.”

## Por qué es peligroso

La IA responde con soluciones genéricas que pueden no aplicar a DonaTrack.

## Cómo evitarlo

Siempre incluir:

- issue;
- módulo;
- código relevante;
- error completo;
- diseño aprobado;
- restricciones;
- qué se quiere lograr;
- qué no debe tocarse.

Usar:

[`06-contexto-base-donatrack.md`](./06-contexto-base-donatrack.md)

---

# 7. Ocultar bloqueos

## Qué es

Quedarse muchas horas intentando resolver algo con IA sin avisar al equipo.

## Por qué es peligroso

Genera:

- pérdida de tiempo;
- soluciones incorrectas;
- frustración;
- issues desactualizadas;
- integración tardía.

## Cómo evitarlo

Si el bloqueo dura demasiado:

1. comentar en la issue;
2. pedir ayuda en canal público;
3. mover a Bloqueada si corresponde;
4. usar `baja-debugger.md` para ordenar el diagnóstico;
5. escalar a Ejecución Guiada o Diseño y Abstracción.

---

# 8. Pedir código cuando falta definición

## Qué es

Intentar implementar aunque todavía no esté claro el alcance, el criterio de aceptación o el diseño.

## Por qué es peligroso

Se termina resolviendo diseño durante el código.

## Cómo evitarlo

Primero usar:

[`prompts/media-analisis-issue.md`](./prompts/media-analisis-issue.md)

Y si hay riesgo técnico:

[`prompts/alta-diseno-arquitectura.md`](./prompts/alta-diseno-arquitectura.md)

---

# 9. Tests generados sin criterio

## Qué es

Pedirle a la IA tests y aceptar cualquier test que compile.

## Por qué es peligroso

Puede generar tests que:

- no validan reglas reales;
- solo verifican mocks;
- son frágiles;
- pasan aunque la lógica esté mal;
- dan falsa sensación de seguridad.

## Cómo evitarlo

Primero diseñar qué testear:

[`prompts/media-diseno-testing.md`](./prompts/media-diseno-testing.md)

Luego generar tests concretos:

[`prompts/baja-tests-predefinidos.md`](./prompts/baja-tests-predefinidos.md)

---

# 10. Usar IA para decisiones de arquitectura sin validación

## Qué es

Aceptar una propuesta de arquitectura, persistencia o integración solo porque la IA la recomendó.

## Por qué es peligroso

Puede entrar en conflicto con:

- diseño existente;
- convenciones del equipo;
- restricciones del enunciado;
- módulos ya implementados;
- estrategia de persistencia;
- límites entre servicios.

## Cómo evitarlo

Toda decisión estructural debe validarse con el Equipo de Diseño y Abstracción.

La IA puede proponer alternativas, pero no decidir sola.

---

# 11. Inventar contexto

## Qué es

Cuando la IA asume que existen clases, métodos, entidades o endpoints que no fueron pegados en el prompt.

## Por qué es peligroso

El código puede parecer correcto pero no compilar o no integrarse.

## Cómo evitarlo

Agregar esta regla en los prompts:

> No inventes clases, métodos, endpoints ni reglas de negocio que no aparezcan en el contexto. Si falta información, pedila explícitamente.

---

# 12. Abrir PR sin revisión propia

## Qué es

Abrir PR apenas el código compila, sin revisar el diff ni correr checklist.

## Por qué es peligroso

Hace que la review humana encuentre errores básicos que el owner debería haber detectado.

## Cómo evitarlo

Antes de abrir PR:

1. correr tests;
2. revisar diff propio;
3. usar `reviewer-pr-implementacion.md`;
4. completar `04-checklist-antes-de-pr.md`;
5. recién después pedir review.

---

# Resumen

Mal uso de IA:

> “Haceme esto y lo pego.”

Buen uso de IA:

> “Ayudame a entender, planificar, validar, testear y revisar este cambio. La responsabilidad final es mía.”