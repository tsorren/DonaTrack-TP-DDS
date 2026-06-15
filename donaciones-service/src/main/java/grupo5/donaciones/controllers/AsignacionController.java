package grupo5.donaciones.controllers;

import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.PropuestaRepository;
import grupo5.donaciones.services.GestorAlgoritmos;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/asignacion")
@RequiredArgsConstructor
public class AsignacionController {

  private final GestorAlgoritmos gestorAlgoritmos;
  private final PropuestaRepository propuestaRepository;

  @PostMapping("/ejecutar")
  public ResponseEntity<List<Propuesta>> ejecutar() {
    return ResponseEntity.ok(gestorAlgoritmos.ejecutar());
  }

  @GetMapping("/propuestas")
  public ResponseEntity<List<Propuesta>> listar() {
    return ResponseEntity.ok(propuestaRepository.findAll());
  }

  @PostMapping("/propuestas/{id}/confirmar")
  public ResponseEntity<Void> confirmar(@PathVariable Long id) {
    propuestaRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        .confirmar();
    return ResponseEntity.ok().build();
  }

  @PostMapping("/propuestas/{id}/rechazar")
  public ResponseEntity<Void> rechazar(@PathVariable Long id) {
    propuestaRepository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND))
        .rechazar();
    return ResponseEntity.ok().build();
  }
}
