# Estado y Vigencia de la Documentación — DonaTrack

> **Panel de Auditoría y Matriz de Vigencia Documental vs. Código Fuente**  
> **Proyecto:** DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones  
> **Equipo:** UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Fecha de Normalización y Sincronización:** 2026-09-05  
> **Propósito:** Diagnóstico y estado de sincronización de toda la documentación del repositorio contrastada contra la implementación real en Java 21, Spring Boot 3, RabbitMQ y n8n tras la formalización de ADRs de oleadas de refactor y auditoría de deuda técnica.

---

## 1. Resumen Ejecutivo de Salud Documental

El repositorio DonaTrack mantiene su grafo documental estrictamente alineado con el código fuente ejecutable:

* **Sincronización:** 🟢 Plena. Todos los agregados de dominio, adaptadores, contratos REST, eventos AMQP y configuraciones de persistencia cuentan con respaldo documental canónico en `docs/`.
* **Discrepancias Críticas:** 0. No existen divergencias no catalogadas entre la documentación y el código fuente.
* **Verificación Mecánica:** La vigencia de enlaces internos, canonicidad de reglas de agentes, detección de términos obsoletos y validez de estados de ADRs se auditan automáticamente en CI/CD mediante el arnés de gobernanza (`scripts/agent-check.js`).

---

## 2. Mapa Estructural Canónico de docs/

```text
docs/
├── ESTADO_DOCUMENTACION.md                # 🟢 Panel central de auditoría y vigencia técnica
├── README.md                              # 🟢 Índice general de navegación y enlaces rápidos
│
├── auditoria/                             # 🟢 Sistema de aseguramiento y verificación adversarial
│   ├── plan-revisor-critico.md            # Plan de auditoría, checklists por etapa y matriz de evaluación
│   └── revision-critica-devops-ci.md      # 🟢 Revisión crítica y auditoría de CI/CD, Dockerfiles y scripts
│
├── arquitectura/                          # 🟢 Especificaciones de dominio, DDD, patrones y Shared Kernel
│   ├── principios-diseno-arquitectura.md  # Fundamentación teórica, 8 atributos, SOLID, GRASP, GoF, DDD
│   ├── shared-kernel.md                   # CrudRepository, eventos de dominio y OpenAPI
│   ├── logging-trazabilidad.md            # Observabilidad distribuida, MDC y traceId
│   ├── catalogo-errores.md                # 🟢 Catálogo exhaustivo de 108 códigos de error unificados
│   ├── analisis-arquitectonico.md         # Diagnóstico estructural del monorepo
│   ├── guia-patrones-diseno.md            # Catálogo de patrones de diseño aplicados
│   ├── aggregates-donaciones.md           # Aggregates DDD, 7 estados de DI y Propuesta
│   ├── aggregates-incentivos.md           # Aggregates de gamificación, Insignia y Rankings
│   ├── aggregates-logistica.md            # Aggregates de transporte, rutas y camiones
│   ├── aggregates-notificaciones.md       # Réplica ligera y contratos REST sincrónicos
│   ├── contratos-rest.md                  # 🟢 Contratos REST consolidados, OpenAPI 3.0 y Swagger UI
│   ├── eventos-amqp.md                    # 🟢 Topología RabbitMQ y contratos de eventos asíncronos
│   ├── contratos/                         # 🟢 Especificaciones OpenAPI 3.0 (YAML) y Schemas JSON
│   └── diseno/                            # Bitácoras de refactor, diagramas PUML y anexos
│       ├── donaciones/
│       ├── incentivos/
│       ├── logistica/
│       ├── notificaciones/
│       ├── common/                        # Estilos compartidos donatrack-style.puml
│       └── anexos-tecnicos/               # Modelos técnicos de build autogenerados por Maven
│
├── testing/                               # 🟢 Pruebas automatizadas y contratos
│   ├── integration-tests.md               # Arquitectura de tests E2E y clientes tipados
│   └── postman/                           # Colecciones y flujos E2E distribuidos
│
├── cicd/                                  # 🟢 Automatización, CI/CD y políticas de PR
│   ├── DonaTrack-CICD.md                  # Documentación de workflows de GitHub Actions
│   └── assignment_reminders_plan.md       # Recordatorios de inactividad de PRs en Discord
│
├── IA/                                    # 🟢 Lineamientos de ingeniería con Inteligencia Artificial
│   ├── README.md                          # Mapa de normas y prompts de IA
│   ├── 06-contexto-base-donatrack.md      # Snippet de contexto con stack técnico completo
│   ├── 07-errores-frecuentes-sonarcloud-ia.md # 🟢 Prevención y checklist pre-flight SonarCloud
│   ├── review/
│   │   └── evaluator.md                  # 🟢 Política Generator/Evaluator, Review Contract, vectores V1–V9
│   ├── evals/                             # 🟢 Infraestructura documental de eval suite v1
│   └── prompts/                           # Prompts especializados por rol de equipo
│
├── herramientas/                          # 🛠️ Aplicaciones web y utilidades locales
│   ├── documentador/                      # Generador interactivo de minutas y ADRs
│   └── hub/                               # Visor web de documentación y PDFs de entregas
│
├── adr/                                   # 🔒 Registros de Decisión de Arquitectura (Log4brains)
│   ├── README.md                          # 🟢 Fuente canónica de ADR governance (Two-Gate Rule, lifecycle, MADR)
│   ├── DEUDA_TECNICA.md                   # 🟢 Catálogo de deuda técnica diferida (DTI-01 a DTI-08)
│   └── donaciones, notificaciones, etc.   # Decisiones de arquitectura por microservicio (Log4brains)
│
└── entregas/                              # 🔒 Enunciados oficiales y diagramas entregados
    ├── README.md                          # 🟢 Matriz curricular e índice de entregas 1 a 4
    ├── 1/ … 4/                            # PDFs de requerimientos de cátedra
    └── interfaz/                          # Bocetos Figma y mapa de navegación
```

