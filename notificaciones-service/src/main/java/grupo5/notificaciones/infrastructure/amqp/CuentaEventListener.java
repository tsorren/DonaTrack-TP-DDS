package grupo5.notificaciones.infrastructure.amqp;

import grupo5.notificaciones.config.RabbitMQConfig;
import grupo5.notificaciones.dto.input.EventoCuentaEnlaceSolicitadoV1;
import grupo5.notificaciones.services.impl.EnlaceDeCuentaService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

/**
 * Consume los eventos de cuenta que publica el SPI de correo de Keycloak (SPEC-06 E2-06).
 * Restablece el {@code traceId} del header {@code X-Trace-Id}, para que el log de este correo se
 * pueda seguir desde el pedido que lo originó. Nada de lo que lleva el evento (destinatario,
 * enlace) se loguea.
 */
@Component
@RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICACIONES_AUTH)
public class CuentaEventListener {

  static final String HEADER_TRACE_ID = "X-Trace-Id";
  private static final String MDC_TRACE_ID = "traceId";
  private static final Logger log = LoggerFactory.getLogger(CuentaEventListener.class);

  private final EnlaceDeCuentaService servicio;

  public CuentaEventListener(EnlaceDeCuentaService servicio) {
    this.servicio = servicio;
  }

  @RabbitHandler
  public void onEnlaceSolicitado(
      @Valid EventoCuentaEnlaceSolicitadoV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId,
      @Header(name = HEADER_TRACE_ID, required = false) String traceId) {
    if (traceId != null && !traceId.isBlank()) {
      MDC.put(MDC_TRACE_ID, traceId);
    }
    try {
      log.info(
          "Consumiendo evento cuenta.enlace-solicitado.v1: tipo={} cuentaId={}",
          evento.tipo(),
          evento.cuentaId());
      servicio.procesar(evento, messageId);
    } finally {
      MDC.remove(MDC_TRACE_ID);
    }
  }
}
