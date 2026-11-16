-- HB-18 (ADR 20261102 del ciclo de vida de la necesidad, DTI-30). Todas las columnas admiten nulos:
-- las filas ya guardadas no se completan (ajuste del 2026-11-09 del ADR).

-- Punto 3: hasta cuándo sirve una extraordinaria. La API la exige al crearla; las anteriores
-- quedan sin fecha y siguen vigentes.
ALTER TABLE necesidad ADD COLUMN fecha_limite DATE NULL;

-- Punto 5: el último cambio de cantidad, para el cartel de la entidad.
ALTER TABLE necesidad ADD COLUMN cantidad_anterior BIGINT NULL
    CHECK (cantidad_anterior IS NULL OR cantidad_anterior > 0);
ALTER TABLE necesidad ADD COLUMN fecha_ultimo_cambio DATE NULL;

-- Punto 7: la unidad propia de la subcategoría; nula, hereda la de su categoría.
ALTER TABLE subcategoria ADD COLUMN unidad VARCHAR(20) NULL
    CHECK (unidad IS NULL
        OR unidad IN ('UNIDADES', 'KILOGRAMO', 'LITROS', 'METRO', 'METRO_CUADRADO', 'METRO_CUBICO'));
