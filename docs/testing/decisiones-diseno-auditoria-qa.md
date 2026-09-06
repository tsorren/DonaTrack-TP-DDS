# Decisiones de Diseño: Plan de Auditoría y Blueprint de QA (DonaTrack)

> **Documento:** Registro y Justificación de Decisiones de Diseño  
> **Ubicación:** `docs/testing/decisiones-diseno-auditoria-qa.md`  
> **Área:** QA, Testing & Arquitectura de Pruebas  
> **Documento Complementario:** [`docs/testing/plan-auditoria-y-blueprint-qa.md`](plan-auditoria-y-blueprint-qa.md)  
> **Estado:** Vigente  
> **Última Actualización:** 2026-09-06  

---

> [!IMPORTANT]
> **Aviso de Gobernanza y Alcance Epistémico:**  
> Este documento sirve como **justificación arquitectónica y memoria de deliberación técnica** para el plan de auditoría y la arquitectura target de testing.  
> Las alternativas catalogadas como **`[REJECTED]` (Descartadas)** están formalmente desestimadas en base a sus trade-offs de costo, complejidad o desvío de los objetivos pedagógicos y de ingeniería del proyecto.  
> **Bajo ninguna circunstancia los desarrolladores o agentes de IA deben asumir que las alternativas descartadas serán implementadas en el futuro ni resucitarlas silenciosamente.** Cualquier reconsideración exige un nuevo proceso formal de ADR de acuerdo con §9 de [`AGENTS.md`](../../AGENTS.md).

---

## 1. Contexto y Objetivos de la Deliberación

Durante la fase de diseño del arnés de testing y QA para **DonaTrack** (conducida bajo el protocolo `/grill-me`), se analizaron las tensiones existentes entre:
- La rigurosidad metodológica de la industria (**Vladimir Khorikov**, **Gerard Meszaros**, **Martin Fowler**, **Google SWE**, **ISO/IEC/IEEE 29119**).
- La realidad operativa de un monorepo distribuido (4 microservicios Spring Boot, RabbitMQ, PostgreSQL multi-schema, n8n y el módulo `integration-tests`).
- Las restricciones computacionales de CI/CD (runners estándar de GitHub Actions con 2 vCPUs y 7 GB de RAM).
- Las demandas y scripts canónicos de entrega académica de la cátedra (**UTN-FRBA DDS 2026**).
- Los hallazgos previos de la [auditoría DevOps/CI](../auditoria/revision-critica-devops-ci.md), en particular **INC-01** (`mvn test` en vez de `mvn verify` para `integration-tests` en CI, omitiendo las fases Failsafe) — directamente relevante para la segregación Surefire/Failsafe propuesta en §2.5.

A continuación se detallan las decisiones adoptadas (`[PROPOSED]`), las alternativas evaluadas y descartadas (`[REJECTED]`), sus trade-offs y las mitigaciones obligatorias.

---

## 2. Registro Exhaustivo de Decisiones de Diseño

### 2.1 Estructura Física y Modularidad de los Entregables

* **Decisión Elegida `[PROPOSED]`:**
  - Desglose hiper-granular en una carpeta dedicada: `docs/testing/auditoria/`.
  - La entrega se estructura en 5 documentos temáticos exhaustivos más un índice maestro:
    1. `README.md` (Índice y síntesis ejecutiva).
    2. `01-diagnostico-ejecutivo.md` (Radiografía factual, Khorikov y Google SWE).
    3. `02-matriz-antipatrones.md` (Hallazgos, smells de Meszaros y severidad).
    4. `03-estudio-comparativo.md` (Trade-offs multidimensionales de herramientas).
    5. `04-blueprint-target.md` (La arquitectura target y diagramas Mermaid).
    6. `05-roadmap-migracion.md` (Plan no disruptivo de adopción en 4 fases).
