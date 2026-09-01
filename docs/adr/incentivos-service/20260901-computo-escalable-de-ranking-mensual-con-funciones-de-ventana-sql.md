# Cómputo Escalable de Ranking Mensual con Funciones de Ventana SQL

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: incentivos, performance, sql, postgresql, rankings, persistencia

## Contexto y Problema

En `incentivos-service`, al finalizar cada mes, el sistema debe calcular el **Ranking Mensual de Donantes** para determinar el podio, asignar insignias de honor y publicar los balances en la cartelera comunitaria. En la implementación en memoria (Fase 1), el job invoca `donantesRepository.findAll()`, carga a todos los donantes del sistema a la memoria heap de la JVM, itera sobre ellos en un stream de Java, los ordena por puntos acumulados y construye los objetos del ranking. Si bien esto funciona para pruebas con 50 usuarios, con una base real de 100.000 o 500.000 donantes este enfoque colapsará la memoria JVM (OutOfMemoryError), provocará pausas masivas de Garbage Collection (Stop-The-World) y degradará severamente la latencia del microservicio.

## Atributos de Calidad y Drivers de Decisión

* **Escalabilidad y Rendimiento:** El tiempo de cálculo y el consumo de memoria deben ser independientes del volumen total de donantes ($O(1)$ en heap).
* **Consistencia y Determinismo:** El cálculo del podio debe aplicar un desempate determinista sin variación incidental.
* **Eficiencia en Red y CPU:** No transferir cientos de miles de filas innecesariamente entre la base de datos y la aplicación.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 10 de Persistencia en `incentivos-service` ([decisiones_futuras_en_oleada_10.md](../arquitectura/diseno/incentivos/decisiones_futuras_en_oleada_10.md) §6).
* **Hallazgo:** El análisis de escalabilidad de la Oleada 10 demostró que trasladar el ordenamiento y cálculo de percentiles al motor relacional PostgreSQL mediante la función de ventana `ROW_NUMBER() OVER (...)` reduce el consumo de memoria del backend a prácticamente cero.

## Alternativas Consideradas

* **Cómputo en Motor SQL Nativo con Window Functions e INSERT directo:**
  Ejecutar un procedimiento o query nativa en PostgreSQL:
  ```sql
  INSERT INTO ranking_mensual_posicion (id, ranking_id, donante_id, posicion, puntos)
  SELECT gen_random_uuid(), :rankingId, id,
         ROW_NUMBER() OVER (ORDER BY puntos_totales DESC, fecha_registro ASC),
         puntos_totales
  FROM donante_incentivos
  WHERE activo = true;
  ```
  La base de datos ejecuta el ordenamiento y la inserción atómicamente en disco/memoria compartida; la JVM solo envía el comando de activación.
* **Procesamiento Paginado en JVM (`PageRequest.of(page, 1000)`):** Traer lotes de 1.000 donantes a memoria secuencialmente.
* **Mantener `findAll()` en Memoria Heap:** Continuar cargando la lista completa de donantes.

## Resultado de la Decisión

Alternativa elegida: "Cómputo en Motor SQL Nativo con Window Functions e INSERT directo"

Justificación:
Los motores de bases de datos relacionales como PostgreSQL están altamente optimizados en C/C++ para ejecutar ordenamientos masivos sobre índices y discos temporales. Delegar el cómputo del ranking a PostgreSQL garantiza un tiempo de ejecución predecible y mantiene la memoria heap de `incentivos-service` en $O(1)$, permitiendo que el microservicio opere de manera ligera incluso con millones de registros.

### Consecuencias Positivas

* Consumo de memoria heap JVM constante e insignificante durante el cierre mensual.
* Reducción drástica del tráfico de red entre el contenedor del backend y PostgreSQL.
* Criterio de desempate determinista explícito (`ORDER BY puntos DESC, fecha_registro ASC`).

### Consecuencias Negativas

* La consulta depende de la sintaxis nativa de PostgreSQL (incompatible con motores simplificados en memoria como H2).

### Validación

Se valida mediante:
1. Query documentada en `decisiones_futuras_en_oleada_10.md` de incentivos.
2. Pruebas de integración con Testcontainers PostgreSQL con fixtures masivas de donantes sintéticos.

## Análisis de Alternativas

### Cómputo Nativo SQL (Window Functions)

#### Pros
* Rendimiento óptimo de nivel empresarial.
* Cero presión sobre el recolector de basura de la JVM.

#### Contras
* Requiere dialecto específico de PostgreSQL.

### Paginación en JVM

#### Pros
* Mantiene la lógica en código Java.

#### Contras
* Ordenar páginas independientes no resuelve un ranking global sin múltiples pasadas.
* Tráfico de red masivo de ida y vuelta.

### findAll() en Heap

#### Pros
* Código de pocas líneas con streams de Java.

#### Contras
* Inviable en producción; garantiza caídas por OutOfMemoryError.