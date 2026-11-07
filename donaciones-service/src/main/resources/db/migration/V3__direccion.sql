-- Dirección inmutable y compartida entre personas y depósitos (decisión D-5 / D-9).
-- altura y piso van como INTEGER (el DER dice SMALLINT, que se queda corto para alturas de ruta).
CREATE TABLE direccion (
    id UUID PRIMARY KEY,
    calle TEXT NOT NULL,
    altura INTEGER NOT NULL,
    piso INTEGER NULL,
    departamento TEXT NULL,
    codigo_postal TEXT NOT NULL,
    localidad TEXT NOT NULL,
    provincia TEXT NOT NULL,
    pais TEXT NOT NULL
);

-- NULLS NOT DISTINCT: dos direcciones sin piso ni departamento son la misma
ALTER TABLE direccion ADD CONSTRAINT uq_direccion UNIQUE NULLS NOT DISTINCT
    (calle, altura, piso, departamento, codigo_postal, localidad, provincia, pais);
