# Informe de investigación — Agent-Friendly Repository Architecture para desarrollo con agentes IA

Investigación enfocada en **cómo diseñar un repositorio para que coding agents como Codex, Claude Code, Copilot, Gemini, Cursor u otros puedan comprenderlo, modificarlo, verificarlo y mantenerlo con menor intervención humana y menor degradación arquitectónica**.

Esta investigación encaja directamente con los temas que venimos trabajando: **Harness Engineering, Context Engineering, SDD, Agent Graphs, Evals y Generator/Evaluator**. En realidad, *Agent-Friendly Repository Architecture* es la capa que hace que muchas de esas prácticas funcionen de forma sostenible.

---

## 1. Qué es Agent-Friendly Repository Architecture

No existe todavía una especificación formal llamada *Agent-Friendly Repository Architecture*. Es más útil entenderla como una disciplina emergente:

> **Diseñar el repositorio no sólo para que sea entendible por humanos, sino también para que un agente pueda descubrir cómo funciona, encontrar el contexto correcto, realizar cambios seguros y comprobar objetivamente que no rompió el sistema.**

El repositorio deja de ser solamente:

```text
source code
+
tests
+
README
```

y pasa a funcionar como:

```text
                    REPOSITORY
                         │
       ┌─────────────────┼─────────────────┐
       │                 │                 │
       ▼                 ▼                 ▼
    código          conocimiento       contratos
       │                 │                 │
       ▼                 ▼                 ▼
 arquitectura        docs / ADRs       specs / APIs
       │
       ▼
   invariantes
       │
       ▼
tests / lint / CI / structural checks
       │
       ▼
       AGENT
```

La idea fundamental es:

> **El repositorio debe contener suficiente información operativa como para que una sesión nueva del agente pueda reconstruir el estado del sistema sin depender del historial de chat de otra sesión.**

Esto coincide fuertemente con tu investigación previa de Harness Engineering: el harness no es solamente el modelo, sino también contexto, herramientas, tests, políticas, evidencia, permisos y observabilidad.

---

## 2. Por qué este tema se volvió importante ahora

OpenAI publicó en 2026 una de las referencias más importantes sobre este tema:

**Harness engineering: leveraging Codex in an agent-first world.**

La publicación describe un entorno interno donde agentes Codex llegaron a producir un sistema de aproximadamente un millón de líneas y miles de PRs.

Una de sus conclusiones centrales fue:

> **“We made repository knowledge the system of record.”**

El problema que encontraron temprano fue el manejo del contexto.

Intentaron concentrar información en un enorme `AGENTS.md` y el enfoque falló porque:

- ocupaba contexto que debía utilizarse para la tarea;
- demasiadas reglas diluían las realmente importantes;
- el archivo se volvía obsoleto rápidamente;
- era difícil verificar mecánicamente su consistencia.

La solución fue cambiar:

```text
AGENTS.md = manual completo
```

por:

```text
AGENTS.md = mapa
          ↓
structured repository knowledge
```

Su `AGENTS.md` quedó alrededor de las 100 líneas y el conocimiento real pasó a un árbol estructurado dentro del repositorio.

Fuente: https://openai.com/index/harness-engineering/

Esta dirección coincide casi exactamente con el refactor que hicimos para DonaTrack: reducir el `AGENTS.md`, introducir `context-index`, separar policies de documentación profunda y convertir restricciones importantes en checks.

---

## 3. El repositorio pasa a ser parte del harness

Una arquitectura tradicional piensa:

```text
Agent
  ↓
reads code
  ↓
writes code
```

Una arquitectura agent-friendly piensa:

```text
                     USER INTENT
                          │
                          ▼
                      AGENTS.md
                          │
                          ▼
                    Context Router
                          │
              ┌───────────┼───────────┐
              ▼           ▼           ▼
            Domain    Architecture   Tests
              │           │           │
              └───────────┼───────────┘
                          ▼
                         CODE
                          │
                          ▼
                  deterministic checks
                          │
                          ▼
                      EVIDENCE
```

Por eso considero que:

> **El repositorio es una parte del harness.**

Y esta relación puede expresarse así:

```text
MODEL
  +
TOOLS
  +
HARNESS
  +
AGENT-FRIENDLY REPOSITORY
  =
REAL CODING CAPABILITY
```

