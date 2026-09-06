# Fitness Functions Arquitectónicas con ArchUnit y Pruebas de Mutación con Pitest

- Status: proposed
- Date: 2026-09-06
- Deciders: Lead QA Architect & Principal Systems Engineer (Revisión Crítica)
- Tags: testing, arquitectura, fitness-functions, archunit, pitest, mutation-testing, calidad, invariants
- Hallazgos Relacionados: AP-07 (*Absence of Architectural Fitness Functions*) y AP-08 (*Assertion Quality Blindspot*)

## Contexto y Problema

[`AGENTS.md`](../../AGENTS.md) define reglas de gobierno e invariantes arquitectónicas estrictas para DonaTrack:
1. **Controllers como Adaptadores Puros (§4.2):** Los controladores HTTP deben limitarse a validar DTOs de entrada y delegar en servicios de aplicación. No deben inyectar directamente repositorios de datos ni contener lógica de dominio.
2. **Pureza del Modelo de Dominio (§4.1):** Las entidades y agregados en `models/entities/` no deben acoplarse a detalles de infraestructura (anotaciones Spring, DTOs de transporte ni frameworks de persistencia).
3. **Segregación Surefire/Failsafe:** Las pruebas unitarias ejecutadas por Surefire (`*Test.java`) no deben levantar contextos pesados de Spring Boot (`@SpringBootTest`).
4. **Shared Kernel Neutral:** `common-lib` no debe tener referencias acopladas a la lógica de negocio de servicios particulares.

`[OBSERVED]` Actualmente, el repositorio no cuenta con ningún mecanismo automatizado de verificación de estas invariantes. Spotless valida el formato del código Java, pero es ciego a dependencias ilegales entre paquetes. La custodia de la arquitectura recae exclusivamente en revisiones manuales de Pull Request, propensas a errores humanos.

Por otra parte, la suite de pruebas reporta una alta cobertura de líneas y ramas en JaCoCo (~80%+), pero la cobertura de líneas **no mide si las aserciones son capaces de detectar defectos sutiles** (ej. alterar una condición de borde en `AlgoritmosService` o una transición de la máquina de estados de `DonacionIndependiente`).

## Atributos de Calidad y Drivers de Decisión

* **Gobernanza Arquitectónica Automatizada:** Las reglas de diseño deben fallar la compilación (`mvn test`) de forma automática y determinística si se introduce una violación.
* **Feedback Rápido:** La verificación de reglas de diseño no debe demorar más de 500 ms por módulo.
* **Efectividad de Pruebas (Mutation Score):** Garantizar que las pruebas de la lógica de negocio crítica fallen cuando el comportamiento del código es alterado deliberadamente.

## Alternativas Consideradas

* **ArchUnit Universal en `mvn test` + Pitest Acotado con Perfil Dedicado (`-Pmutation-test`) (`[PROPOSED]`):**  
  ArchUnit se ejecuta en memoria en cada `mvn test` validando invariantes estructurales en milisegundos. Pitest se acota exclusivamente a los paquetes de dominio crítico bajo un perfil Maven opcional para no degradar el build diario.
* **ArchUnit y Pitest Obligatorios en el Quality Gate General (`[REJECTED]`):**  
  Ejecutar Pitest sobre todo el monorepo en cada `mvn test` eleva el tiempo de compilación de 15 segundos a más de 20 minutos, provocando que los desarrolladores omitan los tests con `-DskipTests` (*Test Run War*).
* **Custodia Manual mediante Linters Estáticos (`[REJECTED]`):**  
  Dejar las reglas de diseño libradas a la inspección visual humana o linters de estilo conduce a la degradación silenciosa de la arquitectura con el paso de los sprints.

## Resultado de la Decisión

Alternativa elegida: **"ArchUnit Universal en `mvn test` y Pitest Acotado bajo Perfil Dedicado (`-Pmutation-test`)"**

Justificación:
ArchUnit inspecciona el bytecode compilado en memoria utilizando Reflection/ASM en menos de 200 ms por módulo, permitiendo codificar como tests unitarios las invariantes de `AGENTS.md`. Por su parte, Pitest proporciona una auditoría adversarial de la calidad de los tests de dominio, pero al restringirse a las clases de mayor impacto algorítmico y aislarse en un perfil Maven opcional, preserva la agilidad del ciclo de desarrollo diario.

### Reglas ArchUnit Codificadas

1. **Adaptadores Puros:**
   ```java
   classes().that().resideInAPackage("..controllers..")
       .should().onlyDependOnClassesThat()
       .resideInAnyPackage(
           "..services..",
           "..dto..",
           "..common..",
           "java..",
           "org.springframework..",
           "jakarta..",
           "io.swagger..",
           "lombok..",
           "org.slf4j..")
   ```
2. **Pureza de Dominio:**
   ```java
   noClasses().that().resideInAPackage("..models.entities..")
       .should().dependOnClassesThat()
       .resideInAnyPackage("..controllers..", "..infrastructure..", "jakarta.persistence..")
   ```
3. **Segregación Surefire (con migración/exclusión de suites pesadas):**
   ```java
   noClasses().that().haveSimpleNameEndingWith("Test")
       .and().doNotHaveSimpleNameEndingWith("ApplicationTest")
       .and().resideOutsideOfPackage("..integration..")
       .should().beAnnotatedWith(SpringBootTest.class)
   ```
   > [!NOTE]
   > Las 4 clases preexistentes con `@SpringBootTest` que finalizan en `*Test.java` (`DonacionesServiceApplicationTest`, `IncentivosServiceApplicationTest`, `PlanificacionManualFlowIntegrationTest` y `RepositoriosJpaTest`) deben excluirse formalmente por nombre/paquete (como se muestra arriba) o renombrarse a `*IT.java` (Maven Failsafe) antes de activar la regla en el build diario para evitar roturas prematuras.

### Consecuencias Positivas

* **Invariantes Inquebrantables:** Cualquier commit que viole las fronteras de capas o inyecte un repositorio en un controller hace fallar `mvn test` inmediatamente con un mensaje descriptivo.
* **Medición Real de Calidad:** Pitest expone aserciones débiles o tests superficiales en la lógica de matching y máquinas de estado, elevando el nivel de rigor del código.
* **Feedback Preservado:** `mvn test` mantiene su tiempo de ejecución en ~20 segundos para todo el monorepo.

### Consecuencias Negativas

* Requiere que el equipo aprenda la sintaxis fluida de ArchUnit.
* El mantenimiento de mutantes supervivientes en Pitest exige tiempo de análisis cuando se modifica la lógica de negocio.

### Validación

1. Incorporación de `com.tngtech.archunit:archunit-junit5` en `pom.xml`.
2. Creación de tests de arquitectura en cada microservicio que pasen al 100%.
3. Introducción intencional de una violación (ej. inyectar un repositorio en un controller de prueba) y verificación de que ArchUnit falle el build.
4. Ejecución exitosa de `mvn test -Pmutation-test` verificando un mutation score $\ge 75\%$ en el motor de asignación.
