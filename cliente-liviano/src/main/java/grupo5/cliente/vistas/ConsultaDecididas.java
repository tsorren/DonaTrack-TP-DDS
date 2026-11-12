package grupo5.cliente.vistas;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * La búsqueda de Todas las decididas (R11-35, ficha r11-p2-admin-bandeja §4.3), ya entendida:
 * {@code buscar} (hasta 100 caracteres), {@code estado} ({@code aprobadas}, {@code descartadas} o
 * ''), {@code categoria} (la clave de la categoría o ''), {@code decidida} (un período fijo: {@code
 * hoy}, {@code 7-dias}, {@code 30-dias}, {@code este-mes}, {@code mes-anterior} o ''), {@code
 * decidio} ({@code vos}, {@code antes}, el id de una cuenta o '') y {@code orden} ({@code antiguas}
 * o '', las más recientes primero). {@code vos} es la cuenta de la sesión ('' si no se sabe). Un
 * valor que no se entiende es el de siempre.
 */
public record ConsultaDecididas(
    String buscar,
    String estado,
    String categoria,
    String decidida,
    String decidio,
    String orden,
    String vos) {

  public static final String APROBADAS = "aprobadas";
  public static final String DESCARTADAS = "descartadas";
  public static final String VOS = "vos";
  public static final String ANTES = "antes";
  public static final String ANTIGUAS = "antiguas";

  /** Quien decidió sin cuenta, como lo entiende Donaciones (junta las que no tienen autor). */
  public static final String SISTEMA = "SISTEMA";

  private static final int LARGO = 100;
  private static final String CLAVE = "[A-Za-z0-9-]{1,64}";
  private static final Set<String> ESTADOS = Set.of(APROBADAS, DESCARTADAS);
  private static final Set<String> PERIODOS =
      Set.of("hoy", "7-dias", "30-dias", "este-mes", "mes-anterior");

  /** Las opciones de la pastilla "Decidida": la primera es "Cualquier fecha". */
  public static final List<AdminAsignacionesVista.Opcion> PERIODOS_OPCIONES =
      List.of(
          new AdminAsignacionesVista.Opcion("", "Cualquier fecha"),
          new AdminAsignacionesVista.Opcion("hoy", "Hoy"),
          new AdminAsignacionesVista.Opcion("7-dias", "Últimos 7 días"),
          new AdminAsignacionesVista.Opcion("30-dias", "Últimos 30 días"),
          new AdminAsignacionesVista.Opcion("este-mes", "Este mes"),
          new AdminAsignacionesVista.Opcion("mes-anterior", "El mes anterior"));

  public ConsultaDecididas {
    buscar = buscar == null ? "" : buscar.strip();
    buscar = buscar.length() > LARGO ? buscar.substring(0, LARGO) : buscar;
    estado = estado != null && ESTADOS.contains(estado) ? estado : "";
    categoria = categoria != null && categoria.matches(CLAVE) ? categoria : "";
    decidida = decidida != null && PERIODOS.contains(decidida) ? decidida : "";
    decidio = decidio != null && decidio.matches(CLAVE) ? decidio : "";
    orden = ANTIGUAS.equals(orden) ? orden : "";
    vos = vos == null ? "" : vos.strip();
  }

  /** Sin búsqueda ni filtros. */
  public static ConsultaDecididas ninguna() {
    return new ConsultaDecididas("", "", "", "", "", "", "");
  }

  /** Si hay búsqueda o algún filtro (el orden no cuenta). */
  public boolean filtrada() {
    return !buscar.isEmpty()
        || !estado.isEmpty()
        || !categoria.isEmpty()
        || !decidida.isEmpty()
        || !decidio.isEmpty();
  }

  /** El id de la cuenta que se pide a Donaciones; null si no se filtra por quién decidió. */
  public String autor() {
    return switch (decidio) {
      case "" -> null;
      case VOS -> vos.isEmpty() ? null : vos;
      case ANTES -> SISTEMA;
      default -> decidio;
    };
  }

  /** Desde y hasta del período elegido (días inclusivos); los dos null si no hay período. */
  public LocalDate[] periodo(LocalDate hoy) {
    return switch (decidida) {
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
