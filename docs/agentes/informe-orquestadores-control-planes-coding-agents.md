# Informe de investigación — Orquestadores y Control Planes para Coding Agents

## 1. Por qué este tema importa ahora

Después de **Spec Driven Development**, **Harness Engineering** y **Agent Graphs**, el siguiente problema aparece cuando dejamos de pensar en *un agente resolviendo una tarea* y empezamos a pensar en:

```text
20 issues
+
8 coding agents
+
varios repositorios
+
PRs
+
CI
+
reintentos
+
workspaces
+
aprobaciones humanas
+
costos
+
logs
```

Ahí el problema ya no es solamente la inteligencia del agente.

El problema pasa a ser:

> **¿quién decide qué trabajo ejecuta cada agente, dónde lo ejecuta, con qué permisos, cuánto puede hacer en paralelo, qué sucede si falla y cómo observa el humano todo el sistema?**

Ese es el territorio de los **orquestadores y control planes para coding agents**.

La evolución que se empieza a observar puede resumirse así:

```text
2024–2025
humano → coding agent → código

2025–2026
humano → harness → coding agent → código verificado

2026
humano
   ↓
control plane / backlog
   ↓
orchestrator
   ↓
┌──────────┬──────────┬──────────┐
│ Agent A  │ Agent B  │ Agent C  │
└──────────┴──────────┴──────────┘
   ↓
PRs + evidence + CI
   ↓
humano revisa resultados
```

La publicación más clara que materializa este cambio es precisamente **OpenAI Symphony**, que pediste explícitamente incluir.

---

## 2. Primero: tres conceptos que conviene separar

Los términos **harness**, **orchestrator** y **control plane** empiezan a mezclarse bastante, pero no son exactamente lo mismo.

### Harness

Es lo que rodea **a una ejecución del agente**.

Define, entre otras cosas:

```text
contexto
tools
permisos
tests
feedback
memoria
evidencia
restricciones
```

### Orchestrator

Coordina **múltiples ejecuciones**.

Responde preguntas como:

```text
¿Qué tarea hay que correr ahora?
¿Hay capacidad disponible?
¿Ya hay otro agente trabajando en esto?
¿En qué workspace?
¿Hay que reintentar?
¿El issue sigue siendo válido?
¿La tarea terminó?
```

### Control plane

Es un concepto un nivel más arriba.

Representa el **estado deseado y la política global del sistema**.

Ejemplo:

```text
Issue tracker

TODO
IN PROGRESS
REVIEW
DONE
```

El control plane declara algo como:

```text
ISSUE-42 está abierto
priority = high
owner = agent
state = ready
```

El orquestador observa ese estado y hace que la realidad converja hacia él:

```text
crear workspace
↓
ejecutar agente
↓
crear PR
↓
esperar CI
↓
actualizar estado
```

Una forma útil de recordarlo:

```text
CONTROL PLANE
¿Qué debería estar ocurriendo?

ORCHESTRATOR
¿Qué tengo que ejecutar para que ocurra?

HARNESS
¿Cómo debe trabajar el agente?

AGENT
Hace el trabajo.
```

---

## 3. La arquitectura completa

Si unimos los conceptos:

```text
                      HUMANO
                         │
                         ▼
                 ┌──────────────┐
                 │ CONTROL PLANE│
                 │ issues/specs │
                 └──────┬───────┘
                        │
                        ▼
                 ┌──────────────┐
                 │ ORCHESTRATOR │
                 │ scheduler    │
                 │ lifecycle    │
                 │ retries      │
                 └──────┬───────┘
                        │
          ┌─────────────┼─────────────┐
          ▼             ▼             ▼
      Workspace A   Workspace B   Workspace C
          │             │             │
          ▼             ▼             ▼
       Harness        Harness        Harness
          │             │             │
          ▼             ▼             ▼
       Agent A         Agent B        Agent C
          │             │             │
          └─────────────┼─────────────┘
                        ▼
                 deterministic
                    checks
                        ↓
                  evaluator / CI
                        ↓
                  evidence / PR
                        ↓
                 CONTROL PLANE
```

---

## 4. ¿Por qué hacen falta?

