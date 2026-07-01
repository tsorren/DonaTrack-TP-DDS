package grupo5.donaciones.models.entities.itemsNormalizados;

import grupo5.common.repositories.AggregateRoot;
import java.util.UUID;
import lombok.Getter;

@Getter
public class ItemDonacionNormalizado implements AggregateRoot {
  private final UUID id;
  private final UUID donacionOriginalId;
  private BienNormalizado bien;
  private final Integer cantidad;
  private boolean segmentado;

  public void marcarComoSegmentado() {
    this.segmentado = true;
  }

  public ItemDonacionNormalizado(UUID donacionOriginalId, BienNormalizado bien, Integer cantidad) {
    this.id = UUID.randomUUID();
    this.donacionOriginalId = donacionOriginalId;
    this.bien = bien;
    this.cantidad = cantidad;
    this.segmentado = false;
  }

  public Double getPesoTotal() {
    return bien.pesoUnitario() * cantidad;
  }

  public Double getVolumenTotal() {
    return bien.volumenUnitario() * cantidad;
  }

  public void actualizarBien(BienNormalizado nuevoBien) {
    this.bien = nuevoBien;
  }
}
