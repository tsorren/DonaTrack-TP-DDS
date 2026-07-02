package grupo5.donaciones.models.entities.donaciones;

import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;

public record ItemDonacion(Bien bien, Integer cantidad) {
  public ItemDonacion {
    validarItemDonacion(bien, cantidad);
  }

  private static void validarItemDonacion(Bien bien, Integer cantidad) {
    if (bien == null) {
      throw new ValidationException(ErrorCatalog.ITEM_DONACION_SIN_BIEN);
    }
    if (cantidad == null || cantidad <= 0) {
      throw new ValidationException(ErrorCatalog.ITEM_DONACION_CANTIDAD_INVALIDA);
    }
  }

  public Double getPesoTotal() {
    return bien.pesoUnitario() * cantidad;
  }

  public Double getVolumenTotal() {
    return bien.volumenUnitario() * cantidad;
  }
}
