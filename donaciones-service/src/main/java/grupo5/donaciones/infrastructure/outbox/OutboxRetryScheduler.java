package grupo5.donaciones.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OutboxRetryScheduler {

  private static final Logger log = LoggerFactory.getLogger(OutboxRetryScheduler.class);

  private final OutboxStore outboxStore;

  public OutboxRetryScheduler(OutboxStore outboxStore) {
    this.outboxStore = outboxStore;
  }

  @Scheduled(fixedDelay = 10_000)
  public void procesarPendientes() {
    var pendientes = outboxStore.obtenerListosParaReintentar();
    if (pendientes.isEmpty()) return;

    log.info("[OUTBOX] Procesando {} entradas pendientes", pendientes.size());

    for (var entry : pendientes) {
      try {
        entry.getAccion().run();
        outboxStore.remover(entry.getId());
        log.info("[OUTBOX] Reintento exitoso para: {}", entry.getDescripcion());
      } catch (Exception e) {
        outboxStore.registrarFallo(entry);
      }
    }
  }
}
