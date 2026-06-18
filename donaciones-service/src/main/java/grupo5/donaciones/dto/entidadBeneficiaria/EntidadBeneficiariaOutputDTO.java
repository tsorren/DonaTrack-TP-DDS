package grupo5.donaciones.dto.entidadBeneficiaria;

import grupo5.donaciones.dto.personas.JuridicaOutputDTO;
import java.util.UUID;

public record EntidadBeneficiariaOutputDTO(UUID id, JuridicaOutputDTO juridica) {}
