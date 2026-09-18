package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.choferes.Chofer;
import java.util.List;

public interface IChoferesRepository extends CrudRepository<Chofer> {
  List<Chofer> findActivos();

  List<Chofer> findDisponibles();
}
