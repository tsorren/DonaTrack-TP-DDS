package grupo5.logistica.models.entities.entregas;

public sealed interface SolicitudTransicionEntrega
    permits ConfirmacionRecepcion, NoRecepcion, RegresoDeposito, RevisionEntrega {

  Entrega entrega();

  String actor();
}
