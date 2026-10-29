package grupo5.donaciones.models.entities.necesidades;

import java.util.UUID;

/**
 * A dónde se asigna una donación independiente, por identificador (DTI-06): la necesidad, el
 * período (solo en las recurrentes) y la entidad beneficiaria. Reemplaza a la interfaz {@code
 * Asignable}, que pasaba el objeto {@code Necesidad}.
 */
public record DestinoAsignacion(UUID necesidadId, UUID periodoId, UUID entidadBeneficiariaId) {}
