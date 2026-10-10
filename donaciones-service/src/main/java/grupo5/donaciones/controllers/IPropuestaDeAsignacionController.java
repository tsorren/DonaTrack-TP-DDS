package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IPropuestaDeAsignacionController {

  ResponseEntity<List<PropuestaDTO>> ejecutar();

  ResponseEntity<List<EjecucionAsignacionDTO>> historial();

  ResponseEntity<List<PropuestaDTO>> listar();

  ResponseEntity<Void> actualizarEstado(UUID id, ActualizarEstadoRequestDTO request);
}
