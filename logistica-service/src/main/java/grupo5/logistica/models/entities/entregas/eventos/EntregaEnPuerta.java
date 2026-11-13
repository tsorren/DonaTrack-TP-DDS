package grupo5.logistica.models.entities.entregas.eventos;

import java.util.UUID;
import lombok.Getter;

/**
 * Evento de dominio emitido por {@code Entrega} cuando el camión llega al destino (HB-22, ADR
 * 20261111-estado-en-puerta-de-la-entrega).
 */
@Getter
public final class EntregaEnPuerta extends EventoEntrega {

  private final UUID entregaId;
  private final UUID donacionId;

  public EntregaEnPuerta(UUID entregaId, UUID donacionId) {
    this.entregaId = entregaId;
    this.donacionId = donacionId;
  }
}
