-- HB-20 (ADR 20261102-nombre-visible-del-donante-y-constancia-de-los-legales, DTI-32).

-- Preferencia del donante: la entidad ve su nombre salvo que lo oculte.
ALTER TABLE donante ADD COLUMN mostrar_nombre BOOLEAN NOT NULL DEFAULT TRUE;

-- "Esta vez, sin mi nombre": solo oculta, nunca muestra lo que el perfil oculta.
ALTER TABLE donacion ADD COLUMN nombre_oculto BOOLEAN NOT NULL DEFAULT FALSE;

-- Constancia de la aceptación de los legales: persona, versión y fecha, sin IP ni navegador.
-- Sobrevive a la anonimización: la persona conserva su id.
CREATE TABLE aceptacion_legales (
    id UUID PRIMARY KEY,
    persona_id UUID NOT NULL REFERENCES persona(id) ON DELETE RESTRICT,
    version INTEGER NOT NULL,
    aceptada_en TIMESTAMP NOT NULL,
    CONSTRAINT uq_aceptacion_legales_persona_version UNIQUE (persona_id, version)
);
