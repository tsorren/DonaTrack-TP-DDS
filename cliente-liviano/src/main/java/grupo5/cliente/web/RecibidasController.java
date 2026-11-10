package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.RecibidasPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Donaciones recibidas de la entidad (RF-11), con los datos del puerto. Sin entidad todavía, manda
 * a ingresar, como un 401 de la API. El filtro por estado lo resuelve la plantilla.
 */
@Controller
public class RecibidasController {

  private final RecibidasPuerto recibidas;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  RecibidasController(RecibidasPuerto recibidas, CuentaActual cuentas, Pantallas pantallas) {
    this.recibidas = recibidas;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/entidad-recibidas")
  String recibidas(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("entidad-recibidas", model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    model.addAttribute("recibidas", recibidas.recibidas(entidad.get()));
    return vista;
  }
}
