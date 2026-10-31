-- El importador busca por número de documento aunque la fila no traiga el tipo. uq_persona_documento
-- (V4) empieza por tipo_documento, así que no sirve para buscar solo por número.
CREATE INDEX ix_persona_nro_documento ON persona (nro_documento);
