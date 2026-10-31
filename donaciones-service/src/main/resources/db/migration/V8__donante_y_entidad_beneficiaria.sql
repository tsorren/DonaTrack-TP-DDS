-- Paso 4: roles Donante y EntidadBeneficiaria (#892). El id del rol es el de su persona.
-- activo no figura en el DER: es la baja lógica del rediseño de roles (ajuste aditivo).
-- RESTRICT: una persona con roles no se borra; la app la anonimiza y da de baja sus roles.

CREATE TABLE donante (
    id UUID PRIMARY KEY REFERENCES persona(id) ON DELETE RESTRICT,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- Referencia a juridica, no a persona: la base impide que una humana sea entidad beneficiaria
CREATE TABLE entidad_beneficiaria (
    id UUID PRIMARY KEY REFERENCES juridica(id) ON DELETE RESTRICT,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- La necesidad siempre pertenece a una entidad (obligatorio en el DER). Falla si una base ya
-- tiene necesidades sin entidad: hay que limpiarlas antes de migrar.
ALTER TABLE necesidad ALTER COLUMN entidad_beneficiaria_id SET NOT NULL;
ALTER TABLE necesidad ADD CONSTRAINT fk_necesidad_entidad_beneficiaria
    FOREIGN KEY (entidad_beneficiaria_id) REFERENCES entidad_beneficiaria (id) ON DELETE RESTRICT;
