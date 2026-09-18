package grupo5.donaciones.infrastructure.outbox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class OutboxStore {

  private static final Logger log = LoggerFactory.getLogger(OutboxStore.class);

  private final ConcurrentHashMap<UUID, OutboxEntry> pendientes = new ConcurrentHashMap<>();

  public void agregar(OutboxEntry entry) {
    pendientes.put(entry.getId(), entry);
    log.warn("[OUTBOX] Llamada encolada para reintento: {}", entry.getDescripcion());
  }

  public List<OutboxEntry> obtenerListosParaReintentar() {
    LocalDateTime ahora = LocalDateTime.now();
    return pendientes.values().stream()
        .filter(e -> !e.getProximoIntento().isAfter(ahora))
        .collect(Collectors.toList());
  }

  public void remover(UUID id) {
    pendientes.remove(id);
  }

  public void registrarFallo(OutboxEntry entry) {
    entry.registrarFallo();
    if (entry.agotoIntentos()) {
      pendientes.remove(entry.getId());
      log.error(
          "[OUTBOX DEAD-LETTER] Llamada descartada tras {} intentos fallidos: {}",
          entry.getIntentos(),
          entry.getDescripcion());
    } else {
      log.warn(
          "[OUTBOX] Intento {}/{} fallido para: {}. Próximo reintento: {}",
          entry.getIntentos(),
          entry.getMaxIntentos(),
          entry.getDescripcion(),
          entry.getProximoIntento());
    }
  }
}
