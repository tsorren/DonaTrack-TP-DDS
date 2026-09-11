package grupo5.donaciones.models.segmentacion;

import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SegmentadorSimple extends AbstractSegmentador<SegmentadorSimple.GroupingKey> {

  private final ISubcategoriasRepository subcategoriasRepository;

  protected record GroupingKey(UUID donacionOriginalId, Subcategoria subcategoria) {}

  @Override
  protected GroupingKey obtenerClaveDeAgrupacion(ItemDonacionNormalizado item) {
    Subcategoria subcat =
        item.getBien().subcategoriaId() != null
            ? subcategoriasRepository.findById(item.getBien().subcategoriaId()).orElse(null)
            : null;
    return new GroupingKey(item.getDonacionOriginalId(), subcat);
  }
}
