package grupo5.common.donaciones.entidades.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record NecesidadRegistradaEvent(
    UUID eventId,
    UUID aggregateId, // necesidadId
    LocalDateTime timestamp,
    UUID entidadId, // Referencia por ID a EntidadBeneficiaria
    String subcategoria,
    int cantidadNecesitada,
    String tipoNecesidad // "RECURRENTE" o "EXTRAORDINARIA"
    ) implements DomainEvent {}
