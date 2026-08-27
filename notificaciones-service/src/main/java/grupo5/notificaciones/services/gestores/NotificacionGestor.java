package grupo5.notificaciones.services.gestores;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import grupo5.notificaciones.services.events.NotificacionesCreadasEvent;
import java.util.List;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacionGestor {
  private final INotificacionRepository repository;
  private final IPersonaRepository personaRepository;
  private final NotificacionSender sender;

  public NotificacionGestor(
      INotificacionRepository repository,
      IPersonaRepository personaRepository,
      NotificacionSender sender) {
    this.repository = repository;
    this.personaRepository = personaRepository;
    this.sender = sender;
  }

  @EventListener
  public void onNotificacionesCreadas(NotificacionesCreadasEvent event) {
    notificarPendientes();
  }

  public void notificarPendientes() {
    List<Notificacion> pendientes = repository.findByEstado(EstadoNotificacion.PENDIENTE);
    for (Notificacion notificacion : pendientes) {
      Persona persona = personaRepository.findById(notificacion.getPersonaId()).orElse(null);
      notificacion.notificar(persona, sender);
      repository.save(notificacion);
    }
  }
}
