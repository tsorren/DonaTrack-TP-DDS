# Pipeline Unificado de CI/CD y Automatización de Calidad
- Status: proposed
- Date: 2026-04-26
- Deciders: Decisión Grupal
- Tags: cicd, devops, calidad-software, gestion-proyecto

## Contexto y Problema
El proyecto DonaTrack está diseñado bajo una arquitectura de microservicios multi-módulo basada en Maven. Se requiere implementar una infraestructura de Integración y Despliegue Continuo (CI/CD) que garantice la integridad del código, la calidad del diseño arquitectónico, el determinismo del formato y el cumplimiento estricto del Git Flow académico de la UTN. Asimismo, se necesita automatizar el despliegue continuo de la documentación interactiva de decisiones de arquitectura (ADRs), estandarizar la creación de requerimientos mediante plantillas, estructurar el seguimiento del proyecto en iteraciones y aplicar políticas estrictas de revisión entre pares para la aprobación de Pull Requests.

## Atributos de Calidad y Drivers de Decisión
* Testeabilidad
* Mantenibilidad
* Escalabilidad

## Alternativas Consideradas
* Pipeline Unificado Basado en Fallo Temprano con GitHub Actions, Git Hooks y Log4brains

## Resultado de la Decisión

Alternativa elegida: "Pipeline Unificado Basado en Fallo Temprano con GitHub Actions, Git Hooks y Log4brains"

Justificación:
Esta alternativa centraliza las validaciones críticas tanto locales como remotas bajo el enfoque de "Fallo Temprano". Permite automatizar la auditoría de ramas del Git Flow universitario, delegar la verificación del formato a los desarrolladores localmente antes de confirmar cambios y desplegar continuamente la base de conocimientos interactiva (Log4brains) en GitHub Pages. Además, integra la gobernanza del proyecto mediante plantillas de requerimientos estructuradas, la agrupación por Milestones académicos, y la automatización de flujos de trabajo en GitHub Projects y Pull Requests sin sobrecargar las tareas manuales del equipo.

### Consecuencias Positivas
* Garantiza el cumplimiento de la jerarquía de ramas de la cátedra (`main`, `ENTREGA_N`, ramas de requerimiento con prefijos `EX_`) impidiendo integraciones erróneas desde Pull Requests.
* Promueve el determinismo en el formato del código mediante el uso local de `spotless:check` en Git Hooks de PowerShell, obligando al desarrollador a revisar y supervisar los cambios en el área de stage antes de firmar commits (usar `mvn spotless:apply`).
* Despliega de forma continua y automatizada la base de conocimientos interactiva en GitHub Pages a través de subcarpetas dedicadas (`/adr-generator` y `/adr-preview`).
* Estandariza el ciclo de vida de desarrollo mediante plantillas declarativas de Issues (`Issue Templates`), forzando la asignación previa de atributos de calidad, componentes afectados y una lista de tareas técnica para cada requerimiento.
* Automatiza el tablero ágil en GitHub Projects dividiendo el desarrollo en una iteración por cada entrega académica, organizando el flujo en los estados: Pendiente, En proceso, Esperando Revisión, Modificación Solicitada y Completado.
* Protege las ramas principales impidiendo el merge de cualquier Pull Request que no pase satisfactoriamente la totalidad de los checks automáticos del pipeline o que no cuente con la aprobación de, al menos, un revisor formal distinto al desarrollador que trabajó en la tarea.

### Consecuencias Negativas
* Aumenta la complejidad en la administración del repositorio por la necesidad de configurar y almacenar múltiples secretos críticos (`SONAR_TOKEN`, `SONAR_PROJECT_KEY`, `SONAR_ORG`, `SONAR_URL`, `GEMINI_API_KEY`).
* Introduce una dependencia externa respecto a la disponibilidad y latencia de las APIs de SonarCloud durante la ejecución de los Pull Requests.

## Análisis de Alternativas

### Pipeline Unificado Basado en Fallo Temprano con GitHub Actions, Git Hooks y Log4brains

Consiste en la orquestación unificada de GitHub Actions mediante un flujo remoto estructurado en etapas secuenciales condicionales (`validate-git-flow`, `static-analysis-and-design`, `build-test-and-sonar`), sincronizado con hooks locales automáticos, gobernanza de plantillas basadas en YAML para Issues y un pipeline independiente de publicación en GitHub Pages para la documentación de decisiones.

#### Pros
* Desacoplamiento y claridad en las responsabilidades del pipeline divididas en trabajos secuenciales (`needs`).
* Evita la introducción de código mal formateado o que difiera de la documentación del modelo de dominio antes de impactar las ramas de entrega.
* El uso de artefactos (`upload-artifact`) permite resguardar reportes de calidad y de cobertura generados por JaCoCo y JUnit para auditorías posteriores.
* Facilita el seguimiento del avance real del equipo gracias a la sincronización automatizada de estados en GitHub Projects según la actividad de las Issues (con excepción de "Esperando Revisión", el cual requiere intervención o cambios de estado explícitos en PRs).
* El uso de Milestones proporciona un mecanismo visual nativo para agrupar las issues asociadas a las entregas parciales del cronograma universitario.

#### Contras
* Aumenta la complejidad del proceso de colaboración y requiere una curva de aprendizaje inicial para que el equipo asimile el flujo de transiciones de estados del tablero y las restricciones de aprobación obligatoria en las PRs.
