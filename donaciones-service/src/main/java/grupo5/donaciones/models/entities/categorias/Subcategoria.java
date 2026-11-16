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

  /**
   * En qué se pide (ADR 20261102 del ciclo de vida de la necesidad, punto 7). Opcional: si falta,
   * vale la de su categoría.
   */
  private Unidad unidad;

  @Getter(AccessLevel.NONE)
  private final List<AliasSubcategoria> aliases = new ArrayList<>();

  public Subcategoria(UUID categoriaId, String nombre) {
    this.id = UUID.randomUUID();
    validarSubCategoria(categoriaId, nombre);
    this.categoriaId = categoriaId;
    this.nombre = nombre;
  }

  /** Constructor de reconstitución para el adaptador JPA */
  public Subcategoria(
      UUID id, UUID categoriaId, String nombre, List<AliasSubcategoria> aliasesPersistidos) {
    this.id = id;
    this.categoriaId = categoriaId;
    this.nombre = nombre;
    this.aliases.addAll(aliasesPersistidos);
  }

  public List<AliasSubcategoria> getAliases() {
    return Collections.unmodifiableList(aliases);
  }

  public void agregarAlias(String alias) {
    if (tieneAlias(alias)) return;
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

  /** {@code null} deja que herede la de su categoría. */
  public void asignarUnidad(Unidad unidad) {
    this.unidad = unidad;
  }

  /** La propia, o la de su categoría si no tiene una. */
  public Unidad unidadCon(Unidad deLaCategoria) {
    return unidad != null ? unidad : deLaCategoria;
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
