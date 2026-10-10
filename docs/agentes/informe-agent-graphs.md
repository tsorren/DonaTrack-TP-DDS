# Informe de investigación — Agent Graphs para desarrollo de software con IA

Investigación sobre Agent Graphs aplicada al desarrollo de software con agentes de IA, con foco en arquitectura, casos de uso, frameworks, buenas prácticas y relación con SDD, Harness Engineering, Generator/Evaluator y Evals.

---

## 1. Qué es un Agent Graph

La forma más útil de entenderlo es:

> Un Agent Graph es una arquitectura donde el comportamiento de uno o varios agentes se representa como un grafo explícito de pasos, estados y transiciones.

Un grafo típico tiene:

```text
           ┌──────────────┐
           │   START      │
           └──────┬───────┘
                  ↓
          ┌───────────────┐
          │   Planner     │
          └──────┬────────┘
                 ↓
          ┌───────────────┐
          │  Implementer  │
          └──────┬────────┘
                 ↓
          ┌───────────────┐
          │   Evaluator   │
          └──────┬────────┘
                 │
          ┌──────┴───────┐
          ↓              ↓
       PASS            FAIL
          ↓              │
        END       ┌──────┘
                  ↓
             Implementer
```

Los **nodos** pueden ser:

- una llamada a un LLM;
- un agente completo;
- código determinista;
- ejecución de tests;
- un script;
- una llamada a API;
- un retrieval;
- una revisión humana;
- otro grafo completo.

Las **aristas** representan qué puede ocurrir después.

Y suele existir un **estado compartido** que va evolucionando:

```text
State
├── requirement
├── plan
├── files_changed
├── test_results
├── review_findings
├── retry_count
└── final_status
```

LangGraph lo modela prácticamente como una máquina de estados: los nodos hacen trabajo, las aristas deciden qué ocurre después y el estado viaja por el grafo.

Microsoft Agent Framework utiliza una abstracción muy cercana: **executors + edges + state**, con branching, fan-out/fan-in, checkpoints y subworkflows.

---

## 2. Por qué aparecen ahora

Los Agent Graphs solucionan una limitación importante de los agentes puramente autónomos.

Un agente tradicional puede funcionar aproximadamente así:

```text
while not done:
    think()
    choose_tool()
    execute()
```

Eso proporciona mucha autonomía, pero el flujo queda fundamentalmente en manos del modelo.

Para tareas pequeñas está bien.

Para tareas como:

- refactors grandes;
- migraciones;
- implementación de features;
- generación + revisión;
- tareas de compliance;
- workflows empresariales;
- pipelines con humanos;
- trabajos que pueden durar horas;

se vuelve peligroso depender solamente del razonamiento del modelo.

La alternativa es separar:

```text
QUÉ decide el sistema
        ↓
estructura del graph

DÓNDE puede decidir libremente el modelo
        ↓
nodos agentic
```

Ésta es una de las ideas centrales:

> **Un Agent Graph no elimina la autonomía del agente. Decide dónde esa autonomía está permitida.**

---

## 3. Agent Graph ≠ workflow lineal

Podrías tener:

```text
A → B → C → D
```

pero eso es simplemente un pipeline.

Los Agent Graphs se vuelven especialmente útiles cuando aparecen:

### Branching

```text
           → BackendAgent
Planner →
           → FrontendAgent
```

### Fan-out

```text
                   → Security review
Implementation →  → Performance review
                   → Architecture review
```

### Fan-in

```text
Security ──────┐
Performance ───┼→ ReviewAggregator
Architecture ──┘
```

### Loops

```text
Implement
   ↓
Review
   ↓
FAIL ─────────→ Implement
```

### Human gates

```text
Agent
 ↓
Risk classifier
 ↓
high risk
 ↓
Human approval
 ↓
Execute
```

### Subgraphs

```text
Main Graph

Planner
  ↓
┌───────────────────────┐
│ Backend Subgraph      │
│                       │
│ plan → code → test    │
│          ↑      ↓     │
│          └─ retry ────┘
└───────────────────────┘
  ↓
Integration
```

Microsoft soporta explícitamente ejecución secuencial, concurrente, handoffs, group chat y otros patrones de coordinación.

---

