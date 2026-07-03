package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface IAsignacionController {
  ResponseEntity<List<PropuestaDTO>> ejecutar();

  ResponseEntity<List<EjecucionAsignacionDTO>> historial();
}
