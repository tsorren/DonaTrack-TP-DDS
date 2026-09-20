package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

public interface IDonacionesIndependientesController {
  ResponseEntity<List<DonacionIndependienteResponseDTO>> obtenerTodas(
      @RequestParam(required = false) TipoEstadoDonacion estado,
      @RequestParam(required = false) UUID subcategoriaId,
      @RequestParam(required = false) UUID donanteId);

  ResponseEntity<DonacionIndependienteResponseDTO> obtener(UUID id);

  ResponseEntity<DonacionIndependienteResponseDTO> cambiarEstado(
      UUID id, CambioEstadoDonacionIndependienteRequestDTO request, String actor);
}
