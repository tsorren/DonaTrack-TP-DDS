# Patrón de Idempotencia y Deduplicación en Consumo de Eventos Distribuidos

- Status: proposed
- Date: 2026-09-01
- Deciders: Decisión Grupal
- Tags: resiliencia, idempotencia, rabbitmq, feign, duplicados, distributed-systems

## Contexto y Problema

En un ecosistema distribuido con redes no confiables, brokers de mensajería (RabbitMQ) y clientes HTTP con políticas de reintento (`FeignRetryConfig`), la semántica de entrega estándar es **al menos una vez (*At-Least-Once Delivery*)**. Esto implica que ante un timeout de red donde el servidor procesó la petición pero la respuesta no llegó a tiempo al cliente, el emisor reintentará el envío, despachando el mismo evento de negocio dos o más veces. Si el consumidor no es estrictamente **idempotente**, se generarán anomalías críticas de negocio: duplicación de donaciones en stock, cómputo doble de rachas/medallas en gamificación, o envíos reiterados de notificaciones por WhatsApp/Email al usuario.

## Atributos de Calidad y Drivers de Decisión

* **Integridad y Consistencia de Datos:** Procesar múltiples veces el mismo evento de negocio debe producir exactamente el mismo estado final que procesarlo una sola vez.
* **Tolerancia a Fallos y Reintentos:** Habilitar políticas agresivas de retry de red sin temor a corromper el modelo de dominio.
* **Rendimiento:** Verificar la duplicación con mínima sobrecarga de I/O.

## Origen y Lecciones de las Oleadas de Refactor

* **Oleadas de Origen:** Oleada 9.5 (Hardening de Bordes), Oleada 10 (§5 y §11) y [Auditoría Final de Notificaciones](../arquitectura/diseno/notificaciones/auditoria-final.md) (RF-10).
* **Hallazgo:** En `notificaciones-service`, se identificó que `NotificacionesFeignClient` reintenta hasta 5 veces ante fallas transitorias, pero como los DTOs de eventos carecían de un identificador de transacción único (`eventId`), cada reintento creaba y despachaba una nueva notificación física. En `incentivos-service`, llamadas duplicadas podían otorgar la misma medalla dos veces.

## Alternativas Consideradas

* **Deduplicación por Clave de Negocio Natural e Identificador de Evento (`eventId`):**
  1. Incorporar `@NotNull UUID eventId` en todos los DTOs y eventos de integración compartidos.
  2. En la capa de aplicación/persistencia, implementar una tabla o repositorio de control de mensajes consumidos (`evento_procesado(event_id UUID PRIMARY KEY, procesado_en TIMESTAMP)`).
  3. En entidades donde aplique, definir restricciones de unicidad a nivel de esquema (ej: `UNIQUE (donante_id, donacion_id)` en el historial de incentivos). Si el evento ya fue procesado, el consumidor responde exitosamente (`200 OK` / `ACK`) sin re-ejecutar la lógica.
* **Idempotencia Asumida en el Emisor (Sin Manejo en el Receptor):** Confiar en que la red no duplicará mensajes.
* **Deduplicación en Memoria con Cache Temporal (Redis / ConcurrentHashMap):** Mantener un conjunto de IDs con TTL en memoria.

## Resultado de la Decisión

Alternativa elegida: "Deduplicación por Clave de Negocio Natural e Identificador de Evento (`eventId`)"

Justificación:
Es el único patrón robusto que garantiza idempotencia duradera en sistemas distribuidos. Al coordinar la clave `eventId` entre el productor y el consumidor, y proteger tanto a nivel de aplicación (tabla de deduplicación) como a nivel de base de datos (restricciones de unicidad), los reintentos automáticos se vuelven 100% seguros y transparentes.

### Consecuencias Positivas

* Inmunidad total frente a tormentas de reintentos de Feign y reentregas de RabbitMQ.
* Garantía de que los usuarios nunca recibirán alertas duplicadas ni recompensas espurias.
* Compatibilidad con la arquitectura Transactional Outbox (que despacha con clave de idempotencia en los headers).

### Consecuencias Negativas

* Requiere actualizar coordinadamente los contratos DTO entre los microservicios emisores y receptores.
* Requiere una consulta adicional de existencia previa por cada mensaje entrante.

### Validación

Se valida mediante:
1. Tests unitarios y de integración enviando dos peticiones idénticas consecutivas con el mismo `eventId`, verificando que la segunda llamada retorne `200 OK` sin duplicar filas en la base de datos ni generar eventos colaterales.
2. Presencia de restricciones `UNIQUE` correspondientes en los esquemas DDL.

## Análisis de Alternativas

### Deduplicación por Clave Natural y eventId

#### Pros
* Consistencia garantizada matemáticamente y por motor relacional.
* Soporta caídas y reinicios del consumidor sin perder el registro de deduplicación.

#### Contras
* Escritura y almacenamiento de IDs procesados en base de datos.

### Idempotencia Asumida

#### Pros
* Cero código adicional.

#### Contras
* Altamente peligroso; genera corrupción de datos inevitable en redes reales.

### Cache en Memoria / Redis

#### Pros
* Búsquedas en memoria sub-milisegundo.

#### Contras
* Si se reinicia el nodo o expira el TTL prematuramente, se pierde la protección de deduplicación.