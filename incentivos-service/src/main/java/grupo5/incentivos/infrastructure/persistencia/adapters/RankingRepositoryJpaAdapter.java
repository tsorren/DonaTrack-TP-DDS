package grupo5.incentivos.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.incentivos.infrastructure.persistencia.entities.RankingMensualEntity;
import grupo5.incentivos.infrastructure.persistencia.mappers.RankingPersistenciaMapper;
import grupo5.incentivos.infrastructure.persistencia.repositories.SpringDataRankingRepository;
import grupo5.incentivos.models.entities.ranking.RankingMensual;
import grupo5.incentivos.models.repositories.IRankingRepository;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class RankingRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        RankingMensual, RankingMensualEntity, SpringDataRankingRepository>
    implements IRankingRepository {

  private final RankingPersistenciaMapper mapper;

  public RankingRepositoryJpaAdapter(
      SpringDataRankingRepository springDataRepo, RankingPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.mapper = mapper;
  }

  @Override
  public Optional<RankingMensual> findByPeriodo(YearMonth periodo) {
    return springDataRepo.findByPeriodo(periodo).map(mapper::toDomain);
  }

  /**
   * El período se guarda como texto {@code AAAA-MM}, así que ordenarlo como texto es ordenarlo por
   * fecha.
   */
  @Override
  public List<RankingMensual> buscarHistorial(int pagina, int tamanio) {
    return springDataRepo
        .findAll(PageRequest.of(pagina, tamanio, Sort.by(Sort.Direction.DESC, "periodo")))
        .map(mapper::toDomain)
        .getContent();
  }
}
