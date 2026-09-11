package grupo5.logistica.models.entities.entregas.eventos;

import java.util.UUID;
import lombok.Getter;

/**
 * Evento de dominio emitido por {@code Entrega} cuando la entidad beneficiaria confirma la
 * recepción.
 */
@Getter
public final class EntregaConfirmada extends EventoEntrega {

  private final UUID entregaId;
  private final UUID donacionId;
  private final UUID idRuta;

  public EntregaConfirmada(UUID entregaId, UUID donacionId, UUID idRuta) {
    this.entregaId = entregaId;
    this.donacionId = donacionId;
    this.idRuta = idRuta;
  }
}
