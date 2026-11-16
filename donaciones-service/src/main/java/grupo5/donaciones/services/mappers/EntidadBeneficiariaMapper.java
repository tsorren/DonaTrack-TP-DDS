package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class EntidadBeneficiariaMapper {
  private final PersonaMapper personaMapper;
  private final IPersonasRepository personasRepository;

  public EntidadBeneficiariaMapper(
      PersonaMapper personaMapper, IPersonasRepository personasRepository) {
    this.personaMapper = personaMapper;
    this.personasRepository = personasRepository;
  }

  public EntidadBeneficiariaOutputDTO toOutputDTO(EntidadBeneficiaria entidad) {
    if (entidad == null) return null;

    Persona persona =
        entidad.juridicaId() == null
            ? null
            : personasRepository.findById(entidad.juridicaId()).orElse(null);
    return toOutputDTO(entidad, persona);
  }

  /** Una página: las jurídicas se leen juntas, no una por fila (R11-01). */
  public List<EntidadBeneficiariaOutputDTO> toOutputDTOs(List<EntidadBeneficiaria> entidades) {
    Map<UUID, Persona> personas =
        personasRepository
            .findAllById(entidades.stream().map(EntidadBeneficiaria::juridicaId).toList())
            .stream()
            .collect(Collectors.toMap(Persona::getId, Function.identity(), (a, b) -> a));
    return entidades.stream().map(e -> toOutputDTO(e, personas.get(e.juridicaId()))).toList();
  }

  private EntidadBeneficiariaOutputDTO toOutputDTO(EntidadBeneficiaria entidad, Persona persona) {
    JuridicaOutputDTO juridicaOutput = null;
    if (persona instanceof Juridica j) {
      juridicaOutput = (JuridicaOutputDTO) personaMapper.toOutputDTO(j);
    }
    return new EntidadBeneficiariaOutputDTO(entidad.getId(), juridicaOutput, entidad.estaActivo());
  }
}
