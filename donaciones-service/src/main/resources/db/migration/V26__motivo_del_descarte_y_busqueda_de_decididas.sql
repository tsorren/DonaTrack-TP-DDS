-- R11-35 (p2-admin-bandeja): el motivo con que se descartó una propuesta se guarda (antes llegaba
-- en el pedido y se perdía). Lo escribe la administración, hasta 500 caracteres (lo valida la API).
-- Las descartadas antes de este cambio y las aprobadas quedan en NULL.
ALTER TABLE propuesta ADD COLUMN motivo_descarte TEXT NULL;

-- Todas las decididas se filtran por estado y se ordenan o acotan por la fecha de la decisión.
CREATE INDEX ix_propuesta_estado_fecha_decision ON propuesta (estado, fecha_decision);
