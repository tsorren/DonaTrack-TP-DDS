package grupo5.notificaciones.infrastructure.amqp;

import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.services.impl.NotificacionService;
import java.time.LocalDateTime;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NotificacionRabbitListener {

  private final NotificacionService notificacionService;
  private final JdbcTemplate jdbcTemplate;

  // Con @Autowired(required = false) permitimos que la app levante en memoria sin que falle
  public NotificacionRabbitListener(
      NotificacionService notificacionService,
      @Autowired(required = false) JdbcTemplate jdbcTemplate) {
    this.notificacionService = notificacionService;
    this.jdbcTemplate = jdbcTemplate;
  }

  @RabbitListener(queues = "${donatrack.amqp.queues.notificaciones}")
  @Transactional
  public void onEventoNotificable(EventoNotificableDTO dto) {
    if (dto.eventId() == null) {
      notificacionService.procesar(dto);
      return;
    }

    try {
      // Patrón Inbox: Intentamos registrar el evento si tenemos base de datos relacional activa
      if (jdbcTemplate != null) {
        jdbcTemplate.update(
            "INSERT INTO evento_procesado (event_id, fecha_procesamiento) VALUES (?, ?)",
            dto.eventId(),
            LocalDateTime.now());
      }

      // Si el insert fue exitoso (no es duplicado) o estamos en memoria, procesamos
      notificacionService.procesar(dto);

    } catch (DataIntegrityViolationException e) {
      System.out.println("Evento duplicado ignorado de RabbitMQ: " + dto.eventId());
    }
  }
}