Hay un límite muy claro en el modelo:

```text
developer
   ↓
abre Claude Code
   ↓
abre Codex
   ↓
abre otro Codex
   ↓
abre otro terminal
```

Al principio parece productividad.

Después aparece esto:

```text
¿cuál había terminado?
¿cuál estaba esperando?
¿qué issue era este?
¿qué branch estaba usando?
¿qué agente había fallado?
¿cuál PR correspondía?
```

Y el cuello de botella vuelve a ser humano.

OpenAI cuenta ese problema en Symphony: muchas personas podían supervisar aproximadamente **3–5 sesiones simultáneas** antes de que el context switching empezara a erosionar la productividad.

La conclusión importante es:

> El límite de escalabilidad dejó de ser únicamente cuántos tokens puede generar el modelo y pasó a ser cuántos agentes puede coordinar un humano.

---

## 5. El cambio de unidad de trabajo

Una de las ideas más importantes de Symphony es cambiar la unidad de trabajo.

El paradigma interactivo trata la unidad como:

```text
session
```

Por ejemplo:

```text
"abro un Codex"
"le doy esta tarea"
"miro qué hace"
"lo corrijo"
```

Symphony cambia la unidad a:

```text
work item
```

Por ejemplo:

```text
ISSUE-142
```

El humano ya no gestiona:

```text
Codex session #AB182
```

Gestiona:

```text
ISSUE-142
```

El sistema se ocupa de convertirlo en:

```text
workspace
+
agent session
+
branch
+
PR
+
CI
+
evidence
```

---

## 6. Symphony — la publicación que no conviene dejar afuera

OpenAI publicó en 2026:

**An open-source spec for Codex orchestration: Symphony**

Es una de las referencias más directas sobre **control planes específicamente para coding agents**.

Fuente principal:

- OpenAI — An open-source spec for Codex orchestration: Symphony  
  https://openai.com/index/open-source-codex-orchestration-symphony/

La idea base es:

```text
Linear issue
      │
      ▼
 Symphony
      │
      ▼
dedicated workspace
      │
      ▼
   Codex
      │
      ▼
     PR
      │
      ▼
Human review
```

Cada tarea abierta puede recibir un agente y los agentes pueden seguir trabajando sin que un desarrollador tenga que mantener abierta una terminal.

---

## 7. Symphony no es “un megaagente”

Symphony no intenta ser:

```text
un LLM jefe
que piensa
cómo distribuir
a otros LLMs
```

El scheduler es fundamentalmente **software tradicional**.

La spec define un orquestador responsable de:

- polling;
- elegibilidad de issues;
- concurrencia;
- retries;
- reconciliation;
- lifecycle de los workers.

Es decir:

```text
"¿hay capacidad?"
```

no necesita LLM.

```text
"¿el issue sigue en estado activo?"
```

no necesita LLM.

```text
"¿cuánto esperar antes de reintentar?"
```

no necesita LLM.

```text
"¿cómo implementar esta feature?"
```

sí puede requerir al agente.

---

## 8. Arquitectura interna de Symphony

La especificación enumera aproximadamente estas piezas:

```text
Workflow Loader
        ↓
Config Layer
        ↓
Issue Tracker Client
        ↓
Orchestrator
        ↓
Workspace Manager
        ↓
Agent Runner
        ↓
Status Surface
        ↓
Logging
```

Separadas conceptualmente:

### Policy layer

```text
WORKFLOW.md
```

Define cómo trabaja el equipo.

### Configuration layer

Convierte configuración declarativa en parámetros tipados.

### Coordination layer

Gestiona:

```text
polling
eligibility
concurrency
retries
reconciliation
```

### Execution layer

Gestiona:

```text
workspace
agent subprocess
```

### Integration layer

Conecta con:

```text
Linear
```

### Observability layer

Expone:

```text
logs
status
metrics
```

---

## 9. El issue tracker como control plane

Symphony utiliza **Linear** como referencia.

Pero conceptualmente podría ser:

```text
GitHub Issues
Jira
Linear
Azure DevOps
Shortcut
un backlog propio
```

