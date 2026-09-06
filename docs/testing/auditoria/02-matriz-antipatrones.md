# Matriz de Anti-patrones, Smells y Hallazgos Empíricos de QA (DonaTrack)

> **Documento:** Matriz de Hallazgos Empíricos y Clasificación de Smells  
> **Ubicación:** `docs/testing/auditoria/02-matriz-antipatrones.md`  
> **Rol:** Lead QA Architect & Principal Systems Engineer  
> **Marco Taxonómico:** Gerard Meszaros (*xUnit Test Patterns*), Vladimir Khorikov (*Unit Testing*), Martin Fowler (*Refactoring* / *Microservices Testing*)  
> **Ámbito:** Factual, Empírico y Documental (`SOURCE_READ_ONLY` en `src/`)  
> **Fecha de Evaluación:** 2026-09-06  
> **Estado:** Vigente (`[DOCUMENTED]`)  

---

## 1. Convención de Taxonomía y Severidad

Para categorizar objetivamente los hallazgos identificados en el código de pruebas de DonaTrack, se adopta la siguiente escala de severidad y clasificación:

* **🔴 Crítico (`CRITICAL`):** Anti-patrón o vicio de diseño que introduce una falsa sensación de seguridad (*False Sense of Security*), invalida los resultados de prueba, degrada severamente la estabilidad de CI/CD o viola los principios fundamentales de testing de la industria.
* **🟡 Deuda Técnica / Oportunidad (`DEBT`):** Oportunidad de modernización arquitectónica, heterogeneidad en el uso de frameworks o ineficiencia operativa que incrementa el costo de mantenimiento.
* **🟢 Fortaleza Arquitectónica (`STRENGTH`):** Patrón idiomático o buena práctica sólidamente implementada en el repositorio que debe preservarse como estándar.

---

## 2. Matriz Exhaustiva de Anti-patrones y Hallazgos

