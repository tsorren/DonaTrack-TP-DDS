package grupo5.cliente.vistas;

import java.time.LocalDate;
import java.util.Set;

/**
 * La búsqueda de Todas las decididas (R11-35, ficha r11-p2-admin-bandeja §4.3), ya entendida:
 * {@code buscar} (hasta 100 caracteres), {@code estado} ({@code aprobadas}, {@code descartadas} o
 * ''), {@code categoria} (la clave de la categoría o ''), {@code desde} y {@code hasta} (días
 * inclusivos sobre la fecha de la decisión, null no acota; desde R11-B04 reemplazan al período fijo
 * "Decidida"), {@code quien} (parte del nombre de la organización, sin mayúsculas ni tildes; '' no
 * filtra), {@code decidio} ({@code vos}, {@code antes}, el id de una cuenta o '') y {@code orden}
 * ({@code antiguas} o '', las más recientes primero). {@code vos} es la cuenta de la sesión ('' si
 * no se sabe). Un valor que no se entiende es el de siempre.
 */
public record ConsultaDecididas(
    String buscar,
    String estado,
    String categoria,
    LocalDate desde,
    LocalDate hasta,
    String quien,
    String decidio,
    String orden,
    String vos) {

  public static final String APROBADAS = "aprobadas";
  public static final String DESCARTADAS = "descartadas";
  public static final String FILTRO_VOS = "vos";
  public static final String ANTES = "antes";
  public static final String ANTIGUAS = "antiguas";

  /** Quien decidió sin cuenta, como lo entiende Donaciones (junta las que no tienen autor). */
  public static final String SISTEMA = "SISTEMA";

  private static final int LARGO = 100;
  private static final String CLAVE = "[A-Za-z0-9-]{1,64}";
  private static final Set<String> ESTADOS = Set.of(APROBADAS, DESCARTADAS);

  public ConsultaDecididas {
    buscar = buscar == null ? "" : buscar.strip();
    buscar = buscar.length() > LARGO ? buscar.substring(0, LARGO) : buscar;
    estado = estado != null && ESTADOS.contains(estado) ? estado : "";
    categoria = categoria != null && categoria.matches(CLAVE) ? categoria : "";
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      LocalDate antes = hasta;
      hasta = desde;
      desde = antes;
    }
    quien = quien == null ? "" : quien.strip();
    quien = quien.length() > LARGO ? quien.substring(0, LARGO) : quien;
    decidio = decidio != null && decidio.matches(CLAVE) ? decidio : "";
    orden = ANTIGUAS.equals(orden) ? orden : "";
    vos = vos == null ? "" : vos.strip();
  }

  /** Sin búsqueda ni filtros. */
  public static ConsultaDecididas ninguna() {
    return new ConsultaDecididas("", "", "", null, null, "", "", "", "");
  }

  /** Si hay búsqueda o algún filtro (el orden no cuenta). */
  public boolean filtrada() {
    return !buscar.isEmpty()
        || !estado.isEmpty()
        || !categoria.isEmpty()
        || conFecha()
        || !quien.isEmpty()
        || !decidio.isEmpty();
  }

  /** Si se acota la fecha de la decisión: las que no la tienen guardada no entran. */
  public boolean conFecha() {
    return desde != null || hasta != null;
  }

  /** El id de la cuenta que se pide a Donaciones; null si no se filtra por quién decidió. */
  public String autor() {
    return switch (decidio) {
      case "" -> null;
      case FILTRO_VOS -> vos.isEmpty() ? null : vos;
      case ANTES -> SISTEMA;
      default -> decidio;
    };
  }
}