Lo fundamental es que exista un **source of truth externo a la sesión del agente**.

Por ejemplo:

```text
ISSUE-32
state: Todo

ISSUE-33
state: In Progress

ISSUE-34
state: Human Review

ISSUE-35
state: Done
```

Eso permite que:

```text
humano
agente
orchestrator
CI
```

compartan la misma visión del trabajo.

---

## 10. Desired state vs runtime state

Supongamos:

```text
Linear:
ISSUE-32 = In Progress
```

Ese es el **estado declarado**.

Mientras tanto:

```text
Orchestrator:
worker = running
workspace = /work/ISSUE-32
agent_session = abc123
attempt = 2
last_event = ...
```

Ese es el **runtime state**.

El orchestrator tiene que reconciliarlos.

Por ejemplo:

```text
Issue: cancelled
Runtime: agent running
```

Entonces:

```text
reconcile()
   ↓
stop worker
```

---

## 11. El reconciliation loop

Esta idea viene del mundo de sistemas distribuidos.

En vez de:

```text
start agent
wait
hope
```

hacés:

```text
while running:
    desired = read_tracker()
    actual = inspect_workers()

    reconcile(desired, actual)
```

Esto vuelve al sistema mucho más resistente.

Ejemplos:

```text
agent cayó
→ retry

issue cancelado
→ stop

agent quedó colgado
→ terminate + retry

worker terminado
pero issue sigue activo
→ continuation

issue terminado
→ cleanup workspace
```

---

## 12. Concurrencia limitada

Uno de los errores sería pensar:

```text
100 issues
=
100 agents
```

No necesariamente.

Hay que controlar:

```text
CPU
RAM
API rate limits
tokens
dinero
git conflicts
CI capacity
review capacity humana
```

Por eso un orchestrator serio necesita **bounded concurrency**.

Ejemplo:

```text
50 issues READY
      ↓
max_concurrency = 5
      ↓
5 workers
```

Cuando uno termina:

```text
slot++
↓
dispatch siguiente
```

---

## 13. El recurso realmente escaso puede pasar a ser review

Inicialmente:

```text
bottleneck = coding
```

Con agentes:

```text
bottleneck = agent supervision
```

Con un orchestrator:

```text
bottleneck = human review
```

Si multiplicás los PR:

```text
no necesariamente
multiplicaste
la capacidad de revisión humana.
```

Esto implica que el control plane debería gestionar también:

```text
review queue
risk ranking
priority
evidence quality
```

---

## 14. Workspaces aislados

No conviene:

```text
Agent A ─┐
Agent B ─┼→ misma checkout
Agent C ─┘
```

Preferible:

```text
ISSUE-12
→ workspace-12

ISSUE-13
→ workspace-13

ISSUE-14
→ workspace-14
```

Esto reduce:

```text
colisiones
working tree sucio
branch contamination
conflictos accidentales
```

---

## 15. WORKFLOW.md: proceso de ingeniería como código

Una pieza especialmente interesante de Symphony es:

```text
WORKFLOW.md
```

El comportamiento del agente se define en el repositorio.

Puede contener:

```text
YAML front matter
+
prompt/workflow
```

y se versiona junto al código.

Conceptualmente:

```yaml
tracker:
  active_states:
    - Todo
    - In Progress

agent:
  max_concurrent_agents: 4
```

más:

```markdown
Implementá el issue asignado.

Antes de completar:
- ejecutá tests;
- abrí PR;
- respondé feedback;
- adjuntá evidencia;
- mové el ticket a Human Review.
```

La idea de fondo es:

> **el proceso agentic pasa de ser conocimiento tácito a ser un artefacto versionado.**

---

## 16. Policy as code para agentes

El control plane abre la posibilidad de convertir:

```text
team policy
↓
machine-enforceable policy
```

Por ejemplo:

```text
migration DB
→ requires human approval

docs-only
→ autonomous merge allowed

security change
→ security evaluator required

PR > 500 lines
→ human review mandatory

CI red
→ cannot transition to done
```

Esto es mucho más fuerte que poner en un prompt:

```text
"tené cuidado con las migraciones."
```

---

