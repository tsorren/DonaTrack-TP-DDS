package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminNotificacionesPuerto;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Marcar las notificaciones de la administración, fuera de la maqueta (en la maqueta lo atiende
 * SitioController). El formulario del buzón manda el tilde de una fila ({@code marcar}), el de
 * dejarla sin leer ({@code no-leer}) o las elegidas; sin ninguna, marca todas las de la
 * Administración. Vuelve a la misma página de la bandeja, con sus filtros.
 */
@Controller
@Profile("!maqueta")
class AdminNotificacionesEnvioController {

  static final String TODAS = "Listo: marcaste todas como leídas.";
  static final String UNA = "Listo: marcaste la notificación como leída.";
  static final String NO_LEIDA = "Listo: marcaste la notificación como no leída.";

  private final AdminNotificacionesPuerto notificaciones;

  AdminNotificacionesEnvioController(AdminNotificacionesPuerto notificaciones) {
    this.notificaciones = notificaciones;
  }

  @PostMapping("/admin-notificaciones-leidas")
  String marcar(
      @RequestParam(required = false) List<UUID> elegidas,
      @RequestParam(required = false) UUID marcar,
      @RequestParam(name = "no-leer", required = false) UUID noLeer,
      HttpServletRequest pedido,
      RedirectAttributes atributos) {
    String volver = BandejaDeNotificaciones.volver(AdminNotificacionesController.PANTALLA, pedido);
    if (noLeer != null) {
      notificaciones.dejarSinLeer(noLeer);
      return Envios.listo(volver, NO_LEIDA, atributos);
    }
    if (marcar != null) {
      notificaciones.marcarLeidas(List.of(marcar));
      return Envios.listo(volver, UNA, atributos);
    }
    List<UUID> ids = elegidas == null ? List.of() : elegidas;
    notificaciones.marcarLeidas(ids);
    String aviso;
    if (ids.isEmpty()) {
      aviso = TODAS;
    } else if (ids.size() == 1) {
      aviso = UNA;
    } else {
      aviso = DonanteNotificacionesEnvioController.elegidas(ids.size());
    }
    return Envios.listo(volver, aviso, atributos);
  }
}
