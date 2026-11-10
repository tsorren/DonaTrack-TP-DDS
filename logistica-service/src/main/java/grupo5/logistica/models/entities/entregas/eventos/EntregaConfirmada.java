package grupo5.logistica.models.entities.entregas.eventos;

import grupo5.logistica.models.entities.entregas.ReporteRecepcion;
import java.util.List;
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

  /** URLs de las fotos de recepción, en el orden de la entrega (ADR de réplicas por evento). */
  private final List<String> fotosRecepcion;

  public EntregaConfirmada(UUID entregaId, UUID donacionId, UUID idRuta) {
    this(entregaId, donacionId, idRuta, null);
  }

  public EntregaConfirmada(UUID entregaId, UUID donacionId, UUID idRuta, ReporteRecepcion reporte) {
    this(entregaId, donacionId, idRuta, reporte, List.of());
  }

  public EntregaConfirmada(
      UUID entregaId,
      UUID donacionId,
      UUID idRuta,
      ReporteRecepcion reporte,
      List<String> fotosRecepcion) {
    this.entregaId = entregaId;
    this.donacionId = donacionId;
    this.idRuta = idRuta;
    this.reporte = reporte;
    this.fotosRecepcion = fotosRecepcion == null ? List.of() : List.copyOf(fotosRecepcion);
  }
}
