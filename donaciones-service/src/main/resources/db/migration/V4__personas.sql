-- Personas (JOINED: persona + humana | juridica) y sus medios de contacto (medio_de_contacto + telefono | correo).
-- Enums como VARCHAR + CHECK (decisión P-8). fecha_nacimiento como DATE (D-10).

CREATE TABLE persona (
    id UUID PRIMARY KEY,
    tipo_persona VARCHAR(10) NOT NULL CHECK (tipo_persona IN ('HUMANA', 'JURIDICA')),
    id_direccion UUID NULL REFERENCES direccion(id),
    tipo_documento VARCHAR(15) NULL CHECK (tipo_documento IN ('DNI', 'PASAPORTE', 'LIBRETA', 'CUIT')),
    nro_documento TEXT NULL
);

-- D-13: el documento identifica a una persona; las que no tienen (o están anonimizadas) no chocan entre sí
CREATE UNIQUE INDEX uq_persona_documento ON persona (tipo_documento, nro_documento) NULLS NOT DISTINCT
    WHERE nro_documento IS NOT NULL;
CREATE INDEX idx_persona_direccion ON persona (id_direccion);

CREATE TABLE juridica (
    id UUID PRIMARY KEY REFERENCES persona(id) ON DELETE CASCADE,
    razon_social TEXT NOT NULL,
    tipo_juridico VARCHAR(20) NOT NULL CHECK (tipo_juridico IN ('GUBERNAMENTAL', 'ONG', 'EMPRESA', 'INSTITUCION')),
    rubro TEXT NOT NULL
);

-- D-14: representante_de es una sola columna, así que una humana representa a una sola jurídica
CREATE TABLE humana (
    id UUID PRIMARY KEY REFERENCES persona(id) ON DELETE CASCADE,
    nombre TEXT NOT NULL,
    apellido TEXT NOT NULL,
    genero VARCHAR(20) NULL CHECK (genero IN ('PREFIERO_NO_DECIR', 'HOMBRE', 'MUJER', 'NO_BINARIO')),
    representante_de UUID NULL REFERENCES juridica(id) ON DELETE SET NULL,
    fecha_nacimiento DATE NULL
);
CREATE INDEX idx_humana_representante_de ON humana (representante_de);

CREATE TABLE medio_de_contacto (
    id UUID PRIMARY KEY,
    id_persona UUID NOT NULL REFERENCES persona(id) ON DELETE CASCADE,
    es_predeterminado BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_medio_de_contacto_persona ON medio_de_contacto (id_persona);

-- Una persona nueva trae un teléfono vacío (así lo crea el dominio), por eso sus campos admiten NULL
CREATE TABLE telefono (
    id UUID PRIMARY KEY REFERENCES medio_de_contacto(id) ON DELETE CASCADE,
    caracteristica TEXT NULL,
    codigo_area TEXT NULL,
    numero TEXT NULL,
    tipo_telefono VARCHAR(10) NOT NULL DEFAULT 'ESTANDAR' CHECK (tipo_telefono IN ('WHATSAPP', 'ESTANDAR'))
);

CREATE TABLE correo (
    id UUID PRIMARY KEY REFERENCES medio_de_contacto(id) ON DELETE CASCADE,
    direccion_correo TEXT NULL
);
