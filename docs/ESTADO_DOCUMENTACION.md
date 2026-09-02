# Estado y Vigencia de la Documentación — DonaTrack

> **Panel de Auditoría y Matriz de Vigencia Documental vs. Código Fuente**  
> **Proyecto:** DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones  
> **Equipo:** UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Fecha de Normalización y Sincronización:** 2026-09-01  
> **Propósito:** Diagnóstico y estado de sincronización de toda la documentación del repositorio contrastada contra la implementación real en Java 21, Spring Boot 3, RabbitMQ y n8n tras la formalización de ADRs de oleadas de refactor y auditoría de deuda técnica.

---

## 1. Resumen Ejecutivo y Estadísticas de Salud Documental

Tras la formalización de 28 nuevos ADRs propuestos basados en las oleadas de refactor, la evaluación de la interfaz Asignable y la resolución de estados de los ADRs existentes:

```text
┌────────────────────────────────────────────────────────────────────────┐
│             MÉTRICAS DE SALUD DOCUMENTAL (POST-AUDITORÍA CANÓNICA)     │
├──────────────────────────────────────┬──────────────────┬──────────────┤
│ Categoría                            │ Cantidad         │ Porcentaje   │
├──────────────────────────────────────┼──────────────────┼──────────────┤
│ 🟢 Vigentes y 100% Sincronizados     │ 62 documentos    │ 44%          │
│ 🔴 Con Discrepancias Altas / Críticas│ 0 documentos     │ 0%           │
│ 🟡 Con Discrepancias Medias          │ 0 documentos     │ 0%           │
│ 🟢 Con Discrepancias Bajas/Cosméticas│ 0 documentos     │ 0%           │
│ 🔒 Históricos y Registros ADR        │ 79 componentes   │ 56%          │
└──────────────────────────────────────┴──────────────────┴──────────────┘
```

---

## 2. Mapa Estructural Canónico de docs/

```text
docs/
├── ESTADO_DOCUMENTACION.md                # 🟢 Panel central de auditoría y vigencia técnica
├── README.md                              # 🟢 Índice general de navegación y enlaces rápidos
│
├── auditoria/                             # 🟢 Sistema de aseguramiento y verificación adversarial
│   └── plan-revisor-critico.md            # Plan de auditoría, checklists por etapa y matriz de evaluación
│
├── arquitectura/                          # 🟢 Especificaciones de dominio, DDD y Shared Kernel
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
├── testing/                               # 🟢 Pruebas automatizadas y contratos
│   ├── integration-tests.md               # Arquitectura de tests E2E y clientes tipados
│   └── postman/                           # 12 colecciones y flujos E2E distribuidos
│
├── cicd/                                  # 🟢 Automatización, CI/CD y políticas de PR
│   ├── DonaTrack-CICD.md                  # Documentación de los 7 workflows de GitHub Actions
│   ├── assignment_reminders_plan.md       # Sistema de asignación dinámica de reviews
│   └── cascading_flow_plan.md             # Flujo de Stacked PRs en cascada
│
├── IA/                                    # 🟢 Lineamientos de ingeniería con Inteligencia Artificial
│   ├── README.md                          # Mapa de prompts y normas de uso
│   ├── 01-principios-de-uso.md … 05
│   ├── 06-contexto-base-donatrack.md      # Snippet de contexto con stack técnico completo
│   ├── 07-errores-frecuentes-sonarcloud-ia.md # 🟢 Prevención y checklist pre-flight SonarCloud
│   └── prompts/                           # Prompts especializados por rol de equipo
│
├── herramientas/                          # 🛠️ Aplicaciones web y utilidades locales
│   ├── documentador/                      # Generador interactivo de minutas y ADRs
│   └── hub/                               # Visor web de documentación y PDFs de entregas
│
├── adr/                                   # 🔒 Registros de Decisión de Arquitectura (Log4brains)
│   ├── DEUDA_TECNICA.md                   # 🟢 Registro de deuda técnica (DTI-01 a DTI-06) con ADRs enlazados
│   └── donaciones, incentivos, etc.       # 76 ADRs (42 aceptados, 28 propuestos, 2 rechazados, 4 superados)
│
└── entregas/                              # 🔒 Enunciados oficiales y diagramas entregados
    ├── 1/ … 4/                            # PDFs de requerimientos de cátedra
    └── interfaz/                          # Bocetos Figma y mapa de navegación
```

---

## 3. Matriz de Resoluciones y Documentos Incorporados

