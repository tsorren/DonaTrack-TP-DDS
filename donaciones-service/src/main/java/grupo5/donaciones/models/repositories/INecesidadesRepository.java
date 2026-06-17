package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.List;
import java.util.UUID;

public interface INecesidadesRepository extends CrudRepository<Necesidad> {

  // Los métodos CRUD básicos ya vienen de BaseRepository

  List<Necesidad> findByEstaSatisfechaFalse();

  List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId);
}
