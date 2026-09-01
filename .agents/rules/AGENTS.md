# AGENTS.md — Engineering & Architecture Rules

> **DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones**
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión:** 3.4.0 (Gobernanza Calibrada, Flujo de 7 Fases con Subagente Revisor y Modo Degradado)
> **Ámbito:** Obligatorio e inmutable para agentes de IA y desarrolladores.

---

## 1. Propósito y Alcance Operativo

Este documento establece las **reglas de gobierno, invariantes arquitectónicas, protocolo de razonamiento y criterios de validación** que rigen la interacción con el repositorio DonaTrack.

`AGENTS.md` **no es un inventario ni una wiki documental**; actúa como el **núcleo de políticas operativas**. Los detalles de diseño, requerimientos de cátedra y decisiones históricas residen en sus respectivos documentos en `docs/`.

```text
                    ┌───────────────────────────────────────────────┐
                    │                   AGENTS.md                   │
                    │      (Gobernanza, Invariantes y Workflow)     │
                    └───────────────────────┬───────────────────────┘
                                            │
                ┌───────────────────────────┼───────────────────────────┐
                ▼                           ▼                           ▼
        [docs/README.md]            [docs/adr/ & DEUDA]         [Código & Tests]
       Índice y Arquitectura        Decisiones y Deuda         Verdad Ejecutable

```

---

## 2. Jerarquía de Fuentes de Verdad y Memoria Histórica

La autoridad de una fuente **se determina según la naturaleza de la consulta**. La siguiente matriz delimita la autoridad primaria:

| Dimensión de la Consulta | Fuente con Autoridad Primaria | Criterio de Resolución |
| --- | --- | --- |
| **Requerimientos y Alcance Académico** | Enunciados de Cátedra (`docs/entregas/`) | Mandatorio. Ningún código ni ADR puede violar el enunciado base. |
| **Decisiones y Justificaciones de Diseño** | ADRs Aprobados (`docs/adr/`) | Explican el *porqué*. Si el código contradice un ADR, constituye una violación arquitectónica o deuda no registrada. |
| **Arquitectura y Modelo de Dominio** | `docs/arquitectura/` | Define límites de agregados, contratos de interfaz y flujos conceptuales. |
| **Contratos Públicos y Comunicación Vigente** | Especificaciones OpenAPI / DTOs / Tests de Contrato | La compatibilidad hacia atrás es prioritaria. |
| **Comportamiento en Ejecución Real** | Código Fuente + Tests Automatizados | Refleja el estado presente del sistema, incluyendo deuda técnica catalogada. |
| **Deuda Técnica Aceptada** | `docs/adr/DEUDA_TECNICA.md` | Lista excepciones temporales reconocidas y autorizadas (DTI-01 a DTI-06). |
| **Metodología y Prompts de IA** | `docs/IA/` | Reglas de interacción y checklists de Pull Request. |

* **`[INVARIANT]` Inmutabilidad de Registros Históricos:** Los enunciados en `docs/entregas/` y los ADRs aprobados son inmutables. **Está terminantemente prohibido editar documentos históricos para hacerlos coincidir con el estado actual del código**. Las divergencias deben documentarse formalmente como nueva evolución, ADR sucesor o deuda técnica.

---

## 3. Protocolo Epistémico y Manejo de Evidencia

Para evitar alucinaciones, conclusiones precipitadas y re-evaluaciones redundantes, todo análisis técnico, diagnóstico o reporte emitido por un agente debe clasificar sus afirmaciones bajo la siguiente taxonomía:

* **`[OBSERVED]`**: Hecho comprobado directamente en el código fuente, archivos de configuración o historial de Git del repositorio local.
* **`[DOCUMENTED]`**: Regla, requerimiento o decisión formalizada en un ADR, especificación o documento de cátedra.
* **`[INFERRED]`**: Deducción lógica o hipótesis derivada a partir de evidencia observable o documentada.
* **`[PROPOSED]`**: Solución, abstracción o cambio de diseño sugerido que **aún no existe** en el repositorio.
* **`[REJECTED]`**: Alternativa evaluada y formalmente descartada en ADRs o minutas por costo, complejidad o desvío académico (evita ciclos de razonamiento repetitivos).
* **`[VERIFIED]`**: Comportamiento confirmado mediante la ejecución exitosa de pruebas automatizadas o comandos de validación.

