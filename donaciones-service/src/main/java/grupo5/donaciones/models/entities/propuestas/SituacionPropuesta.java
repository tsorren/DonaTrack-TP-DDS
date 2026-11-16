package grupo5.donaciones.models.entities.propuestas;

import java.util.List;

/**
 * Cómo está una propuesta pendiente respecto de las otras pendientes y del depósito (R11-24):
 * {@code LIBRE} (lo que pide sigue en el depósito y alcanza aunque se aprueben las demás), {@code
 * EN_CONFLICTO} (comparte una donación que no alcanza para todas, o cubre la misma necesidad que
 * otra pendiente) o {@code SIN_STOCK} (a alguna de sus donaciones ya no le queda lo que pide).
 */
public enum SituacionPropuesta {
  LIBRE,
  EN_CONFLICTO,
  SIN_STOCK;

  /**
   * La regla de la spec R11-24 (§3.1) para una pendiente: {@code pedidas} es, por cada
   * fragmentación, lo que pide, lo que queda en el depósito de esa donación y lo que piden de ella
   * las otras pendientes.
   */
  public static SituacionPropuesta de(List<Pedido> pedidas, int otrasMismaNecesidad) {
    if (pedidas.stream().anyMatch(p -> p.cantidad() > p.disponible())) {
      return SIN_STOCK;
    }
    boolean comparte =
        pedidas.stream().anyMatch(p -> p.cantidad() > p.disponible() - p.pedidaEnOtras());
    return comparte || otrasMismaNecesidad > 0 ? EN_CONFLICTO : LIBRE;
  }

  /** Lo que una fragmentación pide de una donación, lo que queda y lo que piden las otras. */
  public record Pedido(int cantidad, int disponible, int pedidaEnOtras) {}
}
