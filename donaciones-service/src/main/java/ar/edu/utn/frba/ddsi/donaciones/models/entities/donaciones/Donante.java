package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.personas.Persona;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donante {
  private Persona persona;
  private List<Donacion> historialDonaciones = new ArrayList<>();

  public void agregarDonacion(Donacion donacion) {
    this.historialDonaciones.add(donacion);
  }

  // Lanzar excepcion si la donacion no esta en la lista
  public void quitarDonacion(Donacion donacion) {
    this.historialDonaciones.remove(donacion);
  }
}
