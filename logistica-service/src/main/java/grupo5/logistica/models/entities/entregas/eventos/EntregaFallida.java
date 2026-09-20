package grupo5.logistica.models.entities.entregas.eventos;

import java.util.UUID;
import lombok.Getter;

/** Evento de dominio emitido por {@code Entrega} cuando la entrega no puede concretarse. */
@Getter
public final class EntregaFallida extends EventoEntrega {

  private final UUID entregaId;
  private final UUID donacionId;
  private final String justificacion;
  private final boolean replanificable;

  public EntregaFallida(
      UUID entregaId, UUID donacionId, String justificacion, boolean replanificable) {
    this.entregaId = entregaId;
    this.donacionId = donacionId;
    this.justificacion = justificacion;
    this.replanificable = replanificable;
  }
}
