package grupo5.logistica.models.entities.entregas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;

public final class GestorDeEntregas {

  private GestorDeEntregas() {}

  public static void cambiarEstado(SolicitudTransicionEntrega solicitud) {
    if (solicitud == null || solicitud.entrega() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    // spotless:off
    switch (solicitud) {
      case ConfirmacionRecepcion(var entrega, var actor, var fotoRecepcionUrl) -> {
        entrega.confirmarEntrega(actor);
        if (fotoRecepcionUrl != null && !fotoRecepcionUrl.isBlank()) {
          entrega.adjuntarFotoRecepcion(fotoRecepcionUrl);
        }
      }
      case NoRecepcion(var entrega, var actor, var justificacion, var replanificable) ->
          entrega.negarEntrega(actor, justificacion, replanificable);
      case RegresoDeposito(var entrega, var actor) -> entrega.regresarAlDeposito(actor);
    }
    // spotless:on
  }
}