* **Alternativas Descartadas `[REJECTED]`:**
  - *Alternativa 2.1.A — Documento Maestro Único Monolítico (`docs/testing/auditoria-y-blueprint-testing.md`):*  
    **Motivo de descarte:** Unificar el diagnóstico empírico, la matriz de hallazgos, la comparativa de 4 tecnologías, el blueprint target y el roadmap en un solo archivo generaría un documento masivo de miles de líneas (similar a `analisis-arquitectonico.md`). Dificulta la lectura enfocada de los desarrolladores, satura la ventana de contexto de los modelos de IA y aumenta el riesgo de conflictos de merge en Git.
  - *Alternativa 2.1.B — Estructura Dual (Diagnóstico + Blueprint):*  
    **Motivo de descarte:** Mezclar la matriz de hallazgos con el diagnóstico ejecutivo sigue produciendo artefactos densos donde se pierde la visibilidad de los smells y las alternativas tecnológicas.
* **Justificación de la Elección:** La hiper-granularidad permite que un agente o desarrollador cargue únicamente el documento que necesita (ej. solo el blueprint o solo la matriz de smells), optimizando el consumo de contexto y facilitando la gobernanza documental.

---

### 2.2 Gobernanza y Alcance de las Propuestas de ADR

* **Decisión Elegida `[PROPOSED]`:**
  - Crear **4 ADRs atómicos, específicos e independientes** en la raíz de `docs/adr/`, redactados en formato canónico MADR y con estado `proposed`:
    1. `docs/adr/20260906-estrategia-ambientes-efimeros-testcontainers-en-componentes.md`
    2. `docs/adr/20260906-estrategia-contratos-openapi-wiremock-y-esquemas-amqp.md`
    3. `docs/adr/20260906-fitness-functions-arquitectonicas-con-archunit-y-pitest.md`
    4. `docs/adr/20260906-migracion-pruebas-rendimiento-a-k6.md`
* **Alternativas Descartadas `[REJECTED]`:**
  - *Alternativa 2.2.A — Un Único ADR Transversal Monolítico (`docs/adr/20260906-evolucion-arnes-testing-y-qa.md`):*  
    **Motivo de descarte:** Viola el principio de decisiones arquitectónicas atómicas. Si el equipo aprueba Testcontainers y ArchUnit pero desea postergar la migración a k6, un ADR monolítico bloquea o ensucia el ciclo de aprobación (`accepted` vs `rejected`).
  - *Alternativa 2.2.B — ADRs Aislados en un Subdirectorio `docs/adr/integration-tests/`:*  
    **Motivo de descarte:** Las decisiones de testing no son exclusivas del módulo `integration-tests`. Afectan a los 4 microservicios (pruebas de slicing, JPA, WireMock, ArchUnit en compilación) y a los workflows de CI/CD. Deben ser ADRs transversales de primer nivel en `docs/adr/`.
* **Justificación de la Elección:** Los 4 ADRs atómicos permiten una revisión y adopción incremental e independiente por parte del equipo humano.

---

### 2.3 Gestión de Entornos Efímeros: Docker Compose vs. Testcontainers

* **Baseline Actual `[OBSERVED]` (QW-06):**
  - `notificaciones-service` ya tiene `@Testcontainers` + `@DynamicPropertySource` con `postgres:16-alpine` en `RepositoriosJpaTest.java` (boilerplate manual previo a Spring Boot 3.1).
  - `notificaciones-service` también tiene el único `@WebMvcTest(NotificacionController.class)` del monorepo. Los demás servicios usan `MockMvcBuilders.standaloneSetup(...)` — patrón más rápido pero que no valida interceptores (`ControllerLoggingInterceptor`), filtros (`TraceResponseHeaderFilter`) ni `@ControllerAdvice` (`GlobalExceptionHandler`).
  - Los otros 3 servicios (`donaciones`, `logistica`, `incentivos`) usan persistencia en memoria (`CrudRepositoryEnMemoria`) sin JPA ni Testcontainers.
