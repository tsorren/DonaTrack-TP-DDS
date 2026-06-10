# Plan de Implementación: Asignación Automática y Recordatorios en DonaTrack

> [!NOTE]
> **CONTEXTO PARA MODELOS DE IA (OTROS CHATS):**
> * **Proyecto:** `DonaTrack` es un sistema de microservicios basado en Java 21 y Maven.
> * **Regla de Revisión (Crítica):** Cada Pull Request requiere la aprobación de **al menos un miembro del equipo que no haya contribuido** (que no sea el autor y que no tenga ningún commit en la PR).
> * **Estrategia de Ramas:** Las PRs hacia la rama `ENTREGA_N` deben originarse de ramas con prefijo `E{N}_` (ej. `E1_nueva-funcionalidad`).
> * **Decisión de Seguridad:** El mapeo de usuarios GitHub ↔ Discord no debe ser público en el repositorio. Se usará un GitHub Secret llamado `DISCORD_USER_MAP` que contiene un JSON confidencial.
> * **Decisión de Estado (Round-Robin):** Para no requerir bases de datos ni hacer commits automatizados de estado a una rama git, se usará una **API Query Dinámica** (stateless). El script consultará las últimas 50 PRs del historial del repositorio en GitHub para calcular las asignaciones de manera autocurativa.
> * **Objetivo de este Plan:** Desarrollar un script en GitHub Actions (`assign_reviewer.js` y `send_reminders.js`) en DonaTrack que asigne revisores válidos dinámicamente y recuerde reviews inactivas en Discord.

---

## 1. Diseño del Sistema de Seguridad y Mapeo

Para evitar exponer públicamente los IDs de Discord de los estudiantes:
1. Se configurará un secreto en el repositorio de GitHub llamado `DISCORD_USER_MAP`.
2. El contenido de este secreto será un objeto JSON que asocia nombres de usuario de GitHub con los IDs de Discord de 18 dígitos:
   ```json
   {
     "github_username_1": "123456789012345678",
     "github_username_2": "876543210987654321"
   }
   ```
3. Los scripts de GitHub Actions leerán esta variable de entorno de manera segura:
   ```yaml
   env:
     DISCORD_USER_MAP: ${{ secrets.DISCORD_USER_MAP }}
   ```

---

## 2. Asignación Automática (Round-Robin Dinámico)

El script se ejecutará a través de un Job en GitHub Actions en el repositorio de `DonaTrack`.

### Disparador de la Automatización
* Se activará cuando se abra una PR o se marque como lista para revisión (Ready for Review), omitiendo cualquier PR en estado borrador (Draft).

```yaml
# .github/workflows/auto-assign.yml en DonaTrack
name: Auto Assign Reviewer

on:
  pull_request:
    types: [opened, ready_for_review]

jobs:
  assign-reviewer:
    if: github.event.pull_request.draft == false
    runs-on: ubuntu-latest
    permissions:
      pull-requests: write
      repository-projects: read
    steps:
      - name: Checkout del código
        uses: actions/checkout@v4

      - name: Asignar Revisor
        uses: actions/github-script@v7
        env:
          DISCORD_WEBHOOK_URL: ${{ secrets.DISCORD_WEBHOOK_URL }}
          DISCORD_USER_MAP: ${{ secrets.DISCORD_USER_MAP }}
          RECENT_PR_LIMIT: '50'
          BRANCH_PATTERN: '^E(\\d+)_req-(\\d+)-task(\\d+)$'
        with:
          script: |
            const script = require('./.github/scripts/assign_reviewer.js');
            await script({github, context});
```

### Arquitectura y Algoritmo del Script (`assign_reviewer.js`)

Para asegurar la mantenibilidad y aplicar principios **SOLID**, el script se compone de componentes desacoplados:
1. **`GitHubRepository`**: Capa de abstracción para toda la comunicación con la API REST de GitHub (lista de PRs, comentarios, colaboradores, commits y asignación de revisores).
2. **`PullRequestValidator`**: Responsable exclusivo de validar las reglas de negocio de la PR (como la secuencialidad de las revisiones).
3. **`CollaboratorPool`**: Responsable de filtrar y determinar los colaboradores elegibles para revisión (excluyendo autor y committers).
4. **`RoundRobinSelector`**: Responsable de calcular las cargas de revisión previas en base a un round-robin stateless y seleccionar al revisor adecuado.
5. **`DiscordNotifier`**: Responsable de la traducción de usuarios y envío de notificaciones a través del webhook de Discord.
6. **`AssignReviewerOrchestrator`**: Orquestador que coordina todas las clases anteriores.

#### Flujo del Algoritmo:
1. **Validación de Secuencialidad de Reviews (Crítico):**
   * El orquestador invoca a `PullRequestValidator` para validar si la rama de la PR actual sigue el patrón de sub-issue en cascada: `E{N}_req-{id}-task{i}`.
   * Si es una PR de sub-issue y el índice de tarea `i > 1`:
     * Busca en la lista de Pull Requests del repositorio aquellas que pertenezcan al mismo requerimiento (`E{N}_req-{id}`) y correspondan a tareas previas (`task{k}` donde `k < i`).
     * Si encuentra alguna PR de una tarea previa `k` que esté en estado de borrador (`draft == true`), entonces:
       * Publica un comentario en la PR actual indicando: `⚠️ No se puede solicitar revisión para la Task {i} porque la Task {k} precedente aún se encuentra en estado Borrador (Draft). Las revisiones deben ser estrictamente secuenciales.`
       * Establece el estado del check (commit status) como fallido.
       * Detiene el flujo sin asignar revisor.
