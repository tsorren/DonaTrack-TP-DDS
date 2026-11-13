package grupo5.cliente.vistas;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Qué página de importaciones pide el admin (R11-20), ya entendida: {@code buscar}, parte del
 * nombre del archivo; {@code estado}, {@code en-curso}, {@code terminadas}, {@code con-errores},
 * {@code cortadas} o '' (todas); {@code subida}, {@code hoy}, {@code 7-dias}, {@code 30-dias},
 * {@code este-mes}, {@code mes-anterior}, {@code rango} (fechas escritas en la dirección) o ''
 * (cualquier fecha); {@code desde} y {@code hasta}, el rango de días de subida que sale de {@code
 * subida} (null: sin límite); {@code orden}, {@code antiguas} o '' (más recientes primero); {@code
 * pagina}, desde 1. {@code avisos}: lo que se pidió y no se entendió, en palabras, para decirlo
 * arriba de la lista (ese filtro no se aplica).
 */
public record ConsultaImportaciones(
    String buscar,
    String estado,
    String subida,
    LocalDate desde,
    LocalDate hasta,
    String orden,
    int pagina,
    List<String> avisos) {

  /** Largo máximo de la búsqueda, el mismo que acepta Donaciones. */
  public static final int LARGO_MAXIMO = 100;

  static final String NO_ENTENDIMOS_LA_FECHA =
      "No entendimos la fecha pedida: te mostramos todas las fechas.";
  static final String NO_ENTENDIMOS_EL_ESTADO =
      "No entendimos el estado pedido: te mostramos todos.";

  private static final Set<String> ESTADOS =
      Set.of("en-curso", "terminadas", "con-errores", "cortadas");
  private static final String RANGO = "rango";

  public ConsultaImportaciones {
    buscar = buscar == null ? "" : buscar.strip();
    if (buscar.length() > LARGO_MAXIMO) {
      buscar = buscar.substring(0, LARGO_MAXIMO);
    }
    estado = estado == null ? "" : estado;
    subida = subida == null ? "" : subida;
    orden = orden == null ? "" : orden;
    pagina = Math.max(1, pagina);
    avisos = avisos == null ? List.of() : List.copyOf(avisos);
  }

  /** La primera página, sin búsqueda ni filtros. */
  public static ConsultaImportaciones primera() {
    return new ConsultaImportaciones("", "", "", null, null, "", 1, List.of());
  }

  /**
   * Entiende lo que llegó en la dirección. Un estado o una fecha que no se entiende se descarta y
   * queda en {@code avisos}; un orden que no se entiende es el de siempre. {@code hoy} es el día en
   * Argentina, para traducir "hoy", "últimos 7 días" y los meses.
   */
  public static ConsultaImportaciones de(
      String buscar,
      String estado,
      String subida,
      String desde,
      String hasta,
      String orden,
      int pagina,
      LocalDate hoy) {
    List<String> avisos = new ArrayList<>();
    String estadoEntendido = vacio(estado);
    if (!estadoEntendido.isEmpty() && !ESTADOS.contains(estadoEntendido)) {
      avisos.add(NO_ENTENDIMOS_EL_ESTADO);
      estadoEntendido = "";
    }
    String subidaEntendida = vacio(subida);
    LocalDate[] rango = rango(subidaEntendida, hoy);
    if (subidaEntendida.isEmpty() && (!vacio(desde).isEmpty() || !vacio(hasta).isEmpty())) {
      subidaEntendida = RANGO;
    }
    if (RANGO.equals(subidaEntendida)) {
      rango = fechas(desde, hasta);
    }
    if (rango == null) {
      avisos.add(NO_ENTENDIMOS_LA_FECHA);
      subidaEntendida = "";
      rango = new LocalDate[2];
    }
    String ordenEntendido = "antiguas".equals(vacio(orden)) ? "antiguas" : "";
    return new ConsultaImportaciones(
        buscar,
        estadoEntendido,
        subidaEntendida,
        rango[0],
        rango[1],
        ordenEntendido,
        pagina,
        avisos);
  }

  /** Si hay búsqueda o algún filtro (el orden no cuenta). */
  public boolean filtrada() {
    return !buscar.isEmpty() || !estado.isEmpty() || !subida.isEmpty();
  }

  /** Si las fechas son las escritas en la dirección y no una de las opciones fijas. */
  public boolean conRango() {
    return RANGO.equals(subida);
  }

  /** Desde y hasta de una opción fija; vacío para "cualquier fecha"; null si no la entiende. */
  private static LocalDate[] rango(String subida, LocalDate hoy) {
    return switch (subida) {
      case "" -> new LocalDate[2];
      case "hoy" -> new LocalDate[] {hoy, hoy};
      case "7-dias" -> new LocalDate[] {hoy.minusDays(6), hoy};
      case "30-dias" -> new LocalDate[] {hoy.minusDays(29), hoy};
      case "este-mes" -> new LocalDate[] {hoy.withDayOfMonth(1), hoy};
      case "mes-anterior" -> {
        LocalDate primero = hoy.withDayOfMonth(1).minusMonths(1);
        yield new LocalDate[] {primero, primero.withDayOfMonth(primero.lengthOfMonth())};
      }
      case RANGO -> new LocalDate[2];
      default -> null;
    };
  }

  /** Las fechas escritas (AAAA-MM-DD); null si alguna no se entiende o están al revés. */
  private static LocalDate[] fechas(String desde, String hasta) {
    try {
      LocalDate inicio = vacio(desde).isEmpty() ? null : LocalDate.parse(vacio(desde));
      LocalDate fin = vacio(hasta).isEmpty() ? null : LocalDate.parse(vacio(hasta));
      if ((inicio == null && fin == null)
          || (inicio != null && fin != null && inicio.isAfter(fin))) {
        return null;
      }
      return new LocalDate[] {inicio, fin};
    } catch (DateTimeParseException noEsFecha) {
      return null;
    }
  }

  private static String vacio(String valor) {
    return valor == null ? "" : valor.strip();
  }
}
