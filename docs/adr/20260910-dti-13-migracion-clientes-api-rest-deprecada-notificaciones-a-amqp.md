# DTI-13: Migración de Clientes de la API REST Deprecada de Notificaciones a Ruta Canónica y AMQP

- Status: proposed
- Date: 2026-09-10
- Deciders: Decisión Grupal
- Tags: deuda-tecnica, dti-13, notificaciones, donaciones, incentivos, amqp, feign, event-id, inbox-pattern, retrocompatibilidad

## Contexto y Problema

En la Entrega 4, el servicio `notificaciones-service` evolucionó arquitectónicamente para adoptar:
1. **AMQP (RabbitMQ)** como canal de ingesta primario y asíncrono (`notificaciones.exchange`, cola `cola.eventos.notificaciones`, routing key `notificaciones.#`).
2. **Transactional Inbox Pattern** en la tabla relacional `notificaciones.evento_procesado` para deduplicación atómica $O(1)$ de reintentos mediante un campo `eventId: UUID` en `EventoNotificableDTO`.
3. **Ruta canónica REST** `/api/notificaciones/eventos` conforme al estándar de nomenclatura de endpoints `/api/*`.

Sin embargo, los microservicios emisores que despachan alertas a `notificaciones-service` conservan implementaciones heredadas de la Entrega 3:
* **`donaciones-service`:** Su cliente Feign [`NotificacionesFeignClient`](../../donaciones-service/src/main/java/grupo5/donaciones/infrastructure/clients/NotificacionesFeignClient.java) invoca `@PostMapping("/notificaciones")` y sus eventos de dominio no generan un `eventId` unívoco persistente.
* **`incentivos-service`:** Su cliente Feign [`NotificacionesFeignClient`](../../incentivos-service/src/main/java/grupo5/incentivos/infrastructure/clients/NotificacionesFeignClient.java) invoca `@PostMapping("/notificaciones")` con payloads anónimos (`Object`) y los records de [`NotificacionesClientAdapter`](../../incentivos-service/src/main/java/grupo5/incentivos/infrastructure/adapters/NotificacionesClientAdapter.java) carecen de `eventId`.

Para no quebrar la integración durante la remediación del PR 874 (`notificaciones-service`), se implementó una **capa de compatibilidad retrocompatible** en `notificaciones-service`:
* Se preserva el alias HTTP `POST /notificaciones` marcado como `@Deprecated`.
* Si un payload no contiene `eventId` (`null`), el servicio genera un fallback `UUID.randomUUID()` y registra un log estructurado `[FALLBACK_LEGACY_EVENT_ID]`.

Esta solución interina permite que `donaciones-service` e `incentivos-service` sigan funcionando sin cambios de código inmediatos, pero introduce **deuda técnica catalogada**:
1. **Falta de Idempotencia Real en Clientes Legacy:** Al generar un `eventId` aleatorio en cada request sin `eventId`, si `FeignRetryConfig` reintenta una petición ante timeouts de red, el reintento recibe un nuevo UUID y se despachan notificaciones duplicadas (vulnerabilidad de spam al usuario final).
2. **Acoplamiento Temporal Sincrónico:** Los emisores continúan bloqueando hilos HTTP en lugar de publicar asíncronamente en RabbitMQ.
3. **Mantenimiento de Rutas Deprecadas:** `notificaciones-service` debe soportar endpoints legacy fuera de la convención `/api/*`.

## Atributos de Calidad y Drivers de Decisión

* **Mantenibilidad y Limpieza de Contratos:** Erradicar endpoints obsoletos y estandarizar toda la comunicación REST bajo el prefijo `/api/*`.
* **Idempotencia y No Duplicación:** Garantizar que los reintentos de red de clientes Feign no produzcan duplicación física de mensajes.
* **Rendimiento y Desacoplamiento:** Migrar la comunicación inter-servicios de transporte HTTP síncrono a mensajería asíncrona AMQP sobre RabbitMQ.
* **Aislamiento de Módulos (Anti-Scope Creep):** Diferir formalmente la modificación de `donaciones-service` e `incentivos-service` a sus correspondientes ramas y PRs dedicados.

## Decisión Propuesta

Formalizar la migración pendiente de los clientes como **Deuda Técnica DTI-13**, estructurada en las siguientes etapas:

### Fase A: Adopción de `eventId` y Migración a Ruta Canónica REST (Corto Plazo)
1. **En `donaciones-service`:**
   - Actualizar `NotificacionesFeignClient` para apuntar a `@PostMapping("/api/notificaciones/eventos")`.
   - Propagar `eventId: UUID` en cada instancia de evento notificable (utilizando el identificador de evento de dominio de `donaciones`).
2. **En `incentivos-service`:**
   - Actualizar `NotificacionesFeignClient` para apuntar a `@PostMapping("/api/notificaciones/eventos")`.
   - Agregar el campo `UUID eventId` en los records internos de `NotificacionesClientAdapter` (`EventoMisionCumplidaRequest`, `EventoSubioCategoriaRequest`, `EventoDonanteInactivoRequest`).

### Fase B: Migración a Producción Asíncrona AMQP (Medio Plazo / Target)
1. Reemplazar los clientes Feign HTTP sincrónicos por despachadores AMQP que utilicen `RabbitTemplate.convertAndSend("notificaciones.exchange", routingKey, eventoDto)`.
2. Una vez completada la migración en ambos servicios, eliminar definitivamente el endpoint deprecated `POST /notificaciones` en `notificaciones-service` y remover el fallback de generación de UUIDs aleatorios.

## Estado de Implementación y Cuándo se Saldará

* **Implementation Status:** `[OBSERVED] deferred` — `donaciones-service` e `incentivos-service` continúan invocando `POST /notificaciones` sin `eventId`.
* **Servicios Afectados:** `donaciones-service`, `incentivos-service`.
* **Cuándo se saldará:**
  * **Fase A (Ruta canónica REST + `eventId`):** **Entrega 4 (Semana del 14 de Septiembre 2026)** — durante la estabilización de integración de contratos inter-servicios.
  * **Fase B (Migración a AMQP y remoción de ruta legacy):** **Entrega 5 (Semana del 19 de Octubre 2026)** — consolidación de mensajería asíncrona transversal previa a la entrega final.

## Enlaces Relacionados

* Guía técnica de migración: [`docs/arquitectura/contratos/guia-migracion-notificaciones-e4.md`](../arquitectura/contratos/guia-migracion-notificaciones-e4.md)
* Transactional Inbox en notificaciones: [`20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones.md`](./notificaciones-service/20260902-implementacion-del-inbox-pattern-para-idempotencia-en-notificaciones.md)
* Topología RabbitMQ: [`docs/arquitectura/eventos-amqp.md`](../arquitectura/eventos-amqp.md)
* Catálogo de Deuda Técnica: [`docs/adr/DEUDA_TECNICA.md`](./DEUDA_TECNICA.md)
