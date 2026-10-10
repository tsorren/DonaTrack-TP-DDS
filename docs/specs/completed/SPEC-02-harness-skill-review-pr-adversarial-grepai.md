# Spec: SPEC-02 — Skill Estandarizada y Adversarial de Review de PRs (GrepAI-Powered)

> **Estado:** COMPLETED  
> **Nivel:** ARCHITECTURAL  
> **Fecha:** 2026-09-11  
> **Módulos Impactados:** `.agents/skills/`, `scripts/`, `docs/specs/`, `docs/adr/`, `docs/IA/`

---

## 1. Objetivo Funcional (Goal)

Proveer una forma estandarizada, ergonómica y de alta fidelidad para que cualquier agente de IA o desarrollador humano realice revisiones críticas y adversariales de Pull Requests en DonaTrack sin necesidad de copiar y pegar prompts manualmente, ni lidiar con el desbordamiento de ventana de contexto al inspeccionar diffs voluminosos.  

Esto se logra mediante:
1. **Skill Canónica `review-pr`:** Activación declarativa y fluida que asume el rol de *Senior Staff Architect & Adversarial PR Evaluator*.
2. **Protocolo GrepAI-First & Token-Frugal:** Uso estricto de búsquedas semánticas y trazado de grafos de llamadas (`grepai_search`, `grepai_trace_callers`, `grepai_trace_callees`, `grepai_refs_readers`) y lecturas quirúrgicas ($\pm 15$ líneas de contexto) para auditar impactos en todo el monorepo con consumo mínimo de tokens.
3. **8 Vectores Críticos de PR Review:** Auditoría integral sobre Arquitectura, Contratos, Concurrencia/Robustez, Calidad de Pruebas, Seguridad/Privacidad, Rendimiento/Observabilidad, Anti-Scope Creep y Simplicidad (KISS/YAGNI).
4. **Tooling de Contexto Automatizado:** Scripts CLI multiplataforma (`scripts/get-pr-context.ps1` y `scripts/get-pr-context.sh`) que extraen automáticamente metadata, archivos modificados, microservicios impactados y diff stats a partir de un número de PR (`#123`), branch o cambios locales.
5. **Reporte Estandarizado Compacto:** Estructura uniforme con veredicto, nivel de riesgo, diagnóstico en 2-3 oraciones, matriz 8D, hallazgos bloqueantes con formato `[Archivo:Línea]` y sugerencias de corrección concretas, sugerencias técnicas y validaciones pendientes.

---

## 2. Alcance Delimitado (Scope Boundaries)

### In-Scope (Lo que SÍ incluye):
* Creación de `.agents/skills/review-pr/SKILL.md` con frontmatter válido, anclaje a `AGENTS.md`, protocolo GrepAI, 8 vectores y reporte compacto.
* Creación de `scripts/get-pr-context.ps1` y `scripts/get-pr-context.sh` para la extracción autónoma de contexto de PRs y branches.
* Formalización del ADR `docs/adr/20260911-harness-skill-review-pr-adversarial-grepai.md` bajo formato MADR y Two-Gate Rule en estado `proposed`.
* Actualización del arnés de gobernanza: `scripts/agent-check/checks/skills.js` incorporando `review-pr` en `EXPECTED_SKILLS` y validando sus vectores y protocolo.
* Tests unitarios en `scripts/tests/run-tests.js` para asegurar el chequeo determinista de `review-pr`.
* Sincronización del grafo documental en `docs/README.md`, `docs/ESTADO_DOCUMENTACION.md` y `docs/IA/prompts/reviewer-pr-implementacion.md`.

### Out-of-Scope (Lo que NO incluye - Anti-Scope Creep):
* Modificación de código fuente Java de microservicios en `*/src/main/java/` o `*/src/test/java/`.
* Alteración de infraestructura de contenedores Docker (`docker-compose.yml`, Dockerfiles) o flujos de n8n.
* Incorporación de dependencias externas npm (se preserva la política de tooling nativo en Node.js 20+).
* Auto-promoción de ningún ADR a `accepted` o `rejected`.

---

## 3. Decisiones Acordadas y Trade-offs

Para la arquitectura del flujo estandarizado de review de PRs se contrastaron tres alternativas en la Matriz 5D:

