package grupo5.donaciones.models.entities.categorias;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Subcategoria implements AggregateRoot {
  private final UUID id;
  private Categoria categoria;
  private String nombre;
  private final List<AliasSubcategoria> aliases = new ArrayList<>();

  public Subcategoria(Categoria categoria, String nombre) {
    this.id = UUID.randomUUID();
    validarSubCategoria(categoria, nombre);

    this.categoria = categoria;
    this.nombre = nombre;
  }

  public void agregarAlias(String alias) {
    aliases.add(new AliasSubcategoria(this, alias));
  }

  public void removerAlias(String alias) {
    aliases.removeIf(a -> a.getAlias().equals(alias));
  }

  public boolean tieneAlias(String alias) {
    for (AliasSubcategoria a : aliases) {
      if (a.getAlias().equals(alias)) return true;
    }
    return false;
  }

  private static void validarSubCategoria(Categoria categoria, String nombre) {

    if (categoria == null) {
      throw new ValidationException(ErrorCatalog.SUBCATEGORIA_SIN_CATEGORIA);
    }

    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.SUBCATEGORIA_SIN_NOMBRE);
    }
  }
}
