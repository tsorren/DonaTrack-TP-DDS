package grupo5.notificaciones.controllers;

import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.services.IPersonasService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${donatrack.routes.notificaciones.personas-base}")
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
  @DeleteMapping("${donatrack.routes.notificaciones.personas-id}")
  public ResponseEntity<Void> anonimizar(@PathVariable("id") UUID id) {
    service.anonimizar(id);
    return ResponseEntity.ok().build();
  }

  @Override
  @GetMapping("${donatrack.routes.notificaciones.personas-id}")
  public ResponseEntity<PersonaReplicaDTO> obtenerPersona(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(service.obtenerPersona(id));
  }
}
