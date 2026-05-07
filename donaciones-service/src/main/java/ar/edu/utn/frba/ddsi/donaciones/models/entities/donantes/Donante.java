package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import ar.edu.utn.frba.ddsi.common.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import lombok.Data;

@Data
public class Donante {
  private Persona persona;
  private java.util.List<Donacion> historialDonaciones = new java.util.ArrayList<>();

  public void agregarDonacion(Donacion donacion) {
    this.historialDonaciones.add(donacion);
  }

  public void quitarDonacion(Donacion donacion) {
    this.historialDonaciones.remove(donacion);
  }
}
