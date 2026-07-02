package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import grupo5.donaciones.models.entities.personas.Juridica;
import grupo5.donaciones.models.entities.personas.Persona;
import grupo5.donaciones.models.repositories.IPersonasRepository;
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

    JuridicaOutputDTO juridicaOutput = null;
    if (entidad.juridicaId() != null) {
      Persona persona = personasRepository.findById(entidad.juridicaId()).orElse(null);
      if (persona instanceof Juridica j) {
        juridicaOutput = (JuridicaOutputDTO) personaMapper.toOutputDTO(j);
      }
    }

    return new EntidadBeneficiariaOutputDTO(entidad.getId(), juridicaOutput);
  }
}
