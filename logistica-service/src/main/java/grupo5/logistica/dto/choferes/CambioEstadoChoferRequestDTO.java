package grupo5.logistica.dto.choferes;

import grupo5.logistica.models.entities.choferes.EstadoChofer;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoChoferRequestDTO(@NotNull EstadoChofer estado, String motivo) {}
