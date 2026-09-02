# AGENTS.md — Engineering & Architecture Rules

> **DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones**
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión:** 6.2.0 (Oleada 8 — Nested AGENTS: `common-lib/AGENTS.md` — Shared Kernel governance)
> **Ámbito:** Obligatorio e inmutable para agentes de IA y desarrolladores.

---

> [!NOTE]
> Este es el archivo canónico de instrucciones para agentes del repositorio DonaTrack. *This is the canonical vendor-neutral agent instruction file for this repository.*

---

## 1. Propósito y Alcance Operativo

Este documento establece las **reglas de gobierno, invariantes arquitectónicas, protocolo de razonamiento y criterios de validación** que rigen la interacción con el repositorio DonaTrack.

`AGENTS.md` **no es un inventario ni una wiki documental**; actúa como el **núcleo de políticas operativas**. Los detalles de diseño, requerimientos de cátedra y decisiones históricas residen en sus respectivos documentos en `docs/`.

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

* **`[INVARIANT]` Patrones Arquitectónicos Establecidos:** Los patrones de diseño que gobiernan la lógica de negocio de un servicio (State Machine, Template Method, Strategy, etc.) no deben reemplazarse silenciosamente. Antes de modificar un patrón existente: cargar el contexto del servicio desde [`docs/context-index.md`](docs/context-index.md), demostrar equivalencia mediante tests y formalizar un ADR.

* **`[CONSTRAINT]` Pureza del Dominio:** Las entidades y agregados de dominio no deben acoplarse a DTOs de transporte, clientes HTTP ni detalles de infraestructura de persistencia. Constraints temporales activas por servicio: [`docs/context-index.md`](docs/context-index.md).

### 4.2 Arquitectura de Capas y Microservicios

* **`[INVARIANT]` Controladores como Adaptadores Puros:** Los `Controllers` son adaptadores de entrada HTTP. Su única responsabilidad es recibir requests, validar DTOs de frontera, delegar en Application Services y mapear respuestas. No deben contener lógica de dominio ni orquestación compleja.
* **`[INVARIANT]` Criterio de `common-lib` (Shared Kernel):** Solo pertenece a `common-lib` lo genuinamente compartido, cross-cutting y semánticamente neutro respecto a cualquier dominio. Para trabajo bajo `common-lib/`, aplicar también [`common-lib/AGENTS.md`](common-lib/AGENTS.md).
* **`[CONSTRAINT]` Comunicación Inter-Servicios:** Respetar los mecanismos de comunicación ya establecidos en el repositorio. No introducir nuevos canales de comunicación entre servicios sin ADR. Detalle de contratos por servicio: [`docs/context-index.md`](docs/context-index.md).

* **`[INVARIANT]` Trazabilidad e Idempotencia:** Todo flujo distribuido debe preservar trazabilidad (propagación de `traceId`) e idempotencia en el procesamiento de eventos distribuidos. Detalle operativo: [`docs/arquitectura/logging-trazabilidad.md`](docs/arquitectura/logging-trazabilidad.md).

### 4.3 Seguridad Operacional e Integridad de Pruebas

* **`[CONSTRAINT]` Seguridad y Datos Sensibles:** Prohibido introducir credenciales, tokens, contraseñas hardcodeadas o datos personales reales (PII) en código, tests, fixtures, logs o reportes. Toda prueba debe utilizar datos sintéticos.
* **`[INVARIANT]` Integridad de Quality Gates y Pruebas:** Queda terminantemente prohibido deshabilitar tests, eliminar o debilitar aserciones (`assertTrue(true)`), aumentar tolerancias arbitrariamente o alterar linters/CI para enmascarar regresiones o forzar un build verde.
* **`[INVARIANT]` Calidad Estática y Cumplimiento de SonarCloud:** Todo cambio en código Java o configuración de CI/CD debe superar el Quality Gate de SonarCloud. Queda prohibido introducir *code smells* o scripts vulnerables. Ejecutar auto-auditoría pre-flight con [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](docs/IA/07-errores-frecuentes-sonarcloud-ia.md) antes de finalizar.

