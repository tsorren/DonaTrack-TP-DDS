package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.categorias.AliasSubcategoriaOutputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaInputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import grupo5.donaciones.models.entities.categorias.Subcategoria;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SubcategoriaMapper {

  private final CategoriaMapper categoriaMapper;

  public SubcategoriaMapper(CategoriaMapper categoriaMapper) {
    this.categoriaMapper = categoriaMapper;
  }

  public Subcategoria toEntity(SubcategoriaInputDTO dto, Categoria categoria) {
    if (dto == null) {
      return null;
    }
    Subcategoria subcategoria = new Subcategoria(categoria, dto.nombre());
    if (dto.aliases() != null) {
      dto.aliases().forEach(a -> subcategoria.agregarAlias(a.alias()));
    }
    return subcategoria;
  }

  public SubcategoriaOutputDTO toOutputDTO(Subcategoria entity) {
    if (entity == null) {
      return null;
    }

    CategoriaOutputDTO catDto = categoriaMapper.toOutputDTO(entity.getCategoria());

    List<AliasSubcategoriaOutputDTO> aliasDtos =
        entity.getAliases().stream()
            .map(a -> new AliasSubcategoriaOutputDTO(a.getId(), a.getAlias()))
            .toList();

    return new SubcategoriaOutputDTO(entity.getId(), entity.getNombre(), catDto, aliasDtos);
  }

  public void updateEntity(Subcategoria entity, SubcategoriaInputDTO dto, Categoria categoria) {
    if (entity == null || dto == null) {
      return;
    }
    entity.setNombre(dto.nombre());
    entity.setCategoria(categoria);
  }
}
