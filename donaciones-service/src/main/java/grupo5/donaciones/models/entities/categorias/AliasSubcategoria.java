package grupo5.donaciones.models.entities.categorias;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.UUID;
import lombok.Getter;

@Getter
public class AliasSubcategoria {
  private final UUID id;
  @JsonIgnore private final Subcategoria subcategoria;
  private final String alias;

  public AliasSubcategoria(Subcategoria subcategoria, String alias) {
    this.id = UUID.randomUUID();
    this.subcategoria = subcategoria;
    this.alias = alias;
  }
}
