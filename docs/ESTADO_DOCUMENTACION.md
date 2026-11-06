# Estado y Vigencia de la Documentación — DonaTrack

> **Panel de Auditoría y Matriz de Vigencia Documental vs. Código Fuente**  
> **Proyecto:** DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones  
> **Equipo:** UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Fecha de Normalización y Sincronización:** 2026-10-14 (auditoría documental completa contra el código de ENTREGA_4)  
> **Propósito:** Diagnóstico y estado de sincronización de toda la documentación del repositorio contrastada contra la implementación real en Java 21, Spring Boot 4.0.7, RabbitMQ y n8n tras la formalización de ADRs de oleadas de refactor, testing architecture blueprint, arnés de gobernanza y auditoría de deuda técnica.

---

## 1. Resumen Ejecutivo de Salud Documental

El repositorio DonaTrack mantiene su grafo documental estrictamente alineado con el código fuente ejecutable:

* **Sincronización:** 🟢 Plena. Todos los agregados de dominio, adaptadores, contratos REST, eventos AMQP y configuraciones de persistencia cuentan con respaldo documental canónico en `docs/`.
* **Discrepancias Críticas:** la auditoría del 2026-10-14 detectó divergencias entre documentación y código; las de dato factual se corrigieron en esta ronda y las de decisión quedan registradas como deuda técnica o pendientes del equipo (estado del ADR de comunicación asimétrica frente a SPEC-03, cierre de SPEC-04, avance parcial de SPEC-03 Etapa 2).
* **Verificación Mecánica:** La vigencia de enlaces internos, canonicidad de reglas de agentes, detección de términos obsoletos y validez de estados de ADRs se auditan automáticamente en CI/CD mediante el arnés de gobernanza (`scripts/agent-check.js`).

---

## 2. Mapa Estructural Canónico de docs/

```text
docs/
├── ESTADO_DOCUMENTACION.md                # 🟢 Panel central de auditoría y vigencia técnica
├── README.md                              # 🟢 Índice general de navegación y enlaces rápidos
├── context-index.md                       # 🟢 Context Router para coding agents
│
├── auditoria/                             # 🟢 Sistema de aseguramiento y verificación adversarial
│   ├── README.md                          # 🟢 Portal de auditorías críticas y DevOps
│   ├── plan-revisor-critico.md            # Plan de auditoría, checklists por etapa y matriz de evaluación
│   ├── revision-critica-devops-ci.md      # 🟢 Revisión crítica y auditoría de CI/CD, Dockerfiles y scripts
│   └── auditoria-directivas-agentes.md    # 🟢 Auditoría de repositorio bajo directivas de agentes IA (Maturity Scorecard)
│
├── arquitectura/                          # 🟢 Especificaciones de dominio, DDD, patrones y Shared Kernel
│   ├── README.md                          # 🟢 Portal de arquitectura y especificaciones DDD
│   ├── principios-diseno-arquitectura.md  # Fundamentación teórica, 8 atributos, SOLID, GRASP, GoF, DDD
│   ├── shared-kernel.md                   # CrudRepository, eventos de dominio y OpenAPI
│   ├── logging-trazabilidad.md            # Observabilidad distribuida, MDC y traceId
│   ├── catalogo-errores.md                # 🟢 Catálogo exhaustivo de 112 códigos de error unificados
│   ├── analisis-arquitectonico.md         # Diagnóstico estructural del monorepo
│   ├── guia-patrones-diseno.md            # Catálogo de patrones de diseño aplicados
│   ├── aggregates-donaciones.md           # Aggregates DDD, 7 estados de DI y Propuesta
│   ├── aggregates-incentivos.md           # Aggregates de gamificación, Insignia y Rankings
│   ├── aggregates-logistica.md            # Aggregates de transporte, rutas y camiones
│   ├── aggregates-notificaciones.md       # Réplica ligera y contratos REST sincrónicos
│   ├── contratos-rest.md                  # 🟢 Contratos REST consolidados, OpenAPI 3.0 y Swagger UI
│   ├── eventos-amqp.md                    # 🟢 Topología RabbitMQ y contratos de eventos asíncronos
│   ├── contratos/                         # 🟢 Especificaciones OpenAPI 3.0 (YAML), Schemas JSON y guía de migración E4
│   └── diseno/                            # 🟢 Bitácoras de refactor, diagramas PUML y anexos
│       ├── README.md                      # 🟢 Portal de bitácoras y diseño por microservicio
│       ├── donaciones/
│       ├── incentivos/
│       ├── logistica/
│       ├── notificaciones/
│       ├── common/                        # Estilos compartidos donatrack-style.puml
│       └── anexos-tecnicos/               # Modelos técnicos de build autogenerados por Maven
│
├── testing/                               # 🟢 Pruebas automatizadas y contratos
│   ├── README.md                          # 🟢 Portal canónico de testing y QA
│   ├── auditoria-arquitectura-testing.md  # 🏛️ Auditoría integral y Blueprint de Arquitectura de Testing y QA (Documento Maestro)
│   ├── integration-tests.md               # Arquitectura de tests E2E y clientes tipados
│   ├── testing-performance.md             # Optimización de compilación, surefire, slices WebMvc y TIA
│   ├── plan-auditoria-y-blueprint-qa.md   # Plan de auditoría y arquitectura target de QA
│   ├── decisiones-diseno-auditoria-qa.md  # Decisiones de diseño de testing y alternativas
│   ├── preprod-fiel.md                    # Preprod fiel: criterio, estado y brechas (fase 1)
│   ├── preprod-fiel-arquitectura-objetivo.md # Preprod fiel: arquitectura objetivo (fase 2)
│   ├── preprod-fiel-roadmap.md            # Preprod fiel: roadmap por entrega y ADR (fase 3)
│   ├── diagnostico-integration-tests-e5.md # Diagnóstico de integration-tests en E5
│   ├── auditoria/                         # 🟢 Compendio especializado de auditoría QA (01 a 06 + README)
│   └── postman/                           # LEGADO: copia anterior; la fuente vigente es ../postman/ (raíz del repo)
│
├── cicd/                                  # 🟢 Automatización, CI/CD y políticas de PR
│   ├── README.md                          # 🟢 Portal de CI/CD y automatización
│   ├── DonaTrack-CICD.md                  # Documentación de workflows de GitHub Actions
│   └── assignment_reminders_plan.md       # Recordatorios de inactividad de PRs en Discord
│
├── IA/                                    # 🟢 Lineamientos de ingeniería con Inteligencia Artificial
│   ├── README.md                          # Mapa de normas y prompts de IA
│   ├── 01-principios-de-uso.md … 05-antipatrones.md # Normas generales, checklist de PR y antipatrones
│   ├── 06-contexto-base-donatrack.md      # Snippet de contexto con stack técnico completo
│   ├── 07-errores-frecuentes-sonarcloud-ia.md # 🟢 Prevención y checklist pre-flight SonarCloud
│   ├── review/
│   │   └── evaluator.md                  # 🟢 Política Generator/Evaluator, Review Contract, vectores V1–V9
│   ├── evals/                             # 🟢 Infraestructura documental de eval suite v1 (E01 a E11)
│   ├── prompts/                           # Prompts especializados por rol de equipo
│   └── history/                           # AGENTS-v3.5.md: versión histórica de AGENTS.md
│
├── herramientas/                          # 🛠️ Aplicaciones web y utilidades locales
│   ├── README.md                          # 🟢 Portal de herramientas y utilidades
│   ├── documentador/                      # Generador interactivo de minutas y ADRs
│   └── hub/                               # Visor web de documentación y PDFs de entregas
│
├── adr/                                   # 🔒 Registros de Decisión de Arquitectura (Log4brains)
│   ├── README.md                          # 🟢 Fuente canónica de ADR governance (Two-Gate Rule, lifecycle, MADR)
│   ├── index.md                           # 🟢 Base de conocimientos local de Log4brains
│   ├── DEUDA_TECNICA.md                   # 🟢 Catálogo de deuda técnica diferida (DTI-01 a DTI-28)
│   └── donaciones, notificaciones, etc.   # Decisiones de arquitectura por microservicio (Log4brains)
│
├── specs/                                 # 🟢 Especificaciones técnicas y funcionales (SDD)
│   ├── README.md                          # Normas de ciclo de vida de especificaciones
│   ├── active/                            # Specs en curso de diseño o implementación (SPEC-03, SPEC-04, SPEC-05)
│   └── completed/                         # Specs cerradas y verificadas (SPEC-01, SPEC-02)
│
├── generated/                              # 🟢 Conocimiento generado mecánicamente del repositorio (Generated Knowledge)
│   ├── README.md                          # Visión general y reglas de regeneración
│   ├── endpoints-catalog.md               # Catálogo unificado de 100 endpoints REST
│   ├── events-catalog.md                  # Catálogo unificado de eventos AMQP y schemas
│   ├── architecture-graph.md              # Topología y grafo de dependencias entre microservicios
│   └── contracts-summary.json             # Sumario estructurado de contratos en formato JSON
│
├── agentes/                               # 🟢 Informes de investigación en desarrollo con agentes IA
│   ├── README.md                          # 🟢 Portal de investigación en ingeniería con agentes
│   ├── informe-agent-friendly-repository-architecture.md
│   ├── informe-harness-engineering-pi-gentle-pi.md
│   ├── informe-agent-graphs.md
│   ├── informe-orquestadores-control-planes-coding-agents.md
│   └── evals-agentes-resumen.md
│
├── requerimientos/                        # 🟢 Requerimientos por entrega
│   └── requerimientos-e6.md               # RF/RNF de E6 y decisiones D1–D30 (insumo de ADRs)
│
├── entrega-4/                             # 🟢 Documentación de Entrega 4 (índice: README.md)
│   ├── README.md                          # Índice de los .md de la entrega
│   ├── integracion/                       # Catálogo de mensajes y matriz productor-consumidor
│   ├── donaciones/                        # Bitácoras, plan y guion del broker de logística
│   ├── arquitectura/principios.md         # Reglas comunes de persistencia, outbox, aislamiento e idempotencia
│   ├── arquitectura/entregable-5/         # Entregable 5: informe .docx/.pdf, fuente y constructor
│   ├── arquitectura/arquitectura-sistema.md # Respaldo técnico verificado del entregable 5
│   ├── arquitectura/anexo-servicios.md     # Anexo por servicio del documento de arquitectura (entregable 3)
│   └── arquitectura/diagramas/            # Diagramas complementarios (PlantUML + PNG)
│       └── rabbit/                        # Topología RabbitMQ por flujo (exchanges, colas, DLQ)
│
└── entregas/                              # 🔒 Enunciados oficiales y diagramas entregados
    ├── README.md                          # 🟢 Matriz curricular e índice de entregas 1 a 7
    ├── 1/ … 4/                            # PDFs de requerimientos de cátedra
    ├── Enunciado-TP-Anual-2026.pdf        # Enunciado completo y final (entregas 1 a 7)
    └── 1/interfaz/                        # Bocetos Figma y mapa de navegación (dentro de la entrega 1)
```

