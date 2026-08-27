package grupo5.incentivos.infrastructure.schedulers;

import grupo5.incentivos.services.IInactividadService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class InactividadJob {

  private final IInactividadService service;

  public InactividadJob(IInactividadService service) {
    this.service = service;
  }

  // Se ejecuta todos los días a las 8:00 AM
  @Scheduled(cron = "0 0 8 * * *")
  public void ejecutar() {
    service.procesarInactividad();
  }
}
