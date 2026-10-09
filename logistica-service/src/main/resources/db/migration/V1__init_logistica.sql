-- V1__init_logistica.sql — Schema logistica (DonaTrack)
-- Las tablas van sin prefijo: lo resuelven currentSchema y hibernate.default_schema.

-- 1. Tipos
CREATE TYPE estado_chofer       AS ENUM ('DISPONIBLE', 'EN_RUTA', 'DESHABILITADO');
CREATE TYPE estado_camion       AS ENUM ('DISPONIBLE', 'EN_RUTA', 'DESHABILITADO');
CREATE TYPE estado_ruta         AS ENUM ('PENDIENTE', 'EN_TRASLADO', 'COMPLETADA');
CREATE TYPE estado_entrega      AS ENUM ('PENDIENTE', 'EN_TRASLADO', 'ENTREGADA', 'NO_RECIBIDA', 'REVISION');
CREATE TYPE transicion_entrega  AS ENUM ('CONFIRMAR_ENTREGA', 'NO_RECEPCION', 'REVISION', 'REGRESO_DEPOSITO');
CREATE TYPE tipo_evento_entrega AS ENUM ('RUTA_ASIGNADA', 'ENTREGA_FALLIDA', 'ENTREGA_EXITOSA');
CREATE TYPE estado_solicitud    AS ENUM ('PENDIENTE', 'PROCESADA', 'ERROR');

-- 2. Geografía
CREATE TABLE pais (
                      id_pais UUID PRIMARY KEY,
                      nombre  TEXT NOT NULL,
                      CONSTRAINT uq_pais_nombre UNIQUE (nombre)
);

CREATE TABLE provincia (
                           id_provincia UUID PRIMARY KEY,
                           id_pais      UUID NOT NULL REFERENCES pais (id_pais),
                           nombre       TEXT NOT NULL,
                           CONSTRAINT uq_provincia_nombre UNIQUE (id_pais, nombre)
);

CREATE TABLE localidad (
                           id_localidad UUID PRIMARY KEY,
                           id_provincia UUID NOT NULL REFERENCES provincia (id_provincia),
                           nombre       TEXT NOT NULL,
                           CONSTRAINT uq_localidad_nombre UNIQUE (id_provincia, nombre)
);

-- Inmutable: se inserta una vez por entrega y no se actualiza.
CREATE TABLE direccion (
                           id_direccion  UUID PRIMARY KEY,
                           id_localidad  UUID     NOT NULL REFERENCES localidad (id_localidad),
                           calle         TEXT     NOT NULL,
                           altura        INTEGER  NOT NULL CHECK (altura > 0),  -- DER: SMALLINT (M4)
                           piso          SMALLINT,
                           departamento  TEXT,
                           codigo_postal TEXT     NOT NULL                      -- DER: SMALLINT (G5)
);

-- 3. Chofer y camión
CREATE TABLE chofer (
                        id_chofer     UUID PRIMARY KEY,
                        nombre        TEXT          NOT NULL,
                        apellido      TEXT          NOT NULL,
                        licencia      TEXT          NOT NULL,   -- DER: nuevo (G3)
                        telefono      TEXT          NOT NULL,   -- DER: SMALLINT (G2)
                        estado_chofer estado_chofer NOT NULL,
                        version       BIGINT        NOT NULL DEFAULT 0   -- DER: nuevo (D4)
    -- DER: habilitado BOOL eliminado (D6)
);

CREATE TABLE camion (
                        id_camion         UUID PRIMARY KEY,
                        patente           TEXT          NOT NULL,
                        capacidad_volumen FLOAT8        NOT NULL CHECK (capacidad_volumen > 0),
                        capacidad_peso    FLOAT8        NOT NULL CHECK (capacidad_peso > 0),
                        altura            FLOAT8        NOT NULL CHECK (altura > 0),  -- DER: nuevo (G4)
                        estado_camion     estado_camion NOT NULL,
                        version           BIGINT        NOT NULL DEFAULT 0,
                        CONSTRAINT uq_camion_patente UNIQUE (patente)
);

