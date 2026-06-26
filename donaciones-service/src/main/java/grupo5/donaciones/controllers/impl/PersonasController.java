package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IPersonasController;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.services.IPersonasService;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/personas")
public class PersonasController implements IPersonasController {

  private final IPersonasService service;

  public PersonasController(IPersonasService service) {
    this.service = service;
  }

  @Override
  @PostMapping
  public ResponseEntity<PersonaOutputDTO> crearPersona(@RequestBody PersonaInputDTO persona) {
    PersonaOutputDTO creada = service.crearPersona(persona);
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  @Override
  @GetMapping
  public ResponseEntity<List<PersonaOutputDTO>> consultarPersonas(
      @RequestParam(required = false) TipoPersona tipo) {
    List<PersonaOutputDTO> personas = service.consultarPersonas(tipo);
    return ResponseEntity.ok(personas);
  }

  @Override
  @PutMapping("/{id}")
  public ResponseEntity<PersonaOutputDTO> actualizarPersona(
      @PathVariable UUID id, @RequestBody PersonaInputDTO persona) {
    PersonaOutputDTO actualizada = service.actualizarPersona(id, persona);
    return ResponseEntity.ok(actualizada);
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarPersona(@PathVariable UUID id) {
    service.eliminarPersona(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}/canal")
  public ResponseEntity<PersonaOutputDTO> actualizarCanal(
      @PathVariable("id") UUID id, @RequestBody PersonaInputDTO dto) {
    PersonaOutputDTO personaActualizada = service.actualizarCanal(id, dto);
    return ResponseEntity.ok(personaActualizada);
  }
}