2. **Listar Miembros del Equipo:** El orquestador usa `CollaboratorPool` para obtener los colaboradores del repositorio (`GET /repos/{owner}/{repo}/collaborators`).
3. **Filtrar Exclusiones:**
   * Excluye al autor de la PR actual (`github.event.pull_request.user.login`).
   * Obtiene la lista de contribuyentes de la PR (haciendo un `GET /repos/{owner}/{repo}/pulls/{number}/commits`) y excluye a cualquiera que haya realizado commits en la rama.
4. **Analizar Historial Reciente (Últimas 50 PRs):**
   * Obtiene la lista de las últimas 50 PRs del repositorio (`GET /repos/{owner}/{repo}/pulls?state=all&per_page=50`).
   * Por cada colaborador restante elegible, cuenta cuántas veces ha aparecido como `requested_reviewer` en estas 50 PRs.
   * Identifica quién fue el último revisor asignado (buscando la PR más reciente con asignación).
5. **Selección del Revisor:**
   * Filtra y descarta al último revisor asignado para evitar repetición consecutiva (a menos que no queden más revisores elegibles).
   * Selecciona al colaborador elegible con la **menor cantidad** de asignaciones en el historial.
   * En caso de empate en cantidad de revisiones, se asigna al revisor que haya estado más tiempo inactivo (el que tiene la asignación más antigua en el historial).
6. **Asignación Física:**
   * Llama a `POST /repos/{owner}/{repo}/pulls/{number}/requested_reviewers` con el revisor seleccionado.
7. **Notificación Opcional a Discord:**
   * Envía un mensaje al canal notificando la asignación del revisor (utilizando el mapa para hacer la mención `@` de Discord si está disponible).

---

## 3. Recordatorios Inteligentes (Cron Job de 48 Horas)

Se ejecutará un workflow diario para pings persistentes a las PRs estancadas.

### Disparador de Tiempo
Se ejecuta todos los días de lunes a viernes a las 12:00 PM (hora local de Argentina / 15:00 UTC).

```yaml
# .github/workflows/pr-reminders.yml en DonaTrack
name: Daily PR Reminders

on:
  schedule:
    # 15:00 UTC equivale a las 12:00 PM UTC-3 (Argentina)
    - cron: '0 15 * * 1-5'

jobs:
  send-reminders:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout del código
        uses: actions/checkout@v4

      - name: Ejecutar Recordatorios
        uses: actions/github-script@v7
        env:
          DISCORD_WEBHOOK_URL: ${{ secrets.DISCORD_WEBHOOK_URL }}
          DISCORD_USER_MAP: ${{ secrets.DISCORD_USER_MAP }}
          INACTIVITY_LIMIT_HOURS: '48'
          PR_STATE: 'open'
        with:
          script: |
            const script = require('./.github/scripts/send_reminders.js');
            await script({github, context});
```

### Arquitectura y Algoritmo del Script (`send_reminders.js`)

Para asegurar la mantenibilidad y aplicar principios **SOLID**, el script se compone de componentes desacoplados:
1. **`GitHubRepository`**: Capa de abstracción para la comunicación con la API REST de GitHub (lista de PRs abiertas y reseñas).
2. **`PRInactivityFilter`**: Responsable exclusivo de determinar si una PR califica para un recordatorio de inactividad (valida estado no-draft, inactividad mayor a 48 horas y falta de aprobaciones).
3. **`DiscordNotifier`**: Responsable de construir y enviar las notificaciones/menciones al canal de Discord usando la API de webhooks.
4. **`PRRemindersOrchestrator`**: Orquestador que coordina todas las clases anteriores.

#### Flujo del Algoritmo:
1. **Obtener PRs abiertas:** El orquestador usa `GitHubRepository` para listar las PRs abiertas (`GET /repos/{owner}/{repo}/pulls?state=open`).
2. **Filtrar por inactividad y aprobaciones:** Para cada PR, se consulta a `PRInactivityFilter`:
   * Descarta aquellas en estado borrador (`draft == true`).
   * Compara la fecha actual con la fecha de la última actualización de la PR (`updated_at`).
   * Si la diferencia es mayor a 48 horas, realiza una petición para validar si la PR tiene al menos una reseña aprobada (`APPROVED`).
3. **Enviar Notificaciones:** Si la PR califica para un recordatorio, se delega a `DiscordNotifier`:
   * Identifica los revisores asignados en `requested_reviewers`.
   * Traduce los nombres de usuario de GitHub a IDs de Discord mediante el secret `DISCORD_USER_MAP`.
   * Si tiene revisores asignados, envía una mención:
     `⚠️ <@discord_id>, la PR #[número] está esperando tu revisión hace más de 48 horas.`
   * Si no tiene revisores asignados, envía una alerta al canal general para que se actúe.
