-- ============================================================================
-- V1: MIGRACIÓN INICIAL DEL SCHEMA DE NOTIFICACIONES
-- ============================================================================

-- 1. TABLA PERSONA (Réplica de lectura)
CREATE TABLE persona (
                         id UUID PRIMARY KEY,
                         denominacion VARCHAR(150) NOT NULL,
                         tipo_persona VARCHAR(20) NOT NULL -- 'HUMANA', 'JURIDICA'
);

-- 2. TABLA MEDIO_DE_CONTACTO (Estrategia SINGLE TABLE)
CREATE TABLE medio_de_contacto (
                                   id UUID PRIMARY KEY,
                                   persona_id UUID NOT NULL REFERENCES persona(id) ON DELETE CASCADE,
                                   tipo_medio VARCHAR(20) NOT NULL,           -- Discriminador: 'CORREO', 'TELEFONO'
                                   es_predeterminado BOOLEAN NOT NULL DEFAULT FALSE,

    -- Específico de Correo
                                   direccion_correo VARCHAR(150) NULL,

    -- Específico de Teléfono
                                   tipo_telefono VARCHAR(20) NULL,            -- 'ESTANDAR', 'WHATSAPP'
                                   caracteristica VARCHAR(10) NULL,
                                   codigo_area VARCHAR(10) NULL,
                                   numero VARCHAR(20) NULL
);

CREATE INDEX idx_medio_contacto_persona ON medio_de_contacto(persona_id);

-- 3. TABLA NOTIFICACION (Raíz de Agregado Independiente)
CREATE TABLE notificacion (
                              id UUID PRIMARY KEY,
                              persona_id UUID NOT NULL,
                              mensaje TEXT NOT NULL,
                              fecha_creacion TIMESTAMP NOT NULL,
                              estado_notificacion VARCHAR(20) NOT NULL -- 'PENDIENTE', 'ENVIADA', 'FALLIDA'
);

CREATE INDEX idx_notificacion_persona ON notificacion(persona_id);
CREATE INDEX idx_notificacion_estado ON notificacion(estado_notificacion);

-- 4. TABLA HISTORIAL DE ESTADOS
CREATE TABLE notificacion_historial_estado (
                                               id UUID PRIMARY KEY,
                                               notificacion_id UUID NOT NULL REFERENCES notificacion(id) ON DELETE CASCADE,
                                               estado_anterior VARCHAR(20) NULL,
                                               estado_nuevo VARCHAR(20) NOT NULL,
                                               timestamp TIMESTAMP NOT NULL
);

CREATE INDEX idx_notificacion_historial_parent ON notificacion_historial_estado(notificacion_id);