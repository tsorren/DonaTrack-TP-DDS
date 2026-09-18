package grupo5.donaciones.services;

import grupo5.donaciones.dto.comunicaciones.PersonaReplicaDTO;

public interface INotificacionesAsyncService {
  void sincronizarPersona(PersonaReplicaDTO dto);
}
