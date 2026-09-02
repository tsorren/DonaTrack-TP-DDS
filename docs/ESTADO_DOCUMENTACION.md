# Estado y Vigencia de la Documentación — DonaTrack

> **Panel de Auditoría y Matriz de Vigencia Documental vs. Código Fuente**  
> **Proyecto:** DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones  
> **Equipo:** UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Fecha de Normalización y Sincronización:** 2026-09-02  
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
│ 🟢 Vigentes y 100% Sincronizados     │ 36 documentos    │ 44%          │
│ 🔴 Con Discrepancias Altas / Críticas│ 0 documentos     │ 0%           │
│ 🟡 Con Discrepancias Medias          │ 0 documentos     │ 0%           │
│ 🟢 Con Discrepancias Bajas/Cosméticas│ 0 documentos     │ 0%           │
│ 🔒 Históricos e Inmutables (ADRs/Ent)│ 44 componentes   │ 56%          │
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
│   ├── review/
│   │   └── evaluator.md                  # 🟢 Política Generator/Evaluator, Review Contract, vectores V1–V9
│   └── prompts/                           # Prompts especializados por rol de equipo
│
├── herramientas/                          # 🛠️ Aplicaciones web y utilidades locales
│   ├── documentador/                      # Generador interactivo de minutas y ADRs
│   └── hub/                               # Visor web de documentación y PDFs de entregas
│
├── adr/                                   # 🔒 Registros de Decisión de Arquitectura (Log4brains)
│   ├── README.md                          # 🟢 Fuente canónica de ADR governance (Two-Gate Rule, lifecycle, MADR)
│   ├── DEUDA_TECNICA.md                   # 🟢 Índice de deuda técnica diferida (DTI-01 a DTI-06)
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
| **14** | [AGENTS.md](../AGENTS.md) | Gobernanza | v3.4.0 | Evolucionado a v3.5.0 (Gobernanza de ADRs en proposed, Triggers Mandatorios, Rúbrica de Benchmark y No Bloqueo). | 🟢 Sincronizado |
| **14** | [AGENTS.md](../AGENTS.md) | Gobernanza | v1.0 desactualizado | Evolucionado a v3.4.0 (Gobernanza Calibrada, Reporte Estructurado, Modo Degradado y SonarCloud). | 🟢 Sincronizado |
| **16** | [AGENTS.md](../AGENTS.md) | Gobernanza | `.agents/rules/AGENTS.md` | Oleada 0+1 (2026-09-01): promovido a fuente canónica en raíz del repositorio. Snapshot histórico preservado en `docs/IA/history/AGENTS-v3.5.md`. Links internos actualizados de `../docs/` a `docs/`. | 🟢 Sincronizado |
| **18** | [docs/context-index.md](context-index.md) | Gobernanza / Routing | Inexistente | Oleada 3 (2026-09-01): creado como routing de contexto orientado a tareas para coding agents. Distingue: Service Context (Level 2), Task Context, Temporal Constraints (scoped por servicio con drift signals), Level 3 bajo demanda, y gaps documentales pendientes. | 🟢 Sincronizado |
| **17** | [AGENTS.md](../AGENTS.md) + docs destino | Gobernanza | v3.5.0 (461 líneas) | Oleada 2 (2026-09-01): reducido a 329 líneas. Eliminadas: decoración ASCII, LaTeX, duplicaciones (§9.2b, §9.4). Condensadas: §4.1a, §4.3c, §7.1, §7.3, §11.1, §11.3, §12. Movido: Fitness Checks → `docs/arquitectura/principios-diseno-arquitectura.md §9`; template reporte → `docs/IA/04-checklist-antes-de-pr.md Apéndice`; comandos Docker → `docs/testing/integration-tests.md §5`. Marcadas KEEP_TEMPORARY: §4.1b, §4.2c. | 🟢 Sincronizado |
| **15** | [docs/adr/](adr/) | ADRs | 8 propuestos sin resolver / sin ADRs DTI | Formalizados 28 ADRs propuestos basados en oleadas, deuda técnica y evaluación de Asignable; transicionados los 8 existentes (6 accepted, 1 rejected, 1 superseded); auditados y transicionados primeros ADRs (20260520, 20260521 y 20260616 a superseded). Total: 76 ADRs (42 accepted, 28 proposed, 2 rejected, 4 superseded). | 🟢 Sincronizado |
| **20** | [AGENTS.md](../AGENTS.md) | Gobernanza | v3.5.0 (316 líneas) | Oleada 4 (2026-09-01): §7 reestructurado — Core Workflow universal + niveles QUICK / STANDARD / ARCHITECTURAL, árbol de clasificación, regla anti-downgrade, spec policy, profundidad por nivel (`LIGHTWEIGHT_CLOSING_CHECK` / `REVIEW_REQUIRED` / `ENHANCED_REVIEW_REQUIRED`). §12 actualizado con proporcionalidad. Bump a v4.0.0 (SemVer MAJOR). | 🟢 Sincronizado |
| **21** | [AGENTS.md](../AGENTS.md) + [docs/adr/README.md](adr/README.md) + [docs/adr/DEUDA_TECNICA.md](adr/DEUDA_TECNICA.md) + [docs/IA/04-checklist-antes-de-pr.md](IA/04-checklist-antes-de-pr.md) + [docs/context-index.md](context-index.md) | Gobernanza ADR | v4.0.0 (lista de 9 triggers) | Oleada 5 (2026-09-02): ADR governance refactored — Two-Gate Rule (Gate A: decision novelty + Gate B: architectural significance) reemplaza la lista de 9 triggers mecánicos. Desacoplamiento Task Level ↔ ADR. docs/adr/README.md promovido a fuente canónica de governance. docs/adr/DEUDA_TECNICA.md convertido a índice puro con ejes Decision status / Implementation status separados. Bump AGENTS.md a v5.0.0 (SemVer MAJOR). | 🟢 Sincronizado |
| **22** | [AGENTS.md](../AGENTS.md) + [docs/IA/review/evaluator.md](IA/review/evaluator.md) + [docs/IA/04-checklist-antes-de-pr.md](IA/04-checklist-antes-de-pr.md) + [docs/README.md](README.md) + [docs/IA/README.md](IA/README.md) | Gobernanza Review | v5.0.0 (tool-specific review terms) | Oleada 6 (2026-09-02): política de revisión refactored — separación Generator/Evaluator vendor-neutral. Creado `docs/IA/review/evaluator.md` como fuente canónica: roles, modos de independencia (INDEPENDENT_REVIEW / SELF_REVIEW / LIGHTWEIGHT_CLOSING_CHECK), SOURCE_READ_ONLY + NON_DESTRUCTIVE_VERIFICATION, Review Contract, vectores V1–V9, capability detection, ciclo re-check, responsabilidad humana. Eliminadas referencias tool-specific de AGENTS.md §7.4. Corregida referencia §7.5 → §7.4 en §12. Actualizado checklist y plantilla Reporte Operativo. Bump AGENTS.md a v6.0.0 (SemVer MAJOR). | 🟢 Sincronizado |
| **23** | [AGENTS.md](../AGENTS.md) + [`scripts/agent-check.js`](../scripts/agent-check.js) + [`scripts/tests/run-tests.js`](../scripts/tests/run-tests.js) + [`.github/workflows/agent-governance.yml`](../.github/workflows/agent-governance.yml) | Gobernanza Enforcement | v6.0.0 (sin enforcement mecánico) | Oleada 7A (2026-09-02): primer enforcement mecánico del harness. Creado `scripts/agent-check.js` — 3 checks Wave 7A: AGENTS_CANONICAL, AGENTS_UNEXPECTED, EVALUATOR_EXISTS, EVALUATOR_LINK, STALE_TERMS_CHECK. Arquitectura: check logic pura + aggregator + renderer + CLI, sin dependencias npm. Creado `scripts/tests/run-tests.js` con 17 casos de prueba (fixtures temporales in-memory). Creado workflow CI `agent-governance.yml` — trigger en todo PR y push a main/ENTREGA_*. Bloquea merge si FAIL > 0. STALE_TERMS_EXCLUSIONS: `docs/ESTADO_DOCUMENTACION.md` excluido como log de auditoría. Bump AGENTS.md a v6.1.0 (SemVer MINOR: nueva directiva de enforcement). | 🟢 Sincronizado |
| **24** | [`scripts/agent-check.js`](../scripts/agent-check.js) + [`scripts/tests/run-tests.js`](../scripts/tests/run-tests.js) + [docs/IA/review/evaluator.md](IA/review/evaluator.md) | Referential Integrity | Wave 7A (17 tests, 5 checks) | Oleada 7B (2026-09-02): enforcement de integridad referencial. Extendido `agent-check.js` con 3 checks nuevos: INTERNAL_LINKS (88 links verificados en 8 docs canónicos del harness), CONTEXT_INDEX_REFERENCES (10 code-span paths en context-index), DEUDA_TECNICA_INTEGRITY (6 DTIs: IDs únicos, ADR links, Decision status enum, mismatch WARN, impl-status WARN). 8 helpers puros exportados. Tests extendidos a 58 casos (11 grupos). REFACTOR_CANDIDATE: agent-check.js alcanzó 641 líneas (umbral ~500-600). Hallazgo: 2 links rotos reales en evaluator.md (`../auditoria/` y `../adr/` resolvían a `docs/IA/auditoria/` y `docs/IA/adr/` inexistentes) → corregidos a `../../auditoria/` y `../../adr/`. AGENTS.md sin bump (sin cambio de policy visible). | 🟢 Sincronizado |
| **25** | [`scripts/agent-check/`](../scripts/agent-check/) + [`scripts/tests/run-tests.js`](../scripts/tests/run-tests.js) | ADR / Module / Drift | Wave 7B (58 tests, 8 checks) | Oleada 7C (2026-09-02): refactor modular + 3 checks nuevos. Refactor: monolito 641 líneas → 17 módulos (847 líneas totales): config.js, lib/{findings,paths,markdown,adr-parser,pom}.js, checks/{agents,evaluator,stale-terms,links,context-index,deuda-tecnica,adr,modules,temporal-drift}.js, index.js; agent-check.js queda en 14 líneas. Checks nuevos: ADR_STATUS_VALID (76 ADRs × enum check + superseded refs), MODULE_ROUTING_COMPLETENESS (5 módulos Maven × 5 servicios en context-index, integration-tests excluido), TEMPORAL_DRIFT (drift signals A+B por servicio, siempre WARN). Tests: 81/81 PASS (23 nuevos). Repo real: PASS: 11 / WARN: 0 / FAIL: 0 / 826ms. Sin drift JPA detectado (Fase 1 activa). AGENTS.md sin bump. | 🟢 Sincronizado |
| **26** | [`common-lib/AGENTS.md`](../common-lib/AGENTS.md) + [`AGENTS.md`](../AGENTS.md) + [`scripts/agent-check/config.js`](../scripts/agent-check/config.js) + [`scripts/agent-check/checks/agents.js`](../scripts/agent-check/checks/agents.js) + [`scripts/tests/run-tests.js`](../scripts/tests/run-tests.js) | Gobernanza Nested AGENTS | Wave 7C (81 tests, 9 checks) | Oleada 8 (2026-09-02): primer nested AGENTS con ROI positivo. Creado `common-lib/AGENTS.md` (~37 líneas): Shared Kernel membership rules, exclusiones explícitas por categoría, contratos protegidos (AggregateRoot, CrudRepository, ErrorCatalog, ErrorResponse, DonaTrackException, X-Trace-Id) y reglas de validación proporcionales (QUICK / módulo / reactor). Root AGENTS.md §4.2 simplificado a pointer; bump a v6.2.0 (SemVer MINOR: nueva directiva de nested AGENTS). `config.js`: AGENTS_ALLOWLIST extendido con `'common-lib/AGENTS.md'` (explícito, sin wildcards). Check nuevo `AGENTS_ALLOWLISTED_MISSING`: FAIL si un nested autorizado está ausente. `AGENTS_UNEXPECTED` message actualizado. Tests: 86/86 PASS (5 nuevos: 1.5-1.8, 3.9; tests 1.1 y 4.1 actualizados). Repo real: PASS: 12 / WARN: 0 / FAIL: 0. integration-tests/AGENTS.md: MAYBE_LATER (context-index ya cubre; reconsiderar si se observa error real). DO_NOT_CREATE para servicios, docs/, scripts/. | 🟢 Sincronizado |

---

## 4. Estado de Formalización de ADRs y Decisiones de Arquitectura

* **Formalización de ADRs de Deuda Técnica y Oleadas de Refactor:** Completada exitosamente al 100%. Se formalizaron 28 nuevos ADRs en estado propuesto (`Status: proposed`) en formato Log4brains / MADR (incluyendo DTI-01 a DTI-06, evaluación de `Asignable` vs `entidadBeneficiariaId`, arquitectura transversal de persistencia, MinIO S3, Transactional Outbox, Crypto-Shredding, Testcontainers, coordinadores distribuidos ShedLock, e invariantes de dominio de las Oleadas de Refactor), se transicionaron los 8 ADRs pendientes en `docs/adr/`, y se ajustaron los primeros ADRs históricos hacia `superseded`, alcanzando un total consolidado de **76 ADRs** plenamente vigentes y clasificados.

