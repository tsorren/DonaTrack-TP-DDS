package grupo5.donaciones.models.entities.categorias;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Subcategoria implements AggregateRoot {
  private final UUID id;
  private Categoria categoria;
  private String nombre;

  public Subcategoria(Categoria categoria, String nombre) {
    this.id = UUID.randomUUID();
    validarSubCategoria(categoria, nombre);

    this.categoria = categoria;
    this.nombre = nombre;
  }

  private static void validarSubCategoria(Categoria categoria, String nombre) {

    if (categoria == null) {
      throw new ValidationException(ErrorCatalog.SUBCATEGORIA_SIN_CATEGORIA);
    }

    if (nombre == null || nombre.trim().isEmpty()) {
      throw new ValidationException(ErrorCatalog.SUBCATEGORIA_SIN_NOMBRE);
    }
  }

  /* TODO: Pasar a capa de service
  public Integer calcularStock() {
    return this.donacionesIndependientes.stream()
        .mapToInt(DonacionIndependiente::getCantidad)
        .sum();
  }
  */

}
