package grupo5.donaciones.infrastructure.idempotency;

import java.util.UUID;

public interface IEventosConsumidosRepository {
  boolean yaFueConsumido(String eventType, UUID businessId, UUID donacionId);

  void registrar(EventoConsumido evento);
}
