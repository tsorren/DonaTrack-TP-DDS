package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import java.util.List;

public interface INecesidadesRecurrentesRepository extends CrudRepository<NecesidadRecurrente> {

  List<NecesidadRecurrente> findByActivaTrue();
}
