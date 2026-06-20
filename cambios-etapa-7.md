# Cambios Realizados: Etapa 7 (Pruebas de Estrés con Donaciones y Medios de Contacto en Persona Base)

En esta etapa se implementaron mejoras en las pruebas de estrés/performance y en los fixtures base para cumplir con los nuevos requisitos del sistema.

## 1. Modificaciones en el Código y Fixtures

### Fixtures (`integration-tests`)
* **Archivo Modificado**: [crear-persona-humana.json](file:///c:/IdeaProjects/DonaTrack-TP-DDS/integration-tests/src/test/resources/fixtures/personas/crear-persona-humana.json)
  * Se añadió un medio de contacto tipo `CORREO` predeterminado apuntando a `juan.perez@example.com`. Esto satisface el requerimiento de que la persona base cuente con contacto.

### Clase Base de Tests (`integration-tests`)
* **Archivo Modificado**: [BaseIT.java](file:///c:/IdeaProjects/DonaTrack-TP-DDS/integration-tests/src/test/java/grupo5/tests/BaseIT.java)
  * Se implementó `esperarReplicacionPersona(String personaId)`. Este método sondea rápidamente el endpoint `/api/notificaciones/personas/{id}` de `notificaciones-service` con esperas de 10ms hasta que la persona es replicada.
  * Se invocó `esperarReplicacionPersona(personaId)` al inicio de `apiCrearDonante(String personaId)` para evitar fallos HTTP 500 por la latencia de la replicación asíncrona entre servicios en llamadas secuenciales consecutivas.

### Pruebas de Estrés (`integration-tests`)
* **Archivo Modificado**: [PerformanceStressIT.java](file:///c:/IdeaProjects/DonaTrack-TP-DDS/integration-tests/src/test/java/grupo5/tests/performance/PerformanceStressIT.java)
  * Se agregó la llamada a `apiCrearDonacion` dentro de la iteración de creación de donantes, evaluando el flujo completo de registro de Persona + Donante + Donación en cada iteración de la prueba.
  * Se cambiaron las capturas del bucle de `catch (Exception e)` a `catch (Throwable t)` para capturar y reportar adecuadamente aserciones de RestAssured (`AssertionError`), imprimiendo la traza del error en `System.err` para facilitar el diagnóstico.

---

## 2. Resultados de la Ejecución

Se reiniciaron los contenedores para asegurar una base de datos limpia y se corrió la suite de verificación completa:

* **Total de Pruebas**: 17 ejecutadas, todas exitosas (`BUILD SUCCESS`).
* **Prueba de Performance**:
  * **Creación Completa (Persona + Donante + Donación)**: 100 peticiones secuenciales exitosas, latencia promedio de **191.52 ms**, throughput de **5.22 req/sec**.
  * **Ingestión de Eventos de Donación**: 200 peticiones secuenciales exitosas, latencia promedio de **19.37 ms**, throughput de **51.63 req/sec**.
