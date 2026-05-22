package grupo5.donaciones.models.entities.donaciones;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DonacionIndependiente {
  private String descripcion;
  private List<ItemDonacion> items = new ArrayList<>();

  public DonacionIndependiente(String descripcion) {

    if (descripcion == null || descripcion.trim().isEmpty()) {
      throw new IllegalArgumentException(
          "La descripción de la donación independiente no puede estar vacía.");
    }

    this.descripcion = descripcion;
  }

  public void agregarItem(ItemDonacion item) {
    if (item == null) {
      throw new IllegalArgumentException("El ítem a agregar no puede ser nulo.");
    }
    this.items.add(item);
  }

  // Lanzar excepcion si el item no esta en la lista
  public void quitarItem(ItemDonacion bien) {
    if (!this.items.contains(bien)) {
      throw new IllegalArgumentException(
          "El ítem que intenta quitar no pertenece a esta donación.");
    }
    this.items.remove(bien);
  }

  public int getCantidad() {
    return this.items.stream().mapToInt(ItemDonacion::getCantidad).sum();
  }
}
