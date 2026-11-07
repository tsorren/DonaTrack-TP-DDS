-- Paso 3c: el importador CSV busca duplicados por consulta en vez de cargar todas las personas.
-- Por documento ya alcanza con uq_persona_documento (V4, parcial sobre tipo y número).
-- Por correo se compara sin mayúsculas ni espacios en los extremos, igual que el dominio:
-- el índice es funcional sobre esa misma expresión. No es único, porque dos personas pueden
-- compartir un correo (por ejemplo, una jurídica y su representante).
CREATE INDEX ix_correo_direccion_normalizada ON correo (lower(btrim(direccion_correo)));
