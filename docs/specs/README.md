# Especificaciones Técnicas y Funcionales (Specs) — DonaTrack

> **Catálogo Canónico de Especificaciones de Software (SDD)**  
> **Alineación Normativa:** [`AGENTS.md`](../../AGENTS.md) §2, §7, [`docs/context-index.md`](../context-index.md) y [`informe-agent-friendly-repository-architecture.md`](../agentes/informe-agent-friendly-repository-architecture.md).

---

## 1. Propósito

Este directorio constituye la **fuente de verdad (*System of Record*)** para los requerimientos funcionales y el diseño técnico detallado de tareas medianas y grandes en DonaTrack.

En consonancia con la disciplina **Spec-Driven Development (SDD)**:
* Ninguna tarea `STANDARD` o `ARCHITECTURAL` se implementa sin un spec previo.
* Las decisiones de alcance acordadas con el usuario y los trade-offs analizados quedan plasmados aquí para transferir contexto entre distintas sesiones de agentes o desarrolladores humanos.

---

## 2. Estructura de Directorios

```text
docs/specs/
├── README.md              # Este documento normativo
├── active/                # Especificaciones en curso de diseño o implementación
└── completed/             # Especificaciones implementadas, verificadas y cerradas
```

---

## 3. Ciclo de Vida de una Spec

```text
[Requerimiento] ──► skill: define-spec
                         │
                         ▼
        docs/specs/active/<task-id>-spec.md  (Status: APPROVED_BY_USER)
                         │
                         ▼ (si es ARCHITECTURAL)
                    skill: design-spec
                         │
                         ▼
                    skill: design-review (Status: DESIGN_APPROVED)
                         │
                         ▼
                    skill: implement-task
                         │
                         ▼
                    skill: implementation-review (Status: PASS)
                         │
                         ▼
       docs/specs/completed/<task-id>-spec.md
```

---

## 4. Convención de Nomenclatura

Los archivos deben seguir el formato:
* `docs/specs/active/SPEC-<id>-<nombre-descriptivo>.md`
* Ejemplo: `docs/specs/active/SPEC-01-donaciones-mision-validacion.md`

Al completar la implementación y pasar todos los Quality Gates, el archivo se traslada a `docs/specs/completed/` como registro histórico inmutable del desarrollo.

---

## 5. Índice de Especificaciones Completadas

| ID | Título y Enlace | Nivel | Estado | Alcance / Propósito Principal |
|:---:|---|:---:|:---:|---|
| **SPEC-01** | [`SPEC-01-consolidacion-nivel-4-agent-first.md`](completed/SPEC-01-consolidacion-nivel-4-agent-first.md) | ARCHITECTURAL | `COMPLETED` | Consolidación del repositorio a Nivel 4 (Agent-First Pleno): arnés SDD, suite de 7 skills, generated knowledge, benchmark evals y governance checks. |
| **SPEC-02** | [`SPEC-02-harness-skill-review-pr-adversarial-grepai.md`](completed/SPEC-02-harness-skill-review-pr-adversarial-grepai.md) | ARCHITECTURAL | `COMPLETED` | Implementación de la skill canónica `review-pr`, protocolo GrepAI-First, scripts CLI de contexto y ADR de formalización. |

