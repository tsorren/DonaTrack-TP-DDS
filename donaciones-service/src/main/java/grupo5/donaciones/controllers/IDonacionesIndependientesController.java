package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface IDonacionesIndependientesController {
  ResponseEntity<List<DonacionIndependienteResponseDTO>> obtenerTodas(
      FiltroDonacionesIndependientes filtro);

  ResponseEntity<DonacionIndependienteResponseDTO> obtener(UUID id);

  ResponseEntity<DonacionIndependienteResponseDTO> cambiarEstado(
      UUID id,
      CambioEstadoDonacionIndependienteRequestDTO request,
      String actorLegado,
      Authentication autenticacion);
}
