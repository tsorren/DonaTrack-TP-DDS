package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteNotificacionesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Las notificaciones del donante con la bandeja del puerto; también la pantalla de después de
 * marcar de la maqueta, que usa la misma hoja. El atributo es {@code bandeja}. Sin cuenta, manda a
 * ingresar.
 */
@Controller
public class DonanteNotificacionesController {

  private static final String LEIDAS = "/donante-notificaciones-leidas";

  private final DonanteNotificacionesPuerto notificaciones;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  DonanteNotificacionesController(
      DonanteNotificacionesPuerto notificaciones, CuentaActual cuentas, Pantallas pantallas) {
    this.notificaciones = notificaciones;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  // Rutas literales: le ganan a la /{pantalla} de PantallasController.
  @GetMapping({"/donante-notificaciones", LEIDAS})
  String bandeja(Model model, HttpServletRequest pedido) {
    String pantalla =
        pedido.getRequestURI().endsWith(LEIDAS) ? LEIDAS.substring(1) : "donante-notificaciones";
    String vista = pantallas.mostrar(pantalla, model);
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    model.addAttribute("bandeja", notificaciones.bandeja(donante.get()));
    return vista;
  }
}
