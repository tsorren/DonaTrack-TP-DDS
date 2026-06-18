package grupo5.incentivos.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.YearMonth;
import java.util.Optional;

public interface IRankingRepository extends CrudRepository<RankingMensual> {
  Optional<RankingMensual> findByPeriodo(YearMonth periodo);
}
