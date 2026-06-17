package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class NecesidadesRepositoryEnMemoria extends CrudRepositoryEnMemoria<Necesidad>
    implements INecesidadesRepository {

  @Override
  public List<Necesidad> findByEstaSatisfechaFalse() {
    return storage.values().stream().filter(n -> !n.estaSatisfecha()).toList();
  }

  @Override
  public List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId) {
    return storage.values().stream().filter(n -> entidadId.equals(n.getEntidad().getId())).toList();
  }
}
