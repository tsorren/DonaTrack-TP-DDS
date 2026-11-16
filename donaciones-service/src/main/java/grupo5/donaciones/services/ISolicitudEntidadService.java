package grupo5.donaciones.services;

import grupo5.donaciones.dto.personas.SolicitudEntidadDTO;
import java.util.UUID;

/**
 * La solicitud de una entidad que espera la aprobación (R11-41). Una jurídica está pendiente si no
 * es entidad beneficiaria ni donante y no está anonimizada; si no, las dos operaciones responden
 * 409 sin cambiar nada. Si la persona no existe, 404.
 */
public interface ISolicitudEntidadService {

  SolicitudEntidadDTO consultar(UUID personaId);

  /**
   * Cambia solo la razón social, el rubro, el tipo jurídico, la dirección (sin el país ni el piso)
   * y el nombre del primer representante; el documento y los medios de contacto no se tocan.
   */
  SolicitudEntidadDTO corregir(UUID personaId, SolicitudEntidadDTO solicitud);
}
