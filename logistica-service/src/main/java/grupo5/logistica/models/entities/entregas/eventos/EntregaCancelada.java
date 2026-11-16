package grupo5.logistica.models.entities.entregas.eventos;

import java.util.UUID;
import lombok.Getter;

/**
 * Evento de dominio emitido por {@code Entrega} cuando se cancela antes de salir (HB-19, ADR
 * 20261109-cancelacion-de-entregas-que-no-salieron).
 */
@Getter
public final class EntregaCancelada extends EventoEntrega {

  private final UUID entregaId;
  private final UUID donacionId;
  private final String motivo;

  public EntregaCancelada(UUID entregaId, UUID donacionId, String motivo) {
    this.entregaId = entregaId;
    this.donacionId = donacionId;
    this.motivo = motivo;
  }
}
