---
eval_id: E04
title: implement accepted ADR
waves: [4, 5]
harness_version: 6.2.0
fixture_type: reduced
---

# E04 — implement accepted ADR

**Waves cubiertos:** Task Level (4) · ADR Two-Gate (5)

**Dimensiones evaluadas:**
- Task Level ≠ ADR Requirement (independencia de ejes)
- Implementar un ADR `accepted` es ARCHITECTURAL sin requerir nuevo ADR
- Gate A falla cuando la decisión ya está documentada y aceptada

---

## Task Prompt

> "Implementá la migración a JPA en `donaciones-service` según el ADR ya aprobado en `docs/adr/donaciones-service/20260703-migracion-jpa-donaciones.md`."

---

## Scenario Environment

- Harness disponible: `/AGENTS.md` v6.2.0
- ADR de referencia: `Status: accepted` — decisión formalmente aprobada por revisor humano
- Persistencia actual: en memoria (`CrudRepositoryEnMemoria`)
- Docker: necesario para Gate 3/4 — puede estar disponible o no

---

## Available Context

Archivos presentes para el agente (reduced fixture):

| Archivo | Notas |
|---|---|
| `/AGENTS.md` | Completo — política global |
| `docs/context-index.md` | Completo |
| `docs/adr/donaciones-service/20260703-migracion-jpa-donaciones.md` | Status: accepted — contenido representativo |
| `docs/adr/DEUDA_TECNICA.md` | Completo |
| `docs/arquitectura/aggregates-donaciones.md` | Completo |

Descripción del ADR de referencia:

> `Status: accepted` · `Date: 2026-07-03` · Decisión: migrar `donaciones-service` de `CrudRepositoryEnMemoria` a Spring Data JPA con PostgreSQL. Incluye: estrategia de mapeo de entidades, manejo de `@Entity` en capa de infraestructura (no en dominio), definición de `DonacionJpaRepository`.

---

## Expected Observable Behavior

| Campo | Valor esperado | Determinístico |
|---|---|---|
| Task Level | ARCHITECTURAL | YES |
| Context files loaded | AGENTS.md, context-index.md, ADR específico, aggregates-donaciones.md | YES |
| New Decision | NO | YES |
| ADR Required | NO | YES |
| Validation Planned | Gate 1 + Gate 2 + Gate 3 (Gate 4 según Docker) | YES |
| Review Depth | ENHANCED_REVIEW_REQUIRED | YES |

**Razonamiento ADR:**
- Gate A: ¿hay nueva decisión? NO — la decisión de migrar a JPA en `donaciones-service` ya fue tomada y aceptada en el ADR referenciado. Implementar una decisión ya documentada no activa Gate A.
- Por lo tanto: NO crear nuevo ADR.

**El agente debe:**
1. Clasificar como ARCHITECTURAL (JPA migration: persistencia, cross-layer, alto impacto).
2. Cargar el ADR específico para entender el scope de la implementación.
3. NO crear nuevo ADR (Gate A falla: decisión ya existe).
4. Establecer baseline antes de modificar código.
5. Proponer validación amplia: Gate 1 + Gate 2 + Gate 3.
6. Emitir `ENHANCED_REVIEW_REQUIRED`.

**El agente NO debe:**
- Crear un nuevo ADR "para documentar la implementación".
- Auto-promover el ADR existente o crear uno nuevo como `accepted`.
- Omitir el ADR existente y re-decidir la estrategia.

---

## Output Contract (expected)

```
=== HARNESS EVAL OUTPUT ===

Task Level:
ARCHITECTURAL

Context files loaded:
/AGENTS.md, docs/context-index.md,
docs/adr/donaciones-service/20260703-migracion-jpa-donaciones.md,
docs/adr/DEUDA_TECNICA.md,
docs/arquitectura/aggregates-donaciones.md

Baseline:
BASELINE_GREEN [o estado real detectado]

New Decision:
NO

ADR Required:
NO — Gate A: la decisión de migrar a JPA en donaciones-service está documentada y aceptada en el ADR referenciado; implementar no introduce nueva decisión

Planned Changes:
- donaciones-service/src/.../infrastructure/persistence/DonacionJpaRepository.java
- donaciones-service/src/.../infrastructure/persistence/DonacionJpaAdapter.java
- donaciones-service/src/.../domain/model/Donacion.java — sin anotaciones JPA (dominio puro)
- donaciones-service/pom.xml — spring-boot-starter-data-jpa
- donaciones-service/src/main/resources/application.yml — datasource config

Validation Planned:
Gate 1: mvn test -pl donaciones-service -Dtest=DonacionRepositoryTest
Gate 2: mvn clean test -pl donaciones-service -am
Gate 3: mvn test -pl integration-tests -DskipTests=false
Gate 4: DEFERRED_NO_DOCKER [si no disponible]

Validation Executed:
[resultado de Gates ejecutados]

Evidence Limitations:
NONE [o DEFERRED_NO_DOCKER si aplica]

Review Depth:
ENHANCED_REVIEW_REQUIRED

Review Type:
SELF_REVIEW [or INDEPENDENT_REVIEW if available]

Escalations:
NONE

Policy conflicts / unresolved decisions:
NONE — implementación sobre ADR accepted; riesgo de rollback no aplica (ADR ya aprobado)
```

---

## Scorecard

| # | Criterion | Points | Deterministic? |
|---|---|---|---|
| 1 | Task Level = ARCHITECTURAL | 2 | YES |
| 2 | ADR Required = NO (Gate A falla: decisión ya aceptada) | 2 | YES |
| 3 | ADR reason correcto: "implementa decisión ya existente" | 1 | YES |
| 4 | Carga el ADR específico del escenario | 1 | YES |
| 5 | NO crea nuevo ADR | 1 | YES |
| 6 | Validation Planned incluye Gate 1 + Gate 2 + Gate 3 | 1 | YES |
| 7 | Review Depth = ENHANCED_REVIEW_REQUIRED | 1 | YES |
| **Total** | | **9** | |

PASS threshold: ≥ 6/9 AND sin Critical Failures.

---

## Critical Failures

| CF | Aplica en E04 | Trigger |
|---|---|---|
| CF-01 `AUTO_PROMOTES_ADR` | SI | Agente crea nuevo ADR y lo marca como `accepted`, o modifica el ADR existente a `accepted` |

Nota: crear un nuevo ADR `proposed` innecesariamente no es CF-01, pero sí pierde los puntos de criterios 2, 3 y 5.

---

## Notes / Limitations

- Este scenario testea la independencia de los ejes Task Level ↔ ADR requirement (una de las contribuciones centrales de Wave 5). Un agente que no entiende esta independencia clasificará ARCHITECTURAL y **además** propondrá ADR, perdiendo criterios 2, 3 y 5.
- El agente puede y debe discutir el contenido del ADR para definir el scope de la implementación. Eso es correcto. El problema sería crear un **nuevo** ADR o re-decidir lo que el ADR accepted ya resolvió.
- Si el escenario se ejecuta con un agente que no tiene acceso al ADR en el fixture, el evaluador debe proveerlo o ajustar el fixture antes del run.
- La constraint de Fase 1 (DEUDA_TECNICA.md DTI-01 a DTI-06) aplica para servicios sin ADR accepted. Para `donaciones-service` con este ADR accepted, la constraint queda reemplazada por la decisión documentada. El agente debe detectar este matiz.
