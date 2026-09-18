# Aislamiento Completo de Contenedores y Recolección de Logs sin Volúmenes de Host

- Status: proposed
- Date: 2026-09-03
- Deciders: Decisión Grupal
- Tags: docker, devops, seguridad, 12-factor, logs, preproduccion, non-root

## Contexto y Problema

Durante la ejecución de las pruebas de integración en preproducción (`docker-compose.preprod.yml`), tanto en runners de GitHub Actions como en estaciones locales de desarrollo (Windows con Docker Desktop, macOS y Linux), se identificaron severos problemas de permisos y seguridad derivados de la arquitectura de contenedores y el manejo de logs:

1. **Ejecución como Root:** Los Dockerfiles de los cuatro microservicios (`donaciones-service`, `incentivos-service`, `logistica-service`, `notificaciones-service`) compilan y ejecutan el proceso Java bajo el usuario `root` (UID 0), al no definir una directiva `USER`. Esto viola el principio de menor privilegio e incrementa la superficie de ataque ante vulnerabilidades de escape de contenedor (*container breakout*).
2. **Contaminación del Host por Bind-Mounts:** En `docker-compose.preprod.yml`, cada contenedor montaba el volumen de host `./logs:/app/logs`. Los archivos `.log` generados dentro de los contenedores en runners Linux de GitHub Actions pertenecían al usuario `root:root`. Esto forzó la introducción de workarounds como `sudo chown -R $USER:$USER logs/` para permitir que scripts posteriores pudieran leer o limpiar dichos archivos, requiriendo runners con privilegios `sudo` sin password.
3. **Disparidades entre Estaciones de Trabajo:** En entornos Windows y macOS, los drivers de virtualización de Docker gestionan los permisos de forma heterogénea, ocultando fallos de propiedad que luego rompían el pipeline de CI en Linux.

Se requiere formalizar una arquitectura de ejecución de contenedores y telemetría que garantice **seguridad operacional**, **paridad de entornos** y **aislamiento completo del sistema de archivos del host**.

## Atributos de Calidad y Drivers de Decisión

* **Seguridad (Security / Least Privilege):** Ningún contenedor debe ejecutar procesos de aplicación bajo el superusuario `root`.
* **Paridad de Entornos (Portability & Portability Parity):** El comportamiento del entorno debe ser idéntico en runners de CI Linux y estaciones locales Windows/macOS sin requerir privilegios `sudo`.
* **Higiene del Host (Decoupling):** El workspace del runner debe mantenerse inmaculado, sin efectos secundarios ni archivos con permisos heredados que impidan la limpieza automática.
* **Alineación con Estándares Cloud-Native:** Adopción del principio *12-Factor App (XI. Logs as event streams)*.

## Alternativas Consideradas

* **Alternativa 1.A — Aislamiento Completo sin Volúmenes de Host (Elegida):**
  - **Runtime Non-Root:** Creación de un usuario y grupo dedicado sin privilegios (`USER 1001:1001` o `USER appuser:appgroup`) en la etapa final de cada Dockerfile.
  - **Eliminación de Bind-Mounts:** Erradicar `./logs:/app/logs` de `docker-compose.preprod.yml`. Los contenedores emiten sus logs exclusivamente a `stdout`/`stderr`.
  - **Recolección Centralizada de Logs:** La captura de telemetría para análisis post-mortem se realiza externamente a través del demonio de Docker en el runner o estación local:
    ```bash
    docker compose -f docker-compose.preprod.yml logs --no-color --timestamps > ./docker-preprod-full.log
    ```
  - **Almacenamiento Efímero Opcional:** Si la JVM o la aplicación requirieran escribir archivos temporales, se utiliza un punto de montaje en memoria volátil `tmpfs` en `/tmp`.

* **Alternativa 1.B — Bind-Mounts Persistentes con Scripts Sanitizadores de Permisos:**
  - Mantener `./logs:/app/logs` montado en el host pero estandarizar scripts auxiliares (`fix-permissions.sh`) que ejecuten `sudo chown` o `chmod -R 777` antes y después de cada corrida.
  - *Descarte:* Enmascara la causa raíz (ejecución como root), mantiene el riesgo de seguridad y falla en entornos o runners corporativos donde `sudo` está restringido o auditado.

* **Alternativa 1.C — Volúmenes Nombrados de Docker con Contenedor Sidecar de Extracción:**
  - Montar un volumen nombrado de Docker (ej. `preprod_logs`) compartido entre los servicios y un contenedor auxiliar (sidecar) que copie los archivos al host al finalizar la suite.
  - *Descarte:* Introduce sobrecarga innecesaria de orquestación, requiere sincronizar ciclos de vida de contenedores adicionales y no aprovecha el stream nativo de logs de Docker.

## Resultado de la Decisión

Alternativa elegida: **Alternativa 1.A — Aislamiento Completo sin Volúmenes de Host**

### Justificación:
Alinea la arquitectura del sistema con las mejores prácticas de la industria (12-Factor App). El proceso Java deja de ser responsable del almacenamiento de sus propios logs en disco, delegando la captura al runtime de contenedores.  
La ejecución bajo UID 1001 garantiza que, incluso si una vulnerabilidad remota fuera explotada en una dependencia de terceros, el atacante carecerá de permisos administrativos sobre el contenedor y sobre el sistema de archivos del host.

### Consecuencias Positivas:
* **Erradicación de `sudo chown`:** No se requieren privilegios administrativos en los runners de GitHub Actions ni en estaciones locales.
* **Workspace Inmaculado:** El directorio de trabajo queda libre de bloqueos de archivos o directorios corruptos tras la finalización del workflow.
* **Mitigación de Container Breakout:** Los microservicios operan con privilegios mínimos y sin acceso de escritura a recursos sensibles.
* **Compatibilidad Universal:** Mismo comportamiento en Ubuntu, Debian, Windows Docker Desktop y macOS Colima/Rancher.

### Consecuencias Negativas y Mitigaciones:
* **Riesgo de pérdida de logs ante OOMKilled:** Si el contenedor es terminado abruptamente por falta de memoria antes de vaciar sus buffers a consola, podrían perderse las últimas líneas.
  - *Mitigación:* Se configuran flags JVM de terminación controlada (`-XX:+ExitOnOutOfMemoryError`, `-XX:+UseContainerSupport`) y buffers de vaciado inmediato en Logback para eventos de nivel `WARN` y `ERROR`.

## Referencias y Trabajo Futuro

* [`docs/auditoria/revision-critica-devops-ci.md`](../auditoria/revision-critica-devops-ci.md): Eje 1 de la revisión crítica DevOps.
* Principio *12-Factor App XI: Treat logs as event streams*.
