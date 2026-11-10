package grupo5.logistica.models.entities.entregas.eventos;

import grupo5.logistica.models.entities.entregas.ReporteRecepcion;
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

  /** Problema contado al confirmar; {@code null} si llegó bien. */
  private final ReporteRecepcion reporte;

  public EntregaConfirmada(UUID entregaId, UUID donacionId, UUID idRuta) {
    this(entregaId, donacionId, idRuta, null);
  }

  public EntregaConfirmada(UUID entregaId, UUID donacionId, UUID idRuta, ReporteRecepcion reporte) {
    this.entregaId = entregaId;
    this.donacionId = donacionId;
    this.idRuta = idRuta;
    this.reporte = reporte;
  }
}
