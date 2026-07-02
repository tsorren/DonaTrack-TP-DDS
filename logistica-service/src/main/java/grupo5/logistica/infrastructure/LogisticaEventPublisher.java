package grupo5.logistica.infrastructure;

import grupo5.logistica.config.RabbitMQConfig;
import grupo5.logistica.dto.eventos.EntregaExitosaEvent;
import grupo5.logistica.dto.eventos.EntregaFallidaEvent;
import grupo5.logistica.dto.eventos.RutaIniciadaEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/** Publica eventos de dominio de Logística en el exchange de RabbitMQ. */
@Service
public class LogisticaEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(LogisticaEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public LogisticaEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  public void publicarRutaIniciada(RutaIniciadaEvent evento) {
    log.info("Publicando RutaIniciadaEvent: rutaId={}", evento.rutaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_RUTA_INICIADA, evento);
  }

  public void publicarEntregaExitosa(EntregaExitosaEvent evento) {
    log.info("Publicando EntregaExitosaEvent: entregaId={}", evento.entregaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_ENTREGA_EXITOSA, evento);
  }

  public void publicarEntregaFallida(EntregaFallidaEvent evento) {
    log.info("Publicando EntregaFallidaEvent: entregaId={}", evento.entregaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_ENTREGA_FALLIDA, evento);
  }
}
