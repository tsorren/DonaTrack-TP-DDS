package grupo5.donaciones.services.impl;

import grupo5.donaciones.dto.comunicaciones.EventoPersonaSincronizadaV1;
import grupo5.donaciones.dto.comunicaciones.MedioDeContactoEventoDTO;
import grupo5.donaciones.dto.comunicaciones.MedioDeContactoReplicaDTO;
import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
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
  public void sincronizarPersona(PersonaReplicaDTO dto) {
    try {
      eventPublisher.publicarPersonaSincronizada(
          new EventoPersonaSincronizadaV1(
              dto.id(),
              dto.denominacion(),
              dto.tipoPersona().name(),
              dto.mediosDeContacto().stream()
                  .map(NotificacionesAsyncService::toMedioEvento)
                  .toList()));
    } catch (Exception e) {
      log.error(
          "Fallo al publicar persona.sincronizada.v1 para persona {}: {}",
          dto.id(),
          e.getMessage(),
          e);
    }
  }

  private static MedioDeContactoEventoDTO toMedioEvento(MedioDeContactoReplicaDTO medio) {
    return new MedioDeContactoEventoDTO(
        medio.tipo(),
        medio.esPredeterminado(),
        medio.direccionCorreo(),
        medio.caracteristica(),
        medio.codigoArea(),
        medio.numero());
  }
}