## 4. Una aclaración especialmente importante: no suelen ser DAGs

En pipelines de datos estamos acostumbrados a:

> Directed Acyclic Graph.

Pero los agentes necesitan volver atrás.

Un coding agent puede necesitar:

```text
inspect repo
   ↓
implement
   ↓
run tests
   ↓
FAIL
   ↓
diagnose
   ↓
implement
```

Por eso un Agent Graph real suele contener **ciclos**.

Los agentes de producción necesitan loops para retry de tools, revisión, solicitud de información, validación y human-in-the-loop.

---

## 5. Qué diferencia hay entre Agent Graph y Multi-Agent System

No son lo mismo.

Puedes tener un Agent Graph con **un único agente**:

```text
                 ┌→ Search
Agent → Router ──┼→ Execute
                 └→ Ask Human
```

O múltiples agentes:

```text
Planner
  ↓
Backend Agent
  ↓
Reviewer Agent
```

Incluso puedes tener:

```text
Agent Graph
├── deterministic node
├── LLM node
├── Bash node
├── agent node
├── human node
└── another graph
```

La definición más precisa es:

> **Agent Graph es una arquitectura de orquestación. Multi-agent es una posible composición dentro de esa arquitectura.**

---

## 6. El patrón que más sentido tiene para desarrollo de software

Para software, conviene combinar SDD + Harness Engineering + Agent Graphs.

SDD plantea que el spec debería contener:

- Goal;
- Scope;
- Constraints;
- Validation;

y ser:

- incremental;
- pequeño;
- operacional;
- verificable.

El patrón Generator/Evaluator propone separar:

```text
Planner
   ↓
Generator
   ↕
Evaluator
```

usando un contrato verificable entre quien implementa y quien juzga.

Un Agent Graph convierte ese concepto en arquitectura ejecutable:

```text
SPEC
 │
 ▼
┌────────────┐
│  Planner   │
└─────┬──────┘
      │
      ▼
┌────────────┐
│ Implement  │
└─────┬──────┘
      │
      ▼
┌────────────┐
│ Run Tests  │
└─────┬──────┘
      │
      ├── FAIL ─────────────┐
      │                     │
      ▼                     │
┌────────────┐              │
│ Reviewer   │              │
└─────┬──────┘              │
      │                     │
      ├── FAIL ─────────────┘
      │
      ▼
┌────────────┐
│ Integration│
└─────┬──────┘
      ↓
     END
```

Conceptualmente:

```text
SDD
↓
define la intención

Agent Graph
↓
define el proceso

Harness
↓
define las restricciones y feedback

Agents
↓
ejecutan

Evals
↓
determinan si realmente funciona
```

---

## 7. Qué problema resuelven especialmente bien

### Control

Con un agente libre:

```text
"Implementá esta feature."
```

no sabés exactamente:

```text
qué inspeccionará
qué modificará
cuándo testeará
si revisará
cuándo se detendrá
```

Con un graph:

```text
inspect
↓
plan
↓
implement
↓
test
↓
review
↓
finish
```

la estructura está predefinida.

### Observabilidad

Cada nodo se vuelve una unidad observable:

```text
Node: implement
duration: 83s
tokens: 12,430
files_changed: 7

Node: test
result: FAIL
tests_failed: 2

Node: review
findings: 3
```

Esto importa porque evaluar solamente el resultado final puede ocultar una trayectoria defectuosa.

En sistemas agentic conviene evaluar a nivel de:

```text
run
trace
thread
```

porque una respuesta correcta puede haber sido obtenida mediante un camino inestable o riesgoso.

---

## 8. Durable Execution

Uno de los beneficios menos vistosos pero más importantes.

Supongamos:

```text
Plan
↓
Implement
↓
Test
↓
Review
↓
Deploy
```

y el proceso se cae después de `Test`.

Un sistema ingenuo empieza nuevamente:

```text
Plan
↓
Implement
↓
Test
...
```

Un runtime durable puede guardar:

```text
checkpoint = TestCompleted
```

y continuar:

```text
Review
↓
Deploy
```

Un sistema durable puede:

- persistir state;
- continuar después de fallos;
- no repetir pasos completados;
- pausar esperando humanos/eventos;
- ejecutar workflows durante días o semanas.

