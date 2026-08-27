package grupo5.logistica.dto.entregas;

import grupo5.logistica.models.entities.entregas.EstadoEntrega;

public record CambioEstadoEntregaRequestDTO(
    EstadoEntrega estado,
    String actor,
    String justificacion, // Utilizado cuando el estado es NO_RECIBIDA
    Boolean replanificable // Utilizado cuando el estado es NO_RECIBIDA
    ) {}
