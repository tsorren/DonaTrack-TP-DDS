# Estudio Comparativo Multidimensional de Herramientas y Alternativas de QA (DonaTrack)

> **Documento:** Estudio Comparativo de Herramientas, Trade-Offs y Alternativas  
> **Ubicación:** `docs/testing/auditoria/03-estudio-comparativo.md`  
> **Rol:** Principal Systems Engineer & Lead QA Architect  
> **Documento Complementario:** [`docs/testing/decisiones-diseno-auditoria-qa.md`](../decisiones-diseno-auditoria-qa.md)  
> **Ámbito:** Factual, Empírico y Documental (`SOURCE_READ_ONLY` en `src/`)  
> **Fecha de Evaluación:** 2026-09-06  
> **Estado:** Vigente (`[DOCUMENTED]`)  

---

## 1. Propósito y Criterios de Evaluación

Este documento formaliza el análisis comparativo multidimensional de las tecnologías y herramientas candidatas para la modernización del arnés de testing y QA de **DonaTrack**.

Para contrastar objetivamente cada alternativa frente al contexto técnico de la plataforma (4 microservicios Spring Boot, monorepo Maven, PostgreSQL multi-schema, RabbitMQ, n8n, runners de GitHub Actions con 2 vCPUs y 7 GB de RAM), se definen 6 criterios de evaluación:

1. **Velocidad de Ejecución & Feedback:** Tiempo que demora la herramienta en proporcionar un resultado determinístico al desarrollador.
2. **Fidelidad al Entorno Productivo:** Grado de similitud entre el entorno de prueba y el entorno de producción real (eliminación del síndrome *"en mi máquina funciona"*).
3. **Determinismo & Resistencia al Flakiness:** Capacidad de la prueba para arrojar el mismo resultado bajo idénticas condiciones, sin falsos positivos por problemas de red, puertos o concurrencia externa.
4. **Costo de Mantenimiento & Boilerplate:** Esfuerzo cognitivo, cantidad de código de soporte (*glue code*) y fricción en la evolución de las suites.
5. **Curva de Adopción & Ergonomía:** Facilidad con la que el equipo de desarrollo puede escribir, depurar y comprender las pruebas sin dependencias esotéricas.
6. **Consumo de Recursos en CI/CD:** Demanda de memoria RAM, CPU y disco en los runners estándar de GitHub Actions.

---

## 2. Dimensión 1: Gestión de Entornos Efímeros y Persistencia

### 2.1 Tabla Comparativa Multidimensional

| Criterio | Opción A: Testcontainers en Componentes (`[PROPOSED]`) | Opción B: Docker Compose como Único Entorno (`[REJECTED]`) | Opción C: Bases de Datos en Memoria H2 (`[REJECTED]`) |
|---|:---:|:---:|:---:|
| **Velocidad de Ejecución** | 🟡 **Media** (~2-5s por contenedor reutilizado con Ryuk) | 🔴 **Lenta** (2-4 min para levantar 7 contenedores) | 🟢 **Ultra-Rápida** (< 300 ms en JVM) |
| **Fidelidad Productiva** | 🟢 **Total** (PostgreSQL 16 real con multi-schema y roles) | 🟢 **Total** (PostgreSQL 16 + RabbitMQ reales) | 🔴 **Pésima** (Diferencias severas de dialecto SQL, JSONB y funciones) |
| **Determinismo (Anti-Flaky)** | 🟢 **Alto** (Contenedor efímero por test/suite con mapeo dinámico de puertos) | 🔴 **Bajo** (Colisión de puertos fijos, dependencias cruzadas entre servicios) | 🟢 **Alto** (Aislamiento en memoria JVM) |
| **Mantenimiento / Boilerplate** | 🟢 **Bajo** (Spring Boot 3.1+ `@ServiceConnection` elimina `@DynamicPropertySource`) | 🟡 **Medio** (Mantenimiento de archivos docker-compose y scripts .sh) | 🔴 **Alto** (Mantenimiento de scripts DDL duales para H2 y PostgreSQL) |
| **Curva de Adopción** | 🟢 **Excelente** (Estándar de facto en el ecosistema Spring/Java) | 🟢 **Conocida** (Ya utilizada por el equipo docente y CI) | 🟢 **Fácil** (Tradicional en proyectos legacy) |
| **Consumo de Recursos (CI)** | 🟢 **Bajo** (Solo levanta 1 contenedor PostgreSQL de ~150 MB de RAM por servicio) | 🔴 **Crítico** (Levanta 7 contenedores compitiendo por 7 GB de RAM en CI) | 🟢 **Mínimo** (Incluido en el heap de la JVM) |

