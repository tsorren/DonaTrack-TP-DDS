package grupo5.incentivos.infrastructure.persistencia.repositories;

import grupo5.incentivos.infrastructure.persistencia.entities.RankingMensualEntity;
import java.time.YearMonth;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpringDataRankingRepository extends JpaRepository<RankingMensualEntity, UUID> {

  Optional<RankingMensualEntity> findByPeriodo(YearMonth periodo);
}
