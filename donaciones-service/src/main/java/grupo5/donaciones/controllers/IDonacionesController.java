package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donaciones.inputs.DonacionInputDTO;
import grupo5.donaciones.dto.donaciones.outputs.DonacionOutputDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IDonacionesController {
  ResponseEntity<DonacionOutputDTO> cargarDonacion(DonacionInputDTO dto);

  ResponseEntity<List<DonacionOutputDTO>> listarDonaciones();

  ResponseEntity<DonacionOutputDTO> obtenerDonacion(UUID id);
}
