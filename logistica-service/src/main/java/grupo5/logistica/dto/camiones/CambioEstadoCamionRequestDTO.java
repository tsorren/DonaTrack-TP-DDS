package grupo5.logistica.dto.camiones;

import grupo5.logistica.models.entities.camiones.EstadoCamion;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoCamionRequestDTO(@NotNull EstadoCamion estado, String motivo) {}
