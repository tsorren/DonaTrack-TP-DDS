package grupo5.common.donaciones.donantes.events;

import grupo5.common.donaciones.shared.DireccionDTO;
import grupo5.common.donaciones.shared.MedioContactoDTO;
import grupo5.common.events.DomainEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PersonaHumanaModificadaEvent(
    UUID eventId,
    UUID aggregateId, // personaId
    LocalDateTime timestamp,
    String nombre,
    String apellido,
    LocalDate fechaNacimiento,
    String genero,
    String tipoDocumento,
    String nroDocumento,
    DireccionDTO direccion,
    List<MedioContactoDTO> mediosContacto)
    implements DomainEvent {}
