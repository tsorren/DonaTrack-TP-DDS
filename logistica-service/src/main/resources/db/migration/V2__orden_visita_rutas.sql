-- V2__orden_visita_rutas.sql — Paradas de cada ruta en el orden que definió el planificador (la V1 ya corrió en Render).

-- Tabla asociativa de la revisión de diseño (mejora 2): la parada se conserva aunque la entrega vuelva al depósito.
CREATE TABLE parada_ruta (
    id_ruta      UUID NOT NULL REFERENCES ruta (id_ruta),
    orden_visita INT  NOT NULL,
    id_entrega   UUID NOT NULL REFERENCES entrega (id_entrega),
    PRIMARY KEY (id_ruta, orden_visita)
);

-- Rutas existentes: se cargan con el orden que se usaba hasta ahora (por id de entrega).
INSERT INTO parada_ruta (id_ruta, orden_visita, id_entrega)
SELECT id_ruta,
       ROW_NUMBER() OVER (PARTITION BY id_ruta ORDER BY id_entrega) - 1,
       id_entrega
FROM entrega
WHERE id_ruta IS NOT NULL;
