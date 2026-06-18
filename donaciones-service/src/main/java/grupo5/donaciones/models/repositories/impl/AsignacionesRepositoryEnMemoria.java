package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.models.repositories.IAsignacionesRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class AsignacionesRepositoryEnMemoria extends CrudRepositoryEnMemoria<EjecucionAsignacionDTO>
    implements IAsignacionesRepository {
  @Override
  public List<EjecucionAsignacionDTO> obtenerHistorial() {
    return findAll();
  }
}
