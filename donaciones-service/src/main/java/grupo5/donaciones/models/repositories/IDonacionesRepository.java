package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import java.util.Set;
import java.util.UUID;

public interface IDonacionesRepository extends CrudRepository<Donacion> {

  /** Ids de los donantes que registraron al menos una donación (contadores públicos, HB-02). */
  Set<UUID> idsDeDonantes();
}
