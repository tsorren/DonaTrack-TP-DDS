package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.camiones.Camion;
import grupo5.logistica.models.entities.camiones.EstadoCamion;
import java.util.List;
import java.util.Optional;

public interface ICamionRepository extends CrudRepository<Camion> {
  List<Camion> findByEstado(EstadoCamion estado);

  Optional<Camion> findByPatente(String patente);

  List<Camion> findDisponibles();
}
