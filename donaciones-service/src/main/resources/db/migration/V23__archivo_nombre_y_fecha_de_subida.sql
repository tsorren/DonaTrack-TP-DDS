-- R11-20 (p1-importaciones): la lista de importaciones busca por el nombre con que se subió el
-- archivo y filtra y ordena por cuándo se subió. Las importaciones anteriores quedan en NULL en las
-- dos columnas: no se inventa un dato (van al final del orden y no entran en un filtro de fecha).

-- El nombre con que se subió, sin la ruta del navegador, recortado a 255 caracteres. La clave del
-- almacenamiento sigue en path.
ALTER TABLE archivo ADD COLUMN nombre_original VARCHAR(255) NULL;

-- Fecha y hora de la subida, en la hora de Buenos Aires (el Clock del servicio), como fecha_alta.
ALTER TABLE archivo ADD COLUMN subido_en TIMESTAMP NULL;

CREATE INDEX ix_archivo_subido_en ON archivo (subido_en DESC NULLS LAST, id);
