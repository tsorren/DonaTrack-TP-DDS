package grupo5.cliente.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Donaciones recibidas ya no es una pantalla: se fundió con Seguir entregas en Entregas (guía al
 * código, ORG-07, DA-02). La ruta vieja redirige a {@link EntidadSeguimientoController} con la
 * misma consulta, así los vínculos guardados (y los filtros {@code ?estado=}, {@code ?desde=} y
 * demás, que se llaman igual) siguen andando.
 */
@Controller
public class RecibidasController {

  @GetMapping("/entidad-recibidas")
  String recibidas(HttpServletRequest pedido) {
    String consulta = pedido.getQueryString();
    return "redirect:"
        + EntidadSeguimientoController.RUTA
        + (consulta == null || consulta.isBlank() ? "" : "?" + consulta);
  }
}
