package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class DonacionIndependiente {
  private String descripcion;
  private List<ItemDonacion> items = new ArrayList<>();

  public void agregarItem(ItemDonacion item) {
    this.items.add(item);
  }

  // Lanzar excepcion si el item no esta en la lista
  public void quitarItem(ItemDonacion bien) {
    this.items.remove(bien);
  }
}
