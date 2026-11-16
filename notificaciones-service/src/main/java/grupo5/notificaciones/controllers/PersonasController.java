package grupo5.notificaciones.controllers;

import static grupo5.notificaciones.seguridad.Acceso.ADMIN_O_SERVICIO;

import grupo5.notificaciones.dto.PersonaReplicaDTO;
import grupo5.notificaciones.services.IPersonasService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@PreAuthorize(ADMIN_O_SERVICIO)
@RequestMapping("/api/notificaciones/personas")
@RequiredArgsConstructor
public class PersonasController implements IPersonasController {

  private final IPersonasService service;

  @Override
  @PutMapping
  public ResponseEntity<Void> sincronizar(@Valid @RequestBody PersonaReplicaDTO dto) {
    service.sincronizar(dto);
    return ResponseEntity.ok().build();
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> anonimizar(@PathVariable("id") UUID id) {
    service.anonimizar(id);
    return ResponseEntity.noContent().build();
  }

  @Override
  @GetMapping("/{id}")
  public ResponseEntity<PersonaReplicaDTO> obtenerPersona(@PathVariable("id") UUID id) {
    return ResponseEntity.ok(service.obtenerPersona(id));
  }
}