---

## 4. Primera propiedad: discoverability

El agente debería poder entrar en un repositorio nuevo y contestar rápidamente:

```text
¿Qué es este sistema?
¿Cómo está dividido?
¿Dónde está la lógica relevante?
¿Qué puedo modificar?
¿Qué no debo romper?
¿Cómo ejecuto esto?
¿Cómo lo verifico?
```

No debería necesitar recorrer 250 archivos para averiguarlo.

Por eso conviene tener puntos de entrada previsibles:

```text
AGENTS.md
README.md
ARCHITECTURE.md
docs/
```

El estándar abierto `AGENTS.md` ya está siendo utilizado por decenas de miles de repositorios y está pensado precisamente como un README para agentes. Permite además archivos anidados para monorepos y subproyectos.

Fuente: https://agents.md/

GitHub también documenta actualmente `AGENTS.md` como mecanismo compartido entre herramientas, diferenciándolo de instrucciones específicas de Copilot.

Fuente: https://docs.github.com/en/copilot/concepts/agents/code-review

---

## 5. Segunda propiedad: progressive disclosure

Una idea especialmente importante.

No debería cargarse esto automáticamente:

```text
AGENTS.md
+
arquitectura completa
+
53 ADRs
+
manual DB
+
manual frontend
+
security policies
+
testing handbook
+
API documentation
```

para resolver:

```text
rename local de una variable
```

El flujo debería ser:

```text
minimal context
      ↓
identify task
      ↓
retrieve relevant context
      ↓
work
```

Ejemplo:

```text
Task:
"Agregar validación a donaciones"

AGENTS.md
    ↓
context-index
    ↓
donaciones
    ↓
domain rules
    ↓
relevant ADR
    ↓
tests
```

OpenAI denomina explícitamente este enfoque **progressive disclosure**.

Fuente: https://openai.com/index/harness-engineering/

Anthropic recomienda algo similar con `CLAUDE.md`: mantenerlo conciso y permitir instrucciones jerárquicas que sólo se cargan cuando se trabaja en una parte particular del repositorio.

Fuente: https://www.anthropic.com/engineering/claude-code-best-practices

---

## 6. Tercera propiedad: arquitectura explícita

Éste probablemente sea uno de los hallazgos más importantes de OpenAI.

No alcanza con explicarle al agente:

```text
intentá respetar clean architecture
```

La arquitectura debería poder comprobarse.

OpenAI utiliza dependencias por capas similares a:

```text
Types
 ↓
Config
 ↓
Repo
 ↓
Service
 ↓
Runtime
 ↓
UI
```

y prohíbe dependencias en sentido contrario mediante **linters y structural tests**.

Su conclusión es particularmente interesante:

> con coding agents, restricciones arquitectónicas que tradicionalmente aparecerían cuando una organización tiene cientos de desarrolladores pasan a ser útiles desde mucho antes.

Porque los agentes pueden producir código mucho más rápido que los humanos.

Sin restricciones:

```text
generation speed ↑
architectural entropy ↑↑↑
```

Con restricciones:

```text
generation speed ↑
architecture remains bounded
```

OpenAI aplica custom linters para estructura, logging, naming, tamaño de archivos y otros invariantes.

Fuente: https://openai.com/index/harness-engineering/

---

## 7. Instructions as code

Este concepto es fundamental.

Regla débil:

```text
AGENTS.md

"common-lib no debe depender de servicios de dominio"
```

Regla fuerte:

```text
structural test

common-lib
    X
domain-service
```

y CI falla.

Otro ejemplo.

Débil:

```text
Todos los ADR deben tener un status válido.
```

Fuerte:

```text
agent-check

if ADR.status not in
[Proposed, Accepted, Rejected, Deprecated]
    fail CI
```

La evolución ideal es:

```text
Natural language
      ↓
repeated failure
      ↓
explicit policy
      ↓
automated check
```

---

## 8. Cuarta propiedad: deterministic feedback loops

Un repositorio agent-friendly debe proporcionar al agente una forma clara de responder:

```text
¿mi cambio funciona?
```

Sin preguntárselo al propio LLM.

Idealmente debería disponer de:

