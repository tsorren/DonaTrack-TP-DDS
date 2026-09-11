package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import grupo5.donaciones.services.INotificacionesAsyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificacionesAsyncService implements INotificacionesAsyncService {

  private static final Logger log = LoggerFactory.getLogger(NotificacionesAsyncService.class);
  private final NotificacionesFeignClient client;

  public NotificacionesAsyncService(NotificacionesFeignClient client) {
    this.client = client;
  }

  @Async
  public void sincronizarPersona(PersonaReplicaDTO dto) {
    try {
      client.sincronizarPersona(dto);
    } catch (Exception e) {
      log.error(
          "Fallo al sincronizar persona {} en notificaciones-service: {}",
          dto.id(),
          e.getMessage(),
          e);
    }
  }
}
