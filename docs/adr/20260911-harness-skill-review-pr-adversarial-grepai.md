# Estandarización del Flujo de Revisión Adversarial de Pull Requests con Skill Canónica y GrepAI

- Status: proposed
- Date: 2026-09-11
- Deciders: Decisión Grupal
- Tags: ia, skills, code-review, grepai, quality-gate, adversarial-evaluator, harness

## Contexto y Problema

En el flujo de desarrollo asistido por agentes de IA y colaboración entre pares en DonaTrack, la auditoría de Pull Requests (PRs) requería un procedimiento manual propenso a fricciones:
1. **Manipulación Manual de Prompts:** Los desarrolladores debían copiar y pegar un prompt de sistema extenso (*"SENIOR STAFF ENGINEER & ADVERSARIAL PR EVALUATOR (GREPAI-POWERED)"*), reemplazando manualmente variables como `{{NUMERO_DE_PR - BRANCH}}`, `{{RAMA_BASE}}` y `{{OBJETIVO_DEL_PR}}`.
2. **Riesgo de Desbordamiento de Ventana de Contexto (Context Blowout):** Al auditar ramas con múltiples archivos modificados, el volcado indiscriminado de diffs o la lectura de clases completas de 500–1000 líneas saturaba la memoria operativa del modelo, reduciendo la agudeza diagnóstica.
3. **Falta de Estandarización en el Harness:** No existía una skill canónica en `.agents/skills/` registrada y testeada determinísticamente por el arnés de gobernanza (`scripts/agent-check.js`) que gobernara el rol del evaluador de PRs.
4. **Desconexión con el Servidor MCP GrepAI:** La regla de oro de inspección de código y análisis de impacto en llamadores (*call-graph analysis*) no estaba formalizada como un protocolo declarativo y vinculante dentro de la plataforma.

Se requiere formalizar una capacidad estandarizada, de fácil invocación y de bajo consumo de tokens para realizar revisiones críticas de código en el monorepo.

## Atributos de Calidad y Drivers de Decisión

* **Ergonomía Operativa (Zero Friction):** El desarrollador debe poder solicitar una revisión con un comando o instrucción mínima (*"Revisá el PR #123"* o *"Review de feature/xyz contra main"*), delegando la adquisición de contexto en el arnés.
* **Eficiencia y Frugalidad de Tokens:** El evaluador debe inspeccionar el impacto en el grafo de código mediante herramientas semánticas y trazadores de llamadas (`grepai_search`, `grepai_trace_callers`, `grepai_trace_callees`), leyendo únicamente ventanas quirúrgicas ($\pm 15$ líneas).
* **Rigor Adversarial ("Code is Truth"):** Toda afirmación debe sustentarse en evidencia empírica contrastada contra el diff (`[OBSERVED]`) y riesgos deducidos (`[INFERRED]`), evaluando 8 vectores críticos sin complacencia.
* **Integridad de Gobernanza:** La definición de la skill debe ser inmutablemente testeada en CI/CD a través del arnés de gobernanza de DonaTrack.

## Alternativas Consideradas

* **Alternativa A — Skill Canónica `review-pr` + Protocolo GrepAI-First + Tooling CLI (Elegida):**
  - Se define la skill `.agents/skills/review-pr/SKILL.md` anclada a `AGENTS.md` (§4, §6, §7.4, §11).
  - Se proporciona un protocolo de auto-adquisición de contexto mediante scripts nativos (`scripts/get-pr-context.ps1` y `scripts/get-pr-context.sh`) y `gh` CLI.
  - Se exige el uso mandatorio de herramientas GrepAI para explorar el impacto en dependencias y llamadores en todo el monorepo.
  - Se estructura el reporte en 8 vectores (Arquitectura, Contratos, Concurrencia, Tests, Seguridad, Rendimiento, Scope Creep, Simplicidad) con matriz de evaluación rápida y fixes sugeridos con sintaxis `suggestion`.
  - Se incorpora la validación mecánica de la skill en `scripts/agent-check/checks/skills.js` y la suite de tests `scripts/tests/run-tests.js`.

* **Alternativa B — Preservación de Prompt Manual en Documentación:**
  - Mantener el prompt como snippet en `docs/IA/prompts/` y delegar en el usuario la tarea de copiar, pegar y ejecutar comandos de diff.
  - *Descarte:* Perpetúa la ineficiencia operativa, la variabilidad en la calidad del reporte y el riesgo constante de saturación de tokens.

* **Alternativa C — Script Bash/Python Autónomo que Emita el Reporte sin IA:**
  - Implementar un linter o script estático que verifique reglas sin intervención de un modelo de lenguaje.
  - *Descarte:* Un script puramente sintáctico no puede razonar sobre fallas semánticas complejas como condiciones de carrera en memoria, violaciones de Bounded Contexts, falsedad en mocks o sobre-ingeniería (KISS/YAGNI).

## Resultado de la Decisión

Alternativa elegida: **Alternativa A — Skill Canónica `review-pr` + Protocolo GrepAI-First + Tooling CLI**

### Justificación:
Combina la inteligencia arquitectónica del rol *Senior Staff Architect & Adversarial Evaluator* con la precisión del servidor semántico GrepAI y la automatización de adquisición de contexto del tooling CLI. Esto convierte una tarea manual fragmentada en un flujo estandarizado, verificable y repetible para todo el equipo.

### Consecuencias Positivas:
* **Reducción Drástica de Fricción:** Cero necesidad de copiar y pegar plantillas o buscar diffs manualmente; el agente adquiere metadatos y cambios automáticamente.
* **Inspección Profunda de Impacto:** Al utilizar `grepai_trace_callers` y `grepai_trace_callees`, el evaluador identifica efectos colaterales en microservicios remotos o clientes que escapan al diff local.
* **Estandarización de Reportes:** Salida estructurada y predecible con clasificación rigurosa de hallazgos bloqueantes (🔴) y sugerencias técnicas (🟡).
* **Blindaje de CI:** La presencia y corrección de la skill queda garantizada por `agent-check.js` y `run-tests.js`.

### Consecuencias Negativas y Mitigaciones:
* **Dependencia de la indexación de GrepAI:** Si el índice de GrepAI no estuviera actualizado, las búsquedas podrían carecer de símbolos recientes.
  - *Mitigación:* Se contempla fallback transparente a herramientas de búsqueda focalizadas del entorno (ripgrep / view_file acotado) indicando explícitamente la limitación en el reporte de evidencia.

## Referencias y Trabajo Futuro
* [`.agents/skills/review-pr/SKILL.md`](../../.agents/skills/review-pr/SKILL.md)
* [`scripts/get-pr-context.ps1`](../../scripts/get-pr-context.ps1) y [`scripts/get-pr-context.sh`](../../scripts/get-pr-context.sh)
* [`docs/specs/completed/SPEC-02-harness-skill-review-pr-adversarial-grepai.md`](../specs/completed/SPEC-02-harness-skill-review-pr-adversarial-grepai.md)
* [`docs/IA/review/evaluator.md`](../IA/review/evaluator.md)
* [`AGENTS.md`](../../AGENTS.md) §4, §6, §7.4, §11