```text
build
lint
typecheck
unit
integration
e2e
architecture checks
contract tests
security checks
```

Anthropic señala que los coding agents funcionan especialmente bien cuando tienen un target verificable contra el cual iterar, y recomienda tests, TDD y feedback directo desde herramientas.

Fuente: https://www.anthropic.com/engineering/claude-code-best-practices

Esto conecta con la regla:

```text
evidence > claims
```

No debería aceptarse:

```text
"Todo funciona."
```

Debería poder obtenerse:

```text
mvn test
exit=0

npm lint
exit=0

architecture-check
PASS
```

---

## 9. Quinta propiedad: estado importante dentro del repositorio

Una conversación del agente no debería ser el lugar permanente de una decisión.

Mala arquitectura:

```text
Chat session

message 87:
"decidimos usar async AMQP para este flujo"
```

Después:

```text
compaction
session ends
new agent
```

La decisión desaparece.

Agent-friendly:

```text
docs/adr/ADR-017.md
```

o:

```text
specs/feature-x/spec.md
```

La información importante debe convertirse en artefacto.

OpenAI utiliza:

```text
docs/
├── design-docs/
├── exec-plans/
│   ├── active/
│   ├── completed/
│   └── tech-debt-tracker.md
├── product-specs/
├── generated/
├── references/
└── ...
```

y trata planes, decisiones y deuda como artefactos versionados.

Fuente: https://openai.com/index/harness-engineering/

Anthropic llegó a una conclusión similar para tareas largas: dividir el trabajo y utilizar **structured artifacts** para transferir contexto entre sesiones.

Fuente: https://www.anthropic.com/engineering/harness-design-long-running-apps

---

## 10. La arquitectura más sólida hoy

No conviene copiar literalmente la estructura de OpenAI.

Conviene tomar el patrón.

Por ejemplo:

```text
repo/
│
├── AGENTS.md
├── README.md
├── ARCHITECTURE.md
│
├── docs/
│   │
│   ├── context-index.md
│   │
│   ├── architecture/
│   │   ├── overview.md
│   │   ├── boundaries.md
│   │   └── dependencies.md
│   │
│   ├── domain/
│   │   ├── module-a.md
│   │   └── module-b.md
│   │
│   ├── adr/
│   │   ├── README.md
│   │   └── ADR-XXX.md
│   │
│   ├── specs/
│   │   ├── active/
│   │   └── completed/
│   │
│   ├── testing/
│   │   ├── strategy.md
│   │   └── commands.md
│   │
│   ├── contracts/
│   │   ├── REST.md
│   │   └── events.md
│   │
│   └── generated/
│       ├── db-schema.md
│       └── dependency-map.md
│
├── scripts/
│   ├── agent-check
│   ├── architecture-check
│   ├── docs-check
│   └── test-changed
│
├── src/
│
└── tests/
```

Ésta no es una estructura obligatoria.

Lo importante es la separación conceptual:

```text
instructions
knowledge
decisions
current intent
code
verification
```

---

## 11. Qué debería contener `AGENTS.md`

Debería contestar solamente cinco preguntas:

```text
1. ¿Qué proyecto es éste?
2. ¿Qué no debo romper?
3. ¿Cómo trabajo?
4. ¿Cómo verifico?
5. ¿Dónde encuentro más contexto?
```

Ejemplo conceptual:

```markdown
# Project

Short description.

# Core invariants

- Do not introduce domain logic into shared-kernel.
- Do not modify accepted architecture silently.
- Never disable tests to make a change pass.

# Workflow

Inspect → understand → change → validate.

# Verification

Use the closest relevant tests.
Run ./scripts/agent-check before completion.

# Context

Use docs/context-index.md.
Load only documentation relevant to the task.
```

No debería contener:

```text
80 líneas sobre Docker
40 sobre ADRs
70 sobre testing
60 sobre Git
100 sobre arquitectura
```

Todo eso debería estar detrás de links.

---

## 12. Nested AGENTS.md

Para monorepos pueden ser muy útiles.

Ejemplo:

```text
AGENTS.md

common-lib/
└── AGENTS.md

backend/
└── AGENTS.md

frontend/
└── AGENTS.md
```

Root:

```text
global invariants
```

Nested:

```text
local invariants
```

