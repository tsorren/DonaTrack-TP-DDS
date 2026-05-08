package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.Persona;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import java.util.ArrayList;
import lombok.Data;

@Data
public class Donante {
  private Persona persona;
  private java.util.List<Donacion> historialDonaciones = new ArrayList<>();

  public void agregarDonacion(Donacion donacion) {
    this.historialDonaciones.add(donacion);
  }

  // Lanzar excepcion si la donacion no esta en la lista
  public void quitarDonacion(Donacion donacion) {
    this.historialDonaciones.remove(donacion);
  }
}
