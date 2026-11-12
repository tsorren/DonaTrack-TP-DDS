package grupo5.donaciones.services;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import java.util.List;
import java.util.UUID;

public interface IPropuestaDeAsignacionService {

  List<PropuestaDTO> ejecutarAsignacion();

  List<PropuestaDTO> listarPropuestas();

  /**
   * Aprueba o descarta una propuesta a nombre de {@code actor}: el id de la cuenta que decide (el
   * {@code sub} del token), o "SISTEMA" si no hay cuenta.
   */
  void actualizarEstado(UUID id, EstadoPropuesta estado, String actor);

  List<EjecucionAsignacionDTO> historialEjecuciones();
}
