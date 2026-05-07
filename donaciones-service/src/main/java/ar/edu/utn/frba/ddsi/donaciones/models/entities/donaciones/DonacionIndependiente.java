package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import Bienes.Bien;
import lombok.Data;

@Data
public class DonacionIndependiente {
  private String descripcion;
  private java.util.List<ItemDonacion> bienes = new java.util.ArrayList<>();

  public void agregarBien(Bien bien) {
    ItemDonacion itemDonacion = new ItemDonacion();
    itemDonacion.setBien(bien);
    itemDonacion.setCantidad(1);
    this.bienes.add(itemDonacion);
  }

  public void quitarBien(Bien bien) {
    this.bienes.removeIf(itemDonacion -> itemDonacion.getBien().equals(bien));
  }

  public void agregarItemDonacion(ItemDonacion itemDonacion) {
    this.bienes.add(itemDonacion);
  }

  public void quitarItemDonacion(ItemDonacion itemDonacion) {
    this.bienes.remove(itemDonacion);
  }
}
