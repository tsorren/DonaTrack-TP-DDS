package grupo5.donaciones.services;

import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import java.util.UUID;

public interface IDonacionesIndependientesService {
  DonacionIndependienteResponseDTO cambiarEstado(
      UUID id,
      CambioEstadoDonacionIndependienteRequestDTO request,
      String actor);
}