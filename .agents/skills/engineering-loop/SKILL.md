---
name: engineering-loop
description: >-
  Workflow maestro compuesto para el ciclo de vida del desarrollo guiado por agentes en DonaTrack.
  Implementa enrutamiento dinámico por criticidad (QUICK/STANDARD/ARCHITECTURAL), bucles de reparación acotados y human gates.
---

# Skill: engineering-loop — Workflow Maestro y Enrutador Dinámico

> **Ámbito:** Orquestación secuencial y gobierno de tareas de desarrollo en DonaTrack.  
> **Alineación Normativa:** [`AGENTS.md`](../../AGENTS.md) §7.0–§7.4, [`docs/IA/review/evaluator.md`](../../docs/IA/review/evaluator.md), y directivas de control de flujo de [`docs/agentes/`](../../docs/agentes/).

---

## 1. Naturaleza y Principios de Diseño

Esta skill actúa como un **Composite Workflow Runner**. No es un daemon externo ni un orquestador transaccional; es un procedimiento estructurado que previene la degradación arquitectónica y el síndrome de *Graphitis* mediante **enrutamiento condicional por riesgo**.

### Principios Fundamentales:
1. **Enrutamiento por Criticidad (Anti-Graphitis):** No se somete todo cambio a una máquina de estados monolítica. El esfuerzo y la ceremonia deben ser proporcionales al impacto del cambio.
2. **Límites de Reintento Deterministas (Cota = 3):** Los bucles de retorno nunca son infinitos. Al superar 3 reintentos en diseño o código, se fuerza una parada a `HUMAN_GATE_REQUIRED`.
3. **Higiene de Estado:** Cualquier archivo de checkpoint en `.agents/state/` es estrictamente temporal y debe purgarse al concluir la tarea.

---

## 2. Enrutador Dinámico de Tareas (Task-Level Router)

Al recibir una tarea, el agente debe evaluar las señales de `AGENTS.md` §7.0 para clasificar el nivel:

```text
¿Cumple TODOS los criterios QUICK? 
  ├── SÍ ──► NIVEL QUICK
  └── NO ──► ¿Activa alguna señal ARCHITECTURAL?
               ├── SÍ ──► NIVEL ARCHITECTURAL
               └── NO ──► NIVEL STANDARD
```

### Rutas de Ejecución:

### Ruta A: Tareas `QUICK` (Modo Inline)
* **Criterio:** No cambia runtime, no cambia contratos, mecánico y local, sin cambios estructurales en Java.
* **Pipeline:**
  1. Invocar directamente `implement-task` (modo inline).
  2. Ejecutar `LIGHTWEIGHT_CLOSING_CHECK` de `evaluator.md` §5.
  3. Finalizar sin crear specs ni Review Contracts.

### Ruta B: Tareas `STANDARD` (Flujo Acotado)
* **Criterio:** Cambio funcional o de lógica interna acotado, sin señales arquitectónicas, reversible localmente.
* **Pipeline:**
  1. `define-spec`: Redactar spec ligera centrada en alcance y criterios de aceptación en `docs/specs/active/<task-id>-spec.md`.
  2. Bypass de `design-spec` y `design-review` (no requiere formalización técnica pre-código).
  3. `implement-task`: TDD quirúrgico y gates locales.
  4. `implementation-review`: Auditoría de vectores V1–V9 y Review Contract canónico.
  5. Si hay hallazgos bloqueantes ➔ Repair loop (máx 3). Si pasa ➔ Finalizar.

