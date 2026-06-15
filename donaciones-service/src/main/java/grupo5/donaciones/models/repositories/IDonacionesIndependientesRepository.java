package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.BaseRepository;
import grupo5.donaciones.models.entities.donacionesIndependientes.DonacionIndependiente;
import java.util.List;

public interface IDonacionesIndependientesRepository extends BaseRepository<DonacionIndependiente> {

  List<DonacionIndependiente> findEnDeposito();
}
