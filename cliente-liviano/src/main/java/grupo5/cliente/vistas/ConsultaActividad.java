package grupo5.cliente.vistas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Qué página del registro de actividad pide el admin (R11-26), ya entendida: {@code buscar}, parte
 * del nombre, el apellido o el correo de una cuenta de administración; {@code autor}, {@code yo}
 * (la cuenta de la sesión), el id de una cuenta o '' (todas); {@code accion}, un grupo ({@code
 * propuestas}, {@code donaciones}, {@code donantes}, {@code organizaciones}, {@code cuentas},
 * {@code camiones}) o '' (todas); {@code fecha}, como la Subida de Importaciones ({@code hoy},
 * {@code 7-dias}, {@code 30-dias}, {@code este-mes}, {@code mes-anterior}, {@code rango} o ''), con
 * {@code desde} y {@code hasta} ya traducidos; {@code pagina}, desde 1. {@code avisos}: lo que se
 * pidió y no se entendió, en palabras (ese filtro no se aplica).
 */
public record ConsultaActividad(
    String buscar,
    String autor,
    String accion,
    String fecha,
    LocalDate desde,
    LocalDate hasta,
    int pagina,
    List<String> avisos) {

  /** Largo máximo de la búsqueda. */
  public static final int LARGO_MAXIMO = 100;

  /** La cuenta de la sesión, en la dirección. */
  public static final String YO = "yo";

  static final String NO_ENTENDIMOS_LA_ACCION =
      "No entendimos la acción pedida: te mostramos todas.";
  static final String NO_ENTENDIMOS_LA_CUENTA =
      "No entendimos la cuenta pedida: te mostramos la actividad de todos.";

  /** Los grupos de Acción, en el orden del menú del admin (ficha §4.1). */
  public static final List<String> GRUPOS =
      List.of("propuestas", "donaciones", "donantes", "organizaciones", "cuentas", "camiones");

  private static final Set<String> GRUPOS_VALIDOS = Set.copyOf(GRUPOS);

  /** Un id de cuenta: el {@code sub} de Keycloak (un UUID) o el de la maqueta; hasta 64. */
  private static final String CUENTA = "[A-Za-z0-9-]{1,64}";

  public ConsultaActividad {
    buscar = buscar == null ? "" : buscar.strip();
    if (buscar.length() > LARGO_MAXIMO) {
      buscar = buscar.substring(0, LARGO_MAXIMO);
    }
    autor = autor == null ? "" : autor;
    accion = accion == null ? "" : accion;
    fecha = fecha == null ? "" : fecha;
    pagina = Math.max(1, pagina);
    avisos = avisos == null ? List.of() : List.copyOf(avisos);
  }

  /** La primera página, sin búsqueda ni filtros. */
  public static ConsultaActividad primera() {
    return new ConsultaActividad("", "", "", "", null, null, 1, List.of());
  }

  /**
   * Entiende lo que llegó en la dirección. Una cuenta, una acción o una fecha que no se entiende se
   * descarta y queda en {@code avisos}. Las fechas, como en Importaciones; {@code hoy} es el día en
   * Argentina.
   */
  public static ConsultaActividad de(
      String buscar,
      String autor,
      String accion,
      String fecha,
      String desde,
      String hasta,
      int pagina,
      LocalDate hoy) {
    List<String> avisos = new ArrayList<>();
    String autorEntendido = vacio(autor);
    if (!autorEntendido.isEmpty() && !autorEntendido.matches(CUENTA)) {
      avisos.add(NO_ENTENDIMOS_LA_CUENTA);
      autorEntendido = "";
    }
    String accionEntendida = vacio(accion);
    if (!accionEntendida.isEmpty() && !GRUPOS_VALIDOS.contains(accionEntendida)) {
      avisos.add(NO_ENTENDIMOS_LA_ACCION);
      accionEntendida = "";
    }
    ConsultaImportaciones fechas =
        ConsultaImportaciones.de("", "", fecha, desde, hasta, "", 1, hoy);
    avisos.addAll(fechas.avisos());
    return new ConsultaActividad(
        buscar,
        autorEntendido,
        accionEntendida,
        fechas.subida(),
        fechas.desde(),
        fechas.hasta(),
        pagina,
        avisos);
  }

  /** Si hay búsqueda o algún filtro en la dirección. */
  public boolean filtrada() {
    return !buscar.isEmpty() || !autor.isEmpty() || !accion.isEmpty() || !fecha.isEmpty();
  }

  /** Si pide solo lo de la cuenta de la sesión. */
  public boolean soloLoMio() {
    return YO.equals(autor);
  }

  /** Si pide lo de una cuenta elegida (no la de la sesión). */
  public boolean deUnaCuenta() {
    return !autor.isEmpty() && !soloLoMio();
  }

  /** Si las fechas son las escritas en la dirección y no una de las opciones fijas. */
  public boolean conRango() {
    return "rango".equals(fecha);
  }

  /** Las fechas como una consulta de Importaciones, para reusar sus opciones de Subida. */
  ConsultaImportaciones comoFechas() {
    return new ConsultaImportaciones("", "", fecha, desde, hasta, "", 1, List.of());
  }

  private static String vacio(String valor) {
    return valor == null ? "" : valor.strip();
  }
}
