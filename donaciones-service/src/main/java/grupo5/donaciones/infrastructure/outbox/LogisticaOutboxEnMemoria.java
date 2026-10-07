package grupo5.donaciones.infrastructure.outbox;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.services.logistica.EntradaOutboxLogistica;
import grupo5.donaciones.services.logistica.ILogisticaOutbox;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Implementación interina en memoria del outbox del broker. No ofrece atomicidad con el estado ni
 * durabilidad ante reinicios (deuda declarada): se reemplaza por una versión JPA sin tocar el
 * broker.
 */
@Component
public class LogisticaOutboxEnMemoria implements ILogisticaOutbox {

  private final Map<UUID, EntradaOutboxLogistica> entradas = new ConcurrentHashMap<>();

  @Override
  public void guardar(EntradaOutboxLogistica entrada) {
    if (entrada == null || entrada.getId() == null) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_NULO);
    }
    entradas.put(entrada.getId(), entrada);
  }

  @Override
  public Optional<EntradaOutboxLogistica> buscarPorId(UUID id) {
    return Optional.ofNullable(id == null ? null : entradas.get(id));
  }

  @Override
  public List<EntradaOutboxLogistica> pendientesListas(LocalDateTime ahora) {
    return entradas.values().stream()
        .filter(e -> e.estaListaPara(ahora))
        .sorted(Comparator.comparing(EntradaOutboxLogistica::getProximoIntento))
        .toList();
  }
}