---

## 5. Criterios de Evaluación y Atributos de Calidad (Fitness Checks)

Antes de proponer un cambio, verificar los Fitness Checks operacionales en [`docs/arquitectura/principios-diseno-arquitectura.md`](docs/arquitectura/principios-diseno-arquitectura.md).

---

## 6. Control de Alcance (Anti-Scope Creep)

* **`[CONSTRAINT]` Alcance Estrictamente Delimitado:** Una tarea autoriza cambios únicamente sobre los módulos y clases directamente vinculados a su objetivo.
* **`[CONSTRAINT]` Prohibición de Refactorings Oportunistas No Autorizados:** Si durante la inspección se descubren errores secundarios, antipatrones o deuda técnica no relacionada con la tarea en curso:
1. **No corregirlos en la misma iteración**, salvo que el hallazgo bloquee la tarea actual o comprometa críticamente la integridad.
2. Documentarlos formalmente en el reporte de salida como hallazgo o deuda técnica a tratar.
* **`[INVARIANT]` Integridad del Grafo Documental y Excepción de Alcance:** Cuando una tarea cree, traslade, renombre o modifique artefactos arquitectónicos, especificaciones o guías en `docs/`, la actualización sincronizada de `docs/README.md`, `docs/ESTADO_DOCUMENTACION.md` y (cuando corresponda) `docs/adr/DEUDA_TECNICA.md` **es mandatoria y forma parte indivisible del alcance autorizado**. Esta sincronización NO constituye refactoring oportunista ni *scope creep*, sino higiene obligatoria de la entrega.
* **`[CONSTRAINT]` Control de Dependencias Externas:** No incorporar nuevas librerías en `pom.xml` ni alterar imágenes de Docker sin justificación técnica explícita y aprobación previa.
* **`[CONSTRAINT]` Minimalismo Suficiente:** Aplicar el **cambio mínimo suficiente** para cumplir el objetivo funcional o arquitectónico, evitando reescrituras estéticas.

---

## 7. Protocolo de Trabajo — Core Workflow y Niveles de Tarea

El agente estructura su trabajo en un **Core Workflow de 7 pasos** ejecutado con **profundidad variable** según el nivel de impacto clasificado en Fase 0.

### 7.0 Clasificación de Nivel

> **Regla conservadora:** ante duda entre dos niveles, clasificar el superior.

```text
¿Cumple TODOS los criterios QUICK? → SÍ: QUICK
                                   → NO: ¿Activa señal ARCHITECTURAL? → SÍ: ARCHITECTURAL / NO: STANDARD
```

**QUICK:** no cambia runtime · no cambia contratos · no cambia arquitectura · no cambia tests funcionales · mecánico y local · sin fase formal de diseño. Si modifica código Java más allá de comentarios/Javadoc → mínimo STANDARD. Si requiere comparar alternativas o producir propuesta → mínimo STANDARD.

**Señales ARCHITECTURAL** — cualquier señal basta:
- modifica límites de bounded context o módulos
- cambia contrato público (REST, AMQP, Feign, eventos, DTOs compartidos)
- cambia estrategia de persistencia o datos
- cambia mecanismos de seguridad o privacidad
- introduce tecnología o dependencia estructural nueva
- cambia comunicación sync ↔ async
- modifica `common-lib` estructuralmente
- reemplaza patrón arquitectónico establecido
- implica decisión costosa de revertir
- tiene impacto cross-service relevante

> La clasificación de Task Level y el requisito de ADR son **ejes independientes**. Una tarea puede ser ARCHITECTURAL sin requerir nuevo ADR (cuando implementa una decisión ya aceptada). Un ADR es obligatorio solo cuando Gate A + Gate B de §9.1 aplican, independientemente del Task Level.

**STANDARD:** todo lo que no es QUICK ni ARCHITECTURAL. Cambio funcional o estructural acotado, sin señales arquitectónicas, reversible localmente.

### 7.1 Regla Anti-Downgrade

> El nivel se determina por el **mayor impacto descubierto**, no por el tamaño del diff ni la intención inicial.

