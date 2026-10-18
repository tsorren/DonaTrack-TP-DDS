# Spec: SPEC-01 — Consolidación de Nivel 4 (Agent-First) en DonaTrack

> **Estado:** COMPLETED  
> **Nivel:** ARCHITECTURAL  
> **Fecha:** 2026-09-11  
> **Módulos Impactados:** `scripts/`, `docs/specs/`, `docs/IA/evals/`, `docs/generated/`, `docs/auditoria/`

---

## 1. Objetivo Funcional (Goal)

Consolidar operativa y mecánicamente el **Nivel 4 (Agent-First)** de madurez de repositorio en DonaTrack según los estándares de ingeniería de agentes y la auditoría formal (`docs/auditoria/auditoria-directivas-agentes.md`). Esto requiere cerrar de manera demostrable y determinista las tres brechas de gobierno:
1. **Linter Mecánico de Especificaciones (SDD as Code):** Garantizar mediante machine guard que toda especificación activa o completada cumpla estrictamente con la estructura canónica, evitando la deriva documental.
2. **Runner Automatizado de Evals (Harness Executable Pipeline):** Disponer de un arnés de ejecución headless y determinista para los escenarios `E01` a `E11`, con cálculo de scorecards y detección infalible de fallas críticas (CF-01 a CF-12).
3. **Generador de Conocimiento del Repositorio (Generated Knowledge):** Extraer mecánicamente la topología de contratos OpenAPI, esquemas JSON y eventos RabbitMQ en documentación estructurada en `docs/generated/` sin intervención manual.

---

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (Lo que SÍ incluye):
* **Épica 1:** Creación de `scripts/agent-check/checks/specs.js`, integración en `scripts/agent-check/index.js`, y tests unitarios en `scripts/tests/run-tests.js`.
* **Épica 2:** Creación de `scripts/run-evals.js` capaz de evaluar sintaxis, estructura, scorecards y fallas críticas de escenarios E01–E11 en modo determinista y generar scorecards reproducibles en Markdown y consola.
* **Épica 3:** Creación de `scripts/generate-repo-knowledge.js` que inspecciona contratos OpenAPI y schemas en `docs/arquitectura/contratos/` para generar `docs/generated/README.md`, `endpoints-catalog.md`, `events-catalog.md`, `architecture-graph.md` y `contracts-summary.json`.
* **Sincronización de Gobernanza:** Actualización de `docs/auditoria/auditoria-directivas-agentes.md` (promoviendo a 5.0 / 5.0 Nivel 4 Pleno), `docs/README.md`, y `docs/ESTADO_DOCUMENTACION.md`.
* **Ciclo SDD Completo:** Transición de `SPEC-01` de `active/` a `completed/` tras las evaluaciones de diseño e implementación.

### Out-of-Scope (Lo que NO incluye - Anti-Scope Creep):
* Modificación de lógica de negocio o controladores en `*/src/main/java/` o `*/src/test/java/` de los microservicios Java (`donaciones-service`, `logistica-service`, `incentivos-service`, `notificaciones-service`, `common-lib`).
* Alteración de infraestructura Docker (`docker-compose.yml`, Dockerfiles, n8n workflows).
* Adición de dependencias npm externas en `package.json` (todos los scripts deben utilizar exclusivamente APIs nativas de Node.js 20+).
* Auto-promoción de ningún ADR a `accepted` o `rejected`.

---

## 3. Decisiones Acordadas y Trade-offs

Para las decisiones arquitectónicas del tooling de Nivel 4, se evaluaron las siguientes alternativas bajo la Matriz de Trade-offs en 5 Dimensiones (5D):

