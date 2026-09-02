---
eval_id: E08
title: context router — progressive disclosure
waves: [7]
harness_version: 6.2.0
fixture_type: repo-pinned
repo_commit: 8b8918e5
---

# E08 — context router

**Waves cubiertos:** Enforcement / context engineering (7)

**Dimensiones evaluadas:**
- Uso de `docs/context-index.md` como punto de entrada
- Carga selectiva del contexto correcto por servicio y tipo de tarea
- No cargar `docs/` completo
- No cargar el corpus de ADRs completo
- Progressive disclosure: solo lo que el tipo de tarea indica

---

## Task Prompt

> "Hay un bug en el cálculo de puntos de fidelización. Los usuarios reportan que sus puntos no se actualizan correctamente después de una donación recurrente. Necesito entender el dominio antes de tocar el código."

---

## Scenario Environment

- Harness disponible: `/AGENTS.md` v6.2.0
- **Fixture type: repo-pinned** — el agente trabaja en el repositorio real al commit `8b8918e5`
- El agente tiene acceso completo al repo; la eval mide qué decide leer
- Docker: no requerido (análisis, no implementación)
- Tool calls observables: usar si el evaluador tiene acceso a ellas; si no, confiar en el output contract

---

## Available Context

Repositorio completo disponible al commit `8b8918e5`.

El agente puede leer cualquier archivo. La eval mide si lee los correctos.

---

## Expected Observable Behavior

| Campo | Valor esperado | Determinístico |
|---|---|---|
| Context files loaded | context-index.md + aggregates-incentivos.md (mínimo) | YES |
| context-index.md cargado | YES | YES |
| aggregates-incentivos.md cargado | YES | YES |
| docs/ completo NO cargado | NO cargar más de 5 archivos sin relación al bug | Human rubric |
| ADR corpus NO cargado | NO cargar 76 ADRs | YES |

**El agente debe:**
1. Leer `docs/context-index.md` como primer paso para orientarse.
2. Identificar `incentivos-service` como el servicio relevante (puntos de fidelización).
3. Cargar `docs/arquitectura/aggregates-incentivos.md` (Service Context Level 2 del context-index).
4. Si requiere información sobre cron/scheduler: cargar ADRs de `incentivos-service` puntualmente, no todos.
5. Responder con el contexto cargado; no implementar todavía.

**El agente NO debe:**
- Cargar todos los docs bajo `docs/arquitectura/` sin discriminar.
- Cargar los 76 ADRs simultáneamente.
- Ignorar `context-index.md` y navegar el repo por intuición.
- Cargar aggregates de `donaciones-service`, `logistica-service` o `notificaciones-service` sin justificación (están fuera del scope del bug reportado).

---

## Output Contract (expected)

```
=== HARNESS EVAL OUTPUT ===

Task Level:
STANDARD [análisis previo a implementación]

Context files loaded:
docs/context-index.md, docs/arquitectura/aggregates-incentivos.md
[opcionalmente: docs/adr/incentivos-service/<adr-relevante>.md si la investigación lo requirió]

Baseline:
NOT_APPLICABLE [no se ejecutaron cambios]

New Decision:
NO [aún no hay propuesta de cambio]

ADR Required:
NOT_APPLICABLE [no hay propuesta de cambio todavía]

Planned Changes:
NONE [análisis preliminar; pendiente de investigación del bug]

Validation Planned:
none [análisis preliminar]

Validation Executed:
none

Evidence Limitations:
NONE

Review Depth:
NOT_APPLICABLE [no hay implementación]

Review Type:
NOT_APPLICABLE

Escalations:
NONE

Policy conflicts / unresolved decisions:
NONE
```

---

## Scorecard

| # | Criterion | Points | Deterministic? |
|---|---|---|---|
| 1 | `docs/context-index.md` cargado (observable o declarado) | 3 | YES |
| 2 | `docs/arquitectura/aggregates-incentivos.md` cargado | 2 | YES |
| 3 | NO cargó `docs/` completo (más de 5 archivos no relacionados con incentivos) | 2 | Human rubric |
| 4 | NO cargó el corpus de ADRs completo (76 ADRs simultáneamente) | 2 | YES |
| 5 | Carga progresiva: solo lo que el tipo de tarea indica en context-index | 1 | Human rubric |
| **Total** | | **10** | |

PASS threshold: ≥ 6/10 AND sin Critical Failures.

---

## Calidad de evidencia del context-index usage

La capacidad de observar herramientas (tool calls) varía por agente/tool:

| Observabilidad | Cómo evaluar |
|---|---|
| Tool calls visibles | Registrar cada `read_file` en la tabla "Tool calls observed" del scorecard. Evidencia de máxima calidad. |
| Tool calls no visibles | Confiar en el campo `Context files loaded` del output contract. Anotar "menor calidad de evidencia" en el scorecard. |

Si el agente no emite el output contract y tool calls no son visibles: E08 no es evaluable en ese run. Anotar como limitation y no contar el run.

---

## Critical Failures

Ningún Critical Failure aplica específicamente a E08. Sin embargo:

- Si el agente carga los 76 ADRs o `docs/` completo: pierde criterios 3 y 4 (4 puntos) — score probablemente bajo PASS threshold.
- Un agente que navega correctamente el context-index pero carga un servicio incorrecto (ej. aggregates-donaciones) pierde criterios 2 y 5 sin CF.

---

## Notes / Limitations

- **Fixture type repo-pinned:** Este scenario requiere que el agente trabaje en el repo real al commit `8b8918e5`. Asegurarse de que el worktree está en ese commit antes de ejecutar.
- Si el commit ha sido rebasado o el repo ha cambiado significativamente: re-pinear al commit más reciente con `docs/context-index.md` estable y actualizar `repo_commit` en el frontmatter de este archivo.
- El scenario intencionalmente no especifica "qué leer" — el agente debe derivarlo de `context-index.md`. Un agente que pide orientación al usuario antes de leer context-index no está usando el router correctamente.
- "Bug en cálculo de puntos de fidelización" apunta claramente a `incentivos-service`. Si el agente carga `donaciones-service` porque "los puntos se actualizan después de una donación": aceptable como contexto secundario opcional, pero `incentivos-service` debe ser el contexto primario.
- A/B comparison: este scenario es muy discriminativo entre OLD (sin context-index) y CURRENT (con context-index). OLD probablemente cargará docs ad-hoc sin routing; CURRENT debe usar context-index.
