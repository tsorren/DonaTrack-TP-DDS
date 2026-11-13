package grupo5.cliente.web;

import grupo5.cliente.puertos.CampanitaPuerto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.RedirectView;

/**
 * La campanita de la cabecera privada (R11-29, R11-30; ficha
 * docs/interfaz/flujos/r11-p2-campanita.md, spec R11-p2-campanita §3.3), en los dos perfiles. El
 * desplegable se pide al abrirse, con HTMX: el fragmento {@code componentes/campanita :: lista} con
 * hasta 5 sin leer; si la fuente no contesta, el mismo fragmento con su error y 200, para no romper
 * la pantalla de abajo. Sin HTMX, la bandeja del área en "Sin leer". Abrir un aviso lo marca como
 * leído y lleva al destino que calcula el puerto desde la notificación, nunca desde un parámetro
 * del navegador.
 */
@Controller
class CampanitaController {

  static final String FRAGMENTO = "componentes/campanita :: lista";
  private static final String RECIENTES = "-notificaciones-recientes";
  private static final String ABRIR = "-notificaciones-abrir";

  private final CampanitaPuerto campanita;

  CampanitaController(CampanitaPuerto campanita) {
    this.campanita = campanita;
  }

  // Rutas literales: le ganan a la /{pantalla} de PantallasController.
  @GetMapping({"/donante" + RECIENTES, "/entidad" + RECIENTES, "/admin" + RECIENTES})
  ModelAndView recientes(HttpServletRequest pedido) {
    String area = area(pedido);
    if (pedido.getHeader("HX-Request") == null) {
      return new ModelAndView(redirigir("/" + area + "-notificaciones?ver=sin-leer"));
    }
    ModelAndView fragmento = new ModelAndView(FRAGMENTO);
    fragmento.addObject("area", area);
    try {
      fragmento.addObject("campanita", campanita.recientes(area, pedido));
    } catch (RuntimeException sinRespuesta) {
      // Sin la lista: el desplegable dice que no pudo traerlas y deja "Ver todas".
      fragmento.addObject("campanita", null);
    }
    return fragmento;
  }

  @PostMapping({"/donante" + ABRIR, "/entidad" + ABRIR, "/admin" + ABRIR})
  View abrir(@RequestParam(required = false) String id, HttpServletRequest pedido) {
    return redirigir(campanita.abrir(area(pedido), id, pedido));
  }

  /** El área sale de la dirección pedida: {@code /donante-notificaciones-…} es la del donante. */
  private static String area(HttpServletRequest pedido) {
    String ruta = pedido.getRequestURI().substring(pedido.getContextPath().length());
    return ruta.substring(1, ruta.indexOf('-'));
  }

  /** 303: el navegador pide el destino con GET, también después del post de abrir. */
  private static RedirectView redirigir(String destino) {
    RedirectView redireccion = new RedirectView(destino, true);
    redireccion.setStatusCode(HttpStatus.SEE_OTHER);
    return redireccion;
  }
}