| Dimensión | Opción 1: Node.js Nativo Puro (Elegida) | Opción 2: Dependencias npm Externas (Rechazada) | Opción 3: Scripts Bash/Python Mixtos (Rechazada) |
|---|---|---|---|
| **1. Complejidad / YAGNI** | **Baja:** Cero dependencias npm a instalar; ejecución instantánea con `node`. | **Alta:** Requiere `package.json` con dependencias pesadas (`markdown-it`, `ajv`, `commander`). | **Media:** Heterogeneidad en pipelines y plataformas Windows/Linux. |
| **2. Cátedra / ADRs** | **Conforme:** Alineado con la política de tooling nativo de `agent-check.js` y `validate-contracts.js`. | **Riesgo:** Introduce supply chain risks y lentitud en CI. | **Riesgo:** Requiere bash en Windows o dependencias de Python extras. |
| **3. Acoplamiento** | **Nulo:** Tooling desacoplado del runtime de microservicios Java. | **Nulo:** Desacoplado de Java pero acoplado a versiones npm. | **Nulo:** Desacoplado pero frágil entre OS. |
| **4. Performance** | **Óptima:** Tiempos de ejecución sub-segundo (< 200 ms). | **Media:** Overhead de carga de node_modules. | **Variable:** Dependiente de invocaciones entre intérpretes. |
| **5. Reversibilidad** | **Alta:** Scripts autocontenidos y fácilmente mantenibles o reemplazables. | **Baja:** Migraciones de dependencias y versiones de librerías. | **Media:** Mantenimiento duplicado. |

* **Alternativa Elegida:** Opción 1 — Tooling nativo puro en Node.js 20.
* **Justificación:** Mantiene la velocidad extrema de CI (<1s), uniformidad con `agent-check.js` y `validate-contracts.js`, compatibilidad total con Windows y Linux sin npm install previo.
* **Inferencias Operativas Declaradas:**
  * `[INFERRED]` Se asume que los escenarios en `docs/IA/evals/scenarios/` siguen el patrón de nombres `E<01-11>-*.md` con frontmatter YAML delimitado por `---`.
  * `[INFERRED]` Se asume que el directorio `docs/generated/` debe incluir un disclaimer formal advirtiendo que los artefactos son generados mecánicamente y no deben editarse manualmente.

---

## 4. Invariantes de Negocio y Reglas de Integridad

1. **`[INVARIANT]` Pureza de Código de Dominio:** Ninguna clase Java en ningún microservicio es modificada en esta tarea (`git status` limpio en `*/src/`).
2. **`[INVARIANT]` Zero Broken Links:** Todo archivo generado en `docs/generated/` o modificado en `docs/` debe pasar con 0 links rotos en `python ./scripts/validate_docs_links.py`.
3. **`[INVARIANT]` Integridad Determinista de Machine Guards:** `node ./scripts/agent-check.js` debe arrojar `exit code 0` con todas las comprobaciones de gobernanza en verde.
4. **`[INVARIANT]` Cero Falsos Verificados:** Toda aserción de `run-evals.js` y `agent-check.js` debe ser factual y comprobable.

---

## 5. Criterios de Aceptación (Gherkin / Given-When-Then)

```gherkin
Escenario: Validación de especificaciones SDD canónicas
  Dado el directorio docs/specs/active/ y docs/specs/completed/
  Cuando se ejecuta "node scripts/agent-check.js"
  Entonces el check SPECS_INTEGRITY finaliza en [PASS]
  Y verifica que toda spec contenga Objetivo, Alcance In/Out-of-Scope, Trade-offs 5D y Criterios Gherkin.

Escenario: Detección de spec incompleta o mal formateada
  Dada una especificación temporal en docs/specs/active/ sin sección de Trade-offs
  Cuando se evalúa mediante "checkSpecsIntegrity"
  Entonces se emite un finding FAIL con ID "SPEC_MISSING_SECTION" identificando el archivo infractor.

Escenario: Ejecución determinista del arnés de evals
  Dado el conjunto de 11 escenarios E01 a E11 en docs/IA/evals/scenarios/
  Cuando se ejecuta "node scripts/run-evals.js"
  Entonces evalúa los 11 escenarios contra sus scorecards deterministas
  Y valida la detección de fallas críticas CF-01 a CF-12 sin incurrir en falsos positivos
  Y genera el scorecard Markdown consolidado con exit code 0.

Escenario: Generación automática de conocimiento del repositorio
  Dado el catálogo de contratos OpenAPI y schemas en docs/arquitectura/contratos/
  Cuando se ejecuta "node scripts/generate-repo-knowledge.js"
  Entonces se crean o actualizan los 5 archivos en docs/generated/
  Y ningún enlace relativo en docs/generated/ resulta roto según validate_docs_links.py.

Escenario: Certificación de Nivel 4 en la Auditoría
  Dado el cierre exitoso de las tres brechas en scripts y documentación
  Cuando se audita docs/auditoria/auditoria-directivas-agentes.md
  Entonces el scorecard refleja 5.0 / 5.0 con estado "NIVEL 4 (AGENT-FIRST) PLENO Y CONSOLIDADO".
```

