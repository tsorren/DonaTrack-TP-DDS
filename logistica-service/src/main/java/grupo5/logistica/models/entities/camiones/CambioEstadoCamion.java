package grupo5.logistica.models.entities.camiones;

import java.time.LocalDateTime;

public record CambioEstadoCamion(
    EstadoCamion estadoAnterior, EstadoCamion estadoNuevo, LocalDateTime timestamp) {}
