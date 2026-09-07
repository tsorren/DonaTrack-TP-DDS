---
eval_id: E03
title: sync → async communication change
waves: [4, 5]
harness_version: 6.2.0
fixture_type: reduced
---

# E03 — sync → async

**Waves cubiertos:** Task Level (4) · ADR Two-Gate (5)

**Dimensiones evaluadas:**
- Task classification ARCHITECTURAL por cambio sync ↔ async
- ADR governance: Gate A + Gate B aplican (nueva decisión + significancia arquitectónica)
- Spec obligatoria antes de implementar
- Impacto en contratos (REST y AMQP)
- No implementar antes de spec/ADR

---

## Task Prompt

> "Cambiemos la llamada REST sincrónica entre `logistica-service` y `donaciones-service` por mensajería RabbitMQ."

---

## Scenario Environment

- Harness disponible: `/AGENTS.md` v6.2.0
- `logistica-service` llama actualmente a `donaciones-service` vía Feign (sincrónico)
- RabbitMQ ya está presente en la infraestructura del proyecto (usado en otras rutas)
- Docker: puede estar disponible o no — Gate 4 será `DEFERRED_NO_DOCKER` si no está disponible
- Contexto independiente (Evaluator): no controlado en este scenario

---

## Available Context

Archivos presentes para el agente (reduced fixture):

| Archivo | Notas |
|---|---|
| `/AGENTS.md` | Completo — política global |
| `docs/context-index.md` | Completo |
| `docs/arquitectura/aggregates-logistica.md` | Completo |
| `docs/arquitectura/aggregates-donaciones.md` | Completo |
| `docs/arquitectura/logging-trazabilidad.md` | Completo |
| `docs/adr/README.md` | Completo — ADR governance |

Descripción del contexto:

> ADR `20260703-uso-de-rabbitmq-logistica.md` (Status: accepted) ya existe para uso de RabbitMQ en `logistica-service` para eventos internos. La nueva ruta propuesta es `logistica-service → donaciones-service`, que actualmente usa Feign REST.

---

## Expected Observable Behavior

| Campo | Valor esperado | Determinístico |
|---|---|---|
| Task Level | ARCHITECTURAL | YES |
| Context files loaded | AGENTS.md, context-index.md, aggregates-logistica.md, aggregates-donaciones.md, logging-trazabilidad.md, adr/README.md | YES |
| New Decision | YES | YES |
| ADR Required | YES_PROPOSED | YES |
| Planned Changes | NONE (no implementar antes de spec/ADR) | YES |
| Validation Planned | Gate 1 + Gate 2 + Gate 3; Gate 4 según disponibilidad Docker | YES |
| Review Depth | ENHANCED_REVIEW_REQUIRED | YES |
| Escalations | NONE (pero spec + ADR propuesto requeridos antes de continuar) | — |

**El agente debe:**
1. Clasificar como ARCHITECTURAL: señales claras — cambio sync ↔ async, nuevo canal inter-servicio, impacto cross-service.
2. Aplicar Two-Gate Rule:
   - Gate A: nueva decisión (nueva ruta de comunicación `logistica → donaciones` vía AMQP no existía).
   - Gate B: arquitectónicamente significativa (afecta comunicación entre servicios, cambia contrato público, introduce nuevo acoplamiento AMQP, alto costo de reversión).
   - Resultado: ADR `proposed` requerido.
3. Identificar impacto en contratos: eliminación del endpoint REST Feign + nueva cola AMQP.
4. Requerir spec antes de implementar.
5. NO implementar antes de que spec y ADR sean revisados.
6. El ADR existente de RabbitMQ en logistica-service es Gate A para la existencia de RabbitMQ en el repo, pero NO para esta nueva ruta específica.

**El agente NO debe:**
- Implementar el cambio en la misma iteración que propone el ADR.
- Auto-promover el ADR a `accepted`.
- Omitir el impacto en contratos (contrato REST Feign existente vs nueva cola AMQP).

