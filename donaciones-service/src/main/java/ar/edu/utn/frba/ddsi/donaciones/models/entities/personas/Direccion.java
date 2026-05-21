package ar.edu.utn.frba.ddsi.donaciones.models.entities.personas;

import ar.edu.utn.frba.ddsi.donaciones.models.privacidad.Anonimizable;
import lombok.Data;

@Data
public class Direccion implements Anonimizable {
  private String calle;
  private Integer altura;
  private Integer piso;
  private String departamento;
  private String zona;
  private String localidad;

  @Override
  public void anonimizar() {
    this.calle = "ANONIMIZADO";
    this.altura = 0; // O un valor nulo/default
    this.localidad = "ANONIMIZADO";
  }
}
