-- V9__entrega_en_puerta.sql
-- HB-22 (ADR 20261111-estado-en-puerta-de-la-entrega): el camión llegó al destino y la entidad
-- todavía no confirmó. EN_PUERTA es un estado activo: cuenta para "una entrega activa por donación".

ALTER TYPE estado_entrega ADD VALUE IF NOT EXISTS 'EN_PUERTA';

-- El índice de V8 enumera los estados activos. EN_PUERTA no se puede nombrar en esta misma
-- transacción (Postgres no deja usar un valor de enum recién agregado), pero CANCELADA ya está
-- aplicada desde V8: el predicado pasa a excluir el único estado que no es activo.
DROP INDEX uq_entrega_donacion_activa;

CREATE UNIQUE INDEX uq_entrega_donacion_activa
    ON entrega (id_donacion)
    WHERE estado <> 'CANCELADA';
