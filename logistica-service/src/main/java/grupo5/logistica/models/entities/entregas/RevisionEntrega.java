package grupo5.logistica.models.entities.entregas;

public record RevisionEntrega(Entrega entrega, String actor)
    implements SolicitudTransicionEntrega {}
