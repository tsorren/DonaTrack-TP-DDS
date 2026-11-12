package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.repositories.listados.CriterioDonantes;
import grupo5.donaciones.models.repositories.listados.LecturaPorLote;
import grupo5.donaciones.models.repositories.listados.Tramo;

public interface IDonantesRepository extends CrudRepository<Donante>, LecturaPorLote<Donante> {

  /** Una página de donantes con la búsqueda, los filtros y el orden resueltos (R11-01). */
  Tramo<Donante> buscar(CriterioDonantes criterio);

  /** Cuántos donantes están activos. */
  long contarActivos();

  /** Cuántos donantes activos registraron al menos una donación (contadores públicos, HB-02). */
  long contarActivosConDonaciones();
}
