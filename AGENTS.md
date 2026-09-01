# AGENTS.md — Engineering & Architecture Rules

> **DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones**
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5
> **Versión:** 3.5.0 (Gobernanza de ADRs en proposed, Rúbrica de Benchmark y Trazabilidad Histórica)
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
* **`[INVARIANT]` Criterio de `common-lib` (Shared Kernel):** Solo pertenece a `common-lib` lo que es genuinamente compartido por múltiples bounded contexts y semánticamente neutro respecto a cualquier dominio específico. Prohibido introducir entidades de negocio, DTOs o lógica particular. Detalle: [`docs/arquitectura/shared-kernel.md`](docs/arquitectura/shared-kernel.md).
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

## 7. Protocolo de Trabajo del Agente en 7 Fases

El agente debe estructurar su trabajo en las siguientes fases secuenciales:

```text
FASE 1          FASE 2          FASE 3          FASE 4          FASE 5          FASE 6                 FASE 7
Descubrimiento ➔ Análisis      ➔ Diseño        ➔ Implementación ➔ Validación   ➔ Revisión Crítica     ➔ Reporte
(Git/Baseline)  (OBS/INF/DOC)  (PROP/Trade)   (Mínimo suf.)   (Quality Gates) (Subagente Revisor)    (Modular)

```

### Fase 1: Descubrimiento y Baseline

Inspeccionar el repositorio (`git status`, `git log -n 5 --oneline`). Ejecutar los tests antes de modificar. Establecer `BASELINE_GREEN` o `BASELINE_RED`; si es RED, aislar los fallos preexistentes en el reporte para no atribuirlos a la modificación en curso.

### Fase 2: Análisis Estructural

* Separar hechos observados (`[OBSERVED]`) de inferencias (`[INFERRED]`) y alternativas descartadas (`[REJECTED]`).
* Identificar restricciones, dependencias y riesgos. **No modificar código durante esta fase.**
* **Evaluación de Triggers de ADRs:** Contrastar el cambio o hallazgo contra la *Matriz de Triggers Mandatorios* (Sección 9.1). Si encuadra en cualquiera de los 9 vectores arquitectónicos, catalogar el hallazgo como `[PROPOSED]` para su formalización en Fase 3.

### Fase 3: Propuesta de Diseño

* Presentar la solución técnica justificando responsabilidades y trade-offs.
* Verificar compatibilidad con los ADRs e invariantes arquitectónicas.
* **Formalización de ADRs en `proposed`:** Si en Fase 2 se activó un trigger (§9.1), redactar el ADR en `docs/adr/<servicio>/YYYYMMDD-<slug>.md` y registrarlo en `docs/adr/DEUDA_TECNICA.md`. Ver política completa en §9.
* **Regla de Bifurcación Temporal (Presente vs Futuro):** Si el ADR resuelve la tarea actual, implementar en Fase 4; si corresponde a entregas futuras, proponer y catalogar sin alterar el código actual.
* **Interacción Humana Calibrada:** Solicitar confirmación del usuario **únicamente si el cambio modifica o supera el alcance originalmente autorizado**.

### Fase 4: Implementación Quirúrgica

* Aplicar el cambio mínimo suficiente respetando la encapsulación y los contratos vigentes.
* Mantener consistencia de nombres y estilo de código.
* **Auto-revisión de Code Smells:** Durante la codificación, consultar obligatoriamente la guía de errores frecuentes y *smells* recurrentes en [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](docs/IA/07-errores-frecuentes-sonarcloud-ia.md) para evitar violaciones estáticas comunes (métodos que deben ser `static`, constructores privados en utilitarios, `@Override`, literales repetidos, shadowing de variables).

### Fase 5: Validación Gradual (Quality Gates)

* Ejecutar los Quality Gates correspondientes al alcance (Sección 11).
* **Verificación de Cobertura Condicional y Pre-Flight Sonar:** Asegurar que todo nuevo camino lógico (`if`, `switch`, ternarios, `Optional`) cuente con pruebas unitarias para todas sus bifurcaciones (*Condition Coverage $\ge 80\%$*), y verificar el cumplimiento del checklist pre-flight de [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](docs/IA/07-errores-frecuentes-sonarcloud-ia.md).
* Comprobar formato y estilo de código (`mvn spotless:check`).

### Fase 6: Revisión Crítica Adversarial y Refinamiento

Para erradicar el sesgo de auto-confirmación (*confirmation bias*), el agente **no debe dar por concluida su tarea sin someter su entrega a una auditoría independiente**.

