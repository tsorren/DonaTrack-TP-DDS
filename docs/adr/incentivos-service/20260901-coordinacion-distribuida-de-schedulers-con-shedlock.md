# Coordinación Distribuida de Schedulers con ShedLock

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: concurrencia, schedulers, shedlock, alta-disponibilidad, incentivos

## Contexto y Problema

En `incentivos-service`, existen tres tareas programadas críticas gobernadas por `@Scheduled`:
1. `InactividadJob`: Evalúa la falta de actividad de donantes para actualizar su estado.
2. `RachaJob`: Cierra los períodos mensuales de racha.
3. `RankingMensualJob`: Ejecuta el cómputo del ranking de fin de mes.
Al desplegar la aplicación en un entorno de alta disponibilidad con **múltiples réplicas (pods de Kubernetes o contenedores Docker Compose escalados)**, cada instancia ejecutará su propio temporizador local. Esto provoca que la misma tarea programada se ejecute concurrentemente en todas las instancias activas al mismo segundo, causando condiciones de carrera, duplicación de insignias otorgadas, bloqueos en base de datos y saturación innecesaria de recursos.

## Atributos de Calidad y Drivers de Decisión

* **Consistencia e Idempotencia:** Garantizar que cada ciclo programado se ejecute exactamente una sola vez en todo el clúster (*At-Most-Once Execution* por réplica).
* **Alta Disponibilidad y Tolerancia a Fallos:** Si el nodo que adquirió el lock cae inesperadamente, el bloqueo debe liberarse automáticamente para que otro nodo tome el control.
* **Simplicidad de Infraestructura:** No introducir servidores de coordinación pesados adicionales si no es estrictamente necesario.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 10 de Persistencia en `incentivos-service` ([decisiones_futuras_en_oleada_10.md](../../arquitectura/diseno/incentivos/decisiones_futuras_en_oleada_10.md) §12).
* **Hallazgo:** En la auditoría de concurrencia se evidenció que la anotación `@Scheduled` nativa de Spring no tiene noción de clúster. Se seleccionó la librería **ShedLock** coordinada sobre la tabla relacional de PostgreSQL.

## Alternativas Consideradas

* **Locks Distribuidos en PostgreSQL con ShedLock:** Incorporar `shedlock-spring` y `shedlock-provider-jdbc-template`. Crear la tabla liviana `shedlock(name VARCHAR(64) PRIMARY KEY, lock_until TIMESTAMP, locked_at TIMESTAMP, locked_by VARCHAR(255))`. Cada job se anota con `@SchedulerLock(name = "...", lockAtLeastFor = "PT30S", lockAtMostFor = "PT15M")`. Solo el nodo que logra el lock ejecuta la tarea; los demás abortan silenciosamente.
* **Coordinación Distribuida con Apache ZooKeeper o HashiCorp Consul:** Desplegar un clúster de coordinación independiente.
* **Scheduler Centralizado Externo (Quartz Clustered / Airflow / Temporal):** Desplegar una plataforma de orquestación de workflows pesada.
* **Instancia Única (Replica Count = 1):** Prohibir el escalado horizontal del servicio de incentivos.

## Resultado de la Decisión

Alternativa elegida: "Locks Distribuidos en PostgreSQL con ShedLock"

Justificación:
ShedLock es el estándar más ligero y robusto para Spring Boot. Reutiliza la base de datos PostgreSQL ya presente sin requerir componentes externos adicionales como Redis o ZooKeeper. La configuración de `lockAtMostFor` asegura que un bloqueo no quede retenido si el contenedor se apaga de forma abrupta, y `lockAtLeastFor` previene ejecuciones duplicadas en entornos con relojes ligeramente desincronizados.

### Consecuencias Positivas

* Ejecución segura en clúster: solo un pod ejecuta el job a la vez.
* Cero infraestructura nueva requerida (opera sobre una tabla SQL estándar).
* Transparente y declarativo mediante simples anotaciones sobre los métodos existentes.

### Consecuencias Negativas

* Requiere incluir la dependencia `shedlock-spring` en el `pom.xml` al migrar a persistencia física.

### Validación

Se valida mediante:
1. Presencia de la tabla `shedlock` en el script DDL de migraciones Flyway.
2. Pruebas de integración concurrentes con Testcontainers simulando dos beans ejecutando el mismo job simultáneamente y comprobando que solo uno adquiere el bloqueo.

## Análisis de Alternativas

### ShedLock con JDBC

#### Pros
* Simplicidad absoluta y confiabilidad demostrada.
* Cero mantenimiento de infraestructura adicional.

#### Contras
* Depende de la disponibilidad de la base de datos relacional.

### ZooKeeper / Consul

#### Pros
* Bloqueos en memoria ultrarrápidos.

#### Contras
* Complejidad operativa y sobrecarga masiva de recursos en Docker.

### Instancia Única (No Escalar)

#### Pros
* Cero código de coordinación.

#### Contras
* Punto único de fallo (SPOF) inaceptable en arquitecturas de microservicios modernas.