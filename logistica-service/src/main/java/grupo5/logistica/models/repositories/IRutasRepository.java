package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.rutas.EstadoRuta;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IRutasRepository extends CrudRepository<Ruta> {
  List<Ruta> findByFecha(LocalDate fecha);

  List<Ruta> findByEstado(EstadoRuta estado);

  List<Ruta> findByCamionId(UUID camionId);

  List<Ruta> findByCamionIdAndFecha(UUID camionId, LocalDate fecha);

  /** Los de esos ids, en una sola lectura; los que no existen no vuelven (R11-43). */
  List<Ruta> findByIds(Collection<UUID> ids);
}
