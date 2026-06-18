package grupo5.incentivos.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;

@Repository
public class RankingMensualRepository extends CrudRepositoryEnMemoria<RankingMensual>
    implements CrudRepository<RankingMensual> {
  public Optional<RankingMensual> findByPeriodo(YearMonth periodo) {
    return this.findAll().stream().filter(r -> r.getPeriodo().equals(periodo)).findFirst();
  }
}
