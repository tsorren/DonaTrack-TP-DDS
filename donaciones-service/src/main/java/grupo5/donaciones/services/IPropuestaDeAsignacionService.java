package grupo5.donaciones.services;

import grupo5.donaciones.dto.Pagina;
import grupo5.donaciones.dto.propuestas.DonacionEnPropuestasDTO;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.FiltroPropuestas;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.dto.propuestas.ResumenPropuestasDTO;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import java.util.UUID;

public interface IPropuestaDeAsignacionService {

  /** Genera las propuestas nuevas y guarda la ejecución en el historial. */
  EjecucionRealizada ejecutarAsignacion();

  /**
   * Una página de propuestas con los filtros y el orden pedidos ({@code orden}: {@code puesto} o
   * {@code reciente}), resueltos en la base (R11-24).
   */
  Pagina<PropuestaDTO> listarPropuestas(FiltroPropuestas filtro, ParametrosListado listado);

  /** Cuántas pendientes hay en cada situación (R11-24). */
  ResumenPropuestasDTO resumen();

  /**
   * Las donaciones que están en al menos una pendiente, de a una página; primero las que no
   * alcanzan (R11-24).
   */
  Pagina<DonacionEnPropuestasDTO> donacionesEnPendientes(
      UUID categoriaId, boolean soloEnConflicto, ParametrosListado listado);

  /**
   * Aprueba o descarta una propuesta a nombre de {@code actor}: el id de la cuenta que decide (el
   * {@code sub} del token), o "SISTEMA" si no hay cuenta.
   */
  void actualizarEstado(UUID id, EstadoPropuesta estado, String actor);

  /** Una página del historial, de la más reciente a la más vieja (R11-24). */
  Pagina<EjecucionAsignacionDTO> historialEjecuciones(ParametrosListado listado);
}
