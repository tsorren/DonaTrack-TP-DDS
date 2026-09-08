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
    this(UUID.randomUUID(), donacionOriginalId, bien, cantidad, false);
  }

  public ItemDonacionNormalizado(
      UUID id,
      UUID donacionOriginalId,
      BienNormalizado bien,
      Integer cantidad,
      boolean segmentado) {
    if (id == null) {
      throw new IllegalArgumentException("El id del ítem normalizado no puede ser nulo");
    }
    this.id = id;
    this.donacionOriginalId = donacionOriginalId;
    this.bien = bien;
    this.cantidad = cantidad;
    this.segmentado = segmentado;
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

  public boolean estaPendienteDeRevision() {
    return this.bien != null
        && this.bien.estadoNormalizacion() == EstadoNormalizacion.PENDIENTE_REVISION;
  }

  public boolean estaResuelto() {
    return !estaPendienteDeRevision();
  }

  public boolean estaAceptado() {
    return this.bien != null && this.bien.estadoNormalizacion() == EstadoNormalizacion.ACEPTADO;
  }
}
