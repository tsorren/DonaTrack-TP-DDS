package grupo5.notificaciones.services.gestores;

import grupo5.notificaciones.models.entities.notificaciones.EstadoNotificacion;
import grupo5.notificaciones.models.entities.notificaciones.Notificacion;
import grupo5.notificaciones.models.entities.notificaciones.events.NotificacionCreada;
import grupo5.notificaciones.models.entities.personas.Persona;
import grupo5.notificaciones.models.ports.NotificacionSender;
import grupo5.notificaciones.models.repositories.INotificacionRepository;
import grupo5.notificaciones.models.repositories.IPersonaRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

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

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void onNotificacionCreada(NotificacionCreada event) {
    // Como NotificacionCreada hereda de DomainEvent, el id de la notificación suele venir en
    // event.aggregateId()
    Notificacion notificacion = repository.findById(event.notificacionId()).orElse(null);
    if (notificacion == null
        || notificacion.getEstadoNotificacion() != EstadoNotificacion.PENDIENTE) {
      return;
    }

    Persona persona = personaRepository.findById(notificacion.getPersonaId()).orElse(null);

    notificacion.notificar(persona, sender);
    repository.save(notificacion);

    // Publicar eventos posteriores (ENVIADA o FALLIDA)
    notificacion.getDomainEvents().forEach(eventPublisher::publishEvent);
    notificacion.clearDomainEvents();
  }
}
