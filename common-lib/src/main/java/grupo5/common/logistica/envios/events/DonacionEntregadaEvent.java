package grupo5.common.logistica.envios.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DonacionEntregadaEvent(
    UUID eventId,
    UUID aggregateId, // donacionId
    LocalDateTime timestamp,
    List<String> urlsFotosRecepcion,
    String receptorNombre,
    LocalDateTime fechaRecepcion)
    implements DomainEvent {}
