# Matriz de Anti-patrones, Smells y Hallazgos Empíricos de QA (DonaTrack)

> **Documento:** Matriz de Hallazgos Empíricos y Clasificación de Smells  
> **Ubicación:** `docs/testing/auditoria/02-matriz-antipatrones.md`  
> **Rol:** Lead QA Architect & Principal Systems Engineer  
> **Marco Taxonómico:** Gerard Meszaros (*xUnit Test Patterns*), Vladimir Khorikov (*Unit Testing*), Martin Fowler (*Refactoring* / *Microservices Testing*)  
> **Ámbito:** Factual, Empírico y Documental (`SOURCE_READ_ONLY` en `src/`)  
> **Fecha de Evaluación:** 2026-09-06  
> **Estado:** Vigente y Actualizado (`[DOCUMENTED]`)  

---

> [!NOTE]
> **Trazabilidad y Estado Post-Migración:**  
> Este documento registra la radiografía factual inicial de smells junto con su **estado actual tras la ejecución del roadmap de QA**.  
> Los antipatrones críticos **AP-01, AP-02 y AP-03**, así como **AP-04, AP-07, AP-08, AP-09 y AP-10**, han sido formalmente **ERRADICADOS y RESUELTOS**.  
> Los ítems pendientes de infraestructura relacional y mensajería efímera (**AP-05 y AP-06**) están catalogados en el [Backlog de Cobertura Crítica](06-cobertura-critica-qa-backlog.md) (`QA-GAP-01` a `QA-GAP-04`).

---

## 1. Convención de Taxonomía y Severidad

Para categorizar objetivamente los hallazgos identificados en el código de pruebas de DonaTrack, se adopta la siguiente escala de severidad y clasificación:

* **🔴 Crítico (`CRITICAL`):** Anti-patrón o vicio de diseño que introduce una falsa sensación de seguridad (*False Sense of Security*), invalida los resultados de prueba, degrada severamente la estabilidad de CI/CD o viola los principios fundamentales de testing de la industria.
* **🟡 Deuda Técnica / Oportunidad (`DEBT`):** Oportunidad de modernización arquitectónica, heterogeneidad en el uso de frameworks o ineficiencia operativa que incrementa el costo de mantenimiento.
* **🟢 Fortaleza Arquitectónica (`STRENGTH`):** Patrón idiomático o buena práctica sólidamente implementada en el repositorio que debe preservarse como estándar.

---

## 2. Matriz Exhaustiva de Anti-patrones y Hallazgos

