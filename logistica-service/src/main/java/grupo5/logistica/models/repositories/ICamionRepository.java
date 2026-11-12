package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ICamionRepository extends CrudRepository<Camion> {
  List<Camion> findByEstado(EstadoCamion estado);

  Optional<Camion> findByPatente(String patente);

  List<Camion> findActivos();

  List<Camion> findDisponibles();

  /** Los de esos ids, en una sola lectura; los que no existen no vuelven (R11-43). */
  List<Camion> findByIds(Collection<UUID> ids);
}
