package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.categorias.SubcategoriaEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.SubcategoriasPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataSubcategoriaRepository;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Profile("postgres")
public class SubcategoriaRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        Subcategoria, SubcategoriaEntity, SpringDataSubcategoriaRepository>
    implements ISubcategoriasRepository {

  private final SubcategoriasPersistenciaMapper mapper;

  public SubcategoriaRepositoryJpaAdapter(
      SpringDataSubcategoriaRepository springDataRepo, SubcategoriasPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
    this.mapper = mapper;
  }

  // Se parte de la entidad existente para actualizar solo los alias que cambiaron.
  @Override
  public Subcategoria save(Subcategoria domainEntity) {
    SubcategoriaEntity existing = springDataRepo.findById(domainEntity.getId()).orElse(null);
    SubcategoriaEntity saved = springDataRepo.save(mapper.toEntity(domainEntity, existing));
    return mapper.toDomain(saved);
  }

  @Override
  public List<Subcategoria> saveAll(List<Subcategoria> aggregates) {
    return aggregates.stream().map(this::save).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<Subcategoria> findAllById(Collection<UUID> ids) {
    if (ids.isEmpty()) {
      return List.of();
    }
    return springDataRepo.findAllById(ids).stream().map(mapper::toDomain).toList();
  }
}
