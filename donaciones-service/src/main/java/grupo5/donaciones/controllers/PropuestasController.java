package grupo5.donaciones.controllers;

import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.PropuestaResponseDTO;
import grupo5.donaciones.services.impl.PropuestaService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asignaciones/propuestas")
@RequiredArgsConstructor
public class PropuestasController {

  private final PropuestaService propuestaService;

  @GetMapping
  public ResponseEntity<List<PropuestaResponseDTO>> listar() {
    return ResponseEntity.ok(propuestaService.listarPropuestas());
  }

  @PatchMapping("/{id}/estado")
  public ResponseEntity<Void> actualizarEstado(
      @PathVariable UUID id, @RequestBody ActualizarEstadoRequestDTO request) {

    propuestaService.actualizarEstado(id, request.getEstado());
    return ResponseEntity.ok().build();
  }
}
