package grupo5.donaciones.services.mappers;

import grupo5.common.exceptions.RecursoNoEncontradoException; // O tu excepción reglamentaria
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import org.springframework.stereotype.Component;

@Component
public class DonanteMapper {

  private final PersonaMapper personaMapper;
  private final IPersonasRepository personasRepository;

  public DonanteMapper(PersonaMapper personaMapper, IPersonasRepository personasRepository) {
    this.personaMapper = personaMapper;
    this.personasRepository = personasRepository;
  }

  public Donante toEntity(DonanteInputDTO dto) {
    if (dto == null || dto.idPersona() == null) return null;

    Persona persona =
        personasRepository
            .findById(dto.idPersona())
            .orElseThrow(() -> new RecursoNoEncontradoException(dto.idPersona()));

    return new Donante(persona);
  }

  public DonanteOutputDTO toOutputDTO(Donante entity) {
    if (entity == null) return null;

    PersonaOutputDTO personaOutput = null;
    if (entity.getPersona() != null) {
      personaOutput = personaMapper.toOutputDTO(entity.getPersona());
    }

    return new DonanteOutputDTO(entity.getId(), personaOutput);
  }

  public void updateEntity(Donante entity, DonanteInputDTO dto) {
    if (entity == null || dto == null || dto.idPersona() == null) return;

    if (entity.getPersona() == null || !entity.getPersona().getId().equals(dto.idPersona())) {
      Persona nuevaPersona =
          personasRepository
              .findById(dto.idPersona())
              .orElseThrow(() -> new RecursoNoEncontradoException(dto.idPersona()));
      entity.setPersona(nuevaPersona);
    }
  }
}