| ID | Módulo / Componente | Anti-patrón / Smell (Meszaros / Fowler) | Evidencia Factual en Código (`[OBSERVED]`) | Riesgo / Impacto Técnico (`[INFERRED]`) | Severidad | Estado Post-Migración (`[OBSERVED]`) |
|---|---|---|---|---|:---:|---|
| **AP-01** | `integration-tests`<br>`ContractIT.java` | **Green Smoke Contract**<br>*(Fowler / Khorikov)* | `integration-tests/src/test/java/grupo5/tests/contract/ContractIT.java:16-40`<br>Solo verifica `body("paths.\"/api/...\".post", notNullValue())`. No valida esquemas, tipos de datos, enums, obligatoriedad ni status codes devueltos. | **Falsa sensación de seguridad crítica:** Modificaciones incompatibles en request/response DTOs pasan desapercibidas en los tests de contrato de Java. Pasa siempre mientras el path exista. | 🔴 Crítico | 🟢 **RESUELTO** (commit `e2f1d67c`)<br>Validación bidireccional estricta en runtime con `Atlassian Swagger Request Validator` (`OpenApiValidationFilter`) contra `/v3/api-docs` y especificaciones formales. |
| **AP-02** | `integration-tests`<br>`PerformanceStressIT.java` | **Sequential Load Loop** / **Single-Threaded Illusion**<br>*(Meszaros: False Stress)* | `integration-tests/src/test/java/grupo5/tests/performance/PerformanceStressIT.java:31, 100`<br>Bucle `for (int i = 0; i < totalRequests; i++)` síncrono y secuencial con `System.currentTimeMillis()`. | **Cero estrés concurrente:** No evalúa contención de pools (`HikariCP`), saturación de threads de Tomcat, deadlocks de base de datos ni p95/p99. Consume minutos en vano. | 🔴 Crítico | 🟢 **RESUELTO** (commit `ebfa113a`)<br>`PerformanceStressIT.java` eliminado. Migrado a **k6** en contenedor Docker (`tests/performance/k6/`) con VUs, percentiles (p95, p99) y thresholds de SLA. |
| **AP-03** | 4 Microservicios<br>Controllers (`17 clases*`) | **Standalone Setup Blindspot**<br>*(Meszaros: Subcutaneous Blindness)* | 17 clases de controller usan `MockMvcBuilders.standaloneSetup(controller)` (ej. `SubcategoriasControllerTest.java:44`, `CamionesControllerTest.java:47`, `PlanificacionManualControllerTest.java:28`). | **Ceguera de infraestructura web:** No valida interceptores (`ControllerLoggingInterceptor`), filtros MDC (`TraceResponseHeaderFilter`), Jackson Beans ni `@ControllerAdvice` si no se registra a mano. En `PlanificacionManualControllerTest`, ni siquiera se configuró el exception handler. | 🔴 Crítico | 🟢 **RESUELTO** (commits `f875f2ed`, `dc99e2ba`, `b00fb7f8`, `89e68beb`)<br>100% migrado a `@WebMvcTest`. Contextos consolidados en `AbstractDonacionesWebMvcTest` y `AbstractLogisticaWebMvcTest`. |
| **AP-04** | `notificaciones-service`<br>`RepositoriosJpaTest.java` | **Fragile Dynamic Property Setup & Slicing Monolith**<br>*(Meszaros: Resource Optimism)* | `notificaciones-service/src/test/java/grupo5/notificaciones/infrastructure/persistencia/RepositoriosJpaTest.java:33-67`<br>Usa `@SpringBootTest` completo en vez de `@DataJpaTest`. Código manual de resolución de paths (`../persistencia/...`) y `@DynamicPropertySource`. | **Startup lento y fragilidad en CI:** Levanta el contexto entero de Spring Boot para probar 2 repositorios. La ruta relativa al SQL falla si cambia el working dir de ejecución. Debería usar Spring Boot 3.1+ `@ServiceConnection` y montaje de DDL desde classpath. | 🟡 Deuda | 🟢 **RESUELTO** (commit `7a7229c4`)<br>Refactorizado con `@DataJpaTest` y `@ServiceConnection` sobre PostgreSQL 16 Testcontainers. |
| **AP-05** | `donaciones`, `logistica`, `incentivos` | **Database Isolation Void & In-Memory Illusion**<br>*(Khorikov: Over-reliance on Fakes)* | Los repositorios usan `CrudRepositoryEnMemoria` en pruebas. 0 tests con PostgreSQL efímero en 3 de los 4 microservicios. | **Postergación de bugs SQL a E2E:** Dialectos SQL incompatibles, constraints de unicidad o fallos de FK solo se descubren al levantar todo el stack en Docker Compose. | 🟡 Deuda | 🟡 **BACKLOG ACTIVO (QA-GAP-02)**<br>Catalogado en `06-cobertura-critica-qa-backlog.md`. Pendiente incorporar `@DataJpaTest` a los servicios restantes. |
| **AP-06** | `integration-tests`<br>`docker-compose.preprod.yml` | **Heavyweight Distributed E2E Bottleneck**<br>*(Fowler: Ice Cream Cone)* | `main.yml:751-765` y `run-preprod-tests.sh:101-144`<br>Para correr 1 prueba que involucre persistencia o mensajería se deben levantar 7 contenedores (4 Spring Boot, Postgres, Rabbit, n8n) con ~3 min de warmup. | **Lentitud y fragilidad (*Flakiness*):** Cualquier timeout en el arranque de n8n o salud de endpoints aborta toda la suite distribuida. | 🟡 Deuda | 🟡 **MITIGADO / EN BACKLOG (QA-GAP-04)**<br>Docker Compose optimizado con healthchecks y scripts de diagnóstico (`analyze_preprod_logs.py`). Pendiente desacoplar en tests de componentes con broker efímero. |
| **AP-07** | Todo el Monorepo<br>`pom.xml` | **Absence of Architectural Fitness Functions**<br>*(Ford, Parsons, Kua: Building Evolutionary Arch)* | `pom.xml:1-226`<br>0 dependencias de **ArchUnit**. Las invariantes de `AGENTS.md` (controllers sin repositorios, pureza de dominio, aislamiento de capas) solo se revisan manualmente en PR. | **Degradación silenciosa de la arquitectura:** Acoplamientos indeseados pueden filtrarse en commits sin que ningún build automatizado falle. | 🟡 Deuda | 🟢 **RESUELTO** (commit `36110c4d`)<br>Dependencia `archunit-junit5` e implementación de `ArchitectureFitnessTest` en los 5 módulos de servicios. |
| **AP-08** | Dominio Crítico<br>Algoritmos y Estados | **Assertion Quality Blindspot** / **Absence of Mutation Testing**<br>*(Jia & Harman)* | `pom.xml:1-226`<br>0 configuración de **Pitest**. Cobertura de líneas alta en JaCoCo (~80%+), pero sin medición de si los tests realmente matan mutantes en `AlgoritmosService`, `RankingMensual` o transiciones de estado. | **Falsos positivos de cobertura:** Un test puede ejecutar una línea sin asertar su efecto colateral, dejando ramas lógicas críticas sin protección real. | 🟡 Deuda | 🟢 **RESUELTO** (commit `36110c4d`)<br>Plugin `pitest-maven` integrado en root `pom.xml` bajo el perfil `-Pmutation-test` (100% de mutantes eliminados en algoritmos). |
| **AP-09** | `integration-tests`<br>`PollingUtils.java` | **Resource Optimism & Blind Polling**<br>*(Meszaros: Flaky Test / Sleepy Test)* | `integration-tests/src/test/java/grupo5/tests/utils/PollingUtils.java:193-242`<br>Uso de `Awaitility.await().atMost(Duration.ofSeconds(8))` con polling continuo para sincronización asíncrona de eventos en Docker. | **Sensibilidad al entorno:** En runners sobrecargados de CI (GitHub Actions 2 vCPUs), 8 segundos puede ser insuficiente, generando fallos espurios (*Flakiness*). | 🟡 Deuda | 🟢 **RESUELTO / MITIGADO**<br>Sleeps ciegos erradicados de `PollingUtils.java`; estandarizado con Awaitility explícito (`atMost(Duration.ofSeconds(15))`, `pollInterval(Duration.ofMillis(200))`). |
| **AP-10** | `common-lib`<br>`pom.xml` | **Shared Kernel Missing Consumer Contract Tests**<br>*(Fowler: Shared Kernel Smells)* | `common-lib/pom.xml:1-87`<br>Librería transversal con AOP, logging, interceptores y adaptadores JPA opcionales, probada únicamente en aislamiento sin pruebas de compatibilidad binaria contra los servicios consumidores. | **Riesgo de ruptura cross-service:** Un cambio en `common-lib` puede compilar en su módulo pero romper `notificaciones-service` o `donaciones-service` en runtime si divergen las versiones de Spring Boot. | 🟡 Deuda | 🟢 **RESUELTO / MITIGADO**<br>Configuración de `test-jar` en `common-lib/pom.xml` para reutilización segura de builders/fixtures en consumidores (`maven-jar-plugin`). |

