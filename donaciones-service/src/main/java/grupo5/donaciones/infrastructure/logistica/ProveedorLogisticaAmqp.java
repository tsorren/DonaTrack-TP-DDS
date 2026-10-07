package grupo5.donaciones.infrastructure.logistica;

import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoEntregaSolicitadaV1;
import grupo5.donaciones.dto.logistica.DatosEntregaLogistica;
import grupo5.donaciones.services.logistica.EnvioInciertoException;
import grupo5.donaciones.services.logistica.EnvioRechazadoException;
import grupo5.donaciones.services.logistica.IProveedorLogistica;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

/**
 * Proveedor de logística que recibe los pedidos por RabbitMQ: publica el comando {@code
 * entrega.solicitada.<proveedorId>.v1} en {@code donaciones.exchange} como carta certificada
 * ({@code mandatory} + publisher confirm) y espera el acuse para saber qué pasó.
 *
 * <ul>
 *   <li>Acuse positivo sin devolución: publicado.
 *   <li>Mensaje devuelto (no hay cola con esa routing key), nack o sin conexión con RabbitMQ: es
 *       seguro que no llegó → {@link EnvioRechazadoException}.
 *   <li>Sin acuse a tiempo o error de canal durante el envío: no se sabe → {@link
 *       EnvioInciertoException}.
 * </ul>
 */
public class ProveedorLogisticaAmqp implements IProveedorLogistica {

  private final String proveedorId;
  private final RabbitTemplate rabbitTemplate;
  private final long acuseTimeoutMs;

  /**
   * @param rabbitTemplate template con {@code mandatory=true} sobre una conexión con publisher
   *     confirms {@code correlated} y publisher returns habilitados
   */
  public ProveedorLogisticaAmqp(
      String proveedorId, RabbitTemplate rabbitTemplate, long acuseTimeoutMs) {
    this.proveedorId = proveedorId;
    this.rabbitTemplate = rabbitTemplate;
    this.acuseTimeoutMs = acuseTimeoutMs;
  }

  @Override
  public String id() {
    return proveedorId;
  }

  public String routingKey() {
    return RabbitMQConfig.routingKeyEntregaSolicitada(proveedorId);
  }

  @Override
  public void enviar(UUID envioId, DatosEntregaLogistica datos, String traceId) {
    CorrelationData correlacion = new CorrelationData(envioId.toString());
    try {
      rabbitTemplate.convertAndSend(
          RabbitMQConfig.EXCHANGE_DONACIONES,
          routingKey(),
          aComando(datos),
          envelope(envioId, traceId),
          correlacion);
    } catch (AmqpConnectException e) {
      throw new EnvioRechazadoException(proveedorId, "sin conexión con RabbitMQ", e);
    } catch (AmqpException e) {
      throw new EnvioInciertoException(proveedorId, "error de canal AMQP al publicar", e);
    }
    esperarAcuse(correlacion);
  }

  private void esperarAcuse(CorrelationData correlacion) {
    CorrelationData.Confirm confirm;
    try {
      confirm = correlacion.getFuture().get(acuseTimeoutMs, TimeUnit.MILLISECONDS);
    } catch (TimeoutException e) {
      throw new EnvioInciertoException(
          proveedorId, "sin acuse de RabbitMQ en " + acuseTimeoutMs + " ms", e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new EnvioInciertoException(proveedorId, "interrumpido esperando el acuse", e);
    } catch (ExecutionException e) {
      throw new EnvioInciertoException(proveedorId, "error esperando el acuse", e);
    }
    if (correlacion.getReturned() != null) {
      throw new EnvioRechazadoException(
          proveedorId,
          "mensaje devuelto, no hay cola para "
              + routingKey()
              + " ("
              + correlacion.getReturned().getReplyText()
              + ")");
    }
    if (!confirm.ack()) {
      throw new EnvioRechazadoException(proveedorId, "nack de RabbitMQ: " + confirm.reason());
    }
  }

  private static EventoEntregaSolicitadaV1 aComando(DatosEntregaLogistica datos) {
    return new EventoEntregaSolicitadaV1(
        datos.donacionIndependienteId(),
        datos.personaBeneficiariaId(),
        datos.destino(),
        datos.pesoTotalKG(),
        datos.volumenTotalM3(),
        datos.fecha());
  }

  private static MessagePostProcessor envelope(UUID envioId, String traceId) {
    return mensaje -> {
      mensaje.getMessageProperties().setMessageId(envioId.toString());
      mensaje.getMessageProperties().setHeader(FeignTraceRequestInterceptor.TRACE_HEADER, traceId);
      return mensaje;
    };
  }
}
