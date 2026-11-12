package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteMisionesPuerto;
import grupo5.cliente.vistas.DonanteMisionesVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Misiones e insignias del donante (RF-07) con las del puerto. El atributo es {@code misMisiones}.
 * Si el puerto dice que la cuenta no es una persona, la pantalla muestra "Sin permiso"; en la
 * maqueta lo decide la cuenta de ejemplo. Sin cuenta, manda a ingresar.
 */
@Controller
public class DonanteMisionesController {

  private final DonanteMisionesPuerto misiones;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  DonanteMisionesController(
      DonanteMisionesPuerto misiones, CuentaActual cuentas, Pantallas pantallas) {
    this.misiones = misiones;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/donante-misiones")
  String misiones(Model model, HttpServletRequest pedido) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    String vista = pantallas.mostrarDonante("donante-misiones", donante.get(), model, pedido);
    DonanteMisionesVista mias = misiones.misiones(donante.get());
    if (mias.persona() != null) {
      model.addAttribute("persona", mias.persona());
    }
    model.addAttribute("misMisiones", mias);
    return vista;
  }
}
