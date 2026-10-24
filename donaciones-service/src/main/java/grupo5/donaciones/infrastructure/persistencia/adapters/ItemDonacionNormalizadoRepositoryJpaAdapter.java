package grupo5.donaciones.infrastructure.persistencia.adapters;

import grupo5.common.repositories.CrudRepositoryJpaAdapter;
import grupo5.donaciones.infrastructure.persistencia.entities.itemsNormalizados.ItemDonacionNormalizadoEntity;
import grupo5.donaciones.infrastructure.persistencia.mapper.ItemsNormalizadosPersistenciaMapper;
import grupo5.donaciones.infrastructure.persistencia.repositories.SpringDataItemDonacionNormalizadoRepository;
import grupo5.donaciones.models.entities.itemsNormalizados.EstadoNormalizacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("postgres")
public class ItemDonacionNormalizadoRepositoryJpaAdapter
    extends CrudRepositoryJpaAdapter<
        ItemDonacionNormalizado,
        ItemDonacionNormalizadoEntity,
        SpringDataItemDonacionNormalizadoRepository>
    implements IItemDonacionNormalizadoRepository {

  public ItemDonacionNormalizadoRepositoryJpaAdapter(
      SpringDataItemDonacionNormalizadoRepository springDataRepo,
      ItemsNormalizadosPersistenciaMapper mapper) {
    super(springDataRepo, mapper::toEntity, mapper::toDomain);
  }

  @Override
  public boolean existsBySubcategoriaId(UUID subcategoriaId) {
    return subcategoriaId != null && springDataRepo.existsBySubcategoriaId(subcategoriaId);
  }

  @Override
  public List<ItemDonacionNormalizado> findByDonacion(UUID donacionId) {
    return springDataRepo.findByDonacionId(donacionId).stream().map(toDomain).toList();
  }

  @Override
  public List<ItemDonacionNormalizado> findPendientesDeRevision() {
    return springDataRepo.findByEstadoNormalizacion(EstadoNormalizacion.PENDIENTE_REVISION).stream()
        .map(toDomain)
        .toList();
  }
}
