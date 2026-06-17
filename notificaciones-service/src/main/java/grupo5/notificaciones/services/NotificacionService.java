package grupo5.notificaciones.services;

import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.eventos.EventoNotificable;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.NotificacionRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NotificacionService {
  private final NotificacionRepository repository;
  private final NotificacionSender sender;

  public NotificacionService(NotificacionRepository repository, NotificacionSender sender) {
    this.repository = repository;
    this.sender = sender;
  }

  public void procesar(EventoNotificable evento) {
    List<Notificacion> notificaciones = evento.generarNotificaciones();

    for (Notificacion notificacion : notificaciones) {
      notificacion.notificar(sender);
      repository.save(notificacion);
    }
  }
}
