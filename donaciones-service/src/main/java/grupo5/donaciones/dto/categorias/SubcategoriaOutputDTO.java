package grupo5.donaciones.dto.categorias;

import grupo5.donaciones.models.entities.categorias.Unidad;
import java.util.List;
import java.util.UUID;

public record SubcategoriaOutputDTO(
    UUID id,
    String nombre,
    CategoriaOutputDTO categoria,
    List<AliasSubcategoriaOutputDTO> aliases,
    Unidad unidad) {

  public SubcategoriaOutputDTO(
      UUID id,
      String nombre,
      CategoriaOutputDTO categoria,
      List<AliasSubcategoriaOutputDTO> aliases) {
    this(id, nombre, categoria, aliases, null);
  }
}
