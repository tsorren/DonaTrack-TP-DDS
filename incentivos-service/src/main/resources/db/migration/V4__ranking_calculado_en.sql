-- Cuándo se calculó cada ranking mensual (RF-19: el admin ve la fecha de cálculo).
-- Puede ser nulo: los rankings guardados antes de esta migración no tienen el dato.
ALTER TABLE ranking_mensual ADD COLUMN calculado_en TIMESTAMP WITH TIME ZONE;
