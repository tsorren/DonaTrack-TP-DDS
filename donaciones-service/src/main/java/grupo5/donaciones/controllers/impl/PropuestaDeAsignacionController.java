package grupo5.donaciones.controllers.impl;

import grupo5.donaciones.controllers.IPropuestaDeAsignacionController;
import grupo5.donaciones.dto.propuestas.ActualizarEstadoRequestDTO;
import grupo5.donaciones.dto.propuestas.EjecucionAsignacionDTO;
import grupo5.donaciones.dto.propuestas.PropuestaDTO;
import grupo5.donaciones.services.IPropuestaDeAsignacionService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asignaciones")
@RequiredArgsConstructor
public class PropuestaDeAsignacionController implements IPropuestaDeAsignacionController {

  private final IPropuestaDeAsignacionService service;

  @Override
  @PostMapping("/ejecuciones")
  public ResponseEntity<List<PropuestaDTO>> ejecutar() {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.ejecutarAsignacion());
  }

  @Override
  @GetMapping("/ejecuciones")
  public ResponseEntity<List<EjecucionAsignacionDTO>> historial() {
    return ResponseEntity.ok(service.historialEjecuciones());
  }

  @Override
  @GetMapping("/propuestas")
  public ResponseEntity<List<PropuestaDTO>> listar() {
    return ResponseEntity.ok(service.listarPropuestas());
  }

  @Override
  @PutMapping("/propuestas/{id}/estado")
  public ResponseEntity<Void> actualizarEstado(
      @PathVariable UUID id, @RequestBody ActualizarEstadoRequestDTO request) {
    service.actualizarEstado(id, request.estado());
    return ResponseEntity.ok().build();
  }
}
