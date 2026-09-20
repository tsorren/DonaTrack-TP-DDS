---
name: define-spec
description: >-
  Elicitación de requerimientos funcionales y definición de especificaciones (SDD)
  mediante consulta activa al usuario, cero inferencias no validadas y análisis
  comparativo de alternativas viables en 5 dimensiones.
---

# Skill: define-spec — Elicitación de Requerimientos y Formulación de Specs

> **Ámbito:** Bounded Contexts y flujos funcionales de DonaTrack.  
> **Alineación Normativa:** [`AGENTS.md`](../../../AGENTS.md), [`docs/context-index.md`](../../../docs/context-index.md) y filosofía SDD de [`docs/agentes/informe-harness-engineering-pi-gentle-pi.md`](../../../docs/agentes/informe-harness-engineering-pi-gentle-pi.md).

---

## 1. Propósito y Principios Operativos

Esta skill transforma requerimientos vagos, incompletos o ambiguos en una **especificación funcional rigurosa y verificable**, interactuando con el usuario para resolver disyuntivas sin realizar suposiciones desautorizadas.

### Principios Mandatorios:
1. **Tolerancia Cero a Inferencias Especulativas:**
   * Queda terminantemente prohibido inferir o inventar requerimientos de cátedra ([`docs/entregas/`](../../../docs/entregas/)), límites de bounded contexts, contratos públicos (REST DTOs, AMQP, Feign) o nuevas dependencias en `pom.xml`.
   * Si la información no está explícitamente respaldada en la documentación canónica o en la solicitud del usuario, **se debe consultar al usuario**.
   * Inferencias operativas internas (nombres de métodos auxiliares o detalles locales que sigan convenciones ya existentes) están permitidas únicamente si se documentan de forma transparente con la etiqueta `[INFERRED]` de `AGENTS.md` §3.
2. **Generación de Alternativas Intrínsecamente Viables:**
   * Ante cualquier decisión de diseño o alcance, formular de 2 a 3 alternativas donde **todas sean técnicamente viables** dentro del stack de DonaTrack (Java 21, Spring Boot 3, persistencia ConcurrentHashMap/JPA, RabbitMQ).
   * Prohibido presentar opciones inviables o "muñecos de paja" para forzar una elección.
3. **Análisis de Trade-offs en 5 Dimensiones:**
   * Contrastar rigurosamente cada alternativa en la matriz 5D antes de solicitar la decisión del usuario.
4. **Recomendación Técnica Obligatoria:**
   * El agente no debe limitarse a preguntar "¿qué prefieres?"; debe emitir una recomendación fundamentada explicando el porqué.

---

## 2. Flujo de Ejecución Paso a Paso

```text
[Solicitud / Issue]
       │
       ▼
[Paso 1: Relevamiento de Contexto] ──► Consulta docs/context-index.md
       │
       ▼
[Paso 2: Detección de Ambigüedades] ──► ¿Faltan datos de negocio o alcance?
       │
       ├── SÍ ──► [Paso 3: Matriz 5D & Elicitación Interactiva]
       │                 │
       │                 ▼
       └── NO ──► [Paso 4: Redacción de Spec en docs/specs/active/]
```

### Paso 1: Relevamiento de Contexto (Progressive Disclosure)
* Cargar exclusivamente el contexto necesario desde [`docs/context-index.md`](../../../docs/context-index.md) según el módulo o servicio involucrado.
* Validar si existen ADRs aceptados que ya resuelvan la problemática en [`docs/adr/`](../../../docs/adr/). Si ya existe un ADR aceptado, la decisión ya fue tomada y debe respetarse.

### Paso 2: Análisis de Ambigüedad y Alcance
* Clasificar el impacto preliminar según `AGENTS.md` §7.0:
  * Si la tarea es `QUICK`: esta skill no debe ejecutarse (derivar a `implement-task` inline).
  * Si es `STANDARD` o `ARCHITECTURAL`: identificar qué aspectos funcionales o de frontera están indefinidos.

