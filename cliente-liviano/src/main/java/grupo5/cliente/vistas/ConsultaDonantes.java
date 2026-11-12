package grupo5.cliente.vistas;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Qué página de donantes pide el admin (R11-01): la búsqueda por nombre o documento y los filtros
 * de la pantalla, tal como llegan en la dirección. {@code tipo}, {@code dona}, {@code estado} y
 * {@code orden} ya están validados contra las opciones que ofrece la vista ('' es "todos" y, en el
 * orden, "más nuevos primero"); {@code pagina} empieza en 1. {@code alta} (R11-34): un período fijo
 * de {@link PeriodoFijo} sobre la fecha de alta, o '' (cualquier fecha).
 */
public record ConsultaDonantes(
    String buscar, String tipo, String dona, String estado, String orden, int pagina, String alta) {

  public ConsultaDonantes {
    buscar = buscar == null ? "" : buscar.strip();
    tipo = tipo == null ? "" : tipo;
    dona = dona == null ? "" : dona;
    estado = estado == null ? "" : estado;
    orden = orden == null ? "" : orden;
    pagina = Math.max(1, pagina);
    alta = PeriodoFijo.valida(alta);
  }

  /** Sin el período de alta: la consulta de antes de R11-34. */
  public ConsultaDonantes(
      String buscar, String tipo, String dona, String estado, String orden, int pagina) {
    this(buscar, tipo, dona, estado, orden, pagina, "");
  }

  /** La primera página, sin búsqueda ni filtros. */
  public static ConsultaDonantes primera() {
    return new ConsultaDonantes("", "", "", "", "", 1);
  }

  /** Si hay búsqueda o algún filtro (el orden no cuenta). */
  public boolean filtrada() {
    return !buscar.isEmpty()
        || !tipo.isEmpty()
        || !dona.isEmpty()
        || !estado.isEmpty()
        || !alta.isEmpty();
  }

  /** Desde y hasta del período de alta elegido (días inclusivos); null si no hay período. */
  public LocalDate[] periodoDeAlta(LocalDate hoy) {
    return PeriodoFijo.de(alta, hoy);
  }

  /**
   * La dirección de la lista con esta búsqueda y estos filtros, sin la página ni el elegido
   * (R11-34): la base de cada renglón de la bandeja ({@code donante=} se suma al final).
   */
  public String direccion() {
    StringBuilder d = new StringBuilder("/admin-donantes?");
    sumar(d, "buscar", buscar);
    sumar(d, "tipo", tipo);
    sumar(d, "dona", dona);
    sumar(d, "estado", estado);
    sumar(d, "alta", alta);
    sumar(d, "orden", orden);
    return d.toString();
  }

  private static void sumar(StringBuilder d, String nombre, String valor) {
    if (!valor.isEmpty()) {
      d.append(nombre)
          .append('=')
          .append(URLEncoder.encode(valor, StandardCharsets.UTF_8))
          .append('&');
    }
  }
}
