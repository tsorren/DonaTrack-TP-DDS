package grupo5.cliente.web;

import jakarta.servlet.http.HttpServletRequest;

/** Lo común de los controllers de la entidad. */
final class Entidad {

  static final String INGRESAR = "redirect:/publico-ingresar";

  private Entidad() {}

  /** El nombre de la pantalla pedida: la dirección sin la barra ni el contexto. */
  static String pantalla(HttpServletRequest pedido) {
    return pedido.getRequestURI().substring(pedido.getContextPath().length() + 1);
  }
}
