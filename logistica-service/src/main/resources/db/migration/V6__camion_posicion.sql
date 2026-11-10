-- V6__camion_posicion.sql
-- Última posición conocida del camión (ADR-E6-07, HB-05). Las tres columnas son nulables:
-- un camión que nunca salió de ruta no tiene posición. Se cargan juntas o ninguna.
-- Va como V6 porque V5 (donante de la entrega, HB-16) llega en otro PR.

ALTER TABLE camion
    ADD COLUMN posicion_latitud       FLOAT8,
    ADD COLUMN posicion_longitud      FLOAT8,
    ADD COLUMN posicion_registrada_en TIMESTAMPTZ,
    ADD CONSTRAINT ck_camion_posicion_completa CHECK (
        (posicion_latitud IS NULL AND posicion_longitud IS NULL AND posicion_registrada_en IS NULL)
        OR (posicion_latitud IS NOT NULL AND posicion_longitud IS NOT NULL AND posicion_registrada_en IS NOT NULL)
    );
