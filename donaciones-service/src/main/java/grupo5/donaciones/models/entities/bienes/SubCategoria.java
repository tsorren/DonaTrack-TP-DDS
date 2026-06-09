package grupo5.donaciones.models.entities.bienes;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.donaciones.segmentaciones.DonacionIndependiente;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubCategoria {
  private final List<DonacionIndependiente> donacionesIndependientes = new ArrayList<>();
  private Categoria categoria;
  private String nombre;

  public SubCategoria(Categoria categoria, String nombre) {

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

  public void agregarDonacion(DonacionIndependiente donacion) {
    if (donacion == null) {
      throw new ValidationException(ErrorCatalog.SUBCATEGORIA_DONACION_NULA);
    }

    donacion.setSubCategoria(this);
    this.donacionesIndependientes.add(donacion);
  }

  public Integer calcularStock() {
    return this.donacionesIndependientes.stream()
        .mapToInt(DonacionIndependiente::getCantidad)
        .sum();
  }
}
