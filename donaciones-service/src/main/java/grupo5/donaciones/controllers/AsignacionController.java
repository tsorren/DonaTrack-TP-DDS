package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.models.entities.propuestas.Propuesta;
import grupo5.donaciones.services.impl.PropuestaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asignaciones/ejecuciones")
@RequiredArgsConstructor
public class AsignacionController {

  private final PropuestaService propuestaService;

  @PostMapping
  public ResponseEntity<List<Propuesta>> ejecutar() {
    return ResponseEntity.status(HttpStatus.CREATED).body(propuestaService.ejecutarAsignacion());
  }

  @GetMapping
  public ResponseEntity<List<EjecucionAsignacionDTO>> historial() {
    return ResponseEntity.ok(propuestaService.historialEjecuciones());
  }
}
