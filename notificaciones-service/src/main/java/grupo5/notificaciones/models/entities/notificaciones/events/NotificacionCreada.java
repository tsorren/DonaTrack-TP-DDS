package grupo5.notificaciones.models.entities.notificaciones.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Se dispara cuando una {@code Notificacion} se crea y queda en estado PENDIENTE, lista para
 * intentar su envío. {@code personaId} puede ser {@code null} si la notificación se creó sin
 * destinatario (ver {@code Notificacion.notificar()}, que ante persona nula pasa directo a FALLIDA
 * sin haber podido registrar antes un destinatario real).
 *
 * <p>Antes de la Oleada 11 era un {@code record}; pasó a clase porque {@code
 * NotificacionDomainEvent} (y, transitivamente, {@code EventoDeDominio} de common-lib) es una clase
 * abstracta, y los records de Java no pueden extender clases. Conserva el mismo accesor estilo
 * record ({@code personaId()}) que ya usaban los tests, en vez de renombrarlo a {@code
 * getPersonaId()}.
 */
public class NotificacionCreada extends NotificacionDomainEvent {
  private final UUID personaId;

  public NotificacionCreada(UUID notificacionId, UUID personaId, LocalDateTime fecha) {
    super(notificacionId, fecha);
    this.personaId = personaId;
  }

  public UUID personaId() {
    return personaId;
  }
}
