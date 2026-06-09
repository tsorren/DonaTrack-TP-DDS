package grupo5.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import grupo5.donaciones.models.entities.donantes.Donante;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Donacion {
  private Donante donante;
  private List<ItemDonacion> items;
  private String descripcion;
  private LocalDateTime fecha;

  public Donacion(Donante donante) {
    this.items = new ArrayList<>();
    if (donante == null) {
      throw new IllegalArgumentException("La donación debe tener un donante asociado.");
    }

    this.donante = donante;
  }

  public void agregarItem(ItemDonacion item) {
    if (item == null) {
      throw new IllegalArgumentException("La donación independiente no puede ser nula.");
    }

    if (this.items.contains(item)) {
      throw new IllegalArgumentException("La donación independiente ya fue agregada.");
    }

    this.items.add(item);
  }

  // Lanzar excepcion si la donacion independiente no esta en la lista
  public void quitarItem(ItemDonacion item) {
    if (!this.items.contains(item)) {
      throw new IllegalArgumentException("La donación independiente no pertenece a la donación.");
    }

    this.items.remove(item);
  }
}
