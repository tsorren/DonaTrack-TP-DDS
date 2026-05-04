package ar.edu.utn.frba.ddsi.donaciones.models.entities.necesidades;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NecesidadExtraordinaria extends Necesidad {

  private int cantidadObjetivo;
  private int cantidadAcumulada;

  @Override
  public boolean estaSatisfecha() {
    return cantidadAcumulada >= cantidadObjetivo;
  }
}
