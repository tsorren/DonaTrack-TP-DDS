-- ============================================================================
-- V1: MIGRACIÓN INICIAL DEL SCHEMA DE INCENTIVOS
-- ============================================================================
-- Las tablas se crean sin prefijo de schema: lo resuelven currentSchema y
-- hibernate.default_schema (incentivos).

-- ----------------------------------------------------------------------------
-- 1. DONANTE (raíz del agregado DonanteIncentivos + métricas aplanadas)
-- ----------------------------------------------------------------------------
CREATE TABLE donante_incentivos (
    id                          UUID         PRIMARY KEY,
    persona_id                  UUID         NOT NULL,
    nombre                      VARCHAR(255),
    categoria                   VARCHAR(20)  NOT NULL, -- COLABORADOR, SOSTENEDOR, TRANSFORMADOR
    fecha_registro              DATE         NOT NULL,
    total_donaciones_historicas INTEGER      NOT NULL DEFAULT 0,
    total_donaciones_exitosas   INTEGER      NOT NULL DEFAULT 0,
    ultima_donacion             DATE,
    version                     BIGINT       NOT NULL DEFAULT 0 -- optimistic locking (@Version)
);

-- Un donante por persona. Habilita la futura búsqueda por persona_id sin otra migración.
CREATE UNIQUE INDEX idx_donante_incentivos_persona_id ON donante_incentivos (persona_id);

-- ----------------------------------------------------------------------------
-- 2. HIJOS DEL DONANTE SIN IDENTIDAD PROPIA (value objects)
-- ----------------------------------------------------------------------------
CREATE TABLE donante_historial_categoria (
    donante_id          UUID        NOT NULL REFERENCES donante_incentivos (id) ON DELETE CASCADE,
    orden               INTEGER     NOT NULL,
    categoria_anterior  VARCHAR(20) NOT NULL, -- COLABORADOR, SOSTENEDOR, TRANSFORMADOR
    categoria_nueva     VARCHAR(20) NOT NULL, -- COLABORADOR, SOSTENEDOR, TRANSFORMADOR
    fecha               DATE        NOT NULL,
    PRIMARY KEY (donante_id, orden)
);

CREATE TABLE donante_insignia_ganada (
    donante_id     UUID         NOT NULL REFERENCES donante_incentivos (id) ON DELETE CASCADE,
    orden          INTEGER      NOT NULL,
    nombre         VARCHAR(255) NOT NULL,
    descripcion    TEXT,
    imagen_url     TEXT,
    visible        BOOLEAN      NOT NULL DEFAULT TRUE,
    fecha_obtenida DATE,
    PRIMARY KEY (donante_id, orden),
    CONSTRAINT uq_donante_insignia_ganada_nombre UNIQUE (donante_id, nombre)
);

CREATE TABLE donante_organizacion_ayudada (
    donante_id      UUID NOT NULL REFERENCES donante_incentivos (id) ON DELETE CASCADE,
    organizacion_id UUID NOT NULL,
    PRIMARY KEY (donante_id, organizacion_id)
);

-- ----------------------------------------------------------------------------
-- 3. DONACIONES POR PERÍODO (conteo mensual de Metricas)
-- ----------------------------------------------------------------------------
CREATE TABLE donante_donaciones_por_periodo (
    donante_id UUID       NOT NULL REFERENCES donante_incentivos (id) ON DELETE CASCADE,
    periodo    VARCHAR(7) NOT NULL, -- formato yyyy-MM
    cantidad   BIGINT     NOT NULL,
    PRIMARY KEY (donante_id, periodo)
);

-- ----------------------------------------------------------------------------
-- 4. MISIONES (SINGLE_TABLE, discriminador tipo_mision)
-- ----------------------------------------------------------------------------
CREATE TABLE mision (
    id                    UUID         PRIMARY KEY,
    donante_id            UUID         NOT NULL REFERENCES donante_incentivos (id) ON DELETE CASCADE,
    tipo_mision           VARCHAR(20)  NOT NULL, -- COMPLETITUD, EXITOSAS, HABIL, RACHA
    numero_mision         INTEGER,
    nombre                VARCHAR(255) NOT NULL,
    descripcion           TEXT,
    categoria             VARCHAR(20)  NOT NULL, -- COLABORADOR, SOSTENEDOR, TRANSFORMADOR
    objetivo              INTEGER      NOT NULL,
    progreso_actual       INTEGER      NOT NULL DEFAULT 0,
    completada            BOOLEAN      NOT NULL DEFAULT FALSE,
    fecha_completada      DATE,
    insignia_nombre       VARCHAR(255),
    insignia_descripcion  TEXT,
    insignia_imagen_url   TEXT,
    fecha_ultimo_donacion DATE,        -- solo EXITOSAS
    ultimo_mes_donado     VARCHAR(7)   -- solo RACHA, formato yyyy-MM
);

CREATE INDEX idx_mision_donante_id ON mision (donante_id);

CREATE TABLE mision_categorias_donadas (
    mision_id UUID         NOT NULL REFERENCES mision (id) ON DELETE CASCADE,
    categoria VARCHAR(255) NOT NULL,
    PRIMARY KEY (mision_id, categoria)
);

-- ----------------------------------------------------------------------------
-- 5. RANKING MENSUAL
-- ----------------------------------------------------------------------------
CREATE TABLE ranking_mensual (
    id      UUID        PRIMARY KEY,
    periodo VARCHAR(7)  NOT NULL, -- formato yyyy-MM
    CONSTRAINT uq_ranking_mensual_periodo UNIQUE (periodo)
);

-- donante_id sin FK: el ranking es histórico y debe sobrevivir al donante.
CREATE TABLE ranking_mensual_entrada (
    ranking_id           UUID         NOT NULL REFERENCES ranking_mensual (id) ON DELETE CASCADE,
    posicion             INTEGER      NOT NULL,
    donante_id           UUID         NOT NULL,
    nombre_donante       VARCHAR(255),
    misiones_completadas BIGINT       NOT NULL,
    PRIMARY KEY (ranking_id, posicion)
);
