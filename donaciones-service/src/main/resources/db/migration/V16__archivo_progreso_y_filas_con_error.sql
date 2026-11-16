-- HB-14: progreso del padrón de donantes y resultado de cada fila con error.
-- total_filas queda NULL hasta leer el padrón; procesadas = creadas + actualizadas + con_error.
-- Las filas con error guardan solo la línea, el motivo y el código: nunca datos de la persona.

ALTER TABLE archivo
    ADD COLUMN total_filas INTEGER,
    ADD COLUMN creadas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN actualizadas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN con_error INTEGER NOT NULL DEFAULT 0;

CREATE TABLE archivo_fila_error (
    id BIGSERIAL PRIMARY KEY,
    id_archivo UUID NOT NULL REFERENCES archivo(id) ON DELETE CASCADE,
    linea INTEGER NOT NULL,
    motivo VARCHAR(40) NOT NULL
        CHECK (motivo IN ('FORMATO_INVALIDO', 'DATOS_INVALIDOS', 'DONANTE_DADO_DE_BAJA',
                          'PERSONA_DUPLICADA_EN_CONFLICTO', 'ERROR_INESPERADO')),
    codigo VARCHAR(20)
);

CREATE INDEX idx_archivo_fila_error_archivo_linea ON archivo_fila_error(id_archivo, linea);
