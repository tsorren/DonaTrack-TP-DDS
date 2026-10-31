-- Paso 9: propuestas de asignación y sus posibles fragmentaciones (DER de Donaciones, con la
-- corrección D-23). Fechas como TIMESTAMP (LocalDateTime, como V1, V9 y V10).

-- estado y fecha_creacion no están en el DER: el dominio los usa para aprobar o descartar una
-- propuesta y para listarlas. Las descartadas se conservan.
CREATE TABLE propuesta (
    id UUID PRIMARY KEY,
    id_necesidad_que_satisface UUID NOT NULL REFERENCES necesidad(id) ON DELETE RESTRICT,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'APROBADA', 'DESCARTADA')),
    fecha_creacion TIMESTAMP NOT NULL
);
CREATE INDEX idx_propuesta_necesidad ON propuesta (id_necesidad_que_satisface);

-- D-23: id propio y FK a la donación independiente (el DER la conectaba por su PK, y una donación
-- en dos propuestas chocaba). D-8: cantidad INTEGER (el DER dice SMALLINT; el dominio es Integer).
-- Las fragmentaciones no tienen identidad en el dominio: se reemplazan en cada guardado.
CREATE TABLE posible_fragmentacion (
    id UUID PRIMARY KEY,
    id_propuesta UUID NOT NULL REFERENCES propuesta(id) ON DELETE CASCADE,
    id_donacion_independiente UUID NOT NULL REFERENCES donacion_independiente(id) ON DELETE RESTRICT,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0)
);
CREATE INDEX idx_posible_fragmentacion_propuesta ON posible_fragmentacion (id_propuesta);
CREATE INDEX idx_posible_fragmentacion_donacion ON posible_fragmentacion (id_donacion_independiente);