---

## 3. Matriz de Componentes y Documentos Incorporados

Para evitar conflictos de merge recurrentes por solapamiento de índices secuenciales, la matriz se estructura por dominios y componentes estables:

### 3.1 Gobernanza, Metodología y Harness de IA

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`AGENTS.md`](../AGENTS.md) | Gobernanza Raíz | Núcleo canónico de políticas, niveles de tarea (QUICK/STANDARD/ARCHITECTURAL) y Quality Gates. | 🟢 Sincronizado |
| [`common-lib/AGENTS.md`](../common-lib/AGENTS.md) | Gobernanza Nested | Reglas de pertenencia del Shared Kernel, contratos protegidos y validación reactor. | 🟢 Sincronizado |
| [`docs/context-index.md`](context-index.md) | Context Routing | Routing de contexto para agentes de IA por servicio, tarea y temporal constraints. | 🟢 Sincronizado |
| [`docs/IA/review/evaluator.md`](IA/review/evaluator.md) | Revisión Crítica | Roles Generator/Evaluator, Review Contract y vectores de revisión V1–V9. | 🟢 Sincronizado |
| [`scripts/agent-check/`](../scripts/agent-check/) | Enforcement CI | Suite de validación mecánica de integridad referencial, canonicidad y ADRs. | 🟢 Sincronizado |
| [`docs/IA/evals/README.md`](IA/evals/README.md) | Evaluación de IA | Infraestructura de evaluación documental v1, escenarios E01–E09 y scorecards. | 🟢 Sincronizado |
| [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](IA/07-errores-frecuentes-sonarcloud-ia.md) | Calidad Estática | Guía viva de prevención de errores SonarCloud y auto-auditoría pre-flight. | 🟢 Sincronizado |

