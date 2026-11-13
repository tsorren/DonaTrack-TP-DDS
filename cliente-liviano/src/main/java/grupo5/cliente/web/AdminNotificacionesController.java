package grupo5.cliente.web;

import grupo5.cliente.puertos.AdminNotificacionesPuerto;
import grupo5.cliente.vistas.AdminNotificacionesVista;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Notificaciones de la administración (R11-30, spec R11-p2-campanita §3.3) con los datos del
 * puerto: la bandeja compartida de la Administración, de a una página con los filtros de la
 * dirección (R11-14); también la bandeja después de marcar ({@code -leidas}, que en la maqueta
 * muestra el aviso). Los atributos son {@code bandeja} y {@code paginador} (null con una sola
 * página).
 */
@Controller
public class AdminNotificacionesController {

  static final String PANTALLA = "admin-notificaciones";

  private final AdminNotificacionesPuerto notificaciones;
  private final Pantallas pantallas;

  AdminNotificacionesController(AdminNotificacionesPuerto notificaciones, Pantallas pantallas) {
    this.notificaciones = notificaciones;
    this.pantallas = pantallas;
  }

  @GetMapping({"/" + PANTALLA, "/" + PANTALLA + "-leidas"})
  String bandeja(Model model, HttpServletRequest pedido) {
    String vista = pantallas.mostrar(pedido.getRequestURI().substring(1), model);
    AdminNotificacionesVista bandeja =
        notificaciones.bandeja(BandejaDeNotificaciones.consulta(pedido));
    model.addAttribute("bandeja", bandeja);
    model.addAttribute(
        "paginador", Paginadores.de(pedido, BandejaDeNotificaciones.PAGINAS, bandeja.pagina()));
    return vista;
  }
}
