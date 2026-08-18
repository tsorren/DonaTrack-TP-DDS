package grupo5.donaciones.services;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import java.util.List;
import java.util.UUID;

public interface IEntidadBeneficiariaService {
  EntidadBeneficiariaOutputDTO crearEntidad(EntidadBeneficiariaInputDTO input);

  EntidadBeneficiariaOutputDTO obtenerEntidad(UUID id);

  List<EntidadBeneficiariaOutputDTO> obtenerTodas();
}
