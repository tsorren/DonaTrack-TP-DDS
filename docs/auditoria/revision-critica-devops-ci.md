# Revisión Crítica Experta DevOps: Ecosistema CI/CD, Contenedores y Scripts Auxiliares

> **Proyecto:** DonaTrack — Plataforma de Logística, Trazabilidad y Fidelización de Donaciones  
> **Cátedra:** UTN-FRBA — Diseño de Sistemas (2026) — Grupo 5  
> **Rol:** Revisor Crítico Experto en DevOps y Arquitectura de Infraestructura  
> **Fecha de Emisión:** 2026-09-02  
> **Estado:** 🟢 Factual, Verificado y Contrastado Empíricamente  

---

## 1. Resumen Ejecutivo y Alcance de la Auditoría

El presente informe constituye una **auditoría técnica adversarial y profunda** del ecosistema de Integración Continua (CI), Despliegue Continuo (CD), contenedores Docker, orquestación de pruebas en preproducción y scripts auxiliares del repositorio **DonaTrack**.

Esta revisión se realiza tras la simplificación estructural que desmanteló cuatro flujos automatizados de sobrecarga operacional (`auto-assign.yml`, `cascading-setup.yml`, `issue-auto-assign-cron.yml` e `issue-triage.yml`), con el objetivo de evaluar la robustez, seguridad, testeabilidad y mantenibilidad de la tubería restante.

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                       SUPERFICIE DE AUDITORÍA DEVOPS CANÓNICA                              │
│                                                                                             │
│  [GitHub Workflows]  ──> main.yml, merge.yml, deploy-pages.yml,                             │
│                         pr-reminders.yml, agent-governance.yml                              │
│                                                                                             │
│  [Contenedores / IAC] ──> Dockerfile (×4 microservicios),                                   │
│                         docker-compose.yml, docker-compose.preprod.yml                      │
│                                                                                             │
│  [Scripts Auxiliares] ──> analyze_preprod_logs.py, report_test_failures.py,                 │
│                         run-preprod-tests.sh, run-preprod-tests-stay.sh,                     │
│                         setup-hooks.ps1, pre-commit.sh, generate-pdf-tree.js                │
│                                                                                             │
│  [Documentación / ADR]─> DonaTrack-CICD.md, integration-tests.md,                           │
│                         ADR 20260426 (Pipeline Unificado), DEUDA_TECNICA.md                 │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

> [!IMPORTANT]
> **Alcance Operativo:**  
> En estricto cumplimiento de las directivas del proyecto, **la implementación física de parches o refactorizaciones en los workflows y scripts queda fuera del alcance de esta tarea**. Este informe provee el diagnóstico técnico, la evidencia factual en código y el diseño de alternativas ingenieriles listas para su adopción por el equipo de desarrollo.

---

## 2. Matriz de Inconsistencias Factuales (Documentación vs. Código Real)

La verificación adversarial entre los documentos vigentes (`docs/cicd/DonaTrack-CICD.md`, `docs/testing/integration-tests.md`, ADRs) y el código fuente ejecutable arroja las siguientes discrepancias:

