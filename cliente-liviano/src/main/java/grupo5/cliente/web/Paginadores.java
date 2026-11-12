package grupo5.cliente.web;

import grupo5.cliente.vistas.Paginador;
import grupo5.cliente.vistas.Tramo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Arma el paginador provisorio (R11-01) de una lista con la dirección del pedido: cada enlace es la
 * misma pantalla con {@code ?pagina=} cambiado y la búsqueda, los filtros y el orden que había, así
 * funciona sin JavaScript y el "atrás" del navegador vuelve a la página anterior.
 */
final class Paginadores {

  static final String PAGINA = "pagina";

  private Paginadores() {}

  /** El paginador de {@code tramo}, o null si entra en una sola página (no se dibuja). */
  static Paginador de(HttpServletRequest pedido, String nombre, Tramo<?> tramo) {
    if (tramo == null || tramo.paginas() <= 1) {
      return null;
    }
    int pagina = tramo.pagina();
    return new Paginador(
        nombre,
        pagina,
        tramo.paginas(),
        pagina > 1 ? direccion(pedido, pagina - 1) : null,
        pagina < tramo.paginas() ? direccion(pedido, pagina + 1) : null,
        pedido.getParameter(PAGINA) != null);
  }

  /** La página pedida en la dirección: 1 si falta o no se entiende. */
  static int pedida(HttpServletRequest pedido) {
    return Tramo.pedida(pedido.getParameter(PAGINA));
  }

  private static String direccion(HttpServletRequest pedido, int pagina) {
    return UriComponentsBuilder.fromPath(pedido.getRequestURI())
        .query(pedido.getQueryString())
        .replaceQueryParam(PAGINA, pagina)
        .replaceQueryParam(AdminDonantesController.ETAPA)
        .build()
        .toUriString();
  }
}
