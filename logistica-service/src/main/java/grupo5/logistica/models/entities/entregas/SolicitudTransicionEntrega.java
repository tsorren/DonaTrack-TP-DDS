package grupo5.logistica.models.entities.entregas;

public sealed interface SolicitudTransicionEntrega
    permits ConfirmacionRecepcion, NoRecepcion, RegresoDeposito {

  Entrega entrega();

  String actor();
}
