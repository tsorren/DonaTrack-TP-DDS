package grupo5.donaciones.schedulers;

import grupo5.donaciones.services.impl.PlanificacionNecesidadesService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeNecesidades {

  private final PlanificacionNecesidadesService planificacionService;

  @Autowired
  public PlanificadorDeNecesidades(PlanificacionNecesidadesService planificacionService) {
    this.planificacionService = planificacionService;
  }

  @SchedulerLock(
      name = "donaciones.planificacion-necesidades",
      lockAtMostFor = "PT30M",
      lockAtLeastFor = "PT30S")
  @Scheduled(cron = "0 0 0 * * ?")
  public void ejecutarPlanificacionDeNecesidades() {
    planificacionService.generarNuevosPeriodosParaNecesidadesRecurrentes();
  }
}
