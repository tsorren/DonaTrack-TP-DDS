package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.ConteosDeEntidadDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import grupo5.donaciones.services.ParametrosListado;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

public interface IDonacionesIndependientesController {
  ResponseEntity<List<DonacionIndependienteResponseDTO>> obtenerTodas(
      FiltroDonacionesIndependientes filtro, ParametrosListado listado);

  ResponseEntity<DonacionIndependienteResponseDTO> obtener(UUID id);

  ResponseEntity<ConteosDeEntidadDTO> conteos(UUID entidadId, UUID necesidadId);

  ResponseEntity<DonacionIndependienteResponseDTO> cambiarEstado(
      UUID id,
      CambioEstadoDonacionIndependienteRequestDTO request,
      String actorLegado,
      Authentication autenticacion);
}
