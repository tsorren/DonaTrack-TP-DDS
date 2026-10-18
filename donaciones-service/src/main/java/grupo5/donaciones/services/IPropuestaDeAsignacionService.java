package grupo5.donaciones.services;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import java.util.List;
import java.util.UUID;

public interface IPropuestaDeAsignacionService {

  List<PropuestaDTO> ejecutarAsignacion();

  List<PropuestaDTO> listarPropuestas();

  void actualizarEstado(UUID id, EstadoPropuesta estado);

  List<EjecucionAsignacionDTO> historialEjecuciones();
}
