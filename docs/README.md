# Índice General de Documentación — DonaTrack

> **Portal de Documentación y Arquitectura del Sistema**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> Última actualización: **2026-09-05**

---

## 🗺️ Mapa de Navegación de docs/

El portal de documentación está organizado de forma modular según su área de competencia:

```text
docs/
├── ESTADO_DOCUMENTACION.md                # Panel central de auditoría y vigencia técnica
├── README.md                              # Este índice de navegación
├── context-index.md                       # 🤖 Routing de contexto para coding agents (qué leer por tarea)
│
├── auditoria/                             # Sistema de aseguramiento y verificación adversarial
│   ├── plan-revisor-critico.md            # Plan de auditoría, checklists y matriz de evaluación
│   └── revision-critica-devops-ci.md      # 🟢 Revisión crítica y auditoría de CI/CD, Dockerfiles y scripts
│
├── arquitectura/                          # Especificaciones de dominio, DDD y Shared Kernel
│   ├── principios-diseno-arquitectura.md  # Fundamentación teórica, 8 atributos, SOLID, GRASP, GoF, DDD
│   ├── shared-kernel.md                   # CrudRepository, eventos de dominio y OpenAPI
│   ├── logging-trazabilidad.md            # Observabilidad distribuida, MDC y traceId
│   ├── analisis-arquitectonico.md         # Diagnóstico estructural del monorepo
│   ├── guia-patrones-diseno.md            # Catálogo de patrones de diseño aplicados
│   ├── catalogo-errores.md                # ⚠️ Catálogo unificado de códigos de error (ERR-INF, ERR-VAL, etc.)
│   ├── aggregates-donaciones.md           # Aggregates DDD, 7 estados de DI y Propuesta
│   ├── aggregates-incentivos.md           # Aggregates de gamificación, Insignia y Rankings
│   ├── aggregates-logistica.md            # Aggregates de transporte, rutas y camiones
│   ├── aggregates-notificaciones.md       # Réplica ligera y contratos REST sincrónicos
│   ├── contratos-rest.md                  # Contratos REST consolidados, OpenAPI 3.0 y Swagger UI
│   ├── eventos-amqp.md                    # Topología RabbitMQ y contratos de eventos asíncronos
│   ├── contratos/                         # Especificaciones OpenAPI 3.0 (YAML) y Schemas JSON
│   └── diseno/                            # Bitácoras de refactor por oleadas, diagramas PUML y anexos
│       ├── donaciones/
│       ├── incentivos/
│       ├── logistica/
│       ├── notificaciones/
│       ├── common/                        # Estilos compartidos donatrack-style.puml
│       └── anexos-tecnicos/               # Modelos técnicos de build autogenerados por Maven
│
├── testing/                               # Pruebas automatizadas y contratos
│   ├── integration-tests.md               # Arquitectura de tests E2E y clientes tipados
│   ├── plan-auditoria-y-blueprint-qa.md   # Plan de auditoría y arquitectura target de testing y QA
│   ├── decisiones-diseno-auditoria-qa.md  # Decisiones de diseño y justificación de alternativas descartadas
│   └── postman/                           # 12 colecciones y flujos E2E distribuidos
│
├── cicd/                                  # Automatización, CI/CD y políticas de PR
│   ├── DonaTrack-CICD.md                  # Documentación de pipelines y workflows de GitHub Actions
│   └── assignment_reminders_plan.md       # Recordatorios de inactividad de PRs en Discord
│
├── IA/                                    # Lineamientos de ingeniería con Inteligencia Artificial
│   ├── README.md                          # Mapa de prompts y normas de uso
│   ├── 01-principios-de-uso.md … 05
│   ├── 06-contexto-base-donatrack.md      # Snippet de contexto para asistentes de IA
│   ├── 07-errores-frecuentes-sonarcloud-ia.md # Prevención y checklist pre-flight SonarCloud
│   ├── review/
│   │   └── evaluator.md                  # 🤖 Política Generator/Evaluator, Review Contract, vectores V1–V9
│   ├── evals/                             # 🧪 Suite de evaluación del harness (Wave 9)
│   │   ├── README.md                      # Output Contract v1, Critical Failures, A/B methodology, execution policy
│   │   ├── scenarios/                     # 9 scenarios E01–E09 (golden + adversariales integrados)
│   │   ├── scorecards/                    # Plantilla de scorecard por run
│   │   └── results/                       # Resultados de runs (no versionados individualmente)
│   └── prompts/                           # Prompts especializados por rol de equipo
│
├── herramientas/                          # Aplicaciones web y utilidades locales
│   ├── documentador/                      # Generador interactivo de minutas y ADRs
│   └── hub/                               # Visor web de documentación y PDFs de entregas
│
├── adr/                                   # 🔒 Registros de Decisión de Arquitectura (Log4brains)
│   ├── README.md                          # ⚖️ Fuente canónica de ADR governance (Two-Gate Rule, lifecycle, MADR)
│   ├── DEUDA_TECNICA.md                   # Registro de deuda técnica (DTI-01 a DTI-08) con ADRs enlazados
│   └── donaciones, notificaciones, etc.   # Decisiones de arquitectura por microservicio (Log4brains)
│
└── entregas/                              # 🔒 Enunciados oficiales y diagramas entregados
    ├── README.md                          # 🎓 Matriz curricular e índice de entregas 1 a 4
    ├── 1/ … 4/                            # PDFs de requerimientos de cátedra
    └── interfaz/                          # Bocetos Figma y mapa de navegación
```

