-- Paso 7: depósito, donación, sus ítems y su historial, e ítems normalizados (DER de Donaciones).
-- Fechas como TIMESTAMP (el dominio usa LocalDateTime, como en V1) y fechas de vencimiento como DATE
-- (LocalDate, como D-10). Peso y volumen como NUMERIC(12,3) en vez del BIGINT del DER (D-7).

-- P-2: el depósito es una tabla propia y compartida. En el dominio es un valor (nombre + dirección),
-- así que se reutiliza la fila idéntica, igual que la dirección (D-9).
CREATE TABLE deposito (
    id UUID PRIMARY KEY,
    id_direccion UUID NOT NULL REFERENCES direccion(id) ON DELETE RESTRICT,
    nombre TEXT NOT NULL,
    CONSTRAINT uq_deposito UNIQUE (nombre, id_direccion)
);

-- D2 del ADR de desnormalización: estado_donacion repite el último cambio del historial.
CREATE TABLE donacion (
    id UUID PRIMARY KEY,
    id_donante UUID NOT NULL REFERENCES donante(id) ON DELETE RESTRICT,
    descripcion TEXT NOT NULL,
    fecha TIMESTAMP NOT NULL,
    estado_donacion VARCHAR(20) NOT NULL CHECK (estado_donacion IN ('CARGADA', 'NORMALIZADA', 'SEGMENTADA')),
    id_deposito_donde_esta UUID NULL REFERENCES deposito(id) ON DELETE RESTRICT
);
CREATE INDEX idx_donacion_donante ON donacion (id_donante);

-- Los ítems no tienen identidad en el dominio: se reemplazan en cada guardado (como los medios).
CREATE TABLE item_donacion (
    id UUID PRIMARY KEY,
    id_donacion UUID NOT NULL REFERENCES donacion(id) ON DELETE CASCADE,
    descripcion TEXT NOT NULL,
    foto_url TEXT NULL,
    fecha_vencimiento DATE NULL,
    estado VARCHAR(20) NULL CHECK (estado IN ('NUEVO', 'USADO', 'DESGASTADO')),
    -- > 0: un valor que se redondea a 0 al guardar no se podría volver a leer (Bien lo exige)
    peso_unitario NUMERIC(12, 3) NOT NULL CHECK (peso_unitario > 0),
    volumen_unitario NUMERIC(12, 3) NOT NULL CHECK (volumen_unitario > 0),
    cantidad BIGINT NOT NULL
);
CREATE INDEX idx_item_donacion_donacion ON item_donacion (id_donacion);

-- Historial append-only: un cambio de estado no se modifica después de registrarse.
CREATE TABLE cambio_estado_donacion (
    id UUID PRIMARY KEY,
    id_donacion UUID NOT NULL REFERENCES donacion(id) ON DELETE CASCADE,
    estado_anterior VARCHAR(20) NULL CHECK (estado_anterior IN ('CARGADA', 'NORMALIZADA', 'SEGMENTADA')),
    estado_nuevo VARCHAR(20) NOT NULL CHECK (estado_nuevo IN ('CARGADA', 'NORMALIZADA', 'SEGMENTADA')),
    "timestamp" TIMESTAMP NOT NULL
);
CREATE INDEX idx_cambio_estado_donacion_donacion ON cambio_estado_donacion (id_donacion);

-- D4: foto del bien original en cada ítem normalizado. Además de bien_descripcion (DER), se copian
-- foto, vencimiento, estado, peso y volumen, y el estado de normalización con los 3 valores del
-- dominio (P-3). bien_nombre queda del DER sin uso: el dominio no tiene nombre del bien.
CREATE TABLE item_donacion_normalizado (
    id UUID PRIMARY KEY,
    id_donacion UUID NOT NULL REFERENCES donacion(id) ON DELETE RESTRICT,
    cantidad BIGINT NOT NULL,
    segmentado BOOLEAN NOT NULL DEFAULT FALSE,
    bien_nombre TEXT NULL,
    bien_descripcion TEXT NOT NULL,
    foto_url TEXT NULL,
    fecha_vencimiento DATE NULL,
    estado VARCHAR(20) NULL CHECK (estado IN ('NUEVO', 'USADO', 'DESGASTADO')),
    -- > 0: un valor que se redondea a 0 al guardar no se podría volver a leer (Bien lo exige)
    peso_unitario NUMERIC(12, 3) NOT NULL CHECK (peso_unitario > 0),
    volumen_unitario NUMERIC(12, 3) NOT NULL CHECK (volumen_unitario > 0),
    id_subcategoria UUID NOT NULL REFERENCES subcategoria(id) ON DELETE RESTRICT,
    confianza_normalizacion DOUBLE PRECISION NOT NULL,
    estado_normalizacion VARCHAR(20) NOT NULL
        CHECK (estado_normalizacion IN ('PENDIENTE_REVISION', 'ACEPTADO', 'RECHAZADO'))
);
CREATE INDEX idx_item_normalizado_donacion ON item_donacion_normalizado (id_donacion);
CREATE INDEX idx_item_normalizado_subcategoria ON item_donacion_normalizado (id_subcategoria);
