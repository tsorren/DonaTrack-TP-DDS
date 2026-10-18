package grupo5.donaciones.dto.propuestas;

import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import jakarta.validation.constraints.NotNull;

public record ActualizarEstadoRequestDTO(
    @NotNull(message = "El estado de la propuesta es obligatorio") EstadoPropuesta estado,
    String justificacion) {}
