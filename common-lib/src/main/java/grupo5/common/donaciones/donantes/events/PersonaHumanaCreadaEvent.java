package grupo5.common.donaciones.donantes.events;

import grupo5.common.donaciones.shared.DireccionDTO;
import grupo5.common.donaciones.shared.MedioContactoDTO;
import grupo5.common.events.DomainEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record PersonaHumanaCreadaEvent(
    UUID eventId,
    UUID aggregateId, // personaId
    LocalDateTime timestamp,
    String nombre,
    String apellido,
    LocalDate fechaNacimiento,
    String genero, // "HOMBRE", "MUJER", "NO_BINARIO", "PREFIERO_NO_DECIR"
    String tipoDocumento, // "DNI", "LC", "LE", "PASAPORTE"
    String nroDocumento,
    DireccionDTO direccion,
    List<MedioContactoDTO> mediosContacto)
    implements DomainEvent {}