---

## 3. Matriz de Fortalezas Arquitectónicas de Testing

| ID | Componente / Patrón | Fortaleza Identificada (`[OBSERVED]`) | Beneficio Técnico (`[DOCUMENTED]`) |
|---|---|---|---|
| **FT-01** | **Suite Unitaria de Dominio Puro** | 1118 métodos `@Test` (~2020 ejecuciones en surefire) a lo largo de 5 módulos (`common-lib`, `donaciones`, `logistica`, `incentivos`, `notificaciones`). | **Feedback ultra-rápido:** Ejecuta en ~15-20 segundos. Sigue la Escuela Clásica de Detroit (aserciones sobre estado observable), otorgando alta resistencia al refactor y cobertura de reglas de negocio. |
| **FT-02** | **Test Data Builders y Object Mothers** | Implementación consistente de patrones de generación de datos sintéticos: `DonacionTestDataBuilder`, `PersonaTestDataBuilder`, `NecesidadTestDataBuilder`, `PersonaMother`, `DTOFixtures`. | **Mantenibilidad y legibilidad:** Reduce drásticamente el ruido en la fase Arrange (*Given*), elimina duplicación de fixtures y cumple con la restricción de seguridad de datos sintéticos de `AGENTS.md`. |
| **FT-03** | **Especificaciones Canónicas OpenAPI 3.0 y JSON Schemas** | 4 specs OpenAPI completas en `docs/arquitectura/contratos/` y 11 esquemas JSON formales en `docs/arquitectura/contratos/schemas/`, validados en CI por `scripts/validate-contracts.js`. | **Fuente de verdad contractual existente:** El monorepo ya posee los esquemas de frontera listos para ser consumidos por validadores Java en runtime (`swagger-request-validator` y WireMock). |
| **FT-04** | **Observabilidad y Trazabilidad en Pruebas** | Verificación activa de encabezados de trazabilidad `X-Trace-Id` en `TracingContractIT.java` y propagación de contexto MDC en `LoggingAutoConfigurationTest`. | **Diagnóstico distribuido:** Garantiza que cada request HTTP o evento conserve la trazabilidad extremo a extremo para debugging de tests. |
| **FT-05** | **Diagnóstico y Análisis Automático de Logs en Fallos de CI** | Scripts `scripts/analyze_preprod_logs.py` y `scripts/report_test_failures.py` integrados en `.github/workflows/main.yml:773-782` y `run-preprod-tests.sh:59-62`. | **Visibilidad operacional:** Ante la caída de un test en pre-producción, se extraen automáticamente las excepciones, trazas de stack y códigos de error de los contenedores Docker. |

