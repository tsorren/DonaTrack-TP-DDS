package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.categorias.CategoriaEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.CategoriasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataCategoriaRepository;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class CategoriaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<Categoria, CategoriaEntity, SpringDataCategoriaRepository>
    implements ICategoriasRepository {

  public CategoriaRepositoryJpaAdapter(
      SpringDataCategoriaRepository springDataRepo, CategoriasPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
  }

  @Override
  public List<Categoria> findAllById(Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return springDataRepo.findAllById(ids).stream().map(toDomain).toList();
  }
}