## 17. ¿Dónde entran los Agent Graphs?

No compiten con los orchestrators.

Son distintos niveles.

```text
CONTROL PLANE
Linear
   ↓
ORCHESTRATOR
Symphony
   ↓
ISSUE-42
   ↓
AGENT GRAPH
Inspect
 ↓
Plan
 ↓
Implement
 ↓
Test
 ↓
Review
```

Por eso:

> **Control plane coordina work items.  
> Agent Graph coordina pasos dentro de un work item.**

---

## 18. Control plane vs Agent Graph

| | Agent Graph | Control Plane |
|---|---|---|
| Unidad principal | Step / state | Work item |
| Scope | Una ejecución | Muchas ejecuciones |
| Decide | Qué paso sigue | Qué trabajo corre |
| Estado | Workflow state | Fleet/work state |
| Ejemplo | LangGraph | Linear + Symphony |
| Retry | Nodo/workflow | Worker/task |
| Concurrency | nodos | agentes/tasks |
| Human gate | dentro del workflow | governance global |

---

## 19. Control plane vs Harness

Symphony puede decidir ejecutar:

```text
ISSUE-104
```

El harness del coding agent decide:

```text
qué AGENTS.md cargar
qué tools exponer
qué shell usar
qué tests ejecutar
qué permisos tiene
```

La progresión correcta es:

```text
primero:
hacer confiable un agente

después:
escalar cuántos agentes ejecutás
```

No al revés.

---

## 20. Qué NO debería hacer un control plane

No debería convertirse en:

- un workflow engine general;
- un distributed scheduler universal;
- una UI completa;
- un sistema que prescriba toda la lógica de tickets y PRs;
- un reemplazo de todas las políticas de sandbox y seguridad.

Un control plane útil debería seguir siendo acotado.

---

## 21. Cuándo conviene incorporar un orquestador

| Síntoma | Orchestrator |
|---|---:|
| 1 agente ocasional | ❌ |
| 2–3 sesiones simples | probablemente no |
| varias sesiones asíncronas | ✅ |
| personas perdiendo track de sesiones | ✅ |
| backlog repetible | ✅ |
| trabajo continuo desde issues | ✅ |
| retries manuales | ✅ |
| workspaces difíciles de administrar | ✅ |
| muchos PR producidos por agentes | ✅ |
| necesidad de auditoría | ✅ |
| múltiples equipos/repos | ✅✅ |

---

## 22. Niveles de madurez recomendados

### Nivel 0 — Coding agent interactivo

```text
Human
 ↓
Claude Code / Codex
```

### Nivel 1 — Harness serio

```text
Task
 ↓
Agent
 ↓
Tests
 ↓
Evidence
```

### Nivel 2 — Async task execution

```text
Issue
 ↓
Agent
 ↓
PR
```

### Nivel 3 — Orchestrator

```text
Backlog
 ↓
scheduler
 ↓
multiple agents
```

Aquí aparece Symphony.

### Nivel 4 — Control plane gobernado

```text
RBAC
budgets
risk tiers
approvals
audit
rate limits
policy
```

### Nivel 5 — Agentic engineering platform

```text
issues
specs
context routing
agents
graphs
evaluators
CI
deploy
observability
costs
governance
```

---

## 23. Arquitectura recomendada hoy

```text
                    GitHub / Linear
                         │
                    CONTROL PLANE
                         │
                         ▼
                  ┌──────────────┐
                  │ Orchestrator │
                  └──────┬───────┘
                         │
              risk + eligibility
                         │
            ┌────────────┼────────────┐
            ▼            ▼            ▼
         Agent A      Agent B      Agent C
            │            │            │
        worktree      worktree      worktree
            │            │            │
            ▼            ▼            ▼
          HARNESS      HARNESS      HARNESS
            │            │            │
            ▼            ▼            ▼
        test/eval      test/eval     test/eval
            │            │            │
            └────────────┼────────────┘
                         ▼
                        CI
                         │
                  risk classifier
                         │
              ┌──────────┴──────────┐
              ▼                     ▼
           LOW RISK              HIGH RISK
              │                     │
          auto handoff          human review
              │                     │
              └──────────┬──────────┘
                         ▼
                       merge
```

