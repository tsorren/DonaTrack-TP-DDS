-- R11-24 (p1-asignaciones): qué algoritmo sugirió cada propuesta y en qué puesto de su lista de esa
-- corrida (1 = la primera), de cuántas propuso ese algoritmo, y si las donaciones de la propuesta son
-- las que él eligió (cuando los dos sugieren la misma necesidad, queda la del segundo). Una fila por
-- algoritmo. Las propuestas creadas antes de este cambio no tienen filas: la API dice sugeridaPor [].
CREATE TABLE sugerencia_propuesta (
    id_propuesta UUID NOT NULL REFERENCES propuesta(id) ON DELETE CASCADE,
    algoritmo VARCHAR(30) NOT NULL
        CHECK (algoritmo IN ('COMPATIBILIDAD_SEMANTICA', 'PRIORIDAD_SUBATENDIDOS')),
    puesto INTEGER NOT NULL CHECK (puesto >= 1),
    de_cuantas INTEGER NOT NULL,
    eligio_las_donaciones BOOLEAN NOT NULL,
    PRIMARY KEY (id_propuesta, algoritmo),
    CONSTRAINT ck_sugerencia_propuesta_puesto CHECK (de_cuantas >= puesto)
);
-- El filtro y el orden por algoritmo del listado de propuestas
CREATE INDEX idx_sugerencia_propuesta_algoritmo ON sugerencia_propuesta (algoritmo, puesto);

-- El listado de propuestas filtra por estado y ordena por fecha
CREATE INDEX idx_propuesta_estado_fecha ON propuesta (estado, fecha_creacion DESC);