* **Decisión Elegida `[PROPOSED]`:**
  - **Coexistencia en Dos Niveles (*Testing Honeycomb*):**
    1. *Nivel de Componente / Slicing (en cada microservicio):* Se adopta **Testcontainers** para aislar dependencias out-of-process inmediatas (PostgreSQL y RabbitMQ). Se valida persistencia real (`@DataJpaTest`) y listeners AMQP sin levantar los demás microservicios.
    2. *Nivel Distribuido E2E (Black-Box):* Se preserva **Docker Compose preprod** (`docker-compose.preprod.yml`) para validar el sistema distribuido completo (Matching $\rightarrow$ Feign $\rightarrow$ Logística $\rightarrow$ RabbitMQ $\rightarrow$ Incentivos $\rightarrow$ n8n) y garantizar la ejecución de los scripts de cátedra (`./run-preprod-tests.sh`).
* **Mitigaciones Obligatorias Incorporadas:**
  - *Paridad DDL:* Reutilizar estrictamente el script SQL canónico `persistencia/init-db/01-init-schemas-roles.sql` tanto en Testcontainers como en Compose.
  - *Spring Boot 3.1+ `@ServiceConnection`:* Erradicar código manual de `@DynamicPropertySource`.
  - *Modo Degradado (§11.3 `AGENTS.md`):* Extensión `@DisabledIfDockerUnavailable` para no bloquear builds unitarios en máquinas locales sin Docker daemon (`[DEFERRED_NO_DOCKER]`).
  - *Reutilización de Contenedores (`.withReuse(true)`):* Activar Ryuk reuse para no penalizar el startup entre suites de un mismo servicio.
* **Alternativas Descartadas `[REJECTED]`:**
  - *Alternativa 2.3.A — Migración Total a Testcontainers (Java-Orchestrated):*  
    **Motivo de descarte profundo:**
    1. *Colapso por OOM en GitHub Actions:* Levantar los 4 microservicios Spring Boot + Postgres + RabbitMQ + n8n desde la JVM de JUnit compitiendo por 7 GB de RAM satura los runners de CI y dispara `OOMKilled`.
    2. *Riesgo Académico con la Cátedra:* La cátedra evalúa las entregas mediante Docker Compose estándar (`docker compose up` / `./run-preprod-tests.sh`). Eliminar o desatender Compose provocaría rechazo docente.
    3. *Complejidad de n8n en JUnit:* Automatizar la importación de workflows CLI (`n8n import:workflow`), publicación y reinicio dentro de contenedores Java en JUnit es extremadamente frágil.
  - *Alternativa 2.3.B — Mantener Docker Compose como Único Ecosistema (Sin Testcontainers):*  
    **Motivo de descarte profundo:**
    1. *Antipatrón del Cono de Helado (Ice Cream Cone):* Obliga a compilar todo el monorepo y levantar 7 contenedores solo para verificar una consulta SQL o un listener AMQP puntual.
    2. *Feedback Lento:* Viola el pilar de feedback rápido de Khorikov (minutos en lugar de segundos).
    3. *Diagnóstico Difuso:* Ante un fallo E2E en Compose, es imposible determinar si falló la lógica, la red, un script de inicio o un timeout de n8n.

---

### 2.4 Pruebas de Contrato y Verificación de Mensajería Inter-Servicios

* **Baseline Actual `[OBSERVED]` (QW-02):**
  - El repositorio **ya cuenta con infraestructura de contratos parcial**:
    - 4 especificaciones OpenAPI 3.0 estáticas en `docs/arquitectura/contratos/` (donaciones, incentivos, logística, notificaciones).
    - 11 esquemas JSON Schema formales en `docs/arquitectura/contratos/schemas/` (eventos AMQP, requests, responses).
    - Validador automatizado nativo en Node.js (`scripts/validate-contracts.js`) sin dependencias externas, integrado en CI.
  - Sin embargo, las **pruebas de contrato en Java son superficiales** (antipatrón *Green Smoke Contract*):
    - `ContractIT.java` solo verifica la existencia de paths en el JSON de OpenAPI generado dinámicamente (ej. `paths."/api/entregas".post != null`), sin validar esquemas de request/response, obligatoriedad de campos, tipos de datos ni códigos de respuesta.
    - `TracingContractIT.java` verifica la propagación del header `X-Trace-Id` — correcta pero ortogonal a la validación de contratos funcionales.
    - **Diagnóstico:** Tests que pasan siempre porque solo verifican que el endpoint existe, generando falsa confianza. Más grave que no tener tests de contrato.
