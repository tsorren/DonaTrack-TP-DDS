package grupo5.common.donaciones.donantes.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record DonanteActualizadoEvent(
    UUID eventId,
    UUID aggregateId, // donanteId
    LocalDateTime timestamp,
    UUID personaId,
    String tipoDonante,
    String canalPredeterminado,
    String whatsapp)
    implements DomainEvent {}
