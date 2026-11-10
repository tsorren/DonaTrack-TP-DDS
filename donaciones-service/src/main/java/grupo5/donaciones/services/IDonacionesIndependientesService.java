package grupo5.donaciones.services;

import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import java.util.List;
import java.util.UUID;

public interface IDonacionesIndependientesService {
  List<DonacionIndependienteResponseDTO> obtenerTodas();

  List<DonacionIndependienteResponseDTO> obtenerConFiltros(FiltroDonacionesIndependientes filtro);

  DonacionIndependienteResponseDTO obtener(UUID id);

  DonacionIndependienteResponseDTO cambiarEstado(
      UUID id, CambioEstadoDonacionIndependienteRequestDTO request, String actor);

  void vencer();

  /** La entidad contó que llegó menos al confirmar la entrega (HB-17). */
  void registrarCantidadRecibida(UUID id, double cantidadRecibida);
}
