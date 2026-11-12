package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteInicioPuerto;
import grupo5.cliente.vistas.DonanteAnioVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.thymeleaf.context.LazyContextVariable;

/**
 * El inicio del donante con los datos del puerto, en la versión de su tipo de cuenta. La empresa
 * lee {@code miInicio} ({@code donaciones} y {@code avisos} los define datos-donante, que sigue
 * envolviendo la pantalla); la persona (templates/persona/) lee {@code miAnio}, su año con las
 * cifras como números, sin la lista entera. En la empresa {@code miAnio} queda diferido: solo lo
 * lee la persona de ejemplo de la maqueta ({@code ?cuenta=persona}, que cambia la plantilla después
 * del controller), así que la empresa real no lo pide nunca. Sin cuenta, manda a ingresar.
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
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    UUID id = donante.get();
    String vista = pantallas.mostrarDonante("donante-inicio", id, model, pedido);
    if (vista.startsWith("persona/")) {
      model.addAttribute("miAnio", inicios.anio(id));
    } else {
      model.addAttribute("miInicio", inicios.inicio(id));
      model.addAttribute(
          "miAnio",
          new LazyContextVariable<DonanteAnioVista>() {
            @Override
            protected DonanteAnioVista loadValue() {
              return inicios.anio(id);
            }
          });
    }
    return vista;
  }
}
