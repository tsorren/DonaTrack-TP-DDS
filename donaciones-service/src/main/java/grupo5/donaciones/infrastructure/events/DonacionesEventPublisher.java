package grupo5.donaciones.infrastructure.events;

import grupo5.common.logging.FeignTraceRequestInterceptor;
import grupo5.donaciones.config.RabbitMQConfig;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionAsignadaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionEnCaminoV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionEntregaFallidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionRecibidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionSegmentadaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonacionVencidaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteDadoDeBajaV1;
import grupo5.donaciones.dto.comunicaciones.EventoDonanteRegistradoV1;
import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Adaptador de salida: publica los eventos de dominio de Donaciones en {@code donaciones.exchange}.
 */
@Service
public class DonacionesEventPublisher implements IDonacionesEventPublisher {

  private static final Logger log = LoggerFactory.getLogger(DonacionesEventPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public DonacionesEventPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  public void publicarDonanteRegistrado(EventoDonanteRegistradoV1 evento) {
    publicar(RabbitMQConfig.ROUTING_KEY_DONANTE_REGISTRADO, evento);
  }

  @Override
  public void publicarDonanteDadoDeBaja(EventoDonanteDadoDeBajaV1 evento) {
    publicar(RabbitMQConfig.ROUTING_KEY_DONANTE_DADO_DE_BAJA, evento);
  }

  @Override
  public void publicarDonacionAsignada(EventoDonacionAsignadaV1 evento) {
    publicar(RabbitMQConfig.ROUTING_KEY_DONACION_ASIGNADA, evento);
  }

  @Override
  public void publicarDonacionEnCamino(EventoDonacionEnCaminoV1 evento) {
    publicar(RabbitMQConfig.ROUTING_KEY_DONACION_EN_CAMINO, evento);
  }

  @Override
  public void publicarDonacionRecibida(EventoDonacionRecibidaV1 evento) {
    publicar(RabbitMQConfig.ROUTING_KEY_DONACION_RECIBIDA, evento);
  }

  @Override
  public void publicarDonacionEntregaFallida(EventoDonacionEntregaFallidaV1 evento) {
    publicar(RabbitMQConfig.ROUTING_KEY_DONACION_ENTREGA_FALLIDA, evento);
  }

  @Override
  public void publicarDonacionVencida(EventoDonacionVencidaV1 evento) {
    publicar(RabbitMQConfig.ROUTING_KEY_DONACION_VENCIDA, evento);
  }

  @Override
  public void publicarPersonaSincronizada(EventoPersonaSincronizadaV1 evento) {
    publicar(RabbitMQConfig.ROUTING_KEY_PERSONA_SINCRONIZADA, evento);
  }

  @Override
  public void publicarDonacionSegmentada(EventoDonacionSegmentadaV1 evento) {
    publicar(RabbitMQConfig.ROUTING_KEY_DONACION_SEGMENTADA, evento);
  }

  /**
   * Si hay una transacción abierta, el mensaje sale recién cuando se confirma: un rollback no deja
   * un aviso de algo que no quedó guardado. Si la app se cae entre el commit y el envío, el aviso
   * se pierde (no hay outbox de notificaciones).
   */
  private void publicar(String routingKey, Object evento) {
    // Los headers se arman ahora, con el traceId de la operación que originó el evento
    MessagePostProcessor headers = agregarHeadersDeTrazabilidad();
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              enviar(routingKey, evento, headers);
            }
          });
      return;
    }
    enviar(routingKey, evento, headers);
  }

  private void enviar(String routingKey, Object evento, MessagePostProcessor headers) {
    log.info(
        "Publicando evento en {} con routingKey={}",
        RabbitMQConfig.EXCHANGE_DONACIONES,
        routingKey);
    rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_DONACIONES, routingKey, evento, headers);
  }

  private static MessagePostProcessor agregarHeadersDeTrazabilidad() {
    String traceId = MDC.get(FeignTraceRequestInterceptor.MDC_TRACE_KEY);
    String traceIdEfectivo =
        (traceId == null || traceId.isBlank())
            ? UUID.randomUUID().toString().replace("-", "")
            : traceId;
    return mensaje -> {
      mensaje.getMessageProperties().setMessageId(UUID.randomUUID().toString());
      mensaje
          .getMessageProperties()
          .setHeader(FeignTraceRequestInterceptor.TRACE_HEADER, traceIdEfectivo);
      return mensaje;
    };
  }
}
