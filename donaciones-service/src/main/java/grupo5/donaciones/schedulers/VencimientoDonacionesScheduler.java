package grupo5.donaciones.schedulers;

import grupo5.donaciones.services.IDonacionesIndependientesService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class VencimientoDonacionesScheduler {
  private final IDonacionesIndependientesService donacionesIndependientesService;

  public VencimientoDonacionesScheduler(
      IDonacionesIndependientesService donacionesIndependientesService) {
    this.donacionesIndependientesService = donacionesIndependientesService;
  }

  @Scheduled(cron = "0 0 4 * * ?") // todos los dias a las 4am
  private void verificarVencimiento() {
    donacionesIndependientesService.vencer();
  }
}