---

## 3. Matriz de Componentes y Documentos Incorporados

Para evitar conflictos de merge recurrentes por solapamiento de índices secuenciales, la matriz se estructura por dominios y componentes estables:

### 3.1 Gobernanza, Metodología y Harness de IA

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`AGENTS.md`](../AGENTS.md) | Gobernanza Raíz | Núcleo canónico de políticas, niveles de tarea (QUICK/STANDARD/ARCHITECTURAL) y Quality Gates. | 🟢 Sincronizado |
| [`common-lib/AGENTS.md`](../common-lib/AGENTS.md) | Gobernanza Nested | Reglas de pertenencia del Shared Kernel, contratos protegidos y validación reactor. | 🟢 Sincronizado |
| [`cliente-liviano/AGENTS.md`](../cliente-liviano/AGENTS.md) | Gobernanza Nested | Guardarraíles de la interfaz (RNF-CL-06): proceso obligatorio, pertenencia, tokens, HTMX y validación. | 🟢 Sincronizado |
| [`.claude/skills/`](../.claude/skills/) y [`.claude/rules/interfaz.md`](../.claude/rules/interfaz.md) | Adaptadores Claude Code | Adaptadores que hacen descubribles las skills de interfaz y cargan las reglas al tocar `cliente-liviano/` o `docs/interfaz/`. | 🟢 Sincronizado |
| [`.claude/workflows/corrida-creativa.js`](../.claude/workflows/corrida-creativa.js) | Workflow Claude Code | Motor de la skill `corrida-creativa` (2026-10-31): iteraciones que escalan con constructores en paralelo, críticos, arreglos, evidencia, auditoría y PR. Se invoca por nombre. | 🟢 Sincronizado |
| [`scripts/visual/`](../scripts/visual/README.md) | Herramientas de interfaz | `Imagenes.java` (Java 21, sin dependencias): JPG de evidencia, comparación píxel a píxel, recorte, lado a lado y página de antes y después. | 🟢 Sincronizado |
| [`docs/context-index.md`](context-index.md) | Context Routing | Routing de contexto para agentes de IA por servicio, tarea y temporal constraints. | 🟢 Sincronizado |
| [`docs/IA/review/evaluator.md`](IA/review/evaluator.md) | Revisión Crítica | Roles Generator/Evaluator, Review Contract y vectores de revisión V1–V9. | 🟢 Sincronizado |
| [`scripts/agent-check/`](../scripts/agent-check/) | Enforcement CI | Suite de validación mecánica de integridad referencial, canonicidad, skills y ADRs. | 🟢 Sincronizado |
| [`.agents/skills/`](../.agents/skills/) | Suite de Skills | Suite de 15 skills: 8 del Engineering Loop y review de PRs, y 7 del proceso de diseño de la interfaz (identidad-visual, iteracion-ab, ux-flujo, vista-ui, revision-ui, relevamiento-creativo y corrida-creativa). | 🟢 Sincronizado |
| [`docs/specs/`](specs/README.md) | Especificaciones SDD | Catálogo centralizado de especificaciones funcionales y técnicas (active/completed). | 🟢 Sincronizado |
| [`docs/specs/active/SPEC-03-topologia-amqp-y-desacoplamiento-notificaciones.md`](specs/active/SPEC-03-topologia-amqp-y-desacoplamiento-notificaciones.md) | Especificación SDD | Topología AMQP Pub/Sub canónica, colas segregadas, envelope nativo, 10 schemas y Hard Cutover. | 🟢 Sincronizado |
| [`docs/specs/active/SPEC-04-broker-integracion-logistica.md`](specs/active/SPEC-04-broker-integracion-logistica.md) | Especificación SDD | Broker de Integración con Logística: selección de proveedor (AMQP/HTTP), comando dirigido, `mandatory` + acuse, outbox basado en datos. | 🟢 Sincronizado |
| [`docs/specs/active/SPEC-05-preprod-fiel-rediseno-y-rm01-rm08.md`](specs/active/SPEC-05-preprod-fiel-rediseno-y-rm01-rm08.md) | Especificación SDD | Rediseño del roadmap de preprod fiel (D1–D5) e implementación de RM-01 a RM-08. `APPROVED_BY_USER`. | 🟢 Sincronizado |
| [`docs/auditoria/auditoria-directivas-agentes.md`](auditoria/auditoria-directivas-agentes.md) | Auditoría de Repositorio | Diagnóstico de madurez Agent-Friendly (Score 5.0/5.0 — Nivel 4 Pleno) y directivas anti-sesgo. | 🟢 Sincronizado |
| [`docs/IA/evals/README.md`](IA/evals/README.md) | Evaluación de IA | Infraestructura de evaluación documental v1, suite ampliada de escenarios E01–E11 y scorecards. | 🟢 Sincronizado |
| [`scripts/run-evals.js`](../scripts/run-evals.js) | Harness Evals Runner | Runner determinista de escenarios E01–E11 y taxonomía de fallas críticas CF-01 a CF-12 con emisión de scorecard. | 🟢 Sincronizado |
| [`scripts/generate-repo-knowledge.js`](../scripts/generate-repo-knowledge.js) | Extractor de Contratos | Extractor mecánico de topología de microservicios, endpoints OpenAPI y eventos AMQP hacia `docs/generated/`. | 🟢 Sincronizado |
| [`docs/generated/`](generated/README.md) | Generated Knowledge | Catálogo vivo y estructurado de contratos, eventos, endpoints y grafo de arquitectura generado mecánicamente. | 🟢 Sincronizado |
| [`docs/agentes/README.md`](agentes/README.md) | Agentes IA | Portal de investigación y estado del arte en ingeniería de software con agentes autónomos. | 🟢 Sincronizado |
| [`docs/herramientas/README.md`](herramientas/README.md) | Herramientas Locales | Portal de utilidades locales, generador de documentación y visor web. | 🟢 Sincronizado |
| [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](IA/07-errores-frecuentes-sonarcloud-ia.md) | Calidad Estática | Guía viva de prevención de errores SonarCloud y auto-auditoría pre-flight. | 🟢 Sincronizado |

