package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.ArchivosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataArchivoRepository;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class ArchivoDonantesRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Archivo, ArchivoEntity, SpringDataArchivoRepository>
    implements IArchivoDonantesRepository {

  public ArchivoDonantesRepositoryJpaAdapter(
      SpringDataArchivoRepository springDataRepo, ArchivosPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
  }
}
