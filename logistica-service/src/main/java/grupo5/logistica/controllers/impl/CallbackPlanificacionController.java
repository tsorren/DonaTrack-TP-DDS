package grupo5.logistica.controllers.impl;

import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.services.IPlanificacionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logistica/callback/rutas")
public class CallbackPlanificacionController {
  private final IPlanificacionService planificacionService;

  public CallbackPlanificacionController(IPlanificacionService planificacionService) {
    this.planificacionService = planificacionService;
  }

  @PostMapping
  public ResponseEntity<SolicitudPlanificacionResponseDTO> recibirCallback(
      @RequestBody CallbackPlanificacionRequestDTO dto) {
    return ResponseEntity.ok(planificacionService.procesarCallback(dto));
  }
}