| Dimensión | Opción 1: Skill Canónica + GrepAI + Helper CLI (Elegida) | Opción 2: Prompt Copy-Paste Manual (Descartada) | Opción 3: Script Monolítico sin Skill de Agente (Descartada) |
|---|---|---|---|
| **1. Ergonomía / UX** | **Máxima:** El usuario solo dice *"revisá el PR #123"* o branch y el agente auto-adquiere contexto y audita. | **Mínima:** Copiar, pegar, editar placeholders y correr comandos a mano. | **Media:** Requiere ejecutar un script externo y pasar el stdout al agente manualmente. |
| **2. Eficiencia de Tokens** | **Óptima:** Protocolo GrepAI-First y lecturas quirúrgicas ($\pm 15$ líneas); previene context blowout. | **Baja:** Tiende a volcar diffs masivos enteros en la ventana de contexto. | **Variable:** Depende del filtrado que realice el script. |
| **3. Gobernanza e Integridad** | **Alta:** Integrado a `agent-check.js`, validado por CI, anclado a `AGENTS.md`. | **Nula:** Prompt suelto sin validación ni trazabilidad en CI. | **Media:** Script testeable pero desalineado del sistema de skills del agente. |
| **4. Rigor Adversarial** | **Estricto:** 8 vectores estandarizados, cero complacencia, evidencia factual (`[OBSERVED]` vs `[INFERRED]`). | **Inconsistente:** Depende del prompt individual y de la atención del usuario al configurarlo. | **Bajo:** Un script no razona sobre arquitectura ni concurrencia. |
| **5. Reversibilidad / Mantenibilidad** | **Alta:** Definición modular en `.agents/skills/review-pr/` con arnés de testing automatizado. | **Nula:** Prompts locales dispersos y fragmentados. | **Media:** Script de mantenimiento desacoplado. |

* **Alternativa Elegida:** Opción 1 — Skill Canónica `review-pr` + Protocolo GrepAI + Helper CLI.
* **Justificación:** Resuelve la raíz de la incomodidad del usuario (la necesidad de copiar y pegar prompts y rellenar variables) dotando al agente de una capacidad nativa, determinista y verificada por el arnés de gobernanza del repositorio.

---

## 4. Invariantes de Negocio y Reglas de Integridad

1. **`[INVARIANT]` "Code is Truth" (Evidencia sobre Afirmaciones):** El evaluador no asume que las aseveraciones del autor del PR o del agente implementador son ciertas; toda conclusión debe contrastarse contra el diff observable.
2. **`[INVARIANT]` Protocolo GrepAI-First:** Prohibido el uso de lecturas masivas de clases completas o comandos ciegos que desborden el contexto; se priorizan herramientas semánticas y de grafo de llamadas.
3. **`[INVARIANT]` Integridad de Machine Guards:** `node ./scripts/agent-check.js` debe arrojar `exit code 0` con todas las comprobaciones de gobernanza en verde.
4. **`[INVARIANT]` Zero Broken Links:** Todo enlace en la documentación sincronizada debe resolver exitosamente con 0 fallos en `validate_docs_links.py`.
5. **`[INVARIANT]` Separación Epistémica:** Todo hallazgo debe categorizarse honestamente como `[OBSERVED]` (comprobado en código) o `[INFERRED]` (riesgo deducido).

---

## 5. Criterios de Aceptación (Gherkin / Given-When-Then)

```gherkin
Escenario: Activación y estructura canónica de la skill review-pr
  Dado el directorio .agents/skills/review-pr/
  Cuando se inspecciona el archivo SKILL.md
  Entonces contiene frontmatter YAML válido con name "review-pr" y descripción
  Y referencia explícitamente las invariantes de AGENTS.md
  Y define el protocolo obligatorio GrepAI-First y los 8 vectores de auditoría
  Y define la plantilla de Reporte de Salida Compacto con Veredicto, Matriz 8D y Bloqueantes.

Escenario: Verificación mecánica de la skill en agent-check
  Dado el arnés de gobernanza en scripts/agent-check/
  Cuando se ejecuta "node scripts/agent-check.js"
  Entonces la suite incluye "review-pr" en EXPECTED_SKILLS
  Y valida exitosamente los 8 vectores y el anclaje a AGENTS.md con 0 fallos.

Escenario: Extracción automatizada de contexto de PR
  Dado el script scripts/get-pr-context.ps1
  Cuando se invoca con un número de PR o rama
  Entonces extrae la metadata, archivos modificados, microservicios impactados y diff stats sin errores.
```

