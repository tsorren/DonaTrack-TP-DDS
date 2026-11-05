# DonaTrack — Context Index

> Routing de contexto para coding agents.
> Cargar `/AGENTS.md` primero. Usar este índice para decidir qué contexto adicional cargar.

---

## Cómo usar este índice

1. **Tarea en un servicio específico** → ir a Service Context, cargar el doc primario
2. **Codebase desconocido, orientación global o tarea cross-service** → cargar System Primer primero
3. **Tarea que toca persistencia o diseño de dominio** → revisar Temporal Constraints
4. **Pre-PR o cierre de tarea** → cargar Pre-PR Context
5. **Nunca** cargar `docs/` completo ni todos los ADRs simultáneamente

---

## System Primer

Cargar cuando: codebase desconocido, orientación global del sistema, o tarea cross-service sin contexto previo de la sesión.

[`docs/IA/06-contexto-base-donatrack.md`](IA/06-contexto-base-donatrack.md) — Stack, 4 servicios con puertos, roles de equipo, reglas de interacción con IA.

---

## Service Context (Level 2)

| Servicio | Doc primario | También cargar si... |
| --- | --- | --- |
| `donaciones-service` | [`arquitectura/aggregates-donaciones.md`](arquitectura/aggregates-donaciones.md) | State Pattern o algoritmos → ADRs en `docs/adr/donaciones-service/`; AMQP / Pub-Sub → [`arquitectura/eventos-amqp.md`](arquitectura/eventos-amqp.md), ADR transversal [`adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md`](adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md); historial de refactor → `docs/arquitectura/diseno/donaciones/`; broker de integración con logística → ADR transversal [`adr/20261007-broker-de-integracion-con-logistica.md`](adr/20261007-broker-de-integracion-con-logistica.md) y [bitácora](entrega-4/donaciones/bitacora-broker-logistica.md) |
| `logistica-service` | [`arquitectura/aggregates-logistica.md`](arquitectura/aggregates-logistica.md) | Eventos RabbitMQ → ADR `20260703-uso-de-rabbitmq-*` en `docs/adr/logistica-service/`; trazabilidad → [`arquitectura/logging-trazabilidad.md`](arquitectura/logging-trazabilidad.md); broker de integración con logística → ADR transversal [`adr/20261007-broker-de-integracion-con-logistica.md`](adr/20261007-broker-de-integracion-con-logistica.md) y [bitácora](entrega-4/donaciones/bitacora-broker-logistica.md) |
| `incentivos-service` | [`arquitectura/aggregates-incentivos.md`](arquitectura/aggregates-incentivos.md) | Scheduler / cron → ADRs en `docs/adr/incentivos-service/`; AMQP / Pub-Sub → [`arquitectura/eventos-amqp.md`](arquitectura/eventos-amqp.md), ADR transversal [`adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md`](adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md) |
| `notificaciones-service` | [`arquitectura/aggregates-notificaciones.md`](arquitectura/aggregates-notificaciones.md) | REST / Persistencia JPA / AMQP RabbitMQ Pub-Sub → ADRs en `docs/adr/notificaciones-service/`, ADR transversal [`adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md`](adr/20260911-topologia-pubsub-amqp-y-desacoplamiento-notificaciones.md), [`arquitectura/eventos-amqp.md`](arquitectura/eventos-amqp.md) y [`adr/DEUDA_TECNICA.md`](adr/DEUDA_TECNICA.md) (DTI-13) |
| `common-lib` | [`arquitectura/shared-kernel.md`](arquitectura/shared-kernel.md) | Impacto cross-service → aggregates docs de los servicios afectados |
| `cliente-liviano` | [`../cliente-liviano/AGENTS.md`](../cliente-liviano/AGENTS.md) | Proceso de diseño, identidad y flujos → [`interfaz/README.md`](interfaz/README.md); stack (D17–D21) y verificación visual → ADRs en `docs/adr/cliente-liviano/`; verificación fuera del CI → [`adr/DEUDA_TECNICA.md`](adr/DEUDA_TECNICA.md) (DTI-28) |

---

## Task Context (Level 2)