---

## 4. Análisis Detallado de los 3 Anti-patrones Críticos

### 4.1 AP-01: El Antipatrón *Green Smoke Contract* en `ContractIT.java`

`[OBSERVED]` El archivo `integration-tests/src/test/java/grupo5/tests/contract/ContractIT.java` contenía originalmente pruebas superficiales como:
```java
@Test
void testNotificacionesPersonasContract() {
  notificacionesClient
      .obtenerOpenApi()
      .then()
      .statusCode(200)
      .body("paths.\"/api/notificaciones/personas\".put", notNullValue());
}
```
`[INFERRED]` **Por qué era un anti-patrón de severidad crítica:**
1. **Verificación Superficial:** El test solo evaluaba que Springdoc hubiera registrado el endpoint. Si el servicio de notificaciones cambiaba el cuerpo del request o el código de respuesta de `200` a `204`, el test **continuaba pasando con éxito**.
2. **Falsa Confianza:** Generaba un indicador "verde" en el pipeline de CI (`Contract Tests: PASS`), haciendo creer al equipo que los contratos entre servicios estaban verificados cuando en realidad la compatibilidad binaria y semántica no estaba siendo testeada.
3. **Mitigación Mandatoria:** Reemplazar esta aserción trivial por validación estricta de request y response contra las especificaciones OpenAPI usando `OpenApiValidationFilter` (Atlassian) o stubs de **WireMock** validados contra la especificación YAML oficial.

#### 4.1.1 Resolución Post-Mortem (`[VERIFIED]`)
En el commit `e2f1d67c`, `ContractIT.java` fue refactorizado incorporando la dependencia `com.atlassian.oai:swagger-request-validator-restassured:2.44.1`. Ahora las peticiones y respuestas se validan bidireccionalmente contra el endpoint OpenAPI (`/v3/api-docs`) de cada microservicio mediante `OpenApiValidationFilter`. Asimismo, se incorporó un test adversarial (`testAdversarialBreakingChangeContractValidation`) que valida que cualquier rotura en el contrato de request/response detona un fallo inmediato en la suite.

---

### 4.2 AP-02: El Antipatrón *Sequential Load Loop* en `PerformanceStressIT.java`

`[OBSERVED]` El archivo `integration-tests/src/test/java/grupo5/tests/performance/PerformanceStressIT.java` implementaba la prueba de estrés de la siguiente manera:
```java
for (int i = 0; i < totalRequests; i++) {
  long start = System.currentTimeMillis();
  try {
    PersonaTestDTO persona = PersonaTestDataBuilder.humana()...build();
    UUID personaId = donacionesClient.crearPersonaOk(persona);
    PollingUtils.esperarReplicacionPersona(notificacionesClient, personaId);
    UUID donanteId = donacionesClient.crearDonanteOk(personaId);
    ...
    long end = System.currentTimeMillis();
    latencies.add(end - start);
  } catch (Throwable t) {
    errorCount++;
  }
}
```
`[INFERRED]` **Por qué era un anti-patrón de severidad crítica:**
1. **Ausencia de Carga Concurrente:** Una prueba de estrés tiene como objetivo descubrir cuellos de botella bajo concurrencia: agotamiento en `HikariCP`, saturación de threads de Tomcat o bloqueos a nivel de base de datos. Al invocar las peticiones secuencialmente en un bucle `for` de un solo hilo, el sistema se encontraba en reposo absoluto durante cada ciclo.
2. **Métricas de Latencia Distorsionadas:** El cálculo de latencia incluía el tiempo de polling de `esperarReplicacionPersona()`, que dormía el hilo mediante Awaitility.
3. **Falso Éxito:** Un sistema que pasaba esta prueba podía colapsar en producción bajo peticiones simultáneas mínimas.
4. **Mitigación Mandatoria:** Descartar `PerformanceStressIT` de JUnit y adoptar **k6**, ejecutando scripts JavaScript con rampas de usuarios virtuales (20-50 VUs), distribución estocástica de peticiones y cálculo de percentiles (p90, p95, p99).

