package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.personas.AceptarLegalesDTO;
import grupo5.donaciones.dto.personas.LegalesDTO;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface ILegalesController {

  ResponseEntity<LegalesDTO> consultarLegales(UUID id);

  ResponseEntity<LegalesDTO> aceptarLegales(UUID id, AceptarLegalesDTO dto);
}
