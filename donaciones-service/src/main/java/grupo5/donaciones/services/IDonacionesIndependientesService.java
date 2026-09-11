package grupo5.donaciones.services;

import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.models.entities.donacionesIndependientes.TipoEstadoDonacion;
import java.util.List;
import java.util.UUID;

public interface IDonacionesIndependientesService {
  List<DonacionIndependienteResponseDTO> obtenerTodas();

  List<DonacionIndependienteResponseDTO> obtenerConFiltros(
      TipoEstadoDonacion estado, UUID subcategoriaId, UUID donanteId);

  DonacionIndependienteResponseDTO obtener(UUID id);

  DonacionIndependienteResponseDTO cambiarEstado(
      UUID id, CambioEstadoDonacionIndependienteRequestDTO request, String actor);

  void vencer();
}
