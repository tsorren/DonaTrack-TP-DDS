package grupo5.notificaciones.services.mappers;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.MedioDeContactoReplicaDTO;
import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.dto.input.EventoPersonaSincronizadaV1;
import grupo5.notificaciones.dto.input.MedioDeContactoEventoDTO;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.entities.personas.TipoPersona;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PersonaMapper {

  private final MedioDeContactoMapper medioDeContactoMapper;

  public PersonaMapper(MedioDeContactoMapper medioDeContactoMapper) {
    this.medioDeContactoMapper = medioDeContactoMapper;
  }

  public Persona toEntity(PersonaReplicaDTO dto) {
    if (dto == null) {
      return null;
    }
    Persona persona =
        new Persona(dto.id(), new ArrayList<>(), dto.denominacion(), dto.tipoPersona());

    if (dto.mediosDeContacto() != null) {
      dto.mediosDeContacto().stream()
          .map(medioDeContactoMapper::toEntity)
          .forEach(persona::agregarMedioDeContacto);
    }
    return persona;
  }

  public PersonaReplicaDTO toReplicaDTO(Persona entity) {
    if (entity == null) {
      return null;
    }
    return new PersonaReplicaDTO(
        entity.getId(),
        entity.getDenominacion(),
        entity.getTipoPersona(),
        entity.getMediosDeContacto().stream().map(medioDeContactoMapper::toReplicaDTO).toList());
  }

  public PersonaReplicaDTO toReplicaDTO(EventoPersonaSincronizadaV1 evento) {
    if (evento == null) {
      return null;
    }
    TipoPersona tipoPersona;
    try {
      tipoPersona = TipoPersona.valueOf(evento.tipoPersona().trim().toUpperCase());
    } catch (IllegalArgumentException | NullPointerException e) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    List<MedioDeContactoReplicaDTO> medios =
        evento.mediosDeContacto() != null
            ? evento.mediosDeContacto().stream().map(PersonaMapper::toMedioReplicaDTO).toList()
            : List.of();
    return new PersonaReplicaDTO(evento.personaId(), evento.denominacion(), tipoPersona, medios);
  }

  private static MedioDeContactoReplicaDTO toMedioReplicaDTO(MedioDeContactoEventoDTO dto) {
    if (dto == null) {
      return null;
    }
    return new MedioDeContactoReplicaDTO(
        dto.tipo(),
        dto.esPredeterminado(),
        dto.direccionCorreo(),
        dto.caracteristica(),
        dto.codigoArea(),
        dto.numero());
  }
}