Para coding agents largos esto es muy relevante.

---

## 9. Human-in-the-loop como parte del grafo

En lugar de poner revisión humana solamente al final:

```text
AI AI AI AI AI AI
          ↓
        Human
```

podés introducir humanos justo donde aumenta el riesgo.

Por ejemplo:

```text
Code change
   ↓
Risk analysis
   │
   ├── low → continue
   │
   └── high
         ↓
    Human approval
```

O:

```text
Database migration
        ↓
   Human approval
        ↓
     Execute
```

La pausa y reanudación con interacción humana es una capacidad central en sistemas graph-based modernos.

---

## 10. Cómo modelaría un coding Agent Graph

Un graph serio para desarrollo podría ser:

```text
                           ┌──────────────┐
                           │ Requirement  │
                           └──────┬───────┘
                                  ↓
                           ┌──────────────┐
                           │ Context Load │
                           └──────┬───────┘
                                  ↓
                           ┌──────────────┐
                           │   Planner    │
                           └──────┬───────┘
                                  ↓
                          ┌───────────────┐
                          │ Risk Analysis │
                          └──────┬────────┘
                                 │
                 ┌───────────────┴────────────────┐
                 ↓                                ↓
             LOW RISK                         HIGH RISK
                 │                                │
                 │                           Human Gate
                 │                                │
                 └────────────────┬───────────────┘
                                  ↓
                           ┌──────────────┐
                           │ Implementer  │
                           └──────┬───────┘
                                  ↓
                           ┌──────────────┐
                           │ Static Check │
                           └──────┬───────┘
                                  ↓
                           ┌──────────────┐
                           │    Tests     │
                           └──────┬───────┘
                                  │
                   FAIL ──────────┘
                    │
                    ↓
               Implementer
                                  │ PASS
                                  ↓
                        ┌─────────────────┐
                        │ Review fan-out  │
                        └────────┬────────┘
                                 │
            ┌────────────────────┼──────────────────┐
            ↓                    ↓                  ↓
       Security             Architecture        Code Review
            │                    │                  │
            └────────────────────┼──────────────────┘
                                 ↓
                          Review Aggregator
                                 ↓
                         ┌───────┴───────┐
                         ↓               ↓
                       PASS             FAIL
                         ↓               │
                     Finalize ←──────────┘
```

Acá el LLM no controla todo.

El **runtime controla el proceso**.

---

## 11. Estado: una de las decisiones más importantes

Uno de los errores frecuentes es transformar el state del graph en:

```text
messages[]
```

y nada más.

Eso obliga a cada agente a reconstruir el mundo leyendo una conversación enorme.

Mucho mejor:

```json
{
  "task": "...",
  "requirements": [],
  "constraints": [],
  "plan": [],
  "changed_files": [],
  "test_results": {},
  "findings": [],
  "approved_decisions": [],
  "retry_count": 1,
  "status": "review"
}
```

Es decir:

> **estado semántico estructurado > conversación acumulativa gigante.**

Esto está muy alineado con Context Engineering.

---

## 12. Graph como Context Router

Otra aplicación especialmente útil para repositorios grandes.

En vez de:

```text
AGENTS.md
+ todos los ADR
+ todas las reglas
+ todos los módulos
+ toda la documentación
```

podrías hacer:

```text
Task
 ↓
Context Router
 │
 ├── payments → payments rules
 │
 ├── persistence → DB ADRs
 │
 ├── API → API conventions
 │
 └── security → security policies
 ↓
Coding Agent
```

En otras palabras:

```text
Graph
  decide
qué contexto
  entra
a qué agente
  y cuándo.
```

Ésta es una línea especialmente interesante para experimentar en repositorios grandes.

---

## 13. Agent Graph y Harness Engineering

Hay una relación muy fuerte.

Podría pensarse así:

```text
Harness Engineering
        │
        ├── instrucciones
        ├── tools
        ├── permisos
        ├── context
        ├── evaluadores
        ├── invariantes
        └── runtime
                  │
                  ▼
             Agent Graph
```

Un Agent Graph puede ser **una parte del harness**.

El patrón Generator/Evaluator separa:

```text
Generator
   ↓
Evaluator independiente
   ↓
decision
```

