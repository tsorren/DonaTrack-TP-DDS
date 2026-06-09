package grupo5.common.donaciones.donaciones.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DonacionRegistradaEvent(
    UUID eventId,
    UUID aggregateId, // cargaId
    LocalDateTime timestamp,
    UUID donanteId, // Referencia por ID
    String descripcion,
    List<ItemDonacionDTO> items)
    implements DomainEvent {}
