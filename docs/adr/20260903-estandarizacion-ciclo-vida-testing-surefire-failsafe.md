# Estandarización Canónica del Ciclo de Vida de Testing con Surefire y Failsafe

- Status: proposed
- Date: 2026-09-03
- Deciders: Decisión Grupal
- Tags: maven, testing, surefire, failsafe, integration-tests, ciclo-de-vida

## Contexto y Problema

El ecosistema de pruebas automatizadas de DonaTrack combina pruebas unitarias de lógica pura y pruebas de integración de caja negra distribuidas (`integration-tests`). Sin embargo, existía una divergencia con las convenciones estándar del ciclo de vida de Apache Maven:

1. **Forzado Antinatural de Surefire para Pruebas de Integración:** En `integration-tests/pom.xml`, el plugin `maven-surefire-plugin` estaba sobreescrito para incluir patrones `**/*IT.java`. Por convención estándar en Maven y Spring Boot, `maven-surefire-plugin` se liga a la fase `test` para pruebas unitarias (`*Test.java`), mientras que `maven-failsafe-plugin` debe gestionar las pruebas de integración en las fases `integration-test` y `verify` (`*IT.java`).
2. **Divergencia entre CI y Scripts Locales:** En el pipeline de GitHub Actions (`.github/workflows/main.yml`) se invocaba `mvn test -pl integration-tests`, mientras que en los scripts de ejecución local (`run-preprod-tests.sh`) se utilizaba `mvn verify`. Si un desarrollador adoptaba la convención canónica de Failsafe, sus tests corrían en local pero eran ignorados silenciosamente en el pipeline de CI.
3. **Omisión del Desmantelamiento Seguro de Recursos:** La fase `test` de Surefire no garantiza la ejecución de pasos de limpieza ante fallos catastróficos. `maven-failsafe-plugin` garantiza la ejecución de la fase `post-integration-test`, asegurando que contenedores o conexiones se cierren limpiamente incluso si un test lanza una excepción no controlada.
4. **Acoplamiento Rígido de Tooling de Reporte:** Los scripts de Python (`report_test_failures.py`) buscaban reportes XML exclusivamente bajo el path `target/surefire-reports`, imposibilitando la lectura de `target/failsafe-reports`.

Se requiere estandarizar el ciclo de vida de pruebas del repositorio asegurando **separación de responsabilidades**, **integridad de métricas** y **paridad estricta entre entornos**.

## Atributos de Calidad y Drivers de Decisión

* **Mantenibilidad y Convención sobre Configuración:** Respeto estricto del ciclo de vida estándar de Maven para minimizar configuraciones ad-hoc y sorpresas al integrar nuevas herramientas.
* **Integridad de Métricas:** Evitar la contaminación cruzada entre reportes de cobertura unitaria (JaCoCo) y pruebas de integración E2E en SonarCloud.
* **Paridad de Ejecución (Local vs CI):** El mismo comando debe disparar exactamente la misma suite tanto en la terminal del desarrollador como en el runner de CI.

## Alternativas Consideradas

* **Alternativa 3.A — Estandarización Canónica Surefire / Failsafe (Elegida):**
  - **Surefire (`maven-surefire-plugin`):** Vinculado a la fase `test`. Ejecuta exclusivamente `**/*Test.java` (pruebas unitarias, rápidas, aisladas en memoria y sin levantar contexto Spring).
  - **Failsafe (`maven-failsafe-plugin`):** Vinculado a las fases `integration-test` y `verify`. Ejecuta exclusivamente `**/*IT.java` (pruebas de integración, contratos REST, Testcontainers y E2E).
  - **Alineación de Comandos:** Tanto `main.yml` como `run-preprod-tests.sh` invocan la meta canónica `mvn verify`.
  - **Tooling Multi-Directorio:** `report_test_failures.py` se parametriza para soportar múltiples directorios de reporte (`--dir target/surefire-reports --dir target/failsafe-reports`).

* **Alternativa 3.B — Mantener Surefire Unificado para Todo Tipo de Pruebas:**
  - Seguir ejecutando `*IT.java` dentro de la fase `test` mediante Surefire y modificar `run-preprod-tests.sh` para usar `mvn test`.
  - *Descarte:* Viola las convenciones de la industria, priva al proyecto de la fase `post-integration-test` para teardown de recursos y genera reportes anómalos en plugins de análisis estático.

* **Alternativa 3.C — Perfiles Maven Separados (`-Punit` vs `-Pintegration`):**
  - Crear perfiles Maven independientes con inclusión/exclusión de suites.
  - *Descarte:* Introduce complejidad innecesaria en el `pom.xml` padre y obliga a los desarrolladores a recordar flags complejos en lugar de confiar en el ciclo natural de fases de Maven.

## Resultado de la Decisión

Alternativa elegida: **Alternativa 3.A — Estandarización Canónica Surefire / Failsafe**

### Justificación:
La separación canónica de plugins en Maven es un principio ampliamente consolidado. Permite ejecutar pruebas unitarias ultrarrápidas durante el desarrollo (`mvn test`) sin penalización de levantar infraestructura pesada, reservando las fases `integration-test` y `verify` para los escenarios distribuidos con Docker y bases de datos.  
Garantiza además que la fase `post-integration-test` siempre se ejecute para limpiar recursos o recolectar métricas de cierre, incluso si ocurren fallas en los tests de integración.

### Consecuencias Positivas:
* **Convenciones Respetadas:** Cualquier desarrollador o analizador estático identifica de inmediato el tipo de prueba por su sufijo (`*Test.java` vs `*IT.java`).
* **Paridad 100% Local y CI:** `mvn verify` ejecuta uniformemente la totalidad de la pirámide de pruebas en cualquier entorno.
* **Métricas Desacopladas:** JaCoCo y SonarCloud pueden generar informes diferenciados para cobertura de código unitaria frente a pruebas de caja negra.
* **Teardown Seguro:** Garantía de limpieza en `post-integration-test`.

### Consecuencias Negativas y Mitigaciones:
* **Modificación de Configuración Pom:** Requiere actualizar `integration-tests/pom.xml` eliminando la sobrescritura forzada de Surefire y declarando `maven-failsafe-plugin`.
  - *Mitigación:* Se realiza de forma quirúrgica en el módulo `integration-tests` sin afectar al resto de los microservicios.

## Referencias y Trabajo Futuro

* [`docs/auditoria/revision-critica-devops-ci.md`](../auditoria/revision-critica-devops-ci.md): Eje 3 de la revisión crítica DevOps.
* [`docs/testing/integration-tests.md`](../testing/integration-tests.md): Arquitectura de pruebas de integración.
