package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.personas.SolicitudEntidadDTO;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface ISolicitudEntidadController {

  ResponseEntity<SolicitudEntidadDTO> consultarSolicitud(UUID id);

  ResponseEntity<SolicitudEntidadDTO> corregirSolicitud(UUID id, SolicitudEntidadDTO solicitud);
}