### 3.2 Arquitectura y Modelo de Dominio

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`docs/arquitectura/principios-diseno-arquitectura.md`](arquitectura/principios-diseno-arquitectura.md) | Fundamentos | Atributos de calidad, principios SOLID, GRASP, patrones GoF y fitness checks. | 🟢 Sincronizado |
| [`docs/arquitectura/shared-kernel.md`](arquitectura/shared-kernel.md) | Shared Kernel | Definición de `common-lib`, contratos de repositorios, excepciones unificadas y traceId. | 🟢 Sincronizado |
| [`docs/arquitectura/logging-trazabilidad.md`](arquitectura/logging-trazabilidad.md) | Observabilidad | Trazabilidad distribuida con MDC, header `X-Trace-Id` e interceptores Feign. | 🟢 Sincronizado |
| [`docs/arquitectura/guia-patrones-diseno.md`](arquitectura/guia-patrones-diseno.md) | Patrones de Diseño | Catálogo de patrones implementados (State, Strategy, Template Method, Observer, etc.). | 🟢 Sincronizado |
| [`docs/arquitectura/catalogo-errores.md`](arquitectura/catalogo-errores.md) | Catálogo de Errores | Catálogo exhaustivo de los 108 códigos de error unificados (ERR-INF, ERR-CSR, ERR-VAL, ERR-EST). | 🟢 Sincronizado |
| [`docs/arquitectura/aggregates-donaciones.md`](arquitectura/aggregates-donaciones.md) | Donaciones | Modelo de agregados DDD, máquina de 7 estados y propuesta de asignación. | 🟢 Sincronizado |
| [`docs/arquitectura/aggregates-notificaciones.md`](arquitectura/aggregates-notificaciones.md) | Notificaciones | Réplica de personas, adaptadores de envío y eventos notificables. | 🟢 Sincronizado |
| [`docs/arquitectura/aggregates-incentivos.md`](arquitectura/aggregates-incentivos.md) | Incentivos | Misiones con Template Method, insignias, eventos de gamificación y ranking. | 🟢 Sincronizado |
| [`docs/arquitectura/aggregates-logistica.md`](arquitectura/aggregates-logistica.md) | Logística | Ciclo de vida de entregas, planificación de rutas, camiones y eventos RabbitMQ. | 🟢 Sincronizado |
| [`docs/arquitectura/contratos-rest.md`](arquitectura/contratos-rest.md) | Contratos REST | Catálogo consolidado de endpoints, DTOs, Swagger UI y especificaciones OpenAPI 3.0. | 🟢 Sincronizado |
| [`docs/arquitectura/eventos-amqp.md`](arquitectura/eventos-amqp.md) | Mensajería AMQP | Topología RabbitMQ, TopicExchange, routing keys, payloads JSON e idempotencia. | 🟢 Sincronizado |
| [`scripts/validate-contracts.js`](../scripts/validate-contracts.js) | Testing Contratos | Suite de validación mecánica de JSON Schemas y especificaciones OpenAPI 3.0. | 🟢 Sincronizado |
| [`docs/arquitectura/diseno/anexos-tecnicos/README.md`](arquitectura/diseno/anexos-tecnicos/README.md) | Diagramas Técnicos | Modelos técnicos de bytecode autogenerados por Maven (`plantuml-generator`). | 🟢 Sincronizado |
| [`docs/adr/20260903-estandarizacion-de-codigos-de-estado-http-para-enrutamiento-y-recursos-no-encontrados.md`](adr/20260903-estandarizacion-de-codigos-de-estado-http-para-enrutamiento-y-recursos-no-encontrados.md) | ADR Contratos HTTP | Estandarización de respuestas 405 (con header Allow RFC 9110) y 404 en GlobalExceptionHandler. | 🟢 Sincronizado |

