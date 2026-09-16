# Auditoría y Aseguramiento Adversarial — DonaTrack

> **Portal de Auditorías Críticas, Madurez de Repositorio y DevOps**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Alineación con [`AGENTS.md §3, §7.4`](../../AGENTS.md) y [`docs/IA/review/evaluator.md`](../IA/review/evaluator.md)

---

## 1. Propósito

Este directorio agrupa los marcos metodológicos, reportes de auditoría adversarial y análisis de madurez del repositorio DonaTrack. La disciplina de revisión crítica garantiza que los artefactos de diseño, configuración de infraestructura y comportamiento de los agentes cumplan con los más altos estándares de calidad antes de su integración.

```text
docs/auditoria/
├── README.md                          # Este índice de navegación
├── plan-revisor-critico.md            # 🛡️ Marco metodológico y rúbricas del Revisor Crítico
├── revision-critica-devops-ci.md      # ⚙️ Auditoría técnica de DevOps, CI/CD, Docker y scripts
└── auditoria-directivas-agentes.md    # 🤖 Diagnóstico de madurez Agent-Friendly (Nivel 4 Pleno)
```

---

## 2. Documentos del Portal

* [**Plan y Rúbricas del Revisor Crítico (`plan-revisor-critico.md`)**](plan-revisor-critico.md):  
  Marco metodológico para la evaluación adversarial independiente. Define las matrices de control por etapa (diseño, código, testing), posturas de desafío y criterios de detención `BLOCKING`.
* [**Revisión Crítica de DevOps y CI/CD (`revision-critica-devops-ci.md`)**](revision-critica-devops-ci.md):  
  Auditoría técnica exhaustiva de los pipelines de GitHub Actions, Dockerfiles multi-stage, usuarios non-root, recolección de logs sin volúmenes de host y scripts auxiliares.
* [**Auditoría de Directivas y Madurez de Agentes (`auditoria-directivas-agentes.md`)**](auditoria-directivas-agentes.md):  
  Diagnóstico formal de madurez del repositorio bajo la taxonomía de repositorios amigables para agentes (*Agent-Friendly Repositories*). Acredita un puntaje de madurez 5.0/5.0 (**Nivel 4: Agent-First Pleno**), con arnés mecánico de gobernanza y protección anti-sesgo.

---

## 3. Navegación Rápida

* [Volver al Índice General (`docs/README.md`)](../README.md)
* [Consultar el Router de Contexto (`docs/context-index.md`)](../context-index.md)
* [Ver Estado de Vigencia Documental (`docs/ESTADO_DOCUMENTACION.md`)](../ESTADO_DOCUMENTACION.md)
