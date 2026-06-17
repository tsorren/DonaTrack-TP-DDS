package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import java.util.List;

public interface IAsignacionRepository extends CrudRepository<EjecucionAsignacionDTO> {
  List<EjecucionAsignacionDTO> obtenerHistorial();
}
