package ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes;

import lombok.Data;

@Data
public class SubCategoria {
  private Categoria categoria;
  private String nombre;

  public SubCategoria(Categoria categoria, String nombre) {

    validarSubCategoria(categoria, nombre);

    this.categoria = categoria;
    this.nombre = nombre;
  }

  private void validarSubCategoria(
          Categoria categoria,
          String nombre) {

    if (categoria == null) {
      throw new IllegalArgumentException(
              "La subcategoría debe pertenecer a una categoría.");
    }

    if (nombre == null || nombre.trim().isEmpty()) {
      throw new IllegalArgumentException(
              "La subcategoría debe tener un nombre.");
    }
  }
}