### Ruta C: Tareas `ARCHITECTURAL` (Ciclo Completo Riguroso)
* **Criterio:** Toca contratos públicos, bounded contexts, Shared Kernel (`common-lib`), persistencia, seguridad o dependencias.
* **Pipeline Completo:**
  1. `define-spec`: Elicitación activa con el usuario, 0 inferencias en negocio y matriz 5D de trade-offs.
  2. `design-spec`: Mapeo técnico a clases Java 21, Two-Gate Rule para ADRs y plan TDD.
  3. `design-review`: Evaluador adversarial D1–D6 y emisión de `Design Review Contract`.
     * Si `CHANGES_REQUESTED` ➔ Loop 1 a `design-spec` (máx 3 intentos).
     * Si `HUMAN_GATE_REQUIRED` ➔ Pausa por decisión estructural o límite de reintentos superado.
  4. `implement-task`: TDD estricto, baseline previo y auto-auditoría SonarCloud.
  5. `implementation-review`: Evaluador de código V1–V9 en modo `SOURCE_READ_ONLY`.
     * Si `BLOCKING_FINDINGS` ➔ Loop 2 a `implement-task` (máx 3 intentos).
     * Si `PASS` ➔ Finalizar.

---

## 3. Matriz de Estados y Contadores de Reintento

```text
┌────────────────────────────────────────────────────────────────────────┐
│                   MÁQUINA DE ESTADOS COMPUESTA                        │
│                                                                        │
│  [TASK_INIT] ──► [ROUTER]                                              │
│                     │                                                  │
│                     ├── QUICK ──────────► [IMPLEMENT_INLINE] ──► [END] │
│                     │                                                  │
│                     ├── STANDARD ───────► [SPEC_LIGHT]                 │
│                     │                         │                        │
│                     │                         ▼                        │
│                     │                 [IMPLEMENT_TASK]                 │
│                     │                         │                        │
│                     │                         ▼                        │
│                     │                 [IMPL_REVIEW] ──► [PASS] ──► [END│
│                     │                         │                        │
│                     │                   [BLOCKING] (retry <= 3)        │
│                     │                         └──► vuelve a IMPLEMENT │
│                     │                                                  │
│                     └── ARCHITECTURAL ──► [DEFINE_SPEC]                │
│                                               │                        │
│                                               ▼                        │
│                                         [DESIGN_SPEC] ◄────────┐       │
│                                               │                │       │
│                                               ▼                │       │
│                                         [DESIGN_REVIEW]        │       │
│                                               ├── CHANGES ─────┘       │
│                                               ├── HUMAN_GATE ──► [STOP]│
│                                               └── APPROVED             │
│                                                       │                │
│                                                       ▼                │
│                                               [IMPLEMENT_TASK] ◄─┐     │
│                                                       │          │     │
│                                                       ▼          │     │
│                                               [IMPL_REVIEW]      │     │
│                                                       ├── BLOCK ─┘     │
│                                                       └── PASS ──► [END]
└────────────────────────────────────────────────────────────────────────┘
```

### Reglas de Escape y Human Escalation:
* Si el contador de reintentos en diseño (`design_retries`) alcanza 3:  
  **Estado:** `ESCALATE_TO_HUMAN`. Detener el avance automático, resumir los puntos de fricción y solicitar guía al usuario.
* Si el contador de reintentos en implementación (`code_retries`) alcanza 3:  
  **Estado:** `ESCALATE_TO_HUMAN`. Detener la ejecución, emitir informe de defectos persistentes y ofrecer rollback con `git checkout` si se ensució el working tree.

---

## 4. Finalización y Limpieza de Estado (Finalize)
Al alcanzar el estado `PASS` en cualquier ruta:
1. **Mover Spec Finalizada:** Trasladar `docs/specs/active/<task-id>-spec.md` a `docs/specs/completed/<task-id>-spec.md`.
2. **Sincronización Documental:** Actualizar `docs/README.md` y `docs/ESTADO_DOCUMENTACION.md` si se introdujeron nuevas capacidades o documentos.
3. **Purga de Checkpoint:** Si existiera un archivo de estado temporal en `.agents/state/active-task-state.json`, eliminarlo para garantizar que la próxima sesión inicie limpia.
4. **Reporte de Entrega:** Emitir el reporte de cierre bajo la plantilla canónica de [`docs/IA/04-checklist-antes-de-pr.md`](../../docs/IA/04-checklist-antes-de-pr.md).
