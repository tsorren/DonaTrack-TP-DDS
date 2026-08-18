package grupo5.logistica.dto.choferes;

import grupo5.logistica.models.entities.choferes.EstadoChofer;

public record CambioEstadoChoferRequestDTO(EstadoChofer estado, String motivo) {}
