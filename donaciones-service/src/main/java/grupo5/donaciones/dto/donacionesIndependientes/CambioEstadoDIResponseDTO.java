package grupo5.donaciones.dto.donacionesIndependientes;

import java.time.LocalDateTime;

public record CambioEstadoDIResponseDTO(
    String estadoAnterior,
    String estadoNuevo,
    LocalDateTime timestamp,
    String justificacion,
    String actor) {}
