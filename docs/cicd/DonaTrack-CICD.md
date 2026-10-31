# Documentación de CI/CD — DonaTrack

## Resumen General

Este documento detalla la infraestructura de Integración y Despliegue Continuo (CI/CD) y las automatizaciones del flujo de trabajo implementadas para el proyecto **DonaTrack**. El sistema está diseñado bajo una arquitectura de microservicios multi-módulo empleando **Maven**, garantizando la integridad del código, la calidad del diseño académico y la colaboración eficiente en el equipo.

El sistema CI/CD se compone de **dos pipelines centrales y flujos de soporte**:

| Pipeline / Flujo | Archivo | Trigger | Responsabilidad |
|---|---|---|---|
| **PR Pipeline** | [`.github/workflows/main.yml`](../../.github/workflows/main.yml) | `pull_request` | Validar, compilar, testear y generar artefactos efímeros |
| **Merge Pipeline** | [`.github/workflows/merge.yml`](../../.github/workflows/merge.yml) | `push` a `main`/`ENTREGA_*` y `workflow_dispatch` (corte de entrega) | Publicar el candidato `:sha-<7>`, validarlo en el preprod post-merge y promover su digest a los tags estables |
| **Deploy** | [`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) | Fin exitoso de `merge.yml` y `workflow_dispatch` | Ensayar el despliegue del manifiesto de versión en el runner, con smoke y rollback; Render opcional |
| **Recordatorios de Inactividad** | [`.github/workflows/pr-reminders.yml`](../../.github/workflows/pr-reminders.yml) | Cron diario (Lun-Vie 15:00 UTC) | Alertar en Discord sobre PRs inactivas > 48h |
| **Deploy Pages** | [`.github/workflows/deploy-pages.yml`](../../.github/workflows/deploy-pages.yml) | `push` a `main`/`ENTREGA_*` | Desplegar Hub, Documentador y ADRs en GitHub Pages |
| **Agent Governance** | [`.github/workflows/agent-governance.yml`](../../.github/workflows/agent-governance.yml) | `pull_request`, `push` | Validar propiedades mecánicas y canonicidad documental |

---

## 1. Pipeline de PR — `main.yml`

El archivo [`.github/workflows/main.yml`](../../.github/workflows/main.yml) centraliza las validaciones de integración en un pipeline unificado basado en la filosofía de **"Fallo Temprano" (Fail-fast)**, optimizado para compilaciones incrementales de microservicios.

### 1.1. Política de Ramas (Git Flow UTN)
* **Merges a `main`**: Solo permitidos desde ramas `ENTREGA_N` mediante Pull Request.
* **Merges a `ENTREGA_N`**: Solo permitidos desde ramas de requerimiento con prefijo `E{N}_` o `ENTREGA_{N}_` (ej: `E2_nueva-funcionalidad`). La lógica es genérica basada en el número de entrega extraído de `BASH_REMATCH`.

### 1.2. Arquitectura de Jobs del Pipeline de PR

```mermaid
graph TD
  validate-git-flow[1. validate-git-flow] --> detect-changes[2. detect-changes]
  detect-changes --> lint-and-format[3. lint-and-format]
  detect-changes --> build-and-test[4. build-and-test]
  detect-changes --> generate-diagrams[7. generate-diagrams]

  lint-and-format --> sonar-analysis[5. sonar-analysis]
  build-and-test --> sonar-analysis
  build-and-test --> docker-build[6. docker-build]
  build-and-test --> preprod-validation[9. preprod-validation]
  docker-build --> preprod-validation
  validate-git-flow --> contract-diff[9b. contract-diff]

  generate-diagrams --> semantic-uml-diff[8. semantic-uml-diff]

  validate-git-flow --> quality-gate[10. quality-gate]
  detect-changes --> quality-gate
  lint-and-format --> quality-gate
  build-and-test --> quality-gate
  sonar-analysis --> quality-gate
  docker-build --> quality-gate
  generate-diagrams --> quality-gate
  semantic-uml-diff --> quality-gate
  preprod-validation --> quality-gate
```

### 1.3. Detalle de las Etapas

1. **validate-git-flow**: Valida mediante bash regex que la rama origen de la PR respete los prefijos y nomenclatura correspondientes según la rama base destino (`main` o `ENTREGA_N`): hacia `main` solo entran ramas `ENTREGA_N`; hacia `ENTREGA_N`, ramas `E<N>_*` o `ENTREGA_<N>_*`, y también `E<N-1>_*` para reapuntar sin renombrar el trabajo que quedó abierto al cerrar la entrega anterior (deja un `::notice::`). Lógica genérica basada en el número de entrega sin hardcoding de ramas específicas.
2. **detect-changes**: Usa `dorny/paths-filter` para detectar cambios globales y `git diff --name-only origin/base...HEAD` (triple punto, comparación desde ancestro común) para detectar cambios por servicio. Genera dos matrices: `services-matrix` (módulos a compilar) y `docker-matrix` (módulos con Dockerfile). Exporta dos outputs booleanos: `any-service` y `any-docker-service`.
3. **lint-and-format**: Ejecuta `mvn spotless:check` validando el formato de código.
4. **build-and-test**: Compila y testea los microservicios afectados con matriz paralela. Sube JARs, reportes JaCoCo, Surefire y archivos PUML como artefactos de Actions.
5. **sonar-analysis**: Sincroniza cobertura y métricas a SonarCloud y espera su quality gate (`sonar.qualitygate.wait`). Si existe `SONAR_TOKEN` y la base es `main` o `ENTREGA_*`, el Quality Gate lo exige (RM-02); sin el secreto, sigue siendo opcional.
6. **docker-build**: Construye y publica en GHCR la imagen de cada servicio modificado con tag `pr-{N}` (efímero, para uso en preprod-validation). Utiliza caché GHA por scope de servicio.
7. **generate-diagrams** *(deshabilitado: `enable-diagrams: 'false'`, `main.yml:116`)*: Genera diagramas PlantUML (rama base y PR) y los renderiza a SVG. Usa PlantUML `v1.2024.7` con clave de caché fija para 100% hit rate.
8. **semantic-uml-diff** *(deshabilitado: `if: false`, `main.yml:551`)*: Compara estructuralmente los diagramas UML usando `tsorren/SemanticUMLDiff` y comenta las diferencias en el PR y Discord.
9. **preprod-validation**: Invoca `./run-preprod-tests.sh --images ghcr`, el mismo punto de entrada que se usa en local (RM-04). Un servicio modificado solo corre con su imagen `pr-{N}` y, si no la tiene, el job falla (RM-02). Los no modificados se fijan por digest desde el último manifiesto promovido de la rama base (RM-06); mientras no exista, usan `:entrega_N` con aviso. Corre smoke y después `contract,integration,e2e` con `PREPROD_STRICT=true`: un servicio caído o un test salteado de un grupo requerido hacen fallar el job. Las fallas de servicios que el PR no modificó se marcan como heredadas de la base. Los mensajes AMQP que no cumplen su schema son un aviso (D5).
9b. **contract-diff**: Compara las specs OpenAPI con la rama base usando `oasdiff` (`scripts/diff-contratos.sh`). En el PR un cambio incompatible es un aviso (`::warning::`), por eso el Quality Gate no lo exige; en el post-merge y en el corte falla (D5 de la SPEC-05).
10. **quality-gate**: Job centinela con lógica de **doble eje de contexto**: evalúa cada job según `any-service` o `any-docker-service`, evitando falsos positivos cuando jobs son correctamente omitidos por contexto (ej: `docker-build=skipped` cuando no hay Dockerfiles modificados es **válido**).

### 1.4. Estrategia de Caché
* **Maven**: caché nativa de `actions/setup-java` (por workspace).
* **Docker layers**: caché GHA con scope por nombre de servicio, compartida entre el pipeline de PR y el pipeline de merge para acelerar builds sucesivos.
* **PlantUML JAR**: clave fija `plantuml-jar-1.2024.7`. Para actualizar la versión, cambiar la clave **y** la URL de descarga en sincronía.

---

## 2. Pipeline de Merge — `merge.yml`

El archivo [`.github/workflows/merge.yml`](../../.github/workflows/merge.yml) se dispara automáticamente al integrar un PR en `main` o cualquier rama `ENTREGA_*`. Publica un **candidato** inmutable, lo valida en el preprod post-merge y **promueve el mismo digest** a los tags estables (RM-06, [ADR de promoción por digest](../adr/20261020-promocion-de-artefactos-por-digest-y-despliegue-reproducible-de-logistica.md)). Cada promoción deja un **manifiesto de versión** (commit → digest por servicio) como artefacto `manifiesto-version`, que consumen `main.yml` y `deploy.yml`.

**Corte de entrega (D5):** 24 h antes del vencimiento, el responsable de integración corre el post-merge a mano sobre la rama de la entrega: `gh workflow run merge.yml --ref ENTREGA_5`.

> **Prerrequisito operativo**: `preprod-validation` en el pipeline de PR depende de que `merge.yml` haya publicado previamente las imágenes base de la rama destino. Ante el primer PR de una rama nueva, publicar manualmente las imágenes iniciales o forzar el merge pipeline con `git commit --allow-empty`.

### 2.1. Arquitectura del Pipeline de Merge

```mermaid
graph TD
  detect-services[1. detect-services] --> build-and-verify[2. build-and-verify]
  build-and-verify --> docker-publish[3. docker-publish]
  docker-publish --> preprod-post-merge[4. preprod-post-merge]
  preprod-post-merge --> promote[5. promote]
  docker-publish --> cleanup-pr-tags[6. cleanup-pr-tags]
  detect-services --> merge-summary[7. merge-summary]
  build-and-verify --> merge-summary
  docker-publish --> merge-summary
  preprod-post-merge --> merge-summary
  promote --> merge-summary
  cleanup-pr-tags --> merge-summary
```

### 2.2. Detalle de las Etapas

1. **detect-services**: Descubrimiento dinámico de todos los servicios del monorepo. Calcula el `branch-tag` (ej: `entrega_2`) y el `sha-short` (7 chars) para usarlos como tags Docker.
2. **build-and-verify**: Compila y testea **todos** los servicios desde el estado mergeado (no desde artefactos de PR). Usa `mvn verify` para detectar regresiones introducidas por el merge. Si fallan los tests, las imágenes no se publican.
3. **docker-publish**: Construye cada imagen una sola vez y publica solo el candidato `:sha-a1b2c3d` (inmutable). Guarda el digest de cada servicio como artefacto.
4. **preprod-post-merge**: Arma el manifiesto candidato con esos digests y corre `./run-preprod-tests.sh --images ghcr` con todas las imágenes fijadas por digest, en modo estricto (`PREPROD_STRICT` y `CONTRATOS_ESTRICTO`). También corre el diff de OpenAPI en modo error (contra la cabeza anterior de la rama o, en el corte, contra `main`).
5. **promote**: Si el post-merge dio verde, re-etiqueta el mismo digest con `docker buildx imagetools create`, sin reconstruir:
   - `:entrega_2` — tag estable de la rama
   - `:latest` — solo si el push es a `main`

   Verifica que cada tag promovido apunte al digest validado y publica el manifiesto de versión (90 días). Si el post-merge falla, no promueve: el tag de la rama sigue en la última versión validada.
6. **cleanup-pr-tags**: Elimina los tags `pr-N` efímeros de GHCR tras el merge para evitar acumulación de imágenes obsoletas. Requiere el secreto `PACKAGES_CLEANUP_PAT`.
7. **merge-summary**: Genera un resumen con el estado de cada job y falla si el candidato no llegó a promoverse.

### 2.3. Política de Concurrencia
A diferencia del pipeline de PR (`cancel-in-progress: true`), el pipeline de merge usa `cancel-in-progress: false`. Dos merges consecutivos se serializan para evitar publicar un estado intermedio o dejar GHCR con imágenes incompletas.

### 2.4. Filtro de Paths
El merge pipeline solo se dispara cuando hay cambios en código fuente (`*/src/**`), `pom.xml`, `Dockerfile`, `docker-compose*.yml` o en los contratos (`docs/arquitectura/contratos/**`): un PR de "contrato primero" (D5) pasa por el post-merge y por su diff de OpenAPI. El resto de los commits de documentación o ADRs no consume minutos de runner ni cuota de GHCR.

---

### 2.5. Despliegue reproducible — `deploy.yml`

[`.github/workflows/deploy.yml`](../../.github/workflows/deploy.yml) (RM-07, D3 de la SPEC-05) no depende de Render. Corre después de cada `merge.yml` exitoso y a mano (`gh workflow run deploy.yml --ref ENTREGA_5 -f corrida=<run-id>`). GitHub solo dispara esos eventos para workflows que existen en la rama por defecto: `deploy.yml` empieza a correr cuando llega a `main`. La corrida automática solo arranca tras un `merge.yml` exitoso por push en este repositorio, y usa los scripts y el compose del ref del workflow (`main`), nunca los del evento que la disparó (regla de Sonar `githubactions:S7631`). Solo acepta manifiestos de corridas del propio repositorio, nunca de un fork, y el resolvedor rechaza imágenes fuera de `ghcr.io/<repo>/<servicio>@sha256:`.

1. **ensayo**: baja el manifiesto M (el último promovido de la rama o el de la corrida indicada) y el anterior, M-1. `scripts/ensayo-despliegue.sh` levanta el stack de preprod con los digests de M, corre el smoke (`/actuator/health` en `UP`, `/v3/api-docs` y el grupo `smoke`), despliega M-1 encima y repite el smoke: el rollback queda ensayado en cada corrida.
2. **render** (opcional, solo a mano): si existe el secreto `RENDER_DEPLOY_HOOK_LOGISTICA`, llama al deploy hook con la imagen de Logística por digest; si no existe, avisa y no falla.

## 3. Recordatorios Inteligentes de Inactividad (`pr-reminders.yml`)

Para evitar cuellos de botella en la revisión de código, se mantiene activo un cron job diario que audita PRs estancadas.

* **Workflow**: [`.github/workflows/pr-reminders.yml`](../../.github/workflows/pr-reminders.yml)
* **Script Orquestador**: [`.github/scripts/send_reminders.js`](../../.github/scripts/send_reminders.js)
* **Framework Compartido**: [`.github/scripts/shared_notifier.js`](../../.github/scripts/shared_notifier.js)
* **Funcionamiento**: Se ejecuta de lunes a viernes a las 12:00 PM UTC-3 (15:00 UTC). Filtra PRs en estado abierto (no draft) con más de 48 horas sin cambios ni aprobaciones, despachando recordatorios a Discord mencionando a los revisores asignados mediante `DISCORD_USER_MAP`.

---

## 4. Despliegue de Documentación en GitHub Pages (`deploy-pages.yml`)

* **Workflow**: [`.github/workflows/deploy-pages.yml`](../../.github/workflows/deploy-pages.yml)
* **Script de Árbol de Entregas**: [`.github/scripts/generate-pdf-tree.js`](../../.github/scripts/generate-pdf-tree.js)
* **Funcionamiento**: En cada push a `main` o ramas `ENTREGA_*`, compila la previsualización interactiva de ADRs con Log4brains, empaqueta el visor Hub (`docs/herramientas/hub/`), el Documentador (`docs/herramientas/documentador/`) y genera el catálogo de PDFs de entregas publicando todo en GitHub Pages.

---

## 5. Validación Local (Git Hooks)

Facilita la verificación temprana antes de subir cambios al servidor remoto.

### 5.1. Instalación en Windows
Ejecutar el script de PowerShell: `./.github/scripts/setup-hooks.ps1`. Esto configurará los hooks en la carpeta interna `.git/hooks/`.

### 5.2. Comportamiento (`pre-commit`)
Al realizar `git commit`, el script intercepta la acción, detecta qué módulos Maven del reactor fueron modificados y ejecuta localmente `spotless:check` y los tests unitarios correspondientes únicamente sobre los archivos en stage, acelerando los tiempos de commit y asegurando la higiene del historial git.

---

## 6. Configuración de Secretos en GitHub (Secrets)

El correcto funcionamiento de todos los flujos de integración y notificaciones requiere definir las siguientes variables confidenciales en GitHub Secrets:

| Secreto / Variable | Propósito | Formato / Ejemplo |
|---|---|---|
| `GEMINI_API_KEY` | Sin uso: ningún workflow de `.github/workflows/` lo referencia | Llave de Google AI Studio |
| `SONAR_TOKEN` | Token de autenticación para sincronizar con SonarCloud | Generado en SonarCloud |
| `SONAR_PROJECT_KEY` | Identificador del proyecto en SonarCloud | String único del proyecto |
| `SONAR_ORG` | Organización de SonarCloud vinculada | String de la organización |
| `SONAR_URL` | URL de la plataforma SonarCloud | `https://sonarcloud.io` |
| `DISCORD_WEBHOOK_URL` | Webhook de Discord para recordatorios de inactividad | URL del webhook del canal de alertas/reminders |
| `DISCORD_SEMANTIC_DIFF_WEBHOOK_URL` | Webhook de Discord exclusivo para reportar los diffs arquitectónicos visuales generados por la comparación UML | URL del webhook del canal de arquitectura |
| `DISCORD_USER_MAP` | JSON confidencial que mapea usuarios de GitHub con sus IDs de Discord de 18 dígitos | `{"github_user": "123456789012345678"}` |
| `RENDER_DEPLOY_HOOK_LOGISTICA` | Opcional. Deploy hook de Render para Logística; lo usa `deploy.yml` en la corrida manual. Sin él, el paso avisa y no falla. Se lee como secreto del repositorio; si se guarda en un Environment con revisores, hay que sumar `environment:` al job `render` | URL del deploy hook del panel de Render |
| `PACKAGES_CLEANUP_PAT` | PAT Classic con scope `delete:packages` para limpiar tags efímeros `pr-N` de GHCR tras merge. Sin este secreto, la limpieza falla silenciosamente (el pipeline no se bloquea). | PAT generado en GitHub → Settings → Developer settings |

---

## 7. Agent Governance Check (Wave 7A)

* **Workflow**: [`.github/workflows/agent-governance.yml`](../../.github/workflows/agent-governance.yml)
* **Trigger**: Todo `pull_request` y `push` a `main` o `ENTREGA_*`. Sin filtros de paths — el check es lo suficientemente barato.
* **Responsabilidad**: Verifica propiedades mecánicas del harness de agentes. Wave 7A cubre: canonicidad de `AGENTS.md`, existencia y referencia activa de `docs/IA/review/evaluator.md`, y ausencia de términos obsoletos eliminados en Oleada 6.
* **Stack**: Node.js 20, módulos built-in únicamente. Sin Docker, Maven ni dependencias externas.
* **Tests del tooling**: `node scripts/tests/run-tests.js` (86 aserciones de fixtures temporales).
* **Ejecución local**: `node scripts/agent-check.js`
* **Bloquea merge**: Sí, si cualquier check emite severidad `FAIL`. Los `WARN` son informativos.
* **Secretos requeridos**: Ninguno.

---

## 8. Desmantelamiento de Flujos Experimentales (2026)

Con el fin de consolidar la arquitectura CI/CD y eliminar complejidad accidental en la gobernanza del repositorio, se procedió al retiro formal de los siguientes componentes:
1. **Flujo de Tareas en Cascada (Stacked PRs)** (`cascading-setup.yml`, `setup_cascade.js`, `standard_tasks.json`, `sub_issue_body_template.md`, `02-req-con-subissues.yaml`): Eliminado debido a la sobrecarga de sincronización de ramas apiladas y la fricción generada por Draft PRs encadenadas.
2. **Asignación Automática Round-Robin** (`auto-assign.yml`, `assign_reviewer.js`, `notify_reviewer.js`, `reviewer_groups.json`): Eliminado para devolver la autonomía a los desarrolladores en la elección y solicitud manual de revisores de acuerdo al contexto técnico.
3. **Triage Automático y Autoasignación Cron de Issues** (`issue-triage.yml`, `issue-auto-assign-cron.yml`, `triage_issue.js`, `auto_assign_issues.js`): Decomisionado para simplificar la gestión de GitHub Projects sin requerir scripts periódicos con permisos elevados de escritura.
