-- Revisión de la PR (A2 y A9): bloqueo optimista de la donación independiente y orden estable
-- de sus ítems. Ninguna de las dos columnas está en el DER: son ajustes aditivos.

-- A2: la versión sube en cada guardado. Dos guardados concurrentes de la misma donación (un
-- evento de Logística y una acción de la API, o dos aprobaciones) ya no se pisan: el segundo falla.
ALTER TABLE donacion_independiente ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- A9: el dominio consume los ítems en orden (fragmentarse toma el primero) y sin una columna de
-- orden la base los devolvía en cualquier orden. En las filas ya guardadas el orden se completa
-- con el de sus ids, que es arbitrario pero estable.
ALTER TABLE item_donacion_independiente ADD COLUMN orden INTEGER NOT NULL DEFAULT 0;
UPDATE item_donacion_independiente i SET orden = o.n - 1
FROM (SELECT id, row_number() OVER (PARTITION BY id_donacion_independiente ORDER BY id) AS n
      FROM item_donacion_independiente) o
WHERE i.id = o.id;
ALTER TABLE item_donacion_independiente ADD CONSTRAINT ck_item_donacion_independiente_orden
    CHECK (orden >= 0);
-- DEFERRABLE: al reordenar o reemplazar ítems en un mismo guardado, la unicidad se valida al commit
ALTER TABLE item_donacion_independiente ADD CONSTRAINT uq_item_donacion_independiente_orden
    UNIQUE (id_donacion_independiente, orden) DEFERRABLE INITIALLY DEFERRED;
