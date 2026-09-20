# Informe de investigación — Harness Engineering para desarrollo con agentes IA y caso Pi + gentle-pi

Investigación sobre Harness Engineering aplicada al desarrollo de software con agentes de IA, con foco en buenas prácticas, evidencia verificable, arquitectura de harnesses y un informe particular sobre Pi + gentle-pi.

---

## 1. Qué es realmente un harness

Un **agent harness** no es otro modelo de IA y tampoco es simplemente un prompt grande.

Es la infraestructura que rodea al modelo y determina:

```text
qué contexto recibe
qué herramientas puede utilizar
qué puede modificar
cómo divide el trabajo
cómo recuerda lo sucedido
cómo verifica su resultado
qué evidencia debe producir
qué acciones requieren autorización
cuándo debe continuar
cuándo debe detenerse
```

Una manera útil de verlo es:

```text
                  ┌───────────────────────┐
                  │     Intención humana  │
                  └───────────┬───────────┘
                              │
                           Spec / Goal
                              │
                              ▼
┌───────────────────────────────────────────────────┐
│                    HARNESS                        │
│                                                   │
│ Contexto        Tools          Policies           │
│ Memoria         Planning       Permissions        │
│ Tests           Linters        Observabilidad     │
│ Evaluators      Git            Human approvals    │
│                                                   │
│                  ┌──────────┐                     │
│                  │   LLM    │                     │
│                  └──────────┘                     │
└───────────────────────────────────────────────────┘
                              │
                              ▼
                         Cambio verificable
```

Es decir:

**LLM ≠ agente completo.**

Y:

**agente ≠ harness.**

El agente es el modelo actuando en un loop con herramientas. El harness es el sistema que hace que ese loop sea útil y controlable.

---

## 2. Por qué Harness Engineering está tomando tanta importancia

Una de las referencias más importantes actuales es la publicación de OpenAI:

**Harness engineering: leveraging Codex in an agent-first world**

OpenAI describe un experimento interno donde un pequeño equipo construyó un producto con alrededor de **un millón de líneas de código generado por Codex**, unas 1.500 PR y prácticamente sin código escrito manualmente por humanos.

La conclusión no fue simplemente que el modelo "programa muy bien", sino que el trabajo de los ingenieros pasó a centrarse en:

- diseñar entornos;
- expresar intención;
- construir feedback loops;
- asegurar legibilidad para el agente;
- estructurar el repositorio para que el agente pueda operar correctamente.

Fuente:

- OpenAI — Harness engineering: leveraging Codex in an agent-first world  
  https://openai.com/index/harness-engineering/

Una idea conceptual importante es:

```text
Humans steer.
Agents execute.
```

Eso no significa que el humano desaparece.

Significa que el trabajo humano se desplaza.

Antes:

```text
humano
  ↓
escribe código
  ↓
tests
```

Cada vez más:

```text
humano
  ↓
define intención + constraints + validación
  ↓
harness
  ↓
agentes
  ↓
código + tests + docs + infraestructura
  ↓
evidencia
  ↓
humano decide
```

---

## 3. El harness no reemplaza al modelo: multiplica o limita su capacidad

Es fácil caer en:

> "Si uso el modelo más potente ya no necesito infraestructura adicional."

La evidencia actual apunta a algo distinto.

Anthropic publicó:

**Harness design for long-running application development**

En ese trabajo experimentaron con una arquitectura Planner–Generator–Evaluator para construir aplicaciones durante varias horas.

Descubrieron dos problemas frecuentes:

1. pérdida de coherencia en tareas largas;
2. mala autoevaluación del propio agente.

En particular, Anthropic observó que separar:

```text
agente que construye
        ≠
agente que evalúa
```

es una palanca importante para mejorar el resultado.

El evaluador tampoco es perfecto, pero resulta mucho más fácil ajustar un evaluador para ser escéptico que pedirle al propio generador que critique correctamente su trabajo.

Fuente:

- Anthropic — Harness design for long-running application development  
  https://www.anthropic.com/engineering/harness-design-long-running-apps

---

## 4. Pero hay una corrección muy importante: no todo necesita multiagentes

Un harness no debería ser:

```text
planner
→ architect
→ coder
→ reviewer
→ tester
→ security-agent
→ documentation-agent
→ evaluator
→ evaluator-del-evaluator
```

