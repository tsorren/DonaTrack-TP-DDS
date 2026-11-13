package grupo5.cliente.vistas;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

/**
 * Qué página de donaciones pide el admin (R11-01), tal como llega en la dirección: {@code estado}
 * ({@code deposito}, {@code asignada}, {@code camino}, {@code entregada}, {@code vencida} o '' para
 * todas), {@code pronto} (solo las del depósito que vencen en los próximos 7 días), {@code orden}
 * ('' es el de partida de la fuente) y {@code pagina}, desde 1. Un estado que no es de la lista se
 * toma como ''.
 *
 * <p>Desde la bandeja (R11-34, ficha r11-p2-admin-bandeja §4.2): {@code buscar} (lo que se donó,
 * hasta 100 caracteres), {@code categoria} (la clave de la categoría o ''), {@code registrada} (un
 * período fijo: {@code hoy}, {@code 7-dias}, {@code 30-dias}, {@code este-mes}, {@code
 * mes-anterior} o ''), {@code donante} (la clave del donante, cuando se llega desde su ficha, o '')
 * y {@code donacion} (la clave de la elegida, cuya ficha se abre; '' si ninguna). Un valor que no
 * se entiende es el de siempre.
 */
public record ConsultaDonaciones(
    String estado,
    boolean pronto,
    String orden,
    int pagina,
    String buscar,
    String categoria,
    String registrada,
    String donante,
    String donacion) {

  /** Los estados que la pantalla filtra, en el orden de su menú. */
  public static final List<String> ESTADOS =
      List.of("deposito", "asignada", "camino", "entregada", "vencida");

  private static final int LARGO = 100;
  private static final String CLAVE = "[A-Za-z0-9-]{1,64}";

  public ConsultaDonaciones {
    estado = estado != null && ESTADOS.contains(estado) && !pronto ? estado : "";
    orden = orden == null ? "" : orden;
    pagina = Math.max(1, pagina);
    buscar = buscar == null ? "" : buscar.strip();
    buscar = buscar.length() > LARGO ? buscar.substring(0, LARGO) : buscar;
    categoria = clave(categoria);
    registrada = PeriodoFijo.valida(registrada);
    donante = clave(donante);
    donacion = clave(donacion);
  }

  /** Sin búsqueda, categoría, período, donante ni elegida: la consulta de antes de R11-34. */
  public ConsultaDonaciones(String estado, boolean pronto, String orden, int pagina) {
    this(estado, pronto, orden, pagina, "", "", "", "", "");
  }

  /** La misma consulta con otro orden (el que la fuente ofrece). */
  public ConsultaDonaciones conOrden(String otro) {
    return new ConsultaDonaciones(
        estado, pronto, otro, pagina, buscar, categoria, registrada, donante, donacion);
  }

  /** La misma consulta sin el donante ni la elegida ("Quitar" el filtro del donante). */
  public ConsultaDonaciones sinDonante() {
    return new ConsultaDonaciones(
        estado, pronto, orden, pagina, buscar, categoria, registrada, "", "");
  }

  /** Si hay búsqueda o algún filtro de la barra (el orden y el donante no cuentan). */
  public boolean filtrada() {
    return !estado.isEmpty()
        || pronto
        || !buscar.isEmpty()
        || !categoria.isEmpty()
        || !registrada.isEmpty();
  }

  /**
   * La dirección de la lista con estos filtros, sin la elegida ni la página. La base de cada
   * renglón es {@link #renglon(int)}, que suma la página.
   */
  public String direccion() {
    StringBuilder d = new StringBuilder("/admin-donaciones?");
    sumar(d, "estado", estado);
    sumar(d, "vence", pronto ? "pronto" : "");
    sumar(d, "orden", orden);
    sumar(d, "buscar", buscar);
    sumar(d, "categoria", categoria);
    sumar(d, "registrada", registrada);
    sumar(d, "donante", donante);
    return d.toString();
  }

  /**
   * La base de cada renglón de la bandeja y de "Cerrar la ficha" (R11-34, ficha
   * r11-p2-admin-bandeja §4): los filtros, el orden y la página {@code pagina} que se ve (la
   * primera no se escribe); {@code donacion=} se suma al final.
   */
  public String renglon(int pagina) {
    String d = direccion();
    return pagina > 1 ? d + "pagina=" + pagina + "&" : d;
  }

  /** Desde y hasta del período de registro elegido (días inclusivos); null si no hay período. */
  public LocalDate[] periodo(LocalDate hoy) {
    return PeriodoFijo.de(registrada, hoy);
  }

  private static String clave(String valor) {
    return valor != null && valor.strip().matches(CLAVE) ? valor.strip() : "";
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
