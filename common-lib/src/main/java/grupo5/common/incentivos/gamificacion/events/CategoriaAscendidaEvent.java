package grupo5.common.incentivos.gamificacion.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record CategoriaAscendidaEvent(
    UUID eventId,
    UUID aggregateId, // donanteId
    LocalDateTime timestamp,
    String categoriaAnterior,
    String categoriaNueva)
    implements DomainEvent {}
