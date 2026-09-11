package grupo5.notificaciones.models.entities.notificaciones.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Se dispara cuando una {@code Notificacion} no pudo enviarse por ningún medio de contacto
 * disponible (o no tenía destinatario).
 *
 * <p>Antes de la Oleada 11 era un {@code record}; pasó a clase porque {@code
 * NotificacionDomainEvent} extiende la clase abstracta {@code EventoDeDominio} de common-lib, y los
 * records no pueden extender clases (ver {@code NotificacionCreada} para el detalle completo).
 */
public class NotificacionFallida extends NotificacionDomainEvent {
  public NotificacionFallida(UUID notificacionId, LocalDateTime fecha) {
    super(notificacionId, fecha);
  }
}
