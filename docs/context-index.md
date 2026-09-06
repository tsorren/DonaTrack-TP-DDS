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
| `donaciones-service` | [`arquitectura/aggregates-donaciones.md`](arquitectura/aggregates-donaciones.md) | State Pattern o algoritmos → ADRs en `docs/adr/donaciones-service/`; historial de refactor → `docs/arquitectura/diseno/donaciones/` |
| `logistica-service` | [`arquitectura/aggregates-logistica.md`](arquitectura/aggregates-logistica.md) | Eventos RabbitMQ → ADR `20260703-uso-de-rabbitmq-*` en `docs/adr/logistica-service/`; trazabilidad → [`arquitectura/logging-trazabilidad.md`](arquitectura/logging-trazabilidad.md) |
| `incentivos-service` | [`arquitectura/aggregates-incentivos.md`](arquitectura/aggregates-incentivos.md) | Scheduler / cron → ADRs en `docs/adr/incentivos-service/` |
| `notificaciones-service` | [`arquitectura/aggregates-notificaciones.md`](arquitectura/aggregates-notificaciones.md) | REST / Feign / Persistencia JPA → ADRs en `docs/adr/notificaciones-service/` |
| `common-lib` | [`arquitectura/shared-kernel.md`](arquitectura/shared-kernel.md) | Impacto cross-service → aggregates docs de los servicios afectados |

---

## Task Context (Level 2)

| Tipo de tarea | Cargar |
| --- | --- |
| Diseño arquitectónico / review | [`arquitectura/principios-diseno-arquitectura.md`](arquitectura/principios-diseno-arquitectura.md) |
| Creación de ADR | [`adr/README.md`](adr/README.md) + [`adr/DEUDA_TECNICA.md`](adr/DEUDA_TECNICA.md) + ADRs previos del mismo servicio |
| Testing / integración | [`testing/integration-tests.md`](testing/integration-tests.md) |
| CI/CD / pipeline / Docker | [`cicd/DonaTrack-CICD.md`](cicd/DonaTrack-CICD.md) |
| Code quality / SonarCloud | [`IA/07-errores-frecuentes-sonarcloud-ia.md`](IA/07-errores-frecuentes-sonarcloud-ia.md) |
| Revisión adversarial (Fase 6) | [`auditoria/plan-revisor-critico.md`](auditoria/plan-revisor-critico.md) |
| Trazabilidad / RabbitMQ / Feign | [`arquitectura/logging-trazabilidad.md`](arquitectura/logging-trazabilidad.md) |
| Contratos REST / OpenAPI | [`arquitectura/contratos-rest.md`](arquitectura/contratos-rest.md) + `docs/arquitectura/contratos/` |
| Eventos AMQP / RabbitMQ | [`arquitectura/eventos-amqp.md`](arquitectura/eventos-amqp.md) + `docs/arquitectura/contratos/schemas/` |
| Compliance académico | `docs/entregas/<N>/Enunciado-<N>.pdf` — solo el enunciado de la entrega vigente |

---

## Temporal Constraints (Phase 1 — activas por servicio)

Revisar esta sección si la tarea involucra: persistencia, repositorios, diseño de dominio, SQL, `@Entity`, o Entrega 2.

> ⚠️ Estas constraints son **scoped por servicio**. Una decisión de integrar JPA en un servicio no invalida la constraint de los demás. Una constraint deja de aplicar cuando existe una decisión canónica (ADR aprobado o implementación integrada) para **ese servicio específico** que la reemplaza explícitamente.
>
> Los *drift signals* no invalidan una constraint: indican que la documentación podría estar desactualizada y debe revisarse.

| Constraint | Scope | Regla vigente | Fuente de autoridad | Drift signal |
| --- | --- | --- | --- | --- |
| Persistencia en memoria | `donaciones`, `logistica`, `incentivos` (Fase 1) | No introducir JPA, Hibernate ni SQL salvo ADR aprobado para ese servicio. En `notificaciones-service`: JPA activo con Flyway V1; persistencia en memoria retenida bajo `@Profile("!postgres")` | [`adr/DEUDA_TECNICA.md`](adr/DEUDA_TECNICA.md) DTI-01 a DTI-06 | `spring-boot-starter-data-jpa` activo en `pom.xml` del servicio — revisar si la constraint fue reemplazada para ese servicio en particular |
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
