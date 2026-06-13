package grupo5.common.donaciones.entidades.events;

import grupo5.common.donaciones.shared.DireccionDTO;
import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EntidadBeneficiariaCreadaEvent(
    UUID eventId,
    UUID aggregateId, // entidadId
    LocalDateTime timestamp,
    String razonSocial,
    DireccionDTO direccion,
    String telefono,
    List<String> emailsRepresentantes)
    implements DomainEvent {}