en vez de confiar en:

```text
Agent
 ↓
"revisá tu propio trabajo"
```

---

## 14. Graph estático vs Graph dinámico

Hay dos grandes estrategias.

### Static Graph

La topología está definida en código:

```text
Plan → Code → Test → Review
```

Ventajas:

- predecible;
- auditable;
- fácil de testear;
- fácil de razonar.

Ideal para ingeniería de software.

### Dynamic Graph

Un planner puede construir el grafo:

```text
User intent
    ↓
Planner
    ↓
create graph

    ┌→ Research
Plan┤
    ├→ Data Analysis
    ├→ Simulation
    └→ Report
```

Es potente, pero también introduce otro nivel de incertidumbre:

```text
LLM decide acciones
+
LLM decide arquitectura del workflow
```

Por eso no empezaría por ahí para coding agents.

---

## 15. Qué evitar: graphitis

Hay una tentación frecuente:

```text
PlannerAgent
ArchitectureAgent
BackendAgent
FrontendAgent
DatabaseAgent
SecurityAgent
TestAgent
ReviewAgent
DocumentationAgent
DevOpsAgent
```

Parece sofisticado.

Pero puede ser peor que:

```text
CodingAgent
   ↓
Evaluator
```

Más agentes generan:

- más tokens;
- más latencia;
- más handoffs;
- más contexto;
- más fallos;
- más dificultad de debugging.

**No hay que graphificar todo.**

---

## 16. Cuándo usar Agent Graphs

Los usaría cuando existe al menos una de estas condiciones:

| Problema | Graph |
|---|---|
| pasos conocidos | ✅ |
| branching | ✅ |
| retries | ✅ |
| validadores | ✅ |
| acciones peligrosas | ✅ |
| humanos intermedios | ✅ |
| varios agentes | ✅ |
| procesos largos | ✅ |
| necesidad de auditoría | ✅ |
| workflow repetible | ✅ |

No los usaría para:

```text
"explicame este error"
```

o:

```text
"creame este endpoint sencillo"
```

si un único coding agent lo resuelve perfectamente.

---

## 17. Tres niveles de arquitectura recomendados

### Nivel 1 — Single agent + evaluator

```text
Task
 ↓
Agent
 ↓
Tests
 ↓
Evaluator
 ↓
PASS / Retry
```

Muy buena relación complejidad/beneficio.

### Nivel 2 — Graph de ingeniería

```text
Context
 ↓
Plan
 ↓
Implement
 ↓
Tests
 ↓
Review
 ↓
Human Gate
```

Buen punto de inicio para proyectos reales.

### Nivel 3 — Multi-agent graph

```text
Planner
      ↓
 ┌────┼─────┐
 ↓    ↓     ↓
API   DB    UI
 ↓    ↓     ↓
 └────┼─────┘
      ↓
 Integration
      ↓
 Evaluators
```

Lo usaría únicamente cuando las especializaciones tienen valor real.

---

## 18. Buenas prácticas para Agent Graphs

### 18.1. Empezar por el graph mínimo

Primero:

```text
Implement → Evaluate
```

Después agregar complejidad sólo por fallas observadas.

### 18.2. Separar control determinista de decisiones agentic

Ejemplo:

```text
tests_passed?
```

No necesita LLM.

Código tradicional para reglas claras.

LLM para decisiones ambiguas.

### 18.3. Estado estructurado

Evitar:

```text
state = chat_history
```

Preferir:

```text
state.plan
state.requirements
state.test_results
state.findings
```

### 18.4. Nodos con una responsabilidad

Evitar:

```text
analyze_plan_implement_test_review()
```

Mejor:

```text
inspect()
plan()
implement()
test()
review()
```

### 18.5. Separar Generator y Evaluator

No utilizar como criterio principal:

```text
"¿hiciste todo bien?"
```

al mismo agente que desarrolló.

### 18.6. Definir invariantes externas

Por ejemplo:

```text
lint = pass
tests = pass
coverage >= threshold
migration = reversible
API contract = unchanged
```

Las invariantes no deberían depender del juicio del agente.

### 18.7. Limitar loops

Nunca:

```text
while evaluator != pass:
```

sin límite.

Usar algo como:

