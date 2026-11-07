-- V3__e5_correcciones_temporales.sql
-- Migración temporal de soporte sobre PostgreSQL para el desarrollo y validación de las
-- correcciones de E5 (RF-02, RF-09, RF-12 y RF-14) mientras se completa la migración al
-- motor definitivo MongoDB.
--
-- Mapeo previsto en MongoDB definitivo:
--   * Colección `entregas`:
--       - `destino.ubicacion`: GeoJSON Point `{ type: "Point", coordinates: [longitud, latitud] }`
--         con índice `2dsphere` parcial sobre `destino.ubicacion`.
--       - `fotosRecepcion`: arreglo embebido de referencias `List<String>` en el documento de entrega.
--       - Índices sobre `idDonacion` (único), `idBeneficiaria`, `estadoActual` e `idRuta`.
--   * Colección `rutas`:
--       - `paradas`: arreglo embebido con `{ ordenVisita, idEntrega }`.

-- 1. Coordenadas geográficas opcionales en dirección de destino (RF-02 / preparación geocodificación)
ALTER TABLE direccion
    ADD COLUMN latitud  FLOAT8,
    ADD COLUMN longitud FLOAT8,
    ADD CONSTRAINT ck_direccion_latitud_rango
        CHECK (latitud IS NULL OR (latitud >= -90.0 AND latitud <= 90.0)),
    ADD CONSTRAINT ck_direccion_longitud_rango
        CHECK (longitud IS NULL OR (longitud >= -180.0 AND longitud <= 180.0)),
    ADD CONSTRAINT ck_direccion_coordenadas_par_completo
        CHECK ((latitud IS NULL AND longitud IS NULL) OR (latitud IS NOT NULL AND longitud IS NOT NULL));

-- 2. Colección de referencias de fotos de recepción por entrega (RF-12)
CREATE TABLE entrega_foto_recepcion (
    id_entrega UUID NOT NULL REFERENCES entrega (id_entrega) ON DELETE CASCADE,
    orden_foto INT  NOT NULL,
    foto_url   TEXT NOT NULL,
    PRIMARY KEY (id_entrega, orden_foto)
);

INSERT INTO entrega_foto_recepcion (id_entrega, orden_foto, foto_url)
SELECT id_entrega, 0, foto_recepcion_url
FROM entrega
WHERE foto_recepcion_url IS NOT NULL
  AND BTRIM(foto_recepcion_url) <> '';

-- 3. Índices adicionales para consultas de seguimiento por beneficiario (RF-14) y paradas de ruta
CREATE INDEX IF NOT EXISTS idx_entrega_beneficiario ON entrega (id_beneficiario);
CREATE INDEX IF NOT EXISTS idx_parada_ruta_entrega  ON parada_ruta (id_ruta, id_entrega);
