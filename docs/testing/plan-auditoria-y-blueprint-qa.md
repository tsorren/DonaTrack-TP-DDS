# Plan Maestro: Auditoría y Arquitectura Target de Testing y QA (DonaTrack)

> **Documento:** Especificación y Plan Operativo de Auditoría  
> **Ubicación:** `docs/testing/plan-auditoria-y-blueprint-qa.md`  
> **Rol Asignado:** Principal Systems Engineer & Lead QA Architect (Adversarial & Riguroso)  
> **Ámbito Operativo:** 100% Documental y Analítico (`SOURCE_READ_ONLY` en `src/`)  
> **Skill de Ejecución Prevista:** `/goal`  
> **Mandato Mandatorio:** GrepAI-First para el Agente Principal y todo Subagente  
> **Fecha de Diseño:** 2026-09-06  

---

## 1. Propósito y Alcance

Este documento formaliza el plan de trabajo detallado para ejecutar una **auditoría profunda, rigurosa, adversarial y comparativa del ecosistema actual de testing, aseguramiento de calidad (QA), pruebas de integración (`integration-tests`), frameworks, herramientas y componentes de prueba de la plataforma DonaTrack**.

Su propósito es sentar las bases conceptuales, técnicas y arquitectónicas (mediante especificaciones, matrices de hallazgos, blueprints y propuestas de ADRs en estado `proposed`) que guiarán los refactors, la evolución de los pipelines de CI/CD y las nuevas capacidades de prueba del monorepo en sprints futuros, **sin alterar el código fuente ejecutable en `*/src/` durante esta fase**.

### 1.1 Documentos de Auditoría Precedentes

Este plan **no parte de cero**. El repositorio cuenta con auditorías previas cuyo contenido debe reutilizarse como insumo y no duplicarse:

| Documento Precedente | Cobertura | Relación con esta Auditoría |
|---|---|---|
| [`docs/arquitectura/diseno/auditoria-final-proyecto.md`](../arquitectura/diseno/auditoria-final-proyecto.md) | Radiografía completa del dominio, ~2020 tests, 20 hallazgos priorizados | **Reutilizar** hallazgos de dominio y conteos de tests como baseline; **profundizar** la dimensión de QA y herramientas |
| [`docs/auditoria/plan-revisor-critico.md`](../auditoria/plan-revisor-critico.md) | Marco de evaluación en 5 ejes + 8 atributos de calidad + checklists | **Reutilizar** las rúbricas de evaluación como referencia; no duplicar la plantilla de hallazgos |
| [`docs/auditoria/revision-critica-devops-ci.md`](../auditoria/revision-critica-devops-ci.md) | Auditoría DevOps: hallazgos INC-01 a INC-05 sobre CI/CD, Docker Compose y scripts | **Integrar** hallazgo INC-01 (`mvn test` vs `mvn verify` en CI, que viola la segregación Surefire/Failsafe) directamente en el diagnóstico |
| Auditorías por servicio ([logística](../arquitectura/diseno/logistica/auditoria-final.md), [notificaciones](../arquitectura/diseno/notificaciones/auditoria-final.md)) | Fase 0 + auditoría final con checklists RF por servicio | **Reutilizar** el estado de tests por servicio; evitar re-auditar hallazgos ya formalizados |

---

## 2. Invariantes Fundamentales de Ejecución

1. **Modo Documental Estricto (`SOURCE_READ_ONLY` en código):**
   - Prohibido modificar archivos en `*/src/` o crear/editar código de producción o de tests durante esta auditoría.
   - Toda la entrega consiste en diagnósticos, matrices comparativas, diagramas de arquitectura de pruebas y propuestas formales en la documentación (`docs/testing/`, `docs/adr/`).
2. **Mandato GrepAI-Preferred para el Agente Principal y TODO Subagente:**
   - Para toda búsqueda semántica en el código, inspección de clases, herencias, anotaciones de frameworks (`@SpringBootTest`, `@Tag`, `@WebMvcTest`, `@DataJpaTest`) o rastreo de clientes API: **es obligatorio invocar herramientas de `grepai` (`grepai_search`, `grepai_trace_*`, `grepai_refs_*`, `grepai_rpg_*`) de forma primaria**.
   - Para conteos exhaustivos, inspección de archivos XML/YAML (`pom.xml`, `docker-compose.yml`) y verificaciones de existencia de archivos, se permiten herramientas complementarias (`grep_search`, `find_by_name`, `view_file`).
   - **Regla para Subagentes:** Al delegar tareas a subagentes, inyectar explícitamente la directiva: *"Tu herramienta primaria de exploración de código es GrepAI; no uses grep/find ciegos ni leas clases completas. Para XML/YAML y conteos exactos puedes usar herramientas complementarias."*.