* **Decisión Elegida `[PROPOSED]`:**
  - **Enfoque Pragmático: OpenAPI 3.0 + JSON Schema + WireMock.**
    1. *Contratos HTTP:* Validación bidireccional contra las especificaciones OpenAPI existentes en `docs/arquitectura/contratos/` mediante filtros de RestAssured / MockMvc (`OpenApiValidationFilter` de Atlassian `swagger-request-validator`).
    2. *Contratos de Eventos RabbitMQ:* Validación de payloads serializados contra esquemas formales `docs/arquitectura/contratos/schemas/` mediante `networknt/json-schema-validator`.
    3. *Desacoplamiento de Clientes Feign:* Los tests de integración en consumidores usan `WireMockServer` cargado con payloads canónicos derivados de OpenAPI.
  - **Relación con el baseline:** La validación Java con `swagger-request-validator` y `json-schema-validator` **complementa** (no reemplaza) el validador Node.js existente en CI. El validador Node.js opera en tiempo de CI sobre las specs estáticas; la validación Java opera en runtime de tests sobre los contratos vivos.
* **Mitigaciones Obligatorias Incorporadas:**
  - *Detección de Drift de OpenAPI en CI:* Un paso temprano en el pipeline compara el OpenAPI generado en compilación contra la spec estática en `docs/`; si divergen, el build falla exigiendo actualización documental.
  - *Stubs OpenAPI Validados:* Los stubs de WireMock se auto-validan contra la spec del productor para evitar falsos verdes.
* **Alternativas Descartadas `[REJECTED]`:**
  - *Alternativa 2.4.A — Consumer-Driven Contract Testing con Pact:*  
    **Motivo de descarte profundo:**
    1. *Sobrecarga de Infraestructura (Pact Broker):* Requiere desplegar y mantener un servidor Pact Broker con PostgreSQL para almacenar y versionar pactos.
    2. *Costo de Mantenimiento de Provider States (`@State`):* Cada expectativa del consumidor exige configurar métodos de preparación de estado en el productor, acoplando las suites.
    3. *Over-Engineering para el Contexto:* En un equipo monorepo académico de 5 desarrolladores, la fricción de Pact supera ampliamente sus beneficios frente a OpenAPI formal.
  - *Alternativa 2.4.B — Spring Cloud Contract (SCC):*  
    **Motivo de descarte profundo:**
    1. *Acoplamiento al Reactor de Maven:* Requiere compilar el productor para generar un JAR de stubs antes de poder compilar y testear al consumidor. En builds paralelos o modulares (`-pl`), causa fallos frecuentes.
    2. *Dependencia Pesada de Plugins y Groovy:* Incompatibilidades de versiones entre plugins de generación Groovy y Java 21 / Spring Boot 4.x.

---

### 2.5 Fitness Functions de Arquitectura y Pruebas de Mutación

* **Decisión Elegida `[PROPOSED]`:**
  - **ArchUnit Universal en `mvn test`:**
    - Verificación automática de invariantes de `AGENTS.md` en tiempo de compilación (ejecución en ~200 ms por módulo en memoria):
      - *Controllers como adaptadores puros:* Prohibición de inyectar Repositorios en Controllers.
      - *Pureza del dominio:* Paquete `models/entities/` aislado de anotaciones JPA, Spring y HTTP.
      - *Segregación Surefire/Failsafe (ADR 20260903):* Clases `*Test` en Surefire sin `@SpringBootTest`; clases `*IT` en Failsafe.
      - *Shared Kernel:* `common-lib` libre de referencias cruzadas a servicios específicos.
  - **Pitest Acotado con Perfil Dedicado (`-Pmutation-test`):**
    - Mutation testing restringido exclusivamente a dominios con lógica de decisión y estado compleja:
      - Transiciones de estado de `DonacionIndependiente` (State Pattern).
      - Algoritmos de asignación y fragmentación (`AlgoritmosService`).
      - Cómputo de rankings y reglas de gamificación en `incentivos-service`.
    - Excluido del `mvn test` diario; activable bajo demanda local o mediante un workflow cron/label en CI.
