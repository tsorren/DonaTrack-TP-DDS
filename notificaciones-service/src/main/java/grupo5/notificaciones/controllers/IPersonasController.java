package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.PersonaReplicaDTO;
import java.util.UUID;
import org.springframework.http.ResponseEntity;

public interface IPersonasController {
  ResponseEntity<Void> sincronizar(PersonaReplicaDTO dto);

  ResponseEntity<Void> anonimizar(UUID id);
}
