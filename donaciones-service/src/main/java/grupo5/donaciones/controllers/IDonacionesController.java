package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import org.springframework.http.ResponseEntity;

public interface IDonacionesController {
  ResponseEntity<DonacionOutputDTO> cargarDonacion(DonacionInputDTO dto);
}