La clasificación puede escalar (QUICK → STANDARD → ARCHITECTURAL) con nueva evidencia. No se reduce el nivel sin evidencia que demuestre explícitamente que la señal no aplica. Al escalar, reevaluar contexto, validación y reporte.

### 7.2 Core Workflow

```text
Paso 1: Clasificar impacto          → QUICK / STANDARD / ARCHITECTURAL
Paso 2: Cargar contexto necesario   → proporcional al nivel
Paso 3: Establecer baseline         → proporcional; no modificar código en esta fase
Paso 4: Realizar cambio mínimo      → implementación quirúrgica
Paso 5: Validar proporcionalmente   → gates según nivel e impacto real
Paso 6: Revisar proporcionalmente   → profundidad según nivel
Paso 7: Reportar evidencia          → proporcional al nivel
```

Core Invariantes (aplican en todos los niveles): epistemic labels · seguridad · integridad de tests · anti-scope creep · sincronía documental · SonarCloud pre-flight para código Java.

### 7.3 Profundidad por Nivel

> *Validation breadth follows impact, not task label alone.*

| | QUICK | STANDARD | ARCHITECTURAL |
|---|---|---|---|
| **Contexto** | Solo si necesario; skip `context-index` para cambios triviales | [`docs/context-index.md`](docs/context-index.md) | context-index + servicio + transversal + ADRs |
| **Baseline** | `git status`; omitir tests si no hay código ejecutable afectado | `mvn test -pl <modulo>`; registrar BASELINE_GREEN / RED | Full baseline + scan cross-service; registrar BASELINE_GREEN / RED |
| **Validación** | Solo lo directamente afectado; sin tests si no hay código ejecutable | `mvn clean test -pl <modulo> -am`; escalar a reactor cuando: `common-lib`, múltiples módulos, contrato/config compartida, impacto transversal | Amplia; reactor cuando corresponda; Gate 3/4 solo si técnicamente relevantes y ejecutables (§11) |
| **Spec** | No | Mínimo si objetivo no trivial (`Goal / Scope / Constraints / Validation`) | Obligatorio antes de implementar; relación spec–ADR: [`docs/adr/README.md`](docs/adr/README.md) |
| **Revisión** | `LIGHTWEIGHT_CLOSING_CHECK` | `REVIEW_REQUIRED` | `ENHANCED_REVIEW_REQUIRED` |
| **Reporte** | Qué cambió + qué se validó | Scope · baseline · cambios · validación · riesgos | Completo: spec · impacto · alternativas · ADR (§9) · validación · review · riesgos · decisiones pendientes |

### 7.4 Fase 6 — Revisión Crítica

Profundidad según §7.3. Ninguna tarea se da por concluida sin el output de revisión correspondiente.

* **QUICK → `LIGHTWEIGHT_CLOSING_CHECK`:** el Generator completa un chequeo de cierre proporcional (scope/diff, validaciones ejecutadas, evidencia, result `PASS` o `ESCALATE_TO_STANDARD`). No requiere Review Contract.
* **STANDARD → `REVIEW_REQUIRED`:** emitir Review Contract. Usar `INDEPENDENT_REVIEW` si la herramienta soporta un contexto secundario independiente; `SELF_REVIEW` explícito en caso contrario.
* **ARCHITECTURAL → `ENHANCED_REVIEW_REQUIRED`:** emitir Review Contract ampliado. `INDEPENDENT_REVIEW` fuertemente preferido. Si no está disponible: `SELF_REVIEW` con `[SELF_REVIEW_FALLBACK]` declarado en el Reporte de Fase 7.

**Evaluator — SOURCE_READ_ONLY + NON_DESTRUCTIVE_VERIFICATION:** no modifica código fuente, documentación ni configuración durante la evaluación. Puede ejecutar verificaciones no destructivas cuando la herramienta lo permita (tests focalizados, Maven checks, `git diff`, `grep`, inspección de archivos, linters, validación de links). Si no puede ejecutar un check: declarar `[TESTS_NOT_EXECUTED_BY_EVALUATOR]`.

