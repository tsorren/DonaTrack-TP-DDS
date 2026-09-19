package grupo5.incentivos.infrastructure;

import grupo5.incentivos.config.RabbitMQConfig;
import grupo5.incentivos.dto.events.EventoIncentivoDonanteInactivoV1;
import grupo5.incentivos.dto.events.EventoIncentivoMisionCumplidaV1;
import grupo5.incentivos.dto.events.EventoIncentivoSubioCategoriaV1;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class IncentivosEventosPublisher implements INotificacionesClient {

  private static final Logger log = LoggerFactory.getLogger(IncentivosEventosPublisher.class);

  private final RabbitTemplate rabbitTemplate;

  public IncentivosEventosPublisher(RabbitTemplate rabbitTemplate) {
    this.rabbitTemplate = rabbitTemplate;
  }

  @Override
  @Async("notificacionesTaskExecutor")
  public void notificarMisionCumplida(UUID idPersona, String nombreMision, String recompensa) {
    publicar(
        RabbitMQConfig.ROUTING_KEY_MISION_CUMPLIDA,
        new EventoIncentivoMisionCumplidaV1(
            idPersona, LocalDateTime.now(ZoneId.systemDefault()), nombreMision, recompensa),
        idPersona);
  }

  @Override
  @Async("notificacionesTaskExecutor")
  public void notificarAscensoCategoria(
      UUID idPersona, String categoriaNueva, String categoriaVieja) {
    publicar(
        RabbitMQConfig.ROUTING_KEY_SUBIO_CATEGORIA,
        new EventoIncentivoSubioCategoriaV1(
            idPersona, LocalDateTime.now(ZoneId.systemDefault()), categoriaNueva, categoriaVieja),
        idPersona);
  }

  @Override
  @Async("notificacionesTaskExecutor")
  public void notificarInactividad(UUID idPersona, int diasInactivo) {
    publicar(
        RabbitMQConfig.ROUTING_KEY_DONANTE_INACTIVO,
        new EventoIncentivoDonanteInactivoV1(
            idPersona, LocalDateTime.now(ZoneId.systemDefault()), diasInactivo),
        idPersona);
  }

  private void publicar(String routingKey, Object evento, UUID personaId) {
    try {
      rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_INCENTIVOS, routingKey, evento);
      log.info("Evento {} publicado para la persona {}", routingKey, personaId);
    } catch (RuntimeException e) {
      log.warn(
          "No se pudo publicar el evento {} para la persona {}: {}",
          routingKey,
          personaId,
          e.getMessage());
    }
  }
}
