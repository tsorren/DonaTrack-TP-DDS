package grupo5.logistica.schedulers;

import grupo5.logistica.services.IPlanificacionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeEntregas {

  private final IPlanificacionService planificacionService;

  public PlanificadorDeEntregas(IPlanificacionService planificacionService) {
    this.planificacionService = planificacionService;
  }

  @Scheduled(
      cron = "${logistica.planificacion.cron.expression:0 0 2 * * ?}",
      zone = "${logistica.planificacion.cron.zone:UTC}")
  public void ejecutar() {
    planificacionService.iniciarPlanificacion();
  }
}
