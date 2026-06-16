package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepository;
import grupo5.donaciones.dto.EjecucionAsignacionDTO;
import java.util.List;

public interface IAsignacionRepository extends BaseRepository<EjecucionAsignacionDTO> {
  List<EjecucionAsignacionDTO> obtenerHistorial();
}
