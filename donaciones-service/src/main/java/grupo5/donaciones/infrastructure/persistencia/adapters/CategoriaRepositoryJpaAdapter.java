package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.categorias.CategoriaEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.CategoriasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataCategoriaRepository;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
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
}
