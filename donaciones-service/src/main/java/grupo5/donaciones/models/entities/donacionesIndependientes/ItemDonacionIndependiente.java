package grupo5.donaciones.models.entities.donacionesIndependientes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import grupo5.common.exceptions.BusinessStateException;
import grupo5.common.exceptions.ErrorCatalog;
import grupo5.common.exceptions.ValidationException;
import grupo5.donaciones.models.entities.itemsNormalizados.BienNormalizado;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemDonacionIndependiente {
  @JsonIgnore private DonacionIndependiente donacionIndependiente;
  private BienNormalizado bien;
  private Integer cantidad;

  public ItemDonacionIndependiente(BienNormalizado bien, Integer cantidad) {

    validarItemDonacion(bien, cantidad);

    this.bien = bien;
    this.cantidad = cantidad;
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
    if (this.getCantidad() <= cantidadNecesitada) {
      throw new BusinessStateException(
          ErrorCatalog.ITEM_DONACION_INDEPENDIENTE_FRAGMENTACION_INVALIDA);
    }
    this.cantidad -= cantidadNecesitada;

    return new ItemDonacionIndependiente(this.bien, cantidadNecesitada);
  }
}
