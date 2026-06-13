package grupo5.donaciones.infraestructure;

import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import java.time.LocalDate;

public class SegmentadorComplejo extends AbstractSegmentador<SegmentadorComplejo.GroupingKey> {

  protected record GroupingKey(
      Donacion donacionOriginal,
      Subcategoria subcategoria,
      Estado estado,
      LocalDate fechaVencimiento,
      Unidad unidad) {}

  @Override
  protected GroupingKey obtenerClaveDeAgrupacion(ItemDonacionNormalizado item) {
    return new GroupingKey(
        item.getDonacionOriginal(),
        item.getBien().getSubcategoria(),
        item.getBien().getBienOriginal().getEstado(),
        item.getBien().getBienOriginal().getFechaVencimiento(),
        item.getBien().getSubcategoria().getCategoria().getTipoUnidad());
  }
}