### 3.2 Arquitectura y Modelo de Dominio

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`docs/arquitectura/README.md`](arquitectura/README.md) | Portal Arquitectura | Portal e índice canónico de arquitectura, modelo de agregados y contratos. | 🟢 Sincronizado |
| [`docs/arquitectura/principios-diseno-arquitectura.md`](arquitectura/principios-diseno-arquitectura.md) | Fundamentos | Atributos de calidad, principios SOLID, GRASP, patrones GoF y fitness checks. | 🟢 Sincronizado |
| [`docs/arquitectura/shared-kernel.md`](arquitectura/shared-kernel.md) | Shared Kernel | Definición de `common-lib`, contratos de repositorios, excepciones unificadas y traceId. | 🟢 Sincronizado |
| [`docs/arquitectura/logging-trazabilidad.md`](arquitectura/logging-trazabilidad.md) | Observabilidad | Trazabilidad distribuida con MDC, header `X-Trace-Id` e interceptores Feign. | 🟢 Sincronizado |
| [`docs/arquitectura/guia-patrones-diseno.md`](arquitectura/guia-patrones-diseno.md) | Patrones de Diseño | Catálogo de patrones implementados (State, Strategy, Template Method, Observer, etc.). | 🟢 Sincronizado |
| [`docs/arquitectura/catalogo-errores.md`](arquitectura/catalogo-errores.md) | Catálogo de Errores | Catálogo exhaustivo de los 112 códigos de error unificados (ERR-INF, ERR-CSR, ERR-VAL, ERR-EST). | 🟢 Sincronizado |
| [`docs/arquitectura/aggregates-donaciones.md`](arquitectura/aggregates-donaciones.md) | Donaciones | Modelo de agregados DDD, máquina de 7 estados y propuesta de asignación. | 🟢 Sincronizado |
| [`docs/arquitectura/aggregates-notificaciones.md`](arquitectura/aggregates-notificaciones.md) | Notificaciones | Réplica de personas, adaptadores de envío y eventos notificables. | 🟢 Sincronizado |
| [`docs/arquitectura/aggregates-incentivos.md`](arquitectura/aggregates-incentivos.md) | Incentivos | Misiones con Template Method, insignias, eventos de gamificación y ranking. | 🟢 Sincronizado |
| [`docs/arquitectura/aggregates-logistica.md`](arquitectura/aggregates-logistica.md) | Logística | Ciclo de vida de entregas, planificación de rutas, camiones y eventos RabbitMQ. | 🟢 Sincronizado |
| [`docs/arquitectura/contratos-rest.md`](arquitectura/contratos-rest.md) | Contratos REST | Catálogo consolidado de endpoints, DTOs, Swagger UI y especificaciones OpenAPI 3.0. | 🟢 Sincronizado |
| [`docs/arquitectura/eventos-amqp.md`](arquitectura/eventos-amqp.md) | Mensajería AMQP | Topología RabbitMQ, TopicExchange, routing keys, payloads JSON e idempotencia. | 🟢 Sincronizado |
| [`docs/arquitectura/contratos/guia-migracion-notificaciones-e4.md`](arquitectura/contratos/guia-migracion-notificaciones-e4.md) | Migración E4 | Guía de migración técnica de contratos e ingesta AMQP/REST para `notificaciones-service`. | 🟢 Sincronizado |
| [`scripts/validate-contracts.js`](../scripts/validate-contracts.js) | Testing Contratos | Suite de validación mecánica de JSON Schemas, auditoría semántica de OpenAPI 3.0 (tipos y nulabilidad) integrada en CI (`agent-governance.yml`) y tests de drift en Surefire. | 🟢 Sincronizado |
| [`docs/arquitectura/diseno/README.md`](arquitectura/diseno/README.md) | Diseño de Dominio | Portal de diseño detallado de subsistemas y anexos técnicos. | 🟢 Sincronizado |
| [`docs/arquitectura/diseno/anexos-tecnicos/README.md`](arquitectura/diseno/anexos-tecnicos/README.md) | Diagramas Técnicos | Modelos técnicos de bytecode autogenerados por Maven (`plantuml-generator`). | 🟢 Sincronizado |
| [`docs/arquitectura/diseno/donaciones/diagrama-de-clases-donaciones.puml`](arquitectura/diseno/donaciones/diagrama-de-clases-donaciones.puml) | Diagrama de Clases de Donaciones | Dominio de Donaciones. Actualizado en la Entrega 5 para donación independiente, necesidades y propuestas (DTI-06: referencias por id, `DestinoAsignacion`, sin listas cruzadas; patente en `Entregada`; ítem con su ítem normalizado). Las copias `donaciones-clases.puml` y `lucid/donaciones-clases.puml` son idénticas salvo el `!include`. Otras partes del diagrama (dirección, personas) no se revisaron en esta pasada. | 🟡 Parcial |
| [`docs/arquitectura/diseno/donaciones/donaciones-DER.puml`](arquitectura/diseno/donaciones/donaciones-DER.puml) | DER de Donaciones (implementado) | Schema `donaciones` tal como lo crean las migraciones Flyway V1 a V13, con lo que difiere del DER de Lucid en rojo. Fuente de verdad: las migraciones. | 🟢 Sincronizado |
| [`docs/arquitectura/diseno/donaciones/cambios-der-donaciones.md`](arquitectura/diseno/donaciones/cambios-der-donaciones.md) | Cambios al DER de Donaciones | Lista, tabla por tabla, de lo que hay que cambiar en el DER de Lucid para que coincida con las migraciones, con el motivo de cada cambio. Incluye los dos puntos pendientes de discusión (D-37, D-38). | 🟢 Sincronizado |
| [`docs/arquitectura/diseno/donaciones/donaciones-componentes-persistencia.puml`](arquitectura/diseno/donaciones/donaciones-componentes-persistencia.puml) | Componentes de persistencia de Donaciones | Puertos del dominio, adapters JPA (perfil `postgres`) y en memoria, Transactional Outbox del broker y publicación después del commit. | 🟢 Sincronizado |
| [`docs/arquitectura/diseno/diagrama-de-componentes.png`](arquitectura/diseno/diagrama-de-componentes.png) | Diagrama de Componentes (objetivo) | Componentes del sistema y de integración de la Entrega 4 (entregable 4): servicios internos, externos simulados (APIs de WhatsApp, SMS y correo, planificador de rutas, proveedor de logística externo, red social) e infraestructura (PostgreSQL, RabbitMQ, MinIO, n8n), con interfaces provistas y requeridas y el protocolo de cada conector. Imagen elaborada por el grupo; reemplaza a la versión PlantUML. **Representa la arquitectura deseada (objetivo), no el estado actual**: al cierre de la Entrega 4 Donaciones trabajaba en memoria (desde la Entrega 5 persiste en PostgreSQL con el perfil `postgres`), solo Incentivos usaba MinIO (desde la rama `E4_donaciones_minio` Donaciones también, para padrones CSV y fotos de bienes) y los medios de notificación se simulan dentro del proceso (ver `docs/entrega-4/arquitectura/arquitectura-sistema.md` §2). | 🟢 Sincronizado |
| [`docs/adr/20260903-estandarizacion-de-codigos-de-estado-http-para-enrutamiento-y-recursos-no-encontrados.md`](adr/20260903-estandarizacion-de-codigos-de-estado-http-para-enrutamiento-y-recursos-no-encontrados.md) | ADR Contratos HTTP | Estandarización de respuestas 405 (con header Allow RFC 9110) y 404 en GlobalExceptionHandler. | 🟢 Sincronizado |