---

## 24. El control plane debería ser determinista en todo lo posible

No usaría un LLM para decidir:

```text
si hay slots disponibles
```

o:

```text
si una tarea está cancelada
```

o:

```text
si tests_passed == true
```

Esas decisiones deberían ser software normal.

Reservaría el agente para:

```text
interpretar requisitos
explorar código
planificar implementación
resolver bugs
crear cambios
evaluar aspectos ambiguos
```

---

## 25. Reconciliation > pipelines frágiles

Evitar:

```text
step1()
step2()
step3()
```

donde si algo rompe queda un sistema inconsistente.

Preferir:

```text
desired state
       ↕
reconciliation
       ↕
actual state
```

Esta propiedad mejora la robustez ante:

```text
network failures
API limits
agent crashes
human changes
machine restart
stalls
```

---

## 26. Persistencia y durable execution

Para trabajos largos, el sistema debería poder:

```text
pause
restart
resume
```

sin empezar todo nuevamente.

Un sistema de agentes en producción no debería depender de que un proceso permanezca vivo durante horas.

---

## 27. Retries con política explícita

No:

```text
while failed:
    retry()
```

Necesitás distinguir:

```text
transient failure
fatal failure
agent failure
CI failure
blocked dependency
human required
```

Ejemplo:

```text
attempt 1
10 sec

attempt 2
20 sec

attempt 3
40 sec

...

cap
5 min
```

---

## 28. Stall detection

Un agente puede:

```text
seguir vivo
```

pero no estar:

```text
haciendo progreso.
```

Son cosas distintas.

Por eso un control plane serio necesita:

```text
last_progress_timestamp
```

Si excede un timeout:

```text
terminate
↓
retry / escalate
```

---

## 29. Human gates por riesgo, no en todas partes

Human-in-the-loop no debería significar:

```text
agent asks permission
every 30 seconds
```

Mejor:

```text
read repo
→ autonomous

modify tests
→ autonomous

run tests
→ autonomous

drop DB
→ approval

production deploy
→ approval

modify auth policy
→ approval
```

---

## 30. Capability-based permissions

Un control plane debería permitir algo como:

```text
SCOUT
read repository
search docs

CODER
read/write workspace
git
tests

REVIEWER
read-only
run tests

DEPLOYER
deployment tools
requires approval
```

No:

```text
todos los agentes
=
todas las credenciales
```

---

## 31. Secret isolation

Principio recomendado:

```text
agent
   ↓
tool interface
   ↓
privileged service
   ↓
secret
```

en lugar de:

```text
agent
   ↓
env
SECRET_TOKEN=...
```

---

## 32. Observabilidad

Con un agente podés mirar la terminal.

Con 30 agentes ya no.

Necesitás métricas como:

```text
task_id
agent
workspace
state
duration
tokens
cost
attempts
tool calls
last activity
PR
CI status
review findings
```

---

## 33. Métricas que realmente importan

No medir solamente:

```text
tokens generated
```

o:

```text
PRs created
```

Medir:

```text
landed PR rate
first-pass success
time-to-review
time-to-merge
retries/task
human interventions
CI failure rate
rollback rate
agent cost/task
review burden
bugs escaped
scope expansion
```

Y una métrica especialmente interesante:

```text
human attention / landed task
```

---

## 34. Backpressure

Supongamos:

```text
agents produce:
30 PR/día

humans review:
8 PR/día
```

El sistema está técnicamente funcionando.

Pero organizacionalmente está roto.

Necesitás:

```text
backpressure
```

Por ejemplo:

```text
review_queue > 20
↓
reduce concurrency
```

o:

```text
high-risk PRs waiting
↓
don't dispatch more high-risk work
```

---

## 35. Prioridad y scheduling

No todos los issues deberían ser tratados igual.

Podés ordenar por:

```text
priority
age
dependency
risk
estimated cost
human availability
```

En el futuro es razonable esperar schedulers que consideren:

```text
task complexity
×
model capability
×
budget
×
latency
×
risk
```

---

## 36. Model routing también puede vivir en el control plane

