package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import org.springframework.stereotype.Component;

@Component
public class ItemDonacionNormalizadoMapper {

  private final SubcategoriaMapper subcategoriaMapper;

  public ItemDonacionNormalizadoMapper(SubcategoriaMapper subcategoriaMapper) {
    this.subcategoriaMapper = subcategoriaMapper;
  }

  public ItemDonacionNormalizadoOutputDTO toOutputDTO(ItemDonacionNormalizado entity) {
    if (entity == null) {
      return null;
    }

    SubcategoriaOutputDTO subcatDto = null;
    if (entity.getBien() != null && entity.getBien().getSubcategoria() != null) {
      subcatDto = subcategoriaMapper.toOutputDTO(entity.getBien().getSubcategoria());
    }

    return new ItemDonacionNormalizadoOutputDTO(
        entity.getId(),
        entity.getDonacionOriginal() != null ? entity.getDonacionOriginal().getId() : null,
        entity.getBien() != null && entity.getBien().getBienOriginal() != null
            ? entity.getBien().getBienOriginal().getDescripcion()
            : null,
        entity.getCantidad(),
        subcatDto,
        entity.getBien() != null ? entity.getBien().getConfianza() : null,
        entity.getBien() != null ? entity.getBien().getEstadoNormalizacion() : null,
        entity.isSegmentado());
  }
}
