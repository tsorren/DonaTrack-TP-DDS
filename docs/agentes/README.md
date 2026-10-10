# Informes de Investigación en Desarrollo con Agentes IA — DonaTrack

> **Portal de Investigación Teórica y Aplicada en Agentic Software Engineering**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Alineación con [`AGENTS.md`](../../AGENTS.md) y [`.agents/skills/`](../../.agents/skills/)

---

## 1. Propósito

Este directorio alberga los informes de investigación técnica producidos por el equipo de DonaTrack sobre la integración avanzada de agentes autónomos de codificación (*Coding Agents*) en repositorios de software de gran escala. Estos documentos fundamentan el diseño del arnés de gobernanza, la suite de skills y la metodología Spec-Driven Development (SDD) implementada en el proyecto.

```text
docs/agentes/
├── README.md                                             # Este índice de navegación
├── informe-agent-friendly-repository-architecture.md     # 🏛️ Taxonomía de repositorios amigables para agentes
├── informe-harness-engineering-pi-gentle-pi.md           # ⚙️ Ingeniería de arneses y control de inferencias
├── informe-agent-graphs.md                               # 🕸️ Topología de grafos de agentes y prevención de graphitis
├── informe-orquestadores-control-planes-coding-agents.md # 🎛️ Estado del arte en control planes y orquestadores
└── evals-agentes-resumen.md                              # 📊 Síntesis ejecutiva de evaluación del harness
```

---

## 2. Compendio de Informes Técnicos

* [**Arquitectura de Repositorios Amigables para Agentes (`informe-agent-friendly-repository-architecture.md`)**](informe-agent-friendly-repository-architecture.md):  
  Define la taxonomía de madurez en 4 niveles (Level 1: Unstructured, Level 2: Documented, Level 3: Governed, Level 4: Agent-First Pleno). Analiza cómo estructurar el grafo documental, las invariantes arquitectónicas y el context routing para maximizar la eficacia de agentes con presupuesto finito de tokens.

* [**Ingeniería de Arneses: Protocolo Pi / Gentle-Pi (`informe-harness-engineering-pi-gentle-pi.md`)**](informe-harness-engineering-pi-gentle-pi.md):  
  Estudio sobre la prevención de alucinaciones e inferencias no fundamentadas mediante la taxonomía epistémica (`[OBSERVED]`, `[DOCUMENTED]`, `[INFERRED]`, `[PROPOSED]`, `[REJECTED]`, `[VERIFIED]`), y la formalización de la metodología Spec-Driven Development.

* [**Grafos de Agentes y Control de Flujo (`informe-agent-graphs.md`)**](informe-agent-graphs.md):  
  Investigación de patrones de orquestación multi-agente (Chains, Routers, Evaluator-Optimizer, Orchestrator-Workers) y análisis del antipatrón *Graphitis* (hiper-complejidad innecesaria en máquinas de estados de agentes).

* [**Orquestadores y Control Planes para Coding Agents (`informe-orquestadores-control-planes-coding-agents.md`)**](informe-orquestadores-control-planes-coding-agents.md):  
  Análisis comparativo de plataformas industriales (Claude Code, Cursor, Copilot Workspace, Antigravity) y diseño del control plane nativo en DonaTrack basado en scripts mecánicos ligeros y Quality Gates deterministas.

* [**Resumen Ejecutivo de Evaluaciones (`evals-agentes-resumen.md`)**](evals-agentes-resumen.md):  
  Consolidación de las métricas de evaluación del harness (E01 a E11), comparación A/B antes y después de la formalización de reglas, y reducción de fallas críticas (CF).

---

## 3. Navegación Rápida

* [Volver al Índice General (`docs/README.md`)](../README.md)
* [Consultar el Router de Contexto (`docs/context-index.md`)](../context-index.md)
* [Ver Estado de Vigencia Documental (`docs/ESTADO_DOCUMENTACION.md`)](../ESTADO_DOCUMENTACION.md)
* [Consultar Suite de Skills (`.agents/skills/`)](../../.agents/skills/engineering-loop/SKILL.md)