Pero hay una regla importante:

> **No crear nested AGENTS solamente porque existe soporte.**

Sólo deberían existir cuando una subtree tiene invariantes realmente diferentes.

---

## 13. Código legible por agentes

Muchos patrones buenos para humanos también son buenos para agentes:

```text
módulos pequeños
interfaces explícitas
nombres semánticos
bajo acoplamiento
tests cercanos
dependencias predecibles
```

Pero con agentes aumenta el costo de la ambigüedad.

Por ejemplo:

```text
utils/
helpers/
shared/
common/
misc/
```

son carpetas difíciles de navegar semánticamente.

Mejor:

```text
auth/
billing/
reservations/
notifications/
```

Del mismo modo:

```text
doStuff()
processData()
handleThing()
```

es peor que:

```text
calculateReservationPrice()
validateReservationOverlap()
sendConfirmationEmail()
```

Esto reduce el espacio de búsqueda del agente.

---

## 14. Locality importa, pero availability importa todavía más

Un paper reciente de agosto de 2026 estudió el *working set* de coding agents.

Su conclusión es relevante:

> los agentes necesitan tener disponibles los hechos de los que depende una modificación cuando la realizan.

Cuando falta un dato crítico, el agente no necesariamente se detiene.

Frecuentemente **lo inventa**.

Además encontraron algo importante: una convención obsoleta puede ser **peor que no proporcionar ninguna convención**, porque el agente puede obedecer explícitamente una regla incorrecta.

Fuente: https://arxiv.org/abs/2608.16630

Esto implica:

```text
doc correcta
      >
no doc
      >
doc incorrecta
```

Por eso **documentation freshness** debería formar parte del harness.

---

## 15. Documentation gardening

OpenAI incluso utiliza agentes recurrentes para detectar documentación obsoleta.

Conceptualmente:

```text
code change
   ↓
documentation drift?
   ↓
doc-gardening agent
   ↓
PR
```

Además aplican CI para comprobar estructura y enlaces de la knowledge base.

Fuente: https://openai.com/index/harness-engineering/

Podría evolucionarse hacia:

```text
docs-check
├── broken links
├── missing module
├── stale version
├── stale ADR reference
├── generated artifact outdated
└── undocumented architectural boundary
```

---

## 16. Generated documentation

Siempre que el repositorio pueda producir una verdad mecánicamente, conviene eso antes que pedir documentación manual.

Ejemplo:

```text
database
 ↓
generator
 ↓
docs/generated/db-schema.md
```

Otro:

```text
pom.xml
   ↓
script
   ↓
module-map.md
```

Otro:

```text
source imports
   ↓
dependency analyzer
   ↓
architecture graph
```

La regla sería:

> **Derive what can be derived. Document manually only what represents intent, reasoning or decisions.**

---

## 17. Source-of-truth hierarchy

Una arquitectura agent-friendly debería explicar cuál es la autoridad de cada fuente.

Algo como:

```text
Requirements → alcance

Spec → comportamiento deseado

ADR → decisiones aceptadas

Architecture docs → estructura pretendida

Contracts → interfaces públicas

Code → comportamiento implementado

Tests → comportamiento verificable

Runtime / DB → realidad operativa
```

Esto evita que el agente encuentre una wiki vieja y trate de “arreglar” el código para hacerla coincidir.

---

## 18. Git también es contexto

Un coding agent puede aprender muchísimo mediante:

```text
git log
git blame
git show
```

Anthropic recomienda explícitamente investigar el historial Git para comprender por qué existe una API o una decisión.

Fuente: https://www.anthropic.com/engineering/claude-code-best-practices

Esto refuerza prácticas como:

```text
commits coherentes
PRs pequeñas
commit messages descriptivos
ADRs referenciados
issues vinculadas
```

Un historial Git limpio se convierte en otra base de conocimiento.

---

## 19. Work-unit sizing

Los agentes funcionan mejor cuando una modificación tiene límites razonables.

Mala tarea:

```text
"Modernizá el backend."
```

Mejor:

```text
"Convertí PaymentRepository de sync a async
sin modificar el contrato REST.
Agregá regression tests."
```

Esto reduce:

```text
context size
scope creep
review complexity
merge conflict
self-evaluation ambiguity
```

