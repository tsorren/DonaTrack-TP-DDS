package grupo5.donaciones.models.repositories.listados;

import grupo5.donaciones.dto.entidadBeneficiaria.FiltroOrganizaciones;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * Búsqueda de entidades beneficiarias por razón social (HB-15), ordenada por razón social. Con
 * {@code soloActivas}, las de baja no aparecen (el listado público).
 *
 * <p>R11-12: con {@code categoriaId}, solo las que piden algo de esa categoría (una necesidad
 * activa y no vencida en {@code hoy} de una de sus subcategorías). Con {@code donadasPor}, solo las
 * que recibieron al menos una donación de ese donante, en cualquier estado. Nulos no filtran.
 *
 * <p>R11-43: {@code filtro}, los de Organizaciones del admin (estado, con necesidades y rubro).
 */
public record CriterioEntidades(
    String nombre,
    boolean soloActivas,
    UUID categoriaId,
    LocalDate hoy,
    UUID donadasPor,
    FiltroOrganizaciones filtro,
    Paginado paginado) {

  /** Sin {@code hoy}, los filtros por categoría y por necesidades miran el día de hoy. */
  public CriterioEntidades {
    if (filtro == null) {
      filtro = FiltroOrganizaciones.NINGUNO;
    }
    if ((categoriaId != null || filtro.conNecesidades() != null) && hoy == null) {
      hoy = LocalDate.now(ZoneId.systemDefault());
    }
  }

  /** Sin los filtros de Organizaciones: el listado público y el del donante. */
  public CriterioEntidades(
      String nombre,
      boolean soloActivas,
      UUID categoriaId,
      LocalDate hoy,
      UUID donadasPor,
      Paginado paginado) {
    this(nombre, soloActivas, categoriaId, hoy, donadasPor, FiltroOrganizaciones.NINGUNO, paginado);
  }

  /** Solo por nombre: el listado del admin sin filtros y el público sin categoría. */
  public CriterioEntidades(String nombre, boolean soloActivas, Paginado paginado) {
    this(nombre, soloActivas, null, null, null, paginado);
  }
}
