package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Donacion {
  private List<DonacionIndependiente> donaciones = new ArrayList<>();
  private Donante donante;

  public void agregarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
    this.donaciones.add(donacionIndependiente);
  }

  // Lanzar excepcion si la donacion independiente no esta en la lista
  public void quitarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
    this.donaciones.remove(donacionIndependiente);
  }
}
