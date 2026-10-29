-- ============================================================================
-- V3: OUTBOX DE AVISOS A NOTIFICACIONES (Transactional Outbox)
-- ============================================================================
-- Cada entrada es un aviso (misión cumplida, ascenso de categoría, donante inactivo) que se
-- publica en RabbitMQ.
--
-- La entrada se inserta en la misma transacción que el cambio que la origina (el guardado del
-- donante), así que o quedan las dos cosas o ninguna. Un relay programado publica las pendientes y
-- las marca. Con id como messageId de AMQP, Notificaciones descarta las repeticiones (su inbox).
--
-- payload es el evento tal como se publica (JSON). max_intentos es la configuración vigente al
-- crear la entrada.
CREATE TABLE outbox_incentivos (
    id              UUID        PRIMARY KEY,
    routing_key     TEXT        NOT NULL,
    payload         JSONB       NOT NULL,
    fecha_creacion  TIMESTAMP   NOT NULL,
    proximo_intento TIMESTAMP   NOT NULL,
    estado          VARCHAR(20) NOT NULL CHECK (estado IN ('PENDIENTE', 'PUBLICADO', 'FALLIDO')),
    intentos        INTEGER     NOT NULL CHECK (intentos >= 0),
    max_intentos    INTEGER     NOT NULL CHECK (max_intentos >= 1)
);

-- El relay busca las pendientes cuyo próximo intento ya venció, cada pocos segundos
CREATE INDEX idx_outbox_incentivos_pendientes
    ON outbox_incentivos (proximo_intento) WHERE estado = 'PENDIENTE';
