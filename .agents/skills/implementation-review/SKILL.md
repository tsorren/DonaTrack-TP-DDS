---
name: implementation-review
description: >-
  Evaluación adversarial de código implementado (Implementation Evaluator) basada en los
  vectores V1–V9 de docs/IA/review/evaluator.md en modo SOURCE_READ_ONLY y emisión de Review Contract.
---

# Skill: implementation-review — Evaluación Adversarial de Código (V1–V9)

> **Ámbito:** Revisión crítica e independiente de código modificado o nuevo en DonaTrack.  
> **Alineación Canónica:** [`docs/IA/review/evaluator.md`](../../../docs/IA/review/evaluator.md), [`AGENTS.md`](../../../AGENTS.md) §4, §7.4, §11.

---

## 1. Propósito y Modo de Operación

Esta skill actúa como el **Implementation Evaluator**. Opera estrictamente en modo **`SOURCE_READ_ONLY` + `NON_DESTRUCTIVE_VERIFICATION`**:
* **No modifica** código fuente, documentación ni configuración.
* **No aplica fixes** durante la evaluación.
* **Puede ejecutar** comprobaciones no destructivas (`mvn spotless:check`, tests focalizados, `git diff`).
* Adopta una postura adversarial: **no asume que las afirmaciones del implementador son ciertas** (*Evidence > Claims*).

### Modos de Independencia:
* **`INDEPENDENT_REVIEW` (Preferido):** Invocado mediante un subagente o contexto secundario aislado. El evaluador recibe solo los artefactos observables (diff, log de tests, spec) y no tiene acceso al razonamiento previo del generador.
* **`SELF_REVIEW` (Fallback):** Si no hay subagentes disponibles, el mismo contexto asume el rol declarando explícitamente `Mode: SELF_REVIEW` y aplicando con igual rigor el Review Contract.

---

## 2. Auditoría de los 9 Vectores Canónicos (V1–V9)

| Vector | Dimensión Evaluada | Criterio de Falla (BLOCKING) |
|:---:|---|---|
| **V1** | **Regresiones** | Tests existentes que fallan respecto al baseline; manejo deficiente de casos de borde (`null`, vacío, duplicado). |
| **V2** | **Invariantes Arquitectónicas** | Lógica de negocio dentro de controllers; entidades de dominio acopladas a DTOs o HTTP; Shared Kernel contaminado con reglas de microservicio. |
| **V3** | **Contratos** | Ruptura de contratos REST/AMQP existentes; cambio de firmas públicas sin compatibilidad retroactiva. |
| **V4** | **Calidad de Tests** | Aserciones debilitadas (`assertTrue(true)`), tests sin aserciones, tests deshabilitados con `@Disabled` oportunista. |
| **V5** | **Scope Creep** | Archivos modificados o refactors no solicitados ni relacionados con el objetivo de la tarea. |
| **V6** | **Factualidad de Evidencia** | Discrepancias entre lo que el implementador afirma haber ejecutado y el `git diff` real. |
| **V7** | **SonarCloud Pre-Flight** | Introducción de nuevos code smells, bloques comentados, excepciones genéricas o inyecciones por atributo. |
| **V8** | **Seguridad y Privacidad** | Presencia de credenciales, tokens, passwords hardcodeados o datos personales reales (PII). Todo debe ser sintético. |
| **V9** | **Grafo Documental** | Falta de actualización de `docs/README.md`, `ESTADO_DOCUMENTACION.md` o ADRs cuando el cambio afectó la arquitectura. |

---

## 3. Emisión del Review Contract Canónico

El Evaluator debe emitir el reporte estructurado observable según [`docs/IA/review/evaluator.md`](../../../docs/IA/review/evaluator.md):

```markdown
=== REVIEW CONTRACT ===

Task ID / Scope: [<descripción de la tarea>]
Evaluator Mode: INDEPENDENT_REVIEW | SELF_REVIEW
Working Directory: [<ruta del módulo>]

Vector Audit:
  [V1] Regressions:               [OK | FINDING: <detalle>]
  [V2] Architectural Invariants:  [OK | FINDING: <detalle>]
  [V3] Contracts:                 [OK | FINDING: <detalle>]
  [V4] Test Quality & Integrity:  [OK | FINDING: <detalle>]
  [V5] Anti-Scope Creep:          [OK | FINDING: <detalle>]
  [V6] Evidence vs Diff:          [OK | FINDING: <detalle>]
  [V7] SonarCloud Pre-Flight:     [OK | FINDING: <detalle>]
  [V8] Security & Secrets:        [OK | FINDING: <detalle>]
  [V9] Document Graph Integrity:  [OK | FINDING: <detalle>]

Deterministic Checks Run by Evaluator:
  - mvn spotless:check ➔ [PASS / FAIL]
  - mvn test -pl <modulo> -Dtest=<Test> ➔ [PASS / FAIL]

Findings Summary:
  - BLOCKING: [Lista de hallazgos que bloquean la aprobación]
  - NON_BLOCKING: [Hallazgos menores o sugerencias de deuda técnica]

Result: PASS | BLOCKING_FINDINGS | ESCALATE_TO_HUMAN
```

---

## 4. Veredictos y Política de Loops

* **`PASS`:**
  * 0 hallazgos `BLOCKING`.
  * La implementación está formalmente aprobada para merge o reporte final.
* **`BLOCKING_FINDINGS`:**
  * Existen hallazgos bloqueantes.
  * El control retorna a `implement-task` para reparar los defectos señalados.
  * **Cota de Reintentos (Repair Loop):** Máximo 3 iteraciones de reparación.
* **`ESCALATE_TO_HUMAN`:**
  * Se activa de inmediato si se alcanza el intento 3 sin resolución satisfactoria o si se descubre una regresión severa no aislable.
  * Pausa la ejecución y presenta el informe diagnóstico al usuario.
