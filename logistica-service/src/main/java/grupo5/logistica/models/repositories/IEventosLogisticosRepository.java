package grupo5.logistica.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.logistica.models.entities.eventos.EventoLogistico;
import java.util.List;

public interface IEventosLogisticosRepository extends CrudRepository<EventoLogistico> {
  List<EventoLogistico> findPendientes();
}
