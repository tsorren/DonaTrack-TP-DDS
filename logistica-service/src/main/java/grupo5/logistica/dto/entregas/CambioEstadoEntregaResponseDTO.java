package grupo5.logistica.dto.entregas;

import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.time.LocalDateTime;

public record CambioEstadoEntregaResponseDTO(
    EstadoEntrega estadoAnterior,
    EstadoEntrega estadoNuevo,
    LocalDateTime timestamp,
    String actor) {}
