package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepositoryEnMemoria;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public abstract class AsignacionRepositoryEnMemoria
    extends BaseRepositoryEnMemoria<EjecucionAsignacionDTO> implements IAsignacionRepository {
  @Override
  public List<EjecucionAsignacionDTO> obtenerHistorial() {
    return findAll();
  }
}