| ID | Módulo / Componente | Anti-patrón / Smell (Meszaros / Fowler) | Evidencia Factual en Código (`[OBSERVED]`) | Riesgo / Impacto Técnico (`[INFERRED]`) | Severidad |
|---|---|---|---|---|:---:|
| **AP-01** | `integration-tests`<br>`ContractIT.java` | **Green Smoke Contract**<br>*(Fowler / Khorikov)* | `integration-tests/src/test/java/grupo5/tests/contract/ContractIT.java:16-40`<br>Solo verifica `body("paths.\"/api/...\".post", notNullValue())`. No valida esquemas, tipos de datos, enums, obligatoriedad ni status codes devueltos. | **Falsa sensación de seguridad crítica:** Modificaciones incompatibles en request/response DTOs pasan desapercibidas en los tests de contrato de Java. Pasa siempre mientras el path exista. | 🔴 Crítico |
| **AP-02** | `integration-tests`<br>`PerformanceStressIT.java` | **Sequential Load Loop** / **Single-Threaded Illusion**<br>*(Meszaros: False Stress)* | `integration-tests/src/test/java/grupo5/tests/performance/PerformanceStressIT.java:31, 100`<br>Bucle `for (int i = 0; i < totalRequests; i++)` síncrono y secuencial con `System.currentTimeMillis()`. | **Cero estrés concurrente:** No evalúa contención de pools (`HikariCP`), saturación de threads de Tomcat, deadlocks de base de datos ni p95/p99. Consume minutos en vano. | 🔴 Crítico |
| **AP-03** | 4 Microservicios<br>Controllers (`17 clases`) | **Standalone Setup Blindspot**<br>*(Meszaros: Subcutaneous Blindness)* | 17 clases de controller usan `MockMvcBuilders.standaloneSetup(controller)` (ej. `SubcategoriasControllerTest.java:44`, `CamionesControllerTest.java:47`, `PlanificacionManualControllerTest.java:28`). | **Ceguera de infraestructura web:** No valida interceptores (`ControllerLoggingInterceptor`), filtros MDC (`TraceResponseHeaderFilter`), Jackson Beans ni `@ControllerAdvice` si no se registra a mano. En `PlanificacionManualControllerTest`, ni siquiera se configuró el exception handler. | 🔴 Crítico |
| **AP-04** | `notificaciones-service`<br>`RepositoriosJpaTest.java` | **Fragile Dynamic Property Setup & Slicing Monolith**<br>*(Meszaros: Resource Optimism)* | `notificaciones-service/src/test/java/grupo5/notificaciones/infrastructure/persistencia/RepositoriosJpaTest.java:33-67`<br>Usa `@SpringBootTest` completo en vez de `@DataJpaTest`. Código manual de resolución de paths (`../persistencia/...`) y `@DynamicPropertySource`. | **Startup lento y fragilidad en CI:** Levanta el contexto entero de Spring Boot para probar 2 repositorios. La ruta relativa al SQL falla si cambia el working dir de ejecución. Debería usar Spring Boot 3.1+ `@ServiceConnection` y montaje de DDL desde classpath. | 🟡 Deuda |
| **AP-05** | `donaciones`, `logistica`, `incentivos` | **Database Isolation Void & In-Memory Illusion**<br>*(Khorikov: Over-reliance on Fakes)* | Los repositorios usan `CrudRepositoryEnMemoria` en pruebas. 0 tests con PostgreSQL efímero en 3 de los 4 microservicios. | **Postergación de bugs SQL a E2E:** Dialectos SQL incompatibles, constraints de unicidad o fallos de FK solo se descubren al levantar todo el stack en Docker Compose. | 🟡 Deuda |
| **AP-06** | `integration-tests`<br>`docker-compose.preprod.yml` | **Heavyweight Distributed E2E Bottleneck**<br>*(Fowler: Ice Cream Cone)* | `main.yml:751-765` y `run-preprod-tests.sh:101-144`<br>Para correr 1 prueba que involucre persistencia o mensajería se deben levantar 7 contenedores (4 Spring Boot, Postgres, Rabbit, n8n) con ~3 min de warmup. | **Lentitud y fragilidad (*Flakiness*):** Cualquier timeout en el arranque de n8n o salud de endpoints aborta toda la suite distribuida. | 🟡 Deuda |
| **AP-07** | Todo el Monorepo<br>`pom.xml` | **Absence of Architectural Fitness Functions**<br>*(Ford, Parsons, Kua: Building Evolutionary Arch)* | `pom.xml:1-226`<br>0 dependencias de **ArchUnit**. Las invariantes de `AGENTS.md` (controllers sin repositorios, pureza de dominio, aislamiento de capas) solo se revisan manualmente en PR. | **Degradación silenciosa de la arquitectura:** Acoplamientos indeseados pueden filtrarse en commits sin que ningún build automatizado falle. | 🟡 Deuda |
| **AP-08** | Dominio Crítico<br>Algoritmos y Estados | **Assertion Quality Blindspot** / **Absence of Mutation Testing**<br>*(Jia & Harman)* | `pom.xml:1-226`<br>0 configuración de **Pitest**. Cobertura de líneas alta en JaCoCo (~80%+), pero sin medición de si los tests realmente matan mutantes en `AlgoritmosService`, `RankingMensual` o transiciones de estado. | **Falsos positivos de cobertura:** Un test puede ejecutar una línea sin asertar su efecto colateral, dejando ramas lógicas críticas sin protección real. | 🟡 Deuda |
| **AP-09** | `integration-tests`<br>`PollingUtils.java` | **Resource Optimism & Blind Polling**<br>*(Meszaros: Flaky Test / Sleepy Test)* | `integration-tests/src/test/java/grupo5/tests/utils/PollingUtils.java:193-242`<br>Uso de `Awaitility.await().atMost(Duration.ofSeconds(8))` con polling continuo para sincronización asíncrona de eventos en Docker. | **Sensibilidad al entorno:** En runners sobrecargados de CI (GitHub Actions 2 vCPUs), 8 segundos puede ser insuficiente, generando fallos espurios (*Flakiness*). | 🟡 Deuda |
| **AP-10** | `common-lib`<br>`pom.xml` | **Shared Kernel Missing Consumer Contract Tests**<br>*(Fowler: Shared Kernel Smells)* | `common-lib/pom.xml:1-87`<br>Librería transversal con AOP, logging, interceptores y adaptadores JPA opcionales, probada únicamente en aislamiento sin pruebas de compatibilidad binaria contra los servicios consumidores. | **Riesgo de ruptura cross-service:** Un cambio en `common-lib` puede compilar en su módulo pero romper `notificaciones-service` o `donaciones-service` en runtime si divergen las versiones de Spring Boot. | 🟡 Deuda |

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

