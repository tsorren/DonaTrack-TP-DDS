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

  public Donante(Persona persona) {

    if (persona == null) {
      throw new IllegalArgumentException(
              "El donante debe estar asociado a una persona.");
    }
    this.persona = persona;
  }

  public void agregarDonacion(Donacion donacion) {
    if (donacion == null) {
      throw new IllegalArgumentException(
              "La donación no puede ser nula.");
    }

    if (this.historialDonaciones.contains(donacion)) {
      throw new IllegalArgumentException(
              "La donación ya pertenece al historial del donante.");
    }
    this.historialDonaciones.add(donacion);
  }

  // Lanzar excepcion si la donacion no esta en la lista
  public void quitarDonacion(Donacion donacion) {
    if (!this.historialDonaciones.contains(donacion)) {
      throw new IllegalArgumentException(
              "La donación no pertenece al historial del donante.");
    }
    this.historialDonaciones.remove(donacion);
  }
}
