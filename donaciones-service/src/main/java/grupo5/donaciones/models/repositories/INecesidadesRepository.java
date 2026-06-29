package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import java.util.List;
import java.util.UUID;

public interface INecesidadesRepository extends CrudRepository<Necesidad> {

  List<Necesidad> findByEstaSatisfechaFalse();

  List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId);

  List<NecesidadRecurrente> findRecurrentesActivas();
}
