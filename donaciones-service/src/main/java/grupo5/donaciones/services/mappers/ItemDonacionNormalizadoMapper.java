package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.dto.itemsNormalizados.outputs.ItemDonacionNormalizadoOutputDTO;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import grupo5.donaciones.models.entities.itemsNormalizados.ItemDonacionNormalizado;
import grupo5.donaciones.models.repositories.ISubcategoriasRepository;
import org.springframework.stereotype.Component;

@Component
public class ItemDonacionNormalizadoMapper {

  private final SubcategoriaMapper subcategoriaMapper;
  private final ISubcategoriasRepository subcategoriasRepository;

  public ItemDonacionNormalizadoMapper(
      SubcategoriaMapper subcategoriaMapper, ISubcategoriasRepository subcategoriasRepository) {
    this.subcategoriaMapper = subcategoriaMapper;
    this.subcategoriasRepository = subcategoriasRepository;
  }

  public ItemDonacionNormalizadoOutputDTO toOutputDTO(ItemDonacionNormalizado entity) {
    if (entity == null) {
      return null;
    }

    SubcategoriaOutputDTO subcatDto = null;
    if (entity.getBien() != null && entity.getBien().subcategoriaId() != null) {
      Subcategoria subcat =
          subcategoriasRepository.findById(entity.getBien().subcategoriaId()).orElse(null);
      if (subcat != null) {
        subcatDto = subcategoriaMapper.toOutputDTO(subcat);
      }
    }

    return new ItemDonacionNormalizadoOutputDTO(
        entity.getId(),
        entity.getDonacionOriginal() != null ? entity.getDonacionOriginal().getId() : null,
        entity.getBien() != null && entity.getBien().bienOriginal() != null
            ? entity.getBien().bienOriginal().getDescripcion()
            : null,
        entity.getCantidad(),
        subcatDto,
        entity.getBien() != null ? entity.getBien().confianza() : null,
        entity.getBien() != null ? entity.getBien().estadoNormalizacion() : null,
        entity.isSegmentado());
  }
}
