package grupo5.cliente.web;

import grupo5.cliente.maqueta.NotificacionesEntidadMaqueta;
import grupo5.cliente.maqueta.SitioController;
import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.NotificacionesEntidadPuerto;
import grupo5.cliente.vistas.NotificacionesEntidadVista;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Bandeja de notificaciones de la entidad (RF-13): muestra las novedades del puerto y marca todas
 * como leídas antes de redirigir a {@code /entidad-notificaciones-leidas}. Sin entidad, manda a
 * ingresar.
 */
@Controller
public class NotificacionesEntidadController {

  private static final String INGRESAR = "redirect:/publico-ingresar";

  private final NotificacionesEntidadPuerto notificaciones;
  private final CuentaActual cuentas;
  private final SitioController sitio;

  NotificacionesEntidadController(
      NotificacionesEntidadPuerto notificaciones, CuentaActual cuentas, SitioController sitio) {
    this.notificaciones = notificaciones;
    this.cuentas = cuentas;
    this.sitio = sitio;
  }

  @GetMapping("/entidad-notificaciones")
  String bandeja(
      @RequestParam(required = false) String escenario,
      @RequestParam(required = false) String respuesta,
      @RequestParam(required = false) String cuenta,
      Model model,
      HttpServletRequest pedido) {
    String vista =
        sitio.pagina("entidad-notificaciones", respuesta, cuenta, model, pedido.getSession());
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return INGRESAR;
    }
    NotificacionesEntidadVista datos = notificaciones.novedades(entidad.get());
    if ("nada-sin-leer".equals(escenario) && !datos.sinLeer().isEmpty()) {
      datos = NotificacionesEntidadMaqueta.todasLeidas();
    }
    model.addAttribute("novedadesVista", datos);
    return vista;
  }

  @GetMapping("/entidad-notificaciones-leidas")
  String leidas(
      @RequestParam(required = false) String respuesta,
      @RequestParam(required = false) String cuenta,
      Model model,
      HttpServletRequest pedido) {
    String vista =
        sitio.pagina(
            "entidad-notificaciones-leidas", respuesta, cuenta, model, pedido.getSession());
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return INGRESAR;
    }
    NotificacionesEntidadVista datos = notificaciones.novedades(entidad.get());
    if (!datos.sinLeer().isEmpty()) {
      datos = NotificacionesEntidadMaqueta.todasLeidas();
    }
    model.addAttribute("novedadesVista", datos);
    return vista;
  }

  @PostMapping("/entidad-notificaciones-leidas")
  String marcarTodas(HttpServletRequest pedido) {
    Optional<UUID> entidad = cuentas.entidad(pedido);
    if (entidad.isEmpty()) {
      return INGRESAR;
    }
    notificaciones.marcarTodasLeidas(entidad.get());
    return "redirect:/entidad-notificaciones-leidas";
  }
}
