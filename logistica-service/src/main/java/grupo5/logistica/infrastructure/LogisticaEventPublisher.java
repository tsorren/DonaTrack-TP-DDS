package grupo5.logistica.infrastructure;

import grupo5.logistica.config.RabbitMQConfig;
import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.dto.eventos.EventoRutaIniciada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Publica eventos de dominio de Logística en el exchange de RabbitMQ. */
@Service
public class LogisticaEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(LogisticaEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;
  private final ApplicationEventPublisher applicationEventPublisher;

  public LogisticaEventPublisher(RabbitTemplate rabbitTemplate) {
    this(rabbitTemplate, null);
  }

  @Autowired
  public LogisticaEventPublisher(
      RabbitTemplate rabbitTemplate,
      @Nullable ApplicationEventPublisher applicationEventPublisher) {
    this.rabbitTemplate = rabbitTemplate;
    this.applicationEventPublisher = applicationEventPublisher;
  }

  public void publicarRutaAsignada(EventoRutaAsignada evento) {
    if (applicationEventPublisher != null) {
      applicationEventPublisher.publishEvent(evento);
    } else {
      onRutaAsignada(evento);
    }
  }

  public void publicarRutaIniciada(EventoRutaIniciada evento) {
    if (applicationEventPublisher != null) {
      applicationEventPublisher.publishEvent(evento);
    } else {
      onRutaIniciada(evento);
    }
  }

  public void publicarEntregaExitosa(EventoEntregaExitosa evento) {
    if (applicationEventPublisher != null) {
      applicationEventPublisher.publishEvent(evento);
    } else {
      onEntregaExitosa(evento);
    }
  }

  public void publicarEntregaFallida(EventoEntregaFallida evento) {
    if (applicationEventPublisher != null) {
      applicationEventPublisher.publishEvent(evento);
    } else {
      onEntregaFallida(evento);
    }
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onRutaAsignada(EventoRutaAsignada evento) {
    log.info("Publicando RutaAsignadaEvent: rutaId={}", evento.rutaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_RUTA_ASIGNADA, evento);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onRutaIniciada(EventoRutaIniciada evento) {
    log.info("Publicando RutaIniciadaEvent: rutaId={}", evento.rutaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_RUTA_INICIADA, evento);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onEntregaExitosa(EventoEntregaExitosa evento) {
    log.info("Publicando EntregaExitosaEvent: entregaId={}", evento.entregaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_ENTREGA_EXITOSA, evento);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void onEntregaFallida(EventoEntregaFallida evento) {
    log.info("Publicando EntregaFallidaEvent: entregaId={}", evento.entregaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_ENTREGA_FALLIDA, evento);
  }
}
