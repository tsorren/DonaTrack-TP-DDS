-- V15__donacion_fotos_recepcion.sql
-- HB-01 (ADR de réplicas por evento, 2026-11-06; D28): entrega.exitosa trae las URLs estables de
-- las fotos de recepción y Donaciones las replica para servir GET /api/donaciones/destacadas sin
-- llamar a Logística. Solo se replican las de una entrega sin reporte (ADR de entregas con
-- problema, 2026-11-08): fecha_entrega queda nula si no hay fotos publicables. Sin backfill.

ALTER TABLE donacion_independiente ADD COLUMN fecha_entrega TIMESTAMP;

CREATE TABLE foto_recepcion_donacion_independiente (
    id_donacion_independiente UUID NOT NULL
        REFERENCES donacion_independiente(id) ON DELETE CASCADE,
    orden INTEGER NOT NULL,
    url TEXT NOT NULL,
    PRIMARY KEY (id_donacion_independiente, orden)
);

-- Las destacadas filtran por fecha de entrega, de la más nueva a la más vieja
CREATE INDEX idx_donacion_independiente_fecha_entrega
    ON donacion_independiente (fecha_entrega DESC)
    WHERE fecha_entrega IS NOT NULL;