para modificar un typo.

Anthropic experimentó con estructuras complejas y observó que, a medida que los modelos mejoraron, algunas piezas del harness dejaron de ser necesarias.

La recomendación práctica es:

**volver a evaluar el harness con cada generación de modelos y eliminar lo que ya no aporte valor.**

La idea central es:

> Harness Engineering no significa agregar ceremonia. Significa agregar únicamente la estructura que compense debilidades reales del agente.

Fuente:

- Anthropic — Harness design for long-running application development  
  https://www.anthropic.com/engineering/harness-design-long-running-apps

---

## 5. Modelo recomendado de harness

Después de contrastar distintas fuentes, una arquitectura razonable de harness moderno tiene estas capas:

| Capa | Responsabilidad |
|---|---|
| Intención | Qué queremos conseguir |
| Spec | Goal, scope, constraints, validation |
| Context router | Qué información necesita el agente |
| Tools | Terminal, Git, browser, DB, APIs, etc. |
| Execution | Agente que modifica |
| Deterministic checks | Tests, compiler, linter, static analysis |
| Evaluación independiente | Búsqueda adversarial de fallos |
| Evidencia | Logs, tests, screenshots, diff |
| Safety | Permisos, secretos, sandbox, acciones destructivas |
| Observabilidad | Qué hizo el agente y por qué falló |
| Human gate | Decisiones irreversibles o sensibles |

Un preprint académico de 2026 propone formalizar Harness Engineering mediante varias responsabilidades similares, incluyendo especificación, selección de contexto, memoria, observabilidad, verificación, permisos y trazabilidad.

Fuente:

- AI Harness Engineering: A Runtime Substrate for Foundation-Model Software Agents  
  https://arxiv.org/abs/2605.13357

---

## 6. Buenas prácticas más importantes

### 6.1. Dar al agente una forma objetiva de verificar su trabajo

Tests, build, lint, type-check, navegador, screenshots o checks específicos.

La diferencia entre:

> "Creo que funciona"

y

> "Ejecuté este test y pasó"

es fundamental.

Fuente:

- Anthropic — Claude Code best practices  
  https://www.anthropic.com/engineering/claude-code-best-practices

### 6.2. Evidencia > afirmaciones

No aceptar únicamente:

```text
los tests pasan
```

El harness debería poder mostrar:

- comando ejecutado;
- exit code;
- resultado del test;
- screenshot;
- diff;
- log relevante;
- artifact generado.

### 6.3. Mantener el contexto pequeño y de alta señal

El contexto es un recurso finito.

Más contexto no significa automáticamente mejor resultado.

Fuente:

- Anthropic — Effective context engineering for AI agents  
  https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents

OpenAI llegó a una conclusión similar: un `AGENTS.md` enorme suele ser peor que un archivo corto que funcione como índice hacia documentación específica.

Fuente:

- OpenAI — Harness engineering  
  https://openai.com/index/harness-engineering/

### 6.4. Progressive disclosure

No cargar arquitectura, ADR, políticas de seguridad, testing, frontend y persistencia en cada tarea.

El agente debería descubrir información cuando la necesita.

### 6.5. Separar generador de evaluador cuando el riesgo lo justifique

Especialmente útil para:

- bugs complejos;
- seguridad;
- arquitectura;
- PR grandes;
- interfaces visuales;
- dinero;
- autenticación;
- persistencia;
- workflows críticos.

Para cambios triviales probablemente sea innecesario.

### 6.6. Mantener un único writer cuando sea posible

Paralelizar lectura e investigación es mucho más seguro que dejar varios agentes escribir simultáneamente sobre los mismos archivos.

### 6.7. Trabajar en unidades revisables

Cambios pequeños y coherentes producen mejor feedback y facilitan la atribución de errores.

### 6.8. Convertir políticas importantes en controles ejecutables

Una regla escrita como:

```text
no rompas dependencias entre módulos
```

es más débil que:

```text
script/check que falla si la dependencia se rompe
```

### 6.9. Proteger el entorno

Especial cuidado con:

- shell;
- secrets;
- credenciales;
- `git reset`;
- deletes;
- despliegues;
- infraestructura;
- datos productivos.

### 6.10. Reevaluar periódicamente el harness

Cada componente agrega:

- costo;
- latencia;
- complejidad;
- superficie de fallo.

Si un modelo nuevo ya resuelve correctamente un comportamiento, conviene retirar el scaffolding innecesario.

---

## 7. Arquitectura recomendada para desarrollo normal

No usaría Generator/Evaluator completo para cada modificación.

Usaría routing por riesgo:

| Tipo de cambio | Harness |
|---|---|
| Typo / docs / rename simple | Agente directo + check |
| Bug local | Agente + regression test |
| Feature acotada | Spec breve + implementación + tests |
| Investigación extensa | Subagente scout |
| Modificación multiarchivo | Writer dedicado |
| Cambio de arquitectura | SDD + alternativas + ADR + evaluator |
| Seguridad / auth / dinero / datos | SDD + deterministic checks + evaluator adversarial + humano |
| UI compleja | Generator + navegador real + evaluator visual/funcional |

---

## 8. Pi desde cero

Pi es especialmente interesante porque adopta una filosofía distinta de herramientas más opinionadas.

**Pi se define como un minimal agent harness.**

No intenta decidir cómo tiene que ser tu proceso de desarrollo.

El core es pequeño y permite agregar:

```text
Extensions
Skills
Prompt Templates
Themes
Pi Packages
```

Fuente:

- Pi — sitio oficial  
  https://pi.dev/

- Pi — documentación oficial  
  https://pi.dev/docs/latest

Conceptualmente:

```text
Claude Code
━━━━━━━━━━━━━━━━━━
modelo + workflow bastante opinionado


Pi
━━━━━━━━━━━━━━━━━━
modelo + primitives
         ↓
vos construís el workflow
```

Esta filosofía hace que Pi sea muy útil para aprender Harness Engineering porque el harness es visible y modificable.

---

## 9. Qué ofrece Pi

Pi ya resuelve una base útil:

```text
LLM providers
tool calling
terminal interaction
sessions
tree history
context management
compaction
AGENTS.md
extensions
skills
prompt templates
packages
model switching
```

Además permite cambiar de modelo durante una sesión.

Las sesiones forman un árbol, permitiendo volver a un punto anterior y crear otra rama de razonamiento.

Fuente:

- Pi Docs  
  https://pi.dev/docs/latest

---

## 10. Qué es gentle-pi

```text
          PI
minimal agent harness
          │
          ▼
     gentle-pi
          │
          ▼
opinionated engineering harness
```

`gentle-pi` es un **Pi Package creado por Gentleman Programming** que convierte Pi en un entorno más opinionado para ingeniería de software.

No reemplaza Pi.

Lo extiende.

Su objetivo es convertir Pi en un harness de desarrollo con:

- SDD/OpenSpec;
- subagentes;
- evidencia TDD;
- review guardrails;
- skill discovery;
- routing de trabajo.

Fuentes:

- gentle-pi — Pi Packages  
  https://pi.dev/packages/gentle-pi

- gentle-pi — GitHub  
  https://github.com/Gentleman-Programming/gentle-pi

---

## 11. Qué agrega gentle-pi sobre Pi

Su arquitectura incorpora:

```text
Pi
│
├── Gentleman parent/orchestrator
│
├── Work routing
│   ├── inline
│   ├── subagent
│   └── SDD
│
├── SDD/OpenSpec
│   ├── explore
│   ├── proposal
│   ├── spec
│   ├── design
│   ├── tasks
│   ├── apply
│   ├── verify
│   └── archive
│
├── Strict TDD evidence
│   RED
│   GREEN
│   TRIANGULATE
│   REFACTOR
│
├── Subagents
│
├── Skills
│
├── Review
│
├── Git-derived evidence
│
├── Safety guards
│
└── model routing
```

Una idea muy importante del proyecto es:

> confiar en lo que el sistema puede derivar, no únicamente en lo que el agente afirma.

Eso es Harness Engineering puro.

---

## 12. Cómo decide gentle-pi cuánto harness usar

gentle-pi clasifica el trabajo aproximadamente así:

| Solicitud | Estrategia |
|---|---|
| pequeña, clara y local | trabajo inline |
| necesita mucha exploración | subagente focalizado |
| grande, ambigua, arquitectónica o riesgosa | SDD/OpenSpec |

Conceptualmente:

```text
                   Request
                      │
                      ▼
              ¿qué tan riesgoso es?
             /          |          \
            /           |           \
        pequeño     contexto      complejo
           │          pesado          │
           ▼            ▼             ▼
        inline      subagent         SDD
```

Esto evita usar un workflow pesado para cambios triviales.

---

## 13. Los subagentes en gentle-pi

gentle-pi intenta mantener la sesión padre pequeña.

Ejemplo conceptual:

```text
Parent
│
├── entiende objetivo
├── controla scope
├── decide workflow
│
├── Scout
│     └── investiga código
│
├── Worker
│     └── implementa
│
└── Evaluator
      └── verifica
```

La sesión principal no debería consumir todo su contexto explorando detalles.

---

## 14. SDD dentro de gentle-pi

SDD no significa escribir un documento enorme antes de programar.

Un spec moderno debería ser:

```text
incremental
pequeño
operacional
verificable
```

gentle-pi usa OpenSpec para materializar esa idea.

Flujo aproximado:

```text
Intent
 ↓
Explore
 ↓
Proposal
 ↓
Spec
 ↓
Design
 ↓
Tasks
 ↓
Apply
 ↓
Verify
 ↓
Archive
```

Una ventaja clave es que las decisiones importantes dejan de existir únicamente en el chat.

Quedan como artifacts.

Esto protege contra:

```text
context compaction
session reset
otro agente
otro modelo
otro desarrollador
```

---

## 15. El papel del TDD

Cuando el proyecto tiene tests configurados, gentle-pi puede exigir evidencia:

```text
RED
 ↓
GREEN
 ↓
TRIANGULATE
 ↓
REFACTOR
```

La diferencia importante es:

Prompt:

```text
Por favor usá TDD.
```

Harness:

```text
No puedo considerar completado APPLY/VERIFY
sin evidence de los estados definidos.
```

La segunda opción es mucho más fuerte.

---

## 16. Estado de gentle-pi y estabilidad de versiones

El ecosistema evoluciona rápidamente.

Al momento de la investigación, la documentación distinguía entre:

```text
v0.14.0
última versión estable sin native RDD

v0.15.0+
línea experimental/unstable de native RDD
```

Por eso no conviene instalar `latest` a ciegas en un repositorio importante.

Fuente:

- gentle-pi package  
  https://pi.dev/packages/gentle-pi

> Nota: esta sección es temporal. Verificar versiones antes de instalar.

---

## 17. Qué versión usar para aprender

Propuesta de dos laboratorios.

### Laboratorio A — aprender el harness

```bash
pi install npm:gentle-pi@0.14.0
```

Objetivo:

```text
Pi
SDD
subagents
TDD
skills
routing
context engineering
```

### Laboratorio B — investigar lo más reciente

En otro repositorio descartable:

```bash
pi install npm:gentle-pi@latest
```

Ahí estudiar:

```text
native review
authority
review transactions
Git-derived evidence
bounded review
```

---

## 18. Implementación desde cero de Pi + gentle-pi

### Paso 1 — instalar Pi

Opción con npm:

```bash
npm install -g --ignore-scripts @earendil-works/pi-coding-agent
```

También existen instaladores por shell y PowerShell.

Fuente:

- Pi Docs  
  https://pi.dev/docs/latest

---

## 19. Instalar gentle-pi

Para una experiencia estable:

```bash
pi install npm:gentle-pi@0.14.0
```

Para experimentar con la línea más reciente:

```bash
pi install npm:gentle-pi@latest
```

Recomendación:

**No usar latest como primera experiencia en un repositorio importante.**

---

## 20. Primer arranque

Dentro del repositorio:

```bash
pi
```

Después:

```text
/gentle:status
```

y:

```text
/gentle:doctor
```

`status` inspecciona la configuración general.

`doctor` sirve como diagnóstico del entorno.

---

## 21. Inicializar SDD

La documentación utiliza:

```text
/gentle-sdd-init
```

para crear o actualizar:

```text
openspec/config.yaml
```

Flujo sugerido:

```text
pi
↓
/gentle:status
↓
/gentle:doctor
↓
/gentle-sdd-init
```

---

## 22. Configurar modelos

gentle-pi permite asignar distintos modelos a distintos roles.

Ejemplo:

```text
Scout          → modelo rápido/barato

Generator      → modelo fuerte en código

Evaluator      → modelo fuerte en razonamiento

Architect      → modelo fuerte

Docs           → modelo económico
```

Esto permite optimizar:

```text
calidad
latencia
costo
```

por función.

---

## 23. Paquetes complementarios

La documentación de gentle-pi recomienda varios paquetes complementarios, por ejemplo:

```bash
pi install npm:pi-subagents-j0k3r
pi install npm:pi-intercom
pi install npm:gentle-engram
pi install npm:pi-web-access
pi install npm:pi-lens
pi install npm:@juicesharp/rpiv-todo
pi install npm:@juicesharp/rpiv-ask-user-question
```

Pero no conviene instalar todo el primer día.

Estrategia recomendada:

```text
Pi
+
gentle-pi
```

Luego:

```text
subagents
```

Después, si realmente hace falta:

```text
memoria persistente
web
lens
todo
otras extensiones
```

Cada herramienta nueva aumenta:

```text
contexto
complejidad
superficie de ataque
posibles fallos
```

---

## 24. Primer experimento recomendado

No empezar con una feature enorme.

Crear un repositorio chico con:

```text
API
base de datos
tests
3-4 reglas de dominio
```

Ejercicio:

```text
Agregar bloqueo de usuario después de 5 intentos fallidos.
```

Prompt inicial:

```text
Quiero implementar bloqueo temporal después de
5 intentos de login fallidos.

Usá SDD.

Antes de implementar explicame:
- comportamiento esperado;
- edge cases;
- invariantes;
- cómo vamos a verificarlo.

Después creá los artifacts correspondientes.
No implementes hasta que pueda revisar el spec.
```

Luego:

```text
Implementá el cambio siguiendo el spec aprobado.
Usá el método de testing configurado por el proyecto.
Mostrame evidencia de verificación.
```

Finalmente:

```text
Realizá una revisión independiente del cambio contra
el spec, los tests y el diff.
No modifiques código durante la evaluación.
```

Ese experimento permite estudiar:

```text
intent
→ spec
→ design
→ tasks
→ implementation
→ verification
→ review
```

---

## 25. Qué mirar mientras usás gentle-pi

No evaluar solamente:

> "¿el código quedó bien?"

Evaluar también el comportamiento del sistema.

| Pregunta | Qué estás evaluando |
|---|---|
| ¿leyó demasiados archivos? | Context engineering |
| ¿saltó a código demasiado pronto? | Planning |
| ¿expandió scope? | Governance |
| ¿creó artifacts útiles? | SDD |
| ¿los tests comprueban comportamiento? | Verification |
| ¿el evaluator encontró algo real? | QA |
| ¿dos agentes repitieron trabajo? | Orchestration |
| ¿usó un modelo caro innecesariamente? | Model routing |
| ¿afirmó algo sin evidencia? | Trust model |
| ¿el diff quedó revisable? | Work-unit sizing |

---

## 26. Advertencia de seguridad sobre Pi packages

Los Pi packages pueden ejecutar código e influir en el comportamiento del agente.

Por eso conviene:

```text
revisar repository
pin de versión
instalar en repo experimental primero
evitar secrets innecesarios
usar branch/worktree
no darle credenciales productivas
```

Un package de agent harness puede intervenir en:

```text
shell
files
prompts
tools
agent behavior
network
git
```

Fuente:

- Pi Packages  
  https://pi.dev/packages/gentle-pi

---

## 27. Una idea especialmente poderosa de gentle-pi

```text
Artifacts > floating chat context
```

En un flujo normal:

```text
decisión importante
   ↓
mensaje 43 del chat
   ↓
compaction
   ↓
se pierde
```

Con artifacts:

```text
decisión
   ↓
spec/design/task
   ↓
repositorio
   ↓
nuevo agente
   ↓
mismo conocimiento
```

Esto conecta Harness Engineering con:

```text
SDD
context engineering
repository knowledge
ADRs
tests
traceability
```

---

## 28. gentle-pi frente a Claude Code o Codex

No son exactamente herramientas equivalentes.

| Herramienta | Filosofía |
|---|---|
| Claude Code | Coding agent completo |
| Codex | Coding agent completo |
| Pi | Harness mínimo/extensible |
| gentle-pi | Workflow opinionado encima de Pi |

Por eso Pi + gentle-pi es especialmente útil para estudiar Harness Engineering.