`[OBSERVED]` El archivo `integration-tests/src/test/java/grupo5/tests/contract/ContractIT.java` contiene tres pruebas:
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
`[INFERRED]` **Por qué es un anti-patrón de severidad crítica:**
1. **Verificación Superficial:** El test solo evalúa que Springdoc haya registrado el endpoint. Si el servicio de notificaciones cambia el cuerpo del request de `{ "nombre": "...", "correo": "..." }` a `{ "denominacion": "...", "medios": [...] }`, o cambia el código de respuesta de `200` a `204`, el test **continúa pasando con éxito**.
2. **Falsa Confianza:** Genera un indicador "verde" en el pipeline de CI (`Contract Tests: PASS`), haciendo creer al equipo que los contratos entre servicios están verificados cuando en realidad la compatibilidad binaria y semántica no está siendo testeada.
3. **Mitigación Mandatoria:** Reemplazar esta aserción trivial por validación estricta de request y response contra las especificaciones OpenAPI usando `OpenApiValidationFilter` (Atlassian) o stubs de **WireMock** validados contra la especificación YAML oficial.

---

### 4.2 AP-02: El Antipatrón *Sequential Load Loop* en `PerformanceStressIT.java`

`[OBSERVED]` El archivo `integration-tests/src/test/java/grupo5/tests/performance/PerformanceStressIT.java` implementa la prueba de estrés de creación de donantes y donaciones de la siguiente manera:
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
`[INFERRED]` **Por qué es un anti-patrón de severidad crítica:**
1. **Ausencia de Carga Concurrente:** Una prueba de estrés (*Stress/Load Test*) tiene como objetivo descubrir los cuellos de botella de un sistema bajo concurrencia: agotamiento de conexiones en `HikariCP`, contención en el thread pool de Tomcat, bloqueos (*locks*) a nivel de base de datos o saturación de canales RabbitMQ. Al invocar las peticiones secuencialmente en un bucle `for` de un solo hilo, el sistema se encuentra en reposo absoluto durante cada ciclo.
2. **Métricas de Latencia Distorsionadas:** El cálculo de latencia incluye el tiempo de polling de `esperarReplicacionPersona()`, que duerme el hilo mediante Awaitility. No es una medición del tiempo de procesamiento HTTP del microservicio.
3. **Falso Éxito:** Un sistema que pasa esta prueba puede colapsar inmediatamente en producción si recibe apenas 5 peticiones simultáneas.
4. **Mitigación Mandatoria:** Descartar `PerformanceStressIT` de JUnit y adoptar **k6**, ejecutando scripts JavaScript con rampas de usuarios virtuales (ej. 20-50 VUs), distribución estocástica de peticiones y cálculo preciso de percentiles (p90, p95, p99).

---

### 4.3 AP-03: El Antipatrón *Standalone Setup Blindspot* en Controladores REST

`[OBSERVED]` En 17 clases de prueba de controladores (ej. `donaciones-service`, `logistica-service`, `incentivos-service` y `PersonasControllerTest` en `notificaciones-service`), se inicializa MockMvc con:
```java
mockMvc = MockMvcBuilders.standaloneSetup(controller)
    .setControllerAdvice(new GlobalExceptionHandler())
    .build();
```
Y en `PlanificacionManualControllerTest.java:28`:
```java
mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
```
`[INFERRED]` **Por qué es un anti-patrón de severidad crítica:**
1. **Aislamiento Excesivo:** `standaloneSetup` no levanta el contexto web de Spring. Se ejecuta como un test de unidad puro de Java donde MockMvc actúa simplemente como despachador sintáctico.
2. **Omitir Interceptores y Filtros:** El interceptor de logging (`ControllerLoggingInterceptor`) y el filtro de trazabilidad (`TraceResponseHeaderFilter`) **nunca se ejecutan**. Errores en la inyección de MDC o en el retorno del header `X-Trace-Id` nunca son detectados en estos tests.
3. **Fragilidad ante Olvidos:** Si el desarrollador olvida agregar `.setControllerAdvice(new GlobalExceptionHandler())` (como ocurrió en `PlanificacionManualControllerTest`), las excepciones del controlador resultan en un error 500 no manejado dentro del test en lugar de validar la respuesta estructurada `ErrorResponse` (400 Bad Request o 409 Conflict).
4. **Mitigación Mandatoria:** Migrar gradualmente los tests de controladores a `@WebMvcTest(MiController.class)`, tal como ya se implementó exitosamente en `NotificacionControllerTest.java`, asegurando que toda la infraestructura web autoconfigurada participe en la prueba.

---

## 5. Próximos Pasos

Los hallazgos catalogados en esta matriz constituyen la base empírica para:
1. El **Estudio Comparativo Multidimensional** (`03-estudio-comparativo.md`), donde se evaluarán las herramientas y trade-offs para erradicar cada smell.
2. El **Blueprint de la Arquitectura Target** (`04-blueprint-target.md`), donde se redefinirán los límites, SLA y herramientas de cada capa.
3. El **Roadmap de Migración** (`05-roadmap-migracion.md`), que secuenciará la remediación sin perturbar el desarrollo diario.
