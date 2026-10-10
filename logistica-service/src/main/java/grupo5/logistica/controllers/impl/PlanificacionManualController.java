package grupo5.logistica.controllers.impl;

import grupo5.logistica.services.IPlanificacionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Disparador auxiliar de la planificación para pruebas y demostraciones. En producción, la
 * planificación continúa iniciándose mediante el scheduler.
 */
@RestController
@RequestMapping("/api/logistica/planificaciones")
@ConditionalOnProperty(
    prefix = "logistica.planificacion",
    name = "manual-enabled",
    havingValue = "true")
public class PlanificacionManualController {

  private final IPlanificacionService planificacionService;

  public PlanificacionManualController(IPlanificacionService planificacionService) {
    this.planificacionService = planificacionService;
  }

  @PostMapping("/ejecuciones")
  public ResponseEntity<Void> iniciarPlanificacion() {
    planificacionService.iniciarPlanificacion();
    return ResponseEntity.accepted().build();
  }
}
