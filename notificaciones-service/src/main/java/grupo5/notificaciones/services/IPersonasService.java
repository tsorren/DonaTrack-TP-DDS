package grupo5.notificaciones.services;

import grupo5.notificaciones.dto.PersonaReplicaDTO;
import java.util.UUID;

public interface IPersonasService {
  void sincronizar(PersonaReplicaDTO dto);

  void anonimizar(UUID id);

  PersonaReplicaDTO obtenerPersona(UUID id);
}
