package grupo5.donaciones.dto.entidadBeneficiaria;

/**
 * El {@code estado} que acepta {@code GET /api/entidades} (R11-43): activa o de baja. Un valor que
 * no es uno de estos da 400.
 */
public enum EstadoOrganizacion {
  ACTIVA,
  BAJA;

  /** {@code true} para las activas, {@code false} para las de baja y nulo sin estado. */
  public static Boolean activa(EstadoOrganizacion estado) {
    return estado == null ? null : estado == ACTIVA;
  }
}