> [!IMPORTANT]
> **Regla de Honestidad Intelectual:** Nunca presentar una propuesta (`PROPOSED`) o inferencia (`INFERRED`) como si fuera una capacidad ya implementada (`OBSERVED`).

---

## 4. Invariantes Arquitectónicas y Reglas de Integridad

### 4.1 Dominio y Encapsulamiento

* **`[INVARIANT]` State Pattern de Donaciones:** Las transiciones de estado de `DonacionIndependiente` deben preservar rigurosamente:
1. Validación de precondiciones de negocio antes de transicionar.
2. Imposibilidad de ejecutar transiciones inválidas o saltarse estados intermedios.
3. Mantenimiento consistente e inmutable del historial de cambios de estado.
4. Encapsulamiento de la lógica de transición dentro del dominio (evitar mutaciones directas desde servicios externos).


* *Nota:* No sustituir el mecanismo actual sin demostrar mediante tests que preserva estas invariantes y sin el correspondiente ADR.


* **`[CONSTRAINT]` Dependencias del Dominio:** Las entidades y agregados de dominio no deben acoplarse a DTOs de transporte, clientes HTTP ni dependencias de persistencia JPA/Hibernate durante Fase 1.

### 4.2 Arquitectura de Capas y Microservicios

* **`[INVARIANT]` Controladores como Adaptadores Puros:** Los `Controllers` son adaptadores de entrada HTTP. Su única responsabilidad es recibir requests, validar DTOs de frontera, delegar en Application Services y mapear respuestas. No deben contener lógica de dominio ni orquestación compleja.
* **`[INVARIANT]` Criterio de Pertenencia a `common-lib` (Shared Kernel):** Una abstracción solo pertenece a `common-lib` si es utilizada por múltiples bounded contexts **y su semántica no depende de ningún dominio específico** (ej. `CrudRepository<T>`, jerarquía base de excepciones, helpers de logging/tracing). Queda prohibido introducir entidades de dominio, DTOs de negocio o lógica particular.
* **`[PHASE-1 CONSTRAINT]` Persistencia en Fase 1:** La persistencia se resuelve en memoria mediante `CrudRepositoryEnMemoria<T>` (`ConcurrentHashMap`). No incorporar JPA/Hibernate ni bases de datos SQL en esta fase salvo indicación explícita.
* **`[TARGET ARCHITECTURE]` Persistencia Futura:** El diseño del dominio y los repositorios debe permitir migrar la persistencia a SQL en fases posteriores sin contaminar el modelo con detalles de infraestructura ni requerir reescrituras de la lógica de negocio.
* **`[CONSTRAINT]` Comunicación Inter-Servicios:**
* Interacciones HTTP sincrónicas $\rightarrow$ Spring Cloud OpenFeign.
* Eventos de dominio y desacoplamiento temporal $\rightarrow$ Mensajería AMQP vía RabbitMQ (`logistica-service` $\rightarrow$ `donaciones-service`).


* **`[INVARIANT]` Trazabilidad e Idempotencia:** Todo flujo distribuido debe conservar la correlación mediante `traceId`, propagándolo explícitamente en headers de Feign y RabbitMQ, y restaurándolo en el MDC del consumidor. Los consumidores AMQP deben ser idempotentes en la capa de aplicación.

### 4.3 Seguridad Operacional e Integridad de Pruebas

* **`[CONSTRAINT]` Seguridad y Datos Sensibles:** Prohibido introducir credenciales, tokens, contraseñas hardcodeadas o datos personales reales (PII) en código, tests, fixtures, logs o reportes. Toda prueba debe utilizar datos sintéticos.
* **`[INVARIANT]` Integridad de Quality Gates y Pruebas:** Queda terminantemente prohibido deshabilitar tests, eliminar o debilitar aserciones (`assertTrue(true)`), aumentar tolerancias arbitrariamente o alterar linters/CI para enmascarar regresiones o forzar un build verde.
* **`[INVARIANT]` Calidad Estática y Cumplimiento de SonarCloud:** Todo cambio en código Java o configuración de CI/CD debe concebirse para superar el Quality Gate de SonarCloud (*Technical Debt = 0* en ramas principales, *Condition Coverage $\ge$ 80%*, *Security/Reliability/Maintainability = A*, *Duplicación $\le$ 3.0%*). Queda prohibido introducir *code smells* conocidos o scripts vulnerables; el agente debe aplicar la auto-auditoría pre-flight consultando la documentación técnica antes de finalizar su intervención.