3. **Inmutabilidad Histórica y Sincronización:**
   - No modificar registros históricos en `docs/entregas/` ni alterar ADRs ya aceptados en `docs/adr/`.
   - La creación de los artefactos en `docs/testing/` y `docs/adr/` exige la actualización sincronizada de `docs/README.md` y `docs/ESTADO_DOCUMENTACION.md`.
4. **Prohibición Absoluta de Auto-Promoción de ADRs:**
   - Todo ADR creado debe registrarse con estado `proposed`. Ningún agente puede auto-promover un ADR a `accepted`.

---

## 3. Marco Teórico y Estándares de la Industria

Los diagnósticos, matrices y propuestas deben fundamentarse en la contrastación explícita con la literatura seminal y estándares de ingeniería:

1. **Vladimir Khorikov (*Unit Testing: Principles, Practices, and Patterns*):**
   - Definición de unidad (Escuela Clásica / Detroit vs. Escuela de Londres / Mockista).
   - Los 4 pilares de un buen test: Protección contra regresiones, Resistencia a refactorings, Feedback rápido y Facilidad de mantenimiento.
   - Detección de *Fragile Tests*, *Over-mocking* y acoplamiento a detalles de implementación.
2. **Gerard Meszaros (*xUnit Test Patterns: Refactoring Test Code*):**
   - Taxonomía estricta de Test Doubles (Dummy, Stub, Spy, Mock, Fake).
   - Detección de Test Smells (*Obscure Test*, *Conditional Test Logic*, *Flaky Test*, *Mystery Guest*, *Test Run War*, *Resource Optimism*).
3. **Martin Fowler:**
   - Pirámide de Pruebas tradicional vs. Panal de Pruebas (*Testing Honeycomb*) en arquitecturas de microservicios.
   - Pruebas de Contrato impulsadas por el consumidor (*Consumer-Driven Contract Testing*).
   - Diferencia entre Pruebas de Componente (out-of-process mocks) vs. E2E de sistema distribuido.
4. **Google (*Software Engineering at Google* — Winters, Manshreck, Wright):**
   - Clasificación por tamaño (Small, Medium, Large tests) y aislamiento de red/procesos.
   - Eliminación de indeterminismo (*Flakiness*) y costo computacional en infraestructura CI.
5. **Estándares ISO/IEC/IEEE 29119 & IEEE 829:**
   - Niveles de cobertura, trazabilidad de requisitos, reproducibilidad e independencia de entornos de prueba.

---

## 4. Decisiones Técnicas y de Diseño Acordadas en Grill-Me

Durante la sesión de alineación técnica (`/grill-me`), se definieron las siguientes directrices arquitectónicas:

| Dimensión Técnica | Decisión Acordada | Estado Actual `[OBSERVED]` | Justificación y Estrategia de Mitigación |
|---|---|---|---|
| **Estructura Documental** | **Hiper-granular** bajo `docs/testing/auditoria/` | No existe aún; `docs/testing/` tiene 3 archivos | 5 documentos especializados + 1 índice `README.md`, evitando artefactos monolíticos inmanejables. |
| **Gobernanza de ADRs** | **4 ADRs Atómicos en `docs/adr/`** (`proposed`) | Ya existe ADR `20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md` (`proposed`) como predecesor del ADR #1 | Separación clara de responsabilidades: Ambientes efímeros, Contratos, Fitness Functions y Performance. |
| **Gestión de Entornos** | **Coexistencia en Dos Niveles** (*Testing Honeycomb*) | `notificaciones-service` ya usa `@Testcontainers` + `@DynamicPropertySource` con `postgres:16-alpine` en `RepositoriosJpaTest.java`; los otros 3 servicios usan persistencia en memoria sin Testcontainers | Evolución del patrón existente: migrar de `@DynamicPropertySource` manual a `@ServiceConnection` Spring Boot 3.1+, scripts SQL canónicos compartidos `01-init-schemas-roles.sql` y modo degradado para hosts sin Docker; Docker Compose preprod preservado para E2E distribuido y compatibilidad académica. |
| **Contratos Inter-Servicios** | **Enfoque Pragmático OpenAPI + JSON Schema + WireMock** | Existen 4 specs OpenAPI YAML en `docs/arquitectura/contratos/`, 11 JSON Schemas en `schemas/` y validador nativo en Node.js puro `scripts/validate-contracts.js`; `ContractIT.java` solo verifica existencia de paths (antipatrón *Green Smoke Contract*) | Evolución: agregar validación Java en tests con `swagger-request-validator` y `json-schema-validator` para contratos bidireccionales; complementar (no reemplazar) el validador Node.js existente en CI; stubs WireMock en clientes Feign. |
| **Fitness Functions y Calidad** | **ArchUnit Universal + Pitest Acotado** | 0 dependencias de ArchUnit o Pitest en `pom.xml`; adopción greenfield | ArchUnit integrado en `mvn test` para validar invariantes de `AGENTS.md` (controllers puros, pureza de dominio, convención Surefire/Failsafe); Pitest acotado a la lógica de dominio crítica (matching, state transitions y ranking) bajo perfil Maven dedicado (`-Pmutation-test`) y workflow opcional/nocturno. |
| **Pruebas de Rendimiento** | **Migración Total a k6** | `PerformanceStressIT.java` activo con bucle `for` secuencial (100-200 iteraciones, sin concurrencia real); 0 scripts k6 | Descartar el bucle secuencial de `PerformanceStressIT` en Java y migrar las pruebas de carga y estrés a scripts JavaScript de **k6** (Docker/CLI), con wrappers de compatibilidad en `run-preprod-tests.sh` para soportar `--groups performance`. |
| **Flujo de Ejecución** | **Autónomo End-to-End con `/goal`** | N/A (metodológico) | El agente ejecutará las 5 iteraciones metodológicas de forma continua, validando la evidencia con GrepAI hasta completar todos los entregables. |