**Capability detection:** `INDEPENDENT_REVIEW` disponible cuando existe un contexto secundario que no depende del razonamiento previo del Generator y recibe solo artefactos explícitos. En todos los demás casos: `SELF_REVIEW`. Nunca etiquetar `SELF_REVIEW` como independiente.

Aplicar correcciones de findings BLOCKING y re-ejecutar Gate 1 antes de Fase 7.

Política completa, Review Contract y vectores V1–V9: [`docs/IA/review/evaluator.md`](docs/IA/review/evaluator.md).


---

## 8. Política de Refactoring y Evolución de Contratos

### 8.1 Criterio de Refactor Válido

Todo refactor estructural debe regirse por el principio de preservación:

1. **Establecer Baseline:** Identificar el estado inicial (`GREEN` o `RED`).
2. **Preservar Contratos:** Mantener la firma de endpoints, DTOs y payloads de eventos AMQP.
3. **Validación Empírica:** No considerar exitoso un refactor únicamente porque compile; se requiere validación objetiva mediante tests.

### 8.2 Tipificación de Cambios en Contratos y Eventos

* **Refactoring:** Preserva estrictamente los contratos públicos y payloads existentes.
* **Feature:** Permite evolución retrocompatible de contratos REST y eventos AMQP (cambios estrictamente **aditivos**: campos nuevos opcionales; nunca renombrar ni eliminar campos sin ciclo de migración).
* **Breaking Change:** Requiere justificación formal mediante ADR, actualización de suites de integración y aprobación explícita.

---

## 9. Gobernanza de ADRs

### 9.1 Cuándo crear un ADR — Two-Gate Rule

Crear un ADR `proposed` solo cuando se cumplen **ambos**:

**Gate A — Decision novelty:** ¿La tarea introduce una decisión arquitectónica **nueva**?

No hay decisión nueva cuando:
- la tarea implementa un ADR ya aceptado;
- se agrega un endpoint dentro de un contrato o patrón ya decidido;
- se agrega un consumer siguiendo la arquitectura de mensajería adoptada;
- se aplica un patrón de diseño existente en el proyecto a un nuevo caso.

Si NO hay decisión nueva → no crear ADR. Si SÍ → Gate B.

**Gate B — Architectural significance:** ¿La nueva decisión es arquitectónicamente significativa?

Evaluar el impacto real. Señales (no son triggers mecánicos; son criterios de evaluación):
- afecta comunicación o integración entre servicios
- cambia estrategia de persistencia o modelo de datos
- introduce tecnología, framework o dependencia estructural nueva
- modifica límites entre bounded contexts, módulos o capas
- implica trade-off deliberado entre atributos de calidad
- tiene alto costo de reversión
- reemplaza un ADR aceptado o un patrón arquitectónico central al dominio
- tiene implicancias de privacidad, seguridad o compliance

`NEW DECISION + ARCHITECTURAL SIGNIFICANCE = ADR`

### 9.2 Cuándo NO crear ADR

Bugs sin impacto en contratos · cambios cosméticos o de formateo · DTOs internos sin impacto externo · tests con herramientas ya adoptadas · endpoints o consumers que siguen patrones establecidos · refactors locales sin cambio de límites ni contratos · implementación de una decisión ya documentada.

### 9.3 Lifecycle y autoridad

- **`proposed`:** Creado por agente o desarrollador cuando Gate A + Gate B aplican. Implementar sobre un ADR `proposed` es posible (no-blocking), pero conlleva el riesgo explícito de que si el ADR es rechazado, podría requerirse rollback.
- **`accepted`:** Solo revisor humano al integrar PR. **`accepted` ≠ `implemented`** — la aceptación depende de autoridad humana, no del estado del código.
- **`rejected`:** Solo revisor humano. El archivo se preserva para evitar ciclos de razonamiento repetitivos.
- **`superseded`:** El agente puede proponer la relación; la aprobación es humana.