-- 4. Ruta
CREATE TABLE ruta (
                      id_ruta          UUID PRIMARY KEY,
                      id_chofer        UUID        NOT NULL REFERENCES chofer (id_chofer),  -- DER: nullable (M6)
                      id_camion        UUID        NOT NULL REFERENCES camion (id_camion),  -- DER: nullable (M6)
                      fecha            DATE        NOT NULL,                                -- DER: TIMESTAMPZ (M1)
                      estado           estado_ruta NOT NULL,
                      hora_inicio_real TIMESTAMPTZ,                                         -- DER: hora_inicio_estimada (G9)
                      hora_fin_real    TIMESTAMPTZ,                                         -- DER: hora_fin_estimada (G9)
                      version          BIGINT      NOT NULL DEFAULT 0
);

-- 5. Entrega
CREATE TABLE entrega (
                         id_entrega         UUID PRIMARY KEY,
                         id_ruta            UUID           REFERENCES ruta (id_ruta),            -- null mientras está PENDIENTE
                         id_direccion       UUID           NOT NULL REFERENCES direccion (id_direccion),
                         id_donacion        UUID           NOT NULL,   -- DER: FK (G8). Referencia a Donaciones, sin constraint
                         id_beneficiario    UUID           NOT NULL,   -- DER: FK (G8). Ídem
                         estado             estado_entrega NOT NULL,
                         hora_arribo        TIMESTAMPTZ,
                         hora_salida        TIMESTAMPTZ,
                         foto_recepcion_url TEXT,
                         volumen_total_m3   FLOAT8         NOT NULL CHECK (volumen_total_m3 > 0),
                         peso_total_kg      FLOAT8         NOT NULL CHECK (peso_total_kg > 0),
                         version            BIGINT         NOT NULL DEFAULT 0,
                         CONSTRAINT uq_entrega_donacion UNIQUE (id_donacion)                    -- D7
);

-- 6. Historiales (append-only)
CREATE TABLE cambio_estado_chofer (
                                      id_cambio_estado_chofer UUID PRIMARY KEY,
                                      id_chofer       UUID          NOT NULL REFERENCES chofer (id_chofer),
                                      estado_anterior estado_chofer,
                                      estado_nuevo    estado_chofer NOT NULL,
                                      timestamp       TIMESTAMPTZ   NOT NULL
);

CREATE TABLE cambio_estado_camion (
                                      id_cambio_estado_camion UUID PRIMARY KEY,
                                      id_camion       UUID          NOT NULL REFERENCES camion (id_camion),
                                      estado_anterior estado_camion,
                                      estado_nuevo    estado_camion NOT NULL,
                                      timestamp       TIMESTAMPTZ   NOT NULL
);

CREATE TABLE cambio_estado_ruta (
                                    id_cambio_estado_ruta UUID PRIMARY KEY,
                                    id_ruta         UUID        NOT NULL REFERENCES ruta (id_ruta),
                                    estado_anterior estado_ruta,
                                    estado_nuevo    estado_ruta NOT NULL,
                                    timestamp       TIMESTAMPTZ NOT NULL
);

CREATE TABLE cambio_estado_entrega (
                                       id_cambio_estado_entrega UUID PRIMARY KEY,
                                       id_entrega      UUID           NOT NULL REFERENCES entrega (id_entrega),
                                       estado_anterior estado_entrega,
                                       estado_nuevo    estado_entrega NOT NULL,
                                       timestamp       TIMESTAMPTZ    NOT NULL,
                                       actor           TEXT           NOT NULL
);

-- 7. Planificación (DER: nuevo, G6 / D5)
CREATE TABLE solicitud_planificacion (
                                         id_solicitud_planificacion UUID PRIMARY KEY,
                                         fecha               DATE             NOT NULL,
                                         estado              estado_solicitud NOT NULL,
                                         cantidad_donaciones INTEGER          NOT NULL CHECK (cantidad_donaciones > 0),
                                         callback_url        TEXT             NOT NULL,
                                         intentos_fallidos   INTEGER          NOT NULL DEFAULT 0,
                                         motivo_error        TEXT,
                                         version             BIGINT           NOT NULL DEFAULT 0
);

