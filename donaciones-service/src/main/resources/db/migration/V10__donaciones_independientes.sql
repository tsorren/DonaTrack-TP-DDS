-- Paso 8: donación independiente, sus ítems, sus estados y su historial (DER de Donaciones, con las
-- correcciones D-21, D-22 y D-24). Fechas como TIMESTAMP (LocalDateTime, como V1 y V9).

-- D2 del ADR de desnormalización: estado_actual repite el estado nuevo del último cambio.
-- D3: id_necesidad, id_periodo e id_beneficiario_asignado se escriben juntos al asignar y se limpian
-- juntos al volver al depósito. Las tres son opcionales; un período sin necesidad no tiene sentido.
CREATE TABLE donacion_independiente (
    id UUID PRIMARY KEY,
    id_donacion UUID NOT NULL REFERENCES donacion(id) ON DELETE RESTRICT,
    estado_actual VARCHAR(25) NOT NULL CHECK (estado_actual IN ('EN_DEPOSITO', 'ASIGNACION_REALIZADA',
        'LISTA_PARA_ENTREGAR', 'EN_TRASLADO', 'ENTREGADA', 'ENTREGA_FALLIDA', 'VENCIDA')),
    id_necesidad UUID NULL REFERENCES necesidad(id) ON DELETE RESTRICT,
    fecha_registro TIMESTAMP NOT NULL,
    id_beneficiario_asignado UUID NULL REFERENCES entidad_beneficiaria(id) ON DELETE RESTRICT,
    id_periodo UUID NULL REFERENCES periodo_necesidad(id) ON DELETE RESTRICT,
    CONSTRAINT ck_donacion_independiente_periodo_con_necesidad
        CHECK (id_periodo IS NULL OR id_necesidad IS NOT NULL)
);
CREATE INDEX idx_donacion_independiente_donacion ON donacion_independiente (id_donacion);
-- findEnDeposito, en cada corrida del algoritmo
CREATE INDEX idx_donacion_independiente_estado ON donacion_independiente (estado_actual);
-- Acumulado asignado de una necesidad o de un período
CREATE INDEX idx_donacion_independiente_destino ON donacion_independiente (id_necesidad, id_periodo);
-- Conteo por entidad del algoritmo de sub-atendidos y «donaciones asignadas a mi entidad» (UI 11)
CREATE INDEX idx_donacion_independiente_beneficiario ON donacion_independiente (id_beneficiario_asignado);

-- D-24: cada ítem apunta al ítem normalizado del que salió, de donde se reconstituye el bien
-- (subcategoría, confianza, medidas). D4: bien_descripcion es la foto del texto del bien.
-- Los ítems no tienen identidad en el dominio: se reemplazan en cada guardado.
CREATE TABLE item_donacion_independiente (
    id UUID PRIMARY KEY,
    id_donacion_independiente UUID NOT NULL REFERENCES donacion_independiente(id) ON DELETE CASCADE,
    id_item_normalizado UUID NOT NULL REFERENCES item_donacion_normalizado(id) ON DELETE RESTRICT,
    cantidad BIGINT NOT NULL CHECK (cantidad > 0),
    bien_descripcion TEXT NOT NULL
);
CREATE INDEX idx_item_donacion_independiente_donacion ON item_donacion_independiente (id_donacion_independiente);
CREATE INDEX idx_item_donacion_independiente_normalizado ON item_donacion_independiente (id_item_normalizado);

-- D-21: una fila por cada vez que la donación entra a un estado, con su tipo. patente_camion se
-- completa en ENTREGADA; replanificable_hasta queda del DER sin uso (el dominio no tiene plazo).
CREATE TABLE estado_donacion_independiente (
    id UUID PRIMARY KEY,
    id_donacion_independiente UUID NOT NULL REFERENCES donacion_independiente(id) ON DELETE CASCADE,
    tipo VARCHAR(25) NOT NULL CHECK (tipo IN ('EN_DEPOSITO', 'ASIGNACION_REALIZADA',
        'LISTA_PARA_ENTREGAR', 'EN_TRASLADO', 'ENTREGADA', 'ENTREGA_FALLIDA', 'VENCIDA')),
    patente_camion TEXT NULL,
    replanificable_hasta TIMESTAMP NULL
);
CREATE INDEX idx_estado_donacion_independiente_donacion ON estado_donacion_independiente (id_donacion_independiente);

-- Historial append-only. D-22: justificacion (obligatoria en el dominio para una entrega fallida).
-- El orden sale de encadenar anterior y nuevo: dos cambios seguidos pueden compartir el timestamp
-- (una falla replanificable registra dos transiciones en la misma operación). Las FK a los estados
-- se verifican al confirmar: el estado y el cambio se insertan (y se borran) en la misma transacción.
CREATE TABLE cambio_estado_donacion_independiente (
    id UUID PRIMARY KEY,
    id_donacion_independiente UUID NOT NULL REFERENCES donacion_independiente(id) ON DELETE CASCADE,
    id_estado_anterior UUID NULL REFERENCES estado_donacion_independiente(id)
        DEFERRABLE INITIALLY DEFERRED,
    id_estado_nuevo UUID NOT NULL REFERENCES estado_donacion_independiente(id)
        DEFERRABLE INITIALLY DEFERRED,
    "timestamp" TIMESTAMP NOT NULL,
    actor TEXT NULL,
    justificacion TEXT NULL
);
CREATE INDEX idx_cambio_estado_donacion_independiente_donacion
    ON cambio_estado_donacion_independiente (id_donacion_independiente);