Ejemplo:

```text
docs
→ cheap model

simple bug
→ medium coding model

architecture
→ strong reasoning model

security review
→ specialist evaluator
```

Un control plane puede hacer ese routing a escala.

---

## 37. Multi-agent no significa múltiples writers

Un error peligroso:

```text
Issue
 ↓
5 agents
 ↓
todos escriben el mismo repo
```

Preferible:

```text
Scout A ─┐
Scout B ─┼→ analysis
Scout C ─┘
          ↓
       Writer
          ↓
       Reviewer
```

o workspaces completamente independientes.

---

## 38. Relación con Generator / Evaluator

El control plane puede convertir el patrón:

```text
Generator
 ↕
Evaluator
```

en una política organizacional.

Ejemplo:

```text
Issue
 ↓
Generator
 ↓
CI
 ↓
Evaluator
 ↓
PASS
 ↓
Human Review
```

---

## 39. El evaluator no tiene que ser otro modelo siempre

Puede ser:

```text
compiler
tests
lint
typecheck
coverage
security scanner
browser automation
contract tests
```

y recién después:

```text
LLM evaluator
```

Orden recomendado:

```text
cheap + deterministic
        ↓
expensive + probabilistic
```

---

## 40. Evals del control plane

Una vez que existe un orchestrator, también debería ser evaluado.

No solamente:

```text
¿el agente programó bien?
```

Sino:

```text
¿despachó dos veces el mismo issue?
¿respetó concurrencia?
¿canceló workers inválidos?
¿reintentó correctamente?
¿preservó el workspace?
¿aplicó human gate?
¿detuvo un stalled worker?
```

Son **system evals**, no LLM evals.

---

## 41. Paper: A Deterministic Control Plane for LLM Coding Agents

Una publicación académica reciente alineada con este tema es:

**A Deterministic Control Plane for LLM Coding Agents**

Fuente:

- arXiv  
  https://arxiv.org/abs/2606.26924

El trabajo propone gobernar la capa de configuración y reglas alrededor de agentes mediante mecanismos deterministas como:

```text
content addressing
lockfiles
permission tiers
audit logs
phase state machine
traceability
prompt drift detection
```

Debe tomarse como una propuesta de investigación, no como evidencia definitiva de productividad.

---

## 42. Orchestrators que vale la pena estudiar

### Symphony

Referencia principal para:

```text
issue tracker
→ always-on coding agents
```

- https://openai.com/index/open-source-codex-orchestration-symphony/
- https://github.com/openai/symphony

### GitHub coding agents

GitHub ya actúa parcialmente como control plane:

```text
issue
→ agent
→ branch/PR
→ human review
```

- https://docs.github.com/en/copilot/concepts/agents/about-third-party-coding-agents

### LangGraph / Agent runtimes

Útil para:

```text
stateful workflows
durable execution
human gates
graphs
```

- https://docs.langchain.com/oss/python/langgraph/overview

### Temporal / durable runtimes

Interesantes cuando el problema principal es:

```text
long-running workflows
recovery
external events
durability
```

---

## 43. Symphony no debería entenderse como “instalalo y listo”

Symphony es más interesante como **spec de arquitectura** que como herramienta cerrada.

El repositorio permite:

```text
1. implementar Symphony desde SPEC.md

2. utilizar la implementación experimental
```

Eso lo vuelve particularmente útil para estudiar:

```text
dispatch
reconciliation
retry
workspace lifecycle
workflow contract
observability
```

---

## 44. Symphony y Spec Driven Development

Symphony coloca en el centro:

```text
SPEC.md
```

Conceptualmente:

```text
spec
↓
implementations
```

en vez de:

```text
implementation
↓
documentation
```

Eso conecta directamente con Spec Driven Development.

---

## 45. Buenas prácticas para orquestadores y control planes de coding agents

1. **No escalar agentes antes de tener un buen harness.**

2. **Usar un source of truth externo a la sesión.** Issues, specs o tickets deberían representar trabajo, estado y prioridad.

3. **Mantener scheduling y governance deterministas.**

4. **Un workspace aislado por work item/writer.**

