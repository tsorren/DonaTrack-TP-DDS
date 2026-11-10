-- V7__entrega_remito_y_reporte.sql
-- HB-17 (ADR del contrato de entregas para la entidad, 2026-11-02; DTI-29): al confirmar, la
-- entrega guarda el número de remito y, si llegó con un problema, el reporte (motivo, detalle y
-- cantidad recibida). Todo opcional: las entregas anteriores quedan sin remito ni reporte.

ALTER TABLE entrega ADD COLUMN numero_remito VARCHAR(40);
ALTER TABLE entrega ADD COLUMN reporte_motivo VARCHAR(20);
ALTER TABLE entrega ADD COLUMN reporte_detalle VARCHAR(500);
ALTER TABLE entrega ADD COLUMN reporte_cantidad_recibida DOUBLE PRECISION;

ALTER TABLE entrega ADD CONSTRAINT ck_entrega_reporte_motivo
    CHECK (reporte_motivo IS NULL OR reporte_motivo IN ('INCOMPLETA', 'DANADA', 'OTRO'));
