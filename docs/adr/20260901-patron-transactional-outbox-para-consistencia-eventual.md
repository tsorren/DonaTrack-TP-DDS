# Patrón Transactional Outbox para Consistencia Eventual

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: persistencia, outbox, consistencia-eventual, rabbitmq, n8n, dual-write

## Contexto y Problema

Al transicionar hacia la persistencia física (Fase 2 / PostgreSQL), los casos de uso que mutan el estado de un agregado y deben notificar a otros servicios (ej: publicación de eventos a RabbitMQ o llamadas a Webhooks de n8n) se enfrentan al problema clásico de la **doble escritura (*dual-write*)**:
1. Si el mensaje se publica al broker de mensajería dentro de la transacción de base de datos y la base de datos hace `rollback` (por fallo de concurrencia o violación de restricción), se han emitido eventos falsos (*phantom messages*) que el resto del sistema ya consumió.
2. Si el mensaje se publica después del `commit` pero la red o el broker fallan, la base de datos confirmó el cambio pero el evento nunca se despachó, provocando inconsistencia distribuida permanente.
No es posible utilizar transacciones distribuidas en dos fases (2PC / XA) por su alto costo de bloqueo y pobre rendimiento en microservicios.

## Atributos de Calidad y Drivers de Decisión

* **Consistencia de Datos:** Garantizar que nunca se publiquen eventos de transacciones revertidas ni se pierdan eventos de transacciones confirmadas (*At-Least-Once Delivery*).
* **Disponibilidad:** Desacoplar la confirmación de la transacción local de la disponibilidad inmediata de RabbitMQ o n8n.
* **Escalabilidad:** Permitir que múltiples pods compitan por despachar eventos sin colisiones.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 10 de Persistencia en `donaciones-service` e `incentivos-service` ([decisiones_futuras_en_oleada_10.md](../arquitectura/diseno/donaciones/decisiones_futuras_en_oleada_10.md)).
* **Hallazgo:** En `incentivos-service` (Oleada 10 §13), se descubrió que los webhooks hacia n8n se despachaban con llamadas reactivas `.subscribe()` *fire-and-forget*, con alto riesgo de pérdida de datos si n8n sufría sobrecarga temporal. Se diseñó la tabla `outbox_events` con relay desacoplado para garantizar entrega segura.

## Alternativas Consideradas

* **Patrón Transactional Outbox con Polling Worker Desacoplado:** Escribir atómicamente la mutación de la entidad y el registro del evento en la tabla `outbox_events` dentro de la misma transacción local ACID. Un worker en background (`@Scheduled` o relay asíncrono) consulta los eventos pendientes usando `SELECT ... FOR UPDATE SKIP LOCKED`, los publica con reintentos exponenciales y actualiza su estado a `PROCESADO`.
* **Publicación Directa en Transacción con Rollback Compensatorio:** Publicar al broker y, si falla, intentar un rollback manual mediante código de compensación.
* **Captura de Datos de Cambio (CDC) con Debezium/Kafka Connect:** Leer el transaction log (WAL) de PostgreSQL directamente a nivel de infraestructura.

## Resultado de la Decisión

Alternativa elegida: "Patrón Transactional Outbox con Polling Worker Desacoplado"

Justificación:
El Transactional Outbox resuelve de forma elegante y determinista el dual-write utilizando únicamente la base de datos relacional PostgreSQL ya disponible en el proyecto, sin requerir infraestructura externa compleja como Debezium o Kafka. El uso de `SKIP LOCKED` garantiza escalabilidad horizontal entre réplicas sin bloqueos contenciosos, y el índice parcial sobre `WHERE estado = 'PENDIENTE'` asegura tiempos de respuesta menores a 1 ms.

### Consecuencias Positivas

* Garantía de entrega *at-least-once* para todos los eventos de integración.
* Cero riesgo de mensajes fantasma o desincronización por caídas temporales de RabbitMQ o n8n.
* Trazabilidad preservada: cada fila de la tabla outbox guarda el `trace_id` original de la transacción.

### Consecuencias Negativas

* Requiere que los consumidores sean estrictamente idempotentes (ya que ante fallos transitorios del worker el mismo mensaje puede enviarse más de una vez).
* Requiere un job de purga periódica (`OutboxCleanupJob`) para eliminar eventos procesados con más de 14 días.

### Validación

Se valida mediante:
1. Tests de integración con Testcontainers simulando cortes de red con RabbitMQ y confirmando que los eventos quedan acumulados en `outbox_events` y se despachan al restablecer la conexión.
2. Verificación de índice parcial `idx_outbox_pendientes_parcial` en PostgreSQL.

## Análisis de Alternativas

### Transactional Outbox con SKIP LOCKED

#### Pros
* No agrega nuevos contenedores ni complejidad de infraestructura.
* Confiabilidad respaldada por transacciones ACID locales.
* Escalable horizontalmente sin contención de hilos.

#### Contras
* Ligera latencia adicional de polling (típicamente 500 ms - 1 s) en comparación con publicación en memoria.

### Publicación Directa

#### Pros
* Inmediatez de despacho.

#### Contras
* Imposible garantizar consistencia; genera inconsistencias graves de datos ante fallos parciales de red.

### CDC con Debezium

#### Pros
* Latencia de publicación sub-segundo basada en WAL.

#### Contras
* Requiere levantar y operar un clúster Kafka, Kafka Connect y esquemas de registro, sobrecargando masivamente el entorno académico.