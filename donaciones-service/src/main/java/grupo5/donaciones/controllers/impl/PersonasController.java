package grupo5.donaciones.controllers.impl;

import static grupo5.donaciones.seguridad.Acceso.ADMIN;
import static grupo5.donaciones.seguridad.Acceso.ADMIN_O_SERVICIO;
import static grupo5.donaciones.seguridad.Acceso.DONANTE_O_ENTIDAD;
import static grupo5.donaciones.seguridad.Acceso.SU_ID;

import grupo5.donaciones.controllers.IPersonasController;
import grupo5.donaciones.dto.personas.FiltroPersonas;
import grupo5.donaciones.dto.personas.PersonaInputDTO;
import grupo5.donaciones.dto.personas.PersonaOutputDTO;
import grupo5.donaciones.services.IPersonasService;
import grupo5.donaciones.services.ParametrosListado;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize(ADMIN)
@RequestMapping("/api/personas")
public class PersonasController implements IPersonasController {

  private final IPersonasService service;

  public PersonasController(IPersonasService service) {
    this.service = service;
  }

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @PostMapping
  public ResponseEntity<PersonaOutputDTO> crearPersona(
      @Valid @RequestBody PersonaInputDTO persona) {
    PersonaOutputDTO creada = service.crearPersona(persona);
    return ResponseEntity.status(HttpStatus.CREATED).body(creada);
  }

  @Override
  @GetMapping
  public ResponseEntity<List<PersonaOutputDTO>> consultarPersonas(
      FiltroPersonas filtro, ParametrosListado listado) {
    return Paginas.conTotal(service.buscar(filtro, listado));
  }

  @Override
  @PreAuthorize(DONANTE_O_ENTIDAD + "hasAuthority('EDITAR_DATOS_ORGANIZACION') and " + SU_ID)
  @PutMapping("/{id}")
  public ResponseEntity<PersonaOutputDTO> actualizarPersona(
      @PathVariable UUID id, @Valid @RequestBody PersonaInputDTO persona) {
    PersonaOutputDTO actualizada = service.actualizarPersona(id, persona);
    return ResponseEntity.ok(actualizada);
  }

  @Override
  @PreAuthorize(ADMIN_O_SERVICIO)
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> eliminarPersona(@PathVariable UUID id) {
    service.eliminarPersona(id);
    return ResponseEntity.noContent().build();
  }
}
