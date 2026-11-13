-- V8__entrega_cancelada.sql
-- HB-19 (ADR 20261109-cancelacion-de-entregas-que-no-salieron): una entrega que no salió se
-- cancela cuando su necesidad se da de baja. CANCELADA es un estado final y queda como historia.
-- D7 pasa de "una entrega por donación" a "una entrega activa por donación": la donación cancelada
-- puede volver a entregarse si el algoritmo la reasigna.
-- V8 porque V5, V6 y V7 están en PR abiertos (#949, #950 y #951).

ALTER TYPE estado_entrega ADD VALUE IF NOT EXISTS 'CANCELADA';

ALTER TABLE entrega DROP CONSTRAINT uq_entrega_donacion;

-- El predicado enumera los estados activos en lugar de excluir CANCELADA: Postgres no deja usar un
-- valor de enum recién agregado en la misma transacción, y un cast a texto no es IMMUTABLE. Es el
-- mismo filtro que el índice parcial de Mongo (EntregaDocument). Un estado activo nuevo se suma acá.
CREATE UNIQUE INDEX uq_entrega_donacion_activa
    ON entrega (id_donacion)
    WHERE estado IN ('PENDIENTE', 'EN_TRASLADO', 'ENTREGADA', 'NO_RECIBIDA', 'REVISION');
