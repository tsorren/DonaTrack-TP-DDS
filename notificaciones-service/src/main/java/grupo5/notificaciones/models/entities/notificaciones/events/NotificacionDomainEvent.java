package grupo5.notificaciones.models.entities.notificaciones.events;

import grupo5.common.events.EventoDeDominio;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Clase base de los domain events que {@code Notificacion} genera sobre sí misma (Oleada 2, RF-02).
 * Migrada en la Oleada 11 para extender {@code EventoDeDominio} (common-lib, construida por
 * incentivos-service) en vez de ser una interfaz marcadora sin miembros — {@code id} y {@code
 * timestamp} los aporta la clase base; acá solo se centraliza el {@code notificacionId} común a los
 * 3 subtipos ({@code NotificacionCreada}, {@code NotificacionEnviada}, {@code
 * NotificacionFallida}).
 *
 * <p>{@code CambioEstadoNotificacion} (paquete {@code eventos}, no {@code events}) NO es un subtipo
 * de esta clase: es un registro de auditoría permanente dentro de {@code
 * Notificacion.historialEstado}, nunca publicado ni limpiado — un concepto distinto al de un domain
 * event integración, aunque el nombre pueda sugerir lo contrario.
 */
public abstract class NotificacionDomainEvent extends EventoDeDominio {
  private final UUID notificacionId;

  protected NotificacionDomainEvent(UUID notificacionId, LocalDateTime fecha) {
    super(null, fecha);
    this.notificacionId = notificacionId;
  }

  public UUID notificacionId() {
    return notificacionId;
  }
}
