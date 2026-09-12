package grupo5.notificaciones.infrastructure.amqp;

import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.services.impl.NotificacionService;
import jakarta.validation.ValidationException;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacionRabbitListener {

  private static final Logger log = LoggerFactory.getLogger(NotificacionRabbitListener.class);

  private final NotificacionService notificacionService;

  public NotificacionRabbitListener(NotificacionService notificacionService) {
    this.notificacionService = notificacionService;
  }

  @RabbitListener(queues = "${donatrack.amqp.queues.notificaciones}")
  public void onEventoNotificable(EventoNotificableDTO dto) {
    try {
      notificacionService.procesar(dto);
    } catch (ValidationException | IllegalArgumentException | NoSuchElementException e) {
      log.error(
          "Mensaje AMQP descartado por error terminal de validación o negocio: eventId={}, error={}",
          dto != null ? dto.eventId() : null,
          e.getMessage(),
          e);
    }
  }
}
