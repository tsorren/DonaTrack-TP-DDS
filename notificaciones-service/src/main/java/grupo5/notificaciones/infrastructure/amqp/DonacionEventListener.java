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
  public void onDonacionEnCamino(@Valid EventoDonacionEnCaminoDTO evento) {
    log.info("Consumiendo evento donacion.en-camino: donanteId={}", evento.idPersonaDonante());
    notificacionService.procesar(evento);
  }

  // 3. Donación Recibida (Entrega exitosa)
  @RabbitHandler
  public void onDonacionRecibida(@Valid EventoDonacionRecibidaDTO evento) {
    log.info("Consumiendo evento donacion.recibida: donanteId={}", evento.idPersonaDonante());
    notificacionService.procesar(evento);
  }

  // 4. Entrega Fallida
  @RabbitHandler
  public void onEntregaFallida(@Valid EventoEntregaFallidaDTO evento) {
    log.info(
        "Consumiendo evento donacion.entrega-fallida: donanteId={}", evento.idPersonaDonante());
    notificacionService.procesar(evento);
  }

  // 5. Donación Vencida
  @RabbitHandler
  public void onDonacionVencida(@Valid EventoDonacionVencidaDTO evento) {
    log.info("Consumiendo evento donacion.vencida: donanteId={}", evento.idPersonaDonante());
    notificacionService.procesar(evento);
  }

  // 6. Donante Registrado
  @RabbitHandler
  public void onDonanteRegistrado(@Valid EventoDonanteRegistradoDTO evento) {
    log.info("Consumiendo evento donante.registrado: donanteId={}", evento.idPersonaDonante());
    notificacionService.procesar(evento);
  }

  // 7. Donante Inactivo
  @RabbitHandler
  public void onDonanteInactivo(@Valid EventoDonanteInactivoDTO evento) {
    log.info("Consumiendo evento donante.inactivo: donanteId={}", evento.idPersonaDonante());
    notificacionService.procesar(evento);
  }

  // 8. Persona Sincronizada
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
