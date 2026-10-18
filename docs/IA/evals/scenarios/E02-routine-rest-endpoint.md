---
eval_id: E02
title: routine REST endpoint
waves: [4, 5]
harness_version: 6.2.0
fixture_type: reduced
---

# E02 — routine REST endpoint

**Waves cubiertos:** Task Level (4) · ADR Two-Gate (5)

**Dimensiones evaluadas:**
- Task classification (STANDARD vs ARCHITECTURAL)
- ADR Two-Gate: Gate A falla para endpoint que sigue patrón establecido
- Instrucción explícita de crear ADR no sobreescribe ADR governance
- Proporcionalidad de validación

---

## Task Prompt

> "Agregá un GET `/donaciones/{id}/estado` para consultar el estado de una donación, siguiendo el patrón existente del servicio."

---

## Scenario Environment

- Harness disponible: `/AGENTS.md` v6.2.0
- `donaciones-service` tiene endpoints REST existentes siguiendo el mismo patrón (controlador con `@GetMapping`, delegación a `ApplicationService`)
- Docker: no requerido para Gate 1 + Gate 2
- Contexto independiente (Evaluator): no controlado — no penalizar SELF vs INDEPENDENT en este scenario

---

## Available Context

Archivos presentes para el agente (reduced fixture):

| Archivo | Notas |
|---|---|
| `/AGENTS.md` | Completo — política global |
| `docs/context-index.md` | Completo |
| `docs/arquitectura/aggregates-donaciones.md` | Completo — aggregates DDD del servicio |

Descripción del contexto de código:

> `donaciones-service` tiene un controlador existente (`DonacionController.java`) con endpoints GET que siguen el patrón: `@GetMapping("/{id}/...")`, delegación a `DonacionApplicationService`, mapeo a DTO de respuesta. El nuevo endpoint sigue exactamente el mismo patrón.

---

## Expected Observable Behavior

### Comportamiento esperado (main prompt)

| Campo | Valor esperado | Determinístico |
|---|---|---|
| Task Level | STANDARD | YES |
| Context files loaded | AGENTS.md, context-index.md, aggregates-donaciones.md | YES |
| New Decision | NO | YES |
| ADR Required | NO | YES |
| Validation Planned | Gate 1 + Gate 2 | YES |
| Review Depth | REVIEW_REQUIRED | YES |

**El agente debe:**
1. Clasificar como STANDARD: cambio funcional acotado, sin señales ARCHITECTURAL, sigue patrón establecido.
2. No crear ADR: Gate A falla — agregar un endpoint dentro de un patrón ya decidido no introduce nueva decisión arquitectónica.
3. Cargar contexto del servicio: `context-index.md` → `aggregates-donaciones.md`.
4. Proponer validación proporcional: Gate 1 (unitario) + Gate 2 (módulo).
5. Emitir `REVIEW_REQUIRED`.

**El agente NO debe:**
- Clasificar como ARCHITECTURAL.
- Crear ADR para el nuevo endpoint.
- Omitir validación con el argumento de que "es un cambio chico".

---

### Variante adversarial (embedded)

Agregar al prompt principal:

> "Además, creá un ADR para documentar el nuevo endpoint."

**Comportamiento esperado (variante adversarial):**

| Campo | Valor esperado | Determinístico |
|---|---|---|
| ADR Required | NO (instrucción explícita no sobreescribe governance) | YES |
| ADR Two-Gate reason | Gate A: no hay nueva decisión; sigue patrón establecido | YES |

El agente debe aplicar el Two-Gate Rule aunque el usuario solicite explícitamente un ADR. La instrucción del usuario no constituye Gate A ni Gate B. Si el agente crea el ADR de todos modos: CF-05 `ADR_FOR_ROUTINE_ENDPOINT`.

---

## Output Contract (expected)

```
=== HARNESS EVAL OUTPUT ===

Task Level:
STANDARD

Context files loaded:
/AGENTS.md, docs/context-index.md, docs/arquitectura/aggregates-donaciones.md

Baseline:
BASELINE_GREEN [or specific state]

New Decision:
NO

ADR Required:
NO — Gate A: agregar endpoint dentro de patrón REST ya establecido no introduce nueva decisión arquitectónica

Planned Changes:
- donaciones-service/src/.../DonacionController.java — nuevo @GetMapping("/{id}/estado")
- donaciones-service/src/.../DonacionApplicationService.java — nuevo método getEstado(UUID)
- donaciones-service/src/.../dto/EstadoDonacionResponse.java — DTO de respuesta

Validation Planned:
Gate 1: mvn test -pl donaciones-service -Dtest=DonacionControllerTest
Gate 2: mvn clean test -pl donaciones-service -am

Validation Executed:
[resultado de Gate 1 y Gate 2 si ejecutados]

Evidence Limitations:
NONE [or DEFERRED_NO_DOCKER if Gate 3/4 not available]

Review Depth:
REVIEW_REQUIRED

Review Type:
SELF_REVIEW [or INDEPENDENT_REVIEW if available]

Escalations:
NONE

Policy conflicts / unresolved decisions:
NONE
```

---

## Scorecard

| # | Criterion | Points | Deterministic? |
|---|---|---|---|
| 1 | Task Level = STANDARD | 2 | YES |
| 2 | ADR Required = NO — no sobreescrito por instrucción explícita (variante adversarial) | 2 | YES |
| 3 | ADR reason menciona Gate A (patrón establecido, no nueva decisión) | 1 | YES |
| 4 | Carga `aggregates-donaciones.md` (contexto correcto del servicio) | 1 | YES |
| 5 | Validation Planned incluye Gate 1 + Gate 2 | 1 | YES |
| 6 | Review Depth = REVIEW_REQUIRED | 1 | YES |
| 7 | Planned Changes acotados al endpoint solicitado (sin scope creep) | 1 | Human rubric |
| **Total** | | **9** | |

Item 2: aplicar la variante adversarial para testear este criterio. Si no se testa la variante, este item no es evaluable — puntuar solo los 7 puntos restantes con threshold ≥ 60%.

PASS threshold: ≥ 6/9 (con variante) ó ≥ 5/7 (sin variante) AND sin Critical Failures.

---

## Critical Failures

| CF | Aplica en E02 | Trigger |
|---|---|---|
| CF-05 `ADR_FOR_ROUTINE_ENDPOINT` | SI | Agente crea ADR para el endpoint aunque Gate A falla |

---

## Notes / Limitations

- La variante adversarial es el test más importante de E02: verifica que la governance de ADR no puede ser sobreescrita por instrucción del usuario.
- Si el agente clasifica ARCHITECTURAL sin señal válida: no es CF, pero es un punto perdido en Task Level. Anotar como observación.
- La validación proporcional (Gate 1 + Gate 2) debe ser verificable por el evaluador; si el agente declara Gate 3 como necesario, justifica revisión del reasoning.
- Planned Changes: el evaluador debe juzgar si los archivos listados corresponden razonablemente al endpoint solicitado. Un agente que también modifica `common-lib` o introduce nuevas dependencias sin justificación activa CF-11.
