package grupo5.notificaciones.models.entities.notificaciones.events;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Se dispara cuando una {@code Notificacion} no pudo enviarse por ningún medio de contacto
 * disponible (o no tenía destinatario).
 */
public record NotificacionFallida(UUID notificacionId, LocalDateTime fecha)
    implements NotificacionDomainEvent {}
