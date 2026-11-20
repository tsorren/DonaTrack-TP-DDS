package grupo5.donaciones.schedulers;

import grupo5.donaciones.services.IDonacionesIndependientesService;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VencimientoDonacionesScheduler {
  private final IDonacionesIndependientesService donacionesIndependientesService;

  public VencimientoDonacionesScheduler(
      IDonacionesIndependientesService donacionesIndependientesService) {
    this.donacionesIndependientesService = donacionesIndependientesService;
  }

  // Público: @SchedulerLock actúa por el proxy de Spring, que no intercepta métodos privados.
  @SchedulerLock(
      name = "donaciones.vencimiento-donaciones",
      lockAtMostFor = "PT30M",
      lockAtLeastFor = "PT30S")
  @Scheduled(cron = "0 0 4 * * ?") // todos los dias a las 4am
  public void verificarVencimiento() {
    donacionesIndependientesService.vencer();
  }
}
