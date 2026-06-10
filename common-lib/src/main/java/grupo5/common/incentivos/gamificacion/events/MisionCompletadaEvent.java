package grupo5.common.incentivos.gamificacion.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record MisionCompletadaEvent(
    UUID eventId,
    UUID aggregateId, // donanteId
    LocalDateTime timestamp,
    UUID misionId,
    String misionNombre,
    int puntosOtorgados,
    String insigniaGanada)
    implements DomainEvent {}
