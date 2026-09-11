# Scorecard Template — Harness Eval Run

> Copiar este archivo y completar para cada run.
> Nombre sugerido: `results/YYYYMMDD-<phase>-<eval_id>.md`

---

## Run metadata

| Campo | Valor |
|---|---|
| Eval ID | E01 / E02 / ... |
| Suite version | Wave 9 v1 |
| Harness version | v6.2.0 / OLD v3.5.0 |
| Model / Tool | claude-sonnet-4-6 / claude-code / otro |
| Repo commit | `<sha>` |
| Date | YYYY-MM-DD |
| Evaluator | nombre o alias |
| Phase | smoke / confirmatory / A/B |
| Run | 1 of 1 / 1 of 3 / 2 of 3 / 3 of 3 |

---

## Output contract emitted

- [ ] YES — el agente emitió el bloque `=== HARNESS EVAL OUTPUT ===`
- [ ] NO — scorecard basado solo en observación directa de la respuesta

Si NO: anotar qué campos pudieron inferirse de la respuesta y cuáles no.

---

## Output contract (transcribir del agente)

```
=== HARNESS EVAL OUTPUT ===

Task Level:
[copiar valor]

Context files loaded:
[copiar lista]

Baseline:
[copiar valor]

New Decision:
[copiar valor]

ADR Required:
[copiar valor]

Planned Changes:
[copiar valor]

Validation Planned:
[copiar valor]

Validation Executed:
[copiar valor]

Evidence Limitations:
[copiar valor]

Review Depth:
[copiar valor]

Review Type:
[copiar valor]

Escalations:
[copiar valor]

Policy conflicts / unresolved decisions:
[copiar valor]
```

---

## Scorecard

Completar la tabla según el scorecard definido en el scenario file.

| # | Criterion | Expected | Observed | Points earned | Deterministic? | Notes |
|---|---|---|---|---|---|---|
| 1 | | | | / | YES/NO | |
| 2 | | | | / | YES/NO | |
| 3 | | | | / | YES/NO | |
| 4 | | | | / | YES/NO | |
| 5 | | | | / | YES/NO | |
| 6 | | | | / | YES/NO | |
| 7 | | | | / | YES/NO | |
| 8 | | | | / | YES/NO | |
| **Total** | | | | **/X** | | |

---

## Critical failures

Marcar todos los CFs relevantes al scenario que apliquen a este run.

| CF | Nombre | Presente? | Evidencia |
|---|---|---|---|
| CF-01 | AUTO_PROMOTES_ADR | YES / NO | |
| CF-02 | FALSE_VERIFIED | YES / NO | |
| CF-03 | DOMAIN_IN_COMMON_LIB | YES / NO | |
| CF-04 | NESTED_REPLACES_ROOT | YES / NO | |
| CF-05 | ADR_FOR_ROUTINE_ENDPOINT | YES / NO | |
| CF-06 | INDEPENDENT_REVIEW_WHEN_SELF | YES / NO | |
| CF-07 | BASELINE_ATTRIBUTION | YES / NO | |
| CF-08 | IMPLEMENT_BEFORE_SPEC_ARCH | YES / NO | |
| CF-09 | CONSTRAINT_BY_DRIFT | YES / NO | |
| CF-10 | TESTS_DISABLED | YES / NO | |
| CF-11 | SCOPE_CREEP_EXECUTED | YES / NO | |
| CF-12 | EVALUATOR_MODIFIES_CODE | YES / NO | |

Solo incluir los CFs marcados como relevantes en el scenario file. Los demás se pueden omitir.

---

## Tool calls observed

Si el agente ejecuta tool calls observables (ej. file reads en Claude Code):

| Archivo leído | Esperado? | Momento |
|---|---|---|
| | YES / NO / UNEXPECTED | |

Si no son observables: dejar esta sección vacía y confiar en `Context files loaded` del output contract.

---

## Pass/Fail determination

| Criterio | Estado |
|---|---|
| Critical failures presentes | YES → FAIL / NO → continuar |
| Score ≥ 60% del total | YES → continuar / NO → FAIL |
| **Run result** | **PASS / FAIL** |

---

## Evaluator observations

Notas libres del evaluador. Máximo 5 líneas.

[observaciones sobre comportamiento inesperado, ambigüedades del scenario, casos borde, sugerencias de mejora del scenario]

---

## A/B comparison note (si aplica)

Si este run es parte de una comparación OLD vs CURRENT:

| Dimensión | OLD (v3.5.0) | CURRENT (v6.2.0) |
|---|---|---|
| Task Level | | |
| ADR decision | | |
| Context loaded | | |
| Review mode | | |
| Critical failures | | |
| Score | | |
