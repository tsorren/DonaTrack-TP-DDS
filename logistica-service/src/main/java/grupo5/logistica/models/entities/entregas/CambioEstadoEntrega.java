package grupo5.logistica.models.entities.entregas;

import java.time.LocalDateTime;

public record CambioEstadoEntrega(
    EstadoEntrega estadoAnterior,
    EstadoEntrega estadoNuevo,
    LocalDateTime timeStamp,
    String actor) {}
