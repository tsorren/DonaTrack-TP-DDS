package grupo5.logistica.controllers.impl;

import grupo5.logistica.controllers.IPlanificacionController;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.EjecucionPlanificacionResponseDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.services.IPlanificacionService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone la ejecución manual controlada, el callback del proveedor y la consulta del seguimiento.
 */
@RestController
@RequestMapping("/api/logistica")
public class PlanificacionController implements IPlanificacionController {
  private final IPlanificacionService planificacionService;

  public PlanificacionController(IPlanificacionService planificacionService) {
    this.planificacionService = planificacionService;
  }

  @Override
  @PostMapping("/planificaciones/ejecuciones")
  public ResponseEntity<EjecucionPlanificacionResponseDTO> iniciarPlanificacionManual() {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(planificacionService.iniciarPlanificacionManual());
  }

  @Override
  @PostMapping({"/resultados", "/callback/rutas"})
  public ResponseEntity<SolicitudPlanificacionResponseDTO> procesarCallback(
      @Valid @RequestBody CallbackPlanificacionRequestDTO dto) {
    return ResponseEntity.ok(planificacionService.procesarCallback(dto));
  }

  @Override
  @GetMapping("/planificaciones/{id}")
  public ResponseEntity<SolicitudPlanificacionResponseDTO> obtenerPorId(
      @PathVariable("id") UUID id) {
    return ResponseEntity.ok(planificacionService.obtenerPorId(id));
  }
}
