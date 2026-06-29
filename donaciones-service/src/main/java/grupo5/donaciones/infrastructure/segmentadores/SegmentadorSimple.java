package grupo5.donaciones.infrastructure.segmentadores;

import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SegmentadorSimple extends AbstractSegmentador<SegmentadorSimple.GroupingKey> {

  private final ISubcategoriasRepository subcategoriasRepository;

  protected record GroupingKey(Donacion donacionOriginal, Subcategoria subcategoria) {}

  @Override
  protected GroupingKey obtenerClaveDeAgrupacion(ItemDonacionNormalizado item) {
    Subcategoria subcat =
        item.getBien().subcategoriaId() != null
            ? subcategoriasRepository.findById(item.getBien().subcategoriaId()).orElse(null)
            : null;
    return new GroupingKey(item.getDonacionOriginal(), subcat);
  }
}
