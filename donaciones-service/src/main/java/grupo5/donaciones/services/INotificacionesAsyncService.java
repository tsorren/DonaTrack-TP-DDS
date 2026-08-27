package grupo5.donaciones.services;

import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;
import java.util.UUID;

public interface INotificacionesAsyncService {
  void sincronizarPersona(PersonaReplicaDTO dto);

  void anonimizarPersona(UUID id);
}