| Tipo de tarea | Cargar |
| --- | --- |
| Diseño arquitectónico / review | [`arquitectura/principios-diseno-arquitectura.md`](arquitectura/principios-diseno-arquitectura.md) |
| Creación de ADR | [`adr/README.md`](adr/README.md) + [`adr/DEUDA_TECNICA.md`](adr/DEUDA_TECNICA.md) + ADRs previos del mismo servicio |
| Testing / integración / QA | [`testing/integration-tests.md`](testing/integration-tests.md) + [`testing/testing-performance.md`](testing/testing-performance.md) + [`testing/auditoria-arquitectura-testing.md`](testing/auditoria-arquitectura-testing.md) + [`testing/auditoria/README.md`](testing/auditoria/README.md) |
| CI/CD / pipeline / Docker | [`cicd/DonaTrack-CICD.md`](cicd/DonaTrack-CICD.md) |
| Code quality / SonarCloud | [`IA/07-errores-frecuentes-sonarcloud-ia.md`](IA/07-errores-frecuentes-sonarcloud-ia.md) |
| Revisión adversarial (Fase 6) | [`auditoria/plan-revisor-critico.md`](auditoria/plan-revisor-critico.md) |
| Trazabilidad / RabbitMQ / Feign | [`arquitectura/logging-trazabilidad.md`](arquitectura/logging-trazabilidad.md) |
| Contratos REST / OpenAPI | [`arquitectura/contratos-rest.md`](arquitectura/contratos-rest.md) + `docs/arquitectura/contratos/` |
| Eventos AMQP / RabbitMQ | [`arquitectura/eventos-amqp.md`](arquitectura/eventos-amqp.md) + `docs/arquitectura/contratos/schemas/` |
| Especificaciones SDD (Fase 1/2) | [`specs/README.md`](specs/README.md) + `docs/specs/active/` |
| Review de Pull Requests (GrepAI) | [`.agents/skills/review-pr/SKILL.md`](../.agents/skills/review-pr/SKILL.md) + [`IA/review/evaluator.md`](IA/review/evaluator.md) |
| Catálogo Vivo de Contratos | [`generated/README.md`](generated/README.md) + `docs/generated/` |
| Mentoría conceptual / didáctica | [`IA/prompts/transversal-mentoria-conceptos.md`](IA/prompts/transversal-mentoria-conceptos.md) + skill `.agents/skills/explain-concept/` |
| Compliance académico | `docs/entregas/<N>/Enunciado-<N>.pdf` — solo el enunciado de la entrega vigente |
| Interfaz del cliente liviano (UX, identidad visual, vistas) | [`interfaz/README.md`](interfaz/README.md) + [`../cliente-liviano/AGENTS.md`](../cliente-liviano/AGENTS.md) + skills de interfaz (abajo) |

### Skills de `.agents/skills/`

| Skill | Cuándo usarla |
| --- | --- |
| [`engineering-loop`](../.agents/skills/engineering-loop/SKILL.md) | Ciclo completo guiado por agentes con enrutamiento por criticidad (QUICK / STANDARD / ARCHITECTURAL). |
| [`define-spec`](../.agents/skills/define-spec/SKILL.md) | Elicitar requerimientos y definir una especificación SDD con alternativas comparadas. |
| [`design-spec`](../.agents/skills/design-spec/SKILL.md) | Diseño técnico detallado desde una spec aprobada (clases, contratos, Two-Gate Rule, plan TDD). |
| [`design-review`](../.agents/skills/design-review/SKILL.md) | Revisión adversarial de specs y diseño pre-código (vectores D1–D6). |
| [`implement-task`](../.agents/skills/implement-task/SKILL.md) | Implementación con TDD, baseline previo y Quality Gates locales. |
| [`implementation-review`](../.agents/skills/implementation-review/SKILL.md) | Revisión adversarial de código implementado (vectores V1–V9). |
| [`review-pr`](../.agents/skills/review-pr/SKILL.md) | Review de Pull Requests con GrepAI. |
| [`explain-concept`](../.agents/skills/explain-concept/SKILL.md) | Mentoría conceptual y explicaciones didácticas. |
| [`identidad-visual`](../.agents/skills/identidad-visual/SKILL.md) | Definir o ajustar la identidad visual del cliente liviano con rondas de preguntas, A/B y `tokens.css`. |
| [`iteracion-ab`](../.agents/skills/iteracion-ab/SKILL.md) | Comparar dos variantes de un solo tema de diseño y dejar que el usuario elija. |
| [`ux-flujo`](../.agents/skills/ux-flujo/SKILL.md) | Diseñar un flujo de pantallas antes del código: recorrido, estados, textos y heurísticas. |
| [`vista-ui`](../.agents/skills/vista-ui/SKILL.md) | Construir vistas y componentes Thymeleaf + HTMX con estados, accesibilidad y comprobación visual. |
| [`revision-ui`](../.agents/skills/revision-ui/SKILL.md) | Revisión independiente de flujos y pantallas (vectores U1–U9). |

