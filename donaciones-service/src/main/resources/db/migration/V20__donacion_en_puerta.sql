-- HB-22 (ADR logistica-service/20261111-estado-en-puerta-de-la-entrega): la donación que llegó a
-- la entidad espera su confirmación en EN_PUERTA. Los CHECK de V10 son inline: Postgres los nombró
-- <tabla>_<columna>_check.

ALTER TABLE donacion_independiente DROP CONSTRAINT donacion_independiente_estado_actual_check;
ALTER TABLE donacion_independiente ADD CONSTRAINT donacion_independiente_estado_actual_check
    CHECK (estado_actual IN ('EN_DEPOSITO', 'ASIGNACION_REALIZADA', 'LISTA_PARA_ENTREGAR',
        'EN_TRASLADO', 'EN_PUERTA', 'ENTREGADA', 'ENTREGA_FALLIDA', 'VENCIDA'));

ALTER TABLE estado_donacion_independiente DROP CONSTRAINT estado_donacion_independiente_tipo_check;
ALTER TABLE estado_donacion_independiente ADD CONSTRAINT estado_donacion_independiente_tipo_check
    CHECK (tipo IN ('EN_DEPOSITO', 'ASIGNACION_REALIZADA', 'LISTA_PARA_ENTREGAR', 'EN_TRASLADO',
        'EN_PUERTA', 'ENTREGADA', 'ENTREGA_FALLIDA', 'VENCIDA'));
