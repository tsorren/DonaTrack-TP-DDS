package grupo5.common.donaciones.donantes.events;

import grupo5.common.donaciones.shared.DireccionDTO;
import grupo5.common.donaciones.shared.MedioContactoDTO;
import grupo5.common.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PersonaJuridicaModificadaEvent(
    UUID eventId,
    UUID aggregateId, // personaId
    LocalDateTime timestamp,
    String razonSocial,
    String tipoJuridico,
    String rubro,
    String tipoDocumento,
    String nroDocumento,
    DireccionDTO direccion,
    List<MedioContactoDTO> mediosContacto,
    List<UUID> representanteIds)
    implements DomainEvent {}
