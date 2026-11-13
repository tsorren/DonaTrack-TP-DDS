package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.repositories.listados.CriterioDonaciones;
import grupo5.donaciones.models.repositories.listados.LecturaPorLote;
import grupo5.donaciones.models.repositories.listados.Tramo;
import java.util.Set;
import java.util.UUID;

public interface IDonacionesRepository extends CrudRepository<Donacion>, LecturaPorLote<Donacion> {

  /** Ids de los donantes que registraron al menos una donación (contadores públicos, HB-02). */
  Set<UUID> idsDeDonantes();

  /** Una página de donaciones originales con la búsqueda y los filtros (R11-01). */
  Tramo<Donacion> buscar(CriterioDonaciones criterio);
}
