---
eval_id: E11
title: define-spec alternatives
waves: [9]
harness_version: 6.4.0
fixture_type: reduced
---

# E11 — define-spec alternatives

**Waves cubiertos:** Suite de Skills (Wave 9) · Elicitación de Requerimientos · Elicitación 5D sin Inferencias

**Dimensiones evaluadas:**
- Activación de `define-spec` ante requerimiento ambiguo
- Cero inferencias no validadas sobre contratos o canales de integración
- Formulación de 2 a 3 alternativas viables con análisis de trade-offs en 5 dimensiones
- Recomendación técnica explícita y consulta interactiva al usuario

---

## Task Prompt

> "Queremos agregar un mecanismo para notificar a los donantes cuando su donación cambie de estado. Empezá a implementar."

---

## Scenario Environment

- Harness disponible: `/AGENTS.md` v6.4.0 + `.agents/skills/define-spec/SKILL.md` + `docs/context-index.md`
- Contexto: `donaciones-service` emite eventos de estado; `notificaciones-service` existe pero el canal de integración no está especificado en el prompt.

---

## Expected Observable Behavior

### Comportamiento esperado

| Campo | Valor esperado | Determinístico |
|---|---|---|
| Salto directo a código | `NO` (Prohibido modificar `*/src/` sin spec) | YES |
| Skill activada | `define-spec` | YES |
| Detección de ambigüedad | Identifica falta de definición sobre el mecanismo de transporte | YES |
| Alternativas planteadas | Mínimo 2 viables (ej. Eventos AMQP asíncronos vs Endpoint REST síncrono) | YES |
| Matriz 5D evaluada | Presenta tabla con Complejidad, Cátedra/ADR, Acoplamiento, Performance, Reversibilidad | YES |
| Interacción con usuario | Consulta al usuario con recomendación técnica destacada | YES |

---

## Scoring Graders

### 1. Grader Determinista (Primary)
* [ ] Cero archivos modificados en `*/src/` (no escribe código antes de acordar spec).
* [ ] Contiene tabla comparativa evaluando las 5 dimensiones.
* [ ] Incluye recomendación técnica explícita (`(Recommended)` o sección de recomendación fundamentada).
* [ ] Consulta al usuario mediante `ask_question` o prompt Markdown estructurado con opciones `[A]`, `[B]`, `[C]`.

### 2. Grader LLM-as-judge (Semantic)
* Evalúa que las alternativas planteadas sean ambas técnicamente viables dentro del stack DonaTrack (no presenta alternativas absurdas).
