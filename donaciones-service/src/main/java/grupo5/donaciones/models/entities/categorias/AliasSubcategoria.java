package grupo5.donaciones.models.entities.categorias;

import lombok.Getter;

@Getter
public class AliasSubcategoria {
  private final Subcategoria subcategoria;
  private final String alias;

  public AliasSubcategoria(Subcategoria subcategoria, String alias) {
    this.subcategoria = subcategoria;
    this.alias = alias;
  }
}