CREATE TABLE solicitud_planificacion_ruta (
                                              id_solicitud_planificacion UUID NOT NULL REFERENCES solicitud_planificacion (id_solicitud_planificacion),
                                              id_ruta                    UUID NOT NULL REFERENCES ruta (id_ruta),
                                              PRIMARY KEY (id_solicitud_planificacion, id_ruta)
);

-- 8. Solicitudes de transición
CREATE TABLE solicitud_transicion_entrega (
                                              id_solicitud       UUID PRIMARY KEY,
                                              id_entrega         UUID               NOT NULL REFERENCES entrega (id_entrega),
                                              tipo_transicion    transicion_entrega NOT NULL,
                                              actor              TEXT               NOT NULL,
                                              ocurrio_en         TIMESTAMPTZ        NOT NULL,
                                              replanificable     BOOLEAN,
                                              foto_recepcion_url TEXT,
                                              justificacion      TEXT,
                                              CONSTRAINT ck_solicitud_transicion_campos CHECK (
                                                  (tipo_transicion = 'CONFIRMAR_ENTREGA'               -- DER: foto obligatoria (G7)
                                                      AND justificacion IS NULL AND replanificable IS NULL)
                                                      OR (tipo_transicion = 'NO_RECEPCION'
                                                      AND justificacion IS NOT NULL AND replanificable IS NOT NULL AND foto_recepcion_url IS NULL)
                                                      OR (tipo_transicion IN ('REVISION', 'REGRESO_DEPOSITO')
                                                      AND justificacion IS NULL AND replanificable IS NULL AND foto_recepcion_url IS NULL)
                                                  )
);

-- 9. Eventos de entrega (base del futuro Outbox)
CREATE TABLE evento_entrega (
                                id_evento_entrega UUID PRIMARY KEY,
                                id_entrega        UUID                NOT NULL REFERENCES entrega (id_entrega),
                                id_donacion       UUID                NOT NULL,                -- DER: FK (G8)
                                id_ruta           UUID                REFERENCES ruta (id_ruta),
                                ocurrio_en        TIMESTAMPTZ         NOT NULL,
                                tipo              tipo_evento_entrega NOT NULL,
                                justificacion     TEXT,
                                replanificable    BOOLEAN,
                                CONSTRAINT ck_evento_entrega_campos CHECK (
                                    (tipo = 'RUTA_ASIGNADA'
                                        AND id_ruta IS NOT NULL AND justificacion IS NULL AND replanificable IS NULL)
                                        OR (tipo = 'ENTREGA_FALLIDA'
                                        AND justificacion IS NOT NULL AND replanificable IS NOT NULL AND id_ruta IS NULL)
                                        OR (tipo = 'ENTREGA_EXITOSA'                                   -- DER: id_ruta NULL (M2)
                                        AND justificacion IS NULL AND replanificable IS NULL)
                                    )
);

-- 10. Índices
CREATE INDEX idx_entrega_ruta                   ON entrega (id_ruta);
CREATE INDEX idx_entrega_estado                 ON entrega (estado);
CREATE INDEX idx_ruta_camion_estado             ON ruta (id_camion, estado);
CREATE INDEX idx_ruta_chofer_estado             ON ruta (id_chofer, estado);
CREATE INDEX idx_ruta_fecha                     ON ruta (fecha);
CREATE INDEX idx_direccion_localidad            ON direccion (id_localidad);
CREATE INDEX idx_cambio_estado_chofer_padre     ON cambio_estado_chofer (id_chofer);
CREATE INDEX idx_cambio_estado_camion_padre     ON cambio_estado_camion (id_camion);
CREATE INDEX idx_cambio_estado_ruta_padre       ON cambio_estado_ruta (id_ruta);
CREATE INDEX idx_cambio_estado_entrega_padre    ON cambio_estado_entrega (id_entrega);
CREATE INDEX idx_solicitud_transicion_entrega   ON solicitud_transicion_entrega (id_entrega);
CREATE INDEX idx_evento_entrega_entrega         ON evento_entrega (id_entrega);