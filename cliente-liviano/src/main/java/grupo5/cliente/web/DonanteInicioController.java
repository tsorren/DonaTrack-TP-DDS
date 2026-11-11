package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteInicioPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * El inicio del donante con los datos del puerto. El atributo es {@code miInicio}: {@code
 * donaciones} y {@code avisos} los define datos-donante, que sigue envolviendo la pantalla. Sin
 * cuenta, manda a ingresar.
 */
@Controller
public class DonanteInicioController {

  private final DonanteInicioPuerto inicios;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  DonanteInicioController(DonanteInicioPuerto inicios, CuentaActual cuentas, Pantallas pantallas) {
    this.inicios = inicios;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping("/donante-inicio")
  String inicio(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar("donante-inicio", model);
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    model.addAttribute("miInicio", inicios.inicio(donante.get()));
    return vista;
  }
}
