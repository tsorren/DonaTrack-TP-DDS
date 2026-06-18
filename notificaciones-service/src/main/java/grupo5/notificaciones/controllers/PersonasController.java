package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.services.IPersonasService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/notificaciones/personas")
public class PersonasController implements IPersonasController {

  private final IPersonasService service;

  public PersonasController(IPersonasService service) {
    this.service = service;
  }

  @Override
  @PutMapping
  public ResponseEntity<Void> sincronizar(@RequestBody PersonaReplicaDTO dto) {
    service.sincronizar(dto);
    return ResponseEntity.ok().build();
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> anonimizar(@PathVariable("id") UUID id) {
    service.anonimizar(id);
    return ResponseEntity.ok().build();
  }
}
