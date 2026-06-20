# Cambios Realizados: Etapa 6 (Pruebas de Estrés y Performance QA)

En esta etapa se incorporó una nueva suite de pruebas automatizadas de stress y performance secuencial para validar los tiempos de respuesta del sistema bajo cargas volumétricas.

## 1. Modificaciones en el Código

### Pruebas de Estrés y Performance (`integration-tests`)
* **Archivo Nuevo**: [PerformanceStressIT.java](file:///c:/IdeaProjects/DonaTrack-TP-DDS/integration-tests/src/test/java/grupo5/tests/performance/PerformanceStressIT.java)
* **Escenarios**:
  1. `testDonorCreationPerformance`: Registra secuencialmente 100 nuevos donantes completos (Persona + Donante). Mide el impacto total que tiene esta llamada que involucra flujos de sincronía entre `donaciones-service`, `incentivos-service` y `notificaciones-service`.
  2. `testDonationEventProcessingStress`: Ingiere secuencialmente 200 eventos de donación directamente en `incentivos-service` para medir el comportamiento de la capa de lógica del motor de incentivos.

---

## 2. Resultados de la Ejecución

Las pruebas se corrieron con éxito sobre el entorno de preproducción con los siguientes resultados detallados:

### Reporte de QA: Creación de Donante (Persona + Donante)
* **Peticiones Totales**: 100
* **Peticiones Exitosas**: 100 (100% de éxito)
* **Peticiones Fallidas**: 0
* **Duración Total**: 15475 ms
* **Latencia Mínima**: 93 ms
* **Latencia Máxima**: 3025 ms
* **Latencia Promedio**: 154.61 ms
* **Percentil 95 (P95)**: 182 ms
* **Rendimiento (Throughput)**: 6.46 req/sec

### Reporte de QA: Ingestión de Eventos de Donación (Stress)
* **Peticiones Totales**: 200
* **Peticiones Exitosas**: 200 (100% de éxito)
* **Peticiones Fallidas**: 0
* **Duración Total**: 4063 ms
* **Latencia Mínima**: 17 ms
* **Latencia Máxima**: 47 ms
* **Latencia Promedio**: 20.31 ms
* **Percentil 95 (P95)**: 24 ms
* **Rendimiento (Throughput)**: 49.22 req/sec

---

## 3. Conclusión de Performance
El sistema responde de manera estable y predecible bajo llamadas secuenciales rápidas:
* Registrar una Persona y su Donante toma en promedio **154 ms** (con llamadas HTTP internas sincrónicas y notificaciones).
* El motor de incentivos procesa cada evento de donación en tan solo **20 ms**.
* No se registraron fallos ni timeouts en ninguna de las 300 peticiones realizadas.
