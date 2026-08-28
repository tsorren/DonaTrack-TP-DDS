package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IDonacionesIndependientesController {
  ResponseEntity<List<DonacionIndependienteResponseDTO>> obtenerTodas();

  ResponseEntity<DonacionIndependienteResponseDTO> obtener(UUID id);

  ResponseEntity<DonacionIndependienteResponseDTO> cambiarEstado(
      UUID id, CambioEstadoDonacionIndependienteRequestDTO request, String actor);
}
