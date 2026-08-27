package grupo5.logistica.models.entities.choferes;

import java.time.LocalDateTime;

public record CambioEstadoChofer(
    EstadoChofer estadoAnterior, EstadoChofer estadoNuevo, LocalDateTime timestamp) {}
