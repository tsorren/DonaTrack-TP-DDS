package grupo5.cliente.vistas;

import java.time.LocalDate;

/**
 * Qué página de la bandeja de notificaciones se pide (R11-14, ficha
 * docs/interfaz/flujos/r11-p1-notificaciones.md), tal como llega en la dirección: {@code sinLeer}
 * ({@code ?estado=sin-leer}), {@code leidas} ({@code ?estado=leidas}), {@code buscar} (recortado;
 * '' sin búsqueda), {@code tema} (el valor de una categoría del rol, {@code ?categoria=}; ''
 * todas), {@code viejas} ({@code ?orden=antiguas}), {@code desde} y {@code hasta} (días inclusivos
 * sobre la fecha del aviso; null no acota) y {@code pagina}, desde 1. Agrupar no viaja: agrupa
 * dentro de la página.
 *
 * <p>Desde R11-B04 (componente listado), los parámetros se llaman como en todas las listas; los de
 * antes ({@code ?ver=sin-leer}, {@code ?tema=} y {@code ?orden=viejas}) siguen valiendo.
 */
public record ConsultaBandeja(
    boolean sinLeer,
    boolean leidas,
    String buscar,
    String tema,
    boolean viejas,
    LocalDate desde,
    LocalDate hasta,
    int pagina) {

  /** Cuántas notificaciones van en cada página. */
  public static final int TAMANIO = 20;

  /** Los valores de {@code ?estado=}. */
  public static final String SIN_LEER = "sin-leer";

  public static final String ESTADO_LEIDAS = "leidas";

  public ConsultaBandeja {
    leidas = leidas && !sinLeer;
    buscar = buscar == null ? "" : buscar.strip();
    tema = tema == null ? "" : tema.strip();
    if (desde != null && hasta != null && desde.isAfter(hasta)) {
      LocalDate antes = hasta;
      hasta = desde;
      desde = antes;
    }
    pagina = Math.max(1, pagina);
  }

  /** Sin "Leídas" ni fechas: la consulta de antes de R11-B04. */
  public ConsultaBandeja(boolean sinLeer, String buscar, String tema, boolean viejas, int pagina) {
    this(sinLeer, false, buscar, tema, viejas, null, null, pagina);
  }

  /** La primera página, sin filtros, de la más nueva a la más vieja. */
  public static ConsultaBandeja primera() {
    return new ConsultaBandeja(false, "", "", false, 1);
  }

  /**
   * Los parámetros de la dirección de antes de R11-B04: {@code ver}, {@code tema} y {@code orden}.
   */
  public static ConsultaBandeja de(
      String ver, String buscar, String tema, String orden, int pagina) {
    return de(ver, buscar, tema, orden, null, null, pagina);
  }

  /**
   * Los parámetros de la dirección: {@code estado} (o {@code ver}), {@code buscar}, {@code
   * categoria} (o {@code tema}), {@code orden} ({@code antiguas} o {@code viejas}), {@code desde} y
   * {@code hasta}.
   */
  public static ConsultaBandeja de(
      String estado,
      String buscar,
      String categoria,
      String orden,
      String desde,
      String hasta,
      int pagina) {
    LocalDate[] rango = Listado.rango(desde, hasta);
    return new ConsultaBandeja(
        SIN_LEER.equals(estado),
        ESTADO_LEIDAS.equals(estado),
        buscar,
        categoria,
        Listado.ANTIGUAS.equals(orden) || "viejas".equals(orden),
        rango[0],
        rango[1],
        pagina);
  }

  /** El valor de {@code ?estado=}: '' (todas), {@link #SIN_LEER} o {@link #ESTADO_LEIDAS}. */
  public String estado() {
    if (sinLeer) {
      return SIN_LEER;
    }
    return leidas ? ESTADO_LEIDAS : "";
  }

  /** Si hay búsqueda o algún filtro (el orden no cuenta). */
  public boolean filtrada() {
    return sinLeer
        || leidas
        || !buscar.isEmpty()
        || !tema.isEmpty()
        || desde != null
        || hasta != null;
  }
}
