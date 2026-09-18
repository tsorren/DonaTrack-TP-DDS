package grupo5.notificaciones.infrastructure.amqp;

import grupo5.notificaciones.config.RabbitMQConfig;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaV1;
import grupo5.notificaciones.services.impl.NotificacionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
public class DonacionEventListener {

  private static final Logger log = LoggerFactory.getLogger(DonacionEventListener.class);

  private final NotificacionService notificacionService;

  public DonacionEventListener(NotificacionService notificacionService) {
    this.notificacionService = notificacionService;
  }

  @RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICACIONES_DONACIONES)
  public void onDonacionAsignada(
      @Valid EventoDonacionAsignadaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    log.info(
        "Consumiendo evento donacion.asignada.v1: donacionId={}, donanteId={}, messageId={}",
        evento.donacionIndependienteId(),
        evento.personaDonanteId(),
        messageId);

    // Las excepciones no se capturan ni silencian para que opere el mecanismo de reintentos
    // del listener container y el posterior desvío a notificaciones.donaciones.dlq vía
    // notificaciones.dlx
    notificacionService.procesar(evento, messageId);
  }
}