---

## 5. Criterios de Evaluación y Atributos de Calidad (Fitness Checks)

Antes de proponer o implementar un cambio, someter el diseño a las siguientes preguntas operacionales:

| Vector de Calidad | Pregunta de Verificación (Fitness Check) |
| --- | --- |
| **1. Cohesión y Mantenibilidad (SRP / OCP)** | ¿La responsabilidad pertenece naturalmente a la clase asignada y permite extender comportamiento variable sin modificar código base estable? |
| **2. Acoplamiento e Identidad** | ¿Se evitó compartir referencias a objetos en memoria entre agregados o microservicios, interactuando únicamente mediante IDs estables (UUID)? |
| **3. Simplicidad Suficiente (KISS / YAGNI)** | ¿La solución resuelve el problema real sin incorporar capas de indirección innecesarias, librerías prescindibles o abstracciones especulativas? |
| **4. Resiliencia y Manejo de Estado** | ¿El sistema degrada de forma controlada ante la falla de un servicio externo y asume que la memoria del proceso local no es persistencia distribuida? |
| **5. Testeabilidad y Reversibilidad** | ¿La lógica puede validarse con tests unitarios aislados y el cambio puede revertirse de forma limpia sin dejar inconsistencias colaterales? |

---

## 6. Control de Alcance (Anti-Scope Creep)

* **`[CONSTRAINT]` Alcance Estrictamente Delimitado:** Una tarea autoriza cambios únicamente sobre los módulos y clases directamente vinculados a su objetivo.
* **`[CONSTRAINT]` Prohibición de Refactorings Oportunistas No Autorizados:** Si durante la inspección se descubren errores secundarios, antipatrones o deuda técnica no relacionada con la tarea en curso:
1. **No corregirlos en la misma iteración**, salvo que el hallazgo bloquee la tarea actual o comprometa críticamente la integridad.
2. Documentarlos formalmente en el reporte de salida como hallazgo o deuda técnica a tratar.
* **`[INVARIANT]` Integridad del Grafo Documental y Excepción de Alcance:** Cuando una tarea cree, traslade, renombre o modifique artefactos arquitectónicos, especificaciones o guías en `docs/`, la actualización sincronizada de sus índices padre (`docs/README.md`) y del panel canónico de vigencia (`docs/ESTADO_DOCUMENTACION.md`) **es mandatoria y forma parte indivisible del alcance autorizado**. Queda explícitamente aclarado que esta sincronización NO constituye refactoring oportunista ni *scope creep*, sino higiene y completitud obligatoria de la entrega.
* **`[CONSTRAINT]` Control de Dependencias Externas:** No incorporar nuevas librerías en `pom.xml` ni alterar imágenes de Docker sin justificación técnica explícita y aprobación previa.
* **`[CONSTRAINT]` Minimalismo Suficiente:** Aplicar el **cambio mínimo suficiente** para cumplir el objetivo funcional o arquitectónico, evitando reescrituras estéticas.

---

## 7. Protocolo de Trabajo del Agente en 7 Fases

El agente debe estructurar su trabajo en las siguientes fases secuenciales:

```text
FASE 1          FASE 2          FASE 3          FASE 4          FASE 5          FASE 6                 FASE 7
Descubrimiento ➔ Análisis      ➔ Diseño        ➔ Implementación ➔ Validación   ➔ Revisión Crítica     ➔ Reporte
(Git/Baseline)  (OBS/INF/DOC)  (PROP/Trade)   (Mínimo suf.)   (Quality Gates) (Subagente Revisor)    (Modular)

```

### Fase 1: Descubrimiento y Baseline

* Inspeccionar el estado real del repositorio:
```bash
git status
git branch --show-current
git log -n 5 --oneline

```