### Paso 3: Matriz 5D y Consulta Interactiva al Usuario
Para cada bifurcación real de decisión, tabular las alternativas bajo las **5 Dimensiones de Trade-off**:

| Dimensión | Criterio de Evaluación |
|---|---|
| **1. Complejidad / YAGNI** | Impacto en líneas de código, capas, complejidad accidental y facilidad de mantenimiento. |
| **2. Cátedra / ADRs** | Conformidad con los requerimientos de entrega UTN-FRBA y decisiones vigentes en `docs/adr/`. |
| **3. Acoplamiento** | Respeto a los Bounded Contexts y pureza del Shared Kernel (`common-lib`). |
| **4. Performance / Flujo** | Implicancias de procesamiento sincrónico (REST) vs asincrónico (RabbitMQ / `@Async`). |
| **5. Reversibilidad** | Costo de revertir o migrar la solución si los requerimientos cambian a futuro. |

#### Protocolo de Interacción (Capability Detection Pattern):
* **Si la herramienta interactiva `ask_question` está disponible:**  
  Invocar `ask_question` estructurando las alternativas viables como opciones de respuesta, marcando la opción recomendada con el prefijo `(Recommended)` y resumiendo la tabla 5D.
* **Si `ask_question` no está disponible (modo CLI o herramientas externas):**  
  Emitir un bloque interactivo en Markdown con opciones claras `[A]`, `[B]`, `[C]`, la tabla comparativa 5D, la recomendación explícita y pausar la ejecución esperando la respuesta del usuario.

### Paso 4: Publicación del Artefacto de Especificación
Una vez acordada la dirección con el usuario, persistir la especificación en:  
`docs/specs/active/<task-id>-spec.md`.

---

## 3. Plantilla Canónica del Artefacto Spec (`docs/specs/active/`)

```markdown
# Spec: [Identificador y Título de la Tarea]

> **Estado:** APPROVED_BY_USER  
> **Nivel:** STANDARD | ARCHITECTURAL  
> **Fecha:** AAAA-MM-DD  
> **Módulos Impactados:** [ej. donaciones-service, common-lib]

## 1. Objetivo Funcional (Goal)
[Descripción concisa del problema de negocio que se resuelve, sin tecnicismos superfluos.]

## 2. Alcance Delimitado (Scope Boundaries)
### In-Scope (Lo que SÍ incluye):
* [Elemento 1]
* [Elemento 2]

### Out-of-Scope (Lo que NO incluye - Anti Scope Creep):
* [Exclusión 1]
* [Exclusión 2]

## 3. Decisiones Acordadas y Trade-offs
* **Alternativa Elegida:** [Nombre de la alternativa acordada]
* **Justificación:** [Fundamento técnico y alineación con la matriz 5D]
* **Inferencias Operativas Declaradas:**
  * `[INFERRED]` [Decisión de bajo nivel adoptada siguiendo convenciones existentes]

## 4. Invariantes de Negocio y Reglas de Integridad
* [Invariante 1 que no puede romperse bajo ningún concepto]
* [Invariante 2]

## 5. Criterios de Aceptación (Gherkin / Given-When-Then)
```gherkin
Escenario: [Nombre del flujo principal]
  Dado [estado inicial o precondición]
  Cuando [acción o evento ejecutado]
  Entonces [resultado esperado y verificable]
```

## 6. Siguiente Etapa
* Si el nivel es `ARCHITECTURAL` ➔ Derivar a skill `design-spec`.
* Si el nivel es `STANDARD` ➔ Derivar directamente a skill `implement-task`.
```

---

## 4. Criterio de Salida (Definition of Done)
* [ ] Cero suposiciones no documentadas sobre reglas de cátedra o contratos públicos.
* [ ] Matriz 5D evaluada y presentada al usuario.
* [ ] Elección final confirmada explícitamente por el usuario.
* [ ] Artefacto `docs/specs/active/<task-id>-spec.md` guardado y visible en el repositorio.
