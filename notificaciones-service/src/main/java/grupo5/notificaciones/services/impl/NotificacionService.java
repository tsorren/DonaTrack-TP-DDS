package grupo5.notificaciones.services.impl;

import grupo5.notificaciones.dto.NotificacionDTO;
import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {
  private final INotificacionRepository repository;
  private final EventoMapper mapper;
  private final ApplicationEventPublisher eventPublisher;

  public NotificacionService(
      INotificacionRepository repository,
      EventoMapper mapper,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.mapper = mapper;
    this.eventPublisher = eventPublisher;
  }

  public void procesar(EventoNotificableDTO dto) {
    EventoNotificable evento = mapper.toEntity(dto);

    List<Notificacion> notificaciones = evento.generarNotificaciones();
    repository.saveAll(notificaciones);

    // Oleada 2 (RF-02): ya no se arma un evento de aplicación a mano; se publican y limpian los
    // domain events que cada Notificacion generó sobre sí misma al crearse (NotificacionCreada).
    notificaciones.forEach(this::publicarYLimpiarDomainEvents);
  }

  private void publicarYLimpiarDomainEvents(Notificacion notificacion) {
    notificacion.getDomainEvents().forEach(eventPublisher::publishEvent);
    notificacion.clearDomainEvents();
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
