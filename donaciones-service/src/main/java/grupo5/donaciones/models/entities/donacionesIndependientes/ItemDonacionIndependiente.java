package grupo5.donaciones.models.entities.donacionesIndependientes;

import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import java.util.UUID;

/**
 * {@code itemNormalizadoId}: el ítem normalizado del que salió (D-24). Es la referencia con la que
 * se reconstituye el {@link BienNormalizado} al leerlo de la base.
 */
public record ItemDonacionIndependiente(
    UUID itemNormalizadoId, BienNormalizado bien, Integer cantidad) {

  public ItemDonacionIndependiente {
    validarItemDonacion(itemNormalizadoId, bien, cantidad);
  }

  private static void validarItemDonacion(
      UUID itemNormalizadoId, BienNormalizado bien, Integer cantidad) {
    if (itemNormalizadoId == null) {
      throw new ValidationException(ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_SIN_ITEM_NORMALIZADO);
    }
    if (bien == null) {
      throw new ValidationException(ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_SIN_BIEN);
    }
    if (cantidad == null || cantidad <= 0) {
      throw new ValidationException(ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_CANTIDAD_INVALIDA);
    }
  }

  public Double getPesoTotal() {
    return bien.pesoUnitario() * cantidad;
  }

  public Double getVolumenTotal() {
    return bien.volumenUnitario() * cantidad;
  }

  public ItemDonacionIndependiente fragmentarse(Integer cantidadNecesitada) {
    if (this.cantidad() <= cantidadNecesitada) {
      throw new BusinessStateException(
          ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_FRAGMENTACION_INVALIDA);
    }
    return new ItemDonacionIndependiente(this.itemNormalizadoId, this.bien, cantidadNecesitada);
  }
}
