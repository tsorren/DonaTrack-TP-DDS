package grupo5.logistica.schedulers;

import grupo5.logistica.services.IPlanificacionService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeEntregas {

  private final IPlanificacionService planificacionService;

  public PlanificadorDeEntregas(IPlanificacionService planificacionService) {
    this.planificacionService = planificacionService;
  }

  @SchedulerLock(
      name = "logistica.planificacion-entregas",
      lockAtMostFor = "PT30M",
      lockAtLeastFor = "PT30S")
  @Scheduled(cron = "${logistica.planificacion.cron.expression:0 0 2 * * ?}")
  public void ejecutar() {
    planificacionService.iniciarPlanificacion();
  }
}
