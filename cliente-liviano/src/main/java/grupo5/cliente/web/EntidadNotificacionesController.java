package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.EntidadNotificacionesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Notificaciones de la entidad (RF-13) con los datos del puerto, también la bandeja después de
 * marcar ({@code -leidas}, que en la maqueta muestra el aviso). Sin entidad, manda a ingresar.
 */
@Controller
public class EntidadNotificacionesController {

  private final EntidadNotificacionesPuerto notificaciones;
  private final CuentaActual cuentas;
  private final Pantallas pantallas;

  EntidadNotificacionesController(
      EntidadNotificacionesPuerto notificaciones, CuentaActual cuentas, Pantallas pantallas) {
    this.notificaciones = notificaciones;
    this.cuentas = cuentas;
    this.pantallas = pantallas;
  }

  @GetMapping({"/entidad-notificaciones", "/entidad-notificaciones-leidas"})
  String bandeja(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar(Entidad.pantalla(pedido), model);
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return Entidad.INGRESAR;
    }
    model.addAttribute("bandeja", notificaciones.bandeja(entidad.get()));
    return vista;
  }
}
