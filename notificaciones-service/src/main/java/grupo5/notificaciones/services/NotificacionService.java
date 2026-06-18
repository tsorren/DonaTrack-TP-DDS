package grupo5.notificaciones.services;

import grupo5.notificaciones.dto.input.EventoNotificableDTO;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.NotificacionRepository;
import grupo5.notificaciones.services.mappers.EventoMapper;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {
  private final NotificacionRepository repository;
  private final NotificacionSender sender;
  private final EventoMapper mapper;

  public NotificacionService(
      NotificacionRepository repository, NotificacionSender sender, EventoMapper mapper) {
    this.repository = repository;
    this.sender = sender;
    this.mapper = mapper;
  }

  public void procesar(EventoNotificableDTO dto) {
    EventoNotificable evento = mapper.toEntity(dto);

    List<Notificacion> notificaciones = evento.generarNotificaciones();

    for (Notificacion notificacion : notificaciones) {
      notificacion.notificar(sender);
      repository.save(notificacion);
    }
  }
}
