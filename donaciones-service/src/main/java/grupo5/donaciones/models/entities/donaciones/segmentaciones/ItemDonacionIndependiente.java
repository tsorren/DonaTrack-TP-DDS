package grupo5.donaciones.models.entities.donaciones.segmentaciones;

import grupo5.donaciones.models.entities.bienes.Bien;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemDonacionIndependiente {
  private DonacionIndependiente donacionIndependiente;
  private Bien bien;
  private Integer cantidad;

  public ItemDonacionIndependiente(Bien bien, Integer cantidad) {

    validarItemDonacion(bien, cantidad);

    this.bien = bien;
    this.cantidad = cantidad;
  }

  private static void validarItemDonacion(Bien bien, Integer cantidad) {

    if (bien == null) {
      throw new IllegalArgumentException("El item de donación debe tener un bien asociado.");
    }

    if (cantidad == null || cantidad <= 0) {
      throw new IllegalArgumentException("La cantidad del item debe ser mayor a cero.");
    }
  }

  public ItemDonacionIndependiente fragmentarse(Integer cantidadNecesitada) {
    if (this.getCantidad() <= cantidadNecesitada) {
      throw new RuntimeException();
    }
    this.cantidad -= cantidadNecesitada;

    return new ItemDonacionIndependiente(this.bien, cantidadNecesitada);
  }
}
