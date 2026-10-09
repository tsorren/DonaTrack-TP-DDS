package grupo5.logistica.infrastructure;

import grupo5.logistica.config.RabbitMQConfig;
import grupo5.logistica.dto.eventos.EventoEntregaExitosa;
import grupo5.logistica.dto.eventos.EventoEntregaFallida;
import grupo5.logistica.dto.eventos.EventoRutaAsignada;
import grupo5.logistica.dto.eventos.EventoRutaIniciada;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Publica eventos de dominio de Logística en el exchange de RabbitMQ. Cada evento lleva en headers
 * quién lo publica (id de esta instancia y su token), para que Donaciones verifique de qué
 * proveedor viene.
 */
@Service
public class LogisticaEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(LogisticaEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;
  private final String proveedorId;
  private final String token;

  public LogisticaEventPublisher(
      RabbitTemplate rabbitTemplate,
      @Value("${logistica.instancia-id:donatrack}") String proveedorId,
      @Value("${logistica.token-vuelta:}") String token) {
    this.rabbitTemplate = rabbitTemplate;
    this.proveedorId = proveedorId;
    this.token = token;
    if (token == null || token.isBlank()) {
      log.warn(
          "[EVENTOS] logistica.token-vuelta no está configurado: Donaciones descartará los eventos"
              + " que publique esta instancia ({})",
          proveedorId);
    }
  }

  private MessagePostProcessor identidad() {
    return mensaje -> {
      mensaje.getMessageProperties().setHeader(RabbitMQConfig.HEADER_PROVEEDOR_ID, proveedorId);
      if (token != null && !token.isBlank()) {
        mensaje.getMessageProperties().setHeader(RabbitMQConfig.HEADER_PROVEEDOR_TOKEN, token);
      }
      return mensaje;
    };
  }

  public void publicarRutaAsignada(EventoRutaAsignada evento) {
    log.info("Publicando RutaAsignadaEvent: rutaId={}", evento.rutaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_RUTA_ASIGNADA, evento, identidad());
  }

  public void publicarRutaIniciada(EventoRutaIniciada evento) {
    log.info("Publicando RutaIniciadaEvent: rutaId={}", evento.rutaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_RUTA_INICIADA, evento, identidad());
  }

  public void publicarEntregaExitosa(EventoEntregaExitosa evento) {
    log.info("Publicando EntregaExitosaEvent: entregaId={}", evento.entregaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_ENTREGA_EXITOSA, evento, identidad());
  }

  public void publicarEntregaFallida(EventoEntregaFallida evento) {
    log.info("Publicando EntregaFallidaEvent: entregaId={}", evento.entregaId());
    rabbitTemplate.convertAndSend(
        RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY_ENTREGA_FALLIDA, evento, identidad());
  }
}
