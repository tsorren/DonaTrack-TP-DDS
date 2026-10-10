package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaInputDTO;
import grupo5.donaciones.dto.entidadBeneficiaria.EntidadBeneficiariaOutputDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IEntidadBeneficiariaController {
  ResponseEntity<EntidadBeneficiariaOutputDTO> crearEntidad(EntidadBeneficiariaInputDTO entidad);

  ResponseEntity<EntidadBeneficiariaOutputDTO> obtenerEntidad(UUID id);

  ResponseEntity<List<EntidadBeneficiariaOutputDTO>> obtenerTodas();

  ResponseEntity<EntidadBeneficiariaOutputDTO> actualizarEntidad(
      UUID id, EntidadBeneficiariaInputDTO entidad);

  ResponseEntity<Void> eliminarEntidad(UUID id);
}
