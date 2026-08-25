package grupo5.incentivos.models.entities.ranking;

import grupo5.incentivos.models.entities.donante.DonanteIncentivos;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GestorDeRankings {

  public static RankingMensual calcular(List<DonanteIncentivos> todos, YearMonth periodo) {
    RankingMensual ranking = new RankingMensual(periodo);
    AtomicInteger posicion = new AtomicInteger(1);

    todos.stream()
        .map(
            d ->
                new EntradaRanking(
                    0,
                    d.getId(),
                    d.getNombre(),
                    d.misionesCompletadasEnMes(periodo.getYear(), periodo.getMonthValue())))
        .filter(e -> e.getMisionesCompletadas() > 0)
        .sorted(Comparator.comparingLong(EntradaRanking::getMisionesCompletadas).reversed())
        .forEach(
            entrada -> {
              entrada.setPosicion(posicion.getAndIncrement());
              ranking.agregarEntrada(entrada);
            });
    return ranking;
  }
}