---

## 5. Estructura Exhaustiva de Entregables

Al ejecutarse el plan, se generarán y sincronizarán los siguientes artefactos:

### A. Documentos de la Auditoría (`docs/testing/auditoria/`)

```text
docs/testing/auditoria/
├── README.md                     # Índice maestro, síntesis ejecutiva y mapa de navegación
├── 01-diagnostico-ejecutivo.md   # Radiografía del ecosistema actual y evaluación teórica
├── 02-matriz-antipatrones.md     # Matriz de hallazgos empíricos, smells y severidad
├── 03-estudio-comparativo.md     # Comparativa multidimensional de herramientas y trade-offs
├── 04-blueprint-target.md        # Arquitectura target de testing y diagramas Mermaid
└── 05-roadmap-migracion.md       # Plan de migración no disruptivo en 4 fases
```

1. **`README.md` (Índice Maestro):**
   - Propósito, marco institucional (UTN-FRBA DDS 2026), glosario de términos y mapa de lectura.
2. **`01-diagnostico-ejecutivo.md` (Diagnóstico del Ecosistema Actual):**
   - Radiografía factual de cada capa de pruebas en DonaTrack (Unit, Slice, Integration, E2E, Smoke, Contract, Performance).
   - Evaluación contra los 4 Pilares de Khorikov y la taxonomía Google SWE (Small, Medium, Large).
   - Diagnóstico del antipatrón *Inverted Pyramid* o *Ice Cream Cone*.
3. **`02-matriz-antipatrones.md` (Matriz de Hallazgos y Smells):**
   - Tabla exhaustiva con: Módulo/Componente, Anti-patrón (Meszaros/Fowler), Evidencia Factual (GrepAI), Riesgo/Impacto y Severidad (🔴 Crítico, 🟡 Deuda/Oportunidad, 🟢 Fortaleza).
4. **`03-estudio-comparativo.md` (Estudio Comparativo de Alternativas):**
   - Análisis comparativo profundo con tablas de criterios (Velocidad, Determinismo, Fidelidad, Mantenimiento, Curva de Adopción) para:
     1. *Ambientes:* Docker Compose vs. Testcontainers vs. In-Memory Slices.
     2. *Contratos:* OpenAPI + WireMock + JSON Schema vs. Pact vs. Spring Cloud Contract.
     3. *Fitness Functions:* ArchUnit vs. Checkstyle/Linters vs. Pitest (Mutation Testing).
     4. *Rendimiento:* JUnit secuencial (`PerformanceStressIT`) vs. k6 vs. Gatling vs. JMeter.
   - Detalle de consecuencias negativas y mitigaciones concretas para cada opción.
5. **`04-blueprint-target.md` (El Blueprint de la Arquitectura Target):**
   - Definición formal del nuevo Panal de Pruebas de DonaTrack.
   - Responsabilidades, tiempos máximos y herramientas por capa de prueba.
   - Convenciones de fixtures, Test Data Builders y aislamiento de datos.
   - Diagramas Mermaid de topología de ejecución local y flujos en CI/CD (GitHub Actions).
