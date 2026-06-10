package grupo5.common.logistica.envios.events;

import grupo5.common.events.DomainEvent;
import grupo5.common.logistica.shared.CoordenadaDTO;
import java.time.LocalDateTime;
import java.util.UUID;

public record DonacionEnTrasladoEvent(
    UUID eventId,
    UUID aggregateId, // donacionId
    LocalDateTime timestamp,
    UUID rutaId,
    UUID camionId,
    CoordenadaDTO ubicacionActual,
    String estimadoArribo)
    implements DomainEvent {}
