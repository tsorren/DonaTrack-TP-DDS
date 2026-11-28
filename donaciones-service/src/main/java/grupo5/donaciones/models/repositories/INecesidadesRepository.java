package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.necesidades.Necesidad;
import grupo5.donaciones.models.repositories.listados.CriterioNecesidades;
import grupo5.donaciones.models.repositories.listados.LecturaPorLote;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.List;
import java.util.UUID;

public interface INecesidadesRepository
    extends CrudRepository<Necesidad>, LecturaPorLote<Necesidad> {

  // "Satisfecha" no es un filtro del repositorio: depende de lo asignado, que se suma sobre las
  // donaciones independientes (DTI-06, ver AcumuladosDeAsignacion).
  List<Necesidad> findActivas();

  List<Necesidad> buscarNecesidadesPorEntidad(UUID entidadId);

  // Activas y recurrentes (para renovación de períodos)
  List<Necesidad> findByActivaTrueAndRecurrenteTrue();

  /** Si alguna necesidad (activa o no) usa la subcategoría: impide borrarla. */
  boolean existsBySubcategoriaId(UUID subcategoriaId);

  /**
   * Una página de las necesidades vigentes (SPEC-11, G2): activas, sin vencer y no satisfechas. Acá
   * sí se mira "satisfecha", con lo asignado al período actual de cada una, para que la página y el
   * total salgan del repositorio y no de leer todas.
   */
  Tramo<Necesidad> buscarVigentes(CriterioNecesidades criterio);
}
