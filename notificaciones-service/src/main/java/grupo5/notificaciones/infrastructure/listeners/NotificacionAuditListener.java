package grupo5.notificaciones.infrastructure.listeners;

import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionEnviada;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionFallida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener de infraestructura para registrar trazas estructuradas de observabilidad y auditoría de
 * los domain events finales del ciclo de vida de una notificación (NotificacionEnviada /
 * NotificacionFallida).
 */
@Component
public class NotificacionAuditListener {

  private static final Logger log = LoggerFactory.getLogger(NotificacionAuditListener.class);

  @EventListener
  public void onNotificacionEnviada(NotificacionEnviada event) {
    log.info(
        "[AUDIT_NOTIFICACION_ENVIADA] Notificación {} enviada exitosamente en {}",
        event.notificacionId(),
        event.getTimestamp());
  }

  @EventListener
  public void onNotificacionFallida(NotificacionFallida event) {
    log.warn(
        "[AUDIT_NOTIFICACION_FALLIDA] Notificación {} falló en su despacho en {}",
        event.notificacionId(),
        event.getTimestamp());
  }
}