Anthropic también observó que para long-running coding es importante descomponer el trabajo en unidades manejables.

Fuente: https://www.anthropic.com/engineering/harness-design-long-running-apps

---

## 20. Diseñar el repositorio para paralelización futura

Si todo el sistema está acoplado:

```text
A↔B↔C↔D↔E
```

cinco agentes trabajando paralelamente generan conflictos.

Si hay boundaries claros:

```text
Domain A

Domain B

Domain C
```

es posible asignar:

```text
Agent 1 → Domain A
Agent 2 → Domain B
Agent 3 → Domain C
```

Por eso modularidad y ownership dejan de ser solamente decisiones de diseño humano.

También determinan **cómo se puede paralelizar trabajo agentic**.

---

## 21. Seguridad y blast radius

Un repo preparado para agentes tiene que asumir que el agente puede ejecutar herramientas.

Por eso debería ser sencillo ejecutar el sistema dentro de:

```text
container
sandbox
test DB
mock credentials
local services
```

y difícil acceder accidentalmente a:

```text
production DB
real secrets
cloud admin credentials
production deploy
```

Anthropic reporta que el sandboxing mejora autonomía al mismo tiempo que limita el radio de impacto.

Fuente: https://www.anthropic.com/engineering/claude-code-sandboxing

Un repositorio agent-friendly debería facilitar:

```text
autonomy inside safe boundaries
```

en lugar de depender de:

```text
"por favor no hagas nada peligroso"
```

---

## 22. La evidencia académica introduce una advertencia importante

No toda la investigación reciente confirma que simplemente agregar `AGENTS.md` produzca grandes mejoras.

Un estudio de julio de 2026 comparó Claude Code y Codex en 288 runs y no encontró una mejora significativa de correctness únicamente por incluir archivos persistentes de contexto.

En esas tareas, muchas fallas provenían de capacidad de implementación y no de falta de conocimiento del repositorio.

Fuente: https://arxiv.org/abs/2607.27250

Esto no contradice necesariamente la arquitectura que estamos analizando.

Más bien demuestra:

```text
AGENTS.md ≠ solución mágica
```

Un repositorio agent-friendly necesita:

```text
discoverability
+
context
+
architecture
+
verification
+
tools
+
feedback
```

No solamente instrucciones.

---

## 23. Pero la calidad de las instrucciones sí parece importar

Otra investigación de 2026 estudió cómo mejorar sistemáticamente las instrucciones del repositorio.

Con un proceso denominado **probe-and-refine**, la tasa media de resolución pasó aproximadamente de:

```text
25.5% sin guidance

28.3% static guidance

33.0% refined guidance
```

en su experimento.

La mejora vino principalmente de ayudar al agente a encontrar correctamente dónde realizar el cambio.

Fuente: https://arxiv.org/abs/2606.20512

Es una distinción importante:

> repository guidance probablemente ayuda más a **orientar y restringir** al agente que a hacerlo intrínsecamente mejor programador.

---

## 24. Configuration smells

Otro estudio de 2026 analizó 100 repositorios con `AGENTS.md`/`CLAUDE.md`.

Encontró varios anti-patrones frecuentes, incluyendo:

```text
Lint Leakage
Context Bloat
Skill Leakage
Conflicting Instructions
```

`Context Bloat` aparecía en aproximadamente el 42 % de los archivos analizados.

Fuente: https://arxiv.org/abs/2606.15828

Esto refuerza la idea:

```text
AGENTS pequeño
+
context routing
+
task-specific skills
```

en lugar de:

```text
AGENTS gigante
```

---

## 25. AGENTS vs Skills vs Docs

Una distinción importante:

```text
AGENTS
= always-on policy

Docs
= knowledge

Skills
= workflow ejecutable bajo demanda
```

Por ejemplo:

```text
AGENTS.md
"No romper accepted architecture."
```

```text
docs/adr/README.md
"Así funciona nuestro lifecycle de ADR."
```

```text
skills/create-adr/
"Proceso concreto para crear y validar un ADR."
```

GitHub ya distingue explícitamente este uso entre instrucciones permanentes, instrucciones por path, `AGENTS.md` y skills por tarea.

Fuente: https://docs.github.com/en/copilot/concepts/agents/code-review