---

## 🛠️ Comandos de Compilación del Monorepo (Maven)

* **Build completo del reactor:** `mvn clean test`
* **Compilación de un módulo específico:** Utilizar siempre el flag `-am` (`--also-make`) para resolver dependencias internas del reactor (`common-lib`):
  ```bash
  mvn test -pl notificaciones-service -am
  ```
  *(Nota: ejecutar `mvn test -pl notificaciones-service` sin `-am` requerirá que `common-lib` haya sido previamente instalada en el repositorio local vía `mvn install -DskipTests -pl common-lib`).*

---

## 📌 Enlaces Rápidos

* 🤖 **Context Router para Agentes** *(qué leer por tipo de tarea)*: [context-index.md](context-index.md)
* 🔍 **Diagnóstico de Vigencia y Auditoría:** [ESTADO_DOCUMENTACION.md](ESTADO_DOCUMENTACION.md)
* 🛡️ **Plan de Auditoría y Marco Revisor Crítico:** [auditoria/plan-revisor-critico.md](auditoria/plan-revisor-critico.md)
* 📐 **Principios de Diseño y Arquitectura (Documento Maestro):** [arquitectura/principios-diseno-arquitectura.md](arquitectura/principios-diseno-arquitectura.md)
* 🧩 **Guía de Patrones de Diseño Aplicados:** [arquitectura/guia-patrones-diseno.md](arquitectura/guia-patrones-diseno.md)
* ⚠️ **Catálogo Unificado de Errores:** [arquitectura/catalogo-errores.md](arquitectura/catalogo-errores.md)
* 🏗️ **Dominio y Persistencia Compartida:** [arquitectura/shared-kernel.md](arquitectura/shared-kernel.md)
* 📊 **Trazabilidad y Formato de Logs:** [arquitectura/logging-trazabilidad.md](arquitectura/logging-trazabilidad.md)
* 🎓 **Matriz Curricular de Entregas:** [entregas/README.md](entregas/README.md)
* 🧪 **Guía de Pruebas de Integración:** [testing/integration-tests.md](testing/integration-tests.md)
* 🚀 **Pipeline de CI/CD y Workflows:** [cicd/DonaTrack-CICD.md](cicd/DonaTrack-CICD.md)
* 🤖 **Protocolo y Prompts de IA:** [IA/README.md](IA/README.md)
* 🛡️ **Pre-Flight SonarCloud para IA:** [IA/07-errores-frecuentes-sonarcloud-ia.md](IA/07-errores-frecuentes-sonarcloud-ia.md)
* 🔍 **Política Generator/Evaluator — Review Contract y vectores:** [IA/review/evaluator.md](IA/review/evaluator.md)
* 🧪 **Harness Evals — Output Contract, Critical Failures, A/B:** [IA/evals/README.md](IA/evals/README.md)
* ⚖️ **ADR Governance — Two-Gate Rule, lifecycle, MADR:** [adr/README.md](adr/README.md)
* 📝 **Índice de Deuda Técnica:** [adr/DEUDA_TECNICA.md](adr/DEUDA_TECNICA.md)
* 🗄️ **Arquitectura de Persistencia PostgreSQL:** [../persistencia/README.md](../persistencia/README.md)
* ⚙️ **Workflows y Webhooks de n8n:** [../n8n/README.md](../n8n/README.md)
* 📮 **Colecciones Postman y Newman CLI:** [../postman/README.md](../postman/README.md)
* 🔒 **Auth Service (Bounded Context Reservado):** [../auth-service/README.md](../auth-service/README.md)
* 💻 **Cliente Liviano (Bounded Context Reservado):** [../cliente-liviano/README.md](../cliente-liviano/README.md)
* 🗄️ **Scripts de Inicialización de Base de Datos (PostgreSQL):** [../persistencia/init-db/01-init-schemas-roles.sql](../persistencia/init-db/01-init-schemas-roles.sql)

