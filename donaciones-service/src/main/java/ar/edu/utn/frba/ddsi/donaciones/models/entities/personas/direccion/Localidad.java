package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.direccion;

import lombok.Data;

@Data
public class Localidad {
  private String nombre;
  private Provincia provincia;
}
