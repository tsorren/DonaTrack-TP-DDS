package grupo5.logistica.models.entities.rutas;

import java.time.LocalDateTime;

public record CambioEstadoRuta(
    EstadoRuta estadoAnterior, EstadoRuta estadoNuevo, LocalDateTime timestamp) {}
