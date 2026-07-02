package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;

public record ItemDonacionIndependiente(BienNormalizado bien, Integer cantidad) {

  public ItemDonacionIndependiente {
    validarItemDonacion(bien, cantidad);
  }

  private static void validarItemDonacion(BienNormalizado bien, Integer cantidad) {
    if (bien == null) {
      throw new ValidationException(ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_SIN_BIEN);
    }
    if (cantidad == null || cantidad <= 0) {
      throw new ValidationException(ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_CANTIDAD_INVALIDA);
    }
  }

  public ItemDonacionIndependiente fragmentarse(Integer cantidadNecesitada) {
    if (this.cantidad() <= cantidadNecesitada) {
      throw new BusinessStateException(
          ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_FRAGMENTACION_INVALIDA);
    }
    return new ItemDonacionIndependiente(this.bien, cantidadNecesitada);
  }
}
