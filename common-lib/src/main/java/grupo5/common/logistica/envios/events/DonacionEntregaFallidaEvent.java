package grupo5.common.logistica.envios.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record DonacionEntregaFallidaEvent(
    UUID eventId,
    UUID aggregateId, // donacionId
    LocalDateTime timestamp,
    String motivoFallo,
    boolean reintentable)
    implements DomainEvent {}
