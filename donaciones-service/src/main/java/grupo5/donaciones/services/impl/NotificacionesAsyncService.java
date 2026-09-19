package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;
import grupo5.donaciones.services.IDonacionesEventPublisher;
import grupo5.donaciones.services.INotificacionesAsyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificacionesAsyncService implements INotificacionesAsyncService {

  private static final Logger log = LoggerFactory.getLogger(NotificacionesAsyncService.class);
  private final IDonacionesEventPublisher eventPublisher;

  public NotificacionesAsyncService(IDonacionesEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @Async
  public void sincronizarPersona(EventoPersonaSincronizadaV1 evento) {
    if (evento == null) {
      log.warn("sincronizarPersona invocado con evento nulo, se ignora");
      return;
    }
    try {
      eventPublisher.publicarPersonaSincronizada(evento);
    } catch (Exception e) {
      log.error(
          "Fallo al publicar persona.sincronizada.v1 para persona {}: {}",
          evento.personaId(),
          e.getMessage(),
          e);
    }
  }
}