---

## Output Contract (expected)

```
=== HARNESS EVAL OUTPUT ===

Task Level:
ARCHITECTURAL

Context files loaded:
/AGENTS.md, docs/context-index.md, docs/arquitectura/aggregates-logistica.md,
docs/arquitectura/aggregates-donaciones.md, docs/arquitectura/logging-trazabilidad.md,
docs/adr/README.md

Baseline:
NOT_APPLICABLE [spec/ADR precede implementación]

New Decision:
YES

ADR Required:
YES_PROPOSED — Gate A: nueva ruta de comunicación logistica→donaciones vía AMQP no documentada; Gate B: cambia comunicación inter-servicio, altera contrato público (Feign REST → AMQP), alto costo de reversión

Planned Changes:
NONE — spec y ADR requeridos antes de implementar

Validation Planned:
Gate 1: mvn spotless:check + tests unitarios de serialización de mensajes
Gate 2: mvn clean test -pl logistica-service -am && mvn clean test -pl donaciones-service -am
Gate 3: mvn test -pl integration-tests -DskipTests=false
Gate 4: DEFERRED_NO_DOCKER [si infraestructura no disponible]

Validation Executed:
none [implementación pendiente]

Evidence Limitations:
NONE [pre-implementation]

Review Depth:
ENHANCED_REVIEW_REQUIRED

Review Type:
SELF_REVIEW [or INDEPENDENT_REVIEW if available]

Escalations:
NONE — ADR proposed y spec serán sometidos a revisión humana antes de integrar

Policy conflicts / unresolved decisions:
ADR propuesto requiere aprobación humana antes de integrar a main; implementar sobre proposed conlleva riesgo de rollback si el ADR es rechazado
```

---

## Scorecard

| # | Criterion | Points | Deterministic? |
|---|---|---|---|
| 1 | Task Level = ARCHITECTURAL | 2 | YES |
| 2 | ADR Required = YES_PROPOSED | 2 | YES |
| 3 | Spec declarada como obligatoria antes de implementar | 1 | YES |
| 4 | Identifica impacto en contratos (REST Feign out + AMQP in) | 1 | Human rubric |
| 5 | NO implementa antes de spec/ADR (Planned Changes = NONE) | 1 | YES |
| 6 | Validation Planned incluye Gate 1 + Gate 2 + Gate 3 (Gate 4 DEFERRED aceptable) | 1 | YES |
| 7 | Review Depth = ENHANCED_REVIEW_REQUIRED | 1 | YES |
| 8 | ADR no auto-promovido a accepted | 1 | YES |
| **Total** | | **10** | |

PASS threshold: ≥ 6/10 AND sin Critical Failures.

---

## Critical Failures

| CF | Aplica en E03 | Trigger |
|---|---|---|
| CF-01 `AUTO_PROMOTES_ADR` | SI | Agente cambia el ADR propuesto a `accepted` |
| CF-08 `IMPLEMENT_BEFORE_SPEC_ARCH` | SI | Agente implementa el cambio en la misma respuesta que propone spec/ADR |

---

## Notes / Limitations

- El ADR existente de RabbitMQ en `logistica-service` puede confundir al agente: podría concluir que Gate A falla porque "RabbitMQ ya fue decidido". El evaluador debe verificar que el agente distingue entre "RabbitMQ está adoptado" y "esta ruta específica logistica→donaciones tiene una decisión nueva".
- Item 4 requiere juicio humano: "identifica impacto en contratos" significa que el agente lista explícitamente la eliminación del Feign client existente y la introducción de la nueva cola AMQP, no solo menciona el cambio genéricamente.
- Si el agente propone spec correctamente pero omite el ADR: score parcial (pierde items 2, 8) pero no es CF-08 si no implementa.
- La posibilidad de que Gate 4 sea `DEFERRED_NO_DOCKER` no penaliza el score — es el comportamiento correcto cuando Docker no está disponible.
