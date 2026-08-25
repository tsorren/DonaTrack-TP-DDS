package grupo5.notificaciones.services.gestores;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.services.events.NotificacionesCreadasEvent;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacionGestor {
  private final INotificacionRepository repository;
  private final NotificacionSender sender;

  public NotificacionGestor(INotificacionRepository repository, NotificacionSender sender) {
    this.repository = repository;
    this.sender = sender;
  }

  @EventListener // "Escucha creaciones de notificaciones"
  public void onNotificacionesCreadas(NotificacionesCreadasEvent event) {
    notificarPendientes();
  }

  public void notificarPendientes() { // "Obtener notificaciones pendientes de la BD" + "Notificar"
    List<Notificacion> pendientes = repository.findByEstado(EstadoNotificacion.PENDIENTE);
    for (Notificacion notificacion : pendientes) {
      notificacion.notificar(sender); // único método de dominio, ya sin buscar Persona aparte
      repository.save(notificacion);
    }
  }
}