* **Protocolo de Baseline (`BASELINE_GREEN` / `BASELINE_RED`):**
* Ejecutar los tests de los módulos afectados antes de modificar código.
* Si el baseline es `BASELINE_RED` (hay tests preexistentes rotos), **aislar y listar los fallos previos en el reporte** para no atribuirlos a la modificación en curso.



### Fase 2: Análisis Estructural

* Separar hechos observados (`[OBSERVED]`) de inferencias (`[INFERRED]`) y alternativas descartadas (`[REJECTED]`).
* Identificar restricciones, dependencias y riesgos. **No modificar código durante esta fase.**

### Fase 3: Propuesta de Diseño

* Presentar la solución técnica justificando responsabilidades y trade-offs.
* Verificar compatibilidad con los ADRs e invariantes arquitectónicas.
* **Interacción Humana Calibrada:** Solicitar confirmación del usuario **únicamente si el cambio modifica o supera el alcance originalmente autorizado**.

### Fase 4: Implementación Quirúrgica

* Aplicar el cambio mínimo suficiente respetando la encapsulación y los contratos vigentes.
* Mantener consistencia de nombres y estilo de código.
* **Auto-revisión de Code Smells:** Durante la codificación, consultar obligatoriamente la guía de errores frecuentes y *smells* recurrentes en [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](../docs/IA/07-errores-frecuentes-sonarcloud-ia.md) para evitar violaciones estáticas comunes (métodos que deben ser `static`, constructores privados en utilitarios, `@Override`, literales repetidos, shadowing de variables).

### Fase 5: Validación Gradual (Quality Gates)

* Ejecutar los Quality Gates correspondientes al alcance (Sección 11).
* **Verificación de Cobertura Condicional y Pre-Flight Sonar:** Asegurar que todo nuevo camino lógico (`if`, `switch`, ternarios, `Optional`) cuente con pruebas unitarias para todas sus bifurcaciones (*Condition Coverage $\ge 80\%$*), y verificar el cumplimiento del checklist pre-flight de [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](../docs/IA/07-errores-frecuentes-sonarcloud-ia.md).
* Comprobar formato y estilo de código (`mvn spotless:check`).

### Fase 6: Revisión Crítica Adversarial y Refinamiento

Para erradicar el sesgo de auto-confirmación (*confirmation bias*), el agente **no debe dar por concluida su tarea sin someter su entrega a una auditoría independiente**.

* **Invocación Mandatoria de Subagente Revisor:**  
El agente principal debe invocar mediante la herramienta `invoke_subagent` a un subagente independiente de sólo lectura (`Role: Revisor Crítico Adversarial`, `TypeName: research` o `self`) enviándole el `git diff` de la rama y el objetivo de la tarea.
* **Vectores de Auditoría Obligatorios:** El subagente revisor debe auditar la entrega contra tres fuentes normativas:
1. [`docs/auditoria/plan-revisor-critico.md`](../docs/auditoria/plan-revisor-critico.md): Falsación activa, búsqueda de casos borde no cubiertos y violación de invariantes de agregados.
2. [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](../docs/IA/07-errores-frecuentes-sonarcloud-ia.md): Detección de *code smells* de SonarCloud (visibilidad JUnit 5, `@Override`, `static` en utilitarios, duplicación de strings, imports sobrantes).
3. [`docs/ESTADO_DOCUMENTACION.md`](../docs/ESTADO_DOCUMENTACION.md) y [`docs/README.md`](../docs/README.md): Comprobación de integridad del grafo documental (ausencia de documentos huérfanos e índices desincronizados).
* **Ciclo de Corrección Inmediata:**  
El subagente revisor emite un informe estructurado con los defectos u omisiones detectadas. El agente principal **debe aplicar inmediatamente las correcciones pertinentes dentro del alcance autorizado** y re-ejecutar Gate 1 (`mvn spotless:check` / `mvn test`) antes de proceder a la siguiente fase.
* **Modo Fallback Monoproceso (Sin Soporte de Subagentes):**  
Si el entorno carece de la herramienta `invoke_subagent` o no soporta subagentes concurrentes, el agente principal **debe adoptar explícitamente el rol de auditor escéptico** en un bloque de razonamiento aislado de su propia traza, evaluando los 3 vectores anteriores con el máximo rigor crítico antes de emitir el reporte final.

### Fase 7: Reporte y Entrega