5. **Bounded concurrency.**

6. **Implementar reconciliation loops.**

7. **Retries limitados y clasificados.**

8. **Stall detection.**

9. **Human gates basados en riesgo.**

10. **Least privilege y secret isolation.**

11. **Workflow y policies versionados en el repositorio.**

12. **Evidence-first completion.**

13. **Backpressure sobre el backlog agentic.**

14. **Observabilidad por task y por agent run.**

15. **Separar generación y evaluación en trabajos de riesgo.**

16. **Versionar la política de orquestación.**

17. **Fail closed para condiciones importantes.**

18. **No automatizar el merge desde el día uno.**

19. **Evals del orchestrator, no sólo de los modelos.**

20. **Agregar complejidad únicamente cuando resuelve una falla observada.**

---

## 46. Anti-pattern: el “manager agent”

No pondría como primera opción:

```text
Manager LLM
   ↓
decide todo
   ↓
Worker LLMs
```

Preferiría:

```text
deterministic orchestrator
       ↓
agent where reasoning is needed
```

La inteligencia debería estar **dentro de boundaries**, no gobernando necesariamente toda la infraestructura.

---

## 47. Anti-pattern: auto-scaling sin control

Algo como:

```text
100 issues
 ↓
100 agents
```

puede generar:

```text
100 PR
100 CI pipelines
massive token cost
conflicts
review overload
```

Escalar agents sin backpressure puede reducir productividad total.

---

## 48. Anti-pattern: “Done porque el agente lo dijo”

Nunca:

```text
agent:
"Todo está funcionando."

orchestrator:
DONE
```

Preferible:

```text
agent completed
      ↓
test artifacts
      ↓
CI green
      ↓
review/evaluator
      ↓
transition allowed
```

---

## 49. Anti-pattern: usar el issue tracker como base de datos del runtime

El tracker debería guardar:

```text
business/work state
```

El scheduler puede necesitar además:

```text
attempt
last heartbeat
workspace
agent thread
retry_at
token use
```

Control plane y runtime state pueden estar relacionados pero no deberían mezclarse sin necesidad.

---

## 50. Anti-pattern: context duplication

No conviene que cada agente cargue:

```text
todo el repo
todos los ADR
todas las policies
todo el backlog
```

Un orchestrator es un lugar ideal para aplicar **context routing**.

Por ejemplo:

```text
ISSUE
 ↓
classify domain
 ↓
context manifest
 ↓
agent
```

---

## 51. Qué cambia en el rol del desarrollador

Con coding agents interactivos:

```text
developer
=
coder + agent supervisor
```

Con un control plane:

```text
developer
=
specifier
architect
reviewer
policy designer
system operator
```

Cuanto más código producen los agentes, más importante se vuelve saber:

```text
qué debe existir
qué no debe existir
cómo validarlo
qué arquitectura preservar
qué riesgos aceptar
```

---

## 52. Cómo lo probaría en un proyecto real

### Fase A

Elegir:

```text
5 issues independientes
```

y correrlos manualmente con Claude Code/Codex.

Medir:

```text
human time
task time
tokens
tests
PR quality
interventions
```

### Fase B

Crear un mini control plane:

```text
GitHub Issues
↓
simple orchestrator
↓
1 agent max
```

Automatizar solamente:

```text
dispatch
workspace
agent start
PR
```

### Fase C

Agregar:

```text
max_concurrency = 2
retries
observability
```

### Fase D

Agregar:

```text
CI gate
evaluator
human review
```

### Fase E

Recién entonces probar:

```text
3–5 concurrent agents
```

y comparar.

---

## 53. Experimento recomendado para un repositorio real

Antes de usar el control plane sobre features principales, conviene probarlo con tareas de mantenimiento como:

```text
actualizar link roto
revisar temporal drift
resolver warning de ADR
corregir stale context
agregar test faltante
```

Ciclo:

```text
Issue
 ↓
Orchestrator
 ↓
workspace aislado
 ↓
coding agent
 ↓
checks
 ↓
tests
 ↓
PR
 ↓
human review
```

---

## 54. Después sí: features

Una vez comprobado:

