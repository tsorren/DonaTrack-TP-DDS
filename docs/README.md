# Índice General de Documentación — DonaTrack

> **Portal de Documentación y Arquitectura del Sistema**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Última actualización: **2026-09-01**

---

## 🗺️ Mapa de Navegación de docs/

El portal de documentación está organizado de forma modular según su área de competencia:

```text
docs/
├── ESTADO_DOCUMENTACION.md                # Panel central de auditoría y vigencia técnica
├── README.md                              # Este índice de navegación
│
├── auditoria/                             # Sistema de aseguramiento y verificación adversarial
│   └── plan-revisor-critico.md            # Plan de auditoría, checklists y matriz de evaluación
│
├── arquitectura/                          # Especificaciones de dominio, DDD y Shared Kernel
│   ├── principios-diseno-arquitectura.md  # Fundamentación teórica, 8 atributos, SOLID, GRASP, GoF, DDD
│   ├── shared-kernel.md                   # CrudRepository, eventos de dominio y OpenAPI
│   ├── logging-trazabilidad.md            # Observabilidad distribuida, MDC y traceId
│   ├── analisis-arquitectonico.md         # Diagnóstico estructural del monorepo
│   ├── aggregates-donaciones.md           # Aggregates DDD, 7 estados de DI y Propuesta
│   ├── aggregates-incentivos.md           # Aggregates de gamificación, Insignia y Rankings
│   ├── aggregates-logistica.md            # Aggregates de transporte, rutas y camiones
│   ├── aggregates-notificaciones.md       # Réplica ligera y contratos REST sincrónicos
│   └── diseno/                            # Bitácoras de refactor por oleadas y diagramas PUML
│       ├── donaciones/
│       ├── incentivos/
│       ├── logistica/
│       └── notificaciones/
│
├── testing/                               # Pruebas automatizadas y contratos
│   ├── integration-tests.md               # Arquitectura de tests E2E y clientes tipados
│   └── postman/                           # 12 colecciones y flujos E2E distribuidos
│
├── cicd/                                  # Automatización, CI/CD y políticas de PR
│   ├── DonaTrack-CICD.md                  # Documentación de los 7 workflows de GitHub Actions
│   ├── assignment_reminders_plan.md       # Sistema de asignación dinámica de reviews
│   └── cascading_flow_plan.md             # Flujo de Stacked PRs en cascada
│
├── IA/                                    # Lineamientos de ingeniería con Inteligencia Artificial
│   ├── README.md                          # Mapa de prompts y normas de uso
│   ├── 01-principios-de-uso.md … 05
│   ├── 06-contexto-base-donatrack.md      # Snippet de contexto para asistentes de IA
│   ├── 07-errores-frecuentes-sonarcloud-ia.md # Prevención y checklist pre-flight SonarCloud
│   └── prompts/                           # Prompts especializados por rol de equipo
│
├── herramientas/                          # Aplicaciones web y utilidades locales
│   ├── documentador/                      # Generador interactivo de minutas y ADRs
│   └── hub/                               # Visor web de documentación y PDFs de entregas
│
├── adr/                                   # 🔒 Registros de Decisión de Arquitectura (Log4brains)
│   ├── DEUDA_TECNICA.md                   # Registro de deuda técnica (DTI-01 a DTI-06) con ADRs enlazados
│   └── donaciones, incentivos, etc.       # 76 ADRs (42 aceptados, 28 propuestos, 2 rechazados, 4 superados)
│
└── entregas/                              # 🔒 Enunciados oficiales y diagramas entregados
    ├── 1/ … 4/                            # PDFs de requerimientos de cátedra
    └── interfaz/                          # Bocetos Figma y mapa de navegación
```

---

## 📌 Enlaces Rápidos

* 🔍 **Diagnóstico de Vigencia y Auditoría:** [ESTADO_DOCUMENTACION.md](ESTADO_DOCUMENTACION.md)
* 🛡️ **Plan de Auditoría y Marco Revisor Crítico:** [auditoria/plan-revisor-critico.md](auditoria/plan-revisor-critico.md)
* 📐 **Principios de Diseño y Arquitectura (Documento Maestro):** [arquitectura/principios-diseno-arquitectura.md](arquitectura/principios-diseno-arquitectura.md)
* 🏗️ **Dominio y Persistencia Compartida:** [arquitectura/shared-kernel.md](arquitectura/shared-kernel.md)
* 📊 **Trazabilidad y Formato de Logs:** [arquitectura/logging-trazabilidad.md](arquitectura/logging-trazabilidad.md)
* 🧪 **Guía de Pruebas de Integración:** [testing/integration-tests.md](testing/integration-tests.md)
* 🚀 **Pipeline de CI/CD y Workflows:** [cicd/DonaTrack-CICD.md](cicd/DonaTrack-CICD.md)
* 🤖 **Protocolo y Prompts de IA:** [IA/README.md](IA/README.md)
* 🛡️ **Pre-Flight SonarCloud para IA:** [IA/07-errores-frecuentes-sonarcloud-ia.md](IA/07-errores-frecuentes-sonarcloud-ia.md)
* 📝 **Registro de Deuda Técnica:** [adr/DEUDA_TECNICA.md](adr/DEUDA_TECNICA.md)

