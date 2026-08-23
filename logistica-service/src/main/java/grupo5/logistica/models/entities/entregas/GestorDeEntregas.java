package grupo5.logistica.models.entities.entregas;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;

public final class GestorDeEntregas {

  private GestorDeEntregas() {}

  public static void cambiarEstado(SolicitudTransicionEntrega solicitud) {
    if (solicitud == null || solicitud.entrega() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }

    if (solicitud instanceof ConfirmacionRecepcion confirmacion) {
      confirmacion.entrega().confirmarEntrega(confirmacion.actor());
      if (confirmacion.fotoRecepcionUrl() != null && !confirmacion.fotoRecepcionUrl().isBlank()) {
        confirmacion.entrega().adjuntarFotoRecepcion(confirmacion.fotoRecepcionUrl());
      }
      return;
    }

    if (solicitud instanceof NoRecepcion noRecepcion) {
      noRecepcion.entrega().negarEntrega(noRecepcion.actor());
      return;
    }

    RegresoDeposito regreso = (RegresoDeposito) solicitud;
    regreso.entrega().regresarAlDeposito(regreso.actor());
  }
}
