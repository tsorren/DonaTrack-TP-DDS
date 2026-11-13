package grupo5.donaciones.services.logistica;

/** Qué comando lleva una entrada del outbox del broker. */
public enum TipoEntradaOutbox {
  /** Pedido de entrega ({@code entrega.solicitada.<proveedorId>.v1}). */
  SOLICITUD,
  /**
   * Cancelación de una entrega que no salió ({@code entrega.cancelada.<proveedorId>.v1}, HB-19).
   */
  CANCELACION
}
