package grupo5.logistica.schedulers;

import grupo5.logistica.services.impl.RutasService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PlanificadorDeEntregas {

  private static final Logger log = LoggerFactory.getLogger(PlanificadorDeEntregas.class);

  private final RutasService rutasService;

  public PlanificadorDeEntregas(RutasService rutasService) {
    this.rutasService = rutasService;
  }

  @Scheduled(cron = "${logistica.planificacion.cron.expression:0 0 2 * * ?}")
  public void ejecutar() {
    log.info("[SCHEDULER] Iniciando proceso automático programado de planificación...");

    rutasService.planificarEntregasPendientes();

    log.info("[SCHEDULER] Proceso automático finalizado exitosamente.");
  }
}
