package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.donantes.ArchivoEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.ArchivosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataArchivoFilaErrorRepository;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataArchivoRepository;
import grupo5.donaciones.models.entities.donantes.Archivo;
import grupo5.donaciones.models.entities.donantes.FilaConError;
import grupo5.donaciones.models.repositories.IArchivoDonantesRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class ArchivoDonantesRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Archivo, ArchivoEntity, SpringDataArchivoRepository>
    implements IArchivoDonantesRepository {

  private final SpringDataArchivoFilaErrorRepository filasConError;
  private final ArchivosPersistenciaMapper mapper;

  public ArchivoDonantesRepositoryJpaAdapter(
      SpringDataArchivoRepository springDataRepo,
      SpringDataArchivoFilaErrorRepository filasConError,
      ArchivosPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.filasConError = filasConError;
    this.mapper = mapper;
  }

  @Override
  public void guardarFilasConError(List<FilaConError> filas) {
    filasConError.saveAll(filas.stream().map(mapper::filaToEntity).toList());
  }

  @Override
  public List<FilaConError> buscarFilasConError(UUID archivoId, int pagina, int tamanio) {
    return filasConError
        .findByArchivoIdOrderByLineaAsc(archivoId, PageRequest.of(pagina, tamanio))
        .stream()
        .map(mapper::filaToDomain)
        .toList();
  }
}
