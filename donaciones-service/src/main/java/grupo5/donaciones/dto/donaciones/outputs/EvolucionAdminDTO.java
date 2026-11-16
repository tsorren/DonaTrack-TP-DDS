package grupo5.donaciones.dto.donaciones.outputs;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Los gráficos del inicio del admin (R11-42): las donaciones registradas y las entregas confirmadas
 * de los últimos 12 meses, contando el actual, por mes y por categoría, contadas en la base.
 *
 * @param desde primer mes ({@code AAAA-MM}): hace 11 meses, o el de la primera donación si es
 *     posterior; {@code null} si no hay ninguna
 * @param hasta el mes en curso; {@code null} si no hay ninguna donación
 * @param meses uno por mes de {@code desde} a {@code hasta}, todos presentes (0 si no hay); vacía
 *     sin donaciones
 * @param categorias todas las del catálogo, de más a menos registradas (y por nombre), como mucho 8
 * @param otras la suma de las categorías que pasan del tope, o {@code null} si no hay
 * @param calculadoEn momento del cálculo
 */
public record EvolucionAdminDTO(
    String desde,
    String hasta,
    List<Mes> meses,
    List<Categoria> categorias,
    Otras otras,
    OffsetDateTime calculadoEn) {

  /** {@code entregadas}: las que pasaron a ENTREGADA en el mes. */
  public record Mes(String mes, long registradas, long entregadas) {}

  public record Categoria(UUID categoriaId, String nombre, long registradas, long entregadas) {}

  /** {@code categorias}: cuántas se suman. */
  public record Otras(int categorias, long registradas, long entregadas) {}
}