### 2.2 Análisis de Alternativas Descartadas y Justificación de la Elección

* **Por qué se descarta H2 (`[REJECTED]`):**  
  `[DOCUMENTED]` Formalmente descartado en el ADR predecesor [`20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md`](../../adr/20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md). H2 no soporta de forma idéntica el particionamiento de schemas (`currentSchema=notificaciones`), los tipos de datos nativos de PostgreSQL (ej. `JSONB`, `UUID` nativo), ni las funciones y disparadores de PostgreSQL 16. Forzar H2 obligaría a mantener scripts de inicialización paralelos, provocando divergencias silenciosas donde los tests pasan en memoria pero fallan en producción.
* **Por qué se descarta Docker Compose Exclusivo (`[REJECTED]`):**  
  Usar únicamente Docker Compose obliga a los desarrolladores a compilar los 4 microservicios y levantar RabbitMQ, PostgreSQL y n8n incluso para verificar una simple consulta JPA o un constraint de unicidad. Esto destruye el bucle de retroalimentación rápida (*Feedback Loop*) de Khorikov y genera cuellos de botella de memoria en CI.
* **Decisión Elegida: Coexistencia en Dos Niveles (`[PROPOSED]`):**  
  - *Nivel de Componente:* **Testcontainers** con `@ServiceConnection` en cada microservicio para validar persistencia JPA y listeners AMQP en segundos.
  - *Nivel E2E Distribuido:* **Docker Compose preprod** preservado intacto para validar la integración global de la plataforma y garantizar la compatibilidad con los requerimientos académicos de la cátedra.
* **Mitigaciones Obligatorias:**
  - Reutilización del script SQL canónico: `persistencia/init-db/01-init-schemas-roles.sql`.
  - Mecanismo de degradación elegante: `@DisabledIfDockerUnavailable` para no bloquear builds en workstations locales sin Docker daemon (`[DEFERRED_NO_DOCKER]`).

---

## 3. Dimensión 2: Verificación de Contratos y Comunicación Inter-Servicios

### 3.1 Tabla Comparativa Multidimensional

| Criterio | Opción A: OpenAPI + JSON Schema + WireMock (`[PROPOSED]`) | Opción B: Consumer-Driven Contracts con Pact (`[REJECTED]`) | Opción C: Spring Cloud Contract (`[REJECTED]`) |
|---|:---:|:---:|:---:|
| **Velocidad de Ejecución** | 🟢 **Ultra-Rápida** (Validación en memoria con stubs locales en ms) | 🟡 **Media** (Descarga y verificación de pactos contra broker) | 🟡 **Media** (Generación dinámica de stubs en build) |
| **Fidelidad Contractual** | 🟢 **Alta** (Verifica contra los OpenAPI YAML y JSON Schemas canónicos) | 🟢 **Muy Alta** (Validación bidireccional basada en interacciones reales) | 🟢 **Alta** (Contratos DSL en Groovy/YAML) |
| **Determinismo (Anti-Flaky)** | 🟢 **Total** (Sin dependencias externas ni servidores brokers) | 🔴 **Medio** (Requiere servidor Pact Broker activo en red) | 🟡 **Medio** (Acoplamiento al ciclo de compilación Maven de otros módulos) |
| **Costo de Mantenimiento** | 🟢 **Mínimo** (Aprovecha las 4 specs OpenAPI y 11 schemas ya existentes en `docs/`) | 🔴 **Muy Alto** (Mantenimiento de `@State` methods y contratos de consumidor en código) | 🔴 **Alto** (Mantenimiento de plugins Maven complejos y DSLs propietarios) |
| **Curva de Adopción** | 🟢 **Nula** (El equipo ya conoce OpenAPI 3.0, JSON Schema y MockMvc) | 🔴 **Alta** (Conceptos complejos de Pact: Pacts, Interactions, Provider States) | 🔴 **Alta** (Curva empinada de Spring Cloud Contract y Groovy DSL) |
| **Sobrecarga de Infraestructura** | 🟢 **Cero** (Librerías embebidas en tiempo de test) | 🔴 **Pesada** (Requiere desplegar y mantener un Pact Broker con BD dedicada) | 🟡 **Media** (Requiere repositorio local/remoto de artefactos de stubs) |

