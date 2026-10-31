package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import java.util.List;
import java.util.UUID;

public interface INecesidadesRepository extends CrudRepository<Necesidad> {

  // "Satisfecha" no es un filtro del repositorio: depende de lo asignado, que se suma sobre las
  // donaciones independientes (DTI-06, ver AcumuladosDeAsignacion).
  List<Necesidad> findActivas();

  List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId);

  // Activas y recurrentes (para renovación de períodos)
  List<Necesidad> findByActivaTrueAndRecurrenteTrue();

  /** Si alguna necesidad (activa o no) usa la subcategoría: impide borrarla. */
  boolean existsBySubcategoriaId(UUID subcategoriaId);
}
