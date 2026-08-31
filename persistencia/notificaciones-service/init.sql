-- ============================================================================
-- DONATRACK: SERVICIO DE NOTIFICACIONES - ESQUEMA RELACIONAL (POSTGRESQL)
-- ============================================================================
-- Este script refleja las decisiones de diseño guiado por el dominio (DDD)
-- y mapeo objeto-relacional (ORM) para la persistencia real del servicio.

-- ----------------------------------------------------------------------------
-- 1. AGREGADO PERSONA (réplica de lectura)
-- ----------------------------------------------------------------------------
CREATE TABLE persona (
    id UUID PRIMARY KEY,
    denominacion VARCHAR(150) NOT NULL,
    tipo_persona VARCHAR(20) NOT NULL -- 'HUMANA', 'JURIDICA'
);

-- ----------------------------------------------------------------------------
-- 2. JERARQUÍA MEDIO DE CONTACTO (Estrategia SINGLE TABLE)
-- ----------------------------------------------------------------------------
-- Combina Medio de Contacto, Correo y Teléfono en una sola tabla física.
CREATE TABLE medio_de_contacto (
    id UUID PRIMARY KEY,
    persona_id UUID NOT NULL REFERENCES persona(id) ON DELETE CASCADE,
    tipo_medio VARCHAR(20) NOT NULL,           -- Discriminador: 'CORREO', 'TELEFONO'
    es_predeterminado BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Columnas específicas de Correo (nullable)
    direccion_correo VARCHAR(150) NULL,
    
    -- Columnas específicas de Telefono (nullable)
    tipo_telefono VARCHAR(20) NULL,            -- 'ESTANDAR', 'WHATSAPP'
    caracteristica VARCHAR(10) NULL,
    codigo_area VARCHAR(10) NULL,
    numero VARCHAR(20) NULL
);

CREATE INDEX idx_medio_contacto_persona ON medio_de_contacto(persona_id);

-- ----------------------------------------------------------------------------
-- 3. AGREGADO NOTIFICACION (Raíz Independiente)
-- ----------------------------------------------------------------------------
CREATE TABLE notificacion (
    id UUID PRIMARY KEY,
    persona_id UUID NOT NULL,  -- SIN FOREIGN KEY: Se referencia por identidad para mantener independencia de agregados
    mensaje TEXT NOT NULL,
    fecha_creacion TIMESTAMP WITH TIME ZONE NOT NULL,
    estado_notificacion VARCHAR(20) NOT NULL -- 'PENDIENTE', 'ENVIADA', 'FALLIDA'
);

CREATE INDEX idx_notificacion_persona ON notificacion(persona_id);
CREATE INDEX idx_notificacion_estado ON notificacion(estado_notificacion);

-- ----------------------------------------------------------------------------
-- 4. HISTORIAL DE ESTADOS DE NOTIFICACIÓN
-- ----------------------------------------------------------------------------
-- Relación 1 a N desde la perspectiva correcta (la tabla hija apunta al padre)
CREATE TABLE notificacion_historial_estado (
    id UUID PRIMARY KEY,
    notificacion_id UUID NOT NULL REFERENCES notificacion(id) ON DELETE CASCADE,
    estado_anterior VARCHAR(20) NULL, -- Puede ser NULL en la primera transición de estado
    estado_nuevo VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_notificacion_historial_parent ON notificacion_historial_estado(notificacion_id);

-- ----------------------------------------------------------------------------
-- 5. DEDUPLICACIÓN POR EVENTO (Para futura implementación de Idempotencia)
-- ----------------------------------------------------------------------------
-- CREATE TABLE evento_procesado (
--     event_id UUID PRIMARY KEY,
--     procesado_en TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
-- );