### 3.2 Análisis de Alternativas Descartadas y Justificación de la Elección

* **Por qué se descarta Pact (`[REJECTED]`):**  
  Pact es una herramienta excepcional para organizaciones con decenas de equipos independientes donde la comunicación entre productores y consumidores es asíncrona y descentralizada. Sin embargo, en un monorepo mantenido por un equipo cohesionado de 5 desarrolladores, montar y mantener un **Pact Broker** (con su base de datos, autenticación y webhooks) introduce una sobrecarga operativa y de infraestructura completamente desproporcionada. Además, configurar los métodos de proveedor (`@State`) para preparar el estado de la base de datos en el productor acopla indebidamente los tests de los servicios.
* **Por qué se descarta Spring Cloud Contract (`[REJECTED]`):**  
  Spring Cloud Contract genera stubs JAR que deben ser publicados en el repositorio local de Maven (`.m2`) antes de que el consumidor pueda compilar y testear. Esto rompe la compilación paralela de módulos y genera fallos recurrentes cuando se corre Maven con `-pl <modulo>`. Además, su plugin de Maven presenta frecuentes fricciones de compatibilidad con versiones modernas de Spring Boot y Java 21.
* **Decisión Elegida: OpenAPI 3.0 + JSON Schema + WireMock (`[PROPOSED]`):**  
  `[OBSERVED]` DonaTrack ya cuenta con 4 especificaciones OpenAPI 3.0 (`docs/arquitectura/contratos/`) y 11 esquemas JSON formales (`docs/arquitectura/contratos/schemas/`), además del validador `scripts/validate-contracts.js`.  
  La estrategia target consiste en:
  1. Utilizar **WireMockServer** en los tests de clientes Feign para simular los microservicios externos con respuestas canónicas basadas en los esquemas OpenAPI existentes.
  2. Implementar un filtro de validación en tiempo de test (`swagger-request-validator` de Atlassian) que intercepte las peticiones/respuestas y verifique su conformidad estricta con el YAML de OpenAPI.
  3. Validar los eventos RabbitMQ contra los JSON Schemas mediante `networknt/json-schema-validator`.
  4. La validación Java en tiempo de test **complementa** el validador Node.js existente en CI: el validador Node.js opera estáticamente sobre los archivos YAML/JSON; la validación Java opera dinámicamente sobre los endpoints vivos.

---

## 4. Dimensión 3: Fitness Functions Arquitectónicas y Pruebas de Mutación

### 4.1 Tabla Comparativa Multidimensional

| Criterio | ArchUnit (`[PROPOSED]`) | Linters Estáticos (Spotless/Checkstyle) | Mutation Testing (Pitest) (`[PROPOSED]`) |
|---|:---:|:---:|:---:|
| **Propósito Principal** | Guardián de reglas de diseño y arquitectura | Calidad cosmética, estilo y sintaxis | Medición de la calidad real de las aserciones de prueba |
| **Velocidad de Ejecución** | 🟢 **Ultra-Rápida** (~200 ms por módulo en memoria) | 🟢 **Rápida** (~1-2s en compilación) | 🔴 **Lenta** (Minutos si muta todo el código; segundos si está acotado) |
| **Capacidad Semántica** | 🟢 **Muy Alta** (Entiende clases, paquetes, capas, llamadas y anotaciones) | 🔴 **Nula** (Solo analiza tokens y estructura de texto/código) | 🟢 **Extrema** (Inyecta mutaciones de bytecode y evalúa si los tests fallan) |
| **Resistencia al Refactor** | 🟢 **Alta** (Valida contratos de arquitectura, no detalles) | 🟡 **Media** (Puede quejarse por formateo tras refactoring) | 🟢 **Total** (Valida el comportamiento semántico observable) |
| **Momento Óptimo de CI** | En cada `mvn test` (Fast Quality Gate) | En `compile` / pre-commit (`mvn spotless:check`) | Perfil dedicado (`-Pmutation-test`) o workflow nocturno |

### 4.2 Análisis y Justificación de la Elección

