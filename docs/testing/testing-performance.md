# Optimización de Rendimiento en Compilación y Testing

> **DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones**  
> UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Ámbito:** Guía de buenas prácticas y arquitectura de testing local y CI/CD.

---

## 1. Diagnóstico y Objetivos de Rendimiento

A medida que el ecosistema DonaTrack creció incorporando 5 microservicios (`donaciones-service`, `logistica-service`, `notificaciones-service`, `viandas-service`, `incentivos-service`), una librería compartida (`common-lib`) y una suite de pruebas de integración (`integration-tests`), el ciclo de retroalimentación de testing (Inner Dev Loop) demandaba optimizaciones estructurales:

1. **Re-arranque repetitivo de contextos Spring en slices `@WebMvcTest`:** En `donaciones-service`, cada suite de controlador instanciaba su propio `ApplicationContext` aislado, multiplicando los tiempos de inicialización y el consumo de memoria.
2. **Rigidez de ejecución de pruebas focalizadas (`-Dtest`):** En configuraciones multimódulo de Maven, pasar `-Dtest=MiTest` solía fallar en módulos que no poseían la clase especificada a menos que se utilizara `-DfailIfNoSpecifiedTests=false`.
3. **Falta de Test Impact Analysis (TIA):** Los desarrolladores ejecutaban suites completas incluso ante modificaciones menores o quirúrgicas en controladores o servicios individuales.

Este documento formaliza las optimizaciones implementadas en la compilación, configuración de Surefire, consolidación de contextos Spring Boot y herramientas de TIA.

---

## 2. Optimizaciones en Maven y Surefire

En el `pom.xml` raíz se introdujeron directivas en `pluginManagement` y configuración del compilador:

### A. `<failIfNoSpecifiedTests>false</failIfNoSpecifiedTests>`
* **Problema:** Al ejecutar pruebas quirúrgicas en proyectos multimódulo (ej. `mvn test -Dtest=CategoriasControllerTest`), Surefire abortaba la compilación en los módulos donde dicha clase no existía.
* **Solución:** Al fijar `failIfNoSpecifiedTests` en `false`, Maven saltea limpiamente los módulos sin coincidencias y ejecuta las pruebas únicamente donde el test está presente.

### B. Recolección de Basura Serial en Surefire (`-XX:+UseSerialGC`)
* **Problema:** En máquinas de desarrollo y nodos de CI con múltiples núcleos, los Garbage Collectors concurrentes (G1GC) asignan hilos y estructuras de memoria pesadas para JVMs de prueba efímeras (forks de Surefire).
* **Solución:** La directiva `-XX:+UseSerialGC` reduce drásticamente el overhead de CPU y footprint de memoria de los procesos efímeros de prueba de corta duración.

### C. Procesamiento Completo de Anotaciones (`-proc:full`)
* **Problema:** En Java 21+, `javac` emite advertencias sobre procesamiento implícito de anotaciones cuando se utilizan herramientas de generación de código (Lombok, MapStruct, etc.).
* **Solución:** Se configura `<compilerArgs><arg>-proc:full</arg></compilerArgs>` en `maven-compiler-plugin`, garantizando cumplimiento estricto y eliminando advertencias durante la fase de compilación.

---

## 3. Consolidación de Slices Spring Boot WebMvc (`@WebMvcTest`)

### 3.1 El Problema del Context Churn
En `donaciones-service` existían 10 suites de testing de controladores REST. Cada una declaraba individualmente su propia anotación `@WebMvcTest(MiController.class)` y sus correspondientes dependencias mockeadas (`@MockitoBean`).

Dado que Spring Boot genera una clave de caché de contexto (`MergedContextConfiguration`) basada exactamente en los beans y controladores declarados, el framework instanciaba 10 `ApplicationContext` independientes. Cada inicialización requería:
* Escaneo de componentes y auto-configuración de Spring MVC.
* Registro de serializadores Jackson, convertidores y validadores Bean Validation.
* Inicialización de interceptores de logging (`ControllerLoggingInterceptor`) y manejador de excepciones global (`GlobalExceptionHandler`).

### 3.2 Solución: `AbstractDonacionesWebMvcTest`
Se creó la clase base abstracta `AbstractDonacionesWebMvcTest`, que:
1. Registra **todos los controladores REST** del servicio en una única declaración `@WebMvcTest({ ... })`.
2. Declara todos los mocks comunes de servicios (`@MockitoBean protected ICategoriasService ...`).
3. Importa la configuración transversal (`CommonLibAutoConfiguration`, `LoggingAutoConfiguration`).

```java
@WebMvcTest({
  CategoriasController.class,
  DonacionesController.class,
  DonacionesIndependientesController.class,
  DonantesController.class,
  EntidadBeneficiariaController.class,
  ItemDonacionNormalizadoController.class,
  NecesidadesController.class,
  PersonasController.class,
  PropuestaDeAsignacionController.class,
  SubcategoriasController.class
})
@Import({CommonLibAutoConfiguration.class, LoggingAutoConfiguration.class})
@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("donaciones-webmvc-context")
public abstract class AbstractDonacionesWebMvcTest {
  @Autowired protected MockMvc mockMvc;
  @MockitoBean protected ICategoriasService categoriasService;
  // ... resto de mocks protegidos
}
```

