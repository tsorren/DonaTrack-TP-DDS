# Plan de Implementación: Recordatorios de Inactividad de PRs en DonaTrack

> [!NOTE]
> **CONTEXTO PARA MODELOS DE IA Y DESARROLLADORES:**
> * **Proyecto:** `DonaTrack` es un sistema de microservicios basado en Java 21 y Maven.
> * **Regla de Revisión (Crítica):** Cada Pull Request requiere la aprobación de **al menos un miembro del equipo que no haya contribuido** (que no sea el autor y que no tenga ningún commit en la PR).
> * **Estrategia de Ramas:** Las PRs hacia la rama `ENTREGA_N` deben originarse de ramas con prefijo `E{N}_` (ej. `E1_nueva-funcionalidad`).
> * **Decisión de Seguridad:** El mapeo de usuarios GitHub ↔ Discord no debe ser público en el repositorio. Se usa un GitHub Secret llamado `DISCORD_USER_MAP` que contiene un JSON confidencial.
> * **Objetivo de este Plan:** Mantener documentada la arquitectura del workflow de recordatorios diarios en GitHub Actions (`pr-reminders.yml` y `send_reminders.js`) en DonaTrack para alertar sobre PRs inactivas en Discord.

---

## 1. Diseño del Sistema de Seguridad y Mapeo

Para evitar exponer públicamente los IDs de Discord de los estudiantes:
1. Se encuentra configurado un secreto en el repositorio de GitHub llamado `DISCORD_USER_MAP`.
2. El contenido de este secreto es un objeto JSON que asocia nombres de usuario de GitHub con los IDs de Discord de 18 dígitos:
   ```json
   {
     "github_username_1": "123456789012345678",
     "github_username_2": "876543210987654321"
   }
   ```
3. Los scripts de GitHub Actions leen esta variable de entorno de manera segura:
   ```yaml
   env:
     DISCORD_USER_MAP: ${{ secrets.DISCORD_USER_MAP }}
   ```

---

## 2. Recordatorios Inteligentes (Cron Job de 48 Horas)

Se ejecuta un workflow diario para alertar sobre PRs abiertas que presenten inactividad prolongada.

### Disparador de Tiempo
Se ejecuta de lunes a viernes a las 12:00 PM (hora local de Argentina / 15:00 UTC).

```yaml
# .github/workflows/pr-reminders.yml en DonaTrack
name: Daily PR Reminders

on:
  schedule:
    # 15:00 UTC equivale a las 12:00 PM UTC-3 (Argentina)
    - cron: '0 15 * * 1-5'
  workflow_dispatch: {}

jobs:
  send-reminders:
    runs-on: ubuntu-latest
    permissions:
      pull-requests: read
      contents: read
    steps:
      - name: Checkout del código
        uses: actions/checkout@v4

      - name: Configurar Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'

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
3. **`DiscordNotifierChannel`**: Responsable del transporte HTTP hacia el webhook de Discord (definido en `shared_notifier.js`).
4. **`UserResolver`**: Encargado de resolver menciones `@<id>` de Discord a partir de nombres de GitHub.
5. **`PRInactivityNotificationFormatter`**: Formateador de mensajes desacoplado para alertas de inactividad.
6. **`PRRemindersOrchestrator`**: Orquestador que coordina todas las clases anteriores.

#### Flujo del Algoritmo:
1. **Obtener PRs abiertas:** El orquestador usa `GitHubRepository` para listar las PRs abiertas (`GET /repos/{owner}/{repo}/pulls?state=open`).
2. **Filtrar por inactividad y aprobaciones:** Para cada PR, se consulta a `PRInactivityFilter`:
   * Descarta aquellas en estado borrador (`draft == true`).
   * Compara la fecha actual con la fecha de la última actualización de la PR (`updated_at`).
   * Si la diferencia es mayor a 48 horas, realiza una petición para validar si la PR tiene al menos una reseña aprobada (`APPROVED`).
3. **Enviar Notificaciones:** Si la PR califica para un recordatorio:
   * Identifica los revisores asignados en `requested_reviewers`.
   * Traduce los nombres de usuario de GitHub a IDs de Discord mediante `UserResolver`.
   * Si tiene revisores asignados, envía una mención personalizada:
     `⚠️ <@discord_id>, la PR #[número] está esperando tu revisión hace más de 48 horas.`
   * Si no tiene revisores asignados, envía una alerta al canal general para que el equipo tome acción.
