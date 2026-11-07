-- ============================================================================
-- V3: CICLO DE LECTURA DE NOTIFICACIONES (IN-APP)
-- ============================================================================

ALTER TABLE notificacion
    ADD COLUMN leida BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN fecha_lectura TIMESTAMP NULL;

-- Índice compuesto para resolver instantáneamente el conteo y la bandeja de pendientes
CREATE INDEX idx_notificacion_persona_leida ON notificacion(persona_id, leida);