package grupo5.incentivos.jobs;

import grupo5.incentivos.services.IIncentivosService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InactividadJob {

  private final IIncentivosService service;

  public InactividadJob(IIncentivosService service) {
    this.service = service;
  }

  // Se ejecuta todos los días a las 8:00 AM
  @Scheduled(cron = "0 0 8 * * *")
  public void ejecutar() {
    service.procesarInactividad();
  }
}