* **Alternativas Descartadas `[REJECTED]`:**
  - *Alternativa 2.5.A — ArchUnit y Pitest Obligatorios en el Quality Gate General (`mvn test`):*  
    **Motivo de descarte profundo:**
    1. *Destrucción del Feedback Loop:* Correr Pitest en todo el monorepo eleva el tiempo de build de 15 segundos a más de 20 minutos por corrida.
    2. *Agotamiento del Desarrollador (*Test Run War*):* Los desarrolladores comenzarían a saltarse los tests con `-DskipTests` debido a la lentitud.
    3. *Consumo de Minutos en CI:* Riesgo de timeouts en GitHub Actions.
  - *Alternativa 2.5.B — Pitest Solo Manual / Teórico (Sin Perfil Maven):*  
    **Motivo de descarte profundo:**
    1. *Efecto "Shelfware":* Si una herramienta no está formalmente configurada en el `pom.xml`, ningún integrante del equipo la ejecuta y el conocimiento se extingue.

---

### 2.6 Pruebas de Rendimiento, Carga y Saturación

* **Decisión Elegida `[PROPOSED]`:**
  - **Migración Total a k6 (Herramienta Especializada):**
    - Descartar y remover `PerformanceStressIT` de la suite JUnit de Java.
    - Diseñar una suite de carga moderna en **k6** (scripts JavaScript desacoplados en `tests/performance/k6/`).
    - Capacidad de simular usuarios virtuales (VUs), rampas de carga, percentiles precisos (p90, p95, p99) y thresholds de SLA estrictos.
    - Ejecución mediante contenedor Docker oficial (`grafana/k6`) en `docker-compose.preprod.yml` bajo un perfil dedicado (`--profile perf`).
    - **Wrapper de Compatibilidad Docente:** Parametrizar `run-preprod-tests.sh` para que, al invocarse `./run-preprod-tests.sh --groups performance`, levante y ejecute el contenedor de k6, preservando la compatibilidad con los scripts de la cátedra.
* **Alternativas Descartadas `[REJECTED]`:**
  - *Alternativa 2.6.A — Enfoque Híbrido (Conservar `PerformanceStressIT` en JUnit + k6):*  
    **Motivo de descarte profundo:**
    1. *Mantenimiento Dual Innecesario:* Mantener el mismo flujo de donación escrito en Java y en JavaScript.
    2. *Engaño Metodológico:* `PerformanceStressIT` es un bucle `for` secuencial sincrónico que cronometra llamadas con `System.currentTimeMillis()`. No evalúa concurrencia real ni contención de pools de conexiones (`HikariCP` o `Tomcat`), generando una falsa sensación de estrés.
  - *Alternativa 2.6.B — Optimización Nativa Java con Virtual Threads (Java 21):*  
    **Motivo de descarte profundo:**
    1. *Efecto Observador (*Coordinated Omission*):* La misma JVM que genera los hilos virtuales compite por la CPU y la red con los microservicios en el host de prueba, distorsionando las métricas de latencia.
    2. *Carencia de Tooling de Carga:* JUnit no genera histogramas, curvas de throughput ni reportes de saturación nativos.

---

### 2.7 Modo de Orquestación y Flujo de Ejecución del Plan

* **Decisión Elegida `[PROPOSED]`:**
  - **Ejecución Autónoma End-to-End con la skill `/goal`:**
    - El agente ejecuta de forma continua las 5 iteraciones metodológicas (Relevamiento GrepAI $\rightarrow$ Análisis Crítico $\rightarrow$ Comparativa y Blueprint $\rightarrow$ Revisión Adversarial $\rightarrow$ ADRs y Sincronización) sin detenerse hasta completar el checklist de §12 de `AGENTS.md`.
