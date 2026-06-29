package grupo5.donaciones.models.entities.itemsNormalizados;

import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ItemDonacionNormalizado implements AggregateRoot {
  private final UUID id;
  private final UUID donacionOriginalId;
  private BienNormalizado bien;
  private final Integer cantidad;
  @Setter private boolean segmentado;

  public ItemDonacionNormalizado(UUID donacionOriginalId, BienNormalizado bien, Integer cantidad) {
    this.id = UUID.randomUUID();
    this.donacionOriginalId = donacionOriginalId;
    this.bien = bien;
    this.cantidad = cantidad;
    this.segmentado = false;
  }

  public void actualizarBien(BienNormalizado nuevoBien) {
    this.bien = nuevoBien;
  }
}
