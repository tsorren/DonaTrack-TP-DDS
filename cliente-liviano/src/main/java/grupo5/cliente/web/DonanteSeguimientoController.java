package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteSeguimientoPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Seguimiento del donante (RF-09) con los datos del puerto. El atributo es {@code miSeguimiento}:
 * {@code donaciones} lo define viaje-datos, que sigue envolviendo la pantalla. Sin cuenta, manda a
 * ingresar.
 */
@Controller
public class DonanteSeguimientoController {

  private final DonanteSeguimientoPuerto seguimientos;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  DonanteSeguimientoController(
      DonanteSeguimientoPuerto seguimientos, CuentaActual cuentas, Pantallas pantallas) {
    this.seguimientos = seguimientos;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/donante-seguimiento")
  String seguimiento(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("donante-seguimiento", model);
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    model.addAttribute("miSeguimiento", seguimientos.seguimiento(donante.get()));
    return vista;
  }
}
