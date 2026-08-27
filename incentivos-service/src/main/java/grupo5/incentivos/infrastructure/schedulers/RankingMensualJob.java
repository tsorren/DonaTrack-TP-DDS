package grupo5.incentivos.infrastructure.schedulers;

import grupo5.incentivos.services.IRankingService;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
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
      MDC.put("traceId", UUID.randomUUID().toString().replace("-", ""));
      rankingService.calcularYNotificar(periodoActual);
    } catch (Exception e) {
      log.error("Error en el job de ranking mensual para {}: {}", periodoActual, e.getMessage(), e);
    } finally {
      MDC.remove("traceId");
    }
  }
}