### 3.3 Persistencia y Base de Datos

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`persistencia/README.md`](../persistencia/README.md) | Base de Datos | Arquitectura multi-schema, roles PostgreSQL, URLs JDBC y ciclo Flyway. | 🟢 Sincronizado |
| [`persistencia/init-db/01-init-schemas-roles.sql`](../persistencia/init-db/01-init-schemas-roles.sql) | Base de Datos | Inicialización idempotente de esquemas PostgreSQL y roles con privilegios mínimos. | 🟢 Sincronizado |
| `notificaciones-service` (JPA + Flyway) | Persistencia | Mapeo relacional, migraciones Flyway V1 y testing con Testcontainers PostgreSQL 16. | 🟢 Sincronizado |
| [`docs/adr/20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md`](adr/20260902-arquitectura-de-persistencia-multi-schema-y-aislamiento-de-roles-en-postgresql.md) | ADR Persistencia | Formalización de arquitectura multi-schema y segregación de credenciales. | 🟢 Sincronizado |
| [`docs/adr/20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md`](adr/20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md) | ADR Testing | Adopción de Testcontainers frente a bases en memoria H2 para paridad con producción. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260901-estrategia-de-mapeo-orm-en-notificaciones.md`](adr/notificaciones-service/20260901-estrategia-de-mapeo-orm-en-notificaciones.md) | ADR Mapeo ORM | Estrategia SINGLE_TABLE para medios de contacto y @ElementCollection para historial. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-transacciones-atomicas-cortas-y-despacho-asincrono-de-notificaciones.md`](adr/notificaciones-service/20260902-transacciones-atomicas-cortas-y-despacho-asincrono-de-notificaciones.md) | ADR Transacciones | Desacoplamiento de I/O externo, transacciones cortas y despacho asíncrono. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones.md`](adr/notificaciones-service/20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones.md) | ADR Idempotencia | Transactional Inbox Pattern con tabla evento_procesado y eventId UUID. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-sincronizacion-diferencial-de-medios-de-contacto-sin-key-churn.md`](adr/notificaciones-service/20260902-sincronizacion-diferencial-de-medios-de-contacto-sin-key-churn.md) | ADR Optimización ORM | Sincronización diferencial de medios en mapper para erradicar el key churn. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md`](adr/notificaciones-service/20260902-proteccion-de-pii-crypto-shredding-y-desacoplamiento-de-mensajes.md) | ADR Privacidad PII | Crypto-Shredding con Key Broker, Blind Index y desacoplamiento de PII cruzado. | 🟢 Sincronizado |

### 3.4 Aseguramiento, Calidad y Deuda Técnica

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`docs/auditoria/plan-revisor-critico.md`](auditoria/plan-revisor-critico.md) | Auditoría | Marco metodológico, rúbricas de evaluación adversarial y matrices de control. | 🟢 Sincronizado |
| [`docs/auditoria/revision-critica-devops-ci.md`](auditoria/revision-critica-devops-ci.md) | Auditoría DevOps | Revisión crítica experta de pipelines CI/CD, Dockerfiles, observabilidad y scripts auxiliares. | 🟢 Sincronizado |
| [`docs/adr/DEUDA_TECNICA.md`](adr/DEUDA_TECNICA.md) | Deuda Técnica | Registro e índice de deudas técnicas diferidas (DTI-01 a DTI-08) con ADRs enlazados. | 🟢 Sincronizado |
| [`docs/adr/notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md`](adr/notificaciones-service/20260902-dti-07-dependencia-diferida-de-auth-service-para-key-broker.md) | ADR Deuda Técnica | DTI-07: Adaptador interino local para Crypto-Shredding mientras auth-service no exista. | 🟢 Sincronizado |
| [`docs/adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md`](adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md) | ADR Deuda Técnica | DTI-08: Campos de observabilidad diferidos (spanId, executionTimeMs, errorCode estructurado). | 🟢 Sincronizado |
| [`docs/adr/README.md`](adr/README.md) | Gobernanza ADR | Fuente canónica del ciclo de vida de ADRs, Two-Gate Rule y especificación MADR. | 🟢 Sincronizado |

### 3.5 Infraestructura, CI/CD y Testing

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`docs/cicd/DonaTrack-CICD.md`](cicd/DonaTrack-CICD.md) | CI/CD | Documentación de los 7 flujos automatizados en GitHub Actions. | 🟢 Sincronizado |
| [`n8n/README.md`](../n8n/README.md) | Automatización | Workflows de n8n, webhooks de difusión de insignias y ranking mensual. | 🟢 Sincronizado |
| [`postman/README.md`](../postman/README.md) | Pruebas API | 8 colecciones Postman (170 requests), variables y ejecución Newman CLI. | 🟢 Sincronizado |
| [`docs/testing/integration-tests.md`](testing/integration-tests.md) | Testing E2E | Infraestructura de pruebas de integración distribuida (Docker, RabbitMQ, PostgreSQL, n8n). | 🟢 Sincronizado |
| [`docs/testing/plan-auditoria-y-blueprint-qa.md`](testing/plan-auditoria-y-blueprint-qa.md) | Auditoría QA | Especificación y plan maestro de auditoría y arquitectura target de testing para ejecución con /goal. | 🟢 Sincronizado |
| [`docs/testing/decisiones-diseno-auditoria-qa.md`](testing/decisiones-diseno-auditoria-qa.md) | Decisiones QA | Registro y fundamentación de decisiones de diseño y análisis de alternativas descartadas. | 🟢 Sincronizado |
| [`docs/IA/06-contexto-base-donatrack.md`](IA/06-contexto-base-donatrack.md) | Contexto IA | Snippet de contexto con arquitectura de puertos, tecnologías y restricciones. | 🟢 Sincronizado |
| [`docs/adr/20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md`](adr/20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md) | ADR DevOps | Aislamiento de contenedores, usuario non-root y recolección de logs sin volúmenes de host. | 🟢 Sincronizado |
| [`docs/adr/20260903-protocolo-salida-semantico-y-quality-gate-estricto.md`](adr/20260903-protocolo-salida-semantico-y-quality-gate-estricto.md) | ADR CI/CD | Protocolo semántico de códigos de salida (0, 1, 2+) y política fail-if-no-tests. | 🟢 Sincronizado |
| [`docs/adr/20260903-estandarizacion-ciclo-vida-testing-surefire-failsafe.md`](adr/20260903-estandarizacion-ciclo-vida-testing-surefire-failsafe.md) | ADR Testing | Estandarización de ciclo de vida Maven: Surefire (test) y Failsafe (verify). | 🟢 Sincronizado |
| [`docs/adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md`](adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md) | ADR Observabilidad | Observabilidad estructurada en NDJSON y enriquecimiento MDC en el Shared Kernel. | 🟢 Sincronizado |

### 3.6 Bounded Contexts Reservados y Currícula

| Documento / Artefacto | Área | Propósito / Alcance | Estado |
|---|---|---|:---:|
| [`auth-service/README.md`](../auth-service/README.md) | Bounded Context | Placeholder: Bounded context reservado para autenticación y Key Broker (Entrega 6). | 🟢 Sincronizado |
| [`cliente-liviano/README.md`](../cliente-liviano/README.md) | Bounded Context | Placeholder: Bounded context reservado para interfaz Web MVC (Entrega 5). | 🟢 Sincronizado |
| [`docs/entregas/README.md`](entregas/README.md) | Currícula Cátedra | Matriz curricular e índice de enunciados oficiales y artefactos de Entregas 1 a 4. | 🟢 Sincronizado |


---

## 4. Estado de Formalización de ADRs y Decisiones de Arquitectura

* **Gobierno de Decisiones:** Todo cambio arquitectónico significativo se rige por la **Two-Gate Rule** formalizada en [`docs/adr/README.md`](adr/README.md).
* **Catálogo Integrado:** Los ADRs se organizan modularmente por servicio en `docs/adr/<servicio>/` y a nivel transversal en `docs/adr/`.
* **Auditoría Dinámica:** El estado y la consistencia de cada ADR (`proposed`, `accepted`, `rejected`, `superseded`) se audita de forma determinística en cada build mediante `scripts/agent-check.js` (`ADR_STATUS_VALID`), asegurando total trazabilidad sin requerir la actualización manual y conflictiva de totales numéricos en este documento.

