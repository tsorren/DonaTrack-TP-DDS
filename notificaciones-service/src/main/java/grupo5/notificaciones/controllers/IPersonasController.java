package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.PersonaReplicaDTO;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface IPersonasController {
  ResponseEntity<Void> sincronizar(PersonaReplicaDTO dto);

  ResponseEntity<Void> anonimizar(UUID id);
}
