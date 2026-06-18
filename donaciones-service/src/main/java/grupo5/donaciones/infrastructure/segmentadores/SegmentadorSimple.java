package grupo5.donaciones.infrastructure.segmentadores;

import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;

public class SegmentadorSimple extends AbstractSegmentador<SegmentadorSimple.GroupingKey> {

  protected record GroupingKey(Donacion donacionOriginal, Subcategoria subcategoria) {}

  @Override
  protected GroupingKey obtenerClaveDeAgrupacion(ItemDonacionNormalizado item) {
    return new GroupingKey(item.getDonacionOriginal(), item.getBien().getSubcategoria());
  }
}
