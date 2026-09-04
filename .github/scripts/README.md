# Scripts Auxiliares y Automatizaciones — `.github/scripts/`

Este directorio contiene los scripts y herramientas de soporte utilizados por las GitHub Actions de DonaTrack y por el entorno local de desarrollo.

---

## 1. Módulos y Scripts Activos

### A. Framework de Notificaciones (`shared_notifier.js`)
Módulo centralizado y desacoplado bajo principios **SOLID** para despacho de mensajes a canales externos (Discord):
* **Single Responsibility (SRP):** El transporte de red (`DiscordNotifierChannel`), la resolución de identidades de usuario (`UserResolver`) y las reglas de negocio de formato de cada mensaje (`NotificationFormatter`) residen en clases independientes y cohesivas.
* **Open/Closed (OCP):** Nuevos formatos de notificación heredan de `NotificationFormatter` (ej. `PRInactivityNotificationFormatter`).
* **Dependency Inversion (DIP):** Los scripts dependen de abstracciones, permitiendo testing desacoplado y sustitución de canales.

### B. Recordatorios Diarios de Inactividad (`send_reminders.js`)
* **Workflow asociado:** [`.github/workflows/pr-reminders.yml`](../workflows/pr-reminders.yml)
* **Función:** Se ejecuta de lunes a viernes (cron 15:00 UTC / 12:00 PM Argentina) e inspecciona PRs abiertas con más de 48 horas sin actualización ni aprobaciones formales, despachando recordatorios a Discord mediante `shared_notifier.js`.

### C. Generación del Catálogo de Entregas (`generate-pdf-tree.js`)
* **Workflow asociado:** [`.github/workflows/deploy-pages.yml`](../workflows/deploy-pages.yml)
* **Función:** Escanea el árbol de consignas académicas en `docs/entregas/` y genera un índice JSON estructurado consumido por el visor interactivo de PDFs en GitHub Pages.

### D. Hooks Locales de Git
* **`setup-hooks.ps1`:** Script de PowerShell para instalar automáticamente los Git Hooks del proyecto en `.git/hooks/`.
* **`pre-commit.sh`:** Hook de pre-commit que valida `mvn spotless:check` y los tests unitarios únicamente sobre los archivos en stage antes de permitir el commit.
* **`test-hub-local.ps1`:** Utilidad local de testing para levantar y verificar el Hub de Documentación antes de su publicación.

---

## 2. Historial de Simplificación y Desmantelamiento (Oleada 2026)

Los flujos experimentales de autoasignación por Round-Robin (`assign_reviewer.js`, `auto_assign_issues.js`, `reviewer_groups.json`), triage de issues (`triage_issue.js`) y generación de sub-issues en cascada (`setup_cascade.js`, `standard_tasks.json`, `sub_issue_body_template.md`) fueron desmantelados y retirados del repositorio para:
1. Eliminar complejidad accidental y dependencias innecesarias de permisos de escritura (`issues: write`, `pull-requests: write`).
2. Evitar saturación de límites de tasa (Rate Limiting) en la API de GitHub.
3. Centrar el esfuerzo de desarrollo y revisión en el flujo colaborativo estándar y en los Quality Gates del pipeline principal (`main.yml`).
