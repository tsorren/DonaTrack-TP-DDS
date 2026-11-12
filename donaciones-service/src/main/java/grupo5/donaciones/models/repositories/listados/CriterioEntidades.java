package grupo5.donaciones.models.repositories.listados;

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
 */
public record CriterioEntidades(
    String nombre,
    boolean soloActivas,
    UUID categoriaId,
    LocalDate hoy,
    UUID donadasPor,
    Paginado paginado) {

  /** Sin {@code hoy}, el filtro por categoría mira el día de hoy. */
  public CriterioEntidades {
    if (categoriaId != null && hoy == null) {
      hoy = LocalDate.now(ZoneId.systemDefault());
    }
  }

  /** Solo por nombre: el listado del admin y el público sin categoría. */
  public CriterioEntidades(String nombre, boolean soloActivas, Paginado paginado) {
    this(nombre, soloActivas, null, null, null, paginado);
  }
}
