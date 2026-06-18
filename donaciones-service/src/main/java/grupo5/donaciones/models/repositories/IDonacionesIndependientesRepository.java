package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.util.List;

public interface IDonacionesIndependientesRepository extends CrudRepository<DonacionIndependiente> {

  List<DonacionIndependiente> findEnDeposito();
}
