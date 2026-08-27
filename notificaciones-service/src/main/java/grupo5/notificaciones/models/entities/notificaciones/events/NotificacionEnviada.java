package grupo5.notificaciones.models.entities.notificaciones.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Se dispara cuando una {@code Notificacion} logra enviarse por alguno de sus medios de contacto.
 */
public record NotificacionEnviada(UUID notificacionId, LocalDateTime fecha)
    implements NotificacionDomainEvent {}
