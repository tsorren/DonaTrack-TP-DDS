# Migración de Pruebas de Rendimiento y Carga a k6

- Status: proposed
- Date: 2026-09-06
- Deciders: Lead QA Architect & Principal Systems Engineer (Revisión Crítica)
- Tags: testing, performance, k6, grafana, docker, carga, estres, latencia, sla
- Hallazgo Relacionado: AP-02 (*Sequential Load Loop* en `PerformanceStressIT.java`)

## Contexto y Problema

`[OBSERVED]` La prueba de rendimiento y estrés de la plataforma DonaTrack está implementada actualmente en la clase Java `integration-tests/src/test/java/grupo5/tests/performance/PerformanceStressIT.java`.

Un análisis crítico de esta prueba revela deficiencias metodológicas severas:
1. **Ausencia de Concurrencia Real:** Ejecuta peticiones HTTP secuenciales en un bucle sincrónico de un solo hilo (`for (int i = 0; i < totalRequests; i++)`). El sistema se encuentra en reposo durante cada llamada; no existe contención de hilos en Tomcat, saturación del pool de conexiones `HikariCP` ni bloqueos de transacciones concurrentes en PostgreSQL.
2. **Medición Distorsionada:** Cronometra las llamadas con `System.currentTimeMillis()`, acumulando tiempos de polling de Awaitility (`esperarReplicacionPersona()`), lo que distorsiona las métricas de latencia de red.
3. **Carencia de Rigor Estadístico:** No calcula percentiles (p90, p95, p99) ni histogramas de distribución; únicamente evalúa un promedio aritmético básico frente a un umbral arbitrario.
4. **Impacto en Tiempos de CI/CD:** Si la suite corre completa, insume entre 2 y 4 minutos ejecutando peticiones secuenciales innecesarias.

Se requiere una solución moderna de pruebas de carga que permita evaluar la saturación del sistema bajo concurrencia real, sin sacrificar la compatibilidad con los scripts de entrega de la cátedra.

## Atributos de Calidad y Drivers de Decisión

* **Generación Eficiente de Concurrencia:** Capacidad de simular decenas o cientos de usuarios virtuales simultáneos sin consumir excesiva CPU o memoria.
* **Métricas Percentilares y SLA Estricto:** Definición declarativa de umbrales de aceptación basados en percentiles (ej. `p(95) < 500ms`, tasa de error $< 1\%$).
* **Mantenibilidad y Código Auditable:** Scripts declarativos, legibles y versionables en Git sin formatos binarios ni XMLs complejos.
* **Compatibilidad Docente:** El script canónico de la cátedra (`./run-preprod-tests.sh --groups performance`) debe seguir funcionando.

## Alternativas Consideradas

* **Migración Total a k6 (Grafana Labs) en Contenedor Docker (`[PROPOSED]`):**  
  Descartar `PerformanceStressIT` de JUnit y crear scripts modulares en JavaScript ejecutados mediante el contenedor oficial `grafana/k6`, integrados en `docker-compose.preprod.yml` y envueltos en `run-preprod-tests.sh`.
* **Enfoque Híbrido (Conservar `PerformanceStressIT` en JUnit + k6) (`[REJECTED]`):**  
  Obliga a mantener dos suites de prueba redundantes escritas en lenguajes distintos, perpetuando un test metodológicamente defectuoso en Java.
* **Optimización con Virtual Threads en Java 21 (`[REJECTED]`):**  
  La JVM que genera los hilos virtuales compite por la CPU y los sockets del host contra los propios microservicios bajo prueba (*Coordinated Omission*), distorsionando los resultados. Además, JUnit carece de tooling nativo para reportes de carga.
* **Apache JMeter o Gatling (`[REJECTED]`):**  
  JMeter genera archivos XML monolíticos propensos a corrupción en Git. Gatling introduce dependencias pesadas y complejidad innecesaria.

## Resultado de la Decisión

Alternativa elegida: **"Migración Total de Pruebas de Rendimiento a k6 en Contenedor Docker"**

Justificación:
k6 es una herramienta moderna de ingeniería de rendimiento escrita en Go que ejecuta scripts livianos en JavaScript. Genera usuarios virtuales concurrentes con un consumo mínimo de recursos, calcula automáticamente percentiles precisos y permite definir *thresholds* de SLA estrictos que determinan el código de salida del comando. Su ejecución en un contenedor oficial (`grafana/k6`) garantiza portabilidad total y desacoplamiento del ciclo de compilación de Maven.

### Consecuencias Positivas

* **Pruebas de Estrés Realistas:** Simulación de rampas de 20 a 50 usuarios virtuales concurrentes compitiendo por los pools de conexiones y los recursos de base de datos.
* **Métricas Profesionales:** Generación de reportes automáticos en terminal con latencia mínima, media, mediana, p90, p95, p99 y throughput (RPS).
* **Desacoplamiento de JUnit:** La suite de integración Java (`integration-tests`) queda reservada para pruebas funcionales y contratos, reduciendo su tiempo de ejecución en más de 2 minutos.
* **Compatibilidad Docente Preservada:** El script `./run-preprod-tests.sh` incluye un wrapper que, al detectar la bandera `--groups performance`, levanta y ejecuta el contenedor de k6, reportando el resultado al usuario de forma transparente.

### Consecuencias Negativas

* Requiere que el entorno de ejecución tenga Docker activo para correr las pruebas de rendimiento.
* Los desarrolladores deben familiarizarse con la API de k6 en JavaScript para escribir nuevos escenarios de carga.

### Validación

1. Creación de los scripts `donaciones-creacion-carga.js` e `incentivos-eventos-saturacion.js` en `tests/performance/k6/`.
2. Configuración del servicio `k6` en `docker-compose.preprod.yml` con perfil `perf`.
3. Ejecución exitosa de `./run-preprod-tests.sh --groups performance` comprobando que el contenedor de k6 se inicie, mida la latencia y devuelva código 0.
4. Eliminación definitiva del archivo `PerformanceStressIT.java` de `integration-tests/src/test/java/`.
