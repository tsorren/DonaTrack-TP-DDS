package grupo5.donaciones.infrastructure.idempotency;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class EventosConsumidosRepositoryEnMemoria implements IEventosConsumidosRepository {

  private final ConcurrentHashMap<String, EventoConsumido> storage = new ConcurrentHashMap<>();

  private String clave(String eventType, UUID businessId, UUID donacionId) {
    return eventType + ":" + businessId + ":" + donacionId;
  }

  @Override
  public boolean yaFueConsumido(String eventType, UUID businessId, UUID donacionId) {
    return storage.containsKey(clave(eventType, businessId, donacionId));
  }

  @Override
  public void registrar(EventoConsumido evento) {
    storage.put(clave(evento.eventType(), evento.businessId(), evento.donacionId()), evento);
  }
}
