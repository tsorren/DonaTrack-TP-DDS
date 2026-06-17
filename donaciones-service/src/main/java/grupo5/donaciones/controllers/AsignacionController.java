package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.models.entities.donaciones.matchmaking.propuestas.Propuesta;
import grupo5.donaciones.services.AsignacionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/propuestas")
@RequiredArgsConstructor
public class AsignacionController {

  private final AsignacionService asignacionService;

  @PostMapping
  public ResponseEntity<List<Propuesta>> ejecutar() {
    return ResponseEntity.status(HttpStatus.CREATED).body(asignacionService.ejecutarAsignacion());
  }

  @GetMapping
  public ResponseEntity<List<Propuesta>> listar() {
    return ResponseEntity.ok(asignacionService.listarPropuestas());
  }

  @PatchMapping("/{id}")
  public ResponseEntity<Void> actualizarEstado(
      @PathVariable UUID id, @RequestBody ActualizarEstadoRequestDTO request) {

    asignacionService.actualizarEstado(id, request.getEstado());

    return ResponseEntity.ok().build();
  }
}
