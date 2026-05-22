package grupo5.donaciones.models.entities.donaciones;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Donacion {
  private List<DonacionIndependiente> donaciones = new ArrayList<>();
  private Donante donante;

  public Donacion(Donante donante) {

    if (donante == null) {
      throw new IllegalArgumentException("La donación debe tener un donante asociado.");
    }

    this.donante = donante;
  }

  public void agregarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
    if (donacionIndependiente == null) {
      throw new IllegalArgumentException("La donación independiente no puede ser nula.");
    }

    if (this.donaciones.contains(donacionIndependiente)) {
      throw new IllegalArgumentException("La donación independiente ya fue agregada.");
    }

    this.donaciones.add(donacionIndependiente);
  }

  // Lanzar excepcion si la donacion independiente no esta en la lista
  public void quitarDonacionIndependiente(DonacionIndependiente donacionIndependiente) {
    if (!this.donaciones.contains(donacionIndependiente)) {
      throw new IllegalArgumentException("La donación independiente no pertenece a la donación.");
    }

    this.donaciones.remove(donacionIndependiente);
  }
}
