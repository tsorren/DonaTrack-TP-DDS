package grupo5.cliente.vistas;

/**
 * Qué página de la bandeja de notificaciones se pide (R11-14, ficha
 * docs/interfaz/flujos/r11-p1-notificaciones.md), tal como llega en la dirección: {@code sinLeer}
 * ({@code ?ver=sin-leer}), {@code buscar} (recortado; '' sin búsqueda), {@code tema} (el valor de
 * una categoría del rol; '' todas), {@code viejas} ({@code ?orden=viejas}) y {@code pagina}, desde
 * 1. Agrupar no viaja: agrupa dentro de la página.
 */
public record ConsultaBandeja(
    boolean sinLeer, String buscar, String tema, boolean viejas, int pagina) {

  /** Cuántas notificaciones van en cada página. */
  public static final int TAMANIO = 20;

  public ConsultaBandeja {
    buscar = buscar == null ? "" : buscar.strip();
    tema = tema == null ? "" : tema.strip();
    pagina = Math.max(1, pagina);
  }

  /** La primera página, sin filtros, de la más nueva a la más vieja. */
  public static ConsultaBandeja primera() {
    return new ConsultaBandeja(false, "", "", false, 1);
  }

  /** Los parámetros de la dirección: {@code ver}, {@code buscar}, {@code tema} y {@code orden}. */
  public static ConsultaBandeja de(
      String ver, String buscar, String tema, String orden, int pagina) {
    return new ConsultaBandeja(
        "sin-leer".equals(ver), buscar, tema, "viejas".equals(orden), pagina);
  }
}
