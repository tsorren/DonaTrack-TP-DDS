---
name: design-spec
description: >-
  Diseño arquitectónico y técnico detallado a partir de una especificación aprobada (SDD),
  delimitando clases Java 21, contratos, evaluación de la Two-Gate Rule para ADRs y plan TDD.
---

# Skill: design-spec — Diseño Arquitectónico y Especificación Técnica

> **Ámbito:** Diseño de microservicios, Shared Kernel y persistencia en DonaTrack.  
> **Alineación Normativa:** [`AGENTS.md`](../../../AGENTS.md), [`docs/context-index.md`](../../../docs/context-index.md), [`docs/arquitectura/principios-diseno-arquitectura.md`](../../../docs/arquitectura/principios-diseno-arquitectura.md) y Two-Gate Rule (§9.1).

---

## 1. Propósito y Responsabilidades

Esta skill recibe una especificación funcional aprobada en [`docs/specs/active/`](../../../docs/specs/active/) y produce el **diseño técnico de detalle** antes de escribir código fuente ejecutable, asegurando que:
* No se violen invariantes de bounded contexts ni pureza de capas.
* Se evalúe formalmente si la solución requiere un nuevo Architectural Decision Record (ADR `proposed`).
* Se utilice **`grepai`** de forma primaria para la inspección y trazabilidad estructural del código preexistente.
* Se diseñe una estrategia determinista de pruebas unitarias y de integración.

---

## 2. Flujo de Trabajo Técnico

```text
[Spec Aprobada en docs/specs/active/]
                 │
                 ▼
[Paso 1: Inspección de Código vía grepai] ──► Mapeo de tipos y llamadas existentes
                 │
                 ▼
[Paso 2: Modelado de Componentes y Capas] ──► Controllers, Services, Dominio, DTOs
                 │
                 ▼
[Paso 3: Evaluación de Two-Gate Rule para ADR] ──► Gate A (Novedad) + Gate B (Significancia)
                 │
                 ▼
[Paso 4: Estrategia TDD y Casos de Prueba] ──► Fixtures sintéticos y aserciones
                 │
                 ▼
[Paso 5: Emisión de Sección de Diseño Técnico] ──► Listo para design-review
```

### Paso 1: Exploración Estructural con Mandato GrepAI
* Usar de forma primaria y obligatoria las herramientas de `grepai` (`grepai_search`, `grepai_refs_*`, `grepai_trace_*`) para rastrear:
  * Entidades y Aggregate Roots preexistentes.
  * Clientes OpenFeign y contratos DTO compartidos.
  * Puntos de publicación o consumo en RabbitMQ.

### Paso 2: Modelado Técnico y Restricciones de Capa
* **Controllers como Adaptadores Puros (`AGENTS.md` §4.2):** Solo validan DTOs de frontera y delegan en Application Services. No contienen orquestación compleja ni lógica de negocio.
* **Pureza del Dominio:** Las entidades de dominio no deben acoplarse a DTOs de transporte ni a clientes HTTP.
* **Shared Kernel (`common-lib`):** Solo puede contener código neutral y reutilizable. Queda prohibido introducir entidades de negocio o dependencias circulares.

### Paso 3: Gobernanza de ADRs — Two-Gate Rule (`AGENTS.md` §9.1)
Evaluar rigurosamente las dos compuertas antes de proponer un nuevo ADR:
* **Gate A — Decisión Nueva:** ¿Introduce un patrón o tecnología ausente? (Si es una extensión o implementación de un patrón existente, Gate A es NO).
* **Gate B — Significancia Arquitectónica:** ¿Afecta contratos cross-service, modelo de persistencia o límites de módulos?
* **Regla:** Solo si `Gate A = SÍ` Y `Gate B = SÍ`, redactar un ADR `proposed` en `docs/adr/`.
* **`[INVARIANT]` Prohibición de Auto-Promoción:** Queda terminantemente prohibido auto-promover un ADR a `accepted`. Solo el revisor humano puede aprobarlo.

### Paso 4: Estrategia de Pruebas TDD
* Delimitar qué tests unitarios se crearán bajo la metodología TDD (RED ➔ GREEN ➔ REFACTOR).
* Definir datos sintéticos libres de información sensible o credenciales (`AGENTS.md` §4.3).

---

## 3. Plantilla de la Sección de Diseño Técnico

Incorporar esta sección al final de `docs/specs/active/<task-id>-spec.md`:

```markdown
---
## 7. Especificación Técnica de Diseño (Technical Design)

### 7.1. Componentes Afectados
* **Módulos:** [ej. logistica-service, common-lib]
* **Clases a Modificar / Crear:**
  * `[NUEVA | MODIFICADA]` `paquete.Clase` — Responsabilidad acotada.

### 7.2. Contratos y DTOs
* **Endpoints / Métodos:**
  * `POST /api/v1/...`
  * Request DTO: `Campos obligatorios y tipos`
  * Response DTO: `Estructura inmutable`
* **Compatibilidad:** Cambios estrictamente aditivos (retrocompatibles).

### 7.3. Evaluación Two-Gate ADR
* **Gate A (Novedad):** [SÍ / NO] — [Justificación]
* **Gate B (Significancia):** [SÍ / NO] — [Justificación]
* **Dictamen:** [No requiere ADR | Requiere ADR-XXX proposed]

### 7.4. Estrategia de Verificación y Plan TDD
* **Baseline Requerido:** `mvn test -pl <modulo> -Dtest=<TestExistente>`
* **Nuevos Tests a Escribir:**
  1. `debeRetornarEstadoValidoCuandoDatosCorrectos()`
  2. `debeLanzarValidationExceptionCuandoParametroInvalido()`
* **Quality Gates Locales:** `mvn spotless:check`, compilación y test unitario.
```

---

## 4. Criterio de Salida (Definition of Done)
* [ ] Diseño técnico anclado a clases y paquetes concretos.
* [ ] Controllers estrictamente delimitados como adaptadores HTTP.
* [ ] Evaluación de la Two-Gate Rule documentada.
* [ ] Plan de tests TDD con datos sintéticos.
* [ ] Artefacto preparado para evaluación adversarial por la skill `design-review`.