Con herramientas más cerradas, muchas decisiones de harness ya están tomadas por el proveedor.

Con Pi se pueden observar como piezas separadas:

```text
core
+
extensions
+
skills
+
subagents
+
SDD
+
review
+
memory
```

---

## 29. Arquitectura conceptual de gentle-pi

```text
                       HUMANO
                          │
                          ▼
                     REQUEST
                          │
                          ▼
                ┌──────────────────┐
                │ Parent / Gentle  │
                │   Orchestrator   │
                └────────┬─────────┘
                         │
                 clasifica riesgo
                         │
        ┌────────────────┼────────────────┐
        │                │                │
      SMALL          CONTEXT          COMPLEX
        │                │                │
        ▼                ▼                ▼
     inline           scout          SDD/OpenSpec
        │                │                │
        └───────────────┬┴────────────────┘
                        ▼
                     WRITER
                        │
                        ▼
                      CODE
                        │
                        ▼
               deterministic checks
                        │
                        ▼
                   EVALUATOR
                        │
                        ▼
              Git-derived evidence
                        │
                        ▼
                     HUMAN
                        │
                        ▼
                 repository policy
```

Este modelo resume bastante bien una arquitectura moderna de desarrollo agentic.

---

## 30. Documentación imprescindible

### OpenAI — Harness Engineering

Muy buena referencia sobre el cambio del rol del ingeniero y el repositorio como system of record.

https://openai.com/index/harness-engineering/

### Anthropic — Effective harnesses for long-running agents

Buena introducción al problema de contexto entre sesiones, incrementalidad y artifacts de handoff.

https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents

### Anthropic — Harness design for long-running application development

Referencia importante sobre Planner/Generator/Evaluator, QA adversarial y evolución del harness.

https://www.anthropic.com/engineering/harness-design-long-running-apps

### Anthropic — Effective context engineering

Explica por qué el contexto debe considerarse un recurso escaso.

https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents

### Pi documentation

Para entender Pi antes de instalar cualquier harness encima.

https://pi.dev/docs/latest

### gentle-pi

Package:

https://pi.dev/packages/gentle-pi

Repositorio:

https://github.com/Gentleman-Programming/gentle-pi

### Paper académico

AI Harness Engineering: A Runtime Substrate for Foundation-Model Software Agents

https://arxiv.org/abs/2605.13357

---

## 31. Conclusión

La pregunta importante deja de ser únicamente:

```text
¿Qué agente es mejor?
Claude Code vs Codex vs Pi
```

y pasa a ser:

```text
modelo
+
contexto
+
herramientas
+
restricciones
+
tests
+
feedback
+
memoria
+
evaluación
+
evidencia
=
capacidad real del sistema
```

La unidad de análisis ya no debería ser solamente **el modelo**.

Debería ser:

> **modelo + harness + entorno**

Pi + gentle-pi resulta especialmente interesante para aprender esto porque Pi expone las primitives y gentle-pi muestra cómo convertir esas primitives en un sistema de ingeniería opinionado.

Para aprenderlo progresivamente, conviene comenzar con:

```text
Pi
+
gentle-pi
+
repositorio pequeño
```

y estudiar, en este orden:

```text
routing
→ SDD
→ artifacts
→ TDD
→ subagents
→ evaluator
→ model routing
→ review
→ governance
```

Una buena continuación práctica sería tomar un mismo repositorio y comparar dos workflows:

```text
Claude Code + AGENTS.md
vs
Pi + gentle-pi
```

midiendo:

- contexto consumido;
- cantidad de tool calls;
- calidad del diff;
- bugs detectados;
- evidencia generada;
- tiempo;
- costo;
- necesidad de intervención humana.

Así se puede estudiar empíricamente qué partes del harness aportan valor real y cuáles agregan únicamente complejidad.

---

## Nota sobre temporalidad

Las definiciones conceptuales de Harness Engineering, SDD, context engineering, verificación y separación Generator/Evaluator son relativamente estables.

En cambio, estos elementos deben verificarse antes de usarlos:

- versión actual de Pi;
- versión actual de gentle-pi;
- comandos de instalación;
- paquetes recomendados;
- estado stable/experimental;
- proveedores/modelos soportados.

El ecosistema evoluciona rápidamente.
