package grupo5.donaciones.dto;

/**
 * El avance de una necesidad en sus unidades (HG-04): {@code llegaron} (las donaciones {@code
 * ENTREGADA}, menos lo que no llegó), {@code asignadas} (con destino y todavía sin llegar, también
 * las de una entrega fallida que conservan el destino hasta volver al depósito) y {@code
 * sinAsignar} (lo que falta asignar para el objetivo; nulo si no hay objetivo). Solo de salida.
 */
public record AvanceNecesidadDTO(int llegaron, int asignadas, Integer sinAsignar) {

  /**
   * @param objetivo la cantidad pedida (la del período, en una recurrente)
   * @param asignado lo que cuenta para la necesidad: lo enviado menos lo que no llegó
   * @param sinLlegar lo que tiene destino y todavía no está {@code ENTREGADA}
   */
  public static AvanceNecesidadDTO de(Integer objetivo, int asignado, int sinLlegar) {
    return new AvanceNecesidadDTO(
        Math.max(0, asignado - sinLlegar),
        sinLlegar,
        objetivo == null ? null : Math.max(0, objetivo - asignado));
  }
}
