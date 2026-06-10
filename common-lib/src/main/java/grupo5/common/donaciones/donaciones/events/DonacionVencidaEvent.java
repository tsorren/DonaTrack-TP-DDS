package grupo5.common.donaciones.donaciones.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DonacionVencidaEvent(
    UUID eventId,
    UUID aggregateId, // donacionId
    LocalDateTime timestamp,
    String subcategoria,
    int cantidad,
    LocalDate fechaVencimiento)
    implements DomainEvent {}
