package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemDonacion {
  private Bien bien;
  private Integer cantidad;

  public ItemDonacion(Bien bien, Integer cantidad) {

    validarItemDonacion(bien, cantidad);

    this.bien = bien;
    this.cantidad = cantidad;
  }

  private static void validarItemDonacion(Bien bien, Integer cantidad) {

    if (bien == null) {
      throw new ValidationException(ErrorCatalog.ITEM_DONACION_SIN_BIEN);
    }

    if (cantidad == null || cantidad <= 0) {
      throw new ValidationException(ErrorCatalog.ITEM_DONACION_CANTIDAD_INVALIDA);
    }
  }
}