### 3.3 Persistencia y Base de Datos

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`persistencia/README.md`](../persistencia/README.md) | Base de Datos | Arquitectura de una base por servicio en el mismo PostgreSQL (DTI-26), roles, URLs JDBC y ciclo Flyway. | 🟢 Sincronizado |
| [`persistencia/init-db/01-init-schemas-roles.sql`](../persistencia/init-db/01-init-schemas-roles.sql) | Base de Datos | Inicialización idempotente de una base por servicio (dueño su rol, sin `CONNECT` para PUBLIC), su schema y los roles (sin contraseña). | 🟢 Sincronizado |
| [`persistencia/init-db/02-set-role-passwords.sh`](../persistencia/init-db/02-set-role-passwords.sh) | Base de Datos | Fija las contraseñas de los roles desde variables de entorno (`.env`, ver [`.env.example`](../.env.example)). | 🟢 Sincronizado |
| `notificaciones-service` (JPA + Flyway) | Persistencia | Mapeo relacional, migraciones Flyway V1 (schema) y V2 (inbox `evento_procesado`) y testing con Testcontainers PostgreSQL 16. | 🟢 Sincronizado |
| [`docs/adr/20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md`](adr/20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md) | ADR Persistencia | Formalización de arquitectura multi-schema y segregación de credenciales. `superseded` (2026-10-29) por el ADR de base por servicio. | 🟢 Sincronizado |
| [`docs/adr/20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md`](adr/20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md) | ADR Testing | Adopción de Testcontainers frente a bases en memoria H2 para paridad con producción. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260901-estrategia-de-mapeo-orm-en-notificaciones.md`](adr/notificaciones-service/20260901-estrategia-de-mapeo-orm-en-notificaciones.md) | ADR Mapeo ORM | Estrategia SINGLE_TABLE para medios de contacto y @ElementCollection para historial. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-transacciones-atomicas-cortas-y-despacho-asincrono-de-notificaciones.md`](adr/notificaciones-service/20260902-transacciones-atomicas-cortas-y-despacho-asincrono-de-notificaciones.md) | ADR Transacciones | Desacoplamiento de I/O externo, transacciones cortas y despacho asíncrono. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones.md`](adr/notificaciones-service/20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones.md) | ADR Idempotencia | Transactional Inbox Pattern con tabla evento_procesado y eventId UUID. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-sincronizacion-diferencial-de-medios-de-contacto-sin-key-churn.md`](adr/notificaciones-service/20260902-sincronizacion-diferencial-de-medios-de-contacto-sin-key-churn.md) | ADR Optimización ORM | Sincronización diferencial de medios en mapper para erradicar el key churn. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20261018-rechazo-de-sincronizacion-de-personas-anonimizadas-con-codigo-de-catalogo.md`](adr/notificaciones-service/20261018-rechazo-de-sincronizacion-de-personas-anonimizadas-con-codigo-de-catalogo.md) | ADR Errores | Rechazo de re-sincronización de personas anonimizadas con `BusinessStateException` + `ERR-EST-902`; propone reemplazar el eje 3 de `20260902-adapters-de-notificacion-y-observabilidad`. `proposed`. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md`](adr/notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md) | ADR Privacidad PII | Crypto-Shredding con Key Broker, Blind Index y desacoplamiento de PII cruzado. | 🟢 Sincronizado |
| `incentivos-service (JPA + Flyway)` | Persistencia | JPA bajo `@Profile("postgres")` con migraciones Flyway V1–V3 (incluidas inbox y outbox) y persistencia en memoria bajo `@Profile("!postgres")`. | 🟢 Sincronizado |
| `logistica-service (MongoDB)` | Persistencia | Perfil `mongo` (docker compose y preprod): documentos, mappers y adaptadores en `infrastructure/persistencia/mongo`, replica set de un nodo para transacciones y testing con Testcontainers MongoDB 7. | 🟢 Sincronizado |
| `logistica-service (JPA + Flyway)` | Persistencia | Perfil `postgres`, congelado desde E5 en `infrastructure/persistencia/jpa`: mapeo relacional, migraciones Flyway V1 (schema), V2 (`parada_ruta`), V3 (coordenadas de destino y fotos de recepción) y V4 (fotos en la auditoría de confirmación) y testing con Testcontainers PostgreSQL 16. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261007-direcciones-normalizadas-e-inmutables-en-logistica.md`](adr/logistica-service/20261007-direcciones-normalizadas-e-inmutables-en-logistica.md) | ADR Normalización | Direcciones en tablas normalizadas e inmutables; excepción por supresión de datos personales. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261007-pedido-al-planificador-despues-del-commit.md`](adr/logistica-service/20261007-pedido-al-planificador-despues-del-commit.md) | ADR Transacciones | Pedido al planificador externo después del commit para evitar la carrera con el callback. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261007-solicitudes-de-transicion-de-entrega-en-una-sola-tabla.md`](adr/logistica-service/20261007-solicitudes-de-transicion-de-entrega-en-una-sola-tabla.md) | ADR Mapeo ORM | Herencia de SolicitudTransicionEntrega en una sola tabla con discriminador y CHECK por tipo. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261007-orden-de-visita-de-las-paradas-de-una-ruta.md`](adr/logistica-service/20261007-orden-de-visita-de-las-paradas-de-una-ruta.md) | ADR Mapeo ORM | Tabla parada_ruta con orden de visita como colección ordenada de Ruta. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261007-referencias-a-donaciones-sin-clave-foranea.md`](adr/logistica-service/20261007-referencias-a-donaciones-sin-clave-foranea.md) | ADR Persistencia | Referencias por identidad a Donaciones sin FK entre schemas. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261009-despliegue-de-logistica-en-paas-con-contenedores.md`](adr/logistica-service/20261009-despliegue-de-logistica-en-paas-con-contenedores.md) | ADR Despliegue | Despliegue de Logística en PaaS con contenedor (Render) y servicios gestionados (Neon, CloudAMQP); desde E5 la base es MongoDB Atlas (nota de actualización). `proposed`. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261007-comentarios-sobre-el-mapeo-orm-de-logistica.md`](adr/logistica-service/20261007-comentarios-sobre-el-mapeo-orm-de-logistica.md) | ADR Mapeo ORM | Decisiones menores de mapeo: enums nativos, historiales, bloqueo optimista, carga de colecciones. | 🟢 Sincronizado |
| [`docs/adr/donaciones-service/20261022-desnormalizaciones-del-modelo-relacional-de-donaciones.md`](adr/donaciones-service/20261022-desnormalizaciones-del-modelo-relacional-de-donaciones.md) | ADR Desnormalización | Desnormalizaciones del DER de Donaciones (dirección plana, estado actual, asignación, foto del bien, SINGLE_TABLE, payload del outbox) y alternativas no adoptadas. D1 a D6 implementadas (D2 y D4 en los Pasos 7 y 8, D3 en el Paso 8, D6 en el Paso 10). Pendiente: que el equipo lo acepte. | 🟡 Parcial |
| [`docs/adr/donaciones-service/20260901-almacenamiento-de-objetos-minio-s3-para-padrones-y-archivos.md`](adr/donaciones-service/20260901-almacenamiento-de-objetos-minio-s3-para-padrones-y-archivos.md) | ADR (`accepted`, 2026-10-29) | MinIO para padrones CSV y fotos de bienes. Con sección de implementación (rama `E4_donaciones_minio`) y diferencias con la propuesta. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261025-persistencia-documental-de-logistica-en-mongodb.md`](adr/logistica-service/20261025-persistencia-documental-de-logistica-en-mongodb.md) | ADR Persistencia | Logística persiste en MongoDB con Spring Data MongoDB; selección de motor por perfil, JPA congelado y hosting en Atlas M0. `proposed`. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261025-modelo-documental-de-logistica-embebidos-y-referencias.md`](adr/logistica-service/20261025-modelo-documental-de-logistica-embebidos-y-referencias.md) | ADR Mapeo ODM | Un documento por agregado con sus value objects e historial embebidos, referencias por id entre agregados y auditorías en colecciones propias. `proposed`. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261025-desnormalizaciones-de-lectura-en-entregas-y-rutas.md`](adr/logistica-service/20261025-desnormalizaciones-de-lectura-en-entregas-y-rutas.md) | ADR Mapeo ODM | La entrega copia los datos de su asignación y la ruta los de sus paradas al momento de asignar, para leer de un solo documento y conservar la fidelidad histórica. `proposed`. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261025-transacciones-multi-documento-con-replica-set-en-logistica.md`](adr/logistica-service/20261025-transacciones-multi-documento-con-replica-set-en-logistica.md) | ADR Transacciones | `MongoTransactionManager` con replica set de un nodo en local y preprod; Atlas ya es replica set. `proposed`. | 🟢 Sincronizado |
| [`docs/adr/logistica-service/20261025-comentarios-sobre-el-mapeo-odm-de-logistica.md`](adr/logistica-service/20261025-comentarios-sobre-el-mapeo-odm-de-logistica.md) | ADR Mapeo ODM | Decisiones menores: fechas en UTC, ids UUID estándar, enums como texto, nombres de campos, bloqueo optimista e índices. `proposed`. | 🟢 Sincronizado |

### 3.4 Aseguramiento, Calidad y Deuda Técnica

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`docs/auditoria/README.md`](auditoria/README.md) | Portal Auditoría | Portal y catálogo de auditorías arquitectónicas, directivas y planes revisores. | 🟢 Sincronizado |
| [`docs/auditoria/plan-revisor-critico.md`](auditoria/plan-revisor-critico.md) | Auditoría | Marco metodológico, rúbricas de evaluación adversarial y matrices de control. | 🟢 Sincronizado |
| [`docs/auditoria/revision-critica-devops-ci.md`](auditoria/revision-critica-devops-ci.md) | Auditoría DevOps | Revisión crítica experta de pipelines CI/CD, Dockerfiles, observabilidad y scripts auxiliares. | 🟢 Sincronizado |
| [`docs/adr/DEUDA_TECNICA.md`](adr/DEUDA_TECNICA.md) | Deuda Técnica | Registro e índice de deudas técnicas diferidas (DTI-01 a DTI-28; DTI-15 reservada) con ADRs enlazados. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md`](adr/notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md) | ADR Deuda Técnica | DTI-07: Adaptador interino local para Crypto-Shredding mientras auth-service no exista. | 🟢 Sincronizado |
| [`docs/adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md`](adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md) | ADR Deuda Técnica | DTI-08: Campos de observabilidad diferidos (spanId, executionTimeMs, errorCode estructurado). | 🟢 Sincronizado |
| [`docs/adr/incentivos-service/20260905-dti-09-seguridad-y-asincronia-en-procesos-batch-de-incentivos.md`](adr/incentivos-service/20260905-dti-09-seguridad-y-asincronia-en-procesos-batch-de-incentivos.md) | ADR Deuda Técnica | DTI-09: Seguridad, control de acceso y asincronía en endpoints de procesos batch de incentivos. | 🟢 Sincronizado |
| [`docs/adr/incentivos-service/20260905-dti-10-desacoplamiento-de-errores-de-dominio-en-global-exception-handler.md`](adr/incentivos-service/20260905-dti-10-desacoplamiento-de-errores-de-dominio-en-global-exception-handler.md) | ADR Deuda Técnica | DTI-10: Desacoplamiento de errores de dominio de incentivos en GlobalExceptionHandler. | 🟢 Sincronizado |
| [`docs/adr/incentivos-service/20260905-dti-11-extraccion-de-mision-mapper-y-purificacion-de-mision-dto.md`](adr/incentivos-service/20260905-dti-11-extraccion-de-mision-mapper-y-purificacion-de-mision-dto.md) | ADR Deuda Técnica | DTI-11: Extracción de MisionMapper dedicado y purificación anémica de MisionDTO. | 🟢 Sincronizado |
| [`docs/adr/20260910-dti-13-migracion-clientes-api-rest-deprecada-notificaciones-a-amqp.md`](adr/20260910-dti-13-migracion-clientes-api-rest-deprecada-notificaciones-a-amqp.md) | ADR Deuda Técnica | DTI-13: Migración de clientes consumidores de la API REST deprecada de notificaciones a ruta canónica y AMQP. | 🟢 Sincronizado |
| [`docs/adr/20261009-dti-15-normalizacion-y-geocodificacion-de-direcciones.md`](adr/20261009-dti-15-normalizacion-y-geocodificacion-de-direcciones.md) | ADR Deuda Técnica | DTI-15: Normalización de direcciones contra API Georef en donaciones y geocodificación en logística. | 🟢 Sincronizado |
| [`docs/adr/README.md`](adr/README.md) | Gobernanza ADR | Fuente canónica del ciclo de vida de ADRs, Two-Gate Rule y especificación MADR. | 🟢 Sincronizado |
| [`docs/adr/20260911-harness-skill-review-pr-adversarial-grepai.md`](adr/20260911-harness-skill-review-pr-adversarial-grepai.md) | ADR Gobernanza IA | Formalización de la skill canónica review-pr, protocolo GrepAI y tooling CLI de contexto. | 🟢 Sincronizado |
| [`docs/adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md`](adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) | ADR Arquitectura AMQP | Topología Pub/Sub canónica DDD, colas segregadas, clúster DLQ y Hard Cutover inter-servicios. | 🟢 Sincronizado |
| [`docs/adr/20260919-convencion-canonica-identificadores-y-contratos-amqp.md`](adr/20260919-convencion-canonica-identificadores-y-contratos-amqp.md) | ADR Estándar de Contratos | Convención canónica de identificadores (<entidad>Id), separación AMQP/REST y erradicación de JsonAlias. | 🟢 Sincronizado |
| [`docs/adr/20261007-broker-de-integracion-con-logistica.md`](adr/20261007-broker-de-integracion-con-logistica.md) | ADR Integración | Broker in-process (Broker + Adapter + Strategy) entre Donaciones y Logística; regla de ruteo eventos por hecho / comandos por destinatario. | 🟢 Sincronizado |
| [`docs/adr/20261009-estilo-de-microservicios-con-capas-y-puertos-y-adaptadores.md`](adr/20261009-estilo-de-microservicios-con-capas-y-puertos-y-adaptadores.md) | ADR Estilo | Microservicios con capas e inversión de dependencias (puertos y adaptadores) por servicio. `proposed`. | 🟢 Sincronizado |
| [`docs/adr/20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md`](adr/20261008-vuelta-de-proveedores-de-logistica-por-mensajeria-con-identidad.md) | ADR Integración | Los proveedores de logística informan solo por mensajería (HTTP solo de ida), con id y token en headers verificados por Donaciones; refina la decisión 7 del ADR del broker. `proposed`. | 🟢 Sincronizado |
| [`docs/adr/20261009-tolerancia-al-desorden-de-eventos-en-incentivos-y-consistencia-eventual-en-tests.md`](adr/20261009-tolerancia-al-desorden-de-eventos-en-incentivos-y-consistencia-eventual-en-tests.md) | ADR Arquitectura AMQP | Incentivos tolera eventos fuera de orden (reintento con espera creciente y DLQ por cola) y la suite de integración espera la propagación de eventos (PollingUtils + ArchUnit, Toxiproxy en preprod). `proposed`. | 🟢 Sincronizado |
| [`docs/adr/20261020-promocion-de-artefactos-por-digest-y-despliegue-reproducible-de-logistica.md`](adr/20261020-promocion-de-artefactos-por-digest-y-despliegue-reproducible-de-logistica.md) | ADR CI/CD y Despliegue | Preprod post-merge sobre el digest candidato, promoción por re-etiquetado y manifiesto de versión; despliegue reproducible del manifiesto en el runner con smoke y rollback, y Render como adaptador opcional (revisión 2026-10-28). `proposed`. | 🟢 Sincronizado |
| [`docs/adr/20261028-base-de-datos-por-servicio-en-el-mismo-motor-postgresql.md`](adr/20261028-base-de-datos-por-servicio-en-el-mismo-motor-postgresql.md) | ADR Persistencia | Una base por servicio en el mismo PostgreSQL, con el schema `<servicio>` adentro; reemplaza al ADR multi-schema, que queda `superseded`. Implementado el 2026-10-29 (RM-19, DTI-26). `accepted` (2026-10-29). | 🟢 Sincronizado |
| [`docs/adr/20261028-politica-de-evolucion-de-contratos-entre-servicios.md`](adr/20261028-politica-de-evolucion-de-contratos-entre-servicios.md) | ADR Contratos | Contrato primero, schemas de eventos abiertos y chequeos nuevos en aviso en el PR y en error en el post-merge y en el corte de entrega (D5). `accepted` (2026-10-29). | 🟢 Sincronizado |
| [`docs/adr/cliente-liviano/20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md`](adr/cliente-liviano/20261030-stack-del-cliente-liviano-thymeleaf-htmx-pico-tokens-y-mapas.md) | ADR Interfaz | ADR-E6-06: stack del cliente liviano (D17–D21): Thymeleaf, HTMX 2, Pico.css, `tokens.css` y MapLibre; HTMX y Pico como WebJars. `proposed`. | 🟢 Sincronizado |
| [`docs/adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md`](adr/cliente-liviano/20261030-dos-alternativas-de-identidad-visual-con-hojas-propias.md) | ADR Interfaz | Dos alternativas de identidad (Glassmorphism y Cozy / Medieval) con su `tokens.css`, su `app.css` y su juego de plantillas propios (mismos datos y acciones vigilados por test; voz propia en títulos y rótulos, en una tabla por alternativa) y `?alternativa=` en el perfil `maqueta`; refina D20. `proposed`. | 🟢 Sincronizado |
| [`docs/adr/cliente-liviano/20261030-verificacion-visual-con-playwright-y-axe.md`](adr/cliente-liviano/20261030-verificacion-visual-con-playwright-y-axe.md) | ADR Testing | Verificación visual con Playwright Java y axe en el perfil Maven `visual`: capturas en 360/1280/1920, axe AA, toque y desbordes; fuera del CI (DTI-28). `proposed`. | 🟢 Sincronizado |

