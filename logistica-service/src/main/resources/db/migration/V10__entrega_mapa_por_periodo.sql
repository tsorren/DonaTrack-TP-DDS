-- V10__entrega_mapa_por_periodo.sql
-- R11-19 (lote p1-mapa): el mapa público pide las ENTREGADA de un período, de la más reciente a
-- la más vieja y con tope (GET /api/entregas/mapa?desde&hasta&limite). El índice por estado de V1
-- no ordena por hora de arribo; este resuelve el filtro, el orden y el conteo en la base.

CREATE INDEX IF NOT EXISTS idx_entrega_estado_hora_arribo
    ON entrega (estado, hora_arribo DESC);
