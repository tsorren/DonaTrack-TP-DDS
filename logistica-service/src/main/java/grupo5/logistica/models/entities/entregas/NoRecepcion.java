package grupo5.logistica.models.entities.entregas;

public record NoRecepcion(
    Entrega entrega, String actor, String justificacion, boolean replanificable)
    implements SolicitudTransicionEntrega {}