* **Por qué los Linters Estáticos no bastan:**  
  Spotless (Google Java Format) garantiza que el código sea estilísticamente uniforme, pero es completamente ciego a las violaciones de arquitectura: permite que un Controller inyecte un Repositorio, que una entidad de dominio importe librerías de persistencia, o que una clase `*Test` levante un contexto `@SpringBootTest` en tiempo de Surefire.
* **Decisión Elegida: ArchUnit Universal en `mvn test` (`[PROPOSED]`):**  
  Se adopta ArchUnit en todos los módulos para codificar como pruebas ejecutables las invariantes de [`AGENTS.md`](../../../AGENTS.md):
  - *Controllers como Adaptadores Puros:* `classes().that().resideInAPackage("..controllers..").should().onlyDependOnClassesThat().resideInAnyPackage("..services..", "..dto..", "java..")`
  - *Pureza del Dominio:* El paquete `models.entities` no debe acoplarse a Jackson, Spring Data ni clases de infraestructura.
  - *Segregación Surefire/Failsafe:* Las clases `*Test.java` no deben contener `@SpringBootTest`.
* **Decisión Elegida: Pitest Acotado con Perfil Maven (`-Pmutation-test`) (`[PROPOSED]`):**  
  `[REJECTED]` Se descarta la ejecución indiscriminada de Pitest en todo el monorepo en cada build diario, ya que incrementaría los tiempos de CI de 15 segundos a más de 20 minutos, destruyendo el flujo de trabajo del equipo (*Test Run War*).  
  Se opta por **restringir Pitest exclusivamente a paquetes con lógica de negocio compleja**:
  - `grupo5.donaciones.models.entities.donacion` (máquina de estados).
  - `grupo5.donaciones.services.matching` y `AlgoritmosService`.
  - `grupo5.incentivos.services.calculadores` y reglas de ranking.  
  Se aísla bajo el perfil `-Pmutation-test` para ejecuciones locales enfocadas y workflows programados en CI.

---

## 5. Dimensión 4: Pruebas de Rendimiento, Carga y Saturación

### 5.1 Tabla Comparativa Multidimensional

| Criterio | k6 (Grafana Labs) (`[PROPOSED]`) | JUnit Secuencial (`PerformanceStressIT`) (`[REJECTED]`) | Gatling (Scala / Java) (`[REJECTED]`) | Apache JMeter (`[REJECTED]`) |
|---|:---:|:---:|:---:|:---:|
| **Generación de Concurrencia** | 🟢 **Excelente** (Hilos virtuales en Go, mínimo consumo de CPU) | 🔴 **Nula** (Bucle `for` secuencial en un solo hilo) | 🟢 **Excelente** (Modelo de actores Akka/Netty de alto rendimiento) | 🟡 **Media** (Modelo 1 hilo de SO por usuario virtual; pesado) |
| **Definición de Escenarios** | 🟢 **Excelente** (Scripts JavaScript/TypeScript declarativos y limpios) | 🔴 **Pésima** (Código Java con try-catch manuales y cronómetros) | 🟡 **Media** (DSL en Scala o Java verboso) | 🔴 **Pésima** (Archivos XML masivos generados por GUI pesada) |
| **Métricas y Percentiles** | 🟢 **Nativo** (p90, p95, p99, throughput, rate de errores automático) | 🔴 **Manual** (Promedio aritmético con `System.currentTimeMillis()`) | 🟢 **Nativo** (Reportes HTML ricos con percentiles detallados) | 🟢 **Nativo** (Reportes y gráficos de agregación) |
| **Integración en Contenedor** | 🟢 **Perfecta** (Imagen oficial ligera `grafana/k6:latest`, ~30 MB) | 🟡 **Embebida** (Se ejecuta en el runner dentro de JUnit) | 🟡 **Media** (Imagen Docker pesada con JVM) | 🔴 **Pesada** (Imagen Docker compleja de JMeter) |
| **SLA / Thresholds de CI** | 🟢 **Declarativo** (`thresholds: { http_req_duration: ['p(95)<200'] }`) | 🔴 **Frágil** (`assertTrue(average < maxLatency)`) | 🟢 **Declarativo** (`assertions.global.responseTime.percentile3`) | 🟡 **Medio** (Configuración mediante assertions XML) |
| **Amigable con Control de Versiones** | 🟢 **Total** (Archivos `.js` planos fáciles de auditar en diffs de Git) | 🟢 **Total** (Código `.java`) | 🟢 **Total** (Código `.scala` o `.java`) | 🔴 **Pésimo** (Conflictos masivos de merge en archivos `.jmx` XML) |

