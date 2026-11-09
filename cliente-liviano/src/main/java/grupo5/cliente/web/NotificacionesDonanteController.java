package grupo5.cliente.web;

import grupo5.cliente.maqueta.NotificacionesDonanteMaqueta;
import grupo5.cliente.maqueta.SitioController;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.NotificacionesDonantePuerto;
import grupo5.cliente.vistas.NotificacionesDonanteVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Bandeja de notificaciones del donante (RF-08): muestra las noticias del puerto y marca todas como
 * leídas antes de redirigir a {@code /donante-notificaciones-leidas}. Sin cuenta, manda a ingresar.
 */
@Controller
public class NotificacionesDonanteController {

  private static final String INGRESAR = "redirect:/publico-ingresar";

  private final NotificacionesDonantePuerto notificaciones;
  private final CuentaActual cuentas;
  private final SitioController sitio;

  NotificacionesDonanteController(
      NotificacionesDonantePuerto notificaciones, CuentaActual cuentas, SitioController sitio) {
    this.notificaciones = notificaciones;
    this.cuentas = cuentas;
    this.sitio = sitio;
  }

  @GetMapping("/donante-notificaciones")
  String bandeja(
      @RequestParam(required = false) String respuesta,
      @RequestParam(required = false) String cuenta,
      Model model,
      HttpServletRequest pedido) {
    String vista =
        sitio.pagina("donante-notificaciones", respuesta, cuenta, model, pedido.getSession());
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    model.addAttribute("notificaciones", notificaciones.notificaciones(donante.get()));
    return vista;
  }

  @GetMapping("/donante-notificaciones-leidas")
  String leidas(
      @RequestParam(required = false) String respuesta,
      @RequestParam(required = false) String cuenta,
      Model model,
      HttpServletRequest pedido) {
    String vista =
        sitio.pagina(
            "donante-notificaciones-leidas", respuesta, cuenta, model, pedido.getSession());
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    NotificacionesDonanteVista datos = notificaciones.notificaciones(donante.get());
    if (!datos.sinLeer().isEmpty()) {
      datos = NotificacionesDonanteMaqueta.todasLeidas();
    }
    model.addAttribute("notificaciones", datos);
    return vista;
  }

  @PostMapping("/donante-notificaciones-leidas")
  String marcarTodas(HttpServletRequest pedido) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return INGRESAR;
    }
    notificaciones.marcarTodasLeidas(donante.get());
    return "redirect:/donante-notificaciones-leidas";
  }
}
