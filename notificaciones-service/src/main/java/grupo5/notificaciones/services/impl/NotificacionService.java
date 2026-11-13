package grupo5.notificaciones.services.impl;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.notificaciones.dto.FiltroBandeja;
import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoDonacionAsignadaV1;
import grupo5.notificaciones.dto.input.EventoDonacionEnCaminoV1;
import grupo5.notificaciones.dto.input.EventoDonacionEntregaFallidaV1;
import grupo5.notificaciones.dto.input.EventoDonacionRecibidaV1;
import grupo5.notificaciones.dto.input.EventoDonacionVencidaV1;
import grupo5.notificaciones.dto.input.EventoDonanteRegistradoV1;
import grupo5.notificaciones.dto.input.EventoImportacionTerminadaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoDonanteInactivoV1;
import grupo5.notificaciones.dto.input.EventoIncentivoMisionCumplidaV1;
import grupo5.notificaciones.dto.input.EventoIncentivoSubioCategoriaV1;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.TipoEvento;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.repositories.ConsultaBandeja;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.PaginaBandeja;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
  private static final ZoneId ZONA_HORARIA = ZoneId.of("America/Argentina/Buenos_Aires");
  static final int TAMANIO_POR_DEFECTO = 20;
  static final int TAMANIO_MAXIMO = 100;
  static final int LARGO_MAXIMO_BUSQUEDA = 100;

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
    UUID eventId = resolverEventId(messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonanteRegistradoV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonacionEnCaminoV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonacionRecibidaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonacionEntregaFallidaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoDonacionVencidaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoIncentivoDonanteInactivoV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoIncentivoMisionCumplidaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  @Transactional
  public void procesar(EventoIncentivoSubioCategoriaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(messageId);
    if (yaRegistradoEnInbox(eventId)) {
      return;
    }
    persistirYPublicar(mapper.toEntity(evento));
  }

  /** Terminó o se cortó una importación: un aviso para la Administración (R11-30). */
  @Transactional
  public void procesar(EventoImportacionTerminadaV1 evento, @Nullable String messageId) {
    UUID eventId = resolverEventId(messageId);
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
    UUID eventId = resolverEventId(messageId);
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
  private boolean yaRegistradoEnInbox(@Nullable UUID eventId) {
    if (jdbcTemplate == null) {
      return false;
    }
    if (eventId == null) {
      log.warn("[INBOX_SIN_CLAVE] Evento sin eventId/messageId: se procesa sin deduplicar");
      return false;
    }
    int rows =
        jdbcTemplate.update(
            "INSERT INTO evento_procesado (event_id, fecha_procesamiento) VALUES (?, ?)"
                + " ON CONFLICT (event_id) DO NOTHING",
            eventId,
            LocalDateTime.now(ZONA_HORARIA));
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

  /**
   * La clave del inbox es el messageId AMQP que fija el emisor. Sin messageId no hay clave estable:
   * se devuelve null y el evento se procesa sin deduplicar. Un id de entidad (donanteId) o un UUID
   * aleatorio como respaldo descartaría eventos distintos del mismo donante o nunca deduplicaría.
   */
  @Nullable
  private static UUID resolverEventId(@Nullable String messageId) {
    if (messageId == null || messageId.isBlank()) {
      return null;
    }
    try {
      return UUID.fromString(messageId);
    } catch (IllegalArgumentException e) {
      return UUID.nameUUIDFromBytes(messageId.getBytes(StandardCharsets.UTF_8));
    }
  }

  @Transactional
  public void procesar(EventoNotificableDTO dto) {
    if (yaRegistradoEnInbox(dto.eventId())) {
      return;
    }
    persistirYPublicar(mapper.toEntity(dto));
  }

  /**
   * Un aviso directo a una persona: hoy, solo el de mensaje de contacto nuevo a la Administración,
   * por eso lleva el tipo {@code MENSAJE_CONTACTO} (R11-30) y ninguna referencia.
   */
  @Transactional
  public void notificarPersona(UUID personaId, String mensaje) {
    Notificacion notificacion = new Notificacion(personaId, mensaje);
    notificacion.etiquetar(TipoEvento.MENSAJE_CONTACTO.name(), null);
    repository.save(notificacion);
    publicarYLimpiarDomainEvents(notificacion);
  }

  private void publicarYLimpiarDomainEvents(Notificacion notificacion) {
    notificacion.getDomainEvents().forEach(eventPublisher::publishEvent);
    notificacion.clearDomainEvents();
  }

  public Optional<NotificacionDTO> obtenerPorId(UUID id) {
    return repository.findById(id).map(NotificacionService::toDTO);
  }

  /** Persona a la que se dirige la notificación; vacío si no existe (regla de pertenencia). */
  public Optional<UUID> personaDe(UUID id) {
    return repository.findById(id).map(Notificacion::getPersonaId);
  }

  /**
   * Una página de la bandeja de la persona (R11-14): filtra, ordena y pagina el repositorio, y el
   * total cuenta las que cumplen los filtros. Leerla no marca nada.
   *
   * @throws ValidationException ({@code ERR-CSR-003}) si un parámetro está fuera de rango
   */
  public PaginaNotificaciones bandeja(UUID personaId, FiltroBandeja filtro) {
    ConsultaBandeja consulta = consulta(filtro);
    if (personaId == null) {
      return new PaginaNotificaciones(List.of(), 0);
    }
    PaginaBandeja pagina = repository.bandeja(personaId, consulta);
    return new PaginaNotificaciones(
        pagina.notificaciones().stream().map(NotificacionService::toDTO).toList(), pagina.total());
  }

  /** Las notificaciones de una página y cuántas cumplen los filtros, sin contar la página. */
  public record PaginaNotificaciones(List<NotificacionDTO> notificaciones, long total) {}

  static ConsultaBandeja consulta(FiltroBandeja filtro) {
    int pagina = filtro.page() == null ? 0 : filtro.page();
    int tamanio = filtro.size() == null ? TAMANIO_POR_DEFECTO : filtro.size();
    if (pagina < 0 || tamanio < 1 || tamanio > TAMANIO_MAXIMO) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    return new ConsultaBandeja(
        filtro.leida(),
        texto(filtro.q()),
        tipos(filtro.tipo()),
        tipos(filtro.excluirTipo()),
        antiguas(filtro.orden()),
        pagina,
        tamanio);
  }

  private static String texto(@Nullable String q) {
    if (q == null || q.isBlank()) {
      return null;
    }
    String recortado = q.trim();
    if (recortado.length() > LARGO_MAXIMO_BUSQUEDA) {
      throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
    }
    return ConsultaBandeja.sinTildes(recortado);
  }

  /** Los nombres de {@link TipoEvento}, sin distinguir mayúsculas; uno desconocido da 400. */
  private static Set<String> tipos(@Nullable List<String> valores) {
    if (valores == null) {
      return Set.of();
    }
    Set<String> tipos = new HashSet<>();
    for (String valor : valores) {
      if (valor == null) {
        throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
      }
      try {
        tipos.add(TipoEvento.valueOf(valor.trim().toUpperCase(Locale.ROOT)).name());
      } catch (IllegalArgumentException e) {
        throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
      }
    }
    return tipos;
  }

  private static boolean antiguas(@Nullable String orden) {
    if (orden == null || orden.isBlank() || "recientes".equalsIgnoreCase(orden.trim())) {
      return false;
    }
    if ("antiguas".equalsIgnoreCase(orden.trim())) {
      return true;
    }
    throw new ValidationException(ErrorCatalog.ARGUMENTO_INVALIDO);
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
    return notificaciones.stream().map(NotificacionService::toDTO).toList();
  }

  private static NotificacionDTO toDTO(Notificacion n) {
    return new NotificacionDTO(
        n.getId(),
        n.getMensaje(),
        n.getEstadoNotificacion().name(),
        n.getFechaCreacion(),
        n.isLeida(),
        n.getFechaLectura(),
        n.getTipoEvento(),
        n.getReferenciaId());
  }

  @Transactional
  public Optional<NotificacionDTO> marcarComoLeida(UUID id) {
    return repository
        .findById(id)
        .map(
            notificacion -> {
              notificacion.marcarComoLeida();
              Notificacion guardada = repository.save(notificacion);
              return toDTO(guardada);
            });
  }

  @Transactional
  public Optional<NotificacionDTO> marcarComoNoLeida(UUID id) {
    return repository
        .findById(id)
        .map(
            notificacion -> {
              notificacion.marcarComoNoLeida();
              Notificacion guardada = repository.save(notificacion);
              return toDTO(guardada);
            });
  }

  @Transactional
  public void marcarTodasComoLeidas(UUID personaId) {
    if (personaId == null) {
      return;
    }
    repository.marcarTodasComoLeidas(personaId);
  }

  public long contarNoLeidas(UUID personaId) {
    if (personaId == null) {
      return 0L;
    }
    return repository.countByPersonaIdAndLeidaFalse(personaId);
  }
}
