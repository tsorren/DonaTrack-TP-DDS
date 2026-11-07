-- Catálogo (Categoria / Subcategoria / Alias) y Archivo de importación de donantes, según el DER.
-- Los enums se guardan como VARCHAR con CHECK (misma convención que V1).
-- Categoria.tipo_unidad no figura en el DER: el dominio la exige (ajuste aditivo, ver bitácora).

CREATE TABLE categoria (
    id UUID PRIMARY KEY,
    nombre TEXT NOT NULL,
    con_uso BOOLEAN NOT NULL,
    con_vencimiento BOOLEAN NOT NULL,
    tipo_unidad VARCHAR(20) NOT NULL
        CHECK (tipo_unidad IN ('UNIDADES', 'KILOGRAMO', 'LITROS', 'METRO', 'METRO_CUADRADO', 'METRO_CUBICO'))
);

CREATE TABLE subcategoria (
    id UUID PRIMARY KEY,
    id_categoria UUID NOT NULL REFERENCES categoria(id),
    nombre TEXT NOT NULL
);

CREATE TABLE alias (
    id UUID PRIMARY KEY,
    id_subcategoria UUID NOT NULL REFERENCES subcategoria(id) ON DELETE CASCADE,
    alias TEXT NOT NULL,
    CONSTRAINT uq_alias_subcategoria UNIQUE (id_subcategoria, alias)
);

CREATE TABLE archivo (
    id UUID PRIMARY KEY,
    path TEXT NOT NULL,
    estado_archivo VARCHAR(30) NOT NULL
        CHECK (estado_archivo IN ('PENDIENTE', 'PROCESANDO', 'PROCESADO', 'PROCESADO_CON_ERRORES', 'ERROR'))
);

CREATE INDEX idx_subcategoria_categoria ON subcategoria(id_categoria);
