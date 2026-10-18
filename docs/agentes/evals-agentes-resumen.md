# Evals para agentes IA — Resumen

## Idea central

Los **evals** miden si un agente funciona realmente bien y con qué confiabilidad.

No deberían ser una prueba ocasional, sino parte del ciclo de desarrollo:

```text
fallo
→ eval
→ mejora
→ regression protection
```

## Qué evalúan

Un eval puede medir:

- resultado final;
- estado real del entorno;
- trayectoria del agente;
- uso de tools;
- cumplimiento de scope;
- costo, latencia y tokens;
- calidad del evaluator.

Para agentes de software, conviene priorizar:

```text
Outcome real
→ invariantes
→ tests deterministas
→ trayectoria
→ explicación del agente
```

## Evals ≠ tests

```text
Test
→ valida software determinista

Eval
→ valida comportamiento probabilístico del agente
```

Los tests pueden actuar como **graders** dentro de un eval.

## Tipos de grader

1. **Deterministas**
   - tests
   - lint
   - typecheck
   - AST
   - DB state
   - scripts

2. **LLM-as-judge**
   - arquitectura
   - sobreingeniería
   - instruction following
   - calidad de review

3. **Humanos**
   - calibración
   - casos ambiguos
   - muestras críticas

Regla recomendada:

> usar lógica determinista siempre que sea posible y reservar LLM judges para criterios semánticos.

## Outcome y trajectory

No alcanza con preguntar si terminó bien.

También puede importar cómo llegó:

```text
¿leyó secretos?
¿tocó producción?
¿modificó archivos fuera de scope?
¿declaró VERIFIED sin evidencia?
```

Pero no conviene imponer una trayectoria exacta si múltiples caminos pueden ser válidos.

## Capability vs Regression

### Capability evals
Miden cuánto puede hacer el agente.

```text
20% → 40% → 70%
```

Sirven para mejorar.

### Regression evals
Protegen capacidades que ya funcionan.

```text
objetivo ≈ 100%
```

Cuando un capability eval se estabiliza, puede pasar al regression suite.

## Cómo construir una suite

Empezar con unos **20–50 escenarios reales**.

Priorizar casos provenientes de:

- bugs reales;
- errores del agente;
- incidentes;
- revisiones humanas;
- reglas de arquitectura;
- ADRs.

Incluir también casos negativos:

```text
no modificar common
no arreglar un baseline failure ajeno
no implementar una ADR proposed
no declarar VERIFIED sin evidencia
```

## Múltiples trials

Un solo PASS no demuestra confiabilidad.

Ejemplo:

```text
E04
PASS
PASS
FAIL
PASS
PASS

success rate = 80%
```

Métricas útiles:

- `pass@k`: al menos un éxito en k intentos;
- `pass^k`: éxito en todos los intentos.

## Arquitectura recomendada

```text
Spec
 ↓
Evals
 ↓
Harness / Agent
 ↓
Implementation
 ↓
Deterministic checks
 ↓
Evaluator
 ↓
Evidence
 ↓
Eval result
```

## Evaluar también al Evaluator

El evaluator también necesita evals.

Medir, por ejemplo:

```text
bug real → debería FAIL
cambio correcto → debería PASS
```

Importan:

- false positives;
- false negatives;
- defect recall.

## Evals para Agent Graphs

Si existe:

```text
Context Router
↓
Planner
↓
Implementer
↓
Reviewer
```

conviene evaluar:

- cada nodo;
- la trayectoria;
- el resultado end-to-end.

Así un FAIL permite saber dónde se degradó el sistema.

## Reproducibilidad

Cada trial debería empezar limpio:

```text
repo pinned commit
Docker / entorno aislado
DB conocida
filesystem limpio
dependencias fijas
```

También versionar:

- modelo;
- prompt;
- AGENTS.md;
- harness;
- tools;
- suite de evals;
- commit del repo.

## CI recomendado

### PR
- graders deterministas;
- pocos regression evals.

### Nightly
- regression suite completa;
- múltiples trials;
- trajectory evals.

### Cambios de modelo/harness
- capability suite;
- judges más caros;
- revisión humana de muestras.

## Buenas prácticas

- definir éxito antes de optimizar;
- outcome antes que discurso;
- usar graders deterministas cuando se pueda;
- separar graders por dimensión;
- incluir evals negativos;
- trabajar con casos reales;
- ejecutar múltiples trials;
- guardar traces;
- convertir bugs en nuevos evals;
- proteger los graders contra gaming;
- medir también costo y latencia;
- mantener los evals versionados como código.

## Relación con el resto del sistema

```text
SDD
→ define qué debería pasar

Harness Engineering
→ crea el entorno

Agent Graphs
→ controlan el proceso

Generator / Evaluator
→ separan construcción de juicio

Evals
→ miden si todo funciona realmente
```

## Conclusión

Los evals convierten el trabajo con agentes de una práctica basada en intuición a una práctica medible.

El objetivo no es demostrar que el agente es bueno, sino conocer:

```text
dónde falla
con qué frecuencia
bajo qué condiciones
qué regresiones produce
cuánto cuesta resolver una tarea
```

El patrón más útil es:

```text
failure
→ eval
→ improvement
→ regression protection
```
