package grupo5.donaciones.models.entities.bienes;

import grupo5.donaciones.models.entities.donaciones.DonacionIndependiente;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubCategoria {
  private final List<DonacionIndependiente> donaciones = new ArrayList<>();
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

  public void agregarDonacion(DonacionIndependiente donacion) {
    if (donacion == null) {
      throw new IllegalArgumentException("No se puede agregar una donacion nula.");
    }

    this.donaciones.add(donacion);
  }

  public Integer calcularStock() {
    return this.donaciones.stream().mapToInt(DonacionIndependiente::getCantidad).sum();
  }
}