```text
max_retries = 3
```

y después:

```text
human escalation
```

### 18.8. Checkpoints en boundaries importantes

Por ejemplo:

```text
PLAN_APPROVED
IMPLEMENTED
TESTED
REVIEWED
```

Permite recuperación.

### 18.9. Hacer explícita la política de error

Cada nodo debería tener:

```text
success
recoverable failure
fatal failure
human escalation
```

### 18.10. Medir el camino, no solamente la respuesta

Guardar:

```text
node
duration
tokens
tool_calls
errors
retries
state transitions
```

### 18.11. Agentes especializados sólo cuando existe especialización real

Un agente independiente debería justificar al menos una diferencia relevante en:

```text
tools
context
permissions
model
prompt
responsibility
```

Si no, probablemente sea mejor un nodo o una función.

### 18.12. Human gates según riesgo

Ejemplo:

```text
rename variable
→ autonomous

schema migration
→ approval

production deployment
→ approval
```

### 18.13. Versionar el graph

Idealmente:

```text
agent-graph-v1.yaml
agent-graph-v2.yaml
```

porque la topología también es parte del comportamiento del sistema.

### 18.14. Evaluar subgraphs independientemente

Por ejemplo:

```text
context-router evals
planner evals
reviewer evals
integration evals
```

No solamente:

```text
end-to-end task passed
```

### 18.15. No convertir el graph en BPMN para LLMs

Si cada decisión está preprogramada:

```text
A → B → C → D → E
```

el LLM pierde prácticamente toda autonomía.

El valor está en encontrar:

```text
determinismo
    ↕
autonomía
```

no en maximizar ninguno de los dos.

---

## 19. Qué frameworks vale la pena estudiar

### LangGraph

Es probablemente la referencia más madura para esta idea.

Ofrece:

- stateful graphs;
- cycles;
- durable execution;
- persistence;
- human-in-the-loop;
- multi-agent;
- checkpointing;
- streaming.

Documentación:
https://docs.langchain.com/oss/python/langgraph/overview

### Microsoft Agent Framework Workflows

Muy interesante porque Microsoft está consolidando ideas provenientes de AutoGen y Semantic Kernel.

Tiene:

```text
Executors
Edges
State
Checkpointing
HITL
Concurrency
Sub-workflows
```

Documentación:
https://learn.microsoft.com/en-us/agent-framework/concepts/workflows/

### AutoGen GraphFlow

Tiene valor principalmente histórico/conceptual porque introdujo varias ideas de graph orchestration en AutoGen.

El nuevo Agent Framework de Microsoft está evolucionando ese enfoque.

---

## 20. Qué muestran las investigaciones recientes

### Graphs dinámicos

Una línea reciente propone crear automáticamente:

```text
intent
↓
plan
↓
tasks
↓
agent selection
↓
dynamic call graph
```

y añadir un critique agent para revisar la selección de agentes.

### Graphs + estado estructurado

Otra dirección interesante consiste en utilizar un único agente pero rodearlo de un runtime estructurado, tipos y knowledge graphs en lugar de depender de enormes conversaciones de texto.

Esto refuerza una idea importante:

> La calidad del harness y del runtime puede ser más importante que aumentar la cantidad de agentes.

### Graphs adaptativos

Otra línea experimental estudia sistemas donde no todos los nodos se ejecutan con la misma prioridad, sino que se asigna presupuesto según:

```text
goal relevance
graph dependencies
resource constraints
```

buscando reducir tokens y latencia.

---

## 21. Modelo mental recomendado

```text
┌──────────────────────────────────────────────┐
│                   SPEC                       │
│         intención + constraints              │
└───────────────────┬──────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│               AGENT GRAPH                    │
│                                              │
│  define:                                     │
│  • estados                                   │
│  • transiciones                              │
│  • loops                                     │
│  • humanos                                   │
│  • concurrencia                              │
└───────────────────┬──────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│                  HARNESS                     │
│                                              │
│ context · tools · permissions · invariants   │
└───────────────────┬──────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│                  AGENTS                      │
│                                              │
│ planner · coder · reviewer · specialist      │
└───────────────────┬──────────────────────────┘
                    ↓
┌──────────────────────────────────────────────┐
│                   EVALS                      │
│                                              │
│ outcome + trace + state + tool behavior      │
└──────────────────────────────────────────────┘
```

