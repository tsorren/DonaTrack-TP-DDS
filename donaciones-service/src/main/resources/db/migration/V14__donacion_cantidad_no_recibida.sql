-- V14__donacion_cantidad_no_recibida.sql
-- HB-17 (ADR del contrato de entregas para la entidad, 2026-11-02; DTI-29): cuando la entidad
-- confirma una entrega que llegó incompleta, entrega.exitosa trae la cantidad recibida y la
-- donación guarda lo que no llegó. La necesidad cuenta lo que llegó (decisión del usuario,
-- 2026-11-08). Nulo si llegó todo; las donaciones anteriores quedan sin valor (sin backfill).

ALTER TABLE donacion_independiente ADD COLUMN cantidad_no_recibida INTEGER;

ALTER TABLE donacion_independiente ADD CONSTRAINT ck_donacion_cantidad_no_recibida
    CHECK (cantidad_no_recibida IS NULL OR cantidad_no_recibida > 0);