| ID | Área / Componente | Afirmación Documentada | Realidad Factual en Código (`[OBSERVED]`) | Severidad |
|---|---|---|---|:---:|
| **INC-01** | `main.yml` vs `integration-tests.md` | "Las pruebas de integración son ejecutadas mediante el ciclo de verificación de Maven (`mvn verify`)" | En [`.github/workflows/main.yml:L754-L767`](../../.github/workflows/main.yml#L754-L767) se ejecuta `mvn test -pl integration-tests ...` omitiendo las fases estándar de Failsafe (`pre-integration-test`, `integration-test`, `post-integration-test`, `verify`). | 🟡 Media |
| **INC-02** | `scripts/report_test_failures.py` | "El script analiza y reporta exhaustivamente los fallos de la suite de integración" | En [`scripts/report_test_failures.py:L216-L226`](../../scripts/report_test_failures.py#L216-L226) se encuentra hardcodeado el directorio `target/surefire-reports` y la función `main()` concluye con un `return 0` incondicional, ignorando fallos y retornando éxito incluso con tests rotos. | 🔴 Crítica |
| **INC-03** | `docker-compose.preprod.yml` | "Entorno de preproducción autocontenido y determinístico" | En [`docker-compose.preprod.yml:L157`](../../docker-compose.preprod.yml#L157) y [`L200`](../../docker-compose.preprod.yml#L200), imágenes clave (`n8n`, `minio`) utilizan el tag mutable `:latest`, destruyendo el determinismo entre ejecuciones. | 🟡 Media |
| **INC-04** | `merge.yml` vs Organización | "Limpieza universal de paquetes efímeros en GHCR" | En [`.github/workflows/merge.yml:L365`](../../.github/workflows/merge.yml#L365) el endpoint REST de GitHub API está fijado como `users/${REPO_OWNER}/packages/container/...`. Si el repositorio se transfiere o ejecuta bajo una cuenta de Organización, la API devuelve HTTP 404/403 de forma invariable. | 🟡 Media |
| **INC-05** | `deploy-pages.yml` | "Publicación reproducible de Log4brains y entregas" | En [`.github/workflows/deploy-pages.yml:L26-L67`](../../.github/workflows/deploy-pages.yml#L26-L67) se especifica `node-version: lts/*` y se ejecuta `npx --yes log4brains build` sin pin de versión, exponiendo el pipeline a roturas ante updates de npm o upstream packages. | 🟡 Media |

---

## 3. Diagnóstico de Antipatrones Estructurales y Diseño de Tuberías

### 3.1. El "Síndrome del Verde Silencioso" (Silent Failure Masking)
El hallazgo más severo en el pipeline actual es la presencia reiterada de mecanismos de silenciamiento de errores que transforman fallos reales de ejecución en pipelines aparentemente exitosos (falsos negativos):

```text
               ┌────────────────────────────────────────────────────────┐
               │         FLUJO DE ENMASCARAMIENTO SILENCIOSO            │
               └──────────────────────────┬─────────────────────────────┘
                                          │
    1. Ejecución de Analizadores Python   ▼
    [main.yml:771-772] ──────────> python scripts/analyze_preprod_logs.py ... || true
                                   python scripts/report_test_failures.py || true
                                          │
                                          ▼  (Operador || true anula exit code != 0)
    2. Evaluación en Python               │
    [report_test_failures.py:226] ──────> return 0  (Incondicional incluso si failures > 0)
                                          │
                                          ▼
    3. Omisión Total de Pruebas           │
    [report_test_failures.py:120-122] ──> if total == 0: print("⚠️ Sin reportes") -> return 0
                                          │
                                          ▼
    4. Quality Gate Final                 │
    [main.yml:884] ─────────────────────> preprod-validation = SUCCESS (Falso Positivo)
```

* **Operador `|| true` en Steps Diagnósticos:**  
  En [`.github/workflows/main.yml:L771-L772`](../../.github/workflows/main.yml#L771-L772):
  ```yaml
  - name: Generar Reporte Detallado de Fallos y Diagnóstico
    if: always()
    run: |
      python scripts/analyze_preprod_logs.py --run "$EXECUTION_ID" --export-report || true
      python scripts/report_test_failures.py || true
  ```
  Si el script de Python arroja una excepción de sintaxis, falta de librerías o detecta anomalías de severidad `CRITICAL` (como dobles despachos de eventos de dominio), el operador `|| true` intercepta la señal y fuerza el exit code a `0`.
* **Retorno Incondicional de Éxito en `report_test_failures.py`:**  
  En [`scripts/report_test_failures.py:L226`](../../scripts/report_test_failures.py#L226), la rutina principal ejecuta `return 0`. El script jamás emite un código de salida semántico que permita detener un Quality Gate ante un fallo de testeo.
* **Tolerancia Peligrosa a la Ejecución de 0 Pruebas:**  
  Si un error de configuración en Surefire o un filtro erróneo de `-Dgroups` provoca que no se ejecute ninguna prueba (`total_tests == 0`), el script no aborta la ejecución ni marca el step como fallido (`fail-if-no-tests`), dejando pasar builds no verificados.

### 3.2. Desalineación del Ciclo de Vida Maven (Surefire vs. Failsafe)
* **Desviación de Convenciones Maven:**  
  En [`integration-tests/pom.xml:L56-L65`](../../integration-tests/pom.xml#L56-L65), el proyecto fuerza a `maven-surefire-plugin` a ejecutar clases `**/*IT.java`. La convención estándar en el ecosistema Maven y Spring Boot delega las pruebas unitarias a Surefire (fase `test`) y las pruebas de integración a Failsafe (fases `integration-test` y `verify`).
* **Divergencia entre Entorno Local y CI:**  
  Mientras que [`run-preprod-tests.sh:L150`](../../run-preprod-tests.sh#L150) utiliza la fase `verify`, el flujo de CI en [`.github/workflows/main.yml:L754-L762`](../../.github/workflows/main.yml#L754-L762) ejecuta `mvn test`. Si un desarrollador configura el plugin estándar de Failsafe, las pruebas se ejecutarán en local pero se omitirán por completo en GitHub Actions.
* **Fragilidad en la Lectura de Reportes:**  
  Los scripts auxiliares están rígidamente acoplados al path `integration-tests/target/surefire-reports`. Si los reportes se emiten bajo `target/failsafe-reports`, el analizador no los encuentra y concluye con `0` pruebas reportadas sin levantar alertas.

### 3.3. Fragilidad en Observabilidad: Parsing de Texto Libre vs. Logs Estructurados
* **Acoplamiento a Cadenas de Texto y Expresiones Regulares:**  
  En [`scripts/analyze_preprod_logs.py:L101-L103`](../../scripts/analyze_preprod_logs.py#L101-L103) y [`L179-L196`](../../scripts/analyze_preprod_logs.py#L179-L196):
  ```python
  LOG_HEADER_REGEX = re.compile(
      r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s*\|\s*([A-Z]+)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*([^|]+?)\s*\|\s*(.*)$"
  )
  if "[CONTROLLER]" in msg or entry.logger.endswith("ControllerLoggingInterceptor"): ...
  if "[SERVICE-SUCCESS]" in msg: ...
  elif "[SERVICE-ERROR]" in msg: ...
  ```
  El analizador depende de un patrón rígido basado en pipes (`|`) y etiquetas textuales inyectadas por interceptores. Si un desarrollador modifica el formato del patrón en `logback.xml` o renombra una clase de interceptor, el analizador pierde la capacidad de correlacionar eventos de forma silenciosa.
* **Fatiga de Alertas por Clasificación Tosca de Errores:**  
  Cualquier línea con nivel `ERROR` durante los primeros segundos de arranque (como reintentos habituales del driver de conexión de RabbitMQ o PostgreSQL antes de que los contenedores estén completamente arriba) eleva la severidad del análisis a `CRITICAL`, generando ruido y bloqueos espurios.
* **Acoplamiento de Scripts vía Sistema de Archivos Local:**  
  `analyze_preprod_logs.py` escribe su salida en `logs/registro/<run_id>/reporte-analisis.md`, y posteriormente `report_test_failures.py` busca ese archivo mediante `glob.glob(os.path.join(workspace_root, "logs", "registro", "*", "reporte-analisis.md"))`. Este acoplamiento por efectos secundarios en el filesystem es sumamente frágil si se altera la nomenclatura o el orden de invocación.

---

## 4. Vulnerabilidades de Seguridad y Puntos Únicos de Fallo (SPOF)

### [V-01] Ejecución de Contenedores como `root` y Riesgo en Bind Mounts de Logs
* **Evidencia en Código:**  
  En [`donaciones-service/Dockerfile:L22-L30`](../../donaciones-service/Dockerfile#L22-L30) (y réplicas en incentivos, notificaciones y logística):
  ```dockerfile
  FROM eclipse-temurin:21-jre-alpine AS ci
  RUN apk add --no-cache curl
  WORKDIR /app
  ARG JAR_FILE
  COPY ${JAR_FILE} app.jar
  EXPOSE 8080
  ENTRYPOINT ["java", "-jar", "app.jar"]
  ```
  No existe la instrucción `USER`. El runtime ejecuta bajo el usuario `root` (UID 0).
* **Impacto Operativo:**  
  En [`docker-compose.preprod.yml:L28, L60, L97, L131`](../../docker-compose.preprod.yml#L28), los contenedores montan el directorio local `./logs:/app/logs`. Los archivos `.log` generados dentro del runner de GitHub Actions pertenecen a `root:root`. Si pasos posteriores intentan manipular o limpiar dicho directorio sin privilegios de superusuario (`sudo`), la tarea aborta con `PermissionDenied`.  
  En entornos de producción, ejecutar Java como `root` dentro de un contenedor es una vulnerabilidad crítica ante ataques de escape de contenedor (container breakout).

### [V-02] Inyección de Scripts en GitHub Actions (CWE-78)
* **Evidencia en Código:**  
  En [`.github/workflows/main.yml:L71-L98`](../../.github/workflows/main.yml#L71-L98), se aplicó una mitigación parcial asignando `BASE_BRANCH: ${{ github.base_ref }}` y `SRC_BRANCH: ${{ github.head_ref }}` como variables de entorno. Sin embargo, en otros scripts de GitHub Actions que usan Node.js o shell steps se observa interpolación directa de expresiones de contexto de GitHub (`${{ ... }}`) dentro de bloques `run:`.  
  Aunque SonarCloud S7630 fue mitigado en partes de `main.yml` y `merge.yml`, el patrón de interpolación directa en lugar de variables de entorno explícitas debe ser auditado de forma continua para evitar inyecciones remotas de código a través de nombres de ramas maliciosos.

### [V-03] Bootstrapping Frágil y Acoplamiento a IDs Hardcodeados en n8n
* **Evidencia en Código (Previa):**  
  En [`.github/workflows/main.yml`](../../.github/workflows/main.yml) y [`run-preprod-tests.sh`](../../run-preprod-tests.sh) se intentaba importar archivos JSON de un solo workflow sin `--separate`, lo que arrojaba `workflows.map is not a function`, y se invocaba `publish:workflow` (inexistente en n8n 1.x), enmascarado con `|| true`:
  ```bash
  docker compose -f docker-compose.preprod.yml exec -T n8n n8n import:workflow --input=/etc/n8n/workflows/WorkFlow-Insignias.JSON || true
  docker compose -f docker-compose.preprod.yml exec -T n8n n8n import:workflow --input=/etc/n8n/workflows/WorkFlow-Ranking-Mensual.JSON || true
  docker compose -f docker-compose.preprod.yml exec -T n8n n8n publish:workflow --id=1 || true
  docker compose -f docker-compose.preprod.yml exec -T n8n n8n publish:workflow --id=2 || true
  ```
* **Remediación Aplicada:**  
  1. Se utiliza `n8n import:workflow --separate --input=/etc/n8n/workflows`, permitiendo importar archivos individuales con `--separate` y resolviendo la excepción `workflows.map is not a function`.
  2. Se actualiza la imagen a `n8nio/n8n:2.37.7` donde `publish:workflow` es el comando nativo oficial: `n8n publish:workflow --id=1` y `n8n publish:workflow --id=2`.
  3. Se configuran variables de entorno headless (`N8N_ENFORCE_SETTINGS_FILE_PERMISSIONS=false`, `N8N_DIAGNOSTICS_ENABLED=false`).
  4. Se eliminan los operadores `|| true` y `|| warn` para asegurar detección inmediata ante fallos de importación o activación en CI y scripts locales.
  5. Se normalizan los nombres de los archivos de workflows a minúsculas y kebab-case (`workflow-insignias.json` y `workflow-ranking-mensual.json`), resolviendo la omisión silenciosa en Linux causada por el patrón estricto `glob('*.json')` de `fast-glob` en n8n CLI.

### [V-04] Acoplamiento de API GHCR a Cuentas Personales (`/users/`)
* **Evidencia en Código:**  
  En [`.github/workflows/merge.yml:L365`](../../.github/workflows/merge.yml#L365):
  ```bash
  API_BASE="users/${REPO_OWNER}"
  VERSION_ID=$(gh api "${API_BASE}/packages/container/${PACKAGE_SLUG}/versions" ...)
  ```
  La API de GitHub Container Registry bifurca sus rutas según el tipo de propietario de la cuenta:
  - Cuentas de usuario: `/users/{username}/packages/...`
  - Cuentas de organización: `/orgs/{org}/packages/...`
  El workflow asume rígidamente una cuenta personal. Si el repositorio `DonaTrack-TP-DDS` es alojado o migrado bajo una GitHub Organization, la limpieza de tags efímeros falla sistemáticamente, acumulando almacenamiento no gestionado en GHCR.

### [V-05] Uso de Tags Flotantes e Inmutabilidad de Entornos
* **Evidencia en Código:**  
  - En [`docker-compose.preprod.yml:L157`](../../docker-compose.preprod.yml#L157): `image: n8nio/n8n:latest`
  - En [`docker-compose.preprod.yml:L200`](../../docker-compose.preprod.yml#L200): `image: minio/minio:latest`
  - En [`.github/workflows/deploy-pages.yml:L26`](../../.github/workflows/deploy-pages.yml#L26): `node-version: lts/*`
* **Riesgo:**  
  La utilización de `:latest` rompe la reproducibilidad técnica. Una actualización no retrocompatible en n8n o MinIO descargada automáticamente en el runner de CI puede quebrar la suite de integración sin que haya cambiado una sola línea de código en el proyecto.

### [V-06] Colisión de Puertos en Host y Falta de Aislamiento de Red
* **Evidencia en Código:**  
  En [`docker-compose.preprod.yml:L11, L48, L79, L116`](../../docker-compose.preprod.yml#L11), todos los servicios mapean sus puertos directamente al host (`8080:8080`, `8081:8081`, `8082:8082`, `8083:8083`, `5678:5678`, `5432:5432`).  
* **Impacto:**  
  En runners locales de desarrolladores o en runners autohospedados (self-hosted runners) compartidos, si otro proceso ya está ocupando el puerto 8080 o 5432, el arranque falla inmediatamente por `bind: address already in use`. La arquitectura no admite paralelismo multi-job en el mismo host.

### [V-07] Tiempos de Espera Secuenciales Acumulativos
* **Evidencia en Código:**  
  En [`.github/workflows/main.yml:L731-L750`](../../.github/workflows/main.yml#L731-L750), la disponibilidad de `/v3/api-docs` se sondea con un loop secuencial:
  ```bash
  for url in "${URLs[@]}"; do
    until curl -s -f "$url" ... sleep 2; done
  done
  ```
  Con un límite de 60 intentos (120 segundos) por URL, si dos servicios no responden, el pipeline bloquea el runner durante 240 segundos antes de fallar. Este sondeo debe realizarse en paralelo con `wait-for-it` o scripts concurrentes.

---

## 5. Evaluación de los 8 Atributos de Calidad de la Arquitectura

Siguiendo el marco analítico de Bass, Clements y Kazman adoptado por DonaTrack, se califica el estado de la infraestructura DevOps:

```text
┌─────────────────────────────────────────────────────────────────────────────────────────────┐
│                          RADAR DE ATRIBUTOS DE CALIDAD (DEVOPS CI/CD)                       │
│                                                                                             │
│  1. Testeabilidad         [🟡 3.0 / 5.0]  Pruebas robustas pero ciclo Surefire/Failsafe roto│
│  2. Mantenibilidad        [🟢 4.0 / 5.0]  Descubrimiento dinámico excelente; scripts largos │
│  3. Escalabilidad         [🟢 4.0 / 5.0]  Matrices paralelas en GHA con build incremental   │
│  4. Disponibilidad        [🟡 3.2 / 5.0]  SPOFs en n8n, loops secuenciales de 120s          │
│  5. Seguridad             [🔴 2.5 / 5.0]  Contenedores como root, credenciales en compose   │
│  6. Desacoplamiento       [🟡 3.5 / 5.0]  Acoplamiento de scripts por filesystem de logs    │
│  7. Alta Cohesión         [🟢 4.2 / 5.0]  Separación estricta entre PR Pipeline y Merge     │
│  8. Simplicidad           [🟢 4.5 / 5.0]  4 flujos complejos eliminados satisfactoriamente  │
└─────────────────────────────────────────────────────────────────────────────────────────────┘
```

### 1. Testeabilidad (Testability) — Calificación: 🟡 3.0 / 5.0
* **Fortalezas:** La existencia de `integration-tests` con RestAssured, Awaitility y levantamiento de stack completo en preproducción es un estándar de excelencia para un trabajo de cátedra.
* **Debilidades:** El silenciamiento con `|| true` y `return 0` desvirtúa el propósito del testeo en CI. No se puede confiar ciegamente en un check verde si el script no garantiza la aserción de `total_tests > 0`.

### 2. Mantenibilidad (Maintainability) — Calificación: 🟢 4.0 / 5.0
* **Fortalezas:** El descubrimiento dinámico de microservicios basado en `for pom in */pom.xml` en `main.yml` y `merge.yml` permite agregar nuevos servicios sin tocar el YAML del pipeline.
* **Debilidades:** La lógica de scripts en bash embebida en los workflows supera las 80 líneas en varios jobs, dificultando el testing unitario de la propia infraestructura de CI.

### 3. Escalabilidad (Scalability) — Calificación: 🟢 4.0 / 5.0
* **Fortalezas:** Uso eficiente de matrices paralelas (`services-matrix` y `docker-matrix`) y Build Once con publicación a GHCR.
* **Debilidades:** El stack de preproducción es monolítico: levanta siempre los 4 microservicios más RabbitMQ, Postgres, n8n y MinIO, consumiendo importantes recursos de memoria (hasta 3.5 GB de RAM por corrida) en el runner.

### 4. Disponibilidad y Resiliencia (Availability & Resilience) — Calificación: 🟡 3.2 / 5.0
* **Fortalezas:** Reintentos en sondeo de endpoints de salud y serialización de publicaciones en `merge.yml` (`cancel-in-progress: false`).
* **Debilidades:** Si el contenedor de n8n no responde o falla al montar los webhooks en preproducción, el pipeline carece de fallbacks elegantes o mocks de contingencia para las pruebas que no involucran gamificación.

### 5. Seguridad Operacional (Security) — Calificación: 🔴 2.5 / 5.0
* **Fortalezas:** Mapeo confidencial de usuarios mediante secrets (`DISCORD_USER_MAP`) y sanitización de variables de Git Flow en variables de entorno.
* **Debilidades:** Contenedores ejecutando bajo UID 0 (`root`), bind mounts de logs con permisos heredados, contraseñas en texto claro en `docker-compose.preprod.yml` (`POSTGRES_PASSWORD=admin_secure_password`, `MINIO_ROOT_PASSWORD=minioadminpassword`) y endpoint de GHCR no parametrizado para organizaciones.

### 6. Desacoplamiento (Low Coupling) — Calificación: 🟡 3.5 / 5.0
* **Fortalezas:** El Quality Gate opera como un centinela desacoplado con doble eje de contexto (`any-service` y `any-docker-service`).
* **Debilidades:** Los scripts de análisis de logs y reporte de fallos dependen fuertemente de la presencia de carpetas creadas por otros procesos en el sistema de archivos (`logs/registro/`).

### 7. Alta Cohesión (High Cohesion) — Calificación: 🟢 4.2 / 5.0
* **Fortalezas:** La división estricta entre PR Pipeline (`main.yml` para validación y artefactos efímeros) y Merge Pipeline (`merge.yml` para publicación de imágenes estables multi-tag) cumple con el principio de responsabilidad única.

### 8. Simplicidad e Integridad Conceptual (Simplicity) — Calificación: 🟢 4.5 / 5.0
* **Fortalezas:** El desmantelamiento de los flujos de cascada, autoasignación y triage eliminó más de 1.800 líneas de código JavaScript superfluo y dependencias frágiles de la API de GitHub, devolviendo la claridad al ciclo de desarrollo.

---

## 6. Especificación Arquitectónica Consolidada y Análisis de Trade-offs (Los 5 Ejes)

A partir de las evaluaciones adversariales y la definición estratégica del equipo, se formalizan las especificaciones arquitectónicas canónicas organizadas en **5 Ejes de Diseño**, que servirán de base directa para la redacción de los futuros ADRs del proyecto:

---

### Eje 1: Aislamiento Completo de Contenedores y Ciclo de Logs (Alternativa 1.A)

#### 1. Especificación de Diseño
* **Principio Rector:** Cumplimiento del principio *12-Factor App (XI. Logs as event streams)*. Los contenedores no gestionan persistencia local ni escriben en sistemas de archivos montados; emiten eventos estructurados a `stdout`/`stderr`.
* **Capa de Runtime (Dockerfiles):**
  * Se define un usuario y grupo explícito sin privilegios (`USER 1001:1001` o `USER appuser:appgroup`) en la etapa final de ejecución.
  * El contenedor no requiere permisos de escritura en disco salvo directorios efímeros si fueran necesarios (`/tmp` vía `tmpfs`).
* **Capa de Orquestación (`docker-compose.preprod.yml`):**
  * Se eliminan por completo los bind-mounts de volúmenes de host (`./logs:/app/logs`).
  * La captura de telemetría e inspección forense post-mortem se traslada exclusivamente al driver estándar de Docker en el runner o estación local:
    ```bash
    docker compose -f docker-compose.preprod.yml logs --no-color --timestamps > ./docker-preprod-full.log
    ```

#### 2. Matriz de Trade-offs
* **Ventajas (Pros):**
  * **Seguridad (Least Privilege):** Mitiga ataques de escape de contenedor (*container breakout*) al operar bajo UID no privilegiado.
  * **Paridad de Entornos:** Erradica las discrepancias de propiedad de archivos entre estaciones locales (Windows con Docker Desktop, macOS) y runners de CI (Linux Ubuntu), eliminando definitivamente workarounds como `sudo chown`.
  * **Higiene del Host:** El workspace de GitHub Actions permanece inmaculado; no hay riesgo de residuos bloqueados al terminar el job.
* **Desventajas y Mitigaciones (Cons):**
  * Si un contenedor muere súbitamente (*OOMKilled*), la pérdida de logs en buffer previo al flush se mitiga mediante `-XX:+ExitOnOutOfMemoryError` y configuración de buffers acotados en Logback.

---

### Eje 2: Protocolo Semántico de Salida y Quality Gate Estricto (Alternativa 2.B)

#### 1. Especificación de Diseño
* **Principio Rector:** *Determinismo y Fail-Fast Estricto*. Se erradican el operador `|| true` en GitHub Actions y el `return 0` incondicional en los scripts de diagnóstico.
* **Gramática de Códigos de Salida Semánticos:**
  * **`Exit Code 0` (SUCCESS):** Ejecución limpia, suite superada (`total_tests > 0`, `failures == 0`, `errors == 0`) y sin violaciones de invariantes en logs.
  * **`Exit Code 1` (FUNCTIONAL_FAILURE):** Fallo funcional de pruebas unitarias/integración o detección de anomalías arquitectónicas críticas (ej. doble despacho de eventos de dominio, fuga de transacciones). El Quality Gate **debe abortar** el PR.
  * **`Exit Code 2+` (INFRASTRUCTURE_CRASH):** Fallo del arnés de prueba (error de sintaxis de Python, stacktrace del script, timeout de base de datos o directorios ausentes). El step falla explícitamente notificando rotura de infraestructura.
* **Política `fail-if-no-tests`:**
  * Si el job de preproducción se ejecutó pero la suite reporta `total_tests == 0` (por exclusión accidental de tags o configuración rota de plugins), el script emite `::error::` y retorna **`Exit Code 1`**.

#### 2. Matriz de Trade-offs
* **Ventajas (Pros):**
  * **Erradicación de Falsos Negativos:** Se elimina el "síndrome del verde silencioso". Una PR nunca avanzará con cobertura nula o pruebas rotas.
  * **Diagnóstico Inmediato:** El equipo distingue al instante si la falla es de negocio (código 1) o de infraestructura/herramientas (código 2).
* **Desventajas y Mitigaciones (Cons):**
  * Aumenta la rigidez del pipeline ante fluctuaciones de red transitorias; se mitiga asegurando healthchecks con probes estables antes de disparar la suite de pruebas.

---

### Eje 3: Estandarización Canónica del Ciclo de Vida Maven (Alternativa 3.A)

#### 1. Especificación de Diseño
* **Principio Rector:** *Convención sobre Configuración (Maven Lifecycle Separation)*. Aislamiento estricto de responsabilidades entre pruebas unitarias y de integración.
* **Segmentación de Plugins:**
  * **`maven-surefire-plugin` (Fase `test`):** Responsable exclusivo de pruebas unitarias aisladas (`**/*Test.java`). No levanta contexto Spring completo ni servicios externos; falla rápido en memoria.
  * **`maven-failsafe-plugin` (Fases `integration-test` y `verify`):** Responsable de pruebas de integración, contratos y E2E (`**/*IT.java`). Garantiza la ejecución de la fase `post-integration-test` para desmantelar recursos incluso si las pruebas fallan.
* **Alineación de Comandos y Tooling:**
  * El pipeline de CI y los scripts locales invocan `mvn verify` (no `mvn test`).
  * `report_test_failures.py` se parametriza para aceptar flags repetibles:
    ```bash
    python scripts/report_test_failures.py --dir target/surefire-reports --dir target/failsafe-reports --fail-on-zero
    ```

#### 2. Matriz de Trade-offs
* **Ventajas (Pros):**
  * **Integridad de Métricas:** No se contaminan los reportes de cobertura de JaCoCo ni el análisis estático de SonarCloud mezclando tests unitarios con tests de caja negra.
  * **Paridad Local-CI:** `run-preprod-tests.sh` y `main.yml` ejecutan exactamente las mismas fases y metas del reactor.
* **Desventajas y Mitigaciones (Cons):**
  * Requiere actualizar la configuración de `pom.xml` en `integration-tests` eliminando el hack de Surefire sobre `*IT.java`.

---

### Eje 4: Observabilidad Estructurada con Logstash JSON (Alternativa 4.A)

#### 1. Especificación de Diseño
* **Principio Rector:** *Eventos Semánticos Tipados frente a Cadenas Libres*.
* **Capa de Aplicación (`common-lib`):**
  * Se incorpora `net.logstash.logback:logstash-logback-encoder` al Shared Kernel.
  * En perfil de preproducción/producción, Logback emite en formato **NDJSON** (Newline Delimited JSON) directamente a la consola.
  * Los interceptores de logging (`ControllerLoggingInterceptor`, `ServiceLoggingAspect`) enriquecen el contexto **MDC** con campos estructurados tipados:
    * `traceId`, `spanId`, `serviceName`, `eventType` (ej. `HTTP_IN`, `SERVICE_CALL`, `AMQP_DISPATCH`), `executionTimeMs`, `errorCode`.
* **Capa de Análisis (`analyze_preprod_logs.py`):**
  * Se retira la expresión regular frágil `LOG_HEADER_REGEX`.
  * La lectura se realiza mediante `json.loads(line)` seguro. La categorización de errores no se basa en strings arbitrarios como `[SERVICE-ERROR]` sino en la presencia del atributo `entry["eventType"] == "EXCEPTION"` o `entry["level"] == "ERROR"`.

#### 2. Matriz de Trade-offs
* **Ventajas (Pros):**
  * **Inmunidad a Refactors:** Renombrar clases o cambiar formatos de salida visual no rompe los analizadores.
  * **Filtrado Fino (Anti-Fatiga de Alertas):** Permite ignorar programáticamente errores transitorios de inicialización durante los primeros $N$ segundos filtrando por `context.stage == "BOOTSTRAP"`.
  * **Preparación Cloud-Native:** La salida JSON nativa es consumible directamente por stacks ELK, Grafana Loki o Datadog sin parsers intermedios.
* **Desventajas y Mitigaciones (Cons):**
  * La lectura directa del log en terminal cruda es menos legible para un humano sin herramientas como `jq`. Se mitiga manteniendo un perfil `local-dev` con salida tradicional por patrón de consola si el desarrollador no activa el modo JSON.

---

### Eje 5: Nivel de Abstracción y Roadmap hacia ADRs (Alternativa 5.A)

#### 1. Especificación de Diseño
* Las decisiones se mantienen en el nivel de **diseño arquitectónico, contratos de interfaz, flujo de datos y trade-offs**, evitando la sobre-especificación de scripts desechables o código monolítico.
* **Mapeo para Formalización de ADRs:**
  Una vez aprobadas estas especificaciones por el equipo humano, se estructurarán los siguientes registros formales en MADR (`Status: proposed`):
  1. `docs/adr/20260903-aislamiento-contenedores-y-recoleccion-logs-sin-volumenes-host.md` (Eje 1)
  2. `docs/adr/20260903-protocolo-salida-semantico-y-quality-gate-estricto.md` (Eje 2)
  3. `docs/adr/20260903-estandarizacion-ciclo-vida-testing-surefire-failsafe.md` (Eje 3)
  4. `docs/adr/20260903-observabilidad-estructurada-ndjson-y-trazabilidad-mdc.md` (Eje 4)

---

### Resumen Visual de la Arquitectura Objetivo

```text
┌──────────────────────────────────────────────────────────────────────────────────┐
│                         ARQUITECTURA DEVOPS DEFINITIVA                           │
├──────────────────────────────────────────────────────────────────────────────────┤
│                                                                                  │
│  [Microservicio Java 21]                                                         │
│     │ • Non-Root (USER 1001:1001)                                                │
│     │ • Logstash JSON Encoder (MDC tipado: traceId, eventType)                   │
│     ▼                                                                            │
│  [Docker Engine (Preprod)]                                                       │
│     │ • Stdout / Stderr stream (Sin bind-mounts ./logs)                          │
│     ▼                                                                            │
│  [CI Runner Capture: docker compose logs] ──> [analyze_preprod_logs.py]          │
│                                                  • JSON parser determinístico    │
│                                                  • Códigos: 0, 1 (Bug), 2 (Infra)│
│                                                  • Umbral mínimo: total > 0      │
│                                                                                  │
│  [Maven Verify Reactor]                                                          │
│     │ • Surefire (*Test.java) ──> target/surefire-reports/                        │
│     │ • Failsafe (*IT.java)   ──> target/failsafe-reports/                        │
│     ▼                                                                            │
│  [report_test_failures.py --dir (ambos)] ──> Exit Code semántico (Fail-Fast)    │
│                                                                                  │
│  [Quality Gate Consolidado]                                                      │
│     └─► Verde garantizado 100% verificado sin enmascaramiento || true            │
└──────────────────────────────────────────────────────────────────────────────────┘
```

---

## 7. Matriz de Priorización y Roadmap de Remediación

Para una implementación gradual y sin disrupciones en el calendario académico de entregas, se formaliza el siguiente roadmap priorizado:

```text
Impacto en Confiabilidad / Seguridad
       ▲
  Alto │  [P1] Retiro de || true y fail-if-no-tests    [P2] Usuario Non-Root en Dockerfiles
       │  [P1] Multi-dir en report_test_failures.py    [P2] Aislamiento n8n y endpoints OCI
       │
 Medio │  [P3] Logging JSON con Logstash               [P3] Composite Actions locales
       │  [P3] Native GitHub Pages API
       │
       └────────────────────────────────────────────────────────────────────────────────►
         Bajo                                                           Alto    Esfuerzo
```

| Nivel | Ítem / Acción Técnica | Eje Vinculado | Justificación de Ingeniería |
|---|---|:---:|---|
| **P1 (Inmediato)** | **Eliminación de `|| true` y `return 0` incondicional** | Eje 2 | Erradica el *síndrome del verde silencioso*. Asegura que los fallos de pruebas o anomalías críticas detengan el pipeline. |
| **P1 (Inmediato)** | **Validación estricta de `total_tests > 0` (`fail-on-zero`)** | Eje 2 | Evita que una suite desalineada con 0 tests ejecutados pase desapercibida. |
| **P1 (Inmediato)** | **Soporte multi-directorio (`--dir`) en reportes** | Eje 3 | Permite compatibilidad simultánea con Surefire y Failsafe. |
| **P2 (Medio Plazo)** | **Usuario Non-Root en Dockerfiles (`USER 1001:1001`)** | Eje 1 | Elimina la ejecución como root y el problema de propiedad en bind mounts de logs. |
| **P2 (Medio Plazo)** | **Eliminación de bind-mounts `./logs` en Compose** | Eje 1 | Captura de logs centralizada vía `docker compose logs` conforme a 12-Factor App XI. |
| **P2 (Medio Plazo)** | **Sanitización de importación en n8n** | Infra | Desacopla la activación de webhooks de IDs enteros hardcodeados (`--id=1`). |
| **P2 (Medio Plazo)** | **Compatibilidad universal de endpoint GHCR (`/users/` vs `/orgs/`)** | Infra | Evita roturas de limpieza ante migraciones de organización. |
| **P3 (Arquitectural)**| **Migración a Logs Estructurados en JSON (Logstash)** | Eje 4 | Erradica el parsing por expresiones regulares de texto plano y mitiga la fatiga de alertas. |
| **P3 (Arquitectural)**| **Descomposición en Composite Actions locales** | CI/CD | Aumenta la mantenibilidad y testeabilidad de los pasos de CI. |
| **P3 (Arquitectural)**| **Adopción de GitHub Pages API Nativa (`actions/deploy-pages@v4`)** | CI/CD | Elimina commits basura a `gh-pages` y revoca `contents: write`. |

---

## 8. Conclusión del Revisor Crítico

La eliminación de los 4 flujos experimentales (`auto-assign`, `cascading-setup`, `issue-auto-assign-cron` e `issue-triage`) ha significado una **notable mejora en la simplicidad e higiene operativa del repositorio**, reduciendo el ruido en GitHub Actions y removiendo más de una decena de archivos huérfanos.

Con la adopción de las decisiones estratégicas plasmadas en los **5 Ejes de Diseño** (Aislamiento de Contenedores 1.A, Protocolo Semántico 2.B, Estandarización Maven 3.A, Observabilidad JSON 4.A y Abstracción Arquitectónica 5.A), el proyecto dispone de un marco de ingeniería claro, robusto y verificable.

Una vez aceptada formalmente esta propuesta por el equipo humano, la formalización en los 4 ADRs vinculados (`20260903-*`) completará el ciclo de gobernanza técnica, consolidando un pipeline con estándares industriales de excelencia.