* **Alternativas Descartadas `[REJECTED]`:**
  - *Alternativa 2.7.A — Ejecución en Dos Etapas con Pausa Intermedia:*  
    **Motivo de descarte:** Todas las decisiones de diseño, trade-offs, mitigaciones y formatos de salida ya fueron consensuados y validados durante la sesión `/grill-me`. Interrumpir el flujo para una aprobación intermedia resulta redundante y desacelera la concreción de los artefactos.

---

## 3. Síntesis Visual de Decisiones

```text
┌──────────────────────────────────────────────────────────────────────────────────────────────────────────┐
│                            ECOSISTEMA TARGET DE TESTING (DONATRACK)                                     │
├───────────────────────┬──────────────────────────────────┬──────────────────────────┬───────────────────┤
│ Capa de Prueba        │ Tecnologías Seleccionadas        │ Baseline Existente       │ Estado / Rol      │
├───────────────────────┼──────────────────────────────────┼──────────────────────────┼───────────────────┤
│ Fitness / Calidad     │ ArchUnit (Universal en test)     │ Ninguna                  │ [PROPOSED] ADR 3  │
│ Mutation Testing      │ Pitest (-Pmutation-test acotado) │ Ninguna                  │ [PROPOSED] ADR 3  │
│ Unitarios de Dominio  │ JUnit 5 + Mockito (en memoria)   │ ~2020 tests, 49+ clases  │ [OBSERVED] Vigente│
│ Slicing / Componentes │ Testcontainers (@ServiceConn)    │ 1 clase en notificaciones│ [PROPOSED] ADR 1  │
│ Contratos REST / AMQP │ OpenAPI + JSON Schema + WireMock │ 4 specs + 11 schemas     │ [PROPOSED] ADR 2  │
│ E2E Distribuido       │ RestAssured + Docker Compose     │ 9 clases IT + scripts .sh│ [OBSERVED] Vigente│
│ Rendimiento y Carga   │ k6 (Dockerizado + Script Wrapper)│ PerformanceStressIT (seq)│ [PROPOSED] ADR 4  │
└───────────────────────┴──────────────────────────────────┴──────────────────────────┴───────────────────┘
```

---

## 4. Historial y Tracker de Cambios (Change Tracker)

Este documento y su plan complementario ([`plan-auditoria-y-blueprint-qa.md`](plan-auditoria-y-blueprint-qa.md)) son documentos vivos sujetos a gobernanza estricta. Toda modificación ulterior debe registrarse obligatoriamente en esta tabla:

| Versión | Fecha | Autor / Rol | Descripción de la Modificación | Justificación / Motivación |
|:---:|:---:|---|---|---|
| **1.0.0** | 2026-09-06 | Principal QA Architect & Systems Test Engineer | Versión inicial consolidada tras sesión interactiva de `/grill-me`. Formalización de decisiones y alternativas descartadas. | Establecer la memoria histórica de diseño y justificación para la ejecución con `/goal`. |
| **1.1.0** | 2026-09-06 | Revisor Crítico (Antigravity) | Quick wins QW-02, QW-04, QW-06, QW-11, QW-12 aplicados: baseline actual y evidencia de *Green Smoke Contract* en §2.4, baseline de Testcontainers y heterogeneidad `@WebMvcTest`/`standaloneSetup` en §2.3, columna Baseline Existente en §3, referencia cruzada a INC-01 en §1. | Revisión crítica adversarial pre-ejecución con `/goal`. Hallazgos respaldados por evidencia GrepAI. |
| **1.2.0** | 2026-09-06 | Principal QA Architect (Antigravity) | Corrección adversarial H-01 aplicada: rectificada la alusión a Ajv en §2.4, documentando con precisión el motor nativo de validación en Node.js puro de `scripts/validate-contracts.js`. | Dictamen adversarial post-auditoría. |
