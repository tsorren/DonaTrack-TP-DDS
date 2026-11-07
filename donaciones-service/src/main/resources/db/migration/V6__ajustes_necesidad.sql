-- Paso 5: ajustes de persistencia de Necesidad.

-- Bloqueo optimista: el scheduler que renueva períodos y la API pueden guardar la misma necesidad.
ALTER TABLE necesidad ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- El período pasa de días a ISO-8601 (P1M, P1W, P1Y2M): guardarlo en días convertía un mes en
-- 30 días y perdía los años. Los valores viejos se conservan como días (P30D): no se puede saber
-- si eran meses.
ALTER TABLE necesidad ALTER COLUMN periodo TYPE VARCHAR(20)
    USING CASE WHEN periodo IS NULL THEN NULL ELSE 'P' || periodo || 'D' END;

-- Una necesidad sin subcategoría no tiene sentido: borrar una subcategoría en uso se rechaza
-- (el servicio responde 409 antes; la FK es la red de seguridad). El índice sirve a la FK y a
-- existsBySubcategoriaId.
ALTER TABLE necesidad ADD CONSTRAINT fk_necesidad_subcategoria
    FOREIGN KEY (subcategoria_id) REFERENCES subcategoria (id) ON DELETE RESTRICT;
CREATE INDEX idx_necesidad_subcategoria ON necesidad (subcategoria_id);
