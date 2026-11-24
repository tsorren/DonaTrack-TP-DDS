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
 * Seguimiento de una donación (RF-09, arquetipo A07, DA-03a) con los datos del puerto, en el
 * atributo {@code miSeguimiento}. La plantilla elige la donación ({@code ?donacion=}, o la primera
 * en camino), igual para la empresa y la persona. Sin cuenta, manda a ingresar.
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
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    String vista = pantallas.mostrarDonante("donante-seguimiento", donante.get(), model, pedido);
    model.addAttribute("miSeguimiento", seguimientos.seguimiento(donante.get()));
    return vista;
  }
}