---

## 6. Siguiente Etapa

Al ser una tarea de nivel `ARCHITECTURAL` (introduce nuevos linters de CI, pipeline de evaluación de evals y subsistema de documentación generada), se deriva formalmente a la especificación técnica de detalle en la Sección 7.

---

## 7. Especificación Técnica de Diseño (Technical Design)

### 7.1. Componentes Afectados

* **Módulos / Directorios:** `scripts/agent-check/`, `scripts/`, `docs/generated/`, `docs/auditoria/`, `docs/specs/`.
* **Archivos a Crear / Modificar:**
  * `[NUEVO]` `scripts/agent-check/checks/specs.js`: Linter determinista de specs SDD.
  * `[MODIFICADO]` `scripts/agent-check/index.js`: Incorporación de `checkSpecsIntegrity` en el agregador `runAllChecks` y en el public API.
  * `[MODIFICADO]` `scripts/tests/run-tests.js`: Tests unitarios para `checkSpecsIntegrity` y corrección del mock de fixture para `runAllChecks`.
  * `[NUEVO]` `scripts/run-evals.js`: Runner determinista de escenarios de evals E01–E11 y generación de scorecards.
  * `[NUEVO]` `scripts/generate-repo-knowledge.js`: Extractor de contratos, endpoints y eventos RabbitMQ en `docs/generated/`.
  * `[NUEVO]` `docs/generated/README.md`: Índice y metadatos de conocimiento generado.
  * `[NUEVO]` `docs/generated/endpoints-catalog.md`: Catálogo unificado de endpoints REST de los 4 servicios.
  * `[NUEVO]` `docs/generated/events-catalog.md`: Catálogo de eventos asíncronos y schemas RabbitMQ.
  * `[NUEVO]` `docs/generated/architecture-graph.md`: Diagramas de arquitectura y flujo de mensajería.
  * `[NUEVO]` `docs/generated/contracts-summary.json`: Resumen JSON estructurado para herramientas.
  * `[MODIFICADO]` `docs/auditoria/auditoria-directivas-agentes.md`: Certificación Nivel 4 (5.0 / 5.0).
  * `[MODIFICADO]` `docs/README.md`: Registro de las nuevas herramientas y directorio `docs/generated/`.
  * `[MODIFICADO]` `docs/ESTADO_DOCUMENTACION.md`: Registro de sincronización documental.

### 7.2. Contratos y DTOs / Interfaces de Script

1. **`checkSpecsIntegrity(repoRoot)` (`scripts/agent-check/checks/specs.js`):**
   - Retorna: `Array<Finding>` (`{ id, severity: 'PASS' | 'WARN' | 'FAIL', message, file? }`).
   - Reglas:
     - Si `docs/specs/` no existe ➔ `FAIL: SPECS_DIR_MISSING`.
     - Si no hay specs activas ni completadas (solo `.gitkeep`) ➔ `PASS: NO_ACTIVE_SPECS`.
     - Para cada archivo `.md` en `docs/specs/active/` o `docs/specs/completed/`:
       - Título con prefijo `# Spec:` ➔ `SPECS_CANONICAL_TITLE`.
       - Metadato `> **Estado:**` o `> **Status:**` ➔ `SPECS_METADATA_STATUS`.
       - Metadato `> **Nivel:**` (`STANDARD` o `ARCHITECTURAL`) ➔ `SPECS_METADATA_LEVEL`.
       - Sección `## 1. Objetivo Funcional` ➔ `SPECS_SECTION_GOAL`.
       - Sección `## 2. Alcance Delimitado` con `### In-Scope` y `### Out-of-Scope` ➔ `SPECS_SECTION_SCOPE`.
       - Sección `## 3. Decisiones Acordadas y Trade-offs` con mención a `5D` o `Trade-off` ➔ `SPECS_SECTION_TRADEOFFS`.
       - Sección `## 4. Invariantes` ➔ `SPECS_SECTION_INVARIANTS`.
       - Sección `## 5. Criterios de Aceptación` con bloque Gherkin ➔ `SPECS_SECTION_GHERKIN`.
       - Si Nivel es `ARCHITECTURAL`: debe incluir `## 7. Especificación Técnica de Diseño` con `Two-Gate` y plan `TDD`.
       - Si está en `docs/specs/completed/`: debe contener bloque `=== REVIEW CONTRACT ===` o `=== DESIGN REVIEW CONTRACT ===`.

