package grupo5.incentivos.models.entities.ranking;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GestorDeRankings {

  public RankingMensual calcular(List<DonanteIncentivos> todos, YearMonth periodo) {
    RankingMensual ranking = new RankingMensual(periodo);
    if (todos == null || todos.isEmpty()) {
      return ranking;
    }

    AtomicInteger posicion = new AtomicInteger(1);

    List<DonanteIncentivos> donantesConMisiones =
        todos.stream()
            .filter(d -> d.misionesCompletadasEnMes(periodo.getYear(), periodo.getMonthValue()) > 0)
            .sorted(
                Comparator.comparingLong(
                        (DonanteIncentivos d) ->
                            d.misionesCompletadasEnMes(periodo.getYear(), periodo.getMonthValue()))
                    .reversed()
                    .thenComparing(DonanteIncentivos::getId))
            .toList();

    for (DonanteIncentivos d : donantesConMisiones) {
      long misiones = d.misionesCompletadasEnMes(periodo.getYear(), periodo.getMonthValue());
      ranking.agregarEntrada(
          new EntradaRanking(posicion.getAndIncrement(), d.getId(), d.getNombre(), misiones));
    }
    return ranking;
  }
}