6. **`05-roadmap-migracion.md` (Roadmap de Migración No Disruptivo):**
   - Estrategia de migración fase a fase (Fase 1: Preparación; Fase 2: Fitness Functions y Slices; Fase 3: Contratos; Fase 4: Performance con k6).
   - Preservación de la compatibilidad con los scripts docentes (`./run-preprod-tests.sh`, `./run-preprod-tests-stay.sh`).
   - Criterios de aceptación (*Definition of Done*) por fase.

---

### B. Propuestas de ADRs Canónicos (`docs/adr/`)

Formato MADR canónico, estado `proposed`:

1. **`docs/adr/20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes.md`**
   - Adopción de Testcontainers en dos niveles: componentes por microservicio (`@ServiceConnection`), reteniendo Docker Compose para E2E distribuido.
   - **Predecesor:** [`20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md`](../adr/20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md) — evaluar si este ADR lo complementa o lo supersede.
2. **`docs/adr/20260906-estrategia-contratos-openapi-wiremock-y-esquemas-amqp.md`**
   - Verificación pragmática de contratos REST con OpenAPI + WireMock y serialización AMQP con JSON Schema sin brokers externos.
   - **Baseline existente:** 4 specs OpenAPI YAML, 11 JSON Schemas y validador nativo en Node.js puro `scripts/validate-contracts.js`. El ADR debe explicitar si la validación Java reemplaza o complementa al validador Node.js existente.
3. **`docs/adr/20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest.md`**
   - Integración de ArchUnit universal en `mvn test` y perfil Maven dedicado (`-Pmutation-test`) con Pitest para lógica de negocio crítica.
4. **`docs/adr/20260906-migracion-pruebas-rendimiento-a-k6.md`**
   - Migración total de pruebas de rendimiento a k6, deprecación de `PerformanceStressIT` y wrapper en scripts de entrega.

---

### C. Sincronización del Grafo Documental

- **`docs/README.md`**: Enlazar la nueva sección de Auditoría de Testing (`docs/testing/auditoria/`) y este plan maestro.
- **`docs/ESTADO_DOCUMENTACION.md`**: Catalogar formalmente los 5 nuevos documentos y los 4 ADRs propuestos en las tablas de auditoría correspondientes.

---

## 6. Metodología de Ejecución Paso a Paso (5 Fases Iterativas)

Al dispararse la tarea mediante la skill `/goal`, el agente completará secuencialmente:

