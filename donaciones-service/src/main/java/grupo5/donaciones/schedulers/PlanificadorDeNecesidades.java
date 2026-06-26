package grupo5.donaciones.schedulers;

import grupo5.donaciones.services.impl.PlanificacionNecesidadesService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeNecesidades {

  private final PlanificacionNecesidadesService planificacionService;

  public PlanificadorDeNecesidades(PlanificacionNecesidadesService planificacionService) {
    this.planificacionService = planificacionService;
  }

  @Scheduled(cron = "0 0 0 * * ?")
  public void ejecutarPlanificacionDeNecesidades() {
    planificacionService.generarNuevosPeriodosParaNecesidadesRecurrentes();
  }
}
