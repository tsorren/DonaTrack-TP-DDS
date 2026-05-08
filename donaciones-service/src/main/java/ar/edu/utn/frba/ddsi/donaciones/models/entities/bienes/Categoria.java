package ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes;

import lombok.Data;

@Data
public class Categoria {
  private String nombre;
  private Boolean conUso;
  private Boolean conVencimiento;
  private Unidad tipoUnidad;
}