```text
dispatch fiable
context correcto
evidence correcta
retry correcto
PR limpio
```

podrías subir a:

```text
feature issues
```

con clasificación de riesgo:

```text
LOW
→ autonomous until PR

MEDIUM
→ plan approval

HIGH
→ spec + ADR + human approval
```

---

## 55. Para equipos de varios integrantes

El control plane no debería convertirse en:

```text
"los agentes trabajan solos"
```

sino en:

```text
"el equipo administra trabajo,
y los agentes son ejecutores."
```

Así evitás que cada integrante tenga:

```text
su Codex
su prompt
su branch
su forma de testear
```

y permitís que la política de trabajo sea compartida.

---

## 56. Cómo conectan todos estos temas

```text
INTENT
  │
  ▼
SPEC DRIVEN DEVELOPMENT
qué hay que construir
  │
  ▼
CONTEXT ENGINEERING
qué necesita saber el agente
  │
  ▼
HARNESS ENGINEERING
en qué entorno trabaja
  │
  ▼
AGENT GRAPH
qué workflow ejecuta una tarea
  │
  ▼
ORCHESTRATOR
qué tareas ejecutar y cuándo
  │
  ▼
CONTROL PLANE
qué estado/política gobierna el sistema
  │
  ▼
EVALS + OBSERVABILITY
cómo sabemos si todo funciona
```

No son alternativas.

Son **capas diferentes del mismo sistema de ingeniería agentic**.

---

## 57. Cuál es la mejor manera de utilizarlos

Recomendación sintetizada:

```text
NO:
Primero construir un gran orquestador.

SÍ:
1. repo legible para agentes
2. harness confiable
3. verification objetiva
4. issues/specs claros
5. async execution
6. isolated workspaces
7. simple orchestrator
8. reconciliation + retries
9. observability
10. concurrency limitada
11. risk-based human gates
12. recién después más autonomía
```

---

## 58. Dónde ubico Symphony en esa progresión

```text
AGENTS.md / context routing
           ↓
Harness Engineering
           ↓
Generator / Evaluator
           ↓
Evals
           ↓
       SYMPHONY
```

Porque Symphony no arregla un repositorio difícil para agentes.

**Escala un repositorio que ya es agent-friendly.**

---

## 59. Fuentes principales

### OpenAI — Symphony

**An open-source spec for Codex orchestration: Symphony**

https://openai.com/index/open-source-codex-orchestration-symphony/

Repositorio:

https://github.com/openai/symphony

SPEC:

https://github.com/openai/symphony/blob/main/SPEC.md

### OpenAI — Harness Engineering

https://openai.com/index/harness-engineering/

### Anthropic — Harness design for long-running application development

https://www.anthropic.com/engineering/harness-design-long-running-apps

### LangGraph

https://docs.langchain.com/oss/python/langgraph/overview

### GitHub coding agents

https://docs.github.com/en/copilot/concepts/agents/about-third-party-coding-agents

### Paper — A Deterministic Control Plane for LLM Coding Agents

https://arxiv.org/abs/2606.26924

---

## 60. Conclusión

La aparición de orquestadores como Symphony señala otro cambio de escala.

Primero preguntábamos:

```text
¿Cómo hago que el agente programe mejor?
```

Después:

```text
¿Cómo construyo un harness para que programe de forma confiable?
```

Ahora empieza a aparecer:

```text
¿Cómo coordino decenas de trabajos agentic
sin convertirme yo mismo en el scheduler?
```

La respuesta no parece ser simplemente:

```text
más agentes
```

sino:

```text
control plane
+
deterministic orchestration
+
isolated execution
+
good harness
+
objective verification
+
observability
+
human governance
```

Symphony aporta una idea especialmente importante:

> **dejar de administrar sesiones de agentes y empezar a administrar trabajo.**

Ese cambio —de `agent session` a `work item`— probablemente sea una de las evoluciones más importantes de la ingeniería con coding agents durante 2026.

Además, encaja muy bien con SDD, Harness Engineering, Agent Graphs, Generator/Evaluator y Evals: cada uno resuelve una capa diferente del mismo sistema de desarrollo agentic.
