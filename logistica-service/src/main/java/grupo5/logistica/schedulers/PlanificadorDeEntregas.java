package grupo5.logistica.schedulers;

import grupo5.logistica.services.IPlanificacionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeEntregas {

  private static final Logger log = LoggerFactory.getLogger(PlanificadorDeEntregas.class);

  private final IPlanificacionService planificacionService;

  public PlanificadorDeEntregas(IPlanificacionService planificacionService) {
    this.planificacionService = planificacionService;
  }

  @Scheduled(cron = "${logistica.planificacion.cron.expression:0 0 2 * * ?}")
  public void ejecutar() {
    log.info("[PLANIFICADOR-ENTREGAS] Ejecución del ciclo automático de planificación.");
    planificacionService.iniciarPlanificacion();
  }
}
