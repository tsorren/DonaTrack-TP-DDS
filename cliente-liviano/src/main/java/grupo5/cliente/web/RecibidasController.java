package grupo5.cliente.web;

import grupo5.cliente.maqueta.SitioController;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.RecibidasPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Donaciones recibidas de la entidad (RF-11), con los datos del puerto. Sin entidad todavía, manda
 * a ingresar, como un 401 de la API. El filtro por estado lo resuelve la plantilla.
 */
@Controller
public class RecibidasController {

  private final RecibidasPuerto recibidas;
  private final CuentaActual cuentas;
  private final SitioController sitio;

  RecibidasController(RecibidasPuerto recibidas, CuentaActual cuentas, SitioController sitio) {
    this.recibidas = recibidas;
    this.cuentas = cuentas;
    this.sitio = sitio;
  }

  @GetMapping("/entidad-recibidas")
  String recibidas(
      @RequestParam(required = false) String respuesta,
      @RequestParam(required = false) String cuenta,
      Model model,
      HttpServletRequest pedido) {
    // Primero la página: ?respuesta= responde sin llamar a la API.
    String vista = sitio.pagina("entidad-recibidas", respuesta, cuenta, model, pedido.getSession());
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    model.addAttribute("recibidas", recibidas.recibidas(entidad.get()));
    return vista;
  }
}
