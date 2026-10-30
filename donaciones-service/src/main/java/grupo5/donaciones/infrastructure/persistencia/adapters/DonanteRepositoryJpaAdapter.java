package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.donantes.DonanteEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.DonantesPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataDonanteRepository;
import grupo5.donaciones.models.entities.donantes.Donante;
import grupo5.donaciones.models.repositories.IDonantesRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class DonanteRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Donante, DonanteEntity, SpringDataDonanteRepository>
    implements IDonantesRepository {

  public DonanteRepositoryJpaAdapter(
      SpringDataDonanteRepository springDataRepo, DonantesPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
  }
}