* **Invocación Mandatoria de Subagente Revisor:**  
El agente principal debe invocar mediante la herramienta `invoke_subagent` a un subagente independiente de sólo lectura (`Role: Revisor Crítico Adversarial`, `TypeName: research` o `self`) enviándole el `git diff` de la rama y el objetivo de la tarea.
* **Vectores de Auditoría Obligatorios:** El subagente revisor debe auditar la entrega contra cuatro fuentes normativas:
1. [`docs/auditoria/plan-revisor-critico.md`](docs/auditoria/plan-revisor-critico.md): Falsación activa, búsqueda de casos borde no cubiertos y violación de invariantes de agregados.
2. [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](docs/IA/07-errores-frecuentes-sonarcloud-ia.md): Detección de *code smells* de SonarCloud (visibilidad JUnit 5, `@Override`, `static` en utilitarios, duplicación de strings, imports sobrantes).
3. [`docs/ESTADO_DOCUMENTACION.md`](docs/ESTADO_DOCUMENTACION.md) y [`docs/README.md`](docs/README.md): Comprobación de integridad del grafo documental (ausencia de documentos huérfanos e índices desincronizados).
4. **Rúbrica de Benchmark de Calidad de ADRs (Sección 9.5):** Si la entrega incluye o modifica ADRs, calificar cada documento de 1 a 5 en sus 4 dimensiones, exigiendo un promedio ponderado $\ge \mathbf{4.0 / 5.0}$ para autorizar el pase a Fase 7.
* **Ciclo de Corrección Inmediata:**  
El subagente revisor emite un informe estructurado con los defectos u omisiones detectadas. El agente principal **debe aplicar inmediatamente las correcciones pertinentes dentro del alcance autorizado** y re-ejecutar Gate 1 (`mvn spotless:check` / `mvn test`) antes de proceder a la siguiente fase.
* **Modo Fallback Monoproceso (Sin Soporte de Subagentes):**  
Si el entorno carece de la herramienta `invoke_subagent` o no soporta subagentes concurrentes, el agente principal **debe adoptar explícitamente el rol de auditor escéptico** en un bloque de razonamiento aislado de su propia traza, evaluando los 4 vectores anteriores con el máximo rigor crítico antes de emitir el reporte final.

### Fase 7: Reporte y Entrega

* **Paso Previo de Pre-Cierre Documental:** Antes de redactar la lista de *Archivos Modificados*, si la tarea involucró creación, edición o reubicación de documentos en `docs/`, verificar obligatoriamente que `docs/README.md` y `docs/ESTADO_DOCUMENTACION.md` se encuentren 100% sincronizados.
* **Mandatorio:** Emitir reporte de cierre con la plantilla estandarizada disponible en [`docs/IA/04-checklist-antes-de-pr.md`](docs/IA/04-checklist-antes-de-pr.md).


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

## 9. Gobernanza de Decisiones de Arquitectura (ADRs), Ciclo de Vida y Deuda Técnica

### 9.1. Obligatoriedad de Formalización de ADRs en Estado `proposed`
* **`[INVARIANT]` Registro Obligatorio de Decisiones de Diseño:** Toda decisión de diseño de alto nivel identificada o introducida durante el ciclo de vida del software debe documentarse formalmente como un Registro de Decisión de Arquitectura (ADR) en estado `Status: proposed` bajo formato Log4brains / MADR.
* **`[CONSTRAINT]` Triggers Mandatorios:** Es mandatorio redactar un ADR propuesto ante cualquiera de las siguientes circunstancias:
  1. **Domain-Driven Design (DDD):** Alteración de límites de agregados, entidades raíz o desacoplamiento de identidades (`UUID`).
  2. **Persistencia y ORM:** Estrategias de herencia relacional JPA/Hibernate, esquemas PostgreSQL o surrogate keys para anonimización.
  3. **Contratos Públicos:** Creación o modificación de endpoints REST (OpenAPI/Feign) o eventos AMQP (RabbitMQ).
  4. **Concurrencia y Schedulers:** Configuración de tareas `@Scheduled`, coordinadores distribuidos (ShedLock) o pools `@Async`.
  5. **Shared Kernel (`common-lib`):** Inclusión o modificación de clases transversales compartidas.
  6. **Responsabilidades de Capas:** Reubicación de lógica de negocio o segregación de servicios que violen el Principio de Responsabilidad Única (SRP).
  7. **Seguridad y Privacidad:** Tratamiento de datos sensibles (PII), ofuscación o Crypto-Shredding.
  8. **Almacenamiento e Infraestructura:** Integración de storage S3 (MinIO) o drivers de infraestructura.
  9. **Testing de Persistencia:** Adopción de frameworks como Testcontainers frente a bases simuladas.

* **Regla de Exclusión (Anti-Micro-ADRs):** No se redactan ADRs para cambios cosméticos, corrección de bugs puntuales que no alteren contratos ni arquitectura, optimizaciones internas de algoritmos que respeten la interfaz pública, o formateo de código con Spotless.

