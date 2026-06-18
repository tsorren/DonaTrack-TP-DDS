package grupo5.notificaciones.services.mappers;

import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.models.entities.personas.Persona;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

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
}