* **Paso Previo de Pre-Cierre Documental:** Antes de redactar la lista de *Archivos Modificados*, si la tarea involucró creación, edición o reubicación de documentos en `docs/`, verificar obligatoriamente que `docs/README.md` y `docs/ESTADO_DOCUMENTACION.md` se encuentren 100% sincronizados.
* **Mandatorio:** Todo agente debe emitir su reporte de cierre utilizando la siguiente estructura modular estandarizada:

```markdown
### 📋 Reporte Operativo — DonaTrack

#### 1. Resumen Ejecutivo y Alcance
* **Objetivo:** [Breve descripción de la tarea solicitada]
* **Estado de Baseline:** `[BASELINE_GREEN]` | `[BASELINE_RED (Detallar fallos preexistentes aislados)]`
* **Archivos Modificados:** [Listado de rutas relativas intervenidas]

#### 2. Matriz Epistémica de Cambios y Hallazgos
* `[OBSERVED]`: [Evidencias constatadas en código/git/tests antes de intervenir]
* `[DOCUMENTED]`: [ADRs, consignas de cátedra o contratos que respaldan el cambio]
* `[INFERRED]`: [Deducciones lógicas o hipótesis tomadas durante el análisis]
* `[PROPOSED]`: [Modificaciones arquitectónicas o de código implementadas]
* `[REJECTED]`: [Alternativas evaluadas y descartadas con justificación técnica]
* `[VERIFIED]`: [Comandos ejecutados, tests superados y validaciones de formato]

#### 3. Revisión Crítica Adversarial y Correcciones (Fase 6)
* **Modalidad:** `[Subagente Independiente]` | `[Fallback Monoproceso]`
* **Hallazgos Detectados:** [Listado de inconsistencias, smells o desfasajes documentales identificados]
* **Correcciones Aplicadas:** [Detalle de los ajustes realizados antes del cierre]

#### 4. Validación y Quality Gates
* **Gate 1 (Unitario + Formato):** [✅ Aprobado (`mvn test -pl ...`, `mvn spotless:check`)]
* **Gate 2 (Módulo Completo):** [✅ Aprobado | ⏭️ Omitido por alcance]
* **Gate 3/4 (Integración / E2E):** [✅ Aprobado | ⚠️ `[DEFERRED_NO_DOCKER]` (indicar comando pendiente)]

#### 5. Deuda Técnica y Hallazgos Colaterales (Anti-Scope Creep)
* [Deuda técnica catalogada o hallazgos secundarios detectados pero NO modificados en esta iteración]
```

> **Nota de Flexibilidad y Profundidad:** La plantilla anterior fija los campos mínimos de control. Se alienta al agente a enriquecer el reporte con secciones adicionales de análisis arquitectónico profundo, diagramas PlantUML/Mermaid, análisis de trade-offs y explicaciones técnicas detalladas cuando la tarea lo amerite.


---

## 8. Política de Refactoring y Evolución de Contratos

### 8.1 Criterio de Refactor Válido

Todo refactor estructural debe regirse por el principio de preservación:

$$\text{Refactor Válido} = \text{Intención Preservada} \land \text{Invariantes Preservadas} \land \text{Contratos Preservados/Migrados} \land \text{Sin Regresiones}$$

1. **Establecer Baseline:** Identificar el estado inicial (`GREEN` o `RED`).
2. **Preservar Contratos:** Mantener la firma de endpoints, DTOs y payloads de eventos AMQP.
3. **Validación Empírica:** No considerar exitoso un refactor únicamente porque compile; se requiere validación objetiva mediante tests.

### 8.2 Tipificación de Cambios en Contratos y Eventos

* **Refactoring:** Preserva estrictamente los contratos públicos y payloads existentes.
* **Feature:** Permite evolución retrocompatible de contratos REST y eventos AMQP (cambios estrictamente **aditivos**: campos nuevos opcionales; nunca renombrar ni eliminar campos sin ciclo de migración).
* **Breaking Change:** Requiere justificación formal mediante ADR, actualización de suites de integración y aprobación explícita.

---

## 9. Gestión de ADRs y Registro de Deuda Técnica