2. **`scripts/run-evals.js`:**
   - Modo CLI: `node scripts/run-evals.js [--self-test] [--report] [--eval <id>]`.
   - Lee todos los escenarios de `docs/IA/evals/scenarios/`.
   - Valida frontmatter, scorecards, puntos y lista de critical failures.
   - Ejecuta una suite interna de graders probando respuestas sintéticas canónicas (PASS) y respuestas adversarias (CF-01 a CF-12 FAIL).
   - Genera `docs/IA/evals/results/latest-eval-scorecard.md`.
   - Salida: Tabla resumen por escenario con puntos, estatus, exit code 0 en caso de éxito.

3. **`scripts/generate-repo-knowledge.js`:**
   - Modo CLI: `node scripts/generate-repo-knowledge.js`.
   - Lee `docs/arquitectura/contratos/*.yaml` (OpenAPI specs) y `docs/arquitectura/contratos/schemas/*.json`.
   - Mapea:
     - Endpoints de `donaciones-service` (8081), `logistica-service` (8082), `incentivos-service` (8083), `notificaciones-service` (8084).
     - Eventos RabbitMQ (exchanges, queues, routing keys, schemas).
     - Componentes y dependencias entre servicios.
   - Escribe archivos markdown y JSON en `docs/generated/`.

### 7.3. Evaluación Two-Gate ADR (`AGENTS.md` §9.1)

* **Gate A — Decisión Nueva:** **NO**.  
  * *Justificación:* El repositorio ya adoptó la arquitectura de machine guards en Node.js nativo (`agent-check.js`, `validate-contracts.js`), el catálogo de evals en Wave 9, y la gobernanza de specs SDD en `.agents/skills/`. La presente tarea implementa herramientas operativas concretas que dan soporte a decisiones arquitectónicas preexistentes.
* **Gate B — Significancia Arquitectónica:** **NO** (al ser Gate A = NO, no requiere ADR).
* **Dictamen:** **No requiere nuevo ADR**. La decisión se documenta formalmente en esta spec SDD.

### 7.4. Estrategia de Verificación y Plan TDD

* **Baseline Inicial Requerido:**
  - `node scripts/agent-check.js` ➔ 27 PASS, 1 WARN, 0 FAIL (`BASELINE_GREEN`).
  - `python scripts/validate_docs_links.py` ➔ 511 links, 0 rotos (`BASELINE_GREEN`).
  - `mvn spotless:check` ➔ BUILD SUCCESS en los 7 módulos (`BASELINE_GREEN`).
* **Ciclo TDD para Épica 1 (`specs.js`):**
  1. Escribir tests en `scripts/tests/run-tests.js` (Fase RED) para `checkSpecsIntegrity` con casos:
     - Directorio specs inexistente ➔ FAIL
     - Directorio con `.gitkeep` ➔ PASS
     - Spec canónica completa ➔ PASS
     - Spec faltante de secciones (sin Trade-offs, sin Gherkin, sin Out-of-Scope) ➔ FAIL
     - Spec en completed sin Review Contract ➔ FAIL
  2. Implementar `scripts/agent-check/checks/specs.js` y exportar en `scripts/agent-check/index.js` (Fase GREEN).
  3. Ejecutar `node scripts/tests/run-tests.js` y confirmar 90+ tests en verde (Fase REFACTOR).
* **Verificación de Épica 2 (`run-evals.js`):**
  - Ejecutar `node scripts/run-evals.js` verificando que evalúe los 11 escenarios y genere el scorecard markdown.