#### 4.2.1 Resolución Post-Mortem (`[VERIFIED]`)
En el commit `ebfa113a`, la clase `PerformanceStressIT.java` fue erradicada definitivamente de la suite JUnit de `integration-tests`. El arnés de rendimiento fue migrado a **k6** en contenedor Docker (`tests/performance/k6/`), ejecutando escenarios estocásticos de carga con usuarios virtuales concurrentes (VUs) y evaluando SLAs formales sobre percentiles (p90, p95, p99), integrados en el comando docente `./run-preprod-tests.sh --groups performance`.

---

### 4.3 AP-03: El Antipatrón *Standalone Setup Blindspot* en Controladores REST

`[OBSERVED]` En 17 clases de prueba de controladores individuales (ej. `donaciones-service`, `logistica-service`, `incentivos-service` y `PersonasControllerTest` en `notificaciones-service`), se inicializaba MockMvc originalmente con:
```java
mockMvc = MockMvcBuilders.standaloneSetup(controller)
    .setControllerAdvice(new GlobalExceptionHandler())
    .build();
```
Y en `PlanificacionManualControllerTest.java:28`:
```java
mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
```

> [!NOTE]
> **Nota metodológica sobre el conteo:** El conteo de 17 clases correspondió específicamente a pruebas unitarias de controladores REST individuales. En el monorepo existían 19 archivos que contenían `standaloneSetup`, ya que se excluían de esta métrica dos pruebas de validación transversal de infraestructura HTTP: `HandlersWebMvcValidationTest` (en `incentivos-service`) y `ValidacionHttpTest` (en `logistica-service`).
`[INFERRED]` **Por qué era un anti-patrón de severidad crítica:**
1. **Aislamiento Excesivo:** `standaloneSetup` no levantaba el contexto web de Spring. Se ejecutaba como un test de unidad puro donde MockMvc actuaba simplemente como despachador sintáctico.
2. **Omitir Interceptores y Filtros:** El interceptor de logging (`ControllerLoggingInterceptor`) y el filtro de trazabilidad (`TraceResponseHeaderFilter`) **nunca se ejecutaban**.
3. **Fragilidad ante Olvidos:** Si el desarrollador olvidaba agregar `.setControllerAdvice(new GlobalExceptionHandler())` (como ocurrió en `PlanificacionManualControllerTest`), las excepciones resultaban en un error 500 no manejado en lugar de validar la respuesta estructurada `ErrorResponse`.
4. **Mitigación Mandatoria:** Migrar los tests de controladores a `@WebMvcTest(MiController.class)`.

#### 4.3.1 Resolución Post-Mortem (`[VERIFIED]`)
A través de los commits `f875f2ed`, `dc99e2ba`, `b00fb7f8`, `89e68beb` y la consolidación de la suite en la rama actual, los controladores REST fueron 100% migrados de `standaloneSetup` a `@WebMvcTest`. Para prevenir context churn y optimizar los tiempos de ejecución de Spring Boot TestContext, se crearon las clases base de slicing:
- `AbstractDonacionesWebMvcTest.java` (en `donaciones-service`)
- `AbstractLogisticaWebMvcTest.java` (en `logistica-service`)

Esto garantiza que `ControllerLoggingInterceptor`, `TraceResponseHeaderFilter` y `GlobalExceptionHandler` participen en todas las pruebas de controladores, validando el encabezado `X-Trace-Id` y respuestas estructuradas ante excepciones de validación (400) y de negocio (409).

---

## 5. Próximos Pasos y Conexión con el Backlog

Los hallazgos catalogados en esta matriz constituyeron la base empírica para la modernización de la plataforma. Para la continuidad evolutiva:
1. El **Estudio Comparativo Multidimensional** (`03-estudio-comparativo.md`), donde se evaluaron las herramientas y trade-offs para erradicar cada smell.
2. El **Blueprint de la Arquitectura Target** (`04-blueprint-target.md`), donde se redefinieron los límites, SLA y herramientas de cada capa.
3. El **Roadmap de Migración** (`05-roadmap-migracion.md`), que secuenció la remediación en 4 fases incrementales.
4. El **Backlog de Cobertura Crítica** (`06-cobertura-critica-qa-backlog.md`), que cataloga los faltantes de persistencia real (`QA-GAP-02`), resiliencia RabbitMQ (`QA-GAP-01`), stubs WireMock (`QA-GAP-03`) y compensación distribuida para la siguiente iteración.

