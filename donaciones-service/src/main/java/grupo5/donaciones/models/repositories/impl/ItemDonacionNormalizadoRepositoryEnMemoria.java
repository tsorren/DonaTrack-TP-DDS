package grupo5.donaciones.models.repositories.impl;

import grupo5.common.repositories.CrudRepositoryEnMemoria;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.IItemDonacionNormalizadoRepository;
import java.util.Objects;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!postgres")
public class ItemDonacionNormalizadoRepositoryEnMemoria
    extends CrudRepositoryEnMemoria<ItemDonacionNormalizado>
    implements IItemDonacionNormalizadoRepository {

  @Override
  public boolean existsBySubcategoriaId(UUID subcategoriaId) {
    return storage.values().stream()
        .anyMatch(i -> Objects.equals(subcategoriaId, i.getBien().subcategoriaId()));
  }
}