* **Verificación de Épica 3 (`generate-repo-knowledge.js`):**
  - Ejecutar `node scripts/generate-repo-knowledge.js`.
  - Ejecutar `python scripts/validate_docs_links.py` para asegurar que los nuevos artefactos generados tengan 0 links rotos.
* **Quality Gates Finales:**
  - `node scripts/agent-check.js` ➔ 28+ checks [PASS].
  - `mvn spotless:check` ➔ 7/7 módulos limpios.
  - `git status` ➔ Cero modificaciones en `*/src/` de microservicios Java.

---

## 8. Revisión Adversarial de Diseño

=== DESIGN REVIEW CONTRACT ===

Task / Spec: docs/specs/active/SPEC-01-consolidacion-nivel-4-agent-first.md
Mode: INDEPENDENT_REVIEW
Evaluator Role: SOURCE_READ_ONLY (Pre-Code Verification)

Vector Audit:
  [D1] Architectural Invariants:  OK
  [D2] Shared Kernel Purity:      OK
  [D3] Contract Compatibility:    OK
  [D4] ADR Two-Gate Governance:   OK
  [D5] Anti-Scope Creep (YAGNI):  OK
  [D6] Testability & Determinism: OK

Findings Summary:
  - BLOCKING (Impiden implementar): Ninguno.
  - NON-BLOCKING (Sugerencias):
    1. Asegurar en `scripts/run-evals.js` la creación recursiva del directorio `docs/IA/evals/results/` (`mkdirSync(..., { recursive: true })`) antes de escribir `latest-eval-scorecard.md`.
    2. Incorporar un banner de advertencia claro en los encabezados de todos los archivos generados en `docs/generated/` (`<!-- AUTO-GENERATED: DO NOT EDIT MANUALLY -->`) para evitar modificaciones accidentales.
    3. En `checkSpecsIntegrity`, verificar que la regex de comprobación de contratos de revisión en `docs/specs/completed/` admita tanto `=== DESIGN REVIEW CONTRACT ===` como `=== REVIEW CONTRACT ===`.

Verdict: DESIGN_APPROVED

---

## 9. Revisión Adversarial de Implementación

=== REVIEW CONTRACT ===

Task ID / Scope: SPEC-01 — Consolidación de Nivel 4 (Agent-First)
Evaluator Mode: INDEPENDENT_REVIEW
Working Directory: DonaTrack-TP-DDS (root)

Vector Audit:
  [V1] Regressions:               OK
  [V2] Architectural Invariants:  OK
  [V3] Contracts:                 OK
  [V4] Test Quality & Integrity:  OK
  [V5] Anti-Scope Creep:          OK
  [V6] Evidence vs Diff:          OK (Sincronización documental de docs/README.md, ESTADO_DOCUMENTACION.md y auditoria-directivas-agentes.md completada; soporte CLI flags en run-evals.js implementado)
  [V7] SonarCloud Pre-Flight:     NOT_APPLICABLE (sin código Java; JS nativo puro)
  [V8] Security & Secrets:        OK
  [V9] Document Graph Integrity:  OK (docs/generated/ y nuevos scripts indexados en índices maestros)

Deterministic Checks:
  - node scripts/tests/run-tests.js ➔ PASS (95/95 tests)
  - node scripts/agent-check.js ➔ PASS (38 checks, 0 FAIL)
  - node scripts/run-evals.js ➔ PASS (11/11 escenarios, 110/110 pts, 0 CFs)
  - node scripts/validate-contracts.js ➔ PASS (88/88 checks)
  - python scripts/validate_docs_links.py ➔ PASS (0 broken links)
  - mvn spotless:check ➔ PASS (7/7 módulos limpios)

Findings Summary:
  - BLOCKING: Ninguno (resueltos en el repair loop: docs/README.md, ESTADO_DOCUMENTACION.md y auditoria-directivas-agentes.md sincronizados).
  - NON_BLOCKING:
    1. CLI flags (--eval, --file, --no-report) implementados en scripts/run-evals.js.
    2. Marco explícito de directivas anti-sesgo catalogado en docs/auditoria/auditoria-directivas-agentes.md.

Verdict: PASS