---

## 7. Especificación Técnica de Diseño (Technical Design)

### 7.1 Evaluación de la Two-Gate Rule para ADR
* **Gate A — Decisión Novedosa:** SÍ. Introduce un flujo de auditoría de PRs adversarial integrado con herramientas GrepAI y helper de contexto CLI en el arnés de agentes.
* **Gate B — Significancia Arquitectónica:** SÍ. Afecta el workflow de revisión de código, QA y gobierno del repositorio, definiendo un estándar para pull requests hacia las ramas base.
* **Resultado:** Se formaliza el ADR `docs/adr/20260911-harness-skill-review-pr-adversarial-grepai.md` con status `proposed`.

### 7.2 Componentes e Interfaces
1. **Skill `.agents/skills/review-pr/SKILL.md`:**
   - Rol: *Senior Staff Architect & Adversarial PR Evaluator*.
   - 3 Modos de adquisición de contexto (PR Number, Branch/Target, Current Workspace Changes).
   - Matriz de 8 Vectores (Arquitectura, Contratos, Concurrencia, Tests, Seguridad, Rendimiento, Scope Creep, Simplicidad).
   - Formato de reporte compacto con Veredicto, Matriz 8D, Hallazgos Bloqueantes (`[Archivo:Línea]`), Sugerencias Técnicas y Validación Pendiente.
2. **Helper CLI `scripts/get-pr-context.ps1` y `.sh`:**
   - Soporte para `-PR <number>`, `-Branch <branch>`, `-Base <base>`.
   - Consulta GitHub CLI (`gh pr view`, `gh pr diff --stat`) con fallback a comandos `git`.
   - Resumen estructurado por microservicio y clasificación de cambios (código, contratos, configs, tests, docs).
3. **Machine Guard `scripts/agent-check/checks/skills.js`:**
   - Validación de `EXPECTED_SKILLS` incluyendo `review-pr`.
   - Validación de presencia de los 8 vectores (Arquitectura, Contratos, Concurrencia, Tests, Seguridad, Rendimiento, Scope, Simplicidad).
   - Validación de presencia de protocolo GrepAI y estructura de reporte.

### 7.3 Plan TDD (Fase RED ➔ GREEN ➔ REFACTOR)
1. **Fase RED:** Agregar `review-pr` a `EXPECTED_SKILLS` en `scripts/agent-check/checks/skills.js` y test unitario en `scripts/tests/run-tests.js` antes de crear el archivo `SKILL.md`. Correr tests y verificar fallo por archivo faltante.
2. **Fase GREEN:** Crear `.agents/skills/review-pr/SKILL.md` con el contenido completo. Crear `scripts/get-pr-context.ps1` y `scripts/get-pr-context.sh`. Correr tests y verificar que pasen exitosamente.
3. **Fase REFACTOR:** Crear ADR `docs/adr/20260911-harness-skill-review-pr-adversarial-grepai.md`, sincronizar `docs/ESTADO_DOCUMENTACION.md`, `docs/README.md` y `docs/IA/prompts/reviewer-pr-implementacion.md`, y verificar con `agent-check.js` y `validate_docs_links.py`.

---

## 8. Revisión Crítica Canónica (Review Contract)

=== REVIEW CONTRACT ===

Mode: SELF_REVIEW
Task Level: ARCHITECTURAL

V1 Regression risks:
  NONE_DETECTED

V2 Architecture/invariant violations:
  NONE_DETECTED

V3 Contracts/integration:
  NONE_DETECTED

V4 Tests/coverage:
  NONE_DETECTED

V5 Scope violations:
  NONE_DETECTED

V6 Evidence integrity:
  NONE_DETECTED

V7 SonarCloud pre-flight:
  NOT_APPLICABLE

V8 Security/privacy:
  NOT_APPLICABLE

V9 Documentation graph integrity:
  NONE_DETECTED

ADR review (si ADR proposed):
  Puntaje: 4.8/5.0 — ADR docs/adr/20260911-harness-skill-review-pr-adversarial-grepai.md formalizado bajo MADR en status proposed.

Verdict: PASS

Findings:
  BLOCKING: (vacío)
  ADVISORY: (vacío)

[SELF_REVIEW_FALLBACK]
Auditoría adversarial ejecutada en modo SELF_REVIEW tras validación empírica determinista en arnés CI (scripts/agent-check.js y scripts/tests/run-tests.js).
