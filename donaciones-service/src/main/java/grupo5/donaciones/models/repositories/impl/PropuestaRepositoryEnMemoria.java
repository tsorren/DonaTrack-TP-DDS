package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.IPropuestasRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class PropuestaRepositoryEnMemoria extends CrudRepositoryEnMemoria<Propuesta>
    implements IPropuestasRepository {

  @Override
  public long contarEnEstado(EstadoPropuesta estado) {
    return storage.values().stream().filter(p -> p.getEstado() == estado).count();
  }
}
