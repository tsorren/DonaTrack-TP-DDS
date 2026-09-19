package grupo5.notificaciones.services.impl;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaV1;
import grupo5.notificaciones.dto.input.EventoDonacionEnCaminoV1;
import grupo5.notificaciones.dto.input.EventoDonacionEntregaFallidaV1;
import grupo5.notificaciones.dto.input.EventoDonacionRecibidaV1;
import grupo5.notificaciones.dto.input.EventoDonacionVencidaV1;
import grupo5.notificaciones.dto.input.EventoDonanteRegistradoV1;
import grupo5.notificaciones.dto.input.EventoIncentivoDonanteInactivoV1;
import grupo5.notificaciones.dto.input.EventoIncentivoMisionCumplidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoSubioCategoriaV1;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.nio.charset.StandardCharsets;
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
  private final grupo5.notificaciones.services.IPersonasService personasService;

  public NotificacionService(
      INotificacionRepository repository,
      EventoMapper mapper,
      ApplicationEventPublisher eventPublisher) {
    this(repository, mapper, eventPublisher, null, null);
  }

  public NotificacionService(
      INotificacionRepository repository,
      EventoMapper mapper,
      ApplicationEventPublisher eventPublisher,
      @Nullable JdbcTemplate jdbcTemplate) {
    this(repository, mapper, eventPublisher, jdbcTemplate, null);
  }

  @Autowired
  public NotificacionService(
      INotificacionRepository repository,
      EventoMapper mapper,
      ApplicationEventPublisher eventPublisher,
      @Nullable JdbcTemplate jdbcTemplate,
      @Nullable grupo5.notificaciones.services.IPersonasService personasService) {
    this.repository = repository;
    this.mapper = mapper;
    this.eventPublisher = eventPublisher;
    this.jdbcTemplate = jdbcTemplate;
    this.personasService = personasService;
  }

  @Transactional
  public void procesar(EventoDonacionAsignadaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(evento.donacionIndependienteId(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonanteRegistradoV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(evento.donanteId(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonacionEnCaminoV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(evento.donanteId(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonacionRecibidaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(evento.donanteId(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonacionEntregaFallidaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(evento.donanteId(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonacionVencidaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(evento.donanteId(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoIncentivoDonanteInactivoV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(evento.personaDonanteId(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoIncentivoMisionCumplidaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(evento.personaDonanteId(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoIncentivoSubioCategoriaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(evento.personaDonanteId(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesarPersonaSincronizada(
      grupo5.notificaciones.dto.PersonaReplicaDTO dto, @Nullable String messageId) {
    if (dto == null) {
      return;
    }
    UUID eventId = resolverEventId(dto.id(), messageId);
    if (yaRegistradoEnInbox(eventId)) {
      log.info("Evento persona.sincronizada duplicado ignorado (Inbox): {}", eventId);
      return;
    }
    if (personasService != null) {
      personasService.sincronizar(dto);
    }
  }

  /**
   * Registra el evento en el inbox y devuelve true si ya estaba, es decir si esta entrega del
   * mensaje es un duplicado que hay que ignorar. Sin jdbcTemplate (perfil en memoria) no hay inbox
   * y nunca se considera duplicado.
   */
  private boolean yaRegistradoEnInbox(UUID eventId) {
    if (jdbcTemplate == null) {
      return false;
    }
    int rows =
        jdbcTemplate.update(
            "INSERT INTO evento_procesado (event_id, fecha_procesamiento) VALUES (?, ?)"
                + " ON CONFLICT (event_id) DO NOTHING",
            eventId,
            LocalDateTime.now());
    if (rows == 0) {
      log.info("Evento duplicado ignorado (Inbox): {}", eventId);
      return true;
    }
    return false;
  }

  private void persistirYPublicar(EventoNotificable evento) {
    List<Notificacion> notificaciones = evento.generarNotificaciones();
    repository.saveAll(notificaciones);
    notificaciones.forEach(this::publicarYLimpiarDomainEvents);
  }

  private static UUID resolverEventId(@Nullable UUID fallbackId, @Nullable String messageId) {
    if (messageId != null && !messageId.isBlank()) {
      try {
        return UUID.fromString(messageId);
      } catch (IllegalArgumentException e) {
        return UUID.nameUUIDFromBytes(messageId.getBytes(StandardCharsets.UTF_8));
      }
    }
    if (fallbackId != null) {
      return fallbackId;
    }
    return UUID.randomUUID();
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

    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(dto));
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
