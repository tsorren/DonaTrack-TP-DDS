package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.rutas.Ruta;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IRutasRepository extends CrudRepository<Ruta> {
  List<Ruta> findByFecha(LocalDate fecha);

  List<Ruta> findByCamionId(UUID camionId);

  List<Ruta> findByCamionIdAndFecha(UUID camionId, LocalDate fecha);
}
