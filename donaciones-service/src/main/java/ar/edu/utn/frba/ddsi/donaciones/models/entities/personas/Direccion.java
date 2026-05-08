package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas;

import lombok.Data;

@Data
public class Direccion {
  private String calle;
  private Integer altura;
  private Integer piso;
  private String departamento;
  private String zona;
  private String localidad;
}
