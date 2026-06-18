package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;
import org.springframework.stereotype.Component;

@Component
public class DonanteMapper {

  private final PersonaMapper personaMapper;

  public DonanteMapper(PersonaMapper personaMapper) {
    this.personaMapper = personaMapper;
  }

  public Donante toEntity(DonanteInputDTO dto) {
    if (dto == null) return null;

    Persona persona = personaMapper.toEntity(dto.getPersona());

    return new Donante(persona);
  }

  public DonanteOutputDTO toOutputDTO(Donante entity) {
    if (entity == null) return null;

    DonanteOutputDTO dto = new DonanteOutputDTO();
    dto.setId(entity.getId());

    dto.setCanalContacto(dto.getCanalContacto());

    if (entity.getPersona() != null) {
      dto.setPersona(personaMapper.toOutputDTO(entity.getPersona()));
    }

    return dto;
  }

  public void updateEntity(Donante entity, DonanteInputDTO dto) {
    if (entity == null || dto == null) return;

    if (entity.getPersona() != null && dto.getPersona() != null) {
      personaMapper.updateEntity(entity.getPersona(), dto.getPersona());
    }
  }
}
