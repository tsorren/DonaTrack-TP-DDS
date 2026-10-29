package grupo5.donaciones.models.repositories;

import grupo5.common.repositories.CrudRepository;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import java.util.List;
import java.util.UUID;

public interface IItemDonacionNormalizadoRepository
    extends CrudRepository<ItemDonacionNormalizado> {

  /** Si algún ítem normalizado usa la subcategoría: impide borrarla (D-20). */
  boolean existsBySubcategoriaId(UUID subcategoriaId);

  /** Los ítems normalizados de una donación. */
  List<ItemDonacionNormalizado> findByDonacion(UUID donacionId);

  /** Los ítems que esperan la revisión de un administrador. */
  List<ItemDonacionNormalizado> findPendientesDeRevision();
}
