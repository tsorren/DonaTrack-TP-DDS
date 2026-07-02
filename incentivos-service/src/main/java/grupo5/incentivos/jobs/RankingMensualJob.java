package grupo5.incentivos.jobs;

import grupo5.incentivos.services.IRankingService;
import java.time.YearMonth;
import java.time.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RankingMensualJob {

  private static final Logger log = LoggerFactory.getLogger(RankingMensualJob.class);

  private final IRankingService rankingService;

  public RankingMensualJob(IRankingService rankingService) {
    this.rankingService = rankingService;
  }

  @Scheduled(cron = "0 59 23 L * *")
  public void ejecutarRankingMensual() {
    YearMonth periodoActual = YearMonth.now(ZoneId.systemDefault());
    try {
      rankingService.calcularYNotificar(periodoActual);
    } catch (Exception e) {
      log.error("Error en el job de ranking mensual para {}: {}", periodoActual, e.getMessage(), e);
      // tirar mensaje de error
    }
  }
}
