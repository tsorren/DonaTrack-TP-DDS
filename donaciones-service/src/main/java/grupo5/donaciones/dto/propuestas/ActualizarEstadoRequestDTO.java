package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;

public record ActualizarEstadoRequestDTO(EstadoPropuesta estado, String justificacion) {}
