package grupo5.notificaciones.models.entities.notificaciones.eventos;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import java.time.LocalDateTime;

public class CambioEstadoNotificacion {
  private final EstadoNotificacion estadoAnterior;
  private final EstadoNotificacion estadoNuevo;
  private final LocalDateTime timestamp;

  public CambioEstadoNotificacion(
      EstadoNotificacion estadoAnterior, EstadoNotificacion estadoNuevo, LocalDateTime timestamp) {
    this.estadoAnterior = estadoAnterior;
    this.estadoNuevo = estadoNuevo;
    this.timestamp = timestamp;
  }
}