### 5.2 Análisis de Alternativas Descartadas y Justificación de la Elección

* **Por qué se descarta `PerformanceStressIT` (`[REJECTED]`):**  
  Como se diagnosticó en el hallazgo `AP-02`, el bucle secuencial en JUnit es un espejismo metodológico: no somete al sistema a concurrencia real, no compite por los pools de conexiones y cronometra llamadas con funciones básicas de tiempo que incluyen el overhead de polling de Awaitility. Proporciona una falsa sensación de rendimiento sin valor de ingeniería.
* **Por qué se descartan Gatling y JMeter (`[REJECTED]`):**  
  - *Apache JMeter:* Su paradigma centrado en una interfaz gráfica (GUI) que exporta XML monolíticos (`.jmx`) es incompatible con la filosofía de código auditable de DonaTrack. En equipos colaborativos, los merges de archivos `.jmx` son propensos a corrupción. Además, su modelo de un hilo de sistema operativo por usuario virtual satura rápidamente la memoria de los runners de CI.
  - *Gatling:* Aunque potente, su sintaxis tradicional en Scala o su DSL verboso en Java añaden complejidad innecesaria a un repositorio donde el rendimiento debe evaluarse de forma desacoplada como caja negra contra los endpoints HTTP.
* **Decisión Elegida: Migración Total a k6 (`[PROPOSED]`):**  
  - **Eficiencia y Ligereza:** k6 está escrito en Go y ejecuta scripts JavaScript mediante un motor embebido de alto rendimiento. Puede generar cientos de peticiones por segundo consumiendo una fracción minúscula de CPU y memoria.
  - **Métricas Modernas:** Calcula automáticamente histogramas de latencia, tasa de peticiones por segundo (*RPS*) y percentiles (p90, p95, p99).
  - **Thresholds Nativos:** Permite definir criterios estrictos de aceptación (ej. `p(95) < 500ms`, `rate_errors < 0.01`) que retornan automáticamente un código de salida no-cero si se viola el SLA.
  - **Compatibilidad con Scripts de Cátedra:** Se integra en `docker-compose.preprod.yml` bajo un perfil `--profile perf` y se envuelve en `run-preprod-tests.sh` para responder a la bandera `./run-preprod-tests.sh --groups performance`.

---

## 6. Síntesis de Decisiones y Trade-Offs Aceptados

```text
┌────────────────────────┬──────────────────────────────┬────────────────────────────────────────────────────────┐
│ Dimensión Técnica      │ Solución Seleccionada        │ Trade-Off Aceptado y Mitigación Obligatoria            │
├────────────────────────┼──────────────────────────────┼────────────────────────────────────────────────────────┤
│ Entornos Efímeros      │ Testcontainers en Slices     │ Requiere Docker local en desarrollo. Se mitiga con     │
│                        │ (@ServiceConnection)         │ @DisabledIfDockerUnavailable y modo degradado.         │
├────────────────────────┼──────────────────────────────┼────────────────────────────────────────────────────────┤
│ Contratos Inter-Svc    │ OpenAPI + JSON Schema +      │ No valida interacciones dinámicas de consumidor como   │
│                        │ WireMock en Java             │ Pact; se mitiga validando stubs contra las specs YAML. │
├────────────────────────┼──────────────────────────────┼────────────────────────────────────────────────────────┤
│ Fitness Functions      │ ArchUnit Universal +         │ Pitest es costoso en tiempo de cómputo. Se mitiga      │
│                        │ Pitest Acotado (-Pmutation)  │ restringiéndolo a dominio crítico y perfil dedicado.   │
├────────────────────────┼──────────────────────────────┼────────────────────────────────────────────────────────┤
│ Rendimiento y Carga    │ k6 en Contenedor Docker      │ Introduce JavaScript en suite de carga. Se mitiga      │
│                        │ oficial con scripts limpios  │ con wrappers en run-preprod-tests.sh e imagen OCI.     │
└────────────────────────┴──────────────────────────────┴────────────────────────────────────────────────────────┘
```
