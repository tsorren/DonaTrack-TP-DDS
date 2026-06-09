# Plan de Implementación: Flujo de Desarrollo en Cascada (Stacked PRs)

> [!NOTE]
> **CONTEXTO PARA MODELOS DE IA (OTROS CHATS):**
> * **Proyecto:** `DonaTrack` es un repositorio de microservicios.
> * **El Desafío:** Las revisiones se demoran mucho porque los desarrolladores abren Pull Requests gigantes (ej. 80 cambios estructurales).
> * **La Solución:** Implementar **Stacked PRs (PRs en Cascada)** y automatizar el desglose de tareas. Al crear una issue principal con etiqueta `requerimiento`, un bot de GitHub Actions debe parsear el alcance, crear sub-issues, crear ramas git consecutivas y abrir Draft PRs encadenadas automáticamente.
> * **Estructura de Ramas (Crítica):** Las ramas principales de requerimientos deben iniciar con `E{N}_` (ej. `E1_req-45`). Los sub-branches seguirán la nomenclatura `E{N}_req-{id}-task{i}`. Al fusionarse la PR de la primera tarea, GitHub redireccionará automáticamente la siguiente PR para que apunte a `ENTREGA_N`, eliminando fricción.
> * **Objetivo de este Plan:** Desarrollar el workflow `.github/workflows/cascading-setup.yml` y el script de Node/Python `setup_cascade.js` en DonaTrack para orquestar la creación de issues, branches y Draft PRs consecutivas.

---

## 1. Estructura de Sub-Issues Requeridas

Cada requerimiento (`[REQ]`) podrá desglosarse en hasta 8 sub-issues estándar de implementación y calidad. Si el alcance es menor, el creador del requerimiento puede simplemente eliminar o desmarcar del checklist las tareas que no correspondan antes de enviar el formulario.

### Lista de Sub-Issues Estándar:
1. **[Estructura]** Creación de la estructura de la implementación (paquetes, entidades, interfaces).
2. **[Dominio]** Implementación de la lógica de la capa de dominio.
3. **[Tests Unidad]** Desarrollo de pruebas unitarias correspondientes.
4. **[Persistencia]** Implementación de gestores, servicios y repositorios de datos.
5. **[Tests Orquestación]** Desarrollo de pruebas de integración y orquestación de servicios.
6. **[Controladores/Endpoints]** Implementación de controladores, endpoints y planificadores.
7. **[Tests UI/Endpoints]** Desarrollo de pruebas del controlador, endpoint y planificador.
8. **[Calidad y Diseño]** Documentación de decisiones (ADR) y verificación de consistencia con diagramas UML.

---

## 2. Automatización del Flujo al Crear la Issue Base

Crearemos un GitHub Action que se active cuando se abra una issue con la etiqueta `requerimiento`.

```yaml
# .github/workflows/cascading-setup.yml en DonaTrack
name: Auto Setup Cascading Flow

on:
  issues:
    types: [opened, labeled]

jobs:
  setup-cascade:
    if: contains(github.event.issue.labels.*.name, 'requerimiento')
    runs-on: ubuntu-latest
    permissions:
      issues: write
      contents: write
      pull-requests: write
    steps:
      - name: Checkout del código
        uses: actions/checkout@v4

      - name: Generar Estructura en Cascada
        uses: actions/github-script@v7
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
          SUB_ISSUE_TEMPLATE_PATH: '.github/scripts/sub_issue_body_template.md'
          STANDARD_TASKS_PATH: '.github/scripts/standard_tasks.json'
        with:
          script: |
            const script = require('./.github/scripts/setup_cascade.js');
            await script({github, context});
```

### Algoritmo y Arquitectura del Script (`setup_cascade.js`)

Para asegurar la mantenibilidad y aplicar principios **SOLID**, el script se compone de componentes desacoplados:
1. **`IssueBodyParser`**: Se encarga únicamente de parsear la Issue padre en GitHub para extraer el número de entrega/milestone y mapear la lista de asignaciones (`### Asignaciones Planificadas:`).
2. **`GitHubRepository`**: Capa de abstracción que maneja toda la comunicación con la API REST de GitHub (creación de issues, ramas, PRs, lecturas de SHAs y actualizaciones de issues).
3. **`IssueTemplateRenderer`**: Carga el archivo de plantilla `.github/scripts/sub_issue_body_template.md` y realiza el renderizado dinámico de variables (`{task_id}`, `{task_title}`, `{parent_id}`, `{task_name}`).
4. **`CascadeOrchestrator`**: Orquestador principal que coordina el flujo completo.

#### Flujo del Algoritmo:
1. **Parsear el Cuerpo de la Issue:**
   * El orquestador invoca a `IssueBodyParser` sobre `github.event.issue.body`.
   * Analiza la sección `### Sub-Issues Planificadas:` (que contiene las tareas de la plantilla).
   * Filtra las líneas que sigan el formato `- [x] Tarea` o `- [ ] Tarea`. Las que estén marcadas como deshabilitadas o eliminadas de la plantilla se descartan.
   * Analiza la sección `### Asignaciones Planificadas:` para extraer el usuario asignado a cada tarea (ej. `Task 1: @usuario1`, `Task 8: @usuario8`).
