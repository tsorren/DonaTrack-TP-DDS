package grupo5.cliente.web;

import grupo5.cliente.maqueta.SitioController;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonacionesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Mis donaciones del donante (RF-05), con los datos del puerto. Sin cuenta todavía, manda a
 * ingresar, como un 401 de la API. El atributo se llama {@code misDonaciones} porque {@code
 * viaje-datos.html} ya define {@code donaciones} para las pantallas que siguen con la maqueta.
 */
@Controller
public class MisDonacionesController {

  private final DonacionesPuerto donaciones;
  private final CuentaActual cuentas;
  private final SitioController sitio;

  MisDonacionesController(
      DonacionesPuerto donaciones, CuentaActual cuentas, SitioController sitio) {
    this.donaciones = donaciones;
    this.cuentas = cuentas;
    this.sitio = sitio;
  }

  @GetMapping("/donante-donaciones")
  String misDonaciones(
      @RequestParam(required = false) String respuesta,
      @RequestParam(required = false) String cuenta,
      Model model,
      HttpServletRequest pedido) {
    // Primero la página: ?respuesta= responde sin llamar a la API.
    String vista =
        sitio.pagina("donante-donaciones", respuesta, cuenta, model, pedido.getSession());
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    model.addAttribute("misDonaciones", donaciones.misDonaciones(donante.get()));
    return vista;
  }
}
