package grupo5.donaciones.models.entities.categorias;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;

@Getter
public class Subcategoria implements AggregateRoot {
  private final UUID id;
  private UUID categoriaId;
  private String nombre;

  @Getter(AccessLevel.NONE)
  private final List<AliasSubcategoria> aliases = new ArrayList<>();

  public Subcategoria(UUID categoriaId, String nombre) {
    this(UUID.randomUUID(), categoriaId, nombre);
  }

  public Subcategoria(UUID id, UUID categoriaId, String nombre) {
    if (id == null) {
      throw new IllegalArgumentException("El id de la subcategoría no puede ser nulo");
    }
    validarSubCategoria(categoriaId, nombre);
    this.id = id;
    this.categoriaId = categoriaId;
    this.nombre = nombre;
  }

  public List<AliasSubcategoria> getAliases() {
    return Collections.unmodifiableList(aliases);
  }

  public void agregarAlias(String alias) {
    aliases.add(new AliasSubcategoria(alias, this.nombre));
  }

  public void removerAlias(String alias) {
    aliases.removeIf(a -> a.alias().equals(alias));
  }

  public void actualizar(UUID categoriaId, String nombre) {
    validarSubCategoria(categoriaId, nombre);
    this.categoriaId = categoriaId;
    this.nombre = nombre;
  }

  public boolean tieneAlias(String alias) {
    for (AliasSubcategoria a : aliases) {
      if (a.alias().equals(alias)) return true;
    }
    return false;
  }

  private static void validarSubCategoria(UUID categoriaId, String nombre) {
    if (categoriaId == null) {
      throw new ValidationException(ErrorCatalog.SUBCATEGORIA_SIN_CATEGORIA);
    }
    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.SUBCATEGORIA_SIN_NOMBRE);
    }
  }
}
