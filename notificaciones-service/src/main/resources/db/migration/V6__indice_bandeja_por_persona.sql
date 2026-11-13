-- ============================================================================
-- V6: INDICE DE LA BANDEJA POR PERSONA (R11-14)
-- ============================================================================
-- La bandeja se pide de a una pagina, de la mas nueva a la mas vieja (o al reves) y desempatada
-- por id: el indice sirve al orden y al corte de la pagina. idx_notificacion_persona_leida (V3)
-- queda para el conteo de sin leer.

CREATE INDEX idx_notificacion_persona_fecha
    ON notificacion (persona_id, fecha_creacion DESC, id DESC);
