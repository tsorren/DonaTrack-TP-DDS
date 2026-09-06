# Protocolo Semántico de Salida y Quality Gate Estricto

- Status: proposed
- Date: 2026-09-03
- Deciders: Decisión Grupal
- Tags: ci-cd, testing, quality-gate, fail-fast, github-actions, python

## Contexto y Problema

En la tubería de integración continua de DonaTrack (`.github/workflows/main.yml`), el análisis de resultados y diagnóstico de preproducción presentaba un antipatrón crítico denominado el **"Síndrome del Verde Silencioso" (Silent Failure Masking)**:

1. **Operador `|| true` en GitHub Actions:** En pasos como `Generar Reporte Detallado de Fallos y Diagnóstico`, los scripts `analyze_preprod_logs.py` y `report_test_failures.py` se invocaban con `|| true`, garantizando que cualquier error de ejecución retornara `0` al runner.
2. **Retorno Incondicional de Éxito en Scripts:** La función principal de `report_test_failures.py` finalizaba invariablemente con `return 0`, incluso cuando el parseo de reportes XML identificaba múltiples tests fallidos (`failures > 0` o `errors > 0`).
3. **Ausencia de Política de Umbral Mínimo:** Si por una mala configuración de filtros en Maven no se ejecutaba ningún test (`total_tests == 0`), el pipeline continuaba y el Quality Gate final consideraba la validación como exitosa (`SUCCESS`), aprobando pull requests que carecían de verificación empírica.
4. **Indiferenciación de Causas de Falla:** Cuando un pipeline fallaba, era imposible distinguir de inmediato si la falla correspondía a un defecto de lógica de negocio o a un problema del entorno (timeout de base de datos, error de sintaxis en un script o dependencia faltante).

Se requiere formalizar un protocolo de salida y políticas de parada estricta que aseguren el principio de **Fallo Temprano (Fail-Fast)** y **cero falsos positivos en el Quality Gate**.

## Atributos de Calidad y Drivers de Decisión

* **Confiabilidad del Quality Gate (Soundness):** Ningún pull request debe obtener estado verde si contiene tests fallidos, violaciones de invariantes o ausencia total de ejecución de pruebas.
* **Determinismo y Fail-Fast:** Todo error debe detener la ejecución de forma inmediata e inequívoca, sin enmascaramientos artificiales.
* **Inteligibilidad y Diagnóstico Temprano:** La tubería debe informar con precisión la naturaleza del fallo mediante códigos de retorno estandarizados.

## Alternativas Consideradas

* **Alternativa 2.B — Protocolo Semántico de Salida y Regla `fail-if-no-tests` (Elegida):**
  - **Erradicación de `|| true`:** Se retira todo operador de silenciamiento en los workflows de GitHub Actions.
  - **Gramática de Códigos de Salida Semánticos:**
    * **`Exit Code 0` (SUCCESS):** Ejecución limpia, suite superada (`total_tests > 0`, `failures == 0`, `errors == 0`) y sin anomalías de severidad crítica en los registros de telemetría.
    * **`Exit Code 1` (FUNCTIONAL_FAILURE):** Fallo funcional comprobado (aserciones de tests fallidas, errores en ejecución de pruebas o detección de anomalías arquitectónicas severas como doble despacho de eventos). El Quality Gate aborta inmediatamente la integración.
    * **`Exit Code 2+` (INFRASTRUCTURE_CRASH):** Fallo del arnés de infraestructura (error de sintaxis en Python, excepción de I/O, timeouts de conexión a base de datos o directorio de reportes no encontrado). El step falla notificando una rotura del tooling.
  - **Política `fail-if-no-tests`:** Si el job de preproducción se ejecutó pero la suite reporta `total_tests == 0`, el script emite un error `::error::No se ejecutaron pruebas de integración` y retorna `Exit Code 1`.

* **Alternativa 2.A — Silenciamiento Permisivo con Advertencias Informativas:**
  - Mantener `|| true` para evitar que caídas de scripts detengan el pipeline, pero emitir anotaciones de advertencia (`::warning::`) en GitHub Actions.
  - *Descarte:* Perpetúa el riesgo de mezclar ramas rotas en la rama principal y traslada la responsabilidad de detección a la revisión humana visual.

* **Alternativa 2.C — Fallo Binario No Discriminado:**
  - Configurar que cualquier excepción no capturada termine con código `1` sin clasificar si es funcional o de infraestructura.
  - *Descarte:* Complica el triaje y la asignación de responsables ante caídas del pipeline (¿es un bug en el código Java o una caída del daemon de Docker?).

## Resultado de la Decisión

Alternativa elegida: **Alternativa 2.B — Protocolo Semántico de Salida y Regla `fail-if-no-tests`**

### Justificación:
Garantiza que el Quality Gate del pipeline sea completamente confiable. Los desarrolladores y revisores obtienen una señal honesta del estado del software.  
La distinción entre código 1 y 2 permite a los ingenieros de DevOps y a los desarrolladores de backend actuar de manera focalizada sin perder tiempo diagnosticando falsas alarmas de infraestructura como bugs de dominio, o viceversa.

### Consecuencias Positivas:
* **Erradicación Total de Falsos Positivos:** Imposible que un PR con tests rotos o nulos obtenga el check verde de CI.
* **Triaje Inmediato:** El código de retorno del paso indica directamente el ámbito de la anomalía.
* **Auditoría Transparente:** Cumplimiento de las políticas de Integridad de Quality Gates estipuladas en `AGENTS.md §4.3`.

### Consecuencias Negativas y Mitigaciones:
* **Mayor Rigidez del Pipeline:** Micro-cortes o demoras transitorias en el arranque de servicios pueden fallar el build con código 2.
  - *Mitigación:* Se implementan mecanismos robustos de espera con healthchecks (`wait-for-it` o sondeo de endpoints) previos a la invocación de los scripts de análisis.

## Referencias y Trabajo Futuro

* [`docs/auditoria/revision-critica-devops-ci.md`](../auditoria/revision-critica-devops-ci.md): Eje 2 de la revisión crítica DevOps.
* [`AGENTS.md §4.3`](../../AGENTS.md): Invariante de integridad de quality gates y pruebas.
