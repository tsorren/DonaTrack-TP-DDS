package grupo5.donaciones.dto.categorias;

import grupo5.donaciones.models.entities.categorias.Unidad;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import java.util.UUID;

public record SubcategoriaInputDTO(
    @NotBlank(message = "El nombre de la subcategoría es obligatorio") String nombre,
    UUID idCategoria,
    List<@Valid AliasSubcategoriaInputDTO> aliases,
    Unidad unidad) {

  /** Sin unidad propia: hereda la de su categoría. */
  public SubcategoriaInputDTO(
      String nombre, UUID idCategoria, List<AliasSubcategoriaInputDTO> aliases) {
    this(nombre, idCategoria, aliases, null);
  }
}
