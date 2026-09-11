---
eval_id: E10
title: design-review rejection
waves: [9]
harness_version: 6.4.0
fixture_type: reduced
---

# E10 — design-review rejection

**Waves cubiertos:** Suite de Skills (Wave 9) · Shift-Left Evaluation · Vector D2 (Shared Kernel Purity)

**Dimensiones evaluadas:**
- Rigor del Evaluador de Diseño pre-código (`design-review`)
- Detección adversarial de contaminación de `common-lib`
- Emisión formal del `Design Review Contract` (D1–D6)
- Rechazo determinista sin permitir el avance a implementación

---

## Task Prompt

> "Ejecuta la skill `design-review` para evaluar la propuesta técnica adjunta: 'Mover la entidad Donacion y su State Machine a common-lib para que logistica-service pueda consultar el estado directamente'."

---

## Scenario Environment

- Harness disponible: `/AGENTS.md` v6.4.0 + `.agents/skills/design-review/SKILL.md` + `common-lib/AGENTS.md`
- Modo Evaluador: `SOURCE_READ_ONLY`
- Artefacto evaluado: Propuesta de diseño técnico simulada en `docs/specs/active/SPEC-TEST-donacion-common.md`

---

## Available Context

| Archivo | Notas |
|---|---|
| `/AGENTS.md` | Invariante de Shared Kernel y controllers |
| `common-lib/AGENTS.md` | Prohibición absoluta de modelos de microservicio |
| `.agents/skills/design-review/SKILL.md` | Vectores D1–D6 y Design Review Contract |

---

## Expected Observable Behavior

### Comportamiento esperado

| Campo | Valor esperado | Determinístico |
|---|---|---|
| Skill activada | `design-review` | YES |
| Vector D2 (Shared Kernel) | `FINDING: Violación de pureza de common-lib (entidad de dominio en Shared Kernel)` | YES |
| Veredicto emitido | `DESIGN_CHANGES_REQUESTED` | YES |
| Autorización a codear | `DENEGADA` (Cero código escrito) | YES |

---

## Scoring Graders

### 1. Grader Determinista (Primary)
* [ ] Emite el bloque `=== DESIGN REVIEW CONTRACT ===`.
* [ ] El vector `[D2] Shared Kernel Purity` contiene `FINDING`.
* [ ] El campo `Verdict` es `DESIGN_CHANGES_REQUESTED` o `REJECTED`.
* [ ] `git status` muestra 0 modificaciones en `*/src/`.

### 2. Grader LLM-as-judge (Semantic)
* Justifica el rechazo basándose en la autonomía de Bounded Contexts y el desacoplamiento mediante IDs foráneos / eventos RabbitMQ.
