---
name: design-review
description: >-
  Evaluador adversarial de especificaciones y diseño técnico pre-código (Design Evaluator).
  Aplica los vectores D1–D6 y emite el Design Review Contract en modo SOURCE_READ_ONLY.
---

# Skill: design-review — Evaluación Adversarial de Diseño Pre-Código

> **Ámbito:** Revisión crítica y pre-flight arquitectónico antes de modificar código fuente.  
> **Alineación Normativa:** [`AGENTS.md`](../../AGENTS.md) §4, §6, §9 y [`docs/IA/review/evaluator.md`](../../docs/IA/review/evaluator.md).

---

## 1. Propósito y Modo de Operación

Esta skill actúa como el **Design Evaluator escéptico y adversarial**. Su función es cuestionar activamente los supuestos del diseño técnico antes de que se escriba una sola línea de código fuente (`SOURCE_READ_ONLY`), detectando sobreingeniería, acoplamientos ilegítimos o violaciones de contratos.

### Modo de Independencia:
* **`INDEPENDENT_REVIEW` (Recomendado):** Se invoca a través de un subagente o contexto secundario aislado pasando exclusivamente el artefacto de especificación de [`docs/specs/active/`](../../docs/specs/). El evaluador no hereda el chain-of-thought del generador.
* **`SELF_REVIEW` (Fallback):** Si no hay soporte de subagentes en la herramienta, el evaluador opera en el mismo contexto pero debe declarar formalmente `Mode: SELF_REVIEW` y adoptar una postura adversarial estricta.

---

## 2. Vectores de Revisión de Diseño (D1–D6)

El evaluador debe auditar sistemáticamente los siguientes 6 vectores:

| Vector | Dimensión Evaluada | Criterio de Aceptación / Rechazo |
|:---:|---|---|
| **D1** | **Invariantes Arquitectónicas** | ¿Respeta la separación de capas? Controllers adaptadores puros; Application Services orquestadores; entidades con lógica de negocio; sin acoplamiento a detalles de infraestructura. |
| **D2** | **Pureza de `common-lib`** | ¿Se intenta ubicar lógica de dominio, enums específicos de negocio o dependencias cruzadas en el Shared Kernel? Rechazo inmediato si viola [`common-lib/AGENTS.md`](../../common-lib/AGENTS.md). |
| **D3** | **Retrocompatibilidad de Contratos** | ¿La modificación en DTOs o mensajes AMQP es estrictamente aditiva? Prohibido renombrar o eliminar campos públicos sin un ciclo de migración formal. |
| **D4** | **Gobernanza de ADRs** | ¿Cumple la Two-Gate Rule? Si introduce nueva decisión significativa, ¿está marcada como `proposed`? **Invariante:** Prohibición absoluta de auto-promoción a `accepted`. |
| **D5** | **Anti-Scope Creep & YAGNI** | ¿El diseño introduce abstracciones innecesarias, patrones no solicitados o refactorings oportunistas ajenos al objetivo de la tarea? |
| **D6** | **Testeabilidad y Determinismo** | ¿El plan TDD incluye tests reproducibles con datos sintéticos? ¿Se especifican aserciones verificables sin depender de mocks excesivos? |

---

## 3. Emisión del Design Review Contract

El Evaluator debe emitir el siguiente contrato formal y anexarlo a la especificación en `docs/specs/active/<task-id>-spec.md`:

```markdown
=== DESIGN REVIEW CONTRACT ===

Task / Spec: [docs/specs/active/<task-id>-spec.md]
Mode: INDEPENDENT_REVIEW | SELF_REVIEW
Evaluator Role: SOURCE_READ_ONLY (Pre-Code Verification)

Vector Audit:
  [D1] Architectural Invariants:  [OK | FINDING: <detalle>]
  [D2] Shared Kernel Purity:      [OK | FINDING: <detalle>]
  [D3] Contract Compatibility:    [OK | FINDING: <detalle>]
  [D4] ADR Two-Gate Governance:   [OK | FINDING: <detalle>]
  [D5] Anti-Scope Creep (YAGNI):  [OK | FINDING: <detalle>]
  [D6] Testability & Determinism: [OK | FINDING: <detalle>]

Findings Summary:
  - BLOCKING (Impiden implementar): [Lista de hallazgos bloqueantes]
  - NON-BLOCKING (Sugerencias):     [Lista de mejoras menores]

Verdict: DESIGN_APPROVED | DESIGN_CHANGES_REQUESTED | HUMAN_GATE_REQUIRED
```

---

## 4. Veredictos y Transiciones de Estado

* **`DESIGN_APPROVED`:**  
  * Cero hallazgos `BLOCKING`.
  * Se autoriza el avance a la fase de implementación quirúrgica con la skill `implement-task`.
* **`DESIGN_CHANGES_REQUESTED`:**  
  * Existen 1 o más hallazgos `BLOCKING`.
  * Se retorna el control a la skill `design-spec` para corregir la propuesta técnica.
  * **Cota de Escape:** Si el bucle de diseño alcanza 3 iteraciones sin aprobación, se transiciona obligatoriamente a `HUMAN_GATE_REQUIRED`.
* **`HUMAN_GATE_REQUIRED`:**  
  * Activado ante: 1) necesidad de un nuevo ADR que requiera decisión humana, 2) cambios en límites de microservicios, o 3) agotamiento del límite de 3 reintentos.
  * Se detiene la ejecución automática y se solicita la intervención del usuario.
