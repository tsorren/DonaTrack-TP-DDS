package grupo5.incentivos.jobs;

import grupo5.incentivos.services.IIncentivosService;
import java.time.YearMonth;
import java.time.ZoneId;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RachaJob {

  private final IIncentivosService service;

  public RachaJob(IIncentivosService service) {
    this.service = service;
  }

  // Se ejecuta el primer día de cada mes a las 00:05 AM
  @Scheduled(cron = "0 5 0 1 * *")
  public void verificarRachasVencidas() {
    service.verificarRachasVencidas(YearMonth.now(ZoneId.systemDefault()));
  }
}
