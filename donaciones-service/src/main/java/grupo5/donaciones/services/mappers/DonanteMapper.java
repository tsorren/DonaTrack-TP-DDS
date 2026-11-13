package grupo5.donaciones.services.mappers;

import grupo5.common.exceptions.RecursoNoEncontradoException;
import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
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

    if (!personasRepository.existsById(dto.idPersona())) {
      throw new RecursoNoEncontradoException(dto.idPersona());
    }

    return new Donante(dto.idPersona());
  }

  public DonanteOutputDTO toOutputDTO(Donante entity) {
    if (entity == null) return null;

    Persona persona =
        entity.personaId() == null
            ? null
            : personasRepository.findById(entity.personaId()).orElse(null);
    return toOutputDTO(entity, persona);
  }

  /** Una página de donantes: sus personas se leen juntas, no una por fila (R11-01). */
  public List<DonanteOutputDTO> toOutputDTOs(List<Donante> donantes) {
    Map<UUID, Persona> personas =
        personasRepository.findAllById(donantes.stream().map(Donante::personaId).toList()).stream()
            .collect(Collectors.toMap(Persona::getId, Function.identity(), (a, b) -> a));
    return donantes.stream().map(d -> toOutputDTO(d, personas.get(d.personaId()))).toList();
  }

  private DonanteOutputDTO toOutputDTO(Donante entity, Persona persona) {
    PersonaOutputDTO personaOutput = persona == null ? null : personaMapper.toOutputDTO(persona);
    return new DonanteOutputDTO(
        entity.getId(),
        personaOutput,
        entity.estaActivo(),
        entity.muestraNombre(),
        entity.getFechaAlta());
  }

  public void updateEntity(Donante entity, DonanteInputDTO dto) {
    // Como Donante es inmutable con respecto a personaId por diseño DDD, no mutamos directamente,
    // o si lo hacemos, reasignamos el campo final/creamos nuevo.
    // Pero si queremos dar soporte a updateEntity, podemos permitir cambiar personaId si es
    // necesario,
    // pero en el record/clase actual personaId es final. Por tanto, no hacemos nada o lanzamos
    // excepción si cambia.
  }
}
