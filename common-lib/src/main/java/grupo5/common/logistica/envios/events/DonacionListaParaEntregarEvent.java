package grupo5.common.logistica.envios.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record DonacionListaParaEntregarEvent(
    UUID eventId,
    UUID aggregateId, // donacionId
    LocalDateTime timestamp,
    String descripcion)
    implements DomainEvent {}
