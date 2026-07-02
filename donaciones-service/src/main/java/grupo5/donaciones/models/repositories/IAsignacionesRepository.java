package grupo5.donaciones.models.repositories;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import java.util.List;

public interface IAsignacionesRepository {
  EjecucionAsignacionDTO save(EjecucionAsignacionDTO aggregate);

  List<EjecucionAsignacionDTO> obtenerHistorial();

  void deleteAll();
}
