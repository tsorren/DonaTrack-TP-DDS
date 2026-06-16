package grupo5.donaciones.controllers;

import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.EstadoPropuesta;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.models.repositories.PropuestaRepository;
import grupo5.donaciones.services.GestorAlgoritmos;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/propuestas")
@RequiredArgsConstructor
public class AsignacionController {

  private final GestorAlgoritmos gestorAlgoritmos;
  private final PropuestaRepository propuestaRepository;

  @PostMapping
  public ResponseEntity<List<Propuesta>> ejecutar() {
    return ResponseEntity.status(HttpStatus.CREATED).body(gestorAlgoritmos.ejecutar());
  }

  @GetMapping
  public ResponseEntity<List<Propuesta>> listar() {
    return ResponseEntity.ok(propuestaRepository.findAll());
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> actualizarEstado(
      @PathVariable UUID id, @RequestBody ActualizarEstadoRequest request) {
    Propuesta propuesta =
        propuestaRepository
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (request.getEstado() == EstadoPropuesta.APROBADA) {
      propuesta.confirmar();
    } else if (request.getEstado() == EstadoPropuesta.DESCARTADA) {
      propuesta.rechazar();
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Estado invalido para transicion");
    }
    return ResponseEntity.ok().build();
  }

  @Getter
  @Setter
  static class ActualizarEstadoRequest {
    private EstadoPropuesta estado;
  }
}
