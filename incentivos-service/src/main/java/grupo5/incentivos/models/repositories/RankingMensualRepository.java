package grupo5.incentivos.models.repositories;

import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class RankingMensualRepository {

  private final Map<YearMonth, RankingMensual> store = new ConcurrentHashMap<>();

  public RankingMensual guardar(RankingMensual ranking) {
    this.store.put(ranking.getPeriodo(), ranking);
    return ranking;
  }

  public Optional<RankingMensual> buscarPorPeriodo(YearMonth periodo) {
    return Optional.ofNullable(this.store.get(periodo));
  }

  public List<RankingMensual> listarTodos() {
    return new ArrayList<>(this.store.values());
  }
}
