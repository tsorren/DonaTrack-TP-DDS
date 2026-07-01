package grupo5.logistica.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.logistica.models.entities.eventos.EventoLogistico;
import grupo5.logistica.models.repositories.IEventosLogisticosRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class EventosLogisticosRepository extends CrudRepositoryEnMemoria<EventoLogistico>
    implements IEventosLogisticosRepository {

  @Override
  public List<EventoLogistico> findPendientes() {
    return storage.values().stream().filter(evento -> !evento.isProcesado()).toList();
  }
}
