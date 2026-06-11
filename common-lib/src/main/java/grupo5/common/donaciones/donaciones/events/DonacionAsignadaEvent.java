package grupo5.common.donaciones.donaciones.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record DonacionAsignadaEvent(
    UUID eventId,
    UUID aggregateId, // donacionId
    LocalDateTime timestamp,
    UUID entidadId, // Referencia por ID
    UUID donanteId // Referencia por ID
    ) implements DomainEvent {}