| # | Documento | Área | Estado Inicial | Resolución / Incorporación Canónica | Estado Actual |
|:---:|---|---|:---:|---|:---:|
| **1** | [docs/auditoria/plan-revisor-critico.md](auditoria/plan-revisor-critico.md) | Auditoría | Inexistente | Creado marco metodológico completo, matrices 1-5, rúbricas, checklists por etapa y plantillas. | 🟢 Sincronizado |
| **2** | [docs/arquitectura/principios-diseno-arquitectura.md](arquitectura/principios-diseno-arquitectura.md) | Arquitectura | Inexistente | Creado documento maestro con fundamentación teórica, los 8 atributos de calidad, SOLID, GRASP, GoF y DDD. | 🟢 Sincronizado |
| **3** | [docs/README.md](README.md) | Raíz docs/ | Desactualizado | Actualizado con enlaces y estructura modular de auditoria/ y nuevos documentos canónicos. | 🟢 Sincronizado |
| **4** | [docs/arquitectura/shared-kernel.md](arquitectura/shared-kernel.md) | Shared Kernel | Sincronizado | Verificado: CrudRepository<T extends AggregateRoot>, eventos de dominio y OpenAPI. | 🟢 Sincronizado |
| **5** | [docs/arquitectura/logging-trazabilidad.md](arquitectura/logging-trazabilidad.md) | Observabilidad | Sincronizado | Verificado: MDC, TraceResponseHeaderFilter e interceptor OpenFeign en los 4 microservicios. | 🟢 Sincronizado |
| **6** | [docs/arquitectura/aggregates-donaciones.md](arquitectura/aggregates-donaciones.md) | Donaciones | Sincronizado | Verificado: 7 estados del State Pattern y Aggregate Root Propuesta. | 🟢 Sincronizado |
| **7** | [docs/arquitectura/aggregates-notificaciones.md](arquitectura/aggregates-notificaciones.md) | Notificaciones | Sincronizado | Verificado: Transporte HTTP REST vía OpenFeign y réplica ligera de Persona. | 🟢 Sincronizado |
| **8** | [docs/arquitectura/aggregates-incentivos.md](arquitectura/aggregates-incentivos.md) | Incentivos | Sincronizado | Verificado: Misiones con Template Method, InsigniaRepository y rankings. | 🟢 Sincronizado |
| **9** | [docs/arquitectura/aggregates-logistica.md](arquitectura/aggregates-logistica.md) | Logística | Sincronizado | Verificado: Máquina de estados de Entrega, Camion, Ruta y eventos AMQP RabbitMQ. | 🟢 Sincronizado |
| **10** | [docs/cicd/DonaTrack-CICD.md](cicd/DonaTrack-CICD.md) | CI/CD | Sincronizado | Verificado: 7 workflows de GitHub Actions y enlaces relativos válidos. | 🟢 Sincronizado |
| **11** | [docs/IA/06-contexto-base-donatrack.md](IA/06-contexto-base-donatrack.md) | IA | Sincronizado | Verificado: Snippet de contexto con stack técnico y restricciones de persistencia. | 🟢 Sincronizado |
| **12** | [docs/adr/DEUDA_TECNICA.md](adr/DEUDA_TECNICA.md) | ADR | Sincronizado | Verificado: Catálogo de deudas técnicas DTI-01 a DTI-06 con ADRs individuales enlazados. | 🟢 Sincronizado |
| **13** | [docs/IA/07-errores-frecuentes-sonarcloud-ia.md](IA/07-errores-frecuentes-sonarcloud-ia.md) | IA / Calidad | Inexistente | Guía viva de prevención de errores frecuentes de SonarCloud y checklist pre-flight para agentes. | 🟢 Sincronizado |
| **14** | [.agents/rules/AGENTS.md](../.agents/rules/AGENTS.md) | Gobernanza | v1.0 desactualizado | Evolucionado a v3.4.0 (Gobernanza Calibrada, Reporte Estructurado, Modo Degradado y SonarCloud). | 🟢 Sincronizado |
| **15** | [docs/adr/](adr/) | ADRs | 8 propuestos sin resolver / sin ADRs DTI | Formalizados 28 ADRs propuestos basados en oleadas, deuda técnica y evaluación de Asignable; transicionados los 8 existentes (6 accepted, 1 rejected, 1 superseded); auditados y transicionados primeros ADRs (20260520, 20260521 y 20260616 a superseded). Total: 76 ADRs (42 accepted, 28 proposed, 2 rejected, 4 superseded). | 🟢 Sincronizado |
| **16** | [docs/adr/notificaciones-service/20260902-adapters-de-notificacion-y-observabilidad.md](adr/notificaciones-service/20260902-adapters-de-notificacion-y-observabilidad.md) | ADR / Notificaciones | Inexistente | Formalizados adapters simulados con CriterioFalloSimulado (Strategy), NotificacionAuditListener y PersonaYaAnonimizadaException. Total ADRs: 77 (43 accepted, 28 proposed, 2 rejected, 4 superseded). | 🟢 Sincronizado |
| **17** | [docs/adr/notificaciones-service/20260902-evento-donacion-vencida.md](adr/notificaciones-service/20260902-evento-donacion-vencida.md) | ADR / Notificaciones | Inexistente | Formalizado evento DonacionVencida y alerta al administrador vía API REST. Total ADRs: 78 (44 accepted, 28 proposed, 2 rejected, 4 superseded). | 🟢 Sincronizado |

---

## 4. Estado de Formalización de ADRs y Decisiones de Arquitectura

* **Formalización de ADRs de Deuda Técnica y Oleadas de Refactor:** Completada exitosamente al 100%. Se formalizaron 28 nuevos ADRs en estado propuesto (`Status: proposed`) en formato Log4brains / MADR (incluyendo DTI-01 a DTI-06, evaluación de `Asignable` vs `entidadBeneficiariaId`, arquitectura transversal de persistencia, MinIO S3, Transactional Outbox, Crypto-Shredding, Testcontainers, coordinadores distribuidos ShedLock, e invariantes de dominio de las Oleadas de Refactor), se transicionaron los 8 ADRs pendientes en `docs/adr/`, y se ajustaron los primeros ADRs históricos hacia `superseded`. Además, se incorporaron los ADRs `20260902-adapters-de-notificacion-y-observabilidad.md` y `20260902-evento-donacion-vencida.md` en `notificaciones-service`, alcanzando un total consolidado de **78 ADRs** plenamente vigentes y clasificados.