> Las skills de interfaz tienen además un adaptador en `.claude/skills/<nombre>/SKILL.md` para que Claude Code las descubra; la versión canónica es la de `.agents/skills/`.

---

## Temporal Constraints (Phase 1 — activas por servicio)

Revisar esta sección si la tarea involucra: persistencia, repositorios, diseño de dominio, SQL, `@Entity`, o Entrega 2.

> ⚠️ Estas constraints son **scoped por servicio**. Una decisión de integrar JPA en un servicio no invalida la constraint de los demás. Una constraint deja de aplicar cuando existe una decisión canónica (ADR aprobado o implementación integrada) para **ese servicio específico** que la reemplaza explícitamente.
>
> Los *drift signals* no invalidan una constraint: indican que la documentación podría estar desactualizada y debe revisarse.

| Constraint | Scope | Regla vigente | Fuente de autoridad | Drift signal |
| --- | --- | --- | --- | --- |
| Persistencia en memoria | Reemplazada en los cuatro servicios | Ya no aplica: los cuatro servicios persisten y retienen la persistencia en memoria bajo `@Profile("!postgres")`. `notificaciones-service`: JPA con Flyway V1+V2; `logistica-service`: MongoDB con Spring Data MongoDB bajo `@Profile("mongo")` (perfil de docker compose y preprod; ADRs `20261025-*` `proposed`) y JPA con Flyway V1–V4 congelado bajo `@Profile("postgres")`; `incentivos-service`: JPA con Flyway V1–V3 bajo `@Profile("postgres")` y `@Version` en `DonanteIncentivosEntity`; `donaciones-service`: JPA con Flyway V1 a V13 bajo `@Profile("postgres")` (Entrega 5, ver [`entrega-4/donaciones/bitacora-persistencia.md`](entrega-4/donaciones/bitacora-persistencia.md)). Los ADR de mapeo ORM de Incentivos y Donaciones siguen `proposed` | [`adr/DEUDA_TECNICA.md`](adr/DEUDA_TECNICA.md) DTI-01 a DTI-06 | `spring-boot-starter-data-jpa` o `spring-boot-starter-data-mongodb` en el `pom.xml` de un servicio nuevo que no esté en la lista |
| Pureza de dominio | Todos los servicios (Fase 1) | Entidades de dominio sin anotaciones JPA ni acoplamiento a infraestructura de persistencia | [`adr/DEUDA_TECNICA.md`](adr/DEUDA_TECNICA.md) DTI-01 + DTI-06 | `@Entity` / `@Column` en `models/entities/` — revisar si existe ADR que autorice la excepción para ese servicio |


---

## Level 3 — Cargar solo cuando la tarea lo requiere explícitamente

| Contexto | Cuándo | Dónde |
| --- | --- | --- |
| Rationale de una decisión histórica | "¿Por qué se diseñó X así?" | `docs/adr/<servicio>/YYYYMMDD-<slug>.md` |
| Historial de un refactor | Rastrear evolución de un componente | `docs/arquitectura/diseno/<servicio>/` |
| Diagnóstico de estado actual | Análisis estructural del monorepo | [`arquitectura/analisis-arquitectonico.md`](arquitectura/analisis-arquitectonico.md) |

---

## No cargar por defecto

- `docs/entregas/` — solo el enunciado de la entrega vigente y solo para compliance
- `docs/arquitectura/diseno/` — solo para rastrear una decisión histórica concreta
- El catálogo completo de ADRs simultáneamente — usar la tabla de Service Context para llegar al subconjunto correcto
- `docs/herramientas/` — utilidades de desarrollo, no contexto de coding
- Aggregates docs de servicios fuera del alcance de la tarea actual

---

## Gaps documentales resueltos (Wave 10)

- **Contratos REST consolidados:** Resuelto vía [`arquitectura/contratos-rest.md`](arquitectura/contratos-rest.md) y especificaciones OpenAPI en `docs/arquitectura/contratos/`.
- **Contratos AMQP consolidados:** Resuelto vía [`arquitectura/eventos-amqp.md`](arquitectura/eventos-amqp.md) y esquemas JSON en `docs/arquitectura/contratos/schemas/`.

---

## Navegación

Catálogo de documentos para humanos: [`README.md`](README.md)
Estado y vigencia documental: [`ESTADO_DOCUMENTACION.md`](ESTADO_DOCUMENTACION.md)
