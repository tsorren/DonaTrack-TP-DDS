package grupo5.donaciones.services.mappers;

import grupo5.donaciones.dto.categorias.CategoriaInputDTO;
import grupo5.donaciones.dto.categorias.CategoriaOutputDTO;
import grupo5.donaciones.dto.categorias.SubcategoriaOutputDTO;
import grupo5.donaciones.models.entities.categorias.Categoria;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CategoriaMapper {

  public Categoria toEntity(CategoriaInputDTO dto) {
    if (dto == null) {
      return null;
    }
    return new Categoria(dto.nombre(), dto.conUso(), dto.conVencimiento(), dto.unidad());
  }

  public CategoriaOutputDTO toOutputDTO(Categoria entity) {
    return toOutputDTO(entity, List.of());
  }

  public CategoriaOutputDTO toOutputDTO(
      Categoria entity, List<SubcategoriaOutputDTO> subcategorias) {
    if (entity == null) {
      return null;
    }
    return new CategoriaOutputDTO(
        entity.getId(),
        entity.getNombre(),
        entity.getConUso(),
        entity.getConVencimiento(),
        entity.getTipoUnidad(),
        subcategorias);
  }

  public void updateEntity(Categoria entity, CategoriaInputDTO dto) {
    if (entity == null || dto == null) {
      return;
    }
    entity.actualizar(dto.nombre(), dto.conUso(), dto.conVencimiento(), dto.unidad());
  }
}