Esto reúne bastante bien SDD, Harness Engineering, Context Engineering y agentes.

---

## 22. La principal advertencia

**Agent Graphs no deberían convertirse en la nueva arquitectura por defecto para cualquier agente.**

La regla práctica sería:

```text
Simple agent loop
        ↓
si funciona
        ↓
no agregar graph
```

Si aparecen:

```text
control requirements
retries
evaluation
parallelism
human approval
durability
complex context routing
```

recién ahí:

```text
Agent Graph
```

Los graphs son especialmente útiles cuando hay estructura real que vale la pena imponer, pero workflows excesivamente rígidos pueden terminar obstaculizando a modelos cada vez más capaces.

---

## 23. Para una línea de investigación aplicada

No investigaría Agent Graphs como un tema aislado.

Los estudiaría como la siguiente pieza de este sistema:

```text
Spec Driven Development
        ↓
Context Engineering
        ↓
Harness Engineering
        ↓
Agent Graph
        ↓
Generator / Evaluator
        ↓
Evals
```

Y particularmente probaría una arquitectura:

```text
Spec
 ↓
Context Router
 ↓
Planner
 ↓
Generator
 ↓
Tests
 ↓
Evaluator
 ↓
Repair loop
 ↓
Human approval
```

sobre un repositorio real.

Ahí se vuelve mucho más claro qué agrega realmente el graph, qué cosas conviene dejar al agente y cuáles conviene sacar de su control.

Las métricas que usaría:

```text
task success
tests passed
human interventions
iterations
tokens
wall-clock time
cost
regressions
incorrect self-reports
context retrieved
```

Esa comparación sería mucho más valiosa que implementar un framework de graphs sólo para aprender su API.

---

## 24. Fuentes principales para seguir estudiando

### LangGraph

- Overview:
  https://docs.langchain.com/oss/python/langgraph/overview
- 3 years of graph engineering:
  https://www.langchain.com/blog/3-years-of-graph-engineering-with-langgraph
- Multi-agent workflows:
  https://www.langchain.com/blog/langgraph-multi-agent-workflows
- Agent evals:
  https://www.langchain.com/resources/agent-evals

### Microsoft Agent Framework

- Workflows:
  https://learn.microsoft.com/en-us/agent-framework/concepts/workflows/
- Durable extension:
  https://learn.microsoft.com/en-us/agent-framework/integrations/durable-extension
- Migration from AutoGen:
  https://learn.microsoft.com/en-us/agent-framework/migration-guide/from-autogen/

### Papers / trabajos recientes

- From Intent to Execution:
  https://arxiv.org/abs/2605.03986
- El Agente Gráfico:
  https://arxiv.org/abs/2602.17902
- Focus Is All You Need:
  https://arxiv.org/abs/2607.23678

---

## 25. Conclusión

La idea más importante no es “usar un grafo”.

La idea es diseñar explícitamente:

```text
qué controla el runtime
qué decide el agente
qué se verifica automáticamente
qué necesita evaluación independiente
qué requiere intervención humana
qué estado persiste
qué contexto entra
qué condiciones permiten avanzar
```

La arquitectura recomendada para software agentic no debería ser:

```text
muchos agentes porque sí
```

sino:

```text
mínima estructura necesaria
+
autonomía donde aporta valor
+
checks deterministas
+
estado estructurado
+
evaluator independiente
+
observabilidad
+
human gates por riesgo
```

En ese sentido, los Agent Graphs encajan especialmente bien como una capa de orquestación dentro del harness.

Una buena progresión de aprendizaje sería:

```text
single agent
→ agent + evaluator
→ graph mínimo
→ context router
→ durable execution
→ subgraphs
→ multi-agent sólo si es necesario
```

Y una buena prueba experimental sería comparar sobre un mismo repositorio:

```text
single coding agent
vs
generator/evaluator
vs
agent graph
```

midiendo éxito, costo, tokens, tiempo, retries, bugs y necesidad de intervención humana.

Así se puede determinar empíricamente qué partes del Agent Graph aportan valor real y cuáles agregan únicamente complejidad.
