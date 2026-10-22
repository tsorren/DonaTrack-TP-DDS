package grupo5.logistica.dto.entregas;

import com.fasterxml.jackson.annotation.JsonFormat;
import grupo5.logistica.models.entities.entregas.EstadoEntrega;
import java.time.Instant;

public record CambioEstadoEntregaResponseDTO(
    EstadoEntrega estadoAnterior,
    EstadoEntrega estadoNuevo,
    @JsonFormat(shape = JsonFormat.Shape.STRING) Instant timestamp,
    String actor) {}
