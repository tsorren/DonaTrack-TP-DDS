package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donantes.DonanteInputDTO;
import grupo5.donaciones.dto.donantes.DonanteOutputDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IDonantesController {
  ResponseEntity<DonanteOutputDTO> crearDonante(DonanteInputDTO dto);

  ResponseEntity<List<DonanteOutputDTO>> listarDonantes(String canal);

  ResponseEntity<DonanteOutputDTO> obtenerDonante(UUID id);

  ResponseEntity<Void> eliminarDonante(UUID id);
}
