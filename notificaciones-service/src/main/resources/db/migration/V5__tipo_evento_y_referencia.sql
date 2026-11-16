-- ============================================================================
-- V5: TIPO DE EVENTO Y REFERENCIA DE LA NOTIFICACION (HB-09)
-- ============================================================================
-- Columnas opcionales: las notificaciones anteriores quedan en NULL y el cliente las muestra
-- como "Otras novedades". referencia_id es el id de la donacion a la que se refiere el aviso,
-- sin clave foranea (la donacion vive en otro servicio).

ALTER TABLE notificacion
    ADD COLUMN tipo_evento VARCHAR(40) NULL,
    ADD COLUMN referencia_id UUID NULL;
