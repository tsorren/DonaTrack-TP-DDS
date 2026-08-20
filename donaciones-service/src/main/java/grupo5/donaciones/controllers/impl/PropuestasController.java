package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IPropuestasController;
import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.services.impl.PropuestaDeAsignacionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/asignaciones/propuestas")
@RequiredArgsConstructor
public class PropuestasController implements IPropuestasController {

  private final PropuestaDeAsignacionService propuestaDeAsignacionService;

  @Override
  @GetMapping
  public ResponseEntity<List<PropuestaDTO>> listar() {
    return ResponseEntity.ok(propuestaDeAsignacionService.listarPropuestas());
  }

  @Override
  @PutMapping("/{id}/estado")
  public ResponseEntity<Void> actualizarEstado(
      @PathVariable UUID id, @RequestBody ActualizarEstadoRequestDTO request) {

    propuestaDeAsignacionService.actualizarEstado(id, request.estado());
    return ResponseEntity.ok().build();
  }
}
