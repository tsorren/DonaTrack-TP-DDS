package grupo5.logistica.controllers.impl;

import grupo5.logistica.controllers.IPlanificacionController;
import grupo5.logistica.dto.callback.CallbackPlanificacionRequestDTO;
import grupo5.logistica.dto.callback.SolicitudPlanificacionResponseDTO;
import grupo5.logistica.services.IPlanificacionService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las solicitudes de planificación se generan únicamente desde el scheduler interno ({@code
 * PlanificadorDeEntregas}), en horarios de baja carga. Este controller no expone creación manual:
 * sólo el callback del proveedor de rutas y la consulta de una solicitud ya existente.
 */
@RestController
@RequestMapping("/api/logistica")
public class PlanificacionController implements IPlanificacionController {
  private final IPlanificacionService planificacionService;

  public PlanificacionController(IPlanificacionService planificacionService) {
    this.planificacionService = planificacionService;
  }

  @Override
  @PostMapping("/callback/rutas")
  public ResponseEntity<SolicitudPlanificacionResponseDTO> procesarCallback(
      @RequestBody CallbackPlanificacionRequestDTO dto) {
    return ResponseEntity.ok(planificacionService.procesarCallback(dto));
  }

  @Override
  @GetMapping("/planificaciones/{id}")
  public ResponseEntity<SolicitudPlanificacionResponseDTO> obtenerPorId(
      @PathVariable("id") UUID id) {
    return ResponseEntity.ok(planificacionService.obtenerPorId(id));
  }
}
