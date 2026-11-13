-- R11-01: los listados de Donaciones paginan, buscan y cuentan en la base.

-- Búsqueda sin tildes: "Gomez" encuentra "Gómez" (lower(unaccent(...)) en las consultas).
-- unaccent viene con la imagen oficial de PostgreSQL y es una extensión confiable: la puede crear
-- el dueño de la base. Va en el esquema del servicio y las consultas la llaman calificada, con su
-- diccionario (donaciones.unaccent('donaciones.unaccent', x)): así no dependen del search_path.
CREATE EXTENSION IF NOT EXISTS unaccent SCHEMA donaciones;

-- Orden "Más nuevos primero" del listado de donantes. Los que ya existen toman la hora de la
-- migración (aceptado al aprobar la spec).
ALTER TABLE donante ADD COLUMN fecha_alta TIMESTAMP NOT NULL DEFAULT now();
CREATE INDEX ix_donante_fecha_alta ON donante (fecha_alta DESC, id);

-- Orden y filtro por fecha de las donaciones independientes y de las originales.
CREATE INDEX ix_donacion_independiente_fecha ON donacion_independiente (fecha_registro DESC, id);
CREATE INDEX ix_donacion_fecha ON donacion (fecha DESC, id);

-- "Entregadas en el mes" del resumen del admin.
CREATE INDEX ix_cambio_estado_di_timestamp ON cambio_estado_donacion_independiente ("timestamp");

-- "Por vencer" del resumen del admin.
CREATE INDEX ix_item_normalizado_vencimiento ON item_donacion_normalizado (fecha_vencimiento)
    WHERE fecha_vencimiento IS NOT NULL;

-- Filtro tipoJuridico de donantes y personas.
CREATE INDEX ix_juridica_tipo ON juridica (tipo_juridico);

-- Los últimos 5 donantes procesados de un padrón, guardados con el avance (una línea por donante:
-- RESULTADO, tabulador y nombre visible).
ALTER TABLE archivo ADD COLUMN ultimos TEXT NULL;
