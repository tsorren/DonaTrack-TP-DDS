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
   * Una página de propuestas con los filtros y el orden pedidos ({@code orden}: {@code puesto},
   * {@code reciente}, {@code decision_reciente} o {@code decision_antigua}), resueltos en la base
   * (R11-24; la búsqueda de las decididas, R11-35). Un período con el comienzo después del final da
   * 400.
   */
  Pagina<PropuestaDTO> listarPropuestas(FiltroPropuestas filtro, ParametrosListado listado);

  /** Cuántas pendientes hay en cada situación (R11-24) y cuántas decididas no tienen fecha. */
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

  /**
   * Como {@link #actualizarEstado(UUID, EstadoPropuesta, String)}; al descartar, guarda {@code
   * motivo} recortado (R11-35; vacío o nulo, sin motivo). Al aprobar, {@code motivo} se ignora.
   */
  void actualizarEstado(UUID id, EstadoPropuesta estado, String actor, String motivo);

  /** Una página del historial, de la más reciente a la más vieja (R11-24). */
  Pagina<EjecucionAsignacionDTO> historialEjecuciones(ParametrosListado listado);
}
