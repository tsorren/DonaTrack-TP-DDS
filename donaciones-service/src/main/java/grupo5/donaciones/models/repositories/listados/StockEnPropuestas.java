package grupo5.donaciones.models.repositories.listados;

import java.util.UUID;

/**
 * Una donación independiente frente a las propuestas pendientes (R11-24): lo que queda en el
 * depósito ({@code disponible}: 0 si ya no está), cuánto piden de ella todas las pendientes y en
 * cuántas pendientes aparece.
 */
public record StockEnPropuestas(
    UUID donacionId, int disponible, int pedidaEnPendientes, int propuestasPendientes) {

  /** Alcanza para todas las pendientes que la piden. */
  public boolean alcanza() {
    return pedidaEnPendientes <= disponible;
  }
}
