package grupo5.donaciones.services;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.donacionesIndependientes.CambioEstadoDonacionIndependienteRequestDTO;
import grupo5.donaciones.dto.donacionesIndependientes.DonacionIndependienteResponseDTO;
import grupo5.donaciones.dto.donacionesIndependientes.FiltroDonacionesIndependientes;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface IDonacionesIndependientesService {
  /**
   * Una página de las que cumplen los filtros, con búsqueda sin tildes y orden por fecha de
   * registro, resuelta en la base (R11-01). Nunca la lista completa.
   */
  Pagina<DonacionIndependienteResponseDTO> obtenerConFiltros(
      FiltroDonacionesIndependientes filtro, ParametrosListado listado);

  DonacionIndependienteResponseDTO obtener(UUID id);

  DonacionIndependienteResponseDTO cambiarEstado(
      UUID id, CambioEstadoDonacionIndependienteRequestDTO request, String actor);

  void vencer();

  /** La entidad contó que llegó menos al confirmar la entrega (HB-17). */
  void registrarCantidadRecibida(UUID id, double cantidadRecibida);

  /** Réplica de las fotos de una entrega sin reporte, para las destacadas (HB-01). */
  void registrarFotosDeRecepcion(UUID id, LocalDateTime fechaEntrega, List<String> fotos);
}
