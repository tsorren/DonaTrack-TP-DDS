package grupo5.donaciones.models.repositories;

import grupo5.donaciones.models.entities.necesidades.NecesidadRecurrente;
import java.util.List;
import java.util.UUID;

public interface INecesidadesRecurrentesRepository {
  void save(NecesidadRecurrente necesidad);

  List<NecesidadRecurrente> findByActivaTrue();

  List<NecesidadRecurrente> buscarNecesidadesPorEntidad(UUID entidadId);
}
