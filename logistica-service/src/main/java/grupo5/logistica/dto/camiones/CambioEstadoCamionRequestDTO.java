package grupo5.logistica.dto.camiones;

import grupo5.logistica.models.entities.camiones.EstadoCamion;

public record CambioEstadoCamionRequestDTO(EstadoCamion estado, String motivo) {}
