package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import java.util.List;
import java.util.UUID;

public record PropuestaDTO(
    UUID id,
    EstadoPropuesta estado,
    NecesidadResumenDTO necesidad,
    List<FragmentacionDTO> fragmentaciones) {}
