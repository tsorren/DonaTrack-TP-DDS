package grupo5.notificaciones.models.entities.notificaciones.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Se dispara cuando una {@code Notificacion} se crea y queda en estado PENDIENTE, lista para
 * intentar su envío. {@code personaId} puede ser {@code null} si la notificación se creó sin
 * destinatario (ver {@code Notificacion.notificar()}, que ante persona nula pasa directo a FALLIDA
 * sin haber podido registrar antes un destinatario real).
 */
public record NotificacionCreada(UUID notificacionId, UUID personaId, LocalDateTime fecha)
    implements NotificacionDomainEvent {}
