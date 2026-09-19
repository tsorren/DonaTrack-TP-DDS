package grupo5.notificaciones.infrastructure.amqp;

import grupo5.notificaciones.config.RabbitMQConfig;
import grupo5.notificaciones.dto.input.EventoIncentivoDonanteInactivoV1;
import grupo5.notificaciones.dto.input.EventoIncentivoMisionCumplidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoSubioCategoriaV1;
import grupo5.notificaciones.services.impl.NotificacionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICACIONES_INCENTIVOS)
public class IncentivoEventListener {

  private static final Logger log = LoggerFactory.getLogger(IncentivoEventListener.class);
  private final NotificacionService notificacionService;

  public IncentivoEventListener(NotificacionService notificacionService) {
    this.notificacionService = notificacionService;
  }

  @RabbitHandler
  public void onMisionCumplida(
      @Valid EventoIncentivoMisionCumplidaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {

    log.info(
        "Consumiendo evento mision.cumplida: donanteId={}, mision={}, messageId={}",
        evento.personaDonanteId(),
        evento.nombreMision(),
        messageId);
    notificacionService.procesar(evento, messageId);
  }

  @RabbitHandler
  public void onSubioCategoria(
      @Valid EventoIncentivoSubioCategoriaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {

    log.info(
        "Consumiendo evento subio.categoria: donanteId={}, nuevaCategoria={}, messageId={}",
        evento.personaDonanteId(),
        evento.nombreNuevaCategoria(),
        messageId);
    notificacionService.procesar(evento, messageId);
  }

  @RabbitHandler
  public void onDonanteInactivo(
      @Valid EventoIncentivoDonanteInactivoV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {

    log.info(
        "Consumiendo evento donante.inactivo: donanteId={}, diasInactividad={}, messageId={}",
        evento.personaDonanteId(),
        evento.diasInactividad(),
        messageId);
    notificacionService.procesar(evento, messageId);
  }
}