### 3.5 Infraestructura, CI/CD y Testing

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`docs/testing/README.md`](testing/README.md) | Portal Testing | Portal general y mapa de testing unitario, modular, E2E y auditoría de QA. | 🟢 Sincronizado |
| [`docs/cicd/README.md`](cicd/README.md) | Portal CI/CD | Portal de flujos de integración y despliegue continuo en GitHub Actions. | 🟢 Sincronizado |
| [`docs/cicd/DonaTrack-CICD.md`](cicd/DonaTrack-CICD.md) | CI/CD | Documentación de los 5 workflows de GitHub Actions (`main`, `merge`, `pr-reminders`, `deploy-pages`, `agent-governance`). | 🟢 Sincronizado |
| [`n8n/README.md`](../n8n/README.md) | Automatización | Workflows de n8n, webhooks de difusión de insignias y ranking mensual. | 🟢 Sincronizado |
| [`postman/README.md`](../postman/README.md) | Pruebas API | 9 colecciones Postman (250 requests), variables y ejecución Newman CLI. | 🟢 Sincronizado |
| [`docs/testing/auditoria-arquitectura-testing.md`](testing/auditoria-arquitectura-testing.md) | Auditoría Testing Master | Documento canónico maestro de auditoría factual de 1.191 tests, patologías F-01 a F-12, comparativa multicriterio, evaluación adversarial y blueprint target. | 🟢 Sincronizado |
| [`docs/testing/integration-tests.md`](testing/integration-tests.md) | Testing E2E | Infraestructura de pruebas de integración distribuida (Docker, RabbitMQ, PostgreSQL, n8n). | 🟢 Sincronizado |
| [`docs/testing/testing-performance.md`](testing/testing-performance.md) | Rendimiento QA | Optimización de compilación, surefire, consolidación WebMvc, scripts TIA y evaluación mvnd. | 🟢 Sincronizado |
| [`docs/testing/plan-auditoria-y-blueprint-qa.md`](testing/plan-auditoria-y-blueprint-qa.md) | Auditoría QA | Especificación y plan maestro de auditoría y arquitectura target de testing para ejecución con /goal. | 🟢 Sincronizado |
| [`docs/testing/decisiones-diseno-auditoria-qa.md`](testing/decisiones-diseno-auditoria-qa.md) | Decisiones QA | Registro y fundamentación de decisiones de diseño y análisis de alternativas descartadas. | 🟢 Sincronizado |
| [`docs/testing/preprod-fiel.md`](testing/preprod-fiel.md) | Preprod fiel | Criterio de 12 propiedades para un preprod fiel, medición de CI/CD e `integration-tests` sobre `origin/main` y brechas B1–B12. Fase 1 de 3. | 🟢 Sincronizado |
| [`docs/testing/preprod-fiel-arquitectura-objetivo.md`](testing/preprod-fiel-arquitectura-objetivo.md) | Preprod fiel | Alternativas ponderadas por brecha B1–B12, arquitectura objetivo de entorno, pipeline y estratos de la suite, y evaluación Gate A + Gate B de ADR. Fase 2 de 3. | 🟢 Sincronizado |
| [`docs/testing/preprod-fiel-roadmap.md`](testing/preprod-fiel-roadmap.md) | Preprod fiel | Roadmap RM-01 a RM-20 por entrega (E5–E7), rediseño de la SPEC-05, estado de implementación, proyección del puntaje, aprobaciones y confirmación Gate A + Gate B. Fase 3 de 3. | 🟢 Sincronizado |
| [`docs/testing/diagnostico-integration-tests-e5.md`](testing/diagnostico-integration-tests-e5.md) | Diagnóstico IT | Diagnóstico de `integration-tests` en E5: línea base de 3 corridas con stack nuevo, la última seguida de 2 más sobre el mismo stack, hallazgos DIT-01 a DIT-12 cruzados con F-xx, QA-GAP-xx y RM-xx, propuestas P1–P5 y su implementación en la fase 2 (§7). | 🟢 Sincronizado |
| [`docs/testing/auditoria/README.md`](testing/auditoria/README.md) | Portal Auditoría QA | Índice maestro, síntesis ejecutiva, mapa de navegación y glosario canónico de QA. | 🟢 Sincronizado |
| [`docs/testing/auditoria/01-diagnostico-ejecutivo.md`](testing/auditoria/01-diagnostico-ejecutivo.md) | Diagnóstico QA | Radiografía factual, evaluación teórica (Khorikov, Google SWE, Fowler) e INC-01. | 🟢 Sincronizado |
| [`docs/testing/auditoria/02-matriz-antipatrones.md`](testing/auditoria/02-matriz-antipatrones.md) | Smells QA | Matriz de hallazgos empíricos: AP-01 a AP-10 y fortalezas FT-01 a FT-05. | 🟢 Sincronizado |
| [`docs/testing/auditoria/03-estudio-comparativo.md`](testing/auditoria/03-estudio-comparativo.md) | Comparativa QA | Estudio multidimensional de alternativas (Ambientes, Contratos, Fitness, Performance). | 🟢 Sincronizado |
| [`docs/testing/auditoria/04-blueprint-target.md`](testing/auditoria/04-blueprint-target.md) | Blueprint QA | Arquitectura target: Testing Honeycomb, SLA por capa, paridad DDL y diagramas Mermaid. | 🟢 Sincronizado |
| [`docs/testing/auditoria/05-roadmap-migracion.md`](testing/auditoria/05-roadmap-migracion.md) | Roadmap QA | Plan de migración en 4 fases, preservación de compatibilidad docente y contingencias. | 🟢 Sincronizado |
| [`docs/testing/auditoria/06-cobertura-critica-qa-backlog.md`](testing/auditoria/06-cobertura-critica-qa-backlog.md) | Backlog QA | Diagnóstico de cobertura crítica faltante y backlog priorizado para futura iteración de QA. | 🟢 Sincronizado |
| [`docs/IA/06-contexto-base-donatrack.md`](IA/06-contexto-base-donatrack.md) | Contexto IA | Snippet de contexto con arquitectura de puertos, tecnologías y restricciones. | 🟢 Sincronizado |
| [`docs/adr/20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md`](adr/20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md) | ADR DevOps | Aislamiento de contenedores, usuario non-root y recolección de logs sin volúmenes de host. | 🟢 Sincronizado |
| [`docs/adr/20260903-protocolo-salida-semantico-y-quality-gate-estricto.md`](adr/20260903-protocolo-salida-semantico-y-quality-gate-estricto.md) | ADR CI/CD | Protocolo semántico de códigos de salida (0, 1, 2+) y política fail-if-no-tests. | 🟢 Sincronizado |
| [`docs/adr/20260903-estandarizacion-ciclo-vida-testing-surefire-failsafe.md`](adr/20260903-estandarizacion-ciclo-vida-testing-surefire-failsafe.md) | ADR Testing | Estandarización de ciclo de vida Maven: Surefire (test) y Failsafe (verify). | 🟢 Sincronizado |
| [`docs/adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md`](adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md) | ADR Observabilidad | Observabilidad estructurada en NDJSON y enriquecimiento MDC en el Shared Kernel. | 🟢 Sincronizado |
| [`docs/adr/20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes.md`](adr/20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes.md) | ADR QA Ambientes | Testcontainers en componentes (@ServiceConnection) y coexistencia con Docker Compose. | 🟢 Sincronizado |
| [`docs/adr/20260906-estrategia-contratos-openapi-wiremock-y-esquemas-amqp.md`](adr/20260906-estrategia-contratos-openapi-wiremock-y-esquemas-amqp.md) | ADR QA Contratos | Validación bidireccional OpenAPI, stubs WireMock y esquemas AMQP con JSON Schema. | 🟢 Sincronizado |
| [`docs/adr/20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest.md`](adr/20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest.md) | ADR QA Fitness | Fitness Functions de arquitectura con ArchUnit en mvn test y Pitest acotado. | 🟢 Sincronizado |
| [`docs/adr/20260906-migracion-pruebas-rendimiento-a-k6.md`](adr/20260906-migracion-pruebas-rendimiento-a-k6.md) | ADR QA Performance | Migración total de pruebas de rendimiento a k6, deprecación de PerfIT y wrapper docente. | 🟢 Sincronizado |

