-- V4__auditoria_fotos_confirmacion.sql
-- La auditoría de solicitudes de transición guardaba una sola foto por confirmación de
-- recepción (columna foto_recepcion_url). Desde RF-12 una confirmación lleva varias fotos:
-- se guardan todas en una tabla hija, con el mismo criterio que entrega_foto_recepcion (V3).
-- foto_recepcion_url se conserva con la primera foto por compatibilidad con lecturas existentes.

CREATE TABLE solicitud_transicion_foto_recepcion (
    id_solicitud UUID NOT NULL REFERENCES solicitud_transicion_entrega (id_solicitud) ON DELETE CASCADE,
    orden_foto   INT  NOT NULL,
    foto_url     TEXT NOT NULL,
    PRIMARY KEY (id_solicitud, orden_foto)
);

INSERT INTO solicitud_transicion_foto_recepcion (id_solicitud, orden_foto, foto_url)
SELECT id_solicitud, 0, foto_recepcion_url
FROM solicitud_transicion_entrega
WHERE foto_recepcion_url IS NOT NULL
  AND BTRIM(foto_recepcion_url) <> '';
