package grupo5.donaciones.services.logistica;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto del outbox del broker. La implementación actual es en memoria; al pasar a PostgreSQL se
 * reemplaza sin tocar el broker.
 */
public interface ILogisticaOutbox {

  void guardar(EntradaOutboxLogistica entrada);

  Optional<EntradaOutboxLogistica> buscarPorId(UUID id);

  /** Entradas pendientes cuyo próximo intento ya venció. */
  List<EntradaOutboxLogistica> pendientesListas(LocalDateTime ahora);
}
