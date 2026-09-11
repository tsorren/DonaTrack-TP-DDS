package grupo5.logistica.models.entities.entregas;

public record ConfirmacionRecepcion(Entrega entrega, String actor, String fotoRecepcionUrl)
    implements SolicitudTransicionEntrega {}
