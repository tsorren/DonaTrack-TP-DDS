-- Paso 10: solicitud de entrega a Logística, outbox del broker (Transactional Outbox) e idempotencia
-- de los eventos que llegan de Logística (DER de Donaciones). Fechas como TIMESTAMP (LocalDateTime).

-- fecha_creacion y proveedores_descartados no están en el DER. proveedores_descartados es
-- PROVISORIA (D-37, pendiente de discusión con el equipo): sin ella, al leer la solicitud se olvidan
-- los rechazos de la ronda y el broker alterna entre proveedores sin terminar nunca. La alternativa
-- en discusión es una tabla hija (id_solicitud, proveedor_id).
CREATE TABLE solicitud_entrega (
    id UUID PRIMARY KEY,
    id_donacion_independiente UUID NOT NULL REFERENCES donacion_independiente(id) ON DELETE RESTRICT,
    proveedor_actual TEXT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA')),
    ronda INTEGER NOT NULL CHECK (ronda >= 1),
    fecha_creacion TIMESTAMP NOT NULL,
    proveedores_descartados TEXT[] NOT NULL DEFAULT '{}'
);
CREATE INDEX idx_solicitud_entrega_donacion ON solicitud_entrega (id_donacion_independiente);
-- Una sola solicitud activa por donación: el broker ya lo controla; esto lo garantiza ante dos
-- pedidos simultáneos
CREATE UNIQUE INDEX uq_solicitud_entrega_activa
    ON solicitud_entrega (id_donacion_independiente) WHERE estado <> 'FALLIDA';

-- D6 del ADR de desnormalización: payload es el pedido completo, para reintentar exactamente lo que
-- se decidió. max_intentos no está en el DER: es la configuración vigente al crear la entrada.
CREATE TABLE outbox_logistica (
    id UUID PRIMARY KEY,
    id_solicitud_entrega UUID NOT NULL REFERENCES solicitud_entrega(id) ON DELETE CASCADE,
    proveedor_id TEXT NOT NULL,
    payload JSONB NOT NULL,
    trace_id TEXT NULL,
    proximo_intento TIMESTAMP NOT NULL,
    estado VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'PUBLICADO', 'FALLIDO')),
    intentos INTEGER NOT NULL CHECK (intentos >= 0),
    max_intentos INTEGER NOT NULL CHECK (max_intentos >= 1)
);
CREATE INDEX idx_outbox_logistica_solicitud ON outbox_logistica (id_solicitud_entrega);
-- El relay busca las pendientes cuyo próximo intento ya venció, cada pocos segundos
CREATE INDEX idx_outbox_logistica_pendientes
    ON outbox_logistica (proximo_intento) WHERE estado = 'PENDIENTE';

-- Idempotencia: un evento de Logística se aplica una sola vez por donación. fecha_procesamiento no
-- está en el DER. La restricción única es la que garantiza la idempotencia ante dos entregas
-- simultáneas del mismo mensaje.
CREATE TABLE evento_consumido (
    id UUID PRIMARY KEY,
    event_type TEXT NOT NULL,
    queue_name TEXT NULL,
    business_id UUID NULL,
    id_donacion_independiente UUID NULL REFERENCES donacion_independiente(id) ON DELETE RESTRICT,
    fecha_procesamiento TIMESTAMP NOT NULL,
    CONSTRAINT uq_evento_consumido UNIQUE NULLS NOT DISTINCT
        (event_type, business_id, id_donacion_independiente)
);
