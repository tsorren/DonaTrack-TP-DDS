package ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.DonacionIndependiente;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class SubCategoria {
  private Categoria categoria;
  private String nombre;
  private final List<DonacionIndependiente> donaciones = new ArrayList<>();

  public SubCategoria(Categoria categoria, String nombre) {

    validarSubCategoria(categoria, nombre);

    this.categoria = categoria;
    this.nombre = nombre;
  }

  private void validarSubCategoria(Categoria categoria, String nombre) {

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