### 3.3 Aislamiento Concurrente con `@ResourceLock`
El proyecto DonaTrack configura ejecución paralela de clases de prueba en JUnit 5:
```xml
junit.jupiter.execution.parallel.mode.classes.default = concurrent
```
Al compartir un mismo `ApplicationContext` entre múltiples clases de test concurrentes, los mocks `@MockitoBean` (mutados mediante `when(...)` y reseteados por Spring entre pruebas) podían sufrir condiciones de carrera si dos clases de controladores se ejecutaban en hilos simultáneos.

Para garantizar determinismo absoluto sin deshabilitar la concurrencia global del resto de la suite:
* Se anotó la clase base con `@Execution(ExecutionMode.SAME_THREAD)` y `@ResourceLock("donaciones-webmvc-context")`.
* En JUnit 5, `@ResourceLock` es `@Inherited`. Esto sincroniza las suites de controladores entre sí, impidiendo carreras en los mocks sobre el contexto compartido.
* **Resultado:** El contexto se inicializa **una sola vez** (~2.5 segundos) y cada suite subsiguiente ejecuta en ~0.2 segundos, ahorrando más de 20 segundos por corrida y reduciendo la presión sobre la memoria.

---

## 4. Test Impact Analysis (TIA) Local

Para evitar re-ejecutar suites completas ante cambios puntuales, se introducen los scripts de análisis de impacto:
* **PowerShell:** `scripts/test-changed.ps1`
* **Bash:** `scripts/test-changed.sh`

### 4.1 Algoritmo de Detección y Escalamiento
Los scripts inspeccionan `git status --porcelain` y `git diff --name-only HEAD` y aplican las siguientes reglas:

| Ámbito de Archivo Modificado | Acción de Testing | Comando Ejecutado |
|---|---|---|
| `docs/**`, `*.md`, metadatos repo | Salteo inteligente | Ninguno (informa salteo de tests Java) |
| `common-lib/**`, `pom.xml` raíz, `docker-compose*` | **Escalamiento a Reactor** | `mvn test` |
| `domain/**`, `entities/**`, `resources/**`, interfaces `I*Service.java` | **Escalamiento a Módulo** | `mvn test -pl <servicio> -am` |
| `*Controller.java`, `*Service.java`, `*Test.java` | **Testing Quirúrgico** | `mvn test -pl <servicio> -am -Dtest=<TargetTest>` |

### 4.2 Flags de Aceleración (`-Fast` / `--fast`)
El flag `-Fast` aplica optimizaciones adicionales:
* Saltea verificación de formato: `-Dspotless.check.skip=true`.
* Tolera tests no encontrados en módulos ajenos: `-DfailIfNoSpecifiedTests=false`.
* Excluye suites que requieran infraestructura Docker: `-Dtest=!*IntegrationTest,!*E2E*`.

### 4.3 Ejemplos de Uso
```powershell
# PowerShell (Windows)
./scripts/test-changed.ps1 -Fast
./scripts/test-changed.ps1 -DryRun
```

```bash
# Bash / Git Bash / WSL
./scripts/test-changed.sh --fast
./scripts/test-changed.sh --dry-run
```

---

## 5. Evaluación Técnica de `mvnd` (Maven Daemon)

El uso de `mvnd` (Maven Daemon de Apache) fue evaluado como alternativa de aceleración local:

### 5.1 Beneficios
* **Persistencia de JVM en memoria:** Mantiene instancias activas de la JVM entre invocaciones sucesivas, amortizando los tiempos de arranque del compilador y plugins.
* **Compilación paralela nativa:** Integra la lógica de compilación concurrente de Takari de forma nativa.
* **Ahorro en bucle interactivo:** En flujos de TDD donde se ejecutan pruebas repetidamente cada pocos segundos, el ahorro acumulado es significativo.

### 5.2 Trade-offs y Desventajas
* **Consumo de memoria residente:** El demonio retiene entre 500 MB y 1.5 GB de RAM por proceso en segundo plano. En máquinas de desarrollo con recursos limitados (<16 GB) o contenedores CI/CD, puede provocar swaps o fallos por OOM.
* **Riesgo de estado sucio en plugins complejos:** Ciertos plugins de análisis estático o generación de código (Spotless, JaCoCo, ArchUnit) pueden retener metadatos o caches en memoria que requieran paradas manuales (`mvnd --stop`).
* **Incompatibilidad en CI efímero:** En pipelines de CI/CD (GitHub Actions) basados en runners desechables, un demonio no aporta beneficios de amortización ya que el runner se destruye al finalizar el job.

### 5.3 Conclusión y Recomendación
Se recomienda el uso **opcional** de `mvnd` para desarrolladores locales que realicen ciclos de codificación intensiva en workstations holgadas de memoria. Para pipelines de CI y validación formal de Quality Gates (`AGENTS.md`), se mantiene `mvn` canónico como estándar inmutable y determinístico.
