package grupo5.notificaciones.services.impl;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificacionService {
  private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

  private final INotificacionRepository repository;
  private final EventoMapper mapper;
  private final ApplicationEventPublisher eventPublisher;
  private final JdbcTemplate jdbcTemplate;

  public NotificacionService(
      INotificacionRepository repository,
      EventoMapper mapper,
      ApplicationEventPublisher eventPublisher) {
    this(repository, mapper, eventPublisher, null);
  }

  @Autowired
  public NotificacionService(
      INotificacionRepository repository,
      EventoMapper mapper,
      ApplicationEventPublisher eventPublisher,
      @Nullable JdbcTemplate jdbcTemplate) {
    this.repository = repository;
    this.mapper = mapper;
    this.eventPublisher = eventPublisher;
    this.jdbcTemplate = jdbcTemplate;
  }

  @Transactional
  public void procesar(EventoNotificableDTO dto) {
    UUID eventId = dto.eventId();
    if (eventId == null) {
      eventId = UUID.randomUUID();
      log.info(
          "[FALLBACK_LEGACY_EVENT_ID] Generado eventId {} para evento legacy de tipo {}",
          eventId,
          dto.getClass().getSimpleName());
    }

    if (jdbcTemplate != null) {
      int rows =
          jdbcTemplate.update(
              "INSERT INTO evento_procesado (event_id, fecha_procesamiento) VALUES (?, ?) ON CONFLICT (event_id) DO NOTHING",
              eventId,
              LocalDateTime.now());
      if (rows == 0) {
        log.info("Evento duplicado ignorado (Inbox): {}", eventId);
        return;
      }
    }

    EventoNotificable evento = mapper.toEntity(dto);
    List<Notificacion> notificaciones = evento.generarNotificaciones();
    repository.saveAll(notificaciones);
    notificaciones.forEach(this::publicarYLimpiarDomainEvents);
  }

  private void publicarYLimpiarDomainEvents(Notificacion notificacion) {
    notificacion.getDomainEvents().forEach(eventPublisher::publishEvent);
    notificacion.clearDomainEvents();
  }

  public Optional<NotificacionDTO> obtenerPorId(UUID id) {
    return repository.findById(id).map(this::toDTO);
  }

  public List<NotificacionDTO> obtenerPorPersona(UUID personaId) {
    return obtenerNotificaciones(personaId, null);
  }

  public List<NotificacionDTO> obtenerNotificaciones(UUID personaId, EstadoNotificacion estado) {
    List<Notificacion> notificaciones;
    if (personaId != null && estado != null) {
      notificaciones =
          repository.findByPersonaId(personaId).stream()
              .filter(n -> n.getEstadoNotificacion() == estado)
              .toList();
    } else if (personaId != null) {
      notificaciones = repository.findByPersonaId(personaId);
    } else if (estado != null) {
      notificaciones = repository.findByEstado(estado);
    } else {
      notificaciones = repository.findAll();
    }
    return notificaciones.stream().map(this::toDTO).toList();
  }

  private NotificacionDTO toDTO(Notificacion n) {
    return new NotificacionDTO(
        n.getId(), n.getMensaje(), n.getEstadoNotificacion().name(), n.getFechaCreacion());
  }
}
