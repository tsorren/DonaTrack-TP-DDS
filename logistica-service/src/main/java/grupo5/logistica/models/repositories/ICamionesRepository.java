package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.camiones.Camion;
import java.util.List;
import java.util.Optional;

public interface ICamionesRepository extends CrudRepository<Camion> {
  Optional<Camion> findByPatente(String patente);

  List<Camion> findDisponibles();
}
