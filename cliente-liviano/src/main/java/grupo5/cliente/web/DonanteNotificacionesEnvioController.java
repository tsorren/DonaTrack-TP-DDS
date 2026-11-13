package grupo5.cliente.web;

import grupo5.cliente.puertos.CuentaActual;
import grupo5.cliente.puertos.DonanteNotificacionesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Marcar las notificaciones del donante, fuera de la maqueta (en la maqueta lo atiende
 * SitioController). El formulario del buzón manda el tilde de una fila ({@code marcar}), el de
 * dejarla sin leer ({@code no-leer}) o las elegidas; sin ninguna, marca todas. Vuelve a la misma
 * página de la bandeja, con sus filtros, y el aviso de la ficha.
 */
@Controller
@Profile("!maqueta")
class DonanteNotificacionesEnvioController {

  static final String TODAS = "Listo: marcaste todas como leídas.";
  static final String UNA = "Listo: marcaste la notificación como leída.";
  static final String NO_LEIDA = "Listo: marcaste la notificación como no leída.";

  private final DonanteNotificacionesPuerto notificaciones;
  private final CuentaActual cuentas;

  DonanteNotificacionesEnvioController(
      DonanteNotificacionesPuerto notificaciones, CuentaActual cuentas) {
    this.notificaciones = notificaciones;
    this.cuentas = cuentas;
  }

  @PostMapping("/donante-notificaciones-leidas")
  String marcar(
      @RequestParam(required = false) List<String> elegidas,
      @RequestParam(required = false) String marcar,
      @RequestParam(name = "no-leer", required = false) String noLeer,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    Optional<UUID> donante = cuentas.donante(pedido);
    if (donante.isEmpty()) {
      return "redirect:/publico-ingresar";
    }
    String aviso;
    if (noLeer != null) {
      notificaciones.marcar(donante.get(), List.of(noLeer), false);
      aviso = NO_LEIDA;
    } else if (marcar != null) {
      notificaciones.marcar(donante.get(), List.of(marcar), true);
      aviso = UNA;
    } else if (elegidas != null && !elegidas.isEmpty()) {
      notificaciones.marcar(donante.get(), elegidas, true);
      aviso = elegidas.size() == 1 ? UNA : elegidas(elegidas.size());
    } else {
      notificaciones.marcarTodas(donante.get());
      aviso = TODAS;
    }
    return Envios.listo(
        BandejaDeNotificaciones.volver("donante-notificaciones", pedido), aviso, atributos);
  }

  /** El aviso de varias elegidas (ficha, sección 5). */
  static String elegidas(int cuantas) {
    return "Listo: marcaste " + cuantas + " notificaciones como leídas.";
  }
}
