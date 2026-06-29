package grupo5.donaciones.infrastructure.segmentadores;

import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.categorias.Unidad;
import grupo5.donaciones.models.entities.donaciones.Donacion;
import grupo5.donaciones.models.entities.donaciones.Estado;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.ICategoriasRepository;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SegmentadorComplejo extends AbstractSegmentador<SegmentadorComplejo.GroupingKey> {

  private final ICategoriasRepository categoriasRepository;
  private final ISubcategoriasRepository subcategoriasRepository;

  protected record GroupingKey(
      Donacion donacionOriginal,
      Subcategoria subcategoria,
      Estado estado,
      LocalDate fechaVencimiento,
      Unidad unidad) {}

  @Override
  protected GroupingKey obtenerClaveDeAgrupacion(ItemDonacionNormalizado item) {
    Subcategoria subcat =
        item.getBien().subcategoriaId() != null
            ? subcategoriasRepository.findById(item.getBien().subcategoriaId()).orElse(null)
            : null;

    Unidad unidad =
        (subcat != null && subcat.getCategoriaId() != null)
            ? categoriasRepository
                .findById(subcat.getCategoriaId())
                .map(Categoria::getTipoUnidad)
                .orElse(Unidad.UNIDADES)
            : Unidad.UNIDADES;

    return new GroupingKey(
        item.getDonacionOriginal(),
        subcat,
        item.getBien().bienOriginal().getEstado(),
        item.getBien().bienOriginal().getFechaVencimiento(),
        unidad);
  }
}
