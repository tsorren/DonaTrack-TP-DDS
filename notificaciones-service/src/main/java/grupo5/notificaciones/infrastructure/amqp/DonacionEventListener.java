package grupo5.notificaciones.infrastructure.amqp;

import grupo5.notificaciones.config.RabbitMQConfig;
import grupo5.notificaciones.dto.input.*;
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
@RabbitListener(queues = RabbitMQConfig.QUEUE_NOTIFICACIONES_DONACIONES)
public class DonacionEventListener {

  private static final Logger log = LoggerFactory.getLogger(DonacionEventListener.class);
  private final NotificacionService notificacionService;
  private final grupo5.notificaciones.services.mappers.PersonaMapper personaMapper;

  public DonacionEventListener(
      NotificacionService notificacionService,
      grupo5.notificaciones.services.mappers.PersonaMapper personaMapper) {
    this.notificacionService = notificacionService;
    this.personaMapper = personaMapper;
  }

  // 1. Donación Asignada
  @RabbitHandler
  public void onDonacionAsignada(
      @Valid EventoDonacionAsignadaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    log.info(
        "Consumiendo evento donacion.asignada.v1: donacionId={}", evento.donacionIndependienteId());
    notificacionService.procesar(evento, messageId);
  }

  // 2. Donación en Camino (Ruta iniciada)
  @RabbitHandler
  public void onDonacionEnCamino(
      @Valid EventoDonacionEnCaminoV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    log.info("Consumiendo evento donacion.en-camino: donanteId={}", evento.donanteId());
    notificacionService.procesar(evento, messageId);
  }

  // 3. Donación Recibida (Entrega exitosa)
  @RabbitHandler
  public void onDonacionRecibida(
      @Valid EventoDonacionRecibidaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    log.info("Consumiendo evento donacion.recibida: donanteId={}", evento.donanteId());
    notificacionService.procesar(evento, messageId);
  }

  // 4. Entrega Fallida
  @RabbitHandler
  public void onEntregaFallida(
      @Valid EventoDonacionEntregaFallidaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    log.info("Consumiendo evento donacion.entrega-fallida: donanteId={}", evento.donanteId());
    notificacionService.procesar(evento, messageId);
  }

  // 5. Donación Vencida
  @RabbitHandler
  public void onDonacionVencida(
      @Valid EventoDonacionVencidaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    log.info("Consumiendo evento donacion.vencida: donanteId={}", evento.donanteId());
    notificacionService.procesar(evento, messageId);
  }

  // 6. Donante Registrado
  @RabbitHandler
  public void onDonanteRegistrado(
      @Valid EventoDonanteRegistradoV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    log.info("Consumiendo evento donante.registrado: donanteId={}", evento.donanteId());
    notificacionService.procesar(evento, messageId);
  }

  // 7. Persona Sincronizada
  @RabbitHandler
  public void onPersonaSincronizada(
      @Valid EventoPersonaSincronizadaV1 evento,
      @Header(name = AmqpHeaders.MESSAGE_ID, required = false) String messageId) {
    log.info(
        "Consumiendo evento persona.sincronizada.v1: personaId={}, messageId={}",
        evento.personaId(),
        messageId);
    notificacionService.procesarPersonaSincronizada(personaMapper.toReplicaDTO(evento), messageId);
  }
}
