package grupo5.common.incentivos.analiticas.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record RankingMensualCalculadoEvent(
    UUID eventId,
    UUID aggregateId, // rankingId
    LocalDateTime timestamp,
    int mes,
    int anio,
    List<DonanteRankingDTO> topDonantes)
    implements DomainEvent {}
