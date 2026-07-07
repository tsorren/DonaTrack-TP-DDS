# Rediseño del Sistema de Asignación y Notificaciones de GitHub (Issue #697)

Este documento detalla la reestructuración técnica de los flujos de integración y automatizaciones del repositorio de **DonaTrack**, migrando de un esquema de asignación plano a un sistema jerárquico por niveles de prioridad (`ALTA`, `MEDIA`, `BAJA`) y aplicando principios **SOLID** para garantizar la extensibilidad futura del sistema de notificaciones.

---

## 1. Decisiones de Diseño y Arquitectura (SOLID)

### A. Corrección del Bug de Round-Robin en PRs
*   **Problema original:** El selector realizaba conteos de asignación sobre `pr.requested_reviewers` en las últimas 50 PRs. Sin embargo, en GitHub este campo solo contiene solicitudes *pendientes*. En cuanto se aprueba/comenta la PR o esta se fusiona, el campo queda vacío. Esto causaba un empate constante en `0` y hacía que el algoritmo seleccionara siempre al primer integrante alfabéticamente (sobrecargándolo).
*   **Solución:** Se implementó `listReviews` en el repositorio de GitHub. Ahora, el script recupera el historial de revisiones completadas reales en una ventana optimizada de las **últimas 15 PRs** (reduciendo el tamaño de 50 a 15 para evitar bloqueos por Rate Limit de la API de GitHub).

### B. Desacoplamiento de las Notificaciones de Discord
*   **Problema original:** La notificación a Discord estaba acoplada dentro del flujo de asignación automática. Si el equipo asignaba manualmente a un revisor en GitHub, la acción no asignaba y el revisor nunca era notificado en Discord.
*   **Solución:** Se separaron físicamente las responsabilidades. 
    1.  [auto-assign.yml](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/workflows/auto-assign.yml) se encarga de ejecutar la asignación automática por Round-Robin (y finaliza inmediatamente en *skip* si la PR ya posee revisores asignados).
    2.  [notify-reviewer.yml](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/workflows/notify-reviewer.yml) reacciona a los eventos nativos de GitHub `ready_for_review` y `review_requested`, disparando el script de comunicación de forma aislada. Esto garantiza que cualquier revisor asignado (manual, automático o re-solicitado) reciba su alerta en Discord.

### C. Framework Extensible de Notificaciones (`shared_notifier.js`)
Para cumplir con los principios SOLID y dejar el sistema preparado para futuras alertas (issues creadas, issues inactivas y vencimientos cercanos), se diseñó un módulo centralizado:
*   **Single Responsibility (SRP):** El transporte de red de Discord (`DiscordNotifierChannel`), la resolución de identidades de usuario (`UserResolver`) y las reglas de negocio de formato de cada mensaje (`NotificationFormatter`) están en clases independientes y cohesivas.
*   **Open/Closed (OCP):** Agregar un nuevo evento de notificación (por ejemplo, notificar una issue que está por vencerse) se resuelve **añadiendo una nueva clase** que hereda de `NotificationFormatter` (ej. `IssueDueSoonNotificationFormatter`) e inyectándola al canal, sin modificar una sola línea del código de transporte de Discord o de otros flujos.
*   **Dependency Inversion (DIP):** Los scripts dependen de contratos y clases abstractas, facilitando el testeo local con mocks.

---

## 2. Nueva Estructura de Archivos

*   **[reviewer_groups.json](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/scripts/reviewer_groups.json):** Archivo de datos centralizado con la distribución de los integrantes en sus niveles correspondientes (`ALTA`, `MEDIA`, `BAJA`).
*   **[shared_notifier.js](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/scripts/shared_notifier.js):** Núcleo del framework de notificaciones.
*   **[triage_issue.js](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/scripts/triage_issue.js) y [issue-triage.yml](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/workflows/issue-triage.yml):** Automatizan el etiquetado y actualización del campo "Priority" de GitHub Projects V2 a partir del prefijo `[ALTA]`, `[MEDIA]` o `[BAJA]` del título de la issue.
*   **[assign_reviewer.js](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/scripts/assign_reviewer.js) y [auto-assign.yml](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/workflows/auto-assign.yml):** Asignan revisores por Round-Robin corregido, admiten override manual y comentan advertencias en fallbacks.
*   **[notify_reviewer.js](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/scripts/notify_reviewer.js) y [notify-reviewer.yml](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/workflows/notify-reviewer.yml):** Despachan menciones en Discord a los revisores activos en la PR.
*   **[auto_assign_issues.js](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/scripts/auto_assign_issues.js) y [issue-auto-assign-cron.yml](file:///C:/IdeaProjects/DonaTrack-TP-DDS/.github/workflows/issue-auto-assign-cron.yml):** Cron Job planificado diariamente a las 9:00 AM hora de Argentina (incluyendo fines de semana) que autoasigna issues abiertas desatendidas y envía un aviso de omisión a Discord.

---

## 3. Matrices de Asignación y Enrutamiento

### A. Matriz de Reviews (Aprobación Jerárquica de PRs)
Para garantizar la calidad y evitar sobrecargas, la asignación de revisores sigue estas reglas:

| Prioridad de la PR | Pool de Revisores Habilitados | Integrantes del Pool (Seniors / Mids) |
| :--- | :--- | :--- |
| **BAJA** | `MEDIA` + `ALTA` | `tsorren`, `BerEsti`, `MiriMiranda`, `belennn24`, `ndelorte`, `sofiadeane` |
| **MEDIA** | `ALTA` | `tsorren`, `BerEsti`, `MiriMiranda` |
| **ALTA** | `ALTA` | `tsorren`, `BerEsti`, `MiriMiranda` |

*Los desarrolladores junior (pool `BAJA` puro: `martinzaj`, `Anushig04`, `suarezcamila`) quedan exentos de realizar reviews, garantizando que todo cambio de código sea auditado al menos por un perfil Mid.*

### B. Matriz de Autoasignación de Tareas (Cron de Issues)
Cuando el Cron Job detecta una issue abierta sin asignar, calcula el Round-Robin asignando al desarrollador correspondiente al nivel de la issue para repartir la carga operativa:

| Prioridad de la Issue | Pool de Asignados Habilitados | Nivel de Desarrollador |
| :--- | :--- | :--- |
| **BAJA** | `BAJA` | `martinzaj`, `Anushig04`, `suarezcamila` |
| **MEDIA** | `MEDIA` | `belennn24`, `ndelorte`, `sofiadeane` |
| **ALTA** | `ALTA` | `tsorren`, `BerEsti`, `MiriMiranda` |

---

## 4. Verificación Local (Test Suite)

Se introdujo el script de simulación [test_scripts.js](file:///C:/Users/rata/.gemini/antigravity/brain/bc3761f2-6141-4a18-a2b7-50eb9aecf698/scratch/test_scripts.js) (ubicado en el directorio de pruebas local / scratch) que simula el entorno de ejecución de GitHub Actions y valida que:
1. Las issues adquieran su label y prioridad de proyectos al crearse.
2. Las PRs `BAJA` se asignen a un Mid/Senior y las PRs `MEDIA`/`ALTA` obligatoriamente a un Senior.
3. Se omitan asignaciones si la PR ya posee revisores.
4. Las notificaciones a Discord se construyan y resuelvan correctamente.
5. El Cron diario balancee y asigne las issues a los desarrolladores de su nivel de prioridad emitiendo la alerta correspondiente en Discord.
