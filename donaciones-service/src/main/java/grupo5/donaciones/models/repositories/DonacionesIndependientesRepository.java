package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import grupo5.donaciones.models.entities.donacionesIndependientes.EnDeposito;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class DonacionesIndependientesRepository
    extends CrudRepositoryEnMemoria<DonacionIndependiente>
    implements IDonacionesIndependientesRepository {

  @Override
  public List<DonacionIndependiente> findEnDeposito() {
    return findAll().stream().filter(d -> d.getEstadoActual() instanceof EnDeposito).toList();
  }
}
