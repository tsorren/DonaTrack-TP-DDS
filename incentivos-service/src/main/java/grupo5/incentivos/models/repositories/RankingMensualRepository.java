package grupo5.incentivos.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class RankingMensualRepository extends CrudRepositoryEnMemoria<RankingMensual> implements CrudRepository<RankingMensual> {
  public Optional<RankingMensual> findByPeriodo(YearMonth periodo) {
    return this.findAll().stream().filter(r -> r.getPeriodo().equals(periodo)).findFirst();
  }
}