### 9.2. Flujo Operativo de No Bloqueo (Aprobación Exclusiva por Pull Request)
* **`[INVARIANT]` No Bloqueo en Chat:** El agente de IA **nunca debe detener la ejecución interactiva** para solicitar aprobación en el chat ante un ADR propuesto. El agente formula la propuesta técnica completa (con alternativas viables y trade-offs), vincula el registro correspondiente en `docs/adr/DEUDA_TECNICA.md`, implementa el cambio mínimo necesario (o pospone formalmente el impacto si corresponde a entregas posteriores), y somete el ADR para aprobación humana en la Pull Request de GitHub.
* **`[INVARIANT]` Autoridad de Estados:** El estado `Status: accepted` está reservado exclusivamente para decisiones aprobadas formalmente por un revisor humano al integrar la PR, o decisiones preexistentes plenamente verificadas en la rama principal. Ningún agente puede auto-promover un ADR a `accepted` si la decisión introduce un cambio de diseño inédito.

### 9.3. Ciclo de Vida y Preservación de la Memoria Histórica
* **`Status: proposed`:** Estado inicial de toda nueva propuesta arquitectónica generada por agentes.
* **`Status: accepted`:** Decisión aprobada y adoptada oficialmente en el código fuente.
* **`Status: rejected`:** Alternativa evaluada y formalmente descartada. **Prohibido borrar el archivo**; debe preservarse en `docs/adr/` documentando el análisis técnico que motivó el rechazo para prevenir reincidencias.
* **`Status: superseded by [nuevo-adr.md]`:** Decisión histórica adoptada en el pasado que fue superada por una nueva arquitectura:
  - **`[INVARIANT]` Inmutabilidad del Cuerpo Histórico:** Queda terminantemente prohibido alterar los argumentos y deliberaciones originales del documento antiguo.
  - La superación se formaliza únicamente actualizando el encabezado a `Status: superseded by [...]` y añadiendo una nota de contexto histórica fechada en la parte superior del documento.
  - El nuevo ADR sucesor debe explicar las causas y lecciones que condujeron a la superación.

### 9.5. Rúbrica de Benchmark de Calidad en Fase 6 (Subagente Revisor)
Durante la Fase 6 de Revisión Crítica Adversarial, el Subagente Revisor debe auditar todo ADR propuesto asignando una calificación del 1 al 5 en cuatro dimensiones:
1. *Profundidad Técnica y Alternativas* (mínimo 2 alternativas reales analizadas con pros y contras sólidos).
2. *Adherencia a Principios Académicos* (justificación basada en SOLID, GRASP, GoF y DDD).
3. *Factibilidad Operativa* (realismo técnico y compatibilidad con stack Java 21 / PostgreSQL / Docker).
4. *Factualidad y Enlaces* (citas de clases, interfaces, métodos y links 100% operativos sin alucinaciones).

Todo ADR propuesto debe promediar $\ge \mathbf{4.0 / 5.0}$ para autorizar el cierre de la tarea.

### 9.6. Estructura y Campos Canónicos Obligatorios (Formato MADR)
Todo nuevo ADR debe respetar la estructura estándar MADR / Log4brains:
* **Cabecera YAML/Markdown:** `Status: proposed`, `Date: YYYY-MM-DD`, `Deciders: Decisión Grupal`, `Tags: [...]`.
* **Contexto y Problema:** Descripción de la tensión técnica o requerimiento con referencias exactas a clases del proyecto.
* **Atributos de Calidad y Drivers:** Factores clave de decisión (ej. Mantenibilidad, DDD, Persistencia Entrega 4).
* **Alternativas Consideradas:** Mínimo 2 alternativas viables además de la propuesta.
* **Resultado de la Decisión:** Alternativa elegida con justificación y análisis de consecuencias positivas/negativas.
* **Análisis Detallado:** Pros y contras técnicos por cada alternativa analizada.
* **Origen y Deuda Técnica:** Oleada/tarea de origen y vínculo a `docs/adr/DEUDA_TECNICA.md`.

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

Antes de dar por concluida cualquier interacción o propuesta técnica, verificar:

* [ ] **Baseline verificado:** fallos preexistentes identificados y aislados
* [ ] **Tests intactos:** sin aserciones debilitadas, gates alterados ni builds forzados
* [ ] **Pre-flight SonarCloud ejecutado:** consultar [`docs/IA/07-errores-frecuentes-sonarcloud-ia.md`](docs/IA/07-errores-frecuentes-sonarcloud-ia.md)
* [ ] **Revisión Crítica Adversarial completada** (Fase 6)
* [ ] **ADR formalizado** si se activó un trigger §9.1; ninguno auto-promovido a `accepted`
* [ ] **Grafo documental sincronizado:** `docs/README.md`, `docs/ESTADO_DOCUMENTACION.md` y `docs/adr/DEUDA_TECNICA.md` según alcance
* [ ] **Quality Gates superados** o `[DEFERRED_NO_DOCKER]` declarado formalmente
* [ ] **Reporte emitido** con plantilla de [`docs/IA/04-checklist-antes-de-pr.md`](docs/IA/04-checklist-antes-de-pr.md)

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