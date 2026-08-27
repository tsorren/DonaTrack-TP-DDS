package grupo5.notificaciones.services.gestores;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionCreada;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class NotificacionGestor {
  private final INotificacionRepository repository;
  private final IPersonaRepository personaRepository;
  private final NotificacionSender sender;
  private final ApplicationEventPublisher eventPublisher;

  public NotificacionGestor(
      INotificacionRepository repository,
      IPersonaRepository personaRepository,
      NotificacionSender sender,
      ApplicationEventPublisher eventPublisher) {
    this.repository = repository;
    this.personaRepository = personaRepository;
    this.sender = sender;
    this.eventPublisher = eventPublisher;
  }

  // Oleada 2 (RF-02): ya no se traduce a un ApplicationEvent propio — se escucha directamente el
  // domain event que Notificacion generó sobre sí misma al quedar PENDIENTE ("Escucha creaciones
  // de notificaciones").
  @EventListener
  public void onNotificacionCreada(NotificacionCreada event) {
    notificarPendientes();
  }

  public void notificarPendientes() {
    List<Notificacion> pendientes = repository.findByEstado(EstadoNotificacion.PENDIENTE);
    for (Notificacion notificacion : pendientes) {
      Persona persona = personaRepository.findById(notificacion.getPersonaId()).orElse(null);
      notificacion.notificar(persona, sender);
      repository.save(notificacion);
      // "regla de oro" del plan de refactor: si la mutación generó domain events (acá,
      // NotificacionEnviada/NotificacionFallida), hay que publicarlos y limpiarlos, igual que
      // hace NotificacionService al crear la notificación.
      notificacion.getDomainEvents().forEach(eventPublisher::publishEvent);
      notificacion.clearDomainEvents();
    }
  }
}
