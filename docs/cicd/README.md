# Integración y Despliegue Continuo (CI/CD) — DonaTrack

> **Portal de Automatización, Workflows de GitHub Actions y Políticas de PR**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Alineación con [`AGENTS.md §4.3, §11`](../../AGENTS.md)

---

## 1. Propósito

Este directorio documenta la infraestructura de automatización continua del repositorio DonaTrack. Los flujos de CI/CD garantizan que cada Pull Request y commit en ramas principales supere de forma determinista los controles de formato, compilación, pruebas unitarias, análisis estático (SonarCloud), contratos de interfaz y gobernanza del arnés de agentes.

```text
docs/cicd/
├── README.md                      # Este índice de navegación
├── DonaTrack-CICD.md              # 🚀 Documentación integral de pipelines y workflows
└── assignment_reminders_plan.md   # 📢 Sistema de alertas de inactividad de PRs en Discord
```

---

## 2. Documentos del Módulo

* [**Documentación de Pipelines de CI/CD (`DonaTrack-CICD.md`)**](DonaTrack-CICD.md):  
  Detalle técnico de los flujos automatizados en `.github/workflows/`:
  - `main.yml`: Compilación modular, Spotless, tests unitarios, SonarCloud y empaquetado de artefactos.
  - `agent-governance.yml`: Ejecución obligatoria de `agent-check.js`, `run-tests.js` y `validate-contracts.js`.
  - `merge.yml`: Flujo de validación post-merge y despliegue a ramas de entrega.
  - `deploy-pages.yml`: Publicación automatizada del Hub de documentación y Log4brains en GitHub Pages.
  - `pr-reminders.yml`: Monitor de inactividad de revisiones pendientes.
* [**Plan de Recordatorios de Asignación (`assignment_reminders_plan.md`)**](assignment_reminders_plan.md):  
  Especificación técnica del bot de Discord para notificar asignaciones demoradas y evitar estancamiento en el ciclo de revisión de PRs.

---

## 3. Navegación Rápida

* [Volver al Índice General (`docs/README.md`)](../README.md)
* [Consultar el Router de Contexto (`docs/context-index.md`)](../context-index.md)
* [Ver Estado de Vigencia Documental (`docs/ESTADO_DOCUMENTACION.md`)](../ESTADO_DOCUMENTACION.md)