---

## 26. Qué debería ser portable y qué puede ser vendor-specific

Usaría:

```text
AGENTS.md
```

para reglas compartidas.

Y adaptadores solamente cuando hacen falta:

```text
CLAUDE.md
.github/copilot-instructions.md
GEMINI.md
.cursor/
```

Pero esos archivos deberían apuntar a las mismas fuentes.

No:

```text
AGENTS.md     → 100 reglas
CLAUDE.md     → copia
GEMINI.md     → copia
COPILOT.md    → copia
```

porque inmediatamente aparece drift.

La política debería ser:

```text
repository truth
       ↓
small adapters
       ↓
different agents
```

---

## 27. Modelo completo recomendado

```text
┌───────────────────────────────────────────────────┐
│                    INTENT                         │
│                 issue / spec                      │
└───────────────────────┬───────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────┐
│                  ENTRY POINT                      │
│                    AGENTS                         │
└───────────────────────┬───────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────┐
│                CONTEXT ROUTING                    │
│                                                   │
│ domain · architecture · ADR · contracts · tests   │
└───────────────────────┬───────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────┐
│               MODULAR CODEBASE                    │
│                                                   │
│ explicit boundaries · explicit ownership          │
└───────────────────────┬───────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────┐
│                MACHINE GUARDS                     │
│                                                   │
│ compiler · tests · lint · structural checks       │
└───────────────────────┬───────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────┐
│                 EVALUATION                        │
│                                                   │
│ deterministic checks + evaluator                  │
└───────────────────────┬───────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────┐
│                  EVIDENCE                         │
│                                                   │
│ test result · diff · logs · screenshots           │
└───────────────────────┬───────────────────────────┘
                        ↓
                       PR
```

---

## 28. Buenas prácticas para Agent-Friendly Repository Architecture

1. **Hacer del repositorio la fuente de conocimiento**: decisiones importantes deben sobrevivir sesiones y modelos.
2. **`AGENTS.md` corto**: usarlo como policy + navegación.
3. **Progressive disclosure**: cargar únicamente conocimiento relevante.
4. **Context index**: introducir `topic → source`.
5. **Jerarquía de autoridad explícita**: clarificar qué fuente manda.
6. **Convertir invariantes en código**: script, lint, test o CI cuando sea posible.
7. **Arquitectura verificable**: usar structural tests.
8. **Interfaces y boundaries explícitos**.
9. **Tests fáciles de descubrir y ejecutar**.
10. **Feedback rápido**.
11. **Generated facts**: generar schemas, mapas e inventories.
12. **Documentation freshness**.
13. **Docs versionadas junto al código**.
14. **Specs y planes como artifacts**.
15. **Nested instructions sólo cuando aportan scope real**.
16. **Git hygiene**.
17. **Sandbox by default**.
18. **Un solo writer por superficie**.
19. **Evals del propio harness**.
20. **Tratar el repositorio agent-friendly como software**.

---

## 29. Qué NO haría

Evitaría este diseño:

```text
AGENTS.md 1200 líneas

docs/
  400 archivos sin índice

architecture.md
  desactualizado

tests
  difíciles de ejecutar

scripts
  no documentados

utils/
common/
helpers/
misc/
  todos con lógica crítica
```

Aunque tenga más documentación, no es agent-friendly.

El agente tiene más información pero menos **navegabilidad**.

---

## 30. Maturity model propuesto

### Nivel 0 — Repository tradicional

```text
README
code
tests
```

El agente explora todo.

### Nivel 1 — Agent-aware

```text
AGENTS.md
build/test commands
```

### Nivel 2 — Context-aware

```text
AGENTS
+
context router
+
structured docs
+
ADRs
```

### Nivel 3 — Agent-friendly

```text
modular architecture
+
structural checks
+
deterministic validation
+
generated knowledge
```

### Nivel 4 — Agent-first

```text
SDD
+
evals
+
automated context routing
+
agent-specific skills
+
doc gardening
+
parallel agent boundaries
+
agent orchestration
```

Para repositorios universitarios o empresariales convencionales, conviene apuntar primero al **Nivel 3**.

---

## 31. Cómo se conecta con las investigaciones anteriores

