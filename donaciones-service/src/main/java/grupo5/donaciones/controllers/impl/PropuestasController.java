package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.services.impl.PropuestaService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("${donatrack.routes.donaciones.propuestas-base}")
@RequiredArgsConstructor
public class PropuestasController {

  private final PropuestaService propuestaService;

  @GetMapping
  public ResponseEntity<List<PropuestaDTO>> listar() {
    return ResponseEntity.ok(propuestaService.listarPropuestas());
  }

  @PutMapping("${donatrack.routes.donaciones.propuestas-id-estado}")
  public ResponseEntity<Void> actualizarEstado(
      @PathVariable UUID id, @RequestBody ActualizarEstadoRequestDTO request) {

    propuestaService.actualizarEstado(id, request.estado());
    return ResponseEntity.ok().build();
  }
}
