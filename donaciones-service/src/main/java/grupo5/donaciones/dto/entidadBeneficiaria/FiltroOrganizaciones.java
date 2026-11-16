package grupo5.donaciones.dto.entidadBeneficiaria;

/**
 * Los filtros de Organizaciones del admin (R11-43). Nulos no filtran.
 *
 * <p>{@code activa}: {@code true}, las activas; {@code false}, las de baja. {@code conNecesidades}:
 * {@code true}, las que tienen alguna necesidad activa y no vencida hoy; {@code false}, las que no
 * tienen ninguna. {@code rubro}: igual al rubro de la jurídica, sin distinguir mayúsculas ni tildes
 * (una sin rubro no aparece con ninguno).
 */
public record FiltroOrganizaciones(Boolean activa, Boolean conNecesidades, String rubro) {

  public static final FiltroOrganizaciones NINGUNO = new FiltroOrganizaciones(null, null, null);

  /** Un rubro en blanco no filtra. */
  public FiltroOrganizaciones {
    rubro = rubro == null || rubro.isBlank() ? null : rubro.trim();
  }
}
