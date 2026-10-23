package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import java.util.UUID;

public interface IItemDonacionNormalizadoRepository
    extends CrudRepository<ItemDonacionNormalizado> {

  /** Si algún ítem normalizado usa la subcategoría: impide borrarla (D-20). */
  boolean existsBySubcategoriaId(UUID subcategoriaId);
}
