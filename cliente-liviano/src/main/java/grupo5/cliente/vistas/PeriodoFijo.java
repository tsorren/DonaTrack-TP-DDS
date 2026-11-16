package grupo5.cliente.vistas;

import java.time.LocalDate;
import java.util.List;

/**
 * Los períodos fijos de los filtros de fecha del admin (ficha r11-p2-admin-bandeja §4, como en
 * Importaciones): su clave en la dirección, su texto y el desde y hasta que dan, en días
 * inclusivos. Elegir dos fechas a mano llega con el componente de filtros nuevo (R11-B04).
 */
public final class PeriodoFijo {

  /** Una opción: su clave en la dirección ('' es "Cualquier fecha") y su texto. */
  public record Opcion(String clave, String texto) {}

  /** Las opciones, en el orden del menú. */
  public static final List<Opcion> OPCIONES =
      List.of(
          new Opcion("", "Cualquier fecha"),
          new Opcion("hoy", "Hoy"),
          new Opcion("7-dias", "Últimos 7 días"),
          new Opcion("30-dias", "Últimos 30 días"),
          new Opcion("este-mes", "Este mes"),
          new Opcion("mes-anterior", "El mes anterior"));

  private PeriodoFijo() {}

  /** {@code clave} si es un período fijo; si no, '' (cualquier fecha). */
  public static String valida(String clave) {
    return clave != null && OPCIONES.stream().anyMatch(o -> o.clave().equals(clave)) ? clave : "";
  }

  /** Desde y hasta del período (días inclusivos); los dos null si no hay período. */
  public static LocalDate[] de(String clave, LocalDate hoy) {
    return switch (clave == null ? "" : clave) {
      case "hoy" -> new LocalDate[] {hoy, hoy};
      case "7-dias" -> new LocalDate[] {hoy.minusDays(6), hoy};
      case "30-dias" -> new LocalDate[] {hoy.minusDays(29), hoy};
      case "este-mes" -> new LocalDate[] {hoy.withDayOfMonth(1), hoy};
      case "mes-anterior" -> {
        LocalDate primero = hoy.withDayOfMonth(1).minusMonths(1);
        yield new LocalDate[] {primero, primero.withDayOfMonth(primero.lengthOfMonth())};
      }
      default -> new LocalDate[2];
    };
  }
}
