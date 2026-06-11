package grupo5.common.donaciones.donantes.events;

import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.UUID;

public record DonanteCreadoEvent(
    UUID eventId,
    UUID aggregateId, // donanteId
    LocalDateTime timestamp,
    UUID personaId, // Referencia por ID
    String tipoDonante, // "HUMANA" o "JURIDICA"
    String canalPredeterminado, // "CORREO", "SMS", "WHATSAPP"
    String whatsapp)
    implements DomainEvent {}
