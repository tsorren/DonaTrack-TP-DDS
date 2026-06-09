package grupo5.donaciones.models.entities.categorias;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubCategoria {
 private Categoria categoria;
  private String nombre;

  public SubCategoria(Categoria categoria, String nombre) {

    validarSubCategoria(categoria, nombre);

    this.categoria = categoria;
    this.nombre = nombre;
  }

  private static void validarSubCategoria(Categoria categoria, String nombre) {

    if (categoria == null) {
      throw new IllegalArgumentException("La subcategoría debe pertenecer a una categoría.");
    }

    if (nombre == null || nombre.trim().isEmpty()) {
      throw new IllegalArgumentException("La subcategoría debe tener un nombre.");
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
