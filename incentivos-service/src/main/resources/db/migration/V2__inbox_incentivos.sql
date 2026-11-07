-- ============================================================================
-- V2: INBOX DE EVENTOS PROCESADOS (deduplicación por identificador de evento)
-- ============================================================================
-- Misma estructura que evento_procesado de notificaciones-service
-- (V2__inbox_notificaciones.sql). La clave es el messageId AMQP que fija el
-- emisor. Se inserta en la misma transacción que el guardado del donante: si el
-- guardado falla, la marca se revierte y el evento puede reintentarse.
CREATE TABLE evento_procesado (
    event_id            UUID      PRIMARY KEY,
    fecha_procesamiento TIMESTAMP NOT NULL
);
