package grupo5.cliente.web;

import grupo5.cliente.vistas.ConsultaBandeja;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Lo común de las bandejas de notificaciones del donante y de la entidad (R11-14, ficha
 * docs/interfaz/flujos/r11-p1-notificaciones.md): la consulta que viene en la dirección y, después
 * de marcar, la vuelta a la misma página con los mismos filtros (el formulario del buzón los lleva
 * en su {@code action}).
 */
final class BandejaDeNotificaciones {

  /** El nombre del paginador para los lectores de pantalla (ficha, sección 6). */
  static final String PAGINAS = "Páginas de notificaciones";

  /** Lo que la bandeja conserva en la dirección: los filtros, el agrupado y la página. */
  private static final List<String> CONSERVA =
      List.of("ver", "buscar", "tema", "orden", "agrupar", Paginadores.PAGINA);

  private BandejaDeNotificaciones() {}

  static ConsultaBandeja consulta(HttpServletRequest pedido) {
    return ConsultaBandeja.de(
        pedido.getParameter("ver"),
        pedido.getParameter("buscar"),
        pedido.getParameter("tema"),
        pedido.getParameter("orden"),
        Paginadores.pedida(pedido));
  }

  /**
   * La pantalla con los filtros y la página que traía el pedido, para {@link Envios#listo}: {@code
   * donante-notificaciones?ver=sin-leer&pagina=2}.
   */
  static String volver(String pantalla, HttpServletRequest pedido) {
    UriComponentsBuilder direccion = UriComponentsBuilder.fromPath(pantalla);
    for (String nombre : CONSERVA) {
      String valor = pedido.getParameter(nombre);
      if (valor != null) {
        direccion.queryParam(nombre, valor);
      }
    }
    return direccion.encode().build().toUriString();
  }
}
