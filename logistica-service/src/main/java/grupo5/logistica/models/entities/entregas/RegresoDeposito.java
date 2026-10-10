package grupo5.logistica.models.entities.entregas;

public record RegresoDeposito(Entrega entrega, String actor)
    implements SolicitudTransicionEntrega {}
