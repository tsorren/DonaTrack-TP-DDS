package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
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
      throw new IllegalArgumentException(
              "El item de donación no puede ser nulo.");
    }
    this.items.add(item);
  }

  // Lanzar excepcion si el item no esta en la lista
  public void quitarItem(ItemDonacion bien) {
    if (!this.items.contains(bien)) {
      throw new IllegalArgumentException(
              "El item no pertenece a la donación.");
    }
    this.items.remove(bien);
  }
}
