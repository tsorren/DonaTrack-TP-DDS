package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.bienes.Bien;
import lombok.Data;

@Data
public class ItemDonacion {
  private Bien bien;
  private Integer cantidad;

  public ItemDonacion(Bien bien, Integer cantidad) {

    validarItemDonacion(bien, cantidad);

    this.bien = bien;
    this.cantidad = cantidad;
  }

  private void validarItemDonacion(Bien bien, Integer cantidad) {

    if (bien == null) {
      throw new IllegalArgumentException("El item de donación debe tener un bien asociado.");
    }

    if (cantidad == null || cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad del item debe ser mayor a cero.");
    }
  }
}