```text
┌─────────────────────────────────────────────────────────────┐
│  Iteración 1: Relevamiento e Investigación Empírica         │
│  • Búsquedas GrepAI de anotaciones, mocks, ITs y configs    │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Iteración 2: Análisis Crítico y Contrastación Factual      │
│  • Evaluación contra Khorikov, Meszaros, Fowler y SWE       │
│  • Redacción de 01-diagnostico-ejecutivo.md                 │
│  • Redacción de 02-matriz-antipatrones.md                   │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Iteración 3: Planteo de Alternativas y Arquitectura Target │
│  • Redacción de 03-estudio-comparativo.md                   │
│  • Redacción de 04-blueprint-target.md (Diagramas Mermaid)  │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Iteración 4: Revisión Crítica Adversarial de la Propuesta  │
│  • Auto-revisión escéptica de riesgos y mitigaciones        │
│  • Redacción de 05-roadmap-migracion.md                     │
│  • Redacción de README.md en docs/testing/auditoria/        │
└──────────────────────────────┬──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│  Iteración 5: Formalización de ADRs y Sincronización Final  │
│  • Redacción de los 4 ADRs en docs/adr/ (MADR, proposed)    │
│  • Actualización de docs/README.md y ESTADO_DOCUMENTACION.md│
│  • Chequeo de consistencia referencial y de enlaces         │
└─────────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> **Circuit Breaker Post-Iteración 1 (QW-08):** Si la Iteración 1 revela divergencias significativas con las premisas del §4 (ej. tecnologías ya adoptadas no previstas, conteo de tests >30% diferente al baseline esperado, patrones de testing no documentados), el agente **DEBE pausar y reportar** antes de continuar con la Iteración 2. Esto previene el drift acumulativo de construir diagnósticos sobre fundamentos incorrectos.

**Baseline esperado para validación de Iteración 1 (QW-09):**

| Módulo | Tests esperados (aprox.) | Anotaciones clave a verificar |
|---|---|---|
| `common-lib` | ~38 | `@ExtendWith(MockitoExtension.class)` |
| `donaciones-service` | ~790 | `@SpringBootTest`, `@ExtendWith(MockitoExtension.class)` |
| `logistica-service` | ~582 | `@SpringBootTest`, `MockMvcBuilders.standaloneSetup` |
| `incentivos-service` | ~378 | `@SpringBootTest(webEnvironment = NONE)` |
| `notificaciones-service` | ~232 | `@WebMvcTest`, `@Testcontainers`, `@DynamicPropertySource` |
| `integration-tests` | 9 clases IT | `@Tag(smoke/contract/integration/e2e/performance)` |

---

## 7. Plan de Verificación y Criterios de Parada

Dado el carácter estrictamente analítico de la tarea:

1. **`SOURCE_READ_ONLY` Verificado:**
   - Comprobar mediante `git status` que no se ha modificado ni creado ningún archivo bajo directorios `*/src/`.
2. **GrepAI-Preferred Cumplido:**
   - Toda afirmación en la matriz de hallazgos debe estar respaldada por evidencia factual extraída con `grepai` u otra herramienta de inspección de código.
3. **Integridad de ADRs:**
   - Los 4 ADRs deben tener status `proposed` y seguir la estructura canónica MADR.
4. **Sincronización del Grafo Documental:**
   - Confirmar que `docs/README.md` y `docs/ESTADO_DOCUMENTACION.md` tengan enlaces válidos y reflejen fielmente los nuevos artefactos.
5. **Etiquetas Epistémicas Completas (QW-10):**
   - Todo hallazgo en la matriz y en los diagnósticos debe tener su etiqueta epistémica (`[OBSERVED]`, `[DOCUMENTED]`, `[INFERRED]`, `[PROPOSED]`) según §3 de `AGENTS.md`.
6. **Checklist §12 de `AGENTS.md`:**
   - Validar todos los puntos aplicables del checklist de cierre.

---

## 8. Comando para Iniciar la Ejecución

Para iniciar la ejecución autónoma de este plan, ejecutar el siguiente comando en la interfaz:

```text
/goal Ejecutar la auditoría de testing y arquitectura target según docs/testing/plan-auditoria-y-blueprint-qa.md
```

---

## 9. Documentos Complementarios y Justificación de Diseño

Para consultar el registro detallado de las deliberaciones, el análisis en profundidad de las **alternativas descartadas (`[REJECTED]`)** y las mitigaciones arquitectónicas obligatorias, consultar:
* [`docs/testing/decisiones-diseno-auditoria-qa.md`](decisiones-diseno-auditoria-qa.md) — *Decisiones de Diseño: Plan de Auditoría y Blueprint de QA*.

---

## 10. Historial y Tracker de Cambios (Change Tracker)

Este documento es un artefacto vivo sujeto a control de cambios formal. Toda modificación ulterior debe registrarse en esta tabla:

| Versión | Fecha | Autor / Rol | Descripción de la Modificación | Justificación / Motivación |
|:---:|:---:|---|---|---|
| **1.0.0** | 2026-09-06 | Principal QA Architect & Systems Test Engineer | Versión inicial del plan de auditoría de testing y arquitectura target. | Formalización de directrices, invariantes, marco teórico y entregables para ejecución con `/goal`. |
| **1.1.0** | 2026-09-06 | Revisor Crítico (Antigravity) | Quick wins QW-01, QW-03, QW-05, QW-07, QW-08, QW-09, QW-10 aplicados: columna Estado Actual en §4, lista de auditorías precedentes en §1.1, referencia a ADR predecesor en §5.B, GrepAI-Preferred en §2.2, circuit breaker en §6, baseline de tests en §6, etiquetas epistémicas en §7. | Revisión crítica adversarial pre-ejecución con `/goal`. Hallazgos respaldados por evidencia GrepAI. |
| **1.2.0** | 2026-09-06 | Principal QA Architect (Antigravity) | Corrección adversarial H-01 aplicada: rectificada la alusión a Ajv en §4 y §5.B, documentando el motor nativo de validación en Node.js puro de `scripts/validate-contracts.js`. | Dictamen adversarial post-auditoría. |
| **1.3.0** | 2026-09-06 | Senior Staff Architect & Adversarial Evaluator | Correcciones post-review PR #869 aplicadas: reglas ArchUnit refinadas, surefire segregation con exclusión previa de suites pesadas, WireMock 3 (Java 21) y canonización de classpath mounting DDL. | Cierre de observaciones técnicas de PR #869. |