* **¿Cuándo redactar un nuevo ADR?**
* Introducción de un nuevo patrón de diseño o framework.
* Modificación de límites (*boundaries*) entre Bounded Contexts.
* **Cambio significativo en el modelo de dominio** (agregados, entidades raíz, ownership de datos).
* Cambio en la estrategia de integración o transporte de datos.
* Alteración o evolución del modelo de persistencia.


* **Ubicación:** `docs/adr/` (utilizando la convención de numeración existente).
* **Deuda Técnica:** Registrar discrepancias intencionales en `docs/adr/DEUDA_TECNICA.md` asignando un código identificador (ej. `DTI-07`).

---

## 10. Enrutador Documental (Router de Navegación)

Para consultar especificaciones detalladas, dirigirse a las carpetas especializadas en `docs/`:

```text
docs/
├── ESTADO_DOCUMENTACION.md        ← Matriz de vigencia y auditoría documental
├── README.md                      ← Mapa general y navegación del proyecto
├── arquitectura/                  ← Especificaciones de dominio, agregados y Shared Kernel
├── adr/                           ← Decisiones de arquitectura y catálogo de DEUDA_TECNICA.md
├── auditoria/                     ← Marco metodológico y rúbricas de revisión crítica
├── testing/                       ← Arquitectura de integración y colecciones Postman
├── cicd/                          ← Flujos de CI/CD y políticas de Pull Request
├── IA/                            ← Contexto base para LLMs, checklists y SonarCloud (07-errores-frecuentes-sonarcloud-ia.md)
└── entregas/                      ← Enunciados oficiales y requerimientos de cátedra (INMUTABLES)

```

---

## 11. Pirámide de Validación y Quality Gates

Ejecutar las validaciones de menor a mayor costo computacional según el alcance del cambio:

```text
                      ▲
                     / \       Gate 4: Validación Distribuida E2E
                    /   \      ./run-preprod-tests.sh (Docker + n8n + 4 Servicios)
                   /─────\
                  /       \    Gate 3: Integración y Contratos
                 /         \   mvn test -pl integration-tests -DskipTests=false
                /───────────\
               /             \ Gate 2: Módulo Completo
              /               \ mvn test -pl <modulo> -am
             /─────────────────\
            /                   \ Gate 1: Compilación, Formato y Test Unitario
           /                     \ mvn test -pl <modulo> -Dtest=MiTest | mvn spotless:check
          /───────────────────────\

```

### 11.1 Comandos Universales Maven (Java 21)

```bash
# Gate 1: Test unitario específico
mvn test -pl <modulo-service> -Dtest=<NombreTest>

# Gate 1: Verificación y corrección de formato con Spotless
mvn spotless:check
mvn spotless:apply

# Gate 2: Compilación y tests del módulo con sus dependencias
mvn clean test -pl <modulo-service> -am

# Gate 2: Compilación rápida de todo el reactor (omitiendo tests)
mvn clean package -DskipTests

# Gate 2: Suite completa de tests unitarios del monorepo
mvn test

# Gate 3: Tests de integración y contratos
mvn test -pl integration-tests -DskipTests=false

```

### 11.2 Entornos Docker y Tests Distribuidos E2E (Gate 4)

* **En entornos Bash (Linux / macOS / Git Bash / WSL):**
```bash
# Ejecución integral E2E con ciclo de vida automático
./run-preprod-tests.sh

# Ejecución manteniendo la infraestructura activa para depuración
./run-preprod-tests-stay.sh

```


* **En Windows (PowerShell nativo):**
```powershell
# Levantar entorno preproducción con JARs precompilados
docker compose -f docker-compose.preprod.yml up --build -d

# Ejecutar integración con maven apuntando a integration-tests
mvn test -pl integration-tests -DskipTests=false

# Detener y limpiar recursos
docker compose -f docker-compose.preprod.yml down -v

```



### 11.3 Protocolo de Ejecución en Modo Degradado (Sin Acceso a Docker Daemon)

Si el entorno de ejecución del agente carece de socket Docker accesible o el agente no logra levantar por su cuenta el daemon de Docker:

1. **Ejecución Obligatoria:** Completar rigurosamente **Gate 1** y **Gate 2** mediante Maven nativo local.
2. **Prohibición de Simulación Falsa:** Jamás clasificar Gate 3 o Gate 4 como superados (`VERIFIED`) si la infraestructura requerida no estuvo activa.
3. **Marcado Formal:** Registrar los tests omitidos bajo el estado explícito `[DEFERRED_NO_DOCKER]` en la sección de Quality Gates del reporte de Fase 7, indicando el comando pendiente para ejecución humana en un entorno completo.
4. **Validación Compensatoria:** Maximizar la validación de contratos y DTOs a nivel de tests unitarios de serialización Jackson y validadores de beans en memoria sin depender de RabbitMQ ni contenedores.

---

## 12. Checklist de Cierre y Criterio de Parada

Antes de dar por concluida cualquier interacción o propuesta técnica, verificar:

* [ ] **Fuentes y memoria respetadas:** ¿La solución es coherente con los ADRs y se mantuvo intacta la documentación histórica?
* [ ] **Baseline verificado:** ¿Se identificaron y aislaron claramente los fallos preexistentes (`BASELINE_RED`)?
* [ ] **Invariantes protegidas:** ¿Se preservaron las guardas de estado y la pureza del dominio sin dependencias JPA prematuras?
* [ ] **Límites de bounded context intactos:** ¿Se respetó el criterio de pertenencia de `common-lib` y el desacoplamiento por UUIDs?
* [ ] **Seguridad e integridad preservada:** ¿Se evitaron secretos/PII en código y se mantuvieron intactas las aserciones de tests sin degradar umbrales?
* [ ] **Pre-flight SonarCloud verificado:** ¿El código nuevo cumple con las condiciones del Quality Gate (Technical Debt = 0, Condition Coverage $\ge 80\%$) consultando `docs/IA/07-errores-frecuentes-sonarcloud-ia.md`?
* [ ] **Alcance respetado:** ¿Se aplicó el cambio mínimo suficiente sin introducir dependencias no autorizadas ni refactors colaterales?
* [ ] **Revisión Crítica Adversarial completada (Fase 6):** ¿Se ejecutó la auditoría independiente con el Subagente Revisor (o fallback) y se aplicaron las correcciones detectadas?
* [ ] **Grafo documental íntegro y sincronizado:** ¿Todo documento creado, modificado o renombrado está registrado en su índice local, en `docs/README.md` y en `docs/ESTADO_DOCUMENTACION.md` sin dejar archivos huérfanos?
* [ ] **Contratos y eventos verificados:** ¿Se categorizó el impacto en contratos y se garantizó compatibilidad aditiva en eventos AMQP?
* [ ] **Quality Gates superados o clasificados:** ¿Pasaron los tests correspondientes y se ejecutó `mvn spotless:check`? En ausencia de Docker, ¿se declaró formalmente `[DEFERRED_NO_DOCKER]`?
* [ ] **Reporte emitido bajo plantilla modular (Fase 7):** ¿La entrega final sigue la estructura estandarizada de la Fase 7 respetando la taxonomía epistémica y detallando las correcciones de la Fase 6?

---

## 13. Política de Modificación y Gobernanza de `AGENTS.md`

`AGENTS.md` es un artefacto de gobernanza controlada y congelada.

Los agentes y desarrolladores **no deben modificarlo como parte incidental de una tarea de desarrollo, refactorización, corrección de bugs o actualización documental**.

Toda modificación de `AGENTS.md` debe cumplir obligatoriamente:

1. **Aislamiento de Tarea:** Realizarse exclusivamente mediante un commit/PR dedicado e independiente.
2. **Justificación Normativa:** Explicitar qué regla se altera, el motivo técnico y el impacto en los workflows.
3. **Versionado Semántico (SemVer):** Actualizar el encabezado de versión (`MAJOR` para cambios estructurales de gobierno, `MINOR` para nuevas directivas, `PATCH` para correcciones tipográficas).
4. **Preservación de Coherencia:** Mantener compatibilidad con los ADRs, requisitos de cátedra e invariantes vigentes.
5. **Revisión Humana Mandatoria:** Recibir aprobación humana explícita antes de integrarse a la rama principal.

* **`[INVARIANT]` Prohibición de Elusión Normativa:** Un agente nunca debe modificar `AGENTS.md` para eludir una restricción, enmascarar un fallo de Quality Gate o ampliar artificialmente el alcance autorizado de una tarea.