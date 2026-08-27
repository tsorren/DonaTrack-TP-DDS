package grupo5.incentivos.fixtures;

import grupo5.incentivos.models.entities.ranking.EntradaRanking;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.YearMonth;
import java.util.UUID;

public final class RankingMensualMother {

  private RankingMensualMother() {}

  public static RankingMensual vacioDeMayo2026() {
    return new RankingMensual(YearMonth.of(2026, 5));
  }

  public static RankingMensual vacio(YearMonth periodo) {
    return new RankingMensual(periodo);
  }

  public static RankingMensual conNEntradas(YearMonth periodo, int n) {
    RankingMensual ranking = new RankingMensual(periodo);
    for (int i = 1; i <= n; i++) {
      ranking.agregarEntrada(
          new EntradaRanking(i, UUID.randomUUID(), "Donante " + i, (n - i + 1) * 10L));
    }
    return ranking;
  }
}
