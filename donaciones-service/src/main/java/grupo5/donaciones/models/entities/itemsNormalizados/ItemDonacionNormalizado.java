package grupo5.donaciones.models.entities.itemsNormalizados;

import grupo5.donaciones.models.entities.donaciones.Donacion;
import lombok.Getter;

@Getter
public class ItemDonacionNormalizado {
  private final Donacion donacionOriginal;
  private final BienNormalizado bien;
  private final Integer cantidad;

  public ItemDonacionNormalizado(
      Donacion donacionOriginal, BienNormalizado bien, Integer cantidad) {
    this.donacionOriginal = donacionOriginal;
    this.bien = bien;
    this.cantidad = cantidad;
  }
}
