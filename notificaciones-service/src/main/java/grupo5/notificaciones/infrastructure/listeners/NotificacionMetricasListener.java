package grupo5.notificaciones.infrastructure.listeners;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionCreada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionEnviada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionFallida;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Cuenta las transiciones de estado de las notificaciones. Las pendientes del momento salen de
 * restar a las creadas las enviadas y las fallidas.
 */
@Component
public class NotificacionMetricasListener {

  private static final String NOTIFICACIONES = "donatrack.notificaciones";

  private final MeterRegistry registro;

  public NotificacionMetricasListener(MeterRegistry registro) {
    this.registro = registro;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onNotificacionCreada(NotificacionCreada evento) {
    contar(EstadoNotificacion.PENDIENTE);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onNotificacionEnviada(NotificacionEnviada evento) {
    contar(EstadoNotificacion.ENVIADA);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onNotificacionFallida(NotificacionFallida evento) {
    contar(EstadoNotificacion.FALLIDA);
  }

  private void contar(EstadoNotificacion estado) {
    registro.counter(NOTIFICACIONES, "estado", estado.name().toLowerCase(Locale.ROOT)).increment();
  }
}
