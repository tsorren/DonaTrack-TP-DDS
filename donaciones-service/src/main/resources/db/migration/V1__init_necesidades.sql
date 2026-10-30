CREATE TABLE necesidad (
    id UUID PRIMARY KEY,
    tipo_necesidad VARCHAR(20) NOT NULL, -- 'EXTRAORDINARIA', 'RECURRENTE'
    subcategoria_id UUID NOT NULL,
    entidad_beneficiaria_id UUID NULL,
    cantidad_necesitada BIGINT NOT NULL,
    descripcion VARCHAR(500) NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    periodo INT NULL -- Solo para RECURRENTE (días calculados)
);

CREATE TABLE periodo_necesidad (
    id UUID PRIMARY KEY,
    necesidad_id UUID NOT NULL REFERENCES necesidad(id) ON DELETE CASCADE,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin TIMESTAMP NOT NULL,
    cantidad_objetivo BIGINT NOT NULL
);

CREATE INDEX idx_necesidad_entidad_beneficiaria ON necesidad(entidad_beneficiaria_id);
CREATE INDEX idx_necesidad_activa ON necesidad(activa);
CREATE INDEX idx_periodo_necesidad_parent ON periodo_necesidad(necesidad_id);
