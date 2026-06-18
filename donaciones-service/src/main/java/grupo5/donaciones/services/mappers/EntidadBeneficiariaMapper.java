package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import grupo5.donaciones.models.entities.beneficiarios.EntidadBeneficiaria;
import org.springframework.stereotype.Component;

@Component
public class EntidadBeneficiariaMapper {
  private final PersonaMapper personaMapper;

  public EntidadBeneficiariaMapper(PersonaMapper personaMapper) {

    this.personaMapper = personaMapper;
  }

  public EntidadBeneficiariaOutputDTO toOutputDTO(EntidadBeneficiaria entidad) {

    return new EntidadBeneficiariaOutputDTO(
        entidad.getId(), (JuridicaOutputDTO) personaMapper.toOutputDTO(entidad.getJuridica()));
  }
}