**`[INVARIANT]` Prohibición absoluta de auto-promoción:** Ningún agente puede promover un ADR a `accepted` o `rejected` por ningún motivo, incluyendo código ya presente en main o decisión preexistente. Código existente ≠ aprobación arquitectónica. Si el agente descubre una decisión implementada pero no documentada: crear ADR `proposed` marcando `[OBSERVED] El código actual ya implementa esta decisión`; la aprobación queda a cargo del revisor humano.

### 9.4 ADR status ≠ implementation status

Son ejes independientes. Un ADR puede estar `accepted` sin que la implementación esté completa. Ver relación completa: [`docs/adr/README.md`](docs/adr/README.md).

### 9.5 Divergencia código / ADR

Si el código contradice un ADR `accepted`: reportar en Fase 7; no resolver unilateralmente. Es una violación arquitectónica o el ADR requiere un sucesor `proposed`.

### 9.6 Guía completa

[`docs/adr/README.md`](docs/adr/README.md) — criterio detallado, lifecycle, MADR format, spec vs ADR, status vs implementation, benchmark de calidad (Fase 6), bifurcación temporal.

---

## 10. Enrutador de Contexto

Para recuperar contexto específico de tarea: [`docs/context-index.md`](docs/context-index.md) — routing orientado a tareas de coding agents.

Para navegación del catálogo documental completo: [`docs/README.md`](docs/README.md).

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

### 11.1 Comandos Maven

```bash
mvn spotless:check                                     # Gate 1: formato
mvn test -pl <modulo-service> -Dtest=<NombreTest>     # Gate 1: test unitario
mvn clean test -pl <modulo-service> -am               # Gate 2: módulo + deps
mvn test                                              # Gate 2: suite completa
mvn test -pl integration-tests -DskipTests=false      # Gate 3: integración
```

### 11.2 Gate 4 — Entorno Docker E2E

```bash
./run-preprod-tests.sh          # Bash / WSL — ciclo automático
./run-preprod-tests-stay.sh     # Bash / WSL — infraestructura activa
```

Comandos PowerShell y variantes en [`docs/testing/integration-tests.md`](docs/testing/integration-tests.md).

### 11.3 Protocolo Modo Degradado (Sin Docker)

Sin Docker accesible: completar Gate 1 y Gate 2 con Maven nativo. No declarar Gate 3/4 como `[VERIFIED]` sin infraestructura activa. Registrar como `[DEFERRED_NO_DOCKER]` en el reporte.

---

## 12. Checklist de Cierre y Criterio de Parada

Antes de dar por concluida cualquier interacción o propuesta técnica, verificar. Aplicar cada ítem proporcionalmente al nivel de la tarea (§7.0).

* [ ] **Baseline verificado:** fallos preexistentes identificados y aislados (proporcional al nivel: §7.3)
* [ ] **Tests intactos:** sin aserciones debilitadas, gates alterados ni builds forzados
* [ ] **Pre-flight SonarCloud ejecutado:** para todo código Java, consultar [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](docs/IA/07-errores-frecuentes-sonarcloud-ia.md)
* [ ] **Revisión completada** según profundidad del nivel: `LIGHTWEIGHT_CLOSING_CHECK` / `REVIEW_REQUIRED` / `ENHANCED_REVIEW_REQUIRED` (§7.4 Fase 6)
* [ ] **ADR formalizado** si Gate A + Gate B de §9.1 aplican (nueva decisión + significancia arquitectónica); ningún ADR auto-promovido a `accepted`
* [ ] **Grafo documental sincronizado:** `docs/README.md`, `docs/ESTADO_DOCUMENTACION.md` y `docs/adr/DEUDA_TECNICA.md` según alcance
* [ ] **Quality Gates superados** según nivel (§7.3), o `[DEFERRED_NO_DOCKER]` declarado formalmente
* [ ] **Reporte emitido:** breve para QUICK; con plantilla de [`docs/IA/04-checklist-antes-de-pr.md`](docs/IA/04-checklist-antes-de-pr.md) para STANDARD y ARCHITECTURAL

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

---

## 14. Enforcement Mecánico del Harness

Propiedades objetivamente verificables de este harness son validadas automáticamente en CI y localmente.

Ejecutar: `node scripts/agent-check.js`