2. **Crear la Rama Base del Requerimiento:**
   * Extrae la entrega/milestone elegida (por ejemplo, `Entrega 1`).
   * Determina la rama base correspondiente (ej: `ENTREGA_1`).
   * Crea la rama principal del requerimiento: `E{N}_req-{id}` basada en `ENTREGA_{N}`.
3. **Generar las Sub-Issues en GitHub:**
   * Para cada tarea válida del checklist, genera el contenido usando `IssueTemplateRenderer` con la plantilla del archivo `.github/scripts/sub_issue_body_template.md`.
   * Crea una nueva Issue en GitHub:
     * Título: `[REQ-{parent_id}] [TASK-{i}] - [{task_name}] {Nombre_Tarea}`
     * Cuerpo: Renderizado dinámico de la plantilla.
     * Asignados: El usuario especificado en `### Asignaciones Planificadas:` para esa tarea. Si no está definido o no es válido, se deja sin asignar o se asigna al propietario de la issue padre.
   * Almacena los números de las sub-issues creadas para actualizarlas en el checklist de la issue padre.
4. **Crear Ramas Git para cada Sub-Issue (Excluyendo Tarea 8):**
   * Las ramas se crearán secuencialmente basándose en la anterior, **excluyendo la Tarea 8 (Verificación de diagramas/calidad)** que no genera rama:
     * Rama 1: `E{N}_req-{id}-task1` basada en `E{N}_req-{id}`
     * Rama 2: `E{N}_req-{id}-task2` basada en `E{N}_req-{id}-task1`
     * ...
     * Rama 7: `E{N}_req-{id}-task7` basada en `E{N}_req-{id}-task6`
5. **Crear Pull Requests en Estado Borrador (Draft PRs) Encadenadas (Excluyendo Tarea 8):**
   * Por cada rama de sub-issue creada (Tareas 1 a 7), crea una Pull Request en GitHub con `draft: true`:
     * **PR 1:** Cabeza `E{N}_req-{id}-task1` -> Base `E{N}_req-{id}`
     * **PR 2:** Cabeza `E{N}_req-{id}-task2` -> Base `E{N}_req-{id}-task1`
     * ...
     * **PR 7:** Cabeza `E{N}_req-{id}-task7` -> Base `E{N}_req-{id}-task6`
     * **PR Padre:** Cabeza `E{N}_req-{id}` -> Base `ENTREGA_N`
   * En la descripción de cada PR, el script añade automáticamente la referencia a la PR anterior y a la sub-issue de GitHub correspondiente para mantener la trazabilidad.

---

## 3. Estrategia de Asignación de Sub-Issues y Ciclo de Vida de Tareas Especiales

### Asignación mediante Lista en la Issue Base (Obligatoria)
* **Cómo funciona:** El creador del requerimiento define en la descripción de la Issue base los asignados en formato de lista bajo la sección `### Asignaciones Planificadas:`:
  ```markdown
  ### Asignaciones Planificadas:
  - Task 1: @juan
  - Task 2: @maria
  - Task 3: @pedro
  - Task 4: @juan
  - Task 5: @maria
  - Task 6: @pedro
  - Task 7: @juan
  - Task 8: @maria
  ```
  El script `setup_cascade.js` parsea esta lista mapeando `Task {i}` al usuario correspondiente para asignarle la sub-issue de GitHub en su creación.

### Ciclo de Vida de la Sub-Issue de Verificación de Diagramas (Tarea 8)
* **Exclusión de Rama y PR**: Al ser una tarea puramente de diseño y validación de consistencia (sin cambios directos de código que requieran su propia rama aislada en cascada), la **Tarea 8** no tiene rama git propia ni Pull Request.
* **Asignación**: Se le asigna al desarrollador correspondiente (especificado para la `Task 8` en la lista) de la misma forma que a las demás issues. Dado que no cuenta con PR, esta tarea no tiene asignación de PR Reviewer.
* **Cierre Manual**: Se marcará como completada (closed) de forma **manual** directamente en GitHub por el asignado una vez que se complete la verificación de consistencia entre la documentación de diseño y la implementación.

### Remoción de `compare_diagrams.py` del Pipeline de Actions
* **Auditoría de Diagramas**: Se ha removido el archivo `compare_diagrams.py` y, por ende, se elimina del workflow remoto `.github/workflows/main.yml` (job `static-analysis-and-design`) la ejecución de la auditoría automática basada en dicho script.

---

## 4. Plan de Verificación

* **Simulación Local:**
  * Crear un caso de prueba local que simule la carga útil (payload) del webhook de GitHub al crear un issue con la lista de tareas y asignaciones.
  * Verificar que el parser extraiga correctamente la lista de asignaciones y las tareas marcadas como activas, descartando las inactivas.
  * Verificar que se excluya la creación de rama y PR para la Tarea 8.
* **Prueba en Sandbox de GitHub:**
  * Configurar una issue de prueba en un repositorio alternativo de pruebas para validar que la creación de las issues asignadas, ramas (1-7) y Draft PRs (1-7) se realice correctamente en menos de 10 segundos, apuntando a los branches correctos, y que la Issue de la Tarea 8 se cree asignada pero sin rama ni PR.