```text
                      SDD
                       │
              define la intención
                       ↓
              Agent-Friendly Repo
                       │
             organiza conocimiento
                       ↓
               Context Engineering
                       │
             selecciona información
                       ↓
                Harness Engineering
                       │
      tools · permissions · feedback · policy
                       ↓
                  Agent Graph
                       │
              controla ejecución
                       ↓
             Generator / Evaluator
                       │
              separa roles
                       ↓
                     Evals
                       │
           mide si todo funciona
```

No son técnicas aisladas.

Cada una cubre una capa distinta del mismo sistema.

---

## 32. Aplicación a DonaTrack

El cambio de:

```text
AGENTS contiene todo
```

a:

```text
AGENTS
   ↓
context-index
   ↓
docs específicas
   ↓
scripts/checks
```

es precisamente una transición de:

```text
prompt-centric repository
```

a:

```text
agent-friendly repository
```

La incorporación de checks automáticos para links, ADRs, contexto y drift lo lleva un paso más allá: empieza a convertir la **knowledge architecture en algo verificable**.

Conceptualmente, eso ya no es solamente un “refactor de AGENTS.md”.

Es la construcción de un **repository harness**.

---

## 33. Qué investigaría a continuación

Cuatro temas especialmente valiosos:

```text
Repository Evals
```

Medir si la arquitectura del repo realmente ayuda a distintos agentes.

```text
Agent Skills architecture
```

Separar conocimiento permanente de procedimientos bajo demanda.

```text
Generated Repository Knowledge
```

Generar automáticamente context maps, dependency graphs y schemas.

```text
Repository Knowledge Graphs / semantic context routing
```

Que el agente deje de buscar documentación únicamente mediante links y pueda recuperar contexto semánticamente según la tarea.

---

## 34. Fuentes principales recomendadas

### OpenAI — Harness Engineering

https://openai.com/index/harness-engineering/

Especialmente:

```text
repository knowledge as system of record
AGENTS as map
progressive disclosure
mechanical architecture enforcement
documentation gardening
```

### AGENTS.md specification

https://agents.md/

### Anthropic — Claude Code Best Practices

https://www.anthropic.com/engineering/claude-code-best-practices

### Anthropic — Harness Design

https://www.anthropic.com/engineering/harness-design-long-running-apps

### GitHub — Repository instructions

https://docs.github.com/en/copilot/how-tos/copilot-on-github/customize-copilot/add-custom-instructions/add-repository-instructions

### Investigaciones académicas 2026

**Configuration Smells in AGENTS.md Files**  
https://arxiv.org/abs/2606.15828

**Do Context Files Help Coding Agents?**  
https://arxiv.org/abs/2607.27250

**Probe-and-Refine Tuning of Repository Guidance**  
https://arxiv.org/abs/2606.20512

**The Working Set of a Coding Agent: Coherence Debt in Repository-Scale Tasks**  
https://arxiv.org/abs/2608.16630

---

## 35. Conclusión

La idea clave de *Agent-Friendly Repository Architecture* no es:

> “agregar un AGENTS.md”.

Es diseñar un repositorio donde una sesión nueva pueda:

```text
orientarse
↓
encontrar contexto
↓
comprender límites
↓
modificar una superficie pequeña
↓
verificar su trabajo
↓
producir evidencia
↓
dejar conocimiento durable
```

sin depender de que un humano le explique el sistema entero.

La arquitectura recomendada hoy es:

```text
small AGENTS
+
structured repository knowledge
+
context routing
+
clear module boundaries
+
specs / ADRs as artifacts
+
machine-enforced architecture
+
fast deterministic feedback
+
generated knowledge
+
documentation freshness checks
+
agent-specific skills on demand
+
repository evals
```

Y hay una idea que resume todo:

> **Un repositorio verdaderamente agent-friendly no intenta explicarle absolutamente todo al agente. Hace que la información correcta sea fácil de encontrar y que hacer algo incorrecto sea difícil.**

Para tu línea de investigación, este tema encaja como pieza central: **SDD define qué construir; Agent-Friendly Repository Architecture hace que el entorno sea comprensible; Context Engineering decide qué mostrar; Harness Engineering controla cómo trabajar; Agent Graphs orquestan el proceso; Evals comprueban si todo el sistema realmente mejora.**
