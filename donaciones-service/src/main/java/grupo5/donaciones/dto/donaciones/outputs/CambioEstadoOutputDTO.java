package grupo5.donaciones.dto.donaciones.outputs;

import grupo5.donaciones.models.entities.donaciones.EstadoDonacion;
import java.time.LocalDateTime;

public record CambioEstadoOutputDTO(
    EstadoDonacion estadoAnterior, EstadoDonacion estadoNuevo, LocalDateTime timestamp) {}
