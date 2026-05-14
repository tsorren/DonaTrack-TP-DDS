package ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes;

import lombok.Data;

@Data
public class SubCategoria {
  private Categoria categoria;
  private String nombre;
}
