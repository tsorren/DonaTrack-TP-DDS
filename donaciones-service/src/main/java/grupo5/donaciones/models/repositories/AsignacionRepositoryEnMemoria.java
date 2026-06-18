package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public abstract class AsignacionRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<EjecucionAsignacionDTO> implements IAsignacionRepository {
  @Override
  public List<EjecucionAsignacionDTO> obtenerHistorial() {
    return findAll();
  }
}
