package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import grupo5.donaciones.infrastructure.clients.NotificacionesFeignClient;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificacionesAsyncService {

  private static final Logger log = LoggerFactory.getLogger(NotificacionesAsyncService.class);
  private final NotificacionesFeignClient client;

  public NotificacionesAsyncService(NotificacionesFeignClient client) {
    this.client = client;
  }

  @Async
  public void sincronizarPersona(PersonaReplicaDTO dto) {
    try {
      log.info("Iniciando sincronización asincrónica de persona: {}", dto.id());
      client.sincronizarPersona(dto);
      log.info("Sincronización asincrónica exitosa para persona: {}", dto.id());
    } catch (Exception e) {
      log.error(
          "Fallo al sincronizar persona {} en notificaciones-service: {}",
          dto.id(),
          e.getMessage());
    }
  }

  @Async
  public void anonimizarPersona(UUID id) {
    try {
      log.info("Iniciando anonimización asincrónica de persona: {}", id);
      client.anonimizarPersona(id);
      log.info("Anonimización asincrónica exitosa para persona: {}", id);
    } catch (Exception e) {
      log.error("Fallo al anonimizar persona {} en notificaciones-service: {}", id, e.getMessage());
    }
  }
}
