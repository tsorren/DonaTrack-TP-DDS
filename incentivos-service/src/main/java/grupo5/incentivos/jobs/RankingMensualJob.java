package grupo5.incentivos.jobs;

import grupo5.incentivos.services.RankingService;
import java.time.YearMonth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RankingMensualJob {

  private static final Logger log = LoggerFactory.getLogger(RankingMensualJob.class);

  private final RankingService rankingService;

  public RankingMensualJob(RankingService rankingService) {
    this.rankingService = rankingService;
  }

  @Scheduled(cron = "0 59 23 L * *")
  public void ejecutarRankingMensual() {
    YearMonth periodoActual = YearMonth.now();
    log.info("Ejecutando job de ranking mensual para el periodo {}", periodoActual);
    try {
      rankingService.calcularYPersistir(periodoActual);
      log.info("Job de ranking mensual completado exitosamente para {}", periodoActual);
    } catch (Exception e) {
      log.error("Error en el job de ranking mensual para {}: {}", periodoActual, e.getMessage(), e);
    }
  }
}
