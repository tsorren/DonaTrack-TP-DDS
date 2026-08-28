package grupo5.donaciones.services;

import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import java.util.List;
import java.util.UUID;

public interface IDonacionesIndependientesService {
  List<DonacionIndependienteResponseDTO> obtenerTodas();

  DonacionIndependienteResponseDTO obtener(UUID id);

  DonacionIndependienteResponseDTO cambiarEstado(
      UUID id, CambioEstadoDonacionIndependienteRequestDTO request, String actor);
}
