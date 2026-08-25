package grupo5.notificaciones.services.impl;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import grupo5.notificaciones.services.events.NotificacionesCreadasEvent;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.util.List;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {
  private final INotificacionRepository repository;
  private final IPersonaRepository personaRepository;
  private final NotificacionSender sender;
  private final EventoMapper mapper;
  private final ApplicationEventPublisher eventPublisher;

  public NotificacionService(
          INotificacionRepository repository,
          EventoMapper mapper, ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.personaRepository = personaRepository;
    this.sender = sender;
    this.mapper = mapper;
      this.eventPublisher = eventPublisher;
  }

  public void procesar(EventoNotificableDTO dto) {
    EventoNotificable evento = mapper.toEntity(dto);

    List<Notificacion> notificaciones = evento.generarNotificaciones();
    repository.saveAll(notificaciones);

    eventPublisher.publishEvent(new NotificacionesCreadasEvent(this));
  }

  public List<NotificacionDTO> obtenerPorPersona(UUID personaId) {
    return repository.findByPersonaId(personaId).stream()
        .map(
            n ->
                new NotificacionDTO(
                    n.getId(),
                    n.getMensaje(),
                    n.getEstadoNotificacion().name(),
                    n.getFechaCreacion()))
        .toList();
  }
}
