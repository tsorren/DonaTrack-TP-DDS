package grupo5.donaciones.dto.logistica;

import grupo5.donaciones.dto.comunicaciones.DestinoEventoDTO;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Modelo canónico del pedido de entrega que Donaciones le hace a cualquier proveedor de logística.
 * Cada adapter lo traduce al contrato de su proveedor (AMQP o HTTP).
 */
public record DatosEntregaLogistica(
    UUID donacionIndependienteId,
    UUID personaBeneficiariaId,
    DestinoEventoDTO destino,
    Double pesoTotalKG,
    Double volumenTotalM3,
    LocalDateTime fecha,
    UUID donanteId) {

  /**
   * Pedido sin donante. El {@code donanteId} es opcional: una entrada vieja del outbox lo trae en
   * {@code null} (ADR de réplicas por evento, HB-16).
   */
  public DatosEntregaLogistica(
      UUID donacionIndependienteId,
      UUID personaBeneficiariaId,
      DestinoEventoDTO destino,
      Double pesoTotalKG,
      Double volumenTotalM3,
      LocalDateTime fecha) {
    this(
        donacionIndependienteId,
        personaBeneficiariaId,
        destino,
        pesoTotalKG,
        volumenTotalM3,
        fecha,
        null);
  }

  /** Solo la donación: lo que lleva una cancelación en el outbox (HB-19). */
  public static DatosEntregaLogistica soloDonacion(UUID donacionIndependienteId) {
    return new DatosEntregaLogistica(donacionIndependienteId, null, null, null, null, null);
  }
}
