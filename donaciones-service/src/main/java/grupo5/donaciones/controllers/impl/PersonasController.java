package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IPersonasController;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.models.entities.personas.TipoPersona;
import grupo5.donaciones.services.IPersonasService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/personas")
public class PersonasController implements IPersonasController {

  private final IPersonasService service;

  public PersonasController(IPersonasService service) {
    this.service = service;
  }

  @Override
  @PostMapping
  public ResponseEntity<PersonaOutputDTO> crearPersona(
      @Valid @RequestBody PersonaInputDTO persona) {
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
      @PathVariable UUID id, @Valid @RequestBody PersonaInputDTO persona) {
    PersonaOutputDTO actualizada = service.actualizarPersona(id, persona);
    return ResponseEntity.ok(actualizada);
  }

  @Override
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarPersona(@PathVariable UUID id) {
    service.eliminarPersona(id);
    return ResponseEntity.noContent().build();
  }
}
