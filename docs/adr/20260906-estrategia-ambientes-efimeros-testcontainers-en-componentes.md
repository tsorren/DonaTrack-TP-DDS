# Estrategia de Ambientes Efímeros: Testcontainers en Componentes y Coexistencia con Docker Compose

- Status: proposed
- Date: 2026-09-06
- Deciders: Lead QA Architect & Principal Systems Engineer (Revisión Crítica)
- Tags: testing, testcontainers, docker-compose, postgresql, rabbitmq, ambientes-efimeros, testing-honeycomb
- Predecesor: [`20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md`](20260901-estrategia-de-testing-de-persistencia-con-testcontainers-frente-a-h2.md) (Complementado y extendido hacia la arquitectura target)

## Contexto y Problema

El ecosistema de pruebas de DonaTrack presenta una polarización extrema entre dos mundos:
1. Pruebas unitarias masivas en memoria (`*Test.java`) que utilizan colecciones en memoria (`CrudRepositoryEnMemoria`), ciegas a la infraestructura real de PostgreSQL y RabbitMQ.
2. Pruebas de integración distribuida (`integration-tests`) que exigen compilar el monorepo completo y levantar 7 contenedores Docker (`docker-compose.preprod.yml`) con ~3 minutos de warmup para validar incluso una simple consulta SQL o un listener AMQP.

Aunque el ADR predecesor `20260901` estableció la superioridad de Testcontainers frente a H2, su implementación actual en `notificaciones-service` (`RepositoriosJpaTest.java`) sufre de smells técnicos: levanta el contexto completo de `@SpringBootTest`, utiliza boilerplate manual con `@DynamicPropertySource` y rutas relativas frágiles a los scripts DDL.

Se requiere una estrategia arquitectónica formal que defina:
- Cómo aislar y testear los componentes de cada microservicio sin levantar el resto de la plataforma.
- Cómo gestionar la coexistencia entre **Testcontainers** y **Docker Compose**.
- Cómo evitar que la falta de Docker bloquee a los desarrolladores en sus máquinas locales.

## Atributos de Calidad y Drivers de Decisión

* **Feedback Rápido y Testeabilidad:** Las pruebas de persistencia y mensajería deben ejecutarse en pocos segundos (< 4s), sin necesidad de levantar servicios ajenos.
* **Fidelidad de Entorno:** PostgreSQL 16 y RabbitMQ reales, utilizando el script DDL canónico de multi-schema (`01-init-schemas-roles.sql`).
* **Resiliencia y Anti-Flakiness:** Eliminación de dependencias cruzadas de red y contención de recursos en runners de CI/CD.
* **Compatibilidad Docente:** Preservación estricta de la suite E2E en Docker Compose para la evaluación de la cátedra.

## Alternativas Consideradas

* **Coexistencia en Dos Niveles (Testing Honeycomb) (`[PROPOSED]`):**  
  - *Nivel de Componente / Slicing:* Testcontainers en cada microservicio con `@ServiceConnection` (Spring Boot 3.1+) para PostgreSQL y RabbitMQ efímeros.
  - *Nivel E2E Distribuido:* Docker Compose preprod para validar el flujo completo distribuido (Matching $\rightarrow$ Feign $\rightarrow$ Logística $\rightarrow$ RabbitMQ $\rightarrow$ n8n $\rightarrow$ Incentivos).
* **Migración Total a Testcontainers (Java-Orchestrated) (`[REJECTED]`):**  
  Orquestar los 4 microservicios + Postgres + RabbitMQ + n8n desde JUnit compitiendo por 7 GB de RAM en CI dispara `OOMKilled` y rompe la compatibilidad con los scripts de evaluación docente de la cátedra.
* **Docker Compose Exclusivo sin Testcontainers (`[REJECTED]`):**  
  Perpetúa el antipatrón del *Cono de Helado*, obligando a levantar 7 contenedores para verificar una consulta JPA puntual.

## Resultado de la Decisión

Alternativa elegida: **"Coexistencia en Dos Niveles (Testing Honeycomb) con Testcontainers en Slicing y Compose en E2E Distribuido"**

Justificación:
Permite a cada microservicio validar su persistencia real (`@DataJpaTest`) y mensajería AMQP de forma desacoplada y en milisegundos utilizando Testcontainers con **Spring Boot 3.1+ `@ServiceConnection`**, eliminando el código verboso de `@DynamicPropertySource`. Al mismo tiempo, preserva Docker Compose preprod como el estándar de entrega y validación black-box para la cátedra UTN-FRBA.

### Consecuencias Positivas

* **Descompresión de la suite E2E:** Los errores de mapeo JPA o sintaxis SQL se detectan en la fase de slicing de componentes de cada servicio, sin esperar al despliegue distribuido.
* **Erradicación de Boilerplate:** `@ServiceConnection` autoconfigura `spring.datasource.*` a partir del contenedor sin `@DynamicPropertySource`.
* **Reutilización de Contenedores:** Con `.withReuse(true)` (Ryuk), el contenedor PostgreSQL se mantiene activo entre suites de un mismo servicio, reduciendo el startup a < 1s.
* **Modo Degradado Automatizado:** La extensión `@DisabledIfDockerUnavailable` desactiva selectivamente los tests de Testcontainers en estaciones de trabajo sin Docker, preservando `mvn test` en verde reportando `[DEFERRED_NO_DOCKER]`.

### Consecuencias Negativas

* Requiere descargar las imágenes OCI de `postgres:16-alpine` y `rabbitmq:3-management` en la primera ejecución local.
* Coexistencia de dos mecanismos de contenedores (Testcontainers en JVM y Compose en scripts bash/powershell), lo que exige mantener la paridad estricta del script `01-init-schemas-roles.sql`.

### Validación

1. Adopción de `org.springframework.boot:spring-boot-testcontainers` en `pom.xml`.
2. Refactor de `RepositoriosJpaTest.java` en `notificaciones-service` eliminando `@DynamicPropertySource` y adoptando `@ServiceConnection`.
3. Verificación de que `mvn test -pl notificaciones-service` ejecute las pruebas de persistencia en < 4 segundos.
4. Verificación de que `./run-preprod-tests.sh` continúe ejecutando la suite distribuida E2E sobre Docker Compose sin alteraciones.
