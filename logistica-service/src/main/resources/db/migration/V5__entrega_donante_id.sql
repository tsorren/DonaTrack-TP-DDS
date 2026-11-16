-- V5__entrega_donante_id.sql
-- HB-16 (ADR de réplicas por evento, 2026-11-06): la entrega guarda el donante de la donación
-- original como referencia opaca, sin clave foránea, para listar las entregas activas de un
-- donante en una llamada. Es opcional: las entregas anteriores quedan sin donante (sin backfill).

ALTER TABLE entrega ADD COLUMN donante_id UUID;

CREATE INDEX IF NOT EXISTS idx_entrega_donante ON entrega (donante_id);
