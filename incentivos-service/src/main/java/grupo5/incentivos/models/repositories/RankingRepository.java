package grupo5.incentivos.models.repositories;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class RankingRepository extends CrudRepositoryEnMemoria<RankingMensual>
    implements IRankingRepository {
  public Optional<RankingMensual> findByPeriodo(YearMonth periodo) {
    return this.findAll().stream().filter(r -> r.getPeriodo().equals(periodo)).findFirst();
  }

  @Override
  public List<RankingMensual> buscarHistorial(int pagina, int tamanio) {
    return this.findAll().stream()
        .sorted(Comparator.comparing(RankingMensual::getPeriodo).reversed())
        .skip((long) pagina * tamanio)
        .limit(tamanio)
        .toList();
  }
}
