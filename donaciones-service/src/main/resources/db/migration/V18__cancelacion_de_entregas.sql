-- HB-19 (ADR 20261109-cancelacion-de-entregas-que-no-salieron, DTI-31): al dar de baja una
-- necesidad se cancelan las entregas que todavía no salieron. V18 porque V14 a V16 están en PR
-- abiertos (#951, #954 y #956) y V17 es HB-18 (#957).

-- La donación espera la confirmación de Logística con esta marca.
ALTER TABLE donacion_independiente
    ADD COLUMN cancelacion_pedida BOOLEAN NOT NULL DEFAULT FALSE;

-- Una solicitud cancelada se cierra, así la donación puede pedir otra entrega si se reasigna.
ALTER TABLE solicitud_entrega DROP CONSTRAINT solicitud_entrega_estado_check;
ALTER TABLE solicitud_entrega ADD CONSTRAINT solicitud_entrega_estado_check
    CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA', 'CANCELADA'));
DROP INDEX uq_solicitud_entrega_activa;
CREATE UNIQUE INDEX uq_solicitud_entrega_activa
    ON solicitud_entrega (id_donacion_independiente) WHERE estado NOT IN ('FALLIDA', 'CANCELADA');

-- El outbox del broker lleva dos comandos: el pedido de entrega y su cancelación.
ALTER TABLE outbox_logistica
    ADD COLUMN tipo VARCHAR(20) NOT NULL DEFAULT 'SOLICITUD'
        CHECK (tipo IN ('SOLICITUD', 'CANCELACION'));