### 3.6 Bounded Contexts Reservados y Currícula

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`auth-service/README.md`](../auth-service/README.md) | Bounded Context | Placeholder: Bounded context reservado para autenticación (Entrega 6) y Key Broker (Entrega 7, decisión D29 de [requerimientos-e6](requerimientos/requerimientos-e6.md)). | 🟢 Sincronizado |
| [`cliente-liviano/README.md`](../cliente-liviano/README.md) | Bounded Context | Interfaz web de E6 (Spring MVC + Thymeleaf + HTMX): cómo levantarla, estado y enlaces al proceso de diseño. Módulo Maven con catálogo `/estilos`, perfil `maqueta` y verificación visual (`-Pvisual`). | 🟢 Sincronizado |
| [`docs/interfaz/README.md`](interfaz/README.md) | Diseño de Interfaz E6 | Proceso de diseño en 6 pasos, reglas para preguntar al usuario, anchos de referencia, criterio de listo y receta para extender. | 🟢 Sincronizado |
| [`docs/interfaz/`](interfaz/) (bocetos, mapa, dirección de arte, patrones, temas A/B, bitácora, plantillas) | Diseño de Interfaz E6 | Insumos, criterios y registro de decisiones del proceso de diseño del cliente liviano. La dirección de arte suma "Libertad creativa" y "Oficio" y las plantillas, los tres encargos de una corrida creativa (2026-10-31). | 🟢 Sincronizado |
| [`docs/interfaz/encargos/`](interfaz/encargos/README.md) | Diseño de Interfaz E6 | Encargos creativos aprobados por el usuario: la salida de `relevamiento-creativo` y la entrada de `corrida-creativa`. | 🟢 Sincronizado |
| [`docs/interfaz/identidad/brief.md`](interfaz/identidad/brief.md) y [`docs/interfaz/evidencia/`](interfaz/evidencia/) | Diseño de Interfaz E6 | Brief de la Alternativa Glassmorphism (prototipo v2 como punto de partida) y capturas de cada comparación A/B decidida, incluida la tanda de 7 rondas (paleta Grafito cálido y menta, estados, armazón privado, navegación, admin, tarjeta y modo oscuro). | 🟢 Sincronizado |
| [`docs/interfaz/identidad/brief-cozy-medieval.md`](interfaz/identidad/brief-cozy-medieval.md) y [`docs/interfaz/evidencia/cozy-medieval/`](interfaz/evidencia/cozy-medieval/) | Diseño de Interfaz E6 | Brief de la Alternativa Cozy / Medieval (juego de rol para adultos: mapa, pergamino, tinta, madera y latón; voz propia en títulos y rótulos) y capturas de sus comparaciones A/B. | 🟢 Sincronizado |
| [`docs/interfaz/estructura-cozy-medieval.md`](interfaz/estructura-cozy-medieval.md) | Diseño de Interfaz E6 | Directiva de estructura, navegación y momentos memorables de Cozy / Medieval (24 decisiones en 8 rondas, juego propio de plantillas) y su segunda versión, la mesa del cartógrafo (2026-10-31: mesa de nogal, cajones, mapa del viaje desde los datos, muestrario). Implementados: el juego de plantillas, la estructura base en `/estilos` y `/estilos/armazon` y sus 3 primeras comparaciones (bitácora). Pendiente: lo que espera pantallas o datos (tabla "Lo que todavía no se puede hacer"). | 🟡 Parcial |
| [`docs/interfaz/flujo-completo-cozy-medieval.md`](interfaz/flujo-completo-cozy-medieval.md) | Diseño de Interfaz E6 | El flujo completo del sitio en Cozy (iteración 3): recorrido por rol, 21 pantallas con sus estados y decisiones del prototipo navegable `/prototipo/<pantalla>` (perfil `maqueta`). Propuesta sin aprobar; cada pantalla real necesita su ficha de flujo y su gemela en Glassmorphism. | 🟡 Parcial |
| [`docs/interfaz/identidad/referencias-cozy-medieval/`](interfaz/identidad/referencias-cozy-medieval/README.md) | Diseño de Interfaz E6 | Las 18 imágenes de referencia de Cozy que trajo el usuario (2026-10-31), con qué se toma de cada una. Imágenes de terceros: no entran al sitio ni al entregable. | 🟢 Sincronizado |
| [`docs/requerimientos/requerimientos-e6.md`](requerimientos/requerimientos-e6.md) | Requerimientos Entrega 6 | RF y RNF del servicio de autenticación, el API gateway y el cliente liviano; huecos de backend; decisiones D1–D30 con alternativas (5 pendientes) y ADRs candidatos. | 🟢 Sincronizado |
| [`docs/entrega-4/arquitectura/principios.md`](entrega-4/arquitectura/principios.md) | Principios Entrega 4 | Reglas comunes de persistencia, outbox, aislamiento e idempotencia de Entrega 4. | 🟢 Sincronizado |
| [`docs/entrega-4/README.md`](entrega-4/README.md) | Índice Entrega 4 | Índice de todos los `.md` de `docs/entrega-4/` y de la guía de migración de Notificaciones. | 🟢 Sincronizado |
| [`docs/entrega-4/arquitectura/entregable-5/`](entrega-4/arquitectura/entregable-5/README.md) | Entregable 5 Entrega 4 | Informe de arquitectura en .docx/.pdf, su fuente `contenido.md`, las 5 figuras y el constructor. | 🟢 Sincronizado |
| [`docs/entrega-4/arquitectura/arquitectura-sistema.md`](entrega-4/arquitectura/arquitectura-sistema.md) | Arquitectura Entrega 4 | Respaldo técnico verificado del entregable 5: estilo, capas, patrones, integración, datos, despliegue, calidad y deuda; anexo por servicio. | 🟢 Sincronizado |
| [`docs/entrega-4/arquitectura/anexo-servicios.md`](entrega-4/arquitectura/anexo-servicios.md) | Anexo Entrega 4 | Detalle por servicio del documento de arquitectura (entregable 3): responsabilidad, agregados, entradas, salidas, datos y deuda propia. | 🟢 Sincronizado |
| [`docs/entrega-4/arquitectura/diagramas/`](entrega-4/arquitectura/diagramas/) | Diagramas Entrega 4 | Diagramas complementarios PlantUML + PNG: capas, integración AMQP, despliegue, secuencias del broker, planificación y eventos de logística, datos. Subcarpeta `rabbit/`: topología RabbitMQ por flujo. | 🟢 Sincronizado |
| [`docs/entrega-4/donaciones/plan-broker-logistica.md`](entrega-4/donaciones/plan-broker-logistica.md) | Plan Entrega 4 | Plan de implementación unificado del Broker de Integración con Logística. | 🟢 Sincronizado |
| [`docs/entrega-4/donaciones/bitacora-broker-logistica.md`](entrega-4/donaciones/bitacora-broker-logistica.md) | Bitácora Entrega 4 | Registro vivo de decisiones, preguntas y avances del broker (TLDR + Q&A). | 🟢 Sincronizado |
| [`docs/entrega-4/donaciones/bitacora-persistencia.md`](entrega-4/donaciones/bitacora-persistencia.md) | Bitácora Entrega 4 | Registro vivo de la persistencia JPA/PostgreSQL de Donaciones: pasos, decisiones sobre el DER, hallazgos y Q&A. | 🟢 Sincronizado |
| [`docs/entrega-4/donaciones/plan-minio-csv-y-fotos.md`](entrega-4/donaciones/plan-minio-csv-y-fotos.md) | Plan | Plan para guardar en MinIO los padrones CSV y las fotos de bienes de Donaciones (implementado en la rama `E4_donaciones_minio`). | 🟢 Sincronizado |
| [`docs/entrega-4/donaciones/bitacora-minio.md`](entrega-4/donaciones/bitacora-minio.md) | Bitácora | Registro vivo de la integración de Donaciones con MinIO: pasos, decisiones (contrato multipart del CSV, URL prefirmada para fotos, usuario de MinIO por servicio) y Q&A. | 🟢 Sincronizado |
| [`docs/entrega-4/donaciones/guion-demo-broker.md`](entrega-4/donaciones/guion-demo-broker.md) | Guion Entrega 4 | Guion de la demo del broker de logística (escenarios, comandos y qué explicar). | 🟢 Sincronizado |
| [`docs/entrega-4/donaciones/plan-implementacion-roles-donante-entidad.md`](entrega-4/donaciones/plan-implementacion-roles-donante-entidad.md) | Plan Entrega 4 | Plan por etapas del rediseño de los roles Donante y EntidadBeneficiaria (issue #888): identidad compartida, baja lógica y orden de eventos. | 🟢 Sincronizado |
| [`docs/entrega-4/donaciones/bitacora-roles-donante-entidad.md`](entrega-4/donaciones/bitacora-roles-donante-entidad.md) | Bitácora Entrega 4 | Registro vivo de decisiones, preguntas y avances del rediseño de los roles Donante y EntidadBeneficiaria (issue #888). | 🟢 Sincronizado |
| [`docs/entregas/README.md`](entregas/README.md) | Currícula Cátedra | Matriz curricular e índice de enunciados oficiales y artefactos de Entregas 1 a 4; Entregas 5 a 7 con el enunciado completo del TP Anual 2026. | 🟢 Sincronizado |


---

## 4. Estado de Formalización de ADRs y Decisiones de Arquitectura

* **Gobierno de Decisiones:** Todo cambio arquitectónico significativo se rige por la **Two-Gate Rule** formalizada en [`docs/adr/README.md`](adr/README.md).
* **Catálogo Integrado:** Los ADRs se organizan modularmente por servicio en `docs/adr/<servicio>/` y a nivel transversal en `docs/adr/`.
* **Auditoría Dinámica:** El estado y la consistencia de cada ADR (`proposed`, `accepted`, `rejected`, `superseded`) se audita de forma determinística en cada build mediante `scripts/agent-check.js` (`ADR_STATUS_VALID`), asegurando total trazabilidad sin requerir la actualización manual y conflictiva de totales numéricos en este documento.

