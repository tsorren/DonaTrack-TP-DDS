package grupo5.common.notificaciones.alertas.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record NotificacionFallidaEvent(
    UUID eventId,
    UUID aggregateId, // notificacionId
    LocalDateTime timestamp,
    UUID usuarioId, // Referencia por ID
    String canalFallido,
    String motivoError)
    implements DomainEvent {}
